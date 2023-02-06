import pytest
from unittest.mock import AsyncMock

from crm.agency_cabinet.common.server.common.structs.common import UrlResponse
from crm.agency_cabinet.common.server.common.proto import common_pb2
from crm.agency_cabinet.documents.proto import invoices_pb2, request_pb2

pytestmark = [pytest.mark.asyncio]


@pytest.fixture(autouse=True)
def procedure(mocker):
    mock = AsyncMock()
    mock.return_value = UrlResponse(
        url='http://mock.report.url'
    )

    mocker.patch(
        "crm.agency_cabinet.documents.server.src.procedures.GetFactureUrl",
        return_value=mock,
    )

    return mock


async def test_returns_serialized_operation_result(handler):
    input_pb = request_pb2.RpcRequest(
        get_facture_url=invoices_pb2.GetFactureUrlInput(
            agency_id=22, facture_id=42
        )
    )
    result = await handler(input_pb.SerializeToString())

    assert invoices_pb2.GetFactureUrlOutput.FromString(result) == invoices_pb2.GetFactureUrlOutput(
        result=common_pb2.Url(url='http://mock.report.url')
    )
