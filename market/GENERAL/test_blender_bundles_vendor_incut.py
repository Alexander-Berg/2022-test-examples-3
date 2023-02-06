#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    GradeDispersionItem,
    HyperCategory,
    HyperCategoryType,
    Model,
    NavCategory,
    Offer,
    ReportState,
    ReviewDataItem,
    Vendor,
    VendorBanner,
)
from core.testcase import TestCase, main
from core.matcher import Contains, ElementCount, NotEmpty, EmptyList
from core.blender_bundles import get_supported_incuts_cgi, create_blender_bundles
from core.click_context import ClickContext


class BlenderTopVendorIncut:
    BUNDLE = '''
{
    "incut_places": ["Top"],
    "incut_positions": [1],
    "incut_viewtypes": ["GalleryWithBanner", "VendorGallery"],
    "incut_ids": ["vendor_incut_with_banner", "vendor_incut"],
    "result_scores": [
        {
            "incut_place": "Top",
            "row_position": 1,
            "incut_viewtype": "GalleryWithBanner",
            "incut_id": "vendor_incut_with_banner",
            "score": 0.75
        },
        {
            "incut_place": "Top",
            "row_position": 1,
            "incut_viewtype": "VendorGallery",
            "incut_id": "vendor_incut",
            "score": 0.74
        }
    ],
    "calculator_type": "ConstPosition"
}
'''


class BlenderBundlesConfig:
    BUNDLES_CONFIG = """
{
    "INCLID_VENDOR_INCUT" : {
        "client == frontend && platform == desktop" : {
            "bundle_name": "const_vendor_incut.json"
        },
        "client == frontend && platform == touch" : {
            "bundle_name": "const_vendor_incut.json"
        },
        "client == IOS" : {
            "bundle_name": "search_vendor_incut.json"
        },
        "client == ANDROID" : {
            "bundle_name": "search_vendor_incut.json"
        }
    }
}
"""


class BlenderSearchVendorIncut:
    BUNDLE = '''
{
    "incut_places": ["Search"],
    "incut_positions": [1],
    "incut_viewtypes": ["VendorGallery"],
    "incut_ids": ["vendor_incut"],
    "result_scores": [
        {
            "incut_place": "Search",
            "row_position": 1,
            "incut_viewtype": "VendorGallery",
            "incut_id": "vendor_incut",
            "score": 0.74
        }
    ],
    "calculator_type": "ConstPosition"
}
'''


