import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class PCY {

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            final long start = System.nanoTime();

            @Override
            public void run() {
                System.err.printf(Locale.US, "Runtime: %.6fs%n", (System.nanoTime() - start) * 1e-9);
            }
        }));
    }

    private static final String LINE_SEP = System.lineSeparator();
    private static final Map<Integer, Integer> ITEMS_FREQUENCIES = new HashMap<>();
    private static final Set<Integer> FREQUENT_ITEMS = new HashSet<>();
    private static final Map<IntPair, Integer> PAIRS_SUMS = new HashMap<>();

    private static List<List<Integer>> baskets;
    private static double threshold;
    private static int numOfBuckets;
    private static int[] buckets;
    private static int numOfItems;

    public static void main(String[] args) throws IOException {
        readInput();
        hashPairs();
        countPairs();
        int m = FREQUENT_ITEMS.size();
        StringBuilder sb = new StringBuilder();
        sb.append(m * (m - 1) / 2).append(LINE_SEP);
        sb.append(PAIRS_SUMS.size()).append(LINE_SEP);
        PAIRS_SUMS.values().stream()
                .sorted(Collections.reverseOrder())
                .forEach(v -> sb.append(v).append(LINE_SEP));
        sb.setLength(sb.length() - LINE_SEP.length());
        System.out.println(sb.toString());
    }

    private static void readInput() throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            // Read the number of baskets.
            int numOfBaskets = Integer.parseInt(br.readLine().strip());
            baskets = new ArrayList<>(numOfBaskets);
            // Read s and calculate threshold.
            threshold = Double.parseDouble(br.readLine().strip()) * numOfBaskets;
            // Read the number of buckets.
            numOfBuckets = Integer.parseInt(br.readLine().strip());
            buckets = new int[numOfBuckets];
            // Read every bucket.
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.strip().split("\\s+");
                List<Integer> basket = new ArrayList<>(parts.length);
                // For each item in the bucket, calculate its frequency.
                for (String part : parts) {
                    int item = Integer.parseInt(part);
                    basket.add(item);
                    int frequency = ITEMS_FREQUENCIES.merge(item, 1, Integer::sum);
                    if (frequency >= threshold) {
                        FREQUENT_ITEMS.add(item);
                    }
                }
                baskets.add(basket);
            }
        }
        numOfItems = ITEMS_FREQUENCIES.size();
    }

    private static void hashPairs() {
        for (List<Integer> basket : baskets) {
            for (int i = 0; i < basket.size() - 1; i++) {
                int ithItem = basket.get(i);
                if (!FREQUENT_ITEMS.contains(ithItem)) continue;
                for (int j = i + 1; j < basket.size(); j++) {
                    int jthItem = basket.get(j);
                    if (FREQUENT_ITEMS.contains(jthItem)) {
                        buckets[((ithItem * numOfItems) + jthItem) % numOfBuckets]++;
                    }
                }
            }
        }
    }

    private static void countPairs() {
        for (List<Integer> basket : baskets) {
            for (int i = 0; i < basket.size() - 1; i++) {
                int ithItem = basket.get(i);
                if (!FREQUENT_ITEMS.contains(ithItem)) continue;
                for (int j = i + 1; j < basket.size(); j++) {
                    int jthItem = basket.get(j);
                    if (FREQUENT_ITEMS.contains(jthItem)) {
                        if (buckets[((ithItem * numOfItems) + jthItem) % numOfBuckets] >= threshold) {
                            PAIRS_SUMS.merge(IntPair.of(ithItem, jthItem), 1, Integer::sum);
                        }
                    }
                }
            }
        }
    }

    public static final class IntPair {

        private int v1;
        private int v2;

        private IntPair() {
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

}
