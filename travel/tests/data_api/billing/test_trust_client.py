# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import json
import re
from decimal import Decimal

import mock
import pytest
from hamcrest import assert_that, has_entries, anything

import common
from common.data_api.billing.trust_client import (
    FiscalNdsType, TrustClient, TrustClientRequestError, TrustClientException, TrustClientInvalidStatus,
    TrustPaymentOrder, TrustPaymentStatuses, TrustRefundOrder, TrustRefundStatuses, WrongFiscalNdsType,
    TrustClientConnectionError, TrustClientHttpError, measurable
)
from common.tester.utils.replace_setting import replace_setting, replace_dynamic_setting
from common.utils.yasmutil import Metric

FAKE_PARTNER_ID = 42
FAKE_URL = 'http://example.com/some-method/'


def raise_unexpected_error(*args):
    raise Exception('Unexpected error: {}'.format(str(*args)))


class TestFiscalNdsType(object):
    @pytest.mark.parametrize('rate, expected', (
        (None, FiscalNdsType.NDS_NONE),
        (Decimal(0), FiscalNdsType.NDS_0),
        (Decimal(100) * (Decimal(10) / Decimal(110)), FiscalNdsType.NDS_10_110),
        (Decimal(100) * (Decimal(20) / Decimal(120)), FiscalNdsType.NDS_20_120),
        (Decimal(20), FiscalNdsType.NDS_20),
    ))
    def test_from_rate(self, rate, expected):
        assert FiscalNdsType.from_rate(rate) == expected

    def test_from_rate_exception(self):
        with pytest.raises(WrongFiscalNdsType):
            FiscalNdsType.from_rate(Decimal(100500))


@pytest.mark.skip('strange behaviour')
def test_create_partner(httpretty):
    httpretty.register_uri(httpretty.POST, FAKE_URL + 'partners', status=200,
                           body=json.dumps({'status': 'success', 'partner_id': FAKE_PARTNER_ID}))

    trust_client = TrustClient()
    with mock.patch.object(trust_client, 'trust_url', FAKE_URL):
        new_partner_id = trust_client.create_partner()

    assert new_partner_id == FAKE_PARTNER_ID
    assert len(httpretty.latest_requests) == 1


@replace_setting('APPLIED_CONFIG', 'production')
def test_create_partner_error_in_production():
    trust_client = TrustClient()
    with pytest.raises(TrustClientException):
        trust_client.create_partner()


@pytest.mark.parametrize('service_fee, expected_service_fee', [
    (None, 1),
    (0, 0),
    (2, 2),
])
def test_create_product(httpretty, service_fee, expected_service_fee):
    httpretty.register_uri(httpretty.POST, FAKE_URL + 'products', status=200,
                           body=json.dumps({'status': 'success'}))

    trust_client = TrustClient()
    product_name = 'p.name'
    product_id = 'ufs-ticket'
    with mock.patch.object(trust_client, 'trust_url', FAKE_URL):
        new_product_id = trust_client.create_product(FAKE_PARTNER_ID, product_name, product_id,
                                                     is_service_fee=True, service_fee=service_fee)

    assert product_id == new_product_id
    assert len(httpretty.latest_requests) == 1
    assert httpretty.last_request.parsed_body == {
        'name': product_name,
        'partner_id': FAKE_PARTNER_ID,
        'product_id': product_id,
        'service_fee': expected_service_fee,
    }


def test_create_order(httpretty):
    product_id = 'ufs-ticket'
    order_id = 100500
    httpretty.register_uri(httpretty.POST, FAKE_URL + 'orders', status=200, body=json.dumps({
        'status': 'success',
        'status_code': 'created',
        'order_id': order_id,
        'product_id': product_id
    }))

    trust_client = TrustClient(user_ip='1.1.1.1', user_region_id=54, user_passport_uid='256215129')
    with mock.patch.object(trust_client, 'trust_url', FAKE_URL):
        new_order_id = trust_client.create_order(product_id)

    assert new_order_id == order_id
    assert len(httpretty.latest_requests) == 1
    assert httpretty.last_request.headers['X-User-Ip'] == '1.1.1.1'
    assert httpretty.last_request.headers['X-Uid'] == '256215129'
    assert httpretty.last_request.headers['X-Region-Id'] == '54'


