#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa


from core.types import (
    BlueOffer,
    Currency,
    HyperCategory,
    HyperCategoryType,
    MarketSku,
    MnPlace,
    Model,
    Offer,
    QueryIntList,
    Shop,
    Tax,
)
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.shops += [
            Shop(
                fesh=1886710,
                name='Покупки',
                datafeed_id=188671001,
                priority_region=213,
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
            ),
            Shop(
                fesh=12345,
                name='Реально синий магазин',
                datafeed_id=12345,
                priority_region=213,
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                blue=Shop.BLUE_REAL,
            ),
        ]

    @classmethod
    def prepare_cpa_offers(cls):
        cls.index.hypertree += [
            HyperCategory(hid=4057000, name='Мобильные телефоны', output_type=HyperCategoryType.GURU)
        ]

        cls.index.models += [
            Model(hyperid=4057001, title='Nokia 3310', hid=4057000),
            Model(hyperid=4057002, title='Alcatel', hid=4057000),
            Model(hyperid=4057003, title='Philips Xenium E169', hid=4057000),
            Model(hyperid=4057004, title='Nokia 130', hid=4057000),
            Model(hyperid=4057005, title='Philips Xenium E255', hid=4057000),
            Model(hyperid=4057006, title='Nokia 230', hid=4057000),
            Model(hyperid=4057007, title='Iphone 7 Plus', hid=4057000),
            Model(hyperid=4057008, title='Xiaomi Redmi 9', hid=4057000),
            Model(hyperid=4057009, title='Iphone 12', hid=4057000),
            Model(hyperid=4057010, title='Iphone 7', hid=4057000),
            Model(hyperid=4057011, title='Xiaomi Redmi 5', hid=4057000),
            Model(hyperid=4057012, title='Xiaomi Redmi 9A', hid=4057000),
            Model(hyperid=4057013, title='Xiaomi Poco X3 ', hid=4057000),
            Model(hyperid=4057014, title='realme C21', hid=4057000),
            Model(hyperid=4057015, title='Samsung Galaxy A12', hid=4057000),
        ]

        cls.index.offers += [
            Offer(hyperid=4057001, title='Телефон Nokia 3310', hid=4057000),
            Offer(hyperid=4057002, title='Телефон Alcatel', hid=4057000),
            Offer(hyperid=4057003, title='Телефон Philips Xenium E169', hid=4057000),
            Offer(hyperid=4057004, title='Телефон Nokia 130', hid=4057000),
        ]

        cls.index.mskus += [
            MarketSku(
                hyperid=4057001,
                sku=4057001,
                blue_offers=[BlueOffer(ts=4057001, title='Телефон Nokia 3310', feedid=12345)],
            ),
            MarketSku(
                hyperid=4057002, sku=4057002, blue_offers=[BlueOffer(ts=4057002, title='Телефон Alcatel', feedid=12345)]
            ),
            MarketSku(
                hyperid=4057003,
                sku=4057003,
                blue_offers=[BlueOffer(ts=4057003, title='Телефон Philips Xenium E169', feedid=12345)],
            ),
            MarketSku(
                hyperid=4057004,
                sku=4057004,
                blue_offers=[BlueOffer(ts=4057004, title='Телефон Nokia 130', feedid=12345)],
            ),
            MarketSku(
                hyperid=4057005,
                sku=4057005,
                blue_offers=[BlueOffer(ts=4057005, title='Телефон Philips Xenium E255', feedid=12345)],
            ),
            MarketSku(
                hyperid=4057006,
                sku=4057006,
                blue_offers=[BlueOffer(ts=4057006, title='Телефон Nokia 230', feedid=12345)],
            ),
            MarketSku(
                hyperid=4057007,
                sku=4057007,
                blue_offers=[BlueOffer(ts=4057007, title='Телефон Iphone 7 Plus', feedid=12345)],
            ),
            MarketSku(
                hyperid=4057008,
                sku=4057008,
                blue_offers=[BlueOffer(ts=4057008, title='Телефон Xiaomi Redmi 9', feedid=12345)],
            ),
            MarketSku(
                hyperid=4057009,
                sku=4057009,
                blue_offers=[BlueOffer(ts=4057009, title='Телефон Iphone 12', feedid=12345)],
            ),
            MarketSku(
                hyperid=4057010,
                sku=4057010,
                blue_offers=[BlueOffer(ts=4057010, title='Телефон Iphone 7', feedid=12345)],
            ),
            MarketSku(
                hyperid=4057011,
                sku=4057011,
                blue_offers=[BlueOffer(ts=4057011, title='Телефон Xiaomi Redmi 5', feedid=12345)],
            ),
            MarketSku(
                hyperid=4057012,
                sku=4057012,
                blue_offers=[BlueOffer(ts=4057012, title='Телефон Xiaomi Redmi 9A', feedid=12345)],
            ),
            MarketSku(
                hyperid=4057013,
                sku=4057013,
                blue_offers=[BlueOffer(ts=4057013, title='Телефон Xiaomi Poco X3', feedid=12345)],
            ),
            MarketSku(
                hyperid=4057014,
                sku=4057014,
                blue_offers=[BlueOffer(ts=4057014, title='Телефон realme C21', feedid=12345)],
            ),
            MarketSku(
                hyperid=4057015,
                sku=4057015,
                blue_offers=[BlueOffer(ts=4057015, title='Телефон Samsung Galaxy A12', feedid=12345)],
            ),
        ]

        for i in range(1, 16):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 4057000 + i).respond(0.9 - 0.01 * i)

        cls.index.parallel_cpa_query_models += [
            QueryIntList(
                query='телефон',
                integer_ids=[
                    4057008,
                    4057009,
                    4057012,
                    4057013,
                    4057014,
                    4057011,
                    4057007,
                    4057010,
                ],
            )
        ]

    def test_cpa_query_models(self):
        """Ограничиваем список моделей для CPA-врезки
        https://st.yandex-team.ru/MARKETOUT-40545
        """
        request = 'place=parallel&text=телефон&rearr-factors={}'

        # Проверяем порядок gод обратным флагом
        rearr_factors = [
            'market_cpa_offers_incut_count=10',
            'market_parallel_use_fixed_queries=0',
        ]
        response = self.report.request_bs_pb(request.format(';'.join(rearr_factors)))
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "cpaItems": [
                            {"modelId": "4057001"},
                            {"modelId": "4057002"},
                            {"modelId": "4057003"},
                            {"modelId": "4057004"},
                            {"modelId": "4057005"},
                            {"modelId": "4057006"},
                            {"modelId": "4057007"},
                            {"modelId": "4057008"},
                            {"modelId": "4057009"},
                            {"modelId": "4057010"},
                            {"modelId": "4057011"},
                        ]
                    }
                }
            },
            allow_different_len=False,
        )

        # Проверяем порядок без флага - как в файле
        rearr_factors = [
            'market_cpa_offers_incut_count=10',
        ]
        response = self.report.request_bs_pb(request.format(';'.join(rearr_factors)))
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "cpaItems": [
                            {"modelId": "4057008"},
                            {"modelId": "4057009"},
                            {"modelId": "4057012"},
                            {"modelId": "4057013"},
                            {"modelId": "4057014"},
                            {"modelId": "4057011"},
                            {"modelId": "4057007"},
                            {"modelId": "4057010"},
                        ]
                    }
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_cpc_offers(cls):
        cls.index.hypertree += [HyperCategory(hid=4057020, name='Вентиляторы', output_type=HyperCategoryType.GURU)]

        for seq in range(1, 16):
            cls.index.models += [
                Model(hyperid=4057020 + seq, title='Вентилятор {}'.format(20 + seq), hid=4057020),
            ]

            cls.index.offers += [
                Offer(hyperid=4057020 + seq, title='Вентилятор {}'.format(20 + seq), ts=4057020 + seq, hid=4057020),
            ]

            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 4057020 + seq).respond(0.9 - 0.01 * seq)

        cls.index.parallel_cpc_query_models += [
            QueryIntList(
                query='вентилятор',
                integer_ids=[
                    4057028,
                    4057029,
                    4057032,
                    4057033,
                    4057034,
                    4057031,
                    4057027,
                    4057030,
                ],
            )
        ]

    def test_cpc_query_models(self):
        """Ограничиваем список моделей для CPC-врезки
        https://st.yandex-team.ru/MARKETOUT-40545
        """
        rearr_factors = [
            'market_cpa_offers_incut_count=10',
            'market_parallel_use_fixed_queries_for_cpc=0',
        ]
        request = 'place=parallel&text=вентилятор&rearr-factors={}'

        # Проверяем порядок под флагом
        response = self.report.request_bs_pb(request.format(';'.join(rearr_factors)))
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {"modelId": "4057021"},
                            {"modelId": "4057022"},
                            {"modelId": "4057023"},
                            {"modelId": "4057024"},
                            {"modelId": "4057025"},
                            {"modelId": "4057026"},
                            {"modelId": "4057027"},
                            {"modelId": "4057028"},
                            {"modelId": "4057029"},
                        ]
                    }
                }
            },
            allow_different_len=False,
        )

        # Проверяем порядок без флага - модели из файла, но сортировка по релевантности
        rearr_factors = [
            'market_cpa_offers_incut_count=10',
        ]
        response = self.report.request_bs_pb(request.format(';'.join(rearr_factors)))
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {"modelId": "4057027"},
                            {"modelId": "4057028"},
                            {"modelId": "4057029"},
                            {"modelId": "4057030"},
                            {"modelId": "4057031"},
                            {"modelId": "4057032"},
                        ]
                    }
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_fixed_models_for_top_queries(self):
        """Проверка использование фиксированных списков моделей
        https://st.yandex-team.ru/MARKETOUT-40545
        """
        # use flags market_parallel_use_fixed_queries_for_cpc + market_parallel_use_fixed_queries
        rearr = ['market_parallel_use_fixed_queries=1', 'market_cpa_offers_incut_count=8']
        response = self.report.request_bs('place=parallel&text=телефон&rearr-factors={}'.format(';'.join(rearr)))
        response_with_fixed_models = {
            "market_offers_wizard": [
                {
                    "showcase": {
                        "cpaItems": [
                            {"modelId": "4057008"},
                            {"modelId": "4057009"},
                            {"modelId": "4057012"},
                            {"modelId": "4057013"},
                            {"modelId": "4057014"},
                            {"modelId": "4057011"},
                            {"modelId": "4057007"},
                            {"modelId": "4057010"},
                        ]
                    }
                }
            ]
        }
        self.assertFragmentIn(response, response_with_fixed_models, preserve_order=True, allow_different_len=False)

        response = self.report.request_bs('place=parallel&text=купить+телефон&rearr-factors={}'.format(';'.join(rearr)))
        self.assertFragmentIn(response, response_with_fixed_models, preserve_order=True, allow_different_len=False)

        response = self.report.request_bs(
            'place=parallel&text=купить+телефоны&rearr-factors={}'.format(';'.join(rearr))
        )
        self.assertFragmentIn(response, response_with_fixed_models, preserve_order=True, allow_different_len=False)


if __name__ == '__main__':
    main()
