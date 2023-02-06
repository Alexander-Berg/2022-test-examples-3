from contextvars import ContextVar
from datetime import datetime
from unittest import TestCase
from unittest.mock import Mock

from ..mock.client_manager import ClientManagerMock

from extsearch.video.ugc.sqs_moderation.clients.moderation.const import (
    TITLE_BAN_FIELDNAME,
)
from extsearch.video.ugc.sqs_moderation.clients.db_client import (
    ModerationStatus,
    VideoMeta,
    Privacy,
)
from extsearch.video.ugc.sqs_moderation.mod2.handlers.title_ban import TitleBanHandler
from extsearch.video.ugc.sqs_moderation.models.messages import FieldUpdateMessage

id_var = ContextVar("id_var")


class TestTitleBanHandler(TestCase):
    def setUp(self) -> None:
        client_manager = ClientManagerMock()
        db_client = Mock()
        meta = VideoMeta(
            2,
            0,
            1,
            None,
            "title1",
            "thumb1",
            "desc1",
            {},
            ModerationStatus.fail,
            False,
            datetime.now().isoformat(),
            Privacy.PUBLIC,
            "release",
        )
        db_client.get_video_meta = Mock(return_value=meta)
        client_manager.make_db_client = lambda: db_client

        self.handler = TitleBanHandler(
            client_manager,
            "",
        )

    def test_base(self):
        value = {'title': '1000500 сАмЫх МОЩНЫХ __ГОЛОВ__', 'description': ''}
        msg = FieldUpdateMessage(id=1, field=TITLE_BAN_FIELDNAME, value=value, update_time="datetime.now()", meta_id=2)
        self.handler.handle(msg)
        self.assertLogs()
