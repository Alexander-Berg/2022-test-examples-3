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
    "client_type, bonus_type, expected_ids",
    [
        (ClientType.ALL, BonusType.ALL, [100, 200, 300, 400, 500, 600]),
        (ClientType.ACTIVE, BonusType.ALL, [100, 300, 400, 600]),
        (ClientType.ALL, BonusType.WITH_ACTIVATION_OVER_PERIOD, [100, 300]),
        (ClientType.EXCLUDED, BonusType.ALL, [200, 500]),
        (ClientType.ALL, BonusType.WITH_SPENDS_OVER_PERIOD, [100, 300]),
    ],
)
async def test_returns_clients_as_requested(client_type, bonus_type, expected_ids, procedure):
    res = await procedure(
        params=ListClientsBonusesInput(
            agency_id=22,
            client_type=client_type,
            bonus_type=bonus_type,
            limit=100,
            offset=0,
            datetime_start=dt("2020-03-01 00:00:00"),
            datetime_end=dt("2020-05-01 00:00:00"),
            search_query=None,
        )
    )

    assert [r.client_id for r in res] == expected_ids


@pytest.mark.parametrize(
    "pagination, expected_ids",
    [
        # all clients fit pagination
        (dict(limit=10, offset=0), [100, 200, 300, 400, 500, 600]),
        # some clients fit pagination
        (dict(limit=2, offset=2), [300, 400]),
        # all clients outside offset, so no clients in result
        (dict(limit=10, offset=10), []),
    ],
)
async def test_respects_pagination_for_all_clients(pagination, expected_ids, procedure):
    res = await procedure(
        params=ListClientsBonusesInput(
            agency_id=22,
            client_type=ClientType.ALL,
            bonus_type=BonusType.ALL,
            datetime_start=dt("2020-03-01 00:00:00"),
            datetime_end=dt("2020-05-01 00:00:00"),
            search_query=None,
            **pagination,
        )
    )

    assert [r.client_id for r in res] == expected_ids


@pytest.mark.parametrize(
    "pagination, expected_ids",
    [
        # all clients fit pagination
        (dict(limit=10, offset=0), [100, 300, 400, 600]),
        # some clients fit pagination
        (dict(limit=1, offset=1), [300]),
        # all clients outside offset, so no clients in result
        (dict(limit=10, offset=10), []),
    ],
)
async def test_respects_pagination_for_active_clients(
    pagination, expected_ids, procedure
):
    res = await procedure(
        params=ListClientsBonusesInput(
            agency_id=22,
            client_type=ClientType.ACTIVE,
            bonus_type=BonusType.ALL,
            datetime_start=dt("2020-03-01 00:00:00"),
            datetime_end=dt("2020-05-01 00:00:00"),
            search_query=None,
            **pagination,
        )
    )

    assert [r.client_id for r in res] == expected_ids


@pytest.mark.parametrize(
    "pagination, expected_ids",
    [
        # all clients fit pagination
        (dict(limit=10, offset=0), [100, 300]),
        # some clients fit pagination
        (dict(limit=1, offset=1), [300]),
        # all clients outside offset, so no clients in result
        (dict(limit=10, offset=10), []),
    ],
)
async def test_respects_pagination_for_clients_with_activations(
    pagination, expected_ids, procedure
):
    res = await procedure(
        params=ListClientsBonusesInput(
            agency_id=22,
            client_type=ClientType.ALL,
            bonus_type=BonusType.WITH_ACTIVATION_OVER_PERIOD,
            datetime_start=dt("2020-03-01 00:00:00"),
            datetime_end=dt("2020-05-01 00:00:00"),
            search_query=None,
            **pagination,
        )
    )

    assert [r.client_id for r in res] == expected_ids


@pytest.mark.parametrize(
    "pagination, expected_ids",
    [
        # all clients fit pagination
        (dict(limit=10, offset=0), [200, 500]),
        # some clients fit pagination
        (dict(limit=1, offset=1), [500]),
        # all clients outside offset, so no clients in result
        (dict(limit=10, offset=10), []),
    ],
)
async def test_respects_pagination_for_excluded_clients(
    pagination, expected_ids, procedure
):
    res = await procedure(
        params=ListClientsBonusesInput(
            agency_id=22,
            client_type=ClientType.EXCLUDED,
            bonus_type=BonusType.ALL,
            datetime_start=dt("2020-03-01 00:00:00"),
            datetime_end=dt("2020-05-01 00:00:00"),
            search_query=None,
            **pagination,
        )
    )

    assert [r.client_id for r in res] == expected_ids
