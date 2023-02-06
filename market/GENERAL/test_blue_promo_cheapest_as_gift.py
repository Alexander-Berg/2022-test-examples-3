#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.matcher import Absent, NotEmpty
from core.report import REQUEST_TIMESTAMP
from core.testcase import TestCase, main
from core.types import DynamicBlueGenericBundlesPromos, Promo, PromoType, Shop
from core.types.dynamic_filters import DynamicBluePromosBlacklist
from core.types.sku import MarketSku, BlueOffer
from core.types.offer_promo import PromoCheapestAsGift, OffersMatchingRules

from datetime import datetime, timedelta


BLUE = 'blue'
GREEN = 'green'


# участвует в акции 1
blue_offer_1 = BlueOffer(
    waremd5='BlueOffer1-----------w',
    price=1000,
    fesh=777,
    feedid=777,
    offerid='shop_sku_1.яЯя',
)

# участвует в акции 1
blue_offer_2 = BlueOffer(
    waremd5='BlueOffer2-----------w',
    price=1000,
    fesh=888,
    feedid=888,
    offerid='shop_sku_2',
)

# участвует в акции 2
blue_offer_3 = BlueOffer(
    waremd5='BlueOffer3-----------w',
    price=1000,
    fesh=777,
    feedid=777,
    offerid='shop_sku_3',
)

# участвует в акции 3
blue_offer_4 = BlueOffer(
    waremd5='BlueOffer4-----------w',
    price=1000,
    fesh=777,
    feedid=777,
    offerid='shop_sku_4',
)

# не участвует в акции
blue_offer_5 = BlueOffer(
    waremd5='BlueOffer5-----------w',
    price=1000,
    fesh=777,
    feedid=777,
    offerid='shop_sku_5',
)

# для теста приоритета, участвует в акции типа cheapest-as-gift и в акции типа bundle
blue_offer_7 = BlueOffer(
    waremd5='BlueOffer7-----------w',
    price=1000,
    fesh=777,
    feedid=777,
    offerid='shop_sku_7',
)

# для теста приоритета, участвует в акции типа bundle
blue_offer_8 = BlueOffer(
    waremd5='BlueOffer8-----------w',
    price=1000,
    fesh=777,
    feedid=777,
    offerid='shop_sku_8',
)

# участвует в акции которая не в белом списке
blue_offer_9 = BlueOffer(
    waremd5='BlueOffer9-----------w',
    price=1000,
    fesh=777,
    feedid=777,
    offerid='shop_sku_9',
)

# для акции 3=4
blue_offer_A = BlueOffer(
    waremd5='BlueOfferA-----------w',
    price=1000,
    fesh=777,
    feedid=777,
    offerid='shop_sku_A',
)


msku_1 = MarketSku(title='blue market sku1', hyperid=1, sku=110011, blue_offers=[blue_offer_1])

msku_2 = MarketSku(title='blue market sku2', hyperid=2, sku=110012, blue_offers=[blue_offer_2])

msku_3 = MarketSku(title='blue market sku3', hyperid=2, sku=110013, blue_offers=[blue_offer_3])

msku_4 = MarketSku(title='blue market sku4', hyperid=2, sku=110014, blue_offers=[blue_offer_4])

msku_5 = MarketSku(title='blue market sku5', hyperid=2, sku=110015, blue_offers=[blue_offer_5])

msku_7 = MarketSku(title='blue market sku7', hyperid=2, sku=110017, blue_offers=[blue_offer_7])

msku_8 = MarketSku(title='blue market sku8', hyperid=2, sku=110018, blue_offers=[blue_offer_8])

msku_9 = MarketSku(title='blue market sku9', hyperid=2, sku=110019, blue_offers=[blue_offer_9])

msku_A = MarketSku(hyperid=2, sku=110020, blue_offers=[blue_offer_A])


now = datetime.fromtimestamp(REQUEST_TIMESTAMP)  # нет рандома - нет нормального времени
delta_small = timedelta(hours=5)  # похоже что лайт-тесты криво работают с временной зоной…


