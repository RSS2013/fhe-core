package com.kryptnostic.multivariate.gf2;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.base.Function;

import cern.colt.bitvector.BitVector;


/**
 * A polynomial function that can be evaluated with either single argument of input length 
 * or two arguments of input length / 2. 
 * @author Matthew Tamayo-Rios
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public interface PolynomialFunction extends Function<BitVector,BitVector> {
    public abstract BitVector apply( BitVector lhs , BitVector rhs );
    public abstract int getInputLength();
    public abstract int getOutputLength();
}
