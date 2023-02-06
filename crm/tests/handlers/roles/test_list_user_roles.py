import pytest

from crm.agency_cabinet.common.testing import BaseTestClient
from crm.agency_cabinet.common.service_discovery import ServiceDiscovery
from crm.agency_cabinet.grants.common import structs as grants_structs

URL = '/api/agencies/1/roles'


@pytest.mark.parametrize(
    (
        'grants_return_value', 'list_user_roles_return_value', 'expected_result'
    ),
    [
        (
            grants_structs.AccessLevel.ALLOW,
            [
                grants_structs.RolePermissions(
                    role=grants_structs.UserRole(role_id=2, name='admin'),
                    permissions=[grants_structs.Permission(name='edit.all')]
                )
            ],
            {
                'roles': [
                    {
                        'role_id': 2,
                        'name': 'admin',
                        'display_name': 'Администратор',
                        'permissions': [
                            'edit.all',
                        ]
                    }
                ]
            }
        ),
        (
            grants_structs.AccessLevel.ALLOW,
            [],
            {'roles': []}
        ),
    ],

)
async def test_list_user_roles(
    client: BaseTestClient,
    service_discovery: ServiceDiscovery,
    yandex_uid: int,
    grants_return_value,
    list_user_roles_return_value,
    expected_result,
):
    service_discovery.grants.check_access_level.return_value = grants_return_value
    service_discovery.grants.list_user_roles.return_value = list_user_roles_return_value

    got = await client.get(URL, expected_status=200)

    assert got == expected_result
