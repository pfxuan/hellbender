package org.broadinstitute.hellbender.cmdline.argumentcollections;

import org.broadinstitute.hellbender.cmdline.ArgumentCollectionDefinition;

import java.io.File;
import java.util.List;


/**
 * An abstract argument collection for use with tools that accept input files containing reads
 * (eg., BAM/SAM/CRAM files).
 */
public abstract class ReadInputArgumentCollection implements ArgumentCollectionDefinition {
    public abstract List<File> getReadFiles();
}
