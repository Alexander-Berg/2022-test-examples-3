
from extsearch.video.ugc.sqs_moderation.clients.redis.notification_status import NotificationStatus
from extsearch.video.ugc.sqs_moderation.clients.db_client import TranscoderStatus


def test_notification_status_empty(notification_client, new_task):
    assert notification_client.need_notification(new_task, TranscoderStatus.ETSDone) is True
    notification_client.update_notification_status(new_task, TranscoderStatus.ETSDone)
    assert notification_client.need_notification(new_task, TranscoderStatus.ETSDone) is False


def test_notification_status_transcoding(notification_client, transcoding_task):
    assert notification_client.need_notification(transcoding_task, TranscoderStatus.ETSDone) is True
    assert notification_client.need_notification(transcoding_task, TranscoderStatus.ETSLowResTranscoded) is True


def test_notification_status_done(notification_client, done_task):
    assert notification_client.need_notification(done_task, TranscoderStatus.ETSDeleted) is True
    assert notification_client.need_notification(done_task, TranscoderStatus.ETSTranscoding) is False
    assert notification_client.need_notification(done_task, TranscoderStatus.ETSDone) is False


def test_notification_status():
    status = NotificationStatus('test', [TranscoderStatus.ETSTranscoding])
    assert status.lower(TranscoderStatus.ETSDone) is True
    assert status.lower(TranscoderStatus.ETSLowResTranscoded) is True
    assert status.lower(TranscoderStatus.ETSDeleted) is True
    assert status.lower(TranscoderStatus.Empty) is False
    assert status.lower(TranscoderStatus.ETSQueued) is False
    assert status.lower(TranscoderStatus.ETSPreprocessing) is False
    assert status.lower(TranscoderStatus.ETSTranscoding) is False
