# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import json
from datetime import datetime, timedelta

import mock
import pytest
from hamcrest import assert_that, has_entries, contains, anything, has_entry
from rest_framework import status

from common.tester.matchers import has_json
from common.tester.utils.datetime import replace_now
from common.tester.utils.replace_setting import replace_setting, replace_dynamic_setting
from travel.rasp.train_api.helpers.error import ErrorType
from travel.rasp.train_api.train_partners import im
from travel.rasp.train_api.train_partners.base import RzhdStatus
from travel.rasp.train_api.train_partners.im.electronic_registration import (
    ELECTRONIC_REGISTRATION_ENDPOINT as IM_ELECTRONIC_REGISTRATION_ENDPOINT  # noqa
)
from travel.rasp.train_api.train_partners.im.factories.utils import mock_im
from travel.rasp.train_api.train_partners.ufs.electronic_registration import (
    ELECTRONIC_REGISTRATION_ENDPOINT as UFS_ELECTRONIC_REGISTRATION_ENDPOINT  # noqa
)
from travel.rasp.train_api.train_partners.ufs.test_utils import mock_ufs
from travel.rasp.train_api.train_partners.ufs.update_order import UPDATE_ORDER_INFO_ENDPOINT
from travel.rasp.train_api.train_purchase.core import config as train_order_config
from travel.rasp.train_api.train_purchase.core.factories import (
    PassengerFactory, PartnerDataFactory, TicketFactory, ClientContractsFactory
)
from travel.rasp.train_api.train_purchase.core.models import TrainPurchaseSmsVerification, TrainPartner
from travel.rasp.train_api.train_purchase.views.test_utils import create_order
from travel.rasp.train_api.train_purchase.views_async import generate_registration_sms_action_name

OPERATION_ID = '22'
pytestmark = [pytest.mark.dbuser, pytest.mark.mongouser, pytest.mark.usefixtures('mock_trust_client')]


def _create_order(**kwargs):
    if 'partner_data' not in kwargs:
        kwargs['partner_data'] = PartnerDataFactory(expire_set_er=datetime(2017, 11, 6) + timedelta(1),
                                                    operation_id=OPERATION_ID)
    return create_order(**kwargs)


UPDATE_ORDER_INFO_RESPONSE_TEMPLATE = """
<UFS_RZhD_Gate><Status>0</Status>
<Blank ID="1"><RzhdStatus>{0}</RzhdStatus></Blank>
<Blank ID="2"><RzhdStatus>{1}</RzhdStatus></Blank>
</UFS_RZhD_Gate>
""".strip()


def mock_update_order_info(httpretty, variants):
    responses = [
        httpretty.Response(body=UPDATE_ORDER_INFO_RESPONSE_TEMPLATE.format(*variant))
        for variant in variants
    ]

    mock_ufs(httpretty, UPDATE_ORDER_INFO_ENDPOINT, responses=responses)


@pytest.fixture(autouse=True, scope='module')
def fix_time():
    with replace_now('2017-11-05 10:00:00'):
        yield


@pytest.fixture(autouse=True, scope='module')
def fix_yasms_config():
    with replace_setting('YASMS_DONT_SEND_ANYTHING', True), \
            replace_setting('YASMS_URL', 'someurl.yandex.net'):
        yield


def get_sms_validation_code(order, blank_ids, new_status):
    sms_verification = TrainPurchaseSmsVerification(sent_at=datetime(2017, 11, 5, 9, 59), code='1111',
                                                    action_name=generate_registration_sms_action_name(blank_ids),
                                                    action_data={'uid': order.uid, 'blank_ids': blank_ids,
                                                                 'new_status': new_status},
                                                    phone=order.user_info.phone, message='1')
    sms_verification.save()
    return sms_verification.code


