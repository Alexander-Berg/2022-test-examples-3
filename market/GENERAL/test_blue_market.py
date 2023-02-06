#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    ClickType,
    Currency,
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryCalcFeedInfo,
    DeliveryOption,
    DeliveryServiceRegionToRegionInfo,
    DynamicDaysSet,
    DynamicDeliveryServiceInfo,
    DynamicMarketSku,
    DynamicShop,
    DynamicSkuOffer,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseInfo,
    DynamicWarehouseToWarehouseInfo,
    GLParam,
    GLType,
    GLValue,
    GpsCoord,
    HyperCategory,
    HyperCategoryType,
    ImagePickerData,
    LinkData,
    MnPlace,
    Model,
    NavCategory,
    NidsRedirector,
    Offer,
    Outlet,
    OutletDeliveryOption,
    ParameterValue,
    Payment,
    PickupBucket,
    PickupOption,
    Picture,
    RedirectorRecord,
    Region,
    RegionalDelivery,
    Shop,
    Suggestion,
    TimeInfo,
    WarehouseWithPriority,
    WarehousesPriorityInRegion,
    ShopOperationalRating,
)
from core.testcase import (
    TestCase,
    main,
)
from core.types.hypercategory import (
    DRUGS2_CATEG_ID,
    # TODO: выпилить здесь: https://st.yandex-team.ru/MARKETOUT-33898
    HEALTH_CATEG_ID,
    PAINKILLERS_CATEG_ID,
    VITAMINS_AND_MINERALS_CATEG_ID,
    DISINFECTANTS_CATEG_ID,
    # TODO: выпилить ^^^^^^
    MARKET_SUBSCRIPTIONS_CATEG_ID,
)
from core.types.offer import (
    OfferDimensions,
)
from core.types.sku import (
    MarketSku,
    BlueOffer,
)
from core.types.taxes import (
    Vat,
    Tax,
)
from core.matcher import Absent, LikeUrl, NotEmpty, Contains, NoKey, Round, EmptyList, Regex
from core.types.delivery import BlueDeliveryTariff
from core.types.picture import thumbnails_config
from unittest import skip

USE_DEPRECATED_DIRECT_SHIPPING_FLOW = "&rearr-factors=get_rid_of_direct_shipping=0"


def make_hide_rules_requests(prefix, additionalRearrFactors=dict()):
    '''
    Make requests with all possible values of `hide_rules_strategy` flag
    '''

    def makeRearrString(strat):
        rearrFactors = additionalRearrFactors.copy()
        rearrFactors['hide_rules_strategy'] = strat
        return ';'.join(['{}={}'.format(key, val) for key, val in rearrFactors.items()])

    requests = [
        prefix + '&rearr-factors={}'.format(makeRearrString(strat))
        for strat in ['use_dynamic', 'use_all_sources', 'use_unified_hide_rules']
    ]
    return requests


def date_switch_time_info(switch_hour):
    return DateSwitchTimeAndRegionInfo(date_switch_hour=switch_hour, region_to=225)


warehouse145_delivery_service_157 = DynamicWarehouseAndDeliveryServiceInfo(
    warehouse_id=145, delivery_service_id=157, operation_time=0, date_switch_time_infos=[date_switch_time_info(2)]
)
warehouse145_delivery_service_158 = DynamicWarehouseAndDeliveryServiceInfo(
    warehouse_id=145, delivery_service_id=158, operation_time=0, date_switch_time_infos=[date_switch_time_info(3)]
)
warehouse147_delivery_service_147147 = DynamicWarehouseAndDeliveryServiceInfo(
    warehouse_id=147, delivery_service_id=147147, operation_time=0, date_switch_time_infos=[date_switch_time_info(3)]
)


class _Shops(object):
    blue_virtual_shop = Shop(
        fesh=1,
        datafeed_id=1,
        priority_region=213,
        name='virtual_shop',
        currency=Currency.RUR,
        tax_system=Tax.OSN,
        fulfillment_virtual=True,
        delivery_service_outlets=[2001, 2003, 2004],
        cpa=Shop.CPA_REAL,
        virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
        work_schedule='virtual shop work schedule',
        medicine_courier=True,
    )

    red_virtual_shop = Shop(
        fesh=101,
        name='red virtual_shop',
        fulfillment_virtual=True,
        virtual_shop_color=Shop.VIRTUAL_SHOP_RED,
        cpa=Shop.CPA_REAL,
    )

    green_shop = Shop(
        fesh=2,
        priority_region=213,
        name='green_shop',
        currency=Currency.RUR,
        tax_system=Tax.OSN,
        cpa=Shop.CPA_REAL,
    )

    book_shop = Shop(
        fesh=577858,
        datafeed_id=58,
        priority_region=200,
        name='MyShop.ru',
        currency=Currency.RUR,
        supplier_type=Shop.THIRD_PARTY,
        tax_system=Tax.OSN,
        blue=Shop.BLUE_REAL,
        cpa=Shop.CPA_REAL,
        warehouse_id=220,
        medicine_courier=True,
    )

    blue_shop_1 = Shop(
        fesh=3,
        datafeed_id=3,
        priority_region=2,
        name='blue_shop_1',
        currency=Currency.RUR,
        tax_system=Tax.OSN,
        supplier_type=Shop.FIRST_PARTY,
        fulfillment_program=True,
        blue=Shop.BLUE_REAL,
        cpa=Shop.CPA_REAL,
        warehouse_id=145,
        work_schedule='work schedule supplier 3',
        medicine_courier=True,
    )

    blue_shop_1470 = Shop(
        fesh=1470,
        datafeed_id=14700,
        priority_region=39,
        name='blue_shop_1470',
        currency=Currency.RUR,
        tax_system=Tax.OSN,
        supplier_type=Shop.FIRST_PARTY,
        fulfillment_program=True,
        blue=Shop.BLUE_REAL,
        cpa=Shop.CPA_REAL,
        warehouse_id=147,
        work_schedule='work schedule supplier 1470',
        medicine_courier=True,
    )

    blue_shop_2 = Shop(
        fesh=4,
        datafeed_id=4,
        priority_region=213,
        name='blue_shop_2',
        currency=Currency.RUR,
        tax_system=Tax.OSN,
        supplier_type=Shop.THIRD_PARTY,
        fulfillment_program=True,
        blue=Shop.BLUE_REAL,
        warehouse_id=145,
        medicine_courier=True,
    )

    golden_partner = Shop(
        fesh=5,
        datafeed_id=5,
        priority_region=213,
        name='moscow_pharma',
        currency=Currency.RUR,
        tax_system=Tax.OSN,
        supplier_type=Shop.THIRD_PARTY,
        blue=Shop.BLUE_REAL,
        fulfillment_program=False,
        ignore_stocks=True,
        warehouse_id=555,
        medicine_courier=True,
    )

    dropship_shop = Shop(
        fesh=6,
        datafeed_id=6,
        priority_region=213,
        name='DRPSHP',
        currency=Currency.RUR,
        tax_system=Tax.OSN,
        supplier_type=Shop.THIRD_PARTY,
        blue=Shop.BLUE_REAL,
        fulfillment_program=False,
        warehouse_id=666,
        medicine_courier=True,
    )

    dropship_with_delivery_shop = Shop(
        fesh=7,
        datafeed_id=7,
        priority_region=213,
        name='DROP + SHOP_CARRIER',
        currency=Currency.RUR,
        tax_system=Tax.OSN,
        supplier_type=Shop.THIRD_PARTY,
        blue=Shop.BLUE_REAL,
        fulfillment_program=False,
        warehouse_id=777,
        medicine_courier=True,
    )

    dsbs_shop = Shop(fesh=1001, datafeed_id=10, priority_region=213, cpa=Shop.CPA_REAL, cpc=Shop.CPC_NO)


