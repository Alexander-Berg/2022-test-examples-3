#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.types import BlueOffer, MarketSku, Model, Offer
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare_filter_docs(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.offers += [
            Offer(title='найдёныш 1', waremd5="BH8EPLtKmdLQhLUasgaOnA"),
            Offer(title='найдёныш 2', waremd5="KXGI8T3GP_pqjgdd7HfoHQ", hyperid=101),
        ]

        cls.index.models += [
            Model(title='найдёнка 3', hyperid=101),
            Model(title='найдёнка 4', hyperid=102),
        ]

    def test_filter_docs(self):
        """
        Проеряем работоспособность флагов, включающих на прайме фильтрацию
        документов по айдишникам, со схлапыванием и без
        """

        def offer(waremd5):
            return {'entity': 'offer', 'wareId': waremd5}

        def model(hyper, collapsed=False):
            result = {
                'entity': 'product',
                'id': hyper,
            }

            if collapsed:
                result['debug'] = {'isCollapsed': True}

            return result

        # Без схлапывания
        query = 'place=prime&text=найдёныш+найдёнка&debug=da'

        response = self.report.request_json(query)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    model(101),
                    model(102),
                    offer('BH8EPLtKmdLQhLUasgaOnA'),
                    offer('KXGI8T3GP_pqjgdd7HfoHQ'),
                ]
            },
            allow_different_len=False,
        )

        response = self.report.request_json(query + '&waremd5-to-ignore=BH8EPLtKmdLQhLUasgaOnA')
        self.assertFragmentIn(
            response,
            {
                'results': [
                    model(101),
                    model(102),
                    offer('KXGI8T3GP_pqjgdd7HfoHQ'),
                ]
            },
            allow_different_len=False,
        )

        response = self.report.request_json(query + '&filter-models=102')
        self.assertFragmentIn(
            response,
            {
                'results': [
                    model(101),
                    offer('BH8EPLtKmdLQhLUasgaOnA'),
                    offer('KXGI8T3GP_pqjgdd7HfoHQ'),
                ]
            },
            allow_different_len=False,
        )

        response = self.report.request_json(query + '&filter-models=101&waremd5-to-ignore=KXGI8T3GP_pqjgdd7HfoHQ')
        self.assertFragmentIn(
            response,
            {
                'results': [
                    model(102),
                    offer('BH8EPLtKmdLQhLUasgaOnA'),
                ]
            },
            allow_different_len=False,
        )

        # KXGI8T3GP_pqjgdd7HfoHQ отфильтруется, т.к. его модель в filter-models
        response = self.report.request_json(query + '&filter-models=101&filter-models=102')
        self.assertFragmentIn(
            response,
            {
                'results': [
                    offer('BH8EPLtKmdLQhLUasgaOnA'),
                ]
            },
            allow_different_len=False,
        )

        response = self.report.request_json(
            query + '&waremd5-to-ignore=BH8EPLtKmdLQhLUasgaOnA' + '&waremd5-to-ignore=KXGI8T3GP_pqjgdd7HfoHQ'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    model(101),
                    model(102),
                ]
            },
            allow_different_len=False,
        )

        # Со схлапыванием
        query = 'place=prime&text=найдёныш&debug=da&allow-collapsing=1'

        response = self.report.request_json(query)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    model(101, True),
                    offer('BH8EPLtKmdLQhLUasgaOnA'),
                ]
            },
            allow_different_len=False,
        )

        response = self.report.request_json(query + '&filter-models=101')
        self.assertFragmentIn(
            response,
            {
                'results': [
                    offer('BH8EPLtKmdLQhLUasgaOnA'),
                ]
            },
            allow_different_len=False,
        )

        # KXGI8T3GP_pqjgdd7HfoHQ не схлопнется в модель 101
        response = self.report.request_json(query + '&waremd5-to-ignore=KXGI8T3GP_pqjgdd7HfoHQ')
        self.assertFragmentIn(
            response,
            {
                'results': [
                    offer('BH8EPLtKmdLQhLUasgaOnA'),
                ]
            },
            allow_different_len=False,
        )

        response = self.report.request_json(query + '&filter-models=101' + '&waremd5-to-ignore=BH8EPLtKmdLQhLUasgaOnA')
        self.assertFragmentIn(response, {'results': []}, allow_different_len=False)

    @classmethod
    def prepare_filter_docs_blue(cls):
        cls.index.mskus += [
            MarketSku(
                fesh=101010,
                title='Найденный 11',
                hyperid=201,
                sku=3011,
                waremd5='Sku1-wdDXWsIiLVm1goleg',
                blue_offers=[BlueOffer()],
            ),
            MarketSku(
                fesh=101010,
                title='Найденный 12',
                hyperid=201,
                sku=3012,
                waremd5='Sku2-wdDXWsIiLVm1goleg',
                blue_offers=[BlueOffer()],
            ),
            MarketSku(
                fesh=101010,
                title='Найденный 21',
                hyperid=202,
                sku=3021,
                waremd5='Sku3-wdDXWsIiLVm1goleg',
                blue_offers=[BlueOffer()],
            ),
        ]

    def test_filter_docs_blue(self):
        """
        Проеряем работоспособность флагов, включающих на прайме фильтрацию
        документов по айдишникам, на синем
        """

        def model(hyper):
            return {
                'entity': 'product',
                'id': hyper,
            }

        query = 'place=prime&rgb=blue&text=найденный&debug=da'

        response = self.report.request_json(query)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    model(201),
                    model(202),
                ]
            },
            allow_different_len=False,
        )

        response = self.report.request_json(query + '&filter-models=201')
        self.assertFragmentIn(
            response,
            {
                'results': [
                    model(202),
                ]
            },
            allow_different_len=False,
        )

        response = self.report.request_json(query + '&filter-models=201' + '&filter-models=202')
        self.assertFragmentIn(response, {'results': []}, allow_different_len=False)


if __name__ == '__main__':
    main()
