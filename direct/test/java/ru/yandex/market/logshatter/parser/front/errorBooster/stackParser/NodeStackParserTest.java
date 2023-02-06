package ru.yandex.market.logshatter.parser.front.errorBooster.stackParser;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import static org.junit.Assert.*;

public class NodeStackParserTest {
    private static final TestData[] ERRORS = {
        new TestData(
            "Error: request aborted\n" +
                "    at IncomingMessage.onAborted (/place/db/iss3/instances/kc5xxytllrbl6c6c_production_report_renderer_hamster_web_aG7iZxTVV3/report-renderer/node_modules/raw-body/index.js:269:10)\n" +
                "    at emitNone (events.js:111:20)\n" +
                "    at IncomingMessage.emit (events.js:208:7)\n" +
                "    at abortIncoming (_http_server.js:445:9)\n" +
                "    at socketOnClose (_http_server.js:438:3)\n" +
                "    at emitOne (events.js:121:20)\n" +
                "    at Socket.emit (events.js:211:7)\n" +
                "    at TCP._handle.close [as _onclose] (net.js:561:12)",
            new StackFrame("IncomingMessage.onAborted", "/place/db/iss3/instances/kc5xxytllrbl6c6c_production_report_renderer_hamster_web_aG7iZxTVV3/report-renderer/node_modules/raw-body/index.js", 269, 10),
            new StackFrame("emitNone", "events.js", 111, 20),
            new StackFrame("IncomingMessage.emit", "events.js", 208, 7),
            new StackFrame("abortIncoming", "_http_server.js", 445, 9),
            new StackFrame("socketOnClose", "_http_server.js", 438, 3),
            new StackFrame("emitOne", "events.js", 121, 20),
            new StackFrame("Socket.emit", "events.js", 211, 7),
            new StackFrame("TCP._handle.close [as _onclose]", "net.js", 561, 12)
        ),

        new TestData(
            "TypeError: Cannot read property 'data' of undefined\n" +
                "    at mapClient (/place/db/iss3/instances/sgvx363pwagcpj45_renderer_shared_hamster_T4ypL29nPkK/templates/templates-ydo/build/renderer.js:137022:30)\n" +
                "    at region (/place/db/iss3/instances/sgvx363pwagcpj45_renderer_shared_hamster_T4ypL29nPkK/templates/templates-ydo/build/renderer.js:137027:24)\n" +
                "    at adaptersRegistry.reduce (/place/db/iss3/instances/sgvx363pwagcpj45_renderer_shared_hamster_T4ypL29nPkK/templates/templates-ydo/build/renderer.js:136115:60)\n" +
                "    at Array.reduce (<anonymous>)\n" +
                "    at mapServerToState (/place/db/iss3/instances/sgvx363pwagcpj45_renderer_shared_hamster_T4ypL29nPkK/templates/templates-ydo/build/renderer.js:136115:29)\n" +
                "    at htmlTemplate (/place/db/iss3/instances/sgvx363pwagcpj45_renderer_shared_hamster_T4ypL29nPkK/templates/templates-ydo/build/renderer.js:188331:86)\n" +
                "    at /place/db/iss3/instances/sgvx363pwagcpj45_renderer_shared_hamster_T4ypL29nPkK/templates/templates-ydo/build/renderer.js:188449:22\n" +
                "    at View.render (/place/db/iss3/instances/sgvx363pwagcpj45_renderer_shared_hamster_T4ypL29nPkK/report-renderer/lib/view/index.js:138:37)\n" +
                "    at Object.render [as view] (/place/db/iss3/instances/sgvx363pwagcpj45_renderer_shared_hamster_T4ypL29nPkK/report-renderer/middleware/renderer.js:27:39)\n" +
                "    at /place/db/iss3/instances/sgvx363pwagcpj45_renderer_shared_hamster_T4ypL29nPkK/report-renderer/lib/renderer-registry.js:213:29",
            new StackFrame("mapClient", "/place/db/iss3/instances/sgvx363pwagcpj45_renderer_shared_hamster_T4ypL29nPkK/templates/templates-ydo/build/renderer.js", 137022, 30),
            new StackFrame("region", "/place/db/iss3/instances/sgvx363pwagcpj45_renderer_shared_hamster_T4ypL29nPkK/templates/templates-ydo/build/renderer.js", 137027, 24),
            new StackFrame("adaptersRegistry.reduce", "/place/db/iss3/instances/sgvx363pwagcpj45_renderer_shared_hamster_T4ypL29nPkK/templates/templates-ydo/build/renderer.js", 136115, 60),
            new StackFrame("Array.reduce", "<anonymous>", 0, 0),
            new StackFrame("mapServerToState", "/place/db/iss3/instances/sgvx363pwagcpj45_renderer_shared_hamster_T4ypL29nPkK/templates/templates-ydo/build/renderer.js", 136115, 29),
            new StackFrame("htmlTemplate", "/place/db/iss3/instances/sgvx363pwagcpj45_renderer_shared_hamster_T4ypL29nPkK/templates/templates-ydo/build/renderer.js", 188331, 86),
            new StackFrame("(anonymous)", "/place/db/iss3/instances/sgvx363pwagcpj45_renderer_shared_hamster_T4ypL29nPkK/templates/templates-ydo/build/renderer.js", 188449, 22),
            new StackFrame("View.render", "/place/db/iss3/instances/sgvx363pwagcpj45_renderer_shared_hamster_T4ypL29nPkK/report-renderer/lib/view/index.js", 138, 37),
            new StackFrame("Object.render [as view]", "/place/db/iss3/instances/sgvx363pwagcpj45_renderer_shared_hamster_T4ypL29nPkK/report-renderer/middleware/renderer.js", 27, 39),
            new StackFrame("(anonymous)", "/place/db/iss3/instances/sgvx363pwagcpj45_renderer_shared_hamster_T4ypL29nPkK/report-renderer/lib/renderer-registry.js", 213, 29)
        )
    };

