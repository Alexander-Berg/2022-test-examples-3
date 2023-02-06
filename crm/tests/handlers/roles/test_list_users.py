import pytest

from crm.agency_cabinet.common.testing import BaseTestClient
from crm.agency_cabinet.common.service_discovery import ServiceDiscovery
from crm.agency_cabinet.grants.common import structs as grants_structs

URL = '/api/agencies/{agency_id}/users'


@pytest.mark.parametrize(
    (
        'grants_return_value',
        'users_roles_return_value'
    ),
    [(
        grants_structs.AccessLevel.ALLOW,
        grants_structs.GetUsersPermissionsResponse(
            users=[
                grants_structs.UserPermissions(
                    user=grants_structs.User(
                        user_id=1,
                        name='Жан Вальжан',
                        email='test1@mail.ru',
                        login='test1',
                        avatar_id='45848/p3XFjBV5caM7W5fzVSH7Vsoq8-1',
                    ),
                    roles_permissions=[
                        grants_structs.RolePermissions(
                            grants_structs.UserRole(
                                name='admin',
                                role_id=2
                            ),
                            [
                                grants_structs.Permission(
                                    name='edit.all'
                                )
                            ]
                        ),
                        grants_structs.RolePermissions(
                            grants_structs.UserRole(
                                name='owner',
                                role_id=1
                            ),
                            [
                                grants_structs.Permission(
                                    name='perm1'
                                ),
                                grants_structs.Permission(
                                    name='perm2'
                                )
                            ]
                        )
                    ]
                ),
                grants_structs.UserPermissions(
                    user=grants_structs.User(
                        user_id=2,
                        name='Гаврош',
                        email='test2@mail.ru',
                        login='test2',
                        avatar_id=None,
                    ),
                    roles_permissions=[
                        grants_structs.RolePermissions(
                            grants_structs.UserRole(
                                name='manager',
                                role_id=3
                            ),
                            [
                                grants_structs.Permission(
                                    name='rewards.all'
                                ),
                                grants_structs.Permission(
                                    name='bonuses.all'
                                ),
                                grants_structs.Permission(
                                    name='calculator'
                                )
                            ]
                        ),
                    ]
                ),
                grants_structs.UserPermissions(
                    user=grants_structs.User(
                        user_id=3,
                        name='Жавер',
                        email='test3@mail.ru',
                        login='test3',
                        current=True
                    ),
                    roles_permissions=[
                        grants_structs.RolePermissions(
                            grants_structs.UserRole(
                                name='admin',
                                role_id=2
                            ),
                            [
                                grants_structs.Permission(
                                    name='edit.all'
                                )
                            ]
                        ),
                    ]
                )
            ]
        )
    )]
)
async def test_list_users(
    client: BaseTestClient,
    service_discovery: ServiceDiscovery,
    yandex_uid: int,
    grants_return_value,
    users_roles_return_value
):
    service_discovery.grants.check_access_level.return_value = grants_return_value
    service_discovery.grants.get_users_permissions.return_value = users_roles_return_value.users

    got = await client.get(URL.format(agency_id=1), expected_status=200)

    assert got == {
        'users': [
            {
                'avatar_id': '45848/p3XFjBV5caM7W5fzVSH7Vsoq8-1',
                'current': False,
                'email': 'test1@mail.ru',
                'login': 'test1',
                'name': 'Жан Вальжан',
                'roles': [
                    {
                        'permissions': ['edit.all'],
                        'name': 'admin',
                        'display_name': 'Администратор',
                        'role_id': 2,
                    },
                    {
                        'permissions': ['perm1', 'perm2'],
                        'name': 'owner',
                        'display_name': 'Главный представитель',
                        'role_id': 1
                    }
                ],
                'user_id': 1
            },
            {
                'current': False,
                'email': 'test2@mail.ru',
                'login': 'test2',
                'name': 'Гаврош',
                'roles': [
                    {
                        'role_id': 3,
                        'permissions': ['rewards.all', 'bonuses.all', 'calculator'],
                        'name': 'manager',
                        'display_name': 'Менеджер',
                    }
                ],
                'user_id': 2
            },
            {
                'current': True,
                'email': 'test3@mail.ru',
                'login': 'test3',
                'name': 'Жавер',
                'roles': [
                    {
                        'role_id': 2,
                        'permissions': ['edit.all'],
                        'name': 'admin',
                        'display_name': 'Администратор',
                    }
                ],
                'user_id': 3
            }
        ]
    }
