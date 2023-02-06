# -*- coding: utf-8 -*-

from datetime import date, timedelta

from common.models.geo import Station
from common.utils.date import daterange, RunMask
from common.utils.dateparser import parse_human_date
from common.utils.calendar_matcher import get_matcher

from travel.rasp.admin.lib.unittests.testcase import TestCase
from travel.rasp.admin.www.templatetags.durations import round_duration


module_fixtures = [
    'travel.rasp.admin.tester.fixtures.www:countries.yaml',
    'travel.rasp.admin.tester.fixtures.www:regions.yaml',
    'travel.rasp.admin.tester.fixtures.www:settlements.yaml',
    'travel.rasp.admin.tester.fixtures.www:stations.yaml'
]


class DateParserTestCase(TestCase):
    class_fixtures = module_fixtures

    def test_dates_with_year(self):
        dates = u"""
        20.9.2031
        20.09.2031
        20.9.31
        20/9/2031
        20/09/2031
        20/9/31
        20-9-2031
        20-09-2031
        20-9-31
        20 сентября 2031
        20 сентября 31
        """

        for d in dates.strip().split("\n"):
            d = d.strip()

            self.assertEquals(parse_human_date(d), date(2031, 9, 20))

    def test_dates_without_year(self):
        dates = u"""
        20.09
        20 сентября
        """

        today = date(2011, 8, 3)

        for d in dates.strip().split("\n"):
            d = d.strip()

            self.assertEquals(parse_human_date(d, today), date(today.year, 9, 20))

    def test_dates_with_borders(self):
        today = date(2008, 6, 1)

        past_border = timedelta(days=30)

        self.assertEquals(parse_human_date(u'15 июня', today, past_border), date(today.year, 6, 15))
        self.assertEquals(parse_human_date(u'15 мая', today, past_border), date(today.year, 5, 15))

        # Если дата более чем на past_border времени в прошлом, то она пересчитывается в следующем году
        self.assertEquals(parse_human_date(u'15 апреля', today, past_border), date(today.year + 1, 4, 15))

        today = date(2008, 12, 8)
        self.assertEquals(parse_human_date(u'1 янв', today, past_border), date(2009, 1, 1))

        self.assertEquals(parse_human_date(u'20', today, past_border), date(2008, 12, 20))
        self.assertEquals(parse_human_date(u'6', today, past_border), date(2009, 1, 6))

    def test_pointers(self):
        self.assertEquals(parse_human_date(u'позавчера'), date.today() - timedelta(days=2))
        self.assertEquals(parse_human_date(u'вчера'), date.today() - timedelta(days=1))
        self.assertEquals(parse_human_date(u'сегодня'), date.today())
        self.assertEquals(parse_human_date(u'завтра'), date.today() + timedelta(days=1))
        self.assertEquals(parse_human_date(u'послезавтра'), date.today() + timedelta(days=2))
        self.assertEquals(parse_human_date(u'через неделю'), date.today() + timedelta(days=7))
        self.assertEquals(parse_human_date(u'через месяц', today=date(2030, 1, 3)), date(2030, 2, 3))
        self.assertEquals(parse_human_date(u'на все дни', today=date(2030, 1, 3)), None)

    def test_weekdays(self):
        today = date(2010, 1, 27)

        def test_weekday(text, d):
            self.assertEquals(parse_human_date(text, today), d)

        test_weekday(u'понедельник', date(2010, 2, 1))
        test_weekday(u'вторник', date(2010, 2, 2))
        test_weekday(u'среда', date(2010, 1, 27))
        test_weekday(u'четверг', date(2010, 1, 28))
        test_weekday(u'пятница', date(2010, 1, 29))
        test_weekday(u'суббота', date(2010, 1, 30))
        test_weekday(u'воскресенье', date(2010, 1, 31))

    def test_leap_year(self):
        self.assertEquals(parse_human_date(u'29 февраля 2012'), date(2012, 2, 29))


class RunMaskTestCase(TestCase):
    class_fixtures = module_fixtures

    def test_zero(self):
        mask = RunMask()
        self.assertEquals(str(mask), "0" * 372)

    def test_first_day(self):
        mask = RunMask()
        mask[date(2009, 1, 1)] = 1
        self.assertEquals(str(mask), "1" + "0" * 371)

    def test_portion(self):
        mask = RunMask()

        mask[date(2009, 1, 1)] = True
        mask[date(2009, 2, 1)] = True
        mask[date(2009, 6, 10)] = True

        p_mask = mask.portion(date(2009, 1, 1), 32)

        self.assertEquals(str(p_mask), "1" + "0" * 30 + "1" + "0" * (372 - 32))

    def test_converstion(self):
        str_mask = "0" * 360 + "11" + "0" * 10
        mask = RunMask(str_mask)
        self.assertEquals(str(mask), str_mask)


