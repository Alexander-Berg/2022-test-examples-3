#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    Book,
    BucketInfo,
    CostModificationRule,
    Currency,
    DeliveryBucket,
    DeliveryCalendar,
    DeliveryModifier,
    DeliveryOption,
    DynamicWarehouseInfo,
    DynamicWarehouseToWarehouseInfo,
    Dimensions,
    GpsCoord,
    Model,
    ModificationOperation,
    NewPickupBucket,
    NewPickupOption,
    Offer,
    OfferDeliveryInfo,
    OfferDimensions,
    Outlet,
    OutletDeliveryOption,
    PickupBucket,
    PickupOption,
    PickupRegionGroup,
    Picture,
    Promo,
    PromoType,
    RegionalDelivery,
    SortingCenterReference,
    VCluster,
)
from core.testcase import TestCase, main
from core.matcher import NotEmpty, Regex
from core.types.sku import MarketSku, BlueOffer
from core.types.shop import Shop
from core.types.autostrategy import AutostrategyType, Autostrategy, AutostrategyBundle


class _Shops(object):
    virtual_shop = Shop(
        fesh=1,
        priority_region=213,
        name='virtual_shop',
        currency=Currency.RUR,
        fulfillment_virtual=True,
        virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
        cpa=Shop.CPA_REAL,
        delivery_service_outlets=[2001, 2002, 2003, 2004],
    )
    blue_shop_1 = Shop(
        fesh=2,
        datafeed_id=3,
        priority_region=213,
        name='blue_shop_1',
        currency=Currency.RUR,
        cpa=Shop.CPA_REAL,
        blue=Shop.BLUE_REAL,
    )


class _Offers(object):
    sku1_offer1 = BlueOffer(price=5, offerid='Shop1_sku1', feedid=3, waremd5='Sku1Price5-IiLVm1Goleg')
    sku1_offer2 = BlueOffer(price=50, offerid='Shop2_sku1', feedid=3, waremd5='Sku1Price50-iLVm1Goleg')


picture = None


