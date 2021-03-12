import org.apache.commons.codec.digest.DigestUtils;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

public class SimHashBuckets {

    private static final DigestUtils DIGEST_UTILS = new DigestUtils("MD5");
    private static final int HASH_BIN_LENGTH = DIGEST_UTILS.getMessageDigest().getDigestLength() * 8;
    private static final int B = 8;
    private static final int R = HASH_BIN_LENGTH / B;

    private static String[] texts;
    private static String[] queries;
    private static String[] hashesHex;
    private static String[] hashesBin;
    private static Map<Integer, Set<Integer>> candidates;

    public static void main(String[] args) {
        readInput();
        hashesHex = Arrays.stream(texts)
                .map(SimHashBuckets::simHash)
                .toArray(String[]::new);
        hashesBin = Arrays.stream(hashesHex)
                .map(SimHashBuckets::hexToBinary)
                .toArray(String[]::new);
        lsh();
        String results = processQueries();
        System.out.println(results);
    }

    private static void readInput() {
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

    private static void lsh() {
        candidates = new HashMap<>();
        // for each region
        for (int region = 1; region <= B; region++) {
            // buckets for current region
            Map<Integer, Set<Integer>> buckets = new HashMap<>();
            // for each text
            for (int currentTextId = 0; currentTextId < texts.length; currentTextId++) {
                String hashBin = hashesBin[currentTextId];
                // calculate region value
                int regionValue = hashToInt(region, hashBin);
                // fetch text ids from current bucket based on region value
                Set<Integer> bucketTextIds = buckets.get(regionValue);
                if (bucketTextIds != null) {
                    // there are some text ids in current bucket,
                    // so update candidates map
                    for (int textId : bucketTextIds) {
                        candidates.merge(currentTextId, new HashSet<>() {{
                                    add(textId);
                                }},
                                (oldValue, newValue) -> {
                                    oldValue.add(textId);
                                    return oldValue;
                                });
                        int finalCurrentTextId = currentTextId;
                        candidates.merge(textId, new HashSet<>() {{
                                    add(finalCurrentTextId);
                                }},
                                (oldValue,
                                 newValue) -> {
                                    oldValue.add(finalCurrentTextId);
                                    return oldValue;
                                });
                    }
                } else {
                    // there are no text ids in current bucket,
                    // so create a new bucket of text ids
                    bucketTextIds = new HashSet<>();
                }
                // add current text id to current bucket of text ids
                bucketTextIds.add(currentTextId);
                // update buckets
                buckets.put(regionValue, bucketTextIds);
            }
        }
    }

    private static int hashToInt(int region, String hashBin) {
        // region = 1 -> return 0:(R-1) bits
        // region = 2 -> return R:2R-1 bits...
        int startIndex = hashBin.length() - R * region;
        int endIndex = startIndex + R;
        return Integer.valueOf(hashBin.substring(startIndex, endIndex), 2);
    }

    private static String processQueries() {
        StringJoiner sj = new StringJoiner(System.lineSeparator());
        for (String query : queries) {
            String[] parts = query.split("\\s+");
            int I = Integer.parseInt(parts[0]);
            int K = Integer.parseInt(parts[1]);
            int counter = 0;
            for (int candidateId : candidates.get(I)) {
                int distance = hammingDistance(hashesBin[I], hashesBin[candidateId]);
                if (distance <= K) {
                    counter++;
                }
            }
            sj.add(String.valueOf(counter));
        }
        return sj.toString();
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
