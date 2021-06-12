import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class DGIM {

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            final long start = System.nanoTime();

            @Override
            public void run() {
                System.err.printf(Locale.US, "Runtime: %.6fs%n", (System.nanoTime() - start) * 1e-9);
            }
        }));
    }

    private static class Bucket {
        private int timestamp;
        private int cnt;

        private Bucket(int timestamp) {
            this.timestamp = timestamp;
            this.cnt = 1;
        }
    }

    public static void main(String[] args) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
             BufferedWriter bwr = new BufferedWriter(new OutputStreamWriter(System.out))) {
            List<Integer> results = new ArrayList<>();
            int currentTimestamp = -1;
            List<Bucket> buckets = new LinkedList<>();
            int N = Integer.parseInt(br.readLine().strip());
            String line;
            while ((line = br.readLine()) != null) {
                line = line.strip();
                if (line.startsWith("q")) {
                    int k = Integer.parseInt(line.split("\\s+")[1]);
                    int sum = 0;
                    int timestamp = currentTimestamp - k;
                    Bucket lastBucket = null;
                    for (int i = buckets.size() - 1; i >= 0; i--) {
                        Bucket bucket = buckets.get(i);
                        if (bucket.timestamp > timestamp) {
                            sum += bucket.cnt;
                            lastBucket = bucket;
                        } else {
                            break;
                        }
                    }
                    if (lastBucket != null) {
                        sum -= lastBucket.cnt;
                        sum += lastBucket.cnt / 2;
                    }
                    results.add(sum);
                } else {
                    for (char c : line.toCharArray()) {
                        currentTimestamp++;
                        if (!buckets.isEmpty() && currentTimestamp - buckets.get(0).timestamp >= N) {
                            buckets.remove(0);
                        }
                        if (c == '0') continue;
                        Bucket bucket = new Bucket(currentTimestamp);
                        buckets.add(bucket);
                        if (buckets.size() > 2) {
                            int i = buckets.size() - 2;
                            while (true) {
                                Bucket ithBucket = buckets.get(i);
                                Bucket ithPreviousBucket = buckets.get(i - 1);
                                if (ithPreviousBucket.cnt == ithBucket.cnt) {
                                    ithBucket.cnt *= 2;
                                    buckets.remove(i - 1);
                                }
                                i--;
                                if (i <= 1) break;
                                i--;
                                Bucket ithNextBucket = buckets.get(i + 1);
                                ithBucket = buckets.get(i);
                                ithPreviousBucket = buckets.get(i - 1);
                                if (!(ithPreviousBucket.cnt == ithNextBucket.cnt
                                        && ithBucket.cnt == ithNextBucket.cnt)) break;
                            }
                        }

                    }
                }
            }
            for (Integer result : results) {
                bwr.write(result + System.lineSeparator());
            }
        }
    }

}
