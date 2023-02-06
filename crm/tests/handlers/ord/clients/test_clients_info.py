import pytest
from decimal import Decimal

from crm.agency_cabinet.common.testing import BaseTestClient
from crm.agency_cabinet.common.service_discovery import ServiceDiscovery
from crm.agency_cabinet.grants.common import structs as grants_structs
from crm.agency_cabinet.ord.common import structs as ord_structs

URL = '/api/agencies/{agency_id}/ord/reports/{report_id}/clients'


@pytest.mark.parametrize(('grants_return_value',
                          'clients_return_value',
                          'expected'),
                         [(grants_structs.AccessLevel.ALLOW,
                           [
                               ord_structs.ClientInfo(
                                   id=1,
                                   client_id='client_id',
                                   login='login',
                                   name='name',
                                   suggested_amount=Decimal('200'),
                                   has_valid_ad_distributor_partner=False,
                                   has_valid_advertiser=False,
                                   campaigns_count=2,
                                   has_valid_ad_distributor=False,
                                   has_valid_advertiser_contractor=False,
                                   has_valid_partner_client=False,
                               )
                           ],
                          {
                              "size": 1,
                              "items": [
                                  {
                                      "id": 1,
                                      "client_id": "client_id",
                                      "login": "login",
                                      "name": "name",
                                      "suggested_amount": 200.0,
                                      "campaigns_count": 2,
                                      "has_valid_ad_distributor_partner": False,
                                      "has_valid_advertiser_contractor": False,
                                      "has_valid_advertiser": False,
                                      "has_valid_partner_client": False,
                                      "has_valid_ad_distributor": False,
                                  }]
                          },)]
                         )
async def test_get_clients_info(client: BaseTestClient, service_discovery: ServiceDiscovery, yandex_uid: int,
                                grants_return_value, clients_return_value, expected):
    service_discovery.grants.check_access_level.return_value = grants_return_value
    service_discovery.ord.get_report_clients_info.return_value = clients_return_value
    got = await client.get(URL.format(agency_id=1, report_id=1), json={'is_valid': True}, expected_status=200)

    assert got == expected
