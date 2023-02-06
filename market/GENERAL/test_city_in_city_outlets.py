#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    Currency,
    DeliveryBucket,
    DynamicDeliveryRestriction,
    DynamicWarehouseDelivery,
    DynamicWarehouseLink,
    DynamicWarehouseInfo,
    DynamicWarehouseToWarehouseInfo,
    Outlet,
    OutletDeliveryOption,
    PickupBucket,
    PickupOption,
    Region,
    Shop,
)

from core.testcase import TestCase, main
from core.types.offer import OfferDimensions
from core.types.sku import MarketSku, BlueOffer
from core.types.taxes import Vat, Tax


'''
Внутри Москвы есть деревня и город, в которых нет ПВЗ. Все ПВЗ принадлежат Москве.
'''
MOSCOW = 212
CITY_IN_MOSCOW = 214
METRO_IN_MOSCOW = 215
REGION_IN_MOSCOW = 216
VILLAGE_IN_REGION_IN_MOSCOW = 217


'''
Внутри Питера есть город (Митино), у которого тоже есть ПВЗ.
Должны учитываться оба ПВЗ
'''
PITER = 7
MITINO = 8

'''
Идентификаторы оутлетов
'''
MOSCOW_OUTLET_ID = 1001
PITER_OUTLET_ID = 1002
MITINO_OUTLET_ID = 1003


