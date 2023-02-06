#!/usr/bin/env python
# -*- coding: utf-8 -*-

import os
import pytest
import yatest.common

from datetime import (
    datetime,
    timedelta,
)
from hamcrest import (
    any_of,
    assert_that,
    equal_to,
    has_item,
    has_items,
    is_not,
)

from yt.wrapper import ypath_join

from market.idx.marketindexer.marketindexer import msku
from market.idx.marketindexer.yatf.test_env import MarketIndexer
from market.idx.datacamp.yatf.resources.tokens import YtTokenStub
from market.idx.marketindexer.yatf.resources.common_ini import CommonIni
from market.idx.marketindexer.yatf.resources.state_json import StateJson
from market.proto.msku.jump_table_filters_pb2 import JumpTableFilter
import market.proto.content.mbo.MboParameters_pb2 as MboParameters_pb2
from market.proto.content.mbo.ExportReportModel_pb2 import (
    ParameterValue,
    ParameterValueHypothesis,
)
from market.proto.content.mbo.MboParameters_pb2 import (
    Category,
    Parameter,
)
from market.idx.yatf.resources.resource import FileResource
from market.idx.yatf.resources.yt_stuff_resource import (
    get_yt_prefix,
)
from market.idx.yatf.resources.yt_table_resource import (
    YtTableResource,
)
from msku_uploader.yatf.resources.mbo_pb import (
    MboCategoryPb,
    MboModelPb,
    MboSkuPb,
)
from msku_uploader.yatf.utils import (
    make_sku_protobuf,
    make_model_protobuf,
    make_category_option_info_pb,
)

CATEG_ID = 989040L
MODEL_ID = 1713074440L
BASE_SKU = 100
SINGLE_COLOR_GLOB_SKU = 101
MULTI_COLOR_GLOB_SKU = 102
PARAM_BASE_ID = 1
PARAM_BASE_NAME = 'default_param'
PARAM_COLOR_GLOB_ID = 2
PARAM_COLOR_GLOB_NAME = 'color_glob'
PARAM_NUMERIC_ENUM_ID = 3
PARAM_NUMERIC_ENUM_NAME = 'volume'
HYP_PARAM_1_ID = 6
HYP_PARAM_1_NAME = 'hypo param 1'
DONT_SHOW = 0
SHOW_ON_SNIPPET = 1
SHOW_ON_JUMP_TABLE = 2


def _STUBS_DIR():
    return os.path.join(
        yatest.common.source_path(),
        'market', 'idx', 'marketindexer', 'yatf',
        'resources',
        'stubs',
    )


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
                SINGLE_COLOR_GLOB_SKU: [21],
                MULTI_COLOR_GLOB_SKU: [21, 22],
            },
            # MODEL_LEVEL
            'expected_show_on': SHOW_ON_JUMP_TABLE | SHOW_ON_SNIPPET,
        },
        PARAM_NUMERIC_ENUM_ID: {
            'extractor': lambda x: x.numeric_value,
            'expected_values': {
                BASE_SKU: [0.33],
                SINGLE_COLOR_GLOB_SKU: [3.22, 0.33],
                MULTI_COLOR_GLOB_SKU: [3.22],
            },
            # model_filter_index is negative
            'expected_show_on': SHOW_ON_JUMP_TABLE,
        },
        HYP_PARAM_1_ID: {
            'extractor': lambda x: x.hypothesis_value,
            'expected_values': {
                SINGLE_COLOR_GLOB_SKU: [HYP_PARAM_1_NAME],
                MULTI_COLOR_GLOB_SKU: [HYP_PARAM_1_NAME],
            },
            'expected_show_on': SHOW_ON_JUMP_TABLE,
        },
    }