def test_create_payment(httpretty):
    purchase_token = 'ae8b664f529574e086f8fd058be2488d'
    httpretty.register_uri(httpretty.POST, FAKE_URL + 'payments', status=200,
                           body=json.dumps({'status': 'success', 'purchase_token': purchase_token}))

    trust_client = TrustClient(user_ip='2.2.2.2', user_region_id=1, user_passport_uid='777777777')
    payment_orders = [
        TrustPaymentOrder(order_id=100500, price=Decimal('100.00')),
        TrustPaymentOrder(order_id=100501, price=Decimal('666.66'))
    ]
    payment_timeout = 444.0
    with mock.patch.object(trust_client, 'trust_url', FAKE_URL):
        new_purchase_token = trust_client.create_payment(
            payment_orders,
            payment_timeout,
            pass_params={'terminal_route_data': {'description': 'testing'}},
        )

    assert new_purchase_token == purchase_token
    assert len(httpretty.latest_requests) == 1
    assert_that(httpretty.last_request.headers, has_entries({
        'X-User-Ip': '2.2.2.2',
        'X-Uid': '777777777',
        'X-Region-Id': '1',
    }))
    assert_that(httpretty.last_request.parsed_body, has_entries({
        'pass_params': {'terminal_route_data': {'description': 'testing'}},
    }))


def test_start_payment(httpretty):
    purchase_token = 'ae8b664f529574e086f8fd058be2488d'
    payment_url = 'https://tmongo1f.fin.yandex.ru/web/payment?purchase_token=edfc668663362246c7a5df853bd5459b'
    httpretty.register_uri(httpretty.POST, re.compile(FAKE_URL + 'payments/\w+/start'), status=200, body=json.dumps({
        'status': 'success',
        'payment_url': payment_url,
        'payment_status': 'started',
        'start_ts': '1490875571.969',
        'orders': [
            {
                'product_type': 't1',
                'product_id': '77888800112233442',
                'paid_amount': '0.00',
                'order_ts': '1490870400.000',
                'current_qty': '0.00',
                'order_id': '785580256',
                'orig_amount': '666.66',
                'product_name': 'ticket'
            }, {
                'product_type': 't1',
                'product_id': '77888800112233442',
                'paid_amount': '0.00',
                'order_ts': '1490871848.000',
                'current_qty': '0.00',
                'order_id': '785581204',
                'orig_amount': '100.00',
                'product_name': 'ticket'
            }
        ],
        'currency': 'RUB',
        'amount': '766.66',
        'payment_timeout': '444.000',
        'purchase_token': purchase_token
    }))

    trust_client = TrustClient()
    with mock.patch.object(trust_client, 'trust_url', FAKE_URL):
        new_payment_url = trust_client.start_payment(purchase_token)

    assert new_payment_url == payment_url
    assert len(httpretty.latest_requests) == 1


def test_get_payment_info(httpretty):
    purchase_token = '100500'
    payment_status = TrustPaymentStatuses.AUTHORIZED
    httpretty.register_uri(httpretty.GET, re.compile(FAKE_URL + 'payments/\w+'), status=200, body=json.dumps({
        'status': 'success',
        'payment_status': payment_status.value,
        'payment_resp_code': '42',
        'payment_resp_desc': '42 description'
    }))

    trust_client = TrustClient()
    with mock.patch.object(trust_client, 'trust_url', FAKE_URL):
        payment_info = trust_client.get_payment_info(purchase_token)

    assert payment_info.status == TrustPaymentStatuses.AUTHORIZED
    assert payment_info.resp_code == '42'
    assert payment_info.resp_desc == '42 description'
    assert len(httpretty.latest_requests) == 1


