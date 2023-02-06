from crm.agency_cabinet.common.testing import BaseTestClient
from crm.agency_cabinet.common.service_discovery import ServiceDiscovery
from crm.agency_cabinet.grants.common import structs as grants_structs
from crm.agency_cabinet.ord.common import structs as ord_structs


URL_POST = '/api/agencies/1/ord/reports/1/export'
URL_GET = '/api/agencies/1/ord/reports/1/export/1'

RETURN_VALUE = ord_structs.ReportExportResponse(
    report_export_id=1,
    status='requested'
)


async def test_report_export(
    client: BaseTestClient,
    service_discovery: ServiceDiscovery,
    yandex_uid: int
):
    service_discovery.grants.check_access_level.return_value = grants_structs.AccessLevel.ALLOW
    service_discovery.ord.report_export.return_value = RETURN_VALUE

    got = await client.post(URL_POST, json={})

    assert got == {'report_export_id': 1, 'status': 'requested'}


async def test_report_export_no_json(
    client: BaseTestClient,
    service_discovery: ServiceDiscovery,
    yandex_uid: int
):
    service_discovery.grants.check_access_level.return_value = grants_structs.AccessLevel.ALLOW
    service_discovery.ord.report_export.return_value = RETURN_VALUE

    got = await client.post(URL_POST)

    assert got == {'error': {'error_code': 'BAD_REQUEST',
                             'http_code': 400,
                             'messages': [{'params': {},
                                           'text': 'Incorrect mime-type: '
                                           'application/octet-stream'}]}}


async def test_report_export_info(
    client: BaseTestClient,
    service_discovery: ServiceDiscovery,
    yandex_uid: int
):
    service_discovery.grants.check_access_level.return_value = grants_structs.AccessLevel.ALLOW
    service_discovery.ord.get_report_export_info.return_value = RETURN_VALUE

    got = await client.get(URL_GET)

    assert got == {'report_export_id': 1, 'status': 'requested'}
