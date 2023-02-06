#!/usr/bin/env python
# -*- coding: utf-8 -*-
import runner  # noqa

from core.matcher import Absent
from core.types import Const, GLParam, GLType, MarketSku, Model, ModelDescriptionTemplates
from core.testcase import TestCase, main

MODEL_PRIMARY_PARAMS = [
    GLParam(param_id=201, value=11, is_filter=True),
    GLParam(param_id=203, value=1, is_filter=True),
    GLParam(param_id=205, value=15, is_filter=True),
]

MODEL_ADDITIONAL_PARAMS = [
    GLParam(param_id=202, value=21, is_filter=False),
    GLParam(param_id=204, value=0, is_filter=False),
    GLParam(param_id=206, value=16, is_filter=False),
    GLParam(param_id=207, string_value='17', is_filter=False),
]

MSKU_PARAMS = [
    GLParam(param_id=211, value=31, is_filter=True),
    GLParam(param_id=213, value=1, is_filter=True),
    GLParam(param_id=215, value=18, is_filter=True),
]

VIDEO_PARAMS = [
    GLParam(param_id=Const.MODELS_VIDEO_PARAM_ID, string_value='video_1'),
    GLParam(param_id=Const.MODELS_VIDEO_PARAM_ID, string_value='video_2'),
]

HYPOTHESIS_PARAMS = [
    GLParam(param_id=211, string_value='HYP-211', is_filter=False, is_hypothesis=True),
    GLParam(param_id=213, string_value='HYP-213', is_filter=False, is_hypothesis=True),
    GLParam(param_id=215, string_value='HYP-215', is_filter=False, is_hypothesis=True),
    GLParam(param_id=201, string_value='HYP-201', is_filter=False, is_hypothesis=True),
    GLParam(param_id=202, string_value='HYP-202', is_filter=False, is_hypothesis=True),
    GLParam(param_id=204, string_value='HYP-204', is_filter=False, is_hypothesis=True),
    GLParam(param_id=206, string_value='HYP-206', is_filter=False, is_hypothesis=True),
    GLParam(param_id=207, string_value='HYP-207', is_filter=False, is_hypothesis=True),
]


def sample_friendly_param(name, value):
    if isinstance(value, list):
        value = ", ".join(value)
    return "{name} {value}".format(name=name, value=value)


def get_sample_friendly_primary_model(enable_hypothesis=False):
    SAMPLE_FRIENDLY_PRIMARY_MODEL = [
        "ModelPrimaryBool есть",
        "ModelPrimaryNumeric 15",
    ]
    if not enable_hypothesis:
        # "ModelPrimaryEnum VALUE-11"
        SAMPLE_FRIENDLY_PRIMARY_MODEL.insert(0, sample_friendly_param("ModelPrimaryEnum", "VALUE-11"))
    else:
        SAMPLE_FRIENDLY_PRIMARY_MODEL.insert(0, sample_friendly_param("ModelPrimaryEnum", ["VALUE-11", "HYP-201"]))
    return SAMPLE_FRIENDLY_PRIMARY_MODEL


SAMPLE_FRIENDLY_ADDITIONAL_MODEL = [
    "ModelAdditionalEnum VALUE-21",
    "ModelAdditionalBool нет",
    "ModelAdditionalNumeric 16",
    "ModelAdditionalString 17",
]


def get_sample_friendly_primary_msku(enable_hypothesis=False):
    SAMPLE_FRIENDLY_MSKU = [
        "MskuPrimaryBool есть",
        "MskuPrimaryNumeric 18",
    ]
    if not enable_hypothesis:
        # "ModelPrimaryEnum VALUE-11"
        SAMPLE_FRIENDLY_MSKU.insert(0, sample_friendly_param("MskuPrimaryEnum", "VALUE-31"))
    else:
        SAMPLE_FRIENDLY_MSKU.insert(0, sample_friendly_param("MskuPrimaryEnum", ["VALUE-31", "HYP-211"]))
    return SAMPLE_FRIENDLY_MSKU


SAMPLE_FRIENDLY_EMPTY_MSKU = [
    "MskuPrimaryEnum --",
    "MskuPrimaryBool --",
    "MskuPrimaryNumeric --",
]

SAMPLE_FRIENDLY_HYPOTHESIS_MODEL = [
    "ModelAdditionalEnum HYP-202",
    "ModelAdditionalBool HYP-204",
    "ModelAdditionalNumeric HYP-206",
    "ModelAdditionalString HYP-207",
]


