#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import Currency, DynamicDeliveryServiceInfo, Outlet, Phone, Shop, Tax
from core.matcher import Absent

# Магазины и поставщики
SHOP_ID = 1

# Службы доставки
SBERLOGISTICS_SERVICE_ID = 10
DPD_SERVICE_ID = 11

# ПВЗ
MBI_CC_OUTLET_ID = 100
MARKET_BRANDED_OUTLET_ID = 10000000010
MARKET_PARTNER_OUTLET_ID = 10000000011

# Идентификатор ПЗВ от MBI, который отсуствует в shopsOutlet.v2.xml
# Информация о нем доступна только через механизм алиасов для ПВЗ c
# идентификатором '10000000003'
ABSENT_MBI_OUTLET_ID = 1001

SBERLOGISTICS_OUTLET_IDS = [
    # (MBI id, LMS id, create MBI outlet)
    (1000, 10000000001, True),
    (ABSENT_MBI_OUTLET_ID, 10000000002, False),
]

DPD_OUTLET_IDS = [
    # MBI id
    1002,
    1003,
]

ALL_PRESENT_MBI_OUTLET_IDS = [1000, 1002, 1003]

ALL_PRESENT_LMS_OUTLET_IDS = [10000000001, 10000000002]

# Шаблоны запросов
DELIVERY_SERVICE_REQUEST = (
    'place=outlets&'
    'bsformat=2&'
    'pp=18&'
    'rgb={color}&'
    'deliveryServiceId={delivery_service}&'
    'rearr-factors=market_use_lms_outlets={exp_flag}'
)


