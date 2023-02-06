#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    Currency,
    DeliveryBucket,
    DeliveryCalcFeedInfo,
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

SKU1_OFFER1_WARE_MD5 = 'Sku1Offerr-IiLVm1Goleg'

OLD_MOSCOW_OUTLET_IDS = [2013, 2014, 2015]
NEW_MOSCOW_OUTLET_IDS = [2050, 2051, 2052]

OLD_PENZA_OUTLET_IDS = [3014, 3015]
NEW_PENZA_OUTLET_IDS = [3050, 3051]


def create_outlet(outlet_id, region):
    return Outlet(
        point_id=outlet_id,
        delivery_service_id=123,
        region=region,
        point_type=Outlet.FOR_POST_TERM,
        delivery_option=OutletDeliveryOption(shipper_id=123, day_from=1, day_to=1, price=100),
        working_days=[i for i in range(10)],
    )


def create_expected_pickup(outlet_ids):
    return {
        'results': [
            {
                'delivery': {
                    'hasPickup': True,
                    'pickupOptions': [
                        {
                            'outletIds': outlet_ids,
                        }
                    ],
                },
            }
        ]
    }


class T(TestCase):
    @classmethod
    def prepare(cls):
        # НЕ делайте так в новых тестах!
        # Походов в КД на проде уже нет, пожалуйста, проверяйте новую функциональность, создавая доставку через комбинатор
        cls.settings.default_search_experiment_flags += ['force_disable_delivery_calculator_requests=0']

        cls.index.regiontree += [
            Region(
                rid=1,
                name="Москва и Московская область",
                region_type=Region.FEDERATIVE_SUBJECT,
                children=[
                    Region(
                        rid=213,
                        name="Москва",
                        region_type=Region.CITY,
                        children=[
                            Region(rid=216, name="Зеленоград", region_type=Region.CITY),
                            Region(
                                rid=20279,
                                name="Центральный административный округ",
                                region_type=Region.CITY_DISTRICT,
                                children=[
                                    Region(
                                        rid=120538,
                                        name="Пресненский район",
                                        region_type=Region.SECONDARY_DISTRICT,
                                        children=[
                                            Region(rid=20500, name="Баррикадная", region_type=Region.METRO_STATION),
                                        ],
                                    ),
                                ],
                            ),
                        ],
                    ),
                    Region(
                        rid=120999,
                        name="Городской округ Долгопрудный",
                        region_type=Region.SUBJECT_FEDERATION_DISTRICT,
                        children=[
                            Region(
                                rid=214,
                                name="Долгопрудный",
                                region_type=Region.CITY,
                                children=[
                                    Region(rid=214066, name="Шереметьевский", region_type=Region.CITY_DISTRICT),
                                ],
                            ),
                        ],
                    ),
                    Region(
                        rid=98611,
                        name="Солнечногорский район",
                        region_type=Region.SUBJECT_FEDERATION_DISTRICT,
                        children=[
                            Region(
                                rid=214048,
                                name="Городское поселение Солнечногорск",
                                region_type=Region.SETTLEMENT,
                                children=[
                                    Region(rid=10755, name="Солнечногорск", region_type=Region.CITY),
                                    Region(rid=119895, name="Сенеж", region_type=Region.VILLAGE),
                                ],
                            ),
                        ],
                    ),
                ],
            ),
            Region(
                rid=11095,
                name="Пензенская область",
                region_type=Region.FEDERATIVE_SUBJECT,
                children=[
                    Region(
                        rid=120962,
                        name="Городской округ Пенза",
                        region_type=Region.SUBJECT_FEDERATION_DISTRICT,
                        children=[
                            Region(rid=49, name="Пенза", region_type=Region.CITY),
                        ],
                    ),
                ],
            ),
        ]

        cls.index.shops += [
            Shop(
                fesh=1,
                datafeed_id=1,
                priority_region=213,
                name='virtual_shop',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                fulfillment_virtual=True,
                cpa=Shop.CPA_REAL,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                delivery_service_outlets=[2013, 2014, 2015, 3014, 3015],
            ),
        ]

        cls.index.outlets += [
            # Аутлеты из текущего ("старого") поколения
            create_outlet(2013, 213),  # Москва
            create_outlet(2014, 214),  # Долгопрудный
            create_outlet(2015, 10755),  # Солнечногорск
            create_outlet(3014, 49),  # Пенза
            create_outlet(3015, 49),  # Пенза
            # Аутлеты из SaasHub'а
            create_outlet(2050, 213),  # Москва
            create_outlet(2051, 214),  # Долгопрудный
            create_outlet(2052, 10755),  # Солнечногорск
            create_outlet(3050, 49),  # Пенза
            create_outlet(3051, 49),  # Пенза
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=1,
                dc_bucket_id=5001,
                fesh=1,
                carriers=[123],
                options=[PickupOption(outlet_id=outlet_id) for outlet_id in OLD_MOSCOW_OUTLET_IDS],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            PickupBucket(
                bucket_id=2,
                dc_bucket_id=5002,
                fesh=1,
                carriers=[123],
                options=[PickupOption(outlet_id=outlet_id) for outlet_id in OLD_PENZA_OUTLET_IDS],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
        ]

        cls.index.pickup_buckets_saashub += [
            PickupBucket(
                bucket_id=1,
                dc_bucket_id=5001,
                fesh=1,
                carriers=[123],
                options=[PickupOption(outlet_id=outlet_id) for outlet_id in NEW_MOSCOW_OUTLET_IDS],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            PickupBucket(
                bucket_id=2,
                dc_bucket_id=5002,
                fesh=1,
                carriers=[123],
                options=[PickupOption(outlet_id=outlet_id) for outlet_id in NEW_PENZA_OUTLET_IDS],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
        ]

        cls.index.delivery_calc_feed_info += [
            DeliveryCalcFeedInfo(feed_id=1, pickupBuckets=[5001, 5002]),
        ]

        cls.delivery_calc.on_request_offer_buckets(weight=5, width=30, height=10, length=20).respond(
            [], [5001, 5002], []
        )

        cls.index.mskus += [
            MarketSku(
                title="sku1",
                sku=1,
                hyperid=1,
                waremd5='Sku1-wdDXWsIiLVm1goleg',
                blue_offers=[
                    BlueOffer(
                        price=5,
                        vat=Vat.VAT_10,
                        feedid=2,
                        offerid='blue.offer.1',
                        waremd5=SKU1_OFFER1_WARE_MD5,
                        weight=5,
                        dimensions=OfferDimensions(length=20, width=30, height=10),
                    )
                ],
                pickup_buckets=[1],
                post_term_delivery=True,
            ),
        ]

    def _do_request(self, rids, cart_mode=0, satellite_mode=0):
        offers = [SKU1_OFFER1_WARE_MD5]
        offer_list = ';'.join('{}:1'.format(offer) for offer in offers)
        request = (
            'place=actual_delivery&rgb=blue&offers-list={}&rids={}&regset=1&pickup-options=grouped&pickup-options-extended-grouping=1'
            '&rearr-factors=satellite_region_outlets_enabled=1;rty_delivery_cart={};rty_delivery_cart_with_satellite_regions={}'
            '&force-use-delivery-calc=1'.format(offer_list, rids, cart_mode, satellite_mode)
        )
        return self.report.request_json(request)

    def test_turned_off_saashub_cart(self):
        """
        Проверяем, что при выключенных походах в saas-hub, как бы мы ни варьировали значения
        флага rty_delivery_cart_with_satellite_regions, данные о самовывозе будут браться из индекса.
        (сейчас сателлиты в индексе отключены)
        """

        # Москва
        expected = create_expected_pickup([2013])
        for satellite_mode in range(3):
            response = self._do_request(213, cart_mode=0, satellite_mode=satellite_mode)
            self.assertFragmentIn(response, expected, allow_different_len=False)

        # Пенза
        expected = create_expected_pickup(OLD_PENZA_OUTLET_IDS)
        for satellite_mode in range(3):
            response = self._do_request(49, cart_mode=0, satellite_mode=satellite_mode)
            self.assertFragmentIn(response, expected, allow_different_len=False)

    def test_saashub_cart_with_mmap_satellite_regions_only(self):
        """
        Проверяем, что если rty_delivery_cart_with_satellite_regions=0, то данные о доставке
        для региона, у которого есть регионы-спутники (сейчас, например, это Москва) берутся целиком
        из индекса, а для других регионов -- из saas-hub'а.
        """

        # Москва
        expected = create_expected_pickup([2013])
        response = self._do_request(213, cart_mode=2, satellite_mode=0)
        self.assertFragmentIn(response, expected, allow_different_len=False)

        # Пенза
        expected = create_expected_pickup(NEW_PENZA_OUTLET_IDS)
        response = self._do_request(49, cart_mode=2, satellite_mode=0)
        self.assertFragmentIn(response, expected, allow_different_len=False)

    def test_saashub_cart_with_fair_satellite_regions(self):
        """
        Проверяем, что если rty_delivery_cart_with_satellite_regions=1, то данные о доставке
        в регионы-спутники целиком берутся из saas-hub'а.
        """

        expected = create_expected_pickup(NEW_MOSCOW_OUTLET_IDS)
        response = self._do_request(213, cart_mode=2, satellite_mode=1)
        self.assertFragmentIn(response, expected, allow_different_len=False)

    def test_saashub_cart_traverse_pickup_regions_up(self):
        """
        Проверяем, что при запросе регионов из saas-hub'а учитывается логика "подъема" по
        дереву регионов (см. https://a.yandex-team.ru/arc/trunk/arcadia/market/library/regional_delivery_mms/utils.h?rev=6471266#L133).
        """

        # Делаем запрос, указав в качестве региона станцию метро "Баррикадная" (rid=20500). Ожидаем, что получим аутлет Москвы
        expected = create_expected_pickup([2013])
        response = self._do_request(20500, cart_mode=0, satellite_mode=0)
        self.assertFragmentIn(response, expected, allow_different_len=False)

        # То же самое, с той лишь разницей, что аутлет будет взят из saas-hub'а
        expected = create_expected_pickup([2050])
        for cart_mode in [1, 2]:
            for satellite_mode in [0, 1, 2]:
                response = self._do_request(20500, cart_mode=cart_mode, satellite_mode=satellite_mode)
                self.assertFragmentIn(response, expected, allow_different_len=False)

    def test_saashub_cart_traverse_pickup_regions_down(self):
        """
        Проверяем, что при запросе регионов из saas-hub'а учитывается логика "спуска" по
        дереву регионов (см. https://a.yandex-team.ru/arc/trunk/arcadia/market/library/regional_delivery_mms/utils.h?rev=6471266#L135).
        """

        # Делаем запрос, указав в качестве региона городской округ Пенза (rid=120962). Ожидаем, что получим аутлет Пензы
        expected = create_expected_pickup(OLD_PENZA_OUTLET_IDS)
        response = self._do_request(120962, cart_mode=0, satellite_mode=0)
        self.assertFragmentIn(response, expected, allow_different_len=False)

        # То же самое, с той лишь разницей, что аутлет будет взят из saas-hub'а
        expected = create_expected_pickup(NEW_PENZA_OUTLET_IDS)
        for cart_mode in [1, 2]:
            for satellite_mode in [0, 1, 2]:
                response = self._do_request(120962, cart_mode=cart_mode, satellite_mode=satellite_mode)
                self.assertFragmentIn(response, expected, allow_different_len=False)

    def test_saashub_cart_with_hybrid_regions(self):
        """
        Проверяем, что если rty_delivery_cart_with_satellite_regions=2, то данные о доставке
        в регионы-спутники берутся из индекса, а данные по основному региону -- из saas-hub'а.
        После удаления функциональности сателлитов из индекса это перестало работать
        """

        # Для Москвы получаем аутлеты из регионов-спутников
        expected = create_expected_pickup([2050])
        response = self._do_request(213, cart_mode=2, satellite_mode=2)
        self.assertFragmentIn(response, expected, allow_different_len=False)

        # Для Долгопрудного получаем аутлеты Долгопрудного + аутлеты Москвы
        expected = create_expected_pickup([2051])
        response = self._do_request(120999, cart_mode=2, satellite_mode=2)
        self.assertFragmentIn(response, expected, allow_different_len=False)


if __name__ == '__main__':
    main()
