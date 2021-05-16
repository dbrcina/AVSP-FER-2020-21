import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class ClosestBlackNode {

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
    private static final int MAX_DISTANCE = 10;
    private static final int NODE_NOT_FOUND = -1;

    private static class DataModel {
        private final int n;
        private final NodeType[] nodeTypes;
        private final List<List<Integer>> adjacencyMatrix;
        private final int[][] results;

        private DataModel(int n, NodeType[] nodeTypes, List<List<Integer>> adjacencyMatrix) {
            this.n = n;
            this.nodeTypes = nodeTypes;
            this.adjacencyMatrix = adjacencyMatrix;
            this.results = new int[n][];
        }
    }

    private enum NodeType {
        WHITE(0),
        BLACK(1);

        private final static NodeType[] values = values();
        private final int type;

        NodeType(int type) {
            this.type = type;
        }

        private static NodeType forType(int type) {
            for (NodeType nodeType : values) {
                if (nodeType.type == type) {
                    return nodeType;
                }
            }
            return null;
        }
    }

    private static class IntPair {
        private int v1;
        private int v2;

        private IntPair() {
        }

        private static IntPair of(int v1, int v2) {
            IntPair pair = new IntPair();
            pair.v1 = v1;
            pair.v2 = v2;
            return pair;
        }
    }

    public static void main(String[] args) throws IOException {
        DataModel dataModel = parseInput();
        calculateDistances(dataModel);
        try (OutputStream os = new BufferedOutputStream(System.out)) {
            int[][] results = dataModel.results;
            for (int[] result : results) {
                os.write(String.format("%d %d%n", result[0], result[1]).getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    private static DataModel parseInput() throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            // Parse n and e.
            int[] parts = parseLine(br);
            int n = parts[0];
            int e = parts[1];
            // Parse node types.
            NodeType[] nodeTypes = new NodeType[n];
            for (int i = 0; i < n; i++) {
                nodeTypes[i] = NodeType.forType(parseLine(br)[0]);
            }
            // Parse edges.
            List<List<Integer>> adjacencyMatrix = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                adjacencyMatrix.add(new ArrayList<>());
            }
            for (int i = 0; i < e; i++) {
                int[] nodes = parseLine(br);
                int si = nodes[0];
                int di = nodes[1];
                adjacencyMatrix.get(si).add(di);
                adjacencyMatrix.get(di).add(si);
            }
            return new DataModel(n, nodeTypes, adjacencyMatrix);
        }
    }

    private static void calculateDistances(DataModel dataModel) {
        int n = dataModel.n;
        NodeType[] nodeTypes = dataModel.nodeTypes;
        List<List<Integer>> adjacencyMatrix = dataModel.adjacencyMatrix;
        int[][] results = dataModel.results;
        Set<Integer> deadNodes = new HashSet<>();
        for (int i = 0; i < n; i++) {
            PriorityQueue<Integer> open = new PriorityQueue<>();
            open.add(i);
            IntPair result = findClosestBN(0, open, new HashSet<>(), deadNodes, nodeTypes, adjacencyMatrix);
            results[i] = new int[]{result.v1, result.v2};
            if (result.v1 == NODE_NOT_FOUND) {
                deadNodes.add(i);
            }
        }
    }

    private static IntPair findClosestBN(
            int dist,
            PriorityQueue<Integer> open,
            Set<Integer> visited,
            Set<Integer> deadNodes,
            NodeType[] nodeTypes,
            List<List<Integer>> adjacencyMatrix) {
        if (dist > MAX_DISTANCE || open.size() == 0) {
            return IntPair.of(NODE_NOT_FOUND, NODE_NOT_FOUND);
        }
        PriorityQueue<Integer> nextOpen = new PriorityQueue<>();
        while (!open.isEmpty()) {
            int node = open.poll();
            if (nodeTypes[node] == NodeType.BLACK) return IntPair.of(node, dist);
            visited.add(node);
            nextOpen.addAll(adjacencyMatrix.get(node).stream()
                    .filter(child -> !visited.contains(child) && !open.contains(child) && !deadNodes.contains(child))
                    .collect(Collectors.toList())
            );
        }
        return findClosestBN(dist + 1, nextOpen, visited, deadNodes, nodeTypes, adjacencyMatrix);
    }

    private static int[] parseLine(BufferedReader br) throws IOException {
        return Arrays.stream(readLineAndSplit(br))
                .mapToInt(Integer::parseInt)
                .toArray();
    }

    private static String[] readLineAndSplit(BufferedReader br) throws IOException {
        return br.readLine().strip().split(SPLIT_DEL);
    }

}
