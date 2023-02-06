from contextvars import ContextVar

from datetime import datetime, timezone

from unittest import TestCase
from unittest.mock import ANY, Mock

from ..mock.client_manager import ClientManagerMock

from extsearch.video.ugc.sqs_moderation.clients.db_client import (
    VideoInfo, SERVICE_UGC, TranscoderTask
)

from extsearch.video.ugc.sqs_moderation.mod2.handlers.rtt_status_saver import RttStatusSaver
from extsearch.video.ugc.sqs_moderation.models.messages import Message, DictBody

id_var = ContextVar("id_var")


class TestRttStatusSaver(TestCase):
    _video_id = 1
    _task_id = "10000000-00000000-00000000-abacabac"
    _now = None
    set_transcoding_status = None

    def _get_task(self, task_id):
        self.assertEqual(task_id, self._task_id)
        return {"Info": {
            "TaskId": self._task_id,
            "Status": 5,
            "StatusStr": "ETSDone",
            "Streams": [{}],
            "Thumbnails": ["//avatars.mds.yandex.net/get-vh/1337/screen{}/orig".format(i+1) for i in range(5)],
            "CreatedAt": int(self._now.timestamp()),
        }}

    def _get_video_info(self, video_id):
        self.assertEqual(video_id, self._video_id)
        return VideoInfo(
            id=self._video_id,
            service=SERVICE_UGC,
            service_id="ugc_1",
            source_url="https://s3.mds.yandex.net/vh-tusd/source",
            moderation_info={},
            moderation_status=None,
            transcoder_status="ETSQueued",
            transcoder_info=None,
            experiment_transcoder_info=None,
            transcoder_task_info={"TaskId": self._task_id},
            transcoder_quota=None,
            transcoder_params=None,
            deleted=False,
            create_time=self._now,
            update_time=self._now.isoformat(),
        )

    def _get_transcoder_task(self, task_id):
        self.assertEqual(task_id, self._task_id)
        return TranscoderTask(
            task_id=self._task_id,
            video_file_id=self._video_id,
            create_time=self._now,
            update_time=self._now.isoformat(),
            ready=False,
            label="main",
            deleted=False,
            expiration_date=None,
            status="ETSQueued",
            data={},
        )

    def setUp(self) -> None:
        self._now = datetime.now(timezone.utc)
        self.set_transcoding_status = Mock(return_value=None)

        client_manager = ClientManagerMock()
        db_client = Mock()
        db_client.get_video_info = self._get_video_info
        db_client.get_transcoder_task = self._get_transcoder_task
        db_client.set_transcoding_status = self.set_transcoding_status
        db_client.list_video_meta = lambda id_, limit: []
        rtt_client = Mock()
        rtt_client.get_task = self._get_task
        client_manager.make_db_client = lambda: db_client
        client_manager.make_rtt_client = lambda: rtt_client
        client_manager.make_perm_avatars_client = lambda: Mock()

        self.handler = RttStatusSaver(
            client_manager=client_manager,
            meta_update_queue="",
            thumbnails_queue="",
            cleanup_callback_queue="",
            metrics=Mock(),
        )

    def test_handle(self):
        msg = Message[DictBody](id=self._video_id, type_="rtt_status", data=self._get_task(self._task_id)["Info"])
        self.handler.handle(msg)
        self.set_transcoding_status.assert_called_once_with(self._video_id, "ETSDone", ANY)
