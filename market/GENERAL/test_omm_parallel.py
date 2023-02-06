#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BlueOffer,
    CategoryRestriction,
    Const,
    Currency,
    DeliveryBucket,
    Disclaimer,
    DynamicBlueGenericBundlesPromos,
    HybridAuctionParam,
    HyperCategory,
    HyperCategoryType,
    MarketSku,
    MnPlace,
    Model,
    ModelGroup,
    NavCategory,
    Offer,
    Outlet,
    PickupBucket,
    PickupOption,
    Picture,
    Promo,
    PromoType,
    Region,
    RegionalModel,
    RegionalRestriction,
    Shop,
    Tax,
    Vat,
    YamarecDjDefaultModelsList,
    YamarecPlace,
)
from core.testcase import TestCase, main
from core.bigb import (
    BeruModelOrderLastTimeCounter,
    BigBKeyword,
    MarketModelLastTimeCounter,
    MarketModelViewsCounter,
    ModelLastOrderEvent,
    ModelLastSeenEvent,
    ModelViewEvent,
    Query,
    WeightedValue,
)
from core.matcher import Absent, NoKey, NotEmpty, Wildcard
from core.dj import DjModel
from unittest import skip
from core.types.offer_promo import PromoBlueCashback


DEFAULT_PROFILE = [
    BigBKeyword(
        id=BigBKeyword.GENDER,
        weighted_uint_values=[
            WeightedValue(value=BigBKeyword.GENDER_MALE, weight=621947),
            WeightedValue(value=BigBKeyword.GENDER_FEMALE, weight=375515),
        ],
    ),
]

DEFAULT_PROFILE_WITH_UID = [
    BigBKeyword(
        id=BigBKeyword.GENDER,
        weighted_uint_values=[
            WeightedValue(value=BigBKeyword.GENDER_MALE, weight=621947),
            WeightedValue(value=BigBKeyword.GENDER_FEMALE, weight=375515),
        ],
    ),
]

BLUE_DEPARTMENTS = [
    ["Электроника", "elektronika", 80155, 198119],
    ["Компьютерная техника", "kompiuternaia-tekhnika", 79959, 91009],
    ["Бытовая техника", "bytovaia-tekhnika", 79807, 198118],
    ["Товары для ремонта", "tovary-dlia-remonta", 81803, None],
    ["Стирка и уборка", "stirka-i-uborka", 81039, None],
    ["Товары для дома", "tovary-dlia-doma", 81089, None],
    ["Дача, сезонные товары", "dacha-sezonnye-tovary", 81565, 90719],
    ["Детские товары", "detskie-tovary", 77672, 90764],
    ["Продукты", "produkty", 76022, 91307],
    ["Красота и гигиена", "krasota-i-gigiena", 77088, 90509],
    ["Здоровье", "zdorove", 76942, 8475840],
    ["Товары для животных", "tovary-dlia-zhivotnykh", 77580, 90813],
    ["Спорт и отдых", "sport-i-otdykh", 77279, 91512],
    ["Сувениры", "suveniry", 75787, 16056423],
]

