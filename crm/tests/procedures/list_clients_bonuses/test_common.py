from decimal import Decimal

import pytest
from smb.common.testing_utils import dt

from crm.agency_cabinet.client_bonuses.common.structs import (
    BonusType,
    ClientBonus,
    ClientType,
    ListClientsBonusesInput,
)

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def input_params():
    return dict(
        agency_id=22,
        bonus_type=BonusType.ALL,
        client_type=ClientType.ALL,
        limit=100,
        offset=0,
        datetime_start=dt("2020-03-01 00:00:00"),
        datetime_end=dt("2020-05-01 00:00:00"),
        search_query=None,
    )


@pytest.mark.usefixtures("active_clients", "excluded_clients")
async def test_returns_clients_bonuses(procedure, input_params):
    res = await procedure(params=ListClientsBonusesInput(**input_params))

    assert res == [
        # active client with lots of transactions
        ClientBonus(
            client_id=100,
            email="alpaca1@yandex.ru",
            accrued=Decimal("200.50"),
            spent=Decimal("500"),
            awarded=Decimal("300"),
            active=True,
            currency="RUR",
        ),
        # excluded client
        ClientBonus(
            client_id=200,
            email="capibara2@yandex.ru",
            accrued=Decimal("100.50"),
            spent=Decimal("200.50"),
            awarded=Decimal("200.50"),
            active=False,
            currency="RUR",
        ),
        # client without bonuses to activate
        ClientBonus(
            client_id=300,
            email="alpaca3@yandex.ru",
            accrued=Decimal("100"),
            spent=Decimal("300"),
            awarded=Decimal("0"),
            active=True,
            currency="RUR",
        ),
        # client with no transactions withing period
        ClientBonus(
            client_id=400,
            email="alpaca100@yandex.ru",
            accrued=Decimal("0"),
            spent=Decimal("0"),
            awarded=Decimal("300"),
            active=True,
            currency="RUR",
        ),
        # excluded client
        ClientBonus(
            client_id=500,
            email="capibara5@yandex.ru",
            accrued=Decimal("0"),
            spent=Decimal("0"),
            awarded=Decimal("0"),
            active=False,
            currency="RUR",
        ),
        # active client without gains/spends
        ClientBonus(
            client_id=600,
            email="capibara6@yandex.ru",
            accrued=Decimal("0"),
            spent=Decimal("0"),
            awarded=Decimal("0"),
            active=True,
            currency="RUR",
        ),
    ]


@pytest.mark.usefixtures("active_clients", "excluded_clients")
async def test_sorts_clients_by_id(procedure, input_params):
    res = await procedure(params=ListClientsBonusesInput(**input_params))

    assert [r.client_id for r in res] == [100, 200, 300, 400, 500, 600]


@pytest.mark.usefixtures("active_clients", "excluded_clients")
async def test_does_not_return_another_agency_clients(procedure):
    res = await procedure(
        params=ListClientsBonusesInput(
            agency_id=9865,
            bonus_type=BonusType.ALL,
            client_type=ClientType.ALL,
            limit=100,
            offset=0,
            datetime_start=dt("2020-03-01 00:00:00"),
            datetime_end=dt("2020-05-01 00:00:00"),
            search_query=None,
        )
    )

    assert res == []


async def test_marks_client_active_if_has_connected_programs(
    procedure, input_params, factory
):
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
    await factory.create_client(id_=900, login="alpaca1@yandex.ru")
    await factory.create_client_program(client_id=900, program_id=1)

    res = await procedure(params=ListClientsBonusesInput(**input_params))

    assert len(res) == 1
    assert res[0].active is True


async def test_sums_gains_inside_requested_period_for_active_clients(
    procedure, input_params, active_client, factory
):
    await factory.create_gained_client_bonuses(
        client_id=active_client["id"],
        gained_at=dt("2020-03-22 10:10:10"),
        program_id=1,
        amount=Decimal("50.50"),
        currency="RUR",
    )
    await factory.create_gained_client_bonuses(
        client_id=active_client["id"],
        gained_at=dt("2020-04-23 10:10:10"),
        program_id=1,
        amount=Decimal("150"),
        currency="RUR",
    )
    # outside period, will not be summed
    await factory.create_gained_client_bonuses(
        client_id=active_client["id"],
        gained_at=dt("2020-02-23 10:10:10"),
        program_id=1,
        amount=Decimal("15"),
        currency="RUR",
    )

    res = await procedure(params=ListClientsBonusesInput(**input_params))

    assert len(res) == 1
    assert res[0].accrued == Decimal("200.50")