def _prepare_fake_raw_payment_info(
        httpretty,
        purchase_token,
        trust_payment_id='fake_trust_payment_id',
        payment_status=TrustPaymentStatuses.STARTED,
):
    httpretty.register_uri(httpretty.GET, re.compile(FAKE_URL + 'payments/\w+'), status=200, body=json.dumps({
        'status': 'success',
        'payment_status': payment_status.value,
        'orders': [
            {
                'product_type': 't1',
                'product_id': '77888800112233442',
                'paid_amount': '0.00',
                'order_ts': '1490870400.000',
                'current_qty': '0.00',
                'order_id': '785580256',
                'orig_amount': '666.66',
                'product_name': 'ticket'
            }, {
                'product_type': 't1',
                'product_id': '77888800112233442',
                'paid_amount': '0.00',
                'order_ts': '1490871848.000',
                'current_qty': '0.00',
                'order_id': '785581204',
                'orig_amount': '100.00',
                'product_name': 'ticket'
            }
        ],
        'currency': 'RUB',
        'amount': '766.66',
        'payment_timeout': '444.000',
        'purchase_token': purchase_token,
        'trust_payment_id': trust_payment_id,
        'fiscal_receipt_url': 'http://fiscal-receipt.url',
        'fiscal_receipt_clearing_url': 'http://fiscal-receipt-clearing.url',
    }))


def test_get_raw_payment_info(httpretty):
    purchase_token = 'ae8b664f529574e086f8fd058be2488d'
    trust_payment_id = '5be2cee8910d394070616edf'
    payment_status = TrustPaymentStatuses.STARTED
    _prepare_fake_raw_payment_info(httpretty, purchase_token, trust_payment_id, payment_status)

    trust_client = TrustClient()
    with mock.patch.object(trust_client, 'trust_url', FAKE_URL):
        payment_info = trust_client.get_raw_payment_info(purchase_token)

    assert_that(payment_info, has_entries({
        'status': 'success',
        'payment_status': payment_status.value,
        'orders': anything(),
        'currency': anything(),
        'amount': anything(),
        'payment_timeout': anything(),
        'purchase_token': purchase_token,
        'trust_payment_id': trust_payment_id,
    }))
    assert len(httpretty.latest_requests) == 1
    assert_that(httpretty.last_request.querystring, has_entries({'show_trust_payment_id': ['1']}))


def test_get_receipt_url(httpretty):
    purchase_token = 'ae8b664f529574e086f8fd058be2488d'
    _prepare_fake_raw_payment_info(httpretty, purchase_token)
    trust_client = TrustClient()
    with mock.patch.object(trust_client, 'trust_url', FAKE_URL):
        url = trust_client.get_receipt_url(purchase_token)
    assert url == 'http://fiscal-receipt.url'


def test_get_receipt_clearing_url(httpretty):
    purchase_token = 'ae8b664f529574e086f8fd058be2488d'
    _prepare_fake_raw_payment_info(httpretty, purchase_token)
    trust_client = TrustClient()
    with mock.patch.object(trust_client, 'trust_url', FAKE_URL):
        url = trust_client.get_receipt_clearing_url(purchase_token)
    assert url == 'http://fiscal-receipt-clearing.url'


def test_get_payment_status(httpretty):
    purchase_token = 'ae8b664f529574e086f8fd058be2488d'
    payment_status = TrustPaymentStatuses.STARTED
    httpretty.register_uri(httpretty.GET, re.compile(FAKE_URL + 'payments/\w+'), status=200, body=json.dumps({
        'status': 'success',
        'payment_status': payment_status.value,
    }))

    trust_client = TrustClient()
    with mock.patch.object(trust_client, 'trust_url', FAKE_URL):
        new_payment_status = trust_client.get_payment_status(purchase_token)

    assert new_payment_status == payment_status
    assert len(httpretty.latest_requests) == 1


