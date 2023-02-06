#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Model, Offer, Vendor, MarketSku, Picture
from core.testcase import main
from core.types.picture import thumbnails_config
from market.media_adv.proto.output.output_incuts_pb2 import (
    EIncutType,
    EMediaElementType,
    TBidInfo,
    TConstraints,
    THeader,
    TImage,
    TIncut,
    TMediaElement,
    TModel,
    TVendor,
)
from test_media_adv import TestMediaAdv


class T(TestMediaAdv):
    @classmethod
    def prepare_basic_carousel_plain(cls):
        cls.index.vendors += [Vendor(vendor_id=x, name='vendor_{}'.format(x)) for x in range(1, 4)]
        pic1 = Picture(
            picture_id="KdwwrYb4czANgt9-3poEQQ",
            width=1722,
            height=1937,
            thumb_mask=thumbnails_config.get_mask_by_names(['1x1', '100x100']),
        )
        pic2 = Picture(
            picture_id="Awadavra2345Kedavra-3poEQQ",
            width=9313,
            height=4322,
            thumb_mask=thumbnails_config.get_mask_by_names(['1x1', '100x100']),
        )
        hid = 551
        start_hyperid_1 = 1300
        start_sku1 = 1208
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
                sku=start_sku1 + x,
            )
            for x in range(1, 10)
        ]
        cls.index.mskus += [
            MarketSku(
                hyperid=start_hyperid_1 + x,
                sku=start_sku1 + x,
                picture=pic1,
                descr="beautiful garden flower",
                title="flower" + str(start_sku1 + x),
            )
            for x in range(0, 10)
        ]
        start_hyperid_2 = start_hyperid_1 + 10
        start_sku2 = 8012
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
                sku=start_sku2 + x,
            )
            for x in range(1, 10)
        ]
        cls.index.mskus += [
            MarketSku(
                hyperid=start_hyperid_2 + x,
                sku=start_sku2 + x,
                picture=pic2,
                descr="good shampoo with marshmallows",
                title="shampoo" + str(start_sku2 + x),
            )
            for x in range(0, 10)
        ]

        mock_str = TestMediaAdv.media_adv_mock(
            TIncut(
                Models=[TModel(Id=start_hyperid_1 + i) for i in range(10)],
                SaasId=761,
                IncutType=EIncutType.ModelsList,
                Vendor=TVendor(Id=1, DatasourceId=28195, Name='vendor_1'),
                Constraints=TConstraints(MinDocs=1, MaxDocs=10000),
                BidInfo=TBidInfo(ClickPrice=500, Bid=1200),
                Header=THeader(
                    Type='default',
                    Text='vendor_1 logo',
                    Logos=[
                        TMediaElement(
                            Type=EMediaElementType.Logo,
                            Id=761,
                            SourceImage=TImage(Url="picture_url", Width=800, Height=600),
                            PixelUrl="logo_pixel_url",
                            ClickUrl="logo_click_url",
                            BidInfo=TBidInfo(ClickPrice=500, Bid=1200),
                        ),
                    ],
                ),
            )
        )

        cls.media_advertising.on_request_media_adv_incut(hid=hid).respond(mock_str)

    def test_basic_carousel_plain(self):
        """
        Проверяем работу мока врезочника,
        проверяем поход за модельками из новой врезки
        проверка выдачи картинки к каждой из моделеки
        """

        params, rearr_factors = self.get_params_rearr_factors('toy', 551)
        response = self.report.request_json(self.get_request(params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "offers": {
                            "items": [
                                {
                                    "skuAwarePictures": [
                                        {"entity": "picture", "original": {"width": 1722, "height": 1937}}
                                    ]
                                }
                            ]
                        }
                    },
                ]
            },
            preserve_order=True,
        )

        for item in response['incuts']['results'][0]['items']:
            offer_items = item["offers"]["items"]
            # проверяем, что у всех моделей в выдаче один оффер
            self.assertEqual(len(offer_items), 1)


if __name__ == '__main__':
    main()
