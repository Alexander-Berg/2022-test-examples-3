import typing

import pytest
from crm.agency_cabinet.documents.common.structs import (
    GetFactureUrlInput
)
from crm.agency_cabinet.common.server.common.structs.common import UrlResponse
from crm.agency_cabinet.documents.server.src.procedures import GetFactureUrl
from sqlalchemy.engine.result import RowProxy
from crm.agency_cabinet.common.yadoc.base import YaDocClient

pytest_plugins = [
    'crm.agency_cabinet.common.yadoc.pytest.plugin',
]


@pytest.fixture
def procedure():
    return GetFactureUrl()


async def test_get_act_url(
    procedure,
    fixture_contracts: typing.List[RowProxy],
    fixture_acts: typing.List[RowProxy],
    fixture_invoices: typing.List[RowProxy],
    fixture_factures: typing.List[RowProxy],
    yadoc_client: YaDocClient,
    ya_doc_response: dict
):
    yadoc_client.get_first_doc_info.return_value = ya_doc_response['content'][0]
    yadoc_client.get_doc_url.return_value = 'https://test.com'
    got = await procedure(GetFactureUrlInput(
        agency_id=123,
        facture_id=fixture_factures[0].id,)
        , yadoc_client
    )
    assert got == UrlResponse(url='https://test.com')
