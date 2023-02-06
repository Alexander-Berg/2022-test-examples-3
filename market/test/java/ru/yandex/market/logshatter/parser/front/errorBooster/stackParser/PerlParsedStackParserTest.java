package ru.yandex.market.logshatter.parser.front.errorBooster.stackParser;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class PerlParsedStackParserTest {
    private static final TestData[] ERRORS = {
        new TestData(
            "[" +
                "{\"filename\":\"contrib/go/_std/src/runtime/debug/stack.go\",\"function\":\"runtime/debug.Stack\"," +
                "\"lineno\":24,\"colno\":20}," +
                "{\"filename\":null,\"function\":null,\"lineno\":null,\"colno\":null}," +
                "{\"filename\":null,\"function\":\"runtime/debug.Stack\",\"lineno\":null,\"colno\":null}," +
                "{\"filename\":\"\",\"function\":\"\"}," +
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
            StackFrame[] actualFrames = new PerlParsedStackParser(stack).getStackFrames();
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
