# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from decimal import Decimal

import pytest
from hamcrest import assert_that, contains, has_properties

from common.utils import gen_hex_uuid
from common.workflow.process import Process
from travel.rasp.train_api.train_purchase.core.enums import OrderStatus, TravelOrderStatus
from travel.rasp.train_api.train_purchase.core.factories import (
    TrainOrderFactory, TrainRefundFactory, PartnerDataFactory, PaymentFactory, PassengerFactory, TicketFactory,
    TicketPaymentFactory, TicketRefundFactory, InsuranceFactory
)
from travel.rasp.train_api.train_purchase.core.models import TrainOrder, RefundStatus

pytestmark = [pytest.mark.mongouser, pytest.mark.dbuser]


class TestGetRefund(object):
    def test_get_refund_one_refund_only(self):
        refund = TrainRefundFactory(factory_extra_params={'create_order': True})
        assert refund.order.get_refund(refund.uuid) == refund

    def test_get_refund_several_refunds_in_order(self):
        order = TrainOrderFactory()
        refunds = [
            TrainRefundFactory(order_uid=order.uid),
            TrainRefundFactory(order_uid=order.uid),
            TrainRefundFactory(order_uid=order.uid),
            TrainRefundFactory(order_uid=order.uid)
        ]
        for refund in refunds:
            assert order.get_refund(refund.uuid) == refund

    def test_get_refund_exception(self):
        first_order = TrainOrderFactory()
        TrainRefundFactory(order_uid=first_order.uid)
        second_order = TrainOrderFactory()
        TrainRefundFactory(order_uid=second_order.uid)
        TrainRefundFactory(order_uid=second_order.uid)
        for order in (first_order, second_order):
            with pytest.raises(TrainOrder.RefundNotFound):
                order.get_refund('not-existing-uuid')


class TestProperties(object):
    def test_no_refunds(self):
        order = TrainOrderFactory()
        assert order.last_refund is None

    def test_one_refund(self):
        order = TrainOrderFactory()
        refund = TrainRefundFactory(order_uid=order.uid)
        assert order.last_refund.uuid == refund.uuid

    def test_many_refunds(self):
        order = TrainOrderFactory()
        TrainRefundFactory(order_uid=order.uid)
        TrainRefundFactory(order_uid=order.uid)
        third_refund = TrainRefundFactory(order_uid=order.uid)
        assert order.last_refund.uuid == third_refund.uuid

    def test_partner_data(self):
        order = TrainOrderFactory(partner_data_history=[PartnerDataFactory(im_order_id=42)])
        assert order.current_partner_data.im_order_id == 42
        assert order.current_partner_data_lookup_name == 'partner_data_history__0'
        lookup_partner_data = list(order.iter_lookup_partner_data())
        assert len(lookup_partner_data) == 1
        assert lookup_partner_data[0][0] == 'partner_data_history__0'
        assert lookup_partner_data[0][1].im_order_id == 42

    def test_many_partner_data(self):
        order = TrainOrderFactory(
            partner_data_history=[PartnerDataFactory(im_order_id=42), PartnerDataFactory(im_order_id=43)],
        )
        assert order.current_partner_data.im_order_id == 43
        assert order.current_partner_data_lookup_name == 'partner_data_history__1'
        lookup_partner_data = list(order.iter_lookup_partner_data())
        assert len(lookup_partner_data) == 2
        assert lookup_partner_data[0][0] == 'partner_data_history__0'
        assert lookup_partner_data[0][1].im_order_id == 42
        assert lookup_partner_data[1][0] == 'partner_data_history__1'
        assert lookup_partner_data[1][1].im_order_id == 43


