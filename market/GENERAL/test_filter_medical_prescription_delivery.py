#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    Shop,
    PrescriptionManagementSystem,
    MarketSku,
    Model,
    Offer,
    DeliveryOption,
    Region,
)

from core.testcase import TestCase, main


class _Rids:
    moscow = 213


class _Categories:
    prescription = 15758037
    usual = 123456
    medical = 15756919


class _Hyperids:
    prescription_in_pms = 1
    prescription_not_in_pms = 2
    usual = 3
    medical = 4


class _Feshes:
    medical_in_pms = 10
    medical_not_in_pms = 20


class _Feeds:
    medical_in_pms = 100
    medical_not_in_pms = 200


class _Skus:
    prescription_in_pms = 1000
    prescription_not_in_pms = 2000
    usual = 3000
    medical = 4000


class _Buckets:
    medical_in_pms = 10000
    medical_not_in_pms = 20000


class _Shops:
    medical_in_pms = Shop(
        fesh=_Feshes.medical_in_pms,
        datafeed_id=_Feeds.medical_in_pms,
        priority_region=_Rids.moscow,
        regions=[_Rids.moscow],
        cpa=Shop.CPA_REAL,
        medicine_courier=True,
        name='Shop in Prescription Management System',
        prescription_management_system=PrescriptionManagementSystem.PS_MEDICATA,
    )

    medical_not_in_pms = Shop(
        fesh=_Feshes.medical_not_in_pms,
        datafeed_id=_Feeds.medical_not_in_pms,
        priority_region=_Rids.moscow,
        regions=[_Rids.moscow],
        cpa=Shop.CPA_REAL,
        medicine_courier=True,
        name='Shop not in Prescription Management System',
        prescription_management_system=None,
    )


class _Mskus:
    prescription_in_pms = MarketSku(
        hyperid=_Hyperids.prescription_in_pms, sku=_Skus.prescription_in_pms, title="Prescription msku for shop in PMS"
    )

    prescription_not_in_pms = MarketSku(
        hyperid=_Hyperids.prescription_not_in_pms,
        sku=_Skus.prescription_not_in_pms,
        title="Prescription msku for shop not in PMS",
    )

    usual = MarketSku(hyperid=_Hyperids.usual, sku=_Skus.usual, title="Usual msku")

    medical = MarketSku(hyperid=_Hyperids.medical, sku=_Skus.medical, title="Medical msku")


class _Models:
    def create(hid, msku):
        return Model(hid=hid, hyperid=msku.hyperid)

    prescription_in_pms = create(_Categories.prescription, _Mskus.prescription_in_pms)
    prescription_not_in_pms = create(_Categories.prescription, _Mskus.prescription_not_in_pms)
    usual = create(_Categories.usual, _Mskus.usual)
    medical = create(_Categories.medical, _Mskus.medical)


class _Offers:
    prescription_in_pms = Offer(
        waremd5='prescription_in_pms__g',
        is_medicine=True,
        is_prescription=True,
        hyperid=_Mskus.prescription_in_pms.hyperid,
        sku=_Mskus.prescription_in_pms.sku,
        fesh=_Shops.medical_in_pms.fesh,
        delivery_options=[DeliveryOption(price=20, day_from=1, day_to=2)],
        delivery_buckets=[_Buckets.medical_in_pms],
        title="Prescription offer for shop in PMS",
    )

    prescription_not_in_pms = Offer(
        waremd5='prescription_not_pms_g',
        is_medicine=True,
        is_prescription=True,
        hyperid=_Mskus.prescription_not_in_pms.hyperid,
        sku=_Mskus.prescription_not_in_pms.sku,
        fesh=_Shops.medical_not_in_pms.fesh,
        delivery_options=[DeliveryOption(price=20, day_from=1, day_to=2)],
        delivery_buckets=[_Buckets.medical_not_in_pms],
        title="Prescription offer for shop not in PMS",
    )

    usual = Offer(
        waremd5='usual________________g',
        hyperid=_Mskus.usual.hyperid,
        sku=_Mskus.usual.sku,
        fesh=_Shops.medical_not_in_pms.fesh,
        delivery_options=[DeliveryOption(price=20, day_from=1, day_to=2)],
        delivery_buckets=[_Buckets.medical_not_in_pms],
        title="Usual offer for shop not in PMS",
    )

    medical = Offer(
        waremd5='medical______________g',
        is_medicine=True,
        hyperid=_Mskus.medical.hyperid,
        sku=_Mskus.medical.sku,
        fesh=_Shops.medical_not_in_pms.fesh,
        delivery_options=[DeliveryOption(price=20, day_from=1, day_to=2)],
        delivery_buckets=[_Buckets.medical_not_in_pms],
        title="Medical offer for shop not in PMS",
    )


