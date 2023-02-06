import typing

from crm.agency_cabinet.rewards.proto import calculator_pb2, common_pb2, request_pb2
from crm.agency_cabinet.rewards.server.src.db import models
from crm.agency_cabinet.rewards.server.src.handler import Handler


async def test_get_calculator_direct_premium_data(fixture_contracts: typing.List[models.Contract],
                                                  fixture_calculator_data: models.CalculatorData):
    contract = fixture_contracts[2]
    request_pb = request_pb2.RpcRequest(
        get_calculator_data=calculator_pb2.GetCalculatorData(
            contract_id=contract.id,
            agency_id=contract.agency_id,
            service=common_pb2.CalculatorServicesType.calc_direct,
            version='2021'
        )
    )

    output = await Handler()(request_pb.SerializeToString())

    result = calculator_pb2.GetCalculatorDataOutput.FromString(output).result

    assert result == '"stub"'


async def test_get_calculator_direct_premium_data_not_found(fixture_contracts: typing.List[models.Contract]):
    request_pb = request_pb2.RpcRequest(
        get_calculator_data=calculator_pb2.GetCalculatorData(
            contract_id=fixture_contracts[0].id,
            agency_id=123,
            service=common_pb2.CalculatorServicesType.calc_direct,
            version='2021'
        )
    )

    output = await Handler()(request_pb.SerializeToString())

    assert calculator_pb2.GetCalculatorDataOutput.FromString(output) == \
           calculator_pb2.GetCalculatorDataOutput(not_found=common_pb2.Empty())
