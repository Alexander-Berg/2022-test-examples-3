#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.matcher import Absent
from core.testcase import (
    main,
    TestCase,
)
from core.types import DynamicBlueGenericBundlesPromos, DynamicPromo, Promo, PromoType, Shop
from core.types.offer_promo import (
    PromoDirectDiscount,
    PromoBlueSet,
    PromoSaleDetails,
)
from core.types.sku import (
    BlueOffer,
    MarketSku,
)

from market.proto.common.promo_pb2 import ESourceType


DEFAULT_FEED_ID = 777
DEFAULT_SHOP_ID = 7770
DEFAULT_PROMO_QUANTITY_LIMIT = 3


source_type_str = {
    ESourceType.UNKNOWN: 'UNKNOWN',
    ESourceType.ANAPLAN: 'ANAPLAN',
    ESourceType.PARTNER_SOURCE: 'PARTNDER_SOURCE',
    ESourceType.CATEGORYIFACE: 'CATEGORYIFACE',
    ESourceType.DCO_3P_DISCOUNT: 'DCO_3P_DISCOUNT',
    ESourceType.DCO_RECOMMEND: 'DCO_RECOMMEND',
    ESourceType.LOYALTY: 'LOYALTY',
    ESourceType.ROBOT: 'ROBOT',
}


def build_ware_md5(offerid):
    insert = offerid.ljust(14, "_")
    return "Sku{}Goleg".format(insert)


class _Shops(object):
    shop_pr = Shop(
        fesh=DEFAULT_SHOP_ID,
        datafeed_id=DEFAULT_FEED_ID,
        priority_region=213,
        supplier_type=Shop.THIRD_PARTY,
        blue=Shop.BLUE_REAL,
    )


def create_blue_offer(offerid, price, promo=None, fesh=DEFAULT_SHOP_ID, feed=DEFAULT_FEED_ID):
    params = dict(
        waremd5=build_ware_md5(offerid),
        price=price,
        fesh=fesh,
        feedid=feed,
        promo=promo,
        offerid='shop_sku_{}'.format(offerid),
    )

    return BlueOffer(**params)


class _Offers(object):
    blue_offer_pr = create_blue_offer('offer1', price=100)
    blue_offer_pr_secondary = create_blue_offer('offer2', price=100)
    blue_offer_dd_pr = create_blue_offer('offer3_dd', price=100)


def create_msku(sku, offers_list, hid=None):
    msku_args = dict(sku=sku, hyperid=2, blue_offers=offers_list)

    return MarketSku(**msku_args)


def create_promo_dd(offer, key, source_type):
    items = [
        {
            'feed_id': DEFAULT_FEED_ID,
            'offer_id': offer.offerid,
            'discount_price': {
                'value': 40,
                'currency': 'RUR',
            },
            'old_price': {
                'value': 100,
                'currency': 'RUR',
            },
        }
    ]

    if source_type == ESourceType.DCO_3P_DISCOUNT:
        items[0]['max_discount'] = {'value': 70, 'currency': 'RUR'}
        items[0]['max_discount_percent'] = 70.0

    return Promo(
        promo_type=PromoType.DIRECT_DISCOUNT,
        feed_id=888,
        key=key,
        url='http://pokupky.yandex.ru/{}'.format(key),
        source_type=source_type,
        direct_discount=PromoDirectDiscount(
            items=items,
            allow_berubonus=True,
            allow_promocode=True,
        ),
    )


class _Mskus(object):
    msku_pr1 = create_msku(100900, [_Offers.blue_offer_pr])
    msku_pr2 = create_msku(100901, [_Offers.blue_offer_pr_secondary])
    msku_pr3 = create_msku(100902, [_Offers.blue_offer_dd_pr])


class _Promos(object):

    promos_same_type = []
    for source_type, source_name in source_type_str.items():
        promos_same_type.append(
            create_promo_dd(_Offers.blue_offer_dd_pr, 'direct_discount_{}'.format(source_name), source_type)
        )

    promos_different_types = [
        Promo(
            promo_type=PromoType.SECRET_SALE,
            key='secret_sale_pr',
            source_promo_id='secret_sale_pr',
            source_type=ESourceType.LOYALTY,
            secret_sale_details=PromoSaleDetails(msku_list=[int(_Mskus.msku_pr1.sku)], discount_percent_list=[10]),
        ),
        Promo(
            promo_type=PromoType.BLUE_SET,
            feed_id=DEFAULT_FEED_ID,
            key='blue_set_pr',
            url='blue_set_pr.ru',
            source_type=ESourceType.PARTNER_SOURCE,
            blue_set=PromoBlueSet(
                sets_content=[
                    {
                        'items': [
                            {'offer_id': _Offers.blue_offer_pr.offerid},
                            {'offer_id': _Offers.blue_offer_pr_secondary.offerid},
                        ],
                    }
                ],
            ),
        ),
    ]


