import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

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

    private static final String SPLIT_DEL = "\\s+";
    private static final double DELTA = 1e-5;

    public static void main(String[] args) throws IOException {
        TaskModel model = readInput();
        calculateEdgeWeights(model);
        girvanNewman(model);
        try (OutputStream os = new BufferedOutputStream(System.out)) {
            List<int[]> removedEdgesResults = model.removedEdgesResults;
            for (int[] result : removedEdgesResults) {
                os.write(String.format("%d %d%n", result[0], result[1]).getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    private static void girvanNewman(TaskModel model) {
        Map<Integer, Node> nodesMap = model.nodesMap;
        Set<Integer> nodesIds = nodesMap.keySet();
        Map<Integer, List<Edge>> adjacencyMatrix = model.adjacencyMatrix;
        List<Edge> edges = model.edges;
        List<int[]> removedEdgesResults = model.removedEdgesResults;
        while (!edges.isEmpty()) {
            for (int n1 : nodesIds) {
                for (int n2 : nodesIds) {
                    if (n1 == n2) continue;
                    calculateBetweennes(n1, n2, adjacencyMatrix);
                }
            }
            double maxBetweenness = edges.stream()
                    .mapToDouble(edge -> edge.betweenness)
                    .max()
                    .getAsDouble();
            List<Edge> edgesToRemove = edges.stream()
                    .filter(edge -> Math.abs(edge.betweenness - maxBetweenness) <= DELTA)
                    .sorted(Comparator.comparingInt(e -> ((Edge) e).n1)
                            .thenComparingInt(e -> ((Edge) e).n2))
                    .collect(Collectors.toList());
            edges.removeAll(edgesToRemove);
            for (Edge edge : edgesToRemove) {
                adjacencyMatrix.get(edge.n1).remove(edge);
                adjacencyMatrix.get(edge.n2).remove(edge);
                removedEdgesResults.add(new int[]{edge.n1, edge.n2});
            }
        }
    }

    private static void calculateBetweennes(int source, int destination, Map<Integer, List<Edge>> adjacencyMatrix) {
        List<Edge> open = adjacencyMatrix.get(source);
        if (open == null) return;
        List<List<Edge>> paths = new ArrayList<>();
        findShortestPaths(source, destination, open, new HashSet<>(), adjacencyMatrix, paths, new ArrayList<>());
        if (paths.isEmpty()) return;
        List<Integer> mappedPaths = paths.stream()
                .mapToInt(path -> path.stream()
                        .mapToInt(edge -> edge.weight)
                        .sum())
                .boxed()
                .collect(Collectors.toList());
        int minPathLength = mappedPaths.stream()
                .min(Comparator.naturalOrder())
                .get();
        List<List<Edge>> filteredPaths = new ArrayList<>();
        for (int i = 0; i < mappedPaths.size(); i++) {
            if (mappedPaths.get(i) == minPathLength) {
                filteredPaths.add(paths.get(i));
            }
        }
        int n = filteredPaths.size();
        filteredPaths.forEach(path -> path.forEach(edge -> edge.betweenness += 1.0 / n));
    }

    private static void findShortestPaths(
            int source,
            int destination,
            List<Edge> open,
            Set<Edge> visited,
            Map<Integer, List<Edge>> adjacencyMatrix,
            List<List<Edge>> paths,
            List<Edge> temp) {
        for (Edge e : open) {
            visited.add(e);
            List<Edge> newTemp = new ArrayList<>(temp);
            newTemp.add(e);
            int newSrc = e.n1 == source ? e.n2 : e.n1;
            if (newSrc == destination) {
                paths.add(newTemp);
                continue;
            }
            List<Edge> nextOpen = adjacencyMatrix.get(newSrc).stream()
                    .filter(edge -> !open.contains(edge) && !visited.contains(edge))
                    .collect(Collectors.toList());
            findShortestPaths(newSrc, destination, nextOpen, new HashSet<>(visited), adjacencyMatrix, paths, newTemp);
        }
    }

    private static void calculateEdgeWeights(TaskModel model) {
        Map<Integer, Node> nodesMap = model.nodesMap;
        List<Edge> edges = model.edges;
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
            Map<Integer, List<Edge>> adjacencyMatrix = new HashMap<>();
            List<Edge> edges = new ArrayList<>();
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) break;
                int[] ids = Arrays.stream(line.split(SPLIT_DEL))
                        .mapToInt(Integer::parseInt)
                        .toArray();
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
                int[] parsedLine = Arrays.stream(line.split(SPLIT_DEL))
                        .mapToInt(Integer::parseInt)
                        .toArray();
                Node n = new Node(parsedLine[0], Arrays.copyOfRange(parsedLine, 1, parsedLine.length));
                nodesMap.put(n.id, n);
            }
            return new TaskModel(nodesMap, adjacencyMatrix, edges);
        }
    }

    private static class TaskModel {
        private final Map<Integer, Node> nodesMap;
        private final Map<Integer, List<Edge>> adjacencyMatrix;
        private final List<Edge> edges;
        private final List<int[]> removedEdgesResults;

        private TaskModel(
                Map<Integer, Node> nodesMap,
                Map<Integer, List<Edge>> adjacencyMatrix,
                List<Edge> edges) {
            this.nodesMap = nodesMap;
            this.adjacencyMatrix = adjacencyMatrix;
            this.edges = edges;
            this.removedEdgesResults = new ArrayList<>(edges.size());
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
            int min = Math.min(n1, n2);
            int max = min == n1 ? n2 : n1;
            this.n1 = min;
            this.n2 = max;
            weight = 1;
            betweenness = 0;
        }

        @Override
        public String toString() {
            return String.format("(%d, %d)", n1, n2);
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
        public String toString() {
            return String.format("(%d,%d)", v1, v2);
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

}
