#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BlueOffer,
    CategoryRestriction,
    Currency,
    DeliveryBucket,
    HyperCategory,
    MarketSku,
    MnPlace,
    Model,
    Offer,
    Outlet,
    OutletDeliveryOption,
    OutletLicense,
    OverallModel,
    PickupBucket,
    PickupOption,
    RegionalModel,
    RegionalRestriction,
    Shop,
    Tax,
    Vat,
)
from core.testcase import TestCase, main
from core.matcher import ElementCount, NoKey
from core.types.hypercategory import (
    ADULT_CATEG_ID,
    TOBACCO_CATEG_ID,
    ALCOHOL_VINE_CATEG_ID,
    SELF_DEFENSE_WEAPONS_CATEG_ID,
)


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.shops += [
            Shop(fesh=100, priority_region=213),
            Shop(
                fesh=500,
                datafeed_id=500,
                priority_region=213,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
                warehouse_id=145,
            ),
            Shop(
                fesh=10774,
                datafeed_id=1000,
                priority_region=213,
                name='virtual_shop',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                fulfillment_virtual=True,
                cpa=Shop.CPA_REAL,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
            ),
        ]

        cls.index.hypertree += [
            HyperCategory(hid=ADULT_CATEG_ID),
            HyperCategory(hid=TOBACCO_CATEG_ID),
            HyperCategory(hid=SELF_DEFENSE_WEAPONS_CATEG_ID),
            HyperCategory(hid=ALCOHOL_VINE_CATEG_ID),
        ]

        cls.index.category_restrictions += [
            CategoryRestriction(
                name='ask_18',
                hids=[ADULT_CATEG_ID, TOBACCO_CATEG_ID, ALCOHOL_VINE_CATEG_ID, SELF_DEFENSE_WEAPONS_CATEG_ID],
                regional_restrictions=[RegionalRestriction(rids_with_subtree=[213])],
            )
        ]

        cls.index.offers += [
            Offer(fesh=100, hid=ADULT_CATEG_ID, hyperid=100, title="Sasha Grey", adult=1),
            Offer(fesh=100, hid=ADULT_CATEG_ID, hyperid=101, title="Masha Pray", adult=1),
            Offer(fesh=100, hid=TOBACCO_CATEG_ID, hyperid=102, title="Parlament Tobacco", adult=1),
            Offer(fesh=100, hid=SELF_DEFENSE_WEAPONS_CATEG_ID, hyperid=103, title="Elektroshoker", adult=1),
        ]

        for _ in range(11):
            cls.index.offers += [
                Offer(fesh=100, title="Pasha Grey"),
            ]

        for _ in range(9):
            cls.index.offers += [
                Offer(fesh=100, title="Pasha Pray"),
            ]

        for _ in range(7):
            cls.index.offers += [
                Offer(fesh=100, title="parlament water"),
            ]

        cls.index.outlets += [
            Outlet(
                point_id=10101,
                fesh=10774,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    day_from=3, day_to=5, order_before=6, work_in_holiday=False, price=500, price_to=1000
                ),
                licenses=[OutletLicense()],
                working_days=[i for i in range(7)],
            ),
        ]
        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=1,
                fesh=10774,
                carriers=[157],
                options=[PickupOption(outlet_id=10101, day_from=1, day_to=1, price=100)],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            )
        ]

        cls.index.mskus += [
            MarketSku(
                sku=1,
                hyperid=1,
                hid=ADULT_CATEG_ID,
                title="Intimator",
                blue_offers=[
                    BlueOffer(
                        price=20,
                        vat=Vat.VAT_18,
                        title="Intimator",
                        feedid=500,
                        adult=1,
                        waremd5='AdultBlueOffer_______g',
                        pickup_buckets=[1],
                    )
                ],
            ),
            MarketSku(
                sku=2,
                hyperid=2,
                hid=TOBACCO_CATEG_ID,
                title='Iqos',
                blue_offers=[
                    BlueOffer(
                        price=200,
                        vat=Vat.VAT_18,
                        title='Iqos',
                        feedid=500,
                        waremd5='TobaccoBlueOffer_____g',
                        pickup_buckets=[1],
                    )
                ],
            ),
            MarketSku(
                sku=3,
                hyperid=3,
                hid=ALCOHOL_VINE_CATEG_ID,
                alcohol=True,
                title='Booze',
                blue_offers=[
                    BlueOffer(
                        price=2000,
                        vat=Vat.VAT_18,
                        title='Booze',
                        feedid=500,
                        waremd5='AlcoholBlueOffer_____g',
                        pickup_buckets=[1],
                    )
                ],
            ),
        ]

    def test_force_hide_adult(self):
        """
        Проверяем выполненение перезапроса из-за маленького числа взрослых офферов для их скрытия
        12 офферов, 1 из них взрослый
        """

        # перезапрос не происходит, поскольку взрослых товаров больше порога
        request = "place=prime&text=grey&rids=213&adult=1" + "&rearr-factors=market_force_hide_adult_threshold=0"
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"search": {"total": 12, "adult": True}})

        # перезапрос, поскольку взрослых товаров меньше порога
        request = "place=prime&text=grey&rids=213&adult=1" + "&rearr-factors=market_force_hide_adult_threshold=0.2"
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"search": {"total": 11, "adult": False}})

    def test_not_force_hide_adult(self):
        """
        Проверяем, что на безтексте перезапрос не происходит
        """
        # перезапрос не происходит, поскольку безтекст
        request = "place=prime&fesh=100&adult=1" + "&rearr-factors=market_force_hide_adult_threshold=1"
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"search": {"total": 32, "adult": True}})

    def test_adult_is_not_shown_by_default(self):
        for add in ('', '&adult=0'):
            for title in ('sasha', 'tobacco'):
                response = self.report.request_json('place=prime&text={}&rids=213&{}'.format(title, add))
                self.assertFragmentIn(response, {"search": {"total": 0, "adult": True, "restrictionAge18": True}})

            response = self.report.request_json('place=prime&text=elektroshoker&rids=213&{}'.format(add))
            self.assertFragmentIn(response, {"search": {"total": 0, "adult": True, "restrictionAge18": True}})

            for title in ('intimator', 'iqos', 'booze'):
                for color in ('blue', 'white'):
                    response = self.report.request_json(
                        'place=prime&rgb={}&allow-collapsing=0&text={}&rids=213&{}'.format(color, title, add)
                    )
                    self.assertFragmentIn(
                        response,
                        {"search": {"total": 0, "adult": True, "restrictionAge18": True, "results": ElementCount(0)}},
                    )

    def test_adult_warning_prime(self):
        for add in ('', '&adult=0'):
            response = self.report.request_json('place=prime&text=grey&rids=213&{}'.format(add))
            self.assertFragmentIn(
                response,
                {
                    "total": 11,
                    "adult": True,  # https://st.yandex-team.ru/MARKETOUT-26262 Показываем баннер при любом положительном количестве взрослых офферов
                    "restrictionAge18": True,
                },
            )

            for title, count in (('pray', 9), ('parlament', 7)):
                response = self.report.request_json('place=prime&text={}&rids=213&{}'.format(title, add))
                self.assertFragmentIn(response, {"total": count, "adult": True, "restrictionAge18": True})

    def test_adult_is_shown_if_requested(self):
        for title in ('sasha', 'tobacco'):
            response = self.report.request_json('place=prime&text={}&rids=213&adult=1'.format(title))
            self.assertFragmentIn(
                response,
                {
                    "total": 1,
                    "adult": True,
                    "restrictionAge18": True,
                    "results": [
                        {
                            "entity": "offer",
                            "titles": {"raw": "Sasha Grey" if title == 'sasha' else "Parlament Tobacco"},
                            "isAdult": True,
                            "restrictedAge18": True,
                        }
                    ],
                },
            )

        response = self.report.request_json('place=prime&text=elektroshoker&rids=213&adult=1')
        self.assertFragmentIn(
            response,
            {
                "total": 1,
                "adult": True,
                "restrictionAge18": True,
                "results": [
                    {"entity": "offer", "titles": {"raw": "Elektroshoker"}, "isAdult": True, "restrictedAge18": True}
                ],
            },
        )

        for title in ('intimator', 'iqos', 'booze'):
            response = self.report.request_json(
                'place=prime&rgb=blue&allow-collapsing=0&text={}&rids=213&adult=1'.format(title)
            )
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "total": 1,
                        "adult": True,
                        "restrictionAge18": True,
                        "results": [{"entity": "offer", "isAdult": True, "restrictedAge18": True}],
                    }
                },
            )

    def test_adult_for_alice(self):
        """Для алисы не показываем взрослых офферов и не показываем adult=True и restrictionAge18=True"""

        for alice in ['&alice=1', '&clid=850']:
            for title in ('sasha', 'tobacco'):
                response = self.report.request_json('place=prime&text={}&rids=213'.format(title) + alice)
                self.assertFragmentIn(response, {"search": {"total": 0, "adult": False, "restrictionAge18": False}})

            for title in ('intimator', 'iqos', 'booze'):
                response = self.report.request_json(
                    'place=prime&rgb=blue&allow-collapsing=0&text={}&rids=213'.format(title) + alice
                )
                self.assertFragmentIn(
                    response,
                    {"search": {"total": 0, "adult": False, "restrictionAge18": False, "results": ElementCount(0)}},
                )

    def test_adult_is_shown_bid_recommendation(self):
        response = self.report.request_xml('place=bids_recommender&hyperid=100&fesh=100&rids=213&adult=1')
        self.assertFragmentIn(
            response,
            '''
        <search_results>
            <offers>
                <offer>
                    <raw-title>Sasha Grey</raw-title>
                </offer>
            </offers>
        </search_results>
        ''',
        )

    @classmethod
    def prepare_adult_warning_output(cls):
        """
        Создаем магазины и несколько наборов офферов.
        Офферы с номерами 0-6 не adult, с номерами 7-14- adult и далее
        с 15-го до 30-го все нечентные - не adult, все четные - adult
        """

        cls.index.hypertree += [
            HyperCategory(hid=10),
        ]

        for seq in range(35):
            cls.index.shops += [Shop(fesh=200 + seq, priority_region=213)]

        for seq in range(7):
            cls.index.offers += [
                Offer(title='normal movie %s' % seq, fesh=200 + seq, price=1000 + 50 * seq, hyperid=200, hid=10)
            ]

        for seq in range(7, 15):
            cls.index.offers += [
                Offer(
                    title='porn movie %s' % seq,
                    fesh=200 + seq,
                    price=1000 + 50 * seq,
                    hyperid=201,
                    adult=1,
                    hid=ADULT_CATEG_ID,
                )
            ]

        for seq in range(15, 30, 2):
            porn_seq = seq + 1
            cls.index.offers += [
                Offer(title='normal movie %s' % seq, fesh=200 + seq, price=1000 + 50 * seq, hyperid=200, hid=10),
                Offer(
                    title='porn movie %s' % porn_seq,
                    fesh=200 + porn_seq,
                    price=1000 + 50 * (porn_seq),
                    hyperid=201,
                    adult=1,
                    hid=ADULT_CATEG_ID,
                ),
            ]

    @classmethod
    def prepare_adult_models(cls):
        """
        Создаем adult-модели с флагом adult, и adult-модель в adult-категории,
        а также не adult-модели
        """
        cls.index.models += [
            Model(hyperid=501, ts=501, title='Just adults limited 1', hid=50),
            Model(hyperid=502, ts=502, title='Just adults limited 2', hid=51),
            Model(hyperid=503, ts=503, title='Just adults limited 3', hid=ADULT_CATEG_ID),
            Model(hyperid=504, ts=504, title='Just adults limited 4', hid=50),
            Model(hyperid=505, ts=505, title='Just adults limited 5', hid=50),
            Model(hyperid=506, ts=506, title='Just adults limited 6', hid=50),
            Model(hyperid=507, ts=507, title='Just adults limited 7', hid=50),
            Model(hyperid=508, ts=508, title='Just adults limited 8', hid=50),
            Model(hyperid=509, ts=509, title='Just adults limited 9', hid=50),
            Model(hyperid=510, ts=510, title='Just adults limited 10', hid=50),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 501).respond(0.60)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 502).respond(0.59)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 503).respond(0.58)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 504).respond(0.57)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 505).respond(0.56)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 506).respond(0.55)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 507).respond(0.54)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 508).respond(0.53)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 509).respond(0.52)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 510).respond(0.51)

        cls.index.overall_models += [
            OverallModel(hyperid=501, price_med=200.5, is_adult=True),
            OverallModel(hyperid=502, price_med=100, is_adult=False),
            OverallModel(hyperid=509, price_med=50, is_adult=True),
        ]

        cls.index.regional_models += [
            RegionalModel(hyperid=501, offers=100, price_min=1000, price_max=1000, rids=[213]),
            RegionalModel(hyperid=502, offers=100, price_min=2000, price_max=2000, rids=[213]),
            RegionalModel(hyperid=503, offers=100, price_min=100, price_max=3000, rids=[213]),
            RegionalModel(hyperid=504, offers=100, price_min=4200, price_max=4000, rids=[213]),
            RegionalModel(hyperid=505, offers=100, price_min=4300, price_max=5000, rids=[213]),
            RegionalModel(hyperid=506, offers=100, price_min=4400, price_max=6000, rids=[213]),
            RegionalModel(hyperid=507, offers=100, price_min=4500, price_max=7000, rids=[213]),
            RegionalModel(hyperid=508, offers=100, price_min=4600, price_max=8000, rids=[213]),
            RegionalModel(hyperid=509, offers=100, price_min=4700, price_max=9000, rids=[213]),
            RegionalModel(hyperid=510, offers=100, price_min=500, price_max=500, rids=[213]),
        ]

        cls.index.shops += [
            Shop(fesh=700, priority_region=213),
        ]

        cls.index.offers += [Offer(title='o ' + str(i), hyperid=500 + i, fesh=700) for i in range(1, 11)]

    def test_adult_model_on_parallel(self):
        """Проверяем, что adult-модели фильтруются на ПП
        https://st.yandex-team.ru/MARKETOUT-27703
        https://st.yandex-team.ru/MARKETOUT-29385
        """
        response = self.report.request_bs_pb('place=parallel&text=limited')
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "showcase": {
                        "items": [
                            {"title": {"text": {"__hl": {"text": "Just adults limited 2", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "Just adults limited 4", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "Just adults limited 5", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "Just adults limited 6", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "Just adults limited 7", "raw": True}}}},
                            {"title": {"text": {"__hl": {"text": "Just adults limited 8", "raw": True}}}},
                        ]
                    }
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_adult_models_family(self):
        """
        Проверяем, что для family=1 и adult=0 или alice=1 нет adult-моделей
        """
        for query in [
            'place=prime&rids=213&text=limited&entities=product&allow-collapsing=0&how=dprice&family=1',
            'place=prime&rids=213&text=limited&entities=product&allow-collapsing=0&family=1',
            'place=prime&rids=213&text=limited&entities=product&allow-collapsing=0&how=dprice&adult=0',
            'place=prime&rids=213&text=limited&entities=product&allow-collapsing=0&adult=0',
            'place=prime&rids=213&text=limited&entities=product&allow-collapsing=0&alice=1',
        ]:
            response = self.report.request_json(query)
            self.assertFragmentIn(response, {"entity": "product", "id": 508})
            self.assertFragmentNotIn(response, {"entity": "product", "id": 509})
            self.assertFragmentNotIn(response, {"entity": "product", "id": 501})
            self.assertFragmentNotIn(response, {"entity": "product", "id": 503})

    def test_adult_models_for_adults(self):
        """
        Проверяем, что для взрослых показываются как adult так и не adult модели
        """
        response = self.report.request_json(
            'place=prime&rids=213&text=limited&entities=product&allow-collapsing=0&pp=18&adult=1'
        )
        self.assertFragmentIn(response, {"entity": "product", "id": 508})
        self.assertFragmentIn(response, {"entity": "product", "id": 509})
        self.assertFragmentIn(response, {"entity": "product", "id": 501})
        self.assertFragmentIn(response, {"entity": "product", "id": 503})

    def test_hide_adult_offers_flag(self):
        """
        Проверяем, что эксп.флаг market_hide_adult_offers скрывает adult-офферы независимо от значения adult
        """

        def check_has_adult_offer(request):
            for is_blue in (True, False):
                postfix = '&rgb=blue&allow-collapsing=0&text=intimator' if is_blue else '&text=sasha'
                self.assertFragmentIn(
                    self.report.request_json(request + postfix),
                    {
                        "total": 1,
                        "adult": True,
                        "restrictionAge18": True,
                        "results": [
                            {
                                "entity": "offer",
                                "titles": {"raw": "Intimator" if is_blue else "Sasha Grey"},
                                "isAdult": True,
                                "restrictedAge18": True,
                            }
                        ],
                    },
                )

        def check_no_adult_offers(request):
            for is_blue in (True, False):
                postfix = '&rgb=blue&text=intimator' if is_blue else '&text=sasha'
                self.assertFragmentIn(
                    self.report.request_json(request + postfix),
                    {"search": {"total": 0, "adult": True, "restrictionAge18": True}},
                )

        base_request = 'place=prime&rids=213'
        check_has_adult_offer(base_request + '&adult=1')
        check_no_adult_offers(base_request + '&adult=0')

    def test_adult_hids_in_aggregates(self):
        """Проверка того, что без указания adult=1 в агрегаты не попадает adult-категория,
        и её интенты не появляются на выдаче"""

        for flag in (
            '',
            '&rearr-factors=market_blue_prime_without_delivery=0',
            '&rearr-factors=market_blue_prime_without_delivery=1',
        ):
            request = 'place=prime&rids=213&rgb=blue&text=intimator' + flag
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response, {"search": {"total": 0, "totalOffers": 0, "adult": True, "restrictionAge18": True}}
            )
            self.assertFragmentNotIn(response, {"intents": [{"category": {"hid": ADULT_CATEG_ID}}]})

    @classmethod
    def prepare_adult_factor(cls):
        cls.index.offers += [
            Offer(fesh=100, title="Alenka", waremd5='NotAdultOffer________g'),
        ]

    def test_adult_factor(self):
        common = 'place=prime&debug=da&adult=1'
        # проверяем заполненность фактора IS_ADULT
        response = self.report.request_json(common + '&text=Intimator')
        self.assertFragmentIn(response, {'debug': {'wareId': 'AdultBlueOffer_______g', 'factors': {'IS_ADULT': "1"}}})

        response = self.report.request_json(common + '&text=Iqos')
        self.assertFragmentIn(response, {'debug': {'wareId': 'TobaccoBlueOffer_____g', 'factors': {'IS_ADULT': "1"}}})

        response = self.report.request_json(common + '&text=Booze')
        self.assertFragmentIn(response, {'debug': {'wareId': 'AlcoholBlueOffer_____g', 'factors': {'IS_ADULT': "1"}}})

        response = self.report.request_json(common + '&text=Alenka')
        self.assertFragmentIn(
            response, {'debug': {'wareId': 'NotAdultOffer________g', 'factors': {'IS_ADULT': NoKey('IS_ADULT')}}}
        )


if __name__ == '__main__':
    main()
