from crm.agency_cabinet.grants.proto import request_pb2, role_pb2
from crm.agency_cabinet.grants.common.structs import ListRolesResponse, Permission, RolePermissions, UserRole
from crm.agency_cabinet.grants.server.src.handler import Handler


async def test_list_roles(handler: Handler, fixture_users, fixture_user_roles, fixture_permissions,
                          fixture_partners_map, fixture_user_permissions, fixture_role_permissions):
    request_pb = request_pb2.RpcRequest(
        list_roles=role_pb2.ListRolesRequest(
            yandex_uid=fixture_users[2].yandex_uid,  # Главный представитель 1
            agency_id=1
        )
    )
    data = await handler(request_pb.SerializeToString())
    message = role_pb2.ListRolesOutput.FromString(data)

    res = ListRolesResponse.from_proto(message.result)
    assert res == ListRolesResponse(
        roles=[
            RolePermissions(
                role=UserRole(
                    name=fixture_user_roles[0].name,  # Менеджер агентства 1
                    role_id=fixture_user_roles[0].id
                ),
                permissions=[
                    Permission(
                        name=fixture_permissions[1].name,  # Бонусы
                        is_editable=fixture_role_permissions[0].is_editable
                    ),
                    Permission(
                        name=fixture_permissions[2].name,  # Сертификаты
                        is_editable=fixture_role_permissions[1].is_editable
                    ),
                ]
            ),
            RolePermissions(
                role=UserRole(
                    name=fixture_user_roles[6].name,  # Администратор 1
                    role_id=fixture_user_roles[6].id
                ),
                permissions=[
                    Permission(
                        name=fixture_permissions[0].name,  # Доступ к управлению ролями
                        is_editable=fixture_role_permissions[8].is_editable
                    ),
                ]
            ),
        ]
    )
