import pytest

from crm.agency_cabinet.common.testing import BaseTestClient
from crm.agency_cabinet.common.service_discovery import ServiceDiscovery
from crm.agency_cabinet.grants.common import structs as grants_structs


URL = '/api/agencies/{agency_id}/documents/{document_id}/download_url'


@pytest.mark.parametrize(('grants_return_value',
                          'documents_return_value'
                          ),
                         [(
                             grants_structs.AccessLevel.ALLOW,
                             'http://example.com'
                         ),
])
async def test_get_download_url(
    client: BaseTestClient,
    service_discovery: ServiceDiscovery,
    yandex_uid: int,
    grants_return_value,
    documents_return_value
):
    service_discovery.grants.check_access_level.return_value = grants_return_value
    service_discovery.rewards.get_document_url.return_value = documents_return_value

    got = await client.get(URL.format(agency_id=1, document_id=1), expected_status=200)

    assert got == {'url': documents_return_value}
