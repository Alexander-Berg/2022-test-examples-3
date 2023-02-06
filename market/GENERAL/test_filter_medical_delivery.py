#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Shop, MarketSku, Model, Offer, DeliveryOption, Region

from core.testcase import TestCase, main


class _Rids:
    moscow = 213


class _Categories:
    medical = 15758037
    usual = 123456
    baa = 15756525


class _Hyperids:
    medical_with_courier = 1
    medical_without_courier = 2
    usual = 3
    baa = 4


class _Feshes:
    medical_with_courier = 10
    medical_without_courier = 20


class _Feeds:
    medical_with_courier = 100
    medical_without_courier = 200


class _Skus:
    medical_with_courier = 1000
    medical_without_courier = 2000
    usual = 3000
    baa = 4000


class _Buckets:
    medical_with_courier = 10000
    medical_without_courier = 20000


class _Shops:
    medical_with_courier = Shop(
        fesh=_Feshes.medical_with_courier,
        datafeed_id=_Feeds.medical_with_courier,
        priority_region=_Rids.moscow,
        regions=[_Rids.moscow],
        cpa=Shop.CPA_REAL,
        medicine_courier=True,
        name='Shop with medical courier',
    )

    medical_without_courier = Shop(
        fesh=_Feshes.medical_without_courier,
        datafeed_id=_Feeds.medical_without_courier,
        priority_region=_Rids.moscow,
        regions=[_Rids.moscow],
        cpa=Shop.CPA_REAL,
        medicine_courier=False,
        name='Shop without medical courier',
    )


class _Mskus:
    medical_with_courier = MarketSku(
        hyperid=_Hyperids.medical_with_courier,
        sku=_Skus.medical_with_courier,
        title="Medical msku for shop with courier",
    )

    medical_without_courier = MarketSku(
        hyperid=_Hyperids.medical_without_courier,
        sku=_Skus.medical_without_courier,
        title="Medical msku for shop without courier",
    )

    usual = MarketSku(hyperid=_Hyperids.usual, sku=_Skus.usual, title="Usual msku for shop without courier")

    baa = MarketSku(hyperid=_Hyperids.baa, sku=_Skus.baa, title="Baa msku for shop without courier")


class _Models:
    def create(hid, msku):
        return Model(hid=hid, hyperid=msku.hyperid)

    medical_with_courier = create(_Categories.medical, _Mskus.medical_with_courier)
    medical_without_courier = create(_Categories.medical, _Mskus.medical_without_courier)
    usual = create(_Categories.usual, _Mskus.usual)
    baa = create(_Categories.baa, _Mskus.baa)


class _Offers:
    medical_with_courier = Offer(
        waremd5='medical_with_courier_g',
        is_medicine=True,
        hyperid=_Mskus.medical_with_courier.hyperid,
        sku=_Mskus.medical_with_courier.sku,
        fesh=_Shops.medical_with_courier.fesh,
        delivery_options=[DeliveryOption(price=20, day_from=1, day_to=2)],
        delivery_buckets=[_Buckets.medical_with_courier],
        title="Medical offer for shop with medicine courier",
    )

    medical_without_courier = Offer(
        waremd5='medical_no_courier___g',
        is_medicine=True,
        hyperid=_Mskus.medical_without_courier.hyperid,
        sku=_Mskus.medical_without_courier.sku,
        fesh=_Shops.medical_without_courier.fesh,
        delivery_options=[DeliveryOption(price=20, day_from=1, day_to=2)],
        delivery_buckets=[_Buckets.medical_without_courier],
        title="Medical offer for shop without medicine courier",
    )

    usual = Offer(
        waremd5='usual_no_courier_____g',
        hyperid=_Mskus.usual.hyperid,
        sku=_Mskus.usual.sku,
        fesh=_Shops.medical_without_courier.fesh,
        delivery_options=[DeliveryOption(price=20, day_from=1, day_to=2)],
        delivery_buckets=[_Buckets.medical_without_courier],
        title="Usual offer for shop without medicine courier",
    )

    baa = Offer(
        waremd5='baa_no_courier_______g',
        is_baa=True,
        hyperid=_Mskus.baa.hyperid,
        sku=_Mskus.baa.sku,
        fesh=_Shops.medical_without_courier.fesh,
        delivery_options=[DeliveryOption(price=20, day_from=1, day_to=2)],
        delivery_buckets=[_Buckets.medical_without_courier],
        title="Baa offer for shop without medicine courier",
    )


class _Requests:
    offer_info = (
        'place=offerinfo'
        '&pp=18'
        '&rids=213'
        '&regset=0'
        '&rearr-factors=enable_medical_shop_delivery_filter=1'
        '&offerid={offer_id}'
    )


class T(TestCase):
    """
    Набор тестов для проверки фильтрации лекарств и поставщиков
    с или без курьерской медицинской лицензии:
    - https://st.yandex-team.ru/MARKETOUT-41783
    """

    @classmethod
    def prepare_regions(cls):
        cls.index.regiontree += [Region(rid=_Rids.moscow)]

    @classmethod
    def prepare_shops(cls):
        cls.index.shops += [_Shops.medical_with_courier, _Shops.medical_without_courier]

    @classmethod
    def prepare_mskus(cls):
        cls.index.mskus += [_Mskus.medical_with_courier, _Mskus.medical_without_courier, _Mskus.usual, _Mskus.baa]

    @classmethod
    def prepare_offers(cls):
        cls.index.offers += [_Offers.medical_with_courier, _Offers.medical_without_courier, _Offers.usual, _Offers.baa]

    @classmethod
    def prepare_models(cls):
        cls.index.models += [_Models.medical_with_courier, _Models.medical_without_courier, _Models.usual, _Models.baa]

    def test_medical_offer_with_medicine_courier_license(self):
        """
        Проверяем что на выдачу попадет медицинский оффер от поставщика с
        наличием курьерской медицинской лицензии.
        """

        response = self.report.request_json(_Requests.offer_info.format(offer_id=_Offers.medical_with_courier.ware_md5))

        self.assertFragmentIn(
            response,
            {
                "results": [{"wareId": _Offers.medical_with_courier.ware_md5}],
            },
            allow_different_len=False,
        )

    def test_usual_offer_without_medicine_courier_license(self):
        """
        Проверяем что на выдачу попадет обычный оффер от поставщика без
        наличия курьерской медицинской лицензии.
        """

        response = self.report.request_json(_Requests.offer_info.format(offer_id=_Offers.usual.ware_md5))

        self.assertFragmentIn(
            response,
            {
                "results": [{"wareId": _Offers.usual.ware_md5}],
            },
            allow_different_len=False,
        )

    def test_baa_offer_without_medicine_courier_license(self):
        """
        Проверяем что на выдачу попадет БАД оффер от поставщика без
        наличия курьерской медицинской лицензии.
        """

        response = self.report.request_json(_Requests.offer_info.format(offer_id=_Offers.baa.ware_md5))

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"wareId": _Offers.baa.ware_md5},
                ]
            },
            allow_different_len=False,
        )

    def test_medical_offer_without_medicine_courier_license(self):
        """
        Проверяем что на выдачу не попадет медициниский оффер от поставщика без
        наличия курьерской медицинской лицензии.
        """

        response = self.report.request_json(
            _Requests.offer_info.format(offer_id=_Offers.medical_without_courier.ware_md5) + "&debug=1"
        )

        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "brief": {
                        "filters": {"DELIVERY_SHOP_MEDICINE_COURIER": 1},
                    },
                },
                "search": {"results": []},
            },
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()
