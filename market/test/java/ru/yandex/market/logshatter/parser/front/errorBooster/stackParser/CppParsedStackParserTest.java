package ru.yandex.market.logshatter.parser.front.errorBooster.stackParser;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class CppParsedStackParserTest {
    private static final TestData[] ERRORS = {
        new TestData(
            "[" +
                "{\"file\":\"contrib/go/_std/src/runtime/debug/stack.go\",\"func\":\"runtime/debug.Stack\"," +
                "\"line\":24,\"col\":20}," +
                "{\"file\":null,\"func\":null,\"line\":null,\"col\":null}," +
                "{\"file\":null,\"func\":\"runtime/debug.Stack\",\"line\":null,\"col\":null}," +
                "{\"file\":\"\",\"func\":\"\"}," +
                "{}," +
                "]",
            new StackFrame("runtime/debug.Stack", "contrib/go/_std/src/runtime/debug/stack.go", 24, 20),
            new StackFrame("runtime/debug.Stack", "", 0, 0)
        ),

        // Empty
        new TestData(
            "[]"
        ),
    };

    @Test
    public void parseStack() {
        for (TestData test : ERRORS) {
            test.assertStack();
        }
    }

    static class TestData {
        private final JsonArray stack;
        private final StackFrame[] frames;

        TestData(String stack, StackFrame... frames) {
            this.stack = JsonParser.parseString(stack).getAsJsonArray();
            this.frames = frames;
        }

        void assertStack() {
            StackFrame[] actualFrames = new CppParsedStackParser(stack).getStackFrames();
            assertArrayEquals(
                frames,
                actualFrames,
                "Error parsing stack. Original stack:\n" +
                    "-----\n" +
                    stack + "\n" +
                    "-----\n" +
                    "Parsed stack:\n" +
                    "-----\n" +
                    StringUtils.join(actualFrames, '\n') + "\n" +
                    "-----\n" +
                    "Assertion message"
            );
        }
    }
}