    @Test
    public void parseStack() {
        for (TestData test : ERRORS) {
            test.assertStack();
        }
    }

    @Test
    public void parseAtNameUrlLineColFormat() {
        StackFrame frame;

        frame = NodeStackParser.parseAtNameUrlLineColFormat(
            "    at TCP._handle.close [as _onclose] (net.js:561:12)"
        );

        assertNotNull(frame);
        assertEquals("TCP._handle.close [as _onclose]", frame.getName());
        assertEquals("net.js", frame.getUrl());
        assertEquals(561, frame.getLine());
        assertEquals(12, frame.getCol());

        frame = NodeStackParser.parseAtNameUrlLineColFormat(
            "    at Array.reduce (<anonymous>)"
        );

        assertNotNull(frame);
        assertEquals("Array.reduce", frame.getName());
        assertEquals("<anonymous>", frame.getUrl());
        assertEquals(0, frame.getLine());
        assertEquals(0, frame.getCol());
    }

    @Test
    public void parseAtUrlLineColFormat() {
        StackFrame frame = NodeStackParser.parseAtUrlLineColFormat(
            "    at /place/db/iss3/instances/sgvx363pwagcpj45_renderer_shared_hamster_T4ypL29nPkK/report-renderer/lib/renderer-registry.js:213:29"
        );

        assertNotNull(frame);
        assertEquals("(anonymous)", frame.getName());
        assertEquals("/place/db/iss3/instances/sgvx363pwagcpj45_renderer_shared_hamster_T4ypL29nPkK/report-renderer/lib/renderer-registry.js", frame.getUrl());
        assertEquals(213, frame.getLine());
        assertEquals(29, frame.getCol());
    }

    static class TestData {
        private final String stack;
        private final StackFrame[] frames;

        TestData(String stack, StackFrame... frames) {
            this.stack = stack;
            this.frames = frames;
        }

        void assertStack() {
            StackFrame[] actualFrames = new NodeStackParser(stack).getStackFrames();
            assertArrayEquals(
                "Error parsing stack. Original stack:\n" +
                    "-----\n" +
                    stack + "\n" +
                    "-----\n" +
                    "Parsed stack:\n" +
                    "-----\n" +
                    StringUtils.join(actualFrames, '\n') + "\n" +
                    "-----\n" +
                    "Assertion message",
                frames,
                actualFrames
            );
        }
    }
}
