# -*- coding: utf-8 -*-
import pytest
import six

from hamcrest import (
    assert_that,
    contains_inanyorder,
    equal_to,
)
from market.proto.indexer.contex_pb2 import (
    EXPERIMENTAL_BASE_SKU,
    EXPERIMENTAL_SKU,
    TContexRelations,
)
from market.proto.content.mbo import MboParameters_pb2
from market.proto.content.mbo.ExportReportModel_pb2 import (
    EXPERIMENTAL_BASE_MODEL,
    EXPERIMENTAL_MODEL,
    ParameterValue,
    ParameterValueHypothesis,
    Relation,
    SKU_PARENT_MODEL,
)
from market.proto.msku.jump_table_filters_pb2 import (
    JumpTableFilter,
    Picker,
)
from msku_uploader.yatf.test_env import MskuUploaderTestEnv
from msku_uploader.yatf.resources.contex_relations_pb import ContexRelationsPbsn
from msku_uploader.yatf.resources.mbo_pb import (
    MboCategoryPb,
    MboModelPb,
    MboSkuPb,
)
from msku_uploader.yatf.utils import (
    make_category_option_info_pb,
    make_mbo_picker_image_pb,
    make_model_protobuf,
    make_sku_protobuf,
)

CATEG_ID = 989040
MODEL_ID = 1713074440
EXPERIMENTAL_MODEL_ID = 1713074441
BASE_SKU = 100
SINGLE_COLOR_GLOB_SKU = 101
MULTI_COLOR_GLOB_SKU = 102
SIZES_MODEL_ID = 1713075001
SIZES_SKU = 100000000101
ORIGINAL_SKU_ID = 100000000002
EXPERIMENTAL_SKU_ID = 100000000003
# Обычный параметр
PARAM_BASE_ID = 1
PARAM_BASE_NAME = 'default_param'
# Глобальный цвет. Будет показан на снипете
PARAM_COLOR_GLOB_ID = 2
PARAM_COLOR_GLOB_NAME = 'color_glob'
# Нумерик enum параметр, будет скастован в строку
PARAM_NUMERIC_ENUM_ID = 3
PARAM_NUMERIC_ENUM_NAME = 'volume'
# Нумерик параметр, будет скастован в строку
PARAM_NUMERIC_ID = 4
PARAM_NUMERIC_NAME = 'speed'
# Булевый параметр
PARAM_BOOL_ID = 5
PARAM_BOOL_NAME = 'has_wifi'
# Параметры - гипотезы
HYP_PARAM_1_ID = 6
HYP_PARAM_1_XSL_NAME = 'hypo param 1'
HYP_PARAM_1_NAME = 'hypo param value 1'
HYP_PARAM_2_ID = 7
HYP_PARAM_2_XSL_NAME = 'hypo param 2'
HYP_PARAM_2_NAME = 'hypo param value 2'
# Параметры без описания
PARAM_NO_DESCR = 8
PARAM_NO_DESCR_NAME = 'undescribed'
HYP_PARAM_NO_DESCR = 9
HYP_PARAM_NO_DESCR_XSL_NAME = 'undescribed'
HYP_PARAM_NO_DESCR_NAME = 'undescribed value'
# Параметр для эксперимента
EXPERIMENTAL_PARAM_NUMERIC_ID = 10
EXPERIMENTAL_PARAM_NUMERIC_NAME = 'power'
# Параметр размеров
PARAM_SIZE_VALUES = 100
PARAM_SIZE_VALUES_NAME = "sizes_param"
# Параметр сеток размеров
PARAM_SIZE_UNITS = 101
PARAM_SIZE_UNITS_NAME = "sizes_units"
# Параметр производителей
PARAM_VENDORS = 102
PARAM_VENDORS_NAME = "vendor"
# Область показа значения
DONT_SHOW = 0
SHOW_ON_JUMP_TABLE = 2
SHOW_ON_JUMP_AND_SIZE_TABLE = 6

DEFAULT_MSKU_EXP = 0


