# -*- coding: utf-8 -*-

from datetime import date

from travel.rasp.admin.lib.unittests.testcase import TestCase
from travel.rasp.admin.scripts.schedule.utils.mask_builders import MaskBuilder
from travel.rasp.admin.scripts.schedule.utils.afmasktext import AFTextMatcher, AFTextBuilder, OldAfMaskBuilder, DAYS_SEPARATOR


AF_SUITE = u'''
шаблон    ;строка на сегодня                ;строка на завтра
   -57    ;кроме пт{0}вс                    ;кроме пн{0}сб
   -56    ;кроме пт{0}сб                    ;кроме сб{0}вс
   -34    ;кроме ср{0}чт                    ;кроме чт{0}пт
   -24    ;кроме вт{0}чт                    ;кроме ср{0}пт
   -16    ;кроме пн{0}сб                    ;кроме вт{0}вс
    -7    ;кроме вс                         ;кроме пн
    -3    ;кроме ср                         ;кроме чт
    -2    ;кроме вт                         ;кроме ср
    5     ;пт                               ;сб
    7     ;вс                               ;пн
    16    ;пн{0}сб                          ;вт{0}вс
    24    ;вт{0}чт                          ;ср{0}пт
    34    ;ср{0}чт                          ;чт{0}пт
    56    ;пт{0}сб                          ;сб{0}вс
    57    ;пт{0}вс                          ;пн{0}сб
   124    ;пн{0}вт{0}чт                     ;вт{0}ср{0}пт
   1567   ;пн{0}пт{0}сб{0}вс                ;пн{0}вт{0}сб{0}вс
   3567   ;ср{0}пт{0}сб{0}вс                ;пн{0}чт{0}сб{0}вс
 1234567  ;ежедневно                        ;ежедневно
    3H    ;ср и выходные                    ;пн{0}чт{0}вс
    4H    ;чт и выходные                    ;пн{0}пт{0}вс
    5H    ;по пятницам и выходным           ;пн и выходные
   -5H    ;кроме пятниц и выходных          ;кроме пн и выходных
    E     ;по четным числам                 ;по нечетным числам
    H     ;по выходным                      ;пн{0}вс и праздн.дни
    U     ;по нечетным числам               ;по четным числам
    W     ;по будням                        ;кроме пн{0}вс
'''.format(DAYS_SEPARATOR)