async def test_returns_zero_as_accrued_for_active_client_if_no_gains_inside_period(
    procedure, input_params, active_client, factory
):
    # outside period
    await factory.create_gained_client_bonuses(
        client_id=active_client["id"],
        gained_at=dt("2020-02-23 10:10:10"),
        program_id=1,
        amount=Decimal("15"),
        currency="RUR",
    )
    await factory.create_spent_client_bonuses(
        client_id=active_client["id"],
        spent_at=dt("2020-04-22 10:10:10"),
        amount=Decimal("300"),
        currency="RUR",
    )
    await factory.create_client_bonuses_to_activate(
        client_id=active_client["id"], amount=Decimal("300")
    )

    res = await procedure(params=ListClientsBonusesInput(**input_params))

    assert len(res) == 1
    assert res[0].accrued == Decimal("0")


async def test_returns_zero_as_accrued_for_active_client_if_no_gains_for_client(
    procedure, input_params, active_client, factory
):
    await factory.create_spent_client_bonuses(
        client_id=active_client["id"],
        spent_at=dt("2020-04-22 10:10:10"),
        amount=Decimal("300"),
        currency="RUR",
    )
    await factory.create_client_bonuses_to_activate(
        client_id=active_client["id"], amount=Decimal("300")
    )

    res = await procedure(params=ListClientsBonusesInput(**input_params))

    assert len(res) == 1
    assert res[0].accrued == Decimal("0")


async def test_sums_spends_inside_requested_period_for_active_clients(
    procedure, input_params, active_client, factory
):
    await factory.create_spent_client_bonuses(
        client_id=active_client["id"],
        spent_at=dt("2020-04-22 10:10:10"),
        amount=Decimal("300"),
        currency="RUR",
    )
    await factory.create_spent_client_bonuses(
        client_id=active_client["id"],
        spent_at=dt("2020-03-22 10:10:10"),
        amount=Decimal("200"),
        currency="RUR",
    )
    # outside period, will not be summed
    await factory.create_spent_client_bonuses(
        client_id=active_client["id"],
        spent_at=dt("2020-02-22 10:10:10"),
        amount=Decimal("100"),
        currency="RUR",
    )

    res = await procedure(params=ListClientsBonusesInput(**input_params))

    assert len(res) == 1
    assert res[0].spent == Decimal("500")


async def test_returns_zero_as_spent_for_active_client_if_no_spends_inside_period(
    procedure, input_params, active_client, factory
):
    # outside period
    await factory.create_spent_client_bonuses(
        client_id=active_client["id"],
        spent_at=dt("2020-02-22 10:10:10"),
        amount=Decimal("100"),
        currency="RUR",
    )
    await factory.create_gained_client_bonuses(
        client_id=active_client["id"],
        gained_at=dt("2020-04-23 10:10:10"),
        program_id=1,
        amount=Decimal("15"),
        currency="RUR",
    )
    await factory.create_client_bonuses_to_activate(
        client_id=active_client["id"], amount=Decimal("300")
    )

    res = await procedure(params=ListClientsBonusesInput(**input_params))

    assert len(res) == 1
    assert res[0].spent == Decimal("0")


async def test_returns_zero_as_spent_for_active_client_if_no_spends_for_client(
    procedure, input_params, active_client, factory
):
    await factory.create_gained_client_bonuses(
        client_id=active_client["id"],
        gained_at=dt("2020-04-23 10:10:10"),
        program_id=1,
        amount=Decimal("15"),
        currency="RUR",
    )
    await factory.create_client_bonuses_to_activate(
        client_id=active_client["id"], amount=Decimal("300")
    )

    res = await procedure(params=ListClientsBonusesInput(**input_params))

    assert len(res) == 1
    assert res[0].spent == Decimal("0")


async def test_returns_bonuses_to_activate_in_awarded_for_active_client(
    procedure, input_params, active_client, factory
):
    await factory.create_client_bonuses_to_activate(
        client_id=active_client["id"], amount=Decimal("300")
    )

    res = await procedure(params=ListClientsBonusesInput(**input_params))

    assert len(res) == 1
    assert res[0].awarded == Decimal("300")


async def test_returns_zero_as_awarded_for_active_client_if_no_bonuses_to_activate(
    procedure, input_params, active_client, factory
):
    await factory.create_spent_client_bonuses(
        client_id=active_client["id"],
        spent_at=dt("2020-03-22 10:10:10"),
        amount=Decimal("100"),
        currency="RUR",
    )
    await factory.create_gained_client_bonuses(
        client_id=active_client["id"],
        gained_at=dt("2020-04-23 10:10:10"),
        program_id=1,
        amount=Decimal("15"),
        currency="RUR",
    )

    res = await procedure(params=ListClientsBonusesInput(**input_params))

    assert len(res) == 1
    assert res[0].awarded == Decimal("0")