def test_er_expire(async_urlconf_client):
    order = _create_order(partner_data=PartnerDataFactory(expire_set_er=datetime(2001, 1, 1)))
    ClientContractsFactory(partner=order.partner)

    response = async_urlconf_client.get('/ru/api/change-registration-status/{}/'.format(order.uid), {
        'newStatus': 'DISABLED',
        'blankId': ['1', '2'],
        'SMSVerificationCode': get_sms_validation_code(order, ['1', '3'], 'DISABLED'),
    })

    assert response.status_code == 400
    assert_that(
        response.content,
        has_json(
            has_entries(
                errors=has_entries(order='Too late to change ER'))))


def test_ticket_in_wrong_status(async_urlconf_client):
    order = _create_order(
        passengers=[
            PassengerFactory(
                tickets=[
                    TicketFactory(blank_id='1', rzhd_status=RzhdStatus.REFUNDED),
                ]
            ),
        ]
    )
    ClientContractsFactory(partner=order.partner)

    response = async_urlconf_client.get('/ru/api/change-registration-status/{}/'.format(order.uid), {
        'newStatus': 'DISABLED',
        'blankId': ['1']
    })

    assert response.status_code == 400
    assert_that(
        response.content,
        has_json(
            has_entries(
                errors=has_entries(blankIds="Couldn't change er for 1 with status 4"))))


def test_no_blanks(async_urlconf_client):
    order = _create_order()
    ClientContractsFactory(partner=order.partner)

    response = async_urlconf_client.get('/ru/api/change-registration-status/{}/'.format(order.uid), {
        'newStatus': 'DISABLED',
        'blankId': ['100'],
        'SMSVerificationCode': get_sms_validation_code(order, ['100'], 'DISABLED'),
    })

    assert response.status_code == 400
    assert_that(
        response.content,
        has_json(
            has_entries(
                errors=has_entries(blankIds="No blanks with ids 100"))))


def test_ufs_error(httpretty, async_urlconf_client):
    mock_ufs(httpretty, UFS_ELECTRONIC_REGISTRATION_ENDPOINT, responses=[
        httpretty.Response('''
            <UFS_RZhD_Gate><Status>1</Status><Error/><Code>6666</Code><DescrId>6666</DescrId></UFS_RZhD_Gate>
        ''')
    ])
    order = _create_order()
    ClientContractsFactory(partner=order.partner)

    response = async_urlconf_client.get('/ru/api/change-registration-status/{}/'.format(order.uid), {
        'newStatus': 'DISABLED',
        'blankId': ['1'],
        'SMSVerificationCode': get_sms_validation_code(order, ['1'], 'DISABLED'),
    })

    assert response.status_code == 500
    assert_that(
        response.data,
        has_entry('errors',
                  has_entry(ErrorType.PARTNER_ERROR, has_entry('data', has_entries(code=6666, description=""))))
    )
    order.reload()
    assert order.passengers[0].tickets[0].pending
    assert not order.passengers[1].tickets[0].pending


def test_successful_ufs(httpretty, async_urlconf_client):
    mock_ufs(httpretty, UFS_ELECTRONIC_REGISTRATION_ENDPOINT, responses=[
        httpretty.Response('''
            <UFS_RZhD_Gate><Status>0</Status></UFS_RZhD_Gate>
        ''')
    ])
    order = _create_order()
    ClientContractsFactory(partner=order.partner)

    response = async_urlconf_client.get('/ru/api/change-registration-status/{}/'.format(order.uid), {
        'newStatus': 'DISABLED',
        'blankId': ['1', '2'],
        'SMSVerificationCode': get_sms_validation_code(order, ['1', '2'], 'DISABLED'),
    })
    order.reload()

    assert response.status_code == 200
    assert_that(
        json.loads(response.content),
        has_entries(order=has_entries(passengers=contains(
            has_entries(tickets=contains(has_entries(blankId='1', rzhdStatus=RzhdStatus.NO_REMOTE_CHECK_IN.name))),
            has_entries(tickets=contains(has_entries(blankId='2', rzhdStatus=RzhdStatus.NO_REMOTE_CHECK_IN.name)))
        )))
    )