class T(TestCase):
    @classmethod
    def prepare(cls):
        # Numeration rules:
        # hid = { 1 ... 99 }
        # hyperid = { 300 .. 399 }

        cls.index.shipment_service_calendars += [
            DeliveryCalendar(
                fesh=1, calendar_id=1111, date_switch_hour=20, holidays=[0, 1, 2, 3, 4, 5, 6], is_sorting_center=True
            ),
            DeliveryCalendar(
                fesh=1, calendar_id=157, sc_references=[SortingCenterReference(sc_id=1111, duration=0, default=True)]
            ),
            DeliveryCalendar(
                fesh=1, calendar_id=158, sc_references=[SortingCenterReference(sc_id=1111, duration=0, default=True)]
            ),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=801,
                fesh=1,
                carriers=[157],
                regional_options=[
                    RegionalDelivery(
                        rid=213,
                        options=[
                            DeliveryOption(price=5, day_from=1, day_to=2, shop_delivery_price=10),
                            DeliveryOption(price=3, day_from=4, day_to=5, shop_delivery_price=10),
                        ],
                    ),
                    RegionalDelivery(
                        rid=2,
                        options=[
                            DeliveryOption(price=5, day_from=1, day_to=2, shop_delivery_price=10),
                            DeliveryOption(price=3, day_from=4, day_to=5, shop_delivery_price=10),
                        ],
                    ),
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=802,
                fesh=1,
                carriers=[158],
                regional_options=[
                    RegionalDelivery(
                        rid=213,
                        options=[
                            DeliveryOption(price=5, day_from=1, day_to=2, shop_delivery_price=10),
                            DeliveryOption(price=3, day_from=4, day_to=5, shop_delivery_price=10),
                        ],
                    ),
                    RegionalDelivery(
                        rid=2,
                        options=[
                            DeliveryOption(price=5, day_from=1, day_to=2, shop_delivery_price=10),
                            DeliveryOption(price=3, day_from=4, day_to=5, shop_delivery_price=10),
                        ],
                    ),
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
        ]

        cls.index.outlets += [
            Outlet(
                point_id=2001,
                delivery_service_id=157,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    shipper_id=103, day_from=1, day_to=1, order_before=2, work_in_holiday=True, price=100
                ),
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(37.12, 55.32),
            ),
            Outlet(
                point_id=2004,
                delivery_service_id=158,
                region=213,
                point_type=Outlet.FOR_POST_TERM,
                delivery_option=OutletDeliveryOption(shipper_id=123, day_from=1, day_to=1, price=400),
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(37.7, 55.7),
                dimensions=Dimensions(width=100, height=90, length=80, dim_sum=150),
            ),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5001,
                dc_bucket_id=4,
                fesh=1,
                carriers=[157],
                options=[PickupOption(outlet_id=2004, day_from=1, day_to=2, price=5)],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5002,
                dc_bucket_id=5,
                fesh=1,
                carriers=[158],
                options=[PickupOption(outlet_id=2001)],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
        ]

        cls.index.outlets += [
            Outlet(
                point_id=2002,
                delivery_service_id=157,
                region=213,
                point_type=Outlet.FOR_POST,
                post_code=111,
                delivery_option=OutletDeliveryOption(shipper_id=157),
                working_days=[i for i in range(10)],
            ),
            Outlet(
                point_id=2003,
                delivery_service_id=158,
                region=213,
                point_type=Outlet.FOR_POST,
                post_code=222,
                delivery_option=OutletDeliveryOption(shipper_id=158),
                working_days=[i for i in range(10)],
            ),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=901,
                fesh=1,
                carriers=[157],
                options=[PickupOption(outlet_id=2002, day_from=3, day_to=7)],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            PickupBucket(
                bucket_id=902,
                fesh=1,
                carriers=[158],
                options=[PickupOption(outlet_id=2003, day_from=3, day_to=4)],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
        ]

        cls.index.models += [
            Model(hyperid=303, hid=2, title='output_format_test_model_1', vendor_id=10032, ts=1265),
            Model(hyperid=304, hid=3, title='output_format_test_model_2', vendor_id=10342, ts=9821),
        ]

        cls.index.offers += [
            Offer(
                hyperid=303,
                hid=2,
                title='output_format_test_1',
                waremd5='ZRK9Q9nKpuAsmQsKgmUtyg',
                url='http://www.shop-001.ru/2219',
                offerid=12309,
                feedid=239,
                cluster_id=12071,
            ),
            Offer(
                hyperid=304,
                hid=3,
                title='output_format_test_2',
                waremd5='n9XlAsFkD2JzjIqQjT6w9w',
                url='http://www.shop-002.ru/2210',
                offerid=12568,
                feedid=876,
                cluster_id=7622,
            ),
        ]

    @classmethod
    def prepare_output_format(cls):
        cls.index.models += [
            Model(hyperid=300, hid=1),
            Model(hyperid=301, hid=1),
            Model(hyperid=302, hid=1),
        ]

        cls.index.offers += [
            Offer(hyperid=300),
            Offer(hyperid=301),
            Offer(hyperid=302),
        ]

    def test_output_format(self):
        """
        Тестируем формат выдачи плейса print_doc и поиск по hid
        """

        response = self.report.request_json('place=print_doc&hid=1')

        self.assertFragmentIn(response, {'documents_count': 6})

        self.assertEqual(3, response.count({'doc_type': 'offer'}))
        self.assertEqual(3, response.count({'doc_type': 'model'}))
        self.assertEqual(6, response.count({'hidd': '1'}))
        self.assertEqual(2, response.count({'hyper': '300'}))
        self.assertEqual(2, response.count({'hyper': '301'}))
        self.assertEqual(2, response.count({'hyper': '302'}))

        response = self.report.request_json('place=print_doc&hid=2,3')

        self.assertFragmentIn(
            response,
            {
                'documents_count': 4,
                'documents': [
                    {
                        'doc_type': 'model',
                        'title': 'output_format_test_model_1',
                        'properties': {'hidd': '2', 'hyper': '303', 'vendor_id': '10032', 'ts': '1265'},
                    },
                    {
                        'doc_type': 'model',
                        'title': 'output_format_test_model_2',
                        'properties': {'hidd': '3', 'hyper': '304', 'vendor_id': '10342', 'ts': '9821'},
                    },
                    {
                        'url': 'http://www.shop-001.ru/2219',
                        'doc_type': 'offer',
                        'title': 'output_format_test_1',
                        'properties': {
                            'hyper': '303',
                            'hidd': '2',
                            'cluster_id': '12071',
                            'offer_id': '12309',
                            'feed_id': '239',
                            'ware_md5': 'ZRK9Q9nKpuAsmQsKgmUtyg',
                            'rgb': 'green',
                        },
                    },
                    {
                        'url': 'http://www.shop-002.ru/2210',
                        'doc_type': 'offer',
                        'title': 'output_format_test_2',
                        'properties': {
                            'hyper': '304',
                            'hidd': '3',
                            'cluster_id': '7622',
                            'offer_id': '12568',
                            'feed_id': '876',
                            'ware_md5': 'n9XlAsFkD2JzjIqQjT6w9w',
                            'rgb': 'green',
                        },
                    },
                ],
            },
        )

    @classmethod
    def prepare_filtration_by_modelid(cls):
        cls.index.vclusters += [VCluster(vclusterid=1000000001)]

        cls.index.offers += [
            Offer(hyperid=305, title='filtration_by_modelid_test_1'),
            Offer(vclusterid=1000000001, title='filtration_by_modelid_test_2'),
        ]

    def test_filtration_by_modelid(self):
        """
        Тестируем поиск по modelid
        """
        response = self.report.request_json('place=print_doc&modelid=305')

        self.assertFragmentIn(response, {'documents_count': 2})
        self.assertFragmentIn(response, {'doc_type': 'offer', 'title': 'filtration_by_modelid_test_1'})
        self.assertFragmentIn(response, {'doc_type': 'model', 'properties': {'hyper': '305'}})

        response = self.report.request_json('place=print_doc&modelid=1000000001')

        self.assertFragmentIn(response, {'documents_count': 2})
        self.assertFragmentIn(response, {'doc_type': 'offer', 'title': 'filtration_by_modelid_test_2'})
        self.assertFragmentIn(
            response, {'doc_type': 'model', 'properties': {'vcluster_id': '1000000001', 'hyper': '1000000001'}}
        )

    @classmethod
    def prepare_filtration_by_vendor_id(cls):
        cls.index.offers += [
            Offer(title='offer_test_vendor_id_1', vendor_id=1532),
            Offer(title='offer_test_vendor_id_2', vendor_id=6521),
        ]

        cls.index.models += [Model(title='model_test_vendor_id', vendor_id=6521)]

    def test_filtration_by_vendor_id(self):
        """
        Тестируем поиск по vendor_id
        """
        response = self.report.request_json('place=print_doc&vendor_id=1532')

        self.assertFragmentIn(response, {'documents_count': 1})
        self.assertFragmentIn(
            response, {'doc_type': 'offer', 'title': 'offer_test_vendor_id_1', 'properties': {'vendor_id': '1532'}}
        )

        response = self.report.request_json('place=print_doc&vendor_id=6521')

        self.assertFragmentIn(response, {'documents_count': 2})
        self.assertFragmentIn(
            response,
            {
                'doc_type': 'model',
                'title': 'model_test_vendor_id',
            },
        )
        self.assertFragmentIn(response, {'doc_type': 'offer', 'title': 'offer_test_vendor_id_2'})

    @classmethod
    def prepare_filtration_by_ware_md5(cls):
        cls.index.offers += [
            Offer(title='filtration_by_ware_md5_test_1', waremd5='Adi64pGx5HJEQzdWNMz6Dg'),
            Offer(title='filtration_by_ware_md5_test_2', waremd5='2b0-iAnHLZST2Ekoq4xElr'),
        ]

    def test_filtration_by_ware_md5(self):
        """
        Тестируем поиск по offerid
        """
        response = self.report.request_json('place=print_doc&offerid=Adi64pGx5HJEQzdWNMz6Dg,2b0-iAnHLZST2Ekoq4xElr')

        self.assertFragmentIn(response, {'documents_count': 2})
        self.assertFragmentIn(
            response,
            {'doc_type': 'offer', 'title': 'filtration_by_ware_md5_test_1', 'properties': {'ware_md5': NotEmpty()}},
        )
        self.assertFragmentIn(
            response,
            {'doc_type': 'offer', 'title': 'filtration_by_ware_md5_test_2', 'properties': {'ware_md5': NotEmpty()}},
        )

    @classmethod
    def prepare_filtration_by_sku(cls):
        cls.index.offers += [Offer(title='filtration_by_sku', sku=90000000000)]

    def test_filtration_by_sku(self):
        """
        Тестируем поиск по offer-sku
        """
        response = self.report.request_json('place=print_doc&offer-sku=90000000000')

        self.assertFragmentIn(response, {'documents_count': 1})
        self.assertFragmentIn(
            response,
            {
                'doc_type': 'offer',
                'title': 'filtration_by_sku',
            },
        )

    @classmethod
    def prepare_filtration_by_url(cls):
        cls.index.offers += [Offer(title='filtration_by_url', offer_url_hash='test_offer_url_hash')]
        cls.index.offers += [Offer(title='filtration_by_url_2', url='http://shop-0101.ru/1234')]

    def test_filtration_by_url(self):
        """
        Тестируем поиск по offer-url-hash
        """
        response = self.report.request_json('place=print_doc&offer-url-hash=test_offer_url_hash')

        self.assertFragmentIn(response, {'documents_count': 1})
        self.assertFragmentIn(response, {'doc_type': 'offer', 'title': 'filtration_by_url'})

    @classmethod
    def prepare_filtration_by_shop_id(cls):
        cls.index.offers += [Offer(title='filtration_by_shop_id', fesh=1302)]

    def test_filtration_by_shop_id(self):
        """
        Тестируем поиск по shop_id
        """
        response = self.report.request_json('place=print_doc&fesh=1302')

        self.assertFragmentIn(response, {'documents_count': 1})
        self.assertFragmentIn(response, {'doc_type': 'offer', 'title': 'filtration_by_shop_id'})

    @classmethod
    def prepare_filtration_by_feed_shoffer_id(cls):
        cls.index.offers += [
            Offer(title='filtration_by_feed_id_1', feedid=1, offerid="1"),
        ]

    def test_filtration_by_feed_shoffer_id(self):
        """
        Тестируем поиск по идентификатору фида и офера
        """
        response = self.report.request_json('place=print_doc&feed_shoffer_id=1-1')

        self.assertFragmentIn(response, {'documents_count': 1})
        self.assertFragmentIn(response, {'doc_type': 'offer', 'title': 'filtration_by_feed_id_1'})

    @classmethod
    def prepare_filtration_by_isbn(cls):
        cls.index.books += [Book(author='sereja589', title='test_isbn', isbn='978-5-902719-15-1')]

    def test_filtration_by_isbn(self):
        """
        Тестируем поиск по isbn
        """
        response = self.report.request_json('place=print_doc&isbn=978-5-902719-15-1')

        self.assertFragmentIn(response, {'documents_count': 1})
        self.assertFragmentIn(response, {'doc_type': 'book', 'title': 'sereja589 \"test_isbn\"'})

    @classmethod
    def prepare_combined_filtration(cls):
        cls.index.offers += [
            Offer(title='combined_filtration_1', hid=4, hyperid=306, sku=90000000001),
            Offer(title='combined_filtration_2', hid=5, hyperid=307, sku=90000000002),
            Offer(title='combined_filtration_3', hid=4, hyperid=308, sku=90000000003),
        ]

        cls.index.offers += [
            Offer(title='combined_filtration_4', hyperid=309, fesh=9224),
            Offer(title='combined_filtration_4', hyperid=309, fesh=9913),
            Offer(title='combined_filtration_4', hyperid=310, fesh=9224),
        ]

    def test_combined_filtration(self):
        """
        Тестируем поиск по нескольким параметрам
        """
        response = self.report.request_json('place=print_doc&hid=4&offer-sku=90000000001')

        self.assertFragmentIn(response, {'documents_count': 1})
        self.assertFragmentIn(response, {'doc_type': 'offer', 'title': 'combined_filtration_1'})

        response = self.report.request_json('place=print_doc&modelid=309&fesh=9224')

        self.assertFragmentIn(response, {'documents_count': 1})

    def test_user_req_attrs(self):
        """
        Тестируем явное задание запрашиваемых аттрибутов
        """
        response = self.report.request_json('place=print_doc&req_attrs=_Url&offerid=ZRK9Q9nKpuAsmQsKgmUtyg')

        self.assertFragmentIn(response, {'documents_count': 1})

        self.assertFragmentIn(
            response,
            {
                'doc_type': 'offer',
                'url': 'http://www.shop-001.ru/2219',
                'properties': {'_Url': 'http://www.shop-001.ru/2219'},
            },
        )

        for prop in ['doc_type', 'hidd', 'ware_md5', 'hyper', 'cluster_id']:
            self.assertFragmentNotIn(
                response,
                {
                    'properties': {
                        prop: NotEmpty(),
                    }
                },
            )

        response = self.report.request_json('place=print_doc&req_attrs=ware_md5,hyper,hidd,_Url,extra_data&modelid=304')

        self.assertFragmentIn(response, {'documents_count': 2})

        self.assertFragmentIn(
            response,
            {
                'doc_type': 'offer',
                'url': 'http://www.shop-002.ru/2210',
                'properties': {
                    'ware_md5': NotEmpty(),
                    'hidd': '3',
                    '_Url': 'http://www.shop-002.ru/2210',
                    'hyper': '304',
                    'extra_data': {
                        'content': Regex(
                            'book_now_store_outlet_count'
                        ),  # произвольный параметр для проверки декодирования
                    },
                },
            },
        )

        self.assertFragmentIn(
            response,
            {
                'doc_type': 'model',
                'properties': {
                    'hyper': '304',
                    'hidd': '3',
                    '_Url': NotEmpty(),
                    'extra_data': {
                        'content': Regex('is_created_by_partner'),
                    },
                },
            },
        )

        for prop in ['doc_type', 'offer_id', 'dsrcid', 'categid']:
            self.assertFragmentNotIn(
                response,
                {
                    'doc_type': 'offer',
                    'properties': {
                        prop: NotEmpty(),
                    },
                },
            )

        for prop in ['doc_type', 'ware_md5', 'ts']:
            self.assertFragmentNotIn(
                response,
                {
                    'doc_type': 'model',
                    'properties': {
                        prop: NotEmpty(),
                    },
                },
            )

    @classmethod
    def prepare_blue_offers(cls):
        cls.index.shops += [_Shops.virtual_shop, _Shops.blue_shop_1]

        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=145, home_region=213),
            DynamicWarehouseToWarehouseInfo(warehouse_from=145, warehouse_to=145),
        ]

        cls.index.mskus += [
            MarketSku(
                title="blue offer sku1",
                hyperid=331,
                sku='1',
                waremd5='Sku1_SKU_WsIiLVm1goleg',
                blue_offers=[_Offers.sku1_offer1, _Offers.sku1_offer2],
                delivery_buckets=[801, 802],
                pickup_buckets=[5001, 5002],
                post_buckets=[901, 902],
                has_delivery_options=False,  # do not generate default buckets
            ),
        ]

    def test_blue_offers(self):
        """
        Что проверяем: Вывод синих оферов по маркетному СКУ. Будут выведены все синие оферы (без выбора лучшего) и документ самого СКУ
        """
        response = self.report.request_json('place=print_doc&market-sku=1')

        self.assertFragmentIn(response, {'documents_count': 3})
        self.assertFragmentIn(
            response,
            [
                {
                    'doc_type': 'offer',
                    'properties': {'ware_md5': 'Sku1Price5-IiLVm1Goleg', 'rgb': 'blue'},
                },
                {
                    'doc_type': 'offer',
                    'properties': {'ware_md5': 'Sku1Price50-iLVm1Goleg', 'rgb': 'blue'},
                },
                {
                    'doc_type': 'market_sku',  # На самом деле это СКУ, но пока что он имеет тип offer
                    'properties': {'ware_md5': 'Sku1_SKU_WsIiLVm1goleg'},
                },
            ],
        )

    # MARKETOUT-19373
    @classmethod
    def prepare_print_promo(cls):
        cls.index.shops += [
            Shop(fesh=1937301, priority_region=213),
        ]

        cls.index.offers += [
            Offer(
                title='offer 1034701_01',
                fesh=1937301,
                hyperid=1937301,
                waremd5='offer103470101_waremd5',
                promo=Promo(promo_type=PromoType.N_PLUS_ONE, key='promo1034701_01_key000'),
            )
        ]

    def test_warehouse_and_buckets(self):
        response = self.report.request_json(
            'place=print_doc&offerid=Sku1Price5-IiLVm1Goleg&rids=213&rearr-factors=market_nordstream_relevance=0'
        )
        self.assertFragmentIn(
            response,
            {
                "warehouseId": "145",
                "courier_offer_delivery_info": [
                    {"bucket_id": 801, "is_new": False},
                    {"bucket_id": 802, "is_new": False},
                ],
                "pickup_offer_delivery_info": [
                    {"bucket_id": 5001, "is_new": False},
                    {"bucket_id": 5002, "is_new": False},
                ],
                "post_offer_delivery_info": [{"bucket_id": 901, "is_new": False}, {"bucket_id": 902, "is_new": False}],
            },
        )

    def test_supplier_id(self):
        response = self.report.request_json('place=print_doc&offerid=Sku1Price5-IiLVm1Goleg&rids=213')
        self.assertFragmentIn(
            response,
            {
                "supplierId": str(_Shops.blue_shop_1.fesh),
                "dsrcid": str(_Shops.virtual_shop.fesh),
            },
        )

    @classmethod
    def prepare_complex_attrs(cls):
        global picture
        picture = Picture(width=100, height=100)
        cls.index.offers += [
            Offer(title='complex-offer', picture=picture, dimensions=OfferDimensions(length=20, width=30, height=10))
        ]

    def test_complex_attrs(self):
        response = self.report.request_json('place=print_doc&text=complex-offer')
        self.assertFragmentIn(
            response,
            {
                'picture': Picture.ToBase64String(picture, url_format=True),
                'width': '30',
                'height': '10',
                'length': '20',
            },
        )

    @classmethod
    def prepare_new_buckets(cls):
        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=803,
                regional_options=[
                    RegionalDelivery(rid=213, unknown=True),
                ],
                delivery_program=DeliveryBucket.DAAS,
            )
        ]

        cls.index.new_pickup_buckets += [
            NewPickupBucket(
                bucket_id=804,
                region_groups=[
                    PickupRegionGroup(options=[NewPickupOption(price=220, day_from=1, day_to=3)], outlets=[2004])
                ],
            )
        ]

        cls.index.delivery_modifiers += [
            DeliveryModifier(
                action=CostModificationRule(operation=ModificationOperation.ADD, parameter=50), modifier_id=1
            ),
        ]

        cls.index.offers += [
            Offer(
                title='new-bucket-offer',
                delivery_info=OfferDeliveryInfo(
                    courier_buckets=[BucketInfo(bucket_id=803, cost_modifiers=[1])],
                    pickup_buckets=[BucketInfo(bucket_id=804, cost_modifiers=[1])],
                ),
                has_delivery_options=False,
            )
        ]

    def test_new_buckets(self):
        response = self.report.request_json('place=print_doc&text=new-bucket-offer&rids=213')
        self.assertFragmentIn(
            response,
            {
                "courier_offer_delivery_info": [{'bucket_id': 803, 'cost_modifiers_ids': [1], 'is_new': True}],
                "pickup_offer_delivery_info": [{'bucket_id': 804, 'cost_modifiers_ids': [1], 'is_new': True}],
            },
        )

    @classmethod
    def prepare_autostrategy(cls):
        cls.index.offers += [
            Offer(
                title='positional',
                autostrategy_bundle=AutostrategyBundle(
                    id=1, production=Autostrategy(type=AutostrategyType.POSITIONAL, position=2, maxbid=42)
                ),
            ),
            Offer(
                title='drr',
                autostrategy_bundle=AutostrategyBundle(
                    id=2,
                    production=Autostrategy(type=AutostrategyType.DRR, drr=25),
                    experiment=Autostrategy(type=AutostrategyType.POSITIONAL, position=1, maxbid=0),
                ),
            ),
            Offer(
                price=10000,
                title='cpo',
                autostrategy_bundle=AutostrategyBundle(
                    id=100500,
                    production=Autostrategy(type=AutostrategyType.CPO, cpo=100),
                    experiment=Autostrategy(type=AutostrategyType.CPO, cpo=911),
                ),
            ),
            Offer(
                price=20000,
                title='cpa',
                autostrategy_bundle=AutostrategyBundle(
                    id=100501,
                    production=Autostrategy(type=AutostrategyType.CPA, fee=90),
                    experiment=Autostrategy(type=AutostrategyType.CPA, fee=199),
                ),
            ),
            Offer(
                title='all',
                autostrategy_bundle=AutostrategyBundle(
                    id=143,
                    production=Autostrategy(type=AutostrategyType.POSITIONAL, position=32, maxbid=33),
                    experiment=Autostrategy(type=AutostrategyType.POSITIONAL, position=1, maxbid=0),
                ),
            ),
            Offer(
                title='no_autostrategy',
                autostrategy_bundle=AutostrategyBundle(
                    id=3, production=Autostrategy(type=AutostrategyType.NO_STRATEGY)
                ),
            ),
            Offer(
                title='experiment_only',
                autostrategy_bundle=AutostrategyBundle(
                    id=4, experiment=Autostrategy(type=AutostrategyType.DRR, drr=143)
                ),
            ),
        ]

    def test_positional_autostrategy_is_enabled(self):
        """
        Проверяем, что если стратегия прилетает, если если она ВКЛЮЧЕНА флагом
        """
        response = self.report.request_json(
            'place=print_doc&text=positional&rearr-factors=market_disabled_autostrategy_types=0'
        )
        self.assertFragmentIn(
            response,
            {
                "autostrategy": {
                    "id": 1,
                    "production": {"type": "positional", "position": 2, "maxBid": 42},
                    "experiment": "unset",
                }
            },
        )

    def test_drr_autostrategy_is_disabled(self):
        """
        Проверяем, что если стратегия не прилетает, если она выключена флагом
        """
        response = self.report.request_json(
            'place=print_doc&text=drr&rearr-factors=market_disabled_autostrategy_types=3'
        )
        self.assertFragmentIn(
            response,
            {
                "autostrategy": {
                    "id": 2,
                    "production": "unset",
                    "experiment": {"type": "positional", "position": 1, "maxBid": 0},
                }
            },
        )

    def test_positional_autostrategy(self):
        """
        Проверяем, что если позиционная стратегия правильно прилетает, а так же, что не прилетает экспериментальная, если её нет
        Флаг стоит так как по умолчанию позиционная автостратегия выключена
        """
        response = self.report.request_json(
            'place=print_doc&text=positional&rearr-factors=market_disabled_autostrategy_types=0'
        )
        self.assertFragmentIn(
            response,
            {
                "autostrategy": {
                    "id": 1,
                    "production": {"type": "positional", "position": 2, "maxBid": 42},
                    "experiment": "unset",
                }
            },
        )

    def test_drr_autostrategy(self):
        """
        Проверяем, что если стратегия по drr правильно прилетает, а так же, что прилетает экспериментальная
        """
        response = self.report.request_json(
            'place=print_doc&text=drr&rearr-factors=market_disabled_autostrategy_types=0'
        )
        self.assertFragmentIn(
            response,
            {
                "autostrategy": {
                    "id": 2,
                    "production": {"type": "drr", "drr": 25},
                    "experiment": {"type": "positional", "position": 1, "maxBid": 0},
                }
            },
        )

    def test_unset_autostrategy(self):
        """
        Проверяем, что если у оффера явно задано неиспользование автостратегии, то возвращается правильный ответ
        """
        response = self.report.request_json('place=print_doc&text=no_autostrategy')
        self.assertFragmentIn(response, {"autostrategy": "unset"})

    def test_no_autostrategy(self):
        """
        Проверяем, что если у оффера нет автостратегии, то возвращается правильный ответ
        """
        response = self.report.request_json('place=print_doc&text=new-bucket-offer')
        self.assertFragmentIn(response, {"autostrategy": "unset"})

    def test_experiment_only(self):
        """
        Проверяем, что у оффера может быть только экспериментльная автостратегия
        """
        response = self.report.request_json('place=print_doc&text=experiment_only')
        self.assertFragmentIn(
            response, {"autostrategy": {"id": 4, "production": "unset", "experiment": {"type": "drr", "drr": 143}}}
        )

    def test_disabled_RTY_does_not_affect_slow_pipeline(self):
        """
        Проверяем, что выключение RTY не затрагивает доставку медленным пайплайном
        """
        response = self.report.request_json(
            'place=print_doc&text=experiment_only&rearr-factors=market_disable_RTY_for_autostrategies=1'
        )
        self.assertFragmentIn(
            response, {"autostrategy": {"id": 4, "production": "unset", "experiment": {"type": "drr", "drr": 143}}}
        )

    def test_no_drr_autostrategy(self):
        """
        Проверяем, что если стратегия по drr выключена флагом, то она не используется, а выключенная по умолчанию позиционная автостратегия включится
        """
        response = self.report.request_json(
            'place=print_doc&text=drr&rearr-factors=market_disabled_autostrategy_types=3'
        )
        self.assertFragmentIn(
            response,
            {
                "autostrategy": {
                    "id": 2,
                    "production": "unset",
                    "experiment": {"type": "positional", "position": 1, "maxBid": 0},
                }
            },
        )

    def test_no_positional_autostrategy(self):
        """
        Проверяем, что если позиционная стратегия выключена флагом, то она не используется, но ДРР стратегия работает
        """
        response = self.report.request_json(
            'place=print_doc&text=drr&rearr-factors=market_disabled_autostrategy_types=2'
        )
        self.assertFragmentIn(
            response, {"autostrategy": {"id": 2, "production": {"type": "drr", "drr": 25}, "experiment": "unset"}}
        )

    def test_no_positional_and_drr_autostrategy(self):
        """
        Проверяем, что если позиционная стратения и стратегия по drr выключены флагом, то они не используется
        """
        response = self.report.request_json(
            'place=print_doc&text=drr&rearr-factors=market_disabled_autostrategy_types=2,3'
        )
        self.assertFragmentIn(response, {"autostrategy": "unset"})

    def test_cpo_autostrategy(self):
        """
        Проверяем, что CPO стратегия правильно прилетает
        """
        response = self.report.request_json('place=print_doc&text=cpo')
        self.assertFragmentIn(
            response,
            {
                "autostrategy": {
                    "id": 100500,
                    "production": {"type": "cpo", "cpo": 100},
                    "experiment": {"type": "cpo", "cpo": 911},
                }
            },
        )

    def test_cpo_autostrategy_disabled(self):
        """
        Проверяем, что CPO стратегия правильно не прилетает, если выключена флагом
        """
        response = self.report.request_json(
            'place=print_doc&text=cpo&rearr-factors=market_disabled_autostrategy_types=4'
        )
        self.assertFragmentIn(response, {"autostrategy": "unset"})

    def test_fee_through_cpa_autostrategy(self):
        """
        Проверяем, что при включении соответствующего флага в качестве fee используется
        параметр CPA-автостратегии
        """
        response = self.report.request_json(
            'place=print_doc&text=cpa&rearr-factors=market_disabled_autostrategy_types=0;market_report_use_amore_fee=1'
        )
        self.assertFragmentIn(response, {"fee": 90})

    def test_no_fee_through_cpa_autostrategy(self):
        """
        Проверяем, что в качестве fee не используется параметр CPA-автостратегии,
        если соответствующий флаг выключен
        """
        response = self.report.request_json(
            'place=print_doc&text=cpa&rearr-factors=market_disabled_autostrategy_types=0;market_report_use_amore_fee=0'
        )
        self.assertFragmentIn(response, {"fee": 0})


if __name__ == '__main__':
    main()
