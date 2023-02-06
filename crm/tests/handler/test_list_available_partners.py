from crm.agency_cabinet.grants.proto import request_pb2, partners_pb2, common_pb2
from crm.agency_cabinet.grants.common.structs import Partner, ListAvailablePartnersResponse
from crm.agency_cabinet.grants.server.src.handler import Handler


async def test_list_available_partners(handler: Handler, fixture_users, fixture_user_roles, fixture_permissions,
                                       fixture_partners_map, fixture_user_permissions, fixture_role_permissions,
                                       fixture_partners):
    request_pb = request_pb2.RpcRequest(
        list_available_partners=partners_pb2.ListAvailablePartnersInput(
            yandex_uid=fixture_users[2].yandex_uid,  # Главный представитель 1
        )
    )
    data = await handler(request_pb.SerializeToString())
    message = partners_pb2.ListAvailablePartnersOutput.FromString(data)

    res = ListAvailablePartnersResponse.from_proto(message.result)
    assert res == ListAvailablePartnersResponse(
        partners=[
            Partner(
                partner_id=fixture_partners[0].id,
                external_id=fixture_partners[0].external_id,
                type=fixture_partners[0].type,
                name=fixture_partners[0].name
            )
        ]
    )


async def test_get_no_accessible_partners_for_suggested_user(handler: Handler, fixture_users, fixture_user_roles, fixture_partners_map, fixture_user_permissions):
    request_pb = request_pb2.RpcRequest(
        list_available_partners=partners_pb2.ListAvailablePartnersInput(
            yandex_uid=fixture_users[3].yandex_uid,  # Потенциальный пользователь агентства 1
        )
    )
    data = await handler(request_pb.SerializeToString())
    message = partners_pb2.ListAvailablePartnersOutput.FromString(data)

    res = ListAvailablePartnersResponse.from_proto(message.result)

    assert res == ListAvailablePartnersResponse(partners=[])


async def test_get_error_for_unknown_user(handler: Handler, fixture_users, fixture_user_roles, fixture_partners_map, fixture_user_permissions):
    request_pb = request_pb2.RpcRequest(
        list_available_partners=partners_pb2.ListAvailablePartnersInput(
            yandex_uid=666,  # неизвестный пользователь
        )
    )
    data = await handler(request_pb.SerializeToString())
    message = partners_pb2.ListAvailablePartnersOutput.FromString(data)

    assert message == partners_pb2.ListAvailablePartnersOutput(
        no_such_user=common_pb2.ErrorMessageResponse(message='Couldn\'t find user with yandex_uid = 666')
    )
