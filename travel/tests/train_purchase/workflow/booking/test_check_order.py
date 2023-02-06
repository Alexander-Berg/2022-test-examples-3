# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock
import pytest
from requests import ConnectionError

from common.workflow.tests_utils.process import process_state_action
from travel.rasp.train_api.train_partners.base import RzhdStatus
from travel.rasp.train_api.train_partners.im import base
from travel.rasp.train_api.train_partners.im.base import RZHD_STATUS_TO_BLANK_STATUS, OPERATION_STATUS_TO_IM_OPERATION_STATUS
from travel.rasp.train_api.train_partners.im.factories.order_info import (
    ImOrderInfoFactory, ImOrderItemBlankFactory, ImRailwayOrderItemFactory, ImInsuranceOrderItemFactory
)
from travel.rasp.train_api.train_partners.im.factories.utils import mock_im
from travel.rasp.train_api.train_partners.im.get_order_info import IM_ORDER_INFO_METHOD
from travel.rasp.train_api.train_purchase.core.enums import (
    OrderStatus, TrainPartner, OperationStatus, TravelOrderStatus, InsuranceStatus
)
from travel.rasp.train_api.train_purchase.core.factories import (
    TrainOrderFactory, PassengerFactory, TicketFactory, PartnerDataFactory, InsuranceFactory
)
from travel.rasp.train_api.train_purchase.workflow.booking import check_order
from travel.rasp.train_api.train_purchase.workflow.booking.check_order import CheckOrder, CheckOrderEvents

pytestmark = [pytest.mark.mongouser('module'), pytest.mark.dbuser('module')]


