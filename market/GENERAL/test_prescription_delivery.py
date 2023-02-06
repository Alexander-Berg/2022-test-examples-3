#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    CategoryRestriction,
    DeliveryBucket,
    DeliveryOption,
    Model,
    Offer,
    Outlet,
    PickupBucket,
    PickupOption,
    PrescriptionManagementSystem,
    Region,
    RegionalDelivery,
    RegionalRestriction,
    Shop,
)
from core.testcase import TestCase, main
from core.matcher import NotEmptyList, EmptyList


class _Rids:
    kz = 4380
    russia_with_limits = 4381
    kz_with_limits = 4382
    russia_no_limits = 4383


class _DeliveryServices:
    service_1 = 99
    service_2 = 111


class _Params:
    drugs_category_id = 15758037


class _Categories:
    unknown = 1
    prescription_not_medicine_type = 2
    prescription_medical_product = 3
    prescription_medicine = 4
    prescription_narcotic = 5
    prescription_psychotropic = 6
    prescription_precursor = 7
    prescription_not_allowed_alcohol = 8
    prescription_without_management_system = 9

    MEDICAL_SHOPS_CATEGORIES = [
        unknown,
        prescription_not_medicine_type,
        prescription_medical_product,
        prescription_medicine,
        prescription_narcotic,
        prescription_psychotropic,
        prescription_precursor,
        prescription_not_allowed_alcohol,
    ]

    MEDICAL_SHOPS_NO_PMS_CATEGORIES = [
        prescription_without_management_system,
    ]


class _Feshes:
    medical_1 = 10
    medical_2 = 20
    medical_no_pms = 30


class _Feeds:
    medical_1 = 100
    medical_2 = 200
    medical_no_pms = 300


class _Outlets:
    medical_id_1 = 1000
    medical_id_2 = 2000

    medical_1_1 = Outlet(
        fesh=_Feshes.medical_1, region=_Rids.russia_with_limits, point_type=Outlet.FOR_STORE, point_id=medical_id_1
    )

    medical_1_2 = Outlet(
        fesh=_Feshes.medical_1, region=_Rids.kz_with_limits, point_type=Outlet.FOR_STORE, point_id=medical_id_2
    )

    ALL = [medical_1_1, medical_1_2]


class _Shops:
    def create(fesh, datafeed_id, medicine_courier, name, prescription_system):
        return Shop(
            fesh=fesh,
            datafeed_id=datafeed_id,
            priority_region=_Rids.russia_with_limits,
            regions=[
                _Rids.russia_with_limits,
                _Rids.kz_with_limits,
                _Rids.russia_no_limits,
            ],
            medicine_courier=medicine_courier,
            name=name,
            prescription_management_system=prescription_system,
        )

    medical_1 = create(
        fesh=_Feshes.medical_1,
        datafeed_id=_Feeds.medical_1,
        name='Аптека с ПВЗ и доставкой в Казахстан и Россию',
        medicine_courier=True,
        prescription_system=PrescriptionManagementSystem.PS_MEDICATA,
    )

    medical_2 = create(
        fesh=_Feshes.medical_2,
        datafeed_id=_Feeds.medical_2,
        name='Аптека только с доставкой в Казахстан и Россию',
        medicine_courier=True,
        prescription_system=PrescriptionManagementSystem.PS_MEDICATA,
    )

    medical_no_pms = create(
        fesh=_Feshes.medical_no_pms,
        datafeed_id=_Feeds.medical_no_pms,
        name='Аптека с ПВЗ и доставкой в Казахстан и Россию (не подключена к ЭР)',
        medicine_courier=True,
        prescription_system=PrescriptionManagementSystem.PS_NONE,
    )

    ALL = [medical_1, medical_2, medical_no_pms]


