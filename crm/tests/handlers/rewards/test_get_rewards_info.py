import typing

from datetime import datetime, timedelta
from google.protobuf.timestamp_pb2 import Timestamp
from google.protobuf.wrappers_pb2 import BoolValue

from crm.agency_cabinet.common.proto_utils import decimal_to_string
from crm.agency_cabinet.rewards.proto import request_pb2, rewards_info_pb2
from crm.agency_cabinet.rewards.server.src.db import models
from crm.agency_cabinet.rewards.server.src.handler import Handler


async def test_get_rewards_info_filtered(fixture_rewards: typing.List[models.Reward],
                                         fixture_contracts: typing.List[models.Contract],
                                         fixture_service_rewards: typing.List[models.ServiceReward]):
    requested_rewards = [fixture_rewards[0]]
    contract = fixture_contracts[0]
    left_ts = Timestamp()
    left_ts.FromDatetime(datetime.now() - timedelta(days=32))
    request_pb = request_pb2.RpcRequest(
        get_rewards_info=rewards_info_pb2.GetRewardsInfo(
            agency_id=contract.agency_id,
            filter_from=left_ts
        )
    )

    period_from = Timestamp()
    period_from.FromDatetime(requested_rewards[0].period_from)
    expected_rewards_output = [rewards_info_pb2.RewardInfo(
        id=requested_rewards[0].id,
        contract_id=contract.id,
        type=requested_rewards[0].type,
        services=[fixture_service_rewards[0].service, fixture_service_rewards[1].service],
        is_accrued=requested_rewards[0].is_accrued,
        is_paid=requested_rewards[0].is_paid,
        payment=decimal_to_string(requested_rewards[0].payment),
        period_from=period_from
    )]
    output = await Handler()(request_pb.SerializeToString())

    rewards = rewards_info_pb2.GetRewardsInfoOutput.FromString(output).result.rewards

    assert len(rewards) == len(requested_rewards)
    for expected, result in zip(expected_rewards_output, rewards):
        assert expected == result


async def test_get_rewards_info_filtered_by_eid_and_paid(fixture_rewards: typing.List[models.Reward],
                                                         fixture_contracts: typing.List[models.Contract],
                                                         fixture_service_rewards: typing.List[models.ServiceReward]):
    requested_rewards = [fixture_rewards[0]]
    contract = fixture_contracts[0]
    request_pb = request_pb2.RpcRequest(
        get_rewards_info=rewards_info_pb2.GetRewardsInfo(
            agency_id=contract.agency_id,
            filter_contract=1111,
            filter_is_paid=BoolValue(value=False)
        )
    )
    period_from = Timestamp()
    period_from.FromDatetime(requested_rewards[0].period_from)

    expected_rewards_output = [rewards_info_pb2.RewardInfo(
        id=requested_rewards[0].id,
        contract_id=contract.id,
        type=requested_rewards[0].type,
        services=[fixture_service_rewards[0].service, fixture_service_rewards[1].service],
        is_accrued=requested_rewards[0].is_accrued,
        is_paid=requested_rewards[0].is_paid,
        payment=decimal_to_string(requested_rewards[0].payment),
        period_from=period_from
    )]
    output = await Handler()(request_pb.SerializeToString())

    rewards = rewards_info_pb2.GetRewardsInfoOutput.FromString(output).result.rewards

    assert len(rewards) == len(requested_rewards)
    for expected, result in zip(expected_rewards_output, rewards):
        assert expected == result


async def test_get_rewards_info_filtered_by_type(fixture_rewards: typing.List[models.Reward],
                                                 fixture_contracts: typing.List[models.Contract],
                                                 fixture_service_rewards: typing.List[models.ServiceReward]):
    contract = fixture_contracts[0]
    request_pb = request_pb2.RpcRequest(
        get_rewards_info=rewards_info_pb2.GetRewardsInfo(
            agency_id=contract.agency_id,
            filter_type='quarter'
        )
    )

    output = await Handler()(request_pb.SerializeToString())

    rewards = rewards_info_pb2.GetRewardsInfoOutput.FromString(output).result.rewards

    assert len(rewards) == 0


async def test_get_rewards_info(fixture_rewards: typing.List[models.Reward],
                                fixture_contracts: typing.List[models.Contract],
                                fixture_service_rewards: typing.List[models.ServiceReward]):
    requested_rewards = [fixture_rewards[0], fixture_rewards[1]]
    contract = fixture_contracts[0]
    request_pb = request_pb2.RpcRequest(
        get_rewards_info=rewards_info_pb2.GetRewardsInfo(
            agency_id=contract.agency_id,
        )
    )

    period_from = Timestamp()
    period_from.FromDatetime(requested_rewards[0].period_from)

    period_from2 = Timestamp()
    period_from2.FromDatetime(requested_rewards[1].period_from)

    payment_date = Timestamp()
    payment_date.FromDatetime(requested_rewards[1].payment_date)

    expected_rewards_output = [
        rewards_info_pb2.RewardInfo(
            id=requested_rewards[0].id,
            contract_id=contract.id,
            type=requested_rewards[0].type,
            services=[fixture_service_rewards[0].service, fixture_service_rewards[1].service],
            is_accrued=requested_rewards[0].is_accrued,
            is_paid=requested_rewards[0].is_paid,
            payment=decimal_to_string(requested_rewards[0].payment),
            period_from=period_from),

        rewards_info_pb2.RewardInfo(
            id=requested_rewards[1].id,
            contract_id=contract.id,
            type=requested_rewards[1].type,
            services=[fixture_service_rewards[2].service],
            is_accrued=requested_rewards[1].is_accrued,
            is_paid=requested_rewards[1].is_paid,
            payment=decimal_to_string(requested_rewards[1].payment),
            period_from=period_from2,
            payment_date=payment_date)
    ]
    output = await Handler()(request_pb.SerializeToString())

    rewards = rewards_info_pb2.GetRewardsInfoOutput.FromString(output).result.rewards

    assert len(rewards) == len(requested_rewards)
    for expected, result in zip(expected_rewards_output, rewards):
        assert expected == result
