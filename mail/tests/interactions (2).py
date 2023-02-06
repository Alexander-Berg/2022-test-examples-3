import re
from unittest import mock

import pytest
from aiohttp import hdrs

from aioresponses import CallbackResult
from aioresponses import aioresponses as base_aioresponses
from mail.ciao.ciao.interactions.base import create_connector


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
            print('Failed to match:', method, url)
        else:
            for matcher, mock_ in self._mocks.items():
                if matcher.match(method, url):
                    mock_(**kwargs)
                    break
        return response


@pytest.fixture(autouse=True)
def aioresponses_mocker():
    with aioresponses(passthrough=['http://127.0.0.1:']) as m:
        yield m


@pytest.fixture
def tvm_default_uid(user):
    return user.uid


@pytest.fixture(autouse=True)
def setup_tvm(settings, rands, aioresponses_mocker, tvm_default_uid):
    # Since we allow passthrough for localhost, setting up custom host for tvm
    from mail.ciao.ciao.utils.tvm import TVM_CONFIG
    settings.TVM_HOST = TVM_CONFIG['host'] = 'tvm'

    def tvm_callback(url, **kwargs):
        dst = kwargs['params']['dsts']
        return CallbackResult(
            status=200,
            payload={
                settings.TVM_CLIENT: {
                    'tvm_id': dst,
                    'ticket': f'service-ticket-f{rands()}',
                },
            },
        )

    aioresponses_mocker.get(
        re.compile(f'^http://{settings.TVM_HOST}:{settings.TVM_PORT}/tvm/tickets.*$'),
        callback=tvm_callback,
        repeat=True,
    )
    aioresponses_mocker.get(
        re.compile(f'^http://{settings.TVM_HOST}:{settings.TVM_PORT}/tvm/checksrv.*$'),
        payload={'src': settings.TVM_ALLOWED_CLIENTS[0]},
        repeat=True,
    )
    aioresponses_mocker.get(
        re.compile(f'^http://{settings.TVM_HOST}:{settings.TVM_PORT}/tvm/checkusr.*$'),
        payload={'default_uid': tvm_default_uid},
        repeat=True,
    )


@pytest.fixture
def create_client(test_logger, request_id):
    def _inner(client_cls):
        client_cls.CONNECTOR = create_connector()
        return client_cls(test_logger, request_id)

    return _inner
