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
//            double denominator = edges.stream()
//                    .mapToInt(e -> e.weight)
//                    .sum() * 2;
//            double q = 0.0;
//            for (int n1 : nodesIds) {
//                List<Edge> n1Edges = adjacencyMatrix.get(n1);
//                if (n1Edges == null) continue;
//                int kn1 = n1Edges.stream()
//                        .mapToInt(e -> e.weight)
//                        .sum();
//                for (int n2 : nodesIds) {
//                    List<Edge> n2Edges = adjacencyMatrix.get(n2);
//                    if (n2Edges == null) continue;
//                    if (n2Edges.stream().noneMatch(e -> e.n1 == n1 || e.n2 == n1)) continue;
//                    int kn2 = n2Edges.stream()
//                            .mapToInt(e -> e.weight)
//                            .sum();
//                    int A = n2Edges.stream()
//                            .filter(e -> e.n1 == n1 && e.n2 == n2 || e.n1 == n2 && e.n2 == n1)
//                            .findFirst()
//                            .map(edge -> edge.weight).orElse(0);
//                    q += (A - kn1 * kn2 / denominator);
//                }
//            }
//            q /= denominator;
//            System.err.println(q);
            for (int n1 : nodesIds) {
                List<Edge> open = adjacencyMatrix.get(n1);
                if (open == null) continue;
                List<List<Edge>> paths = new ArrayList<>();
                findPaths(n1, open, new HashSet<>(), adjacencyMatrix, paths, new ArrayList<>());
                for (int n2 : nodesIds) {
                    if (n1 != n2) {
                        calculateBetweennes(n2, paths);
                    }
                }
            }
            edges.forEach(e -> e.betweenness /= 2);
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
            for (Edge e : edgesToRemove) {
                adjacencyMatrix.get(e.n1).remove(e);
                adjacencyMatrix.get(e.n2).remove(e);
                removedEdgesResults.add(new int[]{e.n1, e.n2});
            }
            edges.forEach(e -> e.betweenness = 0);
        }
    }

    private static void calculateBetweennes(int destination, List<List<Edge>> paths) {
        List<List<Edge>> srcToDestinationPaths = new ArrayList<>();
        for (List<Edge> path : paths) {
            List<Edge> temp = new ArrayList<>();
            for (Edge e : path) {
                temp.add(e);
                if (e.n1 == destination || e.n2 == destination) {
                    if (!srcToDestinationPaths.contains(temp)) {
                        srcToDestinationPaths.add(temp);
                    }
                    break;
                }
            }
        }
        if (srcToDestinationPaths.isEmpty()) return;
        List<Integer> mappedPathsToLengths = srcToDestinationPaths.stream()
                .mapToInt(path -> path.stream()
                        .mapToInt(edge -> edge.weight)
                        .sum())
                .boxed()
                .collect(Collectors.toList());
        int minPathLength = Collections.min(mappedPathsToLengths);
        List<List<Edge>> filteredPaths = new ArrayList<>();
        for (int i = 0; i < mappedPathsToLengths.size(); i++) {
            if (mappedPathsToLengths.get(i) == minPathLength) {
                filteredPaths.add(srcToDestinationPaths.get(i));
            }
        }
        int n = filteredPaths.size();
        filteredPaths.forEach(path -> path.forEach(edge -> edge.betweenness += 1.0 / n));
    }

    private static void findPaths(
            int src,
            List<Edge> open,
            Set<Integer> visited,
            Map<Integer, List<Edge>> adjacencyMatrix,
            List<List<Edge>> paths,
            List<Edge> temp) {
        if (open.isEmpty()) {
            paths.add(temp);
            return;
        }
        visited.add(src);
        for (Edge e : open) {
            int newSrc = e.n1 == src ? e.n2 : e.n1;
            List<Edge> newTemp = new ArrayList<>(temp);
            newTemp.add(e);
            List<Edge> nextOpen = adjacencyMatrix.get(newSrc).stream()
                    .filter(edge -> !open.contains(edge) && !visited.contains(edge.n1) && !visited.contains(edge.n2))
                    .collect(Collectors.toList());
            findPaths(newSrc, nextOpen, new HashSet<>(visited), adjacencyMatrix, paths, newTemp);
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
            return String.format("(%d, %d); weight=%d; betweenness=%.4f", n1, n2, weight, betweenness);
        }
    }

}
