#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    Model,
    Offer,
    VirtualModel,
    Region,
    Shop,
    OfferDimensions,
    DeliveryOption,
    DeliveryBucket,
    DynamicDaysSet,
    DynamicWarehouseInfo,
    DynamicDeliveryServiceInfo,
    DateSwitchTimeAndRegionInfo,
    DynamicWarehouseAndDeliveryServiceInfo,
    DeliveryServiceRegionToRegionInfo,
    PrescriptionManagementSystem,
)
from core.testcase import TestCase, main


class _Rids:
    moscow = 213
    russia = 225


class _DeliveryServices:
    internal = 99


class _Feshes:
    medical = 10


class _Feeds:
    medical = 100


class _ClientIds:
    medical = 1000


class _Warehouses:
    medical = 10000


class _Constants:
    vidal_atc_code_id = 23181290
    vidal_atc_code = 'J05AX13'
    hyper_id_with_options = 1
    hyper_id_without_options = 2
    drugs_category = 15758037

    virtual_range_start_with_options = 1
    virtual_range_finish_with_options = 10
    virtual_model_id_with_options = (virtual_range_start_with_options + virtual_range_finish_with_options) / 2

    virtual_range_start_without_option = 11
    virtual_range_finish_without_option = 21
    virtual_model_id_without_options = (virtual_range_start_without_option + virtual_range_finish_without_option) / 2


class _Shops:
    def create(fesh, datafeed_id, client_id, warehouse_id, priority_region, regions, name):
        return Shop(
            fesh=fesh,
            datafeed_id=datafeed_id,
            client_id=client_id,
            warehouse_id=warehouse_id,
            priority_region=priority_region,
            regions=regions,
            cpa=Shop.CPA_REAL,
            cis=Shop.CIS_REAL,
            medicine_courier=True,
            medical_booking=True,
            prescription_management_system=PrescriptionManagementSystem.PS_MEDICATA,
            name=name,
        )

    medical = create(
        fesh=_Feshes.medical,
        datafeed_id=_Feeds.medical,
        client_id=_ClientIds.medical,
        warehouse_id=_Warehouses.medical,
        priority_region=_Rids.moscow,
        regions=[_Rids.moscow],
        name='Medical shop',
    )


class _Buckets:
    medical_id = 100000

    def create(bucket_id, shop, carriers):
        return DeliveryBucket(
            bucket_id=bucket_id,
            dc_bucket_id=bucket_id,
            fesh=shop.fesh,
            carriers=carriers,
            delivery_program=DeliveryBucket.REGULAR_PROGRAM,
        )

    medical = create(medical_id, _Shops.medical, [_DeliveryServices.internal])


class _Models:
    # Create a model to use all possible medical offer features
    medical_all_options = Model(
        hyperid=_Constants.hyper_id_with_options,
        hid=_Constants.drugs_category,
        title="Medical model with all options",
        is_medicine=True,
        is_medical_product=True,
        is_baa=True,
        is_prescription=True,
        is_psychotropic=True,
        is_narcotic=True,
        is_precursor=True,
        is_ethanol=True,
        vidal_atc_code=_Constants.vidal_atc_code,
    )
    medical_no_options = Model(
        hyperid=_Constants.hyper_id_without_options,
        hid=_Constants.drugs_category,
        title="Medical model without any options",
    )


class _VirtualModels:
    medical_all_options = VirtualModel(virtual_model_id=_Constants.virtual_model_id_with_options)
    medical_no_options = VirtualModel(virtual_model_id=_Constants.virtual_model_id_without_options)


