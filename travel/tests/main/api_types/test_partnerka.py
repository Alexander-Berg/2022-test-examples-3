# -*- coding: utf-8 -*-
from __future__ import absolute_import

from datetime import date
from decimal import Decimal

from travel.avia.library.python.tester.factories import create_partner
from travel.avia.library.python.tester.testcase import TestCase

from travel.avia.library.python.avia_data.models import (
    BalanceRedirectPrepeared, BalancePriceListRedirectPrepared, ShowLog
)
from travel.avia.library.python.tester.factories import get_model_factory
from travel.avia.backend.main.api_types.partnerka import (
    get_detailed_statistics_new_scheme, generate_partner_code
)


class TestGetDetailedStatisticsNewScheme(TestCase):
    def setUp(self):
        self.redirects_factory = get_model_factory(BalanceRedirectPrepeared)
        self.new_redirects_factory = get_model_factory(BalancePriceListRedirectPrepared)
        self.shows_factory = get_model_factory(ShowLog)

    def test_all_partners(self):
        """
        При запросе без указания billing_order_id получаем статистику по всем партнерам
        """
        pp = 502
        partner_id = 666
        client_id = 111
        order_id = 222
        partner = create_partner(
            code='some1',
            id=partner_id,
            billing_client_id=client_id,
            billing_order_id=order_id,
        )
        create_partner(
            code='some2',
            id=partner_id+1,
            billing_client_id=client_id+1,
            billing_order_id=order_id+1,
        )

        self.redirects_factory(
            eventdate=date(2018, 2, 28),
            billing_order_id=order_id,
            national_version='ru',
            pp=pp,
            count=10,
        )
        self.new_redirects_factory(
            event_date=date(2018, 3, 1),
            billing_client_id=client_id,
            national_version='ru',
            pp=pp,
            count=10,
            cost=12.34,
            payments_count=5,
        )

        self.shows_factory(
            eventdate=date(2018, 2, 28),
            partner=partner,
            billing_order_id=partner.billing_order_id,
            national_version='ru',
            pp=pp,
            show_count=100,
        )
        self.shows_factory(
            eventdate=date(2018, 3, 1),
            partner=partner,
            billing_order_id=partner.billing_order_id,
            national_version='ru',
            pp=pp,
            show_count=200,
        )
        self.shows_factory(
            eventdate=date(2018, 3, 2),
            partner=partner,
            billing_order_id=partner.billing_order_id,
            national_version='ru',
            pp=pp,
            show_count=300,
        )
        expected = [
            {'pp': pp, 'sum_cost': 0., 'click_count': 10, 'show_count': 100, 'national_version': 'ru',
             'billing_order_id': order_id, 'eventdate': '2018-02-28', 'payments_count': 0, 'conversion': 0.},
            {'pp': pp, 'sum_cost': Decimal('12.34'), 'click_count': 10, 'show_count': 200, 'national_version': 'ru',
             'billing_order_id': order_id, 'eventdate': '2018-03-01', 'payments_count': 5, 'conversion': 0.5},
            {'pp': pp, 'sum_cost': 0., 'click_count': 0, 'show_count': 300, 'national_version': 'ru',
             'billing_order_id': order_id, 'eventdate': '2018-03-02', 'payments_count': 0, 'conversion': 0.}
        ]

        request_params = {
            'left_date': '2018-02-28',
            'right_date': '2018-03-02',
        }
        response = get_detailed_statistics_new_scheme(request_params)
        self.assertItemsEqual(expected, response)


class TestPartnerka(TestCase):
    def test_generate_partner_code_existing_code(self):
        code = 'testcode'
        create_partner(code=code)
        expected_code = code + '1'
        actual_code = generate_partner_code(code)
        assert expected_code == actual_code

    def test_generate_partner_code_existing_code_twice(self):
        code = 'testcode'
        create_partner(code=code)
        create_partner(code=code + '1')
        expected_code = code + '2'
        actual_code = generate_partner_code(code)

        assert expected_code == actual_code

    def test_generate_partner_code_not_existing_code(self):
        code = 'testcode'
        actual_code = generate_partner_code(code)
        assert code == actual_code

    def test_generate_partner_code_isalnum(self):
        """заголовки приводятся к нижнему регистру и фильтруются символы"""
        title = 'RanDom Title#$'
        expected_title = 'randomtitle'
        actual_code = generate_partner_code(title)
        assert expected_title == actual_code
