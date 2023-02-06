import yatest

from search.martylib.proto.structures.trace_pb2 import Frame, ParserContext
from search.martylib.test_utils import TestCase

from search.sawmill.proto.trace_pb2 import TraceStartRequest
from search.sawmill.src.services import Agent


EXAMPLE_BLOG = yatest.common.source_path('search/sawmill/tests/data/example.txt')


class TestAgentStreaming(TestCase):
    @classmethod
    def setUpClass(cls):
        cls.agent = Agent()
        cls.request = TraceStartRequest(
            parser_context=ParserContext(
                max_emitted_frames=10,
                filenames=(EXAMPLE_BLOG, ),
            ),
        )

    def test_agent_streaming(self):
        frame_count = 0
        expected_frame_count = 10
        with self.mock_request() as ctx:
            for frame in self.agent.tracedump_stream(self.request, ctx):
                assert isinstance(frame, Frame)
                frame_count += 1

        assert frame_count == expected_frame_count
