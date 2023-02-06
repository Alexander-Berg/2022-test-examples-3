from sqlalchemy import and_
from crm.agency_cabinet.grants.proto import access_level_pb2, request_pb2, role_pb2, common_pb2
from crm.agency_cabinet.grants.server.src.handler import Handler
from crm.agency_cabinet.grants.server.src.db import models
from crm.agency_cabinet.common.blackbox import BlackboxClient


async def test_add_internal_role_correct(blackbox_client: BlackboxClient, handler: Handler):
    request_pb = request_pb2.RpcRequest(
        add_internal_role=role_pb2.InternalRole(
            staff_login='user',
            email='user@yandex.ru'
        )
    )

    blackbox_client.get_uid_by_email.return_value = 123123123
    output = await handler(request_pb.SerializeToString())

    assert role_pb2.AddRoleOutput.FromString(output) == role_pb2.AddRoleOutput(success=common_pb2.Empty())

    new_role = await models.Role.query.where(
        and_(
            models.Role.agency_id.is_(None),
            models.Role.yandex_uid == 123123123,
            models.Role.email == 'user@yandex.ru',
            models.Role.staff_login == 'user'
        )
    ).gino.first()

    assert new_role is not None

    await new_role.delete()


async def test_add_role_conflict_yandex_uid(fixture_yandex_role: models.Role, blackbox_client: BlackboxClient, handler: Handler):
    request_pb = request_pb2.RpcRequest(
        add_internal_role=role_pb2.InternalRole(
            staff_login='some_login',
            email='some_email@yandex.ru'
        )
    )

    blackbox_client.get_uid_by_email.return_value = fixture_yandex_role.yandex_uid
    output = await handler(request_pb.SerializeToString())

    assert role_pb2.AddRoleOutput.FromString(output) == role_pb2.AddRoleOutput(conflicting_role_exists=common_pb2.Empty())


async def test_add_role_conflict_staff_login(fixture_yandex_role: models.Role, blackbox_client: BlackboxClient, handler: Handler):
    request_pb = request_pb2.RpcRequest(
        add_internal_role=role_pb2.InternalRole(
            staff_login=fixture_yandex_role.staff_login,
            email='some_email@yandex.ru'
        )
    )

    blackbox_client.get_uid_by_email.return_value = fixture_yandex_role.yandex_uid
    output = await handler(request_pb.SerializeToString())

    assert role_pb2.AddRoleOutput.FromString(output) == role_pb2.AddRoleOutput(conflicting_role_exists=common_pb2.Empty())


async def test_add_role_correct(blackbox_client: BlackboxClient, handler: Handler):
    request_pb = request_pb2.RpcRequest(
        add_role=role_pb2.Role(
            agency_id=1,
            email='user@yandex.ru'
        )
    )

    blackbox_client.get_uid_by_email.return_value = 123123123
    output = await handler(request_pb.SerializeToString())

    assert role_pb2.AddRoleOutput.FromString(output) == role_pb2.AddRoleOutput(success=common_pb2.Empty())

    new_role = await models.Role.query.where(
        and_(
            models.Role.agency_id == 1,
            models.Role.yandex_uid == 123123123,
            models.Role.email == 'user@yandex.ru',
            )
    ).gino.first()

    assert new_role is not None

    await new_role.delete()


async def test_add_role_conflict(fixture_role: models.Role, blackbox_client: BlackboxClient, handler: Handler):
    request_pb = request_pb2.RpcRequest(
        add_role=role_pb2.Role(
            agency_id=fixture_role.agency_id,
            email=fixture_role.email
        )
    )

    blackbox_client.get_uid_by_email.return_value = fixture_role.yandex_uid
    output = await handler(request_pb.SerializeToString())

    assert role_pb2.AddRoleOutput.FromString(output) == role_pb2.AddRoleOutput(conflicting_role_exists=common_pb2.Empty())


async def test_check_access_level_deny(handler: Handler):
    request_pb = request_pb2.RpcRequest(
        check_access_level=access_level_pb2.CheckAccessLevel(
            yandex_uid=404,
            agency_id=404
        )
    )

    output = await handler(request_pb.SerializeToString())

    expected_output = access_level_pb2.CheckAccessLevelOutput(
        access_level=access_level_pb2.AccessLevel(
            level=access_level_pb2.AccessLevelEnum.DENY
        )
    )

    assert access_level_pb2.CheckAccessLevelOutput.FromString(output) == expected_output


async def test_check_access_level_deny_unknown_agency(handler: Handler, fixture_role: models.Role):
    request_pb = request_pb2.RpcRequest(
        check_access_level=access_level_pb2.CheckAccessLevel(
            yandex_uid=fixture_role.yandex_uid,
            agency_id=404
        )
    )

    output = await handler(request_pb.SerializeToString())

    expected_output = access_level_pb2.CheckAccessLevelOutput(
        access_level=access_level_pb2.AccessLevel(
            level=access_level_pb2.AccessLevelEnum.DENY
        )
    )

    assert access_level_pb2.CheckAccessLevelOutput.FromString(output) == expected_output


