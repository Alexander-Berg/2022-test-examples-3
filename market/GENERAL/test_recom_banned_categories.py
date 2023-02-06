#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    Currency,
    HyperCategory,
    HyperCategoryType,
    Model,
    Offer,
    Shop,
    Tax,
    Vat,
    YamarecCategoryBanList,
    YamarecFeaturePartition,
    YamarecPlace,
    YamarecSettingPartition,
)
from core.testcase import TestCase, main
from core.types.sku import MarketSku, BlueOffer
from core.bigb import WeightedValue, BigBKeyword
from core.dj import DjModel


CATEGORY_BAN_LIST = {
    15756919: 'hard',  # Здоровье Антибиотики, противомикробные средства
    15756581: 'hard',  # Здоровье Борьба с вредными привычками
    15756921: 'hard',  # Здоровье Вакцины, сыворотки, фаги
    15756910: 'hard',  # Здоровье Женское здоровье
    15756502: 'hard',  # Здоровье Лечение аллергии
    15756897: 'hard',  # Здоровье Лечение астмы
    15756503: 'hard',  # Здоровье Лечение боли
    15756538: 'hard',  # Здоровье Лечение вен
    15756892: 'hard',  # Здоровье Лечение глаз и ушей
    15756513: 'hard',  # Здоровье Лечение грибка
    15756357: 'hard',  # Здоровье Лечение гриппа и простуды
    15756589: 'hard',  # Здоровье Лечение зубов и полости рта
    15756517: 'hard',  # Здоровье Лечение кожи
    15756525: 'hard',  # Здоровье Лечение нервной системы
    15756506: 'hard',  # Здоровье Лечение пищеварительной системы
    15756510: 'hard',  # Здоровье Лечение сахарного диабета
    15756887: 'hard',  # Здоровье Лечение сердца и сосудов
    15756903: 'hard',  # Здоровье Лечение травм, болей в мышцах и суставах
    15756907: 'hard',  # Здоровье Лечение щитовидной железы
    15756900: 'hard',  # Здоровье Мужское здоровье
    15756924: 'hard',  # Здоровье Народная медицина
    15756914: 'hard',  # Здоровье Противоопухолевые препараты
    15758037: 'hard',  # Здоровье Растворители для лекарственных препаратов
    15756915: 'hard',  # Здоровье Системные гормональные препараты
    818955: 'hard',  # Красота и здоровье Лекарственные растения
    16155381: 'hard',  # Алкоголь
    16155466: 'hard',  # Алкоголь Вино
    16155647: 'hard',  # Алкоголь Виски, бурбон
    16155455: 'hard',  # Алкоголь Водка
    16155448: 'hard',  # Алкоголь Коньяк, арманьяк, бренди
    16155587: 'hard',  # Алкоголь Крепкий алкоголь
    16155526: 'hard',  # Алкоголь Крепленое вино
    16155651: 'hard',  # Алкоголь Ликеры, настойки, аперитивы
    16155476: 'hard',  # Алкоголь Пиво и пивные напитки
    16155504: 'hard',  # Алкоголь Слабоалкогольные напитки
    16155560: 'hard',  # Алкоголь Шампанское и игристое вин
    6091783: 'soft',  # Товары для взрослых
    6290261: 'soft',  # Интимная косметика и парфюмерия
    6290262: 'soft',  # Эротическая одежда, белье, обувь
    6290266: 'soft',  # Эротические стимуляторы
    6290267: 'soft',  # Фаллоимитаторы
    6290268: 'soft',  # Вибраторы
    6290271: 'soft',  # BDSM атрибутика
    6290273: 'soft',  # Мебель, качели для эротических игр
    6290276: 'soft',  # Игры, книги, журналы эротического содержания
    6290278: 'soft',  # Интим-сувениры
    6290384: 'soft',  # Вакуумные помпы
    6296079: 'soft',  # Симуляторы для мужчин
    13744375: 'soft',  # Презервативы
    14695400: 'soft',  # Интимные смазки
    16339727: 'soft',  # Анальные стимуляторы
    16339999: 'soft',  # Секс-машины
    16344890: 'soft',  # Эротическое белье, одежда, обувь
    16344897: 'soft',  # Эротические бюстгальтеры
    16344944: 'soft',  # Эротические трусы для женщин
    16345021: 'soft',  # Комплекты эротического женского нижнего белья
    16345044: 'soft',  # Эротические трусы для мужчин
    16345063: 'soft',  # Эротические боди, комбинезоны
    16345094: 'soft',  # Эротические корсеты, грации
    16345110: 'soft',  # Эротические колготки и чулки
    16345156: 'soft',  # Эротическая одежда, костюмы для женщин
    16345172: 'soft',  # Эротическая одежда, костюмы для мужчин
    16345180: 'soft',  # Эротическая обувь для женщин
}
BANNED_CATEGORIES = list(CATEGORY_BAN_LIST.keys())
BANNED_MODELS = [(i + 9001, hid) for i, hid in enumerate([hid for hid in BANNED_CATEGORIES for i in range(4)])]


