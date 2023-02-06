#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BlueOffer,
    Currency,
    HyperCategory,
    HyperCategoryType,
    MarketSku,
    Model,
    Shop,
    Tax,
    Vat,
    YamarecCategoryBeruBonusPartition,
    YamarecCategoryPartition,
    YamarecPlace,
    YamarecSettingPartition,
)
from core.testcase import TestCase, main
from core.bigb import BeruModelOrderLastTimeCounter, ModelLastOrderEvent


class T(TestCase):
    """
    Набор тестов для плейса для выдачи беру-бонусов
    """

    @classmethod
    def prepare(cls):

        # commonly_purchased categories
        personal_category_filter = [
            90606,
            7683677,
            7683675,
            13462769,
            91330,
            13337703,
            15720046,
            15720050,
            91331,
            15720051,
            15720045,
            91329,
            15720037,
            15720039,
            16147683,
            91382,
            15714713,
            15714708,
            15714698,
            91388,
            14698852,
            15557928,
            15714682,
            15714680,
            15714675,
            15714671,
            15720042,
            91419,
            91420,
            14621180,
            91422,
            91430,
            91397,
            15726404,
            15726402,
            91352,
            15726412,
            15726410,
            15726408,
            15719803,
            15719820,
            15719828,
            15719799,
            91427,
            91344,
            15714127,
            15714122,
            91342,
            15714129,
            91340,
            91343,
            14706137,
            15714542,
            15714113,
            15714106,
            15727944,
            15728039,
            91345,
            15727473,
            15727886,
            15727888,
            15727896,
            15727878,
            15727884,
            15727954,
            91339,
            15727967,
            91421,
            91408,
            15697700,
            13041400,
            15934091,
            16088924,
            15770939,
            4922657,
            15770934,
            13518990,
            14245094,
            12718081,
            15959385,
            15963644,
            13212408,
            15685457,
            15685787,
            13212400,
            12718223,
            15999360,
            15963668,
            15999143,
            12718332,
            12714755,
            12718255,
            12766642,
            12704208,
            15971367,
            12714763,
            12704139,
            15962102,
            818945,
            8480736,
            13277104,
            13277088,
            13277108,
            13277089,
            13276918,
            13276920,
            14995813,
            14995788,
            4748066,
            4748064,
            4748062,
            8480752,
            8510396,
            14996541,
            4748057,
            14996686,
            8480754,
            13276669,
            4748072,
            4748074,
            13276667,
            14996659,
            4748078,
            14994593,
            14990285,
            8480738,
            13244155,
            13239550,
            13240862,
            13239503,
            13239527,
            4854062,
            14993426,
            13239477,
            13239479,
            14993540,
            91184,
            14993483,
            15011042,
            8476101,
            8476102,
            8476110,
            8476103,
            8476097,
            8476098,
            8476539,
            8476100,
            8476099,
            13239041,
            13238924,
            14994948,
            8478954,
            14989778,
            4748058,
            13239135,
            14990252,
            13238994,
            13239089,
            15350596,
            6470214,
            8475961,
            13357269,
            13314796,
            15019493,
            91179,
            91180,
            13314795,
            13314823,
            14993676,
            13314841,
            8475955,
            14994526,
            14989707,
            4852774,
            4852773,
            13314855,
            14994695,
        ]

        universal_categories = [
            278374,
            13491643,
            91078,
            91335,
            91327,
            91423,
            15368134,
            16044621,
            16044387,
            16044466,
            16044416,
            15714731,
            91392,
            15726400,
            91332,
            818944,
            91346,
            15714135,
            15714102,
            16011677,
            16011796,
            16011704,
            15714105,
            982439,
            15720388,
            16099944,
            15697667,
            15697685,
            15697659,
            15697691,
            90689,
            13041431,
            13196790,
            13041429,
            15696738,
            13041430,
            90691,
            90688,
            13041456,
            90690,
            13041460,
            13041507,
            13041512,
            13041511,
            13041314,
            13041252,
            13277094,
            8480725,
            8480722,
            8480713,
            15927546,
            13243353,
            13239358,
            91183,
            91167,
            91176,
            16042844,
            14989652,
            91173,
            7693914,
            14995755,
            13334231,
            13314877,
            91174,
            15002303,
        ]

        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.COMMONLY_PURCHASED_UNIVERSAL_CATEGORIES,
                kind=YamarecPlace.Type.CATEGORY,
                partitions=[
                    YamarecCategoryPartition(category_list=universal_categories, splits=['*']),
                ],
            ),
            YamarecPlace(
                name=YamarecPlace.Name.COMMONLY_PURCHASED_PERSONAL_CATEGORIES,
                kind=YamarecPlace.Type.CATEGORY,
                partitions=[
                    YamarecCategoryPartition(category_list=personal_category_filter, splits=['*']),
                ],
            ),
        ]
        # Beru Bonus Categories
        # [hid, category name, FixedNominal, MinOrderPrice, PercentNominal, MaxOrderPrice]
        beru_bonus_categories = [
            [13041512, 'Товары для мебели, ковров и напольных покрытий', '', '', '15%', 100000],
            [13334231, 'Зубную пасту', '', '', '15%', 20000],
            [90688, 'Стиральный порошок', '', '', '15%', 100000],
            [13357269, 'Женские бритвы и лезвия', '', '', '15%', 20000],
            [13041429, 'Гели и жидкости для стирки', '', '', '15%', 100000],
            [13475285, 'Кремы и присыпки', '', '', '15%', 10000],
            [9283442, '3D-принтеры', 100, 5000, '', ''],
            [90548, 'Акустические системы', 150, 5000, '', ''],
        ]
        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.BERU_BONUS_CATEGORIES,
                kind=YamarecPlace.Type.CATEGORY_BERU_BONUS,
                partitions=[YamarecCategoryBeruBonusPartition(bonus_category_list=beru_bonus_categories, splits=['*'])],
            )
        ]

        hids = [
            {'hid': 13334231, 'name': 'Зубная паста'},  # seq=0
            {'hid': 9283442, 'name': '3D-принтеры'},  # seq=1
            {'hid': 90548, 'name': 'Акустические системы'},  # seq=2
            {'hid': 90688, 'name': 'Стиральный порошок'},  # seq=3
            {'hid': 12807782, 'name': 'Чехлы для одежды'},  # seq=4
            {'hid': 11911273, 'name': 'Наматрасники и чехлы для матрасов'},  # seq=5
            {'hid': 237418, 'name': 'Варочные панели'},  # seq=6
            {'hid': 13041429, 'name': 'Гели и жидкости для стирки'},  # seq=7
            {'hid': 13475285, 'name': 'Кремы и присыпки'},  # seq=8
            {'hid': 13041512, 'name': 'Для мебели, ковров и напольных покрытий'},  # seq=9
            {'hid': 2473020, 'name': 'HID_020'},  # seq=10
            {'hid': 2473030, 'name': 'HID_030'},  # seq=11
            {'hid': 2473040, 'name': 'HID_040'},  # seq=12
            {'hid': 2473050, 'name': 'HID_050'},  # seq=13
            {'hid': 13357269, 'name': 'Бритвы и лезвия'},  # seq=14
        ]

        for seq, data in enumerate(hids):
            cls.index.hypertree += [
                HyperCategory(hid=data['hid'], name=data['name'], output_type=HyperCategoryType.GURU),
            ]

            cls.index.models += [
                Model(hyperid=2473001 + seq, hid=data['hid']),
            ]

            cls.index.mskus += [
                MarketSku(
                    hyperid=2473001 + seq,
                    sku=247300001 + seq,
                    hid=data['hid'],
                    blue_offers=[BlueOffer(price=100 + seq, hyperid=2473001 + seq, hid=data['hid'], vat=Vat.VAT_10)],
                ),
            ]

        cls.recommender.on_request_models_of_interest(user_id='yandexuid:001').respond(
            {
                'models': [
                    '2473012',
                    '2473013',
                    '2473014',
                    '2473016',
                    '2473003',
                    '2473005',
                    '2473008',
                    '2473009',
                    '2473015',
                ]
            }
        )

        cls.index.shops += [
            Shop(
                fesh=1,
                datafeed_id=1,
                priority_region=213,
                name='blue_shop_1',
                currency=Currency.RUR,
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                tax_system=Tax.OSN,
                cpa=Shop.CPA_REAL,
            ),
        ]

        cls.recommender.on_request_accessory_models(model_id=2473012, item_count=20, version='1').respond(
            {'models': ['2473001', '2473002', '2473003', '2473005', '2473016']}
        )

        cls.recommender.on_request_accessory_models(model_id=2473013, item_count=20, version='1').respond(
            {'models': ['2473010', '2473006']}
        )

        cls.recommender.on_request_accessory_models(model_id=2473014, item_count=20, version='1').respond(
            {'models': ['247308']}
        )

        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.EXTERNAL_PRODUCT_ACCESSORY_BLUE_MARKET,
                kind=YamarecPlace.Type.SETTING,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    YamarecSettingPartition(
                        params={
                            'version': '1',
                            'use-external': '1',
                            'use-local': '0',
                        },
                        splits=[{}],
                    ),
                ],
            ),
            YamarecPlace(
                name=YamarecPlace.Name.EXTERNAL_PRODUCT_ACCESSORY,
                kind=YamarecPlace.Type.SETTING,
                split_rule=YamarecPlace.SplitRule.ABT,
                partitions=[
                    YamarecSettingPartition(
                        params={
                            'version': '1',
                            'use-external': '1',
                            'use-local': '0',
                        },
                        splits=[{}],
                    ),
                ],
            ),
        ]

        counters = [
            BeruModelOrderLastTimeCounter(
                model_order_events=[
                    ModelLastOrderEvent(model_id=2473016, timestamp=478418100),
                    ModelLastOrderEvent(model_id=2473001, timestamp=478418100),
                    ModelLastOrderEvent(model_id=2473004, timestamp=478418100),
                ]
            ),
        ]

        cls.bigb.on_request(yandexuid='001', client='merch-machine').respond(counters=counters)

    def test_beru_bonuses_combined(self):
        """Проверяем плейс для беру бонусов в полной конфигурации,
        категории с 13041512 до 12807782 приходят из аксов к текущей
        корзине, категория 90688 из прошлых заказов и оставшиеся - из
        частотки/персональных
        """
        for suffix in ['cpa=real', 'rgb=blue']:
            response = self.report.request_json(
                "place=beru_bonuses&yandexuid=001&{}&cart-sku=247300012,247300013,247300014".format(suffix)
            )
            self.assertFragmentIn(
                response,
                {
                    "numberToCreate": 1,
                    "items": [
                        {
                            "entity": "bonusCategory",
                            "categories": [{"hid": 13041512}],
                            "placeholders": {"forTitle": "Товары для мебели, ковров и напольных покрытий"},
                            "type": "PERCENT",
                            "nominal": "15",
                            "maxOrderTotal": "100000",
                            "daysToExpire": "30",
                            "recommendationSource": "ACCESSORIES",
                        },
                        {
                            "entity": "bonusCategory",
                            "categories": [{"hid": 13334231}],
                            "placeholders": {"forTitle": "Зубную пасту"},
                            "type": "PERCENT",
                            "nominal": "15",
                            "maxOrderTotal": "20000",
                            "daysToExpire": "30",
                            "recommendationSource": "ACCESSORIES",
                        },
                        {
                            "entity": "bonusCategory",
                            "categories": [{"hid": 9283442}],
                            "placeholders": {"forTitle": "3D-принтеры"},
                            "type": "FIXED",
                            "nominal": "100",
                            "minOrderTotal": "5000",
                            "daysToExpire": "30",
                            "recommendationSource": "ACCESSORIES",
                        },
                        {
                            "entity": "bonusCategory",
                            "categories": [{"hid": 90548}],
                            "placeholders": {"forTitle": "Акустические системы"},
                            "type": "FIXED",
                            "nominal": "150",
                            "minOrderTotal": "5000",
                            "daysToExpire": "30",
                            "recommendationSource": "ACCESSORIES",
                        },
                        {
                            "entity": "bonusCategory",
                            "categories": [{"hid": 90688}],
                            "placeholders": {"forTitle": "Стиральный порошок"},
                            "type": "PERCENT",
                            "nominal": "15",
                            "maxOrderTotal": "100000",
                            "daysToExpire": "30",
                            "recommendationSource": "ORDERS",
                        },
                        {
                            "entity": "bonusCategory",
                            "categories": [{"hid": 13357269}],
                            "placeholders": {"forTitle": "Женские бритвы и лезвия"},
                            "type": "PERCENT",
                            "nominal": "15",
                            "maxOrderTotal": "20000",
                            "daysToExpire": "30",
                            "recommendationSource": "COMMONLY_PURCHASED",
                        },
                        {
                            "entity": "bonusCategory",
                            "categories": [{"hid": 13041429}],
                            "placeholders": {"forTitle": "Гели и жидкости для стирки"},
                            "type": "PERCENT",
                            "nominal": "15",
                            "maxOrderTotal": "100000",
                            "recommendationSource": "COMMONLY_PURCHASED",
                        },
                        {
                            "entity": "bonusCategory",
                            "categories": [{"hid": 13475285}],
                            "placeholders": {"forTitle": "Кремы и присыпки"},
                            "type": "PERCENT",
                            "nominal": "15",
                            "maxOrderTotal": "10000",
                            "daysToExpire": "30",
                            "recommendationSource": "PERSONAL_CATEGORIES",
                        },
                    ],
                },
                preserve_order=True,
                allow_different_len=False,
            )


if __name__ == '__main__':
    main()
