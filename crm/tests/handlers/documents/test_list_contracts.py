from unittest.mock import AsyncMock

import pytest
from crm.agency_cabinet.common.consts import PaymentType, Services
from crm.agency_cabinet.documents.common.structs import Contract, ContractStatus
from crm.agency_cabinet.gateway.server.src.exceptions import AccessDenied
from smb.common.testing_utils import dt

URL = "/api/agencies/123/documents/contracts"


@pytest.fixture(autouse=True)
def procedure(mocker):
    mock = AsyncMock()
    mock.return_value = [
        Contract(
            contract_id=1,
            eid="1234/56",
            status=ContractStatus.valid,
            payment_type=PaymentType.prepayment,
            services=[Services.zen, Services.video],
            signing_date=dt('2021-3-1 00:00:00'),
            finish_date=dt('2022-3-1 00:00:00'),
        ),
        Contract(
            contract_id=2,
            eid="1234/89",
            status=ContractStatus.not_signed,
            payment_type=PaymentType.postpayment,
            services=[Services.zen, Services.video],
            signing_date=None,
            finish_date=dt('2022-3-1 00:00:00'),
        ),
        Contract(
            contract_id=3,
            eid="1234/12",
            status=ContractStatus.not_signed,
            payment_type=None,
            services=[],
            signing_date=None,
            finish_date=dt('2022-3-1 00:00:00'),
        )
    ]

    mocker.patch(
        "crm.agency_cabinet.gateway.server.src.procedures.documents.contracts.ListContracts",
        return_value=mock,
    )
    return mock


async def test_calls_procedure(client, procedure):
    await client.get(URL)

    procedure.assert_called_with(
        yandex_uid=42,
        agency_id=123,
    )


async def test_returns_contracts(client):
    got = await client.get(URL)

    assert got == {
        'items': [
            {
                "contract_id": 1,
                "eid": "1234/56",
                "status": "valid",
                "payment_type": "prepayment",
                "services": ["zen", "video"],
                "signing_date": '2021-03-01T00:00:00+00:00',
                "finish_date": '2022-03-01T00:00:00+00:00',

            },
            {
                "contract_id": 2,
                "eid": "1234/89",
                "status": "not_signed",
                "payment_type": "postpayment",
                "services": ["zen", "video"],
                "signing_date": None,
                "finish_date": '2022-03-01T00:00:00+00:00',
            },
            {
                "contract_id": 3,
                "eid": "1234/12",
                "status": "not_signed",
                "payment_type": None,
                "services": [],
                "signing_date": None,
                "finish_date": '2022-03-01T00:00:00+00:00',
            },
        ]
    }


async def test_returns_403_if_access_denied(client, procedure):
    procedure.side_effect = AccessDenied()

    got = await client.get(URL, expected_status=403)

    assert got == {
        "error": {
            "error_code": "ACCESS_DENIED",
            "http_code": 403,
            "messages": [{"params": {}, "text": "You don't have access"}],
        }
    }