def test_wrong_payment_status(httpretty):
    purchase_token = 'ae8b664f529574e086f8fd058be2488d'
    payment_status = 'invalid'
    httpretty.register_uri(httpretty.GET, re.compile(FAKE_URL + 'payments/\w+'), status=200, body=json.dumps({
        'status': 'success',
        'payment_status': payment_status,
    }))

    trust_client = TrustClient()
    with mock.patch.object(trust_client, 'trust_url', FAKE_URL):
        with pytest.raises(TrustClientInvalidStatus):
            trust_client.get_payment_status(purchase_token)


def test_product_already_exists(httpretty):
    product_id = 'ticket'
    httpretty.register_uri(httpretty.GET, re.compile(FAKE_URL + 'products/\w+'), status=200, body=json.dumps({
        'name': 'p.name2',
        'partner_id': FAKE_PARTNER_ID,
        'prices': [],
        'product_id': product_id,
        'product_type': 'p.type',
        'status': 'success'
    }))

    trust_client = TrustClient()
    with mock.patch.object(trust_client, 'trust_url', FAKE_URL):
        assert trust_client.is_product_exist(product_id)
    assert len(httpretty.latest_requests) == 1


def test_no_product(httpretty):
    product_id = 'ticket'
    httpretty.register_uri(httpretty.GET, re.compile(FAKE_URL + 'products/\w+'), status=200, body=json.dumps({
        'status': 'error',
        'status_code': 'product_not_found'
    }))

    trust_client = TrustClient()
    with mock.patch.object(trust_client, 'trust_url', FAKE_URL):
        assert not trust_client.is_product_exist(product_id)
    assert len(httpretty.latest_requests) == 1


def test_product_exception_than_request_exception(httpretty):
    product_id = 'ticket'
    httpretty.register_uri(httpretty.GET, re.compile(FAKE_URL + 'products/\w+'), status=200,
                           body=raise_unexpected_error)

    trust_client = TrustClient()
    with mock.patch.object(trust_client, 'trust_url', FAKE_URL):
        with pytest.raises(TrustClientRequestError):
            trust_client.is_product_exist(product_id)


def test_partner_already_exists(httpretty):
    httpretty.register_uri(httpretty.GET, re.compile(FAKE_URL + 'partners/\w+'), status=200, body=json.dumps({
        'status': 'success',
        'city': 'Moscow',
        'id': FAKE_PARTNER_ID,
        'email': 'ufs-test@example.com',
        'name': 'УФС'
    }))

    trust_client = TrustClient()
    with mock.patch.object(trust_client, 'trust_url', FAKE_URL):
        assert trust_client.is_partner_exist(FAKE_PARTNER_ID)
    assert len(httpretty.latest_requests) == 1


def test_no_partner(httpretty):
    httpretty.register_uri(httpretty.GET, re.compile(FAKE_URL + 'partners/\w+'), status=200, body=json.dumps({
        'status': 'error',
        'status_code': 'partner_not_found'
    }))

    trust_client = TrustClient()
    with mock.patch.object(trust_client, 'trust_url', FAKE_URL):
        assert not trust_client.is_partner_exist(FAKE_PARTNER_ID)
    assert len(httpretty.latest_requests) == 1


def test_partner_exception_than_request_exception(httpretty):
    httpretty.register_uri(httpretty.GET, re.compile(FAKE_URL + 'partners/\w+'), status=200,
                           body=raise_unexpected_error)

    trust_client = TrustClient()
    with mock.patch.object(trust_client, 'trust_url', FAKE_URL):
        with pytest.raises(TrustClientRequestError):
            trust_client.is_partner_exist(FAKE_PARTNER_ID)


def test_clear_payment_ok(httpretty):
    purchase_token = 'ae8b664f529574e086f8fd058be2488d'
    httpretty.register_uri(httpretty.POST, re.compile(FAKE_URL + 'payments/\w+/clear'), status=200, body=json.dumps({
        'status': 'success',
        'status_code': 'payment_is_updated'
    }))

    trust_client = TrustClient()
    with mock.patch.object(trust_client, 'trust_url', FAKE_URL):
        trust_client.clear_payment(purchase_token)

    assert len(httpretty.latest_requests) == 1


