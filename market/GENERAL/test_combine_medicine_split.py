#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    DeliveryBucket,
    MarketSku,
    Model,
    Offer,
    Outlet,
    OutletDeliveryOption,
    PickupBucket,
    PickupOption,
    PrescriptionManagementSystem,
    Region,
    Shop,
)
from core.types.offer import OfferDimensions
from core.testcase import TestCase, main


class _Rids:
    moscow = 213


class _DeliveryServices:
    internal = 99


class _Params:
    drugs_category_id = 15758037


class _Categories:
    medical_white_cpa = 1


class _Feshes:
    class _White:
        medical_1 = 10


class _Feeds:
    class _White:
        medical_1 = 100


class _ClientIds:
    class _White:
        medical_1 = 101


class _Warehouses:
    class _White:
        medical_1 = 1000


class _Shops:
    class _White:
        def create(
            fesh,
            datafeed_id,
            client_id,
            warehouse_id,
            priority_region,
            regions,
            name,
            prescription_system=PrescriptionManagementSystem.PS_NONE,
            medical_booking=None,
        ):
            return Shop(
                business_fesh=fesh + 1,
                fesh=fesh,
                datafeed_id=datafeed_id,
                client_id=client_id,
                warehouse_id=warehouse_id,
                priority_region=priority_region,
                regions=regions,
                cpa=Shop.CPA_REAL,
                cis=Shop.CIS_REAL,
                medicine_courier=True,
                name=name,
                prescription_management_system=prescription_system,
                medical_booking=medical_booking,
            )

        medical_1 = create(
            fesh=_Feshes._White.medical_1,
            datafeed_id=_Feeds._White.medical_1,
            client_id=_ClientIds._White.medical_1,
            warehouse_id=_Warehouses._White.medical_1,
            priority_region=_Rids.moscow,
            regions=[_Rids.moscow],
            name='White CPA CIS medical shop 1',
            prescription_system=PrescriptionManagementSystem.PS_MEDICATA,
            medical_booking=True,
        )


class _Outlets:
    class _White:
        medical_id = 5000

        def create(fesh, point_id):
            return Outlet(
                fesh=fesh,
                point_id=point_id,
                region=_Rids.moscow,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    shipper_id=103,
                    day_from=1,
                    day_to=1,
                    order_before=2,
                    work_in_holiday=True,
                    price=100,
                ),
                working_days=[i for i in range(10)],
            )

        medical = create(fesh=_Feshes._White.medical_1, point_id=medical_id)


class _Buckets:
    medical_id_pickup_white = 80000

    def create_pickup(bucket_id, outlet_id, shop, carriers):
        return PickupBucket(
            bucket_id=bucket_id,
            dc_bucket_id=bucket_id,
            fesh=shop.fesh,
            carriers=carriers,
            options=[
                PickupOption(
                    outlet_id=outlet_id,
                    price=100,
                    day_from=1,
                    day_to=3,
                )
            ],
            delivery_program=DeliveryBucket.REGULAR_PROGRAM,
        )

    medical_pickup_white = create_pickup(
        medical_id_pickup_white, _Outlets._White.medical_id, _Shops._White.medical_1, [_DeliveryServices.internal]
    )


class _Mskus:
    medical_cpa_id = 100000
    medical_cpa_id1 = 100001
    medical_cpa_id2 = 100002
    medical_cpa_id3 = 100003
    medical_cpa_id4 = 100004
    medical_cpa_id5 = 100005
    medical_cpa_id6 = 100006
    medical_cpa_id7 = 100007
    medical_cpa_id8 = 100008

    def create(title, sku, hyperid, blue_offers=None):
        return MarketSku(title=title, sku=sku, hyperid=hyperid, blue_offers=blue_offers)

    medical_cpa_no_delivery = create("Medical MSKU (cpa)", medical_cpa_id, _Categories.medical_white_cpa)
    medical_cpa_no_delivery1 = create("Medical MSKU1 (cpa)", medical_cpa_id1, _Categories.medical_white_cpa)
    medical_cpa_no_delivery2 = create("Medical MSKU2 (cpa)", medical_cpa_id2, _Categories.medical_white_cpa)
    medical_cpa_no_delivery3 = create("Medical MSKU3 (cpa)", medical_cpa_id3, _Categories.medical_white_cpa)
    medical_cpa_no_delivery4 = create("Medical MSKU4 (cpa)", medical_cpa_id4, _Categories.medical_white_cpa)
    medical_cpa_no_delivery5 = create("Medical MSKU5 (cpa)", medical_cpa_id5, _Categories.medical_white_cpa)
    medical_cpa_no_delivery6 = create("Medical MSKU6 (cpa)", medical_cpa_id6, _Categories.medical_white_cpa)
    medical_cpa_no_delivery7 = create("Medical MSKU7 (cpa)", medical_cpa_id7, _Categories.medical_white_cpa)
    medical_cpa_no_delivery8 = create("Medical MSKU8 (cpa)", medical_cpa_id8, _Categories.medical_white_cpa)


