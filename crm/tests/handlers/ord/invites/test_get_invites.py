from crm.agency_cabinet.common.testing import BaseTestClient
from crm.agency_cabinet.common.service_discovery import ServiceDiscovery
from crm.agency_cabinet.grants.common import structs as grants_structs
from crm.agency_cabinet.ord.common import structs as ord_structs

from datetime import datetime

URL = '/api/agencies/{agency_id}/ord/invites'


async def test_get_invites(client: BaseTestClient, service_discovery: ServiceDiscovery, yandex_uid: int):

    service_discovery.grants.check_access_level.return_value = grants_structs.AccessLevel.ALLOW
    service_discovery.ord.get_invites.return_value = ord_structs.InviteList(
        size=3,
        invites=[
            ord_structs.Invite(
                invite_id=1,
                email='user1@yandex.ru',
                status='sent',
                created_at=datetime(2022, 7, 20, 5, 16)
            ),
            ord_structs.Invite(
                invite_id=3,
                email='user2@yandex.ru',
                status='sent',
                created_at=datetime(2022, 7, 19, 8, 31)
            ),
            ord_structs.Invite(
                invite_id=4,
                email='user3@yandex.ru',
                status='sent',
                created_at=datetime(2022, 7, 18, 9, 55)
            ),
        ]
    )
    got = await client.get(URL.format(agency_id=1), expected_status=200)
    assert got == {
        'size': 3,
        'items':
            [
                {
                    'invite_id': 1,
                    'email': 'user1@yandex.ru',
                    'status': 'sent',
                    'created_at': '2022-07-20'
                },
                {
                    'invite_id': 3,
                    'email': 'user2@yandex.ru',
                    'status': 'sent',
                    'created_at': '2022-07-19'
                },
                {
                    'invite_id': 4,
                    'email': 'user3@yandex.ru',
                    'status': 'sent',
                    'created_at': '2022-07-18'
                },
            ]}


async def test_get_invites_access_denied(client: BaseTestClient, service_discovery: ServiceDiscovery, yandex_uid: int):
    service_discovery.grants.check_access_level.return_value = grants_structs.AccessLevel.DENY
    await client.get(URL.format(agency_id=1), expected_status=403)
