import pytest

from sqlalchemy import and_

from crm.agency_cabinet.grants.proto import request_pb2, grants_pb2, permission_pb2
from crm.agency_cabinet.grants.common.structs import ErrorMessageResponse, EditUserResponse
from crm.agency_cabinet.grants.server.src.db import models
from crm.agency_cabinet.grants.server.src.handler import Handler


@pytest.fixture
def nonexistent_yandex_uid():
    return 12345


@pytest.fixture
def nonexistent_user_uid():
    return 12345


@pytest.fixture
def nonexistent_permission_id():
    return 12345


@pytest.fixture
def nonexistent_permission_name():
    return 'fake_permission'


async def test_no_such_user(handler: Handler, fixture_users, fixture_user_roles, fixture_partners_map,
                            fixture_user_permissions, nonexistent_yandex_uid, nonexistent_user_uid):
    request_pb = request_pb2.RpcRequest(
        edit_user=grants_pb2.EditUserRequest(
            yandex_uid=nonexistent_yandex_uid,
            agency_id=1,
            user_id=nonexistent_user_uid,
            roles=[],
        )
    )
    data = await handler(request_pb.SerializeToString())
    message = grants_pb2.EditUserOutput.FromString(data)

    error_message = ErrorMessageResponse.from_proto(message.no_such_user)

    assert error_message.message != ''


async def test_no_such_role(handler: Handler, fixture_users, fixture_user_roles, fixture_partners_map,
                            fixture_permissions, fixture_user_permissions):
    request_pb = request_pb2.RpcRequest(
        edit_user=grants_pb2.EditUserRequest(
            yandex_uid=fixture_users[2].yandex_uid,  # Главный представитель 1
            agency_id=1,
            user_id=fixture_users[2].id,  # Главный представитель 1
            roles=[
                grants_pb2.InputRole(
                    role_id=fixture_user_roles[1].id,  # Менеджер агентства 2
                    permissions=[permission_pb2.Permission(name=fixture_permissions[1].name)]  # Доступ к бонусам
                )
            ],
        )
    )
    data = await handler(request_pb.SerializeToString())
    message = grants_pb2.EditUserOutput.FromString(data)

    error_message = ErrorMessageResponse.from_proto(message.no_such_role)

    assert error_message.message != ''


async def test_no_such_permission(handler: Handler, fixture_users, fixture_user_roles, fixture_partners_map,
                                  fixture_user_permissions, nonexistent_yandex_uid, nonexistent_user_uid,
                                  nonexistent_permission_id, nonexistent_permission_name):
    request_pb = request_pb2.RpcRequest(
        edit_user=grants_pb2.EditUserRequest(
            yandex_uid=fixture_users[2].yandex_uid,  # Главный представитель 1
            agency_id=1,
            user_id=fixture_users[2].id,  # Главный представитель 1
            roles=[
                grants_pb2.InputRole(
                    role_id=fixture_user_roles[3].id,  # Главный представитель 1
                    permissions=[permission_pb2.Permission(name=nonexistent_permission_name)]
                )
            ],
        )
    )
    data = await handler(request_pb.SerializeToString())
    message = grants_pb2.EditUserOutput.FromString(data)

    error_message = ErrorMessageResponse.from_proto(message.no_such_permission)

    assert error_message.message != ''


async def test_no_permission(handler: Handler, fixture_users, fixture_user_roles, fixture_partners_map,
                                  fixture_user_permissions, nonexistent_yandex_uid, nonexistent_user_uid,
                                  nonexistent_permission_id, nonexistent_permission_name):
    request_pb = request_pb2.RpcRequest(
        edit_user=grants_pb2.EditUserRequest(
            yandex_uid=fixture_users[0].yandex_uid,  # Менеджер с ролями агентств 1 и 2
            agency_id=1,
            user_id=fixture_users[2].id,  # Главный представитель 1
            roles=[
                grants_pb2.InputRole(
                    role_id=fixture_user_roles[3].id,  # Главный представитель 1
                    permissions=[permission_pb2.Permission(name=nonexistent_permission_name)]
                )
            ],
        )
    )
    data = await handler(request_pb.SerializeToString())
    message = grants_pb2.EditUserOutput.FromString(data)

    error_message = ErrorMessageResponse.from_proto(message.not_have_permission)

    assert error_message.message != ''


