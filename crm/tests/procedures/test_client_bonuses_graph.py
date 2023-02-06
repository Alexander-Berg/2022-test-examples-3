from decimal import Decimal

import pytest
from smb.common.testing_utils import dt

from crm.agency_cabinet.client_bonuses.common.structs import (
    ClientGraph,
    GraphPoint,
    ProgramBonusesGraph,
)
from crm.agency_cabinet.client_bonuses.server.lib.exceptions import ClientNotFound
from crm.agency_cabinet.client_bonuses.server.lib.procedures import (
    FetchClientBonusesGraph,
)

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def procedure(db):
    return FetchClientBonusesGraph(db)


@pytest.fixture
async def default_client(factory):
    await factory.create_client(id_=42)


@pytest.fixture
async def default_spent(default_client, factory):
    await factory.create_spent_client_bonuses(
        client_id=42,
        spent_at=dt("2021-09-01 00:00:00"),
        amount=Decimal("200.20"),
        currency="RUR"
    )
    await factory.create_spent_client_bonuses(
        client_id=42,
        spent_at=dt("2021-11-01 00:00:00"),
        amount=Decimal("400"),
        currency="RUR"
    )


@pytest.fixture
async def default_to_activate(default_client, factory):
    await factory.create_client_bonuses_to_activate(client_id=42, amount=Decimal(1000))


@pytest.fixture
async def default_gained(default_client, factory):
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
        client_id=42,
        gained_at=dt("2021-10-01 00:00:00"),
        program_id=1,
        amount=Decimal("200"),
        currency="RUR",
    )
    await factory.create_gained_client_bonuses(
        client_id=42,
        gained_at=dt("2021-11-01 00:00:00"),
        program_id=1,
        amount=Decimal("400"),
        currency="RUR",
    )
    await factory.create_gained_client_bonuses(
        client_id=42,
        gained_at=dt("2021-11-01 00:00:00"),
        program_id=2,
        amount=Decimal("200.20"),
        currency="RUR",
    )


async def test_returns_agency_client_graph_data(
    procedure, default_spent, default_gained, default_to_activate
):
    res = await procedure(agency_id=22, client_id=42)

    assert res == ClientGraph(
        bonuses_available=Decimal("1000.000000"),
        overall_spent=[
            GraphPoint(point=dt("2021-09-01 00:00:00"), value=Decimal("200.20")),
            GraphPoint(point=dt("2021-10-01 00:00:00"), value=Decimal("0")),
            GraphPoint(point=dt("2021-11-01 00:00:00"), value=Decimal("400")),
        ],
        overall_accrued=[
            GraphPoint(point=dt("2021-09-01 00:00:00"), value=Decimal("0")),
            GraphPoint(point=dt("2021-10-01 00:00:00"), value=Decimal("200")),
            GraphPoint(point=dt("2021-11-01 00:00:00"), value=Decimal("600.20")),
        ],
        programs=[
            ProgramBonusesGraph(
                program_id=1,
                historical_monthly_data=[
                    GraphPoint(point=dt("2021-10-01 00:00:00"), value=Decimal("200")),
                    GraphPoint(point=dt("2021-11-01 00:00:00"), value=Decimal("400")),
                ],
            ),
            ProgramBonusesGraph(
                program_id=2,
                historical_monthly_data=[
                    GraphPoint(point=dt("2021-11-01 00:00:00"), value=Decimal("200.20"))
                ],
            ),
        ],
    )


async def test_returns_expected_overall_spent_if_were_spends(procedure, default_spent):
    res = await procedure(agency_id=22, client_id=42)

    assert res.overall_spent == [
        GraphPoint(point=dt("2021-09-01 00:00:00"), value=Decimal("200.20")),
        GraphPoint(point=dt("2021-10-01 00:00:00"), value=Decimal("0")),
        GraphPoint(point=dt("2021-11-01 00:00:00"), value=Decimal("400")),
    ]


async def test_returns_empty_overall_spent_if_nothing_spent(
    procedure, default_gained, default_to_activate
):
    res = await procedure(agency_id=22, client_id=42)

    assert res.overall_spent == [
        GraphPoint(point=dt("2021-10-01 00:00:00"), value=Decimal("0")),
        GraphPoint(point=dt("2021-11-01 00:00:00"), value=Decimal("0")),
    ]


async def test_returns_valid_bonuses_available_if_were_to_activate(
    default_to_activate, procedure
):
    res = await procedure(agency_id=22, client_id=42)

    assert res.bonuses_available == Decimal(1000)


async def test_returns_zero_bonuses_if_no_bonuses_available(
    procedure, default_gained, default_spent
):
    res = await procedure(agency_id=22, client_id=42)

    assert res.bonuses_available == Decimal("0")


async def test_returns_overall_accrued_summed_by_date_if_were_gains(
    procedure, default_gained
):
    res = await procedure(agency_id=22, client_id=42)

    assert res.overall_accrued == [
        GraphPoint(point=dt("2021-10-01 00:00:00"), value=Decimal("200")),
        GraphPoint(point=dt("2021-11-01 00:00:00"), value=Decimal("600.20")),
    ]


async def test_returns_empty_overall_accrued_if_no_gains(
    procedure, default_spent, default_to_activate
):
    res = await procedure(agency_id=22, client_id=42)

    assert res.overall_accrued == [
        GraphPoint(point=dt("2021-9-01 00:00:00"), value=Decimal("0")),
        GraphPoint(point=dt("2021-10-01 00:00:00"), value=Decimal("0")),
        GraphPoint(point=dt("2021-11-01 00:00:00"), value=Decimal("0")),
    ]


async def test_returns_expected_programs_if_were_gains(procedure, default_gained):
    res = await procedure(agency_id=22, client_id=42)

    assert res.programs == [
        ProgramBonusesGraph(
            program_id=1,
            historical_monthly_data=[
                GraphPoint(point=dt("2021-10-01 00:00:00"), value=Decimal("200")),
                GraphPoint(point=dt("2021-11-01 00:00:00"), value=Decimal("400")),
            ],
        ),
        ProgramBonusesGraph(
            program_id=2,
            historical_monthly_data=[
                GraphPoint(point=dt("2021-11-01 00:00:00"), value=Decimal("200.20"))
            ],
        ),
    ]


async def test_returns_empty_programs_if_no_gains(
    procedure, default_spent, default_to_activate
):
    res = await procedure(agency_id=22, client_id=42)

    assert res.programs == []


async def test_returns_default_values_if_no_client_transactions_info(
    procedure, factory, default_spent, default_gained, default_to_activate
):
    await factory.create_client(id_=404)

    res = await procedure(agency_id=22, client_id=404)

    assert res == ClientGraph(
        bonuses_available=Decimal(0),
        overall_spent=[],
        overall_accrued=[],
        programs=[],
    )


@pytest.mark.parametrize(
    "client",
    [
        # mismatch by agency_id
        dict(client_id=111, agency_id=987),
        # mismatch by client_id
        dict(client_id=987, agency_id=222),
    ],
)
async def test_raises_if_client_not_found(client, procedure, factory):
    await factory.create_client(id_=111, agency_id=222)

    with pytest.raises(ClientNotFound):
        await procedure(**client)
