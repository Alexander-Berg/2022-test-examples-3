from pytest import mark
from unittest import mock

from extsearch.video.ugc.sqs_moderation.mod2.handlers.meta_notify_dispatcher.data_extender import VideoDataMaker
from extsearch.video.ugc.sqs_moderation.clients.db_client import Privacy, ModerationStatus, ChannelType
from extsearch.video.ugc.sqs_moderation.clients.index_client import VideoState


def test_read_video(video_maker, video_meta, video_channel, video_info, mock_signatures_data):
    data = video_maker.get_video_info(video_meta, video_channel)
    assert 'video_data' in data
    assert 'status' in data
    assert 'notify_cfg' in data
    assert 'signatures' in data

    assert getattr(data.get('video_data'), 'id') == 9982471538825831858
    assert getattr(data.get('status'), 'value') == 'ready'
    assert getattr(data.get('notify_cfg'), 'upload_to_index') is True
    assert data.get('signatures') == mock_signatures_data
    video_maker.signature_maker.get_signatures.assert_called_with(video_info)


# default fields value (in get_mock_meta, get_mock_channel) is:
# (
#         {'deleted': False, 'privacy': 'public', 'moderation_status': 'success'},
#         {'deleted': False, 'privacy': 'public', 'moderation_status': 'success', 'type': 'regular'}
# )
meta_not_active_data = [
    ({}, {}, False), ({'deleted': True}, {}, True),
    ({'privacy': 'banned'}, {}, True), ({'privacy': 'by_url'}, {}, True), ({'privacy': 'private'}, {}, True),
    ({'moderation_status': 'fail'}, {}, True), ({'moderation_status': 'old_success'}, {}, False),
    ({'moderation_status': 'partial_success'}, {}, False), ({'moderation_status': 'partial_success'}, {}, False),
    ({}, {'deleted': True}, True),
    ({}, {'privacy': 'banned'}, True), ({}, {'privacy': 'private'}, False), ({}, {'privacy': 'by_url'}, False),
    ({}, {'type': 'rightholder'}, True),
    ({}, {'moderation_status': 'partial_success'}, False), ({}, {'moderation_status': 'old_fail'}, False),
    ({}, {'moderation_status': 'error'}, False), ({}, {'moderation_status': 'manual_check'}, False),
]


def get_mock_meta(meta_data: dict):
    meta = mock.Mock()
    meta.deleted = meta_data.get('deleted', False)
    meta.privacy = Privacy(meta_data.get('privacy', 'public'))
    meta.moderation_status = ModerationStatus(meta_data.get('moderation_status', 'success'))

    return meta


def get_mock_channel(channel_data: dict):
    channel = mock.Mock()
    channel.deleted = channel_data.get('deleted', False)
    channel.privacy = Privacy(channel_data.get('privacy', 'public'))
    channel.moderation_status = ModerationStatus(channel_data.get('moderation_status', 'success'))
    channel.type = ChannelType(channel_data.get('type', 'regular'))

    return channel


@mark.parametrize("meta,channel,expected", meta_not_active_data)
def test_is_meta_not_active(meta: dict, channel: dict, expected: bool, video_maker: VideoDataMaker):
    meta_ = get_mock_meta(meta)
    channel_ = get_mock_channel(channel)
    assert video_maker._is_meta_not_active(meta_, channel_) == expected, 'Unexpected check meta active result!'


video_status_data = [
    (ModerationStatus('success'), 'ETSDone', VideoState.ready),
    (ModerationStatus('fail'), 'ETSDone', VideoState.deleted),
    (ModerationStatus('success'), 'ETSError', VideoState.deleted),
    (ModerationStatus('manual_check'), 'ETSDone', VideoState.unpublished),
]


@mark.parametrize("moderation_status,transcoder_status,expected", video_status_data)
def test_get_status_by_file(video_maker, video_meta, video_channel, moderation_status, transcoder_status, expected):
    video_info = mock.Mock()
    video_info.moderation_status = ModerationStatus(moderation_status)
    video_info.transcoder_status = transcoder_status

    status = video_maker._get_status_by_file(meta=video_meta, channel=video_channel, video_file=video_info)
    assert status == expected


stream_status_data = [
    ('onair', VideoState.ready),
    ('finished', VideoState.ready),
    ('frfrfr', VideoState.deleted),
]


@mark.parametrize("stream_state,expected_state", stream_status_data)
def test_get_status_by_stream(video_maker, video_meta, video_channel, stream_state, expected_state):
    stream_info = mock.Mock()
    stream_info.state = stream_state

    status = video_maker._get_status_by_stream(meta=video_meta, channel=video_channel, stream=stream_info)
    assert status == expected_state
