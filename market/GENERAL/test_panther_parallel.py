#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import Model, Offer


class T(TestCase):
    @classmethod
    def prepare(cls):
        """Включаем формирование Пантерного индекса,
        создаем оферы для колдунщика
        """
        cls.index.models += [
            Model(title='iphone 7 model 1'),
            Model(title='iphone 7 model 2'),
            Model(title='iphone 7 model 3'),
            Model(title='iphone 7 model 4'),
        ]

        cls.index.offers += [
            Offer(title='iphone 7 offer 1'),
            Offer(title='iphone 7 offer 2'),
            Offer(title='iphone 7 offer 3'),
            Offer(title='iphone 7 offer 4'),
        ]

    def test_offers_wizard(self):
        """Проверяем, что Пантера выключается флагом parallel_allow_panther=0"""
        response = self.report.request_bs('place=parallel&text=iphone+7&debug=da')
        self.assertIn('tweakBasesearchBehavior(): Enabling Panther]', str(response))
        self.assertFragmentIn(response, {"market_offers_wizard": []})

        response = self.report.request_bs(
            'place=parallel&text=iphone+7&rearr-factors=parallel_allow_panther=0&debug=da'
        )
        self.assertNotIn('tweakBasesearchBehavior(): Enabling Panther]', str(response))
        self.assertFragmentIn(response, {"market_offers_wizard": []})

    def test_partial_panther_disabling(self):
        """Проверяем, что Пантера частично отключается для оферов и моделей"""
        query = 'place=parallel&text=iphone+7&debug=da&rearr-factors='

        response = self.report.request_bs(query)
        self.assertIn('tweakBasesearchBehavior(): Enabling Panther]', str(response))
        self.assertNotIn('tweakBasesearchBehavior(): Disabling Panther for collection &apos;SHOP&apos;]', str(response))
        self.assertNotIn(
            'tweakBasesearchBehavior(): Disabling Panther for collection &apos;SHOP_UPDATE&apos;]', str(response)
        )
        self.assertNotIn(
            'tweakBasesearchBehavior(): Disabling Panther for collection &apos;MODEL&apos;]', str(response)
        )

        response = self.report.request_bs(query + 'parallel_allow_offers_panther=0;')
        self.assertIn('tweakBasesearchBehavior(): Enabling Panther]', str(response))
        self.assertIn('tweakBasesearchBehavior(): Disabling Panther for collection &apos;SHOP&apos;]', str(response))
        self.assertIn(
            'tweakBasesearchBehavior(): Disabling Panther for collection &apos;SHOP_UPDATE&apos;]', str(response)
        )
        self.assertNotIn(
            'tweakBasesearchBehavior(): Disabling Panther for collection &apos;MODEL&apos;]', str(response)
        )

        response = self.report.request_bs(query + 'parallel_allow_models_panther=0;')
        self.assertIn('tweakBasesearchBehavior(): Enabling Panther]', str(response))
        self.assertNotIn('tweakBasesearchBehavior(): Disabling Panther for collection &apos;SHOP&apos;]', str(response))
        self.assertNotIn(
            'tweakBasesearchBehavior(): Disabling Panther for collection &apos;SHOP_UPDATE&apos;]', str(response)
        )
        self.assertIn('tweakBasesearchBehavior(): Disabling Panther for collection &apos;MODEL&apos;]', str(response))


if __name__ == '__main__':
    main()
