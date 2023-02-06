#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BlueOffer,
    CategoryRestriction,
    Currency,
    DeliveryBucket,
    DeliveryCalendar,
    DeliveryOption,
    Disclaimer,
    GLParam,
    GLType,
    HyperCategory,
    MarketSku,
    MnPlace,
    Model,
    NavCategory,
    Offer,
    Outlet,
    PickupBucket,
    PickupOption,
    Region,
    RegionalDelivery,
    RegionalRestriction,
    Shop,
    SortingCenterReference,
    Tax,
    DynamicWarehouseInfo,
    DynamicWarehouseToWarehouseInfo,
)
from core.types.autogen import Const
from core.testcase import TestCase, main
from core.matcher import LikeUrl, NoKey, Absent, NotEmptyList, Greater, EmptyList


# Снятие ограничения на нерецептурные лекарства
DRUGS_CATEGORY = 15758037

# Некоторая немедицинская категория содержащия бады
BAA_CATEGORY = 15758038

BAA_PARAM_ID_IN_NOT_MEDICINE_CATEGORY = 17766785

MODEL_NOT_PRESCRIPTION_MEDICAL_BAA = 34001
MODEL_NOT_PRESCRIPTION_MEDICINE_TYPE = 34002
MODEL_NOT_PRESCRIPTION_OTHER_TYPE = 34003
MODEL_NOT_PRESCRIPTION_MEDICAL_PRODUCT = 34004
MODEL_NOT_PRESCRIPTION_WITH_NOT_ALLOWED_ALCOHOL = 34005
MODEL_NOT_PRESCRIPTION_WITH_NARCOTIC = 34006
MODEL_NOT_PRESCRIPTION_WITH_PSYCHOTROPIC = 34007
MODEL_NOT_PRESCRIPTION_WITH_PRECURSOR = 34008

MODEL_BAA_IN_NOT_MEDICINE_CATEGORY = 34009
MODEL_NOT_BAA_IN_NOT_MEDICINE_CATEGORY = 34010
MODEL_WITHOUT_BAA_PARAM_IN_NOT_MEDICINE_CATEGORY = 34011
MODEL_BAA_IN_NOT_MEDICINE_CATEGORY_BLUE = 34012
MODEL_NOT_PRESCRIPTION_2 = 34013
MODEL_NOT_PRESCRIPTION_MEDICAL_BAA_WITHOUT_DELIVERY_LICENSE = 34014


# Предупреждения
MEDICINE1_WARNING = {
    "type": "medicine1",
    "value": {
        "full": "Лекарство. Не доставляется. Продается только в аптеках.",
        "short": "Лекарство. Продается в аптеках",
    },
}

MEDICINE2_WARNING = {"type": "medicine2", "value": {"full": "Для детей от 6 месяцев", "short": "С 6 месяцев"}}

MEDICINE2_AGE_WARNING = {
    "type": "medicine2",
    "age": 6,
    "value": {"full": "Для детей от 6 месяцев", "short": "С 6 месяцев"},
}

MEDICINE3_WARNING = {
    "type": "medicine3",
    "age": 10,
    "value": {"full": "Для детей от 10 месяцев", "short": "С 10 месяцев"},
}

SUPPLEMENT_WARNING = {
    "type": "supplement",
    "value": {"full": "БАД. Не является лекарством", "short": "Не является лекарством"},
}


def generate_warnings_response(title, category, common_warnings=None, specification_warnings=None, extra_fields=None):
    if common_warnings or specification_warnings:
        warnings = {}
        if common_warnings:
            warnings.update({"common": common_warnings})
        if specification_warnings:
            warnings.update({"specification": specification_warnings})
    else:
        warnings = Absent()

    result = {
        "titles": {"raw": title},
        "categories": [{"id": category}],
        "warnings": warnings,
    }

    if extra_fields:
        result.update(extra_fields)

    return result


def create_medical_offer_with_delivery(
    hyperid,
    fesh=18,  # predefined shop-id (see above)
    feedid=18,  # predefined shop-id (see above)
    is_medicine=False,
    is_baa=False,
    is_medical_product=False,
    is_prescription=False,
    is_ethanol=False,
    is_precursor=False,
    is_narcotic=False,
    is_psychotropic=False,
):
    return Offer(
        hyperid=hyperid,
        is_medicine=is_medicine,
        is_baa=is_baa,
        is_medical_product=is_medical_product,
        is_prescription=is_prescription,
        is_ethanol=is_ethanol,
        is_narcotic=is_narcotic,
        is_precursor=is_precursor,
        is_psychotropic=is_psychotropic,
        fesh=fesh,
        feedid=feedid,
        title="Офер с доставкой",
        delivery_buckets=[4380],
        delivery_options=[DeliveryOption(price=20, day_from=1, day_to=2)],
    )


