from crm.agency_cabinet.agencies.common import QUEUE
from crm.agency_cabinet.agencies.proto import request_pb2, agency_info_pb2
from crm.agency_cabinet.agencies.common import structs
from crm.agency_cabinet.agencies.client import AgenciesClient
from smb.common.rmq.rpc.client import RmqRpcClient


async def test_get_agencies_info(client: AgenciesClient, rmq_rpc_client: RmqRpcClient):
    mocked_output = agency_info_pb2.GetAgenciesInfoOutput(result=agency_info_pb2.AgencyInfoList(
        agencies=[
            agency_info_pb2.AgencyInfo(
                agency_id=1,
                name='test',
                phone='123456789',
                email='1@agency.ru',
                site='agency.ru',
                actual_address='',
                legal_address=''
            ),
            agency_info_pb2.AgencyInfo(
                agency_id=2,
                name='test2',
                phone='987654321',
                email='2@agency.ru',
                site='agency2.ru',
                actual_address='',
                legal_address=''
            ),
        ]
    ))

    rmq_rpc_client.send_proto_message.return_value = mocked_output

    response = await client.get_agencies_info(agency_ids=[1, 2])

    assert response == [
        structs.AgencyInfo(
            agency_id=1,
            name='test',
            phone='123456789',
            email='1@agency.ru',
            site='agency.ru',
            actual_address='',
            legal_address=''
        ),
        structs.AgencyInfo(
            agency_id=2,
            name='test2',
            phone='987654321',
            email='2@agency.ru',
            site='agency2.ru',
            actual_address='',
            legal_address=''
        )
    ]

    request = request_pb2.RpcRequest(
        get_agencies_info=agency_info_pb2.GetAgenciesInfo(agency_ids=[1, 2])
    )

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name=QUEUE,
        message=request,
        response_message_type=agency_info_pb2.GetAgenciesInfoOutput
    )


async def test_get_agencies_info_empty(client: AgenciesClient, rmq_rpc_client: RmqRpcClient):
    mocked_output = agency_info_pb2.GetAgenciesInfoOutput(result=agency_info_pb2.AgencyInfoList(agencies=[]))
    rmq_rpc_client.send_proto_message.return_value = mocked_output

    response = await client.get_agencies_info(agency_ids=[1, 2])

    assert response == []

    request = request_pb2.RpcRequest(
        get_agencies_info=agency_info_pb2.GetAgenciesInfo(agency_ids=[1, 2])
    )

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name=QUEUE,
        message=request,
        response_message_type=agency_info_pb2.GetAgenciesInfoOutput
    )
