import org.apache.commons.codec.digest.DigestUtils;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Scanner;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class SimHash {

    private static final DigestUtils DIGEST_UTILS = new DigestUtils("MD5");

    private static String[] texts;
    private static String[] queries;
    private static String[] hashesHex;
    private static String[] hashesBin;

    public static void main(String[] args) {
        readInput();
        hashesHex = Arrays.stream(texts)
                .map(SimHash::simHash)
                .toArray(String[]::new);
        hashesBin = Arrays.stream(hashesHex)
                .map(SimHash::hexToBinary)
                .toArray(String[]::new);
        String results = processQueries();
        System.out.println(results);
    }

    private static void readInput() {
        try (Scanner sc = new Scanner(System.in)) {
            int N = Integer.parseInt(sc.nextLine());
            texts = new String[N];
            for (int i = 0; i < N; i++) {
                texts[i] = sc.nextLine();
            }
            int Q = Integer.parseInt(sc.nextLine());
            queries = new String[Q];
            for (int i = 0; i < Q; i++) {
                queries[i] = sc.nextLine();
            }
        }
    }

    private static String simHash(String text) {
        int[] sh = new int[DIGEST_UTILS.getMessageDigest().getDigestLength() * 8];
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

    private static String processQueries() {
        StringJoiner sj = new StringJoiner(System.lineSeparator());
        for (String query : queries) {
            String[] parts = query.split("\\s+");
            int I = Integer.parseInt(parts[0]);
            int K = Integer.parseInt(parts[1]);
            int counter = 0;
            for (int i = 0; i < hashesBin.length; i++) {
                if (i == I) {
                    continue;
                }
                int distance = hammingDistance(hashesBin[I], hashesBin[i]);
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
