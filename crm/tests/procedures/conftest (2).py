import pytest
from crm.agency_cabinet.documents.common.structs import ContractStatus, InvoiceStatus
from crm.agency_cabinet.common.consts import PaymentType, Services
from crm.agency_cabinet.documents.server.src.db.models import Contract, Invoice, Payment, Act, Facture, Agreement
from smb.common.testing_utils import dt


# TODO: better data

@pytest.fixture
async def ya_doc_response():
    return {
        "totalElements": 0,
        "totalPages": 0,
        "size": 0,
        "content": [
            {
                "contracts": [
                    {
                        "documents": [
                            {
                                "doc_number": "string",
                                "doc_date": "2022-04-19",
                                "doc_type": "ACT",
                                "doc_id": 0,
                                "reversed_flag": True,
                                "edo_enabled_flag": True,
                                "amount": 0,
                                "currency_code": "string",
                                "delivery_statuses": [
                                    {
                                        "channel": "EMAIL",
                                        "sent_date": "2022-04-19T09:40:51.204Z"
                                    }
                                ],
                                "bill_number": "string"
                            }
                        ],
                        "contract_number": "string",
                        "contract_id": 0,
                        "indv_documents_flag": True,
                        "tax_rate": 0
                    }
                ],
                "documents": [
                    {
                        "doc_number": "string",
                        "doc_date": "2022-04-19",
                        "doc_type": "ACT",
                        "doc_id": 0,
                        "reversed_flag": True,
                        "edo_enabled_flag": True,
                        "amount": 0,
                        "currency_code": "string",
                        "delivery_statuses": [
                            {
                                "channel": "EMAIL",
                                "sent_date": "2022-04-19T09:40:51.204Z"
                            }
                        ],
                        "bill_number": "string"
                    }
                ],
                "party_name": "string",
                "party_id": 0,
                "inn": "string",
                "delivery_type": "string",
                "delivery_type_desc": "string",
                "edo_flag": True,
                "edo_start_date": "2022-04-19",
                "edo_operator_code": "string",
                "organization": "string",
                "email": "string"
            }
        ],
        "number": 0,
        "sort": {
            "unsorted": True,
            "sorted": True,
            "empty": True
        },
        "pageable": {
            "offset": 0,
            "sort": {
                "unsorted": True,
                "sorted": True,
                "empty": True
            },
            "paged": True,
            "unpaged": True,
            "pageNumber": 0,
            "pageSize": 0
        },
        "numberOfElements": 0,
        "first": True,
        "last": True,
        "empty": True
    }


@pytest.fixture
async def fixture_contracts():
    rows = [
        {
            'eid': 'test9',
            'inn': 'inn',
            'status': ContractStatus.valid.value,
            'agency_id': 123,
            'payment_type': PaymentType.prepayment.value,
            'services': [Services.direct.value],
            'signing_date': dt('2021-3-1 00:00:00'),
            'finish_date': dt('2022-3-1 00:00:00'),
            'credit_limit': 66.6,
        },
        {
            'eid': 'test10',
            'inn': 'inn',
            'status': ContractStatus.not_signed.value,
            'agency_id': 123,
            'payment_type': PaymentType.prepayment.value,
            'services': [Services.zen.value],
            'signing_date': dt('2021-5-1 00:00:00'),
            'finish_date': dt('2022-5-1 00:00:00'),
            'credit_limit': 88.8,
        },
        {
            'eid': 'test11',
            'inn': 'inn',
            'status': ContractStatus.valid.value,
            'agency_id': 321,
            'payment_type': PaymentType.prepayment.value,
            'services': [Services.zen.value],
            'signing_date': dt('2021-3-1 00:00:00'),
            'finish_date': dt('2022-3-1 00:00:00'),
            'credit_limit': 66.6,
        },
        {
            'eid': 'test12',
            'inn': 'inn',
            'status': ContractStatus.valid.value,
            'agency_id': 123,
            'payment_type': None,
            'services': [],
            'signing_date': dt('2021-2-1 00:00:00'),
            'finish_date': dt('2022-2-1 00:00:00'),
            'credit_limit': 99.9,
        },
    ]
    yield await Contract.bulk_insert(rows)

    await Contract.delete.gino.status()


@pytest.fixture
async def fixture_invoices(fixture_contracts):
    rows = [
        {
            'contract_id': fixture_contracts[0].id,
            'eid': 'invoice_eid',
            'amount': 50,
            'currency': 'RUR',
            'status': InvoiceStatus.paid.value,
            'date': dt('2021-3-1 00:00:00'),
            'payment_date': dt('2022-4-1 00:00:00'),
        },
        {
            'contract_id': fixture_contracts[0].id,
            'eid': 'invoice_eid2',
            'amount': 150,
            'currency': 'KZT',
            'status': InvoiceStatus.not_paid.value,
            'date': dt('2021-3-1 00:00:00'),
            'payment_date': None,
        },
    ]

    yield await Invoice.bulk_insert(rows)

    await Invoice.delete.gino.status()


@pytest.fixture
async def fixture_factures(fixture_invoices, fixture_acts):
    rows = [
        {
            'invoice_id': fixture_invoices[0].id,
            'amount': 50,
            'amount_with_nds': 60,
            'nds': 10,
            'currency': 'RUB',
            'act_id': fixture_acts[0].id,
            'date': dt('2022-3-1 00:00:00')
        }
    ]

    yield await Facture.bulk_insert(rows)

    await Facture.delete.gino.status()


@pytest.fixture
async def fixture_payments(fixture_invoices):
    rows = [
        {
            'invoice_id': fixture_invoices[0].id,
            'eid': 'eid',
            'amount': fixture_invoices[0].amount,
            'currency': fixture_invoices[0].currency,
            'date': dt('2021-4-1 00:00:00'),
        },
        {
            'invoice_id': fixture_invoices[0].id,
            'eid': 'eid2',
            'amount': fixture_invoices[0].amount,
            'currency': fixture_invoices[0].currency,
            'date': dt('2023-4-1 00:00:00'),
        }

    ]

    yield await Payment.bulk_insert(rows)

    await Payment.delete.gino.status()


@pytest.fixture
async def fixture_acts(fixture_invoices, fixture_contracts):
    rows = [
        {
            'invoice_id': fixture_invoices[1].id,
            'eid': 'act_eid1',
            'amount': fixture_invoices[1].amount,
            'currency': fixture_invoices[1].currency,
            'date': dt('2021-4-1 00:00:00'),
        },
        {
            'invoice_id': fixture_invoices[1].id,
            'eid': 'act_eid2',
            'amount': fixture_invoices[1].amount,
            'currency': fixture_invoices[1].currency,
            'date': dt('2023-4-1 00:00:00'),
        }

    ]

    yield await Act.bulk_insert(rows)

    await Act.delete.gino.status()


@pytest.fixture
async def fixture_agreements(fixture_contracts):
    rows = [
        {
            'name': 'Дополнительное соглашение',
            'contract_id': fixture_contracts[2].id,
            'got_scan': True,
            'got_original': True,
            'date': dt('2024-4-1 00:00:00')
        },
        {
            'name': 'Дополнительное соглашение 2',
            'contract_id': fixture_contracts[2].id,
            'got_scan': False,
            'got_original': False,
            'date': dt('2023-4-1 00:00:00')
        }
    ]
    yield await Agreement.bulk_insert(rows)

    await Agreement.delete.gino.status()
