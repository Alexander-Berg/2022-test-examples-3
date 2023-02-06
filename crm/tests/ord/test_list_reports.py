import datetime

import pytest

from crm.agency_cabinet.common.testing import BaseTestClient
from crm.agency_cabinet.common.service_discovery import ServiceDiscovery
from crm.agency_cabinet.grants.common import structs as grants_structs
from crm.agency_cabinet.ord.common import consts as ord_consts, structs as ord_structs

URL = '/api/v1/reports'


@pytest.mark.parametrize(
    ('grants_return_value', 'reports_return_value', 'expected'),
    [
        (
            grants_structs.CheckPermissionsResponse(
                is_have_permissions=True, missed_permissions=[],
                partner=grants_structs.Partner(partner_id=1, name='name', external_id='1', type=grants_structs.PartnerType.agency)
            ),
            [
                ord_structs.ReportInfo(
                    report_id=1,
                    status=ord_structs.ReportStatuses.draft,
                    reporter_type=ord_consts.ReporterType.partner,
                    clients_count=1,
                    campaigns_count=1,
                    settings=ord_structs.ReportSettings(
                        name='direct',
                        display_name='Директ',
                        allow_create_ad_distributor_acts=False,
                        allow_create_clients=False,
                        allow_create_campaigns=False,
                        allow_edit_report=False,
                    ),
                    period_from=datetime.datetime(2021, 1, 1, tzinfo=datetime.timezone.utc),
                    sending_date=datetime.datetime(2021, 1, 1, tzinfo=datetime.timezone.utc),
                )
            ],
            {
                'size': 1,
                'items':
                    [
                        {
                            'report_id': 1,
                            'sending_date': '2021-01-01',
                            'status': 'draft',
                            'reporter_type': 'partner',
                            'type': 'direct',
                            'period_from': '2021-01-01',
                        }
                    ]
            }
        ),
        (
            grants_structs.CheckPermissionsResponse(is_have_permissions=False, missed_permissions=[]),
            [],
            {'error': {'error_code': 'ACCESS_DENIED',
                       'http_code': 403,
                       'messages': [{'params': {}, 'text': "You don't have access"}]}}
        )
    ]
)
async def test_list_reports(client: BaseTestClient, service_discovery: ServiceDiscovery, app_client_id: str,
                            grants_return_value, reports_return_value, expected):
    service_discovery.grants.check_oauth_permissions.return_value = grants_return_value
    service_discovery.ord.get_reports_info.return_value = reports_return_value
    got = await client.get(URL)
    assert got == expected
