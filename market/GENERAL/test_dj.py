#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BlueOffer,
    Currency,
    GLType,
    HyperCategory,
    HyperCategoryType,
    MarketSku,
    Model,
    NavCategory,
    Offer,
    Opinion,
    Shop,
    Tax,
    Vat,
    Vendor,
    WarehouseWithPriority,
    WarehousesPriorityInRegion,
    YamarecDjDefaultModelsList,
    YamarecPlace,
)
from core.testcase import TestCase, main

from core.bigb import WeightedValue, BigBKeyword, ModelLastSeenEvent, BeruPersHistoryModelViewLastTimeCounter
from market.proto.recom.exported_dj_user_profile_pb2 import (
    TEcomVersionedDjUserProfile,
    TVersionedProfileData,
    TFashionDataV1,
    TFashionSizeDataV1,
)

from core.dj import DjModel
from core.matcher import NotEmptyList, NotEmpty, Contains, Absent, Round

import time

NOW = int(time.time())
DAY = 86400

DEFAULT_PROFILE = [
    BigBKeyword(
        id=BigBKeyword.GENDER,
        weighted_uint_values=[
            WeightedValue(value=BigBKeyword.GENDER_MALE, weight=621947),
            WeightedValue(value=BigBKeyword.GENDER_FEMALE, weight=375515),
        ],
    ),
]

MODELS = list(range(2497601, 2497631))
BAT_MODELS = MODELS[26:]
BOF_MODELS = MODELS[:25]
FAST_MOVING_MODELS = list(range(2490001, 2490011))
MODEL_PLACES_DEFAULT_COUNT = 30
DISABLED_EXPERIMENT_MODELS = list(range(2100, 2120))

DJ_DEFAULT_MODELS = [
    [
        144750053,
        "Смартфон Samsung Galaxy Note 9 128GB",
        "//avatars.mds.yandex.net/get-mpic/1360806/img_id3181675817774459974.jpeg/orig",
    ],
    [
        62467220,
        "Смартфон Xiaomi Mi8 6/128GB",
        "//avatars.mds.yandex.net/get-mpic/1364191/img_id2810265824028071602.png/orig",
    ],
    [
        1732171388,
        "Смартфон Apple iPhone 8 64GB",
        "//avatars.mds.yandex.net/get-mpic/397397/img_id4161693246034904662.jpeg/orig",
    ],
    [
        1901396991,
        "Смартфон Honor 9 Lite 32GB",
        "//avatars.mds.yandex.net/get-mpic/200316/img_id5631679082522642555.jpeg/orig",
    ],
    [
        12830350,
        "Электрический котел Электромаш ЭВПМ-3",
        "//avatars.mds.yandex.net/get-mpic/199079/img_id531921995162419366/orig",
    ],
    [
        13178915,
        "Автомобильная шина Белшина Artmotion Snow",
        "//avatars.mds.yandex.net/get-mpic/96484/img_id8578371652071171360/orig",
    ],
    [
        1725712742,
        "TV-тюнер Cadena CDT-1711SB",
        "//avatars.mds.yandex.net/get-mpic/372220/img_id2890448670849384833.jpeg/orig",
    ],
    [
        151671410,
        "Телевизор Irbis 19S30HA101B",
        "//avatars.mds.yandex.net/get-mpic/1363071/img_id8762330111278873186.jpeg/orig",
    ],
    [
        13177721,
        "Колесный диск ТЗСК Renault Logan",
        "//avatars.mds.yandex.net/get-mpic/175985/img_id6945581164058131778/orig",
    ],
    [
        121980073,
        "Бластер Nerf Зомби Страйк Выживший (E1754)",
        "//avatars.mds.yandex.net/get-mpic/1360852/img_id886623089471276771.jpeg/orig",
    ],
    [
        1043208,
        "Игровая приставка Sony PlayStation 2 Slim",
        "//avatars.mds.yandex.net/get-mpic/175985/img_id2470929088454859365/orig",
    ],
    [
        151671076,
        "Электросамокат KUGOO S2",
        "//avatars.mds.yandex.net/get-mpic/1360852/img_id9146755584087794209.jpeg/orig",
    ],
    [11042631, "Ноутбук AORUS X3", "//avatars.mds.yandex.net/get-mpic/199079/img_id2101095028335038664/orig"],
    [
        1970873251,
        "Кукла-сюрприз MGA Entertainment в шаре LOL Surprise 2 Wave 2, 8 см, в ассортименте",
        "//avatars.mds.yandex.net/get-mpic/199079/img_id5179297560182875956.jpeg/orig",
    ],
    [
        14145524,
        "Палатка СТЭК Куб 3 трехслойная",
        "//avatars.mds.yandex.net/get-mpic/195452/img_id1266853897392419604/orig",
    ],
    [
        1719237819,
        "Виниловый проигрыватель Ion Compact LP",
        "//avatars.mds.yandex.net/get-mpic/96484/img_id6636794736711265103/orig",
    ],
]

COMMONLY_PURCHASED_DJ_DEFAULT_MODELS = [
    [
        1729318722,
        "Synergetic Гель для мытья посуды Алоэ",
        "//avatars.mds.yandex.net/get-mpic/1522540/img_id2700073896132249113.jpeg/orig",
    ],
    [
        41266053,
        "Туалетная бумага Papia белая трёхслойная",
        "//avatars.mds.yandex.net/get-mpic/1886039/img_id8842911826383630132.png/orig",
    ],
    [
        1730135105,
        "Гель для стирки Synergetic универсальный",
        "//avatars.mds.yandex.net/get-mpic/1620389/img_id5441095053932834967.jpeg/orig",
    ],
    [
        1721706013,
        "Стиральный порошок Ушастый Нянь Для стирки детского белья",
        "//avatars.mds.yandex.net/get-mpic/1526692/img_id3573005876247590742.jpeg/orig",
    ],
    [
        1730157230,
        "Clean & Fresh All in 1 таблетки  для посудомоечной машины",
        "//avatars.mds.yandex.net/get-mpic/2017118/img_id6424276124553303416.jpeg/orig",
    ],
    [
        1973844524,
        " Кофе в капсулах Nescafe Dolce Gusto Americano (16 капс.)",
        "//avatars.mds.yandex.net/get-mpic/1361544/img_id5798893311159645272.jpeg/orig",
    ],
    [
        1973839864,
        " Кофе в капсулах Nescafe Dolce Gusto Latte Macchiato (16 капс.)",
        "//avatars.mds.yandex.net/get-mpic/1525999/img_id23377037837326497.jpeg/orig",
    ],
    [
        1729924181,
        "Сменные кассеты Gillette Mach3",
        "//avatars.mds.yandex.net/get-mpic/1042102/img_id8164107944173551806.jpeg/orig",
    ],
    [
        1851358794,
        "Petitfee Гидрогелевые патчи с экстрактом чёрного жемчуга и био-частицами золота Black Pearl & Gold Hydrogel Eye Patch",
        "//avatars.mds.yandex.net/get-mpic/906397/img_id2421871206495136529.jpeg/orig",
    ],
    [
        168903162,
        "Кофе в зернах Lavazza Qualita Oro",
        "//avatars.mds.yandex.net/get-mpic/1545401/img_id6696388115078831976.jpeg/orig",
    ],
    [
        1730197736,
        "BioMio Bio-total таблетки  для посудомоечной машины",
        "//avatars.mds.yandex.net/get-mpic/2008488/img_id2404151118208663556.jpeg/orig",
    ],
    [
        188526011,
        "Капсулы Tide 3 in 1 Pods Color",
        "//avatars.mds.yandex.net/get-mpic/1525355/img_id1761093963488744594.jpeg/orig",
    ],
    [
        152407384,
        "BioAqua Очищающая пузырьковая маска",
        "//avatars.mds.yandex.net/get-mpic/1101307/img_id831315241119370041.jpeg/orig",
    ],
    [
        507382078,
        " Кофе в капсулах Nescafe Dolce Gusto Lungo (48 капс.)",
        "//avatars.mds.yandex.net/get-mpic/1733932/img_id3341715581975581207.jpeg/orig",
    ],
    [
        1851549322,
        "Гель для тела Holika Holika Aloe 99% Soothing Gel Универсальный несмываемый гель для лица и тела",
        "//avatars.mds.yandex.net/get-mpic/933699/img_id6937790009537531756.jpeg/orig",
    ],
    [
        1729924191,
        "Сменные кассеты Gillette Fusion5",
        "//avatars.mds.yandex.net/get-mpic/397397/img_id8257514104117760871.jpeg/orig",
    ],
    [
        1973850257,
        " Горячий шоколад в капсулах Nescafe Dolce Gusto Chococino (16 капс.)",
        "//avatars.mds.yandex.net/get-mpic/1926869/img_id4445896779460470927.jpeg/orig",
    ],
    [
        507275143,
        " Кофе в капсулах Nescafe Dolce Gusto Latte Macchiato Caramel (48 капс.)",
        "//avatars.mds.yandex.net/get-mpic/1568604/img_id5670584790013354808.jpeg/orig",
    ],
    [
        1969729682,
        "Synergetic Антибактериальный гель для мытья посуды Сочный апельсин",
        "//avatars.mds.yandex.net/get-mpic/1620389/img_id792864653168434396.jpeg/orig",
    ],
]

