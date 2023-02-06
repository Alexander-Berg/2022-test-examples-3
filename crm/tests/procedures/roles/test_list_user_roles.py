import pytest
from crm.agency_cabinet.gateway.server.src.procedures.roles import ListUserRoles
from crm.agency_cabinet.grants.common.structs import AccessLevel
from crm.agency_cabinet.grants.common import structs as grants_structs
from crm.agency_cabinet.grants.client import NotHavePermission
from crm.agency_cabinet.gateway.server.src.exceptions import AccessDenied


@pytest.fixture
def procedure(service_discovery):
    return ListUserRoles(service_discovery)


@pytest.fixture
def input_params():
    return dict(agency_id=22)


async def test_returns_role_list(
    procedure, input_params, service_discovery
):
    roles = [
        grants_structs.RolePermissions(
            role=grants_structs.UserRole(role_id=2, name='admin'),
            permissions=[grants_structs.Permission(name='edit.all')]
        )
    ]

    service_discovery.grants.check_access_level.return_value = AccessLevel.ALLOW
    service_discovery.grants.list_user_roles.return_value = roles

    got = await procedure(yandex_uid=123, **input_params)

    assert got == roles


async def test_returns_no_roles_if_none(
    procedure, input_params, service_discovery
):
    roles = []

    service_discovery.grants.check_access_level.return_value = AccessLevel.ALLOW
    service_discovery.grants.list_user_roles.return_value = roles

    got = await procedure(yandex_uid=123, **input_params)

    assert got == roles


async def test_returns_403_for_suggested_user(
    procedure, input_params, service_discovery
):
    service_discovery.grants.check_access_level.return_value = grants_structs.AccessLevel.ALLOW
    service_discovery.grants.list_user_roles.side_effect = NotHavePermission(message='Suggested user has no permissions')

    with pytest.raises(AccessDenied):
        await procedure(yandex_uid=123, **input_params)
