package org.broadinstitute.hellbender.tools.dataflow.pipelines;

import org.broadinstitute.hellbender.cmdline.CommandLineProgramProperties;
import org.broadinstitute.hellbender.cmdline.programgroups.DataFlowProgramGroup;
import org.broadinstitute.hellbender.engine.dataflow.PTransformSAM;
import org.broadinstitute.hellbender.tools.dataflow.pipelines.DataflowReadsPipeline;
import org.broadinstitute.hellbender.tools.dataflow.transforms.PrintReadsDataflowTransform;

@CommandLineProgramProperties(
        usage = "Prints read from a bam file to a new text file.",
        usageShort = "Print reads",
        programGroup = DataFlowProgramGroup.class
)
public class PrintReadsDataflow extends DataflowReadsPipeline{
        @Override
        protected PTransformSAM<String> getTool() {
                return new PrintReadsDataflowTransform();
        }
}
