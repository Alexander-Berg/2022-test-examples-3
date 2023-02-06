from smb.common.testing_utils import dt

import pytest

from crm.agency_cabinet.client_bonuses.common.structs import ReportInfo, ClientType
from crm.agency_cabinet.gateway.server.src.exceptions import AccessDenied
from crm.agency_cabinet.gateway.server.src.procedures.bonuses import CreateReport
from crm.agency_cabinet.grants.common.structs import AccessLevel
from crm.agency_cabinet.common.consts import ReportsStatuses


@pytest.fixture
def procedure(service_discovery):
    return CreateReport(service_discovery)


@pytest.fixture
def input_params():
    return dict(agency_id=22)


report_json = {
    'name': 'Отчет по бонусам 1',
    'period_from': '2021-03-01T00:00:00.000Z',
    'period_to': '2021-06-01T00:00:00.000Z',
    'client_type': "ALL",
}


async def test_creates_report_if_access_allowed(procedure, input_params, service_discovery):
    created_report = ReportInfo(
        id=1,
        name='Отчет по бонусам 1',
        status=ReportsStatuses.requested.value,
        period_from=dt('2021-3-1 00:00:00'),
        period_to=dt('2021-6-1 00:00:00'),
        created_at=dt('2021-7-1 00:00:00'),
        client_type=ClientType.ALL,
    )

    service_discovery.grants.check_access_level.return_value = AccessLevel.ALLOW
    service_discovery.client_bonuses.create_report.return_value = created_report

    got = await procedure(yandex_uid=123, **input_params, report_json=report_json)

    assert got == created_report


async def test_calls_other_services_for_info(procedure, input_params, service_discovery):
    service_discovery.grants.check_access_level.return_value = AccessLevel.ALLOW

    await procedure(yandex_uid=1, **input_params, report_json=report_json)

    service_discovery.grants.check_access_level.assert_called_with(
        yandex_uid=1, agency_id=22
    )
    service_discovery.client_bonuses.create_report.assert_called_with(
        **input_params, **report_json
    )


async def test_raises_if_access_denied(procedure, input_params, service_discovery):
    service_discovery.grants.check_access_level.return_value = AccessLevel.DENY

    with pytest.raises(AccessDenied):
        await procedure(yandex_uid=1, **input_params, report_json=report_json)


async def test_does_not_call_clients_bonuses_if_access_denied(
    procedure, input_params, service_discovery
):
    service_discovery.grants.check_access_level.return_value = AccessLevel.DENY

    with pytest.raises(AccessDenied):
        await procedure(yandex_uid=1, **input_params, report_json=report_json)

    service_discovery.client_bonuses.create_report.assert_not_called()
