from crm.agency_cabinet.grants.proto import request_pb2, grants_pb2
from crm.agency_cabinet.grants.common.structs import ListUserPermissionsResponse, Permission
from crm.agency_cabinet.grants.server.src.handler import Handler


async def test_list_user_permissions(handler: Handler, fixture_users, fixture_user_roles, fixture_permissions,
                                     fixture_partners_map, fixture_user_permissions):
    request_pb = request_pb2.RpcRequest(
        list_user_permissions=grants_pb2.ListUserPermissionsRequest(
            yandex_uid=fixture_users[2].yandex_uid,  # Главный представитель 1
            agency_id=1
        )
    )
    data = await handler(request_pb.SerializeToString())
    message = grants_pb2.ListUserPermissionsOutput.FromString(data)

    res = ListUserPermissionsResponse.from_proto(message.result)
    assert res == ListUserPermissionsResponse(
        permissions=[
            Permission(
                name=fixture_permissions[0].name,  # Доступ к управлению ролями
            ),
        ]
    )
