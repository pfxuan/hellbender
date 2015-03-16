package org.broadinstitute.hellbender.utils.read.mergealignment;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMTextHeaderCodec;
import htsjdk.samtools.ValidationStringency;
import htsjdk.samtools.util.StringLineReader;

public class SamHeaderUtils {
    private SamHeaderUtils(){} // don't instantiate this

    /**
     * create a SAMFileHeader from a String
     */
    public static SAMFileHeader samHeaderFromString(String headerString){
      final SAMTextHeaderCodec headerCodec = new SAMTextHeaderCodec();
      headerCodec.setValidationStringency(ValidationStringency.LENIENT);
      return headerCodec.decode(new StringLineReader(headerString), null);
    }
}