class _Requests:
    offer_info = (
        'place=offerinfo'
        '&pp=18'
        '&rids=213'
        '&regset=0'
        '&rearr-factors=enable_prescription_drugs_delivery=1'
        '&rearr-factors=enable_medical_shop_prescription_delivery_filter=1'
        '&offerid={offer_id}'
    )


class T(TestCase):
    """
    Набор тестов для проверки фильтрации рецептурных лекарств и поставщиков
    состоящих или несостоящих в программе Электронный рецепт:
    - https://st.yandex-team.ru/MARKETOUT-40335
    """

    @classmethod
    def prepare_regions(cls):
        cls.index.regiontree += [Region(rid=_Rids.moscow)]

    @classmethod
    def prepare_shops(cls):
        cls.index.shops += [_Shops.medical_in_pms, _Shops.medical_not_in_pms]

    @classmethod
    def prepare_mskus(cls):
        cls.index.mskus += [_Mskus.prescription_in_pms, _Mskus.prescription_not_in_pms, _Mskus.usual, _Mskus.medical]

    @classmethod
    def prepare_offers(cls):
        cls.index.offers += [
            _Offers.prescription_in_pms,
            _Offers.prescription_not_in_pms,
            _Offers.usual,
            _Offers.medical,
        ]

    @classmethod
    def prepare_models(cls):
        cls.index.models += [
            _Models.prescription_in_pms,
            _Models.prescription_not_in_pms,
            _Models.usual,
            _Models.medical,
        ]

    def test_prescription_offer_for_suppler_in_pms(self):
        """
        Проверяем что на выдачу попадет рецептурный оффер при общемаркетном
        разрешении на продажу рецептурных лекарств и присутствии постащика в
        программе Электронный рецепт.
        """

        response = self.report.request_json(_Requests.offer_info.format(offer_id=_Offers.prescription_in_pms.ware_md5))

        self.assertFragmentIn(
            response,
            {"results": [{"wareId": _Offers.prescription_in_pms.ware_md5}]},
            allow_different_len=False,
        )

    def test_prescription_offer_for_suppler_not_in_pms(self):
        """
        Проверяем что на выдачу не попадет рецептурный оффер при общемаркетном
        разрешении на продажу рецептурных лекарств и отсутствия постащика в
        программе Электронный рецепт.
        """

        response = self.report.request_json(
            _Requests.offer_info.format(offer_id=_Offers.prescription_not_in_pms.ware_md5) + "&debug=1"
        )

        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "brief": {
                        "filters": {"DELIVERY_SHOP_PRESCRIPTION_MANAGEMENT_SYSTEM": 1},
                    },
                },
                "search": {"results": []},
            },
            allow_different_len=False,
        )

    def test_usual_offer_for_suppler_not_in_pms(self):
        """
        Проверяем что на выдачу попадет обычный оффер при общемаркетном
        разрешении на продажу рецептурных лекарств и отсутствия постащика в
        программе Электронный рецепт.
        """

        response = self.report.request_json(_Requests.offer_info.format(offer_id=_Offers.usual.ware_md5))

        self.assertFragmentIn(
            response,
            {"results": [{"wareId": _Offers.usual.ware_md5}]},
            allow_different_len=False,
        )

    def test_medical_offer_for_suppler_not_in_pms(self):
        """
        Проверяем что на выдачу попадет медицинский оффер при общемаркетном
        разрешении на продажу рецептурных лекарств и отсутствия постащика в
        программе Электронный рецепт.
        """

        response = self.report.request_json(_Requests.offer_info.format(offer_id=_Offers.medical.ware_md5))

        self.assertFragmentIn(
            response,
            {"results": [{"wareId": _Offers.medical.ware_md5}]},
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()
