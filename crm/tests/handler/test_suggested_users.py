from crm.agency_cabinet.grants.proto import request_pb2, grants_pb2
from crm.agency_cabinet.grants.common.structs import GetSuggestedUsersResponse, User
from crm.agency_cabinet.grants.server.src.handler import Handler


async def test_get_suggested_user(handler: Handler, fixture_users, fixture_user_roles, fixture_partners_map,
                                  fixture_user_permissions, fixture_partners):
    request_pb = request_pb2.RpcRequest(
        get_suggested_users=grants_pb2.GetSuggestedUsers(
            yandex_uid=fixture_users[2].yandex_uid,  # Главный представитель 1
            partner_id=fixture_partners[0].id
        )
    )
    data = await handler(request_pb.SerializeToString())
    message = grants_pb2.GetSuggestedUsersOutput.FromString(data)

    res = GetSuggestedUsersResponse.from_proto(message.result)

    assert res == GetSuggestedUsersResponse(
        users=[
            User(
                user_id=fixture_users[3].id,
                name='Потенциальный пользователь агентства 1',
                email='suggested@agency1',
                login='suggested',
                avatar_id=None,
                current=False
            )
        ]
    )
