import pytest

from datetime import datetime
from decimal import Decimal

from google.protobuf.timestamp_pb2 import Timestamp
from smb.common.rmq.rpc.client import RmqRpcClient

from crm.agency_cabinet.common.consts import Services, ContractType
from crm.agency_cabinet.rewards import client as rewards_client
from crm.agency_cabinet.rewards.common import QUEUE, structs
from crm.agency_cabinet.rewards.proto import common_pb2, request_pb2, rewards_info_pb2, contracts_info_pb2, calculator_pb2


async def test_ping(client: rewards_client.RewardsClient, rmq_rpc_client: RmqRpcClient):
    rmq_rpc_client.send_proto_message.return_value = common_pb2.PingOutput(ping='pong')

    response = await client.ping()

    assert response == 'pong'

    request = request_pb2.RpcRequest(ping=common_pb2.Empty())

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name=QUEUE,
        message=request,
        response_message_type=common_pb2.PingOutput
    )


async def test_get_rewards_info(client: rewards_client.RewardsClient, rmq_rpc_client: RmqRpcClient):
    now = datetime.now()
    period_from = Timestamp()
    period_from.FromDatetime(now)
    mocked_output = rewards_info_pb2.GetRewardsInfoOutput(result=rewards_info_pb2.RewardsInfoList(
        rewards=[
            rewards_info_pb2.RewardInfo(
                id=1,
                contract_id=1111,
                type='month',
                services=['direct'],
                got_scan=False,
                got_original=False,
                is_accrued=False,
                is_paid=False,
                payment='0.11',
                period_from=period_from
            ),
            rewards_info_pb2.RewardInfo(
                id=2,
                contract_id=2222,
                type='quarter',
                services=['zen'],
                got_scan=False,
                got_original=False,
                is_accrued=False,
                is_paid=False,
                payment='0.22',
                period_from=period_from
            ),
        ]
    ))

    rmq_rpc_client.send_proto_message.return_value = mocked_output

    response = await client.get_rewards_info(agency_id=1)

    assert response == [
        structs.RewardInfo(
            id=1,
            contract_id=1111,
            type='month',
            services=['direct'],
            got_scan=False,
            got_original=False,
            is_accrued=False,
            is_paid=False,
            payment=Decimal('0.11'),
            period_from=now,
            payment_date=None
        ),
        structs.RewardInfo(
            id=2,
            contract_id=2222,
            type='quarter',
            services=['zen'],
            got_scan=False,
            got_original=False,
            is_accrued=False,
            is_paid=False,
            payment=Decimal('0.22'),
            period_from=now,
            payment_date=None
        ),
    ]

    request = request_pb2.RpcRequest(
        get_rewards_info=rewards_info_pb2.GetRewardsInfo(agency_id=1)
    )

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name=QUEUE,
        message=request,
        response_message_type=rewards_info_pb2.GetRewardsInfoOutput
    )


async def test_get_rewards_info_empty(client: rewards_client.RewardsClient, rmq_rpc_client: RmqRpcClient):
    mocked_output = rewards_info_pb2.GetRewardsInfoOutput(result=rewards_info_pb2.RewardsInfoList(rewards=[]))
    rmq_rpc_client.send_proto_message.return_value = mocked_output

    response = await client.get_rewards_info(agency_id=1)

    assert response == []

    request = request_pb2.RpcRequest(
        get_rewards_info=rewards_info_pb2.GetRewardsInfo(agency_id=1)
    )

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name=QUEUE,
        message=request,
        response_message_type=rewards_info_pb2.GetRewardsInfoOutput
    )


async def test_get_contracts_info_empty(client: rewards_client.RewardsClient, rmq_rpc_client: RmqRpcClient):
    mocked_output = contracts_info_pb2.GetContractsInfoOutput(result=contracts_info_pb2.ContractsInfoList(contracts=[]))
    rmq_rpc_client.send_proto_message.return_value = mocked_output

    response = await client.get_contracts_info(agency_id=1)

    assert response == []

    request = request_pb2.RpcRequest(
        get_contracts_info=contracts_info_pb2.GetContractsInfo(agency_id=1)
    )

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name=QUEUE,
        message=request,
        response_message_type=contracts_info_pb2.GetContractsInfoOutput
    )


async def test_get_contracts_info(client: rewards_client.RewardsClient, rmq_rpc_client: RmqRpcClient):
    finish_date = Timestamp()
    finish_date.FromDatetime(datetime(2022, 3, 1))

    mocked_output = contracts_info_pb2.GetContractsInfoOutput(result=contracts_info_pb2.ContractsInfoList(
        contracts=[
            contracts_info_pb2.ContractInfo(
                contract_id=1,
                eid='1111/1',
                inn='123456',
                services=['direct', 'media'],
                finish_date=finish_date,
                payment_type='prepayment',
                type=common_pb2.ContractType.prof,
                is_crisis=True,
            )
        ]
    ))

    rmq_rpc_client.send_proto_message.return_value = mocked_output

    response = await client.get_contracts_info(agency_id=1)

    assert response == [
        structs.ContractInfo(
            contract_id=1,
            eid='1111/1',
            inn='123456',
            services=['direct', 'media'],
            finish_date=datetime(2022, 3, 1),
            payment_type='prepayment',
            type=ContractType.prof,
            is_crisis=True,
        )
    ]

    request = request_pb2.RpcRequest(
        get_contracts_info=contracts_info_pb2.GetContractsInfo(agency_id=1)
    )

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name=QUEUE,
        message=request,
        response_message_type=contracts_info_pb2.GetContractsInfoOutput
    )


