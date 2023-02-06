# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime
from decimal import Decimal

import mock
import pytest

from common.tester.utils.datetime import replace_now
from common.tester.utils.replace_setting import replace_dynamic_setting
from travel.rasp.library.python.common23.date.environment import now_utc
from travel.rasp.train_api.train_partners.im.factories.utils import mock_im
from travel.rasp.train_api.train_partners.im.partner_balance import IM_PARTNER_BALANCES_ENDPOINT
from travel.rasp.train_api.train_purchase.core.enums import TrainPartner
from travel.rasp.train_api.train_purchase.core.models import PartnerBalance
from travel.rasp.train_api.train_purchase.tasks import check_balance
from travel.rasp.train_api.train_purchase.tasks.check_balance import check_im_balance

pytestmark = [pytest.mark.mongouser, pytest.mark.dbuser]

IM_RESPONSE = '''{
  "AccountBalances": [
    {
      "AccountName": "Основной счет",
      "CurrentBalance": "82 499 866,13"
    }
  ]
}'''


@replace_now('2019-08-01 17:00:00')
@replace_dynamic_setting('TRAIN_PURCHASE_IM_BALANCE_THRESHOLD', '500000.00')
@mock.patch.object(check_balance, 'guaranteed_send_email', autospec=True)
def test_check_im_balance_just_update_db(m_guaranteed_send_email, httpretty):
    mock_im(httpretty, IM_PARTNER_BALANCES_ENDPOINT, body=IM_RESPONSE)
    PartnerBalance.objects.create(partner=TrainPartner.IM, updated_at=datetime(2017, 1, 1))
    check_im_balance()

    partner_balance = PartnerBalance.objects.get(partner=TrainPartner.IM)
    assert partner_balance.balance == Decimal('82499866.13')
    assert partner_balance.updated_at == now_utc()
    assert m_guaranteed_send_email.call_count == 0


@replace_now('2019-08-01 17:00:00')
@replace_dynamic_setting('TRAIN_PURCHASE_IM_BALANCE_THRESHOLD', '500000.00')
@mock.patch.object(check_balance, 'guaranteed_send_email', autospec=True)
def test_check_im_balance_first_launch(m_guaranteed_send_email, httpretty):
    mock_im(httpretty, IM_PARTNER_BALANCES_ENDPOINT, body=IM_RESPONSE)

    assert not PartnerBalance.objects.filter(partner=TrainPartner.IM)
    check_im_balance()

    partner_balance = PartnerBalance.objects.get(partner=TrainPartner.IM)
    assert partner_balance.balance == Decimal('82499866.13')
    assert partner_balance.updated_at == now_utc()
    assert m_guaranteed_send_email.call_count == 0


@replace_now('2019-08-01 17:00:00')
@replace_dynamic_setting('TRAIN_PURCHASE_IM_BALANCE_THRESHOLD', '900000000.00')
@pytest.mark.parametrize('last_balance, expect_alert', [
    (Decimal('900000001.00'), True),
    (Decimal('90000000.00'), False),
])
@mock.patch.object(check_balance, 'guaranteed_send_email', autospec=True)
def test_check_im_balance_low(m_guaranteed_send_email, last_balance, expect_alert, httpretty):
    mock_im(httpretty, IM_PARTNER_BALANCES_ENDPOINT, body=IM_RESPONSE)
    PartnerBalance.objects.create(partner=TrainPartner.IM, updated_at=datetime(2017, 1, 1),
                                  balance=last_balance)
    check_im_balance()

    partner_balance = PartnerBalance.objects.get(partner=TrainPartner.IM)
    assert partner_balance.balance == Decimal('82499866.13')
    assert partner_balance.updated_at == now_utc()
    assert m_guaranteed_send_email.call_count == (1 if expect_alert else 0)
