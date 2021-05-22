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
        Map<Integer, Node> nodesMap = readInput();
        calculateEdgeWeights(nodesMap);
        girvanNewman(nodesMap);
        System.out.println();
    }

    private static void girvanNewman(Map<Integer, Node> nodesMap) {
        Collection<Node> nodes = nodesMap.values();
        for (Node n1 : nodes) {
            int id1 = n1.id;
            for (Node n2 : nodes) {
                int id2 = n2.id;
                if (id1 == id2) continue;
                Collection<Collection<Edge>> shortestPaths = new ArrayList<>();
                findShortestPaths(id2, 0, n1.edgesMap.values(), shortestPaths);
            }
        }
    }

    private static void findShortestPaths(
            int destinationId,
            int d,
            Collection<Edge> open,
            Collection<Collection<Edge>> shortestPaths) {

    }

    private static void calculateEdgeWeights(Map<Integer, Node> nodesMap) {
        int maxSimilarity = nodesMap.size();
        for (Node n : nodesMap.values()) {
            for (Edge e : n.edgesMap.values()) {
                if (!e.weightAssigned) {
                    e.weight = calculateSimilarity(e.n1, e.n2, maxSimilarity);
                    e.weightAssigned = true;
                }
            }
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

    private static Map<Integer, Node> readInput() throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            String line;
            // Parse edges.
            Map<Integer, Node> nodesMap = new HashMap<>();
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) break;
                int[] ids = Arrays.stream(line.split("\\s+")).mapToInt(Integer::parseInt).toArray();
                Node n1 = nodesMap.computeIfAbsent(ids[0], Node::new);
                Node n2 = nodesMap.computeIfAbsent(ids[1], Node::new);
                Edge e = new Edge(n1, n2);
                IntPair pair = new IntPair(n1.id, n2.id);
                n1.edgesMap.put(pair, e);
                n2.edgesMap.put(pair, e);
            }
            // Parse properties vectors.
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) break;
                int[] parsedLine = Arrays.stream(line.split("\\s+")).mapToInt(Integer::parseInt).toArray();
                nodesMap.get(parsedLine[0]).properties = Arrays.copyOfRange(parsedLine, 1, parsedLine.length);
            }
            return nodesMap;
        }
    }

    private static class Node {
        private final int id;
        private final Map<IntPair, Edge> edgesMap;
        private int[] properties;

        private Node(int id) {
            this.id = id;
            this.edgesMap = new HashMap<>();
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
    }

    private static class Edge {
        private final Node n1;
        private final Node n2;
        private int weight;
        private boolean weightAssigned;
        private double betweenness;

        private Edge(Node n1, Node n2) {
            this.n1 = n1;
            this.n2 = n2;
            weight = 1;
            weightAssigned = false;
            betweenness = 0;
        }

        @Override
        public String toString() {
            return String.format("(%d,%d), weight=%d, betweenness=%.4f", n1.id, n2.id, weight, betweenness);
        }
    }

}
