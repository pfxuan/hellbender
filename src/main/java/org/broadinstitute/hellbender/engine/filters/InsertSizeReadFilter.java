package org.broadinstitute.hellbender.engine.filters;

import htsjdk.samtools.SAMRecord;
import org.broadinstitute.hellbender.cmdline.Argument;

/**
 * Keep reads that are within a given max insert size.
 */
public final class InsertSizeReadFilter implements ReadFilter {
    @Argument(fullName = "maxInsertSize", shortName = "maxInsert", doc="Keep reads with insert size within than the specified value", optional=true)
    public int maxInsertSize = 1000000;

    @Override
    public boolean test(final SAMRecord read) {
        if (!read.getReadPairedFlag()) {
            return true;
        }
        //Note insert size is negative if mate maps to lower position than read so we take absolute value.
        return Math.abs(read.getInferredInsertSize()) <= maxInsertSize;
    }
}
