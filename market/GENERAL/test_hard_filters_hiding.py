#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

"""
Набор тестов скрытия хардкод фильтров на серче когда в топе интентов не фэшн категория
Хардкод фильтры - сквозные фильтры между разными категориями которые отображаются когда в запросе нет hid и nid
"""

from core.types import BlueOffer, GLParam, GLType, MarketSku, Model
from core.testcase import TestCase, main

from core.types.fashion_parameters import FashionCategory


class HardFiltes:
    brand = 7893318
    size = 26417130
    gender = 14805991
    season = 27142893

    @classmethod
    def get_fashion_params(self, value):
        return [
            GLParam(param_id=self.size, value=value),
            GLParam(param_id=self.gender, value=value),
            GLParam(param_id=self.season, value=value),
        ]

    @classmethod
    def get_common_params(self, value):
        return [
            GLParam(param_id=self.brand, value=value),
        ]


class ModelBase:
    @classmethod
    def get_sku(self):
        self.sku += 1
        return self.sku

    @classmethod
    def get_model(self):
        return Model(
            hyperid=self.model,
            hid=self.hid,
            title=self.title,
        )

    @classmethod
    def get_msku(self, glparams):
        return MarketSku(
            title=self.title,
            hyperid=self.model,
            sku=self.get_sku(),
            blue_offers=[BlueOffer()],
            glparams=glparams,
        )


class Category:
    class Electornics:
        class MobilePhones:
            id = 1
            name = "MobilePhones"

        class PowerSockets:
            id = 2
            name = "PowerSockets"

    class Clothes:
        id = 3
        name = "Clothes"

        class Pants:
            id = 4
            name = "Pants"

        class Backpacks:
            id = 5
            name = "Backpacks"


class Xiaomi:
    class Phone(ModelBase):
        title = "Xiaomi Phone"
        model = 10
        sku = 10
        hid = Category.Electornics.MobilePhones.id

    class PowerSocket(ModelBase):
        title = "Xiaomi PowerSocket"
        model = 20
        sku = 20
        hid = Category.Electornics.PowerSockets.id

    class Backpack(ModelBase):
        title = "Xiaomi Backpack"
        sku = 30
        model = 30
        hid = Category.Clothes.Backpacks.id


class Samsung:
    class PhoneS10(ModelBase):
        title = "Samsung Phone s10"
        model = 100
        sku = 100
        hid = Category.Electornics.MobilePhones.id

    class PhoneNote(ModelBase):
        title = "Samsung Phone note"
        model = 200
        sku = 200
        hid = Category.Electornics.MobilePhones.id


class Gucci:
    class Backpack(ModelBase):
        title = "Gucci Backpack"
        model = 1000
        sku = 1000
        hid = Category.Clothes.Backpacks.id

    class Pants(ModelBase):
        title = "Gucci Pants"
        model = 2000
        sku = 2000
        hid = Category.Clothes.Pants.id

    class Phone(ModelBase):
        title = "Gucci Phone"
        model = 3000
        sku = 3000
        hid = Category.Electornics.MobilePhones.id


