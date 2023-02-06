from crm.agency_cabinet.grants.proto import request_pb2, grants_pb2
from crm.agency_cabinet.grants.common.structs import GetAccessibleAgenciesResponse
from crm.agency_cabinet.grants.server.src.handler import Handler


async def test_get_accessible_agencies(handler: Handler, fixture_users, fixture_user_roles, fixture_partners_map, fixture_user_permissions):
    request_pb = request_pb2.RpcRequest(
        get_accessible_agencies=grants_pb2.GetAccessibleAgenciesRequest(
            yandex_uid=fixture_users[2].yandex_uid,  # Главный представитель 1
        )
    )
    data = await handler(request_pb.SerializeToString())
    message = grants_pb2.GetAccessibleAgenciesOutput.FromString(data)

    res = GetAccessibleAgenciesResponse.from_proto(message.result)

    assert res == GetAccessibleAgenciesResponse(
        agencies=[fixture_partners_map[3].agency_id]
    )


async def test_get_no_accessible_agencies_for_suggested_user(handler: Handler, fixture_users, fixture_user_roles, fixture_partners_map, fixture_user_permissions):
    request_pb = request_pb2.RpcRequest(
        get_accessible_agencies=grants_pb2.GetAccessibleAgenciesRequest(
            yandex_uid=fixture_users[3].yandex_uid,  # Потенциальный пользователь агентства 1
        )
    )
    data = await handler(request_pb.SerializeToString())
    message = grants_pb2.GetAccessibleAgenciesOutput.FromString(data)

    res = GetAccessibleAgenciesResponse.from_proto(message.result)

    assert res == GetAccessibleAgenciesResponse(agencies=[])
