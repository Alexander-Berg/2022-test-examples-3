#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.matcher import Absent
from core.testcase import TestCase, main
from core.types import Offer, Shop, HyperCategory, Model
from core.types.autogen import b64url_md5
from core.types.offer_promo import (
    Promo,
    PromoType,
    OffersMatchingRules,
    MechanicsPaymentType,
)
from core.types.hypercategory import (
    EATS_CATEG_ID,
    CategoryStreamRecord,
    Stream,
)
from itertools import count


class _Const:
    nummer = count()
    food_offer_id = 'food_offer'
    usual_offer_id = 'usual_offer'
    food_category = EATS_CATEG_ID
    food_model_id = 1044


class _Shops:
    usual_shop = Shop(
        fesh=42,
        datafeed_id=4242,
        priority_region=213,
        regions=[213],
        name='usual_shop',
        cpa=Shop.CPA_REAL,
        subsidies=Shop.SUBSIDIES_ON,
    )

    food_shop = Shop(
        fesh=43,
        datafeed_id=4343,
        priority_region=213,
        regions=[213],
        name='food_shop',
        cpa=Shop.CPA_REAL,
        subsidies=Shop.SUBSIDIES_ON,
    )


class _Promos:
    # Действующая акция - промокод на скидку в 15%
    promocode = Promo(
        promo_type=PromoType.PROMO_CODE,
        promo_code='usual_promocode',
        description='usual_promocode',
        discount_value=15,
        key=b64url_md5(next(_Const.nummer)),
        url='http://promocode.com/',
        landing_url='http://promocode.com/',
        mechanics_payment_type=MechanicsPaymentType.CPA,
        shop_promo_id='promocode',
        promo_internal_priority=4,
        offers_matching_rules=[
            OffersMatchingRules(
                feed_offer_ids=[
                    [_Shops.food_shop.datafeed_id, 'offer_id_{}'.format(_Const.food_offer_id)],
                    [_Shops.usual_shop.datafeed_id, 'offer_id_{}'.format(_Const.usual_offer_id)],
                ]
            )
        ],
    )


class _Offers:
    usual_offer = Offer(
        title='usual_offer',
        offerid=_Const.usual_offer_id,
        hyperid=104,
        shop=_Shops.usual_shop,
        waremd5=Offer.generate_waremd5('usual_offer'),
        price=1000,
        cpa=Offer.CPA_REAL,
        promo=_Promos.promocode,
        is_eda=False,
    )

    food_offer = Offer(
        title='food_offer',
        offerid=_Const.food_offer_id,
        hyperid=_Const.food_model_id,
        shop=_Shops.food_shop,
        waremd5=Offer.generate_waremd5('food_offer'),
        price=1000,
        cpa=Offer.CPA_REAL,
        promo=_Promos.promocode,
        is_eda=True,
    )


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.market_access_settings.enabled = True
        cls.settings.market_access_settings.download_catstreams = True
        cls.index.category_streams += [
            CategoryStreamRecord(_Const.food_category, Stream.FMCG.value),
        ]
        cls.index.shops += [
            _Shops.usual_shop,
            _Shops.food_shop,
        ]

        cls.index.offers += [
            _Offers.food_offer,
            _Offers.usual_offer,
        ]

        cls.index.promos += [
            _Promos.promocode,
        ]

        cls.index.hypertree += [
            HyperCategory(
                hid=_Const.food_category,
                name='Еда',
            ),
        ]

        cls.index.models += [
            Model(hyperid=_Const.food_model_id, hid=_Const.food_category, title='food_model'),
        ]

    def test_food_offers_with_promo(self):
        '''
        Проверяем, что для офферов еды не будет промо на выдаче
        '''
        base_request = 'place=offerinfo&rids=213&regset=1&offerid={}&enable-foodtech-offers=eda_retail'
        # для обычного оффера, который не еда промокод будет на выдаче
        response = self.report.request_json(base_request.format(_Offers.usual_offer.waremd5))
        self.assertFragmentIn(
            response,
            [
                {
                    'wareId': _Offers.usual_offer.waremd5,
                    'promos': [
                        {
                            'key': _Promos.promocode.key,
                        }
                    ],
                }
            ],
            allow_different_len=False,
        )
        # для оффера еды промокода нет на выдаче без флагов
        response = self.report.request_json(base_request.format(_Offers.food_offer.waremd5))
        self.assertFragmentIn(
            response,
            [
                {
                    'wareId': _Offers.food_offer.waremd5,
                    'promos': Absent(),
                }
            ],
        )
        # добавляем флаг - разрешаем промокод, и он прорастает на выдачу
        # тип PROMO_CODE это 2 ^ 7 = 128
        response = self.report.request_json(
            base_request.format(_Offers.food_offer.waremd5)
            + '&rearr-factors=market_enabled_promo_types_for_eats_offers=128'
        )
        self.assertFragmentIn(
            response,
            [
                {
                    'wareId': _Offers.food_offer.waremd5,
                    'promos': [
                        {
                            'key': _Promos.promocode.key,
                        }
                    ],
                }
            ],
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()
