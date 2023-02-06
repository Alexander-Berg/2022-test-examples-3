#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from unittest import skip

from core.types import GLParam, MarketSku, MnPlace, Offer, Region, Shop
from core.matcher import GreaterFloat, LessFloat, Round
from core.types.offer import OfferDimensions
from core.testcase import TestCase, main


class _Constants:
    russia_rids = 225
    moscow_rids = 213

    model_id = 1000
    category_id = 2000

    class _Partners:
        fesh = 2
        feed_id = 2


OFFER_COUNT = 6


class _Shops:
    @classmethod
    def gen_shops(clz):
        """Генерируем OFFER_COUNT разных cpa-магазина, чтобы у каждого оффера был свой и все офферы попали в мета-ранжирование"""
        shops = []
        for shop_num in range(OFFER_COUNT):
            plain_shop = Shop(
                fesh=_Constants._Partners.fesh + shop_num,
                datafeed_id=_Constants._Partners.feed_id + shop_num,
                priority_region=_Constants.moscow_rids,
                regions=[_Constants.moscow_rids],
                cpa=Shop.CPA_REAL,
            )
            shops.append(plain_shop)
        return shops


class _Offers:
    @classmethod
    def gen_offers(clz):
        """Генерируем OFFER_COUNT обычных cpa-офферов, оффер с индексом 3 будет пессимизирован, с индексом 2 - получит буст"""
        mskus = []
        offers = []
        for offer_num in range(OFFER_COUNT):
            hid = _Constants.category_id + (offer_num // (OFFER_COUNT / 2))

            white_msku = MarketSku(
                title=("Белый оффер %d" % offer_num),
                hyperid=_Constants.model_id + offer_num,
                hid=hid,
                sku=10 + offer_num,
            )
            mskus.append(white_msku)
            glparams = []
            if offer_num == 3:  # Плохое качество заполнения (content_quality = 0.0)
                glparams = [
                    GLParam(param_id=27477630, value=0),
                ]
            elif offer_num == 2:  # Новинка (hype_offer = true)
                glparams = [
                    GLParam(param_id=27625090, value=1),
                ]

            white_offer = Offer(
                waremd5=('white_offer_%02d___wwwww' % offer_num),
                hyperid=white_msku.hyperid,
                hid=hid,
                sku=white_msku.sku,
                fesh=_Constants._Partners.fesh + offer_num,
                price=30,
                weight=5,
                title=white_msku.title,
                dimensions=OfferDimensions(length=30, width=30, height=30),
                is_express=False,
                glparams=glparams,
                cpa=Offer.CPA_REAL,
            )
            offers.append(white_offer)
        return mskus, offers


class _Requests:
    # cpa=real избавляется от несхлопнутых моделей
    prime_request = (
        'place=prime'
        '&pp=18'
        '&cpa=real'
        '&rids=' + str(_Constants.moscow_rids) + '&local-offers-first=0'
        '&regset=2'
        '&debug=1'
    )
    basic_rearr = (
        '&rearr-factors='
        'market_pessimize_content_quality_coeff_text=10.0;'
        'market_pessimize_content_quality_coeff_textless=5.0;'
        'market_boost_hype_offer_coeff_text=10.0;'
        'market_boost_hype_offer_coeff_textless=5.0'
    )
    rearr_with_hype_hids = basic_rearr + ';market_boost_hype_offer_hids=%d'


class T(TestCase):
    @classmethod
    def prepare_mxnet(cls):
        cls.matrixnet.on_default_place(MnPlace.BASE_SEARCH).respond(0.3)
        cls.matrixnet.on_default_place(MnPlace.META_REARRANGE).respond(0.3)

    @classmethod
    def prepare_regions(cls):
        cls.index.regiontree += [Region(rid=_Constants.moscow_rids, name="Москва", tz_offset=10800)]

    @classmethod
    def prepare_shops(cls):
        cls.index.shops += _Shops.gen_shops()

    @classmethod
    def prepare_offers(cls):
        mskus, offers = _Offers.gen_offers()

        cls.index.mskus += mskus
        cls.index.offers += offers

    def build_pessimized_result(self, count, explicit_offers):
        """Собираем результат поиска из обычных офферов и офферов, переданных в explicit_offers.
        Суммарная длина = count"""
        pre_offers = [
            {
                'trace': {
                    'fullFormulaInfo': [
                        {'tag': 'Default', 'value': GreaterFloat(0.29)},
                    ],
                },
            }
        ] * (count - len(explicit_offers))
        return {'search': {'results': pre_offers + explicit_offers}}

    def build_boosted_result(self, count, explicit_offers):
        """Собираем результат поиска из офферов, переданных в explicit_offers и
        обычных офферов. Суммарная длина = count"""
        post_offers = [
            {
                'trace': {
                    'fullFormulaInfo': [
                        {'tag': 'Default', 'value': LessFloat(0.31)},
                    ],
                },
            }
        ] * (count - len(explicit_offers))
        return {'search': {'results': explicit_offers + post_offers}}

    @skip('deleted old booster')
    def test_pessimize_content_for_text_search(self):
        """GL-параметр 27477630 с числовым значением 0 должен приводить к пессимизации оффера
        множителем к формулам ранжирования."""

        # Явно задаем пессимизацию x10 (в результате значение формулы умножается на 0.1)
        response = self.report.request_json(
            _Requests.prime_request + _Requests.basic_rearr + '&text=оффер&allow-collapsing=0'
        )

        self.assertFragmentIn(
            response,
            self.build_pessimized_result(
                OFFER_COUNT,
                [
                    {
                        'entity': 'offer',
                        'trace': {
                            'fullFormulaInfo': [
                                {'tag': 'Default', 'value': '0.03'},
                                {'tag': 'Meta', 'value': '0.3'},
                            ],
                        },
                        'debug': {
                            'properties': {
                                'BOOST_MULTIPLIER': Round('0.1'),
                                'CPM': '3000',
                            },
                            'metaProperties': {
                                'BOOST_MULTIPLIER': Round('0.1'),
                                'CPM': '3000',
                            },
                        },
                    }
                ],
            ),
            preserve_order=True,
            allow_different_len=True,
        )

        # По умолчанию тоже пессимизируем, но с коэффициентом x1.2
        response = self.report.request_json(_Requests.prime_request + '&text=оффер&allow-collapsing=0')
        self.assertFragmentIn(
            response,
            self.build_pessimized_result(
                OFFER_COUNT,
                [
                    {
                        'entity': 'offer',
                        'trace': {
                            'fullFormulaInfo': [
                                {'tag': 'Default', 'value': '0.25'},
                                {'tag': 'Meta', 'value': '0.3'},
                            ],
                        },
                        'debug': {
                            'properties': {
                                'BOOST_MULTIPLIER': Round('0.8333'),
                                'CPM': '25000',
                            },
                            'metaProperties': {
                                'BOOST_MULTIPLIER': Round('0.8333'),
                                'CPM': '25000',
                            },
                        },
                    }
                ],
            ),
            preserve_order=True,
            allow_different_len=True,
        )

    @skip('deleted old booster')
    def test_pessimize_content_for_textless_search(self):
        """GL-параметр 27477630 с числовым значением 0 должен приводить к пессимизации оффера
        множителем к формулам ранжирования (бестекстовый запрос)"""

        # Явно задаем пессимизацию x5 (в результате значение формулы умножается на 0.2)
        response = self.report.request_json(
            _Requests.prime_request + _Requests.basic_rearr + '&hid=2000,2001&allow-collapsing=0'
        )

        self.assertFragmentIn(
            response,
            self.build_pessimized_result(
                OFFER_COUNT,
                [
                    {
                        'entity': 'offer',
                        'trace': {
                            'fullFormulaInfo': [
                                {'tag': 'Default', 'value': '0.06'},
                            ],
                        },
                        'debug': {
                            'properties': {
                                'BOOST_MULTIPLIER': Round('0.2'),
                                'CPM': '6000',
                            },
                        },
                    }
                ],
            ),
            preserve_order=True,
            allow_different_len=True,
        )

        # Схлопывание включено. Величина буста не вычисляется на мете, т.к. бестекст.
        # Просто ищем на последнем месте нужную модель
        response = self.report.request_json(
            _Requests.prime_request + _Requests.basic_rearr + '&hid=2000,2001&allow-collapsing=1'
        )
        self.assertFragmentIn(
            response,
            self.build_pessimized_result(OFFER_COUNT, [{'debug': {'wareId': 'white_offer_03___wwwww'}}]),
            preserve_order=True,
            allow_different_len=True,
        )

        # По умолчанию тоже пессимизируем, но с коэффициентом x1.2
        response = self.report.request_json(_Requests.prime_request + '&hid=2000,2001&allow-collapsing=0')

        self.assertFragmentIn(
            response,
            self.build_pessimized_result(
                OFFER_COUNT,
                [
                    {
                        'entity': 'offer',
                        'trace': {
                            'fullFormulaInfo': [
                                {'tag': 'Default', 'value': '0.25'},
                            ],
                        },
                        'debug': {
                            'properties': {
                                'BOOST_MULTIPLIER': Round('0.83333333'),
                                'CPM': '25000',
                            },
                        },
                    }
                ],
            ),
            preserve_order=True,
            allow_different_len=True,
        )

    @skip('deleted old booster')
    def test_boost_fresh_offer_for_text_search(self):
        """GL-параметр 27625090 с bool-значением true должен давать офферу буст. Коэффициент
        и фильтр по hid-у задаются параметрами rearr-factors."""
        first_doc_boosted_result = self.build_boosted_result(
            OFFER_COUNT,
            [
                {
                    'trace': {
                        'fullFormulaInfo': [
                            {'tag': 'Default', 'value': Round('3.0')},
                            {'tag': 'Meta', 'value': Round('0.3')},
                        ],
                    },
                    'debug': {
                        'properties': {
                            'BOOST_MULTIPLIER': Round('10.0'),
                            'CPM': '300000',
                        },
                        'metaProperties': {
                            'BOOST_MULTIPLIER': Round('10.0'),
                            'CPM': '300000',
                        },
                    },
                }
            ],
        )

        # Явно заданный буст x10 для всех hid-ов
        response = self.report.request_json(
            _Requests.prime_request + _Requests.basic_rearr + '&text=оффер&allow-collapsing=0'
        )

        # Аналогичный результат должен получаться и для схлопнутых моделей
        response = self.report.request_json(
            _Requests.prime_request + _Requests.basic_rearr + '&text=оффер&allow-collapsing=1'
        )

        self.assertFragmentIn(response, first_doc_boosted_result, preserve_order=True, allow_different_len=True)

        # Фильтр буста по hid. Наш оффер с бустом должен пройти.
        response = self.report.request_json(
            _Requests.prime_request + (_Requests.rearr_with_hype_hids % 2000) + '&text=оффер&allow-collapsing=0'
        )

        # Фильтр буста по hid. Наш оффер с бустом должен пройти.
        response = self.report.request_json(
            _Requests.prime_request + (_Requests.rearr_with_hype_hids % 2001) + '&text=оффер&allow-collapsing=0'
        )

        # Фильтр буста по другому hid. У оффера есть gl-параметр, но он не должен получить буст.
        response = self.report.request_json(
            _Requests.prime_request + (_Requests.rearr_with_hype_hids % 2001) + '&text=оффер&allow-collapsing=0'
        )

        self.assertFragmentIn(
            response, self.build_boosted_result(OFFER_COUNT, []), preserve_order=True, allow_different_len=True
        )

        self.assertFragmentIn(response, self.build_boosted_result(4, []), preserve_order=True, allow_different_len=True)

    @skip('deleted old booster')
    def test_boost_fresh_offer_for_textless_search(self):
        """Аналогично предыдущему кейсу (буст gl 27625090), но для бестекстового поиска"""

        # Коэффициент для бестекста в примере = x5
        # Но на конкретное значение не смотрим, т.к. в бестексте на мете моделям не выставляется дебажный boost_multiplier
        # Проверяем, что нужный схлопнутый оффер на первом месте
        first_doc_boosted_result = self.build_boosted_result(
            OFFER_COUNT, [{'debug': {'wareId': 'white_offer_02___wwwww'}}]
        )

        response = self.report.request_json(
            _Requests.prime_request + _Requests.basic_rearr + '&hid=2000,2001&allow-collapsing=0'
        )

        # Буст должен передаваться схлопнутым моделям
        response = self.report.request_json(
            _Requests.prime_request + _Requests.basic_rearr + '&hid=2000,2001&allow-collapsing=1'
        )

        self.assertFragmentIn(response, first_doc_boosted_result, preserve_order=True, allow_different_len=True)


if __name__ == '__main__':
    main()
