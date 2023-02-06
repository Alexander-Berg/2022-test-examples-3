import pytest
from smb.common.testing_utils import dt

from crm.agency_cabinet.client_bonuses.common.structs import (
    BonusType,
    ClientType,
    ListClientsBonusesInput,
)

pytestmark = [
    pytest.mark.asyncio,
    pytest.mark.usefixtures("active_clients", "excluded_clients"),
]


@pytest.mark.parametrize(
    "search_query, expected_ids",
    [
        # match by email
        ("capibara", [200, 500, 600]),
        # match by id
        ("300", [300]),
        # match by email and id
        ("100", [100, 400]),
    ],
)
async def test_respects_search_query(search_query, expected_ids, procedure):
    res = await procedure(
        params=ListClientsBonusesInput(
            agency_id=22,
            client_type=ClientType.ALL,
            bonus_type=BonusType.ALL,
            limit=100,
            offset=0,
            datetime_start=dt("2020-03-01 00:00:00"),
            datetime_end=dt("2020-05-01 00:00:00"),
            search_query=search_query,
        )
    )

    assert [r.client_id for r in res] == expected_ids


@pytest.mark.parametrize(
    "pagination, expected_ids",
    [
        # all clients fit pagination
        (dict(limit=10, offset=0), [100, 300, 400]),
        # some clients fit pagination
        (dict(limit=1, offset=1), [300]),
        # all clients outside offset, so no clients in result
        (dict(limit=10, offset=10), []),
    ],
)
async def test_respects_pagination_when_filters_by_search_query(
    pagination, expected_ids, procedure
):
    res = await procedure(
        params=ListClientsBonusesInput(
            agency_id=22,
            client_type=ClientType.ALL,
            bonus_type=BonusType.ALL,
            datetime_start=dt("2020-03-01 00:00:00"),
            datetime_end=dt("2020-05-01 00:00:00"),
            search_query="alpaca",
            **pagination,
        )
    )

    assert [r.client_id for r in res] == expected_ids


@pytest.mark.parametrize(
    "client_type, bonus_type, expected_ids",
    [
        (ClientType.ALL, BonusType.ALL, [100, 300, 400]),
        (ClientType.ACTIVE, BonusType.ALL, [100, 300, 400]),
        (ClientType.ALL, BonusType.WITH_ACTIVATION_OVER_PERIOD, [100, 300]),
        (ClientType.EXCLUDED, BonusType.ALL, []),
    ],
)
async def test_respects_filer_by_client_status_when_filters_by_search_query(
    client_type, bonus_type, expected_ids, procedure
):
    res = await procedure(
        params=ListClientsBonusesInput(
            agency_id=22,
            client_type=client_type,
            bonus_type=bonus_type,
            limit=100,
            offset=0,
            datetime_start=dt("2020-03-01 00:00:00"),
            datetime_end=dt("2020-05-01 00:00:00"),
            search_query="alpaca",
        )
    )

    assert [r.client_id for r in res] == expected_ids
