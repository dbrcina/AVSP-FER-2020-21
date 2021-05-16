import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

    private static class DataModel {
        private final int n;
        private final double beta;
        private final List<int[]> adjacencyList;
        private final List<List<Query>> queriesPerIterations;
        private final double[] results;

        private DataModel(int n, double beta, List<int[]> adjacencyList, List<List<Query>> queriesPerIterations) {
            this.n = n;
            this.beta = beta;
            this.adjacencyList = adjacencyList;
            this.queriesPerIterations = queriesPerIterations;
            this.results = new double[n];
        }
    }

    private static class Query {
        private final int id;
        private final int ni;
        private final int ti;

        private Query(int id, int ni, int ti) {
            this.id = id;
            this.ni = ni;
            this.ti = ti;
        }
    }

    public static void main(String[] args) throws IOException {
        DataModel dataModel = parseInput();
        processQueries(dataModel);
        try (OutputStream os = new BufferedOutputStream(System.out)) {
            double[] results = dataModel.results;
            for (double result : results) {
                os.write(String.format(Locale.US, "%.10f%n", result).getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    private static DataModel parseInput() throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            // Parse n and beta.
            String[] parts = readLineAndSplit(br);
            int n = Integer.parseInt(parts[0]);
            double beta = Double.parseDouble(parts[1]);
            List<int[]> adjacencyList = new ArrayList<>(n);
            // Parse connections.
            for (int i = 0; i < n; i++) {
                adjacencyList.add(parseLine(br));
            }
            // Parse q.
            int q = parseLine(br)[0];
            List<List<Query>> queriesPerIterations = new ArrayList<>(N_ITERATIONS);
            for (int i = 0; i < N_ITERATIONS; i++) {
                queriesPerIterations.add(new ArrayList<>());
            }
            // Parse queries.
            for (int i = 0; i < q; i++) {
                int[] parsedData = parseLine(br);
                int ti = parsedData[1] - 1;
                Query query = new Query(i, parsedData[0], ti);
                queriesPerIterations.get(ti).add(query);
            }
            return new DataModel(n, beta, adjacencyList, queriesPerIterations);
        }
    }

    private static void processQueries(DataModel dataModel) {
        int n = dataModel.n;
        double beta = dataModel.beta;
        double teleport = (1 - beta) / n;
        List<int[]> adjacencyList = dataModel.adjacencyList;
        List<List<Query>> queriesPerIterations = dataModel.queriesPerIterations;
        double[] results = dataModel.results;
        double[] previousRanks = new double[n];
        Arrays.fill(previousRanks, 1.0 / n);
        for (List<Query> queries : queriesPerIterations) {
            double[] newRanks = new double[n];
            Arrays.fill(newRanks, teleport);
            for (int i = 0; i < n; i++) {
                int[] connections = adjacencyList.get(i);
                double rank = beta * previousRanks[i] / connections.length;
                for (int connection : connections) {
                    newRanks[connection] += rank;
                }
            }
            for (Query query : queries) {
                results[query.id] = newRanks[query.ni];
            }
            previousRanks = newRanks;
        }
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
