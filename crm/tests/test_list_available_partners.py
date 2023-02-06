import pytest

from crm.agency_cabinet.grants.common import QUEUE
from crm.agency_cabinet.grants.proto import request_pb2, partners_pb2, common_pb2
from crm.agency_cabinet.grants.client import NoSuchUserException
from crm.agency_cabinet.grants.common import structs
from crm.agency_cabinet.grants import client as grants_client
from smb.common.rmq.rpc.client import RmqRpcClient


async def test_list_available_partners(client: grants_client.GrantsClient, rmq_rpc_client: RmqRpcClient):
    mocked_output = partners_pb2.ListAvailablePartnersOutput(result=partners_pb2.PartnersList(partners=[
        partners_pb2.Partner(
            partner_id=1,
            external_id='123',
            type=partners_pb2.PartnerType.AGENCY,
            name='Test'
        ),
        partners_pb2.Partner(
            partner_id=2,
            external_id='124',
            type=partners_pb2.PartnerType.AGENCY,
            name='Test2'
        ),
    ], size=2
    ))

    rmq_rpc_client.send_proto_message.return_value = mocked_output

    response = await client.list_available_partners(yandex_uid=100)

    assert response == structs.ListAvailablePartnersResponse(partners=[
        structs.Partner(
            partner_id=1,
            external_id='123',
            type='agency',
            name='Test'
        ),
        structs.Partner(
            partner_id=2,
            external_id='124',
            type='agency',
            name='Test2'
        )
    ],
        size=2
    )

    request = request_pb2.RpcRequest(list_available_partners=partners_pb2.ListAvailablePartnersInput(yandex_uid=100))

    rmq_rpc_client.send_proto_message.assert_awaited_with(
        queue_name=QUEUE,
        message=request,
        response_message_type=partners_pb2.ListAvailablePartnersOutput
    )


async def test_list_available_partners_unknown_user(client: grants_client.GrantsClient, rmq_rpc_client: RmqRpcClient):
    mocked_output = partners_pb2.ListAvailablePartnersOutput(no_such_user=common_pb2.ErrorMessageResponse(message='Error'))

    rmq_rpc_client.send_proto_message.return_value = mocked_output

    with pytest.raises(NoSuchUserException):
        await client.list_available_partners(yandex_uid=100)
