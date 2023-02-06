import pytest

from crm.agency_cabinet.common.testing import BaseTestClient
from crm.agency_cabinet.common.service_discovery import ServiceDiscovery
from crm.agency_cabinet.grants.common import structs as grants_structs
from crm.agency_cabinet.ord.common import consts as ord_consts, structs as ord_structs

URL = '/api/agencies/{agency_id}/ord/organizations'


@pytest.mark.parametrize(('grants_return_value',
                          'organizations_list_return_value',
                          'expected'),
                         [(grants_structs.AccessLevel.ALLOW,
                           ord_structs.OrganizationsList(
                               size=5,
                               organizations=[
                                   ord_structs.Organization(
                                       id=5,
                                       type=ord_consts.OrganizationType.ffl,
                                       name='Test org',
                                       inn='123456789',
                                   ),
                                   ord_structs.Organization(
                                       id=6,
                                   )
                               ]
                           ),
                           {
                               'size': 5,
                               'items': [
                                   {
                                       'organization_id': 5,
                                       'type': 'ffl',
                                       'name': 'Test org',
                                       'inn': '123456789',
                                       'is_ors': None,
                                       'is_rr': None,
                                       'mobile_phone': None,
                                       'epay_number': None,
                                       'reg_number': None,
                                       'alter_inn': None,
                                       'oksm_number': None,
                                       'rs_url': None,
                                   },
                                   {
                                       'organization_id': 6,
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
                               ]
                           },)]
                         )
async def test_get_organizations(client: BaseTestClient, service_discovery: ServiceDiscovery, yandex_uid: int,
                                 grants_return_value, organizations_list_return_value, expected):
    service_discovery.grants.check_access_level.return_value = grants_return_value
    service_discovery.ord.get_organizations.return_value = organizations_list_return_value
    got = await client.get(URL.format(agency_id=1), expected_status=200)

    assert got == expected


async def test_get_organizations_forbidden(client: BaseTestClient, service_discovery: ServiceDiscovery,
                                           yandex_uid: int):
    service_discovery.grants.check_access_level.return_value = grants_structs.AccessLevel.DENY
    await client.get(URL.format(agency_id=1), expected_status=403)
