import pytest

from crm.agency_cabinet.grants.common.consts import PartnerType
from crm.agency_cabinet.grants.server.src.db import models


@pytest.fixture()
async def fixture_load_partners_permissions_not_enough():
    rows = [
        {
            'name': 'roles',
            'description': 'Доступ к управлению ролями',
        },
        {
            'name': 'client_bonuses',
            'description': 'Доступ к странице бонусов',
        }
    ]

    yield await models.Permission.bulk_insert(rows)

    await models.Permission.delete.gino.status()


@pytest.fixture()
async def fixture_load_partners_all_permissions():
    rows = [
        {
            'name': 'certification',
            'description': 'certification',
        },
        {
            'name': 'rewards',
            'description': 'rewards',
        },
        {
            'name': 'documents',
            'description': 'documents',
        },
        {
            'name': 'client_bonuses',
            'description': 'client_bonuses',
        },
        {
            'name': 'analytics',
            'description': 'analytics',
        },
        {
            'name': 'roles',
            'description': 'roles',
        },
        {
            'name': 'ord',
            'description': 'ord',
        },
    ]

    yield await models.Permission.bulk_insert(rows)

    await models.Permission.delete.gino.status()


@pytest.fixture()
async def fixture_load_partners_cleanup():
    yield None

    await models.PartnersRolesMap.delete.gino.status()
    await models.RolesPermissionsMap.delete.gino.status()
    await models.UserRole.delete.gino.status()
    await models.Partner.delete.gino.status()


@pytest.fixture()
async def fixture_load_partners_empty_name_partner():
    rows = [
        {
            'name': '',
            'external_id': '44',
            'type': PartnerType.agency.value,
        },
    ]

    yield await models.Partner.bulk_insert(rows)

    await models.Partner.delete.gino.status()
