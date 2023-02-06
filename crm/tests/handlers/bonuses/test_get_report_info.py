import pytest

from smb.common.testing_utils import dt

from crm.agency_cabinet.common.consts.report import ReportsStatuses
from crm.agency_cabinet.common.testing import BaseTestClient
from crm.agency_cabinet.common.service_discovery import ServiceDiscovery
from crm.agency_cabinet.grants.common import structs as grants_structs
from crm.agency_cabinet.client_bonuses.common import structs as bonuses_structs


URL = '/api/agencies/{agency_id}/bonuses/reports/{report_id}'


@pytest.mark.parametrize(
    (
        'grants_return_value',
        'reports_return_value'
    ),
    [
        (
            grants_structs.AccessLevel.ALLOW,
            bonuses_structs.ReportInfo(
                id=1,
                name='test',
                created_at=dt('2021-11-11 00:00:00'),
                period_from=dt('2020-11-01 00:00:00'),
                period_to=dt('2020-11-11 00:00:00'),
                status=ReportsStatuses.requested.value,
                client_type=bonuses_structs.ClientType.EXCLUDED,
            )
        ),
    ]
)
async def test_get_report_url(
    client: BaseTestClient,
    service_discovery: ServiceDiscovery,
    yandex_uid: int,
    grants_return_value,
    reports_return_value
):
    service_discovery.grants.check_access_level.return_value = grants_return_value
    service_discovery.client_bonuses.get_detailed_report_info.return_value = reports_return_value

    got = await client.get(URL.format(agency_id=1, report_id=1), expected_status=200)

    assert got == {
        'id': 1,
        'name': 'test',
        'created_at': "2021-11-11T00:00:00+00:00",
        'period_from': "2020-11-01T00:00:00+00:00",
        'period_to': "2020-11-11T00:00:00+00:00",
        'status': ReportsStatuses.requested.value,
        'client_type': 'EXCLUDED',
    }
