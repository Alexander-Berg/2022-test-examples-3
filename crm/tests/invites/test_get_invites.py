import pytest

from datetime import datetime

from crm.agency_cabinet.common.proto_utils import timestamp_or_none
from crm.agency_cabinet.ord.common.structs import InviteList, Invite
from crm.agency_cabinet.ord.proto import request_pb2, invites_pb2

pytestmark = [pytest.mark.asyncio]


async def test_ord_get_invites(client, rmq_rpc_client):

    date = datetime(2022, 7, 20)

    rmq_rpc_client.send_proto_message.return_value = invites_pb2.GetInvitesOutput(
        result=invites_pb2.InviteList(
            invites=[
                invites_pb2.Invite(
                    invite_id=1,
                    email='user1@yandex.ru',
                    status='sent',
                    created_at=timestamp_or_none(date),
                ),
                invites_pb2.Invite(
                    invite_id=2,
                    email='user4@yandex.ru',
                    status='accepted',
                    created_at=timestamp_or_none(date),
                )
            ],
            size=2
        ))

    got = await client.get_invites(
        agency_id=1,
        limit=2,
    )

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name='ord',
        message=request_pb2.RpcRequest(
            get_invites=invites_pb2.GetInvitesInput(
                agency_id=1,
                limit=2,
            )
        ),
        response_message_type=invites_pb2.GetInvitesOutput,
    )

    assert got == InviteList(
        size=2,
        invites=[
            Invite(
                invite_id=1,
                email='user1@yandex.ru',
                status='sent',
                created_at=date
            ),
            Invite(
                invite_id=2,
                email='user4@yandex.ru',
                status='accepted',
                created_at=date
            ),
        ]
    )
