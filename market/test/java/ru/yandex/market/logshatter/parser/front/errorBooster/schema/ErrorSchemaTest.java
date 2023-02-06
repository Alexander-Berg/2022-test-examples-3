package ru.yandex.market.logshatter.parser.front.errorBooster.schema;

import java.util.Arrays;

import com.google.common.primitives.UnsignedLong;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.front.errorBooster.stackParser.StackFrame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.logshatter.parser.ParseUtils.sipHash64;

public class ErrorSchemaTest {

    @Test
    public void getStacktraceId() {
        ErrorsSchema errorSchema = new ErrorsSchema();
        StackFrame[] stackFrames1 =
            Arrays.asList(new StackFrame("test", "random url", 10, 20)).toArray(new StackFrame[0]);
        StackFrame[] stackFrames2 =
            Arrays.asList(new StackFrame("test", "random url2", 10, 20)).toArray(new StackFrame[0]);
        StackFrame[] stackFrames3 =
            Arrays.asList(new StackFrame("test", "random url", 20, 10)).toArray(new StackFrame[0]);
        StackFrame[] stackFrames4 =
            Arrays.asList(new StackFrame("test2", "random url", 10, 20)).toArray(new StackFrame[0]);
        StackFrame[] stackFramesEmpty = Arrays.asList().toArray(new StackFrame[0]);
        StackFrame[] stackFramesEmpty2 =
            Arrays.asList(new StackFrame("(anonymous)", "<anonymous>", 10, 20)).toArray(new StackFrame[0]);

        assertEquals(sipHash64(""), errorSchema.getStacktraceId(stackFramesEmpty));
        assertEquals(sipHash64(""), errorSchema.getStacktraceId(stackFramesEmpty2));
        assertEquals(UnsignedLong.valueOf("5641648498458270684"), errorSchema.getStacktraceId(stackFrames1));
        assertEquals(errorSchema.getStacktraceId(stackFrames1), errorSchema.getStacktraceId(stackFrames2));

        assertNotEquals(errorSchema.getStacktraceId(stackFrames1), errorSchema.getStacktraceId(stackFrames3));
        assertNotEquals(errorSchema.getStacktraceId(stackFrames1), errorSchema.getStacktraceId(stackFrames4));
    }

    @Test
    public void fillCoordinatesGPEmpty() {
        ErrorsSchema errorSchema = new ErrorsSchema();
        errorSchema.fillCoordinatesGP("");
        assertTrue(errorSchema.coordinatesLatitude == 0d);
        assertTrue(errorSchema.coordinatesLongitude == 0d);
        assertTrue(errorSchema.coordinatesPrecision == 0);
        assertTrue(errorSchema.coordinatesTimestamp == 0L);
    }

    @Test
    public void fillCoordinatesGPIncomplete() {
        ErrorsSchema errorSchema = new ErrorsSchema();
        errorSchema.fillCoordinatesGP("43_2959023:76_9437790:140_0000000:1");
        assertTrue(errorSchema.coordinatesLatitude == 0d);
        assertTrue(errorSchema.coordinatesLongitude == 0d);
        assertTrue(errorSchema.coordinatesPrecision == 0);
        assertTrue(errorSchema.coordinatesTimestamp == 0L);
    }

    @Test
    public void fillCoordinatesGPCorrect() {
        ErrorsSchema errorSchema = new ErrorsSchema();
        errorSchema.fillCoordinatesGP("43_2959023:76_9437790:140_0000000:1:1425279224");
        assertTrue(errorSchema.coordinatesLatitude == 43.2959023d);
        assertTrue(errorSchema.coordinatesLongitude == 76.9437790d);
        assertTrue(errorSchema.coordinatesPrecision == 140);
        assertTrue(errorSchema.coordinatesTimestamp == 1425279224L);
    }

    @Test
    public void fillCoordinatesGPPrecisionOverflow() {
        ErrorsSchema errorSchema = new ErrorsSchema();
        errorSchema.fillCoordinatesGP("43_2959023:76_9437790:777777_0000000:1:1425279224");
        assertTrue(errorSchema.coordinatesLatitude == 43.2959023d);
        assertTrue(errorSchema.coordinatesLongitude == 76.9437790d);
        assertTrue(errorSchema.coordinatesPrecision == 65535);
        assertTrue(errorSchema.coordinatesTimestamp == 1425279224L);
    }
}
