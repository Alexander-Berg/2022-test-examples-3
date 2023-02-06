import pytest
import typing

from crm.agency_cabinet.ord.server.src import procedures
from crm.agency_cabinet.ord.server.src.db import models
from crm.agency_cabinet.ord.common import structs


@pytest.fixture
def procedure():
    return procedures.GetContracts()


async def test_ord_get_contracts(
    procedure,
    fixture_contract_procedure: typing.List[models.Contract],
    fixture_reports2: typing.List[models.Report]
):
    got = await procedure(structs.GetContractsInput(
        offset=0,
        limit=5,
        agency_id=fixture_reports2[0].agency_id
    ))

    assert got == structs.ContractsList(
        size=2,
        contracts=[
            structs.Contract(
                id=fixture_contract_procedure[1].id,
                contract_eid=None,
                is_reg_report=None,
                type=None,
                action_type=None,
                subject_type=None,
                date=None,
                amount=None,
                is_vat=None,
                client_organization=structs.Organization(
                    id=fixture_contract_procedure[0].client_id,
                    type='ip',
                    name=None,
                    inn='some inn',
                    is_rr=None,
                    is_ors=None,
                    mobile_phone=None,
                    epay_number=None,
                    reg_number=None,
                    alter_inn=None,
                    oksm_number=None,
                    rs_url=None
                ),
                contractor_organization=structs.Organization(
                    id=fixture_contract_procedure[0].contractor_id,
                    type='ip',
                    name=None,
                    inn='contractor inn',
                    is_rr=None,
                    is_ors=None,
                    mobile_phone=None,
                    epay_number=None,
                    reg_number=None,
                    alter_inn=None,
                    oksm_number=None,
                    rs_url=None
                )
            ),
            structs.Contract(
                id=fixture_contract_procedure[0].id,
                contract_eid='aaaaaa',
                is_reg_report=None,
                type=None,
                action_type=None,
                subject_type=None,
                date=None,
                amount=None,
                is_vat=None,
                client_organization=structs.Organization(
                    id=fixture_contract_procedure[0].client_id,
                    type='ip',
                    name=None,
                    inn='some inn',
                    is_rr=None,
                    is_ors=None,
                    mobile_phone=None,
                    epay_number=None,
                    reg_number=None,
                    alter_inn=None,
                    oksm_number=None,
                    rs_url=None
                ),
                contractor_organization=structs.Organization(
                    id=fixture_contract_procedure[0].contractor_id,
                    type='ip',
                    name=None,
                    inn='contractor inn',
                    is_rr=None,
                    is_ors=None,
                    mobile_phone=None,
                    epay_number=None,
                    reg_number=None,
                    alter_inn=None,
                    oksm_number=None,
                    rs_url=None
                )
            ),
        ]
    )