class _Buckets:
    medical_id_1 = 10000
    medical_id_pickup_1 = 20000
    medical_id_pickup_2 = 30000

    def create_delivery(bucket_id, carriers):
        return DeliveryBucket(
            bucket_id=bucket_id,
            carriers=carriers,
            regional_options=[
                RegionalDelivery(
                    rid=_Rids.russia_with_limits,
                    options=[DeliveryOption(price=5, day_from=1, day_to=2, shop_delivery_price=10)],
                ),
                RegionalDelivery(
                    rid=_Rids.kz_with_limits,
                    options=[DeliveryOption(price=5, day_from=1, day_to=2, shop_delivery_price=10)],
                ),
                RegionalDelivery(
                    rid=_Rids.russia_no_limits,
                    options=[DeliveryOption(price=5, day_from=1, day_to=2, shop_delivery_price=10)],
                ),
            ],
            delivery_program=DeliveryBucket.REGULAR_PROGRAM,
        )

    def create_pickup(bucket_id, shop, outlet_id, carriers):
        return PickupBucket(
            bucket_id=bucket_id,
            fesh=shop.fesh,
            carriers=carriers,
            options=[PickupOption(outlet_id=outlet_id)],
            delivery_program=DeliveryBucket.REGULAR_PROGRAM,
        )

    medical_1 = create_delivery(medical_id_1, [_DeliveryServices.service_2])

    medical_pickup_1 = create_pickup(
        medical_id_pickup_1, _Shops.medical_1, _Outlets.medical_id_1, [_DeliveryServices.service_1]
    )
    medical_pickup_2 = create_pickup(
        medical_id_pickup_2, _Shops.medical_1, _Outlets.medical_id_2, [_DeliveryServices.service_1]
    )

    DELIVERY = [medical_1]
    PICKUP = [medical_pickup_1, medical_pickup_2]


class _Models:
    prescription_not_medicine_type = Model(
        hyperid=_Categories.prescription_not_medicine_type,
        hid=_Params.drugs_category_id,
    )

    medical_unknown_product = Model(
        hyperid=_Categories.unknown,
        hid=_Params.drugs_category_id,
    )

    prescription_medical_product = Model(
        hyperid=_Categories.prescription_medical_product,
        hid=_Params.drugs_category_id,
    )

    prescription_medicine = Model(
        hyperid=_Categories.prescription_medicine,
        hid=_Params.drugs_category_id,
    )

    prescription_medicine_narcoric = Model(
        hyperid=_Categories.prescription_narcotic,
        hid=_Params.drugs_category_id,
    )

    prescription_medicine_psychotropic = Model(
        hyperid=_Categories.prescription_psychotropic,
        hid=_Params.drugs_category_id,
    )

    prescription_medicine_precursor = Model(
        hyperid=_Categories.prescription_precursor,
        hid=_Params.drugs_category_id,
    )

    prescription_medicine_not_allowed_alcohol = Model(
        hyperid=_Categories.prescription_not_allowed_alcohol,
        hid=_Params.drugs_category_id,
    )

    prescription_medicine_no_pms = Model(
        hyperid=_Categories.prescription_without_management_system,
        hid=_Params.drugs_category_id,
    )

    ALL = [
        medical_unknown_product,
        prescription_not_medicine_type,
        prescription_medical_product,
        prescription_medicine,
        prescription_medicine_not_allowed_alcohol,
        prescription_medicine_narcoric,
        prescription_medicine_psychotropic,
        prescription_medicine_precursor,
        prescription_medicine_no_pms,
    ]