@pytest.fixture(scope='module')
def expected_param_values():
    return {
        PARAM_BASE_ID: {
            'extractor': lambda x: x.value,
            'expected_values': {},  # no results, because don't show
            'expected_show_on': DONT_SHOW,  # MODEL_LEVEL
        },
        PARAM_COLOR_GLOB_ID: {
            'extractor': lambda x: x.value,
            'expected_values': {
                SINGLE_COLOR_GLOB_SKU: {
                    DEFAULT_MSKU_EXP: [21],
                },
                MULTI_COLOR_GLOB_SKU: {
                    DEFAULT_MSKU_EXP: [21, 22],
                },
            },
            'expected_is_original': {
                SINGLE_COLOR_GLOB_SKU: {
                    DEFAULT_MSKU_EXP: [True],
                },
                MULTI_COLOR_GLOB_SKU: {
                    DEFAULT_MSKU_EXP: [True, False],
                },
            },
            # MODEL_LEVEL
            'expected_show_on': SHOW_ON_JUMP_TABLE,
            'second_extractor': lambda x: x.option_id,
            'second_expected_values': {
                SINGLE_COLOR_GLOB_SKU: {
                    DEFAULT_MSKU_EXP: [21],
                },
                MULTI_COLOR_GLOB_SKU: {
                    DEFAULT_MSKU_EXP: [21, 22],
                },
            },
            'expected_image_pickers': {
                SINGLE_COLOR_GLOB_SKU: {
                    DEFAULT_MSKU_EXP: [
                        Picker(
                            option_id=21,
                            image=make_mbo_picker_image_pb(
                                url="some_url1.ru",
                                namespace="mpic",
                                group_id="497",
                                image_name="QWERTY",
                                name="QWERTY_FAKE",
                            ),
                        ),
                    ],
                },
                MULTI_COLOR_GLOB_SKU: {
                    DEFAULT_MSKU_EXP: [
                        Picker(
                            option_id=21,
                            image=make_mbo_picker_image_pb(
                                url="some_url1.ru",
                                namespace="mpic",
                                group_id="497",
                                image_name="QWERTY",
                                name="QWERTY_FAKE",
                            ),
                        ),
                        Picker(
                            option_id=22,
                            image=make_mbo_picker_image_pb(
                                url="some_url2.ru",
                                namespace="mpic",
                                group_id="498",
                                image_name="WERTYU",
                                name="WERTYU_FAKE",
                            ),
                        ),
                    ],
                },
            },
        },
        PARAM_NUMERIC_ENUM_ID: {
            'extractor': lambda x: x.numeric_value,
            'expected_values': {
                BASE_SKU: {
                    DEFAULT_MSKU_EXP: [0.33],
                },
                SINGLE_COLOR_GLOB_SKU: {
                    DEFAULT_MSKU_EXP: [3.22, 0.33],
                },
                MULTI_COLOR_GLOB_SKU: {
                    DEFAULT_MSKU_EXP: [3.22],
                },
            },
            'expected_is_original': {
                BASE_SKU: {
                    DEFAULT_MSKU_EXP: [True],
                },
                SINGLE_COLOR_GLOB_SKU: {
                    DEFAULT_MSKU_EXP: [True, True],
                },
                MULTI_COLOR_GLOB_SKU: {
                    DEFAULT_MSKU_EXP: [True],
                },
            },
            # model_filter_index is negative
            'expected_show_on': SHOW_ON_JUMP_TABLE,
        },
        PARAM_NUMERIC_ID: {
            'extractor': lambda x: x.numeric_value,
            'expected_values': {
                BASE_SKU: {
                    DEFAULT_MSKU_EXP: [123.45],
                },
                SINGLE_COLOR_GLOB_SKU: {
                    DEFAULT_MSKU_EXP: [322.1],
                },
                MULTI_COLOR_GLOB_SKU: {
                    DEFAULT_MSKU_EXP: [54.321],
                },
            },
            'expected_is_original': {
                BASE_SKU: {
                    DEFAULT_MSKU_EXP: [False],
                },
                SINGLE_COLOR_GLOB_SKU: {
                    DEFAULT_MSKU_EXP: [True],
                },
                MULTI_COLOR_GLOB_SKU: {
                    DEFAULT_MSKU_EXP: [True],
                },
            },
            'expected_show_on': SHOW_ON_JUMP_TABLE,
        },
        PARAM_BOOL_ID: {
            'extractor': lambda x: x.value,
            'expected_values': {
                BASE_SKU: {
                    DEFAULT_MSKU_EXP: [1],
                },
                SINGLE_COLOR_GLOB_SKU: {
                    DEFAULT_MSKU_EXP: [0],
                },
            },
            'expected_is_original': {
                BASE_SKU: {
                    DEFAULT_MSKU_EXP: [True],
                },
                SINGLE_COLOR_GLOB_SKU: {
                    DEFAULT_MSKU_EXP: [True],
                },
            },
            'expected_show_on': SHOW_ON_JUMP_TABLE,
            'second_extractor': lambda x: x.bool_value,
            'second_expected_values': {
                BASE_SKU: {
                    DEFAULT_MSKU_EXP: [True],
                },
                SINGLE_COLOR_GLOB_SKU: {
                    DEFAULT_MSKU_EXP: [False],
                },
            },
        },
        EXPERIMENTAL_PARAM_NUMERIC_ID: {
            'extractor': lambda x: x.numeric_value,
            'expected_values': {
                ORIGINAL_SKU_ID: {
                    DEFAULT_MSKU_EXP: [43.7],
                    EXPERIMENTAL_SKU_ID: [44.7],
                },
            },
            'expected_is_original': {
                ORIGINAL_SKU_ID: {
                    DEFAULT_MSKU_EXP: [False],
                    EXPERIMENTAL_SKU_ID: [False],
                },
            },
            'expected_show_on': SHOW_ON_JUMP_TABLE,
        },
        HYP_PARAM_1_ID: {
            'extractor': lambda x: x.hypothesis_value,
            'expected_values': {
                SINGLE_COLOR_GLOB_SKU: {
                    DEFAULT_MSKU_EXP: [HYP_PARAM_1_NAME],
                },
                MULTI_COLOR_GLOB_SKU: {
                    DEFAULT_MSKU_EXP: [HYP_PARAM_1_NAME],
                },
            },
            'expected_is_original': {
                SINGLE_COLOR_GLOB_SKU: {
                    DEFAULT_MSKU_EXP: [True],
                },
                MULTI_COLOR_GLOB_SKU: {
                    DEFAULT_MSKU_EXP: [True],
                },
            },
            'expected_show_on': SHOW_ON_JUMP_TABLE,
        },
        HYP_PARAM_2_ID: {
            'extractor': lambda x: x.hypothesis_value,
            'expected_values': {},  # no results, because don't show
            'expected_is_original': {},
            'expected_show_on': DONT_SHOW,  # model_filter_index = -1
        },
        # Проверяем, что через связь значение-сетка-вендор будет определено значение 2 как original
        PARAM_SIZE_VALUES: {
            'extractor': lambda x: x.value,
            'expected_values': {
                SIZES_SKU: {
                    DEFAULT_MSKU_EXP: [1, 2, 3],
                },
            },
            'expected_is_original': {
                SIZES_SKU: {
                    DEFAULT_MSKU_EXP: [False, True, False],
                },
            },
            'expected_show_on': SHOW_ON_JUMP_AND_SIZE_TABLE,
        },
    }


