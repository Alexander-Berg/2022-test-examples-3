from smb.common.testing_utils import dt

import pytest

from crm.agency_cabinet.client_bonuses.common.structs import ReportInfo, ClientType
from crm.agency_cabinet.gateway.server.src.exceptions import AccessDenied
from crm.agency_cabinet.gateway.server.src.procedures.bonuses import ListBonusesReports
from crm.agency_cabinet.grants.common.structs import AccessLevel
from crm.agency_cabinet.common.consts import ReportsStatuses


@pytest.fixture
def procedure(service_discovery):
    return ListBonusesReports(service_discovery)


@pytest.fixture
def input_params():
    return dict(agency_id=22)


async def test_returns_bonuses_list_if_access_allowed(
    procedure, input_params, service_discovery
):
    reports = [
        ReportInfo(
            id=1,
            name='Отчет по бонусам 1',
            status=ReportsStatuses.ready.value,
            period_from=dt('2021-3-1 00:00:00'),
            period_to=dt('2021-6-1 00:00:00'),
            created_at=dt('2021-7-1 00:00:00'),
            client_type=ClientType.ALL,
        ),
        ReportInfo(
            id=2,
            name='Отчет по бонусам 2',
            status=ReportsStatuses.in_progress.value,
            period_from=dt('2021-3-1 00:00:00'),
            period_to=dt('2021-6-1 00:00:00'),
            created_at=dt('2021-7-1 00:00:00'),
            client_type=ClientType.ALL,
        ),
    ]

    service_discovery.grants.check_access_level.return_value = AccessLevel.ALLOW
    service_discovery.client_bonuses.list_bonuses_reports.return_value = reports

    got = await procedure(yandex_uid=123, **input_params)

    assert got == reports


async def test_calls_other_services_for_info(procedure, input_params, service_discovery):
    service_discovery.grants.check_access_level.return_value = AccessLevel.ALLOW

    await procedure(yandex_uid=1, **input_params)

    service_discovery.grants.check_access_level.assert_called_with(
        yandex_uid=1, agency_id=22
    )
    service_discovery.client_bonuses.list_bonuses_reports.assert_called_with(
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

    service_discovery.client_bonuses.list_bonuses_reports.assert_not_called()