class _Offers:
    @staticmethod
    def create(
        hyperid,
        shop,
        title,
        pickup_buckets=None,
        delivery_buckets=None,
        is_medicine=False,
        is_medical_product=False,
        is_baa=False,
        is_prescription=False,
        is_psychotropic=False,
        is_precursor=False,
        is_narcotic=False,
        is_ethanol=False,
    ):
        return Offer(
            hyperid=hyperid,
            fesh=shop.fesh,
            feedid=shop.datafeed_id,
            delivery_options=[DeliveryOption(price=20, day_from=1, day_to=2)],
            pickup_buckets=pickup_buckets,
            delivery_buckets=delivery_buckets,
            title=title,
            is_medicine=is_medicine,
            is_medical_product=is_medical_product,
            is_baa=is_baa,
            is_prescription=is_prescription,
            is_psychotropic=is_psychotropic,
            is_precursor=is_precursor,
            is_narcotic=is_narcotic,
            is_ethanol=is_ethanol,
        )

    ALL = []

    @classmethod
    def prepare_offers(cls):
        for category in [_Categories.unknown]:
            cls.ALL.append(
                cls.create(
                    hyperid=category,
                    shop=_Shops.medical_1,
                    pickup_buckets=[_Buckets.medical_id_pickup_1, _Buckets.medical_id_pickup_2],
                    delivery_buckets=[_Buckets.medical_id_1],
                    title="Офер с ПВЗ и доставкой",
                )
            )
            cls.ALL.append(
                cls.create(
                    hyperid=category,
                    shop=_Shops.medical_2,
                    delivery_buckets=[_Buckets.medical_id_1],
                    title="Офер с доставкой",
                )
            )

        for category in [_Categories.prescription_not_medicine_type]:
            cls.ALL.append(
                cls.create(
                    hyperid=category,
                    shop=_Shops.medical_1,
                    pickup_buckets=[_Buckets.medical_id_pickup_1, _Buckets.medical_id_pickup_2],
                    delivery_buckets=[_Buckets.medical_id_1],
                    title="Офер с ПВЗ и доставкой",
                    is_prescription=True,
                )
            )
            cls.ALL.append(
                cls.create(
                    hyperid=category,
                    shop=_Shops.medical_2,
                    delivery_buckets=[_Buckets.medical_id_1],
                    title="Офер с доставкой",
                    is_prescription=True,
                )
            )

        for category in [_Categories.prescription_medical_product]:
            cls.ALL.append(
                cls.create(
                    hyperid=category,
                    shop=_Shops.medical_1,
                    pickup_buckets=[_Buckets.medical_id_pickup_1, _Buckets.medical_id_pickup_2],
                    delivery_buckets=[_Buckets.medical_id_1],
                    title="Офер с ПВЗ и доставкой",
                    is_medicine=True,
                    is_prescription=True,
                    is_medical_product=True,
                )
            )
            cls.ALL.append(
                cls.create(
                    hyperid=category,
                    shop=_Shops.medical_2,
                    delivery_buckets=[_Buckets.medical_id_1],
                    title="Офер с доставкой",
                    is_medicine=True,
                    is_prescription=True,
                    is_medical_product=True,
                )
            )

        for category in [_Categories.prescription_narcotic]:
            cls.ALL.append(
                cls.create(
                    hyperid=category,
                    shop=_Shops.medical_1,
                    pickup_buckets=[_Buckets.medical_id_pickup_1, _Buckets.medical_id_pickup_2],
                    delivery_buckets=[_Buckets.medical_id_1],
                    title="Офер с ПВЗ и доставкой",
                    is_medicine=True,
                    is_prescription=True,
                    is_narcotic=True,
                )
            )
            cls.ALL.append(
                cls.create(
                    hyperid=category,
                    shop=_Shops.medical_2,
                    delivery_buckets=[_Buckets.medical_id_1],
                    title="Офер с доставкой",
                    is_medicine=True,
                    is_prescription=True,
                    is_narcotic=True,
                )
            )

        for category in [_Categories.prescription_psychotropic]:
            cls.ALL.append(
                cls.create(
                    hyperid=category,
                    shop=_Shops.medical_1,
                    pickup_buckets=[_Buckets.medical_id_pickup_1, _Buckets.medical_id_pickup_2],
                    delivery_buckets=[_Buckets.medical_id_1],
                    title="Офер с ПВЗ и доставкой",
                    is_medicine=True,
                    is_prescription=True,
                    is_psychotropic=True,
                )
            )
            cls.ALL.append(
                cls.create(
                    hyperid=category,
                    shop=_Shops.medical_2,
                    delivery_buckets=[_Buckets.medical_id_1],
                    title="Офер с доставкой",
                    is_medicine=True,
                    is_prescription=True,
                    is_psychotropic=True,
                )
            )

        for category in [_Categories.prescription_precursor]:
            cls.ALL.append(
                cls.create(
                    hyperid=category,
                    shop=_Shops.medical_1,
                    pickup_buckets=[_Buckets.medical_id_pickup_1, _Buckets.medical_id_pickup_2],
                    delivery_buckets=[_Buckets.medical_id_1],
                    title="Офер с ПВЗ и доставкой",
                    is_medicine=True,
                    is_prescription=True,
                    is_precursor=True,
                )
            )
            cls.ALL.append(
                cls.create(
                    hyperid=category,
                    shop=_Shops.medical_2,
                    delivery_buckets=[_Buckets.medical_id_1],
                    title="Офер с доставкой",
                    is_medicine=True,
                    is_prescription=True,
                    is_precursor=True,
                )
            )

        for category in [_Categories.prescription_medicine]:
            cls.ALL.append(
                cls.create(
                    hyperid=category,
                    shop=_Shops.medical_1,
                    pickup_buckets=[_Buckets.medical_id_pickup_1, _Buckets.medical_id_pickup_2],
                    delivery_buckets=[_Buckets.medical_id_1],
                    title="Офер с ПВЗ и доставкой",
                    is_medicine=True,
                    is_prescription=True,
                )
            )
            cls.ALL.append(
                cls.create(
                    hyperid=category,
                    shop=_Shops.medical_2,
                    delivery_buckets=[_Buckets.medical_id_1],
                    title="Офер с доставкой",
                    is_medicine=True,
                    is_prescription=True,
                )
            )

        for category in [_Categories.prescription_not_allowed_alcohol]:
            cls.ALL.append(
                cls.create(
                    hyperid=category,
                    shop=_Shops.medical_1,
                    pickup_buckets=[_Buckets.medical_id_pickup_1, _Buckets.medical_id_pickup_2],
                    delivery_buckets=[_Buckets.medical_id_1],
                    title="Офер с ПВЗ и доставкой",
                    is_medicine=True,
                    is_prescription=True,
                    is_ethanol=True,
                )
            )
            cls.ALL.append(
                cls.create(
                    hyperid=category,
                    shop=_Shops.medical_2,
                    delivery_buckets=[_Buckets.medical_id_1],
                    title="Офер с доставкой",
                    is_medicine=True,
                    is_prescription=True,
                    is_ethanol=True,
                )
            )

        for category in _Categories.MEDICAL_SHOPS_NO_PMS_CATEGORIES:
            cls.ALL.append(
                cls.create(
                    hyperid=category,
                    shop=_Shops.medical_no_pms,
                    pickup_buckets=[_Buckets.medical_id_pickup_1, _Buckets.medical_id_pickup_2],
                    delivery_buckets=[_Buckets.medical_id_1],
                    title="Офер с ПВЗ и доставкой",
                    is_medicine=True,
                    is_prescription=True,
                )
            )


