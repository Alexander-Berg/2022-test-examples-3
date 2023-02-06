#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import (
    DeliveryBucket,
    DeliveryOption,
    Model,
    Offer,
    Opinion,
    Picture,
    RegionalDelivery,
    Shop,
    Vendor,
    VendorLogo,
    VirtualModel,
)
from core.types.catalog import Category
from core.types.picture import thumbnails_config
from core.matcher import (
    Absent,
    NotEmpty,
    Contains,
)


CPA_ONLY_DISABLE = '&rearr-factors=market_cards_everywhere_cpa_only=0'


class T(TestCase):
    """
    Проверка создания модели для офферов, которые не привязаны к моделям
    Карточки везде - https://st.yandex-team.ru/MARKETOUT-33760

    ВАЖНО!
    СЕЙЧАС ВСЕ ФЛАГИ ВИРТУАЛЬНЫХ КАРТОЧЕК ПО-УМОЛЧАНИЮ ВКЛЮЧЕНЫ
    """

    def make_request(self, request_base):
        return self.report.request_json(request_base + CPA_ONLY_DISABLE)

    @classmethod
    def prepare_data(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.virtual_models += [
            VirtualModel(
                virtual_model_id=100500,
                opinion=Opinion(total_count=44, rating=4.3, precise_rating=4.31, rating_count=43, reviews=3),
            ),
            VirtualModel(
                virtual_model_id=100505,
            ),
        ]

        cls.index.models += [
            Model(
                hyperid=500100,
                hid=12,
                full_description="non-virtial model",
                title='Простая вещь',
            )
        ]

        cls.index.shops += [
            Shop(fesh=3476000, cpa=Shop.CPA_NO),
            Shop(fesh=42500),
            Shop(28887, cpa=Shop.CPA_REAL, name="Все коты"),
        ]

        cls.index.vendors += [
            Vendor(
                vendor_id=2,
                name='Panaphonic',
                webpage_recommended_shops='http://www.beko.ru/recommended-online-stores.html',
                description='VendorDescription',
                logos=[VendorLogo(url='//mdata.yandex.net/i?path=b0726220734_img_id5949902134120952009.png')],
                website="http://www.beko.ru/",
            )
        ]

        cls.index.categories += [
            Category(hyper_id=777, name='goods'),
        ]

        cls.index.offers += [
            Offer(
                title='Крутая вещь',
                fesh=3476000,
                waremd5='WhiteCpc_____________g',
                virtual_model_id=100500,
                hid=42,
            ),
            Offer(
                title='Мечта поэта',
                fesh=42500,
                price=123,
                vendor_id=2,
                hid=777,
                picture=Picture(
                    picture_id='iyC4nHslqLtqZJLygVAHeA',
                    width=200,
                    height=200,
                    thumb_mask=thumbnails_config.get_mask_by_names(['200x200']),
                    group_id=1234,
                ),
                virtual_model_id=100501,
            ),
            Offer(
                title='Сихофазатрон резонансный',
                fesh=42500,
                price=123,
                vendor_id=2,
                hid=777,
            ),
            Offer(
                title="Кот с dsbs",
                fesh=28887,
                virtual_model_id=100505,
                cpa=Offer.CPA_REAL,
                delivery_buckets=[4240],
            ),
            Offer(
                fesh=5825,
                hyperid=500100,
            ),
            Offer(
                title="Оффер без картинки",
                fesh=28887,
                virtual_model_id=100506,
                waremd5='offer_pic_vmid_fc0__mQ',
                cpa=Offer.CPA_REAL,
                delivery_buckets=[4240],
                no_picture=True,
            ),
            Offer(
                title='Оффер быстрокарточки без картинки',
                fesh=12708,
                price=165,
                waremd5='offer_pic_vmid_fc1__mQ',
                sku=1005006,
                virtual_model_id=1005006,
                vmid_is_literal=False,
                no_picture=True,
            ),
        ]

    def test_noflag_query(self):
        """
        Проверка, что при выключенном флаге market_cards_everywhere_prime офферы не меняется
        """

        _ = CPA_ONLY_DISABLE
        response = self.make_request(
            'place=prime&text=Крутая&rearr-factors=market_cards_everywhere_prime=0&allow-collapsing=1'
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "totalModels": 0,
                    "totalOffers": 1,
                    "results": [
                        {
                            "entity": "offer",
                            "titles": {"raw": "Крутая вещь"},
                            "slug": "krutaia-veshch",
                        }
                    ],
                }
            },
        )

        response = self.make_request(
            'place=prime&vendor_id=2&rearr-factors=market_cards_everywhere_prime=0&allow-collapsing=1'
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "totalModels": 0,
                    "totalOffers": 2,
                    "results": [
                        {
                            "entity": "offer",
                            "titles": {"raw": "Мечта поэта"},
                            "slug": "mechta-poeta",
                            "prices": {"currency": "RUR", "value": "123", "rawValue": "123"},
                            "vendor": {
                                "entity": "vendor",
                                "id": 2,
                                "name": "Panaphonic",
                                "description": "VendorDescription",
                                "logo": {
                                    "entity": "picture",
                                    "url": "//mdata.yandex.net/i?path=b0726220734_img_id5949902134120952009.png",
                                },
                                "webpageRecommendedShops": "http://www.beko.ru/recommended-online-stores.html",
                                "website": "http://www.beko.ru/",
                            },
                            "pictures": [
                                {
                                    "entity": "picture",
                                    "thumbnails": [
                                        {
                                            "containerWidth": 200,
                                            "containerHeight": 200,
                                            "url": "http://avatars.mdst.yandex.net/get-marketpic/1234/market_iyC4nHslqLtqZJLygVAHeA/200x200",
                                            "width": 200,
                                            "height": 200,
                                        }
                                    ],
                                }
                            ],
                            "categories": [
                                {
                                    "entity": "category",
                                    "id": 777,
                                }
                            ],
                        },
                        {
                            "entity": "offer",
                            "titles": {"raw": "Сихофазатрон резонансный"},
                        },
                    ],
                }
            },
        )

    def test_query_with_flag(self):
        """
        Проверка создания модели по оферу при выставленном флаге market_cards_everywhere_prime
        офера без virtual_model_id в модели не превращаются

        Схлопывание до вирт КМ на прайме работает по аналогии с параметром enableCollapsing, который расчитывается в плейса prime
        """
        right_ans_3476000 = {
            "search": {
                "totalModels": 0,
                "totalOffers": 1,
                "results": [
                    {
                        "entity": "product",
                        "id": 100500,
                        "type": "model",
                        "titles": {"raw": "Крутая вещь"},
                        "slug": "krutaia-veshch",
                        "description": "",
                        "isNew": False,
                        "opinions": 44,
                        "rating": 4.3,
                        "preciseRating": 4.31,
                        "ratingCount": 43,
                        "reviews": 3,
                        "prices": {"avg": "100", "currency": "RUR", "max": "100", "min": "100"},
                        "vendor": {
                            "entity": "vendor",
                            "id": NotEmpty(),
                        },
                        "categories": [
                            {
                                "cpaType": "cpc_and_cpa",
                                "entity": "category",
                                "isLeaf": True,
                                "kinds": [],
                                "type": "simple",
                            }
                        ],
                        "offers": {
                            "count": 1,
                            "items": [
                                {
                                    "model": {"id": 100500},
                                    "prices": {
                                        "value": "100",
                                    },
                                    "slug": "krutaia-veshch",
                                    "shop": {"id": 3476000},
                                    "titles": {"raw": "Крутая вещь"},
                                }
                            ],
                        },
                        "urls": {"direct": Contains("//market.yandex.ru/product/100500?hid=42&nid=")},
                        "showUid": NotEmpty(),
                    }
                ],
            }
        }

        for use_fast_cards in ['', ';use_fast_cards=1']:
            response = self.make_request(
                'place=prime&text=Крутая&rearr-factors=market_cards_everywhere_prime=1;market_cards_everywhere_range=100500:100505{}&allow-collapsing=1&use-default-offers=1'.format(
                    use_fast_cards
                )
            )
            self.assertFragmentIn(response, right_ans_3476000)

            # Если параметр allow-collapsing выставлен в ноль, то не отрисовываем виртуальные карточки
            response = self.make_request(
                'place=prime&text=Крутая&rearr-factors=market_cards_everywhere_prime=1{}&allow-collapsing=0'.format(
                    use_fast_cards
                )
            )
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "entity": "offer",
                            "titles": {"raw": "Крутая вещь"},
                        }
                    ]
                },
            )

            response = self.make_request(
                'place=prime&vendor_id=2&rearr-factors=market_cards_everywhere_prime=1;market_cards_everywhere_range=100500:100505{}&allow-collapsing=1'.format(
                    use_fast_cards
                )
            )
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "totalModels": 0,
                        "totalOffers": 2,
                        "results": [
                            {
                                "entity": "product",
                                "id": 100501,
                                "type": "model",
                                "titles": {"raw": "Мечта поэта"},
                                "slug": "mechta-poeta",
                                "description": "",
                                "isNew": False,
                                "prices": {"avg": "123", "currency": "RUR", "max": "123", "min": "123"},
                                "vendor": {"entity": "vendor", "id": 2, "name": "Panaphonic", "slug": "panaphonic"},
                                "categories": [
                                    {
                                        "cpaType": "cpc_and_cpa",
                                        "entity": "category",
                                        "fullName": "UNIQ-HID-777",
                                        "id": 777,
                                        "isLeaf": True,
                                        "kinds": [],
                                        "name": "HID-777",
                                        "slug": "hid-777",
                                        "type": "simple",
                                    }
                                ],
                                "pictures": [
                                    {
                                        "entity": "picture",
                                        "thumbnails": [
                                            {
                                                "containerWidth": 200,
                                                "containerHeight": 200,
                                                "url": "http://avatars.mdst.yandex.net/get-marketpic/1234/market_iyC4nHslqLtqZJLygVAHeA/200x200",
                                                "width": 200,
                                                "height": 200,
                                            }
                                        ],
                                    }
                                ],
                                "offers": {
                                    "count": 1,
                                    "items": [
                                        {
                                            "model": {"id": 100501},
                                            "prices": {
                                                "value": "123",
                                            },
                                            "slug": "mechta-poeta",
                                            "shop": {"id": 42500},
                                            "titles": {"raw": "Мечта поэта"},
                                        }
                                    ],
                                },
                                "urls": {"direct": Contains("//market.yandex.ru/product/100501?hid=777&nid=")},
                                "showUid": NotEmpty(),
                            },
                            {
                                "entity": "offer",
                                "titles": {"raw": "Сихофазатрон резонансный"},
                            },
                        ],
                    }
                },
            )

    def test_virtuality_tag(self):
        """
        Проверка формирования признака виртуальности модели.
        У обычных моделей он должен быть false, у виртуальных - true.
        """

        # Поиск по тексту, ожидаем найти виртуальную модель
        response = self.make_request(
            'place=prime&text=Крутая&rearr-factors=market_cards_everywhere_prime=1&allow-collapsing=1'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "isVirtual": True,
                        "titles": {"raw": "Крутая вещь"},
                    }
                ]
            },
        )

        non_virtual_ans = {
            "results": [
                {
                    "isVirtual": False,
                    "titles": {"raw": "Простая вещь"},
                }
            ]
        }

        # Поиск по тексту, ожидаем найти обычную (не виртуальную) модель
        response = self.make_request(
            'place=prime&text=Простая&rearr-factors=market_cards_everywhere_prime=1&allow-collapsing=1'
        )
        self.assertFragmentIn(response, non_virtual_ans)

        # Поиск по hyperid, ожидаем найти обычную (не виртуальную) модель
        response = self.make_request(
            'place=prime&hyperid=500100&rearr-factors=market_cards_everywhere_prime=1&allow-collapsing=1'
        )
        self.assertFragmentIn(response, non_virtual_ans)

    @classmethod
    def prepare_wcpa(cls):
        cls.index.shops += [
            Shop(
                fesh=42,
                datafeed_id=4240,
                priority_region=213,
                regions=[213],
                name='Все мечи',
                client_id=11,
                cpa=Shop.CPA_REAL,
            )
        ]

        cls.index.offers += [
            Offer(
                title="Меч с msku 00",
                fesh=42,
                hyperid=1580,
                sku=2000000100000,
                waremd5='WhiteCpaWithMSKU____0g',
                price=1000,
                cpa=Offer.CPA_REAL,
                delivery_buckets=[4240],
            ),
            Offer(
                title="Меч без msku 11",
                fesh=42,
                hyperid=1590,
                waremd5='WhiteCpaWithoutMSKU_0g',
                price=1000,
                cpa=Offer.CPA_REAL,
                delivery_buckets=[4240],
            ),
            Offer(
                title="Меч cpc 11",
                fesh=123,
                hyperid=1591,
                waremd5='WhiteCpcWithoutMSKU_0g',
            ),
            Offer(
                title="Меч без msku, но с vmid 22",
                fesh=42,
                hyperid=1592,
                virtual_model_id=2000000200000,
                waremd5='WhiteCpaWithoutMSKU_1g',
                price=1000,
                cpa=Offer.CPA_REAL,
                delivery_buckets=[4240],
            ),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=4240,
                fesh=42,
                carriers=[99],
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=100, day_from=1, day_to=1)])],
            ),
        ]

    def test_hids_restrictions(self):
        # хиды не указаны - модель строится
        response = self.make_request(
            'place=prime&text=Крутая&rearr-factors=market_cards_everywhere_prime=1&allow-collapsing=1'
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "totalModels": 0,
                    "totalOffers": 1,
                    "results": [
                        {
                            "entity": "product",
                            "id": 100500,
                        }
                    ],
                }
            },
        )
        # указан подходящий хид - модель строится
        response = self.make_request(
            'place=prime&text=Крутая&rearr-factors=market_cards_everywhere_prime=1;market_cards_everywhere_hids=1,42&allow-collapsing=1'
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "totalModels": 0,
                    "totalOffers": 1,
                    "results": [
                        {
                            "entity": "product",
                            "id": 100500,
                        }
                    ],
                }
            },
        )
        # указаны не подходящие хиды - модель не строится
        response = self.make_request(
            'place=prime&text=Крутая&rearr-factors=market_cards_everywhere_prime=1;market_cards_everywhere_hids=1,2&allow-collapsing=1'
        )
        self.assertFragmentNotIn(
            response,
            {
                "search": {
                    "totalModels": 0,
                    "totalOffers": 1,
                    "results": [
                        {
                            "entity": "product",
                            "id": 100500,
                        }
                    ],
                }
            },
        )

    def test_cpaonly_flag(self):
        # market_cards_everywhere_cpa_only выключен
        # модель построилась, оффер не cpa
        response = self.report.request_json(
            'place=prime&text=Крутая&rearr-factors=market_cards_everywhere_prime=1;market_cards_everywhere_cpa_only=0&allow-collapsing=1'
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "totalModels": 0,
                    "totalOffers": 1,
                    "results": [
                        {
                            "entity": "product",
                            "id": 100500,
                        }
                    ],
                }
            },
        )
        # модель построилась, оффер дсбс
        response = self.report.request_json(
            'place=prime&text=Кот&rearr-factors=market_cards_everywhere_prime=1;market_cards_everywhere_cpa_only=0&allow-collapsing=1'
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "totalModels": 0,
                    "totalOffers": 1,
                    "results": [
                        {
                            "entity": "product",
                            "id": 100505,
                        }
                    ],
                }
            },
        )

        # market_cards_everywhere_cpa_only включен
        # модель не построилась, тк оффер не cpa
        response = self.report.request_json(
            'place=prime&text=Крутая&rearr-factors=market_cards_everywhere_prime=1;market_cards_everywhere_cpa_only=1;market_cards_everywhere_productoffers=1&allow-collapsing=1'
        )
        self.assertFragmentNotIn(
            response,
            {
                "search": {
                    "totalModels": 0,
                    "totalOffers": 1,
                    "results": [
                        {
                            "entity": "product",
                            "id": 100500,
                        }
                    ],
                }
            },
        )
        # Также для не cpa офферов не должен отрисовываться блок model с виртуальным айдишником
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "totalModels": 0,
                    "totalOffers": 1,
                    "results": [
                        {
                            "entity": "offer",
                            "wareId": "WhiteCpc_____________g",
                            "model": Absent(),
                        }
                    ],
                }
            },
        )

    def test_dsbsonly_flag(self):
        # market_cards_everywhere_dsbs_only выключен
        # модель построилась, оффер не дсбс
        response = self.make_request(
            'place=prime&text=Крутая&rearr-factors=market_cards_everywhere_prime=1;market_cards_everywhere_dsbs_only=0&allow-collapsing=1'
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "totalModels": 0,
                    "totalOffers": 1,
                    "results": [
                        {
                            "entity": "product",
                            "id": 100500,
                        }
                    ],
                }
            },
        )
        # модель построилась, оффер дсбс
        response = self.make_request(
            'place=prime&text=Кот&rearr-factors=market_cards_everywhere_prime=1;market_cards_everywhere_dsbs_only=0&allow-collapsing=1'
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "totalModels": 0,
                    "totalOffers": 1,
                    "results": [
                        {
                            "entity": "product",
                            "id": 100505,
                        }
                    ],
                }
            },
        )

        # market_cards_everywhere_dsbs_only включен
        # модель не построилась, тк оффер не дсбс
        response = self.make_request(
            'place=prime&text=Крутая&rearr-factors=market_cards_everywhere_prime=1;market_cards_everywhere_dsbs_only=1;market_cards_everywhere_productoffers=1&allow-collapsing=1'
        )
        self.assertFragmentNotIn(
            response,
            {
                "search": {
                    "totalModels": 0,
                    "totalOffers": 1,
                    "results": [
                        {
                            "entity": "product",
                            "id": 100500,
                        }
                    ],
                }
            },
        )
        # Также для не dsbs офферов не должен отрисовываться блок model с виртуальным айдишником
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "totalModels": 0,
                    "totalOffers": 1,
                    "results": [
                        {
                            "entity": "offer",
                            "wareId": "WhiteCpc_____________g",
                            "model": Absent(),
                        }
                    ],
                }
            },
        )

    def test_virtual_msku_id_showing(self):
        """1) Проверяем, что показываем виртуальные msku только под флагом market_show_virtual_msku_id
        2) Проверяем, что значение mksu из флага market_set_virtual_msku_id проставляется только для белых cpa офферов
        """

        # Без флага market_show_virtual_msku_id msku, которые лежат в market_cards_everywhere_range (т.е. "виртуальные") не должны показываться
        # С флагом показываем
        # тк флаги теперь включены по-умолчанию, можно не указывать явно
        for rgb in ['green', 'blue']:
            response = self.make_request(
                'place=prime&text=00&rgb={}&rids=213&rearr-factors=market_show_virtual_msku_id=1;market_cards_everywhere_range=2000000000000:2100000000000&rearr-factors=market_metadoc_search=no'.format(  # noqa
                    rgb
                )
            )
            self.assertFragmentIn(
                response,
                {
                    "cpa": "real",
                    "entity": "offer",
                    "marketSku": "2000000100000",
                    "sku": "2000000100000",
                    "offerColor": "white",
                    "wareId": "WhiteCpaWithMSKU____0g",
                },
            )

        # Также с флагом market_show_virtual_msku_id при наличии вмида мы рисуем его в sku
        for rgb in ['green', 'blue']:
            response = self.make_request(
                'place=prime&text=22&rgb={}&rids=213&rearr-factors=market_show_virtual_msku_id=1;market_cards_everywhere_range=2000000000000:2100000000000'.format(
                    rgb
                )
            )
            self.assertFragmentIn(
                response,
                {
                    "cpa": "real",
                    "entity": "offer",
                    "marketSku": "2000000200000",
                    "sku": "2000000200000",
                    "offerColor": "white",
                    "wareId": "WhiteCpaWithoutMSKU_1g",
                },
            )

        # С флагом market_set_virtual_msku_id у белых cpa офферов отрисуется виртуальный msku, чтобы фронт пошел в sku_offers
        # сейчас это нужно для тестирования, чтобы не ждать индексатор
        response = self.make_request(
            'place=prime&text=11&rgb=blue&rids=213&rearr-factors=market_set_virtual_msku_id=2000000000010;market_show_virtual_msku_id=1;market_cards_everywhere_range=2000000000000:2100000000000'
        )
        self.assertFragmentIn(
            response,
            {
                "cpa": "real",
                "entity": "offer",
                "marketSku": "2000000000010",
                "sku": "2000000000010",
                "offerColor": "white",
                "wareId": "WhiteCpaWithoutMSKU_0g",
            },
        )

        # Но без флага market_show_virtual_msku_id не покажем
        response = self.make_request(
            'place=prime&text=11&rgb=blue&rids=213&rearr-factors=market_set_virtual_msku_id=2000000000010;market_cards_everywhere_range=2000000000000:2100000000000;market_show_virtual_msku_id=0'
        )
        self.assertFragmentIn(
            response,
            {
                "cpa": "real",
                "entity": "offer",
                "marketSku": Absent(),
                "sku": Absent(),
                "offerColor": "white",
                "wareId": "WhiteCpaWithoutMSKU_0g",
            },
        )

        # Также у cpc офферов мы не проставим виртуальный msku
        response = self.make_request(
            'place=prime&text=11&rgb=green&rearr-factors=market_set_virtual_msku_id=2000000000010;market_show_virtual_msku_id=1;market_cards_everywhere_range=2000000000000:2100000000000'
        )
        self.assertFragmentIn(
            response,
            [
                {
                    "cpa": "real",
                    "entity": "offer",
                    "marketSku": "2000000000010",
                    "sku": "2000000000010",
                    "offerColor": "white",
                    "wareId": "WhiteCpaWithoutMSKU_0g",
                },
                {
                    "entity": "offer",
                    "marketSku": Absent(),
                    "sku": Absent(),
                    "offerColor": "white",
                    "wareId": "WhiteCpcWithoutMSKU_0g",
                },
            ],
        )

    @classmethod
    def prepare_fast_cards(cls):
        cls.index.virtual_models += [
            VirtualModel(
                virtual_model_id=1581,
                opinion=Opinion(total_count=45, rating=4.5, precise_rating=4.51, rating_count=45, reviews=5),
            ),
            VirtualModel(
                virtual_model_id=1582,
                opinion=Opinion(total_count=45, rating=4.5, precise_rating=4.51, rating_count=45, reviews=5),
            ),
        ]

        cls.index.shops += [
            Shop(fesh=12708, priority_region=213),
            Shop(fesh=12709, priority_region=213),
            Shop(fesh=12710, datafeed_id=14240, priority_region=213, regions=[213], client_id=12, cpa=Shop.CPA_REAL),
            Shop(
                fesh=12711,
                datafeed_id=14241,
                priority_region=213,
                regions=[213],
                client_id=13,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
            ),
            Shop(fesh=12712, datafeed_id=14242, priority_region=213, regions=[213], client_id=14, cpa=Shop.CPA_REAL),
            Shop(
                fesh=12713,
                datafeed_id=14243,
                priority_region=213,
                regions=[213],
                client_id=15,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
            ),
            Shop(
                fesh=12714,
                datafeed_id=14244,
                priority_region=213,
                regions=[213],
                client_id=16,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
            ),
        ]

        cls.index.offers += [
            Offer(
                fesh=12708,
                price=165,
                waremd5='offer_cpc_vmid_fc0__mQ',
                title='Оффер быстрокарточки 1581 cpc - 0',
                sku=1581,
                virtual_model_id=1581,
                vmid_is_literal=False,
            ),
            Offer(
                fesh=12709,
                price=175,
                waremd5='offer_cpc_vmid_fc1__mQ',
                title='Оффер быстрокарточки 1581 cpc - 1',
                sku=1581,
                virtual_model_id=1581,
                vmid_is_literal=False,
            ),
            Offer(
                fesh=12710,
                waremd5='offer_cpa_vmid_fc0__mQ',
                title='Оффер быстрокарточки 1581 cpa - 0',
                price=150,
                cpa=Offer.CPA_REAL,
                delivery_buckets=[14240],
                sku=1581,
                virtual_model_id=1581,
                vmid_is_literal=False,
            ),
            Offer(
                fesh=12712,
                waremd5='offer_cpa_vmid_fc1__mQ',
                title='Оффер быстрокарточки 1581 cpa - 1',
                price=155,
                cpa=Offer.CPA_REAL,
                delivery_buckets=[14242],
                sku=1581,
                virtual_model_id=1581,
                vmid_is_literal=False,
            ),
            Offer(
                fesh=12711,
                waremd5='offer_blue_vmid_fc0_mQ',
                title='Оффер быстрокарточки 1581 blue - 0',
                price=250,
                delivery_buckets=[14241],
                sku=1581,
                virtual_model_id=1581,
                blue_without_real_sku=True,
                vmid_is_literal=False,
            ),
            Offer(
                fesh=12711,
                waremd5='offer_blue_vmid_fc1_mQ',
                title='Оффер быстрокарточки 1581 blue - 1',
                price=350,
                delivery_buckets=[14241],
                sku=1581,
                virtual_model_id=1581,
                blue_without_real_sku=True,
                vmid_is_literal=False,
            ),
            # только синие
            Offer(
                fesh=12713,
                waremd5='offer_blue_vmid_fc2_mQ',
                title='Оффер быстрокарточки 1582 blue - 0',
                price=10,
                delivery_buckets=[14243],
                sku=1582,
                virtual_model_id=1582,
                blue_without_real_sku=True,
                vmid_is_literal=False,
            ),
            Offer(
                fesh=12714,
                waremd5='offer_blue_vmid_fc3_mQ',
                title='Оффер быстрокарточки 1582 blue - 1',
                price=20,
                delivery_buckets=[14244],
                sku=1582,
                virtual_model_id=1582,
                blue_without_real_sku=True,
                vmid_is_literal=False,
            ),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=14240,
                fesh=12710,
                carriers=[99],
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=100, day_from=1, day_to=1)])],
            ),
            DeliveryBucket(
                bucket_id=14241,
                fesh=12711,
                carriers=[99],
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=100, day_from=1, day_to=1)])],
            ),
            DeliveryBucket(
                bucket_id=14242,
                fesh=12712,
                carriers=[99],
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=100, day_from=1, day_to=1)])],
            ),
            DeliveryBucket(
                bucket_id=14243,
                fesh=12711,
                carriers=[99],
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=100, day_from=1, day_to=1)])],
            ),
            DeliveryBucket(
                bucket_id=14244,
                fesh=12711,
                carriers=[99],
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=100, day_from=1, day_to=1)])],
            ),
        ]

    def test_fast_cards_prime(self):
        '''
        Появились новые типы вуртуальных карточек - Быстрые карточки
        Их айдишник неотлечим от sku и лежит в нем же
        Но у офферов с скюшкой быстрых карточек в extraData лежит VirtualModelId == sku
        Однако литерала vmid нет

        Проверям их работу на прайме
        Под флагом: use_fast_cards
        '''

        flags = '&rearr-factors=use_fast_cards=1'
        # Быстрая карточка на прайме должна строиться из оффера с минимальным ts
        # В данном случае это Оффер быстрокарточки 1581 cpc - 0
        for search_type in ['text=1581', 'hyperid=1581', 'market-sku=1581']:
            response = self.report.request_json('place=prime&{}&allow-collapsing=1'.format(search_type) + flags)
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "total": 1,
                        "results": [
                            {
                                "entity": "product",
                                "id": 1581,
                                "isVirtual": True,
                                "modelCreator": "partner",
                                "offers": {
                                    "count": 6,
                                    "items": [
                                        {
                                            "marketSku": "1581",
                                            "marketSkuCreator": "virtual",
                                            "model": {"id": 1581},
                                            "sku": "1581",
                                            "wareId": "offer_cpc_vmid_fc0__mQ",
                                        }
                                    ],
                                },
                                "opinions": 45,
                                "preciseRating": 4.51,
                                "prices": {"avg": "250", "currency": "RUR", "max": "350", "min": "150"},
                                "rating": 4.5,
                                "ratingCount": 45,
                                "reviews": 5,
                                "titles": {
                                    # Строим из оффера с минимальным ts
                                    "raw": "Оффер быстрокарточки 1581 cpc - 0"
                                },
                                "type": "model",
                            }
                        ],
                    }
                },
                allow_different_len=False,
            )

        # запрос без &allow-collapsing=1, на выдаче должно быть 5 офферов (1 синий уходит из-за байбокса)
        response = self.report.request_json('place=prime&hyperid=1581' + flags)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 6,
                    "results": [
                        {
                            "entity": "offer",
                            "wareId": "offer_cpc_vmid_fc0__mQ",
                        },
                        {
                            "entity": "offer",
                            "wareId": "offer_cpc_vmid_fc1__mQ",
                        },
                        {
                            "entity": "offer",
                            "wareId": "offer_cpa_vmid_fc0__mQ",
                        },
                        {
                            "entity": "offer",
                            "wareId": "offer_cpa_vmid_fc1__mQ",
                        },
                        {
                            "entity": "offer",
                            "wareId": "offer_blue_vmid_fc0_mQ",
                        },
                        {
                            "entity": "offer",
                            "wareId": "offer_blue_vmid_fc1_mQ",
                        },
                    ],
                }
            },
            allow_different_len=False,
        )

        # запрос без флага use_fast_cards - пустая выдача
        response = self.report.request_json('place=prime&hyperid=1581&rearr-factors=use_fast_cards=0')
        self.assertFragmentIn(response, {"search": {"total": 0, "results": []}}, allow_different_len=False)

        # проверяем, что обычные модельки работают под флагом
        response = self.make_request('place=prime&hyperid=500100&allow-collapsing=1' + flags)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "results": [
                        {
                            "isVirtual": False,
                            "titles": {"raw": "Простая вещь"},
                        }
                    ],
                }
            },
            allow_different_len=False,
        )

        # Проверяем с rgb=blue
        for search_type in ['text=1582', 'hyperid=1582', 'market-sku=1582']:
            response = self.report.request_json(
                'place=prime&{}&allow-collapsing=1&rgb=blue'.format(search_type) + flags
            )
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "total": 1,
                        "results": [
                            {
                                "entity": "product",
                                "id": 1582,
                                "isVirtual": True,
                                "modelCreator": "partner",
                                "offers": {
                                    # Кажеься, тут норм, что total == 1, тк на синем отключен metadoc поиск
                                    # И байбокс фильтрует все оффера, кроме одного
                                    "count": 1,
                                    "items": [
                                        {
                                            "marketSku": "1582",
                                            "marketSkuCreator": "virtual",
                                            "model": {"id": 1582},
                                            "sku": "1582",
                                            "wareId": "offer_blue_vmid_fc2_mQ",
                                        }
                                    ],
                                },
                                "opinions": 45,
                                "preciseRating": 4.51,
                                "prices": {"avg": "10", "currency": "RUR", "max": "10", "min": "10"},
                                "rating": 4.5,
                                "ratingCount": 45,
                                "reviews": 5,
                                "titles": {
                                    # Строим из оффера с минимальным ts
                                    "raw": "Оффер быстрокарточки 1582 blue - 0"
                                },
                                "type": "model",
                            }
                        ],
                    }
                },
                allow_different_len=False,
            )

        # проверяем, что для БК работает поход за ДО
        response = self.report.request_json('place=prime&text=1581&allow-collapsing=1&use-default-offers=1' + flags)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "results": [
                        {
                            "entity": "product",
                            "id": 1581,
                            "isVirtual": True,
                            "modelCreator": "partner",
                            "offers": {
                                # Все cpa, кроме самого cpa дешевого отфильтровались по байбоксу
                                "count": 3,
                                "items": [
                                    {
                                        "marketSku": "1581",
                                        "marketSkuCreator": "virtual",
                                        "model": {"id": 1581},
                                        "sku": "1581",
                                        "wareId": "offer_cpa_vmid_fc0__mQ",
                                    }
                                ],
                            },
                            "opinions": 45,
                            "preciseRating": 4.51,
                            "prices": {"avg": "163", "currency": "RUR", "max": "175", "min": "150"},
                            "rating": 4.5,
                            "ratingCount": 45,
                            "reviews": 5,
                            "titles": {
                                # Строим из оффера с минимальным ts
                                "raw": "Оффер быстрокарточки 1581 cpc - 0"
                            },
                            "type": "model",
                        }
                    ],
                }
            },
            allow_different_len=False,
        )

    def test_no_pictures_flag(self):
        '''
        MARKETOUT-46141
        Запускаем проект, нашли, что фронт пятисотит на виртуальных карточках без картинок
        Временно под флагом не будем их возвращать на выдачу, чтобы мониторинги не горели
        '''
        # Без флага карточки будут
        response = self.make_request(
            'place=prime&text=картинки&rearr-factors=market_cards_everywhere_prime=1;market_no_virt_wo_pic=0&allow-collapsing=1'
        )

        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 2,
                    "results": [
                        {
                            "entity": "product",
                            "id": 100506,
                        },
                        {
                            "entity": "product",
                            "id": 1005006,
                        },
                    ],
                }
            },
            allow_different_len=False,
        )

        # С флагом пустая выдача
        response = self.make_request(
            'place=prime&text=картинки&rearr-factors=market_cards_everywhere_prime=1;market_no_virt_wo_pic=1&allow-collapsing=1'
        )

        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 2,
                    "results": [
                        {"entity": "offer", "wareId": 'offer_pic_vmid_fc0__mQ'},
                        {"entity": "offer", "wareId": 'offer_pic_vmid_fc1__mQ'},
                    ],
                }
            },
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()
