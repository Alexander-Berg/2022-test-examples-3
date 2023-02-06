#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import Offer


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.report_subrole = 'goods'
        cls.index.offers += [
            Offer(title="unfamily product", is_unfamily=True, adult=True),  # большинство будет иметь оба флага
            Offer(title="general product"),
        ]

    def test_should_filter_unfamily_goods(self):
        for place in ['prime', 'parallel']:
            response = self.report.request_json(
                'place={}'.format(place) + '&text=product' + '&rearr-factors=goods_enable_unfamily_goods_filtering=1'
            )

            self.assertFragmentIn(response, {"titles": {"raw": "general product"}})
            self.assertFragmentNotIn(response, {"titles": {"raw": "unfamily product"}})

    def test_should_show_unfamily_goods(self):
        for place in ['prime', 'parallel']:
            response = self.report.request_json(
                'place={}'.format(place)
                + '&text=product'
                + '&rearr-factors=goods_enable_unfamily_goods_filtering=1'
                + '&rearr-factors=goods_show_unfamily_goods=1'
            )

            self.assertFragmentIn(response, {"titles": {"raw": "general product"}})
            self.assertFragmentIn(response, {"titles": {"raw": "unfamily product"}})

    def test_ignore_flag_without_exp(self):
        for place in ['prime', 'parallel']:
            response = self.report.request_json('place={}'.format(place) + '&text=product')

            self.assertFragmentIn(response, {"titles": {"raw": "general product"}})
            # потому что так же adult
            self.assertFragmentNotIn(response, {"titles": {"raw": "unfamily product"}})

    def test_should_filter_adult_without_exp(self):
        for place in ['prime', 'parallel']:
            response = self.report.request_json('place={}'.format(place) + '&text=product')

        self.assertFragmentNotIn(response, {"titles": {"raw": "unfamily product"}})

    def test_family_requests(self):
        response = self.report.request_bs(
            'place=parallel&text=product' + '&rearr-factors=goods_show_unfamily_goods=1' + '&family=1'
        )
        self.assertFragmentNotIn(response, {})

        response = self.report.request_bs('place=parallel&text=product' + '&rearr-factors=goods_show_unfamily_goods=1')
        self.assertFragmentIn(response, {})

    def test_unfamily_blurring(self):
        import re

        picture_blurred_re = re.compile(r'13 1 Blurred picture: (.*)_blurred_exp1')
        picture_hd_blurred_re = re.compile(r'13 1 Blurred picture_hd: (.*)_blurred_exp1')

        def assert_blurred_picture_count_is_correct(response, expected_count):
            trace_wizard = str(response.get_trace_wizard())
            self.assertEqual(len(picture_blurred_re.findall(trace_wizard)), expected_count)
            self.assertEqual(len(picture_hd_blurred_re.findall(trace_wizard)), expected_count)

        response = self.report.request_bs_pb(
            'place=parallel&text=product&trace_wizard=1'
            + '&rearr-factors=goods_show_unfamily_goods=1'
            + '&rearr-factors=goods_enable_unfamily_goods_blurring=1'
        )
        assert_blurred_picture_count_is_correct(response, 1)

        response = self.report.request_bs_pb(
            'place=parallel&text=product&trace_wizard=1' + '&rearr-factors=goods_show_unfamily_goods=1'
        )
        assert_blurred_picture_count_is_correct(response, 0)

        response = self.report.request_bs_pb(
            'place=parallel&text=product&trace_wizard=1' + '&rearr-factors=goods_enable_unfamily_goods_blurring=1'
        )
        assert_blurred_picture_count_is_correct(response, 0)


if __name__ == '__main__':
    main()
