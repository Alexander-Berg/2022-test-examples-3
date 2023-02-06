#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    DynamicDeliveryRestriction,
    DynamicWarehouseDelivery,
    DynamicWarehouseLink,
    Region,
    Shop,
)
from core.testcase import TestCase, main
from core.types.offer import OfferDimensions
from core.types.sku import MarketSku, BlueOffer
from core.types.taxes import Vat, Tax
from core.types.fashion_parameters import FashionCategory


PRIME_REQUEST = 'place=prime&pp=18&rids={}&hid={}&allow-collapsing=0&rgb=blue'


class _Delivery:
    courier = 0
    pickup = 1


class _Rids:
    moscow = 213
    russia = 225


class _Hids:
    fashion = 7812062
    not_fashion = 1234567


class _CargoTypes:
    trying_available = 600
    random = 55


class _ShopIds:
    class _Blue:
        virtual = 1
        thirdparty_1 = 2
        thirdparty_2 = 3
        crossdock_1 = 4
        crossdock_2 = 5


class _Shops(object):
    blue_virtual_shop = Shop(
        fesh=_ShopIds._Blue.virtual,
        datafeed_id=_ShopIds._Blue.virtual,
        priority_region=_Rids.moscow,
        name='virtual_shop',
        tax_system=Tax.OSN,
        fulfillment_virtual=True,
        cpa=Shop.CPA_REAL,
        virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
    )

    supplier_3p_1, supplier_3p_2 = [
        Shop(
            fesh=id,
            datafeed_id=id,
            warehouse_id=id,
            name=name,
            priority_region=_Rids.moscow,
            tax_system=Tax.OSN,
            supplier_type=Shop.THIRD_PARTY,
            blue=Shop.BLUE_REAL,
            fulfillment_program=True,
        )
        for id, name in [(_ShopIds._Blue.thirdparty_1, '3P supplier 1'), (_ShopIds._Blue.thirdparty_2, '3P supplier 2')]
    ]

    crossdock_supplier_1, crossdock_supplier_2 = [
        Shop(
            fesh=id,
            datafeed_id=id,
            warehouse_id=id,
            name=name,
            priority_region=_Rids.moscow,
            tax_system=Tax.OSN,
            supplier_type=Shop.THIRD_PARTY,
            blue=Shop.BLUE_REAL,
            fulfillment_program=True,
            direct_shipping=False,
        )
        for id, name in [(_ShopIds._Blue.crossdock_1, 'Crossdock 1'), (_ShopIds._Blue.crossdock_2, 'Crossdock 2')]
    ]

    all_suppliers = [
        supplier_3p_1,
        supplier_3p_2,
        crossdock_supplier_1,
        crossdock_supplier_2,
    ]


