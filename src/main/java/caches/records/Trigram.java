package caches.records;

import java.util.Arrays;
import java.util.Objects;

public record Trigram(byte[] trigram) implements Comparable<Trigram> {

    public Trigram(int i) {
        this((long) i);
    }

    public Trigram(long l) {
        this(new byte[] {(byte) (l >> 16), (byte) (l >> 8), (byte) l});

    }

    public static long toLong(byte[] bytes) {
        return toInt(bytes);
    }

    public static int toInt(byte[] bytes) {
        return ((((((int) bytes[0]) << 8) + bytes[1])) << 8) + bytes[2];
    }

    public int toInt() {
        return toInt(trigram);
    }

    public long toLong() {
        return toLong(trigram);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Trigram trigram1 = (Trigram) o;
        return Arrays.equals(trigram, trigram1.trigram);
    }

    @Override
    public int hashCode() {
        return Objects.hash(trigram[0], trigram[1], trigram[2]);
    }

    @Override
    public int compareTo(Trigram o) {
        return Integer.compare(toInt(trigram), toInt(o.trigram));
    }

    @Override
    public String toString() {
        return "Trigram" + Arrays.toString(trigram);
    }
}
