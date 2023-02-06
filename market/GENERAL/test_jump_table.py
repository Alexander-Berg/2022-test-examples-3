#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.testcase import TestCase, main
from core.types.autogen import b64url_md5
from itertools import count
from core.types import BlueOffer, GLParam, GLType, GLValue, MarketSku, Model


def get_counter():
    result = count()
    next(result)
    return result


wmd5counter = get_counter()


def make_offer():
    return BlueOffer(waremd5=b64url_md5(next(wmd5counter)))


def make_sku(sku_id, hyperid, blue_offers, glparams, original_glparams):
    return MarketSku(
        hyperid=hyperid,
        sku=sku_id,
        blue_offers=blue_offers,
        glparams=glparams,
        original_glparams=original_glparams,
    )


def make_model(start_sku_id, id, mskus_dst, mskus_params):
    sku_id = start_sku_id
    for msku_params in mskus_params:
        mskus_dst += [make_sku(sku_id, id, [make_offer()], msku_params, msku_params)]
        sku_id = sku_id + 1
    return Model(hyperid=id, hid=id, title='model ' + str(id))


class T(TestCase):
    class TIERS:
        class MICHELIN:
            ID = 1

            # params
            class WIDTH:
                ID = 1
                XSL_NAME = "WIDTH"

            class HEIGHT:
                ID = 2
                XSL_NAME = "HEIGHT"

            class RADIUS:
                ID = 3
                XSL_NAME = "RADIUS"

        class GOODYEAR:
            ID = 2

            # params
            class WIDTH:
                ID = 1
                XSL_NAME = "WIDTH"

            class HEIGHT:
                ID = 2
                XSL_NAME = "HEIGHT"

            class RADIUS:
                ID = 3
                XSL_NAME = "RADIUS"

            class INDEX:
                ID = 4
                XSL_NAME = "INDEX"

            class SPEED:
                ID = 5
                XSL_NAME = "SPEED"

            class TECHNOLOGY:
                ID = 6
                XSL_NAME = "TECHNOLOGY"

    @classmethod
    def prepare_short_adaptive_check(cls):
        cls.index.gltypes += [
            GLType(
                name=param.XSL_NAME,
                xslname=param.XSL_NAME,
                param_id=param.ID,
                hid=T.TIERS.MICHELIN.ID,
                cluster_filter=True,
                model_filter_index=param.ID,
                gltype=GLType.ENUM,
                values=[GLValue(position=i, value_id=i, text=str(i)) for i in range(1, 10)],
            )
            for param in [
                T.TIERS.MICHELIN.WIDTH,
                T.TIERS.MICHELIN.HEIGHT,
                T.TIERS.MICHELIN.RADIUS,
            ]
        ]

        def width(value):
            return GLParam(param_id=T.TIERS.MICHELIN.WIDTH.ID, value=value)

        def height(value):
            return GLParam(param_id=T.TIERS.MICHELIN.HEIGHT.ID, value=value)

        def radius(value):
            return GLParam(param_id=T.TIERS.MICHELIN.RADIUS.ID, value=value)

        start_sku_id = 1
        cls.index.models += [
            make_model(
                start_sku_id,
                T.TIERS.MICHELIN.ID,
                cls.index.mskus,
                [
                    [
                        width(1),
                        height(2),
                        radius(3),
                    ],
                    [
                        width(2),
                        height(3),
                        radius(1),
                    ],
                    [
                        width(3),
                        height(3),
                        radius(2),
                    ],
                    [
                        width(1),
                        height(3),
                        radius(4),
                    ],
                ],
            ),
        ]

    """
    Изначально имеем 4 sku
    |_sku_|_p1_|_p2_|_p3_|
    |__1__|_1__|_2__|_3__|
    |__2__|_2__|_3__|_1__|
    |__3__|_3__|_3__|_2__|
    |__4__|_1__|_3__|_4__|
    пользователь выбирает первый параметр с опцией 1 sku 1
    |_p1_|>1<|_2_|_3_|
    |_p2_|>2<|_3_|
    |_p3_|_1_|_2_|>3<|_4_|category
    |_p3_|_1_|>2<|_3_|_4_|
    """

    def test_no_good_choice_param(self):
        """
        Проверяем, что без предположений карта переходов нам предлагает не оптимальный вариант перехода
        где при выборе опции второго параметра мы так же сменим опцию первого параметра
        """
        response = self.report.request_json(
            'place=productoffers&hyperid={}&pp=18&hid={}&market-sku=1&rearr-factors=market_jumptable_adaptive_calc=0'.format(
                T.TIERS.MICHELIN.ID, T.TIERS.MICHELIN.ID
            )
        )

        self.assertFragmentNotIn(
            response,
            {
                "creditOptions": [],
                "filters": [
                    {
                        "name": T.TIERS.MICHELIN.HEIGHT.XSL_NAME,
                        "values": [
                            {
                                "marketSku": "4",
                            }
                        ],
                    }
                ],
            },
            preserve_order=True,
        )

    def test_good_choice_param(self):
        """
        Проверяем, что с нашим предположением, что пользователь выбирает параметры последовательно
        мы предложим пользователю более оптимальный вариант выбора следующией опции
        """
        response = self.report.request_json(
            'place=productoffers&hyperid={}&pp=18&hid={}&market-sku=1&rearr-factors=market_jumptable_adaptive_calc=1'.format(
                T.TIERS.MICHELIN.ID, T.TIERS.MICHELIN.ID
            )
        )

        self.assertFragmentIn(
            response,
            {
                "creditOptions": [],
                "filters": [
                    {
                        "name": T.TIERS.MICHELIN.HEIGHT.XSL_NAME,
                        "values": [
                            {
                                "marketSku": "4",
                            }
                        ],
                    }
                ],
            },
            preserve_order=True,
        )

    @classmethod
    def prepare_long_adaptive_check(cls):
        cls.index.gltypes += [
            GLType(
                name=param.XSL_NAME,
                xslname=param.XSL_NAME,
                param_id=param.ID,
                hid=T.TIERS.GOODYEAR.ID,
                cluster_filter=True,
                model_filter_index=param.ID,
                gltype=GLType.ENUM,
                values=[GLValue(position=i, value_id=i, text=str(i)) for i in range(1, 10)],
            )
            for param in [
                T.TIERS.GOODYEAR.WIDTH,
                T.TIERS.GOODYEAR.HEIGHT,
                T.TIERS.GOODYEAR.RADIUS,
                T.TIERS.GOODYEAR.INDEX,
                T.TIERS.GOODYEAR.SPEED,
                T.TIERS.GOODYEAR.TECHNOLOGY,
            ]
        ]

        def width(value):
            return GLParam(param_id=T.TIERS.GOODYEAR.WIDTH.ID, value=value)

        def height(value):
            return GLParam(param_id=T.TIERS.GOODYEAR.HEIGHT.ID, value=value)

        def radius(value):
            return GLParam(param_id=T.TIERS.GOODYEAR.RADIUS.ID, value=value)

        def index(value):
            return GLParam(param_id=T.TIERS.GOODYEAR.INDEX.ID, value=value)

        def speed(value):
            return GLParam(param_id=T.TIERS.GOODYEAR.SPEED.ID, value=value)

        def technology(value):
            return GLParam(param_id=T.TIERS.GOODYEAR.TECHNOLOGY.ID, value=value)

        many_skus_params = [
            [
                width(num),
                height(num),
                radius(num),
                index(num),
                speed(num),
                technology(num),
            ]
            for num in range(1, 11)
        ]
        # fuzzy sku, выгодное sku 1011 но первые параметры не меняются
        many_skus_params += (
            [
                width(1),
                height(1),
                radius(1),
                index(2),
                speed(2),
                technology(1),
            ],
        )
        # fuzzy sku, не выгодное sku 1012 меняется 1 из первых параметрв radius
        many_skus_params += (
            [
                width(1),
                height(1),
                radius(2),
                index(2),
                speed(1),
                technology(1),
            ],
        )

        start_sku_id = 1001
        cls.index.models += [
            make_model(start_sku_id, T.TIERS.GOODYEAR.ID, cls.index.mskus, many_skus_params),
        ]

    def test_no_good_choice_param_many_choices(self):
        """
        Проверяем, что без предположений карта переходов нам предлагает не оптимальный вариант перехода
        в большом количестве вариаций
        """
        response = self.report.request_json(
            'place=productoffers&hyperid={}&pp=18&hid={}&market-sku=1001&rearr-factors=market_jumptable_adaptive_calc=0'.format(
                T.TIERS.GOODYEAR.ID, T.TIERS.GOODYEAR.ID
            )
        )

        self.assertFragmentNotIn(
            response,
            {
                "creditOptions": [],
                "filters": [
                    {
                        "name": T.TIERS.GOODYEAR.INDEX.XSL_NAME,
                        "values": [
                            {
                                "marketSku": "1011",
                            }
                        ],
                    }
                ],
            },
            preserve_order=True,
        )

    def test_good_choice_param_many_choice(self):
        """
        Проверяем, что с нашим предположением, что пользователь выбирает параметры последовательно
        мы предложим пользователю более оптимальный вариант выбора следующией опции
        в большом количестве вариаций
        """
        response = self.report.request_json(
            'place=productoffers&hyperid={}&pp=18&hid={}&market-sku=1001&rearr-factors=market_jumptable_adaptive_calc=1'.format(
                T.TIERS.GOODYEAR.ID, T.TIERS.GOODYEAR.ID
            )
        )

        self.assertFragmentIn(
            response,
            {
                "creditOptions": [],
                "filters": [
                    {
                        "name": T.TIERS.GOODYEAR.INDEX.XSL_NAME,
                        "values": [
                            {
                                "marketSku": "1011",
                            }
                        ],
                    }
                ],
            },
            preserve_order=True,
        )


if __name__ == '__main__':
    main()
