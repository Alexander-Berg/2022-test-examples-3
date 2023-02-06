import os
from unittest import mock

import aiohttp.pytest_plugin
import pytest
from aiohttp import hdrs
from aioresponses import aioresponses as base_aioresponses

from sendr_pytest import *  # noqa

YA_TEST_RUNNER = os.environ.get('YA_TEST_RUNNER') == '1'

pytestmark = pytest.mark.asyncio

if YA_TEST_RUNNER:
    pytest_plugins = ['aiohttp.pytest_plugin']
    del aiohttp.pytest_plugin.loop

    @pytest.fixture
    def loop(event_loop):
        return event_loop


class aioresponses(base_aioresponses):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self._mocks = {}

    def add(self, url, method, *args, **kwargs):
        super().add(url, method, *args, **kwargs)
        matcher = list(self._matches.values())[-1]
        mock_ = self._mocks[matcher] = mock.Mock()
        return mock_

    def head(self, *args, **kwargs):
        return self.add(*args, method=hdrs.METH_HEAD, **kwargs)

    def get(self, *args, **kwargs):
        return self.add(*args, method=hdrs.METH_GET, **kwargs)

    def post(self, *args, **kwargs):
        return self.add(*args, method=hdrs.METH_POST, **kwargs)

    def put(self, *args, **kwargs):
        return self.add(*args, method=hdrs.METH_PUT, **kwargs)

    def patch(self, *args, **kwargs):
        return self.add(*args, method=hdrs.METH_PATCH, **kwargs)

    def delete(self, *args, **kwargs):
        return self.add(*args, method=hdrs.METH_DELETE, **kwargs)

    def options(self, *args, **kwargs):
        return self.add(*args, method=hdrs.METH_OPTIONS, **kwargs)

    async def match(self, method, url, **kwargs):
        response = await super().match(method, url, **kwargs)
        if response is None:
            print('Failed to match:', method, url)  # noqa: T001
        else:
            for matcher, mock_ in self._mocks.items():
                if matcher.match(method, url):
                    mock_(**kwargs)
                    break
        return response


@pytest.fixture
def aioresponses_mocker():
    with aioresponses() as mock:
        yield mock


@pytest.fixture
def pushers_mock(mocker):
    mock = mocker.Mock()
    mock.response_log.push = mocker.AsyncMock()
    return mock


@pytest.fixture
def request_id():
    return 'unittest-request-id'


@pytest.fixture
def dummy_logger():
    import logging

    from sendr_qlog import LoggerContext
    return LoggerContext(logging.getLogger('dummy_logger'), {})


@pytest.fixture
def create_interaction_client(dummy_logger, request_id, pushers_mock):
    TVM_NOT_SET = object()

    def _inner(client_cls, tvm_id=TVM_NOT_SET):
        params = {
            'logger': dummy_logger,
            'request_id': request_id,
            'pushers': pushers_mock,
        }
        if tvm_id is not TVM_NOT_SET:
            params['tvm_id'] = tvm_id
        return client_cls(**params)

    return _inner
