#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.types import (
    Model,
    Vendor,
    MarketSku,
    BlueOffer,
)
from core.testcase import main
from core.matcher import (
    Contains,
)
from market.media_adv.proto.output.output_incuts_pb2 import (
    EIncutType,
    TBidInfo,
    TConstraints,
    THeader,
    TIncut,
    TModel,
    TVendor,
)
from test_media_adv import TestMediaAdv


class T(TestMediaAdv):
    """
    тесты Врезочника (МПФ) на получение врезок при различных условиях
    """

    @classmethod
    def prepare_market_sku_output(cls):
        vendor_id = 45
        vendor_name = 'vendor_{}'.format(vendor_id)
        cls.index.vendors += [Vendor(vendor_id=vendor_id, name=vendor_name)]

        hid = 100
        start_hyper_id = 2000
        start_sku_id = 1000
        datasource = 447
        cls.index.mskus += [
            MarketSku(
                sku=start_sku_id + x,
                hyperid=start_hyper_id + x if x < 5 else None,
                vendor_id=vendor_id,
                hid=hid,
                blue_offers=[
                    BlueOffer(
                        offerid=start_sku_id + x,
                        feedid=start_sku_id + x,
                    )
                ],
            )
            for x in range(10)
        ]
        cls.index.models += [
            Model(
                hid=hid,
                hyperid=start_hyper_id + x,
                vendor_id=vendor_id,
                vbid=10,
                datasource_id=datasource,
                title="toy {}".format(start_hyper_id + x),
            )
            for x in range(3)
        ]

        mock_str = TestMediaAdv.media_adv_mock(
            TIncut(
                Models=[TModel(Id=start_hyper_id + i if i < 5 else None, Sku=start_sku_id + i) for i in range(10)],
                Vendor=TVendor(
                    Id=vendor_id,
                    DatasourceId=datasource,
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
                Header=THeader(
                    Type='default',
                    Text='title',
                ),
            )
        )
        cls.media_advertising.on_request_media_adv_incut(hid=hid).respond(mock_str)

    def test_market_sku_output(self):
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
                            "titleInfo": {},
                            "items": [
                                {
                                    "entity": "product",
                                    "isVirtual": False,
                                    "urls": {
                                        "encrypted": Contains(
                                            "/madv_incut_id=761/",
                                            "/vendor_ds_id=447/",
                                            "/vendor_price=500/",
                                            "/vc_bid=1200/",
                                            "/pp=84/",
                                        ),
                                    },
                                    "offers": {
                                        "items": [
                                            {
                                                "urls": {
                                                    "cpa": Contains(
                                                        "/madv_incut_id=761/",
                                                        "/pp=84/",
                                                    ),
                                                },
                                            },
                                        ],
                                    },
                                },
                                {
                                    "entity": "product",
                                    "isVirtual": True,
                                    "urls": {
                                        "encrypted": Contains(
                                            "/madv_incut_id=761/",
                                            "/vendor_ds_id=447/",
                                            "/vendor_price=500/",
                                            "/vc_bid=1200/",
                                            "/pp=84/",
                                        ),
                                    },
                                    "offers": {
                                        "items": [
                                            {
                                                "urls": {
                                                    "cpa": Contains(
                                                        "/madv_incut_id=761/",
                                                        "/pp=84/",
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
            },
        )
        self.show_log.expect(pp=84, madv_incut_id=761, url_type=6, position=1)
        self.show_log.expect(pp=84, madv_incut_id=761, url_type=6, position=10)
        self.show_log.expect(
            pp=84, madv_incut_id=761, url_type=16, position=1, vc_bid=1200, vendor_price=500, vendor_ds_id=447
        )
        self.show_log.expect(
            pp=84, madv_incut_id=761, url_type=16, position=10, vc_bid=1200, vendor_price=500, vendor_ds_id=447
        )


if __name__ == '__main__':
    main()
