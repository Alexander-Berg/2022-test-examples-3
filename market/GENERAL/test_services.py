#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa
import math

from core.matcher import Absent, NoKey
from core.testcase import main, TestCase
from core.types import (
    DeliveryBucket,
    DeliveryOption,
    Offer,
    OrderService,
    OrderServiceFilter,
    OrderServiceSupplier,
    SkuListForService,
    Region,
    RegionalDelivery,
)
from core.types.shop import Shop
from core.types.sku import BlueOffer, MarketSku
from core.types.autogen import b64url_md5
from itertools import count

nummer = count(1)

RID_WITH_SERVICES = 213
RID_SUB_WITH_SERVICES = 2131
RID_WITHOUT_SERVICES = 312
RID_WITH_SERVICES_NOT_NOSCOW = 47

MODEL_ID_WITH_SERVICE = 3521
MODEL_ID_WRONG_HID = 3522
MODEL_ID_WRONG_VENDOR = 3523

MODEL_ID_WITH_SERVICE_BY_SKU = 100502
MODEL_ID_WITHOUT_SERVICE_BY_SKU = 1005001

HID_WITH_SERVICE = 94732
HID_WITHOUT_SERVICE = 94731

HID_FOR_SKU_FILTER = 100500

VENDOR_WITH_SERVICE = 194732
VENDOR_WITHOUT_SERVICE = 194731
VENDOR_FOR_SKU_FILTER = 194733


# офферы
def create_blue_offer(
    price,
    fesh,
    feed,
    vendor_id,
    hid,
    delivery_buckets=None,
    pickup_buckets=None,
    is_fulfillment=True,
    name=None,
    post_term_delivery=None,
):
    def build_ware_md5(id):
        return id.ljust(21, "_") + "w"

    if name is not None:
        offerid = name
        waremd5 = build_ware_md5(name)
    else:
        num = next(nummer)
        offerid = 'offerid_{}'.format(num)
        waremd5 = b64url_md5(num)

    return BlueOffer(
        waremd5=waremd5,
        price=price,
        fesh=fesh,
        feedid=feed,
        offerid=offerid,
        delivery_buckets=delivery_buckets,
        pickup_buckets=pickup_buckets,
        is_fulfillment=is_fulfillment,
        post_term_delivery=post_term_delivery,
        vendor_id=vendor_id,
        hid=hid,
    )


def create_white_offer(price, fesh, hyperid, feedid, sku, hid, title, vendor_id, delivery_buckets=None, is_dsbs=False):
    def build_ware_md5(id):
        return id.ljust(21, "_") + "w"

    if title is not None:
        offerid = title
        waremd5 = build_ware_md5(title)
    else:
        num = next(nummer)
        offerid = 'offerid_{}'.format(num)
        waremd5 = b64url_md5(num)

    return Offer(
        fesh=fesh,
        waremd5=waremd5,
        hyperid=hyperid,
        hid=hid,
        sku=sku,
        price=price,
        cpa=Offer.CPA_REAL if is_dsbs else Offer.CPA_NO,
        delivery_buckets=delivery_buckets,
        offerid=offerid,
        feedid=feedid,
        vendor_id=vendor_id,
    )


# Market SKU
def create_msku(offers_list=None, hid=None, num=None):
    num = num or next(nummer)
    msku_args = dict(
        sku=num,
        hyperid=num,
    )
    if offers_list is not None:
        assert isinstance(offers_list, list)
        msku_args["blue_offers"] = offers_list
    if hid is not None:
        msku_args.update(hid=hid)
    return MarketSku(**msku_args)


