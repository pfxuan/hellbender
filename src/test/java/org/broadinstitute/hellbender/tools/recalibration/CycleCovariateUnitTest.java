package org.broadinstitute.hellbender.tools.recalibration;

import htsjdk.samtools.SAMReadGroupRecord;
import htsjdk.samtools.SAMRecord;
import org.broadinstitute.hellbender.tools.recalibration.covariates.CycleCovariate;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.utils.read.ArtificialSAMUtils;
import org.broadinstitute.hellbender.utils.read.ReadUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class CycleCovariateUnitTest {
    CycleCovariate covariate;
    RecalibrationArgumentCollection RAC;

    @BeforeClass
    public void init() {
        RAC = new RecalibrationArgumentCollection();
        covariate = new CycleCovariate();
        covariate.initialize(RAC);
    }

    @BeforeMethod
    public void initCache() {
        ReadCovariates.clearKeysCache();
    }

    @Test
    public void testSimpleCycles() {
        short readLength = 10;
        SAMRecord read = ArtificialSAMUtils.createRandomRead(readLength);
        read.setReadPairedFlag(true);
        ReadUtils.setReadGroup(read, new SAMReadGroupRecord("MY.ID"));
        read.getReadGroup().setPlatform("illumina");

        ReadCovariates readCovariates = new ReadCovariates(read.getReadLength(), 1);
        covariate.recordValues(read, readCovariates);
        verifyCovariateArray(readCovariates.getMismatchesKeySet(), 1, (short) 1);

        read.setReadNegativeStrandFlag(true);
        covariate.recordValues(read, readCovariates);
        verifyCovariateArray(readCovariates.getMismatchesKeySet(), readLength, -1);

        read.setSecondOfPairFlag(true);
        covariate.recordValues(read, readCovariates);
        verifyCovariateArray(readCovariates.getMismatchesKeySet(), -readLength, 1);

        read.setReadNegativeStrandFlag(false);
        covariate.recordValues(read, readCovariates);
        verifyCovariateArray(readCovariates.getMismatchesKeySet(), -1, -1);
    }

    private void verifyCovariateArray(int[][] values, int init, int increment) {
        for (short i = 0; i < values.length; i++) {
            short actual = Short.decode(covariate.formatKey(values[i][0]));
            int expected = init + (increment * i);
            Assert.assertEquals(actual, expected);
        }
    }

    @Test(expectedExceptions={UserException.class})
    public void testMoreThanMaxCycleFails() {
        int readLength = RAC.MAXIMUM_CYCLE_VALUE + 1;
        SAMRecord read = ArtificialSAMUtils.createRandomRead(readLength);
        read.setReadPairedFlag(true);
        ReadUtils.setReadGroup(read, new SAMReadGroupRecord("MY.ID"));
        read.getReadGroup().setPlatform("illumina");

        ReadCovariates readCovariates = new ReadCovariates(read.getReadLength(), 1);
        covariate.recordValues(read, readCovariates);
    }

    @Test
    public void testMaxCyclePasses() {
        int readLength = RAC.MAXIMUM_CYCLE_VALUE;
        SAMRecord read = ArtificialSAMUtils.createRandomRead(readLength);
        read.setReadPairedFlag(true);
        ReadUtils.setReadGroup(read, new SAMReadGroupRecord("MY.ID"));
        read.getReadGroup().setPlatform("illumina");
        ReadCovariates readCovariates = new ReadCovariates(read.getReadLength(), 1);
        covariate.recordValues(read, readCovariates);
    }

    private static int expectedCycleKey(final int baseNumber, final boolean isNegStrand, final boolean isSecondInPair, final int readLength, final boolean indel, final int maxCycle) {
        final int readOrderFactor = isSecondInPair ? -1 : 1;
        final int increment;
        int cycle;
        if (isNegStrand) {
            cycle = readLength * readOrderFactor;
            increment = -1 * readOrderFactor;
        } else {
            cycle = readOrderFactor;
            increment = readOrderFactor;
        }

        cycle = cycle + baseNumber * increment;
        if (indel) {
            final int MAX_CYCLE_FOR_INDELS = readLength - CycleCovariate.CUSHION_FOR_INDELS - 1;
            if ((baseNumber < CycleCovariate.CUSHION_FOR_INDELS || baseNumber > MAX_CYCLE_FOR_INDELS)) {
                return -1;
            } else {
                return CycleCovariate.keyFromCycle(cycle, maxCycle);
            }
        }
        return CycleCovariate.keyFromCycle(cycle, maxCycle);
    }

    public static int expectedCycle(SAMRecord read, final int baseNumber, final boolean indel, final int maxCycle) {
        final int key = expectedCycleKey(baseNumber, read.getReadNegativeStrandFlag(), read.getReadPairedFlag() && read.getSecondOfPairFlag(), read.getReadLength(), indel, maxCycle);
        return CycleCovariate.cycleFromKey(key);
    }
}
