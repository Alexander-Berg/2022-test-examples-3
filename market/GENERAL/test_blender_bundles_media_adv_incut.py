#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    Model,
    Offer,
    Vendor,
    Shop,
    NavCategory,
    ReportState,
    Promo,
    DynamicBlueGenericBundlesPromos,
    PromoType,
    GLParam,
    GLType,
    HyperCategory,
)
from core.types.offer_promo import make_generic_bundle_content
from core.types.autogen import Const
from core.testcase import main
from core.matcher import Contains, ElementCount, EmptyList, NotEmptyList
from core.cpc import Cpc
from core.click_context import ClickContext
from market.report.proto.ReportState_pb2 import TCommonReportState  # noqa pylint: disable=import-error
from test_media_adv import TestMediaAdv

from market.media_adv.proto.output.output_incuts_pb2 import (
    EIncutType,
    EMediaElementType,
    TBidInfo,
    TConstraints,
    TColor,
    TColoredText,
    THeader,
    TImage,
    TIncut,
    TIncutList,
    TIncutTypeInfo,
    TMediaElement,
    TModel,
    TVendor,
)


class T(TestMediaAdv):
    @classmethod
    def prepare_basic_carousel_plain(cls):
        cls.index.vendors += [Vendor(vendor_id=x, name='vendor_{}'.format(x)) for x in range(1, 4)]

        hid = 551
        start_hyperid_1 = 1300
        cls.index.models += [
            Model(
                hid=hid,
                hyperid=start_hyperid_1 + x,
                vendor_id=1,
                vbid=10,
                datasource_id=444,
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

        start_hyperid_2 = start_hyperid_1 + 10
        cls.index.models += [
            Model(
                hid=hid,
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
                price=1000 * x,
                cpa=Offer.CPA_NO,  # should not be a CPA
                hid=hid,
                title="offer for {}".format(start_hyperid_2 + x),
            )
            for x in range(1, 10)
        ]

        mock_str = TestMediaAdv.media_adv_mock(
            TIncut(
                Models=[TModel(Id=start_hyperid_1 + i) for i in range(10)],
                SaasId=761,
                IncutType=EIncutType.ModelsList,
                Vendor=TVendor(
                    DatasourceId=28195,
                    Name='vendor_1',
                    Id=1,
                ),
                Constraints=TConstraints(
                    MinDocs=1,
                    MaxDocs=10000,
                ),
                BidInfo=TBidInfo(ClickPrice=500, Bid=1200),
                SaasRequestHid=651,
                AlternativeIncutTypes=[
                    TIncutTypeInfo(
                        IncutType=EIncutType.ModelsList3Items,
                        MinDocs=3,
                        MaxDocs=9,
                    ),
                ],
                Header=THeader(
                    Type='default',
                    Text='media_element text',
                    Logos=[
                        TMediaElement(
                            Type=EMediaElementType.Logo,
                            Text=TColoredText(
                                Text="media_element text",
                            ),
                            Id=761,
                            BidInfo=TBidInfo(
                                ClickPrice=500,
                                Bid=1200,
                            ),
                            SourceImage=TImage(
                                Url='https://avatars.mds.yandex.net/get-mpic/1862933/img_id5477847296568171778.png/orig',
                                Width=800,
                                Height=600,
                            ),
                        )
                    ],
                ),
            )
        )
        cls.media_advertising.on_request_media_adv_incut(hid=hid).respond(mock_str)

    def test_basic_carousel_plain(self):
        """
        Проверяем работу мока врезочника,
        проверяем поход за модельками из новой врезки
        проверка выдачи корректного урла с правильным урлом, содержащим madv_incut_id
        """
        params, rearr_factors = self.get_params_rearr_factors('toy', 551)
        response = self.report.request_json(self.get_request(params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            'inClid': 16,  # == INCLID_MEDIA_ADV_INCUT
                            'incutId': 'basic_carousel_plain_top',
                            "entity": "searchIncut",
                            "brand_id": 1,
                            "typeId": 8,  # == MadvBasicCarouselPlain
                            "targetHid": 651,
                            "madvIncutId": 761,
                            "titleInfo": {
                                "type": "default",
                                "titleText": "media_element text",
                                "logo": [
                                    {
                                        "entity": "mediaElement",
                                        "type": "logo",
                                        "id": 761,
                                        "title": "media_element text",
                                        "urls": {
                                            "pixel": "",
                                            "click": "",
                                        },
                                        'image': {
                                            'url': 'https://avatars.mds.yandex.net/get-mpic/1862933/img_id5477847296568171778.png/orig',
                                            'width': 800,
                                            'height': 600,
                                        },
                                    }
                                ],
                            },
                            "items": ElementCount(9),  # берём только модели с ДО
                        },
                    ],
                },
            },
            preserve_order=True,
        )

        for item in response['incuts']['results'][0]['items']:
            offer_items = item["offers"]["items"]
            # проверяем, что у всех моделей в выдаче один оффер
            self.assertEqual(len(offer_items), 1)

        # проверим, что в урле и в cpc у модели и оффера стоят правильные pp
        offer_cpc = Cpc.create_for_offer(
            click_price=2,
            offer_id='RcSMzi4xy1yxqGvxRx8atA',
            bid=2,
            minimal_bid=2,
            shop_id=100,
            shop_fee=100,
            fee=0,
            minimal_fee=0,
            bid_type='cbid',
            hid=551,
            click_price_before_bid_correction=2,
            pp=84,
        )
        model_cpc = Cpc.create_for_model(
            model_id=1301,
            vendor_click_price=500,
            vendor_bid=1200,
            pp=84,
        )

        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            "items": [
                                {
                                    "entity": "product",
                                    "id": 1301,
                                    "cpc": str(model_cpc),
                                    "urls": {
                                        "encrypted": Contains(
                                            "/madv_incut_id=761/",
                                            "/vc_bid=1200/",
                                            "/vendor_price=500/",
                                            "/pp=84/",
                                            "/vendor_ds_id=28195/",
                                        ),
                                    },
                                    "offers": {
                                        "items": [
                                            {
                                                "cpc": str(offer_cpc),
                                            },
                                        ],
                                    },
                                }
                            ]
                        },
                    ],
                },
            },
            preserve_order=True,
        )

        # отключение МПФ врезки
        rearr_factors['market_blender_media_adv_incut_enabled'] = 0
        response = self.report.request_json(self.get_request(params, rearr_factors))
        self.assertFragmentNotIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            'inClid': 16,  # любая МПФ врезка
                        },
                    ],
                },
            },
            preserve_order=True,
        )

    def test_basic_carousel_plain_for_touch(self):
        """
        выдача карусели на таче, так же как и на десктопе
        """
        params, rearr_factors = self.get_params_rearr_factors('toy', 551)
        params['touch'] = 1
        response = self.report.request_json(self.get_request(params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            'inClid': 16,  # == INCLID_MEDIA_ADV_INCUT
                            'incutId': 'basic_carousel_plain_top',
                            "entity": "searchIncut",
                            "brand_id": 1,
                            "typeId": 8,  # == MadvBasicCarouselPlain
                            "titleInfo": {
                                "type": "default",
                                "titleText": "media_element text",
                                "logo": ElementCount(1),
                            },
                            "items": ElementCount(9),  # берём только модели с ДО
                        },
                    ],
                },
            },
            preserve_order=True,
        )

    def test_basic_carousel_plain_for_android(self):
        """
        выдача карусели на андройде, так же как и на десктопе
        """
        params, rearr_factors = self.get_params_rearr_factors('toy', 551)
        params['client'] = 'ANDROID'
        params['touch'] = 0
        params.pop('platform')
        response = self.report.request_json(self.get_request(params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            'inClid': 16,  # == INCLID_MEDIA_ADV_INCUT
                            'incutId': 'basic_carousel_plain_top',
                            "entity": "searchIncut",
                            "brand_id": 1,
                            "typeId": 8,  # == MadvBasicCarouselPlain
                            "titleInfo": {
                                "type": "default",
                                "titleText": "media_element text",
                                "logo": ElementCount(1),
                            },
                            "items": ElementCount(9),  # берём только модели с ДО
                        },
                    ],
                },
            },
            preserve_order=True,
        )

    @classmethod
    def prepare_basic_carousel_image(cls):
        vendor_id = 11
        vendor_name = 'vendor_{}'.format(vendor_id)
        cls.index.vendors += [Vendor(vendor_id=vendor_id, name=vendor_name)]

        hid = 552
        start_hyperid_1 = 1400
        cls.index.models += [
            Model(
                hid=hid,
                hyperid=start_hyperid_1 + x,
                vendor_id=vendor_id,
                vbid=10,
                title="stone {}".format(start_hyperid_1 + x),
            )
            for x in range(0, 10)
        ]
        cls.index.offers += [
            Offer(
                hyperid=start_hyperid_1 + x,
                price=1000 * x,
                cpa=Offer.CPA_REAL,
                hid=hid,
                title="offer for {}".format(start_hyperid_1 + x),
            )
            for x in range(1, 10)
        ]

        mock_str = TestMediaAdv.media_adv_mock(
            TIncut(
                Models=[TModel(Id=start_hyperid_1 + i) for i in range(10)],
                SaasId=771,
                IncutType=EIncutType.ModelsWithBanner,
                Vendor=TVendor(
                    DatasourceId=28195 + vendor_id,
                    Name=vendor_name,
                    Id=vendor_id,
                ),
                Constraints=TConstraints(
                    MinDocs=1,
                    MaxDocs=10000,
                ),
                BidInfo=TBidInfo(ClickPrice=0, Bid=1200),
                AlternativeIncutTypes=[
                    TIncutTypeInfo(
                        IncutType=EIncutType.ModelsList3Items,
                        MinDocs=3,
                        MaxDocs=9,
                    ),
                ],
                Header=THeader(
                    Type='default',
                    Text='title',
                ),
                Banner=TMediaElement(
                    Type=EMediaElementType.Banner,
                    Id=771,
                    SourceImage=TImage(
                        Url='https://avatars.mds.yandex.net/get-mpic/1862933/img_id5477847296568171778.png/orig',
                        Width=800,
                        Height=600,
                    ),
                    BidInfo=TBidInfo(
                        ClickPrice=0,
                        Bid=1200,
                    ),
                ),
            )
        )
        cls.media_advertising.on_request_media_adv_incut(hid=hid).respond(mock_str)

    def test_basic_carousel_image(self):
        """
        Проверяем выдачу врезки MadvBasicCarouselImage
        """

        params, rearr_factors = self.get_params_rearr_factors('stone', 552)
        response = self.report.request_json(self.get_request(params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            'inClid': 16,  # == INCLID_MEDIA_ADV_INCUT
                            'incutId': 'basic_carousel_image_top',
                            "entity": "searchIncut",
                            "brand_id": 11,
                            "typeId": 9,  # == MadvBasicCarouselImage
                            "titleInfo": {
                                "type": "default",
                            },
                            "items": ElementCount(9),  # берём только модели с ДО
                        },
                    ],
                },
            },
            preserve_order=True,
        )

        # проверяем, что у всех моделей в выдаче один оффер
        for item in response['incuts']['results'][0]['items']:
            self.assertEqual(len(item["offers"]["items"]), 1)

        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            "items": [
                                {
                                    "urls": {
                                        "encrypted": Contains("madv_incut_id=771"),
                                    }
                                }
                            ]
                        },
                    ],
                },
            },
            preserve_order=True,
        )

    @classmethod
    def prepare_basic_carousel_responsive(cls):
        vendor_id = 51
        vendor_name = 'vendor_{}'.format(vendor_id)
        cls.index.vendors += [Vendor(vendor_id=vendor_id, name=vendor_name)]

        num_models = 6
        hid = 557
        start_hyperid = 2100
        cls.index.models += [
            Model(
                hid=hid,
                hyperid=start_hyperid + x,
                vendor_id=vendor_id,
                vbid=10,
                title="tool {}".format(start_hyperid + x),
            )
            for x in range(num_models)
        ]
        cls.index.offers += [
            Offer(
                hyperid=start_hyperid + x,
                price=1000 * (x + 1),
                cpa=Offer.CPA_REAL,
                hid=hid,
                fee=100,
                bid=100,
                fesh=100,
                title="offer for {}".format(start_hyperid + x),
            )
            for x in range(num_models)
        ]

        mock_str = TestMediaAdv.media_adv_mock(
            TIncut(
                Models=[TModel(Id=start_hyperid + i) for i in range(num_models)],
                SaasId=821,
                IncutType=EIncutType.ModelsWithAdaptiveBanner,
                Vendor=TVendor(
                    DatasourceId=28195 + vendor_id,
                    Name=vendor_name,
                    Id=vendor_id,
                ),
                Constraints=TConstraints(
                    MinDocs=1,
                    MaxDocs=10000,
                ),
                BidInfo=TBidInfo(ClickPrice=500, Bid=1200),
                Header=THeader(
                    Type='default',
                    Text='title',
                ),
                Banner=TMediaElement(
                    Type=EMediaElementType.Banner,
                    Id=821,
                    SourceImage=TImage(
                        Url='https://avatars.mds.yandex.net/get-mpic/1862933/img_id5477847296568171778.png/orig',
                        Width=800,
                        Height=600,
                    ),
                    BidInfo=TBidInfo(
                        ClickPrice=500,
                        Bid=1200,
                    ),
                    Color=TColor(
                        Background="light",
                    ),
                    Subtitle=TColoredText(
                        Text="subtitle text",
                        Color="subtitle color",
                    ),
                    Text=TColoredText(
                        Text="auto banner text",
                        Color="blue",
                    ),
                    Logos=[
                        TMediaElement(
                            Type=EMediaElementType.Logo,
                            Text=TColoredText(
                                Text='logo text',
                                Color='black',
                            ),
                            Id=822,
                            BidInfo=TBidInfo(
                                ClickPrice=500,
                                Bid=1200,
                            ),
                            SourceImage=TImage(
                                Url='https://avatars.mds.yandex.net/get-mpic/1862933/img_id5477847296568171778.png/orig',
                                Width=800,
                                Height=600,
                            ),
                        )
                    ],
                ),
            )
        )
        cls.media_advertising.on_request_media_adv_incut(hid=hid).respond(mock_str)

    def test_basic_carousel_responsive(self):
        """
        Проверяем выдачу врезки MadvBasicCarouselResponsive
        """

        params, rearr_factors = self.get_params_rearr_factors('tool', 557)
        response = self.report.request_json(self.get_request(params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            'inClid': 16,  # == INCLID_MEDIA_ADV_INCUT
                            'incutId': 'basic_carousel_responsive_top',
                            "entity": "searchIncut",
                            "brand_id": 51,
                            "typeId": 10,  # == MadvBasicCarouselResponsive
                            "titleInfo": {
                                "type": "default",
                            },
                            "items": [
                                {
                                    # проверям заполнение полей автобаннера
                                    "entity": "mediaElement",
                                    "type": "banner",
                                    # "color": {
                                    #     "background": "light",  # uncomment this line after fix in media_adv
                                    # },
                                    "text": {
                                        "text": "auto banner text",
                                        "color": "blue",
                                    },
                                    "subtitle": {
                                        "text": "subtitle text",
                                        "color": "subtitle color",
                                    },
                                    "logo": [
                                        {
                                            "entity": "mediaElement",
                                            "type": "logo",
                                            "id": 822,
                                            "title": "logo text",
                                            "urls": {
                                                "click": "",
                                                "pixel": "",
                                            },
                                            "image": {
                                                "url": "https://avatars.mds.yandex.net/get-mpic/1862933/img_id5477847296568171778.png/orig",
                                                "width": 800,
                                                "height": 600,
                                            },
                                        },
                                    ],
                                    "id": 821,
                                    "title": "auto banner text",
                                    "urls": {
                                        "click": "",
                                        "pixel": "",
                                    },
                                    "image": {
                                        'url': 'https://avatars.mds.yandex.net/get-mpic/1862933/img_id5477847296568171778.png/orig',
                                        'width': 800,
                                        'height': 600,
                                    },
                                },
                            ],
                        },
                    ],
                },
            },
            preserve_order=True,
        )

        self.assertEqual(len(response['incuts']['results'][0]['items']), 7)  # 6 моделей и баннер

        self.assertEqual(
            response['incuts']['results'][0]['items'][0]['entity'], 'mediaElement'
        )  # баннер на первом месте

        # проверяем, что у всех моделей в выдаче один оффер
        for item in response['incuts']['results'][0]['items'][1:]:
            self.assertEqual(len(item["offers"]["items"]), 1)

    @classmethod
    def prepare_casual_carousel_responsive(cls):
        vendor_id = 61
        vendor_name = 'vendor_{}'.format(vendor_id)
        cls.index.vendors += [Vendor(vendor_id=vendor_id, name=vendor_name)]

        num_models = 6
        hid = 558
        start_hyperid = 2200
        incut_id = 831
        cls.index.models += [
            Model(
                hid=hid,
                hyperid=start_hyperid + x,
                vendor_id=vendor_id,
                vbid=10,
                title="book {}".format(start_hyperid + x),
            )
            for x in range(num_models)
        ]
        cls.index.offers += [
            Offer(
                hyperid=start_hyperid + x,
                price=1000 * (x + 1),
                cpa=Offer.CPA_REAL,
                hid=hid,
                fee=100,
                bid=100,
                fesh=100,
                title="offer for {}".format(start_hyperid + x),
            )
            for x in range(num_models)
        ]

        mock_str = TestMediaAdv.media_adv_mock(
            TIncut(
                Models=[TModel(Id=start_hyperid + i) for i in range(num_models)],
                Banner=TMediaElement(
                    Type=EMediaElementType.Banner,
                    Text=TColoredText(Text='auto banner text', Color='blue'),
                    Id=incut_id,
                    BidInfo=TBidInfo(ClickPrice=500, Bid=1200),
                    SourceImage=TImage(
                        Url="https://avatars.mds.yandex.net/get-mpic/1862933/img_id5477847296568171778.png/orig",
                        Width=800,
                        Height=600,
                    ),
                    Color=TColor(Background='light'),
                    Subtitle=TColoredText(
                        Text='subtitle text',
                        Color='subtitle color',
                    ),
                    Logos=[
                        TMediaElement(
                            Type=EMediaElementType.Logo,
                            Text=TColoredText(Text='logo text', Color='black'),
                            BidInfo=TBidInfo(ClickPrice=500, Bid=1200),
                            Id=incut_id + 1,
                            SourceImage=TImage(
                                Url="https://avatars.mds.yandex.net/get-mpic/1862933/img_id5477847296568171778.png/orig",
                                Width=800,
                                Height=600,
                            ),
                        ),
                    ],
                ),
                Header=THeader(
                    Type='default',
                    Text='title',
                ),
                SaasId=incut_id,
                IncutType=EIncutType.ModelsWithHorizontalBanner,
                Vendor=TVendor(
                    DatasourceId=28195 + vendor_id,
                    Name=vendor_name,
                    Id=vendor_id,
                ),
                Constraints=TConstraints(
                    MinDocs=1,
                    MaxDocs=10000,
                ),
                BidInfo=TBidInfo(ClickPrice=500, Bid=1200),
            )
        )
        cls.media_advertising.on_request_media_adv_incut(hid=hid).respond(mock_str)

    def test_casual_carousel_responsive(self):
        """
        Проверяем выдачу врезки MadvBasicCarouselResponsive
        """

        params, rearr_factors = self.get_params_rearr_factors('book', 558)
        response = self.report.request_json(self.get_request(params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            'inClid': 16,  # == INCLID_MEDIA_ADV_INCUT
                            'incutId': 'casual_carousel_responsive_top',
                            "entity": "searchIncut",
                            "brand_id": 61,
                            "typeId": 18,  # == MadvBasicCarouselResponsive
                            "titleInfo": {
                                "type": "default",
                            },
                            "items": [
                                {
                                    # проверям заполнение полей автобаннера
                                    "entity": "mediaElement",
                                    "type": "banner",
                                    # "color": {
                                    #     "background": "light",  # uncomment this line after fix in media_adv
                                    # },
                                    "text": {
                                        "text": "auto banner text",
                                        "color": "blue",
                                    },
                                    "subtitle": {
                                        "text": "subtitle text",
                                        "color": "subtitle color",
                                    },
                                    "logo": [
                                        {
                                            "entity": "mediaElement",
                                            "type": "logo",
                                            "id": 832,
                                            "title": "logo text",
                                            "urls": {
                                                "click": "",
                                                "pixel": "",
                                            },
                                            "image": {
                                                "url": "https://avatars.mds.yandex.net/get-mpic/1862933/img_id5477847296568171778.png/orig",
                                                "width": 800,
                                                "height": 600,
                                            },
                                        },
                                    ],
                                    "id": 831,
                                    "title": "auto banner text",
                                    "urls": {
                                        "click": "",
                                        "pixel": "",
                                    },
                                    "image": {
                                        'url': 'https://avatars.mds.yandex.net/get-mpic/1862933/img_id5477847296568171778.png/orig',
                                        'width': 800,
                                        'height': 600,
                                    },
                                },
                            ],
                        },
                    ],
                },
            },
            preserve_order=True,
        )

        self.assertEqual(len(response['incuts']['results'][0]['items']), 7)  # 6 моделей и баннер

        self.assertEqual(
            response['incuts']['results'][0]['items'][0]['entity'], 'mediaElement'
        )  # баннер на первом месте

        # проверяем, что у всех моделей в выдаче один оффер
        for item in response['incuts']['results'][0]['items'][1:]:
            self.assertEqual(len(item["offers"]["items"]), 1)

    @classmethod
    def prepare_casual_carousel_3_items(cls):
        vendor_id = 21
        vendor_name = 'vendor_{}'.format(vendor_id)
        cls.index.vendors += [Vendor(vendor_id=vendor_id, name=vendor_name)]

        num_models = 4
        hid = 556
        start_hyperid = 2000
        cls.index.models += [
            Model(
                hid=hid,
                hyperid=start_hyperid + x,
                vendor_id=vendor_id,
                vbid=10,
                title="symbol {}".format(start_hyperid + x),
            )
            for x in range(num_models)
        ]
        cls.index.offers += [
            Offer(
                hyperid=start_hyperid + x,
                price=1000 * (x + 1),
                cpa=Offer.CPA_REAL,
                hid=hid,
                fee=100,
                bid=100,
                fesh=100,
                title="offer for {}".format(start_hyperid + x),
            )
            for x in range(num_models)
        ]

        mock_str = TestMediaAdv.media_adv_mock(
            TIncut(
                Models=[TModel(Id=start_hyperid + i) for i in range(num_models)],
                SaasId=811,
                IncutType=EIncutType.ModelsList3Items,
                Vendor=TVendor(
                    DatasourceId=28195 + vendor_id,
                    Name=vendor_name,
                    Id=vendor_id,
                ),
                Constraints=TConstraints(
                    MinDocs=1,
                    MaxDocs=10000,
                ),
                BidInfo=TBidInfo(ClickPrice=500, Bid=1200),
                Header=THeader(
                    Type='default',
                    Text='logo text',
                    Logos=[
                        TMediaElement(
                            Type=EMediaElementType.Logo,
                            Text=TColoredText(
                                Text="logo text",
                                Color='black',
                            ),
                            Id=811,
                            BidInfo=TBidInfo(
                                ClickPrice=500,
                                Bid=1200,
                            ),
                            SourceImage=TImage(
                                Url='https://avatars.mds.yandex.net/get-mpic/1862933/img_id5477847296568171778.png/orig',
                                Width=800,
                                Height=600,
                            ),
                        )
                    ],
                ),
            )
        )
        cls.media_advertising.on_request_media_adv_incut(hid=hid).respond(mock_str)

    def test_casual_carousel_3_items(self):
        """
        Проверяем выдачу врезки MadvCasualCarousel3Items
        """

        params, rearr_factors = self.get_params_rearr_factors('symbol', 556)
        response = self.report.request_json(self.get_request(params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            'inClid': 16,  # == INCLID_MEDIA_ADV_INCUT
                            'incutId': 'casual_carousel_3_items_top',
                            "entity": "searchIncut",
                            "brand_id": 21,
                            "typeId": 11,  # == MadvCasualCarousel3Items
                            "titleInfo": {
                                "type": "default",
                                "titleText": "logo text",
                            },
                            "items": ElementCount(4),  # 4 модели
                        },
                    ],
                },
            },
            preserve_order=True,
        )

        # проверяем, что у всех моделей в выдаче один оффер
        for item in response['incuts']['results'][0]['items']:
            self.assertEqual(len(item["offers"]["items"]), 1)

        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            "items": [
                                {
                                    "urls": {
                                        "encrypted": Contains("madv_incut_id=811"),
                                    }
                                }
                            ]
                        },
                    ],
                },
            },
            preserve_order=True,
        )

    @classmethod
    def prepare_recommended_businesses(cls):
        vendor_id = 21
        vendor_name = 'vendor_{}'.format(vendor_id)

        hid = 553
        start_hyperid_1 = 1500
        start_business_id = 2500
        start_shop_id = 3500
        start_feed_id = 4500
        start_offer_id = 5500
        cls.index.models += [
            Model(
                hid=hid,
                hyperid=start_hyperid_1 + x,
                vendor_id=vendor_id,
                vbid=10,
                title="ski {}".format(start_hyperid_1 + x),
            )
            for x in range(0, 10)
        ]
        recommended_shop = Shop(
            fesh=start_shop_id,
            datafeed_id=start_feed_id,
            business_fesh=start_business_id,
            cpa=Shop.CPA_REAL,
            name="Магазин рекомендованного бизнеса",
        )
        not_recommended_shop = Shop(
            fesh=start_shop_id + 1,
            datafeed_id=start_feed_id + 1,
            business_fesh=start_business_id + 1,
            cpa=Shop.CPA_REAL,
            name="Магазин нерекомендованного бизнеса",
        )
        cls.index.shops += [recommended_shop, not_recommended_shop]
        cls.index.offers += [
            Offer(
                hyperid=start_hyperid_1 + x,
                shop=recommended_shop,
                offerid=str(start_offer_id + x),
                price=1000 * x,
                cpa=Offer.CPA_REAL,
                hid=hid,
                title="offer for {}".format(start_hyperid_1 + x),
            )
            for x in range(1, 8)
        ]

        cls.index.offers += [
            Offer(
                hyperid=start_hyperid_1 + x,
                shop=not_recommended_shop,
                offerid=str(start_offer_id + 10 + x),
                price=800 * x,  # дешевле, чем у бизнеса start_business_id
                cpa=Offer.CPA_REAL,
                hid=hid,
                title="offer for {}".format(start_hyperid_1 + x),
            )
            for x in range(1, 10)
        ]

        mock_str = TestMediaAdv.media_adv_mock(
            TIncut(
                Models=[TModel(Id=start_hyperid_1 + i) for i in range(10)],
                SaasId=781,
                IncutType=EIncutType.ModelsList,
                Vendor=TVendor(
                    DatasourceId=28195 + vendor_id,
                    Name='vendor_1',
                    Id=vendor_id,
                ),
                Constraints=TConstraints(
                    BusinessId=[start_business_id],
                    MinDocs=1,
                    MaxDocs=10000,
                ),
                BidInfo=TBidInfo(ClickPrice=0, Bid=1200),
                Header=THeader(
                    Type='default',
                    Text='title',
                    Logos=[
                        TMediaElement(
                            Type=EMediaElementType.Logo,
                            Text=TColoredText(
                                Text="{} logo".format(vendor_name),
                            ),
                            Id=781,
                            BidInfo=TBidInfo(
                                ClickPrice=0,
                                Bid=1200,
                            ),
                            SourceImage=TImage(
                                Url='https://avatars.mds.yandex.net/get-mpic/1862933/img_id5477847296568171778.png/orig',
                                Width=800,
                                Height=600,
                            ),
                        )
                    ],
                ),
            )
        )
        cls.media_advertising.on_request_media_adv_incut(hid=hid).respond(mock_str)

    def test_recommended_businesses(self):
        # проверяем, что во врезку попадают только модели, для которых есть оффер у разрешённого бизнеса
        # проверяем, что к модели прикрепляются именно эти оффера
        params, rearr_factors = self.get_params_rearr_factors('ski', 553)
        response = self.report.request_json(self.get_request(params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            'inClid': 16,  # == INCLID_MEDIA_ADV_INCUT
                            'incutId': 'basic_carousel_plain_top',
                            "entity": "searchIncut",
                            "brand_id": 21,
                            "items": ElementCount(7),  # берём только модели с ДО, которые есть у разрешённых бизнесов
                        },
                    ],
                },
            },
            preserve_order=True,
        )

        model_cpc = Cpc.create_for_model(
            model_id=1501,
            vendor_click_price=0,
            vendor_bid=1200,
            pp=84,
            rec_businesses=[2500],
        )

        # проверяем, что рекмаги сохранились в cpc
        self.assertFragmentIn(
            response, {"incuts": {"results": [{"items": [{"entity": "product", "id": 1501, "cpc": str(model_cpc)}]}]}}
        )

        for item in response['incuts']['results'][0]['items']:
            offer_items = item["offers"]["items"]
            # проверяем, что у всех моделей в выдаче один оффер
            self.assertEqual(len(offer_items), 1)

            # проверяем, что это оффер от разрешённого бизнеса
            self.assertEqual(int(offer_items[0]['shop']['business_id']), 2500)
            self.assertTrue(item['DOFromCertainBusinesses'])

    @staticmethod
    def get_common_params_rearr(request_text, hid, nid=None):
        params, rearr_factors = TestMediaAdv.get_params_rearr_factors(request_text, hid)
        if nid:
            params['nid'] = nid
        return params, rearr_factors

    @classmethod
    def prepare_neighbour_hids(cls):
        """
        данные взяты из реального примера, в котором наигрывалась ошибка
        :return:
        """
        hid = 90597
        nid = 1001
        start_hyperid_1 = 10
        cls.index.vendors += [
            Vendor(vendor_id=1 + x, name='vendor_{}'.format(1 + x), hids=[hid + x]) for x in range(0, 2)
        ]

        cls.index.models += [
            Model(
                hid=hid,
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
                price=1000 * x,
                cpa=Offer.CPA_REAL,
                hid=hid,
                title="offer for {}".format(start_hyperid_1 + x),
            )
            for x in range(1, 10)
        ]

        # модели для второй категории
        hid_2 = hid + 1
        nid_2 = nid + 1
        start_hyperid_2 = start_hyperid_1 + 10
        cls.index.models += [
            Model(
                hid=hid_2,
                hyperid=start_hyperid_2 + x,
                vendor_id=2,
                vbid=10,
                title="sky {}".format(start_hyperid_2 + x),
            )
            for x in range(0, 10)
        ]
        cls.index.offers += [
            Offer(
                hyperid=start_hyperid_2 + x,
                price=1000 * x,
                cpa=Offer.CPA_REAL,
                hid=hid_2,
                title="offer for {}".format(start_hyperid_2 + x),
            )
            for x in range(1, 10)
        ]

        cls.index.navtree += [
            NavCategory(nid=nid, hid=hid, name=str(nid) + str(hid)),
            NavCategory(nid=nid_2, hid=hid_2, name=str(nid) + str(hid) + "_2"),
        ]

        mock_str = TestMediaAdv.media_adv_mock(
            TIncut(
                Models=[TModel(Id=start_hyperid_1 + i) for i in range(10)],
                SaasId=761,
                IncutType=EIncutType.ModelsList,
                Vendor=TVendor(
                    DatasourceId=28195,
                    Name='vendor_1',
                    Id=1,
                ),
                Constraints=TConstraints(
                    MinDocs=1,
                    MaxDocs=10000,
                ),
                BidInfo=TBidInfo(ClickPrice=0, Bid=1200),
                Header=THeader(
                    Type='default',
                    Text='title',
                    Logos=[
                        TMediaElement(
                            Type=EMediaElementType.Logo,
                            Text=TColoredText(
                                Text='vendor_1 logo',
                            ),
                            Id=761,
                            BidInfo=TBidInfo(
                                ClickPrice=0,
                                Bid=1200,
                            ),
                            SourceImage=TImage(
                                Url='https://avatars.mds.yandex.net/get-mpic/1862933/img_id5477847296568171778.png/orig',
                                Width=800,
                                Height=600,
                            ),
                        )
                    ],
                ),
            )
        )
        cls.media_advertising.on_request_media_adv_incut(hid=hid).respond(mock_str)
        # добавление врезки в другую соседнюю категорию
        cls.media_advertising.on_request_media_adv_incut(hid=hid + 1).respond(mock_str)

    def test_neighbour_hids(self):
        """
        показ врезок в категориях-соседях (до правок тест не выполнялся)
        @see https://st.yandex-team.ru/MEDIAADV-51
        """
        hid = 90597
        nid = 1001
        params, rearr = self.get_common_params_rearr("toy", hid, nid=nid)
        request = self.get_request(params, rearr)
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            "items": ElementCount(9),  # берём только модели с ДО
                        },
                    ],
                },
            },
            preserve_order=True,
        )

        # запрос соседней категории (врезка должна быть)
        params, rearr = self.get_common_params_rearr("sky", hid + 1, nid=nid + 1)
        response = self.report.request_json(self.get_request(params, rearr))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            "items": ElementCount(9),  # берём только модели с ДО
                        },
                    ],
                },
            },
            preserve_order=True,
        )

    @classmethod
    def prepare_free_datasources(cls):
        vendor_id = 41
        hid = 555
        start_hyperid_1 = 1700
        vendor_name = 'vendor_{}'.format(vendor_id)
        cls.index.vendors += [Vendor(vendor_id=vendor_id, name=vendor_name)]

        cls.index.models += [
            Model(
                hid=hid,
                hyperid=start_hyperid_1 + x,
                vendor_id=vendor_id,
                vbid=10,
                title="skate {}".format(start_hyperid_1 + x),
            )
            for x in range(0, 10)
        ]
        cls.index.offers += [
            Offer(
                hyperid=start_hyperid_1 + x,
                price=1000 * x,
                cpa=Offer.CPA_REAL,
                hid=hid,
                title="offer for {}".format(start_hyperid_1 + x),
            )
            for x in range(1, 10)
        ]

        mock_str = TestMediaAdv.media_adv_mock(
            TIncut(
                Models=[TModel(Id=start_hyperid_1 + i) for i in range(10)],
                SaasId=801,
                IncutType=EIncutType.ModelsList,
                Vendor=TVendor(
                    DatasourceId=28236,
                    Name=vendor_name,
                    Id=vendor_id,
                ),
                Constraints=TConstraints(
                    MinDocs=6,
                    MaxDocs=15,
                ),
                BidInfo=TBidInfo(ClickPrice=50, Bid=1200),
                Header=THeader(
                    Type='default',
                    Text='title',
                    Logos=[
                        TMediaElement(
                            Type=EMediaElementType.Logo,
                            Text=TColoredText(
                                Text='vendor_1 logo',
                            ),
                            Id=801,
                            BidInfo=TBidInfo(
                                ClickPrice=0,
                                Bid=1200,
                            ),
                            SourceImage=TImage(
                                Url='https://avatars.mds.yandex.net/get-mpic/1862933/img_id5477847296568171778.png/orig',
                                Width=800,
                                Height=600,
                            ),
                        )
                    ],
                ),
            )
        )
        cls.media_advertising.on_request_media_adv_incut(hid=hid).respond(mock_str)

    def test_free_datasources(self):
        # проверяем, что работает реарр на отключение биллинга для определённых или всех датасорсов
        params, rearr_factors = self.get_params_rearr_factors('skate', 555)
        rearr_no_free = rearr_factors.copy()
        rearr_no_free.update({'market_report_madv_all_vendor_datasources_free': False})
        rearr_no_free.update({'market_report_madv_free_vendor_datasources': ''})
        request = self.get_request(params, rearr_no_free)
        response = self.report.request_json(request)
        response_with_paid_clicks_should_be = {
            "incuts": {
                "results": [
                    {
                        "items": [
                            {
                                "entity": "product",
                                "urls": {
                                    "encrypted": Contains("/madv_incut_id=801/", "/vc_bid=1200/", "/vendor_price=50/")
                                },
                            }
                        ]
                    },
                ],
            },
        }

        # проверяем, что без реарр-параметра цена клика такая же, как в ответе врезочника
        self.assertFragmentIn(response, response_with_paid_clicks_should_be, preserve_order=True)

        # проверяем, что если все датасорсы бесплатные, то цена клика нулевая
        rearr_factors_all_free = rearr_factors.copy()
        rearr_factors_all_free.update({'market_report_madv_all_vendor_datasources_free': True})
        request_all_free = self.get_request(params, rearr_factors_all_free)
        response_all_free = self.report.request_json(request_all_free)
        response_with_free_clicks_should_be = {
            "incuts": {
                "results": [
                    {
                        "items": [
                            {
                                "entity": "product",
                                "urls": {
                                    "encrypted": Contains("/madv_incut_id=801/", "/vc_bid=1200/", "/vendor_price=0/")
                                },
                            }
                        ]
                    },
                ],
            },
        }
        self.assertFragmentIn(response_all_free, response_with_free_clicks_should_be, preserve_order=True)

        # проверяем, что если датасорс бесплатный, то цена клика для него нулевая
        rearr_factors_selected_free = rearr_factors.copy()
        rearr_factors_selected_free.update({'market_report_madv_all_vendor_datasources_free': False})
        rearr_factors_selected_free.update({'market_report_madv_free_vendor_datasources': '3664,28236'})
        request_selected_free = self.get_request(params, rearr_factors_selected_free)
        response_selected_free = self.report.request_json(request_selected_free)

        self.assertFragmentIn(response_selected_free, response_with_free_clicks_should_be, preserve_order=True)

        # проверяем, что если датасорс не входит в список бесплатных, то цена клика для него такая же, как в ответе врезочника
        rearr_factors_not_selected_free = rearr_factors.copy()
        rearr_factors_not_selected_free.update({'market_report_madv_all_vendor_datasources_free': False})
        rearr_factors_not_selected_free.update({'market_report_madv_free_vendor_datasources': '3664'})
        request_not_selected_free = self.get_request(params, rearr_factors_not_selected_free)
        response_not_selected_free = self.report.request_json(request_not_selected_free)
        self.assertFragmentIn(response_not_selected_free, response_with_paid_clicks_should_be, preserve_order=True)

    @classmethod
    def prepare_max_limit(cls):
        vendor_id = 31
        vendor_name = 'vendor_{}'.format(vendor_id)

        hid = 564
        start_hyperid_1 = 1800
        cls.index.models += [
            Model(
                hid=hid,
                hyperid=start_hyperid_1 + x,
                vendor_id=vendor_id,
                vbid=10,
                title="stick {}".format(start_hyperid_1 + x),
            )
            for x in range(1, 21)
        ]
        cls.index.offers += [
            Offer(
                hyperid=start_hyperid_1 + x,
                price=1000 * x,
                cpa=Offer.CPA_REAL,
                hid=hid,
                title="offer for {}".format(start_hyperid_1 + x),
            )
            for x in range(1, 21)
        ]

        mock_str = TestMediaAdv.media_adv_mock(
            TIncut(
                Models=[TModel(Id=start_hyperid_1 + i) for i in range(20)],
                SaasId=791,
                IncutType=EIncutType.ModelsList,
                Vendor=TVendor(
                    DatasourceId=28195 + vendor_id,
                    Name='vendor_1',
                    Id=vendor_id,
                ),
                Constraints=TConstraints(
                    MinDocs=6,
                    MaxDocs=15,
                ),
                BidInfo=TBidInfo(ClickPrice=0, Bid=1200),
                Header=THeader(
                    Type='default',
                    Text='title',
                    Logos=[
                        TMediaElement(
                            Type=EMediaElementType.Logo,
                            Text=TColoredText(
                                Text='{} logo'.format(vendor_name),
                            ),
                            Id=791,
                            BidInfo=TBidInfo(
                                ClickPrice=0,
                                Bid=1200,
                            ),
                            SourceImage=TImage(
                                Url='https://avatars.mds.yandex.net/get-mpic/1862933/img_id5477847296568171778.png/orig',
                                Width=800,
                                Height=600,
                            ),
                        )
                    ],
                ),
            )
        )
        cls.media_advertising.on_request_media_adv_incut(hid=hid).respond(mock_str)

    def test_max_limit(self):
        # проверяем, что размер возвращаемой врезки не больше максимально допустимого
        params, rearr_factors = self.get_params_rearr_factors('stick', 564)

        rearr_factors.update(
            {
                'market_media_adv_basic_carousel_plain_min_size_desktop': 6,
                'market_media_adv_basic_carousel_plain_max_size_desktop': 15,
            }
        )
        response = self.report.request_json(self.get_request(params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            'inClid': 16,  # == INCLID_MEDIA_ADV_INCUT
                            'incutId': 'basic_carousel_plain_top',
                            "entity": "searchIncut",
                            "brand_id": 31,
                            "items": ElementCount(15),  # взяли не больше моделей, чем максимальный размер врезки
                        },
                    ],
                },
            },
            preserve_order=True,
        )

    @classmethod
    def prepare_min_filtering(cls):
        vendor_id = 41
        vendor_name = 'vendor_{}'.format(vendor_id)

        hid = 565
        start_hyperid_1 = 1900
        cls.index.models += [
            Model(
                hid=hid,
                hyperid=start_hyperid_1 + x,
                vendor_id=vendor_id,
                vbid=10,
                title="stick {}".format(start_hyperid_1 + x),
            )
            for x in range(1, 10)
        ]
        cls.index.offers += [
            Offer(
                hyperid=start_hyperid_1 + x,
                price=1000 * x,
                cpa=Offer.CPA_REAL,
                hid=hid,
                title="offer for {}".format(start_hyperid_1 + x),
            )
            for x in range(1, 4)  # offers only for 3 models
        ]

        mock_str = TestMediaAdv.media_adv_mock(
            TIncut(
                Models=[TModel(Id=start_hyperid_1 + i) for i in range(10)],
                SaasId=801,
                IncutType=EIncutType.ModelsList,
                Vendor=TVendor(
                    DatasourceId=28195 + vendor_id,
                    Name='vendor_1',
                    Id=vendor_id,
                ),
                Constraints=TConstraints(
                    MinDocs=6,
                    MaxDocs=15,
                ),
                BidInfo=TBidInfo(ClickPrice=0, Bid=1200),
                Header=THeader(
                    Type='default',
                    Text='title',
                    Logos=[
                        TMediaElement(
                            Type=EMediaElementType.Logo,
                            Text=TColoredText(
                                Text='{} logo'.format(vendor_name),
                            ),
                            Id=801,
                            BidInfo=TBidInfo(
                                ClickPrice=0,
                                Bid=1200,
                            ),
                            SourceImage=TImage(
                                Url='https://avatars.mds.yandex.net/get-mpic/1862933/img_id5477847296568171778.png/orig',
                                Width=800,
                                Height=600,
                            ),
                        )
                    ],
                ),
            )
        )
        cls.media_advertising.on_request_media_adv_incut(hid=hid).respond(mock_str)

    def test_min_filtering(self):
        # проверяем, что врезка меньше минимального размера не возвращается
        params, rearr_factors = self.get_params_rearr_factors('stick', 565)

        rearr_factors.update(
            {
                'market_media_adv_basic_carousel_plain_min_size_desktop': 6,
                'market_media_adv_basic_carousel_plain_max_size_desktop': 15,
            }
        )
        response = self.report.request_json(self.get_request(params, rearr_factors))
        self.assertFragmentNotIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            'inClid': 16,  # любая МПФ врезка
                        },
                    ],
                },
            },
            preserve_order=True,
        )

    # проверяем, что топ1 категория передается во врезочник
    def test_top1_category_in_request(self):
        crs = TCommonReportState()
        c = crs.search_state.top_categories.add()
        c.hid = 564
        rs = ReportState.serialize(crs)
        params, rearr_factors = self.get_params_rearr_factors('stick', 565)
        params.update(
            {
                "rs": rs,
                "debug": 1,
            }
        )
        response = self.report.request_json(self.get_request(params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                'debug': {
                    'metasearch': {
                        'subrequests': [
                            {'report': {'logicTrace': [Contains('got top1 category: 564, sending it to vrezochnik')]}}
                        ]
                    }
                }
            },
        )

    @classmethod
    def prepare_incut_type_downgrade(cls):
        vendor_id = 71
        vendor_name = 'vendor_{}'.format(vendor_id)

        hid = 566
        start_hyperid_1 = 2300
        cls.index.models += [
            Model(
                hid=hid,
                hyperid=start_hyperid_1 + x,
                vendor_id=vendor_id,
                vbid=10,
                title="poster {}".format(start_hyperid_1 + x),
            )
            for x in range(1, 11)
        ]
        # offers for only 4 models
        cls.index.offers += [
            Offer(
                hyperid=start_hyperid_1 + x,
                price=1000 * x,
                cpa=Offer.CPA_REAL,
                hid=hid,
                title="offer for {}".format(start_hyperid_1 + x),
            )
            for x in range(1, 5)
        ]

        mock_str = TestMediaAdv.media_adv_mock(
            TIncut(
                Models=[TModel(Id=start_hyperid_1 + i) for i in range(20)],
                SaasId=841,
                IncutType=EIncutType.ModelsList,
                Vendor=TVendor(
                    DatasourceId=28195 + vendor_id,
                    Name='vendor_1',
                    Id=vendor_id,
                ),
                SaasRequestHid=hid,
                Constraints=TConstraints(
                    MinDocs=6,
                    MaxDocs=9,
                ),
                AlternativeIncutTypes=[
                    TIncutTypeInfo(
                        IncutType=EIncutType.ModelsList3Items,
                        MinDocs=3,
                        MaxDocs=9,
                    ),
                ],
                BidInfo=TBidInfo(ClickPrice=0, Bid=1200),
                Header=THeader(
                    Type='default',
                    Text='title',
                    Logos=[
                        TMediaElement(
                            Type=EMediaElementType.Logo,
                            Text=TColoredText(
                                Text='{} logo'.format(vendor_name),
                            ),
                            Id=841,
                            BidInfo=TBidInfo(
                                ClickPrice=0,
                                Bid=1200,
                            ),
                            SourceImage=TImage(
                                Url='https://avatars.mds.yandex.net/get-mpic/1862933/img_id5477847296568171778.png/orig',
                                Width=800,
                                Height=600,
                            ),
                        )
                    ],
                ),
            )
        )
        cls.media_advertising.on_request_media_adv_incut(hid=hid).respond(mock_str)

    def test_incut_type_downgrade(self):
        """
        Проверяем, что если для врезки не хватило офферов,
        но хватило для запасного типа из ответа врезочника,
        то собирается запасного типа
        """
        params, rearr_factors = self.get_params_rearr_factors('poster', 566)
        rearr_factors.update(
            {
                'market_report_downgrade_madv_incut_type_after_filtering': True,
            }
        )
        response = self.report.request_json(self.get_request(params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            'inClid': 16,  # == INCLID_MEDIA_ADV_INCUT
                            'incutId': 'casual_carousel_3_items_top',
                            "entity": "searchIncut",
                            "brand_id": 71,
                            "typeId": 11,  # == MadvCasualCarousel3Items
                            "targetHid": 566,
                            "madvIncutId": 841,
                            "items": ElementCount(4),  # берём только модели с ДО
                        },
                    ],
                },
            },
            preserve_order=True,
        )

    @classmethod
    def prepare_incut_lists_with_candidates(cls):
        vendor_id = 81
        vendor_name = 'vendor_{}'.format(vendor_id)
        vendor_id_2 = 82
        vendor_name_2 = 'vendor_{}'.format(vendor_id_2)

        hid = 586
        start_hyperid_1 = 8300
        start_hyperid_2 = 8320
        cls.index.models += [
            Model(
                hid=hid,
                hyperid=start_hyperid_1 + x,
                vendor_id=vendor_id,
                vbid=10,
                title="shako {}".format(start_hyperid_1 + x),
            )
            for x in range(1, 11)
        ]
        cls.index.models += [
            Model(
                hid=hid,
                hyperid=start_hyperid_2 + x,
                vendor_id=vendor_id_2,
                vbid=10,
                title="shako {}".format(start_hyperid_2 + x),
            )
            for x in range(1, 11)
        ]
        # offers for only 4 models
        cls.index.offers += [
            Offer(
                hyperid=start_hyperid_2 + x,
                price=1000 * x,
                cpa=Offer.CPA_REAL,
                hid=hid,
                title="offer for {}".format(start_hyperid_2 + x),
            )
            for x in range(1, 5)
        ]
        incut_id = 851

        incut_list = TIncutList()
        incut_list.Incuts.extend(
            [
                TIncut(  # 1
                    Models=[TModel(Id=start_hyperid_1 + i) for i in range(20)],
                    SaasId=incut_id,
                    IncutType=EIncutType.ModelsList,
                    Vendor=TVendor(
                        DatasourceId=8195 + vendor_id,
                        Name='vendor_1',
                        Id=vendor_id,
                    ),
                    SaasRequestHid=hid,
                    Constraints=TConstraints(
                        MinDocs=6,
                        MaxDocs=9,
                    ),
                    AlternativeIncutTypes=[
                        TIncutTypeInfo(
                            IncutType=EIncutType.ModelsList3Items,
                            MinDocs=3,
                            MaxDocs=9,
                        ),
                    ],
                    BidInfo=TBidInfo(ClickPrice=0, Bid=1200),
                    Header=THeader(
                        Type='default',
                        Text='title',
                        Logos=[
                            TMediaElement(
                                Type=EMediaElementType.Logo,
                                Text=TColoredText(
                                    Text="{} logo".format(vendor_name),
                                ),
                                Id=incut_id,
                                BidInfo=TBidInfo(
                                    ClickPrice=0,
                                    Bid=1200,
                                ),
                                SourceImage=TImage(
                                    Url='https://avatars.mds.yandex.net/get-mpic/1862933/img_id5477847296568171778.png/orig',
                                    Width=800,
                                    Height=600,
                                ),
                            )
                        ],
                    ),
                ),
                TIncut(  # 2
                    Models=[TModel(Id=start_hyperid_2 + i) for i in range(20)],
                    SaasId=incut_id + 1,
                    IncutType=EIncutType.ModelsList,
                    Vendor=TVendor(
                        DatasourceId=8195 + vendor_id_2,
                        Name='vendor_2',
                        Id=vendor_id_2,
                    ),
                    SaasRequestHid=hid,
                    Constraints=TConstraints(
                        MinDocs=6,
                        MaxDocs=9,
                    ),
                    AlternativeIncutTypes=[
                        TIncutTypeInfo(
                            IncutType=EIncutType.ModelsList3Items,
                            MinDocs=3,
                            MaxDocs=9,
                        ),
                    ],
                    BidInfo=TBidInfo(ClickPrice=0, Bid=1100),
                    Header=THeader(
                        Type='default',
                        Text='title',
                        Logos=[
                            TMediaElement(
                                Type=EMediaElementType.Logo,
                                Text=TColoredText(
                                    Text="{} logo".format(vendor_name_2),
                                ),
                                Id=incut_id + 1,
                                BidInfo=TBidInfo(
                                    ClickPrice=0,
                                    Bid=1100,
                                ),
                                SourceImage=TImage(
                                    Url='https://avatars.mds.yandex.net/get-mpic/1862933/img_id5477847296568171778.png/orig',
                                    Width=800,
                                    Height=600,
                                ),
                            )
                        ],
                    ),
                ),
            ]
        )

        mock_str = TestMediaAdv.media_adv_mock(incut_list)
        cls.media_advertising.on_request_media_adv_incut(hid=hid).respond(mock_str)

    def test_incut_lists_with_candidates(self):
        """
        Проверяем, что если для врезки не хватило офферов,
        не хватило для запасных типов из ответа врезочника,
        то собирается следующая из списка врезок
        """
        params, rearr_factors = self.get_params_rearr_factors('shako', 586)
        rearr_factors.update(
            {
                'market_report_downgrade_madv_incut_type_after_filtering': True,
            }
        )
        response = self.report.request_json(self.get_request(params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            'inClid': 16,  # == INCLID_MEDIA_ADV_INCUT
                            'incutId': 'casual_carousel_3_items_top',
                            "entity": "searchIncut",
                            "brand_id": 82,
                            "typeId": 11,  # == MadvCasualCarousel3Items
                            "targetHid": 586,
                            "madvIncutId": 852,
                            "items": ElementCount(4),  # берём только модели с ДО
                        },
                    ],
                },
            },
            preserve_order=True,
        )

    @classmethod
    def prepare_incut_lists_with_candidates_subrequests(cls):
        vendor_id = 91
        vendor_name = 'vendor_{}'.format(vendor_id)

        hid = 587
        start_hyperid_1 = 8400
        cls.index.models += [
            Model(
                hid=hid,
                hyperid=start_hyperid_1 + x,
                vendor_id=vendor_id,
                vbid=10,
                title="annihilus {}".format(start_hyperid_1 + x),
            )
            for x in range(30)
        ]
        cls.index.offers += [
            Offer(
                hyperid=start_hyperid_1 + x,
                price=1000 * x + 1,
                cpa=Offer.CPA_REAL,
                hid=hid,
                title="offer for {}".format(start_hyperid_1 + x),
            )
            for x in (0, 1, 10, 11, 20, 21)
        ]

        incut_id = 861
        logo = TMediaElement(
            Type=EMediaElementType.Logo,
            Id=incut_id,
            Text=TColoredText(
                Text="{} logo".format(vendor_name),
            ),
            SourceImage=TImage(
                Url='https://avatars.mds.yandex.net/get-mpic/1862933/img_id5477847296568171778.png/orig',
                Width=800,
                Height=600,
            ),
            PixelUrl="logo_pixel_url",
            ClickUrl="logo_click_url",
            BidInfo=TBidInfo(ClickPrice=0, Bid=1200),
        )

        incut_base = TIncut(
            SaasId=incut_id,
            IncutType=EIncutType.ModelsList,
            Vendor=TVendor(
                DatasourceId=28195 + vendor_id,
                Name='vendor_1',
                Id=vendor_id,
            ),
            SaasRequestHid=hid,
            Constraints=TConstraints(
                MinDocs=6,
                MaxDocs=9,
            ),
            BidInfo=TBidInfo(ClickPrice=0, Bid=1200),
            Header=THeader(Type='default', Text='title', Logos=[logo]),
        )
        incut1 = TIncut()
        incut1.CopyFrom(incut_base)
        incut1.Models.extend([TModel(Id=start_hyperid_1 + i) for i in range(10)])
        incut2 = TIncut()
        incut2.CopyFrom(incut_base)
        incut2.SaasId = incut_id + 1
        incut2.Models.extend([TModel(Id=start_hyperid_1 + i) for i in range(10, 20)])
        incut3 = TIncut()
        incut3.CopyFrom(incut_base)
        incut3.SaasId = incut_id + 2
        incut3.Models.extend([TModel(Id=start_hyperid_1 + i) for i in range(20, 30)])
        incut4 = TIncut()
        incut4.CopyFrom(incut_base)
        incut4.SaasId = incut_id + 3
        incut4.Models.extend([TModel(Id=start_hyperid_1 + i) for i in range(30)])

        mock_str = TestMediaAdv.media_adv_mock(TIncutList(Incuts=[incut1, incut2, incut3, incut4]))
        cls.media_advertising.on_request_media_adv_incut(hid=hid).respond(mock_str)

    def test_incut_lists_with_candidates_subrequests(self):
        """
        Проверяем, при запросе за документами для врезки-кандидата учитываются модели,
        которые могли быть получены в предыдущих запросах
        """
        params, rearr_factors = self.get_params_rearr_factors('annihilus', 587)
        rearr_factors.update(
            {
                'market_report_downgrade_madv_incut_type_after_filtering': True,
            }
        )
        response = self.report.request_json(self.get_request(params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            'inClid': 16,  # == INCLID_MEDIA_ADV_INCUT
                            'incutId': 'basic_carousel_plain_top',
                            "entity": "searchIncut",
                            "brand_id": 91,
                            "targetHid": 587,
                            "madvIncutId": 864,
                            "items": ElementCount(6),
                        },
                    ],
                },
            },
            preserve_order=True,
        )

    @classmethod
    def prepare_incut_lists_with_candidates_req_businesses(cls):
        vendor_id = 101
        vendor_name = 'vendor_{}'.format(vendor_id)

        hid = 588
        start_hyperid_1 = 8500
        start_business_id = 313
        start_feed_id = 323
        start_shop_id = 333
        recommended_shop = Shop(
            fesh=start_shop_id,
            datafeed_id=start_feed_id,
            business_fesh=start_business_id,
            cpa=Shop.CPA_REAL,
            name="Магазин рекомендованного бизнеса",
        )
        not_recommended_shop = Shop(
            fesh=start_shop_id + 1,
            datafeed_id=start_feed_id + 1,
            business_fesh=start_business_id + 1,
            cpa=Shop.CPA_REAL,
            name="Магазин нерекомендованного бизнеса",
        )
        cls.index.shops += [recommended_shop, not_recommended_shop]

        cls.index.models += [
            Model(
                hid=hid,
                hyperid=start_hyperid_1 + x,
                vendor_id=vendor_id,
                vbid=10,
                title="rakanishu {}".format(start_hyperid_1 + x),
            )
            for x in range(6)
        ]
        cls.index.offers += [
            Offer(
                hyperid=start_hyperid_1 + x,
                price=1000 * x + 1,
                shop=recommended_shop,
                cpa=Offer.CPA_REAL,
                hid=hid,
                title="offer for {}".format(start_hyperid_1 + x),
            )
            for x in range(3)
        ]
        cls.index.offers += [
            Offer(
                hyperid=start_hyperid_1 + x,
                price=1000 * x + 1,
                shop=not_recommended_shop,
                cpa=Offer.CPA_REAL,
                hid=hid,
                title="offer for {}".format(start_hyperid_1 + x),
            )
            for x in range(6)
        ]

        incut_id = 871

        incut_base = TIncut(
            SaasId=incut_id,
            Models=[TModel(Id=start_hyperid_1 + i) for i in range(6)],
            IncutType=EIncutType.ModelsList,
            Vendor=TVendor(
                DatasourceId=28195 + vendor_id,
                Name='vendor_1',
                Id=vendor_id,
            ),
            SaasRequestHid=hid,
            Constraints=TConstraints(
                MinDocs=6,
                MaxDocs=9,
            ),
            BidInfo=TBidInfo(ClickPrice=0, Bid=1200),
            Header=THeader(
                Type='default',
                Text='title',
                Logos=[
                    TMediaElement(
                        Type=EMediaElementType.Logo,
                        Id=incut_id,
                        Text=TColoredText(
                            Text="{} logo".format(vendor_name),
                        ),
                        SourceImage=TImage(
                            Url='https://avatars.mds.yandex.net/get-mpic/1862933/img_id5477847296568171778.png/orig',
                            Width=800,
                            Height=600,
                        ),
                        PixelUrl="logo_pixel_url",
                        ClickUrl="logo_click_url",
                        BidInfo=TBidInfo(ClickPrice=0, Bid=1200),
                    )
                ],
            ),
        )

        incut1 = TIncut()
        incut1.CopyFrom(incut_base)
        incut1.Constraints.BusinessId.extend([start_business_id])
        incut2 = TIncut()
        incut2.CopyFrom(incut_base)
        incut2.SaasId = incut_id + 1

        mock_str = TestMediaAdv.media_adv_mock(TIncutList(Incuts=[incut1, incut2]))
        cls.media_advertising.on_request_media_adv_incut(hid=hid).respond(mock_str)

    def test_incut_lists_with_candidates_req_businesses(self):
        """
        Проверяем, при запросе за документами для врезки-кандидата
        учитываются рекомендованные магазины, и ДО запрашиваются
        для каждого списка магазинов отдельно
        """
        params, rearr_factors = self.get_params_rearr_factors('rakanishu', 588)
        rearr_factors.update(
            {
                'market_report_downgrade_madv_incut_type_after_filtering': True,
            }
        )
        response = self.report.request_json(self.get_request(params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            'inClid': 16,  # == INCLID_MEDIA_ADV_INCUT
                            'incutId': 'basic_carousel_plain_top',
                            "entity": "searchIncut",
                            "brand_id": 101,
                            "targetHid": 588,
                            "madvIncutId": 872,
                            "items": ElementCount(6),
                        },
                    ],
                },
            },
            preserve_order=True,
        )

    @classmethod
    def prepare_incut_promos(cls):
        hid = 589
        start_hyperid_1 = 5890
        cls.index.models += [
            Model(
                hid=hid,
                hyperid=start_hyperid_1 + x,
                vendor_id=1,
                vbid=10,
                datasource_id=444,
                title="bishibosh {}".format(start_hyperid_1 + x),
                glparams=[GLParam(param_id=7893318, value=1)],
            )
            for x in range(1, 10)
        ]
        cls.settings.loyalty_enabled = True
        cls.dynamic.loyalty += [
            DynamicBlueGenericBundlesPromos(whitelist=['promo1034701_0{}_key000'.format(x) for x in range(1, 10)])
        ]
        cls.index.gltypes += [
            GLType(param_id=Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID, vendor=True, hid=hid),
        ]

        promos = [
            Promo(
                promo_type=PromoType.GENERIC_BUNDLE,
                key='promo1034701_0{}_key000'.format(x),
                generic_bundles_content=[
                    make_generic_bundle_content("offerid_xxxx_{}".format(x), "offerid79", 1),
                ],
                feed_id=777,
                url="url_promo1034701_0{}_key000".format(x),
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
                waremd5='RcSMzi4xz{}yxqGvxRx8atA'.format(x),
                fesh=777,
                feedid=777,
                offerid="offerid_xxxx_{}".format(x),
                title="offer for {}".format(start_hyperid_1 + x),
                promo=[promos[x]],
                glparams=[GLParam(param_id=7893318, value=1)],
            )
            for x in range(1, 10)
        ]
        cls.index.offers += [
            Offer(
                hyperid=start_hyperid_1 + 79,
                price=1000,
                cpa=Offer.CPA_REAL,
                hid=hid,
                fee=100,
                bid=100,
                waremd5='RcSMzi4x{}yxqGvxRx8atA'.format(79),
                fesh=777,
                feedid=777,
                offerid="offerid79",
                title="offer for {}".format(79),
                glparams=[GLParam(param_id=7893318, value=1)],
            )
        ]
        cls.index.promos += promos

        mock_str = TestMediaAdv.media_adv_mock(
            TIncut(
                Models=[TModel(Id=start_hyperid_1 + i) for i in range(10)],
                SaasId=hid + 1000,
                IncutType=EIncutType.ModelsList,
                Vendor=TVendor(
                    DatasourceId=28195,
                    Name='vendor_1',
                    Id=1,
                ),
                SaasRequestHid=hid,
                Constraints=TConstraints(
                    MinDocs=1,
                    MaxDocs=10000,
                ),
                AlternativeIncutTypes=[
                    TIncutTypeInfo(
                        IncutType=EIncutType.ModelsList3Items,
                        MinDocs=3,
                        MaxDocs=9,
                    ),
                ],
                BidInfo=TBidInfo(ClickPrice=500, Bid=1200),
                Header=THeader(
                    Type='default',
                    Text='title',
                    Logos=[
                        TMediaElement(
                            Type=EMediaElementType.Logo,
                            Text=TColoredText(
                                Text='media_element text',
                                Color='black',
                            ),
                            Id=761,
                            BidInfo=TBidInfo(
                                ClickPrice=500,
                                Bid=1200,
                            ),
                            SourceImage=TImage(
                                Url='https://avatars.mds.yandex.net/get-mpic/1862933/img_id5477847296568171778.png/orig',
                                Width=800,
                                Height=600,
                            ),
                        )
                    ],
                ),
            )
        )
        cls.media_advertising.on_request_media_adv_incut(hid=hid).respond(mock_str)

    def test_incut_promos(self):
        """
        Проверяем наличие подарковых промок в офферах во врезке
        """
        params, rearr_factors = self.get_params_rearr_factors('bishibosh', 589)
        response = self.report.request_json(self.get_request(params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            'inClid': 16,  # == INCLID_MEDIA_ADV_INCUT
                            'incutId': 'basic_carousel_plain_top',
                            "entity": "searchIncut",
                            "typeId": 8,  # == MadvBasicCarouselPlain
                            "targetHid": 589,
                            "madvIncutId": 1589,
                            "items": [
                                {
                                    "entity": "product",
                                    "offers": {
                                        "items": [
                                            {
                                                "entity": "offer",
                                                "promos": [
                                                    {
                                                        "type": "generic-bundle",
                                                    },
                                                ],
                                            },
                                        ],
                                    },
                                },
                            ],
                        },
                    ],
                },
            },
        )

    def test_madv_incut_click_context(self):
        """
        Проверяем наличие правильно заполненного поля cc в моделях, офферах и лого
        """
        params, rearr_factors = self.get_params_rearr_factors('bishibosh', 589)
        response = self.report.request_json(self.get_request(params, rearr_factors))
        click_context = str(ClickContext(pp=84, inclid=16, incut_id=1589))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            'inClid': 16,  # == INCLID_MEDIA_ADV_INCUT
                            'incutId': 'basic_carousel_plain_top',
                            "entity": "searchIncut",
                            "typeId": 8,  # == MadvBasicCarouselPlain
                            "targetHid": 589,
                            "madvIncutId": 1589,
                            "titleInfo": {
                                "logo": [
                                    {
                                        "type": "logo",
                                        "cc": click_context,
                                    },
                                ],
                            },
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
                        },
                    ],
                },
            },
        )

    def test_madv_incut_banner_click_flag_off(self):
        """
        Проверяем наличие врезки при отключенном флаге
        """
        params, rearr_factors = self.get_params_rearr_factors('bishibosh', 589)
        params['glfilter'] = str(Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID) + ':1'
        params['cc'] = str(ClickContext(pp=84, inclid=16, incut_id=1589))
        rearr_factors['market_report_click_context_enabled'] = '1'
        rearr_factors['market_report_disable_vendor_incuts_after_banner_click'] = '0'
        response = self.report.request_json(self.get_request(params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": NotEmptyList(),
                },
                "incuts": {
                    "results": [
                        {
                            'inClid': 16,  # == INCLID_MEDIA_ADV_INCUT
                        },
                    ],
                },
            },
        )

    def test_madv_incut_banner_click_flag_on(self):
        """
        Проверяем отстутствие врезки с флагом и зажатым фильтром по вендору
        при переданном cc с инклидом media_adv
        """
        params, rearr_factors = self.get_params_rearr_factors('bishibosh', 589)
        params['glfilter'] = str(Const.DEFAULT_VENDOR_GL_FILTER_PARAM_ID) + ':1'
        params['cc'] = str(ClickContext(pp=84, inclid=16, incut_id=1589))
        rearr_factors['market_report_click_context_enabled'] = '1'
        rearr_factors['market_report_disable_vendor_incuts_after_banner_click'] = '1'
        response = self.report.request_json(self.get_request(params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": NotEmptyList(),
                },
                "incuts": {
                    "results": EmptyList(),
                },
            },
        )

    @classmethod
    def prepare_adult_filtering(cls):
        vendor_id = 111
        vendor_name = 'vendor_{}'.format(vendor_id)

        hid = 16155466
        parent_hid = 18540670
        start_hyperid = 2400
        incut_id = 881
        cls.index.hypertree += [HyperCategory(hid=parent_hid, children=[HyperCategory(hid=hid)])]
        cls.index.models += [
            Model(
                hid=hid,
                hyperid=start_hyperid + x,
                vendor_id=vendor_id,
                vbid=10,
                title="penetrylda {}".format(start_hyperid + x),
            )
            for x in range(1, 7)
        ]
        cls.index.offers += [
            Offer(
                hyperid=start_hyperid + x,
                price=1000 * x,
                cpa=Offer.CPA_REAL,
                hid=hid,
                adult=True,
                title="offer for {}".format(start_hyperid + x),
            )
            for x in range(1, 7)
        ]

        mock_str = TestMediaAdv.media_adv_mock(
            TIncut(
                Models=[TModel(Id=start_hyperid + i) for i in range(1, 7)],
                Vendor=TVendor(
                    Id=vendor_id,
                    DatasourceId=(28195 + vendor_id),
                    Name=vendor_name,
                ),
                SaasUrl='saas_url',
                SaasId=incut_id,
                Constraints=TConstraints(
                    MinDocs=6,
                    MaxDocs=15,
                ),
                BidInfo=TBidInfo(ClickPrice=500, Bid=1200),
                IncutType=EIncutType.ModelsList,
                Header=THeader(
                    Type='default',
                    Text='title',
                    Logos=[
                        TMediaElement(
                            Type=EMediaElementType.Logo,
                            Id=incut_id,
                            Text=TColoredText(
                                Text="{} logo".format(vendor_name),
                            ),
                            SourceImage=TImage(Url="picture_url", Width=800, Height=600),
                            PixelUrl="logo_pixel_url",
                            ClickUrl="logo_click_url",
                            BidInfo=TBidInfo(ClickPrice=502, Bid=1202),
                        ),
                    ],
                ),
            )
        )
        cls.media_advertising.on_request_media_adv_incut(hid=hid).respond(mock_str)
        cls.media_advertising.on_request_media_adv_incut(hid=parent_hid).respond(mock_str)

    # проверяем фильтрацию врезок с товарами для взрослых на невзрослых категориях
    def test_adult_filtering(self):
        params, rearr_factors = self.get_params_rearr_factors('penetrylda', 16155466)

        # проверим, что врезка со взрослыми товарами не отдаётся даже с выключенной фильтрацией, если adult=1 не проставлен в cgi
        rearrs_no_filters = rearr_factors.copy()
        rearrs_no_filters.update(
            {
                'market_force_filter_adult_for_incuts': 0,
                'market_adult_incuts_on_adult_hids_only': 0,
            }
        )
        response = self.report.request_json(self.get_request(params, rearrs_no_filters))
        empty_response = {
            "incuts": {"results": EmptyList()},
        }
        self.assertFragmentIn(
            response,
            empty_response,
            preserve_order=True,
        )

        # включаем adult=1 в cgi
        params.update(
            {
                'adult': 1,
            }
        )
        # проверим, что врезка всё равно не отдаётся, если установлен флаг фильтрации взрослой рекламы
        rearrs_force_filter = rearr_factors.copy()
        rearrs_force_filter.update(
            {
                'market_force_filter_adult_for_incuts': 1,
            }
        )
        request = self.get_request(params, rearrs_force_filter)
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            empty_response,
            preserve_order=True,
        )

        # проверим, что врезка отдаётся, если не фильтруется флагом, и запрашивается взрослая категория
        rearrs_on_adult_hids = rearr_factors.copy()
        rearrs_on_adult_hids.update(
            {
                'market_force_filter_adult_for_incuts': 0,
                'market_adult_incuts_on_adult_hids_only': 1,
            }
        )
        request = self.get_request(params, rearrs_on_adult_hids)
        response = self.report.request_json(self.get_request(params, rearrs_on_adult_hids))
        response_with_incut = {
            "incuts": {
                "results": [
                    {
                        'inClid': 16,  # == INCLID_MEDIA_ADV_INCUT
                        'incutId': 'basic_carousel_plain_top',
                        "entity": "searchIncut",
                        "brand_id": 111,
                        "typeId": 8,
                        "madvIncutId": 881,
                        "items": ElementCount(6),
                    },
                ],
            },
        }
        self.assertFragmentIn(
            response,
            response_with_incut,
            preserve_order=True,
        )

        # сделаем запрос на родительской категории
        params, rearr_factors = self.get_params_rearr_factors('penetrylda', 18540670)
        params.update(
            {
                'adult': 1,
            }
        )
        rearrs_on_adult_hids = rearr_factors.copy()
        rearrs_on_adult_hids.update(
            {
                'market_force_filter_adult_for_incuts': 0,
                'market_adult_incuts_on_adult_hids_only': 1,
            }
        )
        response = self.report.request_json(self.get_request(params, rearrs_on_adult_hids))
        self.assertFragmentIn(
            response,
            empty_response,
            preserve_order=True,
        )

        response = self.report.request_json(self.get_request(params, rearrs_no_filters))
        self.assertFragmentIn(
            response,
            response_with_incut,
            preserve_order=True,
        )


if __name__ == '__main__':
    main()