class OrdinaryShop:
    FEED_ID = 777
    SHOP_ID = 7770
    shop = Shop(
        fesh=SHOP_ID,
        datafeed_id=FEED_ID,
        priority_region=213,
        supplier_type=Shop.THIRD_PARTY,
        blue=Shop.BLUE_REAL,
        warehouse_id=145,
    )

    # оффер к которому не должно быть привязано никаких сервисов по обоим признакам
    blue_offer_no_services = create_blue_offer(
        price=1000,
        fesh=SHOP_ID,
        feed=FEED_ID,
        name="plain_no_services",
        vendor_id=VENDOR_WITHOUT_SERVICE,
        hid=HID_WITHOUT_SERVICE,
        delivery_buckets=[42],
    )

    # оффер к которому не должно быть привязано никаких сервисов по hid
    blue_offer_no_services_by_hid = create_blue_offer(
        price=1000,
        fesh=SHOP_ID,
        feed=FEED_ID,
        name="no_services_hid",
        vendor_id=VENDOR_WITH_SERVICE,
        hid=HID_WITHOUT_SERVICE,
        delivery_buckets=[42],
    )

    # оффер к которому не должно быть привязано никаких сервисов по vendor_id
    blue_offer_no_services_by_vendor = create_blue_offer(
        price=1000,
        fesh=SHOP_ID,
        feed=FEED_ID,
        name="no_services_vendor",
        vendor_id=VENDOR_WITHOUT_SERVICE,
        hid=HID_WITH_SERVICE,
        delivery_buckets=[42],
    )

    # оффер к которому привязан сервис
    # проверяем его в регионах с допустимыми и без допустимых сервисов
    blue_offer_with_service = create_blue_offer(
        price=1000,
        fesh=SHOP_ID,
        feed=FEED_ID,
        name="offer_with_services",
        vendor_id=VENDOR_WITH_SERVICE,
        hid=HID_WITH_SERVICE,
        delivery_buckets=[42],
    )

    # оффер к которому привязан сервис, доставляющийся в подрегион разрешённого региона
    # проверяем его в регионах с допустимыми и без допустимых сервисов
    blue_offer_with_service_sub = create_blue_offer(
        price=1000,
        fesh=SHOP_ID,
        feed=FEED_ID,
        name="offer_with_serv_sub",
        vendor_id=VENDOR_WITH_SERVICE,
        hid=HID_WITH_SERVICE,
        delivery_buckets=[44],
    )
    # оффер, для которого услуги доступны только по sku
    # проверяем на нем фильтр с услугами по sku
    blue_offer_with_services_sku = create_blue_offer(
        price=1100,
        fesh=SHOP_ID,
        feed=FEED_ID,
        name='offer_with_serv_sku',
        vendor_id=VENDOR_FOR_SKU_FILTER,
        hid=HID_FOR_SKU_FILTER,
        delivery_buckets=[42],
    )
    # оффер, для которого услуги доступны только по sku
    # проверяем на нем фильтр с услугами по sku
    blue_offer_without_services_sku = create_blue_offer(
        price=1100,
        fesh=SHOP_ID,
        feed=FEED_ID,
        name='offer_without_ser_sku',
        vendor_id=VENDOR_FOR_SKU_FILTER,
        hid=HID_FOR_SKU_FILTER,
        delivery_buckets=[42],
    )

    blue_offer_washmashine_first = create_blue_offer(
        price=1100,
        fesh=SHOP_ID,
        feed=FEED_ID,
        name='wash_mashine_first',
        vendor_id=next(nummer),
        hid=next(nummer),
        delivery_buckets=[42],
    )

    blue_offer_washmashine_second = create_blue_offer(
        price=1200,
        fesh=SHOP_ID,
        feed=FEED_ID,
        name='wash_mashine_second',
        vendor_id=next(nummer),
        hid=next(nummer),
        delivery_buckets=[42],
    )

    msku_washmashine_first = create_msku(
        [
            blue_offer_washmashine_first,
        ],
        hid=blue_offer_washmashine_first.hid,
    )

    msku_washmashine_second = create_msku(
        [
            blue_offer_washmashine_second,
        ],
        hid=blue_offer_washmashine_second.hid,
    )

    offers = [
        blue_offer_no_services,
        blue_offer_with_service,
        blue_offer_no_services_by_hid,
        blue_offer_no_services_by_vendor,
        blue_offer_with_service_sub,
    ]


class Services:
    ordinary_service = OrderService(
        service_id=123456,
        title="глажка",
        description="погладим вашего кота",
        price=100500.75,
        rid=213,
        ya_service_id='test_service',
        service_supplier=OrderServiceSupplier(id=432, name="КотоГлад"),
        service_filter=OrderServiceFilter(
            hid=HID_WITH_SERVICE,
            vendor_id=VENDOR_WITH_SERVICE,
        ),
    )
    service_in_another_region = OrderService(
        service_id=123,
        title="уборка гаража",
        description="Уборка гаража. Быстро, качественно",
        price=12340,
        rid=RID_WITH_SERVICES_NOT_NOSCOW,
        service_supplier=OrderServiceSupplier(id=111, name='КлинМастер'),
        service_filter=OrderServiceFilter(hid=HID_WITH_SERVICE, vendor_id=VENDOR_WITH_SERVICE),
    )

    service_by_sku = OrderService(
        service_id=124,
        title='Установка',
        description='Установка оборудования',
        price=1000,
        rid=RID_WITH_SERVICES,
        service_supplier=OrderServiceSupplier(id=112, name='Мастера на все руки'),
        service_filter=OrderServiceFilter(sku=str(MODEL_ID_WITH_SERVICE_BY_SKU)),
    )

    wash_machine_service = OrderService(
        service_id=125,
        title='Подключение',
        description='Подключение стиральной машины',
        price=1500,
        rid=RID_WITH_SERVICES,
        service_supplier=OrderServiceSupplier(id=112, name='Мастера на все руки'),
        service_filter=OrderServiceFilter(),
    )

    services = [ordinary_service, service_in_another_region, service_by_sku, wash_machine_service]
    skus_by_services = SkuListForService(
        wash_machine_service.service_id,
        [OrdinaryShop.msku_washmashine_first.sku, OrdinaryShop.msku_washmashine_second.sku],
    )


