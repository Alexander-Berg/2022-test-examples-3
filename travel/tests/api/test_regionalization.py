# -*- coding: utf-8 -*-
import datetime

from travel.avia.library.python.tester.factories import create_partner, create_settlement
from travel.avia.library.python.tester.testcase import TestCase

from travel.avia.ticket_daemon.tests.api.factories import create_regionalizepartnerqueryrule
from travel.avia.ticket_daemon.ticket_daemon.api import regionalization
from travel.avia.library.python.ticket_daemon.memo import reset_all_caches
from travel.avia.ticket_daemon.ticket_daemon.daemon_tester import create_query


class RegionalizationTest(TestCase):
    def setUp(self):
        reset_all_caches()
        self.partner = create_partner(code='code')
        self.query = create_query()

    def test_basic_allow(self):
        create_regionalizepartnerqueryrule(
            partner=self.partner,
            settlement_from_id=self.query.point_from.id,
            settlement_to_id=self.query.point_to.id,
            exclude=False,
        )
        to_ask, to_skip = regionalization.apply(self.query, [self.partner])
        assert to_ask == [self.partner]
        assert to_skip == []

    def test_basic_exclude(self):
        create_regionalizepartnerqueryrule(
            partner=self.partner,
            settlement_from_id=self.query.point_from.id,
            settlement_to_id=self.query.point_to.id,
            exclude=True,
        )
        to_ask, to_skip = regionalization.apply(self.query, [self.partner])
        assert to_ask == []
        assert to_skip == [self.partner]

    def test_all_excludes_without_matches(self):
        """Если все правила "кроме" и ни одно не совпало"""
        s1, s2 = create_settlement(), create_settlement()
        create_regionalizepartnerqueryrule(
            partner=self.partner,
            settlement_from_id=s1.id,
            settlement_to_id=s2.id,
            exclude=True,
        )
        to_ask, to_skip = regionalization.apply(self.query, [self.partner])
        assert to_ask == [self.partner]
        assert to_skip == []

    def test_allow_without_matches(self):
        """Если все правила "разрешающие" и ни одно не совпало"""
        s1, s2 = create_settlement(), create_settlement()
        create_regionalizepartnerqueryrule(
            partner=self.partner,
            settlement_from_id=s1.id,
            settlement_to_id=s2.id,
            exclude=False,
        )
        to_ask, to_skip = regionalization.apply(self.query, [self.partner])
        assert to_ask == []
        assert to_skip == [self.partner]

    def test_allow_with_dates_departure_in_range(self):
        """Применение разрешающего правила с датами вылета"""
        create_regionalizepartnerqueryrule(
            partner=self.partner,
            settlement_from_id=self.query.point_from.id,
            settlement_to_id=self.query.point_to.id,
            exclude=False,
            start_date=self.query.date_forward,
            end_date=self.query.date_forward,
        )
        to_ask, to_skip = regionalization.apply(self.query, [self.partner])
        assert to_ask == [self.partner]
        assert to_skip == []

    def test_not_allow_with_dates_departure_not_in_range(self):
        """Применение разрешающего правила с датами вылета, когда запрос не в интервале"""
        create_regionalizepartnerqueryrule(
            partner=self.partner,
            settlement_from_id=self.query.point_from.id,
            settlement_to_id=self.query.point_to.id,
            exclude=False,
            start_date=self.query.date_forward + datetime.timedelta(days=1),
            end_date=self.query.date_forward + datetime.timedelta(days=1),
        )
        to_ask, to_skip = regionalization.apply(self.query, [self.partner])
        assert to_ask == []
        assert to_skip == [self.partner]

    def test_allow_with_dates_departure_in_range_with_weekdays(self):
        """Применение разрешающего правила с датами вылета и днем недели вылета"""
        create_regionalizepartnerqueryrule(
            partner=self.partner,
            settlement_from_id=self.query.point_from.id,
            settlement_to_id=self.query.point_to.id,
            exclude=False,
            start_date=self.query.date_forward - datetime.timedelta(days=7),
            end_date=self.query.date_forward + datetime.timedelta(days=7),
            week_days='1234567'
        )
        to_ask, to_skip = regionalization.apply(self.query, [self.partner])
        assert to_ask == [self.partner]
        assert to_skip == []

    def test_not_allow_with_departure_date_in_range_but_wrong_weekday(self):
        """Применение разрешающего правила с датами вылета,
         когда запрос в интервале с неверным днем недели вылета
        """
        query_week_day = str(self.query.date_forward.weekday() + 1)
        week_days = '1234567'.replace(query_week_day, '')
        create_regionalizepartnerqueryrule(
            partner=self.partner,
            settlement_from_id=self.query.point_from.id,
            settlement_to_id=self.query.point_to.id,
            exclude=False,
            start_date=self.query.date_forward - datetime.timedelta(days=7),
            end_date=self.query.date_forward + datetime.timedelta(days=7),
            week_days=week_days,
        )
        to_ask, to_skip = regionalization.apply(self.query, [self.partner])
        assert to_ask == []
        assert to_skip == [self.partner]
