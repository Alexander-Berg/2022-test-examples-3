# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

from datetime import date, datetime

import json
import mock
from hamcrest import assert_that, has_entries, has_properties, contains

from common.tester.utils.replace_setting import replace_setting
from common.tester.utils.datetime import replace_now

from travel.rasp.rasp_scripts.scripts.suburban.compare_provider_report_with_billing import (
    MovistaTicket, BillingOrder, get_movista_tickets, get_billing_orders, compare_orders, get_period_dt, ComparePeriod,
    ImTicket, get_im_tickets
)


def test_movista_ticket():
    ticket = MovistaTicket({
        'orderId': 10,
        'status': 'confirmed',
        'confirmDate': '2021-08-12T10:00',
        'price': 100
    })

    assert ticket.provider_order_id == 10
    assert ticket.status == 'confirmed'
    assert ticket.update_dt == '2021-08-12T10:00'
    assert ticket.price == 100

    assert_that(json.loads(ticket.to_file_str()), has_entries({
        'provider_order_id': 10,
        'status': 'confirmed',
        'update_dt': '2021-08-12T10:00',
        'price': 100
    }))


def test_im_ticket():
    ticket = ImTicket({
        'OrderId': 20,
        'Confirmed': '2021-08-12T20:00:00',
        'Amount': 200,
        'OrderItems': [{'SimpleOperationStatus': 'Succeeded'}]
    })

    assert ticket.provider_order_id == 20
    assert ticket.status == 'Succeeded'
    assert ticket.update_dt == '2021-08-12T20:00:00'
    assert ticket.price == 200

    assert_that(json.loads(ticket.to_file_str()), has_entries({
        'provider_order_id': 20,
        'status': 'Succeeded',
        'confirm_dt': '2021-08-12T20:00:00',
        'price': 200
    }))


def test_billing_order():
    order = BillingOrder(('10', 'YA-10', 'payment', '100.00', '2021-08-12T10:10'))
    assert order.provider_order_id == 10
    assert order.service_order_id == 'YA-10'
    assert order.transaction_type == 'payment'
    assert order.update_dt == '2021-08-12T10:10'
    assert order.price == '100.00'

    assert_that(json.loads(order.to_file_str()), has_entries({
        'provider_order_id': 10,
        'service_order_id': 'YA-10',
        'transaction_type': 'payment',
        'update_dt': '2021-08-12T10:10',
        'price': '100.00'
    }))


def test_get_movista_tickets():
    movista_response = {'tickets': [
        {
            'orderId': 10,
            'status': 'confirmed',
            'confirmDate': '2021-08-11T10:00',
            'price': 100
        },
        {
            'orderId': 20,
            'status': 'refunded',
            'refundDate': '2021-08-12T20:00',
            'price': 200
        }
    ]}
    with replace_setting('MINUTES_BETWEEN_BILLING_AND_PROVIDER', 10):
        with mock.patch(
            'travel.rasp.library.python.api_clients.movista.client.MovistaClient.report', return_value=movista_response
        ) as m_report:
            ticket_by_ids = get_movista_tickets(datetime(2021, 8, 11), datetime(2021, 8, 13))

            m_report.assert_called_with(datetime(2021, 8, 10, 23, 50), datetime(2021, 8, 13), status='confirmed')
            assert_that(ticket_by_ids, has_entries({
                10: has_properties({
                    'provider_order_id': 10,
                    'status': 'confirmed',
                    'update_dt': '2021-08-11T10:00',
                    'price': 100
                }),
                20: has_properties({
                    'provider_order_id': 20,
                    'status': 'refunded',
                    'update_dt': '2021-08-12T20:00',
                    'price': 200
                }),
            }))


def test_get_im_tickets():
    im_response = {'Orders': [
        {
            'OrderId': 5,
            'Confirmed': '2021-08-10T10:00:00',
            'Amount': 50,
            'OrderItems': [{'SimpleOperationStatus': 'Error'}]
        },
        {
            'OrderId': 10,
            'Confirmed': '2021-08-11T10:00:00',
            'Amount': 100,
            'OrderItems': [{'SimpleOperationStatus': 'Succeeded'}]
        },
        {
            'OrderId': 20,
            'Confirmed': '2021-08-12T20:00:00',
            'Amount': 200,
            'OrderItems': [{'SimpleOperationStatus': 'Succeeded'}]
        }
    ]}
    with replace_setting('MINUTES_BETWEEN_BILLING_AND_PROVIDER', 10):
        with mock.patch(
            'travel.rasp.library.python.api_clients.im.client.ImClient.order_list', return_value=im_response
        ) as m_report:
            ticket_by_ids = get_im_tickets(datetime(2021, 8, 11), datetime(2021, 8, 13))

            assert m_report.call_count == 3
            assert_that(ticket_by_ids, has_entries({
                10: has_properties({
                    'provider_order_id': 10,
                    'status': 'Succeeded',
                    'update_dt': '2021-08-11T10:00:00',
                    'price': 100
                }),
                20: has_properties({
                    'provider_order_id': 20,
                    'status': 'Succeeded',
                    'update_dt': '2021-08-12T20:00:00',
                    'price': 200
                }),
            }))