class _Models:
    def create(hid, msku):
        return Model(hid=hid, hyperid=msku.hyperid)

    medical_cpa = create(_Params.drugs_category_id, _Mskus.medical_cpa_no_delivery)


class _Offers:
    class _White:
        def create(
            waremd5,
            shop,
            supplier_id,
            msku,
            price,
            weight,
            dimensions,
            delivery_options,
            cpa,
            delivery_buckets=None,
            pickup_buckets=None,
            is_express=False,
            stock_store_count=10,
            is_medicine=False,
            is_psychotropic=False,
            is_narcotic=False,
            is_precursor=False,
            is_prescription=False,
            is_baa=False,
            is_medical_booking=False,
        ):
            return Offer(
                waremd5=waremd5,
                fesh=shop.fesh,
                feedid=shop.datafeed_id,
                supplier_id=supplier_id,
                sku=msku.sku,
                hyperid=msku.hyperid,
                price=price,
                weight=weight,
                dimensions=dimensions,
                delivery_options=delivery_options,
                stock_store_count=stock_store_count,
                cpa=cpa,
                delivery_buckets=delivery_buckets,
                pickup_buckets=pickup_buckets,
                is_express=is_express,
                is_medicine=is_medicine,
                is_psychotropic=is_psychotropic,
                is_narcotic=is_narcotic,
                is_precursor=is_precursor,
                is_prescription=is_prescription,
                is_baa=is_baa,
                is_medical_booking=is_medical_booking,
            )

        just_medicine = create(
            waremd5='just_medicine________g',
            shop=_Shops._White.medical_1,
            supplier_id=_ClientIds._White.medical_1,
            msku=_Mskus.medical_cpa_no_delivery,
            pickup_buckets=[_Buckets.medical_pickup_white.bucket_id],
            price=10,
            weight=1,
            dimensions=OfferDimensions(length=3, width=3, height=3),
            delivery_options=[],
            cpa=Offer.CPA_REAL,
            is_medicine=True,
        )

        medicine_and_prescription = create(
            waremd5='med_and_prescription_g',
            shop=_Shops._White.medical_1,
            supplier_id=_ClientIds._White.medical_1,
            msku=_Mskus.medical_cpa_no_delivery1,
            pickup_buckets=[_Buckets.medical_pickup_white.bucket_id],
            price=10,
            weight=1,
            dimensions=OfferDimensions(length=3, width=3, height=3),
            delivery_options=[],
            cpa=Offer.CPA_REAL,
            is_medicine=True,
            is_prescription=True,
        )

        medicine_and_psychotropic = create(
            waremd5='med_and_psychotropic_g',
            shop=_Shops._White.medical_1,
            supplier_id=_ClientIds._White.medical_1,
            msku=_Mskus.medical_cpa_no_delivery2,
            pickup_buckets=[_Buckets.medical_pickup_white.bucket_id],
            price=10,
            weight=1,
            dimensions=OfferDimensions(length=3, width=3, height=3),
            delivery_options=[],
            cpa=Offer.CPA_REAL,
            is_medicine=True,
            is_psychotropic=True,
        )

        medicine_and_narcotic = create(
            waremd5='med_and_narcotic_____g',
            shop=_Shops._White.medical_1,
            supplier_id=_ClientIds._White.medical_1,
            msku=_Mskus.medical_cpa_no_delivery3,
            pickup_buckets=[_Buckets.medical_pickup_white.bucket_id],
            price=10,
            weight=1,
            dimensions=OfferDimensions(length=3, width=3, height=3),
            delivery_options=[],
            cpa=Offer.CPA_REAL,
            is_medicine=True,
            is_narcotic=True,
        )

        medicine_and_precursor = create(
            waremd5='med_and_precursor____g',
            shop=_Shops._White.medical_1,
            supplier_id=_ClientIds._White.medical_1,
            msku=_Mskus.medical_cpa_no_delivery4,
            pickup_buckets=[_Buckets.medical_pickup_white.bucket_id],
            price=10,
            weight=1,
            dimensions=OfferDimensions(length=3, width=3, height=3),
            delivery_options=[],
            cpa=Offer.CPA_REAL,
            is_medicine=True,
            is_precursor=True,
        )

        just_prescription = create(
            waremd5='just_prescription____g',
            shop=_Shops._White.medical_1,
            supplier_id=_ClientIds._White.medical_1,
            msku=_Mskus.medical_cpa_no_delivery5,
            pickup_buckets=[_Buckets.medical_pickup_white.bucket_id],
            price=10,
            weight=1,
            dimensions=OfferDimensions(length=3, width=3, height=3),
            delivery_options=[],
            cpa=Offer.CPA_REAL,
            is_prescription=True,
        )

        just_psychotropic = create(
            waremd5='just_psychotropic____g',
            shop=_Shops._White.medical_1,
            supplier_id=_ClientIds._White.medical_1,
            msku=_Mskus.medical_cpa_no_delivery6,
            pickup_buckets=[_Buckets.medical_pickup_white.bucket_id],
            price=10,
            weight=1,
            dimensions=OfferDimensions(length=3, width=3, height=3),
            delivery_options=[],
            cpa=Offer.CPA_REAL,
            is_psychotropic=True,
        )

        just_narcotic = create(
            waremd5='just_narcotic________g',
            shop=_Shops._White.medical_1,
            supplier_id=_ClientIds._White.medical_1,
            msku=_Mskus.medical_cpa_no_delivery7,
            pickup_buckets=[_Buckets.medical_pickup_white.bucket_id],
            price=10,
            weight=1,
            dimensions=OfferDimensions(length=3, width=3, height=3),
            delivery_options=[],
            cpa=Offer.CPA_REAL,
            is_narcotic=True,
        )

        just_precursor = create(
            waremd5='just_precursor_______g',
            shop=_Shops._White.medical_1,
            supplier_id=_ClientIds._White.medical_1,
            msku=_Mskus.medical_cpa_no_delivery8,
            pickup_buckets=[_Buckets.medical_pickup_white.bucket_id],
            price=10,
            weight=1,
            dimensions=OfferDimensions(length=3, width=3, height=3),
            delivery_options=[],
            cpa=Offer.CPA_REAL,
            is_precursor=True,
        )


