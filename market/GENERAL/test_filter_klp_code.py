#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.matcher import EmptyList
from core.types import (
    BlueOffer,
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryOption,
    DeliveryServiceRegionToRegionInfo,
    DynamicDaysSet,
    DynamicDeliveryServiceInfo,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseInfo,
    DynamicWarehouseToWarehouseInfo,
    MarketSku,
    Model,
    Offer,
    Region,
    RegionalDelivery,
    Shop,
    TimeInfo,
    WarehouseWithPriority,
    WarehousesPriorityInRegion,
)
from core.testcase import TestCase, main


class _Rids:
    russia = 225
    moscow = 213


class _Categories:
    medicine = 15758037


class _Param_id:
    klp_code = 24277150
    klp_code_value = '21.20.10.221-000010-1-00174-2000000735385'
    another_klp_code_value = '21.20.10.221-000010-1-00176-2000000735386'

    index_klp_code = klp_code_value[: klp_code_value.rfind('-')]
    index_another_klp_code = another_klp_code_value[: another_klp_code_value.rfind('-')]


class _Hyperids:
    medical_courier = 1


class _Feshes:
    class _White:
        medical_courier = 10

    class _Blue:
        medical_courier = 11


class _Feeds:
    class _White:
        medical_courier = 100

    class _Blue:
        medical_courier = 101


class _Skus:
    class _White:
        medical_courier = 1000

    class _Blue:
        medical_courier = 1001


class _Buckets:
    class _White:
        medical_courier = 10000

    class _Blue:
        medical_courier = 10001


class _Courier:
    warehouse_id = 100000
    supplier_id = 1000000
    delivery_service_id = 10000000


class _Model:
    hid = 2
    hyperid = 20


class _Shops:
    class _White:
        medical_courier = Shop(
            fesh=_Feshes._White.medical_courier,
            datafeed_id=_Feeds._White.medical_courier,
            priority_region=_Rids.moscow,
            regions=[_Rids.moscow],
            cpa=Shop.CPA_REAL,
            medicine_courier=True,
            name='White medical courier shop',
        )

    class _Blue:
        medical_courier = Shop(
            fesh=_Feshes._Blue.medical_courier,
            datafeed_id=_Feeds._Blue.medical_courier,
            priority_region=_Rids.moscow,
            regions=[_Rids.moscow],
            cpa=Shop.CPA_REAL,
            medicine_courier=True,
            blue=Shop.BLUE_REAL,
            warehouse_id=_Courier.warehouse_id,
            name='White medical courier shop',
        )


class _BlueOffers:
    medical_courier = BlueOffer(
        waremd5='blue_med_courier_____g',
        hyperid=_Hyperids.medical_courier,
        fesh=_Feshes._Blue.medical_courier,
        feedid=_Feeds._Blue.medical_courier,
        delivery_buckets=[_Buckets._Blue.medical_courier],
        title="Blue medical offer with courier",
        klp_code=_Param_id.index_klp_code,
        is_medicine=True,
    )


class _Mskus:
    class _White:
        medical_courier = MarketSku(
            hyperid=_Hyperids.medical_courier,
            sku=_Skus._White.medical_courier,
            title="White medical courier market sku",
        )

    class _Blue:
        medical_courier = MarketSku(
            hyperid=_Hyperids.medical_courier,
            sku=_Skus._Blue.medical_courier,
            blue_offers=[_BlueOffers.medical_courier],
            title="Blue medical courier market sku",
        )


class _WhiteOffers:
    medical_courier = Offer(
        waremd5='white_med_courier____g',
        hyperid=_Hyperids.medical_courier,
        sku=_Mskus._White.medical_courier.sku,
        fesh=_Feshes._White.medical_courier,
        delivery_buckets=[_Buckets._White.medical_courier],
        title="White medical offer with courier",
        klp_code=_Param_id.index_klp_code,
        is_medicine=True,
    )


