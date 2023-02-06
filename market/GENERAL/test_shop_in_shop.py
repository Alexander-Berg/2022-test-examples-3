#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import Offer, Shop, Region
from core.types.hypercategory import (
    EATS_CATEG_ID,
    CategoryStreamRecord,
    Stream,
)

from core.types.combinator import CombinatorGpsCoords, CombinatorExpressWarehouse


def make_mock_rearr(**kwds):
    suffix = 'parallel_smm=1.0;ext_snippet=1;no_snippet_arc=1;market_enable_sins_offers_wizard=1'
    rearr = make_rearr(**kwds)
    if rearr != '':
        rearr += ';'
    return rearr + suffix


def make_rearr(**kwds):
    kvlist = ['{}={}'.format(key, kwds[key]) for key in kwds]
    kvlist.sort(key=lambda x: x[0])
    return ';'.join(kvlist)


class _Gps1:
    _lat = 15.1234
    _lon = 13.4321

    location_combinator = CombinatorGpsCoords(_lat, _lon)
    location_str = 'lat:{lat};lon:{lon}'.format(lat=_lat, lon=_lon)


class _Constants:
    eda_fesh = [10001, 10002, 10003, 10004]
    eda_feed_id = [10001, 10002, 10003, 10004]
    eda_business_id = [100, 101, 102, 103]

    other_fesh = [1000]
    other_feed_id = [1000]
    other_business_id = [200]


class _Shops:
    retail_shops = []
    for i in range(len(_Constants.eda_fesh)):
        retail_shops.append(
            Shop(
                fesh=_Constants.eda_fesh[i],
                datafeed_id=_Constants.eda_feed_id[i],
                business_fesh=_Constants.eda_business_id[i],
                business_name="Business #{}".format(_Constants.eda_business_id[i]),
                warehouse_id=_Constants.eda_feed_id[i],
                cpa=Shop.CPA_REAL,
                is_eats=True,
            )
        )

    other_shops = [
        Shop(
            fesh=_Constants.other_fesh[0],
            datafeed_id=_Constants.other_feed_id[0],
            business_fesh=_Constants.other_business_id[0],
            business_name="Business #{}".format(_Constants.other_business_id[0]),
            warehouse_id=_Constants.other_feed_id[0],
            priority_region=213,
            cpa=Shop.CPA_REAL,
        )
    ]


class _Offers:
    retail_offers_per_shop = 2

    retail_offers = []
    retail_offers_count = [2, 3, 1, 5]  # разное кол-во офферов для проверки статистики
    for i in range(len(_Constants.eda_fesh)):
        for j in range(retail_offers_count[i]):
            retail_offers.append(
                Offer(
                    offerid='retail_offer_{}_shop_{}'.format(j, i),
                    waremd5='RetailOfferWaremd5_{}{}w'.format(i, j),
                    title='Retail offer {}.{}'.format(i, j),
                    fesh=_Constants.eda_fesh[i],
                    feedid=_Constants.eda_feed_id[i],
                    business_id=_Constants.eda_business_id[i],
                    is_eda_retail=True,
                    is_express=True,
                    warehouse_id=_Constants.eda_feed_id[i],
                    hid=EATS_CATEG_ID,
                )
            )

    other_offers_per_shop = 3
    other_offers = []
    for i in range(len(_Constants.other_fesh)):
        for j in range(other_offers_per_shop):
            other_offers.append(
                Offer(
                    offerid='other_offer_{}_shop_{}'.format(j, i),
                    waremd5='OtherOfferWaremd5__{}{}w'.format(i, j),
                    title='No retail offer {}.{}'.format(
                        i, j
                    ),  # надо чтобы офферы попадали в выдачу по запросу retail, но не были едой
                    fesh=_Constants.other_fesh[i],
                    feedid=_Constants.other_feed_id[i],
                    business_id=_Constants.other_business_id[i],
                    warehouse_id=_Constants.other_feed_id[i],
                )
            )


