package org.broadinstitute.hellbender.utils.commandline;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Combines a list of files into a single output.
 */
public abstract class Gatherer {
    /**
     * Gathers a list of files into a single output.
     * @param inputs Files to combine.
     * @param output Path to output file.
     * @throws IOException when there's a problem with the IO
     */
    public abstract void gather(List<File> inputs, File output) throws IOException;

    /**
     * Returns true if the caller should wait for the input files to propagate over NFS before running gather().
     * @return true if the caller should wait for the input files to propagate over NFS before running gather().
     */
    public boolean waitForInputs() { return true; }
}
