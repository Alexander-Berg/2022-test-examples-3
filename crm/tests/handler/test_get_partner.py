from crm.agency_cabinet.grants.proto import request_pb2, partners_pb2
from crm.agency_cabinet.grants.common.consts.partner import PartnerType
from crm.agency_cabinet.grants.common.structs import Partner
from crm.agency_cabinet.grants.server.src.handler import Handler


async def test_get_partner(handler: Handler, fixture_users, fixture_user_roles, fixture_permissions,
                                       fixture_partners_map, fixture_user_permissions, fixture_role_permissions,
                                       fixture_partners):
    request_pb = request_pb2.RpcRequest(
        get_partner=partners_pb2.GetPartnerInput(
            yandex_uid=fixture_users[2].yandex_uid,  # Главный представитель 1,
            partner_id=fixture_partners[0].id
        )
    )
    data = await handler(request_pb.SerializeToString())
    message = partners_pb2.GetPartnerOutput.FromString(data)

    res = Partner.from_proto(message.result)
    assert res == Partner(
        external_id=str(fixture_partners[0].external_id),
        type=PartnerType.agency,
        partner_id=fixture_partners[0].id,
        name=fixture_partners[0].name
    )


async def test_get_partner_not_found(handler: Handler, fixture_users, fixture_user_roles, fixture_permissions,
                                                 fixture_partners_map, fixture_user_permissions, fixture_role_permissions,
                                                 fixture_partners):
    request_pb = request_pb2.RpcRequest(
        get_partner=partners_pb2.GetPartnerInput(
            yandex_uid=fixture_users[2].yandex_uid,  # Главный представитель 1,
            partner_id=fixture_partners[0].id + 100
        )
    )
    data = await handler(request_pb.SerializeToString())
    message = partners_pb2.GetPartnerOutput.FromString(data)

    assert message.HasField('not_found')


async def test_get_partner_no_permission(handler: Handler, fixture_users, fixture_user_roles,
                                                     fixture_permissions,
                                                     fixture_partners_map, fixture_user_permissions,
                                                     fixture_role_permissions,
                                                     fixture_partners):
    request_pb = request_pb2.RpcRequest(
        get_partner=partners_pb2.GetPartnerInput(
            yandex_uid=fixture_users[2].yandex_uid,  # Главный представитель 1,
            partner_id=fixture_partners[1].id
        )
    )
    data = await handler(request_pb.SerializeToString())
    message = partners_pb2.GetPartnerOutput.FromString(data)

    assert message.HasField('not_have_permission')
