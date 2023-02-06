#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from itertools import product

from core.types import BlueOffer, GLParam, GLType, MarketSku, NavCategory, NavRecipe, NavRecipeFilter, Vat
from core.testcase import TestCase, main
from core.matcher import Absent

P_ENUM_1 = 100
P_ENUM_2 = 101

HID = 1


def create_sample(nid, count, children=None):
    return {
        "category": {"nid": nid, "hid": 1},
        "ownCount": count,
        "intents": children if children is not None else Absent(),
    }


part_1111 = create_sample(1111, 1)
part_1112 = create_sample(1112, 1)
part_1121 = create_sample(1121, 1)
part_1122 = create_sample(1122, 2)  # В этом рецепте два документа подходит, т.к. два значения у фильтра

part_111 = create_sample(111, 4, [part_1111, part_1112])
part_112 = create_sample(112, 4, [part_1121, part_1122])


def create_filter(param, values):
    return NavRecipeFilter(filter_type=NavRecipeFilter.ENUM, param_id=param, enum_values=values)


def create_nid(nid, filters, children=None):
    return NavCategory(
        nid=nid,
        hid=HID,
        primary=False,
        recipe=NavRecipe(
            filters=filters,
        )
        if filters
        else None,
        children=children,
    )


class T(TestCase):
    @classmethod
    def prepare(cls):

        cls.index.gltypes += [
            GLType(param_id=P_ENUM_1, hid=HID, cluster_filter=False, gltype=GLType.ENUM, values=[1, 2]),
            GLType(param_id=P_ENUM_2, hid=HID, cluster_filter=False, gltype=GLType.ENUM, values=[11, 12, 13]),
        ]

        '''
        Будет настроен праймари узел для хида (нид=11).
        В нем два рецепта для этого же хида
        В каждом из этих рецептов будет свой вложенный рецепт.
        '''
        filter_1_1 = create_filter(P_ENUM_1, [1])
        filter_2_11 = create_filter(P_ENUM_2, [11])
        filter_2_12 = create_filter(P_ENUM_2, [11])

        filter_1_2 = create_filter(P_ENUM_1, [2])
        filter_2_12 = create_filter(P_ENUM_2, [12])
        filter_2_12_13 = create_filter(P_ENUM_2, [12, 13])  # Специально сделал два значения у фильтра

        nid_1111 = create_nid(1111, [filter_1_1, filter_2_11], None)
        nid_1112 = create_nid(1112, [filter_1_1, filter_2_12], None)
        nid_111 = create_nid(111, [filter_1_1], [nid_1111, nid_1112])

        nid_1121 = create_nid(1121, [filter_1_2, filter_2_11], None)
        nid_1122 = create_nid(1122, [filter_1_2, filter_2_12_13], None)
        nid_112 = create_nid(112, filters=[filter_1_2], children=[nid_1121, nid_1122])

        cls.index.navtree += [NavCategory(nid=11, hid=HID, primary=True, children=[nid_111, nid_112])]

        cls.index.navtree_blue = cls.index.navtree

        # Создаем оферы со всеми комбинациями параметров
        # [нет параметра, 1, 2, 3] для обоих параметров P_ENUM_1 и P_ENUM_2

        for v1, v2 in product([0, 1, 2, 3], [0, 11, 12, 13]):
            index = 10000 + v2 * 10 + v1
            glparams = []
            if v1:
                glparams += [GLParam(param_id=P_ENUM_1, value=v1)]
            if v2:
                glparams += [GLParam(param_id=P_ENUM_2, value=v2)]
            cls.index.mskus += [
                MarketSku(
                    title='OFFER {}'.format(index),
                    hyperid=index,
                    hid=HID,
                    sku=index,
                    blue_offers=[
                        BlueOffer(price=1000, vat=Vat.VAT_10, offerid=index),
                    ],
                    glparams=glparams,
                ),
            ]

    def test_nested_recipes(self):
        '''
        Проверяем, что на синем в интентах отображаются все ниды, подходящие оферу, а не только лучший
        '''

        # Проверяем, что ВСЕ вложенные рецепты показаны
        sample_11 = create_sample(11, 16, [part_111, part_112])

        response = self.report.request_json('place=prime&rgb=blue&nid=11')
        self.assertFragmentIn(response, {"search": {}, "intents": [sample_11]}, allow_different_len=False)

        # Проверяем, что интенты ищутся только внутри запрошенного нида
        sample_111 = create_sample(11, 0, [part_111])
        sample_112 = create_sample(11, 0, [part_112])
        sample_1111 = create_sample(11, 0, [create_sample(111, 0, [part_1111])])
        sample_1112 = create_sample(11, 0, [create_sample(111, 0, [part_1112])])
        sample_1121 = create_sample(11, 0, [create_sample(112, 0, [part_1121])])
        sample_1122 = create_sample(11, 0, [create_sample(112, 0, [part_1122])])

        for nid, sample in [
            (11, sample_11),
            (111, sample_111),
            (112, sample_112),
            (1111, sample_1111),
            (1112, sample_1112),
            (1121, sample_1121),
            (1122, sample_1122),
        ]:
            response = self.report.request_json('place=prime&rgb=blue&text=OFFER&nid={}'.format(nid))
            self.assertFragmentIn(response, {"search": {}, "intents": [sample]}, allow_different_len=False)

    def test_nested_recipes_user_filters(self):
        '''
        Пользовательские фильтры влияют на выдачу
        '''

        # Нашли только оферы, у которых значение первого фильтра 1 (т.е. только подмножество 111 нида)
        response = self.report.request_json('place=prime&rgb=blue&nid=11&glfilter={}:1'.format(P_ENUM_1))
        self.assertFragmentIn(
            response, {"search": {}, "intents": [create_sample(11, 4, [part_111])]}, allow_different_len=False
        )

        # Нашли только оферы, у которых значение первого фильтра 2 (т.е. только подмножество 112 нида)
        response = self.report.request_json('place=prime&rgb=blue&nid=11&glfilter={}:2'.format(P_ENUM_1))
        self.assertFragmentIn(
            response, {"search": {}, "intents": [create_sample(11, 4, [part_112])]}, allow_different_len=False
        )

        # Нашли только оферы, у которых значение первого фильтра 3. Ни 111, ни 112 не подходит. Все оферы привязались к 11 ниду
        response = self.report.request_json('place=prime&rgb=blue&nid=11&glfilter={}:3'.format(P_ENUM_1))
        self.assertFragmentIn(response, {"search": {}, "intents": [create_sample(11, 4)]}, allow_different_len=False)

        # Фильтрация по второму параметру.
        response = self.report.request_json('place=prime&rgb=blue&nid=11&glfilter={}:11'.format(P_ENUM_2))
        self.assertFragmentIn(
            response,
            {
                "search": {},
                "intents": [
                    create_sample(
                        11,
                        4,
                        [  # Два офера привязались к рецептам, еще два к 11 ниду
                            create_sample(111, 1, [part_1111]),
                            create_sample(112, 1, [part_1121]),
                        ],
                    )
                ],
            },
            allow_different_len=False,
        )

        response = self.report.request_json('place=prime&rgb=blue&nid=11&glfilter={}:12'.format(P_ENUM_2))
        self.assertFragmentIn(
            response,
            {
                "search": {},
                "intents": [
                    create_sample(
                        11,
                        4,
                        [
                            create_sample(111, 1, [part_1112]),
                            create_sample(112, 1, [create_sample(1122, 1)]),
                        ],
                    )
                ],
            },
            allow_different_len=False,
        )

        response = self.report.request_json('place=prime&rgb=blue&nid=11&glfilter={}:13'.format(P_ENUM_2))
        self.assertFragmentIn(
            response,
            {
                "search": {},
                "intents": [
                    create_sample(
                        11,
                        4,
                        [
                            create_sample(
                                111, 1
                            ),  # Вложенные в этот рецепт не показываем. Ни один офер не подходит под них
                            create_sample(112, 1, [create_sample(1122, 1)]),
                        ],
                    )
                ],
            },
            allow_different_len=False,
        )

    def test_without_nested_recipes(self):
        '''
        Проверяем, что функциональность можно выключить с помощью флага
        '''
        # 11 нид главный. Все оферы привязаны только к нему
        sample_11 = create_sample(11, 16)

        response = self.report.request_json(
            'place=prime&rgb=blue&nid=11&rearr-factors=market_blue_nid_nested_recipes=0'
        )
        self.assertFragmentIn(response, {"search": {}, "intents": [sample_11]}, allow_different_len=False)

    def test_text_search_without_nested_recipes(self):
        '''
        Проверяем, что подбор вложенных рецептов не работает для просто текстового поиска
        '''
        # 11 нид главный. Все оферы привязаны только к нему
        sample_11 = create_sample(11, 16)

        response = self.report.request_json('place=prime&rgb=blue&text=OFFER')
        self.assertFragmentIn(response, {"search": {}, "intents": [sample_11]}, allow_different_len=False)


if __name__ == '__main__':
    main()
