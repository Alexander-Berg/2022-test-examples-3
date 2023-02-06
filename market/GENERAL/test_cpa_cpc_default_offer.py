#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import (
    BlueOffer,
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryOption,
    DynamicDeliveryServiceInfo,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseInfo,
    DynamicWarehousesPriorityInRegion,
    MarketSku,
    MnPlace,
    Offer,
    RegionalDelivery,
    Shop,
)
from core.matcher import NotEmpty, NoKey, Not, Equal
from unittest import skip


class T(TestCase):
    """
    https://st.yandex-team.ru/MARKETOUT-32932
    Кроме дефолтного оффера нужны еще default-cpc и default-cpa
    """

    @classmethod
    def prepare(cls):
        cls.index.shops += [
            Shop(
                fesh=1,
                datafeed_id=1,
                priority_region=213,
                name='ВиртуальныйМагазинНаБеру',
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
            ),
            Shop(
                fesh=11,
                datafeed_id=11,
                priority_region=213,
                regions=[225],
                name="Один 1P поставщик",
                supplier_type=Shop.FIRST_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
                fulfillment_program=True,
            ),
            Shop(fesh=2, priority_region=213, regions=[225], name="Белый магазин"),
            Shop(
                fesh=3, priority_region=213, regions=[255], name="Белый cpa магазин", cpa=Shop.CPA_REAL, cpc=Shop.CPC_NO
            ),
            Shop(fesh=4, priority_region=2, regions=[225], name="Белый магазин в Питере"),
        ]

        cls.settings.lms_autogenerate = False

        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=145, home_region=213),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=145,
                delivery_service_id=157,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=23, region_to=225)],
            ),
            DynamicDeliveryServiceInfo(id=157, rating=2),
            DynamicWarehousesPriorityInRegion(region=225, warehouses=[145]),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=1234,
                carriers=[157],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[
                    RegionalDelivery(rid=225, options=[DeliveryOption(price=15, day_from=1, day_to=2)]),
                ],
            ),
            DeliveryBucket(
                bucket_id=5678,
                carriers=[99],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
                regional_options=[
                    RegionalDelivery(rid=225, options=[DeliveryOption(price=15, day_from=1, day_to=2)]),
                ],
            ),
        ]

        # hyperid=1 ДО достается fesh=2 (затем по приоритету fesh=1 затем fesh=3)
        cls.index.mskus += [
            MarketSku(
                hyperid=1,
                hid=1,
                sku=1,
                delivery_buckets=[1234],
                blue_offers=[BlueOffer(price=2100, feedid=11, waremd5='ozmCtRBXgUJgvxo4kHPBzg', ts=11, randx=2)],
            ),
        ]
        cls.index.offers += [
            Offer(
                hyperid=1, hid=1, fesh=2, price=2000, waremd5='1jduB9LH2zF21wvUJdyrZQ', delivery_buckets=[5678], randx=3
            ),
            Offer(
                hyperid=1,
                hid=1,
                fesh=3,
                price=1999,
                waremd5='qPmVtLHDotYmTXs1ADF67w',
                delivery_buckets=[5678],
                randx=1,
                cpa=Offer.CPA_REAL,
            ),
            Offer(
                hyperid=1, hid=1, fesh=4, price=2200, waremd5='CB8MLpcTXsvhl4yf_wgEag', delivery_buckets=[5678], randx=1
            ),  # cpc-оффер из Питера
        ]

        # hyperid=2 ДО достается fesh=1 (затем по приоритету fesh=3 затем fesh=2)
        cls.index.mskus += [
            MarketSku(
                hyperid=2,
                hid=1,
                sku=2,
                delivery_buckets=[1234],
                blue_offers=[BlueOffer(price=2500, feedid=11, waremd5='EpjVJ4VEoRFJue0jmAoUkg', ts=12, randx=3)],
            ),
        ]

        cls.index.offers += [
            Offer(
                hyperid=2, hid=1, fesh=2, price=3100, waremd5='mBPKTnft_sOeVWOcwl5i5Q', delivery_buckets=[5678], randx=1
            ),
            Offer(
                hyperid=2,
                hid=1,
                fesh=3,
                price=3150,
                waremd5='8M8KriRywfqxO15sAtn6ag',
                delivery_buckets=[5678],
                randx=2,
                cpa=Offer.CPA_REAL,
            ),
        ]

    def test_cpc_offer(self):
        """market_cpc_default_offer=1 добавляет дефолтный оффер среди cpc-оффреов"""

        response = self.report.request_json('place=productoffers&hid=1&hyperid=2&rids=213&offers-set=defaultList')
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        # сейчас дефолтный - оффер Беру
                        'wareId': 'EpjVJ4VEoRFJue0jmAoUkg',
                        'benefit': {'type': 'cheapest'},
                    }
                ]
            },
            allow_different_len=False,
        )

        response = self.report.request_json(
            'place=productoffers&hid=1&hyperid=2&rids=213&offers-set=defaultList'
            '&rearr-factors=market_cpc_default_offer=1'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'wareId': 'EpjVJ4VEoRFJue0jmAoUkg', 'benefit': {'type': 'cheapest'}},
                    # default-cpc выбирается среди офферов НЕ беру и НЕ белых сpa-магазинов
                    {'wareId': 'mBPKTnft_sOeVWOcwl5i5Q', 'benefit': {'type': 'default-cpc'}},
                ]
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_forbidden_category(cls):

        # 91273 - ювлирка
        cls.index.mskus += [
            MarketSku(
                hyperid=3,
                hid=91273,
                sku=3,
                delivery_buckets=[1234],
                blue_offers=[BlueOffer(price=2100, feedid=11, waremd5='h5zoPbCGjXAxruVncrG4rw', ts=11, randx=2)],
            )
        ]

        cls.index.offers += [
            Offer(
                hyperid=3,
                hid=91273,
                fesh=2,
                price=3000,
                waremd5='cBTfX8aiTFp1swugYbv3_w',
                delivery_buckets=[5678],
                randx=3,
            ),
            Offer(
                hyperid=3,
                hid=91273,
                fesh=3,
                price=3100,
                waremd5='LesrKznCMIAbWF0TdB2FLw',
                delivery_buckets=[5678],
                randx=1,
            ),
        ]

    def test_cpc_and_cpa_do_disabled_for_forbidden_category(self):
        # под флагом market_beru_order_ignore_category=1 (включен по умолчанию)
        # ограничений на категории нет

        response = self.report.request_json(
            'place=productoffers&hid=91273&hyperid=3&rids=213&offers-set=defaultList&debug=da'
            '&rearr-factors=market_cpa_default_offer=1;market_cpc_default_offer=1;'
            'market_ha_cpa_ctr_mult=0'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'wareId': 'h5zoPbCGjXAxruVncrG4rw', 'benefit': {'type': 'cheapest'}},
                    {'wareId': 'h5zoPbCGjXAxruVncrG4rw', 'benefit': {'type': 'default-cpa'}},
                    {'wareId': 'cBTfX8aiTFp1swugYbv3_w', 'benefit': {'type': 'default-cpc'}},
                ]
            },
            allow_different_len=False,
        )
        self.assertFragmentNotIn(response, "default-cpa and default-cpc DO is forbidden")

        response = self.report.request_json(
            'place=productoffers&hid=91273&hyperid=3&rids=213&offers-set=defaultList&debug=da'
            '&show-urls=beruOrder&rearr-factors=market_cpa_default_offer=1;market_cpc_default_offer=1;'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'wareId': 'h5zoPbCGjXAxruVncrG4rw', 'benefit': {'type': 'cheapest'}},
                    {'wareId': 'h5zoPbCGjXAxruVncrG4rw', 'benefit': {'type': 'default-cpa'}},
                    {'wareId': 'cBTfX8aiTFp1swugYbv3_w', 'benefit': {'type': 'default-cpc'}},
                ]
            },
            allow_different_len=False,
        )
        self.assertFragmentNotIn(response, "default-cpa and default-cpc DO is forbidden")

    @classmethod
    def prepare_premium_offer(cls):
        cls.index.mskus += [
            # и premium-ДО и cpa-ДО выиграет один и тот же оффер
            MarketSku(
                hyperid=4,
                hid=1,
                sku=4,
                delivery_buckets=[1234],
                blue_offers=[BlueOffer(price=4200, feedid=11, waremd5='Z6q6AeXY7RHkSARanSac4g', randx=10, cbid=100)],
            ),
            # premium-ДО выиграет оффер с большей ставкой а cpa-ДО выиграет наиболее подходящий оффер
            MarketSku(
                hyperid=5,
                hid=1,
                sku=51,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(price=5100, feedid=11, waremd5='3Lr1V7qeQzXooDED4fv-tw', ts=51, randx=11, cbid=50)
                ],
            ),
            MarketSku(
                hyperid=5,
                hid=1,
                sku=52,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(price=5200, feedid=11, waremd5='jooaMuw3dToRIrcA3DG32A', ts=52, randx=12, cbid=100)
                ],
            ),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 51).respond(0.22)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 52).respond(0.21)

        cls.index.offers += [
            Offer(
                hyperid=4,
                hid=1,
                fesh=2,
                price=4000,
                waremd5='9rj8uIe14Uh66QFnv-M2zQ',
                delivery_buckets=[5678],
                randx=13,
            ),
            Offer(
                hyperid=5,
                hid=1,
                fesh=2,
                price=5000,
                waremd5='0RhmoXF7h1t-vVLVYLbJfw',
                delivery_buckets=[5678],
                randx=14,
            ),
        ]

    @skip('while premium is dead')
    def test_premium_offer_cannot_duplicate_cpa_offer(self):
        """Если premium-ДО оффер совпадет с cpa-ДО то он не выведется, а в benefit.nestedTypes совпавшего cpa-ДО запишется premium"""
        # кейс 1 premium-ДО совпадает с cpa-ДО и не выводится

        response = self.report.request_json(
            'place=productoffers&hyperid=4&hid=1&rids=213&offers-set=defaultList&debug=da&rearr-factors=market_premium_offer_logic=add-and-mark-touch'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'wareId': '9rj8uIe14Uh66QFnv-M2zQ', 'benefit': {'type': 'cheapest'}},
                    {'wareId': 'Z6q6AeXY7RHkSARanSac4g', 'benefit': {'type': 'premium'}},
                ]
            },
            allow_different_len=False,
        )

        response = self.report.request_json(
            'place=productoffers&hyperid=4&hid=1&rids=213&offers-set=defaultList&debug=da'
            '&show-urls=beruOrder&rearr-factors=market_mix_cpa_offer=1;market_premium_offer_logic=add-and-mark-touch'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'wareId': '9rj8uIe14Uh66QFnv-M2zQ', 'benefit': {'type': 'cheapest'}},
                    {'wareId': 'Z6q6AeXY7RHkSARanSac4g', 'benefit': {'type': 'cpa', 'nestedTypes': ['cpa', 'premium']}},
                ]
            },
            allow_different_len=False,
        )

        self.assertFragmentIn(
            response,
            'Offer Z6q6AeXY7RHkSARanSac4g from shop 1 as premium offer is dropped '
            'because there are already exists offer Z6q6AeXY7RHkSARanSac4g from shop 1 as cpa offer',
        )
        # кейс 2 premium-ДО не совпадает с cpa-ДО но они из одного магазина
        response = self.report.request_json(
            'place=productoffers&hyperid=5&hid=1&rids=213&offers-set=defaultList&debug=da&rearr-factors=market_premium_offer_logic=add-and-mark-touch'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'wareId': '0RhmoXF7h1t-vVLVYLbJfw', 'benefit': {'type': 'cheapest'}},
                    {'wareId': 'jooaMuw3dToRIrcA3DG32A', 'benefit': {'type': 'premium'}},
                ]
            },
            allow_different_len=False,
        )

        response = self.report.request_json(
            'place=productoffers&hyperid=5&hid=1&rids=213&offers-set=defaultList&debug=da'
            '&show-urls=beruOrder&rearr-factors=market_mix_cpa_offer=1;market_premium_offer_logic=add-and-mark-touch'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'wareId': '0RhmoXF7h1t-vVLVYLbJfw', 'benefit': {'type': 'cheapest'}},
                    {'wareId': '3Lr1V7qeQzXooDED4fv-tw', 'benefit': {'type': 'cpa', 'nestedTypes': ['cpa', 'premium']}},
                ]
            },
            allow_different_len=False,
        )
        self.assertFragmentIn(
            response,
            'Offer jooaMuw3dToRIrcA3DG32A from shop 1 as premium offer is dropped '
            'because there are already exists offer 3Lr1V7qeQzXooDED4fv-tw from shop 1 as cpa offer',
        )

    @skip('while premium is dead')
    def test_premium_offer_can_be_cpa(self):
        """premium оффер может быть cpa-оффром"""

        response = self.report.request_json(
            'place=productoffers&hyperid=4&hid=1&rids=213&offers-set=defaultList&debug=da&show-urls=beruOrder&rearr-factors=market_premium_offer_logic=add-and-mark-touch'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'wareId': '9rj8uIe14Uh66QFnv-M2zQ', 'benefit': {'type': 'cheapest'}},
                    {'wareId': 'Z6q6AeXY7RHkSARanSac4g', 'benefit': {'type': 'premium'}},
                ]
            },
            allow_different_len=False,
        )

    @skip('https://st.yandex-team.ru/MARKETOUT-35795')
    def test_cpa_links(self):
        """Проверяем что show_blue_cpa_offers_on_white=1 show_white_cpa_offers_on_white=1
        включают cpa-ссылки по отдельности на синих и на белых офферах
        """

        response = self.report.request_json(
            'place=prime&hid=1&show-urls=cpa,external&rearr-factors=show_white_cpa_offers_on_white=1'
        )
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'wareId': '8M8KriRywfqxO15sAtn6ag',
                'cpa': 'real',
                'urls': {
                    'cpa': NotEmpty(),
                },
                'shop': {'name': 'Белый cpa магазин'},
            },
        )

        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'wareId': 'Z6q6AeXY7RHkSARanSac4g',
                'cpa': NoKey('cpa'),
                'urls': {
                    'cpa': NoKey('cpa'),
                },
                'shop': {'name': 'ВиртуальныйМагазинНаБеру'},
            },
        )

        response = self.report.request_json('place=prime&hid=1&show-urls=cpa,external')
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'wareId': '8M8KriRywfqxO15sAtn6ag',
                'cpa': NoKey('cpa'),
                'urls': {
                    'cpa': NoKey('cpa'),
                },
                'shop': {'name': 'Белый cpa магазин'},
            },
        )

        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'wareId': 'Z6q6AeXY7RHkSARanSac4g',
                'cpa': 'real',
                'urls': {
                    'cpa': NotEmpty(),
                },
                'shop': {'name': 'ВиртуальныйМагазинНаБеру'},
            },
        )

        response = self.report.request_json('place=prime&hid=1&show-urls=cpa,external')
        self.assertFragmentNotIn(
            response,
            {
                'entity': 'offer',
                'wareId': '8M8KriRywfqxO15sAtn6ag',
            },
        )
        self.assertFragmentIn(response, {'entity': 'offer', 'wareId': 'Z6q6AeXY7RHkSARanSac4g'})

    @classmethod
    def prepare_cpa_only_default_offer(cls):
        cls.index.offers += [
            Offer(hyperid=10, fesh=3, randx=1, ts=100301, cpa=Offer.CPA_REAL, title='Хороший оффер с неудачным randx'),
            Offer(hyperid=10, fesh=3, randx=2, ts=100302, cpa=Offer.CPA_REAL, title='Плохой оффер с удачным randx'),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 100301).respond(0.02)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 100302).respond(0.01)

    def test_cpa_only_default_offer(self):
        """Проверяем, что для CPA-only офферов (все DSBS в проде) формула ДО считается"""
        response = self.report.request_json('place=productoffers&hyperid=10&offers-set=default&debug=1')
        self.assertFragmentIn(
            response,
            {
                'titles': {'raw': 'Хороший оффер с неудачным randx'},
            },
        )
        self.assertFragmentIn(
            response,
            {
                'rank': [
                    {
                        'name': 'MATRIXNET_VALUE',
                        'value': Not(Equal('0')),
                    }
                ],
            },
        )


if __name__ == '__main__':
    main()
