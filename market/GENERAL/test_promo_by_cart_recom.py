#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.bigb import BeruPersHistoryModelViewLastTimeCounter, ModelLastSeenEvent
from core.dj import DjModel
from core.testcase import TestCase, main
from core.types import (
    BlueOffer,
    DeliveryBucket,
    HyperCategory,
    MarketSku,
    Model,
    PromoByCart,
    Shop,
    YamarecCategoryPartition,
    YamarecCategoryRanksPartition,
    YamarecDjDefaultModelsList,
    YamarecPlace,
    YamarecSettingPartition,
)
from time import time

NOW = int(time())
DAY = 86400

SKU_SET = {60000, 60100, 60110, 60120, 60130, 60200, 60300, 60400, 61000}
MODEL_SET = {sku / 10 for sku in SKU_SET}
CATEGORY_SET = {model / 10 for model in MODEL_SET}


class T(TestCase):
    """
    MARKETOUT-28104
    Main functional testing done at `test_promo_by_cart.py`.
    Here I only check that places related to recommendations respect new flag.
    """

    def check_query(self, request):
        request += (
            '&rgb=blue&yandexuid=400&cart=waremd5_________600000'
            '&rearr-factors=market_promo_by_user_cart_hids=1'
            ';market_promo_cart_force_pricedrop_return_nothing=0'
        )
        # TODO: удалить rearrs после возвращения дефолтных флагов в репорте
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'promos': [{'type': 'cart-discount'}],
            },
        )

    @classmethod
    def prepare(cls):
        cls.settings.rgb_blue_is_cpa = True
        cls.index.shops += [
            Shop(
                fesh=1111,
                datafeed_id=1111,
                supplier_type=Shop.FIRST_PARTY,
                blue=Shop.BLUE_REAL,
                fulfillment_program=True,
                is_supplier=True,
            )
        ]

        cls.index.hypertree += [HyperCategory(hid=category) for category in CATEGORY_SET]

        cls.index.models += [Model(hyperid=model, hid=model / 10) for ts, model in enumerate(MODEL_SET)]

        cls.index.mskus += [
            MarketSku(
                sku=sku,
                hyperid=sku / 10,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(price=100, discount=10, waremd5='waremd5_________{}'.format(sku * 10), feedid=1111)
                ],
            )
            for sku in SKU_SET
        ]

        cls.index.promos_by_cart += [
            PromoByCart(cart_hid=600, promo_hid=category, percent=25) for category in CATEGORY_SET if category != 600
        ]

        cls.index.delivery_buckets += [DeliveryBucket(bucket_id=1234)]

        ichwill_answer = {
            'models': [str(model) for model in MODEL_SET],
            'timestamps': [str(ts) for ts in range(len(MODEL_SET))],
        }
        cls.recommender.on_request_models_of_interest(user_id='yandexuid:400').respond(ichwill_answer)
        cls.recommender.on_request_models_of_interest(
            user_id='yandexuid:400', item_count=40, with_timestamps=True
        ).respond(ichwill_answer)

        cls.bigb.on_request(yandexuid=400, client='merch-machine').respond(
            counters=[
                BeruPersHistoryModelViewLastTimeCounter(
                    [ModelLastSeenEvent(model_id=model, timestamp=NOW - 1 * DAY) for model in MODEL_SET]
                ),
            ],
        )

    @classmethod
    def prepare_also_viewed(cls):
        cls.recommender.on_request_accessory_models(model_id=600, item_count=1000, version='1').respond(
            {'models': [str(model) for model in MODEL_SET if model != 600]}
        )

        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.ALSO_VIEWED_PRODUCTS,
                kind=YamarecPlace.Type.SETTING,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    YamarecSettingPartition(params={'version': '1'}, splits=[{'split': 'normal'}]),
                ],
            ),
        ]

    @classmethod
    def prepare_commonly_purchased(cls):
        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.COMMONLY_PURCHASED_UNIVERSAL_CATEGORIES,
                kind=YamarecPlace.Type.CATEGORY,
                partitions=[YamarecCategoryPartition(category_list=[610])],
            )
        ]

    @classmethod
    def prepare_dj(cls):
        cls.settings.set_default_reqid = False
        cls.dj.on_request(yandexuid='400').respond(
            [DjModel(id=str(model), title='model#{}'.format(model)) for model in MODEL_SET]
        )

    @classmethod
    def prepare_omm(cls):
        # CRAP: I had problems with properly configuring OMM, so right now i'm
        #       using report fallback mechanism to test generate output.
        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.DJ_DEFAULT_MODELS_LIST,
                kind=YamarecPlace.Type.DJ_DEFAULT_MODELS_LIST,
                partitions=[
                    YamarecDjDefaultModelsList(
                        dj_default_models_list=[[model, 'title', 'picture'] for model in MODEL_SET]
                    ),
                ],
            ),
        ]

    @classmethod
    def prepare_deals(cls):
        # CRAP: Same story here. Using report's fallback to generate output.
        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.COLDSTART_CATEGORIES,
                kind=YamarecPlace.Type.CATEGORY_RANKS,
                partitions=[
                    YamarecCategoryRanksPartition(
                        category_list=list(CATEGORY_SET),
                    ),
                ],
            )
        ]

    def test_also_viewed(self):
        self.check_query('place=also_viewed&hyperid=600&rearr-factors=split=normal')

    def test_attractive_models(self):
        self.check_query('place=blue_attractive_models&rearr-factors=market_dj_exp_for_blue_attractive_models=')
        # See comments inside `prepare_omm`.
        self.check_query('place=blue_attractive_models')
        self.error_log.ignore(code=3772)

    def test_omm_findings(self):
        self.check_query('place=blue_omm_findings&market_dj_exp_for_blue_omm_findings=')
        # See comments inside `prepare_omm`.
        self.check_query('place=blue_omm_findings')
        self.error_log.ignore(code=3772)

    def test_commonly_purchased(self):
        self.check_query('place=commonly_purchased&rearr-factors=turn_on_commonly_purchased=1')

    def test_deals(self):
        # See comments inside `prepare_deals`.
        self.check_query('place=deals')

    def test_popular_products(self):
        self.check_query('place=popular_products')

    def test_products_by_history(self):
        self.check_query('place=products_by_history')
        self.check_query('place=products_by_history&history=blue')


if __name__ == '__main__':
    main()
