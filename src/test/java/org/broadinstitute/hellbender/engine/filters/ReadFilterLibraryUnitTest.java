package org.broadinstitute.hellbender.engine.filters;

import htsjdk.samtools.*;
import org.broadinstitute.hellbender.utils.QualityUtils;
import org.broadinstitute.hellbender.utils.read.ArtificialReadUtils;
import org.broadinstitute.hellbender.utils.read.MutableRead;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static org.broadinstitute.hellbender.engine.filters.ReadFilterLibrary.*;

/**
 * Tests for the read filter library.
 */
public final class ReadFilterLibraryUnitTest {
    private static final int CHR_COUNT = 1;
    private static final int CHR_START = 1;
    private static final int CHR_SIZE = 1000;
    private static final int GROUP_COUNT = 5;

    private final SAMFileHeader header = ArtificialReadUtils.createArtificialSamHeaderWithGroups(CHR_COUNT, CHR_START, CHR_SIZE, GROUP_COUNT);

    /**
     * Creates a read record.
     *
     * @param cigar the new record CIGAR.
     * @param group the new record group index that must be in the range \
     *              [0,{@link #GROUP_COUNT})
     * @param reference the reference sequence index (0-based)
     * @param start the start position of the read alignment in the reference
     *              (1-based)
     * @return never <code>null</code>
     */
    private MutableRead createRead(final Cigar cigar, final int group, final int reference, final int start) {
        final MutableRead record = ArtificialReadUtils.createArtificialRead(header, cigar);
        record.setPosition(header.getSequence(reference).getSequenceName(), start);
        record.setReadGroup(header.getReadGroups().get(group).getReadGroupId());
        return record;

    }

    @Test
    public void testCheckSeqStored() {
        final MutableRead goodRead = ArtificialReadUtils.createArtificialRead(new byte[]{(byte) 'A'}, new byte[]{(byte) 'A'}, "1M");
        final MutableRead badRead = ArtificialReadUtils.createArtificialRead(new byte[]{}, new byte[]{}, "1M");
        badRead.setBases(new byte[0]);

        Assert.assertTrue(SEQ_IS_STORED.test(goodRead));
        Assert.assertFalse(SEQ_IS_STORED.test(badRead));
    }

    @Test(dataProvider = "UnsupportedCigarOperatorDataProvider")
    public void testCigarNOperatorFilter(String cigarString) {

        final ReadFilter filter = ReadFilterLibrary.WELLFORMED;
        final MutableRead read = buildMutableRead(cigarString);
        final boolean containsN = cigarString.contains("N");
        Assert.assertEquals(containsN, !filter.test(read), cigarString);
    }

    protected MutableRead buildMutableRead(final String cigarString) {
        final Cigar nContainingCigar = TextCigarCodec.decode(cigarString);
        return this.createRead(nContainingCigar, 1, 0, 10);
    }


    @DataProvider(name = "UnsupportedCigarOperatorDataProvider")
    public Iterator<Object[]> unsupportedOperatorDataProvider(final Method testMethod) {
        /**
         * Cigar test data for unsupported operator test.
         * Each element of this array corresponds to a test case. In turn the first element of the test case array is the
         * Cigar string for that test case and the second indicates whether it should be filtered due to the presence of a
         * unsupported operator
         */
        final String[] TEST_CIGARS = {
                "101M10D20I10M",
                "6M14N5M",
                "1N",
                "101M",
                "110N",
                "2N4M",
                "4M2N",
                "3M1I1M",
                "1M2I2M",
                "1M10N1I1M",
                "1M1I1D",
                "11N12M1I34M12N"
        };
        final List<Object[]> result = new LinkedList<>();
        for (final String cigarString : TEST_CIGARS) {
            result.add(new Object[]{cigarString});
        }
        return result.iterator();
    }

    private MutableRead simpleGoodRead() {
        String cigarString = "101M";
        final Cigar nContainingCigar = TextCigarCodec.decode(cigarString);
        MutableRead read = createRead(nContainingCigar, 1, 0, 10);
        read.setMappingQuality(50);
        return read;
    }

