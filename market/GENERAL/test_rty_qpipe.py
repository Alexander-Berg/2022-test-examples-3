#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

import time
import struct
from datetime import datetime, timedelta
from market.idx.pylibrary.offer_flags.flags import DisabledFlags
from core.types.autostrategy import AutostrategyType

from core.matcher import Absent, NotEmpty, Contains
from core.testcase import (
    TestCase,
    main,
)
from core.types.taxes import (
    Vat,
    Tax,
)

from core.types import (
    BlueOffer,
    CpaCategory,
    Currency,
    DynamicOfferNew,
    DynamicSkuOffer,
    MarketSku,
    Offer,
    Outlet,
    RtyOffer,
    Shop,
)


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ["market_money_disable_bids=0"]
        # cls.settings.enable_panther = False
        cls.settings.rty_qpipe = True
        cls.settings.report_subrole = 'main'

        cls.index.offers += [
            Offer(title='iphone', feedid=25, offerid='fff', price=300),
            Offer(title='android', feedid=26, offerid='fff', price=300),
            Offer(title='windowsphone', feedid=27, offerid='fff', price=300),
        ]
        cls.index.creation_time = int(time.time()) // 60 * 60

    def _check_offer(self, text, price, vat=None, pricefrom=False, rgb=None):
        request = 'place=prime&text={}&rearr-factors=rty_qpipe=1'.format(text)
        if rgb:
            request += '&rgb={}'.format(rgb)
        response = self.report.request_json(request)

        price_value_field = 'value' if not pricefrom else 'min'
        self.assertFragmentIn(response, {'prices': {'currency': 'RUR', price_value_field: str(price)}})

        if vat:
            self.assertFragmentIn(response, {'vat': str(Vat(vat))})
        else:
            self.assertFragmentNotIn(response, {'vat': NotEmpty()})

    def _timestamp_to_string(self, ts):
        return datetime.utcfromtimestamp(ts).strftime("%Y-%m-%dT%H:%M:%SZ")

    def test_original_price_outside_exp(self):
        """
        Проверяем, что старое поведение сохраняется: цены читаются как и читались
        """
        self._check_offer('windowsphone', 300)

    def test_changed_price(self):
        """
        Проверяем, что в эксперименте подменяются цены в индексе и что пишется статистика
        """
        self._check_offer('iphone', 300)

        self.rty.offers += [RtyOffer(feedid=25, offerid='fff', price=400)]

        # проверяем цену в in-memory индексе
        self._check_offer('iphone', 400)

        self.rty.flush()

        # проверяем цену в final индексе
        self._check_offer('iphone', 400)

        # проверяем статистику голована
        # (точных чисел проверить нельзя, т.к. порядок запуска тестов недетерминирован)
        time.sleep(1)
        tass_data = self.base_search_client.request_tass()
        for signal in [
            "qprice_rty_count_dmmm",
            "qprice_rty_fail_count_dmmm",
            "qprice_legacy_count_dmmm",
            "qprice_fallback_count_dmmm",
            "backend-index-CTYPE-200_dmmm",
            "index-disk-docs_avvv",
        ]:
            self.assertIn(signal, tass_data)

    def test_reindex(self):
        """
        Проверяем, что при переиндексациях документа все остается как надо
        """
        self._check_offer('android', 300)

        def reindex_and_check(self):
            self.rty.offers += [RtyOffer(feedid=26, offerid='fff', price=400)]
            self._check_offer('android', 400)

        # первая индексация
        reindex_and_check(self)
        # переиндексация в memory
        reindex_and_check(self)

        reindex_and_check(self)

    @classmethod
    def prepare_other_qfields(cls):
        cls.index.shops += [
            Shop(
                fesh=1,
                priority_region=213,
                name='test_shop',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
            ),
        ]

        cls.index.outlets += [
            Outlet(fesh=3, region=213),
        ]

        cls.index.offers += [
            Offer(fesh=1, title='OfferWithAddVat', feedid=28, offerid='OfferWithAddVat', price=400),
            Offer(
                fesh=1,
                title='OfferWithChangeVat',
                feedid=28,
                offerid='OfferWithChangeVat',
                price=410,
                vat=Vat.VAT_18_118,
            ),
            Offer(
                fesh=1,
                title='OfferWithChangeVatSamePrice',
                feedid=28,
                offerid='OfferWithChangeVatSamePrice',
                price=415,
                vat=Vat.VAT_18_118,
            ),
            Offer(
                fesh=1, title='OfferWithRemoveVat', feedid=28, offerid='OfferWithRemoveVat', price=420, vat=Vat.VAT_18
            ),
            Offer(fesh=1, title='OfferWithAddPriceFrom', feedid=28, offerid='OfferWithAddPriceFrom', price=440),
            Offer(
                fesh=1,
                title='OfferWithAddPriceFromSamePrice',
                feedid=28,
                offerid='OfferWithAddPriceFromSamePrice',
                price=445,
            ),
            Offer(
                fesh=1,
                title='OfferWithRemovePriceFrom',
                feedid=28,
                offerid='OfferWithRemovePriceFrom',
                price=450,
                pricefrom=True,
            ),
            Offer(fesh=1, title='OfferCombinedChangeOne', feedid=28, offerid='OfferCombinedChangeOne', price=461),
            Offer(
                fesh=1,
                title='OfferCombinedChangeTwo',
                feedid=28,
                offerid='OfferCombinedChangeTwo',
                price=462,
                vat=Vat.VAT_18,
            ),
            Offer(
                fesh=1,
                title='OfferCombinedChangeThree',
                feedid=28,
                offerid='OfferCombinedChangeThree',
                price=463,
                pricefrom=True,
            ),
            Offer(
                fesh=1,
                title='OfferCombinedChangeFour',
                feedid=28,
                offerid='OfferCombinedChangeFour',
                price=464,
                pricefrom=True,
                vat=Vat.VAT_18,
            ),
        ]

    def test_other_qfields(self):
        """
        Отдельно проверяем корректную работу других полей быстрого пайплайна.
        На текущий момент это флаг price_from.

        Включать в основные тесты qpipe и увеличивать количество комбинаций не обязательно,
        т.к. данные быстро-пайплайна ездят вместе с ценами как единое целое.
        """

        # Проверяем, что данные нормально читаются из поколения, когда в rty этих оферов ещё нет
        self._check_offer('OfferWithAddVat', 400, vat=None)
        self._check_offer('OfferWithChangeVat', 410, vat=Vat.VAT_20_120)
        self._check_offer('OfferWithChangeVatSamePrice', 415, vat=Vat.VAT_20_120)
        self._check_offer('OfferWithRemoveVat', 420, vat=Vat.VAT_20)
        self._check_offer('OfferWithAddPriceFrom', 440, pricefrom=False)
        self._check_offer('OfferWithAddPriceFromSamePrice', 445, pricefrom=False)
        self._check_offer('OfferWithRemovePriceFrom', 450, pricefrom=True)
        self._check_offer('OfferCombinedChangeOne', 461)
        self._check_offer('OfferCombinedChangeTwo', 462, vat=Vat.VAT_20)
        self._check_offer('OfferCombinedChangeThree', 463, pricefrom=True)
        self._check_offer('OfferCombinedChangeFour', 464, vat=Vat.VAT_20, pricefrom=True)

        # Делаем изменения через rty
        self.rty.offers += [
            RtyOffer(feedid=28, offerid='OfferWithAddPriceFrom', price=540, pricefrom=True),
            RtyOffer(feedid=28, offerid='OfferWithAddPriceFromSamePrice', price=445, pricefrom=True),
            RtyOffer(feedid=28, offerid='OfferWithRemovePriceFrom', price=550, pricefrom=False),
        ]

        # Проверяем изменения через rty
        # для VAT ничего не меняется
        self._check_offer('OfferWithAddPriceFrom', 540, pricefrom=True)
        self._check_offer('OfferWithAddPriceFromSamePrice', 445, pricefrom=True)
        self._check_offer('OfferWithRemovePriceFrom', 550, pricefrom=False)

    @classmethod
    def prepare_qbids(cls):
        cls.index.offers += [
            Offer(hid=10, bid=10, title='bid-offer 1', fesh=1001, feedid=1, offerid='a'),
            Offer(hid=10, bid=40, title='bid-offer 2', fesh=1001, feedid=1, offerid='b'),
        ]

        cls.index.cpa_categories += [CpaCategory(hid=10)]

    def test_qbids(self):
        """
        Тестируем доставку ставок по rty:
        1) запрашиваем до обновления -- ожидаем индексные цены
        2) запрашиваем после обновления -- ожидаем подмерженные rty-цены
           - обновляем в первом оффере все ставки
           - обновляем во втором оффере только fee, больше не обновляем :) выпилили fee, но нули в принт доке должны приходить ибо legacy
           чтобы была меньше минимальной (тестируем подтягивание до минимума)
        3) запрашиваем без эксперимента -- ожидаем индексные цены
        """

        # до обновления -- ожидаем старые ставки
        response = self.report.request_json(
            'place=print_doc&text=bid-offer&rearr-factors=rty_qpipe=1;rty_qbids=1;market_ranging_cpa_by_ue_in_top=1&req_attrs=cbid,fee'
        )

        legacy_bids = {
            'documents': [
                {
                    'doc_type': 'offer',
                    'title': 'bid-offer 1',
                    'properties': {
                        'cbid': '10',
                        'fee': '0',
                    },
                },
                {
                    'doc_type': 'offer',
                    'title': 'bid-offer 2',
                    'properties': {
                        'cbid': '40',
                        'fee': '0',
                    },
                },
            ]
        }

        autostrategyBundle = [
            18,
            struct.unpack('<i', bytearray([AutostrategyType.CPA, 11, 0, 0]))[0],
            struct.unpack('<i', bytearray([AutostrategyType.CPA, 11, 0, 0]))[0],
        ]

        self.assertFragmentIn(response, legacy_bids)

        rty_timestamp = self.index.creation_time + 1
        self.rty.offers += [
            RtyOffer(feedid=1, offerid='a', bid_and_flags=100, bid_and_flags_ts=rty_timestamp),
            RtyOffer(feedid=1, offerid='b', fee_ts=rty_timestamp, amore_data=autostrategyBundle),
        ]

        # после обновления для rty-ставок -- ожидаем замену ставок для первого оффера, замену fee (без подтягиванием
        # до минимума) и старые ставки для второго оффера
        response = self.report.request_json(
            'place=print_doc&text=bid-offer&rearr-factors=rty_qpipe=1;rty_qbids=1;market_ranging_cpa_by_ue_in_top=1&req_attrs=cbid,fee'
        )

        self.assertFragmentIn(
            response,
            {
                'documents': [
                    {
                        'doc_type': 'offer',
                        'title': 'bid-offer 1',
                        'properties': {
                            'cbid': '100',
                        },
                    },
                    {
                        'doc_type': 'offer',
                        'title': 'bid-offer 2',
                        'properties': {
                            'cbid': '40',
                        },
                    },
                ]
            },
        )

        # после обновления для legacy-ставок -- ожидаем старые ставки
        response = self.report.request_json(
            'place=print_doc&text=bid-offer&rearr-factors=rty_qpipe=1;rty_qbids=0;market_ranging_cpa_by_ue_in_top=1&req_attrs=cbid,fee'
        )

        self.assertFragmentIn(response, legacy_bids)

        # спрашиваем репорт в легаси-режиме (без rty) -- ожидаем увидеть старые ставки
        response = self.report.request_json(
            'place=print_doc&text=bid-offer&rearr-factors=rty_qpipe=0;rty_qbids=0;market_ranging_cpa_by_ue_in_top=1&req_attrs=cbid,fee'
        )
        self.assertFragmentIn(response, legacy_bids)

    @classmethod
    def prepare_offer_dynamics(cls):
        cls.index.offers += [
            Offer(title='disabled-offer 1', fesh=39, feedid=5, offerid='1'),
            Offer(title='disabled-offer 2', fesh=39, feedid=5, offerid='2'),
        ]

        cls.dynamic.market_dynamic.creation_time = 5

    @classmethod
    def prepare_offer_legacy_dynamics(cls):
        cls.index.shops += [
            Shop(
                fesh=111,
                datafeed_id=111,
                priority_region=2,
                name='blue_shop_111',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
                warehouse_id=145,
            ),
        ]

        blue_offer_1 = BlueOffer(
            title='shopskulgcy1',
            waremd5='BlueOffer-1-RTY-LGCY-w',
            price=11,
            price_old=21,
            feedid=111,
            offerid='shopskulgcy1',
        )

        cls.index.mskus += [
            MarketSku(
                title='blueMarketSku1',
                hyperid=1,
                sku=220011,
                waremd5='MarketSku1-IiLVm1glgcy',
                blue_offers=[blue_offer_1],
            ),
        ]

    def test_offer_legacy_dynamics(self):
        """
        Если у оффера есть признак скрытия только в legacy_qpipe, то оффер скрывается несмотря на то, что включен RTY
        """
        response = self.report.request_json('place=prime&text=shopskulgcy1&rearr-factors=rty_dynamics=1;rty_qpipe=1')
        self.assertFragmentIn(
            response,
            [
                {'titles': {'raw': 'shopskulgcy1'}},
            ],
            allow_different_len=True,
        )

        # Скрываем старым динамиком
        self.dynamic.disabled_sku_offers = [DynamicSkuOffer(shop_id=111, sku='shopskulgcy1')]
        # Причем в RTY тоже есть данные, но признак скрытия не установлен
        self.rty.offers += [RtyOffer(feedid=111, offerid='shopskulgcy1', price=400, disabled=0, disabled_ts=0)]

        response = self.report.request_json('place=prime&text=shopskulgcy1&rearr-factors=rty_dynamics=1;rty_qpipe=1')
        self.assertFragmentNotIn(response, [{'titles': {'raw': 'shopskulgcy1'}}])

    @classmethod
    def prepare_qdata_in_print_doc(cls):
        cls.index.offers += [
            Offer(
                feedid=6,
                offerid='1',
                fesh=1006,
                price=100,
                fee=10,
                has_gone=True,
            ),
            Offer(
                feedid=6,
                offerid='2',
                fesh=1006,
                price=100,
                fee=10,
                has_gone=False,
                waremd5="BH8EPLtKmdLQhLUasgaOnA",
            ),
        ]

    def test_qdata_in_print_doc(self):
        """
        Тестируем выдачу быстрых данных через плейс print_doc
        """
        # проверяем старые быстрые данные
        response = self.report.request_json('place=print_doc&feed_shoffer_id=6-1&req_attrs=qdata')
        self.assertFragmentIn(
            response,
            {
                "qdata": {
                    "prices": {
                        "value": 100,
                        "currency": "RUR",
                        "price from": False,
                        "vat": "null",
                        "modification time": self.index.creation_time,
                        "modification date": self._timestamp_to_string(self.index.creation_time),
                        "source": "generation",
                    },
                    "bids": {
                        "bid": 10,
                        "bid ts": self.index.creation_time,
                        "bid date": self._timestamp_to_string(self.index.creation_time),
                        "bid source": "generation",
                        "fee source": "generation",
                        "dont_pull_up_bids": False,
                        "dont_pull_up_bids source": "generation",
                    },
                    "dynamics": {
                        "offer disabled": False,
                        "offer disabled ts": 0,
                        "offer disabled source": "generation",
                        "offer disabled date": self._timestamp_to_string(0),
                        "offer has gone": True,
                        "offer has gone ts": self.index.creation_time,
                        "offer has gone date": self._timestamp_to_string(self.index.creation_time),
                    },
                }
            },
        )

        # обновляем данные по rty
        self.rty.offers += [
            RtyOffer(
                feedid=6,
                offerid='1',
                disabled=DisabledFlags.build_offer_disabled([DisabledFlags.MARKET_MBI], [DisabledFlags.MARKET_MBI]),
                disabled_ts=self.index.creation_time + 4,
                price=200,
                modification_time=self.index.creation_time + 10,
                version=self.index.creation_time + 10,
                pricefrom=True,
                bid_and_flags=struct.unpack('<i', bytearray([23, 0, 1, 0]))[0],
                bid_and_flags_ts=self.index.creation_time + 5,
            )
        ]

        # проверяем rty быстрые данные
        response = self.report.request_json(
            'place=print_doc&feed_shoffer_id=6-1&req_attrs=qdata&'
            'rearr-factors=rty_qpipe=1;rty_qbids=1;rty_dynamics=1'
        )
        self.assertFragmentIn(
            response,
            {
                "qdata": {
                    "prices": {
                        "value": 200,
                        "currency": "RUR",
                        "price from": True,
                        "vat": "null",  # данные vat продолжаем брать из индекса
                        "modification time": self.index.creation_time + 10,
                        "modification date": self._timestamp_to_string(self.index.creation_time + 10),
                        "source": "rty",
                    },
                    "bids": {
                        "bid": 23,
                        "bid ts": self.index.creation_time + 5,
                        "bid date": self._timestamp_to_string(self.index.creation_time + 5),
                        "bid source": "rty",
                        "fee source": "generation",  # данные для fee берутся из поколения, всегда
                        # TODO: remove fee from place output
                        "dont_pull_up_bids source": "rty",
                    },
                    "dynamics": {
                        "offer disabled": True,
                        "offer disabled ts": self.index.creation_time + 4,
                        "offer disabled source": "rty",
                        "offer disabled date": self._timestamp_to_string(self.index.creation_time + 4),
                        "offer has gone": True,  # from generation
                        "offer has gone ts": self.index.creation_time,
                        "offer has gone date": self._timestamp_to_string(self.index.creation_time),
                    },
                }
            },
        )

        # проверяем rty быстрые из фактора, который замокан и изменился, в том числе и в "dont_pull_up_bids": True
        response = self.report.request_json(
            'place=print_doc&feed_shoffer_id=6-1&req_attrs=qdata&'
            'rearr-factors=rty_qpipe=1;rty_qbids=1;rty_dynamics=1'
        )
        self.assertFragmentIn(
            response,
            {
                "qdata": {
                    "bids": {
                        "bid": 23,
                        "bid ts": self.index.creation_time + 5,
                        "bid date": self._timestamp_to_string(self.index.creation_time + 5),
                        "bid source": "rty",
                        "fee source": "generation",  # данные для fee берутся из индекса, всегда
                        "dont_pull_up_bids": True,
                        "dont_pull_up_bids source": "rty",
                    },
                }
            },
        )

    def test_qdata_in_print_doc_with_fallback(self):
        """
        Тестируем выдачу быстрых данных через плейс print_doc с учетом фолбека на поколение
        """
        # обновляем данные по rty с меткой времени старше метки времени поколения на 2 часа и 1 секунду
        rty_fallback_interval = 2
        modification_time = self.index.creation_time - int(
            timedelta(hours=rty_fallback_interval, seconds=1).total_seconds()
        )
        self.rty.offers += [
            RtyOffer(
                feedid=6,
                offerid='2',
                disabled=DisabledFlags.build_offer_disabled([DisabledFlags.MARKET_MBI], [DisabledFlags.MARKET_MBI]),
                disabled_ts=modification_time,
                price=200,
                modification_time=modification_time,
                version=modification_time,
                pricefrom=True,
            )
        ]

        print_doc = (
            'place=print_doc&feed_shoffer_id=6-2&req_attrs=qdata&debug=da'
            + '&rearr-factors=rty_qpipe=1;rty_qbids=1;rty_dynamics=1'
        )

        # проверяем, что данные берутся из RTY, если fallback выключен
        response = self.report.request_json(print_doc)
        self.assertFragmentIn(response, {'shard': Contains('basesearch16-0')})
        self.assertFragmentIn(
            response,
            {
                "qdata": {
                    "prices": {
                        "value": 200,
                        "currency": "RUR",
                        "price from": True,
                        "vat": "null",
                        "modification time": modification_time,
                        "modification date": self._timestamp_to_string(modification_time),
                        "source": "rty",
                    },
                    "dynamics": {
                        "offer disabled": True,
                        "offer disabled ts": modification_time,
                        "offer disabled source": "rty",
                        "offer disabled date": self._timestamp_to_string(modification_time),
                        "offer has gone": False,
                        "offer has gone ts": self.index.creation_time,
                        "offer has gone date": self._timestamp_to_string(self.index.creation_time),
                        "offer order method ts": 0,
                        "offer order method date": self._timestamp_to_string(0),
                    },
                }
            },
        )

        # проверяем, что данные берутся из поколения, если fallback включен и в rty данные более старые
        response = self.report.request_json('{};rty_fallback_interval={}h'.format(print_doc, rty_fallback_interval))
        self.assertFragmentIn(
            response,
            {
                "qdata": {
                    "prices": {
                        "value": 100,
                        "currency": "RUR",
                        "price from": False,
                        "vat": "null",
                        "modification time": self.index.creation_time,
                        "modification date": self._timestamp_to_string(self.index.creation_time),
                        "source": "generation",
                    },
                    "dynamics": {
                        "offer disabled": False,
                        "offer disabled ts": 0,
                        "offer disabled source": "generation",
                        "offer disabled date": self._timestamp_to_string(0),
                        "offer has gone": False,
                        "offer has gone ts": self.index.creation_time,
                        "offer has gone date": self._timestamp_to_string(self.index.creation_time),
                        "offer order method ts": 0,
                        "offer order method date": self._timestamp_to_string(0),
                    },
                }
            },
        )

        prime_base = 'place=prime&rids=213&show-urls=&regset=1&pp=42&offerid=BH8EPLtKmdLQhLUasgaOnA'
        prime_with_rty = '{}&debug=1&rearr-factors=rty_qpipe=1;rty_qbids=1;rty_dynamics=1'.format(prime_base)

        # проверяем, что без RTY документ есть на выдаче
        response = self.report.request_json(prime_base)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                }
            },
        )

        # проверяем, что с включенным RTY, но без fallback документа нет на выдаче
        response = self.report.request_json(prime_with_rty)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 0,
                }
            },
        )

        # проверяем, что со старым RTY и включенынм fallback документ есть на выдаче
        response = self.report.request_json(
            '{};rty_fallback_interval={}h'.format(prime_with_rty, rty_fallback_interval)
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                }
            },
        )

        # проверяем, что со свежим RTY и включенным fallback документа нет на выдаче
        response = self.report.request_json(
            '{};rty_fallback_interval={}h'.format(prime_with_rty, rty_fallback_interval + 1)
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 0,
                }
            },
        )

    @classmethod
    def prepare_has_gone(cls):
        cls.index.offers += [Offer(feedid=7, offerid='2', fesh=123)]

    def test_has_gone(self):
        """
        Проверяем выставление флага HAS_GONE через rty-пайплайн
        """

        # проверяем, что оффер не выключен
        response = self.report.request_json(
            'place=print_doc&feed_shoffer_id=7-2&rearr-factors=rty_qpipe=1;rty_dynamics=1'
        )

        self.assertFragmentIn(response, {'qdata': {'dynamics': {'offer has gone': False}}})

        # выключаем
        self.rty.offers += [
            RtyOffer(feedid=7, offerid='2', flags=2048, flags_ts=self.index.creation_time + 1, price=200)
        ]

        # проверяем, что оффер выключен
        response = self.report.request_json(
            'place=print_doc&feed_shoffer_id=7-2&rearr-factors=rty_qpipe=1;rty_dynamics=1'
        )

        self.assertFragmentIn(
            response,
            {
                'qdata': {
                    'dynamics': {
                        'offer has gone': True,
                        'offer has gone ts': self.index.creation_time + 1,
                        "offer has gone date": self._timestamp_to_string(self.index.creation_time + 1),
                    }
                }
            },
        )

    def check_preorder(self, ware_id, msku, cgi, preorder, inverse=False):
        request = 'place=offerinfo&rids=213&show-urls=&regset=1&pp=42&offerid={}&rgb=BLUE'.format(ware_id)
        response = self.report.request_json(request + cgi)
        result_1 = {
            'results': [
                {
                    'entity': 'offer',
                    'wareId': ware_id,
                    'isPreorder': preorder,
                }
            ]
        }
        if inverse:
            self.assertFragmentNotIn(response, result_1)
        else:
            self.assertFragmentIn(response, result_1)

        request = 'place=sku_offers&rgb=BLUE&rids=213&show-urls=&regset=1&pp=42&market-sku={}'.format(msku)
        response = self.report.request_json(request + cgi)
        result_2 = {
            'entity': 'offer',
            'wareId': ware_id,
            'isPreorder': preorder,
        }
        if inverse:
            self.assertFragmentNotIn(response, result_2)
        else:
            self.assertFragmentIn(response, result_2)

    def check_print_preorder(self, shop_id, offer_id, order_type, ts, cgi=''):
        request = 'place=print_doc&feed_shoffer_id={}-{}'.format(shop_id, offer_id)
        response = self.report.request_json(request + cgi)
        self.assertFragmentIn(
            response,
            {
                'qdata': {
                    'dynamics': {
                        'offer order method': order_type,
                        'offer order method ts': ts,
                        'offer order method date': self._timestamp_to_string(ts),
                    }
                }
            },
        )

    def check_preorder_enabled(
        self, ware_id, msku, shop_id, offer_id, order_type, order_method_ts, rty_stock_dynamics=0
    ):
        exp_flag_use_order_method = '&rearr-factors=rty_stock_dynamics={};rty_dynamics=1'.format(rty_stock_dynamics)
        self.check_print_preorder(
            shop_id=shop_id, offer_id=offer_id, order_type=order_type, ts=order_method_ts, cgi=exp_flag_use_order_method
        )
        # Без show-preorder, параметра предзаказа нет
        # (при этом оффер пропадает из выдачи, т.к. он уже помечен как предзаказный: inverse=True)
        self.check_preorder(ware_id=ware_id, msku=msku, cgi=exp_flag_use_order_method, preorder=Absent(), inverse=True)
        # С show-preorder параметр предзаказа появляется
        self.check_preorder(
            ware_id=ware_id, msku=msku, cgi='&show-preorder=1' + exp_flag_use_order_method, preorder=True
        )

    def check_preorder_disabled(
        self, ware_id, msku, shop_id, offer_id, order_type, order_method_ts, rty_stock_dynamics=0
    ):
        exp_flag_use_order_method = '&rearr-factors=rty_stock_dynamics={};rty_dynamics=1'.format(rty_stock_dynamics)
        self.check_print_preorder(
            shop_id=shop_id, offer_id=offer_id, order_type=order_type, ts=order_method_ts, cgi=exp_flag_use_order_method
        )
        self.check_preorder(ware_id=ware_id, msku=msku, cgi=exp_flag_use_order_method, preorder=Absent())
        self.check_preorder(
            ware_id=ware_id, msku=msku, cgi='&show-preorder=1' + exp_flag_use_order_method, preorder=Absent()
        )

    @classmethod
    def prepare_offer_order_method(cls):
        cls.index.shops += [
            Shop(
                fesh=777,
                datafeed_id=777,
                priority_region=2,
                name='blue_shop_777',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
                warehouse_id=145,
            ),
        ]

        cls.index.outlets += [
            Outlet(fesh=777, region=213),
        ]

        """При наличии стоков все равно предзаказ должен быть в приоритете и товар можно приобрести только по предзаказу"""
        blue_offer_1 = BlueOffer(
            waremd5='BlueOffer-1-RTY-PROR-w',
            price=11,
            price_old=21,
            feedid=777,
            offerid='shop_sku_1',
            stock_store_count=5,
        )

        blue_offer_2 = BlueOffer(
            waremd5='BlueOffer-2-RTY-PROR-w',
            price=12,
            price_old=22,
            feedid=777,
            offerid='shop_sku_2',
        )

        blue_offer_3 = BlueOffer(
            waremd5='BlueOffer-3-RTY-PROR-w',
            price=13,
            price_old=23,
            feedid=777,
            offerid='shop_sku_3',
        )

        """При наличии стоков все равно предзаказ должен быть в приоритете и товар можно приобрести только по предзаказу"""
        blue_offer_4 = BlueOffer(
            waremd5='BlueOffer-4-RTY-PROR-w',
            price=14,
            price_old=24,
            feedid=777,
            offerid='shop_sku_4',
            is_preorder=True,
            stock_store_count=5,
        )

        cls.index.mskus += [
            MarketSku(
                title='blueMarketSku1',
                hyperid=1,
                sku=110011,
                waremd5='MarketSku1-IiLVm1goleg',
                blue_offers=[blue_offer_1],
            ),
            MarketSku(
                title='blueMarketSku2',
                hyperid=1,
                sku=110012,
                waremd5='MarketSku2-IiLVm1goleg',
                blue_offers=[blue_offer_2],
            ),
            MarketSku(
                title='blueMarketSku3',
                hyperid=1,
                sku=110013,
                waremd5='MarketSku3-IiLVm1goleg',
                blue_offers=[blue_offer_3],
            ),
            MarketSku(
                title='blueMarketSku4',
                hyperid=1,
                sku=110014,
                waremd5='MarketSku4-IiLVm1goleg',
                blue_offers=[blue_offer_4],
            ),
        ]

    # Флаги эксперимента по предзаказу
    RSD_ORDER_METHOD = 1 << 0
    RSD_DISABLED = 1 << 1
    RSD_FORCE = 1 << 8

    # Параметры заказа
    UNKNOWN_ORDER_METHOD = 0
    AVAILABLE_FOR_ORDER = 2
    PRE_ORDERED = 3

    def test_offer_order_method_legacy(self):
        """Признак предзаказа через rty не должен ломать старой функциональности (аналог теста
        test_blue_market.py::test_preorder).
        """
        shop_id = 777
        feed_id = 777
        offer_id = 'shop_sku_1'
        ware_id = 'BlueOffer-1-RTY-PROR-w'
        msku = 110011
        shop_sku = 'shop_sku_1'
        order_method_ts = 1

        # Проверим, что у оффера сначала нет возможности предзаказа
        self.check_preorder_disabled(
            ware_id=ware_id,
            msku=msku,
            shop_id=shop_id,
            offer_id=offer_id,
            order_type='UNKNOWN_ORDER_METHOD',
            order_method_ts=0,
        )

        # Добавим возможность предзаказа в rty
        self.rty.offers += [
            RtyOffer(feedid=feed_id, offerid=offer_id, order_method=T.PRE_ORDERED, order_method_ts=order_method_ts)
        ]

        # Без флага эксперимента признак предзаказа не появляется
        self.check_preorder_disabled(
            ware_id=ware_id,
            msku=msku,
            shop_id=shop_id,
            offer_id=offer_id,
            order_type='UNKNOWN_ORDER_METHOD',
            order_method_ts=0,
        )
        # Если экспериментальный флаг есть, но не предусматривает возможность включения метода заказа, то выставленный
        # через rty предзаказ не должен влиять на выдачу
        self.check_preorder_disabled(
            ware_id=ware_id,
            msku=msku,
            shop_id=shop_id,
            offer_id=offer_id,
            order_type='UNKNOWN_ORDER_METHOD',
            order_method_ts=0,
            rty_stock_dynamics=(T.RSD_DISABLED | T.RSD_FORCE),
        )

        # Добавим возможность предзаказа в legacy динамик
        self.dynamic.preorder_sku_offers = [DynamicSkuOffer(shop_id=shop_id, sku=shop_sku)]

        # Предзаказ доступен без эксперимента
        self.check_preorder_enabled(
            ware_id=ware_id, msku=msku, shop_id=shop_id, offer_id=offer_id, order_type='PRE_ORDERED', order_method_ts=0
        )
        # Если экспериментальный флаг есть, но не предусматривает возможность включения метода заказа, то выставленный
        # через rty предзаказ не должен влиять на выдачу
        self.check_preorder_enabled(
            ware_id=ware_id,
            msku=msku,
            shop_id=shop_id,
            offer_id=offer_id,
            order_type='PRE_ORDERED',
            order_method_ts=0,
            rty_stock_dynamics=(T.RSD_DISABLED | T.RSD_FORCE),
        )

    def test_offer_order_method_merge_1(self):
        """При включенном эксперименте (без флага RSD_FORCE) проверяется значение в RTY. Если в RTY значение
        UNKNOWN_ORDER_METHOD, то данные берутся из файла sku-filter.pbuf.sn (legacy_qpipe).
        """
        shop_id = 777
        feed_id = 777
        offer_id = 'shop_sku_2'
        ware_id = 'BlueOffer-2-RTY-PROR-w'
        msku = 110012
        shop_sku = 'shop_sku_2'

        # У оффера нет возможности предзаказа без эксперимента
        self.check_preorder_disabled(
            ware_id=ware_id,
            msku=msku,
            shop_id=shop_id,
            offer_id=offer_id,
            order_type='UNKNOWN_ORDER_METHOD',
            order_method_ts=0,
        )
        # У оффера нет возможности предзаказа с экспериментом
        self.check_preorder_disabled(
            ware_id=ware_id,
            msku=msku,
            shop_id=shop_id,
            offer_id=offer_id,
            order_type='UNKNOWN_ORDER_METHOD',
            order_method_ts=0,
            rty_stock_dynamics=T.RSD_ORDER_METHOD,
        )

        # Добавим возможность предзаказа в legacy динамик
        self.dynamic.preorder_sku_offers = [DynamicSkuOffer(shop_id=shop_id, sku=shop_sku)]

        # Параметр предзаказа работает без эксперимента
        self.check_preorder_enabled(
            ware_id=ware_id, msku=msku, shop_id=shop_id, offer_id=offer_id, order_type='PRE_ORDERED', order_method_ts=0
        )
        # Параметр предзаказа работает с экспериментом
        self.check_preorder_enabled(
            ware_id=ware_id,
            msku=msku,
            shop_id=shop_id,
            offer_id=offer_id,
            order_type='PRE_ORDERED',
            order_method_ts=0,
            rty_stock_dynamics=T.RSD_ORDER_METHOD,
        )

        # Добавляем пустую запись об оффере в RTY
        self.rty.offers += [RtyOffer(feedid=feed_id, offerid=offer_id, price=1)]

        # Параметр предзаказа работает без эксперимента
        self.check_preorder_enabled(
            ware_id=ware_id, msku=msku, shop_id=shop_id, offer_id=offer_id, order_type='PRE_ORDERED', order_method_ts=0
        )
        # Параметр предзаказа работает с экспериментом
        self.check_preorder_enabled(
            ware_id=ware_id,
            msku=msku,
            shop_id=shop_id,
            offer_id=offer_id,
            order_type='PRE_ORDERED',
            order_method_ts=0,
            rty_stock_dynamics=T.RSD_ORDER_METHOD,
        )

    def test_offer_order_method_merge_2(self):
        """При включенном эксперименте (без флага RSD_FORCE) проверяется значение в RTY. Если в RTY значение
        указано, то данные берутся RTY.
        """
        shop_id = 777
        feed_id = 777
        offer_id = 'shop_sku_3'
        ware_id = 'BlueOffer-3-RTY-PROR-w'
        msku = 110013
        order_method_ts = 3

        # У оффера нет возможности предзаказа без эксперимента
        self.check_preorder_disabled(
            ware_id=ware_id,
            msku=msku,
            shop_id=shop_id,
            offer_id=offer_id,
            order_type='UNKNOWN_ORDER_METHOD',
            order_method_ts=0,
        )
        # У оффера нет возможности предзаказа с экспериментом
        self.check_preorder_disabled(
            ware_id=ware_id,
            msku=msku,
            shop_id=shop_id,
            offer_id=offer_id,
            order_type='UNKNOWN_ORDER_METHOD',
            order_method_ts=0,
            rty_stock_dynamics=T.RSD_ORDER_METHOD,
        )

        # Добавим возможность предзаказа в rty
        self.rty.offers += [
            RtyOffer(feedid=feed_id, offerid=offer_id, order_method=T.PRE_ORDERED, order_method_ts=order_method_ts)
        ]

        # Без флага эксперимента признак предзаказа не появляется
        self.check_preorder_disabled(
            ware_id=ware_id,
            msku=msku,
            shop_id=shop_id,
            offer_id=offer_id,
            order_type='UNKNOWN_ORDER_METHOD',
            order_method_ts=0,
        )
        # Параметр предзаказа работает с экспериментом
        self.check_preorder_enabled(
            ware_id=ware_id,
            msku=msku,
            shop_id=shop_id,
            offer_id=offer_id,
            order_type='PRE_ORDERED',
            order_method_ts=order_method_ts,
            rty_stock_dynamics=T.RSD_ORDER_METHOD,
        )

    def test_offer_order_method_force(self):
        """При включенном эксперименте (с флагом RSD_FORCE) проверяется значение в RTY. Если в RTY значение
        UNKNOWN_ORDER_METHOD, то данные берутся из поколения, иначе из RTY.
        """
        shop_id = 777
        feed_id = 777
        offer_id = 'shop_sku_4'
        ware_id = 'BlueOffer-4-RTY-PROR-w'
        msku = 110014
        shop_sku = 'shop_sku_4'
        order_method_ts = 4
        preorder_method_ts = 5

        # Проверим, что у оффера сначала нет возможности предзаказа
        self.check_preorder_disabled(
            ware_id=ware_id,
            msku=msku,
            shop_id=shop_id,
            offer_id=offer_id,
            order_type='UNKNOWN_ORDER_METHOD',
            order_method_ts=0,
        )
        # У оффера нет возможности предзаказа с экспериментом в гибридном режиме
        self.check_preorder_disabled(
            ware_id=ware_id,
            msku=msku,
            shop_id=shop_id,
            offer_id=offer_id,
            order_type='UNKNOWN_ORDER_METHOD',
            order_method_ts=0,
            rty_stock_dynamics=T.RSD_ORDER_METHOD,
        )
        # У оффера есть возможность предзаказа с экспериментом в force режиме (берется из поколения)
        self.check_preorder_enabled(
            ware_id=ware_id,
            msku=msku,
            shop_id=shop_id,
            offer_id=offer_id,
            order_type='PRE_ORDERED',
            order_method_ts=self.index.creation_time,
            rty_stock_dynamics=(T.RSD_ORDER_METHOD | T.RSD_FORCE),
        )

        # Добавим возможность предзаказа в файл sku-filter
        self.dynamic.preorder_sku_offers = [DynamicSkuOffer(shop_id=shop_id, sku=shop_sku)]

        # Без флага эксперимента признак предзаказа появляется (берется из файла)
        self.check_preorder_enabled(
            ware_id=ware_id, msku=msku, shop_id=shop_id, offer_id=offer_id, order_type='PRE_ORDERED', order_method_ts=0
        )
        # С флагом эксперимента признак предзаказа все еще появляется, т.к. в RTY нет никакой информации об оффере
        self.check_preorder_enabled(
            ware_id=ware_id,
            msku=msku,
            shop_id=shop_id,
            offer_id=offer_id,
            order_type='PRE_ORDERED',
            order_method_ts=0,
            rty_stock_dynamics=T.RSD_ORDER_METHOD,
        )
        # У оффера есть возможность предзаказа с экспериментом в force режиме (берется из поколения)
        self.check_preorder_enabled(
            ware_id=ware_id,
            msku=msku,
            shop_id=shop_id,
            offer_id=offer_id,
            order_type='PRE_ORDERED',
            order_method_ts=self.index.creation_time,
            rty_stock_dynamics=(T.RSD_ORDER_METHOD | T.RSD_FORCE),
        )

        # Добавим информацию об оффере в rty, без данных о предзаказе
        self.rty.offers += [RtyOffer(feedid=feed_id, offerid=offer_id, price=1)]

        # Без флага эксперимента признак предзаказа появляется (берется из файла)
        self.check_preorder_enabled(
            ware_id=ware_id, msku=msku, shop_id=shop_id, offer_id=offer_id, order_type='PRE_ORDERED', order_method_ts=0
        )
        # С флагом эксперимента признак предзаказа все еще появляется, т.к. в RTY нет никакой информации об оффере
        self.check_preorder_enabled(
            ware_id=ware_id,
            msku=msku,
            shop_id=shop_id,
            offer_id=offer_id,
            order_type='PRE_ORDERED',
            order_method_ts=0,
            rty_stock_dynamics=T.RSD_ORDER_METHOD,
        )
        # У оффера есть возможность предзаказа с экспериментом в force режиме (берется из поколения)
        self.check_preorder_enabled(
            ware_id=ware_id,
            msku=msku,
            shop_id=shop_id,
            offer_id=offer_id,
            order_type='PRE_ORDERED',
            order_method_ts=self.index.creation_time,
            rty_stock_dynamics=(T.RSD_ORDER_METHOD | T.RSD_FORCE),
        )

        # Добавим информацию об отсутствии предзаказа в rty
        self.rty.offers += [
            RtyOffer(
                feedid=feed_id, offerid=offer_id, order_method=T.AVAILABLE_FOR_ORDER, order_method_ts=order_method_ts
            )
        ]

        # Без флага эксперимента признак предзаказа появляется (берется из файла)
        self.check_preorder_enabled(
            ware_id=ware_id, msku=msku, shop_id=shop_id, offer_id=offer_id, order_type='PRE_ORDERED', order_method_ts=0
        )
        # Предзаказа нет с экспериментом в гибридном режиме (берется из RTY)
        self.check_preorder_disabled(
            ware_id=ware_id,
            msku=msku,
            shop_id=shop_id,
            offer_id=offer_id,
            order_type='AVAILABLE_FOR_ORDER',
            order_method_ts=order_method_ts,
            rty_stock_dynamics=T.RSD_ORDER_METHOD,
        )
        # Предзаказа нет с экспериментом в force режиме (берется из RTY)
        self.check_preorder_disabled(
            ware_id=ware_id,
            msku=msku,
            shop_id=shop_id,
            offer_id=offer_id,
            order_type='AVAILABLE_FOR_ORDER',
            order_method_ts=order_method_ts,
            rty_stock_dynamics=(T.RSD_ORDER_METHOD | T.RSD_FORCE),
        )

        # Добавим возможность предзаказа в rty
        self.rty.offers += [
            RtyOffer(feedid=feed_id, offerid=offer_id, order_method=T.PRE_ORDERED, order_method_ts=preorder_method_ts)
        ]

        # Без флага эксперимента признак предзаказа появляется (берется из файла)
        self.check_preorder_enabled(
            ware_id=ware_id, msku=msku, shop_id=shop_id, offer_id=offer_id, order_type='PRE_ORDERED', order_method_ts=0
        )
        # Параметр предзаказа работает с экспериментом в гибридном режиме (берется из RTY)
        self.check_preorder_enabled(
            ware_id=ware_id,
            msku=msku,
            shop_id=shop_id,
            offer_id=offer_id,
            order_type='PRE_ORDERED',
            order_method_ts=preorder_method_ts,
            rty_stock_dynamics=T.RSD_ORDER_METHOD,
        )
        # У оффера есть возможность предзаказа с экспериментом в force режиме (берется из RTY)
        self.check_preorder_enabled(
            ware_id=ware_id,
            msku=msku,
            shop_id=shop_id,
            offer_id=offer_id,
            order_type='PRE_ORDERED',
            order_method_ts=preorder_method_ts,
            rty_stock_dynamics=(T.RSD_ORDER_METHOD | T.RSD_FORCE),
        )

    @classmethod
    def prepare_offer_disbaled_source(cls):
        cls.index.offers += [
            Offer(title='offer-disabledsource 1', fesh=39, feedid=8, offerid='1'),
            Offer(title='offer-disabledsource 2', fesh=39, feedid=8, offerid='2'),
            Offer(title='offer-disabledsource 3', fesh=39, feedid=8, offerid='3'),
            Offer(title='offer-disabledsource 4', fesh=39, feedid=8, offerid='4'),
        ]

        cls.dynamic.market_dynamic.creation_time = 5

    def test_offer_disabled_source(self):
        """Проверка логики работы источников скрытия
        1-й оффер отключаем через rty источником MARKET_STOCK.
        2-й оффер отключаем через rty источником MARKET_MBI.
        3-й оффер отключаем через rty источниками MARKET_MBI и MARKET_STOCK.
        4-й оффер отключаем через rty но битовая маска в старших 16ти байтах не корректна.

        Проверяется игнорирование скрытия по стоку через RTY:
          - 1-й оффер скрывается, только если включен эксперимент RSD_DISABLED. Иначе скрывается только по legacy
            динамику.
          - 2-й и 3-й оффер скрывается всегда
          - 4-й оффер всегда скрывается только по legacy динамику.
        """

        tail = 'place=prime&text=disabledsource&rearr-factors=rty_dynamics=1;rty_stock_dynamics={}'

        response = self.report.request_json(tail.format(0))
        self.assertFragmentIn(
            response,
            [
                {'titles': {'raw': 'offer-disabledsource 1'}},
                {'titles': {'raw': 'offer-disabledsource 2'}},
                {'titles': {'raw': 'offer-disabledsource 3'}},
                {'titles': {'raw': 'offer-disabledsource 4'}},
            ],
            allow_different_len=False,
        )

        self.rty.offers += [
            RtyOffer(
                disabled=DisabledFlags.build_offer_disabled([DisabledFlags.MARKET_STOCK], [DisabledFlags.MARKET_STOCK]),
                feedid=8,
                offerid='1',
                disabled_ts=6,
                price=200,
            ),
            RtyOffer(
                disabled=DisabledFlags.build_offer_disabled([DisabledFlags.MARKET_MBI], [DisabledFlags.MARKET_MBI]),
                feedid=8,
                offerid='2',
                disabled_ts=7,
                price=200,
            ),
            RtyOffer(
                disabled=DisabledFlags.build_offer_disabled(
                    [DisabledFlags.MARKET_STOCK, DisabledFlags.MARKET_MBI],
                    [DisabledFlags.MARKET_STOCK, DisabledFlags.MARKET_MBI],
                ),
                feedid=8,
                offerid='3',
                disabled_ts=8,
                price=200,
            ),
            RtyOffer(
                disabled=DisabledFlags.build_offer_disabled([DisabledFlags.MARKET_MBI], []),
                feedid=8,
                offerid='4',
                disabled_ts=9,
                price=200,
            ),
        ]

        response = self.report.request_json(tail.format(0))
        self.assertFragmentIn(
            response,
            [{'titles': {'raw': 'offer-disabledsource 1'}}, {'titles': {'raw': 'offer-disabledsource 4'}}],
            allow_different_len=False,
        )
        self.assertFragmentNotIn(
            response, [{'titles': {'raw': 'offer-disabledsource 2'}}, {'titles': {'raw': 'offer-disabledsource 3'}}]
        )

        response = self.report.request_json(tail.format(T.RSD_DISABLED))
        self.assertFragmentIn(
            response,
            [
                {'titles': {'raw': 'offer-disabledsource 4'}},
            ],
            allow_different_len=False,
        )
        self.assertFragmentNotIn(
            response,
            [
                {'titles': {'raw': 'offer-disabledsource 1'}},
                {'titles': {'raw': 'offer-disabledsource 2'}},
                {'titles': {'raw': 'offer-disabledsource 3'}},
            ],
        )

        # Скрываем оффер 4 legacy динамиком с более старым timestamp чем RTY
        self.dynamic.market_dynamic.disabled_offers_new += [
            DynamicOfferNew(feed_id=8, offer_id='4'),
        ]

        # Здесь срабатывает fallback, который мы должны увидеть на графике в случае некорретных данных от SaaS-hub
        response = self.report.request_json(tail.format(0))
        self.assertFragmentNotIn(
            response,
            [
                {'titles': {'raw': 'offer-disabledsource 1'}},
                {'titles': {'raw': 'offer-disabledsource 2'}},
                {'titles': {'raw': 'offer-disabledsource 3'}},
                {'titles': {'raw': 'offer-disabledsource 4'}},
            ],
        )

    @classmethod
    def prepare_offer_disbaled_flags(cls):
        cls.index.offers += [
            Offer(
                title='disabledflags1',
                fesh=39,
                feedid=9,
                offerid='1',
                disabled_flags=DisabledFlags.build_offer_disabled_flags([DisabledFlags.MARKET_STOCK]),
            ),
            Offer(
                title='disabledflags2',
                fesh=39,
                feedid=9,
                offerid='2',
                disabled_flags=DisabledFlags.build_offer_disabled_flags([]),
            ),
            Offer(
                title='disabledflags3',
                fesh=39,
                feedid=9,
                offerid='3',
                disabled_flags=DisabledFlags.build_offer_disabled_flags([DisabledFlags.MARKET_STOCK]),
            ),
            Offer(
                title='disabledflags4',
                fesh=39,
                feedid=9,
                offerid='4',
                disabled_flags=DisabledFlags.build_offer_disabled_flags([]),
            ),
            Offer(
                title='disabledflags5',
                fesh=39,
                feedid=9,
                offerid='5',
                disabled_flags=DisabledFlags.build_offer_disabled_flags([DisabledFlags.MARKET_STOCK]),
            ),
            Offer(
                title='disabledflags7',
                fesh=39,
                feedid=9,
                offerid='7',
                disabled_flags=DisabledFlags.build_offer_disabled_flags([]),
            ),
            Offer(
                title='disabledflags8',
                fesh=39,
                feedid=9,
                offerid='8',
                disabled_flags=DisabledFlags.build_offer_disabled_flags([DisabledFlags.MARKET_IDX]),
            ),
            Offer(
                title='disabledflags9',
                fesh=39,
                feedid=9,
                offerid='9',
                disabled_flags=DisabledFlags.build_offer_disabled_flags([DisabledFlags.MARKET_IDX]),
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                title='disabledflags6-msku',
                hyperid=1,
                sku=1000006,
                blue_offers=[
                    BlueOffer(
                        title='disabledflags6',
                        feedid=777,
                        offerid='6',
                    )
                ],
            ),
            MarketSku(
                title='disabledflags10-msku',
                hyperid=1,
                sku=1000007,
                blue_offers=[
                    BlueOffer(
                        title='disabledflags10',
                        feedid=777,
                        offerid='10',
                    )
                ],
            ),
        ]

        cls.dynamic.market_dynamic.creation_time = 5

    def _test_disabled(self, title, rty_stock_dynamics=0, rty_dynamics=1, rty_ignored_disabled_flags=0):
        response = self.report.request_json(
            'place=prime&text={}&rearr-factors=rty_dynamics={};rty_stock_dynamics={};'
            'rty_ignored_disabled_flags={}'.format(title, rty_dynamics, rty_stock_dynamics, rty_ignored_disabled_flags)
        )

        self.assertFragmentNotIn(
            response,
            [
                {'titles': {'raw': title}},
            ],
        )

    def _test_enabled(self, title, rty_stock_dynamics=0, rty_dynamics=1, rty_ignored_disabled_flags=0):
        response = self.report.request_json(
            'place=prime&text={}&rearr-factors=rty_dynamics={};rty_stock_dynamics={};'
            'rty_ignored_disabled_flags={}'.format(title, rty_dynamics, rty_stock_dynamics, rty_ignored_disabled_flags)
        )

        self.assertFragmentIn(
            response,
            [
                {'titles': {'raw': title}},
            ],
        )

    def test_offer_disabled_flags_disabled_in_index_unknown_in_rty(self):
        """Если оффер скрыт флагами скрытия в индексе, и скрытие не определено в RTY, то оффер скрыт"""
        self._test_disabled('disabledflags1', rty_stock_dynamics=T.RSD_DISABLED)

    def test_offer_disabled_flags_disabled_in_index_without_rty(self):
        """Если оффер скрыт флагами скрытия в индексе, и RTY не используется, то оффер скрыт"""
        self._test_disabled('disabledflags1', rty_stock_dynamics=T.RSD_DISABLED, rty_dynamics=0)

    def test_offer_disabled_flags_enabled_in_index_unknown_in_rty(self):
        """Если оффер открыт флагами скрытия в индексе, и скрытие не определено в RTY, то оффер открыт"""
        self._test_enabled('disabledflags2', rty_stock_dynamics=T.RSD_DISABLED)

    def test_offer_disabled_flags_disabled_in_index_enabled_in_rty(self):
        """Если оффер скрыт флагами скрытия в индексе, но открыт в RTY, то оффер открыт"""
        self._test_disabled('disabledflags3', rty_stock_dynamics=T.RSD_DISABLED)
        self.rty.offers += [
            RtyOffer(
                disabled=DisabledFlags.build_offer_disabled([], [DisabledFlags.MARKET_STOCK]),
                feedid=9,
                offerid='3',
                disabled_ts=6,
                price=200,
            ),
        ]
        self._test_enabled('disabledflags3', rty_stock_dynamics=T.RSD_DISABLED)

    def test_offer_disabled_flags_enabled_in_index_disabled_in_rty(self):
        """Если оффер открыт флагами скрытия в индексе, но скрыт в RTY, то оффер скрыт"""
        self._test_enabled('disabledflags4', rty_stock_dynamics=T.RSD_DISABLED)
        self.rty.offers += [
            RtyOffer(
                disabled=DisabledFlags.build_offer_disabled([DisabledFlags.MARKET_STOCK], [DisabledFlags.MARKET_STOCK]),
                feedid=9,
                offerid='4',
                disabled_ts=6,
                price=200,
            ),
        ]
        self._test_disabled('disabledflags4', rty_stock_dynamics=T.RSD_DISABLED)

    def test_offer_disabled_flags_disabled_in_index_enabled_in_rty_by_another_source(self):
        """Если оффер скрыт флагами скрытия в индексе, и открыт в RTY, но от другого источника, то оффер скрыт"""
        self._test_disabled('disabledflags5', rty_stock_dynamics=T.RSD_DISABLED)
        self.rty.offers += [
            RtyOffer(
                disabled=DisabledFlags.build_offer_disabled([], [DisabledFlags.MARKET_MBI]),
                feedid=9,
                offerid='5',
                disabled_ts=6,
            ),
        ]
        self._test_disabled('disabledflags5', rty_stock_dynamics=T.RSD_DISABLED)

    def test_offer_disabled_flags_disabled_in_index_not_disabled_in_sku_filter(self):
        """Если синий оффер скрыт флагами скрытия по стоку в индексе, но не скрыт в sku-filter, причем флаги скрытия
        по стоку в RTY игнорируются, то оффер открыт
        """
        self._test_enabled('disabledflags6', rty_stock_dynamics=0)

    def test_offer_disabled_flags_enabled_in_index_but_disabled_in_sku_filter(self):
        """Если синий оффер открыт флагами скрытия по стоку в индексе, но скрыт в sku-filter, а в RTY нет информации
        по этому офферу, то оффер скрывается только в legacy и гибридном режиме
        """
        self.dynamic.disabled_sku_offers = [DynamicSkuOffer(shop_id=777, sku='10')]
        self._test_disabled('disabledflags10', rty_stock_dynamics=0)
        self._test_disabled('disabledflags10', rty_stock_dynamics=T.RSD_DISABLED)
        self._test_enabled('disabledflags10', rty_stock_dynamics=(T.RSD_DISABLED | T.RSD_FORCE))

    def test_offer_disabled_flags_enabled_in_index_ignored_disabled_in_rty(self):
        """Если оффер открыт флагами скрытия в индексе,
        и скрыт игнорируемым источником скрытия в RTY, то оффер открыт
        """
        rty_ignored_disabled_flags = DisabledFlags.build_offer_disabled_flags([DisabledFlags.MARKET_IDX])
        self._test_enabled('disabledflags7', rty_ignored_disabled_flags=rty_ignored_disabled_flags)
        self.rty.offers += [
            RtyOffer(
                disabled=DisabledFlags.build_offer_disabled([DisabledFlags.MARKET_IDX], [DisabledFlags.MARKET_IDX]),
                feedid=9,
                offerid='7',
                disabled_ts=6,
                price=200,
            ),
        ]
        self._test_enabled('disabledflags7', rty_ignored_disabled_flags=rty_ignored_disabled_flags)

    def test_offer_disabled_flags_ignored_disabled_in_index_unknown_in_rty(self):
        """Если оффер скрыт игнорируемым флагамом скрытия в индексе, то оффер скрыт"""
        rty_ignored_disabled_flags = DisabledFlags.build_offer_disabled_flags([DisabledFlags.MARKET_IDX])
        self._test_disabled('disabledflags8', rty_ignored_disabled_flags=rty_ignored_disabled_flags)

    def test_offer_disabled_flags_disabled_in_index_ignored_enabled_in_rty(self):
        """Если оффер скрыт флагами скрытия в индексе,
        и открыт игнорируемым источником скрытия в RTY, то оффер скрыт
        """
        rty_ignored_disabled_flags = DisabledFlags.build_offer_disabled_flags([DisabledFlags.MARKET_IDX])
        self._test_disabled('disabledflags9', rty_ignored_disabled_flags=rty_ignored_disabled_flags)
        self.rty.offers += [
            RtyOffer(
                disabled=DisabledFlags.build_offer_disabled([], [DisabledFlags.MARKET_IDX]),
                feedid=9,
                offerid='9',
                disabled_ts=6,
            ),
        ]
        self._test_disabled('disabledflags9', rty_ignored_disabled_flags=rty_ignored_disabled_flags)


if __name__ == '__main__':
    main()
