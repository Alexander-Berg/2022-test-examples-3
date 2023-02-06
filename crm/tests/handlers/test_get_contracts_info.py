import typing

from google.protobuf.timestamp_pb2 import Timestamp

from crm.agency_cabinet.rewards.proto import contracts_info_pb2, request_pb2, common_pb2
from crm.agency_cabinet.rewards.server.src.db import models
from crm.agency_cabinet.rewards.server.src.handler import Handler


async def test_get_contracts_info(fixture_contracts: typing.List[models.Contract]):
    fixture_contract = fixture_contracts[2]
    request_pb = request_pb2.RpcRequest(
        get_contracts_info=contracts_info_pb2.GetContractsInfo(
            agency_id=fixture_contract.agency_id
        )
    )

    finish_date = Timestamp()
    finish_date.FromDatetime(fixture_contract.finish_date)

    expected_output = [
        contracts_info_pb2.ContractInfo(
            contract_id=fixture_contract.id,
            eid=fixture_contract.eid,
            inn=fixture_contract.inn,
            services=sorted(fixture_contract.services),
            finish_date=finish_date,
            payment_type=fixture_contract.payment_type,
            type=common_pb2.ContractType.prof,
            is_crisis=fixture_contract.is_crisis,
        )
    ]

    output = await Handler()(request_pb.SerializeToString())

    contracts = contracts_info_pb2.GetContractsInfoOutput.FromString(output).result.contracts

    assert len(contracts) == 1

    for expected, result in zip(expected_output, contracts):
        assert expected == result


async def test_get_contracts_info_empty_services(fixture_contracts: typing.List[models.Contract]):
    fixture_contract = fixture_contracts[6]
    request_pb = request_pb2.RpcRequest(
        get_contracts_info=contracts_info_pb2.GetContractsInfo(
            agency_id=fixture_contract.agency_id
        )
    )

    finish_date = Timestamp()
    finish_date.FromDatetime(fixture_contract.finish_date)

    expected_output = [
        contracts_info_pb2.ContractInfo(
            contract_id=fixture_contract.id,
            eid=fixture_contract.eid,
            inn=fixture_contract.inn,
            services=sorted(fixture_contract.services),
            finish_date=finish_date,
            payment_type=fixture_contract.payment_type,
            type=common_pb2.ContractType.prof,
            is_crisis=fixture_contract.is_crisis,
        )
    ]

    output = await Handler()(request_pb.SerializeToString())

    contracts = contracts_info_pb2.GetContractsInfoOutput.FromString(output).result.contracts

    assert len(contracts) == 1

    for expected, result in zip(expected_output, contracts):
        assert expected == result


async def test_get_contracts_info_empty_result(fixture_contracts: typing.List[models.Contract]):
    fixture_contract = fixture_contracts[7]
    request_pb = request_pb2.RpcRequest(
        get_contracts_info=contracts_info_pb2.GetContractsInfo(
            agency_id=fixture_contract.agency_id
        )
    )

    output = await Handler()(request_pb.SerializeToString())
    contracts = contracts_info_pb2.GetContractsInfoOutput.FromString(output).result.contracts
    assert len(contracts) == 0