@pytest.fixture(scope='module')
def mbo_category_protobufs():
    return [
        MboParameters_pb2.Category(
            hid=CATEG_ID,
            parameter_value_links=[
                MboParameters_pb2.ParameterValueLinks(
                    param_id=PARAM_SIZE_UNITS,
                    linked_param_id=PARAM_SIZE_VALUES,
                    type=MboParameters_pb2.DIRECT,
                    linked_value=[
                        MboParameters_pb2.ValueLink(
                            option_id=10,
                            linked_option_id=[1],
                        ),
                        MboParameters_pb2.ValueLink(
                            option_id=20,
                            linked_option_id=[2],
                        ),
                        MboParameters_pb2.ValueLink(
                            option_id=30,
                            linked_option_id=[3],
                        ),
                    ],
                ),
                MboParameters_pb2.ParameterValueLinks(
                    param_id=PARAM_VENDORS,
                    linked_param_id=PARAM_SIZE_UNITS,
                    type=MboParameters_pb2.DIRECT,
                    linked_value=[
                        MboParameters_pb2.ValueLink(
                            option_id=200,
                            linked_option_id=[20],
                        ),
                    ],
                ),
            ],
            parameter=[
                MboParameters_pb2.Parameter(
                    id=PARAM_BASE_ID,
                    xsl_name=PARAM_BASE_NAME,
                    published=True,
                    value_type=MboParameters_pb2.ENUM,
                    common_filter_index=1,
                    param_type=MboParameters_pb2.MODEL_LEVEL,
                    model_filter_index=1,
                    option=[make_category_option_info_pb(option_id=11, value='base')],
                ),
                MboParameters_pb2.Parameter(
                    id=PARAM_COLOR_GLOB_ID,
                    xsl_name=PARAM_COLOR_GLOB_NAME,
                    published=True,
                    value_type=MboParameters_pb2.ENUM,
                    common_filter_index=1,
                    model_filter_index=1,
                    param_type=MboParameters_pb2.OFFER_LEVEL,
                    option=[
                        make_category_option_info_pb(option_id=21, value='blue'),
                        make_category_option_info_pb(option_id=22, value='red'),
                    ],
                ),
                MboParameters_pb2.Parameter(
                    id=PARAM_NUMERIC_ENUM_ID,
                    xsl_name=PARAM_NUMERIC_ENUM_NAME,
                    published=True,
                    value_type=MboParameters_pb2.NUMERIC_ENUM,
                    common_filter_index=1,
                    model_filter_index=1,
                    param_type=MboParameters_pb2.OFFER_LEVEL,
                    option=[
                        make_category_option_info_pb(option_id=322, value="3.22"),
                        make_category_option_info_pb(option_id=33, value="0.33"),
                    ],
                ),
                MboParameters_pb2.Parameter(
                    id=PARAM_NUMERIC_ID,
                    xsl_name=PARAM_NUMERIC_NAME,
                    published=True,
                    value_type=MboParameters_pb2.NUMERIC,
                    common_filter_index=1,
                    model_filter_index=1,
                    param_type=MboParameters_pb2.OFFER_LEVEL,
                ),
                MboParameters_pb2.Parameter(
                    id=PARAM_BOOL_ID,
                    xsl_name=PARAM_BOOL_NAME,
                    published=True,
                    value_type=MboParameters_pb2.BOOLEAN,
                    common_filter_index=1,
                    model_filter_index=1,
                    param_type=MboParameters_pb2.OFFER_LEVEL,
                ),
                MboParameters_pb2.Parameter(
                    id=HYP_PARAM_1_ID,
                    xsl_name=HYP_PARAM_1_XSL_NAME,
                    published=True,
                    value_type=MboParameters_pb2.STRING,
                    common_filter_index=1,
                    model_filter_index=1,
                    param_type=MboParameters_pb2.OFFER_LEVEL,
                ),
                MboParameters_pb2.Parameter(
                    id=HYP_PARAM_2_ID,
                    xsl_name=HYP_PARAM_2_XSL_NAME,
                    published=True,
                    value_type=MboParameters_pb2.STRING,
                    common_filter_index=1,
                    model_filter_index=-1,
                    param_type=MboParameters_pb2.OFFER_LEVEL,
                ),
                MboParameters_pb2.Parameter(
                    id=EXPERIMENTAL_PARAM_NUMERIC_ID,
                    xsl_name=EXPERIMENTAL_PARAM_NUMERIC_NAME,
                    published=True,
                    value_type=MboParameters_pb2.NUMERIC,
                    common_filter_index=1,
                    param_type=MboParameters_pb2.OFFER_LEVEL,
                    model_filter_index=1,
                ),
                MboParameters_pb2.Parameter(
                    id=PARAM_SIZE_VALUES,
                    xsl_name=PARAM_SIZE_VALUES_NAME,
                    published=True,
                    value_type=MboParameters_pb2.ENUM,
                    param_type=MboParameters_pb2.OFFER_LEVEL,
                    sub_type=MboParameters_pb2.SIZE,
                    model_filter_index=1,
                    option=[
                        make_category_option_info_pb(option_id=1, value="1"),
                        make_category_option_info_pb(option_id=2, value="2"),
                        make_category_option_info_pb(option_id=3, value="3"),
                    ],
                    parameter_link=[
                        MboParameters_pb2.ParameterLink(type=MboParameters_pb2.UNIT, parameter_id=PARAM_SIZE_UNITS)
                    ],
                ),
                MboParameters_pb2.Parameter(
                    id=PARAM_SIZE_UNITS,
                    xsl_name=PARAM_SIZE_UNITS_NAME,
                    value_type=MboParameters_pb2.ENUM,
                    option=[
                        make_category_option_info_pb(option_id=10, value="10"),
                        make_category_option_info_pb(option_id=20, value="20"),
                        make_category_option_info_pb(option_id=30, value="30"),
                    ],
                ),
                MboParameters_pb2.Parameter(
                    id=PARAM_VENDORS,
                    xsl_name=PARAM_VENDORS_NAME,
                    published=True,
                    value_type=MboParameters_pb2.ENUM,
                    common_filter_index=1,
                    param_type=MboParameters_pb2.OFFER_LEVEL,
                    model_filter_index=-1,
                    option=[
                        make_category_option_info_pb(option_id=100, value="100"),
                        make_category_option_info_pb(option_id=200, value="200"),
                        make_category_option_info_pb(option_id=300, value="300"),
                    ],
                ),
            ],
        ),
    ]


