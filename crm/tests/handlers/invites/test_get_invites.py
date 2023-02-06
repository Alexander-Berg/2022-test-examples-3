from datetime import datetime
from unittest.mock import AsyncMock

import pytest

from crm.agency_cabinet.ord.proto import invites_pb2, request_pb2
from crm.agency_cabinet.ord.common import structs


@pytest.fixture(autouse=True)
def procedure(mocker):
    mock = AsyncMock()
    mock.return_value = structs.InviteList(
        size=2,
        invites=[
            structs.Invite(
                invite_id=1,
                email='user1@yandex.ru',
                status='sent',
                created_at=datetime(2022, 7, 20)
            ),
            structs.Invite(
                invite_id=2,
                email='user4@yandex.ru',
                status='accepted',
                created_at=datetime(2022, 6, 12)
            ),
        ]
    )

    mocker.patch(
        "crm.agency_cabinet.ord.server.src.procedures.GetInvites",
        return_value=mock,
    )

    return mock


async def test_ord_get_invites(handler):
    request_pb = request_pb2.RpcRequest(
        get_invites=invites_pb2.GetInvitesInput(
            agency_id=1,
            limit=2,
            offset=0,
        )
    )

    data = await handler(request_pb.SerializeToString())

    message = invites_pb2.GetInvitesOutput.FromString(data)

    res = structs.InviteList.from_proto(message.result)

    assert res == structs.InviteList(
        size=2,
        invites=[
            structs.Invite(
                invite_id=1,
                email='user1@yandex.ru',
                status='sent',
                created_at=datetime(2022, 7, 20)
            ),
            structs.Invite(
                invite_id=2,
                email='user4@yandex.ru',
                status='accepted',
                created_at=datetime(2022, 6, 12)
            ),
        ]
    )


async def test_calls_procedure_get_invites(handler, procedure):
    input_pb = request_pb2.RpcRequest(
        get_invites=invites_pb2.GetInvitesInput(
            limit=5,
            offset=0,
            agency_id=1
        )
    )

    await handler(input_pb.SerializeToString())

    procedure.assert_awaited_with(request=structs.GetInvitesInput(
        offset=0,
        limit=5,
        agency_id=1
    ))
