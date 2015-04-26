package org.broadinstitute.hellbender.utils.read;

import htsjdk.samtools.Cigar;
import htsjdk.samtools.CigarElement;
import htsjdk.samtools.CigarOperator;
import htsjdk.samtools.SAMRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

public final class CigarUtils {
    private CigarUtils(){}

    /**
     * Combines equal adjacent elements of a Cigar object
     *
     * @param rawCigar the cigar object
     * @return a combined cigar object
     */
    public static Cigar combineAdjacentCigarElements(Cigar rawCigar) {
        Cigar combinedCigar = new Cigar();
        CigarElement lastElement = null;
        int lastElementLength = 0;
        for (CigarElement cigarElement : rawCigar.getCigarElements()) {
            if (lastElement != null &&
                    ((lastElement.getOperator() == cigarElement.getOperator()) ||
                            (lastElement.getOperator() == CigarOperator.I && cigarElement.getOperator() == CigarOperator.D) ||
                            (lastElement.getOperator() == CigarOperator.D && cigarElement.getOperator() == CigarOperator.I)))
                lastElementLength += cigarElement.getLength();
            else
            {
                if (lastElement != null)
                    combinedCigar.add(new CigarElement(lastElementLength, lastElement.getOperator()));

                lastElement = cigarElement;
                lastElementLength = cigarElement.getLength();
            }
        }
        if (lastElement != null)
            combinedCigar.add(new CigarElement(lastElementLength, lastElement.getOperator()));

        return combinedCigar;
    }

    /**
     * Checks whether the cigar has any element that is not H or S
     * @return true the cigar has elements other than S or H, false otherwise.
     */
    public static boolean hasNonClippedBases(Cigar cigar) {
        return cigar.getCigarElements().stream().anyMatch(el -> el.getOperator() != CigarOperator.SOFT_CLIP && el.getOperator() != CigarOperator.HARD_CLIP);
    }

    /**
     * Inverts the order of the operators in the cigar.
     * Eg 10M1D20M -> 20M1D10M
     */
    public static Cigar invertCigar (final Cigar cigar) {
        final List<CigarElement>  els = new ArrayList<>(cigar.getCigarElements());
        Collections.reverse(els);
        return new Cigar(els);
    }

    /**
    * A valid cigar object obeys the following rules:
    *  - No Hard/Soft clips in the middle of the read
    *  - No deletions in the beginning / end of the read
    *  - No repeated adjacent element (e.g. 1M2M -> this should be 3M)
    *  - No consecutive I/D elements
    **/
    public static boolean isCigarValid(Cigar cigar) {
        if (cigar.isValid(null, -1) == null) {                                                                          // This should take care of most invalid Cigar Strings (picard's "exhaustive" implementation)

            Stack<CigarElement> cigarElementStack = new Stack<>();                                          // Stack to invert cigar string to find ending operator
            CigarOperator startingOp = null;
            CigarOperator endingOp = null;

            // check if it doesn't start with deletions
            boolean readHasStarted = false;                                                                             // search the list of elements for the starting operator
            for (CigarElement cigarElement : cigar.getCigarElements()) {
                if (!readHasStarted) {
                    if (cigarElement.getOperator() != CigarOperator.SOFT_CLIP && cigarElement.getOperator() != CigarOperator.HARD_CLIP) {
                        readHasStarted = true;
                        startingOp = cigarElement.getOperator();
                    }
                }
                cigarElementStack.push(cigarElement);
            }

            while (!cigarElementStack.empty()) {
                CigarElement cigarElement = cigarElementStack.pop();
                if (cigarElement.getOperator() != CigarOperator.SOFT_CLIP && cigarElement.getOperator() != CigarOperator.HARD_CLIP) {
                    endingOp = cigarElement.getOperator();
                    break;
                }
            }

            if (startingOp != CigarOperator.DELETION && endingOp != CigarOperator.DELETION && startingOp != CigarOperator.SKIPPED_REGION && endingOp != CigarOperator.SKIPPED_REGION)
                return true;                                                                                          // we don't accept reads starting or ending in deletions (add any other constraint here)
        }

        return false;
    }

    /**
     * Compute the number of reference bases between the start (inclusive) and end (exclusive) cigar elements.
     */
    @SuppressWarnings("fallthru")
    public static int countRefBasesBasedOnCigar(final SAMRecord read, final int cigarStartIndex, final int cigarEndIndex){
        if (read == null){
            throw new IllegalArgumentException("null read");
        }
        final List<CigarElement> elems = read.getCigar().getCigarElements();
        if (cigarStartIndex < 0 || cigarEndIndex > elems.size() || cigarStartIndex > cigarEndIndex){
            throw new IllegalArgumentException("invalid index:" + 0 + " -" + elems.size());
        }
        int result = 0;
        for(int i = cigarStartIndex; i < cigarEndIndex; i++){
            final CigarElement cigarElement = elems.get(i);
            switch (cigarElement.getOperator()) {
                case M:
                case D:
                case N:
                case EQ:
                case X:
                case S:        //TODO is this correct? CigarOperator.consumesReferenceBases is false
                case P:        //TODO is this correct? CigarOperator.consumesReferenceBases is false
                case H:        //TODO is this correct? CigarOperator.consumesReferenceBases is false
                    result += cigarElement.getLength();
                    break;
                default://I
                    break;
            }
        }
        return result;
    }

    /**
     * Removes all clipping operators from the cigar.
     */
    public static Cigar unclipCigar(final Cigar cigar) {
        final List<CigarElement> elements = new ArrayList<>(cigar.numCigarElements());
        for ( CigarElement ce : cigar.getCigarElements() ) {
            if ( !isClipOperator(ce.getOperator()) )
                elements.add(ce);
        }
        return new Cigar(elements);
    }

    private static boolean isClipOperator(final CigarOperator op) {
        return op == CigarOperator.S || op == CigarOperator.H || op == CigarOperator.P;
    }

    private static boolean isClipOperator(final CigarElement el) {
        return isClipOperator(el.getOperator());
    }

    /**
     * Given a cigar1 and a read with cigar2,
     * this method creates cigar3 such that it has flanking clip operators from cigar2
     * and it has all operators from cigar1 in the middle.
     *
     * In other words if:
     * cigar2 = leftClip2 + noclips2 + rightClip2
     *
     * then
     * cigar3 = leftClip2 + cigar1 + rightClip2
     */
    public static Cigar reclipCigar(final Cigar cigar, final SAMRecord read) {
        final List<CigarElement> elements = new ArrayList<>();

        int i = 0;
        final Cigar readCigar = read.getCigar();
        final int n = readCigar.numCigarElements();
        final List<CigarElement> readEls = readCigar.getCigarElements();

        //copy head clips
        while ( i < n && isClipOperator(readEls.get(i)) ) {
            elements.add(readEls.get(i));
            i++;
        }

        elements.addAll(cigar.getCigarElements());

        //skip over non-clips
        i++;
        while ( i < n && !isClipOperator(readEls.get(i)) ) {
            i++;
        }

        //copy tail clips
        while ( i < n && isClipOperator(readEls.get(i)) ) {
            elements.add(readEls.get(i));
            i++;
        }

        return new Cigar(elements);
    }

    /**
     * Returns whether the cigar has any N operators.
     */
    public static boolean containsNOperator(final Cigar cigar) {
        return cigar.getCigarElements().stream().anyMatch(el -> el.getOperator() == CigarOperator.N);
    }
}