@mock.patch.object(im, 'get_route_info', return_value=None, autospec=True)
def test_successful_im(m_, httpretty, async_urlconf_client):
    mock_im(httpretty, IM_ELECTRONIC_REGISTRATION_ENDPOINT, json={
        'ExpirationElectronicRegistrationDateTime': '2017-11-06T19:16:17'
    })
    order = _create_order(partner=TrainPartner.IM)
    ClientContractsFactory(partner=order.partner)

    response = async_urlconf_client.get('/ru/api/change-registration-status/{}/'.format(order.uid), {
        'newStatus': 'DISABLED',
        'blankId': ['1', '2'],
        'SMSVerificationCode': get_sms_validation_code(order, ['1', '2'], 'DISABLED'),
    })
    order.reload()

    assert response.status_code == 200
    assert_that(
        json.loads(response.content),
        has_entries(order=has_entries(passengers=contains(
            has_entries(tickets=contains(has_entries(blankId='1', rzhdStatus=RzhdStatus.NO_REMOTE_CHECK_IN.name))),
            has_entries(tickets=contains(has_entries(blankId='2', rzhdStatus=RzhdStatus.NO_REMOTE_CHECK_IN.name)))
        )))
    )
    assert_that(httpretty.last_request.body, has_json(has_entries({
        'OrderItemBlankIds': ['1', '2'],
        'OrderItemId': int(OPERATION_ID),
        'SendNotification': False,
        'Set': False
    })))


def test_request_sms_verification(async_urlconf_client):
    order = _create_order()
    ClientContractsFactory(partner=order.partner)

    response = async_urlconf_client.get('/ru/api/change-registration-status/{}/'.format(order.uid), {
        'newStatus': 'DISABLED',
        'blankId': ['1', '2'],
        'requestSMSVerification': True
    })
    assert_that(json.loads(response.content), has_entries({
        'smsSent': True,
        'canSendNextSmsAfter': train_order_config.TRAIN_PURCHASE_VERIFICATION_SMS_THROTTLING_TIMEOUT.total_seconds(),
        'smsExpiredAfter': train_order_config.TRAIN_PURCHASE_VERIFICATION_SMS_LIFE_TIME.total_seconds()
    }))

    response = async_urlconf_client.get('/ru/api/change-registration-status/{}/'.format(order.uid), {
        'newStatus': 'DISABLED',
        'blankId': ['1', '2'],
        'requestSMSVerification': True
    })
    assert response.status_code == status.HTTP_429_TOO_MANY_REQUESTS
    assert_that(
        json.loads(response.content),
        has_entry('errors', has_entry(ErrorType.SMS_VALIDATION_ERROR, has_entries({
            'type': ErrorType.SMS_VALIDATION_ERROR,
            'message': anything(),
            'data': has_entries({
                'canSendNextSmsAfter': (train_order_config.TRAIN_PURCHASE_VERIFICATION_SMS_THROTTLING_TIMEOUT
                                        .total_seconds())
            })
        })))
    )


