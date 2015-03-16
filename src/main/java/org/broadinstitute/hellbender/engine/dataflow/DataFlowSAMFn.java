package org.broadinstitute.hellbender.engine.dataflow;


import com.google.api.services.genomics.model.Read;
import com.google.cloud.dataflow.sdk.transforms.DoFn;
import com.google.cloud.genomics.gatk.common.GenomicsConverter;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMRecord;

import java.util.ArrayList;
import java.util.List;

import static org.broadinstitute.hellbender.utils.read.mergealignment.SamHeaderUtils.samHeaderFromString;

/**
 * A DoFn from Read -> Something which presents a SAMRecord to its apply method.
 * This is a temporary workaround for not having a common Read interface.
 */
public abstract class DataFlowSAMFn<O> extends DoFn<Read, O> {

  private transient SAMFileHeader header;
  private final String headerString;

  private List<O> toEmit = new ArrayList<>();

  public DataFlowSAMFn(String headerString){
    this.headerString = headerString;
  }

  public SAMFileHeader getHeader(){
    if (header == null) {
      this.header = samHeaderFromString(headerString);
    }
    return header;
  }


  @Override
  public void processElement(ProcessContext c) throws Exception {
    SAMRecord sam = GenomicsConverter.makeSAMRecord(c.element(), getHeader());
    apply(sam);
    toEmit.forEach(c::output);
    toEmit.clear();
  }


  protected void output(O output){
    toEmit.add(output);
  }

  protected abstract void apply(SAMRecord read);
}
