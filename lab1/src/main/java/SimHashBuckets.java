import org.apache.commons.codec.binary.BinaryCodec;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

public class SimHashBuckets {

    private static final DigestUtils DIGEST_UTILS = new DigestUtils("MD5");
    private static final int HASH_BIN_LENGTH = DIGEST_UTILS.getMessageDigest().getDigestLength() * 8;
    private static final int B = 8;
    private static final int R = HASH_BIN_LENGTH / B;

    public static void main(String[] args) {
        // Linked list: texts -> queries
        List<String[]> inputs = readInput();
        // inputs.remove(0) removes and returns texts and so on...
        int[][] hashes = prepareHashes(inputs.remove(0));
        Arrays.stream(processQueries(inputs.remove(0), hashes, lsh(hashes)))
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
        Map<String, char[]> hashBinCache = new HashMap<>();
        for (int i = 0; i < hashes.length; i++) {
            hashes[i] = simHash(texts[i], hashBinCache);
        }
        return hashes;
    }

    private static int[] simHash(String text, Map<String, char[]> hashBinCache) {
        int[] sh = new int[HASH_BIN_LENGTH];
        String[] terms = text.split("\\s+");
        for (String term : terms) {
            char[] hashBinChars = hashBinCache.computeIfAbsent(
                    term, k -> BinaryCodec.toAsciiString(DIGEST_UTILS.digest(term)).toCharArray()
            );
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

    private static Map<Integer, Set<Integer>> lsh(int[][] hashes) {
        Map<Integer, Set<Integer>> candidates = new HashMap<>();
        String[] hashesAsStrings = hashesToStrings(hashes);
        // for each band
        for (int band = 1; band <= B; band++) {
            // buckets for current band
            Map<Integer, Set<Integer>> buckets = new HashMap<>();
            // for each text
            for (int currentTextId = 0; currentTextId < hashes.length; currentTextId++) {
                // calculate band value
                int bandValue = hashToInt(band, hashesAsStrings[currentTextId]);
                // fetch text ids from current bucket based on band value
                Set<Integer> bucketTextIds = buckets.get(bandValue);
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
                buckets.put(bandValue, bucketTextIds);
            }
        }
        return candidates;
    }

    private static String[] hashesToStrings(int[][] hashes) {
        String[] hashesAsStrings = new String[hashes.length];
        for (int i = 0; i < hashesAsStrings.length; i++) {
            hashesAsStrings[i] = Arrays.stream(hashes[i])
                    .mapToObj(String::valueOf)
                    .collect(Collectors.joining());
        }
        return hashesAsStrings;
    }

    private static int hashToInt(int band, String hashString) {
        // band = 1 -> return 0:(R-1) bits
        // band = 2 -> return R:2R-1 bits...
        int startIndex = hashString.length() - R * band;
        int endIndex = startIndex + R;
        return Integer.valueOf(hashString.substring(startIndex, endIndex), 2);
    }

    private static int[] processQueries(String[] queries, int[][] hashes, Map<Integer, Set<Integer>> candidates) {
        int[] results = new int[queries.length];
        Map<String, Integer> distancesCache = new HashMap<>();
        for (int i = 0; i < queries.length; i++) {
            String query = queries[i];
            String[] parts = query.split("\\s+");
            int I = Integer.parseInt(parts[0]);
            int K = Integer.parseInt(parts[1]);
            int counter = 0;
            Set<Integer> candidateIds = candidates.get(I);
            if (candidateIds != null) {
                for (int candidateId : candidateIds) {
                    String key = "" + Math.min(I, candidateId) + "," + Math.max(I, candidateId);
                    Integer distance = distancesCache.computeIfAbsent(
                            key, k -> hammingDistance(hashes[I], hashes[candidateId])
                    );
                    if (distance <= K) {
                        counter++;
                    }
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
