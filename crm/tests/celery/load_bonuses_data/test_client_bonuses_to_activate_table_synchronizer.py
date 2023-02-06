from decimal import Decimal

import pytest
from smb.common.testing_utils import Any

from crm.agency_cabinet.client_bonuses.server.lib.celery.tasks.load_bonuses_data.synchronizers import (  # noqa: E501
    ClientBonusesToActivateTableSynchronizer,
)

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def synchronizer(con):
    return ClientBonusesToActivateTableSynchronizer(con)


@pytest.fixture(autouse=True)
async def initial_data(factory):
    for client_id in (1, 2, 3):
        await factory.create_client(id_=client_id)


async def test_inserts_data_into_db(factory, synchronizer):
    await synchronizer.process_data(
        [
            (1, Decimal("11.22")),
            (2, Decimal("33.44")),
        ]
    )

    assert await factory.list_client_bonuses_to_activate() == [
        {
            "id": Any(int),
            "client_id": 1,
            "amount": Decimal("11.22"),
        },
        {
            "id": Any(int),
            "client_id": 2,
            "amount": Decimal("33.44"),
        },
    ]


async def test_removes_existing_values(factory, synchronizer):
    await factory.create_client_bonuses_to_activate(
        client_id=1, amount=Decimal("444.55")
    )
    await factory.create_client_bonuses_to_activate(
        client_id=2, amount=Decimal("555.66")
    )

    await synchronizer.process_data(
        [
            (1, Decimal("11.22")),
        ]
    )

    assert await factory.list_client_bonuses_to_activate() == [
        {
            "id": Any(int),
            "client_id": 1,
            "amount": Decimal("11.22"),
        },
    ]
