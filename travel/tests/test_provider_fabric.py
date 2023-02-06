import os
from collections import namedtuple

import mock

from travel.library.python.tvm_ticket_provider import (
    FakeTvmTicketProvider,
    ProviderFabric,
    QloudTvmClient,
    TvmTicketProvider,
    TvmToolClient,
)

Settings = namedtuple('Settings', ('ENABLE_TVM',))


def test_fake_provider_by_param():
    provider = ProviderFabric().create(fake=True)

    assert isinstance(provider, FakeTvmTicketProvider)


def test_fake_provider_by_settings():
    provider = ProviderFabric().create(settings=Settings(ENABLE_TVM=False))

    assert isinstance(provider, FakeTvmTicketProvider)


@mock.patch.dict(os.environ, {'QLOUD_TVM_TOKEN': 'qloud_token'})
def test_qloud_client():
    provider = ProviderFabric().create()

    assert isinstance(provider, TvmTicketProvider)
    assert isinstance(provider._client, QloudTvmClient)
    assert provider._client._url == QloudTvmClient.API_URL
    assert provider._client._token == 'qloud_token'


@mock.patch.dict(os.environ, {'TVM_TOOL_TOKEN': 'tvm_tool_token', 'TVM_TOOL_URL': 'http://localhost:8080/tvm'})
def test_tvm_tool_client():
    provider = ProviderFabric().create()

    assert isinstance(provider, TvmTicketProvider)
    assert isinstance(provider._client, TvmToolClient)
    assert provider._client._url == 'http://localhost:8080/tvm'
    assert provider._client._token == 'tvm_tool_token'