class BannedSkuOffers(object):
    feed_ids = [2] * len(BANNED_MODELS)
    # С флагом &rearr-factors=market_disable_blue_3p_discount_profitability_check=1 скидки размером больше 75%
    # признаются невалидными, поэтому устанавливаем скидку в размере 60% (60 = 100 - 40)
    discounts = [40] * len(BANNED_MODELS)
    shop_skus = ['Feed_{feedid}_sku{i}'.format(feedid=feedid, i=i + 1) for i, feedid in enumerate(feed_ids)]
    sku_offers = [
        BlueOffer(vat=Vat.VAT_10, offerid=shop_sku, feedid=feedid, discount=discount)
        for feedid, shop_sku, discount in zip(feed_ids, shop_skus, discounts)
    ]


REGULAR_CATEGORIES = list(range(201, 211))
REGULAR_MODELS = [(i + 1, hid) for i, hid in enumerate(REGULAR_CATEGORIES * 4)]


class SkuOffers(object):
    feed_ids = [2] * len(BANNED_MODELS)
    # С флагом &rearr-factors=market_disable_blue_3p_discount_profitability_check=1 скидки размером больше 75%
    # признаются невалидными, поэтому устанавливаем скидку в размере 60% (60 = 100 - 40)
    discounts = [40] * len(BANNED_MODELS)
    shop_skus = ['Feed_{feedid}_sku{i}'.format(feedid=feedid, i=i + 1) for i, feedid in enumerate(feed_ids)]
    sku_offers = [
        BlueOffer(vat=Vat.VAT_10, offerid=shop_sku, feedid=feedid, discount=discount)
        for feedid, shop_sku, discount in zip(feed_ids, shop_skus, discounts)
    ]


DEFAULT_BIGB_PROFILE = [
    BigBKeyword(
        id=BigBKeyword.GENDER,
        weighted_uint_values=[
            WeightedValue(value=BigBKeyword.GENDER_MALE, weight=621947),
            WeightedValue(value=BigBKeyword.GENDER_FEMALE, weight=375515),
        ],
    ),
]
OMM_OPTIONS = {
    'yandexuid': '1001',
    'keywords': DEFAULT_BIGB_PROFILE,
    'models': [],
    'history_models': [],
}


