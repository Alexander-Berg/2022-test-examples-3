package ru.yandex.common.util.io;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.io.OutputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Created on 18:39:05 11.08.2008
 *
 * @author jkff
 */
public class WriterOutputStreamTest {
    @Test
    public void testWriterOutputStream() throws Exception {
        String str = "Автор выражает надежду, что эта довольно-таки длинная строка будет успешно обработана классом " +
                     "WriterOutputStream, но о полной уверенности в настоящий момент речи не идет";

        StringWriter writer = new StringWriter();

        OutputStream os = new WriterOutputStream(writer, "utf-8");

        byte[] bs = str.getBytes("utf-8");


        int nSlices = 5;

        // Generate 'nBreakpoints' increasing numbers between 0 and nSlices-1
        List<Boolean> breakHere = new ArrayList<Boolean>();
        for(int i = 0; i < bs.length+1; ++i) {
            breakHere.add(i < nSlices-1);
        }
        Collections.shuffle(breakHere, new Random(2837192837L));

        int[] breakpoints = new int[nSlices+1];
        int writePtr = 0;
        breakpoints[writePtr++] = 0;
        for(int i = 0; i < breakHere.size(); ++i) {
            if(breakHere.get(i)) {
                breakpoints[writePtr++] = i;
            }
        }
        breakpoints[writePtr] = bs.length;

        for(int i = 0; i < breakpoints.length-1; ++i) {
            int start = breakpoints[i];
            int end = breakpoints[i+1];
            os.write(bs, start, end-start);
        }

        assertEquals(str, writer.toString());
    }
}
