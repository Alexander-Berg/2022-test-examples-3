from decimal import Decimal

import pytest
from smb.common.testing_utils import Any, dt

from crm.agency_cabinet.client_bonuses.server.lib.celery.tasks.load_bonuses_data.synchronizers import (  # noqa: E501
    SpentClientBonusesTableSynchronizer,
)

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def synchronizer(con):
    return SpentClientBonusesTableSynchronizer(con, spent_at=dt("2021-05-01 00:00:00"))


@pytest.fixture(autouse=True)
async def initial_data(factory):
    for client_id in (1, 2, 3):
        await factory.create_client(id_=client_id)


async def test_inserts_data_into_db(factory, synchronizer):
    await synchronizer.process_data(
        [
            (1, Decimal("11.22"), "RUR"),
            (2, Decimal("33.44"), "RUR"),
        ]
    )

    assert await factory.list_spent_client_bonuses() == [
        {
            "id": Any(int),
            "client_id": 1,
            "amount": Decimal("11.22"),
            "spent_at": dt("2021-05-01 00:00:00"),
            "currency": "RUR",
        },
        {
            "id": Any(int),
            "client_id": 2,
            "amount": Decimal("33.44"),
            "spent_at": dt("2021-05-01 00:00:00"),
            "currency": "RUR",
        },
    ]


async def test_removes_existing_values_for_same_date(factory, synchronizer):
    await factory.create_spent_client_bonuses(
        client_id=1,
        spent_at=dt("2021-05-01 00:00:00"),
        currency="RUR"
    )
    await factory.create_spent_client_bonuses(
        client_id=2,
        spent_at=dt("2021-05-01 00:00:00"),
        currency="RUR"
    )

    await synchronizer.process_data(
        [
            (1, Decimal("11.22"), "RUR"),
        ]
    )

    assert await factory.list_spent_client_bonuses() == [
        {
            "id": Any(int),
            "client_id": 1,
            "amount": Decimal("11.22"),
            "spent_at": dt("2021-05-01 00:00:00"),
            "currency": "RUR",
        },
    ]


async def test_does_not_remove_exiting_values_for_other_dates(factory, synchronizer):
    await factory.create_spent_client_bonuses(
        client_id=1,
        spent_at=dt("2021-04-01 00:00:00"),
        currency="RUR"
    )
    await factory.create_spent_client_bonuses(
        client_id=2,
        spent_at=dt("2021-04-01 00:00:00"),
        currency="RUR"
    )

    await synchronizer.process_data(
        [
            (1, Decimal("11.22"), "RUR"),
        ]
    )

    assert await factory.list_spent_client_bonuses() == [
        {
            "id": Any(int),
            "client_id": 1,
            "amount": Decimal("1234.56"),
            "spent_at": dt("2021-04-01 00:00:00"),
            "currency": "RUR",
        },
        {
            "id": Any(int),
            "client_id": 1,
            "amount": Decimal("11.22"),
            "spent_at": dt("2021-05-01 00:00:00"),
            "currency": "RUR",
        },
        {
            "id": Any(int),
            "client_id": 2,
            "amount": Decimal("1234.56"),
            "spent_at": dt("2021-04-01 00:00:00"),
            "currency": "RUR",
        },
    ]
