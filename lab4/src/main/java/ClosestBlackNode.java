import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Locale;

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

    private static int n;
    private static int e;
    private static NodeType[] nodes;
    private static int[][] edges;
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
            e = parts[1];
            // Parse node types.
            nodes = new NodeType[n];
            for (int i = 0; i < n; i++) {
                nodes[i] = NodeType.forType(parseLine(br)[0]);
            }
            // Parse edges.
            edges = new int[e][];
            for (int i = 0; i < e; i++) {
                edges[i] = parseLine(br);
            }
        }
    }

    private static void calculateDistances() {
        distances = new int[n][];
        for (int i = 0; i < n; i++) {
            if (nodes[i] == NodeType.BLACK) {
                distances[i] = new int[]{i, 0};
                continue;
            }
            for (int[] edge : edges) {
                if (edge[0] == i) {

                }
            }
        }
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
