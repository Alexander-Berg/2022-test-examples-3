import pytest
from crm.agency_cabinet.grants.common import QUEUE
from crm.agency_cabinet.grants.proto import request_pb2, access_level_pb2, role_pb2, common_pb2
from crm.agency_cabinet.grants.common import structs
from crm.agency_cabinet.grants import client as grants_client
from smb.common.rmq.rpc.client import RmqRpcClient


async def test_check_access_level(client: grants_client.GrantsClient, rmq_rpc_client: RmqRpcClient):
    mocked_output = access_level_pb2.CheckAccessLevelOutput(
        access_level=access_level_pb2.AccessLevel(level=access_level_pb2.AccessLevelEnum.DENY)
    )
    rmq_rpc_client.send_proto_message.return_value = mocked_output

    response = await client.check_access_level(yandex_uid=100, agency_id=1)

    assert response == structs.AccessLevel.DENY

    request = request_pb2.RpcRequest(check_access_level=access_level_pb2.CheckAccessLevel(yandex_uid=100, agency_id=1))

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name=QUEUE,
        message=request,
        response_message_type=access_level_pb2.CheckAccessLevelOutput
    )


async def test_check_access_level_protocol_error(client: grants_client.GrantsClient, rmq_rpc_client: RmqRpcClient):
    mocked_output = access_level_pb2.CheckAccessLevel(
        agency_id=1,
        yandex_uid=100,
    )

    rmq_rpc_client.send_proto_message.return_value = mocked_output

    with pytest.raises(grants_client.ProtocolError):
        await client.check_access_level(yandex_uid=100, agency_id=1)

    request = request_pb2.RpcRequest(
        check_access_level=access_level_pb2.CheckAccessLevel(yandex_uid=100, agency_id=1))

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name=QUEUE,
        message=request,
        response_message_type=access_level_pb2.CheckAccessLevelOutput
    )


async def test_get_access_scope(client: grants_client.GrantsClient, rmq_rpc_client: RmqRpcClient):
    mocked_output = access_level_pb2.GetAccessScopeOutput(result=access_level_pb2.AccessScope(scope_type=access_level_pb2.BY_ID, agencies=[1, 2, 3]))

    rmq_rpc_client.send_proto_message.return_value = mocked_output

    response = await client.get_access_scope(yandex_uid=100)

    assert response == (structs.AccessScopeType.BY_ID, [1, 2, 3])

    request = request_pb2.RpcRequest(get_access_scope=access_level_pb2.GetAccessScope(yandex_uid=100))

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name=QUEUE,
        message=request,
        response_message_type=access_level_pb2.GetAccessScopeOutput
    )


async def test_get_access_scope_protocol_error(client: grants_client.GrantsClient, rmq_rpc_client: RmqRpcClient):
    mocked_output = access_level_pb2.GetAccessScope(yandex_uid=100)
    rmq_rpc_client.send_proto_message.return_value = mocked_output

    with pytest.raises(grants_client.ProtocolError):
        await client.get_access_scope(yandex_uid=100)

    request = request_pb2.RpcRequest(get_access_scope=access_level_pb2.GetAccessScope(yandex_uid=100))

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name=QUEUE,
        message=request,
        response_message_type=access_level_pb2.GetAccessScopeOutput
    )


async def test_add_role(client: grants_client.GrantsClient, rmq_rpc_client: RmqRpcClient):
    mocked_output = role_pb2.AddRoleOutput(
        success=common_pb2.Empty()
    )
    rmq_rpc_client.send_proto_message.return_value = mocked_output

    response = await client.add_role(
        agency_id=1,
        email='user@yandex.ru',
        real_ip='127.0.0.1',
    )

    assert response is None

    request = request_pb2.RpcRequest(add_role=role_pb2.Role(
        agency_id=1,
        email='user@yandex.ru',
        real_ip='127.0.0.1',
    ))

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name=QUEUE,
        message=request,
        response_message_type=role_pb2.AddRoleOutput
    )


async def test_add_role_conflict(client: grants_client.GrantsClient, rmq_rpc_client: RmqRpcClient):
    mocked_output = role_pb2.AddRoleOutput(
        conflicting_role_exists=common_pb2.Empty()
    )
    rmq_rpc_client.send_proto_message.return_value = mocked_output

    with pytest.raises(grants_client.ConflictRoleException):
        await client.add_role(
            agency_id=1,
            email='user@yandex.ru',
            real_ip='127.0.0.1',
        )

    request = request_pb2.RpcRequest(add_role=role_pb2.Role(
        agency_id=1,
        email='user@yandex.ru',
        real_ip='127.0.0.1',
    ))

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name=QUEUE,
        message=request,
        response_message_type=role_pb2.AddRoleOutput
    )


