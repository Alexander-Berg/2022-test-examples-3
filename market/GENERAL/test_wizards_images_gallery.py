#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.types import BlueOffer, MarketSku, MnPlace, Model, Offer, Opinion, Shop
from core.testcase import TestCase, main
from core.types.picture import Picture, to_mbo_picture
from core.matcher import Contains

"""
Тест для эксперимента с галереей картинок для колдунщика
@see MARKETOUT-39134
"""


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.shops += [
            Shop(fesh=5, cpa=Shop.CPA_REAL),
            Shop(fesh=6, cpa=Shop.CPA_REAL),
            Shop(fesh=7, cpa=Shop.CPA_REAL),
            Shop(fesh=101, priority_region=1),
            Shop(
                fesh=431782,
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                priority_region=213,
                name='Яндекс.Маркет',
            ),
            Shop(fesh=1, priority_region=213, regions=[225]),
            Shop(
                fesh=10,
                datafeed_id=10,
                priority_region=213,
                regions=[225],
                supplier_type=Shop.THIRD_PARTY,
                blue='REAL',
                name='3p shop',
            ),
        ]
        offer_images = [
            Picture(picture_id='iyC3nHslqLtqZJLygVAHeA', width=200, height=200, group_id=1),
            Picture(picture_id='iyC3nHslqLtqZJLygVAHeB', width=200, height=200, group_id=2),
        ]
        cls.index.mskus += [
            MarketSku(
                sku=1001,
                title="iphone blue 1",
                hyperid=101,
                blue_offers=[
                    BlueOffer(
                        ts=100101,
                        waremd5='gTL-3D5IXpiHAL-CvNRmNQ',
                        feedid=431782,
                        delivery_buckets=[101, 104],
                        offerid='iphone-blue-1',
                        price=100,
                        hid=10,
                        pictures=offer_images,
                    )
                ],
            ),
            MarketSku(
                sku=1002,
                title="iphone blue 2",
                blue_offers=[
                    BlueOffer(
                        ts=100201,
                        waremd5='jsFnEBncNV6VLkT9w4BajQ',
                        feedid=431782,
                        delivery_buckets=[102],
                        pictures=offer_images,
                    )
                ],
            ),
            MarketSku(
                sku=1003,
                title="iphone blue 3",
                blue_offers=[
                    BlueOffer(
                        ts=100301,
                        waremd5='pnO6jtfjEy9AfE4RIpBsnQ',
                        feedid=10,
                        delivery_buckets=[103],
                        pictures=offer_images,
                    )
                ],
            ),
            MarketSku(
                sku=1100,
                title="air pods 1",
                blue_offers=[BlueOffer(feedid=431782, offerid='air-pods-1', pictures=offer_images)],
            ),
        ]
        cls.index.models += [
            Model(
                hyperid=101,
                title="iphone blue model 1",
                opinion=Opinion(rating=4, precise_rating=4.5),
                hid=10,
                proto_picture=to_mbo_picture('//avatars.mds.yandex.net/get-mpic/model_101_0/orig#200#200'),
                proto_add_pictures=[to_mbo_picture('//avatars.mds.yandex.net/get-mpic/model_101_1/orig#200#200')],
            ),
            Model(
                hyperid=2001,
                title="iphone model 1",
                hid=10,
                proto_picture=to_mbo_picture('//avatars.mds.yandex.net/get-mpic/model_2001_0/orig#200#200'),
                proto_add_pictures=[to_mbo_picture('//avatars.mds.yandex.net/get-mpic/model_2001_1/orig#200#200')],
            ),
            Model(
                hyperid=2002,
                title="iphone model 2",
                proto_picture=to_mbo_picture('//avatars.mds.yandex.net/get-mpic/model_2002_0/orig#200#200'),
                proto_add_pictures=[to_mbo_picture('//avatars.mds.yandex.net/get-mpic/model_2002_1/orig#200#200')],
            ),
        ]
        cls.index.offers += [
            Offer(hyperid=2001, fesh=1, pictures=offer_images),
            Offer(title="iphone white 1", fesh=2, ts=101, pictures=offer_images),
            Offer(
                title="iphone dsbs 1",
                fesh=5,
                ts=201,
                cpa=Offer.CPA_REAL,
                hyperid=2001,
                hid=10,
                waremd5="A1c7MrJ0bm6MqYuRI_Ikmw",
                pictures=offer_images,
            ),
            Offer(title="iphone dsbs 2", fesh=6, ts=202, cpa=Offer.CPA_NO, pictures=offer_images),
            Offer(title="iphone dsbs 3", fesh=7, ts=203, cpa=Offer.CPA_REAL, pictures=offer_images),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 101).respond(0.6)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 201).respond(0.6)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 202).respond(0.5)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 203).respond(0.4)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 100101).respond(0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 100201).respond(0.8)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 100301).respond(0.7)

    def test_gallery_in_cpa_items(self):
        """
        Проверяем наличие/отсутствие галереи изображений в cpaItems
        """
        request = 'place=parallel&text=iphone&rearr-factors=market_cpa_offers_incut_threshold=0;market_cpa_offers_incut_count=1;market_offers_wizard_cpa_offers_incut=1;market_parallel_document_images_count=2;market_cpa_offers_incut_hide_duplicates=0'  # noqa
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "cpaItems": [
                            {
                                "images": [
                                    {
                                        "image": "http://avatars.mdst.yandex.net/get-marketpic/1/market_iyC3nHslqLtqZJLygVAHeA/100x100",
                                        "imageHd": "http://avatars.mdst.yandex.net/get-marketpic/1/market_iyC3nHslqLtqZJLygVAHeA/200x200",
                                    },
                                    {
                                        "image": "http://avatars.mdst.yandex.net/get-marketpic/2/market_iyC3nHslqLtqZJLygVAHeA/100x100",
                                        "imageHd": "http://avatars.mdst.yandex.net/get-marketpic/2/market_iyC3nHslqLtqZJLygVAHeA/200x200",
                                    },
                                ]
                            }
                        ]
                    }
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_gallery_in_offers_wizard(self):
        """
        Проверяем наличие/отсутствие галереи изображений в офферных колдунщиках
        """
        request = 'place=parallel&text=iphone&rearr-factors=market_parallel_document_images_count=2'
        response = self.report.request_bs_pb(request)
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {
                                "images": [
                                    {
                                        "image": "http://avatars.mdst.yandex.net/get-marketpic/1/market_iyC3nHslqLtqZJLygVAHeA/100x100",
                                        "imageHd": "http://avatars.mdst.yandex.net/get-marketpic/1/market_iyC3nHslqLtqZJLygVAHeA/200x200",
                                    },
                                    {
                                        "image": "http://avatars.mdst.yandex.net/get-marketpic/2/market_iyC3nHslqLtqZJLygVAHeA/100x100",
                                        "imageHd": "http://avatars.mdst.yandex.net/get-marketpic/2/market_iyC3nHslqLtqZJLygVAHeA/200x200",
                                    },
                                ]
                            }
                            for i in range(3)
                        ]
                    }
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_gallery_in_model_offers(self):
        """
        Проверяем наличие/отсутствие галереи изображений в модельно-офферных колдунщиках
        """
        request = 'place=parallel&text=iphone&rearr-factors=market_enable_model_offers_wizard=1;'
        response = self.report.request_bs_pb(request + 'market_parallel_document_images_count=2')
        self.assertFragmentIn(
            response,
            {
                "market_model_offers_wizard": {
                    "showcase": {
                        "items": [
                            {
                                "images": [
                                    {
                                        "image": Contains("//avatars.mds.yandex.net"),
                                        "imageHd": Contains("//avatars.mds.yandex.net"),
                                    },
                                    {
                                        "image": Contains("//avatars.mds.yandex.net"),
                                        "imageHd": Contains("//avatars.mds.yandex.net"),
                                    },
                                ]
                            }
                        ]
                    }
                }
            },
        )

    def test_gallery_in_implicit_model(self):
        """
        Проверка количества картинок в галереи для неявной модели колдунщика
        """
        request = "place=parallel&text=iphone&rearr-factors="
        # market_parallel_document_images_count=3, но картинок в модели только две -> лишних объектов изображений быть не должно
        response = self.report.request_bs(request + "market_parallel_document_images_count=3")
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": [
                    {
                        "showcase": {
                            "items": [
                                {
                                    "images": [
                                        {
                                            "image": "//avatars.mds.yandex.net/get-mpic/model_2001_0/2hq",
                                            "imageHd": "//avatars.mds.yandex.net/get-mpic/model_2001_0/5hq",
                                        },
                                        {
                                            "image": "//avatars.mds.yandex.net/get-mpic/model_2001_1/2hq",
                                            "imageHd": "//avatars.mds.yandex.net/get-mpic/model_2001_1/5hq",
                                        },
                                    ]
                                }
                            ]
                        }
                    }
                ]
            },
        )


if __name__ == '__main__':
    main()
