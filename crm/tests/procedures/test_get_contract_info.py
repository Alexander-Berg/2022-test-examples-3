import typing
from decimal import Decimal

import pytest
from smb.common.testing_utils import dt
from sqlalchemy.engine.result import RowProxy

from crm.agency_cabinet.common.consts import PaymentType, Services
from crm.agency_cabinet.documents.common.structs import (
    ContractStatus,
    Contract,
    GetContractInfoInput,
)
from crm.agency_cabinet.documents.server.src.exceptions import NoSuchContractException, UnsuitableAgencyException
from crm.agency_cabinet.documents.server.src.procedures import GetContractInfo


@pytest.fixture
def procedure():
    return GetContractInfo()


async def test_select_contract_with_agreement(procedure,
                                              fixture_contracts: typing.List[RowProxy],
                                              fixture_agreements: typing.List[RowProxy]):
    got = await procedure(GetContractInfoInput(agency_id=321, contract_id=fixture_contracts[2].id))
    assert got == Contract(
        contract_id=fixture_contracts[2].id,
        eid='test11',
        inn='inn',
        status=ContractStatus.valid,
        payment_type=PaymentType.prepayment,
        services=[Services.zen],
        signing_date=dt('2021-3-1 00:00:00'),
        finish_date=dt('2022-3-1 00:00:00'),
        credit_limit=Decimal('66.6'),
    )


async def test_select_contract_without_agreement(procedure,
                                                 fixture_contracts: typing.List[RowProxy],
                                                 fixture_agreements: typing.List[RowProxy]):
    got = await procedure(GetContractInfoInput(agency_id=123, contract_id=fixture_contracts[1].id))
    assert got == Contract(
        contract_id=fixture_contracts[1].id,
        eid='test10',
        inn='inn',
        status=ContractStatus.not_signed,
        payment_type=PaymentType.prepayment,
        services=[Services.zen],
        signing_date=dt('2021-5-1 00:00:00'),
        finish_date=dt('2022-5-1 00:00:00'),
        credit_limit=Decimal('88.8'),
    )


async def test_select_no_such_contract(procedure, fixture_contracts: typing.List[RowProxy]):
    with pytest.raises(NoSuchContractException):
        await procedure(GetContractInfoInput(agency_id=1234, contract_id=0))


async def test_select_contract_unsuitable_agency(procedure, fixture_contracts: typing.List[RowProxy]):
    with pytest.raises(UnsuitableAgencyException):
        await procedure(GetContractInfoInput(agency_id=1234, contract_id=fixture_contracts[2].id))
