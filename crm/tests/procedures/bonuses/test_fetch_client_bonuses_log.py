from datetime import datetime, timezone
from decimal import Decimal

import pytest

from crm.agency_cabinet.client_bonuses.client import ClientNotFound
from crm.agency_cabinet.client_bonuses.common.structs import (
    BonusAmount,
    BonusDetails,
    BonusStatusType,
)
from crm.agency_cabinet.gateway.server.src.exceptions import AccessDenied, NotFound
from crm.agency_cabinet.gateway.server.src.procedures.bonuses import (
    FetchClientBonusesDetails,
)
from crm.agency_cabinet.grants.common.structs import AccessLevel


@pytest.fixture
def procedure(service_discovery):
    return FetchClientBonusesDetails(service_discovery)


@pytest.fixture
def input_params():
    return dict(
        agency_id=62342,
        client_id=11111,
        datetime_start=datetime(2021, 1, 5, 14, 48, 00, tzinfo=timezone.utc),
        datetime_end=datetime(2021, 5, 1, 18, 00, 00, tzinfo=timezone.utc),
    )


@pytest.mark.parametrize(
    "bonuses_details",
    [
        [],
        [
            BonusDetails(
                type=BonusStatusType.spent,
                amounts=[],
                total=Decimal("500"),
                date=datetime(2021, 3, 1, 00, 00, 00, tzinfo=timezone.utc),
            ),
            BonusDetails(
                type=BonusStatusType.accrued,
                amounts=[
                    BonusAmount(program_id=1, amount=Decimal("100.20")),
                    BonusAmount(program_id=2, amount=Decimal("10.20")),
                ],
                total=Decimal("200"),
                date=datetime(2021, 2, 1, 00, 00, 00, tzinfo=timezone.utc),
            ),
        ],
    ],
)
async def test_returns_history_log_if_access_allowed(
    bonuses_details, procedure, input_params, service_discovery
):
    service_discovery.grants.check_access_level.return_value = AccessLevel.ALLOW
    service_discovery.client_bonuses.fetch_bonuses_details.return_value = (
        bonuses_details
    )

    got = await procedure(yandex_uid=123, **input_params)

    assert got == bonuses_details


async def test_calls_other_services_for_info(
    procedure, input_params, service_discovery
):
    service_discovery.grants.check_access_level.return_value = AccessLevel.ALLOW

    await procedure(yandex_uid=123, **input_params)

    service_discovery.grants.check_access_level.assert_called_with(
        yandex_uid=123, agency_id=62342
    )
    service_discovery.client_bonuses.fetch_bonuses_details.assert_called_with(
        **input_params
    )


async def test_raises_if_access_denied(procedure, input_params, service_discovery):
    service_discovery.grants.check_access_level.return_value = AccessLevel.DENY

    with pytest.raises(AccessDenied):
        await procedure(yandex_uid=123, **input_params)


async def test_does_not_call_clients_bonuses_if_access_denied(
    procedure, input_params, service_discovery
):
    service_discovery.grants.check_access_level.return_value = AccessLevel.DENY

    with pytest.raises(AccessDenied):
        await procedure(yandex_uid=123, **input_params)

    service_discovery.client_bonuses.fetch_bonuses_details.assert_not_called()


async def test_raises_if_client_not_found(procedure, input_params, service_discovery):
    service_discovery.grants.check_access_level.return_value = AccessLevel.ALLOW
    service_discovery.client_bonuses.fetch_bonuses_details.side_effect = ClientNotFound(
        agency_id=62342, client_id=11111
    )

    with pytest.raises(NotFound):
        await procedure(yandex_uid=123, **input_params)
