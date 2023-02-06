import pytest


from crm.agency_cabinet.common.testing import BaseTestClient
from crm.agency_cabinet.common.service_discovery import ServiceDiscovery
from crm.agency_cabinet.grants.common import structs as grants_structs
from crm.agency_cabinet.ord.common import structs as ord_structs

URL = '/api/agencies/{agency_id}/ord/reports/{report_id}/lock'


@pytest.mark.parametrize(
    (
        'grants_return_value',
        'reports_return_value',
        'expected'
    ),
    [
        (
            grants_structs.AccessLevel.ALLOW,
            ord_structs.LockStatus(
                lock=True
            ),
            {
                'lock': True
            }
        ),
    ]
)
async def test_get_lock_status(client: BaseTestClient, service_discovery: ServiceDiscovery, yandex_uid: int,
                               grants_return_value, reports_return_value, expected):
    service_discovery.grants.check_access_level.return_value = grants_return_value
    service_discovery.ord.get_lock_status.return_value = reports_return_value
    got = await client.get(URL.format(agency_id=1, report_id=1), json={})
    assert got == expected