class T(TestCase):
    """
    MARKETOUT-20638 Набор тестов блокировки мед категорий в рекомендациях
    """

    @classmethod
    def prepare(cls):
        """
        Все модели, офферы, магазины и история пользователя
        """

        # categories
        cls.index.hypertree += [
            HyperCategory(
                hid=100,
                children=[HyperCategory(hid=hid, output_type=HyperCategoryType.GURU) for hid in BANNED_CATEGORIES]
                + [HyperCategory(hid=hid, output_type=HyperCategoryType.GURU) for hid in REGULAR_CATEGORIES],
            ),
        ]
        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.CATEGORY_BAN_LIST,
                kind=YamarecPlace.Type.CATEGORY_BAN_LIST,
                partitions=[YamarecCategoryBanList(ban_list=CATEGORY_BAN_LIST, splits=['*'])],
            )
        ]
        # shops
        cls.index.shops += [
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
            ),
            Shop(
                fesh=3,
                datafeed_id=3,
                priority_region=213,
                name='blue_shop_3',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                cpa=Shop.CPA_REAL,
                warehouse_id=145,
            ),
            # green shop
            Shop(fesh=2, priority_region=213),
        ]

        # models
        models = BANNED_MODELS + REGULAR_MODELS
        model_ids = [m[0] for m in models]
        cls.index.models += [Model(hyperid=hyperid, hid=hid) for hyperid, hid in models]

        # green offers
        cls.index.offers += [Offer(fesh=2, hyperid=hyperid) for hyperid, _ in models]

        # blue offers and skus
        shop_skus = BannedSkuOffers.shop_skus + SkuOffers.shop_skus
        sku_offers = BannedSkuOffers.sku_offers + SkuOffers.sku_offers
        cls.index.mskus += [
            MarketSku(
                title='Blue offer {sku}'.format(sku=shop_sku),
                hyperid=hyperid,
                sku='{i}'.format(i=hyperid),
                # waremd5='Sku{i}-wdDXWsIiLVm1goleg'.format(i=hyperid),
                blue_offers=[sku_offer],
            )
            for hyperid, shop_sku, sku_offer in zip(model_ids, shop_skus, sku_offers)
        ]

        # user history
        cls.recommender.on_request_viewed_models(user_id="yandexuid:1001").respond(
            {"models": list(map(str, model_ids))}
        )
        cls.recommender.on_request_models_of_interest(
            user_id="yandexuid:1001", item_count=40, with_timestamps=True, version=4
        ).respond(
            {'models': list(map(str, model_ids)), 'timestamps': list(map(str, list(range(len(model_ids), 0, -1))))}
        )
        # personal categories ordering
        cls.recommender.on_request_models_of_interest(user_id='yandexuid:1001').respond(
            {"models": list(map(str, model_ids))}
        )

        cls.settings.set_default_reqid = False
        cls.dj.on_request(yandexuid='1001').respond(
            models=[DjModel(id=m[0], title='model') for m in BANNED_MODELS[:15] + REGULAR_MODELS[:15]]
        )

    @classmethod
    def prepare_products_by_history(cls):
        # green history config
        names = keys = ['model_id', 'category_id']
        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.PRODUCTS_BY_HISTORY,
                kind=YamarecPlace.Type.FORMULA,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    YamarecFeaturePartition(
                        feature_names=names, feature_keys=keys, features=[], formula_id="market", splits=[{}]
                    ),
                ],
            ),
            YamarecPlace(
                name=YamarecPlace.Name.CATEGORY_GENERIC,
                kind=YamarecPlace.Type.FORMULA,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    YamarecFeaturePartition(
                        feature_names=['category_id', 'position'],
                        feature_keys=['category_id'],
                        features=[[hid, 0] for hid in BANNED_CATEGORIES + REGULAR_CATEGORIES],
                        splits=[{}],
                    ),
                ],
            ),
        ]

    def test_products_by_history(self):
        """
        История не должна показывать спец категории
        """
        # show regular categories for all markets
        for suffix in ['', '&rgb=green', '&cpa=real', '&rgb=blue', '&rgb=green_with_blue']:
            response = self.report.request_json(
                'place=products_by_history&yandexuid=1001&rids=213{}&numdoc=1000'.format(suffix)
            )
            self.assertFragmentIn(
                response,
                {'search': {'results': [{'entity': 'product', 'id': model[0]} for model in REGULAR_MODELS[:4]]}},
                preserve_order=False,
                allow_different_len=True,
            )
        # skip banned categories for rgb!=blue
        for rgb in ['', '&rgb=green', '&rgb=green_with_blue']:
            response = self.report.request_json(
                'place=products_by_history&yandexuid=1001&rids=213{}&numdoc=1000'.format(rgb)
            )
            self.assertFragmentNotIn(
                response,
                {'search': {'results': [{'entity': 'product', 'id': model[0]} for model in BANNED_MODELS[:4]]}},
                preserve_order=False,
            )

    def test_popular_products(self):
        # show regular categories and skip banned categories for all markets
        response = self.report.request_json(
            'place=popular_products&yandexuid=1001&rids=213&hid=100&rgb=blue&numdoc=1000'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [{'entity': 'product', 'categories': [{'id': hid}]} for hid in REGULAR_CATEGORIES[:5]]
                }
            },
            preserve_order=False,
            allow_different_len=True,
        )
        self.assertFragmentNotIn(
            response,
            {
                'search': {
                    'results': [{'entity': 'product', 'categories': [{'id': hid}]} for hid in BANNED_CATEGORIES[:5]]
                }
            },
            preserve_order=False,
        )

    def test_personal_categories(self):
        # show regular categories and skip banned categories for all markets
        response = self.report.request_json(
            'place=personal_categories&yandexuid=1001&rids=213&rgb=blue&numdoc=1000&prun-count=10000'
        )
        self.assertFragmentIn(
            response,
            {'search': {'results': [{'link': {'params': {'hid': str(hid)}}} for hid in REGULAR_CATEGORIES[:5]]}},
            preserve_order=False,
            allow_different_len=True,
        )
        self.assertFragmentNotIn(
            response,
            {'search': {'results': [{'link': {'params': {'hid': str(hid)}}} for hid in BANNED_CATEGORIES[:5]]}},
            preserve_order=False,
        )

    def test_categories_and_models_by_history(self):
        for rgb in ['', '&rgb=green', '&rgb=blue', '&rgb=green_with_blue']:
            response = self.report.request_json(
                'place=categories_and_models_by_history&yandexuid=1001&rids=213{}&numdoc=1000&debug=1'.format(rgb)
            )
            self.assertFragmentIn(
                response, {'categories': [{'entity': 'category', 'id': hid} for hid in REGULAR_CATEGORIES[:5]]}
            )
            self.assertFragmentNotIn(
                response, {'categories': [{'entity': 'category', 'id': hid} for hid in BANNED_CATEGORIES[:5]]}
            )

    @classmethod
    def prepare_item2item(cls):
        model_id, recom_ids = REGULAR_MODELS[0][0], [m[0] for m in BANNED_MODELS + REGULAR_MODELS[1:]]
        """also viewed"""
        cls.index.yamarec_places += [
            YamarecPlace(
                name=place_id,
                kind=YamarecPlace.Type.SETTING,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    YamarecSettingPartition(params={'version': '1'}, splits=[{}]),
                ],
            )
            for place_id in [YamarecPlace.Name.ALSO_VIEWED_PRODUCTS, YamarecPlace.Name.ALSO_VIEWED_PRODUCTS_BLUE_MARKET]
        ]
        cls.recommender.on_request_accessory_models(model_id=model_id, item_count=1000, version='1').respond(
            {"models": list(map(str, recom_ids))}
        )
        """product_accessories"""
        cls.index.yamarec_places += [
            YamarecPlace(
                name=place_id,
                kind=YamarecPlace.Type.SETTING,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    YamarecSettingPartition(
                        params={
                            'version': '2',
                            'use-external': '1',
                            'use-local': '0',
                        },
                        splits=[{}],
                    ),
                ],
            )
            for place_id in [
                YamarecPlace.Name.EXTERNAL_PRODUCT_ACCESSORY,
                YamarecPlace.Name.EXTERNAL_PRODUCT_ACCESSORY_BLUE_MARKET,
            ]
        ]
        cls.recommender.on_request_accessory_models(model_id=model_id, item_count=1000, version='2').respond(
            {"models": list(map(str, recom_ids))}
        )

    def test_item2item(self):
        model_id = REGULAR_MODELS[0][0]
        for place in ['also_viewed', 'product_accessories&rearr-factors=market_disable_product_accessories=0']:
            # show regular categories and skip banned categories for all markets
            response = self.report.request_json(
                'place={place}&yandexuid=1001&rids=213&hyperid={hyperid}&rgb=blue&numdoc=1000&prun-count=10000'.format(
                    place=place,
                    hyperid=model_id,
                )
            )
            self.assertFragmentIn(
                response,
                {'search': {'results': [{'entity': 'product', 'id': model[0]} for model in REGULAR_MODELS[1:3]]}},
                preserve_order=False,
                allow_different_len=True,
            )
            self.assertFragmentNotIn(
                response,
                {'search': {'results': [{'entity': 'product', 'id': model[0]} for model in BANNED_MODELS[:3]]}},
                preserve_order=False,
            )

    def test_personal_deals(self):
        for pers in ['&show-personal=0', '&show-personal=1']:
            # show regular categories and skip banned categories for all markets
            for rgb in ['', '&rgb=green', '&rgb=blue', '&rgb=green_with_blue']:
                response = self.report.request_json(
                    'place=deals&yandexuid=1001&rids=213{}{}&hid=100&numdoc=1000&prun-count=10000'.format(rgb, pers)
                )
                self.assertFragmentIn(
                    response,
                    {
                        'search': {
                            'results': [
                                {'entity': 'product', 'categories': [{'id': hid}]} for hid in REGULAR_CATEGORIES[:5]
                            ]
                        }
                    },
                    preserve_order=False,
                    allow_different_len=True,
                )
                self.assertFragmentNotIn(
                    response,
                    {
                        'search': {
                            'results': [
                                {'entity': 'product', 'categories': [{'id': hid}]} for hid in BANNED_CATEGORIES[:5]
                            ]
                        }
                    },
                    preserve_order=False,
                )

    @classmethod
    def prepare_omm(cls):
        cls.settings.set_default_reqid = False
        for exp in ['white_attractive_models']:
            cls.fast_dj.on_request(exp=exp, yandexuid='1001').respond(
                models=[DjModel(id=m[0], title='model') for m in BANNED_MODELS[:15] + REGULAR_MODELS[:15]]
            )

    def test_omm(self):
        for place in [
            'place=attractive_models',
        ]:
            # show regular categories and skip banned categories for all markets
            response = self.report.request_json('{place}&yandexuid=1001&rgb=blue&rids=213'.format(place=place))
            self.assertFragmentIn(
                response,
                {'search': {'results': [{'entity': 'product', 'id': model[0]} for model in REGULAR_MODELS[:3]]}},
                preserve_order=False,
                allow_different_len=True,
            )
            self.assertFragmentNotIn(
                response,
                {'search': {'results': [{'entity': 'product', 'id': model[0]} for model in BANNED_MODELS[:3]]}},
                preserve_order=False,
            )

    @classmethod
    def prepare_better_price(cls):
        cls.index.yamarec_places += [
            YamarecPlace(
                name="better-price",
                kind=YamarecPlace.Type.SETTING,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    YamarecSettingPartition(
                        params={
                            "show-history": "1",
                        },
                        splits=[{}],
                    ),
                ],
            ),
        ]
        cls.recommender.on_request_we_have_cheaper(user_id='yandexuid:1001', item_count=100).respond(
            {
                "we_have_cheaper": [
                    {"model_id": m[0], "price": 100.0, "success_requests_share": 0.1, "timestamp": "1495206745"}
                    for m in BANNED_MODELS[:15] + REGULAR_MODELS[:15]
                ]
            }
        )

    def test_better_price(self):
        # show regular categories and skip banned categories for all markets
        for rgb in ['', '&rgb=green', '&rgb=blue', '&rgb=green_with_blue']:
            response = self.report.request_json(
                'place=better_price&yandexuid=1001&rids=213{}&rearr-factors=products_by_history_with_bigb_and_sovetnik%3D0'.format(
                    rgb
                )
            )
            self.assertFragmentIn(
                response,
                {'search': {'results': [{'entity': 'product', 'id': model[0]} for model in REGULAR_MODELS[:3]]}},
                preserve_order=False,
                allow_different_len=True,
            )
            self.assertFragmentNotIn(
                response,
                {'search': {'results': [{'entity': 'product', 'id': model[0]} for model in BANNED_MODELS[:3]]}},
                preserve_order=False,
            )

    def test_recommendations_meta(self):
        for request in ['popular_products:3:3', 'products_by_history:3:3', 'better_price:3:3']:
            response = self.report.request_json(
                'place=recommendations_meta&recommendation-places={}&yandexuid=1001'.format(request)
            )
            ids = set(result['id'] for place in response.root['places'] for result in place['results'])
            banned_ids = set(m[0] for m in BANNED_MODELS)
            self.assertNotEqual(len(ids), 0)
            self.assertEqual(len(ids.intersection(banned_ids)), 0)

    @classmethod
    def prepare_personal_recommendations(cls):
        # default stuff
        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.ENDLESS_MAINPAGE,
                kind=YamarecPlace.Type.SETTING,
                partitions=[YamarecSettingPartition()],
            ),
            YamarecPlace(
                name=YamarecPlace.Name.CATEGORY_DISCOUNT,
                kind=YamarecPlace.Type.FORMULA,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    YamarecFeaturePartition(
                        feature_names=['category_id', 'position'],
                        feature_keys=['category_id'],
                        features=[[hid, 0] for hid in BANNED_CATEGORIES[:5] + REGULAR_CATEGORIES[:5]],
                        splits=[{}],
                    ),
                ],
            ),
            YamarecPlace(
                name=YamarecPlace.Name.ARTICLE_GENERIC,
                kind=YamarecPlace.Type.FORMULA,
                partitions=[
                    YamarecFeaturePartition(
                        feature_names=['article_id', 'category_id'],
                        feature_keys=['article_id', 'category_id'],
                        features=[[0, 1101]],
                    )
                ],
            ),
            YamarecPlace(
                name=YamarecPlace.Name.COLLECTION_GENERIC,
                kind=YamarecPlace.Type.FORMULA,
                partitions=[
                    YamarecFeaturePartition(
                        feature_names=['collection_id', 'category_id'],
                        feature_keys=['collection_id', 'category_id'],
                        features=[[0, 1101]],
                    )
                ],
            ),
        ]

    def test_personal_recommendations(self):
        response = self.report.request_json(
            'place=personal_recommendations&yandexuid=1001&rearr-factors=switch_popular_products_to_dj_no_nid_check=0'
        )
        ids = set(result['subEntity']['id'] for result in response.root['search']['results'])
        banned_ids = set(m[0] for m in BANNED_MODELS)
        self.assertNotEqual(len(ids), 0)
        self.assertEqual(len(ids.intersection(banned_ids)), 0)

        self.error_log.ignore(code=3787)


if __name__ == '__main__':
    main()
