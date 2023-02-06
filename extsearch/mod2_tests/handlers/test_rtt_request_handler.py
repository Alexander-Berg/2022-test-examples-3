from contextvars import ContextVar

from datetime import datetime
import pytz

from unittest import TestCase
from unittest.mock import Mock

from ..mock.client_manager import ClientManagerMock

from extsearch.video.ugc.sqs_moderation.clients.db_client import (
    ModerationStatus,
    VideoInfo, SERVICE_UGC
)

from extsearch.video.ugc.sqs_moderation.mod2.handlers.rtt_request_handler import (
    RttRequestHandler
)
from extsearch.video.ugc.sqs_moderation.models.messages import RttRequest

id_var = ContextVar("id_var")


class TestRttRequestHandler(TestCase):

    def setUp(self) -> None:

        client_manager = ClientManagerMock()

        file = VideoInfo(
            id=1,
            service=SERVICE_UGC,
            service_id="service_id",
            source_url="",
            moderation_info={},
            moderation_status=ModerationStatus.fail,
            transcoder_status="ETSDone",
            transcoder_info={"SignaturesStatusStr": "ESSFail"},
            experiment_transcoder_info={"test_exp": {"SignaturesStatusStr": "ESSFail"}},
            transcoder_task_info=None,
            transcoder_quota="default",
            transcoder_params={"Graph": "regular"},
            deleted=False,
            create_time=datetime.utcnow().replace(tzinfo=pytz.utc),
            update_time=datetime.now().isoformat(),
        )
        db_client = Mock()
        db_client.get_video_info = Mock(return_value=file)
        db_client.set_transcoding_task_status = Mock()

        client_manager.make_db_client = lambda: db_client

        self.handler = RttRequestHandler(
            client_manager,
            Mock()
        )

    def test_base(self):
        msg = RttRequest(video_id=1, upload_id="", source_url="")
        self.handler.handle(msg)
