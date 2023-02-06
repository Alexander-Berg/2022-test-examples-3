from datetime import datetime
from decimal import Decimal

import pytest
from smb.common.testing_utils import dt

from crm.agency_cabinet.client_bonuses.server.lib.procedures import ListClientsBonuses


@pytest.fixture
def procedure(db):
    return ListClientsBonuses(db)


@pytest.fixture
async def active_client(factory):
    client = await factory.create_client(id_=900, login="alpaca1@yandex.ru")
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
    await factory.create_client_program(client_id=client["id"], program_id=1)

    return client


@pytest.fixture
async def excluded_client(factory):
    return await factory.create_client(id_=900, login="alpaca1@yandex.ru")


@pytest.fixture
async def active_clients(factory):
    # with gains and spends inside period, bonuses
    # many transactions
    await factory.create_client(
        id_=100,
        login="alpaca1@yandex.ru",
        create_date=datetime(year=2021, month=6, day=1)
    )
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
    await factory.create_client_program(client_id=100, program_id=1)
    await factory.create_gained_client_bonuses(
        client_id=100,
        gained_at=dt("2020-03-22 10:10:10"),
        program_id=1,
        amount=Decimal("50.50"),
        currency="RUR",
    )
    await factory.create_gained_client_bonuses(
        client_id=100,
        gained_at=dt("2020-04-23 10:10:10"),
        program_id=1,
        amount=Decimal("150"),
        currency="RUR",
    )
    await factory.create_gained_client_bonuses(
        client_id=100,
        gained_at=dt("2020-02-23 10:10:10"),
        program_id=1,
        amount=Decimal("15"),
        currency="RUR",
    )
    await factory.create_spent_client_bonuses(
        client_id=100, spent_at=dt("2020-03-10 10:10:10"), amount=Decimal("200"), currency="RUR"
    )
    await factory.create_spent_client_bonuses(
        client_id=100, spent_at=dt("2020-05-22 10:10:10"), amount=Decimal("400"), currency="RUR"
    )
    await factory.create_spent_client_bonuses(
        client_id=100, spent_at=dt("2020-04-22 10:10:10"), amount=Decimal("300"), currency="RUR"
    )
    await factory.create_client_bonuses_to_activate(
        client_id=100, amount=Decimal("300")
    )

    # spends and gains inside period, no bonuses
    await factory.create_client(
        id_=300,
        login="alpaca3@yandex.ru",
        create_date=datetime(year=2021, month=4, day=1)
    )
    await factory.create_client_program(client_id=300, program_id=1)
    await factory.create_gained_client_bonuses(
        client_id=300,
        gained_at=dt("2020-03-22 10:10:10"),
        program_id=1,
        amount=Decimal("100"),
        currency="RUR",
    )
    await factory.create_spent_client_bonuses(
        client_id=300, spent_at=dt("2020-03-10 10:10:10"), amount=Decimal("300"), currency="RUR"
    )

    # spends and gains outside period, has bonuses
    await factory.create_client(
        id_=400,
        login="alpaca100@yandex.ru",
        create_date=datetime(year=2021, month=3, day=1)
    )
    await factory.create_client_program(client_id=400, program_id=2)
    await factory.create_gained_client_bonuses(
        client_id=400,
        gained_at=dt("2020-02-22 10:10:10"),
        program_id=2,
        amount=Decimal("100"),
        currency="RUR",
    )
    await factory.create_spent_client_bonuses(
        client_id=400, spent_at=dt("2020-05-10 10:10:10"), amount=Decimal("300"), currency="RUR"
    )
    await factory.create_client_bonuses_to_activate(
        client_id=400, amount=Decimal("300")
    )

    # no spends/gains
    await factory.create_client(
        id_=600,
        login="capibara6@yandex.ru",
        create_date=datetime(year=2021, month=1, day=1)
    )
    await factory.create_client_program(client_id=600, program_id=1)


@pytest.fixture
async def excluded_clients(factory):
    # excluded client with data inside period
    await factory.create_client(
        id_=200,
        login="capibara2@yandex.ru",
        is_active=False,
        create_date=datetime(year=2021, month=5, day=1)
    )
    await factory.create_gained_client_bonuses(
        client_id=200,
        gained_at=dt("2020-03-21 10:10:10"),
        program_id=1,
        amount=Decimal("100.50"),
        currency="RUR",
    )
    await factory.create_spent_client_bonuses(
        client_id=200, spent_at=dt("2020-03-22 10:10:10"), amount=Decimal("200.50"), currency="RUR"
    )
    await factory.create_client_bonuses_to_activate(
        client_id=200, amount=Decimal("200.50")
    )

    # excluded client without data inside period and no bonuses
    await factory.create_client(
        id_=500,
        login="capibara5@yandex.ru",
        is_active=False,
        create_date=datetime(year=2021, month=2, day=1)
    )
    await factory.create_gained_client_bonuses(
        client_id=500,
        gained_at=dt("2020-05-21 10:10:10"),
        program_id=2,
        amount=Decimal("100.50"),
        currency="RUR",
    )
    await factory.create_spent_client_bonuses(
        client_id=500, spent_at=dt("2020-02-22 10:10:10"), amount=Decimal("200.50"), currency="RUR"
    )