async def test_add_role_protocol_error(client: grants_client.GrantsClient, rmq_rpc_client: RmqRpcClient):
    mocked_output = role_pb2.Role(
        agency_id=1,
        email='user@yandex.ru',
        real_ip='127.0.0.1',
    )

    rmq_rpc_client.send_proto_message.return_value = mocked_output

    with pytest.raises(grants_client.ProtocolError):
        await client.add_role(
            agency_id=1,
            email='user@yandex.ru',
            real_ip='127.0.0.1',
        )

    request = request_pb2.RpcRequest(add_role=role_pb2.Role(
        agency_id=1,
        email='user@yandex.ru',
        real_ip='127.0.0.1',
    ))

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name=QUEUE,
        message=request,
        response_message_type=role_pb2.AddRoleOutput
    )


async def test_remove_role(client: grants_client.GrantsClient, rmq_rpc_client: RmqRpcClient):
    mocked_output = role_pb2.RemoveRoleOutput(
        success=common_pb2.Empty()
    )
    rmq_rpc_client.send_proto_message.return_value = mocked_output

    response = await client.remove_role(
        agency_id=1,
        email='user@yandex.ru'
    )

    assert response is None

    request = request_pb2.RpcRequest(remove_role=role_pb2.Role(
        agency_id=1,
        email='user@yandex.ru'
    ))

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name=QUEUE,
        message=request,
        response_message_type=role_pb2.RemoveRoleOutput
    )


async def test_remove_role_no_such_role(client: grants_client.GrantsClient, rmq_rpc_client: RmqRpcClient):
    mocked_output = role_pb2.RemoveRoleOutput(
        no_such_role=common_pb2.Empty()
    )
    rmq_rpc_client.send_proto_message.return_value = mocked_output

    with pytest.raises(grants_client.NoSuchRoleException):
        await client.remove_role(
            agency_id=1,
            email='user@yandex.ru'
        )

    request = request_pb2.RpcRequest(remove_role=role_pb2.Role(
        agency_id=1,
        email='user@yandex.ru'
    ))

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name=QUEUE,
        message=request,
        response_message_type=role_pb2.RemoveRoleOutput
    )


async def test_remove_role_protocol_error(client: grants_client.GrantsClient, rmq_rpc_client: RmqRpcClient):
    mocked_output = role_pb2.Role(
        agency_id=1,
        email='user@yandex.ru'
    )
    rmq_rpc_client.send_proto_message.return_value = mocked_output

    with pytest.raises(grants_client.ProtocolError):
        await client.remove_role(
            agency_id=1,
            email='user@yandex.ru'
        )

    request = request_pb2.RpcRequest(remove_role=role_pb2.Role(
        agency_id=1,
        email='user@yandex.ru'
    ))

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name=QUEUE,
        message=request,
        response_message_type=role_pb2.RemoveRoleOutput
    )


async def test_get_all_internal_roles(client: grants_client.GrantsClient, rmq_rpc_client: RmqRpcClient):
    mocked_output = role_pb2.GetAllInternalRolesOutput(result=role_pb2.InternalRoleList(roles=[
        role_pb2.InternalRole(
            staff_login='test_yandex_manager_123',
            email='test_yandex_manager_123@mail.ru',
            real_ip='127.0.0.1',
        ),
        role_pb2.InternalRole(
            staff_login='test_yandex_manager_456',
            email='test_yandex_manager_456@mail.ru',
            real_ip='127.0.0.1',
        ),
    ]))

    rmq_rpc_client.send_proto_message.return_value = mocked_output

    response = await client.get_all_internal_roles()

    assert response == [
        structs.InternalRole(
            staff_login='test_yandex_manager_123',
            email='test_yandex_manager_123@mail.ru',
            real_ip='127.0.0.1',
        ),
        structs.InternalRole(
            staff_login='test_yandex_manager_456',
            email='test_yandex_manager_456@mail.ru',
            real_ip='127.0.0.1',
        )
    ]

    request = request_pb2.RpcRequest(get_all_internal_roles=common_pb2.Empty())

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name=QUEUE,
        message=request,
        response_message_type=role_pb2.GetAllInternalRolesOutput
    )


async def test_get_all_internal_roles_empty(client: grants_client.GrantsClient, rmq_rpc_client: RmqRpcClient):
    mocked_output = role_pb2.GetAllInternalRolesOutput(result=role_pb2.InternalRoleList(roles=[]))

    rmq_rpc_client.send_proto_message.return_value = mocked_output

    response = await client.get_all_internal_roles()

    assert response == []

    request = request_pb2.RpcRequest(get_all_internal_roles=common_pb2.Empty())

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name=QUEUE,
        message=request,
        response_message_type=role_pb2.GetAllInternalRolesOutput
    )


