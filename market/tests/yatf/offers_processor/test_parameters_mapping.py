#!/usr/bin/env python
# coding: utf-8
import pytest
from hamcrest import assert_that, equal_to

from market.idx.offers.yatf.resources.offers_indexer.gl_mbo_pb import GlMboPb
from market.idx.offers.yatf.resources.offers_processor.parameters_mapping_pb import ParametersMappingPb
from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.offers.yatf.utils.fixtures import default_genlog
from market.idx.yatf.resources.tovar_tree_pb import MboCategory, TovarTreePbGz
from market.idx.generation.yatf.resources.genlog_dumper.input_records_proto import (
    make_params,
    make_param_entry
)

from market.proto.content.mbo.MboParameters_pb2 import (
    Category,
    BOOLEAN,
    ENUM,
    NUMERIC,
    NUMERIC_ENUM,
    Parameter,
    ParameterMigration,
    OptionMigration,
    Unit,
    Word,
)

from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join
import yt.wrapper as yt


VAL_ID = 1
VAL_NUM = 17.0

CATEGORY_ID1 = 1
CATEGORY_ID2 = 2
CATEGORY_ID3 = 3
CATEGORY_ID4 = 4

CAT1_BOOL_PARAM_OK_MAPPING_SRC = 11
CAT1_BOOL_PARAM_OK_MAPPING_TRG = 12
CAT1_NUMERIC_PARAM_OK_MAPPING_SRC = 13
CAT1_NUMERIC_PARAM_OK_MAPPING_TRG = 14
CAT1_ENUM_PARAM_OK_MAPPING_SRC = 15
CAT1_ENUM_PARAM_OK_MAPPING_TRG = 16
CAT1_NUMERIC_ENUM_PARAM_OK_MAPPING_SRC = 17
CAT1_NUMERIC_ENUM_PARAM_OK_MAPPING_TRG = 18
CAT1_BOOL_PARAM_WITHOUT_MAPPING_SRC = 19
CAT1_NUMERIC_PARAM_WITHOUT_MAPPING_SRC = 20

CAT1_OPTION_WITHOUT_MAPPING_SRC = 153
CAT1_OPTION_OK_MAPPING_SRC = 150
CAT1_OPTION_OK_MAPPING_TRG = 151

CAT2_NO_TRG_PARAM_IN_MBO_SRC = 11
CAT2_NO_TRG_PARAM_IN_MBO_TRG = 12
CAT2_NOT_PUBLISHED_TRG_PARAM_SRC = 13
CAT2_NOT_PUBLISHED_TRG_PARAM_TRG = 14
CAT2_NO_SRC_PARAM_IN_MBO_SRC = 15
CAT2_NO_SRC_PARAM_IN_MBO_TRG = 16
CAT2_NOT_PUBLISHED_SRC_PARAM_SRC = 17
CAT2_NOT_PUBLISHED_SRC_PARAM_TRG = 18
CAT2_NO_SRC_AND_DST_PARAMS_IN_MBO_SRC = 19
CAT2_NO_SRC_AND_DST_PARAMS_IN_MBO_TGT = 20
CAT2_NOT_PUBLISHED_SRC_AND_DST_SRC = 21
CAT2_NOT_PUBLISHED_SRC_AND_DST_TGT = 22

CAT2_OPTION_SRC = 130
CAT2_OPTION_TRG = 131

CAT3_DIFFERENT_PARAM_TYPES_SRC = 11
CAT3_DIFFERENT_PARAM_TYPES_TRG = 12
CAT3_NUMERIC_PARAM_EQUAL_UNITS_SRC = 13
CAT3_NUMERIC_PARAM_EQUAL_UNITS_TRG = 14

CAT3_NUMERIC_PARAM_DIFFERENT_UNITS_SRC = 15
CAT3_NUMERIC_PARAM_DIFFERENT_UNITS_TRG = 16