class DsbsShop:
    FEED_ID = 11111
    SHOP_ID = 111110

    shop = Shop(
        fesh=SHOP_ID,
        datafeed_id=FEED_ID,
        priority_region=213,
        name='DSBS shop',
        cpa=Shop.CPA_REAL,
        warehouse_id=35984,
    )

    # dsbs оффер
    blue_offer_with_service_by_msku_dsbs = create_white_offer(
        price=1000,
        sku=next(nummer),
        hyperid=MODEL_ID_WITH_SERVICE,
        hid=HID_WITH_SERVICE,
        fesh=SHOP_ID,
        feedid=FEED_ID,
        title="offer_dsbs",
        is_dsbs=True,
        vendor_id=VENDOR_WITH_SERVICE,
        delivery_buckets=[43],
    )

    offers = [blue_offer_with_service_by_msku_dsbs]


class DsbsShopWithServices:
    FEED_ID = 22222
    SHOP_ID = 222220

    shop = Shop(
        fesh=SHOP_ID,
        datafeed_id=FEED_ID,
        priority_region=213,
        name='DSBS shop with services',
        cpa=Shop.CPA_REAL,
        warehouse_id=35984,
    )

    # dsbs оффер
    blue_offer_with_service_dsbs_from_list = create_white_offer(
        price=1000,
        sku=MODEL_ID_WITH_SERVICE_BY_SKU,
        hyperid=MODEL_ID_WITH_SERVICE_BY_SKU,
        hid=HID_FOR_SKU_FILTER,
        fesh=SHOP_ID,
        feedid=FEED_ID,
        title="offer_dsbs_with_serv",
        is_dsbs=True,
        vendor_id=VENDOR_WITHOUT_SERVICE,
        delivery_buckets=[43],
    )

    offers = [blue_offer_with_service_dsbs_from_list]


class CpcShop:
    FEED_ID = 33333
    SHOP_ID = 333330

    shop = Shop(fesh=SHOP_ID, datafeed_id=FEED_ID, priority_region=213, cpa=Shop.CPA_NO, warehouse_id=35985)

    cpc_offer = create_white_offer(
        price=200,
        sku=MODEL_ID_WITH_SERVICE_BY_SKU,
        hyperid=MODEL_ID_WITH_SERVICE_BY_SKU,
        hid=HID_FOR_SKU_FILTER,
        fesh=SHOP_ID,
        feedid=FEED_ID,
        title="cpc_offer",
        is_dsbs=False,
        vendor_id=VENDOR_WITH_SERVICE,
        delivery_buckets=[46],
    )

    offers = [cpc_offer]