async def test_add_internal_role(client: grants_client.GrantsClient, rmq_rpc_client: RmqRpcClient):
    mocked_output = role_pb2.AddRoleOutput(
        success=common_pb2.Empty()
    )
    rmq_rpc_client.send_proto_message.return_value = mocked_output

    response = await client.add_internal_role(
        staff_login='user',
        email='user@yandex.ru',
        real_ip='127.0.0.1',
    )

    assert response is None

    request = request_pb2.RpcRequest(add_internal_role=role_pb2.InternalRole(
        staff_login='user',
        email='user@yandex.ru',
        real_ip='127.0.0.1',
    ))

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name=QUEUE,
        message=request,
        response_message_type=role_pb2.AddRoleOutput
    )


async def test_add_internal_role_conflict(client: grants_client.GrantsClient, rmq_rpc_client: RmqRpcClient):
    mocked_output = role_pb2.AddRoleOutput(
        conflicting_role_exists=common_pb2.Empty()
    )
    rmq_rpc_client.send_proto_message.return_value = mocked_output

    with pytest.raises(grants_client.ConflictRoleException):
        await client.add_internal_role(
            staff_login='user',
            email='user@yandex.ru',
            real_ip='127.0.0.1',
        )

    request = request_pb2.RpcRequest(add_internal_role=role_pb2.InternalRole(
        staff_login='user',
        email='user@yandex.ru',
        real_ip='127.0.0.1',
    ))

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name=QUEUE,
        message=request,
        response_message_type=role_pb2.AddRoleOutput
    )


async def test_add_internal_role_protocol_error(client: grants_client.GrantsClient, rmq_rpc_client: RmqRpcClient):
    wrong_output = role_pb2.InternalRole(
        staff_login='user',
        email='user@yandex.ru',
        real_ip='127.0.0.1',
    )

    rmq_rpc_client.send_proto_message.return_value = wrong_output

    with pytest.raises(grants_client.ProtocolError):
        await client.add_internal_role(
            staff_login='user',
            email='user@yandex.ru',
            real_ip='127.0.0.1',
        )

    request = request_pb2.RpcRequest(add_internal_role=role_pb2.InternalRole(
        staff_login='user',
        email='user@yandex.ru',
        real_ip='127.0.0.1',
    ))

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name=QUEUE,
        message=request,
        response_message_type=role_pb2.AddRoleOutput
    )


async def test_remove_internal_role(client: grants_client.GrantsClient, rmq_rpc_client: RmqRpcClient):
    mocked_output = role_pb2.RemoveRoleOutput(
        success=common_pb2.Empty()
    )
    rmq_rpc_client.send_proto_message.return_value = mocked_output

    response = await client.remove_internal_role(
        staff_login='user',
        email='user@yandex.ru'
    )

    assert response is None

    request = request_pb2.RpcRequest(remove_internal_role=role_pb2.InternalRole(
        staff_login='user',
        email='user@yandex.ru'
    ))

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name=QUEUE,
        message=request,
        response_message_type=role_pb2.RemoveRoleOutput
    )


async def test_remove_internal_role_no_such_role(client: grants_client.GrantsClient, rmq_rpc_client: RmqRpcClient):
    mocked_output = role_pb2.RemoveRoleOutput(
        no_such_role=common_pb2.Empty()
    )
    rmq_rpc_client.send_proto_message.return_value = mocked_output

    with pytest.raises(grants_client.NoSuchRoleException):
        await client.remove_internal_role(
            staff_login='user',
            email='user@yandex.ru'
        )

    request = request_pb2.RpcRequest(remove_internal_role=role_pb2.InternalRole(
        staff_login='user',
        email='user@yandex.ru'
    ))

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name=QUEUE,
        message=request,
        response_message_type=role_pb2.RemoveRoleOutput
    )


async def test_remove_internal_role_protocol_error(client: grants_client.GrantsClient, rmq_rpc_client: RmqRpcClient):
    wrong_output = role_pb2.InternalRole(
        staff_login='user',
        email='user@yandex.ru'
    )
    rmq_rpc_client.send_proto_message.return_value = wrong_output

    with pytest.raises(grants_client.ProtocolError):
        await client.remove_internal_role(
            staff_login='user',
            email='user@yandex.ru'
        )

    request = request_pb2.RpcRequest(remove_internal_role=role_pb2.InternalRole(
        staff_login='user',
        email='user@yandex.ru'
    ))

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name=QUEUE,
        message=request,
        response_message_type=role_pb2.RemoveRoleOutput
    )
