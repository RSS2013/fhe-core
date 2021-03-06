package com.kryptnostic.crypto;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import cern.colt.bitvector.BitVector;

import com.kryptnostic.bitwise.BitVectors;
import com.kryptnostic.linear.EnhancedBitMatrix;
import com.kryptnostic.linear.EnhancedBitMatrix.SingularMatrixException;
import com.kryptnostic.multivariate.gf2.SimplePolynomialFunction;
import com.kryptnostic.multivariate.util.SimplePolynomialFunctions;

public class EncryptedSearchPrivateKeyTests {
    private static EncryptedSearchPrivateKey privateKey;
    private static PrivateKey                fhePrivateKey;
    private static PublicKey                 fhePublicKey;
    private static SimplePolynomialFunction  globalHash;

    @BeforeClass
    public static void generateKey() throws SingularMatrixException {
        fhePrivateKey = new PrivateKey( 128, 64 );
        fhePublicKey = new PublicKey( fhePrivateKey );
        privateKey = new EncryptedSearchPrivateKey( 8 );
        globalHash = SimplePolynomialFunctions.denseRandomMultivariateQuadratic(
                EncryptedSearchPrivateKey.getHashBits(),
                EncryptedSearchPrivateKey.getHashBits() >>> 1 );
    }

    @Test
    public void testQueryGeneration() throws SingularMatrixException {
        String term = "risefall";
        BitVector expected = privateKey.hash( term );
        BitVector intermediate = privateKey.prepareSearchToken( fhePublicKey , term );
        BitVector actual = fhePrivateKey.getDecryptor().apply( intermediate );

        Assert.assertEquals( expected, actual );
    }

    @Test
    public void testQueryHasherPairGeneration() throws SingularMatrixException {
        String term = "barbarian";

        BitVector searchHash = privateKey.hash( term );
        BitVector encryptedSearchHash = privateKey.prepareSearchToken(fhePublicKey, term );

        BitVector searchNonce = BitVectors.randomVector( 64 );
        BitVector encryptedSearchNonce = fhePublicKey.getEncrypter().apply(
                BitVectors.concatenate( searchNonce, BitVectors.randomVector( 64 ) ) );

        EnhancedBitMatrix expectedMatrix = EnhancedBitMatrix.squareMatrixfromBitVector( globalHash.apply( BitVectors
                .concatenate( searchHash, searchNonce ) ) );
        BitVector expected = BitVectors.fromMatrix( expectedMatrix.multiply( expectedMatrix ) );

        Pair<SimplePolynomialFunction, SimplePolynomialFunction> p = privateKey.getQueryHasherPair(
                globalHash,
                fhePrivateKey );
        SimplePolynomialFunction hL = p.getLeft();
        SimplePolynomialFunction hR = p.getRight();

        EnhancedBitMatrix intermediateL = EnhancedBitMatrix.squareMatrixfromBitVector( hL.apply( BitVectors
                .concatenate( encryptedSearchHash, encryptedSearchNonce ) ) );
        EnhancedBitMatrix intermediateR = EnhancedBitMatrix.squareMatrixfromBitVector( hR.apply( BitVectors
                .concatenate( encryptedSearchHash, encryptedSearchNonce ) ) );

        EnhancedBitMatrix documentKey = privateKey.newDocumentKey();
        EncryptedSearchSharingKey sharingKey = new EncryptedSearchSharingKey( documentKey );
        EncryptedSearchBridgeKey bridgeKey = new EncryptedSearchBridgeKey( privateKey, sharingKey );

        BitVector actual = BitVectors.fromSquareMatrix( intermediateL.multiply(
                privateKey.getLeftSquaringMatrix().inverse() ).multiply(
                privateKey.getRightSquaringMatrix().inverse().multiply( intermediateR ) ) );
        Assert.assertEquals( expected, actual );

        // Now let's test running a search
        actual = BitVectors
                .fromSquareMatrix( intermediateL.multiply( bridgeKey.getBridge() ).multiply( intermediateR ) );
        expectedMatrix = EnhancedBitMatrix.squareMatrixfromBitVector( globalHash.apply( BitVectors.concatenate(
                searchHash,
                searchNonce ) ) );
        expected = BitVectors.fromSquareMatrix( expectedMatrix.multiply( sharingKey.getMiddle() ).multiply(
                expectedMatrix ) );
        Assert.assertEquals( expected, actual );
    }

}
