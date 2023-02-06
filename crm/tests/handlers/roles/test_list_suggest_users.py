import pytest

from crm.agency_cabinet.common.testing import BaseTestClient
from crm.agency_cabinet.common.service_discovery import ServiceDiscovery
from crm.agency_cabinet.grants.common import structs as grants_structs

URL = '/api/agencies/{agency_id}/users/suggested'


@pytest.mark.parametrize(
    (
        'grants_return_value',
        'users_roles_return_value'
    ),
    [
        (
            grants_structs.AccessLevel.ALLOW,
            grants_structs.GetSuggestedUsersResponse(
                users=[
                    grants_structs.User(
                        user_id=1,
                        name='Потенциальный пользователь агентства 1',
                        email='suggested@agency1',
                        login='suggested',
                        avatar_id=None,
                        current=False
                    )
                ]
            )
        ),
    ]
)
async def test_list_suggest_users(
    client: BaseTestClient,
    service_discovery: ServiceDiscovery,
    yandex_uid: int,
    grants_return_value,
    users_roles_return_value
):
    service_discovery.grants.check_access_level.return_value = grants_return_value
    service_discovery.grants.get_suggested_users.return_value = users_roles_return_value.users

    got = await client.get(URL.format(agency_id=1), expected_status=200)

    assert got == {
        'users': [
            {
                'avatar_id': None,
                'email': 'suggested@agency1',
                'login': 'suggested',
                'name': 'Потенциальный пользователь агентства 1',
                'user_id': 1
            }
        ]
    }
