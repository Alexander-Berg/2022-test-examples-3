import pytest

from crm.agency_cabinet.common.testing import BaseTestClient
from crm.agency_cabinet.common.service_discovery import ServiceDiscovery
from crm.agency_cabinet.grants.common import structs as grants_structs

URL = '/api/agencies/{agency_id}/users/roles'


@pytest.mark.parametrize(
    (
        'grants_return_value', 'list_roles_return_value'
    ),
    [
        (
            grants_structs.AccessLevel.ALLOW,
            [
                grants_structs.RolePermissions(
                    role=grants_structs.UserRole(name='owner', role_id=1),
                    permissions=[
                        grants_structs.Permission(name='perm1', is_editable=True),
                        grants_structs.Permission(name='perm2', is_editable=True),
                    ]
                ),
                grants_structs.RolePermissions(
                    role=grants_structs.UserRole(name='admin', role_id=2),
                    permissions=[
                        grants_structs.Permission(name='edit.all', is_editable=False),
                    ]
                ),
                grants_structs.RolePermissions(
                    role=grants_structs.UserRole(name='admin', role_id=3),
                    permissions=[
                        grants_structs.Permission(name='rewards.all', is_editable=True),
                        grants_structs.Permission(name='bonuses.all', is_editable=True),
                        grants_structs.Permission(name='calculator', is_editable=True),
                    ]
                ),
            ]
        ),
    ]
)
async def test_list_roles(
    client: BaseTestClient,
    service_discovery: ServiceDiscovery,
    yandex_uid: int,
    grants_return_value,
    list_roles_return_value
):
    service_discovery.grants.check_access_level.return_value = grants_return_value
    service_discovery.grants.list_roles.return_value = list_roles_return_value

    got = await client.get(URL.format(agency_id=1), expected_status=200)

    assert got == {
        'roles': [
            {
                'name': 'owner',
                'display_name': 'Главный представитель',
                'permissions': [
                    {
                        'name': 'perm1',
                        'editable': True
                    },
                    {
                        'name': 'perm2',
                        'editable': True
                    }
                ],
                'role_id': 1
            },
            {
                'name': 'admin',
                'display_name': 'Администратор',
                'permissions': [
                    {
                        'name': 'edit.all',
                        'editable': False
                    }
                ],
                'role_id': 2
            },
            {
                'name': 'admin',
                'display_name': 'Администратор',
                'permissions': [
                    {
                        'name': 'rewards.all',
                        'editable': True
                    },
                    {
                        'name': 'bonuses.all',
                        'editable': True
                    },
                    {
                        'name': 'calculator',
                        'editable': True
                    }
                ],
                'role_id': 3
            }
        ]
    }
