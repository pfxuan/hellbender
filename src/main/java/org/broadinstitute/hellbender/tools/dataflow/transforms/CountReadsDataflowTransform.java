package org.broadinstitute.hellbender.tools.dataflow.transforms;

import com.google.api.services.genomics.model.Read;
import com.google.cloud.dataflow.sdk.transforms.Count;
import com.google.cloud.dataflow.sdk.values.PCollection;
import org.broadinstitute.hellbender.engine.dataflow.PTransformSAM;

/**
 * Count the number of Reads in a PCollection<Read>
 */
public class CountReadsDataflowTransform extends PTransformSAM<Long> {

    @Override
    public PCollection<Long> apply(PCollection<Read> input) {
        return input.apply(Count.globally());
    }
}
