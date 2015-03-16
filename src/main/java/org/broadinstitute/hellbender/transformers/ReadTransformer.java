package org.broadinstitute.hellbender.transformers;

import com.google.cloud.dataflow.sdk.transforms.SerializableFunction;
import htsjdk.samtools.SAMRecord;

import java.io.Serializable;
import java.util.Objects;
import java.util.function.UnaryOperator;

@FunctionalInterface
public interface ReadTransformer extends UnaryOperator<SAMRecord>, SerializableFunction<SAMRecord, SAMRecord>{
    //HACK: These methods are a hack to get to get the type system to accept compositions of ReadTransformers.

    @SuppressWarnings("overloads")
    default ReadTransformer andThen(ReadTransformer after) {
        Objects.requireNonNull(after);
        return (SAMRecord r) -> after.apply(apply(r));
    }

    @SuppressWarnings("overloads")
    default ReadTransformer compose(ReadTransformer before) {
        Objects.requireNonNull(before);
        return (SAMRecord r) -> apply(before.apply(r));
    }

    static ReadTransformer identity(){
        return read -> read;
    }

}
