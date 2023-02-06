import typing

from decimal import Decimal
from google.protobuf.timestamp_pb2 import Timestamp

from crm.agency_cabinet.common.proto_utils import decimal_to_string
from crm.agency_cabinet.rewards.proto import common_pb2, request_pb2, rewards_info_pb2
from crm.agency_cabinet.rewards.server.src.db import models
from crm.agency_cabinet.rewards.server.src.handler import Handler
from crm.agency_cabinet.common.consts import PaymentsStatuses, Services


async def test_get_detailed_reward_info_not_found(fixture_rewards: typing.List[models.Reward],
                                                  fixture_contracts: typing.List[models.Contract],
                                                  fixture_service_rewards: typing.List[models.ServiceReward]):
    contract = fixture_contracts[0]
    request_pb = request_pb2.RpcRequest(
        get_detailed_reward_info=rewards_info_pb2.GetDetailedRewardInfo(
            agency_id=contract.agency_id,
            reward_id=404,
        )
    )

    output = await Handler()(request_pb.SerializeToString())

    reward = rewards_info_pb2.GetDetailedRewardInfoOutput.FromString(output)
    reward_type = reward.WhichOneof('response')

    assert reward_type == 'no_such_reward'
    assert reward.no_such_reward == common_pb2.Empty()


async def test_get_detailed_reward_info_unsuitable_agency(fixture_rewards: typing.List[models.Reward],
                                                          fixture_contracts: typing.List[models.Contract],
                                                          fixture_service_rewards: typing.List[models.ServiceReward]):
    requested_reward = fixture_rewards[0]
    request_pb = request_pb2.RpcRequest(
        get_detailed_reward_info=rewards_info_pb2.GetDetailedRewardInfo(
            agency_id=404,
            reward_id=requested_reward.id,
        )
    )

    output = await Handler()(request_pb.SerializeToString())

    reward = rewards_info_pb2.GetDetailedRewardInfoOutput.FromString(output)
    reward_type = reward.WhichOneof('response')

    assert reward_type == 'unsuitable_agency'
    assert reward.unsuitable_agency == common_pb2.Empty()


async def test_get_detailed_reward_info(fixture_rewards: typing.List[models.Reward],
                                        fixture_contracts: typing.List[models.Contract],
                                        fixture_service_rewards: typing.List[models.ServiceReward],
                                        fixture_documents: typing.List[models.Document]):
    requested_reward = fixture_rewards[0]
    contract = fixture_contracts[0]
    requested_document = fixture_documents[0]
    request_pb = request_pb2.RpcRequest(
        get_detailed_reward_info=rewards_info_pb2.GetDetailedRewardInfo(
            agency_id=contract.agency_id,
            reward_id=requested_reward.id,
        )
    )

    period_from = Timestamp()
    period_from.FromDatetime(requested_reward.period_from)

    accrual_date = Timestamp()
    accrual_date.FromDatetime(requested_reward.created_at)

    sending_date = Timestamp()
    sending_date.FromDatetime(requested_document.sending_date)

    payment_date = None
    if requested_reward.payment_date:
        payment_date = Timestamp()
        payment_date.FromDatetime(requested_reward.payment_date)

    services = [
        rewards_info_pb2.DetailedServiceInfo(
            service=Services.direct.value,
            revenue='0.75',
            currency='RUB',
            reward_percent='5.440',
            accrual='0.52',
            error_message=None,
        ),
        rewards_info_pb2.DetailedServiceInfo(
            service=Services.zen.value,
            revenue='0.62',
            currency='RUB',
            reward_percent='7.000',
            accrual='0.33',
            error_message=None,
        )
    ]

    accrual = sum(Decimal(service.accrual) for service in services) if services else None

    expected_reward_output = rewards_info_pb2.DetailedRewardInfo(
        id=requested_reward.id,
        contract_id=requested_reward.contract_id,
        type=requested_reward.type,
        services=services,
        documents=[
            rewards_info_pb2.DocumentInfo(
                id=requested_document.id,
                name=requested_document.name,
                sending_date=sending_date,
                got_scan=requested_document.got_scan,
                got_original=requested_document.got_original
            )
        ],
        status=PaymentsStatuses.accrued.value,
        accrual=decimal_to_string(accrual),
        payment=decimal_to_string(requested_reward.payment),
        accrual_date=accrual_date,
        payment_date=payment_date,
        period_from=period_from,
    )
    output = await Handler()(request_pb.SerializeToString())

    reward = rewards_info_pb2.GetDetailedRewardInfoOutput.FromString(output).result

    assert reward == expected_reward_output


async def test_get_detailed_reward_info_check_sum(fixture_rewards: typing.List[models.Reward],
                                                  fixture_contracts: typing.List[models.Contract],
                                                  fixture_service_rewards: typing.List[models.ServiceReward],
                                                  fixture_documents: typing.List[models.Document]):
    requested_reward = fixture_rewards[1]
    contract = fixture_contracts[0]
    requested_document = fixture_documents[1]
    request_pb = request_pb2.RpcRequest(
        get_detailed_reward_info=rewards_info_pb2.GetDetailedRewardInfo(
            agency_id=contract.agency_id,
            reward_id=requested_reward.id,
        )
    )

    period_from = Timestamp()
    period_from.FromDatetime(requested_reward.period_from)

    accrual_date = Timestamp()
    accrual_date.FromDatetime(requested_reward.created_at)

    sending_date = Timestamp()
    sending_date.FromDatetime(requested_document.sending_date)

    payment_date = None
    if requested_reward.payment_date:
        payment_date = Timestamp()
        payment_date.FromDatetime(requested_reward.payment_date)

    services = [
        rewards_info_pb2.DetailedServiceInfo(
            service=Services.media.value,
            revenue='4200.43',
            currency='RUB',
            reward_percent='7.152',
            accrual='300.41',
            error_message=None,
        )
    ]

    accrual = sum(Decimal(service.accrual) for service in services) if services else None

    expected_reward_output = rewards_info_pb2.DetailedRewardInfo(
        id=requested_reward.id,
        contract_id=requested_reward.contract_id,
        type=requested_reward.type,
        services=services,
        documents=[
            rewards_info_pb2.DocumentInfo(
                id=requested_document.id,
                name=requested_document.name,
                sending_date=sending_date,
                got_scan=requested_document.got_scan,
                got_original=requested_document.got_original
            )
        ],
        status=PaymentsStatuses.paid.value,
        accrual=decimal_to_string(accrual),
        payment=decimal_to_string(requested_reward.payment),
        accrual_date=accrual_date,
        payment_date=payment_date,
        period_from=period_from,
    )
    output = await Handler()(request_pb.SerializeToString())

    reward = rewards_info_pb2.GetDetailedRewardInfoOutput.FromString(output).result

    assert reward == expected_reward_output
