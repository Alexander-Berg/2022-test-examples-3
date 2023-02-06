#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from itertools import chain

from core.types import (
    CategoryRestriction,
    ClickType,
    Currency,
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryOption,
    DeliveryServiceRegionToRegionInfo,
    DynamicDaysSet,
    DynamicDeliveryServiceInfo,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseInfo,
    DynamicWarehouseToWarehouseInfo,
    DynamicWarehousesPriorityInRegion,
    GLParam,
    GLType,
    GLValue,
    ImagePickerData,
    Model,
    ModelGroup,
    Offer,
    Opinion,
    ParameterValue,
    RegionalDelivery,
    RegionalRestriction,
    Shop,
    ShopOperationalRating,
    UrlType,
    Vendor,
)
from core.testcase import (
    TestCase,
    main,
)
from core.types.sku import (
    MarketSku,
    BlueOffer,
)
from core.types.taxes import (
    Vat,
    Tax,
)
from core.matcher import (
    Absent,
    NotEmpty,
)
from core.types.hypercategory import ADULT_CATEG_ID


SHORT_HTML_DESCRIPTION_BASE = ''.join(
    ['<b>Text{}</b>'.format(i) for i in range(100, 164)] + ['<b>Text{}</b>'.format(i) for i in range(200, 209)]
)

SHORT_HTML_DESCRIPTION = '<u>' + SHORT_HTML_DESCRIPTION_BASE + '</u>'
FULL_HTML_DESCRIPTION = '<u>' + SHORT_HTML_DESCRIPTION_BASE + '<b>World!</b>' * 10 + '</u>'

SHORT_PLAIN_DESCRIPTION = ''.join(['Text{} '.format(i) for i in range(100, 164)])[:-1]  # Последний пробел выкидывается
FULL_PLAIN_DESCRIPTION = ''.join(
    [SHORT_PLAIN_DESCRIPTION, ' ', ''.join(['Text{} '.format(i) for i in range(200, 209)]), 'World! ' * 10]
)[:-1]

MSKUS_IN_ONE_GROUP = [308, 309]
GROUP_ID = 30
GROUP_ID_BLUE_SPECIFIC = 31


