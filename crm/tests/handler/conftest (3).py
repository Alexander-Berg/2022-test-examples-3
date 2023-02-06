import pytest
import typing
from crm.agency_cabinet.grants.server.src.db import models
from crm.agency_cabinet.grants.server.src.procedures.role_manager import UserManager


@pytest.fixture(scope='module')
async def fixture_partners():
    rows = [
        {
            'external_id': '1',
            'type': 'agency'
        },
        {
            'external_id': '2',
            'type': 'agency'
        },
        {
            'external_id': '3',
            'type': 'agency'
        }
    ]

    yield await models.Partner.bulk_insert(rows)

    await models.Partner.delete.gino.status()


@pytest.fixture(scope='module')
async def fixture_partners_map(fixture_user_roles: typing.List[models.UserRole], fixture_partners: typing.List[models.Partner]):
    rows = [
        {
            'role_id': fixture_user_roles[0].id,  # Менеджер агентства 1
            'agency_id': 1,
            'partner_id': fixture_partners[0].id
        },
        {
            'role_id': fixture_user_roles[1].id,  # Менеджер агентства 2
            'agency_id': 2,
            'partner_id': fixture_partners[1].id
        },
        {
            'role_id': fixture_user_roles[2].id,  # Менеджер агентства 3
            'agency_id': 3,
            'partner_id': fixture_partners[2].id
        },
        {
            'role_id': fixture_user_roles[3].id,  # Главный представитель агентства 1
            'agency_id': 1,
            'partner_id': fixture_partners[0].id
        },
        {
            'role_id': fixture_user_roles[4].id,  # Пользователь без прав для агентства 1
            'agency_id': 1,
            'partner_id': fixture_partners[0].id
        },
        {
            'role_id': fixture_user_roles[5].id,  # Главный представитель агентства 2
            'agency_id': 2,
            'partner_id': fixture_partners[1].id
        },
        {
            'role_id': fixture_user_roles[6].id,  # Администратор агентства 1
            'agency_id': 1,
            'partner_id': fixture_partners[0].id
        },
    ]
    yield await models.PartnersRolesMap.bulk_insert(rows)

    await models.PartnersRolesMap.delete.gino.status()


@pytest.fixture(scope='module')
async def fixture_role_permissions(fixture_permissions: typing.List[models.Permission], fixture_user_roles: typing.List[models.UserRole]):
    rows = [
        {
            'permission_id': fixture_permissions[1].id,  # Бонусы
            'role_id': fixture_user_roles[0].id,  # Менеджер агентства 1
            'is_editable': True
        },
        {
            'permission_id': fixture_permissions[2].id,  # Сертификаты
            'role_id': fixture_user_roles[0].id,  # Менеджер агентства 1
            'is_editable': True
        },
        {
            'permission_id': fixture_permissions[1].id,  # Бонусы
            'role_id': fixture_user_roles[1].id,  # Менеджер агентства 2
            'is_editable': True
        },
        {
            'permission_id': fixture_permissions[2].id,  # Сертификаты
            'role_id': fixture_user_roles[1].id,  # Менеджер агентства 2
            'is_editable': True
        },
        {
            'permission_id': fixture_permissions[1].id,  # Бонусы
            'role_id': fixture_user_roles[2].id,  # Менеджер агентства 3
            'is_editable': True
        },
        {
            'permission_id': fixture_permissions[2].id,  # Сертификаты
            'role_id': fixture_user_roles[2].id,  # Менеджер агентства 3
            'is_editable': True
        },
        {
            'permission_id': fixture_permissions[0].id,  # Доступ к ролям
            'role_id': fixture_user_roles[3].id,  # Главный представитель агентства 1
            'is_editable': False
        },
        {
            'permission_id': fixture_permissions[0].id,  # Доступ к ролям
            'role_id': fixture_user_roles[5].id,  # Главный представитель агентства 2
            'is_editable': False
        },
        {
            'permission_id': fixture_permissions[0].id,  # Доступ к ролям
            'role_id': fixture_user_roles[6].id,  # Администратор агентства 1
            'is_editable': False
        },

    ]
    yield await models.RolesPermissionsMap.bulk_insert(rows)

    await models.RolesPermissionsMap.delete.gino.status()


@pytest.fixture(scope='module')
async def fixture_permissions():
    rows = [
        {
            'name': 'roles',
            'description': 'Доступ к управлению ролями',
        },
        {
            'name': 'bonuses',
            'description': 'Доступ к странице бонусов',
        },
        {
            'name': 'certificate',
            'description': 'Доступ к странице сертификации',
        }
    ]

    yield await models.Permission.bulk_insert(rows)

    await models.Permission.delete.gino.status()


@pytest.fixture(scope='module')
async def fixture_user_roles():
    rows = [
        {
            'name': 'manager',
            'description': 'Менеджер агентства 1'
        },
        {
            'name': 'manager',
            'description': 'Менеджер агентства 2'
        },
        {
            'name': 'manager',
            'description': 'Менеджер агентства 3'
        },
        {
            'name': 'owner',
            'description': 'Главный представитель агентства 1'
        },
        {
            'name': 'suggested_user',
            'description': 'Пользователь без прав для агентства 1'
        },
        {
            'name': 'owner',
            'description': 'Главный представитель агентства 2'
        },
        {
            'name': 'admin',
            'description': 'Администратор агентства 1'
        },
    ]
    yield await models.UserRole.bulk_insert(rows)

    await models.UserRole.delete.gino.status()


