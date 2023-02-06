# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import xmlrpclib
from datetime import datetime

import mock
import pytest
from hamcrest import assert_that, contains_inanyorder, has_properties

from common.tester.utils.datetime import replace_now

from travel.rasp.suburban_selling.selling.aeroexpress.factories import ClientContractsFactory, ClientContractFactory
from travel.rasp.suburban_selling.selling.aeroexpress.models import ClientContracts
from travel.rasp.suburban_selling.selling.tasks.client_contracts import update_contracts

pytestmark = [pytest.mark.mongouser, pytest.mark.dbuser]


RESPONSE = [{'PARTNER_COMMISSION_SUM': '40', 'IS_SUSPENDED': 0, 'MANAGER_CODE': 32907, 'IS_ACTIVE': 1, 'IS_SIGNED': 1,
             'CURRENCY': 'RUR', 'IS_FAXED': 0, 'SERVICES': [35, 607], 'PERSON_ID': 4885589, 'CONTRACT_TYPE': 0,
             'IS_DEACTIVATED': 0, 'DT': xmlrpclib.DateTime(datetime(2017, 11, 10)), 'IS_CANCELLED': 0,
             'EXTERNAL_ID': '75323/17', 'ID': 310445, 'PAYMENT_TYPE': 3}]


def test_update_billing_contracts():
    with mock.patch('travel.rasp.suburban_selling.selling.tasks.client_contracts.get_client_contracts',
                    return_value=RESPONSE):
        with replace_now(datetime(2018, 9, 10, 12)):
            update_contracts()

        contracts = list(ClientContracts.objects.all())
        assert_that(contracts, contains_inanyorder(has_properties({
            'updated_at': datetime(2018, 9, 10, 12),
            'contracts': contains_inanyorder(
                has_properties({
                    'is_active': True,
                    'start_dt': datetime(2017, 11, 10),
                    'finish_dt': None,
                    'services': [35, 607],
                    'partner_commission_sum': 40
                })
            )
        })))

        with replace_now(datetime(2018, 9, 10, 16)):
            update_contracts()

        contracts = list(ClientContracts.objects.all())
        assert_that(contracts, contains_inanyorder(has_properties({
            'updated_at': datetime(2018, 9, 10, 16)
        })))


def test_has_active_contract():
    ClientContractsFactory(updated_at=datetime(2018, 9, 10, 10))

    with replace_now(datetime(2018, 9, 10, 11)):
        assert ClientContracts.has_active_contract()

    with replace_now(datetime(2018, 9, 10, 16, 59)):
        assert ClientContracts.has_active_contract()

    with replace_now(datetime(2018, 9, 11)):
        assert not ClientContracts.has_active_contract()

    with replace_now(datetime(2018, 9, 11, 3)):
        ClientContractsFactory(updated_at=datetime(2018, 9, 11), contracts=[ClientContractFactory(services=[35])])
        assert not ClientContracts.has_active_contract()

        ClientContractsFactory(updated_at=datetime(2018, 9, 11), partner_id='12345')
        assert not ClientContracts.has_active_contract()
