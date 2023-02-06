import logging

import httpretty
import pytest
import requests
import responses
from hamcrest import assert_that, has_entries
from requests import ConnectTimeout, HTTPError
from requests.exceptions import RetryError, ConnectionError, Timeout, ReadTimeout

from calendar_attach_processor.tvm.session import TvmSession
from tests.fixtures.services import config, tvm, http_pretty, TVM_TICKETS, ticket_headers

logging.basicConfig(level=logging.DEBUG)

TEST_URL = "http://test.url"


def test_session_with_tvm(config, http_pretty, tvm, ticket_headers):
    tvm_session = TvmSession(101, 102, 'some-secret')
    httpretty.register_uri(httpretty.GET, config['ml']['url'], status=200)
    tvm_session.request('get', config['ml']['url'])

    assert_that(httpretty.last_request().headers, has_entries(TVM_TICKETS))


@responses.activate
def test_conn_timeout(config, tvm, ticket_headers):
    responses.add(responses.GET, TEST_URL, body=ConnectTimeout(request=requests.Request(url=TEST_URL)))
    with pytest.raises(HTTPError):
        tvm_session = TvmSession(101, 102, 'some-secret')
        tvm_session.request('get', TEST_URL)

@responses.activate
def test_read_timeout(config, tvm, ticket_headers):
    responses.add(responses.POST, TEST_URL, body=ReadTimeout(request=requests.Request(url=TEST_URL)))
    with pytest.raises(HTTPError):
        tvm_session = TvmSession(101, 102, 'some-secret')
        tvm_session.request('post', TEST_URL)


@responses.activate
def test_request_timeout(config, tvm, ticket_headers):
    responses.add(responses.POST, TEST_URL, body=Timeout(request=requests.Request(url=TEST_URL)))
    with pytest.raises(HTTPError):
        tvm_session = TvmSession(101, 102, 'some-secret')
        tvm_session.request('post', TEST_URL)


@responses.activate
def test_conn_error(config, tvm, ticket_headers):
    responses.add(responses.GET, TEST_URL, body=ConnectionError(request=requests.Request(url=TEST_URL)))
    with pytest.raises(HTTPError):
        tvm_session = TvmSession(101, 102, 'some-secret')
        tvm_session.request('get', TEST_URL)


@responses.activate
def test_http_error(config, tvm, ticket_headers):
    responses.add(responses.GET, TEST_URL, status=500, body="internal_error")
    with pytest.raises(HTTPError):
        tvm_session = TvmSession(101, 102, 'some-secret')
        logging.info(tvm_session.request('get', TEST_URL))


@responses.activate
def test_retry_error(config, tvm, ticket_headers):
    responses.add(responses.GET, TEST_URL, body=RetryError(request=requests.Request(url=TEST_URL)))
    with pytest.raises(HTTPError):
        tvm_session = TvmSession(101, 102, 'some-secret')
        tvm_session.request('get', TEST_URL)
