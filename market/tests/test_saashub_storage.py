# coding: utf-8
import pytest
import mock
from market.idx.pylibrary.saashub.saashub_storage import SaashubStorage

HOST = 'saashub'
PORT = 13333
SAASHUB_URL = 'http://{}:{}'.format(HOST, PORT)


@pytest.fixture(scope='session')
def saashub_storage():
    return SaashubStorage(HOST, PORT)


class MockResponse:
    def __init__(self, json_data, status_code, exception=None):
        self.json_data = json_data
        self.status_code = status_code
        self.exception = exception

    def raise_for_status(self):
        if self.exception is not None:
            raise Exception

    def json(self):
        return self.json_data


def get_response_function(url_data):
    # noinspection PyUnusedLocal
    def mocked_session_get(*args, **kwargs):
        url = args[0]
        if url in url_data:
            return MockResponse(url_data[url]['data'], url_data[url]['status_code'])

        return MockResponse(None, 404)
    return mocked_session_get


# noinspection PyUnusedLocal
def response_with_http_status_error(*args, **kwargs):
    return MockResponse(None, 404)


# noinspection PyUnusedLocal
def response_function_with_exception(*args, **kwargs):
    raise Exception('Request failed')


def get_doc_state_url(feed_id, offer_id):
    return '{}/doc_state/{}/{}'.format(SAASHUB_URL, feed_id, offer_id)


# Валидный запрос реального фида, валидный ответ.
def test_correct_response_real_feed_id(saashub_storage):
    # На запрос http://saashub:80/doc_state/1234/223 придёт ответ {'1234/223/': 'someData'}
    with mock.patch('market.idx.pylibrary.saashub.saashub_storage.Session.get') as mock_get:
        mock_get.side_effect = get_response_function({get_doc_state_url(1234, '223'): {
            'data': {'1234/223/': 'someData'},
            'status_code': 200
        }})

        doc_state = saashub_storage.get_doc_state(1234, '223')

        assert doc_state == 'someData'


# Валидный запрос виртуального фида, валидный ответ.
def test_correct_response_virtual_feed_id(saashub_storage):
    # На запрос http://saashub:80/doc_state/1234/223.33.55 придёт ответ 404
    # На запрос http://saashub:80/doc_state/223/33.55 придёт ответ {'223/33.55/': 'someData'}
    with mock.patch('market.idx.pylibrary.saashub.saashub_storage.Session.get') as mock_get:
        mock_get.side_effect = get_response_function({
            get_doc_state_url(1234, '223.33.55'): {'data': '', 'status_code': 404},
            get_doc_state_url(223, '33.55'): {'data': {'223/33.55/': 'someData'}, 'status_code': 200}})

        doc_state = saashub_storage.get_doc_state(1234, '223.33.55')

        assert doc_state == 'someData'


# Исключение из Session.get
def test_request_exception(saashub_storage):
    with mock.patch('market.idx.pylibrary.saashub.saashub_storage.Session.get') as mock_get:
        mock_get.side_effect = response_function_with_exception

        with pytest.raises(Exception):
            saashub_storage.get_doc_state(1234, '223')


# Если SaaS-hub возвращает 404, то нет исколючения, но возвращается None
def test_status_exception(saashub_storage):
    # Saashub отвечает 404 всегда
    with mock.patch('market.idx.pylibrary.saashub.saashub_storage.Session.get') as mock_get:
        mock_get.side_effect = response_with_http_status_error

        doc_state = saashub_storage.get_doc_state(1234, '223')
        assert doc_state is None


# Некорректный ответ от SaasHub
def test_exception_incorrect_response(saashub_storage):
    # На запрос http://saashub:80/doc_state/1234/223 придёт некорректный ответ {'': 'someData'}
    with mock.patch('market.idx.pylibrary.saashub.saashub_storage.Session.get') as mock_get:
        mock_get.side_effect = get_response_function({
            get_doc_state_url(1234, '223'): {'data': {'': 'someData'}, 'status_code': 200}})

        with pytest.raises(Exception):
            saashub_storage.get_doc_state(1234, '223')