class _Offers:
    def create(
        waremd5,
        hyperid,
        title,
        shop=None,
        supplier_id=None,
        delivery_buckets=None,
        is_medicine=None,
        is_medical_product=None,
        is_baa=None,
        is_prescription=None,
        is_psychotropic=None,
        is_narcotic=None,
        is_precursor=None,
        is_ethanol=None,
        is_medical_booking=None,
        vidal_atc_code=None,
        virtual_model_id=None,
    ):

        _fesh = None
        _datafeed_id = None
        if shop:
            _fesh = shop.fesh
            _datafeed_id = shop.datafeed_id

        return Offer(
            waremd5=waremd5,
            hyperid=hyperid,
            title=title,
            fesh=_fesh,
            feedid=_datafeed_id,
            supplier_id=supplier_id,
            delivery_buckets=delivery_buckets,
            is_medicine=is_medicine,
            is_medical_product=is_medical_product,
            is_baa=is_baa,
            is_prescription=is_prescription,
            is_psychotropic=is_psychotropic,
            is_narcotic=is_narcotic,
            is_precursor=is_precursor,
            is_ethanol=is_ethanol,
            is_medical_booking=is_medical_booking,
            vidal_atc_code=vidal_atc_code,
            virtual_model_id=virtual_model_id,
            price=100,
            weight=1,
            dimensions=OfferDimensions(length=3, width=3, height=3),
            delivery_options=[DeliveryOption(price=100, day_from=1, day_to=2, order_before=14)],
        )

    medical_all_options = create(
        hyperid=_Constants.hyper_id_with_options,
        title='Medical offer with all options',
        waremd5='medical_all_flags____g',
        shop=_Shops.medical,
        supplier_id=_ClientIds.medical,
        is_medicine=True,
        is_medical_product=True,
        is_baa=True,
        is_prescription=True,
        is_psychotropic=True,
        is_narcotic=True,
        is_precursor=True,
        is_ethanol=True,
        is_medical_booking=True,
        vidal_atc_code=_Constants.vidal_atc_code,
        delivery_buckets=[_Buckets.medical.bucket_id],
    )

    medical_no_options = create(
        hyperid=_Constants.hyper_id_without_options,
        title='Medical offer without any options',
        waremd5='medical_no_flags_____g',
        shop=_Shops.medical,
        supplier_id=_ClientIds.medical,
        delivery_buckets=[_Buckets.medical.bucket_id],
    )

    medical_all_options_virtual = create(
        hyperid=_Constants.hyper_id_with_options,
        title='Virtual medical offer with all options',
        waremd5='medical_all_flags_v__g',
        shop=_Shops.medical,
        supplier_id=_ClientIds.medical,
        is_medicine=True,
        is_medical_product=True,
        is_baa=True,
        is_prescription=True,
        is_psychotropic=True,
        is_narcotic=True,
        is_precursor=True,
        is_ethanol=True,
        is_medical_booking=True,
        vidal_atc_code=_Constants.vidal_atc_code,
        virtual_model_id=_Constants.virtual_model_id_with_options,
        delivery_buckets=[_Buckets.medical.bucket_id],
    )

    medical_no_options_virtual = create(
        hyperid=_Constants.hyper_id_without_options,
        title='Virtual medical offer without any options',
        waremd5='medical_no_flags_v___g',
        shop=_Shops.medical,
        supplier_id=_ClientIds.medical,
        virtual_model_id=_Constants.virtual_model_id_without_options,
        delivery_buckets=[_Buckets.medical.bucket_id],
    )

    unknown_medicine = create(
        hyperid=_Constants.hyper_id_with_options,
        title='Unknown medicine',
        waremd5='unknown_medicine_____g',
        shop=_Shops.medical,
        supplier_id=_ClientIds.medical,
        delivery_buckets=[_Buckets.medical.bucket_id],
    )

    baa_with_delivery = create(
        hyperid=_Constants.hyper_id_with_options,
        title='Baa offer with delivery',
        waremd5='baa_with_delivery____g',
        shop=_Shops.medical,
        supplier_id=_ClientIds.medical,
        is_baa=True,
        delivery_buckets=[_Buckets.medical.bucket_id],
    )

    baa_prescription_without_delivery = create(
        hyperid=_Constants.hyper_id_with_options,
        title='Baa prescription offer without delivery',
        waremd5='baa_prescription_____g',
        shop=_Shops.medical,
        supplier_id=_ClientIds.medical,
        is_baa=True,
        is_prescription=True,
        delivery_buckets=[_Buckets.medical.bucket_id],
    )

    baa_forbidden_medicament_without_delivery = create(
        hyperid=_Constants.hyper_id_with_options,
        title='Baa forbidden medicament offer without delivery',
        waremd5='baa_forbidden_med____g',
        shop=_Shops.medical,
        supplier_id=_ClientIds.medical,
        is_baa=True,
        is_psychotropic=True,
        is_narcotic=True,
        is_precursor=True,
        is_ethanol=True,
        delivery_buckets=[_Buckets.medical.bucket_id],
    )

    medical_product_with_delivery = create(
        hyperid=_Constants.hyper_id_with_options,
        title='Medical product offer with delivery',
        waremd5='med_p_with_delivery__g',
        shop=_Shops.medical,
        supplier_id=_ClientIds.medical,
        is_medical_product=True,
        delivery_buckets=[_Buckets.medical.bucket_id],
    )

    medical_product_prescription = create(
        hyperid=_Constants.hyper_id_with_options,
        title='Medical product prescription offer',
        waremd5='med_p_prescription___g',
        shop=_Shops.medical,
        supplier_id=_ClientIds.medical,
        is_medical_product=True,
        is_prescription=True,
        delivery_buckets=[_Buckets.medical.bucket_id],
    )

    medical_product_forbidden_medicament_without_delivery = create(
        hyperid=_Constants.hyper_id_with_options,
        title='Medical product forbidden medicament offer without delivery',
        waremd5='med_p_forbidden_med__g',
        shop=_Shops.medical,
        supplier_id=_ClientIds.medical,
        is_medical_product=True,
        is_psychotropic=True,
        is_narcotic=True,
        is_precursor=True,
        is_ethanol=True,
        delivery_buckets=[_Buckets.medical.bucket_id],
    )

    prescription_with_delivery = create(
        hyperid=_Constants.hyper_id_with_options,
        title='Prescription offer with delivery',
        waremd5='prescription_________g',
        shop=_Shops.medical,
        supplier_id=_ClientIds.medical,
        is_medicine=True,
        is_prescription=True,
        delivery_buckets=[_Buckets.medical.bucket_id],
    )

    prescription_unknown = create(
        hyperid=_Constants.hyper_id_with_options,
        title='Prescription unknown offer without delivery',
        waremd5='prescription_unknown_g',
        shop=_Shops.medical,
        supplier_id=_ClientIds.medical,
        is_prescription=True,
        delivery_buckets=[_Buckets.medical.bucket_id],
    )

    prescription_forbidden_medicament = create(
        hyperid=_Constants.hyper_id_with_options,
        title='Prescription forbidden medicament offer without delivery',
        waremd5='prescription_f_med___g',
        shop=_Shops.medical,
        supplier_id=_ClientIds.medical,
        is_medicine=True,
        is_prescription=True,
        is_psychotropic=True,
        is_narcotic=True,
        is_precursor=True,
        is_ethanol=True,
        delivery_buckets=[_Buckets.medical.bucket_id],
    )

    not_prescription_with_delivery = create(
        hyperid=_Constants.hyper_id_with_options,
        title='Not prescription offer with delivery',
        waremd5='not_prescription_____g',
        shop=_Shops.medical,
        supplier_id=_ClientIds.medical,
        is_medicine=True,
        delivery_buckets=[_Buckets.medical.bucket_id],
    )

    not_prescription_forbidden_medicament = create(
        hyperid=_Constants.hyper_id_with_options,
        title='Not prescription forbidden medicament offer without delivery',
        waremd5='notprescription_f_medg',
        shop=_Shops.medical,
        supplier_id=_ClientIds.medical,
        is_medicine=True,
        is_psychotropic=True,
        is_narcotic=True,
        is_precursor=True,
        is_ethanol=True,
        delivery_buckets=[_Buckets.medical.bucket_id],
    )


