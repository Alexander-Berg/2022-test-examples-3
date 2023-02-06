import typing
from unittest.mock import AsyncMock
from crm.agency_cabinet.agencies.proto import request_pb2, agency_info_pb2, common_pb2
from crm.agency_cabinet.agencies.server.src.handler import Handler
from crm.agency_cabinet.agencies.common import structs


async def test_get_all_agencies_info(mocker, all_agencies_struct: typing.List[structs.AgencyInfo]):
    procedure = AsyncMock()
    procedure.return_value = structs.GetAgenciesInfoResponse(agencies=all_agencies_struct)

    mocker.patch(
        "crm.agency_cabinet.agencies.server.src.procedures.GetAllAgenciesInfo",
        return_value=procedure,
    )

    request_pb = request_pb2.RpcRequest(
        get_all_agencies_info=common_pb2.Empty()
    )

    output = await Handler()(request_pb.SerializeToString())

    procedure.assert_awaited_with()

    assert agency_info_pb2.GetAgenciesInfoOutput.FromString(output).result \
        == structs.GetAgenciesInfoResponse(agencies=all_agencies_struct).to_proto()
