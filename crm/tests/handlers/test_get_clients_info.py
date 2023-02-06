import typing
from unittest.mock import AsyncMock
from crm.agency_cabinet.agencies.proto import request_pb2, clients_pb2
from crm.agency_cabinet.agencies.server.src.db import models
from crm.agency_cabinet.agencies.common import structs


async def test_get_clients_info(handler, mocker, all_clients_struct: typing.List[models.Client]):
    procedure = AsyncMock()
    procedure.return_value = structs.GetClientsInfoResponse(clients=all_clients_struct)

    mocker.patch(
        "crm.agency_cabinet.agencies.server.src.procedures.GetClientsInfo",
        return_value=procedure,
    )

    request_pb = request_pb2.RpcRequest(
        get_clients_info=clients_pb2.GetClientsInfo(
            agency_id=1,
        )
    )

    output = await handler(request_pb.SerializeToString())

    procedure.assert_awaited_with(
        request=structs.GetClientsInfoRequest(
            agency_id=1,
        )
    )

    assert clients_pb2.GetClientsInfoOutput.FromString(output).result \
           == structs.GetClientsInfoResponse(clients=all_clients_struct).to_proto()
