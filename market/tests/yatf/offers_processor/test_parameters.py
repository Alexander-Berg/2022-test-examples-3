#!/usr/bin/env python
# coding: utf-8
import pytest

from market.idx.offers.yatf.resources.offers_indexer.gl_mbo_pb import GlMboPb
from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.offers.yatf.utils.fixtures import default_genlog
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join
import yt.wrapper as yt

from market.proto.content.mbo.MboParameters_pb2 import (
    Category,
    BOOLEAN,
    ENUM,
    NUMERIC,
    NUMERIC_ENUM,
    Parameter,
)


VAL_ID = 1
VAL_NUM = 17.0


@pytest.fixture(scope="module")
def gl_mbo():
    return [
        Category(
            hid=90401,
            parameter=[
                Parameter(id=11, xsl_name='bool_param1', value_type=BOOLEAN, published=True),
                Parameter(id=12, xsl_name='bool_param2', value_type=BOOLEAN, published=True),
                Parameter(id=13, xsl_name='bool_param3', value_type=BOOLEAN, published=True),
                Parameter(id=14, xsl_name='bool_param4', value_type=BOOLEAN, published=True),

                Parameter(id=21, xsl_name='numeric_param1', value_type=NUMERIC, published=True),
                Parameter(id=22, xsl_name='numeric_param2', value_type=NUMERIC, published=True),
                Parameter(id=23, xsl_name='numeric_param3', value_type=NUMERIC, published=True),
                Parameter(id=24, xsl_name='numeric_param4', value_type=NUMERIC, published=True),

                Parameter(id=31, xsl_name='enum_param1', value_type=ENUM, published=True),
                Parameter(id=32, xsl_name='enum_param2', value_type=ENUM, published=True),
                Parameter(id=33, xsl_name='enum_param3', value_type=ENUM, published=True),
                Parameter(id=34, xsl_name='enum_param4', value_type=ENUM, published=True),

                Parameter(id=41, xsl_name='numeric_enum_param1', value_type=NUMERIC_ENUM, published=True),
                Parameter(id=42, xsl_name='numeric_enum_param2', value_type=NUMERIC_ENUM, published=True),
                Parameter(id=43, xsl_name='numeric_enum_param3', value_type=NUMERIC_ENUM, published=True),
                Parameter(id=44, xsl_name='numeric_enum_param4', value_type=NUMERIC_ENUM, published=True),

                Parameter(id=5, xsl_name='enum_param_', value_type=ENUM, published=True, highlight_original_value=True),
            ]
        )
    ]


@pytest.fixture(scope="module")
def genlog_rows(yt_server):
    offers = [
        default_genlog(  # just simple offer
            classifier_magic_id="ad1d66153519254f804f33eda7868cbd",
            category_id=90401,
            offer_params=[
                {'enriched_param_id': yt.yson.YsonUint64(11), 'id': yt.yson.YsonUint64(VAL_ID)},
                {'enriched_param_id': yt.yson.YsonUint64(21), 'num': VAL_NUM},
            ]
        ),
        default_genlog(
            classifier_magic_id="614787758f7d87a279d2d808f5ef747a",
            category_id=90401,
            offer_params=[
                {'enriched_param_id': yt.yson.YsonUint64(11), 'id': yt.yson.YsonUint64(VAL_ID)},
                {'enriched_param_id': yt.yson.YsonUint64(21), 'num': VAL_NUM},
            ]
        ),
        default_genlog(  # ignored offer
            classifier_magic_id="a8e5eedceac12373ade6d2b701f8e037",
            category_id=90401,
            offer_params=[
                {'enriched_param_id': yt.yson.YsonUint64(33), 'id': yt.yson.YsonUint64(VAL_ID)},
                {'enriched_param_id': yt.yson.YsonUint64(5), 'num': VAL_NUM},  # ignored, was special case
            ]
        ),
        default_genlog(
            classifier_magic_id="9dae2a7d11a152241190838e439bcd84",
            category_id=90401,
            offer_params=[
                # BOOL
                {'enriched_param_id': yt.yson.YsonUint64(11), 'id': yt.yson.YsonUint64(0)},  # BOOL will read "id == 0"
                {'enriched_param_id': yt.yson.YsonUint64(12), 'id': yt.yson.YsonUint64(VAL_ID)},  # BOOL
                {'enriched_param_id': yt.yson.YsonUint64(14), 'id': yt.yson.YsonUint64(VAL_ID)},  # numeric
                # NUMERIC
                {'enriched_param_id': yt.yson.YsonUint64(23), 'num': VAL_NUM},  # numeric
                {'enriched_param_id': yt.yson.YsonUint64(24), 'id': yt.yson.YsonUint64(VAL_ID), 'num': VAL_NUM},  # numeric

                # ENUM
                {'enriched_param_id': yt.yson.YsonUint64(31), 'id': yt.yson.YsonUint64(0)},  # ENUM will read "id == 0"
                {'enriched_param_id': yt.yson.YsonUint64(32), 'id': yt.yson.YsonUint64(VAL_ID)},  # ENUM
                {'enriched_param_id': yt.yson.YsonUint64(34), 'id': yt.yson.YsonUint64(VAL_ID), 'num': VAL_NUM},  # ENUM

                # NUMERIC_ENUM
                {'enriched_param_id': yt.yson.YsonUint64(43), 'num': VAL_NUM},  # numeric
                {'enriched_param_id': yt.yson.YsonUint64(44), 'id': yt.yson.YsonUint64(VAL_ID), 'num': VAL_NUM},  # numeric
            ]
        ),

        # Multi values param
        default_genlog(  # ignored offer
            classifier_magic_id="dddddedceac12373ade6d2b701f8e037",
            category_id=90401,
            offer_params=[
                {'enriched_param_id': yt.yson.YsonUint64(32), 'id': yt.yson.YsonUint64(VAL_ID)},
                {'enriched_param_id': yt.yson.YsonUint64(32), 'id': yt.yson.YsonUint64(VAL_ID+1)},
                {'enriched_param_id': yt.yson.YsonUint64(33), 'id': yt.yson.YsonUint64(VAL_ID+0)},
                {'enriched_param_id': yt.yson.YsonUint64(33), 'id': yt.yson.YsonUint64(VAL_ID+1)},
                {'enriched_param_id': yt.yson.YsonUint64(33), 'id': yt.yson.YsonUint64(VAL_ID+2)},
                {'enriched_param_id': yt.yson.YsonUint64(33), 'id': yt.yson.YsonUint64(VAL_ID+3)},
            ]
        ),

    ]
    return offers


