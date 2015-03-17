package org.broadinstitute.hellbender.cmdline.argumentcollections;

import org.broadinstitute.hellbender.cmdline.ArgumentCollectionDefinition;

import java.io.File;

public abstract class ReferenceInputArgumentCollection implements ArgumentCollectionDefinition {
    abstract public File getReferenceFile();
}