def test_clear_payment_fail(httpretty):
    purchase_token = 'ae8b664f529574e086f8fd058be2488d'
    httpretty.register_uri(httpretty.POST, re.compile(FAKE_URL + 'payments/\w+/clear'), status=200, body=json.dumps({
        'status': 'error',
        'status_code': 'invalid_state'
    }))

    trust_client = TrustClient()
    with mock.patch.object(trust_client, 'trust_url', FAKE_URL):
        with pytest.raises(TrustClientInvalidStatus):
            trust_client.clear_payment(purchase_token)


def test_unhold_payment_ok(httpretty):
    purchase_token = 'ae8b664f529574e086f8fd058be2488d'
    httpretty.register_uri(httpretty.POST, FAKE_URL + 'payments/' + purchase_token + '/unhold', status=200,
                           body=json.dumps({'status': 'success', 'status_code': 'payment_is_updated'}))

    trust_client = TrustClient()
    with mock.patch.object(trust_client, 'trust_url', FAKE_URL):
        trust_client.unhold_payment(purchase_token)

    assert len(httpretty.latest_requests) == 1


def test_unhold_payment_fail(httpretty):
    purchase_token = 'ae8b664f529574e086f8fd058be2488d'
    httpretty.register_uri(httpretty.POST, FAKE_URL + 'payments/' + purchase_token + '/unhold', status=200,
                           body=json.dumps({'status': 'error'}))

    trust_client = TrustClient()
    with mock.patch.object(trust_client, 'trust_url', FAKE_URL):
        with pytest.raises(TrustClientException):
            trust_client.unhold_payment(purchase_token)


def test_create_refund(httpretty):
    purchase_token = 'ae8b664f529574e086f8fd058be2488d'
    trust_refund_id = '590871ba795be2183d81c142'
    httpretty.register_uri(httpretty.POST, FAKE_URL + 'refunds', status=200,
                           body=json.dumps({'status': 'success', 'trust_refund_id': trust_refund_id}))

    trust_client = TrustClient(user_ip='3.3.3.3', user_region_id=213, user_passport_uid='123456789')
    refund_orders = [
        TrustRefundOrder(order_id=100500, delta_amount='100.00'),
        TrustRefundOrder(order_id=100501, delta_amount='666.66')
    ]
    with mock.patch.object(trust_client, 'trust_url', FAKE_URL):
        new_trust_refund_id = trust_client.create_refund(refund_orders, purchase_token)
    assert new_trust_refund_id == trust_refund_id
    assert len(httpretty.latest_requests) == 1
    assert httpretty.last_request.headers['X-User-Ip'] == '3.3.3.3'
    assert httpretty.last_request.headers['X-Uid'] == '123456789'
    assert httpretty.last_request.headers['X-Region-Id'] == '213'


def test_start_refund(httpretty):
    trust_refund_id = '590871ba795be2183d81c142'
    httpretty.register_uri(httpretty.POST, re.compile(FAKE_URL + 'refunds/\w+/start'), status=200, body=json.dumps({
        'status': 'wait_for_notification',
        'status_desc': 'refund is in queue'
    }))

    trust_client = TrustClient()
    with mock.patch.object(trust_client, 'trust_url', FAKE_URL):
        trust_client.start_refund(trust_refund_id)

    assert len(httpretty.latest_requests) == 1