async def test_unsuitable_permission(handler: Handler, fixture_users, fixture_user_roles, fixture_partners_map,
                                     fixture_permissions, fixture_user_permissions, nonexistent_yandex_uid,
                                     nonexistent_user_uid, nonexistent_permission_id, nonexistent_permission_name):
    request_pb = request_pb2.RpcRequest(
        edit_user=grants_pb2.EditUserRequest(
            yandex_uid=fixture_users[2].yandex_uid,  # Главный представитель 1
            agency_id=1,
            user_id=fixture_users[0].id,  # Менеджер с ролями агентств 1 и 2
            roles=[
                grants_pb2.InputRole(
                    role_id=fixture_user_roles[0].id,  # Менеджер агентства 1
                    permissions=[permission_pb2.Permission(name=fixture_permissions[0].name)]  # Доступ ко всему
                )
            ],
        )
    )
    data = await handler(request_pb.SerializeToString())
    message = grants_pb2.EditUserOutput.FromString(data)

    error_message = ErrorMessageResponse.from_proto(message.unsuitable_permission)

    assert error_message.message != ''


async def test_not_editable_permission(handler: Handler, fixture_users, fixture_user_roles, fixture_partners_map,
                                       fixture_permissions, fixture_user_permissions, nonexistent_yandex_uid,
                                       nonexistent_user_uid, nonexistent_permission_id, nonexistent_permission_name):
    request_pb = request_pb2.RpcRequest(
        edit_user=grants_pb2.EditUserRequest(
            yandex_uid=fixture_users[2].yandex_uid,  # Главный представитель 1
            agency_id=1,
            user_id=fixture_users[2].id,  # Главный представитель 1
            roles=[
                grants_pb2.InputRole(
                    role_id=fixture_user_roles[3].id,  # Главный представитель 1
                    permissions=[]  # trying to revoke 'Доступ ко всему', which is not editable
                )
            ],
        )
    )
    data = await handler(request_pb.SerializeToString())
    message = grants_pb2.EditUserOutput.FromString(data)

    error_message = ErrorMessageResponse.from_proto(message.not_editable_permission)

    assert error_message.message != ''


async def test_permission_editing(handler: Handler, fixture_users, fixture_user_roles, fixture_partners_map,
                                  fixture_permissions, fixture_user_permissions, nonexistent_yandex_uid,
                                  nonexistent_user_uid, nonexistent_permission_id, nonexistent_permission_name):
    # disable
    request_pb = request_pb2.RpcRequest(
        edit_user=grants_pb2.EditUserRequest(
            yandex_uid=fixture_users[2].yandex_uid,  # Главный представитель 1
            agency_id=1,
            user_id=fixture_users[0].id,  # Менеджер с ролями агентств 1 и 2
            roles=[
                grants_pb2.InputRole(
                    role_id=fixture_user_roles[0].id,  # role 'Менеджер агентства 1'
                    permissions=[permission_pb2.Permission(name=fixture_permissions[1].name)]  # Доступ к бонусам
                )
            ],
        )
    )
    data = await handler(request_pb.SerializeToString())
    message = grants_pb2.EditUserOutput.FromString(data)

    is_edited = EditUserResponse.from_proto(message.result).is_edited
    assert is_edited

    permissions = await models.UsersRolesPermissionsMap.query.where(
        and_(
            models.UsersRolesPermissionsMap.user_id == fixture_users[0].id,
            models.UsersRolesPermissionsMap.role_id == fixture_user_roles[0].id
        )
    ).gino.all()
    assert len(permissions) == 2

    for permission in permissions:
        if permission.permission_id == fixture_permissions[1].id:  # checking if we only have access to bonuses
            assert permission.is_active
        else:
            assert not permission.is_active

    # enable
    request_pb = request_pb2.RpcRequest(
        edit_user=grants_pb2.EditUserRequest(
            yandex_uid=fixture_users[2].yandex_uid,  # Главный представитель 1
            agency_id=1,
            user_id=fixture_users[0].id,  # Менеджер с ролями агентств 1 и 2
            roles=[
                grants_pb2.InputRole(
                    role_id=fixture_user_roles[0].id,  # роль 'Менеджер агентства 1'
                    permissions=[
                        permission_pb2.Permission(name=fixture_permissions[1].name),  # Доступ к бонусам
                        permission_pb2.Permission(name=fixture_permissions[2].name),  # Доступ к сертификатам
                    ]
                )
            ],
        )
    )
    data = await handler(request_pb.SerializeToString())
    message = grants_pb2.EditUserOutput.FromString(data)

    is_edited = EditUserResponse.from_proto(message.result).is_edited
    assert is_edited

    permissions = await models.UsersRolesPermissionsMap.query.where(
        and_(
            models.UsersRolesPermissionsMap.user_id == fixture_users[0].id,
            models.UsersRolesPermissionsMap.role_id == fixture_user_roles[0].id,
            models.UsersRolesPermissionsMap.is_active
        )
    ).gino.all()
    assert len(permissions) == 2
    assert [permission.permission_id for permission in permissions] == \
           [fixture_permissions[1].id, fixture_permissions[2].id]