source_type_business_priority = [
    [ESourceType.PARTNER_SOURCE],
    [
        ESourceType.ANAPLAN,
        ESourceType.ROBOT,
        ESourceType.CATEGORYIFACE,
        ESourceType.LOYALTY,
        ESourceType.DCO_RECOMMEND,
        ESourceType.DCO_3P_DISCOUNT,
        ESourceType.UNKNOWN,
    ],
]

source_type_internal_priority = [
    ESourceType.PARTNER_SOURCE,
    ESourceType.ANAPLAN,
    ESourceType.ROBOT,
    ESourceType.CATEGORYIFACE,
    ESourceType.LOYALTY,
    ESourceType.DCO_RECOMMEND,
    ESourceType.DCO_3P_DISCOUNT,
    ESourceType.UNKNOWN,
]


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['enable_fast_promo_matcher=0;enable_fast_promo_matcher_test=0']

        _Offers.blue_offer_pr.promo = _Promos.promos_different_types
        _Offers.blue_offer_dd_pr.promo = _Promos.promos_same_type

        cls.index.shops += [
            _Shops.shop_pr,
        ]

        cls.index.mskus += [
            _Mskus.msku_pr1,
            _Mskus.msku_pr2,
            _Mskus.msku_pr3,
        ]

        cls.settings.loyalty_enabled = True
        cls.dynamic.loyalty += [DynamicBlueGenericBundlesPromos(whitelist=[p.key for p in _Promos.promos_same_type])]
        cls.dynamic.loyalty += [
            DynamicBlueGenericBundlesPromos(whitelist=[p.key for p in _Promos.promos_different_types])
        ]

    def build_request(self, place, msku, rearr_flags):
        request = "place={place}&rgb=blue&numdoc=100&regset=0&pp=18&yandexuid=1".format(place=place)
        request += '&&allowed-promos=secret_sale_pr'  # для закрытых распродаж
        request += '&rearr-factors={}'.format(";".join("{}={}".format(key, val) for key, val in rearr_flags.items()))
        request += "&market-sku={msku}".format(msku=msku.sku)
        return request

    def blue_offer_checker(self, msku, offer, place, expected_promos, rearr_flag):
        req = self.build_request(place, msku, rearr_flag)
        response = self.report.request_json(req)
        res_promos = (
            list(map(lambda p: {'key': p.key, 'type': p.type_name}, expected_promos)) if expected_promos else Absent()
        )

        self.assertFragmentIn(
            response,
            {
                'offers': {
                    'items': [
                        {
                            'entity': 'offer',
                            'wareId': offer.waremd5,
                            'promos': res_promos,
                        }
                    ]
                }
            },
            allow_different_len=False,
        )

    def check_promos(self, msku, offer, expected_promos):
        flags = {"market_enable_multipromo": 1, "market_promo_quantity_limit": 1}

        for place in ('prime', 'sku_offers'):
            self.blue_offer_checker(msku, offer, place, expected_promos, flags)

    def test_same_promo_type(self):
        """
        blue_offer_pr - оффер со всеми доступными акциями
        Проверяем, что из нескольких акций одного типа всегда будет возвращаться самая приоритетная по типу источника
        """

        self.dynamic.market_dynamic.disabled_promos = []
        for source_type in source_type_internal_priority:
            promos = [promo for promo in _Promos.promos_same_type if promo.source_type == source_type]
            self.check_promos(_Mskus.msku_pr3, _Offers.blue_offer_dd_pr, promos)
            self.dynamic.market_dynamic.disabled_promos += [DynamicPromo(promo_key=promo.key) for promo in promos]

        # Если все акции выключены, то в полях promo и promos будет пусто
        self.check_promos(_Mskus.msku_pr3, _Offers.blue_offer_dd_pr, [])

    def test_different_promo_types(self):
        """
        blue_offer_pr - оффер со всеми доступными акциями
        Проверяем, что из нескольких акций разного типа всегда сперва будут возвращаться акции из партнетского интерфейса
        """

        self.check_promos(
            _Mskus.msku_pr1,
            _Offers.blue_offer_pr,
            [p for p in _Promos.promos_different_types if p.type_name == PromoType.BLUE_SET],
        )
        pass


if __name__ == '__main__':
    main()