RECEIPT_BODY = {
    'id': 149,
    'dt': '2017-08-16 12:43:22',
    'fp': 2150738395,
    'document_index': 30,
    'shift_number': 2,
    'ofd_ticket_received': False,
    'receipt_content': {
        'firm_inn': '7736207543',
        'taxation_type': 'OSN',
        'agent_type': 'agent',
        'client_email_or_phone': 'lorekhov@yandex-team.ru',
        'receipt_type': 'income',
        'payments': [
            {
                'amount': '3000.00',
                'payment_type': 'card'
            }
        ],
        'rows': [
            {
                'payment_type_type': 'prepayment',
                'price': '1000.00',
                'qty': '1',
                'tax_type': 'nds_18',
                'text': 'order-1'
            },
            {
                'payment_type_type': 'prepayment',
                'price': '2000.00',
                'qty': '1',
                'tax_type': 'nds_18',
                'text': 'order-2'
            }
        ]
    },
    'receipt_calculated_content': {
        'total': '3000.00',
        'money_received_total': '3000.00',
        'rows': [
            {
                'qty': '1',
                'price': '1000.00',
                'payment_type_type': 'prepayment',
                'amount': '1000.00',
                'tax_pct': '18.00',
                'tax_amount': '152.54',
                'tax_type': 'nds_18',
                'text': 'order-1'
            },
            {
                'qty': '1',
                'price': '2000.00',
                'payment_type_type': 'prepayment',
                'amount': '2000.00',
                'tax_pct': '18.00',
                'tax_amount': '305.08',
                'tax_type': 'nds_18',
                'text': 'order-2'
            }
        ],
        'tax_totals': [
            {
                'tax_type': 'nds_18',
                'tax_pct': '18.00',
                'tax_amount': '457.62'
            }
        ],
        'totals': [
            {
                'payment_type': 'card',
                'amount': '3000.00'
            }
        ],
        'firm_reply_email': 'isupov@yandex-team.ru',
        'firm_url': 'yandex.ru'
    },
    'firm': {
        'inn': '7736207543',
        'name': 'ООО "ЯНДЕКС"',
        'reply_email': 'isupov@yandex-team.ru'
    },
    'fn': {
        'sn': '9999078900005747',
        'model': 'ФН-1'
    },
    'ofd': {
        'inn': '7704358518',
        'name': 'ОБЩЕСТВО С ОГРАНИЧЕННОЙ ОТВЕТСТВЕННОСТЬЮ \'ЯНДЕКС.ОФД\'',
        'check_url': 'nalog.ru'
    },
    'kkt': {
        'sn': '00000000381001543159',
        'rn': '1709430568058600',
        'url': 'yandex.ru',
        'automatic_machine_number': '6660666'
    },
    'location': {
        'id': '666',
        'address': '119021, Россия, г. Москва, ул. Льва Толстого, д. 16',
        'description': 'Стойка номер 666'
    },
    'origin': 'online',
    'check_url': 'https://check.greed-ts.yandex.ru/?n=149&fn=9999078900005747&fpd=2150738395',
    'localzone': 'Europe/Moscow'
}


def _prepare_fake_refund_info(
        httpretty,
        refund_status=TrustRefundStatuses.WAIT_FOR_NOTIFICATION,
):
    httpretty.register_uri(httpretty.GET, re.compile(FAKE_URL + 'refunds/\w+'), status=200, body=json.dumps({
        'fiscal_receipt_url': 'http://fiscal-receipt.url',
        'status': refund_status.value,
        'status_desc': 'refund sent to payment system',
    }))


def test_get_refund_info(httpretty):
    trust_refund_id = '590871ba795be2183d81c142'
    refund_status = TrustRefundStatuses.WAIT_FOR_NOTIFICATION
    _prepare_fake_refund_info(httpretty, refund_status)

    trust_client = TrustClient()
    with mock.patch.object(trust_client, 'trust_url', FAKE_URL):
        refund_info = trust_client.get_refund_info(trust_refund_id)

    assert_that(refund_info, has_entries({
        'status': refund_status.value,
        'status_desc': anything()
    }))
    assert len(httpretty.latest_requests) == 1


