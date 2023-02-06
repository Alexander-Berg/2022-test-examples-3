import pytest
import json
from unittest.mock import AsyncMock
from crm.agency_cabinet.common.consts import CalculatorServiceType
from crm.agency_cabinet.rewards.common import structs
from crm.agency_cabinet.rewards.server.src import procedures


@pytest.fixture
def procedure():
    return procedures.GetCalculatorMeta()


@pytest.fixture
def calculator_version():
    return '2022'


async def test_list_available_calculators(procedure: procedures.GetCalculatorMeta, fixture_contracts, calculator_version, prof_postpayment_meta, mocker):
    mock = AsyncMock()
    mock.return_value = {
        'data': prof_postpayment_meta
    }
    procedure.client.cat = mock

    got = await procedure(structs.GetCalculatorMetaRequest(
        agency_id=fixture_contracts[3].agency_id,
        contract_id=fixture_contracts[3].id,
        service=CalculatorServiceType.direct.value,
        version=calculator_version
    ))

    expected = structs.GetCalculatorMetaResponse(result=json.dumps(prof_postpayment_meta))
    assert got == expected
