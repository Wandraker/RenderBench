package dev.onelsey.renderbench.benchmark;

import java.util.Arrays;

public final class FrameSeries {
    private long[] values = new long[8192];
    private int size;

    public void add(long nanos) {
        if (nanos <= 0) return;
        if (size == values.length) {
            values = Arrays.copyOf(values, values.length * 2);
        }
        values[size++] = nanos;
    }

    public int size() {
        return size;
    }

    public long[] copy() {
        return Arrays.copyOf(values, size);
    }

    public void clear() {
        size = 0;
    }
}