# действующая акция
promo1 = Promo(
    promo_type=PromoType.CHEAPEST_AS_GIFT,
    key='JVvklxUgdnawSJPG4UhZ-1',
    url='http://localhost.ru/',
    cheapest_as_gift=PromoCheapestAsGift(
        offer_ids=[
            (777, blue_offer_1.offerid),
            (888, blue_offer_2.offerid),
        ],
        count=3,
        promo_url='url',
        link_text='text',
        allow_berubonus=False,
        allow_promocode=False,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            mskus=[
                msku_1,
                msku_2,
            ]
        ),
    ],
)

# уже закончилась
promo2 = Promo(
    promo_type=PromoType.CHEAPEST_AS_GIFT,
    key='JVvklxUgdnawSJPG4UhZ-2',
    url='http://localhost.ru/',
    end_date=now - delta_small,
    cheapest_as_gift=PromoCheapestAsGift(
        offer_ids=[
            (777, blue_offer_3.offerid),
        ],
        count=1,
        promo_url='url',
        link_text='text',
        allow_berubonus=False,
        allow_promocode=False,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            mskus=[
                msku_3,
            ]
        ),
    ],
)

# ещё не началась
promo3 = Promo(
    promo_type=PromoType.CHEAPEST_AS_GIFT,
    key='JVvklxUgdnawSJPG4UhZ-3',
    url='http://localhost.ru/',
    start_date=now + delta_small,
    cheapest_as_gift=PromoCheapestAsGift(
        offer_ids=[
            (777, blue_offer_4.offerid),
        ],
        count=1,
        promo_url='url',
        link_text='text',
        allow_berubonus=False,
        allow_promocode=False,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            mskus=[
                msku_4,
            ]
        ),
    ],
)

# действующая акция для теста приоритетов
promo4 = Promo(
    promo_type=PromoType.CHEAPEST_AS_GIFT,
    key='JVvklxUgdnawSJPG4UhZ-4',
    url='http://localhost.ru/',
    cheapest_as_gift=PromoCheapestAsGift(
        offer_ids=[
            (777, blue_offer_7.offerid),
        ],
        count=3,
        promo_url='url',
        link_text='text',
        allow_berubonus=False,
        allow_promocode=False,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            mskus=[
                msku_7,
            ]
        ),
    ],
)

# акция не включена в белом списке лоялти
promo5 = Promo(
    promo_type=PromoType.CHEAPEST_AS_GIFT,
    key='JVvklxUgdnawSJPG4UhZ-5',
    url='http://localhost.ru/',
    cheapest_as_gift=PromoCheapestAsGift(
        offer_ids=[
            (777, blue_offer_9.offerid),
        ],
        count=1,
        promo_url='url',
        link_text='text',
        allow_berubonus=False,
        allow_promocode=False,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            mskus=[
                msku_9,
            ]
        ),
    ],
)

# акция 3=4
promo6 = Promo(
    promo_type=PromoType.CHEAPEST_AS_GIFT,
    key='JVvklxUgdnawSJPG4UhZ-6',
    url='http://localhost.ru/',
    cheapest_as_gift=PromoCheapestAsGift(
        offer_ids=[
            (777, blue_offer_A.offerid),
        ],
        count=4,
        promo_url='url',
        link_text='text',
        allow_berubonus=False,
        allow_promocode=False,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            mskus=[
                msku_A,
            ]
        ),
    ],
)


