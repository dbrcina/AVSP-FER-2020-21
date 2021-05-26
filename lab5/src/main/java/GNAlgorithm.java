import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private static final int ROUND_PLACES = 4;
    private static final RoundingMode ROUND_MODE = RoundingMode.HALF_UP;

    public static void main(String[] args) throws IOException {
        TaskModel model = readInput();
        calculateEdgeWeights(model);
        girvanNewman(model);
        try (OutputStream os = new BufferedOutputStream(System.out)) {
            List<int[]> removedEdgesResults = model.removedEdgesResults;
            for (int[] result : removedEdgesResults) {
                os.write(String.format("%d %d%n", result[0], result[1]).getBytes(StandardCharsets.UTF_8));
            }
            List<List<Integer>> communities = model.communities;
            communities.sort(Comparator.<List<Integer>>comparingInt(List::size).thenComparingInt(c -> c.get(0)));
            for (int i = 0; i < communities.size(); i++) {
                List<Integer> community = communities.get(i);
                if (community.size() == 1) {
                    os.write(String.format("%d", community.get(0)).getBytes(StandardCharsets.UTF_8));
                } else {
                    StringJoiner sj = new StringJoiner("-");
                    community.forEach(node -> sj.add(Integer.toString(node)));
                    os.write(sj.toString().getBytes(StandardCharsets.UTF_8));
                }
                if (i != communities.size() - 1) {
                    os.write(" ".getBytes(StandardCharsets.UTF_8));
                } else {
                    os.write(System.lineSeparator().getBytes(StandardCharsets.UTF_8));
                }
            }
        }
    }

    private static void girvanNewman(TaskModel model) {
        Map<Integer, Node> nodesMap = model.nodesMap;
        Set<Integer> nodesIds = nodesMap.keySet();
        Map<Integer, List<Edge>> adjacencyMatrix = model.adjacencyMatrix;
        List<Edge> edges = model.edges;
        List<int[]> removedEdgesResults = model.removedEdgesResults;
        Map<Integer, Map<Integer, Integer>> A = new HashMap<>();
        for (Edge e : edges) {
            int n1 = e.n1;
            int n2 = e.n2;
            int weight = e.weight;
            A.compute(n1, (k, v) -> {
                if (v == null) {
                    v = new HashMap<>();
                }
                v.put(n2, weight);
                return v;
            });
            A.compute(n2, (k, v) -> {
                if (v == null) {
                    v = new HashMap<>();
                }
                v.put(n1, weight);
                return v;
            });
        }
        double modularity = 0.0;
        List<List<Integer>> communities = null;
        double denominator = edges.stream()
                .mapToInt(e -> e.weight)
                .sum() * 2;
        while (!edges.isEmpty()) {
            double q = 0.0;
            for (int n1 : nodesIds) {
                Map<Integer, Integer> weightsFromN1 = A.get(n1);
                int kn1 = weightsFromN1 == null ? 0 : weightsFromN1.values().stream().mapToInt(i -> i).sum();
                for (int n2 : nodesIds) {
                    if (n1 != n2) {
                        Set<Integer> n2Community = new HashSet<>();
                        findCommunity(n2, n2Community, adjacencyMatrix);
                        if (!n2Community.contains(n1)) continue;
                    }
                    Map<Integer, Integer> weightsFromN2 = A.get(n2);
                    int kn2 = weightsFromN2 == null ? 0 : weightsFromN2.values().stream().mapToInt(i -> i).sum();
                    int a = weightsFromN1 == null ? 0 : weightsFromN1.getOrDefault(n2, 0);
                    q += (a - kn1 * kn2 / denominator);
                }
            }
            q /= denominator;
            q = new BigDecimal(q).setScale(ROUND_PLACES, ROUND_MODE).doubleValue();
            if (Math.abs(q) < DELTA) {
                q = 0.0;
            }
            System.err.println(q);
            if (communities == null || q > modularity) {
                modularity = q;
                communities = new ArrayList<>();
                for (int node : nodesIds) {
                    Set<Integer> communitySet = new TreeSet<>();
                    findCommunity(node, communitySet, adjacencyMatrix);
                    List<Integer> community = new ArrayList<>(communitySet);
                    if (!communities.contains(community)) {
                        communities.add(community);
                    }
                }
                model.communities = communities;
            }
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
            edges.forEach(e -> e.betweenness = new BigDecimal(e.betweenness / 2)
                    .setScale(ROUND_PLACES, ROUND_MODE)
                    .doubleValue()
            );
            double maxBetweenness = edges.stream()
                    .mapToDouble(edge -> edge.betweenness)
                    .max()
                    .getAsDouble();
            List<Edge> edgesToRemove = edges.stream()
                    .filter(edge -> Math.abs(edge.betweenness - maxBetweenness) <= DELTA)
                    .sorted(Comparator.<Edge>comparingInt(e -> e.n1).thenComparingInt(e -> e.n2))
                    .collect(Collectors.toList());
            edges.removeAll(edgesToRemove);
            for (Edge e : edgesToRemove) {
                List<Edge> n1Edges = adjacencyMatrix.get(e.n1);
                n1Edges.remove(e);
                if (n1Edges.isEmpty()) adjacencyMatrix.remove(e.n1);
                List<Edge> n2Edges = adjacencyMatrix.get(e.n2);
                n2Edges.remove(e);
                if (n2Edges.isEmpty()) adjacencyMatrix.remove(e.n2);
                removedEdgesResults.add(new int[]{e.n1, e.n2});
            }
            edges.forEach(e -> e.betweenness = 0);
        }
    }

    private static void findCommunity(int node, Set<Integer> community, Map<Integer, List<Edge>> adjacencyMatrix) {
        List<Edge> edges = adjacencyMatrix.get(node);
        if (!community.add(node) || edges == null) return;
        for (Edge edge : edges) {
            int newNode = edge.n1 == node ? edge.n2 : edge.n1;
            findCommunity(newNode, community, adjacencyMatrix);
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
        filteredPaths.forEach(path -> path.forEach(edge -> {
            BigDecimal bd = new BigDecimal(edge.betweenness + 1.0 / n);
            bd = bd.setScale(ROUND_PLACES, ROUND_MODE);
            edge.betweenness = bd.doubleValue();
        }));
    }

    private static void findPaths(
            int src,
            List<Edge> open,
            Set<Integer> visited,
            Map<Integer, List<Edge>> adjacencyMatrix,
            List<List<Edge>> paths,
            List<Edge> temp) {
        visited.add(src);
        if (open.isEmpty()) {
            paths.add(temp);
            return;
        }
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
        for (Edge e : edges) {
            Node n1 = nodesMap.get(e.n1);
            Node n2 = nodesMap.get(e.n2);
            e.weight = calculateSimilarity(n1, n2);
        }
    }

    private static int calculateSimilarity(Node n1, Node n2) {
        int counter = 0;
        int[] n1Properties = n1.properties;
        int[] n2Properties = n2.properties;
        for (int i = 0; i < n1.properties.length; i++) {
            if (n1Properties[i] == n2Properties[i]) {
                counter++;
            }
        }
        return n1Properties.length - (counter - 1);
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
        private List<List<Integer>> communities;

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

        private Edge(int n1, int n2, int weight) {
            int min = Math.min(n1, n2);
            int max = min == n1 ? n2 : n1;
            this.n1 = min;
            this.n2 = max;
            this.weight = weight;
        }

        private Edge(int n1, int n2) {
            this(n1, n2, 1);
        }

        @Override
        public String toString() {
            return String.format("(%d, %d); weight=%d; betweenness=%.4f", n1, n2, weight, betweenness);
        }
    }

}
