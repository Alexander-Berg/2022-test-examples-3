import pytest
import datetime

from crm.agency_cabinet.gateway.server.src.procedures.ord.reports import CreateReport
from crm.agency_cabinet.grants.common.structs import AccessLevel
from crm.agency_cabinet.ord.common import consts as ord_consts, structs as ord_structs

REPORT = ord_structs.ReportInfo(
    report_id=1,
    status=ord_consts.ReportStatuses.draft,
    reporter_type=ord_consts.ReporterType.partner,
    clients_count=1,
    campaigns_count=1,
    period_from=datetime.datetime(2022, 1, 20, tzinfo=datetime.timezone.utc),
    sending_date=datetime.datetime(2022, 1, 20, tzinfo=datetime.timezone.utc),
    settings=ord_structs.ReportSettings(
        name='direct',
        display_name='Директ',
        allow_create_ad_distributor_acts=False,
        allow_create_clients=False,
        allow_create_campaigns=False,
        allow_edit_report=False,
    ),
)


@pytest.fixture
def procedure(service_discovery):
    return CreateReport(service_discovery)


@pytest.fixture
def input_params():
    return dict(
        agency_id=22,
        period_from=datetime.datetime(2022, 1, 20, tzinfo=datetime.timezone.utc),
        reporter_type=ord_consts.ReporterType.partner.value
    )


async def test_creates_report(
    procedure, input_params, service_discovery
):
    service_discovery.grants.check_access_level.return_value = AccessLevel.ALLOW
    service_discovery.ord.create_report.return_value = REPORT

    got = await procedure(yandex_uid=123, **input_params)

    assert got == REPORT
