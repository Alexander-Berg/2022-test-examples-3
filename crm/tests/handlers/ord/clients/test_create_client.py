import pytest
from decimal import Decimal

from crm.agency_cabinet.common.testing import BaseTestClient
from crm.agency_cabinet.common.service_discovery import ServiceDiscovery
from crm.agency_cabinet.grants.common import structs as grants_structs
from crm.agency_cabinet.ord.common import structs as ord_structs

URL = "api/agencies/{agency_id}/ord/reports/{report_id}/clients"


@pytest.mark.parametrize(("grants_return_value",
                          "clients_return_value",
                          "expected"),
                         [(grants_structs.AccessLevel.ALLOW,
                           ord_structs.ClientInfo(
                               id=1,
                               client_id="client_id",
                               login="login",
                               name="name",
                               suggested_amount=Decimal("200"),
                               campaigns_count=0,
                               has_valid_ad_distributor=True,
                               has_valid_ad_distributor_partner=True,
                               has_valid_advertiser=True,
                               has_valid_advertiser_contractor=True,
                               has_valid_partner_client=True
                           ),
                           {
                               "id": 1,
                               "client_id": "client_id",
                               "login": "login",
                               "name": "name",
                               "suggested_amount": 200.0,
                               "campaigns_count": 0,
                               "has_valid_ad_distributor": True,
                               "has_valid_ad_distributor_partner": True,
                               "has_valid_advertiser": True,
                               "has_valid_advertiser_contractor": True,
                               "has_valid_partner_client": True
                           }
                           )])
async def test_create_client(client: BaseTestClient, service_discovery: ServiceDiscovery, yandex_uid: int,
                             grants_return_value, clients_return_value, expected):
    service_discovery.grants.check_access_level.return_value = grants_return_value
    service_discovery.ord.create_client.return_value = clients_return_value
    got = await client.post(URL.format(agency_id=1, report_id=1), json={"client_id": "1"})

    assert got == expected
