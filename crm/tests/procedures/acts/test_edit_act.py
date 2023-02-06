import pytest
import typing
from decimal import Decimal

from crm.agency_cabinet.ord.server.src import procedures
from crm.agency_cabinet.ord.server.src.db import models
from crm.agency_cabinet.ord.common import structs
from crm.agency_cabinet.ord.common.exceptions import NoSuchActException, UnsuitableReportException


@pytest.fixture
def procedure():
    return procedures.EditAct()


async def test_edit_act(
    procedure,
    fixture_reports2: typing.List[models.Report],
    fixture_acts_procedures: typing.List[models.Act],
):
    act_id = fixture_acts_procedures[0].id
    new_amount = Decimal('100.0')
    new_is_vat = False
    new_act_eid = 'act_eid_update'

    await procedure(structs.EditActInput(
        act_id=act_id,
        agency_id=1,
        report_id=fixture_reports2[0].id,
        act_eid=new_act_eid,
        amount=new_amount,
        is_vat=new_is_vat,
    ))

    updated_act = structs.Act(
        act_id=act_id,
        act_eid=new_act_eid,
        amount=new_amount,
        is_vat=new_is_vat,
    )

    requested_act = await models.Act.query.where(models.Act.id == act_id).gino.first()

    assert updated_act == structs.Act(
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


async def test_edit_act_incorrect_act(
    procedure,
    fixture_reports2: typing.List[models.Report],
    fixture_acts_procedures: typing.List[models.Act],
):
    with pytest.raises(NoSuchActException):
        await procedure(structs.EditActInput(
            act_id=1234,
            agency_id=1,
            report_id=fixture_reports2[0].id,
            act_eid='act_eid_update',
            amount=Decimal('100.0'),
            is_vat=False,
        ))

    await models.ClientRow.delete.gino.status()
    await models.Campaign.delete.gino.status()
    await models.Client.delete.gino.status()
    await models.Act.delete.gino.status()
    await models.Report.delete.gino.status()


async def test_edit_act_incorrect_report(
    procedure,
    fixture_acts_procedures: typing.List[models.Act],
):
    with pytest.raises(UnsuitableReportException):
        await procedure(structs.EditActInput(
            act_id=fixture_acts_procedures[0].id,
            agency_id=1,
            report_id=6543,
            act_eid='act_eid_update',
            amount=Decimal('100.0'),
            is_vat=False,
        ))

    await models.ClientRow.delete.gino.status()
    await models.Campaign.delete.gino.status()
    await models.Client.delete.gino.status()
    await models.Act.delete.gino.status()
    await models.Report.delete.gino.status()
