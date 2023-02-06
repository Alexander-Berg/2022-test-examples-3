import copy
import pytest

from extsearch.video.ugc.sqs_moderation.clients.index_client import VideoState
from extsearch.video.ugc.sqs_moderation.clients.db_client.resources import CmntData
from extsearch.video.ugc.sqs_moderation.mod2.handlers.meta_notify_dispatcher.data_formatter import (
    UGCDocFormatter, HTTPCallbackFormatter, RightHolderFormatter, SignatureFormatter
)
from extsearch.video.ugc.sqs_moderation.mod2.handlers.meta_notify_dispatcher.message import (
    NotifySQSMessage, MetaNotifyMessage, ExtendedSQSMessageData, NotificationServices
)
from extsearch.video.ugc.sqs_moderation.clients.streams import StreamGenerator


@pytest.fixture
def inherited_effects():
    return [
        {'service': 'ui', 'effect': 'green_url_available', 'data': {}, 'target': 'channel', 'from_time': 1611666540},
        {'service': 'ui', 'effect': 'green_url_available', 'data': {}, 'target': 'channel'},
        {'service': 'callback', 'effect': 'premium', 'data': {'source': 'cv'}, 'target': 'video'}
    ]


@pytest.fixture
def video_effects():
    return [{'service': 'callback', 'effect': 'premium', 'data': {'premium': 'cv'}}]


@pytest.fixture
def cmnt_data_obj(cmnt_data):
    return CmntData(**cmnt_data)


@pytest.fixture
def ugc_doc_formatter():
    return UGCDocFormatter()


@pytest.fixture
def stream_generator():
    generator = StreamGenerator('111', 100)
    return generator


@pytest.fixture
def http_callback_formatter(stream_generator):
    return HTTPCallbackFormatter(stream_generator)


@pytest.fixture
def meta_notify_message(valid_meta_id):
    return MetaNotifyMessage(meta_id=valid_meta_id, notification_services=[
        service for service in NotificationServices if service != NotificationServices.DEV_NULL
    ])


@pytest.fixture
def stream_meta_notify_message(stream_meta_id):
    return MetaNotifyMessage(meta_id=stream_meta_id, notification_services=[
        service for service in NotificationServices if service != NotificationServices.DEV_NULL
    ])


@pytest.fixture
def video_status():
    return VideoState.ready


@pytest.fixture
def video_extended_sqs_message_data(
    video_meta, video_channel, tags, valid_cmnt_res, video_effects, inherited_effects,
    cmnt_data_obj, thumb_data_info, video_status, notify_configs, mock_signatures_data,
    video_base_url, video_info
):
    resources = [valid_cmnt_res]
    return ExtendedSQSMessageData(
        meta=video_meta,
        channel=video_channel,
        tags=tags,
        resources=resources,
        video_effects=video_effects,
        inherited_effects=inherited_effects,
        cmnt_data=cmnt_data_obj,
        thumb_info=thumb_data_info,
        status=video_status,
        notify_cfg=notify_configs[video_info.service],
        signatures=mock_signatures_data,
        video_base_url=video_base_url,
        video_data=video_info,
    )


@pytest.fixture
def formatter_test_msg_video(meta_notify_message, video_extended_sqs_message_data):
    msg = NotifySQSMessage(meta_notify_message)
    msg.extended_data = video_extended_sqs_message_data
    return msg


@pytest.fixture
def common_rule_test_video(formatter_test_msg_video):
    tmp_video = copy.deepcopy(formatter_test_msg_video)
    tmp_video.extended_data.video_data.transcoder_status = 'ETSDeleted'
    return tmp_video


@pytest.fixture
def age_test_video(formatter_test_msg_video):

    tmp_video = copy.deepcopy(formatter_test_msg_video)

    tmp_video.extended_data.notify_cfg.max_age_for_notification = 30
    tmp_video.extended_data.notify_cfg.upload_to_lb = True
    tmp_video.extended_data.video_data.service = 'games'

    return tmp_video


@pytest.fixture
def stream_extended_sqs_message_data(
    stream_meta, video_channel, tags, valid_cmnt_res, video_effects, inherited_effects,
    cmnt_data_obj, thumb_data_info, video_status, notify_configs, mock_signatures_data,
    video_base_url, stream_info
):
    resources = [valid_cmnt_res]
    return ExtendedSQSMessageData(
        meta=stream_meta,
        channel=video_channel,
        tags=tags,
        resources=resources,
        video_effects=video_effects,
        inherited_effects=inherited_effects,
        cmnt_data=cmnt_data_obj,
        thumb_info=thumb_data_info,
        status=video_status,
        notify_cfg=notify_configs[stream_info.service],
        signatures=mock_signatures_data,
        video_base_url=video_base_url,
        video_data=stream_info,
    )


@pytest.fixture
def formatter_test_msg_stream(stream_meta_notify_message, stream_extended_sqs_message_data):
    msg = NotifySQSMessage(stream_meta_notify_message)
    msg.extended_data = stream_extended_sqs_message_data
    return msg


@pytest.fixture
def signature_formatter():
    return SignatureFormatter()


@pytest.fixture
def right_holder_formatter():
    return RightHolderFormatter()