class _Offers(object):
    sku1_offer1 = BlueOffer(
        price=5,
        price_old=8,
        vat=Vat.VAT_10,
        feedid=3,
        offerid='blue.offer.1.1',
        waremd5='Sku1Price5-IiLVm1Goleg',
        randx=1,
    )
    sku1_offer2 = BlueOffer(
        price=50,
        vat=Vat.VAT_0,
        feedid=4,
        offerid='blue.offer.1.2',
        waremd5='Sku1Price50-iLVm1Goleg',
        randx=2,
    )

    sku2_offer1 = BlueOffer(
        price=55,
        vat=Vat.VAT_18,
        feedid=3,
        offerid='blue.offer.2.1',
        waremd5='Sku2Price55-iLVm1Goleg',
        randx=3,
    )
    sku2_offer2 = BlueOffer(
        price=50,
        vat=Vat.NO_VAT,
        feedid=4,
        offerid='blue.offer.2.2',
        waremd5='Sku2Price50-iLVm1Goleg',
        randx=4,
    )
    sku2_offer3 = BlueOffer(
        price=52,
        vat=Vat.NO_VAT,
        feedid=3,
        offerid='blue.offer.2.3',
        waremd5='Sku2Price52-iLVm1Goleg',
        randx=5,
    )
    sku2_offer4 = BlueOffer(
        price=53,
        vat=Vat.NO_VAT,
        feedid=4,
        offerid='blue.offer.2.4',
        waremd5='Sku2Price53-iLVm1Goleg',
        randx=6,
    )
    sku2_offer5 = BlueOffer(
        price=54,
        vat=Vat.NO_VAT,
        feedid=4,
        offerid='blue.offer.2.5',
        waremd5='Sku2Price54-iLVm1Goleg',
        randx=7,
    )

    """При наличии стоков все равно предзаказ должен быть в приоритете и товар можно приобрести только по предзаказу"""
    sku4_offer1 = BlueOffer(
        price=55,
        vat=Vat.VAT_18,
        feedid=3,
        offerid='blue.offer.4.1',
        waremd5='Sku4Price55-iLVm1Goleg',
        model_title='model-aware title',
        stock_store_count=10,
    )

    sku5_offer1 = BlueOffer(
        price=1000,
        vat=Vat.VAT_10,
        feedid=3,
        offerid='blue.offer.5.1',
        waremd5='Sku5PriceL-IiLVm1Goleg',
    )
    sku5_offer2 = BlueOffer(
        price=1001,
        vat=Vat.VAT_10,
        feedid=4,
        offerid='blue.offer.5.2',
        waremd5='Sku5PriceH-IiLVm1Goleg',
    )

    sku6_offer1 = BlueOffer(
        price=1,
        vat=Vat.VAT_10,
        feedid=3,
        offerid='blue.offer.6.1',
        waremd5='Sku6Price1-IiLVm1Goleg',
    )
    sku6_offer2 = BlueOffer(
        price=2,
        vat=Vat.VAT_10,
        feedid=3,
        offerid='blue.offer.6.2',
        waremd5='Sku6Price2-IiLVm1Goleg',
    )

    sku7_offer1 = BlueOffer(
        price=1,
        vat=Vat.VAT_10,
        feedid=3,
        offerid='blue.offer.7.1',
        waremd5='Sku7Price1-IiLVm1Goleg',
    )

    sku8_offer1 = BlueOffer(
        price=1,
        vat=Vat.VAT_10,
        feedid=3,
        offerid='blue.offer.8.1',
        waremd5='Sku8Price1-IiLVm1Goleg',
    )
    sku8_offer2 = BlueOffer(
        price=2,
        vat=Vat.VAT_10,
        feedid=3,
        offerid='blue.offer.8.2',
        waremd5='Sku8Price2-IiLVm1Goleg',
    )
    sku8_offer3 = BlueOffer(
        price=3,
        vat=Vat.VAT_10,
        feedid=3,
        offerid='blue.offer.8.3',
        waremd5='Sku8Price3-IiLVm1Goleg',
    )

    sku14_offer1 = BlueOffer(
        price=1,
        vat=Vat.VAT_10,
        feedid=4,
        offerid='blue.offer.14.1',
        waremd5='Sku14Price1-IiLVm1GolQ',
    )
    sku14_offer2 = BlueOffer(
        price=2000,
        vat=Vat.VAT_10,
        feedid=14700,
        offerid='blue.offer.14.2',
        waremd5='Sku14Price2-IiLVm1GolQ',
    )

    sku15_offer1 = BlueOffer(
        price=1,
        vat=Vat.VAT_10,
        feedid=4,
        offerid='blue.offer.15.1',
        waremd5='Sku15Price1-IiLVm1Gole',
    )

    parallel_sku1_offer1 = BlueOffer(
        price=7,
        vat=Vat.VAT_10,
        feedid=3,
        offerid='blue.offer.P1.1',
        waremd5='SkuP1Price7-iLVm1Goleg',
    )
    parallel_sku2_offer1 = BlueOffer(
        price=7,
        vat=Vat.VAT_10,
        feedid=3,
        offerid='blue.offer.P2.1',
        waremd5='SkuP2Price7-iLVm1Goleg',
    )
    parallel_sku3_offer1 = BlueOffer(
        price=7,
        vat=Vat.VAT_10,
        feedid=3,
        offerid='blue.offer.P3.1',
        waremd5='SkuP3Price7-iLVm1Goleg',
    )

    prime_offer = BlueOffer(
        price=10,
        vat=Vat.VAT_10,
        feedid=3,
        offerid='prime.offer',
        waremd5='x2uPN3XNsizR0Kt2DeS6MQ',
    )

    cat_offer2 = BlueOffer(price=10, vat=Vat.VAT_10, feedid=3, offerid='cat.offer.2')
    cat_offer3 = BlueOffer(price=10, vat=Vat.VAT_10, feedid=3, offerid='cat.offer.3')
    cat_offer4 = BlueOffer(price=10, vat=Vat.VAT_10, feedid=3, offerid='cat.offer.4')
    cat_offer5 = BlueOffer(price=10, vat=Vat.VAT_10, feedid=3, offerid='cat.offer.5')
    cat_offer6 = BlueOffer(price=10, vat=Vat.VAT_10, feedid=3, offerid='cat.offer.6', randx=6001)
    cat_offer6_1 = BlueOffer(price=10, vat=Vat.VAT_10, feedid=3, offerid='cat.offer.6.1', randx=5001)
    cat_offer7 = BlueOffer(
        price=10,
        vat=Vat.VAT_10,
        feedid=5,
        offerid='cat.offer.7',
        is_fulfillment=False,
        waremd5='BlueNoFulfillment____g',
    )
    cat_offer8 = BlueOffer(price=10, vat=Vat.VAT_10, feedid=3, offerid='cat.offer.8')
    cat_offer9 = BlueOffer(price=10, vat=Vat.VAT_10, feedid=3, offerid='cat.offer.9')
    cat_offer10 = BlueOffer(
        price=10,
        vat=Vat.VAT_10,
        feedid=3,
        offerid='cat.offer.10',
        post_term_delivery=True,
        waremd5='BlueOfferFulfillment_g',
        weight=7,
        dimensions=OfferDimensions(length=10, width=20, height=30),
    )
    cat_offer11 = BlueOffer(price=10, vat=Vat.VAT_10, feedid=3, offerid='cat.offer.11')
    cat_offer12 = BlueOffer(price=10, vat=Vat.VAT_10, feedid=3, offerid='cat.offer.12')
    dog_offer13 = BlueOffer(price=10, vat=Vat.VAT_10, feedid=3, offerid='dog.offer.13')

    pills_offer = BlueOffer(
        price=1500,
        vat=Vat.VAT_18,
        feedid=5,
        offerid='pills.1',
        is_fulfillment=False,
        waremd5='BluePills____________g',
        is_medicine=True,
    )
    pills_offer_ff = BlueOffer(
        price=2000,
        vat=Vat.VAT_18,
        feedid=3,
        offerid='pills.1.ff',
        waremd5='BluePillsFulfillment_g',
        is_medicine=True,
    )
    pills_offer_ff2 = BlueOffer(
        price=2000,
        vat=Vat.VAT_18,
        feedid=3,
        offerid='pills.1.ff2',
        waremd5='BluePillsFF2_________g',
        is_medicine=True,
    )
    # TODO: выпилить здесь: https://st.yandex-team.ru/MARKETOUT-33898
    painkiller_offer = BlueOffer(
        price=1500,
        vat=Vat.VAT_18,
        feedid=5,
        offerid='painkiller.1',
        is_fulfillment=False,
        waremd5='BluePainkiller_______g',
        is_medicine=True,
    )
    painkiller_offer_ff2 = BlueOffer(
        price=2000,
        vat=Vat.VAT_18,
        feedid=3,
        offerid='painkiller.1.ff2',
        waremd5='BluePainkillerFF2____g',
        is_medicine=True,
    )
    vitamin_Z_offer = BlueOffer(
        price=1500,
        vat=Vat.VAT_18,
        feedid=5,
        offerid='vitamin_Z.1',
        is_fulfillment=False,
        waremd5='BlueVitaminZ_________g',
        is_baa=True,
    )
    vitamin_Z_offer_ff2 = BlueOffer(
        price=2000,
        vat=Vat.VAT_18,
        feedid=3,
        offerid='vitamin_Z.1.ff2',
        waremd5='BlueVitaminZFF2______g',
        is_baa=True,
    )
    germkiller_offer = BlueOffer(
        price=1500,
        vat=Vat.VAT_18,
        feedid=5,
        offerid='germkiller.1',
        is_fulfillment=False,
        waremd5='BlueGermkiller_______g',
    )
    germkiller_offer_ff2 = BlueOffer(
        price=2000,
        vat=Vat.VAT_18,
        feedid=3,
        offerid='germkiller.1.ff2',
        waremd5='BlueGermkillerFF2____g',
    )
    # TODO: выпилить ^^^^^^
    fridge_offer = BlueOffer(
        price=35000,
        vat=Vat.VAT_20,
        feedid=6,
        offerid='CB.1',
        is_fulfillment=False,
        waremd5='SoLargeWeDontStoreIt_g',
        weight=75,
        dimensions=OfferDimensions(length=80, width=60, height=220),
    )
    all_delivery_offer = BlueOffer(
        price=3500,
        vat=Vat.VAT_20,
        feedid=7,
        offerid='birdie',
        is_fulfillment=False,
        waremd5='DeliveredByShopToo___g',
        weight=1,
        dimensions=OfferDimensions(length=20, width=30, height=20),
    )

    p_one_offer = BlueOffer(
        price=200, vat=Vat.VAT_10, feedid=3, offerid='p_one.offer', waremd5='FAKE1POFFEROOOOOOOOOOQ'
    )
    p_one_offer_md = BlueOffer(
        price=100,
        vat=Vat.VAT_10,
        feedid=5,
        offerid='p_three_md.offer',
        waremd5='FAKE3POFFERTTTTTTTTTTQ',
    )

    offer_3p = BlueOffer(price=100500, vat=Vat.VAT_10, feedid=4, waremd5='TestOffer_3P_________g')
    offer_3p_stool = BlueOffer(price=100500, vat=Vat.VAT_10, feedid=4, waremd5='TestOffer_3P_Stool___g')
    rus_eng_dict_offer = BlueOffer(
        price=325,
        vat=Vat.VAT_18,
        feedid=58,
        waremd5='ReuEngDict___________g',
        offerid='rus.eng.dict',
    )

    dsbs_offer = Offer(
        cpa=Offer.CPA_REAL,
        price=666,
        fesh=1001,
        feedid=10,
        waremd5='DsbsWithoutMsku______g',
        offerid='proh.offer',
    )
    dsbs_with_msku_offer = Offer(
        sku=9116,
        cpa=Offer.CPA_REAL,
        price=666,
        fesh=1001,
        feedid=11,
        waremd5='DsbsWithMsku_________g',
        offerid='proh.offer',
        vat=Vat.NO_VAT,
    )

    white_offer = Offer(price=10, waremd5='White________________g', hyperid=4242)


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.rgb_blue_is_cpa = True
        cls.settings.nordstream_autogenerate = False

        cls.settings.default_search_experiment_flags += ['market_nordstream=0']
        cls.settings.default_search_experiment_flags += ['market_new_cpm_iterator=0']
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        pic = Picture(
            picture_id="KdwwrYb4czANgt9-3poEQQ",
            width=500,
            height=600,
            thumb_mask=thumbnails_config.get_mask_by_names(['1x1', '100x100']),
            group_id=1234,
        )
        pic2 = Picture(
            picture_id="KdwwrYb4caANgt9-3poEQQ",
            width=400,
            height=700,
            thumb_mask=thumbnails_config.get_mask_by_names(['1x1', '100x100']),
            group_id=1234,
        )

        cls.index.hypertree = [
            HyperCategory(
                hid=1000,
                name='A',
                children=[
                    HyperCategory(hid=2000, name='AA'),
                    HyperCategory(hid=3000, name='AB'),
                ],
            ),
            HyperCategory(
                hid=4000,
                name='B',
                children=[
                    HyperCategory(
                        hid=5000,
                        name='BA',
                        children=[
                            HyperCategory(
                                hid=6000,
                                name='BAA',
                                children=[
                                    HyperCategory(hid=7000, name='BAAA'),
                                    HyperCategory(hid=9000, name='BAAB'),
                                    HyperCategory(hid=10000, name='BAAC'),
                                    HyperCategory(
                                        hid=11000,
                                        name='BAAD',
                                        children=[
                                            HyperCategory(hid=12000, name='BAADA'),
                                        ],
                                    ),
                                ],
                            ),
                        ],
                    ),
                    HyperCategory(hid=8000, name='BB'),
                ],
            ),
            HyperCategory(
                hid=1,
                fee=123,
                children=[
                    HyperCategory(hid=4, fee=321),
                ],
            ),
            HyperCategory(hid=MARKET_SUBSCRIPTIONS_CATEG_ID, fee=123),
            HyperCategory(uniq_name="Books", hid=90829, children=[HyperCategory(hid=90831, uniq_name='Dictionaries')]),
            # TODO: выпилить здесь: https://st.yandex-team.ru/MARKETOUT-33898
            HyperCategory(
                hid=HEALTH_CATEG_ID,
                name='Товары для здоровья',
                children=[
                    HyperCategory(
                        hid=DRUGS2_CATEG_ID,
                        name='Лекарственные препараты и БАД',
                        children=[
                            HyperCategory(hid=PAINKILLERS_CATEG_ID, name='Лечение боли'),
                        ],
                    ),
                    HyperCategory(hid=VITAMINS_AND_MINERALS_CATEG_ID, name='Витамины и минералы'),
                    HyperCategory(hid=DISINFECTANTS_CATEG_ID, name='Дезинфицирующие средства'),
                ],
            ),
            # TODO: выпилить ^^^^^^
        ]

        cls.index.navtree = [
            NavCategory(
                hid=1000,
                nid=1001,
                is_blue=False,
                name='A',
                children=[
                    NavCategory(hid=2000, nid=2001, is_blue=True, name='AA'),
                    NavCategory(hid=3000, nid=3001, is_blue=True, name='AB'),
                ],
            ),
            NavCategory(
                hid=4000,
                nid=4001,
                is_blue=True,
                name='B',
                children=[
                    NavCategory(
                        hid=5000,
                        nid=5001,
                        is_blue=False,
                        name='BA',
                        children=[
                            NavCategory(
                                hid=6000,
                                nid=6001,
                                is_blue=True,
                                name='BAA',
                                children=[
                                    NavCategory(
                                        nid=6500,
                                        name='virtual',
                                        children=[
                                            NavCategory(hid=7000, nid=7001, is_blue=False, name='BAAA'),
                                            NavCategory(hid=9000, nid=9001, is_blue=False, primary=True, name='BAAB'),
                                            NavCategory(
                                                hid=10000,
                                                nid=10001,
                                                is_blue=False,
                                                name='BAAC',
                                                children=[
                                                    NavCategory(
                                                        hid=9000, nid=9003, is_blue=True, primary=False, name='BAACA'
                                                    ),
                                                ],
                                            ),
                                            NavCategory(
                                                hid=11000,
                                                nid=11001,
                                                is_blue=True,
                                                name='BAAD',
                                                children=[
                                                    NavCategory(hid=12000, nid=12001, is_blue=False, name='BAADA'),
                                                ],
                                            ),
                                            NavCategory(
                                                nid=13001,
                                                is_blue=True,
                                                name='BAAE',
                                                children=[
                                                    NavCategory(hid=13000, nid=13002, is_blue=False, name='BAAEA'),
                                                ],
                                            ),
                                            NavCategory(
                                                nid=14001, name='BAAF', children=[NavCategory(nid=14002, name='BAAFA')]
                                            ),
                                        ],
                                    ),
                                ],
                                tags=['tag1', 'tag2'],
                            ),
                        ],
                    ),
                    NavCategory(hid=8000, nid=8001, is_blue=False, name='BB'),
                    NavCategory(hid=9000, nid=9002, is_blue=True, primary=False, name='BC'),
                ],
            ),
            NavCategory(hid=1, nid=1, children=[NavCategory(hid=4, nid=4)]),
            NavCategory(nid=100, children=[NavCategory(nid=101, hid=1)]),
            NavCategory(
                nid=90829,
                hid=90829,
                is_blue=True,
                name="Books",
                children=[NavCategory(nid=90831, hid=90831, is_blue=True, name="Dictionaries")],
            ),
            NavCategory(
                nid=1400011,
                is_blue=True,
                name="LINK_BAAF_VIRTUAL_PARENT",
                children=[
                    NavCategory(nid=140001, name='LINK_BAAF', is_blue=True, link=LinkData("catalog", {"nid": 14001}))
                ],
            ),
            NavCategory(nid=40001, name='LINK_A', is_blue=True, link=LinkData("catalog", {"nid": 4001})),
        ]

        cls.index.navtree += [NavCategory(nid=99001, hid=1000, is_blue=True)]

        cls.index.navtree_blue += [NavCategory(nid=99001, hid=9000)]
        cls.settings.blue_market_free_delivery_threshold = 55
        cls.settings.blue_market_prime_free_delivery_threshold = 53
        cls.settings.blue_market_yandex_plus_free_delivery_threshold = 52
        cls.index.blue_delivery_modifiers.set_default_modifier(
            tariffs=[BlueDeliveryTariff(user_price=99)], ya_plus_threshold=52
        )

        cls.index.outlets += [
            Outlet(
                point_id=2001,
                delivery_service_id=103,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(shipper_id=103, day_from=1, day_to=1, order_before=2, price=100),
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(37.12, 55.32),
            ),
            Outlet(
                point_id=2002,
                fesh=1,
                region=213,
                point_type=Outlet.FOR_STORE,
                delivery_option=OutletDeliveryOption(shipper_id=103, day_from=1, day_to=1, order_before=1, price=100),
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(38.12, 54.67),
            ),
            Outlet(
                point_id=2003,
                delivery_service_id=103,
                region=213,
                point_type=Outlet.FOR_POST,
                delivery_option=OutletDeliveryOption(
                    shipper_id=103, day_from=4, day_to=6, order_before=2, work_in_holiday=False, price=100
                ),
                working_days=[i for i in range(7)],
                gps_coord=GpsCoord(37.12, 55.33),
            ),
            Outlet(
                point_id=2004,
                delivery_service_id=103,
                region=213,
                point_type=Outlet.FOR_POST_TERM,
                delivery_option=OutletDeliveryOption(shipper_id=103, day_from=2, day_to=2, order_before=1, price=100),
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(36.13, 55.45),
            ),
            Outlet(
                point_id=2005,
                fesh=3,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(day_from=1, day_to=1, order_before=1, price=50),
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(36.14, 55.43),
            ),
            Outlet(
                point_id=2006,
                fesh=5,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(day_from=1, day_to=1, order_before=1, price=50),
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(36.17, 55.42),
            ),
            Outlet(
                point_id=2007,
                fesh=5,
                region=213,
                point_type=Outlet.FOR_STORE,
                delivery_option=OutletDeliveryOption(day_from=1, day_to=1, order_before=1, price=50),
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(36.19, 55.4),
            ),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5001,
                fesh=1,
                carriers=[103],
                options=[
                    PickupOption(outlet_id=2004, day_from=2, day_to=2, price=100),
                    PickupOption(outlet_id=2002, day_from=1, day_to=1, price=100),
                    PickupOption(outlet_id=2001, day_from=1, day_to=1, price=100),
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5002,
                fesh=5,
                carriers=[99],
                options=[PickupOption(outlet_id=2006), PickupOption(outlet_id=2007)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5003,
                fesh=1,
                carriers=[103],
                options=[
                    PickupOption(outlet_id=2002, day_from=1, day_to=1, price=100),
                    PickupOption(outlet_id=2001, day_from=1, day_to=1, price=100),
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5004,
                fesh=1,
                carriers=[103],
                options=[PickupOption(outlet_id=2003, day_from=2, day_to=2, price=100)],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
        ]

        cls.index.delivery_calc_feed_info += [
            DeliveryCalcFeedInfo(feed_id=1, pickupBuckets=[5001]),
            DeliveryCalcFeedInfo(feed_id=5, pickupBuckets=[5002]),
        ]

        def delivery_service_region_to_region_info():
            return DeliveryServiceRegionToRegionInfo(region_from=213, region_to=225, days_key=1)

        cls.settings.lms_autogenerate = False
        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=145, home_region=213, holidays_days_set_key=2),
            DynamicWarehouseInfo(id=147, home_region=39, holidays_days_set_key=2),
            DynamicWarehouseInfo(id=555, home_region=213),
            DynamicWarehouseInfo(id=666, home_region=213),
            DynamicWarehouseInfo(id=777, home_region=213),
            DynamicWarehouseInfo(id=1213, home_region=213),
            DynamicWarehouseInfo(id=220, home_region=213),
            DynamicDeliveryServiceInfo(
                99, "self-delivery", region_to_region_info=[delivery_service_region_to_region_info()]
            ),
            DynamicDeliveryServiceInfo(103, "c_103", region_to_region_info=[delivery_service_region_to_region_info()]),
            DynamicDeliveryServiceInfo(157, "c_157", region_to_region_info=[delivery_service_region_to_region_info()]),
            DynamicDeliveryServiceInfo(158, "c_158", region_to_region_info=[delivery_service_region_to_region_info()]),
            DynamicDeliveryServiceInfo(
                147147,
                "c_147147",
                region_to_region_info=[
                    DeliveryServiceRegionToRegionInfo(region_from=39, region_to=26, days_key=1),
                    DeliveryServiceRegionToRegionInfo(region_from=39, region_to=456, days_key=1),
                ],
            ),
            DynamicDeliveryServiceInfo(
                165, "dropship_delivery", region_to_region_info=[delivery_service_region_to_region_info()]
            ),
            DynamicDeliveryServiceInfo(
                163,
                "books_delivery",
                region_to_region_info=[DeliveryServiceRegionToRegionInfo(region_from=200, region_to=213, days_key=1)],
            ),
            DynamicDaysSet(key=1, days=[]),
            DynamicDaysSet(key=2, days=[0, 1, 2, 5, 6, 14, 20, 21, 27, 28]),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=145,
                delivery_service_id=103,
                operation_time=0,
                date_switch_time_infos=[date_switch_time_info(2)],
            ),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=220,
                delivery_service_id=163,
                operation_time=0,
                date_switch_time_infos=[date_switch_time_info(2)],
            ),
            warehouse145_delivery_service_157,
            warehouse145_delivery_service_158,
            warehouse147_delivery_service_147147,
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=555, delivery_service_id=99, date_switch_time_infos=[date_switch_time_info(22)]
            ),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=666, delivery_service_id=165, date_switch_time_infos=[date_switch_time_info(21)]
            ),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=777, delivery_service_id=99, date_switch_time_infos=[date_switch_time_info(22)]
            ),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=777, delivery_service_id=165, date_switch_time_infos=[date_switch_time_info(21)]
            ),
            DynamicWarehouseToWarehouseInfo(warehouse_from=145, warehouse_to=145),
            DynamicWarehouseToWarehouseInfo(warehouse_from=147, warehouse_to=147),
            DynamicWarehouseToWarehouseInfo(warehouse_from=555, warehouse_to=555),
            DynamicWarehouseToWarehouseInfo(warehouse_from=666, warehouse_to=666),
            DynamicWarehouseToWarehouseInfo(warehouse_from=777, warehouse_to=777),
            DynamicWarehouseToWarehouseInfo(warehouse_from=220, warehouse_to=220),
            DynamicWarehouseToWarehouseInfo(
                warehouse_from=1213,
                warehouse_to=145,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=18, region_to=225)],
                inbound_time=TimeInfo(1, 30),
                transfer_time=TimeInfo(4),
                operation_time=1,
            ),
        ]
        cls.index.warehouse_priorities += [
            WarehousesPriorityInRegion(regions=[225], warehouse_with_priority=[WarehouseWithPriority(220, 100)])
        ]

        cls.index.shops += [
            _Shops.blue_virtual_shop,
            _Shops.red_virtual_shop,
            _Shops.green_shop,
            _Shops.book_shop,
            _Shops.blue_shop_1,
            _Shops.blue_shop_1470,
            _Shops.blue_shop_2,
            _Shops.golden_partner,
            _Shops.dropship_shop,
            _Shops.dropship_with_delivery_shop,
            _Shops.dsbs_shop,
        ]

        cls.index.shop_operational_rating += [
            ShopOperationalRating(
                shop_id=4,
                late_ship_rate=0.0019,
                cancellation_rate=0.0027,
                return_rate=0.001,
                total=0,
            ),
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
                        201,
                        3,
                        ImagePickerData(
                            url='//avatars.mds.yandex.net/get-mpic/466729/img_model1_201_3/orig',
                            namespace="get-mpic",
                            group_id='466729',
                            image_name='img_model1_201_3',
                        ),
                    ),
                    ParameterValue(
                        201,
                        2,
                        ImagePickerData(
                            url='//avatars.mds.yandex.net/get-mpic/466729/img_model1_201_2/orig',
                            namespace="get-mpic",
                            group_id='466729',
                            image_name='img_model1_201_2',
                        ),
                    ),
                ],
            ),
            Model(hyperid=2, hid=1, title='blue only model'),
            Model(hyperid=3, hid=1, title='not blue model'),
            Model(hyperid=5, hid=2, title='Model for jump table test'),
            Model(hyperid=123456, hid=MARKET_SUBSCRIPTIONS_CATEG_ID, title='Prime'),
            Model(hyperid=90829, hid=90829, title='Books'),
            Model(hyperid=90831, hid=90831, title='Dictionaries'),
            Model(hyperid=4242, hid=4242, title='тест at-beru-warehouse'),
        ]

        cls.index.models += [
            Model(hyperid=2000, hid=2000),
            Model(hyperid=3000, hid=3000),
            Model(hyperid=4000, hid=4000),
            Model(hyperid=5000, hid=5000),
            Model(hyperid=6000, hid=6000),
            Model(hyperid=7000, hid=7000),
            Model(hyperid=8000, hid=8000),
            Model(hyperid=9000, hid=9000),
            Model(hyperid=10000, hid=10000),
            Model(hyperid=11000, hid=11000),
            Model(hyperid=12000, hid=12000),
            Model(hyperid=13000, hid=13000),
            Model(hyperid=14000, hid=15002),
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
                gltype=GLType.ENUM,
                subtype='image_picker',
                model_filter_index=0,
                values=[
                    GLValue(
                        1,
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
                        2,
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
            GLType(param_id=202, hid=1, cluster_filter=True, model_filter_index=1, gltype=GLType.ENUM),
            GLType(param_id=203, hid=1, cluster_filter=True, model_filter_index=2, gltype=GLType.BOOL, hasboolno=False),
            GLType(param_id=204, hid=1, cluster_filter=True, model_filter_index=3, gltype=GLType.BOOL, hasboolno=True),
            GLType(param_id=205, hid=1, cluster_filter=True, model_filter_index=4, gltype=GLType.NUMERIC),
            # Фильтры, к которым не прикреплены оферы. У такого фильтра initialFound равен 0. Он должен быть скрыт с выдачи на синем прайме
            GLType(param_id=301, hid=1, cluster_filter=True, model_filter_index=5, gltype=GLType.ENUM, values=[1, 2]),
            # Фильтры для проверки неточного перехода
            GLType(param_id=201, hid=2, cluster_filter=True, model_filter_index=6, gltype=GLType.ENUM),
            GLType(param_id=205, hid=2, cluster_filter=True, model_filter_index=7, gltype=GLType.NUMERIC),
            GLType(param_id=204, hid=2, cluster_filter=True, model_filter_index=8, gltype=GLType.BOOL, hasboolno=True),
        ]

        cls.index.mskus += [
            MarketSku(
                title="blue offer sku1 john",
                hyperid=1,
                sku=1,
                waremd5='Sku1-wdDXWsIiLVm1goleg',
                blue_offers=[_Offers.sku1_offer1, _Offers.sku1_offer2],
                glparams=[
                    GLParam(param_id=201, value=1),
                    GLParam(param_id=202, value=1),
                    GLParam(param_id=205, value=1),
                ],
                pickup_buckets=[5001],
                post_buckets=[5004],
                randx=1,
            ),
            MarketSku(
                title="blue offer sku2",
                hyperid=1,
                sku=20,
                waremd5='Sku2-wdDXWsIiLVm1goleg',
                blue_offers=[
                    _Offers.sku2_offer1,
                    _Offers.sku2_offer2,
                    _Offers.sku2_offer3,
                    _Offers.sku2_offer4,
                    _Offers.sku2_offer5,
                ],
                glparams=[
                    GLParam(param_id=201, value=2),
                    GLParam(param_id=202, value=1),
                    GLParam(param_id=205, value=1),
                ],
                delivery_buckets=[801],
                pickup_buckets=[5001],
                randx=2,
            ),
            MarketSku(
                title="blue offer sku3",
                hyperid=1,
                sku=3,
                waremd5='Sku3-wdDXWsIiLVm1goleg',
                glparams=[
                    GLParam(param_id=201, value=3),
                    GLParam(param_id=202, value=2),
                    GLParam(param_id=205, value=3),
                ],
                randx=3,
            ),
            MarketSku(
                title="blue offer sku4 ringo",
                hyperid=2,
                sku=4,
                waremd5='Sku4-wdDXWsIiLVm1goleg',
                blue_offers=[_Offers.sku4_offer1],
                glparams=[
                    GLParam(param_id=201, value=1),
                    GLParam(param_id=202, value=1),
                    GLParam(param_id=205, value=1),
                ],
                randx=4,
            ),
            MarketSku(
                hyperid=4,
                sku=5,
                waremd5='Sku5-wdDXWsIiLVm1goleg',
                blue_offers=[_Offers.sku5_offer1, _Offers.sku5_offer2],
                randx=5,
            ),
            MarketSku(
                hyperid=5,
                sku=6,
                waremd5='Sku6-wdDXWsIiLVm1goleg',
                blue_offers=[_Offers.sku6_offer1, _Offers.sku6_offer2],
                randx=6,
                glparams=[
                    GLParam(param_id=201, value=1),
                    GLParam(param_id=205, value=1),
                ],
            ),
            MarketSku(
                hyperid=5,
                sku=7,
                waremd5='Sku7-wdDXWsIiLVm1goleg',
                blue_offers=[_Offers.sku7_offer1],
                randx=7,
                glparams=[
                    GLParam(param_id=201, value=2),
                    GLParam(param_id=205, value=1),
                    GLParam(param_id=204, value=1),
                ],
            ),
            MarketSku(
                hyperid=5,
                sku=8,
                waremd5='Sku8-wdDXWsIiLVm1goleg',
                blue_offers=[_Offers.sku8_offer1, _Offers.sku8_offer2, _Offers.sku8_offer3],
                randx=8,
                glparams=[
                    GLParam(param_id=201, value=2),
                    GLParam(param_id=205, value=2),
                    GLParam(param_id=204, value=0),
                ],
            ),
            MarketSku(
                hyperid=5,
                sku=9,
                waremd5='Sku9-wdDXWsIiLVm1goleg',
                blue_offers=[],
                randx=9,
            ),
            MarketSku(
                hyperid=5,
                sku=10,
                waremd5='Sku10-dDXWsIiLVm1goleg',
                blue_offers=[],
                randx=10,
            ),
            MarketSku(
                hyperid=5,
                sku=11,
                waremd5='Sku11-dDXWsIiLVm1goleg',
                blue_offers=[],
                randx=11,
            ),
            MarketSku(
                title="prime",
                hyperid=123456,
                sku=123456,
                waremd5='8rXbTj04e6jzGqjhE3IpdA',
                blue_offers=[_Offers.prime_offer],
                randx=12,
            ),
            MarketSku(
                title="P1SKU",
                hyperid=5,
                sku=789,
                waremd5='8rXbTj04e6jzGqjhE3SKUA',
                blue_offers=[
                    _Offers.p_one_offer,
                    _Offers.p_one_offer_md,
                ],
                randx=12,
            ),
            MarketSku(
                title='RusEngDict',
                sku=908,
                waremd5='RusEngDict_IiLVm1goleg',
                hyperid=90831,
                blue_offers=[_Offers.rus_eng_dict_offer],
                delivery_buckets=[813],
                randx=13,
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                title="cat",
                hyperid=2000,
                sku=12000,
                blue_offers=[_Offers.cat_offer2],
                randx=200,
            ),
            MarketSku(
                title="cat",
                hyperid=3000,
                sku=13000,
                blue_offers=[_Offers.cat_offer3],
                randx=300,
            ),
            MarketSku(
                title="cat",
                hyperid=4000,
                sku=14000,
                blue_offers=[_Offers.cat_offer4],
                randx=400,
            ),
            MarketSku(
                title="cat",
                hyperid=5000,
                sku=15000,
                blue_offers=[_Offers.cat_offer5],
                randx=500,
            ),
            MarketSku(
                title="cat",
                hyperid=6000,
                sku=16000,
                blue_offers=[_Offers.cat_offer6],
                randx=6000,
                picture=pic,
            ),
            MarketSku(
                title="cat",
                hyperid=6000,
                sku=16001,
                blue_offers=[_Offers.cat_offer6_1],
                randx=500,
                picture=pic2,
            ),
            MarketSku(
                title="cat",
                hyperid=7000,
                sku=17000,
                blue_offers=[_Offers.cat_offer7],
                randx=700,
                picture=pic2,
                pickup_buckets=[5002],
            ),
            MarketSku(
                title="cat", hyperid=8000, sku=18000, blue_offers=[_Offers.cat_offer8], randx=800, pickup_buckets=[5003]
            ),
            MarketSku(
                title="cat",
                hyperid=9000,
                sku=19000,
                blue_offers=[_Offers.cat_offer9],
                randx=900,
            ),
            MarketSku(
                title="cat",
                hyperid=10000,
                sku=110000,
                blue_offers=[_Offers.cat_offer10],
                randx=1000,
                delivery_buckets=[804],
                pickup_buckets=[5003],
            ),
            MarketSku(
                title="cat",
                hyperid=11000,
                sku=111000,
                blue_offers=[_Offers.cat_offer11],
                randx=1100,
            ),
            MarketSku(
                title="cat",
                hyperid=12000,
                sku=112000,
                blue_offers=[_Offers.cat_offer12],
                randx=1200,
            ),
            MarketSku(title="dog", hyperid=13000, sku=113000, blue_offers=[_Offers.dog_offer13], randx=1300),
            MarketSku(
                title="stomach_pills_msku",
                hid=DRUGS2_CATEG_ID,
                hyperid=23000,
                sku=223000,
                blue_offers=[_Offers.pills_offer, _Offers.pills_offer_ff],
                pickup_buckets=[5002],
                randx=2300,
            ),
            MarketSku(
                title="stomach_pills_msku_ff2",
                hid=DRUGS2_CATEG_ID,
                hyperid=23000,
                sku=223001,
                blue_offers=[_Offers.pills_offer_ff2],
                pickup_buckets=[5003],
                randx=2300,
            ),
            # TODO: выпилить здесь: https://st.yandex-team.ru/MARKETOUT-33898
            MarketSku(
                title="painkiller_msku",
                hid=PAINKILLERS_CATEG_ID,
                hyperid=23500,
                sku=223500,
                blue_offers=[_Offers.painkiller_offer],
                pickup_buckets=[5002],
                randx=2300,
            ),
            MarketSku(
                title="painkiller_msku_ff2",
                hid=PAINKILLERS_CATEG_ID,
                hyperid=23500,
                sku=223501,
                blue_offers=[_Offers.painkiller_offer_ff2],
                pickup_buckets=[5003],
                randx=2300,
            ),
            MarketSku(
                title="vitamin_Z_msku",
                hid=VITAMINS_AND_MINERALS_CATEG_ID,
                hyperid=23502,
                sku=223502,
                blue_offers=[_Offers.vitamin_Z_offer],
                pickup_buckets=[5002],
                randx=2300,
            ),
            MarketSku(
                title="vitamin_Z_msku_ff2",
                hid=VITAMINS_AND_MINERALS_CATEG_ID,
                hyperid=23502,
                sku=223503,
                blue_offers=[_Offers.vitamin_Z_offer_ff2],
                pickup_buckets=[5003],
                randx=2300,
            ),
            MarketSku(
                title="germkiller_msku",
                hid=DISINFECTANTS_CATEG_ID,
                hyperid=23504,
                sku=223504,
                blue_offers=[_Offers.germkiller_offer],
                pickup_buckets=[5002],
                randx=2300,
            ),
            MarketSku(
                title="germkiller_msku_ff2",
                hid=DISINFECTANTS_CATEG_ID,
                hyperid=23504,
                sku=223505,
                blue_offers=[_Offers.germkiller_offer_ff2],
                pickup_buckets=[5003],
                randx=2300,
            ),
            # TODO: выпилить ^^^^^^
            MarketSku(
                title="fridge_msku",
                hyperid=24000,
                sku=224000,
                blue_offers=[_Offers.fridge_offer],
                randx=2400,
                delivery_buckets=[803],
            ),
            MarketSku(
                title="bird_msku",
                hyperid=25000,
                sku=225000,
                blue_offers=[_Offers.all_delivery_offer],
                randx=2500,
                delivery_buckets=[805, 806],
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                title="order_before",
                hyperid=15000,
                sku=15,
                blue_offers=[_Offers.sku15_offer1],
                randx=3600,
                delivery_buckets=[804, 807],
            )
        ]

        cls.index.mskus += [
            MarketSku(
                title="dsbs msku",
                hyperid=15000,
                sku=9116,
            )
        ]

        cls.index.regiontree += [
            Region(
                rid=213,
                children=[
                    Region(rid=123, children=[Region(rid=200, children=[Region(rid=100)])]),
                    Region(rid=234),
                    Region(rid=345),
                    Region(rid=456),
                    Region(
                        rid=26,
                        children=[
                            Region(
                                rid=977,
                                children=[
                                    Region(rid=121220),
                                ],
                            ),
                            Region(rid=39),
                        ],
                    ),
                    Region(rid=11309),
                    Region(rid=40),
                    Region(rid=2),
                    Region(rid=24),
                    Region(rid=969),
                    Region(rid=10867),
                    Region(rid=10876),
                    Region(rid=10870),
                    Region(rid=18),
                    Region(rid=14),
                    Region(rid=6),
                    Region(rid=192),
                    Region(rid=11),
                    Region(rid=15),
                ],
            ),
        ]

        cls.index.allowed_regions_for_books += [200]

        cls.index.hypertree += [HyperCategory(hid=15001, children=[HyperCategory(hid=15002)])]

        std_options = [RegionalDelivery(rid=213, options=[DeliveryOption(price=5, day_from=1, day_to=2)])]
        cls.index.delivery_buckets += [
            DeliveryBucket(bucket_id=801, fesh=1, carriers=[157], regional_options=std_options),
            DeliveryBucket(
                bucket_id=803,
                fesh=6,
                carriers=[165],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=std_options,
            ),
            DeliveryBucket(
                bucket_id=804,
                fesh=1,
                carriers=[157],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=std_options,
            ),
            DeliveryBucket(
                bucket_id=805,
                fesh=7,
                carriers=[99],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=std_options,
            ),
            DeliveryBucket(
                bucket_id=806,
                fesh=7,
                carriers=[165],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=std_options,
            ),
            DeliveryBucket(
                bucket_id=807,
                fesh=1,
                carriers=[158],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=std_options,
            ),
            DeliveryBucket(
                bucket_id=808,
                fesh=4,
                carriers=[157],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=std_options,
            ),
            DeliveryBucket(
                bucket_id=800147,
                fesh=1470,
                carriers=[147147],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=std_options,
            ),
            DeliveryBucket(
                bucket_id=811,
                fesh=10,
                carriers=[161],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[
                    RegionalDelivery(
                        rid=213,
                        options=[DeliveryOption(price=5, day_from=1, day_to=2)],
                        payment_methods=[Payment.PT_YANDEX, Payment.PT_CARD_ON_DELIVERY],
                    )
                ],
            ),
            DeliveryBucket(
                bucket_id=813,
                fesh=577858,
                carriers=[163],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=5, day_from=1, day_to=2)])],
            ),
        ]

        cls.index.offers += [
            Offer(
                title='not blue offer 1_1',
                fesh=2,
                feedid=2,
                price=2,
                hyperid=1,
                waremd5='GreenOffer1_1_gggggggg',
                offerid='white_offer_id',
                randx=100,
            ),
            Offer(title='not blue offer 3_1', fesh=2, price=3, hyperid=3, waremd5='GreenOffer3_1_gggggggg'),
        ]

        cls.index.offers += [
            _Offers.dsbs_offer,
            _Offers.dsbs_with_msku_offer,
            _Offers.white_offer,
        ]

        cls.index.offers += [
            Offer(title='cat', hyperid=2000),
            Offer(title='cat', hyperid=3000),
            Offer(title='cat', hyperid=4000),
            Offer(title='cat', hyperid=5000),
            Offer(title='cat', hyperid=6000),
            Offer(title='cat', hyperid=7000),
            Offer(title='cat', hyperid=8000),
            Offer(title='cat', hyperid=9000),
            Offer(title='cat', hyperid=10000),
            Offer(title='cat', hyperid=11000),
            Offer(title='cat', hyperid=12000),
        ]

        cls.index.navtree += [NavCategory(nid=100041, hid=100041, is_blue=True, name="Redirect")]

        cls.index.navtree_blue += [NavCategory(nid=100041, hid=100041)]

        cls.index.nidsredirector = [
            NidsRedirector(
                "blue",
                [
                    RedirectorRecord(frm=99003, to=99001),  # не существует в дереве
                    RedirectorRecord(frm=9003, to=99001),
                    RedirectorRecord(frm=100041, to=100042),  # не существует в дереве
                ],
            )
        ]

        cls.index.mskus += [
            MarketSku(
                title="reverseredirectmsku",
                hid=100041,
                sku=100041,
                hyperid=100041,
                blue_offers=[BlueOffer(price=1, vat=Vat.VAT_10)],
            )
        ]

    def __check_choose_best_blue_offer(self, request):
        """
        Что проверяем:
         * в рамках одного маркетного СКУ на зеленой выдаче остается только один синий офер - с лучшей ценой
         * маркетное СКУ не выводится на зеленом маркете
         * формат выдачи синего офера:
           * supplier - информация о магазине поставщике товара. В том числе тип поставщика (1P/3P) и СНО
           * urls.direct - ссылка на карточку маркетного СКУ
           * shop.id - идентификатор виртуального магазина
           * shop.feed.id - фид виртуального магазина
           * cpa пессимизируется
        """

        def create_blue_offer(blue_offer, title, sku, supplier, supplier_work_schedule):
            result = {
                "entity": "offer",
                "titles": {"raw": title},
                "wareId": blue_offer.waremd5,
                "marketSku": str(sku),
                "supplierSku": blue_offer.offerid,
                "vat": str(Vat(blue_offer.vat)),
                "supplier": {
                    "id": supplier.fesh,
                    "name": supplier.name,
                    "type": supplier.supplier_type,
                    "taxSystem": str(Tax(supplier.tax_system)),
                    "workSchedule": supplier_work_schedule,
                },
                "realShop": {
                    "id": supplier.fesh,
                    "name": supplier.name,
                },
                "shop": {
                    "id": 1,
                    "feed": {
                        "id": '1',
                        "offerId": '{}.{}'.format(blue_offer.feedid, blue_offer.offerid),
                    },
                    "workSchedule": "virtual shop work schedule",
                },
                "urls": {
                    "direct": LikeUrl(url_path="/product/{}".format(sku), url_params={"offerid": blue_offer.waremd5})
                },
                "cpa": "real",
            }

            if blue_offer.has('price_old'):
                result['prices'] = {
                    'discount': {
                        'oldMin': str(blue_offer.price_old),
                    },
                }

            return result

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    # Зеленый офер первой модели тоже попадает в выдачу
                    {"entity": "offer", "titles": {"raw": "not blue offer 1_1"}, "wareId": "GreenOffer1_1_gggggggg"},
                    # Для второго СКУ был выбран синий офер с ценой 50
                    create_blue_offer(
                        title="blue offer sku2",
                        blue_offer=_Offers.sku2_offer2,
                        sku=20,
                        supplier=_Shops.blue_shop_2,
                        supplier_work_schedule=Absent(),
                    ),
                    # Для первого СКУ был выбран синий офер с ценой 5
                    create_blue_offer(
                        title="blue offer sku1 john",
                        blue_offer=_Offers.sku1_offer1,
                        sku=1,
                        supplier=_Shops.blue_shop_1,
                        supplier_work_schedule="work schedule supplier 3",
                    ),
                ]
            },
            allow_different_len=False,
        )

    def __check_choose_best_blue_offer_uncollapsed(self, request):
        """
        Что проверяем:
         * в рамках одного маркетного СКУ на зеленой выдаче остается синих оферов по числу поставщиков - с лучшей ценой
         * маркетное СКУ не выводится на зеленом маркете
         * формат выдачи синего офера:
           * supplier - информация о магазине поставщике товара. В том числе тип поставщика (1P/3P) и СНО
           * urls.direct - ссылка на карточку маркетного СКУ
           * shop.id - идентификатор виртуального магазина
           * shop.feed.id - фид виртуального магазина
           * cpa пессимизируется
        """

        def create_blue_offer(blue_offer, title, sku, supplier, supplier_work_schedule):
            result = {
                "entity": "offer",
                "titles": {"raw": title},
                "wareId": blue_offer.waremd5,
                "marketSku": str(sku),
                "supplierSku": blue_offer.offerid,
                "vat": str(Vat(blue_offer.vat)),
                "supplier": {
                    "id": supplier.fesh,
                    "name": supplier.name,
                    "type": supplier.supplier_type,
                    "taxSystem": str(Tax(supplier.tax_system)),
                    "workSchedule": supplier_work_schedule,
                },
                "realShop": {
                    "id": supplier.fesh,
                    "name": supplier.name,
                },
                "shop": {
                    "id": 1,
                    "feed": {
                        "id": '1',
                        "offerId": '{}.{}'.format(blue_offer.feedid, blue_offer.offerid),
                    },
                    "workSchedule": "virtual shop work schedule",
                },
                "urls": {
                    "direct": LikeUrl(url_path="/product/{}".format(sku), url_params={"offerid": blue_offer.waremd5})
                },
                "cpa": "real",
            }

            if blue_offer.has('price_old'):
                result['prices'] = {
                    'discount': {
                        'oldMin': str(blue_offer.price_old),
                    },
                }

            return result

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    # Зеленый офер первой модели тоже попадает в выдачу
                    {"entity": "offer", "titles": {"raw": "not blue offer 1_1"}, "wareId": "GreenOffer1_1_gggggggg"},
                    # Для второго СКУ был выбран синий офер с ценой 50 и 52
                    create_blue_offer(
                        title="blue offer sku2",
                        blue_offer=_Offers.sku2_offer2,
                        sku=20,
                        supplier=_Shops.blue_shop_2,
                        supplier_work_schedule=Absent(),
                    ),
                    create_blue_offer(
                        title="blue offer sku2",
                        blue_offer=_Offers.sku2_offer3,
                        sku=20,
                        supplier=_Shops.blue_shop_1,
                        supplier_work_schedule="work schedule supplier 3",
                    ),
                    # Для первого СКУ был выбран синий офер с ценой 5 и 50
                    create_blue_offer(
                        title="blue offer sku1 john",
                        blue_offer=_Offers.sku1_offer2,
                        sku=1,
                        supplier=_Shops.blue_shop_2,
                        supplier_work_schedule=Absent(),
                    ),
                    create_blue_offer(
                        title="blue offer sku1 john",
                        blue_offer=_Offers.sku1_offer1,
                        sku=1,
                        supplier=_Shops.blue_shop_1,
                        supplier_work_schedule="work schedule supplier 3",
                    ),
                ]
            },
            allow_different_len=False,
        )

    def test_choose_best_blue_offer_prime(self):
        """
        Что проверяем: синие оферы на place=prime
        """
        self.__check_choose_best_blue_offer(
            'place=prime&hyperid=1&rids=213&rgb=green_with_blue&base=default.market-exp-prestable.yandex.ru&rearr-factors=market_blue_buybox_max_price_rel_add_diff=0'
        )

    def test_choose_best_blue_offer_prime_filter_discount(self):
        """
        Что проверяем: синие оферы на place=prime со скидочным фильтром
        """
        for filter_cgi in ['&filter-discount-only=1', '&filter-promo-or-discount=1']:
            response = self.report.request_json(
                'place=prime&hyperid=1&rids=213&rgb=green_with_blue&base=default.market-exp-prestable.yandex.ru&rearr-factors=market_blue_buybox_max_price_rel_add_diff=0'
                + filter_cgi
            )
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "entity": "offer",
                            "wareId": "Sku1Price5-IiLVm1Goleg",
                            "prices": {
                                "discount": {
                                    "oldMin": "8",
                                },
                            },
                        },
                    ],
                },
            )

    @skip('https://st.yandex-team.ru/MARKETOUT-35795')
    def test_choose_best_blue_offer_miprime(self):
        """
        Что проверяем: синие оферы на place=miprime
        """
        self.__check_choose_best_blue_offer(
            'place=miprime&hyperid=1&rids=213&rgb=green_with_blue&base=default.market-exp-prestable.yandex.ru'
        )

    def test_choose_best_blue_offer_productoffers(self):
        """
        Что проверяем: синие оферы на place=productoffers
        """
        self.__check_choose_best_blue_offer_uncollapsed(
            'place=productoffers&hyperid=1&rids=213&rgb=green_with_blue&base=default.market-exp-prestable.yandex.ru&rearr-factors=market_uncollapse_supplier=1;market_blue_buybox_max_price_rel_add_diff=0'  # noqa
        )

    def test_productoffers_blue_only(self):
        '''
        Проверяем выдачу только синих документов при rgb=blue
        '''

        blue_offers = [
            {"wareId": "Sku2Price50-iLVm1Goleg", "offerColor": "blue"},
            {"wareId": "Sku2Price52-iLVm1Goleg", "offerColor": "blue"},
            {"wareId": "Sku1Price50-iLVm1Goleg", "offerColor": "blue"},
            {"wareId": "Sku1Price5-IiLVm1Goleg", "offerColor": "blue"},
        ]

        response = self.report.request_json(
            "place=productoffers&hyperid=1&rids=213&rgb=blue&rearr-factors=market_uncollapse_supplier=1;market_blue_buybox_max_price_rel_add_diff=0"
        )
        self.assertFragmentIn(response, blue_offers, allow_different_len=False)

        # Если это запрос в белый маркет, то показываются белые и синие оферы
        white_offers = [{"wareId": "GreenOffer1_1_gggggggg", "offerColor": "white"}]
        response = self.report.request_json(
            "place=productoffers&hyperid=1&rids=213&rearr-factors=market_uncollapse_supplier=1;market_blue_buybox_max_price_rel_add_diff=0"
        )
        self.assertFragmentIn(response, blue_offers + white_offers, allow_different_len=False)

    def __check_blue_offer_info(
        self,
        ware_md5,
        msku,
        supplier_id,
        supplier_type,
        rgb_request='GREEN_WITH_BLUE',
        rgb_compare='GREEN',
        cpa=False,
        price_old=None,
        subscription=False,
    ):
        """
        Что проверяем: place=offerinfo показывает все оферы, даже, если они не лучшие среди СКУ
        Проверяется, что для записи в show.log выставляется цвет
        """
        response = self.report.request_json(
            'place=offerinfo&offerid={}&rids=213&show-urls=external,cpa&regset=1&rgb={}'.format(ware_md5, rgb_request)
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "wareId": ware_md5,
                        "cpa": 'real' if cpa else Absent(),
                        "prices": {
                            "discount": {"oldMin": price_old} if price_old else Absent(),
                        },
                        "subscription": {
                            "name": "prime",
                        }
                        if subscription
                        else Absent(),
                    },
                ]
            },
        )

        if cpa:
            self.show_log.expect(
                ware_md5=ware_md5, rgb=rgb_compare, msku=msku, supplier_id=supplier_id, supplier_type=supplier_type
            ).times(2)
            self.click_log.expect(
                ware_md5=ware_md5,
                rgb=rgb_compare,
                msku=msku,
                supplier_id=supplier_id,
                supplier_type=supplier_type,
                clicktype=ClickType.CPA,
            ).once()
        else:
            self.show_log.expect(
                ware_md5=ware_md5, rgb=rgb_compare, msku=msku, supplier_id=supplier_id, supplier_type=supplier_type
            ).once()
            self.click_log.expect(
                ware_md5=ware_md5,
                rgb=rgb_compare,
                msku=msku,
                supplier_id=supplier_id,
                supplier_type=supplier_type,
                clicktype=ClickType.EXTERNAL,
            ).once()

    def test_choose_best_blue_offer_offerinfo(self):
        """
        Что проверяем: синие оферы на place=offerinfo
        """
        self.__check_blue_offer_info(
            'Sku1Price5-IiLVm1Goleg', msku=1, supplier_id=3, supplier_type=Shop.FIRST_PARTY, cpa=True, price_old='8'
        )
        self.__check_blue_offer_info(
            'Sku1Price50-iLVm1Goleg', msku=1, supplier_id=4, supplier_type=Shop.THIRD_PARTY, cpa=True
        )
        self.__check_blue_offer_info(
            'Sku2Price55-iLVm1Goleg', msku=20, supplier_id=3, supplier_type=Shop.FIRST_PARTY, cpa=True
        )
        self.__check_blue_offer_info(
            'Sku2Price50-iLVm1Goleg', msku=20, supplier_id=4, supplier_type=Shop.THIRD_PARTY, cpa=True
        )

    def test_blue_subscription(self):
        """
        Что проверяем: подписки на place=offerinfo
        """
        self.__check_blue_offer_info(
            'x2uPN3XNsizR0Kt2DeS6MQ',
            msku='123456',
            supplier_id=3,
            supplier_type=Shop.FIRST_PARTY,
            subscription=True,
            cpa=True,
        )

        response = self.report.request_json('place=sku_offers&market-sku=123456&rgb=blue')
        self.assertFragmentIn(response, 'prime.offer')

        response = self.report.request_json('place=prime&text=prime&rgb=blue&debug=1')
        self.assertFragmentNotIn(response, 'prime.offer')
        self.assertFragmentIn(response, {"filters": {"OFFER_IN_SUBSCRIPTION_CATEGORY": 1}})

    def test_blue_offerinfo_and_show_log(self):
        """
        Что проверяем: поведение offerinfo на синем прайме и записи в журнале показов
        """
        self.__check_blue_offer_info(
            'Sku1Price5-IiLVm1Goleg',
            rgb_request='BLUE',
            rgb_compare='BLUE',
            cpa='real',
            msku=1,
            supplier_id=3,
            supplier_type=Shop.FIRST_PARTY,
            price_old='8',
        )

    def test_prime_super_uid_in_show_log(self):
        """
        Проверяем связь показов офера и модели через super_id
        Для модели показывается ware_md5 его ДО и super_uid - show_uid этого же офера
        """
        show_uid = '048841920011177788888{}001'
        offer_show_uid = show_uid.format('06')
        model_show_uid = show_uid.format('16')

        self.report.request_json(
            'place=prime&rgb=blue&hyperid=1&show-urls=external,cpa&rearr-factors=market_blue_buybox_max_price_rel_add_diff=0'
        )

        self.show_log.expect(
            ware_md5='Sku2Price50-iLVm1Goleg', show_uid=offer_show_uid, super_uid=offer_show_uid, supplier_id=4
        )  # offer
        self.show_log.expect(
            ware_md5='Sku2Price50-iLVm1Goleg', show_uid=model_show_uid, super_uid=offer_show_uid
        )  # model

    def test_choose_best_blue_offer_offerinfo_batch(self):
        """
        Что проверяем: выводятся все синие оферы на place=offerinfo, даже если они в одном СКУ
        """
        response = self.report.request_json(
            'place=offerinfo&offerid=Sku1Price5-IiLVm1Goleg,Sku1Price50-iLVm1Goleg,Sku2Price55-iLVm1Goleg&rids=213&show-urls=cpa&regset=1&rgb=green_with_blue'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "offer", "wareId": 'Sku1Price5-IiLVm1Goleg'},
                    {"entity": "offer", "wareId": 'Sku1Price50-iLVm1Goleg'},
                    {"entity": "offer", "wareId": 'Sku2Price55-iLVm1Goleg'},
                ]
            },
            allow_different_len=False,
        )

    @staticmethod
    def _create_model_item(model_id, offer_ware_id, offers_count, min_model_price, max_model_price):
        return {
            "entity": "product",
            "id": model_id,
            "offers": {
                "count": offers_count,
                "items": [
                    {
                        "wareId": offer_ware_id,
                        "cpa": "real",
                    }
                ],
            },
            "prices": {
                "min": min_model_price,
                "max": max_model_price,
            },
        }

    def test_blue_prime_sku_statistics(self):
        """
        Что проверяем: вывод статистики СКУ по моделям.
        Общее количество СКУ, привязанных к модели (пока что равно СКУ в продаже)
        Количество СКУ в продаже
        Количество СКУ, прошедших фильтр
        """

        def product_answer(id, total, before, after):
            return {
                "entity": "product",
                "id": id,
                "skuStats": {
                    "totalCount": total,
                    "beforeFiltersCount": before,
                    "afterFiltersCount": after,
                },
            }

        request = "place=prime&text=blue&allow-collapsing=1&use-default-offers=1&rgb=BLUE&rids=213"

        # Запрос без фильтров
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    product_answer(id=1, total=2, before=2, after=2),
                    product_answer(id=2, total=1, before=1, after=1),
                ]
            },
            allow_different_len=False,
        )

        # Добавляем к запросу фильтр
        response = self.report.request_json(
            request + "&nid=1&glfilter=201:1&rearr-factors=market_early_pre_early_gl_filtering=0"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    product_answer(id=1, total=2, before=2, after=1),
                    product_answer(id=2, total=1, before=1, after=1),
                ]
            },
            allow_different_len=False,
        )

        # Добавляем к запросу фильтр
        # если фильтрация происходит на этапе AcceptDocWithHits то beforeFiltersCount и totalCount не будет включать офферы не подходящие под фильтры
        # вот то что totalCount меняется не очень хорошо, т.к. это скорее всего "Еще N вариантов"
        response = self.report.request_json(
            request + "&nid=1&glfilter=201:1&rearr-factors=market_early_pre_early_gl_filtering=1"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    product_answer(id=1, total=1, before=1, after=1),
                    product_answer(id=2, total=1, before=1, after=1),
                ]
            },
            allow_different_len=False,
        )

        # Скрываем динамиком один из оферов СКУ 1, но он продолжает быть на выдаче
        self.dynamic.disabled_sku_offers += [
            DynamicSkuOffer(shop_id=3, sku=_Offers.sku1_offer1.offerid),
        ]
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    product_answer(1, 2, 2, 2),
                    product_answer(2, 1, 1, 1),
                ]
            },
            allow_different_len=False,
        )

        # Скрываем динамиком второй офер СКУ 1 - он пропадает из выдачи
        self.dynamic.disabled_sku_offers += [
            DynamicSkuOffer(shop_id=4, sku=_Offers.sku1_offer2.offerid),
        ]
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    product_answer(1, 1, 1, 1),
                    product_answer(2, 1, 1, 1),
                ]
            },
            allow_different_len=False,
        )

    def _check_blue_prime(self, flag_value, item1, item2, use_rearr=False, how=None, supplier_id=None):
        """
        Проверка выдачи прайма на синем маркете.
        Выводятся модели и к модели прикрепляется дефолтный офер (он обязательно только синий)
        Дефолтный офер не пессимизируется до cpc
        """

        request = 'place=prime&text=blue&allow-collapsing=1&use-default-offers=1&rids=213&disable-rotation-on-blue=1&debug=da&onstock=da&rearr-factors=market_blue_buybox_max_price_rel_add_diff=0'
        request += ('&rearr-factors=market_rgb_type={}' if use_rearr else '&rgb={}').format(flag_value)
        if supplier_id:
            request += '&supplier-id={}'.format(supplier_id)
        if how:
            request += '&how={}'.format(how)
        response = self.report.request_json(request)
        # Третьей модели нет, т.к. в ней нет синих оферов
        self.assertFragmentIn(response, {"results": [item1, item2]}, allow_different_len=False, preserve_order=False)

        # Проверяем, что вызывается пересортировка для синего маркета
        self.assertFragmentIn(response, {"logicTrace": [Contains("rearrangeDocsOnBlueMarket")]})

    def test_at_beru_warehouse_filter(self):
        """
        Тестируем наличие под экспериментальным флагом фильтра at-beru-warehouse c булевыми значениями 0 / 1
        Логика фильтра проверяется на 6 типах оферов:
        1P на складе Беру:    atSupplierWarehouse = False,  ЕСТЬ на выдаче с &at-beru-warehouse=1
        3P на складе Беру:    atSupplierWarehouse = False,  ЕСТЬ на выдаче с &at-beru-warehouse=1
        На складе дропшипа:   atSupplierWarehouse = True,   НЕТ на выдаче  с &at-beru-warehouse=1
        Фарма:                atSupplierWarehouse = True,   НЕТ на выдаче  с &at-beru-warehouse=1
                              atSupplierWarehouse = False,  ЕСТЬ на выдаче с &at-beru-warehouse=1
        """
        request_template = (
            'place=prime&rgb=blue&rids=213&allow-collapsing=0'
            '&hyperid=2&hyperid=11212&hyperid=11213&hyperid=23000&hyperid=24000'
            '&rearr-factors=market_blue_buybox_by_gmv_ue=0;market_blue_buybox_price_rel_max_threshold=1000'
        )
        request_template += USE_DEPRECATED_DIRECT_SHIPPING_FLOW

        for enable_filter in (None, False, True):
            request = request_template
            if enable_filter is not None:
                request += '&rearr-factors=all_beru_stock_filter_enabled={}'.format(1 if enable_filter else 0)
            response = self.report.request_json(request)
            if enable_filter is None or enable_filter:
                self.assertFragmentIn(
                    response,
                    {
                        "filters": [
                            {"id": "glprice"},
                            {
                                "id": "at-beru-warehouse",
                                "type": "boolean",
                                "values": [
                                    {"value": "0", "found": 2},
                                    {"value": "1", "found": 3},
                                ],
                            },
                            {"id": "offer-shipping"},
                        ]
                    },
                    preserve_order=True,
                )
            else:
                self.assertFragmentNotIn(response, {"filters": [{"id": "at-beru-warehouse"}]})

        request = request_template + '&rearr-factors=all_beru_stock_filter_enabled=1'
        response = self.report.request_json(request)

        at_beru_warehouse_offers_count = len(
            filter(lambda result_item: not result_item['atSupplierWarehouse'], response.root['search']['results'])
        )
        self.assertEqual(at_beru_warehouse_offers_count, 3)

        at_supplier_warehouse_offers_count = len(
            filter(lambda result_item: result_item['atSupplierWarehouse'], response.root['search']['results'])
        )
        self.assertEqual(at_supplier_warehouse_offers_count, 2)

        request = request_template + '&rearr-factors=all_beru_stock_filter_enabled=1&at-beru-warehouse=1'
        response = self.report.request_json(request)

        at_beru_warehouse_offers_count = len(
            filter(lambda result_item: not result_item['atSupplierWarehouse'], response.root['search']['results'])
        )
        self.assertEqual(at_beru_warehouse_offers_count, 3)

        at_supplier_warehouse_offers_count = len(
            filter(lambda result_item: result_item['atSupplierWarehouse'], response.root['search']['results'])
        )
        self.assertEqual(at_supplier_warehouse_offers_count, 0)

        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {"id": "glprice"},
                    {
                        "id": "at-beru-warehouse",
                        "type": "boolean",
                        "values": [
                            {"value": "1", "checked": True, "found": 3},
                        ],
                    },
                    {"id": "offer-shipping"},
                ]
            },
            preserve_order=True,
        )

    def test_at_beru_warehouse_filter_dsbs(self):
        # MARKETOUT-40126
        # проверяем что при включенном фильтре at-beru-warehouse из выдачи удаляются модели, т.к. у моделей нет склада
        offer = _Offers.white_offer
        hid = 4242
        offer_check = {
            "results": [
                {
                    "entity": "offer",
                    "wareId": offer.waremd5,
                },
            ]
        }
        model_check = {
            "results": [
                {
                    "entity": "product",
                    "type": "model",
                    "id": hid,
                },
            ]
        }

        for flag in (0, 1):
            request = 'place=prime&at-beru-warehouse={}&hid={}'.format(flag, hid)
            response = self.report.request_json(request)

            if flag:
                # с флагом склада маркета - оффер найтись не должен
                self.assertFragmentNotIn(response, offer_check)
                self.assertFragmentNotIn(response, model_check)
            else:
                # без флага - находится
                self.assertFragmentIn(response, offer_check)
                self.assertFragmentIn(response, model_check)

    def test_offer_shipping_filter(self):
        """
        На синем маркете offer-shipping сейчас не работает потому что там работае skip-delivery-calculation
        На белом маркете и на синем при явном указании calculate-delivery=1 все считается как надо
        """
        request = 'place=prime&rgb=blue&text=blue&rids=213'
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"filters": [{"id": "offer-shipping"}]})

        request = 'place=prime&text=blue&rids=213&calculate-delivery=1'
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"filters": [{"id": "offer-shipping"}]})

        request = 'place=prime&text=blue&rids=213'
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"filters": [{"id": "offer-shipping"}]})

    def test_only_blue_offers_on_prime(self):
        """
        Что проверяем: на синем прайме и дефолтном офере показываются
        только синие оферы. ФФ-оферы не отображаются
        """
        response = self.report.request_json(
            'place=prime&text=blue&rgb=BLUE&rids=213&allow-collapsing=0&rearr-factors=market_blue_buybox_max_price_rel_add_diff=0'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "offer", "wareId": 'Sku2Price50-iLVm1Goleg'},
                    {"entity": "offer", "wareId": 'Sku1Price5-IiLVm1Goleg'},
                    {"entity": "offer", "wareId": 'Sku4Price55-iLVm1Goleg'},
                ]
            },
            allow_different_len=False,
        )

    def test_only_blue_offers_on_default_offer(self):
        """
        Что проверяем: на синем прайме и дефолтном офере показываются
        только синие оферы. ФФ-оферы не отображаются
        """
        request = 'place=defaultoffer&hyperid=1,2,3&rids=213&base=default.market-exp-prestable.yandex.ru'
        response = self.report.request_json(request + '&rgb=BLUE')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "offer", "wareId": 'Sku2Price50-iLVm1Goleg', "model": {"id": 1}},
                    {"entity": "offer", "wareId": 'Sku4Price55-iLVm1Goleg', "model": {"id": 2}},
                    # Третья модель не попадает в выдачу, т.к. у них нет синих оферов
                ]
            },
            allow_different_len=False,
        )

        # Для контроля запрашиваем для зеленого маркета
        response = self.report.request_json(request + '&rgb=green_with_blue')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "offer", "wareId": 'Sku2Price50-iLVm1Goleg', "model": {"id": 1}, "shop": {"id": 1}},
                    {"entity": "offer", "wareId": 'Sku4Price55-iLVm1Goleg', "model": {"id": 2}},
                    {"entity": "offer", "wareId": 'GreenOffer3_1_gggggggg', "model": {"id": 3}, "shop": {"id": 2}},
                ]
            },
            allow_different_len=False,
        )

    def _blue_prime_test_base(self, add_supplier_id=None):
        item1 = T._create_model_item(
            2, "Sku4Price55-iLVm1Goleg", offers_count=1, min_model_price="55", max_model_price="55"
        )
        item2 = T._create_model_item(
            1, "Sku2Price50-iLVm1Goleg", offers_count=2, min_model_price="5", max_model_price="50"
        )
        if add_supplier_id:
            supplier_ids = [3, 4, 5, 9]
        else:
            supplier_ids = [None] * 4

        self._check_blue_prime("BLUE", item1=item1, item2=item2, supplier_id=supplier_ids[0])
        self._check_blue_prime("blue", item1=item1, item2=item2, supplier_id=supplier_ids[1])
        self._check_blue_prime("BlUe", item1=item1, item2=item2, supplier_id=supplier_ids[2])
        self._check_blue_prime("BLUE", use_rearr=True, item1=item1, item2=item2, supplier_id=supplier_ids[3])

        # Для контроля выдача без флага
        # CPA у синих оферов пессимизируются на зеленом маркете
        response = self.report.request_json(
            'place=prime&text=blue&allow-collapsing=1&use-default-offers=1&rids=213&rgb=green_with_blue&base=default.market-exp-prestable.yandex.ru&onstock=1&debug=1'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "id": 1,
                        "offers": {"items": [{"wareId": "Sku2Price50-iLVm1Goleg"}]},
                    },  # Для первой модели был выбран зеленый офер
                    {
                        "entity": "product",
                        "id": 2,
                        "offers": {"items": [{"wareId": "Sku4Price55-iLVm1Goleg"}]},
                    },  # В этой модели только синие оферы
                    {"entity": "product", "id": 3, "offers": {"items": [{"wareId": "GreenOffer3_1_gggggggg"}]}},
                ]
            },
            allow_different_len=False,
        )

        # Проверяем, что вызывается пересортировка с фильтрацией моделей в продаже
        self.assertFragmentIn(response, {"logicTrace": [Contains("rearrangeDocsWithDynamicDocStats")]})

    def test_blue_prime(self):
        """
        Что проверяем: с флагом rgb=BLUE prime ведет поиск только по синим оферам.
        Параметр rgb регистронезависимый
        Так же проверяется, что статистики для моделей рассчитываются только
        для байбоксов синих оферов (цена зеленого офера 2 и ФФ-офера 70 не вошли в статистику)
        """
        self._blue_prime_test_base()

    def _check_supplier_filter(self, text, sup_id, expects):
        request = 'place=prime&text={}&allow-collapsing=1&use-default-offers=1&rids=213&'.format(text)
        request += 'disable-rotation-on-blue=1&debug=da&onstock=da&rgb=BLUE'
        request += '&supplier-id={}'.format(sup_id)
        response = self.report.request_json(request)
        items = [
            {
                "entity": "product",
                "offers": {
                    "items": [
                        {
                            "supplier": {"id": sup_id},
                            "wareId": ware_id,
                            "cpa": "real",
                        }
                    ]
                },
            }
            for ware_id in expects
        ]
        # Третьей модели нет, т.к. в ней нет синих оферов
        self.assertFragmentIn(response, {"results": items}, allow_different_len=False, preserve_order=False)

    def test_blue_prime_supplier_has_effect(self):
        """
        Что проверяем: с флагом rgb=BLUE prime ведет поиск только по синим оферам.
        Проверяем, что supplier-id работает.
        """
        self._check_supplier_filter("blue", 3, ("Sku4Price55-iLVm1Goleg", "Sku2Price52-iLVm1Goleg"))
        self._check_supplier_filter("blue", 4, ("Sku2Price50-iLVm1Goleg",))
        self._check_supplier_filter("blue", 5, [])
        self._check_supplier_filter("pill", 5, ("BluePills____________g",))
        self._check_supplier_filter("pill", 4, [])
        self._check_supplier_filter("fridge", 6, ("SoLargeWeDontStoreIt_g",))
        self._check_supplier_filter("fridge", 3, [])

    def test_blue_prime_aprice(self):
        """
        Что проверяем: при сортировке по возрастанию цены, сперва будет
        отображен офер СКУ1 (цена 5), а потом офер СКУ4 (цена 55)
        """
        self._check_blue_prime(
            "BLUE",
            how="aprice",
            item1=T._create_model_item(
                1, "Sku1Price5-IiLVm1Goleg", offers_count=2, min_model_price="5", max_model_price="50"
            ),
            item2=T._create_model_item(
                2, "Sku4Price55-iLVm1Goleg", offers_count=1, min_model_price="55", max_model_price="55"
            ),
        )

    def test_blue_prime_dprice(self):
        """
        Что проверяем: при сортировке по возрастанию цены, сперва будет
        отображен офер СКУ4 (цена 55), а потом офер СКУ2 (цена 50)
        """
        self._check_blue_prime(
            "BLUE",
            how="dprice",
            item1=T._create_model_item(
                2, "Sku4Price55-iLVm1Goleg", offers_count=1, min_model_price="55", max_model_price="55"
            ),
            item2=T._create_model_item(
                1, "Sku2Price50-iLVm1Goleg", offers_count=2, min_model_price="5", max_model_price="50"
            ),
        )

    def test_blue_prime_model_statistic(self):
        """
        Что проверяем: статистика модели изменится, если байбокс офер ушел из продажи и выбрался другой офер
        В тесте test_blue_prime видно, что цены модели 1 от 5 до 50. Уберем из продажи офер за 50. Выберется следующая цена 52
        """
        self.dynamic.disabled_sku_offers += [
            DynamicSkuOffer(shop_id=4, sku=_Offers.sku2_offer2.offerid),
        ]

        self._check_blue_prime(
            "BLUE",
            how="dprice",
            item1=T._create_model_item(
                2, "Sku4Price55-iLVm1Goleg", offers_count=1, min_model_price="55", max_model_price="55"
            ),
            item2=T._create_model_item(
                1, "Sku2Price52-iLVm1Goleg", offers_count=2, min_model_price="5", max_model_price="52"
            ),
        )

    def test_blue_prime_collapsing_hid(self):
        """
        Что проверяем: схлопывание оферов до модели в синем прайме, даже если был не текстовый запрос
        """
        response = self.report.request_json('place=prime&allow-collapsing=1&use-default-offers=1&hid=1&rgb=BLUE')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "id": 1,
                    },
                    {
                        "entity": "product",
                        "id": 2,
                    },
                ]
            },
            allow_different_len=False,
        )

    def test_blue_prime_ask_offers_collection_only(self):
        """
        Проверяем, что на синем прайме запрашивается только оферная коллекция
        """
        response = self.report.request_json(
            'place=prime&allow-collapsing=1&use-default-offers=1&rids=213&text=blue&rgb=BLUE&debug=da'
        )
        self.assertFragmentNotIn(
            response, {"debug": {"report": {"context": {"collections": {"MODEL": {"text": NotEmpty()}}}}}}
        )

    def test_hide_initial_filters(self):
        """
        Что проверяем: фильтры с initialFound=0 скрываются на синем прайме
        """
        request = "place=prime&allow-collapsing=1&use-default-offers=1&rids=213&hid=1"
        # На синем прайме
        response = self.report.request_json(request + '&rgb=blue')
        self.assertFragmentNotIn(response, {"filters": [{"id": "301"}]})

    def test_dynamic_filter(self):
        """
        Что проверяем: офферы исчезают из выдачи по динамику от StockStorage.
        """
        request = 'place=sku_offers&market-sku=1&show-urls=direct&rids=213&rgb=green_with_blue&rearr-factors=market_blue_buybox_max_price_rel_add_diff=0'

        # Проверяем дефолтный офер без динамика
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
                                    "wareId": _Offers.sku1_offer1.waremd5,
                                }
                            ]
                        },
                    }
                ]
            },
            allow_different_len=False,
        )

        # Скрываем динамиком дефолтный офер для СКУ и дефолтным становится другой офер.
        self.dynamic.disabled_sku_offers += [
            DynamicSkuOffer(shop_id=3, sku=_Offers.sku1_offer1.offerid),
        ]
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
                                    "wareId": _Offers.sku1_offer2.waremd5,
                                }
                            ]
                        },
                    }
                ]
            },
            allow_different_len=False,
        )

    def test_sku_offers_do_not_pessimize_by_delivery(self):
        """
        Что тестируем: карточка СКУ не отбрасывается по доставке в регион.
        В реальности любые фильтры заблокированы для СКУ, но проверять их все не представляется возможным
        """
        response = self.report.request_json('place=sku_offers&market-sku=1&show-urls=direct&rids=2')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "sku",
                        "id": "1",
                    }
                ]
            },
            allow_different_len=False,
        )

    def test_offerinfo_by_blue_feed_shoffer_id(self):
        """
        Что проверяем: правильность запроса по синему feed_shoffer_id.
        В качестве фида указан фид виртуального магазина, а offerid=supplier_feed_id.supplier_offer_id
        """
        response = self.report.request_json(
            'place=offerinfo&feed_shoffer_id=1-{}.{}&rids=213&show-urls=cpa&regset=1&rgb=green_with_blue'.format(
                _Offers.sku1_offer1.feedid, _Offers.sku1_offer1.offerid
            )
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "offer", "wareId": _Offers.sku1_offer1.waremd5},
                ]
            },
        )

    def test_sku_offers_price_rotation(self):
        """
        Что проверяем:
        puid (passport_uid) id пользователя залогиненного в системе - один для одного и того же пользователя на всех устройствах
        uuid (user uid) id установки приложения - может быть разным для одного пользователя - используется в приложениях
        yandexuid - id пользователя - может быть разным на разных устройствах для одного и того же пользователя (не используется в приложениях)
        Для разных puid  возвращаются разные офферы.
        Если нет puid - для разных uuid возвращаются разные офферы
        Если нет puid и uuid - для разных yandexuid возвращаются разные офферы
        При одном и том же puid но разных uuid, yandexuid возвращается один и тот же оффер
        """

        offer1 = {
            "results": [
                {
                    "entity": "sku",
                    "id": "5",
                    "offers": {
                        "items": [
                            {
                                "entity": "offer",
                                "marketSku": "5",
                                "wareId": _Offers.sku5_offer1.waremd5,
                            }
                        ]
                    },
                }
            ]
        }

        offer2 = {
            "results": [
                {
                    "entity": "sku",
                    "id": "5",
                    "offers": {
                        "items": [
                            {
                                "entity": "offer",
                                "marketSku": "5",
                                "wareId": _Offers.sku5_offer2.waremd5,
                            }
                        ]
                    },
                }
            ]
        }

        # Для разных puid  возвращаются разные офферы.
        response = self.report.request_json('place=sku_offers&market-sku=5&show-urls=direct&rids=213&puid=456')
        self.assertFragmentIn(response, offer1)
        response = self.report.request_json('place=sku_offers&market-sku=5&show-urls=direct&rids=213&puid=345')
        self.assertFragmentIn(response, offer2)

        # Если нет puid - для разных uuid возвращаются разные офферы
        response = self.report.request_json(
            'place=sku_offers&market-sku=5&show-urls=direct&rids=213&uuid=2365327e90594e0fa3d710ef0040146e'
        )
        self.assertFragmentIn(response, offer1)
        response = self.report.request_json(
            'place=sku_offers&market-sku=5&show-urls=direct&rids=213&uuid=c688a87e4fcc40c181cf677038166ddf'
        )
        self.assertFragmentIn(response, offer2)

        # Если нет puid и uuid - для разных yandexuid возвращаются разные офферы
        response = self.report.request_json('place=sku_offers&market-sku=5&show-urls=direct&rids=213&yandexuid=2')
        self.assertFragmentIn(response, offer1)
        response = self.report.request_json('place=sku_offers&market-sku=5&show-urls=direct&rids=213&yandexuid=8')
        self.assertFragmentIn(response, offer2)

        # При одном и том же puid но разных uuid и yandexuid возвращается один и тот же оффер
        response = self.report.request_json(
            'place=sku_offers&market-sku=5&show-urls=direct&rids=213&puid=456&uuid=2365327e90594e0fa3d710ef0040146e&yandexuid=1'
        )
        self.assertFragmentIn(response, offer1)
        response = self.report.request_json(
            'place=sku_offers&market-sku=5&show-urls=direct&rids=213&puid=456&uuid=c688a87e4fcc40c181cf677038166ddf&yandexuid=5'
        )
        self.assertFragmentIn(response, offer1)

        # При отсутствии puid одном и том же uuid но разных yandexuid возвращается один и тот же оффер
        response = self.report.request_json(
            'place=sku_offers&market-sku=5&show-urls=direct&rids=213&uuid=c688a87e4fcc40c181cf677038166ddf&yandexuid=1'
        )
        self.assertFragmentIn(response, offer2)
        response = self.report.request_json(
            'place=sku_offers&market-sku=5&show-urls=direct&rids=213&uuid=c688a87e4fcc40c181cf677038166ddf&yandexuid=5'
        )
        self.assertFragmentIn(response, offer2)

    def test_showing_blue_on_green(self):
        """
        Что проверяем: с флагом rgb=green_with_blue, rgb=green или без него prime ведет поиск по зеленым и синим оферам.
        """
        result = {
            "results": [
                {"entity": "product", "id": 1, "offers": {"items": [{"wareId": "Sku2Price50-iLVm1Goleg"}]}},
                {"entity": "product", "id": 2, "offers": {"items": [{"wareId": "Sku4Price55-iLVm1Goleg"}]}},
                {"entity": "product", "id": 3, "offers": {"items": [{"wareId": "GreenOffer3_1_gggggggg"}]}},
            ]
        }
        for color in ["", "&rgb=green", "&rgb=green_with_blue"]:
            response = self.report.request_json(
                'place=prime&text=blue&allow-collapsing=1&use-default-offers=1&base=default.market-exp-prestable.yandex.ru&rids=213&base=default.market-exp-prestable.yandex.ru{}'.format(
                    color
                )
            )
            self.assertFragmentIn(response, result, allow_different_len=False)

    def check_free_delivery_for_blue_offers(
        self, request, offer_id, service_id, courier_price, pickup_price, outlet_price, patch=None
    ):
        response = self.report.request_json(request)

        checked = [
            {
                "entity": "offer",
                "wareId": offer_id,
                "delivery": {
                    "options": [
                        {
                            "price": {
                                "value": courier_price,
                            },
                            "discount": Absent(),
                            "serviceId": service_id,
                        }
                    ],
                    "pickupOptions": [
                        {
                            "serviceId": 103,
                            "price": {
                                "value": pickup_price,
                            },
                            "discount": Absent(),
                        }
                    ],
                },
                "outlet": {
                    "id": "2002",
                    "selfDeliveryRule": {
                        "cost": pickup_price,
                        "discount": Absent(),
                    },
                },
            }
        ]
        if patch is not None:
            patch(checked)
        self.assertFragmentIn(response, checked)

    OFFERS_DELIVERY_INFO = {
        "Sku2Price50-iLVm1Goleg": {
            "service_id": "157",
            "courier_price": "99",
            "pickup_price": "99",
            "outlet_price": "100",
        },
        "Sku2Price52-iLVm1Goleg": {
            "service_id": "157",
            "courier_price": "99",
            "pickup_price": "99",
            "outlet_price": "100",
        },
        "Sku2Price53-iLVm1Goleg": {
            "service_id": "157",
            "courier_price": "99",
            "pickup_price": "99",
            "outlet_price": "100",
        },
        "Sku2Price54-iLVm1Goleg": {
            "service_id": "157",
            "courier_price": "99",
            "pickup_price": "99",
            "outlet_price": "100",
        },
        "Sku2Price55-iLVm1Goleg": {
            "service_id": "157",
            "courier_price": "99",
            "pickup_price": "99",
            "outlet_price": "100",
        },
    }

    def patch_with_discount_type(self, checked, discount_type):
        if discount_type is None:
            return
        for check in checked:
            for option in ["options", "pickupOptions"]:
                check["delivery"][option][0]["discount"] = {
                    "oldMin": {
                        "value": check["delivery"][option][0]["price"]["value"],
                    },
                    "discountType": discount_type,
                }
                check["delivery"][option][0]["price"]["value"] = "0"

            option = "outlet"
            check[option]["selfDeliveryRule"]["discount"] = {
                "oldMin": {
                    "value": check[option]["selfDeliveryRule"]["cost"],
                },
                "discountType": discount_type,
            }
            check[option]["selfDeliveryRule"]["cost"] = "0"

    def test_free_delivery_for_blue_offers(self):
        """Проверяется, что цена доставки обнуляется для синего оффера, если цена оффера больше или равна blue_market_free_delivery_threshold"""
        prime_request = "place=prime&offerid={}&rids=213&allow-collapsing=0&pickup-options=grouped"
        NO_DELIVERY_DISCOUNT_FLAG = '&no-delivery-discount=1'
        for request in [
            prime_request + "&rgb=blue",
            prime_request + "&rgb=green_with_blue",
            "place=offerinfo&offerid={}&rids=213&show-urls=cpa&regset=1&rgb=green_with_blue&pickup-options=grouped",
            "place=sku_offers&market-sku=20&offerid={}&rids=213&pickup-options=grouped",
        ]:
            for flag in ('', NO_DELIVERY_DISCOUNT_FLAG):
                offer = "Sku2Price50-iLVm1Goleg"
                delivery_info = T.OFFERS_DELIVERY_INFO[offer]
                formatted_request = request.format(offer) + flag
                self.check_free_delivery_for_blue_offers(formatted_request, offer, **delivery_info)

            offer = "Sku2Price55-iLVm1Goleg"
            delivery_info = T.OFFERS_DELIVERY_INFO[offer]
            formatted_request = (
                request.format(offer) + '&rearr-factors=market_conf_loyalty_delivery_threshold_enabled=1'
            )
            self.check_free_delivery_for_blue_offers(
                formatted_request,
                offer,
                patch=lambda data: self.patch_with_discount_type(data, "threshold"),
                **delivery_info
            )
            formatted_request = request.format(offer) + NO_DELIVERY_DISCOUNT_FLAG
            self.check_free_delivery_for_blue_offers(formatted_request, offer, **delivery_info)

    def test_free_delivery_perks(self):
        """Проверяется, что цена доставки обнуляется для синего оффера, если передан перк.
        Пока только на синем маркете
        """
        for request in [
            "place=prime&offerid={offer}&rids=213&allow-collapsing=0&pickup-options=grouped&rgb=blue&perks={perks}",
            "place=prime&offerid={offer}&rids=213&allow-collapsing=0&pickup-options=grouped&rgb=green_with_blue&perks={perks}",
            "place=offerinfo&offerid={offer}&rids=213&show-urls=cpa&regset=1&rgb=blue&pickup-options=grouped&perks={perks}",
            "place=offerinfo&offerid={offer}&rids=213&show-urls=cpa&regset=1&rgb=green_with_blue&pickup-options=grouped&perks={perks}",
            "place=sku_offers&market-sku=20&offerid={offer}&rids=213&pickup-options=grouped&perks={perks}",
        ]:
            # cls.settings.blue_market_free_delivery_threshold = 55
            # cls.settings.blue_market_prime_free_delivery_threshold = 53
            # cls.settings.blue_market_yandex_plus_free_delivery_threshold = 52
            for perks, discount_type_50, discount_type_52, discount_type_53, discount_type_54, discount_type_55 in [
                ('prime', None, None, 'prime', 'prime', 'threshold'),
                ('yandex_plus', None, 'yandex_plus', 'yandex_plus', 'yandex_plus', 'threshold'),
                ('prime,yandex_plus', None, 'yandex_plus', 'prime', 'prime', 'threshold'),
                ('yandex_plus,prime', None, 'yandex_plus', 'prime', 'prime', 'threshold'),
                ('beru_plus', 'beru_plus', 'beru_plus', 'beru_plus', 'beru_plus', 'threshold'),
                ('yandex_plus,beru_plus', 'beru_plus', 'beru_plus', 'beru_plus', 'beru_plus', 'threshold'),
                ('fubar', None, None, None, None, 'threshold'),
            ]:
                for offer, discount_type, rearr_factor in [
                    ("Sku2Price50-iLVm1Goleg", discount_type_50, None),
                    ("Sku2Price52-iLVm1Goleg", discount_type_52, None),
                    ("Sku2Price53-iLVm1Goleg", discount_type_53, None),
                    ("Sku2Price54-iLVm1Goleg", discount_type_54, None),
                    ("Sku2Price55-iLVm1Goleg", discount_type_55, "market_conf_loyalty_delivery_threshold_enabled=1"),
                ]:
                    delviery_info = T.OFFERS_DELIVERY_INFO[offer]
                    formatted_request = request.format(
                        offer=offer,
                        perks=perks,
                    )
                    if rearr_factor is not None:
                        formatted_request += "&rearr-factors={}".format(rearr_factor)
                    self.check_free_delivery_for_blue_offers(
                        formatted_request,
                        offer,
                        service_id=delviery_info["service_id"],
                        courier_price=delviery_info["courier_price"],
                        pickup_price=delviery_info["pickup_price"],
                        outlet_price=delviery_info["outlet_price"],
                        patch=lambda data: self.patch_with_discount_type(data, discount_type),
                    )

    def test_better_with_plus(self):
        """Проверяется, что если бесплатная доставка у товара отсутствует, но может быть получена
        при добавлении перка плюса, это будет отображено в поле possibilities
        """
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        for request in [
            "place=prime&offerid={offer}&rids=213&allow-collapsing=0&pickup-options=grouped&rgb=blue&perks={perks}",
            "place=prime&offerid={offer}&rids=213&allow-collapsing=0&pickup-options=grouped&rgb=green_with_blue&perks={perks}",
            "place=offerinfo&offerid={offer}&rids=213&show-urls=cpa&regset=1&rgb=blue&pickup-options=grouped&perks={perks}",
            "place=offerinfo&offerid={offer}&rids=213&show-urls=cpa&regset=1&rgb=green_with_blue&pickup-options=grouped&perks={perks}",
            "place=sku_offers&market-sku={sku}&offerid={offer}&rids=213&pickup-options=grouped&perks={perks}&rgb=blue",
        ]:
            request += unified_off_flags
            for perks, possibility_50, possibility_52, possibility_53, possibility_54, possibility_55 in [
                # перк плюса выставлен - проращивать его в возможности бессмысленно
                ('yandex_plus', False, False, False, False, False),
                # первый товар не получит бесплатную доставку даже с плюсом - нет возможности получить бесплатную доставку
                # следующие товары могут получить доставку с плюсом, последний товар уже получил бесплатную доставку - новых возможностей нет
                (None, False, True, True, True, False),
                # все товары получили бесплатную доставку по другому перку. Новых возможностей нет
                ('beru_plus', False, False, False, False, False),
            ]:
                for offer, possib_perk_set, rearr_factor, msku in [
                    ("Sku2Price50-iLVm1Goleg", possibility_50, None, 20),
                    ("Sku2Price52-iLVm1Goleg", possibility_52, None, 20),
                    ("Sku2Price53-iLVm1Goleg", possibility_53, None, 20),
                    ("Sku2Price54-iLVm1Goleg", possibility_54, None, 20),
                    ("Sku2Price55-iLVm1Goleg", possibility_55, "market_conf_loyalty_delivery_threshold_enabled=1", 20),
                    # у dsbs всегда или отсутствует или false -> надом поменять
                    ("DsbsWithMsku_________g", False, None, 9116),
                    (_Offers.pills_offer.waremd5, False, None, 223000),
                ]:
                    for possib_perk, possib_rear in [
                        (possib_perk_set, "market_delivery_possibilities=1"),
                        (None, "market_delivery_possibilities=0"),
                        (possib_perk_set, None),
                    ]:
                        formatted_request = request.format(sku=msku, offer=offer, perks=perks)
                        rearr_factors = ";".join(flag for flag in [rearr_factor, possib_rear] if flag is not None)
                        if rearr_factors:
                            formatted_request += "&rearr-factors={}".format(rearr_factors)
                        response = self.report.request_json(formatted_request)
                        better_with_plus = Absent()

                        if possib_perk is not None:
                            better_with_plus = possib_perk

                        checked = [
                            {"entity": "offer", "wareId": offer, "delivery": {"betterWithPlus": better_with_plus}}
                        ]
                        self.assertFragmentIn(response, checked)

    def test_better_with_plus_cpc(self):
        """Проверяется, что для СРС (adv) офферов нет поля betterWithPlus"""
        white_offer = 'GreenOffer1_1_gggggggg'
        dsbs_tariffs_flag = '&rearr-factors=market_dsbs_tariffs={}'
        unified_tariffs_flag = '&rearr-factors=market_unified_tariffs={}'
        for request in [
            "place=prime&offerid={offer}&rids=213&allow-collapsing=0&pickup-options=grouped&perks={perks}",
            "place=offerinfo&offerid={offer}&rids=213&show-urls=cpa&regset=1&pickup-options=grouped&perks={perks}",
        ]:
            for dsbs_tariffs_flag_value in (0, 1, None):
                for unified_tariffs_flag_value in (0, 1, None):
                    if dsbs_tariffs_flag_value is not None:
                        request += dsbs_tariffs_flag.format(dsbs_tariffs_flag_value)
                    if unified_tariffs_flag_value is not None:
                        request += unified_tariffs_flag.format(unified_tariffs_flag_value)

                    for perks in ('yandex_plus', 'beru_plus', None):
                        formatted_request = request.format(offer=white_offer, perks=perks)
                        response = self.report.request_json(formatted_request)
                        self.assertFragmentNotIn(response, {"betterWithPlus"})

    def test_perks_dont_affect_green_offers(self):
        """Проверяется, что перки не влияют на зеленые офферы."""
        for request in [
            "place=prime&offerid={offer}&rids=213&allow-collapsing=0&pickup-options=grouped&rgb=green_with_blue&perks={perks}",
            "place=offerinfo&offerid={offer}&rids=213&show-urls=cpa&regset=1&rgb=green_with_blue&pickup-options=grouped&perks={perks}",
        ]:
            # цена доставки не должна аффектиться перками
            for perks in [
                'prime',
                'beru_plus',
                'yandex_plus',
                'prime,yandex_plus',
                'yandex_plus,prime',
                'beru_plus,yandex_plus',
                'prime,beru_plus,yandex_plus',
                'fubar',
            ]:
                response = self.report.request_json(request.format(offer='GreenOffer1_1_gggggggg', perks=perks))
                self.assertFragmentIn(
                    response,
                    [
                        {
                            "entity": "offer",
                            "wareId": 'GreenOffer1_1_gggggggg',
                            "delivery": {
                                "options": [
                                    {
                                        "price": {"value": "100"},
                                        "discount": Absent(),
                                        "serviceId": "99",
                                    },
                                ],
                            },
                        }
                    ],
                )

    def test_show_log_for_blue_offer(self):
        """
        Что проверяем: наличие в кликах и показах синего оффера признака 'is_blue'
        """
        self.report.request_json('place=productoffers&hyperid=2&rids=213&rgb=green_with_blue')
        self.click_log.expect(is_blue=1, ware_md5='Sku4Price55-iLVm1Goleg')
        self.show_log.expect(is_blue_offer=1, ware_md5='Sku4Price55-iLVm1Goleg')

    @classmethod
    def prepare_suggest(cls):
        """
        Подготовка данных для проверки запроса в саджесты и редиректы.
        На синем маркете репорт ходит с флагом &rgb=blue
        """
        cls.suggester.on_custom_url_request(part='blue suggest service', location='suggest-market-rich-blue').respond(
            suggestions=[Suggestion(part='blue suggest service', url='/product/9001')]
        )

    def test_suggest(self):
        """
        Что проверяем: на синем маркете репорт ходит в саджесты с флагом &rgb=blue
        """
        response = self.report.request_json('place=prime&cvredirect=1' '&text=blue+suggest+service' '&rgb=blue')

        self.assertFragmentIn(response, {'redirect': NotEmpty()})

    def _check_supplier_dynamic_filter(self, request, check_green_offers):
        """
        Что проверяем: работу динамического отключения поставщиков синего маркета
        1. Наличие синих оферов от магазина №3 до внесения поставщика в динамический список
        2. Отсутствие синих оферов от поставщика №3 при внесении поставщика в динамический список
        3. Наличие зеленых оферов этого же магазина
        """
        realRequests = make_hide_rules_requests(request)

        self.dynamic.market_dynamic.disabled_market_sku = []
        # 1
        self.dynamic.market_dynamic.disabled_blue_suppliers = []
        for realRequest in realRequests:
            response = self.report.request_json(realRequest)
            self.assertFragmentIn(
                response,
                {
                    "entity": "offer",
                    "supplier": {
                        "id": 3,
                    },
                },
            )

        # 2
        self.dynamic.market_dynamic.disabled_blue_suppliers += [
            DynamicShop(2),
            DynamicShop(3),
        ]
        for realRequest in realRequests:
            response = self.report.request_json(realRequest)
            self.assertFragmentNotIn(
                response,
                {
                    "entity": "offer",
                    "supplier": {
                        "id": 3,
                    },
                },
            )

            # 3
            if check_green_offers:
                self.assertFragmentIn(
                    response,
                    {
                        "entity": "offer",
                        "shop": {
                            "id": 2,
                        },
                        "supplier": Absent(),
                    },
                )

    def test_ignore_supplier_filter(self):
        """
        Что проверяем: отсутствие динамического отключения поставщиков
        для синего прайма при флаге игнорирования отключения
        """
        requests = make_hide_rules_requests(
            'place=prime&text=blue&rgb=blue&allow-collapsing=0&numdoc=1000', {'market_blue_ignore_supplier_filter': '1'}
        )

        self.dynamic.market_dynamic.disabled_blue_suppliers = []
        for request in requests:
            response = self.report.request_json(request)

            self.assertFragmentIn(
                response,
                {
                    "entity": "offer",
                    "supplier": {
                        "id": 3,
                    },
                },
            )

        self.dynamic.market_dynamic.disabled_blue_suppliers += [
            DynamicShop(2),
            DynamicShop(3),
        ]
        for request in requests:

            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {
                    "entity": "offer",
                    "supplier": {
                        "id": 3,
                    },
                },
            )

    def test_supplier_dynamic_filter_blue_prime(self):
        """
        Что проверяем: работу динамического отключения поставщиков для синего прайма
        """
        self._check_supplier_dynamic_filter('place=prime&text=blue&rgb=blue&allow-collapsing=0&numdoc=1000', False)

    def test_supplier_dynamic_filter_green_prime(self):
        """
        Что проверяем: работу динамического отключения поставщиков для зеленого прайма
        """
        self._check_supplier_dynamic_filter(
            'place=prime&text=blue&rgb=green_with_blue&allow-collapsing=0&numdoc=1000', True
        )

    def test_supplier_dynamic_filter_sku_offers(self):
        """
        Что проверяем: работу динамического отключения поставщиков для карточки СКУ
        """
        request = 'place=sku_offers&market-sku=1&market-sku=20&market-sku=4&offerid={}'
        offers_to_check = [
            _Offers.sku1_offer1.waremd5,
            _Offers.sku2_offer1.waremd5,
            _Offers.sku4_offer1.waremd5,
        ]

        self._check_supplier_dynamic_filter(request.format(','.join(offers_to_check)), False)

    def test_supplier_dynamic_filter_offerinfo(self):
        """
        Что проверяем: работу динамического отключения поставщиков для карточки офера
        """
        request = 'place=offerinfo&rgb=green_with_blue&rids=213&regset=2&offerid={}'
        offers_to_check = [
            _Offers.sku1_offer1.waremd5,
            _Offers.sku2_offer1.waremd5,
            _Offers.sku4_offer1.waremd5,
            # Для проверки добавляем зеленый офер поставщика №2 и блокируем его тоже
            'GreenOffer3_1_gggggggg',
        ]

        self._check_supplier_dynamic_filter(request.format(','.join(offers_to_check)), True)

    @skip('https://st.yandex-team.ru/MARKETOUT-35795')
    def test_supplier_dynamic_filter_miprime(self):
        """
        Что проверяем: работу динамического отключения поставщиков для miprime
        """
        self._check_supplier_dynamic_filter('place=miprime&text=blue&rgb=green_with_blue', True)

    def test_virtual_shop_dynamic_filter(self):
        """
        Что проверяем: работу динамического отключения синего маркета на белом
        1. Наличие на Белом синих оферов от Беру до внесения виртуального магазина в фильтр
        2. Наличие на Белом синих оферов от Беру при внесении виртуального в фильтр (в виде cpa-only)
        3. Наличие синих оферов на Беру
        """
        green_request = 'place=prime&text=blue&rgb=green_with_blue&allow-collapsing=0&numdoc=1000'
        blue_request = 'place=prime&text=blue&rgb=blue&allow-collapsing=0&numdoc=1000'

        response = self.report.request_json(green_request)
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "shop": {
                    "id": 1,
                },
            },
        )

        self.dynamic.market_dynamic.disabled_cpc_shops += [
            DynamicShop(1),
        ]

        response = self.report.request_json(green_request)
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "shop": {
                    "id": 1,
                },
            },
        )

        response = self.report.request_json(blue_request)
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "supplier": {
                    "id": 3,
                },
                "shop": {
                    "id": 1,
                },
            },
        )

    @classmethod
    def prepare_supplier_dynamic_filter_parallel(cls):
        cls.index.mskus += [
            # Эти СКУ поставляются магазином 3
            MarketSku(
                title="parallel sku1",
                hyperid=21,
                sku=21,
                waremd5='SkuP1-dDXWsIiLVm1goleg',
                blue_offers=[_Offers.parallel_sku1_offer1],
            ),
            MarketSku(
                title="parallel sku2",
                hyperid=22,
                sku=22,
                waremd5='SkuP2-dDXWsIiLVm1goleg',
                blue_offers=[_Offers.parallel_sku2_offer1],
            ),
            MarketSku(
                title="parallel sku3",
                hyperid=23,
                sku=23,
                waremd5='SkuP3-dDXWsIiLVm1goleg',
                blue_offers=[_Offers.parallel_sku3_offer1],
            ),
        ]

        cls.index.offers += [
            Offer(title='parallel green offer 1', fesh=2, price=2, hyperid=21),
            Offer(title='parallel green offer 2', fesh=2, price=3, hyperid=22),
            Offer(title='parallel green offer 3', fesh=2, price=3, hyperid=23),
        ]

    def test_supplier_dynamic_filter_parallel(self):
        """
        Что проверяем: работу динамического отключения поставщиков для параллельного поиска
        Без динамика будут отображаться все оферы
        """

        requests = make_hide_rules_requests('place=parallel&text=parallel&rgb=green_with_blue')

        self.dynamic.market_dynamic.disabled_blue_suppliers = []
        for request in requests:
            response = self.report.request_bs_pb(request)
            self.assertFragmentNotIn(response, {"market_offers_wizard_right_incut": {}})
            self.assertFragmentNotIn(response, {"market_offers_wizard_center_incut": {}})
            self.assertFragmentIn(response, {'market_offers_wizard': {"offer_count": 6}})

        self.dynamic.market_dynamic.disabled_blue_suppliers += [
            DynamicShop(2),
            DynamicShop(3),
        ]

        # Зеленые оферы остались
        for request in requests:
            response = self.report.request_bs_pb(request)
            self.assertFragmentNotIn(response, {"market_offers_wizard_right_incut": {}})
            self.assertFragmentNotIn(response, {"market_offers_wizard_center_incut": {}})
            self.assertFragmentIn(response, {'market_offers_wizard': {"offer_count": 3}})

    def test_virtual_shop_dynamic_filter_parallel(self):
        """
        Что проверяем: работу динамического отключения синего маркета для параллельного поиска
        """
        request = 'place=parallel&text=parallel&rgb=green_with_blue'
        response = self.report.request_bs_pb(request)
        self.assertFragmentNotIn(response, {"market_offers_wizard_right_incut": {}})
        self.assertFragmentNotIn(response, {"market_offers_wizard_center_incut": {}})
        self.assertFragmentIn(response, {'market_offers_wizard': {"offer_count": 6}})

        self.dynamic.market_dynamic.disabled_cpc_shops += [
            DynamicShop(1),
        ]

        response = self.report.request_bs_pb(request)
        self.assertFragmentNotIn(response, {"market_offers_wizard_right_incut": {}})
        self.assertFragmentNotIn(response, {"market_offers_wizard_center_incut": {}})
        self.assertFragmentIn(response, {'market_offers_wizard': {"offer_count": 3}})

    def test_blue_prime_sorts(self):
        """
        Что проверяем: название и порядок сортировок на синем
        """
        request = "place=prime&text=blue&rearr-factors=market_blue_buybox_max_price_rel_add_diff=0&hid=1&rgb="

        # Новые названия сортировок показываются на синем маркете:
        response = self.report.request_json(request + "blue")
        self.assertFragmentIn(
            response,
            {
                'sorts': [
                    {
                        "text": "сначала популярное",
                    },
                    {
                        "text": "сначала подешевле",
                        "options": [{"id": "aprice"}],
                    },
                    {
                        "text": "сначала подороже",
                        "options": [{"id": "dprice"}],
                    },
                    {
                        "text": "сначала с лучшей оценкой",
                        "options": [{"id": "quality"}],
                    },
                    {
                        "text": "сначала с отзывами",
                        "options": [{"id": "opinions"}],
                    },
                    {"text": "сначала со скидками", "options": [{"id": "discount_p"}]},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # на белом другие названия
        response = self.report.request_json(request + "green")
        self.assertFragmentIn(
            response,
            {
                'sorts': [
                    {
                        "text": "по популярности",
                    },
                    {
                        "text": "по цене",
                        "options": [{"id": "aprice"}, {"id": "dprice"}],
                    },
                    {
                        "text": "по рейтингу",
                        "options": [{"id": "quality"}],
                    },
                    {
                        "text": "по отзывам",
                        "options": [{"id": "opinions"}],
                    },
                    {"text": "по размеру скидки", "options": [{"id": "discount_p"}]},
                ]
            },
            preserve_order=True,
        )

        # и на белом есть сортировка по скидке
        response = self.report.request_json(request + "green_with_blue")
        self.assertFragmentIn(response, {'sorts': [{"options": [{"id": "discount_p"}]}]})

    def test_short_prime_sorts(self):
        """
        Что проверяем: короткие названия сортировок с cgi-параметром short_sorts_names=1
        """
        request = (
            "place=prime&text=blue&rearr-factors=market_blue_buybox_max_price_rel_add_diff=0&hid=1&short_sorts_names=1"
        )

        # Короткие названия сортировок показываются на синем и белом маркетах
        result = {
            'sorts': [
                {
                    "text": "Популярные",
                },
                {
                    "text": "Подешевле",
                    "options": [{"id": "aprice", "type": "asc"}],
                },
                {
                    "text": "Подороже",
                    "options": [{"id": "dprice", "type": "desc"}],
                },
                {
                    "text": "С лучшей оценкой",
                    "options": [{"id": "quality"}],
                },
                {
                    "text": "С отзывами",
                    "options": [{"id": "opinions"}],
                },
                {"text": "По скидке", "options": [{"id": "discount_p"}]},
            ]
        }

        response = self.report.request_json(request + "&rgb=blue")
        self.assertFragmentIn(
            response,
            result,
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            result,
            preserve_order=True,
        )

    def test_type_for_blue_prime_sorts(self):
        """
        Что проверяем: вывод поля type для сортировок по цене на синем маркете
        """
        request = "place=prime&text=blue&rearr-factors=market_blue_buybox_max_price_rel_add_diff=0&hid=1&short_sorts_names=1&rgb=blue"

        # Для id=("aprice"|"dprice") должно выводиться поле type
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'sorts': [
                    {
                        "options": [{"id": "aprice", "type": "asc"}],
                    },
                    {
                        "options": [{"id": "dprice", "type": "desc"}],
                    },
                ]
            },
        )

    def test_prime_discount_filter(self):
        """
        Что проверяем: что на синем маркете работает фильтр скидок и акций
        """
        # &base=default.market-exp-prestable.yandex.ru включает режим черной пятницы,
        # без него почему-то то один то другой SKU не находится
        request = "place=prime&hyperid=1&rids=213&rgb=green_with_blue&base=default.market-exp-prestable.yandex.ru&rearr-factors=market_blue_buybox_max_price_rel_add_diff=0"

        discount_offer_fragment = {'supplierSku': _Offers.sku1_offer1.offerid}
        no_discount_offer_fragment = {
            'supplierSku': _Offers.sku2_offer2.offerid,
        }

        # обычный запрос - находит все
        normal_response = self.report.request_json(request)
        self.assertFragmentIn(normal_response, discount_offer_fragment)
        self.assertFragmentIn(normal_response, no_discount_offer_fragment)

        # запрос со скидочным фильтром - не находит офферы без скидок и акций
        discount_response = self.report.request_json(request + '&filter-promo-or-discount=1')
        self.assertFragmentIn(discount_response, discount_offer_fragment)
        self.assertFragmentNotIn(discount_response, no_discount_offer_fragment)

    def test_blue_prime_sku_in_model_shows(self):
        """
        Проверяем, что для показа модели записался SKU.
        """
        self.report.request_json("place=prime&text=blue&allow-collapsing=1&use-default-offers=1&rgb=BLUE&rids=213")
        self.show_log.expect(hyper_id=1, shop_id=99999999, msku='20')

    def test_intents_pictures(self):
        """
        Что проверяем: добавление картинки наиболее релевантного офера для категорий в intents
        """

        intent_pics = [
            {
                "original": {
                    "containerWidth": 500,
                    "containerHeight": 600,
                    "url": "http://avatars.mdst.yandex.net/get-marketpic/1234/market_KdwwrYb4czANgt9-3poEQQ/orig",
                    "width": 500,
                    "height": 600,
                },
                "thumbnails": NotEmpty(),
            },
        ]

        request = 'place=prime&rids=213&rgb=blue&text=cat_sku6&hyperid=6000'

        # Без эксперимента картинок у интентов нет
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "intents": [
                    {
                        "category": {"hid": 4000, "nid": 4001},
                        "pictures": Absent(),
                        "intents": [
                            {
                                "category": {"hid": 5000, "nid": 5001},
                                "pictures": Absent(),
                                "intents": [
                                    {
                                        "category": {"hid": 6000, "nid": 6001},
                                        "pictures": Absent(),
                                    }
                                ],
                            }
                        ],
                    }
                ]
            },
            allow_different_len=False,
        )

        # С включеным экспериментом картинки появляются
        request += '&additional_entities=intents_pictures'
        response = self.report.request_json(request)
        # Включаем эксперимент для запрашивания картинок напрямую из сниппета вместо запроса обогощенного оффера с картинками.
        response_direct_snippet = self.report.request_json(request + '&rearr-factors=market_direct_snippet_intents=1')
        # Результат должен быть тот же
        self.assertEqual(response['intents'], response_direct_snippet['intents'])

        self.assertFragmentIn(
            response,
            {
                "intents": [
                    {
                        "category": {"hid": 4000, "nid": 4001},
                        "pictures": intent_pics,  # Категория 4001 не имеет привязанного к ней офера, но наследует лучший офер от дочерних
                        "intents": [
                            {
                                "category": {"hid": 5000, "nid": 5001},
                                "pictures": intent_pics,
                                "intents": [
                                    {
                                        "category": {"hid": 6000, "nid": 6001},
                                        "pictures": intent_pics,
                                    }
                                ],
                            }
                        ],
                    }
                ]
            },
            allow_different_len=False,
        )

    def test_link_nid(self):
        """
        Проверяется что ссылки и оригинальный узел имеют одинаковый ответ.
        """
        testcases = [
            (140001, 14001),  # ссылка на листовую категорию
            (40001, 4001),  # ссылка на нелистовую категорию
            (1400011, 14001),  # родительский нид ссылки с единственным ребёнком
        ]

        for link_id, navnode_id in testcases:
            template = 'place=prime&rgb=blue&nid={nid}&rearr-factors=market_enable_navigation_links=1'
            response = self.report.request_json(template.format(nid=navnode_id))
            response_link = self.report.request_json(template.format(nid=link_id))
            self.assertTrue(response.equal_to(response_link))

    def test_blue_nid(self):
        """Проверяется, что используется единое белое навигационное дерево"""
        response = self.report.request_json('place=prime&rgb=blue&hyperid=9000&nid=9002')
        self.assertFragmentIn(
            response,
            {
                "intents": [
                    {"category": {"hid": 4000, "nid": 4001}, "intents": [{"category": {"hid": 9000, "nid": 9002}}]}
                ]
            },
            allow_different_len=False,
        )

        # Флаг market_web_like_app_intents, который ключает хидовые интенты на приложении, расскатан
        response = self.report.request_json(
            'place=prime&hyperid=9000&rearr-factors=market_web_like_app_intents=0&client=ANDROID',
        )
        self.assertFragmentIn(
            response,
            {
                "intents": [
                    {
                        "category": {
                            "hid": 4000,
                            "nid": 4001,
                        },
                        "intents": [
                            {
                                "category": {
                                    "hid": 5000,
                                    "nid": 5001,
                                },
                                "intents": [
                                    {
                                        "category": {
                                            "hid": 6000,
                                            "nid": 6001,
                                        },
                                        "intents": [
                                            {
                                                "category": {
                                                    "hid": 0,
                                                    "nid": 6500,  # на синем интенты строятся по нидам поэтому там есть виртуальные ниды
                                                },
                                                "intents": [
                                                    {
                                                        "category": {
                                                            "hid": 9000,
                                                            "nid": 9001,
                                                        }
                                                    }
                                                ],
                                            }
                                        ],
                                    }
                                ],
                            }
                        ],
                    }
                ]
            },
            allow_different_len=False,
        )

        # на белом интенты строятся по hid-ам и там нет виртуальных узлов
        # (уже по нидам)
        response = self.report.request_json('place=prime&hyperid=9000&rearr-factors=turn_off_nid_intents_on_serp=0')
        self.assertFragmentIn(
            response,
            {
                "intents": [
                    {
                        "category": {
                            "hid": 4000,
                            "nid": 4001,
                        },
                        "intents": [
                            {
                                "category": {
                                    "hid": 5000,
                                    "nid": 5001,
                                },
                                "intents": [
                                    {
                                        "category": {
                                            "hid": 6000,
                                            "nid": 6001,
                                        },
                                        "intents": [
                                            {
                                                "category": {
                                                    "hid": 0,
                                                    "nid": 6500,
                                                },
                                                "intents": [
                                                    {
                                                        "category": {
                                                            "hid": 9000,
                                                            "nid": 9001,
                                                        }
                                                    }
                                                ],
                                            }
                                        ],
                                    }
                                ],
                            }
                        ],
                    }
                ]
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_blue_prime_sorting_by_guru_popularity(cls):
        cls.index.hypertree += [
            HyperCategory(hid=1001, output_type=HyperCategoryType.GURU),
        ]

        cls.index.models += [
            Model(hid=1001, hyperid=101, model_clicks=201),
            Model(hid=1001, hyperid=102, model_clicks=102),
            Model(hid=1001, hyperid=103, model_clicks=150),
            Model(hid=1001, hyperid=104, model_clicks=160),
            Model(hid=1001, hyperid=105, model_clicks=99),
            Model(hid=1001, hyperid=106, model_clicks=120),
        ]

        cls.index.mskus += [
            MarketSku(
                title='guru_popularity_sku 1',
                sku=1000112,
                hyperid=101,
                blue_offers=[BlueOffer(ts=101 + 200 * i) for i in range(10)],
            ),
            MarketSku(
                title='guru_popularity_sku 2',
                sku=1000113,
                hyperid=102,
                blue_offers=[BlueOffer(ts=102 + 2100 * i) for i in range(5)],
            ),
            MarketSku(
                title='guru_popularity_sku 3',
                sku=1000114,
                hyperid=103,
                blue_offers=[BlueOffer(ts=103 + 15000 * i) for i in range(8)],
            ),
            MarketSku(
                title='guru_popularity_sku 4',
                sku=1000115,
                hyperid=104,
                blue_offers=[BlueOffer(ts=104 + 150000 * i) for i in range(2)],
            ),
            MarketSku(title='guru_popularity_sku 5', sku=1000116, hyperid=105, blue_offers=[BlueOffer(ts=105)]),
            MarketSku(
                title='guru_popularity_sku 6',
                sku=1000117,
                hyperid=106,
                blue_offers=[BlueOffer(ts=106 + 350000 * i) for i in range(3)],
            ),
        ]

        for i in range(10):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 101 + 200 * i).respond(0.6)

        for i in range(5):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 102 + 2100 * i).respond(0.4)

        for i in range(8):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 103 + 15000 * i).respond(0.5)

        for i in range(2):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 104 + 150000 * i).respond(0.2)

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 105).respond(0.1)

        for i in range(3):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 106 + 350000 * i).respond(0.3)

    def test_blue_prime_sorting_by_guru_popularity(self):
        """Тест элементов релевантности на синем маркете на дефолтной сортировке"""
        response = self.report.request_json('place=prime&hid=1001&rgb=blue&debug=1')

        self.assertFragmentIn(
            response,
            {
                "rank": [
                    {"name": "HAS_PICTURE"},
                    {"name": "DELIVERY_TYPE"},
                    {"name": "IS_MODEL"},
                    {"name": "CPM"},
                    {"name": "MODEL_TYPE"},
                    {"name": "POPULARITY"},
                    {"name": "ONSTOCK"},
                    {"name": "RANDX"},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_blue_prime_text_search_default_sorting_(self):
        """Тест элементов релевантности на синем маркете на дефолтной сортировке на текстовом поиске"""
        response = self.report.request_json('place=prime&hid=1001&text=guru_popularity_sku&rgb=blue&debug=1')

        self.assertFragmentIn(
            response,
            {
                "rank": [
                    {"name": "HAS_PICTURE"},
                    {"name": "DELIVERY_TYPE"},
                    {"name": "IS_MODEL"},
                    {"name": "CPM"},
                    {"name": "MODEL_TYPE"},
                    {"name": "POPULARITY"},
                    {"name": "ONSTOCK"},
                    {"name": "RANDX"},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_blue_prime_price_sorting(self):
        """Тест элементов релевантности на синем маркете на сортировке по цене"""
        response = self.report.request_json('place=prime&hid=1001&how=aprice&rgb=blue&debug=1')
        self.assertFragmentIn(
            response,
            {
                "rank": [
                    {"name": "ONSTOCK"},
                    {"name": "DELIVERY_TYPE"},
                    {"name": "PRICE"},
                    {"name": "GURU_POPULARITY"},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_nav_category_without_child_not_is_leaf(self):
        """
        Что проверяем: Для зеленого и для синего маркета этот узел не является листом, несмотря на то что нет офферов из ее дочерних категорий.
        """
        request = 'place=prime&hyperid=11000&rgb='

        #
        for color in ['GREEN_WITH_BLUE', 'GREEN', 'BLUE', '']:
            response = self.report.request_json(request + color)
            self.assertFragmentIn(response, {"navnodes": [{"id": 11001, "isLeaf": False}]})

    def test_nav_category_with_blue_child_is_branch(self):
        """
        Что проверяем: синяя навигационная категория является веткой,
        если под ней есть зоть одна синяя категория.
        """
        request = 'place=sku_offers&market-sku=14000&rgb='

        response = self.report.request_json(request + 'BLUE')
        self.assertFragmentIn(response, {"entity": "sku", "navnodes": [{"id": 4001, "isLeaf": False}]})

    def test_market_sku_filter(self):
        def getSkuInfo(supplier):
            return {
                "entity": "sku",
                "id": "5",
                "offers": {"items": [{"entity": "offer", "supplier": {"id": supplier}}]},
            }

        request = 'place=sku_offers&market-sku=5&rids=213&rgb=blue'
        """Проверяется, что выбирается байбокс от поставщика 4:
            - в случае отсутствия динамика
            - в случае присутствия динамика с запрещенными комбинациями значений (только shopSku)
        """
        for skus in [
            [],
            [DynamicMarketSku(shop_sku='blue.offer.5.1'), DynamicMarketSku(shop_sku='blue.offer.5.2')],
        ]:
            self.dynamic.market_dynamic.disabled_market_sku = skus
            response = self.report.request_json(request)
            self.assertFragmentIn(response, getSkuInfo(4), allow_different_len=False)

        """Проверяется, что выводится оффер от поставщика 3, если оффер поставщика 4 заблокирован"""
        for skus in [
            [DynamicMarketSku(market_sku='5', supplier_id=4)],
            [
                DynamicMarketSku(market_sku='5', supplier_id=4, shop_sku='blue.offer.5.2'),
            ],
        ]:
            self.dynamic.market_dynamic.disabled_market_sku = skus
            response = self.report.request_json(request)
            self.assertFragmentIn(response, getSkuInfo(3), allow_different_len=False)

        """Проверяется, что скрываются все офферы от SKU '5' """
        for skus in [
            [DynamicMarketSku(market_sku='5', supplier_id=3), DynamicMarketSku(market_sku='5', supplier_id=4)],
            [
                DynamicMarketSku(supplier_id=3, shop_sku='blue.offer.5.1'),
                DynamicMarketSku(supplier_id=4, shop_sku='blue.offer.5.2'),
            ],
        ]:
            self.dynamic.market_dynamic.disabled_market_sku = skus
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response, {"entity": "sku", "id": "5", "offers": {"items": Absent()}}, allow_different_len=False
            )

        for skus in [
            [DynamicMarketSku(market_sku='5')],
            [
                DynamicMarketSku(
                    market_sku='5'
                ),  # скрываются все офферы для msku=5. Запись (market_sku='5', supplier_id=4) игнорируется
                DynamicMarketSku(market_sku='5', supplier_id=4),
            ],
            [
                DynamicMarketSku(market_sku='5', shop_sku='blue.offer.5.1'),
                DynamicMarketSku(market_sku='5', shop_sku='blue.offer.5.2'),
            ],
        ]:
            self.dynamic.market_dynamic.disabled_market_sku = skus
            response = self.report.request_json(request)
            self.assertFragmentIn(response, {'results': []}, allow_different_len=False)

        """Проверяется, что выбирается байбокс от поставщика 3 при скрытии офферов от SKU '5' и поставщика 4"""
        self.dynamic.market_dynamic.disabled_market_sku = [DynamicMarketSku(market_sku='5', supplier_id=4)]
        response = self.report.request_json(request)
        self.assertFragmentIn(response, getSkuInfo(3), allow_different_len=False)

        # Скрытие по модели
        self.dynamic.market_dynamic.disabled_market_sku = [DynamicMarketSku(model_id=4)]
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {'search': {'results': EmptyList()}}, allow_different_len=False)

    def test_filter_by_price(self):
        '''
        Что проверяем: В фильтрации оферов по цене участвует только байбокс.
        Если цена байбокса ниже, чем mcpricefrom, то весь СКУ отбрасывается
        '''
        # Байбокс с ценой 50. Ограничение >=50. Офер отображается
        response = self.report.request_json('place=prime&hyperid=1&rids=213&rgb=green_with_blue&mcpricefrom=50')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "wareId": "Sku2Price50-iLVm1Goleg",
                        "prices": {"value": "50"},
                    }
                ],
            },
        )

        # Байбокс с ценой 50. Ограничение >=52. Офер не отображается
        response = self.report.request_json('place=prime&hyperid=1&rids=213&rgb=green_with_blue&mcpricefrom=52')
        self.assertFragmentIn(response, {"result": Absent()})

        # Уберем офер с ценой 50. Ограничение >=52. Байбокс станет 52 и офер отобразится
        self.dynamic.disabled_sku_offers += [
            DynamicSkuOffer(shop_id=4, sku=_Offers.sku2_offer2.offerid),
        ]
        response = self.report.request_json('place=prime&hyperid=1&rids=213&rgb=green_with_blue&mcpricefrom=52')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "wareId": "Sku2Price52-iLVm1Goleg",
                        "prices": {"value": "52"},
                    }
                ],
            },
        )

        # Уберем еще офер с ценой 52. Ограничение >=52. Байбокс станет 53. Офер отобразится
        self.dynamic.disabled_sku_offers += [
            DynamicSkuOffer(shop_id=3, sku=_Offers.sku2_offer3.offerid),
        ]
        response = self.report.request_json('place=prime&hyperid=1&rids=213&rgb=green_with_blue&mcpricefrom=52')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "wareId": "Sku2Price53-iLVm1Goleg",
                        "prices": {"value": "53"},
                    }
                ],
            },
        )

    def check_dynamic_filter(self):
        '''
        Проверяем работу фильтра на базовых для синего офера
        '''
        requests = make_hide_rules_requests("place=prime&hyperid=1&rids=213&rgb=green_with_blue")

        # Офер с ценой 50 для СКУ 2 выключен. На выдаче отображается байбокс за 52
        for request in requests:
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "entity": "offer",
                            "wareId": "Sku2Price52-iLVm1Goleg",
                            "prices": {"value": "52"},
                        }
                    ],
                },
            )

        # Если игнорировать динамические фильтры для офера, то байбокс снова офер за 50
        requests = make_hide_rules_requests('place=prime&hyperid=1&rids=213&rgb=green_with_blue&dynamic-filters=0')
        for request in requests:
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "entity": "offer",
                            "wareId": "Sku2Price50-iLVm1Goleg",
                            "prices": {"value": "50"},
                        }
                    ],
                },
            )

    def test_dynamic_filter_for_blue_offer_by_stock(self):
        """
        Что проверяем: работу динамического фильтра по стокам
        """
        self.dynamic.disabled_sku_offers += [
            DynamicSkuOffer(shop_id=4, sku=_Offers.sku2_offer2.offerid),
        ]
        self.check_dynamic_filter()

    def test_dynamic_filter_for_blue_offer_by_supplier(self):
        """
        Что проверяем: работу динамического фильтра по поставщику
        """
        self.dynamic.market_dynamic.disabled_blue_suppliers += [
            # Поставщик 4 делает поставку офера с ценой 50
            DynamicShop(4),
        ]
        self.check_dynamic_filter()

    def test_model_aware_title(self):
        """
        Что проверяем: modelAwareTitle присутствует на продуктово важных плейсах
        """
        for request, title, not_title in [
            ('place=sku_offers&market-sku=4', 'model-aware title', 'blue offer sku4 ringo'),
            ('place=sku_offers&market-sku=1', 'blue offer sku1 john', 'model-aware title'),
            # тайтл sku - 'blue offer sku4 ringo', поэтому ищется по ringo
            (
                'place=prime&text=ringo&base=default.market-exp-prestable.yandex.ru',
                'model-aware title',
                'blue offer sku4 ringo',
            ),
            (
                'place=prime&text=john&base=default.market-exp-prestable.yandex.ru',
                'blue offer sku1 john',
                'model-aware title',
            ),
        ]:
            response = self.report.request_json(request + '&rgb=blue')
            self.assertFragmentIn(
                response,
                {
                    "entity": "offer",
                    "modelAwareTitles": {
                        "raw": title,
                    },
                },
            )
            self.assertFragmentNotIn(
                response,
                {
                    "entity": "offer",
                    "modelAwareTitles": {
                        "raw": not_title,
                    },
                },
            )

    def test_search_by_nid_on_white(self):
        """Фильтрация по ниду отключена на Белом
        https://st.yandex-team.ru/MARKETOUT-18608
        """
        expect = {
            "results": [
                {
                    "categories": [{"id": 6000}],
                    "navnodes": [{"id": 6001}],
                    "supplier": NotEmpty(),
                },
                {
                    "categories": [{"id": 6000}],
                    "navnodes": [{"id": 6001}],
                    "supplier": NotEmpty(),
                },
                {
                    "categories": [{"id": 12000}],
                    "navnodes": [{"id": 12001}],
                    "supplier": Absent(),
                },
                {
                    "categories": [{"id": 11000}],
                    "navnodes": [{"id": 11001}],
                    "supplier": Absent(),
                },
                {
                    "categories": [{"id": 10000}],
                    "navnodes": [{"id": 10001}],
                    "supplier": Absent(),
                },
                {
                    "categories": [{"id": 9000}],
                    "navnodes": [{"id": 9001}],
                    "supplier": Absent(),
                },
                {
                    "categories": [{"id": 7000}],
                    "navnodes": [{"id": 7001}],
                    "supplier": Absent(),
                },
                {
                    "categories": [{"id": 6000}],
                    "navnodes": [{"id": 6001}],
                    "supplier": Absent(),
                },
                {
                    "categories": [{"id": 10000}],
                    "navnodes": [{"id": 10001}],
                    "supplier": NotEmpty(),
                },
                {
                    "categories": [{"id": 11000}],
                    "navnodes": [{"id": 11001}],
                    "supplier": NotEmpty(),
                },
                {
                    "categories": [{"id": 12000}],
                    "navnodes": [{"id": 12001}],
                    "supplier": NotEmpty(),
                },
                {
                    "categories": [{"id": 9000}],
                    "navnodes": [{"id": 9001}],
                    "supplier": NotEmpty(),
                },
                {
                    "categories": [{"id": 7000}],
                    "navnodes": [{"id": 7001}],
                    "supplier": NotEmpty(),
                },
            ]
        }

        response = self.report.request_json('place=prime&text=cat&nid=6001&debug=1&numdoc=100')
        self.assertFragmentIn(response, expect, allow_different_len=False)

    def test_search_by_nid(self):
        """Проверяем, что поиск происходит по nid-литералу"""

        #
        for rgb in ['&rgb=blue', '']:
            response = self.report.request_json('place=prime&text=cat&nid=6500&debug=1' + rgb)

            self.assertFragmentIn(
                response,
                {
                    "debug": {
                        "brief": {
                            "reqwizardText": Contains('cat::', 'nid:"6500"'),
                        }
                    }
                },
            )

            # hid-ы не присутствуют в поисковом запросе
            self.assertFragmentNotIn(
                response,
                {
                    "debug": {
                        "brief": {
                            "reqwizardText": Contains('hyper_categ_id'),
                        }
                    }
                },
            )

    def test_sku_offers_feed_id_filter(self):
        """
        Проверка фильтрации офферов по supplier_feed_id
        """
        response = self.report.request_json('place=sku_offers&market-sku=20')
        # без фильтра получаем "оптимальный" оффер (3P)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "totalOffers": 1,
                    "results": [
                        {
                            "entity": "sku",
                            "id": "20",
                            "offers": {
                                "items": [
                                    {
                                        "entity": "offer",
                                        "shop": {
                                            "name": "virtual_shop",
                                            "feed": {"id": "1", "offerId": "4.blue.offer.2.2"},
                                        },
                                        "supplier": {"id": 4, "type": "3"},
                                        "prices": {"value": "50"},
                                    }
                                ]
                            },
                        }
                    ],
                }
            },
        )

        # 1P
        response = self.report.request_json('place=sku_offers&market-sku=20&feedid=3')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "sku",
                        "id": "20",
                        "offers": {
                            "items": [
                                {
                                    "entity": "offer",
                                    "shop": {
                                        "name": "virtual_shop",
                                        "feed": {"id": "1", "offerId": "3.blue.offer.2.3"},
                                    },
                                    "supplier": {"id": 3, "type": "1"},
                                }
                            ]
                        },
                    }
                ]
            },
        )

        # non-existing feed
        response = self.report.request_json('place=sku_offers&market-sku=20&feedid=5')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "sku",
                        "id": "20",
                        "offers": {"items": Absent()},
                    }
                ]
            },
        )

    def check_preorder(self, cgi, preorder, inverse=False):
        request = 'place=prime&text=blue&allow-collapsing=1&use-default-offers=1&rgb=BLUE&rids=213'
        response = self.report.request_json(request + cgi)
        result_1 = {
            "results": [
                {
                    "entity": "product",
                    "isPreorder": Absent(),
                    "offers": {
                        "items": [
                            {
                                "entity": "offer",
                                "supplier": {"id": 3},
                                "isPreorder": preorder,
                                "supplierSku": "blue.offer.4.1",
                            }
                        ]
                    },
                }
            ]
        }
        if inverse:
            self.assertFragmentNotIn(response, result_1)
        else:
            self.assertFragmentIn(response, result_1)
        result_2 = {"entity": "offer", "supplier": {"id": 3}, "isPreorder": preorder, "supplierSku": "blue.offer.4.1"}
        response = self.report.request_json('place=sku_offers&rgb=BLUE&rids=213&market-sku=4' + cgi)
        if inverse:
            self.assertFragmentNotIn(response, result_2)
        else:
            self.assertFragmentIn(response, result_2)

    def test_preorder(self):
        """Проверяется, что у офферов нет возможности предзаказа
        Тест будет закопан в MARKETOUT-23980. Его аналог сделан в test_rty_qpipe.py::test_preorder MARKETOUT-23782."""
        self.check_preorder('', Absent())

        """При добавлении только &show-preorder=1 к запросу у офферов все еще нет возможности предзаказа"""
        self.check_preorder('&show-preorder=1', Absent())

        """При добавлении оффера blue.offer.4.1 в динамик (и отсутствии &show-preorder=1) все еще нет возможности предзаказа."""
        """При этом оффер пропадает из выдачи, т.к. он уже помечен как предзаказный, но нет &show-preorder=1"""
        self.dynamic.preorder_sku_offers = [DynamicSkuOffer(shop_id=3, sku="blue.offer.4.1")]
        self.check_preorder('', Absent(), True)

        """Только при наличии оффера в динамике и &show-preorder=1 имеем признак предзаказа в оффере"""
        self.check_preorder('&show-preorder=1', True)

    def make_expected_offer(
        self, supplier, blue_offer, has_shop_delivery, outlet_id, is_dropship=False, is_white=False, skip_delivery=False
    ):
        # if is_white or blue_offer.is_fulfillment:
        feed_id = "1"  # virtual shop feed
        offer_id = str(blue_offer.feedid) + '.' + blue_offer.offerid
        outlet = Absent() if skip_delivery else {"id": outlet_id}
        # else:
        #    feed_id = str(blue_offer.feedid)
        #    offer_id = blue_offer.offerid
        #    outlet = Absent() if is_dropship or skip_delivery else {"id": outlet_id}

        delivery_partner_types = []
        if is_dropship or blue_offer.is_fulfillment:
            delivery_partner_types.append("YANDEX_MARKET")
        if has_shop_delivery:
            delivery_partner_types.append("SHOP")

        # if is_white or supplier.fulfillment_program is not False:
        shop_id = _Shops.blue_virtual_shop.fesh
        # else:
        #    shop_id = supplier.fesh

        return {
            "entity": "offer",
            "shop": {"id": shop_id, "feed": {"id": feed_id, "offerId": offer_id}},
            "supplier": {
                "entity": "shop",
                "name": supplier.name,
                "id": supplier.datafeed_id,
                "type": supplier.supplier_type,
            },
            "delivery": {
                "hasPickup": False if skip_delivery else not (is_dropship),
                "hasLocalStore": False if skip_delivery else not (is_dropship),
                "deliveryPartnerTypes": delivery_partner_types,
            },
            "outlet": outlet,
            "isFulfillment": blue_offer.is_fulfillment,
        }

    def test_blue_offer_fulfillment_flag(self):
        '''
        Проверяем выдачу репорта для офферов с разными значениями флага IS_FULFILLMENT
        '''
        for flag in (
            '',
            '&rearr-factors=market_blue_prime_without_delivery=1',
            '&rearr-factors=market_blue_prime_without_delivery=0',
        ):
            response = self.report.request_json('place=prime&text=cat&rgb=blue&rids=213&numdoc=12' + flag)
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        # pharmacy offer
                        {
                            "offers": {
                                "items": [
                                    self.make_expected_offer(
                                        _Shops.golden_partner,
                                        _Offers.cat_offer7,
                                        has_shop_delivery=True,
                                        outlet_id="2006",
                                    )
                                ]
                            }
                        },
                        # FF offer with courier delivery
                        {
                            "offers": {
                                "items": [
                                    self.make_expected_offer(
                                        _Shops.blue_shop_1,
                                        _Offers.cat_offer10,
                                        has_shop_delivery=False,
                                        outlet_id="2002",
                                    )
                                ]
                            }
                        },
                        # FF offer without courier delivery
                        {
                            "offers": {
                                "items": [
                                    self.make_expected_offer(
                                        _Shops.blue_shop_1,
                                        _Offers.cat_offer8,
                                        has_shop_delivery=False,
                                        outlet_id="2002",
                                    )
                                ]
                            }
                        },
                    ]
                },
            )

    def test_blue_offer_fulfillment_flag_on_white(self):
        '''
        Проверяем выдачу репорта для офферов с разными значениями флага IS_FULFILLMENT на белом маркете.
        Для синих офферов всегда должен отображаться виртуальный магазин.
        '''
        response = self.report.request_json(
            'place=prime&text=cat&rids=213' + '&offerid=BlueNoFulfillment____g,BlueOfferFulfillment_g'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    self.make_expected_offer(
                        _Shops.golden_partner,
                        _Offers.cat_offer7,
                        has_shop_delivery=True,
                        is_white=True,
                        outlet_id="2006",
                    ),
                    self.make_expected_offer(
                        _Shops.blue_shop_1,
                        _Offers.cat_offer10,
                        has_shop_delivery=False,
                        is_white=True,
                        outlet_id="2002",
                    ),
                ]
            },
        )

    def test_market_delivery_flag_for_dropship_offer(self):
        """
        Проверка правильного отображения наличия Маркет.Доставки для дропшип-оффера
        """
        skip_delivery_calculation_rearr = '&rearr-factors=enable_dsbs_filter_by_delivery_interval=0'
        prime_request = 'place=prime&rgb=blue&rids=213&text={}' + skip_delivery_calculation_rearr

        response = self.report.request_json(prime_request.format('fridge'))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "offers": {
                            "items": [
                                self.make_expected_offer(
                                    _Shops.dropship_shop,
                                    _Offers.fridge_offer,
                                    has_shop_delivery=False,
                                    is_dropship=True,
                                    skip_delivery=True,
                                    outlet_id="",
                                )
                            ]
                        }
                    },
                ]
            },
        )

        response = self.report.request_json(prime_request.format('bird'))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "offers": {
                            "items": [
                                self.make_expected_offer(
                                    _Shops.dropship_with_delivery_shop,
                                    _Offers.all_delivery_offer,
                                    has_shop_delivery=True,
                                    is_dropship=True,
                                    skip_delivery=True,
                                    outlet_id="",
                                )
                            ]
                        }
                    },
                ]
            },
        )

    def test_panthera_increased_top_limit(self):
        '''
        Проверям, что на синем репорте кастомное значение для топа пантеры для офферной коллекции (7000)
        по модельной коллекции не ищем
        '''

        response = self.report.request_json('place=prime&text=blue&debug=da&rgb=blue')
        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "report": {
                        "context": {
                            "collections": {
                                "SHOP": {
                                    "pron": ["panther_top_size_=250"],
                                },
                                "MODEL": NoKey("MODEL"),
                            }
                        }
                    }
                }
            },
            preserve_order=False,
            allow_different_len=True,
        )

    def geo_request(self, fesh):
        return (
            'place=geo&rids=213&require-geo-coords=1&fesh={}'
            '&tile=153%2C79&tile=153%2C80&tile=153%2C81&tile=154%2C79&tile=154%2C80&tile=154%2C81'
            '&tile=155%2C79&tile=155%2C80&tile=155%2C81&tile=156%2C79&tile=156%2C80&tile=156%2C81'
            '&zoom=8&ontile=500&show-outlet=tiles&debug=1'.format(fesh)
        )

    def expected_geo_response(self):
        return {
            "search": {
                "tiles": [
                    {"coord": {"x": 153, "y": 80, "zoom": 8}, "outlets": [{"id": "2004", "type": "pickup"}]},
                    {"coord": {"x": 155, "y": 81, "zoom": 8}, "outlets": [{"id": "2002", "type": "store"}]},
                    {"coord": {"x": 154, "y": 80, "zoom": 8}, "outlets": [{"id": "2001", "type": "pickup"}]},
                ]
            }
        }

    def test_geo_blue_outlets(self):
        """
        Для синего маркета запрос всех доступных пунктов выдачи можно обработать без запроса на базовые.
        Для этого достаточно вернуть все доступные точки магазина из shopsOutlets, с фильтрацией стандартным OutletFilter.
        Этот тест проверяет корректность обработки такого запроса.
        """
        response = self.report.request_json(self.geo_request(1))
        self.assertFragmentIn(response, self.expected_geo_response())
        # Проверяем, что почтовые пункты выдачи, привязанные к службе доставки, не выводятся
        # (фильтруются в ShopInfo.cpp::LoadShopsOutlets)
        self.assertFragmentNotIn(response, {"tiles": [{"outlets": [{"id": "2003", "type": "post"}]}]})

    def test_white_geo_uses_basesearch(self):
        """
        На белом этот запрос не должен был перестать ходить на базовые.
        """
        response = self.report.request_json(self.geo_request(1))
        self.assertFragmentIn(response, self.expected_geo_response())
        self.assertFragmentNotIn(
            response, {"logicTrace": [Contains("Skipping base searches for blue market geo request...")]}
        )

    def test_shop_info_through_feed_buckets(self):
        """Проверяется, что в place=shop_info вычисление pickup доставки магазином
        происходит с использованием пофидовых наборов бакетов"""
        response = self.report.request_json('place=shop_info&rids=213&fesh=5&shop-delivery=1')
        self.assertFragmentIn(response, {"outletCounts": {"all": 2}})

    def test_filter_ordering_filter(self):
        '''
        Проверка порядка следования фильтров на синем маркете. Фильтры скидка и бесплатная доставка идёт
        сразу после цены
        '''
        booleans = (None, False, True)
        for enable_at_beru_warehouse_filter in booleans:
            request = 'place=prime&rgb=BLUE&hid=1&perks=yandex_plus'
            if enable_at_beru_warehouse_filter is not None:
                request += '&rearr-factors=all_beru_stock_filter_enabled={}'.format(
                    1 if enable_at_beru_warehouse_filter else 0
                )

            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {
                    "filters": [
                        {"id": "glprice"},
                    ]
                    + [
                        {"id": "at-beru-warehouse"},
                    ]
                    if enable_at_beru_warehouse_filter is None or enable_at_beru_warehouse_filter
                    else []
                    + [
                        {"id": "filter-delivery-perks-eligible"},
                        {"id": "101"},
                    ],
                },
                preserve_order=True,
            )

    def test_use_glfilters_with_nid(self):
        """
        Проверяем, что на синем маркете hid определяется по nid
        """

        request = "place=prime&rearr-factors=market_through_gl_filters_on_search_blue=0&text=blue&allow-collapsing=1&use-default-offers=1&rgb=BLUE&rids=213&glfilter=201:2"

        # Запрос по nid, который имеет точное соответствие hid=1
        # Показывается только модель 1, т.к. у нее есть СКУ с таким значением фильтра
        response = self.report.request_json(request + "&nid=1")
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "id": 1,
                    }
                ]
            },
            allow_different_len=False,
        )

        # Запрос по виртуальному nid, который имеет дочерний узел hid=1
        # С новым поискам по нидам, возьмется первый ребенок этого узла (hid=1) + применятся &glfilter из запроса
        response = self.report.request_json(request + "&nid=100")
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "id": 1,
                    }
                ]
            },
            allow_different_len=False,
        )

        # ошибка 3019, для nid=11001 т.к. как категория не рутовая.
        request = (
            "place=prime&rearr-factors=market_through_gl_filters_on_search_blue=0&text=cat&rgb=blue&glfilter=201:2"
        )
        response = self.report.request_json(request + "&nid=11001")
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "id": 12000,
                    },
                    {
                        "entity": "product",
                        "id": 11000,
                    },
                ]
            },
            allow_different_len=False,
        )
        self.error_log.expect(code=3019).times(1)

    def check_logs(self, ware_md5, cgi_params, supplier_id, feed_id, offer_id, shop_id, shop_name):
        """
        Проверяем запись идентфикатора офера и фида в логи
        Для синих оферов применяется виртуализация offerid: он заменяется на строку вида
        <supplier_feed_id> + "." + <supplier_offerid>. При этом feed_id берется виртуального магазина.
        Сделано это для привязки офера к виртуальному магазину
        """
        response = self.report.request_json(
            'place=offerinfo&offerid={}&rids=213&show-urls=external&regset=1'.format(ware_md5) + cgi_params
        )
        self.assertFragmentIn(
            response, {'entity': 'offer', 'wareId': ware_md5, 'shop': {'id': shop_id, 'name': shop_name}}
        )
        self.show_log.expect(
            ware_md5=ware_md5,
            supplier_id=supplier_id,
            feed_id=feed_id,
            offer_id=offer_id,
            shop_id=shop_id,
            shop_name=shop_name,
        ).once()
        self.click_log.expect(
            ware_md5=ware_md5, supplier_id=supplier_id, feed_id=feed_id, offer_id=offer_id, shop_id=shop_id
        ).once()

    def test_logs_blue_offer_on_blue_market(self):
        """Для синего офера на синем маркете виртуализация есть"""
        self.check_logs(
            _Offers.sku1_offer1.waremd5,
            '&rgb=BLUE',
            supplier_id=3,
            feed_id=1,
            offer_id="3." + _Offers.sku1_offer1.offerid,
            shop_id=1,
            shop_name="virtual_shop",
        )

    def test_logs_blue_offer_on_white_market(self):
        """Для синего офера на белом  маркете виртуализация есть"""
        self.check_logs(
            _Offers.sku1_offer1.waremd5,
            '&rgb=GREEN',
            supplier_id=3,
            feed_id=1,
            offer_id="3." + _Offers.sku1_offer1.offerid,
            shop_id=1,
            shop_name="virtual_shop",
        )

    def test_logs_white_offer_on_white_market(self):
        """Белый офер на белом маркете не виртуализирует feed_id и offer_id"""
        # Для белых оферов виртуализации нет
        self.check_logs(
            "GreenOffer1_1_gggggggg",
            '&rgb=GREEN',
            supplier_id=None,
            feed_id=2,
            offer_id="white_offer_id",
            shop_id=2,
            shop_name="green_shop",
        )

    def test_logs_gold_partner_on_white_market(self):
        """На белом маркете оферы золотых партнеров представлены от виртуального магазина"""
        self.check_logs(
            _Offers.pills_offer.waremd5,
            '&rgb=GREEN',
            supplier_id=5,
            feed_id=1,
            offer_id="5." + _Offers.pills_offer.offerid,
            shop_id=1,
            shop_name="virtual_shop",
        )

    def test_logs_gold_partner_on_white_market_for_checkouter(self):
        """На белом мркете офферы золотых партнеров представлены от виртуального магазина,
        но под cgi-параметром &use-virt-shop=0 для чекаутера записывается в лог и возвращается shop_id поставщика
        (репорт эмулирует синие поведение для этих офферов)
        """
        self.check_logs(
            _Offers.pills_offer.waremd5,
            '&rgb=GREEN&use-virt-shop=0',
            supplier_id=None,
            feed_id=5,
            offer_id=_Offers.pills_offer.offerid,
            shop_id=5,
            shop_name="moscow_pharma",
        )

    @classmethod
    def prepare_blue_prime_redirect(cls):
        cls.suggester.on_default_request().respond()

    def test_blue_prime_redirect_without_rs(self):
        """
        Что проверяем: при редиректе на синем маркете не генерируются intents в ReportStat(rs)
        """
        request = (
            "place=prime&text=blue&cvredirect=1" "&rearr-factors=market_report_blender_premium_ios_text_redirect=0"
        )

        response = self.report.request_json(request + "&rgb=blue")
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "search",
                    "params": {
                        "rs": Absent(),
                    },
                }
            },
        )

        # На белом маркете тоже нет rs
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "search",
                    "params": {"rs": Absent()},
                }
            },
        )

    def test_suggest_is_disabled_for_b2b(self):
        '''
        Проверяем, что для b2b не используется саджестер.
        Позже, когда в саджестере поддержат чёрные навигационные деревья, это надо будет выпилить.
        '''
        request_base = (
            'place=prime'
            '&text=blue'
            '&cvredirect=1'
            '&use-multi-navigation-trees=1'
            '&available-for-business={is_b2b}'
            '&debug=da'
        )

        skip_suggest_log_pattern = (
            r"[ME].*CheckSuggestRedirect\(\)\: .*Skip suggest redirect\: suggester doesn't support B2B yet.*"
        )

        # не b2b флоу, саджестер должен использоваться
        response = self.report.request_json(request_base.format(is_b2b='0'))
        self.assertFragmentNotIn(
            response,
            {"logicTrace": [Regex(skip_suggest_log_pattern)]},
        )

        # b2b флоу, саджестер НЕ должен использоваться
        response = self.report.request_json(request_base.format(is_b2b='1'))
        self.assertFragmentIn(
            response,
            {"logicTrace": [Regex(skip_suggest_log_pattern)]},
        )

    def test_fulfillment_offers_only_flag(self):
        # Добавили флаг fulfillment-offers-only
        # чтобы скрыть оферы золотых партнеров для клиентов,
        # которые их не готовы поддержать
        request = "rgb=Blue&place=prime&hyperid=23000&allow-collapsing=0"
        sample = {
            "entity": "offer",
            "marketSku": "223000",
        }

        # Проверка, что оффер существует и выдаётся по запросу
        resp = self.report.request_json(request)
        self.assertFragmentIn(resp, sample)

        # Проверка, что оффер фильтруется по данному флагу
        # В текущей реализации фильтрация по данному флагу
        # происходит после вычисления buybox
        # и если buybox-оффер не-фулфилмент -
        # отфильтруются все офферы (включая фулфиллмент) данного msku
        request1 = request + "&fulfillment-offers-only=da"
        resp = self.report.request_json(request1)
        self.assertFragmentNotIn(resp, sample)

        # Проверяем второй вариант того же флага (подвалы вместо минусов)
        request2 = request + "&fulfillment_offers_only=da"
        resp = self.report.request_json(request2)
        self.assertFragmentNotIn(resp, sample)

    def test_p_one_offers_only_flag(self):
        # Добавили флаг supplier_type
        # для фильтрации по 1p

        request = "place=prime&hyperid=5&allow-collapsing=0"
        one_p = {
            "entity": "offer",
            "wareId": "FAKE1POFFEROOOOOOOOOOQ",
        }
        not_one_p = {
            "entity": "offer",
            "wareId": "FAKE3POFFERTTTTTTTTTTQ",
        }

        # Проверка, что не 1p оффер выдаётся для байбокса
        resp = self.report.request_json(request)
        self.assertFragmentIn(
            resp,
            [
                not_one_p,
            ],
        )

        request += "&supplier_type=1"
        resp = self.report.request_json(request)
        # Проверяем, что не 1p оффер был отфильтрован и вместо него
        # был выставлен соответствующий 1p оффер
        self.assertFragmentNotIn(
            resp,
            [
                not_one_p,
            ],
        )
        self.assertFragmentIn(
            resp,
            [
                one_p,
            ],
        )

    def test_p_one_offers_only_flag_validation(self):
        # Проверка валидации флага supplier_type
        # для фильтрации по 1p
        # На данной итерации флаг должен принимать значение только в 1

        request = "place=prime&hyperid=5&allow-collapsing=0&supplier_type=5"
        try:
            self.report.request_plain(request)
        except RuntimeError as e:
            self.assertIn("supplier_type values differing from 1, are not supported", str(e))
            self.error_log.expect(code=3043)
            return
        self.assertTrue(False)

    def test_prime_cpa_url(self):
        """
        Что проверяем: формирование cpa урла для офера на поисковой выдаче
        encrypted ссылка скрыта на синем прайме, т.к. ей некуда вести
        """
        request = 'place=prime&rids=213&show-urls=cpa,external&offerid=Sku1Price5-IiLVm1Goleg'
        resp = self.report.request_json(request + '&rgb=blue')
        self.assertFragmentIn(
            resp,
            {
                'results': [
                    {
                        'entity': 'product',
                        'offers': {
                            'items': [
                                {
                                    'entity': 'offer',
                                    'wareId': 'Sku1Price5-IiLVm1Goleg',
                                    'urls': {
                                        'cpa': NotEmpty(),
                                        'encrypted': Absent(),
                                    },
                                }
                            ]
                        },
                    }
                ]
            },
        )

        # Контролируем, что на белом маркете сра-ссылки показываются
        resp = self.report.request_json(request + '&rgb=green')
        self.assertFragmentIn(
            resp,
            {
                'entity': 'offer',
                'wareId': 'Sku1Price5-IiLVm1Goleg',
                'urls': {
                    'cpa': NotEmpty(),
                    'encrypted': NotEmpty(),
                },
            },
        )

    def test_offer_show_uid(self):
        """
        Что проверяем: наличие showUid у ДО на выдаче
        """
        resp = self.report.request_json('place=prime&rids=213&show-urls=cpa&rgb=blue&offerid=Sku1Price5-IiLVm1Goleg')
        self.assertFragmentIn(
            resp,
            {
                'results': [
                    {
                        'entity': 'product',
                        'showUid': '04884192001117778888816001',
                        'offers': {
                            'items': [
                                {
                                    'entity': 'offer',
                                    'showUid': '04884192001117778888806001',
                                }
                            ]
                        },
                    }
                ]
            },
        )

    def test_nids_reverse_redirects(self):
        """
        Проверяем обратный редирект с включенным белым деревом при узлах в синем поддереве. Он срабатывает, только с несуществующего (100042) нида в реальный (100041)
        """
        for nid in [100041, 100042]:
            resp = self.report.request_json(
                'rgb=Blue&nid={}&place=prime&cvredirect=1&use-multi-navigation-trees=1&non-dummy-redirects=1'.format(
                    nid
                )
            )
            self.assertFragmentIn(resp, {'results': [{'entity': 'product', 'navnodes': [{'id': 100041}]}]})

    def test_spoofing_redirects(self):
        """
        Что проверяем: При попытке обратиться к nid, который есть в navigation-redirects,
        не происходит подмена nid.
        cvredirects не влияет на это
        """
        for flag in ['', '&cvredirect=1']:
            resp = self.report.request_json(
                'rgb=Blue&nid=99003&place=prime&use-multi-navigation-trees=1&rearr-factors=market_use_white_tree_always=1'
                + flag
            )
            self.assertFragmentIn(
                resp,
                {
                    "navnodes": [
                        {
                            "entity": "navnode",
                            "id": 99001,
                        }
                    ]
                },
            )

    @classmethod
    def prepare_panther_tpsz(cls):
        cls.index.models += [Model(hyperid=100, hid=100)]
        cls.index.mskus += [
            MarketSku(title="bluepanther 1", hyperid=100, sku=100, blue_offers=[BlueOffer()]),
            MarketSku(title="bluepanther 2", hyperid=100, sku=101, blue_offers=[BlueOffer()]),
            MarketSku(title="bluepanther 3", hyperid=100, sku=102, blue_offers=[BlueOffer()]),
        ]

    def test_panther_tpsz(self):
        """
        На синем работают флаги panther_offer_tpsz и panther_no_categ_offer_tpsz
        """
        # запрос с категорией, но без флагов -- находятся все 3 оффера
        response = self.report.request_json('place=prime&rgb=blue&text=bluepanther&hid=100&debug=da')
        self.assertFragmentIn(response, {'debug': {'brief': {'counters': {'TOTAL_DOCUMENTS_PROCESSED': 3}}}})

        # запрос без категории и флагов -- те же 3 оффера
        response = self.report.request_json('place=prime&rgb=blue&text=bluepanther&debug=da')
        self.assertFragmentIn(response, {'debug': {'brief': {'counters': {'TOTAL_DOCUMENTS_PROCESSED': 3}}}})

        # запрос с категорией и с флагом panther_offer_tpsz
        response = self.report.request_json(
            'place=prime&rgb=blue&text=bluepanther&hid=100&debug=da&rearr-factors=panther_offer_tpsz=2'
        )
        self.assertFragmentIn(response, {'debug': {'brief': {'counters': {'TOTAL_DOCUMENTS_PROCESSED': 2}}}})

        # запрос без категории и с флагом panther_offer_tpsz
        response = self.report.request_json(
            'place=prime&rgb=blue&text=bluepanther&debug=da&rearr-factors=panther_offer_tpsz=2'
        )
        self.assertFragmentIn(response, {'debug': {'brief': {'counters': {'TOTAL_DOCUMENTS_PROCESSED': 2}}}})

        # запрос с категорией и с флагами panther_offer_tpsz и panther_no_categ_offer_tpsz
        # panther_no_categ_offer_tpsz не влияет, т.к. запрос с категорией
        response = self.report.request_json(
            'place=prime&rgb=blue&text=bluepanther&hid=100&debug=da&rearr-factors=panther_offer_tpsz=2;panther_no_categ_offer_tpsz=1'
        )
        self.assertFragmentIn(response, {'debug': {'brief': {'counters': {'TOTAL_DOCUMENTS_PROCESSED': 2}}}})

        # запрос без категории и с флагами panther_offer_tpsz и panther_no_categ_offer_tpsz
        # panther_no_categ_offer_tpsz влияет, т.к. запрос без категории
        response = self.report.request_json(
            'place=prime&rgb=blue&text=bluepanther&debug=da&rearr-factors=panther_offer_tpsz=2;panther_no_categ_offer_tpsz=1'
        )
        self.assertFragmentIn(response, {'debug': {'brief': {'counters': {'TOTAL_DOCUMENTS_PROCESSED': 1}}}})

    def test_groupings(self):
        """Проверяем что на синем мы ходим в офферную коллекцию с одной группировкой"""

        response = self.report.request_json('place=prime&rgb=blue&text=bluepanther&hid=100&debug=da')
        self.assertFragmentIn(
            response,
            {
                'debug': {
                    'report': {
                        'context': {
                            'collections': {
                                'SHOP': NotEmpty(),
                                'SHOP_UPDATE': NotEmpty(),
                                '*': {
                                    "g": [
                                        "1._virtual98.100.1.-1",
                                    ]
                                },
                            }
                        }
                    }
                }
            },
            allow_different_len=False,
        )

    def check_report_response(self, request, expected_offers=None, unexpected_offers=None):
        if expected_offers is None:
            expected_offers = []
        if unexpected_offers is None:
            unexpected_offers = []
        response = self.report.request_json(request)
        if expected_offers:
            self.assertFragmentIn(response, [{"entity": "offer", "wareId": o.waremd5} for o in expected_offers])

        for o in unexpected_offers:
            self.assertFragmentNotIn(response, {"entity": "offer", "wareId": o.waremd5})

    def test_market_delivery_offers_only_flag(self):
        """
        Проверка работы флага market_delivery_offers_only, который удаляет из выдачи офферы без нашей доставки.
        """
        request_prefix = "place=prime&rgb=blue&rids=213&numdoc=15&allow-collapsing=0&hyperid={}"

        def check_prime_response(hyperid, expected_offers=None, unexpected_offers=None):
            if expected_offers is None:
                expected_offers = []
            if unexpected_offers is None:
                unexpected_offers = []
            request = request_prefix.format(hyperid)
            self.check_report_response(request, expected_offers, unexpected_offers)

        def check_mardo_only_response(hyperid, expected_offers=None, unexpected_offers=None):
            if expected_offers is None:
                expected_offers = []
            if unexpected_offers is None:
                unexpected_offers = []
            for mdo_flag in ("&market_delivery_offers_only=1", "&market-delivery-offers-only=1"):
                for ffo_flag in ("", "&fulfillment_offers_only=0", "&fulfillment_offers_only=1"):
                    request = request_prefix.format(hyperid) + mdo_flag + ffo_flag
                    self.check_report_response(request, expected_offers, unexpected_offers)

        # Dropship
        check_prime_response(24000, [_Offers.fridge_offer])
        check_mardo_only_response(24000, [_Offers.fridge_offer])
        check_prime_response(25000, [_Offers.all_delivery_offer])
        check_mardo_only_response(25000, [_Offers.all_delivery_offer])
        # Click & collect
        check_prime_response(23000, [_Offers.pills_offer], [_Offers.pills_offer_ff])
        check_mardo_only_response(23000, [], [_Offers.pills_offer, _Offers.pills_offer_ff])
        # Ordinary blue
        check_prime_response(10000, [_Offers.cat_offer10], [_Offers.cat_offer2])
        check_mardo_only_response(10000, [_Offers.cat_offer10], [_Offers.cat_offer2])

    @classmethod
    def prepare_blue_dssm(cls):
        cls.index.hypertree += [HyperCategory(uniq_name='blue_panthalons_category', hid=93736)]

        cls.index.models += [Model(hyperid=3892104, hid=93736, title="collapsed from bluepanthalons")]
        cls.index.mskus += [
            MarketSku(title="bluepanthalons", hyperid=3892104, sku=389210401, blue_offers=[BlueOffer(ts=1109011)])
        ]

        cls.index.dssm.category_model_by_cat_uniqname_blue.on(query='bluepanthalons').set(*[0.24] * 50)
        cls.index.dssm.category_model_by_cat_uniqname_blue.on(hid=93736, category_name='blue_panthalons_category').set(
            *[0.24] * 50
        )
        cls.index.dssm.category_model_by_cat_uniqname_blue.on(
            query='bluepanthalons', hid=93736, category_name='blue_panthalons_category'
        ).set(0.58)

    def test_blue_dssm(self):
        '''В метафакторах синего (когда включено переранжирование) есть факторы от синей категорийной дссм-ки
        DSSM_BY_UNIQNAME_BLUE аналог DSSM_BY_UNIQNAME, но используется только на синих кластерах
        '''

        response = self.report.request_json(
            'place=prime&rgb=blue&text=bluepanthalons&hid=93736&debug=da'
            '&rearr-factors=market_blue_meta_formula_type=TESTALGO_trivial'
        )

        self.assertFragmentIn(
            response,
            {
                'entity': 'product',
                'titles': {'raw': 'collapsed from bluepanthalons'},
                'debug': {
                    'factors': {
                        "HEAD_AVG_CATEGORY_DSSM_BY_UNIQNAME_BLUE": Round(0.87, 2),  # avg NormDotProduct
                        "CATEGORY_DSSM_BY_UNIQNAME_BLUE": Round(0.87, 2),  # NormDotProduct(qEmb, dEmb)
                        "DSSM_BY_UNIQNAME_BLUE": Round(0.87, 2),  # joint_output by nonStopWords
                    }
                },
            },
        )

    def test_no_date_switch_hour_in_courier_options_sorting(self):
        request = "place=sku_offers&market-sku=15&rids=213"
        res = {"options": [{"serviceId": "157"}]}

        """Проверяется, что час перескока не влияет на сортировку опций курьерки"""
        # 1
        response = self.report.request_json(request)
        self.assertFragmentIn(response, res)

        # 2
        self.dynamic.lms -= [warehouse145_delivery_service_158]
        self.dynamic.lms += [
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=145,
                delivery_service_id=158,
                operation_time=0,
                date_switch_time_infos=[date_switch_time_info(1)],
            )
        ]

        response = self.report.request_json(request)
        self.assertFragmentIn(response, res)

        # 3
        self.dynamic.lms -= [warehouse145_delivery_service_158]
        self.dynamic.lms += [warehouse145_delivery_service_158]

        response = self.report.request_json(request)
        self.assertFragmentIn(response, res)

    def test_hide_msku_without_offers(self):
        """
        Что проверяем: скрытие msku без офферов по флагу hide-msku-without-offers
        """
        request = "place=sku_offers&market-sku=1&market-sku=11"
        hide_flag = "&hide-msku-without-offers=1"
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "sku", "id": "1"},
                    {"entity": "sku", "id": "11"},
                ]
            },
            allow_different_len=False,
        )

        request += hide_flag
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "sku", "id": "1"},
                ]
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_3p(cls):
        cls.index.mskus += [
            MarketSku(
                title="armchair",
                hyperid=11211,
                sku=22122,
                blue_offers=[
                    _Offers.offer_3p,
                ],
            ),
            MarketSku(
                title="stool",
                hyperid=11212,
                sku=22123,
                blue_offers=[
                    _Offers.offer_3p_stool,
                ],
            ),
        ]

    def check_offer(self, waremd5, shop, one_offer=True, market_sku=22122):
        for rgb in ('', '&rgb=blue'):
            offerid = ("&offerid=" + waremd5) if one_offer else ""
            request = (
                "place=sku_offers&rids=213&market-sku={}".format(market_sku)
                + offerid
                + rgb
                + USE_DEPRECATED_DIRECT_SHIPPING_FLOW
            )
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "entity": "sku",
                            "id": str(market_sku),
                            "offers": {
                                "items": [
                                    {
                                        "entity": "offer",
                                        "supplier": {"name": shop.name, "warehouseId": shop.warehouse_id},
                                        "wareId": waremd5,
                                        "isFulfillment": True,
                                        "atSupplierWarehouse": False,
                                        "fulfillmentWarehouse": 145,
                                    }
                                ]
                            },
                        }
                    ]
                },
                allow_different_len=False,
            )

    def test_3p_offer(self):
        """
        Проверяем правильность флагов на выдаче для 3P-офферов
        """
        self.check_offer(_Offers.offer_3p.waremd5, _Shops.blue_shop_2)
        self.check_offer(_Offers.offer_3p_stool.waremd5, _Shops.blue_shop_2, market_sku=22123)

    def test_ignore_stocks(self):
        """
        Проверяем, что скрытие на основании sku-filter.pbuf.sn (legacy qpipe) происходит с учетом тега 'ignore_stocks'
        """
        request = 'place=prime' '&rgb=blue' '&rids=213' '&allow-collapsing=0' '&offerid={waremd5_list}'

        # ignore_stocks = true
        pharma_shop_id = 5
        pharma_warehouse_id = 555
        pharma_offers = [_Offers.cat_offer7, _Offers.pills_offer]

        # производим скрытие через sku-filter.pbuf.sn
        self.dynamic.disabled_sku_offers += [
            DynamicSkuOffer(shop_id=pharma_shop_id, sku=offer.offerid, warehouse_id=pharma_warehouse_id)
            for offer in pharma_offers
        ]

        # Скрытие через динамик проигнорировано
        response = self.report.request_json(
            request.format(waremd5_list=','.join([offer.waremd5 for offer in pharma_offers]))
        )
        self.assertFragmentIn(
            response, {'results': [{'entity': 'offer', 'wareId': offer.waremd5} for offer in pharma_offers]}
        )

    def test_drugs_category_from_click_n_collect_only(self):
        """
        Костыль Костыльевич, на время запуска Покупок
        TODO: выпилить здесь: https://st.yandex-team.ru/MARKETOUT-33898
        01.10.2020 оставить в выдаче Покупок/Беру
        + опубликованными в "белый маркет"
        только C&C офферы из категории Лекарства и БАДы
        see: https://st.yandex-team.ru/MARKETOUT-33868
        see: https://st.yandex-team.ru/MARKETOUT-33954
        """
        request_template = (
            'place={place}{rgb}&rids=213&regset=2&allow-collapsing=0&market-sku={msku_list}&offerid={waremd5_list}'
            + '&rearr-factors=market_drugs_category_from_click_n_collect_only={drugs_from_cc_only}'
        )

        pharma_mskus_requested = ["223000", "223001", "223500", "223501", "223502", "223503", "223504", "223505"]
        pharma_offers_requested = [
            _Offers.pills_offer,
            _Offers.pills_offer_ff2,
            _Offers.painkiller_offer,
            _Offers.painkiller_offer_ff2,
            _Offers.vitamin_Z_offer,
            _Offers.vitamin_Z_offer_ff2,
            _Offers.germkiller_offer,
            _Offers.germkiller_offer_ff2,
        ]

        for rgb in ["", "&rgb=blue"]:
            for place in ["prime", "offerinfo", "sku_offers"]:
                for drugs_from_cc_only in [False, True]:
                    pharma_offers_expected = [
                        _Offers.pills_offer,
                        _Offers.painkiller_offer,
                        _Offers.vitamin_Z_offer,
                        _Offers.germkiller_offer,
                    ] + (
                        [_Offers.germkiller_offer_ff2]
                        if drugs_from_cc_only
                        else [
                            _Offers.pills_offer_ff2,
                            _Offers.painkiller_offer_ff2,
                            _Offers.vitamin_Z_offer_ff2,
                            _Offers.germkiller_offer_ff2,
                        ]
                    )

                    request = request_template.format(
                        place=place,
                        rgb=rgb,
                        msku_list=','.join(pharma_mskus_requested),
                        waremd5_list=','.join([offer.waremd5 for offer in pharma_offers_requested]),
                        drugs_from_cc_only=1 if drugs_from_cc_only else 0,
                    )

                    response = self.report.request_json(request)
                    for offer in pharma_offers_expected:
                        self.assertFragmentIn(response, {'entity': 'offer', 'wareId': offer.waremd5})
                    for offer in set(pharma_offers_requested) - set(pharma_offers_expected):
                        self.assertFragmentNotIn(response, {'entity': 'offer', 'wareId': offer.waremd5})

    def test_blue_strong_threshold(self):
        response = self.report.request_json(
            'text=guru_popularity&place=prime&exact-match=relevant&rgb=blue&rearr-factors=market_strong_assessor_threshold=0.4&market_relevance_formula_threshold=0.2'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"titles": {"raw": "HYPERID-101"}},
                    {"titles": {"raw": "HYPERID-103"}},
                    {"entity": "strongThresholdDelimiter"},
                    {"titles": {"raw": "HYPERID-102"}},
                    {"titles": {"raw": "HYPERID-106"}},
                    {"titles": {"raw": "HYPERID-104"}},
                    {"titles": {"raw": "HYPERID-105"}},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_at_supplier_warehouse(self):
        test_data = [
            (_Offers.sku1_offer1, True, False),  # 1P
            (_Offers.offer_3p, True, False),  # 3P
            (_Offers.fridge_offer, False, True),  # dropship
            (_Offers.pills_offer, False, True),  # click & collect
            (_Offers.dsbs_offer, False, True),  # dsbs
        ]

        for offer, is_fulfillment, at_supplier_warehouse in test_data:
            request = 'place=offerinfo&rids=213&show-urls=external&regset=2&offerid={}'.format(offer.waremd5)
            request += USE_DEPRECATED_DIRECT_SHIPPING_FLOW
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "entity": "offer",
                            "wareId": offer.waremd5,
                            "isFulfillment": is_fulfillment,
                            "atSupplierWarehouse": at_supplier_warehouse,
                        },
                    ]
                },
            )

    def test_delivery_calculation_disabling(self):
        """
        Если указан флаг &calculate-delivery=0, доставка не должна подробно вычисляться/отображаться
        В остальных случаях подсчет доставки включен по дефолту
        """
        request_template = 'place=prime&text=blue&rgb=blue&allow-collapsing=0'
        TEST_DATA = (
            (True, '&calculate-delivery=1'),
            (True, '&rearr-factors=market_blue_prime_without_delivery=0'),
            (True, ''),
            (False, '&calculate-delivery=0'),
            (True, '&rearr-factors=market_blue_prime_without_delivery=1'),
        )

        for calculate_delivery, suffix in TEST_DATA:
            response = self.report.request_json(request_template + suffix)
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'titles': {
                                'raw': 'blue offer sku4 ringo',
                            },
                            'delivery': {
                                'isAvailable': True,
                                'shopPriorityRegion': {'entity': 'region'} if calculate_delivery else Absent(),
                            },
                        }
                    ],
                },
            )

    def test_filters_disabling(self):
        """Если указан флаг &no-search-filters=1, не должны отображаться общие фильтры в прайме"""
        response = self.report.request_json('place=prime&text=blue&rgb=blue')
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': NotEmpty(),
                },
                'filters': NotEmpty(),
            },
        )

        response = self.report.request_json('place=prime&text=blue&no-search-filters=1&rgb=blue')
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': NotEmpty(),
                },
                'filters': Absent(),
            },
        )

        for flag in [0, 1]:
            for client in ["IOS", "ANDROID"]:
                response = self.report.request_json(
                    'place=prime&text=blue&rgb=blue&on-page=1&page=2&client={}&rearr-factors=disable_filter_calculating_on_far_page={}'.format(
                        client, flag
                    )
                )
                if flag:
                    self.assertFragmentIn(response, {"filters": Absent()})
                else:
                    self.assertFragmentIn(response, {"filters": NotEmpty()})

    NONDRUGS_CATEGORY = 11111
    DRUGS_CATEGORY = 15758037

    @classmethod
    def prepare_boost_medicine_with_courier(cls):
        cls.index.models += [
            Model(hyperid=1722204347, hid=T.DRUGS_CATEGORY),
            Model(hyperid=1732204347, hid=T.DRUGS_CATEGORY),
            Model(hyperid=1742204394, hid=T.DRUGS_CATEGORY),
            Model(hyperid=558171104, hid=T.NONDRUGS_CATEGORY),
            Model(hyperid=568171105, hid=T.NONDRUGS_CATEGORY),
        ]

        cls.index.mskus += [
            MarketSku(
                title="not medicine no courier",
                hyperid=558171104,
                sku=55000,
                blue_offers=[
                    BlueOffer(price=5, vat=Vat.VAT_10, feedid=3, ts=1, offerid='not.med.nocour.1'),
                ],
                pickup_buckets=[5003],
            ),
            MarketSku(
                title="not medicine with courier",
                hyperid=568171105,
                sku=56000,
                blue_offers=[
                    BlueOffer(
                        price=10,
                        vat=Vat.VAT_10,
                        feedid=3,
                        ts=2,
                        offerid='not.med.cour.1',
                        post_term_delivery=True,
                        weight=1,
                        dimensions=OfferDimensions(length=10, width=20, height=30),
                    ),
                ],
                pickup_buckets=[5003],
                delivery_buckets=[804],
            ),
            MarketSku(
                title="medicine no courier",
                hyperid=1742204394,
                sku=174000,
                blue_offers=[
                    BlueOffer(price=15, vat=Vat.VAT_10, feedid=3, ts=3, offerid='med.nocour.1'),
                ],
                pickup_buckets=[5003],
            ),
            MarketSku(
                title="medicine with courier",
                hyperid=1732204347,
                sku=173000,
                blue_offers=[
                    BlueOffer(
                        price=20,
                        vat=Vat.VAT_10,
                        feedid=3,
                        ts=4,
                        offerid='med.cour.1',
                        post_term_delivery=True,
                        weight=1,
                        dimensions=OfferDimensions(length=10, width=20, height=30),
                    ),
                ],
                delivery_buckets=[804],
                pickup_buckets=[5003],
            ),
            MarketSku(
                title="medicine with courier homeopathy",
                hyperid=1722204347,
                sku=172000,
                blue_offers=[
                    BlueOffer(
                        price=25,
                        vat=Vat.VAT_10,
                        feedid=3,
                        ts=5,
                        offerid='med.cour.hp.1',
                        post_term_delivery=True,
                        weight=1,
                        dimensions=OfferDimensions(length=10, width=20, height=30),
                    ),
                ],
                delivery_buckets=[804],
                pickup_buckets=[5003],
            ),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1).respond(1.0)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2).respond(0.99)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 3).respond(0.98)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 4).respond(0.94)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 5).respond(0.92)

    @staticmethod
    def entity(title, courier=False):
        return {
            'offers': {'items': [{'titles': {'raw': title}}]},
            'debug': {
                'factors': {
                    'COURIER_DELIVERY_EXISTS': '1' if courier else Absent(),
                }
            },
        }

    def test_boost_medicine_with_courier_text(self):
        """
        Проверяем, что на тексте на дефолтной сортировке на синем бустятся
        безрецептурные товары лекарственных категорий с курьерской доставкой
        с коэффициентом буста 1 по умолчанию. Проверяем также, что коэф-
        фициент настраивается флагом
        market_boost_medicine_with_courier_on_blue_text_coef
        """

        request = 'place=prime&rgb=blue&text=medicine&rids=213&debug=da&rearr-factors=market_not_prescription_drugs_delivery=1&'
        flag = '&rearr-factors=' 'market_boost_medicine_with_courier_on_blue_text_coef={}'

        # the order without boost
        # skip_delivery_flag is ignored and delivery is always calculated
        skip_delivery_flag = '&rearr-factors=market_blue_prime_without_delivery={}'
        for delivery_flag in ('', skip_delivery_flag.format(1), skip_delivery_flag.format(0)):
            response = self.report.request_json(request + delivery_flag)
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'results': [
                            T.entity('not medicine no courier', courier=False),
                            T.entity('not medicine with courier', courier=True),
                            T.entity('medicine no courier', courier=False),
                            T.entity('medicine with courier', courier=True),
                            T.entity('medicine with courier homeopathy', courier=True),
                        ]
                    }
                },
                allow_different_len=False,
                preserve_order=True,
            )

        # to use boost, we'll need to enable delivery calculation
        delivery_enabled = delivery_flag.format(1)
        # 0.98 < (0.94 * 1.05 = 0.987) < 0.99
        # (medicine no c) < (medicine with c) < (not medicine with c)
        #
        # (0.92 * 1.05 = 0.966) < 0.98
        # (medicine with c homeo) < (medicine no c)
        response = self.report.request_json(request + flag.format('1.05') + delivery_enabled)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        T.entity('not medicine no courier', courier=False),
                        T.entity('not medicine with courier', courier=True),
                        T.entity('medicine with courier', courier=True),
                        T.entity('medicine no courier', courier=False),
                        T.entity('medicine with courier homeopathy', courier=True),
                    ]
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

        # 1.0 < (0.94 * 1.08 = 1.0152)
        # (everybody else) < (not medicine no c) < (medicine with c)
        #
        # 0.99 < (0.92 * 1.08 = 0.9936) < 1.0
        # (not medicine with c) < (medicine with c homeo) < (not medicine no c)
        response = self.report.request_json(request + flag.format('1.08') + delivery_enabled)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        T.entity('medicine with courier', courier=True),
                        T.entity('not medicine no courier', courier=False),
                        T.entity('medicine with courier homeopathy', courier=True),
                        T.entity('not medicine with courier', courier=True),
                        T.entity('medicine no courier', courier=False),
                    ]
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

    def test_nav_category_tags(self):
        """
        Ищем теги
        """
        request = 'place=prime&hyperid=6000'
        response = self.report.request_json(request)

        self.assertFragmentIn(
            response,
            {
                "navnodes": [
                    {
                        "entity": "navnode",
                        "id": 6001,
                        "tags": [
                            "tag1",
                            "tag2",
                        ],
                    }
                ]
            },
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()
