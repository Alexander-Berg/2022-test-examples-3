import pytest

from crm.agency_cabinet.client_bonuses.common.structs import ClientBonusSettings
from crm.agency_cabinet.gateway.server.src.exceptions import AccessDenied
from crm.agency_cabinet.gateway.server.src.procedures.bonuses import GetBonusesSettings
from crm.agency_cabinet.grants.common.structs import AccessLevel
from smb.common.testing_utils import dt


@pytest.fixture
def procedure(service_discovery):
    return GetBonusesSettings(service_discovery)


@pytest.fixture
def input_params():
    return dict(
        agency_id=22
    )


async def test_returns_bonus_settings_if_access_allowed(procedure, input_params, service_discovery):
    bonuses_settings = ClientBonusSettings(
        first_date=dt("2020-11-11 10:10:10", as_proto=True),
        last_date=dt("2021-11-11 10:10:10", as_proto=True)
        )

    service_discovery.grants.check_access_level.return_value = AccessLevel.ALLOW
    service_discovery.client_bonuses.get_clients_bonuses_settings.return_value = bonuses_settings

    got = await procedure(yandex_uid=123, **input_params)

    assert got == bonuses_settings


async def test_returns_empty_bonus_settings(procedure, input_params, service_discovery):
    bonuses_settings = ClientBonusSettings(
        first_date=None,
        last_date=None
        )

    service_discovery.grants.check_access_level.return_value = AccessLevel.ALLOW
    service_discovery.client_bonuses.get_clients_bonuses_settings.return_value = bonuses_settings

    got = await procedure(yandex_uid=123, **input_params)

    assert got == bonuses_settings


async def test_calls_other_services_for_info(procedure, input_params, service_discovery):
    service_discovery.grants.check_access_level.return_value = AccessLevel.ALLOW

    await procedure(yandex_uid=1, **input_params)

    service_discovery.grants.check_access_level.assert_called_with(
        yandex_uid=1, agency_id=22
    )
    service_discovery.client_bonuses.get_clients_bonuses_settings.assert_called_with(
        **input_params
    )


async def test_raises_if_access_denied(procedure, input_params, service_discovery):
    service_discovery.grants.check_access_level.return_value = AccessLevel.DENY

    with pytest.raises(AccessDenied):
        await procedure(yandex_uid=1, **input_params)


async def test_does_not_call_clients_bonuses_settings_if_access_denied(
    procedure, input_params, service_discovery
):
    service_discovery.grants.check_access_level.return_value = AccessLevel.DENY

    with pytest.raises(AccessDenied):
        await procedure(yandex_uid=1, **input_params)

    service_discovery.client_bonuses.get_clients_bonuses_settings.assert_not_called()
