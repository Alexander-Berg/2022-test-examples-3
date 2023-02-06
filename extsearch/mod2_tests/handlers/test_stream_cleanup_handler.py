import json

from datetime import datetime
from unittest import TestCase
from unittest.mock import Mock

from ..mock.client_manager import ClientManagerMock

from extsearch.video.robot.rt_transcoder.output_cleaner.proto import msgs_pb2

from extsearch.video.ugc.sqs_moderation.clients.db_client import VideoMeta
from extsearch.video.ugc.sqs_moderation.mod2.handlers.stream_cleanup_handler import (
    StreamCleanupHandler,
)
from extsearch.video.ugc.sqs_moderation.clients.rtt import encode_pb_message, CleanupMethod
from extsearch.video.ugc.sqs_moderation.models.messages import (
    RawMessage,
    DictBody,
)


DUMMY_VIDEO_META: DictBody = {
    "id": 111,
    "channel_id": 222,
    "video_file_id": 333,
    "video_stream_id": None,
    "title": None,
    "thumbnail": None,
    "description": None,
    "moderation_info": {},
    "moderation_status": None,
    "deleted": False,
    "update_time": datetime.now().isoformat(),
    "privacy": None,
    "release_date": datetime.now().isoformat(),
}

DUMMY_TASK_INFO: DictBody = {"TaskId": "123-456", "Streams": []}