def sample_full_spec_param(name, id, value):
    if isinstance(value, list):
        value = ", ".join(value)
    return {"name": name, "usedParams": [{"id": id}], "value": value}


def get_sample_full_primary_model(enable_hypothesis=False):
    SAMPLE_FULL_PRIMARY_MODEL = [
        sample_full_spec_param("ModelPrimaryBool", 203, "есть"),
        sample_full_spec_param("ModelPrimaryNumeric", 205, "15"),
    ]
    if not enable_hypothesis:
        # "ModelPrimaryEnum VALUE-11"
        SAMPLE_FULL_PRIMARY_MODEL.insert(
            0,
            sample_full_spec_param("ModelPrimaryEnum", 201, "VALUE-11"),
        )
    else:
        SAMPLE_FULL_PRIMARY_MODEL.insert(0, sample_full_spec_param("ModelPrimaryEnum", 201, ["VALUE-11", "HYP-201"]))
    return SAMPLE_FULL_PRIMARY_MODEL


SAMPLE_FULL_ADDITIONAL_MODEL = [
    sample_full_spec_param("ModelAdditionalEnum", 202, "VALUE-21"),
    sample_full_spec_param("ModelAdditionalBool", 204, "нет"),
    sample_full_spec_param("ModelAdditionalNumeric", 206, "16"),
    sample_full_spec_param("ModelAdditionalString", 207, "17"),
]

SAMPLE_FULL_MSKU = [
    sample_full_spec_param("MskuPrimaryEnum", 211, "VALUE-31"),
    sample_full_spec_param("MskuPrimaryBool", 213, "есть"),
    sample_full_spec_param("MskuPrimaryNumeric", 215, "18"),
]


def get_sample_full_msku(enable_hypothesis=False):
    SAMPLE_FULL_MSKU = [
        sample_full_spec_param("MskuPrimaryBool", 213, "есть"),
        sample_full_spec_param("MskuPrimaryNumeric", 215, "18"),
    ]

    if not enable_hypothesis:
        # "ModelPrimaryEnum VALUE-11"
        SAMPLE_FULL_MSKU.insert(0, sample_full_spec_param("MskuPrimaryEnum", 211, "VALUE-31"))
    else:
        SAMPLE_FULL_MSKU.insert(0, sample_full_spec_param("MskuPrimaryEnum", 211, ["VALUE-31", "HYP-211"]))
    return SAMPLE_FULL_MSKU


SAMPLE_FULL_EMPTY_MSKU = [
    sample_full_spec_param("MskuPrimaryEnum", 211, "--"),
    sample_full_spec_param("MskuPrimaryBool", 213, "--"),
    sample_full_spec_param("MskuPrimaryNumeric", 215, "--"),
]

SAMPLE_FULL_HYPOTHESIS_MSKU = [
    sample_full_spec_param("ModelAdditionalEnum", 202, "HYP-202"),
    sample_full_spec_param("ModelAdditionalBool", 204, "HYP-204"),
    sample_full_spec_param("ModelAdditionalNumeric", 206, "HYP-206"),
    sample_full_spec_param("ModelAdditionalString", 207, "HYP-207"),
]


def sample_full_param(name, param_id, value):
    return {"name": name, "value": value, "usedParams": [{"id": param_id}]}


SAMPLE_FULL_MODEL = [
    sample_full_param("ModelPrimaryEnum", 201, 11),
    sample_full_param("ModelAdditionalEnum", 202, 21),
    sample_full_param("ModelPrimaryBool", 203, "есть"),
    sample_full_param("ModelAdditionalBool", 204, "нет"),
    sample_full_param("ModelPrimaryNumeric", 205, 15),
    sample_full_param("ModelAdditionalNumeric", 206, 16),
    sample_full_param("ModelAdditionalString", 207, 17),
]

