import pytest
import os
from unittest import mock
from datetime import datetime
from crm.agency_cabinet.common.service_discovery import ServiceDiscovery
from crm.agency_cabinet.common.testing import BaseTestClient
from crm.agency_cabinet.grants.common import structs as grants_structs
from crm.agency_cabinet.rewards.common import structs as rewards_structs
from crm.agency_cabinet.rewards.client import UnsuitableAgency, NoSuchReportException, UnsupportedReportParametersException

URL = '/api/agencies/{agency_id}/reports'


async def test_access_deny(client: BaseTestClient, service_discovery: ServiceDiscovery, yandex_uid: int):
    service_discovery.grants.check_access_level.return_value = grants_structs.AccessLevel.DENY
    await client.get(URL.format(agency_id=1), expected_status=403)


@pytest.mark.parametrize(('grants_return_value',
                          'reports_side_effect'),
                         [(grants_structs.AccessLevel.ALLOW, UnsuitableAgency()),
                          (grants_structs.AccessLevel.ALLOW, NoSuchReportException())
                          ])
async def test_detailed_report_info_exception(client: BaseTestClient, service_discovery: ServiceDiscovery, yandex_uid: int,
                                              grants_return_value, reports_side_effect):
    service_discovery.grants.check_access_level.return_value = grants_return_value
    service_discovery.rewards.get_detailed_report_info.side_effect = reports_side_effect
    await client.get(URL.format(agency_id=1) + '/1', expected_status=404)


@pytest.mark.parametrize(('grants_return_value',
                          'reports_return_value',
                          'expected'),
                         [(grants_structs.AccessLevel.ALLOW,
                           rewards_structs.ReportInfo(id=1,
                                                      contract_id=1111,
                                                      type='month',
                                                      service='direct',
                                                      name='Отчет',
                                                      created_at=datetime(2021, 3, 1),
                                                      period_from=datetime(2021, 1, 1),
                                                      period_to=datetime(2021, 2, 1)
                                                      ),
                           {
                               'id': 1,
                               'contract_id': 1111,
                               'type': 'month',
                               'name': 'Отчет',
                               'created_at': datetime(2021, 3, 1).isoformat(),
                               'period_from': datetime(2021, 1, 1).strftime('%Y-%m-%d'),
                               'period_to': datetime(2021, 2, 1).strftime('%Y-%m-%d'),
                               'status': 'requested',
                               'service': 'direct',
                               'clients': []

                           }),
                          ])
async def test_detailed_report_info(client: BaseTestClient, service_discovery: ServiceDiscovery, yandex_uid: int,
                                    grants_return_value, reports_return_value, expected):
    service_discovery.grants.check_access_level.return_value = grants_return_value
    service_discovery.rewards.get_detailed_report_info.return_value = reports_return_value
    got = await client.get(URL.format(agency_id=1) + '/1', expected_status=200)

    assert got == expected

    service_discovery.grants.check_access_level.assert_awaited_with(yandex_uid=yandex_uid, agency_id=1)
    service_discovery.rewards.get_detailed_report_info.assert_awaited_with(1, 1)


@pytest.mark.parametrize(('grants_return_value',
                          'reports_return_value',
                          'expected'),
                         [(grants_structs.AccessLevel.ALLOW,
                           rewards_structs.ReportInfo(id=1,
                                                      contract_id=1111,
                                                      type='month',
                                                      service='direct',
                                                      name='Отчет',
                                                      created_at=datetime(2021, 3, 1),
                                                      period_from=datetime(2021, 1, 1),
                                                      period_to=datetime(2021, 2, 1)
                                                      ),
                           {
                               'id': 1,
                               'contract_id': 1111,
                               'type': 'month',
                               'name': 'Отчет',
                               'created_at': datetime(2021, 3, 1).isoformat(),
                               'period_from': datetime(2021, 1, 1).strftime('%Y-%m-%d'),
                               'period_to': datetime(2021, 2, 1).strftime('%Y-%m-%d'),
                               'status': 'requested',
                               'service': 'direct',
                               'clients': []

                           }),
                          ])
async def test_create_report(client: BaseTestClient, service_discovery: ServiceDiscovery, yandex_uid: int,
                             grants_return_value, reports_return_value, expected):
    service_discovery.grants.check_access_level.return_value = grants_return_value
    service_discovery.rewards.create_report.return_value = reports_return_value
    got = await client.post(URL.format(agency_id=1), expected_status=200,
                            json={
        'contract_id': 1111,
        'type': 'month',
        'name': 'Отчет',
        'period_from': datetime(2021, 1, 1).strftime('%Y-%m-%d'),
        # 'period_to': datetime(2021, 2, 1).strftime('%Y-%m-%d'),
        'service': 'direct',
        'clients': []

    })

    assert got == expected

    service_discovery.grants.check_access_level.assert_awaited_with(yandex_uid=yandex_uid, agency_id=1)
    service_discovery.rewards.create_report.assert_awaited_with(
        1, 1111, 'Отчет', 'month', 'direct', datetime(2021, 1, 1),
        datetime.combine(datetime(2021, 1, 31), datetime.max.time()), [])


