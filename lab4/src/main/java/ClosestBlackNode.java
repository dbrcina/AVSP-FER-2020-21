import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
    private static final StringBuilder RESULTS = new StringBuilder();
    private static final String LINE_SEP = System.lineSeparator();

    private enum NodeType {
        WHITE(0),
        BLACK(1);

        private final int type;

        NodeType(int type) {
            this.type = type;
        }

        private static NodeType forType(int type) {
            for (NodeType nodeType : values()) {
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

        @Override
        public String toString() {
            return "(%d, %d)".formatted(v1, v2);
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

        private static IntPair of(int v1, int v2) {
            IntPair pair = new IntPair();
            pair.v1 = v1;
            pair.v2 = v2;
            return pair;
        }
    }

    private static int n;
    private static NodeType[] nodeTypes;
    private static List<Set<Integer>> adjacencyMatrix;
    private static int[][] distances;

    public static void main(String[] args) throws IOException {
        parseInput();
        calculateDistances();
        prepareResults();
        System.out.println(RESULTS);
    }

    private static void parseInput() throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            // Parse n and e.
            int[] parts = parseLine(br);
            n = parts[0];
            int e = parts[1];
            // Parse node types.
            nodeTypes = new NodeType[n];
            for (int i = 0; i < n; i++) {
                nodeTypes[i] = NodeType.forType(parseLine(br)[0]);
            }
            // Parse edges.
            adjacencyMatrix = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                adjacencyMatrix.add(new HashSet<>());
            }
            for (int i = 0; i < e; i++) {
                int[] nodes = parseLine(br);
                int n1 = nodes[0];
                int n2 = nodes[1];
                adjacencyMatrix.get(n1).add(n2);
                adjacencyMatrix.get(n2).add(n1);
            }
        }
    }

    private static void calculateDistances() {
        distances = new int[n][];
        Set<Integer> deadNodes = new HashSet<>();
        for (int i = 0; i < n; i++) {
            IntPair result = findClosestBN(0, new TreeSet<>(Set.of(i)), new BitSet(n), deadNodes);
            distances[i] = new int[]{result.v1, result.v2};
            if (result.v1 == NODE_NOT_FOUND) {
                deadNodes.add(i);
            }
        }
    }

    private static IntPair findClosestBN(int dist, SortedSet<Integer> open, BitSet visited, Set<Integer> deadNodes) {
        if (dist > MAX_DISTANCE || open.size() == 0) {
            return IntPair.of(NODE_NOT_FOUND, NODE_NOT_FOUND);
        }
        SortedSet<Integer> nextOpen = new TreeSet<>();
        for (int node : open) {
            if (nodeTypes[node] == NodeType.BLACK) {
                return IntPair.of(node, dist);
            }
            visited.set(node);
            nextOpen.addAll(adjacencyMatrix.get(node).stream()
                    .filter(i -> !visited.get(i) && !open.contains(i) && !deadNodes.contains(i))
                    .collect(Collectors.toList())
            );
        }
        return findClosestBN(dist + 1, nextOpen, visited, deadNodes);
    }

    private static void prepareResults() {
        Arrays.stream(distances).forEach(d -> RESULTS.append(String.format("%d %d%n", d[0], d[1])));
        RESULTS.setLength(RESULTS.length() - LINE_SEP.length());
    }

    private static String[] readLineAndSplit(BufferedReader br) throws IOException {
        return br.readLine().strip().split(SPLIT_DEL);
    }

    private static int[] parseLine(BufferedReader br) throws IOException {
        return Arrays.stream(readLineAndSplit(br))
                .mapToInt(Integer::parseInt)
                .toArray();
    }

}
