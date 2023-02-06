import pytest
from decimal import Decimal

from crm.agency_cabinet.common.testing import BaseTestClient
from crm.agency_cabinet.common.service_discovery import ServiceDiscovery
from crm.agency_cabinet.grants.common import structs as grants_structs
from crm.agency_cabinet.ord.common import consts as ord_consts, structs as ord_structs

URL = '/api/agencies/{agency_id}/ord/reports/{report_id}/clients/{client_id}/rows'


@pytest.mark.parametrize(('grants_return_value',
                          'rows_return_value',
                          'expected'),
                         [(grants_structs.AccessLevel.ALLOW,
                           ord_structs.ClientRowsList(
                               size=10,
                               rows=[
                                   ord_structs.ClientRow(
                                       id=1,
                                       suggested_amount=Decimal('200'),
                                       campaign=ord_structs.Campaign(
                                           id=1,
                                           campaign_eid='campaign1',
                                           name='name'
                                       ),
                                       ad_distributor_organization=ord_structs.Organization(
                                           id=2,
                                           type=ord_consts.OrganizationType.ffl,
                                           name='test2',
                                           inn='654321',
                                           is_rr=False,
                                           is_ors=False,
                                           mobile_phone='3535353',
                                           epay_number='321',
                                           reg_number='123',
                                           alter_inn='123456',
                                           oksm_number='2',
                                           rs_url='test2'
                                       ),
                                       ad_distributor_partner_organization=ord_structs.Organization(
                                           id=3,
                                       )
                                   )
                               ]
                           ),
                           {
                               'size': 10,
                               'items': [
                                   {
                                       'id': 1,
                                       'suggested_amount': 200.0,
                                       'campaign': {
                                           'id': 1,
                                           'campaign_eid': 'campaign1',
                                           'name': 'name'
                                       },
                                       'ad_distributor_organization': {
                                           'organization_id': 2,
                                           'type': 'ffl',
                                           'name': 'test2',
                                           'inn': '654321',
                                           'is_ors': False,
                                           'is_rr': False,
                                           'mobile_phone': '3535353',
                                           'epay_number': '321',
                                           'reg_number': '123',
                                           'alter_inn': '123456',
                                           'oksm_number': '2',
                                           'rs_url': 'test2',
                                       },
                                       'ad_distributor_partner_organization': {
                                           'organization_id': 3,
                                           'type': None,
                                           'name': None,
                                           'inn': None,
                                           'is_ors': None,
                                           'is_rr': None,
                                           'mobile_phone': None,
                                           'epay_number': None,
                                           'reg_number': None,
                                           'alter_inn': None,
                                           'oksm_number': None,
                                           'rs_url': None,
                                       },
                                       'partner_client_organization': None,
                                       'advertiser_contractor_organization': None,
                                       'advertiser_organization': None,
                                       'ad_distributor_contract': None,
                                       'ad_distributor_partner_contract': None,
                                       'advertiser_contract': None,
                                       'ad_distributor_act': None,
                                       'ad_distributor_partner_act': None,
                                   }
                               ]
                           }
                           ), ])
async def test_get_client_rows(client: BaseTestClient, service_discovery: ServiceDiscovery, yandex_uid: int,
                               grants_return_value, rows_return_value, expected):
    service_discovery.grants.check_access_level.return_value = grants_return_value
    service_discovery.ord.get_client_rows.return_value = rows_return_value
    got = await client.get(URL.format(agency_id=1, report_id=1, client_id=1), expected_status=200)

    assert got == expected
