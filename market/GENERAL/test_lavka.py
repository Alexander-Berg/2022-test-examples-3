#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.matcher import EmptyList
from core.types import HyperCategory, Offer, Region, Shop
from core.types.hypercategory import (
    EATS_CATEG_ID,
    CategoryStreamRecord,
    Stream,
)

FOOD_CATEGORY = EATS_CATEG_ID


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.market_access_settings.enabled = True
        cls.settings.market_access_settings.download_catstreams = True
        cls.index.category_streams += [
            CategoryStreamRecord(FOOD_CATEGORY, Stream.FMCG.value),
        ]
        cls.reqwizard.on_default_request().respond()

        cls.index.hypertree += [
            HyperCategory(
                hid=9040100,
                children=[
                    HyperCategory(hid=9040200),
                    HyperCategory(hid=9040300),
                ],
            ),
        ]

        cls.index.regiontree += [
            Region(
                rid=1,
                name='Московская область',
                region_type=Region.FEDERATIVE_SUBJECT,
                children=[
                    Region(rid=213, name='Москва'),
                    Region(rid=2, name="Санкт-Петербург"),
                    Region(rid=193, name='Воронеж', preposition='в', locative='Воронеже'),
                    Region(rid=56, name='Челябинск', preposition='в', locative='Челябинске'),
                    Region(rid=35, name='Краснодар', preposition='в', locative='Краснодаре'),
                ],
            )
        ]

        cls.index.offers += [
            Offer(
                title='молоко',
                shop_category_path='категория 1\\категория 2\\категория 3',
                shop_category_path_ids='1\\2\\3',
                original_sku='milk123',
                offer_source='market.cpa',
                hid=9040100,
                cpa=Offer.CPA_REAL,
            ),
            Offer(
                title='круассан',
                navigation_node_ids='1\\2\\3',
                navigation_node_names='Super cat\\Cat\\Uber cat',
                navigation_path_lengths='2\\1',
                hid=9040100,
                cpa=Offer.CPA_REAL,
            ),
        ]

        cls.index.offers += [
            Offer(
                title='лавка',
                shop_category_path='лавка',
                shop_category_path_ids='1',
                is_lavka=True,
                hid=FOOD_CATEGORY,
                cpa=Offer.CPA_REAL,
            ),
            Offer(
                title='еда',
                shop_category_path='еда',
                shop_category_path_ids='1',
                is_eda=True,
                hid=FOOD_CATEGORY,
                cpa=Offer.CPA_REAL,
            ),
        ]

        eats_shop = Shop(100500, is_eats=True, eats_and_lavka_id='100432', priority_region=213)
        cls.index.shops += [eats_shop]

        cls.index.offers += [
            Offer(
                title='ритейл',
                shop_category_path='ритейл',
                shop_category_path_ids='1',
                is_eda_retail=True,
                hid=FOOD_CATEGORY,
                waremd5=Offer.generate_waremd5('retail_offer'),
                shop=eats_shop,
                has_delivery_options=False,
                cpa=Offer.CPA_REAL,
            ),
            Offer(
                title='рестораны',
                shop_category_path='рестораны',
                shop_category_path_ids='1',
                is_eda_restaurants=True,
                hid=FOOD_CATEGORY,
                has_delivery_options=False,
                cpa=Offer.CPA_REAL,
            ),
        ]

    def test_category_being_leaf_is_not_essential_for_meta_formula(self):
        response = self.report.request_json(
            'place=prime&pp=18&rids=213&regset=1&ignore-has-gone=1'
            '&numdoc=100&text=молоко&entities=offer&allow-collapsing=0'
            '&market-force-business-id=1&enable-foodtech-offers=1&show-urls=offercard'
        )
        self.assertGreaterEqual(len(response['search']['results'][-1]['trace']['fullFormulaInfo']), 2)

    def test_lavka_is_cpa(self):
        response = self.report.request_json('place=lavka&text=лавка')
        self.assertFragmentIn(
            response,
            {
                'cpaCount': 1,
            },
        )

    def test_eats_is_cpa(self):
        response = self.report.request_json('place=lavka&text=еда')
        self.assertFragmentIn(
            response,
            {
                'cpaCount': 1,
            },
        )

    def test_output_category_path(self):
        """
        tests output category and original_id attrs
        """
        response = self.report.request_json('place=lavka&text=молоко')
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'shop_category_path': 'категория 1\\категория 2\\категория 3',
                'shop_category_path_ids': '1\\2\\3',
                'original_sku': 'milk123',
                'offer_source': 'market.cpa',
            },
        )

    def test_output_nav_nodes(self):
        """
        tests output nav node attrs
        """
        response = self.report.request_json('place=lavka&text=круассан')
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'navigationPaths': [
                    [{'id': '1', 'name': 'Super cat'}, {'id': '2', 'name': 'Cat'}],
                    [{'id': '3', 'name': 'Uber cat'}],
                ],
            },
        )

    def test_search_literals_in_lavka_place(self):
        """
        test searching offers with literals
        """

        response = self.report.request_json('place=lavka&text=лавка')
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'shop_category_path': 'лавка',
                'shop_category_path_ids': '1',
            },
        )

        response = self.report.request_json('place=lavka&text=еда')
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'shop_category_path': 'еда',
                'shop_category_path_ids': '1',
            },
        )

    def test_search_literals_in_prime_place(self):
        """
        test searching offers with literals, should not find them, unless cgi enable-foodtech-offers specified
        """

        for flag in [
            '',  # Без флагов
            '&enable-foodtech-offers=lavka&rearr-factors=market_enable_foodtech_offers=0',  # С флагом, но запретом на показ через реар
            '&enable-foodtech-offers=eda_retail',  # Другой флаг
            '&enable-foodtech-offers=eda_restaurants',  # Еще один
        ]:
            response = self.report.request_json('place=prime&text=лавка' + flag)
            self.assertFragmentIn(
                response,
                {'search': {'results': EmptyList()}},
            )

        for flag in [
            '&enable-foodtech-offers=1',  # Показываем все оферы
            '&enable-foodtech-offers=1&rearr-factors=market_enable_foodtech_offers=0',  # Реарр не мешает показывать все оферы
            '&enable-foodtech-offers=lavka',  # Оферы только лавки
            '&enable-foodtech-offers=lavka&rearr-factors=market_enable_foodtech_offers=eda_retail',  # Реарр не мешает показывать оферы лавки
            '&rearr-factors=market_enable_foodtech_offers=lavka',  # Оферы лавки через реарр
            '&enable-foodtech-offers=lavka,eda_retail',  # Оферы лавки и еще ретэйла
            '&enable-foodtech-offers=eda_retail&rearr-factors=market_enable_foodtech_offers=lavka',  # ретэйл не мешает показывать лавку через реарр
        ]:
            response = self.report.request_json('place=prime&text=лавка' + flag)
            self.assertFragmentIn(
                response,
                {
                    'entity': 'offer',
                    'shop_category_path': 'лавка',
                    'shop_category_path_ids': '1',
                    'foodtechType': 'lavka',
                },
            )

        for flag in [
            '',  # Без флагов
            '&enable-foodtech-offers=eda_restaurants&rearr-factors=market_enable_foodtech_offers=0',  # С флагом, но запретом на показ через реар
            '&enable-foodtech-offers=eda_retail&rearr-factors=market_enable_foodtech_offers=0',  # С флагом, но запретом на показ через реар
            '&enable-foodtech-offers=lavka',  # Другой флаг
        ]:
            response = self.report.request_json('place=prime&text=еда' + flag)
            self.assertFragmentIn(
                response,
                {'search': {'results': EmptyList()}},
            )

        for flag in [
            '&enable-foodtech-offers=1',  # Показываем все оферы
            '&enable-foodtech-offers=1&rearr-factors=market_enable_foodtech_offers=0',  # Реарр не мешает показывать все оферы
            '&enable-foodtech-offers=eda_restaurants',  # Оферы только еды
            '&enable-foodtech-offers=eda_restaurants&rearr-factors=market_enable_foodtech_offers=lavka',  # Реарр не мешает показывать оферы
            '&rearr-factors=market_enable_foodtech_offers=eda_restaurants',  # Оферы через реарр
            '&rearr-factors=market_enable_foodtech_offers=eda_retail',  # Оферы через реарр
            '&enable-foodtech-offers=lavka,eda_retail',  # Оферы лавки и еще ретэйла
            '&enable-foodtech-offers=lavka&rearr-factors=market_enable_foodtech_offers=eda_retail',  # параметр не мешает показывать через реарр
        ]:
            response = self.report.request_json('place=prime&text=еда' + flag)
            self.assertFragmentIn(
                response,
                {
                    'entity': 'offer',
                    'shop_category_path': 'еда',
                    'shop_category_path_ids': '1',
                },
            )

        for flag in [
            '',  # Без флагов
            '&enable-foodtech-offers=lavka',  # другой флаг
            '&enable-foodtech-offers=eda_restaurants',  # другой флаг
            '&enable-foodtech-offers=eda_retail&rearr-factors=market_enable_foodtech_offers=0',  # С флагом, но запретом на показ через реар
        ]:
            response = self.report.request_json('place=prime&text=ритейл' + flag)
            self.assertFragmentIn(
                response,
                {'search': {'results': EmptyList()}},
            )

        for flag in [
            '&enable-foodtech-offers=1',  # Показываем все оферы
            '&enable-foodtech-offers=1&rearr-factors=market_enable_foodtech_offers=0',  # Реарр не мешает показывать все оферы
            '&enable-foodtech-offers=eda_retail',  # Оферы только ретэйла
            '&enable-foodtech-offers=eda_retail&rearr-factors=market_enable_foodtech_offers=lavka',  # Реарр не мешает показывать оферы
            '&rearr-factors=market_enable_foodtech_offers=eda_retail',  # Оферы через реарр
            '&enable-foodtech-offers=lavka,eda_retail',  # Оферы лавки и еще ретэйла
            '&enable-foodtech-offers=lavka&rearr-factors=market_enable_foodtech_offers=eda_retail',  # параметр не мешает показывать через реарр
        ]:
            response = self.report.request_json('place=prime&text=ритейл&rids=213' + flag)
            self.assertFragmentIn(
                response,
                {
                    'entity': 'offer',
                    'shop_category_path': 'ритейл',
                    'shop_category_path_ids': '1',
                    'foodtechType': 'retail',
                    'shop': {'isEats': True},
                },
            )

        for flag in [
            '',  # Без флагов
            '&enable-foodtech-offers=lavka',  # Другой флаг
            '&enable-foodtech-offers=eda_retail',  # другой флаг
            '&enable-foodtech-offers=eda_restaurants&rearr-factors=market_enable_foodtech_offers=0',  # С флагом, но запретом на показ через реар
        ]:
            response = self.report.request_json('place=prime&text=рестораны&rids=213' + flag)
            self.assertFragmentIn(
                response,
                {'search': {'results': EmptyList()}},
            )

        for flag in [
            '&enable-foodtech-offers=1',  # Показываем все оферы
            '&enable-foodtech-offers=1&rearr-factors=market_enable_foodtech_offers=0',  # Реарр не мешает показывать все оферы
            '&enable-foodtech-offers=eda_restaurants',  # Оферы только ретэйла
            '&enable-foodtech-offers=eda_restaurants&rearr-factors=market_enable_foodtech_offers=lavka',  # Реарр не мешает показывать оферы
            '&rearr-factors=market_enable_foodtech_offers=eda_restaurants',  # Оферы через реарр
            '&enable-foodtech-offers=lavka,eda_restaurants',  # Оферы лавки и еще ресторана
            '&enable-foodtech-offers=lavka&rearr-factors=market_enable_foodtech_offers=eda_restaurants',  # параметр не мешает показывать через реарр
        ]:
            request = 'place=prime&text=рестораны&rids=213'
            # для клиентов Еды и лавки фильтрации нет
            response = self.report.request_json(request + "&client=lavka" + flag)
            self.assertFragmentIn(
                response,
                {
                    'entity': 'offer',
                    'shop_category_path': 'рестораны',
                    'shop_category_path_ids': '1',
                    'foodtechType': 'restaurants',
                },
            )

    def test_override_params(self):
        """
        test overriding cgi params with foodtech-cgi
        """
        response = self.report.request_json('place=prime&text=abc&foodtech-cgi=text%3Dлавка&enable-foodtech-offers=1')
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'shop_category_path': 'лавка',
                'shop_category_path_ids': '1',
            },
        )

    @classmethod
    def prepare_early_pre_early_filterig(cls):
        cls.index.offers += [
            Offer(title='сыр фудтех 1', is_lavka=True, hid=FOOD_CATEGORY),
            Offer(title='сыр маркет', hid=FOOD_CATEGORY),
            Offer(title='сыр фудтех 2', is_eda=True, hid=FOOD_CATEGORY),
        ]

    def test_early_pre_early_filtering_eda_offers(self):
        """
        проверяем что при включенной ранней фильтрации немаркетных офферов
        офферы еды и лавки не попадают в пантерный топ
        """
        response = self.report.request_json(
            'place=prime&text=сыр&enable-foodtech-offers=0&rearr-factors=panther_offer_tpsz=1;early_pre_early_not_market_docs_filtering=0'
        )
        self.assertFragmentNotIn(
            response,
            {
                'entity': 'offer',
            },
        )

        response = self.report.request_json(
            'place=prime&text=сыр&enable-foodtech-offers=0&rearr-factors=panther_offer_tpsz=1'
        )
        self.assertFragmentIn(
            response,
            {'entity': 'offer', 'titles': {"raw": "сыр маркет"}},
        )

    def test_lavka_literal(self):
        """
        Проверяем ограничение выдачи только оферами фудтеха
        """
        request = 'place=prime&text=сыр&enable-foodtech-offers=1&filter-offer-type={}'

        for flag, results in [
            ('', ['сыр фудтех 2', 'сыр маркет', 'сыр фудтех 1']),  # Все оферы разрешены
            ('eda_restaurants', ['сыр фудтех 2']),  # Только еда
            ('eda_retail', ['сыр фудтех 2']),  # Только еда
            ('lavka', ['сыр фудтех 1']),  # Только лавка
            ('lavka,eda_retail', ['сыр фудтех 1', 'сыр фудтех 2']),  # лавка и еда
        ]:
            response = self.report.request_json(request.format(flag))
            self.assertFragmentIn(
                response,
                {'results': [{'entity': 'offer', 'titles': {"raw": result}} for result in results]},
                allow_different_len=False,
            )

    def test_eats_and_lavka_id(self):
        '''
        Проверяем показ идентификатора магазина еды и лавки
        '''
        offerid = Offer.generate_waremd5('retail_offer')
        response = self.report.request_json(
            'place=offerinfo&offerid={}&enable-foodtech-offers=1&client=eats&rids=0&regset=1&show-urls='.format(offerid)
        )
        self.assertFragmentIn(
            response,
            [
                {
                    'entity': 'offer',
                    'wareId': offerid,
                    'shop': {
                        'eatsAndLavkaId': '100432',
                    },
                }
            ],
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()
