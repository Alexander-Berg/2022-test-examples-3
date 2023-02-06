import pytest
from unittest.mock import AsyncMock

from crm.agency_cabinet.grants.proto import request_pb2, grants_pb2, common_pb2
from crm.agency_cabinet.grants.common.structs import CheckPermissionsResponse
from crm.agency_cabinet.grants.server.src.procedures.exceptions import NoSuchPermissionException, \
    NoSuchOAuthTokenException, InactiveOAuthToken


async def test_calls_method(handler, mocker):
    mock = AsyncMock()
    mock.return_value = CheckPermissionsResponse(
        is_have_permissions=True,
    )

    mocker.patch(
        "crm.agency_cabinet.grants.server.src.procedures.role_manager.UserManager.check_oauth_permissions",
        side_effect=mock,
    )

    request_pb = request_pb2.RpcRequest(
        check_oauth_permissions=grants_pb2.CheckOAuthPermissionsRequest(
            app_client_id='aaaaaaaa',
            permissions=[]
        )
    )
    result = await handler(request_pb.SerializeToString())
    assert grants_pb2.CheckOAuthPermissionsOutput.FromString(result) == grants_pb2.CheckOAuthPermissionsOutput(
        result=grants_pb2.CheckPermissionsStatus(is_have_permissions=True)
    )


@pytest.mark.parametrize(
    ('side_effect', 'expected_message'),
    [
        (
            NoSuchOAuthTokenException,
            grants_pb2.CheckOAuthPermissionsOutput(no_such_oauth_token=common_pb2.ErrorMessageResponse(message=''))
        ),
        (
            InactiveOAuthToken,
            grants_pb2.CheckOAuthPermissionsOutput(inactive_oauth_token=common_pb2.ErrorMessageResponse(message=''))
        ),
        (
            NoSuchPermissionException,
            grants_pb2.CheckOAuthPermissionsOutput(no_such_permissions=common_pb2.ErrorMessageResponse(message=''))
        ),
    ]
)
async def test_check_oauth_returns_error(handler, mocker, side_effect, expected_message):
    mock = AsyncMock()
    mock.return_value = None
    mock.side_effect = [side_effect]

    mocker.patch(
        "crm.agency_cabinet.grants.server.src.procedures.role_manager.UserManager.check_oauth_permissions",
        side_effect=mock,
    )
    request_pb = request_pb2.RpcRequest(
        check_oauth_permissions=grants_pb2.CheckOAuthPermissionsRequest(
            app_client_id='aaaaaaaa',
            permissions=[]
        )
    )

    result = await handler(request_pb.SerializeToString())
    assert grants_pb2.CheckOAuthPermissionsOutput.FromString(result) == expected_message