@pytest.fixture(scope='module')
def mbo_models_protobufs():
    return [
        make_model_protobuf(
            model_id=MODEL_ID,
            category_id=CATEG_ID,
            relations=[
                Relation(
                    id=EXPERIMENTAL_MODEL_ID,
                    category_id=CATEG_ID,
                    type=EXPERIMENTAL_MODEL
                ),
            ]
        ),
        make_model_protobuf(
            model_id=EXPERIMENTAL_MODEL_ID,
            category_id=CATEG_ID,
            relations=[
                Relation(
                    id=MODEL_ID,
                    category_id=CATEG_ID,
                    type=EXPERIMENTAL_BASE_MODEL
                ),
            ]
        ),
        make_model_protobuf(
            model_id=SIZES_MODEL_ID,
            category_id=CATEG_ID
        ),
    ]


@pytest.fixture(scope='module')
def mbo_msku_protobufs():
    return [
        make_sku_protobuf(
            skuid=SIZES_SKU,
            title="sizes sku",
            model_id=SIZES_MODEL_ID,
            category_id=CATEG_ID,
            parameters=[
                ParameterValue(
                    param_id=PARAM_SIZE_VALUES,
                    xsl_name=PARAM_SIZE_VALUES_NAME,
                    option_id=1,
                    value_type=MboParameters_pb2.ENUM,
                ),
                ParameterValue(
                    param_id=PARAM_SIZE_VALUES,
                    xsl_name=PARAM_SIZE_VALUES_NAME,
                    option_id=2,
                    value_type=MboParameters_pb2.ENUM,
                ),
                ParameterValue(
                    param_id=PARAM_SIZE_VALUES,
                    xsl_name=PARAM_SIZE_VALUES_NAME,
                    option_id=3,
                    value_type=MboParameters_pb2.ENUM,
                ),
            ],
        ),
        make_sku_protobuf(
            skuid=BASE_SKU,
            title="base",
            model_id=MODEL_ID,
            category_id=CATEG_ID,
            parameters=[
                ParameterValue(
                    param_id=PARAM_BASE_ID,
                    xsl_name=PARAM_BASE_NAME,
                    option_id=11,
                    value_type=MboParameters_pb2.ENUM,
                ),
                ParameterValue(
                    param_id=PARAM_NUMERIC_ENUM_ID,
                    xsl_name=PARAM_NUMERIC_ENUM_NAME,
                    option_id=33,
                    value_type=MboParameters_pb2.NUMERIC_ENUM,
                ),
                ParameterValue(
                    param_id=PARAM_NUMERIC_ID,
                    xsl_name=PARAM_NUMERIC_NAME,
                    numeric_value="123.45",
                    value_type=MboParameters_pb2.NUMERIC,
                    rule_id=54321,
                ),
                ParameterValue(
                    param_id=PARAM_BOOL_ID,
                    xsl_name=PARAM_BOOL_NAME,
                    bool_value=True,
                    value_type=MboParameters_pb2.NUMERIC,
                ),
                ParameterValue(
                    param_id=PARAM_NO_DESCR,
                    xsl_name=PARAM_NO_DESCR_NAME,
                    numeric_value="26.87",
                    value_type=MboParameters_pb2.NUMERIC,
                ),
            ],
        ),
        make_sku_protobuf(
            skuid=SINGLE_COLOR_GLOB_SKU,
            title="single color glob",
            model_id=MODEL_ID,
            category_id=CATEG_ID,
            parameters=[
                ParameterValue(
                    param_id=PARAM_COLOR_GLOB_ID,
                    xsl_name=PARAM_COLOR_GLOB_NAME,
                    option_id=21,
                    value_type=MboParameters_pb2.ENUM,
                    image_picker=make_mbo_picker_image_pb(
                        url="some_url1.ru",
                        namespace="mpic",
                        group_id="497",
                        image_name="QWERTY",
                        name="QWERTY_FAKE",
                    ),
                ),
                ParameterValue(
                    param_id=PARAM_NUMERIC_ENUM_ID,
                    xsl_name=PARAM_NUMERIC_ENUM_NAME,
                    option_id=322,
                    value_type=MboParameters_pb2.NUMERIC_ENUM,
                    image_picker=make_mbo_picker_image_pb(
                        url="some_url3.ru",
                        namespace="mpic",
                        group_id="499",
                        image_name="ERTYUI",
                        name="ERTYUI_FAKE",
                    ),
                ),
                ParameterValue(
                    param_id=PARAM_NUMERIC_ENUM_ID,
                    xsl_name=PARAM_NUMERIC_ENUM_NAME,
                    option_id=33,
                    value_type=MboParameters_pb2.NUMERIC_ENUM,
                ),
                ParameterValue(
                    param_id=PARAM_NUMERIC_ID,
                    xsl_name=PARAM_NUMERIC_NAME,
                    numeric_value="322.1",
                    value_type=MboParameters_pb2.NUMERIC,
                ),
                ParameterValue(
                    param_id=PARAM_BOOL_ID,
                    xsl_name=PARAM_BOOL_NAME,
                    bool_value=False,
                    value_type=MboParameters_pb2.NUMERIC,
                ),
            ],
            parameter_hypothesis=[
                ParameterValueHypothesis(
                    param_id=HYP_PARAM_1_ID,
                    xsl_name=HYP_PARAM_1_XSL_NAME,
                    str_value=[
                        MboParameters_pb2.Word(name=HYP_PARAM_1_NAME),
                    ],
                ),
                ParameterValueHypothesis(
                    param_id=HYP_PARAM_2_ID,
                    xsl_name=HYP_PARAM_2_XSL_NAME,
                    str_value=[
                        MboParameters_pb2.Word(name=HYP_PARAM_2_NAME),
                    ],
                ),
            ],
        ),
        make_sku_protobuf(
            skuid=MULTI_COLOR_GLOB_SKU,
            title="multi color glob",
            model_id=MODEL_ID,
            category_id=CATEG_ID,
            parameters=[
                ParameterValue(
                    param_id=PARAM_BASE_ID,
                    xsl_name=PARAM_BASE_NAME,
                    option_id=11,
                    value_type=MboParameters_pb2.ENUM,
                ),
                ParameterValue(
                    param_id=PARAM_NUMERIC_ENUM_ID,
                    xsl_name=PARAM_NUMERIC_ENUM_NAME,
                    option_id=322,
                    value_type=MboParameters_pb2.NUMERIC_ENUM,
                    image_picker=make_mbo_picker_image_pb(
                        url="some_url3.ru",
                        namespace="mpic",
                        group_id="499",
                        image_name="ERTYUI",
                        name="ERTYUI_FAKE",
                    ),
                ),
                ParameterValue(
                    param_id=PARAM_COLOR_GLOB_ID,
                    xsl_name=PARAM_COLOR_GLOB_NAME,
                    option_id=21,
                    value_type=MboParameters_pb2.ENUM,
                    image_picker=make_mbo_picker_image_pb(
                        url="some_url1.ru",
                        namespace="mpic",
                        group_id="497",
                        image_name="QWERTY",
                        name="QWERTY_FAKE",
                    ),
                ),
                ParameterValue(
                    param_id=PARAM_NUMERIC_ID,
                    xsl_name=PARAM_NUMERIC_NAME,
                    numeric_value="54.321",
                    value_type=MboParameters_pb2.NUMERIC,
                ),
                ParameterValue(
                    param_id=PARAM_COLOR_GLOB_ID,
                    xsl_name=PARAM_COLOR_GLOB_NAME,
                    option_id=22,
                    value_type=MboParameters_pb2.ENUM,
                    rule_id=5000,
                    image_picker=make_mbo_picker_image_pb(
                        url="some_url2.ru",
                        namespace="mpic",
                        group_id="498",
                        image_name="WERTYU",
                        name="WERTYU_FAKE",
                    ),
                ),
            ],
            parameter_hypothesis=[
                ParameterValueHypothesis(
                    param_id=HYP_PARAM_1_ID,
                    xsl_name=HYP_PARAM_1_XSL_NAME,
                    str_value=[
                        MboParameters_pb2.Word(name=HYP_PARAM_1_NAME),
                    ],
                ),
                ParameterValueHypothesis(
                    param_id=HYP_PARAM_2_ID,
                    xsl_name=HYP_PARAM_2_XSL_NAME,
                    str_value=[
                        MboParameters_pb2.Word(name=HYP_PARAM_2_NAME),
                    ],
                ),
                ParameterValueHypothesis(
                    param_id=HYP_PARAM_NO_DESCR,
                    xsl_name=HYP_PARAM_NO_DESCR_XSL_NAME,
                    str_value=[
                        MboParameters_pb2.Word(name=HYP_PARAM_NO_DESCR_NAME),
                    ],
                ),
            ],
        ),
        make_sku_protobuf(
            skuid=ORIGINAL_SKU_ID,
            title='Оригинальный MSKU 100000000002',
            model_id=MODEL_ID,
            category_id=CATEG_ID,
            experiment_flag='some_test_id',
            parameters=[
                ParameterValue(
                    param_id=EXPERIMENTAL_PARAM_NUMERIC_ID,
                    xsl_name=EXPERIMENTAL_PARAM_NUMERIC_NAME,
                    numeric_value="43.7",
                    value_type=MboParameters_pb2.NUMERIC,
                    rule_id=54321,
                ),
            ],
            relations=[
                Relation(id=MODEL_ID, category_id=CATEG_ID, type=SKU_PARENT_MODEL),
                Relation(
                    id=EXPERIMENTAL_SKU_ID,
                    category_id=CATEG_ID,
                    type=EXPERIMENTAL_MODEL,
                ),
            ],
        ),
        make_sku_protobuf(
            skuid=EXPERIMENTAL_SKU_ID,
            title='Экспериментальная MSKU для 100000000002',
            model_id=EXPERIMENTAL_MODEL_ID,
            category_id=CATEG_ID,
            experiment_flag='some_test_id',
            parameters=[
                ParameterValue(
                    param_id=EXPERIMENTAL_PARAM_NUMERIC_ID,
                    xsl_name=EXPERIMENTAL_PARAM_NUMERIC_NAME,
                    numeric_value="44.7",
                    value_type=MboParameters_pb2.NUMERIC,
                    rule_id=54321,
                ),
            ],
            relations=[
                Relation(id=EXPERIMENTAL_MODEL_ID, category_id=CATEG_ID, type=SKU_PARENT_MODEL),
                Relation(
                    id=ORIGINAL_SKU_ID,
                    category_id=CATEG_ID,
                    type=EXPERIMENTAL_BASE_MODEL,
                ),
            ],
        ),
    ]


