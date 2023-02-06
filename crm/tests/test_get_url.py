import pytest

from crm.agency_cabinet.common.server.common.proto import common_pb2
from crm.agency_cabinet.documents.proto import invoices_pb2, acts_pb2, agreements_pb2, contracts_pb2


pytestmark = [pytest.mark.asyncio]


async def test_returns_invoice_data(client, rmq_rpc_client):
    report_url = 'test.url'
    rmq_rpc_client.send_proto_message.return_value = invoices_pb2.GetInvoiceUrlOutput(
        result=common_pb2.Url(url=report_url)
    )

    got = await client.get_invoice_url(agency_id=22, invoice_id=1)

    assert got == report_url


async def test_returns_act_data(client, rmq_rpc_client):
    report_url = 'test.url'
    rmq_rpc_client.send_proto_message.return_value = acts_pb2.GetActUrlOutput(
        result=common_pb2.Url(url=report_url)
    )

    got = await client.get_act_url(agency_id=22, act_id=1)

    assert got == report_url


async def test_returns_contract_data(client, rmq_rpc_client):
    report_url = 'test.url'
    rmq_rpc_client.send_proto_message.return_value = contracts_pb2.GetContractUrlOutput(
        result=common_pb2.Url(url=report_url)
    )

    got = await client.get_contract_url(agency_id=22, contract_id=1)

    assert got == report_url


async def test_returns_facture_data(client, rmq_rpc_client):
    report_url = 'test.url'
    rmq_rpc_client.send_proto_message.return_value = invoices_pb2.GetFactureUrlOutput(
        result=common_pb2.Url(url=report_url)
    )

    got = await client.get_facture_url(agency_id=22, facture_id=1)

    assert got == report_url


async def test_returns_agreements_data(client, rmq_rpc_client):
    report_url = 'test.url'
    rmq_rpc_client.send_proto_message.return_value = agreements_pb2.GetAgreementUrlOutput(
        result=common_pb2.Url(url=report_url)
    )

    got = await client.get_agreement_url(agency_id=22, agreement_id=1)

    assert got == report_url
