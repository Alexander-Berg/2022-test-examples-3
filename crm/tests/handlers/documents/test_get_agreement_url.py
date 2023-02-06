from crm.agency_cabinet.common.testing import BaseTestClient
from crm.agency_cabinet.common.service_discovery import ServiceDiscovery
from crm.agency_cabinet.grants.common import structs as grants_structs


URL = '/api/agencies/1/documents/agreements/1/download_url'


async def test_get_agreement_url(
    client: BaseTestClient,
    service_discovery: ServiceDiscovery,
    yandex_uid: int,
):
    return_value = 'http://example.com'
    service_discovery.grants.check_access_level.return_value = grants_structs.AccessLevel.ALLOW
    service_discovery.documents.get_agreement_url.return_value = return_value

    got = await client.get(URL, expected_status=200)

    assert got == {'url': return_value}
