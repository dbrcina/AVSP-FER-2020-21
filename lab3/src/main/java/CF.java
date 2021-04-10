import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class CF {

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
    private static final String NO_RATING = "X";
    private static final int QUERY_LENGTH = 4;
    private static final StringBuilder RESULTS = new StringBuilder();
    private static final String LINE_SEP = System.lineSeparator();
    private static final Map<IntPair, Double> I_SIMILARITIES = new HashMap<>();
    private static final Map<IntPair, Double> U_SIMILARITIES = new HashMap<>();

    private static double[][] itemsUsersM;
    private static double[][] usersItemsM;
    private static int[][] queries;

    private enum CFMode {
        ITEM_ITEM(0),
        USER_USER(1);

        private final int key;

        CFMode(int key) {
            this.key = key;
        }

        private static CFMode getValue(int key) {
            for (CFMode mode : values()) {
                if (mode.key == key) return mode;
            }
            throw new RuntimeException("Wrong CFMode type!");
        }
    }

    public static final class IntPair {

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

        public static IntPair of(int v1, int v2) {
            IntPair pair = new IntPair();
            pair.v1 = v1;
            pair.v2 = v2;
            return pair;
        }
    }

    public static void main(String[] args) throws IOException {
        parseInput();
        calculateSimilarities(itemsUsersM, I_SIMILARITIES);
        calculateSimilarities(usersItemsM, U_SIMILARITIES);
        processQueries();
        System.out.println(RESULTS);
    }

    private static void parseInput() throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            // Read the number of items and number of users.
            String[] parts = br.readLine().strip().split(SPLIT_DEL);
            int nItems = Integer.parseInt(parts[0]);
            int nUsers = Integer.parseInt(parts[1]);
            // Read the utility matrix.
            itemsUsersM = new double[nItems][nUsers];
            usersItemsM = new double[nUsers][nItems];
            for (int i = 0; i < nItems; i++) {
                parts = br.readLine().strip().split(SPLIT_DEL);
                for (int j = 0; j < nUsers; j++) {
                    String s = parts[j];
                    double val = s.equals(NO_RATING) ? 0.0 : Double.parseDouble(s);
                    itemsUsersM[i][j] = val;
                    usersItemsM[j][i] = val;
                }
            }
            // Read the number of queries.
            int nQueries = Integer.parseInt(br.readLine().strip());
            // Read the queries.
            queries = new int[nQueries][QUERY_LENGTH];
            for (int i = 0; i < nQueries; i++) {
                parts = br.readLine().strip().split(SPLIT_DEL);
                for (int j = 0; j < QUERY_LENGTH; j++) {
                    queries[i][j] = Integer.parseInt(parts[j]);
                }
            }
        }
    }

    private static void calculateSimilarities(double[][] matrix, Map<IntPair, Double> similarities) {
        int rows = matrix.length;
        int cols = matrix[0].length;
        // Calculate means in every row.
        double[] meanRows = new double[rows];
        for (int i = 0; i < rows; i++) {
            int counter = 0;
            double sum = 0;
            for (double value : matrix[i]) {
                if (value != 0) {
                    sum += value;
                    counter++;
                }
            }
            if (counter != 0) meanRows[i] = sum / counter;
        }
        // Normalize every row.
        double[][] normalizedRows = new double[rows][cols];
        for (int i = 0; i < rows; i++) {
            normalizedRows[i] = subtract(matrix[i], meanRows[i]);
        }
        // Calculate similarities.
        for (int i = 0; i < rows; i++) {
            double[] v1 = normalizedRows[i];
            for (int j = i; j < rows; j++) {
                double[] v2 = normalizedRows[j];
                double value = i == j ? 1 : cosine(v1, v2);
                similarities.put(IntPair.of(i, j), value);
            }
        }
    }

    private static double[] subtract(double[] v, double r) {
        double[] result = new double[v.length];
        for (int i = 0; i < v.length; i++) {
            if (v[i] != 0) {
                result[i] = v[i] - r;
            }
        }
        return result;
    }

    private static double cosine(double[] v1, double[] v2) {
        assert v1.length == v2.length;
        double nominator = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        for (int i = 0; i < v1.length; i++) {
            double value1 = v1[i];
            double value2 = v2[i];
            nominator += value1 * value2;
            norm1 += value1 * value1;
            norm2 += value2 * value2;
        }
        double denominator = Math.sqrt(norm1) * Math.sqrt(norm2);
        return nominator / denominator;
    }

    private static void processQueries() {
        for (int[] query : queries) {
            int i = query[0] - 1;
            int j = query[1] - 1;
            CFMode mode = CFMode.getValue(query[2]);
            int k = query[3];
            double score = switch (mode) {
                case ITEM_ITEM -> recommendScore(itemsUsersM, I_SIMILARITIES, i, j, k);
                case USER_USER -> recommendScore(usersItemsM, U_SIMILARITIES, j, i, k);
            };
            RESULTS.append(String.format(Locale.US, "%.3f%n", score));
        }
        RESULTS.setLength(RESULTS.length() - LINE_SEP.length());
    }

    @SuppressWarnings("unchecked")
    private static double recommendScore(double[][] matrix, Map<IntPair, Double> similarities, int i, int j, int k) {
        Map.Entry<IntPair, Double>[] similaritiesArray = similarities.entrySet().stream()
                .filter(entry -> {
                    IntPair key = entry.getKey();
                    double value = entry.getValue();
                    return (key.v1 == i || key.v2 == i) && value > 0 && value != 1;
                })
                .sorted(Map.Entry.<IntPair, Double>comparingByValue().reversed())
                .toArray(Map.Entry[]::new);
        double nominator = 0.0;
        double denominator = 0.0;
        for (Map.Entry<IntPair, Double> entry : similaritiesArray) {
            IntPair key = entry.getKey();
            int row = key.v1 == i ? key.v2 : key.v1;
            double r = matrix[row][j];
            if (r != 0) {
                double sim = entry.getValue();
                nominator += r * sim;
                denominator += sim;
                k--;
            }
            if (k == 0) break;
        }
        return nominator / denominator;
    }

}

