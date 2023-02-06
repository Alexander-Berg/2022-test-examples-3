#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.types import (
    Model,
    Offer,
    Vendor,
)
from core.matcher import (
    Contains,
    ElementCount,
)
from core.testcase import main
from test_madv_model_card import TMadvModelCardTest
from test_media_adv import TestMediaAdv

from market.media_adv.proto.output.output_incuts_pb2 import (
    TMediaElement,
    EMediaElementType,
    TColoredText,
    TBidInfo,
    TImage,
    THeader,
    TIncut,
    EIncutType,
    TVendor,
    TConstraints,
    TModel,
    TIncutTypeInfo,
)


class T(TMadvModelCardTest):
    @classmethod
    def __create_request(cls, hyperid):
        params = {
            'place': 'madv_incut',
            'supported-incuts': '{"101":[20,8]}',  # 20 - MadvCardBasic; 8 - MadvBasicCarouselPlain
            'hyperid': hyperid,
            'show-urls': 'productVendorBid',
        }
        rearr_flags = {
            'market_madv_saas_request_with_target_page': 1,  # TODO remove after https://st.yandex-team.ru/MEDIAADV-154
            'market_madv_saas_request_with_vendor_id': 1,
            'market_blender_media_adv_incut_enabled': 1,
        }
        return cls.get_request(params, rearr_flags)

    @classmethod
    def prepare_basic_incut(cls):
        hid = 1
        vendor_id = 1

        cls.index.models += [
            Model(
                hyperid=x,
                hid=hid,
                vbid=50,
                vendor_id=vendor_id,
                title='Моделька {}'.format(x),
                datasource_id=111,
            )
            for x in range(1, 10)
        ]
        cls.index.offers += [
            Offer(
                hyperid=x,
                fesh=100,
                fee=100,
                bid=100,
                price=1000,
                cpa=Offer.CPA_REAL,
                hid=hid,
                waremd5='RcSMzi4xy{}yxqGvxRx8atA'.format(x),
                title='Оффер для модельки {}'.format(x),
            )
            for x in range(1, 10)
        ]
        cls.index.vendors += [
            Vendor(
                vendor_id=vendor_id,
                name='vendor_{}'.format(vendor_id),
            )
        ]

        bid_info = TBidInfo(
            Bid=1200,
            ClickPrice=500,
        )

        media_element = TMediaElement(
            Type=EMediaElementType.Logo,
            Text=TColoredText(Text="media_element text", Color="black"),
            Id=761,
            BidInfo=bid_info,
            PixelUrl="",
            ClickUrl="",
            SourceImage=TImage(
                Url='https://avatars.mds.yandex.net/get-mpic/1862933/img_id5477847296568171778.png/orig',
                Width=800,
                Height=600,
            ),
        )

        header = THeader(
            Type='default',
            Text='Идеи для покупок',  # заголовок по умолчанию
            Logos=[
                media_element,
            ],
        )

        vendor = TVendor(
            Id=1,
            Name='vendor_1',
            DatasourceId=28195,
        )

        incut = TIncut(
            SaasId=111,
            IncutType=EIncutType.ModelsList,
            Vendor=vendor,
            Constraints=TConstraints(
                MinDocs=1,
                MaxDocs=10000,
            ),
            SaasRequestHid=2,
            BidInfo=bid_info,
            AlternativeIncutTypes=[
                TIncutTypeInfo(
                    IncutType=EIncutType.ModelCardBasic,
                    MinDocs=3,
                    MaxDocs=9,
                ),
            ],
            Models=[TModel(Id=x) for x in range(1, 10)],
            Header=header,
        )

        mock_str = TestMediaAdv.media_adv_mock(incut)

        # TODO add page and vendor
        cls.media_advertising.on_request_media_adv_incut(hid=hid).respond(mock_str)

    def test_supported_incuts(self):
        request = self.__create_request(5)
        response = self.report.request_json(request + "&debug=da")
        self.assertFragmentIn(
            response,
            {
                "logicTrace": [
                    Contains(
                        "&incuts=ml,mcb",
                    )
                ]
            },
        )

    def test_basic_incut(self):
        """
        получение врезки с базовой каруселью
        """
        request = self.__create_request(5)
        response = self.report.request_json(request)

        self.assertFragmentIn(
            response,
            {
                "results": ElementCount(1),
            },
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        'incutId': 'MadvCardCarousel',
                        'typeId': 8,
                        "items": ElementCount(9),
                    }
                ]
            },
        )

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        'incutId': 'MadvCardCarousel',
                        "items": [
                            {
                                "urls": {
                                    "encrypted": Contains(
                                        "madv_incut_id=111",
                                        "hyper_cat_id=1",
                                        "pp=87",
                                        "vendor_ds_id=28195",
                                        "brand_id=1",
                                        "position=2",
                                        "vendor_price=500",
                                        "vc_bid=1200",
                                        "url_type=16",
                                        "madv_target_hid=2",
                                    ),
                                }
                            }
                        ],
                        "titleInfo": {
                            "logo": [
                                {
                                    "urls": {
                                        "encrypted": Contains(
                                            "madv_incut_id=111",
                                            "hyper_cat_id=1",
                                            "pp=87",
                                            "vendor_ds_id=28195",
                                            "brand_id=1",
                                            "position=1",
                                            "vendor_price=500",
                                            "vc_bid=1200",
                                            "url_type=58",
                                            "madv_target_hid=2",
                                        ),
                                    }
                                }
                            ]
                        },
                    }
                ]
            },
        )

        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'items': [
                            {
                                'vendor': {
                                    'name': 'vendor_1',
                                    'id': 1,
                                },
                                'slug': Contains('modelka-'),
                            }
                        ]
                    }
                ]
            },
        )

    # TODO add test with supported-incuts (cmc or basic carousel)


if __name__ == '__main__':
    main()
