from crm.agency_cabinet.common.testing import BaseTestClient
from crm.agency_cabinet.common.service_discovery import ServiceDiscovery
from crm.agency_cabinet.grants.common import structs as grants_structs
from crm.agency_cabinet.ord.common import structs as ord_structs

from decimal import Decimal

URL = '/api/agencies/{agency_id}/ord/reports/{report_id}/acts'


async def test_get_acts(client: BaseTestClient, service_discovery: ServiceDiscovery, yandex_uid: int):
    service_discovery.grants.check_access_level.return_value = grants_structs.AccessLevel.ALLOW
    service_discovery.ord.get_acts.return_value = ord_structs.ActList(
        size=3,
        acts=[
            ord_structs.Act(
                act_id=1,
                act_eid='act5',
                amount=Decimal('5'),
                is_vat=True,
            ),
            ord_structs.Act(
                act_id=2,
                act_eid='act4',
                amount=Decimal('3'),
                is_vat=False,
            ),
            ord_structs.Act(
                act_id=3,
                act_eid='act3',
                amount=None,
                is_vat=None,
            ),
        ]
    )
    got = await client.get(URL.format(agency_id=1, report_id=1), expected_status=200)
    assert got == {
        'size': 3,
        'items':
            [
                {
                    'act_id': 1,
                    'act_eid': 'act5',
                    'amount': '5',
                    'is_vat': True,
                },
                {
                    'act_id': 2,
                    'act_eid': 'act4',
                    'amount': '3',
                    'is_vat': False,
                },
                {
                    'act_id': 3,
                    'act_eid': 'act3',
                    'amount': None,
                    'is_vat': None,
                },
            ]}


async def test_get_acts_access_denied(client: BaseTestClient, service_discovery: ServiceDiscovery, yandex_uid: int):
    service_discovery.grants.check_access_level.return_value = grants_structs.AccessLevel.DENY
    await client.get(URL.format(agency_id=1, report_id=1), expected_status=403)
