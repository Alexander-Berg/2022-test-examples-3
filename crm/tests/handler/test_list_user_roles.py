from crm.agency_cabinet.grants.proto import request_pb2, role_pb2
from crm.agency_cabinet.grants.common.structs import ListUserRolesResponse, Permission, RolePermissions, UserRole, ErrorMessageResponse
from crm.agency_cabinet.grants.server.src.handler import Handler


async def test_list_user_roles(handler: Handler, fixture_users, fixture_user_roles, fixture_permissions,
                               fixture_partners_map, fixture_user_permissions):
    request_pb = request_pb2.RpcRequest(
        list_user_roles=role_pb2.ListUserRolesRequest(
            yandex_uid=fixture_users[2].yandex_uid,  # Главный представитель 1
            agency_id=1
        )
    )
    data = await handler(request_pb.SerializeToString())
    message = role_pb2.ListUserRolesOutput.FromString(data)

    res = ListUserRolesResponse.from_proto(message.result)
    assert res == ListUserRolesResponse(
        roles=[
            RolePermissions(
                role=UserRole(
                    name=fixture_user_roles[3].name,  # Главный представитель 1
                    role_id=fixture_user_roles[3].id
                ),
                permissions=[
                    Permission(
                        name=fixture_permissions[0].name,  # Доступ к управлению ролями
                    ),
                ]
            ),
        ]
    )


async def test_list_user_roles_no_admin_role(handler: Handler, fixture_users, fixture_user_roles, fixture_permissions,
                                             fixture_partners_map, fixture_user_permissions):
    request_pb = request_pb2.RpcRequest(
        list_user_roles=role_pb2.ListUserRolesRequest(
            yandex_uid=fixture_users[0].yandex_uid,  # Менеджер агентства 1
            agency_id=1
        )
    )
    data = await handler(request_pb.SerializeToString())
    message = role_pb2.ListUserRolesOutput.FromString(data)

    res = ListUserRolesResponse.from_proto(message.result)
    assert res == ListUserRolesResponse(
        roles=[
            RolePermissions(
                role=UserRole(
                    name=fixture_user_roles[0].name,  # Главный представитель 1
                    role_id=fixture_user_roles[0].id
                ),
                permissions=[
                    Permission(
                        name=fixture_permissions[1].name,  # Доступ к бонусам
                    ),
                    Permission(
                        name=fixture_permissions[2].name,  # Доступ к сертификатам
                    ),
                ]
            ),
        ]
    )


async def test_list_user_roles_for_suggested_user(handler: Handler, fixture_users, fixture_user_roles,
                                                  fixture_permissions, fixture_partners_map, fixture_user_permissions):
    request_pb = request_pb2.RpcRequest(
        list_user_roles=role_pb2.ListUserRolesRequest(
            yandex_uid=fixture_users[3].yandex_uid,  # Потенциальный пользователь агентства 1 без ролей
            agency_id=1
        )
    )
    data = await handler(request_pb.SerializeToString())
    message = role_pb2.ListUserRolesOutput.FromString(data)

    assert message.HasField('not_have_permission')
    res = ErrorMessageResponse.from_proto(message.not_have_permission)

    assert res == ErrorMessageResponse(
        message="Suggested user has no permissions"
    )