DEFAULT_MODELS = [hid for hid, name, picture_link in DJ_DEFAULT_MODELS]


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.set_default_reqid = False
        cls.settings.rgb_blue_is_cpa = True

        cls.bigb.on_default_request().respond(counters=[])

    @classmethod
    def prepare_blue_attr_models_valid_without_oom_and_bigb(cls):
        cls.bigb.on_request(yandexuid='123', client='merch-machine').return_code(500)

        cls.dj.on_request(yandexuid='123').respond(
            [DjModel(id='123', title='Берулька', url='/product/123', pic_url='http://pic')]
        )

    def test_blue_attr_models_valid_without_oom_and_bigb(self):
        """
        Проверяем, что без ответов от OMM и BigB плeйс blue_attractive_models
        не падает с флагом market_dj_exp_for_blue_attractive_models.
        Под этим флагом используется Dj
        """
        self.report.request_json(
            'place=blue_attractive_models&rgb=blue&yandexuid=123&numdoc=5'
            '&rearr-factors=market_dj_exp_for_blue_attractive_models='
        )

    @classmethod
    def prepare_blue_model_places(cls):
        cls.index.models += [Model(hyperid=hyperid) for hyperid in MODELS]

        cls.index.mskus += [
            MarketSku(
                hyperid=hyperid,
                sku=hyperid * 10 + 1,
                blue_offers=[
                    BlueOffer(price=500, vat=Vat.NO_VAT, feedid=12345),
                ],
            )
            for hyperid in MODELS
        ]

        blue_attractive_models = [DjModel(id=str(hyperid), title='model#%d' % hyperid) for hyperid in BAT_MODELS]
        blue_findings_models = [DjModel(id=str(hyperid), title='model#%d' % hyperid) for hyperid in BOF_MODELS]

        cls.dj.on_request(yandexuid='2497601').respond(blue_attractive_models)
        cls.dj.on_request(yandexuid='2497601', exp='blue_attractive_models_1').respond(blue_attractive_models)
        cls.dj.on_request(yandexuid='2497601', exp='blue_attractive_models').respond(blue_attractive_models)
        cls.dj.on_request(yandexuid='2497602').respond(blue_findings_models)
        cls.dj.on_request(yandexuid='2497602', exp='blue_omm_findings_1').respond(blue_findings_models)
        cls.dj.on_request(yandexuid='2497602', exp='blue_omm_findings').respond(blue_findings_models)

    def test_blue_model_places(self):
        """
        Проверяем, что плейсы blue_attractive_models и blue_omm_findings отдаёт
        ожидаемую выдачу с поплейсовыми флагами
        Референсный тест: test_omm.py::test_blue_omm_experiment
        """

        for add_cgi in [
            'rearr-factors=market_dj_exp_for_blue_attractive_models=blue_attractive_models_1&place=blue_attractive_models',
            'rearr-factors=market_dj_exp_for_blue_attractive_models=&place=blue_attractive_models',
            'place=dj&dj-place=blue_attractive_models_1&dj-output-mode=loop&dj-stats-source-policy=default_offers',
            'place=blue_attractive_models',
        ]:

            response = self.report.request_json('rgb=blue&yandexuid=2497601&' + add_cgi)

            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            'entity': 'product',
                            'id': hyperid,
                            'prices': {
                                'min': NotEmpty(),
                                'max': NotEmpty(),
                            },
                            'offers': {
                                'count': 1,
                                'items': NotEmptyList(),
                            },
                        }
                        for hyperid in MODELS[26:]
                    ]
                },
                preserve_order=True,
            )

            dj_len = len(BAT_MODELS)
            expected_len = dj_len * ((MODEL_PLACES_DEFAULT_COUNT + dj_len - 1) / dj_len)
            self.assertFragmentIn(response, {"search": {"total": expected_len}})

        for add_cgi in (
            'rearr-factors=market_dj_exp_for_blue_omm_findings=blue_omm_findings_1&place=blue_omm_findings',
            'rearr-factors=market_dj_exp_for_blue_omm_findings=&place=blue_omm_findings',
            'place=dj&dj-place=blue_omm_findings_1&dj-output-mode=loop&dj-stats-source-policy=default_offers&dj-use-default-models=1',
            'place=blue_omm_findings',
        ):

            response = self.report.request_json('&rgb=blue&yandexuid=2497602&' + add_cgi)

            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            'entity': 'product',
                            'id': hyperid,
                            'prices': {
                                'min': NotEmpty(),
                                'max': NotEmpty(),
                            },
                            'offers': {
                                'count': 1,
                                'items': NotEmptyList(),
                            },
                        }
                        for hyperid in MODELS[:25]
                    ]
                },
                preserve_order=True,
            )

            dj_len = len(BOF_MODELS)
            expected_len = dj_len * ((MODEL_PLACES_DEFAULT_COUNT + dj_len - 1) / dj_len)
            self.assertFragmentIn(response, {"search": {"total": expected_len}})

    @classmethod
    def prepare_dj_commonly_purchased(cls):
        hid = 90606

        cls.index.hypertree += [HyperCategory(hid, output_type=HyperCategoryType.GURU)]

        cls.index.models += [Model(hyperid=hyperid, hid=hid) for hyperid in FAST_MOVING_MODELS]

        cls.index.mskus += [
            MarketSku(
                hyperid=hyperid,
                sku=hyperid * 100 + 1,
                blue_offers=[
                    BlueOffer(price=500, vat=Vat.NO_VAT, feedid=12345),
                ],
            )
            for hyperid in FAST_MOVING_MODELS
        ]

        recommended_models = [DjModel(id=str(hyperid), title='model#%d' % hyperid) for hyperid in FAST_MOVING_MODELS]

        cls.dj.on_request(exp='commonly_purchased_1', yandexuid='12345').respond(recommended_models)

    def test_dj_commonly_purchased(self):
        """
        https://st.yandex-team.ru/MARKETRECOM-1880
        Проверяем, что при использовании dj плейс commonly_purchased
        отдаёт рекомендованные dj модели в таком же формате, как и
        без использования dj
        """
        # Если параметр numdoc передан, отдаём не более numdoc моделей
        response = self.report.request_json(
            'place=commonly_purchased&yandexuid=12345&debug=da'
            '&rgb=BLUE&numdoc=14&rearr-factors='
            'market_dj_exp_for_commonly_purchased=commonly_purchased_1'
            ';turn_on_commonly_purchased=1'
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": len(FAST_MOVING_MODELS),
                    "results": [{"categories": [{"id": 90606}], "id": hyperid} for hyperid in FAST_MOVING_MODELS],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # Если параметр numdoc не задан, отдаются все найденные модели
        response = self.report.request_json(
            'place=commonly_purchased&yandexuid=12345&debug=da'
            '&rgb=BLUE&rearr-factors='
            'market_dj_exp_for_commonly_purchased=commonly_purchased_1'
            ';turn_on_commonly_purchased=1'
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": len(FAST_MOVING_MODELS),
                    "results": [{"categories": [{"id": 90606}], "id": hyperid} for hyperid in FAST_MOVING_MODELS],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json(
            'place=dj&yandexuid=12345&debug=da'
            '&rgb=BLUE&place-dj=commonly_purchased_1&dj-place=commonly_purchased_1'
            '&dj-default-item-count=16&dj-stats-source-policy=default_offers&dj-yamarec-place=default-dj-commonly-purchased-models'
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": len(FAST_MOVING_MODELS),
                    "results": [{"categories": [{"id": 90606}], "id": hyperid} for hyperid in FAST_MOVING_MODELS],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_dj_blue_deals(cls):
        HID = 54321
        cls.index.hypertree += [HyperCategory(hid=HID, output_type=HyperCategoryType.GURU)]
        # blue offer with blue_market_sku_1 - with discount 10%
        blue_offer_1 = BlueOffer(
            price=1350, offerid='shop_sku_1', feedid=2, waremd5='BlueOffer-1-WithDisc-w', price_old=1500
        )

        # blue offer with blue_market_sku_2 - with discount 10%
        blue_offer_2 = BlueOffer(
            price=1800, offerid='shop_sku_2', feedid=2, waremd5='BlueOffer-2-WithDisc-w', price_old=2000
        )

        # blue offer with blue_market_sku_2 - with discount 5%
        blue_offer_3 = BlueOffer(
            price=1800, offerid='shop_sku_3', feedid=2, waremd5='BlueOffer-3-WithDisc-w', price_old=1895
        )

        # blue offer with blue_market_sku_3 - without discount
        blue_offer_4 = BlueOffer(price=1000, offerid='shop_sku_4', feedid=2, waremd5='BlueOffer-4-NoDiscou-w')

        # blue offer with blue_market_sku_4 - without discount
        blue_offer_5 = BlueOffer(price=1100, offerid='shop_sku_5', feedid=2, waremd5='BlueOffer-5-NoDiscou-w')

        # blue offer with blue_market_sku_4 - without discount
        blue_offer_6 = BlueOffer(price=1000, offerid='shop_sku_6', feedid=2, waremd5='BlueOffer-6-NoDiscou-w')

        # blue offer with blue_market_sku_5 - with discount 10%
        blue_offer_7 = BlueOffer(
            price=1350, offerid='shop_sku_7', feedid=2, waremd5='BlueOffer-7-WithDisc-w', price_old=1500
        )

        # blue offer with blue_market_sku_6 - with discount 5%
        blue_offer_8 = BlueOffer(
            price=300, offerid='shop_sku_8', feedid=2, waremd5='BlueOffer-8-WithDisc-w', discount=5
        )

        # models
        cls.index.models += [
            Model(hyperid=1234, hid=HID),
            Model(hyperid=1235, hid=HID),
            Model(hyperid=1236, hid=HID),
            Model(hyperid=1237, hid=HID),
            Model(hyperid=1238, hid=HID),
            Model(hyperid=1239, hid=HID),  # эту модель не рекомендует dj. Не должно быть в выдаче
            Model(hyperid=1241, hid=HID),
        ]

        # market skus
        cls.index.mskus += [
            MarketSku(
                title='dj_blue_market_sku_1',
                hyperid=1234,
                sku=11200001,
                waremd5='Sku1-wdDYWsIiLVm1goleg',
                blue_offers=[blue_offer_1],
            ),
            MarketSku(
                title='dj_blue_market_sku_2',
                hyperid=1235,
                sku=11200002,
                waremd5='Sku2-wdDYWsIiLVm1goleg',
                blue_offers=[blue_offer_2, blue_offer_3],
            ),
            MarketSku(
                title='dj_blue_market_sku_3',
                hyperid=1236,
                sku=11200003,
                waremd5='Sku3-wdDYWsIiLVm1goleg',
                blue_offers=[blue_offer_4],
            ),
            MarketSku(
                title='dj_blue_market_sku_4',
                hyperid=1237,
                sku=11200004,
                waremd5='Sku4-wdDYWsIiLVm1goleg',
                blue_offers=[blue_offer_5, blue_offer_6],
            ),
            MarketSku(
                title='dj_blue_market_sku_5',
                hyperid=1239,
                sku=11200005,
                waremd5='Sku5-wdDYWsIiLVm1goleg',
                blue_offers=[blue_offer_7],
            ),
            MarketSku(
                title='dj_blue_market_sku6',
                hyperid=1241,
                sku=11200006,
                waremd5='Sku6-wdDYWsIiLVm1goleg',
                blue_offers=[blue_offer_8],
            ),
        ]

        recommended_models = [
            DjModel(
                id="1241", title='model#1241'
            ),  # все (один) офферы со скидкой, байбокс тоже. Модель должна быть в выдаче
            DjModel(id="1240", title='model#1240'),  # модель не нашлась, не попадёт в выдачу
            DjModel(id="1238", title='model#1238'),  # нет офферов для модели, не попадёт в выдачу
            DjModel(
                id="1237", title='model#1237'
            ),  # все офферы без скидок, поэтому байбокс тоже. Нет скидки - нет модели в выдаче
            DjModel(id="1236", title='model#1236'),  # все (один) офферы без скидок, не попадёт в выдачу
            DjModel(id="1235", title='model#1235'),  # все офферы со скидкой, байбокс тоже. Модель должна быть в выдаче
            DjModel(
                id="1234", title='model#1234'
            ),  # все (один) офферы со скидкой, байбокс тоже. Модель должна быть в выдаче
        ]

        recommended_models_with_skus = [
            DjModel(id="1241", title='model#1241', sku="11200006"),
            DjModel(id="1235", title='model#1235', sku="11200002"),
            DjModel(id="1234", title='model#1234', sku="11200001"),
        ]

        cls.dj.on_request(exp='blue_deals', yandexuid='12345').respond(recommended_models)
        cls.dj.on_request(exp='blue_deals', yandexuid='12346').respond(recommended_models_with_skus)

    def test_dj_blue_deals(self):
        """
        https://st.yandex-team.ru/MARKETRECOM-1937
        Проверяем, что плейс deals при использовании dj отдаёт только модели,
        для которых есть офферы со скидкой. Выдача должна быть в том же формате,
        что и без dj. На каждую модель приходит ровно один оффер - байбокс.
        """
        for request in (
            'place=deals&yandexuid=12345&debug=da&rgb=BLUE&rearr-factors=' 'market_dj_exp_for_blue_deals=blue_deals',
            'place=dj&yandexuid=12345&debug=da&rgb=BLUE&dj-place=blue_deals&filter-discount-only=1&'
            'dj-use-default-models=true&dj-stats-source-policy=default_offers&dj-output-items=models',
            'place=dj&yandexuid=12346&debug=da&rgb=BLUE&dj-place=blue_deals&filter-discount-only=1&'
            'dj-use-default-models=true&dj-stats-source-policy=default_offers&dj-output-items=models',
        ):

            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            'entity': 'product',
                            'id': 1241,
                            'offers': {'items': [{'entity': 'offer', 'prices': {'discount': {'percent': 5}}}]},
                        },
                        {
                            'entity': 'product',
                            'id': 1235,
                            'offers': {'items': [{'entity': 'offer', 'prices': {'discount': NotEmpty()}}]},
                        },
                        {
                            'entity': 'product',
                            'id': 1234,
                            'offers': {'items': [{'entity': 'offer', 'prices': {'discount': {'percent': 10}}}]},
                        },
                    ]
                },
                preserve_order=True,
                allow_different_len=False,
            )

    @classmethod
    def prepare_blue_deals_with_hid(cls):
        # Выдача с использованием hid.
        cls.dj.on_request(exp='blue_deals_with_hid', yandexuid='123456', hid='54321').respond(
            [
                DjModel(id="1235", title='model#1235'),
                DjModel(id="1234", title='model#1234'),
            ]
        )

        # Выдача без hid.
        cls.dj.on_request(exp='blue_deals_with_hid', yandexuid='123456').respond(
            [
                DjModel(id="1241", title='model#1241'),
            ]
        )

    def test_blue_deals_with_hid(self):
        """
        https://st.yandex-team.ru/MARKETRECOM-2011
        По мотивам тикета - под другим флагом передаём в dj параметр hid
        hid передаётся в dj. Ожидаем 2 модели.
        """
        response = self.report.request_json(
            'place=deals&yandexuid=123456&debug=da&rgb=BLUE&rearr-factors='
            'market_dj_exp_for_blue_deals_with_hid=blue_deals_with_hid&hid=54321'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        'entity': 'product',
                        'id': 1235,
                        'offers': {'items': [{'entity': 'offer', 'prices': {'discount': NotEmpty()}}]},
                    },
                    {
                        'entity': 'product',
                        'id': 1234,
                        'offers': {'items': [{'entity': 'offer', 'prices': {'discount': {'percent': 10}}}]},
                    },
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_blue_deals_both_flags(self):
        """
        Проверяем, что если оба флага заданы, то выдача зависит от наличия параметры hid
        Если hid задан, то используется market_dj_exp_for_blue_deals_with_hid
        Если hid не задан, то используется market_dj_exp_for_blue_deals
        """
        response = self.report.request_json(
            'place=deals&yandexuid=123456&debug=da&rgb=BLUE&rearr-factors='
            'market_dj_exp_for_blue_deals=blue_deals;'
            'market_dj_exp_for_blue_deals_with_hid=blue_deals_with_hid&hid=54321'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        'entity': 'product',
                        'id': 1235,
                        'offers': {'items': [{'entity': 'offer', 'prices': {'discount': NotEmpty()}}]},
                    },
                    {
                        'entity': 'product',
                        'id': 1234,
                        'offers': {'items': [{'entity': 'offer', 'prices': {'discount': {'percent': 10}}}]},
                    },
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json(
            'place=deals&yandexuid=12345&debug=da&rgb=BLUE&rearr-factors='
            'market_dj_exp_for_blue_deals=blue_deals;'
            'market_dj_exp_for_blue_deals_with_hid=blue_deals_with_hid'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        'entity': 'product',
                        'id': 1241,
                        'offers': {'items': [{'entity': 'offer', 'prices': {'discount': {'percent': 5}}}]},
                    },
                    {
                        'entity': 'product',
                        'id': 1235,
                        'offers': {'items': [{'entity': 'offer', 'prices': {'discount': NotEmpty()}}]},
                    },
                    {
                        'entity': 'product',
                        'id': 1234,
                        'offers': {'items': [{'entity': 'offer', 'prices': {'discount': {'percent': 10}}}]},
                    },
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_discounts_for_vendor(cls):
        cls.index.vendors += [
            Vendor(vendor_id=1853),
            Vendor(vendor_id=1854),
        ]

        # models
        cls.index.models += [
            Model(hyperid=1242, hid=54321, vendor_id=1853),
            Model(hyperid=1243, hid=54321, vendor_id=1853),
            Model(hyperid=1244, hid=54321, vendor_id=1854),
            Model(hyperid=1245, hid=54321, vendor_id=1854),
        ]

        # offers
        blue_offer_100 = BlueOffer(price=1900, offerid='shop_sku_100', price_old=2000)

        blue_offer_101 = BlueOffer(price=1700, offerid='shop_sku_101', price_old=2000)

        blue_offer_102 = BlueOffer(price=1750, offerid='shop_sku_102', price_old=2000)

        blue_offer_103 = BlueOffer(price=1850, offerid='shop_sku_103', price_old=2000)

        # market skus
        cls.index.mskus += [
            MarketSku(
                title='dj_blue_market_sku_100', hyperid=1242, sku=11200100, vendor_id=1853, blue_offers=[blue_offer_100]
            ),
            MarketSku(title='dj_blue_market_sku_101', hyperid=1243, sku=11200101, blue_offers=[blue_offer_101]),
            MarketSku(title='dj_blue_market_sku_102', hyperid=1244, sku=11200102, blue_offers=[blue_offer_102]),
            MarketSku(title='dj_blue_market_sku_103', hyperid=1245, sku=11200103, blue_offers=[blue_offer_103]),
        ]

        cls.dj.on_request(exp='pass_vendor', yandexuid='12345', vendor_id='1853', hid='54321').respond(
            [
                DjModel(id="1243", title='model#1243'),
                DjModel(id="1242", title='model#1242'),
            ]
        )

        cls.dj.on_request(exp='pass_vendor', yandexuid='12346', vendor_id='1853,1854').respond(
            [
                DjModel(id="1243", title='model#1243'),
                DjModel(id="1244", title='model#1244'),
            ]
        )

        cls.dj.on_request(exp='pass_vendor', yandexuid='12347', vendor_id='1854', discount_from='10').respond(
            [
                DjModel(id="1244", title='model#1244'),
                DjModel(id="1245", title='model#1245'),
            ]
        )

        cls.dj.on_request(exp='pass_vendor', yandexuid='12347', vendor_id='1854').respond(
            [
                DjModel(id="1244", title='model#1244'),
                DjModel(id="1245", title='model#1245'),
            ]
        )

    def test_discounts_for_vendor(self):
        """
        Проверяем, что параметр vendor_id передаётся в dj независимо от dj-эксперимента
        """

        # Один вендор
        response = self.report.request_json(
            'place=deals&yandexuid=12345&debug=da&rgb=BLUE&hid=54321&vendor_id=1853&rearr-factors='
            'market_dj_exp_for_blue_deals_with_hid=pass_vendor'
        )

        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'product',
                        'id': 1243,
                        'vendor': {
                            'entity': 'vendor',
                            'id': 1853,
                        },
                    },
                    {
                        'entity': 'product',
                        'id': 1242,
                        'vendor': {
                            'entity': 'vendor',
                            'id': 1853,
                        },
                    },
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # Несколько вендоров, без категории
        response = self.report.request_json(
            'place=deals&yandexuid=12346&debug=da&rgb=BLUE&vendor_id=1853,1854&rearr-factors='
            'market_dj_exp_for_blue_deals=pass_vendor'
        )

        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'product',
                        'id': 1243,
                        'vendor': {
                            'entity': 'vendor',
                            'id': 1853,
                        },
                    },
                    {
                        'entity': 'product',
                        'id': 1244,
                        'vendor': {
                            'entity': 'vendor',
                            'id': 1854,
                        },
                    },
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # Товары вендора с достаточно большой скидкой
        response = self.report.request_json(
            'place=deals&yandexuid=12347&debug=da&rgb=BLUE&vendor_id=1854&discount-from=10&rearr-factors='
            'market_dj_exp_for_blue_deals=pass_vendor'
        )

        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'product',
                        'id': 1244,
                        'vendor': {
                            'entity': 'vendor',
                            'id': 1854,
                        },
                    },
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )
        # 1245 отфильтровалась из-за слишком маленькой скидки

        # Без ограничения на скидку в выдаче должны быть обе модели
        response = self.report.request_json(
            'place=deals&yandexuid=12347&debug=da&rgb=BLUE&vendor_id=1854&rearr-factors='
            'market_dj_exp_for_blue_deals=pass_vendor'
        )

        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'product',
                        'id': 1244,
                        'vendor': {
                            'entity': 'vendor',
                            'id': 1854,
                        },
                    },
                    {
                        'entity': 'product',
                        'id': 1245,
                        'vendor': {
                            'entity': 'vendor',
                            'id': 1854,
                        },
                    },
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_discounts_for_vendor_dj_exp(cls):
        cls.dj.on_request(exp='vendor_exp', yandexuid='55555', vendor_id='1853').respond(
            [
                DjModel(id="1242", title='model#1242'),
            ]
        )
        cls.dj.on_request(exp='vendor_exp', yandexuid='55555', vendor_id='1853', hid='54321').respond(
            [DjModel(id="1243", title='model#1243')]
        )
        cls.dj.on_request(exp='cat_exp', yandexuid='55555', hid='54321').respond(
            [
                DjModel(id="1244", title='model#1244'),
            ]
        )
        cls.dj.on_request(exp='morda_exp', yandexuid='55555').respond(
            [
                DjModel(id="1245", title='model#1245'),
            ]
        )

    def test_discounts_for_vendor_dj_exp(self):
        """
        Проверяем, что при наличии флага market_dj_exp_for_blue_deals_with_vendor
        dj-эксперимент определяется наличием id вендора в запросе
        """
        # Вендор указан, используется vendor_exp
        response = self.report.request_json(
            'vendor_id=1853&'
            'place=deals&yandexuid=55555&debug=da&rgb=BLUE&rearr-factors='
            'market_dj_exp_for_blue_deals=morda_exp;'
            'market_dj_exp_for_blue_deals_with_hid=cat_exp;'
            'market_dj_exp_for_blue_deals_with_vendor=vendor_exp'
        )

        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'product',
                        'id': 1242,
                    },
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # Вендор указан, используется vendor_exp. Категория тоже передаётся
        response = self.report.request_json(
            'vendor_id=1853&hid=54321&'
            'place=deals&yandexuid=55555&debug=da&rgb=BLUE&rearr-factors='
            'market_dj_exp_for_blue_deals=morda_exp;'
            'market_dj_exp_for_blue_deals_with_hid=cat_exp;'
            'market_dj_exp_for_blue_deals_with_vendor=vendor_exp'
        )

        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'product',
                        'id': 1243,
                    },
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # Вендор не указан. Категория задана, поэтому используется cat_exp
        response = self.report.request_json(
            'hid=54321&'
            'place=deals&yandexuid=55555&debug=da&rgb=BLUE&rearr-factors='
            'market_dj_exp_for_blue_deals=morda_exp;'
            'market_dj_exp_for_blue_deals_with_hid=cat_exp;'
            'market_dj_exp_for_blue_deals_with_vendor=vendor_exp'
        )

        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'product',
                        'id': 1244,
                    },
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # Вендор не указан. Категория не задана, поэтому используется morda_exp
        response = self.report.request_json(
            ''
            'place=deals&yandexuid=55555&debug=da&rgb=BLUE&rearr-factors='
            'market_dj_exp_for_blue_deals=morda_exp;'
            'market_dj_exp_for_blue_deals_with_hid=cat_exp;'
            'market_dj_exp_for_blue_deals_with_vendor=vendor_exp'
        )

        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'product',
                        'id': 1245,
                    },
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_dj_exp(cls):
        models = (1, 2, 3, 4)

        for hyperid in models:
            cls.index.models += [Model(hyperid=hyperid)]

        cls.index.mskus += [
            MarketSku(
                hyperid=hyperid,
                sku=hyperid * 10 + 1,
                blue_offers=[
                    BlueOffer(price=500, vat=Vat.NO_VAT, feedid=12345),
                ],
            )
            for hyperid in models
        ]

        cls.dj.on_request(yandexuid='456').respond([DjModel(id='1', title='model no exp')])

        cls.fast_dj.on_request(yandexuid='456').respond([DjModel(id='3', title='model no exp')])

        cls.dj.on_request(yandexuid='456', exp='external_candidates').respond([DjModel(id='2', title='model exp')])

        cls.fast_dj.on_request(yandexuid='456', exp='external_candidates').respond([DjModel(id='4', title='model exp')])

    def test_dj_exp(self):
        """
        Проверяем, что dj-эксперимент пробрасывается из rearr-factors
        """
        for place in ['attractive_models', 'omm_findings']:
            response = self.report.request_json(
                'place=blue_{place}&rgb=blue&yandexuid=456'
                '&rearr-factors=market_dj_exp_for_blue_{place}='.format(place=place)
            )
            self.assertFragmentIn(response, {"results": [{'entity': 'product', 'id': 1}]})
            self.assertFragmentNotIn(response, {"results": [{'entity': 'product', 'id': 2}]})

            response = self.report.request_json(
                'place=blue_{place}&rgb=blue&yandexuid=456'
                '&rearr-factors=market_dj_exp_for_blue_{place}=external_candidates'.format(place=place)
            )
            self.assertFragmentIn(response, {"results": [{'entity': 'product', 'id': 2}]})
            self.assertFragmentNotIn(response, {"results": [{'entity': 'product', 'id': 1}]})

            response = self.report.request_json(
                'place={place}&rgb=blue&yandexuid=456' '&rearr-factors=market_dj_exp_for_{place}='.format(place=place)
            )
            self.assertFragmentIn(response, {"results": [{'entity': 'product', 'id': 3}]})
            self.assertFragmentNotIn(response, {"results": [{'entity': 'product', 'id': 4}]})

            response = self.report.request_json(
                'place={place}&rgb=blue&yandexuid=456'
                '&rearr-factors=market_dj_exp_for_{place}=external_candidates'.format(place=place)
            )
            self.assertFragmentIn(response, {"results": [{'entity': 'product', 'id': 4}]})
            self.assertFragmentNotIn(response, {"results": [{'entity': 'product', 'id': 3}]})

    @classmethod
    def prepare_pass_client_to_dj(cls):
        cls.dj.on_request(exp="blue_attractive_models", yandexuid="1645", client_str="desktop").respond(
            [DjModel(id="1", title='model#1')]
        )

    def test_pass_client_to_dj(self):
        """
        https://st.yandex-team.ru/MARKETRECOM-2294
        Проверяем, что при наличии параметра client, его строковое значение передаётся в dj
        """
        response = self.report.request_json(
            'place=blue_attractive_models&rgb=blue&debug=1&yandexuid=1645&client=desktop&'
            'rearr-factors=market_dj_exp_for_blue_attractive_models=blue_attractive_models'
        )
        self.assertFragmentIn(response, {'entity': 'product', 'id': 1})

    @classmethod
    def prepare_different_clients_for_blue_attractive_models_on_dj(cls):
        cls.dj.on_request(exp="attractive_app", yandexuid="1452", uuid="u1d", platform="app").respond(
            [DjModel(id="1", title='model#1')]
        )
        cls.dj.on_request(exp="attractive_touch", yandexuid="1452", platform="touch").respond(
            [DjModel(id="2", title='model#2')]
        )
        cls.dj.on_request(
            exp="attractive_desktop", yandexuid="1452", platform="desktop", client_str="frontend"
        ).respond([DjModel(id="3", title='model#3')])
        cls.dj.on_request(exp="attractive_default", yandexuid="1452").respond([DjModel(id="4", title='model#4')])
        cls.dj.on_request(exp="attractive_default", yandexuid="1452", client_str="frontend").respond(
            [DjModel(id="4", title='model#4')]
        )
        cls.dj.on_request(exp="attractive_default", yandexuid="1452", uuid="u1d", platform="app").respond(
            [DjModel(id="4", title='model#4')]
        )

    def test_different_clients_for_blue_attractive_models_on_dj(self):
        """
        https://st.yandex-team.ru/MARKETRECOM-2259
        Проверяем, что если указаны dj-эксперименты для приложения/тача/десктопа,
        dj-эксп выбирается из них
        """
        rearr = (
            'rearr-factors=market_dj_exp_for_blue_attractive_models=attractive_default;'
            'market_dj_exp_for_blue_attractive_models_desktop=attractive_desktop;'
            'market_dj_exp_for_blue_attractive_models_touch=attractive_touch;'
            'market_dj_exp_for_blue_attractive_models_app=attractive_app'
        )
        query = 'place=blue_attractive_models&rgb=blue&debug=1&yandexuid=1452&' + rearr

        # Приложение
        response = self.report.request_json('uuid=u1d&' + query)
        self.assertFragmentIn(response, {'entity': 'product', 'id': 1})

        # Тач
        response = self.report.request_json('touch=1&' + query)
        self.assertFragmentIn(response, {'entity': 'product', 'id': 2})

        # Не тач - дефолтный эксперимент
        response = self.report.request_json('touch=0&' + query)
        self.assertFragmentIn(response, {'entity': 'product', 'id': 4})

        # Десктоп
        response = self.report.request_json('client=frontend&' + query)
        self.assertFragmentIn(response, {'entity': 'product', 'id': 3})

        # Запросы без флагов для разных клиентов должны идти в дефолтный dj-эксп
        response = self.report.request_json(
            'uuid=u1d&place=blue_attractive_models&rgb=blue&debug=1&yandexuid=1452&'
            'rearr-factors=market_dj_exp_for_blue_attractive_models=attractive_default'
        )
        self.assertFragmentIn(response, {'entity': 'product', 'id': 4})

        response = self.report.request_json(
            'touch=1&place=blue_attractive_models&rgb=blue&debug=1&yandexuid=1452&'
            'rearr-factors=market_dj_exp_for_blue_attractive_models=attractive_default'
        )
        self.assertFragmentIn(response, {'entity': 'product', 'id': 4})

        response = self.report.request_json(
            'client=frontend&place=blue_attractive_models&rgb=blue&debug=1&yandexuid=1452&'
            'rearr-factors=market_dj_exp_for_blue_attractive_models=attractive_default'
        )
        self.assertFragmentIn(response, {'entity': 'product', 'id': 4})

    @classmethod
    def prepare_pass_all_ids(cls):
        for hyperid in (5, 6):
            cls.index.models += [Model(hyperid=hyperid)]

        cls.index.mskus += [
            MarketSku(
                hyperid=hyperid,
                sku=hyperid * 10 + 1,
                blue_offers=[
                    BlueOffer(price=500, vat=Vat.NO_VAT, feedid=12345),
                ],
            )
            for hyperid in (5, 6)
        ]

        cls.dj.on_request(yandexuid='789').respond([DjModel(id='5', title='model')])

        cls.dj.on_request(
            yandexuid='789', msid='abc', puid='def', uuid='19', idfa='21', gaid='23', platform="app"
        ).respond([DjModel(id='6', title='model')])

    def test_dj_use_offers(self):
        """
        Проверяем, что офферная выдача возвращает офферы
        """
        response = self.report.request_json(
            'place=blue_attractive_models&rgb=blue&yandexuid=789'
            '&rearr-factors=market_dj_exp_for_blue_attractive_models=&dj-output-items=offers'
        )

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        'entity': 'offer',
                        'sku': '51',
                    }
                ]
            },
        )

    def test_pass_all_ids(self):
        """
        Проверяем, что в запрос в dj пробрасываются reqid (в msid), puid, uuid,
        idfa и gaid
        """
        response = self.report.request_json(
            'place=blue_attractive_models&rgb=blue&yandexuid=789'
            '&rearr-factors=market_dj_exp_for_blue_attractive_models='
        )

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        'entity': 'product',
                        'id': 5,
                    }
                ]
            },
        )

        self.assertFragmentNotIn(
            response,
            {
                "results": [
                    {
                        'entity': 'product',
                        'id': 6,
                    }
                ]
            },
        )

        response = self.report.request_json(
            'place=blue_attractive_models&rgb=blue&yandexuid=789'
            '&rearr-factors=market_dj_exp_for_blue_attractive_models=&reqid=abc&puid=def&uuid=19'
            '&idfa=21&gaid=23'
        )

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        'entity': 'product',
                        'id': 6,
                    }
                ]
            },
        )

        self.assertFragmentNotIn(
            response,
            {
                "results": [
                    {
                        'entity': 'product',
                        'id': 5,
                    }
                ]
            },
        )

    @classmethod
    def prepare_default_models(cls):
        for hyperid in DEFAULT_MODELS:
            cls.index.models += [Model(hyperid=hyperid)]

        cls.index.mskus += [
            MarketSku(
                hyperid=hyperid,
                sku=hyperid * 10 + 1,
                blue_offers=[
                    BlueOffer(price=500, vat=Vat.NO_VAT, feedid=12345),
                ],
            )
            for hyperid in DEFAULT_MODELS
        ]

        cls.index.mskus += [
            MarketSku(
                hyperid=hyperid,
                sku=hyperid * 10 + 1,
                blue_offers=[
                    BlueOffer(price=300, vat=Vat.NO_VAT, feedid=12345),
                ],
            )
            for hyperid, _, _ in COMMONLY_PURCHASED_DJ_DEFAULT_MODELS
        ]

        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.DJ_DEFAULT_MODELS_LIST,
                kind=YamarecPlace.Type.DJ_DEFAULT_MODELS_LIST,
                partitions=[
                    YamarecDjDefaultModelsList(dj_default_models_list=DJ_DEFAULT_MODELS),
                ],
            ),
        ]

        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.COMMONLY_PURCHASED_DJ_DEFAULT_MODELS_LIST,
                kind=YamarecPlace.Type.DJ_DEFAULT_MODELS_LIST,
                partitions=[
                    YamarecDjDefaultModelsList(dj_default_models_list=COMMONLY_PURCHASED_DJ_DEFAULT_MODELS),
                ],
            ),
        ]

        # empty response for 1011
        cls.dj.on_request(yandexuid='1011').respond([])

        # 400 for 1213
        pass

    def test_default_models(self):
        """
        Проверяем, что, если dj ответил пустой выдачей или не ответил вовсе,
        отдаются дефолтные модели (тыква Dj)
        """
        # dj empty recom
        yuid = 1011
        response = self.report.request_json(
            'place=blue_attractive_models&rgb=blue&yandexuid=%d'
            '&rearr-factors=market_dj_exp_for_blue_attractive_models=' % yuid
        )
        self.assertFragmentIn(response, {"total": 0, "results": []}, allow_different_len=False)

        # dj request failed
        yuid = 1213
        response = self.report.request_json(
            'place=blue_attractive_models&rgb=blue&yandexuid=%d'
            '&rearr-factors=market_dj_exp_for_blue_attractive_models=' % yuid
        )
        for model in DEFAULT_MODELS:
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            'entity': 'product',
                            'id': model,
                        }
                    ]
                },
            )
        self.error_log.expect(code=3787).once()

    def test_commonly_purchased_default_models(self):
        """
        Проверяем, что, если dj ответил пустой выдачей или не ответил вовсе,
        в плейсе commonly_purchased отдаются дефолтные модели (тыква Dj)
        По умолчанию загружается 16 моделей
        """
        # dj empty recom
        yuid = 1011
        response = self.report.request_json(
            'place=commonly_purchased&rgb=blue&yandexuid=%d&rearr-factors=turn_on_commonly_purchased=1;'
            'market_dj_exp_for_commonly_purchased=' % yuid
        )
        self.assertFragmentIn(response, {"total": 0, "results": []}, allow_different_len=False)

        # dj request failed
        yuid = 1213
        response = self.report.request_json(
            'place=commonly_purchased&rgb=blue&yandexuid=%d&rearr-factors=turn_on_commonly_purchased=1;'
            'market_dj_exp_for_commonly_purchased=' % yuid
        )
        for model_id, _, _ in COMMONLY_PURCHASED_DJ_DEFAULT_MODELS[:16]:
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            'entity': 'product',
                            'id': model_id,
                        }
                    ]
                },
            )
        self.error_log.expect(code=3787).once()

    def test_no_show_commonly_purchased(self):
        """
        Проверяем, что, если dj ответил пустой выдачей или не ответил вовсе,
        плейс commonly_purchased отвечает пустой выдачей при наличии флага
        market_dj_show_pumpkin_for_commonly_purchased=false
        """
        for yuid in (1011, 1213):
            response = self.report.request_json(
                'place=commonly_purchased&rgb=blue&yandexuid=%d&rearr-factors=turn_on_commonly_purchased=1;'
                'market_dj_exp_for_commonly_purchased=;'
                'market_dj_show_pumpkin_for_commonly_purchased=false' % yuid
            )
            self.assertFragmentIn(response, {"total": 0, "results": []}, allow_different_len=False)

        self.error_log.expect(code=3787).once()

    def _inspect_dj_unistat(self, query, await_pumpkin=False):
        """
        Проверка ответа unistat-ручки. Основана на _inspect_parallel_unistat
        из test_omm_parallel
        """

        # Сохраняем счётчики до запроса
        tass_data = self.report.request_tass()

        # Делаем запрос
        self.report.request_json(query)

        # Сохраняем счётчики после запроса
        tass_data_new = self.report.request_tass()

        # Сравниваем
        # Число обращений к тыкве не изменилось
        self.assertEqual(
            tass_data.get('dj_pumpkin_shows_count_dmmm', 0) + int(await_pumpkin),
            tass_data_new.get('dj_pumpkin_shows_count_dmmm', 0),
        )
        # Тайминги как-то считаются
        self.assertIn('dj_request_time_hgram', tass_data_new.keys())
        self.assertIn('dj_request_time_hgram', tass_data_new.keys())

    def test_dj_unistat(self):
        """
        Проверяем, что запросы к place=blue_attractive_models
        с market_dj_exp_for_blue_attractive_models влияют на unistat-счётчики
        """

        # Нормальная выдача
        self._inspect_dj_unistat(
            'place=blue_attractive_models&rgb=blue&yandexuid=123&numdoc=5'
            '&rearr-factors=market_dj_exp_for_blue_attractive_models=&debug=1'
        )

        # Тыква
        for yuid in (1011, 1213):
            self._inspect_dj_unistat(
                'place=blue_attractive_models&rgb=blue&yandexuid={}'
                '&rearr-factors=market_dj_exp_for_blue_attractive_models=&debug=1'.format(yuid),
                yuid == 1213,
            )

        # Каждый вызов функции для тыквы порождает запрос в репорт, на каждый запрос одна ошибка хождения в DJ
        self.error_log.expect(code=3787).once()

    @classmethod
    def prepare_pass_discount_from_to_dj(cls):
        cls.dj.on_request(exp="blue_deals_with_discount_min_bound", yandexuid="1527", discount_from="10").respond(
            [
                DjModel(id="1234", title='model#1234'),  # модель со скидкой 10%
                DjModel(id="1241", title='model#1241'),  # модель со скидкой 5%
            ]
        )

        cls.dj.on_request(exp="blue_deals_with_discount_min_bound", yandexuid="1527", discount_from="3").respond(
            [
                DjModel(id="1234", title='model#1234'),  # модель со скидкой 10%
                DjModel(id="1241", title='model#1241'),  # модель со скидкой 5%
            ]
        )

    def test_pass_discount_from_to_dj(self):
        """
        Проверяем, что если есть параметр discount-from, его значение передаётся в dj
        """

        # модель 1241 будет отфильтрована в репорте, так как у неё слишком маленькая скидка
        response = self.report.request_json(
            "place=deals&pp=18&numdoc=100&rgb=BLUE&filter-discount-only=1&rearr-factors="
            "market_dj_exp_for_blue_deals=blue_deals_with_discount_min_bound&yandexuid=1527&"
            "discount-from=10"
        )
        self.assertFragmentIn(
            response,
            {"search": {"total": 1, "results": [{"entity": "product", "id": 1234}]}},
            allow_different_len=False,
            preserve_order=True,
        )

        response = self.report.request_json(
            "place=deals&pp=18&numdoc=100&rgb=BLUE&filter-discount-only=1&rearr-factors="
            "market_dj_exp_for_blue_deals=blue_deals_with_discount_min_bound&yandexuid=1527&"
            "discount-from=3"
        )
        # обе модели попадут в выдачу
        self.assertFragmentIn(
            response,
            {"search": {"total": 2, "results": [{"entity": "product", "id": 1234}, {"entity": "product", "id": 1241}]}},
            allow_different_len=False,
            preserve_order=True,
        )

    @classmethod
    def prepare_for_product_accessories_blue(cls):
        cls.index.models += [
            # key model
            Model(hyperid=11),
            # accessories
            Model(hyperid=12),
            Model(hyperid=13),
            Model(hyperid=14),
            Model(hyperid=15),
        ]

        cls.index.shops += [
            Shop(
                fesh=431782,
                datafeed_id=1,
                priority_region=213,
                name='blue_shop_1',
                currency=Currency.RUR,
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                tax_system=Tax.OSN,
                cpa=Shop.BLUE_REAL,
            ),
            Shop(
                fesh=80001,
                datafeed_id=70001,
                priority_region=213,
                currency=Currency.RUR,
                blue=Shop.BLUE_REAL,
                warehouse_id=147,
            ),
            Shop(
                fesh=80002,
                datafeed_id=70002,
                priority_region=213,
                currency=Currency.RUR,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
            ),
            Shop(fesh=1104, priority_region=213),
        ]
        cls.index.warehouse_priorities += [
            WarehousesPriorityInRegion(
                regions=[213, 225],
                warehouse_with_priority=[
                    WarehouseWithPriority(
                        100, 1
                    ),  # необходим хоть какой-то приоретет, чтобы связки вообще создались. Для всех остальных будет выставлен минимальный приоритет
                ],
            )
        ]

        # key model offers
        sku_offer_11_1 = BlueOffer(model_title='Key model #11', price=1000, feedid=70001)
        # buybox
        sku_offer_11_2 = BlueOffer(model_title='Key model #11', price=1100, feedid=70001)

        # accessories
        # accessory offer from target warehouse
        sku_offer_12_1 = BlueOffer(model_title='Accessory model #12', price=210, feedid=70001)
        # offer from different warehouse
        sku_offer_12_2 = BlueOffer(model_title='Accessory model #12', price=100, feedid=70002)
        # best offer from different warehouse
        sku_offer_12_3 = BlueOffer(model_title='Accessory model #12', price=200, feedid=70002)
        # second accessory offer from different warehouse
        sku_offer_13 = BlueOffer(model_title='Accessory model #13', price=300, feedid=70002)
        # yet another accessory offer from target warehouse
        sku_offer_14 = BlueOffer(model_title='Accessory model #14', price=300, feedid=70001)
        # yet another accessory offer from target warehouse
        sku_offer_15 = BlueOffer(model_title='Accessory model #15', price=500, feedid=70001)

        cls.index.offers += [Offer(hyperid=12, fesh=1104, price=234)]

        cls.index.mskus += [
            # key sku
            MarketSku(hyperid=11, sku=10011, blue_offers=[sku_offer_11_1, sku_offer_11_2]),
            # accessory sku without offers in target warehouse
            MarketSku(hyperid=12, sku=10012, blue_offers=[sku_offer_12_1, sku_offer_12_2, sku_offer_12_3]),
            # accessory sku with offers in target warehouse
            MarketSku(hyperid=13, sku=10013, blue_offers=[sku_offer_13]),
            MarketSku(hyperid=14, sku=10014, blue_offers=[sku_offer_14]),
            MarketSku(hyperid=15, sku=10015, blue_offers=[sku_offer_15]),
        ]

        cls.dj.on_request(exp="product_accessories_blue", yandexuid="1837", hyperid="11", fesh="431782").respond(
            [
                DjModel(id="15", title='model#15'),
                DjModel(id="13", title='model#13'),
                DjModel(id="12", title='model#12'),
                DjModel(id="14", title='model#14'),
            ]
        )

        cls.dj.on_request(exp="product_accessories_blue", yandexuid="1837", hyperid="11").respond(
            [
                DjModel(id="15", title='model#15'),
                DjModel(id="13", title='model#13'),
                DjModel(id="12", title='model#12'),
                DjModel(id="14", title='model#14'),
            ]
        )

    def test_dj_for_product_accessories_blue(self):
        """
        https://st.yandex-team.ru/MARKETRECOM-2023
        Проверяем, что под флагом market_dj_exp_for_product_accessories_blue
        плейс product_accessories ходит в dj. Логика пессимизации должна сохраниться

        Аксессуары, недоступные на складе байбокса ключевой модели, пессимизируются
        (Если передан идентификатор sku ключевой модели)
        """
        response = self.report.request_json(
            'place=product_accessories&rgb=blue&hyperid=11&rids=213&debug=1&yandexuid=1837&'
            'rearr-factors=market_dj_exp_for_product_accessories_blue=product_accessories_blue;market_disable_product_accessories=0'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 4,
                    'results': [
                        {'entity': 'product', 'id': 15},
                        {'entity': 'product', 'id': 12},
                        {'entity': 'product', 'id': 14},
                        {'entity': 'product', 'id': 13},  # модель пессимизирована
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_pass_market_sku(cls):
        cls.dj.on_request(exp="pass_msku", yandexuid="1636", hyperid="11", market_sku="10011").respond(
            [
                DjModel(id="12", title='model#12'),
            ]
        )

    def test_pass_market_sku(self):
        """
        https://st.yandex-team.ru/MARKETRECOM-2390
        На примере плейса product_accessories_blue проверяем, что параметр market-sku передаётся из репорта в dj
        """
        response = self.report.request_json(
            'place=product_accessories_blue&rgb=blue&hyperid=11&market-sku=10011&yandexuid=1636&'
            'rids=213&rearr-factors=market_dj_exp_for_product_accessories_blue=pass_msku;market_disable_product_accessories=0'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 1,
                    'results': [
                        {'entity': 'product', 'id': 12},
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_dj_for_also_viewed_blue(cls):
        cls.dj.on_request(exp="also_viewed_blue", yandexuid="1838", hyperid="11").respond(
            [
                DjModel(id="15", title='model#15'),
                DjModel(id="14", title='model#14'),
                DjModel(id="13", title='model#13'),
                DjModel(id="12", title='model#12'),
            ]
        )

    def test_dj_for_also_viewed_blue(self):
        """
        https://st.yandex-team.ru/MARKETRECOM-2023
        Проверяем, что под флагом market_dj_exp_for_product_accessories_blue
        плейс product_accessories ходит в dj
        """
        response = self.report.request_json(
            'place=also_viewed&rgb=blue&hyperid=11&rids=213&debug=1&yandexuid=1838&'
            'rearr-factors=market_dj_exp_for_also_viewed_blue=also_viewed_blue'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 4,
                    'results': [
                        {'entity': 'product', 'id': 15},
                        {'entity': 'product', 'id': 14},
                        {'entity': 'product', 'id': 13},
                        {'entity': 'product', 'id': 12},
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_dj_for_popular_products(cls):
        cls.dj.on_request(exp="popular_products_dj_exp_name", yandexuid="1945", hid="54321").respond(
            [
                DjModel(id="1236", title='model#1236'),
                DjModel(id="1234", title='model#1234'),
                DjModel(id="1235", title='model#1235'),
            ]
        )

    def test_dj_for_popular_products(self):
        """
        https://st.yandex-team.ru/MARKETRECOM-2064
        Проверяем, что под флагом market_dj_exp_for_popular_products на синем
        плейс popular_products ходит в dj за моделями
        """
        response = self.report.request_json(
            'place=popular_products&rgb=blue&debug=1&yandexuid=1945&hid=54321&'
            'rearr-factors=market_dj_exp_for_popular_products=popular_products_dj_exp_name'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 3,
                    'results': [
                        {'entity': 'product', 'id': 1236},
                        {'entity': 'product', 'id': 1234},
                        {'entity': 'product', 'id': 1235},
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_dj_for_recent_findings(cls):
        cls.dj.on_request(exp="recent_findings_dj", yandexuid="2245",).respond(
            [
                DjModel(id="1235", title='model#1235'),
                DjModel(id="1236", title='model#1236'),
                DjModel(id="1234", title='model#1234'),
            ]
        )

        cls.dj.on_request(exp="empty_recent_findings", yandexuid="2047").respond([])

    @classmethod
    def prepare_disable_dj_for_recent_findings(cls):
        cls.index.models += [
            Model(hyperid=6000, hid=600),
        ]
        cls.index.mskus += [
            MarketSku(hyperid=6000, sku=60000, blue_offers=[BlueOffer()]),
        ]
        cls.bigb.on_request(yandexuid=700, client='merch-machine').respond(
            counters=[
                BeruPersHistoryModelViewLastTimeCounter(
                    [
                        ModelLastSeenEvent(model_id=6000, timestamp=NOW - 2 * DAY),
                    ]
                ),
            ]
        )
        cls.dj.on_request(exp="recent_findings_dj", yandexuid="700",).respond(
            [
                DjModel(id="1235", title='model#1235'),
                DjModel(id="1236", title='model#1236'),
                DjModel(id="1234", title='model#1234'),
            ]
        )

    def test_disable_dj_for_recent_findings(self):
        """
        https://st.yandex-team.ru/MARKETRECOM-2244
        Проверяем, что при наличии флага market_disable_dj_for_recent_findings
        place=products_by_history&history=blue не ходит в dj
        """
        for suffix in ['cpa=real', 'rgb=blue']:
            response = self.report.request_json(
                'place=products_by_history&{}&history=blue&yandexuid=700&'
                'rearr-factors=market_dj_exp_for_recent_findings=recent_findings_dj;'
                'market_disable_dj_for_recent_findings=true'.format(suffix)
            )
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'total': 1,
                        'results': [
                            {'entity': 'product', 'id': 6000},
                        ],
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )

    def test_dj_for_recent_findings(self):
        """
        https://st.yandex-team.ru/MARKETRECOM-2244
        Проверяем, что под флагом market_dj_exp_for_recent_findings на синем
        плейс products_by_history&history=blue (мои недавние находки) входит в dj за моделями
        """
        for suffix in ['cpa=real', 'rgb=blue']:
            response = self.report.request_json(
                'place=products_by_history&history=blue&{}&debug=1&yandexuid=2245&'
                'rearr-factors=market_dj_exp_for_recent_findings=recent_findings_dj'.format(suffix)
            )
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'total': 3,
                        'results': [
                            {'entity': 'product', 'id': 1235},
                            {'entity': 'product', 'id': 1236},
                            {'entity': 'product', 'id': 1234},
                        ],
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )

        response = self.report.request_json(
            'place=dj&history=blue&rgb=blue&debug=1&yandexuid=2245&'
            'dj-place=recent_findings_dj&'
            'dj-default-item-count=60&dj-use-default-models=0&dj-alert-empty-recom=0'
        )

        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 3,
                    'results': [
                        {'entity': 'product', 'id': 1235},
                        {'entity': 'product', 'id': 1236},
                        {'entity': 'product', 'id': 1234},
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # флаг market_disable_dj_for_recent_findings=false не запрещает поход в dj
        response = self.report.request_json(
            'place=products_by_history&history=blue&rgb=blue&debug=1&yandexuid=2245&'
            'rearr-factors=market_dj_exp_for_recent_findings=recent_findings_dj;'
            'market_disable_dj_for_recent_findings=false'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 3,
                    'results': [
                        {'entity': 'product', 'id': 1235},
                        {'entity': 'product', 'id': 1236},
                        {'entity': 'product', 'id': 1234},
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # Проверяем, что в случае пустого ответа ошибка 3788 не пишется в лог
        for suffix in ['cpa=real', 'rgb=blue']:
            response = self.report.request_json(
                'place=products_by_history&history=blue&{}&debug=1&yandexuid=2047&'
                'rearr-factors=market_dj_exp_for_recent_findings=empty_recent_findings'.format(suffix)
            )

        # Проверяем, что параметр fesh тоже работает
        for suffix in ['cpa=real', 'rgb=blue']:
            response = self.report.request_json(
                'place=products_by_history&{}&debug=1&yandexuid=2245&fesh=100&'
                'rearr-factors=market_dj_exp_for_recent_findings=recent_findings_dj'.format(suffix)
            )
            self.assertFragmentIn(
                response, {'search': {'total': 0, 'results': []}}, preserve_order=True, allow_different_len=False
            )

    @classmethod
    def prepare_dj_pass_cart(cls):
        cls.index.hypertree += [
            HyperCategory(hid=16044621, name="Велосипеды", output_type=HyperCategoryType.GURU),
            HyperCategory(hid=15728039, name="Замки для велосипедов", output_type=HyperCategoryType.GURU),
        ]

        cls.index.shops += [
            Shop(
                fesh=1111,
                datafeed_id=1111,
                priority_region=213,
                regions=[225],
                name="Беру!",
                supplier_type=Shop.FIRST_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
                fulfillment_program=True,
            ),
        ]

        description = "Клиент всегда У! Подавай скидки ему..."

        def create_offers(hid, title, hyperid):
            cls.index.models += [Model(hid=hid, hyperid=hyperid, title=title)]
            cls.index.mskus += [
                MarketSku(
                    title=title + " от Беру и со скидкой",
                    descr=description,
                    hyperid=hyperid,
                    sku=hyperid * 10 + 1,
                    hid=hid,
                    delivery_buckets=[1234],
                    blue_offers=[
                        BlueOffer(
                            price=1300,
                            price_old=1500,
                            feedid=1111,
                            offerid="beru_model{}_Q".format(hyperid),
                            waremd5="BLUEModel{}FEED1111QQQ".format(hyperid)[0:22],
                            randx=hyperid * 10 + 1,
                        )
                    ],
                ),
            ]

        create_offers(hid=16044621, title="Велосипед", hyperid=71)
        create_offers(hid=15728039, title="Замок для велосипеда", hyperid=72)

        cls.dj.on_request(exp="pass_cart_to_dj", yandexuid="1728", cart="71").respond(
            [
                DjModel(id="72", title='Замок для велосипеда'),
            ]
        )

        cls.dj.on_request(exp="pass_cart_to_dj", yandexuid="1728", cart="71,72").respond(
            [
                DjModel(id="1235", title='model#1235'),
                DjModel(id="1236", title='model#1236'),
                DjModel(id="1234", title='model#1234'),
            ]
        )

    def test_dj_pass_cart(self):
        """
        https://st.yandex-team.ru/MARKETOUT-31651
        Проверяем, что под флагом market_dj_pass_cart в плейсах, использующих TDjMarket,
        модели из корзины передаются в dj
        """
        single_item_in_cart = ["cart=BLUEModel71FEED1111QQQ", "cart-fo=1-1111.beru_model71_Q", "cart-sku=711"]

        two_items_in_cart = [
            "cart=BLUEModel71FEED1111QQQ,BLUEModel72FEED1111QQQ",
            "cart-fo=1-1111.beru_model71_Q,1-1111.beru_model72_Q",
            "cart-sku=711,721",
        ]

        # одна модель в корзине
        for cart in single_item_in_cart:
            response = self.report.request_json(
                'numdoc=1&place=blue_attractive_models&rgb=blue&rearr-factors=market_dj_pass_cart=1;'
                'market_dj_exp_for_blue_attractive_models=pass_cart_to_dj&yandexuid=1728&{}'.format(cart)
            )

            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'results': [
                            {'entity': 'product', 'id': 72},
                        ]
                    }
                },
                allow_different_len=False,
            )

        for cart in single_item_in_cart:
            response = self.report.request_json(
                'numdoc=1&place=blue_omm_findings&rgb=blue&rearr-factors=market_dj_pass_cart=1;'
                'market_dj_exp_for_blue_omm_findings=pass_cart_to_dj&yandexuid=1728&{}'.format(cart)
            )

            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'results': [
                            {'entity': 'product', 'id': 72},
                        ]
                    }
                },
                allow_different_len=False,
            )

        # несколько моделей в корзине
        for cart in two_items_in_cart:
            response = self.report.request_json(
                'numdoc=1&place=blue_attractive_models&rgb=blue&rearr-factors=market_dj_pass_cart=1;'
                'market_dj_exp_for_blue_attractive_models=pass_cart_to_dj&yandexuid=1728&{}'.format(cart)
            )

            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'results': [
                            {'entity': 'product', 'id': 1235},
                        ]
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )

        for cart in two_items_in_cart:
            response = self.report.request_json(
                'numdoc=1&place=blue_omm_findings&rgb=blue&rearr-factors=market_dj_pass_cart=1;'
                'market_dj_exp_for_blue_omm_findings=pass_cart_to_dj&yandexuid=1728&{}'.format(cart)
            )

            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'results': [
                            {'entity': 'product', 'id': 1235},
                        ]
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )

    @classmethod
    def prepare_market_req_id_forwarded(cls):
        cls.dj.on_request(exp="blue_attractive_models", yandexuid="1645", client_str="desktop").respond(
            [DjModel(id="1", title='model#1')]
        )

    def test_market_req_id_forwarded(self):
        market_req_id_header_value = '123456789/deadbeef-0000-0000-0000-deadbeef0000/1'
        self.report.request_json(
            "place=blue_attractive_models&rgb=blue&debug=1&yandexuid=1645&client=desktop&"
            "rearr-factors=market_dj_exp_for_blue_attractive_models=blue_attractive_models",
            headers={'X-Market-Req-ID': market_req_id_header_value},
        )
        self.dj_log.expect(headers=Contains("x-market-req-id=" + market_req_id_header_value + "/"))

    @classmethod
    def prepare_disabled_experiments(cls):
        cls.index.models += [Model(hyperid=i) for i in DISABLED_EXPERIMENT_MODELS]
        cls.index.mskus += [
            MarketSku(
                hyperid=hyperid,
                sku=hyperid * 10 + 1,
                blue_offers=[
                    BlueOffer(price=500, vat=Vat.NO_VAT, feedid=12345),
                ],
            )
            for hyperid in DISABLED_EXPERIMENT_MODELS
        ]

        recommendations = [DjModel(id=i, title="recommendation#{}".format(i)) for i in DISABLED_EXPERIMENT_MODELS]
        cls.dj.on_request(exp="experiment_to_disable", yandexuid="123456").respond(recommendations)

    def test_disabled_experiments(self):
        response1 = self.report.request_json(
            "place=dj&rgb=blue&yandexuid=123456&client=desktop&" "dj-place=experiment_to_disable"
        )
        response2 = self.report.request_json(
            "place=dj&rgb=blue&yandexuid=123456&client=desktop&"
            "dj-place=experiment_to_disable&"
            "rearr-factors=recom_disabled_dj_places=experiment_to_disable"
        )
        self.assertTrue(len(response1.root["search"]["results"]) > 0)
        self.assertEqual(len(response2.root["search"]["results"]), 0)

    @classmethod
    def prepare_attractive_models(cls):
        cls.fast_dj.on_request(exp='white_attractive_models', yandexuid='001').respond(
            [DjModel(id=1721802, title='title'), DjModel(id=1721801, title='title'), DjModel(id=1721803, title='title')]
        )

        cls.index.models += [
            Model(hyperid=1721801, hid=1721800, title='Модель №1', vendor_id=172180101, opinion=Opinion(rating=4.0)),
            Model(hyperid=1721802, hid=1721800),
            Model(hyperid=1721803, hid=1721810),
        ]
        cls.index.offers += [
            Offer(title='title N1', hyperid=1721801, discount=50),
            Offer(title='title N2', hyperid=1721802, discount=50),
            Offer(title='title N3', hyperid=1721803, discount=50),
        ]
        cls.index.hypertree += [
            HyperCategory(hid=1721800, output_type=HyperCategoryType.GURU),
        ]

    def test_attractive_models(self):
        """Проверяем, что на выдаче place=attractive_models находятся модели, рекоммендуемые DJ"""
        response = self.report.request_json('place=attractive_models&yandexuid=001&numdoc=3&debug=1')

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "product", "id": 1721802},
                    {"entity": "product", "id": 1721801},
                    {"entity": "product", "id": 1721803},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_omm_numdoc(self):
        """Проверяем, что на плейсе attractive_models работает параметр numdoc"""
        response = self.report.request_json('place=attractive_models&yandexuid=001&numdoc=2&debug=1')
        self.assertFragmentIn(
            response,
            {"results": [{"entity": "product", "id": 1721802}, {"entity": "product", "id": 1721801}]},
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_dj_different_ids(cls):
        kwargs_list = [
            {'puid': '123'},
            {'gaid': '456'},
            {'idfa': '789'},
            {
                'yandexuid': '009',
                'puid': '123',
                'gaid': '456',
                'idfa': '789',
            },
        ]
        for kwargs in kwargs_list:
            cls.fast_dj.on_request(exp='white_attractive_models', **kwargs).respond(
                [
                    DjModel(id=1721802, title='title'),
                    DjModel(id=1721801, title='title'),
                    DjModel(id=1721803, title='title'),
                ]
            )

    def test_dj_by_different_ids(self):
        """Проверяем, что в запросах к DJ передаются puid, gaid, idfa, если они есть"""
        for uniq_id in ['puid=123', 'gaid=456', 'idfa=789', 'yandexuid=009&puid=123&gaid=456&idfa=789']:
            response = self.report.request_json('place=attractive_models&numdoc=3&{}'.format(uniq_id))
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {"entity": "product", "id": 1721802},
                        {"entity": "product", "id": 1721801},
                        {"entity": "product", "id": 1721803},
                    ]
                },
                preserve_order=True,
                allow_different_len=False,
            )

    @classmethod
    def prepare_specific_sku(cls):
        cls.dj.on_request(exp='specific_sku', yandexuid='001').respond(
            models=[DjModel(id=2000001, title='model1'), DjModel(id=2000002, title='model2')]
        )
        cls.dj.on_request(exp='specific_sku', yandexuid='002').respond(
            models=[DjModel(id=2000001, title='model1'), DjModel(id=2000002, title='model2', sku="2000002001")]
        )
        cls.dj.on_request(exp='specific_sku', yandexuid='003').respond(
            models=[DjModel(id=2000001, title='model1'), DjModel(id=2000002, title='model2', sku="2000002002")]
        )
        cls.dj.on_request(exp='specific_sku', yandexuid='004').respond(
            models=[DjModel(id=2000001, title='model1'), DjModel(id=2000002, title='model2', sku="2000002003")]
        )

        cls.index.models += [
            Model(hyperid=2000001),
            Model(hyperid=2000002),
        ]

        cls.index.mskus += [
            MarketSku(
                hyperid=2000001,
                sku=2000001001,
                blue_offers=[
                    BlueOffer(price=500, vat=Vat.NO_VAT, feedid=12345),
                ],
            ),
            MarketSku(
                hyperid=2000002,
                sku=2000002001,
                blue_offers=[
                    BlueOffer(price=500, vat=Vat.NO_VAT, feedid=12345),
                ],
            ),
            MarketSku(
                hyperid=2000002,
                sku=2000002002,
                blue_offers=[
                    BlueOffer(price=500, vat=Vat.NO_VAT, feedid=12345),
                ],
            ),
        ]

    def test_specific_sku(self):
        response = self.report.request_json(
            'place=dj&rgb=blue&client=desktop&dj-place=specific_sku&pp=18' '&yandexuid=001'
        )
        self.assertFragmentIn(
            response,
            {"results": [{"entity": "product", "id": 2000001}, {"entity": "product", "id": 2000002}]},
            preserve_order=True,
            allow_different_len=False,
        )
        response = self.report.request_json(
            'place=dj&rgb=blue&client=desktop&dj-place=specific_sku&pp=18' '&yandexuid=002'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "product", "id": 2000001},
                    {"entity": "product", "id": 2000002, "offers": {"items": [{"marketSku": "2000002001"}]}},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )
        response = self.report.request_json(
            'place=dj&rgb=blue&client=desktop&dj-place=specific_sku&pp=18' '&yandexuid=003'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "product", "id": 2000001},
                    {"entity": "product", "id": 2000002, "offers": {"items": [{"marketSku": "2000002002"}]}},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )
        response = self.report.request_json(
            'place=dj&rgb=blue&client=desktop&dj-place=specific_sku&pp=18' '&yandexuid=004'
        )
        self.assertFragmentIn(
            response,
            {"results": [{"entity": "product", "id": 2000001}]},
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_specific_sku_titles(cls):
        cls.dj.on_request(exp='specific_sku_titles', yandexuid='001').respond(
            models=[DjModel(id=2000101, title='model1', sku="2000101001"), DjModel(id=2000102, title='model2')]
        )

        cls.index.models += [
            Model(hyperid=2000101, title="Model 1 Title"),
            Model(hyperid=2000102, title="Model 2 Title"),
        ]

        cls.index.mskus += [
            MarketSku(
                hyperid=2000101,
                sku=2000101001,
                blue_offers=[
                    BlueOffer(
                        price=500,
                        vat=Vat.NO_VAT,
                        feedid=12345,
                        title="Model 1 Offer Title",
                        model_title="Model 1 Offer Model Title",
                    ),
                ],
            ),
            MarketSku(
                hyperid=2000102,
                sku=2000102001,
                blue_offers=[
                    BlueOffer(
                        price=500,
                        vat=Vat.NO_VAT,
                        feedid=12345,
                        title="Model 2 Offer Title",
                        model_title="Model 2 Offer Model Title",
                    ),
                ],
            ),
        ]

    # if dj response contains sku we change offer's modelAwareTitle to match offer title
    def test_specific_sku_titles(self):
        response1 = self.report.request_json(
            'place=dj&rgb=blue&client=desktop&dj-place=specific_sku_titles&pp=18' '&yandexuid=001'
        )
        self.assertFragmentIn(
            response1,
            {
                "results": [
                    {
                        "entity": "product",
                        "id": 2000101,
                        "titles": {"raw": "Model 1 Title"},
                        "offers": {
                            "count": 1,
                            "items": [
                                {
                                    "marketSku": "2000101001",
                                    "titles": {"raw": "Model 1 Offer Title"},
                                    "modelAwareTitles": {"raw": "Model 1 Offer Title"},
                                }
                            ],
                        },
                    },
                    {
                        "entity": "product",
                        "id": 2000102,
                        "titles": {"raw": "Model 2 Title"},
                        "offers": {
                            "count": 1,
                            "items": [
                                {
                                    "marketSku": "2000102001",
                                    "titles": {"raw": "Model 2 Offer Title"},
                                    "modelAwareTitles": {"raw": "Model 2 Offer Model Title"},
                                }
                            ],
                        },
                    },
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )
        response2 = self.report.request_json(
            'place=dj&rgb=blue&client=desktop&dj-place=specific_sku_titles&pp=18'
            '&yandexuid=001&rearr-factors=market_dj_no_override_offer_titles=1'
        )
        self.assertFragmentIn(
            response2,
            {
                "results": [
                    {
                        "entity": "product",
                        "id": 2000101,
                        "titles": {"raw": "Model 1 Title"},
                        "offers": {
                            "count": 1,
                            "items": [
                                {
                                    "marketSku": "2000101001",
                                    "titles": {"raw": "Model 1 Offer Title"},
                                    "modelAwareTitles": {"raw": "Model 1 Offer Model Title"},
                                }
                            ],
                        },
                    },
                    {
                        "entity": "product",
                        "id": 2000102,
                        "titles": {"raw": "Model 2 Title"},
                        "offers": {
                            "count": 1,
                            "items": [
                                {
                                    "marketSku": "2000102001",
                                    "titles": {"raw": "Model 2 Offer Title"},
                                    "modelAwareTitles": {"raw": "Model 2 Offer Model Title"},
                                }
                            ],
                        },
                    },
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_dj_factors_on_prime(cls):
        # Нормально потестить возможности нет, факторы хитро сериализуются: добавляются границы, а сами факторы
        # сжимаются алгоритмом хаффмана. Текущее решение - подсунуть реальный ответ, где нужные факторы не пустые
        # Ответ для модели 1414858424 на запрос curl 'http://dj-recommender.tst.vs.market.yandex.net/recommend?add-factors=1&deviceid=&experiment=report_calculate_factors_from_input_profiles&gaid=&hyperid=1414858424&max_count=64&max_count_per_categ=64&msid=&puid=925853942&uuid=&yandexuid=4838333671612265064'  # noqa
        factor_str = "AVAAAABhbGxbMDsxOTMpIG1hcmtldF9kal9jYW5kaWRhdGVfZ2VuZXJhdG9yc1swOzU1KSBtYXJrZXRfZGpfcmVwb3J0X2ZhY3RvcnNbNTU7MTkzKegBAAD///////9/c55hTRBz6zuEELODZoIQs4NmghDTJLz5AOVda5Lrh8zqjrZSV21hFEJ+o2oRV0ih/Eo5VrIo70y2VQPBqH7yLfHNjo11cXam3G7sA3LFLLOEBC4yqf50DzC/TKQAaFm/qnPUh+XX3FnaKaB5NnZXgDnTWVuASd7wMsg3h6leBTEF34QHMQPPLwoxAQC9BjFx/hoIMUekdgQx6fVTCDEBIEsLIUiNiwkAeE5Iy0Z1ryPVVkMVwDZOWlW0Roiibvr+aSJgdqo1H2CCoRYsYDoZqBPw30QPRnWgRP7Y/G/GqTETAI6vT+77FZoAAMQHvAkAAEiICQAASIgp81dMgHkFzEwAwYzlzUdZfwJgEmU0hzkeCzA54wUQMZkHRQ0wD11CDTB7CRIIMa1nzQoxNejPCjE1jIYNMclEuw4x/wGZDjFjI6oPMdur7Q4xl3lCDTAVZDINkL/nhNTlOFJnnG+zueaUANPtZOIQUws62hBz84HvENMf/MMAk3SX4gBzW8rSAGJdVJpZWv0EmLz8ZgWYk0LEBpiN2xSHmAVltQaYKaRohphC8qkHmB0bHQeY7Kq+BpgD6JGCmGRyWISYUMduhJgaHDGGmOykb4eYHvQbCJjGFcAHmP///8QAAAAAAAAAAA=="  # noqa
        # этот запрос сматчится если к нам придет какой-то body но мы его проигнорируем по факту
        cls.dj.on_request(exp="calc_factors_exp", yandexuid="42136", post_body=True).respond(
            [DjModel(id="42136", title='model#42136', factors=factor_str)]
        )

        # Ответ для модели 612787165 на запрос curl 'http://dj-recommender.tst.vs.market.yandex.net/recommend?add-factors=1&deviceid=&experiment=report_calculate_factors_from_input_profiles&gaid=&hyperid=612787165&max_count=64&max_count_per_categ=64&msid=&puid=925853942&uuid=&yandexuid=4838333671612265064'  # noqa
        factor_str = "AVAAAABhbGxbMDsxOTMpIG1hcmtldF9kal9jYW5kaWRhdGVfZ2VuZXJhdG9yc1swOzU1KSBtYXJrZXRfZGpfcmVwb3J0X2ZhY3RvcnNbNTU7MTkzKRMCAAD///////9/E0QCYBBTtgyWEHPxDZQQc/ENlBAzT9o7AWmi7S3d6fskh0lXKXqWJr1yCEs76jQJSbEqYYtf5KkmixyqJixtZ/d4/SJH4sgVk9S0PwLTAdYagCanViUApsKEcgImeP8KAC1ehqKObt33mm9i/QRI4VoS05TL6gBTV7TjAJPOhcMAEwAAzADrx0e9YrEoRm0E2aHSC5dsc6k7UBBzm9hwEHK+AJsA4AkFmJp7H4SYYWhUgpi6jkCEmACEP4WYkOkXBRAA3LcJi/gHgOycgNe8YUO5HYMYaTa7SQ8PuoCpla6agHl0Zc6A6WSgTsB/Ez0Y1YES+WPzvxmnxkwAOL4+ue9XaAIAEB+ghC0h5u/wEwB2z6lBxwQAkDzABAAAJMQEAAAkxIToqSfAJPnxJwDn2DWTQ8SjAEy0rsxhjscCzMIo/UPMJapTA8xDl1ADTI70A0JMpISyQsxscrZCTKbNY0PMMiqiQ8x/QKZDTFk15ENMIoylQ8yXfFEDTAWZTAP0wWAXFmIp3PrXzTzkzAJMJml1Q0wt6GhDzKkor0PMpPEOA0zt3YMDzG0pSwPoZS9iLhM8EmBipZkTYF7aKBlgsjouG2IWlNUaYOo0xRRiSg+3HmCO3QgcYLKr+hpgCkJNDWIOMakTYjpDPRRi6px7GmLiVBMdYnrQbyBggvZVH2D+//8TAwAAAAAAAAA="  # noqa

        cls.dj.on_request(
            exp="report_calculate_factors_from_input_profiles", yandexuid="42136", post_body=True
        ).respond([DjModel(id="42136", title='model#42136', factors=factor_str)])

        profile = TEcomVersionedDjUserProfile(
            ProfileData=TVersionedProfileData(
                FashionV1=TFashionDataV1(
                    SizeClothesFemale=TFashionSizeDataV1(
                        Sizes={"46": 0.288602501, "48": 0.355698764, "S": 0.395698764, "M": 0.288602501}
                    ),
                    SizeClothesMale=TFashionSizeDataV1(
                        Sizes={"46": 0.188602501, "48": 0.155698764, "L": 0.155698764, "M": 0.198602501}
                    ),
                )
            ),
            DjProfiles=b'1234567890',  # некоторые бинарные данные которые мы будем передавать (10 bytes)
        )

        cls.bigb.on_request(yandexuid=42136, client='merch-machine').respond(keywords=DEFAULT_PROFILE)
        cls.dj.on_request(yandexuid='42136', exp='fetch_user_profile_versioned').respond(
            profile_data=profile.SerializeToString(), is_binary_data=True
        )

        cls.index.models += [
            Model(hyperid=42136 + 0, title="телефон 1"),
        ]

    def test_dj_factors_on_prime_with_forwarded_profiles(self):
        """
        Проверяем что передаются bigB профиль и dj_profiles
        https://st.yandex-team.ru/MARKETOUT-42136
        Проверяем, что получили, распарсили и правильно записали факторы из DJ
        """

        response = self.report.request_json(
            'place=prime&text=телефон&yandexuid=42136&rids=213&allow-collapsing=1&debug=da'
            '&rearr-factors=fetch_recom_profile_for_prime=1;market_dj_prime_factors_enabled=1;'
            'market_dj_prime_factors_compress_body=0;market_send_bigb_to_dj_calculate_factors=1;market_dj_prime_factors_exp=calc_factors_exp'
        )
        self.assertFragmentIn(response, 'Add DjProfiles to dj request (10 bytes)')
        self.assertFragmentIn(response, 'Add BigBProfile to dj request')  # market_send_bigb_to_dj_calculate_factors=1

        self.assertFragmentIn(
            response,
            {
                'debug': {
                    'factors': {
                        "DJ_BENEFIT_RAW_WITH_NEG": Round(-0.05738539621),
                        "DJ_BENEFIT_WITH_NEG": Round(-0.632314384),
                        "DJ_BENEFIT_WITH_NEG_PROMO": Round(-0.5750139356),
                        "DJ_BENEFIT_WITH_NEG_PROMO_WITH_SHIFT": Round(-0.5750139356),
                        "DJ_CAT_RW_WEIGHT_AVG": Round(0.01629590057),
                        "DJ_CAT_RW_WEIGHT_COUNT": Round(98),
                        "DJ_CAT_RW_WEIGHT_MAX": Round(0.530636251),
                        "DJ_CAT_RW_WEIGHT_SUM": Round(1.596998334),
                        "DJ_C_F_S2_FULL_MODEL_VECTOR_FULL_USER_VECTOR_COS": Round(-0.0856108889),
                        "DJ_C_F_S2_FULL_MODEL_VECTOR_FULL_USER_VECTOR_DOT_PRODUCT_RIGHT": Round(-0.3797569871),
                        "DJ_C_F_S2_FULL_SCORE": Round(-2.373504877),
                        "DJ_C_F_S2_MODEL_BIAS_VALUE": Round(-0.2172851563),
                        "DJ_C_F_S2_MODEL_VECTOR_MU_MODEL_VECTOR_COS": Round(-0.5527224541),
                        "DJ_C_F_S2_MODEL_VECTOR_USER_VECTOR_COS": Round(-0.04573262855),
                        "DJ_C_F_S2_MODEL_VECTOR_USER_VECTOR_DOT_PRODUCT": Round(-0.6639854908),
                        "DJ_C_F_S3_FULL_BIAS": Round(-5.173828125),
                        "DJ_C_F_S3_FULL_SCORE": Round(0.1701245308),
                        "DJ_C_F_S3_MODEL_BIAS_VALUE": Round(-1.80859375),
                        "DJ_DJ_PYTORCH_DSSM_V6_FPS_POSITIVE_BIAS": Round(96.04252625),
                        "DJ_DJ_PYTORCH_DSSM_V6_FPS_POSITIVE_COS": Round(-0.07124330848),
                        "DJ_DJ_PYTORCH_DSSM_V6_FPS_POSITIVE_DOT": Round(-11.06936073),
                        "DJ_DJ_PYTORCH_DSSM_V6_FPS_POSITIVE_LEFT_BIAS": Round(-59.91105652),
                        "DJ_DJ_PYTORCH_DSSM_V6_FPS_POSITIVE_LEFT_DOT": Round(-0.8454954624),
                        "DJ_DJ_PYTORCH_DSSM_V6_FPS_POSITIVE_LEFT_NORM": Round(39.27647781),
                        "DJ_DJ_PYTORCH_DSSM_V6_FPS_POSITIVE_RIGHT_BIAS": Round(155.9535828),
                        "DJ_DJ_PYTORCH_DSSM_V6_FPS_POSITIVE_RIGHT_DOT": Round(-0.9327287674),
                        "DJ_DJ_PYTORCH_DSSM_V6_FPS_POSITIVE_RIGHT_NORM": Round(23.83345795),
                        "DJ_DJ_PYTORCH_DSSM_V6_FPS_POSITIVE_SCORE": Round(84.97315979),
                        "DJ_DJ_PYTORCH_DSSM_V6_LOGIT_CART_ADDITION_BIAS": Round(-63.01523972),
                        "DJ_DJ_PYTORCH_DSSM_V6_LOGIT_CART_ADDITION_COS": Round(0.0162237864),
                        "DJ_DJ_PYTORCH_DSSM_V6_LOGIT_CART_ADDITION_DOT": Round(1.30632937),
                        "DJ_DJ_PYTORCH_DSSM_V6_LOGIT_CART_ADDITION_LEFT_BIAS": Round(-36.78850555),
                        "DJ_DJ_PYTORCH_DSSM_V6_LOGIT_CART_ADDITION_LEFT_DOT": Round(0.1944455206),
                        "DJ_DJ_PYTORCH_DSSM_V6_LOGIT_CART_ADDITION_LEFT_NORM": Round(20.15468407),
                        "DJ_DJ_PYTORCH_DSSM_V6_LOGIT_CART_ADDITION_RIGHT_BIAS": Round(-26.22673416),
                        "DJ_DJ_PYTORCH_DSSM_V6_LOGIT_CART_ADDITION_RIGHT_DOT": Round(0.1089950651),
                        "DJ_DJ_PYTORCH_DSSM_V6_LOGIT_CART_ADDITION_RIGHT_NORM": Round(19.14943314),
                        "DJ_DJ_PYTORCH_DSSM_V6_LOGIT_CART_ADDITION_SCORE": Round(-61.7089119),
                        "DJ_DJ_PYTORCH_DSSM_V6_LOGIT_IMPLICIT_BIAS": Round(-14.54007816),
                        "DJ_DJ_PYTORCH_DSSM_V6_LOGIT_IMPLICIT_COS": Round(0.2911937237),
                        "DJ_DJ_PYTORCH_DSSM_V6_LOGIT_IMPLICIT_DOT": Round(24.53250694),
                        "DJ_DJ_PYTORCH_DSSM_V6_LOGIT_IMPLICIT_LEFT_BIAS": Round(-37.21440506),
                        "DJ_DJ_PYTORCH_DSSM_V6_LOGIT_IMPLICIT_LEFT_DOT": Round(1.979319692),
                        "DJ_DJ_PYTORCH_DSSM_V6_LOGIT_IMPLICIT_LEFT_NORM": Round(37.1832428),
                        "DJ_DJ_PYTORCH_DSSM_V6_LOGIT_IMPLICIT_RIGHT_BIAS": Round(22.67432594),
                        "DJ_DJ_PYTORCH_DSSM_V6_LOGIT_IMPLICIT_RIGHT_DOT": Round(3.609175682),
                        "DJ_DJ_PYTORCH_DSSM_V6_LOGIT_IMPLICIT_RIGHT_NORM": Round(18.79045677),
                        "DJ_DJ_PYTORCH_DSSM_V6_LOGIT_IMPLICIT_SCORE": Round(9.992429733),
                        "DJ_DJ_PYTORCH_DSSM_V6_LOGIT_INTERNAL_ORDER_BIAS": Round(-106.5345535),
                        "DJ_DJ_PYTORCH_DSSM_V6_LOGIT_INTERNAL_ORDER_COS": Round(-0.5352285504),
                        "DJ_DJ_PYTORCH_DSSM_V6_LOGIT_INTERNAL_ORDER_DOT": Round(-24.40922928),
                        "DJ_DJ_PYTORCH_DSSM_V6_LOGIT_INTERNAL_ORDER_LEFT_BIAS": Round(-55.40858459),
                        "DJ_DJ_PYTORCH_DSSM_V6_LOGIT_INTERNAL_ORDER_LEFT_DOT": Round(-3.604726315),
                        "DJ_DJ_PYTORCH_DSSM_V6_LOGIT_INTERNAL_ORDER_LEFT_NORM": Round(20.31435394),
                        "DJ_DJ_PYTORCH_DSSM_V6_LOGIT_INTERNAL_ORDER_RIGHT_BIAS": Round(-51.12597275),
                        "DJ_DJ_PYTORCH_DSSM_V6_LOGIT_INTERNAL_ORDER_RIGHT_DOT": Round(-3.624273777),
                        "DJ_DJ_PYTORCH_DSSM_V6_LOGIT_INTERNAL_ORDER_RIGHT_NORM": Round(20.14771843),
                        "DJ_DJ_PYTORCH_DSSM_V6_LOGIT_INTERNAL_ORDER_SCORE": Round(-130.9437866),
                        "DJ_DOCUMENT_ADD_TO_FAVORITE_COUNT7D": Round(102.939743),
                        "DJ_DOCUMENT_CART_ADDITION2_PREVIEW7D": Round(0.05015157163),
                        "DJ_DOCUMENT_CTR_ATTRACTIVE_MODELS_MARKET_COUNT7D": Round(0.02496715449),
                        "DJ_DOCUMENT_CTR_BERU_COUNT180D": Round(0.04184811562),
                        "DJ_DOCUMENT_CTR_BERU_COUNT7D": Round(0.04314295575),
                        "DJ_DOCUMENT_CTR_MARKET_COUNT180D": Round(0.03807264566),
                        "DJ_DOCUMENT_CTR_MARKET_COUNT7D": Round(0.03656420857),
                        "DJ_DOCUMENT_CTR_MODEL_PICTURE_THEMATIC_CATEGORIES_BERU_COUNT180D": Round(0.01830733195),
                        "DJ_DOCUMENT_CTR_MODEL_PICTURE_THEMATIC_CATEGORIES_BERU_COUNT7D": Round(0.01698560081),
                        "DJ_DOCUMENT_CTR_THEMATIC_GOODS_BERU_COUNT180D": Round(0.0267517399),
                        "DJ_DOCUMENT_GENERAL_CTR_COUNT180D": Round(0.04834757),
                        "DJ_DOCUMENT_GENERAL_CTR_COUNT7D": Round(0.04967939481),
                        "DJ_DOCUMENT_INTERNAL_ORDER2_CART_ADDITION180D": Round(0.2309205383),
                        "DJ_DOCUMENT_INTERNAL_ORDER2_CART_ADDITION7D": Round(0.2406671792),
                        "DJ_DOCUMENT_INTERNAL_ORDER2_CLICK_THEMATIC_CATEGORIES_BERU7D": Round(5823929),
                        "DJ_DOCUMENT_INTERNAL_ORDER2_PREVIEW180D": Round(0.0187254753),
                        "DJ_DOCUMENT_INTERNAL_ORDER2_PREVIEW7D": Round(0.01206983626),
                        "DJ_DOCUMENT_INTERNAL_ORDER_COUNT7D": Round(93.24868774),
                        "DJ_DOCUMENT_INTERNAL_PURCHASES_TO_VIEWS180D": Round(0.0004069780698),
                        "DJ_DOCUMENT_INTERNAL_PURCHASE_CTR180D": Round(0.003206632799),
                        "DJ_DOCUMENT_NEW_CART_ADDITION_FILTER_MODEL_HISTORY_DIV_ALL_INTERNAL_ORDERS180D": Round(
                            0.09334610403
                        ),
                        "DJ_DOCUMENT_NEW_CLICK_FILTER_MODEL_HISTORY_DIV_ALL_INTERNAL_ORDERS7D": Round(2.926466703),
                        "DJ_DOCUMENT_PRICE_GRADE30D_CATEGORY": Round(1.542944193),
                        "DJ_DOCUMENT_PRICE_GRADE7D_CATEGORY": Round(1.599619746),
                        "DJ_DOCUMENT_PRICE_GRADE7D_VENDOR_CATEGORY": Round(0.4741697311),
                        "DJ_DOCUMENT_PRICE_GRADE_CATEGORY": Round(1.989824176),
                        "DJ_DOCUMENT_PRICE_GRADE_VENDOR_CATEGORY": Round(0.7218344212),
                        "DJ_DOCUMENT_TREND_HEURISTIC_LINE_COEF_SCORE": Round(5.675439358),
                        "DJ_DOCUMENT_TREND_HEURISTIC_LIN_REG_SCORE": Round(3.865442038),
                        "DJ_DOCUMENT_TREND_HEURISTIC_WEEK_RATIO_SCORE": Round(-0.170951426),
                        "DJ_DSSM_SPYNET3_FPS_S_CLICK_SPYNET_LR000075_DELAY4_B_S_D_E_V65865": Round(0.01973505691),
                        "DJ_DSSM_SPYNET_ARG_REGIONS_CLICK_HARD": Round(0.001886976417),
                        "DJ_DSSM_SPYNET_FPS": Round(0.05901145935),
                        "DJ_DSSM_SPYNET_QUERIES_FPS": Round(0.02112833783),
                        "DJ_PRODUCT_FRONT_EVENTS_TOTAL_CART_ADDITION_MAX30_D": Round(0.9946675301),
                        "DJ_PRODUCT_FRONT_EVENTS_TOTAL_COMPARISON_ADDITION_SUM180_D": Round(308.9979858),
                        "DJ_PRODUCT_FRONT_EVENTS_TOTAL_COMPARISON_ADDITION_SUM7_D": Round(102.7082291),
                        "DJ_PRODUCT_FRONT_EVENTS_TOTAL_VIEW_SUM7_D": Round(8554.09375),
                        "DJ_USER_CATEGORY_BERU_MODEL_VIEW_TIME_DIFF": Round(1382461),
                        "DJ_USER_CATEGORY_FRONT_EVENTS_UNKNOWN_BLOCK_VIEW_SUM7_D": Round(1.828317404),
                        "DJ_USER_CATEGORY_MODEL_VIEW_FRC180D": Round(0.01893605292),
                        "DJ_USER_VIEWS_IN_CATEGORY_AVG_PRICE_GRADE30D_CATEGORY": Round(-1),
                        "DJ_USER_VIEWS_IN_CATEGORY_AVG_PRICE_GRADE7D_CATEGORY": Round(-1),
                    }
                }
            },
        )

        # по умолчанию market_dj_prime_factors_exp=report_calculate_factors_from_input_profiles
        # для той же модели получаем другие факторы
        response = self.report.request_json(
            'place=prime&text=телефон&yandexuid=42136&rids=213&allow-collapsing=1&debug=da'
            '&rearr-factors=fetch_recom_profile_for_prime=1;market_dj_prime_factors_enabled=1;'
            'market_dj_prime_factors_compress_body=0;market_send_bigb_to_dj_calculate_factors=1'
        )
        self.assertFragmentIn(response, 'Add DjProfiles to dj request (10 bytes)')
        self.assertFragmentIn(response, 'Add BigBProfile to dj request')  # market_send_bigb_to_dj_calculate_factors=1
        self.assertFragmentIn(
            response,
            {
                'debug': {
                    'factors': {
                        "DJ_C_F_S2_FULL_MODEL_VECTOR_FULL_USER_VECTOR_COS": Round(-0.06341036409),
                        "DJ_DOCUMENT_INTERNAL_ORDER_COUNT7D": Round(1616.594971),
                    }
                }
            },
        )

    def test_promo_parameters(self):
        """
        https://st.yandex-team.ru/MARKETRECOM-4218
        Проверяем, что в DJ пробрасываются параметры, связанные с акциями
        """
        request = 'place=dj&djid=dummy&shop-promo-id=one,two,three&parentPromoId=four,five,six&debug=da&pp=18'
        response = self.report.request_json(request)

        self.error_log.ignore(code=3787)

        self.assertFragmentIn(response, {"logicTrace": [Contains('Set param for DJ: promo-id=one,two,three')]})
        self.assertFragmentIn(response, {"logicTrace": [Contains('Set param for DJ: parent-promo-id=four')]})

    def test_boost_promo_parameters(self):
        """
        https://st.yandex-team.ru/MARKETOUT-46478
        Проверяем, что в DJ пробрасываются параметры, связанные с бустингом товаров с акциями
        """
        request = 'place=dj&djid=dummy&boostParentPromoId=four,five,six&debug=da&pp=18'
        response = self.report.request_json(request)

        self.error_log.ignore(code=3787)

        self.assertFragmentIn(response, {"logicTrace": [Contains('Set param for DJ: boost-parent-promo-id=four')]})

    def test_warehouse_id_parameter(self):
        """
        Проверяем, что в DJ пробрасывается параметр warehouse_id
        """
        request = 'place=dj&djid=dummy&warehouse_id=147&debug=da'
        response = self.report.request_json(request)
        self.error_log.ignore(code=3787)
        self.assertFragmentIn(response, {"logicTrace": [Contains('Set param for DJ: warehouse-id=147')]})

    def test_supplier_id_parameter(self):
        """
        Проверяем, что в DJ пробрасывается параметр supplier-id
        """
        request = 'place=dj&djid=dummy&supplier-id=1,2&debug=da'
        response = self.report.request_json(request)
        self.error_log.ignore(code=3787)
        self.assertFragmentIn(response, {"logicTrace": [Contains('Set param for DJ: supplier-id=1,2')]})

    def test_visual_similar_count_parameter(self):
        """
        Проверяем, что в DJ пробрасывается параметр visual-similar-count
        """
        request = 'place=dj&djid=dummy&visual-similar-count=4&debug=da'
        response = self.report.request_json(request)
        self.error_log.ignore(code=3787)
        self.assertFragmentIn(response, {"logicTrace": [Contains('Set param for DJ: visual-similar-count=4')]})

    @classmethod
    def prepare_size_parameter(cls):
        cls.index.hypertree += [HyperCategory(822)]
        cls.index.navtree += [
            NavCategory(nid=822, hid=822),
        ]
        cls.index.gltypes += [
            GLType(param_id=26417130, hid=822, gltype=GLType.ENUM, values=list(range(27014300, 27014400))),
            GLType(param_id=7893318, hid=822, gltype=GLType.ENUM, values=[8340189, 17786883, 12256233]),
        ]

    def test_clothes_size_filters_parameter(self):
        """
        https://st.yandex-team.ru/MARKETYA-822
        Проверяем, что в DJ пробрасываются параметр размера одежды из glfilter c id=26417130
        """
        request = 'place=dj&djid=dummy&debug=da&nid=822&hid=822&glfilter=26417130:27014310,27014370'
        response = self.report.request_json(request)
        self.error_log.ignore(code=3787)
        # не указываем значения фильтров из-за возможной перестановки
        self.assertFragmentIn(response, {"logicTrace": [Contains('Set param for DJ: clothes-size-filters=')]})

    def test_vendor_filters_parameter(self):
        """
        https://st.yandex-team.ru/MARKETYA-822
        Проверяем, что в DJ пробрасываются параметр размера одежды из glfilter c id=7893318
        """
        request = 'place=dj&djid=dummy&debug=da&nid=822&hid=822&glfilter=7893318:8340189,17786883,12256233'
        response = self.report.request_json(request)
        self.error_log.ignore(code=3787)
        # не указываем значения фильтров из-за возможной перестановки
        self.assertFragmentIn(response, {"logicTrace": [Contains('Set param for DJ: vendor-filters=')]})

    @classmethod
    def prepare_category_like(cls):
        cls.dj.on_request(yandexuid='1010').respond(
            models=[DjModel(id="123")],
            title='Выдача с лайками для place=dj',
            djid=None,
            recommended_queries=None,
            items=None,
            liked=True,
        )
        cls.dj.on_request(yandexuid='10100').respond(
            models=[DjModel(id="123")],
            title='Выдача без лайков для place=dj',
            djid=None,
            recommended_queries=None,
            items=None,
            liked=False,
        )

    def test_category_like(self):
        response = self.report.request_json(
            'place=dj&dj-place=thematics_product_block&rgb=blue&yandexuid=1010&allow-collapsing=true&numdoc=1'
        )
        self.assertFragmentIn(
            response, {"search": {"title": "Выдача с лайками для place=dj", "recomParams": {"liked": True}}}
        )
        response = self.report.request_json(
            'place=dj&dj-place=thematics_product_block&rgb=blue&yandexuid=10100&allow-collapsing=true&numdoc=1'
        )
        self.assertFragmentIn(
            response, {"search": {"title": "Выдача без лайков для place=dj", "recomParams": Absent()}}
        )

    @classmethod
    def prepare_pass_nid(cls):
        cls.index.navtree += [
            NavCategory(nid=202110311225, hid=311225),
            NavCategory(nid=202110311226, hid=311226),
        ]

        cls.index.hypertree += [
            HyperCategory(hid=311225, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=311226, output_type=HyperCategoryType.GURU),
        ]

        cls.index.models += [
            Model(hyperid=3371245, title="model#3371245", hid=311226),
            Model(hyperid=3371244, title="model#3371244", hid=311225),
        ]
        cls.index.mskus += [
            MarketSku(
                hyperid=3371245,
                sku=337124501,
                blue_offers=[
                    BlueOffer(
                        price=500,
                        vat=Vat.NO_VAT,
                        feedid=12345,
                    ),
                ],
            ),
            MarketSku(
                hyperid=3371244,
                sku=337124401,
                blue_offers=[
                    BlueOffer(
                        price=500,
                        vat=Vat.NO_VAT,
                        feedid=12345,
                    ),
                ],
            ),
        ]

        cls.dj.on_request(exp="test_pass_nid", yandexuid="1226", nid='202110311225,202110311226').respond(
            [
                DjModel(id="3371245", title='model#3371245'),
                DjModel(id="3371244", title='model#3371244'),
            ]
        )

    def test_pass_nid(self):
        """
        Проверяем, что в DJ пробрасывается параметр nid
        """
        request = (
            'place=dj&djid=retargeting_block&rearr-factors=dj_exp_retargeting_block=test_pass_nid&'
            'nid=202110311225,202110311226&debug=da&yandexuid=1226'
        )
        response = self.report.request_json(request)
        self.error_log.not_expect(code=3787)
        self.assertFragmentIn(response, {"logicTrace": [Contains('Set param for DJ: nid=202110311225,202110311226')]})

    @classmethod
    def prepare_pass_i2i_params(cls):
        cls.dj.on_request(exp="blue_attractive_models", yandexuid="1645").respond([DjModel(id="1", title='model#1')])

    def test_pass_i2i_params(self):
        """
        Проверяем, что в DJ пробрасываются i2i параметры
        https://st.yandex-team.ru/MARKETRECOM-4418
        """

        response = self.report.request_json(
            'place=blue_attractive_models&rgb=blue&debug=1&yandexuid=1645&i2i-folder=games&i2i-version=v1&'
            'rearr-factors=market_dj_exp_for_blue_attractive_models=blue_attractive_models'
        )
        self.error_log.not_expect(code=3787)
        self.assertFragmentIn(response, {"logicTrace": [Contains('Set param for DJ: i2i-folder=games')]})
        self.assertFragmentIn(response, {"logicTrace": [Contains('Set param for DJ: i2i-version=v1')]})

    @classmethod
    def prepare_dj_match_warehouses(cls):
        cls.dj.on_request(exp="cart_complementary2product", yandexuid="16455").respond(
            [
                DjModel(id="331411", title='model#331411'),
                DjModel(id="331412", title='model#331412'),
                DjModel(id="331413", title='model#331413'),
                DjModel(id="331414", title='model#331414'),
            ]
        )

        cls.index.models += [
            Model(hyperid=331411),
            Model(hyperid=331412),
            Model(hyperid=331413),
            Model(hyperid=331414),
        ]

        cls.index.shops += [
            Shop(
                fesh=700011,
                datafeed_id=700011,
                priority_region=213,
                regions=[213],
                name="Магазин раз",
                currency=Currency.RUR,
                blue=Shop.BLUE_REAL,
                warehouse_id=1477,
            ),
            Shop(
                fesh=700022,
                datafeed_id=700022,
                priority_region=213,
                regions=[213],
                name="Магазин два",
                currency=Currency.RUR,
                blue=Shop.BLUE_REAL,
                warehouse_id=1455,
            ),
            Shop(
                fesh=700033,
                datafeed_id=700033,
                priority_region=213,
                regions=[213],
                name="Магазин три",
                currency=Currency.RUR,
                blue=Shop.BLUE_REAL,
                warehouse_id=1455,
            ),
        ]

        cls.index.warehouse_priorities += [
            WarehousesPriorityInRegion(
                regions=[213, 225],
                warehouse_with_priority=[
                    WarehouseWithPriority(
                        100, 1
                    ),  # необходим хоть какой-то приоретет, чтобы связки вообще создались. Для всех остальных будет выставлен минимальный приоритет
                ],
            )
        ]

        sku_offer_1_1 = BlueOffer(
            model_title='Key model #1', price=1000, feedid=700011, waremd5='Fyxz9YsFmwmoMUnjyIq_tQ'
        )
        sku_offer_1_2_0 = BlueOffer(
            model_title='Key model #2.1', price=1100, feedid=700011, waremd5='Q9kNdXeoDnHVgcOV8deZRA'
        )
        # этот должен быть выбран в качестве дефолтного офера (ниже цена), но из-за dj-match-warehouse будет выбран тот, что выше
        sku_offer_1_2_1 = BlueOffer(
            model_title='Key model #2.2', price=1000, feedid=700033, waremd5='Nz7aFAwekuzQztSv5bbwLg'
        )
        sku_offer_1_3 = BlueOffer(
            model_title='Key model #3', price=1100, feedid=700011, waremd5='zuAHa0PEA13edLO06p26NQ'
        )
        sku_offer_1_4 = BlueOffer(
            model_title='Key model #4', price=1100, feedid=700022, waremd5='xIkyZdgco9x9vkv32JAU6g'
        )

        cls.index.mskus += [
            MarketSku(hyperid=331411, sku=100111, blue_offers=[sku_offer_1_1]),
            MarketSku(hyperid=331412, sku=100122, blue_offers=[sku_offer_1_2_0, sku_offer_1_2_1]),
            MarketSku(hyperid=331413, sku=100133, blue_offers=[sku_offer_1_3]),
            MarketSku(hyperid=331414, sku=100144, blue_offers=[sku_offer_1_4]),
        ]

    def test_dj_match_warehouses(self):

        response = self.report.request_json(
            'place=dj&djid=cart_complementary2product&pp=18&yandexuid=16455&dj-match-warehouse=1&cart=Fyxz9YsFmwmoMUnjyIq_tQ&debug=1'
        )

        self.assertFragmentIn(response, {"logicTrace": [Contains('Set param for DJ: match-warehouse=1')]})

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "product", "id": 331411, "offers": {"items": [{"supplier": {"id": 700011}}]}},
                    {
                        "entity": "product",
                        "id": 331412,
                        "offers": {"items": [{"wareId": "Q9kNdXeoDnHVgcOV8deZRA", "supplier": {"id": 700011}}]},
                    },
                    {"entity": "product", "id": 331413, "offers": {"items": [{"supplier": {"id": 700011}}]}},
                ]
            },
        )

        self.assertFragmentNotIn(
            response,
            {
                "results": [
                    {"entity": "product", "id": 331414, "offers": [{"supplier": {"id": 700022}}]},
                ]
            },
        )

    def test_stats_policy_selection(self):
        for cgi_param, rearr_param, expected_policy in [
            ['', 'none', 'none'],
            ['', 'null_nil_none', 'dyn_model_stat'],
            ['', 'default_offers', 'default_offers'],
            ['', 'mrs_only', 'mrs_only'],
            ['dyn_model_stat', 'default_offers', 'dyn_model_stat'],
            ['default_offers', 'default_offers', 'default_offers'],
            ['mrs_only', 'default_offers', 'mrs_only'],
        ]:
            request = 'place=dj&djid=unknown&yandexuid=777&debug=1'
            if cgi_param:
                request += '&dj-stats-source-policy=' + cgi_param
            if rearr_param:
                request += '&rearr-factors=recom_dj_base_default_model_stats_policy=' + rearr_param
            response = self.report.request_json(request)
            self.error_log.ignore(code=3787)
            self.assertFragmentIn(
                response, {'logicTrace': [Contains('Selected stats source policy: {}'.format(expected_policy))]}
            )

            debug_message_models_statistics = {'metasearch': {'name': 'models real time statistics'}}
            if expected_policy == 'dyn_model_stat':
                self.assertFragmentIn(response, debug_message_models_statistics)
            else:
                self.assertFragmentNotIn(response, debug_message_models_statistics)

    @classmethod
    def prepare_dyn_model_stat_prune_and_tbs(cls):
        cls.dj.on_request(exp="dyn_model_stat_prune_and_tbs", yandexuid="777").respond([DjModel(id=str(MODELS[0]))])

    def test_dyn_model_stat_prune_and_tbs(self):
        """
        Проверяем, что в подзапросе models_statistics будут применены значения прюнинга и tbs-value
        из флагов recom_model_stats_prune_count и recom_model_stats_tbs_value
        """

        request = (
            'place=dj&djid=dyn_model_stat_prune_and_tbs&yandexuid=777&debug=1&dj-stats-source-policy=dyn_model_stat'
        )

        # Проверяем, что без флагов прюнинга нет
        response = self.report.request_json(request)
        self.assertFragmentNotIn(
            response,
            {
                'metasearch': {
                    'name': 'models real time statistics',
                    'subrequests': [
                        'debug',
                        {'report': {'context': {'collections': {'SHOP': {'pron': ['prune']}}}}},
                    ],
                }
            },
        )

        request_with_pruning = (
            request
            + '&rearr-factors=recom_model_stats_prune_count=300'
            + '&rearr-factors=recom_model_stats_tbs_value=17000'
            + '&rearr-factors=market_model_statistics_prun_count=500'
            + '&rearr-factors=market_model_statistics_tbs_count=23000'
        )

        response = self.report.request_json(request_with_pruning)
        self.assertFragmentIn(
            response,
            {
                'metasearch': {
                    'name': 'models real time statistics',
                    'subrequests': [
                        'debug',
                        {
                            'report': {
                                'context': {'collections': {'*': {'pron': ['tbs17000', 'prune', 'pruncount200']}}}
                            }
                        },
                    ],
                }
            },
        )

    @classmethod
    def prepare_do_prune_and_tbs(cls):
        cls.dj.on_request(exp="do_prune_and_tbs", yandexuid="777").respond([DjModel(id=str(MODELS[0]))])

    def test_do_prune_and_tbs(self):
        """
        Проверяем, что в подзапросе для ДО будут применены значения прюнинга и tbs-value
        """

        request = 'place=dj&djid=do_prune_and_tbs&yandexuid=777&debug=1'

        request_with_pruning = (
            request + '&rearr-factors=recom_do_prune_count=300' + '&rearr-factors=recom_do_tbs_value=17000'
        )

        response = self.report.request_json(request_with_pruning)
        self.assertFragmentIn(
            response,
            {
                'metasearch': {
                    'name': '',
                    'subrequests': [
                        'debug',
                        {
                            'report': {
                                'context': {'collections': {'*': {'pron': ['tbs17000', 'prune', 'pruncount200']}}}
                            },
                            'metasearch': {'name': 'Default offer'},
                        },
                    ],
                }
            },
        )

    @classmethod
    def prepare_do_via_sku_max_bulk_size_in_request(cls):
        cls.dj.on_request(exp="do_via_sku_max_bulk_size_in_request", yandexuid="777").respond(
            [DjModel(id=model, sku=str(model)) for model in MODELS]
        )

    def test_do_via_sku_max_bulk_size_in_request(self):
        """
        Флаг do_via_sku_max_bulk_size_in_request ограничевает максимальное число СКУ в подзапросе.
        Проверяем, что количество подзапросов за ДО (по СКУ) соответствует ожидаемому числу.
        """

        request = (
            'place=dj&djid=do_via_sku_max_bulk_size_in_request&yandexuid=777&debug=1'
            '&rearr-factors=recom_do_via_sku_max_bulk_size_in_request={}'
        )

        for max_bulk_size in list(range(1, 8)) + [len(MODELS)]:
            response = self.report.request_json(request.format(max_bulk_size))

            expected_do_request_count, rest_items_count = divmod(len(MODELS), max_bulk_size)
            if rest_items_count > 0:
                expected_do_request_count += 1

            self.assertFragmentIn(
                response,
                {
                    'metasearch': {
                        'subrequests': [{'metasearch': {'name': 'Default offer'}}] * expected_do_request_count,
                    }
                },
            )


if __name__ == '__main__':
    main()
