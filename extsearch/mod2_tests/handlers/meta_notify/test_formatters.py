import json

from extsearch.video.ugc.sqs_moderation.clients.pq import to_dict
from extsearch.video.ugc.sqs_moderation.clients.index_client import UGCDoc


def test_ugc_formatter_get_cmnt_id(ugc_doc_formatter, cmnt_data_obj):
    data = ugc_doc_formatter._get_cmnt_id(cmnt_data_obj)
    json.dumps(data)
    assert data == cmnt_data_obj.cmnt_id


def test_ugc_formatter_get_cmnt_id_none(ugc_doc_formatter):
    data = ugc_doc_formatter._get_cmnt_id(None)
    json.dumps(data)
    assert data is None


def test_ugc_formatter_get_video_data_video(ugc_doc_formatter, video_info):
    data = ugc_doc_formatter._get_video_data(video_info, True)
    json.dumps(data)
    assert 'video_service' in data
    assert 'video_service_id' in data
    assert 'source_url' in data
    assert 'video_create_time' in data


def test_ugc_formatter_video(ugc_doc_formatter, formatter_test_msg_video):
    ugc_doc_formatter.dump_message(formatter_test_msg_video)


def test_ugc_formatter_stream(ugc_doc_formatter, formatter_test_msg_stream):
    ugc_doc_formatter.dump_message(formatter_test_msg_stream)


def test_ugc_formatter_to_dict(ugc_doc_formatter, formatter_test_msg_video):
    data = ugc_doc_formatter.dump_message(formatter_test_msg_video)
    assert isinstance(to_dict(UGCDoc(**data)), dict)


def test_http_callback_formatter(http_callback_formatter, formatter_test_msg_video):
    data = http_callback_formatter.dump_message(formatter_test_msg_video)
    json.dumps(data)
    assert data.get('notify_data').get('video_meta_id') == formatter_test_msg_video.meta_id


def test_signature_formatter_video(signature_formatter, formatter_test_msg_video):
    data = signature_formatter.dump_message(formatter_test_msg_video)
    json.dumps(data)


def test_rightholder_formatter_video(right_holder_formatter, formatter_test_msg_video):
    data = right_holder_formatter.dump_message(formatter_test_msg_video)
    json.dumps(data)