BASE_REQUEST = "place=sku_offers&market-sku={msku}&show-models-specs={specs}"
SHOW_MODELS_REQUEST = "&show-models=1"


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.gltypes += [
            GLType(
                param_id=201,
                xslname="ModelPrimaryEnum",
                hid=1,
                cluster_filter=False,
                gltype=GLType.ENUM,
                values=[11, 12],
            ),
            GLType(
                param_id=202,
                xslname="ModelAdditionalEnum",
                hid=1,
                cluster_filter=False,
                gltype=GLType.ENUM,
                values=[21, 22],
            ),
            GLType(param_id=203, xslname="ModelPrimaryBool", hid=1, cluster_filter=False, gltype=GLType.BOOL),
            GLType(param_id=204, xslname="ModelAdditionalBool", hid=1, cluster_filter=False, gltype=GLType.BOOL),
            GLType(param_id=205, xslname="ModelPrimaryNumeric", hid=1, cluster_filter=False, gltype=GLType.NUMERIC),
            GLType(param_id=206, xslname="ModelAdditionalNumeric", hid=1, cluster_filter=False, gltype=GLType.NUMERIC),
            GLType(param_id=207, xslname="ModelAdditionalString", hid=1, cluster_filter=False, gltype=GLType.STRING),
            GLType(
                param_id=211, xslname="MskuPrimaryEnum", hid=1, cluster_filter=True, gltype=GLType.ENUM, values=[31, 32]
            ),
            GLType(param_id=213, xslname="MskuPrimaryBool", hid=1, cluster_filter=True, gltype=GLType.BOOL),
            GLType(param_id=215, xslname="MskuPrimaryNumeric", hid=1, cluster_filter=True, gltype=GLType.NUMERIC),
            GLType(param_id=Const.MODELS_VIDEO_PARAM_ID, hid=1, xslname="models_video", gltype=GLType.STRING),
        ]

        cls.index.model_description_templates += [
            ModelDescriptionTemplates(
                hid=1,
                micromodel="{ModelPrimaryEnum}, {ModelAdditionalEnum}, {ModelPrimaryBool}, {ModelAdditionalBool}, {ModelPrimaryNumeric}, {ModelAdditionalNumeric}, {ModelAdditionalString}, {MskuPrimaryEnum}, {MskuPrimaryBool}, {MskuPrimaryNumeric}",  # noqa
                friendlymodel=[
                    "ModelPrimaryEnum {ModelPrimaryEnum}",
                    "ModelAdditionalEnum {ModelAdditionalEnum}",
                    "ModelPrimaryBool {ModelPrimaryBool}",
                    "ModelAdditionalBool {ModelAdditionalBool}",
                    "ModelPrimaryNumeric {ModelPrimaryNumeric}",
                    "ModelAdditionalNumeric {ModelAdditionalNumeric}",
                    "ModelAdditionalString {ModelAdditionalString}",
                    "MskuPrimaryEnum {MskuPrimaryEnum}",
                    "MskuPrimaryBool {MskuPrimaryBool}",
                    "MskuPrimaryNumeric {MskuPrimaryNumeric}",
                ],
                model=[
                    (
                        "Основное",
                        {
                            "ModelPrimaryEnum": "{ModelPrimaryEnum}",
                            "ModelAdditionalEnum": "{ModelAdditionalEnum}",
                            "ModelPrimaryBool": "{ModelPrimaryBool}",
                            "ModelAdditionalBool": "{ModelAdditionalBool}",
                            "ModelPrimaryNumeric": "{ModelPrimaryNumeric}",
                            "ModelAdditionalNumeric": "{ModelAdditionalNumeric}",
                            "ModelAdditionalString": "{ModelAdditionalString}",
                            "MskuPrimaryEnum": "{MskuPrimaryEnum}",
                            "MskuPrimaryBool": "{MskuPrimaryBool}",
                            "MskuPrimaryNumeric": "{MskuPrimaryNumeric}",
                        },
                    )
                ],
            )
        ]

        cls.index.models += [
            Model(hid=1, hyperid=1, glparams=MODEL_PRIMARY_PARAMS + MODEL_ADDITIONAL_PARAMS),
        ]
        cls.index.mskus += [
            MarketSku(
                hyperid=1, sku=1, glparams=MODEL_PRIMARY_PARAMS + MSKU_PARAMS + VIDEO_PARAMS + MODEL_ADDITIONAL_PARAMS
            ),
            MarketSku(
                hyperid=1,
                sku=2,
                glparams=MODEL_PRIMARY_PARAMS + MODEL_ADDITIONAL_PARAMS,
                video=['video_2_1', 'video_2_2', 'video_2_1'],
            ),
            MarketSku(hyperid=1, sku=3, glparams=MODEL_PRIMARY_PARAMS + MSKU_PARAMS + HYPOTHESIS_PARAMS),
        ]

    def __specs_sample(self, friendly_specs=None, full_specs=None):
        if friendly_specs is None and full_specs is None:
            return Absent()
        result = {}
        result["friendly"] = friendly_specs if friendly_specs else Absent()
        result["full"] = [{"groupSpecs": full_specs}] if full_specs else Absent()
        return result

    def __product_sample(self, extra_req='', friendly_specs=None, full_specs=None):
        if extra_req == '':
            return {"id": 1, "specs": self.__specs_sample(friendly_specs, full_specs)}
        else:
            return {
                "id": 1,
                "specs": {
                    "friendly": Absent(),
                    "full": Absent()
                    # "internal" always renders
                },
            }

    def __check_specs(self, request, msku_specs, product, product_id=1):
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "sku",
                        "id": str(product_id),
                        "specs": msku_specs,
                        "product": product,
                    }
                ]
            },
            allow_different_len=False,
        )

    def test_video(self):
        # у скю 1 видео в старом формате
        result = self.report.request_json(BASE_REQUEST.format(msku=1, specs='msku-friendly'))
        self.assertFragmentIn(
            result, {'entity': 'sku', 'id': '1', 'video': ['video_1', 'video_2']}, allow_different_len=False
        )

        # у скю 2 видео в новом формате
        result = self.report.request_json(BASE_REQUEST.format(msku=2, specs='msku-friendly'))
        self.assertFragmentIn(
            result,
            {'entity': 'sku', 'id': '2', 'video': ['video_2_1', 'video_2_2']},  # заодно проверим сокрытие дубликатов
            allow_different_len=False,
        )

        # Под флагом disable_video_urls видео должны отсутствовать
        for sku_id in [1, 2]:
            response = self.report.request_json(
                BASE_REQUEST.format(msku=sku_id, specs='msku-friendly') + '&rearr-factors=disable_video_urls=1'
            )
            self.assertFragmentIn(
                response,
                {
                    "id": str(sku_id),
                    "video": Absent(),
                },
                preserve_order=True,
                allow_different_len=False,
            )

    def test_friendly_descriptions(self):
        """
        Friendly описания рендерятся в виде массива строк по шаблону friendlymodel
        Мы должны явно указать параметр &show-models-specs=friendly-msku, чтобы отрендерить их
        У МСКУ заданы основные параметры, но дополнительные параметры модели были добавлены к характеирстикам МСКУ
        """
        # Наличие или отсутствие модели в выдаче не влияет на характеристики МСКУ
        for extra_req in ['', SHOW_MODELS_REQUEST]:
            self.__check_specs(
                request=BASE_REQUEST.format(msku=1, specs='msku-friendly') + extra_req,
                msku_specs=self.__specs_sample(
                    friendly_specs=get_sample_friendly_primary_model()
                    + SAMPLE_FRIENDLY_ADDITIONAL_MODEL
                    + get_sample_friendly_primary_msku()
                ),
                product=self.__product_sample(extra_req=extra_req),
            )

        # Характеристики модели не влияют на характеристики МСКУ
        self.__check_specs(
            request=BASE_REQUEST.format(msku=1, specs='msku-friendly,friendly') + SHOW_MODELS_REQUEST,
            msku_specs=self.__specs_sample(
                friendly_specs=get_sample_friendly_primary_model()
                + SAMPLE_FRIENDLY_ADDITIONAL_MODEL
                + get_sample_friendly_primary_msku()
            ),
            product=self.__product_sample(
                friendly_specs=get_sample_friendly_primary_model()
                + SAMPLE_FRIENDLY_ADDITIONAL_MODEL
                + SAMPLE_FRIENDLY_EMPTY_MSKU
            ),
        )

    def test_full_descriptions(self):
        """
        Full описания рендерятся в виде объекта full по шаблону
        https://github.yandex-team.ru/market/microformats/blob/feature/guru-card-in-report/product/product.sample.js#L118
        Мы должны явно указать параметр &show-models-specs=full-msku, чтобы отрендерить их
        У МСКУ заданы основные параметры, но дополнительные параметры модели были добавлены к характеирстикам МСКУ
        """
        # Наличие или отсутствие модели в выдаче не влияет на характеристики МСКУ
        for extra_req in ['', SHOW_MODELS_REQUEST]:
            self.__check_specs(
                request=BASE_REQUEST.format(msku=1, specs='msku-full') + extra_req,
                msku_specs=self.__specs_sample(
                    full_specs=get_sample_full_primary_model() + SAMPLE_FULL_ADDITIONAL_MODEL + get_sample_full_msku()
                ),
                product=self.__product_sample(extra_req=extra_req),
            )

        # Характеристики модели не влияют на характеристики МСКУ
        self.__check_specs(
            request=BASE_REQUEST.format(msku=1, specs='msku-full,full') + SHOW_MODELS_REQUEST,
            msku_specs=self.__specs_sample(
                full_specs=get_sample_full_primary_model() + SAMPLE_FULL_ADDITIONAL_MODEL + get_sample_full_msku()
            ),
            product=self.__product_sample(full_specs=get_sample_full_primary_model() + SAMPLE_FULL_ADDITIONAL_MODEL),
        )

    def test_mixed_descriptions(self):
        """
        Отображаем все описания для МСКУ и только полное для модели
        """
        self.__check_specs(
            request=BASE_REQUEST.format(msku=1, specs='msku-full,msku-friendly,full') + SHOW_MODELS_REQUEST,
            msku_specs=self.__specs_sample(
                friendly_specs=get_sample_friendly_primary_model()
                + SAMPLE_FRIENDLY_ADDITIONAL_MODEL
                + get_sample_friendly_primary_msku(),
                full_specs=get_sample_full_primary_model() + SAMPLE_FULL_ADDITIONAL_MODEL + get_sample_full_msku(),
            ),
            product=self.__product_sample(full_specs=get_sample_full_primary_model() + SAMPLE_FULL_ADDITIONAL_MODEL),
        )

    def test_full_descriptions_with_hypothesis(self):
        """
        Full описания рендерятся в виде объекта full по шаблону
        Проверяем, что
        - гипотезы показываются
        - гипотезы выводятся только для Additional параметров
        """
        # Наличие или отсутствие модели в выдаче не влияет на характеристики МСКУ
        for extra_req in ['', SHOW_MODELS_REQUEST]:
            self.__check_specs(
                request=BASE_REQUEST.format(msku=3, specs='msku-full') + extra_req,
                msku_specs=self.__specs_sample(
                    full_specs=get_sample_full_primary_model(enable_hypothesis=True)
                    + SAMPLE_FULL_HYPOTHESIS_MSKU
                    + get_sample_full_msku(enable_hypothesis=True)
                ),
                product=self.__product_sample(extra_req=extra_req),
                product_id=3,
            )

        # Характеристики модели не влияют на характеристики МСКУ
        self.__check_specs(
            request=BASE_REQUEST.format(msku=3, specs='msku-full,full') + SHOW_MODELS_REQUEST,
            msku_specs=self.__specs_sample(
                full_specs=get_sample_full_primary_model(enable_hypothesis=True)
                + SAMPLE_FULL_HYPOTHESIS_MSKU
                + get_sample_full_msku(enable_hypothesis=True)
            ),
            product=self.__product_sample(
                full_specs=get_sample_full_primary_model(enable_hypothesis=False) + SAMPLE_FULL_ADDITIONAL_MODEL
            ),
            product_id=3,
        )

    def test_friendly_descriptions_with_hypothesis(self):
        """
        Friendly описания рендерятся в виде массива строк по шаблону friendlymodel
        Проверяем, что
        - гипотезы показываются
        - гипотезы выводятся только для Additional параметров
        """
        # Наличие или отсутствие модели в выдаче не влияет на характеристики МСКУ
        for extra_req in ['', SHOW_MODELS_REQUEST]:
            self.__check_specs(
                request=BASE_REQUEST.format(msku=3, specs='msku-friendly') + extra_req,
                msku_specs=self.__specs_sample(
                    friendly_specs=get_sample_friendly_primary_model(enable_hypothesis=True)
                    + SAMPLE_FRIENDLY_HYPOTHESIS_MODEL
                    + get_sample_friendly_primary_msku(enable_hypothesis=True)
                ),
                product=self.__product_sample(extra_req=extra_req),
                product_id=3,
            )

        # Характеристики модели не влияют на характеристики МСКУ
        self.__check_specs(
            request=BASE_REQUEST.format(msku=3, specs='msku-friendly,friendly') + SHOW_MODELS_REQUEST,
            msku_specs=self.__specs_sample(
                friendly_specs=get_sample_friendly_primary_model(enable_hypothesis=True)
                + SAMPLE_FRIENDLY_HYPOTHESIS_MODEL
                + get_sample_friendly_primary_msku(enable_hypothesis=True)
            ),
            product=self.__product_sample(
                friendly_specs=get_sample_friendly_primary_model(enable_hypothesis=False)
                + SAMPLE_FRIENDLY_ADDITIONAL_MODEL
                + SAMPLE_FRIENDLY_EMPTY_MSKU
            ),
            product_id=3,
        )


if __name__ == '__main__':
    main()
