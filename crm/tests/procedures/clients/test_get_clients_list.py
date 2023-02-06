import pytest
from decimal import Decimal

from crm.agency_cabinet.ord.server.src.db import models
from crm.agency_cabinet.ord.common import structs
from crm.agency_cabinet.ord.server.src import procedures
from crm.agency_cabinet.ord.server.tests.procedures.conftest import AGENCY_ID


@pytest.fixture
def procedure():
    return procedures.GetReportClientsInfo()


async def test_get_clients_info(
    procedure, fixture_reports2, fixture_campaign2, fixture_client_rows2, fixture_clients2,
    fixture_organizations, fixture_contract_procedure, fixture_act_procedure
):
    result = await procedure(structs.GetReportClientsInfoInput(
        agency_id=AGENCY_ID,
        report_id=fixture_reports2[0].id,
        is_valid=True
    ))

    assert result == structs.ClientsInfoList(
        clients=[
            structs.ClientInfo(
                id=fixture_clients2[0].id,
                client_id=fixture_clients2[0].client_id,
                name=fixture_clients2[0].name,
                login=fixture_clients2[0].login,
                suggested_amount=Decimal('10000.0'),
                has_valid_advertiser=True,
                has_valid_partner_client=True,
                campaigns_count=1,
                has_valid_ad_distributor=True,
                has_valid_ad_distributor_partner=True,
                has_valid_advertiser_contractor=True,
            )
        ],
        size=1
    )
    await models.ClientRow.delete.gino.status()
    await models.Campaign.delete.gino.status()
    await models.Client.delete.gino.status()
    await models.Act.delete.gino.status()
    await models.Contract.delete.gino.status()
    await models.Report.delete.gino.status()


async def test_get_clients_info_has_errors(
    procedure, fixture_reports2, fixture_client_rows_has_errors, fixture_clients2,
    fixture_organizations, fixture_contract_procedure, fixture_act_procedure
):
    result = await procedure(structs.GetReportClientsInfoInput(
        agency_id=AGENCY_ID,
        report_id=fixture_reports2[0].id,
        is_valid=False
    ))

    assert result == structs.ClientsInfoList(
        clients=[
            structs.ClientInfo(
                id=fixture_clients2[0].id,
                client_id=fixture_clients2[0].client_id,
                campaigns_count=1,
                login=fixture_clients2[0].login,
                name=fixture_clients2[0].name,
                suggested_amount=Decimal('1000.000000'),
                has_valid_ad_distributor=True,
                has_valid_ad_distributor_partner=True,
                has_valid_partner_client=True,
                has_valid_advertiser=False,
                has_valid_advertiser_contractor=True
            ),
            structs.ClientInfo(
                id=fixture_clients2[1].id,
                client_id=fixture_clients2[1].client_id,
                campaigns_count=1,
                login=fixture_clients2[1].login,
                name=fixture_clients2[1].name,
                suggested_amount=Decimal('2000.000000'),
                has_valid_ad_distributor=True,
                has_valid_ad_distributor_partner=True,
                has_valid_partner_client=False,
                has_valid_advertiser=True,
                has_valid_advertiser_contractor=True
            ),
        ],
        size=2)

    result = await procedure(structs.GetReportClientsInfoInput(
        agency_id=AGENCY_ID,
        report_id=fixture_reports2[1].id,
        is_valid=True
    ))

    assert result == structs.ClientsInfoList(clients=[
        structs.ClientInfo(
            id=fixture_clients2[3].id,
            client_id=fixture_clients2[3].client_id,
            campaigns_count=1,
            login=fixture_clients2[3].login,
            name=fixture_clients2[3].name,
            suggested_amount=Decimal('4000.000000'),
            has_valid_ad_distributor=True,
            has_valid_ad_distributor_partner=True,
            has_valid_partner_client=True,
            has_valid_advertiser=True,
            has_valid_advertiser_contractor=True
        )
    ],
        size=1)

    await models.ClientRow.delete.gino.status()
    await models.Campaign.delete.gino.status()
    await models.Client.delete.gino.status()
    await models.Act.delete.gino.status()
    await models.Contract.delete.gino.status()
    await models.Report.delete.gino.status()