class T(TestCase):
    @classmethod
    def prepare(cls):
        def create_model_and_msku(info, params):
            cls.index.mskus += [
                info.get_msku(params),
            ]

            cls.index.models += [
                info.get_model(),
            ]

        cls.index.fashion_categories += [
            FashionCategory(Category.Clothes.Pants.name, Category.Clothes.Pants.id),
            FashionCategory(Category.Clothes.Backpacks.name, Category.Clothes.Backpacks.id),
        ]

        create_model_and_msku(Gucci.Backpack, HardFiltes.get_fashion_params(1) + HardFiltes.get_common_params(1))
        create_model_and_msku(Gucci.Pants, HardFiltes.get_fashion_params(2) + HardFiltes.get_common_params(1))
        create_model_and_msku(Xiaomi.Backpack, HardFiltes.get_fashion_params(3) + HardFiltes.get_common_params(2))
        create_model_and_msku(Gucci.Phone, HardFiltes.get_common_params(1))
        create_model_and_msku(Xiaomi.PowerSocket, HardFiltes.get_common_params(2))
        create_model_and_msku(Xiaomi.Phone, HardFiltes.get_common_params(2))
        create_model_and_msku(Samsung.PhoneS10, HardFiltes.get_common_params(3))
        create_model_and_msku(Samsung.PhoneNote, HardFiltes.get_common_params(3))

        for hid in [Category.Clothes.Pants.id, Category.Clothes.Backpacks.id]:
            cls.index.gltypes += [
                GLType(param_id=HardFiltes.size, hid=hid, gltype=GLType.ENUM, values=[1, 2, 3, 4, 5, 6, 7, 8, 9]),
                GLType(param_id=HardFiltes.gender, hid=hid, gltype=GLType.ENUM, values=[1, 2, 3, 4, 5, 6, 7, 8, 9]),
                GLType(param_id=HardFiltes.season, hid=hid, gltype=GLType.ENUM, values=[1, 2, 3, 4, 5, 6, 7, 8, 9]),
            ]

        for hid in [
            Category.Electornics.MobilePhones.id,
            Category.Electornics.PowerSockets.id,
            Category.Clothes.Pants.id,
            Category.Clothes.Backpacks.id,
        ]:
            cls.index.gltypes += [
                GLType(param_id=HardFiltes.brand, hid=hid, gltype=GLType.ENUM, values=[1, 2, 3, 4, 5, 6, 7, 8, 9]),
            ]

    def test_showed_all_hard_filters_with_different_items_brands(self):
        """
        Проверяем, что при запросе Backpack у нас есть хардкод фильтры фэшн и общий фильтр брэнд
        т.к. рюкзаки Cucci и Xiaomi это фэшн
        """
        response = self.report.request_json('place=prime&text=Backpack')
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {"id": str(HardFiltes.size)},
                    {"id": str(HardFiltes.gender)},
                    {"id": str(HardFiltes.season)},
                    {"id": str(HardFiltes.brand)},
                ],
            },
        )

    def test_hide_fashion_hard_filters_when_brand_is_no_mostly_fashion(self):
        """
        Проверяем, что при запросе Xiaomi у нас нет хардкод фильтров фэшн, только общий фильтр брэнд
        т.к. Xiaomi это в основном электроника, а не фэшн
        """
        response = self.report.request_json('place=prime&text=Xiaomi')
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {"id": str(HardFiltes.brand)},
                ],
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                "filters": [{"id": str(HardFiltes.size)}],
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                "filters": [{"id": str(HardFiltes.gender)}],
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                "filters": [{"id": str(HardFiltes.season)}],
            },
        )

    def test_hide_fashion_hard_filters_when_result_is_no_mostly_fashion(self):
        """
        Проверяем, что при запросе Phone у нас нет хардкод фильтров фэшн, только общий фильтр брэнд
        т.к. Phone это в основном электроника, а не фэшн
        """
        response = self.report.request_json('place=prime&text=Phone')
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {"id": str(HardFiltes.brand)},
                ],
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                "filters": [{"id": str(HardFiltes.size)}],
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                "filters": [{"id": str(HardFiltes.gender)}],
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                "filters": [{"id": str(HardFiltes.season)}],
            },
        )

    def test_hide_fashion_hard_filters_when_result_only_eclectronic_brand(self):
        """
        Проверяем, что при запросе Samsung у нас нет хардкод фильтров фэшн, только общий фильтр брэнд
        т.к. Samsung это только электроника, а не фэшн
        """
        response = self.report.request_json('place=prime&text=Samsung')
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {"id": str(HardFiltes.brand)},
                ],
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                "filters": [{"id": str(HardFiltes.size)}],
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                "filters": [{"id": str(HardFiltes.gender)}],
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                "filters": [{"id": str(HardFiltes.season)}],
            },
        )


if __name__ == '__main__':
    main()
