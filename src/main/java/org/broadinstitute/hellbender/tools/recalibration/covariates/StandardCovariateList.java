package org.broadinstitute.hellbender.tools.recalibration.covariates;

import htsjdk.samtools.SAMRecord;
import org.broadinstitute.hellbender.tools.recalibration.ReadCovariates;
import org.broadinstitute.hellbender.tools.recalibration.RecalibrationArgumentCollection;

import java.util.*;

/**
 * Represents the list of standard BQSR covariates.
 *
 * Note: the first two covariates ({@link ReadGroupCovariate} and {@link QualityScoreCovariate})
 * are special in the way that they are represented in the BQSR recalibration table.
 *
 * The remaining covariates are called "additional covariates".
 */
public final class StandardCovariateList implements Iterable<Covariate>{

    private final ReadGroupCovariate readGroupCovariate;
    private final QualityScoreCovariate qualityScoreCovariate;
    private final List<Covariate> additionalCovariates;
    private final List<Covariate> allCovariates;

    public StandardCovariateList(){
        this.readGroupCovariate = new ReadGroupCovariate();
        this.qualityScoreCovariate = new QualityScoreCovariate();
        ContextCovariate contextCovariate = new ContextCovariate();
        CycleCovariate cycleCovariate = new CycleCovariate();

        this.additionalCovariates = Arrays.asList(contextCovariate, cycleCovariate);
        this.allCovariates = Arrays.asList(readGroupCovariate, qualityScoreCovariate, contextCovariate, cycleCovariate);
    }

    /**
     * Returns the list of simple class names of standard covariates.
     */
    public List<String> getStandardCovariateClassNames() {
        final List<String> names = new ArrayList<>(allCovariates.size());
        for ( final Covariate cov : allCovariates) {
            names.add(cov.getClass().getSimpleName());
        }
        return names;
    }

    /**
     * Returns the size of the list.
     */
    public final int size(){
        return allCovariates.size();
    }

    /**
     * Returns a new iterator over all covariates in this list.
     */
    @Override
    public Iterator<Covariate> iterator() {
        return allCovariates.iterator();
    }

    public ReadGroupCovariate getReadGroupCovariate() {
        return readGroupCovariate;
    }

    public QualityScoreCovariate getQualityScoreCovariate() {
        return qualityScoreCovariate;
    }

    /**
     * returns an unmodifiable view of the additional covariates stored in this list.
     */
    public Iterable<Covariate> getAdditionalCovariates() {
        return Collections.unmodifiableList(additionalCovariates);
    }

    /**
     * Return a human-readable string representing the used covariates
     *
     * @return a non-null comma-separated string
     */
    public String covariateNames() {
        return String.join(",", getStandardCovariateClassNames());
    }

    /**
     * Get the covariate by the index.
     * @throws IndexOutOfBoundsException if the index is out of range
     *         (<tt>index &lt; 0 || index &gt;= size()</tt>)
     */
    public Covariate get(int covIndex) {
        return allCovariates.get(covIndex);
    }

    /**
     * Returns the index of the covariate by class name or -1 if not found.
     */
    public int indexByClass(Class<? extends Covariate> clazz){
        for(int i = 0; i < allCovariates.size(); i++){
            Covariate cov = allCovariates.get(i);
            if (cov.getClass().equals(clazz))  {
                return i;
            }
        }
        return -1;
    }

    /**
     * For each covariate compute the values for all positions in this read and
     * record the values in the provided storage object.
      */
    public void recordAllValuesInStorage(SAMRecord read, ReadCovariates resultsStorage) {
        forEach(cov -> {
            final int index = indexByClass(cov.getClass());
            resultsStorage.setCovariateIndex(index);
            cov.recordValues(read, resultsStorage);
        });
    }

    /**
     * Initializes all covariates in the list using the argument collection.
     */
    public void initializeAll(RecalibrationArgumentCollection rac) {
        allCovariates.forEach(c -> c.initialize(rac));
    }

    /**
     * Retrieves a covariate by the parsed name {@link Covariate#parseNameForReport()} or null
     * if no covariate with that name exists in the list.
     */
    public Covariate getCovariateByParsedName(String covName) {
        return allCovariates.stream().filter(cov -> cov.parseNameForReport().equals(covName)).findFirst().orElse(null);
    }

}