class T(TestCase):
    @classmethod
    def prepare(cls):
        region_tree = [
            Region(
                rid=1,
                name='РФ',
                children=[
                    Region(
                        rid=RID_WITH_SERVICES,
                        name='Москва',
                        children=[Region(rid=RID_SUB_WITH_SERVICES, name='Москва')],
                    ),
                    Region(rid=RID_WITH_SERVICES_NOT_NOSCOW, name='Нижний Новгород'),
                    Region(rid=RID_WITHOUT_SERVICES, name='Россия'),
                ],
            ),
        ]
        cls.index.regiontree += region_tree

        cls.index.shops += [OrdinaryShop.shop, DsbsShop.shop, DsbsShopWithServices.shop, CpcShop.shop]

        cls.index.mskus += [
            create_msku(
                [
                    offer,
                ],
                hid=offer.hid,
            )
            for offer in OrdinaryShop.offers
        ]

        cls.index.mskus += [
            create_msku(
                [OrdinaryShop.blue_offer_with_services_sku], hid=HID_FOR_SKU_FILTER, num=MODEL_ID_WITH_SERVICE_BY_SKU
            ),
            create_msku(
                [OrdinaryShop.blue_offer_without_services_sku],
                hid=HID_FOR_SKU_FILTER,
                num=MODEL_ID_WITHOUT_SERVICE_BY_SKU,
            ),
            OrdinaryShop.msku_washmashine_first,
            OrdinaryShop.msku_washmashine_second,
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=42,
                fesh=OrdinaryShop.SHOP_ID,
                carriers=[99],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
                regional_options=[
                    RegionalDelivery(rid=RID_WITH_SERVICES, options=[DeliveryOption(price=100, day_from=1, day_to=1)]),
                    RegionalDelivery(
                        rid=RID_WITH_SERVICES_NOT_NOSCOW, options=[DeliveryOption(price=100, day_from=1, day_to=1)]
                    ),
                    RegionalDelivery(
                        rid=RID_WITHOUT_SERVICES, options=[DeliveryOption(price=100, day_from=1, day_to=1)]
                    ),
                ],
            ),
            DeliveryBucket(
                bucket_id=43,
                fesh=DsbsShop.SHOP_ID,
                carriers=[99],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
                regional_options=[
                    RegionalDelivery(rid=RID_WITH_SERVICES, options=[DeliveryOption(price=100, day_from=1, day_to=1)]),
                    RegionalDelivery(
                        rid=RID_WITH_SERVICES_NOT_NOSCOW, options=[DeliveryOption(price=100, day_from=1, day_to=1)]
                    ),
                    RegionalDelivery(
                        rid=RID_WITHOUT_SERVICES, options=[DeliveryOption(price=100, day_from=1, day_to=1)]
                    ),
                ],
            ),
            DeliveryBucket(
                bucket_id=44,
                fesh=OrdinaryShop.SHOP_ID,
                carriers=[99],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
                regional_options=[
                    RegionalDelivery(
                        rid=RID_SUB_WITH_SERVICES, options=[DeliveryOption(price=100, day_from=1, day_to=1)]
                    ),
                    RegionalDelivery(
                        rid=RID_WITH_SERVICES_NOT_NOSCOW, options=[DeliveryOption(price=100, day_from=1, day_to=1)]
                    ),
                    RegionalDelivery(
                        rid=RID_WITHOUT_SERVICES, options=[DeliveryOption(price=100, day_from=1, day_to=1)]
                    ),
                ],
            ),
            DeliveryBucket(
                bucket_id=45,
                fesh=DsbsShopWithServices.SHOP_ID,
                carriers=[99],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
                regional_options=[
                    RegionalDelivery(rid=RID_WITH_SERVICES, options=[DeliveryOption(price=100, day_from=1, day_to=1)]),
                    RegionalDelivery(
                        rid=RID_WITH_SERVICES_NOT_NOSCOW, options=[DeliveryOption(price=100, day_from=1, day_to=1)]
                    ),
                    RegionalDelivery(
                        rid=RID_WITHOUT_SERVICES, options=[DeliveryOption(price=100, day_from=1, day_to=1)]
                    ),
                ],
            ),
            DeliveryBucket(
                bucket_id=46,
                fesh=CpcShop.SHOP_ID,
                carriers=[99],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
                regional_options=[
                    RegionalDelivery(rid=RID_WITH_SERVICES, options=[DeliveryOption(price=100, day_from=1, day_to=1)]),
                    RegionalDelivery(
                        rid=RID_WITH_SERVICES_NOT_NOSCOW, options=[DeliveryOption(price=100, day_from=1, day_to=1)]
                    ),
                    RegionalDelivery(
                        rid=RID_WITHOUT_SERVICES, options=[DeliveryOption(price=100, day_from=1, day_to=1)]
                    ),
                ],
            ),
        ]

        cls.index.offers += DsbsShop.offers
        cls.index.offers += DsbsShopWithServices.offers
        cls.index.offers += CpcShop.offers

        cls.index.order_services += Services.services
        cls.index.dsbs_shops_with_services += [DsbsShopWithServices.SHOP_ID]
        cls.index.sku_by_service += [Services.skus_by_services]

    def build_request(self, offer, rids, additional_params):
        request = "&regset=2&rids={rids}&place=offerinfo&offerid={waremd5}".format(waremd5=offer.waremd5, rids=rids)
        request += "&use-default-offers=1&pp=28&rearr-factors=market_hide_regional_delimiter=1"
        request += "" if additional_params is None else "&" + "&".join(additional_params)
        return request

    def build_offer_entity(self, offer):
        return {
            "entity": "offer",
            "wareId": offer.waremd5,
        }

    def add_service_entity(self, offer_output, services):
        def render_price(price):
            return {
                # имитируем округление, которое происходит во время рендеринга
                "value": str(int(math.ceil(price))),
                "currency": "RUR",
            }

        def render_supplier(supplier):
            return {
                "id": supplier.id,
                "name": supplier.name,
            }

        if isinstance(services, list):
            offer_output["services"] = [
                {
                    "service_id": service.service_id,
                    "title": service.title,
                    "description": service.description,
                    "ya_service_id": NoKey("ya_service_id") if service.ya_service_id is None else service.ya_service_id,
                    "price": render_price(service.price),
                    "service_supplier": render_supplier(service.supplier),
                }
                for service in services
            ]
        else:
            offer_output["services"] = services

    def check_services(self, offer, rids, services, additional_params):
        request = self.build_request(offer, rids, additional_params)
        response = self.report.request_json(request)
        offer_view = self.build_offer_entity(offer)
        self.add_service_entity(offer_view, services)
        self.assertFragmentIn(response, offer_view)

    def test_no_services(self):
        '''
        Проверяем офферы для которых услуг не предусмотрено по следующим причинам
        * настройка услуги категории
        * настройка услуги вендора
        * dsbs [хардкод в коде репорта]
        Услуги для данных офферов недоступны независимо от региона
        '''
        for rid in [RID_WITH_SERVICES, RID_WITHOUT_SERVICES]:
            for offer in [
                DsbsShop.blue_offer_with_service_by_msku_dsbs,
                OrdinaryShop.blue_offer_no_services,
                OrdinaryShop.blue_offer_no_services_by_hid,
                OrdinaryShop.blue_offer_no_services_by_vendor,
            ]:
                for flag in [0, 1, None]:
                    additional = None
                    if flag is not None:
                        additional = ["rearr-factors=market_enable_services={}".format(flag)]

                    self.check_services(
                        offer=offer,
                        rids=rid,
                        services=Absent(),
                        additional_params=additional,
                    )

    def test_services_for_dsbs(self):
        '''
        Проверяем, что услуги для ДСБС доступны только явно заданным в динамике магазинам
        И здесь же проверяем, что для ДСБС работает поиск услуг по SKU
        '''
        _ = {}
        for offer in [
            DsbsShop.blue_offer_with_service_by_msku_dsbs,
            DsbsShopWithServices.blue_offer_with_service_dsbs_from_list,
        ]:
            self.check_services(
                offer=offer,
                rids=213,
                services=[Services.service_by_sku] if offer.offerid == "offer_dsbs_with_serv" else Absent(),
                additional_params=['', '&rearr-factors=market_enable_services=1'],
            )

    def test_cpc_no_services(self):
        '''
        Проверяем, что для CPC офферов недоступны услуги, даже если данные оффера подходят под фильтр услуг
        '''
        self.check_services(
            offer=CpcShop.offers[0],
            rids=213,
            services=Absent(),
            additional_params=['', '&rearr-factors=market_enable_services=1'],
        )

    def test_services_by_rids(self):
        '''
        Позитивные кейсы. К офферу должен привязываться сервис. Однако его доступность
        будет зависить от региона.
        '''

        # в регионе где этот сервис доступен
        # сервис должен быть показан
        # причем это должно работать не только для Москвы, но и для других регионов, для которых доступны услуги
        for rid in [RID_WITH_SERVICES, RID_WITH_SERVICES_NOT_NOSCOW]:
            for offer in [
                OrdinaryShop.blue_offer_with_service,
            ]:
                for flag in [1]:
                    additional = None
                    if flag is not None:
                        additional = ["rearr-factors=market_enable_services={}".format(flag)]

                    self.check_services(
                        offer=offer,
                        rids=rid,
                        services=[
                            Services.ordinary_service
                            if rid == RID_WITH_SERVICES
                            else Services.service_in_another_region
                        ],
                        additional_params=additional,
                    )

        # в регионе где этот сервис недоступен
        # сервис не должен быть показан
        for rid in [
            RID_WITHOUT_SERVICES,
        ]:
            for offer in [
                OrdinaryShop.blue_offer_with_service,
            ]:
                for flag in [0, 1, None]:
                    additional = None
                    if flag is not None:
                        additional = ["rearr-factors=market_enable_services={}".format(flag)]

                    self.check_services(
                        offer=offer,
                        rids=rid,
                        services=Absent(),
                        additional_params=additional,
                    )

        # в регионе где этот сервис доступен
        # сервис должен быть показан
        for rid in [
            RID_SUB_WITH_SERVICES,
        ]:
            for offer in [
                OrdinaryShop.blue_offer_with_service_sub,
            ]:
                for flag in [1]:
                    additional = None
                    if flag is not None:
                        additional = ["rearr-factors=market_enable_services={}".format(flag)]
                    self.check_services(
                        offer=offer, rids=rid, services=[Services.ordinary_service], additional_params=additional
                    )

    def test_services_filter(self):
        '''
        Проверяем работу фильтра по наличию доп.услуг
        Фильтр должен одинаково работать и в Москве, и в других регионах
        '''
        # подготавливаем ответы для проверки запросов
        regions = [RID_WITH_SERVICES, RID_WITH_SERVICES_NOT_NOSCOW]
        full_response = {
            "search": {
                "total": 9,
                "totalOffers": 9,
                "results": [
                    {"entity": "offer", "wareId": "plain_no_services____w"},
                    {"entity": "offer", "wareId": "offer_with_services__w"},
                    {"entity": "offer", "wareId": "no_services_hid______w"},
                    {"entity": "offer", "wareId": "offer_with_serv_sub__w"},
                    {"entity": "offer", "wareId": "no_services_vendor___w"},
                    {"entity": "offer", "wareId": "offer_with_serv_sku__w"},
                    {"entity": "offer", "wareId": "offer_without_ser_skuw"},
                    {"entity": "offer", "wareId": OrdinaryShop.blue_offer_washmashine_first.waremd5},
                    {"entity": "offer", "wareId": OrdinaryShop.blue_offer_washmashine_second.waremd5},
                ],
            }
        }
        # в этих ответах мы заодно проверим, что приходит фильтр "with-services"
        filtered_responses = {}
        filtered_responses[RID_WITH_SERVICES] = {
            "search": {
                "total": 5,
                "totalOffers": 5,
                "totalOffersBeforeFilters": 9,
                "totalPassedAllGlFilters": 5,
                "results": [
                    {"entity": "offer", "wareId": "offer_with_services__w"},
                    {"entity": "offer", "wareId": "offer_with_serv_sub__w"},
                    {"entity": "offer", "wareId": "offer_with_serv_sku__w"},
                    {"entity": "offer", "wareId": OrdinaryShop.blue_offer_washmashine_first.waremd5},
                    {"entity": "offer", "wareId": OrdinaryShop.blue_offer_washmashine_second.waremd5},
                ],
            },
            "filters": [{"id": "with-services"}],
            "debug": {
                "brief": {"filters": {"WITH_SERVICES": 4}}  # проверяем, что скрытие произошло именно по нашему фильтру
            },
        }

        filtered_responses[RID_WITH_SERVICES_NOT_NOSCOW] = {
            "search": {
                "total": 2,
                "totalOffers": 2,
                "totalOffersBeforeFilters": 9,
                "totalPassedAllGlFilters": 2,
                "results": [
                    {"entity": "offer", "wareId": "offer_with_services__w"},
                    {"entity": "offer", "wareId": "offer_with_serv_sub__w"},
                ],
            },
            "filters": [{"id": "with-services"}],
            "debug": {
                "brief": {"filters": {"WITH_SERVICES": 7}}  # проверяем, что скрытие произошло именно по нашему фильтру
            },
        }

        empty_response = {
            "search": {
                "total": 0,
                "totalOffers": 0,
            }
        }
        response_with_services = {
            "search": {
                "total": 2,
                "totalOffers": 2,
                "results": [
                    {"entity": "offer", "wareId": "offer_with_services__w"},
                    {"entity": "offer", "wareId": "offer_with_serv_sub__w"},
                ],
            }
        }
        responses_by_hid = {}
        responses_by_hid[HID_WITH_SERVICE] = {
            "search": {
                "total": 3,
                "totalOffers": 3,
                "results": [
                    {"entity": "offer", "wareId": "offer_with_serv_sub__w"},
                    {"entity": "offer", "wareId": "offer_with_services__w"},
                    {"entity": "offer", "wareId": "no_services_vendor___w"},
                ],
            }
        }
        responses_by_hid[HID_WITHOUT_SERVICE] = {
            "search": {
                "total": 2,
                "totalOffers": 2,
                "results": [
                    {"entity": "offer", "wareId": "plain_no_services____w"},
                    {"entity": "offer", "wareId": "no_services_hid______w"},
                ],
            }
        }
        responses_by_vendor = {}
        responses_by_vendor[VENDOR_WITH_SERVICE] = {
            "search": {
                "total": 3,
                "totalOffers": 3,
                "results": [
                    {"entity": "offer", "wareId": "offer_with_serv_sub__w"},
                    {"entity": "offer", "wareId": "offer_with_services__w"},
                    {"entity": "offer", "wareId": "no_services_hid______w"},
                ],
            }
        }
        responses_by_vendor[VENDOR_WITHOUT_SERVICE] = {
            "search": {
                "total": 2,
                "totalOffers": 2,
                "results": [
                    {"entity": "offer", "wareId": "plain_no_services____w"},
                    {"entity": "offer", "wareId": "no_services_vendor___w"},
                ],
            }
        }
        responses_filtered_by_vendor = {
            "search": {
                "total": 2,
                "totalOffers": 2,
                "results": [
                    {"entity": "offer", "wareId": "offer_with_serv_sub__w"},
                    {"entity": "offer", "wareId": "offer_with_services__w"},
                ],
            }
        }

        sku_filter_response = {
            "search": {
                "total": 1,
                "totalOffers": 1,
                "results": [
                    {
                        "entity": "offer",
                        "wareId": "offer_with_serv_full_w",
                        "services": [
                            {
                                "service_id": 124,
                                "title": "Установка",
                                "description": "Установка оборудования",
                                "price": {"currency": "RUR", "value": "1000"},
                                "service_supplier": {"id": 112, "name": "Мастера на все руки"},
                            }
                        ],
                    }
                ],
            }
        }

        for rid in regions:
            request = 'place=prime&rids={rids}&fesh={fesh}&debug=1'.format(fesh=OrdinaryShop.SHOP_ID, rids=rid)
            filtered_response = filtered_responses[rid]
            # проверяем работу самого фильтра с различными комбинациями фильтра и rearr-флага
            rearr_variants = ['', '&rearr-factors=market_enable_services={}']
            filter_variants = ['', '&with-services={}']
            for service_filter in filter_variants:
                for rearr in rearr_variants:
                    for filter_value in 0, 1:
                        for rearr_value in 0, 1:
                            is_filter_enabled = len(service_filter) > 0 and filter_value
                            is_services_enabled = len(rearr) == 0 or rearr_value == 1
                            expected_response = (
                                filtered_response if is_filter_enabled and is_services_enabled else full_response
                            )
                            current_request = (
                                request.format(rids=rid)
                                + service_filter.format(filter_value)
                                + rearr.format(rearr_value)
                            )
                            response = self.report.request_json(current_request)
                            self.assertFragmentIn(response, expected_response)
            # проверяем комбинации фильтра with-services с другими фильтрами
            request = 'place=prime&rids={rids}&fesh={fesh}&rearr-factors=market_enable_services={services_enabled}&debug=1'.format(
                fesh=OrdinaryShop.SHOP_ID, rids=rid, services_enabled=1
            )
            # фильтр по hid
            filter_part = '&hid={hid}&with-services={with_services}'
            for hid in HID_WITH_SERVICE, HID_WITHOUT_SERVICE:
                for services_filter_enabled in 0, 1:
                    current_request = request + filter_part.format(hid=hid, with_services=services_filter_enabled)
                    expected_response = None
                    if services_filter_enabled:
                        expected_response = empty_response if hid == HID_WITHOUT_SERVICE else response_with_services
                    else:
                        expected_response = responses_by_hid[hid]
                    response = self.report.request_json(current_request)
                    self.assertFragmentIn(response, expected_response)

            # фильтр по вендору
            filter_part = '&vendor_id={vendor}&with-services={with_services}'
            for vendor in VENDOR_WITH_SERVICE, VENDOR_WITHOUT_SERVICE:
                for services_filter_enabled in 0, 1:
                    current_request = request + filter_part.format(vendor=vendor, with_services=services_filter_enabled)
                    expected_response = None
                    if services_filter_enabled:
                        expected_response = (
                            empty_response if vendor == VENDOR_WITHOUT_SERVICE else responses_filtered_by_vendor
                        )
                    else:
                        expected_response = responses_by_vendor[vendor]
                    response = self.report.request_json(current_request)
                    self.assertFragmentIn(response, expected_response)
            # фильтр по sku
            filter_part = '&offer-sku={sku}&with-services={with_services}'
            for sku in MODEL_ID_WITH_SERVICE_BY_SKU, MODEL_ID_WITHOUT_SERVICE_BY_SKU:
                for services_filter_enabled in 0, 1:
                    current_request = request + filter_part.format(sku=sku, with_services=services_filter_enabled)
                    expected_response = (
                        sku_filter_response
                        if not services_filter_enabled or sku == MODEL_ID_WITHOUT_SERVICE_BY_SKU
                        else sku_filter_response
                    )
        # фильтр по региону
        # регион с услугами проверили выше, поэтому проверяем только регион без услуг
        request = (
            'place=prime&rids=312&with-services={with_services}&fesh={fesh}&rearr-factors=market_enable_services=1'
        )
        for services_filter_enabled in 0, 1:
            current_request = request.format(with_services=services_filter_enabled, fesh=OrdinaryShop.SHOP_ID)
            expected_response = empty_response if services_filter_enabled else full_response
            response = self.report.request_json(current_request)
            self.assertFragmentIn(response, expected_response)

    def test_services_disabled_by_experiment(self):
        '''
        при отключенном флажке сервисов быть не должно ни на одном оффере
        '''
        # в регионе где этот сервис доступен
        # сервис должен быть показан
        for rid in [RID_WITH_SERVICES, RID_WITHOUT_SERVICES]:
            for offer in [
                OrdinaryShop.blue_offer_with_service,
                OrdinaryShop.blue_offer_no_services_by_hid,
                OrdinaryShop.blue_offer_no_services_by_vendor,
                OrdinaryShop.blue_offer_no_services,
                DsbsShop.blue_offer_with_service_by_msku_dsbs,
            ]:
                self.check_services(
                    offer=offer,
                    rids=rid,
                    services=Absent(),
                    additional_params=["rearr-factors=market_enable_services=0"],
                )

        for rid in [RID_WITHOUT_SERVICES, RID_SUB_WITH_SERVICES]:
            for offer in [
                OrdinaryShop.blue_offer_with_service_sub,
            ]:
                self.check_services(
                    offer=offer,
                    rids=rid,
                    services=Absent(),
                    additional_params=["rearr-factors=market_enable_services=0"],
                )

    def test_services_filter_in_output(self):
        '''
        Проверяем, что фильтр with-services появляется на выдаче только в случае наличия офферов с услугами
        '''
        request = 'place=prime&fesh={fesh}&rids=213&rearr-factors=market_enable_services=1'
        # проверяем наличие фильтра, когда офферы с услугами есть на выдаче
        response = self.report.request_json(request.format(fesh=OrdinaryShop.SHOP_ID))
        self.assertFragmentIn(
            response,
            {"filters": [{"id": "with-services", "values": [{"value": "0", "found": 4}, {"value": "1", "found": 5}]}]},
        )
        # проверяем, что если офферов с услугами нет - то и фильтр не попадает на выдачу
        request += '&hid={hid}'
        response = self.report.request_json(request.format(fesh=OrdinaryShop.SHOP_ID, hid=HID_WITHOUT_SERVICE))
        self.assertFragmentNotIn(response, {"search": {"filters": [{"id": "with-services"}]}})

    def test_services_by_sku(self):
        '''
        Проверяем, способ задания услуг через таблицу service_id -> sku_list
        '''
        request = 'place=prime&rids=213&rearr-factors=market_enable_services=1&offer-sku={}&with-services=1'
        for offer, msku in [
            (OrdinaryShop.blue_offer_washmashine_first, OrdinaryShop.msku_washmashine_first),
            (OrdinaryShop.blue_offer_washmashine_second, OrdinaryShop.msku_washmashine_second),
        ]:
            response = self.report.request_json(request.format(msku.sku))
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "total": 1,
                        "totalOffers": 1,
                        "results": [
                            {
                                "entity": "offer",
                                "wareId": offer.waremd5,
                                "services": [
                                    {
                                        "service_id": Services.wash_machine_service.service_id,
                                        "title": Services.wash_machine_service.title,
                                        "description": Services.wash_machine_service.description,
                                        "price": {"currency": "RUR", "value": "1500"},
                                        "service_supplier": {"id": 112, "name": "Мастера на все руки"},
                                    }
                                ],
                            }
                        ],
                    },
                },
            )


if __name__ == '__main__':
    main()
