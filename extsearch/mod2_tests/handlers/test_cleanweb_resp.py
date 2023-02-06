
from contextvars import ContextVar
from datetime import datetime
from unittest import TestCase
from unittest.mock import Mock

from ..mock.client_manager import ClientManagerMock

from extsearch.video.ugc.sqs_moderation.models.messages import CleanWebResponseMessage
from extsearch.video.ugc.sqs_moderation.clients.moderation.const import (
    OBJECT_FILE,
    VIDEO_FIELDNAME
)
from extsearch.video.ugc.sqs_moderation.mod2.handlers.cleanweb_resp import (
    CleanWebResultHandler
)

id_var = ContextVar("id_var")
OTHER_VALUE = "***"
META_FIELD_VALUE = "meta field"
FILE_FIELD_VALUE = "file field"


class TestCleanWebResultHandler(TestCase):
    def setUp(self) -> None:
        client_manager = ClientManagerMock()

        self.handler = CleanWebResultHandler(
            client_manager,
            "",
            Mock(),
        )

    def test_base(self):
        verdict_data = {
            "object": OBJECT_FILE,
            "field": VIDEO_FIELDNAME,
            "value": True,
            "update_time": datetime.now(),
        }
        msg = CleanWebResponseMessage(
            key="123/2",
            entity="screenshots",
            source="clean-web",
            subsource="custom",
            name="test",
            value={},
            verdict_data=verdict_data,
        )
        self.handler.handle(msg)

    def test_empty_verdict_data(self):
        self.handler._handle_intermediate = Mock(
            side_effect=Exception("Should not be called")
        )
        self.handler._handle_final = Mock(
            side_effect=Exception("Should not be called")
        )
        msg = CleanWebResponseMessage(
            key="123/2",
            entity="screenshots",
            source="clean-web",
            subsource="custom",
            name="test",
            value={},
            verdict_data=None,
        )
        self.handler.handle(msg)
        self.handler._handle_intermediate.assert_not_called()
        self.handler._handle_final.assert_not_called()

    def test_final(self):
        self.handler._handle_intermediate = Mock(
            side_effect=Exception("Should not be called")
        )
        self.handler._handle_final = Mock()
        verdict_data = {
            "object": OBJECT_FILE,
            "field": VIDEO_FIELDNAME,
            "value": True,
            "update_time": datetime.now(),
        }
        msg = CleanWebResponseMessage(
            key="123/2",
            entity="screenshots",
            source="clean-web",
            subsource="custom",
            name="moderation_completed",
            value={},
            verdict_data=verdict_data,
        )
        self.handler.handle(msg)
        self.handler._handle_intermediate.assert_not_called()
        self.handler._handle_final.assert_called()
