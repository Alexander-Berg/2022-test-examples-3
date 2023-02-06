#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.matcher import Absent
from core.testcase import TestCase, main
from core.types import DynamicBlueGenericBundlesPromos, Promo, PromoType, Region, Shop
from core.types.sku import MarketSku, BlueOffer
from core.types.offer_promo import OffersMatchingRules, PromoCheapestAsGift, PromoRestrictions
from core.types.autogen import b64url_md5

from itertools import count


FEED = 777
nummer = count()


def __blue_offer(price=1000, old_price=1000):
    num = next(nummer)
    return BlueOffer(
        waremd5=b64url_md5(num),
        price=price,
        fesh=FEED,
        feedid=FEED,
        offerid='ССКУ_{}'.format(num),
    )


blue_offer_1 = __blue_offer()
blue_offer_2 = __blue_offer()
blue_offer_3 = __blue_offer()
blue_offer_4 = __blue_offer()


def __msku(offers):
    num = next(nummer)
    return MarketSku(sku=num, hyperid=num, blue_offers=offers if isinstance(offers, list) else [offers])


msku_1 = __msku(blue_offer_1)
msku_2 = __msku(blue_offer_2)
msku_3 = __msku(blue_offer_3)
msku_4 = __msku(blue_offer_4)


# обычное промо, доступно по-умолчанию
enabled_promo = Promo(
    promo_type=PromoType.CHEAPEST_AS_GIFT,
    key=b64url_md5(next(nummer)),
    cheapest_as_gift=PromoCheapestAsGift(
        offer_ids=[
            (FEED, blue_offer_1.offerid),
        ],
        count=3,
        promo_url='url',
        link_text='text',
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [FEED, blue_offer_1.offerid],
            ]
        )
    ],
)

# промо, изначально скрытое флагом disabled_by_default, должно быть доступно через эксп. флаги в запросе
disabled_promo = Promo(
    promo_type=PromoType.CHEAPEST_AS_GIFT,
    shop_promo_id='disabled_promo',
    anaplan_id='anaplan2',
    key=b64url_md5(next(nummer)),
    cheapest_as_gift=PromoCheapestAsGift(
        offer_ids=[
            (FEED, blue_offer_2.offerid),
        ],
        count=3,
        promo_url='url',
        link_text='text',
    ),
    disabled_by_default=True,
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [FEED, blue_offer_2.offerid],
            ]
        )
    ],
)

# промо, ограничено по регионам
whitelist_region_promo = Promo(
    promo_type=PromoType.CHEAPEST_AS_GIFT,
    key=b64url_md5(next(nummer)),
    cheapest_as_gift=PromoCheapestAsGift(
        offer_ids=[
            (FEED, blue_offer_3.offerid),
        ],
        count=3,
        promo_url='url',
        link_text='text',
    ),
    restrictions=PromoRestrictions(
        regions=[213, 2, 134],
        excluded_regions=[10590],
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [FEED, blue_offer_3.offerid],
            ]
        )
    ],
)

