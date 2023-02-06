# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import json
from datetime import datetime

import pytest
from django.test import Client
from hamcrest import assert_that, has_entry, contains_inanyorder

from common.tester.utils.datetime import replace_now
from common.tester.utils.replace_setting import replace_dynamic_setting
from travel.rasp.train_api.train_purchase.core.factories import ClientContractsFactory


def _create_partner_contracts(partner_codes):
    for partner_code in partner_codes:
        ClientContractsFactory(updated_at=datetime(2017, 6, 30), partner=partner_code)


@replace_now('2017-06-30')
@pytest.mark.mongouser
@pytest.mark.parametrize('partner_codes', [
    [],
    ['im'],
    ['im', 'ufs']
])
def test_get_active_partners_by_contracts(partner_codes):
    _create_partner_contracts(partner_codes)

    client = Client()
    response = client.get('/ru/api/active-partners/')

    assert response.status_code == 200
    data = json.loads(response.content)
    assert_that(data, has_entry('partnerCodes', contains_inanyorder(*partner_codes)))


@replace_now('2017-06-30')
@pytest.mark.mongouser
@pytest.mark.parametrize('partner_codes', [
    [],
    ['im'],
    ['im', 'ufs']
])
def test_get_active_partners_by_setting(partner_codes):
    _create_partner_contracts(['im', 'ufs'])

    with replace_dynamic_setting('TRAN_PURCHASE_ENABLED_PARTNERS', ' '.join(partner_codes)):
        client = Client()
        response = client.get('/ru/api/active-partners/')

    assert response.status_code == 200
    data = json.loads(response.content)
    assert_that(data, has_entry('partnerCodes', contains_inanyorder(*partner_codes)))