@pytest.fixture(scope="module")
def genlog_table(yt_server, genlog_rows):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0000'), genlog_rows)
    genlog_table.dump()
    return genlog_table


@pytest.yield_fixture(scope="module")
def workflow(genlog_table, gl_mbo, yt_server):
    input_table_paths = [genlog_table.get_path()]

    resources = {
        'gl_mbo_pbuf_sn': GlMboPb(gl_mbo)
    }

    with OffersProcessorTestEnv(
            yt_server,
            use_genlog_scheme=True,
            input_table_paths=input_table_paths,
            **resources
    ) as env:
        env.execute()
        env.verify()
        yield env


def test_generation_log_nrecords(workflow):
    assert len(list(workflow.genlog)) == 5


@pytest.mark.parametrize('offer', [0, 1])
def test_simple_record(workflow, offer):
    record = workflow.genlog[offer]

    assert len(record.mbo_params) == 2

    assert len(record.mbo_params[0].values) == 1
    assert record.mbo_params[0].values[0] == VAL_ID

    assert len(record.mbo_params[1].values) == 0

    assert len(record.numeric_params) == 1
    assert len(record.numeric_params[0].ranges) == 1
    assert record.numeric_params[0].ranges[0] == VAL_NUM


def test_big_record(workflow):
    for genlog in workflow.genlog:
        if genlog.classifier_magic_id == "9dae2a7d11a152241190838e439bcd84":
            break

    assert len(genlog.mbo_params) == 10
    assert len(genlog.numeric_params) == 4

    id2val = {
        11: 0, 12: VAL_ID, 13: None, 14: VAL_ID,         # BOOL
        21: None, 22: None, 23: None, 24: None,          # NUMERIC
        31: 0, 32: VAL_ID, 33: None, 34: VAL_ID,         # ENUM
        41: None, 42: None, 43: None, 44: None,          # NUMERIC_ENUM
    }

    for param in genlog.mbo_params:
        id = param.id
        assert id in id2val, "unexpected id={}".format(id)
        expected = id2val[id]
        if expected is None:
            assert len(param.values) == 0, "unexpected value for id={}".format(id)
        else:
            assert len(param.values) == 1, "expected one value for id={}, got {}".format(id, len(param.values))
            assert param.values[0] == expected, "expected '{}' for id={}, got '{}'".format(param.values[0], id, expected)


def test_multi_values(workflow):
    record = workflow.genlog[4]
    assert len(record.mbo_params) == 2
    assert len(record.numeric_params) == 0

    id2val = {
        11: None, 12: None, 13: None, 14: None,         # BOOL
        21: None, 22: None, 23: None, 24: None,         # NUMERIC
        31: None, 32: [VAL_ID, VAL_ID+1], 33: [VAL_ID, VAL_ID+1, VAL_ID+2, VAL_ID+3], 34: None,         # ENUM
        41: None, 42: None, 43: None, 44: None,         # NUMERIC_ENUM
    }

    for param in record.mbo_params:
        id = param.id
        assert id in id2val, "unexpected id={}".format(id)
        expected = id2val[id]
        if expected is None:
            assert len(param.values) == 0, "unexpected value for id={}".format(id)
        else:
            assert len(param.values) == len(expected), "expected {} values for id={}, got {}".format(len(expected), id, len(param.values))
            assert param.values == expected, "expected '{}' for id={}, got '{}'".format(id, param.values, expected)
