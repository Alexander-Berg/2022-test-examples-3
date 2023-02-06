#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BlueOffer,
    HyperCategory,
    MarketSku,
    Region,
    Shop,
    DynamicWarehouseInfo,
)
from itertools import count
from core.types.autogen import b64url_md5
from core.testcase import TestCase, main
from core.types.hypercategory import Stream, CategoryStreamRecord


def get_counter(num=1):
    result = count(num)
    return result


wmd5counter = get_counter()
fesh_counter = get_counter()

ONE_P_FESH = next(fesh_counter)
THREE_P_FESH = next(fesh_counter)
FBS_FESH = next(fesh_counter)
FBS_FESH_SECOND = next(fesh_counter)
FBS_FESH_EXPRESS = next(fesh_counter)

ROOT_EATS_HID = 91307
FMCG_CATEG_ID = 13360738
FMCG_CATEG_ID_SECOND = 15870267
FMCG_ALLOWED_FOR_FBS_CATEGORY = 1111
COMMON_MODEL_ID = 1001

ROSTOV_FBS_WAREHOUSE_ID = 1501
MOSKOW_FBS_WAREHOUSE_ID = 1701
KRASNODAR_FBS_WAREHOUSE_ID = 1901
MOSKOW_FBS_EXPRESS_WAREHOUSE_ID = 403

SOFINO_WAREHOUSE_ID = 172
ROSTOV_WAREHOUSE_ID = 147
SPB_WAREHOUSE_ID = 301

MOSCOW_AND_MOSCOW_REGION = 1
MOSCOW = 213
ZELENOGRAD = 216
SAINT_PETERSBURG = 2
SAINT_PETERSBURG_AND_LENINGRAD_REGION = 10174
ROSTOV_NA_DONY_REGION = 11029
ROSTOV_DUGINO = 136443
KRASNODAR_REGION = 10995
KRASNODAR_ABINSK = 20183
ADYGEYA_REGION = 11004
ADYGEYA_PROGRESS = 123629


class Data:
    offers = []
    shops = []
    mskus = {}

    @staticmethod
    def create_offer(sku, price, warehouse_id, supplier_type, fulfillment_program, is_golden_matrix):
        if sku not in Data.mskus:
            Data.mskus[sku] = MarketSku(
                hyperid=COMMON_MODEL_ID,
                sku=sku,
                hid=FMCG_CATEG_ID_SECOND,
            )
        shop_id = next(fesh_counter)
        new_shop = Shop(
            fesh=shop_id,
            datafeed_id=shop_id,
            supplier_type=supplier_type,
            blue=Shop.BLUE_REAL,
            warehouse_id=warehouse_id,
            fulfillment_program=fulfillment_program,
        )
        new_offer = BlueOffer(
            price=price,
            feedid=new_shop.datafeed_id,
            waremd5=b64url_md5(next(wmd5counter)),
            is_golden_matrix=is_golden_matrix,
        )
        Data.shops += [new_shop]
        Data.offers += [new_offer]
        offers = Data.mskus[sku].get_blue_offers()
        offers += [new_offer]
        return new_offer

    @staticmethod
    def create_fby_offer(sku, price, warehouse_id):
        return Data.create_offer(sku, price, warehouse_id, Shop.THIRD_PARTY, True, False)

    @staticmethod
    def create_fbs_offer(sku, price, warehouse_id):
        return Data.create_offer(sku, price, warehouse_id, Shop.THIRD_PARTY, False, False)

    @staticmethod
    def create_corefix_fby_offer(sku, price, warehouse_id):
        return Data.create_offer(sku, price, warehouse_id, Shop.THIRD_PARTY, True, True)

    @staticmethod
    def create_corefix_fbs_offer(sku, price, warehouse_id):
        return Data.create_offer(sku, price, warehouse_id, Shop.THIRD_PARTY, False, True)


