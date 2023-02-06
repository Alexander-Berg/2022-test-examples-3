import typing
from unittest.mock import AsyncMock
from crm.agency_cabinet.agencies.proto import request_pb2, agency_info_pb2
from crm.agency_cabinet.agencies.server.src.handler import Handler
from crm.agency_cabinet.agencies.common import structs


async def test_get_agencies_info_empty(handler: Handler, mocker):
    procedure = AsyncMock()
    procedure.return_value = structs.GetAgenciesInfoResponse(agencies=[])

    mocker.patch(
        "crm.agency_cabinet.agencies.server.src.procedures.GetAgenciesInfo",
        return_value=procedure,
    )

    request_pb = request_pb2.RpcRequest(
        get_agencies_info=agency_info_pb2.GetAgenciesInfo(agency_ids=[404])
    )

    output = await handler(request_pb.SerializeToString())

    procedure.assert_awaited_with(
        request=structs.GetAgenciesInfoRequest(
            agency_ids=[404]
        )
    )

    assert agency_info_pb2.GetAgenciesInfoOutput.FromString(output) == agency_info_pb2.GetAgenciesInfoOutput(
        result=agency_info_pb2.AgencyInfoList(agencies=[])
    )


async def test_get_agencies_info(mocker, all_agencies_struct: typing.List[structs.AgencyInfo]):
    agencies = all_agencies_struct[:2]

    procedure = AsyncMock()
    procedure.return_value = structs.GetAgenciesInfoResponse(agencies=agencies)

    mocker.patch(
        "crm.agency_cabinet.agencies.server.src.procedures.GetAgenciesInfo",
        return_value=procedure,
    )

    request_pb = request_pb2.RpcRequest(
        get_agencies_info=agency_info_pb2.GetAgenciesInfo(
            agency_ids=[agency.agency_id for agency in agencies]
        )
    )

    output = await Handler()(request_pb.SerializeToString())

    procedure.assert_awaited_with(
        request=structs.GetAgenciesInfoRequest(
            agency_ids=[agency.agency_id for agency in agencies]
        )
    )

    assert agency_info_pb2.GetAgenciesInfoOutput.FromString(
        output).result == structs.GetAgenciesInfoResponse(agencies=agencies).to_proto()