class TestHasNoSMSVerification(object):
    def test_sms_not_sent(self, async_urlconf_client):
        order = _create_order()
        ClientContractsFactory(partner=order.partner)

        response = async_urlconf_client.get('/ru/api/change-registration-status/{}/'.format(order.uid), {
            'newStatus': 'DISABLED',
            'blankId': ['1', '2'],
        })
        assert response.status_code == status.HTTP_412_PRECONDITION_FAILED
        assert_that(response.data, has_entry('errors', has_entry('sms_validation_error', has_entries({
            'type': 'sms_validation_error',
            'message': anything(),
        }))))

    def test_illegal_code(self, async_urlconf_client):
        order = _create_order()
        ClientContractsFactory(partner=order.partner)
        sms_verification = TrainPurchaseSmsVerification(sent_at=datetime(2017, 11, 5, 9, 59), code='1111',
                                                        action_data={'uid': order.uid, 'blank_ids': ['1', '3'],
                                                                     'new_status': 'DISABLED'},
                                                        phone=order.user_info.phone, message='1')
        sms_verification.save()

        response = async_urlconf_client.get('/ru/api/change-registration-status/{}/'.format(order.uid), {
            'newStatus': 'DISABLED',
            'blankId': ['1', '2'],
            'SMSVerificationCode': '2222'
        })
        assert response.status_code == status.HTTP_412_PRECONDITION_FAILED
        assert_that(json.loads(response.content), has_entry('errors', has_entry(
            ErrorType.SMS_VALIDATION_ERROR, has_entries({
                'type': 'sms_validation_error',
                'message': anything(),
            })
        )))

    def test_bad_order_uid(self, async_urlconf_client):
        order = _create_order()
        ClientContractsFactory(partner=order.partner)
        sms_verification = TrainPurchaseSmsVerification(sent_at=datetime(2017, 11, 5, 9, 59), code='1111',
                                                        action_data={'uid': order.uid + '_2323',
                                                                     'blank_ids': ['1', '3'],
                                                                     'new_status': 'DISABLED'},
                                                        phone=order.user_info.phone, message='1')
        sms_verification.save()

        response = async_urlconf_client.get('/ru/api/change-registration-status/{}/'.format(order.uid), {
            'newStatus': 'DISABLED',
            'blankId': ['1', '2'],
            'SMSVerificationCode': '1111'
        })
        assert response.status_code == status.HTTP_412_PRECONDITION_FAILED
        assert_that(json.loads(response.content), has_entry('errors', has_entry(
            ErrorType.SMS_VALIDATION_ERROR, has_entries({
                'type': ErrorType.SMS_VALIDATION_ERROR,
                'message': anything(),
            })
        )))

    def test_bad_blank_id(self, async_urlconf_client):
        order = _create_order()
        ClientContractsFactory(partner=order.partner)
        sms_verification = TrainPurchaseSmsVerification(sent_at=datetime(2017, 11, 5, 9, 59), code='1111',
                                                        action_data={'uid': order.uid + '_2323',
                                                                     'blank_ids': ['1'],
                                                                     'new_status': 'DISABLED'},
                                                        phone=order.user_info.phone, message='1')
        sms_verification.save()

        response = async_urlconf_client.get('/ru/api/change-registration-status/{}/'.format(order.uid), {
            'newStatus': 'DISABLED',
            'blankId': ['1', '2'],
            'SMSVerificationCode': '1111'
        })
        assert response.status_code == status.HTTP_412_PRECONDITION_FAILED
        assert_that(json.loads(response.content), has_entry('errors', has_entry(
            ErrorType.SMS_VALIDATION_ERROR, has_entries({
                'type': ErrorType.SMS_VALIDATION_ERROR,
                'message': anything(),
            })
        )))

    def test_outdate_code(self, async_urlconf_client):
        order = _create_order()
        ClientContractsFactory(partner=order.partner)
        sms_verification = TrainPurchaseSmsVerification(sent_at=datetime(2017, 11, 5, 8), code='1111',
                                                        action_data={'uid': order.uid + '_2323',
                                                                     'blank_ids': ['1', '3'],
                                                                     'new_status': 'DISABLED'},
                                                        phone=order.user_info.phone, message='1')
        sms_verification.save()

        response = async_urlconf_client.get('/ru/api/change-registration-status/{}/'.format(order.uid), {
            'newStatus': 'DISABLED',
            'blankId': ['1', '2'],
            'SMSVerificationCode': '1111'
        })
        assert response.status_code == status.HTTP_412_PRECONDITION_FAILED
        assert_that(json.loads(response.content), has_entry('errors', has_entry(
            ErrorType.SMS_VALIDATION_ERROR, has_entries({
                'type': 'sms_validation_error',
                'message': anything(),
            })
        )))


@replace_dynamic_setting('TRAN_PURCHASE_ENABLED_PARTNERS', '')
def test_partner_is_not_active(async_urlconf_client):
    order = _create_order()

    response = async_urlconf_client.get('/ru/api/change-registration-status/{}/'.format(order.uid), {
        'newStatus': 'DISABLED',
        'blankId': ['1'],
        'SMSVerificationCode': '111',
    })

    assert response.status_code == 400
    assert_that(response.content, has_json(
        has_entries(errors='Partner is not active')
    ))
