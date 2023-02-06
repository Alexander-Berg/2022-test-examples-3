#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.types import (
    Model,
    NavCategory,
    Offer,
    Region,
    Vendor,
)
from core.testcase import main
from core.matcher import Contains, ElementCount, NotEmpty, Regex
from core.blender_bundles import get_supported_incuts_cgi
from market.media_adv.proto.output.output_incuts_pb2 import (
    EIncutType,
    EMediaElementType,
    TBidInfo,
    TColor,
    TColoredText,
    TConstraints,
    THeader,
    TImage,
    TIncut,
    TMediaElement,
    TModel,
    TVendor,
)
from market.report.proto.ReportState_pb2 import TCommonReportState  # noqa pylint: disable=import-error
from test_media_adv import TestMediaAdv


# TODO перенести некоторые тесты из test_blender_bundles_media_adv_incut.py
class T(TestMediaAdv):
    """
    тесты Врезочника (МПФ) на получение врезок при различных условиях
    """

    @classmethod
    def prepare_correct_request(cls):
        cls.index.regiontree += [
            Region(
                rid=200,
                name="Neverwhere",
                region_type=Region.FEDERATIVE_SUBJECT,
            ),
        ]

    def test_correct_request(self):
        """
        проверка формирования корректного запроса во Врезочник
        """
        params, rearr_factors = self.get_params_rearr_factors('toy', 100)
        params['rids'] = 200
        params['debug'] = 1
        response = self.report.request_json(self.get_request(params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                'report': {
                    'logicTrace': [Regex("Sending request '\/incuts\?.*&region=200")],
                },
            },
        )

    @classmethod
    def prepare_correct_urls(cls):
        vendor_id = 45
        vendor_name = 'vendor_{}'.format(vendor_id)
        cls.index.vendors += [Vendor(vendor_id=vendor_id, name=vendor_name)]

        hid = 100
        start_hyperid_1 = 1000
        datasource = 447
        cls.index.models += [
            Model(
                hid=hid,
                hyperid=start_hyperid_1 + x,
                vendor_id=vendor_id,
                vbid=10,
                datasource_id=datasource,
                title="toy {}".format(start_hyperid_1 + x),
            )
            for x in range(0, 10)
        ]
        cls.index.offers += [
            Offer(
                hyperid=start_hyperid_1 + x,
                price=1000 * x,
                cpa=Offer.CPA_REAL,
                hid=hid,
                fee=100,
                bid=100,
                waremd5='RcSMzi4xy{}yxqGvxRx8atA'.format(x),
                fesh=100,
                title="offer for {}".format(start_hyperid_1 + x),
            )
            for x in range(1, 10)
        ]

        logo = TMediaElement(
            Type=EMediaElementType.Logo,
            Id=762,
            SourceImage=TImage(Url="picture_url", Width=800, Height=600),
            PixelUrl="logo_pixel_url",
            ClickUrl="logo_click_url",
            BidInfo=TBidInfo(ClickPrice=502, Bid=1202),
        )

        mock_str = TestMediaAdv.media_adv_mock(
            TIncut(
                Models=[TModel(Id=start_hyperid_1 + i) for i in range(10)],
                Vendor=TVendor(
                    Id=vendor_id,
                    DatasourceId=28195,
                    Name=vendor_name,
                ),
                SaasUrl='saas_url',
                SaasId=761,
                Constraints=TConstraints(
                    MinDocs=1,
                    MaxDocs=10000,
                ),
                BidInfo=TBidInfo(ClickPrice=500, Bid=1200),
                IncutType=EIncutType.ModelsWithAdaptiveBanner,
                Banner=TMediaElement(
                    Type=EMediaElementType.Banner,
                    Id=761,
                    SourceImage=TImage(Url="picture_url", Width=800, Height=600),
                    PixelUrl="banner_pixel_url",
                    ClickUrl="banner_click_url",
                    BidInfo=TBidInfo(ClickPrice=501, Bid=1201),
                    Color=TColor(Background="light"),
                    Text=TColoredText(Text="auto banner text", Color="blue"),
                    Subtitle=TColoredText(Text="subtitle text", Color="subtitle color"),
                    Logos=[logo],
                ),
                SaasRequestHid=105,
                Header=THeader(Type='default', Text='title', Logos=[logo]),
            )
        )
        cls.media_advertising.on_request_media_adv_incut(hid=hid).respond(mock_str)

    def test_correct_urls(self):
        params, rearr_factors = self.get_params_rearr_factors('toy', 100)
        response = self.report.request_json(self.get_request(params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            'inClid': 16,  # == INCLID_MEDIA_ADV_INCUT
                            'incutId': 'basic_carousel_responsive_top',
                            "typeId": 10,  # == MadvBasicCarouselResponsive
                            "titleInfo": {
                                "logo": [
                                    {
                                        "urls": {
                                            "encrypted": Contains(
                                                "madv_incut_id=761",
                                                "hyper_cat_id=100",
                                                "pp=84",
                                                "vendor_ds_id=28195",
                                                "brand_id=45",
                                                "position=1",  # cause is a logo
                                                "vendor_price=502",
                                                "vc_bid=1202",
                                                "url_type=58",
                                                "madv_target_hid=105",
                                            ),
                                        },
                                    }
                                ]
                            },
                            "items": [
                                {
                                    # автобаннер
                                    "entity": "mediaElement",
                                    "type": "banner",
                                    "id": 761,
                                    "title": "auto banner text",
                                    "urls": {
                                        "click": "banner_click_url",
                                        "pixel": "banner_pixel_url",
                                        "encrypted": Contains(
                                            "madv_incut_id=761",
                                            "hyper_cat_id=100",
                                            "pp=84",
                                            "vendor_ds_id=28195",
                                            "brand_id=45",
                                            "position=2",  # cause is a banner
                                            "vendor_price=501",
                                            "vc_bid=1201",
                                            "url_type=58",
                                            "madv_target_hid=105",
                                        ),
                                    },
                                    "logo": [
                                        {
                                            "entity": "mediaElement",
                                            "type": "logo",
                                            "id": 762,
                                            "urls": {
                                                "pixel": "logo_pixel_url",
                                                "click": "logo_click_url",
                                                "encrypted": Contains(
                                                    "madv_incut_id=761",
                                                    "hyper_cat_id=100",
                                                    "pp=84",
                                                    "vendor_ds_id=28195",
                                                    "brand_id=45",
                                                    "position=1",  # cause is a logo
                                                    "vendor_price=502",
                                                    "vc_bid=1202",
                                                    "url_type=58",
                                                    "madv_target_hid=105",
                                                ),
                                            },
                                        }
                                    ],
                                },
                                {
                                    "entity": "product",
                                    "offers": {
                                        "items": [
                                            {
                                                "urls": {
                                                    "cpa": Contains(
                                                        "/madv_incut_id=761/",
                                                    ),
                                                },
                                            },
                                        ],
                                    },
                                    "urls": {
                                        "encrypted": Contains(
                                            "/madv_incut_id=761/",
                                            "/madv_target_hid=105/",
                                        ),
                                    },
                                },
                            ],
                        },
                    ],
                },
            },
        )

    def test_correct_pp(self):
        """
        Проверка корректных PP в ссылках
        @see https://st.yandex-team.ru/MARKETOUT-47089
        """
        params, rearr_factors = self.get_params_rearr_factors('toy', 100)

        def __get_expect_response(pp):
            return {
                "incuts": {
                    "results": [
                        {
                            "items": [
                                {
                                    # автобаннер
                                    'entity': 'mediaElement',
                                    "urls": {
                                        "encrypted": Contains(
                                            "pp={}".format(pp),
                                        ),
                                    },
                                    "logo": [
                                        {
                                            "urls": {
                                                "encrypted": Contains(
                                                    "pp={}".format(pp),
                                                ),
                                            },
                                        }
                                    ],
                                },
                                {
                                    "entity": "product",
                                    'urls': {
                                        'encrypted': Contains(
                                            'pp={}'.format(pp),
                                        ),
                                    },
                                    "offers": {
                                        "items": [
                                            {
                                                "urls": {
                                                    "cpa": Contains(
                                                        "pp={}".format(pp),
                                                    ),
                                                },
                                            },
                                        ],
                                    },
                                },
                            ],
                        },
                    ],
                },
            }

        # desktop
        params['platform'] = 'desktop'
        params['client'] = 'frontend'
        response = self.report.request_json(self.get_request(params, rearr_factors))
        self.assertFragmentIn(response, __get_expect_response(84))

        # touch
        params['touch'] = 1
        response = self.report.request_json(self.get_request(params, rearr_factors))
        self.assertFragmentIn(response, __get_expect_response(684))

        # android
        params.pop('platform')
        params.pop('touch')
        params['client'] = 'ANDROID'
        response = self.report.request_json(self.get_request(params, rearr_factors))
        self.assertFragmentIn(response, __get_expect_response(1784))

        # ios
        params['client'] = 'IOS'
        response = self.report.request_json(self.get_request(params, rearr_factors))
        self.assertFragmentIn(response, __get_expect_response(1884))

    def test_correct_pp_for_widget(self):
        """
        Проверка корректных PP в ссылках при запросе в madv_incut с &client=widget
        @see https://st.yandex-team.ru/MARKETOUT-47089
        """
        params, rearr_factors = self.get_params_rearr_factors('toy', 100)
        params.pop('platform')
        params.pop('pp')
        params['client'] = 'widget'
        params['place'] = 'madv_incut'
        params['supported-incuts'] = get_supported_incuts_cgi({"101": ["8"]})
        response = self.report.request_json(self.get_request(params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'incutId': 'MadvCardCarousel',
                            "typeId": 10,
                            "titleInfo": {},
                            "items": [
                                {
                                    "entity": "product",
                                    "offers": {
                                        "items": [
                                            {
                                                "urls": {
                                                    "cpa": Contains(
                                                        "/pp=926/",
                                                    ),
                                                },
                                            },
                                        ],
                                    },
                                },
                            ],
                        }
                    ],
                },
            },
        )
        for pos in range(9):
            self.show_log.expect(madv_incut_id=761, position=pos + 1, pp=926, url_type=6)
            self.show_log.expect(madv_incut_id=761, position=pos + 1, pp=926, url_type=16)

    @classmethod
    def prepare_incuts_without_hid(cls):
        # use data from prepare_correct_urls
        cls.index.navtree += [
            NavCategory(nid=255, hid=100),
        ]

    def test_incuts_without_hid(self):
        """
        Получение врезки без hid по nid
        @see https://st.yandex-team.ru/MARKETOUT-47485
        """
        params, rearr_factors = self.get_params_rearr_factors('toy', 100)
        params.pop('hid')
        params['nid'] = 255  # hid = 100
        rearr_factors['market_blender_incuts_get_hid_from_nid'] = 0
        response = self.report.request_json(self.get_request(params, rearr_factors))
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
        response = self.report.request_json(self.get_request(params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            'inClid': 16,  # == INCLID_MEDIA_ADV_INCUT
                            'incutId': 'basic_carousel_responsive_top',
                            "typeId": 10,  # == MadvBasicCarouselResponsive
                            "titleInfo": {},
                            "items": NotEmpty(),
                        }
                    ],
                },
            },
        )

    @classmethod
    def prepare_multiple_places(cls):
        # создаём 5 врезок
        vendor_ids = range(46, 51)
        vendor_names = ['vendor_{}'.format(vendor_id) for vendor_id in vendor_ids]
        cls.index.vendors += [
            Vendor(vendor_id=vendor_id, name=vendor_name) for vendor_id, vendor_name in zip(vendor_ids, vendor_names)
        ]

        hid = 101
        start_datasource = 448
        incuts = []
        models_in_incut = 10
        for i, vendor_id in enumerate(vendor_ids):
            start_hyperid = 1100 + i * models_in_incut
            hyperids = range(start_hyperid, start_hyperid + models_in_incut)
            datasource = start_datasource + i
            cls.index.models += [
                Model(
                    hid=hid,
                    hyperid=hyperid,
                    vendor_id=vendor_id,
                    vbid=10,
                    datasource_id=datasource,
                    title="window {}".format(hyperid),
                )
                for hyperid in hyperids
            ]
            num_offers = models_in_incut if i < 3 else 2
            cls.index.offers += [
                Offer(
                    hyperid=start_hyperid + x,
                    price=1000 * (x + 1),
                    cpa=Offer.CPA_REAL,
                    hid=hid,
                    fee=100,
                    bid=100,
                    waremd5='RcSMzi4xy{0}yxqGvxR{1}8atA'.format(x, i),
                    fesh=100,
                    title="offer for {}".format(start_hyperid + x),
                )
                for x in range(num_offers)
            ]
            incuts.append(
                TestMediaAdv.create_incut(
                    models=hyperids,
                    vendor_id=vendor_id,
                    incut_id=770 + i,
                    vendor_name=vendor_names[i],
                    datasource_id=datasource,
                )
            )

        # incuts[0:3] -- ДО набираются
        # для incuts[3], incuts[4] -- не набираются

        # ожидаем [incuts[0], incuts[2]]
        incuts_response0 = TestMediaAdv.create_incut_response(
            [[incuts[0], incuts[1], incuts[3]], [incuts[2], incuts[4], incuts[0]]]
        )
        mock_str0 = TestMediaAdv.media_adv_mock(incuts_response0)
        cls.media_advertising.on_request_media_adv_incut(hid=hid).respond(mock_str0)

        # ожидаем [incuts[1], incuts[2]]
        incuts_response1 = TestMediaAdv.create_incut_response([[incuts[3], incuts[1]], [incuts[2], incuts[4]]])
        mock_str1 = TestMediaAdv.media_adv_mock(incuts_response1)
        cls.media_advertising.on_request_media_adv_incut(hid=hid + 1).respond(mock_str1)

        # ожидаем [incuts[0], incuts[1]]
        incuts_response2 = TestMediaAdv.create_incut_response(
            [[incuts[0], incuts[2], incuts[3]], [incuts[3], incuts[4], incuts[1]]]
        )
        mock_str2 = TestMediaAdv.media_adv_mock(incuts_response2)
        cls.media_advertising.on_request_media_adv_incut(hid=hid + 2).respond(mock_str2)

        # ожидаем [incuts[0], incuts[1]]
        incuts_response3 = TestMediaAdv.create_incut_response(
            [[incuts[3], incuts[0], incuts[4]], [incuts[0], incuts[1], incuts[2]]]
        )
        mock_str3 = TestMediaAdv.media_adv_mock(incuts_response3)
        cls.media_advertising.on_request_media_adv_incut(hid=hid + 3).respond(mock_str3)

        # ожидаем [incuts[0]] на поиске
        incuts_response4 = TestMediaAdv.create_incut_response(
            [[incuts[3], incuts[4]], [incuts[0], incuts[1], incuts[2]]]
        )
        mock_str4 = TestMediaAdv.media_adv_mock(incuts_response4)
        cls.media_advertising.on_request_media_adv_incut(hid=hid + 4).respond(mock_str4)

        # ожидаем [incuts[0]] в топе
        incuts_response5 = TestMediaAdv.create_incut_response([[incuts[0], incuts[1]], [incuts[3], incuts[0]]])
        mock_str5 = TestMediaAdv.media_adv_mock(incuts_response5)
        cls.media_advertising.on_request_media_adv_incut(hid=hid + 5).respond(mock_str5)

        # ожидаем пустой ответ
        incuts_response6 = TestMediaAdv.create_incut_response([[incuts[4]], [incuts[3]]])
        mock_str6 = TestMediaAdv.media_adv_mock(incuts_response6)
        cls.media_advertising.on_request_media_adv_incut(hid=hid + 6).respond(mock_str6)

        start_hyperid_2 = 1200
        for i in range(7):
            cls.index.models += [
                Model(
                    hid=(hid + i),
                    hyperid=start_hyperid_2 + 10 * i + j,
                    vendor_id=52,
                    vbid=10,
                    datasource_id=458,
                    title="window {}".format(hyperid),
                )
                for j in range(10)
            ]

    def test_multiple_places(self):
        """
        Возврат врезки для каждого плейсмента
        @see https://st.yandex-team.ru/MEDIAADV-195
        """

        def __get_expected_response(incut_id_top=None, incut_id_search=None):
            results = []
            if incut_id_top is not None:
                results.append(
                    {
                        'inClid': 16,  # == INCLID_MEDIA_ADV_INCUT
                        'typeId': 8,
                        'brand_id': incut_id_top - 724,  # brand_id = 46 if madvIncutId = 770
                        'incutId': 'basic_carousel_plain_top',
                        'madvIncutId': incut_id_top,
                        'items': ElementCount(10),
                    }
                )
            if incut_id_search is not None:
                results.append(
                    {
                        'inClid': 16,  # == INCLID_MEDIA_ADV_INCUT
                        'typeId': 8,
                        'brand_id': incut_id_search - 724,
                        'incutId': 'basic_carousel_plain_search',
                        'madvIncutId': incut_id_search,
                        'items': ElementCount(10),
                    }
                )
            return {
                "incuts": {
                    "results": results,
                },
            }

        def __test_hid_case(hid, incut_id_top, incut_id_search, madv_incuts_limit=2):
            params, rearr_factors = self.get_params_rearr_factors('window', hid)
            rearr_factors['market_media_adv_incut_on_search_place'] = True
            rearr_factors['market_max_madv_incuts_on_search'] = madv_incuts_limit
            response = self.report.request_json(self.get_request(params, rearr_factors))

            expected = __get_expected_response(incut_id_top, incut_id_search)
            self.assertFragmentIn(response, expected)
            self.assertFragmentIn(response, {'incuts': {'results': ElementCount(len(expected['incuts']['results']))}})

        __test_hid_case(101, 770, 772)
        __test_hid_case(102, 771, 772)
        __test_hid_case(103, 770, 771)
        __test_hid_case(104, 770, 771)
        __test_hid_case(105, None, 770)
        __test_hid_case(106, 770, None)
        __test_hid_case(107, None, None)

        __test_hid_case(101, 770, None, 1)
        __test_hid_case(102, 771, None, 1)
        __test_hid_case(103, 770, None, 1)
        __test_hid_case(104, 770, None, 1)
        __test_hid_case(105, None, 770, 1)
        __test_hid_case(106, 770, None, 1)
        __test_hid_case(107, None, None, 1)

    def test_correct_place_single_incut(self):
        """
        Выключаем флаг market_media_adv_incut_on_search_place.
        В этом случае из врезочника возвращается одна врезка,
        проверяем место её показа
        """
        params, rearr_factors = self.get_params_rearr_factors('toy', 100)
        rearr_factors['market_media_adv_incut_on_search_place'] = False
        rearr_factors['market_max_madv_incuts_on_search'] = 1

        response = self.report.request_json(self.get_request(params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {"madvIncutId": 761, "placeId": 2, "position": 1},
                    ],
                },
            },
        )
        self.assertEqual(len(response['incuts']['results']), 1)

        params, rearr_factors = self.get_params_rearr_factors('toy', 100)
        params['client'] = 'ANDROID'
        params['touch'] = 0
        params.pop('platform')
        rearr_factors['market_media_adv_incut_on_search_place'] = False
        rearr_factors['market_max_madv_incuts_on_search'] = 1
        response = self.report.request_json(self.get_request(params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {"madvIncutId": 761, "placeId": 1, "position": 1},
                    ],
                },
            },
        )
        self.assertEqual(len(response['incuts']['results']), 1)

        params['client'] = 'IOS'
        response = self.report.request_json(self.get_request(params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {"madvIncutId": 761, "placeId": 1, "position": 1},
                    ],
                },
            },
        )
        self.assertEqual(len(response['incuts']['results']), 1)

    def test_correct_place_two_incuts(self):
        """
        Включаем флаг market_media_adv_incut_on_search_place.
        В этом случае из врезочника возвращается две врезки.
        Устанавливаем market_max_madv_incuts_on_search = 1
        Должна остаться только одна врезка.
        Проверяем, что остаётся правильная и показывается на правильном месте.
        Поскольку разрешаем МПФ-врезки в поиске, то используем бандлы, которые разрешают
        врезке из топа спускаться на 4 позицию
        """
        params, rearr_factors = self.get_params_rearr_factors('window', 101)
        rearr_factors['market_media_adv_incut_on_search_place'] = True
        rearr_factors['market_blender_bundles_for_inclid'] = "16:const_media_adv_incut_lowering.json"
        rearr_factors['market_max_madv_incuts_on_search'] = 1

        response = self.report.request_json(self.get_request(params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {"madvIncutId": 770, "placeId": 2, "position": 1},
                    ],
                },
            },
        )
        self.assertEqual(len(response['incuts']['results']), 1)

        params, rearr_factors = self.get_params_rearr_factors('window', 101)
        params['client'] = 'ANDROID'
        params['touch'] = 0
        params.pop('platform')
        rearr_factors['market_media_adv_incut_on_search_place'] = True
        rearr_factors['market_blender_bundles_for_inclid'] = "16:const_media_adv_incut_search_lowering.json"
        rearr_factors['market_max_madv_incuts_on_search'] = 1
        response = self.report.request_json(self.get_request(params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {"madvIncutId": 770, "placeId": 1, "position": 1},
                    ],
                },
            },
        )
        self.assertEqual(len(response['incuts']['results']), 1)

        params['client'] = 'IOS'
        response = self.report.request_json(self.get_request(params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {"madvIncutId": 770, "placeId": 1, "position": 1},
                    ],
                },
            },
        )
        self.assertEqual(len(response['incuts']['results']), 1)

    @classmethod
    def prepare_madv_incut_on_search_place(cls):
        # создаём ответ врезочника с 2 врезками: пустой и допохватной
        vendor_id = 51
        vendor_name = 'vendor_{}'.format(vendor_id)
        cls.index.vendors += [Vendor(vendor_id=vendor_id, name=vendor_name)]

        hid = 200
        start_hyperid_1 = 1300
        datasource = 460
        incut_id = 780
        cls.index.models += [
            Model(
                hid=hid,
                hyperid=start_hyperid_1 + x,
                vendor_id=vendor_id,
                vbid=10,
                datasource_id=datasource,
                title="допохват {}".format(start_hyperid_1 + x),
            )
            for x in range(0, 10)
        ]
        cls.index.offers += [
            Offer(
                hyperid=start_hyperid_1 + x,
                price=1000 * x,
                cpa=Offer.CPA_REAL,
                hid=hid,
                fee=100,
                bid=100,
                fesh=100,
                title="оффер для допохвата {}".format(start_hyperid_1 + x),
            )
            for x in range(1, 10)
        ]

        incut = TIncut(
            Models=[TModel(Id=start_hyperid_1 + i) for i in range(10)],
            Vendor=TVendor(
                Id=vendor_id,
                DatasourceId=datasource,
                Name=vendor_name,
            ),
            SaasUrl='saas_url',
            SaasId=incut_id,
            Constraints=TConstraints(
                MinDocs=1,
                MaxDocs=10000,
            ),
            BidInfo=TBidInfo(ClickPrice=500, Bid=1200),
            IncutType=EIncutType.ModelsWithAdaptiveBanner,
            Banner=TMediaElement(
                Type=EMediaElementType.Banner,
                Id=incut_id,
                SourceImage=TImage(Url="picture_url", Width=800, Height=600),
                PixelUrl="banner_pixel_url",
                ClickUrl="banner_click_url",
                BidInfo=TBidInfo(ClickPrice=501, Bid=1201),
                Color=TColor(Background="light"),
                Text=TColoredText(Text="auto banner text", Color="blue"),
                Subtitle=TColoredText(Text="subtitle text", Color="subtitle color"),
                Logos=[
                    TMediaElement(
                        Type=EMediaElementType.Logo,
                        Id=(incut_id + 1),
                        SourceImage=TImage(Url="picture_url", Width=800, Height=600),
                        PixelUrl="logo_pixel_url",
                        ClickUrl="logo_click_url",
                        BidInfo=TBidInfo(ClickPrice=502, Bid=1202),
                    ),
                ],
            ),
            Header=THeader(
                Type='default',
                Text='title',
            ),
        )

        mock_str = TestMediaAdv.media_adv_mock(TestMediaAdv.create_incut_response([[TIncut()], [incut]]))
        cls.media_advertising.on_request_media_adv_incut(hid=hid).respond(mock_str)

    def test_correct_place_one_search_incut(self):
        """
        Включаем флаг market_media_adv_incut_on_search_place.
        В этом случае из врезочника возвращается две врезки.
        Та, которая для топа, будет пустой.
        Устанавливаем market_max_madv_incuts_on_search = 1
        Должна остаться только одна врезка, не пустая.
        Проверяем, что она показывается на 4 позиции.
        Поскольку разрешаем МПФ-врезки в поиске, то используем бандлы, которые разрешают
        врезке из топа спускаться на 4 позицию
        """
        params, rearr_factors = self.get_params_rearr_factors('допохват', 200)
        rearr_factors['market_media_adv_incut_on_search_place'] = True
        rearr_factors['market_blender_bundles_for_inclid'] = "16:const_media_adv_incut_lowering.json"
        rearr_factors['market_max_madv_incuts_on_search'] = 1

        response = self.report.request_json(self.get_request(params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {"madvIncutId": 780, "placeId": 1, "position": 4},
                    ],
                },
            },
        )
        self.assertEqual(len(response['incuts']['results']), 1)

        # выключаем supported-incuts для топа, ничего не меняется
        params['supported-incuts'] = get_supported_incuts_cgi({"1": ["8", "9", "10", "11", "18"]})
        response = self.report.request_json(self.get_request(params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {"madvIncutId": 780, "placeId": 1, "position": 4},
                    ],
                },
            },
        )
        self.assertEqual(len(response['incuts']['results']), 1)

        params, rearr_factors = self.get_params_rearr_factors('допохват', 200)
        params['client'] = 'ANDROID'
        params['touch'] = 0
        params.pop('platform')
        rearr_factors['market_media_adv_incut_on_search_place'] = True
        rearr_factors['market_blender_bundles_for_inclid'] = "16:const_media_adv_incut_search_lowering.json"
        rearr_factors['market_max_madv_incuts_on_search'] = 1
        response = self.report.request_json(self.get_request(params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {"madvIncutId": 780, "placeId": 1, "position": 4},
                    ],
                },
            },
        )
        self.assertEqual(len(response['incuts']['results']), 1)

        params['client'] = 'IOS'
        response = self.report.request_json(self.get_request(params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {"madvIncutId": 780, "placeId": 1, "position": 4},
                    ],
                },
            },
        )
        self.assertEqual(len(response['incuts']['results']), 1)

    @classmethod
    def prepare_title_replacement(cls):
        vendor_ids = [52, 53, 54]
        vendor_names = map(lambda v_id: 'vendor_{}'.format(v_id), vendor_ids)
        cls.index.vendors += [
            Vendor(vendor_id=vendor_id, name=vendor_name) for vendor_id, vendor_name in zip(vendor_ids, vendor_names)
        ]
        hid = 210
        datasources = [449, 450, 451]
        models_in_incut = 10
        incuts = []
        for i, vendor_id in enumerate(vendor_ids):
            start_hyper_id = 1400 + i * models_in_incut
            hyperids = range(start_hyper_id, start_hyper_id + models_in_incut)
            cls.index.models += [
                Model(
                    hid=(hid if vendor_id < 54 else hid + 1),
                    hyperid=hyperid,
                    vendor_id=vendor_id,
                    vbid=10,
                    datasource_id=datasources[i],
                    title="branch {}".format(hyperid),
                )
                for hyperid in hyperids
            ]
            cls.index.offers += [
                Offer(
                    hyperid=hyperids[x],
                    price=1000 * x,
                    cpa=Offer.CPA_REAL,
                    hid=(hid if vendor_id < 54 else hid + 1),
                    fee=100,
                    bid=100,
                    fesh=100,
                    title="offer for {}".format(hyperids[x]),
                )
                for x in range(1, 10)
            ]
            incuts.append(
                TestMediaAdv.create_incut(
                    models=hyperids,
                    vendor_id=vendor_id,
                    incut_id=790 + i,
                    vendor_name=vendor_names[i],
                    datasource_id=datasources[i],
                    header_text=(
                        'Идеи для покупок от {}'.format(vendor_names[i])
                        if vendor_id < 54
                        else 'Врезка с оригинальным названием от {}'.format(vendor_names[i])
                    ),
                )
            )
        # первый ответ -- заголовок обеих врезок "Идеи для покупок от ..."
        incuts_response = TestMediaAdv.create_incut_response([[incuts[0]], [incuts[1]]])
        mock_str = TestMediaAdv.media_adv_mock(incuts_response)
        cls.media_advertising.on_request_media_adv_incut(hid=hid).respond(mock_str)
        # второй ответ -- заголовок второй врезки отличается
        incuts_response2 = TestMediaAdv.create_incut_response([[incuts[0]], [incuts[2]]])
        mock_str2 = TestMediaAdv.media_adv_mock(incuts_response2)
        cls.media_advertising.on_request_media_adv_incut(hid=hid + 1).respond(mock_str2)

    def test_title_replacement(self):
        """
        Получаем две врезки. "Идеи для покупок от" в заголовке у второй надо по флагу
        заменить на "Предложения от".
        """
        params, rearr_factors = self.get_params_rearr_factors('branch', 210)
        rearr_factors['market_media_adv_incut_on_search_place'] = 1
        rearr_factors['market_max_madv_incuts_on_search'] = 2
        rearr_factors['market_replace_second_madv_incut_title'] = 1
        response = self.report.request_json(self.get_request(params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            "madvIncutId": 790,
                            "titleInfo": {
                                "titleText": "Идеи для покупок от vendor_52",
                                "logo": [
                                    {
                                        "title": "Идеи для покупок от vendor_52",
                                        "text": {
                                            "text": "Идеи для покупок от vendor_52",
                                        },
                                    }
                                ],
                            },
                        },
                        {
                            "madvIncutId": 791,
                            "titleInfo": {
                                "titleText": "Предложения от vendor_53",
                                "logo": [
                                    {
                                        "title": "Предложения от vendor_53",
                                        "text": {
                                            "text": "Предложения от vendor_53",
                                        },
                                    }
                                ],
                            },
                        },
                    ]
                }
            },
        )

        # выключаем флаг. Без него заменять заголовки не будем
        rearr_factors['market_replace_second_madv_incut_title'] = 0
        response = self.report.request_json(self.get_request(params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            "madvIncutId": 790,
                            "titleInfo": {
                                "titleText": "Идеи для покупок от vendor_52",
                                "logo": [
                                    {
                                        "title": "Идеи для покупок от vendor_52",
                                        "text": {
                                            "text": "Идеи для покупок от vendor_52",
                                        },
                                    }
                                ],
                            },
                        },
                        {
                            "madvIncutId": 791,
                            "titleInfo": {
                                "titleText": "Идеи для покупок от vendor_53",
                                "logo": [
                                    {
                                        "title": "Идеи для покупок от vendor_53",
                                        "text": {
                                            "text": "Идеи для покупок от vendor_53",
                                        },
                                    }
                                ],
                            },
                        },
                    ]
                }
            },
        )

        # если у второй врезки оригинальное название, не меняем его
        params, rearr_factors = self.get_params_rearr_factors('branch', 211)
        rearr_factors['market_media_adv_incut_on_search_place'] = 1
        rearr_factors['market_max_madv_incuts_on_search'] = 2
        rearr_factors['market_replace_second_madv_incut_title'] = 1
        response = self.report.request_json(self.get_request(params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            "madvIncutId": 790,
                            "titleInfo": {
                                "titleText": "Идеи для покупок от vendor_52",
                                "logo": [
                                    {
                                        "title": "Идеи для покупок от vendor_52",
                                        "text": {
                                            "text": "Идеи для покупок от vendor_52",
                                        },
                                    }
                                ],
                            },
                        },
                        {
                            "madvIncutId": 792,
                            "titleInfo": {
                                "titleText": "Врезка с оригинальным названием от vendor_54",
                                "logo": [
                                    {
                                        "title": "Врезка с оригинальным названием от vendor_54",
                                        "text": {
                                            "text": "Врезка с оригинальным названием от vendor_54",
                                        },
                                    }
                                ],
                            },
                        },
                    ]
                }
            },
        )


if __name__ == '__main__':
    main()