@pytest.yield_fixture(scope='module')
def contex_relations():
    return [
        TContexRelations(
            id=MODEL_ID,
            relations=[{'id': EXPERIMENTAL_MODEL_ID, 'type': EXPERIMENTAL_MODEL}],
            experiment_flag=None,
            is_sku=False,
        ),
        TContexRelations(
            id=EXPERIMENTAL_MODEL_ID,
            relations=[{'id': MODEL_ID, 'type': EXPERIMENTAL_BASE_MODEL}],
            experiment_flag='some_test_id',
            is_sku=False,
        ),
        TContexRelations(
            id=ORIGINAL_SKU_ID,
            relations=[{'id': EXPERIMENTAL_SKU_ID, 'type': EXPERIMENTAL_SKU}],
            experiment_flag=None,
            is_sku=True,
        ),
        TContexRelations(
            id=EXPERIMENTAL_SKU_ID,
            relations=[{'id': ORIGINAL_SKU_ID, 'type': EXPERIMENTAL_BASE_SKU}],
            experiment_flag='some_test_id',
            is_sku=False,
        ),
    ]


def create_cargo_types_table(yt_server):
    yt_client = yt_server.get_yt_client()
    schema = [
        dict(name='id', type='int64'),  # cargo_type_id
        dict(name='mbo_parameter_id', type='int64'),  # mbo_param_id
    ]
    attributes = {'schema': schema}
    table_name = '//home/test/mbo_id_to_cargo_type'
    yt_client.create('table', table_name, ignore_existing=True, recursive=True, attributes=attributes)
    yt_client.write_table(
        table_name,
        [
            dict(id=100, mbo_parameter_id=1000009),
            dict(id=200, mbo_parameter_id=1000010),
            dict(id=400, mbo_parameter_id=1000012),
        ],
    )


