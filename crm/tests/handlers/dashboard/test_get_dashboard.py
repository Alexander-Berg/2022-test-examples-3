import typing

from dateutil.relativedelta import relativedelta
from google.protobuf.timestamp_pb2 import Timestamp

from crm.agency_cabinet.common.consts import Services, START_FIN_YEAR_2021
from crm.agency_cabinet.rewards.proto import request_pb2, dashboard_pb2, common_pb2
from crm.agency_cabinet.rewards.server.src.db import models
from crm.agency_cabinet.rewards.server.src.handler import Handler


async def test_get_dashboard(fixture_dashboard_rewards: typing.List[models.Reward],
                             fixture_contracts: typing.List[models.Contract],
                             fixture_dashboard_service_rewards: typing.List[models.ServiceReward]):
    contract = fixture_contracts[3]

    request_pb = request_pb2.RpcRequest(
        get_dashboard=dashboard_pb2.GetDashboard(
            agency_id=contract.agency_id,
            year=2021
        )
    )

    output = await Handler()(request_pb.SerializeToString())
    dashboard = dashboard_pb2.DashboardOutput.FromString(output).result.dashboard
    direct_item = next(
        filter(lambda item: item.service == Services.direct.value and item.contract_id == contract.id, dashboard),
        None
    )

    assert len(dashboard) == 10
    assert direct_item is not None
    assert direct_item.active

    expected_updated_at = Timestamp()
    expected_updated_at.FromDatetime(fixture_dashboard_rewards[0].updated_at)
    assert direct_item.updated_at

    assert len(direct_item.rewards.month) == 12
    assert len(direct_item.rewards.quarter) == 4
    assert len(direct_item.rewards.semiyear) == 2

    period_from = Timestamp()
    period_from.FromDatetime(START_FIN_YEAR_2021)
    expected_month_reward_0 = dashboard_pb2.DashboardReward(
        reward='50',
        reward_percent='8.333',
        period_from=period_from,
        predict=False
    )
    assert direct_item.rewards.month[0] == expected_month_reward_0

    period_from = Timestamp()
    period_from.FromDatetime(START_FIN_YEAR_2021 + relativedelta(months=4))
    expected_month_reward_4 = dashboard_pb2.DashboardReward(
        reward='100',
        reward_percent='12.500',
        period_from=period_from,
        predict=True
    )
    assert direct_item.rewards.month[4] == expected_month_reward_4

    period_from = Timestamp()
    period_from.FromDatetime(START_FIN_YEAR_2021 + relativedelta(months=3))
    expected_quarter_reward_1 = dashboard_pb2.DashboardReward(
        reward='100',
        reward_percent='5.882',
        period_from=period_from,
        predict=False
    )
    assert direct_item.rewards.quarter[1] == expected_quarter_reward_1

    period_from = Timestamp()
    period_from.FromDatetime(START_FIN_YEAR_2021)
    expected_semiyear_reward_0 = dashboard_pb2.DashboardReward(
        reward='100',
        reward_percent='5.000',
        period_from=period_from,
        predict=False
    )
    assert direct_item.rewards.semiyear[0] == expected_semiyear_reward_0


async def test_get_dashboard_check_sum(fixture_dashboard_rewards: typing.List[models.Reward],
                                       fixture_contracts: typing.List[models.Contract],
                                       fixture_dashboard_service_rewards: typing.List[models.ServiceReward]):
    contract = fixture_contracts[3]

    request_pb = request_pb2.RpcRequest(
        get_dashboard=dashboard_pb2.GetDashboard(
            agency_id=contract.agency_id,
            year=2021
        )
    )

    output = await Handler()(request_pb.SerializeToString())
    dashboard = dashboard_pb2.DashboardOutput.FromString(output).result.dashboard

    business_item = next(
        filter(lambda item: item.service == Services.business.value and item.contract_id == contract.id, dashboard),
        None
    )
    period_from = Timestamp()
    period_from.FromDatetime(START_FIN_YEAR_2021)
    expected_month_reward_0 = dashboard_pb2.DashboardReward(
        reward='200',
        reward_percent='12.500',
        period_from=period_from,
        predict=False
    )
    assert business_item.rewards.month[0] == expected_month_reward_0


async def test_get_dashboard_filter_contract(fixture_dashboard_rewards: typing.List[models.Reward],
                                             fixture_contracts: typing.List[models.Contract],
                                             fixture_dashboard_service_rewards: typing.List[models.ServiceReward]):
    contract = fixture_contracts[3]

    request_pb = request_pb2.RpcRequest(
        get_dashboard=dashboard_pb2.GetDashboard(
            agency_id=contract.agency_id,
            filter_contract=contract.id,
            year=2021
        )
    )

    output = await Handler()(request_pb.SerializeToString())
    dashboard = dashboard_pb2.DashboardOutput.FromString(output).result.dashboard

    assert len(dashboard) == 5


async def test_get_dashboard_filter_service(fixture_dashboard_rewards: typing.List[models.Reward],
                                            fixture_contracts: typing.List[models.Contract],
                                            fixture_dashboard_service_rewards: typing.List[models.ServiceReward]):
    contract = fixture_contracts[3]

    request_pb = request_pb2.RpcRequest(
        get_dashboard=dashboard_pb2.GetDashboard(
            agency_id=contract.agency_id,
            filter_service=Services.direct.value,
            year=2021
        )
    )

    output = await Handler()(request_pb.SerializeToString())
    dashboard = dashboard_pb2.DashboardOutput.FromString(output).result.dashboard

    assert len(dashboard) == 2


async def test_get_dashboard_empty(fixture_dashboard_rewards: typing.List[models.Reward],
                                   fixture_contracts: typing.List[models.Contract],
                                   fixture_dashboard_service_rewards: typing.List[models.ServiceReward]):

    request_pb = request_pb2.RpcRequest(
        get_dashboard=dashboard_pb2.GetDashboard(
            agency_id=10,
            year=2021
        )
    )

    output = await Handler()(request_pb.SerializeToString())
    dashboard = dashboard_pb2.DashboardOutput.FromString(output).result.dashboard

    assert len(dashboard) == 0


async def test_get_dashboard_unknown_year(fixture_dashboard_rewards: typing.List[models.Reward],
                                            fixture_contracts: typing.List[models.Contract],
                                            fixture_dashboard_service_rewards: typing.List[models.ServiceReward]):
    contract = fixture_contracts[3]

    request_pb = request_pb2.RpcRequest(
        get_dashboard=dashboard_pb2.GetDashboard(
            agency_id=contract.agency_id,
            filter_service=Services.direct.value,
            year=3000
        )
    )

    output = await Handler()(request_pb.SerializeToString())
    proto = dashboard_pb2.DashboardOutput.FromString(output)
    result = proto.WhichOneof('response')
    assert result == 'unsupported_parameters'
    assert proto.unsupported_parameters == common_pb2.Empty()
