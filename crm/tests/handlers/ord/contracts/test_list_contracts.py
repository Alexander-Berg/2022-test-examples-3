import pytest
from decimal import Decimal
from datetime import datetime, timezone
from crm.agency_cabinet.common.testing import BaseTestClient
from crm.agency_cabinet.common.service_discovery import ServiceDiscovery
from crm.agency_cabinet.grants.common import structs as grants_structs
from crm.agency_cabinet.ord.common import consts as ord_consts, structs as ord_structs

URL = '/api/agencies/1/ord/contracts'


@pytest.fixture
def contracts():
    contracts = [
        ord_structs.Contract(
            id=1,
            contract_eid='123',
            is_reg_report=True,
            type=ord_consts.ContractType.contract,
            action_type=ord_consts.ContractActionType.other,
            subject_type=ord_consts.ContractSubjectType.other,
            date=datetime(2022, 7, 20, 0, 0, 0, tzinfo=timezone.utc),
            amount=Decimal(100),
            is_vat=True,
            client_organization=ord_structs.Organization(
                id=1,
                type=ord_consts.OrganizationType.ffl,
                name='test',
                inn='123456',
                is_rr=False,
                is_ors=False,
                mobile_phone='5353535',
                epay_number='123',
                reg_number='321',
                alter_inn='654321',
                oksm_number='1',
                rs_url='test'
            ),
            contractor_organization=ord_structs.Organization(
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
        )
    ]

    return contracts


async def test_get_contracts(client: BaseTestClient, service_discovery: ServiceDiscovery, yandex_uid: int, contracts):
    service_discovery.grants.check_access_level.return_value = grants_structs.AccessLevel.ALLOW
    service_discovery.ord.get_contracts.return_value = ord_structs.ContractsList(
        contracts=contracts,
        size=len(contracts)
    )
    got = await client.get(URL)
    assert got == {'items': [{'action_type': 'other',
                              'amount': 100.0,
                              'client_organization': {'alter_inn': '654321',
                                                      'epay_number': '123',
                                                      'inn': '123456',
                                                      'is_ors': False,
                                                      'is_rr': False,
                                                      'mobile_phone': '5353535',
                                                      'name': 'test',
                                                      'oksm_number': '1',
                                                      'organization_id': 1,
                                                      'reg_number': '321',
                                                      'rs_url': 'test',
                                                      'type': 'ffl'},
                              'contract_eid': '123',
                              'contractor_organization': {'alter_inn': '123456',
                                                          'epay_number': '321',
                                                          'inn': '654321',
                                                          'is_ors': False,
                                                          'is_rr': False,
                                                          'mobile_phone': '3535353',
                                                          'name': 'test2',
                                                          'oksm_number': '2',
                                                          'organization_id': 2,
                                                          'reg_number': '123',
                                                          'rs_url': 'test2',
                                                          'type': 'ffl'},
                              'date': contracts[0].date.strftime('%Y-%m-%d'),
                              'id': 1,
                              'is_reg_report': True,
                              'is_vat': True,
                              'subject_type': 'other',
                              'type': 'contract'}],
                   'size': 1}
