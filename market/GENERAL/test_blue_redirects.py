#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Currency, NavCategory, Shop, Suggestion, Tax
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += [
            'market_disable_redirect_to_model_by_formula=0'  # по умолчанию в проде выключены редиректы в модель
        ]

        cls.index.shops += [
            Shop(
                # Виртуальный магазин синего маркета
                fesh=104,
                datafeed_id=1,
                priority_region=213,
                regions=[213],
                name='virtual_shop',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                cpa=Shop.CPA_REAL,
            ),
        ]

    @classmethod
    def prepare_blue_suggest_redirects(cls):
        cls.index.navtree += [
            NavCategory(nid=123125, hid=111),
        ]

        cls.suggester.on_default_request().respond()
        cls.suggester.on_custom_url_request(part='blue model', location='suggest-market-rich-blue').respond(
            suggestions=[
                Suggestion(
                    part='blue model',
                    url='/product/blue-model/9001',
                ),
            ],
        )
        cls.suggester.on_custom_url_request(part='алиса', location='suggest-market-rich-blue').respond(
            suggestions=[
                Suggestion(part='алиса', url='/catalog/alisa/123123?hid=111&gfilter=1801946:1871375&suggest=1'),
            ]
        )

    def test_blue_suggests(self):
        """
        Синие ЧПУ должны корректно парситься. Пример синего ЧПУ: /product/<slug>/<id>
        Проверка, что парсятся ЧПУ от саджестера
        """
        response = self.report.request_json(
            'place=prime&text=blue model&cvredirect=1&rgb=blue&cpa=real&non-dummy-redirects=1'
            '&rearr-factors=market_disable_suggest_model_redirect=0;'
            'market_use_new_suggest_for_redirect=0'
        )
        self.assertFragmentIn(
            response,
            {
                'redirect': {
                    'params': {
                        'slug': ['blue-model'],
                        'modelid': ['9001'],
                    },
                    'target': 'product',
                },
            },
        )

        response = self.report.request_json(
            'place=prime&text=алиса&cvredirect=1&rgb=blue&cpa=real&non-dummy-redirects=1'
            '&rearr-factors=market_disable_suggest_model_redirect=0;'
            'market_use_new_suggest_for_redirect=0'
        )
        self.assertFragmentIn(
            response,
            {
                'redirect': {
                    'params': {
                        'slug': ['alisa'],
                        'hid': ['111'],
                        'nid': ['123123'],
                    },
                    'target': 'catalog',
                },
            },
        )

    def test_disable_blue_model_suggest_redirect(self):
        """Проверяем, что флаг market_disable_suggest_model_redirect
        отключает модельные саджестовые редиректы на синем маркете
        https://st.yandex-team.ru/MARKETOUT-25763
        """

        # Без флага редиректы отключены
        response = self.report.request_json(
            'place=prime&text=blue+model&cvredirect=1&rgb=blue&cpa=real&non-dummy-redirects=1&debug=1'
            '&rearr-factors=market_use_new_suggest_for_redirect=0'
        )
        self.assertFragmentNotIn(response, {"redirect": {}})
        self.assertFragmentIn(response, 'Model redirect from suggest is disabled')

        # Под market_disable_suggest_model_redirect=1 редиректы отключены
        response = self.report.request_json(
            'place=prime&text=blue+model&cvredirect=1&rgb=blue&cpa=real&non-dummy-redirects=1&debug=1'
            '&rearr-factors=market_disable_suggest_model_redirect=1;'
            'market_use_new_suggest_for_redirect=0'
        )
        self.assertFragmentNotIn(response, {"redirect": {}})
        self.assertFragmentIn(response, 'Model redirect from suggest is disabled')

        # Под market_disable_suggest_model_redirect=0 редиректы включены
        response = self.report.request_json(
            'place=prime&text=blue+model&cvredirect=1&rgb=blue&cpa=real&non-dummy-redirects=1&debug=1'
            '&rearr-factors=market_disable_suggest_model_redirect=0;'
            'market_use_new_suggest_for_redirect=0'
        )
        self.assertFragmentIn(
            response,
            {
                'redirect': {
                    'params': {
                        'modelid': ['9001'],
                    },
                    'target': 'product',
                },
            },
        )
        self.assertFragmentNotIn(response, 'Model redirect from suggest is disabled')


if __name__ == '__main__':
    main()
