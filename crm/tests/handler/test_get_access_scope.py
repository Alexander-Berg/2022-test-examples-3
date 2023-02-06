from crm.agency_cabinet.grants.proto import access_level_pb2, request_pb2
from crm.agency_cabinet.grants.server.src.handler import Handler
from crm.agency_cabinet.grants.server.src.db import models


async def test_get_access_scope(handler: Handler, fixture_role: models.Role):
    request_pb = request_pb2.RpcRequest(
        get_access_scope=access_level_pb2.GetAccessScope(yandex_uid=fixture_role.yandex_uid)
    )

    output = await handler(request_pb.SerializeToString())

    expected_output = access_level_pb2.GetAccessScopeOutput(
        result=access_level_pb2.AccessScope(
            scope_type=access_level_pb2.AccessScopeTypeEnum.BY_ID,
            agencies=[fixture_role.agency_id]
        )
    )

    assert access_level_pb2.GetAccessScopeOutput.FromString(output) == expected_output


async def test_get_access_scope_yandex(handler: Handler, fixture_yandex_role: models.Role):
    request_pb = request_pb2.RpcRequest(
        get_access_scope=access_level_pb2.GetAccessScope(yandex_uid=fixture_yandex_role.yandex_uid)
    )

    output = await handler(request_pb.SerializeToString())

    expected_output = access_level_pb2.GetAccessScopeOutput(
        result=access_level_pb2.AccessScope(
            scope_type=access_level_pb2.AccessScopeTypeEnum.ALL,
            agencies=[]
        )
    )

    assert access_level_pb2.GetAccessScopeOutput.FromString(output) == expected_output


async def test_get_access_scope_empty(handler: Handler):
    request_pb = request_pb2.RpcRequest(
        get_access_scope=access_level_pb2.GetAccessScope(yandex_uid=404)
    )

    output = await handler(request_pb.SerializeToString())

    expected_output = access_level_pb2.GetAccessScopeOutput(
        result=access_level_pb2.AccessScope(
            scope_type=access_level_pb2.AccessScopeTypeEnum.BY_ID,
            agencies=[]
        )
    )

    assert access_level_pb2.GetAccessScopeOutput.FromString(output) == expected_output


async def test_get_access_scope_not_active(handler: Handler, fixture_not_active_role: models.Role):
    request_pb = request_pb2.RpcRequest(
        get_access_scope=access_level_pb2.GetAccessScope(yandex_uid=fixture_not_active_role.yandex_uid)
    )

    output = await handler(request_pb.SerializeToString())

    expected_output = access_level_pb2.GetAccessScopeOutput(
        result=access_level_pb2.AccessScope(
            scope_type=access_level_pb2.AccessScopeTypeEnum.BY_ID,
            agencies=[]
        )
    )

    assert access_level_pb2.GetAccessScopeOutput.FromString(output) == expected_output
