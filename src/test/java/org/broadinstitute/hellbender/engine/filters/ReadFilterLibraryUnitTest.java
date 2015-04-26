package org.broadinstitute.hellbender.engine.filters;

import htsjdk.samtools.*;
import org.broadinstitute.hellbender.utils.QualityUtils;
import org.broadinstitute.hellbender.utils.read.ArtificialSAMUtils;
import org.broadinstitute.hellbender.utils.read.ReadUtils;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.util.*;

import static org.broadinstitute.hellbender.engine.filters.ReadFilterLibrary.*;

/**
 * Tests for the read filter library.
 */
public final class ReadFilterLibraryUnitTest {
    private static final int CHR_COUNT = 1;
    private static final int CHR_START = 1;
    private static final int CHR_SIZE = 1000;
    private static final int GROUP_COUNT = 5;

    private final SAMFileHeader header = ArtificialSAMUtils.createArtificialSamHeaderWithGroups(CHR_COUNT, CHR_START, CHR_SIZE, GROUP_COUNT);

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
    private SAMRecord createRead(final Cigar cigar, final int group, final int reference, final int start) {
        final SAMRecord record = ArtificialSAMUtils.createArtificialRead(cigar);
        record.setHeader(header);
        record.setAlignmentStart(start);
        record.setReferenceIndex(reference);
        record.setAttribute(SAMTag.RG.toString(), header.getReadGroups().get(group).getReadGroupId());
        return record;
    }

    @Test
    public void testCheckSeqStored() {
        final SAMRecord goodRead = ArtificialSAMUtils.createArtificialRead(new byte[]{(byte) 'A'}, new byte[]{(byte) 'A'}, "1M");
        final SAMRecord badRead = ArtificialSAMUtils.createArtificialRead(new byte[]{}, new byte[]{}, "1M");
        badRead.setReadString("*");

        Assert.assertTrue(SEQ_IS_STORED.test(goodRead));
        Assert.assertFalse(SEQ_IS_STORED.test(badRead));
    }

    @Test(dataProvider = "UnsupportedCigarOperatorDataProvider")
    public void testCigarNOperatorFilter(String cigarString) {

        final ReadFilter filter = ReadFilterLibrary.WELLFORMED;
        final SAMRecord read = buildSAMRecord(cigarString);
        final boolean containsN = cigarString.contains("N");
        Assert.assertEquals(containsN, !filter.test(read), cigarString);
    }