@pytest.fixture(scope='module')
def mbo_category_protobufs():
    return [
        Category(
            hid=CATEG_ID,
            parameter=[
                Parameter(
                    id=PARAM_BASE_ID,
                    xsl_name=PARAM_BASE_NAME,
                    published=True,
                    value_type=MboParameters_pb2.ENUM,
                    common_filter_index=1,
                    param_type=MboParameters_pb2.MODEL_LEVEL,
                    model_filter_index=1,
                    option=[
                        make_category_option_info_pb(
                            option_id=11,
                            value='base'
                        )
                    ]
                ),
                Parameter(
                    id=PARAM_COLOR_GLOB_ID,
                    xsl_name=PARAM_COLOR_GLOB_NAME,
                    published=True,
                    value_type=MboParameters_pb2.ENUM,
                    common_filter_index=1,
                    model_filter_index=1,
                    param_type=MboParameters_pb2.OFFER_LEVEL,
                    option=[
                        make_category_option_info_pb(
                            option_id=21,
                            value='blue'
                        ),
                        make_category_option_info_pb(
                            option_id=22,
                            value='red'
                        ),
                    ]
                ),
                Parameter(
                    id=PARAM_NUMERIC_ENUM_ID,
                    xsl_name=PARAM_NUMERIC_ENUM_NAME,
                    published=True,
                    value_type=MboParameters_pb2.NUMERIC_ENUM,
                    common_filter_index=1,
                    model_filter_index=1,
                    param_type=MboParameters_pb2.OFFER_LEVEL,
                    option=[
                        make_category_option_info_pb(
                            option_id=322,
                            value="3.22"
                        ),
                        make_category_option_info_pb(
                            option_id=33,
                            value="0.33"
                        ),
                    ]
                ),
                Parameter(
                    id=HYP_PARAM_1_ID,
                    xsl_name=HYP_PARAM_1_NAME,
                    published=True,
                    value_type=MboParameters_pb2.STRING,
                    common_filter_index=1,
                    model_filter_index=1,
                    param_type=MboParameters_pb2.OFFER_LEVEL,
                ),
            ],
        )
    ]


@pytest.fixture(scope='module')
def mbo_models_protobufs():
    return [
        make_model_protobuf(
            model_id=MODEL_ID,
            category_id=CATEG_ID
        )
    ]


@pytest.fixture(scope='module')
def mbo_msku_protobufs():
    return [
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
            ]
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
                ),
                ParameterValue(
                    param_id=PARAM_NUMERIC_ENUM_ID,
                    xsl_name=PARAM_NUMERIC_ENUM_NAME,
                    option_id=322,
                    value_type=MboParameters_pb2.NUMERIC_ENUM,
                ),
                ParameterValue(
                    param_id=PARAM_NUMERIC_ENUM_ID,
                    xsl_name=PARAM_NUMERIC_ENUM_NAME,
                    option_id=33,
                    value_type=MboParameters_pb2.NUMERIC_ENUM,
                ),
            ],
            parameter_hypothesis=[
                ParameterValueHypothesis(
                    param_id=HYP_PARAM_1_ID,
                    xsl_name=HYP_PARAM_1_NAME,
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
                ),
                ParameterValue(
                    param_id=PARAM_COLOR_GLOB_ID,
                    xsl_name=PARAM_COLOR_GLOB_NAME,
                    option_id=21,
                    value_type=MboParameters_pb2.ENUM,
                ),
                ParameterValue(
                    param_id=PARAM_COLOR_GLOB_ID,
                    xsl_name=PARAM_COLOR_GLOB_NAME,
                    option_id=22,
                    value_type=MboParameters_pb2.ENUM,
                ),
            ],
            parameter_hypothesis=[
                ParameterValueHypothesis(
                    param_id=HYP_PARAM_1_ID,
                    xsl_name=HYP_PARAM_1_NAME,
                ),
            ],
        )
    ]


@pytest.yield_fixture(scope='module')
def absolute_bin_path():
    relative_bin_path = os.path.join(
        'market',
        'tools',
        'msku-uploader',
        'bin',
        'msku-uploader'
    )
    return yatest.common.binary_path(relative_bin_path)


@pytest.fixture(scope="module")
def old_table_names():
    hours_deltas = range(1, 10)[::-1]
    table_name_pattern = "%Y%m%d_%H%M"
    names = [
        (datetime.now() - timedelta(hours=hour)).strftime(
            table_name_pattern
        ) for hour in hours_deltas
    ]

    return names