@pytest.yield_fixture(scope='module')
def workflow(
        yt_server,
        mbo_msku_protobufs,
        mbo_models_protobufs,
        mbo_category_protobufs,
        contex_relations,
):
    create_cargo_types_table(yt_server)
    resources = {
        "sku": MboSkuPb(mbo_msku_protobufs, CATEG_ID),
        "models": MboModelPb(mbo_models_protobufs, CATEG_ID),
        "parameters": MboCategoryPb(mbo_category_protobufs, CATEG_ID),
        "contex_relations": ContexRelationsPbsn(contex_relations),
    }
    with MskuUploaderTestEnv(jump_table_path="//home/test/jump_table", use_original_by_link=True, contex_enabled=True, **resources) as env:
        env.execute(yt_server)
        env.verify()
        yield env


@pytest.fixture(scope='module')
def result_yt_table(workflow):
    return workflow.outputs.get('jump_table')


@pytest.fixture(scope='module')
def result_exp_records(result_yt_table):
    return [record for record in result_yt_table.data if record.get('msku_exp')]


def test_table_size(result_yt_table, expected_param_values):
    """
    Проверяем, что записей в таблице столько сколько надо
    """
    expected_num = 0
    for param_id in expected_param_values:
        expected_data = expected_param_values[param_id]
        for value in list(expected_data['expected_values'].values()):
            expected_num += len(value)

    assert_that(len(result_yt_table.data), equal_to(expected_num), 'Number of records in table is unexpected')


