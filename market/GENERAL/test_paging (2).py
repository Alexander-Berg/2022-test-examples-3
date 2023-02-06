#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BlueOffer,
    CategoryStatsRecord,
    Currency,
    HyperCategory,
    HyperCategoryType,
    MarketSku,
    Model,
    Offer,
    Opinion,
    RegionalModel,
    Shop,
    Tax,
    Vat,
    YamarecFeaturePartition,
    YamarecPlace,
    YamarecSettingPartition,
)
from core.matcher import Absent
from core.testcase import main
from simple_testcase import SimpleTestCase
from core.dj import DjModel


class T(SimpleTestCase):
    """
    Проверка корректности работы со стандартными параметрами для пэйджинга numdoc и page
    """

    @classmethod
    def prepare(cls):
        # blue shop
        cls.index.shops += [
            Shop(
                fesh=1886710,
                datafeed_id=188671001,
                priority_region=213,
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
            ),
            Shop(
                fesh=1,
                datafeed_id=1,
                priority_region=213,
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                blue=Shop.BLUE_REAL,
            ),
            Shop(fesh=2, priority_region=213),
        ]
        model_ids = list(range(1, 6))
        cls.recommender.on_request_viewed_models(user_id="yandexuid:1001").respond({"models": map(str, model_ids)})
        cls.recommender.on_request_models_of_interest(
            user_id="yandexuid:1001", item_count=40, with_timestamps=True, version=4
        ).respond({'models': map(str, model_ids), 'timestamps': map(str, list(range(len(model_ids), 0, -1)))})
        cls.bigb.on_request(yandexuid="1001", client='merch-machine').respond(counters=[])
        cls.index.models += [Model(hyperid=hyperid, hid=100 + hyperid) for hyperid in model_ids]
        cls.index.offers += [Offer(hyperid=hyperid) for hyperid in model_ids]
        cls.settings.set_default_reqid = False
        cls.dj.on_request(yandexuid='1001').respond(
            [DjModel(id='1'), DjModel(id='2'), DjModel(id='3'), DjModel(id='4')]
        )

    def test_products_by_history(self):
        """
        Проверяем paging в выдаче products_by_history
        """
        self.assertPagingSupportedForModels(
            base_query="place=products_by_history&yandexuid=1001&&rearr-factors=market_disable_dj_for_recent_findings%3D1",
            ids=list(range(1, 5)),
        )

    @classmethod
    def prepare_also_viewed(cls):
        """
        Конфигурация для получения нескольких моделей на выдаче also_viewed
        """
        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.ALSO_VIEWED_PRODUCTS,
                kind=YamarecPlace.Type.SETTING,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    YamarecSettingPartition(params={'version': '1'}, splits=[{"split": "also_viewed"}]),
                ],
            ),
        ]
        cls.recommender.on_request_accessory_models(model_id=1, item_count=1000, version='1').respond(
            {'models': ['2', '3', '4', '5']}
        )

    def test_also_viewed(self):
        """
        Проверяем paging в выдаче also_viewed
        """
        self.assertPagingSupportedForModels(
            base_query="place=also_viewed&rearr-factors=split=also_viewed&hyperid=1", ids=list(range(2, 6))
        )

    def test_also_viewed_track_last_page(self):
        """
        Проверяем, что не отдаются офферы со страниц после последней
        """
        response = self.report.request_json(
            'place=also_viewed&rearr-factors=split=also_viewed&hyperid=1&numdoc=10&page=2'
        )
        self.assertFragmentIn(response, {"results": Absent()})

    @classmethod
    def prepare_personal_category_models(cls):
        """
        Данные для непустой выдачи personalcategorymodels
        """

        cls.index.models += [
            Model(hyperid=11, hid=111, model_clicks=100),
            Model(hyperid=12, hid=112, model_clicks=100),
            Model(hyperid=13, hid=113, model_clicks=100),
            Model(hyperid=14, hid=114, model_clicks=100),
            Model(hyperid=15, hid=115, model_clicks=100),
        ]

        cls.index.regional_models += [
            RegionalModel(hyperid=11, offers=100),
            RegionalModel(hyperid=12, offers=100),
            RegionalModel(hyperid=13, offers=100),
            RegionalModel(hyperid=14, offers=100),
            RegionalModel(hyperid=15, offers=100),
        ]

        cls.index.offers += [Offer(hyperid=hyperid) for hyperid in range(11, 16)]

        cls.index.hypertree += [
            HyperCategory(hid=111, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=112, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=113, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=114, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=115, output_type=HyperCategoryType.GURU),
        ]

        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.CATEGORY_GENERIC,
                kind=YamarecPlace.Type.FORMULA,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    YamarecFeaturePartition(
                        feature_names=['category_id', 'position'],
                        feature_keys=['category_id'],
                        features=[],
                        splits=[{}],
                    ),
                ],
            )
        ]
        cls.recommender.on_request_models_of_interest(
            user_id="yandexuid:personalcategorymodels", item_count=1000
        ).respond({"models": map(str, list(range(11, 16)))})

    def test_personal_category_models(self):
        """
        Проверка пэйджинга для place=personalcategorymodels
        """
        self.assertPagingSupportedForModels(
            base_query='place=personalcategorymodels&yandexuid=personalcategorymodels&rearr-factors=split=personalcategorymodels',
            ids=list(range(11, 16)),
        )

    @classmethod
    def prepare_popular_products(cls):
        """
        Проверка пэйджинга для популярных товаров
        """
        cls.index.hypertree += [
            HyperCategory(
                hid=9200,
                children=[
                    HyperCategory(hid=9201, output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=9202, output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=9203, output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=9204, output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=9205, output_type=HyperCategoryType.GURU),
                ],
            ),
        ]

        model_ids = list(range(91, 96))
        cls.index.models += [
            Model(hyperid=91, hid=9201, model_clicks=500, opinion=Opinion(total_count=300, rating=5.0)),
            Model(hyperid=92, hid=9201, model_clicks=400, opinion=Opinion(total_count=300, rating=4.0)),
            Model(hyperid=93, hid=9201, model_clicks=300, opinion=Opinion(total_count=300, rating=3.0)),
            Model(hyperid=94, hid=9201, model_clicks=200, opinion=Opinion(total_count=300, rating=2.0)),
            Model(hyperid=95, hid=9201, model_clicks=100, opinion=Opinion(total_count=300, rating=1.0)),
            Model(hyperid=96, hid=9201, model_clicks=500, opinion=Opinion(total_count=300, rating=5.0)),
            Model(hyperid=97, hid=9201, model_clicks=400, opinion=Opinion(total_count=300, rating=4.0)),
            Model(hyperid=98, hid=9201, model_clicks=300, opinion=Opinion(total_count=300, rating=3.0)),
            Model(hyperid=99, hid=9201, model_clicks=200, opinion=Opinion(total_count=300, rating=2.0)),
            Model(hyperid=100, hid=9201, model_clicks=100, opinion=Opinion(total_count=300, rating=1.0)),
        ]

        cls.index.offers += [Offer(hyperid=hyperid, fesh=2) for hyperid in model_ids]
        feed_ids = [1] * 5
        prices = [5, 50, 45, 36, 15]
        sku_offers = [BlueOffer(price=price, vat=Vat.VAT_10, feedid=feedid) for feedid, price in zip(feed_ids, prices)]

        # market skus
        cls.index.mskus += [
            MarketSku(hyperid=hyperid, sku=hyperid * 1000 + 1, blue_offers=[sku_offer])
            for hyperid, sku_offer in zip(model_ids, sku_offers)
        ]

        cls.index.blue_category_region_stat += [
            CategoryStatsRecord(hid=9201, region=213, n_offers=3, n_discounts=3),
        ]
        cls.bigb.on_request(yandexuid="1009", client='merch-machine').respond(counters=[])

        cls.recommender.on_request_models_of_interest(user_id='yandexuid:1009').respond(
            {'models': map(str, list(range(96, 101))), 'timestamps': map(str, model_ids)}
        )
        cls.recommender.on_request_models_of_interest(
            user_id='yandexuid:1009', item_count=40, with_timestamps=True
        ).respond({'models': map(str, list(range(96, 101))), 'timestamps': map(str, model_ids)})

    def test_popular_products(self):
        """
        Проверка пэйджинга для popular_products
        """
        self.assertPagingSupportedForModels(
            base_query='place=popular_products&rids=213&hid=9200&yandexuid=1009&rgb=green&rearr-factors=switch_popular_products_to_dj_no_nid_check=0',
            ids=[91, 92, 93, 94, 95],
        )

    def test_popular_products_blue(self):
        """
        Проверка пэйджинга для popular_products&rgb=blue
        """
        self.assertPagingSupportedForModels(
            base_query='place=popular_products&rids=213&hid=9200&yandexuid=1009&rgb=blue&rearr-factors=switch_popular_products_to_dj_no_nid_check=0',
            ids=[91, 92, 93, 94, 95],
        )


if __name__ == '__main__':
    main()
