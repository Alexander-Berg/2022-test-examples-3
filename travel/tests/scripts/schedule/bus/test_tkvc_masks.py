# -*- coding: utf-8 -*-

import datetime
from travel.rasp.admin.lib.unittests import replace_now

from travel.rasp.admin.lib.unittests.testcase import TestCase
from travel.rasp.admin.scripts.schedule.bus.tkvc import parse_mask


class TkvcMasksTest(TestCase):

    def setUp(self):
        self.route_title = u""

    def testTest(self):
        self.assertEqual(1, 1)

        self.assertTrue(self.is_days_equal(None, None))
        self.assertTrue(self.is_days_equal('1', '1'))
        self.assertTrue(self.is_days_equal('12', '21'))
        self.assertTrue(self.is_days_equal(u'12', '12'))

        self.assertFalse(self.is_days_equal('1', '2'))
        self.assertFalse(self.is_days_equal('12', '34'))
        self.assertFalse(self.is_days_equal(None, '34'))
        self.assertFalse(self.is_days_equal('12', None))

    def testSimple(self):
        raw_days = u'пн.'
        days, period_start_date, exclude = parse_mask(self.route_title, raw_days, datetime.date(2013, 8, 1))

        self.assertTrue(self.is_days_equal(days, u'1'))
        self.assertEqual(period_start_date, None)
        self.assertEqual(exclude, None)
        
    def testSimple2(self):
        raw_days = u'пн'
        days, period_start_date, exclude = parse_mask(self.route_title, raw_days,  datetime.date(2013, 8, 1))

        self.assertTrue(self.is_days_equal(days, u'1'))
        self.assertEqual(period_start_date, None)
        self.assertEqual(exclude, None)

    def testAllDays(self):
        raw_days = u'пн.вт.ср.чт.пт.сб.вс.'
        days, period_start_date, exclude = parse_mask(self.route_title, raw_days,  datetime.date(2013, 8, 1))

        self.assertTrue(self.is_days_equal(days, u'1234567'))
        self.assertEqual(period_start_date, None)
        self.assertEqual(exclude, None)

    def testAllDays2(self):
        raw_days = u'пн.вт.ср.чт.пт.сб.вс'
        days, period_start_date, exclude = parse_mask(self.route_title, raw_days,  datetime.date(2013, 8, 1))

        self.assertTrue(self.is_days_equal(days, u'1234567'))
        self.assertEqual(period_start_date, None)
        self.assertEqual(exclude, None)

    # ok 'ежедневно'
    def testAllDays3(self):
        raw_days = u'ежедневно'
        days, period_start_date, exclude = parse_mask(self.route_title, raw_days,  datetime.date(2013, 8, 1))

        self.assertTrue(self.is_days_equal(days, u'1234567'))
        self.assertEqual(period_start_date, None)
        self.assertEqual(exclude, None)

    # ok 'пн.вт.ср.чт.пт.вс.'
    def testSomeDays(self):
        raw_days = u'пн.вт.ср.чт.пт.вс.'
        days, period_start_date, exclude = parse_mask(self.route_title, raw_days,  datetime.date(2013, 8, 1))

        self.assertTrue(self.is_days_equal(days, u'123457'))
        self.assertEqual(period_start_date, None)
        self.assertEqual(exclude, None)

    # ok 'через день, с 11.08'
    def testEveryOtherDay(self):
        raw_days = u'через день, с 11.08'
        days, period_start_date, exclude = parse_mask(self.route_title, raw_days,  datetime.date(2013, 8, 1))

        self.assertTrue(self.is_days_equal(days, u'через день'))
        self.assertEqual(period_start_date, u'2013-08-11')
        self.assertEqual(exclude, None)

    # ok 'через день, с 12.08'
    def testEveryOtherDay2(self):
        raw_days = u'через день, с 12.08'
        days, period_start_date, exclude = parse_mask(self.route_title, raw_days,  datetime.date(2013, 8, 1))

        self.assertTrue(self.is_days_equal(days, u'через день'))
        self.assertEqual(period_start_date, u'2013-08-12')
        self.assertEqual(exclude, None)

    def testEveryOtherDay3(self):
        raw_days = u'через день, с 12.01'
        days, period_start_date, exclude = parse_mask(self.route_title, raw_days,  datetime.date(2013, 12, 20))

        self.assertTrue(self.is_days_equal(days, u'через день'))
        self.assertEqual(period_start_date, u'2014-01-12')
        self.assertEqual(exclude, None)

    def testEveryOtherDay4(self):
        raw_days = u'через день, с 12.12'
        days, period_start_date, exclude = parse_mask(self.route_title, raw_days,  datetime.date(2014, 1, 10))

        self.assertTrue(self.is_days_equal(days, u'через день'))
        self.assertEqual(period_start_date, u'2013-12-12')
        self.assertEqual(exclude, None)

    # ok 'по нечетным числам'
    def testOdd(self):
        raw_days = u'по нечетным числам'
        days, period_start_date, exclude = parse_mask(self.route_title, raw_days,  datetime.date(2013, 8, 1))

        self.assertTrue(self.is_days_equal(days, u'нечетные'))
        self.assertEqual(period_start_date, None)
        self.assertEqual(exclude, None)

    # ok 'по четным числам'
    def testEven(self):
        raw_days = u'по четным числам'
        days, period_start_date, exclude = parse_mask(self.route_title, raw_days,  datetime.date(2013, 8, 1))

        self.assertTrue(self.is_days_equal(days, u'четные'))
        self.assertEqual(period_start_date, None)
        self.assertEqual(exclude, None)

    # ok '14.8 16.8 17.8 18.8 21.8 23.8 25.8 28.8 30.8 1.9 4.9 8.9 11.9 15.9 18.9'
    def testManyDates(self):
        raw_days = u'14.8 16.8 17.8 18.8 21.8 23.8 25.8 28.8 30.8 1.9 4.9 8.9 11.9 15.9 18.9'
        days, period_start_date, exclude = parse_mask(self.route_title, raw_days,  datetime.date(2013, 8, 14))

        days_true = [
            u'2013-08-14',
            u'2013-08-16',
            u'2013-08-17',
            u'2013-08-18',
            u'2013-08-21',
            u'2013-08-23',
            u'2013-08-25',
            u'2013-08-28',
            u'2013-08-30',
            u'2013-09-01',
            u'2013-09-04',
            u'2013-09-08',
            u'2013-09-11',
            u'2013-09-15',
            u'2013-09-18',
        ]
        days_true = u";".join(days_true)
        self.assertTrue(self.is_days_equal(days, days_true))
        self.assertEqual(period_start_date, None)
        self.assertEqual(exclude, None)

    # ok 'недели:1, пн.'
    def testWeeks(self):
        raw_days = u'недели:1, пн.'
        days, period_start_date, exclude = parse_mask(self.route_title, raw_days,  datetime.date(2013, 8, 1))

        self.assertTrue(self.is_days_equal(days, u'2013-08-05;2013-09-02'))
        self.assertEqual(period_start_date, None)
        self.assertEqual(exclude, None)

    def testWeeks2(self):
        raw_days = u'недели:2, пн.'
        days, period_start_date, exclude = parse_mask(self.route_title, raw_days,  datetime.date(2013, 8, 1))

        self.assertTrue(self.is_days_equal(days, u'2013-08-12;2013-09-09'))
        self.assertEqual(period_start_date, None)
        self.assertEqual(exclude, None)

    def testWeeks3(self):
        raw_days = u'недели:3, сб'
        days, period_start_date, exclude = parse_mask(self.route_title, raw_days,  datetime.date(2013, 8, 1))

        self.assertTrue(self.is_days_equal(days, u'2013-08-17;2013-09-21'))
        self.assertEqual(period_start_date, None)
        self.assertEqual(exclude, None)

    # ok 'через день, с 12.08, кроме пт.сб.вс.'
    def testEveryOtherDayExclude(self):
        raw_days = u'через день, с 12.08, кроме пт.сб.вс.'
        days, period_start_date, exclude = parse_mask(self.route_title, raw_days,  datetime.date(2013, 8, 1))

        self.assertTrue(self.is_days_equal(days, u'через день'))
        self.assertEqual(period_start_date, u'2013-08-12')
        self.assertTrue(self.is_days_equal(exclude, u'567'))

    # ok 'через день, с 12.08, кроме пн.вт.ср.чт.пт.вс.'
    def testEveryOtherDayExclude2(self):
        raw_days = u'через день, с 12.08, кроме пн.вт.ср.чт.пт.вс.'
        days, period_start_date, exclude = parse_mask(self.route_title, raw_days,  datetime.date(2013, 8, 1))

        self.assertTrue(self.is_days_equal(days, u'через день'))
        self.assertEqual(period_start_date, u'2013-08-12')
        self.assertTrue(self.is_days_equal(exclude, u'123457'))

    def testEveryOtherDayExclude3(self):
        raw_days = u'через день, с 12.08, кроме пт.сб.вс'
        days, period_start_date, exclude = parse_mask(self.route_title, raw_days,  datetime.date(2013, 8, 1))

        self.assertTrue(self.is_days_equal(days, u'через день'))
        self.assertEqual(period_start_date, u'2013-08-12')
        self.assertTrue(self.is_days_equal(exclude, u'567'))

    # error 'по отдельным дням'
    def testError(self):
        raw_days = u'по отдельным дням'
        days, period_start_date, exclude = parse_mask(self.route_title, raw_days,  datetime.date(2013, 8, 1))

        self.assertTrue(self.is_days_equal(days, None))
        self.assertEqual(period_start_date, None)
        self.assertTrue(self.is_days_equal(exclude, None))

    # 'через день, с 16.06, пн.вт.ср.чт.пт'
    def testEveryOtherDay5(self):
        raw_days = u'через день, с 1.08, пн.вт.ср.чт.пт'
        days, period_start_date, exclude = parse_mask(self.route_title, raw_days,  datetime.date(2013, 8, 1))

        self.assertTrue(self.is_days_equal(days, u'через день'))
        self.assertEqual(period_start_date, u'2013-08-01')
        self.assertTrue(self.is_days_equal(exclude, u'67'))

    def testFeb29(self):
        raw_days = u'29.02'
        days, period_start_date, exclude = parse_mask(self.route_title, raw_days,  datetime.date(2013, 8, 1))

        self.assertTrue(self.is_days_equal(days, u'2012-02-29'))
        self.assertEqual(period_start_date, None)
        self.assertEqual(exclude, None)

    def is_days_equal(self, days, days2):
        if days == days2:
            return True

        if days is None or days2 is None:
            return False

        return set(days) == set(days2)
