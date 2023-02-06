from unittest.mock import AsyncMock

import pytest

from crm.agency_cabinet.rewards.common import structs
from crm.agency_cabinet.rewards.proto import request_pb2, calculator_pb2

pytestmark = [pytest.mark.asyncio]


@pytest.fixture(autouse=True)
def procedure(mocker):
    mock = AsyncMock()
    mock.return_value = structs.ListAvailableCalculatorsResponse(calculators_descriptions=[
        structs.CalculatorDescription(
            contract_id=1,
            service='media',
            version='2021',
            contract_type='base'
        ),
        structs.CalculatorDescription(
            contract_id=1,
            service='media',
            version='2022',
            contract_type='base'
        ),
    ]
    )

    mocker.patch(
        "crm.agency_cabinet.rewards.server.src.procedures.ListAvailableCalculators",
        return_value=mock,
    )

    return mock


async def test_calls_procedure(handler, procedure):
    input_pb = request_pb2.RpcRequest(
        list_available_calculators=calculator_pb2.ListAvailableCalculatorsInput(
            agency_id=1
        )
    )

    await handler(input_pb.SerializeToString())

    procedure.assert_awaited_with(
        request=structs.ListAvailableCalculatorsInput(
            agency_id=1,
        )
    )


async def test_returns_serialized_operation_result(handler, procedure):
    input_pb = request_pb2.RpcRequest(
        list_available_calculators=calculator_pb2.ListAvailableCalculatorsInput(
            agency_id=1
        )
    )

    result = await handler(input_pb.SerializeToString())

    assert calculator_pb2.ListAvailableCalculatorsOutput.FromString(result) == calculator_pb2.ListAvailableCalculatorsOutput(
        result=calculator_pb2.CalculatorDescriptionList(
            calculators_descriptions=[
                structs.CalculatorDescription(
                    contract_id=1,
                    service='media',
                    version='2021',
                    contract_type='base').to_proto(),
                structs.CalculatorDescription(
                    contract_id=1,
                    service='media',
                    version='2022',
                    contract_type='base').to_proto(),
            ]))
