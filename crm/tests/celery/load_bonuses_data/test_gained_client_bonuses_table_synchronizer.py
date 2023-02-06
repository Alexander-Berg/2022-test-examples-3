from decimal import Decimal

import pytest
from smb.common.testing_utils import Any, dt

from crm.agency_cabinet.client_bonuses.server.lib.celery.tasks.load_bonuses_data.synchronizers import (  # noqa: E501
    GainedClientBonusesTableSynchronizer,
)

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def synchronizer(con):
    return GainedClientBonusesTableSynchronizer(
        con, gained_at=dt("2021-05-01 00:00:00")
    )


@pytest.fixture
def synchronizer_corrections(con):
    return GainedClientBonusesTableSynchronizer(
        con, gained_at=dt("2021-05-01 00:00:00"), program_ids=[14]
    )


@pytest.fixture
def synchronizer_market(con):
    return GainedClientBonusesTableSynchronizer(
        con, gained_at=dt("2021-05-01 00:00:00"), program_ids=[7, 13]
    )


@pytest.fixture(autouse=True)
async def initial_data(factory):
    for client_id in (1, 2, 3):
        await factory.create_client(id_=client_id)


async def test_inserts_data_into_db(factory, synchronizer):
    await factory.create_cashback_program(
        id=1,
        category_id=1,
        is_general=True,
        is_enabled=True,
        name_ru="Программа 1",
        name_en="Program 1",
        description_ru="Программа кэшбека #1",
        description_en="Cashback program #1",
    )
    await factory.create_cashback_program(
        id=2,
        category_id=2,
        is_general=True,
        is_enabled=True,
        name_ru="Программа 2",
        name_en="Program 2",
        description_ru="Программа кэшбека #2",
        description_en="Cashback program #2",
    )
    await synchronizer.process_data(
        [
            (1, 1, Decimal("11.22"), "RUR"),
            (1, 2, Decimal("22.33"), "RUR"),
            (2, 2, Decimal("33.44"), "RUR"),
        ]
    )

    assert await factory.list_gained_client_bonuses() == [
        {
            "id": Any(int),
            "client_id": 1,
            "program_id": 1,
            "amount": Decimal("11.22"),
            "gained_at": dt("2021-05-01 00:00:00"),
            "currency": "RUR",
        },
        {
            "id": Any(int),
            "client_id": 1,
            "program_id": 2,
            "amount": Decimal("22.33"),
            "gained_at": dt("2021-05-01 00:00:00"),
            "currency": "RUR",
        },
        {
            "id": Any(int),
            "client_id": 2,
            "program_id": 2,
            "amount": Decimal("33.44"),
            "gained_at": dt("2021-05-01 00:00:00"),
            "currency": "RUR",
        },
    ]


async def test_removes_existing_values_for_same_date(factory, synchronizer):
    await factory.create_cashback_program(
        id=1,
        category_id=1,
        is_general=True,
        is_enabled=True,
        name_ru="Программа 1",
        name_en="Program 1",
        description_ru="Программа кэшбека #1",
        description_en="Cashback program #1",
    )
    await factory.create_cashback_program(
        id=2,
        category_id=2,
        is_general=True,
        is_enabled=True,
        name_ru="Программа 2",
        name_en="Program 2",
        description_ru="Программа кэшбека #2",
        description_en="Cashback program #2",
    )
    await factory.create_cashback_program(
        id=14,
        category_id=14,
        is_general=True,
        is_enabled=True,
        name_ru="Программа 14",
        name_en="Program 14",
        description_ru="Программа кэшбека #14",
        description_en="Cashback program #14",
    )
    await factory.create_gained_client_bonuses(
        client_id=1,
        program_id=1,
        gained_at=dt("2021-05-01 00:00:00"),
        currency="RUR",
    )
    await factory.create_gained_client_bonuses(
        client_id=1,
        program_id=2,
        gained_at=dt("2021-05-01 00:00:00"),
        currency="RUR",
    )
    await factory.create_gained_client_bonuses(
        client_id=2,
        program_id=1,
        gained_at=dt("2021-05-01 00:00:00"),
        currency="RUR",
    )
    await factory.create_gained_client_bonuses(
        client_id=2,
        program_id=14,
        gained_at=dt("2021-05-01 00:00:00"),
        currency="RUR",
    )

    await synchronizer.process_data(
        [
            (1, 1, Decimal("11.22"), "RUR"),
            (1, 2, Decimal("22.33"), "RUR"),
        ]
    )

    assert await factory.list_gained_client_bonuses() == [
        {
            "id": Any(int),
            "client_id": 1,
            "program_id": 1,
            "amount": Decimal("11.22"),
            "gained_at": dt("2021-05-01 00:00:00"),
            "currency": "RUR",
        },
        {
            "id": Any(int),
            "client_id": 1,
            "program_id": 2,
            "amount": Decimal("22.33"),
            "gained_at": dt("2021-05-01 00:00:00"),
            "currency": "RUR",
        },
    ]