class T(TestCase):
    """
    Набор тестов для 'place=outlets' на корректную работу с отображением идентификаторов
    из нумерации LMS в нумерацию MBI

    Данное отображение используется для постепенного перевода синего Report на работу с ПВЗ от LMS
    (служба за службой) под экспериментальным флагом 'market_use_lms_outlets'

    См. https://st.yandex-team.ru/MARKETOUT-30693
    """

    @staticmethod
    def __create_outlets(point_id, fesh=None, delivery_service_id=None, mbi_alias_point_id=None, create_mbi=False):
        outlets = [
            Outlet(
                point_id=point_id,
                fesh=fesh,
                delivery_service_id=delivery_service_id,
                mbi_alias_point_id=mbi_alias_point_id,
                email="{prefix}_point_{id}".format(
                    prefix=("lms" if mbi_alias_point_id is not None else "mbi"), id=point_id
                ),
                point_type=Outlet.FOR_PICKUP,
                working_days=list(range(10)),
                bool_props=['cashAllowed', 'cardAllowed', 'prepayAllowed'],
            )
        ]
        if mbi_alias_point_id is not None and create_mbi:
            outlets += [
                Outlet(
                    point_id=mbi_alias_point_id,
                    fesh=fesh,
                    delivery_service_id=delivery_service_id,
                    email="mbi_point_{id}".format(id=mbi_alias_point_id),
                    point_type=Outlet.FOR_PICKUP,
                    working_days=list(range(10)),
                    bool_props=['cashAllowed', 'cardAllowed', 'prepayAllowed'],
                )
            ]

        return outlets

    @classmethod
    def prepare(cls):
        cls.settings.report_subrole = 'blue-main'
        cls.settings.lms_autogenerate = True

    @classmethod
    def prepare_shops(cls):
        outlet_id_list = ALL_PRESENT_MBI_OUTLET_IDS + ALL_PRESENT_LMS_OUTLET_IDS
        cls.index.shops += [
            Shop(
                fesh=SHOP_ID,
                name="Тестовый магазин",
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                delivery_service_outlets=outlet_id_list,
            )
        ]

    @classmethod
    def prepare_outlets(cls):
        cls.index.outlets += [
            Outlet(
                point_id=MBI_CC_OUTLET_ID,
                fesh=SHOP_ID,
                point_type=Outlet.FOR_PICKUP,
                working_days=list(range(10)),
                bool_props=['cashAllowed', 'cardAllowed', 'prepayAllowed'],
            )
        ]
        for mbi_id, lms_id, create_mbi in SBERLOGISTICS_OUTLET_IDS:
            cls.index.outlets += T.__create_outlets(
                point_id=lms_id,
                delivery_service_id=SBERLOGISTICS_SERVICE_ID,
                mbi_alias_point_id=mbi_id,
                create_mbi=create_mbi,
            )

        for mbi_id in DPD_OUTLET_IDS:
            cls.index.outlets += T.__create_outlets(point_id=mbi_id, delivery_service_id=DPD_SERVICE_ID)

    @classmethod
    def prepare_lms(cls):
        cls.dynamic.lms += [
            DynamicDeliveryServiceInfo(id=SBERLOGISTICS_SERVICE_ID),
            DynamicDeliveryServiceInfo(id=DPD_SERVICE_ID),
        ]

    def test_outlets_param(self):
        """
        Проверяем, что при взведенном экспериментальном флаге 'market_use_lms_outlets'
        по CGI-параметру '&outlets' в выдаче присутствуют ПВЗ как от MBI, так и от LMS

        Если параметр выставлен в 'false' или отсутствует, то в выдаче - только ПВЗ от MBI
        """
        request_string = (
            'place=outlets&'
            'bsformat=2&'
            'pp=18&'
            'rgb={color}&'
            'outlets={outlet_list}&'
            'rearr-factors=market_use_lms_outlets={exp_flag}'
        )
        outlet_id_list = (
            ALL_PRESENT_MBI_OUTLET_IDS + ALL_PRESENT_LMS_OUTLET_IDS + [ABSENT_MBI_OUTLET_ID, MBI_CC_OUTLET_ID]
        )

        for flag_value in [0, 1]:
            for color in ['blue', 'white']:
                response = self.report.request_json(
                    request_string.format(
                        color=color,
                        outlet_list=','.join(str(outlet_id) for outlet_id in outlet_id_list),
                        exp_flag=flag_value,
                    )
                )
                self.assertFragmentIn(
                    response,
                    {'results': [{'entity': 'outlet', 'id': str(outlet_id)} for outlet_id in outlet_id_list]},
                    allow_different_len=False,
                )

    def test_sberlogistics_delivery_service(self):
        """
        Проверяем выдачу для СД "СберЛогистика". Для нее определены:
            1. ПВЗ от LMS и его аналог от MBI, который еще присутствует в выгрузке 'shopsOutlet.v2.xml'
            2. ПВЗ от LMS с "алиасом" на уже удаленный из выгрузки MBI-аналог

        Если флаг выставлен в 'true', выдаем ПВЗ с идентификаторами и "мясом" от LMS

        Если флаг не выставлен или равен 'false', то выдаем ПВЗ с идентификаторами от MBI и "мясом" от MBI,
        если оно еще есть в xml-файле. Иначе - "мясо" от LMS (именно так работает логика "алиасов")
        """
        for color in ['blue', 'white']:
            response = self.report.request_json(
                DELIVERY_SERVICE_REQUEST.format(color=color, delivery_service=SBERLOGISTICS_SERVICE_ID, exp_flag=1)
            )
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {'entity': 'outlet', 'id': str(lms_id), 'email': "lms_point_{id}".format(id=lms_id)}
                        for _, lms_id, _ in SBERLOGISTICS_OUTLET_IDS
                    ]
                },
                allow_different_len=False,
            )

            response = self.report.request_json(
                DELIVERY_SERVICE_REQUEST.format(color=color, delivery_service=SBERLOGISTICS_SERVICE_ID, exp_flag=0)
            )
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'outlet',
                            'id': str(mbi_id),
                            'email': "mbi_point_{id}".format(id=mbi_id)
                            if present_in_xml
                            else "lms_point_{id}".format(id=lms_id),
                        }
                        for mbi_id, lms_id, present_in_xml in SBERLOGISTICS_OUTLET_IDS
                    ]
                },
                allow_different_len=False,
            )

    def test_dpd_delivery_service(self):
        """
        Проверяем выдачу для СД "DPD". Для нее определены ПВЗ только в нумерации MBI,
        поэтому при любом значении флага в выдаче присутствуют только они
        """
        for color in ['blue', 'white']:
            for flag_value in [0, 1]:
                response = self.report.request_json(
                    DELIVERY_SERVICE_REQUEST.format(color=color, delivery_service=DPD_SERVICE_ID, exp_flag=flag_value)
                )
                self.assertFragmentIn(
                    response,
                    {
                        'results': [
                            {'entity': 'outlet', 'id': str(outlet_id), 'email': "mbi_point_{id}".format(id=outlet_id)}
                            for outlet_id in DPD_OUTLET_IDS
                        ]
                    },
                    allow_different_len=False,
                )

    @classmethod
    def prepare_market_outlet(cls):
        cls.index.outlets += [
            Outlet(
                point_id=MARKET_BRANDED_OUTLET_ID,
                fesh=SHOP_ID,
                point_type=Outlet.FOR_PICKUP,
                working_days=list(range(10)),
                bool_props=['cashAllowed', 'cardAllowed', 'prepayAllowed', "isMarketBranded"],
                phones=[
                    Phone('+7-495-123-45-67*89'),
                    Phone('+7-495-987-65-43*21'),
                ],
            ),
            Outlet(
                point_id=MARKET_PARTNER_OUTLET_ID,
                fesh=SHOP_ID,
                point_type=Outlet.FOR_PICKUP,
                working_days=list(range(10)),
                bool_props=['cashAllowed', 'cardAllowed', 'prepayAllowed', "isMarketPartner"],
                phones=[
                    Phone('+7-495-123-45-67*89'),
                    Phone('+7-495-987-65-43*21'),
                ],
            ),
        ]

    def test_phones_for_market_outlets(self):
        for flag in [1, 0, None]:
            rearr_factors = (
                "&rearr-factors=disable_telephone_for_market_outlets=" + str(flag) if flag is not None else ""
            )
            outlets = ','.join(str(id) for id in [MARKET_BRANDED_OUTLET_ID, MARKET_PARTNER_OUTLET_ID])
            response = self.report.request_json("place=outlets&outlets={}".format(outlets) + rearr_factors)
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'outlet',
                            'id': str(id),
                            "telephones": [
                                {
                                    "entity": "telephone",
                                    "countryCode": "+7",
                                    "cityCode": "495",
                                    "telephoneNumber": "123-45-67",
                                    "extensionNumber": "89",
                                },
                                {
                                    "entity": "telephone",
                                    "countryCode": "+7",
                                    "cityCode": "495",
                                    "telephoneNumber": "987-65-43",
                                    "extensionNumber": "21",
                                },
                            ]
                            if flag == 0
                            else Absent(),
                        }
                        for id in [MARKET_BRANDED_OUTLET_ID, MARKET_PARTNER_OUTLET_ID]
                    ]
                },
                allow_different_len=False,
            )


if __name__ == '__main__':
    main()
