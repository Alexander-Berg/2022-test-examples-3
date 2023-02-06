import typing

import pytest
from crm.agency_cabinet.documents.common.structs import (
    GetContractUrlInput
)
from crm.agency_cabinet.documents.server.src.procedures import GetContractUrl
from crm.agency_cabinet.documents.server.src.exceptions import FileNotFound
from sqlalchemy.engine.result import RowProxy


@pytest.fixture
def procedure():
    return GetContractUrl()


async def test_get_contract_url(
    procedure,
    fixture_contracts: typing.List[RowProxy],
):
    with pytest.raises(FileNotFound):
        await procedure(GetContractUrlInput(
            agency_id=123,
            contract_id=fixture_contracts[1].id,)

        )
