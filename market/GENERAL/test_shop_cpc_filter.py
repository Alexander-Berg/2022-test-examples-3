#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import DynamicShop, MnPlace, Offer, Shop
from core.matcher import Contains, NotEmpty


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ["market_money_disable_bids=0"]
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

    # MARKETOUT-12600
    @classmethod
    def prepare_shop_cpc_filter(cls):
        cls.index.shops += [
            Shop(fesh=1260001, cpc=Shop.CPC_REAL),
            Shop(fesh=1260002, cpc=Shop.CPC_SANDBOX),
            Shop(fesh=1260003, cpc=Shop.CPC_NO),
            Shop(fesh=1260004, cpc=Shop.CPC_REAL),
            Shop(fesh=1260005, cpc=Shop.CPC_SANDBOX),
            Shop(fesh=1260006, cpc=Shop.CPC_NO),
        ]

    def test_shop_cpc_filter_real_no_filter(self):
        """
        Делаем запросы к place=shop_info, проверяем выдачу:

        магазин не в фильтре и его CPC=REAL
        проверяем (через place=shop_info), что Репорт эти настройки подхватил

        Описание CPC-программы см. в MARKETOUT-12495
        """
        self.dynamic.market_dynamic.disabled_cpc_shops += [
            DynamicShop(1260004),
            DynamicShop(1260005),
            DynamicShop(1260006),
        ]

        response = self.report.request_json('place=shop_info&fesh=1260001')
        self.assertFragmentIn(
            response,
            {
                'entity': 'shop',
                'id': 1260001,
                'cpc': {
                    'shopsDat': 'real',
                    'isInShopCpcFilterDb': False,
                    'calculated': 'real',
                },
            },
        )

    def test_shop_cpc_filter_sbx_no_filter(self):
        """
        Делаем запросы к place=shop_info, проверяем выдачу:

        магазин не в фильтре и его CPC=SBX
        проверяем (через place=shop_info), что Репорт эти настройки подхватил

        Описание CPC-программы см. в MARKETOUT-12495
        """
        response = self.report.request_json('place=shop_info&fesh=1260002')
        self.assertFragmentIn(
            response,
            {
                'entity': 'shop',
                'id': 1260002,
                'cpc': {
                    'shopsDat': 'sandbox',
                    'isInShopCpcFilterDb': False,
                    'calculated': 'sandbox',
                },
            },
        )

    def test_shop_cpc_filter_no_no_filter(self):
        """
        Делаем запросы к place=shop_info, проверяем выдачу:

        магазин не в фильтре и его CPC=NO
        проверяем (через place=shop_info), что Репорт эти настройки подхватил

        Описание CPC-программы см. в MARKETOUT-12495
        """
        response = self.report.request_json('place=shop_info&fesh=1260003')
        self.assertFragmentIn(
            response,
            {
                'entity': 'shop',
                'id': 1260003,
                'cpc': {
                    'shopsDat': 'no',
                    'isInShopCpcFilterDb': False,
                    'calculated': 'no',
                },
            },
        )

    def test_shop_cpc_filter_real_filtered(self):
        """
        Делаем запросы к place=shop_info, проверяем выдачу:

        магазин в фильтре и его CPC=REAL
        проверяем (через place=shop_info), что Репорт эти настройки подхватил

        Описание CPC-программы см. в MARKETOUT-12495
        """
        self.dynamic.market_dynamic.disabled_cpc_shops += [
            DynamicShop(1260004),
            DynamicShop(1260005),
            DynamicShop(1260006),
        ]

        response = self.report.request_json('place=shop_info&fesh=1260004')
        self.assertFragmentIn(
            response,
            {
                'entity': 'shop',
                'id': 1260004,
                'cpc': {
                    'shopsDat': 'real',
                    'isInShopCpcFilterDb': True,
                    'calculated': 'no',
                },
            },
        )

    def test_shop_cpc_filter_sbx_filtered(self):
        """
        Делаем запросы к place=shop_info, проверяем выдачу:

        магазин в фильтре и его CPC=SBX
        проверяем (через place=shop_info), что Репорт эти настройки подхватил

        Описание CPC-программы см. в MARKETOUT-12495
        """
        self.dynamic.market_dynamic.disabled_cpc_shops += [
            DynamicShop(1260004),
            DynamicShop(1260005),
            DynamicShop(1260006),
        ]

        response = self.report.request_json('place=shop_info&fesh=1260005')
        self.assertFragmentIn(
            response,
            {
                'entity': 'shop',
                'id': 1260005,
                'cpc': {
                    'shopsDat': 'sandbox',
                    'isInShopCpcFilterDb': True,
                    'calculated': 'no',
                },
            },
        )

    def test_shop_cpc_filter_no_filtered(self):
        """
        Делаем запросы к place=shop_info, проверяем выдачу:

        магазин в фильтре и его CPC=NO
        проверяем (через place=shop_info), что Репорт эти настройки подхватил

        Описание CPC-программы см. в MARKETOUT-12495
        """
        self.dynamic.market_dynamic.disabled_cpc_shops += [
            DynamicShop(1260004),
            DynamicShop(1260005),
            DynamicShop(1260006),
        ]

        response = self.report.request_json('place=shop_info&fesh=1260006')
        self.assertFragmentIn(
            response,
            {
                'entity': 'shop',
                'id': 1260006,
                'cpc': {
                    'shopsDat': 'no',
                    'isInShopCpcFilterDb': True,
                    'calculated': 'no',
                },
            },
        )

    @classmethod
    def prepare_cpc_quality_shop_filter(cls):
        cls.index.shops += [
            Shop(fesh=201, priority_region=213, cpa=Shop.CPA_REAL),
            Shop(fesh=202, priority_region=213, cpa=Shop.CPA_REAL),
        ]

        cls.index.offers += [
            Offer(title='additional 201', fesh=201),
            Offer(title="additional goodcpa 201", fesh=201, cpa=Offer.CPA_REAL),
            Offer(title='additional 202', fesh=202),
            Offer(title="additional badcpa 202", fesh=202, cpa=Offer.CPA_REAL),
        ]

    def test_cpc_quality_shop_filter_baseline(self):
        """
        Проверяем, что магазины выключенные через список CPC-магазинов с плохим качеством,
        когда эксперимент выключен, показываются. Проверяем, что показываются cpa-офферы
        cpa-ссылки на белом отключены
        """
        response = self.report.request_json('place=prime&text=additional 201&rids=213&pp=18&show-urls=cpa,external')
        self.assertFragmentIn(response, {'titles': {'raw': 'additional 201'}, 'urls': {'encrypted': NotEmpty()}})
        self.assertFragmentIn(
            response, {'titles': {'raw': 'additional goodcpa 201'}, 'urls': {'encrypted': NotEmpty()}}
        )

    @classmethod
    def prepare_cpc_quality_shop_filter_combine_with_shop_cpc_filter(cls):
        cls.index.shops += [
            Shop(fesh=1270004, priority_region=213, cpc=Shop.CPC_REAL),
        ]

        cls.index.offers += [
            Offer(title='additional 1270004', fesh=1270004),
        ]

    def test_cpc_quality_shop_filter_combine_with_cpa_shop_quality_filter1(self):
        """
        Магазин в cpc-фильтре и его CPC=REAL. Кроме того устанавливаем cpc-фильтр по качеству магазина.
        Проверяем (через place=shop_info), что Репорт эти настройки подхватил.
        """
        self.dynamic.market_dynamic.disabled_cpc_shops += [DynamicShop(1270004)]

        response = self.report.request_json('place=shop_info&fesh=1270004')
        self.assertFragmentIn(
            response,
            {
                'entity': 'shop',
                'id': 1270004,
                'cpc': {
                    'shopsDat': 'real',
                    'isInShopCpcFilterDb': True,
                    'calculated': 'no',
                },
            },
        )

    @classmethod
    def prepare_shop_cutoff_reasons(cls):
        cls.index.shops += [
            Shop(fesh=270005, priority_region=213, cpc=Shop.CPC_REAL),
            Shop(fesh=270006, priority_region=213, cpc=Shop.CPC_REAL),
            Shop(fesh=270015, priority_region=213, cpc=Shop.CPC_REAL),
        ]

        cls.index.offers += [
            Offer(fesh=270005, price=1000, hyperid=50000, ts=10),
            Offer(fesh=270006, price=900, hyperid=50000, ts=11),
            Offer(fesh=270015, price=800, hyperid=50000, ts=12),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 10).respond(0.02)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 11).respond(0.01)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 12).respond(0.005)

    def test_shop_cutoff_reasons(self):
        """
        Проверяем, репорт нормально работает с причинами скрытия в shop-cpc-filter.db
        магазины в правильном порядке попадают в выдачу прайма
        """

        response = self.report.request_json(
            'place=prime&hyperid=50000&rearr-factors=market_do_not_cutoff_shops_with_no_more_money=1;market_write_click_price_to_fuid=1'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'offer',
                        'shop': {'id': 270005},
                    },
                    {
                        'entity': 'offer',
                        'shop': {'id': 270006},
                    },
                    {
                        'entity': 'offer',
                        'shop': {'id': 270015},
                    },
                ]
            },
            preserve_order=True,
        )

        self.dynamic.market_dynamic.disabled_cpc_shops += [
            DynamicShop(270005, [3]),
            DynamicShop(270006, [4, 5, 6]),
            DynamicShop(270015, [44]),
        ]

        response = self.report.request_json(
            'place=prime&hyperid=50000&rearr-factors=market_do_not_cutoff_shops_with_no_more_money=1;market_write_click_price_to_fuid=1'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'offer',
                        'shop': {'id': 270005},
                    }
                ]
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                "results": [
                    {
                        'entity': 'offer',
                        'shop': {'id': 270006},
                    },
                    {
                        'entity': 'offer',
                        'shop': {'id': 270015},
                    },
                ]
            },
        )

        response = self.report.request_json(
            'place=prime&hyperid=50000&rearr-factors=market_do_not_cutoff_shops_ex_cpc_partners=1;market_write_click_price_to_fuid=1'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'offer',
                        'shop': {'id': 270015},
                    }
                ]
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                "results": [
                    {
                        'entity': 'offer',
                        'shop': {'id': 270006},
                    },
                    {
                        'entity': 'offer',
                        'shop': {'id': 270015},
                    },
                ]
            },
        )

    @classmethod
    def prepare_do_not_cutoff_shop_with_no_more_money_place_prime(cls):
        cls.index.shops += [
            Shop(fesh=270007, priority_region=213, cpc=Shop.CPC_REAL),
            Shop(fesh=270008, priority_region=213, cpc=Shop.CPC_REAL),
        ]

        cls.index.offers += [
            Offer(title='finger', fesh=270007, hyperid=50001, price=1000),
            Offer(title='ear', fesh=270008, hyperid=50001, price=1000),
        ]

    def test_do_not_cutoff_shop_with_no_more_money_place_prime(self):
        """
        Проверяем, репорт нормально работает с причинами скрытия в shop-cpc-filter.db
        """
        self.dynamic.market_dynamic.disabled_cpc_shops += [
            DynamicShop(270007, [3]),
            DynamicShop(270008, [4, 5, 6]),
        ]

        response = self.report.request_json(
            'place=prime&hyperid=50001&rearr-factors=market_do_not_cutoff_shops_with_no_more_money=1'
        )
        self.assertFragmentIn(response, {'entity': 'offer', "titles": {"raw": "finger"}})

        """
        Проверяем, что с реарр флагом не найдется тот оффер у которого причина скрытия не 3
        """
        self.assertFragmentNotIn(response, {'entity': 'offer', "titles": {"raw": "ear"}})

        """
        Проверяем, что без реарр флага оффер не найдется даже с правильной причиной скрытия
        """
        response = self.report.request_json('place=prime&hyperid=50001')
        self.assertFragmentNotIn(response, {'entity': 'offer', "titles": {"raw": "finger"}})

    @classmethod
    def prepare_do_not_cutoff_shop_with_no_more_money_auction_in_action(cls):
        cls.index.shops += [
            Shop(fesh=270009, priority_region=213, cpc=Shop.CPC_REAL),
            Shop(fesh=270010, priority_region=213, cpc=Shop.CPC_REAL),
            Shop(fesh=270011, priority_region=213, cpc=Shop.CPC_REAL),
        ]

        cls.index.offers += [
            Offer(title='finger', fesh=270009, hyperid=50002, price=1000, bid=1000, ts=1),
            Offer(title='horse head', fesh=270010, hyperid=50002, price=1000, bid=100, ts=2),
            Offer(title='ear', fesh=270011, hyperid=50002, price=1000, bid=10, ts=3),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1).respond(0.01)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2).respond(0.02)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3).respond(0.03)

    def test_do_not_cutoff_shop_with_no_more_money_auction_in_action(self):
        """
        Проверяем, что репорт нормально работает с причинами скрытия в shop-cpc-filter.db
        """

        """
        Вигрывает магазин с большей ствкой
        """
        request_string = 'place=productoffers&hyperid=50002&offers-set=defaultList&debug=1&pp=106&show-urls=external&rearr-factors=market_do_not_cutoff_shops_with_no_more_money=1;market_premium_offer_logic=add-and-mark-touch'  # noqa
        response = self.report.request_json(request_string)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        'entity': 'offer',
                        'shop': {'id': 270009},
                        'isPremium': True,
                        'titles': {'raw': 'finger'},
                        'benefit': {'type': 'premium'},
                    }
                ]
            },
            preserve_order=True,
        )

        self.dynamic.market_dynamic.disabled_cpc_shops += [
            DynamicShop(270009, [78, 90]),
        ]

        """
        Вигрывает магазин с средней ствкой, так как в причинах выключения магазина с наибольшей ставкой нет 3
        """
        response = self.report.request_json(request_string)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        'entity': 'offer',
                        'shop': {'id': 270010},
                        'isPremium': True,
                        'titles': {'raw': 'horse head'},
                        'benefit': {'type': 'premium'},
                    }
                ]
            },
            preserve_order=True,
        )

        self.dynamic.market_dynamic.disabled_cpc_shops += [
            DynamicShop(270010, [78, 90, 3]),
        ]

        """
        Все равно, вигрывает магазин с средней ствкой, так как в причинах его выключения есть 3
        """
        response = self.report.request_json(request_string)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        'entity': 'offer',
                        'shop': {'id': 270010},
                        'isPremium': True,
                        'titles': {'raw': 'horse head'},
                        'benefit': {'type': 'premium'},
                    }
                ]
            },
            preserve_order=True,
        )

    @classmethod
    def prepare_do_not_cutoff_shop_with_no_more_money_check_logs(cls):
        cls.index.shops += [
            Shop(fesh=270012, priority_region=213, cpc=Shop.CPC_REAL),
            Shop(fesh=270013, priority_region=213, cpc=Shop.CPC_REAL),
            Shop(fesh=270014, priority_region=213, cpc=Shop.CPC_REAL),
        ]

        cls.index.offers += [
            Offer(title='finger', fesh=270012, hyperid=50003, price=1000, bid=1000, ts=4),
            Offer(title='horse head', fesh=270014, hyperid=50004, price=1000, bid=100),
            Offer(title='ear', fesh=270013, hyperid=50003, price=1000, bid=10, ts=5),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 4).respond(0.01)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 5).respond(0.02)

    def test_do_not_cutoff_shop_with_no_more_money_check_logs(self):
        """
        Проверяем, что в логах clickPrice == 0 а цена клика пишется в fuid если магазин скрыт с причиной 3, и нормальный клик прайс если он не скрыт
        """

        self.dynamic.market_dynamic.disabled_cpc_shops += [
            DynamicShop(270012, [78, 90, 3]),
        ]

        request_string = 'place=productoffers&hyperid=50003&offers-set=defaultList&debug=1&pp=106&show-urls=external&rearr-factors=market_do_not_cutoff_shops_with_no_more_money=1;market_write_click_price_to_fuid=1;market_premium_offer_logic=add-and-mark-touch'  # noqa

        """
        Проверяем, что в логах clickPrice == 0 а цена клика пишется в fuid
        """
        _ = self.report.request_json(request_string)

        self.show_log_tskv.expect(bid=1000, click_price=0, fuid=Contains('cp=21'))

        """
        Проверяем, что в логах clickPrice == 0, а цена клика пишется в fuid, но теперь в прайме, а так же, вообще, что не выключенный магазин наодится под флагом и кликпрайс не нулевой
        """
        _ = self.report.request_json(
            'place=prime&hyperid=50004&rearr-factors=market_do_not_cutoff_shops_with_no_more_money=1;market_write_click_price_to_fuid=1;market_premium_offer_logic=add-and-mark-touch'
        )

        self.show_log_tskv.expect(bid=100, click_price=2, fuid=Contains('cp=2'))

    @classmethod
    def prepare_do_not_cutoff_shops_ex_cpc_partners_place_prime(cls):
        cls.index.shops += [
            Shop(fesh=270016, priority_region=213, cpc=Shop.CPC_REAL),
            Shop(fesh=270017, priority_region=213, cpc=Shop.CPC_REAL),
        ]

        cls.index.offers += [
            Offer(title='nose', fesh=270016, hyperid=50005, price=1000),
            Offer(title='eye', fesh=270017, hyperid=50005, price=1000),
        ]

    def test_do_not_cutoff_shop_ex_cpc_partners(self):
        """
        Проверяем, репорт нормально работает с причинами скрытия в shop-cpc-filter.db
        """
        self.dynamic.market_dynamic.disabled_cpc_shops += [
            DynamicShop(270016, [44]),
            DynamicShop(270017, [4, 5, 6]),
        ]

        response = self.report.request_json(
            'place=prime&hyperid=50005&rearr-factors=market_do_not_cutoff_shops_ex_cpc_partners=1'
        )
        self.assertFragmentIn(response, {'entity': 'offer', "titles": {"raw": "nose"}})

        """
        Проверяем, что с реарр флагом не найдется тот оффер у которого причина скрытия не 44
        """
        self.assertFragmentNotIn(response, {'entity': 'offer', "titles": {"raw": "eye"}})

        """
        Проверяем, что без реарр флага оффер не найдется даже с правильной причиной скрытия
        """
        response = self.report.request_json('place=prime&hyperid=50005')
        self.assertFragmentNotIn(response, {'entity': 'offer', "titles": {"raw": "nose"}})

    @classmethod
    def prepare_cutoff_double_disabled(cls):
        cls.index.shops += [
            Shop(fesh=280016, priority_region=213, cpc=Shop.CPC_REAL),
            Shop(fesh=280017, priority_region=213, cpc=Shop.CPC_REAL),
            Shop(fesh=280018, priority_region=213, cpc=Shop.CPC_REAL),
        ]

        cls.index.offers += [
            Offer(title='nose', fesh=280016, hyperid=50007, price=1000),
            Offer(title='eye', fesh=280017, hyperid=50007, price=1000),
            Offer(title='banana', fesh=280018, hyperid=50007, price=1000),
        ]

    def test_cutoff_double_disabled(self):
        """
        Проверяем, репорт нормально работает с причинами скрытия в shop-cpc-filter.db
        """
        self.dynamic.market_dynamic.disabled_cpc_shops += [
            DynamicShop(280016, [44]),
            DynamicShop(280017, [4, 5, 6]),
            DynamicShop(280018, [44]),
        ]

        self.dynamic.market_dynamic.disabled_cpa_shops += [
            DynamicShop(280018),
        ]

        response = self.report.request_json(
            'place=prime&hyperid=50007&rearr-factors=market_do_not_cutoff_shops_ex_cpc_partners=1;market_cutoff_cpa_and_cpc_disabled_shops=1'
        )
        self.assertFragmentIn(response, {'entity': 'offer', "titles": {"raw": "nose"}})

        """
        Проверяем, что с реарр флагом не найдется тот оффер у которого причина скрытия не 44
        """
        self.assertFragmentNotIn(response, {'entity': 'offer', "titles": {"raw": "eye"}})

        """
        Проверяем, что оффер скрытый и в cpc и в cpa не найдется даже с реарр флагом
        """
        self.assertFragmentNotIn(response, {'entity': 'offer', "titles": {"raw": "banana"}})


if __name__ == '__main__':
    main()