class TestGetActualTravelStatus(object):
    @pytest.mark.parametrize('order_status', [s for s in OrderStatus])
    def test_any_order_status(self, order_status):
        order = TrainOrderFactory(status=order_status)
        assert not order.get_actual_travel_status() == TravelOrderStatus.UNKNOWN

    @pytest.mark.parametrize('refund_status', [s for s, _ in RefundStatus.get_choices()])
    def test_any_refund_status(self, refund_status):
        order = TrainOrderFactory(status=OrderStatus.DONE)
        TrainRefundFactory(order_uid=order.uid, status=refund_status)
        assert not order.get_actual_travel_status() == TravelOrderStatus.UNKNOWN

    def test_order_exception_state(self):
        order = TrainOrderFactory(process={'state': Process.EXCEPTION_STATE})
        assert order.get_actual_travel_status() == TravelOrderStatus.CANCELLED

    def test_payment_exception_state(self):
        order = TrainOrderFactory()
        PaymentFactory(order_uid=order.uid, process={'state': Process.EXCEPTION_STATE})
        assert order.get_actual_travel_status() == TravelOrderStatus.CANCELLED

    def test_refund_exception_state(self):
        order = TrainOrderFactory(status=OrderStatus.DONE)
        TrainRefundFactory(order_uid=order.uid, process={'state': Process.EXCEPTION_STATE})
        assert order.get_actual_travel_status() == TravelOrderStatus.DONE

    @pytest.mark.parametrize('order_status, refund_status, travel_status', [
        (OrderStatus.RESERVED, None, TravelOrderStatus.RESERVED),
        (OrderStatus.DONE, None, TravelOrderStatus.DONE),
        (OrderStatus.PAID, None, TravelOrderStatus.IN_PROGRESS),
        (OrderStatus.CONFIRM_FAILED, None, TravelOrderStatus.CANCELLED),
        (OrderStatus.DONE, RefundStatus.NEW, TravelOrderStatus.IN_PROGRESS),
        (OrderStatus.DONE, RefundStatus.PARTNER_REFUND_UNKNOWN, TravelOrderStatus.IN_PROGRESS),
        (OrderStatus.DONE, RefundStatus.DONE, TravelOrderStatus.DONE),
        (OrderStatus.DONE, RefundStatus.FAILED, TravelOrderStatus.DONE),
        (OrderStatus.PAYMENT_FAILED, RefundStatus.DONE, TravelOrderStatus.CANCELLED),
    ])
    def test_travel_status_by_order_statuses(self, order_status, refund_status, travel_status):
        order = TrainOrderFactory(status=order_status)
        if refund_status:
            TrainRefundFactory(order_uid=order.uid, status=refund_status)
        assert order.get_actual_travel_status() == travel_status


class TestPassengerTotals(object):
    def test_with_insurance(self):
        order = TrainOrderFactory(
            passengers=[
                PassengerFactory(
                    insurance=InsuranceFactory(amount=Decimal('30'), trust_order_id='some_id',
                                               refund_uuid=gen_hex_uuid()),
                    tickets=[
                        TicketFactory(
                            payment=TicketPaymentFactory(amount=Decimal('100'), fee=Decimal('10')),
                            refund=TicketRefundFactory(amount=Decimal('70')),
                        ),
                        TicketFactory(
                            payment=TicketPaymentFactory(amount=Decimal('100'), fee=Decimal('10')),
                            refund=TicketRefundFactory(amount=Decimal('70')),
                        )
                    ],
                ),
            ]
        )

        assert_that(order.passengers, contains(
            has_properties(
                total=Decimal('250'),
                total_amount=Decimal('200'),
                total_tickets_fee=Decimal('20'),
                total_insurance=Decimal('30'),
                total_fee=Decimal('39.5'),
            ),
        ))

    def test_without_insurance(self):
        order = TrainOrderFactory(
            passengers=[
                PassengerFactory(
                    insurance=None,
                    tickets=[
                        TicketFactory(
                            payment=TicketPaymentFactory(amount=Decimal('100'), fee=Decimal('10')),
                        ),
                        TicketFactory(
                            payment=TicketPaymentFactory(amount=Decimal('100'), fee=Decimal('10')),
                        )
                    ],
                ),
            ]
        )

        assert_that(order.passengers, contains(
            has_properties(
                total=Decimal('220'),
                total_amount=Decimal('200'),
                total_tickets_fee=Decimal('20'),
                total_insurance=Decimal('0'),
                total_fee=Decimal('20'),
            ),
        ))
