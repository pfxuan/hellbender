package org.broadinstitute.hellbender.tools.recalibration;

import htsjdk.samtools.SAMRecord;
import org.broadinstitute.hellbender.tools.recalibration.covariates.ContextCovariate;
import org.broadinstitute.hellbender.tools.recalibration.covariates.Covariate;
import org.broadinstitute.hellbender.utils.BaseUtils;
import org.broadinstitute.hellbender.utils.Utils;
import org.broadinstitute.hellbender.utils.clipping.ClippingRepresentation;
import org.broadinstitute.hellbender.utils.clipping.ReadClipper;
import org.broadinstitute.hellbender.utils.read.ArtificialSAMUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Random;

public class ContextCovariateUnitTest {
    ContextCovariate covariate;
    RecalibrationArgumentCollection RAC;

    @BeforeClass
    public void init() {
        RAC = new RecalibrationArgumentCollection();
        covariate = new ContextCovariate();
        covariate.initialize(RAC);
    }

    @BeforeMethod
    public void initCache() {
        ReadCovariates.clearKeysCache();
    }

    @Test
    public void testSimpleContexts() {
        final Random rnd = Utils.getRandomGenerator();

        for(int i = 0; i < 10; i++) {
            SAMRecord read = ArtificialSAMUtils.createRandomRead(1000);
            read.setReadNegativeStrandFlag(rnd.nextBoolean());
            SAMRecord clippedRead = ReadClipper.clipLowQualEnds(read, RAC.LOW_QUAL_TAIL, ClippingRepresentation.WRITE_NS);
            ReadCovariates readCovariates = new ReadCovariates(read.getReadLength(), 1);
            covariate.recordValues(read, readCovariates);

            verifyCovariateArray(readCovariates.getMismatchesKeySet(), RAC.MISMATCHES_CONTEXT_SIZE, clippedRead, covariate, RAC.LOW_QUAL_TAIL);
            verifyCovariateArray(readCovariates.getInsertionsKeySet(), RAC.INDELS_CONTEXT_SIZE, clippedRead, covariate, RAC.LOW_QUAL_TAIL);
            verifyCovariateArray(readCovariates.getDeletionsKeySet(), RAC.INDELS_CONTEXT_SIZE, clippedRead, covariate, RAC.LOW_QUAL_TAIL);
        }
    }

    public static void verifyCovariateArray(int[][] values, int contextSize, SAMRecord read, Covariate contextCovariate, final byte lowQualTail) {
        for (int i = 0; i < values.length; i++) {
            Assert.assertEquals(contextCovariate.formatKey(values[i][0]), expectedContext(read, i, contextSize, lowQualTail), "offset " + i);
        }
    }

    public static String expectedContext (final SAMRecord originalRead, final int offset, final int contextSize, final byte lowQualTail) {
        final SAMRecord clippedRead = ReadClipper.clipLowQualEnds(originalRead, lowQualTail, ClippingRepresentation.WRITE_NS);
        final boolean negStrand = clippedRead.getReadNegativeStrandFlag();

        final byte[] strandedBaseArray = negStrand ? BaseUtils.simpleReverseComplement(clippedRead.getReadBases()): clippedRead.getReadBases();
        final String strandedBases = stringFrom(strandedBaseArray);

        final int strandedOffset = negStrand ? clippedRead.getReadLength() - offset - 1 : offset;
        final int offsetOfContextStart = strandedOffset - contextSize + 1;
        if (offsetOfContextStart < 0) {
            return null;
        } else {
            final int offsetOneAfterContextEnd = offsetOfContextStart + contextSize;
            final String context = strandedBases.substring(offsetOfContextStart, offsetOneAfterContextEnd);
            if (context.contains("N")) {
                return null;
            } else {
                return context;
            }
        }
    }

    private static String stringFrom(byte[] array) {
        String s = "";
        for (byte value : array)
            s += (char) value;
        return s;
    }

}
