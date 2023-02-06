# -*- encoding: utf-8 -*-
import mock
import pytest
import requests_mock

from feature_flag_client import Client, Storage


@pytest.yield_fixture()
def m():
    with requests_mock.mock() as _m:
        yield _m


@pytest.fixture()
def url():
    return 'http://example.net/feature-flag?service-code=test-service'


@pytest.fixture()
def logger():
    _logger = mock.Mock()
    _logger.exception = mock.Mock()
    return _logger


@pytest.fixture()
def client(logger):
    return Client(
        host='example.net',
        service_code='test-service',
        logger=logger
    )


@pytest.fixture()
def storage(client):
    return Storage(client)