CAT3_NUMERIC_ENUM_PARAM_EQUAL_UNITS_SRC = 17
CAT3_NUMERIC_ENUM_PARAM_EQUAL_UNITS_TRG = 18
CAT3_NUMERIC_ENUM_PARAM_DIFFERENT_UNITS_SRC = 19
CAT3_NUMERIC_ENUM_PARAM_DIFFERENT_UNITS_TRG = 20

NUMERIC_PARAM_EQUAL_UNIT_ID = 130
NUMERIC_PARAM_UNIT_NAME = 'NUMERIC_PARAM_UNIT_NAME'
NUMERIC_PARAM_DIFFERENT_UNIT_ID1 = 150
NUMERIC_PARAM_DIFFERENT_UNIT_ID2 = 160

CAT4_ENUM_PARAM_NO_OPTIONS_MAPPING_SRC = 11
CAT4_ENUM_PARAM_NO_OPTIONS_MAPPING_TRG = 12
CAT4_ENUM_PARAM_BAD_OPTIONS_MAPPING_SRC = 13
CAT4_ENUM_PARAM_BAD_OPTIONS_MAPPING_TRG = 14

OFFER1_CL_MAGIC_ID = 'ad1d66153519254f804f33eda7868cb1'
OFFER2_CL_MAGIC_ID = 'ad1d66153519254f804f33eda7868cb2'
OFFER3_CL_MAGIC_ID = 'ad1d66153519254f804f33eda7868cb3'
OFFER4_CL_MAGIC_ID = 'ad1d66153519254f804f33eda7868cb4'
OFFER5_CL_MAGIC_ID = 'ad1d66153519254f804f33eda7868cb5'


@pytest.fixture(scope="module")
def tovar_tree():
    return [
        MboCategory(
            hid=CATEGORY_ID1, tovar_id=0,
            unique_name="Все товары", name="Все товары",
            output_type=MboCategory.GURULIGHT),
        MboCategory(
            hid=CATEGORY_ID2, tovar_id=1, parent_hid=CATEGORY_ID1,
            unique_name="Телефоны", name="Телефоны",
            output_type=MboCategory.GURULIGHT),
        MboCategory(
            hid=CATEGORY_ID3, tovar_id=2, parent_hid=CATEGORY_ID1,
            unique_name="Stub", name="Stub",
            output_type=MboCategory.GURULIGHT),
         MboCategory(
            hid=CATEGORY_ID4, tovar_id=3, parent_hid=CATEGORY_ID1,
            unique_name="Stub2", name="Stub2",
            output_type=MboCategory.GURULIGHT),
    ]


