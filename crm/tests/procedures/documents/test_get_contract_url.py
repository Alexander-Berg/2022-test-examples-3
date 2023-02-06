import pytest

from crm.agency_cabinet.common.server.common.structs.common import UrlResponse
from crm.agency_cabinet.gateway.server.src.procedures.documents.contracts import GetContractUrl
from crm.agency_cabinet.grants.common.structs import AccessLevel


@pytest.fixture
def procedure(service_discovery):
    return GetContractUrl(service_discovery)


@pytest.fixture
def input_params():
    return dict(agency_id=22, contract_id=1)


async def test_returns_contract_url(
    procedure, input_params, service_discovery
):
    url = UrlResponse(url='url.ru')

    service_discovery.grants.check_access_level.return_value = AccessLevel.ALLOW
    service_discovery.documents.get_contract_url.return_value = url

    got = await procedure(yandex_uid=123, **input_params)

    assert got == url