def create_old_tables(yt_server, old_table_names):
    yt_dir = ypath_join(get_yt_prefix(), 'in/jump_table')
    for name in old_table_names:
        client = yt_server.get_yt_client()
        client.create(
            'table',
            os.path.join(yt_dir, name),
            ignore_existing=True,
            recursive=True,
        )

    client.link(
        os.path.join(yt_dir, old_table_names[-1]),
        os.path.join(yt_dir, 'recent'),
    )


@pytest.fixture(scope="module")
def jump_table_dump_dir():
    return os.path.join(yatest.common.work_path(), 'dump_dir')


@pytest.yield_fixture(scope='function')
def workflow(
        yt_server,
        absolute_bin_path,
        jump_table_dump_dir,
        old_table_names,
        mbo_category_protobufs,
        mbo_models_protobufs,
        mbo_msku_protobufs,
):
    yt_token_path = os.path.join(yatest.common.work_path(), 'token')
    cargo_type_template = ypath_join(get_yt_prefix(), '{}/cargo_type/latest')
    resources = {
        'cargo_table': YtTableResource(
            yt_stuff=yt_server,
            path=ypath_join(get_yt_prefix(), 'prestable/cargo_type/latest'),
        ),
        'sku': MboSkuPb(
            export_protos=mbo_msku_protobufs,
            category_id=CATEG_ID,
            prefix=os.path.join('stable', 'models'),
        ),
        'models': MboModelPb(
            export_protos=mbo_models_protobufs,
            category_id=CATEG_ID,
            prefix=os.path.join('stable', 'models'),
        ),
        'parameters': MboCategoryPb(
            export_protos=mbo_category_protobufs,
            category_id=CATEG_ID,
            prefix=os.path.join('stable', 'models'),
        ),
        'yt_token': YtTokenStub(yt_token_path),
        'shopsdat': FileResource(
            os.path.join(_STUBS_DIR(), 'shops-utf8.dat')
        ),
        'state_json': StateJson(),
        'common_ini': CommonIni(
            os.path.join(yatest.common.work_path(), 'common.ini'),
            yatest.common.work_path(),
            misc={
                'msku_upload_enabled': 'true',
                'msku_jump_table_enabled': 'true',
                'cargo_table_template': cargo_type_template,
            },
            yt={
                'home_dir': get_yt_prefix(),
                'yt_proxy_primary': yt_server.get_server(),
                'yt_tokenpath': yt_token_path,
            },
            bin={
                'msku_uploader': absolute_bin_path,
            },
        ),
    }

    with MarketIndexer(yt_server, **resources) as env:
        create_old_tables(yt_server, old_table_names)
        clt_command_args_list = [
            'msku_upload',
            env.input_dir,  # mbo-stuff-path
            env.resources['shopsdat'].path  # shopsdat-path
        ]
        env.execute(clt_command_args_list)
        yield env


@pytest.fixture(scope='function')
def result_yt_table(workflow):
    path = ypath_join(get_yt_prefix(), 'in/jump_table/recent')
    return workflow.get_table_resource_data(path)


def test_parameters_values(result_yt_table, expected_param_values):
    for id, record in enumerate(result_yt_table):
        fact_jump_table = JumpTableFilter()
        fact_jump_table.ParseFromString(record["jump_filter"])
        param_id = record["param_id"]
        msku = record["msku"]
        expected_data = expected_param_values[param_id]
        extractor = expected_data['extractor']
        expected_values = expected_data['expected_values'][msku]
        actual_values = [
            extractor(value)
            for value in fact_jump_table.values
        ]
        assert_that(
            set(actual_values),
            equal_to(set(expected_values)),
            "Correct param values for msku: {}, param_id: {}".format(msku, param_id)
        )


def test_table_rotations(workflow, yt_server, old_table_names):
    client = yt_server.get_yt_client()
    yt_dir = ypath_join(get_yt_prefix(), 'in/jump_table')
    table_names_after = client.list(yt_dir)
    keep_count = msku.KEEP_COUNT
    expected_deleted = old_table_names[:-keep_count]
    expected_available = old_table_names[-keep_count:]
    assert_that(
        table_names_after,
        has_items(*expected_available),
        'Latest old tables are presented'
    )
    assert_that(
        table_names_after,
        is_not(any_of(
            has_item(name)
            for name in expected_deleted
        )),
        'The oldest tables are removed'
    )
