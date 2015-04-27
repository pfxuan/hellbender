package org.broadinstitute.hellbender.tools.recalibration.covariates;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMReadGroupRecord;
import org.broadinstitute.hellbender.tools.recalibration.ReadCovariates;
import org.broadinstitute.hellbender.tools.recalibration.RecalibrationArgumentCollection;
import org.broadinstitute.hellbender.utils.read.MutableRead;
import org.broadinstitute.hellbender.utils.read.ReadUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * The Read Group covariate.
 */

public class ReadGroupCovariate implements Covariate {

    private final HashMap<String, Integer> readGroupLookupTable = new HashMap<String, Integer>();
    private final HashMap<Integer, String> readGroupReverseLookupTable = new HashMap<Integer, String>();
    private int nextId = 0;
    private String forceReadGroup;

    // Initialize any member variables using the command-line arguments passed to the walkers
    @Override
    public void initialize(final RecalibrationArgumentCollection RAC) {
        forceReadGroup = RAC.FORCE_READGROUP;
    }

    @Override
    public void recordValues(final MutableRead read, final SAMFileHeader header, final ReadCovariates values) {
        final String readGroupId = readGroupValueFromRG(ReadUtils.getSAMReadGroupRecordForRead(read, header));
        final int key = keyForReadGroup(readGroupId);

        final int l = read.getLength();
        for (int i = 0; i < l; i++)
            values.addCovariate(key, key, key, i);
    }

    @Override
    public final Object getValue(final String str) {
        return str;
    }

    @Override
    public String formatKey(final int key) {
        return readGroupReverseLookupTable.get(key);
    }

    @Override
    public int keyFromValue(final Object value) {
        return keyForReadGroup((String) value);
    }

    /**
     * Get the mapping from read group names to integer key values for all read groups in this covariate
     * @return a set of mappings from read group names -> integer key values
     */
    public Set<Map.Entry<String, Integer>> getKeyMap() {
        return readGroupLookupTable.entrySet();
    }

    private int keyForReadGroup(final String readGroupId) {
        if ( ! readGroupLookupTable.containsKey(readGroupId) ) {
            readGroupLookupTable.put(readGroupId, nextId);
            readGroupReverseLookupTable.put(nextId, readGroupId);
            nextId++;
        }

        return readGroupLookupTable.get(readGroupId);
    }

    @Override
    public int maximumKeyValue() {
        return readGroupLookupTable.size() - 1;
    }

    /**
     * If the sample has a PU tag annotation, return that. If not, return the read group id.
     *
     * @param rg the read group record
     * @return platform unit or readgroup id
     */
    private String readGroupValueFromRG(final SAMReadGroupRecord rg) {
        if ( forceReadGroup != null )
            return forceReadGroup;

        final String platformUnit = rg.getPlatformUnit();
        return platformUnit == null ? rg.getId() : platformUnit;
    }
    
}