class T(TestCase):
    @classmethod
    def prepare_offer(cls):
        cls.index.mskus += [
            MarketSku(
                title="blue offer sku1",
                hyperid=1,
                sku=101010,
                blue_offers=[
                    BlueOffer(
                        price=5,
                        vat=Vat.VAT_10,
                        waremd5='Sku1Price5-IiLVm1Goleg',
                        weight=5,
                        dimensions=OfferDimensions(length=20, width=30, height=10),
                        pickup_buckets=[
                            MOSCOW_OUTLET_ID,
                            PITER_OUTLET_ID,
                            MITINO_OUTLET_ID,
                        ],
                    )
                ],
            ),
        ]

    @classmethod
    def prepare_region(cls):
        cls.index.regiontree += [
            Region(
                rid=MOSCOW,
                name='Москва',
                region_type=Region.CITY,
                children=[
                    Region(rid=CITY_IN_MOSCOW, name='Город в Москве', region_type=Region.CITY),
                    Region(rid=METRO_IN_MOSCOW, name='Метро в Москве', region_type=Region.METRO_STATION),
                    Region(
                        rid=REGION_IN_MOSCOW,
                        name='Район в Москве',
                        region_type=Region.CITY_DISTRICT,
                        children=[
                            Region(
                                rid=VILLAGE_IN_REGION_IN_MOSCOW, name='Деревня в Москве', region_type=Region.VILLAGE
                            ),
                        ],
                    ),
                ],
            ),
            Region(
                rid=PITER,
                name='Питер',
                region_type=Region.CITY,
                children=[
                    Region(rid=MITINO, name='Город в Питере', region_type=Region.CITY),
                ],
            ),
        ]

    @classmethod
    def prepare(cls):
        cls.settings.blue_market_free_delivery_threshold = 3000
        cls.settings.blue_market_prime_free_delivery_threshold = 2991
        cls.settings.blue_market_yandex_plus_free_delivery_threshold = 2992
        cls.settings.nordstream_autogenerate = False
        cls.settings.default_search_experiment_flags += ['market_nordstream=0']

        cls.index.shops += [
            Shop(
                fesh=1,
                datafeed_id=1,
                priority_region=MOSCOW,
                name='virtual_shop',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                fulfillment_virtual=True,
                cpa=Shop.CPA_REAL,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                delivery_service_outlets=[
                    MOSCOW_OUTLET_ID,
                    PITER_OUTLET_ID,
                    MITINO_OUTLET_ID,
                ],
            )
        ]

    @classmethod
    def prepare_outlet(cls):
        def create_outlet(id, region):
            return Outlet(
                point_id=id,
                delivery_service_id=103,
                region=region,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    shipper_id=103, day_from=1, day_to=1, order_before=2, work_in_holiday=True, price=100
                ),
                working_days=[i for i in range(10)],
            )

        cls.index.outlets += [
            create_outlet(MOSCOW_OUTLET_ID, MOSCOW),
            create_outlet(PITER_OUTLET_ID, PITER),
            create_outlet(MITINO_OUTLET_ID, MITINO),
        ]

        def create_bucket(id):
            return PickupBucket(
                bucket_id=id,
                dc_bucket_id=id,
                fesh=1,
                carriers=[103],
                options=[PickupOption(outlet_id=id, day_from=1, day_to=2, price=5)],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            )

        cls.index.pickup_buckets += [
            create_bucket(MOSCOW_OUTLET_ID),
            create_bucket(PITER_OUTLET_ID),
            create_bucket(MITINO_OUTLET_ID),
        ]

    @classmethod
    def prepare_nordstream(cls):
        WAREHOUSE_ID = 145
        cls.dynamic.nordstream += [
            DynamicWarehouseLink(WAREHOUSE_ID, [WAREHOUSE_ID]),
            DynamicWarehouseDelivery(
                WAREHOUSE_ID,
                {region: [DynamicDeliveryRestriction(min_days=1, max_days=2)] for region in (MOSCOW, PITER, MITINO)},
            ),
        ]
        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=WAREHOUSE_ID, home_region=MOSCOW),
            DynamicWarehouseToWarehouseInfo(warehouse_from=WAREHOUSE_ID, warehouse_to=WAREHOUSE_ID),
        ]

    def __check_outlet(self, region, outlets):
        response = self.report.request_json(
            "place=actual_delivery&offers-list=Sku1Price5-IiLVm1Goleg:1"
            "&pickup-options=grouped&pickup-options-extended-grouping=1"
            "&combinator=0&rids={}".format(region)
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "deliveryGroup",
                "delivery": {"pickupOptions": [{"outletIds": outlets}]},
            },
            allow_different_len=False,
        )

    def test_moscow(self):
        """
        Проверяет отображение оутлета в родительском городе
        """
        self.__check_outlet(MOSCOW, [MOSCOW_OUTLET_ID])

    def test_city_in_moscow(self):
        """
        Проверяет отображение оутлета родительского города, если запросили дочерний город
        """
        self.__check_outlet(CITY_IN_MOSCOW, [MOSCOW_OUTLET_ID])

    def test_village_in_moscow(self):
        """
        Деревня, которая находится под регионом внутри города
        """
        self.__check_outlet(VILLAGE_IN_REGION_IN_MOSCOW, [MOSCOW_OUTLET_ID])

    def test_metro_in_moscow(self):
        """
        Запрошена станция метро внутри города. Показывается ПВЗ из самого города
        """
        self.__check_outlet(METRO_IN_MOSCOW, [MOSCOW_OUTLET_ID])

    def test_region_in_moscow(self):
        """
        Запрошен регион внутри города. Показывается ПВЗ из самого города
        """
        self.__check_outlet(REGION_IN_MOSCOW, [MOSCOW_OUTLET_ID])

    def test_piter(self):
        """
        Внутри Питера есть еще город, тоже содержащий ПВЗ.
        В данный момент генерация связей в regional_delivery.mmap организована так, что к Питеру не прикрепляются подрегионы.
        Поэтому показывается только ПВЗ самого Питера
        """
        self.__check_outlet(PITER, [PITER_OUTLET_ID])

    def test_mitino(self):
        """
        Внутри Питера есть еще город, тоже содержащий ПВЗ
        Запросили ПВЗ вложенного города
        Проверяем, что показаны ПВЗ этого города и его родителя
        """
        self.__check_outlet(MITINO, [PITER_OUTLET_ID, MITINO_OUTLET_ID])

    def update_flags(self, **kwargs):
        self.stop_report()
        self.emergency_flags.reset()
        self.emergency_flags.add_flags(**kwargs)
        self.emergency_flags.save()
        self.restart_report()

    def __check_non_existen_offer(self, region):
        response = self.report.request_json(
            "place=actual_delivery&offers-list=Sku1Price5-IiLVm1Goleg:1"
            "&pickup-options=grouped&pickup-options-extended-grouping=1"
            "&combinator=0&rids={}".format(region)
        )
        self.assertFragmentIn(
            response,
            {
                "offerProblems": [{"problems": ["NONEXISTENT_OFFER"], "wareId": "Sku1Price5-IiLVm1Goleg"}],
            },
            allow_different_len=False,
        )

    def test_parent_city_disabled(self):
        '''
        Проверяем, что при выключенном функционале ПВЗ родительского города не добавляются
        '''
        self.update_flags(market_parent_city_enabled=0)
        try:
            self.__check_outlet(MITINO, [MITINO_OUTLET_ID])
            self.__check_non_existen_offer(VILLAGE_IN_REGION_IN_MOSCOW)
            self.__check_outlet(MOSCOW, [MOSCOW_OUTLET_ID])
        finally:
            self.update_flags(market_parent_city_enabled=1)


if __name__ == '__main__':
    main()