class T(TestCase):
    blue_offer_1.promo = [promo1]
    blue_offer_2.promo = [promo1]
    blue_offer_3.promo = [promo2]
    blue_offer_4.promo = [promo3]
    blue_offer_7.promo = [promo4]
    blue_offer_9.promo = [promo5]
    blue_offer_A.promo = [promo6]

    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['enable_fast_promo_matcher=1;enable_fast_promo_matcher_test=1']

        cls.index.shops += [
            Shop(fesh=777, datafeed_id=777, priority_region=213, supplier_type=Shop.THIRD_PARTY, blue=Shop.BLUE_REAL),
            Shop(fesh=888, datafeed_id=888, priority_region=213, supplier_type=Shop.THIRD_PARTY, blue=Shop.BLUE_REAL),
        ]

        cls.index.mskus += [
            msku_1,
            msku_2,
            msku_3,
            msku_4,
            msku_5,
            msku_7,
            msku_8,
            msku_9,
            msku_A,
        ]

        cls.index.promos += [promo1, promo2, promo3, promo4, promo5, promo6]

        cls.settings.loyalty_enabled = True
        cls.dynamic.loyalty += [DynamicBlueGenericBundlesPromos(whitelist=[promo1.key, promo4.key, promo6.key])]

    def __should_not(self, msku, waremd5):
        for rgb in ('blue', 'green', 'green_with_blue'):
            for place in ('sku_offers', 'prime', 'offerinfo'):
                params = 'place={place}&rids=0&regset=1&pp=18&market-sku={msku}&rgb={rgb}&yandexuid=1'
                response = self.report.request_json(params.format(place=place, msku=msku, rgb=rgb))
                # блок промо должен отсутстовать
                self.assertFragmentIn(
                    response,
                    [
                        {
                            'entity': 'offer',
                            'wareId': waremd5,
                            'promos': Absent(),
                        }
                    ],
                )

    def __should(self, promo, msku, offer):
        for rgb in ('blue', 'green', 'green_with_blue'):
            for place in ('sku_offers', 'prime', 'offerinfo'):
                # выбор байбокса нужен детерминированный, для этого фиксируем yandexuid (MARKETOUT-16443)
                params = 'place={place}&rids=0&regset=1&pp=18&market-sku={msku}&rgb={rgb}&yandexuid=1'
                response = self.report.request_json(params.format(place=place, msku=msku.sku, rgb=rgb))

                # проверяем что в выдаче есть оффер с корректным блоком "promos"
                self.assertFragmentIn(
                    response,
                    [
                        {
                            'entity': 'offer',
                            'wareId': offer.waremd5,
                            'promos': [
                                {
                                    'type': promo.type_name,
                                    'key': promo.key,
                                    'url': promo.url,
                                    'startDate': NotEmpty() if promo.start_date else Absent(),
                                    'endDate': NotEmpty() if promo.end_date else Absent(),
                                    'extra': Absent(),
                                    'parameters': Absent(),
                                    'itemsInfo': {
                                        'count': promo.cheapest_as_gift.count,
                                        'promo_url': promo.cheapest_as_gift.promo_url,
                                        'link_text': promo.cheapest_as_gift.link_text,
                                        'constraints': {
                                            'allow_berubonus': promo.cheapest_as_gift.allow_berubonus,
                                            'allow_promocode': promo.cheapest_as_gift.allow_promocode,
                                        },
                                    },
                                }
                            ],
                        }
                    ],
                    allow_different_len=False,
                )

    def test_promo_cheapest_as_gift(self):
        self.__should(promo1, msku_1, blue_offer_1)
        self.__should(promo1, msku_2, blue_offer_2)

        self.dynamic.loyalty += [
            DynamicBluePromosBlacklist(
                blacklist=[
                    (777, blue_offer_1.offerid),
                ]
            )
        ]
        self.__should_not(msku_1.sku, blue_offer_1.waremd5)
        self.__should(promo1, msku_2, blue_offer_2)

    def test_promo_inactive(self):
        # проверяем отключение акций по времени
        self.__should_not(msku_3.sku, blue_offer_3.waremd5)
        self.__should_not(msku_4.sku, blue_offer_4.waremd5)
        # проверяем что у оффера без акции нет блока promo
        self.__should_not(msku_5.sku, blue_offer_5.waremd5)
        # проверяем акция не работает без белого списка
        self.__should_not(msku_9.sku, blue_offer_9.waremd5)

    def test_34(self):
        # акции с кол-вом товаров 3+
        params = 'place={place}&rids=0&regset=1&pp=18&market-sku={msku}&rgb=blue'
        response = self.report.request_json(params.format(place='offerinfo', msku=msku_A.sku))
        self.assertFragmentIn(
            response,
            [
                {
                    'entity': 'offer',
                    'wareId': blue_offer_A.waremd5,
                    'promos': [
                        {
                            'type': promo6.type_name,
                        }
                    ],
                }
            ],
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()