class BasicCalendarTemplatesTestCase(TestCase):
    class_fixtures = module_fixtures

    today = date(2009, 2, 1)
    start_date = date(2009, 1, 30)
    far_border = date(2009, 10, 25)
    schedule_length = 45
    length = 365
    matcher = get_matcher('basic', today, start_date, length)

    def get_text(self, mask):
        return self.matcher.find_template(mask, self.schedule_length)[0]

    def test_empty(self):
        self.assertEquals(self.get_text(RunMask(today=self.today)), None)

    def test_exceptions(self):
        mask = RunMask(today=self.today)

        for d in daterange(self.start_date, self.start_date + timedelta(self.length)):
            mask[d] = True

        exception_date = date(2009, 3, 1)

        mask[exception_date] = False

        self.assertEquals(self.get_text(mask)['ru'], u'ежедневно, кроме 01.03')

    def test_weekdays(self):
        mask = RunMask(today=self.today)

        for d in daterange(self.start_date, self.start_date + timedelta(365)):
            if d.isoweekday() == 7:
                mask[d] = True

        self.assertEquals(self.get_text(mask)['ru'], u'вс')

    def test_weekdays_combo(self):
        mask = RunMask(today=self.today)

        for d in daterange(self.start_date, self.start_date + timedelta(365)):
            if d.isoweekday() in [1, 7]:
                mask[d] = True

        self.assertEquals(self.get_text(mask)['ru'], u'пн, вс')

    def test_weekdays_inverted_combo(self):
        mask = RunMask(today=self.today)

        for d in daterange(self.start_date, self.start_date + timedelta(365)):
            if d.isoweekday() not in [1, 7]:
                mask[d] = True

        self.assertEquals(self.get_text(mask)['ru'], u'ежедневно, кроме пн, вс')

    def test_casualdays(self):
        mask = RunMask(today=self.today)

        mask[date(2009, 2, 26)] = True
        mask[date(2009, 2, 28)] = True
        mask[date(2009, 3, 12)] = True

        self.assertEquals(self.get_text(mask), None)

    def test_oneday(self):
        # Один день должен возвращать None, строка сформируется при отображении
        self.assertEquals(self.get_text(RunMask(today=self.today, days=[date(2009, 2, 26)])), None)


class RussianDurationTestCase(TestCase):
    class_fixtures = module_fixtures

    def test(self):
        # Дни, часы, минуты, "показывать минуты"
        variants = ((0, 4, 0, False, u"4 часа"),
                    (0, 4, 20, False, u"5 часов"),
                    (0, 4, 20, True, u"4 часа 20 минут"),
                    (0, 7, 20, True, u"7 часов 20 минут"),
                    (0, 7, 0, True, u"7 часов"),
                    (0, 7, 20, False, u"8 часов"),
                    (0, 8, 0, False, u"8 часов"),
                    (1, 1, 0, False, u"около 1 суток"),
                    (1, 8, 0, False, u"около 1,5 суток"),
                    (1, 16, 0, False, u"около 2 суток"),
                    (1, 18, 0, False, u"около 2 суток"),
                    )

        for v in variants:
            self.assertEquals(round_duration(timedelta(days=v[0], hours=v[1], minutes=v[2]), v[3]), v[4])


class StationTypeTestCase(TestCase):
    class_fixtures = module_fixtures + ['travel.rasp.admin.tester.fixtures.www:station_type_test.yaml']

    UNKNOWN_STATION_TYPE = 12

    def test(self):
        # Проверка определения типа станций. Id станции, id типа станции, название станции
        tests = ((u"Москва (Казанский вокзал)", 8, None),
                 (u"Екатеринбург-Пасс._test", 8, None),
                 (u"Берёзовка_test", 3, u'Ост. Пункт Березовка'),
                 (u"Берёзовка_test", 3, u'Ост. Березовка'),
                 (u"Берёзовка_test", 7, u'Обг. Пункт Березовка'),
                 (u"Берёзовка_test", 1, u' Обг. Пункт Березовка'),
                 (u"Берёзовка_test", 6, u'Разъезд Березовка'),
                 (u"Берёзовка_test", 1, u' Разъезд Березовка'),
                 (u"Берёзовка_test", 2, u'платформа Березовка'),
                 (u"Берёзовка_test", 1, u' платформа Березовка'),
                 (u"Екатеринбург Северный_test", 10, None),
                 (u"Пулково", 9, None),
                 (u"Кольцово", 9, None),
                 )

        for t in tests:
            station = Station.objects.get(title=t[0])
            station.station_type_id = self.UNKNOWN_STATION_TYPE
            if t[2]:
                station.title = t[2]

            determined = station.determine_station_type().id
            self.assertEqual(determined, t[1],
                             u"Ошибка определения в %s %s %s != %s" % (t[0], t[2], determined, t[1]))
