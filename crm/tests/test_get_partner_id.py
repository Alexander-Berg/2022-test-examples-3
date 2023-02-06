import pytest
from crm.agency_cabinet.grants.common.consts.partner import PartnerType
from crm.agency_cabinet.grants.common import QUEUE
from crm.agency_cabinet.grants.proto import request_pb2, partners_pb2, common_pb2
from crm.agency_cabinet.grants.common import structs
from crm.agency_cabinet.grants.client import NotHavePermission, PartnerNotFound
from crm.agency_cabinet.grants import client as grants_client
from smb.common.rmq.rpc.client import RmqRpcClient


async def test_get_partner_id(client: grants_client.GrantsClient, rmq_rpc_client: RmqRpcClient):
    mocked_output = partners_pb2.GetPartnerIDOutput(result=partners_pb2.PartnerID(
        partner_id=1
    ))

    rmq_rpc_client.send_proto_message.return_value = mocked_output

    response = await client.get_partner_id(yandex_uid=100, external_id='123', type=PartnerType.agency)

    assert response == structs.GetPartnerIDResponse(
        partner_id=1
    )

    request = request_pb2.RpcRequest(get_partner_id=partners_pb2.GetPartnerIDInput(
        yandex_uid=100, external_id='123', type=partners_pb2.PartnerType.AGENCY))

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name=QUEUE,
        message=request,
        response_message_type=partners_pb2.GetPartnerIDOutput
    )


async def test_partner_not_found(client: grants_client.GrantsClient, rmq_rpc_client: RmqRpcClient):
    rmq_rpc_client.send_proto_message.return_value = partners_pb2.GetPartnerIDOutput(
        not_found=common_pb2.ErrorMessageResponse(message='Partner not found')
    )

    with pytest.raises(PartnerNotFound):
        await client.get_partner_id(yandex_uid=123, external_id='123', type=PartnerType.agency)


async def test_partner_no_permission(client: grants_client.GrantsClient, rmq_rpc_client: RmqRpcClient):
    rmq_rpc_client.send_proto_message.return_value = partners_pb2.GetPartnerIDOutput(
        not_have_permission=common_pb2.ErrorMessageResponse(message='Partner not found')
    )

    with pytest.raises(NotHavePermission):
        await client.get_partner_id(yandex_uid=123, external_id='123', type=PartnerType.agency)
