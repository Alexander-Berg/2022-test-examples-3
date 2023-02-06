import pytest


from crm.agency_cabinet.common.testing import BaseTestClient
from crm.agency_cabinet.common.service_discovery import ServiceDiscovery
from crm.agency_cabinet.grants.common import structs as grants_structs

URL = '/api/agencies/{agency_id}/ord/reports/{report_id}/send'


@pytest.mark.parametrize(('grants_return_value',
                          'reports_return_value',
                          'expected'),
                         [(grants_structs.AccessLevel.ALLOW,
                           'TODO',
                           {'status': 'ok'}),
                          ])
async def test_send_report(client: BaseTestClient, service_discovery: ServiceDiscovery, yandex_uid: int,
                           grants_return_value, reports_return_value, expected):
    service_discovery.grants.check_access_level.return_value = grants_return_value
    got = await client.post(URL.format(agency_id=1, report_id=1), expected_status=200, json={})

    assert got == expected
