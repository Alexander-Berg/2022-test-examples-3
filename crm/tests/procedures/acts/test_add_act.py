import pytest
import typing
from decimal import Decimal

from crm.agency_cabinet.ord.server.src import procedures
from crm.agency_cabinet.ord.server.src.db import models
from crm.agency_cabinet.ord.common import structs
from crm.agency_cabinet.ord.common.exceptions import UnsuitableReportException
from crm.agency_cabinet.ord.server.tests.procedures.conftest import AGENCY_ID


@pytest.fixture
def procedure():
    return procedures.AddAct()


async def test_add_act(
    procedure,
    fixture_reports2: typing.List[models.Report],
):
    got = await procedure(structs.AddActInput(
        agency_id=AGENCY_ID,
        report_id=fixture_reports2[0].id,
        act_eid='act_eid_insert',
        amount=Decimal('10.0'),
        is_vat=False,
    ))

    act_id = got.act_id

    assert act_id

    created_act = structs.Act(
        act_id=act_id,
        act_eid='act_eid_insert',
        amount=Decimal('10.0'),
        is_vat=False,
    )

    assert got == created_act

    requested_act = await models.Act.query.where(models.Act.id == act_id).gino.first()

    assert created_act == structs.Act(
        act_id=requested_act.id,
        act_eid=requested_act.act_eid,
        amount=requested_act.amount,
        is_vat=requested_act.is_vat,
    )

    assert requested_act.is_valid is True

    await models.ClientRow.delete.gino.status()
    await models.Campaign.delete.gino.status()
    await models.Client.delete.gino.status()
    await models.Act.delete.gino.status()
    await models.Report.delete.gino.status()


async def test_add_act_unsuitable_report(
    procedure,
    fixture_reports2: typing.List[models.Report],
):
    with pytest.raises(UnsuitableReportException):
        await procedure(structs.AddActInput(
            agency_id=1,
            report_id=5050,
            act_eid='act_eid_insert',
            amount=Decimal('10.0'),
            is_vat=False,
        ))

    await models.ClientRow.delete.gino.status()
    await models.Campaign.delete.gino.status()
    await models.Client.delete.gino.status()
    await models.Act.delete.gino.status()
    await models.Report.delete.gino.status()
