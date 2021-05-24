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
        Set<Integer> nodesIds = model.nodesMap.keySet();
        List<Edge> edges = model.edges;
        Map<Integer, List<Integer>> adjacencyMatrix = model.adjacencyMatrix;
        Map<SortedIntPair, Edge> edgesMap = model.edgesMap;
        List<int[]> removedEdgesResults = model.removedEdgesResults;
//        double modularity = 0.0;
//        Set<Set<Integer>> communities = new HashSet<>();
        while (!edges.isEmpty()) {
            Map<Integer, List<List<Integer>>> allPaths = findAllPaths(nodesIds, adjacencyMatrix);
            updateEdgeBetweennes(nodesIds, allPaths);
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
//                            .map(e -> e.weight)
//                            .orElse(0);
//                    q += (A - kn1 * kn2 / denominator);
//                }
//            }
//            q /= denominator;
//            if (Math.abs(q) <= DELTA) {
//                q = 0;
//            }
//            if (q >= modularity) {
//                communities.clear();
//                for (int n1 : nodesIds) {
//                    List<Edge> nEdges = adjacencyMatrix.get(n1);
//                    if (nEdges == null) {
//                        communities.add(Set.of(n1));
//                    } else {
//                        Set<Integer> community = new HashSet<>();
//                        for (Edge e : nEdges) {
//                            int n2 = e.n1 == n1 ? e.n2 : e.n1;
//                        }
//                    }
//                }
//            }
//            System.err.println(q);
//            edges.forEach(e -> e.betweenness /= 2);
//            double maxBetweenness = edges.stream()
//                    .mapToDouble(edge -> edge.betweenness)
//                    .max()
//                    .getAsDouble();
//            List<Edge> edgesToRemove = edges.stream()
//                    .filter(edge -> Math.abs(edge.betweenness - maxBetweenness) <= DELTA)
//                    .sorted(Comparator.comparingInt(e -> ((Edge) e).n1)
//                            .thenComparingInt(e -> ((Edge) e).n2))
//                    .collect(Collectors.toList());
//            edges.removeAll(edgesToRemove);
//            for (Edge e : edgesToRemove) {
//                adjacencyMatrix.get(e.n1).remove(e);
//                adjacencyMatrix.get(e.n2).remove(e);
//                removedEdgesResults.add(new int[]{e.n1, e.n2});
//            }
//            edges.forEach(e -> e.betweenness = 0);
        }
    }

    private static void updateEdgeBetweennes(Set<Integer> nodesIds, Map<Integer, List<List<Integer>>> allPaths) {
        for (int source : nodesIds) {
            List<List<Integer>> fromSourcePaths = allPaths.get(source);
            for (int destination : nodesIds) {
                if (source == destination) continue;
                List<List<Integer>> sourceToDestinationPaths = new ArrayList<>();
                for (List<Integer> fromSourcePath : fromSourcePaths) {
                    List<Integer> sourceToDestinationPath = new ArrayList<>();
                    for (int nextNode : fromSourcePath) {
                        sourceToDestinationPath.add(nextNode);
                        if (nextNode == destination && !sourceToDestinationPaths.contains(sourceToDestinationPath)) {
                            sourceToDestinationPaths.add(sourceToDestinationPath);
                            break;
                        }
                    }
                }
                if (sourceToDestinationPaths.isEmpty()) continue;

            }
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

    private static Map<Integer, List<List<Integer>>> findAllPaths(
            Set<Integer> nodesIds, Map<Integer, List<Integer>> adjacencyMatrix) {
        Map<Integer, List<List<Integer>>> allPaths = new HashMap<>();
        for (int node : nodesIds) {
            List<Integer> open = adjacencyMatrix.get(node);
            if (open == null) continue;
            List<List<Integer>> paths = new ArrayList<>();
            findPaths(node, open, new ArrayList<>(), adjacencyMatrix, paths, new ArrayList<>());
            allPaths.put(node, paths);
        }
        return allPaths;
    }

    private static void findPaths(
            int current,
            List<Integer> open,
            List<Integer> visited,
            Map<Integer, List<Integer>> adjacencyMatrix,
            List<List<Integer>> paths,
            List<Integer> temp) {
        visited.add(current);
        if (open.isEmpty()) {
            paths.add(temp);
            return;
        }
        for (int nextNode : open) {
            List<Integer> nextOpen = adjacencyMatrix.get(nextNode).stream()
                    .filter(node -> !visited.contains(node))
                    .collect(Collectors.toList());
            List<Integer> newTemp = new ArrayList<>(temp);
            newTemp.add(nextNode);
            findPaths(nextNode, nextOpen, new ArrayList<>(visited), adjacencyMatrix, paths, newTemp);
        }
    }

    private static void calculateEdgeWeights(TaskModel model) {
        Map<Integer, Node> nodesMap = model.nodesMap;
        List<Edge> edges = model.edges;
        int maxSimilarity = nodesMap.size();
        for (Edge edge : edges) {
            Node n1 = nodesMap.get(edge.n1);
            Node n2 = nodesMap.get(edge.n2);
            edge.weight = calculateSimilarity(n1, n2, maxSimilarity);
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
            List<Edge> edges = new ArrayList<>();
            Map<Integer, List<Integer>> adjacencyMatrix = new HashMap<>();
            Map<SortedIntPair, Edge> edgesMap = new HashMap<>();
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) break;
                int[] ids = Arrays.stream(line.split(SPLIT_DEL))
                        .mapToInt(Integer::parseInt)
                        .toArray();
                SortedIntPair pair = new SortedIntPair(ids[0], ids[1]);
                Edge edge = new Edge(pair);
                edges.add(edge);
                adjacencyMatrix.compute(ids[0], (k, v) -> {
                    if (v == null) {
                        v = new ArrayList<>();
                    }
                    v.add(ids[1]);
                    return v;
                });
                adjacencyMatrix.compute(ids[1], (k, v) -> {
                    if (v == null) {
                        v = new ArrayList<>();
                    }
                    v.add(ids[0]);
                    return v;
                });
                edgesMap.put(pair, edge);
            }
            // Parse properties vectors.
            Map<Integer, Node> nodesMap = new HashMap<>();
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) break;
                int[] parsedLine = Arrays.stream(line.split(SPLIT_DEL))
                        .mapToInt(Integer::parseInt)
                        .toArray();
                Node node = new Node(parsedLine[0], Arrays.copyOfRange(parsedLine, 1, parsedLine.length));
                nodesMap.put(node.id, node);
            }
            return new TaskModel(nodesMap, edges, adjacencyMatrix, edgesMap);
        }
    }

    private static class TaskModel {
        private final Map<Integer, Node> nodesMap;
        private final List<Edge> edges;
        private final Map<Integer, List<Integer>> adjacencyMatrix;
        private final Map<SortedIntPair, Edge> edgesMap;
        private final List<int[]> removedEdgesResults;
        private List<List<Edge>> communities;

        private TaskModel(
                Map<Integer, Node> nodesMap,
                List<Edge> edges,
                Map<Integer, List<Integer>> adjacencyMatrix,
                Map<SortedIntPair, Edge> edgesMap) {
            this.nodesMap = nodesMap;
            this.edges = edges;
            this.adjacencyMatrix = adjacencyMatrix;
            this.edgesMap = edgesMap;
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

        private Edge(SortedIntPair pair) {
            this.n1 = pair.v1;
            this.n2 = pair.v2;
            weight = 1;
        }

        @Override
        public String toString() {
            return String.format("(%d, %d); weight=%d; betweenness=%.4f", n1, n2, weight, betweenness);
        }
    }

    private static class SortedIntPair {
        private final int v1;
        private final int v2;

        private SortedIntPair(int v1, int v2) {
            int min = Math.min(v1, v2);
            int max = min == v1 ? v2 : v1;
            this.v1 = min;
            this.v2 = max;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SortedIntPair)) return false;
            SortedIntPair sortedIntPair = (SortedIntPair) o;
            return v1 == sortedIntPair.v1 && v2 == sortedIntPair.v2;
        }

        @Override
        public int hashCode() {
            return Objects.hash(v1, v2);
        }
    }

}