async def test_role_editing(handler: Handler, fixture_users, fixture_user_roles, fixture_partners_map,
                            fixture_permissions, fixture_user_permissions, nonexistent_yandex_uid,
                            nonexistent_user_uid, nonexistent_permission_id, nonexistent_permission_name):
    # removing
    request_pb = request_pb2.RpcRequest(
        edit_user=grants_pb2.EditUserRequest(
            yandex_uid=fixture_users[2].yandex_uid,  # Главный представитель 1
            agency_id=1,
            user_id=fixture_users[0].id,  # Менеджер с ролями агентств 1 и 2
            roles=[],
        )
    )
    data = await handler(request_pb.SerializeToString())
    message = grants_pb2.EditUserOutput.FromString(data)

    is_edited = EditUserResponse.from_proto(message.result).is_edited
    assert is_edited

    permissions = await models.UsersRolesPermissionsMap.query.where(
        and_(
            models.UsersRolesPermissionsMap.user_id == fixture_users[0].id,
            models.UsersRolesPermissionsMap.role_id == fixture_user_roles[0].id
        )
    ).gino.all()
    assert not permissions

    # adding
    request_pb = request_pb2.RpcRequest(
        edit_user=grants_pb2.EditUserRequest(
            yandex_uid=fixture_users[2].yandex_uid,  # Главный представитель 1
            agency_id=1,
            user_id=fixture_users[0].id,  # Менеджер с ролями агентств 1 и 2
            roles=[
                grants_pb2.InputRole(
                    role_id=fixture_user_roles[0].id,  # роль 'Менеджер агентства 1'
                    permissions=[
                        permission_pb2.Permission(name=fixture_permissions[1].name),  # Доступ к бонусам
                        permission_pb2.Permission(name=fixture_permissions[2].name),  # Доступ к сертификатам
                    ]
                )
            ],
        )
    )
    data = await handler(request_pb.SerializeToString())
    message = grants_pb2.EditUserOutput.FromString(data)

    is_edited = EditUserResponse.from_proto(message.result).is_edited
    assert is_edited

    permissions = await models.UsersRolesPermissionsMap.query.where(
        and_(
            models.UsersRolesPermissionsMap.user_id == fixture_users[0].id,
            models.UsersRolesPermissionsMap.role_id == fixture_user_roles[0].id
        )
    ).gino.all()
    assert len(permissions) == 2
    assert [permission.permission_id for permission in permissions] == \
           [fixture_permissions[1].id, fixture_permissions[2].id]
