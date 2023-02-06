#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Book, HyperCategory, HyperCategoryType, Model, ModelGroup, Offer, Picture, VCluster
from core.types.picture import thumbnails_config, to_mbo_picture
from core.testcase import TestCase, main

from core.matcher import NoKey, Absent


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']
        # RANDX randomizing is disabled because these tests don't work with it. See MARKETOUT-21319
        cls.disable_randx_randomize()

    @classmethod
    def prepare_pictures(cls):
        # MARKETOUT-11027 (model with different pictures)
        cls.index.models += [
            Model(title='mdata model', hyperid=1000),
            Model(
                hyperid=1002,
                title='avatars model',
                picinfo='//avatars.mdst.yandex.net/get-mpic/4138/test100/orig#100#200',
                add_picinfo='//avatars.mdst.yandex.net/get-mpic/1338/test100/orig#661#519',
            ),
        ]
        cls.index.model_groups += [
            ModelGroup(title='mdata groupmodel', hyperid=1001),
            ModelGroup(
                hyperid=1003,
                title='avatars groupmodel',
                picinfo='//avatars.mdst.yandex.net/get-mpic/4139/test100/orig#100#200',
                add_picinfo='//avatars.mdst.yandex.net/get-mpic/1339/test100/orig#661#519',
            ),
        ]

    def test_mdata_thumbnails(self):
        expected_group_model_fragment = {
            "results": [
                {
                    "pictures": [
                        {
                            "original": {
                                "containerWidth": 100,
                                "containerHeight": 200,
                                "url": ModelGroup.DEFAULT_PIC_URL,
                                "width": 100,
                                "height": 200,
                            },
                            "thumbnails": [
                                {"url": ModelGroup.DEFAULT_PIC_URL + "&size=1"},
                                {"url": ModelGroup.DEFAULT_PIC_URL + "&size=2"},
                                {"url": ModelGroup.DEFAULT_PIC_URL + "&size=3"},
                                {"url": ModelGroup.DEFAULT_PIC_URL + "&size=4"},
                                {"url": ModelGroup.DEFAULT_PIC_URL + "&size=5"},
                                {"url": ModelGroup.DEFAULT_PIC_URL + "&size=6"},
                                {"url": ModelGroup.DEFAULT_PIC_URL + "&size=7"},
                                {"url": ModelGroup.DEFAULT_PIC_URL + "&size=8"},
                                {"url": ModelGroup.DEFAULT_PIC_URL + "&size=9"},
                            ],
                        },
                        {
                            "original": {
                                "containerWidth": 661,
                                "containerHeight": 519,
                                "url": ModelGroup.DEFAULT_ADD_PIC_URL,
                                "width": 661,
                                "height": 519,
                            },
                            "thumbnails": [
                                {"url": ModelGroup.DEFAULT_ADD_PIC_URL + "&size=1"},
                                {"url": ModelGroup.DEFAULT_ADD_PIC_URL + "&size=2"},
                                {"url": ModelGroup.DEFAULT_ADD_PIC_URL + "&size=3"},
                                {"url": ModelGroup.DEFAULT_ADD_PIC_URL + "&size=4"},
                                {"url": ModelGroup.DEFAULT_ADD_PIC_URL + "&size=5"},
                                {"url": ModelGroup.DEFAULT_ADD_PIC_URL + "&size=6"},
                                {"url": ModelGroup.DEFAULT_ADD_PIC_URL + "&size=7"},
                                {"url": ModelGroup.DEFAULT_ADD_PIC_URL + "&size=8"},
                                {"url": ModelGroup.DEFAULT_ADD_PIC_URL + "&size=9"},
                            ],
                        },
                    ]
                }
            ]
        }
        expected_model_fragment = {
            "results": [
                {
                    "pictures": [
                        {
                            "original": {
                                "containerWidth": 100,
                                "containerHeight": 200,
                                "url": Model.DEFAULT_PIC_URL,
                                "width": 100,
                                "height": 200,
                            },
                            "thumbnails": [
                                {"url": Model.DEFAULT_PIC_URL + "&size=1"},
                                {"url": Model.DEFAULT_PIC_URL + "&size=2"},
                                {"url": Model.DEFAULT_PIC_URL + "&size=3"},
                                {"url": Model.DEFAULT_PIC_URL + "&size=4"},
                                {"url": Model.DEFAULT_PIC_URL + "&size=5"},
                                {"url": Model.DEFAULT_PIC_URL + "&size=6"},
                                {"url": Model.DEFAULT_PIC_URL + "&size=7"},
                                {"url": Model.DEFAULT_PIC_URL + "&size=8"},
                                {"url": Model.DEFAULT_PIC_URL + "&size=9"},
                            ],
                        },
                        {
                            "original": {
                                "containerWidth": 661,
                                "containerHeight": 519,
                                "url": Model.DEFAULT_ADD_PIC_URL,
                                "width": 661,
                                "height": 519,
                            },
                            "thumbnails": [
                                {"url": Model.DEFAULT_ADD_PIC_URL + "&size=1"},
                                {"url": Model.DEFAULT_ADD_PIC_URL + "&size=2"},
                                {"url": Model.DEFAULT_ADD_PIC_URL + "&size=3"},
                                {"url": Model.DEFAULT_ADD_PIC_URL + "&size=4"},
                                {"url": Model.DEFAULT_ADD_PIC_URL + "&size=5"},
                                {"url": Model.DEFAULT_ADD_PIC_URL + "&size=6"},
                                {"url": Model.DEFAULT_ADD_PIC_URL + "&size=7"},
                                {"url": Model.DEFAULT_ADD_PIC_URL + "&size=8"},
                                {"url": Model.DEFAULT_ADD_PIC_URL + "&size=9"},
                            ],
                        },
                    ]
                }
            ]
        }

        for q in ('place=prime&text=mdata+groupmodel', 'place=modelinfo&hyperid=1001&rids=225'):
            self.assertFragmentIn(self.report.request_json(q), expected_group_model_fragment)

        for q in ('place=prime&text=mdata+model', 'place=modelinfo&hyperid=1000&rids=225'):
            self.assertFragmentIn(self.report.request_json(q), expected_model_fragment)

    def test_avatars_thumbnails(self):
        expected_group_model_fragment = {
            "results": [
                {
                    "pictures": [
                        {
                            "original": {
                                "containerWidth": 100,
                                "containerHeight": 200,
                                "url": "//avatars.mdst.yandex.net/get-mpic/4139/test100/orig",
                                "width": 100,
                                "height": 200,
                            },
                            "thumbnails": [
                                {"url": "//avatars.mdst.yandex.net/get-mpic/4139/test100/1hq"},
                                {"url": "//avatars.mdst.yandex.net/get-mpic/4139/test100/2hq"},
                                {"url": "//avatars.mdst.yandex.net/get-mpic/4139/test100/3hq"},
                                {"url": "//avatars.mdst.yandex.net/get-mpic/4139/test100/4hq"},
                                {"url": "//avatars.mdst.yandex.net/get-mpic/4139/test100/5hq"},
                                {"url": "//avatars.mdst.yandex.net/get-mpic/4139/test100/6hq"},
                                {"url": "//avatars.mdst.yandex.net/get-mpic/4139/test100/7hq"},
                                {"url": "//avatars.mdst.yandex.net/get-mpic/4139/test100/8hq"},
                                {"url": "//avatars.mdst.yandex.net/get-mpic/4139/test100/9hq"},
                            ],
                        },
                        {
                            "original": {
                                "containerWidth": 661,
                                "containerHeight": 519,
                                "url": "//avatars.mdst.yandex.net/get-mpic/1339/test100/orig",
                                "width": 661,
                                "height": 519,
                            },
                            "thumbnails": [
                                {"url": "//avatars.mdst.yandex.net/get-mpic/1339/test100/1hq"},
                                {"url": "//avatars.mdst.yandex.net/get-mpic/1339/test100/2hq"},
                                {"url": "//avatars.mdst.yandex.net/get-mpic/1339/test100/3hq"},
                                {"url": "//avatars.mdst.yandex.net/get-mpic/1339/test100/4hq"},
                                {"url": "//avatars.mdst.yandex.net/get-mpic/1339/test100/5hq"},
                                {"url": "//avatars.mdst.yandex.net/get-mpic/1339/test100/6hq"},
                                {"url": "//avatars.mdst.yandex.net/get-mpic/1339/test100/7hq"},
                                {"url": "//avatars.mdst.yandex.net/get-mpic/1339/test100/8hq"},
                                {"url": "//avatars.mdst.yandex.net/get-mpic/1339/test100/9hq"},
                            ],
                        },
                    ]
                }
            ]
        }
        expected_model_fragment = {
            "results": [
                {
                    "pictures": [
                        {
                            "original": {
                                "containerWidth": 100,
                                "containerHeight": 200,
                                "url": "//avatars.mdst.yandex.net/get-mpic/4138/test100/orig",
                                "width": 100,
                                "height": 200,
                            },
                            "thumbnails": [
                                {"url": "//avatars.mdst.yandex.net/get-mpic/4138/test100/1hq"},
                                {"url": "//avatars.mdst.yandex.net/get-mpic/4138/test100/2hq"},
                                {"url": "//avatars.mdst.yandex.net/get-mpic/4138/test100/3hq"},
                                {"url": "//avatars.mdst.yandex.net/get-mpic/4138/test100/4hq"},
                                {"url": "//avatars.mdst.yandex.net/get-mpic/4138/test100/5hq"},
                                {"url": "//avatars.mdst.yandex.net/get-mpic/4138/test100/6hq"},
                                {"url": "//avatars.mdst.yandex.net/get-mpic/4138/test100/7hq"},
                                {"url": "//avatars.mdst.yandex.net/get-mpic/4138/test100/8hq"},
                                {"url": "//avatars.mdst.yandex.net/get-mpic/4138/test100/9hq"},
                            ],
                        },
                        {
                            "original": {
                                "containerWidth": 661,
                                "containerHeight": 519,
                                "url": "//avatars.mdst.yandex.net/get-mpic/1338/test100/orig",
                                "width": 661,
                                "height": 519,
                            },
                            "thumbnails": [
                                {"url": "//avatars.mdst.yandex.net/get-mpic/1338/test100/1hq"},
                                {"url": "//avatars.mdst.yandex.net/get-mpic/1338/test100/2hq"},
                                {"url": "//avatars.mdst.yandex.net/get-mpic/1338/test100/3hq"},
                                {"url": "//avatars.mdst.yandex.net/get-mpic/1338/test100/4hq"},
                                {"url": "//avatars.mdst.yandex.net/get-mpic/1338/test100/5hq"},
                                {"url": "//avatars.mdst.yandex.net/get-mpic/1338/test100/6hq"},
                                {"url": "//avatars.mdst.yandex.net/get-mpic/1338/test100/7hq"},
                                {"url": "//avatars.mdst.yandex.net/get-mpic/1338/test100/8hq"},
                                {"url": "//avatars.mdst.yandex.net/get-mpic/1338/test100/9hq"},
                            ],
                        },
                    ]
                }
            ]
        }

        for q in ('place=prime&text=avatars+groupmodel', 'place=modelinfo&hyperid=1003&rids=225'):
            self.assertFragmentIn(self.report.request_json(q), expected_group_model_fragment)

        for q in ('place=prime&text=avatars+model', 'place=modelinfo&hyperid=1002&rids=225'):
            self.assertFragmentIn(self.report.request_json(q), expected_model_fragment)

    def test_avatars_thumbnails_original_ratio_thumbs(self):
        def thumbnails19(group):
            return [
                {"url": "//avatars.mdst.yandex.net/get-mpic/{}/test100/{}hq".format(group, i + 1)} for i in range(0, 9)
            ]

        expected_group_model_fragment = {
            "results": [
                {
                    "pictures": [
                        {
                            "original": {
                                "containerWidth": 100,
                                "containerHeight": 200,
                                "url": "//avatars.mdst.yandex.net/get-mpic/4139/test100/orig",
                                "width": 100,
                                "height": 200,
                            },
                            "thumbnails": thumbnails19(4139)
                            + [
                                {
                                    "containerWidth": 250,
                                    "containerHeight": 500,
                                    "url": "//avatars.mdst.yandex.net/get-mpic/4139/test100/11hq",
                                    "width": 250,
                                    "height": 500,
                                },
                                {
                                    "containerWidth": 500,
                                    "containerHeight": 1000,
                                    "url": "//avatars.mdst.yandex.net/get-mpic/4139/test100/12hq",
                                    "width": 500,
                                    "height": 1000,
                                },
                                {
                                    "containerWidth": 700,
                                    "containerHeight": 1400,
                                    "url": "//avatars.mdst.yandex.net/get-mpic/4139/test100/13hq",
                                    "width": 700,
                                    "height": 1400,
                                },
                                {
                                    "containerWidth": 1200,
                                    "containerHeight": 2400,
                                    "url": "//avatars.mdst.yandex.net/get-mpic/4139/test100/14hq",
                                    "width": 1200,
                                    "height": 2400,
                                },
                            ],
                        },
                        {
                            "original": {
                                "containerWidth": 661,
                                "containerHeight": 519,
                                "url": "//avatars.mdst.yandex.net/get-mpic/1339/test100/orig",
                                "width": 661,
                                "height": 519,
                            },
                            "thumbnails": thumbnails19(1339)
                            + [
                                {
                                    "containerWidth": 250,
                                    "containerHeight": 196,
                                    "url": "//avatars.mdst.yandex.net/get-mpic/1339/test100/11hq",
                                    "width": 250,
                                    "height": 196,
                                },
                                {
                                    "containerWidth": 500,
                                    "containerHeight": 392,
                                    "url": "//avatars.mdst.yandex.net/get-mpic/1339/test100/12hq",
                                    "width": 500,
                                    "height": 392,
                                },
                                {
                                    "containerWidth": 700,
                                    "containerHeight": 549,
                                    "url": "//avatars.mdst.yandex.net/get-mpic/1339/test100/13hq",
                                    "width": 700,
                                    "height": 549,
                                },
                                {
                                    "containerWidth": 1200,
                                    "containerHeight": 942,
                                    "url": "//avatars.mdst.yandex.net/get-mpic/1339/test100/14hq",
                                    "width": 1200,
                                    "height": 942,
                                },
                            ],
                        },
                    ]
                }
            ]
        }
        expected_model_fragment = {
            "results": [
                {
                    "pictures": [
                        {
                            "original": {
                                "containerWidth": 100,
                                "containerHeight": 200,
                                "url": "//avatars.mdst.yandex.net/get-mpic/4138/test100/orig",
                                "width": 100,
                                "height": 200,
                            },
                            "thumbnails": thumbnails19(4138)
                            + [
                                {
                                    "containerWidth": 250,
                                    "containerHeight": 500,
                                    "url": "//avatars.mdst.yandex.net/get-mpic/4138/test100/11hq",
                                    "width": 250,
                                    "height": 500,
                                },
                                {
                                    "containerWidth": 500,
                                    "containerHeight": 1000,
                                    "url": "//avatars.mdst.yandex.net/get-mpic/4138/test100/12hq",
                                    "width": 500,
                                    "height": 1000,
                                },
                                {
                                    "containerWidth": 700,
                                    "containerHeight": 1400,
                                    "url": "//avatars.mdst.yandex.net/get-mpic/4138/test100/13hq",
                                    "width": 700,
                                    "height": 1400,
                                },
                                {
                                    "containerWidth": 1200,
                                    "containerHeight": 2400,
                                    "url": "//avatars.mdst.yandex.net/get-mpic/4138/test100/14hq",
                                    "width": 1200,
                                    "height": 2400,
                                },
                            ],
                        },
                        {
                            "original": {
                                "containerWidth": 661,
                                "containerHeight": 519,
                                "url": "//avatars.mdst.yandex.net/get-mpic/1338/test100/orig",
                                "width": 661,
                                "height": 519,
                            },
                            "thumbnails": thumbnails19(1338)
                            + [
                                {
                                    "containerWidth": 250,
                                    "containerHeight": 196,
                                    "url": "//avatars.mdst.yandex.net/get-mpic/1338/test100/11hq",
                                    "width": 250,
                                    "height": 196,
                                },
                                {
                                    "containerWidth": 500,
                                    "containerHeight": 392,
                                    "url": "//avatars.mdst.yandex.net/get-mpic/1338/test100/12hq",
                                    "width": 500,
                                    "height": 392,
                                },
                                {
                                    "containerWidth": 700,
                                    "containerHeight": 549,
                                    "url": "//avatars.mdst.yandex.net/get-mpic/1338/test100/13hq",
                                    "width": 700,
                                    "height": 549,
                                },
                                {
                                    "containerWidth": 1200,
                                    "containerHeight": 942,
                                    "url": "//avatars.mdst.yandex.net/get-mpic/1338/test100/14hq",
                                    "width": 1200,
                                    "height": 942,
                                },
                            ],
                        },
                    ]
                }
            ]
        }

        for q in (
            'place=prime&text=avatars+groupmodel&add-original-ratio-thumbs=1',
            'place=modelinfo&hyperid=1003&rids=225&add-original-ratio-thumbs=1',
        ):
            self.assertFragmentIn(self.report.request_json(q), expected_group_model_fragment)

        for q in (
            'place=prime&text=avatars+model&add-original-ratio-thumbs=1',
            'place=modelinfo&hyperid=1002&rids=225&add-original-ratio-thumbs=1',
        ):
            self.assertFragmentIn(self.report.request_json(q), expected_model_fragment)

    @classmethod
    def prepare_original_thumb_data(cls):
        '''Создаем модель, кластер и офер с картинками'''
        cls.index.models += [
            Model(title='pic', vendor_id=1, hid=101, hyperid=1004),
        ]

        pic = Picture(
            picture_id="KdwwrYb4czANgt9-3poEQQ",
            width=500,
            height=600,
            thumb_mask=thumbnails_config.get_mask_by_names(['1x1', '100x100']),
            group_id=1234,
        )

        avapic = Picture(
            picture_id="WQMdunc5xp5CQSvqdyP0pQ",
            width=500,
            height=600,
            thumb_mask=thumbnails_config.get_mask_by_names(['1x1', '100x100']),
            group_id=1234,
        )

        avapic2 = Picture(
            picture_id="ZO4NzuDl2m4liGAb9dNXWg",
            width=500,
            height=600,
            thumb_mask=thumbnails_config.get_mask_by_names(['1x1', '100x100']),
            group_id=1234,
        )

        # Make sure that thumb_mask does not make any difference anymore
        avapic3 = Picture(
            picture_id="bmp-7jhzSPmqlvkv-WKl-A", width=250, height=250, thumb_mask=4611686018427396095, group_id=805400
        )

        cls.index.vclusters += [
            VCluster(title='pic', vendor_id=1, hid=102, vclusterid=1000000001, pictures=[pic]),
            VCluster(title='pic', vendor_id=1, hid=102, vclusterid=1000000002, pictures=[avapic, avapic2, avapic3]),
        ]
        cls.index.offers += [
            Offer(hyperid=1004, hid=101),
            Offer(vclusterid=1000000001, hid=102),
            Offer(vclusterid=1000000002, hid=102),
            Offer(title='pic', vendor_id=1, hyperid=1005, hid=103, picture=pic),
        ]

    def test_original_thumb(self):
        '''place={prime, top_categories, productoffers}
        Проверяем, что для модели, кластера и офера
        присутствует оригинальная картинка с размерами'''

        model_pics = [
            {
                "original": {
                    "containerWidth": 100,
                    "containerHeight": 200,
                    "url": Model.DEFAULT_PIC_URL,
                    "width": 100,
                    "height": 200,
                },
                "thumbnails": [],
            },
            {
                "original": {
                    "containerWidth": 661,
                    "containerHeight": 519,
                    "url": Model.DEFAULT_ADD_PIC_URL,
                    "width": 661,
                    "height": 519,
                },
                "thumbnails": [],
            },
        ]
        cluster_pics = [
            {
                "original": {
                    "containerWidth": 500,
                    "containerHeight": 600,
                    "url": "http://avatars.mdst.yandex.net/get-marketpic/1234/market_KdwwrYb4czANgt9-3poEQQ/orig",
                    "width": 500,
                    "height": 600,
                },
                "thumbnails": [
                    {
                        "containerWidth": 100,
                        "containerHeight": 100,
                        "url": "http://avatars.mdst.yandex.net/get-marketpic/1234/market_KdwwrYb4czANgt9-3poEQQ/100x100",
                        "width": 83,
                        "height": 100,
                    }
                ],
            },
        ]
        ava_cluster_pics = [
            {
                "original": {
                    "containerWidth": 500,
                    "containerHeight": 600,
                    "url": "http://avatars.mdst.yandex.net/get-marketpic/1234/market_WQMdunc5xp5CQSvqdyP0pQ/orig",
                    "width": 500,
                    "height": 600,
                },
                "thumbnails": [
                    {
                        "containerWidth": 100,
                        "containerHeight": 100,
                        "url": "http://avatars.mdst.yandex.net/get-marketpic/1234/market_WQMdunc5xp5CQSvqdyP0pQ/100x100",
                        "width": 83,
                        "height": 100,
                    }
                ],
            },
            {
                "original": {
                    "containerWidth": 500,
                    "containerHeight": 600,
                    "url": "http://avatars.mdst.yandex.net/get-marketpic/1234/market_ZO4NzuDl2m4liGAb9dNXWg/orig",
                    "width": 500,
                    "height": 600,
                },
                "thumbnails": [
                    {
                        "containerWidth": 100,
                        "containerHeight": 100,
                        "url": "http://avatars.mdst.yandex.net/get-marketpic/1234/market_ZO4NzuDl2m4liGAb9dNXWg/100x100",
                        "width": 83,
                        "height": 100,
                    }
                ],
            },
            {
                "original": {
                    "containerWidth": 250,
                    "containerHeight": 250,
                    "url": "http://avatars.mdst.yandex.net/get-marketpic/805400/market_bmp-7jhzSPmqlvkv-WKl-A/orig",
                    "width": 250,
                    "height": 250,
                },
                "thumbnails": [
                    {
                        "containerWidth": 100,
                        "containerHeight": 100,
                        "url": "http://avatars.mdst.yandex.net/get-marketpic/805400/market_bmp-7jhzSPmqlvkv-WKl-A/100x100",
                        "width": 100,
                        "height": 100,
                    }
                ],
            },
        ]
        offer_pics = [
            {
                "original": {
                    "containerWidth": 500,
                    "containerHeight": 600,
                    "url": "http://avatars.mdst.yandex.net/get-marketpic/1234/market_KdwwrYb4czANgt9-3poEQQ/orig",
                    "width": 500,
                    "height": 600,
                },
                "thumbnails": [
                    {
                        "containerWidth": 100,
                        "containerHeight": 100,
                        "url": "http://avatars.mdst.yandex.net/get-marketpic/1234/market_KdwwrYb4czANgt9-3poEQQ/100x100",
                        "width": 83,
                        "height": 100,
                    }
                ],
            }
        ]

        response = self.report.request_json('place=prime&text=pic')
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "product",
                    "id": 1004,
                    "pictures": model_pics,
                },
                {
                    "entity": "product",
                    "id": 1000000001,
                    "pictures": cluster_pics,
                },
                {
                    "entity": "product",
                    "id": 1000000002,
                    "pictures": ava_cluster_pics,
                },
                {
                    "entity": "offer",
                    "pictures": offer_pics,
                },
            ],
        )

        response = self.report.request_json('place=top_categories&vendor_id=1&numdoc=10')
        self.assertFragmentIn(
            response,
            [
                {
                    "link": {"params": {"hid": "101"}},
                    "icons": model_pics[:1],
                },
                {
                    "link": {"params": {"hid": "102"}},
                    "icons": ava_cluster_pics,
                },
                {
                    "link": {"params": {"hid": "103"}},
                    "icons": offer_pics[:1],
                },
            ],
        )

        response = self.report.request_json('place=productoffers&hyperid=1005')
        self.assertFragmentIn(
            response,
            [
                {
                    "pictures": offer_pics,
                }
            ],
        )

    @classmethod
    def prepare_avatar_universal_picture(cls):
        '''Создаем офферв с универсальной картинкой'''

        pic = Picture(
            imagename='img_id1234567890.jpeg',
            namespace='mbo',
            width=500,
            height=600,
            thumb_mask=thumbnails_config.get_mask_by_names(['1x1', '100x100']),
            group_id=1234,
        )

        cls.index.offers += [Offer(title='pic', vendor_id=1, hyperid=1005, hid=103, picture=pic)]

    def test_avatar_universal_picture(self):
        offer_pics = [
            {
                "original": {
                    "containerWidth": 500,
                    "containerHeight": 600,
                    "url": "http://avatars.mdst.yandex.net/get-mbo/1234/img_id1234567890.jpeg/orig",
                    "width": 500,
                    "height": 600,
                },
                "thumbnails": [
                    {
                        "containerWidth": 100,
                        "containerHeight": 100,
                        "url": "http://avatars.mdst.yandex.net/get-mbo/1234/img_id1234567890.jpeg/100x100",
                        "width": 83,
                        "height": 100,
                    }
                ],
            }
        ]

        response = self.report.request_json('place=prime&text=pic')
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "pictures": offer_pics,
                },
            ],
        )

        response = self.report.request_json('place=productoffers&hyperid=1005')
        self.assertFragmentIn(
            response,
            [
                {
                    "pictures": offer_pics,
                }
            ],
        )

    @classmethod
    def prepare_avatar_offer_picture_data(cls):
        '''Создаем офер с новой аватарной картинкой'''

        pic = Picture(
            picture_id="KdwwrYb4czANgt9-3poEQQ",
            width=500,
            height=600,
            thumb_mask=thumbnails_config.get_mask_by_names(['1x1', '100x100']),
            group_id=1234,
        )

        cls.index.offers += [Offer(title='pic', vendor_id=1, hyperid=1005, hid=103, picture=pic)]

    def test_avatar_offer_picture(self):
        '''place={prime, productoffers}
        Проверяем, что для офера поддерживается генерация картинок на аватарницу'''

        offer_pics = [
            {
                "original": {
                    "containerWidth": 500,
                    "containerHeight": 600,
                    "url": "http://avatars.mdst.yandex.net/get-marketpic/1234/market_KdwwrYb4czANgt9-3poEQQ/orig",
                    "width": 500,
                    "height": 600,
                },
                "thumbnails": [
                    {
                        "containerWidth": 100,
                        "containerHeight": 100,
                        "url": "http://avatars.mdst.yandex.net/get-marketpic/1234/market_KdwwrYb4czANgt9-3poEQQ/100x100",
                        "width": 83,
                        "height": 100,
                    }
                ],
            }
        ]

        response = self.report.request_json('place=prime&text=pic')
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "pictures": offer_pics,
                },
            ],
        )

        response = self.report.request_json('place=productoffers&hyperid=1005')
        self.assertFragmentIn(
            response,
            [
                {
                    "pictures": offer_pics,
                }
            ],
        )

    @classmethod
    def prepare_thumb_bad_size_data(cls):
        '''Создаем 2 документа с картинками: в одной нормальные размеры,
        в другой - битые'''
        cls.index.models += [
            Model(
                title='bad_pic_data',
                hyperid=1006,
                picinfo='//mdata.yandex.net/i?path=b0130135356_img_id2520674011472212068.jpg#100#200',
                add_picinfo='//mdata.yandex.net/i?path=b0130135324_img_id6600772400047913164.jpg#661#519',
            ),
            Model(
                title='bad_pic_data',
                hyperid=1007,
                picinfo='//mdata.yandex.net/i?path=b0130135356_img_id2520674011472212068.jpg#100',
                add_picinfo='//mdata.yandex.net/i?path=b0130135324_img_id6600772400047913164.jpg#661#abc',
            ),
        ]

    def test_thumb_bad_size(self):
        '''Проверяем, что для модели с битыми размерами картинки
        не выводится поле origin'''
        response = self.report.request_json('place=prime&text=bad_pic_data')
        self.assertFragmentIn(
            response,
            [
                {
                    "id": 1006,
                    "pictures": [
                        {"original": {}},
                    ],
                },
                {
                    "id": 1007,
                    "pictures": [
                        {"original": NoKey("original")},
                    ],
                },
            ],
        )

    @classmethod
    def prepare_test_main_or_additional_pictures_choice(cls):
        """
        Создаем 5 моделей:
         1. Есть и главная и дополнительная картинка (разные)
         2. Есть и главная и дополнительная картинка (одинаковые)
         3. Есть только дополнительная
         4. Есть только главная
         5. Нет картинок вообще
        """
        cls.index.models += [
            Model(hyperid=2000),
            Model(
                hyperid=2001,
                picinfo='//avatars.mds.yandex.net/get-mpic/1622123/img_id8062604474619834731/orig#200#200',
                add_picinfo='//avatars.mds.yandex.net/get-mpic/1622123/img_id8062604474619834731/orig#200#200',
            ),
            Model(hyperid=2002, no_picture=True),
            Model(hyperid=2003, no_add_picture=True),
            Model(hyperid=2004, no_picture=True, no_add_picture=True),
        ]

        # То же самое для групповых моделей
        cls.index.model_groups += [
            ModelGroup(hyperid=2100),
            ModelGroup(
                hyperid=2101,
                picinfo='//avatars.mds.yandex.net/get-mpic/1622121/img_id8062604474619834731/orig#200#200',
                add_picinfo='//avatars.mds.yandex.net/get-mpic/1622121/img_id8062604474619834731/orig#200#200',
            ),
            ModelGroup(hyperid=2102, picinfo=''),
            ModelGroup(hyperid=2103, add_picinfo=''),
            ModelGroup(hyperid=2104, picinfo='', add_picinfo=''),
        ]

        pic_avatar = Picture(
            picture_id="R7U-OyHca7pFegFti5_p5g",
            width=500,
            height=600,
            thumb_mask=thumbnails_config.get_mask_by_names(['1x1', '100x100', '200x200', '300x300']),
            group_id=1234,
        )

        cls.index.books += [
            # Создаем книгу без картинки и с главной картинкой. Дополнительных картинок у книг нет
            Book(hyperid=2200, picinfo=''),
            Book(
                hyperid=2201,
                picinfo='//avatars.mdst.yandex.net/get-marketpic/1234/market_wlcJgLkH7us9tdsCSFyd5Q/orig#100#200',
            ),
            # и книжку с новой картинкой, которая лежит в аватарнице
            Book(hyperid=2203, picture=pic_avatar),
            # и книжку с картинкой, пришедшей от MBO в модельном формате
            Book(hyperid=2204, proto_picture=to_mbo_picture(Book.DEFAULT_PIC_URL + '#100#200')),
        ]

    def test_model_pictures_choice(self):
        """
        Проверяем рендеринг картинок для моделей в каждом из 5-и случаев (см. prepare)
        """

        default_pic_info = {
            "original": {
                "url": Model.DEFAULT_PIC_URL,
                "width": 100,
                "height": 200,
            }
        }

        default_add_pic_info = {
            "original": {
                "url": Model.DEFAULT_ADD_PIC_URL,
                "width": 661,
                "height": 519,
            }
        }

        # у модели с разными главной и дополнительной картинкой, берем обе
        expected = {"pictures": [default_pic_info, default_add_pic_info]}
        response = self.report.request_json('place=modelinfo&hyperid=2000&rids=0')
        self.assertFragmentIn(response, expected, allow_different_len=False)

        # у модели с одинаковыми главной и дополнительной картинкой, берем лишь одну из них
        expected = {
            "pictures": [
                {
                    "original": {
                        "url": "//avatars.mds.yandex.net/get-mpic/1622123/img_id8062604474619834731/orig",
                        "width": 200,
                        "height": 200,
                    }
                }
            ]
        }
        response = self.report.request_json('place=modelinfo&hyperid=2001&rids=0')
        self.assertFragmentIn(response, expected, allow_different_len=False)

        # у моделей без главной картинки возвращаем только дополнительные
        expected = {"pictures": [default_add_pic_info]}
        response = self.report.request_json('place=modelinfo&hyperid=2002&rids=0')
        self.assertFragmentIn(response, expected, allow_different_len=False)

        # у модели без дополнительной картинки берем основную
        expected = {"pictures": [default_pic_info]}
        response = self.report.request_json('place=modelinfo&hyperid=2003&rids=0')
        self.assertFragmentIn(response, expected, allow_different_len=False)

        # если никаких картинок нет, то нечего отображать
        response = self.report.request_json('place=modelinfo&hyperid=2004&rids=0')
        self.assertFragmentIn(response, {'pictures': Absent()}, allow_different_len=False)

    def test_model_group_pictures_choice(self):
        """
        Проверяем рендеринг картинок для групповых моделей в каждом из 4-х случаев (см. prepare)
        """

        default_pic_info = {
            "original": {
                "url": ModelGroup.DEFAULT_PIC_URL,
                "width": 100,
                "height": 200,
            }
        }

        default_add_pic_info = {
            "original": {
                "url": ModelGroup.DEFAULT_ADD_PIC_URL,
                "width": 661,
                "height": 519,
            }
        }

        # у групповых моделей с разными главной и дополнительной, берем обе
        expected = {"pictures": [default_pic_info, default_add_pic_info]}
        response = self.report.request_json('place=modelinfo&hyperid=2100&rids=0')
        self.assertFragmentIn(response, expected, allow_different_len=False)

        # у групповых моделей с одинаковой главной и дополнительной, берем одну из них
        expected = {
            "pictures": [
                {
                    "original": {
                        "url": "//avatars.mds.yandex.net/get-mpic/1622121/img_id8062604474619834731/orig",
                        "width": 200,
                        "height": 200,
                    }
                }
            ]
        }
        response = self.report.request_json('place=modelinfo&hyperid=2101&rids=0')
        self.assertFragmentIn(response, expected, allow_different_len=False)

        # у групповых моделей без главной картинки берем дополнительную
        expected = {"pictures": [default_add_pic_info]}
        response = self.report.request_json('place=modelinfo&hyperid=2102&rids=0')
        self.assertFragmentIn(response, expected, allow_different_len=False)

        # у групповых моделей без дополнительной картинки, берем главную
        expected = {"pictures": [default_pic_info]}
        response = self.report.request_json('place=modelinfo&hyperid=2103&rids=0')
        self.assertFragmentIn(response, expected, allow_different_len=False)

        # если никаких картинок нет, то нечего отображать
        response = self.report.request_json('place=modelinfo&hyperid=2104&rids=0')
        self.assertFragmentIn(response, {'pictures': Absent()}, allow_different_len=False)

    def test_book_pictures_choice(self):
        # Проверяем рендеринг книг в каждом случае (см. prepare)

        # у книг без картинок нет картинок
        response = self.report.request_json('place=modelinfo&hyperid=2200&rids=0')
        self.assertFragmentIn(response, {'pictures': Absent()}, allow_different_len=False)

        # у книг с картинкой есть картинка
        response = self.report.request_json('place=modelinfo&hyperid=2201&rids=0')
        self.assertFragmentIn(
            response,
            {
                'pictures': [
                    {
                        "original": {
                            "url": "//avatars.mdst.yandex.net/get-marketpic/1234/market_wlcJgLkH7us9tdsCSFyd5Q/orig",
                            "width": 100,
                            "height": 200,
                        }
                    }
                ]
            },
            allow_different_len=False,
        )

        # У книг с новой картинкой с group_id урлы ведут в аватарницу
        response = self.report.request_json('place=modelinfo&hyperid=2203&rids=0')
        self.assertFragmentIn(
            response,
            {
                'pictures': [
                    {
                        "original": {
                            "url": "http://avatars.mdst.yandex.net/get-marketpic/1234/market_R7U-OyHca7pFegFti5_p5g/orig",
                            "width": 500,
                            "height": 600,
                        }
                    }
                ]
            },
            allow_different_len=False,
        )

        # У книг с картинкой от MBO в модельном формате есть картинка от MBO
        response = self.report.request_json('place=modelinfo&hyperid=2204&rids=0')
        self.assertFragmentIn(
            response,
            {
                'pictures': [
                    {
                        "original": {
                            "url": Book.DEFAULT_PIC_URL,
                            "width": 100,
                            "height": 200,
                        }
                    }
                ]
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_trimmed_thumbnails(cls):
        """Создаем четыре категории разных типов
        Создаем офферы в этих категориях с картинками
        Создаем модель и кластер
        """
        cls.index.hypertree += [
            HyperCategory(
                hid=1674100,
                output_type=HyperCategoryType.SIMPLE,
                children=[
                    HyperCategory(hid=1674101, output_type=HyperCategoryType.SIMPLE),
                    HyperCategory(hid=1674102, output_type=HyperCategoryType.CLUSTERS, visual=True),
                    HyperCategory(hid=1674103, output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=1674104, output_type=HyperCategoryType.GURULIGHT),
                ],
            ),
        ]

        cls.index.models += [
            Model(
                hyperid=1674110,
                hid=1674103,
                picinfo='//avatars.mdst.yandex.net/get-mpic/209514/iphone.jpg#640#480',
                add_picinfo='//avatars.mdst.yandex.net/get-mpic/209524/iphone_back.jpg#640#480',
            ),
        ]

        cls.index.offers += [
            Offer(
                title="guru offer WITH picture",
                hyperid=1674110,
                picture=Picture(picture_id='GVKmloXC2JIjtou3C0DOHg', width=525, height=420, group_id=167410311),
                hid=1674103,
                waremd5='dcbFUrHTQQeIHMlbxl7KBQ',
            ),
            Offer(
                title="simple offer WITH picture",
                picture=Picture(picture_id='AQJnjRkieZz5Eis_EDl18Q', width=525, height=420, group_id=167410111),
                hid=1674101,
                waremd5='Zhn6--NDU5ZeokYwhVD6-A',
            ),
            Offer(
                title="visual offer WITH picture",
                vclusterid=1674102001,
                picture=Picture(picture_id='IWn9efjIWifefpJ_WIN35Q', width=525, height=420, group_id=167410211),
                hid=1674102,
                waremd5='ypy_qQL_Bdj2icMR6MyRSg',
            ),
            Offer(
                title="non-guru offer WITH picture",
                picture=Picture(picture_id='Nin3dIIHBidheII_HBIE3w', width=525, height=420, group_id=167410411),
                hid=1674104,
                waremd5='_Tsw9QgmHkrneymgl4Ba8A',
            ),
        ]

        cls.index.vclusters += [
            VCluster(
                vclusterid=1674102001,
                hid=1674102,
                pictures=[Picture(picture_id='IWn9efjIWifefpJ_WIN35Q', width=525, height=420, group_id=167410211)],
            ),
        ]

        cls.index.books += [
            Book(
                hyperid=1674111,
                hid=1674100,
                picture=Picture(picture_id='yo-yWPr-hIszpbw3Y62E2w', width=525, height=420, group_id=167410011),
            ),
        ]

    def check_entity_trimmed_thumb(self, response, entity_type, group_id, picture_id):
        """Универсальная проверка формата вывода картинки в коллекции marketpic"""
        base_url = "http://avatars.mdst.yandex.net/get-marketpic/{}/market_{}".format(group_id, picture_id)
        self.assertFragmentIn(
            response,
            {
                "entity": entity_type,
                "pictures": [
                    {
                        "entity": "picture",
                        "original": {"url": base_url + "/orig", "width": 525, "height": 420},
                        "thumbnails": [
                            {
                                "url": base_url + "/x124_trim",
                                "width": 166,
                                "height": 124,
                            },
                            {
                                "url": base_url + "/x166_trim",
                                "width": 248,
                                "height": 166,
                            },
                            {
                                "url": base_url + "/x248_trim",
                                "width": 332,
                                "height": 248,
                            },
                            {
                                "url": base_url + "/x332_trim",
                                "width": 496,
                                "height": 332,
                            },
                        ],
                    }
                ],
            },
            allow_different_len=False,
        )

    def check_simple_offer_trimmed_thumb(self, response):
        self.check_entity_trimmed_thumb(response, "offer", 167410111, "AQJnjRkieZz5Eis_EDl18Q")

    def check_visual_offer_trimmed_thumb(self, response):
        self.check_entity_trimmed_thumb(response, "offer", 167410211, "IWn9efjIWifefpJ_WIN35Q")

    def check_guru_offer_trimmed_thumb(self, response):
        self.check_entity_trimmed_thumb(response, "offer", 167410311, "GVKmloXC2JIjtou3C0DOHg")

    def check_gurulight_offer_trimmed_thumb(self, response):
        self.check_entity_trimmed_thumb(response, "offer", 167410411, "Nin3dIIHBidheII_HBIE3w")

    def check_vcluster_trimmed_thumb(self, response):
        self.check_entity_trimmed_thumb(response, "product", 167410211, "IWn9efjIWifefpJ_WIN35Q")

    def check_book_trimmed_thumb(self, response):
        self.check_entity_trimmed_thumb(response, "product", 167410011, "yo-yWPr-hIszpbw3Y62E2w")

    def check_model_trimmed_thumb(self, response):
        """Особая проверка для гуру-моделей: картинки хранятся в коллекции
        mpic, но ее настройки в проде не отличаются от marketpic для урезанных
        тумбов
        """
        pic_base_url = "//avatars.mdst.yandex.net/get-mpic/209514"
        add_pic_base_url = "//avatars.mdst.yandex.net/get-mpic/209524"
        self.assertFragmentIn(
            response,
            {
                "entity": "product",
                "pictures": [
                    {
                        "entity": "picture",
                        "original": {"url": pic_base_url + "/iphone.jpg", "width": 640, "height": 480},
                        "thumbnails": [
                            {
                                "url": pic_base_url + "/x124_trim",
                                "width": 166,
                                "height": 124,
                            },
                            {
                                "url": pic_base_url + "/x166_trim",
                                "width": 248,
                                "height": 166,
                            },
                            {
                                "url": pic_base_url + "/x248_trim",
                                "width": 332,
                                "height": 248,
                            },
                            {
                                "url": pic_base_url + "/x332_trim",
                                "width": 496,
                                "height": 332,
                            },
                        ],
                    },
                    {
                        "entity": "picture",
                        "original": {"url": add_pic_base_url + "/iphone_back.jpg", "width": 640, "height": 480},
                        "thumbnails": [
                            {
                                "url": add_pic_base_url + "/x124_trim",
                                "width": 166,
                                "height": 124,
                            },
                            {
                                "url": add_pic_base_url + "/x166_trim",
                                "width": 248,
                                "height": 166,
                            },
                            {
                                "url": add_pic_base_url + "/x248_trim",
                                "width": 332,
                                "height": 248,
                            },
                            {
                                "url": add_pic_base_url + "/x332_trim",
                                "width": 496,
                                "height": 332,
                            },
                        ],
                    },
                ],
            },
            allow_different_len=False,
        )

    def test_prime_trimmed_thumbs(self):
        """Проверяем вывод урезанных тумбов картинок на плейсе prime для категорий разных типов"""
        response = self.report.request_json('place=prime&hid=1674100&trim-thumbs=1')
        self.check_simple_offer_trimmed_thumb(response)
        self.check_visual_offer_trimmed_thumb(response)
        self.check_guru_offer_trimmed_thumb(response)
        self.check_gurulight_offer_trimmed_thumb(response)
        self.check_model_trimmed_thumb(response)
        self.check_vcluster_trimmed_thumb(response)
        self.check_book_trimmed_thumb(response)

    def test_modelinfo_trimmed_thumbs(self):
        """Проверяем вывод урезанных тумбов картинок на плейсе modelinfo для категорий разных типов"""
        response = self.report.request_json('place=modelinfo&hyperid=1674110&trim-thumbs=1&rids=0')
        self.check_model_trimmed_thumb(response)

        response = self.report.request_json('place=modelinfo&hyperid=1674102001&trim-thumbs=1&rids=0')
        self.check_vcluster_trimmed_thumb(response)

        response = self.report.request_json('place=modelinfo&hyperid=1674111&trim-thumbs=1&rids=0')
        self.check_book_trimmed_thumb(response)

    def test_productoffers_trimmed_thumbs(self):
        """Проверяем вывод урезанных тумбов картинок на плейсе productoffers для категорий разных типов"""
        response = self.report.request_json('place=productoffers&hyperid=1674110&trim-thumbs=1&rids=0')
        self.check_guru_offer_trimmed_thumb(response)

        response = self.report.request_json('place=productoffers&hyperid=1674102001&trim-thumbs=1&rids=0')
        self.check_visual_offer_trimmed_thumb(response)

    def test_offerinfo_trimmed_thumbs(self):
        """Проверяем вывод урезанных тумбов картинок на плейсе offerinfo для категорий разных типов"""
        response = self.report.request_json(
            'place=offerinfo&offerid=dcbFUrHTQQeIHMlbxl7KBQ&trim-thumbs=1&rids=0&regset=2'
        )
        self.check_guru_offer_trimmed_thumb(response)

        response = self.report.request_json(
            'place=offerinfo&offerid=ypy_qQL_Bdj2icMR6MyRSg&trim-thumbs=1&rids=0&regset=2'
        )
        self.check_visual_offer_trimmed_thumb(response)

        response = self.report.request_json(
            'place=offerinfo&offerid=Zhn6--NDU5ZeokYwhVD6-A&trim-thumbs=1&rids=0&regset=2'
        )
        self.check_simple_offer_trimmed_thumb(response)

        response = self.report.request_json(
            'place=offerinfo&offerid=_Tsw9QgmHkrneymgl4Ba8A&trim-thumbs=1&rids=0&regset=2'
        )
        self.check_gurulight_offer_trimmed_thumb(response)

    def check_entity_pictures_without_thumbs(
        self, response, entity_type, namespace, group_id, picture_id, width=525, height=420
    ):
        self.assertFragmentIn(
            response,
            {
                "entity": entity_type,
                "pictures": [
                    {
                        "entity": "picture",
                        "original": {
                            "width": width,
                            "height": height,
                            "namespace": namespace,
                            "groupId": group_id,
                            "key": picture_id,
                        },
                        "thumbnails": Absent(),
                    }
                ],
            },
        )
        self.assertFragmentIn(response, {"knownThumbnails": []})

    def check_simple_offer_pictures_without_thumbs(self, response):
        self.check_entity_pictures_without_thumbs(
            response, "offer", "marketpic", 167410111, "market_AQJnjRkieZz5Eis_EDl18Q"
        )

    def check_visual_offer_pictures_without_thumbs(self, response):
        self.check_entity_pictures_without_thumbs(
            response, "offer", "marketpic", 167410211, "market_IWn9efjIWifefpJ_WIN35Q"
        )

    def check_guru_offer_pictures_without_thumbs(self, response):
        self.check_entity_pictures_without_thumbs(
            response, "offer", "marketpic", 167410311, "market_GVKmloXC2JIjtou3C0DOHg"
        )

    def check_gurulight_offer_pictures_without_thumbs(self, response):
        self.check_entity_pictures_without_thumbs(
            response, "offer", "marketpic", 167410411, "market_Nin3dIIHBidheII_HBIE3w"
        )

    def check_vcluster_pictures_without_thumbs(self, response):
        self.check_entity_pictures_without_thumbs(
            response, "product", "marketpic", 167410211, "market_IWn9efjIWifefpJ_WIN35Q"
        )

    def check_book_pictures_without_thumbs(self, response):
        self.check_entity_pictures_without_thumbs(
            response, "product", "marketpic", 167410011, "market_yo-yWPr-hIszpbw3Y62E2w"
        )

    def check_model_pictures_without_thumbs(self, response):
        self.check_entity_pictures_without_thumbs(response, "product", "mpic", 209524, "iphone_back.jpg", 640, 480)

    def test_prime_picture_format_without_thumbs(self):
        """Проверяем вывод картинок в формате без тумбов на плейсе prime"""
        response = self.report.request_json('place=prime&hid=1674100&new-picture-format=1')
        self.check_simple_offer_pictures_without_thumbs(response)
        self.check_visual_offer_pictures_without_thumbs(response)
        self.check_guru_offer_pictures_without_thumbs(response)
        self.check_gurulight_offer_pictures_without_thumbs(response)
        self.check_vcluster_pictures_without_thumbs(response)
        self.check_book_pictures_without_thumbs(response)
        self.check_model_pictures_without_thumbs(response)

    def test_modelinfo_picture_format_without_thumbs(self):
        """Проверяем вывод картинок в формате без тумбов на плейсе modelinfo"""
        response = self.report.request_json('place=modelinfo&hyperid=1674110&new-picture-format=1&rids=0')
        self.check_model_pictures_without_thumbs(response)

        response = self.report.request_json('place=modelinfo&hyperid=1674102001&new-picture-format=1&rids=0')
        self.check_vcluster_pictures_without_thumbs(response)

        response = self.report.request_json('place=modelinfo&hyperid=1674111&new-picture-format=1&rids=0')
        self.check_book_pictures_without_thumbs(response)

    def test_productoffers_picture_format_without_thumbs(self):
        """Проверяем вывод картинок в формате без тумбов на плейсе productoffers"""
        response = self.report.request_json('place=productoffers&hyperid=1674110&new-picture-format=1&rids=0')
        self.check_guru_offer_pictures_without_thumbs(response)

        response = self.report.request_json('place=productoffers&hyperid=1674102001&new-picture-format=1&rids=0')
        self.check_visual_offer_pictures_without_thumbs(response)

    def test_offerinfo_picture_format_without_thumbs(self):
        """Проверяем вывод картинок в формате без тумбов на плейсе offerinfo"""
        response = self.report.request_json(
            'place=offerinfo&offerid=dcbFUrHTQQeIHMlbxl7KBQ&new-picture-format=1&rids=0&regset=2'
        )
        self.check_guru_offer_pictures_without_thumbs(response)

        response = self.report.request_json(
            'place=offerinfo&offerid=ypy_qQL_Bdj2icMR6MyRSg&new-picture-format=1&rids=0&regset=2'
        )
        self.check_visual_offer_pictures_without_thumbs(response)

        response = self.report.request_json(
            'place=offerinfo&offerid=Zhn6--NDU5ZeokYwhVD6-A&new-picture-format=1&rids=0&regset=2'
        )
        self.check_simple_offer_pictures_without_thumbs(response)

        response = self.report.request_json(
            'place=offerinfo&offerid=_Tsw9QgmHkrneymgl4Ba8A&new-picture-format=1&rids=0&regset=2'
        )
        self.check_gurulight_offer_pictures_without_thumbs(response)

    def test_known_thumbnails_output(self):
        response = self.report.request_json('place=prime&text=pic&new-picture-format=1')
        self.assertFragmentIn(
            response,
            {
                'knownThumbnails': [
                    {
                        'namespace': 'marketpic',
                        'thumbnails': [
                            {"name": "50x50", "width": 50, "height": 50},
                            {"name": "55x70", "width": 55, "height": 70},
                            {"name": "60x80", "width": 60, "height": 80},
                            {"name": "74x100", "width": 74, "height": 100},
                            {"name": "75x75", "width": 75, "height": 75},
                            {"name": "90x120", "width": 90, "height": 120},
                            {"name": "100x100", "width": 100, "height": 100},
                            {"name": "120x160", "width": 120, "height": 160},
                            {"name": "150x150", "width": 150, "height": 150},
                            {"name": "180x240", "width": 180, "height": 240},
                            {"name": "190x250", "width": 190, "height": 250},
                            {"name": "200x200", "width": 200, "height": 200},
                            {"name": "240x320", "width": 240, "height": 320},
                            {"name": "300x300", "width": 300, "height": 300},
                            {"name": "300x400", "width": 300, "height": 400},
                            {"name": "600x600", "width": 600, "height": 600},
                            {"name": "600x800", "width": 600, "height": 800},
                            {"name": "900x1200", "width": 900, "height": 1200},
                            {"name": "x124_trim", "width": 166, "height": 124},
                            {"name": "x166_trim", "width": 248, "height": 166},
                            {"name": "x248_trim", "width": 332, "height": 248},
                            {"name": "x332_trim", "width": 496, "height": 332},
                        ],
                    },
                    {
                        'namespace': 'mpic',
                        'thumbnails': [
                            {"name": "1hq", "width": 50, "height": 50},
                            {"name": "2hq", "width": 100, "height": 100},
                            {"name": "3hq", "width": 75, "height": 75},
                            {"name": "4hq", "width": 150, "height": 150},
                            {"name": "5hq", "width": 200, "height": 200},
                            {"name": "6hq", "width": 250, "height": 250},
                            {"name": "7hq", "width": 120, "height": 120},
                            {"name": "8hq", "width": 240, "height": 240},
                            {"name": "9hq", "width": 500, "height": 500},
                            {"name": "x124_trim", "width": 166, "height": 124},
                            {"name": "x166_trim", "width": 248, "height": 166},
                            {"name": "x248_trim", "width": 332, "height": 248},
                            {"name": "x332_trim", "width": 496, "height": 332},
                        ],
                    },
                ],
                'results': [],
            },
            preserve_order=True,
        )

        response = self.report.request_json('place=prime&text=pic')
        self.assertFragmentNotIn(response, {'knownThumbnails': []})

    @classmethod
    def prepare_cluster_color(cls):
        """
        Добавляем картинки трех разных цветов
        """
        pic_blue = Picture(picture_id="bE6WA9EK6_WrAR1IyhFq_A", width=500, height=600, group_id=6789, color_id=7925376)
        pic_red = Picture(picture_id="c8PYxnwyqKGP30D5RcQn-Q", width=500, height=600, group_id=6789, color_id=7925352)
        pic_yellow = Picture(
            picture_id="jJ_3m2oV_RJrJpJKU2AJJg", width=500, height=600, group_id=6789, color_id=7925350
        )

        """
        Кластер с картинками разных цветов
        """
        cls.index.vclusters += [
            VCluster(title='dress', hid=7300, vclusterid=1000000010, pictures=[pic_red, pic_yellow, pic_blue]),
        ]

        cls.index.offers += [
            Offer(vclusterid=1000000010, hid=7300),
        ]

    def test_cluster_color(self):
        """
        Проверяем, что есть поле filtersMatching с нужными цветом для каждой картинкой
        """
        response = self.report.request_json('place=prime&text=dress')
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "product",
                    "id": 1000000010,
                    "pictures": [
                        {
                            "color": 7925376,
                            "original": {
                                "containerWidth": 500,
                                "containerHeight": 600,
                                "url": "http://avatars.mdst.yandex.net/get-marketpic/6789/market_bE6WA9EK6_WrAR1IyhFq_A/orig",
                                "width": 500,
                                "height": 600,
                            },
                            "filtersMatching": {"13887626": ["7925376"]},
                        },
                        {
                            "color": 7925352,
                            "original": {
                                "containerWidth": 500,
                                "containerHeight": 600,
                                "url": "http://avatars.mdst.yandex.net/get-marketpic/6789/market_c8PYxnwyqKGP30D5RcQn-Q/orig",
                                "width": 500,
                                "height": 600,
                            },
                            "filtersMatching": {"13887626": ["7925352"]},
                        },
                        {
                            "color": 7925350,
                            "original": {
                                "containerWidth": 500,
                                "containerHeight": 600,
                                "url": "http://avatars.mdst.yandex.net/get-marketpic/6789/market_jJ_3m2oV_RJrJpJKU2AJJg/orig",
                                "width": 500,
                                "height": 600,
                            },
                            "filtersMatching": {"13887626": ["7925350"]},
                        },
                    ],
                },
            ],
        )


if __name__ == '__main__':
    main()
