import pytest

from crm.agency_cabinet.common.testing import BaseTestClient
from crm.agency_cabinet.common.service_discovery import ServiceDiscovery
from crm.agency_cabinet.grants.common import structs as grants_structs

URL = '/api/agencies/1/ord/reports/1/clients/1/rows/1'


@pytest.mark.parametrize(
    (
        'grants_return_value',
        'reports_return_value',
        'edit_row_params',
    ),
    [
        (
            grants_structs.AccessLevel.ALLOW,
            None,
            {
                'ad_distributor_act_id': 1,
                'client_contract_id': 1,
                'campaign_eid': 'eid',
                'campaign_name': 'name',
                'client_act_id': 1,
            }
        ),
        (
            grants_structs.AccessLevel.ALLOW,
            None,
            {
                'ad_distributor_act_id': 1,
            }
        ),
    ]
)
async def test_edit_client_rows_info(
    client: BaseTestClient,
    service_discovery: ServiceDiscovery,
    yandex_uid: int,
    grants_return_value,
    reports_return_value,
    edit_row_params
):
    service_discovery.grants.check_access_level.return_value = grants_return_value
    service_discovery.ord.edit_client_row.return_value = reports_return_value
    got = await client.post(URL, json=edit_row_params)
    assert got == {'status': 'ok'}