class T(TestCase):
    sku1_offer1 = BlueOffer(
        price=5,
        price_old=8,
        vat=Vat.VAT_10,
        feedid=3,
        offerid='blue.offer.1.1',
        waremd5='Sku1Price5-IiLVm1Goleg',
        randx=12,
        delivery_buckets=[2],
    )
    sku1_offer2 = BlueOffer(
        price=7,
        price_old=8,
        vat=Vat.VAT_10,
        feedid=3,
        offerid='blue.offer.1.2',
        waremd5='Sku1Price7-IiLVm1Goleg',
        randx=11,
        delivery_buckets=[2],
    )
    sku2_offer1 = BlueOffer(
        price=6,
        price_old=8,
        vat=Vat.VAT_10,
        feedid=3,
        offerid='blue.offer.2.1',
        waremd5='Sku2Price6-IiLVm1Goleg',
        delivery_buckets=[2],
    )
    sku15_offer1 = BlueOffer(
        price=8,
        price_old=8,
        vat=Vat.VAT_10,
        feedid=4,
        offerid='blue.offer.15.1',
        waremd5='Sku15Price8__________g',
        delivery_buckets=[1, 2],
    )
    sku15_offer2 = BlueOffer(
        price=9,
        price_old=8,
        vat=Vat.VAT_10,
        feedid=5,
        offerid='blue.offer.15.2',
        waremd5='Sku15Price9__________g',
        delivery_buckets=[2],
    )
    sku77_offer1 = BlueOffer(
        price=77,
        feedid=7,
        supplier_id=7,
        offerid='blue.offer.77.1',
        waremd5='Sku77Price77_________g',
        delivery_buckets=[7],
    )
    sku77_offer2 = BlueOffer(
        price=78,
        feedid=8,
        supplier_id=8,
        offerid='blue.offer.77.2',
        waremd5='Sku77Price78_________g',
        delivery_buckets=[8],
    )

    @classmethod
    def prepare(cls):
        cls.settings.disable_random = 1
        cls.settings.microseconds_for_disabled_random = 1483228800000000  # 01/01/2017 @ 12:00am (UTC)

        cls.index.shops += [
            Shop(
                fesh=1,
                datafeed_id=1,
                priority_region=213,
                name='virtual_shop',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                cpa=Shop.CPA_REAL,
            ),
            Shop(
                fesh=3,
                datafeed_id=3,
                priority_region=2,
                name='blue_shop_1',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                blue='REAL',
                cpa=Shop.CPA_REAL,
            ),
            Shop(
                fesh=4,
                datafeed_id=4,
                priority_region=2,
                name='blue_shop_4',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
                warehouse_id=147,
            ),
            Shop(
                fesh=5,
                datafeed_id=5,
                priority_region=213,
                name='blue_shop_5',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
                warehouse_id=145,
            ),
            Shop(
                fesh=7,
                datafeed_id=7,
                priority_region=77,
                name='blue_shop_7',
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                warehouse_id=77,
            ),
            Shop(
                fesh=8,
                datafeed_id=8,
                priority_region=77,
                name='blue_shop_8',
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                warehouse_id=77,
            ),
            Shop(
                fesh=11,
                datafeed_id=11,
                priority_region=68,
                name='blue_shop_6',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
                warehouse_id=303,
            ),
        ]

        cls.index.shop_operational_rating += [
            ShopOperationalRating(
                calc_time=1589936458409,
                shop_id=3,
                late_ship_rate=5.9,
                cancellation_rate=1.93,
                return_rate=0.14,
                total=99.8,
            ),
        ]

        def get_warehouse_and_delivery_service(warehouse_id, service_id):
            date_switch_hours = [DateSwitchTimeAndRegionInfo(date_switch_hour=1, region_to=225)]
            return DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=warehouse_id,
                delivery_service_id=service_id,
                operation_time=0,
                date_switch_time_infos=date_switch_hours,
                shipment_holidays_days_set_key=1,
            )

        def delivery_service_region_to_region_info():
            return DeliveryServiceRegionToRegionInfo(region_from=213, region_to=225, days_key=1)

        cls.settings.lms_autogenerate = False
        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=147, home_region=2, holidays_days_set_key=1),
            DynamicWarehouseInfo(id=145, home_region=213, holidays_days_set_key=1),
            DynamicWarehouseInfo(id=77, home_region=77, holidays_days_set_key=1),
            DynamicWarehouseInfo(id=303, home_region=68, holidays_days_set_key=1),
            DynamicWarehouseToWarehouseInfo(warehouse_from=147, warehouse_to=147),
            DynamicWarehouseToWarehouseInfo(warehouse_from=145, warehouse_to=145),
            DynamicWarehouseToWarehouseInfo(warehouse_from=77, warehouse_to=77),
            DynamicWarehouseToWarehouseInfo(warehouse_from=303, warehouse_to=303),
            # DynamicWarehousesPriorityInRegion(region=213, warehouses=[145, 147]),
            DynamicWarehousesPriorityInRegion(region=225, warehouses=[145, 147]),
            DynamicWarehousesPriorityInRegion(region=2, warehouses=[147]),
            DynamicWarehousesPriorityInRegion(region=77, warehouses=[77]),
            DynamicWarehousesPriorityInRegion(region=68, warehouses=[303]),
            get_warehouse_and_delivery_service(145, 48),
            get_warehouse_and_delivery_service(147, 48),
            get_warehouse_and_delivery_service(77, 77),
            get_warehouse_and_delivery_service(303, 50),
            DynamicDeliveryServiceInfo(id=48, name="c_48"),
            DynamicDeliveryServiceInfo(id=50, name="c_50"),
            DynamicDeliveryServiceInfo(id=77, name="ds_77"),
            DynamicDaysSet(key=1, days=[]),
        ]
        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=1,
                fesh=1,
                carriers=[48],
                regional_options=[
                    RegionalDelivery(rid=2, options=[DeliveryOption(price=5, day_from=1, day_to=2)]),
                ],
            ),
            DeliveryBucket(
                bucket_id=2,
                fesh=1,
                carriers=[48],
                regional_options=[
                    RegionalDelivery(rid=213, options=[DeliveryOption(price=5, day_from=1, day_to=2)]),
                ],
            ),
            DeliveryBucket(
                bucket_id=7,
                fesh=7,
                carriers=[77],
                regional_options=[
                    RegionalDelivery(rid=77, options=[DeliveryOption(price=5, day_from=1, day_to=2)]),
                ],
            ),
            DeliveryBucket(
                bucket_id=8,
                fesh=8,
                carriers=[77],
                regional_options=[
                    RegionalDelivery(rid=77, options=[DeliveryOption(price=5, day_from=1, day_to=2)]),
                ],
            ),
            DeliveryBucket(
                bucket_id=11,
                fesh=11,
                carriers=[49],
                regional_options=[
                    RegionalDelivery(rid=68, options=[DeliveryOption(price=5, day_from=1, day_to=2)]),
                ],
            ),
        ]

        cls.index.gltypes += [
            GLType(param_id=101, hid=1, cluster_filter=False, gltype=GLType.ENUM, values=[1, 2]),
            GLType(param_id=102, hid=1, cluster_filter=False, gltype=GLType.NUMERIC),
            GLType(param_id=103, hid=1, cluster_filter=False, gltype=GLType.BOOL, hasboolno=False),
            GLType(param_id=104, hid=1, cluster_filter=False, gltype=GLType.BOOL, hasboolno=True),
            GLType(
                param_id=201,
                hid=1,
                cluster_filter=True,
                model_filter_index=3,
                gltype=GLType.ENUM,
                subtype='image_picker',
                values=[
                    GLValue(
                        value_id=1,
                        image=ImagePickerData(
                            url='//avatars.mds.yandex.net/get-mpic/466729/img_mbo_201_1/orig',
                            namespace="get-mpic",
                            group_id="466729",
                            image_name="img_mbo_201_1",
                        ),
                        position=1,
                    ),
                    3,
                    GLValue(
                        value_id=2,
                        image=ImagePickerData(
                            url='//avatars.mds.yandex.net/get-mpic/466729/img_mbo_201_2/orig',
                            namespace="get-mpic",
                            group_id="466729",
                            image_name="img_mbo_201_2",
                        ),
                        position=2,
                    ),
                ],
            ),
            GLType(param_id=202, hid=1, cluster_filter=True, model_filter_index=2, gltype=GLType.ENUM),
            GLType(param_id=203, hid=1, cluster_filter=True, model_filter_index=1, gltype=GLType.BOOL, hasboolno=False),
            GLType(param_id=204, hid=1, cluster_filter=True, model_filter_index=0, gltype=GLType.BOOL, hasboolno=True),
            GLType(param_id=205, hid=1, cluster_filter=True, model_filter_index=4, gltype=GLType.NUMERIC),
            # Фильтры, к которым не прикреплены оферы.
            # У такого фильтра initialFound равен 0. Он должен быть скрыт с выдачи на синем прайме
            GLType(param_id=301, hid=1, cluster_filter=True, model_filter_index=5, gltype=GLType.ENUM, values=[1, 2]),
            # Фильтры для проверки неточного перехода
            GLType(param_id=201, hid=2, cluster_filter=True, model_filter_index=6, gltype=GLType.ENUM),
            GLType(param_id=205, hid=2, cluster_filter=True, model_filter_index=7, gltype=GLType.NUMERIC),
            GLType(param_id=204, hid=2, cluster_filter=True, model_filter_index=8, gltype=GLType.BOOL, hasboolno=True),
        ]

        cls.index.models += [
            Model(
                hyperid=1,
                hid=1,
                title='blue and green model',
                glparams=[
                    GLParam(param_id=101, value=1),
                    GLParam(param_id=102, value=1),
                    GLParam(param_id=103, value=1),
                    GLParam(param_id=104, value=1),
                ],
                parameter_value_links=[
                    ParameterValue(
                        param_id=201,
                        option_id=3,
                        picture=ImagePickerData(
                            url='//avatars.mds.yandex.net/get-mpic/466729/img_model1_201_3/orig',
                            namespace="get-mpic",
                            group_id='466729',
                            image_name='img_model1_201_3',
                        ),
                    ),
                    ParameterValue(
                        param_id=201,
                        option_id=2,
                        picture=ImagePickerData(
                            url='//avatars.mds.yandex.net/get-mpic/466729/img_model1_201_2/orig',
                            namespace="get-mpic",
                            group_id='466729',
                            image_name='img_model1_201_2',
                        ),
                    ),
                ],
                opinion=Opinion(reviews=100500, total_count=3, rating=4.5, rating_count=17),
            ),
            Model(
                title='hidden_model_250340000',
                hyperid=250340000,
                hid=2,
                vendor_min_publish_timestamp=2052777600,  # 01/19/2035 @ 12:00am (UTC)
            ),
            Model(
                title='hidden_model_250340001',
                hyperid=250340001,
                hid=2,
                vendor_min_publish_timestamp=2052777600,  # 01/19/2035 @ 12:00am (UTC)
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                title="blue offer sku1 john",
                hyperid=1,
                sku=1,
                blue_offers=[T.sku1_offer1, T.sku1_offer2],
                descr=FULL_HTML_DESCRIPTION,
                glparams=[
                    GLParam(param_id=201, value=1),
                    GLParam(param_id=202, value=1),
                    GLParam(param_id=205, value=1),
                ],
                randx=1,
            ),
            MarketSku(
                title="blue offer sku2",
                hyperid=1,
                sku=2,
                blue_offers=[T.sku2_offer1],
                glparams=[
                    GLParam(param_id=201, value=2),
                    GLParam(param_id=202, value=1),
                    GLParam(param_id=205, value=1),
                ],
                randx=2,
            ),
            MarketSku(
                title="blue offer sku3",
                hyperid=1,
                sku=3,
                glparams=[
                    GLParam(param_id=201, value=3),
                    GLParam(param_id=202, value=2),
                    GLParam(param_id=205, value=3),
                ],
                randx=3,
            ),
            MarketSku(
                title="blue offer sku4",
                hyperid=2,
                sku=4,
                blue_offers=[
                    BlueOffer(
                        price=5,
                        vat=Vat.VAT_10,
                        feedid=3,
                        offerid='blue.offer.4.1',
                        delivery_buckets=[2],
                    )
                ],
                glparams=[
                    GLParam(param_id=201, value=2),
                    GLParam(param_id=202, value=1),
                    GLParam(param_id=205, value=1),
                ],
                randx=4,
            ),
            MarketSku(
                title="blue offer sku5",
                hyperid=1,
                sku=5,
                glparams=[
                    GLParam(param_id=201, value=1),
                    GLParam(param_id=202, value=1),
                    GLParam(param_id=205, value=2),
                ],
                randx=5,
            ),
            MarketSku(
                title="blue offer sku15", blue_offers=[T.sku15_offer1, T.sku15_offer2], randx=15, sku=15, hyperid=3
            ),
            MarketSku(
                title="blue offer sku25034000",
                hyperid=250340000,
                sku=25034000,
                randx=3,
            ),
            MarketSku(
                title="blue offer sku25034001",
                hyperid=250340001,
                sku=25034001,
                randx=3,
            ),
            MarketSku(
                title="blue msku77",
                blue_offers=[T.sku77_offer1, T.sku77_offer2],
                hyperid=77,
                sku=77,
            ),
        ]

        cls.index.mskus += [MarketSku(title="Sku_{}".format(i), hyperid=2, sku=i, randx=i) for i in range(6, 12)]

        cls.index.category_restrictions += [
            CategoryRestriction(name='ask_18', hids=[ADULT_CATEG_ID], regional_restrictions=[RegionalRestriction()])
        ]

    def test_banned_sku_offers_not_shown(self):
        for msku in [25034000, 25034001]:
            response = self.report.request_json(
                'place=sku_offers&market-sku={}&show-urls=direct,cpa&rids=213&rgb=BLUE'.format(msku)
            )
            self.assertFragmentIn(response, {"total": 0, "results": []}, allow_different_len=False)

    def test_banned_sku_offers_shown_with_debug_ignore_min_vendor_publish_timestamp(self):
        for msku in [25034000, 25034001]:
            response = self.report.request_json(
                'place=sku_offers&market-sku={}&show-urls=direct,cpa&rids=213&rgb=BLUE&debug-ignore-vendor-min-publish-timestamp=true'.format(
                    msku
                )
            )
            self.assertFragmentIn(response, {"total": 1, "results": [{}]}, allow_different_len=False)

    def test_sku_offers_format(self):
        """
        Что проверяем: формат выдачи карточки SKU - Stock Keeping Unit
        https://wiki.yandex-team.ru/market/pokupka/streamline/bluemarket/bluemarket-tech/dev/
        SKU - детализированная до последнего параметра модель,
        объединяющая несколько оферов, полностью идентичных во всех параметрах.
        В выдаче отдаются поля, присущие СКУ (общие поля офера) и модель
        Если данный SKU есть в продаже (на складе есть оферы этого SKU),
        то в поле offers.items указывается дефолтный офер (тот¸ который будем продавать).
        Дефолтный офер не песимизируется до cpc
        """
        response = self.report.request_json('place=sku_offers&market-sku=1&show-urls=direct,cpa&rids=213&rgb=BLUE')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "sku",
                        "titles": {"raw": "blue offer sku1 john"},
                        "description": SHORT_PLAIN_DESCRIPTION,
                        "formattedDescription": {
                            "shortPlain": SHORT_PLAIN_DESCRIPTION,
                            "fullPlain": FULL_PLAIN_DESCRIPTION,
                            "shortHtml": SHORT_HTML_DESCRIPTION,
                            "fullHtml": FULL_HTML_DESCRIPTION,
                        },
                        "id": "1",
                        "showUid": NotEmpty(),
                        "product": {
                            "id": 1,
                        },
                        "offers": {
                            "items": [
                                {
                                    "entity": "offer",
                                    "marketSku": "1",
                                    "ownMarketPlace": True,
                                    "cpa": "real",
                                    "fee": NotEmpty(),
                                    "wareId": T.sku1_offer1.waremd5,
                                    "benefit": {
                                        "type": "default",
                                        "isPrimary": True,
                                        "description": "Хорошая цена от надёжного магазина",
                                    },
                                    "prices": {
                                        "discount": {
                                            "oldMin": str(T.sku1_offer1.price_old),
                                        },
                                    },
                                }
                            ]
                        },
                    }
                ]
            },
            allow_different_len=False,
        )

        self.show_log.expect(
            ware_md5=T.sku1_offer1.waremd5,
            rgb='BLUE',
            msku='1',
            supplier_id=3,
            supplier_type=Shop.FIRST_PARTY,
            url_type=UrlType.CPA,
        ).once()
        self.click_log.expect(
            ware_md5=T.sku1_offer1.waremd5,
            rgb='BLUE',
            msku='1',
            supplier_id=3,
            supplier_type=Shop.FIRST_PARTY,
            clicktype=ClickType.CPA,
        ).once()

    def test_show_model(self):
        """
        Что проверяем: выдачу информации о модели, если был передан параметр show-models=1
        """

        for pipeline in [0, 1]:
            request = 'place=sku_offers&market-sku=1&show-urls=direct&rids=213&rearr-factors=use_new_jump_table_pipeline={}'.format(
                pipeline
            )

            # Если параметра нет или он равен 0, то выводится только идентификатор модели
            for param in ['', '&show-models=0']:
                response = self.report.request_json(request + param)
                self.assertFragmentIn(
                    response,
                    {
                        "results": [
                            {
                                "entity": "sku",
                                "id": "1",
                                "product": {
                                    "id": 1,
                                    "entity": Absent(),
                                },
                            }
                        ]
                    },
                    allow_different_len=False,
                )

            response = self.report.request_json(request + '&show-models=1')
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "entity": "sku",
                            "id": "1",
                            "product": {
                                "id": 1,
                                "entity": "product",
                                "titles": {"raw": "blue and green model"},
                                "opinions": 3,
                                "rating": 4.5,
                                "ratingCount": 17,
                                "reviews": 100500,
                            },
                        }
                    ]
                },
                allow_different_len=False,
            )

    def test_sku_offers_without_default_offer(self):
        """
        Что проверяем: формат выдачи карточки SKU, которой нет в продаже. В этом случае список оферов пуст.
        """
        for pipeline in [0, 1]:
            response = self.report.request_json(
                'place=sku_offers&market-sku=3&show-urls=direct&rids=213&rearr-factors=use_new_jump_table_pipeline={}'.format(
                    pipeline
                )
            )
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "entity": "sku",
                            "id": "3",
                            "offers": {"items": Absent()},
                        }
                    ]
                },
                allow_different_len=False,
            )

    def test_sku_offers_batch(self):
        """
        Что проверяем: возможность запроса нескольких СКУ за один раз
        В этом случае выводятся все запрошенные СКУ и дефолтные оферы к ним
        Было ограничение в количестве выводимых за раз СКУ. Связано было с ограничением в numdoc=10 (по-умолчанию).
        На мете остальные документы отбрасывались. Пришлось ввести группировку
        и запрашивать в два раза больше документов, чем запрашивается СКУ (СКУ + дефолтный офер для него) +
        кол-во оферов, которые запросили.
        """

        result = [
            {
                "entity": "sku",
                "id": "{}".format(i),
            }
            for i in range(3, 12)
        ]

        for pipeline in [0, 1]:
            response = self.report.request_json(
                'place=sku_offers&market-sku=1,2,3,4,5,6,7,8,9,10,11&show-urls=direct&rids=213&offerid={},{}&debug=da&rearr-factors=use_new_jump_table_pipeline={}'.format(
                    T.sku1_offer1.waremd5, T.sku2_offer1.waremd5, pipeline
                )
            )
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "entity": "sku",
                            "titles": {"raw": "blue offer sku1 john"},
                            "id": "1",
                            "product": {
                                "id": 1,
                            },
                            "offers": {
                                "items": [{"entity": "offer", "marketSku": "1", "wareId": T.sku1_offer1.waremd5}]
                            },
                        },
                        {
                            "entity": "sku",
                            "titles": {"raw": "blue offer sku2"},
                            "id": "2",
                            "product": {
                                "id": 1,
                            },
                            "offers": {
                                "items": [
                                    {
                                        "entity": "offer",
                                        "marketSku": "2",
                                        "wareId": T.sku2_offer1.waremd5,
                                    }
                                ]
                            },
                        },
                    ]
                    + result
                },
                allow_different_len=False,
            )

    def test_sku_offers_preferred_offer(self):
        """
        Что проверяем: возвращается предпочитаемый дефолтный офер у СКУ.
        В запросе к плэйсу передается идентификатор офера, который ожидаем увидеть как дефолтный,
        даже если он не лучший в данный момент.
        """
        for pipeline in [0, 1]:
            request = 'place=sku_offers&market-sku=1&show-urls=direct&rids=213&rearr-factors=use_new_jump_table_pipeline={}'.format(
                pipeline
            )

            # Если ни один идентификатор не передан, то берется лучший
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "entity": "sku",
                            "id": "1",
                            "offers": {
                                "items": [
                                    {
                                        "entity": "offer",
                                        "marketSku": "1",
                                        "wareId": T.sku1_offer1.waremd5,
                                    }
                                ]
                            },
                        }
                    ]
                },
                allow_different_len=False,
            )

            # Если есть идентификатор офера, то берется он
            response = self.report.request_json(request + '&offerid={}'.format(T.sku1_offer2.waremd5))
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "entity": "sku",
                            "id": "1",
                            "offers": {
                                "items": [
                                    {
                                        "entity": "offer",
                                        "marketSku": "1",
                                        "wareId": T.sku1_offer2.waremd5,
                                    }
                                ]
                            },
                        }
                    ]
                },
                allow_different_len=False,
            )

            # Если идентификатор офера не валиден или не принадлежит данному СКУ, то берется лучший
            response = self.report.request_json(request + '&offerid={}'.format(T.sku2_offer1.waremd5))
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "entity": "sku",
                            "id": "1",
                            "offers": {
                                "items": [
                                    {
                                        "entity": "offer",
                                        "marketSku": "1",
                                        "wareId": T.sku1_offer1.waremd5,
                                    }
                                ]
                            },
                        }
                    ]
                },
                allow_different_len=False,
            )

    def test_preferred_offer_filtered_by_delivery(self):
        """
        Оффер sku15_offer2 доставляется только по региону 213.
        Здесь проверяется, что его отбрасывание (для региона 2) приводит к выдаче лучшего оффера.
        """
        for pipeline in [0, 1]:
            request = 'place=sku_offers&market-sku=15&show-urls=direct&rids=2&offerid={}&rearr-factors=use_new_jump_table_pipeline={};market_nordstream=0'.format(
                T.sku15_offer2.waremd5, pipeline
            )
            self.assertFragmentIn(
                self.report.request_json(request),
                {
                    "results": [
                        {
                            "entity": "sku",
                            "id": "15",
                            "offers": {
                                "items": [{"entity": "offer", "marketSku": "15", "wareId": T.sku15_offer1.waremd5}]
                            },
                        }
                    ]
                },
                allow_different_len=False,
            )

    def test_sku_offers_preferred_offer_batch(self):
        """
        Что проверяем: множественный запрос с предпочитаемыми дефолтными оферами у СКУ.
        Для каждого СКУ будет выведен свой дефолтный офер
        """
        for pipeline in [0, 1]:
            response = self.report.request_json(
                'place=sku_offers&market-sku=1,2&offerid={},{}&show-urls=direct&rids=213&rearr-factors=use_new_jump_table_pipeline={}'.format(
                    T.sku1_offer2.waremd5, T.sku2_offer1.waremd5, pipeline
                )
            )
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "entity": "sku",
                            "id": "2",
                            "offers": {
                                "items": [
                                    {
                                        "entity": "offer",
                                        "marketSku": "2",
                                        "wareId": T.sku2_offer1.waremd5,
                                    }
                                ]
                            },
                        },
                        {
                            "entity": "sku",
                            "id": "1",
                            "offers": {
                                "items": [
                                    {
                                        "entity": "offer",
                                        "marketSku": "1",
                                        "wareId": T.sku1_offer2.waremd5,
                                    }
                                ]
                            },
                        },
                    ]
                },
                allow_different_len=False,
            )

    def test_sku_offers_jump_table(self):
        """
        Что тестируем: таблицу переходов для карточки СКУ.
        Для каждого параметра второго рода (пока что только enum) строится таблица переходов:
        какой СКУ будет соответствовать, если для данного фильтра выбрать данное значение,
        при условии фиксации остальных фильтров значениями данного СКУ.
        Фильтры numeric преобразуются в тип enum с подтипом radio (только одно значение за раз)
        СКУ, которых нет в продаже, выбрасываются из рассчета. В данном случае ThirdSku.
        Фильтры с количеством Values меньшим единицы - выбрасываются.
        Проверяется порядок значений в enum фильтре.
        202й фильтр должен быть отброшен в связи с малым количеством значений (1).
        205й фильтр должен быть отброшен в связи с малым количеством значений (1).
        """
        for pipeline in [0, 1]:
            response = self.report.request_json(
                'place=sku_offers&market-sku=1&show-urls=direct&rearr-factors=use_new_jump_table_pipeline={}'.format(
                    pipeline
                )
            )
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "entity": "sku",
                            "id": "1",
                            "filters": [
                                {
                                    "id": "201",
                                    "values": [
                                        {
                                            "id": "1",  # Значение нашего СКУ - переход на этот же СКУ. Значение помечено checked
                                            "marketSku": "1",
                                            "fuzzy": Absent(),
                                            "checked": True,
                                            "slug": "blue-offer-sku1-john",
                                        },
                                        {
                                            "id": "2",  # Точный переход на СКУ 2
                                            "marketSku": "2",
                                            "fuzzy": Absent(),
                                            "checked": Absent(),
                                            "slug": "blue-offer-sku2",
                                        },
                                    ],
                                }
                            ],
                        }
                    ]
                },
                allow_different_len=False,
                preserve_order=True,
            )

            self.assertFragmentNotIn(response, {"filters": [{"id": 101}]})
            self.assertFragmentNotIn(response, {"filters": [{"id": 102}]})
            self.assertFragmentNotIn(response, {"filters": [{"id": 103}]})
            self.assertFragmentNotIn(response, {"filters": [{"id": 104}]})

    def test_sku_offers_jump_table_for_empty_sku(self):
        """
        Что тестируем: таблицу переходов для карточки СКУ. Для sku, которые не имеют офферов
        202й фильт должен быть отброшен всвязи с малым количеством значений(1).
        """
        for pipeline in [0, 1]:
            response = self.report.request_json(
                'place=sku_offers&market-sku=5&show-urls=direct&rearr-factors=use_new_jump_table_pipeline={}'.format(
                    pipeline
                )
            )
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "entity": "sku",
                            "id": "5",
                            "filters": [
                                {
                                    "id": "201",
                                    "values": [
                                        {
                                            "id": "1",  # Значение нашего СКУ - переход на этот же СКУ. Значение помечено checked
                                            "marketSku": "5",
                                            "fuzzy": Absent(),
                                            "checked": True,
                                            "slug": "blue-offer-sku5",
                                        },
                                        {
                                            "id": "2",
                                            "marketSku": "2",
                                            "fuzzy": True,
                                            "checked": Absent(),
                                            "slug": "blue-offer-sku2",
                                        },
                                    ],
                                },
                                {
                                    "id": "205",
                                    "values": [
                                        {
                                            "id": "1~1",
                                            "marketSku": "1",
                                            "fuzzy": Absent(),
                                            "checked": Absent(),
                                            "slug": "blue-offer-sku1-john",
                                        },
                                        {
                                            "id": "2~2",  # Значение нашего СКУ - переход на этот же СКУ. Значение помечено checked
                                            "marketSku": "5",
                                            "fuzzy": Absent(),
                                            "checked": True,
                                            "slug": "blue-offer-sku5",
                                        },
                                    ],
                                },
                            ],
                        }
                    ]
                },
                allow_different_len=False,
                preserve_order=True,
            )

            self.assertFragmentNotIn(response, {"filters": [{"id": 101}]})
            self.assertFragmentNotIn(response, {"filters": [{"id": 102}]})
            self.assertFragmentNotIn(response, {"filters": [{"id": 103}]})
            self.assertFragmentNotIn(response, {"filters": [{"id": 104}]})

    @classmethod
    def prepare_fuzzy_jump_table(cls):
        cls.index.models += [
            Model(hyperid=5, hid=2, title='Model for fuzzy jump table test'),
        ]
        cls.index.mskus += [
            MarketSku(
                hyperid=5,
                sku=110,
                blue_offers=[
                    BlueOffer(
                        price=1,
                        vat=Vat.VAT_10,
                        feedid=3,
                        offerid='blue.offer.110.1',
                        delivery_buckets=[2],
                    ),
                    BlueOffer(
                        price=1,
                        vat=Vat.VAT_10,
                        feedid=3,
                        offerid='blue.offer.110.2',
                        delivery_buckets=[2],
                    ),
                ],
                randx=6,
                glparams=[
                    GLParam(param_id=201, value=1),
                    GLParam(param_id=205, value=1),
                    GLParam(param_id=204, value=1),
                ],
            ),
            MarketSku(
                hyperid=5,
                sku=111,
                blue_offers=[
                    BlueOffer(
                        price=1,
                        vat=Vat.VAT_10,
                        feedid=3,
                        offerid='blue.offer.111.1',
                        delivery_buckets=[2],
                    )
                ],
                randx=7,
                glparams=[
                    GLParam(param_id=201, value=2),
                    GLParam(param_id=205, value=1),
                    GLParam(param_id=204, value=1),
                ],
            ),
            MarketSku(
                hyperid=5,
                sku=112,
                blue_offers=[
                    BlueOffer(
                        price=1,
                        vat=Vat.VAT_10,
                        feedid=3,
                        offerid='blue.offer.112.1',
                        delivery_buckets=[2],
                    ),
                    BlueOffer(
                        price=1,
                        vat=Vat.VAT_10,
                        feedid=3,
                        offerid='blue.offer.112.2',
                        delivery_buckets=[2],
                    ),
                    BlueOffer(
                        price=1,
                        vat=Vat.VAT_10,
                        feedid=3,
                        offerid='blue.offer.112.3',
                        delivery_buckets=[2],
                    ),
                ],
                randx=8,
                glparams=[
                    GLParam(param_id=201, value=2),
                    GLParam(param_id=205, value=2),
                    GLParam(param_id=204, value=0),
                ],
            ),
        ]

    def test_sku_offers_jump_table_fuzzy_old(self):
        """
        Что проверяем: построение таблицы переходов с неточным переходом
        Для 6 СКУ есть точный переход на себя и на СКУ 7 в значении 201:2.
        Не точный переход на СКУ 8 в значении 202:2, т.к. нет СКУ для 201:1, 202:2
        Проверяется поле found - количество оферов для данного СКУ в продаже (старый пайплайн)
        Проверяется порядок enum (201) и числовых (205 преобразованный в енум) фильтров.
        Для числовых используется порядок их значений
        """
        response = self.report.request_json(
            'place=sku_offers&market-sku=110&show-urls=direct&rearr-factors=use_new_jump_table_pipeline=0'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "sku",
                        "id": "110",
                        "filters": [
                            {
                                "id": "201",
                                "values": [
                                    {
                                        "id": "1",
                                        "marketSku": "110",
                                        "fuzzy": Absent(),
                                        "checked": True,
                                        "found": 2,
                                    },
                                    {
                                        "id": "2",
                                        "marketSku": "111",
                                        "fuzzy": Absent(),
                                        "checked": Absent(),
                                        "found": 1,
                                    },
                                ],
                            },
                            {
                                "id": "205",
                                "values": [
                                    {
                                        "id": "1~1",
                                        "marketSku": "110",
                                        "fuzzy": Absent(),
                                        "checked": True,
                                        "found": 2,
                                    },
                                    {
                                        "id": "2~2",
                                        "marketSku": "112",
                                        "fuzzy": True,
                                        "checked": Absent(),
                                        "found": 3,
                                    },
                                ],
                            },
                            {
                                "id": "204",
                                "values": [
                                    {
                                        "id": "1",
                                        "value": "да",
                                        "found": 2,
                                        "marketSku": "110",
                                        "checked": True,
                                        "initialFound": 1,
                                        "slug": "",
                                    },
                                    {
                                        "id": "0",
                                        "value": "нет",
                                        "found": 3,
                                        "marketSku": "112",
                                        "fuzzy": True,
                                        "checked": Absent(),
                                        "initialFound": 0,
                                        "slug": "",
                                    },
                                ],
                            },
                        ],
                    }
                ]
            },
            allow_different_len=False,
            preserve_order=True,
        )

    def test_sku_offers_jump_table_fuzzy_new(self):
        """
        Что проверяем: построение таблицы переходов с неточным переходом
        Для 6 СКУ есть точный переход на себя и на СКУ 7 в значении 201:2.
        Не точный переход на СКУ 8 в значении 202:2, т.к. нет СКУ для 201:1, 202:2
        Проверяется поле found - 1 если sku есть, 0 - иначе (новый пайплайн)
        Проверяется порядок enum (201) и числовых (205 преобразованный в енум) фильтров.
        Для числовых используется порядок их значений
        """
        response = self.report.request_json(
            'place=sku_offers&market-sku=110&show-urls=direct&rearr-factors=use_new_jump_table_pipeline=1'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "sku",
                        "id": "110",
                        "filters": [
                            {
                                "id": "201",
                                "values": [
                                    {
                                        "id": "1",
                                        "marketSku": "110",
                                        "fuzzy": Absent(),
                                        "checked": True,
                                        "found": 1,  # kek 2
                                    },
                                    {
                                        "id": "2",
                                        "marketSku": "111",
                                        "fuzzy": Absent(),
                                        "checked": Absent(),
                                        "found": 1,
                                    },
                                ],
                            },
                            {
                                "id": "204",
                                "values": [
                                    {
                                        "id": "1",
                                        "value": "да",
                                        "found": 1,  # kek 2
                                        "marketSku": "110",
                                        "checked": True,
                                        # "initialFound": 1,
                                        "slug": "",
                                    },
                                    {
                                        "id": "0",
                                        "value": "нет",
                                        "found": 1,  # kek 3
                                        "marketSku": "112",
                                        "fuzzy": True,
                                        "checked": Absent(),
                                        # "initialFound": 0,
                                        "slug": "",
                                    },
                                ],
                            },
                            {
                                "id": "205",
                                "values": [
                                    {
                                        "id": "1~1",
                                        "marketSku": "110",
                                        "fuzzy": Absent(),
                                        "checked": True,
                                        "found": 1,  # kek 2
                                    },
                                    {
                                        "id": "2~2",
                                        "marketSku": "112",
                                        "fuzzy": True,
                                        "checked": Absent(),
                                        "found": 1,  # kek 3
                                    },
                                ],
                            },
                        ],
                    }
                ]
            },
            allow_different_len=False,
            preserve_order=True,
        )

    def test_bool_filters_in_jump_table_old(self):
        """Проверяется преобразование boolean фильтра в enum и построение таблицы переходов для boolean фильтра"""
        response = self.report.request_json(
            'place=sku_offers&market-sku=111&show-urls=direct&rearr-factors=use_new_jump_table_pipeline=0'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "sku",
                        "id": "111",
                        "filters": [
                            {
                                "id": "204",
                                "subType": "radio",
                                "values": [
                                    {
                                        "id": "1",
                                        "value": "да",
                                        "marketSku": "111",
                                        "fuzzy": Absent(),
                                        "checked": True,
                                        "found": 1,
                                    },
                                    {
                                        "id": "0",
                                        "value": "нет",
                                        "marketSku": "112",
                                        "fuzzy": True,
                                        "checked": Absent(),
                                        "found": 3,
                                    },
                                ],
                            }
                        ],
                    }
                ]
            },
        )

    def test_bool_filters_in_jump_table_new(self):
        """Проверяется преобразование boolean фильтра в enum и построение таблицы переходов для boolean фильтра"""
        response = self.report.request_json(
            'place=sku_offers&market-sku=111&show-urls=direct&rearr-factors=use_new_jump_table_pipeline=1'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "sku",
                        "id": "111",
                        "filters": [
                            {
                                "id": "204",
                                "subType": "radio",
                                "values": [
                                    {
                                        "id": "1",
                                        "value": "да",
                                        "marketSku": "111",
                                        "fuzzy": Absent(),
                                        "checked": True,
                                        "found": 1,
                                    },
                                    {
                                        "id": "0",
                                        "value": "нет",
                                        "marketSku": "112",
                                        "fuzzy": True,
                                        "checked": Absent(),
                                        "found": 1,
                                    },
                                ],
                            }
                        ],
                    }
                ]
            },
        )

    def test_sku_offers_no_jump_table_for_single_sku_in_model_old(self):
        """
        Что проверяем: Для единственной СКУ в модели таблицы переходов не строится
        """
        response = self.report.request_json(
            'place=sku_offers&market-sku=4&show-urls=direct&rearr-factors=use_new_jump_table_pipeline=0'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "sku",
                        "id": "4",
                        "filters": [],
                    }
                ]
            },
            allow_different_len=False,
            preserve_order=False,
        )

    def test_sku_offers_no_jump_table_for_single_sku_in_model_new(self):
        """
        Что проверяем: Для единственной СКУ в модели таблицы переходов не строится
        """
        response = self.report.request_json(
            'place=sku_offers&market-sku=4&show-urls=direct&rearr-factors=use_new_jump_table_pipeline=1'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "sku",
                        "id": "4",
                        "filters": Absent(),
                    }
                ]
            },
            allow_different_len=False,
            preserve_order=False,
        )

    def expected_mbo_picker_result(self):
        return {
            "results": [
                {
                    "filters": [
                        {
                            "id": "201",
                            "subType": "image_picker",
                            "values": [
                                {
                                    "id": "1",
                                    "picker": {"imageName": "img_mbo_201_1"},
                                },
                                {
                                    "id": "2",
                                    "picker": {"imageName": "img_mbo_201_2"},
                                },
                            ],
                        }
                    ],
                }
            ]
        }

    def test_sku_filter_picker_old(self):
        """
        Что проверяем: наличие пикеров у фильтров СКУ.
        Пикер первого значения берется из mbo, т.к. у модели этот пикер отстутствует
        Пикер второго значения берется из параметров модели
        """
        request = "place=sku_offers&market-sku=1&show-urls=direct&rids=213&show-models={}&rearr-factors=use_new_jump_table_pipeline=0"

        response = self.report.request_json(request.format(1))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "filters": [
                            {
                                "id": "201",
                                "subType": "image_picker",
                                "values": [
                                    {
                                        "id": "1",
                                        "picker": {"imageName": "img_mbo_201_1"},
                                    },
                                    {
                                        "id": "2",
                                        "picker": {"imageName": "img_model1_201_2"},
                                    },
                                ],
                            }
                        ],
                    }
                ]
            },
        )

        # Если детальная информация о модели не запрашивается, то и информация о пикерах модели не публикуется
        # Пикеры из mbo при этом остаются
        response = self.report.request_json(request.format(0))
        self.assertFragmentIn(response, self.expected_mbo_picker_result())

    def test_sku_filter_picker_new(self):
        """
        Что проверяем: наличие пикеров у фильтров СКУ.
        В новом пайплайне наличие картинок не зависит от подробной информации для модели,
        проверяем, что результат одинаков.
        """
        request = "place=sku_offers&market-sku=1&show-urls=direct&rids=213&show-models={}&rearr-factors=use_new_jump_table_pipeline=1"

        response = self.report.request_json(request.format(1))
        self.assertFragmentIn(response, self.expected_mbo_picker_result())
        response = self.report.request_json(request.format(0))
        self.assertFragmentIn(response, self.expected_mbo_picker_result())

    @classmethod
    def prepare_sku_offers_without_white_offers(cls):
        cls.index.mskus += [
            MarketSku(
                title="with white cpc",
                hyperid=10000,
                sku=101,
                blue_offers=[],
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                title="with white cpa",
                hyperid=10000,
                sku=102,
                blue_offers=[],
            ),
        ]

        cls.index.shops += [Shop(fesh=222), Shop(fesh=333, cpa=Shop.CPA_REAL)]
        cls.index.offers += [
            # Белый cpc-офер имеет такой же MSKU, но не будет искаться на sku_offers
            Offer(title='white_cpc_offer_with_msku', fesh=222, price=2, hyperid=10000, sku=101),
            Offer(title='white_cpa_offer_with_msku', fesh=333, price=2, hyperid=10000, sku=102, cpa=Offer.CPA_REAL),
        ]

    def test_sku_offers_with_white_offers(self):

        response = self.report.request_json('place=sku_offers&market-sku=101&rgb=BLUE')
        self.assertFragmentIn(response, {"entity": "sku", "offers": {"items": Absent()}}, allow_different_len=False)

        # cpa-офферы попадают в sku->offers и при rgb=blue и при rgb=green
        response = self.report.request_json('place=sku_offers&market-sku=102&rgb=BLUE')
        self.assertFragmentIn(
            response,
            {"entity": "sku", "offers": {"items": [{'titles': {'raw': 'white_cpa_offer_with_msku'}}]}},
            allow_different_len=False,
        )

        response = self.report.request_json('place=sku_offers&market-sku=102&rgb=GREEN')
        self.assertFragmentIn(
            response,
            {"entity": "sku", "offers": {"items": [{'titles': {'raw': 'white_cpa_offer_with_msku'}}]}},
            allow_different_len=False,
        )

    @classmethod
    def prepare_jump_table_with_units(cls):
        cls.index.models += [
            Model(hyperid=3, hid=3),
            Model(hyperid=4, hid=4),
            Model(hyperid=6, hid=6),
        ]
        cls.index.gltypes += [
            GLType(
                param_id=301,
                hid=3,
                cluster_filter=True,
                gltype=GLType.ENUM,
                subtype='size',
                unit_param_id=311,
                model_filter_index=9,
                values=[
                    GLValue(value_id=1, text='1', unit_value_id=1),
                    GLValue(value_id=2, text='2', unit_value_id=1),
                    GLValue(value_id=11, text='S', unit_value_id=2, position=1),
                    GLValue(value_id=12, text='M', unit_value_id=2, position=2),
                    GLValue(value_id=13, text='L', unit_value_id=2, position=3),
                    GLValue(value_id=21, text='Rus1', unit_value_id=3, position=1),
                    GLValue(value_id=22, text='Rus2', unit_value_id=3, position=2),
                    GLValue(value_id=23, text='Rus3', unit_value_id=3, position=3),
                ],
            ),
            GLType(
                param_id=303,
                hid=4,
                cluster_filter=True,
                gltype=GLType.ENUM,
                subtype='size',
                unit_param_id=322,
                model_filter_index=10,
                values=[
                    GLValue(value_id=3, text='1', unit_value_id=4, position=1),
                    GLValue(value_id=4, text='2', unit_value_id=4, position=2),
                    GLValue(value_id=31, text='4', unit_value_id=5),
                    GLValue(value_id=32, text='4.5', unit_value_id=5),
                    GLValue(value_id=33, text='5', unit_value_id=5),
                    GLValue(value_id=41, text='Rus1', unit_value_id=6, position=1),
                    GLValue(value_id=42, text='Rus2', unit_value_id=6, position=2),
                    GLValue(value_id=43, text='Rus3', unit_value_id=6, position=3),
                ],
            ),
            GLType(
                param_id=304,
                hid=6,
                cluster_filter=True,
                gltype=GLType.ENUM,
                subtype='size',
                unit_param_id=333,
                model_filter_index=11,
                values=[
                    GLValue(value_id=5, text='1'),
                    GLValue(value_id=6, text='2'),
                    GLValue(value_id=51, text='S', unit_value_id=7),
                    GLValue(value_id=52, text='M', unit_value_id=7),
                    GLValue(value_id=53, text='L', unit_value_id=7),
                    GLValue(value_id=61, text='Rus1', unit_value_id=8, position=1),
                    GLValue(value_id=62, text='Rus2', unit_value_id=8, position=2),
                    GLValue(value_id=63, text='Rus3', unit_value_id=8, position=3),
                ],
            ),
            GLType(
                param_id=311,
                hid=3,
                position=None,
                gltype=GLType.ENUM,
                model_filter_index=12,
                values=[
                    GLValue(value_id=1, text='VENDOR'),
                    GLValue(value_id=2, text='EU', default=True),
                    GLValue(value_id=3, text='RU'),
                ],
            ),
            GLType(
                param_id=322,
                hid=4,
                position=None,
                gltype=GLType.ENUM,
                model_filter_index=13,
                values=[
                    GLValue(value_id=4, text='VENDOR1', default=True),
                    GLValue(value_id=5, text='US'),
                    GLValue(value_id=6, text='RU'),
                ],
            ),
            GLType(
                param_id=333,
                hid=6,
                position=None,
                gltype=GLType.ENUM,
                model_filter_index=14,
                values=[
                    GLValue(value_id=7, text='EU', default=True),
                    GLValue(value_id=8, text='RU'),
                ],
            ),
            GLType(
                param_id=302,
                hid=3,
                cluster_filter=True,
                gltype=GLType.ENUM,
                model_filter_index=15,
                values=[
                    GLValue(value_id=100, text='100'),
                    GLValue(value_id=200, text='200'),
                ],
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                hyperid=3,
                sku=31,
                blue_offers=[
                    BlueOffer(
                        price=5,
                        vat=Vat.VAT_10,
                        feedid=3,
                        offerid='blue.offer.31.1',
                        delivery_buckets=[2],
                    )
                ],
                descr=FULL_HTML_DESCRIPTION,
                glparams=[
                    GLParam(param_id=301, value=1),
                    GLParam(
                        param_id=301, value=11
                    ),  # Моделируем ситуацию когда вендорский размер 1 конвертируется в два размера S и M
                    GLParam(param_id=301, value=12),
                    GLParam(param_id=301, value=21),  # Аналогично в два русских размера
                    GLParam(param_id=301, value=22),
                    GLParam(param_id=302, value=100),
                ],
                original_glparams=[GLParam(param_id=301, value=1)],
            ),
            MarketSku(
                hyperid=3,
                sku=32,
                blue_offers=[
                    BlueOffer(
                        price=5,
                        vat=Vat.VAT_10,
                        feedid=3,
                        offerid='blue.offer.32.1',
                        delivery_buckets=[2],
                    )
                ],
                glparams=[
                    GLParam(param_id=301, value=2),  # Вендорский размер 2 конвертируется в M+L и Rus2+Rus3
                    GLParam(param_id=301, value=12),
                    GLParam(param_id=301, value=13),
                    GLParam(param_id=301, value=22),
                    GLParam(param_id=301, value=23),
                    GLParam(param_id=302, value=200),
                ],
                original_glparams=[GLParam(param_id=301, value=2)],
            ),
            MarketSku(
                hyperid=4,
                sku=41,
                blue_offers=[
                    BlueOffer(
                        price=5,
                        vat=Vat.VAT_10,
                        feedid=3,
                        offerid='blue.offer.41.1',
                        delivery_buckets=[2],
                    )
                ],
                descr=FULL_HTML_DESCRIPTION,
                glparams=[
                    GLParam(param_id=303, value=3),
                    GLParam(param_id=303, value=31),
                    GLParam(param_id=303, value=32),
                    GLParam(param_id=303, value=41),
                    GLParam(param_id=303, value=42),
                ],
                original_glparams=[GLParam(param_id=303, value=3)],
            ),
            MarketSku(
                hyperid=4,
                sku=42,
                blue_offers=[
                    BlueOffer(
                        price=5,
                        vat=Vat.VAT_10,
                        feedid=3,
                        offerid='blue.offer.42.1',
                        delivery_buckets=[2],
                    )
                ],
                glparams=[
                    GLParam(param_id=303, value=4),
                    GLParam(param_id=303, value=32),
                    GLParam(param_id=303, value=33),
                    GLParam(param_id=303, value=42),
                    GLParam(param_id=303, value=43),
                ],
                original_glparams=[GLParam(param_id=303, value=4)],
            ),
            MarketSku(
                hyperid=6,
                sku=51,
                blue_offers=[
                    BlueOffer(
                        price=5,
                        vat=Vat.VAT_10,
                        feedid=3,
                        offerid='blue.offer.51.1',
                        delivery_buckets=[2],
                    )
                ],
                descr=FULL_HTML_DESCRIPTION,
                glparams=[
                    GLParam(param_id=304, value=5),
                    GLParam(param_id=304, value=51),
                    GLParam(param_id=304, value=52),
                    GLParam(param_id=304, value=61),
                    GLParam(param_id=304, value=62),
                ],
                original_glparams=[GLParam(param_id=304, value=5)],
            ),
            MarketSku(
                hyperid=6,
                sku=52,
                blue_offers=[
                    BlueOffer(
                        price=5,
                        vat=Vat.VAT_10,
                        feedid=3,
                        offerid='blue.offer.52.1',
                        delivery_buckets=[2],
                    )
                ],
                glparams=[
                    GLParam(param_id=304, value=6),
                    GLParam(param_id=304, value=52),
                    GLParam(param_id=304, value=53),
                    GLParam(param_id=304, value=62),
                    GLParam(param_id=304, value=63),
                ],
                original_glparams=[GLParam(param_id=304, value=6)],
            ),
        ]

    def test_jump_table_with_units(self):
        """
        Что проверяем: при построении таблицы переходов для параметров с несколькими units
        таблица строится только по значениям, заданным оператором
        """
        for pipeline in [0, 1]:
            response = self.report.request_json(
                'place=sku_offers&market-sku=31&rids=213&rgb=BLUE&rearr-factors=use_new_jump_table_pipeline={}'.format(
                    pipeline
                )
            )
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "entity": "sku",
                            "id": "31",
                            "filters": [
                                {
                                    "id": "301",
                                    "subtype": Absent(),
                                    "values": [
                                        {
                                            "id": "1",
                                            "marketSku": "31",
                                            "fuzzy": Absent(),
                                            "checked": True,
                                        },
                                        {
                                            "id": "2",
                                            "marketSku": "32",
                                            "fuzzy": True,
                                            "checked": Absent(),
                                        },
                                    ],
                                },
                                {
                                    "id": "302",
                                    "values": [
                                        {
                                            "id": "100",
                                            "marketSku": "31",
                                            "fuzzy": Absent(),  # Собственное значение не может быть не точным
                                            "checked": True,
                                        },
                                        {
                                            "id": "200",
                                            "marketSku": "32",
                                            "fuzzy": True,
                                            "checked": Absent(),
                                        },
                                    ],
                                },
                            ],
                        }
                    ]
                },
                allow_different_len=False,
                preserve_order=True,
            )

    def test_jump_table_filter_has_originalsubtype(self):

        for pipeline in [0, 1]:
            response = self.report.request_json(
                'place=sku_offers&market-sku=31&rids=213&rgb=BLUE&rearr-factors=use_new_jump_table_pipeline={}'.format(
                    pipeline
                )
            )
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "entity": "sku",
                            "id": "31",
                            "filters": [
                                {
                                    "name": "GLPARAM-301",
                                    "originalSubType": "size",
                                    "subtype": Absent(),
                                }
                            ],
                        }
                    ]
                },
            )

    @classmethod
    def prepare_show_size_table_with_one_offer(cls):
        cls.index.models += [
            Model(hyperid=7, hid=7),
        ]
        cls.index.gltypes += [
            GLType(
                param_id=307,
                hid=7,
                cluster_filter=True,
                gltype=GLType.ENUM,
                subtype='size',
                unit_param_id=344,
                model_filter_index=16,
                values=[
                    GLValue(value_id=7, text='1', unit_value_id=9),
                    GLValue(value_id=8, text='2', unit_value_id=9),
                    GLValue(value_id=9, text='3', unit_value_id=9),
                    GLValue(value_id=10, text='4', unit_value_id=9),
                    GLValue(value_id=11, text='5', unit_value_id=9),
                ],
            ),
            GLType(
                param_id=344,
                hid=7,
                position=None,
                gltype=GLType.ENUM,
                model_filter_index=17,
                values=[
                    GLValue(value_id=9, text='VENDOR'),
                    GLValue(value_id=10, text='EU', default=True),
                ],
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                hyperid=7,
                sku=71,
                blue_offers=[],
                descr=FULL_HTML_DESCRIPTION,
                glparams=[
                    GLParam(param_id=307, value=7),
                    GLParam(param_id=307, value=8),
                ],
                original_glparams=[GLParam(param_id=307, value=7)],
            ),
            MarketSku(
                hyperid=7,
                sku=72,
                blue_offers=[],
                glparams=[
                    GLParam(param_id=307, value=8),
                    GLParam(param_id=307, value=9),
                ],
                original_glparams=[GLParam(param_id=307, value=8)],
            ),
            MarketSku(
                hyperid=7,
                sku=73,
                blue_offers=[],
                glparams=[
                    GLParam(param_id=307, value=9),
                    GLParam(param_id=307, value=10),
                ],
                original_glparams=[GLParam(param_id=307, value=9)],
            ),
            MarketSku(
                hyperid=7,
                sku=74,
                blue_offers=[],
                glparams=[
                    GLParam(param_id=307, value=10),
                    GLParam(param_id=307, value=11),
                ],
                original_glparams=[GLParam(param_id=307, value=10)],
            ),
            MarketSku(
                hyperid=7,
                sku=75,
                blue_offers=[
                    BlueOffer(
                        price=5,
                        vat=Vat.VAT_10,
                        feedid=3,
                        offerid='blue.offer.71.1',
                        delivery_buckets=[2],
                    )
                ],
                glparams=[
                    GLParam(param_id=307, value=11),
                ],
                original_glparams=[GLParam(param_id=307, value=11)],
            ),
        ]

    @classmethod
    def prepare_vendor_sku(cls):
        # Для проверки создаём sku с вендором,
        # но не привязываем офферы (в соответствии с тикетом)

        cls.index.vendors += [
            Vendor(vendor_id=100500, name="CIA", website="https://www.cia.gov/index.html"),
        ]

        # выставляем отрицательный индекс для
        # фильтра вендора

        cls.index.gltypes += [
            GLType(
                model_filter_index=-1,
                param_id=201,
                hid=11,
                gltype=GLType.ENUM,
                values=[
                    100500,
                ],
                unit_name="Производитель",
                cluster_filter=True,
                vendor=True,
            ),
        ]

        cls.index.models += [
            Model(
                hyperid=109,
                hid=11,
                title="Grabli3",
                vendor_id=100500,
                glparams=[
                    GLParam(param_id=201, value=100500),
                ],
            )
        ]

        cls.index.mskus += [
            MarketSku(
                title="SomeGrabliSku",
                hyperid=109,
                sku=1111,
                glparams=[
                    GLParam(param_id=201, value=100500),
                ],
                randx=1,
            ),
        ]

    def test_vendor_sku(self):
        # в соответствии MARKETOUT-19649
        # Для sku должен быть указан vendor,
        # даже если индес этого фильтра отрицательный

        response = self.report.request_json("place=sku_offers&market-sku=1111")
        self.assertFragmentIn(response, [{"entity": "sku", "vendor": {"filter": "201:100500"}}])

    @classmethod
    def prepare_sku_adult(cls):
        cls.index.models += [
            Model(
                hyperid=120,
                hid=ADULT_CATEG_ID,
                title="Adult",
            )
        ]

        cls.index.mskus += [
            MarketSku(
                title="AdultSku",
                hyperid=120,
                sku=1200,
                blue_offers=[
                    BlueOffer(
                        price=5,
                        vat=Vat.VAT_10,
                        feedid=3,
                        offerid='adult_sku',
                        delivery_buckets=[2],
                    )
                ],
                adult=True,
            ),
            MarketSku(
                title="NotAdultSku",
                hyperid=121,
                sku=1201,
                blue_offers=[
                    BlueOffer(
                        price=5,
                        vat=Vat.VAT_10,
                        feedid=3,
                        offerid='not_adult_sku',
                        delivery_buckets=[2],
                    )
                ],
                adult=False,
            ),
        ]

    def test_sku_adult(self):
        '''
        Проверяем скрытие взрослой карточки МСКУ
        '''
        for pipeline in [0, 1]:
            flag = ';market_use_adult_filter_for_msku=1'
            request = "place=sku_offers&rgb=blue&market-sku=1200&market-sku=1201&adult={adult}&rearr-factors=use_new_jump_table_pipeline={pipeline}"

            sample_offer_adult = {
                "entity": "offer",
                "supplierSku": "adult_sku",
                "isAdult": True,
                "restrictedAge18": True,
            }

            sample_offer_not_adult = {
                "entity": "offer",
                "supplierSku": "not_adult_sku",
                "isAdult": False,
                "restrictedAge18": False,
            }

            def sample_msku(msku, offers, adult):
                return {
                    "entity": "sku",
                    "id": str(msku),
                    "offers": {"items": offers},
                    "isAdult": adult,
                    "restrictedAge18": adult,
                }

            # Без эксперимента скрытия МСКУ нет
            # Показ взрослого контента разрешен. Видим синий офер у МСКУ
            response = self.report.request_json(request.format(adult=1, pipeline=pipeline))
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        sample_msku(msku=1200, offers=[sample_offer_adult], adult=True),
                        sample_msku(msku=1201, offers=[sample_offer_not_adult], adult=False),
                    ]
                },
                allow_different_len=False,
            )

            # Текущее поведение говорит, что показывать офер в sku_offers надо всегда
            response = self.report.request_json(request.format(adult=0, pipeline=pipeline))
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        sample_msku(msku=1200, offers=[sample_offer_adult], adult=True),
                        sample_msku(msku=1201, offers=[sample_offer_not_adult], adult=False),
                    ]
                },
                allow_different_len=False,
            )

            # С экспериментом скрываются и офер и МСКУ
            # Показ взрослого контента разрешен. Видим синий офер у МСКУ
            response = self.report.request_json(request.format(adult=1, pipeline=pipeline) + flag)
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        sample_msku(msku=1200, offers=[sample_offer_adult], adult=True),
                        sample_msku(msku=1201, offers=[sample_offer_not_adult], adult=False),
                    ]
                },
                allow_different_len=False,
            )

            # Показ взрослого контента запрещен
            response = self.report.request_json(request.format(adult=0, pipeline=pipeline) + flag)
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        # Нет МСКУ с пометкой "взрослый"
                        sample_msku(msku=1201, offers=[sample_offer_not_adult], adult=False)
                    ]
                },
                allow_different_len=False,
            )

    # Оферы для проверки фильтрации доставки до выбора байбокса
    skuDelivery_offer1 = BlueOffer(
        price=8,
        price_old=8,
        vat=Vat.VAT_10,
        feedid=4,
        offerid='blue.offer.Del.1',
        waremd5='SkuDelPrice8_________g',
        delivery_buckets=[3, 4],
    )
    skuDelivery_offer2 = BlueOffer(
        price=9,
        price_old=8,
        vat=Vat.VAT_10,
        feedid=5,
        offerid='blue.offer.Del.2',
        waremd5='SkuDelPrice9_________g',
        delivery_buckets=[4],
    )

    @classmethod
    def prepare_preffered_and_delivery(cls):
        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=3,
                fesh=1,
                carriers=[48],
                regional_options=[
                    RegionalDelivery(rid=3, options=[DeliveryOption(price=5, day_from=1, day_to=2)]),
                ],
            ),
            DeliveryBucket(
                bucket_id=4,
                fesh=1,
                carriers=[48],
                regional_options=[
                    RegionalDelivery(rid=4, options=[DeliveryOption(price=5, day_from=1, day_to=2)]),
                ],
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                title="BuyboxDeliveryTest",
                hyperid=110,
                sku=116,
                blue_offers=[T.skuDelivery_offer1, T.skuDelivery_offer2],
            ),
        ]

    def test_preffered_and_delivery(self):
        '''
        Проверяем, что офер отбрасывается по доставке перед тем, как будет выбран байбокс.
        Даже если он был приоритетным
        '''

        for pipeline in [0, 1]:
            request = 'place=sku_offers&market-sku=116&rids={rid}&offerid={waremd5}&regset=2&rearr-factors=use_new_jump_table_pipeline={pipeline};market_nordstream=0'
            rids = [3, 4, 4]
            waremds = [T.skuDelivery_offer1.waremd5, T.skuDelivery_offer1.waremd5, T.skuDelivery_offer2.waremd5]
            # Офер приоритетен и доставляется в регион
            for rid, waremd5 in zip(rids, waremds):
                response = self.report.request_json(request.format(rid=rid, waremd5=waremd5, pipeline=pipeline))
                self.assertFragmentIn(
                    response,
                    {"results": [{"entity": "sku", "offers": {"items": [{"wareId": waremd5}]}}]},
                    allow_different_len=False,
                )

            # Офер приоритетен, но не доставляется. Поэтому был выбран другой офер
            response = self.report.request_json(
                request.format(rid=3, waremd5=T.skuDelivery_offer2.waremd5, pipeline=pipeline)
            )
            self.assertFragmentIn(
                response,
                {"results": [{"entity": "sku", "offers": {"items": [{"wareId": T.skuDelivery_offer1.waremd5}]}}]},
                allow_different_len=False,
            )

    @classmethod
    def prepare_hidden_warehouses(cls):
        cls.index.mskus += [
            MarketSku(
                title="HiddenWhMsku",
                hyperid=197,
                sku=197,
                price=5,
                vat=Vat.VAT_10,
                blue_offers=[
                    BlueOffer(feedid=4, waremd5='OfferWarehouse147____g', delivery_buckets=[2]),
                    BlueOffer(feedid=5, waremd5='OfferWarehouse145____g', delivery_buckets=[2]),
                    BlueOffer(feedid=11, waremd5='OfferWarehouse303____g', delivery_buckets=[11]),
                ],
            )
        ]
        cls.index.hidden_warehouses += [303, 304]

    def test_hidden_warehouses(self):
        '''
        Проверяем работу конфига hidden-warehouses.json и флага show_hidden_warehouses, данный конфиг отменяющий
        '''

        def check_offer_in_response(request, flag, offer):
            self.assertFragmentIn(
                self.report.request_json(request + flag),
                {"offers": {"items": [{"entity": "offer", "wareId": offer}]}},
                allow_different_len=False,
            )

        # Тут мы ожидаем в ответе все офферы кроме 303го со скрытого склада
        for pipeline in [0, 1]:
            request = "place=sku_offers&rgb=blue&market-sku=197&offerid="
            flag = "&rearr-factors=use_new_jump_table_pipeline={}".format(pipeline)
            check_offer_in_response(request + 'OfferWarehouse145____g', flag, offer='OfferWarehouse145____g')
            check_offer_in_response(request + 'OfferWarehouse147____g', flag, offer='OfferWarehouse147____g')

            # оффер 'OfferWarehouse303____g' скрыт по складу
            check_offer_in_response(request + 'OfferWarehouse303____g', flag, offer='OfferWarehouse145____g')

            # А теперь проверим, что даже скрытый через конфиг склад будет возвращен, когда мы пользуемся флагом show_hidden_warehouses
            flag = "&rearr-factors=show_hidden_warehouses=1;use_new_jump_table_pipeline={}".format(pipeline)
            check_offer_in_response(request + 'OfferWarehouse303____g', flag, offer='OfferWarehouse303____g')

    def test_filter_by_supplier_id(self):
        for pipeline in [0, 1]:
            # Проверяем фильтрацию по идентификатору магазина (supplier_id для синего)
            # Обычный запрос без фильтра вернёт оффер с ценой ниже
            request = "place=sku_offers&rgb=blue&market-sku={msku}&rids=77&rearr-factors=use_new_jump_table_pipeline={pipeline}".format(
                msku=77, pipeline=pipeline
            )
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {"results": [{"entity": "sku", "offers": {"items": [{"wareId": T.sku77_offer1.waremd5}]}}]},
                allow_different_len=False,
            )

            # Фильтрация по этому же магазину ничего не меняет
            request = "place=sku_offers&rgb=blue&market-sku={msku}&rids=77&supplier-id={shop}&rearr-factors=use_new_jump_table_pipeline={pipeline}".format(
                msku=77,
                shop=7,
                pipeline=pipeline,
            )
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {"results": [{"entity": "sku", "offers": {"items": [{"wareId": T.sku77_offer1.waremd5}]}}]},
                allow_different_len=False,
            )

            # Фильтрация по списку магазинов, где есть нужный магаз - ничего не меняет
            request = "place=sku_offers&rgb=blue&market-sku={msku}&rids=77&supplier-id={shop}&rearr-factors=use_new_jump_table_pipeline={pipeline}".format(
                msku=77,
                shop='8,9,7',
                pipeline=pipeline,
            )
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {"results": [{"entity": "sku", "offers": {"items": [{"wareId": T.sku77_offer1.waremd5}]}}]},
                allow_different_len=False,
            )

            # Фильтрация по списку магазинов, где есть нужный магаз - ничего не меняет (задано несколько раз supplier-id=X)
            request = "place=sku_offers&rgb=blue&market-sku={msku}&rids=77&supplier-id={shop1}&supplier-id={shop2}&rearr-factors=use_new_jump_table_pipeline={pipeline}".format(
                msku=77,
                shop1=22,
                shop2=7,
                pipeline=pipeline,
            )
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {"results": [{"entity": "sku", "offers": {"items": [{"wareId": T.sku77_offer1.waremd5}]}}]},
                allow_different_len=False,
            )

            # Если в фильтре указать другой магазин, то лучший оффер отфильтруется и в результирующий buybox попадёт оффер с ценой выше
            request = "place=sku_offers&rgb=blue&market-sku={msku}&rids=77&supplier-id={shop}&rearr-factors=use_new_jump_table_pipeline={pipeline}".format(
                msku=77,
                shop=8,
                pipeline=pipeline,
            )
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {"results": [{"entity": "sku", "offers": {"items": [{"wareId": T.sku77_offer2.waremd5}]}}]},
                allow_different_len=False,
            )

            # Если в фильтре указать список других магазинов, то лучший оффер отфильтруется и в результирующий buybox попадёт оффер с ценой выше
            request = "place=sku_offers&rgb=blue&market-sku={msku}&rids=77&supplier-id={shop}&rearr-factors=use_new_jump_table_pipeline={pipeline}".format(
                msku=77,
                shop='100,200,8',
                pipeline=pipeline,
            )
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {"results": [{"entity": "sku", "offers": {"items": [{"wareId": T.sku77_offer2.waremd5}]}}]},
                allow_different_len=False,
            )

    def test_filter_by_fesh(self):
        for pipeline in [0, 1]:
            # Проверяем фильтрацию по fesh
            # Обычный запрос без фильтра вернёт оффер с ценой ниже
            request = "place=sku_offers&rgb=blue&market-sku={msku}&rids=77&rearr-factors=use_new_jump_table_pipeline={pipeline}".format(
                msku=77, pipeline=pipeline
            )
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {"results": [{"entity": "sku", "offers": {"items": [{"wareId": T.sku77_offer1.waremd5}]}}]},
                allow_different_len=False,
            )

            # Фильтрация по этому же магазину ничего не меняет
            request = "place=sku_offers&rgb=blue&market-sku={msku}&rids=77&fesh={shop}&rearr-factors=use_new_jump_table_pipeline={pipeline}".format(
                msku=77,
                shop=7,
                pipeline=pipeline,
            )
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {"results": [{"entity": "sku", "offers": {"items": [{"wareId": T.sku77_offer1.waremd5}]}}]},
                allow_different_len=False,
            )

            # Фильтрация по списку магазинов, где есть нужный магаз - ничего не меняет
            request = "place=sku_offers&rgb=blue&market-sku={msku}&rids=77&fesh={shop}&rearr-factors=use_new_jump_table_pipeline={pipeline}".format(
                msku=77,
                shop='8,9,7',
                pipeline=pipeline,
            )
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {"results": [{"entity": "sku", "offers": {"items": [{"wareId": T.sku77_offer1.waremd5}]}}]},
                allow_different_len=False,
            )

            # Фильтрация по списку магазинов, где есть нужный магаз - ничего не меняет (задано несколько раз fesh=X)
            request = "place=sku_offers&rgb=blue&market-sku={msku}&rids=77&fesh={shop1}&fesh={shop2}&rearr-factors=use_new_jump_table_pipeline={pipeline}".format(
                msku=77,
                shop1=22,
                shop2=7,
                pipeline=pipeline,
            )
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {"results": [{"entity": "sku", "offers": {"items": [{"wareId": T.sku77_offer1.waremd5}]}}]},
                allow_different_len=False,
            )

    @classmethod
    def prepare_sort_values_by_position_in_top(cls):
        cls.index.gltypes += [
            GLType(
                param_id=2301,
                hid=11,
                cluster_filter=True,
                gltype=GLType.ENUM,
                model_filter_index=18,
                values=[
                    GLValue(1301, short_enum_position=3, position=2, text="A"),
                    GLValue(1302, short_enum_position=5, position=1, text="C"),
                    GLValue(1303, text="D"),
                    GLValue(1304, short_enum_position=1, text="B"),
                    GLValue(1305, position=3, text="E"),
                ],
            )
        ]

        def create_msku(model_id, msku_id, value_id):
            return MarketSku(
                hyperid=model_id,
                sku=msku_id,
                hid=11,
                blue_offers=[BlueOffer(price=5, vat=Vat.VAT_10, feedid=3)],
                glparams=[GLParam(param_id=2301, value=value_id)],
            )

        # В этой модели собраны МСКУ со всеми значениями параметра
        cls.index.mskus += [
            create_msku(301, 311, 1301),
            create_msku(301, 312, 1302),
            create_msku(301, 313, 1303),
            create_msku(301, 314, 1304),
            create_msku(301, 315, 1305),
        ]

        # В этой модели собраны МСКУ только с полными позициями
        cls.index.mskus += [
            create_msku(302, 321, 1301),
            create_msku(302, 322, 1302),
            create_msku(302, 323, 1305),
        ]

        # В этой модели собраны МСКУ только с короткими позициями
        cls.index.mskus += [
            create_msku(303, 331, 1301),
            create_msku(303, 332, 1302),
            create_msku(303, 333, 1304),
        ]

        # В этой модели собраны МСКУ и с короткими и с полными позициями
        cls.index.mskus += [
            create_msku(304, 341, 1301),
            create_msku(304, 342, 1302),
        ]

    def test_sort_values_by_top(self):
        '''
        Проверяем сортировку по короткому списку
        '''
        for pipeline in [0, 1]:
            response = self.report.request_json(
                "place=sku_offers&market-sku=331&rearr-factors=use_new_jump_table_pipeline={}".format(pipeline)
            )
            self.assertFragmentIn(
                response,
                {
                    "entity": "sku",
                    "id": "331",
                    "filters": [
                        {
                            "id": "2301",
                            "values": [
                                # Порядок короткого списка
                                {"id": "1304", "value": "B"},
                                {"id": "1301", "value": "A"},
                                {"id": "1302", "value": "C"},
                            ],
                        }
                    ],
                },
                allow_different_len=False,
                preserve_order=True,
            )

    def test_sort_values_by_position(self):
        '''
        Проверяем сортировку по позициям
        '''
        for pipeline in [0, 1]:
            response = self.report.request_json(
                "place=sku_offers&market-sku=321&rearr-factors=use_new_jump_table_pipeline={}".format(pipeline)
            )
            self.assertFragmentIn(
                response,
                {
                    "entity": "sku",
                    "id": "321",
                    "filters": [
                        {
                            "id": "2301",
                            "values": [
                                # Порядок, заданный position
                                {"id": "1302", "value": "C"},
                                {"id": "1301", "value": "A"},
                                {"id": "1305", "value": "E"},
                            ],
                        }
                    ],
                },
                allow_different_len=False,
                preserve_order=True,
            )

    def test_sort_values_by_top_and_position(self):
        '''
        Проверяем сортировку по позициям полным и короткого списка
        '''
        for pipeline in [0, 1]:
            response = self.report.request_json(
                "place=sku_offers&market-sku=341&rearr-factors=use_new_jump_table_pipeline={}".format(pipeline)
            )
            self.assertFragmentIn(
                response,
                {
                    "entity": "sku",
                    "id": "341",
                    "filters": [
                        {
                            "id": "2301",
                            "values": [
                                # Порядок, заданный position, т.к. он имеет приоритет над топовым списком
                                {"id": "1302", "value": "C"},
                                {"id": "1301", "value": "A"},
                            ],
                        }
                    ],
                },
                allow_different_len=False,
                preserve_order=True,
            )

    def test_sort_values_without_position(self):
        '''
        Сортировка по отображаемому имени, т.к. не у всех значений есть позиция
        '''
        for pipeline in [0, 1]:
            response = self.report.request_json(
                "place=sku_offers&market-sku=311&rgb=blue&rearr-factors=use_new_jump_table_pipeline={}".format(pipeline)
            )
            self.assertFragmentIn(
                response,
                {
                    "entity": "sku",
                    "id": "311",
                    "filters": [
                        {
                            "id": "2301",
                            "values": [
                                {"id": "1301", "value": "A"},
                                {"id": "1304", "value": "B"},
                                {"id": "1302", "value": "C"},
                                {"id": "1303", "value": "D"},
                                {"id": "1305", "value": "E"},
                            ],
                        }
                    ],
                },
                allow_different_len=False,
                preserve_order=True,
            )

    def test_jump_table_single_only(self):
        '''
        Проверяем, что карта переходов создается только для запроса с одним СКУ
        '''
        for pipeline in [0, 1]:
            response = self.report.request_json(
                "place=sku_offers&market-sku=1&rgb=blue&rearr-factors=use_new_jump_table_pipeline={}".format(pipeline)
            )
            self.assertFragmentIn(response, {"entity": "sku", "id": "1", "filters": NotEmpty()})

    def test_jump_table_multi_sku(self):
        '''
        Проверяем, что карта переходов не создается, если было запрошено несколько СКУ (это не карточка товара)
        '''
        for pipeline in [0, 1]:
            response = self.report.request_json(
                "place=sku_offers&market-sku=1,2&rgb=blue&rearr-factors=use_new_jump_table_pipeline={}".format(pipeline)
            )
            self.assertFragmentIn(response, {"entity": "sku", "id": "1", "filters": Absent()})

            response = self.report.request_json(
                "place=sku_offers&market-sku=1&market-sku=2&rgb=blue&rearr-factors=use_new_jump_table_pipeline={}".format(
                    pipeline
                )
            )
            self.assertFragmentIn(response, {"entity": "sku", "id": "1", "filters": Absent()})

            # Если передан флаг rearr-factors=market_blue_sku_offers_always_create_jump_table=1, карта переходов создается
            response = self.report.request_json(
                "place=sku_offers&market-sku=1&market-sku=2&rgb=blue&rearr-factors=market_blue_sku_offers_always_create_jump_table=1;use_new_jump_table_pipeline={}".format(
                    pipeline
                )
            )
            self.assertFragmentIn(response, {"entity": "sku", "id": "1", "filters": NotEmpty()})

    def test_jump_table_checkout(self):
        '''
        Проверяем, что карта переходов не создается для чекаутера
        '''
        for pipeline in [0, 1]:
            request = (
                "place=sku_offers&market-sku=1&rgb=blue&rearr-factors=use_new_jump_table_pipeline={}&client=".format(
                    pipeline
                )
            )
            response = self.report.request_json(request + "checkout")
            self.assertFragmentIn(response, {"entity": "sku", "id": "1", "filters": Absent()})

            # Для основных клиентов создается карта переходов
            for client in ["frontend", "partnerinterface"]:
                response = self.report.request_json(request + client)
                self.assertFragmentIn(response, {"entity": "sku", "id": "1", "filters": NotEmpty()})

            # Под флагом карта переходов генерируется даже для чекаутера
            response = self.report.request_json(
                request
                + "checkout"
                + "&rearr-factors=market_blue_sku_offers_always_create_jump_table=1;use_new_jump_table_pipeline={}".format(
                    pipeline
                )
            )
            self.assertFragmentIn(response, {"entity": "sku", "id": "1", "filters": NotEmpty()})

    def test_multiple_buyboxes_request(self):
        '''
        Проверяем, что запрос двух офферов для одного MSKU приводит к ошибке репорта
        (каждому MSKU в запросе должен соответствовать один оффер)
        '''
        request = "place=sku_offers&market-sku=1&rgb=blue&offerid={}&offerid={}".format(
            T.sku1_offer1.waremd5,
            T.sku1_offer2.waremd5,
        )
        self.report.request_json(request)
        self.error_log.expect(
            code=3624,
            message="More than one offer requested for MSKU '1', offer1 = Sku1Price5-IiLVm1Goleg, offer2 = Sku1Price7-IiLVm1Goleg",
        ).once()

    @classmethod
    def prepare_hypothesis(cls):
        cls.index.gltypes += [
            GLType(
                param_id=322,
                hid=12,
                cluster_filter=True,
                gltype=GLType.ENUM,
                model_filter_index=18,
                values=[
                    3221,
                    3222,
                ],
            )
        ]

        cls.index.models += [
            Model(
                hyperid=5566,
                hid=12,
                title="HypothesisModel",
                vendor_id=1005001,
                glparams=[
                    GLParam(param_id=322, value=3221, is_filter=True),
                    GLParam(param_id=322, value=3222, is_filter=True),
                    GLParam(param_id=322, string_value='HYPOTHESIS_3223', is_filter=False, is_hypothesis=True),
                ],
            )
        ]

        cls.index.mskus += [
            MarketSku(
                hyperid=5566,
                sku=322221,
                hid=12,
                blue_offers=[BlueOffer(price=5, vat=Vat.VAT_10, feedid=3)],
                glparams=[GLParam(param_id=322, value=3221, is_filter=True)],
            ),
            MarketSku(
                hyperid=5566,
                sku=322222,
                hid=12,
                blue_offers=[BlueOffer(price=5, vat=Vat.VAT_10, feedid=3)],
                glparams=[GLParam(param_id=322, value=3222, is_filter=True)],
            ),
            MarketSku(
                hyperid=5566,
                sku=322223,
                hid=12,
                blue_offers=[BlueOffer(price=5, vat=Vat.VAT_10, feedid=3)],
                glparams=[GLParam(param_id=322, string_value='HYPOTHESIS_3223', is_filter=False, is_hypothesis=True)],
            ),
        ]

    def test_hypothesis_new_pipeline(self):
        '''
        Проверяем, что в таблице переходов корректно отображается значение гипотезы
        '''
        request = "place=sku_offers&market-sku=322221&rgb=blue&rearr-factors=use_new_jump_table_pipeline=1"
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "entity": "sku",
                "id": "322221",
                "filters": [
                    {
                        "values": [
                            {
                                "id": "SFlQT1RIRVNJU18zMjIz",
                                "marketSku": "322223",
                                "value": "HYPOTHESIS_3223",
                                "found": 1,
                                "fuzzy": Absent(),
                            },
                            {
                                "id": "3221",
                                "marketSku": "322221",
                                "value": "VALUE-3221",
                                "found": 1,
                                "checked": True,
                                "fuzzy": Absent(),
                            },
                            {
                                "id": "3222",
                                "marketSku": "322222",
                                "value": "VALUE-3222",
                                "found": 1,
                                "fuzzy": Absent(),
                            },
                        ],
                        "valuesCount": 3,
                        "subType": "",
                        "xslname": "GLPARAM322",
                        "name": "GLPARAM-322",
                        "type": "enum",
                        "id": "322",
                    }
                ],
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_a_lot_of_values(cls):
        total_skus_num = 50

        cls.index.gltypes += [
            GLType(
                param_id=323,
                hid=13,
                cluster_filter=True,
                gltype=GLType.ENUM,
                model_filter_index=19,
                values=[4321 + i for i in range(total_skus_num)],
            )
        ]

        cls.index.models += [
            Model(
                hyperid=5567,
                hid=13,
                title="ManyValueModel",
                vendor_id=1006001,
                glparams=[GLParam(param_id=323, value=(4321 + i), is_filter=True) for i in range(total_skus_num)],
            )
        ]

        cls.index.mskus += [
            MarketSku(
                hyperid=5567,
                sku=(422221 + i),
                hid=13,
                blue_offers=[BlueOffer(price=5, vat=Vat.VAT_10, feedid=3)],
                glparams=[GLParam(param_id=323, value=(4321 + i), is_filter=True)],
            )
            for i in range(total_skus_num)
        ]

    def test_many_values_new_pipeline(self):
        '''
        Проверяем, что все 50 параметров попадут в таблицу переходов
        '''
        request = "place=sku_offers&market-sku=422221&rgb=blue&rearr-factors=use_new_jump_table_pipeline=1"
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "entity": "sku",
                            "id": "422221",
                            "filters": [
                                {
                                    "valuesCount": 50,
                                    "subType": "",
                                    "xslname": "GLPARAM323",
                                    "name": "GLPARAM-323",
                                    "type": "enum",
                                    "id": "323",
                                }
                            ],
                        }
                    ],
                }
            },
        )

    @classmethod
    def prepare_show_on_check_new_pipeline(cls):
        total_skus_num = 3

        cls.index.gltypes += [
            GLType(
                param_id=324,
                hid=14,
                cluster_filter=False,
                gltype=GLType.ENUM,
                model_filter_index=20,
                values=[5321 + i for i in range(total_skus_num)],
            ),
            GLType(
                param_id=3245,
                hid=14,
                cluster_filter=True,
                gltype=GLType.ENUM,
                model_filter_index=200,
                values=[53210 + i for i in range(total_skus_num)],
            ),
        ]

        cls.index.models += [
            Model(
                hyperid=5568,
                hid=14,
                title="ShowOnCheck",
                vendor_id=1007001,
                glparams=list(
                    chain(
                        [GLParam(param_id=324, value=(5321 + i), is_filter=True) for i in range(total_skus_num)],
                        [GLParam(param_id=3245, value=(53210 + i), is_filter=True) for i in range(total_skus_num)],
                    )
                ),
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                hyperid=5568,
                sku=(522221 + i),
                hid=14,
                blue_offers=[BlueOffer(price=5, vat=Vat.VAT_10, feedid=3)],
                glparams=[
                    GLParam(param_id=324, value=(5321 + i), is_filter=True),  # here show_on = 0
                    GLParam(param_id=3245, value=(53210 + i), is_filter=True),  # here show_on = 2
                ],
            )
            for i in range(total_skus_num)
        ]

    def test_show_on_filter_new_pipeline(self):
        '''
        Проверяем, что таблица переходов не содержит параметров с show_on != 2 (значение для jump_table).
        В данном случае таблицы перехода вообще нет, так как параметр всего один.
        '''
        request = "place=sku_offers&market-sku=522223&rgb=blue&rearr-factors=use_new_jump_table_pipeline=1"
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "offers": {
                                "items": [
                                    {
                                        "filters": [
                                            {
                                                "valuesCount": 3,
                                            }
                                        ],
                                    }
                                ],
                            },
                        }
                    ],
                }
            },
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "entity": "sku",
                            "id": "522223",
                            "filters": [
                                {
                                    "values": [
                                        {
                                            "id": "53210",
                                            "marketSku": "522221",
                                            "value": "VALUE-53210",
                                        },
                                        {
                                            "id": "53211",
                                            "marketSku": "522222",
                                            "value": "VALUE-53211",
                                        },
                                        {
                                            "id": "53212",
                                            "marketSku": "522223",
                                            "value": "VALUE-53212",
                                            "checked": True,
                                        },
                                    ],
                                    "valuesCount": 3,
                                    "xslname": "GLPARAM3245",
                                    "name": "GLPARAM-3245",
                                    "type": "enum",
                                    "id": "3245",
                                }
                            ],
                        }
                    ],
                }
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_modifications_new_pipeline(cls):
        total_skus_num = 3

        cls.index.gltypes += [
            GLType(
                param_id=325,
                hid=15,
                cluster_filter=True,
                gltype=GLType.ENUM,
                model_filter_index=21,
                values=[6421 + i for i in range(total_skus_num)],
            ),
            GLType(
                param_id=326,
                hid=16,
                cluster_filter=True,
                gltype=GLType.ENUM,
                model_filter_index=22,
                values=[
                    7421,
                ],
            ),
            GLType(
                param_id=327,
                hid=18,
                cluster_filter=True,
                gltype=GLType.ENUM,
                model_filter_index=23,
                values=[8421 + i for i in range(total_skus_num)],
            ),
            GLType(
                param_id=328,
                hid=19,
                cluster_filter=True,
                gltype=GLType.ENUM,
                model_filter_index=24,
                values=[
                    9421,
                ],
            ),
            GLType(
                param_id=329,
                hid=20,
                cluster_filter=True,
                gltype=GLType.ENUM,
                model_filter_index=25,
                values=[
                    10421,
                    10422,
                ],
            ),
        ]

        cls.index.models += [
            Model(
                hyperid=5569,
                hid=15,
                title="ModificationModel",
                vendor_id=1009001,
                glparams=[GLParam(param_id=325, value=(6421 + i), is_filter=True) for i in range(total_skus_num)],
            ),
            Model(
                hyperid=5570,
                hid=16,
                title="ModificationNoJumpTableModel",
                vendor_id=2000002,
                glparams=[GLParam(param_id=326, value=(7421), is_filter=True)],
            ),
            Model(hyperid=5571, hid=17, title="ModificationNoJumpTableModelNoParams", vendor_id=2001002, glparams=[]),
            Model(
                hyperid=5572,
                hid=18,
                title="ModificationMixedCase",
                vendor_id=2002002,
                glparams=[
                    GLParam(param_id=327, value=(8421), is_filter=True),
                    GLParam(param_id=327, value=(8422), is_filter=True),
                ],
            ),
            Model(
                hyperid=5573,
                hid=19,
                title="ModificationMixedTrivialCase",
                vendor_id=2003002,
                glparams=[GLParam(param_id=328, value=(9421), is_filter=True)],
            ),
        ]
        # Some pskus have the same set of params, the will be showed as modifications
        cls.index.mskus += [
            MarketSku(
                title="psku 1 1",
                hyperid=5569,
                sku=622221,
                hid=15,
                blue_offers=[BlueOffer(price=5, vat=Vat.VAT_10, feedid=3)],
                glparams=[GLParam(param_id=325, value=(6421), is_filter=True)],
            ),
            MarketSku(
                title="psku 1 2",
                hyperid=5569,
                sku=622222,
                hid=15,
                blue_offers=[BlueOffer(price=5, vat=Vat.VAT_10, feedid=3)],
                glparams=[GLParam(param_id=325, value=(6421), is_filter=True)],
            ),
            MarketSku(
                title="psku 2 1",
                hyperid=5569,
                sku=622223,
                hid=15,
                blue_offers=[BlueOffer(price=5, vat=Vat.VAT_10, feedid=3)],
                glparams=[GLParam(param_id=325, value=(6422), is_filter=True)],
            ),
            MarketSku(
                title="psku 3 1",
                hyperid=5569,
                sku=622224,
                hid=15,
                blue_offers=[BlueOffer(price=5, vat=Vat.VAT_10, feedid=3)],
                glparams=[GLParam(param_id=325, value=(6423), is_filter=True)],
            ),
        ]
        # Pskus with same params (only modifications and empty jump_table)
        cls.index.mskus += [
            MarketSku(
                title="psku_same_params_1",
                hyperid=5570,
                sku=622225,
                hid=16,
                blue_offers=[BlueOffer(price=5, vat=Vat.VAT_10, feedid=3)],
                glparams=[GLParam(param_id=326, value=(7421), is_filter=True)],
            ),
            MarketSku(
                title="psku_same_params_2",
                hyperid=5570,
                sku=622226,
                hid=16,
                blue_offers=[BlueOffer(price=5, vat=Vat.VAT_10, feedid=3)],
                glparams=[GLParam(param_id=326, value=(7421), is_filter=True)],
            ),
        ]
        # Pskus without params (only modifications and empty jump_table)
        cls.index.mskus += [
            MarketSku(
                title="psku_no_params_1",
                hyperid=5571,
                sku=622227,
                hid=17,
                blue_offers=[BlueOffer(price=5, vat=Vat.VAT_10, feedid=3)],
                glparams=[],
            ),
            MarketSku(
                title="psku_no_params_2",
                hyperid=5571,
                sku=622228,
                hid=17,
                blue_offers=[BlueOffer(price=5, vat=Vat.VAT_10, feedid=3)],
                glparams=[],
            ),
        ]
        # Two mskus has valid params values and two other doesn't.
        # Create fake value for parameter called Other
        cls.index.mskus += [
            MarketSku(
                title="psku_no_param_1",
                hyperid=5572,
                sku=622229,
                hid=18,
                blue_offers=[BlueOffer(price=5, vat=Vat.VAT_10, feedid=3)],
                glparams=[],
            ),
            MarketSku(
                title="psku_no_param_2",
                hyperid=5572,
                sku=622230,
                hid=18,
                blue_offers=[BlueOffer(price=5, vat=Vat.VAT_10, feedid=3)],
                glparams=[],
            ),
            MarketSku(
                title="psku_with_param_1",
                hyperid=5572,
                sku=622231,
                hid=18,
                blue_offers=[BlueOffer(price=5, vat=Vat.VAT_10, feedid=3)],
                glparams=[GLParam(param_id=327, value=(8421), is_filter=True)],
            ),
            MarketSku(
                title="psku_with_param_2",
                hyperid=5572,
                sku=622232,
                hid=18,
                blue_offers=[BlueOffer(price=5, vat=Vat.VAT_10, feedid=3)],
                glparams=[GLParam(param_id=327, value=(8422), is_filter=True)],
            ),
        ]
        # One psku without params and the other one with params
        # Only one param for jump_table so it is empty.
        # Show modifications instead.
        cls.index.mskus += [
            MarketSku(
                title="psku_no_param",
                hyperid=5573,
                sku=622233,
                hid=19,
                blue_offers=[BlueOffer(price=5, vat=Vat.VAT_10, feedid=3)],
                glparams=[],
            ),
            MarketSku(
                title="psku_with_param",
                hyperid=5573,
                sku=622234,
                hid=19,
                blue_offers=[BlueOffer(price=5, vat=Vat.VAT_10, feedid=3)],
                glparams=[GLParam(param_id=328, value=(9421), is_filter=True)],
            ),
        ]

    def test_modifications_not_empty_jump_table_new_pipeline(self):
        '''
        Проверяем, что psku у которых одинаковый набор определяющих параметров
        будут показаны как модификации.
        '''
        request = "place=sku_offers&market-sku=622221&rgb=blue&rearr-factors=use_new_jump_table_pipeline=1"
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "filters": [
                                {
                                    "values": [
                                        {
                                            "id": "6421",
                                            "marketSku": "622221",
                                            "value": "VALUE-6421",
                                            "found": 1,
                                            "checked": True,
                                            "fuzzy": Absent(),
                                            "slug": "psku-1-1",
                                        },
                                        {
                                            "id": "6422",
                                            "marketSku": "622223",
                                            "value": "VALUE-6422",
                                            "found": 1,
                                            "checked": Absent(),
                                            "fuzzy": Absent(),
                                            "slug": "psku-2-1",
                                        },
                                        {
                                            "id": "6423",
                                            "marketSku": "622224",
                                            "value": "VALUE-6423",
                                            "found": 1,
                                            "checked": Absent(),
                                            "fuzzy": Absent(),
                                            "slug": "psku-3-1",
                                        },
                                    ],
                                    "valuesCount": 3,
                                    "xslname": "GLPARAM325",
                                    "name": "GLPARAM-325",
                                    "type": "enum",
                                    "id": "325",
                                },
                                {
                                    "values": [
                                        {
                                            "id": "622221",
                                            "marketSku": "622221",
                                            "value": "psku 1 1",
                                            "found": 1,
                                            "slug": "psku-1-1",
                                            "checked": True,
                                        },
                                        {
                                            "id": "622222",
                                            "marketSku": "622222",
                                            "value": "psku 1 2",
                                            "found": 1,
                                            "slug": "psku-1-2",
                                            "checked": Absent(),
                                            "fuzzy": Absent(),
                                        },
                                    ],
                                    "valuesCount": 2,
                                    "subType": "text_extended",
                                    "name": "Модификации",
                                    "type": "enum",
                                    "id": "modifications",
                                },
                            ],
                        }
                    ],
                }
            },
            allow_different_len=False,
        )

    def test_modifications_empty_jump_table_new_pipeline(self):
        '''
        Проверяем, что когда у всех psku определяющие параметры полностью совпадают,
        то показыватся только таблица модификаций, содержащая список всех psku.
        '''
        request = "place=sku_offers&market-sku=622225&rgb=blue&rearr-factors=use_new_jump_table_pipeline=1"
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "filters": [
                                {
                                    "values": [
                                        {
                                            "id": "622225",
                                            "marketSku": "622225",
                                            "value": "psku_same_params_1",
                                            "found": 1,
                                            "checked": True,
                                            "fuzzy": Absent(),
                                            "slug": "psku-same-params-1",
                                        },
                                        {
                                            "id": "622226",
                                            "marketSku": "622226",
                                            "value": "psku_same_params_2",
                                            "found": 1,
                                            "slug": "psku-same-params-2",
                                            "checked": Absent(),
                                            "fuzzy": Absent(),
                                        },
                                    ],
                                    "valuesCount": 2,
                                    "subType": "text_extended",
                                    "name": "Модификации",
                                    "type": "enum",
                                    "id": "modifications",
                                }
                            ],
                        }
                    ],
                }
            },
            allow_different_len=False,
        )

    def test_modifications_empty_jump_table_no_params_new_pipeline(self):
        '''
        Проверяем, что когда у всех psku определяющие параметры полностью отсутствуют,
        то показыватся только таблица модификаций, содержащая список всех psku.
        '''
        request = "place=sku_offers&market-sku=622227&rgb=blue&rearr-factors=use_new_jump_table_pipeline=1"
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "filters": [
                                {
                                    "values": [
                                        {
                                            "id": "622227",
                                            "marketSku": "622227",
                                            "value": "psku_no_params_1",
                                            "found": 1,
                                            "checked": True,
                                            "fuzzy": Absent(),
                                            "slug": "psku-no-params-1",
                                        },
                                        {
                                            "id": "622228",
                                            "marketSku": "622228",
                                            "value": "psku_no_params_2",
                                            "found": 1,
                                            "slug": "psku-no-params-2",
                                            "checked": Absent(),
                                            "fuzzy": Absent(),
                                        },
                                    ],
                                    "valuesCount": 2,
                                    "subType": "text_extended",
                                    "name": "Модификации",
                                    "type": "enum",
                                    "id": "modifications",
                                }
                            ],
                        }
                    ],
                }
            },
            allow_different_len=False,
        )

    def test_modifications_mixed_case_jump_table_new_pipeline(self):
        '''
        Проверяем, что при неконсистентном заполнении параметров,
        выводится таблица модификаций.
        '''
        request = "place=sku_offers&market-sku=622229&rgb=blue&rearr-factors=use_new_jump_table_pipeline=1&rearr-factors=market_jumptable_show_modifications=1"
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "filters": [
                                {
                                    "values": [
                                        {
                                            "id": "622229",
                                            "marketSku": "622229",
                                            "value": "psku_no_param_1",
                                            "found": 1,
                                            "checked": True,
                                            "fuzzy": Absent(),
                                            "slug": "psku-no-param-1",
                                        },
                                        {
                                            "id": "622230",
                                            "marketSku": "622230",
                                            "value": "psku_no_param_2",
                                            "found": 1,
                                            "checked": Absent(),
                                            "fuzzy": Absent(),
                                            "slug": "psku-no-param-2",
                                        },
                                        {
                                            "id": "622231",
                                            "marketSku": "622231",
                                            "value": "psku_with_param_1",
                                            "found": 1,
                                            "checked": Absent(),
                                            "fuzzy": Absent(),
                                            "slug": "psku-with-param-1",
                                        },
                                        {
                                            "id": "622232",
                                            "marketSku": "622232",
                                            "value": "psku_with_param_2",
                                            "found": 1,
                                            "checked": Absent(),
                                            "fuzzy": Absent(),
                                            "slug": "psku-with-param-2",
                                        },
                                    ],
                                    "valuesCount": 4,
                                    "subType": "text_extended",
                                    "name": "Модификации",
                                    "type": "enum",
                                    "id": "modifications",
                                }
                            ],
                        }
                    ],
                }
            },
            allow_different_len=False,
        )

    def test_modifications_mixed_trivial_case_new_pipeline(self):
        '''
        Проверяем, что когда есть psku с заполненными и незаполненными определяющими параметрами,
        при этом таблица переходов оказалась пустой, отрисовываем таблицу модификаций.
        '''
        request = "place=sku_offers&market-sku=622233&rgb=blue&rearr-factors=use_new_jump_table_pipeline=1"
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "filters": [
                                {
                                    "values": [
                                        {
                                            "id": "622233",
                                            "marketSku": "622233",
                                            "value": "psku_no_param",
                                            "found": 1,
                                            "checked": True,
                                            "fuzzy": Absent(),
                                            "slug": "psku-no-param",
                                        },
                                        {
                                            "id": "622234",
                                            "marketSku": "622234",
                                            "value": "psku_with_param",
                                            "found": 1,
                                            "slug": "psku-with-param",
                                            "checked": Absent(),
                                            "fuzzy": Absent(),
                                        },
                                    ],
                                    "valuesCount": 2,
                                    "subType": "text_extended",
                                    "name": "Модификации",
                                    "type": "enum",
                                    "id": "modifications",
                                }
                            ],
                        }
                    ],
                }
            },
            allow_different_len=False,
        )

    @classmethod
    def add_model_group(self, blue_special, group_id):
        group_prefix = group_id * 1000 if blue_special else 0
        self.index.model_groups += [
            ModelGroup(hid=group_id, hyperid=group_id, ts=group_id, title='group model', blue_special=blue_special),
        ]

        not_from_group_hyperid = group_prefix + 310

        self.index.gltypes += [
            GLType(
                param_id=group_prefix + 901,
                hid=group_id,
                cluster_filter=True,
                model_filter_index=1,
                gltype=GLType.ENUM,
                subtype='image_picker',
                values=[1, 2, 3],
            ),
            GLType(
                param_id=group_prefix + 902, hid=group_id, cluster_filter=True, model_filter_index=2, gltype=GLType.ENUM
            ),
            GLType(
                param_id=group_prefix + 903,
                hid=group_id,
                cluster_filter=False,
                model_filter_index=3,
                gltype=GLType.ENUM,
                values=[1, 2],
            ),
        ]
        MSKUS_IN_ONE_GROUP_PREFIX = [group_prefix + sku for sku in MSKUS_IN_ONE_GROUP]
        self.index.models += [
            Model(
                hyperid=MSKUS_IN_ONE_GROUP_PREFIX[0],
                hid=group_id,
                group_hyperid=group_id,
                parameter_value_links=[
                    ParameterValue(
                        param_id=group_prefix + 901,
                        option_id=1,
                        picture=ImagePickerData(
                            url='//avatars.mds.yandex.net/get-mpic/466729/img_group_model1_1/orig',
                            namespace="get-mpic",
                            group_id='466729',
                            image_name='img_group_model1_1',
                        ),
                    ),
                    ParameterValue(
                        param_id=group_prefix + 901,
                        option_id=2,
                        picture=ImagePickerData(
                            url='//avatars.mds.yandex.net/get-mpic/466729/img_group_model1_2/orig',
                            namespace="get-mpic",
                            group_id='466729',
                            image_name='img_group_model1_2',
                        ),
                    ),
                ],
            ),
            Model(
                hyperid=MSKUS_IN_ONE_GROUP_PREFIX[1],
                hid=group_id,
                group_hyperid=group_id,
                parameter_value_links=[
                    ParameterValue(
                        param_id=901,
                        option_id=1,
                        picture=ImagePickerData(
                            url='//avatars.mds.yandex.net/get-mpic/466729/img_group_model2_1/orig',
                            namespace="get-mpic",
                            group_id='466729',
                            image_name='img_group_model2_1',
                        ),
                    ),
                ],
            ),
            Model(hyperid=not_from_group_hyperid, hid=group_id),
        ]

        def make_blue_offer(sku):
            return BlueOffer(
                price=5, price_old=8, vat=Vat.VAT_10, offerid='Shop1_sku' + str(sku), feedid=3, delivery_buckets=[2]
            )

        mskus = [
            {
                'hyperid': MSKUS_IN_ONE_GROUP_PREFIX[0],
                'params': {group_prefix + 901: 1, group_prefix + 902: 1, group_prefix + 903: 1},
            },
            {
                'hyperid': MSKUS_IN_ONE_GROUP_PREFIX[1],
                'params': {group_prefix + 901: 2, group_prefix + 902: 1, group_prefix + 903: 2},
            },
            {
                'hyperid': not_from_group_hyperid,
                'params': {group_prefix + 901: 3, group_prefix + 902: 1},
            },
        ]

        self.index.mskus += [
            MarketSku(
                title="offer" + str(msku['hyperid']),
                hyperid=msku['hyperid'],
                sku=msku['hyperid'],
                blue_offers=[make_blue_offer(msku['hyperid'])],
                glparams=[GLParam(param_id=key, value=value) for key, value in msku['params'].items()],
            )
            for msku in mskus
        ]

    @classmethod
    def prepare_group_jump_table(self):
        self.add_model_group(False, GROUP_ID)

    @classmethod
    def prepare_group_jump_table_blue_specific(self):
        self.add_model_group(True, GROUP_ID_BLUE_SPECIFIC)

    def test_sku_offers_jump_table_by_parent_model(self):
        """
        Проверяем что значения из группы попадают в таблицу переходов
        Для 901 фильтра попадает значение 1 (этого же msku), 2 (msku из этой же группы). Значение 3 не попадает из-за того что не входит в группу
        Фильтр 902 не попадает из-за того что у обоих msku из группы одинаковое значение фильтра
        Фильтр 903 не попадает из-за того что является параметром первого рода (cluster_filter=False)
        """
        for pipeline in [0, 1]:
            for index in [0, 1]:
                response = self.report.request_json(
                    'place=sku_offers&market-sku={}&show-urls=direct&rearr-factors=use_new_jump_table_pipeline={};market_blue_use_parent_model_for_jump_table=1&show-models=1&rgb=blue'.format(
                        MSKUS_IN_ONE_GROUP[index], pipeline
                    )
                )
                self.assertFragmentIn(
                    response,
                    {
                        "results": [
                            {
                                "entity": "sku",
                                "id": str(MSKUS_IN_ONE_GROUP[index]),
                                "filters": [
                                    {
                                        "id": "901",
                                        "values": [
                                            {
                                                "id": "1",  # Значение нашего СКУ - переход на этот же СКУ. Значение помечено checked
                                                "marketSku": str(MSKUS_IN_ONE_GROUP[0]),
                                                "fuzzy": Absent(),
                                                "checked": True if index == 0 else Absent(),
                                                "picker": {
                                                    "imageName": "img_group_model{}_1".format(index + 1),
                                                },
                                            },
                                            {
                                                "id": "2",  # Точный переход на СКУ 309
                                                "marketSku": str(MSKUS_IN_ONE_GROUP[1]),
                                                "fuzzy": Absent(),
                                                "checked": True if index == 1 else Absent(),
                                                "picker": {
                                                    # У второй модели нет картинки для значения 2. Поэтому оно всегда будет получено из первой модели
                                                    "imageName": "img_group_model1_2",
                                                },
                                            },
                                        ],
                                    }
                                ],
                            }
                        ]
                    },
                    allow_different_len=False,
                    preserve_order=True,
                )

    def test_show_shop_operational_rating(self):
        """
        Проверяем, что при флаге market_operational_rating=1 показывается операционный рейтинг
        """
        OPERATIONAL_RATING_FLAGS = (
            ('', True),
            ('&rearr-factors=market_operational_rating=1', True),
            ('&rearr-factors=market_operational_rating=0', False),
        )

        for rearr_flag, has_rating in OPERATIONAL_RATING_FLAGS:
            response = self.report.request_json(
                'place=sku_offers&market-sku=1&show-urls=direct,cpa&rids=213&rgb=BLUE{}'.format(rearr_flag)
            )
            if has_rating:
                self.assertFragmentIn(
                    response,
                    {
                        "wareId": T.sku1_offer1.waremd5,
                        "supplier": {
                            "id": 3,
                            "operationalRating": {
                                "calcTime": 1589936458409,
                                "lateShipRate": 5.9,
                                "cancellationRate": 1.93,
                                "returnRate": 0.14,
                                "total": 99.8,
                            },
                        },
                    },
                )
            else:
                self.assertFragmentIn(
                    response, {"wareId": T.sku1_offer1.waremd5, "supplier": {"id": 3, "operationalRating": Absent()}}
                )

    def test_hide_descriptions(self):
        '''Отрываем описания под флагом market_hide_descriptions
        https://st.yandex-team.ru/MARKETOUT-36826
        '''
        response = self.report.request_json(
            'place=sku_offers&market-sku=1&show-urls=direct,cpa&rids=213&rgb=BLUE&rearr-factors=market_hide_descriptions=1'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "sku",
                        "description": "",
                        "id": "1",
                        "showUid": NotEmpty(),
                        "product": {
                            "id": 1,
                        },
                        "offers": {
                            "items": [
                                {
                                    "entity": "offer",
                                    "description": "",
                                }
                            ]
                        },
                    }
                ]
            },
            allow_different_len=False,
        )
        self.assertFragmentNotIn(
            response,
            {
                "results": [
                    {
                        "formattedDescription": {
                            "shortPlain": SHORT_PLAIN_DESCRIPTION,
                            "fullPlain": FULL_PLAIN_DESCRIPTION,
                            "shortHtml": SHORT_HTML_DESCRIPTION,
                            "fullHtml": FULL_HTML_DESCRIPTION,
                        },
                    }
                ]
            },
        )

    def test_non_zero_market_sku(self):
        """
        Проверяем, что репорт отфильтровывает market-sku=0, если поднят флаг filter_out_msku_id_zero_on_sku_offers.
        """

        response = self.report.request_json('place=sku_offers&market-sku=0&rids=213&rgb=BLUE')
        self.assertFragmentIn(
            response,
            {
                'total': 0,
            },
        )

        response = self.report.request_json(
            'place=sku_offers&market-sku=0&rids=213&rgb=BLUE&rearr-factors=filter_out_msku_id_zero_on_sku_offers=1'
        )
        self.error_log.expect(code=3043, message="provide at least 1 non-zero market-sku").once()

        response = self.report.request_json('place=sku_offers&market-sku=0&market-sku=1&rids=213&rgb=BLUE')
        self.assertFragmentIn(
            response,
            {
                'total': 1,
            },
        )

        response = self.report.request_json(
            'place=sku_offers&market-sku=0&market-sku=1&rids=213&rgb=BLUE&rearr-factors=filter_out_msku_id_zero_on_sku_offers=1'
        )
        self.assertFragmentIn(
            response,
            {
                'total': 1,
            },
        )


if __name__ == '__main__':
    main()
