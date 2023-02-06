# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime

import mock
import pytest
from six.moves.xmlrpc_client import DateTime

from travel.rasp.train_api.train_purchase.core.models import ClientContracts
from travel.rasp.train_api.train_purchase.tasks.client_contracts import update_billing_client_contracts

GET_CLIENT_CONTRACT_RESPONSE = [{'PARTNER_COMMISSION_SUM': '60',
                                 'PARTNER_COMMISSION_SUM2': '30',
                                 'IS_SUSPENDED': 0,
                                 'MANAGER_CODE': 29108,
                                 'IS_ACTIVE': 1,
                                 'IS_SIGNED': 1,
                                 'CURRENCY': 'RUR',
                                 'IS_FAXED': 0,
                                 'SERVICES': [171],
                                 'PERSON_ID': 5081232,
                                 'CONTRACT_TYPE': 0,
                                 'DT': DateTime(datetime(2017, 4, 8)),
                                 'IS_CANCELLED': 0,
                                 'EXTERNAL_ID': '93321/17',
                                 'ID': 312353,
                                 'PAYMENT_TYPE': 2}]


@pytest.mark.mongouser
def test_update_billing_client_contracts():
    with mock.patch('travel.rasp.train_api.train_purchase.tasks.client_contracts.get_client_contracts',
                    return_value=GET_CLIENT_CONTRACT_RESPONSE):
        assert ClientContracts.objects.count() == 0

        update_billing_client_contracts()
        assert ClientContracts.objects.count() > 0
