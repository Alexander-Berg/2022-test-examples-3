import os
import datetime
from unittest import mock
from crm.agency_cabinet.common.testing import BaseTestClient
from crm.agency_cabinet.common.service_discovery import ServiceDiscovery
from crm.agency_cabinet.grants.common import structs as grants_structs
from crm.agency_cabinet.ord.common import consts as ord_consts, structs as ord_structs


URL = '/api/agencies/{}/ord/reports'


async def test_create_report(client: BaseTestClient, service_discovery: ServiceDiscovery, yandex_uid: int):
    service_discovery.grants.check_access_level.return_value = grants_structs.AccessLevel.ALLOW
    service_discovery.ord.create_report.return_value = ord_structs.ReportInfo(
        report_id=1,
        status=ord_consts.ReportStatuses.draft,
        reporter_type=ord_consts.ReporterType.partner,
        clients_count=0,
        campaigns_count=0,
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

    got = await client.post(URL.format(1), json={
        'period_from': '2022-01-20',
        'reporter_type': ord_consts.ReporterType.partner.value
    })
    service_discovery.ord.create_report.assert_awaited_with(
        agency_id=1,
        period_from=datetime.datetime(2022, 1, 20, 0, 0, tzinfo=datetime.timezone.utc),
        reporter_type=ord_consts.ReporterType.partner
    )

    assert got == {
        'report_id': 1,
        'status': 'draft',
        'reporter_type': 'partner',
        'period_from': '2022-01-20',
        'sending_date': '2022-01-20',
        'clients_count': 0,
        'campaigns_count': 0,
        'settings': {
            'name': 'direct',
            'display_name': 'Директ',
            'allow_create_ad_distributor_acts': False,
            'allow_create_clients': False,
            'allow_create_campaigns': False,
            'allow_edit_report': False,
        }
    }


async def test_create_report_new_grants(client: BaseTestClient, service_discovery: ServiceDiscovery, yandex_uid: int):
    with mock.patch.dict(os.environ, {"USE_REPORTS_API_NEW_GRANTS": 'True'},
                         clear=True):
        service_discovery.grants.check_permissions.return_value = grants_structs.CheckPermissionsResponse(
            is_have_permissions=True,
            partner=grants_structs.Partner(
                external_id='1',
                partner_id=123,
                type='agency',
                name='test'
            )
        )
        service_discovery.ord.create_report.return_value = ord_structs.ReportInfo(
            report_id=1,
            status=ord_consts.ReportStatuses.draft,
            reporter_type=ord_consts.ReporterType.partner,
            clients_count=0,
            campaigns_count=0,
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

        got = await client.post(URL.format(1), json={
            'period_from': '2022-01-20',
            'reporter_type': ord_consts.ReporterType.partner.value
        })
        service_discovery.ord.create_report.assert_awaited_with(
            agency_id=1,
            period_from=datetime.datetime(2022, 1, 20, 0, 0, tzinfo=datetime.timezone.utc),
            reporter_type=ord_consts.ReporterType.partner
        )

        assert got == {
            'report_id': 1,
            'status': 'draft',
            'reporter_type': 'partner',
            'period_from': '2022-01-20',
            'sending_date': '2022-01-20',
            'clients_count': 0,
            'campaigns_count': 0,
            'settings': {
                'name': 'direct',
                'display_name': 'Директ',
                'allow_create_ad_distributor_acts': False,
                'allow_create_clients': False,
                'allow_create_campaigns': False,
                'allow_edit_report': False,
            }
        }


async def test_create_report_new_grants_partner_id(client: BaseTestClient, service_discovery: ServiceDiscovery, yandex_uid: int):
    with mock.patch.dict(os.environ, {"USE_REPORTS_API_NEW_GRANTS": 'True', "USE_AGENCY_ID_AS_PARTNER_ID": 'True'},
                         clear=True):
        service_discovery.grants.check_access_level.return_value = grants_structs.CheckPermissionsResponse(
            is_have_permissions=True,
            partner=grants_structs.Partner(
                external_id='2',
                partner_id=123,
                type='agency',
                name='test'
            )
        )
        service_discovery.ord.create_report.return_value = ord_structs.ReportInfo(
            report_id=1,
            status=ord_consts.ReportStatuses.draft,
            reporter_type=ord_consts.ReporterType.partner,
            clients_count=0,
            campaigns_count=0,
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

        got = await client.post(URL.format(123), json={
            'period_from': '2022-01-20',
            'reporter_type': ord_consts.ReporterType.partner.value
        })
        service_discovery.ord.create_report.assert_awaited_with(
            agency_id=123,
            period_from=datetime.datetime(2022, 1, 20, 0, 0, tzinfo=datetime.timezone.utc),
            reporter_type=ord_consts.ReporterType.partner
        )

        assert got == {
            'report_id': 1,
            'status': 'draft',
            'reporter_type': 'partner',
            'period_from': '2022-01-20',
            'sending_date': '2022-01-20',
            'clients_count': 0,
            'campaigns_count': 0,
            'settings': {
                'name': 'direct',
                'display_name': 'Директ',
                'allow_create_ad_distributor_acts': False,
                'allow_create_clients': False,
                'allow_create_campaigns': False,
                'allow_edit_report': False,
            }
        }
