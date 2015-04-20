package org.broadinstitute.hellbender.tools.recalibration;

import htsjdk.samtools.SAMRecord;
import org.broadinstitute.hellbender.tools.recalibration.covariates.*;
import org.broadinstitute.hellbender.utils.test.BaseTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public final class StandardCovariateListUnitTest extends BaseTest {
    @Test
    public void testSize() {
        StandardCovariateList scl = new StandardCovariateList();
        Assert.assertEquals(scl.size(), 4);
    }

    @Test
    public void testCovariateNames() {
        StandardCovariateList scl = new StandardCovariateList();
        Assert.assertEquals(scl.covariateNames(), "ReadGroupCovariate,QualityScoreCovariate,ContextCovariate,CycleCovariate");
    }

    @Test
    public void testIterator() {
        StandardCovariateList scl = new StandardCovariateList();
        Assert.assertEquals(StreamSupport.stream(scl.spliterator(), false).count(), 4);
    }

    @Test
    public void testGetCovariates() {
        StandardCovariateList scl = new StandardCovariateList();
        Assert.assertEquals(scl.getReadGroupCovariate().parseNameForReport(), "ReadGroup");
        Assert.assertEquals(scl.getQualityScoreCovariate().parseNameForReport(), "QualityScore");
        final List<Covariate> additionalCovars = StreamSupport.stream(scl.getAdditionalCovariates().spliterator(), false).collect(Collectors.toList());
        Assert.assertEquals(additionalCovars.get(0).parseNameForReport(), "Context");
        Assert.assertEquals(additionalCovars.get(1).parseNameForReport(), "Cycle");
    }

    @Test
    public void testGetCovariatesByIndex() {
        StandardCovariateList scl = new StandardCovariateList();
        Assert.assertEquals(scl.get(0).parseNameForReport(), "ReadGroup");
        Assert.assertEquals(scl.get(1).parseNameForReport(), "QualityScore");
        Assert.assertEquals(scl.get(2).parseNameForReport(), "Context");
        Assert.assertEquals(scl.get(3).parseNameForReport(), "Cycle");
    }

    @Test(expectedExceptions = IndexOutOfBoundsException.class)
    public void testGetCovariatesByIndexInvalid() {
        StandardCovariateList scl = new StandardCovariateList();
        scl.get(4);
    }

    @Test
    public void testGetCovariatesByIndexClass() {
        StandardCovariateList scl = new StandardCovariateList();
        Assert.assertEquals(scl.indexByClass(ReadGroupCovariate.class), 0);
        Assert.assertEquals(scl.indexByClass(QualityScoreCovariate.class), 1);
        Assert.assertEquals(scl.indexByClass(ContextCovariate.class), 2);
        Assert.assertEquals(scl.indexByClass(CycleCovariate.class), 3);

        //finally, test an anonymous subclass
        Assert.assertEquals(scl.indexByClass(new Covariate() {

            @Override
            public void initialize(RecalibrationArgumentCollection RAC) {

            }

            @Override
            public void recordValues(SAMRecord read, ReadCovariates values) {

            }

            @Override
            public Object getValue(String str) {
                return null;
            }

            @Override
            public String formatKey(int key) {
                return null;
            }

            @Override
            public int keyFromValue(Object value) {
                return 0;
            }

            @Override
            public int maximumKeyValue() {
                return 0;
            }
        }.getClass()), -1);
    }

    @Test
    public void testGetCovariatesByParsedName() {
        StandardCovariateList scl = new StandardCovariateList();
        final String[] parsedNames = {"ReadGroup", "QualityScore", "Context", "Cycle"};
        for (String parsedName : parsedNames) {
            Assert.assertEquals(scl.getCovariateByParsedName(parsedName).parseNameForReport(), parsedName);
        }
        Assert.assertEquals(scl.getCovariateByParsedName("fred"), null);
    }

    @Test
    public void testCovariateInitialize() {
        StandardCovariateList scl = new StandardCovariateList();
        RecalibrationArgumentCollection rac = new RecalibrationArgumentCollection();
        scl.initializeAll(rac);
        //this just tests non blowing up.
    }
}
