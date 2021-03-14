import org.apache.commons.codec.digest.DigestUtils;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

public class SimHash {

    private static final DigestUtils DIGEST_UTILS = new DigestUtils("MD5");
    private static final int HASH_BIN_LENGTH = DIGEST_UTILS.getMessageDigest().getDigestLength() * 8;

    public static void main(String[] args) {
        // Linked list: texts -> queries
        List<String[]> inputs = readInput();
        // inputs.remove(0) removes and returns texts and so on...
        String[] hashes = Arrays.stream(inputs.remove(0))
                .map(SimHash::simHash)
                .map(SimHash::hexToBinary)
                .toArray(String[]::new);
        Arrays.stream(processQueries(inputs.remove(0), hashes))
                .forEach(System.out::println);
    }

    private static List<String[]> readInput() {
        List<String[]> inputs = new LinkedList<>();
        String[] texts;
        String[] queries;
        try (Scanner sc = new Scanner(System.in)) {
            int N = Integer.parseInt(sc.nextLine().strip());
            texts = new String[N];
            for (int i = 0; i < N; i++) {
                texts[i] = sc.nextLine().strip();
            }
            int Q = Integer.parseInt(sc.nextLine().strip());
            queries = new String[Q];
            for (int i = 0; i < Q; i++) {
                queries[i] = sc.nextLine().strip();
            }
        }
        inputs.add(texts);
        inputs.add(queries);
        return inputs;
    }

    private static String simHash(String text) {
        int[] sh = new int[HASH_BIN_LENGTH];
        String[] terms = text.split("\\s+");
        for (String term : terms) {
            String[] hashBin = hexToBinary(DIGEST_UTILS.digestAsHex(term)).split("");
            for (int i = 0; i < hashBin.length; i++) {
                if (hashBin[i].equals("1")) {
                    sh[i] += 1;
                } else {
                    sh[i] -= 1;
                }
            }
        }
        for (int i = 0; i < sh.length; i++) {
            sh[i] = sh[i] >= 0 ? 1 : 0;
        }
        String binary = Arrays.stream(sh)
                .mapToObj(String::valueOf)
                .collect(Collectors.joining());
        return addPadding(new BigInteger(binary, 2).toString(16), sh.length);
    }

    // Stackoverflow: https://stackoverflow.com/a/41707271
    private static String hexToBinary(String hex) {
        int len = hex.length() * 4;
        String bin = new BigInteger(hex, 16).toString(2);
        return addPadding(bin, len);
    }

    // Left pad the string result with 0s if converting to BigInteger removes them.
    private static String addPadding(String s, int len) {
        int diff = len - s.length();
        if (diff != 0) {
            String pad = "";
            for (int i = 0; i < diff; ++i) {
                pad = pad.concat("0");
            }
            s = pad.concat(s);
        }
        return s;
    }

    private static int[] processQueries(String[] queries, String[] hashes) {
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
                Integer distance = distancesCache.get(key);
                if (distance == null) {
                    distance = hammingDistance(hashes[I], hashes[j]);
                    distancesCache.put(key, distance);
                }
                if (distance <= K) {
                    counter++;
                }
            }
            results[i] = counter;
        }
        return results;
    }

    private static int hammingDistance(String s1, String s2) {
        int counter = 0;
        if (s1.length() != s2.length()) {
            throw new RuntimeException("Binaries have different length!");
        }
        for (int i = 0; i < s1.length(); i++) {
            if (s1.charAt(i) != s2.charAt(i)) {
                counter++;
            }
        }
        return counter;
    }

}