blacklist_region_promo = Promo(
    promo_type=PromoType.CHEAPEST_AS_GIFT,
    key=b64url_md5(next(nummer)),
    cheapest_as_gift=PromoCheapestAsGift(
        offer_ids=[
            (FEED, blue_offer_4.offerid),
        ],
        count=3,
        promo_url='url',
        link_text='text',
    ),
    restrictions=PromoRestrictions(
        regions=[],
        excluded_regions=[134, 157],
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [FEED, blue_offer_4.offerid],
            ]
        )
    ],
)


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['enable_fast_promo_matcher=1;enable_fast_promo_matcher_test=1']

        blue_offer_1.promo = [enabled_promo]
        blue_offer_2.promo = [disabled_promo]
        blue_offer_3.promo = [whitelist_region_promo]
        blue_offer_4.promo = [blacklist_region_promo]
        cls.index.regiontree += [
            Region(rid=213, name='Москва', genitive='Москвы', preposition='в ', accusative='Москву', tz_offset=10800),
            Region(rid=2, name='Санкт-Петербург', tz_offset=10800),
            Region(rid=10758, name='Химки', tz_offset=10800),
            Region(rid=157, name='Минск', tz_offset=10800),
            Region(
                rid=134,
                name='Китай',
                tz_offset=28800,
                region_type=Region.COUNTRY,
                genitive="Китая",
                preposition="в",
                accusative="Китай",
                children=[
                    Region(
                        rid=10590, name='Пекин', genitive="Пекина", preposition="в", accusative="Пекин", tz_offset=28800
                    ),
                ],
            ),
        ]

        cls.index.shops += [
            Shop(
                fesh=FEED,
                datafeed_id=FEED,
                priority_region=213,
                regions=[1, 2, 3, 4],
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
            ),
        ]

        cls.index.mskus += [
            msku_1,
            msku_2,
            msku_3,
            msku_4,
        ]

        cls.index.promos += [
            enabled_promo,
            disabled_promo,
            whitelist_region_promo,
            blacklist_region_promo,
        ]

        cls.settings.loyalty_enabled = True
        cls.dynamic.loyalty += [DynamicBlueGenericBundlesPromos(whitelist=[promo.key for promo in cls.index.promos])]

    def __should(self, promo, msku, main_offer, extra_rearr='', rids=0):
        for place in ['sku_offers', 'prime']:
            params = 'place={place}&rids={rids}&regset=1&pp=18&market-sku={msku}&rgb=blue&rearr-factors={extra_rearr}'
            response = self.report.request_json(
                params.format(place=place, rids=rids, msku=msku.sku, extra_rearr=extra_rearr)
            )

            # проверяем что в выдаче есть оффер с корректным блоком "promos"
            self.assertFragmentIn(
                response,
                [
                    {
                        'entity': 'offer',
                        'wareId': main_offer.waremd5,
                        'promos': [
                            {
                                'type': promo.type_name,
                                'key': promo.key,
                                'anaplan_id': promo.anaplan_id or Absent(),
                            }
                        ],
                    }
                ],
                allow_different_len=False,
            )

    def __should_not(self, msku, offer, extra_rearr='', rids=0):
        for place in ['sku_offers', 'prime']:
            params = 'debug=1&place={place}&rids={rids}&regset=1&pp=18&market-sku={msku}&rgb=blue&rearr-factors={extra_rearr}&yandexuid=1'
            response = self.report.request_json(
                params.format(place=place, rids=rids, msku=msku.sku, extra_rearr=extra_rearr)
            )
            # блок промо должен отсутстовать
            self.assertFragmentIn(
                response,
                [
                    {
                        'entity': 'offer',
                        'wareId': offer.waremd5,
                        'promos': Absent(),
                    }
                ],
            )

    def test_disabled_by_default(self):
        # по-умолчанию 1е промо работает, 2е - нет
        self.__should(enabled_promo, msku_1, blue_offer_1, '')
        self.__should_not(msku_2, blue_offer_2, '')
        # при задании в параметре shop_promo_id или anaplan_id 2е промо становится доступным
        self.__should(
            disabled_promo,
            msku_2,
            blue_offer_2,
            'promo_enable_by_shop_promo_id=SOMEID,{}'.format(disabled_promo.shop_promo_id),
        )
        self.__should(
            disabled_promo,
            msku_2,
            blue_offer_2,
            'promo_enable_by_anaplan_id={},OTHER_A_ID'.format(disabled_promo.anaplan_id),
        )

    def test_region_restrictions(self):
        """
        Проверяем, что для промо с ограничением по регионам приходит в ответе приходят только в запросе rids из белого списка
        Если белый список пустой, то из черного списка не приходят
        MARKETOUT-43721 добавляем rearr для включение cpa выдачи в других странах
        """
        self.__should(whitelist_region_promo, msku_3, blue_offer_3, rids=213)
        self.__should(whitelist_region_promo, msku_3, blue_offer_3, rids=134, extra_rearr='cpa_enabled_countries=134')
        # Черный список в приоритете, поэтому дочерний регион из черного списка не попадает в выдачу, даже если родительский есть в белом
        self.__should_not(msku_3, blue_offer_3, rids=10590, extra_rearr='cpa_enabled_countries=134')
        self.__should_not(msku_3, blue_offer_3, rids=0)

        # Поскольку белый список отсутствует, то разрешены все регионы, кроме региона из черного списка
        self.__should(blacklist_region_promo, msku_4, blue_offer_4, rids=0)
        self.__should(blacklist_region_promo, msku_4, blue_offer_4, rids=2)
        self.__should_not(msku_4, blue_offer_4, rids=157)
        # Родительский узел в черном списке, поэтому промо не выбирается
        self.__should_not(msku_4, blue_offer_4, rids=10590, extra_rearr='cpa_enabled_countries=134')


if __name__ == '__main__':
    main()
