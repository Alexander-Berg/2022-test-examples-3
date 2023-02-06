import typing
import re
from unittest.mock import AsyncMock
from aioresponses import aioresponses

from crm.agency_cabinet.rewards.proto import documents_pb2, request_pb2
from crm.agency_cabinet.rewards.server.src.db import models
from crm.agency_cabinet.rewards.server.src.handler import Handler
from crm.agency_cabinet.rewards.server.config.tvm import RewardsTvm2Config


async def test_get_document_url(
    fixture_contracts: typing.List[models.Contract],
    fixture_documents: typing.List[models.Document],
    tvm_client,
    mocker
):

    url = 'https://example.com'

    mocker.patch(
        'crm.agency_cabinet.rewards.server.src.handler.get_tvm_client',
        mock=AsyncMock,
        return_value=tvm_client
    )

    request_pb = request_pb2.RpcRequest(
        get_document_url=documents_pb2.GetDocumentUrl(
            agency_id=fixture_contracts[0].agency_id,
            document_id=fixture_documents[0].id
        )
    )
    with aioresponses() as m:
        api_path = re.compile(r'^{}.*$'.format(url))
        m.get(api_path, status=200, body=f'{{"url":"{url}"}}')

        tvm2_config = RewardsTvm2Config.from_environ({'TVM2_ASYNC': 1, 'TVM2_CLIENT_ID': 1})
        handler = Handler(yadoc_endpoint_url=url, tvm_config=tvm2_config)
        await handler.setup()
        output = await handler(request_pb.SerializeToString())

        result = documents_pb2.GetDocumentUrlOutput.FromString(output).result.url
        await handler.teardown()
        assert result == url