def create_medical_offer_with_pickup_and_delivery(
    hyperid,
    fesh=17,  # predefined shop-id (see above)
    feedid=17,  # predefined shop-id (see above)
    is_medicine=False,
    is_baa=False,
    is_medical_product=False,
    is_prescription=False,
    is_ethanol=False,
    is_narcotic=False,
    is_precursor=False,
    is_psychotropic=False,
):
    return Offer(
        hyperid=hyperid,
        is_medicine=is_medicine,
        is_baa=is_baa,
        is_medical_product=is_medical_product,
        is_prescription=is_prescription,
        is_ethanol=is_ethanol,
        is_narcotic=is_narcotic,
        is_precursor=is_precursor,
        is_psychotropic=is_psychotropic,
        fesh=fesh,
        feedid=feedid,
        title="Офер с ПВЗ и доставкой",
        pickup_buckets=[4381, 4382],
        delivery_buckets=[4380],
        delivery_options=[DeliveryOption(price=20, day_from=1, day_to=2)],
    )


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']
        # RANDX randomizing is disabled because these tests don't work with it. See MARKETOUT-21319
        cls.disable_randx_randomize()

        cls.index.category_restrictions += [
            CategoryRestriction(
                name='medicine',
                hids=[101],
                regional_restrictions=[
                    RegionalRestriction(
                        show_offers=False,
                        display_only_matched_offers=False,
                        delivery=False,
                        rids=[301, 302],
                        rids_with_subtree=[303],
                        disclaimers=[
                            Disclaimer(
                                name='medicine1',
                                text='Лекарство. Не доставляется. Продается только в аптеках.',
                                short_text='Лекарство. Продается в аптеках',
                            ),
                            Disclaimer(
                                name='medicine2', age='6month', text='Для детей от 6 месяцев', short_text='С 6 месяцев'
                            ),
                            Disclaimer(
                                name='medicine3',
                                age='10month',
                                text='Для детей от 10 месяцев',
                                short_text='С 10 месяцев',
                            ),
                        ],
                    ),
                ],
            ),
            CategoryRestriction(
                name='vitamines',
                hids=[102],
                regional_restrictions=[
                    RegionalRestriction(
                        show_offers=True,
                        display_only_matched_offers=False,
                        delivery=True,
                        rids=[301, 302],
                        rids_with_subtree=[303],
                        disclaimers=[
                            Disclaimer(name='medicine4', text='Витамины', short_text='Витамины'),
                        ],
                    ),
                    RegionalRestriction(
                        show_offers=True,
                        display_only_matched_offers=False,
                        delivery=False,
                        rids=[305],
                        disclaimers=[
                            Disclaimer(name='medicine5', text='Витамины', short_text='Витамины'),
                        ],
                    ),
                    RegionalRestriction(
                        show_offers=True,
                        display_only_matched_offers=False,
                        delivery=False,
                        disclaimers=[
                            Disclaimer(name='medicine6', text='Витамины во всех регионах'),
                        ],
                    ),
                ],
            ),
            CategoryRestriction(
                name='medicine-prescripted',
                hids_with_subtree=[103],
                regional_restrictions=[
                    RegionalRestriction(
                        show_offers=False,
                        display_only_matched_offers=False,
                        delivery=False,
                        rids=[301, 302],
                        rids_with_subtree=[303],
                        disclaimers=[
                            Disclaimer(
                                name='medicine7',
                                text='Лекарство. Только по рецепту врача.',
                                short_text='По рецепту врача',
                            ),
                        ],
                    ),
                ],
            ),
            CategoryRestriction(
                name='supplements',
                hids_with_subtree=[106],
                hids=[101],
                regional_restrictions=[
                    RegionalRestriction(
                        show_offers=False,
                        display_only_matched_offers=False,
                        delivery=False,
                        rids=[302],
                        rids_with_subtree=[303],
                        disclaimers=[
                            Disclaimer(
                                name='supplement',
                                text='БАД. Не является лекарством',
                                short_text='Не является лекарством',
                                default_warning=False,
                            ),
                        ],
                    ),
                    RegionalRestriction(
                        show_offers=False,
                        display_only_matched_offers=True,
                        delivery=True,
                        rids=[304, 307],
                        disclaimers=[
                            Disclaimer(
                                name='supplement',
                                text='БАД. Не является лекарством',
                                short_text='Не является лекарством',
                                default_warning=False,
                            ),
                        ],
                    ),
                    RegionalRestriction(
                        show_offers=True,
                        display_only_matched_offers=False,
                        delivery=False,
                        rids=[301, 306],
                        disclaimers=[
                            Disclaimer(
                                name='supplement',
                                text='БАД. Не является лекарством',
                                short_text='Не является лекарством',
                                default_warning=False,
                            ),
                        ],
                    ),
                ],
            ),
            CategoryRestriction(
                name='age',
                hids_with_subtree=[108],
                regional_restrictions=[
                    RegionalRestriction(
                        rids=[Const.ROOT_COUNTRY],
                        show_offers=True,
                        disclaimers=[
                            Disclaimer(name='age', text='Для взрослых', short_text='Для взрослых'),
                        ],
                    )
                ],
            ),
        ]

        cls.index.regiontree += [
            Region(rid=301, name='Ленинградская область', region_type=Region.FEDERATIVE_SUBJECT),
            Region(
                rid=303,
                name='Московская область',
                region_type=Region.FEDERATIVE_SUBJECT,
                children=[
                    Region(rid=302, name='Москва'),
                    Region(rid=304, name='Химки'),
                ],
            ),
            Region(
                rid=305,
                name='Нижегородская область',
                region_type=Region.FEDERATIVE_SUBJECT,
                genitive='Нижегородской области',
                locative='Нижегородской области',
                preposition='в',
            ),
            Region(rid=306, name='Владивосток', genitive='Владивостока', locative='Владивостоке', preposition='во'),
            Region(rid=307, name='Хабаровск'),
        ]

        cls.index.hypertree += [
            HyperCategory(
                hid=101,
                name='Лекарства',
                children=[
                    HyperCategory(hid=102, name='Витамины'),
                    HyperCategory(
                        hid=103,
                        name='Рецептурные',
                        children=[
                            HyperCategory(hid=104, name='Рецептурные без побочных эффектов'),
                        ],
                    ),
                    HyperCategory(hid=106, name='БАД'),
                ],
            ),
            HyperCategory(
                hid=108, name='Книжки детям не игрушки', children=[HyperCategory(hid=109, name='Детские энциклопедии')]
            ),
        ]

        cls.index.navtree += [
            NavCategory(nid=306, hid=106),
        ]

        cls.index.shops = [
            Shop(
                fesh=1,
                priority_region=301,
                regions=[302, 304],
                name='Магазин в Питере с доставкой в Москву и Химки',
                pickup_buckets=[5001],
            )
        ]

        cls.index.shops += [
            Shop(fesh=x, priority_region=301, regions=[302, 304, 305, 306], name='Магазин{} с доставкой'.format(x))
            for x in [2, 3, 11, 12, 13, 14, 15, 16]
        ]

        cls.index.outlets += [
            Outlet(fesh=1, region=301, point_type=Outlet.FOR_STORE, point_id=1),
            Outlet(fesh=1, region=302, point_type=Outlet.FOR_STORE, point_id=2),
            Outlet(fesh=1, region=305, point_type=Outlet.FOR_STORE, point_id=3),
            Outlet(fesh=1, region=306, point_type=Outlet.FOR_STORE, point_id=4),
            Outlet(fesh=1, region=307, point_type=Outlet.FOR_STORE, point_id=5),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5001,
                fesh=1,
                carriers=[99],
                options=[
                    PickupOption(outlet_id=1),
                    PickupOption(outlet_id=2),
                    PickupOption(outlet_id=3),
                    PickupOption(outlet_id=4),
                    PickupOption(outlet_id=5),
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        cls.index.models += [
            Model(hyperid=401, title='Киянка резиновая', hid=106),
            Model(hyperid=402, title='Название0'),
            Model(hyperid=403, title='Название1', disclaimers_model='medicine1'),
            Model(hyperid=404, title='Название2', hid=102),
            Model(hyperid=405, title='Название3', hid=102, disclaimers_model='medicine1'),
            Model(
                hyperid=406,
                title='Нурофен-детский-свечи-с-6-месяцев',
                hid=101,
                disclaimers_model='medicine2',
                warning_specification="supplement",
            ),
            Model(hyperid=408, ts=408, title='Соль антидепрессантная (с шоколадом)', hid=106, group_hyperid=100408),
            Model(
                hyperid=409,
                ts=409,
                title='Аналог соли (с мармеладом)',
                hid=106,
                group_hyperid=100408,
                disclaimers_model='supplement',
            ),
            Model(hyperid=410, title='Добавка йодированная', hid=106),
            Model(
                hyperid=4111,
                title='Сахар обыкновенный рафинированный',
                hid=106,
                group_hyperid=100411,
                disclaimers_model='supplement',
            ),
            Model(hyperid=4112, title='Сахар обыкновенный рафинированный', hid=106, group_hyperid=100411),
            Model(
                hyperid=4113,
                title='Сахар обыкновенный рафинированный',
                hid=106,
                group_hyperid=100411,
                disclaimers_model='supplement',
            ),
            Model(hyperid=4114, title='Сахар обыкновенный рафинированный', hid=106, group_hyperid=100411),
            Model(
                hyperid=4115,
                title='Сахар обыкновенный рафинированный',
                hid=106,
                group_hyperid=100411,
                disclaimers_model='supplement',
            ),
            Model(hyperid=4116, title='Сахар обыкновенный рафинированный', hid=106, group_hyperid=100411),
            Model(
                hyperid=5001,
                title='магия медицина',
                hid=108,
                disclaimers_model='medicine2',
                warning_specification="supplement",
            ),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 408).respond(0.02)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 409).respond(0.01)

        cls.index.offers += [
            Offer(title='Просто-лекарство', hid=101, fesh=1),
            Offer(title='Витаминка', hid=102, fesh=1),
            Offer(title='Лекарство-без-побочных-эффектов', hid=104, fesh=1),
            Offer(title='БАД без модели', hid=106, fesh=1),
            Offer(title='БАД с моделью', hyperid=401, hid=106, fesh=1),
            Offer(title='Лекарство-для-детей-от-6-месяцев', hid=101, fesh=1, age='6', age_unit='month'),
            Offer(title='Лекарство-для-детей-от-10-месяцев', hid=101, fesh=1, age='10', age_unit='month'),
            Offer(title='Название0', hyperid=402),
            Offer(title='Название1', hyperid=403),
            Offer(title='Название2', hid=102, fesh=1, hyperid=404),
            Offer(title='Название3', hid=102, fesh=1, hyperid=405),
            Offer(hyperid=408, hid=106, fesh=2, pickup_buckets=[5002]),
            Offer(hyperid=409, hid=106, fesh=3, pickup_buckets=[5003]),
            Offer(hyperid=410, hid=106, fesh=2, pickup_buckets=[5002]),
            Offer(hyperid=4111, hid=106, fesh=11),
            Offer(hyperid=4112, hid=106, fesh=12),
            Offer(hyperid=4113, hid=106, fesh=13),
            Offer(hyperid=4114, hid=106, fesh=14),
            Offer(hyperid=4115, hid=106, fesh=15),
            Offer(hyperid=4116, hid=106, fesh=16),
            Offer(
                title='Основы черной магии. Пособие для начинающих.',
                hyperid=5001,
                hid=108,
                age='12',
                age_unit='year',
                fesh=1,
            ),
            Offer(title='Травоведение. Начала зельеварения.', hyperid=5001, hid=108, fesh=1, ts=50010),
            Offer(title='Тело человека', hyperid=5002, hid=109, age='6', age_unit='year', fesh=1),
            Offer(title='Пони и лошади', hyperid=5002, hid=109, fesh=1),
        ]

        # geo
        cls.index.category_restrictions += [
            CategoryRestriction(
                name='porn',
                hids=[107],
                regional_restrictions=[
                    RegionalRestriction(
                        rids=[301],
                        show_offers=False,
                        disclaimers=[
                            Disclaimer(name='porn', text='Sluts, whores, bitches.', short_text='Babes'),
                        ],
                    )
                ],
            ),
        ]

        cls.index.outlets += [
            Outlet(fesh=2, region=301, point_type=Outlet.FOR_STORE, point_id=20),
            Outlet(fesh=3, region=302, point_type=Outlet.FOR_STORE, point_id=30),
            Outlet(fesh=2, region=302, point_type=Outlet.FOR_STORE, point_id=40),
            Outlet(fesh=3, region=301, point_type=Outlet.FOR_STORE, point_id=50),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5002,
                fesh=2,
                carriers=[99],
                options=[PickupOption(outlet_id=20), PickupOption(outlet_id=40)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5003,
                fesh=3,
                carriers=[99],
                options=[PickupOption(outlet_id=30), PickupOption(outlet_id=50)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        cls.index.offers += [
            Offer(fesh=2, hyperid=407, hid=107, pickup_buckets=[5002]),
            Offer(fesh=3, hyperid=407, hid=107, pickup_buckets=[5003]),
        ]

    def test_age_disclaimer(self):
        """
        Проверка возрастного дисклеймера для категории, включая ее подкатегории
        """
        for place in ['productoffers', 'prime']:
            response = self.report.request_json('place={}&hyperid=5001,5002&rids=301'.format(place))
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {"titles": {"raw": "Основы черной магии. Пособие для начинающих."}, "age": "12"},
                        {"titles": {"raw": "Травоведение. Начала зельеварения."}, "age": "18"},
                        {"titles": {"raw": "Пони и лошади"}, "age": "18"},
                        {"titles": {"raw": "Тело человека"}, "age": "6"},
                    ]
                },
            )

    def test_allow_explicit_content(self):
        """
        Проверяется, что явный показ контента работает как с cgi параметром, так и с rearr флагом
        """
        for param in ("show_explicit_content", "rearr-factors=show_explicit_content"):
            response = self.report.request_json(
                'place=prime&text=Лекарство&rids=301&{}=medicine,medicine-prescripted'.format(param)
            )
            self.assertEqual(response.count({"entity": "offer"}), 4)

            response = self.report.request_json(
                'place=prime&text=Лекарство-без-побочных-эффектов&rids=302&{}=medicine-prescripted'.format(param)
            )
            self.assertEqual(response.count({"entity": "offer"}), 1)

            response = self.report.request_json('place=prime&text=БАД&rids=307&{}=supplements'.format(param))
            self.assertEqual(response.count({"entity": "offer"}), 1)

            # Курьерки нет из-за ограничения, и аутлета в регионе нет
            response = self.report.request_json('place=prime&text=БАД&rids=304&{}=supplements'.format(param))
            self.assertEqual(response.count({"entity": "offer"}), 0)

    def test_combine_param_and_flag_content(self):
        """
        Проверяется, что cgi параметр, и экспериментальный флаг могут работать совместно
        """
        param, flag = "show_explicit_content", "rearr-factors=show_explicit_content"
        for (p1, p2) in ((param, flag), (flag, param)):
            response = self.report.request_json(
                'place=prime&text=Лекарство&rids=301&{}=medicine&{}=medicine-prescripted'.format(p1, p2)
            )
            self.assertEqual(response.count({"entity": "offer"}), 4)

    # MARKETOUT-9059
    def test_allow_explicit_content_all_1(self):
        """
        Запрос из теста `test_allow_explicit_content`, но с =all
        Передача all также работает как с cgi параметром, так и с rearr флагом
        """
        for param in ("show_explicit_content", "rearr-factors=show_explicit_content"):
            response = self.report.request_json('place=prime&text=Лекарство&rids=301&{}=all'.format(param))
            self.assertEqual(response.count({"entity": "offer"}), 4)

    # MARKETOUT-9059
    def test_allow_explicit_content_all_2(self):
        """
        Запрос из теста `test_allow_explicit_content`, но с =all
        """
        response = self.report.request_json(
            'place=prime&text=Лекарство-без-побочных-эффектов&rids=302&show_explicit_content=medicine-prescripted'
        )
        self.assertEqual(response.count({"entity": "offer"}, preserve_order=True), 1)

    # MARKETOUT-9059
    def test_allow_explicit_content_all_3(self):
        """
        Запрос из теста `test_allow_explicit_content`, но с =all
        """
        response = self.report.request_json('place=prime&text=БАД&rids=307&show_explicit_content=all')
        self.assertEqual(response.count({"entity": "offer"}), 1)

        response = self.report.request_json('place=prime&text=БАД&rids=304&show_explicit_content=all')
        self.assertEqual(response.count({"entity": "offer"}), 0)

    def test_warnings_age(self):
        response = self.report.request_json(
            'place=prime&text=Лекарство-для-детей-от-6-месяцев&rids=301&show_explicit_content=medicine&rearr-factors=market_no_strict_distances=0'
        )
        self.assertFragmentIn(
            response,
            {
                "warnings": {
                    "common": [
                        {
                            "type": "medicine2",
                            "age": 6,
                            "value": {"full": "Для детей от 6 месяцев", "short": "С 6 месяцев"},
                        }
                    ]
                }
            },
        )

        response = self.report.request_json(
            'place=prime&text=Лекарство-для-детей-от-10-месяцев&rids=302&show_explicit_content=medicine,supplements&rearr-factors=market_no_strict_distances=0'
        )
        self.assertFragmentIn(
            response,
            {
                "warnings": {
                    "common": [
                        {
                            "type": "medicine3",
                            "age": 10,
                            "value": {"full": "Для детей от 10 месяцев", "short": "С 10 месяцев"},
                        }
                    ]
                }
            },
        )

    # MARKETOUT-9059
    def test_warnings_age_1(self):
        """
        Тот же запрос, что и в `test_warnings_age`
        Но с =all
        """
        response = self.report.request_json(
            'place=prime&text=Лекарство-для-детей-от-6-месяцев&rids=301&show_explicit_content=all&rearr-factors=market_no_strict_distances=0'
        )
        self.assertFragmentIn(
            response,
            {
                "warnings": {
                    "common": [
                        {
                            "type": "medicine2",
                            "age": 6,
                            "value": {"full": "Для детей от 6 месяцев", "short": "С 6 месяцев"},
                        }
                    ]
                }
            },
        )

    # MARKETOUT-9059
    def test_warnings_age_2(self):
        """
        Тот же запрос, что и в `test_warnings_age`
        Но с =all
        """
        response = self.report.request_json(
            'place=prime&text=Лекарство-для-детей-от-10-месяцев&rids=302&show_explicit_content=all&rearr-factors=market_no_strict_distances=0'
        )
        self.assertFragmentIn(
            response,
            {
                "warnings": {
                    "common": [
                        {
                            "type": "medicine3",
                            "age": 10,
                            "value": {"full": "Для детей от 10 месяцев", "short": "С 10 месяцев"},
                        }
                    ]
                }
            },
        )

    def test_warnings_age_bug(self):  # MARKETOUT_8700
        response = self.report.request_json('place=prime&text=нурофен&rids=301')
        self.assertFragmentIn(response, {"search": {"total": 0, "totalOffers": 0, "totalModels": 0}})

        response = self.report.request_json('place=prime&text=нурофен&rids=301&show_explicit_content=medicine')
        self.assertFragmentIn(
            response,
            {
                "warnings": {
                    "common": [
                        {
                            "type": "medicine2",
                            "value": {
                                "full": "Для детей от 6 месяцев",
                            },
                        }
                    ]
                }
            },
        )

    # MARKETOUT-9059
    def test_warnings_age_bug_all(self):  # MARKETOUT_8700
        """
        Тот же запрос, что и в `test_warnings_age_bug`, но с =all
        """
        response = self.report.request_json('place=prime&text=нурофен&rids=301&show_explicit_content=all')
        self.assertFragmentIn(
            response,
            {
                "warnings": {
                    "common": [
                        {
                            "type": "medicine2",
                            "value": {
                                "full": "Для детей от 6 месяцев",
                            },
                        }
                    ]
                }
            },
        )

    def test_prime_warnings(self):
        response = self.report.request_json('place=prime&text=Просто-лекарство&rids=301&show_explicit_content=medicine')
        # Только "medicine1" является дефолтным предупреждением для категории 101
        self.assertFragmentIn(
            response,
            {
                "titles": {"raw": "Просто-лекарство"},
                "categories": [{"id": 101}],
                "warnings": {
                    "common": [
                        {
                            "type": "medicine1",
                            "value": {
                                "full": u"Лекарство. Не доставляется. Продается только в аптеках.",
                                "short": u"Лекарство. Продается в аптеках",
                            },
                        }
                    ]
                },
            },
        )

        response = self.report.request_json(
            'place=prime&text=Лекарство-для-детей-от-6-месяцев&rids=301&show_explicit_content=medicine'
        )
        self.assertFragmentIn(
            response,
            {
                "warnings": {
                    "common": [
                        {
                            "age": 6,
                            "type": "medicine2",
                            "value": {"full": u"Для детей от 6 месяцев", "short": u"С 6 месяцев"},
                        }
                    ]
                }
            },
        )

        response = self.report.request_json('place=prime&text=Витаминка&rids=302')
        self.assertFragmentIn(
            response,
            {"warnings": {"common": [{"type": "medicine4", "value": {"full": "Витамины", "short": "Витамины"}}]}},
        )

        response = self.report.request_json('place=prime&text=Витаминка&rids=306')
        self.assertFragmentIn(
            response,
            {
                "warnings": {
                    "common": [{"type": "medicine6", "value": {"full": "Витамины во всех регионах", "short": "None"}}]
                }
            },
        )

    # MARKETOUT-9059
    def test_prime_warnings_all_1(self):
        """
        Тот же запрос что и выше в `test_prime_warnings`, но с =all
        """
        response = self.report.request_json('place=prime&text=Просто-лекарство&rids=301&show_explicit_content=all')
        self.assertFragmentIn(
            response,
            {
                "warnings": {
                    "common": [
                        {
                            "type": "medicine1",
                            "value": {
                                "full": u"Лекарство. Не доставляется. Продается только в аптеках.",
                                "short": u"Лекарство. Продается в аптеках",
                            },
                        }
                    ]
                }
            },
        )

    # MARKETOUT-9059
    def test_prime_warnings_all_2(self):
        """
        Тот же запрос что и выше в `test_prime_warnings`, но с =all
        """
        response = self.report.request_json(
            'place=prime&text=Лекарство-для-детей-от-6-месяцев&rids=301&show_explicit_content=all'
        )
        self.assertFragmentIn(
            response,
            {
                "warnings": {
                    "common": [
                        {
                            "age": 6,
                            "type": "medicine2",
                            "value": {"full": u"Для детей от 6 месяцев", "short": u"С 6 месяцев"},
                        }
                    ]
                }
            },
        )

    # MARKETOUT-9059
    def test_prime_warnings_specification(self):
        '''Модель hyperid=406 имеет кроме дисклеймера модели (medicine2)
        еще и дисклеймер для страницы Характеристике на КМ (supplement)
        Проверяем, что они оба отображаются в списке warnings
        show_explicit_content равен  medicine или all
        '''
        product_id = {"id": 406, "entity": "product"}

        for content in ['medicine', 'all']:
            request = 'place=prime&text=нурофен&rids=301&show_explicit_content={}'.format(content)
            # выводится возрастное предупреждение. дефолтное для категории не выводится
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                generate_warnings_response(
                    title="Нурофен-детский-свечи-с-6-месяцев",
                    category=101,
                    common_warnings=[MEDICINE2_WARNING],
                    specification_warnings=[SUPPLEMENT_WARNING],
                    extra_fields=product_id,
                ),
                allow_different_len=False,
            )

    def test_filter_warnings(self):
        """Проверяем что параметр &filter-warnings отсеивает документы имеющие указанные дисклеймеры"""

        # без фильтра находится модель с ограничениями medicine2 и supplement
        response = self.report.request_json('place=prime&text=нурофен&rids=301&show_explicit_content=all')
        self.assertFragmentIn(
            response,
            {
                "entity": "product",
                "id": 406,
                "titles": {"raw": "Нурофен-детский-свечи-с-6-месяцев"},
                "warnings": {"common": [{"type": "medicine2"}], "specification": [{"type": "supplement"}]},
            },
        )

        # фильтрует ограничения в warnings.common
        response = self.report.request_json(
            'place=prime&text=нурофен&rids=301&show_explicit_content=all&filter-warnings=medicine2,other'
        )
        self.assertFragmentNotIn(response, {"entity": "product", "id": 406})

        # фильтрует ограничения в warnings.specification
        response = self.report.request_json(
            'place=prime&text=нурофен&rids=301&show_explicit_content=all&filter-warnings=supplement,other'
        )
        self.assertFragmentNotIn(response, {"entity": "product", "id": 406})

        # не фильтрует модель, если ее ограничения не указаны в фильтре
        response = self.report.request_json(
            'place=prime&text=нурофен&rids=301&show_explicit_content=all&filter-warnings=other'
        )
        self.assertFragmentIn(response, {"entity": "product", "id": 406})

    def test_productoffers_filter_warnings_no_filter(self):
        response = self.report.request_json('place=productoffers&hyperid=5001&rids=301')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"titles": {"raw": "Основы черной магии. Пособие для начинающих."}},
                    {"titles": {"raw": "Травоведение. Начала зельеварения."}},
                ]
            },
        )

    def test_productoffers_filter_warnings_common(self):
        response = self.report.request_json('place=productoffers&hyperid=5001&rids=301&filter-warnings=medicine2,other')
        self.assertFragmentNotIn(
            response,
            {
                "results": [
                    {"titles": {"raw": "Основы черной магии. Пособие для начинающих."}},
                    {"titles": {"raw": "Травоведение. Начала зельеварения."}},
                ]
            },
        )

    def test_productoffers_filter_warnings_specification(self):
        response = self.report.request_json(
            'place=productoffers&hyperid=5001&rids=301&filter-warnings=supplement,other'
        )
        self.assertFragmentNotIn(
            response,
            {
                "results": [
                    {"titles": {"raw": "Основы черной магии. Пособие для начинающих."}},
                    {"titles": {"raw": "Травоведение. Начала зельеварения."}},
                ]
            },
        )

    def test_productoffers_filter_warnings_other(self):
        response = self.report.request_json('place=productoffers&hyperid=5001&rids=301&filter-warnings=other')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"titles": {"raw": "Основы черной магии. Пособие для начинающих."}},
                    {"titles": {"raw": "Травоведение. Начала зельеварения."}},
                ]
            },
        )

    def test_productoffers_mulitiple_filter_warnings_no_filter(self):
        response = self.report.request_json('place=productoffers&use_multiple_hyperid=1&hyperid=5001,5002')
        self.assertFragmentIn(
            response,
            {
                "modelIdWithOffers": [
                    {
                        "model_id": 5001,
                        "offers": [
                            {"titles": {"raw": "Основы черной магии. Пособие для начинающих."}},
                            {"titles": {"raw": "Травоведение. Начала зельеварения."}},
                        ],
                    },
                    {"model_id": 5002},
                ]
            },
        )

    def test_productoffers_mulitiple_filter_warnings_common(self):
        response = self.report.request_json(
            'place=productoffers&use_multiple_hyperid=1&hyperid=5001,5002&filter-warnings=medicine2,other'
        )
        self.assertFragmentNotIn(
            response,
            {
                "modelIdWithOffers": [
                    {
                        "model_id": 5001,
                        "offers": [
                            {"titles": {"raw": "Основы черной магии. Пособие для начинающих."}},
                            {"titles": {"raw": "Травоведение. Начала зельеварения."}},
                        ],
                    }
                ]
            },
        )

    def test_productoffers_mulitiple_filter_warnings_specification(self):
        response = self.report.request_json(
            'place=productoffers&use_multiple_hyperid=1&hyperid=5001,5002&filter-warnings=supplement,other'
        )
        self.assertFragmentNotIn(
            response,
            {
                "modelIdWithOffers": [
                    {
                        "model_id": 5001,
                        "offers": [
                            {"titles": {"raw": "Основы черной магии. Пособие для начинающих."}},
                            {"titles": {"raw": "Травоведение. Начала зельеварения."}},
                        ],
                    }
                ]
            },
        )

    def test_productoffers_mulitiple_filter_warnings_other(self):
        response = self.report.request_json(
            'place=productoffers&use_multiple_hyperid=1&hyperid=5001,5002&filter-warnings=other'
        )
        self.assertFragmentIn(
            response,
            {
                "modelIdWithOffers": [
                    {
                        "model_id": 5001,
                        "offers": [
                            {"titles": {"raw": "Основы черной магии. Пособие для начинающих."}},
                            {"titles": {"raw": "Травоведение. Начала зельеварения."}},
                        ],
                    },
                    {"model_id": 5002},
                ]
            },
        )

    def test_productoffers_default_filter_warnings_no_filter(self):
        response = self.report.request_json(
            'place=productoffers&hyperid=5001&rids=301&offers-set=default&rearr-factors=prefer_do_with_sku=0'
        )
        self.assertFragmentIn(response, {"results": [{"titles": {"raw": "Корень мандрагоры"}}]})

    def test_productoffers_default_filter_warnings_common(self):
        response = self.report.request_json(
            'place=productoffers&hyperid=5001&rids=301&offers-set=default&filter-warnings=medicine2,other'
        )
        self.assertFragmentNotIn(response, {"results": [{"titles": {"raw": "Травоведение. Начала зельеварения."}}]})
        self.assertFragmentNotIn(
            response, {"results": [{"titles": {"raw": "Основы черной магии. Пособие для начинающих."}}]}
        )

    def test_productoffers_default_filter_warnings_specification(self):
        response = self.report.request_json(
            'place=productoffers&hyperid=5001&rids=301&offers-set=default&filter-warnings=supplement,other'
        )
        self.assertFragmentNotIn(response, {"results": [{"titles": {"raw": "Травоведение. Начала зельеварения."}}]})
        self.assertFragmentNotIn(
            response, {"results": [{"titles": {"raw": "Основы черной магии. Пособие для начинающих."}}]}
        )

    def test_productoffers_default_filter_warnings_other(self):
        response = self.report.request_json(
            'place=productoffers&hyperid=5001&rids=301&offers-set=default&filter-warnings=other&rearr-factors=prefer_do_with_sku=0'
        )
        self.assertFragmentIn(response, {"results": [{"titles": {"raw": "Корень мандрагоры"}}]})

    def test_modelinfo_filter_warnings_no_filter(self):
        response = self.report.request_json('place=modelinfo&hyperid=5001&rids=301')
        self.assertFragmentIn(response, {"type": "model", "id": 5001})

    def test_modelinfo_filter_warnings_common(self):
        response = self.report.request_json('place=modelinfo&hyperid=5001&rids=301&filter-warnings=medicine2,other')
        self.assertFragmentNotIn(response, {"type": "model", "id": 5001})

    def test_modelinfo_filter_warnings_specification(self):
        response = self.report.request_json('place=modelinfo&hyperid=5001&rids=301&filter-warnings=supplement,other')
        self.assertFragmentNotIn(response, {"type": "model", "id": 5001})

    def test_modelinfo_filter_warnings_other(self):
        response = self.report.request_json('place=modelinfo&hyperid=5001&rids=301&filter-warnings=other')
        self.assertFragmentIn(response, {"type": "model", "id": 5001})

    @classmethod
    def prepare_sku_offers_filter_warnings(cls):
        cls.index.mskus += [
            MarketSku(
                title="Корень мандрагоры",
                fesh=8,
                hyperid=5001,
                sku=1,
                randx=1,
                ref_min_price=100,
                blue_offers=[
                    BlueOffer(price=500, feedid=100, hid=108, ts=50011),
                ],
            )
        ]

    def test_sku_offers_filter_no_warnings(self):
        response = self.report.request_json('place=sku_offers&market-sku=1&rgb=blue&rids=301')
        self.assertFragmentIn(response, {"entity": "sku", "titles": {"raw": "Корень мандрагоры"}, "id": "1"})

    def test_sku_offers_filter_warnings_common(self):
        response = self.report.request_json(
            'place=sku_offers&market-sku=1&rgb=blue&rids=301&filter-warnings=medicine2,other'
        )
        self.assertFragmentNotIn(response, {"entity": "sku", "titles": {"raw": "Корень мандрагоры"}, "id": "1"})

    def test_sku_offers_filter_warnings_specification(self):
        response = self.report.request_json(
            'place=sku_offers&market-sku=1&rgb=blue&rids=301&filter-warnings=supplement,other'
        )
        self.assertFragmentNotIn(response, {"entity": "sku", "titles": {"raw": "Корень мандрагоры"}, "id": "1"})

    def test_sku_offers_filter_warnings_other(self):
        response = self.report.request_json('place=sku_offers&market-sku=1&rgb=blue&rids=301&filter-warnings=other')
        self.assertFragmentIn(response, {"entity": "sku", "titles": {"raw": "Корень мандрагоры"}, "id": "1"})

    # MARKETOUT-8970 не показывать в модельном колдунщике офферы с предупреждениями (но саму модель показывать можно)
    def test_warnings_in_model_wizard(self):

        # запрашиваем одиночную модель (410) в регионе, в котором для нее есть предупреждения.
        # колдунщик будет сформирован, но офферов не будет
        response = self.report.request_bs("place=parallel&text=Добавка%20йодированная&rids=306")
        self.assertFragmentIn(
            response,
            {
                "market_model": [
                    {
                        "title": {"__hl": {"raw": True, "text": "Добавка йодированная"}},
                        "url": LikeUrl.of(
                            '//market.yandex.ru/product--dobavka-iodirovannaia/410?clid=502&hid=106&nid=306'
                        ),
                        "showcase": {"items": EmptyList()},
                    }
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # запрашиваем одиночную модель (410) в регионе, в котором для нее нет предупреждений
        # колдунщик с данной моделью будет сформирован
        response = self.report.request_bs("place=parallel&text=Добавка%20йодированная&rids=305")
        self.assertFragmentIn(
            response,
            {
                "market_model": [
                    {
                        "title": {"__hl": {"raw": True, "text": "Добавка йодированная"}},
                        "url": LikeUrl.of(
                            '//market.yandex.ru/product--dobavka-iodirovannaia/410?clid=502&hid=106&nid=306'
                        ),
                        "showcase": {"items": [{"greenUrl": {"text": "Магазин2 с доставкой"}}]},
                    }
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # запрашиваем групповую модель в регионе, в котором для всех ее модификаций есть предупреждения (модели 408, 409)
        # колдунщик будет сформирован без офферов (выберется одна из модификаций - в нашем случае модель 408)
        response = self.report.request_bs("place=parallel&text=Соль%20антидепрессантная&rids=306")
        self.assertFragmentIn(
            response,
            {
                "market_model": [
                    {
                        "title": {"__hl": {"raw": True, "text": "Соль антидепрессантная (с шоколадом)"}},
                        "url": LikeUrl.of(
                            '//market.yandex.ru/product--sol-antidepressantnaia-s-shokoladom/408?clid=502&hid=106&nid=306'
                        ),
                        "showcase": {"items": EmptyList()},
                    }
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # запрашиваем групповую модель в регионе, в котором для выбранной модификации нет предупреждений (выбирается модель 408)
        # колдунщик будет сформирован с офферами
        response = self.report.request_bs("place=parallel&text=Соль%20антидепрессантная&rids=305")
        self.assertFragmentIn(
            response,
            {
                "market_model": [
                    {
                        "title": {"__hl": {"raw": True, "text": "Соль антидепрессантная (с шоколадом)"}},
                        "url": LikeUrl.of(
                            '//market.yandex.ru/product--sol-antidepressantnaia-s-shokoladom/408?clid=502&hid=106&nid=306'
                        ),
                        "showcase": {"items": [{"greenUrl": {"text": "Магазин2 с доставкой"}}]},
                    }
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # запрашиваем конкретную модификацию групповой модели, на которую есть непосредственные предупреждения (модель 409)
        # колдунщик cформируется без офферов
        response = self.report.request_bs("place=parallel&text=Аналог%20с%20мармеладом&rids=305")
        self.assertFragmentIn(
            response,
            {
                "market_model": [
                    {
                        "title": {"__hl": {"raw": True, "text": "Аналог соли (с мармеладом)"}},
                        "url": LikeUrl.of(
                            '//market.yandex.ru/product--analog-soli-s-marmeladom/409?clid=502&hid=106&nid=306'
                        ),
                        "showcase": {"items": EmptyList()},
                    }
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # запрашиваем ту же модель в регионе в котором на нее есть предупреждения 306
        # колдунщик сформируется без офферов
        response = self.report.request_bs("place=parallel&text=Аналог%20с%20мармеладом&rids=306")
        self.assertFragmentIn(
            response,
            {
                "market_model": [
                    {
                        "title": {"__hl": {"raw": True, "text": "Аналог соли (с мармеладом)"}},
                        "url": LikeUrl.of(
                            '//market.yandex.ru/product--analog-soli-s-marmeladom/409?clid=502&hid=106&nid=306'
                        ),
                        "showcase": {"items": EmptyList()},
                    }
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_medicine_delivery_restriction(self):

        response = self.report.request_json("place=prime&text=Просто-лекарство&rids=301&show_explicit_content=medicine")
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "titles": {"raw": "Просто-лекарство"},
                "delivery": {"isAvailable": False, "hasLocalStore": True},
            },
        )

        response = self.report.request_json(
            "place=prime&text=Лекарство-без-побочных-эффектов&rids=301&show_explicit_content=medicine-prescripted"
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "titles": {"raw": "Лекарство-без-побочных-эффектов"},
                "delivery": {"isAvailable": False, "hasLocalStore": True},
            },
        )

        response = self.report.request_json("place=prime&text=БАД&rids=302&show_explicit_content=supplements")
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "titles": {"raw": "БАД с моделью"},
                "delivery": {"isAvailable": False, "hasLocalStore": True},
            },
        )

        response = self.report.request_json("place=prime&text=Витаминка&rids=305")
        self.assertFragmentIn(response, {"entity": "offer", "delivery": {"isAvailable": False, "hasLocalStore": True}})

    def test_medicine_delivery_allowance(self):
        response = self.report.request_json("place=prime&text=Витаминка&rids=301")
        self.assertFragmentIn(
            response, {"entity": "offer", "delivery": {"isAvailable": False, "isPriorityRegion": False}}
        )

        response = self.report.request_json("place=prime&text=Витаминка&rids=302")
        self.assertFragmentIn(response, {"entity": "offer", "delivery": {"isAvailable": False, "isCountrywide": False}})

    def test_delivery_restriction(self):
        """Ограничения могут задавать условия при которых офферы не отображаются либо для офферов не доступна доставка
        Все ограничения действуют на синий и белый маркет
        """

        response = self.report.request_json("place=prime&text=deliverydisclaimer&rids=301&debug=da")
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        # В этой категории нет ограничения доставки
                        "entity": "offer",
                        "categories": [{"id": 302}],
                        "delivery": {
                            "isAvailable": True,
                        },
                    },
                    {
                        #  все ограничения действуют и на белом и на синем маркете. Поэтому доставка курьером недоступна
                        "entity": "offer",
                        "categories": [{"id": 303}],
                        "delivery": {
                            "isAvailable": False,
                        },
                    },
                ]
            },
            allow_different_len=False,
        )

        # оффер из категории 301 не отображается т.к. в ограничении задано show_offers: false
        self.assertFragmentIn(response, {'debug': {'brief': {'filters': {'RESTRICTION': 1}}}})

    @classmethod
    def prepare_delivery_allowance(cls):
        # Cиний магазин для синего офера
        cls.index.shops += [
            Shop(
                fesh=20,
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                regions=[302, 306],
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                cpa=Shop.CPA_REAL,
                pickup_buckets=[5004],
            ),
            Shop(
                fesh=21,
                regions=[302, 306],
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                cpa=Shop.CPA_REAL,
                warehouse_id=145,
                pickup_buckets=[5004],
            ),
        ]

        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=145, home_region=213),
            DynamicWarehouseToWarehouseInfo(warehouse_from=145, warehouse_to=145),
        ]

        # Аутлеты для синего магазина
        cls.index.outlets += [
            Outlet(fesh=20, region=302, point_type=Outlet.FOR_STORE, point_id=200),
            Outlet(fesh=20, region=306, point_type=Outlet.FOR_STORE, point_id=300),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5004,
                fesh=20,
                carriers=[99],
                options=[PickupOption(outlet_id=200), PickupOption(outlet_id=300)],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
        ]

        cls.index.shipment_service_calendars += [
            DeliveryCalendar(
                fesh=20,
                calendar_id=1111,
                date_switch_hour=20,
                holidays=[7, 8, 9, 10, 11, 12, 13],
                is_sorting_center=True,
            ),
            DeliveryCalendar(
                fesh=20, calendar_id=257, sc_references=[SortingCenterReference(sc_id=1111, duration=0, default=True)]
            ),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=901,
                dc_bucket_id=1,
                fesh=1,
                carriers=[257],
                regional_options=[
                    RegionalDelivery(
                        rid=302,
                        options=[
                            DeliveryOption(price=5, day_from=1, day_to=2, shop_delivery_price=10),
                            DeliveryOption(price=3, day_from=4, day_to=5, shop_delivery_price=10),
                        ],
                    ),
                    RegionalDelivery(
                        rid=306,
                        options=[
                            DeliveryOption(price=5, day_from=1, day_to=2, shop_delivery_price=10),
                            DeliveryOption(price=3, day_from=4, day_to=5, shop_delivery_price=10),
                        ],
                    ),
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
        ]

        # Синий офер
        cls.index.mskus += [
            MarketSku(
                title='Just-A-Supplement-Blue',
                hyperid=401,
                hid=106,
                sku=1001,
                blue_offers=[BlueOffer(offerid='Shop-SKU-for-Just-A-Supplement-Blue')],
                delivery_buckets=[901],
            )
        ]

        # Белый офер
        cls.index.offers += [
            Offer(title='Just-A-Supplement-White', hyperid=401, hid=106, fesh=1, delivery_buckets=[901])
        ]

    def test_delivery_allowance_blue(self):
        # Тестируем отрыв для синих оферов проверки категорийных ограничений от MBO

        # Белый офер с категорийными ограничениями доставки (show_offers=False, delivery=False):
        # нет в выдаче (тест дублируем для наглядности)
        response = self.report.request_json(
            "place=prime&text=Just-A-Supplement-White&rids=302&rearr-factors=market_metadoc_search=no"
        )
        self.assertFragmentNotIn(response, {"entity": "offer", "titles": {"raw": "Just-A-Supplement-White"}})

        # Белый офер с категорийными ограничениями доставки (show_offers=True, delivery=False):
        # есть в выдаче, доставка запрещена (тест дублируем для наглядности)
        response = self.report.request_json(
            "place=prime&text=Just-A-Supplement-White&rids=306&rearr-factors=market_metadoc_search=no"
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "titles": {"raw": "Just-A-Supplement-White"},
                "delivery": {"isAvailable": False},
            },
        )

        # Полностью аналогичный синий офер с категорийными ограничениями доставки (show_offers=False/True, delivery=False):
        # есть в выдаче на синем и на белом, доставка разрешена при наличии флага apply_delivery_restrictions_for_blue_on_white=0
        for color in ['&rgb=blue', '&rgb=green', '&rgb=green_with_blue', '']:
            for region in ['&rids=302', '&rids=306']:
                response = self.report.request_json(
                    "place=prime&text=Just-A-Supplement-Blue&offer-shipping=delivery"
                    + color
                    + region
                    + "&rearr-factors=apply_delivery_restrictions_for_blue_on_white=0;market_metadoc_search=no"
                )
                self.assertFragmentIn(
                    response,
                    {
                        "entity": "offer",
                        "titles": {"raw": "Just-A-Supplement-Blue"},
                        "delivery": {
                            "isAvailable": True,
                            "options": NotEmptyList(),
                        },
                    },
                )

        # При наличии флага apply_delivery_restrictions_for_blue_on_white=1 категорийные ограничения на доставку для синего оффера
        # на белом уже применяются
        # https://st.yandex-team.ru/MARKETOUT-31426
        for color in ['&rgb=green', '&rgb=green_with_blue', '']:
            for region in ['&rids=302', '&rids=306']:
                response = self.report.request_json(
                    "place=prime&text=Just-A-Supplement-Blue&rgb=green"
                    + region
                    + "&rearr-factors=apply_delivery_restrictions_for_blue_on_white=1;market_metadoc_search=no"
                )
                self.assertFragmentIn(
                    response,
                    {
                        "entity": "offer",
                        "titles": {"raw": "Just-A-Supplement-Blue"},
                        "delivery": {
                            "isAvailable": False,
                        },
                    },
                )

        # А для синих офферов на синем категорийные ограничения на доставку продолжают игнорироваться
        # и с флагом apply_delivery_restrictions_for_blue_on_white=1
        for region in ['&rids=302', '&rids=306']:
            response = self.report.request_json(
                "place=prime&text=Just-A-Supplement-Blue&offer-shipping=delivery&rgb=blue"
                + region
                + "&rearr-factors=apply_delivery_restrictions_for_blue_on_white=1;market_metadoc_search=no"
                + '&rearr-factors=enable_dsbs_filter_by_delivery_interval=0'
            )
            self.assertFragmentIn(
                response,
                {
                    "entity": "offer",
                    "titles": {"raw": "Just-A-Supplement-Blue"},
                    "delivery": {
                        "isAvailable": True,
                    },
                },
            )

    def test_model_warnings_on_prime_empty(self):
        # Проверяем, что у оффера без disclaimer warning не выводится
        response = self.report.request_json('place=prime' '&text=Название0' '&show_explicit_content=medicine' '')
        # Согласно MARKETOUT-8526 пустого warnings быть не должно
        self.assertFragmentIn(response, {"entity": "offer", "model": {"id": 402}, "warnings": Absent()})

    def test_model_warnings_on_prime__category_restrictions__all_regions(self):
        # Если для соответсвтующей офферу модели disclaimer не задан, но есть
        # в CategoryRestriction, то должен быть выдан последний (для всех регионов).
        response = self.report.request_json('place=prime' '&text=Название2' '&show_explicit_content=medicine')
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "model": {"id": 404},
                "warnings": {
                    "common": [{"type": "medicine6", "value": {"full": u"Витамины во всех регионах", "short": u"None"}}]
                },
            },
            allow_different_len=False,
        )

    def test_model_warnings_on_prime__category_restrictions__region(self):
        # региональные дисклеймеры имеют приоритет перед глобальным
        response = self.report.request_json(
            'place=prime' '&text=Название2' '&show_explicit_content=medicine' '&rids=305'
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "model": {"id": 404},
                "warnings": {"common": [{"type": "medicine5", "value": {"full": u"Витамины", "short": u"Витамины"}}]},
            },
            allow_different_len=False,
        )

    def test_model_warnings_on_prime__model_warning_prior(self):
        # Случай, когда для модели, соответствующей офферу, один disclaimer, а в
        # CategoryRestriction другой.
        # Показаны только предупреждения из текущей категории

        request = 'place=prime&text=Название3&show_explicit_content=medicine'

        def sample_response(warning_type):
            return {
                "entity": "offer",
                "model": {"id": 405},
                "warnings": {
                    "common": [
                        {
                            "type": warning_type,
                        }
                    ]
                },
            }

        response = self.report.request_json(request + '1')
        self.assertFragmentIn(response, sample_response("medicine6"), allow_different_len=False)

    def test_geo_show_warning_geo_place(self):
        # запрашиваем офферы на карте из нулевого региона (старое поведение фронта)
        # ожидаем: показываются все офферы, без предупреждений
        response = self.report.request_json('place=geo&rids=0&regset=2&grhow=shop&hyperid=407')
        self.assertFragmentIn(
            response,
            [
                {"entity": "offer", "shop": {"id": 2}, "warnings": NoKey("warnings")},
                {"entity": "offer", "shop": {"id": 3}, "warnings": NoKey("warnings")},
            ],
        )

        # запрашиваем офферы на карте из региона, где нет ограничений
        # ожидаем: показываются все офферы, без предупреждений
        response = self.report.request_json('place=geo&rids=302&regset=1&grhow=shop&hyperid=407')
        self.assertFragmentIn(
            response,
            [
                {"entity": "offer", "shop": {"id": 2}, "warnings": NoKey("warnings")},
                {"entity": "offer", "shop": {"id": 3}, "warnings": NoKey("warnings")},
            ],
        )

        # запрашиваем офферы на карте из региона, где нет ограничений
        # ожидаем: показываются все офферы, без предупреждений, show_explicit_content силы не имеет
        response = self.report.request_json(
            'place=geo&rids=302&regset=1&grhow=shop&hyperid=407&show_explicit_content=porn'
        )
        self.assertFragmentIn(
            response,
            [
                {"entity": "offer", "shop": {"id": 2}, "warnings": NoKey("warnings")},
                {"entity": "offer", "shop": {"id": 3}, "warnings": NoKey("warnings")},
            ],
        )

        # запрашиваем офферы на карте из региона, где есть ограничения
        # ожидаем: офферов не показывается
        response = self.report.request_json('place=geo&rids=301&regset=1&grhow=shop&hyperid=407')
        self.assertFragmentIn(response, {"search": {"total": 0}})

        # запрашиваем офферы на карте из региона, где есть ограничения с форсированным показом
        # ожидаем: показываются все офферы, с предупреждениями
        response = self.report.request_json(
            'place=geo&rids=301&regset=1&grhow=shop&hyperid=407&show_explicit_content=porn'
        )
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "shop": {"id": 2},
                    "warnings": {
                        "common": [{"type": "porn", "value": {"full": "Sluts, whores, bitches.", "short": "Babes"}}]
                    },
                },
                {
                    "entity": "offer",
                    "shop": {"id": 3},
                    "warnings": {
                        "common": [{"type": "porn", "value": {"full": "Sluts, whores, bitches.", "short": "Babes"}}]
                    },
                },
            ],
        )

    # MARKETOUT-9059
    def test_geo_show_warning_geo_place_show_explicit_content_all(self):
        """
        запрашиваем офферы на карте из региона, где есть ограничения с форсированным показом через show_explicit_content=all
        ожидаем: показываются все офферы, с предупреждениями
        """
        response = self.report.request_json(
            'place=geo&rids=301&regset=1&grhow=shop&hyperid=407&show_explicit_content=all'
        )
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "shop": {"id": 2},
                    "warnings": {
                        "common": [{"type": "porn", "value": {"full": "Sluts, whores, bitches.", "short": "Babes"}}]
                    },
                },
                {
                    "entity": "offer",
                    "shop": {"id": 3},
                    "warnings": {
                        "common": [{"type": "porn", "value": {"full": "Sluts, whores, bitches.", "short": "Babes"}}]
                    },
                },
            ],
        )

    @classmethod
    def prepare_blue_white_warnings(cls):
        cls.index.category_restrictions += [
            CategoryRestriction(
                name='blue_only',
                hids=[201],
                regional_restrictions=[
                    RegionalRestriction(
                        show_offers=False,
                        display_only_matched_offers=False,
                        delivery=False,
                        disclaimers=[
                            Disclaimer(
                                name='model_only',
                                text='Blue. Model only',
                                short_text='Blue. Model only',
                                default_warning=False,
                            ),
                            Disclaimer(
                                name='blue_default',
                                text='Blue. Default',
                                short_text='Blue. Default',
                                default_warning=True,
                            ),
                            Disclaimer(
                                name='blue_age_6',
                                age='6month',
                                text='Blue. Age6',
                                short_text='Blue. Age6',
                                default_warning=True,
                            ),  # Не смотря на default_warning=True предупреждение не будет показано по-умолчанию
                            Disclaimer(
                                age='10month', text='Blue. Age10', short_text='Blue. Age10', default_warning=True
                            ),
                        ],
                        on_blue=True,
                        on_white=False,
                    ),
                ],
            ),
            CategoryRestriction(
                name='white_only',
                hids=[201],
                regional_restrictions=[
                    RegionalRestriction(
                        show_offers=False,
                        display_only_matched_offers=False,
                        delivery=True,
                        disclaimers=[
                            Disclaimer(
                                name='model_only',
                                text='White. Model only',
                                short_text='White. Model only',
                                default_warning=False,
                            ),
                            Disclaimer(
                                name='white_default',
                                text='White. Default',
                                short_text='White. Default',
                                default_warning=True,
                            ),
                            Disclaimer(
                                name='white_age_6',
                                age='6month',
                                text='White. Age6',
                                short_text='White. Age6',
                                default_warning=True,
                            ),
                            Disclaimer(
                                age='10month', text='White. Age10', short_text='White. Age10', default_warning=True
                            ),
                        ],
                        on_blue=False,
                        on_white=True,
                    ),
                ],
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                title='Simple',
                hyperid=1201,
                hid=201,
                sku=2201,
                blue_offers=[BlueOffer(offerid='Simple_offer')],
                delivery_buckets=[901],
            ),
            MarketSku(
                title='Model warning',
                hyperid=1202,
                hid=201,
                sku=2202,
                blue_offers=[BlueOffer(offerid='Model_warning_offer')],
            ),
            MarketSku(
                title='Age6',
                hyperid=1203,
                hid=201,
                sku=2203,
                blue_offers=[BlueOffer(offerid='Age6_warning_offer')],
                age='6',
                age_unit='month',
            ),
            MarketSku(
                title='Model and age warning',
                hyperid=1204,
                hid=201,
                sku=2204,
                blue_offers=[BlueOffer(offerid='Model_and_age_warning_offer')],
                age='10',
                age_unit='month',
            ),
        ]

        cls.index.models += [
            Model(hyperid=1201, hid=201, title='Simple'),
            Model(hyperid=1202, hid=201, title='Model warning', disclaimers_model='model_only'),
            Model(hyperid=1203, hid=201, title='Age6'),
            Model(hyperid=1204, hid=201, title='Model and age warning', disclaimers_model='model_only'),
        ]

    def test_warnings_common_for_all_markets(self):
        """
        И белые и синие предупреждения показываются на маркете и при rgb=blue и при rgb=green
        """

        def disclaimer(warning_type, text):
            return {
                'type': warning_type,
                'value': {'full': text},
            }

        blue_default_disclaimer = disclaimer('blue_default', 'Blue. Default')
        blue_model_disclaimer = disclaimer('model_only', 'Blue. Model only')
        blue_age6_disclaimer = disclaimer('blue_age_6', 'Blue. Age6')
        blue_age10_disclaimer = disclaimer('None', 'Blue. Age10')

        white_default_disclaimer = disclaimer('white_default', 'White. Default')
        white_model_disclaimer = disclaimer('model_only', 'White. Model only')
        white_age6_disclaimer = disclaimer('white_age_6', 'White. Age6')
        white_age10_disclaimer = disclaimer('None', 'White. Age10')

        response = self.report.request_json('place=prime&hid=201&allow-collapsing=0&rgb=blue')
        self.assertFragmentIn(
            response,
            {
                'results': [
                    # Для простой модели отдается только дефолтный дисклеймер
                    generate_warnings_response(
                        title='Simple',
                        category=201,
                        common_warnings=[blue_default_disclaimer, white_default_disclaimer],
                    ),
                    # Если есть модельный дисклеймер, то он отдается. Дефолтное не показывается
                    generate_warnings_response(
                        title='Model warning',
                        category=201,
                        common_warnings=[blue_model_disclaimer, white_model_disclaimer],
                    ),
                    # Если задано возрастное ограничение, то оно отдается. Дефолтное не показывается
                    generate_warnings_response(
                        title='Age6', category=201, common_warnings=[blue_age6_disclaimer, white_age6_disclaimer]
                    ),
                    # Заданы возрастное и модельное ограничения. Дефолтное не показывается
                    generate_warnings_response(
                        title='Model and age warning',
                        category=201,
                        common_warnings=[
                            blue_model_disclaimer,
                            white_model_disclaimer,
                            blue_age10_disclaimer,
                            white_age10_disclaimer,
                        ],
                    ),
                ]
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_blue_white_delivery(cls):
        cls.index.category_restrictions += [
            CategoryRestriction(
                name='blue_only_delivery',
                hids=[301],
                regional_restrictions=[
                    RegionalRestriction(
                        show_offers=False,
                        display_only_matched_offers=False,
                        delivery=False,
                        on_blue=True,
                        on_white=False,
                    ),
                ],
            ),
            CategoryRestriction(
                name='white_only_delivery',
                hids=[302],
                regional_restrictions=[
                    RegionalRestriction(
                        show_offers=True,
                        display_only_matched_offers=False,
                        delivery=True,
                        on_blue=False,
                        on_white=True,
                    ),
                ],
            ),
            CategoryRestriction(
                name='white_only_without_delivery',
                hids=[303],
                regional_restrictions=[
                    RegionalRestriction(
                        show_offers=True,
                        display_only_matched_offers=False,
                        delivery=False,
                        on_blue=False,
                        on_white=True,
                    ),
                ],
            ),
        ]

        cls.index.offers += [
            Offer(title='deliverydisclaimer', hyperid=501, hid=301, fesh=1),
            Offer(title='deliverydisclaimer', hyperid=502, hid=302, fesh=1),
            Offer(title='deliverydisclaimer', hyperid=503, hid=303, fesh=1),
        ]

    @classmethod
    def prepare_multi_model_warnings(cls):
        cls.index.models += [
            Model(hyperid=701, title='Multi', hid=311, disclaimers_model=['multi1', 'multi2']),
            Model(hyperid=702, title='Multi', hid=311, warning_specification=['multi1', 'multi2']),
            Model(hyperid=703, title='Multi', hid=311, disclaimers_model=['multi1']),
            Model(hyperid=704, title='Multi', hid=311, warning_specification=['multi1']),
            Model(hyperid=705, title='Multi', hid=311, disclaimers_model=['multi2']),
            Model(hyperid=706, title='Multi', hid=311, warning_specification=['multi2']),
        ]

        cls.index.category_restrictions += [
            CategoryRestriction(
                name='multi',
                hids=[311],
                regional_restrictions=[
                    RegionalRestriction(
                        disclaimers=[
                            Disclaimer(name='multi1', text='multi1', short_text='multi1', default_warning=False),
                            Disclaimer(name='multi2', text='multi2', short_text='multi2', default_warning=False),
                        ]
                    ),
                ],
            ),
        ]

    def test_multi_model_warnings(self):
        """
        Проверяем отображение всех предупреждений, заданных в модели
        """
        response = self.report.request_json("place=prime&hid=311&allow-collapsing=1")
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        "id": 701,
                        "warnings": {
                            "common": [
                                {"type": "multi1"},
                                {"type": "multi2"},
                            ],
                            "specification": Absent(),
                        },
                    },
                    {
                        "id": 702,
                        "warnings": {
                            "common": Absent(),
                            "specification": [
                                {"type": "multi1"},
                                {"type": "multi2"},
                            ],
                        },
                    },
                    {
                        "id": 703,
                        "warnings": {
                            "common": [
                                {"type": "multi1"},
                            ],
                            "specification": Absent(),
                        },
                    },
                    {
                        "id": 704,
                        "warnings": {
                            "common": Absent(),
                            "specification": [
                                {"type": "multi1"},
                            ],
                        },
                    },
                    {
                        "id": 705,
                        "warnings": {
                            "common": [
                                {"type": "multi2"},
                            ],
                            "specification": Absent(),
                        },
                    },
                    {
                        "id": 706,
                        "warnings": {
                            "common": Absent(),
                            "specification": [
                                {"type": "multi2"},
                            ],
                        },
                    },
                ]
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_ask_18_restriction(cls):
        cls.index.hypertree += [
            HyperCategory(
                hid=110,
                name='Только для взрослых',
                children=[HyperCategory(hid=111, name='Сигары'), HyperCategory(hid=112, name='Лотерея')],
            ),
            HyperCategory(
                hid=113,
                name='Не для детей',
                children=[
                    HyperCategory(hid=114, name='Теоретическая физика'),
                ],
            ),
        ]

        cls.index.category_restrictions += [
            CategoryRestriction(
                name='ask_18', hids_with_subtree=[110, 113], regional_restrictions=[RegionalRestriction(rids=[306])]
            )
        ]

        cls.index.models += [
            Model(hyperid=1211, hid=111, title='Cigara cubana'),
            Model(hyperid=1212, hid=112, title='National lottery ticket'),
            Model(hyperid=1214, hid=114, title='Ландавшиц. Том 6'),
        ]

        cls.index.offers += [
            Offer(title='Cigar', hyperid=1211, fesh=1),
            Offer(title='Lottery ticket', hyperid=1212, fesh=1),
            Offer(title='Hydrodynamics', hyperid=1214, fesh=1),
        ]

        cls.index.mskus += [
            MarketSku(
                title='Rothmans blue',
                hyperid=1211,
                sku=1011,
                delivery_buckets=[901],
                blue_offers=[BlueOffer(offerid='rothmans-blue')],
            ),
            MarketSku(
                title='Red White Blue',
                hyperid=1212,
                sku=1012,
                delivery_buckets=[901],
                blue_offers=[BlueOffer(offerid='lottery-blue')],
            ),
            MarketSku(
                title='Blue textbook',
                hyperid=1214,
                sku=1014,
                delivery_buckets=[901],
                blue_offers=[BlueOffer(offerid='literature-blue')],
            ),
        ]

    def test_ask_18_restriction_prime(self):
        """
        Проверяем, что для предложений из категорий, помеченных как ask_18, на выдаче отображается
        restrictedAge18: true и restrictionAge18: true.
        """
        restricted_categories = (111, 112, 114)
        regions_with_restriction = (306,)
        regions_with_delivery = (302, 306)

        for hid in (111, 112, 114, 201, 106):
            for region in regions_with_delivery:
                for color in ('blue', 'green'):
                    for adult in (0, 1):
                        request = (
                            "place=prime&rearr-factors=market_hide_regional_delimiter=1;market_metadoc_search=no"
                            + "&debug=1&pp=18&allow-collapsing=0"
                            + "&hid={}&rids={}&rgb={}&adult={}".format(hid, region, color, adult)
                        )
                        response = self.report.request_json(request)
                        is_restricted = hid in restricted_categories and region in regions_with_restriction
                        is_shown = not is_restricted or adult == 1

                        self.assertFragmentIn(
                            response,
                            {
                                'search': {
                                    'total': Greater(0) if is_shown else 0,
                                    'totalOffers': Greater(0) if is_shown else 0,
                                    'totalOffersBeforeFilters': Greater(0),
                                    'restrictionAge18': is_restricted,
                                    'adult': is_restricted,
                                },
                                'debug': {
                                    'brief': {
                                        'filters': {'ADULT': Absent() if is_shown else Greater(0)},
                                        'counters': {'TOTAL_ADULT': Greater(0) if is_restricted else 0},
                                    }
                                },
                            },
                            allow_different_len=False,
                        )

                        if is_shown:
                            self.assertFragmentIn(
                                response,
                                {
                                    'results': [
                                        {
                                            'entity': 'offer',
                                            'categories': [{'id': hid}],
                                            'restrictedAge18': is_restricted,
                                            'isAdult': is_restricted,
                                        }
                                    ]
                                },
                            )

    def test_ask_18_restriction_sku_offers(self):
        """
        Проверяем флаги restrictedAge в выдаче MSKU
        """
        hid_msku = ((111, 1011), (112, 1012), (114, 1014), (106, 1001))
        restricted_categories = (111, 112, 114)
        regions_with_restriction = (306,)
        regions_with_delivery = (302, 306)

        for hid, msku in hid_msku:
            for region in regions_with_delivery:
                for adult in (0, 1):
                    request = (
                        "place=sku_offers&rgb=blue&market-sku={}&rids={}&adult={}".format(msku, region, adult)
                        + '&rearr-factors=market_use_adult_filter_for_msku=1'
                    )
                    response = self.report.request_json(request)
                    is_restricted = hid in restricted_categories and region in regions_with_restriction
                    is_shown = not is_restricted or adult == 1

                    self.assertFragmentIn(
                        response,
                        {
                            'search': {
                                'total': 1 if is_shown else 0,
                                'totalOffers': 1 if is_shown else 0,
                                'totalOffersBeforeFilters': 1,
                                'restrictionAge18': is_restricted,
                                'adult': is_restricted,
                            }
                        },
                        allow_different_len=False,
                    )

                    if is_shown:
                        self.assertFragmentIn(
                            response,
                            {
                                'results': [
                                    {
                                        'entity': 'sku',
                                        'categories': [{'id': hid}],
                                        'isAdult': is_restricted,
                                        'restrictedAge18': is_restricted,
                                        'offers': {
                                            'items': [
                                                {
                                                    'entity': 'offer',
                                                    'restrictedAge18': is_restricted,
                                                    'isAdult': is_restricted,
                                                }
                                            ]
                                        },
                                    }
                                ]
                            },
                        )

    @classmethod
    def prepare_place_images(cls):
        cls.index.category_restrictions += [
            CategoryRestriction(
                name='tobacco',
                hids_with_subtree=[401],
                regional_restrictions=[
                    RegionalRestriction(
                        rids=[Const.ROOT_COUNTRY],
                        show_offers=True,
                        disclaimers=[
                            Disclaimer(name='tobacco', text='Для курильщиков', short_text='Для кур'),
                        ],
                    )
                ],
            )
        ]

        cls.index.hypertree += [
            HyperCategory(hid=401, name='Табачные изделия', children=[HyperCategory(hid=402, name='Трубки мира')])
        ]

        cls.index.offers += [
            # отфильтруется, т.к. рестрикшн medicine в списке "плохих" рестрикшнов
            Offer(waremd5='AAAAAAAAAAAAA_AAAAAAAA', hid=101, title='Картинкооффер медицинский'),
            # не отфильтруется, т.к., хоть 102 и подкатегория 101, у 101 не указано, что все подкатегории
            # тоже подпадают под рестрикшн
            Offer(waremd5='BAAAAAAAAAAAA_AAAAAAAA', hid=102, title='Картинкооффер подмедицинский'),
            # отфильтруется, т.к. подкатегория категрии 401, у которой указано, что подкатегории подпадают
            Offer(waremd5='CAAAAAAAAAAAA_AAAAAAAA', hid=402, title='Картинкооффер курильщика'),
            # не отфильтруется, т.к. категория 108 имеет рестрикшн не из списка
            Offer(waremd5='DAAAAAAAAAAAA_AAAAAAAA', hid=108, title='Картинкооффер недетский'),
            # не отфильтруется, т.к. вообще не поматчен к категории
            Offer(waremd5='EAAAAAAAAAAAA_AAAAAAAA', title='Картинкооффер обыкновенный'),
        ]

    def test_place_images(self):
        """
        Проверяем, что на place=images не показываются товары с некоторыми рестрикшнами
        см. market/report/data/place_images_restrictions
        """

        offers = self.report.request_images(
            'place=images&'
            'offerid=AAAAAAAAAAAAA_AAAAAAAA&'
            'offerid=BAAAAAAAAAAAA_AAAAAAAA&'
            'offerid=CAAAAAAAAAAAA_AAAAAAAA&'
            'offerid=DAAAAAAAAAAAA_AAAAAAAA&'
            'offerid=EAAAAAAAAAAAA_AAAAAAAA'
        )

        expected = set(
            [
                'BAAAAAAAAAAAA_AAAAAAAA',
                'DAAAAAAAAAAAA_AAAAAAAA',
                'EAAAAAAAAAAAA_AAAAAAAA',
            ]
        )

        got = set([offer.WareMd5 for offer in offers])

        self.assertEqual(got.difference(expected), set())

    @classmethod
    def prepare_drugs(cls):
        cls.index.shops += [
            Shop(
                fesh=17,
                datafeed_id=17,
                priority_region=4381,
                regions=[
                    4381,  # Россия с ограничением
                    4382,  # Казахстан с ограничением
                    4383,  # Россия без ограничения
                ],
                name='Аптека с ПВЗ и доставкой в Казахстан и Россию',
                medicine_courier=True,
            ),
            Shop(
                fesh=18,
                datafeed_id=18,
                priority_region=4381,
                regions=[
                    4381,  # Россия с ограничением
                    4382,  # Казахстан с ограничением
                    4383,  # Россия без ограничения
                ],
                name='Аптека только с доставкой в Казахстан и Россию',
                medicine_courier=True,
            ),
            Shop(
                fesh=19,
                datafeed_id=19,
                priority_region=4381,
                regions=[
                    4381,  # Россия с ограничением
                    4382,  # Казахстан с ограничением
                    4383,  # Россия без ограничения
                ],
                name='Аптека с ПВЗ и доставкой в Казахстан и Россию без разрешения на курьерскую доставку',
            ),
            Shop(
                fesh=20,
                datafeed_id=20,
                priority_region=4381,
                regions=[
                    4381,  # Россия с ограничением
                    4382,  # Казахстан с ограничением
                    4383,  # Россия без ограничения
                ],
                name='Аптека с ПВЗ и доставкой в Казахстан и Россию (БАД)',
                medicine_courier=False,  # Для БАД лицензия не нужна, всегда доставляем
            ),
            Shop(
                fesh=21,
                datafeed_id=21,
                priority_region=4381,
                regions=[
                    4381,  # Россия с ограничением
                    4382,  # Казахстан с ограничением
                    4383,  # Россия без ограничения
                ],
                name='Аптека только с доставкой в Казахстан и Россию (БАД)',
                medicine_courier=False,  # Для БАД лицензия не нужна, всегда доставляем
            ),
        ]

        cls.index.outlets += [
            Outlet(fesh=17, region=4381, point_type=Outlet.FOR_STORE, point_id=4381),
            Outlet(fesh=17, region=4382, point_type=Outlet.FOR_STORE, point_id=4382),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=4381,
                fesh=17,
                carriers=[99],
                options=[PickupOption(outlet_id=4381)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=4382,
                fesh=17,
                carriers=[99],
                options=[PickupOption(outlet_id=4382)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=4383,
                fesh=17,
                carriers=[99],
                options=[PickupOption(outlet_id=4382)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=4380,
                carriers=[111],
                regional_options=[
                    RegionalDelivery(
                        rid=4381, options=[DeliveryOption(price=5, day_from=1, day_to=2, shop_delivery_price=10)]
                    ),
                    RegionalDelivery(
                        rid=4382, options=[DeliveryOption(price=5, day_from=1, day_to=2, shop_delivery_price=10)]
                    ),
                    RegionalDelivery(
                        rid=4383, options=[DeliveryOption(price=5, day_from=1, day_to=2, shop_delivery_price=10)]
                    ),
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        cls.index.regiontree += [
            Region(
                rid=4380, name="Казахстан", region_type=Region.COUNTRY, children=[Region(rid=4382, name="Караганда")]
            )
        ]

        cls.index.regiontree += [
            Region(rid=4381),
            Region(rid=4383),
        ]

        cls.index.category_restrictions += [
            CategoryRestriction(
                name='not_prescription',
                hids=[15758037],
                regional_restrictions=[
                    RegionalRestriction(
                        show_offers=True, display_only_matched_offers=False, delivery=False, rids=[4381, 4382]
                    ),
                ],
            )
        ]

        cls.index.gltypes += [
            GLType(param_id=BAA_PARAM_ID_IN_NOT_MEDICINE_CATEGORY, hid=BAA_CATEGORY, gltype=GLType.BOOL),
        ]

        cls.index.models += [
            Model(
                hyperid=MODEL_NOT_PRESCRIPTION_MEDICINE_TYPE,
                hid=DRUGS_CATEGORY,
            ),
            Model(
                hyperid=MODEL_NOT_PRESCRIPTION_OTHER_TYPE,
                hid=DRUGS_CATEGORY,
            ),
            Model(
                hyperid=MODEL_NOT_PRESCRIPTION_MEDICAL_BAA,
                hid=DRUGS_CATEGORY,
            ),
            Model(
                hyperid=MODEL_NOT_PRESCRIPTION_MEDICAL_BAA_WITHOUT_DELIVERY_LICENSE,
                hid=DRUGS_CATEGORY,
            ),
            Model(
                hyperid=MODEL_NOT_PRESCRIPTION_MEDICAL_PRODUCT,
                hid=DRUGS_CATEGORY,
            ),
            Model(
                hyperid=MODEL_NOT_PRESCRIPTION_WITH_NOT_ALLOWED_ALCOHOL,
                hid=DRUGS_CATEGORY,
            ),
            Model(
                hyperid=MODEL_NOT_PRESCRIPTION_WITH_NARCOTIC,
                hid=DRUGS_CATEGORY,
            ),
            Model(
                hyperid=MODEL_NOT_PRESCRIPTION_WITH_PSYCHOTROPIC,
                hid=DRUGS_CATEGORY,
            ),
            Model(
                hyperid=MODEL_NOT_PRESCRIPTION_WITH_PRECURSOR,
                hid=DRUGS_CATEGORY,
            ),
            Model(
                hyperid=MODEL_BAA_IN_NOT_MEDICINE_CATEGORY,
                hid=BAA_CATEGORY,
                glparams=[GLParam(param_id=BAA_PARAM_ID_IN_NOT_MEDICINE_CATEGORY, value=1)],
            ),
            Model(
                hyperid=MODEL_NOT_BAA_IN_NOT_MEDICINE_CATEGORY,
                hid=BAA_CATEGORY,
                glparams=[GLParam(param_id=BAA_PARAM_ID_IN_NOT_MEDICINE_CATEGORY, value=0)],
            ),
            Model(hyperid=MODEL_WITHOUT_BAA_PARAM_IN_NOT_MEDICINE_CATEGORY, hid=BAA_CATEGORY),
            Model(
                hyperid=MODEL_BAA_IN_NOT_MEDICINE_CATEGORY_BLUE,
                hid=BAA_CATEGORY,
                glparams=[GLParam(param_id=BAA_PARAM_ID_IN_NOT_MEDICINE_CATEGORY, value=1)],
            ),
            Model(
                hyperid=MODEL_NOT_PRESCRIPTION_2,
                hid=DRUGS_CATEGORY,
            ),
        ]

        for model_id in [MODEL_NOT_PRESCRIPTION_MEDICINE_TYPE]:
            cls.index.offers += [
                create_medical_offer_with_delivery(hyperid=model_id, is_medicine=True),
                create_medical_offer_with_pickup_and_delivery(hyperid=model_id, is_medicine=True),
            ]

        for model_id in [MODEL_NOT_PRESCRIPTION_OTHER_TYPE]:
            cls.index.offers += [
                create_medical_offer_with_delivery(hyperid=model_id),
                create_medical_offer_with_pickup_and_delivery(hyperid=model_id),
            ]

        for model_id in [MODEL_NOT_PRESCRIPTION_MEDICAL_BAA]:
            cls.index.offers += [
                create_medical_offer_with_delivery(hyperid=model_id, is_baa=True),
                create_medical_offer_with_pickup_and_delivery(hyperid=model_id, is_baa=True),
            ]

        for model_id in [MODEL_NOT_PRESCRIPTION_MEDICAL_PRODUCT]:
            cls.index.offers += [
                create_medical_offer_with_delivery(hyperid=model_id, is_medical_product=True),
                create_medical_offer_with_pickup_and_delivery(hyperid=model_id, is_medical_product=True),
            ]

        for model_id in [MODEL_NOT_PRESCRIPTION_WITH_NOT_ALLOWED_ALCOHOL]:
            cls.index.offers += [
                create_medical_offer_with_delivery(hyperid=model_id, is_medicine=True, is_ethanol=True),
                create_medical_offer_with_pickup_and_delivery(hyperid=model_id, is_medicine=True, is_ethanol=True),
            ]

        for model_id in [MODEL_NOT_PRESCRIPTION_WITH_NARCOTIC]:
            cls.index.offers += [
                create_medical_offer_with_delivery(hyperid=model_id, is_medicine=True, is_narcotic=True),
                create_medical_offer_with_pickup_and_delivery(hyperid=model_id, is_medicine=True, is_narcotic=True),
            ]

        for model_id in [MODEL_NOT_PRESCRIPTION_WITH_PSYCHOTROPIC]:
            cls.index.offers += [
                create_medical_offer_with_delivery(hyperid=model_id, is_medicine=True, is_psychotropic=True),
                create_medical_offer_with_pickup_and_delivery(hyperid=model_id, is_medicine=True, is_psychotropic=True),
            ]

        for model_id in [MODEL_NOT_PRESCRIPTION_WITH_PRECURSOR]:
            cls.index.offers += [
                create_medical_offer_with_delivery(hyperid=model_id, is_medicine=True, is_precursor=True),
                create_medical_offer_with_pickup_and_delivery(hyperid=model_id, is_medicine=True, is_precursor=True),
            ]

        for model_id in [MODEL_NOT_PRESCRIPTION_MEDICAL_BAA_WITHOUT_DELIVERY_LICENSE]:
            cls.index.offers += [
                create_medical_offer_with_delivery(hyperid=model_id, fesh=21, feedid=21, is_baa=True),
                create_medical_offer_with_pickup_and_delivery(hyperid=model_id, fesh=20, feedid=20, is_baa=True),
            ]

        NON_MEDICAL_BAA_MODELS = [
            MODEL_BAA_IN_NOT_MEDICINE_CATEGORY,
            MODEL_NOT_BAA_IN_NOT_MEDICINE_CATEGORY,
            MODEL_WITHOUT_BAA_PARAM_IN_NOT_MEDICINE_CATEGORY,
        ]

        for model_id in NON_MEDICAL_BAA_MODELS:
            cls.index.offers += [
                create_medical_offer_with_delivery(hyperid=model_id),
                create_medical_offer_with_pickup_and_delivery(hyperid=model_id),
            ]

        cls.index.mskus += [
            MarketSku(
                title='Офер с ПВЗ и доставкой',
                hyperid=MODEL_BAA_IN_NOT_MEDICINE_CATEGORY_BLUE,
                sku=1002,
                fesh=12,
                feedid=12,
                delivery_options=[DeliveryOption(price=20, day_from=1, day_to=2)],
                pickup_buckets=[4381, 4382],
                blue_offers=[BlueOffer(offerid='offer-with-pickup-and-delivery')],
                delivery_buckets=[4380],
            )
        ]

        cls.index.mskus += [
            MarketSku(
                title='Офер с доставкой',
                hyperid=MODEL_BAA_IN_NOT_MEDICINE_CATEGORY_BLUE,
                sku=1003,
                fesh=13,
                feedid=13,
                delivery_options=[DeliveryOption(price=20, day_from=1, day_to=2)],
                blue_offers=[BlueOffer(offerid='offer-with-delivery')],
                delivery_buckets=[4380],
            )
        ]

        # У этого оффера магазин не имеет права на доставку лекарств курьером
        cls.index.offers += [
            Offer(
                hyperid=MODEL_NOT_PRESCRIPTION_2,
                fesh=19,
                feedid=19,
                delivery_options=[DeliveryOption(price=20, day_from=1, day_to=2)],
                pickup_buckets=[4381, 4382],
                delivery_buckets=[4380],
                title="Офер с ПВЗ и доставкой, без разрешения на доставку курьером",
            )
        ]

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

    def test_drugs_without_delivery(self):
        '''
        Проверяем, что лекарственные препараты не будут иметь доставку, даже если есть флаг, снимающий
        ограничение.
        '''
        request = "place=prime&regset=2&regional-delivery=1&local-offers-first=0"
        MODELS = [
            MODEL_NOT_PRESCRIPTION_WITH_NOT_ALLOWED_ALCOHOL,
            MODEL_NOT_PRESCRIPTION_WITH_NARCOTIC,
            MODEL_NOT_PRESCRIPTION_WITH_PSYCHOTROPIC,
            MODEL_NOT_PRESCRIPTION_WITH_PRECURSOR,
        ]
        for model_id in MODELS:
            for flag in [
                "",
                "&rearr-factors=market_not_prescription_drugs_delivery=1;enable_prescription_drugs_delivery=1",
            ]:
                # С флагом и без - доставки не будет
                response = self.report.request_json(request + "&rids=4381" + "&hyperid={}".format(model_id) + flag)
                self._assert_offer_without_delivery(response, model_id)

    def test_not_prescription_drugs(self):
        '''
        Проверяем, что не рецептурные препараты будут иметь доставку, если есть флаг, снимающий ограничение.
        '''

        for model_id in [MODEL_NOT_PRESCRIPTION_MEDICINE_TYPE, MODEL_NOT_PRESCRIPTION_OTHER_TYPE]:
            request = "place=prime&regset=2&regional-delivery=1&local-offers-first=0"
            response = self.report.request_json(request + "&rids=4381" + "&hyperid={}".format(model_id))

            self._assert_offer_without_delivery(response, model_id)
            response = self.report.request_json(
                request
                + "&rids=4381"
                + "&hyperid={}".format(model_id)
                + "&rearr-factors=market_not_prescription_drugs_delivery=1"
            )

            # С флагом доставка показана
            self._assert_offer_with_delivery(response, model_id)

    def test_medical_product(self):
        '''
        Проверяем, что медицинские продукты не будут иметь доставку, если выставлен флаг
        market_enable_medical_product_delivery=0.
        '''

        model_id = MODEL_NOT_PRESCRIPTION_MEDICAL_PRODUCT

        # По умолчанию доставка медицинских продуктов разрешена
        request = "place=prime&regset=2&regional-delivery=1&local-offers-first=0"
        response = self.report.request_json(request + "&rids=4381" + "&hyperid={}".format(model_id))
        self._assert_offer_with_delivery(response, model_id)

        # Проверям, что при выключеном флаге доставка отстутсвует
        request = "place=prime&regset=2&regional-delivery=1&local-offers-first=0"
        response = self.report.request_json(
            request
            + "&rids=4381"
            + "&hyperid={}".format(model_id)
            + "&rearr-factors=market_enable_medical_product_delivery=0"
        )
        self._assert_offer_without_delivery(response, model_id)

    def test_medical_baa(self):
        '''
        Проверяем, что БАДы в медицинских категориях не будут иметь доставку, если выставлен флаг
        market_enable_medical_baa_delivery=0.
        '''

        model_id = MODEL_NOT_PRESCRIPTION_MEDICAL_BAA

        request = "place=prime&regset=2&regional-delivery=1&local-offers-first=0"
        # Проверям, что при выключеном флаге доставка отстутсвует
        response = self.report.request_json(
            request
            + "&rids=4381"
            + "&hyperid={}".format(model_id)
            + "&rearr-factors=market_enable_medical_baa_delivery=0"
        )
        self._assert_offer_without_delivery(response, model_id)
        # По умолчанию доставка медицинских БАД препаратов разрешена
        response = self.report.request_json(request + "&rids=4381" + "&hyperid={}".format(model_id))
        self._assert_offer_with_delivery(response, model_id)

    def test_medical_baa_without_delivery_license(self):
        '''
        Проверяем, что БАДы в медицинских категориях будут иметь доставку.
        '''

        model_id = MODEL_NOT_PRESCRIPTION_MEDICAL_BAA_WITHOUT_DELIVERY_LICENSE

        # Доставка медицинских БАД препаратов разрешена, не смотря на shops->medicine_courier=false
        request = "place=prime&regset=2&regional-delivery=1&local-offers-first=0"
        response = self.report.request_json(request + "&rids=4381" + "&hyperid={}".format(model_id))
        self._assert_offer_with_delivery(response, model_id)

    def test_non_medical_baa(self):
        '''
        Проверяем, что БАДы в немедицинских категориях не имеют доставку с флагом forbid_baa_delivery=1,
        но имеют доставку без флага
        '''
        request = 'place=prime&regset=2&regional-delivery=1&local-offers-first=0&rids=4381&rearr-factors=apply_delivery_restrictions_for_blue_on_white=0;market_nordstream=0;market_metadoc_search=no'

        response = self.report.request_json(request + '&hyperid={}'.format(MODEL_BAA_IN_NOT_MEDICINE_CATEGORY))
        self._assert_offer_with_delivery(response, MODEL_BAA_IN_NOT_MEDICINE_CATEGORY)

        response = self.report.request_json(
            request + '&hyperid={}'.format(MODEL_BAA_IN_NOT_MEDICINE_CATEGORY) + '&rearr-factors=forbid_baa_delivery=1'
        )
        self._assert_offer_without_delivery(response, MODEL_BAA_IN_NOT_MEDICINE_CATEGORY)

        for model_id in (MODEL_NOT_BAA_IN_NOT_MEDICINE_CATEGORY, MODEL_WITHOUT_BAA_PARAM_IN_NOT_MEDICINE_CATEGORY):
            response = self.report.request_json(
                request + '&hyperid={}'.format(model_id) + '&rearr-factors=forbid_baa_delivery=1'
            )
            self._assert_offer_with_delivery(response, model_id)

        # Проверяем, что на беру ограничения не отражаются
        response = self.report.request_json(
            request
            + '&hyperid={}'.format(MODEL_BAA_IN_NOT_MEDICINE_CATEGORY_BLUE)
            + '&rearr-factors=forbid_baa_delivery=1'
        )
        self._assert_offer_with_delivery(response, MODEL_BAA_IN_NOT_MEDICINE_CATEGORY_BLUE)

    def test_medicine_has_no_courier_without_shop_permissions(self):
        '''
        Тестируем, что если магазин не имеет права на доставку лекарств курьером (флаг medicine_courier в shops.dat),
        то мы не показываем доставку курьером
        '''
        response = self.report.request_json(
            'place=prime&regset=2&regional-delivery=1&local-offers-first=0&rids=4381&hyperid={}'
            '&rearr-factors=market_not_prescription_drugs_delivery=1'.format(MODEL_NOT_PRESCRIPTION_2)
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "titles": {"raw": "Офер с ПВЗ и доставкой, без разрешения на доставку курьером"},
                        "model": {"id": MODEL_NOT_PRESCRIPTION_2},
                        "delivery": {"isAvailable": False, "hasLocalStore": True, "options": EmptyList()},
                    }
                ]
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_restriction_merge(cls):
        cls.index.hypertree += [
            HyperCategory(
                hid=3048700,
                name='Все товары',
                children=[
                    HyperCategory(
                        hid=3048701,
                        name='Товары для здоровья',
                        children=[
                            HyperCategory(hid=3048702, name='Лекарственные растения'),
                        ],
                    ),
                    HyperCategory(hid=3048703, name='Прочие товары'),
                ],
            ),
        ]

        cls.index.category_restrictions += [
            CategoryRestriction(
                name='appearance',
                hids_with_subtree=[3048701],
                regional_restrictions=[
                    RegionalRestriction(
                        show_offers=True,
                        display_only_matched_offers=False,
                        delivery=True,
                        rids=[301],
                        rids_with_subtree=[303],
                        disclaimers=[
                            Disclaimer(
                                name='apperarance',
                                text='Внешний вид товара может отличаться от представленного на картинке',
                                short_text='Внешний вид товара может отличаться от представленного на картинке',
                            ),
                        ],
                    ),
                ],
            ),
            CategoryRestriction(
                name='drugs',
                hids_with_subtree=[3048702],
                regional_restrictions=[
                    RegionalRestriction(
                        show_offers=False,
                        display_only_matched_offers=False,
                        delivery=False,
                        disclaimers=[
                            Disclaimer(
                                name='medicine111',
                                text='Лекарство. Не доставляется. Продается только в аптеках.',
                                short_text='Лекарство. Продается в аптеках',
                            ),
                        ],
                    ),
                ],
            ),
        ]

        cls.index.offers += [
            Offer(title='Пустырник', hid=3048702, fesh=1, store=True),
            Offer(title='Бинт эластичный', hid=3048701, fesh=1, store=True),
            Offer(title='Подорожник', hid=3048703, fesh=1),
        ]

    def test_restriction_merge(self):
        for rids in [301, 302]:
            # Запрос с show_explicit_content, товар из категории
            # Лекарственные растения покажется без доставки, дисклеймеры объединятся
            response = self.report.request_json(
                'place=prime&hid=3048700&rids={}&show_explicit_content=drugs'.format(rids)
            )
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "titles": {"raw": "Бинт эластичный"},
                            "delivery": {"isAvailable": True},
                        },
                        {
                            "titles": {"raw": "Подорожник"},
                            "delivery": {"isAvailable": True},
                        },
                        {
                            "titles": {"raw": "Пустырник"},
                            "delivery": {"isAvailable": False},
                            "warnings": {
                                "common": [
                                    {
                                        "type": "apperarance",
                                        "value": {
                                            "full": "Внешний вид товара может отличаться от представленного на картинке",
                                            "short": "Внешний вид товара может отличаться от представленного на картинке",
                                        },
                                    },
                                    {
                                        "type": "medicine111",
                                        "value": {
                                            "full": "Лекарство. Не доставляется. Продается только в аптеках.",
                                            "short": "Лекарство. Продается в аптеках",
                                        },
                                    },
                                ]
                            },
                        },
                    ]
                },
                allow_different_len=False,
                preserve_order=False,
            )

            # Запрос без show_explicit_content, товар из категории
            # Лекарственные растения не покажется
            response = self.report.request_json('place=prime&hid=3048700&rids={}'.format(rids))
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "titles": {"raw": "Бинт эластичный"},
                            "delivery": {"isAvailable": True},
                        },
                        {
                            "titles": {"raw": "Подорожник"},
                            "delivery": {"isAvailable": True},
                        },
                    ]
                },
                allow_different_len=False,
                preserve_order=False,
            )

    @classmethod
    def prepare_multiple_restriction_filtering(cls):
        cls.index.hypertree += [
            HyperCategory(
                hid=3407000,
                name='Все товары',
                children=[
                    HyperCategory(
                        hid=3407001,
                        name='Товары для здоровья',
                        children=[
                            HyperCategory(hid=3407002, name='Лекарственные растения'),
                        ],
                    ),
                    HyperCategory(hid=3407003, name='Прочие товары'),
                ],
            ),
        ]

        cls.index.category_restrictions += [
            CategoryRestriction(
                name='multi-drugs',
                hids_with_subtree=[3407002],
                regional_restrictions=[
                    RegionalRestriction(
                        show_offers=True,
                        display_only_matched_offers=False,
                        delivery=False,
                        disclaimers=[
                            Disclaimer(
                                name='multi-medicine111',
                                text='Лекарство. Не доставляется. Продается только в аптеках.',
                                short_text='Лекарство. Продается в аптеках',
                            ),
                        ],
                    ),
                ],
            ),
            CategoryRestriction(
                name='multi-appearance',
                hids_with_subtree=[3407001],
                regional_restrictions=[
                    RegionalRestriction(
                        show_offers=True,
                        display_only_matched_offers=False,
                        delivery=True,
                        disclaimers=[
                            Disclaimer(
                                name='multi-apperarance',
                                text='Внешний вид товара может отличаться от представленного на картинке',
                                short_text='Внешний вид товара может отличаться от представленного на картинке',
                            ),
                        ],
                    ),
                ],
            ),
        ]

        cls.index.models += [
            Model(hyperid=3407020, title='Пустырник', hid=3407002),
            Model(hyperid=3407010, title='Бинт эластичный', hid=3407001),
        ]

    def test_multiple_restriction_filtering(self):
        """Проверяем, что при нескольких одновременно действующих ограничениях
        filter-warnings будет правильно работать на modelinfo
        """
        for hyperid in [3407020, 3407010]:
            response = self.report.request_json(
                'place=modelinfo&hyperid={}&rids=301&filter-warnings=multi-apperarance'.format(hyperid)
            )
            self.assertFragmentIn(response, {"results": EmptyList()}, allow_different_len=False)


if __name__ == '__main__':
    main()