class T(TestCase):
    """
    Набор тестов на проверку скрытия fbs офферов при наличии 1p или fby в группе категорий fmcg
    """

    fmcg_1p_offer = BlueOffer(price=101, feedid=ONE_P_FESH, waremd5="B001-100000-FEED-1_1PQ")
    fmcg_fby_offer = BlueOffer(price=102, feedid=THREE_P_FESH, waremd5="B002-100000-FEED-3FBYQ")
    fmcg_fbs_offer = BlueOffer(price=100, feedid=FBS_FESH, waremd5="B003-100000-FEED-7FBSQ")
    fmcg_fbs_offer_second_supplier = BlueOffer(price=95, feedid=FBS_FESH_SECOND, waremd5="B103-100000-FEED-7FBSQ")
    fmcg_fbs_offer_express = BlueOffer(
        price=97, feedid=FBS_FESH_EXPRESS, is_express=True, waremd5="B203-100000-FEED-7FBSQ"
    )
    fmcg_fbs_offer_eda = BlueOffer(
        price=96, feedid=FBS_FESH_EXPRESS, is_eda_retail=True, waremd5="B303-100000-FEED-7FBSQ"
    )

    fmcg_alowed_1p_offer = BlueOffer(price=102, feedid=ONE_P_FESH, waremd5="B004-100001-FEED-1_1PQ")
    fmcg_alowed_fby_offer = BlueOffer(price=101, feedid=THREE_P_FESH, waremd5="B005-100001-FEED-3FBYQ")
    fmcg_alowed_fbs_offer = BlueOffer(price=100, feedid=FBS_FESH, waremd5="B006-100001-FEED-7FBSQ")

    fmcg_lonely_fbs_offer_golden = BlueOffer(price=100, feedid=FBS_FESH, waremd5="B007-100001-FEED-7FBSQ")

    def assert_offers_prime(self, response, offers, exist):
        for offer in offers:
            if exist:
                self.assertFragmentIn(response, {'search': {'results': [{'wareId': offer.waremd5}]}})
            else:
                self.assertFragmentNotIn(response, {'search': {'results': [{'wareId': offer.waremd5}]}})

    def assert_offers_buybox(self, response, offers, exist):
        for offer in offers:
            if exist:
                self.assertFragmentIn(response, {'buyboxDebug': {'Offers': [{'WareMd5': offer.waremd5}]}})
            else:
                self.assertFragmentNotIn(response, {'buyboxDebug': {'Offers': [{'WareMd5': offer.waremd5}]}})

    def assert_offers_rejected(self, response, offers, exist):
        for offer in offers:
            if exist:
                self.assertFragmentIn(
                    response,
                    {
                        'buyboxDebug': {
                            'RejectedOffers': [{"Offer": {'WareMd5': offer.waremd5}}],
                        }
                    },
                )
            else:
                self.assertFragmentNotIn(
                    response,
                    {
                        'buyboxDebug': {
                            'RejectedOffers': [{"Offer": {'WareMd5': offer.waremd5}}],
                        }
                    },
                )

    def request_product_offers(self, req, flags, rid):
        base_request = 'place=productoffers&enable-foodtech-offers=eda_retail&offers-set=defaultList%2ClistCpa&show-cutprice=1&debug=da&'
        return self.report.request_json(base_request + req + '&rids={}&rearr-factors={}'.format(rid, flags))

    def request_prime(self, hid, flags, rid):
        base_request = 'place=prime&enable-foodtech-offers=eda_retail&offers-set=defaultList%2ClistCpa&show-cutprice=1'
        return self.report.request_json(base_request + '&hid={}&rids={}&rearr-factors={}'.format(hid, rid, flags))

    @classmethod
    def prepare_common(cls):
        cls.settings.market_access_settings.enabled = True
        cls.settings.market_access_settings.download_catstreams = True
        cls.index.category_streams += [
            CategoryStreamRecord(ROOT_EATS_HID, Stream.FMCG.value),
            CategoryStreamRecord(FMCG_CATEG_ID, Stream.FMCG.value),
            CategoryStreamRecord(FMCG_CATEG_ID_SECOND, Stream.FMCG.value),
        ]
        for id, msku in Data.mskus.items():
            cls.index.mskus += [msku]
        cls.index.shops += Data.shops
        cls.index.regiontree += [
            Region(
                rid=3,
                children=[
                    Region(
                        rid=MOSCOW_AND_MOSCOW_REGION,
                        children=[
                            Region(
                                rid=MOSCOW,
                                children=[
                                    Region(rid=ZELENOGRAD, children=[]),
                                ],
                            ),
                        ],
                    ),
                ],
            ),
            Region(
                rid=17,
                children=[
                    Region(
                        rid=SAINT_PETERSBURG_AND_LENINGRAD_REGION,
                        children=[
                            Region(rid=SAINT_PETERSBURG, children=[]),
                        ],
                    ),
                ],
            ),
            Region(
                rid=26,
                children=[
                    Region(
                        rid=ADYGEYA_REGION,
                        children=[
                            Region(rid=ADYGEYA_PROGRESS, children=[]),
                        ],
                    ),
                    Region(
                        rid=KRASNODAR_REGION,
                        children=[
                            Region(rid=KRASNODAR_ABINSK, children=[]),
                        ],
                    ),
                    Region(
                        rid=ROSTOV_NA_DONY_REGION,
                        children=[
                            Region(rid=ROSTOV_DUGINO, children=[]),
                        ],
                    ),
                ],
            ),
        ]

        cls.settings.lms_autogenerate = False
        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=ROSTOV_FBS_WAREHOUSE_ID, home_region=ROSTOV_NA_DONY_REGION),
            DynamicWarehouseInfo(id=MOSKOW_FBS_WAREHOUSE_ID, home_region=MOSCOW_AND_MOSCOW_REGION),
            DynamicWarehouseInfo(
                id=MOSKOW_FBS_EXPRESS_WAREHOUSE_ID, is_express=True, home_region=MOSCOW_AND_MOSCOW_REGION
            ),
            DynamicWarehouseInfo(id=KRASNODAR_FBS_WAREHOUSE_ID, home_region=KRASNODAR_REGION),
        ]

        cls.index.hypertree += [
            HyperCategory(
                hid=ROOT_EATS_HID,
                name='ROOT_EATS_HID',
                children=[
                    HyperCategory(
                        hid=FMCG_CATEG_ID,
                        name='FMCG',
                        children=[
                            HyperCategory(hid=FMCG_ALLOWED_FOR_FBS_CATEGORY, name='FMCG FOR FBS'),
                        ],
                    ),
                    HyperCategory(
                        hid=FMCG_CATEG_ID_SECOND,
                        name='FMCG_SECOND',
                        children=[],
                    ),
                ],
            )
        ]

    @classmethod
    def prepare_default_region_behavior(cls):
        cls.index.shops += [
            Shop(
                fesh=ONE_P_FESH,
                datafeed_id=ONE_P_FESH,
                name="1P Магазин",
                supplier_type=Shop.FIRST_PARTY,
                blue=Shop.BLUE_REAL,
                warehouse_id=SOFINO_WAREHOUSE_ID,
                fulfillment_program=True,
            ),
            Shop(
                fesh=THREE_P_FESH,
                datafeed_id=THREE_P_FESH,
                name="FBY поставщик",
                supplier_type=Shop.THIRD_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                warehouse_id=SOFINO_WAREHOUSE_ID,
                fulfillment_program=True,
            ),
            Shop(
                fesh=FBS_FESH,
                datafeed_id=FBS_FESH,
                name="FBS магазин",
                supplier_type=Shop.THIRD_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                warehouse_id=401,
                fulfillment_program=False,
            ),
            Shop(
                fesh=FBS_FESH_SECOND,
                datafeed_id=FBS_FESH_SECOND,
                name="FBS магазин 2",
                supplier_type=Shop.THIRD_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                warehouse_id=402,
                fulfillment_program=False,
            ),
            Shop(
                fesh=FBS_FESH_EXPRESS,
                datafeed_id=FBS_FESH_EXPRESS,
                name="FBS магазин express",
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                warehouse_id=403,
                fulfillment_program=False,
                with_express_warehouse=True,
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                hyperid=1,
                sku=100000,
                hid=FMCG_CATEG_ID,
                blue_offers=[
                    T.fmcg_1p_offer,
                    T.fmcg_fby_offer,
                    T.fmcg_fbs_offer,
                    T.fmcg_fbs_offer_second_supplier,
                    T.fmcg_fbs_offer_express,
                    T.fmcg_fbs_offer_eda,
                ],
            ),
            MarketSku(
                hyperid=2,
                sku=100001,
                hid=FMCG_ALLOWED_FOR_FBS_CATEGORY,
                blue_offers=[
                    T.fmcg_alowed_1p_offer,
                    T.fmcg_alowed_fby_offer,
                    T.fmcg_alowed_fbs_offer,
                ],
            ),
            MarketSku(
                hyperid=3,
                sku=100002,
                hid=FMCG_CATEG_ID,
                blue_offers=[T.fmcg_lonely_fbs_offer_golden],
                is_golden_matrix=True,
            ),
        ]

        cls.index.fmcg_parameters_config_records = {
            'dbs_pessimie_categories': [FMCG_CATEG_ID, FMCG_CATEG_ID_SECOND],
            'fbs_pessimie_categories': [FMCG_CATEG_ID, FMCG_CATEG_ID_SECOND],
        }

    def test_fbs_not_disabled_with_rearr_has_zero(self):
        """
        С отключеным флагом fbs не удаляется
        """
        allowed_offers = [
            T.fmcg_1p_offer,
            T.fmcg_fbs_offer,
            T.fmcg_fbs_offer_second_supplier,
            T.fmcg_fbs_offer_express,
            T.fmcg_fbs_offer_eda,
        ]

        for group in ["", "&grhow=supplier"]:
            response = self.request_product_offers(
                req='market-sku=100000{}'.format(group), flags='market_fbs_pessimize_stage=0', rid=MOSCOW
            )
            self.assert_offers_buybox(response, allowed_offers, True)
            self.assert_offers_rejected(response, allowed_offers, False)

    def test_fbs_disabled_with_rearr_enabled(self):
        """
        С включеным флагом при наличии 1p или fby удаляем fbs, но не еду
        так же будет отключено вычесление express дефолтного оффера
        """
        allowed_offers = [T.fmcg_1p_offer, T.fmcg_fbs_offer_eda]
        disabled_offers = [
            T.fmcg_fbs_offer,
            T.fmcg_fbs_offer_second_supplier,
            T.fmcg_fbs_offer_express,
        ]
        for group in ["", "&grhow=supplier"]:
            response = self.request_product_offers(
                req='market-sku=100000{}'.format(group), flags='market_fbs_pessimize_stage=1', rid=MOSCOW
            )
            self.assert_offers_buybox(response, allowed_offers, True)
            self.assert_offers_buybox(response, disabled_offers, False)

    def test_showed_express_do(self):
        """
        По умолчанию ДО с экспрессом скрыт
        с флагом market_fbs_pessimize_disable_express_do=0 можно получить Express ДО на выдачу
        """
        response = self.request_product_offers(
            req='market-sku=100000',
            flags='market_fbs_pessimize_stage=1;market_fbs_pessimize_disable_express_do=0',
            rid=MOSCOW,
        )
        allowed_offers = [
            T.fmcg_fbs_offer_express,
        ]
        self.assert_offers_buybox(response, allowed_offers, True)

    def test_only_fbs_enabled_with_rearr_enabled(self):
        """
        С включеным флагом при отсутствии 1p или fby остается fbs
        """
        response = self.request_product_offers(
            req='market-sku=100002', flags='market_fbs_pessimize_stage=1', rid=MOSCOW
        )
        allowed_offers = [
            T.fmcg_lonely_fbs_offer_golden,
        ]
        self.assert_offers_buybox(response, allowed_offers, True)
        self.assert_offers_rejected(response, allowed_offers, False)

    def test_fbs_not_disabled_with_not_fmcg_category(self):
        """
        С включеным флагом, но не в fmcg категории fbs не удаляется
        """
        response = self.request_product_offers(
            req='market-sku=100001', flags='market_fbs_pessimize_stage=1', rid=MOSCOW
        )
        allowed_offers = [
            T.fmcg_alowed_1p_offer,
            T.fmcg_alowed_fby_offer,
            T.fmcg_alowed_fbs_offer,
        ]
        self.assert_offers_buybox(response, allowed_offers, True)
        self.assert_offers_rejected(response, allowed_offers, False)

    def test_fbs_filter_prime_enabld(self):
        """
        С включеным флагом, на прайм попадут самые дешевые офферы без fbs в fmcg категории
        не локальный FBS попадает на выдачу т.к. в sku единственный, а sku is_golden_matrix=True
        """
        response = self.request_prime(FMCG_CATEG_ID, flags='market_fbs_pessimize_stage=1', rid=MOSCOW)
        allowed_offers = [T.fmcg_1p_offer, T.fmcg_alowed_fbs_offer, T.fmcg_lonely_fbs_offer_golden]
        disabled_offers = [
            T.fmcg_fbs_offer,
            T.fmcg_fbs_offer_second_supplier,
            T.fmcg_fbs_offer_express,
        ]
        self.assert_offers_prime(response, allowed_offers, True)
        self.assert_offers_prime(response, disabled_offers, False)

    def test_fbs_filter_prime_disabled(self):
        """
        С выключеным флагом, на прайм попадут самые дешевые офферы вместе с fbs из fmcg категории
        """
        response = self.request_prime(FMCG_CATEG_ID, flags='market_fbs_pessimize_stage=0', rid=MOSCOW)
        allowed_offers = [T.fmcg_fbs_offer, T.fmcg_alowed_fbs_offer, T.fmcg_lonely_fbs_offer_golden]
        self.assert_offers_prime(response, allowed_offers, True)

    """
    Секция проверки логики НЕ corefix в Москве
    """

    class make_sofino_rostov_fby_proiority_in_moskow:
        fby_sofino = Data.create_fby_offer(sku=200001, price=11, warehouse_id=SOFINO_WAREHOUSE_ID)
        fby_rostov = Data.create_fby_offer(sku=200001, price=10, warehouse_id=ROSTOV_WAREHOUSE_ID)
        fbs_moskow = Data.create_fbs_offer(sku=200001, price=9, warehouse_id=MOSKOW_FBS_WAREHOUSE_ID)
        fbs_no_local = Data.create_fbs_offer(sku=200001, price=8, warehouse_id=ROSTOV_FBS_WAREHOUSE_ID)

    def test_sofino_rostov_proiority_in_moskow(self):
        """
        Удаляем все офферы кроме приоритетного локального FBY оффера
        """
        allowed_offers = [
            T.make_sofino_rostov_fby_proiority_in_moskow.fby_sofino,
        ]
        disabled_offers = [
            T.make_sofino_rostov_fby_proiority_in_moskow.fbs_moskow,
            T.make_sofino_rostov_fby_proiority_in_moskow.fby_rostov,
            T.make_sofino_rostov_fby_proiority_in_moskow.fbs_no_local,
        ]
        # для москвы 1 или 2 stage не имеет значения
        for stage in [1, 2]:
            response = self.request_product_offers(
                req='market-sku=200001', flags='market_fbs_pessimize_stage={}'.format(stage), rid=MOSCOW
            )
            self.assert_offers_buybox(response, allowed_offers, True)
            self.assert_offers_buybox(response, disabled_offers, False)

    class make_show_local_fbs_when_have_no_local_fby:
        fby_rostov = Data.create_fby_offer(sku=200002, price=10, warehouse_id=ROSTOV_WAREHOUSE_ID)
        fbs_moskow = Data.create_fbs_offer(sku=200002, price=9, warehouse_id=MOSKOW_FBS_WAREHOUSE_ID)
        fbs_rostov = Data.create_fbs_offer(sku=200002, price=8, warehouse_id=ROSTOV_FBS_WAREHOUSE_ID)

    def test_show_local_fbs_when_have_no_local_fby(self):
        """
        Локальный FBS приоритетнее не локального FBY
        """
        allowed_offers = [
            T.make_show_local_fbs_when_have_no_local_fby.fbs_moskow,
        ]
        disabled_offers = [
            T.make_show_local_fbs_when_have_no_local_fby.fby_rostov,
            T.make_show_local_fbs_when_have_no_local_fby.fbs_rostov,
        ]
        # для москвы 1 или 2 stage не имеет значения
        for stage in [1, 2]:
            response = self.request_product_offers(
                req='market-sku=200002', flags='market_fbs_pessimize_stage={}'.format(stage), rid=MOSCOW
            )
            self.assert_offers_buybox(response, allowed_offers, True)
            self.assert_offers_buybox(response, disabled_offers, False)

    class make_show_local_fbs_when_have_no_local_fbs:
        fbs_rostov = Data.create_fbs_offer(sku=200003, price=9, warehouse_id=ROSTOV_FBS_WAREHOUSE_ID)
        fbs_moskow = Data.create_fbs_offer(sku=200003, price=10, warehouse_id=MOSKOW_FBS_WAREHOUSE_ID)

    def test_show_local_fbs_when_have_no_local_fbs(self):
        """
        Оффер FBS с ростовского склада пессимизируется т.к. есть локальный FBS
        """
        allowed_offers = [
            T.make_show_local_fbs_when_have_no_local_fbs.fbs_moskow,
        ]
        disabled_offers = [
            T.make_show_local_fbs_when_have_no_local_fbs.fbs_rostov,
        ]

        for group in ["", "&grhow=supplier"]:
            response = self.request_product_offers(
                req='market-sku=200003{}'.format(group), flags='market_fbs_pessimize_stage=1', rid=MOSCOW
            )
            self.assert_offers_buybox(response, allowed_offers, True)
            self.assert_offers_buybox(response, disabled_offers, False)

    class make_no_show_when_have_no_local_offers:
        fbs_rostov = Data.create_fbs_offer(sku=200004, price=9, warehouse_id=ROSTOV_FBS_WAREHOUSE_ID)
        fby_rostov = Data.create_fby_offer(sku=200004, price=10, warehouse_id=ROSTOV_WAREHOUSE_ID)

    def test_no_show_when_have_no_local_offers(self):
        """
        Не показываем офферы если нет локальных
        """
        disabled_offers = [
            T.make_no_show_when_have_no_local_offers.fbs_rostov,
            T.make_no_show_when_have_no_local_offers.fby_rostov,
        ]
        response = self.request_product_offers(
            req='market-sku=200004', flags='market_fbs_pessimize_stage=1', rid=MOSCOW
        )
        self.assert_offers_buybox(response, disabled_offers, False)

    """
    Секция проверки логики corefix в Москве
    """

    class make_corefix_sofino_rostov_fby_proiority_in_moskow:
        fby_sofino = Data.create_corefix_fby_offer(sku=200005, price=11, warehouse_id=SOFINO_WAREHOUSE_ID)
        fby_rostov = Data.create_corefix_fby_offer(sku=200005, price=10, warehouse_id=ROSTOV_WAREHOUSE_ID)
        fbs_moskow = Data.create_corefix_fbs_offer(sku=200005, price=9, warehouse_id=MOSKOW_FBS_WAREHOUSE_ID)
        fbs_no_local = Data.create_corefix_fbs_offer(sku=200005, price=8, warehouse_id=ROSTOV_FBS_WAREHOUSE_ID)

    def test_corefix_sofino_rostov_proiority_in_moskow(self):
        """
        Удаляем все офферы кроме приоритетного локального corefix FBY оффера
        """
        allowed_offers = [
            T.make_corefix_sofino_rostov_fby_proiority_in_moskow.fby_sofino,
        ]
        disabled_offers = [
            T.make_corefix_sofino_rostov_fby_proiority_in_moskow.fbs_moskow,
            T.make_corefix_sofino_rostov_fby_proiority_in_moskow.fby_rostov,
            T.make_corefix_sofino_rostov_fby_proiority_in_moskow.fbs_no_local,
        ]
        response = self.request_product_offers(
            req='market-sku=200005', flags='market_fbs_pessimize_stage=1', rid=MOSCOW
        )
        self.assert_offers_buybox(response, allowed_offers, True)
        self.assert_offers_buybox(response, disabled_offers, False)

    class make_corefix_show_local_fbs_when_have_no_local_fby:
        fby_rostov = Data.create_corefix_fby_offer(sku=200006, price=10, warehouse_id=ROSTOV_WAREHOUSE_ID)
        fbs_moskow = Data.create_corefix_fbs_offer(sku=200006, price=9, warehouse_id=MOSKOW_FBS_WAREHOUSE_ID)
        fbs_rostov = Data.create_corefix_fbs_offer(sku=200006, price=8, warehouse_id=ROSTOV_FBS_WAREHOUSE_ID)

    def test_corefix_show_local_fbs_when_have_no_local_fby(self):
        """
        В corefix не локальный FBY приоритетнее локального FBS
        """
        allowed_offers = [
            T.make_corefix_show_local_fbs_when_have_no_local_fby.fby_rostov,
        ]
        disabled_offers = [
            T.make_corefix_show_local_fbs_when_have_no_local_fby.fbs_moskow,
            T.make_corefix_show_local_fbs_when_have_no_local_fby.fbs_rostov,
        ]
        response = self.request_product_offers(
            req='market-sku=200006', flags='market_fbs_pessimize_stage=1', rid=MOSCOW
        )
        self.assert_offers_buybox(response, allowed_offers, True)
        self.assert_offers_buybox(response, disabled_offers, False)

    class make_corefix_show_local_fbs_when_have_no_local_fbs:
        fbs_rostov = Data.create_corefix_fbs_offer(sku=200007, price=9, warehouse_id=ROSTOV_FBS_WAREHOUSE_ID)
        fbs_moskow = Data.create_corefix_fbs_offer(sku=200007, price=10, warehouse_id=MOSKOW_FBS_WAREHOUSE_ID)

    def test_corefix_show_local_fbs_when_have_no_local_fbs(self):
        """
        Corefix оффер FBS с ростовского склада пессимизируется т.к. есть локальный сorefix FBS
        """
        allowed_offers = [
            T.make_corefix_show_local_fbs_when_have_no_local_fbs.fbs_moskow,
        ]
        disabled_offers = [
            T.make_corefix_show_local_fbs_when_have_no_local_fbs.fbs_rostov,
        ]
        response = self.request_product_offers(
            req='market-sku=200007', flags='market_fbs_pessimize_stage=1', rid=MOSCOW
        )
        self.assert_offers_buybox(response, allowed_offers, True)
        self.assert_offers_buybox(response, disabled_offers, False)

    class make_corefix_show_no_local_fby_when_have_no_local_offers:
        fbs_rostov = Data.create_corefix_fbs_offer(sku=200008, price=9, warehouse_id=ROSTOV_FBS_WAREHOUSE_ID)
        fby_rostov = Data.create_corefix_fby_offer(sku=200008, price=10, warehouse_id=ROSTOV_WAREHOUSE_ID)

    def test_corefix_show_no_local_fby_when_have_no_local_offers(self):
        """
        Corefix не локальный FBY оффер доступен и приоритетнее не локального FBS
        """
        allowed_offers = [
            T.make_corefix_show_no_local_fby_when_have_no_local_offers.fby_rostov,
        ]
        disabled_offers = [
            T.make_corefix_show_no_local_fby_when_have_no_local_offers.fbs_rostov,
        ]
        response = self.request_product_offers(
            req='market-sku=200008', flags='market_fbs_pessimize_stage=1', rid=MOSCOW
        )
        self.assert_offers_buybox(response, allowed_offers, True)
        self.assert_offers_buybox(response, disabled_offers, False)

    class make_corefix_show_no_local_fbs_when_have_no_other_offers:
        fbs_rostov = Data.create_corefix_fbs_offer(sku=200009, price=9, warehouse_id=ROSTOV_FBS_WAREHOUSE_ID)

    def test_corefix_show_no_local_fbs_when_have_no_other_offers(self):
        """
        Corefix не локальный FBS остается при отсутствии иных вариантов
        """
        allowed_offers = [
            T.make_corefix_show_no_local_fbs_when_have_no_other_offers.fbs_rostov,
        ]
        response = self.request_product_offers(
            req='market-sku=200009', flags='market_fbs_pessimize_stage=1', rid=MOSCOW
        )
        self.assert_offers_buybox(response, allowed_offers, True)

    """
    Секция проверки логики НЕ corefix в регионах(везде кроме Москвы)
    На первом этапе возим не локальный FBY, в остальном поведение идентично Москве
    """

    class make_show_local_fby_krasnodar:
        fby_rostov = Data.create_fby_offer(sku=200010, price=11, warehouse_id=ROSTOV_WAREHOUSE_ID)
        fby_piter = Data.create_fby_offer(sku=200010, price=10, warehouse_id=SPB_WAREHOUSE_ID)
        fbs_krasnodar = Data.create_fbs_offer(sku=200010, price=9, warehouse_id=KRASNODAR_FBS_WAREHOUSE_ID)
        fbs_no_local = Data.create_fbs_offer(sku=200010, price=8, warehouse_id=ROSTOV_FBS_WAREHOUSE_ID)

    def test_show_local_fby_krasnodar(self):
        """
        Удаляем все офферы кроме приоритетного локального FBY оффера
        """
        allowed_offers = [
            T.make_show_local_fby_krasnodar.fby_rostov,
        ]
        disabled_offers = [
            T.make_show_local_fby_krasnodar.fby_piter,
            T.make_show_local_fby_krasnodar.fbs_krasnodar,
            T.make_show_local_fby_krasnodar.fbs_no_local,
        ]
        # для регионов и москвы локальный fby одинаково приоритетен на 1 и 2 этапе
        for stage in [1, 2]:
            response = self.request_product_offers(
                req='market-sku=200010', flags='market_fbs_pessimize_stage={}'.format(stage), rid=KRASNODAR_ABINSK
            )
            self.assert_offers_buybox(response, allowed_offers, True)
            self.assert_offers_buybox(response, disabled_offers, False)

    class make_show_no_local_fby_when_have_no_local_fby_in_region:
        fby_piter = Data.create_fby_offer(sku=200011, price=10, warehouse_id=SPB_WAREHOUSE_ID)
        fbs_krasnodar = Data.create_fbs_offer(sku=200011, price=9, warehouse_id=KRASNODAR_FBS_WAREHOUSE_ID)
        fbs_no_local = Data.create_fbs_offer(sku=200011, price=8, warehouse_id=ROSTOV_FBS_WAREHOUSE_ID)

    def test_show_no_local_fby_when_have_no_local_fby_in_region(self):
        """
        Не локальный FBY приоритетнее локального FBS на первом этапе в регионах
        """
        allowed_offers = [
            T.make_show_no_local_fby_when_have_no_local_fby_in_region.fby_piter,
        ]
        disabled_offers = [
            T.make_show_no_local_fby_when_have_no_local_fby_in_region.fbs_krasnodar,
            T.make_show_no_local_fby_when_have_no_local_fby_in_region.fbs_no_local,
        ]
        response = self.request_product_offers(
            req='market-sku=200011', flags='market_fbs_pessimize_stage=1', rid=KRASNODAR_ABINSK
        )
        self.assert_offers_buybox(response, allowed_offers, True)
        self.assert_offers_buybox(response, disabled_offers, False)

    def test_show_local_fbs_when_have_no_local_fby_in_region_stage_2(self):
        """
        На втором этапе локальный FBS приоритетнее не локального FBY в ригионах так же как и в москве
        """
        allowed_offers = [
            T.make_show_no_local_fby_when_have_no_local_fby_in_region.fbs_krasnodar,
        ]
        disabled_offers = [
            T.make_show_no_local_fby_when_have_no_local_fby_in_region.fby_piter,
            T.make_show_no_local_fby_when_have_no_local_fby_in_region.fbs_no_local,
        ]
        response = self.request_product_offers(
            req='market-sku=200011', flags='market_fbs_pessimize_stage=2', rid=KRASNODAR_ABINSK
        )
        self.assert_offers_buybox(response, allowed_offers, True)
        self.assert_offers_buybox(response, disabled_offers, False)

    """
    Секция проверки логики corefix в регионах
    """

    class make_corefix_show_no_local_fbs_when_have_no_other_offers_in_region:
        fbs_rostov = Data.create_corefix_fbs_offer(sku=200012, price=9, warehouse_id=ROSTOV_FBS_WAREHOUSE_ID)

    def test_corefix_show_no_local_fbs_when_have_no_other_offers_in_region(self):
        """
        Corefix не локальный FBS остается при отсутствии иных вариантов
        """
        allowed_offers = [
            T.make_corefix_show_no_local_fbs_when_have_no_other_offers_in_region.fbs_rostov,
        ]
        # не важно на каком этапе
        for stage in [1, 2]:
            response = self.request_product_offers(
                req='market-sku=200012', flags='market_fbs_pessimize_stage={}'.format(stage), rid=KRASNODAR_ABINSK
            )
            self.assert_offers_buybox(response, allowed_offers, True)


if __name__ == '__main__':
    main()
