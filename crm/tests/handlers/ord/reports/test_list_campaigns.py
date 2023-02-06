from crm.agency_cabinet.common.testing import BaseTestClient
from crm.agency_cabinet.common.service_discovery import ServiceDiscovery
from crm.agency_cabinet.grants.common import structs as grants_structs
from crm.agency_cabinet.ord.common import structs as ord_structs

URL = '/api/agencies/{agency_id}/ord/reports/{report_id}/clients/{client_id}/campaigns'


async def test_list_campaigns(client: BaseTestClient, service_discovery: ServiceDiscovery, yandex_uid: int):
    service_discovery.grants.check_access_level.return_value = grants_structs.AccessLevel.ALLOW
    service_discovery.ord.get_campaigns.return_value = ord_structs.CampaignList(
        campaigns=[
            ord_structs.Campaign(
                id=1,
                campaign_eid='123',
                name='name123',
            ),
            ord_structs.Campaign(
                id=2,
                campaign_eid='456',
                name='name456',
            ),
        ],
        size=2
    )
    got = await client.get(URL.format(agency_id=1, report_id=1, client_id=1))
    assert got == {
        'size': 2,
        'items':
            [
                {
                    'id': 1,
                    'campaign_eid': '123',
                    'name': 'name123'
                },
                {
                    'id': 2,
                    'campaign_eid': '456',
                    'name': 'name456'
                },
            ]}
