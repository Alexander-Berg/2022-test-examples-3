# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime, timedelta

import mock
import pytest
from hamcrest import assert_that, has_properties, has_entry

from common.tester.utils.datetime import replace_now
from common.tester.utils.replace_setting import replace_setting
from travel.rasp.train_api.train_purchase.core import config as train_order_config
from travel.rasp.train_api.train_purchase.core.models import TrainPurchaseSmsVerification
from travel.rasp.train_api.train_purchase.core.sms_verification import send_verification_sms, SmsThrottlingError

FAKE_NOW = datetime(2017, 10, 5, 10)


@pytest.yield_fixture(autouse=True)
def fix_now():
    with replace_now(FAKE_NOW):
        yield


@pytest.mark.mongouser
@replace_setting('YASMS_URL', 'https://yasms.yandex.net')
@mock.patch('random.randint', return_value=1234)
def test_send_sms_simple_way(_m_randint, httpretty):
    httpretty.register_uri(
        httpretty.GET, 'https://yasms.yandex.net/sendsms',
        body=b"""
<?xml version="1.0" encoding="utf-8"?>
<doc><message-sent id="{}" /></doc>
        """.strip().format(42)
    )

    send_verification_sms('+71234567890', 'установка пароля', '{code} Some Message', {'some_key': 'some_value'})

    sms_verification = TrainPurchaseSmsVerification.objects.get()
    assert_that(sms_verification, has_properties(
        sent_at=datetime(2017, 10, 5, 10),
        message='1234 Some Message',
        used=False,
        action_data=has_entry('some_key', 'some_value'),
        action_name='установка пароля',
        expired_after=train_order_config.TRAIN_PURCHASE_VERIFICATION_SMS_LIFE_TIME.total_seconds(),
        can_send_next_after=train_order_config.TRAIN_PURCHASE_VERIFICATION_SMS_THROTTLING_TIMEOUT.total_seconds(),
    ))
    assert sms_verification.expired_after > 0
    assert sms_verification.can_send_next_after > 0


@pytest.mark.mongouser
@pytest.mark.parametrize('sent_at_list,result', [
    ([FAKE_NOW], FAKE_NOW),
    ([FAKE_NOW - train_order_config.TRAIN_PURCHASE_VERIFICATION_SMS_LIFE_TIME - timedelta(seconds=1)], False),
    ([FAKE_NOW - timedelta(seconds=10), FAKE_NOW - timedelta(seconds=20)], FAKE_NOW - timedelta(seconds=10)),
])
def test_get_latest_sms(sent_at_list, result):
    for sent_at in sent_at_list:
        TrainPurchaseSmsVerification.objects.create(sent_at=sent_at, phone='+7111', message='asdf', code='1111',
                                                    action_name='установка пароля')

    latest_sms = TrainPurchaseSmsVerification.get_latest_live_sms('+7111', 'установка пароля')
    if not result:
        assert latest_sms is None
    else:
        assert latest_sms.sent_at == result


@replace_setting('YASMS_URL', 'https://yasms.yandex.net')
@replace_setting('YASMS_DONT_SEND_ANYTHING', True)
@pytest.mark.mongouser
def test_send_sms_duplicate_error():
    send_verification_sms('+71234567890', 'установка пароля', '{code} Some Message', {'some_key': 'some_value'})
    with pytest.raises(SmsThrottlingError):
        send_verification_sms('+71234567890', 'установка пароля', '{code} Some Message', {'some_key': 'some_value'})

    send_verification_sms('+71234567890', 'установка второго пароля', '{code} Some Message', {'some_key': 'some_value'})
