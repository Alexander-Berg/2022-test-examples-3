#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

# Таблица илюстрирует всю информацию о размерах у доступных мскю модели.
#
#        | Размер | INT  | RU |  EU   | Обхват шеи |
# msku 1 |   178  |  S   | 33 |  45   |     63     |
# msku 3 |   180  |  M   | 34 |  46   |     66     |
# msku 8 |   182  |  L   | 35 |  47   |     69     |
# msku 9 |   184  |  XL  | 36 | 48-50 |     72     |
#
# Таблица строится на основе msku построенной карты переходов.
# Берем все msku из карты переходов, собираем все параметры и additional values
# из данных доступных в JumpTableAccessor по msku что бы была возможность отрисовать подобную табличку
# Каждая строка соответствует одному msku
# При наличии нескольких значений у одной msku сохраняются только 2 значения value и valueMax
# Сначала выводятся параметры с типом size и model_index > -1, следующие идут информация о сконверченых размерах size и model_index = -1
# В конце идут мерки, объединенные параметры MIN и MAX

from core.testcase import TestCase, main
from core.types.autogen import b64url_md5
from itertools import count
from core.types import BlueOffer, GLParam, GLType, GLValue, MarketSku, Model, Shop, GLSizeChart
from core.types.fashion_parameters import FashionCategory
from core.matcher import Absent


def get_counter(num=1):
    result = count(num)
    return result


wmd5counter = get_counter()


def make_offer():
    return BlueOffer(fesh=100, waremd5=b64url_md5(next(wmd5counter)))


# sku не должны пересекаться с идентификаторами модели
skucounter = get_counter(10000000)

one_record_gloria_offers = []


def make_sku(hyperid, blue_offers, glparams, original_glparams):
    return MarketSku(
        hyperid=hyperid,
        sku=next(skucounter),
        blue_offers=blue_offers,
        glparams=glparams,
        original_glparams=original_glparams,
    )


def make_model(id, hid, mskus_dst, params, offers=[]):
    for msku_params, original_glparams in params:
        offer = make_offer()
        mskus_dst += [make_sku(id, [offer], msku_params, original_glparams)]
        offers.append(offer)
    return Model(hyperid=id, hid=hid, title='model ' + str(id))