@pytest.fixture(scope="module")
def gl_mbo():
    return [
        Category(
            hid=CATEGORY_ID1,
            parameter=[
                Parameter(id=CAT1_BOOL_PARAM_OK_MAPPING_SRC, xsl_name='CAT1_BOOL_PARAM_OK_MAPPING_SRC', value_type=BOOLEAN, published=True),
                Parameter(id=CAT1_BOOL_PARAM_OK_MAPPING_TRG, xsl_name='CAT1_BOOL_PARAM_OK_MAPPING_TRG', value_type=BOOLEAN, published=True),

                Parameter(id=CAT1_NUMERIC_PARAM_OK_MAPPING_SRC, xsl_name='CAT1_NUMERIC_PARAM_OK_MAPPING_SRC', value_type=NUMERIC, published=True),
                Parameter(id=CAT1_NUMERIC_PARAM_OK_MAPPING_TRG, xsl_name='CAT1_NUMERIC_PARAM_OK_MAPPING_TRG', value_type=NUMERIC, published=True),

                Parameter(id=CAT1_ENUM_PARAM_OK_MAPPING_SRC, xsl_name='CAT1_ENUM_PARAM_OK_MAPPING_SRC', value_type=ENUM, published=True),
                Parameter(id=CAT1_ENUM_PARAM_OK_MAPPING_TRG, xsl_name='CAT1_ENUM_PARAM_OK_MAPPING_TRG', value_type=ENUM, published=True),

                Parameter(id=CAT1_NUMERIC_ENUM_PARAM_OK_MAPPING_SRC, xsl_name='CAT1_NUMERIC_ENUM_PARAM_OK_MAPPING_SRC', value_type=NUMERIC_ENUM, published=True),
                Parameter(id=CAT1_NUMERIC_ENUM_PARAM_OK_MAPPING_TRG, xsl_name='CAT1_NUMERIC_ENUM_PARAM_OK_MAPPING_TRG', value_type=NUMERIC_ENUM, published=True),

                Parameter(id=CAT1_BOOL_PARAM_WITHOUT_MAPPING_SRC, xsl_name='CAT1_BOOL_PARAM_WITHOUT_MAPPING_SRC', value_type=BOOLEAN, published=True),
                Parameter(id=CAT1_NUMERIC_PARAM_WITHOUT_MAPPING_SRC, xsl_name='CAT1_NUMERIC_PARAM_WITHOUT_MAPPING_SRC', value_type=NUMERIC, published=True),
            ]
        ),
        Category(
            hid=CATEGORY_ID2,
            parameter=[
                Parameter(id=CAT2_NO_TRG_PARAM_IN_MBO_SRC, xsl_name='CAT2_NO_TRG_PARAM_IN_MBO_SRC', value_type=BOOLEAN, published=True),

                Parameter(id=CAT2_NOT_PUBLISHED_TRG_PARAM_SRC, xsl_name='CAT2_NOT_PUBLISHED_TRG_PARAM_SRC', value_type=BOOLEAN, published=True),
                Parameter(id=CAT2_NOT_PUBLISHED_TRG_PARAM_TRG, xsl_name='CAT2_NOT_PUBLISHED_TRG_PARAM_TRG', value_type=BOOLEAN, published=False),

                Parameter(id=CAT2_NO_SRC_PARAM_IN_MBO_TRG, xsl_name='CAT2_NO_SRC_PARAM_IN_MBO_TRG', value_type=BOOLEAN, published=True),

                Parameter(id=CAT2_NOT_PUBLISHED_SRC_PARAM_SRC, xsl_name='CAT2_NOT_PUBLISHED_SRC_PARAM_SRC', value_type=BOOLEAN, published=False),
                Parameter(id=CAT2_NOT_PUBLISHED_SRC_PARAM_TRG, xsl_name='CAT2_NOT_PUBLISHED_SRC_PARAM_TRG', value_type=BOOLEAN, published=True),

                Parameter(id=CAT2_NOT_PUBLISHED_SRC_AND_DST_SRC, xsl_name='CAT2_NOT_PUBLISHED_SRC_AND_DST_SRC', value_type=BOOLEAN, published=False),
                Parameter(id=CAT2_NOT_PUBLISHED_SRC_AND_DST_TGT, xsl_name='CAT2_NOT_PUBLISHED_SRC_AND_DST_TGT', value_type=BOOLEAN, published=False),
            ]
        ),
        Category(
            hid=CATEGORY_ID3,
            parameter=[
                Parameter(id=CAT3_DIFFERENT_PARAM_TYPES_SRC, xsl_name='CAT3_DIFFERENT_PARAM_TYPES_SRC', value_type=NUMERIC, published=True),
                Parameter(id=CAT3_DIFFERENT_PARAM_TYPES_TRG, xsl_name='CAT3_DIFFERENT_PARAM_TYPES_TRG', value_type=ENUM, published=True),
                Parameter(id=CAT3_NUMERIC_PARAM_EQUAL_UNITS_SRC, xsl_name='CAT3_NUMERIC_PARAM_EQUAL_UNITS_SRC', value_type=NUMERIC, published=True,
                          unit=Unit(id=NUMERIC_PARAM_EQUAL_UNIT_ID, name=[Word(name=NUMERIC_PARAM_UNIT_NAME)])),
                Parameter(id=CAT3_NUMERIC_PARAM_EQUAL_UNITS_TRG, xsl_name='CAT3_NUMERIC_PARAM_EQUAL_UNITS_TRG', value_type=NUMERIC, published=True,
                          unit=Unit(id=NUMERIC_PARAM_EQUAL_UNIT_ID, name=[Word(name=NUMERIC_PARAM_UNIT_NAME)])),
                Parameter(id=CAT3_NUMERIC_PARAM_DIFFERENT_UNITS_SRC, xsl_name='CAT3_NUMERIC_PARAM_DIFFERENT_UNITS_SRC', value_type=NUMERIC, published=True,
                          unit=Unit(id=NUMERIC_PARAM_DIFFERENT_UNIT_ID1, name=[Word(name=NUMERIC_PARAM_UNIT_NAME)])),
                Parameter(id=CAT3_NUMERIC_PARAM_DIFFERENT_UNITS_TRG, xsl_name='CAT3_NUMERIC_PARAM_DIFFERENT_UNITS_TRG', value_type=NUMERIC, published=True,
                          unit=Unit(id=NUMERIC_PARAM_DIFFERENT_UNIT_ID2, name=[Word(name=NUMERIC_PARAM_UNIT_NAME)])),
                Parameter(id=CAT3_NUMERIC_ENUM_PARAM_EQUAL_UNITS_SRC, xsl_name='CAT3_NUMERIC_ENUM_PARAM_EQUAL_UNITS_SRC', value_type=NUMERIC_ENUM, published=True,
                          unit=Unit(id=NUMERIC_PARAM_EQUAL_UNIT_ID, name=[Word(name=NUMERIC_PARAM_UNIT_NAME)])),
                Parameter(id=CAT3_NUMERIC_ENUM_PARAM_EQUAL_UNITS_TRG, xsl_name='CAT3_NUMERIC_ENUM_PARAM_EQUAL_UNITS_TRG', value_type=NUMERIC_ENUM, published=True,
                          unit=Unit(id=NUMERIC_PARAM_EQUAL_UNIT_ID, name=[Word(name=NUMERIC_PARAM_UNIT_NAME)])),
                Parameter(id=CAT3_NUMERIC_ENUM_PARAM_DIFFERENT_UNITS_SRC, xsl_name='CAT3_NUMERIC_ENUM_PARAM_DIFFERENT_UNITS_SRC', value_type=NUMERIC_ENUM, published=True,
                          unit=Unit(id=NUMERIC_PARAM_DIFFERENT_UNIT_ID1, name=[Word(name=NUMERIC_PARAM_UNIT_NAME)])),
                Parameter(id=CAT3_NUMERIC_ENUM_PARAM_DIFFERENT_UNITS_TRG, xsl_name='CAT3_NUMERIC_ENUM_PARAM_DIFFERENT_UNITS_TRG', value_type=NUMERIC_ENUM, published=True,
                          unit=Unit(id=NUMERIC_PARAM_DIFFERENT_UNIT_ID2, name=[Word(name=NUMERIC_PARAM_UNIT_NAME)])),
            ]
        ),
        Category(
            hid=CATEGORY_ID4,
            parameter=[
                Parameter(id=CAT4_ENUM_PARAM_NO_OPTIONS_MAPPING_SRC, xsl_name='CAT4_ENUM_PARAM_NO_OPTIONS_MAPPING_SRC', value_type=ENUM, published=True),
                Parameter(id=CAT4_ENUM_PARAM_NO_OPTIONS_MAPPING_TRG, xsl_name='CAT4_ENUM_PARAM_NO_OPTIONS_MAPPING_TRG', value_type=ENUM, published=True),
                Parameter(id=CAT4_ENUM_PARAM_BAD_OPTIONS_MAPPING_SRC, xsl_name='CAT4_ENUM_PARAM_BAD_OPTIONS_MAPPING_SRC', value_type=ENUM, published=True),
                Parameter(id=CAT4_ENUM_PARAM_BAD_OPTIONS_MAPPING_TRG, xsl_name='CAT4_ENUM_PARAM_BAD_OPTIONS_MAPPING_TRG', value_type=ENUM, published=True),
            ]
        )
    ]


