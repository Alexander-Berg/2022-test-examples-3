#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

import time

from core.bigb import (
    ModelLastSeenEvent,
    BigBKeyword,
    CategoryLastSeenEvent,
    CategoryLastOrderEvent,
    BeruPersHistoryModelViewLastTimeCounter,
    BeruPersHistoryCategoryViewLastTimeCounter,
    BeruCategoryOrderLastTimeCounter,
    WeightedValue,
)

from core.types import (
    Currency,
    DeliveryBucket,
    DeliveryOption,
    DynamicBlueGenericBundlesPromos,
    DynamicDeliveryServiceInfo,
    DynamicWarehouseInfo,
    DynamicWarehousesPriorityInRegion,
    HyperCategory,
    HyperCategoryType,
    Model,
    OfferDimensions,
    Promo,
    PromoType,
    Region,
    RegionalDelivery,
    Shop,
    Tax,
    Vat,
    YamarecMatchingPartition,
    YamarecPlace,
    YamarecSettingPartition,
)
from core.testcase import TestCase, main
from core.types.sku import MarketSku, BlueOffer
from core.types.offer_promo import make_generic_bundle_content, PromoBlueSet, OffersMatchingRules
from core.dj import DjModel
import copy


BLUE = 'blue'
GREEN = 'green'
GREEN_WITH_BLUE = 'green_with_blue'

# основной оффер
PRIMARY_OFFER = BlueOffer(
    price=500,
    vat=Vat.VAT_10,
    offerid="Blue_offer_Feed_4_sku14",
    feedid=4,
    discount=10,
    waremd5="h_fuT9tXvf1SF5lHIRt-uw",
    weight=1,
    dimensions=OfferDimensions(length=10, width=10, height=10),
)
PRIMARY_OFFER_HYPERID = 14
PRIMARY_OFFER_HID = 201

# подарочный оффер
SECONDARY_OFFER = BlueOffer(
    price=11,
    feedid=4,
    offerid='Blue_offer_Feed_4_sku15',
    waremd5="Kj1tbMS153I4wfwbts3WgQ",
    weight=1,
    dimensions=OfferDimensions(length=10, width=10, height=10),
)
SECONDARY_OFFER_HYPER_ID = 15

OFFER_SET1 = BlueOffer(
    price=500,
    offerid="Blue_offer_Feed_4_sku_set",
    feedid=4,
    waremd5="SET1-9tXvf1SF5lHIRt-uw",
    weight=1,
    dimensions=OfferDimensions(length=10, width=10, height=10),
)
OFFER_SET1_HYPERID = 16
OFFER_SET1_HID = 300

OFFER_SET_SECONDARY = BlueOffer(
    price=50,
    offerid="Blue_offer_Feed_4_sku_set_secondary",
    feedid=4,
    waremd5="SET1-sec-f1SF5lHIRt-uw",
    weight=1,
    dimensions=OfferDimensions(length=10, width=10, height=10),
)
OFFER_SET_SECONDARY_HYPERID = 17
OFFER_SET_SECONDARY_HID = 301


PARENT_HID = 200

# ключ промоакции
PROMO_KEY = "JVvklxUgdnawSJPG4UhZ-1"
NOW = int(time.time())
DAY = 86400

DEFAULT_PROFILE = [
    BigBKeyword(
        id=BigBKeyword.GENDER,
        weighted_uint_values=[
            WeightedValue(value=BigBKeyword.GENDER_MALE, weight=621947),
            WeightedValue(value=BigBKeyword.GENDER_FEMALE, weight=375515),
        ],
    ),
]

UIDS = [1001, 700, 702, 5]


