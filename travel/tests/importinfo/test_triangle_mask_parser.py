# coding: utf-8
from datetime import date

import pytest

from travel.rasp.admin.importinfo.triangle_import.mask_parser import TriangleMaskTemplateParser
from travel.rasp.admin.lib.unittests import MaskComparisonMixIn
from travel.rasp.admin.lib.unittests.testcase import TestCase


class TestTriangleMaskParser(TestCase, MaskComparisonMixIn):
    def setUp(self):
        self.mask_parser = TriangleMaskTemplateParser(date(2011, 3, 1), date(2011, 3, 31), date(2011, 3, 1))
        super(TestTriangleMaskParser, self).setUp()

    def testEveryDay(self):
        self.assert_mask_equal_description(
            self.mask_parser.parse_template(u"ежедневно"),
            u"""
                март 2011
                пн   вт   ср   чт   пт   сб   вс
               # 1  # 2  # 3  # 4  # 5  # 6
               # 7  # 8  # 9  #10  #11  #12  #13
               #14  #15  #16  #17  #18  #19  #20
               #21  #22  #23  #24  #25  #26  #27
               #28  #29  #30  #31
            """
        )

        self.assert_mask_equal_description(
            self.mask_parser.parse_template(u'D'),
            u"""
                март 2011
                пн   вт   ср   чт   пт   сб   вс
               # 1  # 2  # 3  # 4  # 5  # 6
               # 7  # 8  # 9  #10  #11  #12  #13
               #14  #15  #16  #17  #18  #19  #20
               #21  #22  #23  #24  #25  #26  #27
               #28  #29  #30  #31
            """
        )

    def testEveryDayWithModifier(self):
        self.assert_mask_equal_description(
            self.mask_parser.parse_template(u"c 2011-01-01,ежедневно"),
            u"""
                март 2011
                пн   вт   ср   чт   пт   сб   вс
                    # 1  # 2  # 3  # 4  # 5  # 6
               # 7  # 8  # 9  #10  #11  #12  #13
               #14  #15  #16  #17  #18  #19  #20
               #21  #22  #23  #24  #25  #26  #27
               #28  #29  #30  #31
            """
        )

        self.assert_mask_equal_description(
            self.mask_parser.parse_template(u"from 2011-01-01,ежедневно"),
            u"""
                март 2011
                пн   вт   ср   чт   пт   сб   вс
                    # 1  # 2  # 3  # 4  # 5  # 6
               # 7  # 8  # 9  #10  #11  #12  #13
               #14  #15  #16  #17  #18  #19  #20
               #21  #22  #23  #24  #25  #26  #27
               #28  #29  #30  #31
            """
        )

        self.assert_mask_equal_description(
            self.mask_parser.parse_template(u"c 2011-03-10, ежедневно"),
            u"""
                март 2011
                пн   вт   ср   чт   пт   сб   вс
                      1    2    3    4    5    6
                 7    8    9  #10  #11  #12  #13
               #14  #15  #16  #17  #18  #19  #20
               #21  #22  #23  #24  #25  #26  #27
               #28  #29  #30  #31
            """
        )

        self.assert_mask_equal_description(
            self.mask_parser.parse_template(u"c 2011-03-10,до   2011-03-20, ежедневно"),
            u"""
                март 2011
                пн   вт   ср   чт   пт   сб   вс
                      1    2    3    4    5    6
                 7    8    9  #10  #11  #12  #13
               #14  #15  #16  #17  #18  #19  #20
                21   22   23   24   25   26   27
                28   29   30   31
            """
        )

        self.assert_mask_equal_description(
            self.mask_parser.parse_template(u"   до 2011-03-25, ежедневно"),
            u"""
                март 2011
                пн   вт   ср   чт   пт   сб   вс
                    # 1  # 2  # 3  # 4  # 5  # 6
               # 7  # 8  # 9  #10  #11  #12  #13
               #14  #15  #16  #17  #18  #19  #20
               #21  #22  #23  #24  #25   26   27
                28   29   30   31
            """
        )

    def testEven(self):
        self.assert_mask_equal_description(
            self.mask_parser.parse_template(u"четные"),
            u"""
                март 2011
                пн   вт   ср   чт   пт   сб   вс
                      1  # 2    3  # 4    5  # 6
                 7  # 8    9  #10   11  #12   13
               #14   15  #16   17  #18   19  #20
                21  #22   23  #24   25  #26   27
               #28   29  #30   31
            """
        )

        self.assert_mask_equal_description(
            self.mask_parser.parse_template(u'E'),
            u"""
                март 2011
                пн   вт   ср   чт   пт   сб   вс
                      1  # 2    3  # 4    5  # 6
                 7  # 8    9  #10   11  #12   13
               #14   15  #16   17  #18   19  #20
                21  #22   23  #24   25  #26   27
               #28   29  #30   31
            """
        )


    def testOdd(self):
        self.assert_mask_equal_description(
            self.mask_parser.parse_template(u"нечетные"),
            u"""
                март 2011
                пн   вт   ср   чт   пт   сб   вс
                    # 1    2  # 3    4  # 5    6
               # 7    8  # 9   10  #11   12  #13
                14  #15   16  #17   18  #19   20
               #21   22  #23   24  #25   26  #27
                28  #29   30  #31
            """
        )
        self.assert_mask_equal_description(
            self.mask_parser.parse_template(u'U'),
            u"""
                март 2011
                пн   вт   ср   чт   пт   сб   вс
                    # 1    2  # 3    4  # 5    6
               # 7    8  # 9   10  #11   12  #13
                14  #15   16  #17   18  #19   20
               #21   22  #23   24  #25   26  #27
                28  #29   30  #31
            """
        )


    def testWeekDays(self):
        self.assertRaises(TriangleMaskTemplateParser.ParseError, self.mask_parser.parse_template,  u"1278")

        self.assert_mask_equal_description(
            self.mask_parser.parse_template(u"167"),
            u"""
                март 2011
                пн   вт   ср   чт   пт   сб   вс
                      1    2    3    4  # 5  # 6
               # 7    8    9   10   11  #12  #13
               #14   15   16   17   18  #19  #20
               #21   22   23   24   25  #26  #27
               #28   29   30   31
            """
        )

    def testDateMaskWithAdditions(self):
        self.assert_mask_equal_description(
            self.mask_parser.parse_template(u"2011-03-29|2011-03-18|1"),
            u"""
                март 2011
                пн   вт   ср   чт   пт   сб   вс
                      1    2    3    4    5    6
               # 7    8    9   10   11   12   13
               #14   15   16   17  #18   19   20
               #21   22   23   24   25   26   27
               #28  #29   30   31
            """
        )

    def testNegativeMask(self):
        self.assertRaises(TriangleMaskTemplateParser.ParseError, self.mask_parser.parse_template,
                          u"127;####")

        self.assert_mask_equal_description(
            self.mask_parser.parse_template(u"ежедневно;кроме 2011-03-11|2011-03-12"),
            u"""
                март 2011
                пн   вт   ср   чт   пт   сб   вс
                    # 1  # 2  # 3  # 4  # 5  # 6
               # 7  # 8  # 9  #10   11   12  #13
               #14  #15  #16  #17  #18  #19  #20
               #21  #22  #23  #24  #25  #26  #27
               #28  #29  #30  #31
            """
        )

        self.assert_mask_equal_description(
            self.mask_parser.parse_template(u"ежедневно;except 2011-03-11|2011-03-12"),
            u"""
                март 2011
                пн   вт   ср   чт   пт   сб   вс
                    # 1  # 2  # 3  # 4  # 5  # 6
               # 7  # 8  # 9  #10   11   12  #13
               #14  #15  #16  #17  #18  #19  #20
               #21  #22  #23  #24  #25  #26  #27
               #28  #29  #30  #31
            """
        )

        self.assert_mask_equal_description(
            self.mask_parser.parse_template(u"ежедневно;- 2011-03-11|2011-03-12"),
            u"""
                март 2011
                пн   вт   ср   чт   пт   сб   вс
                    # 1  # 2  # 3  # 4  # 5  # 6
               # 7  # 8  # 9  #10   11   12  #13
               #14  #15  #16  #17  #18  #19  #20
               #21  #22  #23  #24  #25  #26  #27
               #28  #29  #30  #31
            """
        )

    def testComplexMask(self):
        self.assert_mask_equal_description(

            self.mask_parser.parse_template(
                # Маски разделяются на части по модификаторам 'с' и 'до'
                u"с 2011-03-01, до 2011-03-05,1234567; кроме 2011-03-26; кроме 2011-03-27"
                u";c 2011-03-19,167"
            ),
            u"""
                март 2011
                пн   вт   ср   чт   пт   сб   вс
                    # 1  # 2  # 3  # 4  # 5    6
                 7    8    9   10   11   12   13
                14   15   16   17   18  #19  #20
               #21   22   23   24   25  #26  #27
               #28   29   30   31
            """
        )

    def testEveryOtherDay(self):
        self.assert_mask_equal_description(
            self.mask_parser.parse_template(u"с 2011-03-01, до 2011-03-20, через день"),
            u"""
                март 2011
                пн   вт   ср   чт   пт   сб   вс
                    # 1    2  # 3    4  # 5    6
               # 7    8  # 9   10  #11   12  #13
                14  #15   16  #17   18  #19   20
                21   22   23   24   25   26   27
                28   29   30   31
            """
        )

        self.assert_mask_equal_description(
            self.mask_parser.parse_template(u"до 2011-03-20, from 2011-03-01/1"),
            u"""
                март 2011
                пн   вт   ср   чт   пт   сб   вс
                    # 1    2  # 3    4  # 5    6
               # 7    8  # 9   10  #11   12  #13
                14  #15   16  #17   18  #19   20
                21   22   23   24   25   26   27
                28   29   30   31
            """
        )

    def testEveryNumberDay(self):
        self.assert_mask_equal_description(
            self.mask_parser.parse_template(u"с 2011-03-01, до 2011-03-20, через 2 дня"),
            u"""
                март 2011
                пн   вт   ср   чт   пт   сб   вс
                    # 1    2    3  # 4    5    6
               # 7    8    9  #10   11   12  #13
                14   15  #16   17   18  #19   20
                21   22   23   24   25   26   27
                28   29   30   31
            """
        )

        self.assert_mask_equal_description(
            self.mask_parser.parse_template(u"from 2011-03-01, to 2011-03-20, from 2011-03-16/2"),
            u"""
                март 2011
                пн   вт   ср   чт   пт   сб   вс
                      1    2    3    4    5    6
                 7    8    9   10   11   12   13
                14   15  #16   17   18  #19   20
                21   22   23   24   25   26   27
                28   29   30   31
            """
        )

        self.assert_mask_equal_description(
            self.mask_parser.parse_template(u"с 2011-03-01, до 2011-03-20, через 3"),
            u"""
                март 2011
                пн   вт   ср   чт   пт   сб   вс
                    # 1    2    3    4  # 5    6
                 7    8  # 9   10   11   12  #13
                14   15   16  #17   18   19   20
                21   22   23   24   25   26   27
                28   29   30   31
            """
        )

        self.assert_mask_equal_description(
            self.mask_parser.parse_template(u"с 2011-03-01, до 2011-03-20, через 5 дней"),
            u"""
                март 2011
                пн   вт   ср   чт   пт   сб   вс
                    # 1    2    3    4    5    6
               # 7    8    9   10   11   12  #13
                14   15   16   17   18  #19   20
                21   22   23   24   25   26   27
                28   29   30   31
            """
        )

        self.assertRaises(TriangleMaskTemplateParser.ParseError, self.mask_parser.parse_template,  u"через")
        self.assertRaises(TriangleMaskTemplateParser.ParseError, self.mask_parser.parse_template,  u"через 1")
        self.assertRaises(TriangleMaskTemplateParser.ParseError, self.mask_parser.parse_template,  u"через 3")

    def testOnlyExceptMask(self):
        self.assert_mask_equal_description(
            self.mask_parser.parse_template(u"с 2011-03-01, до 2011-03-20, кроме 234567;"
                                            u"с 2011-03-20, до 2011-03-31, кроме 123456"),
            u"""
                март 2011
                пн   вт   ср   чт   пт   сб   вс
                      1    2    3    4    5    6
               # 7    8    9   10   11   12   13
               #14   15   16   17   18   19  #20
                21   22   23   24   25   26  #27
                28   29   30   31
            """
        )

        self.assert_mask_equal_description(
            self.mask_parser.parse_template(u"с 2011-03-01, до 2011-03-20, кроме 3h;"),
            # 7, 8 марта выходной, а 5 рабочий день, перенос понедельника
            u"""
                март 2011
                пн   вт   ср   чт   пт   сб   вс
                    # 1    2  # 3  # 4  # 5    6
                 7    8    9  #10  #11   12   13
               #14  #15   16  #17  #18   19   20
                21   22   23   24   25   26   27
                28   29   30   31
            """
        )

    def testAFMask(self):
        self.assert_mask_equal_description(
            self.mask_parser.parse_template(u"с 2011-03-01, до 2011-03-20, 1E;"),
            u"""
                март 2011
                пн   вт   ср   чт   пт   сб   вс
                      1  # 2    3  # 4    5  # 6
               # 7  # 8    9  #10   11  #12   13
               #14   15  #16   17  #18   19  #20
                21   22   23   24   25   26   27
                28   29   30   31
            """
        )