@pytest.fixture(scope="module")
def parameters_mapping():
    return [
        Category(
            hid=CATEGORY_ID1,
            parameters_mapping=[
                ParameterMigration(target_param_id=CAT1_BOOL_PARAM_OK_MAPPING_TRG, source_param_id=CAT1_BOOL_PARAM_OK_MAPPING_SRC),
                ParameterMigration(target_param_id=CAT1_NUMERIC_PARAM_OK_MAPPING_TRG, source_param_id=CAT1_NUMERIC_PARAM_OK_MAPPING_SRC),
                ParameterMigration(target_param_id=CAT1_ENUM_PARAM_OK_MAPPING_TRG, source_param_id=CAT1_ENUM_PARAM_OK_MAPPING_SRC,
                                   options_migration=[OptionMigration(source_option_id=CAT1_OPTION_OK_MAPPING_SRC, target_option_id=CAT1_OPTION_OK_MAPPING_TRG)]),
                ParameterMigration(target_param_id=CAT1_NUMERIC_ENUM_PARAM_OK_MAPPING_TRG, source_param_id=CAT1_NUMERIC_ENUM_PARAM_OK_MAPPING_SRC),
            ]
        ),
        Category(
            hid=CATEGORY_ID2,
            parameters_mapping=[
                ParameterMigration(target_param_id=CAT2_NO_TRG_PARAM_IN_MBO_TRG, source_param_id=CAT2_NO_TRG_PARAM_IN_MBO_SRC),
                ParameterMigration(target_param_id=CAT2_NOT_PUBLISHED_TRG_PARAM_TRG, source_param_id=CAT2_NOT_PUBLISHED_TRG_PARAM_SRC,
                                   options_migration=[OptionMigration(source_option_id=CAT2_OPTION_SRC, target_option_id=CAT2_OPTION_TRG)]),
                ParameterMigration(target_param_id=CAT2_NO_SRC_PARAM_IN_MBO_TRG, source_param_id=CAT2_NO_SRC_PARAM_IN_MBO_SRC),
                ParameterMigration(target_param_id=CAT2_NOT_PUBLISHED_SRC_PARAM_TRG, source_param_id=CAT2_NOT_PUBLISHED_SRC_PARAM_SRC),
                ParameterMigration(target_param_id=CAT2_NO_SRC_AND_DST_PARAMS_IN_MBO_TGT, source_param_id=CAT2_NO_SRC_AND_DST_PARAMS_IN_MBO_SRC),
                ParameterMigration(target_param_id=CAT2_NOT_PUBLISHED_SRC_AND_DST_TGT, source_param_id=CAT2_NOT_PUBLISHED_SRC_AND_DST_SRC),
            ]
        ),
        Category(
            hid=CATEGORY_ID3,
            parameters_mapping=[
                ParameterMigration(target_param_id=CAT3_DIFFERENT_PARAM_TYPES_TRG, source_param_id=CAT3_DIFFERENT_PARAM_TYPES_SRC),
                ParameterMigration(target_param_id=CAT3_NUMERIC_PARAM_EQUAL_UNITS_TRG, source_param_id=CAT3_NUMERIC_PARAM_EQUAL_UNITS_SRC),
                ParameterMigration(target_param_id=CAT3_NUMERIC_PARAM_DIFFERENT_UNITS_TRG, source_param_id=CAT3_NUMERIC_PARAM_DIFFERENT_UNITS_SRC),
                ParameterMigration(target_param_id=CAT3_NUMERIC_ENUM_PARAM_EQUAL_UNITS_TRG, source_param_id=CAT3_NUMERIC_ENUM_PARAM_EQUAL_UNITS_SRC),
                ParameterMigration(target_param_id=CAT3_NUMERIC_ENUM_PARAM_DIFFERENT_UNITS_TRG, source_param_id=CAT3_NUMERIC_ENUM_PARAM_DIFFERENT_UNITS_SRC),
            ]
        ),
        Category(
            hid=CATEGORY_ID4,
            parameters_mapping=[
                ParameterMigration(target_param_id=CAT4_ENUM_PARAM_NO_OPTIONS_MAPPING_TRG, source_param_id=CAT4_ENUM_PARAM_NO_OPTIONS_MAPPING_SRC),
                ParameterMigration(target_param_id=CAT4_ENUM_PARAM_BAD_OPTIONS_MAPPING_TRG, source_param_id=CAT4_ENUM_PARAM_BAD_OPTIONS_MAPPING_SRC,
                                   options_migration=[OptionMigration(source_option_id=1000, target_option_id=2000)]),
            ]
        )
    ]


