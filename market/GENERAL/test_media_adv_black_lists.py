#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    Model,
    Offer,
    Vendor,
    IncutBlackListFb,
)
from core.testcase import main
from core.matcher import EmptyList
from market.report.proto.ReportState_pb2 import TCommonReportState  # noqa pylint: disable=import-error
from test_media_adv import TestMediaAdv

base_params = []

hid = 1212


class T(TestMediaAdv):
    @classmethod
    def prepare_basic_carousel_plain(cls):
        global hid

        cls.index.vendors += [Vendor(vendor_id=x, name='Noodles shop N {}'.format(x)) for x in range(1, 4)]

        cls.index.incut_black_list_fb += [IncutBlackListFb(subtreeHids=[hid], inclids=['AllAdv'])]

        start_hyperid_1 = 9876
        cls.index.models += [
            Model(
                hid=hid,
                hyperid=start_hyperid_1 + x,
                vendor_id=1,
                vbid=10,
                datasource_id=444,
                title="Noodles {}".format(start_hyperid_1 + x),
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
                vbid=20,  # more than vendor 1
                title="Noodles {}".format(start_hyperid_2 + x),
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

        models = {}
        for i in range(10):
            models.update({"{}".format(i + 1): {"modelId": start_hyperid_1 + i}})

        media_element = {
            "1": {
                "type": "Logo",
                "title": "vendor_1 logo",
                "text": {
                    "text": "media_element text",
                    "color": "black",
                },
                "id": 761,
                "bidInfo": {
                    "clickPrice": 500,
                    "bid": 1200,
                },
                "pixelUrl": "",
                "clickUrl": "",
                'image': {
                    'url': 'https://avatars.mds.yandex.net/get-mpic/1862933/img_id5477847296568171778.png/orig',
                    'width': 800,
                    'height': 600,
                },
            }
        }

        incut_items = [{"entity": "model", "id": "{}".format(i)} for i in range(1, 11)]
        header = {
            'type': 'default',
            'text': 'media_element text',
        }
        incut = {
            "1": {
                "saasId": 761,
                "incutType": "ModelsList",
                "vendor": {"entity": "vendor", "id": "1"},
                "constraints": {
                    "minDocs": 1,
                    "maxDocs": 10000,
                },
                "saasRequestHid": 651,
                "bidInfo": {
                    "clickPrice": 500,
                    "bid": 1200,
                },
                "logo": {"entity": "mediaElement", "id": "1"},
                "models": incut_items,
                "alternativeIncutTypes": [
                    {
                        "incutType": "ModelsList3Items",
                        "minDocs": 3,
                        "maxDocs": 9,
                    },
                ],
                'header': header,
            }
        }

        vendor = {"1": {"datasourceId": 28195, "vendorName": 'vendor_1', "vendorId": 1}}

        cls.media_advertising.on_request_media_adv_incut(hid=hid).respond(
            {
                "incutLists": [
                    [
                        {
                            "entity": "incut",
                            "id": "1",
                        }
                    ]
                ],
                "entities": {
                    "model": models,
                    "mediaElement": media_element,
                    "incut": incut,
                    "vendor": vendor,
                },
            }
        )

    def test_turn_off_black_list(self):
        """
        Проверка работы фильтрации врезок по black_list
        Флаг выключен - врезка должна собраться
        """
        params, rearr_factors = self.get_params_rearr_factors('Noodles', hid)
        rearr_factors["market_output_advert_request_blacklist_fb"] = 0
        response = self.report.request_json(self.get_request(params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            "incutId": "basic_carousel_plain_top",
                            "items": [
                                {
                                    "entity": "product",
                                }
                            ],
                        }
                    ]
                },
            },
            preserve_order=True,
        )

    def test_turn_on_black_list(self):
        """
        Проверка работы фильтрации врезок по black_list
        Флаг включен - не должно быть врезки
        """
        params, rearr_factors = self.get_params_rearr_factors('Noodles', hid)
        rearr_factors["market_output_advert_request_blacklist_fb"] = 1
        response = self.report.request_json(self.get_request(params, rearr_factors))
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": EmptyList(),
                },
            },
            preserve_order=True,
        )


if __name__ == '__main__':
    main()