def test_get_refund_receipt_url(httpretty):
    trust_refund_id = '590871ba795be2183d81c142'
    _prepare_fake_refund_info(httpretty)
    trust_client = TrustClient()
    with mock.patch.object(trust_client, 'trust_url', FAKE_URL):
        url = trust_client.get_refund_receipt_url(trust_refund_id)
    assert url == 'http://fiscal-receipt.url'


def test_get_refund_status(httpretty):
    trust_refund_id = '590871ba795be2183d81c142'
    refund_status = TrustRefundStatuses.WAIT_FOR_NOTIFICATION
    httpretty.register_uri(httpretty.GET, re.compile(FAKE_URL + 'refunds/\w+'), status=200, body=json.dumps({
        'status': refund_status.value,
        'status_desc': 'refund sent to payment system'
    }))

    trust_client = TrustClient()
    with mock.patch.object(trust_client, 'trust_url', FAKE_URL):
        new_refund_status = trust_client.get_refund_status(trust_refund_id)

    assert new_refund_status == refund_status
    assert len(httpretty.latest_requests) == 1


def test_wrong_refund_status(httpretty):
    trust_refund_id = '590871ba795be2183d81c142'
    payment_status = 'invalid'
    httpretty.register_uri(httpretty.GET, re.compile(FAKE_URL + 'refunds/\w+'), status=200, body=json.dumps({
        'status': 'payment_status',
        'payment_status': payment_status,
    }))

    trust_client = TrustClient()
    with mock.patch.object(trust_client, 'trust_url', FAKE_URL):
        with pytest.raises(TrustClientInvalidStatus):
            trust_client.get_refund_status(trust_refund_id)


def test_is_payment_cleared(httpretty):
    purchase_token = '100500'
    payment_status = TrustPaymentStatuses.AUTHORIZED
    httpretty.register_uri(httpretty.GET, re.compile(FAKE_URL + 'payments/\w+'), status=200, body=json.dumps({
        'status': 'success',
        'payment_status': payment_status.value,
        'clear_real_ts': '4321'
    }))

    trust_client = TrustClient()
    with mock.patch.object(trust_client, 'trust_url', FAKE_URL):
        is_payment_cleared = trust_client.is_payment_cleared(purchase_token)

    assert is_payment_cleared


def test_clear_payment_order_ok(httpretty):
    purchase_token = 'ae8b664f529574e086f8fd058be2488d'
    order_id = '5124388031'
    httpretty.register_uri(
        httpretty.POST, re.compile(FAKE_URL + 'payments/\w+/orders/\d+/clear'), status=200, body=json.dumps({
            'status': 'success',
            'status_code': 'payment_is_updated'
        })
    )

    trust_client = TrustClient()
    with mock.patch.object(trust_client, 'trust_url', FAKE_URL):
        trust_client.clear_payment_order(purchase_token, order_id)

    assert len(httpretty.latest_requests) == 1


def test_clear_payment_order_fail(httpretty):
    purchase_token = 'ae8b664f529574e086f8fd058be2488d'
    order_id = '5124388031'
    httpretty.register_uri(
        httpretty.POST, re.compile(FAKE_URL + 'payments/\w+/orders/\d+/clear'), status=200, body=json.dumps({
            'status': 'error',
            'status_code': 'invalid_state'
        })
    )

    trust_client = TrustClient()
    with mock.patch.object(trust_client, 'trust_url', FAKE_URL):
        with pytest.raises(TrustClientInvalidStatus):
            trust_client.clear_payment_order(purchase_token, order_id)


def test_unhold_payment_order_ok(httpretty):
    purchase_token = 'ae8b664f529574e086f8fd058be2488d'
    order_id = '5124388031'
    httpretty.register_uri(
        httpretty.POST, re.compile(FAKE_URL + 'payments/\w+/orders/\d+/unhold'), status=200, body=json.dumps({
            'status': 'success',
            'status_code': 'payment_is_updated'
        })
    )

    trust_client = TrustClient()
    with mock.patch.object(trust_client, 'trust_url', FAKE_URL):
        trust_client.unhold_payment_order(purchase_token, order_id)

    assert len(httpretty.latest_requests) == 1


