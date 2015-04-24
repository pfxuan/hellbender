package org.broadinstitute.hellbender.utils.dataflow;

import com.google.api.services.genomics.model.Read;
import com.google.cloud.dataflow.sdk.Pipeline;
import com.google.cloud.dataflow.sdk.transforms.Create;
import com.google.cloud.dataflow.sdk.transforms.DoFn;
import com.google.cloud.dataflow.sdk.transforms.PTransform;
import com.google.cloud.dataflow.sdk.transforms.ParDo;
import com.google.cloud.dataflow.sdk.values.PCollection;
import com.google.cloud.genomics.dataflow.readers.bam.ReadConverter;
import htsjdk.samtools.SAMRecord;
import org.broadinstitute.hellbender.engine.ReadsDataSource;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.utils.SimpleInterval;
import sun.java2d.pipe.SpanShapeRenderer;

import java.io.File;
import java.util.List;

public class DataflowUtils {

    private DataflowUtils(){} //prevent instantiation

    /**
     * a transform which will convert the input PCollection<I> to a PCollection<String> by calling toString() on each element
     * @return a Transform from I -> String
     */
    public static <I> PTransform<PCollection<? extends I>,PCollection<String>> convertToString(){
        return ParDo.of(
                new DoFn<I, String>() {
                  @Override
                  public void processElement(ProcessContext c) {
                      c.output(c.element().toString());
                  }
              });
    }

    /**
     * ingest local bam files from the file system and loads them into a PCollection<Read>
     * @param pipeline a configured Pipeline
     * @param intervals intervals to select reads from
     * @param bams paths to bam files to read from
     * @return a PCollection<Read> with all the reads the overlap the given intervals in the bams
     */
    public static PCollection<Read> getReadsFromLocalBams(final Pipeline pipeline, final List<SimpleInterval> intervals, final List<File> bams) {
        return pipeline.apply(Create.of(bams))
                .apply(ParDo.of(new LoadReadsFromFileFn(intervals)));
    }


    /**
     * get a transform that throws a specified exception
     */
    public static <I,O> PTransform<PCollection<? extends I>,PCollection<O>> throwException(Exception e){
        return ParDo.of(new ThrowExceptionFn<I, O>(e));
    }

    /**
     * throw a specified exception on execution
     */
    public static class ThrowExceptionFn<I,O> extends DoFn<I,O> {
        private final Exception e;

        public ThrowExceptionFn(Exception e){
            this.e = e;
        }

        @Override
        public void processElement(ProcessContext c) throws Exception {
            throw e;
        }
    }

    /**
     * Read a bam file and output each of the reads in it
     */
    public static class LoadReadsFromFileFn extends DoFn<File, Read> {
        private final List<SimpleInterval> intervals;

        public LoadReadsFromFileFn(List<SimpleInterval> intervals) {
            this.intervals = intervals;
        }

        @Override
        public void processElement(ProcessContext c) {
            ReadsDataSource sams = new ReadsDataSource(c.element());
            sams.setIntervalsForTraversal(intervals);
            for (SAMRecord sam : sams) {
                c.output(ReadConverter.makeRead(sam));
            }
        }
    }
}
