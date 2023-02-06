# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import json
import re
from datetime import timedelta, datetime

import mock
import pytest
from hamcrest import assert_that, has_properties, contains, has_property, contains_inanyorder, starts_with

from common.data_api.billing.trust_client import TrustPaymentStatuses, TrustClient
from common.tester.utils.datetime import replace_now
from travel.rasp.library.python.common23.date import environment
from common.utils.date import UTC_TZ, MSK_TZ
from travel.rasp.train_api.train_purchase.core.factories import TrainOrderFactory
from travel.rasp.train_api.train_purchase.core.models import TrainOrder
from travel.rasp.train_api.train_purchase.tasks.fill_payment_status_and_response import (
    fill_billing_status_and_response, fill_status_and_response
)

pytestmark = [pytest.mark.mongouser, pytest.mark.dbuser]
UTC_NOW = datetime(2018, 3, 19)


@pytest.fixture(autouse=True)
def fix_now():
    msk_now = UTC_TZ.localize(UTC_NOW).astimezone(MSK_TZ).replace(tzinfo=None)
    with replace_now(msk_now):
        yield


@pytest.fixture
def trust_client(httpretty):
    fake_url = 'http://fake_url/'
    payment_status = TrustPaymentStatuses.NOT_AUTHORIZED
    httpretty.register_uri(httpretty.GET, re.compile(fake_url + 'payments/\w+'), status=200, body=json.dumps({
        'status': 'success',
        'payment_status': payment_status.value,
        'payment_resp_code': 'resp-code-new',
        'payment_resp_desc': 'resp-desc-new'
    }))

    trust_client = TrustClient()
    with mock.patch.object(trust_client, 'trust_url', fake_url):
        yield trust_client


