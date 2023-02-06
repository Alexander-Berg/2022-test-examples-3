from decimal import Decimal
from crm.agency_cabinet.common.testing import BaseTestClient
from crm.agency_cabinet.common.service_discovery import ServiceDiscovery
from crm.agency_cabinet.grants.common import structs as grants_structs
from crm.agency_cabinet.ord.common import structs as ord_structs

URL = '/api/agencies/{agency_id}/ord/reports/{report_id}/acts'


async def test_add_act(client: BaseTestClient, service_discovery: ServiceDiscovery, yandex_uid: int):
    service_discovery.grants.check_access_level.return_value = grants_structs.AccessLevel.ALLOW
    service_discovery.ord.add_act.return_value = ord_structs.Act(
        act_id=1234,
        act_eid='eid',
        amount=Decimal('10.0'),
        is_vat=False,
    )
    got = await client.post(URL.format(agency_id=1, report_id=1), json={'act_eid': 'eid', 'amount': '10.0', 'is_vat': False})
    assert got == {
        'act_id': 1234,
        'act_eid': 'eid',
        'amount': '10.0',
        'is_vat': False,
    }
