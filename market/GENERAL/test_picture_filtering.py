#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    GLParam,
    GLType,
    HyperCategory,
    HyperCategoryType,
    MnPlace,
    Model,
    Offer,
    Picture,
    PictureMbo,
    PictureParam,
    Shop,
    VCluster,
    VendorToGlobalColor,
)
from core.testcase import TestCase, main

from core.matcher import NoKey, EmptyList, NotEmptyList


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']
        # MARKETOUT-11490

        cls.index.hypertree += [
            HyperCategory(hid=1, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=2, output_type=HyperCategoryType.CLUSTERS, visual=True),
            HyperCategory(hid=3, output_type=HyperCategoryType.GURULIGHT),
        ]

        cls.index.offers += [
            Offer(title="guru offer WO picture Bottle", hyperid=1004, bid=1000, no_picture=True, hid=1),
            Offer(title="visual offer WO picture Bottle", vclusterid=1000000001, bid=500, no_picture=True, hid=2),
            Offer(title="non-guru offer WO picture Bottle", bid=100, no_picture=True, hid=3),
            Offer(title="guru offer WITH picture Bottle", hyperid=1004, bid=50, hid=1),
            Offer(title="visual offer WITH picture Bottle", vclusterid=1000000001, bid=40, hid=2),
            Offer(title="non-guru offer WITH picture Bottle", bid=30, hid=3),
        ]

        for i in range(1, 7):
            cls.index.shops += [Shop(fesh=i, priority_region=213)]

            ts = 100 + 2 * i
            cls.index.offers += [
                Offer(title="Snow Flakes {}".format(ts), fesh=i, no_picture=True, bid=100 + i * 10, ts=ts),
                Offer(title="Snow Flakes {}".format(ts + 1), fesh=i, bid=10 + i * 10, ts=ts + 1),
            ]
            # оферы без картинкок ранжируем выше
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, ts).respond(ts * 0.001)
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, ts + 1).respond(ts * 0.0001)

    def test_no_experiment(self):
        """
        Check that all offers are shown at search w/o experiments
        """
        response = self.report.request_json('place=prime&text=Bottle')
        self.assertFragmentIn(
            response,
            [
                {"titles": {"raw": "guru offer WO picture Bottle"}},
                {"titles": {"raw": "visual offer WO picture Bottle"}},
                {"titles": {"raw": "non-guru offer WO picture Bottle"}},
                {"titles": {"raw": "guru offer WITH picture Bottle"}},
                {"titles": {"raw": "visual offer WITH picture Bottle"}},
                {"titles": {"raw": "non-guru offer WITH picture Bottle"}},
            ],
            preserve_order=False,
            allow_different_len=False,
        )

    def test_experiment_filter_all_search(self):
        """
        Check that all offers w/o picture are absent in search results
        """
        for req in (
            'place=prime&text=Bottle&rearr-factors=market_hide_all_offers_wo_picture=1',
            'place=prime&text=Bottle&filter-with-picture=1',
        ):
            response = self.report.request_json(req)
            self.assertFragmentIn(
                response,
                [
                    {"titles": {"raw": "guru offer WITH picture Bottle"}},
                    {"titles": {"raw": "visual offer WITH picture Bottle"}},
                    {"titles": {"raw": "non-guru offer WITH picture Bottle"}},
                ],
                preserve_order=False,
                allow_different_len=False,
            )

    def test_experiment_filter_all_card(self):
        """
        Check that all offers w/o picture are absent in cards offers output.
        e.g. check that there is just one offer (2 matched to the card)
        """
        for hyperid, title in [
            (1004, "guru offer WITH picture Bottle"),
            (1000000001, "visual offer WITH picture Bottle"),
        ]:
            response = self.report.request_json(
                'place=productoffers&hyperid={}&rearr-factors=market_hide_all_offers_wo_picture=1'.format(hyperid)
            )
            self.assertFragmentIn(
                response,
                [
                    {"titles": {"raw": title}},
                ],
                preserve_order=False,
                allow_different_len=False,
            )

    def test_experiment_filter_not_matched_only_search(self):
        """
        Check that all offers not matched to model/cluster w/o picture are absent in search results
        """
        response = self.report.request_json(
            'place=prime&text=Bottle&rearr-factors=market_hide_search_not_matched_offers_wo_picture=1'
        )
        self.assertFragmentIn(
            response,
            [
                {"titles": {"raw": "guru offer WO picture Bottle"}},
                {"titles": {"raw": "visual offer WO picture Bottle"}},
                {"titles": {"raw": "guru offer WITH picture Bottle"}},
                {"titles": {"raw": "visual offer WITH picture Bottle"}},
                {"titles": {"raw": "non-guru offer WITH picture Bottle"}},
            ],
            preserve_order=False,
            allow_different_len=False,
        )

    def test_experiment_filter_not_matched_only_card(self):
        """
        Check that all offers w/o picture exist in cards offers output
        e.g. 2 offers are shown
        """
        for hyperid in [1004, 1000000001]:
            response = self.report.request_json(
                'place=productoffers&hyperid={}&rearr-factors=market_hide_search_not_matched_offers_wo_picture=1'.format(
                    hyperid
                )
            )
            self.assertEqual(response.count({"entity": "offer"}), 2)

    def test_experiment_filter_by_picture_parallel(self):
        """
        MARKETOUT-11499
        Filter offers by picture at base search.
        We request 1 offer per shop from base search and then apply picture filter at meta stage.

        1) W/O experiment request parallel offers. Max CPM offer per shop is returned but it has no picture
            12 offers found(accepted) but no offers incut generated as far as all offers filtered out by picture at meta

        2) In experiment request parallel offers. Max CPM offer per shop should be filtered out by absent picture and another is accepted
            6 offers found(accepted) and offers incut generated as far as all offers has picture
        """
        # market_offers_incut_threshold=0.0 нужен, чтобы врезка не отфильтровывалась по топ-4
        # market_offers_wiz_top_offers_threshold=0 нужен, чтобы оферный не отфильтровывался по топ-4
        request = 'place=parallel&text=snow+flakes&rids=213&rearr-factors=market_offers_incut_threshold=0.0;market_offers_wiz_top_offers_threshold=0;'

        response = self.report.request_bs(request)
        self.assertFragmentIn(
            response, {"market_offers_wizard": [{"showcase": {"items": EmptyList()}, "offer_count": 12}]}
        )

        response = self.report.request_bs(request + 'market_hide_offers_wo_picture_prl=1;')
        self.assertFragmentIn(
            response, {"market_offers_wizard": [{"showcase": {"items": NotEmptyList()}, "offer_count": 6}]}
        )

    # MARKETOUT-13238
    @classmethod
    def prepare_dup_pictures(cls):
        pic = Picture(picture_id='KE0MY3zMHwD_P1h0BPPfow', width=200, height=118, group_id=1234)

        cls.index.vclusters += [
            VCluster(
                hid=1323801,
                vclusterid=1001323801,
                pictures=[pic, pic, pic],
            )
        ]

        cls.index.offers += [
            Offer(vclusterid=1001323801),
        ]

    def test_no_dup_pictures(self):
        """
        Проверяем, что картинка в выдаче только одна, а не три

        Здесь мы в MARKETOUT-13238 чиним ТОЛЬКО кейс, когда все картинки одинаковые
        """
        response = self.report.request_json('place=modelinfo&hyperid=1001323801&rids=213')
        self.assertEqual(1, response.count({"entity": "picture"}))

        response = self.report.request_json('place=modelinfo&hyperid=1001323801&rids=213&trim-thumbs=1')
        self.assertEqual(1, response.count({"entity": "picture"}))

        response = self.report.request_json('place=modelinfo&hyperid=1001323801&rids=213&new-picture-format=1')
        self.assertEqual(1, response.count({"entity": "picture"}))

    @classmethod
    def prepare_pictures_with_gl_params(cls):
        """
        Создаем gl-параметры и модель с картинкой, для которой указаны
        значения этих параметров. При этом в значениях картинки есть
        записи с неправильным типом (тип в картинке не соотвествует типу
        в GLType)
        """
        cls.index.gltypes += [
            GLType(param_id=201, hid=4, gltype=GLType.ENUM, values=[1, 2, 3], cluster_filter=True),
            GLType(param_id=202, hid=4, gltype=GLType.NUMERIC, cluster_filter=True),
            GLType(param_id=203, hid=4, gltype=GLType.BOOL, cluster_filter=True),
        ]

        picture_params = [
            PictureParam(param_id=201, type=GLType.ENUM, value=1),
            PictureParam(param_id=201, type=GLType.ENUM, value=3),
            PictureParam(param_id=201, type=GLType.ENUM, value=4),  # Invalid, should be logged
            PictureParam(param_id=201, type=GLType.NUMERIC, value='1.5'),  # Invalid, should be ignored
            PictureParam(param_id=202, type=GLType.NUMERIC, value='1.2'),
            PictureParam(param_id=202, type=GLType.NUMERIC, value='2'),
            PictureParam(param_id=202, type=GLType.NUMERIC, value='2.7'),
            PictureParam(param_id=202, type=GLType.NUMERIC, value=''),  # Invalid, should be logged
            PictureParam(param_id=202, type=GLType.ENUM, value=3),  # Invalid, should be ignored
            PictureParam(param_id=203, type=GLType.BOOL, value=False),
            PictureParam(param_id=203, type=GLType.BOOL, value=True),
            PictureParam(param_id=203, type=GLType.ENUM, value=2),  # Invalid, should be ignored
            PictureParam(param_id=204, type=GLType.ENUM, value=1),  # Invalid, should be ignored
        ]

        cls.index.models += [
            Model(
                title="phone 1",
                hid=4,
                hyperid=101,
                proto_picture=PictureMbo(params=picture_params),
                proto_add_pictures=[PictureMbo(params=picture_params)],
            )
        ]

    def check_picture_format(self, response):
        self.assertFragmentIn(
            response,
            {
                "pictures": [
                    {
                        "entity": "picture",
                        "filtersMatching": {"201": ["1", "3"], "202": [1.2, 2, 2.7], "203": False, "204": NoKey("204")},
                    },
                    {
                        "entity": "picture",
                        "filtersMatching": {"201": ["1", "3"], "202": [1.2, 2, 2.7], "203": False, "204": NoKey("204")},
                    },
                ]
            },
            allow_different_len=False,
        )

    def test_pictures_with_invalid_gl_params(self):
        """
        Что тестируем: формат выдачи параметров картинки гуру-моделей
        Задаем запросы к плейсам prime и modelinfo.
        Проверяем, что на выдаче есть параметры картинок, причем отдельные
        значения "схлопнулись" в массивы (кроме bool), а значений
        с неправильным типом нет
        """
        response = self.report.request_json('place=prime&hid=4')
        self.check_picture_format(response)

        response = self.report.request_json('place=modelinfo&hyperid=101&rids=0')
        self.check_picture_format(response)

        # У каждой из картинок модели 6 неправильных параметров, но запись в лог будет лишь о двух из них для каждой
        # Т.к. 2 запроса и 2 картинки, то записей в лог должно быть 2*2*2 = 8
        self.error_log.expect(code=3601).times(2 * 2 * 2)

    @classmethod
    def prepare_without_main_picture(cls):
        """Создаем модель без главной картинки (PicInfo/ProtoPicInfo)"""
        cls.index.models += [
            Model(
                hid=1622101,
                hyperid=1622111,
                no_picture=True,
                proto_add_pictures=[
                    PictureMbo(
                        url='//avatars.mds.yandex.net/get-mpic/1622121/img_id8062604474619834730/orig',
                        width=200,
                        height=118,
                    ),
                    PictureMbo(
                        url='//avatars.mds.yandex.net/get-mpic/1622122/img_id8062604474619834730/orig',
                        width=400,
                        height=218,
                    ),
                ],
            ),
        ]

    def check_picture_format_without_main_picture(self, response):
        self.assertFragmentIn(
            response,
            {
                "pictures": [
                    {
                        "entity": "picture",
                        "original": {
                            "url": "//avatars.mds.yandex.net/get-mpic/1622121/img_id8062604474619834730/orig",
                            "width": 200,
                            "height": 118,
                        },
                    },
                    {
                        "entity": "picture",
                        "original": {
                            "url": "//avatars.mds.yandex.net/get-mpic/1622122/img_id8062604474619834730/orig",
                            "width": 400,
                            "height": 218,
                        },
                    },
                ]
            },
            allow_different_len=False,
        )

    def test_model_without_main_picture(self):
        """Что тестируем: модель без главной картинки PicInfo (ProtoPicInfo), но с AddPicsInfo (AddProtoPicsInfo)
        есть на выдаче с картинками
        Проверяем плейсы prime и modelinfo
        """
        response = self.report.request_json('place=prime&hid=1622101')
        self.check_picture_format_without_main_picture(response)

        response = self.report.request_json('place=modelinfo&hyperid=1622111&rids=0')
        self.check_picture_format_without_main_picture(response)

        response = self.report.request_json('place=prime&hid=1622101&trim-thumbs=1')
        self.check_picture_format_without_main_picture(response)

        response = self.report.request_json('place=modelinfo&hyperid=1622111&rids=0&trim-thumbs=1')
        self.check_picture_format_without_main_picture(response)

    @classmethod
    def prepare_with_main_picture(cls):
        """
        Создаем модель с главной картинкой PicInfo (ProtoPicInfo), и с AddPicsInfo (AddProtoPicsInfo)
        """
        cls.index.models += [
            Model(
                hid=1622102,
                hyperid=1622112,
                proto_picture=PictureMbo(
                    url='//avatars.mds.yandex.net/get-mpic/1622123/img_id8062604474619834731/orig',
                    width=200,
                    height=200,
                ),
                proto_add_pictures=[
                    PictureMbo(
                        url='//avatars.mds.yandex.net/get-mpic/1622123/img_id8062604474619834732/orig',
                        width=300,
                        height=300,
                    ),
                    PictureMbo(
                        url='//avatars.mds.yandex.net/get-mpic/1622123/img_id8062604474619834733/orig',
                        width=400,
                        height=400,
                    ),
                ],
            ),
        ]

    def check_picture_format_with_main_picture(self, response):
        self.assertFragmentIn(
            response,
            {
                "pictures": [
                    {
                        "entity": "picture",
                        "original": {
                            "url": "//avatars.mds.yandex.net/get-mpic/1622123/img_id8062604474619834731/orig",
                            "width": 200,
                            "height": 200,
                        },
                    },
                    {
                        "entity": "picture",
                        "original": {
                            "url": "//avatars.mds.yandex.net/get-mpic/1622123/img_id8062604474619834732/orig",
                            "width": 300,
                            "height": 300,
                        },
                    },
                    {
                        "entity": "picture",
                        "original": {
                            "url": "//avatars.mds.yandex.net/get-mpic/1622123/img_id8062604474619834733/orig",
                            "width": 400,
                            "height": 400,
                        },
                    },
                ]
            },
            allow_different_len=False,
        )

    def test_model_with_main_picture(self):
        """
        Что тестируем: модель с главной картинкой PicInfo (ProtoPicInfo), и с AddPicsInfo (AddProtoPicsInfo)
        есть на выдаче с картинками
        Проверяем плейсы prime и modelinfo
        """
        response = self.report.request_json('place=prime&hid=1622102')
        self.check_picture_format_with_main_picture(response)

        response = self.report.request_json('place=modelinfo&hyperid=1622112&rids=0')
        self.check_picture_format_with_main_picture(response)

        response = self.report.request_json('place=prime&hid=1622102&trim-thumbs=1')
        self.check_picture_format_with_main_picture(response)

        response = self.report.request_json('place=modelinfo&hyperid=1622112&rids=0&trim-thumbs=1')
        self.check_picture_format_with_main_picture(response)

    @classmethod
    def prepare_with_dup_main_picture(cls):
        """
        Создаем модель с главной картинкой PicInfo (ProtoPicInfo) одинаковой с AddPicsInfo (AddProtoPicsInfo)
        """

        dup_picture = PictureMbo(
            url='//avatars.mds.yandex.net/get-mpic/1622123/img_id8062604474619834732/orig', width=200, height=200
        )

        cls.index.models += [
            Model(
                hid=1622103,
                hyperid=1622113,
                proto_picture=dup_picture,
                proto_add_pictures=[
                    dup_picture,
                    PictureMbo(
                        url='//avatars.mds.yandex.net/get-mpic/1622123/img_id8062604474619834733/orig',
                        width=400,
                        height=400,
                    ),
                ],
            ),
        ]

    def check_picture_format_with_dup_main_picture(self, response):
        self.assertFragmentIn(
            response,
            {
                "pictures": [
                    {
                        "entity": "picture",
                        "original": {
                            "url": "//avatars.mds.yandex.net/get-mpic/1622123/img_id8062604474619834732/orig",
                            "width": 200,
                            "height": 200,
                        },
                    },
                    {
                        "entity": "picture",
                        "original": {
                            "url": "//avatars.mds.yandex.net/get-mpic/1622123/img_id8062604474619834733/orig",
                            "width": 400,
                            "height": 400,
                        },
                    },
                ]
            },
            allow_different_len=False,
        )

    def test_model_with_dup_main_picture(self):
        """
        Что тестируем: модель с главной картинкой PicInfo (ProtoPicInfo) одинаковой с одной из AddPicsInfo
        (AddProtoPicsInfo) есть на выдаче с картинками без дубликатов
        Проверяем плейсы prime и modelinfo
        """
        response = self.report.request_json('place=prime&hid=1622103')
        self.check_picture_format_with_dup_main_picture(response)

        response = self.report.request_json('place=modelinfo&hyperid=1622113&rids=0')
        self.check_picture_format_with_dup_main_picture(response)

        response = self.report.request_json('place=prime&hid=1622103&trim-thumbs=1')
        self.check_picture_format_with_dup_main_picture(response)

        response = self.report.request_json('place=modelinfo&hyperid=1622113&rids=0&trim-thumbs=1')
        self.check_picture_format_with_dup_main_picture(response)

    @classmethod
    def prepare_picture_color_matching(cls):
        cls.index.gltypes += [
            GLType(
                param_id=13887626, hid=20, gltype=GLType.ENUM, values=[100, 200, 300], cluster_filter=True
            ),  # базовый цвет
            GLType(
                param_id=14871214, hid=20, gltype=GLType.ENUM, values=[110, 120, 210, 310], cluster_filter=True
            ),  # вендорский цвет
        ]

        cls.index.models += [
            Model(
                title="iphone-7",
                hid=20,
                hyperid=333,
                proto_picture=PictureMbo(params=[PictureParam(param_id=14871214, type=GLType.ENUM, value=110)]),
                proto_add_pictures=[
                    PictureMbo(
                        params=[
                            PictureParam(param_id=14871214, type=GLType.ENUM, value=120),
                            PictureParam(param_id=13887626, type=GLType.ENUM, value=100),
                        ]
                    ),
                    PictureMbo(params=[PictureParam(param_id=14871214, type=GLType.ENUM, value=210)]),
                    PictureMbo(params=[PictureParam(param_id=14871214, type=GLType.ENUM, value=310)]),
                ],
            ),
        ]

        cls.index.vendor_to_glob_colors += [
            VendorToGlobalColor(333, 100, [110, 120]),
            VendorToGlobalColor(333, 200, [210]),
        ]

        cls.index.offers += [
            Offer(title="color 1", hyperid=333, hid=20, glparams=[GLParam(param_id=14871214, value=110)]),
            Offer(title="color 2", hyperid=333, hid=20, glparams=[GLParam(param_id=14871214, value=120)]),
            Offer(title="color 3", hyperid=333, hid=20, glparams=[GLParam(param_id=14871214, value=110)]),
        ]

    def test_picture_color_mapping(self):
        """
        Проверяем, что в параметрах картинок, кроме вендорского присутствует также соотв. глобальный (базовый) цвет
        у 110 и 120 одинаковый базовый цвет - 100
        у 310 не присутсвтует, потому что соответствие не задано
        """
        response = self.report.request_json('place=prime&hid=20')

        self.assertFragmentIn(
            response,
            {
                "pictures": [
                    {"entity": "picture", "filtersMatching": {"13887626": ["100"], "14871214": ["110"]}},
                    {"entity": "picture", "filtersMatching": {"13887626": ["100"], "14871214": ["120"]}},
                    {"entity": "picture", "filtersMatching": {"13887626": ["200"], "14871214": ["210"]}},
                    {"entity": "picture", "filtersMatching": {"14871214": ["310"]}},
                ]
            },
            allow_different_len=False,
        )

    def test_choose_first_picture_by_filter(self):
        """
        MARKETOUT-40947
        """
        response = self.report.request_json('place=prime&hid=20&pp=18&glfilter=14871214:120')

        self.assertFragmentIn(
            response,
            {
                "pictures": [
                    {"entity": "picture", "filtersMatching": {"13887626": ["100"], "14871214": ["120"]}},
                    {"entity": "picture", "filtersMatching": {"13887626": ["100"], "14871214": ["110"]}},
                    {"entity": "picture", "filtersMatching": {"13887626": ["200"], "14871214": ["210"]}},
                    {"entity": "picture", "filtersMatching": {"14871214": ["310"]}},
                ]
            },
            allow_different_len=False,
            preserve_order=True,
        )


if __name__ == '__main__':
    main()
