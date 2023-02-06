# coding: utf-8
from __future__ import unicode_literals

import json

import mock
import pytest

from common.data_api.billing import trust_client
from common.data_api.billing.helpers import download_receipt


@pytest.fixture(autouse=True)
def _mock_trust_urls():
    with mock.patch.object(
            trust_client.TrustClient, '_build_trust_url',
            return_value='https://trust_url/',
    ) as m_:
        yield m_


def test_download_receipt(httpretty):
    httpretty.register_uri(
        method=httpretty.GET,
        uri='https://trust-test.yandex.ru/checks/token/receipts/token?mode=pdf',
        body='pdf_body',
    )

    httpretty.register_uri(
        method=httpretty.GET,
        uri='https://trust_url/payments/token?show_trust_payment_id=1',
        body=json.dumps({
            'fiscal_receipt_url': 'https://trust-test.yandex.ru/checks/token/receipts/token?mode=mobile',
            'status': 'success',
        }),
    )

    assert download_receipt('token') == 'pdf_body'
    assert httpretty.last_request.querystring['mode'] == ['pdf']


def test_download_refund_receipt(httpretty):
    httpretty.register_uri(
        method=httpretty.GET,
        uri='https://trust-test.yandex.ru/checks/token/receipts/refund_id?mode=pdf',
        body='refund_pdf_body',
    )

    httpretty.register_uri(
        method=httpretty.GET,
        uri='https://trust_url/refunds/refund_id',
        body=json.dumps({
            'fiscal_receipt_url': 'https://trust-test.yandex.ru/checks/token/receipts/refund_id?mode=mobile',
            'status': 'success',
        }),
    )

    assert download_receipt('token', 'refund_id') == 'refund_pdf_body'
    assert httpretty.last_request.querystring['mode'] == ['pdf']


def test_download_resize_receipt(httpretty):
    httpretty.register_uri(
        method=httpretty.GET,
        uri='https://trust-test.yandex.ru/checks/token/receipts/refund_id?mode=pdf',
        body='resize_pdf_body',
    )

    httpretty.register_uri(
        method=httpretty.GET,
        uri='https://trust_url/payments/token?show_trust_payment_id=1',
        body=json.dumps({
            'fiscal_receipt_clearing_url': 'https://trust-test.yandex.ru/checks/token/receipts/refund_id?mode=mobile',
            'status': 'success',
        }),
    )

    assert download_receipt('token', resize=True) == 'resize_pdf_body'
    assert httpretty.last_request.querystring['mode'] == ['pdf']


def test_download_argument_error():
    with pytest.raises(trust_client.ArgumentError):
        download_receipt('token', refund_id='refund_id', resize=True)