async def test_removes_existing_values_for_corrections(factory, synchronizer_corrections):
    await factory.create_cashback_program(
        id=1,
        category_id=1,
        is_general=True,
        is_enabled=True,
        name_ru="Программа 1",
        name_en="Program 1",
        description_ru="Программа кэшбека #1",
        description_en="Cashback program #1",
    )
    await factory.create_cashback_program(
        id=14,
        category_id=14,
        is_general=True,
        is_enabled=True,
        name_ru="Программа 14",
        name_en="Program 14",
        description_ru="Программа кэшбека #14",
        description_en="Cashback program #14",
    )
    await factory.create_gained_client_bonuses(
        client_id=2,
        program_id=14,
        gained_at=dt("2021-05-01 00:00:00"),
        currency="RUR",
    )

    await synchronizer_corrections.process_data(
        [
            (1, 14, Decimal("11.22"), "RUR"),
            (1, 14, Decimal("22.33"), "RUR"),
        ]
    )

    assert await factory.list_gained_client_bonuses() == [
        {
            "id": Any(int),
            "client_id": 1,
            "program_id": 14,
            "amount": Decimal("11.22"),
            "gained_at": dt("2021-05-01 00:00:00"),
            "currency": "RUR",
        },
        {
            "id": Any(int),
            "client_id": 1,
            "program_id": 14,
            "amount": Decimal("22.33"),
            "gained_at": dt("2021-05-01 00:00:00"),
            "currency": "RUR",
        },
    ]


async def test_does_not_remove_exiting_values_for_other_dates(factory, synchronizer):
    await factory.create_cashback_program(
        id=1,
        category_id=1,
        is_general=True,
        is_enabled=True,
        name_ru="Программа 1",
        name_en="Program 1",
        description_ru="Программа кэшбека #1",
        description_en="Cashback program #1",
    )
    await factory.create_cashback_program(
        id=2,
        category_id=2,
        is_general=True,
        is_enabled=True,
        name_ru="Программа 2",
        name_en="Program 2",
        description_ru="Программа кэшбека #2",
        description_en="Cashback program #2",
    )
    await factory.create_gained_client_bonuses(
        client_id=1,
        program_id=1,
        gained_at=dt("2021-04-01 00:00:00"),
        currency="RUR",
    )
    await factory.create_gained_client_bonuses(
        client_id=1,
        program_id=2,
        gained_at=dt("2021-04-01 00:00:00"),
        currency="RUR",
    )
    await factory.create_gained_client_bonuses(
        client_id=2,
        program_id=1,
        gained_at=dt("2021-04-01 00:00:00"),
        currency="RUR",
    )

    await synchronizer.process_data(
        [
            (1, 1, Decimal("11.22"), "RUR"),
            (1, 2, Decimal("22.33"), "RUR"),
        ]
    )

    assert await factory.list_gained_client_bonuses() == [
        {
            "id": Any(int),
            "client_id": 1,
            "program_id": 1,
            "amount": Decimal("1234.56"),
            "gained_at": dt("2021-04-01 00:00:00"),
            "currency": "RUR",
        },
        {
            "id": Any(int),
            "client_id": 1,
            "program_id": 2,
            "amount": Decimal("1234.56"),
            "gained_at": dt("2021-04-01 00:00:00"),
            "currency": "RUR",
        },
        {
            "id": Any(int),
            "client_id": 1,
            "program_id": 1,
            "amount": Decimal("11.22"),
            "gained_at": dt("2021-05-01 00:00:00"),
            "currency": "RUR",
        },
        {
            "id": Any(int),
            "client_id": 1,
            "program_id": 2,
            "amount": Decimal("22.33"),
            "gained_at": dt("2021-05-01 00:00:00"),
            "currency": "RUR",
        },
        {
            "id": Any(int),
            "client_id": 2,
            "program_id": 1,
            "amount": Decimal("1234.56"),
            "gained_at": dt("2021-04-01 00:00:00"),
            "currency": "RUR",
        },
    ]


async def test_removes_existing_values_for_market(factory, synchronizer_market):
    await factory.create_cashback_program(
        id=7,
        category_id=7,
        is_general=True,
        is_enabled=True,
        name_ru="Программа 1",
        name_en="Program 1",
        description_ru="Программа кэшбека #1",
        description_en="Cashback program #1",
    )
    await factory.create_cashback_program(
        id=13,
        category_id=13,
        is_general=True,
        is_enabled=True,
        name_ru="Программа 14",
        name_en="Program 14",
        description_ru="Программа кэшбека #14",
        description_en="Cashback program #14",
    )
    await factory.create_gained_client_bonuses(
        client_id=2,
        program_id=13,
        gained_at=dt("2021-05-01 00:00:00"),
        currency="RUR",
    )
    await factory.create_gained_client_bonuses(
        client_id=2,
        program_id=7,
        gained_at=dt("2021-05-01 00:00:00"),
        currency="RUR",
    )

    await synchronizer_market.process_data(
        [
            (1, 13, Decimal("11.22"), "RUR"),
            (1, 13, Decimal("22.33"), "RUR"),
        ]
    )

    assert await factory.list_gained_client_bonuses() == [
        {
            "id": Any(int),
            "client_id": 1,
            "program_id": 13,
            "amount": Decimal("11.22"),
            "gained_at": dt("2021-05-01 00:00:00"),
            "currency": "RUR",
        },
        {
            "id": Any(int),
            "client_id": 1,
            "program_id": 13,
            "amount": Decimal("22.33"),
            "gained_at": dt("2021-05-01 00:00:00"),
            "currency": "RUR",
        },
    ]