class AfMaskText(TestCase):
    longMessage = True
    mask_codes = [
        '1', '2', '3', '4', '5', '6', '7', '-1', '-2', '-3', '-4', '-5', '-6', '-7',
        'H', '-H', 'W', '-W', '<d[2-4]>', '<d[5-6]>', '5H', '17H', '-17H', '<d[1-4]>H'
    ]

    def setUp(self):
        self.matcher = AFTextMatcher(self.mask_codes)
        self.builder = AFTextBuilder()

    def testMatcher(self):
        self.check_match(u'1', ['1'], [])
        self.check_match(u'H', ['H'], [])

        self.check_match(u'H-1', ['H'], ['-1'])

        self.check_match(u'12', ['<d[2-4]>'], [])
        self.check_match(u'12-1', ['<d[2-4]>'], ['-1'])
        self.check_match(u'12H', ['<d[1-4]>H'], [])
        self.check_match(u'1H', ['<d[1-4]>H'], [])

        self.check_match(u'5H', ['5H'], [])
        self.check_match(u'5H-17H', ['5H'], ['-17H'])

        self.check_match(u'12HW', ['1', '2', 'H', 'W'], [])
        self.check_match(u'12-123H', ['<d[2-4]>'], ['-1', '-2', '-3', '-H'])

    def check_match(self, template, positive, negative):
        positive_matches, negative_matches = self.matcher.find_compound_match(template)

        self.assertListEqual([pm.match_template.code for pm in positive_matches], positive)
        self.assertListEqual([nm.match_template.code for nm in negative_matches], negative)

    def testTextBuilderTemplates(self):
        self.check_mask('ru', u'1', None, None, u'пн')
        self.check_mask('ru', u'12', None, None, u'пн{}вт'.format(DAYS_SEPARATOR))

        self.check_mask('ru', u'124567H', None, None, u'кроме ср и выходные')
        self.check_mask('ru', u'12H', None, None, u'пн{}вт и выходные'.format(DAYS_SEPARATOR))
        self.check_mask('ru', u'2H', None, None, u'вт и выходные')

        self.check_mask('ru', u'-3U', None, None, u'кроме ср, нечетных')
        self.check_mask('ru', u'167F', None, None, u'пн{0}сб{0}вс и праздн.дни'.format(DAYS_SEPARATOR))
        self.check_mask('ru', u'-167F', None, None, u'кроме пн{0}сб{0}вс, праздн.дней'.format(DAYS_SEPARATOR))

        self.check_mask('ru', u'D', None, None, u'ежедневно')
        self.check_mask('ru', u'2134567', None, None, u'ежедневно')

        self.check_mask('ru', u'C', None, None, u'отмена')

    def testTextBuildVariants(self):
        self.check_mask('ru', u'1', None, None, u'пн')
        self.check_mask('ru', u'-1', None, None, u'кроме пн')
        self.check_mask('ru', u'E-1', None, None, u'по четным числам кроме пн')

        self.check_mask('ru', u'1', date(2013, 3, 10), None, u'пн с 10 марта')
        self.check_mask('ru', u'1', None, date(2013, 3, 20), u'пн по 20 марта')
        self.check_mask('ru', u'1', date(2013, 3, 10), date(2013, 3, 20), u'пн с 10 марта по 20 марта')

        self.check_mask('ru', u'-1', date(2013, 3, 10), None, u'кроме пн с 10 марта')
        self.check_mask('ru', u'-1', None, date(2013, 3, 20), u'кроме пн по 20 марта')
        self.check_mask('ru', u'-1', date(2013, 3, 10), date(2013, 3, 20), u'кроме пн с 10 марта по 20 марта')

        self.check_mask('ru', u'E-1', date(2013, 3, 10), None, u'по четным числам кроме пн с 10 марта')
        self.check_mask('ru', u'E-1', None, date(2013, 3, 20), u'по четным числам кроме пн по 20 марта')
        self.check_mask('ru', u'E-1', date(2013, 3, 10), date(2013, 3, 20), u'по четным числам кроме пн с 10 марта по 20 марта')

    def check_mask(self, lang, template, start, end, result):
        built_result = self.builder.build_day_texts(template, start, end)[lang]
        assert built_result == result

    def testShifts(self):
        self.check_shift(u'1', -1, u'7')
        self.check_shift(u'1', 1, u'2')

        self.check_shift(u'U', -1, u'E')
        self.check_shift(u'U', 1, u'E')

        self.check_shift(u'5H', 1, u'1H')
        self.check_shift(u'-5H', 1, u'-1H')

    def check_shift(self, template, shift, result):
        got_result = self.builder.shift_template(template, shift)
        self.assertEqual(got_result, result)

    def testAfTextSuite(self):
        for row in AF_SUITE.strip().splitlines()[1:]:
            template_code, today_text, tomorrow_text = map(unicode.strip, row.split(u';'))

            texts = self.builder.build_range_day_texts(template_code)
            fail_text = u"Ошибка преобразования '{}' '{}' '{}'".format(
                template_code, today_text, tomorrow_text
            )

            self.assertEqual(texts[0]['ru'], today_text, fail_text)
            self.assertEqual(texts[1]['ru'], tomorrow_text, fail_text)


class TestAfMaskBuilder(TestCase):
    class_fixtures = ['travel.rasp.admin.tester.fixtures.www:countries.yaml']

    @classmethod
    def setUpTestData(cls):
        super(TestAfMaskBuilder, cls).setUpTestData()

        cls.start, cls.end, cls.today = date(2012, 3, 1), date(2012, 3, 31), date(2012, 3, 1)
        cls.mask_builder = MaskBuilder(cls.start, cls.end, cls.today)
        cls.af_mask_builder = OldAfMaskBuilder(cls.start, cls.end, cls.today)

    def testCheckIsAfMask(self):
        self.assertTrue(self.af_mask_builder.is_af_mask_text(u'W'))
        self.assertTrue(self.af_mask_builder.is_af_mask_text(u'D23W'))
        self.assertTrue(self.af_mask_builder.is_af_mask_text(u'EUH'))
        self.assertTrue(self.af_mask_builder.is_af_mask_text(u'234'))

        self.assertFalse(self.af_mask_builder.is_af_mask_text(u'eUH'))
        self.assertFalse(self.af_mask_builder.is_af_mask_text(u'p'))
        self.assertFalse(self.af_mask_builder.is_af_mask_text(u'p-5'))

    def testDayOfWeek(self):
        self.assertEqual(
            self.mask_builder.mask_from_days_of_week(u'1234'),
            self.af_mask_builder.parse_af_text(u'1234')
        )

    def testEveryDay(self):
        self.assertEqual(
            self.mask_builder.daily_mask(),
            self.af_mask_builder.parse_af_text(u'D')
        )

    def testEven(self):
        self.assertEqual(
            self.mask_builder.even_mask(),
            self.af_mask_builder.parse_af_text(u'E')
        )

    def testCancelled(self):
        assert self.af_mask_builder.parse_af_text(u'С') == self.mask_builder.empty_mask()  # en C
        assert self.af_mask_builder.parse_af_text(u'C') == self.mask_builder.empty_mask()  # ru C

