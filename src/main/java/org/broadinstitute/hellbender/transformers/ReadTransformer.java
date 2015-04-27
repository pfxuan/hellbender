package org.broadinstitute.hellbender.transformers;

import org.broadinstitute.hellbender.utils.read.MutableRead;

import java.util.function.UnaryOperator;

@FunctionalInterface
public interface ReadTransformer extends UnaryOperator<MutableRead> {
    //HACK: These methods are a hack to get to get the type system to accept compositions of ReadTransformers.

    @SuppressWarnings("overloads")
    default public ReadTransformer andThen(ReadTransformer after) {
        return UnaryOperator.super.andThen(after)::apply;
    }

    @SuppressWarnings("overloads")
    default public  ReadTransformer compose(ReadTransformer before) {
        return UnaryOperator.super.compose(before)::apply;
    }

    static public ReadTransformer identity(){
        return read -> read;
    }

}
