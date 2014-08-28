package com.kryptnostic.bitwise;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;

import org.apache.commons.codec.binary.Base64;

import cern.colt.bitvector.BitVector;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;

public final class BitVectors {
    private static final int INTEGER_BYTES = Integer.SIZE / Byte.SIZE;
    private static final Base64 codec = new Base64();

    private BitVectors() {
    }

    private final static Function<BitVector, BitVector> cloner = new Function<BitVector, BitVector>() {
        @Override
        public BitVector apply(BitVector input) {
            return input.copy();
        }
    };

    /**
     * Returns an iterable that allow lazy evaluation of the cloning functions for efficient use of Guava collection
     * factory methods.
     * 
     * @param vectors
     * @return
     */
    public static Iterable<BitVector> cloneToIterable(BitVector... vectors) {
        return Iterables.transform(Arrays.asList(vectors), cloner);
    }

    /**
     * Performs a deep clone of Monomial class.
     * 
     * @param vectors
     *            to clone
     * @return An array of monomials generated by calling {@link BitVector#clone()} on each element in {@code monomials}
     */
    public static BitVector[] deepCloneBitvectorArray(BitVector... vectors) {
        BitVector[] copies = new BitVector[vectors.length];
        for (int i = 0; i < vectors.length; ++i) {
            copies[i] = vectors[i].copy();
        }
        return copies;
    }

    /**
     * Turns a BitVector into a Base64 encoded string
     * 
     * @param input
     *            BitVector to be marshaled
     * @return Base64 encoded byte array in the following format: { bit_vector_size:int, bit_vector_bits[0]:long,
     *         bit_vector_bits[1]:long, ..., bit_vector_bits[n]:long }
     */
    public static String marshalBitvector(BitVector input) {
        if (input == null) {
            return null;
        }
        long[] data = input.elements();
        byte[] target = new byte[(data.length << 3) + INTEGER_BYTES];
        ByteBuffer buf = ByteBuffer.wrap(target);
        buf.putInt(input.size());
        buf.asLongBuffer().put(data);
        return new String(codec.encode(target));
    }

    /**
     * Creates a BitVector from a Base64 encoded string
     * 
     * @param input
     *            Base64 encoded string of a byte array in the following format: { bit_vector_size:int,
     *            bit_vector_bits[0]:long, bit_vector_bits[1]:long, ..., bit_vector_bits[n]:long }
     * @return The unmarshaled BitVector
     */
    public static BitVector unmarshalBitvector(String input) {
        if (input == null) {
            return null;
        }
        byte[] decoded = Base64.decodeBase64(input.getBytes());
        ByteBuffer buf = ByteBuffer.wrap(decoded);
        int size = buf.getInt();
        LongBuffer longBuffer = buf.asLongBuffer();
        long[] longs = new long[(decoded.length - INTEGER_BYTES) >>> 3];
        longBuffer.get(longs);
        return new BitVector(longs, size);
    }

}