def test_parameters_values_and_is_original(result_yt_table, expected_param_values):
    """
    Проверяем, что значение параметров правильно преобразовались
    """
    for record in result_yt_table.data:
        fact_jump_table = JumpTableFilter()
        fact_jump_table.ParseFromString(six.ensure_binary(record["jump_filter"]))
        param_id = record["param_id"]
        msku = record["msku"]
        msku_exp = record["msku_exp"]
        expected_data = expected_param_values[param_id]
        extractor = expected_data['extractor']
        expected_values = expected_data['expected_values'][msku][msku_exp]
        actual_values = [extractor(value) for value in fact_jump_table.values]
        assert_that(
            set(actual_values),
            equal_to(set(expected_values)),
            "Incorrect param values for msku: {}, msku_exp: {}, param_id: {}".format(
                msku,
                msku_exp,
                param_id
            )
        )

        expected_is_original = expected_data['expected_is_original'][msku][msku_exp]
        actual_is_original = [value.is_original_value for value in fact_jump_table.values]
        assert_that(
            set(actual_is_original),
            equal_to(set(expected_is_original)),
            "Incorrect param is_original_value flag for msku: {}, msku_exp: {}, param_id: {}".format(
                msku, msku_exp, param_id
            )
        )
        # На момент переходного периода параметры типов BOOL и ENUM
        # пишутся сразу в два поля
        if expected_data.get('second_extractor'):
            extractor = expected_data['second_extractor']
            expected_values = expected_data['second_expected_values'][msku][msku_exp]
            actual_values = [extractor(value) for value in fact_jump_table.values]
            assert_that(
                set(actual_values),
                equal_to(set(expected_values)),
                "Incorrect param values for msku: {}, msku_exp: {}, param_id: {}, for second value field".format(
                    msku, msku_exp, param_id
                )
            )


