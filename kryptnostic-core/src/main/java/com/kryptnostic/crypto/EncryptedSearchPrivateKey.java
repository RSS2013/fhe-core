package com.kryptnostic.crypto;

import org.apache.commons.lang3.tuple.Pair;

import cern.colt.bitvector.BitVector;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Charsets;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.kryptnostic.bitwise.BitVectors;
import com.kryptnostic.linear.EnhancedBitMatrix;
import com.kryptnostic.linear.EnhancedBitMatrix.SingularMatrixException;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;
import com.kryptnostic.multivariate.polynomial.OptimizedPolynomialFunctionGF2;
import com.kryptnostic.multivariate.polynomial.ParameterizedPolynomialFunctionGF2;

public class EncryptedSearchPrivateKey {
    public static final String        HASH_COLLAPSER        = "hashCollapser";
    public static final String        LEFT_SQUARING_MATRIX  = "leftMatrix";
    public static final String        RIGHT_SQUARING_MATRIX = "rightMatrix";
    private static final HashFunction hf                    = Hashing.murmur3_128();
    private static final int          hashBits              = hf.bits();

    private final EnhancedBitMatrix   leftSquaringMatrix, rightSquaringMatrix;

    @JsonCreator
    public EncryptedSearchPrivateKey(
            @JsonProperty( LEFT_SQUARING_MATRIX ) EnhancedBitMatrix leftSquaringMatrix,
            @JsonProperty( RIGHT_SQUARING_MATRIX ) EnhancedBitMatrix rightSquaringMatrix ) throws SingularMatrixException {
        this.leftSquaringMatrix = leftSquaringMatrix;
        this.rightSquaringMatrix = rightSquaringMatrix;
    }

    public EncryptedSearchPrivateKey( int sqrRootHashLength ) throws SingularMatrixException {
        this.leftSquaringMatrix = EnhancedBitMatrix.randomInvertibleMatrix( sqrRootHashLength );
        this.rightSquaringMatrix = EnhancedBitMatrix.randomInvertibleMatrix( sqrRootHashLength );
    }

    /**
     * Generates a search token by computing the hash of the provided term and encrypting its hash.
     * 
     * @param term
     * @return A bitvector search token generated by encrypted the hash of the search term.
     */
    public BitVector prepareSearchToken( PublicKey publicKey, String term ) {
        BitVector searchHash = hash( term );
        return publicKey.getEncrypter().apply(
                BitVectors.concatenate( searchHash, BitVectors.randomVector( searchHash.size() ) ) );
    }

    public BitVector hash( String term ) {
        BitVector hash = BitVectors.fromBytes( hashBits, hf.hashString( term, Charsets.UTF_8 ).asBytes() );
        BitVector halfHash = hash.partFromTo( 0, 63 );
        halfHash.xor( hash.partFromTo( 64, 127 ) );
        return halfHash;
    }

    @JsonProperty( LEFT_SQUARING_MATRIX )
    public EnhancedBitMatrix getLeftSquaringMatrix() {
        return leftSquaringMatrix;
    }

    @JsonProperty( RIGHT_SQUARING_MATRIX )
    public EnhancedBitMatrix getRightSquaringMatrix() {
        return rightSquaringMatrix;
    }

    public EnhancedBitMatrix newDocumentKey() {
        return EnhancedBitMatrix.randomInvertibleMatrix( 8 );
    }

    public Pair<SimplePolynomialFunction, SimplePolynomialFunction> getQueryHasherPair(
            SimplePolynomialFunction globalHash,
            PrivateKey privateKey ) throws SingularMatrixException {
        SimplePolynomialFunction hashOfDecryptor = globalHash.compose( privateKey.getMirroredDecryptor() );
        return Pair.of(
                rightMultiply( hashOfDecryptor, leftSquaringMatrix ),
                leftMultiply( hashOfDecryptor, rightSquaringMatrix ) );
    }

    public static SimplePolynomialFunction rightMultiply( SimplePolynomialFunction f, EnhancedBitMatrix rhs ) {
        BitVector[] contributions = f.getContributions();
        BitVector[] newContributions = new BitVector[ contributions.length ];

        for ( int i = 0; i < contributions.length; ++i ) {
            newContributions[ i ] = BitVectors.fromSquareMatrix( EnhancedBitMatrix.squareMatrixfromBitVector(
                    contributions[ i ] ).multiply( rhs ) );
        }

        if ( f.getClass().equals( ParameterizedPolynomialFunctionGF2.class ) ) {
            ParameterizedPolynomialFunctionGF2 g = (ParameterizedPolynomialFunctionGF2) f;
            return new ParameterizedPolynomialFunctionGF2(
                    g.getInputLength(),
                    newContributions[ 0 ].size(),
                    g.getMonomials(),
                    newContributions,
                    g.getPipelines() );
        } else {
            return new OptimizedPolynomialFunctionGF2(
                    f.getInputLength(),
                    newContributions[ 0 ].size(),
                    f.getMonomials(),
                    newContributions );
        }
    }

    public static SimplePolynomialFunction leftMultiply( SimplePolynomialFunction f, EnhancedBitMatrix lhs ) {
        BitVector[] contributions = f.getContributions();
        BitVector[] newContributions = new BitVector[ contributions.length ];

        for ( int i = 0; i < contributions.length; ++i ) {
            newContributions[ i ] = BitVectors.fromSquareMatrix( lhs.multiply( EnhancedBitMatrix
                    .squareMatrixfromBitVector( contributions[ i ] ) ) );
        }

        if ( f.getClass().equals( ParameterizedPolynomialFunctionGF2.class ) ) {
            ParameterizedPolynomialFunctionGF2 g = (ParameterizedPolynomialFunctionGF2) f;
            return new ParameterizedPolynomialFunctionGF2(
                    g.getInputLength(),
                    newContributions[ 0 ].size(),
                    g.getMonomials(),
                    newContributions,
                    g.getPipelines() );
        } else {
            return new OptimizedPolynomialFunctionGF2(
                    f.getInputLength(),
                    newContributions[ 0 ].size(),
                    f.getMonomials(),
                    newContributions );
        }
    }

    public static SimplePolynomialFunction twoSidedMultiply(
            SimplePolynomialFunction f,
            EnhancedBitMatrix lhs,
            EnhancedBitMatrix rhs ) {
        BitVector[] contributions = f.getContributions();
        BitVector[] newContributions = new BitVector[ contributions.length ];

        for ( int i = 0; i < contributions.length; ++i ) {
            newContributions[ i ] = BitVectors.fromSquareMatrix( lhs.multiply(
                    EnhancedBitMatrix.squareMatrixfromBitVector( contributions[ i ] ) ).multiply( rhs ) );
        }

        if ( f.getClass().equals( ParameterizedPolynomialFunctionGF2.class ) ) {
            ParameterizedPolynomialFunctionGF2 g = (ParameterizedPolynomialFunctionGF2) f;
            return new ParameterizedPolynomialFunctionGF2(
                    g.getInputLength(),
                    newContributions[ 0 ].size(),
                    g.getMonomials(),
                    newContributions,
                    g.getPipelines() );
        } else {
            return new OptimizedPolynomialFunctionGF2(
                    f.getInputLength(),
                    newContributions[ 0 ].size(),
                    f.getMonomials(),
                    newContributions );
        }
    }

    public static int getHashBits() {
        return hashBits;
    }

}