    @Test
    public void passesAllFilters() {
        MutableRead read = simpleGoodRead();
        Assert.assertTrue(MAPPED.test(read), "MAPPED " + read.toString());
        Assert.assertTrue(PRIMARY_ALIGNMENT.test(read), "PRIMARY_ALIGNMENT " + read.toString());
        Assert.assertTrue(NOT_DUPLICATE.test(read), "NOT_DUPLICATE " + read.toString());
        Assert.assertTrue(PASSES_VENDOR_QUALITY_CHECK.test(read), "PASSES_VENDOR_QUALITY_CHECK " + read.toString());
        Assert.assertTrue(MAPPING_QUALITY_AVAILABLE.test(read), "MAPPING_QUALITY_AVAILABLE " + read.toString());
        Assert.assertTrue(MAPPING_QUALITY_NOT_ZERO.test(read), "MAPPING_QUALITY_NOT_ZERO " + read.toString());
        Assert.assertTrue(VALID_ALIGNMENT_START.test(read), "VALID_ALIGNMENT_START " + read.toString());
        Assert.assertTrue(VALID_ALIGNMENT_END.test(read), "VALID_ALIGNMENT_END " + read.toString());
        Assert.assertTrue(HAS_READ_GROUP.test(read), "HAS_READ_GROUP " + read.toString());
        Assert.assertTrue(HAS_MATCHING_BASES_AND_QUALS.test(read), "HAS_MATCHING_BASES_AND_QUALS " + read.toString());
        Assert.assertTrue(SEQ_IS_STORED.test(read), "SEQ_IS_STORED " + read.toString());
        Assert.assertTrue(CIGAR_IS_SUPPORTED.test(read), "CIGAR_IS_SUPPORTED " + read.toString());

        Assert.assertTrue(WELLFORMED.test(read), "WELLFORMED " + read.toString());
    }

    @Test
    public void failsMAPPED_flag() {
        MutableRead read = simpleGoodRead();
        read.setIsUnmapped();
        Assert.assertFalse(MAPPED.test(read), read.toString());
    }

    @Test
    public void failsMAPPED_alignmentStart() {
        MutableRead read = simpleGoodRead();
        read.setPosition(read.getContig(), 0);
        Assert.assertFalse(MAPPED.test(read), read.toString());
    }

    @Test
    public void failsNOT_DUPLICATE() {
        MutableRead read = simpleGoodRead();
        read.setIsDuplicate(true);
        Assert.assertFalse(NOT_DUPLICATE.test(read), read.toString());
    }

    @Test
    public void failsPASSES_VENDOR_QUALITY_CHECK() {
        MutableRead read = simpleGoodRead();
        read.setFailsVendorQualityCheck(true);
        Assert.assertFalse(PASSES_VENDOR_QUALITY_CHECK.test(read), read.toString());
    }

    @Test
    public void failsMAPPING_QUALITY_AVAILABLE() {
        MutableRead read = simpleGoodRead();
        read.setMappingQuality(QualityUtils.MAPPING_QUALITY_UNAVAILABLE);
        Assert.assertFalse(MAPPING_QUALITY_AVAILABLE.test(read), read.toString());
    }

    @Test
    public void failsMAPPING_QUALITY_NOT_ZERO() {
        MutableRead read = simpleGoodRead();
        read.setMappingQuality(0);
        Assert.assertFalse(MAPPING_QUALITY_NOT_ZERO.test(read), read.toString());
    }

    @Test
    public void failsVALID_ALIGNMENT_START_case1() {
        MutableRead read = simpleGoodRead();
        read.setPosition(read.getContig(), 0);
        Assert.assertFalse(VALID_ALIGNMENT_START.test(read), read.toString());
    }

    @Test
    public void failsVALID_ALIGNMENT_START_case2() {
        MutableRead read = simpleGoodRead();
        read.setPosition(read.getContig(), -1);
        Assert.assertFalse(VALID_ALIGNMENT_START.test(read), read.toString());
    }

    @Test
    public void failsHAS_READ_GROUP() {
        MutableRead read = simpleGoodRead();
        read.setReadGroup(null);
        Assert.assertFalse(HAS_READ_GROUP.test(read), read.toString());
    }

    @Test
    public void failsHAS_MATCHING_BASES_AND_QUALS() {
        MutableRead read = simpleGoodRead();
        read.setBaseQualities(new byte[]{1,2,3});
        Assert.assertFalse(HAS_MATCHING_BASES_AND_QUALS.test(read), read.toString());
    }

    @Test
    public void failsSEQ_IS_STORED() {
        MutableRead read = simpleGoodRead();
        read.setBases(new byte[0]);
        Assert.assertFalse(SEQ_IS_STORED.test(read), read.toString());
    }

    @Test
    public void failsCIGAR_IS_SUPPORTED() {
        MutableRead read = simpleGoodRead();
        read.setCigar("10M2N10M");
        Assert.assertFalse(CIGAR_IS_SUPPORTED.test(read), read.toString());
    }

}
