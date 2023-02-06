import json
import pytest
from unittest import mock

from google.protobuf import text_format

from extsearch.video.ugc.sqs_moderation import config_utils
import extsearch.video.ugc.service.protos.service_pb2 as service_pb2
from extsearch.video.ugc.sqs_moderation.mod2.handlers.meta_notify_dispatcher.data_extender import VideoDataMaker
from extsearch.video.ugc.sqs_moderation.clients.db_client.api_schema import (
    video_meta_schema, channel_schema, video_info_schema, video_stream_schema
)

from .utills import load_data, source_path


@pytest.fixture(scope='session')
def mock_signatures_text_data():
    with open(source_path(r'resources/video_data/signatures.json'), r'rb') as f:
        return f.read()


@pytest.fixture(scope='session')
def mock_signatures_data(mock_signatures_text_data):
    return json.loads(mock_signatures_text_data)


@pytest.fixture(scope='session')
def signature_maker_mock(mock_signatures_data):
    signature_maker = mock.Mock()
    signature_maker.get_signatures = mock.Mock(return_value=mock_signatures_data)
    return signature_maker


@pytest.fixture(scope='session')
def clients_config():
    clients_config = service_pb2.TVHUploadLinkFactoryConfig()
    with open(source_path('resources/video_data/test_client.cfg'), r'r', encoding='utf8') as f:
        text_format.Parse(f.read(), clients_config)
    return clients_config


@pytest.fixture(scope='session')
def notify_configs(clients_config):
    return config_utils.notify_configs(clients_config)


@pytest.fixture(scope='session')
def video_maker(db_client, signature_maker_mock, notify_configs):

    return VideoDataMaker(
        db_client=db_client,
        notify_configs=notify_configs,
        signature_maker=signature_maker_mock
    )


@pytest.fixture(scope='session')
def video_meta_id():
    return 9982471538825831858


@pytest.fixture(scope='session')
def stream_meta_id():
    return 6003922774413904656


@pytest.fixture(scope='session')
def video_meta(video_meta_id):
    meta = load_data('resources/video_data/video_meta.json', video_meta_schema)
    meta.id = int(video_meta_id)
    return meta


@pytest.fixture(scope='session')
def video_channel():
    return load_data('resources/video_data/channel.json', channel_schema)


@pytest.fixture(scope='session')
def stream_meta(stream_meta_id):
    meta = load_data('resources/video_data/stream_meta.json', video_meta_schema)
    meta.id = int(stream_meta_id)
    return meta


@pytest.fixture(scope='session')
def meta_dict(video_meta, stream_meta):
    return {
        video_meta.id: video_meta,
        stream_meta.id: stream_meta
    }


@pytest.fixture(scope='session')
def video_info(thumb_url):
    video = load_data('resources/video_data/video_file.json', video_info_schema)
    video.transcoder_info['SignaturesUrl'] = thumb_url
    return video


@pytest.fixture(scope='session')
def stream_info():
    stream = load_data('resources/video_data/video_stream.json', video_stream_schema)
    return stream


@pytest.fixture
def video_base_url():
    return 'http://test.ru/'
