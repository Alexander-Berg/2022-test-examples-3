import pytest

from crm.agency_cabinet.common.testing import BaseTestClient
from crm.agency_cabinet.common.service_discovery import ServiceDiscovery
from crm.agency_cabinet.grants.common import structs as grants_structs
from crm.agency_cabinet.ord.common import structs as ord_structs
from crm.agency_cabinet.ord.client.exceptions import UnsuitableClientException, UnsuitableReportException, \
    UnsuitableAgencyException

URL = '/api/agencies/1/ord/reports/1/clients/1'


async def test_get_client_short_info(client: BaseTestClient, service_discovery: ServiceDiscovery, yandex_uid: int):
    service_discovery.grants.check_access_level.return_value = grants_structs.AccessLevel.ALLOW
    service_discovery.ord.get_client_short_info.return_value = ord_structs.ClientShortInfo(
        id=1,
        client_id='123456',
        login='login1',
        name='name1',
    )

    got = await client.get(URL)

    assert got == {
        'id': 1,
        'client_id': '123456',
        'login': 'login1',
        'name': 'name1',
    }


@pytest.mark.parametrize(
    (
        'raised_exception',
        'expected_status',
    ),
    [
        (
            UnsuitableClientException(),
            404,
        ),
        (
            UnsuitableReportException(),
            404,
        ),
        (
            UnsuitableAgencyException(),
            404,
        ),
    ]
)
async def test_get_client_short_info_errors(
    client: BaseTestClient,
    service_discovery: ServiceDiscovery,
    yandex_uid: int,
    raised_exception: Exception,
    expected_status: int,
):
    service_discovery.grants.check_access_level.return_value = grants_structs.AccessLevel.ALLOW
    service_discovery.ord.get_client_short_info.return_value = None
    service_discovery.ord.get_client_short_info.side_effect = raised_exception

    await client.get(URL, expected_status=expected_status)
