package com.emlcompare;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InlineDiffGenerator {

    public static class DiffSegment {
        public enum Type { UNCHANGED, DELETED, INSERTED }

        private final String text;
        private final Type type;

        public DiffSegment(String text, Type type) {
            this.text = text;
            this.type = type;
        }

        public String getText() { return text; }
        public Type getType() { return type; }
    }

    public static class InlineDiffResult {
        private final List<DiffSegment> segments;
        private final boolean hasDifferences;

        public InlineDiffResult(List<DiffSegment> segments, boolean hasDifferences) {
            this.segments = segments;
            this.hasDifferences = hasDifferences;
        }

        public List<DiffSegment> getSegments() { return segments; }
        public boolean hasDifferences() { return hasDifferences; }
    }

    public static InlineDiffResult generateInlineDiff(String original, String revised) {
        if (original == null) original = "";
        if (revised == null) revised = "";

        if (original.equals(revised)) {
            List<DiffSegment> segments = new ArrayList<>();
            segments.add(new DiffSegment(original, DiffSegment.Type.UNCHANGED));
            return new InlineDiffResult(segments, false);
        }

        // Split into words and whitespace
        List<String> originalTokens = tokenize(original);
        List<String> revisedTokens = tokenize(revised);

        // Compute word-level diff using Myers algorithm
        List<DiffSegment> segments = computeWordDiff(originalTokens, revisedTokens);

        return new InlineDiffResult(segments, true);
    }

    private static List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();

        // Split text into lines
        String[] lines = text.split("\r?\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            if (!line.isEmpty()) {
                // Split line by common delimiters but keep longer chunks (10-15 words)
                // This creates much larger, more readable highlight blocks
                String[] sentences = line.split("(?<=[.!?;,])\\s+");

                for (int j = 0; j < sentences.length; j++) {
                    String sentence = sentences[j].trim();
                    if (!sentence.isEmpty()) {
                        // Further split very long sentences into chunks of ~10-15 words
                        String[] words = sentence.split("\\s+");
                        if (words.length <= 15) {
                            // Keep the whole sentence/phrase together
                            tokens.add(sentence);
                        } else {
                            // Break into chunks of 10-15 words
                            int chunkSize = 12;
                            for (int k = 0; k < words.length; k += chunkSize) {
                                int end = Math.min(k + chunkSize, words.length);
                                String chunk = String.join(" ", Arrays.copyOfRange(words, k, end));
                                tokens.add(chunk);
                                if (end < words.length) {
                                    tokens.add(" ");
                                }
                            }
                        }

                        // Add space between sentences if not the last
                        if (j < sentences.length - 1) {
                            tokens.add(" ");
                        }
                    }
                }
            }

            // Add newline separator between lines (except for the last line)
            if (i < lines.length - 1) {
                tokens.add("\n");
            }
        }

        return tokens;
    }

    private static List<DiffSegment> computeWordDiff(List<String> original, List<String> revised) {
        int[][] dp = new int[original.size() + 1][revised.size() + 1];

        // Fill DP table for LCS
        for (int i = 1; i <= original.size(); i++) {
            for (int j = 1; j <= revised.size(); j++) {
                if (original.get(i - 1).equals(revised.get(j - 1))) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }

        // Backtrack to find differences
        List<DiffSegment> segments = new ArrayList<>();
        int i = original.size();
        int j = revised.size();

        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && original.get(i - 1).equals(revised.get(j - 1))) {
                segments.add(0, new DiffSegment(original.get(i - 1), DiffSegment.Type.UNCHANGED));
                i--;
                j--;
            } else if (j > 0 && (i == 0 || dp[i][j - 1] >= dp[i - 1][j])) {
                segments.add(0, new DiffSegment(revised.get(j - 1), DiffSegment.Type.INSERTED));
                j--;
            } else if (i > 0) {
                segments.add(0, new DiffSegment(original.get(i - 1), DiffSegment.Type.DELETED));
                i--;
            }
        }

        // Merge consecutive segments of the same type
        return mergeSegments(segments);
    }

    private static List<DiffSegment> mergeSegments(List<DiffSegment> segments) {
        if (segments.isEmpty()) return segments;

        List<DiffSegment> merged = new ArrayList<>();
        StringBuilder currentText = new StringBuilder();
        DiffSegment.Type currentType = segments.get(0).getType();

        for (DiffSegment segment : segments) {
            if (segment.getType() == currentType) {
                currentText.append(segment.getText());
            } else {
                if (currentText.length() > 0) {
                    merged.add(new DiffSegment(currentText.toString(), currentType));
                }
                currentText = new StringBuilder(segment.getText());
                currentType = segment.getType();
            }
        }

        if (currentText.length() > 0) {
            merged.add(new DiffSegment(currentText.toString(), currentType));
        }

        return merged;
    }
}
