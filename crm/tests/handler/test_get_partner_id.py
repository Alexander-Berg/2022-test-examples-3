from crm.agency_cabinet.grants.proto import request_pb2, partners_pb2
from crm.agency_cabinet.grants.common.structs import GetPartnerIDResponse, GetPartnerIDInput
from crm.agency_cabinet.grants.server.src.handler import Handler


async def test_get_partner_id(handler: Handler, fixture_users, fixture_user_roles, fixture_permissions,
                              fixture_partners_map, fixture_user_permissions, fixture_role_permissions,
                              fixture_partners):
    request_pb = request_pb2.RpcRequest(
        get_partner_id=GetPartnerIDInput(
            yandex_uid=fixture_users[2].yandex_uid,  # Главный представитель 1,
            external_id=fixture_partners[0].external_id,
            type=fixture_partners[0].type
        ).to_proto()
    )
    data = await handler(request_pb.SerializeToString())
    message = partners_pb2.GetPartnerIDOutput.FromString(data)

    res = GetPartnerIDResponse.from_proto(message.result)
    assert res == GetPartnerIDResponse(
        partner_id=fixture_partners[0].id
    )


async def test_get_partner_id_not_found(handler: Handler, fixture_users, fixture_user_roles, fixture_permissions,
                                        fixture_partners_map, fixture_user_permissions, fixture_role_permissions,
                                        fixture_partners):
    request_pb = request_pb2.RpcRequest(
        get_partner_id=GetPartnerIDInput(
            yandex_uid=fixture_users[2].yandex_uid,  # Главный представитель 1,
            external_id=fixture_partners[0].external_id + '123',
            type=fixture_partners[0].type
        ).to_proto()
    )
    data = await handler(request_pb.SerializeToString())
    message = partners_pb2.GetPartnerOutput.FromString(data)

    assert message.HasField('not_found')


async def test_get_partner_id_no_permission(handler: Handler, fixture_users, fixture_user_roles,
                                            fixture_permissions,
                                            fixture_partners_map, fixture_user_permissions,
                                            fixture_role_permissions,
                                            fixture_partners):
    request_pb = request_pb2.RpcRequest(
        get_partner_id=GetPartnerIDInput(
            yandex_uid=fixture_users[2].yandex_uid,  # Главный представитель 1,
            external_id=fixture_partners[1].external_id,
            type=fixture_partners[0].type
        ).to_proto()
    )
    data = await handler(request_pb.SerializeToString())
    message = partners_pb2.GetPartnerOutput.FromString(data)

    assert message.HasField('not_have_permission')
