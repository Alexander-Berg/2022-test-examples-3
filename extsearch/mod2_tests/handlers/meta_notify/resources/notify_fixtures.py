import pytest

from extsearch.video.ugc.sqs_moderation.mod2.handlers.meta_notify_dispatcher.http_notifier import (
    HTTPNotifyMessage, HTTPNotifier
)
from extsearch.video.ugc.sqs_moderation.mod2.handlers.meta_notify_dispatcher.deduplicator import MessageCondition
from extsearch.video.ugc.sqs_moderation.models.messages import HTTPNotifyData
from extsearch.video.ugc.sqs_moderation.clients.callback_notifier import CallbackNotifier


@pytest.fixture
def callback_notifier(session, clients_config, mock_tvm):
    return CallbackNotifier(
        session=session,
        clients=clients_config,
        tvm_client=mock_tvm
    )


@pytest.fixture
def http_notify_worker(callback_notifier, boto_client, deduplicator):
    return HTTPNotifier(
        callback_notifier=callback_notifier,
        deduplicator=deduplicator,
    )


@pytest.fixture
def http_notify_data(formatter_test_msg_video):
    return HTTPNotifyData(
        id=formatter_test_msg_video.extended_data.video_data.service_id,
        video_meta_id=formatter_test_msg_video.extended_data.meta.id,
        short_id='testtesttest',
        player_url='',
        stream_url='',
        thumbnail=formatter_test_msg_video.extended_data.meta.thumbnail,
        duration_ms=0,
        screenshots=['', ''],
        transcoder_status_str='OK',
        status='111',
        height=100,
        width=100,
        effects=formatter_test_msg_video.extended_data.video_effects
    )


@pytest.fixture
def http_notify_msg_games(http_notify_data, formatter_test_msg_video):
    http_notificatiable_service = {
        'service': 'games',
        'service_id': '1'
    }
    return HTTPNotifyMessage(
        meta_id=formatter_test_msg_video.meta_id,
        file_id=formatter_test_msg_video.file_id,
        **http_notificatiable_service,
        notify_data=http_notify_data
    )


@pytest.fixture
def http_notify_msg_games_testing(http_notify_data, formatter_test_msg_video):
    http_notificatiable_service = {
        'service': 'games-testing',
        'service_id': '1'
    }
    return HTTPNotifyMessage(
        meta_id=formatter_test_msg_video.meta_id,
        file_id=formatter_test_msg_video.file_id,
        **http_notificatiable_service,
        notify_data=http_notify_data
    )


@pytest.fixture
def http_notify_msg_maps_testing(http_notify_data, formatter_test_msg_video):
    http_notificatiable_service = {
        'service': 'maps-testing',
        'service_id': '1'
    }
    return HTTPNotifyMessage(
        meta_id=formatter_test_msg_video.meta_id,
        file_id=formatter_test_msg_video.file_id,
        **http_notificatiable_service,
        notify_data=http_notify_data
    )


@pytest.fixture
def http_notify_msg_condition(http_notify_msg_games, mock_redis):
    return MessageCondition(http_notify_msg_games.deduplication_info, mock_redis)
