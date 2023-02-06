from decimal import Decimal

import pytest
from smb.common.testing_utils import dt

from crm.agency_cabinet.client_bonuses.common.structs import (
    BonusAmount,
    BonusDetails,
    BonusDetailsList,
    BonusStatusType,
    FetchBonusesDetailsInput,
)
from crm.agency_cabinet.client_bonuses.server.lib.exceptions import ClientNotFound
from crm.agency_cabinet.client_bonuses.server.lib.procedures import FetchBonusesDetails

pytestmark = [pytest.mark.asyncio]


@pytest.fixture
def procedure(db):
    return FetchBonusesDetails(db)


@pytest.fixture
async def client_id(factory):
    client = await factory.create_client(id_=321)

    return client["id"]


@pytest.fixture
async def create_spends(client_id, factory):
    await factory.create_spent_client_bonuses(
        client_id=client_id,
        spent_at=dt("2020-06-02 18:00:00"),
        amount=Decimal("500"),
        currency="RUR"
    )
    await factory.create_spent_client_bonuses(
        client_id=client_id,
        spent_at=dt("2020-07-03 18:00:00"),
        amount=Decimal("600"),
        currency="RUR"
    )


@pytest.fixture
async def create_gains(client_id, factory):
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
        client_id=client_id,
        gained_at=dt("2020-04-01 18:00:00"),
        program_id=1,
        amount=Decimal("100"),
        currency="RUR",
    )
    await factory.create_gained_client_bonuses(
        client_id=client_id,
        gained_at=dt("2020-05-01 18:00:00"),
        program_id=1,
        amount=Decimal("100.20"),
        currency="RUR",
    )
    await factory.create_gained_client_bonuses(
        client_id=client_id,
        gained_at=dt("2020-05-01 18:00:00"),
        program_id=2,
        amount=Decimal("10.20"),
        currency="RUR",
    )
    await factory.create_gained_client_bonuses(
        client_id=client_id,
        gained_at=dt("2020-06-02 18:00:00"),
        program_id=2,
        amount=Decimal("20.20"),
        currency="RUR",
    )


@pytest.mark.parametrize(
    "period, expected",
    [
        (
            # no events inside period
            dict(
                datetime_start=dt("2019-02-01 00:00:00"),
                datetime_end=dt("2020-02-28 00:00:00"),
            ),
            [],
        ),
        (
            # some events inside period
            dict(
                datetime_start=dt("2020-06-01 00:00:00"),
                datetime_end=dt("2020-06-28 00:00:00"),
            ),
            [
                BonusDetails(
                    type=BonusStatusType.accrued,
                    amounts=[BonusAmount(program_id=2, amount=Decimal("20.20"))],
                    total=Decimal("20.20"),
                    date=dt("2020-06-01 00:00:00"),
                ),
                BonusDetails(
                    type=BonusStatusType.spent,
                    amounts=[],
                    total=Decimal("500"),
                    date=dt("2020-06-01 00:00:00"),
                ),
            ],
        ),
        (
            # all events inside period
            dict(
                datetime_start=dt("2020-02-01 00:00:00"),
                datetime_end=dt("2020-09-01 00:00:00"),
            ),
            [
                BonusDetails(
                    type=BonusStatusType.spent,
                    amounts=[],
                    total=Decimal("600"),
                    date=dt("2020-07-01 00:00:00"),
                ),
                BonusDetails(
                    type=BonusStatusType.accrued,
                    amounts=[BonusAmount(program_id=2, amount=Decimal("20.20"))],
                    total=Decimal("20.20"),
                    date=dt("2020-06-01 00:00:00"),
                ),
                BonusDetails(
                    type=BonusStatusType.spent,
                    amounts=[],
                    total=Decimal("500"),
                    date=dt("2020-06-01 00:00:00"),
                ),
                BonusDetails(
                    type=BonusStatusType.accrued,
                    amounts=[
                        BonusAmount(program_id=1, amount=Decimal("100.20")),
                        BonusAmount(program_id=2, amount=Decimal("10.20")),
                    ],
                    total=Decimal("110.40"),
                    date=dt("2020-05-01 00:00:00"),
                ),
                BonusDetails(
                    type=BonusStatusType.accrued,
                    amounts=[
                        BonusAmount(program_id=1, amount=Decimal("100")),
                    ],
                    total=Decimal("100"),
                    date=dt("2020-04-01 00:00:00"),
                ),
            ],
        ),
    ],
)
async def test_returns_some_data_withing_selected_period(
    period, expected, procedure, create_gains, create_spends
):
    result = await procedure(
        params=FetchBonusesDetailsInput(agency_id=22, client_id=321, **period),
    )

    assert result == BonusDetailsList(items=expected)


async def test_returns_data_correctly_if_only_spends(procedure, create_spends):
    result = await procedure(
        params=FetchBonusesDetailsInput(
            agency_id=22,
            client_id=321,
            datetime_start=dt("2020-02-01 00:00:00"),
            datetime_end=dt("2020-09-01 00:00:00"),
        ),
    )

    assert result == BonusDetailsList(
        items=[
            BonusDetails(
                type=BonusStatusType.spent,
                amounts=[],
                total=Decimal("600"),
                date=dt("2020-07-01 00:00:00"),
            ),
            BonusDetails(
                type=BonusStatusType.spent,
                amounts=[],
                total=Decimal("500"),
                date=dt("2020-06-01 00:00:00"),
            ),
        ],
    )


async def test_returns_data_correctly_if_only_gains(procedure, create_gains):
    result = await procedure(
        params=FetchBonusesDetailsInput(
            agency_id=22,
            client_id=321,
            datetime_start=dt("2020-02-01 00:00:00"),
            datetime_end=dt("2020-09-01 00:00:00"),
        ),
    )

    assert result == BonusDetailsList(
        items=[
            BonusDetails(
                type=BonusStatusType.accrued,
                amounts=[BonusAmount(program_id=2, amount=Decimal("20.20"))],
                total=Decimal("20.20"),
                date=dt("2020-06-01 00:00:00"),
            ),
            BonusDetails(
                type=BonusStatusType.accrued,
                amounts=[
                    BonusAmount(program_id=1, amount=Decimal("100.20")),
                    BonusAmount(program_id=2, amount=Decimal("10.20")),
                ],
                total=Decimal("110.40"),
                date=dt("2020-05-01 00:00:00"),
            ),
            BonusDetails(
                type=BonusStatusType.accrued,
                amounts=[
                    BonusAmount(program_id=1, amount=Decimal("100")),
                ],
                total=Decimal("100"),
                date=dt("2020-04-01 00:00:00"),
            ),
        ],
    )


async def test_returns_empty_list_if_nothing_for_client(procedure, factory):
    await factory.create_client(id_=111, agency_id=222)

    result = await procedure(
        params=FetchBonusesDetailsInput(
            client_id=111,
            agency_id=222,
            datetime_start=dt("2020-05-01 18:00:00"),
            datetime_end=dt("2020-08-01 18:00:00"),
        ),
    )

    assert result == BonusDetailsList(items=[])


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
        await procedure(
            params=FetchBonusesDetailsInput(
                datetime_start=dt("2020-05-01 18:00:00"),
                datetime_end=dt("2020-08-01 18:00:00"),
                **client,
            )
        )
