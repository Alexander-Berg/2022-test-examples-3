import typing
import pytest
from unittest.mock import AsyncMock
from crm.agency_cabinet.agencies.proto import request_pb2, clients_pb2
from crm.agency_cabinet.agencies.common import structs
from crm.agency_cabinet.agencies.server.src import procedures


async def test_get_client_info(mocker, handler, all_clients_struct: typing.List[structs.ClientInfo]):
    procedure = AsyncMock()
    procedure.return_value = all_clients_struct[0]

    mocker.patch(
        "crm.agency_cabinet.agencies.server.src.procedures.GetClientInfo",
        return_value=procedure,
    )

    request_pb = request_pb2.RpcRequest(
        get_client_info=clients_pb2.GetClientInfo(
            agency_id=1,
            client_id=all_clients_struct[0].id
        )
    )

    output = await handler(request_pb.SerializeToString())

    procedure.assert_awaited_with(
        request=structs.GetClientInfoRequest(
            agency_id=1,
            client_id=all_clients_struct[0].id
        )
    )

    assert clients_pb2.GetClientInfoOutput.FromString(output).result \
           == all_clients_struct[0].to_proto()


@pytest.mark.parametrize(('exception_class', 'field_name'), [
    (procedures.NoSuchClient, 'no_such_client'),
    (procedures.UnsuitableAgency, 'unsuitable_agency'),
])
async def test_get_client_info_exception(mocker, handler, exception_class, field_name):
    procedure = AsyncMock()
    procedure.side_effect = [exception_class]

    mocker.patch(
        "crm.agency_cabinet.agencies.server.src.procedures.GetClientInfo",
        return_value=procedure,
    )

    request_pb = request_pb2.RpcRequest(
        get_client_info=clients_pb2.GetClientInfo(
            agency_id=1,
            client_id=1
        )
    )

    output = await handler(request_pb.SerializeToString())

    procedure.assert_awaited_with(
        request=structs.GetClientInfoRequest(
            agency_id=1,
            client_id=1
        )
    )

    response_proto = clients_pb2.GetClientInfoOutput.FromString(output)
    assert not response_proto.HasField('result')
    assert response_proto.HasField(field_name)