async def test_get_calculator_direct_premium_data(client: rewards_client.RewardsClient, rmq_rpc_client: RmqRpcClient):
    mocked_output = calculator_pb2.GetCalculatorDataOutput(
        result='stub'
    )

    rmq_rpc_client.send_proto_message.return_value = mocked_output

    response = await client.get_calculator_data(agency_id=1, contract_id=1, service='direct', version='2021')

    assert response == structs.GetCalculatorDataResponse(result='stub')

    request = request_pb2.RpcRequest(
        get_calculator_data=calculator_pb2.GetCalculatorData(agency_id=1, contract_id=1,
                                                             service=common_pb2.CalculatorServicesType.calc_direct, version='2021')
    )

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name=QUEUE,
        message=request,
        response_message_type=calculator_pb2.GetCalculatorDataOutput
    )


async def test_get_detailed_reward_info(client: rewards_client.RewardsClient,
                                        rmq_rpc_client: RmqRpcClient):
    period_from = Timestamp()
    period_from.FromDatetime(datetime(2021, 8, 1, 0, 0))

    mocked_output = rewards_info_pb2.GetDetailedRewardInfoOutput(
        result=rewards_info_pb2.DetailedRewardInfo(
            id=1,
            contract_id=1,
            type='month',
            services=[
                rewards_info_pb2.DetailedServiceInfo(
                    service=Services.direct.value,
                    revenue='0.11',
                    currency='RUB',
                    reward_percent='5.440',
                    accrual='0.22',
                ),
                rewards_info_pb2.DetailedServiceInfo(
                    service=Services.business.value,
                    revenue='0.11',
                    currency='RUB',
                    reward_percent='5.440',
                    accrual='0.22',
                    error_message='Error',
                ),
                rewards_info_pb2.DetailedServiceInfo(
                    service=Services.early_payment.value,
                    currency='RUB',
                    accrual='0.22',
                ),
            ],
            documents=[],
            status='no_information_available',
            accrual='0.11',
            payment='0.22',
            accrual_date=None,
            payment_date=None,
            period_from=period_from,
        )
    )
    rmq_rpc_client.send_proto_message.return_value = mocked_output

    agency_id = 0
    reward_id = 0

    response = await client.get_detailed_reward_info(
        agency_id=agency_id,
        reward_id=reward_id,
    )

    assert response == structs.DetailedRewardInfo(
        id=1,
        contract_id=1,
        type='month',
        services=[
            structs.DetailedServiceInfo(
                service=Services.direct.value,
                revenue=Decimal('0.11'),
                currency='RUB',
                reward_percent=Decimal('5.44'),
                accrual=Decimal('0.22'),
                error_message=''
            ),
            structs.DetailedServiceInfo(
                service=Services.business.value,
                revenue=Decimal('0.11'),
                currency='RUB',
                reward_percent=Decimal('5.44'),
                accrual=Decimal('0.22'),
                error_message='Error',
            ),
            structs.DetailedServiceInfo(
                service=Services.early_payment.value,
                revenue=None,
                currency='RUB',
                reward_percent=None,
                accrual=Decimal('0.22'),
                error_message='',
            )
        ],
        documents=[],
        status='no_information_available',
        accrual=Decimal('0.11'),
        payment=Decimal('0.22'),
        accrual_date=None,
        payment_date=None,
        period_from=datetime(2021, 8, 1, 0, 0),
        predict=False
    )

    request = request_pb2.RpcRequest(
        get_detailed_reward_info=rewards_info_pb2.GetDetailedRewardInfo(
            agency_id=agency_id,
            reward_id=reward_id,
        )
    )

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name=QUEUE,
        message=request,
        response_message_type=rewards_info_pb2.GetDetailedRewardInfoOutput
    )


async def test_get_detailed_reward_info_no_such_reward(client: rewards_client.RewardsClient,
                                                       rmq_rpc_client: RmqRpcClient):
    mocked_output = rewards_info_pb2.GetDetailedRewardInfoOutput(
        no_such_reward=common_pb2.Empty()
    )
    rmq_rpc_client.send_proto_message.return_value = mocked_output

    agency_id = 0
    reward_id = 0

    with pytest.raises(rewards_client.NoSuchRewardException):
        await client.get_detailed_reward_info(
            agency_id=agency_id,
            reward_id=reward_id,
        )

    request = request_pb2.RpcRequest(
        get_detailed_reward_info=rewards_info_pb2.GetDetailedRewardInfo(
            agency_id=agency_id,
            reward_id=reward_id,
        )
    )

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name=QUEUE,
        message=request,
        response_message_type=rewards_info_pb2.GetDetailedRewardInfoOutput
    )


async def test_get_detailed_reward_info_unsuitable_agency(client: rewards_client.RewardsClient,
                                                          rmq_rpc_client: RmqRpcClient):
    mocked_output = rewards_info_pb2.GetDetailedRewardInfoOutput(
        unsuitable_agency=common_pb2.Empty()
    )
    rmq_rpc_client.send_proto_message.return_value = mocked_output

    agency_id = 0
    reward_id = 0

    with pytest.raises(rewards_client.UnsuitableAgency):
        await client.get_detailed_reward_info(
            agency_id=agency_id,
            reward_id=reward_id,
        )

    request = request_pb2.RpcRequest(
        get_detailed_reward_info=rewards_info_pb2.GetDetailedRewardInfo(
            agency_id=agency_id,
            reward_id=reward_id,
        )
    )

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name=QUEUE,
        message=request,
        response_message_type=rewards_info_pb2.GetDetailedRewardInfoOutput
    )
