# -*- coding: utf-8 -*-

import pytest
import flask
from market.proto import SessionMetadata_pb2

from hamcrest import (
    assert_that,
    contains_string,
)

from utils import (
    is_success_response,
    is_error_response,
)


from market.idx.api.backend.idxapi import create_flask_app
from market.idx.api.backend.marketindexer.storage.storage import Storage


class StorageMock(Storage):
    def get_feed_qsession(self, feed_id, storage_type='hbase'):
        if feed_id != '1069':
            return None

        session = {
            'session_id': '1069_00000000_0000',
            'data:deliverycalc_generation': '7806',
            'data:descrobot_start': '2017-05-16T14:46:31',
            'data:feed_checksum': '6be46a8f6de880eef2120196f34e657c',
            'data:finish_time': '2017-05-16T14:28:28',
            'data:published': 'True',
            'data:url_for_log': 'test.yandex.ru',
            'diff:heartbeat': '2017-05-16T09:54:48',
            'diff:start': '2017-05-16T09:54:48',
            'diff:status': 'processed',
            'diff:stop': '2017-05-16T09:54:48',
        }

        meta = SessionMetadata_pb2.OffersRobot()
        meta.download_retcode = 0
        meta.download_status = '200 OK'
        meta.feedparser.classifier_feed_id = 'be46a8f6de880eef2120196f34e657c'
        meta.feedparser.currencies.add()
        meta.feedparser.currencies[0].rate = 1.0
        meta.feedparser.currencies[0].currency = 'RUR'
        meta.mbi_params = '<?xml version="1.0" encoding="utf-8"?><map><datafeed_id>1069</datafeed_id></map>'
        meta.parser_retcode = 0
        meta.parser_stdout = '[2017-05-17 12:52:25] [3:21] (Message) 162 YMLDATE= #json#{\"posColumn\":21...'
        session['data:meta'] = meta

        return session


@pytest.fixture(scope='module')
def test_app():
    return create_flask_app(StorageMock())


def _test_get_qsession_by_feed_id_not_found(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/feeds/1/quick/session')
        assert_that(resp, is_error_response('404 Not Found', 404))


def _test_get_qsession_by_feed_id(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/feeds/1069/quick/session')
        assert_that(resp, is_success_response())

        data = flask.json.loads(resp.data)
        assert data['session_id'] == '1069_00000000_0000'
        assert data['self_url'] == 'http://localhost:29334/v1/feeds/1069/quick/session'
        assert data['qoffers_url'] == 'http://localhost:29334/v1/feeds/1069/quick/session/offers'

        assert data['data:deliverycalc_generation'] == '7806'
        assert data['data:descrobot_start'] == '2017-05-16T14:46:31'
        assert data['data:feed_checksum'] == '6be46a8f6de880eef2120196f34e657c'
        assert data['data:finish_time'] == '2017-05-16T14:28:28'
        assert data['data:published'] is True
        assert data['data:url_for_log'] == 'test.yandex.ru'
        assert data['diff:heartbeat'] == '2017-05-16T09:54:48'
        assert data['diff:start'] == '2017-05-16T09:54:48'
        assert data['diff:status'] == 'processed'
        assert data['diff:stop'] == '2017-05-16T09:54:48'

        assert data['data:meta']
        assert data['data:meta']['download_retcode'] == 0
        assert data['data:meta']['download_status'] == '200 OK'
        assert data['data:meta']['feedparser']
        assert data['data:meta']['feedparser']['classifier_feed_id'] == 'be46a8f6de880eef2120196f34e657c'
        assert data['data:meta']['feedparser']['currencies']
        assert len(data['data:meta']['feedparser']['currencies']) == 1
        assert data['data:meta']['feedparser']['currencies'][0]['rate'] == 1.0
        assert data['data:meta']['feedparser']['currencies'][0]['currency'] == 'RUR'

        assert data['data:meta']['mbi_params']
        assert data['data:meta']['mbi_params']['datafeed_id'] == 1069
        assert data['data:meta']['parser_retcode'] == 0
        assert data['data:meta'][
            'parser_stdout'] == '[2017-05-17 12:52:25] [3:21] (Message) 162 YMLDATE= #json#{\"posColumn\":21...'


def _test_parse_format_json(test_app):
    with test_app.test_request_context('/v1/feeds/1069/quick/session?format=json'):
        assert flask.request.path == '/v1/feeds/1069/quick/session'
        assert flask.request.args['format'] == 'json'


def _test_parse_format_xml(test_app):
    with test_app.test_request_context('/v1/feeds/1069/quick/session?format=xml'):
        assert flask.request.path == '/v1/feeds/1069/quick/session'
        assert flask.request.args['format'] == 'xml'


def _test_response_headers_default_json(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/feeds/1069/quick/session')
        assert_that(resp, is_success_response(content_type='application/json; charset=utf-8'))


def _test_response_headers_format_json(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/feeds/1069/quick/session?format=json')
        assert_that(resp, is_success_response(content_type='application/json; charset=utf-8'))


def _test_response_headers_format_xml(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/feeds/1069/quick/session?format=xml')
        assert_that(
            resp,
            is_success_response(
                data=contains_string('<?xml version="1.0" encoding="utf-8"?>'),
                content_type='application/xml; charset=utf-8'
            )
        )


def _test_response_headers_format_undef(test_app):
    with test_app.test_client() as client:
        resp = client.get('/v1/feeds/1069/quick/session?format=someformat')
        assert_that(resp, is_error_response('406 Not Acceptable\nrequest mime type is not implemented: someformat', 406))