@pytest.fixture(scope="module")
def genlog_rows(yt_server):
    offers = [
        default_genlog(
            classifier_magic_id=OFFER1_CL_MAGIC_ID,
            category_id=CATEGORY_ID1,
            offer_params=[
                {'enriched_param_id': yt.yson.YsonUint64(CAT1_BOOL_PARAM_OK_MAPPING_SRC), 'id': yt.yson.YsonUint64(VAL_ID), 'original': True},
                {'enriched_param_id': yt.yson.YsonUint64(CAT1_NUMERIC_PARAM_OK_MAPPING_SRC), 'num': VAL_NUM, 'original': True},
                {'enriched_param_id': yt.yson.YsonUint64(CAT1_BOOL_PARAM_WITHOUT_MAPPING_SRC), 'id': yt.yson.YsonUint64(VAL_ID), 'original': True},
            ]
        ),
        default_genlog(
            classifier_magic_id=OFFER2_CL_MAGIC_ID,
            category_id=CATEGORY_ID1,
            offer_params=[
                {'enriched_param_id': yt.yson.YsonUint64(CAT1_ENUM_PARAM_OK_MAPPING_SRC), 'id': yt.yson.YsonUint64(CAT1_OPTION_OK_MAPPING_SRC), 'original': True},
                {'enriched_param_id': yt.yson.YsonUint64(CAT1_ENUM_PARAM_OK_MAPPING_SRC), 'id': yt.yson.YsonUint64(CAT1_OPTION_WITHOUT_MAPPING_SRC), 'original': True},
                {'enriched_param_id': yt.yson.YsonUint64(CAT1_NUMERIC_ENUM_PARAM_OK_MAPPING_SRC), 'num': VAL_NUM, 'original': True},
                {'enriched_param_id': yt.yson.YsonUint64(CAT1_NUMERIC_PARAM_WITHOUT_MAPPING_SRC), 'num': VAL_NUM, 'original': True},
            ]
        ),
        default_genlog(
            classifier_magic_id=OFFER3_CL_MAGIC_ID,
            category_id=CATEGORY_ID2,
            offer_params=[
                {'enriched_param_id': yt.yson.YsonUint64(CAT2_NO_TRG_PARAM_IN_MBO_SRC), 'id': yt.yson.YsonUint64(VAL_ID), 'original': True},
                {'enriched_param_id': yt.yson.YsonUint64(CAT2_NOT_PUBLISHED_TRG_PARAM_SRC), 'id': yt.yson.YsonUint64(CAT2_OPTION_SRC), 'original': True},
                {'enriched_param_id': yt.yson.YsonUint64(CAT2_NO_SRC_PARAM_IN_MBO_SRC), 'id': yt.yson.YsonUint64(VAL_ID), 'original': True},
                {'enriched_param_id': yt.yson.YsonUint64(CAT2_NOT_PUBLISHED_SRC_PARAM_SRC), 'id': yt.yson.YsonUint64(VAL_ID), 'original': True},
                {'enriched_param_id': yt.yson.YsonUint64(CAT2_NO_SRC_AND_DST_PARAMS_IN_MBO_SRC), 'id': yt.yson.YsonUint64(VAL_ID), 'original': True},
                {'enriched_param_id': yt.yson.YsonUint64(CAT2_NOT_PUBLISHED_SRC_AND_DST_SRC), 'id': yt.yson.YsonUint64(VAL_ID), 'original': True},
            ]
        ),
        default_genlog(
            classifier_magic_id=OFFER4_CL_MAGIC_ID,
            category_id=CATEGORY_ID3,
            offer_params=[
                {'enriched_param_id': yt.yson.YsonUint64(CAT3_DIFFERENT_PARAM_TYPES_SRC), 'num': VAL_NUM, 'original': True},
                {'enriched_param_id': yt.yson.YsonUint64(CAT3_NUMERIC_PARAM_EQUAL_UNITS_SRC), 'num': VAL_NUM, 'original': True},
                {'enriched_param_id': yt.yson.YsonUint64(CAT3_NUMERIC_PARAM_DIFFERENT_UNITS_SRC), 'num': VAL_NUM, 'original': True},
                {'enriched_param_id': yt.yson.YsonUint64(CAT3_NUMERIC_ENUM_PARAM_EQUAL_UNITS_SRC), 'num': VAL_NUM, 'original': True},
                {'enriched_param_id': yt.yson.YsonUint64(CAT3_NUMERIC_ENUM_PARAM_DIFFERENT_UNITS_SRC), 'num': VAL_NUM, 'original': True},
            ]
        ),
        default_genlog(
            classifier_magic_id=OFFER5_CL_MAGIC_ID,
            category_id=CATEGORY_ID4,
            offer_params=[
                {'enriched_param_id': yt.yson.YsonUint64(CAT4_ENUM_PARAM_NO_OPTIONS_MAPPING_SRC), 'id': yt.yson.YsonUint64(VAL_ID), 'original': True},
                {'enriched_param_id': yt.yson.YsonUint64(CAT4_ENUM_PARAM_BAD_OPTIONS_MAPPING_SRC), 'id': yt.yson.YsonUint64(VAL_ID), 'original': True},
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
def workflow(genlog_table, gl_mbo, parameters_mapping, tovar_tree, yt_server):
    input_table_paths = [genlog_table.get_path()]

    resources = {
        'gl_mbo_pbuf_sn': GlMboPb(gl_mbo),
        'parameters_mapping_pb': ParametersMappingPb(parameters_mapping),
        'tovar_tree_pb': TovarTreePbGz(tovar_tree),
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


EXPECTED_DATA = [
    {
        "classifier_magic_id": OFFER1_CL_MAGIC_ID,
        "params_entry": make_params(
            model=0,
            category=CATEGORY_ID1,
            values=[
                make_param_entry(key=CAT1_BOOL_PARAM_OK_MAPPING_TRG, id=VAL_ID, original=True),  # удачный маппинг bool-параметра
                make_param_entry(key=CAT1_NUMERIC_PARAM_OK_MAPPING_TRG, num=VAL_NUM, original=True),  # удачный маппинг numeric-параметра
                make_param_entry(key=CAT1_BOOL_PARAM_WITHOUT_MAPPING_SRC, id=VAL_ID, original=True),  # у параметра не задан маппинг
            ]
        )
    },
    {
        "classifier_magic_id": OFFER2_CL_MAGIC_ID,
        "params_entry": make_params(
            model=0,
            category=CATEGORY_ID1,
            values=[
                make_param_entry(key=CAT1_ENUM_PARAM_OK_MAPPING_SRC, id=CAT1_OPTION_WITHOUT_MAPPING_SRC, original=True),  # маппинг enum-параметра не прошел, т.к. нет маппинга для опции
                make_param_entry(key=CAT1_ENUM_PARAM_OK_MAPPING_TRG, id=CAT1_OPTION_OK_MAPPING_TRG, original=True),  # удачный маппинг того же enum-параметра с другим option_id
                make_param_entry(key=CAT1_NUMERIC_ENUM_PARAM_OK_MAPPING_TRG, num=VAL_NUM, original=True),  # удачный маппинг numeric enum
                make_param_entry(key=CAT1_NUMERIC_PARAM_WITHOUT_MAPPING_SRC, num=VAL_NUM, original=True),  # у параметра не задан маппинг
            ]
        )
    },
    {
        "classifier_magic_id": OFFER3_CL_MAGIC_ID,
        "params_entry": make_params(
            model=0,
            category=CATEGORY_ID2,
            values=[
                make_param_entry(key=CAT2_NO_TRG_PARAM_IN_MBO_SRC, id=VAL_ID, original=True),  # нет маппинга, т.к. нет target-параметра в выгрузке мбо
                make_param_entry(key=CAT2_NOT_PUBLISHED_TRG_PARAM_SRC, id=CAT2_OPTION_SRC, original=True),  # нет маппинга, т.к. target-параметр не опубликован
                make_param_entry(key=CAT2_NOT_PUBLISHED_SRC_PARAM_TRG, id=VAL_ID, original=True),  # удачный маппинг, хоть исходный параметр и не опубликован
                # нет исходного параметра в выгрузке мбо - нет маппинга (т.к. не определить тип параметра) + исходный параметр отфильтровался
                # нет исходного и целевого параметров в выгрузке мбо - нет маппинга + исходный параметр отфильтровался
                # исходный и целевой параметры не опубликованы - нет маппинга + исходный параметр отфильтровался
            ]
        )
    },
    {
        "classifier_magic_id": OFFER4_CL_MAGIC_ID,
        "params_entry": make_params(
            model=0,
            category=CATEGORY_ID3,
            values=[
                make_param_entry(key=CAT3_DIFFERENT_PARAM_TYPES_SRC, num=VAL_NUM, original=True),  # нет маппинга, т.к. у параметров разные типы
                make_param_entry(key=CAT3_NUMERIC_PARAM_EQUAL_UNITS_TRG, num=VAL_NUM, original=True),  # удачный маппинг, т.к. у numeric-парамтеров одинаковые ед. измерения
                make_param_entry(key=CAT3_NUMERIC_PARAM_DIFFERENT_UNITS_SRC, num=VAL_NUM, original=True),  # нет маппинга, т.к. у numeric-парамтеров разные ед. измерения
                make_param_entry(key=CAT3_NUMERIC_ENUM_PARAM_EQUAL_UNITS_TRG, num=VAL_NUM, original=True),  # удачный маппинг, т.к. у numeric enum парамтеров одинаковые ед. измерения
                make_param_entry(key=CAT3_NUMERIC_ENUM_PARAM_DIFFERENT_UNITS_SRC, num=VAL_NUM, original=True),  # нет маппинга, т.к. у numeric enum парамтеров разные ед. измерения
            ]
        )
    },
    {
        "classifier_magic_id": OFFER5_CL_MAGIC_ID,
        "params_entry": make_params(
            model=0,
            category=CATEGORY_ID4,
            values=[
                make_param_entry(key=CAT4_ENUM_PARAM_NO_OPTIONS_MAPPING_SRC, id=VAL_ID, original=True),  # нет маппинга, т.к. для enum-параметра не задано маппинга опций
                make_param_entry(key=CAT4_ENUM_PARAM_BAD_OPTIONS_MAPPING_SRC, id=VAL_ID, original=True),  # нет маппинга enum-параметра, т.к. у него нет маппинга для опции
            ]
        )
    }
]


def test_mapping(workflow):
    for (index, record) in workflow.genlog_with_row_index:
        assert_that(record.classifier_magic_id, equal_to(EXPECTED_DATA[index]['classifier_magic_id']))
        assert_that(record.params_entry, equal_to(EXPECTED_DATA[index]['params_entry']))