class _Requests:
    offerinfo = (
        'place=offerinfo'
        '&rids=213'
        '&pp=18'
        '&regset=1'
        '&offerid={offerid}'
        '&rearr-factors=enable_prescription_drugs_delivery={prescription_drugs_delivery}'
        '&rearr-factors=market_enable_medical_baa_delivery=1'
        '&rearr-factors=market_enable_medical_product_delivery=1'
        '&rearr-factors=market_not_prescription_drugs_delivery=1'
    )

    modelinfo = 'place=modelinfo' '&rids=213' '&hyperid={hyperid}'

    virtualmodelinfo = (
        'place=modelinfo'
        '&rids=213'
        '&hyperid={hyperid}'
        '&rearr-factors=market_cards_everywhere_model_info={everywhere};'
        'market_cards_everywhere_range={start}:{finish}'
    )


class T(TestCase):
    """
    Набор тестов для проверки использования медицинских флагов, полученных на
    этапе индексации.
    Проверяем:
      а) отображение specs->internal полей у моделей и офферов (мета поиск)
      б) расчет наличия доставляемости медицинских товаров (базовый поиск)
    См.: https://st.yandex-team.ru/MARKETOUT-43560
    """

    @classmethod
    def prepare_regions(cls):
        cls.index.regiontree += [Region(rid=_Rids.moscow, name="Moscow", tz_offset=10800)]

    @classmethod
    def prepare_shops(cls):
        cls.index.shops += [
            _Shops.medical,
        ]

    @classmethod
    def prepare_delivery_buckets(cls):
        cls.index.delivery_buckets += [
            _Buckets.medical,
        ]

    @classmethod
    def prepare_models(cls):
        cls.index.models += [
            _Models.medical_all_options,
            _Models.medical_no_options,
        ]

    @classmethod
    def prepare_virtual_models(cls):
        cls.index.virtual_models += [
            _VirtualModels.medical_all_options,
            _VirtualModels.medical_no_options,
        ]

    @classmethod
    def prepare_offers(cls):
        cls.index.offers += [
            _Offers.medical_all_options,
            _Offers.medical_no_options,
            _Offers.medical_all_options_virtual,
            _Offers.medical_no_options_virtual,
            _Offers.unknown_medicine,
            _Offers.baa_with_delivery,
            _Offers.baa_prescription_without_delivery,
            _Offers.baa_forbidden_medicament_without_delivery,
            _Offers.medical_product_with_delivery,
            _Offers.medical_product_prescription,
            _Offers.medical_product_forbidden_medicament_without_delivery,
            _Offers.prescription_unknown,
            _Offers.prescription_with_delivery,
            _Offers.prescription_forbidden_medicament,
            _Offers.not_prescription_with_delivery,
            _Offers.not_prescription_forbidden_medicament,
        ]

    @classmethod
    def prepare_lms(cls):
        cls.settings.lms_autogenerate = False
        cls.dynamic.lms += [DynamicDaysSet(key=1, days=[])]

        for wh_id, ds_id, ds_name in [
            (_Warehouses.medical, _DeliveryServices.internal, 'medical_shop_delivery_service'),
        ]:
            cls.dynamic.lms += [
                DynamicDeliveryServiceInfo(
                    id=ds_id,
                    name=ds_name,
                    region_to_region_info=[
                        DeliveryServiceRegionToRegionInfo(region_from=_Rids.moscow, region_to=_Rids.russia, days_key=1)
                    ],
                ),
                DynamicWarehouseInfo(id=wh_id, home_region=_Rids.moscow, holidays_days_set_key=2),
                DynamicWarehouseAndDeliveryServiceInfo(
                    warehouse_id=wh_id,
                    delivery_service_id=ds_id,
                    operation_time=0,
                    date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=2, region_to=_Rids.russia)],
                ),
            ]

    def _check_medical_offer_spec_internal_in(self, response):
        self.assertFragmentIn(
            response,
            {
                "specs": {
                    "internal": [
                        {"type": "spec", "value": "baa", "usedParams": []},
                        {"type": "spec", "value": "medicine", "usedParams": []},
                        {"type": "spec", "value": "medical_product", "usedParams": []},
                        {"type": "spec", "value": "prescription", "usedParams": []},
                        {"type": "spec", "value": "psychotropic", "usedParams": []},
                        {"type": "spec", "value": "narcotic", "usedParams": []},
                        {"type": "spec", "value": "precursor", "usedParams": []},
                        {"type": "spec", "value": "ethanol", "usedParams": []},
                        {"type": "spec", "value": "medical_booking", "usedParams": []},
                        {
                            "type": "spec",
                            "value": "vidal",
                            "usedParams": [{"id": _Constants.vidal_atc_code_id, "name": _Constants.vidal_atc_code}],
                        },
                    ],
                }
            },
            allow_different_len=False,
        )

    def _check_medical_model_spec_internal_in(self, response):
        self.assertFragmentIn(
            response,
            {
                "specs": {
                    "internal": [
                        {"type": "spec", "value": "baa", "usedParams": []},
                        {"type": "spec", "value": "medicine", "usedParams": []},
                        {"type": "spec", "value": "medical_product", "usedParams": []},
                        {"type": "spec", "value": "prescription", "usedParams": []},
                        {"type": "spec", "value": "psychotropic", "usedParams": []},
                        {"type": "spec", "value": "narcotic", "usedParams": []},
                        {"type": "spec", "value": "precursor", "usedParams": []},
                        {"type": "spec", "value": "ethanol", "usedParams": []},
                        {
                            "type": "spec",
                            "value": "vidal",
                            "usedParams": [{"id": _Constants.vidal_atc_code_id, "name": _Constants.vidal_atc_code}],
                        },
                    ],
                }
            },
            allow_different_len=False,
        )

    def _check_medical_spec_internal_not_in(self, response):
        self.assertFragmentIn(
            response,
            {
                "specs": {
                    "internal": [],
                }
            },
            allow_different_len=False,
        )

    def _check_offer_delivery_in(self, response, wareId):
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "entity": "offer",
                            "wareId": wareId,
                            "delivery": {"options": [{"serviceId": str(_DeliveryServices.internal)}]},
                        }
                    ]
                }
            },
            allow_different_len=False,
        )

    def _check_offer_delivery_not_in(self, response, wareId):
        self.assertFragmentIn(
            response,
            {"search": {"results": [{"entity": "offer", "wareId": wareId, "delivery": {"options": []}}]}},
            allow_different_len=False,
        )

    def test_offer_render(self):
        """
        Проверяем, что передаются медицинские флаги с базового поиска на мета поиск
        для отображения specs->internal.
        """

        response = self.report.request_json(
            _Requests.offerinfo.format(
                offerid=_Offers.medical_all_options.waremd5,
                prescription_drugs_delivery=1,
            )
        )
        self._check_medical_offer_spec_internal_in(response)

        response = self.report.request_json(
            _Requests.offerinfo.format(
                offerid=_Offers.medical_no_options.waremd5,
                prescription_drugs_delivery=1,
            )
        )
        self._check_medical_spec_internal_not_in(response)

    def test_model_render(self):
        """
        Проверяем, что передаются медицинские флаги с базового поиска на мета поиск
        для отображения specs->internal.
        """

        response = self.report.request_json(_Requests.modelinfo.format(hyperid=_Constants.hyper_id_with_options))
        self._check_medical_model_spec_internal_in(response)

        response = self.report.request_json(_Requests.modelinfo.format(hyperid=_Constants.hyper_id_without_options))
        self._check_medical_spec_internal_not_in(response)

    def test_virtual_model_render(self):
        """
        Проверяем, что передаются медицинские флаги с базового поиска на мета поиск
        для отображения specs->internal.
        """

        response = self.report.request_json(
            _Requests.virtualmodelinfo.format(
                hyperid=_Constants.virtual_model_id_with_options,
                everywhere=1,
                start=_Constants.virtual_range_start_with_options,
                finish=_Constants.virtual_range_finish_with_options,
            )
        )
        self._check_medical_offer_spec_internal_in(response)

        response = self.report.request_json(
            _Requests.virtualmodelinfo.format(
                hyperid=_Constants.virtual_model_id_with_options,
                everywhere=1,
                start=_Constants.virtual_range_start_with_options,
                finish=_Constants.virtual_range_finish_with_options,
            )
        )
        self._check_medical_offer_spec_internal_in(response)

    def test_unknown_medicine_offer_delivery(self):
        """
        Проверяем, что передаются медицинские флаги для расчета доставки на базовом поиске.
        """

        # У медицинского препарата (который без бита IS_MEDICINE, IS_BAA, IS_MEDICAL_PRODUCT)
        # но относящийся к медицинской категории есть опции доставки.
        # Это очень пограничный случай!
        response = self.report.request_json(
            _Requests.offerinfo.format(offerid=_Offers.unknown_medicine.waremd5, prescription_drugs_delivery=1)
        )
        self._check_offer_delivery_in(response, _Offers.unknown_medicine.waremd5)

    def test_not_prescription_offer_delivery(self):
        """
        Проверяем, что передаются медицинские флаги для расчета доставки на базовом поиске.
        """

        # У безрецептурного препарата есть опции доставки
        response = self.report.request_json(
            _Requests.offerinfo.format(
                offerid=_Offers.not_prescription_with_delivery.waremd5,
                prescription_drugs_delivery=1,
            )
        )
        self._check_offer_delivery_in(response, _Offers.not_prescription_with_delivery.waremd5)

    def test_not_prescription_forbidden_medicament_offer_delivery(self):
        """
        Проверяем, что передаются медицинские флаги для расчета доставки на базовом поиске.
        """

        # Отсутсвие доставки у безрецептурного препарата с запрещенным веществами
        response = self.report.request_json(
            _Requests.offerinfo.format(
                offerid=_Offers.not_prescription_forbidden_medicament.waremd5,
                prescription_drugs_delivery=1,
            )
        )
        self._check_offer_delivery_not_in(response, _Offers.not_prescription_forbidden_medicament.waremd5)

    def test_prescription_offer_delivery(self):
        """
        Проверяем, что передаются медицинские флаги для расчета доставки на базовом поиске.
        """

        # У рецептурного препарата есть опции доставки
        response = self.report.request_json(
            _Requests.offerinfo.format(
                offerid=_Offers.prescription_with_delivery.waremd5,
                prescription_drugs_delivery=1,
            )
        )
        self._check_offer_delivery_in(response, _Offers.prescription_with_delivery.waremd5)

    def test_prescription_unknown_offer_delivery(self):
        """
        Проверяем, что передаются медицинские флаги для расчета доставки на базовом поиске.
        """

        # Отсутсвие доставки у рецептурного препарата без медицинского признака
        response = self.report.request_json(
            _Requests.offerinfo.format(
                offerid=_Offers.prescription_unknown.waremd5,
                prescription_drugs_delivery=1,
            )
        )
        self._check_offer_delivery_not_in(response, _Offers.prescription_unknown.waremd5)

    def test_prescription_forbidden_medicament_offer_delivery(self):
        """
        Проверяем, что передаются медицинские флаги для расчета доставки на базовом поиске.
        """

        # Отсутсвие доставки у рецептурного препарата с запрещенным веществами
        response = self.report.request_json(
            _Requests.offerinfo.format(
                offerid=_Offers.prescription_forbidden_medicament.waremd5,
                prescription_drugs_delivery=1,
            )
        )
        self._check_offer_delivery_not_in(response, _Offers.prescription_forbidden_medicament.waremd5)

    def test_baa_offer_delivery(self):
        """
        Проверяем, что передаются медицинские флаги для расчета доставки на базовом поиске.
        """

        # У медицинского бада есть опции доставки
        response = self.report.request_json(
            _Requests.offerinfo.format(offerid=_Offers.baa_with_delivery.waremd5, prescription_drugs_delivery=1)
        )
        self._check_offer_delivery_in(response, _Offers.baa_with_delivery.waremd5)

    def test_baa_prescription_offer_delivery(self):
        """
        Проверяем, что передаются медицинские флаги для расчета доставки на базовом поиске.
        """

        # Отсутсвие доствки у медицинского бада с рецептурностью
        response = self.report.request_json(
            _Requests.offerinfo.format(
                offerid=_Offers.baa_prescription_without_delivery.waremd5,
                prescription_drugs_delivery=1,
            )
        )
        self._check_offer_delivery_not_in(response, _Offers.baa_prescription_without_delivery.waremd5)

    def test_baa_forbidden_medicament_offer_delivery(self):
        """
        Проверяем, что передаются медицинские флаги для расчета доставки на базовом поиске.
        """

        # Отсутсвие доставки у медицинского бада с запрещенным веществами
        response = self.report.request_json(
            _Requests.offerinfo.format(
                offerid=_Offers.baa_forbidden_medicament_without_delivery.waremd5,
                prescription_drugs_delivery=1,
            )
        )
        self._check_offer_delivery_not_in(response, _Offers.baa_forbidden_medicament_without_delivery.waremd5)

    def test_medical_product_offer_delivery(self):
        """
        Проверяем, что передаются медицинские флаги для расчета доставки на базовом поиске.
        """

        # У медицинского продукта есть опции доставки
        response = self.report.request_json(
            _Requests.offerinfo.format(
                offerid=_Offers.medical_product_with_delivery.waremd5,
                prescription_drugs_delivery=1,
            )
        )
        self._check_offer_delivery_in(response, _Offers.medical_product_with_delivery.waremd5)

    def test_medical_product_prescription_offer_delivery(self):
        """
        Проверяем, что передаются медицинские флаги для расчета доставки на базовом поиске.
        """

        # У медицинского рецептурного продукта есть опции доставки
        response = self.report.request_json(
            _Requests.offerinfo.format(
                offerid=_Offers.medical_product_prescription.waremd5,
                prescription_drugs_delivery=1,
            )
        )
        self._check_offer_delivery_in(response, _Offers.medical_product_prescription.waremd5)

        # Отсутсвие доставки у медицинского рецептурного продукта
        # (при запрете на доставку рецептурных лекарств)
        response = self.report.request_json(
            _Requests.offerinfo.format(
                offerid=_Offers.medical_product_prescription.waremd5,
                prescription_drugs_delivery=0,
            )
        )
        self._check_offer_delivery_not_in(response, _Offers.medical_product_prescription.waremd5)

    def test_medical_product_forbidden_medicament_offer_delivery(self):
        """
        Проверяем, что передаются медицинские флаги для расчета доставки на базовом поиске.
        """

        # Отсутсвие доставки у медицинского продукта с запрещенным веществами
        response = self.report.request_json(
            _Requests.offerinfo.format(
                offerid=_Offers.medical_product_forbidden_medicament_without_delivery.waremd5,
                prescription_drugs_delivery=1,
            )
        )
        self._check_offer_delivery_not_in(
            response, _Offers.medical_product_forbidden_medicament_without_delivery.waremd5
        )


if __name__ == '__main__':
    main()
