from unittest.mock import AsyncMock

import pytest

from crm.agency_cabinet.documents.server.src.handler import Handler


@pytest.fixture
def yadoc_client():
    return AsyncMock()


@pytest.fixture
def tvm_config():
    return AsyncMock()


@pytest.fixture
def handler():
    return Handler(
        'endpoint_url',
        1,
        tvm_config,
        yadoc_client
    )
