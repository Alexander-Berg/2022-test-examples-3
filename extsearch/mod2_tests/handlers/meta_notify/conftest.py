import pytest
from unittest import mock
from requests import Response

from extsearch.video.ugc.sqs_moderation.clients.db_client import DbClient
from extsearch.video.ugc.sqs_moderation.clients.client_manager.http_session import create_session
from extsearch.video.ugc.sqs_moderation.mod2.app import create_fake_boto

from .resources.utills import source_path

pytest_plugins = (
    "resources.video_maker_fixtures",
    "resources.cmnt_fixtures",
    "resources.thumb_fixtures",
    "resources.effect_fixtures",
    "resources.signature_maker_fixtures",
    "resources.data_extender_fixtures",
    "resources.formatter_fixture",
    "resources.rule_fixtures",
    "resources.notify_fixtures",
    "resources.deduplicator_fixtures",
    "resources.clients_fixtures",
    "resources.dispatcher_fixtures",
)


@pytest.fixture(scope='session')
def mock_db_client_request(video_info, channel_spells, meta_spells, spell_templates, tags, video_channel, meta_dict,
                           valid_cmnt_res, stream_info):
    def wrapper(url, method, data, params, *args, **kwargs):
        if 'spells' in url:
            return effect_response_spells(params, channel_spells, meta_spells)
        elif 'spell_templates' in url:
            return get_spell_templates(url, spell_templates)
        elif 'video_file' in url:
            return video_info
        elif 'video_tag' in url:
            return tags
        elif 'channel' in url:
            return video_channel
        elif 'video_meta' in url:
            return get_video_meta(url, meta_dict)
        elif 'video_resource' in url:
            return [valid_cmnt_res]
        elif 'video_stream' in url:
            return stream_info
        else:
            raise ValueError(f'No definition for url: {url}')

    return wrapper


def get_video_meta(url: str, meta: dict):
    for key in meta.keys():
        if str(key) in url:
            return meta.get(key)


def effect_response_spells(params, channel_spells, meta_spells):
    if 'user_uid' in params:
        return []
    elif 'channel_id' in params:
        return channel_spells
    elif 'video_meta_id' in params:
        return meta_spells
    else:
        return []


def get_spell_templates(url, templates):
    tmpl_id = None if len(url.split('/')) == 1 else url.split('/')[-1]
    if not tmpl_id:
        return templates
    else:
        return next((t for t in templates if t.id == tmpl_id))


@pytest.fixture(scope='session')
def mock_session_get(signature_response, signature_error_response, signature_response_wrong_content,
                     thumb_key, thumb_key_404):
    def wrapper(session, url: str, *args, **kwargs):
        if thumb_key in url:
            return valid_thumb_data_response()
        elif thumb_key_404 in url:
            return resp_404()
        elif 'test_sig_valid' in url:
            return signature_response
        elif 'test_sig_err' in url:
            return signature_error_response
        elif 'test_sig_wrong' in url:
            return signature_response_wrong_content

    return wrapper


@pytest.fixture(scope='session')
def mock_session_post():
    def wrapper(session, url: str, *args, **kwargs):
        if 'localhost:32456/callback' in url:
            return resp_200()
        elif 'maps.yandex-team.ru/video/callback' in url:
            return resp_404()
        elif 'admin-api.games-test.yandex.ru/vh/v1/callback' in url:
            return response(403)
    return wrapper


def response(status_code):
    resp = Response()
    resp.status_code = status_code
    return resp


def resp_404():
    return response(404)


def resp_200():
    return response(200)


def valid_thumb_data_response():
    resp = Response()
    resp.status_code = 200
    with open(source_path(r'resources/thumb/thumbnaildata_.json'), r'rb') as f:
        resp._content = f.read()
    return resp


@pytest.fixture(scope='session')
def session(mock_session_get, mock_session_post):
    with mock.patch.multiple('requests.Session', get=mock_session_get, post=mock_session_post):
        yield create_session()


@pytest.fixture(scope='session')
def mock_tvm():
    tvm = mock.Mock()
    tvm.get_service_ticket_for = mock.Mock(return_value='ticket')
    return tvm


@pytest.fixture(scope='session')
def db_client(session, mock_db_client_request, mock_tvm):
    with mock.patch(
            'extsearch.video.ugc.sqs_moderation.clients.db_client.base.BaseClient._request',
            side_effect=mock_db_client_request):
        yield DbClient(
            base_url='https://test.ya',
            session=session,
            tvm_client=mock_tvm
        )


@pytest.fixture
def mock_index_write():
    return mock.Mock()


@pytest.fixture
def mock_pq():
    with mock.patch('kikimr.public.sdk.python.persqueue.grpc_pq_streaming_api.PQStreamingAPI.start'):
        with mock.patch('extsearch.video.vh.vhs_lib.pq_writer.PQTopicWriter'):
            yield


@pytest.fixture
def index_pg_client():
    return mock.Mock()


@pytest.fixture
def pg_notifier():
    return mock.Mock()


@pytest.fixture
def robot_pq_client():
    return mock.Mock()


@pytest.fixture
def boto_client():
    boto = create_fake_boto('acc', 'token')

    def send_message(self, QueueUrl: str, MessageBody: str, *_, **__):
        self.messages[QueueUrl].append(MessageBody)

    boto.messages['fake://http_queue'] = []
    boto.messages['fake://meta_notify_queue'] = []
    with mock.patch.object(
        boto,
        'send_message',
        side_effect=lambda *args, **kwargs: send_message(boto, *args, **kwargs)
    ):
        yield boto
