
from datetime import datetime
from decimal import Decimal
from google.protobuf.timestamp_pb2 import Timestamp

from crm.agency_cabinet.common.consts import Services
from crm.agency_cabinet.rewards import client as rewards_client
from crm.agency_cabinet.rewards.common import QUEUE, structs
from crm.agency_cabinet.rewards.proto import request_pb2, dashboard_pb2
from smb.common.rmq.rpc.client import RmqRpcClient


async def test_get_dashboard(client: rewards_client.RewardsClient, rmq_rpc_client: RmqRpcClient):
    now = datetime.now()
    period_from = Timestamp()
    period_from.FromDatetime(now)

    mocked_output = dashboard_pb2.DashboardOutput(result=dashboard_pb2.Dashboard(
        dashboard=[
            dashboard_pb2.DashboardItem(
                contract_id=1111,
                service=Services.direct.value,
                active=True,
                rewards=dashboard_pb2.DashboardRewardsMap(
                    month=[
                        dashboard_pb2.DashboardReward(
                            reward='1000',
                            reward_percent='4.321',
                            predict=False,
                            period_from=period_from
                        )
                    ],
                    quarter=[
                        dashboard_pb2.DashboardReward(
                            reward='2000',
                            reward_percent='5.678',
                            predict=False,
                            period_from=period_from
                        )
                    ],
                    semiyear=[
                        dashboard_pb2.DashboardReward(
                            reward='3000',
                            reward_percent='3',
                            predict=True,
                            period_from=period_from
                        )
                    ]
                ),
                updated_at=period_from
            ),
            dashboard_pb2.DashboardItem(
                contract_id=1111,
                service=Services.zen.value,
                active=False,
            ),
        ]
    ))

    rmq_rpc_client.send_proto_message.return_value = mocked_output

    response = await client.get_dashboard(agency_id=1, year=2021)

    assert response == [
        structs.DashboardItem(
            contract_id=1111,
            service=Services.direct,
            active=True,
            rewards=structs.DashboardRewardsMap(
                month=[
                    structs.DashboardReward(
                        reward=Decimal(1000),
                        reward_percent=Decimal('4.321'),
                        predict=False,
                        period_from=now
                    )
                ],
                quarter=[
                    structs.DashboardReward(
                        reward=Decimal(2000),
                        reward_percent=Decimal('5.678'),
                        predict=False,
                        period_from=now
                    )
                ],
                semiyear=[
                    structs.DashboardReward(
                        reward=Decimal(3000),
                        reward_percent=Decimal('3'),
                        predict=True,
                        period_from=now
                    )
                ]
            ),
            updated_at=now
        ),
        structs.DashboardItem(
            contract_id=1111,
            service=Services.zen,
            active=False,
            rewards=None,
            updated_at=None
        ),
    ]

    request = request_pb2.RpcRequest(
        get_dashboard=dashboard_pb2.GetDashboard(agency_id=1, year=2021)
    )

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name=QUEUE,
        message=request,
        response_message_type=dashboard_pb2.DashboardOutput
    )


async def test_get_dashboard_empty(client: rewards_client.RewardsClient, rmq_rpc_client: RmqRpcClient):
    mocked_output = dashboard_pb2.DashboardOutput(result=dashboard_pb2.Dashboard(dashboard=[]))
    rmq_rpc_client.send_proto_message.return_value = mocked_output

    response = await client.get_dashboard(agency_id=1, year=2021)

    assert response == []

    request = request_pb2.RpcRequest(
        get_dashboard=dashboard_pb2.GetDashboard(agency_id=1, year=2021)
    )

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name=QUEUE,
        message=request,
        response_message_type=dashboard_pb2.DashboardOutput
    )