class T(TestCase):
    """
    Набор тестов для проекта Shop in shop.
    MARKETPROJECT-7523
    """

    @classmethod
    def prepare(cls):
        cls.settings.market_access_settings.enabled = True
        cls.settings.market_access_settings.download_catstreams = True
        cls.index.category_streams += [
            CategoryStreamRecord(EATS_CATEG_ID, Stream.FMCG.value),
        ]
        cls.reqwizard.on_default_request().respond()

        cls.index.shops += _Shops.retail_shops
        cls.index.shops += _Shops.other_shops

        cls.index.offers += _Offers.retail_offers
        cls.index.offers += _Offers.other_offers

        cls.index.regiontree += [
            Region(rid=213, name='Москва'),
        ]

    @classmethod
    def prepare_express(cls):
        cls.combinatorExpress.on_express_warehouses_request(
            region_id=213,
            gps_coords=_Gps1.location_combinator,
            rear_factors=make_mock_rearr(market_express_eda=1),
        ).respond_with_express_warehouses(
            [
                CombinatorExpressWarehouse(
                    warehouse_id=_Constants.eda_feed_id[i],
                    zone_id=1,
                    priority=len(_Constants.eda_feed_id) - i,
                    business_id=_Constants.eda_business_id[i],
                )
                for i in range(len(_Constants.eda_feed_id))
            ]
        )

    @classmethod
    def prepare_express_without_rearr_eda_business(cls):
        cls.combinatorExpress.on_express_warehouses_request(
            region_id=213,
            gps_coords=_Gps1.location_combinator,
            rear_factors=make_mock_rearr(),
        ).respond_with_express_warehouses(
            [
                CombinatorExpressWarehouse(
                    warehouse_id=_Constants.eda_feed_id[i],
                    zone_id=1,
                    priority=len(_Constants.eda_feed_id) - i,
                    business_id=_Constants.eda_business_id[i],
                )
                for i in range(len(_Constants.eda_feed_id))
            ]
        )

    @classmethod
    def prepare_express_without_rearr_eda_business2(cls):
        cls.combinatorExpress.on_express_warehouses_request(
            region_id=213,
            gps_coords=_Gps1.location_combinator,
            rear_factors=make_mock_rearr(a=1),
        ).respond_with_express_warehouses(
            [
                CombinatorExpressWarehouse(
                    warehouse_id=_Constants.other_feed_id[i],
                    zone_id=1,
                    priority=len(_Constants.other_feed_id) - i,
                    business_id=_Constants.other_business_id[i],
                )
                for i in range(len(_Constants.other_feed_id))
            ]
        )

    def test_shop_in_shop_top(self):
        """
        Параметр show-shop-in-shop-top=eda_retail добавляет в выдачу топ ритейл-магазинов отсортированных по удаленности от пользователя.
        Удаленность складов берется из гиперлокального контекста.
        В выдаче офферы из 5 магазинов, из них только 4 ритейл.
        Ожидаем в shopInShopTop 4 магазина, для каждого указанно количество его офферов в выдаче в параметре found.
        MARKETOUT-44187
        """
        response = self.report.request_json(
            'place=prime&text=retail'
            '&enable-foodtech-offers=1&'
            'rearr-factors=market_express_eda=1&'
            'rids=213&'
            'gps={}'.format(_Gps1.location_str)
        )

        self.assertFragmentIn(
            response,
            {
                "shopInShopTop": [
                    {
                        "businessId": _Shops.retail_shops[3].business_fesh,
                        "businessName": _Shops.retail_shops[3].business_name,
                        "found": _Offers.retail_offers_count[3],
                    },
                    {
                        "businessId": _Shops.retail_shops[2].business_fesh,
                        "businessName": _Shops.retail_shops[2].business_name,
                        "found": _Offers.retail_offers_count[2],
                    },
                    {
                        "businessId": _Shops.retail_shops[1].business_fesh,
                        "businessName": _Shops.retail_shops[1].business_name,
                        "found": _Offers.retail_offers_count[1],
                    },
                    {
                        "businessId": _Shops.retail_shops[0].business_fesh,
                        "businessName": _Shops.retail_shops[0].business_name,
                        "found": _Offers.retail_offers_count[0],
                    },
                ],
            },
            allow_different_len=False,
            preserve_order=True,
        )

    def test_shop_in_shop_top_count(self):
        # Оставляем только один shop-in-shop-top-count=1
        response = self.report.request_json(
            'place=prime&text=retail'
            '&enable-foodtech-offers=1&'
            'shop-in-shop-top-count=1&'
            'rids=213&'
            'gps={}'.format(_Gps1.location_str)
        )

        self.assertFragmentIn(
            response,
            {
                "shopInShopTop": [
                    {
                        "businessId": _Shops.retail_shops[3].business_fesh,
                        "businessName": _Shops.retail_shops[3].business_name,
                        "found": _Offers.retail_offers_count[3],
                    },
                ],
            },
            allow_different_len=False,
            preserve_order=True,
        )

        # При shop-in-shop-top-count=0 отдаем все что нашли
        response = self.report.request_json(
            'place=prime&text=retail'
            '&enable-foodtech-offers=1&'
            'shop-in-shop-top-count=0&'
            'rids=213&'
            'gps={}'.format(_Gps1.location_str)
        )

        self.assertFragmentIn(
            response,
            {
                "shopInShopTop": [
                    {
                        "businessId": _Shops.retail_shops[3].business_fesh,
                    },
                    {
                        "businessId": _Shops.retail_shops[2].business_fesh,
                    },
                    {
                        "businessId": _Shops.retail_shops[1].business_fesh,
                    },
                    {
                        "businessId": _Shops.retail_shops[0].business_fesh,
                    },
                ],
            },
            allow_different_len=False,
            preserve_order=True,
        )

    def test_eda_request_optimization(self):
        # Проверяем частный случай: бизнес еды и гиперлокальный склад заданный через express-warehouse-id;
        # для такого сценария для офферов добавляем к фильтру express=1&warehouseId=xxx

        request = (
            'place=prime&text=retail'
            '&enable-foodtech-offers=1&'
            'express-warehouse-id={whId}&'
            'fesh={fesh}&'
            'debug=1&'
            'rids=213&'
            'gps={gps}'
        )

        response = self.report.request_json(
            request.format(
                whId=_Constants.eda_feed_id[0], fesh=str(_Constants.eda_business_id[0]), gps=_Gps1.location_str
            )
        )

        reqwizardText = (
            "retail::1124746168 is_b2c:\"1\" << "
            "(is_express:\"1\" &/(-64 64) warehouse_id:\"10001\") <<"
            " (blue_doctype:\"b\" | blue_doctype:\"w\")"
        )

        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "brief": {
                        "reqwizardText": reqwizardText,
                    },
                },
                "search": {
                    "results": [
                        {
                            "entity": "offer",
                            "wareId": _Offers.retail_offers[0].waremd5,
                        },
                        {
                            "entity": "offer",
                            "wareId": _Offers.retail_offers[1].waremd5,
                        },
                    ],
                },
            },
            allow_different_len=False,
        )

        response = self.report.request_json(
            request.format(
                whId=_Constants.eda_feed_id[0], fesh=str(_Constants.other_business_id[0]), gps=_Gps1.location_str
            )
        )
        reqwizardText = (
            "retail::1124746168 is_b2c:\"1\" is_express:\"0\" <<"
            " (yx_ds_id:\"200\" | bsid:\"200\") << "
            "(blue_doctype:\"b\" | blue_doctype:\"w\")"
        )

        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "brief": {
                        "reqwizardText": reqwizardText,
                    },
                },
                "search": {
                    "results": [
                        {
                            "entity": "offer",
                            "wareId": _Offers.other_offers[0].waremd5,
                        },
                        {
                            "entity": "offer",
                            "wareId": _Offers.other_offers[1].waremd5,
                        },
                        {
                            "entity": "offer",
                            "wareId": _Offers.other_offers[2].waremd5,
                        },
                    ],
                },
            },
            allow_different_len=False,
        )

    def test_express_and_warehouse_request_optimization(self):
        # Если включен фильтр по экспресс-доставке и указаны склады добавляем к фильтру офферов
        # is_express=1 & warehouse_id=XXX
        warehouses = ','.join(str(feed_id) for feed_id in _Constants.eda_feed_id)

        request = (
            'place=prime&text=retail'
            '&enable-foodtech-offers=1&'
            'expresswarehouses={whs}&'
            'express-warehouse-id={whId}&'
            'filter-express-delivery=1&'
            'debug=1'
        )

        fesh = '{}'.format(_Constants.eda_business_id[0])

        response = self.report.request_json(request.format(whs=warehouses, whId=_Constants.eda_feed_id[0], fesh=fesh))

        reqwizardText = (
            "retail::1124746168 is_b2c:\"1\" warehouse_id:\"10001\" is_express:\"1\" <<"
            " (blue_doctype:\"b\" | blue_doctype:\"w\")"
        )

        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "brief": {
                        "reqwizardText": reqwizardText,
                    },
                },
                "search": {
                    "results": [
                        {
                            "entity": "offer",
                            "wareId": _Offers.retail_offers[0].waremd5,
                        },
                        {
                            "entity": "offer",
                            "wareId": _Offers.retail_offers[1].waremd5,
                        },
                    ],
                },
            },
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()
