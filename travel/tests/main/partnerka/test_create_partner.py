from __future__ import absolute_import

import pytest
from mock import patch

from travel.avia.library.python.balance_client.client import Client as BalanceClient
from travel.avia.library.python.tester.factories import create_partner as create_partner_in_db

from travel.avia.backend.main.api_types import partnerka

from travel.avia.library.python.common.models.partner import Partner, PartnerUser


@pytest.mark.dbuser
def test_simple():
    params = {
        'login': 'mangin',
        'title': 'title',
        'siteUrl': 'https://yandex.ru',
        'operatorUid': 'some_operator_uid',
        'code': 'some_code'
    }

    create_partner_in_db(billing_order_id=100)

    with patch.object(BalanceClient, 'create_avia_client', return_value=None, autospec=True) as create_avia_client:
        with patch.object(partnerka, 'get_uid', return_value=123, autospec=True) as get_uid:

            partnerka.create_partner(params)
            assert create_avia_client.call_count == 1
            assert get_uid.call_count == 1

            p = Partner.objects.using('writable').filter(billing_order_id=101)[0]

            assert p.billing_order_id == 101
            assert p.title == params['title']
            assert p.site_url == params['siteUrl']

            user = PartnerUser.objects.using('writable').filter(partner=p)[0]
            assert user.login == params['login']
            assert user.role == 'owner'
            assert user.name == ''
