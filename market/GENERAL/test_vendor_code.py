#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.types import MnPlace, Offer
from core.testcase import TestCase, main
from core.matcher import Absent


class T(TestCase):
    @classmethod
    def prepare_has_equal_vendor_code_feature(cls):
        cls.index.offers += [
            Offer(title='VENDOR', vendor_code='VENDOR-CODE-123', waremd5='TTnVlqbztMi95ithBNMa3g'),
            Offer(title='VENDOR', vendor_code='123-CODE-VENDOR', waremd5='BH8EPLtKmdLQhLUasgaOnA'),
        ]

    def test_has_equal_vendor_code_feature(self):
        """
        Проверяем, что флаг EQUAL_VENDOR_CODE есть в фичалоге
        """

        request = 'place=prime&text={}'
        _ = self.report.request_json(request.format("VENDOR-CODE-123"))
        self.feature_log.expect(ware_md5='TTnVlqbztMi95ithBNMa3g', equal_vendor_code=1)
        self.feature_log.expect(ware_md5='BH8EPLtKmdLQhLUasgaOnA', equal_vendor_code=Absent())

    @classmethod
    def prepare_disable_threshold_with_vendor_code(cls):
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1).respond(0.3)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2).respond(0.4)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3).respond(0.6)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 4).respond(0.7)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 5).respond(0.1)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 6).respond(0.2)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 7).respond(0.25)

        cls.index.offers = [
            Offer(title='offer 1', vendor_code="offer 90NR00N1-M01360", ts=1),
            Offer(title='offer 2', vendor_code="offer FIT IT_62760", ts=2),
            Offer(title="offer 3", vendor_code="offer 90NR00N1-M01360", ts=3),
            Offer(title="offer 4", vendor_code="offer FIT IT_62760", ts=4),
            Offer(title="offer 5", vendor_code="offer 64196TSP2", ts=5),
            Offer(title="offer 6", vendor_code="offer 62760", ts=6),
            Offer(title="offer 7", ts=7),
        ]

    def test_disable_threshold_with_vendor_code(self):
        """
        Проверяем, что при совпадении запроса и вендор кода оффера для этого
        оффера отключается threshold
        """

        def check(text, with_rev_flag, titles):
            request = 'place=prime&text={}&rearr-factors=' 'market_relevance_formula_threshold=0.5;'
            if with_rev_flag:
                request += 'market_disable_threshold_when_has_vendor_code=0;'

            response = self.report.request_json(request.format(text))

            self.assertFragmentIn(
                response, {"results": [{"titles": {"raw": title}} for title in titles]}, allow_different_len=False
            )

        check('offer 90NR00N1-M01360', with_rev_flag=True, titles=['offer 3', 'offer 4'])

        check('offer FIT IT_62760', with_rev_flag=True, titles=['offer 3', 'offer 4'])

        check('offer 90NR00N1-M01360', with_rev_flag=False, titles=['offer 1', 'offer 3', 'offer 4'])

        check('offer FIT IT_62760', with_rev_flag=False, titles=['offer 2', 'offer 3', 'offer 4'])

        # При не точном совпадении эффект работать перестаёт
        check('offer offer 90NR00N1-M01360', with_rev_flag=False, titles=['offer 3', 'offer 4'])

        check('offer offer FIT IT_62760', with_rev_flag=False, titles=['offer 3', 'offer 4'])


if __name__ == '__main__':
    main()
