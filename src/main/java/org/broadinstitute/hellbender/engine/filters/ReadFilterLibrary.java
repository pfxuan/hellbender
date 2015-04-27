package org.broadinstitute.hellbender.engine.filters;

import org.broadinstitute.hellbender.utils.QualityUtils;
import org.broadinstitute.hellbender.utils.read.CigarUtils;

/**
 * Standard ReadFilters
 */
public final class ReadFilterLibrary {
    private ReadFilterLibrary(){ /*no instance*/ }

    public static final ReadFilter ALLOW_ALL_READS = read -> true;

    public static final ReadFilter MAPPED =  read -> !read.isUnmapped();
    public static final ReadFilter PRIMARY_ALIGNMENT = read -> !read.isNonPrimaryAlignment();
    public static final ReadFilter NOT_DUPLICATE = read -> !read.isDuplicate();
    public static final ReadFilter PASSES_VENDOR_QUALITY_CHECK = read -> !read.failsVendorQualityCheck();
    public static final ReadFilter MAPPING_QUALITY_AVAILABLE = read -> read.getMappingQuality() != QualityUtils.MAPPING_QUALITY_UNAVAILABLE;
    public static final ReadFilter MAPPING_QUALITY_NOT_ZERO = read -> read.getMappingQuality() != 0;

    public static final ReadFilter VALID_ALIGNMENT_START = read -> read.isUnmapped() || read.getStart() > 0;

    // Alignment aligns to negative number of bases in the reference.
    //Note: there seems to be no way a SAMRecord can fail this filter but we'll keep it because another read implementation may.
    public static final ReadFilter VALID_ALIGNMENT_END = read -> read.isUnmapped() || read.getEnd() == -1 || (read.getEnd() - read.getStart() + 1) >= 0;

    public static final ReadFilter HAS_READ_GROUP = read -> read.getReadGroup() != null;
    public static final ReadFilter HAS_MATCHING_BASES_AND_QUALS = read -> read.getLength() == read.getBaseQualities().length;
    public static final ReadFilter SEQ_IS_STORED = read -> read.getBases().length > 0;
    public static final ReadFilter CIGAR_IS_SUPPORTED = read -> read.getCigar() == null || !CigarUtils.containsNOperator(read.getCigar());

    public static final ReadFilter WELLFORMED =
            VALID_ALIGNMENT_START
            .and(VALID_ALIGNMENT_END)
            .and(HAS_READ_GROUP)
            .and(HAS_MATCHING_BASES_AND_QUALS)
            .and(SEQ_IS_STORED)
            .and(CIGAR_IS_SUPPORTED);
}