class TestCheckOrderIm(object):
    @pytest.yield_fixture(autouse=True)
    def auto_use_fixtues(self, httpretty):
        self.httpretty = httpretty
        yield

    def test_ok(self):
        httpretty = self.httpretty
        order = TrainOrderFactory(
            partner=TrainPartner.IM,
            passengers=[PassengerFactory(tickets=[
                TicketFactory(blank_id='123', rzhd_status=RzhdStatus.RESERVATION)
            ])],
        )

        mock_im(httpretty, IM_ORDER_INFO_METHOD, json=ImOrderInfoFactory(
            train_order=order,
            OrderItems=[ImRailwayOrderItemFactory(
                train_order=order,
                OrderItemBlanks=[ImOrderItemBlankFactory(
                    train_order_ticket=order.passengers[0].tickets[0],
                    BlankStatus=RZHD_STATUS_TO_BLANK_STATUS[RzhdStatus.REMOTE_CHECK_IN],
                )])]
        ))
        event, order = process_state_action(
            CheckOrder,
            (CheckOrderEvents.PENDING, CheckOrderEvents.FAILED, CheckOrderEvents.DONE),
            order
        )

        assert event == CheckOrderEvents.DONE
        assert order.passengers[0].tickets[0].rzhd_status == RzhdStatus.REMOTE_CHECK_IN
        assert order.status == OrderStatus.DONE
        assert order.travel_status == TravelOrderStatus.DONE

    def test_pending_because_operation_in_process(self):
        httpretty = self.httpretty
        order = TrainOrderFactory(partner=TrainPartner.IM, passengers=[PassengerFactory(tickets=[
            TicketFactory(blank_id='123', rzhd_status=RzhdStatus.RESERVATION)
        ])])

        mock_im(httpretty, IM_ORDER_INFO_METHOD, json=ImOrderInfoFactory(
            train_order=order,
            OrderItems=[ImRailwayOrderItemFactory(
                train_order=order,
                SimpleOperationStatus=OPERATION_STATUS_TO_IM_OPERATION_STATUS[OperationStatus.IN_PROCESS],
                OrderItemBlanks=[ImOrderItemBlankFactory(
                    train_order_ticket=order.passengers[0].tickets[0],
                    BlankStatus=RZHD_STATUS_TO_BLANK_STATUS[RzhdStatus.REMOTE_CHECK_IN],
                )])]
        ))
        event, order = process_state_action(CheckOrder, (CheckOrderEvents.DONE,
                                                         CheckOrderEvents.PENDING,
                                                         CheckOrderEvents.FAILED), order)

        assert event == CheckOrderEvents.PENDING
        assert order.current_partner_data.check_transaction_counter == 1
        assert order.status == OrderStatus.RESERVED
        assert order.travel_status == TravelOrderStatus.RESERVED
        assert order.passengers[0].tickets[0].rzhd_status == RzhdStatus.RESERVATION

    def test_failed_because_operation_failed(self):
        httpretty = self.httpretty
        order = TrainOrderFactory(partner=TrainPartner.IM, passengers=[PassengerFactory(tickets=[
            TicketFactory(blank_id='123', rzhd_status=RzhdStatus.RESERVATION)
        ])])

        mock_im(httpretty, IM_ORDER_INFO_METHOD, json=ImOrderInfoFactory(
            train_order=order,
            OrderItems=[ImRailwayOrderItemFactory(
                train_order=order,
                SimpleOperationStatus=OPERATION_STATUS_TO_IM_OPERATION_STATUS[OperationStatus.FAILED],
            )]))
        event, order = process_state_action(CheckOrder, (CheckOrderEvents.DONE,
                                                         CheckOrderEvents.PENDING,
                                                         CheckOrderEvents.FAILED), order)

        assert event == CheckOrderEvents.FAILED
        assert order.status == OrderStatus.CONFIRM_FAILED
        assert order.travel_status == TravelOrderStatus.CANCELLED
        assert order.passengers[0].tickets[0].rzhd_status == RzhdStatus.RESERVATION

    def test_pending_because_of_request_error(self):
        order = TrainOrderFactory(partner=TrainPartner.IM)
        with mock.patch.object(base, 'get_im_raw_response', side_effect=ConnectionError('foo')):
            event, order = process_state_action(CheckOrder, (CheckOrderEvents.DONE,
                                                             CheckOrderEvents.PENDING,
                                                             CheckOrderEvents.FAILED), order)
            assert event == CheckOrderEvents.PENDING
            assert order.current_partner_data.check_transaction_counter == 1

    def test_pending_because_of_im_error(self):
        httpretty = self.httpretty
        order = TrainOrderFactory(partner=TrainPartner.IM)
        mock_im(httpretty, IM_ORDER_INFO_METHOD, status=500,
                body="""{"Code":1, "Message":"Сервис временно недоступен", "MessageParams":[]}""")

        event, order = process_state_action(CheckOrder, (CheckOrderEvents.DONE,
                                                         CheckOrderEvents.PENDING,
                                                         CheckOrderEvents.FAILED), order)
        assert event == CheckOrderEvents.PENDING
        assert order.current_partner_data.check_transaction_counter == 1

    @mock.patch.object(check_order, 'CHECK_ORDER_COUNT', 3)
    def test_failed_as_counter_overflown(self):
        httpretty = self.httpretty
        order = TrainOrderFactory(partner=TrainPartner.IM, passengers=[PassengerFactory(tickets=[
            TicketFactory(blank_id='123', rzhd_status=RzhdStatus.RESERVATION)
        ])], partner_data=PartnerDataFactory(check_transaction_counter=2))

        mock_im(httpretty, IM_ORDER_INFO_METHOD, json=ImOrderInfoFactory(
            train_order=order,
            OrderItems=[ImRailwayOrderItemFactory(
                train_order=order,
                SimpleOperationStatus=OPERATION_STATUS_TO_IM_OPERATION_STATUS[OperationStatus.IN_PROCESS])]))

        event, order = process_state_action(CheckOrder, (CheckOrderEvents.DONE,
                                                         CheckOrderEvents.PENDING,
                                                         CheckOrderEvents.FAILED), order)

        assert event == CheckOrderEvents.FAILED
        assert order.status == OrderStatus.CONFIRM_FAILED
        assert order.travel_status == TravelOrderStatus.CANCELLED
        assert order.current_partner_data.check_transaction_counter == 3

    def test_check_insurance_ok(self):
        httpretty = self.httpretty
        order = TrainOrderFactory(
            partner=TrainPartner.IM,
            passengers=[PassengerFactory(
                tickets=[TicketFactory(blank_id='123', rzhd_status=RzhdStatus.RESERVATION)],
                insurance=InsuranceFactory(operation_id='12345', trust_order_id='54321'),
            )],
        )

        mock_im(httpretty, IM_ORDER_INFO_METHOD, json=ImOrderInfoFactory(train_order=order))
        event, order = process_state_action(
            CheckOrder,
            (CheckOrderEvents.PENDING, CheckOrderEvents.FAILED, CheckOrderEvents.DONE),
            order
        )

        assert event == CheckOrderEvents.DONE
        assert order.passengers[0].insurance.operation_status == OperationStatus.OK
        assert order.status == OrderStatus.DONE
        assert order.travel_status == TravelOrderStatus.DONE

    def test_check_insurance_failed(self):
        httpretty = self.httpretty
        order = TrainOrderFactory(
            partner=TrainPartner.IM,
            passengers=[PassengerFactory(
                tickets=[TicketFactory(blank_id='123', rzhd_status=RzhdStatus.RESERVATION)],
                insurance=InsuranceFactory(operation_id='12345', trust_order_id='54321'),
            )],
        )

        mock_im(httpretty, IM_ORDER_INFO_METHOD, json=ImOrderInfoFactory(
            train_order=order,
            OrderItems=[
                ImRailwayOrderItemFactory(train_order=order),
                ImInsuranceOrderItemFactory(
                    train_order_passenger=order.passengers[0],
                    SimpleOperationStatus=OPERATION_STATUS_TO_IM_OPERATION_STATUS[OperationStatus.FAILED]
                ),
            ]
        ))
        event, order = process_state_action(
            CheckOrder,
            (CheckOrderEvents.PENDING, CheckOrderEvents.FAILED, CheckOrderEvents.DONE,
             CheckOrderEvents.INSURANCE_FAILED),
            order
        )

        assert event == CheckOrderEvents.INSURANCE_FAILED
        assert order.passengers[0].insurance.operation_status == OperationStatus.FAILED
        assert order.status == OrderStatus.DONE
        assert order.travel_status == TravelOrderStatus.DONE

    @pytest.mark.parametrize('check_transaction_counter, expected_event', [
        (0, CheckOrderEvents.PENDING),
        (100, CheckOrderEvents.DONE)
    ])
    def test_check_insurance_pending(self, check_transaction_counter, expected_event):
        httpretty = self.httpretty
        order = TrainOrderFactory(
            partner_data_history=[PartnerDataFactory(check_transaction_counter=check_transaction_counter)],
            partner=TrainPartner.IM,
            passengers=[PassengerFactory(
                tickets=[TicketFactory(blank_id='123', rzhd_status=RzhdStatus.RESERVATION)],
                insurance=InsuranceFactory(operation_id='12345', trust_order_id='54321'),
            )],
            insurance={'status': InsuranceStatus.CHECKED_OUT},
        )

        mock_im(httpretty, IM_ORDER_INFO_METHOD, json=ImOrderInfoFactory(
            train_order=order,
            OrderItems=[
                ImRailwayOrderItemFactory(train_order=order),
                ImInsuranceOrderItemFactory(
                    train_order_passenger=order.passengers[0],
                    SimpleOperationStatus=OPERATION_STATUS_TO_IM_OPERATION_STATUS[OperationStatus.IN_PROCESS]
                ),
            ]
        ))
        with mock.patch.object(check_order, 'guaranteed_send_email') as m_guaranteed_send_email, \
                mock.patch.object(check_order, 'send_event_to_payment') as m_send_event_to_payment:
            event, order = process_state_action(
                CheckOrder,
                (CheckOrderEvents.PENDING, CheckOrderEvents.FAILED, CheckOrderEvents.DONE,
                 CheckOrderEvents.INSURANCE_FAILED),
                order
            )

            assert event == expected_event
            if expected_event != CheckOrderEvents.PENDING:
                assert order.passengers[0].insurance.operation_status == OperationStatus.IN_PROCESS
                assert m_guaranteed_send_email.call_count == 1
                assert m_send_event_to_payment.call_count == 1
