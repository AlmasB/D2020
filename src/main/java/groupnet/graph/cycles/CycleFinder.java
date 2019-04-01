package groupnet.graph.cycles;

import groupnet.euler.dual.*;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.graph.SimpleGraph;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This is an adapted version from the following stackoverflow answer:
 * http://stackoverflow.com/questions/14146165/find-all-the-paths-forming-simple-cycles-on-an-undirected-graph
 * from 13 Jan 2013.
 */
public class CycleFinder<V, E> {

    private UndirectedGraph<V, E> graph;
    private List<V> vertexList = new ArrayList<>();

    public CycleFinder(Class<E> type) {
        graph = new SimpleGraph<>(type);
    }

    public void addVertex(V vertex) {
        graph.addVertex(vertex);
        vertexList.add(vertex);
    }

    public void addEdge(V vertex1, V vertex2, E edge) {
        graph.addEdge(vertex1, vertex2, edge);
    }

    public UndirectedGraph<V, E> getGraph() {
        return graph;
    }

    @SuppressWarnings("unchecked")
    public List<MEDCycle> computeCycles() {
        List<MEDCycle> graphCycles = new ArrayList<>();

        List<List<V> > cycles = getAllCycles();

        for (List<V> cycle : cycles) {

            List<MEDVertex> vertices = (List<MEDVertex>) cycle;

            // we only care about cycles of even unique length
            int lengthUnique = MEDCycleKt.distinctSize(vertices);
            if (lengthUnique % 2 != 0) {
                continue;
            }

            List<E> edges = new ArrayList<>();

            for (int i = 0; i < cycle.size(); i++) {
                int j = i + 1 < cycle.size() ? i + 1 : 0;

                V v1 = cycle.get(i);
                V v2 = cycle.get(j);

                edges.add(graph.getEdge(v1, v2));
            }

            graphCycles.add(new MEDCycle(vertices, (List<MEDEdge>) edges));
        }

        graphCycles.sort((c1, c2) -> c1.length() - c2.length());

        return graphCycles;
    }

    @SuppressWarnings("unchecked")
    public List<groupnet.euler.dual.Cycle> computeCyclesAbstract() {
        List<groupnet.euler.dual.Cycle> graphCycles = new ArrayList<>();

        List<List<V> > cycles = getAllCycles();

        for (List<V> cycle : cycles) {

            List<Vertex> vertices = (List<Vertex>) cycle;
            List<E> edges = new ArrayList<>();

            for (int i = 0; i < cycle.size(); i++) {
                int j = i + 1 < cycle.size() ? i + 1 : 0;

                V v1 = cycle.get(i);
                V v2 = cycle.get(j);

                edges.add(graph.getEdge(v1, v2));
            }

            graphCycles.add(new groupnet.euler.dual.Cycle(vertices, (List<Edge>) edges));
        }

        graphCycles.sort((c1, c2) -> c1.getVertices().size() - c2.getVertices().size());

        return graphCycles;
    }

    @SuppressWarnings("unchecked")
    private List<List<V>> getAllCycles() {
        boolean adjMatrix[][] = buildAdjacencyMatrix();

        V[] vertexArray = (V[]) vertexList.toArray();
        ElementaryCyclesSearch ecs = new ElementaryCyclesSearch(adjMatrix, vertexArray);

        List<List<V>> cycles = ecs.getElementaryCycles();
        cycles.removeIf(cycle -> cycle.size() == 2);

        return removeRepeatedLists(cycles);
    }

    private boolean[][] buildAdjacencyMatrix() {
        Set<E> edges = graph.edgeSet();
        Integer nVertex = vertexList.size();

        boolean adjMatrix[][] = new boolean[nVertex][nVertex];

        for (E edge : edges) {
            V v1 = graph.getEdgeSource(edge);
            V v2 = graph.getEdgeTarget(edge);

            int i = vertexList.indexOf(v1);
            int j = vertexList.indexOf(v2);

            adjMatrix[i][j] = true;
            adjMatrix[j][i] = true;
        }

        return adjMatrix;
    }

    /*
     * Here repeated lists are those with the same vertices, no matter the order,
     * and it is assumed that there are no repeated vertices on any of the lists
     */
    private List<List<V>> removeRepeatedLists(List<List<V>> listOfLists) {
        return listOfLists.stream()
                .map(Cycle::new)
                .distinct()
                .map(c -> c.vertices)
                .collect(Collectors.toList());
    }

    private static class Cycle<V> {
        private List<V> vertices;

        Cycle(List<V> vertices) {
            this.vertices = vertices;
        }

        @SuppressWarnings({"EqualsWhichDoesntCheckParameterClass", "unchecked"})
        @Override
        public boolean equals(Object obj) {
            Cycle<V> other = (Cycle<V>) obj;

            if (vertices.size() != other.vertices.size())
                return false;

            List<V> copyVertices = new ArrayList<V>(vertices);

            for (V vertex : other.vertices) {
                copyVertices.removeIf(v -> v == vertex);
            }

            return copyVertices.isEmpty();
        }
    }
}