class T(TestCase):
    def assert_has_generic_bundle(self, response):
        self.assertFragmentIn(
            response,
            {
                'offers': [
                    {
                        'entity': 'offer',
                        'wareId': SECONDARY_OFFER.waremd5,
                    }
                ]
            },
        )
        # проверяем что в выдаче есть оффер с корректным блоком "promo"
        self.assertFragmentIn(
            response,
            [
                {
                    'entity': 'offer',
                    'wareId': PRIMARY_OFFER.waremd5,
                    'promos': [
                        {
                            'type': 'generic-bundle',
                            'key': PROMO_KEY,
                        }
                    ],
                }
            ],
        )

    def assert_has_blue_set(self, response):
        self.assertFragmentIn(
            response,
            {
                'offers': [
                    {
                        'entity': 'offer',
                        'wareId': OFFER_SET_SECONDARY.waremd5,
                    }
                ]
            },
        )
        # проверяем что в выдаче есть оффер с корректным блоком "promo"
        self.assertFragmentIn(
            response,
            [
                {
                    'entity': 'offer',
                    'wareId': OFFER_SET1.waremd5,
                    'promos': [
                        {
                            'type': 'blue-set',
                            'key': 'BLUE_SET',
                        }
                    ],
                }
            ],
        )

    @classmethod
    def prepare_popular_products(self):
        self.settings.rgb_blue_is_cpa = True

        """
        Подготовка данных для популярных товаров
        """
        self.settings.default_search_experiment_flags += [
            'enable_fast_promo_matcher=1;enable_fast_promo_matcher_test=1'
        ]
        # НЕ делайте так в новых тестах!
        # Походов в КД на проде уже нет, пожалуйста, проверяйте новую функциональность, создавая доставку через комбинатор
        self.settings.default_search_experiment_flags += ['force_disable_delivery_calculator_requests=0']

        self.settings.loyalty_enabled = True
        self.settings.disable_random = False  # нужно генерировать разные showUid для основного товара и подарка

        self.index.creation_time = 488419200
        self.index.regiontree += [
            Region(rid=213, name='Москва'),
        ]

        self.index.hypertree += [
            HyperCategory(
                hid=PARENT_HID,
                children=[
                    HyperCategory(hid=PRIMARY_OFFER_HID, output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=OFFER_SET1_HID, output_type=HyperCategoryType.GURU),
                ],
            ),
        ]

        # shops
        self.index.shops += [
            # blue shop
            Shop(
                fesh=1,
                datafeed_id=1,
                priority_region=213,
                name='blue_shop_1',
                currency=Currency.RUR,
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                tax_system=Tax.OSN,
                cpa=Shop.CPA_REAL,
                supplier_type=Shop.FIRST_PARTY,
            ),
            Shop(
                fesh=4, datafeed_id=4, priority_region=213, currency=Currency.RUR, blue=Shop.BLUE_REAL, warehouse_id=145
            ),
        ]

        self.index.models += [
            Model(hyperid=PRIMARY_OFFER_HYPERID, hid=PRIMARY_OFFER_HID),
            Model(hyperid=SECONDARY_OFFER_HYPER_ID, hid=SECONDARY_OFFER_HYPER_ID),
            Model(hyperid=OFFER_SET1_HYPERID, hid=OFFER_SET1_HID),
            Model(hyperid=OFFER_SET_SECONDARY_HYPERID, hid=OFFER_SET_SECONDARY_HID),
            Model(hyperid=6000, hid=600),
            Model(hyperid=6001, hid=600),
            Model(hyperid=1886701, hid=188670101),
        ]

        promo = Promo(
            promo_type=PromoType.GENERIC_BUNDLE,
            feed_id=4,
            key=PROMO_KEY,
            url='http://localhost.ru/',
            generic_bundles_content=[
                make_generic_bundle_content(PRIMARY_OFFER.offerid, SECONDARY_OFFER.offerid, 1),
            ],
            offers_matching_rules=[
                OffersMatchingRules(
                    feed_offer_ids=[
                        [4, PRIMARY_OFFER.offerid],
                    ]
                ),
            ],
        )
        promo_blue_set = Promo(
            promo_type=PromoType.BLUE_SET,
            feed_id=4,
            key='BLUE_SET',
            url='http://localhost.ru/',
            blue_set=PromoBlueSet(
                sets_content=[
                    {
                        'items': [
                            {'offer_id': OFFER_SET1.offerid},
                            {'offer_id': OFFER_SET_SECONDARY.offerid},
                        ],
                    }
                ],
            ),
            offers_matching_rules=[
                OffersMatchingRules(
                    feed_offer_ids=[
                        [4, OFFER_SET1.offerid],
                        [4, OFFER_SET_SECONDARY.offerid],
                    ]
                ),
            ],
        )
        PRIMARY_OFFER.promo = [promo]
        OFFER_SET1.promo = [promo_blue_set]
        OFFER_SET_SECONDARY.promo = [promo_blue_set]

        self.index.promos += [promo, promo_blue_set]

        self.dynamic.loyalty += [DynamicBlueGenericBundlesPromos(whitelist=[promo.key, promo_blue_set.key])]
        self.index.mskus += [
            MarketSku(
                ts=14000,
                hyperid=PRIMARY_OFFER_HYPERID,
                sku='{}'.format(PRIMARY_OFFER_HYPERID),
                blue_offers=[PRIMARY_OFFER],
            ),
            MarketSku(
                ts=15000,
                hyperid=SECONDARY_OFFER_HYPER_ID,
                sku='{}'.format(SECONDARY_OFFER_HYPER_ID),
                blue_offers=[SECONDARY_OFFER],
            ),
            MarketSku(
                ts=16000, hyperid=OFFER_SET1_HYPERID, sku='{}'.format(OFFER_SET1_HYPERID), blue_offers=[OFFER_SET1]
            ),
            MarketSku(
                ts=17000,
                hyperid=OFFER_SET_SECONDARY_HYPERID,
                sku='{}'.format(OFFER_SET_SECONDARY_HYPERID),
                blue_offers=[OFFER_SET_SECONDARY],
            ),
            MarketSku(hyperid=6000, sku=60000, blue_offers=[BlueOffer()]),
            MarketSku(hyperid=6001, sku=60010, blue_offers=[BlueOffer()]),
        ]

        # personal categories ordering
        ichwill_answer = {
            'models': ['{}'.format(PRIMARY_OFFER_HYPERID)],
            'timestamps': ['1'],
        }
        self.recommender.on_request_models_of_interest(user_id='yandexuid:{}'.format(UIDS[0])).respond(ichwill_answer)
        self.recommender.on_request_models_of_interest(
            user_id='yandexuid:{}'.format(UIDS[0]), item_count=40, with_timestamps=True
        ).respond(ichwill_answer)

        ichwill_answer_set = {
            'models': ['{}'.format(OFFER_SET1_HYPERID)],
            'timestamps': ['1'],
        }
        self.recommender.on_request_models_of_interest(user_id='yandexuid:{}'.format(UIDS[3])).respond(
            ichwill_answer_set
        )
        self.recommender.on_request_models_of_interest(
            user_id='yandexuid:{}'.format(UIDS[3]), item_count=40, with_timestamps=True
        ).respond(ichwill_answer_set)

        self.dynamic.lms += [
            DynamicWarehouseInfo(id=145, home_region=213, holidays_days_set_key=4),
            DynamicWarehousesPriorityInRegion(
                region=213,
                warehouses=[
                    145,
                ],
            ),
            DynamicDeliveryServiceInfo(1, "B_1"),
        ]
        self.index.lms = copy.deepcopy(self.dynamic.lms)

        self.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=71,
                dc_bucket_id=71,
                carriers=[1],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[
                    RegionalDelivery(rid=213, options=[DeliveryOption(price=15, day_from=1, day_to=2)]),
                ],
            )
        ]
        self.delivery_calc.on_request_offer_buckets(weight=2, height=22, length=11, width=11, warehouse_id=145).respond(
            [71], [], []
        )

        self.bigb.on_request(yandexuid=UIDS[0], client='merch-machine').respond(counters=[])
        self.bigb.on_request(yandexuid=UIDS[3], client='merch-machine').respond(counters=[])

    def test_generic_bundles_on_popular_products(self):
        """
        Проверка привязки подарка к ответу плейса popular_products
        """
        for rgb in (BLUE, GREEN, GREEN_WITH_BLUE):
            rearr_flags = "rearr-factors=switch_popular_products_to_dj=0;switch_popular_products_to_dj_no_nid_check=0"
            response = self.report.request_json(
                'place=popular_products&{}&numdoc=8&rids=213&hid={}'
                '&yandexuid={}&rgb={}'.format(rearr_flags, PARENT_HID, UIDS[0], rgb)
            )
            self.assert_has_generic_bundle(response)

    def test_blue_set_on_popular_products(self):
        for rgb in (BLUE, GREEN, GREEN_WITH_BLUE):
            rearr_flags = "rearr-factors=switch_popular_products_to_dj=0;switch_popular_products_to_dj_no_nid_check=0"
            response = self.report.request_json(
                'place=popular_products&{}&numdoc=8&rids=213&hid={}'
                '&yandexuid={}&rgb={}'.format(rearr_flags, PARENT_HID, UIDS[3], rgb)
            )
            self.assert_has_blue_set(response)

    def test_blue_set_on_combine(self):
        offers = ((OFFER_SET1, OFFER_SET1_HYPERID), (OFFER_SET_SECONDARY, OFFER_SET_SECONDARY_HYPERID))

        for rgb in (BLUE, GREEN, GREEN_WITH_BLUE):
            response = self.report.request_json(
                'place=combine&rgb={}&rids=213&offers-list={}&rearr-factors=enable_cart_split_on_combinator=0;market_blender_media_adv_incut_enabled=0'.format(
                    rgb,
                    ','.join('{}:1;msku:{}'.format(offer.waremd5, msku) for offer, msku in offers),
                )
            )

            # проверяем что в выдаче есть офферы с корректным блоком "promo"
            self.assertFragmentIn(
                response,
                {
                    'offers': {
                        'items': [
                            {
                                'entity': 'offer',
                                'wareId': OFFER_SET1.waremd5,
                                'promos': [
                                    {
                                        'type': 'blue-set',
                                        'key': 'BLUE_SET',
                                    }
                                ],
                            },
                            {
                                'entity': 'offer',
                                'wareId': OFFER_SET_SECONDARY.waremd5,
                                'promos': [
                                    {
                                        'type': 'blue-set-secondary',
                                        'key': 'BLUE_SET',
                                    }
                                ],
                            },
                        ]
                    }
                },
            )

    @classmethod
    def prepare_products_by_history(self):
        self.bigb.on_request(yandexuid=UIDS[1], client='merch-machine').respond(
            counters=[
                BeruPersHistoryModelViewLastTimeCounter(
                    [
                        ModelLastSeenEvent(model_id=6000, timestamp=NOW - 2 * DAY),
                        ModelLastSeenEvent(model_id=6001, timestamp=NOW - 2 * DAY),
                        ModelLastSeenEvent(model_id=PRIMARY_OFFER_HYPERID, timestamp=NOW - 2 * DAY),
                    ]
                ),
                BeruPersHistoryCategoryViewLastTimeCounter(
                    [
                        CategoryLastSeenEvent(category_id=600, timestamp=NOW - 2 * DAY),
                        CategoryLastSeenEvent(category_id=601, timestamp=NOW - 2 * DAY),
                    ]
                ),
                BeruCategoryOrderLastTimeCounter(
                    [
                        CategoryLastOrderEvent(category_id=600, timestamp=NOW - 1 * DAY),
                    ]
                ),
            ]
        )
        self.dj.on_request(yandexuid=UIDS[2]).respond(
            models=[
                DjModel(id=PRIMARY_OFFER_HYPERID, title='model'),
                DjModel(id=2287602, title='model'),
                DjModel(id=2287603, title='model'),
                DjModel(id=2287604, title='model'),
            ]
        )

    def test_generic_bundles_on_products_by_history(self):
        """
        Проверка привязки подарка к ответу плейса products_by_history
        """
        for suffix in ['cpa=real', 'rgb=blue']:
            response = self.report.request_json(
                'place=products_by_history&{}&history=blue&yandexuid={}'
                '&rearr-factors=market_disable_dj_for_recent_findings%3D1'.format(suffix, UIDS[1])
            )
            self.assert_has_generic_bundle(response)
            response = self.report.request_json(
                'place=products_by_history&{}&history=blue&yandexuid={}'.format(suffix, UIDS[2])
            )
            self.assert_has_generic_bundle(response)

    @classmethod
    def prepare_blue_attractive_models_and_omm_findings(self):
        self.settings.set_default_reqid = False

        self.bigb.on_request(yandexuid=str(UIDS[2]), client='merch-machine').respond(keywords=DEFAULT_PROFILE)
        self.dj.on_request(exp='blue_attractive_models', yandexuid=str(UIDS[2]),).respond(
            models=[
                DjModel(id=PRIMARY_OFFER_HYPERID, title='model'),
                DjModel(id=2287602, title='model'),
                DjModel(id=2287603, title='model'),
                DjModel(id=2287604, title='model'),
            ]
        )

        self.dj.on_request(exp='blue_omm_findings', yandexuid=str(UIDS[2]),).respond(
            models=[
                DjModel(id=PRIMARY_OFFER_HYPERID, title='model'),
                DjModel(id=2287602, title='model'),
                DjModel(id=2287603, title='model'),
                DjModel(id=2287604, title='model'),
            ]
        )

    def test_generic_bundles_on_blue_attractive_models(self):
        """
        Проверка привязки подарка к ответу плейса blue_attractive_models
        """
        response = self.report.request_json(
            'place=blue_attractive_models' '&rgb=blue&yandexuid={}&numdoc=5'.format(UIDS[2])
        )
        self.assert_has_generic_bundle(response)

    def test_generic_bundles_on_blue_omm_findings(self):
        """
        Проверка привязки подарка к ответу плейса blue_attractive_models
        """
        response = self.report.request_json('place=blue_omm_findings&rgb=blue&yandexuid={}&numdoc=5'.format(UIDS[2]))
        self.assert_has_generic_bundle(response)

    @classmethod
    def prepare_also_viewed_blue(self):
        self.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.ALSO_VIEWED_PRODUCTS_BLUE_MARKET,
                kind=YamarecPlace.Type.SETTING,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    YamarecSettingPartition(params={'version': 'COVIEWS1'}, splits=[{}]),
                ],
            ),
        ]
        self.recommender.on_request_accessory_models(model_id=6000, item_count=1000, version='COVIEWS1').respond(
            {'models': ['{}'.format(PRIMARY_OFFER_HYPERID)]}
        )

    def test_generic_bundles_on_also_viewed_blue(self):
        """
        Проверка привязки подарка к ответу плейса also_viewed на синем маркете
        """
        response = self.report.request_json(
            'place=also_viewed&yandexuid={}&hyperid=6000&numdoc=2&rgb=blue'.format(UIDS[2])
        )
        self.assert_has_generic_bundle(response)

    @classmethod
    def prepare_also_viewed_white(self):
        self.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.ALSO_VIEWED_PRODUCTS,
                kind=YamarecPlace.Type.SETTING,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    YamarecSettingPartition(params={'version': 'COVIEWS1'}, splits=[{}]),
                ],
            ),
        ]
        self.recommender.on_request_accessory_models(model_id=6000, item_count=1000, version='COVIEWS1').respond(
            {'models': ['{}'.format(PRIMARY_OFFER_HYPERID)]}
        )

    def test_generic_bundles_on_also_viewed_white(self):
        """
        Проверка привязки подарка к ответу плейса also_viewed на белом маркете
        """
        for rgb in (GREEN, GREEN_WITH_BLUE):
            response = self.report.request_json(
                'place=also_viewed&yandexuid={}&hyperid=6000&numdoc=2&rgb={}'.format(UIDS[2], rgb)
            )
            self.assert_has_generic_bundle(response)

    @classmethod
    def prepare_product_accessories_blue(self):
        self.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.EXTERNAL_PRODUCT_ACCESSORY_BLUE_MARKET,
                kind=YamarecPlace.Type.SETTING,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    YamarecSettingPartition(
                        params={
                            'version': '11',
                            'use-external': '1',
                            'use-local': '0',
                        },
                        splits=[{'split': '25'}],
                    ),
                    YamarecSettingPartition(
                        params={
                            'version': '11',
                            'use-external': '0',
                            'use-local': '1',
                        },
                        splits=[{'split': '26'}],
                    ),
                ],
            ),
            YamarecPlace(
                name=YamarecPlace.Name.MODEL_CARD_ACCESSORY_BLUE_MARKET,
                kind=YamarecPlace.Type.MATCHING,
                partitions=[
                    YamarecMatchingPartition(
                        name='accessory_blue_market_matching', matching={6001: [PRIMARY_OFFER_HYPERID]}, splits=['*']
                    )
                ],
            ),
        ]
        self.recommender.on_request_accessory_models(model_id=6001, item_count=1000, version='11').respond(
            {'models': ['{}'.format(PRIMARY_OFFER_HYPERID)]}
        )

    def test_generic_bundles_on_product_accessories_blue(self):
        """
        Проверка привязки подарка к ответу плейса product_accessories на синем маркете
        """
        response = self.report.request_json(
            'place=product_accessories_blue&rgb=blue&hyperid=6001&rids=213&rearr'
            '-factors=split=25;market_disable_product_accessories=0'
        )
        self.assert_has_generic_bundle(response)

    @classmethod
    def prepare_product_accessories_white(self):
        self.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.EXTERNAL_PRODUCT_ACCESSORY,
                kind=YamarecPlace.Type.SETTING,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    YamarecSettingPartition(
                        params={
                            'version': '11',
                            'use-external': '1',
                            'use-local': '0',
                        },
                        splits=[{'split': '25'}],
                    ),
                    YamarecSettingPartition(
                        params={
                            'version': '11',
                            'use-external': '0',
                            'use-local': '1',
                        },
                        splits=[{'split': '26'}],
                    ),
                ],
            ),
            YamarecPlace(
                name=YamarecPlace.Name.MODEL_CARD_ACCESSORY,
                kind=YamarecPlace.Type.MATCHING,
                partitions=[
                    YamarecMatchingPartition(
                        name='accessory_white_market_matching', matching={6001: [PRIMARY_OFFER_HYPERID]}, splits=['*']
                    )
                ],
            ),
        ]
        self.recommender.on_request_accessory_models(model_id=6001, item_count=1000, version='11').respond(
            {'models': ['{}'.format(PRIMARY_OFFER_HYPERID)]}
        )

    def test_generic_bundles_on_product_accessories_white(self):
        """
        Проверка привязки подарка к ответу плейса product_accessories на белом маркете
        """
        for rgb in (GREEN, GREEN_WITH_BLUE):
            response = self.report.request_json(
                'place=product_accessories&rgb={}&hyperid=6001&rids=213&rearr'
                '-factors=split=25;market_disable_product_accessories=0'.format(rgb)
            )
            self.assert_has_generic_bundle(response)


if __name__ == '__main__':
    main()