def test_image_picker(result_yt_table, expected_param_values):
    """
    Проверяем, что у ENUM параметров, у которых есть пикеры
    они проростают в итоговую таблицу.
    У остальных пикеры не прокидываются
    """
    for record in result_yt_table.data:
        fact_jump_table = JumpTableFilter()
        fact_jump_table.ParseFromString(six.ensure_binary(record["jump_filter"]))
        param_id = record["param_id"]
        msku = record["msku"]
        msku_exp = record["msku_exp"]
        expected_data = expected_param_values[param_id]
        expected_pickers = expected_data.get('expected_image_pickers')
        if not expected_pickers:
            assert_that(
                len(fact_jump_table.pickers),
                equal_to(0),
                "No pickers for msku: {}, msku_exp: {}, param_id: {}".format(
                    msku, msku_exp, param_id
                )
            )
        else:
            assert_that(
                len(fact_jump_table.pickers),
                equal_to(len(expected_pickers[msku][msku_exp])),
                "Number of pickers for msku: {}, msku_exp: {}, param_id: {}, is unexpected".format(
                    msku, msku_exp, param_id
                )
            )
            assert_that(
                fact_jump_table.pickers,
                contains_inanyorder(*expected_pickers[msku][msku_exp]),
                "Images of pickers are the different for msku: {}, msku_exp: {}, param_id: {}".format(
                    msku, msku_exp, param_id
                )
            )


def test_show_on(result_yt_table, expected_param_values):
    """
    Проверяем, что значение show_on проставлено верно
    """
    for record in result_yt_table.data:
        fact_jump_table = JumpTableFilter()
        fact_jump_table.ParseFromString(six.ensure_binary(record["jump_filter"]))
        param_id = record["param_id"]
        msku = record["msku"]
        msku_exp = record["msku_exp"]
        expected_data = expected_param_values[param_id]
        expected_show_on = expected_data['expected_show_on']
        assert_that(
            fact_jump_table.show_on,
            equal_to(expected_show_on),
            "Incorrect show on value for msku: {}, msku_exp: {}, param_id: {}".format(
                msku, msku_exp, param_id
            )
        )


def test_exp_count(result_exp_records):
    assert_that(
        len(result_exp_records),
        equal_to(1),
        "Unexpected number records with nonzero field 'msku_exp'"
    )


def test_msku_exp(result_exp_records):
    """
    Проверяем, что строки с ненулевыми msku_exp относятся к верным msku
    """
    exp_to_original_mskus = {
        EXPERIMENTAL_SKU_ID: ORIGINAL_SKU_ID,
    }

    for record in result_exp_records:
        msku = record["msku"]
        msku_exp = record["msku_exp"]
        assert_that(
            msku_exp in exp_to_original_mskus,
            equal_to(True),
            "Unexpected msku_exp {} with msku {}".format(msku_exp, msku),
        )
        assert_that(
            exp_to_original_mskus[msku_exp], equal_to(msku), "Unexpected msku {} for msku_exp {}".format(msku, msku_exp)
        )


def test_msku_exp_model_id(result_exp_records):
    """
    Проверяем что строки с ненулевым msku_exp относятся к оригинальной, а не экспериментальной модели
    """
    for record in result_exp_records:
        assert_that(record['model_id'], equal_to(MODEL_ID))
