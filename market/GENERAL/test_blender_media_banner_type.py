#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.types import (
    Model,
)
from core.testcase import main
from market.report.proto.ReportState_pb2 import TCommonReportState  # noqa pylint: disable=import-error
from test_media_adv import TestMediaAdv
from core.blender_bundles import get_supported_incuts_cgi

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
    TMediaElement,
    TVendor,
)


class T(TestMediaAdv):
    @classmethod
    def prepare_media_banner(cls):
        """
        Тест 1. Подготовка медийного баннера
        """
        hid = 9090
        # Помещаем в индекс модели для выдачи в органике
        cls.index.models += [
            Model(
                hid=hid,
                hyperid=312312,
                vendor_id=1,
                vbid=10,
                datasource_id=444,
                title="toy 312312",
            )
        ]
        mock_str = TestMediaAdv.media_adv_mock(
            TIncut(
                Models=None,
                SaasId=4044,
                IncutType=EIncutType.MediaBannerAdaptive,
                BidInfo=TBidInfo(ClickPrice=200, Bid=400),
                Banner=TMediaElement(
                    Type=EMediaElementType.BannerAdaptive,
                    Text=TColoredText(Text='adaptive_banner', Color='red'),
                    Id=6777,
                    BidInfo=TBidInfo(ClickPrice=17, Bid=600),
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
                ),
                Constraints=TConstraints(
                    MinDocs=0,
                    MaxDocs=10000,
                ),
                Header=THeader(
                    Type='default',
                    Text='title',
                ),
                Vendor=TVendor(
                    DatasourceId=28195,
                    Name='vendor_1',
                    Id=1,
                ),
            )
        )
        cls.media_advertising.on_request_media_adv_incut(hid=hid).respond(mock_str)

    def test_media_banner_adaptive(self):
        """
        Тест 1. Проверям выдачу медийного баннера - MediaBannerAdaptive
        """
        params = {
            'place': 'blender',
            'text': "toy",
            'use-default-offers': 1,
            'allow-collapsing': 1,
            'pp': 18,
            'show-urls': 'productVendorBid,cpa',
            'hid': 9090,
            'client': 'frontend',
            'platform': 'desktop',
            'supported-incuts': get_supported_incuts_cgi({"1": ["23", "24"], "2": ["23", "24"]}),
        }
        rearr_factors = {
            'market_blender_media_adv_incut_enabled': 1,  # включение  МПФ врезки
        }
        response = self.report.request_json(self.get_request(params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            'inClid': 16,  # == INCLID_MEDIA_ADV_INCUT
                            'incutId': 'media_banner_adaptive_top',
                            "entity": "searchIncut",
                            "brand_id": 1,
                            "typeId": 23,  # == MadvMediaBannerAdaptive
                            "madvIncutId": 4044,
                            "items": [
                                {
                                    'entity': 'mediaElement',
                                    'type': 'banner-adaptive',
                                    'id': 6777,
                                    "title": "adaptive_banner",
                                    "image": {
                                        "url": "https://avatars.mds.yandex.net/get-mpic/1862933/img_id5477847296568171778.png/orig",
                                        "width": 800,
                                        "height": 600,
                                    },
                                }
                            ],
                        },
                    ],
                },
            },
        )

    @classmethod
    def prepare_banner_fixed(cls):
        """
        Тест 2. Подготовка медийного фиксированного баннера
        """
        hid = 12
        # Помещаем в индекс модели для выдачи в органике
        cls.index.models += [
            Model(
                hid=hid,
                hyperid=15,
                vendor_id=1,
                vbid=10,
                datasource_id=444,
                title="toy 15",
            )
        ]
        mock_str = TestMediaAdv.media_adv_mock(
            TIncut(
                Models=None,
                SaasId=16,
                IncutType=EIncutType.MediaBannerFixed,
                BidInfo=TBidInfo(ClickPrice=200, Bid=400),
                Banner=TMediaElement(
                    Type=EMediaElementType.BannerFixed,
                    Text=TColoredText(Text='fixed_banner', Color='green'),
                    Id=505,
                    BidInfo=TBidInfo(ClickPrice=17, Bid=100),
                    SourceImage=TImage(
                        Url="https://avatars.mds.yandex.net/get-mpic/1862933/img_id5477847296568171778.png/orig",
                        Width=400,
                        Height=300,
                    ),
                    Color=TColor(Background='light'),
                    Subtitle=TColoredText(
                        Text='subtitle text',
                        Color='subtitle color',
                    ),
                ),
                Constraints=TConstraints(
                    MinDocs=0,
                    MaxDocs=10000,
                ),
                Header=THeader(
                    Type='default',
                    Text='title',
                ),
                Vendor=TVendor(
                    DatasourceId=234567,
                    Name='vendor_2',
                    Id=1,
                ),
            )
        )
        cls.media_advertising.on_request_media_adv_incut(hid=hid).respond(mock_str)

    def test_banner_fixed(self):
        """
        Тест 2. Проверям выдачу медийного баннера - MediaBannerFixed
        """
        params = {
            'place': 'blender',
            'text': "toy",
            'use-default-offers': 1,
            'allow-collapsing': 1,
            'pp': 18,
            'show-urls': 'productVendorBid,cpa',
            'hid': 12,
            'client': 'frontend',
            'platform': 'desktop',
            'supported-incuts': get_supported_incuts_cgi({"1": ["23", "24"], "2": ["23", "24"]}),
        }
        rearr_factors = {
            'market_blender_media_adv_incut_enabled': 1,  # включение МПФ врезки
        }
        response = self.report.request_json(self.get_request(params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            'inClid': 16,  # == INCLID_MEDIA_ADV_INCUT
                            'incutId': 'media_banner_fixed_top',
                            "entity": "searchIncut",
                            "brand_id": 1,
                            "typeId": 24,  # == MadvMediaBannerFixed
                            "madvIncutId": 16,
                            "items": [
                                {
                                    'entity': 'mediaElement',
                                    'type': 'banner-fixed',
                                    'id': 505,
                                    "title": "fixed_banner",
                                    "image": {
                                        "url": "https://avatars.mds.yandex.net/get-mpic/1862933/img_id5477847296568171778.png/orig",
                                        "width": 400,
                                        "height": 300,
                                    },
                                }
                            ],
                        },
                    ],
                },
            },
        )


if __name__ == '__main__':
    main()