class T(TestCase):
    class CgiParams(dict):
        def raw(self, separator='&'):
            if len(self):
                return separator.join("{}={}".format(str(k), str(v)) for (k, v) in self.items())
            return ""

    class RearrFlags(CgiParams):
        def __init__(self, *args, **kwargs):
            super(T.RearrFlags, self).__init__(*args, **kwargs)

        def raw(self):
            if len(self):
                return 'rearr-factors={}'.format(super(T.RearrFlags, self).raw(';'))
            return str()

    @staticmethod
    def create_request(parameters, rearr):
        return '{}{}'.format(parameters.raw(), '&{}'.format(rearr.raw()) if len(rearr) else '')

    @classmethod
    def prepare(cls):

        cls.index.hypertree += [HyperCategory(hid=100, output_type=HyperCategoryType.GURU)]

        cls.index.models += [
            Model(hid=100, hyperid=1, title="Первая модель", ts=1),
        ]

        cls.index.model_reviews_data += [
            ReviewDataItem(
                review_id=101,
                model_id=1,
                short_text="Nice",
                most_useful=1,
            )
        ]
        cls.index.model_grade_dispersion_data += [
            GradeDispersionItem(model_id=1, five=1),
        ]

    @classmethod
    def prepare_blender_bundles_config(cls):
        cls.settings.formulas_path = create_blender_bundles(
            cls.meta_paths.testroot,
            BlenderBundlesConfig.BUNDLES_CONFIG,
            {
                "const_vendor_incut.json": BlenderTopVendorIncut.BUNDLE,
                "search_vendor_incut.json": BlenderSearchVendorIncut.BUNDLE,
            },
        )

    @classmethod
    def prepare_vendor_incut(cls):
        cls.index.vendors += [Vendor(vendor_id=x, name='vendor_{}'.format(x)) for x in range(1, 4)]

        start_hyperid_1 = 1300
        cls.index.models += [
            Model(
                hid=551,
                hyperid=start_hyperid_1 + x,
                vendor_id=1,
                vbid=10,
                title="toy {}".format(start_hyperid_1 + x),
            )
            for x in range(0, 10)
        ]
        cls.index.offers += [
            Offer(
                hyperid=start_hyperid_1 + x,
                price=1000 + x,
                cpa=Offer.CPA_REAL,
                hid=551,
                title="offer for {}".format(start_hyperid_1 + x),
            )
            for x in range(0, 10)
        ]

        start_hyperid_2 = start_hyperid_1 + 10
        cls.index.models += [
            Model(
                hid=551,
                hyperid=start_hyperid_2 + x,
                vendor_id=2,
                vbid=20,  # more then vendor 1
                title="toy {}".format(start_hyperid_2 + x),
            )
            for x in range(0, 10)
        ]
        cls.index.offers += [
            Offer(
                hyperid=start_hyperid_2 + x,
                price=2000 * x,  # цена моделей больше чем у первого вендора
                cpa=Offer.CPA_NO,  # should not be a CPA
                hid=551,
                title="offer for {}".format(start_hyperid_2 + x),
            )
            for x in range(0, 10)
        ]

        # вендор с банером
        start_hyperid_3 = start_hyperid_2 + 10
        cls.index.models += [
            Model(
                hid=552,  # другой, относительно предыдущих
                hyperid=start_hyperid_3 + x,
                vendor_id=3,
                vbid=20,  # more then vendor 1
                title="toy {}".format(start_hyperid_3 + x),
                datasource_id=1,
            )
            for x in range(0, 10)
        ]
        cls.index.offers += [
            Offer(
                hyperid=start_hyperid_3 + x,
                price=1000 * x,
                cpa=Offer.CPA_REAL,
                hid=552,
                title="offer for {}".format(start_hyperid_3 + x),
            )
            for x in range(0, 10)
        ]
        cls.index.vendors_banners += [
            VendorBanner(
                datasource_id=1,
                vendor_id=3,
                hyper_id=552,
                vendor_banner_id=71,
                bbid=800,
            )
        ]
        # хотим что в категории, где есть баннер, была еще менее выгодная врезка без баннера
        start_hyperid_4 = start_hyperid_3 + 10
        cls.index.models += [
            Model(
                hid=552,
                hyperid=start_hyperid_4 + x,
                vendor_id=3,
                vbid=10,  # less then vendor 3
                title="toy {}".format(start_hyperid_4 + x),
                datasource_id=1,
            )
            for x in range(0, 10)
        ]
        cls.index.offers += [
            Offer(
                hyperid=start_hyperid_4 + x,
                price=1000 * x,
                cpa=Offer.CPA_REAL,
                hid=552,
                title="offer for {}".format(start_hyperid_4 + x),
            )
            for x in range(0, 10)
        ]

    def test_vendor_incut(self):
        """
        Проверка выдачи вендорской врезки
        """

        params = self.CgiParams(
            {
                'place': 'blender',
                'text': 'toy',
                'use-default-offers': 1,
                'debug': 'da',
                'allow-collapsing': 1,
                'pp': 18,
                'show-urls': 'productVendorBid',
                'hid': 551,
                'client': 'frontend',
                'platform': 'desktop',
                'supported-incuts': get_supported_incuts_cgi(),
            }
        )

        req_size = 8
        rearr_factors = self.RearrFlags(
            {
                'market_report_blender_vendor_incut_enable': 1,  # разрешение работы вендорской врезки
                'market_vendor_incut_size': req_size,
                'market_vendor_incut_with_CPA_offers_only': 0,
                'market_vendor_incut_hide_undeliverable_models': 0,
                'market_blender_use_bundles_config': 1,
                'market_blender_media_adv_incut_enabled': 0,
            }
        )

        response = self.report.request_json(self.create_request(params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            'incutId': 'vendor_incut',
                            "entity": "searchIncut",
                            "items": ElementCount(req_size),
                            'title': 'Предложения vendor_{}'.format(2),
                        },
                    ],
                },
            },
            preserve_order=True,
        )

        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'items': [
                                {
                                    'vendor': {
                                        'id': 2,  # ставка больше, но без учета CPA
                                    },
                                }
                            ]
                        }
                    ]
                }
            },
            preserve_order=True,
        )
        self.assertFragmentNotIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            'incutId': 'vendor_incut_with_banner',
                        }
                    ]
                }
            },
            preserve_order=True,
        )

        # учитывание CPA (тогда должен быть первый, хотя и ставка меньше)
        rearr_factors['market_vendor_incut_with_CPA_offers_only'] = 1
        response = self.report.request_json(self.create_request(params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            "items": ElementCount(req_size),
                            'title': 'Предложения vendor_{}'.format(1),
                        },
                    ],
                },
            },
            preserve_order=True,
        )
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'title': 'Предложения vendor_{}'.format(1),
                            'items': [
                                {
                                    'vendor': {
                                        'id': 1,  # ставка меньше, но учитывается CPA
                                    },
                                    'offers': {
                                        'items': ElementCount(1),  # one DO
                                    },
                                }
                            ],
                        }
                    ]
                }
            },
            preserve_order=True,
        )

        # запрет на формирование вендорской врезки
        rearr_factors['market_report_blender_vendor_incut_enable'] = 0  # disable vendor_incut
        response = self.report.request_json(self.create_request(params, rearr_factors))
        self.assertFragmentNotIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            "incutId": "vendor_incut",
                            "entity": "searchIncut",
                        },
                    ],
                },
            },
            preserve_order=True,
        )
        self.assertFragmentNotIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            'incutId': 'vendor_incut_with_banner',
                        }
                    ]
                }
            },
            preserve_order=True,
        )

        self.show_log_tskv.expect(show_uid="04884192001117778888843001", inclid=3, position=1).times(2)
        for pos in range(1, 9):
            self.show_log_tskv.expect(
                show_uid="04884192001117778888816{:03d}".format(pos),
                inclid=3,
                position=pos,
                super_uid="04884192001117778888843001",
                incut_position=1,
            ).times(2)

    def test_vendor_incut_first_page(self):
        """
        запрос вендорской врезки только для первой страницы
        """
        params = self.CgiParams(
            {
                'place': 'blender',
                'text': 'toy',
                'use-default-offers': 1,
                'debug': 'da',
                'allow-collapsing': 1,
                'pp': 18,
                'show-urls': 'productVendorBid',
                'hid': 551,
                'client': 'frontend',
                'platform': 'desktop',
                'supported-incuts': get_supported_incuts_cgi(),
            }
        )
        rearr_factors = self.RearrFlags(
            {
                'market_report_blender_vendor_incut_enable': 1,  # разрешение работы вендорской врезки
                'market_vendor_incut_size': 8,
                'market_vendor_incut_with_CPA_offers_only': 0,
                'market_vendor_incut_hide_undeliverable_models': 0,
                'market_blender_use_bundles_config': 1,
                'market_blender_media_adv_incut_enabled': 0,
            }
        )

        # дефолтный запрос (страница 1)
        response = self.report.request_json(self.create_request(params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            'incutId': 'vendor_incut',
                            "entity": "searchIncut",
                            "items": ElementCount(8),
                            'title': 'Предложения vendor_{}'.format(2),
                        },
                    ],
                },
            },
            preserve_order=True,
        )

        # запрос страницы №2
        # врезка есть в топе на любой странице
        params['page'] = 2
        response = self.report.request_json(self.create_request(params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            'incutId': 'vendor_incut',
                            'placeId': 2,
                        },
                    ]
                },
            },
            preserve_order=True,
        )

    def test_switching_off_vendor_incut(self):
        """
        Проверяем, что можно полностью отключить набор врезки флагом market_force_disable_vendor_incut
        даже при запросе через блендер
        """
        params = self.CgiParams(
            {
                'place': 'blender',
                'text': 'toy',
                'use-default-offers': 1,
                'debug': 'da',
                'allow-collapsing': 1,
                'pp': 18,
                'show-urls': 'productVendorBid',
                'hid': 551,
                'client': 'frontend',
                'platform': 'desktop',
                'supported-incuts': get_supported_incuts_cgi(),
            }
        )
        rearr_factors = self.RearrFlags(
            {
                'market_report_blender_vendor_incut_enable': 1,  # разрешение работы вендорской врезки
                'market_vendor_incut_size': 8,
                'market_vendor_incut_with_CPA_offers_only': 0,
                'market_vendor_incut_hide_undeliverable_models': 0,
                'market_blender_use_bundles_config': 1,
                'market_blender_media_adv_incut_enabled': 0,
                'market_force_disable_vendor_incut': 1,  # отключение вендорской врезки
            }
        )
        response = self.report.request_json(self.create_request(params, rearr_factors))
        self.assertFragmentIn(response, {"incuts": {"results": EmptyList()}})

    def test_vendor_incut_click_context(self):
        """
        проверяем заполнение поля cc для моделей и офферов врезки
        """
        params = self.CgiParams(
            {
                'place': 'blender',
                'text': 'toy',
                'use-default-offers': 1,
                'debug': 'da',
                'allow-collapsing': 1,
                'pp': 18,
                'show-urls': 'productVendorBid',
                'hid': 551,
                'client': 'frontend',
                'platform': 'desktop',
                'supported-incuts': get_supported_incuts_cgi(),
            }
        )
        rearr_factors = self.RearrFlags(
            {
                'market_report_blender_vendor_incut_enable': 1,  # разрешение работы вендорской врезки
                'market_vendor_incut_size': 8,
                'market_vendor_incut_with_CPA_offers_only': 1,
                'market_vendor_incut_hide_undeliverable_models': 0,
                'market_blender_use_bundles_config': 1,
                'market_blender_media_adv_incut_enabled': 0,
            }
        )
        click_context = str(ClickContext(pp=8, inclid=3))

        response = self.report.request_json(self.create_request(params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            'incutId': 'vendor_incut',
                            "entity": "searchIncut",
                            "items": [
                                {
                                    "entity": "product",
                                    "cc": click_context,
                                    "offers": {
                                        "items": [
                                            {
                                                "entity": "offer",
                                                "cc": click_context,
                                            },
                                        ],
                                    },
                                },
                            ],
                            'title': 'Предложения vendor_{}'.format(1),
                        },
                    ],
                },
            },
        )

    def test_vendor_incut_with_banner(self):
        params = self.CgiParams(
            {
                'place': 'blender',
                'text': 'toy',
                'use-default-offers': 1,
                'debug': 'da',
                'allow-collapsing': 1,
                'pp': 18,
                'show-urls': 'productVendorBid',
                'hid': 552,  # вендор с баннером
                'client': 'frontend',
                'platform': 'desktop',
                'supported-incuts': get_supported_incuts_cgi(),
            }
        )
        req_size = 8
        rearr_factors = self.RearrFlags(
            {
                'market_report_blender_vendor_incut_enable': 1,  # разрешение работы вендорской врезки
                'market_vendor_incut_size': req_size,
                'market_vendor_incut_with_CPA_offers_only': 1,
                'market_vendor_incut_hide_undeliverable_models': 0,
                'market_vendor_incut_enable_banners': 1,  # enable to show vendor
                'market_blender_use_bundles_config': 1,
                'market_blender_media_adv_incut_enabled': 0,
            }
        )

        # выигрыш вендора с баннером
        rearr_factors['market_report_blender_vendor_incut_banner_render_enable'] = 0
        response = self.report.request_json(
            self.create_request(params, rearr_factors)
        )  # запрос категории где только один вендор с банером
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            'incutId': 'vendor_incut_with_banner',
                            "entity": "searchIncut",
                            "items": ElementCount(req_size + 1),  # на одну позицию больше, т.к. есть баннер
                        },
                    ],
                },
            },
            preserve_order=True,
        )
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {"items": [{'entity': "vendorBanner", 'vendorId': 3, 'bannerId': 71}]},  # banner
                    ],
                },
            },
            preserve_order=True,
        )
        self.assertFragmentNotIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            'incutId': 'vendor_incut',
                        }
                    ]
                }
            },
            preserve_order=True,
        )

        # новый вид рендеринга (инфа о баннере в теле самой врезки)
        expect_banner = {
            "incuts": {
                "results": [
                    {"banner": {'entity': "vendorBanner", 'vendorId': 3, 'bannerId': 71}},
                ],
            },
        }
        self.assertFragmentNotIn(  # изначально новые поля отсутствуют
            response,
            expect_banner,
            preserve_order=True,
        )
        rearr_factors['market_report_blender_vendor_incut_banner_render_enable'] = 1
        response = self.report.request_json(
            self.create_request(params, rearr_factors)
        )  # запрос категории где только один вендор с банером
        self.assertFragmentIn(  # новый фрагмент должен присутствовать
            response,
            expect_banner,
            preserve_order=True,
        )

        # старый фрагмент должен отсутствовать
        self.assertFragmentNotIn(
            response,
            {
                "incuts": {
                    "results": [
                        {"items": [{'entity': "vendorBanner", 'vendorId': 3, 'bannerId': 71}]},  # banner
                    ],
                },
            },
            preserve_order=True,
        )

        # запрос врезки с баннером, но есть только без него
        params['hid'] = 551
        response = self.report.request_json(
            self.create_request(params, rearr_factors)
        )  # запрос категории где есть только вендоры без банером
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            'incutId': 'vendor_incut',  # без баннера
                            "entity": "searchIncut",
                            "items": ElementCount(req_size),
                        },
                    ],
                },
            },
            preserve_order=True,
        )

    @classmethod
    def prepare_vendor_incut_num_doc(cls):
        cls.index.vendors += [Vendor(vendor_id=10 + x, name='vendor_{}'.format(10 + x)) for x in range(1, 4)]

        # предложения первого вендора (11)
        start_hyperid_1 = 2300
        cls.index.models += [
            Model(
                hid=651,
                hyperid=start_hyperid_1 + x,
                vendor_id=11,
                vbid=10,
                title="blyander {}".format(start_hyperid_1 + x),
            )
            for x in range(0, 10)
        ]
        cls.index.offers += [
            Offer(
                hyperid=start_hyperid_1 + x,
                price=1000 * x,
                cpa=Offer.CPA_REAL,
                hid=651,
                title="offer for {}".format(start_hyperid_1 + x),
            )
            for x in range(0, 10)
        ]

        # предложения второго вендора (12)
        start_hyperid_2 = start_hyperid_1 + 10
        cls.index.models += [
            Model(
                hid=651,
                hyperid=start_hyperid_2 + x,
                vendor_id=12,
                vbid=20,  # ставка больше
                title="blyander {}".format(start_hyperid_2 + x),
            )
            for x in range(0, 8)
        ]  # кол-во моделей меньше
        cls.index.offers += [
            Offer(
                hyperid=start_hyperid_2 + x,
                price=1000 * x,
                cpa=Offer.CPA_REAL,
                hid=651,
                title="offer for {}".format(start_hyperid_2 + x),
            )
            for x in range(0, 8)
        ]

        # предложения третьего вендора (13)
        start_hyperid_3 = start_hyperid_2 + 10
        cls.index.models += [
            Model(
                hid=651,
                hyperid=start_hyperid_3 + x,
                vendor_id=13,
                vbid=30,  # ставка больше
                title="blyander {}".format(start_hyperid_3 + x),
            )
            for x in range(0, 3)
        ]  # кол-во моделей меньше
        cls.index.offers += [
            Offer(
                hyperid=start_hyperid_3 + x,
                price=1000 * x,
                cpa=Offer.CPA_REAL,
                hid=651,
                title="offer for {}".format(start_hyperid_3 + x),
            )
            for x in range(0, 3)
        ]

    @staticmethod
    def update_num_doc(rearr, num_doc):
        """установка в rearr флаги запрашиваемого кол-ва элементов"""
        rearr['market_vendor_incut_size'] = num_doc

    @staticmethod
    def update_num_doc_min(rearr, min_num_doc):
        """установка в rearr флаги минимального кол-ва элементов"""
        rearr['market_vendor_incut_min_size'] = min_num_doc

    def test_vendor_incut_num_doc_default(self):
        """
        тест на корректную работу дефолтных значений минимального кол-ва (4) выдаваемых элементов во врезке
        """
        params = self.CgiParams(
            {
                'place': 'blender',
                'text': 'blyander',
                'use-default-offers': 1,
                'debug': 'da',
                'allow-collapsing': 1,
                'pp': 18,
                'show-urls': 'productVendorBid',
                'hid': 651,
                'client': 'frontend',
                'platform': 'desktop',
                'supported-incuts': get_supported_incuts_cgi(),
            }
        )

        rearr_factors = self.RearrFlags(
            {
                'market_report_blender_vendor_incut_enable': 1,  # разрешение работы вендорской врезки
                'market_vendor_incut_with_CPA_offers_only': 0,
                'market_vendor_incut_hide_undeliverable_models': 0,
                'market_blender_use_bundles_config': 1,
                'market_blender_media_adv_incut_enabled': 0,
            }
        )

        # запрос без указания минимального и требуемого кол-ва
        response = self.report.request_json(self.create_request(params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            'incutId': 'vendor_incut',
                            "entity": "searchIncut",
                            "items": ElementCount(8),  # сколько есть у этого вендора
                            'title': 'Предложения vendor_{}'.format(
                                12
                            ),  # выиграет вендор 12 (8 моделей), хотя вендор 13 с большей ставкой (3 модели)
                        },
                    ],
                },
            },
            preserve_order=True,
        )

        # запрос меньшего кол-ва элементов, чем выдает по умолчанию
        self.update_num_doc(rearr_factors, 3)
        self.update_num_doc_min(rearr_factors, 3)
        response = self.report.request_json(self.create_request(params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            'incutId': 'vendor_incut',
                            "entity": "searchIncut",
                            "items": ElementCount(3),
                            'title': 'Предложения vendor_{}'.format(13),  # выиграет вендор 13 с максимальной ставкой
                        },
                    ],
                },
            },
            preserve_order=True,
        )

    def test_vendor_incut_num_doc_change(self):
        """
        тест на корректную работу при различных значениях numDoc и minNumDoc
        """
        params = self.CgiParams(
            {
                'place': 'blender',
                'text': 'blyander',
                'use-default-offers': 1,
                'debug': 'da',
                'allow-collapsing': 1,
                'pp': 18,
                'show-urls': 'productVendorBid',
                'hid': 651,
                'client': 'frontend',
                'platform': 'desktop',
                'supported-incuts': get_supported_incuts_cgi(),
            }
        )

        rearr_factors = self.RearrFlags(
            {
                'market_report_blender_vendor_incut_enable': 1,  # разрешение работы вендорской врезки
                'market_vendor_incut_with_CPA_offers_only': 0,
                'market_vendor_incut_hide_undeliverable_models': 0,
                'market_blender_use_bundles_config': 1,
                'market_blender_media_adv_incut_enabled': 0,
            }
        )

        self.update_num_doc(rearr_factors, 6)
        self.update_num_doc_min(rearr_factors, 4)
        response = self.report.request_json(self.create_request(params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            'incutId': 'vendor_incut',
                            "entity": "searchIncut",
                            "items": ElementCount(6),
                            'title': 'Предложения vendor_{}'.format(12),  # где ставка выше
                        },
                    ],
                },
            },
            preserve_order=True,
        )

        # запрашиваем больше, чем есть у вендора, который выиграет (но минимальное число всё так же меньше)
        self.update_num_doc(rearr_factors, 9)
        self.update_num_doc_min(rearr_factors, 4)
        response = self.report.request_json(self.create_request(params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            'incutId': 'vendor_incut',
                            "entity": "searchIncut",
                            "items": ElementCount(8),  # запрашивали 9, но есть только 8 у лучшего вендора
                            'title': 'Предложения vendor_{}'.format(12),  # где ставка выше
                        },
                    ],
                },
            },
            preserve_order=True,
        )

        # запрашиваем больше, чем есть у вендора который выиграет (ставим минимальное число больше, чем у него предложений)
        self.update_num_doc(rearr_factors, 9)
        self.update_num_doc_min(rearr_factors, 9)
        response = self.report.request_json(self.create_request(params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            'incutId': 'vendor_incut',
                            "entity": "searchIncut",
                            "items": ElementCount(9),  # именно столько, сколько запрашивали. В наличии 10
                            'title': 'Предложения vendor_{}'.format(
                                11
                            ),  # вендор с меньшей ставкой, но с тем кол-вом, которое требовалось
                        },
                    ],
                },
            },
            preserve_order=True,
        )

    @classmethod
    def prepare_rs(cls):

        cls.index.hypertree += [
            HyperCategory(
                hid=700,
                output_type=HyperCategoryType.GURU,
                children=[
                    HyperCategory(hid=701, output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=702, output_type=HyperCategoryType.GURU),
                ],
            )
        ]

        start_hyperid = 7000
        cls.index.models += [
            Model(
                hid=700,
                hyperid=start_hyperid + x,
                vendor_id=1,
                vbid=1000,
                title="department {}".format(start_hyperid + x),
            )
            for x in range(0, 6)
        ]
        cls.index.offers += [
            Offer(
                hyperid=start_hyperid + x,
                price=1000 * x,
                cpa=Offer.CPA_REAL,
                hid=700,
                title="offer for {}".format(start_hyperid + x),
            )
            for x in range(0, 6)
        ]

        start_hyperid = 7010
        cls.index.models += [
            Model(
                hid=701,
                hyperid=start_hyperid + x,
                vendor_id=2,
                vbid=100,
                title="leaf {}".format(start_hyperid + x),
            )
            for x in range(0, 6)
        ]
        cls.index.offers += [
            Offer(
                hyperid=start_hyperid + x,
                price=1000 * x,
                cpa=Offer.CPA_REAL,
                hid=701,
                title="offer for {}".format(start_hyperid + x),
            )
            for x in range(0, 6)
        ]

        start_hyperid = 7020
        cls.index.models += [
            Model(
                hid=702,
                hyperid=start_hyperid + x,
                vendor_id=3,
                vbid=10,
                title="leaf {}".format(start_hyperid + x),
            )
            for x in range(0, 6)
        ]
        cls.index.offers += [
            Offer(
                hyperid=start_hyperid + x,
                price=1000 * x,
                cpa=Offer.CPA_REAL,
                hid=702,
                title="offer for {}".format(start_hyperid + x),
            )
            for x in range(0, 6)
        ]

    def test_vendor_incut_rs_single_hid(self):
        """
        Тест на корректную работу категорий пришедших из rs
        Поиск идёт только среди категорий указанных в rs
        """
        rs = ReportState.create()
        c = rs.search_state.top_categories.add()
        c.hid = 702
        params = self.CgiParams(
            {
                'place': 'blender',
                'use-default-offers': 1,
                'allow-collapsing': 1,
                'pp': 18,
                'show-urls': 'productVendorBid',
                'hid': 700,
                'client': 'frontend',
                'platform': 'desktop',
                'rs': ReportState.serialize(rs).replace('=', ','),
                'supported-incuts': get_supported_incuts_cgi(),
            }
        )

        rearr_factors = self.RearrFlags(
            {
                'market_report_blender_vendor_incut_enable': 1,  # разрешение работы вендорской врезки
                'market_vendor_incut_with_CPA_offers_only': 0,
                'market_vendor_incut_hide_undeliverable_models': 0,
                'market_blender_use_bundles_config': 1,
                'market_money_use_top_categories_in_vendor_incut': 1,
                'market_blender_media_adv_incut_enabled': 0,
            }
        )

        self.update_num_doc(rearr_factors, 6)
        self.update_num_doc_min(rearr_factors, 4)
        response = self.report.request_json(self.create_request(params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            'incutId': 'vendor_incut',
                            "entity": "searchIncut",
                            'title': 'Предложения vendor_{}'.format(3),  # единственный вендор в 702 категории
                        },
                    ],
                },
            },
        )

    def test_vendor_incut_rs_multiple_hid(self):
        """
        Тест на корректную работу категорий пришедших из rs
        Поиск идёт только среди категорий указанных в rs
        """
        rs = ReportState.create()
        c = rs.search_state.top_categories.add()
        c.hid = 701
        c = rs.search_state.top_categories.add()
        c.hid = 702
        params = self.CgiParams(
            {
                'place': 'blender',
                'use-default-offers': 1,
                'allow-collapsing': 1,
                'pp': 18,
                'show-urls': 'productVendorBid',
                'hid': 700,
                'client': 'frontend',
                'platform': 'desktop',
                'rs': ReportState.serialize(rs).replace('=', ','),
                'supported-incuts': get_supported_incuts_cgi(),
            }
        )

        rearr_factors = self.RearrFlags(
            {
                'market_report_blender_vendor_incut_enable': 1,  # разрешение работы вендорской врезки
                'market_vendor_incut_with_CPA_offers_only': 0,
                'market_vendor_incut_hide_undeliverable_models': 0,
                'market_money_use_top_categories_in_vendor_incut': 1,
                'market_blender_use_bundles_config': 1,
                'market_blender_media_adv_incut_enabled': 0,
            }
        )

        self.update_num_doc(rearr_factors, 6)
        self.update_num_doc_min(rearr_factors, 4)
        response = self.report.request_json(self.create_request(params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            'incutId': 'vendor_incut',
                            "entity": "searchIncut",
                            'title': 'Предложения vendor_{}'.format(2),  # где ставка выше из 701 и 702 категории
                        },
                    ],
                },
            },
        )

    def test_vendor_incut_apps(self):
        """
        на ios и android поддерживается только вендорская без баннера. Для этого при запросе за ней блендер должен подставлять cgi need_banner=0
        проверяем, что в случае, где для десктопа была врезка с баннером, для ios будет без баннера
        """

        # для десктопа в случае если все вьютайпы поддерживаются должна победить врезка с баннером
        params = self.CgiParams(
            {
                'place': 'blender',
                'text': 'toy',
                'use-default-offers': 1,
                'debug': 'da',
                'allow-collapsing': 1,
                'pp': 18,
                'show-urls': 'productVendorBid',
                'hid': 552,  # вендор с баннером
                'client': 'frontend',
                'platform': 'desktop',
                'supported-incuts': get_supported_incuts_cgi(),
            }
        )
        req_size = 8
        rearr_factors = self.RearrFlags(
            {
                'market_report_blender_vendor_incut_enable': 1,  # разрешение работы вендорской врезки
                'market_vendor_incut_size': req_size,
                'market_vendor_incut_with_CPA_offers_only': 1,
                'market_vendor_incut_hide_undeliverable_models': 0,
                'market_blender_use_bundles_config': 1,
                'market_vendor_incut_enable_banners': 1,  # enable to show vendor
                'market_blender_media_adv_incut_enabled': 0,
            }
        )

        # выигрыш вендора с баннером
        response = self.report.request_json(self.create_request(params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            'incutId': 'vendor_incut_with_banner',
                            "entity": "searchIncut",
                        },
                    ],
                },
            },
            preserve_order=True,
        )

        # для этого же запроса в случае приложений (за счет изменения supported-incuts) должна быть уже врезка без баннера
        params = self.CgiParams(
            {
                'place': 'blender',
                'text': 'toy',
                'use-default-offers': 1,
                'debug': 'da',
                'allow-collapsing': 1,
                'pp': 18,
                'show-urls': 'productVendorBid',
                'hid': 552,  # вендор с баннером
                'client': 'IOS',
                'supported-incuts': "%5Bobject%20Object%5D",  # для старых версий приложений этот параметр был сломан (он конвертируется в {1: [1, 2]})
            }
        )
        req_size = 8
        rearr_factors = self.RearrFlags(
            {
                'market_vendor_incut_size': req_size,
                'market_vendor_incut_with_CPA_offers_only': 1,
                'market_vendor_incut_hide_undeliverable_models': 0,
                'market_vendor_incut_enable_banners': 1,  # enable to show vendor
                'market_blender_use_bundles_config': 1,
                'market_blender_media_adv_incut_enabled': 0,
            }
        )

        # выигрыш вендора без баннера
        response = self.report.request_json(self.create_request(params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            'incutId': 'vendor_incut',
                            "entity": "searchIncut",
                        },
                    ],
                },
            },
            preserve_order=True,
        )

        # проверяем то же самое для андроида
        params['client'] = "ANDROID"
        response = self.report.request_json(self.create_request(params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            'incutId': 'vendor_incut',
                            "entity": "searchIncut",
                        },
                    ],
                },
            },
            preserve_order=True,
        )

    def test_correct_placement(self):
        """
        корректная замена PP в зависимости от платформы и собранной врезки (с баннером или без)
        """
        params = self.CgiParams(
            {
                'place': 'blender',
                'text': 'toy',
                'use-default-offers': 1,
                'debug': 'da',
                'allow-collapsing': 1,
                'pp': 18,
                'show-urls': 'productVendorBid',
                'hid': 551,
                'client': 'frontend',
                'platform': 'desktop',
                'supported-incuts': get_supported_incuts_cgi(),
                'need_banner': 1,
            }
        )
        rearr_factors = self.RearrFlags(
            {
                'market_report_blender_vendor_incut_enable': 1,  # разрешение работы вендорской врезки
                'market_vendor_incut_with_CPA_offers_only': 1,
                'market_vendor_incut_hide_undeliverable_models': 0,
                'market_blender_use_bundles_config': 1,
                # 'market_vendor_incut_enable_banners': 1,
                'market_blender_media_adv_incut_enabled': 0,
            }
        )

        # desktop without banner
        response = self.report.request_json(self.create_request(params, rearr_factors))
        self.assertFragmentIn(response, {'items': [{'urls': {'encrypted': Contains('/pp=8')}}]})

        # touch without banner
        params["touch"] = 1
        response = self.report.request_json(self.create_request(params, rearr_factors))
        self.assertFragmentIn(response, {'items': [{'urls': {'encrypted': Contains('/pp=608')}}]})

        # android without banner
        params["touch"] = 0
        params["client"] = 'ANDROID'
        response = self.report.request_json(self.create_request(params, rearr_factors))
        self.assertFragmentIn(response, {'items': [{'urls': {'encrypted': Contains('/pp=1708')}}]})

        # ios without banner
        params["client"] = 'IOS'
        response = self.report.request_json(self.create_request(params, rearr_factors))
        self.assertFragmentIn(response, {'items': [{'urls': {'encrypted': Contains('/pp=1808')}}]})

        # desktop with banner
        params['client'] = 'frontend'
        params['platform'] = 'desktop'
        rearr_factors['market_vendor_incut_enable_banners'] = 1
        response = self.report.request_json(self.create_request(params, rearr_factors))
        self.assertFragmentIn(response, {'items': [{'urls': {'encrypted': Contains('/pp=8')}}]})

        # touch with banner
        params["touch"] = 1
        response = self.report.request_json(self.create_request(params, rearr_factors))
        self.assertFragmentIn(response, {'items': [{'urls': {'encrypted': Contains('/pp=608')}}]})

        # android with banner
        params["touch"] = 0
        params["client"] = 'ANDROID'
        response = self.report.request_json(self.create_request(params, rearr_factors))
        self.assertFragmentIn(response, {'items': [{'urls': {'encrypted': Contains('/pp=1708')}}]})

        # ios with banner
        params["client"] = 'IOS'
        response = self.report.request_json(self.create_request(params, rearr_factors))
        self.assertFragmentIn(response, {'items': [{'urls': {'encrypted': Contains('/pp=1808')}}]})

    def test_ignore_price_filters(self):
        """
        фильтры по ценам не должны влиять на выдачу в вендорской врезке
        @see https://st.yandex-team.ru/MARKETOUT-45498
        до исправления этой задачи, тест ломался
        """
        params = self.CgiParams(
            {
                'place': 'blender',
                'text': 'toy',
                'use-default-offers': 1,
                'debug': 'da',
                'allow-collapsing': 1,
                'pp': 18,
                'show-urls': 'productVendorBid',
                'hid': 551,
                'client': 'frontend',
                'platform': 'desktop',
                'supported-incuts': get_supported_incuts_cgi(),
                'mcpriceto': 1999,  # цена отсеивает все товары вендора 2
            }
        )

        rearr_factors = self.RearrFlags(
            {
                'market_report_blender_vendor_incut_enable': 1,  # разрешение работы вендорской врезки
                'market_vendor_incut_with_CPA_offers_only': 0,
                'market_vendor_incut_hide_undeliverable_models': 0,
                'market_blender_use_bundles_config': 1,
                'market_blender_media_adv_incut_enabled': 0,
            }
        )

        response = self.report.request_json(self.create_request(params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            'incutId': 'vendor_incut',
                            "entity": "searchIncut",
                            'title': 'Предложения vendor_{}'.format(2),  # вендор 2
                            'items': [
                                {
                                    'vendor': {
                                        'id': 2,
                                    },
                                }
                            ],
                        },
                    ],
                },
            },
            preserve_order=True,
        )

    @classmethod
    def prepare_incuts_without_hid(cls):
        # use data from prepare_correct_urls
        cls.index.navtree += [
            NavCategory(nid=1551, hid=551),
        ]

    def test_incuts_without_hid(self):
        """
        Получение врезки без hid по nid
        @see https://st.yandex-team.ru/MARKETOUT-47485
        """
        params = self.CgiParams(
            {
                'place': 'blender',
                'text': 'toy',
                'use-default-offers': 1,
                'debug': 'da',
                'allow-collapsing': 1,
                'pp': 18,
                'show-urls': 'productVendorBid',
                'client': 'frontend',
                'platform': 'desktop',
                'supported-incuts': get_supported_incuts_cgi(),
                'mcpriceto': 1999,  # цена отсеивает все товары вендора 2
            }
        )

        rearr_factors = self.RearrFlags(
            {
                'market_report_blender_vendor_incut_enable': 1,  # разрешение работы вендорской врезки
                'market_vendor_incut_with_CPA_offers_only': 0,
                'market_vendor_incut_hide_undeliverable_models': 0,
                'market_blender_use_bundles_config': 1,
                'market_blender_media_adv_incut_enabled': 0,
            }
        )
        params['nid'] = 1551  # hid = 551
        rearr_factors['market_blender_incuts_get_hid_from_nid'] = 0
        response = self.report.request_json(self.create_request(params, rearr_factors))
        # expect empty incut
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": ElementCount(0),
                },
            },
        )

        # use nid for hid getting
        rearr_factors['market_blender_incuts_get_hid_from_nid'] = 1
        response = self.report.request_json(self.create_request(params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            'incutId': 'vendor_incut',
                            "entity": "searchIncut",
                            "items": NotEmpty(),
                        }
                    ],
                },
            },
        )


if __name__ == '__main__':
    main()
