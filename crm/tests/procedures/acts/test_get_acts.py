import pytest
import typing
from decimal import Decimal

from crm.agency_cabinet.ord.server.src import procedures
from crm.agency_cabinet.ord.server.src.db import models
from crm.agency_cabinet.ord.common import structs
from crm.agency_cabinet.ord.common.exceptions import UnsuitableReportException


@pytest.fixture
def procedure():
    return procedures.GetActs()


async def test_ord_get_acts(
    procedure,
    fixture_acts_procedures: typing.List[models.Act],
    fixture_reports2: typing.List[models.Report],
):
    got = await procedure(structs.GetActsInput(
        offset=0,
        limit=2,
        report_id=fixture_reports2[0].id,
        agency_id=1
    ))

    assert got == structs.ActList(
        size=4,
        acts=[
            structs.Act(
                act_id=fixture_acts_procedures[3].id,
                act_eid='act4',
                amount=Decimal('3'),
                is_vat=False,
            ),
            structs.Act(
                act_id=fixture_acts_procedures[2].id,
                act_eid='act3',
                amount=None,
                is_vat=None,
            ),
        ]
    )


async def test_ord_get_acts_bad_report(
    procedure,
):
    with pytest.raises(UnsuitableReportException):
        await procedure(structs.GetActsInput(
            offset=0,
            limit=5,
            report_id=1020,
            agency_id=1
        ))