    private SAMRecord buildSAMRecord(final String cigarString) {
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

    private SAMRecord simpleGoodRead() {
        String cigarString = "101M";
        final Cigar nContainingCigar = TextCigarCodec.decode(cigarString);
        SAMRecord read =  createRead(nContainingCigar, 1, 0, 10);
        read.setMappingQuality(50);
        return read;
    }

    @Test
    public void passesAllFilters() {
        SAMRecord read = simpleGoodRead();
        Assert.assertTrue(MAPPED.test(read), "MAPPED " + read.toString());
        Assert.assertTrue(PRIMARY_ALIGNMENT.test(read), "PRIMARY_ALIGNMENT " + read.toString());
        Assert.assertTrue(NOT_DUPLICATE.test(read), "NOT_DUPLICATE " + read.toString());
        Assert.assertTrue(PASSES_VENDOR_QUALITY_CHECK.test(read), "PASSES_VENDOR_QUALITY_CHECK " + read.toString());
        Assert.assertTrue(MAPPING_QUALITY_AVAILABLE.test(read), "MAPPING_QUALITY_AVAILABLE " + read.toString());
        Assert.assertTrue(MAPPING_QUALITY_NOT_ZERO.test(read), "MAPPING_QUALITY_NOT_ZERO " + read.toString());
        Assert.assertTrue(VALID_ALIGNMENT_START.test(read), "VALID_ALIGNMENT_START " + read.toString());
        Assert.assertTrue(VALID_ALIGNMENT_END.test(read), "VALID_ALIGNMENT_END " + read.toString());
        Assert.assertTrue(ALIGNMENT_AGREES_WITH_HEADER.test(read), "ALIGNMENT_AGREES_WITH_HEADER " + read.toString());
        Assert.assertTrue(HAS_READ_GROUP.test(read), "HAS_READ_GROUP " + read.toString());
        Assert.assertTrue(HAS_MATCHING_BASES_AND_QUALS.test(read), "HAS_MATCHING_BASES_AND_QUALS " + read.toString());
        Assert.assertTrue(SEQ_IS_STORED.test(read), "SEQ_IS_STORED " + read.toString());
        Assert.assertTrue(CIGAR_IS_SUPPORTED.test(read), "CIGAR_IS_SUPPORTED " + read.toString());

        Assert.assertTrue(WELLFORMED.test(read), "WELLFORMED " + read.toString());
    }

    @Test
    public void failsMAPPED_flag() {
        SAMRecord read = simpleGoodRead();
        read.setReadUnmappedFlag(true);
        Assert.assertFalse(MAPPED.test(read), read.toString());
    }

    @Test
    public void failsMAPPED_alignmenStart() {
        SAMRecord read = simpleGoodRead();
        read.setAlignmentStart(SAMRecord.NO_ALIGNMENT_START);
        Assert.assertFalse(MAPPED.test(read), read.toString());
    }

    @Test
    public void failsNOT_DUPLICATE() {
        SAMRecord read = simpleGoodRead();
        read.setDuplicateReadFlag(true);
        Assert.assertFalse(NOT_DUPLICATE.test(read), read.toString());
    }

    @Test
    public void failsPASSES_VENDOR_QUALITY_CHECK() {
        SAMRecord read = simpleGoodRead();
        read.setReadFailsVendorQualityCheckFlag(true);
        Assert.assertFalse(PASSES_VENDOR_QUALITY_CHECK.test(read), read.toString());
    }

    @Test
    public void failsMAPPING_QUALITY_AVAILABLE() {
        SAMRecord read = simpleGoodRead();
        read.setMappingQuality(QualityUtils.MAPPING_QUALITY_UNAVAILABLE);
        Assert.assertFalse(MAPPING_QUALITY_AVAILABLE.test(read), read.toString());
    }

    @Test
    public void failsMAPPING_QUALITY_NOT_ZERO() {
        SAMRecord read = simpleGoodRead();
        read.setMappingQuality(0);
        Assert.assertFalse(MAPPING_QUALITY_NOT_ZERO.test(read), read.toString());
    }

    @Test
    public void failsVALID_ALIGNMENT_START_case1() {
        SAMRecord read = simpleGoodRead();
        read.setAlignmentStart(SAMRecord.NO_ALIGNMENT_START);
        Assert.assertFalse(VALID_ALIGNMENT_START.test(read), read.toString());
    }

    @Test
    public void failsVALID_ALIGNMENT_START_case2() {
        SAMRecord read = simpleGoodRead();
        read.setAlignmentStart(-1);
        Assert.assertFalse(VALID_ALIGNMENT_START.test(read), read.toString());
    }

    @Test
    public void failsALIGNMENT_AGREES_WITH_HEADER_case1() {
        SAMRecord read = simpleGoodRead();
        read.setReferenceIndex(SAMRecord.NO_ALIGNMENT_REFERENCE_INDEX);
        read.setAlignmentStart(10);
        Assert.assertFalse(ALIGNMENT_AGREES_WITH_HEADER.test(read), read.toString());
    }

    @Test
    public void failsALIGNMENT_AGREES_WITH_HEADER_case2() {
        SAMRecord read = simpleGoodRead();
        final int length = read.getHeader().getSequence(0).getSequenceLength();
        read.setAlignmentStart(length + 10);
        Assert.assertFalse(ALIGNMENT_AGREES_WITH_HEADER.test(read), read.toString());
    }

    @Test
    public void failsHAS_READ_GROUP() {
        SAMRecord read = simpleGoodRead();
        read.setAttribute(SAMTag.RG.name(), null);
        Assert.assertFalse(HAS_READ_GROUP.test(read), read.toString());
    }

    @Test
    public void failsHAS_MATCHING_BASES_AND_QUALS() {
        SAMRecord read = simpleGoodRead();
        read.setBaseQualities(new byte[]{1, 2, 3});
        Assert.assertFalse(HAS_MATCHING_BASES_AND_QUALS.test(read), read.toString());
    }

    @Test
    public void failsSEQ_IS_STORED() {
        SAMRecord read = simpleGoodRead();
        read.setReadBases(SAMRecord.NULL_SEQUENCE);
        Assert.assertFalse(SEQ_IS_STORED.test(read), read.toString());
    }

    @Test
    public void failsCIGAR_IS_SUPPORTED() {
        SAMRecord read = simpleGoodRead();
        read.setCigarString("10M2N10M");
        Assert.assertFalse(CIGAR_IS_SUPPORTED.test(read), read.toString());
    }

    @Test
    public void testLibraryReadFilter(){
        SAMRecord read = simpleGoodRead();
        final LibraryReadFilter f = new LibraryReadFilter();

        final String foo = "Foo";
        read.getReadGroup().setLibrary(foo);

        Assert.assertFalse(f.test(read), read.toString());//fail
        f.libraryToKeep = foo;
        Assert.assertTrue(f.test(read), read.toString());//pass
    }

    @Test
    public void testMappingQualityFilter(){
        SAMRecord read = simpleGoodRead();
        final MappingQualityReadFilter f = new MappingQualityReadFilter();

        f.minMappingQualtyScore = 17;
        read.setMappingQuality(11);
        Assert.assertFalse(f.test(read), read.toString());//fail

        f.minMappingQualtyScore = 9;
        Assert.assertTrue(f.test(read), read.toString());//pass
    }

    @Test
    public void testMaxInsertSizeFilter(){
        SAMRecord pairedRead = simpleGoodRead();
        SAMRecord unpairedRead = simpleGoodRead();
        pairedRead.setReadPairedFlag(true);

        final InsertSizeReadFilter f = new InsertSizeReadFilter();

        pairedRead.setInferredInsertSize(150);
        unpairedRead.setInferredInsertSize(150);

        f.maxInsertSize = 180;
        Assert.assertTrue(f.test(pairedRead), pairedRead.toString());//pass
        Assert.assertTrue(f.test(unpairedRead), pairedRead.toString());//pass

        f.maxInsertSize = 90;
        Assert.assertFalse(f.test(pairedRead), pairedRead.toString());//fail
        Assert.assertTrue(f.test(unpairedRead), pairedRead.toString());//pass

        pairedRead.setInferredInsertSize(-150);

        f.maxInsertSize = 180;
        Assert.assertTrue(f.test(pairedRead), pairedRead.toString());//pass
        Assert.assertTrue(f.test(unpairedRead), pairedRead.toString());//pass

        f.maxInsertSize = 90;
        Assert.assertFalse(f.test(pairedRead), pairedRead.toString());//fail
        Assert.assertTrue(f.test(unpairedRead), pairedRead.toString());//pass
    }

    @Test
    public void testPlatformFilter() {
        SAMRecord read = simpleGoodRead();
        PlatformReadFilter f = new PlatformReadFilter();

        f.PLFilterNames = new HashSet<>(Arrays.asList("PL1", "PL2"));
        read.getReadGroup().setPlatform("PL1");
        Assert.assertTrue(f.test(read), read.toString());//pass

        read.getReadGroup().setPlatform(null);
        Assert.assertFalse(f.test(read), read.toString());//fail - no match

        read.getReadGroup().setPlatform("prefix pl1 suffix");  //not exact matching
        Assert.assertTrue(f.test(read), read.toString());//pass

        f.PLFilterNames = new HashSet<>(Arrays.asList("Fred"));
        read.getReadGroup().setPlatform("PL1");
        Assert.assertFalse(f.test(read), read.toString());//fail
    }

    @Test
    public void testReadLengthFilter() {
        SAMRecord read = simpleGoodRead();
        ReadLengthReadFilter f = new ReadLengthReadFilter();
        f.minReadLength = 10;
        f.maxReadLength = 20;

        read.setReadBases(new byte[5]);
        Assert.assertFalse(f.test(read), read.toString());//fail

        read.setReadBases(new byte[10]);
        Assert.assertTrue(f.test(read), read.toString());//pass

        read.setReadBases(new byte[15]);
        Assert.assertTrue(f.test(read), read.toString());//pass

        read.setReadBases(new byte[20]);
        Assert.assertTrue(f.test(read), read.toString());//pass

        read.setReadBases(new byte[25]);
        Assert.assertFalse(f.test(read), read.toString());//fail
    }

    @Test
    public void testReadNameFilter() {
        SAMRecord read = simpleGoodRead();
        ReadNameReadFilter f = new ReadNameReadFilter();

        final String fred= "fred";
        f.readName = fred;
        read.setReadName(fred);
        Assert.assertTrue(f.test(read), read.toString());//pass

        read.setReadName(fred.toUpperCase());
        Assert.assertFalse(f.test(read), read.toString());//fail
    }

    @Test
    public void testReadStrandFilter() {
        SAMRecord read = simpleGoodRead();
        ReadStrandFilter f = new ReadStrandFilter();

        f.keepOnlyReverse = false;
        read.setReadNegativeStrandFlag(false);
        Assert.assertTrue(f.test(read), read.toString());//pass

        read.setReadNegativeStrandFlag(true);
        Assert.assertFalse(f.test(read), read.toString());//fail

        f.keepOnlyReverse = true;
        read.setReadNegativeStrandFlag(false);
        Assert.assertFalse(f.test(read), read.toString());//fail

        read.setReadNegativeStrandFlag(true);
        Assert.assertTrue(f.test(read), read.toString());//pass
    }

    @Test
    public void testSampleFilter() {
        SAMRecord read = simpleGoodRead();
        SampleReadFilter f = new SampleReadFilter();

        final String fred = "fred";
        f.samplesToKeep = Collections.emptySet();
        read.getReadGroup().setSample(fred);
        Assert.assertFalse(f.test(read), read.toString());//fail

        f.samplesToKeep = Collections.singleton(fred);
        read.getReadGroup().setSample(fred);
        Assert.assertTrue(f.test(read), read.toString());//pass

        f.samplesToKeep = Collections.singleton(fred);
        read.getReadGroup().setSample(fred + "suffix");
        Assert.assertFalse(f.test(read), read.toString());//fail - exact matching

        f.samplesToKeep = Collections.singleton(fred);
        read.getReadGroup().setSample(fred.toUpperCase());
        Assert.assertFalse(f.test(read), read.toString());//fail - case sensitive matching

        f.samplesToKeep = new HashSet<>(Arrays.asList(fred, "bozo"));
        read.getReadGroup().setSample(fred);
        Assert.assertTrue(f.test(read), read.toString());//pass
    }

    @Test
    public void testSingleReadGroupFilter() {
        SAMRecord read = simpleGoodRead();
        final String fred = "fred";

        ReadGroupReadFilter f = new ReadGroupReadFilter();

        f.readGroup = "";
        ReadUtils.setReadGroup(read, new SAMReadGroupRecord(fred));
        Assert.assertFalse(f.test(read), read.toString());//fail

        f.readGroup = fred;
        Assert.assertTrue(f.test(read), read.toString());//pass

        f.readGroup = fred;
        ReadUtils.setReadGroup(read, new SAMReadGroupRecord(fred + "suffix"));
        Assert.assertFalse(f.test(read), read.toString());//fail - exact matching

        f.readGroup = fred;
        ReadUtils.setReadGroup(read, new SAMReadGroupRecord(fred.toUpperCase()));
        Assert.assertFalse(f.test(read), read.toString());//fail - case sensitive matching
    }

    @Test
    public void testPlatformUnitFilter(){
        PlatformUnitReadFilter f = new PlatformUnitReadFilter();
        SAMRecord read = simpleGoodRead();
        final String fred = "fred";
        read.getReadGroup().setPlatformUnit(fred);

        f.blackListedLanes = Collections.emptySet();
        read.getReadGroup().setPlatformUnit(fred);
        Assert.assertTrue(f.test(read), read.toString());//pass - no blacklist

        f.blackListedLanes = Collections.singleton(fred);
        read.getReadGroup().setPlatformUnit(fred);
        Assert.assertFalse(f.test(read), read.toString());//fail

        f.blackListedLanes = Collections.singleton(fred);
        read.getReadGroup().setPlatformUnit(fred + "suffix");
        Assert.assertTrue(f.test(read), read.toString());//pass - exact matching

        f.blackListedLanes = Collections.singleton(fred);
        read.getReadGroup().setPlatformUnit(fred.toUpperCase());
        Assert.assertTrue(f.test(read), read.toString());//pass - case sensitive matching

        f.blackListedLanes = new HashSet<>(Arrays.asList(fred, "bozo"));
        read.getReadGroup().setPlatformUnit(fred);
        Assert.assertFalse(f.test(read), read.toString());//fail

        f.blackListedLanes = Collections.singleton(fred);
        read.setAttribute(SAMTag.PU.name(), fred);
        Assert.assertFalse(f.test(read), read.toString());//fail - match

    }
}