@pytest.fixture(scope='module')
async def fixture_users():
    rows = [
        {
            'yandex_uid': 1,
            'email': 'manager@agency12',
            'display_name': 'Менеджер с ролями агентств 1 и 2',
            'type': 'external',
            'login': 'manager1',
        },
        {
            'yandex_uid': 2,
            'email': 'manager@yandex',
            'display_name': 'Менеджер с ролями агентств 1, 2, 3',
            'type': 'internal',
            'login': 'manager2',
        },
        {
            'yandex_uid': 3,
            'email': 'main@agency1',
            'display_name': 'Главный представитель агентства 1',
            'type': 'external',
            'login': 'main1',
        },
        {
            'yandex_uid': 4,
            'email': 'suggested@agency1',
            'display_name': 'Потенциальный пользователь агентства 1',
            'type': 'external',
            'login': 'suggested',
        },
        {
            'yandex_uid': 5,
            'email': 'main@agency2',
            'display_name': 'Главный представитель агентства 2',
            'type': 'external',
            'login': 'main2',
        },
    ]

    yield await models.User.bulk_insert(rows)

    await models.User.delete.gino.status()
    UserManager.get_user_by_uid.cache_clear()


@pytest.fixture(scope='module')
async def fixture_user_permissions(fixture_users: typing.List[models.User],
                                   fixture_role_permissions: typing.List[models.RolesPermissionsMap],
                                   fixture_user_roles: typing.List[models.UserRole]):
    rows = [
        # Пользователь 1 с правами "Бонусы" и "Сертификаты" в агентстве 1 и правами "Бонусы" в агентстве 2
        {
            'user_id': fixture_users[0].id,
            'role_id': fixture_role_permissions[0].role_id,
            'permission_id': fixture_role_permissions[0].permission_id,
            'is_editable': fixture_role_permissions[0].is_editable,
            'is_active': True
        },
        {
            'user_id': fixture_users[0].id,
            'role_id': fixture_role_permissions[1].role_id,
            'permission_id': fixture_role_permissions[1].permission_id,
            'is_editable': fixture_role_permissions[1].is_editable,
            'is_active': True
        },
        {
            'user_id': fixture_users[0].id,
            'role_id': fixture_role_permissions[2].role_id,
            'permission_id': fixture_role_permissions[2].permission_id,
            'is_editable': fixture_role_permissions[2].is_editable,
            'is_active': True
        },
        {
            'user_id': fixture_users[0].id,
            'role_id': fixture_role_permissions[3].role_id,
            'permission_id': fixture_role_permissions[3].permission_id,
            'is_editable': fixture_role_permissions[3].is_editable,
            'is_active': False
        },
        # Внутренний пользователь с правами на "Бонусы и Сертификаты" для агентств 1, 2, 3
        {
            'user_id': fixture_users[1].id,
            'role_id': fixture_role_permissions[0].role_id,
            'permission_id': fixture_role_permissions[0].permission_id,
            'is_editable': fixture_role_permissions[0].is_editable,
            'is_active': True
        },
        {
            'user_id': fixture_users[1].id,
            'role_id': fixture_role_permissions[1].role_id,
            'permission_id': fixture_role_permissions[1].permission_id,
            'is_editable': fixture_role_permissions[1].is_editable,
            'is_active': True
        },
        {
            'user_id': fixture_users[1].id,
            'role_id': fixture_role_permissions[2].role_id,
            'permission_id': fixture_role_permissions[2].permission_id,
            'is_editable': fixture_role_permissions[2].is_editable,
            'is_active': True
        },
        {
            'user_id': fixture_users[1].id,
            'role_id': fixture_role_permissions[3].role_id,
            'permission_id': fixture_role_permissions[3].permission_id,
            'is_editable': fixture_role_permissions[3].is_editable,
            'is_active': True
        },
        {
            'user_id': fixture_users[1].id,
            'role_id': fixture_role_permissions[4].role_id,
            'permission_id': fixture_role_permissions[4].permission_id,
            'is_editable': fixture_role_permissions[4].is_editable,
            'is_active': True
        },
        {
            'user_id': fixture_users[1].id,
            'role_id': fixture_role_permissions[5].role_id,
            'permission_id': fixture_role_permissions[5].permission_id,
            'is_editable': fixture_role_permissions[5].is_editable,
            'is_active': True
        },
        # Главный представитель агентства 1
        {
            'user_id': fixture_users[2].id,
            'role_id': fixture_role_permissions[6].role_id,
            'permission_id': fixture_role_permissions[6].permission_id,
            'is_editable': fixture_role_permissions[6].is_editable,
            'is_active': True
        },
        # Потенциальный пользователь агентства 1
        {
            'user_id': fixture_users[3].id,
            'role_id': fixture_user_roles[4].id,
            'permission_id': None,
            'is_editable': False,
            'is_active': True
        },
        # Главный представитель агентства 2
        {
            'user_id': fixture_users[4].id,
            'role_id': fixture_role_permissions[7].role_id,
            'permission_id': fixture_role_permissions[7].permission_id,
            'is_editable': fixture_role_permissions[7].is_editable,
            'is_active': True
        },
    ]

    yield await models.UsersRolesPermissionsMap.bulk_insert(rows)

    await models.UsersRolesPermissionsMap.delete.gino.status()