class _Requests:
    white_prime_offer = (
        'place=prime'
        '&rgb=white'
        '&pp=18'
        '&allow-collapsing=0'
        '&rearr-factors=market_not_prescription_drugs_delivery=1'
        '&rids={rids}'
        '&market-sku={msku}'
    )

    white_prime_model = 'place=prime' '&rgb=white' '&pp=18' '&allow-collapsing=0' '&hid={hid}'


class T(TestCase):
    """
    Набор тестов для поиска медицинских препаратов по КЛП коду:
    - https://st.yandex-team.ru/MARKETOUT-45649
    - Допустимые аргументы:
    - klp-code=<string> - КЛП код, по которому будем искать
    """

    @classmethod
    def prepare_regions(cls):
        cls.index.regiontree += [Region(rid=_Rids.moscow)]

    @classmethod
    def prepare_delivery_buckets(cls):
        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=_Buckets._White.medical_courier,
                carriers=[_Courier.delivery_service_id],
                regional_options=[
                    RegionalDelivery(
                        rid=_Rids.moscow,
                        options=[DeliveryOption(price=5, day_from=1, day_to=2, shop_delivery_price=10)],
                    )
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=_Buckets._Blue.medical_courier,
                carriers=[_Courier.delivery_service_id],
                regional_options=[
                    RegionalDelivery(
                        rid=_Rids.moscow,
                        options=[DeliveryOption(price=5, day_from=1, day_to=2, shop_delivery_price=10)],
                    )
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

    @classmethod
    def prepare_shops(cls):
        cls.index.shops += [_Shops._White.medical_courier, _Shops._Blue.medical_courier]

    @classmethod
    def prepare_mskus(cls):
        cls.index.mskus += [_Mskus._White.medical_courier, _Mskus._Blue.medical_courier]

    @classmethod
    def prepare_offers(cls):
        cls.index.offers += [
            _WhiteOffers.medical_courier,
        ]

    @classmethod
    def prepare_models(cls):
        cls.index.models += [
            Model(
                hid=_Categories.medicine,
                hyperid=_Hyperids.medical_courier,
            ),
            Model(
                hid=_Model.hid,
                hyperid=_Model.hyperid,
                klp_code=_Param_id.index_klp_code,
            ),
            Model(
                hid=_Model.hid,
                hyperid=_Model.hyperid + 1,
                klp_code=_Param_id.index_klp_code,
            ),
        ]

    @classmethod
    def prepare_warehouses(cls):
        cls.index.warehouse_priorities += [
            WarehousesPriorityInRegion(
                regions=[_Rids.russia, _Rids.moscow],
                warehouse_with_priority=[WarehouseWithPriority(warehouse_id=_Courier.warehouse_id, priority=1)],
            )
        ]

    @classmethod
    def prepare_lms(cls):
        cls.settings.lms_autogenerate = False
        cls.dynamic.lms += [DynamicDaysSet(key=1, days=[])]

        for wh_id, ds_id, ds_name in [
            (_Courier.warehouse_id, _Courier.delivery_service_id, 'courier_delivery_service'),
        ]:
            cls.dynamic.lms += [
                DynamicDeliveryServiceInfo(
                    id=ds_id,
                    name=ds_name,
                    region_to_region_info=[
                        DeliveryServiceRegionToRegionInfo(region_from=_Rids.moscow, region_to=_Rids.russia, days_key=1)
                    ],
                ),
                DynamicWarehouseInfo(id=wh_id, home_region=_Rids.moscow),
                DynamicWarehouseToWarehouseInfo(warehouse_from=wh_id, warehouse_to=wh_id),
                DynamicWarehouseAndDeliveryServiceInfo(
                    warehouse_id=wh_id,
                    delivery_service_id=ds_id,
                    operation_time=0,
                    date_switch_time_infos=[
                        DateSwitchTimeAndRegionInfo(
                            date_switch_hour=2,
                            region_to=_Rids.russia,
                            date_switch_time=TimeInfo(19, 0),
                            packaging_time=TimeInfo(3, 30),
                        )
                    ],
                ),
            ]

    def test_search_literal_by_offers(self):
        """
        Проверяем, что происходит фильтрация офферов по поисковому литералу 'klp_code'.
        """

        # Проверяем, что на выдачу не влияет значение поискового литерала для белых и синих офферов
        for sku, waremd5 in {
            _Mskus._White.medical_courier.sku: _WhiteOffers.medical_courier.waremd5,
            _Mskus._Blue.medical_courier.sku: _BlueOffers.medical_courier.waremd5,
        }.items():
            response = self.report.request_json(
                _Requests.white_prime_offer.format(
                    rids=_Rids.moscow,
                    msku=sku,
                )
                + '&rearr-factors=market_metadoc_search=no'
            )
            self.assertFragmentIn(response, {'results': [{'entity': 'offer', 'wareId': waremd5}]})

        # Проверяем, что белые и синие оффера попадают на выдачу при совпадении поискового литерала
        klp_code = '&klp-code={0}'.format(_Param_id.klp_code_value)
        for sku, waremd5 in {
            _Mskus._White.medical_courier.sku: _WhiteOffers.medical_courier.waremd5,
            _Mskus._Blue.medical_courier.sku: _BlueOffers.medical_courier.waremd5,
        }.items():
            response = self.report.request_json(
                _Requests.white_prime_offer.format(
                    rids=_Rids.moscow,
                    msku=sku,
                )
                + klp_code
                + '&rearr-factors=market_metadoc_search=no'
            )
            self.assertFragmentIn(response, {'results': [{'entity': 'offer', 'wareId': waremd5}]})

        # Проверяем, что белые и синие оффера не попадают на выдачу при несовпадении поискового литерала
        invalid_klp_code = '&klp-code={0}'.format(_Param_id.another_klp_code_value)
        for sku, waremd5 in {
            _Mskus._White.medical_courier.sku: _WhiteOffers.medical_courier.waremd5,
            _Mskus._Blue.medical_courier.sku: _BlueOffers.medical_courier.waremd5,
        }.items():
            response = self.report.request_json(
                _Requests.white_prime_offer.format(
                    rids=_Rids.moscow,
                    msku=sku,
                )
                + invalid_klp_code
                + '&rearr-factors=market_metadoc_search=no'
            )
            self.assertFragmentIn(response, {'results': EmptyList()})

    def test_search_literal_by_models(self):
        """
        Проверяем, что происходит фильтрация моделей по поисковому литералу 'klp_code'.
        """

        # Проверяем, что на выдачу не влияет значение поискового литерала для моделей
        response = self.report.request_json(
            _Requests.white_prime_model.format(
                hid=_Model.hid,
            )
        )
        self.assertFragmentIn(
            response, {'search': {'totalModels': 2, 'results': [{'id': _Model.hyperid}, {'id': _Model.hyperid + 1}]}}
        )

        # Проверяем, что модели попадают на выдачу при совпадении поискового литерала
        klp_code = '&klp-code={0}'.format(_Param_id.klp_code_value)
        response = self.report.request_json(
            _Requests.white_prime_model.format(
                hid=_Model.hid,
            )
            + klp_code
        )
        self.assertFragmentIn(
            response, {'search': {'totalModels': 2, 'results': [{'id': _Model.hyperid}, {'id': _Model.hyperid + 1}]}}
        )

        # Проверяем, что модели не попадают на выдачу при несовпадении поискового литерала
        invalid_klp_code = '&klp-code={0}'.format(_Param_id.another_klp_code_value)
        response = self.report.request_json(
            _Requests.white_prime_model.format(
                hid=_Model.hid,
            )
            + invalid_klp_code
        )
        self.assertFragmentIn(response, {'results': EmptyList()})


if __name__ == '__main__':
    main()
