import pytest

from crm.agency_cabinet.grants.common import structs
from crm.agency_cabinet.grants.server.src.procedures.role_manager import UserManager
from crm.agency_cabinet.grants.server.src.procedures.exceptions import NoSuchOAuthTokenException, InactiveOAuthToken


@pytest.fixture
def procedure():
    return UserManager


async def test_check_oauth(procedure, fixture_permissions, fixture_oauth, fixture_oauth_map, fixture_partners):
    result = await procedure.check_oauth_permissions(structs.CheckOAuthPermissionsRequest(
        app_client_id='123123123',
        permissions=[structs.Permission(name=fixture_permissions[0].name)]
    ))
    assert result == structs.CheckPermissionsResponse(
        is_have_permissions=True, partner=structs.Partner.from_model(fixture_partners[0])
    )


async def test_check_oauth_no_such_oauth_token(procedure, fixture_partners, fixture_oauth, fixture_oauth_map, fixture_permissions):
    with pytest.raises(NoSuchOAuthTokenException):
        await procedure.check_oauth_permissions(structs.CheckOAuthPermissionsRequest(
            app_client_id='33333333',
            permissions=[structs.Permission(name=fixture_permissions[0].name)]
        ))


async def test_check_oauth_inactive_oauth_token(procedure, fixture_oauth, fixture_oauth_map, fixture_partners, fixture_permissions):
    with pytest.raises(InactiveOAuthToken):
        await procedure.check_oauth_permissions(structs.CheckOAuthPermissionsRequest(
            app_client_id='222222222',
            permissions=[structs.Permission(name=fixture_permissions[0].name)]
        ))


async def test_missed_permissions(procedure, fixture_permissions, fixture_oauth, fixture_oauth_map, fixture_partners):
    result = await procedure.check_oauth_permissions(structs.CheckOAuthPermissionsRequest(
        app_client_id='123123123',
        permissions=[structs.Permission(name=fixture_permissions[0].name),
                     structs.Permission(name=fixture_permissions[1].name)]
    ))
    assert result == structs.CheckPermissionsResponse(
        is_have_permissions=False,
        missed_permissions=[structs.Permission(name=fixture_permissions[1].name)],
        partner=structs.Partner.from_model(fixture_partners[0])
    )
