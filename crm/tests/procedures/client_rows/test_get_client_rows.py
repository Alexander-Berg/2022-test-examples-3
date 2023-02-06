import pytest

from decimal import Decimal
from smb.common.testing_utils import dt

from crm.agency_cabinet.common.testing import any_int
from crm.agency_cabinet.ord.common import consts, structs
from crm.agency_cabinet.ord.server.src import procedures


@pytest.fixture
def procedure():
    return procedures.GetClientRows()


async def test_get_client_rows(procedure, fixture_cr_reports, fixture_cr_clients, fixture_cr_client_rows):
    result = await procedure(structs.GetClientRowsInput(
        agency_id=1,
        report_id=fixture_cr_reports[0].id,
        client_id=fixture_cr_clients[0].id,
    ))

    assert len(result.rows) == 3

    assert result.rows[0] == structs.ClientRow(
        id=fixture_cr_client_rows[0].id,
        suggested_amount=Decimal(10000),
        campaign=structs.Campaign(
            id=any_int(),
            campaign_eid='campaign-1',
            name='campaign_name_1',
        ),
        ad_distributor_organization=structs.Organization(
            id=any_int(),
            type=consts.OrganizationType.ul,
            inn='inn-123456',
            name='OOO Vesna',
            is_rr=False,
            is_ors=False,
            mobile_phone='987654321',
            epay_number='234',
            reg_number='345',
            alter_inn='456',
            oksm_number='567',
            rs_url='www.test.ru',
        ),
        ad_distributor_partner_organization=structs.Organization(
            id=any_int(),
            type=consts.OrganizationType.fl,
            inn='inn-234567',
        ),
        partner_client_organization=structs.Organization(
            id=any_int(),
            type=consts.OrganizationType.ffl,
            mobile_phone='0987654321'
        ),
        advertiser_contractor_organization=structs.Organization(
            id=any_int(),
            type=consts.OrganizationType.fl,
            inn='inn-234567',
        ),
        advertiser_organization=structs.Organization(
            id=any_int(),
            type=consts.OrganizationType.ffl,
            mobile_phone='0987654321'
        ),
        ad_distributor_contract=structs.Contract(
            id=any_int(),
            contract_eid='c-1',
            is_reg_report=False,
            type=consts.ContractType.contract,
            action_type=consts.ContractActionType.other,
            subject_type=consts.ContractSubjectType.distribution,
            date=dt('2022-3-1 00:00:00'),
            amount=Decimal(100000),
            is_vat=True
        ),
        ad_distributor_partner_contract=structs.Contract(
            id=any_int(),
            contract_eid='c-2',
        ),
        advertiser_contract=structs.Contract(
            id=any_int(),
            contract_eid='c-2',
        ),
        ad_distributor_act=structs.Act(
            act_id=any_int(),
            act_eid='a-1',
            amount=Decimal(20000),
            is_vat=True
        ),
        ad_distributor_partner_act=structs.Act(
            act_id=any_int(),
            act_eid='a-2',
        ),
    )

    assert result.rows[1] == structs.ClientRow(
        id=fixture_cr_client_rows[1].id,
        suggested_amount=Decimal(10000),
        campaign=structs.Campaign(
            id=any_int(),
            campaign_eid='campaign-2',
            name='campaign_name_2',
        ),
        advertiser_contractor_organization=structs.Organization(
            id=any_int(),
            type=consts.OrganizationType.ul,
            inn='inn-123456',
            name='OOO Vesna',
            is_rr=False,
            is_ors=False,
            mobile_phone='987654321',
            epay_number='234',
            reg_number='345',
            alter_inn='456',
            oksm_number='567',
            rs_url='www.test.ru',
        ),
        advertiser_organization=structs.Organization(
            id=any_int(),
            type=consts.OrganizationType.ffl,
            mobile_phone='0987654321'
        ),
        ad_distributor_contract=structs.Contract(
            id=any_int(),
            contract_eid='c-4',
        ),
        advertiser_contract=structs.Contract(
            id=any_int(),
            contract_eid='c-3',
        ),
    )

    assert result.rows[2] == structs.ClientRow(
        id=fixture_cr_client_rows[2].id,
        campaign=structs.Campaign(
            id=any_int(),
            campaign_eid='campaign-3',
            name='campaign_name_3',
        ),
    )


async def test_get_client_rows_limit_offset(procedure, fixture_cr_reports, fixture_cr_clients, fixture_cr_client_rows):
    result = await procedure(structs.GetClientRowsInput(
        agency_id=1,
        report_id=fixture_cr_reports[0].id,
        client_id=fixture_cr_clients[0].id,
        limit=1,
        offset=1,
    ))

    assert result.size == 3
    assert len(result.rows) == 1
    assert result.rows[0].id == fixture_cr_client_rows[1].id


@pytest.mark.parametrize(
    ('search_query', 'row_indexes'),
    [
        ('inn-1234', (0, 1)),
        ('inn-2345', (0,)),
        ('campaign-3', (2,)),
    ]
)
async def test_get_client_rows_search_query(procedure, fixture_cr_reports, fixture_cr_clients, fixture_cr_client_rows,
                                            search_query, row_indexes):
    result = await procedure(structs.GetClientRowsInput(
        agency_id=1,
        report_id=fixture_cr_reports[0].id,
        client_id=fixture_cr_clients[0].id,
        search_query=search_query
    ))

    expected_ids = {fixture_cr_client_rows[index].id for index in row_indexes}
    actual_ids = {r.id for r in result.rows}
    assert actual_ids == expected_ids
