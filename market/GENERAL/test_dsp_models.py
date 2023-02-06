#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.testcase import TestCase, main
from core.types import BlueOffer, Const, MarketSku, Model, Offer, PictureMbo
from core.matcher import LikeUrl
from core.logs import ErrorCodes

from google.protobuf.json_format import MessageToDict
import market.report.proto.recom.DspModels_pb2 as dsp_models_pb2


avatarsSizes = {2: [100, 100], 5: [200, 200], 8: [240, 240]}


def full_model():
    return {
        "wprid": "cb8f7a995444ab4dc3215cad33fcd6bb",
        "dspModels": [
            {
                "title": "Синяя модель для DSP 1001",
                "pictures": [
                    {
                        "original": {
                            "url": "//avatars.mds.yandex.net/get-mpic/1001/img_id1/orig",
                            "width": 500,
                            "height": 600,
                            "containerWidth": 500,
                            "containerHeight": 600,
                        }
                    }
                ],
                "marketSku": "10010",
                "urls": {
                    "direct": "//market.yandex.ru/product--siniaia-model-dlia-dsp-1001/1001?wprid=cb8f7a995444ab4dc3215cad33fcd6bb"
                },
                "prices": {"max": "1001", "avg": "1001", "min": "1001", "discount": {"oldMin": "1101", "percent": 9}},
                "modelId": 1001,
            }
        ],
    }


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.fixed_index_generation = '20200101_0300'

        models = list(range(1000, 1101))
        cls.index.models += [
            Model(
                hyperid=m,
                title='Синяя модель для DSP ' + str(m),
                proto_picture=PictureMbo(
                    '//avatars.mds.yandex.net/get-mpic/{}/img_id1/orig'.format(m), width=500, height=600
                ),
                proto_add_pictures=[
                    PictureMbo('//avatars.mds.yandex.net/get-mpic/{}/img_id2/orig'.format(m), width=600, height=700),
                    PictureMbo('//avatars.mds.yandex.net/get-mpic/{}/img_id3/orig'.format(m), width=700, height=800),
                    PictureMbo('//avatars.mds.yandex.net/get-mpic/{}/img_id4/orig'.format(m), width=800, height=900),
                    PictureMbo('//avatars.mds.yandex.net/get-mpic/{}/img_id5/orig'.format(m), width=900, height=1000),
                ],
            )
            for m in models
        ]
        cls.index.offers += [Offer(hyperid=m, price=100 + m * 10) for m in models]
        cls.index.mskus += [
            MarketSku(hyperid=m, sku=m * 10, blue_offers=[BlueOffer(price=m, price_old=100 + m)]) for m in models
        ]

    def get_dsp_model(self, index, clid=None, touch=False, lr=None, picture_num=1, thumbnail_size=-1):
        model_id = index
        sku_id = index * 10
        parts = []
        if clid:
            parts.append('clid={}'.format(clid))
        if lr:
            parts.append('lr={}'.format(lr))
        parts.append('wprid={}'.format(Const.DEFAULT_REQ_ID))

        touch_str = 'm.' if touch else ''
        slug_str = 'product--siniaia-model-dlia-dsp-{}'.format(index)

        res = {
            "entity": "product",
            "titles": {"raw": "Синяя модель для DSP " + str(index)},
            "urls": {"direct": "//{}market.yandex.ru/{}/{}?{}".format(touch_str, slug_str, model_id, '&'.join(parts))},
            "id": model_id,
            "marketSku": str(sku_id),
            "prices": {
                "min": str(index),
                "max": str(index),
                "currency": "RUR",
                "avg": str(index),
                "discount": {"oldMin": str(100 + index)},
            },
        }
        if picture_num > 0:
            res["pictures"] = []
        for pic in range(picture_num):
            thumbnails = []
            if thumbnail_size > -1:
                thumbnails.append(
                    {
                        "containerHeight": avatarsSizes[thumbnail_size][0],
                        "containerWidth": avatarsSizes[thumbnail_size][0],
                        "height": avatarsSizes[thumbnail_size][0],
                        "url": '//avatars.mds.yandex.net/get-mpic/{}/img_id{}/{}hq'.format(
                            index, pic + 1, thumbnail_size
                        ),
                        "width": avatarsSizes[thumbnail_size][0],
                    }
                )
            res["pictures"].append(
                {
                    "entity": "picture",
                    "original": {
                        "containerHeight": 600 + pic * 100,
                        "containerWidth": 500 + pic * 100,
                        "height": 600 + pic * 100,
                        "url": '//avatars.mds.yandex.net/get-mpic/{}/img_id{}/orig'.format(index, pic + 1),
                        "width": 500 + pic * 100,
                    },
                    "signatures": [],
                    "thumbnails": thumbnails,
                }
            )

        return res

    def test_dsp_models(self):
        # проверяем, что плейс возвращает нужные нам ответы
        response = self.report.request_json(
            'place=dsp_models&hyperid=1001&hyperid=1002&hyperid=1000&hyperid=1050&hyperid=1100'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        self.get_dsp_model(1001),
                        self.get_dsp_model(1002),
                        self.get_dsp_model(1000),
                        self.get_dsp_model(1050),
                        self.get_dsp_model(1100),
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_dsp_models_default_single_picture(self):
        # проверяем, что плейс возвращает нужные нам ответы
        response = self.report.request_json('place=dsp_models&hyperid=1001&hyperid=1002')
        # only one pictire
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        self.get_dsp_model(1001, picture_num=1),
                        self.get_dsp_model(1002, picture_num=1),
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )
        # no second picture
        self.assertFragmentNotIn(
            response,
            {
                'search': {
                    'results': [
                        self.get_dsp_model(1001, picture_num=2),
                        self.get_dsp_model(1002, picture_num=2),
                    ],
                }
            },
        )

    def test_dsp_models_three_pictures(self):
        # проверяем, что плейс возвращает нужные нам ответы
        response = self.report.request_json(
            'place=dsp_models&hyperid=1001&hyperid=1002&rearr-factors=mars_dsp_max_images_count=3'
        )
        # only one pictire
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        self.get_dsp_model(1001, picture_num=3),
                        self.get_dsp_model(1002, picture_num=3),
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )
        # no fourth picture
        self.assertFragmentNotIn(
            response,
            {
                'search': {
                    'results': [
                        self.get_dsp_model(1001, picture_num=4),
                        self.get_dsp_model(1002, picture_num=4),
                    ],
                }
            },
        )

    def test_dsp_models_add_thumbnail(self):
        # проверяем, что плейс возвращает нужные нам ответы
        for thumbnail_size in [2, 5, 8]:
            response = self.report.request_json(
                'place=dsp_models&hyperid=1001&hyperid=1002'
                '&rearr-factors=mars_dsp_thumbnails_size={}'.format(thumbnail_size)
            )
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'results': [
                            self.get_dsp_model(1001, thumbnail_size=thumbnail_size),
                            self.get_dsp_model(1002, thumbnail_size=thumbnail_size),
                        ],
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )

    def test_dsp_pictures_and_thumbnail(self):
        # проверяем, что плейс возвращает нужные нам ответы
        response = self.report.request_json(
            'place=dsp_models&hyperid=1001&hyperid=1002'
            '&rearr-factors=mars_dsp_thumbnails_size=5;mars_dsp_max_images_count=2'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        self.get_dsp_model(1001, picture_num=2, thumbnail_size=5),
                        self.get_dsp_model(1002, picture_num=2, thumbnail_size=5),
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_wrong_experiment_flags_format(self):
        wrong_rearrs = [
            ['mars_dsp_max_images_count=!', 1],
            ['mars_dsp_thumbnails_size=x', 1],
            ['mars_dsp_max_images_count=-1', 1],
        ]
        for case in wrong_rearrs:
            response = self.report.request_json(
                'place=dsp_models&hyperid=1001&hyperid=1002' '&rearr-factors={}'.format(case[0])
            )
            self.error_log.expect(code=ErrorCodes.CGI_CANNOT_PARSE_EXPERIMENTAL_FLAG)
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'results': [
                            self.get_dsp_model(1001, picture_num=case[1]),
                            self.get_dsp_model(1002, picture_num=case[1]),
                        ],
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )
        self.error_log.expect(code=ErrorCodes.CGI_CANNOT_PARSE_EXPERIMENTAL_FLAG).times(6)
        self.base_logs_storage.error_log.expect(code=ErrorCodes.CGI_CANNOT_PARSE_EXPERIMENTAL_FLAG).times(3)

    def test_wrong_experiment_flags_value(self):
        wrong_rearrs = [
            ['mars_dsp_max_images_count=0', 0],
            ['mars_dsp_max_images_count=100', 5],  # max possible
            ['mars_dsp_thumbnails_size=15', 1],  # one pic with no thumbnails
        ]
        for case in wrong_rearrs:
            response = self.report.request_json(
                'place=dsp_models&hyperid=1001&hyperid=1002' '&rearr-factors={}'.format(case[0])
            )
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'results': [
                            self.get_dsp_model(1001, picture_num=case[1]),
                            self.get_dsp_model(1002, picture_num=case[1]),
                        ],
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )

    def test_protobuf_format(self):
        """Проверяем, что ручка поддерживает формат для выдачи протобуфа"""
        response = self.report.request_plain('place=dsp_models&hyperid=1001&bsformat=7')
        dsp_models = dsp_models_pb2.TDspModels()
        dsp_models.ParseFromString(str(response))

        self.assertEqual(MessageToDict(dsp_models), full_model())

    @classmethod
    def prepare_price_source(cls):
        cls.index.models += [
            Model(
                hyperid=1200,
                title='Пельмени замороженные',
                proto_picture=PictureMbo('//avatars.mds.yandex.net/get-mpic/1200/img_id1/orig', width=500, height=600),
                proto_add_pictures=[
                    PictureMbo('//avatars.mds.yandex.net/get-mpic/1200/img_id3/orig', width=700, height=800),
                ],
            )
        ]
        cls.index.mskus += [
            MarketSku(
                hyperid=1200,
                sku=12000,
                blue_offers=[
                    BlueOffer(price=1200, price_old=1300, waremd5="Waremd5FirstBlueOfferg"),
                    BlueOffer(price=1220, price_old=1280, waremd5="Waremd5SecondBlueOffer"),
                    BlueOffer(price=1230, price_old=1250, waremd5="-Waremd5ThirdBlueOffer"),
                ],
            )
        ]

    def test_price_source(self):
        """Проверяем, что логика заполнения значения цены для модели меняется
        в зависимости от значения флага mars_dsp_models_prices_source"""

        # mars_dsp_models_prices_source=regional_statistic
        response = self.report.request_json(
            'place=dsp_models&hyperid=1200&rearr-factors=mars_dsp_models_prices_source=regional_statistic'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            "prices": {
                                "currency": "RUR",
                                "discount": {"oldMin": "1250"},
                                "min": "1200",
                                "avg": "1220",
                                "max": "1230",
                            },
                            "urls": {"direct": LikeUrl(url_host="market.yandex.ru", no_params={"do-waremd5"})},
                        }
                    ],
                }
            },
        )

        # mars_dsp_models_prices_source=default_offer
        response = self.report.request_json(
            'place=dsp_models&hyperid=1200&rearr-factors=mars_dsp_models_prices_source=default_offer'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            "prices": {
                                "currency": "RUR",
                                "discount": {"oldMin": "1300"},
                                "min": "1200",
                                "avg": "1200",
                                "max": "1200",
                            },
                            "urls": {
                                "direct": LikeUrl(
                                    url_host="market.yandex.ru", url_params={"do-waremd5": "Waremd5FirstBlueOfferg"}
                                )
                            },
                        }
                    ],
                }
            },
        )

    def test_show_log_generation(self):
        self.report.request_json('place=dsp_models&hyperid=1001')
        self.show_log.expect(record_type=1, hyper_id=1001, index_generation=self.index.fixed_index_generation)


if __name__ == '__main__':
    main()
