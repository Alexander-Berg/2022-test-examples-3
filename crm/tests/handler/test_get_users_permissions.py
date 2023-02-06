from crm.agency_cabinet.grants.proto import request_pb2, grants_pb2
from crm.agency_cabinet.grants.common.structs import (
    GetUsersPermissionsResponse,
    UserPermissions,
    User,
    Permission,
    RolePermissions,
    UserRole,
    ErrorMessageResponse
)
from crm.agency_cabinet.grants.server.src.handler import Handler


async def test_get_external_users(handler: Handler, fixture_users, fixture_user_roles, fixture_partners_map, fixture_user_permissions):
    request_pb = request_pb2.RpcRequest(
        get_users_permissions=grants_pb2.GetUsersPermissions(
            yandex_uid=fixture_users[2].yandex_uid,  # Главный представитель 1
            agency_id=1
        )
    )
    data = await handler(request_pb.SerializeToString())
    message = grants_pb2.GetUsersPermissionsOutput.FromString(data)

    res = GetUsersPermissionsResponse.from_proto(message.result)

    # пользователи агентства 1

    assert res == GetUsersPermissionsResponse(
        users=[
            UserPermissions(
                user=User(
                    user_id=fixture_users[0].id,
                    name='Менеджер с ролями агентств 1 и 2',
                    email='manager@agency12',
                    login='manager1',
                    avatar_id=None,
                    current=False
                ),
                roles_permissions=[
                    RolePermissions(
                        role=UserRole(role_id=fixture_user_roles[0].id, name='manager'),
                        permissions=[Permission(name='bonuses'), Permission(name='certificate')]
                    )
                ]),
            UserPermissions(
                user=User(
                    user_id=fixture_users[2].id,
                    name='Главный представитель агентства 1',
                    email='main@agency1',
                    login='main1',
                    avatar_id=None,
                    current=True
                ),
                roles_permissions=[
                    RolePermissions(
                        role=UserRole(role_id=fixture_user_roles[3].id, name='owner'),
                        permissions=[Permission(name='roles')]
                    )
                ]
            )
        ]
    )
    # TODO: check if user have permission to get list?
    # сейчас менеджер и любой пользователь тоже может спокойно запросить список прав всех пользователь
    # хотя в gateway будет прикрытие

    request_pb = request_pb2.RpcRequest(
        get_users_permissions=grants_pb2.GetUsersPermissions(
            yandex_uid=fixture_users[0].yandex_uid,  # Менеджер 1
            agency_id=1
        )
    )
    data = await handler(request_pb.SerializeToString())
    message = grants_pb2.GetUsersPermissionsOutput.FromString(data)

    res = ErrorMessageResponse.from_proto(message.not_have_permission)

    # пользователи агентства 1 запрошенные от имени менджера

    assert res == ErrorMessageResponse(
        message='Missing permissions: roles'
    )