def test_unhold_payment_order_fail(httpretty):
    purchase_token = 'ae8b664f529574e086f8fd058be2488d'
    order_id = '5124388031'
    httpretty.register_uri(
        httpretty.POST, re.compile(FAKE_URL + 'payments/\w+/orders/\d+/unhold'), status=200, body=json.dumps({
            'status': 'error',
            'status_code': 'invalid_state'
        })
    )

    trust_client = TrustClient()
    with mock.patch.object(trust_client, 'trust_url', FAKE_URL):
        with pytest.raises(TrustClientInvalidStatus):
            trust_client.unhold_payment_order(purchase_token, order_id)


def test_resize_payment_order_ok(httpretty):
    purchase_token = 'ae8b664f529574e086f8fd058be2488d'
    order_id = '5124388031'
    httpretty.register_uri(
        httpretty.POST, re.compile(FAKE_URL + 'payments/\w+/orders/\d+/resize'), status=200, body=json.dumps({
            'status': 'success',
            'status_code': 'payment_is_updated'
        })
    )

    trust_client = TrustClient()
    with mock.patch.object(trust_client, 'trust_url', FAKE_URL):
        trust_client.resize_payment_order(purchase_token, order_id, Decimal('200.00'))

    assert len(httpretty.latest_requests) == 1
    assert httpretty.latest_requests[0].parsed_body == {'amount': 200.0, 'data': None, 'qty': 0}


def test_resize_payment_order_fail(httpretty):
    purchase_token = 'ae8b664f529574e086f8fd058be2488d'
    order_id = '5124388031'
    httpretty.register_uri(
        httpretty.POST, re.compile(FAKE_URL + 'payments/\w+/orders/\d+/resize'), status=200, body=json.dumps({
            'status': 'error',
            'status_code': 'invalid_state'
        })
    )

    trust_client = TrustClient()
    with mock.patch.object(trust_client, 'trust_url', FAKE_URL):
        with pytest.raises(TrustClientInvalidStatus):
            trust_client.resize_payment_order(purchase_token, order_id, Decimal('200.00'))


@pytest.mark.parametrize('use_prod, verify, expected', [
    (True, 'foo', True),
    (False, 'foo', 'foo'),
    (False, False, False)
])
def test_get_verify(use_prod, verify, expected):
    trust_client = TrustClient()
    with replace_dynamic_setting('TRUST_USE_PRODUCTION_FOR_TESTING', use_prod), \
            replace_setting('TRUST_VERIFY', verify):
        assert trust_client._get_verify() == expected


class TestMeasurable(object):
    @replace_setting('YASMAGENT_ENABLE_MEASURABLE', True)
    @pytest.mark.parametrize('exception, metric_name', [
        (TrustClientConnectionError('foo'), 'connection_errors_cnt'),
        (TrustClientHttpError('bar'), 'http_errors_cnt'),
        (TrustClientInvalidStatus('baz'), 'invalid_status_errors_cnt')
    ])
    def test_handle_error(self, exception, metric_name):
        @measurable(buckets=[1])
        def failing_func():
            raise exception

        assert measurable.prefix == 'trust'
        with mock.patch.object(common.utils.yasmutil, 'YasmMetricSender') as m_yasm_metric_sender:
            with pytest.raises(exception.__class__):
                failing_func()

            assert m_yasm_metric_sender.mock_calls[1] == mock.call().send_many(
                [
                    Metric(name='failing_func.errors_cnt', value=1, suffix='ammm'),
                    Metric(name='failing_func.{}'.format(metric_name), value=1, suffix='ammm'),
                    Metric(name='errors_cnt', value=1, suffix='ammm'),
                    Metric(name='{}'.format(metric_name), value=1, suffix='ammm'),
                    Metric(name='failing_func.timings', value=[[0, 1]], suffix='ahhh')
                ]
            )
