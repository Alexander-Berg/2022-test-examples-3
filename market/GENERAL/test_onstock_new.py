#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import HyperCategory, Model, Offer


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']
        cls.index.hypertree += [
            HyperCategory(
                hid=10101,
                children=[
                    HyperCategory(hid=1),
                ],
            ),
        ]

        cls.index.models += [
            Model(hyperid=3017201, hid=1, title='iphone 10'),  # old
            Model(hyperid=3017202, hid=1, title='iphone 11', new=True),  # new + offer
            Model(hyperid=3017203, hid=1, title='iphone 12', new=True),  # new w/o offer
        ]

        cls.index.offers += [
            Offer(title='iphone 1 again', hyperid=3017201, waremd5='n9XlAsFkD2JzjIqQjT6w9w'),
            Offer(title='iphone 11 again', hyperid=3017202, waremd5='n0YlAsFkD2JzjIqQjT6w9w'),
        ]

    def test_new_shown_with_onstock_no_rearr(self):
        """
        без rearr-флага новые покажутся
        """
        response = self.report.request_json('place=prime&hid=1&allow-collapsing=1&onstock=1')
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'id': 3017201,
                        'isNew': False,
                    },
                    {
                        'id': 3017202,
                        'isNew': True,
                    },
                    {
                        'id': 3017203,
                        'isNew': True,
                    },
                ]
            },
        )

    def test_new_shown_with_onstock_no_rearr_no_excludes(self):
        """
        без rearr-флага новые покажутся
        без excludes покажутся
        """
        response = self.report.request_json('place=prime&hid=1&allow-collapsing=1&onstock=1')
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'id': 3017201,
                        'isNew': False,
                    },
                    {
                        'id': 3017202,
                        'isNew': True,
                    },
                    {
                        'id': 3017203,
                        'isNew': True,
                    },
                ]
            },
        )

    def test_new_shown_with_onstock_no_rearr_excludes_0(self):
        """
        без rearr-флага
        с excludes=0
        покажутся
        """
        response = self.report.request_json('place=prime&hid=1&allow-collapsing=1&onstock=1&onstock-excludes-new=0')
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'id': 3017201,
                        'isNew': False,
                    },
                    {
                        'id': 3017202,
                        'isNew': True,
                    },
                    {
                        'id': 3017203,
                        'isNew': True,
                    },
                ]
            },
        )

    def test_new_not_show_with_onstock_no_rearr_excludes_1(self):
        """
        без rearr-флага
        с excludes=1
        не покажутся
        """
        response = self.report.request_json(
            'place=prime&hid=1&allow-collapsing=1&onstock=1&rearr-factors=market_onstock_includes_new=0'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'id': 3017201,
                        'isNew': False,
                    },
                    {
                        'id': 3017202,
                        'isNew': True,
                    },
                ]
            },
        )

        self.assertFragmentNotIn(
            response,
            {
                'results': [
                    {
                        'id': 3017203,
                        'isNew': True,
                    }
                ]
            },
        )

    def test_new_shown_with_onstock_rearr_enabled(self):
        """
        с включенным rearr-флагом новые покажутся тоже
        """
        response = self.report.request_json(
            'place=prime&hid=1&allow-collapsing=1&onstock=1&rearr-factors=market_onstock_includes_new=1'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'id': 3017201,
                        'isNew': False,
                    },
                    {
                        'id': 3017202,
                        'isNew': True,
                    },
                    {
                        'id': 3017203,
                        'isNew': True,
                    },
                ]
            },
        )

    def test_new_not_shown_with_onstock_rearr_disabled(self):
        """
        с выключенным rearr-флагом новые без офферов не покажутся
        """
        response = self.report.request_json(
            'place=prime&hid=1&allow-collapsing=1&onstock=1&rearr-factors=market_onstock_includes_new=0'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'id': 3017201,
                        'isNew': False,
                    },
                    {
                        'id': 3017202,
                        'isNew': True,
                    },
                ]
            },
        )

        self.assertFragmentNotIn(
            response,
            {
                'results': [
                    {
                        'id': 3017203,
                        'isNew': True,
                    }
                ]
            },
        )

    def test_new_not_shown_with_onstock_rearr_disabled_excludes_0(self):
        """
        с выключенным rearr-флагом и с excludes=0 новые без офферов не покажутся
        """
        response = self.report.request_json(
            'place=prime&hid=1&allow-collapsing=1&onstock=1&rearr-factors=market_onstock_includes_new=0&onstock-excludes-new=0'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'id': 3017201,
                        'isNew': False,
                    },
                    {
                        'id': 3017202,
                        'isNew': True,
                    },
                ]
            },
        )

        self.assertFragmentNotIn(
            response,
            {
                'results': [
                    {
                        'id': 3017203,
                        'isNew': True,
                    }
                ]
            },
        )


if __name__ == '__main__':
    main()