class _Offers(object):
    def _create_offer(id, shop, cargo_types=None):

        return BlueOffer(
            vat=Vat.VAT_10,
            feedid=shop.datafeed_id,
            offerid='blue.offer.{}'.format(id),
            waremd5=id,
            weight=5,
            blue_weight=5,
            dimensions=OfferDimensions(length=10, width=20, height=30),
            blue_dimensions=OfferDimensions(length=10, width=20, height=30),
            cargo_types=cargo_types,
        )

    offer_3p_1 = _create_offer(
        'Offer_3P_1___________g', _Shops.supplier_3p_1, cargo_types=[_CargoTypes.random, _CargoTypes.trying_available]
    )
    offer_3p_2 = _create_offer(
        'Offer_3P_2___________g', _Shops.supplier_3p_2, cargo_types=[_CargoTypes.random, _CargoTypes.trying_available]
    )
    offer_crossdock_1 = _create_offer(
        'Offer_Crossdock_1____g',
        _Shops.crossdock_supplier_1,
        cargo_types=[_CargoTypes.random, _CargoTypes.trying_available],
    )
    offer_crossdock_2 = _create_offer(
        'Offer_Crossdock_2____g',
        _Shops.crossdock_supplier_2,
        cargo_types=[_CargoTypes.random, _CargoTypes.trying_available],
    )
    not_fashion_offer_3p_1 = _create_offer(
        'Offer_3P_1not_fashiong', _Shops.supplier_3p_1, cargo_types=[_CargoTypes.trying_available]
    )

    all_fashion_offers = [
        (offer_3p_1, _Shops.supplier_3p_1),
        (offer_crossdock_1, _Shops.crossdock_supplier_1),
        (offer_3p_2, _Shops.supplier_3p_2),
        (offer_crossdock_2, _Shops.crossdock_supplier_2),
    ]

    trying_available_offer_indices = [1, 2]

    count = len(all_fashion_offers)


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.check_combinator_errors = True
        cls.settings.nordstream_autogenerate = False

        cls.index.regiontree += [
            Region(rid=_Rids.moscow, name='Москва'),
        ]

        '''
        Создаем иерархию категорий фешн
        '''
        cls.index.fashion_categories += [
            FashionCategory('FASHION_CATEGORY', _Hids.fashion),
        ]

        cls.index.shops += [_Shops.blue_virtual_shop]
        cls.index.shops.extend(_Shops.all_suppliers)

        cls.index.mskus = [
            MarketSku(
                title='Джинсы' + str(index + 1),
                hid=_Hids.fashion,
                sku=index + 1,
                blue_offers=[
                    _Offers.all_fashion_offers[index][0],
                ],
            )
            for index in range(_Offers.count)
        ]

        cls.index.mskus += [
            MarketSku(
                title='Не фешн',
                hid=_Hids.not_fashion,
                sku=100,
                blue_offers=[
                    _Offers.not_fashion_offer_3p_1,
                ],
            )
        ]

    @classmethod
    def prepare_nordstream(cls):
        cls.dynamic.nordstream += [
            DynamicWarehouseLink(
                shop.warehouse_id,
                [_Shops.supplier_3p_1.warehouse_id if idx < 2 else _Shops.supplier_3p_2.warehouse_id],
                is_trying_available=idx in _Offers.trying_available_offer_indices,
            )
            for idx, (offer, shop) in enumerate(_Offers.all_fashion_offers)
        ]
        cls.dynamic.nordstream += [
            DynamicWarehouseDelivery(
                warehouse_id,
                {
                    _Rids.moscow: [
                        DynamicDeliveryRestriction(
                            is_trying_available=True,
                            delivery_type=_Delivery.courier,
                        ),
                        DynamicDeliveryRestriction(
                            is_trying_available=True,
                            delivery_type=_Delivery.pickup,
                        ),
                    ],
                    _Rids.russia: [
                        DynamicDeliveryRestriction(
                            is_trying_available=True,
                            delivery_type=_Delivery.courier,
                        )
                    ],
                },
            )
            for warehouse_id in (
                _Shops.supplier_3p_1.warehouse_id,
                _Shops.supplier_3p_2.warehouse_id,
            )
        ]

    def test_trying_available_filter_not_visible(self):
        '''
        Проверяем что если выключен нордстрим (market_nordstream=0) или сам фильтр (market_enable_trying_available_filter=0)
        фильтр не отображается не зависимо от категории (hid)
        '''
        for rearr in ['&rearr-factors=market_nordstream=0', '&rearr-factors=market_enable_trying_available_filter=0']:
            for hid in [_Hids.fashion, _Hids.not_fashion]:
                response = self.report.request_json(PRIME_REQUEST.format(_Rids.moscow, hid) + rearr)
                self.assertFragmentNotIn(response, {'filters': [{'id': 'trying-available'}]})

    def test_trying_available_filter_visible(self):
        '''
        Проверяем что если включен нордстрим (market_nordstream=1) и сам фильтр (market_enable_trying_available_filter=1)
        либо оба реара отсутствуют (поведение по умолчанию)
        фильтр отображается в случае если в запросе hid фешна, причем это первый фильтр в списке.
        А также проверяем корректность вычисления статистик.
        '''
        for rearr in ['&rearr-factors=market_nordstream=1;market_enable_trying_available_filter=1', '']:
            response = self.report.request_json(PRIME_REQUEST.format(_Rids.moscow, _Hids.fashion) + rearr)
            self.assertFragmentIn(
                response,
                {
                    'filters': [
                        {
                            'id': 'trying-available',
                            'name': 'С примеркой',
                            'values': [
                                {
                                    'value': '0',
                                    'found': len(_Offers.all_fashion_offers)
                                    - len(_Offers.trying_available_offer_indices),
                                },
                                {'value': '1', 'found': len(_Offers.trying_available_offer_indices)},
                            ],
                        }
                    ]
                },
            )
            self.assertEqual(response['filters'][0]['id'], 'trying-available')

    def test_trying_available_filter_not_fashion_not_visible(self):
        '''
        Проверяем что если включен нордстрим (market_nordstream=1) и сам фильтр (market_enable_trying_available_filter=1)
        либо оба реара отсутствуют (поведение по умолчанию)
        фильтр не отображается если hid не относится к фешну
        '''
        for rearr in ['&rearr-factors=market_nordstream=1;market_enable_trying_available_filter=1', '']:
            response = self.report.request_json(PRIME_REQUEST.format(_Rids.moscow, _Hids.not_fashion) + rearr)
            self.assertFragmentNotIn(response, {'filters': [{'id': 'trying-available'}]})

    def test_trying_available_filer_in_action(self):
        '''
        Проверяем что если включен нордстрим (market_nordstream=1) и сам фильтр (market_enable_trying_available_filter=1)
        и указано что нужны офферы с примеркой (trying-available=1), найденные офферы и их число
        совпадают с офферами из известного списка офферов с примеркой
        '''
        rearr = '&rearr-factors=market_nordstream=1;market_enable_trying_available_filter=1'
        response = self.report.request_json(
            PRIME_REQUEST.format(_Rids.moscow, _Hids.fashion) + rearr + '&trying-available=1'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': len(_Offers.trying_available_offer_indices),
                    'results': [
                        {'wareId': _Offers.all_fashion_offers[index][0].waremd5}
                        for index in _Offers.trying_available_offer_indices
                    ],
                },
            },
            preserve_order=False,
            allow_different_len=False,
        )
        self.assertFragmentIn(
            response,
            {
                'filters': [
                    {
                        'id': 'trying-available',
                        'values': [
                            {
                                'value': '1',
                                'found': len(_Offers.trying_available_offer_indices),
                                "initialFound": 2,
                                "checked": True,
                            },
                        ],
                    }
                ],
            },
        )

        '''
        Проверяем что если включен нордстрим (market_nordstream=1) и сам фильтр (market_enable_trying_available_filter=1),
        но не указано что нужны офферы с примеркой (trying-available=0 или отсутствует),
        находятся все офферы
        '''
        rearr = '&rearr-factors=market_nordstream=1;market_enable_trying_available_filter=1'
        for trying in ['&trying-available=0', '']:
            response = self.report.request_json(PRIME_REQUEST.format(_Rids.moscow, _Hids.fashion) + rearr + trying)
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'total': _Offers.count,
                    },
                },
            )

    def test_trying_available_filter_hidden_because_only_no_trying_offers(self):
        '''
        Проверяем что если в ответе только оффера без примерки, то фильтр отображать не надо
        '''
        rearr = '&rearr-factors=market_nordstream=1;market_enable_trying_available_filter=1'
        response = self.report.request_json(
            PRIME_REQUEST.format(_Rids.moscow, _Hids.fashion) + rearr + "&offerid=Offer_3P_1___________g"
        )
        self.assertFragmentNotIn(response, {'filters': [{'id': 'trying-available'}]})

    def test_trying_factor(self):
        '''
        Проверяем наличие фактора у офферов с примеркой
        '''
        rearr = '&rearr-factors=market_nordstream=1;market_enable_trying_available_filter=1'
        response = self.report.request_json(
            PRIME_REQUEST.format(_Rids.moscow, _Hids.fashion) + rearr + '&trying-available=1' + '&debug=da'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': len(_Offers.trying_available_offer_indices),
                    'results': [
                        {'debug': {'factors': {'IS_TRYING_AVAILABLE': '1'}}}
                        for index in _Offers.trying_available_offer_indices
                    ],
                },
            },
            preserve_order=False,
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()
