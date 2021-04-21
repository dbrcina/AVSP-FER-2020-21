import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class NodeRank {

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
    private static final StringBuilder RESULTS = new StringBuilder();
    private static final String LINE_SEP = System.lineSeparator();

    private static class NodeData {
        private final int id;
        private final int degree;
        private final int[] connections;

        private NodeData(int id, int... connections) {
            this.id = id;
            this.degree = connections.length;
            this.connections = connections;
        }
    }

    private static class QueryData {
        private final int nodeId;
        private final int iteration;

        private QueryData(int nodeId, int iteration) {
            this.nodeId = nodeId;
            this.iteration = iteration;
        }
    }

    private static double beta;
    private static Map<Integer, NodeData> adjacencyMap;
    private static Collection<NodeData> adjacencyList;
    private static double initialValue;
    private static double[] r;
    private static QueryData[] queries;

    public static void main(String[] args) throws IOException {
        parseInput();
        processQueries();
        System.out.println(RESULTS);
    }

    private static void parseInput() throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            // Parse n and beta.
            String[] parts = readLineAndSplit(br);
            int n = Integer.parseInt(parts[0]);
            beta = Double.parseDouble(parts[1]);
            adjacencyMap = new HashMap<>(n);
            initialValue = 1.0 / n;
            r = new double[n];
            // Parse connections.
            for (int i = 0; i < n; i++) {
                int[] connections = parseLine(br);
                adjacencyMap.putIfAbsent(i, new NodeData(i, connections));
            }
            adjacencyList = adjacencyMap.values();
            // Parse q.
            int q = parseLine(br)[0];
            queries = new QueryData[q];
            // Parse queries.
            for (int i = 0; i < q; i++) {
                int[] queryData = parseLine(br);
                queries[i] = new QueryData(queryData[0], queryData[1]);
            }
        }
    }

    private static String[] readLineAndSplit(BufferedReader br) throws IOException {
        return br.readLine().strip().split(SPLIT_DEL);
    }

    private static int[] parseLine(BufferedReader br) throws IOException {
        return Arrays.stream(readLineAndSplit(br))
                .mapToInt(Integer::parseInt)
                .toArray();
    }

    private static void processQueries() {
        for (QueryData query : queries) {
            int nodeId = query.nodeId;
            int iteration = query.iteration;
            pageRankAlgorithm(iteration);
            RESULTS.append(String.format(Locale.US, "%.10f%n", r[nodeId]));
        }
        RESULTS.setLength(RESULTS.length() - LINE_SEP.length());
    }

    private static void pageRankAlgorithm(int iteration) {
        Arrays.fill(r, initialValue);
        for (int i = 0; i < iteration; i++) {
            for (NodeData nodeData : adjacencyList) {
                double rank = 0.0;
                for (int connection : nodeData.connections) {
                    rank += beta * r[connection] / adjacencyMap.get(connection).degree;
                }
                r[nodeData.id] = rank;
            }
        }
    }

}

