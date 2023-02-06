import pytest

from datetime import datetime, timezone

from crm.agency_cabinet.client_bonuses.server.lib.celery.tasks.load_bonuses_data.synchronizers import (  # noqa: E501
    ClientsTableSynchronizer,
)

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def synchronizer(con):
    return ClientsTableSynchronizer(con)


async def test_inserts_data_into_db(factory, synchronizer):
    await synchronizer.process_data(
        [
            (1, "login1", 11, True, "2021-11-11 00:00:00"),
            (3, "login3", 22, True, "2021-11-11 00:00:00"),
        ]
    )

    assert await factory.list_clients() == [
        {
            "id": 1,
            "login": "login1",
            "agency_id": 11,
            "is_active": True,
            "create_date": datetime(2021, 11, 11, 0, 0, 0, 0, tzinfo=timezone.utc),
        },
        {
            "id": 3,
            "login": "login3",
            "agency_id": 22,
            "is_active": True,
            "create_date": datetime(2021, 11, 11, 0, 0, 0, tzinfo=timezone.utc),
        },
    ]


async def test_ignores_existing_values(factory, synchronizer):
    await factory.create_client(
        id_=13,
        login="login13",
        agency_id=11,
        is_active=True,
        create_date=datetime(2021, 11, 11, 0, 0, 0, tzinfo=timezone.utc)
    )
    await factory.create_client(
        id_=14,
        login="login14",
        agency_id=44,
        is_active=True,
        create_date=datetime(2021, 11, 11, 0, 0, 0, tzinfo=timezone.utc)
    )

    await synchronizer.process_data(
        [
            (1, "login1", 11, True, "2021-11-11 00:00:00"),
            (3, "login3", 33, True, "2021-11-11 00:00:00"),
        ]
    )

    assert await factory.list_clients() == [
        {
            "id": 1,
            "login": "login1",
            "agency_id": 11,
            "is_active": True,
            "create_date": datetime(2021, 11, 11, 0, 0, 0, tzinfo=timezone.utc),
        },
        {
            "id": 3,
            "login": "login3",
            "agency_id": 33,
            "is_active": True,
            "create_date": datetime(2021, 11, 11, 0, 0, 0, tzinfo=timezone.utc),
        },
        {
            "id": 13,
            "login": "login13",
            "agency_id": 11,
            "is_active": True,
            "create_date": datetime(2021, 11, 11, 0, 0, 0, tzinfo=timezone.utc),
        },
        {
            "id": 14,
            "login": "login14",
            "agency_id": 44,
            "is_active": True,
            "create_date": datetime(2021, 11, 11, 0, 0, 0, tzinfo=timezone.utc),
        },
    ]


async def test_ignores_duplicate_values(factory, synchronizer):
    await factory.create_client(
        id_=1,
        login="login0",
        agency_id=22,
        is_active=True,
        create_date=datetime(2021, 11, 11, 0, 0, 0, tzinfo=timezone.utc),
    )

    await synchronizer.process_data(
        [
            (1, "login1", 11, True, "2021-11-11 00:00:00"),
            (3, "login3", 33, True, "2021-11-11 00:00:00"),
        ]
    )

    assert await factory.list_clients() == [
        {
            "id": 1,
            "login": "login0",
            "agency_id": 22,
            "is_active": True,
            "create_date": datetime(2021, 11, 11, 0, 0, 0, tzinfo=timezone.utc),
        },
        {
            "id": 3,
            "login": "login3",
            "agency_id": 33,
            "is_active": True,
            "create_date": datetime(2021, 11, 11, 0, 0, 0, tzinfo=timezone.utc),
        },
    ]
