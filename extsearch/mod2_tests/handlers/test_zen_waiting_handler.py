from contextvars import ContextVar
from unittest import TestCase
from unittest.mock import Mock

from ..mock.client_manager import ClientManagerMock

from extsearch.video.ugc.sqs_moderation.models.messages import YtWaitingMessage
from extsearch.video.ugc.sqs_moderation.mod2.handlers.zen_moderation import ZenWaitingHandler

id_var = ContextVar("id_var")


class TestZenWaitingHandler(TestCase):
    def setUp(self) -> None:
        client_manager = ClientManagerMock()

        yt_client = Mock()
        yt_client.exists = Mock(return_value=True)
        yt_client.remove = Mock()
        result = {
            "id": 1,
            "title": "",
            "snippet": "",
            "imageUrl": "",
            "url": "1",
            "verdicts": ["test1", "test2"],
            "version": 1,
        }
        yt_client.read_table = Mock(return_value=[result])
        client_manager.make_zen_yt_client = lambda: yt_client
        self.handler = ZenWaitingHandler(
            client_manager,
            Mock(),
            "",
            "",
            "",
            Mock(),
        )

    def test_base(self):
        msg = YtWaitingMessage(id=1)
        self.handler.handle(msg)