def test_get_billing_orders():
    with replace_setting('YT_CPA_SUBURBAN_ORDERS_DIR', 'cpa_dir'):
        with replace_setting('YT_BILLING_TRANSACTIONS_DIR', 'billing_dir'):
            with replace_setting('BILLING_SUBURBAN_SERVICE_ID', 716):
                with replace_setting('MINUTES_BETWEEN_BILLING_AND_PROVIDER', 10):
                    with mock.patch(
                        'travel.rasp.rasp_scripts.scripts.suburban.compare_provider_report_with_billing.YqlClient.query'
                    ) as m_query:
                        get_billing_orders('im', datetime(2021, 8, 11), datetime(2021, 8, 13))

                        m_query.assert_called_with('''
        $provider_id = (
            SELECT substring(partner_order_id, 0, 17) as inner_id, provider_order_id
            FROM hahn.`cpa_dir`
            WHERE departure_date >= '2021-08-09'
                  and provider = 'IM'
                  and provider_order_id is not null
        );

        SELECT provider.provider_order_id, service_order_id, transaction_type, price, update_dt
        FROM RANGE(`billing_dir`, '2021-08-11', '2021-08-13') as log
        JOIN $provider_id as provider ON log.service_order_id = provider.inner_id
        WHERE service_id = 716
              and transaction_type = 'payment'
              and payment_type = 'cost'
              and update_dt < '2021-08-13T00:10:00'
    ''')


def test_compare_orders():
    provider_tickets_by_ids = {
        10: MovistaTicket({'orderId': 10, 'status': 'confirmed', 'confirmDate': '2021-08-11T23:50:00', 'price': 100}),
        11: MovistaTicket({'orderId': 11, 'status': 'confirmed', 'confirmDate': '2021-08-11T23:55:00', 'price': 110.0}),
        12: MovistaTicket({'orderId': 12, 'status': 'confirmed', 'confirmDate': '2021-08-12T12:20:00', 'price': 121}),
        131: MovistaTicket({'orderId': 131, 'status': 'confirmed', 'confirmDate': '2021-08-12T13:30:00', 'price': 130}),
        14: MovistaTicket({'orderId': 14, 'status': 'confirmed', 'confirmDate': '2021-08-12T23:55:00', 'price': 140})
    }

    billing_orders_by_ids = {
        11: BillingOrder(('11', 'YA-11', 'payment', '110.00', '2021-08-12T00:10:00')),
        12: BillingOrder(('12', 'YA-12', 'payment', '122.00', '2021-08-12T12:25:00')),
        132: BillingOrder(('132', 'YA-13', 'payment', '130.00', '2021-08-12T13:35:00')),
        14: BillingOrder(('14', 'YA-14', 'payment', '140.00', '2021-08-13T00:05:00')),
        15: BillingOrder(('15', 'YA-15', 'payment', '150.00', '2021-08-13T00:10:00')),
    }

    result = compare_orders(
        'movista', datetime(2021, 8, 12), datetime(2021, 8, 13), provider_tickets_by_ids, billing_orders_by_ids
    )

    assert_that(result, has_properties({
        'start_date': date(2021, 8, 12),
        'end_date': date(2021, 8, 12),
        'provider_tickets_count': 2,
        'missed_provider_tickets': contains(
            has_properties({'provider_order_id': 131, 'update_dt': '2021-08-12T13:30:00', 'price': 130})
        ),
        'billing_orders_count': 2,
        'missed_billing_orders': contains(
            has_properties({'provider_order_id': 132, 'update_dt': '2021-08-12T13:35:00', 'price': '130.00'})
        ),
        'different_price_orders': contains(
            has_properties({
                'order_id': 12, 'provider_update_dt': '2021-08-12T12:20:00',
                'provider_price': 121, 'billing_price': '122.00'
            })
        )
    }))


def test_get_period_dt():
    start_dt, end_dt = get_period_dt(ComparePeriod.CUSTOM_PERIOD, '2021-10-02', '2021-10-04')
    assert start_dt == datetime(2021, 10, 2)
    assert end_dt == datetime(2021, 10, 5)

    with replace_now('2021-10-06'):
        start_dt, end_dt = get_period_dt(ComparePeriod.PREVIOUS_DAY)
        assert start_dt == datetime(2021, 10, 5)
        assert end_dt == datetime(2021, 10, 6)

        start_dt, end_dt = get_period_dt(ComparePeriod.PREVIOUS_DAY_MONTH)
        assert start_dt == datetime(2021, 10, 1)
        assert end_dt == datetime(2021, 10, 6)

        start_dt, end_dt = get_period_dt(ComparePeriod.PREVIOUS_MONTH)
        assert start_dt == datetime(2021, 9, 1)
        assert end_dt == datetime(2021, 10, 1)

    with replace_now('2021-01-06'):
        start_dt, end_dt = get_period_dt(ComparePeriod.PREVIOUS_MONTH)
        assert start_dt == datetime(2020, 12, 1)
        assert end_dt == datetime(2021, 1, 1)
