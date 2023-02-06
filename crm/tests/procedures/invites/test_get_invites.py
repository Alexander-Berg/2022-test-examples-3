import pytest
import typing
from datetime import datetime

from crm.agency_cabinet.ord.server.src import procedures
from crm.agency_cabinet.ord.server.src.db import models
from crm.agency_cabinet.ord.common import structs


@pytest.fixture
def procedure():
    return procedures.GetInvites()


async def test_ord_get_invites(
    procedure,
    fixture_invites_procedures: typing.List[models.Invitation],
):
    got = await procedure(structs.GetInvitesInput(
        offset=0,
        limit=2,
        agency_id=1
    ))

    today = datetime.today()

    for invite in got.invites:
        invite.created_at = today

    assert got == structs.InviteList(
        size=4,
        invites=[
            structs.Invite(
                invite_id=fixture_invites_procedures[0].id,
                email='invite1@yandex.ru',
                status='sent',
                created_at=today
            ),
            structs.Invite(
                invite_id=fixture_invites_procedures[1].id,
                email='invite2@yandex.ru',
                status='accepted',
                created_at=today
            ),
        ]
    )
