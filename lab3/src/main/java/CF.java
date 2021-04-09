import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;

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

    private static int nItems;
    private static int nUsers;
    private static double[][] utilityM;
    private static int nQueries;
    private static int[][] queries;

    public static void main(String[] args) throws IOException {
        readInput();
    }

    private static void readInput() throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            // Read the number of items and number of users.
            String[] parts = br.readLine().strip().split(SPLIT_DEL);
            nItems = Integer.parseInt(parts[0]);
            nUsers = Integer.parseInt(parts[1]);
            // Read the utility matrix.
            utilityM = new double[nItems][nUsers];
            for (int i = 0; i < nItems; i++) {
                parts = br.readLine().strip().split(SPLIT_DEL);
                for (int j = 0; j < nUsers; j++) {
                    String s = parts[j];
                    utilityM[i][j] = s.equals(NO_RATING) ? 0.0 : Double.parseDouble(s);
                }
            }
            // Read the number of queries.
            nQueries = Integer.parseInt(br.readLine().strip());
            // Read the queries.
            queries = new int[nQueries][QUERY_LENGTH];
            for (int i = 0; i < nQueries; i++) {
                parts = br.readLine().strip().split(SPLIT_DEL);
                for (int j = 0; j < QUERY_LENGTH; j++) {
                    int val = Integer.parseInt(parts[j]);
                    queries[i][j] = val;
                    // If T=0, do item-item, otherwise do user-user.
                    if (j == 2 && val == 1) {
                        // swap i and j matrix indexes if user-user mode.
                        int iIndex = queries[i][0];
                        int jIndex = queries[i][1];
                        iIndex = iIndex ^ jIndex;
                        jIndex = iIndex ^ jIndex;
                        iIndex = iIndex ^ jIndex;
                        queries[i][0] = iIndex;
                        queries[i][1] = jIndex;
                    }
                }
            }
        }
    }

}