class T(TestCase):

    # hid
    class SHIRTS:
        ID = 1
        SECOND_ID = 3

        # model
        class GREG:
            ID = 1
            SECOND_ID = 3

            # params
            class HEIGHT_MAN:
                ID = 1
                XSL_NAME = "height_man"
                RUS_NAME = "Рост"

            class HEIGHT_MAN_UNITS:
                ID = 2
                XSL_NAME = "height_man_units"

            class GIRTH_NECK:
                ID = 3
                XSL_NAME = "girth_neck"
                RUS_NAME = "Обхват шеи"

            class COLOR:
                ID = 4
                XSL_NAME = "color"
                RUS_NAME = "Цвет"

            class TYPE:
                ID = 5
                XSL_NAME = "type"
                RUS_NAME = "тип"

            class GIRTH_CHEST_MIN:
                ID = 6
                XSL_NAME = "girth_chest_MIN"
                RUS_NAME = "Обхват груди мужская размерная сетка см (min)"
                NEW_PARTHNER_NAME = "Обхват груди (min)"

            class GIRTH_CHEST_MAX:
                ID = 7
                XSL_NAME = "girth_chest_MAX"
                RUS_NAME = "Обхват груди мужская размерная сетка см (max)"
                NEW_PARTHNER_NAME = "Обхват груди (max)"

    class SKIRTS:
        ID = 2

        # model
        class ALBARI:
            ID = 2

            # params
            class HEIGHT_GIRL:
                ID = 1
                XSL_NAME = "height_girl"
                RUS_NAME = "Рост"

            class HEIGHT_GIRL_UNITS:
                ID = 2
                XSL_NAME = "height_girl_units"

            class GIRTH_HIP:
                ID = 3
                XSL_NAME = "girth_hip"
                RUS_NAME = "Обхват бедра"

            class GIRTH_HIP_UNITS:
                ID = 4
                XSL_NAME = "girth_hip_units"

    class BLOUSES:
        ID = 4

        # model
        class VALENTINO:
            ID = 4

            # params
            # рост с гипотезами, у которого нет сконверченых размеров
            class HEIGHT_GIRL_UNITED:
                ID = 1
                XSL_NAME = "height_girl_united"
                RUS_NAME = "Рост"

            # рост с enum значениями и сконверчеными значениями
            class HEIGHT_GIRL:
                ID = 2
                XSL_NAME = "height_girl"
                RUS_NAME = "Рост"

            class HEIGHT_GIRL_UNITS:
                ID = 3
                XSL_NAME = "height_girl_units"

            class GIRTH_WRIST_MIN:
                ID = 7
                XSL_NAME = "girth_wrist_MIN"
                RUS_NAME = "Обхват запястья (min)"

            class GIRTH_WRIST_MAX:
                ID = 8
                XSL_NAME = "girth_wrist_MAX"
                RUS_NAME = "Обхват запястья (max)"

            class HARD_HEIGHT_GIRL:
                ID = 9
                XSL_NAME = "hard_height_girl"
                RUS_NAME = "Рост"

            class HARD_HEIGHT_GIRL_UNITS:
                ID = 10
                XSL_NAME = "hard_height_girl_units"

    class SOCKS:
        ID = 5

        # model
        class PUMA:
            ID = 5

            # params
            class HEIGHT_MAN:
                ID = 1
                XSL_NAME = "height_man"
                RUS_NAME = "Рост"

            class HEIGHT_MAN_UNITS:
                ID = 2
                XSL_NAME = "height_man_units"

            class GIRTH_FOOT:
                ID = 3
                XSL_NAME = "girth_foot"
                RUS_NAME = "Обхват ступни"

    class PANTS:
        ID = 600

        # model
        class GLORIA:
            ID = 600

            # params
            class HEIGHT_MAN:
                ID = 601
                XSL_NAME = "height_man"
                RUS_NAME = "Рост"

    class GLOVES:
        ID = 700

        # model
        class GLORIA:
            ID = 700

            # params
            class HEIGHT_GIRL:
                ID = 701
                XSL_NAME = "height_girl"
                RUS_NAME = "Рост"

            class HEIGHT_GIRL_UNITS:
                ID = 702
                XSL_NAME = "height_girl_units"

    class CATEGORY_1:
        ID = 1001

        # model
        class BRAND_1:
            ID = 10001

            # params
            class PARAM_1:
                ID = 100001
                XSL_NAME = "param_1"
                RUS_NAME = "Параметр 1"

            class PARAM_1_UNITS:
                ID = 100002
                XSL_NAME = "param_1_units"

    class CATEGORY_2:
        ID = 1002

        # model
        class BRAND_1:
            ID = 20001

            # params
            class PARAM_1:
                ID = 200001
                XSL_NAME = "param_1"
                RUS_NAME = "Параметр 1"

    class CATEGORY_3:
        ID = 1003

        # model
        class BRAND_1:
            ID = 30001

            # params
            class PARAM_1:
                ID = 300001
                XSL_NAME = "param_without_converted"
                RUS_NAME = "Параметр без сконверченых"

            class PARAM_2:
                ID = 300002
                XSL_NAME = "param_hidden_with_converted"
                RUS_NAME = "Параметр скрытый с сконверченными"

            class PARAM_2_UNITS:
                ID = 300003
                XSL_NAME = "param_2_units"
                RUS_NAME = "Параметр с юнитами второго параметра"

    class CATEGORY_4:
        ID = 1004

        # model
        class BRAND_1:
            ID = 40001

            class PARAM:
                ID = 400001
                XSL_NAME = "param_with_hypothesis"
                RUS_NAME = "Параметр c гипотезой"

            class PARAM_UNITS:
                ID = 400002
                XSL_NAME = "param_with_hypothesis_units"
                RUS_NAME = "Параметр с юнитами параметра"

    @classmethod
    def prepare_group_size_table_blue(cls):
        cls.index.shops += [
            Shop(fesh=100, priority_region=213),
        ]

        cls.index.gltypes += [
            GLType(
                name=T.SHIRTS.GREG.HEIGHT_MAN.RUS_NAME,
                xslname=T.SHIRTS.GREG.HEIGHT_MAN.XSL_NAME,
                param_id=T.SHIRTS.GREG.HEIGHT_MAN.ID,
                unit_param_id=T.SHIRTS.GREG.HEIGHT_MAN_UNITS.ID,
                size_range_min_param_id=T.SHIRTS.GREG.GIRTH_CHEST_MIN.ID,
                size_range_max_param_id=T.SHIRTS.GREG.GIRTH_CHEST_MAX.ID,
                hid=T.SHIRTS.ID,
                cluster_filter=True,
                size_charts=[
                    GLSizeChart(option_id=11, default=True),
                    GLSizeChart(option_id=22),
                    GLSizeChart(option_id=33),
                ],
                model_filter_index=3,
                gltype=GLType.ENUM,
                subtype='size',
                values=[
                    # original glparams
                    GLValue(position=1, value_id=1, text='174-186', unit_value_id=11),
                    GLValue(position=2, value_id=2, text='186-192', unit_value_id=11),
                    GLValue(position=3, value_id=3, text='192-200', unit_value_id=11),
                    # paramsposition=NT  unit INT
                    GLValue(position=4, value_id=4, text='M', unit_value_id=22),
                    GLValue(position=5, value_id=5, text='S', unit_value_id=22),
                    GLValue(position=6, value_id=6, text='L', unit_value_id=22),
                    # paramsposition=RU  unit RU
                    GLValue(position=7, value_id=7, text='42', unit_value_id=33),
                    GLValue(position=8, value_id=8, text='43', unit_value_id=33),
                    GLValue(position=9, value_id=9, text='44', unit_value_id=33),
                    GLValue(position=10, value_id=10, text='45', unit_value_id=33),
                    GLValue(position=11, value_id=11, text='46', unit_value_id=33),
                    GLValue(position=12, value_id=12, text='47', unit_value_id=33),
                ],
            ),
            GLType(
                param_id=T.SHIRTS.GREG.HEIGHT_MAN_UNITS.ID,
                hid=T.SHIRTS.ID,
                gltype=GLType.ENUM,
                name=T.SHIRTS.GREG.HEIGHT_MAN_UNITS.XSL_NAME,
                values=[
                    GLValue(value_id=11, text='GREG'),
                    GLValue(value_id=22, text='INT'),
                    GLValue(value_id=33, text='RU'),
                ],
            ),
            GLType(
                name=T.SHIRTS.GREG.GIRTH_NECK.RUS_NAME,
                xslname=T.SHIRTS.GREG.GIRTH_NECK.XSL_NAME,
                param_id=T.SHIRTS.GREG.GIRTH_NECK.ID,
                hid=T.SHIRTS.ID,
                cluster_filter=True,
                model_filter_index=4,
                gltype=GLType.ENUM,
                subtype='size',
                values=[
                    GLValue(position=1, value_id=1, text='62'),
                    GLValue(position=2, value_id=2, text='63'),
                    GLValue(position=3, value_id=3, text='64'),
                    GLValue(position=4, value_id=4, text='65'),
                    GLValue(position=5, value_id=5, text='66'),
                    GLValue(position=6, value_id=6, text='67'),
                ],
            ),
            GLType(
                name=T.SHIRTS.GREG.COLOR.RUS_NAME,
                xslname=T.SHIRTS.GREG.COLOR.XSL_NAME,
                param_id=T.SHIRTS.GREG.COLOR.ID,
                hid=T.SHIRTS.ID,
                cluster_filter=True,
                model_filter_index=2,
                gltype=GLType.ENUM,
                values=[
                    GLValue(position=1, value_id=1, text='red'),
                    GLValue(position=2, value_id=2, text='green'),
                    GLValue(position=3, value_id=3, text='blue'),
                    GLValue(position=4, value_id=4, text='white'),
                    GLValue(position=5, value_id=5, text='black'),
                    GLValue(position=6, value_id=6, text='purple'),
                ],
            ),
            GLType(
                name=T.SHIRTS.GREG.TYPE.RUS_NAME,
                xslname=T.SHIRTS.GREG.TYPE.XSL_NAME,
                param_id=T.SHIRTS.GREG.TYPE.ID,
                hid=T.SHIRTS.ID,
                cluster_filter=True,
                model_filter_index=2,
                gltype=GLType.ENUM,
                values=[
                    GLValue(position=1, value_id=1, text='a'),
                    GLValue(position=2, value_id=2, text='b'),
                    GLValue(position=3, value_id=3, text='c'),
                    GLValue(position=4, value_id=4, text='d'),
                    GLValue(position=5, value_id=5, text='e'),
                    GLValue(position=6, value_id=6, text='f'),
                ],
            ),
            GLType(
                name=T.SKIRTS.ALBARI.HEIGHT_GIRL.RUS_NAME,
                xslname=T.SKIRTS.ALBARI.HEIGHT_GIRL.XSL_NAME,
                param_id=T.SKIRTS.ALBARI.HEIGHT_GIRL.ID,
                unit_param_id=T.SKIRTS.ALBARI.HEIGHT_GIRL_UNITS.ID,
                hid=T.SKIRTS.ID,
                cluster_filter=True,
                model_filter_index=3,
                gltype=GLType.ENUM,
                subtype='size',
                size_charts=[GLSizeChart(option_id=22), GLSizeChart(option_id=33)],
                values=[
                    # paramsposition=NT  unit INT
                    GLValue(position=1, value_id=1, text='M', unit_value_id=22),
                    GLValue(position=5, value_id=5, text='S', unit_value_id=22),
                    GLValue(position=7, value_id=7, text='L', unit_value_id=22),
                    # paramsposition=RU  unit EU
                    GLValue(position=4, value_id=4, text='32', unit_value_id=33),
                    GLValue(position=8, value_id=8, text='33', unit_value_id=33),
                    GLValue(position=9, value_id=9, text='34', unit_value_id=33),
                    GLValue(position=10, value_id=10, text='35', unit_value_id=33),
                    GLValue(position=11, value_id=11, text='36', unit_value_id=33),
                    GLValue(position=12, value_id=12, text='37', unit_value_id=33),
                ],
            ),
            GLType(
                param_id=T.SKIRTS.ALBARI.HEIGHT_GIRL_UNITS.ID,
                hid=T.SKIRTS.ID,
                gltype=GLType.ENUM,
                name=T.SKIRTS.ALBARI.HEIGHT_GIRL_UNITS.XSL_NAME,
                values=[
                    GLValue(value_id=22, text='INT'),
                    GLValue(value_id=33, text='EU'),
                ],
            ),
            GLType(
                name=T.SKIRTS.ALBARI.GIRTH_HIP.RUS_NAME,
                xslname=T.SKIRTS.ALBARI.GIRTH_HIP.XSL_NAME,
                param_id=T.SKIRTS.ALBARI.GIRTH_HIP.ID,
                unit_param_id=T.SKIRTS.ALBARI.GIRTH_HIP_UNITS.ID,
                hid=T.SKIRTS.ID,
                cluster_filter=True,
                model_filter_index=4,
                gltype=GLType.ENUM,
                subtype='size',
                values=[
                    GLValue(position=1, value_id=1, text='52', unit_value_id=11),
                    GLValue(position=2, value_id=2, text='53', unit_value_id=11),
                    GLValue(position=3, value_id=3, text='54', unit_value_id=11),
                    GLValue(position=4, value_id=4, text='55', unit_value_id=11),
                    GLValue(position=5, value_id=5, text='56', unit_value_id=11),
                    GLValue(position=6, value_id=6, text='57', unit_value_id=11),
                    GLValue(position=7, value_id=7, text='2', unit_value_id=22),
                    GLValue(position=8, value_id=8, text='3', unit_value_id=22),
                    GLValue(position=9, value_id=9, text='4', unit_value_id=22),
                    GLValue(position=10, value_id=10, text='5', unit_value_id=22),
                    GLValue(position=11, value_id=11, text='6', unit_value_id=22),
                    GLValue(position=12, value_id=12, text='7', unit_value_id=22),
                ],
            ),
            GLType(
                param_id=T.SKIRTS.ALBARI.GIRTH_HIP_UNITS.ID,
                hid=T.SKIRTS.ID,
                gltype=GLType.ENUM,
                name=T.SKIRTS.ALBARI.GIRTH_HIP_UNITS.XSL_NAME,
                values=[
                    GLValue(value_id=11, text='ALBARI'),
                    GLValue(value_id=22, text='US'),
                ],
            ),
            GLType(
                name=T.SOCKS.PUMA.HEIGHT_MAN.RUS_NAME,
                xslname=T.SOCKS.PUMA.HEIGHT_MAN.XSL_NAME,
                param_id=T.SOCKS.PUMA.HEIGHT_MAN.ID,
                unit_param_id=T.SOCKS.PUMA.HEIGHT_MAN_UNITS.ID,
                hid=T.SOCKS.ID,
                cluster_filter=True,
                model_filter_index=3,
                gltype=GLType.ENUM,
                subtype='size',
                values=[
                    # original glparams
                    GLValue(position=1, value_id=1, text='174-186', unit_value_id=11),
                    GLValue(position=2, value_id=2, text='186-192', unit_value_id=11),
                    GLValue(position=3, value_id=3, text='192-200', unit_value_id=11),
                    # paramsposition=NT  unit INT
                    GLValue(position=4, value_id=4, text='M', unit_value_id=22),
                    GLValue(position=5, value_id=5, text='S', unit_value_id=22),
                    GLValue(position=6, value_id=6, text='L', unit_value_id=22),
                    # paramsposition=RU  unit RU
                    GLValue(position=7, value_id=7, text='42', unit_value_id=33),
                    GLValue(position=8, value_id=8, text='43', unit_value_id=33),
                    GLValue(position=9, value_id=9, text='44', unit_value_id=33),
                    GLValue(position=10, value_id=10, text='45', unit_value_id=33),
                    GLValue(position=11, value_id=11, text='46', unit_value_id=33),
                    GLValue(position=12, value_id=12, text='47', unit_value_id=33),
                ],
            ),
            GLType(
                param_id=T.SOCKS.PUMA.HEIGHT_MAN_UNITS.ID,
                hid=T.SOCKS.ID,
                gltype=GLType.ENUM,
                name=T.SOCKS.PUMA.HEIGHT_MAN_UNITS.XSL_NAME,
                values=[
                    GLValue(value_id=11, text='PUMA', default=True),
                    GLValue(value_id=22, text='INT'),
                    GLValue(value_id=33, text='RU'),
                ],
            ),
            GLType(
                name=T.SOCKS.PUMA.GIRTH_FOOT.RUS_NAME,
                xslname=T.SOCKS.PUMA.GIRTH_FOOT.XSL_NAME,
                param_id=T.SOCKS.PUMA.GIRTH_FOOT.ID,
                hid=T.SOCKS.ID,
                cluster_filter=True,
                model_filter_index=4,
                gltype=GLType.ENUM,
                subtype='size',
                values=[
                    GLValue(position=1, value_id=1, text='12'),
                    GLValue(position=2, value_id=2, text='13'),
                    GLValue(position=3, value_id=3, text='14'),
                    GLValue(position=4, value_id=4, text='15'),
                    GLValue(position=5, value_id=5, text='16'),
                    GLValue(position=6, value_id=6, text='17'),
                ],
            ),
            GLType(
                name=T.SHIRTS.GREG.GIRTH_CHEST_MIN.RUS_NAME,
                xslname=T.SHIRTS.GREG.GIRTH_CHEST_MIN.XSL_NAME,
                param_id=T.SHIRTS.GREG.GIRTH_CHEST_MIN.ID,
                name_for_partner_new=T.SHIRTS.GREG.GIRTH_CHEST_MIN.NEW_PARTHNER_NAME,
                hid=T.SHIRTS.ID,
                model_filter_index=-1,
                gltype=GLType.NUMERIC,
                positionless=True,
                hidden=True,
                subtype='size',
            ),
            GLType(
                name=T.SHIRTS.GREG.GIRTH_CHEST_MAX.RUS_NAME,
                xslname=T.SHIRTS.GREG.GIRTH_CHEST_MAX.XSL_NAME,
                param_id=T.SHIRTS.GREG.GIRTH_CHEST_MAX.ID,
                name_for_partner_new=T.SHIRTS.GREG.GIRTH_CHEST_MAX.NEW_PARTHNER_NAME,
                hid=T.SHIRTS.ID,
                model_filter_index=-1,
                gltype=GLType.NUMERIC,
                positionless=True,
                hidden=True,
                subtype='size',
            ),
            GLType(
                name=T.PANTS.GLORIA.HEIGHT_MAN.RUS_NAME,
                xslname=T.PANTS.GLORIA.HEIGHT_MAN.XSL_NAME,
                param_id=T.PANTS.GLORIA.HEIGHT_MAN.ID,
                hid=T.PANTS.ID,
                cluster_filter=True,
                model_filter_index=4,
                gltype=GLType.ENUM,
                subtype='size',
                values=[
                    GLValue(value_id=1, text='120'),
                ],
            ),
            GLType(
                name=T.CATEGORY_1.BRAND_1.PARAM_1.RUS_NAME,
                xslname=T.CATEGORY_1.BRAND_1.PARAM_1.XSL_NAME,
                param_id=T.CATEGORY_1.BRAND_1.PARAM_1.ID,
                unit_param_id=T.CATEGORY_1.BRAND_1.PARAM_1_UNITS.ID,
                hid=T.CATEGORY_1.ID,
                cluster_filter=True,
                model_filter_index=3,
                gltype=GLType.ENUM,
                subtype='size',
                values=[
                    # original glparams
                    GLValue(position=1, value_id=1, text='174-186', unit_value_id=11),
                    GLValue(position=2, value_id=2, text='186-192', unit_value_id=11),
                    GLValue(position=3, value_id=3, text='192-200', unit_value_id=11),
                    # paramsposition=NT  unit INT
                    GLValue(position=4, value_id=4, text='M', unit_value_id=22),
                    GLValue(position=5, value_id=5, text='S', unit_value_id=22),
                    GLValue(position=6, value_id=6, text='L', unit_value_id=22),
                    # paramsposition=RU  unit RU
                    GLValue(position=7, value_id=7, text='42', unit_value_id=33),
                    GLValue(position=8, value_id=8, text='43', unit_value_id=33),
                    GLValue(position=9, value_id=9, text='44', unit_value_id=33),
                    GLValue(position=10, value_id=10, text='45', unit_value_id=33),
                    GLValue(position=11, value_id=11, text='46', unit_value_id=33),
                    GLValue(position=12, value_id=12, text='47', unit_value_id=33),
                ],
            ),
            GLType(
                param_id=T.CATEGORY_1.BRAND_1.PARAM_1_UNITS.ID,
                hid=T.CATEGORY_1.ID,
                gltype=GLType.ENUM,
                name=T.CATEGORY_1.BRAND_1.PARAM_1_UNITS.XSL_NAME,
                values=[
                    GLValue(value_id=11, text='BRAND_1', default=True),
                    GLValue(value_id=22, text='INT'),
                    GLValue(value_id=33, text='RU'),
                ],
            ),
            GLType(
                name=T.CATEGORY_2.BRAND_1.PARAM_1.RUS_NAME,
                xslname=T.CATEGORY_2.BRAND_1.PARAM_1.XSL_NAME,
                param_id=T.CATEGORY_2.BRAND_1.PARAM_1.ID,
                hid=T.CATEGORY_2.ID,
                cluster_filter=True,
                model_filter_index=3,
                gltype=GLType.ENUM,
                subtype='size',
                values=[
                    # original glparams
                    GLValue(position=1, value_id=1, text='174-186'),
                    GLValue(position=2, value_id=2, text='186-192'),
                    GLValue(position=3, value_id=3, text='192-200'),
                ],
            ),
            GLType(
                name=T.GLOVES.GLORIA.HEIGHT_GIRL.RUS_NAME,
                xslname=T.GLOVES.GLORIA.HEIGHT_GIRL.XSL_NAME,
                param_id=T.GLOVES.GLORIA.HEIGHT_GIRL.ID,
                unit_param_id=T.GLOVES.GLORIA.HEIGHT_GIRL_UNITS.ID,
                hid=T.GLOVES.ID,
                cluster_filter=True,
                model_filter_index=3,
                gltype=GLType.ENUM,
                subtype='size',
                values=[
                    # paramsposition=INT  unit INT
                    GLValue(value_id=5, text='S', unit_value_id=22, position=1),
                    GLValue(value_id=1, text='M', unit_value_id=22, position=2),
                    GLValue(value_id=7, text='L', unit_value_id=22, position=3),
                    # paramsposition=EU  unit EU
                    GLValue(value_id=4, text='32', unit_value_id=33, position=1),
                    GLValue(value_id=8, text='33', unit_value_id=33, position=2),
                    GLValue(value_id=9, text='34', unit_value_id=33, position=3),
                    GLValue(value_id=10, text='35', unit_value_id=33, position=4),
                    GLValue(value_id=11, text='36', unit_value_id=33, position=5),
                    GLValue(value_id=12, text='37', unit_value_id=33, position=6),
                    # paramsposition=RU  unit RU
                    GLValue(value_id=15, text='42', unit_value_id=44, position=1),
                    GLValue(value_id=19, text='43', unit_value_id=44, position=2),
                    GLValue(value_id=18, text='44', unit_value_id=44, position=3),
                    GLValue(value_id=17, text='45', unit_value_id=44, position=4),
                    GLValue(value_id=16, text='46', unit_value_id=44, position=5),
                    GLValue(value_id=20, text='47', unit_value_id=44, position=6),
                ],
            ),
            GLType(
                param_id=T.GLOVES.GLORIA.HEIGHT_GIRL_UNITS.ID,
                hid=T.GLOVES.ID,
                gltype=GLType.ENUM,
                name=T.GLOVES.GLORIA.HEIGHT_GIRL_UNITS.XSL_NAME,
                values=[
                    GLValue(value_id=22, text='INT'),
                    GLValue(value_id=33, text='EU'),
                    GLValue(value_id=44, text='RU', default=True),
                ],
            ),
            GLType(
                name=T.CATEGORY_3.BRAND_1.PARAM_1.RUS_NAME,
                xslname=T.CATEGORY_3.BRAND_1.PARAM_1.XSL_NAME,
                param_id=T.CATEGORY_3.BRAND_1.PARAM_1.ID,
                hid=T.CATEGORY_3.ID,
                cluster_filter=True,
                model_filter_index=4,
                gltype=GLType.ENUM,
                subtype='size',
                values=[],
            ),
            GLType(
                name=T.CATEGORY_3.BRAND_1.PARAM_2.RUS_NAME,
                xslname=T.CATEGORY_3.BRAND_1.PARAM_2.XSL_NAME,
                param_id=T.CATEGORY_3.BRAND_1.PARAM_2.ID,
                unit_param_id=T.CATEGORY_3.BRAND_1.PARAM_2_UNITS.ID,
                hid=T.CATEGORY_3.ID,
                cluster_filter=True,
                model_filter_index=-1,
                gltype=GLType.ENUM,
                subtype='size',
                values=[
                    # original glparams
                    GLValue(position=1, value_id=1, text='174-186', unit_value_id=11),
                    GLValue(position=2, value_id=2, text='186-192', unit_value_id=11),
                    GLValue(position=3, value_id=3, text='192-200', unit_value_id=11),
                    # paramsposition=NT  unit INT
                    GLValue(position=4, value_id=4, text='S', unit_value_id=22),
                    GLValue(position=5, value_id=5, text='M', unit_value_id=22),
                    GLValue(position=6, value_id=6, text='L', unit_value_id=22),
                    # paramsposition=RU  unit RU
                    GLValue(position=7, value_id=7, text='42', unit_value_id=33),
                    GLValue(position=8, value_id=8, text='43', unit_value_id=33),
                    GLValue(position=9, value_id=9, text='44', unit_value_id=33),
                    GLValue(position=10, value_id=10, text='45', unit_value_id=33),
                    GLValue(position=11, value_id=11, text='46', unit_value_id=33),
                    GLValue(position=12, value_id=12, text='47', unit_value_id=33),
                ],
            ),
            GLType(
                param_id=T.CATEGORY_3.BRAND_1.PARAM_2_UNITS.ID,
                hid=T.CATEGORY_3.ID,
                gltype=GLType.ENUM,
                name=T.CATEGORY_3.BRAND_1.PARAM_2_UNITS.XSL_NAME,
                values=[
                    GLValue(value_id=11, text='BRAND_1', default=True),
                    GLValue(value_id=22, text='INT'),
                    GLValue(value_id=33, text='RU'),
                ],
            ),
            GLType(
                name=T.CATEGORY_4.BRAND_1.PARAM.RUS_NAME,
                xslname=T.CATEGORY_4.BRAND_1.PARAM.XSL_NAME,
                param_id=T.CATEGORY_4.BRAND_1.PARAM.ID,
                unit_param_id=T.CATEGORY_4.BRAND_1.PARAM_UNITS.ID,
                hid=T.CATEGORY_4.ID,
                cluster_filter=True,
                model_filter_index=3,
                gltype=GLType.ENUM,
                subtype='size',
                values=[
                    # original glparams
                    GLValue(position=1, value_id=1, text='174-176', unit_value_id=11),
                    GLValue(position=2, value_id=2, text='176-178', unit_value_id=11),
                    GLValue(position=3, value_id=3, text='178-180', unit_value_id=11),
                    GLValue(position=4, value_id=4, text='180-182', unit_value_id=11),
                    # paramsposition=NT  unit INT
                    GLValue(position=5, value_id=5, text='XS', unit_value_id=22),
                    GLValue(position=6, value_id=6, text='S', unit_value_id=22),
                    GLValue(position=7, value_id=7, text='M', unit_value_id=22),
                    GLValue(position=8, value_id=8, text='L', unit_value_id=22),
                    GLValue(position=9, value_id=9, text='XL', unit_value_id=22),
                ],
            ),
            GLType(
                param_id=T.CATEGORY_4.BRAND_1.PARAM_UNITS.ID,
                hid=T.CATEGORY_4.ID,
                gltype=GLType.ENUM,
                name=T.CATEGORY_4.BRAND_1.PARAM_UNITS.XSL_NAME,
                values=[
                    GLValue(value_id=11, text='BRAND_1', default=True),
                    GLValue(value_id=22, text='INT'),
                ],
            ),
        ]

        def shirt_height(value):
            return GLParam(param_id=T.SHIRTS.GREG.HEIGHT_MAN.ID, value=value)

        def shirt_girth(value):
            return GLParam(param_id=T.SHIRTS.GREG.GIRTH_NECK.ID, value=value)

        def shirt_color(value):
            return GLParam(param_id=T.SHIRTS.GREG.COLOR.ID, value=value)

        def shirt_type(value):
            return GLParam(param_id=T.SHIRTS.GREG.TYPE.ID, value=value)

        def shirt_chest_min(value):
            return GLParam(param_id=T.SHIRTS.GREG.GIRTH_CHEST_MIN.ID, value=value)

        def shirt_chest_max(value):
            return GLParam(param_id=T.SHIRTS.GREG.GIRTH_CHEST_MAX.ID, value=value)

        def skirt_height(value):
            return GLParam(param_id=T.SKIRTS.ALBARI.HEIGHT_GIRL.ID, value=value)

        def skirt_height_hypothesis(value):
            return GLParam(
                param_id=T.SKIRTS.ALBARI.HEIGHT_GIRL.ID, string_value=value, is_filter=False, is_hypothesis=True
            )

        def skirt_girth(value):
            return GLParam(param_id=T.SKIRTS.ALBARI.GIRTH_HIP.ID, value=value)

        def socks_height(value):
            return GLParam(param_id=T.SOCKS.PUMA.HEIGHT_MAN.ID, value=value)

        def socks_girth(value):
            return GLParam(param_id=T.SOCKS.PUMA.GIRTH_FOOT.ID, value=value)

        def gen_many_shirt_skus_with_parameters():

            params_base = [
                (
                    [
                        shirt_height(1),
                        shirt_height(4),
                        shirt_height(5),
                        shirt_height(7),
                        shirt_height(8),
                        shirt_girth(1),
                        shirt_chest_min(40),
                        shirt_chest_max(45),
                    ],
                    [shirt_height(1)],
                ),
                (
                    [
                        shirt_height(2),
                        shirt_height(6),
                        shirt_height(9),
                        shirt_height(10),
                        shirt_height(11),
                        shirt_girth(2),
                        shirt_chest_min(46),
                        shirt_chest_max(50),
                    ],
                    [shirt_height(2)],
                ),
            ]

            def gen_parameters(sizes, func, range):
                result = []
                for base, orig in sizes:
                    for n in range:
                        new_base = [] + base
                        new_orig = [] + orig
                        new_base.append(func(n))
                        result.append((new_base, new_orig))
                return result

            return gen_parameters(
                gen_parameters(params_base, shirt_type, list(range(1, 6))), shirt_color, list(range(1, 6))
            )

        cls.index.models += [
            make_model(T.SHIRTS.GREG.ID, T.SHIRTS.ID, cls.index.mskus, gen_many_shirt_skus_with_parameters()),
            make_model(
                T.SKIRTS.ALBARI.ID,
                T.SKIRTS.ID,
                cls.index.mskus,
                [
                    # glparams
                    (
                        [
                            skirt_height_hypothesis('124-136'),
                            skirt_height(1),
                            skirt_height(5),
                            skirt_height(4),
                            skirt_height(8),
                            skirt_girth(1),
                            skirt_girth(7),
                            skirt_girth(8),
                        ],
                        [],
                    ),
                    (
                        [
                            skirt_height_hypothesis('136-142'),
                            skirt_height(7),
                            skirt_height(9),
                            skirt_height(10),
                            skirt_height(11),
                            skirt_girth(2),
                            skirt_girth(9),
                            skirt_girth(10),
                        ],
                        [],
                    ),
                ],
            ),
            make_model(
                T.SOCKS.PUMA.ID,
                T.SOCKS.ID,
                cls.index.mskus,
                [
                    # glparams                                                                                   original glparams
                    (
                        [
                            socks_height(1),
                            socks_height(8),
                            socks_girth(1),
                        ],
                        [socks_height(1)],
                    ),
                    (
                        [
                            socks_height(2),
                            socks_height(6),
                            socks_height(9),
                            socks_height(10),
                            socks_girth(2),
                        ],
                        [socks_height(2)],
                    ),
                ],
            ),
        ]

    def test_size_table_info(self):
        """
        Проверяем первоначальный вариант таблицы размеров
        """
        response = self.report.request_json(
            'place=productoffers&hyperid={}&pp=18&hid={}'.format(T.SHIRTS.GREG.ID, T.SHIRTS.ID)
        )
        self.assertFragmentIn(
            response,
            {
                "sizes_table": {
                    "header": [
                        {
                            "unit_name": T.SHIRTS.GREG.HEIGHT_MAN.RUS_NAME,
                        },
                        {
                            "unit_name": "INT",
                        },
                        {
                            "unit_name": "RU",
                        },
                        {
                            "unit_name": T.SHIRTS.GREG.GIRTH_NECK.RUS_NAME,
                        },
                    ],
                    "msku_list": [
                        {
                            "values": [
                                {
                                    "unit_name": T.SHIRTS.GREG.HEIGHT_MAN.RUS_NAME,
                                    "value_min": "174-186",
                                    "value_max": "174-186",
                                },
                                {"unit_name": "INT", "value_min": "M", "value_max": "S"},
                                {"unit_name": "RU", "value_min": "42", "value_max": "43"},
                                {"unit_name": T.SHIRTS.GREG.GIRTH_NECK.RUS_NAME, "value_min": "62", "value_max": "62"},
                            ]
                        },
                        {
                            "values": [
                                {
                                    "unit_name": T.SHIRTS.GREG.HEIGHT_MAN.RUS_NAME,
                                    "value_min": "186-192",
                                    "value_max": "186-192",
                                },
                                {"unit_name": "INT", "value_min": "L", "value_max": "L"},
                                {"unit_name": "RU", "value_min": "44", "value_max": "46"},
                                {"unit_name": T.SHIRTS.GREG.GIRTH_NECK.RUS_NAME, "value_min": "63", "value_max": "63"},
                            ]
                        },
                    ],
                }
            },
            preserve_order=True,
        )

    def test_unordered_params_size_table_info(self):
        """
        Проверяем построение таблицы размеров на не осортированных данных, когда юниты перемешаны INT EU параметре T.SKIRTS.ALBARI.HEIGHT_GIRL
        """
        response = self.report.request_json(
            'place=productoffers&hyperid={}&pp=18&hid={}'.format(T.SKIRTS.ALBARI.ID, T.SKIRTS.ID)
            + '&rearr-factors=use_new_jump_table_pipeline=1'
        )
        self.assertFragmentIn(
            response,
            {
                "sizes_table": {
                    "header": [
                        {
                            "unit_name": T.SKIRTS.ALBARI.HEIGHT_GIRL.RUS_NAME,
                        },
                        {
                            "unit_name": "EU",
                        },
                        {
                            "unit_name": "INT",
                        },
                        {
                            "unit_name": T.SKIRTS.ALBARI.GIRTH_HIP.RUS_NAME,
                        },
                        {"unit_name": "US"},
                    ],
                    "msku_list": [
                        {
                            "values": [
                                {
                                    "unit_name": T.SKIRTS.ALBARI.HEIGHT_GIRL.RUS_NAME,
                                    "value_min": "124-136",
                                    "value_max": "124-136",
                                },
                                {"unit_name": "EU", "value_min": "32", "value_max": "33"},
                                {"unit_name": "INT", "value_min": "M", "value_max": "S"},
                                {"unit_name": T.SKIRTS.ALBARI.GIRTH_HIP.RUS_NAME, "value_min": "52", "value_max": "52"},
                                {"unit_name": "US", "value_min": "2", "value_max": "3"},
                            ]
                        },
                        {
                            "values": [
                                {
                                    "unit_name": T.SKIRTS.ALBARI.HEIGHT_GIRL.RUS_NAME,
                                    "value_min": "136-142",
                                    "value_max": "136-142",
                                },
                                {"unit_name": "EU", "value_min": "34", "value_max": "36"},
                                {"unit_name": "INT", "value_min": "L", "value_max": "L"},
                                {"unit_name": T.SKIRTS.ALBARI.GIRTH_HIP.RUS_NAME, "value_min": "53", "value_max": "53"},
                                {"unit_name": "US", "value_min": "4", "value_max": "5"},
                            ]
                        },
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_new_size_table_info(cls):
        def ShirtsHeight(value):
            return GLParam(param_id=T.SHIRTS.GREG.HEIGHT_MAN.ID, value=value)

        def ShirtsGirth(value):
            return GLParam(param_id=T.SHIRTS.GREG.GIRTH_NECK.ID, value=value)

        cls.index.models += [
            make_model(
                T.SHIRTS.GREG.SECOND_ID,
                T.SHIRTS.SECOND_ID,
                cls.index.mskus,
                [
                    # glparams                                                                       original glparams
                    (
                        [
                            ShirtsHeight(2),
                            ShirtsHeight(1),
                            ShirtsHeight(5),
                            ShirtsHeight(4),
                            ShirtsHeight(8),
                            ShirtsGirth(1),
                        ],
                        [
                            ShirtsHeight(2),
                        ],
                    ),
                    (
                        [
                            ShirtsHeight(3),
                            ShirtsHeight(7),
                            ShirtsHeight(9),
                            ShirtsHeight(10),
                            ShirtsHeight(11),
                            ShirtsGirth(2),
                        ],
                        [
                            ShirtsHeight(3),
                        ],
                    ),
                ],
            ),
        ]

    def test_new_size_table_info(self):
        """
        Проверяем построени таблицы размеров через новый пайплайн на данных для первого варианта таблицы
        """
        request = (
            'place=productoffers&hyperid={}&pp=18&hid={}'.format(T.SHIRTS.GREG.ID, T.SHIRTS.ID)
        ) + '&rearr-factors=use_new_jump_table_pipeline=1'
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "sizes_table": {
                    "header": [
                        {
                            "unit_name": T.SHIRTS.GREG.HEIGHT_MAN.RUS_NAME,
                        },
                        {
                            "unit_name": "INT",
                        },
                        {
                            "unit_name": "RU",
                        },
                        {
                            "unit_name": T.SHIRTS.GREG.GIRTH_NECK.RUS_NAME,
                        },
                    ],
                    "msku_list": [
                        {
                            "values": [
                                {
                                    "unit_name": T.SHIRTS.GREG.HEIGHT_MAN.RUS_NAME,
                                    "value_min": "174-186",
                                    "value_max": "174-186",
                                },
                                {"unit_name": "INT", "value_min": "M", "value_max": "S"},
                                {"unit_name": "RU", "value_min": "42", "value_max": "43"},
                                {"unit_name": T.SHIRTS.GREG.GIRTH_NECK.RUS_NAME, "value_min": "62", "value_max": "62"},
                            ]
                        },
                        {
                            "values": [
                                {
                                    "unit_name": T.SHIRTS.GREG.HEIGHT_MAN.RUS_NAME,
                                    "value_min": "186-192",
                                    "value_max": "186-192",
                                },
                                {"unit_name": "INT", "value_min": "L", "value_max": "L"},
                                {"unit_name": "RU", "value_min": "44", "value_max": "46"},
                                {"unit_name": T.SHIRTS.GREG.GIRTH_NECK.RUS_NAME, "value_min": "63", "value_max": "63"},
                            ]
                        },
                    ],
                }
            },
        )

    def test_originalsubtype_in_jump_table(self):
        """
        subType в карте переходов очищается, что бы построить таблицу по карте переходов размера, нам нужен этот признак, но что бы не ломать старые кейсы
        используется originalSubType который передается как есть
        """
        response = self.report.request_json(
            'place=productoffers&hyperid={}&pp=18&hid={}'.format(T.SKIRTS.ALBARI.ID, T.SKIRTS.ID)
        )
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": "1",
                        "name": T.SKIRTS.ALBARI.HEIGHT_GIRL.RUS_NAME,
                        "subType": "",
                        "originalSubType": "size",
                    },
                    {
                        "id": "3",
                        "name": T.SKIRTS.ALBARI.GIRTH_HIP.RUS_NAME,
                        "subType": "",
                        "originalSubType": "size",
                    },
                ]
            },
        )

    def test_size_header_info(self):
        """
        Проверяем, что при наличии меньшего количества параметров у первого оффера заголовок будет иметь корректное количество параметров
        """
        response = self.report.request_json(
            'place=productoffers&hyperid={}&pp=18&hid={}'.format(T.SOCKS.PUMA.ID, T.SOCKS.ID)
        )
        self.assertFragmentIn(
            response,
            {
                "sizes_table": {
                    "header": [
                        {
                            "unit_name": T.SOCKS.PUMA.HEIGHT_MAN.RUS_NAME,
                        },
                        {
                            "unit_name": "INT",
                        },
                        {
                            "unit_name": "RU",
                        },
                        {
                            "unit_name": T.SOCKS.PUMA.GIRTH_FOOT.RUS_NAME,
                        },
                    ],
                    "msku_list": [
                        {
                            "values": [
                                {
                                    "unit_name": T.SOCKS.PUMA.HEIGHT_MAN.RUS_NAME,
                                    "value_min": "174-186",
                                    "value_max": "174-186",
                                },
                                {"unit_name": "RU", "value_min": "43", "value_max": "43"},
                                {"unit_name": T.SOCKS.PUMA.GIRTH_FOOT.RUS_NAME, "value_min": "12", "value_max": "12"},
                            ]
                        },
                        {
                            "values": [
                                {
                                    "unit_name": T.SOCKS.PUMA.HEIGHT_MAN.RUS_NAME,
                                    "value_min": "186-192",
                                    "value_max": "186-192",
                                },
                                {"unit_name": "INT", "value_min": "L", "value_max": "L"},
                                {"unit_name": "RU", "value_min": "44", "value_max": "45"},
                                {"unit_name": T.SOCKS.PUMA.GIRTH_FOOT.RUS_NAME, "value_min": "13", "value_max": "13"},
                            ]
                        },
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_size_table_measures(self):
        """
        Проверяем мерки в таблице размеров, и наличие типа значения
        """
        response = self.report.request_json(
            'place=productoffers&hyperid={}&pp=18&hid={}&fb-based-size-table=1'.format(T.SHIRTS.GREG.ID, T.SHIRTS.ID)
        )

        self.assertFragmentIn(
            response,
            {
                "sizes_table": {
                    "header": [
                        {
                            "unit_name": T.SHIRTS.GREG.HEIGHT_MAN.RUS_NAME,
                            "param_type": "size",
                            "param_id": T.SHIRTS.GREG.HEIGHT_MAN.ID,
                        },
                        {
                            "unit_name": "INT",
                            "param_type": "unit",
                            "param_id": T.SHIRTS.GREG.HEIGHT_MAN.ID,
                        },
                        {
                            "unit_name": "RU",
                            "param_type": "unit",
                            "param_id": T.SHIRTS.GREG.HEIGHT_MAN.ID,
                        },
                        {
                            "unit_name": T.SHIRTS.GREG.GIRTH_NECK.RUS_NAME,
                            "param_type": "size",
                            "param_id": T.SHIRTS.GREG.GIRTH_NECK.ID,
                        },
                        {
                            "unit_name": "Обхват груди",
                            "param_type": "measure",
                            "param_id": 0,
                        },
                    ],
                    "msku_list": [
                        {
                            "values": [
                                {
                                    "unit_name": T.SHIRTS.GREG.HEIGHT_MAN.RUS_NAME,
                                    "value_min": "174-186",
                                    "value_max": "174-186",
                                },
                                {"unit_name": "INT", "value_min": "M", "value_max": "S"},
                                {"unit_name": "RU", "value_min": "42", "value_max": "43"},
                                {"unit_name": T.SHIRTS.GREG.GIRTH_NECK.RUS_NAME, "value_min": "62", "value_max": "62"},
                                {"unit_name": "Обхват груди", "value_min": "40", "value_max": "45"},
                            ]
                        },
                        {
                            "values": [
                                {
                                    "unit_name": T.SHIRTS.GREG.HEIGHT_MAN.RUS_NAME,
                                    "value_min": "186-192",
                                    "value_max": "186-192",
                                },
                                {"unit_name": "INT", "value_min": "L", "value_max": "L"},
                                {"unit_name": "RU", "value_min": "44", "value_max": "46"},
                                {"unit_name": T.SHIRTS.GREG.GIRTH_NECK.RUS_NAME, "value_min": "63", "value_max": "63"},
                                {"unit_name": "Обхват груди", "value_min": "46", "value_max": "50"},
                            ]
                        },
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_measure_and_units(cls):
        cls.index.gltypes += [
            GLType(
                is_hard=True,
                name=T.BLOUSES.VALENTINO.HEIGHT_GIRL_UNITED.RUS_NAME,
                xslname=T.BLOUSES.VALENTINO.HEIGHT_GIRL_UNITED.XSL_NAME,
                param_id=T.BLOUSES.VALENTINO.HEIGHT_GIRL_UNITED.ID,
                hid=T.BLOUSES.ID,
                cluster_filter=True,
                model_filter_index=3,
                gltype=GLType.ENUM,
                subtype='size',
            ),
            GLType(
                name=T.BLOUSES.VALENTINO.HEIGHT_GIRL.RUS_NAME,
                xslname=T.BLOUSES.VALENTINO.HEIGHT_GIRL.XSL_NAME,
                param_id=T.BLOUSES.VALENTINO.HEIGHT_GIRL.ID,
                unit_param_id=T.BLOUSES.VALENTINO.HEIGHT_GIRL_UNITS.ID,
                hid=T.BLOUSES.ID,
                cluster_filter=True,
                model_filter_index=-1,
                gltype=GLType.ENUM,
                subtype='size',
                values=[
                    # original glparams
                    GLValue(position=2, value_id=2, text='224-236', unit_value_id=11),
                    GLValue(position=3, value_id=3, text='236-242', unit_value_id=11),
                    GLValue(position=6, value_id=6, text='242-290', unit_value_id=11),
                    # unit INT
                    GLValue(position=1, value_id=1, text='XM', unit_value_id=22),
                    GLValue(position=5, value_id=5, text='XS', unit_value_id=22),
                    GLValue(position=7, value_id=7, text='XL', unit_value_id=22),
                    # unit DE
                    GLValue(position=4, value_id=4, text='52', unit_value_id=33),
                    GLValue(position=8, value_id=8, text='53', unit_value_id=33),
                    GLValue(position=9, value_id=9, text='54', unit_value_id=33),
                    GLValue(position=10, value_id=10, text='55', unit_value_id=33),
                    GLValue(position=11, value_id=11, text='56', unit_value_id=33),
                    GLValue(position=12, value_id=12, text='57', unit_value_id=33),
                ],
            ),
            GLType(
                param_id=T.BLOUSES.VALENTINO.HEIGHT_GIRL_UNITS.ID,
                hid=T.BLOUSES.ID,
                gltype=GLType.ENUM,
                name=T.BLOUSES.VALENTINO.HEIGHT_GIRL_UNITS.XSL_NAME,
                values=[
                    GLValue(value_id=11, text='VALENTINO', default=True),
                    GLValue(value_id=22, text='INT'),
                    GLValue(value_id=33, text='DE'),
                ],
            ),
            GLType(
                is_hard=True,
                name=T.BLOUSES.VALENTINO.HARD_HEIGHT_GIRL.RUS_NAME,
                xslname=T.BLOUSES.VALENTINO.HARD_HEIGHT_GIRL.XSL_NAME,
                param_id=T.BLOUSES.VALENTINO.HARD_HEIGHT_GIRL.ID,
                unit_param_id=T.BLOUSES.VALENTINO.HARD_HEIGHT_GIRL_UNITS.ID,
                hid=T.BLOUSES.ID,
                cluster_filter=True,
                model_filter_index=-1,
                gltype=GLType.ENUM,
                subtype='size',
                values=[
                    # original glparams
                    GLValue(value_id=2, text='224-236', unit_value_id=11),
                    GLValue(value_id=3, text='236-242', unit_value_id=11),
                    GLValue(value_id=6, text='242-290', unit_value_id=11),
                    # unit INT
                    GLValue(value_id=1, text='XM', unit_value_id=22),
                    GLValue(value_id=5, text='XS', unit_value_id=22),
                    GLValue(value_id=7, text='XL', unit_value_id=22),
                    # unit DE
                    GLValue(value_id=4, text='52', unit_value_id=33),
                    GLValue(value_id=8, text='53', unit_value_id=33),
                    GLValue(value_id=9, text='54', unit_value_id=33),
                    GLValue(value_id=10, text='55', unit_value_id=33),
                    GLValue(value_id=11, text='56', unit_value_id=33),
                    GLValue(value_id=12, text='57', unit_value_id=33),
                ],
            ),
            GLType(
                param_id=T.BLOUSES.VALENTINO.HARD_HEIGHT_GIRL_UNITS.ID,
                hid=T.BLOUSES.ID,
                gltype=GLType.ENUM,
                name=T.BLOUSES.VALENTINO.HARD_HEIGHT_GIRL_UNITS.XSL_NAME,
                values=[
                    GLValue(value_id=11, text='VALENTINO', default=True),
                    GLValue(value_id=22, text='INT'),
                    GLValue(value_id=33, text='DE'),
                ],
            ),
            GLType(
                is_hard=True,
                name=T.BLOUSES.VALENTINO.GIRTH_WRIST_MIN.RUS_NAME,
                xslname=T.BLOUSES.VALENTINO.GIRTH_WRIST_MIN.XSL_NAME,
                param_id=T.BLOUSES.VALENTINO.GIRTH_WRIST_MIN.ID,
                hid=T.BLOUSES.ID,
                model_filter_index=-1,
                gltype=GLType.NUMERIC,
                positionless=True,
                hidden=True,
                subtype='size',
            ),
            GLType(
                is_hard=True,
                name=T.BLOUSES.VALENTINO.GIRTH_WRIST_MAX.RUS_NAME,
                xslname=T.BLOUSES.VALENTINO.GIRTH_WRIST_MAX.XSL_NAME,
                param_id=T.BLOUSES.VALENTINO.GIRTH_WRIST_MAX.ID,
                hid=T.BLOUSES.ID,
                model_filter_index=-1,
                gltype=GLType.NUMERIC,
                positionless=True,
                hidden=True,
                subtype='size',
            ),
        ]

        def blouses_height_united(value):
            return GLParam(
                param_id=T.BLOUSES.VALENTINO.HEIGHT_GIRL_UNITED.ID,
                string_value=value,
                is_filter=False,
                is_hypothesis=True,
            )

        def hard_blouses_height(value):
            return GLParam(param_id=T.BLOUSES.VALENTINO.HARD_HEIGHT_GIRL.ID, value=value)

        def blouses_height(value):
            return GLParam(param_id=T.BLOUSES.VALENTINO.HEIGHT_GIRL.ID, value=value)

        def blouses_girth_min(value):
            return GLParam(param_id=T.BLOUSES.VALENTINO.GIRTH_WRIST_MIN.ID, value=value)

        def blouses_girth_max(value):
            return GLParam(param_id=T.BLOUSES.VALENTINO.GIRTH_WRIST_MAX.ID, value=value)

        cls.index.models += [
            make_model(
                T.BLOUSES.VALENTINO.ID,
                T.BLOUSES.ID,
                cls.index.mskus,
                [
                    # glparams                                                                       original glparams
                    (
                        [
                            blouses_height_united("174-186"),
                            blouses_height(1),
                            blouses_height(5),
                            blouses_height(4),
                            blouses_height(8),
                            blouses_height(2),
                            blouses_girth_min(42),
                            blouses_girth_max(46),
                            hard_blouses_height(1),
                            hard_blouses_height(5),
                            hard_blouses_height(4),
                            hard_blouses_height(8),
                        ],
                        [blouses_height(2)],
                    ),
                    (
                        [
                            blouses_height_united("186-192"),
                            blouses_height(7),
                            blouses_height(9),
                            blouses_height(10),
                            blouses_height(11),
                            blouses_height(3),
                            blouses_girth_min(46),
                            blouses_girth_max(50),
                            hard_blouses_height(7),
                            hard_blouses_height(9),
                            hard_blouses_height(10),
                            hard_blouses_height(11),
                        ],
                        [blouses_height(3)],
                    ),
                ],
            ),
        ]

    def test_measure_and_units(self):
        """
        Проверяем мерки и сконверченые значения скрытого размера
        и скрытые "хард" параметры не отображются
        """
        response = self.report.request_json(
            'place=productoffers&hyperid={}&pp=18&hid={}&fb-based-size-table=1&rearr-factors=market_unit_info_in_size_table=1'.format(
                T.BLOUSES.VALENTINO.ID, T.BLOUSES.ID
            )
        )
        self.assertFragmentIn(
            response,
            {
                "sizes_table": {
                    "header": [
                        {
                            "unit_name": T.BLOUSES.VALENTINO.HEIGHT_GIRL_UNITED.RUS_NAME,
                            "param_name": T.BLOUSES.VALENTINO.HEIGHT_GIRL_UNITED.RUS_NAME,
                            "param_type": "size",
                            "param_id": T.BLOUSES.VALENTINO.HEIGHT_GIRL_UNITED.ID,
                        },
                        {
                            "unit_name": "DE",
                            "param_name": T.BLOUSES.VALENTINO.HEIGHT_GIRL.RUS_NAME,
                            "param_type": "unit",
                            "param_id": T.BLOUSES.VALENTINO.HEIGHT_GIRL.ID,
                        },
                        {
                            "unit_name": "INT",
                            "param_name": T.BLOUSES.VALENTINO.HEIGHT_GIRL.RUS_NAME,
                            "param_type": "unit",
                            "param_id": T.BLOUSES.VALENTINO.HEIGHT_GIRL.ID,
                        },
                        {
                            "unit_name": "Обхват запястья",
                            "param_name": "Обхват запястья",
                            "param_type": "measure",
                            "param_id": 0,
                        },
                    ],
                    "msku_list": [
                        {
                            "values": [
                                {
                                    "unit_name": T.BLOUSES.VALENTINO.HEIGHT_GIRL_UNITED.RUS_NAME,
                                    "value_min": "174-186",
                                    "value_max": "174-186",
                                },
                                {"unit_name": "DE", "value_min": "52", "value_max": "53"},
                                {"unit_name": "INT", "value_min": "XM", "value_max": "XS"},
                                {"unit_name": "Обхват запястья", "value_min": "42", "value_max": "46"},
                            ]
                        },
                        {
                            "values": [
                                {
                                    "unit_name": T.BLOUSES.VALENTINO.HEIGHT_GIRL_UNITED.RUS_NAME,
                                    "value_min": "186-192",
                                    "value_max": "186-192",
                                },
                                {"unit_name": "DE", "value_min": "54", "value_max": "56"},
                                {"unit_name": "INT", "value_min": "XL", "value_max": "XL"},
                                {"unit_name": "Обхват запястья", "value_min": "46", "value_max": "50"},
                            ]
                        },
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_measure_and_units_disabled(self):
        """
        Проверяем мерки и сконверченые значения скрытого размера не отображаются по умолчанию
        """
        response = self.report.request_json(
            'place=productoffers&hyperid={}&pp=18&hid={}&fb-based-size-table=1'.format(
                T.BLOUSES.VALENTINO.ID, T.BLOUSES.ID
            )
        )
        self.assertFragmentIn(
            response,
            {
                "sizes_table": {
                    "header": [
                        {
                            "unit_name": T.BLOUSES.VALENTINO.HEIGHT_GIRL_UNITED.RUS_NAME,
                            "param_type": "size",
                            "param_id": T.BLOUSES.VALENTINO.HEIGHT_GIRL_UNITED.ID,
                        },
                        {
                            "unit_name": "Обхват запястья",
                            "param_type": "measure",
                            "param_id": 0,
                        },
                    ],
                    "msku_list": [
                        {
                            "values": [
                                {
                                    "unit_name": T.BLOUSES.VALENTINO.HEIGHT_GIRL_UNITED.RUS_NAME,
                                    "value_min": "174-186",
                                    "value_max": "174-186",
                                },
                                {"unit_name": "Обхват запястья", "value_min": "42", "value_max": "46"},
                            ]
                        },
                        {
                            "values": [
                                {
                                    "unit_name": T.BLOUSES.VALENTINO.HEIGHT_GIRL_UNITED.RUS_NAME,
                                    "value_min": "186-192",
                                    "value_max": "186-192",
                                },
                                {"unit_name": "Обхват запястья", "value_min": "46", "value_max": "50"},
                            ]
                        },
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_one_record_table(cls):
        def pants_height(value):
            return GLParam(param_id=T.PANTS.GLORIA.HEIGHT_MAN.ID, value=value)

        cls.index.models += [
            make_model(
                T.PANTS.GLORIA.ID,
                T.PANTS.ID,
                cls.index.mskus,
                [
                    (
                        # glparams
                        [
                            pants_height(1),
                        ],
                        # original glparams
                        [
                            pants_height(1),
                        ],
                    ),
                ],
                one_record_gloria_offers,
            ),
        ]

        cls.index.fashion_categories += [
            FashionCategory("PANTS", T.PANTS.ID),
        ]

    def test_one_record_table(self):
        """
        Проверяем построени таблицы размеров даже когда всего 1 скю.
        Так же должна присутствовать карта переходов в виде фильтра с 1 значением
        """
        request = 'place=productoffers&hyperid={}&pp=18&hid={}'.format(T.PANTS.GLORIA.ID, T.PANTS.ID)
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "sizes_table": {
                    "header": [
                        {
                            "unit_name": T.PANTS.GLORIA.HEIGHT_MAN.RUS_NAME,
                        },
                    ],
                    "msku_list": [
                        {
                            "values": [
                                {
                                    "unit_name": T.PANTS.GLORIA.HEIGHT_MAN.RUS_NAME,
                                    "value_min": "120",
                                    "value_max": "120",
                                },
                            ]
                        },
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": str(T.PANTS.GLORIA.HEIGHT_MAN.ID),
                        "name": T.PANTS.GLORIA.HEIGHT_MAN.RUS_NAME,
                        "originalSubType": "size",
                        "type": "enum",
                        "values": [{"found": 1, "value": "120"}],
                        "valuesCount": 1,
                        "xslname": T.PANTS.GLORIA.HEIGHT_MAN.XSL_NAME,
                    }
                ]
            },
        )

    def test_one_record_table_in_offerinfo(self):
        """
        Проверяем, что на оффер инфо нам придет таблица размеров для оффера
        """
        request = 'place=offerinfo&offerid={}&rids=213&show-urls=cpa,external,phone,showPhone&regset=1&add-offerinfo-jump-and-size-table=1'.format(
            one_record_gloria_offers[0].waremd5
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "sizes_table": {
                            "header": [
                                {
                                    "unit_name": T.PANTS.GLORIA.HEIGHT_MAN.RUS_NAME,
                                },
                            ],
                            "msku_list": [
                                {
                                    "values": [
                                        {
                                            "unit_name": T.PANTS.GLORIA.HEIGHT_MAN.RUS_NAME,
                                            "value_min": "120",
                                            "value_max": "120",
                                        },
                                    ]
                                },
                            ],
                        }
                    }
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_one_record_table_in_offerinfo_default_disabled(self):
        """
        Проверяем, что на оффер инфо по умолчанию таблица размеров для оффера не приходит
        """
        request = 'place=offerinfo&offerid={}&rids=213&show-urls=cpa,external,phone,showPhone&regset=1'.format(
            one_record_gloria_offers[0].waremd5
        )
        response = self.report.request_json(request)
        self.assertFragmentNotIn(response, {"results": [{"sizes_table": {}}]})

    @classmethod
    def prepare_test_original_value(cls):
        def category_1_param_1(value):
            return GLParam(param_id=T.CATEGORY_1.BRAND_1.PARAM_1.ID, value=value)

        cls.index.models += [
            make_model(
                T.CATEGORY_1.BRAND_1.ID,
                T.CATEGORY_1.ID,
                cls.index.mskus,
                [
                    (
                        # glparams
                        [
                            category_1_param_1(4),
                            category_1_param_1(1),
                        ],
                        # original glparams
                        [
                            category_1_param_1(4),
                        ],
                    ),
                    (
                        # glparams
                        [
                            category_1_param_1(5),
                            category_1_param_1(7),
                        ],
                        # original glparams
                        [
                            category_1_param_1(7),
                        ],
                    ),
                ],
            ),
        ]

    def test_original_value(self):
        """
        Проверяем, что основным значением у "param_type": "size" в значении будет именно original value
        в не зависимости от его идентификатора или позиции
        в данном случае не логично выбраны original параметры, в одном случае original из одной сетки первое значение,
        во втором из другой второе значение, но это точно отражает, что primary станет именно original, а не в зависимости от порядка
        """
        request = 'place=productoffers&hyperid={}&pp=18&hid={}'.format(T.CATEGORY_1.BRAND_1.ID, T.CATEGORY_1.ID)
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "sizes_table": {
                    "header": [
                        {
                            "unit_name": T.CATEGORY_1.BRAND_1.PARAM_1.RUS_NAME,
                            "param_type": "size",
                        },
                        {
                            "unit_name": "BRAND_1",
                            "param_type": "unit",
                        },
                    ],
                    "msku_list": [
                        {
                            "values": [
                                {
                                    "unit_name": T.CATEGORY_1.BRAND_1.PARAM_1.RUS_NAME,
                                    "value_id": "4",
                                    "value_min": "M",
                                    "value_max": "M",
                                },
                                {
                                    "unit_name": "BRAND_1",
                                    "value_id": "1",
                                    "value_min": "174-186",
                                    "value_max": "174-186",
                                },
                            ]
                        },
                        {
                            "values": [
                                {
                                    "unit_name": T.CATEGORY_1.BRAND_1.PARAM_1.RUS_NAME,
                                    "value_id": "7",
                                    "value_min": "42",
                                    "value_max": "42",
                                },
                                {
                                    "unit_name": "INT",
                                    "value_id": "5",
                                    "value_min": "S",
                                    "value_max": "S",
                                },
                            ]
                        },
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_test_order_parameters(cls):
        def gloves_height(value):
            return GLParam(param_id=T.GLOVES.GLORIA.HEIGHT_GIRL.ID, value=value)

        def gloves_height_hypothesis(value):
            return GLParam(
                param_id=T.GLOVES.GLORIA.HEIGHT_GIRL.ID, string_value=value, is_filter=False, is_hypothesis=True
            )

        cls.index.models += [
            make_model(
                T.GLOVES.GLORIA.ID,
                T.GLOVES.ID,
                cls.index.mskus,
                [
                    (
                        [
                            gloves_height_hypothesis("L"),
                            gloves_height(18),
                            gloves_height(9),
                        ],
                        [],
                    ),
                    (
                        [
                            gloves_height_hypothesis("M"),
                            gloves_height(19),
                            gloves_height(8),
                        ],
                        [],
                    ),
                    (
                        [
                            gloves_height_hypothesis("S"),
                            gloves_height(15),
                            gloves_height(4),
                        ],
                        [],
                    ),
                ],
            ),
        ]

        cls.index.fashion_categories += [
            FashionCategory("PANTS", T.PANTS.ID),
        ]

    @classmethod
    def prepare_test_order_by_additional_params_with_same_values(cls):
        def hypothesis_param(value):
            return GLParam(
                param_id=T.CATEGORY_4.BRAND_1.PARAM.ID, string_value=value, is_filter=False, is_hypothesis=True
            )

        def hidden_param(value):
            return GLParam(param_id=T.CATEGORY_4.BRAND_1.PARAM.ID, value=value)

        params = [
            (
                [
                    hypothesis_param("M/HYP"),
                    hidden_param(3),
                    hidden_param(4),
                    hidden_param(2),
                    hidden_param(7),
                ],
                [],
            ),
            (
                [
                    hypothesis_param("S/HYP"),
                    hidden_param(3),
                    hidden_param(1),
                    hidden_param(2),
                    hidden_param(6),
                ],
                [],
            ),
            (
                [
                    hypothesis_param("L/HYP"),
                    hidden_param(3),
                    hidden_param(4),
                    hidden_param(8),
                ],
                [],
            ),
            (
                [
                    hypothesis_param("XL/HYP"),
                    hidden_param(4),
                    hidden_param(9),
                ],
                [],
            ),
            (
                [
                    hypothesis_param("XS/HYP"),
                    hidden_param(1),
                    hidden_param(2),
                    hidden_param(5),
                ],
                [],
            ),
        ]

        cls.index.models += [
            make_model(T.CATEGORY_4.BRAND_1.ID, T.CATEGORY_4.ID, cls.index.mskus, params),
        ]

        cls.index.fashion_categories += [
            FashionCategory("CATEGORY_4", T.CATEGORY_4.ID),
        ]

    def test_order_parameters(self):
        """
        Проверяем сортировку параметров в карте переходов при использовании гипотез, порядок значений в таблице соответствует карте переходов
        """
        request = 'place=productoffers&hyperid={}&pp=18&hid={}'.format(T.GLOVES.GLORIA.ID, T.GLOVES.ID)
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "sizes_table": {
                    "header": [
                        {
                            "unit_name": T.GLOVES.GLORIA.HEIGHT_GIRL.RUS_NAME,
                        },
                        {
                            "unit_name": "EU",
                        },
                        {
                            "unit_name": "RU",
                        },
                    ],
                    "msku_list": [
                        {
                            "values": [
                                {
                                    "unit_name": T.GLOVES.GLORIA.HEIGHT_GIRL.RUS_NAME,
                                    "value_min": "S",
                                    "value_max": "S",
                                },
                                {"unit_name": "EU", "value_min": "32", "value_max": "32"},
                                {"unit_name": "RU", "value_min": "42", "value_max": "42"},
                            ]
                        },
                        {
                            "values": [
                                {
                                    "unit_name": T.GLOVES.GLORIA.HEIGHT_GIRL.RUS_NAME,
                                    "value_min": "M",
                                    "value_max": "M",
                                },
                                {"unit_name": "EU", "value_min": "33", "value_max": "33"},
                                {"unit_name": "RU", "value_min": "43", "value_max": "43"},
                            ]
                        },
                        {
                            "values": [
                                {
                                    "unit_name": T.GLOVES.GLORIA.HEIGHT_GIRL.RUS_NAME,
                                    "value_min": "L",
                                    "value_max": "L",
                                },
                                {"unit_name": "EU", "value_min": "34", "value_max": "34"},
                                {"unit_name": "RU", "value_min": "44", "value_max": "44"},
                            ]
                        },
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_hypothesis_params_order_by_additional_position_work_properly_with_use_of_average_value(self):
        """
        Сортировка параметров в карте переходов при использовании гипотез с использованием среднего значения дает правильный порядок
        """
        xs = "XS/HYP"
        s = "S/HYP"
        m = "M/HYP"
        l = "L/HYP"
        xl = "XL/HYP"
        right_order = [xs, s, m, l, xl]

        request = 'place=productoffers&hyperid={}&pp=18&hid={}&rearr-factors=market_use_average_value_for_additional_position_in_jump_table=true'.format(
            T.CATEGORY_4.BRAND_1.ID, T.CATEGORY_4.ID
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {"sizes_table": {"msku_list": [{"values": [{"value_min": value}]} for value in right_order]}},
            preserve_order=True,
            allow_different_len=True,
        )

    def test_hypothesis_params_order_by_additional_position_work_wrong_without_use_of_average_value(self):
        """
        Сортировка параметров в карте переходов при использовании гипотез без использования среднего значения дает неправильный порядок
        """
        xs = "XS/HYP"
        s = "S/HYP"
        m = "M/HYP"
        l = "L/HYP"
        xl = "XL/HYP"
        wrong_order = [s, xs, l, m, xl]

        request = 'place=productoffers&hyperid={}&pp=18&hid={}&rearr-factors=market_use_average_value_for_additional_position_in_jump_table=false'.format(
            T.CATEGORY_4.BRAND_1.ID, T.CATEGORY_4.ID
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {"sizes_table": {"msku_list": [{"values": [{"value_min": value}]} for value in wrong_order]}},
            preserve_order=True,
            allow_different_len=True,
        )

    @classmethod
    def prepare_one_record_table_with_flag(cls):
        def category2_param1(value):
            return GLParam(param_id=T.CATEGORY_2.BRAND_1.PARAM_1.ID, value=value)

        cls.index.models += [
            make_model(
                T.CATEGORY_2.BRAND_1.ID,
                T.CATEGORY_2.ID,
                cls.index.mskus,
                [
                    (
                        # glparams
                        [
                            category2_param1(1),
                        ],
                        # original glparams
                        [
                            category2_param1(1),
                        ],
                    ),
                ],
            ),
        ]

    def test_one_record_table_with_flag(self):
        """
        Проверяем построени таблицы размеров даже когда всего 1 скю при наличии флага.
        Так же должна присутствовать карта переходов в виде фильтра с 1 значением
        """
        request = (
            'place=productoffers&hyperid={}&pp=18&hid={}&rearr-factors=market_build_jump_table_with_one_sku=1'.format(
                T.CATEGORY_2.BRAND_1.ID, T.CATEGORY_2.ID
            )
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "sizes_table": {
                    "header": [
                        {
                            "unit_name": T.CATEGORY_2.BRAND_1.PARAM_1.RUS_NAME,
                        },
                    ],
                    "msku_list": [
                        {
                            "values": [
                                {
                                    "unit_name": T.CATEGORY_2.BRAND_1.PARAM_1.RUS_NAME,
                                    "value_min": "174-186",
                                    "value_max": "174-186",
                                },
                            ]
                        },
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": str(T.CATEGORY_2.BRAND_1.PARAM_1.ID),
                        "name": T.CATEGORY_2.BRAND_1.PARAM_1.RUS_NAME,
                        "originalSubType": "size",
                        "type": "enum",
                        "values": [{"found": 1, "value": "174-186"}],
                        "valuesCount": 1,
                        "xslname": T.CATEGORY_2.BRAND_1.PARAM_1.XSL_NAME,
                    }
                ]
            },
        )

    def test_one_record_table_without_flag(self):
        """
        Проверяем что таблица не построится когда всего 1 скю и нет флага market_build_jump_table_with_one_sku.
        """
        request = 'place=productoffers&hyperid={}&pp=18&hid={}'.format(T.CATEGORY_2.BRAND_1.ID, T.CATEGORY_2.ID)
        response = self.report.request_json(request)
        self.assertFragmentNotIn(
            response,
            {
                "sizes_table": {
                    "header": [
                        {
                            "unit_name": T.CATEGORY_2.BRAND_1.PARAM_1.RUS_NAME,
                        },
                    ],
                }
            },
        )

    test_order_hypothesis_mskus = []

    @classmethod
    def prepare_test_order_hypothesis_parameter_by_another_parameter(cls):
        """
        Проверяем что параметр с одиними гипотезами, без позиций корректно отсортируется по другому, скрытому параметру
        """

        def hypothesis_param(value):
            return GLParam(
                param_id=T.CATEGORY_3.BRAND_1.PARAM_1.ID, string_value=value, is_filter=False, is_hypothesis=True
            )

        def hidden_param(value):
            return GLParam(param_id=T.CATEGORY_3.BRAND_1.PARAM_2.ID, value=value)

        params = [
            (
                [
                    hypothesis_param("L/HYP"),
                    hidden_param(3),
                    hidden_param(6),
                    hidden_param(11),
                ],
                [
                    hidden_param(3),
                ],
            ),
            (
                [
                    hypothesis_param("S/HYP"),
                    hidden_param(1),
                    hidden_param(4),
                    hidden_param(7),
                ],
                [
                    hidden_param(1),
                ],
            ),
            (
                [
                    hypothesis_param("M/HYP"),
                    hidden_param(2),
                    hidden_param(5),
                    hidden_param(9),
                ],
                [
                    hidden_param(2),
                ],
            ),
        ]

        cls.index.models += [
            make_model(T.CATEGORY_3.BRAND_1.ID, T.CATEGORY_3.ID, cls.index.mskus, params),
        ]

        T.test_order_hypothesis_mskus = [item.sku for item in cls.index.mskus[-len(params) :]]

        cls.index.fashion_categories += [
            FashionCategory("CATEGORY_3", T.CATEGORY_3.ID),
        ]

    def test_enabled_order_hypothesis_parameter_by_another_parameter(self):
        """
        Проверяем сортировку параметров в карте переходов при использовании гипотез, порядок значений в таблице соответствует карте переходов
        реар market_sort_jump_table_size_hypothesis включен доп сортировка должна работать
        верный порядок гипотез S M L
        """
        request = 'place=productoffers&hyperid={}&pp=18&hid={}&rearr-factors=market_unit_info_in_size_table=1;market_sort_jump_table_size_hypothesis=1'.format(
            T.CATEGORY_3.BRAND_1.ID, T.CATEGORY_3.ID
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "sizes_table": {
                    "header": [
                        {
                            "unit_name": T.CATEGORY_3.BRAND_1.PARAM_1.RUS_NAME,
                        },
                        {
                            "unit_name": "INT",
                        },
                        {
                            "unit_name": "RU",
                        },
                    ],
                    "msku_list": [
                        {
                            "values": [
                                {
                                    "unit_name": T.CATEGORY_3.BRAND_1.PARAM_1.RUS_NAME,
                                    "value_min": "S/HYP",
                                    "value_max": "S/HYP",
                                },
                                {"unit_name": "INT", "value_min": "S", "value_max": "S"},
                                {"unit_name": "RU", "value_min": "42", "value_max": "42"},
                            ]
                        },
                        {
                            "values": [
                                {
                                    "unit_name": T.CATEGORY_3.BRAND_1.PARAM_1.RUS_NAME,
                                    "value_min": "M/HYP",
                                    "value_max": "M/HYP",
                                },
                                {"unit_name": "INT", "value_min": "M", "value_max": "M"},
                                {"unit_name": "RU", "value_min": "44", "value_max": "44"},
                            ]
                        },
                        {
                            "values": [
                                {
                                    "unit_name": T.CATEGORY_3.BRAND_1.PARAM_1.RUS_NAME,
                                    "value_min": "L/HYP",
                                    "value_max": "L/HYP",
                                },
                                {"unit_name": "INT", "value_min": "L", "value_max": "L"},
                                {"unit_name": "RU", "value_min": "46", "value_max": "46"},
                            ]
                        },
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_filtering_hidden_params_helps_avoid_redundant_fuzzy_transitions(self):
        """
        Проверяем что использование фильтрации скрытых параметров после сортировки параметров в карте переходов
        позволяет избавиться от появления лишних fuzzy-переходов
        """
        msku_count = len(T.test_order_hypothesis_mskus)
        for checked_msku_id in range(msku_count):
            request = 'place=productoffers&hyperid={}&pp=18&sku-jmp-table=1&hid={}&market-sku={}&rearr-factors=market_unit_info_in_size_table=1;market_sort_jump_table_size_hypothesis=1'.format(
                T.CATEGORY_3.BRAND_1.ID, T.CATEGORY_3.ID, T.test_order_hypothesis_mskus[checked_msku_id]
            )
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {
                    "filters": [
                        {
                            "id": "300001",
                            "originalSubType": "size",
                            "values": [
                                {
                                    "fuzzy": Absent(),
                                    "marketSku": T.test_order_hypothesis_mskus[i],
                                    "checked": True if i == checked_msku_id else Absent(),
                                }
                                for i in range(msku_count)
                            ],
                        },
                    ]
                },
            )

    def test_disabled_order_hypothesis_parameter_by_another_parameter(self):
        """
        Проверяем сортировку параметров в карте переходов при использовании гипотез, порядок значений в таблице соответствует карте переходов
        реар market_sort_jump_table_size_hypothesis выключен доп сортировка не работает
        верный порядок гипотез без сортировки L M S
        """
        request = 'place=productoffers&hyperid={}&pp=18&hid={}&rearr-factors=market_unit_info_in_size_table=1;market_sort_jump_table_size_hypothesis=0'.format(
            T.CATEGORY_3.BRAND_1.ID, T.CATEGORY_3.ID
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "sizes_table": {
                    "header": [
                        {
                            "unit_name": T.CATEGORY_3.BRAND_1.PARAM_1.RUS_NAME,
                        },
                        {
                            "unit_name": "INT",
                        },
                        {
                            "unit_name": "RU",
                        },
                    ],
                    "msku_list": [
                        {
                            "values": [
                                {
                                    "unit_name": T.CATEGORY_3.BRAND_1.PARAM_1.RUS_NAME,
                                    "value_min": "L/HYP",
                                    "value_max": "L/HYP",
                                },
                                {"unit_name": "INT", "value_min": "L", "value_max": "L"},
                                {"unit_name": "RU", "value_min": "46", "value_max": "46"},
                            ]
                        },
                        {
                            "values": [
                                {
                                    "unit_name": T.CATEGORY_3.BRAND_1.PARAM_1.RUS_NAME,
                                    "value_min": "M/HYP",
                                    "value_max": "M/HYP",
                                },
                                {"unit_name": "INT", "value_min": "M", "value_max": "M"},
                                {"unit_name": "RU", "value_min": "44", "value_max": "44"},
                            ]
                        },
                        {
                            "values": [
                                {
                                    "unit_name": T.CATEGORY_3.BRAND_1.PARAM_1.RUS_NAME,
                                    "value_min": "S/HYP",
                                    "value_max": "S/HYP",
                                },
                                {"unit_name": "INT", "value_min": "S", "value_max": "S"},
                                {"unit_name": "RU", "value_min": "42", "value_max": "42"},
                            ]
                        },
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_size_jump_table_on_prime_disabled(self):
        """
        Проверяем, что по умолчанию карты переходов для моделей с размерами НЕТ
        """
        response = self.report.request_json('place=prime&pp=18&hid={}&use-default-offers=1'.format(T.SKIRTS.ID))
        self.assertFragmentNotIn(
            response,
            {"jumpTable": []},
        )

    def test_size_jump_table_on_prime_enabled(self):
        """
        Проверяем НАЛИЧИЕ карты переходов для моделей с размерами если есть rearr market_enable_size_jump_table_on_prime
        """
        response = self.report.request_json(
            'place=prime&pp=18&hid={}&use-default-offers=1&rearr-factors=market_enable_size_jump_table_on_prime=1'.format(
                T.SKIRTS.ID
            )
        )
        self.assertFragmentIn(
            response,
            {
                "jumpTable": [
                    {
                        "id": "1",
                        "name": T.SKIRTS.ALBARI.HEIGHT_GIRL.RUS_NAME,
                        "subType": "",
                        "originalSubType": "size",
                        "values": [{"value": "124-136"}, {"value": "136-142"}],
                    },
                ]
            },
        )


if __name__ == '__main__':
    main()
