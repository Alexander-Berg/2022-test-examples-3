import logging

import pytest
import requests
import responses
from hamcrest import assert_that, equal_to
from requests.exceptions import ConnectionError, ReadTimeout, ConnectTimeout, Timeout, HTTPError, RetryError

from calendar_attach_processor.service.utils import http_request

logging.basicConfig(level=logging.DEBUG)

TEST_URL = "http://localhost:10010/gate/get/1000013.mail:4001202093.E4192:556108792168591100747183832416"


@responses.activate
def test_conn_timeout():
    responses.add(responses.GET, TEST_URL, body=ConnectTimeout(request=requests.Request(url=TEST_URL)))
    with pytest.raises(HTTPError):
        http_request(TEST_URL)


@responses.activate
def test_read_timeout():
    responses.add(responses.POST, TEST_URL, body=ReadTimeout(request=requests.Request(url=TEST_URL)))
    with pytest.raises(HTTPError):
        http_request(TEST_URL, method="POST")


@responses.activate
def test_request_timeout():
    responses.add(responses.POST, TEST_URL, body=Timeout(request=requests.Request(url=TEST_URL)))
    with pytest.raises(HTTPError):
        http_request(TEST_URL, method="POST", params={'gettype': "xml"})


@responses.activate
def test_conn_error():
    responses.add(responses.GET, TEST_URL, body=ConnectionError(request=requests.Request(url=TEST_URL)))
    with pytest.raises(HTTPError):
        http_request(TEST_URL)


@responses.activate
def test_ok():
    responses.add(responses.POST, TEST_URL, status=200, body="ok")
    response = http_request(TEST_URL, method="POST", data="data")

    assert_that(response.content, equal_to("ok"))


@responses.activate
def test_http_error():
    responses.add(responses.GET, TEST_URL, status=500, body="internal_error")
    with pytest.raises(HTTPError):
        http_request(TEST_URL, timeout=10)


@responses.activate
def test_retry_error():
    responses.add(responses.GET, TEST_URL, body=RetryError(request=requests.Request(url=TEST_URL)))
    with pytest.raises(HTTPError):
        http_request(TEST_URL)