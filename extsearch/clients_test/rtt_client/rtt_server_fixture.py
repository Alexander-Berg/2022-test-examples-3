import json
import uuid
from urllib import parse
from unittest import mock

from requests import Response
import pytest

from extsearch.video.ugc.sqs_moderation.clients.client_manager.http_session import create_session
from extsearch.video.ugc.sqs_moderation.clients.rtt import RttSqsClient


@pytest.fixture(scope='session')
def test_faas_host():
    return 'http://faas-test.mock'


@pytest.fixture(scope='session')
def session(mock_session_get, mock_session_post):
    with mock.patch.multiple('requests.Session', get=mock_session_get, post=mock_session_post):
        yield create_session()


@pytest.fixture(scope='session')
def mock_session_get(mock_faas_server, test_faas_host):
    def wrapper(session, url: str, *args, **kwargs):
        if test_faas_host not in url:
            raise NotImplementedError('Url %s not implemented in mock session', url)
        return mock_faas_server.get(url, *args, **kwargs)
    return wrapper


@pytest.fixture(scope='session')
def mock_session_post(mock_faas_server, test_faas_host):
    def wrapper(session, url: str, *args, **kwargs):
        if test_faas_host not in url:
            raise NotImplementedError('Url %s not implemented in mock session', url)
        return mock_faas_server.post(url, *args, **kwargs)

    return wrapper


class MockFaasServer:

    def __init__(self, tasks: list, video_id: dict):
        self.tasks = tasks
        self.video_id = video_id
        self.video_id_map = {id_: err_code for err_code, id_ in video_id.items()}

    def get(self, url, *args, **kwargs):
        if 'get-task' not in url:
            return response(404)
        prams = self._parse_url(url)
        return self._get_task(prams, *args, **kwargs)

    def post(self, url, *args, **kwargs):
        if 'create-task' not in url:
            return response(404)
        data = kwargs.get('json')
        if not data:
            return response(400)
        prams = self._parse_url(url)
        return self._create_task(prams, data, *args, **kwargs)

    def _parse_url(self, url: str) -> dict:
        params = {}
        parsed = parse.urlparse(url)
        if parsed.query:
            for part in parsed.query.split('&'):
                k, v = part.split('=')
                params[k] = v
        return params

    def _create_task(self, prams, data, *args, **kwargs):
        resp = response(200)
        if code := data.get('http_err'):
            return response(code)
        video_id = data.get('ExternalID')
        code = self.video_id_map.get(video_id)
        if code == 0 or code is None:
            task = {'task_id': str(uuid.uuid4())}
            self.tasks.append(task)
            resp._content = json.dumps(task).encode('utf8')
        else:
            resp._content = json.dumps({'Error': code}).encode('utf8')
        return resp

    def _get_task(self, params, *args, **kwargs):
        task_id = params.get('id')
        if not task_id:
            return response(400)
        if 'http_err_' in task_id:
            *_, code = task_id.split('_')
            return response(int(code))
        task = next(filter(lambda x: x.get('task_id') == task_id, self.tasks), None)
        if not task:
            return response(404)
        resp = response(200)
        resp._content = json.dumps(task).encode('utf8')
        return resp


@pytest.fixture(scope='session')
def mock_faas_server(tasks: list, video_id):
    return MockFaasServer(tasks, video_id)


def response(status_code):
    resp = Response()
    resp.status_code = status_code
    return resp


@pytest.fixture(scope='session')
def rtt_client(session, test_faas_host):
    boto_mock = mock.Mock()
    boto_mock.get_queue_url = mock.Mock(return_value={'QueueUrl': ''})
    return RttSqsClient(test_faas_host, {'default': 'test'}, '', boto_mock, session)