class T(TestCase):
    @classmethod
    def prepare_regions(cls):
        cls.index.regiontree += [Region(rid=_Rids.moscow, name="Moscow", tz_offset=10800)]

    @classmethod
    def prepare_shops(cls):
        cls.index.shops += [
            _Shops._White.medical_1,
        ]

    @classmethod
    def prepare_mskus(cls):
        cls.index.mskus += [
            _Mskus.medical_cpa_no_delivery,
            _Mskus.medical_cpa_no_delivery1,
            _Mskus.medical_cpa_no_delivery2,
            _Mskus.medical_cpa_no_delivery3,
            _Mskus.medical_cpa_no_delivery4,
            _Mskus.medical_cpa_no_delivery5,
            _Mskus.medical_cpa_no_delivery6,
            _Mskus.medical_cpa_no_delivery7,
            _Mskus.medical_cpa_no_delivery8,
        ]

    @classmethod
    def prepare_offers(cls):
        cls.index.offers += [
            _Offers._White.just_medicine,
            _Offers._White.medicine_and_prescription,
            _Offers._White.medicine_and_psychotropic,
            _Offers._White.medicine_and_narcotic,
            _Offers._White.medicine_and_precursor,
            _Offers._White.just_prescription,
            _Offers._White.just_psychotropic,
            _Offers._White.just_narcotic,
            _Offers._White.just_precursor,
        ]

    @classmethod
    def prepare_models(cls):
        cls.index.models += [
            _Models.medical_cpa,
        ]

    @classmethod
    def prepare_pickup_buckets(cls):
        cls.index.pickup_buckets += [_Buckets.medical_pickup_white]

    @classmethod
    def prepare_outlets(cls):
        cls.index.outlets += [_Outlets._White.medical]

    def test_just_medicine(self):
        request = (
            'place=combine'
            '&split-strategy=split-medicine'
            '&pp=18'
            '&rids=213'
            '&rearr-factors=market_not_prescription_drugs_delivery=1'
            '&rearr-factors=enable_prescription_drugs_delivery=1'
        )

        offers_list = ('&offers-list={ware_id_1}:1;msku:{msku_1};cart_item_id:1').format(
            ware_id_1=_Offers._White.just_medicine.ware_md5,
            msku_1=_Offers._White.just_medicine.sku,
        )

        response = self.report.request_json(request + offers_list)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "buckets": [
                                {
                                    "IsMedicalParcel": True,
                                    "offers": [
                                        {
                                            "cartItemIds": [1],
                                            "marketSku": _Offers._White.just_medicine.sku,
                                            "replacedId": _Offers._White.just_medicine.ware_md5,
                                            "wareId": _Offers._White.just_medicine.ware_md5,
                                        }
                                    ],
                                }
                            ],
                        }
                    ]
                }
            },
            allow_different_len=False,
        )

    def test_medicine_and_prescription(self):
        request = (
            'place=combine'
            '&split-strategy=split-medicine'
            '&pp=18'
            '&rids=213'
            '&rearr-factors=market_not_prescription_drugs_delivery=1'
            '&rearr-factors=enable_prescription_drugs_delivery=1'
        )

        offers_list = ('&offers-list={ware_id_1}:1;msku:{msku_1};cart_item_id:1').format(
            ware_id_1=_Offers._White.medicine_and_prescription.ware_md5,
            msku_1=_Offers._White.medicine_and_prescription.sku,
        )

        response = self.report.request_json(request + offers_list)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "buckets": [
                                {
                                    "IsMedicalParcel": True,
                                    "offers": [
                                        {
                                            "cartItemIds": [1],
                                            "marketSku": _Offers._White.medicine_and_prescription.sku,
                                            "replacedId": _Offers._White.medicine_and_prescription.ware_md5,
                                            "wareId": _Offers._White.medicine_and_prescription.ware_md5,
                                        }
                                    ],
                                }
                            ],
                        }
                    ]
                }
            },
            allow_different_len=False,
        )

    def test_medicine_and_psychotropic(self):
        request = (
            'place=combine'
            '&split-strategy=split-medicine'
            '&pp=18'
            '&rids=213'
            '&rearr-factors=market_not_prescription_drugs_delivery=1'
            '&rearr-factors=enable_prescription_drugs_delivery=1'
        )

        offers_list = ('&offers-list={ware_id_1}:1;msku:{msku_1};cart_item_id:1').format(
            ware_id_1=_Offers._White.medicine_and_psychotropic.ware_md5,
            msku_1=_Offers._White.medicine_and_psychotropic.sku,
        )

        response = self.report.request_json(request + offers_list)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "buckets": [
                                {
                                    "IsMedicalParcel": True,
                                    "offers": [
                                        {
                                            "cartItemIds": [1],
                                            "marketSku": _Offers._White.medicine_and_psychotropic.sku,
                                            "replacedId": _Offers._White.medicine_and_psychotropic.ware_md5,
                                            "wareId": _Offers._White.medicine_and_psychotropic.ware_md5,
                                        }
                                    ],
                                }
                            ],
                        }
                    ]
                }
            },
            allow_different_len=False,
        )

    def test_medicine_and_narcotic(self):
        request = (
            'place=combine'
            '&split-strategy=split-medicine'
            '&pp=18'
            '&rids=213'
            '&rearr-factors=market_not_prescription_drugs_delivery=1'
            '&rearr-factors=enable_prescription_drugs_delivery=1'
        )

        offers_list = ('&offers-list={ware_id_1}:1;msku:{msku_1};cart_item_id:1').format(
            ware_id_1=_Offers._White.medicine_and_narcotic.ware_md5,
            msku_1=_Offers._White.medicine_and_narcotic.sku,
        )

        response = self.report.request_json(request + offers_list)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "buckets": [
                                {
                                    "IsMedicalParcel": True,
                                    "offers": [
                                        {
                                            "cartItemIds": [1],
                                            "marketSku": _Offers._White.medicine_and_narcotic.sku,
                                            "replacedId": _Offers._White.medicine_and_narcotic.ware_md5,
                                            "wareId": _Offers._White.medicine_and_narcotic.ware_md5,
                                        }
                                    ],
                                }
                            ],
                        }
                    ]
                }
            },
            allow_different_len=False,
        )

    def test_medicine_and_precursor(self):
        request = (
            'place=combine'
            '&split-strategy=split-medicine'
            '&pp=18'
            '&rids=213'
            '&rearr-factors=market_not_prescription_drugs_delivery=1'
            '&rearr-factors=enable_prescription_drugs_delivery=1'
        )

        offers_list = ('&offers-list={ware_id_1}:1;msku:{msku_1};cart_item_id:1').format(
            ware_id_1=_Offers._White.medicine_and_precursor.ware_md5,
            msku_1=_Offers._White.medicine_and_precursor.sku,
        )

        response = self.report.request_json(request + offers_list)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "buckets": [
                                {
                                    "IsMedicalParcel": True,
                                    "offers": [
                                        {
                                            "cartItemIds": [1],
                                            "marketSku": _Offers._White.medicine_and_precursor.sku,
                                            "replacedId": _Offers._White.medicine_and_precursor.ware_md5,
                                            "wareId": _Offers._White.medicine_and_precursor.ware_md5,
                                        }
                                    ],
                                }
                            ],
                        }
                    ]
                }
            },
            allow_different_len=False,
        )

    def test_just_prescription(self):
        request = (
            'place=combine'
            '&split-strategy=split-medicine'
            '&pp=18'
            '&rids=213'
            '&rearr-factors=market_not_prescription_drugs_delivery=1'
            '&rearr-factors=enable_prescription_drugs_delivery=1'
        )

        offers_list = ('&offers-list={ware_id_1}:1;msku:{msku_1};cart_item_id:1').format(
            ware_id_1=_Offers._White.just_prescription.ware_md5,
            msku_1=_Offers._White.just_prescription.sku,
        )

        response = self.report.request_json(request + offers_list)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "buckets": [
                                {
                                    "IsMedicalParcel": True,
                                    "offers": [
                                        {
                                            "cartItemIds": [1],
                                            "marketSku": _Offers._White.just_prescription.sku,
                                            "replacedId": _Offers._White.just_prescription.ware_md5,
                                            "wareId": _Offers._White.just_prescription.ware_md5,
                                        }
                                    ],
                                }
                            ],
                        }
                    ]
                }
            },
            allow_different_len=False,
        )

    def test_just_psychotropic(self):
        request = (
            'place=combine'
            '&split-strategy=split-medicine'
            '&pp=18'
            '&rids=213'
            '&rearr-factors=market_not_prescription_drugs_delivery=1'
            '&rearr-factors=enable_prescription_drugs_delivery=1'
        )

        offers_list = ('&offers-list={ware_id_1}:1;msku:{msku_1};cart_item_id:1').format(
            ware_id_1=_Offers._White.just_psychotropic.ware_md5,
            msku_1=_Offers._White.just_psychotropic.sku,
        )

        response = self.report.request_json(request + offers_list)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "buckets": [
                                {
                                    "IsMedicalParcel": True,
                                    "offers": [
                                        {
                                            "cartItemIds": [1],
                                            "marketSku": _Offers._White.just_psychotropic.sku,
                                            "replacedId": _Offers._White.just_psychotropic.ware_md5,
                                            "wareId": _Offers._White.just_psychotropic.ware_md5,
                                        }
                                    ],
                                }
                            ],
                        }
                    ]
                }
            },
            allow_different_len=False,
        )

    def test_just_narcotic(self):
        request = (
            'place=combine'
            '&split-strategy=split-medicine'
            '&pp=18'
            '&rids=213'
            '&rearr-factors=market_not_prescription_drugs_delivery=1'
            '&rearr-factors=enable_prescription_drugs_delivery=1'
        )

        offers_list = ('&offers-list={ware_id_1}:1;msku:{msku_1};cart_item_id:1').format(
            ware_id_1=_Offers._White.just_narcotic.ware_md5,
            msku_1=_Offers._White.just_narcotic.sku,
        )

        response = self.report.request_json(request + offers_list)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "buckets": [
                                {
                                    "IsMedicalParcel": True,
                                    "offers": [
                                        {
                                            "cartItemIds": [1],
                                            "marketSku": _Offers._White.just_narcotic.sku,
                                            "replacedId": _Offers._White.just_narcotic.ware_md5,
                                            "wareId": _Offers._White.just_narcotic.ware_md5,
                                        }
                                    ],
                                }
                            ],
                        }
                    ]
                }
            },
            allow_different_len=False,
        )

    def test_just_precursor(self):
        request = (
            'place=combine'
            '&split-strategy=split-medicine'
            '&pp=18'
            '&rids=213'
            '&rearr-factors=market_not_prescription_drugs_delivery=1'
            '&rearr-factors=enable_prescription_drugs_delivery=1'
        )

        offers_list = ('&offers-list={ware_id_1}:1;msku:{msku_1};cart_item_id:1').format(
            ware_id_1=_Offers._White.just_precursor.ware_md5,
            msku_1=_Offers._White.just_precursor.sku,
        )

        response = self.report.request_json(request + offers_list)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "buckets": [
                                {
                                    "IsMedicalParcel": True,
                                    "offers": [
                                        {
                                            "cartItemIds": [1],
                                            "marketSku": _Offers._White.just_precursor.sku,
                                            "replacedId": _Offers._White.just_precursor.ware_md5,
                                            "wareId": _Offers._White.just_precursor.ware_md5,
                                        }
                                    ],
                                }
                            ],
                        }
                    ]
                }
            },
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()
