package kz.fahrentech.unraidtelegrambot.docker;

import java.nio.charset.StandardCharsets;

final class BoundedOutput {

    private final int maxChars;
    private final StringBuilder output = new StringBuilder();
    private boolean truncated;

    BoundedOutput(int maxChars) {
        this.maxChars = Math.max(1, maxChars);
    }

    void append(byte[] payload) {
        if (payload == null || payload.length == 0 || output.length() >= maxChars) {
            truncated = truncated || output.length() >= maxChars;
            return;
        }

        String chunk = new String(payload, StandardCharsets.UTF_8);
        int remaining = maxChars - output.length();
        if (chunk.length() <= remaining) {
            output.append(chunk);
            return;
        }

        output.append(chunk, 0, remaining);
        truncated = true;
    }

    String value() {
        return output.toString();
    }

    boolean truncated() {
        return truncated;
    }
}
