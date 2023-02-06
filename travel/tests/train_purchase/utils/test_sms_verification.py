# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import timedelta

import mock
import pytest
from hamcrest import assert_that, has_entries

from common.tester.utils.datetime import replace_now
from travel.rasp.library.python.common23.date.environment import now
from travel.rasp.train_api.train_purchase.core.factories import TrainPurchaseSmsVerificationFactory
from travel.rasp.train_api.train_purchase.core.sms_verification import SmsThrottlingError
from travel.rasp.train_api.train_purchase.utils import sms_verification
from travel.rasp.train_api.train_purchase.utils.sms_verification import verify_sms

pytestmark = [pytest.mark.mongouser, pytest.mark.dbuser]


@pytest.fixture(autouse=True, name='m_log')
def _mock_logger():
    with mock.patch.object(sms_verification, 'log', autospec=True) as m_log:
        yield m_log


def test_verify_sms_request(m_log):
    train_purchase_sms_verification = TrainPurchaseSmsVerificationFactory(code='generated_code')

    with mock.patch.object(
            sms_verification, 'send_verification_sms', autospec=True,
            return_value=train_purchase_sms_verification,
    ) as m_send_verification_sms:
        response = verify_sms(
            request_sms_verification=True,
            sms_verification_code=train_purchase_sms_verification.code,
            phone=train_purchase_sms_verification.phone,
            action_name=train_purchase_sms_verification.action_name,
            action_data=train_purchase_sms_verification.action_data,
            message_template='',
        )

    assert m_send_verification_sms.call_count == 1
    m_log.info.assert_called_once_with('Отправлен код generated_code по запросу на 1111, action testing')
    assert_that(response.data, has_entries('smsSent', True))


def test_verify_sms_request_throttling(m_log):
    train_purchase_sms_verification = TrainPurchaseSmsVerificationFactory(code='generated_code')

    with mock.patch.object(
            sms_verification, 'send_verification_sms', autospec=True,
            side_effect=[SmsThrottlingError(train_purchase_sms_verification)],
    ) as m_send_verification_sms:
        response = verify_sms(
            request_sms_verification=True,
            sms_verification_code=train_purchase_sms_verification.code,
            phone=train_purchase_sms_verification.phone,
            action_name=train_purchase_sms_verification.action_name,
            action_data=train_purchase_sms_verification.action_data,
            message_template='',
        )

    assert m_send_verification_sms.call_count == 1
    m_log.exception.assert_called_once_with('Превышен лимит сообщений для 1111, action testing')
    assert_that(
        response.data,
        has_entries('errors', has_entries('sms_validation_error', has_entries('type', 'sms_validation_error'))),
    )


@replace_now('2019-01-01')
@pytest.mark.parametrize('code, action_data, sent_at_delta, expected_used, expected_message, expected_response', [
    (
        'verification_code',
        {},
        timedelta(hours=-1),
        False,
        'Нет актуальных сообщений для 1111, action testing',
        has_entries('errors', has_entries('sms_validation_error', has_entries('type', 'sms_validation_error'))),
    ),
    (
        'verification_code',
        {'invalid': 'data'},
        timedelta(hours=0),
        True,
        'Код верификации verification_code верный, action_data некорректный для 1111, action testing',
        has_entries('errors', has_entries('sms_validation_error', has_entries('type', 'sms_validation_error'))),
    ),
    (
        'invalid_code',
        {},
        timedelta(hours=0),
        False,
        'Указан неверный код верификации invalid_code для 1111, action testing',
        has_entries('errors', has_entries('sms_validation_error', has_entries('type', 'sms_validation_error'))),
    ),
    (
        '',
        {},
        timedelta(hours=0),
        False,
        'Не указан код верификации для 1111, action testing',
        has_entries('errors', has_entries('sms_validation_error', has_entries('type', 'sms_validation_error'))),
    ),
    (
        'verification_code',
        {},
        timedelta(hours=0),
        True,
        'Успешно верифицирован код verification_code для 1111, action testing',
        None,
    ),
])
def test_verify_sms(m_log, code, action_data, sent_at_delta, expected_used, expected_message, expected_response):
    train_purchase_sms_verification = TrainPurchaseSmsVerificationFactory(sent_at=now() + sent_at_delta)

    response = verify_sms(
        request_sms_verification=False,
        sms_verification_code=code,
        phone=train_purchase_sms_verification.phone,
        action_name=train_purchase_sms_verification.action_name,
        action_data=action_data,
        message_template='',
    )

    train_purchase_sms_verification.reload()

    assert train_purchase_sms_verification.used == expected_used
    m_log.info.assert_called_once_with(expected_message)
    if not expected_response:
        assert not response
    else:
        assert_that(response.data, expected_response)
