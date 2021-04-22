import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Locale;

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
    private static final int N_ITERATIONS = 100;
    private static final StringBuilder RESULTS = new StringBuilder();
    private static final String LINE_SEP = System.lineSeparator();

    private static int n;
    private static double beta;
    private static int[][] adjacencyList;
    private static double[][] ranksPerIterations;
    private static int[][] queries;

    public static void main(String[] args) throws IOException {
        parseInput();
        pageRankAlgorithm();
        processQueries();
        System.out.println(RESULTS);
    }

    private static void parseInput() throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            // Parse n and beta.
            String[] parts = readLineAndSplit(br);
            n = Integer.parseInt(parts[0]);
            beta = Double.parseDouble(parts[1]);
            adjacencyList = new int[n][];
            // Parse connections.
            for (int i = 0; i < n; i++) {
                adjacencyList[i] = parseLine(br);
            }
            ranksPerIterations = new double[N_ITERATIONS + 1][n];
            // Parse q.
            int q = parseLine(br)[0];
            queries = new int[q][];
            // Parse queries.
            for (int i = 0; i < q; i++) {
                queries[i] = parseLine(br);
            }
        }
    }

    private static void pageRankAlgorithm() {
        double[] initialRanks = new double[n];
        Arrays.fill(initialRanks, 1.0 / n);
        ranksPerIterations[0] = initialRanks;
        for (int i = 1; i <= N_ITERATIONS; i++) {
            double[] previousRanks = ranksPerIterations[i - 1];
            double[] newRanks = new double[n];
            Arrays.fill(newRanks, (1 - beta) / n);
            for (int j = 0; j < n; j++) {
                int[] jthConnections = adjacencyList[j];
                double jthRank = beta * previousRanks[j] / jthConnections.length;
                for (int connection : jthConnections) {
                    newRanks[connection] += jthRank;
                }
            }
            ranksPerIterations[i] = newRanks;
        }
    }

    private static void processQueries() {
        for (int[] query : queries) {
            int ni = query[0];
            int ti = query[1];
            RESULTS.append(String.format(Locale.US, "%.10f%n", ranksPerIterations[ti][ni]));
        }
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