OMM_DEFAULT_MODELS = [
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


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ["market_money_disable_bids=0"]
        # cls.settings.set_default_reqid = False
        cls.settings.default_search_experiment_flags += ['enable_fast_promo_matcher=0;enable_fast_promo_matcher_test=0']
        cls.settings.rgb_blue_is_cpa = True
        # RANDX randomizing is disabled because these tests don't work with it. See MARKETOUT-21319
        cls.disable_randx_randomize()
        cls.index.fixed_index_generation = '20200101_0300'
        cls.settings.report_subrole = 'parallel'

        cls.index.regiontree += [
            Region(rid=54, name='Екатеринбург'),
        ]

        cls.index.shops += [
            Shop(
                fesh=1886710,
                name='БЕРУ',
                datafeed_id=188671001,
                priority_region=213,
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
            ),
            Shop(
                fesh=12345,
                name='Реально синий магазин',
                datafeed_id=12345,
                priority_region=213,
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                blue=Shop.BLUE_REAL,
            ),
        ]

    @classmethod
    def prepare_omm_parallel(cls):
        """Создаем модели с различными картинками и ценами
        Создаем запросы к OMM на 3, 6, 9, 12 и 30 моделей
        """
        dj_models = []
        for seq in range(40):
            picture_link = '//avatars.mds.yandex.net/get-marketpic/2359{}/market_-dRDIVQlD_J6ZSezQ1j{}Q/orig'.format(
                seq, seq
            )

            cls.index.models += [
                Model(
                    hyperid=1786501 + seq,
                    title='Модель для ПП ' + str(seq),
                    hid=1786500,
                    picinfo=picture_link,
                    add_picinfo=picture_link,
                ),
            ]

            cls.index.offers += [
                Offer(hyperid=1786501 + seq, price=100 + seq * 10),
                Offer(hyperid=1786501 + seq, price=2000 + seq * 10),
            ]

            dj_models.append(
                DjModel(
                    id=1786501 + seq,
                    title='Модель для ПП {}'.format(seq),
                    pic_url=picture_link,
                )
            )

        cls.bigb.on_request(yandexuid='003', client='merch-machine').respond(keywords=DEFAULT_PROFILE)
        cls.bigb.on_request(yandexuid='003', client='merch-machine', parallel=True).respond(keywords=DEFAULT_PROFILE)

        cls.dj.on_request(
            exp='inter_vertical_unknown',
            msid=Const.DEFAULT_REQ_ID,
            yandexuid='003',
        ).respond(models=dj_models)

    def get_omm_parallel_model(self, index, clid=None):
        model_id = 1786501 + index
        clid_str = ''
        wprid_str = ''
        if clid:
            clid_str = '?clid={}'.format(clid)
            wprid_str = '&wprid={}'.format(Const.DEFAULT_REQ_ID)
        else:
            wprid_str = '?wprid={}'.format(Const.DEFAULT_REQ_ID)
        slug_str = 'product--model-dlia-pp-{}'.format(index)
        return {
            "entity": "product",
            "titles": {"raw": "Модель для ПП " + str(index)},
            "pictures": [
                {
                    "entity": "picture",
                    "thumbnails": [
                        {
                            "containerWidth": 250,
                            "containerHeight": 250,
                            "url": "//avatars.mds.yandex.net/get-marketpic/2359{}/market_-dRDIVQlD_J6ZSezQ1j{}Q/6hq".format(
                                index, index
                            ),
                            "width": 250,
                            "height": 250,
                        },
                        {
                            "containerWidth": 500,
                            "containerHeight": 500,
                            "url": "//avatars.mds.yandex.net/get-marketpic/2359{}/market_-dRDIVQlD_J6ZSezQ1j{}Q/9hq".format(
                                index, index
                            ),
                            "width": 500,
                            "height": 500,
                        },
                    ],
                }
            ],
            "urls": {"direct": "//m.market.yandex.ru/{}/{}{}{}".format(slug_str, model_id, clid_str, wprid_str)},
            "id": model_id,
            "prices": {
                "min": str(100 + index * 10),
                "max": str(2000 + index * 10),
                "currency": "RUR",
                "avg": str(2000 + index * 10),
            },
        }

    def test_omm_parallel_format(self):
        """
        Проверяем, что на выдаче place=omm_parallel находятся модели,
        рекомендуемые OMM в заданном формате
        """
        for place in ['', '&omm_place=yandexapp_vertical']:
            response = self.report.request_json('place=omm_parallel{}&yandexuid=003&numdoc=3'.format(place))
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            self.get_omm_parallel_model(0),
                            self.get_omm_parallel_model(1),
                            self.get_omm_parallel_model(2),
                        ]
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )

        # initial query for verticals
        pages = [
            "V3HCQqFomKGs2jeFTHCnLKiY2RNAO1FE7J7WmAtXjhblGG3bCDf8OLIr8iSFJKou9XsYZIC9Tgfw4-0-_DZsQ4QEEa0XMr7P5UCCOhwbYzaC8uWArv3GYaeh2Q52tf6B6eQFnYXabzDDUpFsfYmlSDj4lzIQOaUtpOWw4bTKzfBIFUHCRGeLwQ,,",
            "ZBC0NIJ47kZfDCksFFRAB-Ut3gZ_DpJ0w9xo7UcuexxGhvt7DtwvDXFqqfTTYzWAkgQuAUM4riIizA2U3JShFrHH_KQKTkskOMRw374hpWTNlJ7r4JwbK-CmX6HHwXBwNHXFMkwDrCXG0_ZKGYOny2bSUq29jNVakeeTmpp5oudcMjolAyp2VQ,,",
            "fTX1aL7PvpX4FyLRAj89uKPIhn_mgw4Iszzk4ZgVfQHdZsGo_67JIzN9noG5-k6oDuERk11KJ3Itg99Zr1sp-QudHCf-epwZ",
        ]
        for omm_place in ['yandexapp_vertical']:
            response = self.report.request_json('place=omm_parallel&omm_place={}&yandexuid=003'.format(omm_place))
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [self.get_omm_parallel_model(i) for i in range(16)],
                        "departments": [
                            {
                                "title": "Бытовая техника",
                                "url": "//m.market.yandex.ru/catalog--bytovaia-tekhnika/54419?hid=198118",
                            },
                            {"title": "Компьютеры", "url": "//m.market.yandex.ru/catalog--kompiutery/54425?hid=91009"},
                            {
                                "title": "Электроника",
                                "url": "//m.market.yandex.ru/catalog--elektronika/54440?hid=198119",
                            },
                            {
                                "title": "Детские товары",
                                "url": "//m.market.yandex.ru/catalog--detskie-tovary/54421?hid=90764",
                            },
                            {"title": "Дом и дача", "url": "//m.market.yandex.ru/catalog--dom-i-dacha/54422?hid=90666"},
                            {"title": "Авто", "url": "//m.market.yandex.ru/catalog--avto/54418?hid=90402"},
                            {
                                "title": "Одежда, обувь, аксессуары",
                                "url": "//m.market.yandex.ru/catalog--odezhda-obuv-aksessuary/54432?hid=7877999",
                            },
                            {
                                "title": "Спорт и отдых",
                                "url": "//m.market.yandex.ru/catalog--sport-i-otdykh/54436?hid=91512",
                            },
                            {
                                "title": "Товары для красоты",
                                "url": "//m.market.yandex.ru/catalog--tovary-dlia-krasoty/54438?hid=90509",
                            },
                            {
                                "title": "Товары для животных",
                                "url": "//m.market.yandex.ru/catalog--tovary-dlia-zhivotnykh/54496?hid=90813",
                            },
                            {
                                "title": "Товары для здоровья",
                                "url": "//m.market.yandex.ru/catalog--tovary-dlia-zdorovia/54734?hid=8475840",
                            },
                        ],
                        "apiUrl": "https://api.market.yandex.ru/yandex/home/v2.1.5/models/recommended/vertical/app?geo_id=0",
                        "pages": pages,
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )

    def test_omm_parallel_default(self):
        """Проверяем, что на выдаче place=omm_parallel по умолчанию 30 моделей
        в нужном формате
        """
        result_models = []
        for seq in range(16):
            result_models.append(self.get_omm_parallel_model(seq))

        response = self.report.request_json('place=omm_parallel&omm_place=yandexapp_vertical&yandexuid=003')
        self.assertFragmentIn(response, {"results": result_models}, preserve_order=True, allow_different_len=False)

    def _inspect_parallel_unistat(
        self, query, unistat_prefix, is_blue=False, with_blue_omm_data=False, request_default_offers=False
    ):
        # Сохраняем счётчики до запроса
        tass_data = self.report.request_tass()

        # Делаем запрос
        self.report.request_json(query)

        # Сохраняем счётчики после запроса
        tass_data_new = self.report.request_tass()
        # Сравниваем
        # Общее число запросов к нужному плейсу
        self.assertEqual(
            tass_data.get(unistat_prefix + '_request_count_dmmm', 0) + 1,
            tass_data_new.get(unistat_prefix + '_request_count_dmmm', 0),
        )
        self.assertEqual(
            tass_data.get(unistat_prefix + '_empty_dj_response_count_dmmm', 0),
            tass_data_new.get(unistat_prefix + '_empty_dj_response_count_dmmm', 0),
        )
        response_count_increment = 1 if is_blue and not with_blue_omm_data else 0
        self.assertEqual(
            tass_data.get(unistat_prefix + '_empty_omm_parallel_response_count_dmmm', 0) + response_count_increment,
            tass_data_new.get(unistat_prefix + '_empty_omm_parallel_response_count_dmmm', 0),
        )
        # Тайминги как-то считаются
        self.assertIn(unistat_prefix + '_dj_request_time_hgram', tass_data_new.keys())
        self.assertIn(unistat_prefix + '_full_request_time_hgram', tass_data_new.keys())
        self.assertIn(unistat_prefix + '_fill_output_models_request_time_hgram', tass_data_new.keys())
        if is_blue and request_default_offers:
            self.assertIn(unistat_prefix + '_results_enhancement_time_hgram', tass_data_new.keys())
        # Число ошибок не изменилось
        self.assertEqual(
            tass_data.get(unistat_prefix + '_error_count_dmmm', 0),
            tass_data_new.get(unistat_prefix + '_error_count_dmmm', 0),
        )
        # Число показов тыквы не изменилось
        self.assertEqual(
            tass_data.get(unistat_prefix + '_pumpkin_shows_count_dmmm', 0),
            tass_data_new.get(unistat_prefix + '_pumpkin_shows_count_dmmm', 0),
        )

    def _inspect_po_parallel_unistat(self, query, unistat_prefix, await_pumpkin=False):
        # Сохраняем счётчики до запроса
        tass_data = self.report.request_tass()

        # Делаем запрос
        self.report.request_json(query)

        # Сохраняем счётчики после запроса
        tass_data_new = self.report.request_tass()

        # Сравниваем
        # Общее число запросов к нужному плейсу
        self.assertEqual(
            tass_data.get(unistat_prefix + '_po_request_count_dmmm', 0) + 1,
            tass_data_new.get(unistat_prefix + '_po_request_count_dmmm', 0),
        )
        # Тайминги как-то считаются
        self.assertIn(unistat_prefix + '_po_request_time_hgram', tass_data_new.keys())

    def _inspect_omm_parallel_unistat(self, with_blue_omm_data=False, yandexuid='003'):
        """
        Проверяем, что запросы к place=omm_parallel влияют на unistat-счётчики
        """
        self._inspect_parallel_unistat(
            'place=omm_parallel&omm_place=yandexapp_vertical&yandexuid=003',
            'omm_parallel_yandexapp_vertical',
        )
        self._inspect_parallel_unistat(
            'place=omm_parallel&omm_place=podborki_web&yandexuid={}&rgb=blue'.format(yandexuid),
            'omm_parallel_podborki_web',
            is_blue=True,
            request_default_offers=True,
            with_blue_omm_data=with_blue_omm_data,
        )
        self._inspect_parallel_unistat(
            'place=omm_parallel&omm_place=podborki_web&yandexuid=003',
            'omm_parallel_podborki_web',
            request_default_offers=True,
        )
        self._inspect_parallel_unistat(
            'place=omm_parallel&omm_place=podborki_touch&yandexuid={}&rgb=blue'.format(yandexuid),
            'omm_parallel_podborki_touch',
            is_blue=True,
            request_default_offers=True,
            with_blue_omm_data=with_blue_omm_data,
        )
        self._inspect_parallel_unistat(
            'place=omm_parallel&omm_place=podborki_touch&yandexuid=003',
            'omm_parallel_podborki_touch',
            request_default_offers=True,
        )
        self._inspect_parallel_unistat(
            'place=omm_parallel&omm_place=podborki_yandexapp&yandexuid={}&rgb=blue'.format(yandexuid),
            'omm_parallel_podborki_yandexapp',
            is_blue=True,
            request_default_offers=True,
            with_blue_omm_data=with_blue_omm_data,
        )
        self._inspect_parallel_unistat(
            'place=omm_parallel&omm_place=podborki_yandexapp&yandexuid=003',
            'omm_parallel_podborki_yandexapp',
            request_default_offers=True,
        )
        self._inspect_parallel_unistat(
            'place=omm_parallel&omm_place=marketblock_web&yandexuid={}&rgb=blue'.format(yandexuid),
            'omm_parallel_marketblock_web',
            is_blue=True,
            request_default_offers=True,
            with_blue_omm_data=with_blue_omm_data,
        )
        self._inspect_parallel_unistat(
            'place=omm_parallel&omm_place=marketblock_web&yandexuid=003',
            'omm_parallel_marketblock_web',
            request_default_offers=True,
        )
        self._inspect_parallel_unistat(
            'place=omm_parallel&omm_place=marketblock_touch&yandexuid={}&rgb=blue'.format(yandexuid),
            'omm_parallel_marketblock_touch',
            is_blue=True,
            request_default_offers=True,
            with_blue_omm_data=with_blue_omm_data,
        )
        self._inspect_parallel_unistat(
            'place=omm_parallel&omm_place=marketblock_touch&yandexuid=003',
            'omm_parallel_marketblock_touch',
            request_default_offers=True,
        )
        self._inspect_parallel_unistat(
            'place=omm_parallel&omm_place=marketblock_yandexapp&yandexuid={}&rgb=blue'.format(yandexuid),
            'omm_parallel_marketblock_yandexapp',
            is_blue=True,
            request_default_offers=True,
            with_blue_omm_data=with_blue_omm_data,
        )
        self._inspect_parallel_unistat(
            'place=omm_parallel&omm_place=marketblock_yandexapp&yandexuid=003',
            'omm_parallel_marketblock_yandexapp',
            request_default_offers=True,
        )

    def test_omm_parallel_unistat(self):
        self._inspect_omm_parallel_unistat(with_blue_omm_data=False)
        self._inspect_omm_parallel_unistat(with_blue_omm_data=True, yandexuid='24040001')

    @classmethod
    def prepare_omm_parallel_empty_mrs(cls):
        """Создаем 10 моделей с различными картинками и ценами
        Создаем запросы к OMM на 3, 6, 9 и 30 моделей
        """
        cls.index.models += [
            Model(hyperid=1809701, title='parallel_mrs_model_1', hid=1809700),
            Model(hyperid=1809702, title='parallel_mrs_model_2', hid=1809700),
        ]

        cls.index.regional_models += [
            RegionalModel(hyperid=1809701, offers=0),
            RegionalModel(hyperid=1809702, offers=100),
        ]

        cls.bigb.on_request(yandexuid='004', client='merch-machine', parallel=True).respond(keywords=DEFAULT_PROFILE)

        cls.dj.on_request(exp='inter_vertical_unknown', msid=Const.DEFAULT_REQ_ID, yandexuid='004',).respond(
            models=[
                DjModel(id=1809701, title='parallel_mrs_model_1'),
                DjModel(id=1809702, title='parallel_mrs_model_2'),
            ]
        )

    def test_omm_parallel_empty_mrs(self):
        for place in ['omm_parallel', 'omm_parallel&omm_place=yandexapp_vertical']:
            response = self.report.request_json('place={}&yandexuid=004'.format(place))
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "entity": "product",
                            "id": 1809702,
                            'offers': {
                                'count': 100,
                            },
                        }
                    ]
                },
                allow_different_len=False,
            )
            if place == 'omm_parallel':
                self.assertFragmentIn(response, {"pages": ["n5gj-NL6XzWFuXak0MlCBw,,"]}, allow_different_len=False)

        response = self.report.request_json('place=omm_parallel&omm_place=yandexapp_vertical&&yandexuid=004')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "id": 1809702,
                        'offers': {
                            'count': 100,
                        },
                    }
                ]
            },
            allow_different_len=False,
        )

        response = self.report.request_json('place=omm_parallel&omm_place=yandexapp_vertical&yandexuid=004')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "id": 1809702,
                        'offers': {
                            'count': 100,
                        },
                    }
                ]
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_bigb_noglue_response(cls):
        cls.bigb.on_request(yandexuid='008', client='merch-machine', glue=False, parallel=True).respond(
            keywords=DEFAULT_PROFILE
        )
        cls.dj.on_request(exp='inter_vertical_unknown', msid=Const.DEFAULT_REQ_ID, yandexuid='008',).respond(
            models=[
                DjModel(id=1721802, title='model2'),
                DjModel(id=1721801, title='model1'),
                DjModel(id=1721803, title='model3'),
            ]
        )

    def test_bigb_noglue_response(self):
        """Проверяем, что при запросе BigB делается перезапрос c glue=0
        Для этого запрашиваем с yandexuid, для которого нет ответа на
        запрос со склеиванием и проверяем, что ошибок в логах нет
        """
        self.report.request_json('place=omm_parallel&yandexuid=008')

    def test_omm_parallel_clid(self):
        """Проверяем, что в урлах моделей place=omm_parallel есть clid,
        если указан pof=903 или pof=904 или pof=905 или pof=906
        """
        for pof in [903, 904, 905, 906]:
            response = self.report.request_json('place=omm_parallel&yandexuid=003&numdoc=3&pof={}'.format(pof))
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            self.get_omm_parallel_model(0, pof),
                            self.get_omm_parallel_model(1, pof),
                            self.get_omm_parallel_model(2, pof),
                        ]
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )

    def get_departments(self, clid):
        return [
            {
                "title": "Бытовая техника",
                "url": "//m.market.yandex.ru/catalog--bytovaia-tekhnika/54419?clid={}&hid=198118".format(clid),
            },
            {
                "title": "Компьютеры",
                "url": "//m.market.yandex.ru/catalog--kompiutery/54425?clid={}&hid=91009".format(clid),
            },
            {
                "title": "Электроника",
                "url": "//m.market.yandex.ru/catalog--elektronika/54440?clid={}&hid=198119".format(clid),
            },
            {
                "title": "Детские товары",
                "url": "//m.market.yandex.ru/catalog--detskie-tovary/54421?clid={}&hid=90764".format(clid),
            },
            {
                "title": "Дом и дача",
                "url": "//m.market.yandex.ru/catalog--dom-i-dacha/54422?clid={}&hid=90666".format(clid),
            },
            {"title": "Авто", "url": "//m.market.yandex.ru/catalog--avto/54418?clid={}&hid=90402".format(clid)},
            {
                "title": "Одежда, обувь, аксессуары",
                "url": "//m.market.yandex.ru/catalog--odezhda-obuv-aksessuary/54432?clid={}&hid=7877999".format(clid),
            },
            {
                "title": "Спорт и отдых",
                "url": "//m.market.yandex.ru/catalog--sport-i-otdykh/54436?clid={}&hid=91512".format(clid),
            },
            {
                "title": "Товары для красоты",
                "url": "//m.market.yandex.ru/catalog--tovary-dlia-krasoty/54438?clid={}&hid=90509".format(clid),
            },
            {
                "title": "Товары для животных",
                "url": "//m.market.yandex.ru/catalog--tovary-dlia-zhivotnykh/54496?clid={}&hid=90813".format(clid),
            },
            {
                "title": "Товары для здоровья",
                "url": "//m.market.yandex.ru/catalog--tovary-dlia-zdorovia/54734?clid={}&hid=8475840".format(clid),
            },
        ]

    def test_omm_parallel_departments(self):
        """Проверяем, что на выдаче place=omm_parallel находятся департаменты,
        если указан pof=905 или pof=906
        """
        response = self.report.request_json('place=omm_parallel&yandexuid=003&numdoc=3&pof=905')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        self.get_omm_parallel_model(0, 905),
                        self.get_omm_parallel_model(1, 905),
                        self.get_omm_parallel_model(2, 905),
                    ],
                    "departments": self.get_departments(905),
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json('place=omm_parallel&yandexuid=003&numdoc=3&pof=906')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        self.get_omm_parallel_model(0, 906),
                        self.get_omm_parallel_model(1, 906),
                        self.get_omm_parallel_model(2, 906),
                    ],
                    "departments": self.get_departments(clid=906),
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_omm_cgi_parameter(self):
        """Проверяем, что новый формат запросов к OMM работает"""
        response = self.report.request_json('place=omm_parallel&yandexuid=003&numdoc=3')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        self.get_omm_parallel_model(0),
                        self.get_omm_parallel_model(1),
                        self.get_omm_parallel_model(2),
                    ]
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_omm_block_tap(cls):
        adv_machine_models = []
        for seq in range(30):
            picture_link = '//avatars.mds.yandex.net/get-marketpic/2359{}/market_-dRDIVQlD_J6ZSezQ1j{}Q/orig'.format(
                seq, seq
            )

            cls.index.models += [
                Model(
                    hyperid=2351601 + seq,
                    title='Модель для ПП ' + str(seq),
                    hid=1786500,
                    picinfo=picture_link,
                    add_picinfo=picture_link,
                ),
            ]

            cls.index.offers += [
                Offer(hyperid=2351601 + seq, price=100 + seq * 10),
                Offer(hyperid=2351601 + seq, price=2000 + seq * 10),
            ]

            adv_machine_models += [DjModel(id=2351601 + seq, title='Модель для ПП ' + str(seq), pic_url=picture_link)]

        _ = [
            adv_machine_models[29],
            adv_machine_models[28],
            adv_machine_models[27],
            adv_machine_models[0],
            adv_machine_models[26],
            adv_machine_models[25],
            adv_machine_models[23],
            adv_machine_models[22],
            adv_machine_models[1],
            adv_machine_models[2],
            adv_machine_models[3],
            adv_machine_models[21],
            adv_machine_models[20],
            adv_machine_models[19],
            adv_machine_models[17],
            adv_machine_models[16],
            adv_machine_models[15],
            adv_machine_models[14],
            adv_machine_models[13],
            adv_machine_models[12],
            adv_machine_models[11],
            adv_machine_models[10],
        ]

        cls.bigb.on_request(yandexuid='005', client='merch-machine').respond(keywords=DEFAULT_PROFILE)
        cls.bigb.on_request(yandexuid='005', client='merch-machine', parallel=True).respond(keywords=DEFAULT_PROFILE)

        cls.dj.on_request(
            exp='inter_vertical_unknown',
            msid=Const.DEFAULT_REQ_ID,
            yandexuid='005',
        ).respond(models=adv_machine_models)

    def test_omm_block_tap(self):
        """Проверяем, что в плейсе omm_parallel по дефолту появляется блок pages
        Проверяем, что плейс omm_vertical умеет мержить шифрованные hyperid
        из cgi-параметров с теми, что приходят от OMM
        Проверяем, что параметр chosen-hyperid работает
        Проверяем, что вторая страница в omm_vertical формируется в соотвествии с
        первой (модели из блока не попадают на вторую страницу вертикали)
        """

        PAGE_ENCRYPTED_HYPERIDS = [
            "cEwj3t3vM6S530Z3Du2H2bWheNtlKkHS1W2g08wNJoFAHiL7FGu2BXnU-k57M4s8wYMHINxJqH5DSXoK_UbMHIiW6Lu1MAaryq68O11yE4Ox_YqO2uPpZYQm3Fj_o7f2x3kikue3WAXvi-z_wmGjcSuSM5WH6Ivi8jk-0hK0ZtWEMI8xllizAQ,,",
            "XQDWJg-n0G9JLPtt3nOhvU7s7AQ79sHVSCXowz3RUtjEGKMw86jBR5dzNIMGqJBWvfy0ZMJlMR8DnfO6cfb4A0B38smddRq-d0pawXWSM-WCgf0_27Ogq2EOijnRhocvk1HRVCtia4ffu_NQunMJA9j7MdMdAp5u",
        ]
        response = self.report.request_json('place=omm_parallel&yandexuid=005&numdoc=16')
        self.assertFragmentIn(
            response, {"pages": PAGE_ENCRYPTED_HYPERIDS}, preserve_order=True, allow_different_len=False
        )

    def test_omm_yandexapp_block_tap(self):
        """
        Проверяем, что плейс omm_parallel с yandexapp_vertical умеет мержить
        шифрованные hyperid из cgi-параметров с теми, что приходят от OMM
        Проверяем, что параметр chosen-hyperid работает
        Проверяем, что вторая страница в yandexapp_vertical формируется в
        соотвествии с первой (модели из блока не попадают на вторую страницу
        вертикали)
        """
        PAGE_ENCRYPTED_HYPERIDS = [
            "cEwj3t3vM6S530Z3Du2H2bWheNtlKkHS1W2g08wNJoFAHiL7FGu2BXnU-k57M4s8wYMHINxJqH5DSXoK_UbMHIiW6Lu1MAaryq68O11yE4Ox_YqO2uPpZYQm3Fj_o7f2x3kikue3WAXvi-z_wmGjcSuSM5WH6Ivi8jk-0hK0ZtWEMI8xllizAQ,,",
            "XQDWJg-n0G9JLPtt3nOhvU7s7AQ79sHVSCXowz3RUtjEGKMw86jBR5dzNIMGqJBWvfy0ZMJlMR8DnfO6cfb4A0B38smddRq-d0pawXWSM-WCgf0_27Ogq2EOijnRhocvk1HRVCtia4ffu_NQunMJA9j7MdMdAp5u",
        ]
        response = self.report.request_json('place=omm_parallel&omm_place=yandexapp_vertical&yandexuid=005&numdoc=16')
        self.assertFragmentIn(
            response, {"pages": PAGE_ENCRYPTED_HYPERIDS}, preserve_order=True, allow_different_len=False
        )

    @classmethod
    def prepare_blue_omm_parallel_places(cls):
        """Создаем категории, модели, ответы BigB, OMM для синих моделей"""
        cls.index.outlets += [
            Outlet(fesh=1886710, region=213, point_type=Outlet.FOR_PICKUP, point_id=18867101),
        ]
        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=1,
                fesh=1886710,
                carriers=[101],
                options=[PickupOption(outlet_id=18867101)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        promo_blue_cashback = Promo(
            promo_type=PromoType.BLUE_CASHBACK,
            key='JVvklxUgdnawSJPG4UhZ-1',
            shop_promo_id='blue_cashback_1',
            blue_cashback=PromoBlueCashback(
                share=0.2,
                version=10,
                priority=3,
            ),
        )
        promo_promocode = Promo(
            key='JVvklxUgdnawSJPG4UhZ-2',
            promo_type=PromoType.PROMO_CODE,
            promo_code='promocode_1_text',
            discount_value=300,
            discount_currency='RUR',
        )
        cls.index.promos += [
            promo_blue_cashback,
            promo_promocode,
        ]
        cls.settings.loyalty_enabled = True
        cls.dynamic.loyalty += [DynamicBlueGenericBundlesPromos(whitelist=[promo.key for promo in cls.index.promos])]

        dj_models = []
        for seq in range(1, 16):
            picture_link = '//avatars.mds.yandex.net/get-marketpic/2359{}/market_-dRDIVQlD_J6ZSezQ1j{}Q/orig'.format(
                seq, seq
            )

            cls.index.models += [
                Model(
                    hyperid=2404000 + seq, title='Синяя модель для ПП ' + str(seq), hid=2404000, picinfo=picture_link
                ),
            ]

            cls.index.offers += [
                Offer(hyperid=2404000 + seq, price=100 + seq * 10),
                Offer(hyperid=2404000 + seq, price=2000 + seq * 10),
            ]

            cls.index.mskus += [
                MarketSku(
                    hyperid=2404000 + seq,
                    sku=240400000 + seq,
                    blue_offers=[
                        BlueOffer(
                            price=500,
                            price_old=600,
                            waremd5='WARE_{:02d}______________g'.format(seq),
                            vat=Vat.NO_VAT,
                            feedid=12345,
                            pickup_buckets=[1],
                            promo=[promo_blue_cashback, promo_promocode],
                            blue_promo_key=[promo_blue_cashback.shop_promo_id]
                            if seq % 2 == 0
                            else [promo_blue_cashback.shop_promo_id, promo_promocode.shop_promo_id],
                        ),
                    ],
                ),
            ]
            dj_models.append(DjModel(id=2404000 + seq, title='Синяя модель для ПП ' + str(seq), pic_url=picture_link))

        cls.bigb.on_request(yandexuid='24040001', client='merch-machine').respond(keywords=DEFAULT_PROFILE)
        cls.bigb.on_request(yandexuid='24040001', client='merch-machine', parallel=True).respond(
            keywords=DEFAULT_PROFILE
        )

        cls.dj.on_request(
            exp='inter_vertical_unknown',
            msid=Const.DEFAULT_REQ_ID,
            yandexuid='24040001',
        ).respond(models=dj_models)

    def get_blue_omm_parallel_model(
        self, index, clid=None, touch=True, has_waremd5=False, discount=False, promos=False, promo_codes=False, lr=None
    ):
        seq = index + 1
        model_id = 2404000 + seq
        sku_id = 240400000 + seq
        parts = []
        if clid:
            parts.append('clid={}'.format(clid))
        if has_waremd5:
            parts.append('do-waremd5={}'.format('WARE_{:02d}______________g'.format(seq)))
        if lr:
            parts.append('lr={}'.format(lr))
        parts.append('wprid={}'.format(Const.DEFAULT_REQ_ID))

        touch_str = 'm.' if touch else ''
        slug_str = 'product--siniaia-model-dlia-pp-{}'.format(seq)

        res = {
            "entity": "product",
            "titles": {"raw": "Синяя модель для ПП " + str(seq)},
            "urls": {"direct": "//{}market.yandex.ru/{}/{}?{}".format(touch_str, slug_str, model_id, '&'.join(parts))},
            "id": model_id,
            "marketSku": str(sku_id),
            "prices": {
                "min": str(500),
                "max": str(500),
                "currency": "RUR",
                "avg": str(500),
                "discount": {"oldMin": str(600)} if discount else Absent(),
            },
        }
        if promos and promo_codes:
            res["promos"] = [{"type": "blue-cashback"}, {"type": "promo-code"}]
        elif promos:
            res["promos"] = [{"type": "blue-cashback"}]
        elif promo_codes:
            res["promos"] = [{"type": "promo-code"}]
        else:
            res["promos"] = Absent()
        return res

    def test_blue_omm_parallel_format(self):
        """Проверяем, что на выдаче place=omm_parallel в эксперименте находятся синие модели,
        рекомендуемые OMM в заданном формате
        """
        for place_rearr in ['omm_parallel&rearr-factors=market_yandexapp_blue_vertical=1']:
            response = self.report.request_json('place={}&yandexuid=24040001&rgb=blue&numdoc=3'.format(place_rearr))
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            self.get_blue_omm_parallel_model(0),
                            self.get_blue_omm_parallel_model(1),
                            self.get_blue_omm_parallel_model(2),
                        ]
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )

    def test_blue_omm_parallel_lr_arg(self):
        """Проверяем наличие в url моделей параметра lr"""
        response = self.report.request_json(
            'place=omm_parallel&omm_place=marketblock_touch&rgb=blue&yandexuid=24040001&numdoc=3&&rids=213'
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        self.get_blue_omm_parallel_model(0, lr=213),
                        self.get_blue_omm_parallel_model(1, lr=213),
                        self.get_blue_omm_parallel_model(2, lr=213),
                    ]
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def get_blue_departments(self, clid=None):
        departments = []
        for dpt in BLUE_DEPARTMENTS:
            cgi_params = []
            if clid is not None:
                cgi_params.append("clid={}".format(clid))
            if dpt[3] is not None:
                cgi_params.append("hid={}".format(dpt[3]))
            params_str = ""
            prefix = "?"
            for param in cgi_params:
                params_str += prefix + param
                prefix = "&"

            department = {
                "title": dpt[0],
                "url": "//m.market.yandex.ru/catalog--{}/{}{}".format(dpt[1], dpt[2], params_str),
            }
            departments.append(department)
        return departments

    def test_omm_parallel_podborki_marketblock(self):
        offers = 10
        for i in range(pow(2, 4)):
            flag_discount = bool(i & (1 << 0))
            flag_offerid = bool(i & (1 << 1))
            flag_promos = bool(i & (1 << 2))
            flag_promo_codes = bool(i & (1 << 3))
            for sub_place, is_touch in (
                ('podborki_web', False),
                ('podborki_touch', True),
                ('podborki_yandexapp', True),
                ('marketblock_web', False),
                ('marketblock_touch', True),
                ('marketblock_yandexapp', True),
            ):
                response = self.report.request_json(
                    'place=omm_parallel&omm_place={}&rgb=blue&perks=yandex_cashback&yandexuid=24040001&numdoc={}&rearr-factors=omm_parallel_discount={};omm_parallel_offerid={};omm_parallel_promos={};omm_parallel_promo_codes={}'.format(  # noqa
                        sub_place,
                        offers,
                        int(flag_discount),
                        int(flag_offerid),
                        int(flag_promos),
                        int(flag_promo_codes),
                    )
                )
                self.assertFragmentIn(
                    response,
                    {
                        "results": [
                            self.get_blue_omm_parallel_model(
                                seq, None, is_touch, flag_offerid, flag_discount, flag_promos, flag_promo_codes
                            )
                            for seq in range(offers)
                        ]
                    },
                    preserve_order=True,
                    allow_different_len=False,
                )

    def test_omm_parallel_departments_blue(self):
        """Проверяем, что на выдаче place=omm_parallel находятся департаменты,
        если указан pof=905 или pof=906
        """
        for place_rearr in ['omm_parallel&omm_place=yandexapp_vertical&rearr-factors=market_yandexapp_blue_vertical=1']:
            response = self.report.request_json(
                'place={}&yandexuid=24040001&numdoc=3&rgb=blue&pof=905'.format(place_rearr)
            )
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            self.get_blue_omm_parallel_model(0, 905),
                            self.get_blue_omm_parallel_model(1, 905),
                            self.get_blue_omm_parallel_model(2, 905),
                        ],
                        "departments": self.get_blue_departments(clid=905),
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )

            response = self.report.request_json(
                'place={}&yandexuid=24040001&numdoc=3&rgb=blue&pof=906'.format(place_rearr)
            )
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            self.get_blue_omm_parallel_model(0, 906),
                            self.get_blue_omm_parallel_model(1, 906),
                            self.get_blue_omm_parallel_model(2, 906),
                        ],
                        "departments": self.get_blue_departments(clid=906),
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )

    def test_blue_omm_yandexapp_vertical(self):
        """Проверяем, что на выдаче place=omm_parallel&omm_place=yandexapp_vertical / place=omm_vertical находятся синие модели,
        рекомендуемые OMM.
        """
        for place_id in [
            "place=omm_parallel&omm_place=yandexapp_vertical&rearr-factors=market_yandexapp_blue_vertical=1"
        ]:
            # initial query
            response = self.report.request_json(
                '{place_id}&yandexuid=24040001&rgb=blue&numdoc=3&pof=905'.format(place_id=place_id)
            )
            pages = response.root["search"]["pages"]
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            self.get_blue_omm_parallel_model(0, 905),
                            self.get_blue_omm_parallel_model(1, 905),
                            self.get_blue_omm_parallel_model(2, 905),
                        ],
                        "departments": self.get_blue_departments(clid=905),
                        "pages": NotEmpty(),
                        "apiUrl": "https://api.market.yandex.ru/yandex/home/v2.1.5/models/recommended/vertical/app?geo_id=0",
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )

            # page query
            for i, encrypted_ids in enumerate(pages):
                response = self.report.request_json(
                    '{place_id}&pof=905&yandexuid=24040001&rgb=blue&numdoc=3&encrypted-hyperids={encrypted_ids}&page={page_number}'.format(
                        place_id=place_id, encrypted_ids=encrypted_ids, page_number=i + 1
                    )
                )
                self.assertFragmentIn(
                    response,
                    {
                        "search": {
                            "results": [
                                self.get_blue_omm_parallel_model(i * 3, 905),
                                self.get_blue_omm_parallel_model(i * 3 + 1, 905),
                                self.get_blue_omm_parallel_model(i * 3 + 2, 905),
                            ],
                            "apiUrl": Absent(),
                            "pages": Absent(),
                        }
                    },
                    preserve_order=True,
                    allow_different_len=False,
                )

    def test_invalid_encrypted_hyperids(self):
        self.report.request_json(
            'place=omm_parallel&encrypted-hyperids=abyrv&yandexuid=003&numdoc=3&pof=905&add-more-hyperids=1'
        )
        self.error_log.expect(message='incorrect input length for base64 decode')

    def test_bigb_error_handling(self):
        """
        Запрашиваем с несуществующим yandexuid и
        проверяем наличие соответствующих ошибок в логах
        """
        for place in ['omm_parallel']:
            self.report.request_json('place={}&yandexuid=000&debug=1'.format(place))

            self.report.request_json('place={}&puid=000&debug=1'.format(place))

        self.error_log.expect(code=3787)

    @classmethod
    def prepare_omm_no_picture(cls):
        """Создаем модели без картинок"""
        picture_link = ''

        cls.index.models += [
            Model(hyperid=2455301, title='Модель для ПП ', hid=2455300, no_picture=True),
        ]

        cls.index.offers += [
            Offer(hyperid=2455301, price=100),
            Offer(hyperid=2455301, price=2000),
        ]

        cls.bigb.on_request(yandexuid='245530', client='merch-machine').respond(keywords=DEFAULT_PROFILE)
        cls.bigb.on_request(yandexuid='245530', client='merch-machine', parallel=True).respond(keywords=DEFAULT_PROFILE)

        cls.dj.on_request(
            exp='inter_vertical_unknown',
            msid=Const.DEFAULT_REQ_ID,
            yandexuid='245530',
        ).respond(models=[DjModel(id=2455301, title='Модель для ПП ', pic_url=picture_link)])

    def test_no_picture(self):
        """Проверяем, что при появлении модели без картинки
        она не появляется на выдаче omm_parallel, но остается в pages
        """
        response = self.report.request_json('place=omm_parallel&omm_place=yandexapp_vertical&yandexuid=245530')
        self.assertFragmentIn(
            response,
            {
                'results': [],
                'pages': ['JHa24DI7wwVOGfor9sbhuw,,'],
            },
            allow_different_len=False,
        )

    def test_blue_omm_parallel_toxic_regions(self):
        """Проверяем, что на выдаче place=omm_parallel и place=vertical на синем в эксперименте
        test_blue_omm_parallel_toxic_regions выдача пустая в токсичных регионах
        При этом для другого плейса omm_market в токсичном регионе есть выдача
        """
        for place_rearr in [
            'omm_parallel&rearr-factors=market_blue_omm_parallel=1',
            'omm_parallel&omm_place=yandexapp_vertical&rearr-factors=market_yandexapp_blue_vertical=1',
        ]:

            response = self.report.request_json(
                'place={}&rids=54&yandexuid=24040001&rgb=blue&numdoc=3&pof=905'.format(place_rearr)
            )
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": NotEmpty(),
                        "departments": NotEmpty(),
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )

    @classmethod
    def prepare_white_model_from_blue_omm(cls):
        """Создаем категории, модели, ответы BigB, OMM для синих моделей"""
        adv_machine_models = []
        for seq in range(1, 16):
            picture_link = '//avatars.mds.yandex.net/get-marketpic/2359{}/market_-dRDIVQlD_J6ZSezQ1j{}Q/orig'.format(
                seq, seq
            )

            cls.index.models += [
                Model(
                    hyperid=2659100 + seq,
                    title='Синяя модель на белом для ПП ' + str(seq),
                    hid=2659100,
                    picinfo=picture_link,
                ),
            ]

            if seq % 2 == 0:
                cls.index.offers += [
                    Offer(hyperid=2659100 + seq, price=120),
                    Offer(hyperid=2659100 + seq, price=2020),
                ]

            cls.index.mskus += [
                MarketSku(
                    hyperid=2659100 + seq,
                    sku=265910000 + seq,
                    blue_offers=[
                        BlueOffer(price=500, vat=Vat.NO_VAT, feedid=12345),
                    ],
                ),
            ]

            adv_machine_models += [
                DjModel(id=2659100 + seq, title='Синяя модель на белом для ПП ' + str(seq), pic_url=picture_link)
            ]

        cls.bigb.on_request(yandexuid='26591001', client='merch-machine').respond(keywords=DEFAULT_PROFILE)
        cls.bigb.on_request(yandexuid='26591001', client='merch-machine', parallel=True).respond(
            keywords=DEFAULT_PROFILE
        )

        cls.dj.on_request(
            exp='inter_vertical_unknown',
            msid=Const.DEFAULT_REQ_ID,
            yandexuid='26591001',
        ).respond(models=adv_machine_models)

    def get_white_model_from_blue_omm_price(self, seq):
        if seq % 2 == 0:
            return {"min": str(120), "max": str(2020), "currency": "RUR", "avg": str(500)}
        return {"min": str(500), "max": str(500), "currency": "RUR", "avg": str(500)}

    def get_white_model_from_blue_omm(self, index, clid=None, touch=True, omm_vertical=False):
        seq = index + 1
        model_id = 2659100 + seq
        clid_str = ''
        wprid_str = ''
        if clid:
            clid_str = '?clid={}'.format(clid)
            wprid_str = '&wprid={}'.format(Const.DEFAULT_REQ_ID)
        else:
            wprid_str = '?wprid={}'.format(Const.DEFAULT_REQ_ID)
        slug_str = 'product--siniaia-model-na-belom-dlia-pp-'
        touch_str = 'm.' if touch else ''
        return {
            "entity": "product",
            "titles": {"raw": "Синяя модель на белом для ПП " + str(seq)},
            "urls": {
                "direct": "//{}market.yandex.ru/{}{}/{}{}{}".format(
                    touch_str, slug_str, seq, model_id, clid_str, wprid_str
                )
            }
            if not omm_vertical
            else {},
            "id": model_id,
            "marketSku": Absent(),
            "prices": self.get_white_model_from_blue_omm_price(seq),
        }

    @classmethod
    def prepare_uniqueness_group_models(cls):
        cls.index.model_groups += [
            ModelGroup(hyperid=269461, title='group model 1', hid=26946),
            ModelGroup(hyperid=269462, title='group model 2', hid=26946),
        ]
        cls.index.models += [
            Model(hyperid=2694611, group_hyperid=269461, title='model 1', hid=26946),
            Model(hyperid=2694612, group_hyperid=269461, title='model 2', hid=26946),
            Model(hyperid=2694621, group_hyperid=269462, title='model 3', hid=26946),
            Model(hyperid=2694622, group_hyperid=269462, title='model 4', hid=26946),
            Model(hyperid=2694623, group_hyperid=269462, title='model 5', hid=26946),
            Model(hyperid=2694631, title='model 6', hid=26946),
            Model(hyperid=2694641, title='model 7', hid=26946),
        ]

        cls.index.offers += [
            Offer(hyperid=2694611, price=11),
            Offer(hyperid=2694612, price=12),
            Offer(hyperid=2694621, price=21),
            Offer(hyperid=2694622, price=22),
            Offer(hyperid=2694623, price=23),
            Offer(hyperid=2694631, price=31),
            Offer(hyperid=2694641, price=41),
        ]

        cls.bigb.on_request(yandexuid='007', client='merch-machine').respond(keywords=DEFAULT_PROFILE)
        cls.bigb.on_request(yandexuid='007', client='merch-machine', parallel=True).respond(keywords=DEFAULT_PROFILE)

        cls.dj.on_request(exp='inter_vertical_unknown', msid=Const.DEFAULT_REQ_ID, yandexuid='007',).respond(
            models=[
                DjModel(id=2694611, title='model 1'),
                DjModel(id=2694612, title='model 2'),
                DjModel(id=2694621, title='model 3'),
                DjModel(id=2694622, title='model 4'),
                DjModel(id=2694623, title='model 5'),
                DjModel(id=2694631, title='model 6'),
                DjModel(id=2694641, title='model 7'),
            ]
        )

    def get_model_by_number(self, model_number):
        result = {
            'entity': 'product',
            'titles': {
                'raw': 'model {}'.format(model_number),
            },
        }
        return result

    def test_uniqueness_group_models(self):
        """
        На выдаче omm_market и omm_parallel модели должны быть уникальными по групповой моделе (если она есть).
        В нашем случае отфильтруются модели 2, 4, 5
        """
        for place in ['place=omm_parallel&omm_place=yandexapp_vertical']:
            response = self.report.request_json('{}&yandexuid=007'.format(place))
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            self.get_model_by_number(1),
                            self.get_model_by_number(3),
                            self.get_model_by_number(6),
                            self.get_model_by_number(7),
                        ],
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )

    @classmethod
    def prepare_models_from_history(cls):
        cls.index.hypertree += [HyperCategory(hid=26198, output_type=HyperCategoryType.GURU)]

        cls.index.models += [
            Model(hyperid=261980 + index, hid=26198, title='model {}'.format(index + 1)) for index in range(34)
        ]

        cls.index.offers += [Offer(hyperid=261980 + index, price=100 + index * 100) for index in range(34)]

        model_views = [ModelLastSeenEvent(model_id=261980 + index, timestamp=200 + index) for index in range(17)]
        model_last_seen_counter = MarketModelLastTimeCounter(model_views)

        cls.dj.on_request(
            exp='inter_vertical_unknown',
            msid=Const.DEFAULT_REQ_ID,
            yandexuid='006',
        ).respond(models=[DjModel(id=261980 + index, title='model {}'.format(index + 1)) for index in range(18, 34)])

        cls.bigb.on_request(yandexuid='006', client='merch-machine', parallel=True).respond(
            keywords=DEFAULT_PROFILE, counters=[model_last_seen_counter]
        )

    def test_models_from_history(self):
        response = self.report.request_json(
            'place=omm_parallel&omm_place=yandexapp_vertical&rearr-factors=market_recom_parallel_history=1&yandexuid=006&pp=18'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'history': [self.get_model_by_number(index) for index in reversed(list(range(2, 18)))],
                    'results': [self.get_model_by_number(index) for index in range(19, 35)],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json(
            'place=omm_parallel&omm_place=yandexapp_vertical&rearr-factors=market_recom_parallel_history=0&yandexuid=006&pp=18'
        )
        self.assertFragmentIn(response, {'search': {'history': NoKey('history')}})

    @classmethod
    def prepare_light_premium_offer(cls):
        cls.index.hybrid_auction_settings += [HybridAuctionParam(category=Const.ROOT_HID, cpc_ctr_for_cpc=0.033)]

        cls.index.navtree += [NavCategory(nid=26470000, hid=2647000, primary=True)]

        cls.index.shops += [
            Shop(fesh=2647010, name='Белый магазин 1', priority_region=213, cpa=Shop.CPA_NO),
            Shop(fesh=2647011, name='Белый магазин 2', priority_region=213, cpa=Shop.CPA_REAL),
            Shop(fesh=2647012, name='Белый магазин 3', priority_region=312, cpa=Shop.CPA_NO, regions=[213]),
        ]

        for seq in range(9):
            picture = Picture(
                width=500, height=450, group_id=23590 + seq + 1, picture_id='uS6z5i755IOLmUXx1CBy{}B'.format(seq + 1)
            )

            cls.index.models += [
                Model(hyperid=2647001 + seq, hid=2647000, vendor_id=26470001),
            ]

            cls.index.offers += [
                Offer(
                    hyperid=2647001 + seq,
                    fesh=2647010,
                    price=10000,
                    bid=10,
                    vbid=2,
                    waremd5="AAAAAAAA{}AAAAAAAAAAAAA".format(seq + 1),
                    ts=2647001 + seq,
                ),  # default; CPM = 100000 * 10 * 0.02 ~ 20000
                Offer(
                    hyperid=2647001 + seq,
                    fesh=2647011,
                    title='Offer B' + str(seq + 1),
                    descr='Description Offer B' + str(seq + 1),
                    price=10000,
                    bid=50,
                    vendor_id=26470001,
                    vbid=15,
                    waremd5="BBBBBBBB{}BBBBBBBBBBBBB".format(seq + 1),
                    picture=picture,
                    ts=2647011 + seq,
                ),  # premium & top-1; CPM = 100000 * 50 * 0.01 ~ 50000
                Offer(
                    hyperid=2647001 + seq,
                    fesh=2647011,
                    title='Offer D' + str(seq + 1),
                    price=10000,
                    bid=49,
                    vbid=14,
                    waremd5="DDDDDDDD{}DDDDDDDDDDDDD".format(seq + 1),
                    ts=2647031 + seq,
                ),  # premium & top-1; CPM = 100000 * 50 * 0.01 ~ 50000
                Offer(
                    hyperid=2647001 + seq,
                    fesh=2647012,
                    price=10000,
                    bid=30,
                    vbid=20,
                    waremd5="CCCCCCCC{}CCCCCCCCCCCCC".format(seq + 1),
                    ts=2647021 + seq,
                ),  # CPM = 100000 * 30 * 0.01 ~ 30000
            ]
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2647001 + seq).respond(0.02)
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2647011 + seq).respond(0.01)
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2647021 + seq).respond(0.01)
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2647031 + seq).respond(0.01)

        for seq in (2, 5, 8):
            picture = Picture(
                width=500, height=450, group_id=23590 + seq + 1, picture_id='uS6z5i755IOLmUXx1CEy{}E'.format(seq + 1)
            )
            cls.index.mskus += [
                MarketSku(
                    hyperid=2647001 + seq,
                    sku=26470000 + seq,
                    blue_offers=[
                        BlueOffer(
                            price=500,
                            title='Blue offer E' + str(seq + 1),
                            descr='Description Blue offer E' + str(seq + 1),
                            bid=100,
                            vbid=100,
                            vat=Vat.NO_VAT,
                            feedid=12345,
                            vendor_id=26470001,
                            waremd5="EEEEEEEE{}EEEEEEEEEEEEE".format(seq + 1),
                            pickup_buckets=[1],
                            picture=picture,
                            ts=2647041 + seq,
                        ),
                    ],
                ),
            ]
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2647041 + seq).respond(0.01)

        cls.bigb.on_request(yandexuid='2647001', client='merch-machine').respond(keywords=DEFAULT_PROFILE)
        cls.bigb.on_request(yandexuid='2647001', client='merch-machine', parallel=True).respond(
            keywords=DEFAULT_PROFILE
        )

        cls.dj.on_request(exp='inter_vertical_unknown', msid=Const.DEFAULT_REQ_ID, yandexuid='2647001',).respond(
            models=[
                DjModel(id=2647001, title='model 1'),
                DjModel(id=2647002, title='model 2'),
                DjModel(id=2647003, title='model 3'),
                DjModel(id=2647004, title='model 4'),
                DjModel(id=2647005, title='model 5'),
                DjModel(id=2647006, title='model 6'),
                DjModel(id=2647007, title='model 7'),
                DjModel(id=2647008, title='model 8'),
                DjModel(id=2647009, title='model 9'),
            ]
        )

    def get_premium_offer(self, model_seq):
        result = {
            'entity': 'offer',
            'titles': {
                'raw': 'Offer B{}'.format(model_seq + 1),
            },
            'urls': {
                'encrypted': NotEmpty(),
            },
            'pictures': [
                {
                    "entity": "picture",
                    "thumbnails": [
                        {
                            "url": "http://avatars.mdst.yandex.net/get-marketpic/2359{}/market_uS6z5i755IOLmUXx1CBy{}A/300x300".format(
                                model_seq + 1, model_seq + 1
                            ),
                            "width": 300,
                            "height": 270,
                        }
                    ],
                }
            ],
            'wareId': 'BBBBBBBB{}BBBBBBBBBBBBA'.format(model_seq + 1),
            'model': {
                'id': 2647001 + model_seq,
            },
            'shopName': 'Белый магазин 2',
            'prices': {
                'value': '10000',
            },
        }
        return result

    def get_premium_offer_blue(self, model_seq):
        result = {
            'entity': 'offer',
            'titles': {
                'raw': 'Blue offer E{}'.format(model_seq + 1),
            },
            'urls': {
                'encrypted': NotEmpty(),
            },
            'pictures': [
                {
                    "entity": "picture",
                    "thumbnails": [
                        {
                            "url": "http://avatars.mdst.yandex.net/get-marketpic/2359{}/market_uS6z5i755IOLmUXx1CEy{}A/300x300".format(
                                model_seq + 1, model_seq + 1
                            ),
                            "width": 300,
                            "height": 270,
                        }
                    ],
                }
            ],
            'wareId': 'EEEEEEEE{}EEEEEEEEEEEEA'.format(model_seq + 1),
            'model': {
                'id': 2647001 + model_seq,
            },
            'shopName': 'БЕРУ',
            'prices': {
                'value': '500',
            },
        }
        return result

    def test_light_premium_offer(self):
        """
        Проверяем выдачу премиального оффера под флагом
        market_po_in_omm_parallel=1 в плейсах omm_parallel
        Проверяем логи, ставки, цену клика
        Проверяем пейджинг
        """
        for place in [
            'place=omm_parallel&omm_place=yandexapp_vertical',
            'place=omm_parallel&omm_place=yandexapp_vertical',
        ]:
            for rearr_factors in ['', ';market_use_flat_grouping_in_multi_premium=1']:
                response = self.report.request_json(
                    '{}&yandexuid=2647001&rearr-factors=market_po_in_omm_parallel=1;market_ranging_cpa_by_ue_in_top_cpa_multiplier=1{}&rids=213&show-urls=encrypted&numdoc=3'.format(
                        place, rearr_factors
                    )
                )
                self.assertFragmentIn(
                    response,
                    {
                        "search": {
                            "results": [
                                self.get_premium_offer(0),
                                self.get_premium_offer(1),
                                self.get_premium_offer_blue(2),
                            ],
                        }
                    },
                    preserve_order=True,
                    allow_different_len=False,
                )

                for hyper_id in [2647001, 2647002]:
                    self.show_log.expect(
                        hyper_id=hyper_id,
                        click_price=27,
                        cpm=50000,
                        next_offer_cpm=0.26,
                        bid=50,
                        min_bid=13,
                        is_premium_offer=1,
                        price=10000,
                        hyper_cat_id=2647000,
                        nid=26470000,
                        position=0,
                        shop_id=2647011,
                        vendor_click_price=0,
                        vendor_id=26470001,
                        vbid=0,
                        yandex_uid=2647001,
                    )

                    self.click_log.expect(
                        hyper_id=hyper_id,
                        cp=27,
                        cb=50,
                        price=10000,
                        hyper_cat_id=2647000,
                        min_bid=13,
                        nav_cat_id=26470000,
                        position=0,
                        shop_id=2647011,
                        cp_vnd=0,
                        vnd_id=26470001,
                        cb_vnd=0,
                        yandexuid=2647001,
                    )

                self.show_log.expect(
                    hyper_id=2647003,
                    click_price=1,
                    cpm=100000,
                    next_offer_cpm=0.5,
                    bid=100,
                    min_bid=1,
                    msku=26470002,
                    is_blue_offer=1,
                    is_premium_offer=1,
                    price=500,
                    hyper_cat_id=2647000,
                    nid=26470000,
                    position=0,
                    shop_id=1886710,
                    vendor_click_price=0,
                    vendor_id=26470001,
                    vbid=0,
                    yandex_uid=2647001,
                )
                self.click_log.expect(
                    hyper_id=2647003,
                    cp=1,
                    cb=100,
                    price=500,
                    is_blue=1,
                    hyper_cat_id=2647000,
                    min_bid=1,
                    msku=26470002,
                    nav_cat_id=26470000,
                    position=0,
                    shop_id=1886710,
                    cp_vnd=0,
                    vnd_id=26470001,
                    cb_vnd=0,
                    yandexuid=2647001,
                )

            pages = response.root["search"]["pages"]
            # page query
            for i, encrypted_ids in enumerate(pages):
                for rearr_factors in ['', ';market_use_flat_grouping_in_multi_premium=1']:
                    page_num = i + 1
                    response = self.report.request_json(
                        '{}&page={}&encrypted-hyperids={}&yandexuid=2647001&rearr-factors=market_po_in_omm_parallel=1{}&rids=213&show-urls=encrypted&numdoc=3'.format(
                            place, page_num, encrypted_ids, rearr_factors
                        )
                    )
                    self.assertFragmentIn(
                        response,
                        {
                            "search": {
                                "results": [
                                    self.get_premium_offer(i * 3 + 0),
                                    self.get_premium_offer(i * 3 + 1),
                                    self.get_premium_offer_blue(i * 3 + 2),
                                ],
                            }
                        },
                        preserve_order=True,
                        allow_different_len=False,
                    )

                    for hyper_id in [2647001, 2647002]:
                        self.show_log.expect(
                            hyper_id=hyper_id + i * 3,
                            click_price=27,
                            cpm=50000,
                            next_offer_cpm=0.26,
                            bid=50,
                            min_bid=13,
                            is_premium_offer=1,
                            price=10000,
                            hyper_cat_id=2647000,
                            nid=26470000,
                            position=0,
                            shop_id=2647011,
                            vendor_click_price=0,
                            vendor_id=26470001,
                            vbid=0,
                            yandex_uid=2647001,
                        )
                        self.click_log.expect(
                            hyper_id=hyper_id + i * 3,
                            cp=27,
                            cb=50,
                            price=10000,
                            hyper_cat_id=2647000,
                            min_bid=13,
                            nav_cat_id=26470000,
                            position=0,
                            shop_id=2647011,
                            cp_vnd=0,
                            vnd_id=26470001,
                            cb_vnd=0,
                            yandexuid=2647001,
                        )

                    self.show_log.expect(
                        hyper_id=2647003 + i * 3,
                        click_price=1,
                        cpm=100000,
                        next_offer_cpm=0.5,
                        bid=100,
                        min_bid=1,
                        msku=26470002 + i * 3,
                        is_blue_offer=1,
                        is_premium_offer=1,
                        price=500,
                        hyper_cat_id=2647000,
                        nid=26470000,
                        position=0,
                        shop_id=1886710,
                        vendor_click_price=0,
                        vendor_id=26470001,
                        vbid=0,
                        yandex_uid=2647001,
                    )
                    self.click_log.expect(
                        hyper_id=2647003 + i * 3,
                        cp=1,
                        cb=100,
                        price=500,
                        is_blue=1,
                        hyper_cat_id=2647000,
                        min_bid=1,
                        msku=26470002 + i * 3,
                        nav_cat_id=26470000,
                        position=0,
                        shop_id=1886710,
                        cp_vnd=0,
                        vnd_id=26470001,
                        cb_vnd=0,
                        yandexuid=2647001,
                    )

    def get_product_with_premium_offer(self, model_seq):
        result = {
            'entity': 'product',
            'id': 2647001 + model_seq,
            'offers': {
                'count': 4,
                'items': [
                    {
                        'entity': 'offer',
                        'titles': {
                            'raw': 'Offer B{}'.format(model_seq + 1),
                        },
                        'description': 'Description Offer B{}'.format(model_seq + 1),
                        'urls': {
                            'encrypted': NotEmpty(),
                        },
                        'shop': {
                            'name': 'Белый магазин 2',
                        },
                        'wareId': 'BBBBBBBB{}BBBBBBBBBBBBA'.format(model_seq + 1),
                        'model': {
                            'id': 2647001 + model_seq,
                        },
                        'prices': {
                            'value': '10000',
                        },
                    }
                ],
            },
        }
        return result

    def get_product_with_premium_offer_blue(self, model_seq):
        result = {
            'entity': 'product',
            'id': 2647001 + model_seq,
            'offers': {
                'count': 5,
                'items': [
                    {
                        'entity': 'offer',
                        'titles': {
                            'raw': 'Blue offer E{}'.format(model_seq + 1),
                        },
                        'description': 'Description Blue offer E{}'.format(model_seq + 1),
                        'urls': {
                            'encrypted': NotEmpty(),
                        },
                        'shop': {
                            'name': 'БЕРУ',
                        },
                        'wareId': 'EEEEEEEE{}EEEEEEEEEEEEA'.format(model_seq + 1),
                        'model': {
                            'id': 2647001 + model_seq,
                        },
                        'prices': {
                            'value': '500',
                        },
                    }
                ],
            },
        }
        return result

    @classmethod
    def prepare_light_premium_offer_warnings(cls):
        cls.index.shops += [
            Shop(fesh=2768110, name='Белый магазин 1', priority_region=213),
        ]

        cls.index.navtree += [NavCategory(nid=27681000, hid=2768100, primary=True)]

        cls.index.category_restrictions += [
            CategoryRestriction(
                name='medicine',
                hids_with_subtree=[2768110, 2768160],
                regional_restrictions=[
                    RegionalRestriction(
                        rids=[213],
                        show_offers=True,
                        disclaimers=[
                            Disclaimer(
                                name='medicine',
                                text='Лекарство. Не доставляется. Продается только в аптеках.',
                                short_text='Лекарство. Продается в аптеках',
                            ),
                            Disclaimer(name='medicine2', text='Для детей от 6 месяцев', short_text='С 6 месяцев'),
                            Disclaimer(
                                name='supplement',
                                text='БАД. Не является лекарством',
                                short_text='Не является лекарством',
                                default_warning=False,
                            ),
                        ],
                    )
                ],
            ),
            CategoryRestriction(
                name='age',
                hids_with_subtree=[2768120, 2768170],
                regional_restrictions=[
                    RegionalRestriction(
                        rids=[Const.ROOT_COUNTRY],
                        show_offers=True,
                        disclaimers=[
                            Disclaimer(name='age'),
                        ],
                    )
                ],
            ),
            CategoryRestriction(
                name='supplement',
                hids_with_subtree=[2768140, 2768150, 2768190, 2768200],
                regional_restrictions=[
                    RegionalRestriction(
                        rids=[213],
                        show_offers=True,
                        disclaimers=[
                            Disclaimer(
                                name='medicine2',
                                text='Для детей от 6 месяцев',
                                short_text='С 6 месяцев',
                                default_warning=False,
                            ),
                            Disclaimer(
                                name='supplement',
                                text='БАД. Не является лекарством',
                                short_text='Не является лекарством',
                                default_warning=False,
                            ),
                        ],
                    )
                ],
            ),
        ]

        for seq in range(3):
            cls.index.models += [
                Model(hyperid=2768101 + seq, hid=2768110 + (seq * 10)),
            ]

        for seq in range(5, 8):
            cls.index.models += [
                Model(hyperid=2768101 + seq, hid=2768110 + (seq * 10)),
            ]

        cls.index.models += [
            Model(hyperid=2768104, hid=2768140, disclaimers_model='medicine2'),
            Model(hyperid=2768105, hid=2768150, warning_specification="supplement"),
            Model(hyperid=2768109, hid=2768190, disclaimers_model='medicine2'),
            Model(hyperid=2768110, hid=2768200, warning_specification="supplement"),
        ]

        cls.index.offers += [
            Offer(hyperid=2768101, price=10000, fesh=2768110, bid=50, vbid=2, ts=2768101),
            Offer(hyperid=2768102, price=10000, fesh=2768110, age=18, age_unit='year', bid=50, vbid=2, ts=2768102),
            Offer(hyperid=2768103, price=10000, fesh=2768110, adult=True, bid=50, vbid=2, ts=2768103),
            Offer(hyperid=2768104, price=10000, fesh=2768110, bid=50, vbid=2, ts=2768104),
            Offer(hyperid=2768105, price=10000, fesh=2768110, bid=50, vbid=2, ts=2768105),
        ]
        for seq in range(5):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2647001 + seq).respond(0.02)

        cls.index.mskus += [
            MarketSku(
                hyperid=2768106,
                sku=27681006,
                blue_offers=[
                    BlueOffer(price=500, bid=100, vbid=100, feedid=12345, pickup_buckets=[1], ts=2768146),
                ],
            ),
            MarketSku(
                hyperid=2768107,
                sku=27681007,
                blue_offers=[
                    BlueOffer(
                        price=500,
                        bid=100,
                        vbid=100,
                        feedid=12345,
                        age=18,
                        age_unit='year',
                        pickup_buckets=[1],
                        ts=2768147,
                    ),
                ],
            ),
            MarketSku(
                hyperid=2768108,
                sku=27681008,
                blue_offers=[
                    BlueOffer(price=500, bid=100, vbid=100, feedid=12345, adult=True, pickup_buckets=[1], ts=2768148),
                ],
            ),
            MarketSku(
                hyperid=2768109,
                sku=27681009,
                blue_offers=[
                    BlueOffer(price=500, bid=100, vbid=100, feedid=12345, pickup_buckets=[1], ts=2768149),
                ],
            ),
            MarketSku(
                hyperid=2768110,
                sku=27681010,
                blue_offers=[
                    BlueOffer(price=500, bid=100, vbid=100, feedid=12345, pickup_buckets=[1], ts=2768150),
                ],
            ),
        ]
        for seq in range(5, 10):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2647041 + seq).respond(0.01)

        cls.bigb.on_request(yandexuid='2768101', client='merch-machine').respond(keywords=DEFAULT_PROFILE)
        cls.bigb.on_request(yandexuid='2768101', client='merch-machine', parallel=True).respond(
            keywords=DEFAULT_PROFILE
        )

        cls.dj.on_request(exp='inter_vertical_unknown', msid=Const.DEFAULT_REQ_ID, yandexuid='2768101',).respond(
            models=[
                DjModel(id=2768101, title='model 1'),
                DjModel(id=2768102, title='model 2'),
                DjModel(id=2768103, title='model 3'),
                DjModel(id=2768104, title='model 4'),
                DjModel(id=2768105, title='model 5'),
                DjModel(id=2768106, title='model 6'),
                DjModel(id=2768107, title='model 7'),
                DjModel(id=2768108, title='model 8'),
                DjModel(id=2768109, title='model 9'),
                DjModel(id=2768110, title='model 10'),
            ]
        )

    def test_light_premium_offer_warnings(self):
        """
        Проверяем скрытие премиальных офферов в случае наличия
        каких-либо дисклеймеров
        В данном случае все офферы (и синие и белые) скрыты за разные
        дисклеймеры
        """
        for place in ['place=omm_parallel&omm_place=yandexapp_vertical']:
            response = self.report.request_json(
                '{}&yandexuid=2768101&rearr-factors=market_po_in_omm_parallel=1&rids=213&show-urls=encrypted'.format(
                    place
                )
            )
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {'entity': 'product', 'id': 2768101},
                            {'entity': 'product', 'id': 2768102},
                            {'entity': 'product', 'id': 2768103},
                            {'entity': 'product', 'id': 2768104},
                            {'entity': 'product', 'id': 2768105},
                            {'entity': 'product', 'id': 2768106},
                            {'entity': 'product', 'id': 2768107},
                            {'entity': 'product', 'id': 2768108},
                            {'entity': 'product', 'id': 2768109},
                            {'entity': 'product', 'id': 2768110},
                        ]
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )

    @classmethod
    def prepare_wprid(cls):
        cls.dj.on_request(exp='inter_vertical_unknown', msid='', yandexuid='007',).respond(
            models=[
                DjModel(id=2694611, title='model 1'),
                DjModel(id=2694612, title='model 2'),
                DjModel(id=2694621, title='model 3'),
                DjModel(id=2694622, title='model 4'),
                DjModel(id=2694623, title='model 5'),
                DjModel(id=2694631, title='model 6'),
                DjModel(id=2694641, title='model 7'),
            ]
        )

    def test_wprid(self):
        """
        Проверяем, что wprid выводится в корневой секции выдачи и вариант
        с пустым reqid без wprid
        """
        for place in ['place=omm_parallel&omm_place=yandexapp_vertical']:
            response = self.report.request_json('{}&yandexuid=007'.format(place))
            self.assertFragmentIn(response, {"search": {"wprid": Const.DEFAULT_REQ_ID}})

        for place in ['place=omm_parallel&omm_place=yandexapp_vertical']:
            response = self.report.request_json('{}&yandexuid=007&reqid='.format(place))
            self.assertFragmentIn(response, {"search": {"wprid": Absent()}})

    def test_po_in_omm_parallel_unistat(self):
        """Проверяем, что запросы за премиальным оффером в
        place=omm_parallel влияют на unistat-счётчики
        """

        self._inspect_po_parallel_unistat(
            'place=omm_parallel&omm_place=yandexapp_vertical&yandexuid=2647001&rearr-factors=market_po_in_omm_parallel=1',
            'omm_parallel_yandexapp_vertical',
        )

        self._inspect_po_parallel_unistat(
            'place=omm_parallel&yandexuid=2647001&rearr-factors=market_po_in_omm_parallel=1',
            'omm_parallel_yandexapp_vertical',
        )

    @classmethod
    def prepare_profile_cutting(cls):
        model_views_last_seen = []
        model_views = []
        queries = []
        for seq in range(0, 10):
            cls.index.models += [Model(hyperid=2825900 + seq, title='model {}'.format(seq + 1))]

            cls.index.offers += [Offer(hyperid=2825900 + seq)]

            model_views_last_seen += [ModelLastSeenEvent(model_id=2825900 + seq, timestamp=200 + seq)]

            model_views += [ModelViewEvent(model_id=2825900 + seq, view_count=1000 - seq)]

            queries += [Query(query_id=2825900 + seq, query_text='Запрос за {}'.format(seq), update_time=200 + seq)]

        model_last_seen_counter = MarketModelLastTimeCounter(model_views_last_seen)
        model_views_counter = MarketModelViewsCounter(model_views)

        cls.bigb.on_request(yandexuid='2825901', client='merch-machine', parallel=True).respond(
            keywords=DEFAULT_PROFILE, counters=[model_last_seen_counter, model_views_counter], queries=queries
        )

        cls.dj.on_request(
            exp='inter_vertical_unknown',
            msid=Const.DEFAULT_REQ_ID,
            yandexuid='2825901',
        ).respond(models=[DjModel(id=2825900 + seq, title='model {}'.format(seq + 1)) for seq in range(0, 3)])

    def test_profile_cutting(self):
        """Проверяем, что при наличии флагов уменьшения профиля репорт отправляет
        сокращенный профиль при запросе в OMM
        Корректность уменьшенного профиля подтверждается наличием выдачи и отсутствием
        ошибок в логах
        """
        response = self.report.request_json('place=omm_parallel&omm_place=yandexapp_vertical&yandexuid=004')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "id": 1809702,
                        'offers': {
                            'count': 100,
                        },
                    }
                ]
            },
            allow_different_len=False,
        )

    def test_light_premium_offer_rgb_type(self):
        '''Делаем запрос с дебаг-выдачей
        Проверяем, что rgb_type = GREEN_WITH_BLUE при отстуствии &rgb в запросе
        '''
        RGB_TYPE_GREEN_WITH_BLUE = 'rgb_type: 2'
        for place in ['place=omm_parallel&omm_place=yandexapp_vertical']:
            response = self.report.request_json(
                '{}&yandexuid=2647001&rearr-factors=market_po_in_omm_parallel=1&rids=213&numdoc=3&debug=1'.format(place)
            )
            self.assertFragmentIn(response, RGB_TYPE_GREEN_WITH_BLUE)
            self.assertFragmentIn(response, {"search": {"pages": NotEmpty()}})

            pages = response.root["search"]["pages"]
            # page query
            for i, encrypted_ids in enumerate(pages):
                page_num = i + 1
                response = self.report.request_json(
                    '{}&page={}&encrypted-hyperids={}&yandexuid=2647001&rearr-factors=market_po_in_omm_parallel=1&rids=213&numdoc=3&debug=1'.format(
                        place, page_num, encrypted_ids
                    )
                )
                self.assertFragmentIn(response, RGB_TYPE_GREEN_WITH_BLUE)

    def test_shop_id_for_content_api(self):
        """
        Проверяем выдачу shop.id в премиального оффера под флагом
        &api=content
        """
        for place in [
            'place=omm_parallel&omm_place=yandexapp_vertical',
        ]:
            for rearr_factors in ['', ';market_use_flat_grouping_in_multi_premium=1']:
                response = self.report.request_json(
                    '{}&yandexuid=2647001&rearr-factors=market_po_in_omm_parallel=1{}&rids=213&show-urls=encrypted&numdoc=3&api=content'.format(
                        place, rearr_factors
                    )
                )
                self.assertFragmentIn(
                    response,
                    {
                        "results": [
                            {
                                'shop': {'id': 2647011, 'name': 'Белый магазин 2'},
                            },
                            {
                                'shop': {'id': 2647011, 'name': 'Белый магазин 2'},
                            },
                            {
                                'shop': {'id': 1886710, 'name': 'БЕРУ'},
                            },
                        ]
                    },
                    preserve_order=True,
                    allow_different_len=False,
                )

    def test_omm_parallel_snippet_request_count(self):
        base_request = (
            'place=omm_parallel&omm_place=yandexapp_vertical&yandexuid=003&rearr-factors=market_po_in_omm_parallel=1'
        )
        self.report.reset_unistats()

        self.report.request_json(base_request + ";ext_snippet_bulk_size=8")

        tass_data = self.report.request_tass()
        self.assertEqual(tass_data.get('Market_snippets_query_count_dmmm'), 4)  # 4 == 32 / 8 (ext_snippet_bulk_size)

        self.report.reset_unistats()

        self.report.request_json(base_request + ';ext_snippet_lazy_on_omm_parallel=1')
        tass_data = self.report.request_tass()
        self.assertEqual(tass_data.get('Market_snippets_query_count_dmmm'), 1)

    @classmethod
    def prepare_default_models(cls):
        """
        OMM ответ с пустым списком моделей и OMM ошибка
        """
        cls.index.models += [
            Model(hyperid=hyperid, title=title, picinfo=picture_link, add_picinfo=picture_link)
            for hyperid, title, picture_link in OMM_DEFAULT_MODELS
        ]
        cls.index.offers += [Offer(hyperid=hyperid) for hyperid, _, _ in OMM_DEFAULT_MODELS]
        cls.index.regional_models += [
            RegionalModel(hyperid=hyperid, offers=123) for hyperid, _, _ in OMM_DEFAULT_MODELS
        ]

        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.DJ_DEFAULT_MODELS_LIST,
                kind=YamarecPlace.Type.DJ_DEFAULT_MODELS_LIST,
                partitions=[
                    YamarecDjDefaultModelsList(dj_default_models_list=OMM_DEFAULT_MODELS),
                ],
            ),
        ]

        omm_options = [
            {'yandexuid': '019', 'keywords': DEFAULT_PROFILE},
            {'yandexuid': '029', 'keywords': []},
            {'yandexuid': '039', 'keywords': []},
            {'yandexuid': '0', 'keywords': []},
        ]

        for opts in omm_options:
            cls.bigb.on_request(yandexuid=opts['yandexuid'], client='merch-machine').respond(keywords=opts['keywords'])
            cls.bigb.on_request(yandexuid=opts['yandexuid'], client='merch-machine', parallel=True).respond(
                keywords=opts['keywords']
            )
            cls.dj.on_request(
                exp='inter_vertical_unknown',
                msid=Const.DEFAULT_REQ_ID,
                yandexuid=opts['yandexuid'],
            ).respond(models=[])

    def test_default_models(self):
        """
        Проверка тыквы для вертикалей
        """

        def to_thumbnails(picture_link):
            base_link = picture_link[:-4] if picture_link.endswith('/orig') else picture_link + '/'
            for th in ['6hq', '9hq']:
                yield base_link + th

        for yandexuid in ['019', '029', '039', '0']:
            for omm_place in ['place=omm_parallel&omm_place=yandexapp_vertical']:
                response = self.report.request_json(
                    '{place}&yandexuid={yandexuid}'.format(place=omm_place, yandexuid=yandexuid)
                )
                self.assertFragmentIn(
                    response,
                    {
                        "search": {
                            "results": [
                                {
                                    'entity': 'product',
                                    'id': hyperid,
                                    'titles': {'raw': title},
                                    'pictures': [
                                        {
                                            'entity': 'picture',
                                            'thumbnails': [
                                                {'url': thumb_url} for thumb_url in to_thumbnails(picture_link)
                                            ],
                                        }
                                    ],
                                }
                                for hyperid, title, picture_link in OMM_DEFAULT_MODELS[:16]
                            ],
                        }
                    },
                    preserve_order=False,
                    allow_different_len=True,
                )
        self.error_log.ignore('No recommendations from Dj')

    def test_pumpkin_split(self):
        """
        Проверка сплита со 100% тыквой для DJ
        """

        def to_thumbnails(picture_link):
            base_link = picture_link[:-4] if picture_link.endswith('/orig') else picture_link + '/'
            for th in ['6hq', '9hq']:
                yield base_link + th

        for omm_place in ['place=omm_parallel']:
            response = self.report.request_json(
                '{place}&yandexuid=29641001&rearr-factors=market_dj_exp_for_omm_parallel=inter10'.format(
                    place=omm_place
                )
            )
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {
                                'entity': 'product',
                                'id': hyperid,
                                'titles': {'raw': title},
                                'pictures': [
                                    {
                                        'entity': 'picture',
                                        'thumbnails': [{'url': thumb_url} for thumb_url in to_thumbnails(picture_link)],
                                    }
                                ],
                            }
                            for hyperid, title, picture_link in OMM_DEFAULT_MODELS[:16]
                        ],
                    }
                },
                preserve_order=False,
                allow_different_len=True,
            )

    @classmethod
    def prepare_pumpkin_orders_and_random(cls):
        counters = [
            BeruModelOrderLastTimeCounter(
                model_order_events=[
                    ModelLastOrderEvent(model_id=1725712742, timestamp=478418100),
                ]
            ),
        ]

        cls.bigb.on_request(puid='3253301', client='merch-machine').respond(counters=counters)
        cls.bigb.on_request(puid='3253301', client='merch-machine', parallel=True).respond(counters=counters)

    def test_pumpkin_orders_and_random(self):
        """
        Проверка экспериментов с рандомизацией тыквы
        и выфильтровыванем заказов
        """
        for omm_place in [
            'place=omm_parallel&omm_place=yandexapp_vertical',
            'place=omm_parallel&omm_place=yandexapp_vertical',
            'place=omm_parallel',
        ]:
            for dj_exp in ['', '&rearr-factors=market_dj_exp_for_omm_parallel=inter1']:
                response = self.report.request_json(
                    '{}&puid=3253301&numdoc=4&rearr-factors=market_omm_pumpkin_random=1&debug=1{}'.format(
                        omm_place, dj_exp
                    )
                )
                self.assertFragmentIn(
                    response,
                    {
                        "search": {
                            "results": [
                                {"id": 13177721},
                                {"id": 1719237819},
                                {"id": 1970873251},
                                {"id": 1901396991},
                            ]
                        }
                    },
                    preserve_order=True,
                    allow_different_len=True,
                )

                # Фильтруем заказ
                response = self.report.request_json(
                    '{}&puid=3253301&numdoc=4&rearr-factors=market_omm_pumpkin_random=1;market_omm_pumpkin_filter_orders=1&debug=1{}'.format(
                        omm_place, dj_exp
                    )
                )
                self.assertFragmentIn(
                    response,
                    {
                        "search": {
                            "results": [
                                {"id": 13177721},
                                {"id": 1719237819},
                                {"id": 1970873251},
                                {"id": 1901396991},
                            ]
                        }
                    },
                    preserve_order=True,
                    allow_different_len=True,
                )

        self.error_log.expect(code=3787).times(12)

    @classmethod
    def prepare_omm_parallel_black_friday(cls):

        cls.index.hypertree += [
            HyperCategory(
                hid=198119,
                name="Электроника",
                children=[
                    HyperCategory(hid=91491, name="Смартфоны"),
                ],
            ),
            HyperCategory(hid=90509, name="Красота"),
        ]

        cls.index.models += [
            Model(hyperid=914911, title='Iphone X', hid=91491),
            Model(hyperid=914912, title='Galaxy S20', hid=91491),
            Model(hyperid=905091, title='Крем для лица', hid=90509),
        ]

        cls.index.offers += [
            Offer(hyperid=914911, price=100),
            Offer(hyperid=914912, price=2000),
            Offer(hyperid=905091, price=200),
        ]

        cls.bigb.on_request(yandexuid='00023', client='merch-machine').respond(keywords=DEFAULT_PROFILE)
        cls.bigb.on_request(yandexuid='00023', client='merch-machine', parallel=True).respond(keywords=DEFAULT_PROFILE)

        cls.dj.on_request(exp='inter_vertical_black_friday', msid=Const.DEFAULT_REQ_ID, yandexuid='00023',).respond(
            models=[
                DjModel(id=914911, title='Iphone X', attributes={'hid': '91491'}),
                DjModel(id=914912, title='Galaxy S20', attributes={'hid': '91491'}),
                DjModel(id=905091, title='Крем для лица', attributes={'hid': '90509'}),
            ]
        )

    @skip('should be fixed next commit')
    def test_omm_parallel_black_friday(self):
        for omm_place in ['', 'yandexapp_parallel', 'yandexapp_vertical', 'browser_vertical']:
            response = self.report.request_json(
                'place=omm_parallel&omm_place={}&yandexuid=00023&pof=905&rearr-factors=market_black_friday_omm_parallel=1;market_dj_exp_for_omm_parallel=inter_vertical_black_friday'.format(
                    omm_place
                )
            )
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "departments": [
                            {
                                "title": "Электроника",
                                "url": "//m.pokupki.market.yandex.ru/special/black-friday-electron?clid=905",
                                "nid": 54440,
                            },
                            {
                                "title": "Красота и гигиена",
                                "url": "//m.pokupki.market.yandex.ru/special/black-friday-krasota?clid=905",
                                "nid": 54438,
                            },
                            {
                                "title": "Товары для животных",
                                "url": "//m.pokupki.market.yandex.ru/special/black-friday-pets?clid=905",
                                "nid": 81089,
                            },
                            {
                                "title": "Спорт и здоровье",
                                "url": "//m.pokupki.market.yandex.ru/special/black-friday-sport?clid=905",
                                "nid": 54436,
                            },
                            {
                                "title": "Часы и украшения",
                                "url": "//m.pokupki.market.yandex.ru/special/black-friday-chasy-acsessuary?clid=905",
                                "nid": 18057664,
                            },
                            {
                                "title": "Детские товары",
                                "url": "//m.pokupki.market.yandex.ru/special/black-friday-kids?clid=905",
                                "nid": 54421,
                            },
                            {
                                "title": "Одежда",
                                "url": "//m.pokupki.market.yandex.ru/special/black-friday-clothes?clid=905",
                                "nid": 54432,
                            },
                            {
                                "title": "Всё для ремонта и дачи",
                                "url": "//m.pokupki.market.yandex.ru/special/black-friday-dacha?clid=905",
                                "nid": 54503,
                            },
                            {
                                "title": "Бытовая техника",
                                "url": "//m.pokupki.market.yandex.ru/special/black-friday-appliances?clid=905",
                                "nid": 54419,
                            },
                            {
                                "title": "Хобби и творчество",
                                "url": "//m.pokupki.market.yandex.ru/special/black-friday-hobby?clid=905",
                                "nid": 54423,
                            },
                            {
                                "title": "Товары для авто",
                                "url": "//m.pokupki.market.yandex.ru/special/black-friday-avto?clid=905",
                                "nid": 54418,
                            },
                            {
                                "title": "Товары для дома",
                                "url": "//m.pokupki.market.yandex.ru/special/black-friday-dom?clid=905",
                                "nid": 54422,
                            },
                            {
                                "title": "Продукты",
                                "url": "//m.pokupki.market.yandex.ru/special/black-friday-food?clid=905",
                                "nid": 54434,
                            },
                        ],
                        "allDepartments": "//m.pokupki.market.yandex.ru/special/black-friday?clid=905",
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )

            response = self.report.request_json(
                'place=omm_parallel&omm_place={}&yandexuid=&pof=905&rearr-factors=market_black_friday_omm_parallel=1;market_dj_exp_for_omm_parallel=inter_vertical_black_friday'.format(
                    omm_place
                )
            )
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "departments": [
                            {
                                "title": "Спорт и здоровье",
                                "url": "//m.pokupki.market.yandex.ru/special/black-friday-sport?clid=905",
                                "nid": 54436,
                            },
                            {
                                "title": "Красота и гигиена",
                                "url": "//m.pokupki.market.yandex.ru/special/black-friday-krasota?clid=905",
                                "nid": 54438,
                            },
                            {
                                "title": "Детские товары",
                                "url": "//m.pokupki.market.yandex.ru/special/black-friday-kids?clid=905",
                                "nid": 54421,
                            },
                            {
                                "title": "Бытовая техника",
                                "url": "//m.pokupki.market.yandex.ru/special/black-friday-appliances?clid=905",
                                "nid": 54419,
                            },
                            {
                                "title": "Хобби и творчество",
                                "url": "//m.pokupki.market.yandex.ru/special/black-friday-hobby?clid=905",
                                "nid": 54423,
                            },
                            {
                                "title": "Одежда",
                                "url": "//m.pokupki.market.yandex.ru/special/black-friday-clothes?clid=905",
                                "nid": 54432,
                            },
                            {
                                "title": "Продукты",
                                "url": "//m.pokupki.market.yandex.ru/special/black-friday-food?clid=905",
                                "nid": 54434,
                            },
                            {
                                "title": "Товары для авто",
                                "url": "//m.pokupki.market.yandex.ru/special/black-friday-avto?clid=905",
                                "nid": 54418,
                            },
                            {
                                "title": "Товары для дома",
                                "url": "//m.pokupki.market.yandex.ru/special/black-friday-dom?clid=905",
                                "nid": 54422,
                            },
                            {
                                "title": "Часы и украшения",
                                "url": "//m.pokupki.market.yandex.ru/special/black-friday-chasy-acsessuary?clid=905",
                                "nid": 18057664,
                            },
                            {
                                "title": "Всё для ремонта и дачи",
                                "url": "//m.pokupki.market.yandex.ru/special/black-friday-dacha?clid=905",
                                "nid": 54503,
                            },
                            {
                                "title": "Электроника",
                                "url": "//m.pokupki.market.yandex.ru/special/black-friday-electron?clid=905",
                                "nid": 54440,
                            },
                            {
                                "title": "Товары для животных",
                                "url": "//m.pokupki.market.yandex.ru/special/black-friday-pets?clid=905",
                                "nid": 81089,
                            },
                        ],
                        "allDepartments": "//m.pokupki.market.yandex.ru/special/black-friday?clid=905",
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )

        self.error_log.expect(code=3787).times(4)

    @classmethod
    def prepare_pass_price_filter(cls):
        for seq in range(1, 4):
            cls.index.models += [
                Model(hyperid=2809700 + seq, title='Price filter ' + str(seq), hid=2809700),
            ]
            cls.index.offers += [
                Offer(hyperid=2809700 + seq, price=100 + seq * 10),
                Offer(hyperid=2809700 + seq, price=2000 + seq * 10),
            ]
            cls.index.mskus += [
                MarketSku(
                    hyperid=2809700 + seq,
                    sku=280970000 + seq,
                    blue_offers=[
                        BlueOffer(),
                    ],
                ),
            ]

        cls.bigb.on_request(yandexuid='009', client='merch-machine').respond(keywords=DEFAULT_PROFILE)
        cls.bigb.on_request(yandexuid='009', client='merch-machine', parallel=True).respond(keywords=DEFAULT_PROFILE)

        cls.dj.on_request(
            exp='blue_attractive_models_omm_parallel', msid=Const.DEFAULT_REQ_ID, yandexuid='009'
        ).respond(models=[DjModel(id=2404001)])
        cls.dj.on_request(
            exp='blue_attractive_models_omm_parallel', msid=Const.DEFAULT_REQ_ID, yandexuid='009', mcpricefrom='5000'
        ).respond(models=[DjModel(id=2404002)])
        cls.dj.on_request(
            exp='blue_attractive_models_omm_parallel', msid=Const.DEFAULT_REQ_ID, yandexuid='009', mcpriceto='6000'
        ).respond(models=[DjModel(id=2404003)])
        cls.dj.on_request(
            exp='blue_attractive_models_omm_parallel',
            msid=Const.DEFAULT_REQ_ID,
            yandexuid='009',
            mcpricefrom='5000',
            mcpriceto='6000',
        ).respond(models=[DjModel(id=2404004)])

    def test_pass_price_filter(self):
        response1 = self.report.request_json(
            "place=omm_parallel&rearr-factors=market_dj_exp_for_omm_parallel=blue_attractive_models_omm_parallel&yandexuid=009"
        )
        self.assertFragmentIn(
            response1,
            {'results': [{'entity': 'product', 'id': 2404001}]},
            preserve_order=True,
            allow_different_len=False,
        )
        response2 = self.report.request_json(
            "place=omm_parallel&rearr-factors=market_dj_exp_for_omm_parallel=blue_attractive_models_omm_parallel&yandexuid=009&mcpricefrom=5000"
        )
        self.assertFragmentIn(
            response2,
            {'results': [{'entity': 'product', 'id': 2404002}]},
            preserve_order=True,
            allow_different_len=False,
        )
        response3 = self.report.request_json(
            "place=omm_parallel&rearr-factors=market_dj_exp_for_omm_parallel=blue_attractive_models_omm_parallel&yandexuid=009&mcpriceto=6000"
        )
        self.assertFragmentIn(
            response3,
            {'results': [{'entity': 'product', 'id': 2404003}]},
            preserve_order=True,
            allow_different_len=False,
        )
        response4 = self.report.request_json(
            "place=omm_parallel&rearr-factors=market_dj_exp_for_omm_parallel=blue_attractive_models_omm_parallel&yandexuid=009&mcpricefrom=5000&mcpriceto=6000"
        )
        self.assertFragmentIn(
            response4,
            {'results': [{'entity': 'product', 'id': 2404004}]},
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_pass_boost_discount_abs_from(cls):

        cls.bigb.on_request(yandexuid='011', client='merch-machine').respond(keywords=DEFAULT_PROFILE)
        cls.bigb.on_request(yandexuid='011', client='merch-machine', parallel=True).respond(keywords=DEFAULT_PROFILE)

        cls.dj.on_request(
            exp='blue_attractive_models_omm_parallel', msid=Const.DEFAULT_REQ_ID, yandexuid='011'
        ).respond(models=[DjModel(id=2404011)])
        cls.dj.on_request(
            exp='blue_attractive_models_omm_parallel',
            msid=Const.DEFAULT_REQ_ID,
            yandexuid='011',
            boost_discount_abs_from='1000',
        ).respond(models=[DjModel(id=2404012)])
        cls.dj.on_request(
            exp='blue_attractive_models_omm_parallel',
            msid=Const.DEFAULT_REQ_ID,
            yandexuid='011',
            boost_discount_abs_from='2000',
        ).respond(models=[DjModel(id=2404013)])

    def test_pass_boost_discount_abs_from(self):
        response1 = self.report.request_json(
            "place=omm_parallel&rearr-factors=market_dj_exp_for_omm_parallel=blue_attractive_models_omm_parallel&yandexuid=011&debug=1"
        )
        self.assertFragmentIn(
            response1,
            {'results': [{'entity': 'product', 'id': 2404011}]},
            preserve_order=True,
            allow_different_len=False,
        )
        response2 = self.report.request_json(
            "place=omm_parallel&rearr-factors=market_dj_exp_for_omm_parallel=blue_attractive_models_omm_parallel&yandexuid=011&boost-discount-abs-from=1000"
        )
        self.assertFragmentIn(
            response2,
            {'results': [{'entity': 'product', 'id': 2404012}]},
            preserve_order=True,
            allow_different_len=False,
        )
        response3 = self.report.request_json(
            "place=omm_parallel&rearr-factors=market_dj_exp_for_omm_parallel=blue_attractive_models_omm_parallel&yandexuid=011&boost-discount-abs-from=2000"
        )
        self.assertFragmentIn(
            response3,
            {'results': [{'entity': 'product', 'id': 2404013}]},
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_pass_boost_discount_from(cls):

        cls.bigb.on_request(yandexuid='051', client='merch-machine').respond(keywords=DEFAULT_PROFILE)
        cls.bigb.on_request(yandexuid='051', client='merch-machine', parallel=True).respond(keywords=DEFAULT_PROFILE)

        cls.dj.on_request(
            exp='blue_attractive_models_omm_parallel', msid=Const.DEFAULT_REQ_ID, yandexuid='051'
        ).respond(models=[DjModel(id=2404010)])
        cls.dj.on_request(
            exp='blue_attractive_models_omm_parallel',
            msid=Const.DEFAULT_REQ_ID,
            yandexuid='051',
            boost_discount_from='10',
        ).respond(models=[DjModel(id=2404009)])
        cls.dj.on_request(
            exp='blue_attractive_models_omm_parallel',
            msid=Const.DEFAULT_REQ_ID,
            yandexuid='051',
            boost_discount_from='20',
        ).respond(models=[DjModel(id=2404008)])

    def test_pass_boost_discount_from(self):
        response1 = self.report.request_json(
            "place=omm_parallel&rearr-factors=market_dj_exp_for_omm_parallel=blue_attractive_models_omm_parallel&yandexuid=051&debug=1"
        )
        self.assertFragmentIn(
            response1,
            {'results': [{'entity': 'product', 'id': 2404010}]},
            preserve_order=True,
            allow_different_len=False,
        )
        response2 = self.report.request_json(
            "place=omm_parallel&rearr-factors=market_dj_exp_for_omm_parallel=blue_attractive_models_omm_parallel&yandexuid=051&boost-discount-from=10"
        )
        self.assertFragmentIn(
            response2,
            {'results': [{'entity': 'product', 'id': 2404009}]},
            preserve_order=True,
            allow_different_len=False,
        )
        response3 = self.report.request_json(
            "place=omm_parallel&rearr-factors=market_dj_exp_for_omm_parallel=blue_attractive_models_omm_parallel&yandexuid=051&boost-discount-from=20"
        )
        self.assertFragmentIn(
            response3,
            {'results': [{'entity': 'product', 'id': 2404008}]},
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_price_filtration_for_blue_models(cls):
        dj_models = []
        for seq in range(1, 10):
            cls.index.models += [
                Model(hyperid=2809800 + seq, title='Price filter ' + str(seq), hid=2809800),
            ]
            cls.index.offers += [
                Offer(hyperid=2809800 + seq, price=100 + seq * 10),
                Offer(hyperid=2809800 + seq, price=2000 + seq * 10),
            ]
            cls.index.mskus += [
                MarketSku(
                    hyperid=2809800 + seq,
                    sku=280980000 + seq,
                    blue_offers=[
                        BlueOffer(price=1000 + seq * 10),
                    ],
                ),
            ]
            dj_models.append(DjModel(id=2809800 + seq))
        cls.bigb.on_request(yandexuid='010', client='merch-machine').respond(keywords=DEFAULT_PROFILE)
        cls.bigb.on_request(yandexuid='010', client='merch-machine', parallel=True).respond(keywords=DEFAULT_PROFILE)

        cls.dj.on_request(
            exp='blue_attractive_models_omm_parallel', msid=Const.DEFAULT_REQ_ID, yandexuid='010'
        ).respond(models=dj_models)

    def test_price_filtration_for_blue_models(self):
        response = self.report.request_json(
            "place=omm_parallel&omm_place=marketblock_web&rearr-factors=market_dj_exp_for_omm_parallel=blue_attractive_models_omm_parallel&yandexuid=010&rgb=blue&mcpricefrom=5000"
        )
        self.assertFragmentIn(response, {'results': []}, preserve_order=True, allow_different_len=False)
        response = self.report.request_json(
            "place=omm_parallel&omm_place=marketblock_web&rearr-factors=market_dj_exp_for_omm_parallel=blue_attractive_models_omm_parallel&yandexuid=010&rgb=blue&mcpriceto=500"
        )
        self.assertFragmentIn(response, {'results': []}, preserve_order=True, allow_different_len=False)
        response1 = self.report.request_json(
            "place=omm_parallel&omm_place=marketblock_web&rearr-factors=market_dj_exp_for_omm_parallel=blue_attractive_models_omm_parallel&yandexuid=010&rgb=blue"
        )
        self.assertFragmentIn(
            response1,
            {
                'results': [
                    {'entity': 'product', 'id': 2809801},
                    {'entity': 'product', 'id': 2809802},
                    {'entity': 'product', 'id': 2809803},
                    {'entity': 'product', 'id': 2809804},
                    {'entity': 'product', 'id': 2809805},
                    {'entity': 'product', 'id': 2809806},
                    {'entity': 'product', 'id': 2809807},
                    {'entity': 'product', 'id': 2809808},
                    {'entity': 'product', 'id': 2809809},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )
        response2 = self.report.request_json(
            "place=omm_parallel&omm_place=marketblock_web&rearr-factors=market_dj_exp_for_omm_parallel=blue_attractive_models_omm_parallel&yandexuid=010&rgb=blue&mcpricefrom=1020"
        )
        self.assertFragmentIn(
            response2,
            {
                'results': [
                    {'entity': 'product', 'id': 2809802},
                    {'entity': 'product', 'id': 2809803},
                    {'entity': 'product', 'id': 2809804},
                    {'entity': 'product', 'id': 2809805},
                    {'entity': 'product', 'id': 2809806},
                    {'entity': 'product', 'id': 2809807},
                    {'entity': 'product', 'id': 2809808},
                    {'entity': 'product', 'id': 2809809},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )
        response3 = self.report.request_json(
            "place=omm_parallel&omm_place=marketblock_web&rearr-factors=market_dj_exp_for_omm_parallel=blue_attractive_models_omm_parallel&yandexuid=010&rgb=blue&mcpriceto=1020"
        )
        self.assertFragmentIn(
            response3,
            {
                'results': [
                    {'entity': 'product', 'id': 2809801},
                    {'entity': 'product', 'id': 2809802},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )
        response4 = self.report.request_json(
            "place=omm_parallel&omm_place=marketblock_web&rearr-factors=market_dj_exp_for_omm_parallel=blue_attractive_models_omm_parallel&yandexuid=010&rgb=blue&mcpricefrom=1020&mcpriceto=1030"
        )
        self.assertFragmentIn(
            response4,
            {'results': [{'entity': 'product', 'id': 2809802}, {'entity': 'product', 'id': 2809803}]},
            preserve_order=True,
            allow_different_len=False,
        )
        # chech for extended result with mcprice*
        response5 = self.report.request_json(
            "place=omm_parallel&omm_place=marketblock_web&rearr-factors=market_dj_exp_for_omm_parallel=blue_attractive_models_omm_parallel&yandexuid=010&rgb=blue&mcpricefrom=1030&numdoc=3"
        )
        self.assertFragmentIn(
            response5,
            {'results': [{'entity': 'product', 'id': 2809803}]},
            preserve_order=True,
            allow_different_len=False,
        )
        # negative omm_parallel_blue_models_factor must be ignored. response6 must be identical to response5
        response6 = self.report.request_json(
            "place=omm_parallel&omm_place=marketblock_web&rearr-factors=market_dj_exp_for_omm_parallel=blue_attractive_models_omm_parallel;omm_parallel_blue_models_factor=-1.5&yandexuid=010&rgb=blue&mcpricefrom=1030&numdoc=3"  # noqa
        )
        self.assertFragmentIn(
            response6,
            {'results': [{'entity': 'product', 'id': 2809803}]},
            preserve_order=True,
            allow_different_len=False,
        )
        self.error_log.expect(code=4800)
        # omm_parallel_blue_models_factor in action - one more result in response
        response7 = self.report.request_json(
            "place=omm_parallel&omm_place=marketblock_web&rearr-factors=market_dj_exp_for_omm_parallel=blue_attractive_models_omm_parallel;omm_parallel_blue_models_factor=1.5&yandexuid=010&rgb=blue&mcpricefrom=1030&numdoc=3"  # noqa
        )
        self.assertFragmentIn(
            response7,
            {'results': [{'entity': 'product', 'id': 2809803}, {'entity': 'product', 'id': 2809804}]},
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_pass_boost_discount(cls):
        cls.bigb.on_request(yandexuid='053', client='merch-machine').respond(keywords=DEFAULT_PROFILE)
        cls.bigb.on_request(yandexuid='053', client='merch-machine', parallel=True).respond(keywords=DEFAULT_PROFILE)

        cls.dj.on_request(exp='discovery_block_omm_parallel', msid=Const.DEFAULT_REQ_ID, yandexuid='053').respond(
            models=[DjModel(id=2404014)]
        )
        cls.dj.on_request(
            exp='discovery_block_omm_parallel', msid=Const.DEFAULT_REQ_ID, yandexuid='053', boost_discount='1'
        ).respond(models=[DjModel(id=2404015)])
        cls.dj.on_request(
            exp='discovery_block_omm_parallel', msid=Const.DEFAULT_REQ_ID, yandexuid='053', boost_discount='0'
        ).respond(models=[DjModel(id=2404013)])
        cls.dj.on_request(
            exp='discovery_block_omm_parallel', msid=Const.DEFAULT_REQ_ID, yandexuid='053', boost_discount='5'
        ).respond(models=[DjModel(id=2404012)])
        cls.dj.on_request(
            exp='discovery_block_omm_parallel', msid=Const.DEFAULT_REQ_ID, yandexuid='053', boost_discount='gg'
        ).respond(models=[DjModel(id=2404011)])

    def test_pass_boost_discount(self):
        response1 = self.report.request_json(
            "place=omm_parallel&rearr-factors=market_dj_exp_for_omm_parallel=discovery_block_omm_parallel&yandexuid=053"
        )
        self.assertFragmentIn(
            response1,
            {'results': [{'entity': 'product', 'id': 2404014}]},
            preserve_order=True,
            allow_different_len=False,
        )
        response2 = self.report.request_json(
            "place=omm_parallel&rearr-factors=market_dj_exp_for_omm_parallel=discovery_block_omm_parallel&yandexuid=053&boost-discount=1"
        )
        self.assertFragmentIn(
            response2,
            {'results': [{'entity': 'product', 'id': 2404015}]},
            preserve_order=True,
            allow_different_len=False,
        )
        response3 = self.report.request_json(
            "place=omm_parallel&rearr-factors=market_dj_exp_for_omm_parallel=discovery_block_omm_parallel&yandexuid=053&boost-discount=0"
        )
        self.assertFragmentIn(
            response3,
            {'results': [{'entity': 'product', 'id': 2404014}]},
            preserve_order=True,
            allow_different_len=False,
        )
        response4 = self.report.request_json(
            "place=omm_parallel&rearr-factors=market_dj_exp_for_omm_parallel=discovery_block_omm_parallel&yandexuid=053&boost-discount=5"
        )
        self.assertFragmentIn(
            response4,
            {'results': [{'entity': 'product', 'id': 2404014}]},
            preserve_order=True,
            allow_different_len=False,
        )
        response5 = self.report.request_json(
            "place=omm_parallel&rearr-factors=market_dj_exp_for_omm_parallel=discovery_block_omm_parallel&yandexuid=053&boost-discount=5"
        )
        self.assertFragmentIn(
            response5,
            {'results': [{'entity': 'product', 'id': 2404014}]},
            preserve_order=True,
            allow_different_len=False,
        )

    def test_cpa_real_omm_parallel_format(self):
        """Проверяем cpa=real"""
        for place_rearr in ['omm_parallel&rearr-factors=market_yandexapp_blue_vertical=1']:
            response = self.report.request_json('place={}&yandexuid=24040001&cpa=real&numdoc=3'.format(place_rearr))
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            self.get_blue_omm_parallel_model(0),
                            self.get_blue_omm_parallel_model(1),
                            self.get_blue_omm_parallel_model(2),
                        ]
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )

    @classmethod
    def prepare_omm_parallel_categories_mix(cls):
        dj_categories = []
        for idx in range(10):
            step = (idx % 5) + 1
            for i in range(20):
                hid = 1000 * idx + 100
                hyperid = 1000 * idx + 100 + i
                sku = 100000 * idx + 100 + i
                cls.index.models += [
                    Model(hyperid=hyperid, title='Модель для проверки актуальности категории ' + str(idx), hid=hid),
                ]
                if i % step == 0:
                    cls.index.offers += [
                        Offer(hyperid=hyperid, price=300 + idx * 10),
                    ]
                    cls.index.mskus += [
                        MarketSku(
                            hyperid=hyperid,
                            sku=sku,
                            blue_offers=[
                                BlueOffer(price=1000 + idx * 10),
                            ],
                        ),
                    ]

            dj_categories.append(
                {
                    'title': 'Категория ' + str(idx),
                    'nid': 200 + idx,
                    'model_pictures': [
                        {
                            'url': '//avatars.mds.yandex.net/get-mpic/{}/img_id{}.jpeg/orig'.format(
                                1000 * idx + i, 10000 * idx + i
                            ),
                            'model_id': 1000 * idx + i + 100,
                        }
                        for i in range(20)
                    ],
                }
            )

        dj_models = []
        for idx in range(15):
            cls.index.models += [
                Model(hyperid=2909800 + idx, title='Модель для микса с категориями ' + str(idx), hid=2909800),
            ]
            cls.index.offers += [
                Offer(hyperid=2909800 + idx, price=300 + idx * 10),
            ]
            cls.index.mskus += [
                MarketSku(
                    hyperid=2909800 + idx,
                    sku=290980000 + idx,
                    blue_offers=[
                        BlueOffer(price=1000 + idx * 10),
                    ],
                ),
            ]
            dj_models.append(
                DjModel(
                    id=2909800 + idx,
                    title='Модель для микса с категориями ' + str(idx),
                    attributes={'nid': str(200 + idx % 10)},
                )
            )

        cls.dj.on_request(
            exp='blue_attractive_models_omm_parallel',
            msid=Const.DEFAULT_REQ_ID,
            yandexuid='1337',
            enhance_with_categories='true',
        ).respond(
            models=dj_models,
            categories=dj_categories,
        )

        cls.dj.on_request(
            exp='blue_attractive_models_omm_parallel', msid=Const.DEFAULT_REQ_ID, yandexuid='1337'
        ).respond(models=dj_models, categories=[])

    def get_omm_parallel_model_for_categories_mix(self, index, clid=None, touch=True, has_waremd5=False, lr=None):
        seq = index
        model_id = 2909800 + seq
        parts = []
        if clid:
            parts.append('clid={}'.format(clid))
        if has_waremd5:
            parts.append('do-waremd5={}'.format('WARE_{:02d}______________g'.format(seq)))
        if lr:
            parts.append('lr={}'.format(lr))
        parts.append('wprid={}'.format(Const.DEFAULT_REQ_ID))

        touch_str = 'm.' if touch else ''
        slug_str = 'product--model-dlia-miksa-s-kategoriiami-{}'.format(seq)

        res = {
            "entity": "product",
            "titles": {"raw": "Модель для микса с категориями {}".format(seq)},
            "urls": {"direct": "//{}market.yandex.ru/{}/{}?{}".format(touch_str, slug_str, model_id, '&'.join(parts))},
            "id": model_id,
            "prices": {
                "min": str(300 + index * 10),
                "max": str(1000 + index * 10),
                "currency": "RUR",
                "avg": str(1000 + index * 10),
                "discount": Absent(),
            },
            "promos": Absent(),
        }
        return res

    def get_omm_parallel_relevant_pictures_for_category_for_categories_mix(self, idx):
        step = (idx % 5) + 1
        pictures = []
        for index in range(4):
            i = step * index
            pictures.append(
                '//avatars.mds.yandex.net/get-mpic/{}/img_id{}.jpeg/orig'.format(1000 * idx + i, 10000 * idx + i)
            )
        return pictures

    def get_omm_parallel_category_for_categories_mix(self, index, touch=True, cpa=True):
        slug = 'catalog--kategoriia-{}'.format(index)
        touch_str = 'm.' if touch else ''
        nid = 200 + index

        parts = []
        if cpa:
            parts.append('cpa=1')
        parts.append('wprid={}'.format(Const.DEFAULT_REQ_ID))

        res = {
            'entity': 'category',
            'title': 'Категория {}'.format(index),
            'nid': nid,
            'pictures': self.get_omm_parallel_relevant_pictures_for_category_for_categories_mix(index),
            'url': '//{}market.yandex.ru/{}/{}/list?{}'.format(touch_str, slug, nid, '&'.join(parts)),
        }
        return res

    def test_omm_parallel_categories_mix(self):
        response_0_5 = self.report.request_json(
            "place=omm_parallel&rearr-factors=market_dj_exp_for_omm_parallel=blue_attractive_models_omm_parallel&yandexuid=1337&cpa=real&category-mix-saturation=0.5"
        )
        self.assertFragmentIn(
            response_0_5,
            {
                "search": {
                    "results": [
                        self.get_omm_parallel_category_for_categories_mix(0),
                        self.get_omm_parallel_model_for_categories_mix(1),
                        self.get_omm_parallel_category_for_categories_mix(2),
                        self.get_omm_parallel_model_for_categories_mix(3),
                        self.get_omm_parallel_category_for_categories_mix(4),
                        self.get_omm_parallel_model_for_categories_mix(5),
                        self.get_omm_parallel_category_for_categories_mix(6),
                        self.get_omm_parallel_model_for_categories_mix(7),
                        self.get_omm_parallel_category_for_categories_mix(8),
                        self.get_omm_parallel_model_for_categories_mix(9),
                        self.get_omm_parallel_model_for_categories_mix(10),
                        self.get_omm_parallel_category_for_categories_mix(1),
                        self.get_omm_parallel_model_for_categories_mix(12),
                        self.get_omm_parallel_category_for_categories_mix(3),
                        self.get_omm_parallel_model_for_categories_mix(14),
                    ]
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response_0_0 = self.report.request_json(
            "place=omm_parallel&rearr-factors=market_dj_exp_for_omm_parallel=blue_attractive_models_omm_parallel&yandexuid=1337&cpa=real&category-mix-saturation=0"
        )
        self.assertFragmentIn(
            response_0_0,
            {
                "search": {
                    "results": [
                        self.get_omm_parallel_model_for_categories_mix(0),
                        self.get_omm_parallel_model_for_categories_mix(1),
                        self.get_omm_parallel_model_for_categories_mix(2),
                        self.get_omm_parallel_model_for_categories_mix(3),
                        self.get_omm_parallel_model_for_categories_mix(4),
                        self.get_omm_parallel_model_for_categories_mix(5),
                        self.get_omm_parallel_model_for_categories_mix(6),
                        self.get_omm_parallel_model_for_categories_mix(7),
                        self.get_omm_parallel_model_for_categories_mix(8),
                        self.get_omm_parallel_model_for_categories_mix(9),
                        self.get_omm_parallel_model_for_categories_mix(10),
                        self.get_omm_parallel_model_for_categories_mix(11),
                        self.get_omm_parallel_model_for_categories_mix(12),
                        self.get_omm_parallel_model_for_categories_mix(13),
                        self.get_omm_parallel_model_for_categories_mix(14),
                    ]
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response_1_0 = self.report.request_json(
            "place=omm_parallel&rearr-factors=market_dj_exp_for_omm_parallel=blue_attractive_models_omm_parallel&yandexuid=1337&cpa=real&category-mix-saturation=1.0"
        )
        self.assertFragmentIn(
            response_1_0,
            {
                "search": {
                    "results": [
                        self.get_omm_parallel_category_for_categories_mix(0),
                        self.get_omm_parallel_category_for_categories_mix(1),
                        self.get_omm_parallel_category_for_categories_mix(2),
                        self.get_omm_parallel_category_for_categories_mix(3),
                        self.get_omm_parallel_category_for_categories_mix(4),
                        self.get_omm_parallel_category_for_categories_mix(5),
                        self.get_omm_parallel_category_for_categories_mix(6),
                        self.get_omm_parallel_category_for_categories_mix(7),
                        self.get_omm_parallel_category_for_categories_mix(8),
                        self.get_omm_parallel_category_for_categories_mix(9),
                        self.get_omm_parallel_model_for_categories_mix(10),
                        self.get_omm_parallel_model_for_categories_mix(11),
                        self.get_omm_parallel_model_for_categories_mix(12),
                        self.get_omm_parallel_model_for_categories_mix(13),
                        self.get_omm_parallel_model_for_categories_mix(14),
                    ]
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_dj_response_with_no_titles_and_pictures(cls):
        cls.bigb.on_request(yandexuid='053', client='merch-machine').respond(keywords=DEFAULT_PROFILE)
        cls.bigb.on_request(yandexuid='053', client='merch-machine', parallel=True).respond(keywords=DEFAULT_PROFILE)

        cls.dj.on_request(exp='models_with_no_title_and_picture', msid=Const.DEFAULT_REQ_ID, yandexuid='053').respond(
            models=[
                DjModel(id=2404011),
                DjModel(id=2404012).clear_title(),
                DjModel(id=2404013).clear_picture(),
                DjModel(id=2404014).clear_title().clear_picture(),
            ]
        )

    def test_dj_response_with_no_titles_and_pictures(self):
        response = self.report.request_json(
            "place=omm_parallel&rearr-factors=market_dj_exp_for_omm_parallel=models_with_no_title_and_picture&yandexuid=053"
        )
        self.assertFragmentIn(
            response,
            {'results': [{'entity': 'product', 'id': 2404011}]},
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_omm_parallel_rearr_factors_category_model_pictures_num_and_with_pins(cls):
        dj_categories = []
        for idx in range(1, 7):
            for i in range(5 * idx):
                hid = 1000 * idx + 200
                hyperid = 1000 * idx + i + 200
                sku = 100000 * idx + i + 200
                cls.index.models += [
                    Model(
                        hyperid=hyperid,
                        title='Модель для проверки фактора количества моделей в категории ' + str(idx * 10),
                        hid=hid,
                    ),
                ]
                cls.index.offers += [
                    Offer(hyperid=hyperid, price=300 + idx * 10),
                ]
                cls.index.mskus += [
                    MarketSku(
                        hyperid=1000 * idx + i + 200,
                        sku=sku,
                        blue_offers=[
                            BlueOffer(price=1000 + idx * 10),
                        ],
                    ),
                ]

            dj_categories.append(
                {
                    'title': 'Категория ' + str(idx),
                    'nid': 200 + idx,
                    'model_pictures': [
                        {
                            'url': '//avatars.mds.yandex.net/get-mpic/{}/img_id{}.jpeg/orig'.format(
                                1000 * idx + i, 10000 * idx + i
                            ),
                            'model_id': 1000 * idx + i + 200,
                        }
                        for i in range(5 * idx)
                    ],
                }
            )

        dj_models = []
        for idx in range(1, 7):
            cls.index.models += [
                Model(
                    hyperid=3909800 + idx,
                    title='Модель для проверки фактора количества моделей в категории ' + str(idx),
                    hid=3909800,
                ),
            ]
            cls.index.offers += [
                Offer(hyperid=3909800 + idx, price=300 + idx * 10),
            ]
            cls.index.mskus += [
                MarketSku(
                    hyperid=3909800 + idx,
                    sku=390980000 + idx,
                    blue_offers=[
                        BlueOffer(price=1000 + idx * 10),
                    ],
                ),
            ]
            dj_models.append(
                DjModel(
                    id=3909800 + idx,
                    title='Модель для проверки фактора количества моделей в категории ' + str(idx),
                    attributes={'nid': str(200 + idx % 10)},
                )
            )

        for i in range(1, 7):
            cls.dj.on_request(
                exp='blue_attractive_models_omm_parallel',
                msid=Const.DEFAULT_REQ_ID,
                yandexuid='1337',
                enhance_with_categories='true',
                category_model_pictures_num=str(5 * i),
            ).respond(models=[dj_models[i - 1]], categories=[dj_categories[i - 1]])

    def get_omm_parallel_category_for_rearr_factor_category_model_pictures_num(self, index, touch=True, cpa=True):
        slug = 'catalog--kategoriia-{}'.format(index)
        touch_str = 'm.' if touch else ''
        nid = 200 + index

        parts = []
        if cpa:
            parts.append('cpa=1')
        parts.append('wprid={}'.format(Const.DEFAULT_REQ_ID))

        res = {
            'entity': 'category',
            'title': 'Категория {}'.format(index),
            'nid': nid,
            'pictures': [
                '//avatars.mds.yandex.net/get-mpic/{}/img_id{}.jpeg/orig'.format(1000 * index + i, 10000 * index + i)
                for i in range(4)
            ],
            'url': '//{}market.yandex.ru/{}/{}/list?{}'.format(touch_str, slug, nid, '&'.join(parts)),
        }
        return res

    def get_omm_parallel_category_for_rearr_factor_with_pins(self, index, touch=True, cpa=True):
        slug = 'catalog--kategoriia-{}'.format(index)
        touch_str = 'm.' if touch else ''
        nid = 200 + index

        parts = []
        if cpa:
            parts.append('cpa=1')
        parts.append('rs=*')
        parts.append('wprid={}'.format(Const.DEFAULT_REQ_ID))

        res = {
            'entity': 'category',
            'title': 'Категория {}'.format(index),
            'nid': nid,
            'pictures': [
                '//avatars.mds.yandex.net/get-mpic/{}/img_id{}.jpeg/orig'.format(1000 * index + i, 10000 * index + i)
                for i in range(4)
            ],
            'url': Wildcard('//{}market.yandex.ru/{}/{}/list?{}'.format(touch_str, slug, nid, '&'.join(parts))),
        }
        return res

    def test_omm_parallel_rearr_factor_category_model_pictures_num(self):
        for i in range(1, 7):
            response = self.report.request_json(
                "place=omm_parallel&rearr-factors=market_dj_exp_for_omm_parallel=blue_attractive_models_omm_parallel;omm_parallel_category_model_pictures_num={}&yandexuid=1337&cpa=real&category-mix-saturation=1".format(  # noqa
                    5 * i
                )
            )
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [self.get_omm_parallel_category_for_rearr_factor_category_model_pictures_num(i)]
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )

    def test_omm_parallel_rearr_factor_category_mix_with_pins(self):
        for i in range(1, 7):
            response = self.report.request_json(
                "place=omm_parallel&rearr-factors=market_dj_exp_for_omm_parallel=blue_attractive_models_omm_parallel;omm_parallel_category_model_pictures_num={};omm_parallel_category_mix_with_pins=true&yandexuid=1337&cpa=real&category-mix-saturation=1".format(  # noqa
                    5 * i
                )
            )
            self.assertFragmentIn(
                response,
                {"search": {"results": [self.get_omm_parallel_category_for_rearr_factor_with_pins(i)]}},
                preserve_order=True,
                allow_different_len=False,
            )

    def test_show_log_generation(self):
        self.report.request_json('place=omm_parallel&yandexuid=003&numdoc=1')
        self.show_log.expect(record_type=1, hyper_id=1786501, index_generation=self.index.fixed_index_generation)


if __name__ == '__main__':
    main()
