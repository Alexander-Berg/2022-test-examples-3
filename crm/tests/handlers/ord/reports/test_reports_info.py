import datetime

import pytest

from crm.agency_cabinet.common.testing import BaseTestClient
from crm.agency_cabinet.common.service_discovery import ServiceDiscovery
from crm.agency_cabinet.grants.common import structs as grants_structs
from crm.agency_cabinet.ord.common import consts as ord_consts, structs as ord_structs

URL = '/api/agencies/{agency_id}/ord/reports'


@pytest.mark.parametrize(('grants_return_value',
                          'reports_return_value',
                          'expected'),
                         [(grants_structs.AccessLevel.ALLOW,
                           ord_structs.ReportInfo(
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
                           ),
                           {
                               'report_id': 1,
                               'status': 'draft',
                               'reporter_type': 'partner',
                               'period_from': '2022-01-20',
                               'sending_date': '2022-01-20',
                               'clients_count': 1,
                               'campaigns_count': 1,
                               'settings': {
                                   'name': 'direct',
                                   'display_name': 'Директ',
                                   'allow_create_ad_distributor_acts': False,
                                   'allow_create_clients': False,
                                   'allow_create_campaigns': False,
                                   'allow_edit_report': False,
                               }
                           }),
                          ])
async def test_detailed_report_info(client: BaseTestClient, service_discovery: ServiceDiscovery, yandex_uid: int,
                                    grants_return_value, reports_return_value, expected):
    service_discovery.grants.check_access_level.return_value = grants_return_value
    service_discovery.ord.get_detailed_report_info.return_value = reports_return_value
    got = await client.get(URL.format(agency_id=1) + '/1', expected_status=200)

    assert got == expected


@pytest.mark.parametrize(('grants_return_value',
                          'reports_return_value',
                          'expected'),
                         [(grants_structs.AccessLevel.ALLOW,
                           [
                               ord_structs.ReportInfo(
                                   report_id=1,
                                   status=ord_structs.ReportStatuses.draft,
                                   reporter_type=ord_consts.ReporterType.partner,
                                   clients_count=1,
                                   campaigns_count=1,
                                   period_from=datetime.datetime(2021, 1, 1, tzinfo=datetime.timezone.utc),
                                   settings=ord_structs.ReportSettings(
                                       name='direct',
                                       display_name='Директ',
                                       allow_create_ad_distributor_acts=False,
                                       allow_create_clients=False,
                                       allow_create_campaigns=False,
                                       allow_edit_report=False,
                                   ),
                               )
                           ],
                           {
                               'size': 1,
                               'items':
                                   [
                                       {
                                           'report_id': 1,
                                           'sending_date': None,
                                           'status': 'draft',
                                           'reporter_type': 'partner',
                                           'period_from': '2021-01-01',
                                           'clients_count': 1,
                                           'campaigns_count': 1,
                                           'settings': {
                                               'name': 'direct',
                                               'display_name': 'Директ',
                                               'allow_create_ad_distributor_acts': False,
                                               'allow_create_clients': False,
                                               'allow_create_campaigns': False,
                                               'allow_edit_report': False,
                                           }
                                       }
                                   ]
                           }),
                          ])
async def test_list_reports(client: BaseTestClient, service_discovery: ServiceDiscovery, yandex_uid: int,
                            grants_return_value, reports_return_value, expected):
    service_discovery.grants.check_access_level.return_value = grants_return_value
    service_discovery.ord.get_reports_info.return_value = reports_return_value
    got = await client.get(URL.format(agency_id=1), expected_status=200)
    assert got == expected

    got = await client.get(URL.format(agency_id=1) + '?sort=status,sending_date,clients_count', expected_status=200)
    assert got == expected


async def test_list_reports_wrong_sort_format(client: BaseTestClient, service_discovery: ServiceDiscovery,
                                              yandex_uid: int):
    service_discovery.grants.check_access_level.return_value = grants_structs.AccessLevel.ALLOW
    await client.get(URL.format(agency_id=1) + '?sort=wrong', expected_status=422)