class TestFillStatusAndResponse(object):
    @pytest.mark.parametrize('status', (s.value for s in [
        TrustPaymentStatuses.NOT_AUTHORIZED, TrustPaymentStatuses.CANCELED,
        TrustPaymentStatuses.STARTED, TrustPaymentStatuses.NOT_STARTED
    ]))
    def test_fill_billing_status_and_response_1_payment(self, httpretty, trust_client, status):
        old_order = TrainOrderFactory(payments=[dict(
            status=status,
            trust_created_at=environment.now_utc() - timedelta(days=2),
            purchase_token='some-token',
        )])

        fill_billing_status_and_response(trust_client=trust_client)

        new_order = TrainOrder.objects.get()
        assert_that(new_order, has_properties(
            uid=old_order.uid,
            payments=contains(has_properties(
                purchase_token=old_order.payments[0].purchase_token,
                payment_url=old_order.payments[0].payment_url,
                status=TrustPaymentStatuses.NOT_AUTHORIZED.value,
                resp_code='resp-code-new',
                resp_desc='resp-desc-new',
                updated_from_billing_at=environment.now_utc()
            )),
        ))
        assert_that(httpretty.latest_requests, contains(
            has_property('path', starts_with('/payments/some-token')),
        ))

    def test_fill_billing_status_and_response_3_payments(self, httpretty, trust_client):
        old_order = TrainOrderFactory(payments=[
            dict(
                purchase_token='token3',
                status=TrustPaymentStatuses.NOT_AUTHORIZED.value,
                trust_created_at=environment.now_utc() - timedelta(days=2),
            ),
            dict(
                purchase_token='token2',
                status=TrustPaymentStatuses.NOT_AUTHORIZED.value,
                trust_created_at=environment.now_utc() - timedelta(days=2, minutes=2),
            ),
            dict(
                purchase_token='token1',
                status=TrustPaymentStatuses.NOT_AUTHORIZED.value,
                trust_created_at=environment.now_utc() - timedelta(days=2, minutes=3),
            ),
        ])

        fill_billing_status_and_response(trust_client=trust_client)

        new_order = TrainOrder.objects.get()
        assert_that(new_order, has_properties(
            uid=old_order.uid,
            payments=contains(
                has_properties(
                    status=TrustPaymentStatuses.NOT_AUTHORIZED.value,
                    resp_code='resp-code-new',
                    resp_desc='resp-desc-new',
                    updated_from_billing_at=environment.now_utc()
                ),
                has_properties(
                    status=TrustPaymentStatuses.NOT_AUTHORIZED.value,
                    resp_code='resp-code-new',
                    resp_desc='resp-desc-new',
                    updated_from_billing_at=environment.now_utc()
                ),
                has_properties(
                    status=TrustPaymentStatuses.NOT_AUTHORIZED.value,
                    resp_code='resp-code-new',
                    resp_desc='resp-desc-new',
                    updated_from_billing_at=environment.now_utc()
                )
            ),
        ))
        assert_that(httpretty.latest_requests, contains_inanyorder(
            has_property('path', starts_with('/payments/token3')),
            has_property('path', starts_with('/payments/token2')),
            has_property('path', starts_with('/payments/token1'))
        ))

    def test_fill_billing_status_and_response_too_new_order(self, httpretty, trust_client):
        TrainOrderFactory(payments=[dict(
            trust_created_at=environment.now_utc(),
            status=TrustPaymentStatuses.NOT_AUTHORIZED.value,
            purchase_token='some-token3'
        )])

        fill_billing_status_and_response(trust_client=trust_client)

        assert not httpretty.latest_requests

    def test_fill_billing_status_and_response_too_old_order(self, httpretty, trust_client):
        TrainOrderFactory(payments=[dict(
            trust_created_at=environment.now_utc() - timedelta(days=3, minutes=1),
            status=TrustPaymentStatuses.NOT_AUTHORIZED.value,
            purchase_token='some-token4'
        )])

        fill_billing_status_and_response(trust_client=trust_client)

        assert not httpretty.latest_requests

    def test_fill_billing_status_and_response_already_updated(self, httpretty, trust_client):
        TrainOrderFactory(
            payments=[dict(
                trust_created_at=environment.now_utc() - timedelta(days=2),
                status=TrustPaymentStatuses.NOT_AUTHORIZED.value,
                purchase_token='some-token4')
            ],
            updated_from_billing_at=environment.now_utc() - timedelta(days=1),
        )

        fill_billing_status_and_response(trust_client=trust_client)

        assert not httpretty.latest_requests

    @pytest.mark.parametrize('updated_from_billing_at_shift, updated_at_boundary_shift, update_expected', (
        (-timedelta(4), -timedelta(3), True),  # заказ обновляли слишком давно
        (-timedelta(3), -timedelta(4), False),  # заказ обновляли недавно
        (None, None, True),  # заказ еще не обновляли, границы нет
        (None, -timedelta(3), True),  # заказ еще не обновляли, граница есть
        (-timedelta(3), None, True)  # заказ обновляли, граница не задана
    ))
    def test_fill_status_and_response_updated_at_boundary(self, httpretty, trust_client, updated_from_billing_at_shift,
                                                          updated_at_boundary_shift, update_expected):
        updated_from_billing_at = updated_from_billing_at_shift \
            and (environment.now_utc() + updated_from_billing_at_shift)
        updated_at_boundary = updated_at_boundary_shift and (environment.now_utc() + updated_at_boundary_shift)
        order = TrainOrderFactory(
            payments=[dict(
                status=TrustPaymentStatuses.STARTED.value,
                trust_created_at=environment.now_utc() - timedelta(days=2),
                purchase_token='some-token',
            )],
            updated_from_billing_at=updated_from_billing_at,
        )

        fill_status_and_response(trust_client, updated_at_boundary_utc_naive=updated_at_boundary)

        order.reload()
        if update_expected:
            assert_that(order, has_properties(
                payments=contains(has_properties(
                    status=TrustPaymentStatuses.NOT_AUTHORIZED.value,
                    resp_code='resp-code-new',
                    resp_desc='resp-desc-new',
                    updated_from_billing_at=environment.now_utc()
                )),
            ))
            assert_that(httpretty.latest_requests, contains(
                has_property('path', starts_with('/payments/some-token')),
            ))
        else:
            assert_that(order, has_properties(
                payments=contains(has_properties(
                    status=TrustPaymentStatuses.STARTED.value,
                    updated_from_billing_at=updated_from_billing_at,
                )),
            ))
            assert not httpretty.latest_requests

    @pytest.mark.parametrize('payment_created_at_shift, begin_shift, update_expected', (
        (-timedelta(4), -timedelta(3), False),  # платеж сделали слишком давно
        (-timedelta(3), -timedelta(4), True),  # платеж попал в обработку
        (-timedelta(3), None, True)  # граница не задана, поэтому любой платеж обрабатываем
    ))
    def test_fill_status_and_response_begin(self, httpretty, trust_client, payment_created_at_shift,
                                            begin_shift, update_expected):
        begin = begin_shift and (environment.now_utc() + begin_shift)
        order = TrainOrderFactory(
            payments=[dict(
                status=TrustPaymentStatuses.STARTED.value,
                trust_created_at=environment.now_utc() + payment_created_at_shift,
                purchase_token='some-token',
            )],
            updated_from_billing_at=None,
        )

        fill_status_and_response(trust_client, begin_utc_naive=begin)

        order.reload()
        if update_expected:
            assert_that(order, has_properties(
                payments=contains(has_properties(
                    status=TrustPaymentStatuses.NOT_AUTHORIZED.value,
                    resp_code='resp-code-new',
                    resp_desc='resp-desc-new',
                    updated_from_billing_at=environment.now_utc()
                )),
            ))
            assert_that(httpretty.latest_requests, contains(
                has_property('path', starts_with('/payments/some-token')),
            ))
        else:
            assert_that(order, has_properties(
                payments=contains(has_properties(
                    status=TrustPaymentStatuses.STARTED.value,
                    updated_from_billing_at=None
                )),
            ))
            assert not httpretty.latest_requests

    @pytest.mark.parametrize('payment_created_at_shift, end_shift, update_expected', (
        (-timedelta(4), -timedelta(3), True),  # платеж создали достаточно давно
        (-timedelta(3), -timedelta(4), False),  # платеж новый, его еще рано обновлять
        (-timedelta(3), None, True),  # платеж создали достаточно давно
        (-timedelta(minutes=5), None, False)  # платеж создали только что
    ))
    def test_fill_status_and_response_end(self, httpretty, trust_client, payment_created_at_shift,
                                          end_shift, update_expected):
        end = end_shift and (environment.now_utc() + end_shift)
        factory = TrainOrderFactory(
            payments=[dict(
                status=TrustPaymentStatuses.STARTED.value,
                trust_created_at=environment.now_utc() + payment_created_at_shift,
                purchase_token='some-token',
            )],
            updated_from_billing_at=None,
        )
        order = factory

        fill_status_and_response(trust_client, end_utc_naive=end)

        order.reload()
        if update_expected:
            assert_that(order, has_properties(
                payments=contains(has_properties(
                    status=TrustPaymentStatuses.NOT_AUTHORIZED.value,
                    resp_code='resp-code-new',
                    resp_desc='resp-desc-new',
                    updated_from_billing_at=environment.now_utc()
                )),
            ))
            assert_that(httpretty.latest_requests, contains(
                has_property('path', starts_with('/payments/some-token')),
            ))
        else:
            assert_that(order, has_properties(
                payments=contains(has_properties(
                    status=TrustPaymentStatuses.STARTED.value,
                    updated_from_billing_at=None
                )),
            ))
            assert not httpretty.latest_requests
