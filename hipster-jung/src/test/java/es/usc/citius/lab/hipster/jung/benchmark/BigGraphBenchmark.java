/*
 * Copyright 2013 Centro de Investigación en Tecnoloxías da Información (CITIUS), University of Santiago de Compostela (USC).
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package es.usc.citius.lab.hipster.jung.benchmark;

import com.google.caliper.Benchmark;
import com.google.caliper.runner.CaliperMain;
import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.io.GraphMLReader;
import es.usc.citius.lab.hipster.algorithm.SearchIterators;
import es.usc.citius.lab.hipster.jung.JUNGSearchProblem;
import es.usc.citius.lab.hipster.jung.JungUtils;
import es.usc.citius.lab.hipster.node.CostNode;
import org.apache.commons.collections15.Factory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * @author Pablo Rodríguez Mier
 */
public class BigGraphBenchmark extends Benchmark {

    private Graph<String,String> testGraph = new DirectedSparseGraph<String, String>();

    // It takes about ~350 MB to read the entire graph in memory
    public static final String GRAPH_FILE = "slashdot0811.graphml.gz";

    private static final String SOURCE_VERTEX = "0";
    private static final String GOAL_VERTEX = "50000";

    @Override protected void setUp() throws Exception {
        // Define a factory to generate missing edge names
        Factory<String> edgeFactory = new Factory<String>() {
            private int counter = 0;
            @Override
            public String create() {
                return "edge-" + counter++;
            }
        };
        // Create the custom reader
        GraphMLReader<Graph<String,String>, String, String> reader = new GraphMLReader<Graph<String, String>, String, String>(null, edgeFactory);
        // Get the stream associated with the example graph slashdot.graphml
        InputStream fileStream = getClass().getClassLoader().getResourceAsStream(GRAPH_FILE);
        // Check if the file was located successfully
        if (fileStream == null){
            throw new NullPointerException(GRAPH_FILE + " cannot be found");
        }
        // Use a GZip stream
        InputStream ungzippedStream = new GZIPInputStream(fileStream);
        // Open the graph example
        InputStreamReader inputReader = new InputStreamReader(ungzippedStream);
        // populate it
        reader.load(inputReader, this.testGraph);
        // Close streams
        inputReader.close();
        ungzippedStream.close();
        fileStream.close();
    }

    public long timeJungGraph(long reps) {
        //System.out.println("Running Jung Dijkstra on graph with " + testGraph.getVertexCount() + " vertices");
        long pathSize = 0;
        for(int i=0; i < reps; i++){
            DijkstraShortestPath<String, String> dijkstra = JungUtils.createUnweightedDijkstraAlgorithm(testGraph, false);
            //Stopwatch w = new Stopwatch().start();
            List<String> path = dijkstra.getPath(SOURCE_VERTEX,GOAL_VERTEX);
            pathSize += path.size();
            //System.out.println("Path size: " + path.size() + " - " + w.stop().toString());
        }
        return pathSize;
    }

    public long timeHipster(long reps){
        //System.out.println("Running Hipster Dijkstra on graph with " + testGraph.getVertexCount() + " vertices");
        long pathSize = 0;
        for(int i=0; i<reps; i++){
            JUNGSearchProblem<String, String> problem = new JUNGSearchProblem<String, String>(this.testGraph, SOURCE_VERTEX, GOAL_VERTEX);
            Iterator<? extends CostNode<String, Double>> it = SearchIterators.aStar(problem);
            //Stopwatch w = new Stopwatch().start();
            pathSize += findPath(it, GOAL_VERTEX).path().size();
        }
        return pathSize;
        //System.out.println("Path size: " + pathSize + " - " + w.stop().toString());
    }


    private CostNode<String, Double> findPath(Iterator<? extends CostNode<String, Double>> it, String goal){
        while(it.hasNext()){
            CostNode<String, Double> o = it.next();
            if (o.transition().to().equals(GOAL_VERTEX)){
                return o;
            }
        }
        return null;
    }

    public static void main(String[] args) throws Exception {
        CaliperMain.main(BigGraphBenchmark.class, args);
    }

}
