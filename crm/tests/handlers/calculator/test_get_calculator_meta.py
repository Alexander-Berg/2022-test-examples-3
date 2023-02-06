import json
import typing
import pytest
from unittest.mock import AsyncMock
from crm.agency_cabinet.rewards.proto import calculator_pb2, common_pb2, request_pb2
from crm.agency_cabinet.rewards.server.src.db import models
from crm.agency_cabinet.rewards.server.src.handler import Handler
from crm.agency_cabinet.common.bunker.client import BunkerNotFoundError, BunkerError


@pytest.fixture
def calculator_version():
    return '2022'


async def test_get_calculator_direct_meta(fixture_contracts: typing.List[models.Contract],
                                          fixture_calculator_data: models.CalculatorData,
                                          calculator_version,
                                          prof_postpayment_meta,
                                          mocker):
    contract = fixture_contracts[3]
    request_pb = request_pb2.RpcRequest(
        get_calculator_meta=calculator_pb2.GetCalculatorMeta(
            contract_id=contract.id,
            agency_id=contract.agency_id,
            service=common_pb2.CalculatorServicesType.calc_direct,
            version=calculator_version
        )
    )
    with mocker.patch('crm.agency_cabinet.common.bunker.client.BunkerClient.cat',
                      mock=AsyncMock,
                      return_value={
                          'data': prof_postpayment_meta
                      }):
        output = await Handler()(request_pb.SerializeToString())

    result = calculator_pb2.GetCalculatorDataOutput.FromString(output).result

    assert result == json.dumps(prof_postpayment_meta)


async def test_get_calculator_direct_meta_not_found(fixture_contracts: typing.List[models.Contract], calculator_version, mocker):
    contract = fixture_contracts[3]
    request_pb = request_pb2.RpcRequest(
        get_calculator_meta=calculator_pb2.GetCalculatorMeta(
            contract_id=contract.id,
            agency_id=contract.agency_id,
            service=common_pb2.CalculatorServicesType.calc_direct,
            version=calculator_version
        )
    )
    with mocker.patch('crm.agency_cabinet.common.bunker.client.BunkerClient.cat',
                      mock=AsyncMock,
                      return_value=None,
                      side_effect=[BunkerNotFoundError('Not found')]
                      ):
        output = await Handler()(request_pb.SerializeToString())

    assert calculator_pb2.GetCalculatorMetaOutput.FromString(output) == \
        calculator_pb2.GetCalculatorMetaOutput(not_found=common_pb2.Empty())


async def test_get_calculator_direct_meta_bunker_error(fixture_contracts: typing.List[models.Contract], calculator_version, mocker):
    contract = fixture_contracts[3]
    request_pb = request_pb2.RpcRequest(
        get_calculator_meta=calculator_pb2.GetCalculatorMeta(
            contract_id=contract.id,
            agency_id=contract.agency_id,
            service=common_pb2.CalculatorServicesType.calc_direct,
            version=calculator_version
        )
    )
    with mocker.patch('crm.agency_cabinet.common.bunker.client.BunkerClient.cat',
                      mock=AsyncMock,
                      return_value=None,
                      side_effect=[BunkerError('Bad request')]
                      ):
        output = await Handler()(request_pb.SerializeToString())

    assert calculator_pb2.GetCalculatorMetaOutput.FromString(output) == \
        calculator_pb2.GetCalculatorMetaOutput(bunker_error='Bad request')
