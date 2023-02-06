from decimal import Decimal

import pytest

from crm.agency_cabinet.client_bonuses.common.structs import BonusType, ClientBonus, ClientType
from crm.agency_cabinet.gateway.server.src.exceptions import AccessDenied
from crm.agency_cabinet.gateway.server.src.procedures.bonuses import ListBonuses
from crm.agency_cabinet.grants.common.structs import AccessLevel
from smb.common.testing_utils import dt


@pytest.fixture
def procedure(service_discovery):
    return ListBonuses(service_discovery)


@pytest.fixture
def input_params():
    return dict(
        agency_id=22,
        client_type=ClientType.ALL,
        bonus_type=BonusType.ALL,
        datetime_start=dt("2020-08-21 10:10:10"),
        datetime_end=dt("2020-08-23 10:10:10"),
        limit=100,
        offset=0,
        search_query="Something",
    )


async def test_returns_bonuses_list_if_access_allowed(
    procedure, input_params, service_discovery
):
    bonuses = [
        ClientBonus(
            client_id=1,
            email="alpaca@yandex.ru",
            accrued=Decimal("50.5"),
            spent=Decimal("20"),
            awarded=Decimal("200"),
            active=True,
            currency="RUR",
        ),
        ClientBonus(
            client_id=2,
            email="capibara@yandex.ru",
            accrued=None,
            spent=None,
            awarded=None,
            active=False,
            currency="RUR",
        ),
    ]

    service_discovery.grants.check_access_level.return_value = AccessLevel.ALLOW
    service_discovery.client_bonuses.list_clients_bonuses.return_value = bonuses

    got = await procedure(yandex_uid=123, **input_params)

    assert got == bonuses


async def test_calls_other_services_for_info(
    procedure, input_params, service_discovery
):
    service_discovery.grants.check_access_level.return_value = AccessLevel.ALLOW

    await procedure(yandex_uid=1, **input_params)

    service_discovery.grants.check_access_level.assert_called_with(
        yandex_uid=1, agency_id=22
    )
    service_discovery.client_bonuses.list_clients_bonuses.assert_called_with(
        **input_params
    )


async def test_raises_if_access_denied(procedure, input_params, service_discovery):
    service_discovery.grants.check_access_level.return_value = AccessLevel.DENY

    with pytest.raises(AccessDenied):
        await procedure(yandex_uid=1, **input_params)


async def test_does_not_call_clients_bonuses_if_access_denied(
    procedure, input_params, service_discovery
):
    service_discovery.grants.check_access_level.return_value = AccessLevel.DENY

    with pytest.raises(AccessDenied):
        await procedure(yandex_uid=1, **input_params)

    service_discovery.client_bonuses.list_clients_bonuses.assert_not_called()
