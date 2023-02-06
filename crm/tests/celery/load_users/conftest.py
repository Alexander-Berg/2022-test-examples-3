import pytest

from crm.agency_cabinet.grants.server.src.db import models
from crm.agency_cabinet.grants.common.consts import PartnerType


@pytest.fixture(scope='module')
async def fixture_lu_permissions():
    rows = [
        {
            'name': 'roles',
            'description': 'Доступ к управлению ролями',
        },
        {
            'name': 'bonuses',
            'description': 'Доступ к странице бонусов',
        }
    ]

    yield await models.Permission.bulk_insert(rows)

    await models.Permission.delete.gino.status()


@pytest.fixture(scope='module')
async def fixture_lu_partners():
    rows = [
        {
            'external_id': '1',
            'type': PartnerType.agency.value
        },
        {
            'external_id': '11',
            'type': PartnerType.agency.value
        },
        {
            'external_id': '22',
            'type': PartnerType.agency_wo_contract.value
        },
    ]
    yield await models.Partner.bulk_insert(rows)

    await models.Partner.delete.gino.status()


@pytest.fixture(scope='module')
async def fixture_lu_user_roles():
    rows = [
        {
            'name': 'owner',
            'description': 'Главный представитель агентства 1'
        },
        {
            'name': 'manager',
            'description': 'Менеджер агентства 1'
        },
        {
            'name': 'suggested_user',
            'description': 'Пользователь без прав для агентства 1'
        },
        {
            'name': 'owner',
            'description': 'Главный представитель агентства 11'
        },
        {
            'name': 'manager',
            'description': 'Менеджер агентства 11'
        },
        {
            'name': 'suggested_user',
            'description': 'Пользователь без прав для агентства 11'
        },
        {
            'name': 'owner',
            'description': 'Главный представитель агентства 22'
        },
        {
            'name': 'suggested_user',
            'description': 'Пользователь без прав для агентства 22'
        },
    ]
    yield await models.UserRole.bulk_insert(rows)

    await models.UserRole.delete.gino.status()


@pytest.fixture(scope='module')
async def fixture_lu_role_permissions(fixture_lu_permissions: list[models.Permission], fixture_lu_user_roles: list[models.UserRole]):
    rows = [
        {
            'role_id': fixture_lu_user_roles[0].id,  # owner 1
            'permission_id': fixture_lu_permissions[0].id,  # roles
            'is_editable': False
        },
        {
            'role_id': fixture_lu_user_roles[0].id,  # owner 1
            'permission_id': fixture_lu_permissions[1].id,  # bonuses
            'is_editable': False
        },
        {
            'role_id': fixture_lu_user_roles[1].id,  # manager 1
            'permission_id': fixture_lu_permissions[1].id,  # bonuses
            'is_editable': True
        },

        {
            'role_id': fixture_lu_user_roles[3].id,  # owner 11
            'permission_id': fixture_lu_permissions[0].id,  # roles
            'is_editable': False
        },
        {
            'role_id': fixture_lu_user_roles[3].id,  # owner 11
            'permission_id': fixture_lu_permissions[1].id,  # bonuses
            'is_editable': False
        },
        {
            'role_id': fixture_lu_user_roles[4].id,  # manager 11
            'permission_id': fixture_lu_permissions[1].id,  # bonuses
            'is_editable': True
        },

        {
            'role_id': fixture_lu_user_roles[6].id,  # owner 22
            'permission_id': fixture_lu_permissions[0].id,  # roles
            'is_editable': False
        },
        {
            'role_id': fixture_lu_user_roles[6].id,  # owner 22
            'permission_id': fixture_lu_permissions[1].id,  # bonuses
            'is_editable': False
        },
    ]
    yield await models.RolesPermissionsMap.bulk_insert(rows)

    await models.RolesPermissionsMap.delete.gino.status()


@pytest.fixture(scope='module')
async def fixture_lu_partners_roles_map(fixture_lu_user_roles: list[models.UserRole], fixture_lu_partners: list[models.Partner]):
    rows = [
        {
            'role_id': fixture_lu_user_roles[0].id,  # owner 1
            'agency_id': 1,
            'partner_id': fixture_lu_partners[0].id,
        },
        {
            'role_id': fixture_lu_user_roles[1].id,  # manager 1
            'agency_id': 1,
            'partner_id': fixture_lu_partners[0].id,
        },
        {
            'role_id': fixture_lu_user_roles[2].id,  # suggested_user 1
            'agency_id': 1,
            'partner_id': fixture_lu_partners[0].id,
        },
        {
            'role_id': fixture_lu_user_roles[3].id,  # owner 11
            'agency_id': 11,
            'partner_id': fixture_lu_partners[1].id,
        },
        {
            'role_id': fixture_lu_user_roles[4].id,  # manager 11
            'agency_id': 11,
            'partner_id': fixture_lu_partners[1].id,
        },
        {
            'role_id': fixture_lu_user_roles[5].id,  # suggested_user 11
            'agency_id': 11,
            'partner_id': fixture_lu_partners[1].id,
        },
        {
            'role_id': fixture_lu_user_roles[6].id,  # owner 22
            'agency_id': None,
            'partner_id': fixture_lu_partners[2].id,
        },
        {
            'role_id': fixture_lu_user_roles[7].id,  # suggested_user 22
            'agency_id': None,
            'partner_id': fixture_lu_partners[2].id,
        },
    ]
    yield await models.PartnersRolesMap.bulk_insert(rows)

    await models.PartnersRolesMap.delete.gino.status()


@pytest.fixture()
async def fixture_lu_users():
    rows = [
        {
            'yandex_uid': 1,
            'email': 'owner@agency1',
            'display_name': 'Owner agency 1',
            'type': 'external'
        },
        {
            'yandex_uid': 2,
            'email': 'manager@agency1',
            'display_name': 'Manager agency 1',
            'type': 'internal'
        }
    ]

    yield await models.User.bulk_insert(rows)

    await models.User.delete.gino.status()


@pytest.fixture()
async def fixture_lu_user_permissions(fixture_lu_users: list[models.User],
                                      fixture_lu_role_permissions: list[models.RolesPermissionsMap],
                                      fixture_lu_user_roles: list[models.UserRole]):
    rows = [
        # Owner agency 1
        {
            'user_id': fixture_lu_users[0].id,
            'role_id': fixture_lu_role_permissions[0].role_id,
            'permission_id': fixture_lu_role_permissions[0].permission_id,
            'is_editable': fixture_lu_role_permissions[0].is_editable,
            'is_active': True
        },
        {
            'user_id': fixture_lu_users[0].id,
            'role_id': fixture_lu_role_permissions[1].role_id,
            'permission_id': fixture_lu_role_permissions[1].permission_id,
            'is_editable': fixture_lu_role_permissions[1].is_editable,
            'is_active': True
        },
        # Manager agency 1
        {
            'user_id': fixture_lu_users[1].id,
            'role_id': fixture_lu_role_permissions[2].role_id,
            'permission_id': fixture_lu_role_permissions[2].permission_id,
            'is_editable': fixture_lu_role_permissions[2].is_editable,
            'is_active': True
        },
    ]

    yield await models.UsersRolesPermissionsMap.bulk_insert(rows)

    await models.UsersRolesPermissionsMap.delete.gino.status()
