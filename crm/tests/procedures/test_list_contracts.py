import typing

import pytest
from smb.common.testing_utils import dt
from sqlalchemy.engine.result import RowProxy

from crm.agency_cabinet.common.consts import PaymentType, Services
from crm.agency_cabinet.documents.common.structs import (
    ContractStatus,
    Contract as ContractStruct,
    ListContractsInput
)
from crm.agency_cabinet.documents.server.src.procedures import ListContracts


@pytest.fixture
def procedure():
    return ListContracts()


async def test_select_contracts(procedure, fixture_contracts: typing.List[RowProxy]):
    got = await procedure(ListContractsInput(agency_id=123))
    assert got == [
        ContractStruct(
            contract_id=fixture_contracts[1].id,
            eid="test10",
            status=ContractStatus.not_signed,
            payment_type=PaymentType.prepayment,
            services=[Services.zen],
            signing_date=dt('2021-5-1 00:00:00'),
            finish_date=dt('2022-5-1 00:00:00'),
        ),
        ContractStruct(
            contract_id=fixture_contracts[0].id,
            eid="test9",
            status=ContractStatus.valid,
            payment_type=PaymentType.prepayment,
            services=[Services.direct],
            signing_date=dt('2021-3-1 00:00:00'),
            finish_date=dt('2022-3-1 00:00:00'),
        ),
        ContractStruct(
            contract_id=fixture_contracts[3].id,
            eid="test12",
            status=ContractStatus.valid,
            payment_type=None,
            services=[],
            signing_date=dt('2021-2-1 00:00:00'),
            finish_date=dt('2022-2-1 00:00:00'),
        )
    ]


async def test_select_no_contracts(procedure, fixture_contracts: typing.List[RowProxy]):
    got = await procedure(ListContractsInput(agency_id=1234))
    assert got == []
