import pytest
from crm.agency_cabinet.grants.common import QUEUE
from crm.agency_cabinet.grants.common.consts import PartnerType
from crm.agency_cabinet.grants.proto import request_pb2, partners_pb2, common_pb2
from crm.agency_cabinet.grants.common import structs
from crm.agency_cabinet.grants.client import NotHavePermission, PartnerNotFound
from crm.agency_cabinet.grants import client as grants_client
from smb.common.rmq.rpc.client import RmqRpcClient


async def test_get_partner(client: grants_client.GrantsClient, rmq_rpc_client: RmqRpcClient):
    mocked_output = partners_pb2.GetPartnerOutput(result=partners_pb2.Partner(
        external_id='123',
        type='AGENCY',
        partner_id=1,
        name='Test'
    ))

    rmq_rpc_client.send_proto_message.return_value = mocked_output

    response = await client.get_partner(yandex_uid=100, partner_id=1)

    assert response == structs.Partner(
        external_id='123',
        type=PartnerType.agency,
        partner_id=1,
        name='Test'
    )

    request = request_pb2.RpcRequest(get_partner=partners_pb2.GetPartnerInput(yandex_uid=100, partner_id=1))

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name=QUEUE,
        message=request,
        response_message_type=partners_pb2.GetPartnerOutput
    )


async def test_partner_not_found(client: grants_client.GrantsClient, rmq_rpc_client: RmqRpcClient):
    rmq_rpc_client.send_proto_message.return_value = partners_pb2.GetPartnerOutput(
        not_found=common_pb2.ErrorMessageResponse(message='Partner not found')
    )

    with pytest.raises(PartnerNotFound):
        await client.get_partner(yandex_uid=123, partner_id=1)


async def test_partner_no_permission(client: grants_client.GrantsClient, rmq_rpc_client: RmqRpcClient):
    rmq_rpc_client.send_proto_message.return_value = partners_pb2.GetPartnerOutput(
        not_have_permission=common_pb2.ErrorMessageResponse(message='Partner not found')
    )

    with pytest.raises(NotHavePermission):
        await client.get_partner(yandex_uid=123, partner_id=1)
