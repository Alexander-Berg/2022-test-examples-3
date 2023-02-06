#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import GLParam, GLType, Model, Offer, PictureMbo, PictureParam, Region, Shop, VendorToGlobalColor
from core.testcase import TestCase, main

from core.matcher import NoKey


class T(TestCase):
    @classmethod
    def prepare_picture_color_mapping(cls):
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
                    PictureMbo(params=[PictureParam(param_id=14871214, type=GLType.ENUM, value=120)]),
                    PictureMbo(params=[PictureParam(param_id=14871214, type=GLType.ENUM, value=210)]),
                    PictureMbo(params=[PictureParam(param_id=14871214, type=GLType.ENUM, value=310)]),
                ],
            ),
        ]

        cls.index.vendor_to_glob_colors += [
            VendorToGlobalColor(333, 100, [110, 120]),
            VendorToGlobalColor(333, 200, [210]),
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

    @classmethod
    def prepare_vendor_colors_count(cls):
        cls.index.regiontree += [
            Region(rid=213, name='Москва'),
            Region(rid=43, name='Казань'),
            Region(rid=120916, name='Липецк'),
        ]

        cls.index.shops += [
            Shop(fesh=1000, name="Московский магазин", priority_region=213),
            Shop(fesh=2000, name="Казанский магазин", priority_region=43),
            Shop(fesh=3000, name="Липецкий магазин", priority_region=120916),
        ]

        cls.index.models += [
            Model(title="samsung galaxy 1", hid=10, hyperid=111),
            Model(title="samsung galaxy 5", hid=11, hyperid=555),
        ]

        cls.index.gltypes += [
            GLType(
                param_id=14871214,
                hid=10,
                gltype=GLType.ENUM,
                values=[100, 110, 120, 200, 210, 220, 300, 310, 320],
                cluster_filter=True,
            ),  # вендорский цвет
        ]

        cls.index.offers += [
            Offer(
                title="Moscow color 1", hyperid=111, hid=10, fesh=1000, glparams=[GLParam(param_id=14871214, value=100)]
            ),
            Offer(
                title="Moscow color 2", hyperid=111, hid=10, fesh=1000, glparams=[GLParam(param_id=14871214, value=110)]
            ),
            Offer(
                title="Moscow color 3", hyperid=111, hid=10, fesh=1000, glparams=[GLParam(param_id=14871214, value=100)]
            ),
            Offer(
                title="Kazan color 1", hyperid=111, hid=10, fesh=2000, glparams=[GLParam(param_id=14871214, value=100)]
            ),
            Offer(
                title="Kazan color 2", hyperid=111, hid=10, fesh=2000, glparams=[GLParam(param_id=14871214, value=210)]
            ),
            Offer(
                title="Kazan color 3", hyperid=111, hid=10, fesh=2000, glparams=[GLParam(param_id=14871214, value=220)]
            ),
            Offer(title="Lipetsk no color 1", hyperid=111, hid=10, fesh=3000),
            Offer(title="Lipetsk no color 2", hyperid=111, hid=10, fesh=3000),
            Offer(title="Moscow no color 1", hyperid=555, hid=11, fesh=1000),
        ]

    def test_vendor_color_count(self):
        '''
        Проверяем, что выводим кол-во разных значений colorVendorCount соответствующих модели
        '''

        # в Москве три оффера, но два разных цвета
        response = self.report.request_json('place=prime&hid=10&rids=213')
        self.assertFragmentIn(response, {"colorVendorCount": 2})

        # в Казани - три разных цвета
        response = self.report.request_json('place=prime&hid=10&rids=43')
        self.assertFragmentIn(response, {"colorVendorCount": 3})

        # в Липецке - нет цветов
        response = self.report.request_json('place=prime&hid=10&rids=120916')
        self.assertFragmentIn(response, {"colorVendorCount": NoKey("colorVendorCount")})

        # всего использовано разных цветов - 4
        response = self.report.request_json('place=prime&hid=10')
        self.assertFragmentIn(response, {"colorVendorCount": 4})

        # модель без цветов
        response = self.report.request_json('place=prime&hid=11&rids=213')
        self.assertFragmentIn(response, {"colorVendorCount": NoKey("colorVendorCount")})


if __name__ == '__main__':
    main()
