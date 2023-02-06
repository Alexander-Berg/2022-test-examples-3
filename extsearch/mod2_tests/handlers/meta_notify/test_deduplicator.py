import pytest

from extsearch.video.ugc.sqs_moderation.models.messages import NotificationServices
from extsearch.video.ugc.sqs_moderation.mod2.handlers.meta_notify_dispatcher.deduplicator import MessageStatus


def test_message_condition_set_extend_retry_info(message_state):
    assert message_state.retry_info == []
    message_state.retry_info = [NotificationServices.INDEX]
    assert message_state.retry_info == [NotificationServices.INDEX]
    message_state.retry_info = [NotificationServices.HTTP_CALLBACK]
    assert message_state.retry_info == [NotificationServices.HTTP_CALLBACK]
    message_state.extend_retry_info([NotificationServices.INDEX])
    assert set(message_state.retry_info) == {NotificationServices.HTTP_CALLBACK, NotificationServices.INDEX}


def test_message_condition_set_retry_info_wrong_value(message_state):
    with pytest.raises(ValueError):
        message_state.retry_info = 1


def test_message_condition_set_retry_info(message_state):
    message_state.retry_info = [NotificationServices.INDEX]
    assert message_state.retry_info == [NotificationServices.INDEX]


def tet_message_condition_set_delay(message_state):
    message_state.delay_message()
    assert message_state.message_delayed is True


def test_message_condition_duplication_info(message_state, deduplicator_test_message):
    assert message_state.deduplication_info == deduplicator_test_message.deduplication_info


def test_message_condition_change_status(message_state):
    assert message_state.current_status == MessageStatus.NEW

    message_state.next_status()
    assert message_state.current_status == MessageStatus.IN_PROGRESS
    assert message_state.progress == b'1'
    assert message_state.ttl_progress == message_state.ttl

    message_state.next_status()
    assert message_state.current_status == MessageStatus.SENT_RETRY_MESSAGE
    assert message_state.delay_message_sent is True
