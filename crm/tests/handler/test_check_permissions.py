from crm.agency_cabinet.grants.proto import request_pb2, grants_pb2, permission_pb2
from crm.agency_cabinet.grants.common.structs import CheckPermissionsResponse, Permission, ErrorMessageResponse, Partner
from crm.agency_cabinet.grants.server.src.handler import Handler


async def test_have_permissions(handler: Handler, fixture_users, fixture_user_roles, fixture_partners_map,
                                fixture_user_permissions, fixture_permissions, fixture_partners):
    request_pb = request_pb2.RpcRequest(
        check_permissions=grants_pb2.CheckPermissionsRequest(
            yandex_uid=fixture_users[2].yandex_uid,  # Главный представитель 1
            agency_id=1,
            permissions=[permission_pb2.Permission(name=fixture_permissions[0].name)]

        )
    )
    data = await handler(request_pb.SerializeToString())
    message = grants_pb2.CheckPermissionsOutput.FromString(data)

    res = CheckPermissionsResponse.from_proto(message.result)

    assert res == CheckPermissionsResponse(
        is_have_permissions=True,
        partner=Partner.from_model(fixture_partners[0])
    )


async def test_do_not_have_permissions_for_another_agency(handler: Handler, fixture_users,
                                                          fixture_user_roles, fixture_partners_map, fixture_user_permissions, fixture_permissions,
                                                          fixture_partners):
    request_pb = request_pb2.RpcRequest(
        check_permissions=grants_pb2.CheckPermissionsRequest(
            yandex_uid=fixture_users[2].yandex_uid,  # Главный представитель 1
            agency_id=2,
            permissions=[permission_pb2.Permission(name=fixture_permissions[0].name)]

        )
    )
    data = await handler(request_pb.SerializeToString())
    message = grants_pb2.CheckPermissionsOutput.FromString(data)

    res = CheckPermissionsResponse.from_proto(message.result)

    assert res == CheckPermissionsResponse(
        is_have_permissions=False,
        missed_permissions=[Permission(name=fixture_permissions[0].name)],
    )


async def test_do_not_have_permissions(handler: Handler, fixture_users, fixture_user_roles, fixture_partners_map, fixture_user_permissions, fixture_permissions):
    request_pb = request_pb2.RpcRequest(
        check_permissions=grants_pb2.CheckPermissionsRequest(
            yandex_uid=fixture_users[2].yandex_uid,  # Главный представитель 1
            agency_id=1,
            permissions=[permission_pb2.Permission(name=fixture_permissions[0].name), permission_pb2.Permission(name=fixture_permissions[1].name)]

        )
    )
    data = await handler(request_pb.SerializeToString())
    message = grants_pb2.CheckPermissionsOutput.FromString(data)

    res = CheckPermissionsResponse.from_proto(message.result)

    assert res == CheckPermissionsResponse(
        is_have_permissions=False,
        missed_permissions=[Permission(name=fixture_permissions[1].name)]
    )


async def test_unknown_permissions(handler: Handler, fixture_users, fixture_user_roles, fixture_partners_map, fixture_user_permissions, fixture_permissions):
    request_pb = request_pb2.RpcRequest(
        check_permissions=grants_pb2.CheckPermissionsRequest(
            yandex_uid=fixture_users[2].yandex_uid,  # Главный представитель 1
            agency_id=1,
            permissions=[permission_pb2.Permission(name=fixture_permissions[0].name), permission_pb2.Permission(name='unknown')]

        )
    )
    data = await handler(request_pb.SerializeToString())
    message = grants_pb2.CheckPermissionsOutput.FromString(data)

    assert message.HasField('no_such_permissions')
    res = ErrorMessageResponse.from_proto(message.no_such_permissions)

    assert res == ErrorMessageResponse(
        message="Couldn't find permissions: unknown"
    )


async def test_unknown_user(handler: Handler, fixture_users, fixture_user_roles, fixture_partners_map, fixture_user_permissions, fixture_permissions):
    request_pb = request_pb2.RpcRequest(
        check_permissions=grants_pb2.CheckPermissionsRequest(
            yandex_uid=99999999,  # unknown
            agency_id=1,
            permissions=[permission_pb2.Permission(name=fixture_permissions[0].name)]

        )
    )
    data = await handler(request_pb.SerializeToString())
    message = grants_pb2.CheckPermissionsOutput.FromString(data)

    assert message.HasField('no_such_user')
    res = ErrorMessageResponse.from_proto(message.no_such_user)

    assert res == ErrorMessageResponse(
        message="Couldn't find user with yandex_uid = 99999999"
    )
