from contextvars import ContextVar
from datetime import datetime
from unittest import TestCase
from unittest.mock import Mock
import pytz

from ..mock.client_manager import ClientManagerMock

from extsearch.video.ugc.sqs_moderation.clients.db_client import (
    ModerationStatus,
    VideoInfo,
    VideoMeta,
    SERVICE_UGC,
    Privacy,
)
from extsearch.video.ugc.sqs_moderation.clients.moderation.const import COMPLEX_FIELDNAME
from extsearch.video.ugc.sqs_moderation.mod2.handlers.zen_moderation import ZenModerationHandler
from extsearch.video.ugc.sqs_moderation.models.messages import FieldUpdateMessage

id_var = ContextVar("id_var")


class TestZenModerationHandler(TestCase):
    def setUp(self) -> None:
        client_manager = ClientManagerMock()

        db_client = Mock()
        file = VideoInfo(
            id=1,
            service=SERVICE_UGC,
            service_id="service_id",
            source_url="",
            moderation_info={},
            moderation_status=ModerationStatus.fail,
            transcoder_status="ETSDone",
            transcoder_info={
                "SignaturesStatusStr": "ESSFail",
                "Streams": ["stream1"],
            },
            experiment_transcoder_info={
                "test_exp": {
                    "SignaturesStatusStr": "ESSFail",
                    "Streams": ["stream1"],
                },
            },
            transcoder_task_info=None,
            transcoder_quota="default",
            transcoder_params={"Graph": "regular"},
            deleted=False,
            create_time=datetime.utcnow().replace(tzinfo=pytz.utc),
            update_time=datetime.now().isoformat(),
        )
        meta = VideoMeta(
            id=2,
            channel_id=0,
            video_file_id=1,
            video_stream_id=None,
            title="title1",
            thumbnail="thumb1",
            description="desc1",
            moderation_info={},
            moderation_status=ModerationStatus.fail,
            deleted=False,
            update_time=datetime.now().isoformat(),
            privacy=Privacy.PUBLIC,
            release_date="release",
        )
        db_client.get_video_info = Mock(return_value=file)
        db_client.get_video_meta = Mock(return_value=meta)

        client_manager.make_db_client = lambda: db_client

        self.handler = ZenModerationHandler(
            client_manager,
            "",
            "",
        )

    def test_base(self):
        value = {}
        msg = FieldUpdateMessage(id=1, field=COMPLEX_FIELDNAME, value=value, update_time="datetime.now()", meta_id=2)
        self.handler.handle([msg])
