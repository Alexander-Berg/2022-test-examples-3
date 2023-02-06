#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BlueOffer,
    ClickType,
    Currency,
    DeliveryBucket,
    HyperCategory,
    IncutBlackListFb,
    MarketSku,
    MnPlace,
    Model,
    Offer,
    Outlet,
    OverallModel,
    PickupBucket,
    PickupOption,
    Picture,
    Region,
    RegionalModel,
    Shop,
    Tax,
    UrlType,
    VendorBanner,
    NavCategory,
)
from core.types.fashion_parameters import FashionCategory
from core.testcase import TestCase, main
from core.matcher import Contains, ElementCount, NotEmpty, Absent, EmptyList


def dict_to_rearr(rearr_flags_dict):
    return ';'.join([str(flag) + "=" + str(rearr_flags_dict[flag]) for flag in rearr_flags_dict])


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.navtree += [
            NavCategory(nid=1, hid=1, primary=True),
        ]
        cls.index.models += [
            Model(hid=1, hyperid=1, vendor_id=1),
            Model(hid=1, hyperid=2, vbid=20, vendor_id=1, datasource_id=1),
            Model(hid=1, hyperid=3, vbid=10, vendor_id=1, datasource_id=1),
            Model(hid=1, hyperid=11, vbid=25, vendor_id=2, datasource_id=2),
        ]

        cls.index.offers += [Offer(hyperid=3)]

        cls.index.models += [Model(hid=2, hyperid=110 + i, vbid=1, vendor_id=1, datasource_id=1) for i in range(0, 10)]

        # for test vendor_incut_relevance_formula_threshold
        cls.index.models += [
            Model(hid=5, hyperid=1125, vbid=100, vendor_id=7, ts=1, title="turbolazer 70 mm"),
            Model(hid=5, hyperid=1126, vbid=10, vendor_id=8, ts=2, title="turbolazer 120 mm"),
        ]

        cls.matrixnet.on_default_place(MnPlace.BASE_SEARCH).respond(0.02)

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2).respond(0.04)

    def test_incut(self):
        '''
        Проверяем, что выбрался правильный вендор - с большей суммой ставок
        '''
        response = self.report.request_json(
            'place=vendor_incut&hid=1&pp=8&rearr-factors=market_vendor_incut_min_size=1;market_vendor_incut_with_CPA_offers_only=0;market_vendor_incut_hide_undeliverable_models=0'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'product',
                        'vendor': {
                            'id': 1,
                        },
                    },
                    {
                        'entity': 'product',
                        'vendor': {
                            'id': 1,
                        },
                    },
                ]
            },
            allow_different_len=False,
        )

    def test_nid(self):
        '''
        Проверяем, что фильтрация по NID работает
        '''
        response = self.report.request_json(
            'place=vendor_incut&nid=1&pp=8&hid=1'
            '&rearr-factors=market_vendor_incut_min_size=1;market_vendor_incut_with_CPA_offers_only=0;market_vendor_incut_hide_undeliverable_models=0'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'product',
                        'vendor': {
                            'id': 1,
                        },
                    },
                    {
                        'entity': 'product',
                        'vendor': {
                            'id': 1,
                        },
                    },
                ]
            },
            allow_different_len=False,
        )

    def test_min_size(self):
        '''
        Проверяем, что задание минимального размера врезки работает
        '''
        response = self.report.request_json(
            'place=vendor_incut&hid=1&pp=8'
            '&rearr-factors=market_vendor_incut_min_size=4;market_vendor_incut_with_CPA_offers_only=0;market_vendor_incut_hide_undeliverable_models=0'
        )
        self.assertFragmentIn(response, {'results': []}, allow_different_len=False)

        response = self.report.request_json(
            'place=vendor_incut&hid=1&pp=8'
            '&rearr-factors=market_vendor_incut_min_size=1;market_vendor_incut_with_CPA_offers_only=0;market_vendor_incut_hide_undeliverable_models=0'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'product',
                        'vendor': {
                            'id': 1,
                        },
                    },
                    {
                        'entity': 'product',
                        'vendor': {
                            'id': 1,
                        },
                    },
                ]
            },
            allow_different_len=False,
        )

    def test_onstock(self):
        '''
        Проверяем, что &onstock=1 работает
        '''
        response = self.report.request_json(
            'place=vendor_incut&hid=1&onstock=1&rearr-factors=market_vendor_incut_min_size=1;market_vendor_incut_with_CPA_offers_only=0;market_vendor_incut_hide_undeliverable_models=0'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'product',
                        'vendor': {
                            'id': 1,
                        },
                    },
                ]
            },
            allow_different_len=False,
        )

    def test_use_default_offers(self):
        '''
        Проверяем, что можно получить ДО для моделей
        '''
        response = self.report.request_json(
            'place=vendor_incut&hid=1&use-default-offers=1&rearr-factors=market_vendor_incut_min_size=1;market_vendor_incut_with_CPA_offers_only=0;market_vendor_incut_hide_undeliverable_models=0'
        )
        self.assertFragmentIn(
            response,
            {
                'entity': 'product',
                'offers': {
                    'items': [
                        {
                            'entity': 'offer',
                        }
                    ]
                },
            },
        )

    def test_switching_off_vendor_incut(self):
        '''
        Проверяем, что можно полностью отключить набор врезки флагом market_force_disable_vendor_incut
        '''
        rearr_flags_dict = {
            "market_force_disable_vendor_incut": 1,  # Выставляем флаг, чтобы врезка не набралась
            "market_vendor_incut_min_size": 1,
            "market_vendor_incut_with_CPA_offers_only": 0,
            "market_vendor_incut_hide_undeliverable_models": 0,
        }
        rearr_flags_str = '&rearr-factors=' + ';'.join(
            [(str(flag) + "=" + str(rearr_flags_dict[flag])) for flag in rearr_flags_dict]
        )
        response = self.report.request_json('place=vendor_incut&hid=1&use-default-offers=1' + rearr_flags_str)
        self.assertFragmentIn(response, {"results": EmptyList()})

    def test_debug(self):
        '''
        Проверяем, что дебажную выдачу: как посчитался auction value
        '''
        response = self.report.request_json(
            'place=vendor_incut&hid=1&debug=1&rearr-factors=market_vendor_incut_min_size=1;market_vendor_incut_with_CPA_offers_only=0;market_vendor_incut_hide_undeliverable_models=0'
        )
        self.assertFragmentIn(
            response,
            {
                'logicTrace': [
                    Contains('Top group auction value: 30'),
                    Contains('Second group auction value: 25'),
                    Contains('Autobroker multplier: 0.833333'),
                ]
            },
        )

    def test_autobroker(self):
        '''
        Проверяем работу автоброкера:
        Цена клика каждой модели должна уменьшиться пропорционально auction value двух вендоров
        '''
        _ = self.report.request_json(
            'place=vendor_incut&hid=1&show-urls=productVendorBid&rearr-factors=market_vendor_incut_min_size=1;market_vendor_incut_with_CPA_offers_only=0;market_vendor_incut_hide_undeliverable_models=0'  # noqa
        )
        self.click_log.expect(clicktype=ClickType.EXTERNAL, dtype='modelcard', hyper_id=2, vc_bid=20, vendor_price=17)
        self.click_log.expect(clicktype=ClickType.EXTERNAL, dtype='modelcard', hyper_id=3, vc_bid=10, vendor_price=9)

    def test_zero_click_price(self):
        '''
        Проверяем работу флага market_vendor_incut_do_not_bill
        '''
        _ = self.report.request_json(
            'place=vendor_incut&hid=1&show-urls=productVendorBid'
            '&rearr-factors=market_vendor_incut_do_not_bill=1;market_write_click_price_to_fuid=1;market_vendor_incut_min_size=1;market_vendor_incut_with_CPA_offers_only=0;market_vendor_incut_hide_undeliverable_models=0'  # noqa
        )
        self.click_log.expect(
            clicktype=ClickType.EXTERNAL,
            dtype='modelcard',
            hyper_id=2,
            vc_bid=20,
            vendor_price=0,
            fuid=Contains('vcp=17'),
        )
        self.click_log.expect(
            clicktype=ClickType.EXTERNAL,
            dtype='modelcard',
            hyper_id=3,
            vc_bid=10,
            vendor_price=0,
            fuid=Contains('vcp=9'),
        )

    def test_incut_size_desktop(self):
        '''
        Проверяем, что размер врезки ограничен по умолчанию 8 моделям на десктопе
        '''
        response = self.report.request_json(
            'place=vendor_incut&hid=2&rearr-factors=market_vendor_incut_with_CPA_offers_only=0;market_vendor_incut_hide_undeliverable_models=0'
        )
        self.assertEqual(
            8,
            response.count(
                {
                    'entity': 'product',
                }
            ),
        )

    def test_incut_size_touch(self):
        '''
        Проверяем, что размер врезки ограничен по умолчанию 7 моделями на таче
        '''
        response = self.report.request_json(
            'place=vendor_incut&hid=2&touch=1&rearr-factors=market_vendor_incut_with_CPA_offers_only=0;market_vendor_incut_hide_undeliverable_models=0'
        )
        self.assertEqual(
            7,
            response.count(
                {
                    'entity': 'product',
                }
            ),
        )

    @classmethod
    def prepare_zero_bids_filtering(cls):
        '''
        Один вендор, который проходит критерий 'не меньше 4-х моделей' и два, которые не проходят:
        один, с четырмя моделями, но с одной ставкой == 0;
        второй с тремя моделями
        '''
        cls.index.models += [
            Model(hid=3, hyperid=1110, vbid=1, vendor_id=3, datasource_id=1, title="Incom Corporation XVing"),
            Model(hid=3, hyperid=1111, vbid=11, vendor_id=3, datasource_id=1, title="Incom Corporation T-16 skyhopper"),
            Model(
                hid=3, hyperid=1112, vbid=12, vendor_id=3, datasource_id=1, title="Incom Corporation T-47 airspeeder"
            ),
            Model(hid=3, hyperid=1113, vbid=21, vendor_id=3, datasource_id=1, title="Incom Corporation X4 Gunship"),
            Model(hid=3, hyperid=1114, vbid=0, vendor_id=4, datasource_id=1, title="MandalMotors Aka'jor-class"),
            Model(hid=3, hyperid=1115, vbid=110, vendor_id=4, datasource_id=1, title="MandalMotors Fang-class"),
            Model(
                hid=3,
                hyperid=1116,
                vbid=120,
                vendor_id=4,
                datasource_id=1,
                title="MandalMotors Buirk'alor-class speeder",
            ),
            Model(hid=3, hyperid=1117, vbid=210, vendor_id=4, datasource_id=1, title="MandalMotors Lancer-class"),
            Model(hid=4, hyperid=1118, vbid=10, vendor_id=5, datasource_id=1, title="MandalMotors Aka'jor-class"),
            Model(hid=4, hyperid=1119, vbid=110, vendor_id=5, datasource_id=1, title="MandalMotors Fang-class"),
            Model(
                hid=4,
                hyperid=1120,
                vbid=120,
                vendor_id=5,
                datasource_id=1,
                title="MandalMotors Buirk'alor-class speeder",
            ),
            Model(hid=4, hyperid=1121, vbid=299, vendor_id=5, datasource_id=1, title="MandalMotors Lancer-class"),
            Model(hid=3, hyperid=1122, vbid=110, vendor_id=6, datasource_id=1, title="CEC CR92a"),
            Model(hid=3, hyperid=1123, vbid=120, vendor_id=6, datasource_id=1, title="CEC YT-1300"),
            Model(hid=3, hyperid=1124, vbid=209, vendor_id=6, datasource_id=1, title="CEC CR90"),
        ]

    def test_zero_bids_filtering(self):
        '''
        Проверяем, что если даже сумма ставок у вендора больше, но количество моделей меньше заданного, то он не победит
        '''
        response = self.report.request_json(
            'place=vendor_incut&hid=3&touch=0&rearr-factors=market_vendor_incut_min_size=4;market_vendor_incut_size=7;market_vendor_incut_with_CPA_offers_only=0;market_vendor_incut_hide_undeliverable_models=0'  # noqa
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'product',
                        'vendor': {
                            'id': 3,
                        },
                    },
                ]
            },
            allow_different_len=True,
        )

    def test_zero_bids_pass_filtering(self):
        '''
        Проверяем, что если сумма ставок у вендора больше, а количество моделей больше или равно заданному, то он победит
        '''
        response = self.report.request_json(
            'place=vendor_incut&hid=3&rearr-factors=market_vendor_incut_min_size=3;market_vendor_incut_with_CPA_offers_only=0;market_vendor_incut_hide_undeliverable_models=0'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'product',
                        'vendor': {
                            'id': 4,
                        },
                    },
                ]
            },
            allow_different_len=True,
        )

    def test_vendor_incut_size(self):
        '''
        Проверяем, работу флага market_vendor_incut_size
        '''
        response = self.report.request_json(
            'place=vendor_incut&hid=3&rearr-factors=market_vendor_incut_min_size=4;market_vendor_incut_size=2;market_vendor_incut_with_CPA_offers_only=0;market_vendor_incut_hide_undeliverable_models=0'  # noqa
        )
        self.assertFragmentIn(response, {'results': ElementCount(2)}, allow_different_len=False)

    def test_text_search_touch(self):
        '''
        Проверяем, текстовый поиск по карточкам моделей работает на таче
        '''
        response = self.report.request_json(
            'place=vendor_incut&place=vendor_incut&text=Incom%20Corporation&touch=1&hid=3'
            '&rearr-factors=market_vendor_incut_min_size=2;market_vendor_incut_with_CPA_offers_only=0;market_vendor_incut_hide_undeliverable_models=0'  # noqa
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'product',
                        'vendor': {
                            'id': 3,
                        },
                    },
                ]
            },
            allow_different_len=True,
        )

    def test_text_search_desktop_enabled(self):
        '''
        Проверяем, текстовый поиск по карточкам моделей работает на десктопе
        '''
        response = self.report.request_json(
            'place=vendor_incut&place=vendor_incut&text=Incom%20Corporation&touch=0&hid=3'
            '&rearr-factors=market_vendor_incut_min_size=2;market_vendor_incut_with_CPA_offers_only=0;market_vendor_incut_hide_undeliverable_models=0'  # noqa
        )

        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'product',
                        'vendor': {
                            'id': 3,
                        },
                    },
                ]
            },
            allow_different_len=True,
        )

        self.assertFragmentIn(response, {'results': ElementCount(4)}, allow_different_len=True)

    def test_text_search_desktop_disabled(self):
        '''
        Проверяем, текстовый поиск по карточкам моделей НЕ работает на десктопе под флагом
        '''
        response = self.report.request_json(
            'place=vendor_incut&place=vendor_incut&text=Incom%20Corporation&touch=0&rearr-factors=market_vendor_incut_min_size=2;market_vendor_incut_enable_descktop_text=0;market_vendor_incut_with_CPA_offers_only=0;market_vendor_incut_hide_undeliverable_models=0'  # noqa
        )

        self.assertFragmentIn(response, {'results': ElementCount(0)}, allow_different_len=True)

    def test_text_and_hid_search_desktop(self):
        '''
        Проверяем, текстовый поиск с учетом категоии по карточкам моделей на десктопе
        '''
        response = self.report.request_json(
            'place=vendor_incut&place=vendor_incut&text=MandalMotors&hid=3&rearr-factors=market_vendor_incut_min_size=2;market_vendor_incut_with_CPA_offers_only=0;market_vendor_incut_hide_undeliverable_models=0'  # noqa
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'product',
                        'vendor': {
                            'id': 4,
                        },
                    },
                ]
            },
            allow_different_len=True,
        )

        self.assertFragmentIn(response, {'results': ElementCount(3)}, allow_different_len=True)

    def test_text_and_hid_search_touch(self):
        '''
        Проверяем, текстовый поиск с учетом категоии по карточкам моделей на таче
        '''
        response = self.report.request_json(
            'place=vendor_incut&place=vendor_incut&text=MandalMotors&hid=3&rearr-factors=market_vendor_incut_min_size=2;market_vendor_incut_with_CPA_offers_only=0;market_vendor_incut_hide_undeliverable_models=0&touch=1'  # noqa
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'product',
                        'vendor': {
                            'id': 4,
                        },
                    },
                ]
            },
            allow_different_len=True,
        )

        self.assertFragmentIn(response, {'results': ElementCount(3)}, allow_different_len=True)

    def test_vendor_incut_relevance_formula_threshold(self):
        '''
        Проверяем, порог входа в выдачу по релевантности, должена выиграть моделька с большим значением mn value
        '''
        response = self.report.request_json(
            'place=vendor_incut&place=vendor_incut&text=turbolazer&touch=1&hid=5'
            '&rearr-factors=market_vendor_incut_min_size_touch=1;market_vendor_incut_relevance_formula_threshold=0.03;market_vendor_incut_with_CPA_offers_only=0;market_vendor_incut_hide_undeliverable_models=0'  # noqa
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'product',
                        'vendor': {
                            'id': 8,
                        },
                    },
                ]
            },
            allow_different_len=False,
        )

    def test_vendor_incut_relevance_formula_threshold_disabled(self):
        '''
        Проверяем, что если порог не установлен, то выигрывает моделька с большей ставкаой, не смотря на mn value
        '''
        response = self.report.request_json(
            'place=vendor_incut&place=vendor_incut&hid=5&touch=0&rearr-factors=market_vendor_incut_min_size=1;market_vendor_incut_with_CPA_offers_only=0;market_vendor_incut_hide_undeliverable_models=0'  # noqa
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'product',
                        'vendor': {
                            'id': 7,
                        },
                    },
                ]
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_vendor_incut_default_logic_desktop(cls):
        cls.index.models += [
            Model(
                hid=6,
                hyperid=1127 + i,
                vbid=10,
                vendor_id=9,
                datasource_id=1,
            )
            for i in range(10)
        ]
        cls.index.models += [
            Model(
                hid=6,
                hyperid=1137 + i,
                vbid=12,
                vendor_id=10,
                datasource_id=1,
            )
            for i in range(7)
        ]
        cls.index.models += [
            Model(
                hid=6,
                hyperid=1147 + i,
                vbid=1200,
                vendor_id=11,
                datasource_id=1,
            )
            for i in range(2)
        ]

    def test_vendor_incut_default_logic_desktop(self):
        """
        Проверяем, что по дефолту на десктопе в безтекстовом поиске в аукционе учитывается не более 8 моделей (т.е. не более 8 ставок вендоров на модели)
        """
        response = self.report.request_json(
            'place=vendor_incut&place=vendor_incut&hid=6&rearr-factors=market_vendor_incut_with_CPA_offers_only=0;market_vendor_incut_hide_undeliverable_models=0'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'product',
                        'vendor': {
                            'id': 10,
                        },
                    }
                ]
            },
            allow_different_len=True,
        )

        self.assertFragmentIn(response, {'results': ElementCount(7)}, allow_different_len=True)

        self.assertFragmentNotIn(
            response,
            {
                'results': [
                    {
                        'entity': 'product',
                        'vendor': {
                            'id': 9,
                        },
                    }
                ]
            },
        )

    @classmethod
    def prepare_vendor_incut_default_logic_touch(cls):
        cls.index.models += [
            Model(
                hid=7,
                hyperid=1157 + i,
                vbid=10,
                vendor_id=12,
                datasource_id=1,
            )
            for i in range(10)
        ]
        cls.index.models += [
            Model(
                hid=7,
                hyperid=1167 + i,
                vbid=12,
                vendor_id=13,
                datasource_id=1,
            )
            for i in range(6)
        ]
        cls.index.models += [
            Model(
                hid=7,
                hyperid=1177 + i,
                vbid=1200,
                vendor_id=14,
                datasource_id=1,
            )
            for i in range(1)
        ]

    def test_vendor_incut_default_logic_touch(self):
        """
        Проверяем, что по дефолту на таче в аукционе учитывается не более 7 моделей (т.е. не более 7 ставок вендоров на модели)
        """
        response = self.report.request_json(
            'place=vendor_incut&place=vendor_incut&hid=7&touch=1&rearr-factors=market_vendor_incut_with_CPA_offers_only=0;market_vendor_incut_hide_undeliverable_models=0'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'product',
                        'vendor': {
                            'id': 13,
                        },
                    }
                ]
            },
            allow_different_len=True,
        )

        self.assertFragmentIn(response, {'results': ElementCount(6)}, allow_different_len=True)

        self.assertFragmentNotIn(
            response,
            {
                'results': [
                    {
                        'entity': 'product',
                        'vendor': {
                            'id': 12,
                        },
                    }
                ]
            },
        )

    @classmethod
    def prepare_with_CPA_only(cls):
        cls.index.models += [
            Model(
                hid=8,
                hyperid=1257 + i,
                vbid=10,
                vendor_id=15,
                datasource_id=1,
            )
            for i in range(10)
        ]
        cls.index.regional_models += [RegionalModel(hyperid=1257 + i, has_cpa=True) for i in range(10)]
        cls.index.models += [
            Model(
                hid=8,
                hyperid=1267 + i,
                vbid=12,
                vendor_id=16,
                datasource_id=1,
            )
            for i in range(10)
        ]
        cls.index.regional_models += [RegionalModel(hyperid=1267 + i, has_cpa=True) for i in range(10)]
        cls.index.models += [
            Model(
                hid=8,
                hyperid=1277 + i,
                vbid=1200,
                vendor_id=17,
                datasource_id=1,
            )
            for i in range(10)
        ]

    def test_with_CPA_only(self):
        """
        Проверяем, что под дефолтным значением флага market_vendor_incut_with_CPA_offers_only в вендорском аукционе участвуют только модельки с CPA офферами
        """
        response = self.report.request_json(
            'place=vendor_incut&place=vendor_incut&hid=8&rearr-factors=market_vendor_incut_hide_undeliverable_models=0'
        )

        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'product',
                        'vendor': {
                            'id': 16,
                        },
                    }
                ]
            },
            allow_different_len=True,
        )

    @classmethod
    def prepare_boost_with_CPA(cls):
        cls.index.models += [
            Model(
                hid=9,
                hyperid=1217 + i,
                vbid=11,
                vendor_id=18,
                datasource_id=1,
            )
            for i in range(10)
        ]
        cls.index.regional_models += [RegionalModel(hyperid=1217 + i, has_cpa=False) for i in range(10)]
        cls.index.models += [
            Model(
                hid=9,
                hyperid=1227 + i,
                vbid=10,
                vendor_id=19,
                datasource_id=1,
            )
            for i in range(10)
        ]
        cls.index.regional_models += [RegionalModel(hyperid=1227 + i, has_cpa=True) for i in range(10)]
        cls.index.models += [
            Model(
                hid=9,
                hyperid=1237 + i,
                vbid=9,
                vendor_id=20,
                datasource_id=1,
            )
            for i in range(10)
        ]
        cls.index.regional_models += [RegionalModel(hyperid=1237 + i, has_cpa=True) for i in range(10)]

    def test_boost_with_CPA(self):
        """
        Проверяем, что под флагом market_vendor_incut_boost_with_CPA_offers в вендорском аукционе бустятся модели с CPA офферами,
        и несмотря на то, что у вендора 18 ставка больше, он не выиграет. А из двух вендоров с CPA офферами в модельках выиграет тот, кто больше поставил
        """
        response = self.report.request_json(
            'place=vendor_incut&place=vendor_incut&hid=9&rearr-factors=market_vendor_incut_boost_with_CPA_offers=1;market_vendor_incut_boost_with_CPA_offers_multiplier=1.2;market_vendor_incut_with_CPA_offers_only=0;market_vendor_incut_hide_undeliverable_models=0'  # noqa
        )

        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'product',
                        'vendor': {
                            'id': 19,
                        },
                    }
                ]
            },
            allow_different_len=True,
        )

    @classmethod
    def prepare_with_CPA_only_mixed(cls):
        cls.index.models += [
            Model(
                hid=10,
                hyperid=1347,
                vbid=11,
                vendor_id=20,
                datasource_id=1,
            ),
            Model(
                hid=10,
                hyperid=1348,
                vbid=19,
                vendor_id=20,
                datasource_id=1,
            ),
            Model(
                hid=10,
                hyperid=1349,
                vbid=11,
                vendor_id=20,
                datasource_id=1,
            ),
            Model(
                hid=10,
                hyperid=1350,
                vbid=19,
                vendor_id=20,
                datasource_id=1,
            ),
        ]
        cls.index.regional_models += [
            RegionalModel(hyperid=1347, has_cpa=False),
            RegionalModel(hyperid=1348, has_cpa=True),
            RegionalModel(hyperid=1349, has_cpa=False),
            RegionalModel(hyperid=1350, has_cpa=True),
        ]
        cls.index.models += [
            Model(
                hid=10,
                hyperid=1357,
                vbid=10,
                vendor_id=21,
                datasource_id=1,
            ),
            Model(
                hid=10,
                hyperid=1358,
                vbid=10,
                vendor_id=21,
                datasource_id=1,
            ),
            Model(
                hid=10,
                hyperid=1359,
                vbid=10,
                vendor_id=21,
                datasource_id=1,
            ),
            Model(
                hid=10,
                hyperid=1360,
                vbid=10,
                vendor_id=21,
                datasource_id=1,
            ),
            Model(
                hid=10,
                hyperid=1361,
                vbid=10,
                vendor_id=21,
                datasource_id=1,
            ),
        ]
        cls.index.regional_models += [
            RegionalModel(hyperid=1357, has_cpa=True),
            RegionalModel(hyperid=1358, has_cpa=True),
            RegionalModel(hyperid=1359, has_cpa=True),
            RegionalModel(hyperid=1360, has_cpa=False),
            RegionalModel(hyperid=1361, has_cpa=True),
        ]
        cls.index.models += [
            Model(
                hid=10,
                hyperid=1367,
                vbid=1000,
                vendor_id=22,
                datasource_id=1,
            ),
            Model(
                hid=10,
                hyperid=1368,
                vbid=1000,
                vendor_id=22,
                datasource_id=1,
            ),
            Model(
                hid=10,
                hyperid=1369,
                vbid=1000,
                vendor_id=22,
                datasource_id=1,
            ),
            Model(
                hid=10,
                hyperid=1371,
                vbid=1000,
                vendor_id=22,
                datasource_id=1,
            ),
            Model(
                hid=10,
                hyperid=1372,
                vbid=1000,
                vendor_id=22,
                datasource_id=1,
            ),
            Model(
                hid=10,
                hyperid=1373,
                vbid=1000,
                vendor_id=22,
                datasource_id=1,
            ),
        ]
        cls.index.regional_models += [
            RegionalModel(hyperid=1367, has_cpa=True),
            RegionalModel(hyperid=1369, has_cpa=False),
            RegionalModel(hyperid=1372, has_cpa=False),
        ]

    def test_with_CPA_only_mixed(self):
        """
        Проверяем, что под дефолтным значением флага market_vendor_incut_with_CPA_offers_only в вендорском аукционе участвуют только модели с CPA офферами,
        и при этом:
        1. выигрывает вендор 21 - у него суммарная максимальная ставка на модели с CPA и он удовлеворяет условиям входа в аукцион
        2. в выдаче будет 4 модели
        3. вендор 22 не попадает в аукцион, так как у него всего одна модель с CPA оффером
        4. что будет, если не для всех моделей задать в лайтах статистику
        5. восемь моделек отфильтровано
        """
        response = self.report.request_json(
            'place=vendor_incut&place=vendor_incut&hid=10&&rearr-factors=market_vendor_incut_min_size=2;market_vendor_incut_size=5;market_vendor_incut_hide_undeliverable_models=0&debug=1'
        )

        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'product',
                        'vendor': {
                            'id': 21,
                        },
                    }
                ]
            },
            allow_different_len=True,
        )

        self.assertFragmentIn(response, {'results': ElementCount(4)}, allow_different_len=True)

        self.assertFragmentIn(
            response, {"debug": {"brief": {"filters": {"MODEL_WITHOUT_CPA_OFFER": 8}}}}, allow_different_len=True
        )

    @classmethod
    def prepare_boost_with_CPA_correct_multiply(cls):
        cls.index.models += [
            Model(
                hid=11,
                hyperid=1380,
                vbid=10,
                vendor_id=23,
                datasource_id=1,
            ),
            Model(
                hid=11,
                hyperid=1381,
                vbid=10,
                vendor_id=23,
                datasource_id=1,
            ),
            Model(
                hid=11,
                hyperid=1382,
                vbid=10,
                vendor_id=23,
                datasource_id=1,
            ),
            Model(
                hid=11,
                hyperid=1383,
                vbid=21,
                vendor_id=24,
                datasource_id=1,
            ),
            Model(
                hid=11,
                hyperid=1384,
                vbid=10,
                vendor_id=24,
                datasource_id=1,
            ),
        ]

        cls.index.regional_models += [
            RegionalModel(hyperid=1382, has_cpa=True),
        ]

    def test_boost_with_CPA_correct_multiply(self):
        """
        Проверяем, что под флагом market_vendor_incut_boost_with_CPA_offers в вендорском аукционе бустятся модели с CPA офферами,
        и при этом:
        1. модельки без CPA участвуют в аукционе
        2. буст моделек работает
        """
        response = self.report.request_json(
            'place=vendor_incut&place=vendor_incut&hid=11&&rearr-factors=market_vendor_incut_min_size=2;market_vendor_incut_size=5;market_vendor_incut_boost_with_CPA_offers=1;market_vendor_incut_boost_with_CPA_offers_multiplier=1.2;market_vendor_incut_with_CPA_offers_only=0;market_vendor_incut_hide_undeliverable_models=0'  # noqa
        )

        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'product',
                        'vendor': {
                            'id': 23,
                        },
                    }
                ]
            },
            allow_different_len=True,
        )

        self.assertFragmentIn(response, {'results': ElementCount(3)}, allow_different_len=True)

    @classmethod
    def prepare_boost_with_CPA_correct_multiply2(cls):
        cls.index.models += [
            Model(
                hid=12,
                hyperid=1385,
                vbid=10,
                vendor_id=25,
                datasource_id=1,
            ),
            Model(
                hid=12,
                hyperid=1386,
                vbid=10,
                vendor_id=25,
                datasource_id=1,
            ),
            Model(
                hid=12,
                hyperid=1387,
                vbid=10,
                vendor_id=25,
                datasource_id=1,
            ),
            Model(
                hid=12,
                hyperid=1388,
                vbid=21,
                vendor_id=26,
                datasource_id=1,
            ),
            Model(
                hid=12,
                hyperid=1389,
                vbid=12,
                vendor_id=26,
                datasource_id=1,
            ),
        ]

        cls.index.regional_models += [
            RegionalModel(hyperid=1387, has_cpa=True),
        ]

    def test_boost_with_CPA_correct_multiply2(self):
        """
        Проверяем, что под флагом market_vendor_incut_boost_with_CPA_offers можно выиграть и без CPA офферов
        """
        response = self.report.request_json(
            'place=vendor_incut&place=vendor_incut&hid=12&&rearr-factors=market_vendor_incut_min_size=2;market_vendor_incut_size=5;market_vendor_incut_boost_with_CPA_offers=1;market_vendor_incut_boost_with_CPA_offers_multiplier=1.2;market_vendor_incut_with_CPA_offers_only=0;market_vendor_incut_hide_undeliverable_models=0'  # noqa
        )

        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'product',
                        'vendor': {
                            'id': 26,
                        },
                    }
                ]
            },
            allow_different_len=True,
        )

        self.assertFragmentIn(response, {'results': ElementCount(2)}, allow_different_len=True)

    @classmethod
    def prepare_truthful_CPA_trait(cls):
        cls.index.regiontree += [Region(rid=2, name='Питер'), Region(rid=213, name='Нерезиновая')]
        cls.index.shops += [
            Shop(fesh=3, priority_region=213, regions=[225], cpa=Shop.CPA_REAL, cpc=Shop.CPC_NO),
            Shop(fesh=4, priority_region=213, regions=[225], cpa=Shop.CPA_REAL, cpc=Shop.CPC_NO),
        ]

        cls.index.models += [
            Model(
                hid=13,
                hyperid=1400,
                title='Large Kwama Egg',
                vbid=120,
                vendor_id=27,
                datasource_id=1,
            ),
            Model(
                hid=13,
                hyperid=1401,
                title='Diamond',
                vbid=10,
                vendor_id=27,
                datasource_id=1,
            ),
            Model(
                hid=13,
                hyperid=1402,
                title='Netch Leather',
                vbid=100,
                vendor_id=28,
                datasource_id=1,
            ),
            Model(
                hid=13,
                hyperid=1403,
                title='Scamp Skin',
                vbid=100,
                vendor_id=28,
                datasource_id=1,
            ),
        ]

        def pic():
            return Picture(width=100, height=100, group_id=12345)

        cls.index.offers += [
            Offer(
                hid=13,
                title='market 4.0 cpa',
                waremd5='VkjX-08eqaaf0Q_MrNfQBw',
                hyperid=1400,
                fesh=3,
                cpa=Offer.CPA_REAL,
                picture=pic(),
            ),
            Offer(
                hid=13,
                title='market 4.0 cpa',
                waremd5='mxyTpoZMOeSEF-5Ia4PDhw',
                hyperid=1401,
                fesh=3,
                cpa=Offer.CPA_REAL,
                picture=pic(),
            ),
            Offer(
                hid=13, title='market 4.0 cpc', waremd5='UChUMwabn69TyQDNJkL6nQ', hyperid=1402, fesh=4, picture=pic()
            ),
            Offer(
                hid=13, title='market 4.0 cpc', waremd5='q-u3w59LXka7z6srVl7zKw', hyperid=1403, fesh=4, picture=pic()
            ),
        ]

    def test_truthful_CPA_trait(self):
        """
        Проверяем, что сочетание флагов market_vendor_incut_truthful_CPA_trait=1 и market_vendor_incut_with_CPA_offers_only=0
        приводит к честному сбору информации о наличии CPA офферов у моделей и выфильтровыванию моделей без CPA при подсчете ставки:
        проверяем, что вооще работает: должен выиграть вендор с меншьшей суммой ставок, так как у второго нет CPA офферов
        """
        response = self.report.request_json(
            'place=vendor_incut&hid=13&&rearr-factors=market_vendor_incut_min_size=2;market_vendor_incut_size=5;market_vendor_incut_truthful_CPA_trait=1;market_vendor_incut_with_CPA_offers_only=0;market_vendor_incut_hide_undeliverable_models=0'  # noqa
        )

        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'product',
                        'vendor': {
                            'id': 27,
                        },
                    }
                ]
            },
            allow_different_len=True,
        )

    @classmethod
    def prepare_truthful_CPA_trait_correct_vendor_bid(cls):
        cls.index.shops += [
            Shop(fesh=5, priority_region=213, regions=[225], cpa=Shop.CPA_REAL, cpc=Shop.CPC_NO),
            Shop(fesh=6, priority_region=213, regions=[225], cpa=Shop.CPA_REAL, cpc=Shop.CPC_NO),
        ]

        cls.index.models += [
            Model(
                hid=14,
                hyperid=1404,
                title='Bonemeal',
                vbid=10,
                vendor_id=29,
                datasource_id=1,
            ),
            Model(
                hid=14,
                hyperid=1405,
                title='Spore Pod',
                vbid=10,
                vendor_id=29,
                datasource_id=1,
            ),
            Model(
                hid=14,
                hyperid=1406,
                title='Chokeweed',
                vbid=10,
                vendor_id=29,
                datasource_id=1,
            ),
            Model(
                hid=14,
                hyperid=1407,
                title='Ash Salts',
                vbid=100,
                vendor_id=30,
                datasource_id=1,
            ),
            Model(
                hid=14,
                hyperid=1408,
                title='Raw Ebony',
                vbid=14,
                vendor_id=30,
                datasource_id=1,
            ),
            Model(
                hid=14,
                hyperid=1409,
                title='Racer Plumes',
                vbid=15,
                vendor_id=30,
                datasource_id=1,
            ),
        ]

        def pic():
            return Picture(width=100, height=100, group_id=12345)

        cls.index.offers += [
            Offer(
                hid=14,
                title='market 4.0 cpa',
                waremd5='VkjX-08eqaaf1Q_MrNfQBw',
                hyperid=1404,
                fesh=5,
                cpa=Offer.CPA_REAL,
                picture=pic(),
            ),
            Offer(
                hid=14,
                title='market 4.0 cpa',
                waremd5='mxyTpoZMOeSEF25Ia4PDhw',
                hyperid=1405,
                fesh=5,
                cpa=Offer.CPA_REAL,
                picture=pic(),
            ),
            Offer(
                hid=14,
                title='market 4.0 cpa',
                waremd5='UChUMwabn69TyQ2NJkL6nQ',
                hyperid=1406,
                fesh=5,
                cpa=Offer.CPA_REAL,
                picture=pic(),
            ),
            Offer(
                hid=14, title='market 4.0 cpc', waremd5='q-u3w59LXka7z6s3Vl7zKw', hyperid=1407, fesh=6, picture=pic()
            ),
            Offer(
                hid=14,
                title='market 4.0 cpa',
                waremd5='q-udw59LXka7z6sr3l7zKw',
                hyperid=1408,
                fesh=6,
                cpa=Offer.CPA_REAL,
                picture=pic(),
            ),
            Offer(
                hid=14,
                title='market 4.0 cpa',
                waremd5='q-u3r59LXka7z6srV57zKw',
                hyperid=1409,
                fesh=6,
                cpa=Offer.CPA_REAL,
                picture=pic(),
            ),
        ]

    def test_truthful_CPA_trait_correct_vendor_bid(self):
        """
        Проверяем, что сочетание флагов market_vendor_incut_truthful_CPA_trait=1 и market_vendor_incut_with_CPA_offers_only=0
        приводит к честному сбору информации о наличии CPA офферов у моделей и выфильтровыванию моделей без CPA при подсчете ставки:
        у аукционе не участвует одна модель, выигрывает ставка 30 и клик прайс 30 так как подперта ставкой 29
        """
        response = self.report.request_json(
            'place=vendor_incut&place=vendor_incut&hid=14&rearr-factors=market_vendor_incut_min_size=2;market_vendor_incut_size=5;'
            'market_vendor_incut_truthful_CPA_trait=1;market_vendor_incut_with_CPA_offers_only=0;market_vendor_incut_hide_undeliverable_models=0&debug=1'
        )

        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'product',
                        'vendor': {
                            'id': 29,
                        },
                    }
                ]
            },
            allow_different_len=True,
        )

        self.assertFragmentIn(
            response, {"logicTrace": [Contains("CalculateAutobrokerMultiplier(): Top group auction value: 30")]}
        )
        self.assertFragmentIn(
            response, {"logicTrace": [Contains("CalculateAutobrokerMultiplier(): Second group auction value: 29")]}
        )

    @classmethod
    def prepare_truthful_CPA_trait_correct_output(cls):
        cls.index.shops += [
            Shop(fesh=6, priority_region=213, regions=[225], cpa=Shop.CPA_REAL, cpc=Shop.CPC_NO),
            Shop(fesh=7, priority_region=213, regions=[225], cpa=Shop.CPA_REAL, cpc=Shop.CPC_NO),
        ]

        cls.index.models += [
            Model(
                hid=15,
                hyperid=1410,
                title='Durzog Meat',
                vbid=10,
                vendor_id=31,
                datasource_id=1,
            ),
            Model(
                hid=15,
                hyperid=1411,
                title='Wolf Pelt',
                vbid=10,
                vendor_id=31,
                datasource_id=1,
            ),
            Model(
                hid=15,
                hyperid=1412,
                title='Horn Lily Bulb',
                vbid=10,
                vendor_id=31,
                datasource_id=1,
            ),
            Model(
                hid=15,
                hyperid=1413,
                title='Heart of an Innocent',
                vbid=100,
                vendor_id=32,
                datasource_id=1,
            ),
            Model(
                hid=15,
                hyperid=1414,
                title='Raw Glass',
                vbid=16,
                vendor_id=32,
                datasource_id=1,
            ),
            Model(
                hid=15,
                hyperid=1415,
                title='Treated Bittergreen Petals',
                vbid=15,
                vendor_id=32,
                datasource_id=1,
            ),
        ]

        def pic():
            return Picture(width=100, height=100, group_id=12345)

        cls.index.offers += [
            Offer(
                hid=15,
                title='market 4.0 cpa',
                waremd5='VkjX-0weqaaf1Q_MrNfQBw',
                hyperid=1410,
                fesh=5,
                cpa=Offer.CPA_REAL,
                picture=pic(),
            ),
            Offer(
                hid=15,
                title='market 4.0 cpa',
                waremd5='mxyTpoZfOeSEF25Ia4PDhw',
                hyperid=1411,
                fesh=5,
                cpa=Offer.CPA_REAL,
                picture=pic(),
            ),
            Offer(
                hid=15,
                title='market 4.0 cpa',
                waremd5='UChUMwabg69TyQ2NJkL6nQ',
                hyperid=1412,
                fesh=5,
                cpa=Offer.CPA_REAL,
                picture=pic(),
            ),
            Offer(
                hid=15, title='market 4.0 cpc', waremd5='q-u3w59LXga7z6s3Vl7zKw', hyperid=1413, fesh=6, picture=pic()
            ),
            Offer(
                hid=15,
                title='market 4.0 cpa',
                waremd5='q-udw59LXkj7z6sr3l7zKw',
                hyperid=1414,
                fesh=6,
                cpa=Offer.CPA_REAL,
                picture=pic(),
            ),
            Offer(
                hid=15,
                title='market 4.0 cpa',
                waremd5='q-u3r59LXka8z6srV57zKw',
                hyperid=1415,
                fesh=6,
                cpa=Offer.CPA_REAL,
                picture=pic(),
            ),
        ]

    def test_truthful_CPA_trait_correct_output(self):
        """
        Проверяем, что сочетание флагов market_vendor_incut_truthful_CPA_trait=1 и market_vendor_incut_with_CPA_offers_only=0
        приводит к честному сбору информации о наличии CPA офферов у моделей и выфильтровыванию моделей без CPA при подсчете ставки:
        у аукционе не участвует одна модель, выигрывает ставка 31 так как подперта ставкой 30, а в выдаче всего два модели,
        так как третья модель вендора не CPA
        """
        response = self.report.request_json(
            'place=vendor_incut&place=vendor_incut&hid=15&rearr-factors=market_vendor_incut_min_size=2;market_vendor_incut_size=5;market_vendor_incut_truthful_CPA_trait=1;market_vendor_incut_with_CPA_offers_only=0;market_vendor_incut_hide_undeliverable_models=0&debug=1'  # noqa
        )

        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'product',
                        'vendor': {
                            'id': 32,
                        },
                    }
                ]
            },
            allow_different_len=True,
        )

        self.assertFragmentIn(response, {'results': ElementCount(2)}, allow_different_len=True)

        self.assertFragmentIn(
            response, {"logicTrace": [Contains("CalculateAutobrokerMultiplier(): Top group auction value: 31")]}
        )
        self.assertFragmentIn(
            response, {"logicTrace": [Contains("CalculateAutobrokerMultiplier(): Second group auction value: 30")]}
        )

    @classmethod
    def prepare_truthful_CPA_trait_minimal_offers_number(cls):
        cls.index.shops += [
            Shop(fesh=8, priority_region=213, regions=[225], cpa=Shop.CPA_REAL, cpc=Shop.CPC_NO),
            Shop(fesh=9, priority_region=213, regions=[225], cpa=Shop.CPA_REAL, cpc=Shop.CPC_NO),
        ]

        cls.index.models += [
            Model(
                hid=16,
                hyperid=1416,
                title='Durzog Meat',
                vbid=10,
                vendor_id=33,
                datasource_id=1,
            ),
            Model(
                hid=16,
                hyperid=1417,
                title='Wolf Pelt',
                vbid=10,
                vendor_id=33,
                datasource_id=1,
            ),
            Model(
                hid=16,
                hyperid=1418,
                title='Horn Lily Bulb',
                vbid=10,
                vendor_id=33,
                datasource_id=1,
            ),
            Model(
                hid=16,
                hyperid=1419,
                title='Heart of an Innocent',
                vbid=100,
                vendor_id=34,
                datasource_id=1,
            ),
            Model(
                hid=16,
                hyperid=1420,
                title='Raw Glass',
                vbid=66,
                vendor_id=34,
                datasource_id=1,
            ),
            Model(
                hid=16,
                hyperid=1421,
                title='Treated Bittergreen Petals',
                vbid=65,
                vendor_id=34,
                datasource_id=1,
            ),
        ]

        def pic():
            return Picture(width=100, height=100, group_id=12345)

        cls.index.offers += [
            Offer(
                hid=16,
                title='market 4.0 cpa',
                waremd5='VkjX-0weqaaf1QfMrNfQBw',
                hyperid=1416,
                fesh=8,
                cpa=Offer.CPA_REAL,
                picture=pic(),
            ),
            Offer(
                hid=16,
                title='market 4.0 cpa',
                waremd5='mxyTpoZfOeSEF2dIa4PDhw',
                hyperid=1417,
                fesh=8,
                cpa=Offer.CPA_REAL,
                picture=pic(),
            ),
            Offer(
                hid=16,
                title='market 4.0 cpa',
                waremd5='UChUMwabg69TkikNJkL6nQ',
                hyperid=1417,
                fesh=8,
                cpa=Offer.CPA_REAL,
                picture=pic(),
            ),
            Offer(
                hid=16,
                title='market 4.0 cpa',
                waremd5='q-u3w59LXgi6z6s3Vl7zKw',
                hyperid=1418,
                fesh=9,
                cpa=Offer.CPA_REAL,
                picture=pic(),
            ),
            Offer(
                hid=16, title='market 4.0 cpc', waremd5='q-udw59LXkhjm6sr3l7zKw', hyperid=1420, fesh=9, picture=pic()
            ),
            Offer(
                hid=16, title='market 4.0 cpc', waremd5='q-u3r59Lhmj8z6srV57zKw', hyperid=1421, fesh=9, picture=pic()
            ),
        ]

    def test_truthful_CPA_trait_minimal_offers_number(self):
        """
        Проверяем, что сочетание флагов market_vendor_incut_truthful_CPA_trait=1 и market_vendor_incut_with_CPA_offers_only=0
        приводит к честному сбору информации о наличии CPA офферов у моделей и выфильтровыванию моделей без CPA при подсчете ставки:
        выиграет вендор 33 несмотря на то, что его ставка меньше, так как у 34 всего одна модель с CPA, а нужно как минимум 2
        """
        response = self.report.request_json(
            'place=vendor_incut&place=vendor_incut&hid=16&rearr-factors=market_vendor_incut_min_size=2;market_vendor_incut_size=5;market_vendor_incut_truthful_CPA_trait=1;market_vendor_incut_with_CPA_offers_only=0;market_vendor_incut_hide_undeliverable_models=0&debug=1'  # noqa
        )

        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'product',
                        'vendor': {
                            'id': 33,
                        },
                    }
                ]
            },
            allow_different_len=True,
        )

        self.assertFragmentIn(response, {'results': ElementCount(3)}, allow_different_len=True)

    @classmethod
    def prepare_banners_base(cls):
        cls.index.models += [
            Model(
                hid=17,
                hyperid=1422,
                title='with banner',
                vbid=10,
                vendor_id=34,
                datasource_id=1,
            ),
        ]

        cls.index.vendors_banners += [VendorBanner(1, 34, 17, 1, 800)]

    def test_banners_base(self):
        """
        Проверяем, что баннер возвращается, если он есть, и флаги соответствующие
        """
        response = self.report.request_json(
            'place=vendor_incut&place=vendor_incut&hid=17&rearr-factors=market_vendor_incut_min_size=1;market_vendor_incut_size=5;market_vendor_incut_truthful_CPA_trait=0;market_vendor_incut_with_CPA_offers_only=0;market_vendor_incut_enable_banners=1;market_vendor_incut_hide_undeliverable_models=0&debug=1'  # noqa
        )

        self.assertFragmentIn(
            response,
            {
                'entity': 'vendorBanner',
                'vendorId': 34,
                'bannerId': 1,
            },
            allow_different_len=True,
        )

    @classmethod
    def prepare_use_default_CPA_offers(cls):
        cls.index.shops += [
            Shop(fesh=10, priority_region=213, regions=[225], cpa=Shop.CPA_REAL, cpc=Shop.CPC_NO),
            Shop(fesh=11, priority_region=213, regions=[225]),
        ]

        cls.index.models += [
            Model(
                hid=18,
                hyperid=1423,
                title=' PZM-9 Straznik',
                vbid=10,
                vendor_id=33,
                datasource_id=1,
            ),
        ]

        cls.index.offers += [
            Offer(
                hid=18,
                title='Mech from Market',
                waremd5='VkjX-0weqaaf1QfMrNfQlw',
                hyperid=1423,
                fesh=10,
                ts=3,
                cpa=Offer.CPA_REAL,
            ),
            Offer(hid=18, title='Other mech', waremd5='mx6TpoZfOeSEF2dIa4PDhw', hyperid=1423, fesh=11, ts=4),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3).respond(0.04)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 4).respond(0.14)

    def test_use_default_CPA_offers(self):
        '''
        Проверяем логику выбора оффера при условии, что во врезку попадают модели только с CPA офферами. ДО, прибитый к модели должен быть CPA
        '''
        response = self.report.request_json(
            'place=vendor_incut&hid=18&use-default-offers=1&rearr-factors=market_vendor_incut_min_size=1;market_vendor_incut_hide_undeliverable_models=0'
        )
        self.assertFragmentIn(
            response,
            {
                'entity': 'product',
                'offers': {
                    'items': [
                        {
                            'entity': 'offer',
                            'titles': {
                                'raw': 'Mech from Market',
                            },
                        }
                    ]
                },
            },
        )

        '''
        Проверяем, что всегда вернется CPA ДО (мы его бустим)
        '''
        response = self.report.request_json(
            'place=vendor_incut&hid=18&use-default-offers=1&rearr-factors=market_vendor_incut_min_size=1;market_vendor_incut_with_CPA_offers_only=0;market_vendor_incut_hide_undeliverable_models=0'
        )
        self.assertFragmentIn(
            response,
            {'entity': 'product', 'offers': {'items': [{'entity': 'offer', 'titles': {'raw': 'Mech from Market'}}]}},
        )

    @classmethod
    def prepare_banners_affects_auction(cls):
        cls.index.models += [
            Model(
                hid=19,
                hyperid=1424,
                title='with banner',
                vbid=10,
                vendor_id=35,
                datasource_id=1,
            ),
            Model(
                hid=19,
                hyperid=1425,
                title='with out banner',
                vbid=100,
                vendor_id=36,
                datasource_id=1,
            ),
        ]

        cls.index.vendors_banners += [VendorBanner(1, 35, 19, 2, 800)]

    def test_banners_affects_auction(self):
        """
        Проверяем, что баннеры учитываются при проведении аукциона во врезке, а так же баннер пишется в show лог
        """
        response = self.report.request_json(
            'place=vendor_incut&place=vendor_incut&hid=19&rearr-factors=market_vendor_incut_min_size=1;market_vendor_incut_size=5;'
            'market_vendor_incut_truthful_CPA_trait=0;market_vendor_incut_with_CPA_offers_only=0;market_vendor_incut_enable_banners=1;market_vendor_incut_banner_with_out_autobroker=0;market_vendor_incut_hide_undeliverable_models=0'  # noqa
            '&debug=1'
        )
        self.assertFragmentIn(
            response,
            {
                'entity': 'vendorBanner',
                'vendorId': 35,
                'bannerId': 2,
            },
            allow_different_len=True,
        )

        self.show_log_tskv.expect(hyper_id=1424, vc_bid=10, url_type=16)
        self.show_log_tskv.expect(hyper_cat_id=19, vendor_id=35, vc_bid=800, url_type=40, vendor_banner_id=2)

    def test_banners_click_log(self):
        """
        Проверяем, что баннеры учитываются при проведении аукциона во врезке, а так же баннер пишется в show лог
        """
        _ = self.report.request_json(
            'place=vendor_incut&place=vendor_incut&hid=19&show-urls=productVendorBid&'
            'rearr-factors=market_vendor_incut_min_size=1;market_vendor_incut_size=5;market_vendor_incut_truthful_CPA_trait=0;market_vendor_incut_with_CPA_offers_only=0;'
            'market_vendor_incut_enable_banners=1;market_vendor_incut_banner_with_out_autobroker=0;market_vendor_incut_hide_undeliverable_models=0&debug=1&pp=8'
        )

        self.click_log.expect(
            clicktype=ClickType.EXTERNAL,
            dtype='modelcard',
            hyper_cat_id=19,
            vc_bid=800,
            vendor_price=99,
            hyper_id=-1,
            brand_id=35,
            vendor_banner_id=2,
            vendor_ds_id=1,
            pp=8,
        )

    @classmethod
    def prepare_banners_autobroker(cls):
        cls.index.models += [
            Model(
                hid=20,
                hyperid=1426,
                title='жаренные мухи',
                vbid=800,
                vendor_id=37,
                datasource_id=1,
            ),
            Model(
                hid=20,
                hyperid=1427,
                title='вяленные тараканы',
                vbid=800,
                vendor_id=37,
                datasource_id=1,
            ),
            Model(
                hid=20,
                hyperid=1428,
                title='маринованные сверчки',
                vbid=800,
                vendor_id=37,
                datasource_id=1,
            ),
            Model(
                hid=20,
                hyperid=1429,
                title='копченые пауки',
                vbid=400,
                vendor_id=38,
                datasource_id=1,
            ),
            Model(
                hid=20,
                hyperid=1430,
                title='сушеные жуки',
                vbid=400,
                vendor_id=38,
                datasource_id=1,
            ),
        ]

        cls.index.vendors_banners += [VendorBanner(1, 37, 20, 3, 800)]
        cls.index.vendors_banners += [VendorBanner(1, 38, 20, 4, 800)]

    def test_banners_autobroker(self):
        """
        Проверяем, что автоброкер работает для баннеров
        """
        response = self.report.request_json(
            'place=vendor_incut&place=vendor_incut&hid=20&show-urls=productVendorBid&'
            'rearr-factors=market_vendor_incut_min_size=1;market_vendor_incut_size=5;market_vendor_incut_truthful_CPA_trait=0;market_vendor_incut_with_CPA_offers_only=0;'
            'market_vendor_incut_enable_banners=1;market_vendor_incut_banner_with_out_autobroker=0;market_vendor_incut_hide_undeliverable_models=0&debug=1'
        )

        self.assertFragmentIn(
            response,
            {
                'entity': 'vendorBanner',
                'vendorId': 37,
                'bannerId': 3,
                'urls': {
                    'encrypted': NotEmpty(),
                },
            },
            allow_different_len=True,
        )

        self.click_log.expect(
            clicktype=ClickType.EXTERNAL, dtype='modelcard', vc_bid=800, vendor_price=400, vendor_banner_id=3
        )

    def test_disabled_banners_autobroker(self):
        """
        Проверяем, что автоброкер работает для ручки и баннеры не участвуют, хотя и есть у моделек, т.е. проверяем флаг market_vendor_incut_enable_banners
        """
        response = self.report.request_json(
            'place=vendor_incut&place=vendor_incut&hid=20&show-urls=productVendorBid&'
            'rearr-factors=market_vendor_incut_min_size=1;market_vendor_incut_size=5;market_vendor_incut_truthful_CPA_trait=0;market_vendor_incut_with_CPA_offers_only=0;'
            'market_vendor_incut_banner_with_out_autobroker=0;market_vendor_incut_hide_undeliverable_models=0&debug=1'
        )

        self.assertFragmentNotIn(
            response,
            {
                'entity': 'vendorBanner',
                'vendorId': 37,
                'bannerId': 3,
            },
        )

        self.click_log.expect(
            clicktype=ClickType.EXTERNAL, dtype='modelcard', vc_bid=800, vendor_price=267, brand_id=37, hyper_id=1426
        )
        self.click_log.expect(
            clicktype=ClickType.EXTERNAL, dtype='modelcard', vc_bid=800, vendor_price=267, brand_id=37, hyper_id=1427
        )
        self.click_log.expect(
            clicktype=ClickType.EXTERNAL, dtype='modelcard', vc_bid=800, vendor_price=267, brand_id=37, hyper_id=1428
        )

    def test_need_banner_disabled(self):
        """
        Проверяем параметр need_banner. Флаг market_vendor_incut_enable_banners включен.
        """
        response = self.report.request_json(
            'place=vendor_incut&place=vendor_incut&hid=20&show-urls=productVendorBid&need_banner=0&'
            'rearr-factors=market_vendor_incut_min_size=1;market_vendor_incut_size=5;market_vendor_incut_truthful_CPA_trait=0;market_vendor_incut_with_CPA_offers_only=0;'
            'market_vendor_incut_banner_with_out_autobroker=0;market_vendor_incut_enable_banners=1;market_vendor_incut_hide_undeliverable_models=0&debug=1'
        )

        self.assertFragmentNotIn(
            response,
            {
                'entity': 'vendorBanner',
                'vendorId': 37,
                'bannerId': 3,
            },
        )

        self.click_log.expect(
            clicktype=ClickType.EXTERNAL, dtype='modelcard', vc_bid=800, vendor_price=267, brand_id=37, hyper_id=1426
        )
        self.click_log.expect(
            clicktype=ClickType.EXTERNAL, dtype='modelcard', vc_bid=800, vendor_price=267, brand_id=37, hyper_id=1427
        )
        self.click_log.expect(
            clicktype=ClickType.EXTERNAL, dtype='modelcard', vc_bid=800, vendor_price=267, brand_id=37, hyper_id=1428
        )

    def test_need_banner_enabled(self):
        """
        Проверяем параметр need_banner. Параметр включен. Флаг market_vendor_incut_enable_banners включен.
        """
        response = self.report.request_json(
            'place=vendor_incut&place=vendor_incut&hid=20&show-urls=productVendorBid&need_banner=1&'
            'rearr-factors=market_vendor_incut_min_size=1;market_vendor_incut_size=5;market_vendor_incut_truthful_CPA_trait=0;market_vendor_incut_with_CPA_offers_only=0;'
            'market_vendor_incut_banner_with_out_autobroker=0;market_vendor_incut_enable_banners=1;market_vendor_incut_hide_undeliverable_models=0&debug=1'
        )

        self.assertFragmentIn(
            response,
            {
                'entity': 'vendorBanner',
                'vendorId': 37,
                'bannerId': 3,
                'urls': {
                    'encrypted': NotEmpty(),
                },
            },
            allow_different_len=True,
        )

        self.click_log.expect(
            clicktype=ClickType.EXTERNAL, dtype='modelcard', vc_bid=800, vendor_price=400, vendor_banner_id=3
        )

    def test_need_banner_and_flag(self):
        """
        Проверяем параметр need_banner. Проверяем, что при включенном параметре, но выключенном флаге
        market_vendor_incut_enable_banners баннера не будет
        """
        response = self.report.request_json(
            'place=vendor_incut&place=vendor_incut&hid=20&show-urls=productVendorBid&need_banner=1&'
            'rearr-factors=market_vendor_incut_min_size=1;market_vendor_incut_size=5;market_vendor_incut_truthful_CPA_trait=0;market_vendor_incut_with_CPA_offers_only=0;'
            'market_vendor_incut_banner_with_out_autobroker=0;market_vendor_incut_enable_banners=0;market_vendor_incut_hide_undeliverable_models=0&debug=1'
        )

        self.assertFragmentNotIn(
            response,
            {
                'entity': 'vendorBanner',
                'vendorId': 37,
                'bannerId': 3,
            },
        )

        self.click_log.expect(
            clicktype=ClickType.EXTERNAL, dtype='modelcard', vc_bid=800, vendor_price=267, brand_id=37, hyper_id=1426
        )
        self.click_log.expect(
            clicktype=ClickType.EXTERNAL, dtype='modelcard', vc_bid=800, vendor_price=267, brand_id=37, hyper_id=1427
        )
        self.click_log.expect(
            clicktype=ClickType.EXTERNAL, dtype='modelcard', vc_bid=800, vendor_price=267, brand_id=37, hyper_id=1428
        )

    def test_do_not_bill_banners(self):
        """
        Проверяем, что работает флаг не биллить баннера, причем только для баннеров
        """
        _ = self.report.request_json(
            'place=vendor_incut&place=vendor_incut&hid=20&show-urls=productVendorBid&'
            'rearr-factors=market_vendor_incut_min_size=1;market_vendor_incut_size=5;market_vendor_incut_truthful_CPA_trait=0;market_vendor_incut_with_CPA_offers_only=0;market_vendor_incut_enable_banners=1;'  # noqa
            'market_vendor_incut_banner_do_not_bill=1;market_vendor_incut_banner_with_out_autobroker=0;market_vendor_incut_hide_undeliverable_models=0&debug=1'
        )

        self.click_log.expect(
            clicktype=ClickType.EXTERNAL, dtype='modelcard', vc_bid=800, vendor_price=0, hyper_id=-1, vendor_banner_id=3
        )
        self.click_log.expect(
            clicktype=ClickType.EXTERNAL, dtype='modelcard', vc_bid=800, vendor_price=400, hyper_id=1428
        )

    def test_do_not_use_autobroker_on_banner(self):
        """
        Проверяем, что по дефолту работает флаг 'не амнистировать баннеры' - market_vendor_incut_banner_with_out_autobroker
        """
        response = self.report.request_json(
            'place=vendor_incut&place=vendor_incut&hid=20&show-urls=productVendorBid&'
            'rearr-factors=market_vendor_incut_min_size=1;market_vendor_incut_size=5;market_vendor_incut_truthful_CPA_trait=0;market_vendor_incut_with_CPA_offers_only=0;'
            'market_vendor_incut_enable_banners=1;market_vendor_incut_hide_undeliverable_models=0&debug=1'
        )

        self.assertFragmentIn(
            response,
            {
                'entity': 'vendorBanner',
                'vendorId': 37,
                'bannerId': 3,
                'urls': {
                    'encrypted': NotEmpty(),
                },
            },
            allow_different_len=True,
        )

        self.click_log.expect(
            clicktype=ClickType.EXTERNAL, dtype='modelcard', vc_bid=800, vendor_price=800, vendor_banner_id=3
        )

    @classmethod
    def prepare_relevance_in_ranking(cls):

        cls.index.models += [
            Model(
                hid=21,
                hyperid=1431,
                title='SHM-70 Gulyay-Gorod',
                vbid=100,
                vendor_id=39,
                ts=5,
                datasource_id=1,
            ),
            Model(
                hid=21,
                hyperid=1432,
                title='SKS 156 Wotan',
                vbid=10,
                vendor_id=40,
                ts=6,
                datasource_id=1,
            ),
            Model(
                hid=21,
                hyperid=1433,
                title='SKS 300 Kaiser',
                vbid=10,
                vendor_id=40,
                ts=7,
                datasource_id=1,
            ),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 5).respond(0.04)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 6).respond(0.4)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 7).respond(0.4)

    def test_relevance_in_ranking(self):
        """
        Проверяем, что релевантность учитывается при соответствующем флаге:
        ставка больше у первой модели, но у неё очень плохая релевантность, и должен выиграть второй вендор с меньшими ставками
        """
        response = self.report.request_json(
            'place=vendor_incut&place=vendor_incut&hid=21&show-urls=productVendorBid&'
            'rearr-factors=market_vendor_incut_min_size=1;market_vendor_incut_size=5;market_vendor_incut_truthful_CPA_trait=0;market_vendor_incut_with_CPA_offers_only=0;'
            'market_vendor_incut_enable_banners=1;market_vendor_incut_use_relevance_in_ranking=1;market_vendor_incut_hide_undeliverable_models=0&debug=1'
        )

        self.assertFragmentIn(response, {'results': ElementCount(2)})

        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'product',
                        'vendor': {
                            'id': 40,
                        },
                    }
                ]
            },
            allow_different_len=True,
        )

        """
        На всякий случай, прверим, что без флага выиграет вендор с большей ставкой, т.е. проверим, что выдача с флагом
        market_vendor_incut_use_relevance_in_ranking отличается от выдачи без флага
        """

        response = self.report.request_json(
            'place=vendor_incut&place=vendor_incut&hid=21&show-urls=productVendorBid&'
            'rearr-factors=market_vendor_incut_min_size=1;market_vendor_incut_size=5;market_vendor_incut_truthful_CPA_trait=0;market_vendor_incut_with_CPA_offers_only=0;'
            'market_vendor_incut_enable_banners=1;market_vendor_incut_hide_undeliverable_models=0&debug=1'
        )

        self.assertFragmentIn(response, {'results': ElementCount(1)})

        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'product',
                        'vendor': {
                            'id': 39,
                        },
                    }
                ]
            },
            allow_different_len=True,
        )

    @classmethod
    def prepare_sku_required_flag(cls):
        cls.index.models += [
            Model(hid=681, hyperid=681, vendor_id=681),
            Model(hid=681, hyperid=682, vbid=20, vendor_id=681, datasource_id=1),
            Model(hid=681, hyperid=683, vbid=10, vendor_id=681, datasource_id=1),
            Model(hid=681, hyperid=6811, vbid=25, vendor_id=682, datasource_id=2),
        ]

        cls.index.offers += [Offer(hyperid=683)]

        cls.index.models += [
            Model(hid=682, hyperid=6110 + i, vbid=1, vendor_id=681, datasource_id=1) for i in range(0, 10)
        ]

    def test_sku_required_flag(self):
        response = self.report.request_json(
            'place=vendor_incut&hid=1&onstock=1&rearr-factors=market_vendor_incut_min_size=1'
            ';market_vendor_incut_with_CPA_offers_only=0;market_vendor_incut_hide_undeliverable_models=0&sku-required=1'
        )
        self.assertFragmentIn(response, {"results": ElementCount(0)})

    def test_disable_in_android(self):
        """
        Проверяем работу параметра client=ANDROID: по умолчанию, в приложеньках (client == ANDROID || client == IOS) врезку показываем
        """
        response = self.report.request_json(
            'place=vendor_incut&place=vendor_incut&hid=21&show-urls=productVendorBid&'
            'rearr-factors=market_vendor_incut_min_size_touch=1;market_vendor_incut_size=5;market_vendor_incut_truthful_CPA_trait=0;market_vendor_incut_with_CPA_offers_only=0;'
            'market_vendor_incut_enable_banners=1;market_vendor_incut_use_relevance_in_ranking=1;market_vendor_incut_hide_undeliverable_models=0&debug=1'
            '&client=ANDROID'
        )

        self.assertFragmentIn(response, {'results': ElementCount(2)})

        """
        Не показываем врезку в приложении под флагом
        """
        response = self.report.request_json(
            'place=vendor_incut&place=vendor_incut&hid=21&show-urls=productVendorBid&'
            'rearr-factors=market_vendor_incut_min_size_touch=1;market_vendor_incut_size=5;market_vendor_incut_truthful_CPA_trait=0;market_vendor_incut_with_CPA_offers_only=0;'
            'market_vendor_incut_enable_banners=1;market_vendor_incut_use_relevance_in_ranking=1;market_vendor_incut_disable_in_application=1;market_vendor_incut_hide_undeliverable_models=0&debug=1'
            '&client=ANDROID'
        )

        self.assertFragmentIn(response, {"results": ElementCount(0)})

    def test_disable_in_ios(self):
        """
        Проверяем работу параметра client=IOS: по умолчанию, в приложеньках (client == ANDROID || client == IOS) врезку показываем
        """
        response = self.report.request_json(
            'place=vendor_incut&place=vendor_incut&hid=21&show-urls=productVendorBid&'
            'rearr-factors=market_vendor_incut_min_size_touch=1;market_vendor_incut_size=5;market_vendor_incut_truthful_CPA_trait=0;market_vendor_incut_with_CPA_offers_only=0;'
            'market_vendor_incut_enable_banners=1;market_vendor_incut_use_relevance_in_ranking=1;market_vendor_incut_hide_undeliverable_models=0&debug=1'
            '&client=IOS'
        )

        self.assertFragmentIn(response, {'results': ElementCount(2)})

        """
        Не показываем врезку в приложении под флагом
        """
        response = self.report.request_json(
            'place=vendor_incut&place=vendor_incut&hid=21&show-urls=productVendorBid&'
            'rearr-factors=market_vendor_incut_min_size_touch=1;market_vendor_incut_size=5;market_vendor_incut_truthful_CPA_trait=0;market_vendor_incut_with_CPA_offers_only=0;'
            'market_vendor_incut_enable_banners=1;market_vendor_incut_use_relevance_in_ranking=1;market_vendor_incut_disable_in_application=1;market_vendor_incut_hide_undeliverable_models=0&debug=1'
            '&client=IOS'
        )

        self.assertFragmentIn(response, {"results": ElementCount(0)})

    @classmethod
    def prepare_cpa_only_categories(cls):
        cls.index.models += [
            Model(hid=16044621, hyperid=31422, title='твикс 31422', vbid=60, vendor_id=334, datasource_id=1),
            Model(hid=16044621, hyperid=31423, title='твикс 31423', vbid=50, vendor_id=334, datasource_id=1),
            Model(hid=16044621, hyperid=31424, title='твикс 31424', vbid=40, vendor_id=334, datasource_id=1),
            Model(hid=16044621, hyperid=31425, title='твикс 31425', vbid=30, vendor_id=334, datasource_id=1),
            Model(hid=16044621, hyperid=31426, title='твикс 31426', vbid=20, vendor_id=334, datasource_id=1),
            Model(hid=16044621, hyperid=31427, title='твикс 31427', vbid=10, vendor_id=334, datasource_id=1),
            Model(hid=16044621, hyperid=31428, title='твикс 31428', vbid=10, vendor_id=334, datasource_id=1),
            Model(hid=16044621, hyperid=31429, title='твикс 31429', vbid=10, vendor_id=334, datasource_id=1),
        ]

        cls.index.shops += [
            Shop(fesh=311, priority_region=213, cpa=Shop.CPA_REAL, name='CPA Магазин в Москве #1'),
            Shop(fesh=312, priority_region=213, cpa=Shop.CPA_REAL, name='CPA Магазин в Москве #2'),
            Shop(fesh=313, priority_region=213, cpa=Shop.CPA_REAL, name='CPA Магазин в Москве #3'),
        ]

        cls.index.offers += [
            Offer(
                hyperid=31422,
                fesh=311,
                hid=16044621,
                ts=31101,
                cpa=Offer.CPA_REAL,
                price=511,
                title="твикс 31422",
                bid=500,
                fee=50,
            ),
            Offer(
                hyperid=31422,
                fesh=312,
                hid=16044621,
                ts=31102,
                cpa=Offer.CPA_REAL,
                price=511,
                title="твикс 31422",
                bid=480,
                fee=20,
            ),
            Offer(
                hyperid=31422,
                fesh=313,
                hid=16044621,
                ts=31103,
                cpa=Offer.CPA_REAL,
                price=511,
                title="твикс 31422",
                bid=10,
                fee=30,
            ),
            Offer(
                hyperid=31423,
                fesh=311,
                hid=16044621,
                ts=31201,
                cpa=Offer.CPA_REAL,
                price=530,
                title="твикс 31423",
                bid=10,
                fee=40,
            ),
            Offer(
                hyperid=31423,
                fesh=312,
                hid=16044621,
                ts=31202,
                cpa=Offer.CPA_REAL,
                price=530,
                title="твикс 31423",
                bid=450,
                fee=50,
            ),
            Offer(
                hyperid=31424,
                fesh=311,
                hid=16044621,
                ts=31111,
                cpa=Offer.CPA_REAL,
                price=540,
                title="твикс 31424",
                bid=500,
                fee=60,
            ),
            Offer(
                hyperid=31425,
                fesh=311,
                hid=16044621,
                ts=31111,
                cpa=Offer.CPA_REAL,
                price=550,
                title="твикс 31425",
                bid=300,
                fee=70,
            ),
            Offer(
                hyperid=31426,
                fesh=311,
                hid=16044621,
                ts=31111,
                cpa=Offer.CPA_REAL,
                price=560,
                title="твикс 31426",
                bid=800,
                fee=80,
            ),
            Offer(
                hyperid=31427,
                fesh=311,
                hid=16044621,
                ts=31111,
                cpa=Offer.CPA_REAL,
                price=570,
                title="твикс 31427",
                bid=800,
                fee=90,
            ),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 31101).respond(0.03)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 31102).respond(0.02)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 31103).respond(0.01)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 31201).respond(0.01)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 31202).respond(0.04)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 31111).respond(0.005)

    # проверяем, что модели из CPA-only категорий возвращаются
    # если стоит market_cpa_only_by_index или один из флагов, включающих CPA в плейсе
    def test_cpa_only_categories(self):
        response = self.report.request_json(
            'place=vendor_incut&hid=16044621&show-urls=productVendorBid,cpa'
            '&rearr-factors=market_vendor_incut_truthful_CPA_trait=1;market_vendor_incut_with_CPA_offers_only=0;market_cpa_only_by_index=0;market_vendor_incut_hide_undeliverable_models=0'
        )
        self.assertFragmentIn(response, {"results": ElementCount(6)})
        response = self.report.request_json(
            'place=vendor_incut&hid=16044621&show-urls=productVendorBid,cpa'
            '&rearr-factors=market_vendor_incut_truthful_CPA_trait=0;market_vendor_incut_with_CPA_offers_only=1;market_cpa_only_by_index=0;market_vendor_incut_hide_undeliverable_models=0'
        )
        self.assertFragmentIn(response, {"results": ElementCount(6)})
        response = self.report.request_json(
            'place=vendor_incut&hid=16044621&show-urls=productVendorBid,cpa'
            '&rearr-factors=market_vendor_incut_truthful_CPA_trait=0;market_vendor_incut_with_CPA_offers_only=0;market_cpa_only_by_index=1;market_vendor_incut_hide_undeliverable_models=0'
        )
        self.assertFragmentIn(response, {"results": ElementCount(6)})

    @classmethod
    def prepare_request_DO_with_CPA_offers_only_flag(cls):
        cls.index.models += [
            Model(hid=4401, hyperid=44811, vbid=25, vendor_id=682, datasource_id=1),
            Model(hid=4401, hyperid=44812, vbid=250, vendor_id=682, datasource_id=1),
        ]

        cls.index.shops += [
            Shop(
                fesh=411, priority_region=2, regions=[213], cpa=Shop.CPA_REAL, name='CPA Магазин в далеком Замкадье #1'
            ),
            Shop(fesh=412, priority_region=213, cpa=Shop.CPA_NO, name='CPC Магазин в Москве #2'),
        ]

        cls.index.offers += [
            Offer(hyperid=44811, fesh=411, hid=4401, ts=41101, cpa=Offer.CPA_REAL, price=511, bid=50, fee=5),
            Offer(hyperid=44811, fesh=412, hid=4401, ts=41102, cpa=Offer.CPA_NO, price=51, bid=480, fee=20),
            Offer(hyperid=44812, fesh=412, hid=4401, ts=41102, cpa=Offer.CPA_NO, price=51, bid=480, fee=20),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 41101).respond(0.03)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 41102).respond(0.06)

    def test_request_DO_with_CPA_offers_only_flag(self):
        """
        Проверяем выбор ДО в зависимости от market_vendor_incut_with_CPA_offers_only
        без флага получаем CPC оффер с большей ставкой
        """
        response = self.report.request_json(
            'place=vendor_incut&pp=18&hid=4401&show-urls=productVendorBid,cpa&rids=213&use-default-offers=1&'
            'rearr-factors=market_vendor_incut_min_size=1;market_vendor_incut_size=1;market_vendor_incut_with_CPA_offers_only=0;market_vendor_incut_hide_undeliverable_models=0'
        )
        self.assertFragmentIn(
            response,
            {'results': [{'offers': {'items': [{'urls': {'cpa': Absent()}, 'cpa': Absent(), 'shop': {'id': 412}}]}}]},
        )
        """
        с флагом ДО будет только CPA
        """
        response = self.report.request_json(
            'place=vendor_incut&pp=18&hid=4401&show-urls=productVendorBid,cpa&rids=213&use-default-offers=1&'
            'rearr-factors=market_vendor_incut_min_size=1;market_vendor_incut_size=1;market_vendor_incut_with_CPA_offers_only=1;market_vendor_incut_hide_undeliverable_models=0'
        )
        self.assertFragmentIn(
            response,
            {'results': [{'offers': {'items': [{'urls': {'cpa': NotEmpty()}, 'cpa': 'real', 'shop': {'id': 411}}]}}]},
        )

    @classmethod
    def prepare_vendor_incut_force_filter_adult_for_incuts(cls):
        cls.index.hypertree += [
            HyperCategory(hid=18540670, children=[HyperCategory(hid=16155466)])
        ]  # нехудожественная литература; книги о любви и эротике
        cls.index.offers += [
            Offer(
                fesh=866 + i,
                hyperid=866 + i,
                hid=16155466,
                fee=890 + i,
                price=100,
                cpa=Offer.CPA_REAL,
                adult=1,
                vendor_id=16155466,
                title="Пенетрылда {} см".format(i * 2 + 15),
            )
            for i in range(1, 10)
        ]

        cls.index.models += [
            Model(
                hyperid=866 + i,
                hid=16155466,
                vendor_id=866,
                vbid=100 + i,
                title="Пенетрылда {} см".format(i * 2 + 15),
            )
            for i in range(1, 10)
        ]

        cls.index.overall_models += [OverallModel(hyperid=866 + i, is_adult=True) for i in range(1, 10)]

    def test_vendor_incut_adult_filters(self):
        request = (
            "place=vendor_incut&pp=18&text=пенетрылда&adult=1&hid=18540670"
            "&rearr-factors=market_vendor_incut_size=5;market_vendor_incut_hide_undeliverable_models=0"
            ";market_force_filter_adult_for_incuts="
        )
        # ищем на более общей категории, не являющейся категорией для взрослых.
        # Если флаги отключены, то врезка с товарами для взрослых из дочерней категории должна собираться.
        # Если включён флаг показа врезок для взрослых только на взрослых категориях, то врезка не должна собираться

        # флаги фильтрации взрослой рекламы отключены
        response = self.report.request_json(request + "0;market_adult_incuts_on_adult_hids_only=0")
        self.assertFragmentIn(response, {'results': ElementCount(5)})

        # взрослая реклама показывается только во взрослых категориях (не в этой)
        response = self.report.request_json(request + "0;market_adult_incuts_on_adult_hids_only=1")
        self.assertFragmentIn(response, {'results': EmptyList()})

        # флаг фильтрации взрослой рекламы включён
        response = self.report.request_json(request + "1")
        self.assertFragmentIn(response, {'results': EmptyList()})

        # ищем на категории "книги о любви и эротике".
        # Врезки для взрослых должны собираться, только если их показ в принципе не запрещён
        request_adult_hid = (
            "place=vendor_incut&pp=18&text=пенетрылда&adult=1&hid=16155466"
            "&rearr-factors=market_vendor_incut_size=5;market_vendor_incut_hide_undeliverable_models=0"
            ";market_force_filter_adult_for_incuts="
        )
        response = self.report.request_json(request_adult_hid + "0;market_adult_incuts_on_adult_hids_only=1")
        self.assertFragmentIn(response, {'results': ElementCount(5)})

        # флаг фильтрации взрослой рекламы включён
        response = self.report.request_json(request_adult_hid + "1")
        self.assertFragmentIn(response, {'results': EmptyList()})

    @classmethod
    def prepare_intim_non_adult_incut(cls):
        cls.index.offers += [
            Offer(
                fesh=876 + i,
                hyperid=876 + i,
                hid=13744375,  # # CONDOMS_CATEG_ID
                fee=890 + i,
                price=100,
                cpa=Offer.CPA_REAL,
                vendor_id=876,
                title="Презервативы {}".format(i),
            )
            for i in range(1, 10)
        ]

        cls.index.models += [
            Model(
                hyperid=876 + i,
                hid=13744375,
                vendor_id=876,
                vbid=100 + i,
                title="Презервативы {}".format(i),
            )
            for i in range(1, 10)
        ]

    def test_intim_non_adult_incut(self):
        # запретим товары для взрослых в рекламе. Проверим, что врезка всё равно собирается
        request = (
            "place=vendor_incut&pp=18&text=презервативы&adult=1&hid=13744375"
            "&rearr-factors=market_vendor_incut_size=5;market_vendor_incut_hide_undeliverable_models=0"
            ";market_force_filter_adult_for_incuts=1"
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {'results': ElementCount(5)})

    @classmethod
    def prepare_vendor_incut_DO_without_delivery(cls):
        cls.index.regiontree += [Region(rid=57, name='Далекое замкадье')]

        cls.index.models += [
            Model(hid=5501, hyperid=54811, vbid=25, vendor_id=782, datasource_id=1, title='54811'),
            Model(hid=5501, hyperid=54812, vbid=250, vendor_id=782, datasource_id=1, title='54812'),
            Model(hid=5501, hyperid=54813, vbid=25, vendor_id=782, datasource_id=1, title='54813'),
        ]

        cls.index.shops += [
            Shop(fesh=511, priority_region=213, regions=[225], cpa=Shop.CPA_REAL, name='511'),
            Shop(fesh=512, priority_region=213, regions=[225], cpa=Shop.CPA_REAL, name='512'),
            Shop(fesh=513, priority_region=213, regions=[225], cpa=Shop.CPA_REAL, name='513'),
        ]

        cls.index.outlets += [
            Outlet(fesh=511, region=213, point_type=Outlet.FOR_PICKUP, point_id=511213),
            Outlet(fesh=511, region=2, point_type=Outlet.FOR_PICKUP, point_id=511002),
            Outlet(fesh=512, region=213, point_type=Outlet.FOR_PICKUP, point_id=512213),
            Outlet(fesh=511, region=57, point_type=Outlet.FOR_PICKUP, point_id=511057),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=50501,
                fesh=511,
                carriers=[99],
                options=[PickupOption(outlet_id=511213)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=50502,
                fesh=511,
                carriers=[99],
                options=[PickupOption(outlet_id=511002)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=50503,
                fesh=512,
                carriers=[99],
                options=[PickupOption(outlet_id=512213)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=50504,
                fesh=512,
                carriers=[99],
                options=[PickupOption(outlet_id=511057)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        cls.index.offers += [
            Offer(
                hyperid=54811,
                fesh=511,
                hid=5501,
                ts=51101,
                cpa=Offer.CPA_REAL,
                price=511,
                bid=50,
                fee=5,
                pickup_buckets=[50501, 50502],
                forbidden_regions=[57],
            ),
            Offer(
                hyperid=54812,
                fesh=512,
                hid=5501,
                ts=51102,
                cpa=Offer.CPA_REAL,
                price=51,
                bid=480,
                fee=20,
                pickup_buckets=[50503],
                forbidden_regions=[2, 57],
            ),
            Offer(
                hyperid=54813,
                fesh=513,
                hid=5501,
                ts=51101,
                cpa=Offer.CPA_REAL,
                price=511,
                bid=50,
                fee=5,
                pickup_buckets=[50501, 50502, 50504],
            ),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 51101).respond(0.03)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 51102).respond(0.06)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 51102).respond(0.04)

    # проверяем, что с включенным флагом во врезке не отображаются модели с CPA офферами,
    # которые не могут быть доставлены в запрошенный регион
    def test_vendor_incut_DO_without_delivery(self):
        request = (
            "place=vendor_incut&pp=18&hid=5501&show-urls=productVendorBid,cpa&use-default-offers=1&"
            "rearr-factors=market_vendor_incut_min_size=2;market_vendor_incut_size=3;market_vendor_incut_with_CPA_offers_only=1"
        )
        response = self.report.request_json(request + "&rids=213")
        self.assertFragmentIn(response, {'results': ElementCount(3)})
        response = self.report.request_json(request + ";market_vendor_incut_hide_undeliverable_models=0&rids=2")
        self.assertFragmentIn(response, {'results': ElementCount(3)})
        response = self.report.request_json(request + ";market_vendor_incut_hide_undeliverable_models=1&rids=2")
        self.assertFragmentIn(response, {'results': ElementCount(2)})
        response = self.report.request_json(
            request + ";market_vendor_incut_hide_undeliverable_models=1&rids=57&debug=1"
        )
        self.assertFragmentIn(
            response,
            {
                'results': EmptyList(),
                'debug': {
                    'report': {
                        'logicTrace': [
                            Contains("vendor_incut.cpp", "Output()", "Vendor won auction with less deliverable models")
                        ],
                    }
                },
            },
        )

    @classmethod
    def prepare_vendor_banner_id_for_incut_items(cls):
        cls.index.models += [
            Model(
                hid=717,
                hyperid=1722,
                title='with banner',
                vbid=10,
                vendor_id=734,
                datasource_id=1,
            ),
        ]

        cls.index.offers += [
            Offer(hid=717, hyperid=1722, vendor_id=734, cpa=Offer.CPA_REAL),
        ]

        cls.index.vendors_banners += [VendorBanner(1, 734, 717, 71, 800)]

    def test_vendor_banner_id_for_incut_items(self):
        """
        Проверяем, что vendor_banner_id есть в урлах и логах для оффера и модели
        """
        response = self.report.request_json(
            'place=vendor_incut&hid=717&debug=1&show-urls=productVendorBid,cpa&use-default-offers=1'
            '&rearr-factors=market_vendor_incut_min_size=1;market_vendor_incut_size=5;market_vendor_incut_truthful_CPA_trait=0;market_vendor_incut_enable_banners=1;market_vendor_incut_hide_undeliverable_models=0'  # noqa
        )

        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'vendorBanner',
                        'vendorId': 734,
                        'bannerId': 71,
                    },
                    {
                        'entity': 'product',
                        'vendor': {'id': 734},
                        'urls': {
                            'encrypted': Contains("vendor_banner_id=71"),
                        },
                        'offers': {
                            'count': 1,
                            'items': [
                                {
                                    'entity': 'offer',
                                    'urls': {
                                        'cpa': Contains("vendor_banner_id=71"),
                                    },
                                }
                            ],
                        },
                    },
                ]
            },
            allow_different_len=True,
        )

        self.show_log.expect(
            url_type=UrlType.VENDOR_BANNER,
            url='www.dummy.com',
            vendor_banner_id=71,
        )
        self.show_log.expect(
            url_type=UrlType.MODEL,
            hyper_id=1722,
            hyper_cat_id=717,
            vendor_banner_id=71,
        )
        self.show_log.expect(
            url_type=UrlType.CPA,
            hyper_id=1722,
            hyper_cat_id=717,
            vendor_banner_id=71,
            cpa=1,
        )

        self.click_log.expect(
            clicktype=ClickType.EXTERNAL,
            dtype='modelcard',
            url_type=UrlType.VENDOR_BANNER,
            url='www.dummy.com',
            hyper_id=-1,
            vendor_banner_id=71,
        )
        self.click_log.expect(
            clicktype=ClickType.EXTERNAL,
            dtype='modelcard',
            url_type=UrlType.MODEL,
            hyper_id=1722,
            hyper_cat_id=717,
            vendor_banner_id=71,
        )
        self.click_log.expect(
            clicktype=ClickType.CPA,
            url_type=UrlType.CPA,
            hyper_id=1722,
            hyper_cat_id=717,
            vendor_banner_id=71,
            cpa=1,
        )

    @classmethod
    def prepare_logs_from_default_offer(cls):
        cls.index.shops += [
            Shop(fesh=611, priority_region=213, regions=[225], cpa=Shop.CPA_REAL),  # DSBS
            Shop(
                fesh=612,
                datafeed_id=612,
                priority_region=213,
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                fulfillment_virtual=True,
                cpa=Shop.CPA_REAL,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
            ),  # virtual shop
            Shop(
                fesh=613,
                datafeed_id=613,
                priority_region=213,
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.THIRD_PARTY,
                fulfillment_program=True,
                blue=Shop.BLUE_REAL,
            ),  # supplier
        ]

        cls.index.models += [
            Model(hid=5601, hyperid=56001, vbid=300, vendor_id=782, datasource_id=1),
            Model(hid=5601, hyperid=56002, vbid=200, vendor_id=782, datasource_id=1),
        ]

        cls.index.mskus += [
            MarketSku(
                hyperid=56002,
                sku=56002,
                blue_offers=[
                    BlueOffer(
                        feedid=613,
                    )
                ],
            ),
        ]

        cls.index.offers += [
            Offer(hyperid=56001, fesh=611, cpa=Offer.CPA_REAL),
        ]

    def test_logs_from_default_offer(self):
        """
        Проверяем, что в вендлорские клики и показы пишутся shop_id и supplier_id из ДО
        """
        _ = self.report.request_json(
            'place=vendor_incut&hid=5601&show-urls=productVendorBid,cpa&use-default-offers=1'
            '&rearr-factors=market_vendor_incut_min_size=1;market_vendor_incut_size=5'
        )

        self.click_log.expect(
            clicktype=ClickType.EXTERNAL, dtype='modelcard', hyper_id=56001, shop_id=611, supplier_id=611
        )
        self.click_log.expect(
            clicktype=ClickType.EXTERNAL,
            dtype='modelcard',
            hyper_id=56002,
            shop_id=612,
            supplier_id=613,
            supplier_type=Shop.THIRD_PARTY,
        )

        self.show_log.expect(hyper_id=56001, url_type=16, original_shop_id=611, supplier_id=611)
        self.show_log.expect(
            hyper_id=56002, url_type=16, original_shop_id=612, supplier_id=613, supplier_type=Shop.THIRD_PARTY
        )

    @classmethod
    def prepare_incut_blacklist(cls):
        cls.index.models += [
            Model(hid=22, hyperid=4, vbid=1, vendor_id=41, datasource_id=1, title='Apple Watch 7'),
            Model(hid=22, hyperid=5, vbid=11, vendor_id=41, datasource_id=1, title='Apple Watch 7 GPS'),
            Model(hid=22, hyperid=6, vbid=12, vendor_id=41, datasource_id=1, title='Apple Watch 7 Something'),
            Model(hid=22, hyperid=7, vbid=21, vendor_id=41, datasource_id=1, title='Apple Watch 7 Other'),
        ]
        cls.index.offers += [
            Offer(hyperid=4, title='Умные часы Apple Watch Series 7 41mm Aluminium with Sport Band, темная ночь, R'),
            Offer(hyperid=5, title='Apple Watch Series 7 GPS 45mm Aluminum Case with Sport Band (Зеленый) (MKN73) RU'),
            Offer(hyperid=6, title='Apple Watch 7 Something great'),
            Offer(hyperid=7, title='Apple Watch 7 Other Device'),
        ]
        cls.index.incut_black_list_fb += [IncutBlackListFb(texts=['apple watch 7'], inclids=['PremiumAds'])]

    def test_incut_blacklist(self):
        """
        Проверяем, что врезка пустая при попадании запроса в блэк-лист
        """
        response = self.report.request_json(
            'place=vendor_incut&text=Apple%20Watch%207&'
            'rearr-factors=market_vendor_incut_min_size=0;'
            'market_vendor_incut_with_CPA_offers_only=0;'
            'market_vendor_incut_hide_undeliverable_models=0;'
            'market_output_advert_request_blacklist_fb=0'
            '&hid=22'
        )
        self.assertFragmentIn(response, {'results': [{'entity': 'product'}]})
        response_using_blacklist = self.report.request_json(
            'place=vendor_incut&text=Apple%20Watch%207&'
            + 'rearr-factors=market_vendor_incut_min_size=0;'
            + 'market_vendor_incut_with_CPA_offers_only=0;'
            + 'market_vendor_incut_hide_undeliverable_models=0;'
            + 'market_output_advert_request_blacklist_fb=1'
        )
        self.assertFragmentIn(response_using_blacklist, {'results': ElementCount(0)})

    @classmethod
    def prepare_incut_blacklist_hid(cls):
        cls.index.models += [
            Model(hid=23, hyperid=224, vbid=1, vendor_id=41, datasource_id=1),
            Model(hid=23, hyperid=225, vbid=11, vendor_id=41, datasource_id=1),
            Model(hid=23, hyperid=226, vbid=12, vendor_id=41, datasource_id=1),
            Model(hid=23, hyperid=227, vbid=21, vendor_id=41, datasource_id=1),
        ]
        cls.index.offers += [
            Offer(hyperid=224),
            Offer(hyperid=225),
            Offer(hyperid=226),
            Offer(hyperid=227),
        ]
        cls.index.incut_black_list_fb += [IncutBlackListFb(subtreeHids=[23], inclids=['VendorIncut'])]

    def test_incut_blacklist_hid(self):
        """
        Проверяем, что врезка пустая при попадании hid в блэк-лист
        """
        response = self.report.request_json(
            'place=vendor_incut&hid=23&'
            'rearr-factors=market_vendor_incut_min_size=0;'
            'market_vendor_incut_with_CPA_offers_only=0;'
            'market_vendor_incut_hide_undeliverable_models=0;'
            'market_output_advert_request_blacklist_fb=0'
        )
        self.assertFragmentIn(response, {'results': [{'entity': 'product'}]})
        response_using_blacklist = self.report.request_json(
            'place=vendor_incut&hid=23&'
            'rearr-factors=market_vendor_incut_min_size=0;'
            'market_vendor_incut_with_CPA_offers_only=0;'
            'market_vendor_incut_hide_undeliverable_models=0;'
            'market_output_advert_request_blacklist_fb=1'
        )
        self.assertFragmentIn(response_using_blacklist, {'results': ElementCount(0)})

    @classmethod
    def prepare_fashion_blacklist(cls):
        cls.index.hypertree += [
            HyperCategory(
                hid=7877999,
                name='Одежда, обувь и аксессуары',
            ),
        ]
        cls.index.incut_black_list_fb += [
            IncutBlackListFb(subtreeHids=[7877999], inclids=['PremiumAds', 'VendorIncut'])
        ]
        cls.index.fashion_categories += [
            FashionCategory("CATEGORY_COMMON_FASHION", 7877999),
        ]
        cls.index.models += [
            Model(
                hid=7877999,
                hyperid=4010,
                ts=610,
                title='model_10_fashion',
                vbid=11,
            ),
        ]
        cls.index.mskus += [
            MarketSku(
                title="model_4010_msku_1_fashion",
                hid=7877999,
                hyperid=4010,
                sku=120010,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(
                        price=1600,
                        feedid=803,
                        fee=200,
                        waremd5="BLUE-120010-FEED-0001Q",
                        title="model_4010 3P buybox offer fashion",
                        ts=8017,
                    ),
                ],
            ),
        ]

    def test_fashion_blacklist(self):
        """
        Проверяем работу флага market_premium_ads_gallery_no_blacklist_for_fashion, который выключает blacklist для fashion
        """
        request_base_cpa_shop_incut = "place=vendor_incut&hid=7877999&&rearr-factors="
        rearr_flags_dict = {
            "market_vendor_incut_min_size": 0,
            "market_vendor_incut_with_CPA_offers_only": 0,
            "market_vendor_incut_hide_undeliverable_models": 0,
            "market_output_advert_request_blacklist_fb": 1,
        }
        # Сначала ожидаем пустой ответ, так как категория fashion в blacklist
        response = self.report.request_json(request_base_cpa_shop_incut + dict_to_rearr(rearr_flags_dict))
        self.assertFragmentIn(
            response,
            {
                "results": EmptyList(),
            },
        )
        # Выставляем флаг, который отключит blacklist на категорию fashion, и врезка соберётся
        rearr_flags_dict["market_premium_ads_gallery_no_blacklist_for_fashion"] = 1
        response = self.report.request_json(request_base_cpa_shop_incut + dict_to_rearr(rearr_flags_dict))
        self.assertFragmentIn(
            response,
            {
                "results": ElementCount(1),
            },
        )


if __name__ == '__main__':
    main()
