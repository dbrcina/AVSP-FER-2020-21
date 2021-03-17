import org.apache.commons.codec.binary.BinaryCodec;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class SimHash {

    private static final DigestUtils DIGEST_UTILS = new DigestUtils("MD5");
    private static final int HASH_BIN_LENGTH = DIGEST_UTILS.getMessageDigest().getDigestLength() * 8;

    public static void main(String[] args) {
        // Linked list: texts -> queries
        List<String[]> inputs = readInput();
        // inputs.remove(0) removes and returns texts and so on...
        int[][] hashes = prepareHashes(inputs.remove(0));
        Arrays.stream(processQueries(inputs.remove(0), hashes))
                .forEach(System.out::println);
    }

    private static List<String[]> readInput() {
        List<String[]> inputs = new LinkedList<>();
        String[] texts = null;
        String[] queries = null;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            int N = Integer.parseInt(br.readLine().strip());
            texts = new String[N];
            for (int i = 0; i < N; i++) {
                texts[i] = br.readLine().strip();
            }
            int Q = Integer.parseInt(br.readLine().strip());
            queries = new String[Q];
            for (int i = 0; i < Q; i++) {
                queries[i] = br.readLine().strip();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        inputs.add(texts);
        inputs.add(queries);
        return inputs;
    }

    private static int[][] prepareHashes(String[] texts) {
        int[][] hashes = new int[texts.length][];
        Map<String, byte[]> digestsCache = new HashMap<>();
        for (int i = 0; i < hashes.length; i++) {
            hashes[i] = simHash(texts[i], digestsCache);
        }
        return hashes;
    }

    private static int[] simHash(String text, Map<String, byte[]> digestsCache) {
        int[] sh = new int[HASH_BIN_LENGTH];
        String[] terms = text.split("\\s+");
        for (String term : terms) {
            byte[] digest = digestsCache.computeIfAbsent(term, k -> DIGEST_UTILS.digest(term));
            char[] hashBinChars = BinaryCodec.toAsciiString(digest).toCharArray();
            for (int i = 0; i < hashBinChars.length; i++) {
                if (hashBinChars[i] == '1') {
                    sh[i] += 1;
                } else {
                    sh[i] -= 1;
                }
            }
        }
        for (int i = 0; i < sh.length; i++) {
            sh[i] = sh[i] >= 0 ? 1 : 0;
        }
        return sh;
    }

    private static int[] processQueries(String[] queries, int[][] hashes) {
        int[] results = new int[queries.length];
        Map<String, Integer> distancesCache = new HashMap<>();
        for (int i = 0; i < queries.length; i++) {
            String query = queries[i];
            String[] parts = query.split("\\s+");
            int I = Integer.parseInt(parts[0]);
            int K = Integer.parseInt(parts[1]);
            int counter = 0;
            for (int j = 0; j < hashes.length; j++) {
                if (j == I) {
                    continue;
                }
                String key = "" + Math.min(I, j) + "," + Math.max(I, j);
                int finalJ = j;
                Integer distance = distancesCache.computeIfAbsent(
                        key, k -> hammingDistance(hashes[I], hashes[finalJ])
                );
                if (distance <= K) {
                    counter++;
                }
            }
            results[i] = counter;
        }
        return results;
    }

    private static int hammingDistance(int[] h1, int[] h2) {
        int counter = 0;
        if (h1.length != h2.length) {
            throw new RuntimeException("Binaries have different length!");
        }
        for (int i = 0; i < h1.length; i++) {
            if (h1[i] != h2[i]) {
                counter++;
            }
        }
        return counter;
    }

}
