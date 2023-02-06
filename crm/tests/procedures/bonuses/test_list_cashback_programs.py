import pytest

from crm.agency_cabinet.client_bonuses.common.structs import CashbackProgram
from crm.agency_cabinet.gateway.server.src.exceptions import AccessDenied
from crm.agency_cabinet.gateway.server.src.procedures.bonuses import ListCashbackPrograms
from crm.agency_cabinet.grants.common.structs import AccessLevel


@pytest.fixture
def procedure(service_discovery):
    return ListCashbackPrograms(service_discovery)


@pytest.fixture
def input_params():
    return dict(agency_id=22)


async def test_returns_cashback_programs_if_access_allowed(
    procedure, input_params, service_discovery
):
    programs = [
        CashbackProgram(
            id=1,
            category_id=1,
            is_general=True,
            is_enabled=True,
            name_ru='Программа 1',
            name_en='Program 1',
            description_ru='Программа кэшбека #1',
            description_en='Cashback program #1',
        ),
        CashbackProgram(
            id=2,
            category_id=2,
            is_general=True,
            is_enabled=False,
            name_ru='Программа 2',
            name_en='Program 2',
            description_ru='Программа кэшбека #2',
            description_en='Cashback program #2',
        ),
        CashbackProgram(
            id=3,
            category_id=1,
            is_general=False,
            is_enabled=False,
            name_ru='Программа 1',
            name_en='Program 1',
            description_ru='Программа кэшбека #1',
            description_en='Cashback program #1',
        ),
        CashbackProgram(
            id=4,
            category_id=3,
            is_general=False,
            is_enabled=True,
            name_ru='Программа 4',
            name_en='Program 4',
            description_ru='Программа кэшбека #4',
            description_en='Cashback program #4',
        ),
    ]

    service_discovery.grants.check_access_level.return_value = AccessLevel.ALLOW
    service_discovery.client_bonuses.list_cashback_programs.return_value = programs

    got = await procedure(yandex_uid=123, **input_params)

    assert got == programs


async def test_calls_other_services_for_info(
    procedure, input_params, service_discovery
):
    service_discovery.grants.check_access_level.return_value = AccessLevel.ALLOW

    await procedure(yandex_uid=1, **input_params)

    service_discovery.grants.check_access_level.assert_called_with(
        yandex_uid=1, agency_id=22
    )
    service_discovery.client_bonuses.list_cashback_programs.assert_called_with(22)


async def test_raises_if_access_denied(procedure, input_params, service_discovery):
    service_discovery.grants.check_access_level.return_value = AccessLevel.DENY

    with pytest.raises(AccessDenied):
        await procedure(yandex_uid=1, **input_params)


async def test_does_not_call_cashback_programs_if_access_denied(
    procedure, input_params, service_discovery
):
    service_discovery.grants.check_access_level.return_value = AccessLevel.DENY

    with pytest.raises(AccessDenied):
        await procedure(yandex_uid=1, **input_params)

    service_discovery.client_bonuses.list_cashback_programs.assert_not_called()
