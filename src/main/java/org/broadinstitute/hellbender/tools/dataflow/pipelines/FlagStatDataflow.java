package org.broadinstitute.hellbender.tools.dataflow.pipelines;


import com.google.common.collect.ImmutableList;
import org.broadinstitute.hellbender.cmdline.CommandLineProgramProperties;
import org.broadinstitute.hellbender.cmdline.programgroups.DataFlowProgramGroup;
import org.broadinstitute.hellbender.engine.dataflow.PTransformSAM;
import org.broadinstitute.hellbender.engine.filters.ReadFilter;
import org.broadinstitute.hellbender.engine.filters.ReadFilterLibrary;
import org.broadinstitute.hellbender.tools.FlagStat;
import org.broadinstitute.hellbender.tools.dataflow.transforms.FlagStatusDataflowTransform;

@CommandLineProgramProperties(usage="runs FlagStat on dataflow", usageShort = "FlagStat", programGroup = DataFlowProgramGroup.class)
public class FlagStatDataflow extends DataflowReadsPipeline {
    @Override
    protected ImmutableList<ReadFilter> getReadFilters(){
        return ImmutableList.of(ReadFilterLibrary.WELLFORMED);
    }

    @Override
    protected PTransformSAM<FlagStat.FlagStatus> getTool() {
        return new FlagStatusDataflowTransform();
    }


}