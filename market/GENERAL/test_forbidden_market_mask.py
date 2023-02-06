#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    Currency,
    DeliveryBucket,
    DeliveryCalendar,
    GpsCoord,
    HyperCategory,
    HyperCategoryType,
    MarketSku,
    Model,
    NavCategory,
    Offer,
    Outlet,
    PickupBucket,
    PickupOption,
    Region,
    Shop,
    SortingCenterReference,
    Tax,
)
from core.testcase import TestCase, main
from core.types.sku import BlueOffer


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.hypertree += [
            HyperCategory(hid=1620400, output_type=HyperCategoryType.GURU),
        ]

        cls.index.navtree += [
            NavCategory(nid=1620410, hid=1620400, name="Test category"),
        ]

        cls.index.shops += [
            Shop(fesh=1620411, priority_region=213, regions=[225]),
            Shop(fesh=1620412, priority_region=213, regions=[225], cpa=Shop.CPA_REAL),
        ]

        cls.index.models += [
            Model(hyperid=1620402, hid=1620400, title='Test model', vendor_id=1620420),
        ]

    @classmethod
    def prepare_white_market_offers(cls):
        """
        Подготовка офферов для теста фильтрации на белом маркете
        """
        cls.index.offers += [
            Offer(title='White offer (none)', hid=1620400, fesh=1620411, hyperid=1620402),
            Offer(title='White offer (none - 0)', hid=1620400, fesh=1620411, hyperid=1620402, forbidden_market_mask=0),
            Offer(title='White offer (white)', hid=1620400, fesh=1620411, hyperid=1620402, forbidden_market_mask=1),
            Offer(title='White offer (blue)', hid=1620400, fesh=1620411, hyperid=1620402, forbidden_market_mask=2),
            Offer(
                title='White offer (blue/white)', hid=1620400, fesh=1620411, hyperid=1620402, forbidden_market_mask=3
            ),
            Offer(title='White offer (red)', hid=1620400, fesh=1620411, hyperid=1620402, forbidden_market_mask=4),
            Offer(title='White offer (red/white)', hid=1620400, fesh=1620411, hyperid=1620402, forbidden_market_mask=5),
            Offer(title='White offer (red/blue)', hid=1620400, fesh=1620411, hyperid=1620402, forbidden_market_mask=6),
            Offer(
                title='White offer (red/blue/white)',
                hid=1620400,
                fesh=1620411,
                hyperid=1620402,
                forbidden_market_mask=7,
            ),
            Offer(title='White CPA offer (none)', cpa=Offer.CPA_REAL, hid=1620400, fesh=1620412, hyperid=1620402),
            Offer(
                title='White CPA offer (none - 0)',
                cpa=Offer.CPA_REAL,
                hid=1620400,
                fesh=1620412,
                hyperid=1620402,
                forbidden_market_mask=0,
            ),
            Offer(
                title='White CPA offer (white)',
                cpa=Offer.CPA_REAL,
                hid=1620400,
                fesh=1620412,
                hyperid=1620402,
                forbidden_market_mask=1,
            ),
            Offer(
                title='White CPA offer (blue)',
                cpa=Offer.CPA_REAL,
                hid=1620400,
                fesh=1620412,
                hyperid=1620402,
                forbidden_market_mask=2,
            ),
            Offer(
                title='White CPA offer (blue/white)',
                cpa=Offer.CPA_REAL,
                hid=1620400,
                fesh=1620412,
                hyperid=1620402,
                forbidden_market_mask=3,
            ),
            Offer(
                title='White CPA offer (red)',
                cpa=Offer.CPA_REAL,
                hid=1620400,
                fesh=1620412,
                hyperid=1620402,
                forbidden_market_mask=4,
            ),
            Offer(
                title='White CPA offer (red/white)',
                cpa=Offer.CPA_REAL,
                hid=1620400,
                fesh=1620412,
                hyperid=1620402,
                forbidden_market_mask=5,
            ),
            Offer(
                title='White CPA offer (red/blue)',
                cpa=Offer.CPA_REAL,
                hid=1620400,
                fesh=1620412,
                hyperid=1620402,
                forbidden_market_mask=6,
            ),
            Offer(
                title='White CPA offer (red/blue/white)',
                cpa=Offer.CPA_REAL,
                hid=1620400,
                fesh=1620412,
                hyperid=1620402,
                forbidden_market_mask=7,
            ),
        ]

    def test_white_market_offers(self):
        """
        Проверка того, что фильтрация белых и синих офферов на белом маркете происходит корректно
        с учетом скорректированной логики https://st.yandex-team.ru/MARKETOUT-34721
        """
        response = self.report.request_json(
            'place=prime&pp=18&debug=1&text=offer&rgb=white&numdoc=100' '&rearr-factors=market_metadoc_search=no'
        )

        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'entity': 'offer', 'titles': {'raw': 'White offer (none)'}},
                    {'entity': 'offer', 'titles': {'raw': 'White offer (none - 0)'}},
                    {'entity': 'offer', 'titles': {'raw': 'White offer (blue)'}},
                    {'entity': 'offer', 'titles': {'raw': 'White offer (red)'}},
                    {'entity': 'offer', 'titles': {'raw': 'White offer (red/blue)'}},
                    {'entity': 'offer', 'titles': {'raw': 'White CPA offer (none)'}},
                    {'entity': 'offer', 'titles': {'raw': 'White CPA offer (none - 0)'}},
                    {'entity': 'offer', 'titles': {'raw': 'White CPA offer (white)'}},
                    {'entity': 'offer', 'titles': {'raw': 'White CPA offer (red)'}},
                    {'entity': 'offer', 'titles': {'raw': 'White CPA offer (red/white)'}},
                    {'entity': 'offer', 'titles': {'raw': 'Blue offer (none)'}},
                    {'entity': 'offer', 'titles': {'raw': 'Blue offer (none - 0)'}},
                    {'entity': 'offer', 'titles': {'raw': 'Blue offer (white)'}},
                    {'entity': 'offer', 'titles': {'raw': 'Blue offer (red)'}},
                    {'entity': 'offer', 'titles': {'raw': 'Blue offer (red/white)'}},
                ]
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_blue_market_offers(cls):
        """
        Подготовка офферов для теста фильтрации на синем маркете
        """
        cls.index.regiontree += [
            Region(
                rid=3,
                name='Центральный федеральный округ',
                region_type=Region.FEDERAL_DISTRICT,
                children=[
                    Region(
                        rid=1,
                        name='Московская область',
                        region_type=Region.FEDERATIVE_SUBJECT,
                        children=[
                            Region(rid=213, name='Москва'),
                        ],
                    ),
                ],
            ),
            Region(
                rid=17,
                name='Северо-западный федеральный округ',
                region_type=Region.FEDERAL_DISTRICT,
                children=[
                    Region(
                        rid=10174,
                        name='Санкт-Петербург и Ленинградская область',
                        region_type=Region.FEDERATIVE_SUBJECT,
                        children=[
                            Region(rid=2, name='Санкт-Петербург'),
                        ],
                    ),
                ],
            ),
        ]

        cls.index.shops += [
            Shop(
                fesh=1,
                datafeed_id=1,
                priority_region=213,
                regions=[213],
                name='Московская пепячечная "Доставляем"',
                delivery_service_outlets=[2001, 2002, 2003, 2011],
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
                warehouse_id=145,
            ),
            Shop(
                fesh=2,
                datafeed_id=2,
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                delivery_service_outlets=[12003, 12011],
            ),
        ]

        cls.index.shipment_service_calendars += [
            DeliveryCalendar(
                fesh=2, calendar_id=1111, date_switch_hour=20, holidays=[0, 1, 2, 3, 4, 5, 6], is_sorting_center=True
            ),
            DeliveryCalendar(
                fesh=2, calendar_id=1102, sc_references=[SortingCenterReference(sc_id=1111, duration=0, default=True)]
            ),
            DeliveryCalendar(fesh=1, calendar_id=101, date_switch_hour=20, holidays=[0, 1, 2, 3]),
            DeliveryCalendar(fesh=1, calendar_id=102, date_switch_hour=21, holidays=[4, 5, 6]),
        ]

        cls.index.outlets += [
            Outlet(
                point_id=2001,
                delivery_service_id=101,
                region=213,
                point_type=Outlet.FOR_POST,
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(37.7, 55.7),
            ),
            Outlet(
                point_id=2002,
                delivery_service_id=102,
                region=213,
                point_type=Outlet.FOR_POST,
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(37.7, 55.8),
            ),
            Outlet(
                point_id=2003,
                delivery_service_id=102,
                region=213,
                point_type=Outlet.FOR_POST,
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(37.7, 55.9),
            ),
            Outlet(
                point_id=2011,
                delivery_service_id=102,
                region=2,
                point_type=Outlet.FOR_POST,
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(67.7, 55.7),
            ),
            Outlet(
                point_id=12003,
                delivery_service_id=1102,
                region=213,
                point_type=Outlet.FOR_POST,
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(37.7, 55.9),
            ),
            Outlet(
                point_id=12011,
                delivery_service_id=1102,
                region=2,
                point_type=Outlet.FOR_POST,
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(67.7, 55.7),
            ),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=0,
                dc_bucket_id=5001,
                fesh=1,
                carriers=[101],
                options=[PickupOption(outlet_id=2001, day_from=1, day_to=2, price=5)],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            PickupBucket(
                bucket_id=1,
                dc_bucket_id=5002,
                fesh=1,
                carriers=[102],
                options=[
                    PickupOption(outlet_id=2002, day_from=3, day_to=3, price=20),
                    PickupOption(outlet_id=2003, day_from=5, day_to=5, price=30),
                    PickupOption(outlet_id=2011, day_from=14, day_to=15, price=50),
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            PickupBucket(
                bucket_id=11,
                dc_bucket_id=15002,
                fesh=2,
                carriers=[1102],
                options=[
                    PickupOption(outlet_id=12003, day_from=5, day_to=5, price=30),
                    PickupOption(outlet_id=12011, day_from=14, day_to=15, price=50),
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                title="Blue offer (none)",
                hid=10,
                hyperid=1,
                sku="12",
                post_buckets=[1, 11],
                blue_offers=[BlueOffer(price=1000, offerid="Shop2_sku12", waremd5="Sku12Price100-vm1Goleg", feedid=1)],
            ),
            MarketSku(
                title="Blue offer (none - 0)",
                hid=10,
                hyperid=1,
                sku="13",
                post_buckets=[1, 11],
                blue_offers=[
                    BlueOffer(
                        price=2000,
                        offerid="Shop2_sku13",
                        waremd5="Sku13Price200-vm1Goleg",
                        feedid=1,
                        forbidden_market_mask=0,
                    )
                ],
            ),
            MarketSku(
                title="Blue offer (white)",
                hid=10,
                hyperid=1,
                sku="14",
                post_buckets=[1, 11],
                blue_offers=[
                    BlueOffer(
                        price=3000,
                        offerid="Shop2_sku14",
                        waremd5="Sku14Price300-vm1Goleg",
                        feedid=1,
                        forbidden_market_mask=1,
                    )
                ],
            ),
            MarketSku(
                title="Blue offer (blue)",
                hid=10,
                hyperid=1,
                sku="15",
                post_buckets=[1, 11],
                blue_offers=[
                    BlueOffer(
                        price=4000,
                        offerid="Shop2_sku15",
                        waremd5="Sku15Price400-vm1Goleg",
                        feedid=1,
                        forbidden_market_mask=2,
                    )
                ],
            ),
            MarketSku(
                title="Blue offer (blue/white)",
                hid=10,
                hyperid=1,
                sku="16",
                post_buckets=[1, 11],
                blue_offers=[
                    BlueOffer(
                        price=5000,
                        offerid="Shop2_sku16",
                        waremd5="Sku16Price500-vm1Goleg",
                        feedid=1,
                        forbidden_market_mask=3,
                    )
                ],
            ),
            MarketSku(
                title="Blue offer (red)",
                hid=10,
                hyperid=1,
                sku="17",
                post_buckets=[1, 11],
                blue_offers=[
                    BlueOffer(
                        price=6000,
                        offerid="Shop2_sku17",
                        waremd5="Sku17Price600-vm1Goleg",
                        feedid=1,
                        forbidden_market_mask=4,
                    )
                ],
            ),
            MarketSku(
                title="Blue offer (red/white)",
                hid=10,
                hyperid=1,
                sku="18",
                post_buckets=[1, 11],
                blue_offers=[
                    BlueOffer(
                        price=7000,
                        offerid="Shop2_sku18",
                        waremd5="Sku18Price700-vm1Goleg",
                        feedid=1,
                        forbidden_market_mask=5,
                    )
                ],
            ),
            MarketSku(
                title="Blue offer (red/blue)",
                hid=10,
                hyperid=1,
                sku="19",
                post_buckets=[1, 11],
                blue_offers=[
                    BlueOffer(
                        price=8000,
                        offerid="Shop2_sku19",
                        waremd5="Sku19Price800-vm1Goleg",
                        feedid=1,
                        forbidden_market_mask=6,
                    )
                ],
            ),
            MarketSku(
                title="Blue offer (red/blue/white)",
                hid=10,
                hyperid=1,
                sku="20",
                post_buckets=[1, 11],
                blue_offers=[
                    BlueOffer(
                        price=9000,
                        offerid="Shop2_sku20",
                        waremd5="Sku20Price900-vm1Goleg",
                        feedid=1,
                        forbidden_market_mask=7,
                    )
                ],
            ),
        ]

    def test_blue_market_offers(self):
        """
        Проверка того, что фильтрация офферов на синем маркете происходит корректно
        """
        response = self.report.request_json(
            'place=prime&hid=10&pp=18&debug=0&rgb=blue&rids=0&allow-collapsing=0&numdoc=100'
        )

        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'entity': 'offer', 'titles': {'raw': 'Blue offer (none)'}},
                    {'entity': 'offer', 'titles': {'raw': 'Blue offer (none - 0)'}},
                    {'entity': 'offer', 'titles': {'raw': 'Blue offer (white)'}},
                    {'entity': 'offer', 'titles': {'raw': 'Blue offer (red)'}},
                    {'entity': 'offer', 'titles': {'raw': 'Blue offer (red/white)'}},
                ]
            },
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()
