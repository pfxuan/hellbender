package org.broadinstitute.hellbender.tools.exome;

import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.variant.utils.SAMSequenceDictionaryExtractor;
import org.broadinstitute.hellbender.CommandLineProgramTest;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.utils.SimpleInterval;

import java.io.File;
import java.util.Comparator;

/**
 * Some common elements for exome analysis tool unit and integration tests.
 *
 * @author Valentin Ruano-Rubio &lt;valentin@broadinstitute.org&gt;
 */
public final class ExomeToolsTestUtils {

    /**
     * Returns a {@link File} pointing to the directory that contains the test data.
     * @return never {@code null}.
     */
    protected static File getTestDataDir(){
        return new File(CommandLineProgramTest.getTestDataDir(),"exome");
    }

    /**
     * {@link File} pointing to the test toy reference used in exome analysis tool tests.
     */
    protected final static File REFERENCE_FILE = new File(getTestDataDir(),"test_reference.fasta");

    /**
     * Sequence dictionary extracted from {@link #REFERENCE_FILE}.
     */
    protected final static SAMSequenceDictionary REFERENCE_DICTIONARY = SAMSequenceDictionaryExtractor.extractDictionary(REFERENCE_FILE);

    protected static Comparator<SimpleInterval> createIntervalComparator() {
        return ((Comparator<SimpleInterval>) (a,b) -> a.getContig().compareTo(b.getContig()))
                .thenComparing((a,b) -> {
                    final int startCompare = Integer.compare(a.getStart(),b.getStart());
                    return startCompare != 0 ? startCompare : Integer.compare(a.getEnd(),b.getEnd());
                });
    }

    /**
     * Creates a {@link SimpleInterval} instance given its contig and base range.
     * @param contig the new location contig name.
     * @param start  the new location start base index.
     * @param stop the new location stop base index.
     * @return never {@code null}.
     * @throws UserException if there was some problem when creating the location.
     */
    protected static SimpleInterval createInterval(final String contig, final int start, final int stop) {
        return new SimpleInterval(REFERENCE_DICTIONARY.getSequence(contig).getSequenceName(),start,stop);
    }


    /**
     * Creates a {@link SimpleInterval} instance on an entire contig.
     * @param contigIndex the new location contig index.
     * @return never {@code null}.
     * @throws UserException if there was some problem when creating the location.
     */
    protected static SimpleInterval createOverEntireContig(final int contigIndex) {
        final int contigLength = REFERENCE_DICTIONARY.getSequence(contigIndex).getSequenceLength();
        return new SimpleInterval(REFERENCE_DICTIONARY.getSequence(contigIndex).getSequenceName(),1,contigLength);
    }

    /**
     * Creates a {@link SimpleInterval} instance on an entire contig.
     * @param contig the new location contig.
     * @return never {@code null}.
     * @throws UserException if there was some problem when creating the location.
     */
    protected static SimpleInterval createOverEntireContig(final String contig) {
        final int contigLength = REFERENCE_DICTIONARY.getSequence(contig).getSequenceLength();
        return new SimpleInterval(REFERENCE_DICTIONARY.getSequence(contig).getSequenceName(),1,contigLength);
    }

    /**
     * Creates a {@link SimpleInterval} at a give contig and position.
     * @param contig the contig name.
     * @param start the start and stop position.
     * @return never {@code null}.
     * @throws UserException if there was some problem when creating the location.
     */
    protected static SimpleInterval createInterval(final String contig, final int start) {
        return new SimpleInterval(contig,start,start);
    }
}
