import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class GNAlgorithm {

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            final long start = System.nanoTime();

            @Override
            public void run() {
                System.err.printf(Locale.US, "Runtime: %.6fs%n", (System.nanoTime() - start) * 1e-9);
            }
        }));
    }

    public static void main(String[] args) throws IOException {
        TaskModel model = readInput();
        calculateEdgeWeights(model);
        girvanNewman(model);
        System.out.println();
    }

    private static void girvanNewman(TaskModel model) {
        Map<Integer, Node> nodesMap = model.nodesMap;
        Set<Integer> nodesIds = nodesMap.keySet();
        Map<Integer, Collection<Edge>> adjacencyMatrix = model.adjacencyMatrix;
        Collection<Edge> edges = model.edges;
        Map<IntPair, Collection<Collection<Edge>>> allPathsBetweenTwoNodes = new HashMap<>();
        for (int n1 : nodesIds) {
            for (int n2 : nodesIds) {
                if (n1 == n2) continue;
                IntPair pair = new IntPair(Math.min(n1, n2), Math.max(n1, n2));
                if (allPathsBetweenTwoNodes.containsKey(pair)) continue;
                Collection<Collection<Edge>> results = new ArrayList<>();
                findShortestPaths(n1, n2, adjacencyMatrix, new HashSet<>(), results, new ArrayList<>());
                allPathsBetweenTwoNodes.put(pair, results);
            }
        }
        System.out.println();
    }

    private static void findShortestPaths(
            int src,
            int dest,
            Map<Integer, Collection<Edge>> adjacencyMatrix,
            Set<Integer> visited,
            Collection<Collection<Edge>> results,
            List<Edge> temp) {
        visited.add(src);
        Collection<Edge> edgesFromSrc = adjacencyMatrix.get(src);
        for (Edge e : edgesFromSrc) {
            int newSrc = e.n1 == src ? e.n2 : e.n1;
            if (newSrc != dest && visited.contains(newSrc)) continue;
            List<Edge> newTemp = new ArrayList<>(temp);
            newTemp.add(e);
            if (newSrc == dest) {
                results.add(newTemp);
                continue;
            }
            findShortestPaths(newSrc, dest, adjacencyMatrix, new HashSet<>(visited), results, newTemp);
        }
    }

    private static void calculateEdgeWeights(TaskModel model) {
        Map<Integer, Node> nodesMap = model.nodesMap;
        Collection<Edge> edges = model.edges;
        int maxSimilarity = nodesMap.size();
        for (Edge e : edges) {
            Node n1 = nodesMap.get(e.n1);
            Node n2 = nodesMap.get(e.n2);
            e.weight = calculateSimilarity(n1, n2, maxSimilarity);
        }
    }

    private static int calculateSimilarity(Node n1, Node n2, int maxSimilarity) {
        int counter = 0;
        int[] n1Properties = n1.properties;
        int[] n2Properties = n2.properties;
        for (int i = 0; i < n1.properties.length; i++) {
            if (n1Properties[i] == n2Properties[i]) {
                counter++;
            }
        }
        return maxSimilarity - (counter - 1);
    }

    private static TaskModel readInput() throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            String line;
            // Parse edges.
            Map<Integer, Collection<Edge>> adjacencyMatrix = new HashMap<>();
            Collection<Edge> edges = new ArrayList<>();
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) break;
                int[] ids = Arrays.stream(line.split("\\s+")).mapToInt(Integer::parseInt).toArray();
                Edge e = new Edge(ids[0], ids[1]);
                edges.add(e);
                adjacencyMatrix.compute(ids[0], (k, v) -> {
                    if (v == null) {
                        v = new ArrayList<>();
                    }
                    v.add(e);
                    return v;
                });
                adjacencyMatrix.compute(ids[1], (k, v) -> {
                    if (v == null) {
                        v = new ArrayList<>();
                    }
                    v.add(e);
                    return v;
                });
            }
            // Parse properties vectors.
            Map<Integer, Node> nodesMap = new HashMap<>();
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) break;
                int[] parsedLine = Arrays.stream(line.split("\\s+")).mapToInt(Integer::parseInt).toArray();
                Node n = new Node(parsedLine[0], Arrays.copyOfRange(parsedLine, 1, parsedLine.length));
                nodesMap.put(n.id, n);
            }
            return new TaskModel(nodesMap, adjacencyMatrix, edges);
        }
    }

    private static class TaskModel {
        private final Map<Integer, Node> nodesMap;
        private final Map<Integer, Collection<Edge>> adjacencyMatrix;
        private final Collection<Edge> edges;

        private TaskModel(
                Map<Integer, Node> nodesMap, Map<Integer,
                Collection<Edge>> adjacencyMatrix,
                Collection<Edge> edges) {
            this.nodesMap = nodesMap;
            this.adjacencyMatrix = adjacencyMatrix;
            this.edges = edges;
        }
    }

    private static class Node {
        private final int id;
        private final int[] properties;

        private Node(int id, int[] properties) {
            this.id = id;
            this.properties = properties;
        }
    }

    private static class Edge {
        private final int n1;
        private final int n2;
        private int weight;
        private double betweenness;

        private Edge(int n1, int n2) {
            this.n1 = n1;
            this.n2 = n2;
            weight = 1;
            betweenness = 0;
        }

        @Override
        public String toString() {
            return String.format("(%d,%d), weight=%d, betweenness=%.4f", n1, n2, weight, betweenness);
        }
    }

    private static class IntPair {
        private final int v1;
        private final int v2;

        private IntPair(int v1, int v2) {
            this.v1 = v1;
            this.v2 = v2;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof IntPair)) return false;
            IntPair intPair = (IntPair) o;
            return v1 == intPair.v1 && v2 == intPair.v2;
        }

        @Override
        public int hashCode() {
            return Objects.hash(v1, v2);
        }

        @Override
        public String toString() {
            return String.format("(%d,%d)", v1, v2);
        }
    }

}