class _Requests:
    request = (
        'place=prime'
        '&regional-delivery=1'
        '&local-offers-first=0'
        '&rids=' + str(_Rids.russia_with_limits) + '&hyperid={hyperid}'
    )


class T(TestCase):
    """
    Набор тестов для проекта "Электронный рецепт".
    Для запрашиваемых категорий рецептурных медицинских товаров проверяются следующие условия:
    - Медицинские препараты с некорретными параметрами не подлежат доставке
    - Медицинские препараты с запрещенными компонентами не подлежат доставке
    - Корректные рецептурные медицинские препараты без запрещенных компонентов подлежат доставке при условии
      наличия разрешающего флага (rearr-factors=enable_prescription_drugs_delivery=1) и подключения поставщика
      к программе Электронного Рецепта
    См.: https://st.yandex-team.ru/MARKETPROJECT-5505
    """

    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

    @classmethod
    def prepare_regiontree(cls):
        cls.index.regiontree += [
            Region(
                rid=_Rids.kz,
                name="Казахстан",
                region_type=Region.COUNTRY,
                children=[Region(rid=_Rids.kz_with_limits, name="Караганда")],
            ),
            Region(rid=_Rids.russia_with_limits),
            Region(rid=_Rids.russia_no_limits),
        ]

    @classmethod
    def prepare_category_restrictions(cls):
        cls.index.category_restrictions += [
            CategoryRestriction(
                name='not_prescription',
                hids=[_Params.drugs_category_id],
                regional_restrictions=[
                    RegionalRestriction(
                        show_offers=True,
                        display_only_matched_offers=False,
                        delivery=False,
                        rids=[_Rids.russia_with_limits, _Rids.kz_with_limits],
                    ),
                ],
            )
        ]

    @classmethod
    def prepare_shops(cls):
        cls.index.shops += _Shops.ALL

    @classmethod
    def prepare_offers(cls):
        _Offers.prepare_offers()
        cls.index.offers += _Offers.ALL

    @classmethod
    def prepare_models(cls):
        cls.index.models += _Models.ALL

    @classmethod
    def prepare_outlets(cls):
        cls.index.outlets += _Outlets.ALL

    @classmethod
    def prepare_delivery_buckets(cls):
        cls.index.delivery_buckets += _Buckets.DELIVERY

    @classmethod
    def prepare_pickup_buckets(cls):
        cls.index.pickup_buckets += _Buckets.PICKUP

    def _assert_offer_with_delivery(self, response, model_id):
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "titles": {"raw": "Офер с ПВЗ и доставкой"},
                        "model": {"id": model_id},
                        "delivery": {"isAvailable": True, "hasLocalStore": True, "options": NotEmptyList()},
                    },
                    {
                        "entity": "offer",
                        "titles": {"raw": "Офер с доставкой"},
                        "model": {"id": model_id},
                        "delivery": {"isAvailable": True, "hasLocalStore": False, "options": NotEmptyList()},
                    },
                ]
            },
            allow_different_len=False,
        )

    def _assert_offer_without_delivery(self, response, model_id):
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "titles": {"raw": "Офер с ПВЗ и доставкой"},
                        "model": {"id": model_id},
                        "delivery": {"isAvailable": False, "hasLocalStore": True, "options": EmptyList()},
                    }
                ]
            },
            allow_different_len=False,
        )

    def test_prescription_drug_delivery_with_forbidden_medicament(self):
        '''
        Проверяем, что лекарственные препараты, в чей состав входят запрещенные вещества,
        не будут иметь доставку, даже если есть флаг, снимающий ограничение на доставку рецептурных препаратов.
        Подключение поставщика к системе Электронного Рецепта не отменяет данных требований.

        '''
        CATEGORIES = [
            _Categories.prescription_narcotic,
            _Categories.prescription_psychotropic,
            _Categories.prescription_precursor,
            _Categories.prescription_not_allowed_alcohol,
        ]
        for category in CATEGORIES:
            for flag in [
                "",
                "&rearr-factors=enable_prescription_drugs_delivery=1",
            ]:
                # С флагом и без - доставки не будет
                response = self.report.request_json(_Requests.request.format(hyperid=category) + flag)
                self._assert_offer_without_delivery(response, category)

    def test_prescription_drug_delivery_with_error_medicine_type(self):
        '''
        Проверяем, что лекарственные препараты с ошибочным типом или без необходимых параметров
        не будут иметь доставку, даже если есть флаг, снимающий ограничение на доставку рецептурных препаратов.
        Подключение поставщика к системе Электронного Рецепта не отменяет данных требований.
        '''
        CATEGORIES = [
            _Categories.unknown,
            _Categories.prescription_not_medicine_type,
        ]
        for category in CATEGORIES:
            for flag in [
                "",
                "&rearr-factors=enable_prescription_drugs_delivery=1",
            ]:
                # С флагом и без - доставки не будет
                response = self.report.request_json(_Requests.request.format(hyperid=category) + flag)
                self._assert_offer_without_delivery(response, category)

    def test_prescription_drugs_delivery_with_pms(self):
        '''
        Проверяем, что корректные рецептурные лекарственные препараты без запрещенных компонентов
        и при подключении поставщика к программе Электронного Рецепта будут иметь доставку.
        '''
        CATEGORIES = [
            _Categories.prescription_medical_product,
            _Categories.prescription_medicine,
        ]

        # Проверям, что при выключеном флаге доставка отстутсвует
        for category in CATEGORIES:
            response = self.report.request_json(_Requests.request.format(hyperid=category))
            self._assert_offer_without_delivery(response, category)

        # Проверям, что при включеном флаге доставка разрешена
        for category in CATEGORIES:
            response = self.report.request_json(
                _Requests.request.format(hyperid=category) + "&rearr-factors=enable_prescription_drugs_delivery=1"
            )
            self._assert_offer_with_delivery(response, category)

    def test_prescription_drugs_delivery_without_pms(self):
        '''
        Проверяем, что корректные рецептурные лекарственные препараты без запрещенных компонентов
        и без подключения к программе Электронный рецепт не будут иметь доставку, ни при каких условиях.
        '''

        # Проверям, что при включеном флаге доставка разрешена только для модели с поставщиком, подключенным к ЭР
        for category in _Categories.MEDICAL_SHOPS_NO_PMS_CATEGORIES:
            for flag in [
                "",
                "&rearr-factors=enable_prescription_drugs_delivery=1",
            ]:
                response = self.report.request_json(_Requests.request.format(hyperid=category) + flag)
                self._assert_offer_without_delivery(response, category)


if __name__ == '__main__':
    main()