async def test_check_access_level_deny_not_active_role(handler: Handler, fixture_not_active_role: models.Role):
    request_pb = request_pb2.RpcRequest(
        check_access_level=access_level_pb2.CheckAccessLevel(
            yandex_uid=fixture_not_active_role.yandex_uid,
            agency_id=fixture_not_active_role.agency_id
        )
    )

    output = await handler(request_pb.SerializeToString())

    expected_output = access_level_pb2.CheckAccessLevelOutput(
        access_level=access_level_pb2.AccessLevel(
            level=access_level_pb2.AccessLevelEnum.DENY
        )
    )

    assert access_level_pb2.CheckAccessLevelOutput.FromString(output) == expected_output


async def test_check_access_level_allow(handler: Handler, fixture_role: models.Role):
    request_pb = request_pb2.RpcRequest(
        check_access_level=access_level_pb2.CheckAccessLevel(
            yandex_uid=fixture_role.yandex_uid,
            agency_id=fixture_role.agency_id
        )
    )

    output = await handler(request_pb.SerializeToString())

    expected_output = access_level_pb2.CheckAccessLevelOutput(
        access_level=access_level_pb2.AccessLevel(
            level=access_level_pb2.AccessLevelEnum.ALLOW
        )
    )

    assert access_level_pb2.CheckAccessLevelOutput.FromString(output) == expected_output


async def test_check_access_level_allow_for_yandex(handler: Handler, fixture_yandex_role: models.Role):
    request_pb = request_pb2.RpcRequest(
        check_access_level=access_level_pb2.CheckAccessLevel(
            yandex_uid=fixture_yandex_role.yandex_uid,
            agency_id=123456789
        )
    )

    output = await handler(request_pb.SerializeToString())

    expected_output = access_level_pb2.CheckAccessLevelOutput(
        access_level=access_level_pb2.AccessLevel(
            level=access_level_pb2.AccessLevelEnum.ALLOW
        )
    )

    assert access_level_pb2.CheckAccessLevelOutput.FromString(output) == expected_output


async def test_get_all_internal_roles(handler: Handler, fixture_yandex_role: models.Role):
    request_pb = request_pb2.RpcRequest(
        get_all_internal_roles=common_pb2.Empty()
    )

    output = await handler(request_pb.SerializeToString())

    assert role_pb2.GetAllInternalRolesOutput.FromString(output) == role_pb2.GetAllInternalRolesOutput(
        result=role_pb2.InternalRoleList(
            roles=[
                role_pb2.InternalRole(
                    email=fixture_yandex_role.email,
                    staff_login=fixture_yandex_role.staff_login
                )
            ]
        ))


async def test_remove_internal_role_correct(handler: Handler, fixture_yandex_role: models.Role):
    request_pb = request_pb2.RpcRequest(
        remove_internal_role=role_pb2.InternalRole(
            staff_login=fixture_yandex_role.staff_login,
            email=fixture_yandex_role.email
        )
    )

    output = await handler(request_pb.SerializeToString())

    assert role_pb2.RemoveRoleOutput.FromString(output) == role_pb2.RemoveRoleOutput(success=common_pb2.Empty())

    role = await models.Role.query.where(
        and_(
            models.Role.staff_login == fixture_yandex_role.staff_login,
            models.Role.email == fixture_yandex_role.email,
            )
    ).gino.first()

    assert role is None


async def test_remove_internal_role_no_such_role(handler: Handler):
    request_pb = request_pb2.RpcRequest(
        remove_internal_role=role_pb2.InternalRole(
            staff_login='404',
            email='404@yandex.ru'
        )
    )

    output = await handler(request_pb.SerializeToString())

    assert role_pb2.RemoveRoleOutput.FromString(output) == role_pb2.RemoveRoleOutput(no_such_role=common_pb2.Empty())


async def test_remove_role_correct(handler: Handler, fixture_role: models.Role):
    request_pb = request_pb2.RpcRequest(
        remove_role=role_pb2.Role(
            agency_id=fixture_role.agency_id,
            email=fixture_role.email
        )
    )

    output = await handler(request_pb.SerializeToString())

    assert role_pb2.RemoveRoleOutput.FromString(output) == role_pb2.RemoveRoleOutput(success=common_pb2.Empty())

    role = await models.Role.query.where(
        and_(
            models.Role.agency_id == fixture_role.agency_id,
            models.Role.email == fixture_role.email,
            )
    ).gino.first()

    assert role is None


async def test_remove_role_no_such_role(handler: Handler):
    request_pb = request_pb2.RpcRequest(
        remove_role=role_pb2.Role(
            agency_id=404,
            email='404@yandex.ru'
        )
    )

    output = await handler(request_pb.SerializeToString())

    assert role_pb2.RemoveRoleOutput.FromString(output) == role_pb2.RemoveRoleOutput(no_such_role=common_pb2.Empty())
