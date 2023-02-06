import pytest
from smb.common.testing_utils import Any

from crm.agency_cabinet.ord.server.src.db import models
from crm.agency_cabinet.ord.common import structs
from crm.agency_cabinet.ord.server.src import procedures
from crm.agency_cabinet.ord.server.tests.procedures.conftest import AGENCY_ID
from crm.agency_cabinet.ord.common.exceptions import ForbiddenByReportSettingsException


@pytest.fixture
def procedure():
    return procedures.CreateClient()


async def test_create_client_all_info(procedure, fixture_reports2, fixture_campaign2,
                                      fixture_client_rows2, fixture_clients2):
    result = await procedure(structs.CreateClientInput(
        agency_id=AGENCY_ID,
        report_id=fixture_reports2[3].id,
        client_id='new client',
        login='new login',
    ))

    assert result == structs.ClientInfo(
        id=Any(int),
        client_id='new client',
        login='new login',
        suggested_amount=None,
        campaigns_count=0,
        has_valid_ad_distributor=False,
        has_valid_ad_distributor_partner=False,
        has_valid_partner_client=False,
        has_valid_advertiser_contractor=False,
        has_valid_advertiser=False,
    )

    await models.ClientRow.delete.gino.status()
    await models.Campaign.delete.gino.status()
    await models.Client.delete.gino.status()
    await models.Act.delete.gino.status()
    await models.Report.delete.gino.status()


async def test_create_client_in_yandex_report(procedure, fixture_reports2):
    with pytest.raises(ForbiddenByReportSettingsException):
        await procedure(structs.CreateClientInput(
            agency_id=AGENCY_ID,
            report_id=fixture_reports2[0].id,
            client_id='new client',
        ))
