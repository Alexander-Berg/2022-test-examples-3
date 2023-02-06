import pytest

from crm.agency_cabinet.client_bonuses.common.structs import DeleteReportOutput
from crm.agency_cabinet.gateway.server.src.exceptions import AccessDenied, NotFound
from crm.agency_cabinet.gateway.server.src.procedures.bonuses import DeleteReport
from crm.agency_cabinet.grants.common.structs import AccessLevel


@pytest.fixture
def procedure(service_discovery):
    return DeleteReport(service_discovery)


@pytest.fixture
def input_params():
    return dict(agency_id=22, report_id=1)


async def test_deletes_report_if_access_allowed(procedure, input_params, service_discovery):
    deleted_report = DeleteReportOutput(is_deleted=True)

    service_discovery.grants.check_access_level.return_value = AccessLevel.ALLOW
    service_discovery.client_bonuses.delete_report.return_value = deleted_report

    got = await procedure(yandex_uid=123, **input_params)

    assert got == deleted_report


async def test_calls_other_services_for_info(procedure, input_params, service_discovery):
    service_discovery.grants.check_access_level.return_value = AccessLevel.ALLOW

    await procedure(yandex_uid=1, **input_params)

    service_discovery.grants.check_access_level.assert_called_with(
        yandex_uid=1, agency_id=22
    )
    service_discovery.client_bonuses.delete_report.assert_called_with(
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

    service_discovery.client_bonuses.delete_report.assert_not_called()


async def test_raises_if_report_not_found(procedure, input_params, service_discovery):
    service_discovery.grants.check_access_level.return_value = AccessLevel.ALLOW
    service_discovery.client_bonuses.delete_report.side_effect = NotFound('Can\'t find report with id 1')

    with pytest.raises(NotFound):
        await procedure(yandex_uid=1, **input_params)


async def test_raises_if_agency_not_suitable(procedure, input_params, service_discovery):
    service_discovery.grants.check_access_level.return_value = AccessLevel.ALLOW
    service_discovery.client_bonuses.delete_report.side_effect = NotFound('Wrong agency id 22 for report with id 1')

    with pytest.raises(NotFound):
        await procedure(yandex_uid=1, **input_params)
