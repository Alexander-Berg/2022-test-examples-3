#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import (
    CardCategory,
    HyperCategory,
    HyperCategoryType,
    MnPlace,
    Model,
    ModelGroup,
    NavCategory,
    Offer,
    Opinion,
    Region,
    RegionalModel,
    Shop,
    Vendor,
    VendorLogo,
)
from core.matcher import ElementCount, LikeUrl, NoKey
from core.types.picture import to_mbo_picture


class T(TestCase):
    @classmethod
    def prepare(cls):
        # создаем регион
        cls.index.regiontree += [
            Region(rid=1),
        ]

        # создаем магазин
        cls.index.shops += [
            Shop(fesh=101, priority_region=1),
        ]

        # Создаем категории
        cls.index.hypertree += [
            HyperCategory(hid=201, name='Guru category', output_type=HyperCategoryType.GURU),
            HyperCategory(hid=202, name='Category 1'),
        ]

        # создаем модели
        cls.index.models += [
            Model(hid=201, hyperid=301, title='OfferModel', ts=301),
            Model(hid=202, hyperid=302, title='OfferModel 2', ts=302),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 301).respond(0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 302).respond(0.8)

        cls.index.cards += [CardCategory(hid=201, vendor_ids=[10, 11, 12])]

        # создаем офферы
        cls.index.offers += [
            Offer(hyperid=301, fesh=101, title="Offer", price=100),
            Offer(hyperid=302, fesh=101, title="Offer", price=200),
        ]

    def test_offers_wizard_titles(self):
        """
        Проверка новых title офферного колдунщика
        https://st.yandex-team.ru/MARKETOUT-13261
        """

        def test_title(request, titles):
            # Подстановка флагов из flags и titles в request и проверка функцией из flags
            for title_flag, title_text in titles.items():
                response = self.report.request_bs(request.format(title_flag))
                self.assertFragmentIn(response, {"market_offers_wizard": [{"title": title_text}]})

        # Проверка формирования title общего вида
        request = "place=parallel&rids=1&pp=18&text=Offer&rearr-factors=market_offers_wizard_title_common={}"
        titles = {
            1: "Найдено 2 предложения на Маркете",
            2: "Яндекс.Маркет — 2 предложения",
            3: "Найдено 2 предложения в 2 категориях на Маркете",
        }
        test_title(request, titles)

        # Проверка формирования title с информацией о категории
        request = "place=parallel&rids=1&pp=18&text=Offer&rearr-factors=market_offers_wizard_title_categories={}"
        titles = {
            1: "Guru category и прочие товары на Яндекс.Маркете",
            2: "Guru category, Cat... и прочие товары на Яндекс.Маркете",
            3: "Guru category и другие товары на Яндекс.Маркете",
            4: "Guru category, Cat... и другие товары на Яндекс.Маркете",
        }
        test_title(request, titles)

    @classmethod
    def prepare_top_models(cls):
        '''
        Подготовка для проверки количества моделей в категорийном и категорийно-вендорном колдунщиках
        MARKETOUT-13604
        https://st.yandex-team.ru/MARKETOUT-14128
        '''

        # Добавляем категорию
        cls.index.hypertree += [
            HyperCategory(hid=1001, name='TopModels category', output_type=HyperCategoryType.GURU),
        ]

        models_count = 7  # Количество моделей для тестирования
        for i in range(models_count):
            # Добавляем модели
            cls.index.models += [
                Model(
                    hid=1001,
                    vendor_id=1201,
                    hyperid=1101 + i,
                    title='TopModel {0}'.format(i),
                    proto_picture=to_mbo_picture(
                        '//avatars.mds.yandex.net/get-mpic/top_model_{}_0/orig#100#100'.format(1101 + i)
                    ),
                    proto_add_pictures=[
                        to_mbo_picture(
                            '//avatars.mds.yandex.net/get-mpic/top_model_{}_1/orig#100#100'.format(1101 + i)
                        ),
                        to_mbo_picture(
                            '//avatars.mds.yandex.net/get-mpic/top_model_{}_2/orig#100#100'.format(1101 + i)
                        ),
                    ],
                    opinion=Opinion(rating=i + 1, total_count=i),
                    ts=1101 + i,
                ),
            ]
            # Добавляем по офферу к каждой модели
            cls.index.offers += [Offer(hyperid=1101 + i, price=100)]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1101).respond(0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1102).respond(0.8)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1103).respond(0.7)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1104).respond(0.6)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1105).respond(0.5)

        cls.index.cards += [
            # Добавляем категорийную карточку
            CardCategory(
                hid=1001,
                hyperids=[1101 + i for i in range(models_count)],  # Список всех моделей
                vendor_ids=[1201, 1202, 1203],
            ),
        ]

    def test_top_models(self):
        '''
        Проверка количества топовых моделей в категорийном и категорийно-вендорном колдунщиках
        MARKETOUT-13604
        '''
        default_models_count = 4

        response = self.report.request_bs("place=parallel&text=TopModels category")
        # Проверка количества топовых моделей в категорийном колдунщике по дефолту
        self.assertFragmentIn(
            response, {"market_ext_category": [{"showcase": {"top_models": ElementCount(default_models_count)}}]}
        )

        for models_count in range(3, 6):
            request = (
                "place=parallel&text=TopModels category&rearr-factors="
                "market_ext_category_wizard_top_models_count={0};market_category_vendor_wizard_top_models_count={0}".format(
                    models_count
                )
            )
            response = self.report.request_bs(request)
            # Проверка количества топовых моделей в категорийном колдунщике
            self.assertFragmentIn(
                response, {"market_ext_category": [{"showcase": {"top_models": ElementCount(models_count)}}]}
            )

    def test_implicit_model_wizard_top_models_count(self):
        """Проверка количества топ моделей в колдунщике неявной модели под конструктором
        https://st.yandex-team.ru/MARKETOUT-14064
        https://st.yandex-team.ru/SERP-105416
        """
        default_models_count = 6

        response = self.report.request_bs('place=parallel&text=TopModel')
        # Проверка количества топ моделей в колдунщике неявной модели по дефолту
        self.assertFragmentIn(
            response, {"market_implicit_model": [{"showcase": {"items": ElementCount(default_models_count)}}]}
        )

        for models_count in range(5, 8):
            request = (
                "place=parallel&text=TopModel&rearr-factors="
                "market_implicit_wizard_top_models_count={0}".format(models_count)
            )
            response = self.report.request_bs(request)
            # Проверка количества топ моделей в колдунщике неявной модели
            self.assertFragmentIn(
                response, {"market_implicit_model": [{"showcase": {"items": ElementCount(models_count)}}]}
            )

    def test_top_models_pictures(self):
        """
        Проверка наличия картинок топовых моделей в категорийном, категорийно-вендорном и неяаной модели колдунщиках
        https://st.yandex-team.ru/MARKETOUT-15492
        """
        for size in (5, 8):
            # Проверка картинок категорийного колдунщика
            request = (
                "place=parallel&text=TopModels category&rearr-factors="
                "market_ext_category_wizard_top_models_count=2;"
                "market_ext_category_wizard_model_pictures=1;"
                "market_ext_category_wizard_model_pictures_size={}".format(size)
            )
            response = self.report.request_bs(request)
            self.assertFragmentIn(
                response,
                {
                    "market_ext_category": [
                        {
                            "showcase": {
                                "top_models": [
                                    {
                                        "pictures": [
                                            "//avatars.mds.yandex.net/get-mpic/top_model_1101_0/{}hq".format(size),
                                            "//avatars.mds.yandex.net/get-mpic/top_model_1101_1/{}hq".format(size),
                                            "//avatars.mds.yandex.net/get-mpic/top_model_1101_2/{}hq".format(size),
                                        ]
                                    },
                                    {
                                        "pictures": [
                                            "//avatars.mds.yandex.net/get-mpic/top_model_1102_0/{}hq".format(size),
                                            "//avatars.mds.yandex.net/get-mpic/top_model_1102_1/{}hq".format(size),
                                            "//avatars.mds.yandex.net/get-mpic/top_model_1102_2/{}hq".format(size),
                                        ]
                                    },
                                ]
                            }
                        }
                    ]
                },
            )

            # Проверка картинок колдунщика неявной модели
            request = (
                "place=parallel&text=TopModel&rearr-factors="
                "market_implicit_wizard_top_models_count=2;"
                "market_implicit_model_wizard_model_pictures=1;"
                "market_implicit_model_wizard_model_pictures_size={}".format(size)
            )
            response = self.report.request_bs(request)
            self.assertFragmentIn(
                response,
                {
                    "market_implicit_model": [
                        {
                            "showcase": {
                                "items": [
                                    {
                                        "pictures": [
                                            "//avatars.mds.yandex.net/get-mpic/top_model_1101_0/{}hq".format(size),
                                            "//avatars.mds.yandex.net/get-mpic/top_model_1101_1/{}hq".format(size),
                                            "//avatars.mds.yandex.net/get-mpic/top_model_1101_2/{}hq".format(size),
                                        ]
                                    },
                                    {
                                        "pictures": [
                                            "//avatars.mds.yandex.net/get-mpic/top_model_1102_0/{}hq".format(size),
                                            "//avatars.mds.yandex.net/get-mpic/top_model_1102_1/{}hq".format(size),
                                            "//avatars.mds.yandex.net/get-mpic/top_model_1102_2/{}hq".format(size),
                                        ]
                                    },
                                ]
                            }
                        }
                    ]
                },
            )

        # Проверка отсутствия картинок без флагов
        # Категорийный колдунщик
        request = "place=parallel&text=TopModels category&rearr-factors=market_ext_category_wizard_top_models_count=2"
        response = self.report.request_bs(request)
        self.assertFragmentNotIn(response, {"market_ext_category": [{"showcase": {"top_models": [{"pictures": []}]}}]})

        # Колдунщик неявной модели
        request = "place=parallel&text=TopModel&rearr-factors=market_implicit_wizard_top_models_count=2"
        response = self.report.request_bs(request)
        self.assertFragmentNotIn(response, {"market_implicit_model": [{"showcase": {"items": [{"pictures": []}]}}]})

    @classmethod
    def prepare_thin_frame_pictures_in_wizards(cls):
        """Подготовка данных для проверки добавления картинок с тонкой рамкой в колдунщиках под конструктором
        https://st.yandex-team.ru/MARKETOUT-13975
        https://st.yandex-team.ru/MARKETOUT-14043
        """

        # создаем модель для модельного колдунщика
        cls.index.models += [
            Model(
                title='SingleModel thin frame pictures',
                picinfo='//avatars.mds.yandex.net/get-mpic/12345/model-thin-frame/orig#100#100',
            ),
        ]

        # Создаем модели для колдунщика неявной модели
        cls.index.models += [
            Model(
                hyperid=402,
                title='ImplicitModel thin frame pictures 1',
                picinfo='//avatars.mds.yandex.net/get-mpic/12345/implicit-model-thin-frame/orig#100#100',
            ),
            Model(
                hyperid=403,
                title='ImplicitModel thin frame pictures 2',
                picinfo='//avatars.mds.yandex.net/get-mpic/12345/implicit-model-thin-frame/orig#100#100',
            ),
        ]
        # создаем регион
        cls.index.regiontree += [
            Region(rid=2, name='Екатеринбург', genitive='Екатеринбурга', locative='Екатеринбурге', preposition='в'),
        ]
        # создаем магазин
        cls.index.shops += [
            Shop(fesh=201, priority_region=2),
        ]
        # создаем офферы
        cls.index.offers += [
            Offer(hyperid=402, fesh=201, title="Offer", price=100),
            Offer(hyperid=403, fesh=201, title="Offer", price=200),
        ]

    def test_thin_frame_pictures_in_wizards(self):
        """Проверка добавления картинок с тонкой рамкой в колдунщиках под конструктором
        https://st.yandex-team.ru/MARKETOUT-13975
        https://st.yandex-team.ru/MARKETOUT-14043
        """

        # Проверка картинок с тонкой рамкой модельного колдунщика
        response = self.report.request_bs('place=parallel&text=SingleModel&rearr-factors=showcase_universal=1')
        self.assertFragmentIn(
            response,
            {
                "market_model": [
                    {
                        "title": {"__hl": {"raw": True, "text": "SingleModel thin frame pictures"}},
                        "picture": "//avatars.mds.yandex.net/get-mpic/12345/model-thin-frame/2hq",
                        "pictureTouch": "//avatars.mds.yandex.net/get-mpic/12345/model-thin-frame/7hq",
                        "pictureTouchHd": "//avatars.mds.yandex.net/get-mpic/12345/model-thin-frame/8hq",
                    }
                ]
            },
        )

        # Проверка картинок с тонкой рамкой колдунщика неявной модели
        response = self.report.request_bs('place=parallel&rids=2&text=ImplicitModel+thin+frame+pictures')
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": [
                    {
                        "title": "\7[Implicitmodel thin frame pictures\7] в Екатеринбурге",
                        "showcase": {
                            "items": [
                                {
                                    "thumb": {
                                        "source": "//avatars.mds.yandex.net/get-mpic/12345/implicit-model-thin-frame/2hq",
                                        "retinaSource": "//avatars.mds.yandex.net/get-mpic/12345/implicit-model-thin-frame/5hq",
                                    }
                                },
                                {
                                    "thumb": {
                                        "source": "//avatars.mds.yandex.net/get-mpic/12345/implicit-model-thin-frame/2hq",
                                        "retinaSource": "//avatars.mds.yandex.net/get-mpic/12345/implicit-model-thin-frame/5hq",
                                    }
                                },
                            ]
                        },
                    }
                ]
            },
        )

    @classmethod
    def prepare_ext_category_vendor_logo(cls):
        """Подготовка данных для проверки добавления логотипов брендов для категорийного колдунщика под конструктором
        https://st.yandex-team.ru/MARKETOUT-14047
        """

        # Добавляем вендоров
        cls.index.vendors += [
            Vendor(
                vendor_id=21,
                name='Vendor logo 1',
                logos=[VendorLogo(url='//avatars.mds.yandex.net/get-mpic/1234/vendor_logo_1/orig')],
            ),
            Vendor(
                vendor_id=22,
                name='Vendor logo 2',
                logos=[VendorLogo(url='//avatars.mds.yandex.net/get-mpic/1234/vendor_logo_2/orig')],
            ),
            Vendor(vendor_id=23, name='Vendor no logo'),
        ]

        # Создаем категорию
        cls.index.hypertree += [
            HyperCategory(hid=203, name='Vendors logo category', output_type=HyperCategoryType.GURU)
        ]
        # Создаем карточку категории
        cls.index.cards += [CardCategory(hid=203, vendor_ids=[21, 22, 23])]

    def test_ext_category_vendor_logo(self):
        """Проверка добавления логотипов брендов для категорийного колдунщика под конструктором
        https://st.yandex-team.ru/MARKETOUT-14047
        """
        response = self.report.request_bs(
            "place=parallel&rids=1&text=Vendors+logo+category&rearr-factors=market_category_wizard_vendor_logo=1"
        )
        self.assertFragmentIn(
            response,
            {
                "market_ext_category": [
                    {
                        "showcase": {
                            "items": [
                                {
                                    "title": {"text": {"__hl": {"text": "Vendor logo 1", "raw": True}}},
                                    "vendor_logo": {
                                        "source": LikeUrl.of(
                                            '//avatars.mds.yandex.net/get-mpic/1234/vendor_logo_1/orig'
                                        )
                                    },
                                },
                                {
                                    "title": {"text": {"__hl": {"text": "Vendor logo 2", "raw": True}}},
                                    "vendor_logo": {
                                        "source": LikeUrl.of(
                                            '//avatars.mds.yandex.net/get-mpic/1234/vendor_logo_2/orig'
                                        )
                                    },
                                },
                                {
                                    "title": {"text": {"__hl": {"text": "Vendor no logo", "raw": True}}},
                                    "vendor_logo": NoKey("vendor_logo"),
                                },
                            ]
                        }
                    }
                ]
            },
        )

    @classmethod
    def prepare_model_analogs_count(cls):
        """Подготовка данных для проверки количества аналогов в модельном колдунщике
        https://st.yandex-team.ru/MARKETOUT-14188
        """
        # Количество аналогов модели
        analogs_count = 10
        # Добавляем модель
        cls.index.models += [
            Model(
                hyperid=501, hid=101, title='Single model with analogs', analogs=[502 + i for i in range(analogs_count)]
            ),
        ]
        # Добавляем аналоги
        cls.index.models += [
            Model(hyperid=502 + i, hid=101, title='Single model analog {}'.format(i)) for i in range(analogs_count)
        ]
        # Добавляем цены для аналогов
        cls.index.regional_models += [
            RegionalModel(hyperid=502 + i, price_min=i, price_max=i + 100) for i in range(analogs_count)
        ]

        # Создаем групповую модель
        cls.index.model_groups += [
            ModelGroup(hyperid=511, title='Group model with analogs', analogs=[502 + i for i in range(analogs_count)])
        ]
        # Добавляем 5 модификаций, чтобы сработал колдунщик групповой модели
        cls.index.models += [
            Model(group_hyperid=511, title='Group model with analogs 1'),
            Model(group_hyperid=511, title='Group model with analogs 2'),
            Model(group_hyperid=511, title='Group model with analogs 3'),
            Model(group_hyperid=511, title='Group model with analogs 4'),
            Model(group_hyperid=511, title='Group model with analogs 5'),
        ]
        # Добавляем оффер групповой модели, чтобы сработал шаблон model_group_default
        cls.index.offers += [
            Offer(hyperid=511),
        ]

        cls.index.navtree += [
            NavCategory(nid=201, hid=101),
        ]


if __name__ == '__main__':
    main()