class TestStreamCleanupHandler(TestCase):
    def setUp(self) -> None:
        self.handler = StreamCleanupHandler(
            cleanup_callback_queue="cleanup_callback_queue",
            client_manager=ClientManagerMock(),
            rtt_status_queue="rtt_status_queue",
        )

        # Prepare variables
        self.handler.cleanup_callback_queue_url = (
            f"http://{self.handler.cleanup_callback_queue}"
        )
        self.handler.rtt_status_queue_url = (
            f"http://{self.handler.rtt_status_queue}"
        )

        # Forbid external methods by default
        self.handler.db_client.list_video_meta = Mock(
            side_effect=Exception("Should not be called")
        )
        self.handler.db_client.set_meta_deleted = Mock(
            side_effect=Exception("Should not be called")
        )
        self.handler.rtt_client.get_task = Mock(
            side_effect=Exception("Should not be called")
        )
        self.handler.rtt_client.remove_streams = Mock(
            side_effect=Exception("Should not be called")
        )
        self.handler.boto_client.send_message = Mock(
            side_effect=Exception("Should not be called")
        )

    def test_full_remove(self):
        meta = VideoMeta(**DUMMY_VIDEO_META)

        self.handler.db_client.list_video_meta = Mock(return_value=[meta])
        self.handler.db_client.set_meta_deleted = Mock()
        self.handler.boto_client.send_message = Mock()

        rtt_response = msgs_pb2.TResponse()
        rtt_response.Request.TaskId = DUMMY_TASK_INFO["TaskId"]
        rtt_response.Request.ReplyQueue = (
            self.handler.cleanup_callback_queue_url
        )
        rtt_response.Request.ReplyId = str(meta.video_file_id)
        rtt_response.Request.FullRemove.SetInParent()
        rtt_response.RemovedFiles = 8
        rtt_response.RemovedBytes = 256

        message_string = rtt_response.SerializeToString()
        message_data = {"Body": encode_pb_message(message_string)}
        message = RawMessage(data=message_data)

        self.handler.handle(message)

        self.handler.db_client.list_video_meta.assert_called_with(
            video_file_id=int(rtt_response.Request.ReplyId), limit=20
        )
        self.handler.db_client.set_meta_deleted.assert_called_with(
            meta.id, value=True
        )
        self.handler.boto_client.send_message.assert_called_with(
            QueueUrl=self.handler.rtt_status_queue_url,
            MessageBody=json.dumps(
                {
                    "id": str(meta.video_file_id),
                    "type": "rtt_status",
                    "callback": "",
                    "data": {"TaskId": DUMMY_TASK_INFO["TaskId"]},
                }
            ),
            MessageGroupId=str(rtt_response.Request.ReplyId),
        )

    def test_full_remove_no_video_file_id(self):
        rtt_response = msgs_pb2.TResponse()
        rtt_response.Request.TaskId = DUMMY_TASK_INFO["TaskId"]
        rtt_response.Request.ReplyQueue = (
            self.handler.cleanup_callback_queue_url
        )
        rtt_response.Request.FullRemove.SetInParent()
        rtt_response.RemovedFiles = 8
        rtt_response.RemovedBytes = 256

        message_string = rtt_response.SerializeToString()
        message_data = {"Body": encode_pb_message(message_string)}
        message = RawMessage(data=message_data)

        self.handler.handle(message)

    def test_new_playlist(self):
        meta = VideoMeta(**DUMMY_VIDEO_META)

        self.handler.boto_client.send_message = Mock()
        self.handler.rtt_client.remove_streams = Mock()

        rtt_response = msgs_pb2.TResponse()
        rtt_response.Request.TaskId = DUMMY_TASK_INFO["TaskId"]
        rtt_response.Request.ReplyQueue = (
            self.handler.cleanup_callback_queue_url
        )
        rtt_response.Request.ReplyId = str(meta.video_file_id)
        rtt_response.Request.NewPlaylist.SetInParent()

        message_string = rtt_response.SerializeToString()
        message_data = {"Body": encode_pb_message(message_string)}
        message = RawMessage(data=message_data)

        self.handler.handle(message)

        self.handler.boto_client.send_message.assert_called_with(
            QueueUrl=self.handler.rtt_status_queue_url,
            MessageBody=json.dumps(
                {
                    "id": str(meta.video_file_id),
                    "type": "rtt_status",
                    "callback": "",
                    "data": {"TaskId": DUMMY_TASK_INFO["TaskId"]},
                }
            ),
            MessageGroupId=str(rtt_response.Request.ReplyId),
        )

        self.handler.rtt_client.remove_streams.assert_called_with(
            task_id=rtt_response.Request.TaskId,
            callback_queue_url=self.handler.cleanup_callback_queue_url,
            method=CleanupMethod.REMOVE_UNUSED,
            reply_id=rtt_response.Request.ReplyId,
        )

    def test_remove_unused(self):
        meta = VideoMeta(**DUMMY_VIDEO_META)

        self.handler.boto_client.send_message = Mock()

        rtt_response = msgs_pb2.TResponse()
        rtt_response.Request.TaskId = DUMMY_TASK_INFO["TaskId"]
        rtt_response.Request.ReplyQueue = (
            self.handler.cleanup_callback_queue_url
        )
        rtt_response.Request.ReplyId = str(meta.video_file_id)
        rtt_response.Request.RemoveUnused.SetInParent()
        rtt_response.RemovedFiles = 8
        rtt_response.RemovedBytes = 256

        message_string = rtt_response.SerializeToString()
        message_data = {"Body": encode_pb_message(message_string)}
        message = RawMessage(data=message_data)

        self.handler.handle(message)

        self.handler.boto_client.send_message.assert_called_with(
            QueueUrl=self.handler.rtt_status_queue_url,
            MessageBody=json.dumps(
                {
                    "id": str(meta.video_file_id),
                    "type": "rtt_status",
                    "callback": "",
                    "data": {"TaskId": DUMMY_TASK_INFO["TaskId"]},
                }
            ),
            MessageGroupId=str(rtt_response.Request.ReplyId),
        )

    def test_error_message(self):
        meta = VideoMeta(**DUMMY_VIDEO_META)

        rtt_response = msgs_pb2.TResponse()
        rtt_response.Request.TaskId = DUMMY_TASK_INFO["TaskId"]
        rtt_response.Request.ReplyQueue = (
            self.handler.cleanup_callback_queue_url
        )
        rtt_response.Request.ReplyId = str(meta.video_file_id)
        rtt_response.Request.FullRemove.SetInParent()
        rtt_response.Error = "RTT error message"

        message_string = rtt_response.SerializeToString()
        message_data = {"Body": encode_pb_message(message_string)}
        message = RawMessage(data=message_data)

        self.handler.handle(message)