async def test_create_report_validation(client: BaseTestClient, service_discovery: ServiceDiscovery, yandex_uid: int):
    await client.post(URL.format(agency_id=1), expected_status=422, json={
        'contract_id': 1111,
        'type': 'WTF',
        'name': 'Отчет',
        'period_from': datetime(2021, 1, 1).strftime('%Y-%m-%d'),
        'period_to': datetime(2021, 2, 1).strftime('%Y-%m-%d'),
        'service': 'direct',
        'clients': []
    })


async def test_create_report_validation_period(client: BaseTestClient, service_discovery: ServiceDiscovery, yandex_uid: int):
    service_discovery.grants.check_access_level.return_value = grants_structs.AccessLevel.ALLOW

    service_discovery.rewards.create_report.side_effect = UnsupportedReportParametersException()

    got = await client.post(
        URL.format(agency_id=1),
        json={
            'contract_id': 1111,
            'type': 'month',
            'name': 'Отчет',
            'period_from': datetime(2023, 1, 1).strftime('%Y-%m-%d'),
            'service': 'direct',
            'clients': []
        },
        expected_status=422
    )
    assert got['error']['error_code'] == 'IMPOSSIBLE_REPORT'
    assert got['error']['messages'] == [{'params': {}, 'text': 'It is impossible to build report: '}]


# TODO: rework test and remove old check_access_level method
@pytest.mark.parametrize(('grants_return_value',
                          'reports_return_value',
                          'expected'),
                         [(grants_structs.CheckPermissionsResponse(
                             is_have_permissions=True,
                             partner=grants_structs.Partner(
                                 external_id='1',
                                 partner_id=123,
                                 type='agency',
                                 name='test'
                             )
                         ),
                             rewards_structs.ReportInfo(id=1,
                                                        contract_id=1111,
                                                        type='month',
                                                        service='direct',
                                                        name='Отчет',
                                                        created_at=datetime(2021, 3, 1),
                                                        period_from=datetime(2021, 1, 1),
                                                        period_to=datetime(2021, 2, 1)
                                                        ),
                             {
                             'id': 1,
                             'contract_id': 1111,
                             'type': 'month',
                             'name': 'Отчет',
                             'created_at': datetime(2021, 3, 1).isoformat(),
                             'period_from': datetime(2021, 1, 1).strftime('%Y-%m-%d'),
                             'period_to': datetime(2021, 2, 1).strftime('%Y-%m-%d'),
                             'status': 'requested',
                             'service': 'direct',
                             'clients': []

                         }),
])
async def test_detailed_report_info_new_grants(
    client: BaseTestClient, service_discovery: ServiceDiscovery, yandex_uid: int,
    grants_return_value, reports_return_value, expected
):
    with mock.patch.dict(os.environ, {"USE_REPORTS_API_NEW_GRANTS": 'True'}):
        service_discovery.grants.check_permissions.return_value = grants_return_value
        service_discovery.rewards.get_detailed_report_info.return_value = reports_return_value
        got = await client.get(URL.format(agency_id=1) + '/1', expected_status=200)

        assert got == expected

        service_discovery.grants.check_permissions.assert_awaited_with(
            yandex_uid=yandex_uid, agency_id=1, permissions=('rewards',))
        service_discovery.rewards.get_detailed_report_info.assert_awaited_with(1, 1)


@pytest.mark.parametrize(('grants_return_value',
                          'reports_return_value',
                          'expected'),
                         [(grants_structs.CheckPermissionsResponse(
                             is_have_permissions=True,
                             partner=grants_structs.Partner(
                                 external_id='1',
                                 partner_id=123,
                                 type='agency',
                                 name='test'
                             )
                         ),
                             rewards_structs.ReportInfo(id=1,
                                                        contract_id=1111,
                                                        type='month',
                                                        service='direct',
                                                        name='Отчет',
                                                        created_at=datetime(2021, 3, 1),
                                                        period_from=datetime(2021, 1, 1),
                                                        period_to=datetime(2021, 2, 1)
                                                        ),
                             {
                             'id': 1,
                             'contract_id': 1111,
                             'type': 'month',
                             'name': 'Отчет',
                             'created_at': datetime(2021, 3, 1).isoformat(),
                             'period_from': datetime(2021, 1, 1).strftime('%Y-%m-%d'),
                             'period_to': datetime(2021, 2, 1).strftime('%Y-%m-%d'),
                             'status': 'requested',
                             'service': 'direct',
                             'clients': []

                         }),
])
async def test_detailed_report_info_new_grants_partner_id(
    client: BaseTestClient, service_discovery: ServiceDiscovery, yandex_uid: int,
    grants_return_value, reports_return_value, expected
):
    with mock.patch.dict(os.environ, {"USE_REPORTS_API_NEW_GRANTS": 'True', "USE_AGENCY_ID_AS_PARTNER_ID": 'True'}, clear=True):

        service_discovery.grants.check_permissions.return_value = grants_return_value
        service_discovery.rewards.get_detailed_report_info.return_value = reports_return_value
        got = await client.get(URL.format(agency_id=123) + '/1', expected_status=200)

        assert got == expected

        service_discovery.grants.check_permissions.assert_awaited_with(
            yandex_uid=yandex_uid, partner_id=123, permissions=('rewards',))
        service_discovery.rewards.get_detailed_report_info.assert_awaited_with(agency_id=1, report_id=1)
