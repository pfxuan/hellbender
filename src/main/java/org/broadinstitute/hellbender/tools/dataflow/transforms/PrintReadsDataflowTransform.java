package org.broadinstitute.hellbender.tools.dataflow.transforms;

import com.google.api.services.genomics.model.Read;
import com.google.cloud.dataflow.sdk.transforms.ParDo;
import com.google.cloud.dataflow.sdk.values.PCollection;
import htsjdk.samtools.SAMRecord;
import org.broadinstitute.hellbender.engine.dataflow.DataFlowSAMFn;
import org.broadinstitute.hellbender.engine.dataflow.PTransformSAM;


/**
 * Print a string representation of reads in the PCollection<String>
 */
public class PrintReadsDataflowTransform extends PTransformSAM<String> {
        @Override
        public PCollection<String> apply(PCollection<Read> reads) {
                return reads.apply(ParDo.of(new DataFlowSAMFn<String>(getHeaderString()) {
                        @Override
                        protected void apply(SAMRecord read) {
                                output(read.getSAMString());
                        }
                }));
        }
}
