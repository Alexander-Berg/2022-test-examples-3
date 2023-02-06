import pytest
from crm.agency_cabinet.grants.common import QUEUE
from crm.agency_cabinet.grants.proto import request_pb2, grants_pb2, common_pb2
from crm.agency_cabinet.grants.common import structs
from crm.agency_cabinet.grants.client import NoSuchPermissionException, NoSuchOAuthTokenException, InactiveOAuthToken


async def test_check_oauth_sends_correct_request(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = grants_pb2.CheckOAuthPermissionsOutput(
        result=grants_pb2.CheckPermissionsStatus(
            is_have_permissions=True,
        )
    )

    got = await client.check_oauth_permissions(app_client_id='aaaaaaaa', permissions=[])

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name=QUEUE,
        message=request_pb2.RpcRequest(
            check_oauth_permissions=grants_pb2.CheckOAuthPermissionsRequest(
                app_client_id='aaaaaaaa',
                permissions=[]
            )
        ),
        response_message_type=grants_pb2.CheckOAuthPermissionsOutput,
    )

    assert got == structs.CheckPermissionsResponse(is_have_permissions=True)


async def test_check_oauth_inactive_token(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = grants_pb2.CheckOAuthPermissionsOutput(
        inactive_oauth_token=common_pb2.ErrorMessageResponse(message='')
    )

    with pytest.raises(InactiveOAuthToken):
        await client.check_oauth_permissions(app_client_id='aaaaaaaa', permissions=[])


async def test_check_oauth_no_such_token(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = grants_pb2.CheckOAuthPermissionsOutput(
        no_such_oauth_token=common_pb2.ErrorMessageResponse(message='')
    )

    with pytest.raises(NoSuchOAuthTokenException):
        await client.check_oauth_permissions(app_client_id='aaaaaaaa', permissions=[])


async def test_check_oauth_no_such_permission(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = grants_pb2.CheckOAuthPermissionsOutput(
        no_such_permissions=common_pb2.ErrorMessageResponse(message='')
    )

    with pytest.raises(NoSuchPermissionException):
        await client.check_oauth_permissions(app_client_id='aaaaaaaa', permissions=[])
