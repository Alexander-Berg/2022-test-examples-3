#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.types import (
    Model,
    Offer,
    Vendor,
    IncutBlackListFb,
)
from core.matcher import EmptyList
from core.testcase import main
from test_madv_model_card import TMadvModelCardTest


class T(TMadvModelCardTest):
    @classmethod
    def __create_request(cls, hyperid, flag=0):
        params = {
            'place': 'madv_incut',
            'supported-incuts': '{"101":[20]}',  # 20 - basic_carousel
            'hyperid': hyperid,
            'show-urls': 'productVendorBid',
        }
        rearr_flags = {
            'market_madv_saas_request_with_target_page': 1,  # TODO remove after https://st.yandex-team.ru/MEDIAADV-154
            'market_madv_saas_request_with_vendor_id': 1,
            'market_blender_media_adv_incut_enabled': 1,
            'market_output_advert_request_blacklist_fb': flag,
        }
        return cls.get_request(params, rearr_flags)

    @classmethod
    def prepare_basic_incut(cls):
        hid = 1
        vendor_id = 1

        cls.index.incut_black_list_fb += [IncutBlackListFb(subtreeHids=[1], inclids=['AllAdv'])]

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

        models = {}
        for i in range(1, 10):
            models.update({"{}".format(i): {"modelId": i}})

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
        incut_items = [{"entity": "model", "id": "{}".format(i)} for i in range(1, 10)]
        header = {'type': 'default', 'text': 'media_element text', 'logos': [{"entity": "mediaElement", "id": "1"}]}
        incut = {
            "1": {
                "saasId": 111,
                "incutType": "ModelsList",
                "vendor": {"entity": "vendor", "id": str(vendor_id)},
                "constraints": {
                    "minDocs": 1,
                    "maxDocs": 10000,
                },
                "saasRequestHid": hid,
                "bidInfo": {
                    "clickPrice": 500,
                    "bid": 1200,
                },
                "logo": {"entity": "mediaElement", "id": "1"},
                "models": incut_items,
                "alternativeIncutTypes": [
                    {
                        "incutType": "ModelCardBasic",
                        "minDocs": 3,
                        "maxDocs": 9,
                    },
                ],
                'header': header,
            }
        }

        vendor = {"1": {"datasourceId": 28195, "vendorName": 'vendor_1', "vendorId": 1}}

        # TODO add page and vendor
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

    def test_black_list_on_incut(self):
        """
        Провекра работы blacklist для врезок
        Флаг включен, ответ - пустая врезка
        """
        request = self.__create_request(5, 1)
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            "entity": "competitive_model_card",
                            "incutId": "CompetitiveModelCardSingle",
                            "items": EmptyList(),
                        }
                    ]
                }
            },
        )

    def test_black_list_off_incut(self):
        """
        Провекра работы blacklist для врезок
        Флаг выключен, ответ - врезка с элементами
        """
        request = self.__create_request(5, 0)
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            "entity": "madv_incut",
                            "incutId": "MadvCardCarousel",
                        }
                    ]
                }
            },
        )


if __name__ == '__main__':
    main()
