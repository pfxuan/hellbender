package org.broadinstitute.hellbender.engine.filters;

import org.broadinstitute.hellbender.utils.read.Read;

import java.util.function.Predicate;

@FunctionalInterface
public interface ReadFilter extends Predicate<Read> {

    //HACK: These methods are a hack to get to get the type system to accept compositions of ReadFilters.
    default ReadFilter and(ReadFilter filter ) {
        return Predicate.super.and(filter)::test;
    }

    default ReadFilter or(ReadFilter filter ) {
        return Predicate.super.or(filter)::test;
    }

    default ReadFilter negate(){
        return Predicate.super.negate()::test;
    }
}
