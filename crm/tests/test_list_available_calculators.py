import pytest
from crm.agency_cabinet.rewards.common import structs
from crm.agency_cabinet.rewards.proto import request_pb2, calculator_pb2, common_pb2

pytestmark = [pytest.mark.asyncio]


async def test_sends_correct_request(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = calculator_pb2.ListAvailableCalculatorsOutput(
        result=calculator_pb2.CalculatorDescriptionList(
            calculators_descriptions=[
                calculator_pb2.CalculatorDescription(
                    contract_id=1,
                    service=common_pb2.Services.media,
                    version='2021',
                    contract_type=common_pb2.ContractType.base
                ),
                calculator_pb2.CalculatorDescription(
                    contract_id=2,
                    service=common_pb2.Services.media,
                    version='2022',
                    contract_type=common_pb2.ContractType.prof
                )
            ]
        )
    )

    await client.list_available_calculators(agency_id=1)

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name='rewards',
        message=request_pb2.RpcRequest(
            list_available_calculators=calculator_pb2.ListAvailableCalculatorsInput(
                agency_id=1,
            )
        ),
        response_message_type=calculator_pb2.ListAvailableCalculatorsOutput,
    )


async def test_returns_data(client, rmq_rpc_client):
    rmq_rpc_client.send_proto_message.return_value = calculator_pb2.ListAvailableCalculatorsOutput(
        result=calculator_pb2.CalculatorDescriptionList(
            calculators_descriptions=[
                calculator_pb2.CalculatorDescription(
                    contract_id=1,
                    service=common_pb2.Services.media,
                    version='2021',
                    contract_type=common_pb2.ContractType.base
                ),
                calculator_pb2.CalculatorDescription(
                    contract_id=2,
                    service=common_pb2.Services.media,
                    version='2022',
                    contract_type=common_pb2.ContractType.prof
                )
            ]
        )
    )

    got = await client.list_available_calculators(agency_id=1)
    assert got == structs.ListAvailableCalculatorsResponse(calculators_descriptions=[
        structs.CalculatorDescription(
            contract_id=1,
            service='media',
            version='2021',
            contract_type='base'
        ),
        structs.CalculatorDescription(
            contract_id=2,
            service='media',
            version='2022',
            contract_type='prof'
        ),
    ])
