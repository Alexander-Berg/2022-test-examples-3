from crm.agency_cabinet.common.testing import BaseTestClient
from crm.agency_cabinet.common.service_discovery import ServiceDiscovery
from crm.agency_cabinet.grants.common import structs as grants_structs

URL = '/api/agencies/1/ord/reports/1/delete'


async def test_delete_rows_info(client: BaseTestClient, service_discovery: ServiceDiscovery, yandex_uid: int):
    service_discovery.grants.check_access_level.return_value = grants_structs.AccessLevel.ALLOW
    service_discovery.ord.delete_report.return_value = None
    got = await client.post(URL, expected_status=200, json={})
    assert got == {'status': 'ok'}
