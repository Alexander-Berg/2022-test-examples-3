import pytest

from unittest.mock import AsyncMock

from crm.agency_cabinet.ord.proto import clients_pb2, common_pb2, request_pb2
from crm.agency_cabinet.ord.common import structs
from crm.agency_cabinet.ord.common.exceptions import UnsuitableAgencyException, UnsuitableReportException, \
    UnsuitableClientException


@pytest.fixture
def procedure(mocker):
    mock = AsyncMock()
    mock.return_value = structs.ClientShortInfo(
        id=42,
        client_id='123456',
        login='test-login',
        name='test name'
    )

    mocker.patch(
        'crm.agency_cabinet.ord.server.src.procedures.GetClientShortInfo',
        return_value=mock,
    )

    return mock


async def test_get_client_short_info(handler, procedure):
    request_pb = request_pb2.RpcRequest(
        get_client_short_info=structs.ClientShortInfoInput(
            agency_id=1,
            report_id=10,
            client_id=42
        ).to_proto()
    )
    data = await handler(request_pb.SerializeToString())

    message = clients_pb2.ClientShortInfoOutput.FromString(data)
    res = structs.ClientShortInfo.from_proto(message.result)

    assert res == structs.ClientShortInfo(
        id=42,
        client_id='123456',
        login='test-login',
        name='test name'
    )


@pytest.mark.parametrize(
    ('side_effect', 'expected_message'),
    [
        (
            UnsuitableAgencyException,
            clients_pb2.ClientShortInfoOutput(unsuitable_agency=common_pb2.ErrorMessageResponse(message=''))
        ),
        (
            UnsuitableReportException,
            clients_pb2.ClientShortInfoOutput(unsuitable_report=common_pb2.ErrorMessageResponse(message=''))
        ),
        (
            UnsuitableClientException,
            clients_pb2.ClientShortInfoOutput(unsuitable_client=common_pb2.ErrorMessageResponse(message=''))
        ),
    ]
)
async def test_get_client_short_info_returns_error(handler, mocker, side_effect, expected_message):
    mock = AsyncMock()
    mock.return_value = None
    mock.side_effect = [side_effect]

    mocker.patch(
        'crm.agency_cabinet.ord.server.src.procedures.GetClientShortInfo',
        return_value=mock,
    )

    request_pb = request_pb2.RpcRequest(
        get_client_short_info=structs.ClientShortInfoInput(
            agency_id=1,
            report_id=10,
            client_id=42
        ).to_proto()
    )
    result = await handler(request_pb.SerializeToString())

    assert clients_pb2.ClientShortInfoOutput.FromString(result) == expected_message
