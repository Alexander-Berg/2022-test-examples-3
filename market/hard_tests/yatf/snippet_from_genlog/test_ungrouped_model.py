# coding=utf-8

from hamcrest import assert_that
import pytest

from market.idx.offers.yatf.resources.offers_indexer.gl_mbo_pb import GlMboPb
from market.idx.yatf.resources.model_ids import ModelIds
from market.idx.offers.yatf.resources.offers_indexer.ungrouping_models import (
    UngroupingModelParams,
    UngroupingModels,
)

from market.idx.generation.yatf.matchers.arc_indexer.env_matchers import HasArchiveEntry
from market.idx.generation.yatf.matchers.snippet_diff_builder.env_matchers import HasOutputStateRecord

from market.idx.generation.yatf.test_envs.arc_indexer import IndexarcTestEnv
from market.idx.generation.yatf.test_envs.mr_mindexer import MrMindexerBuildTestEnv, MrMindexerMergeTestEnv
from market.idx.generation.yatf.test_envs.snippet_diff_builder import SnippetDiffBuilderTestEnv
from market.idx.generation.yatf.resources.mr_mindexer.mr_mindexer_helpers import MrMindexerMergeOptions, MrMindexerMergeIndexType

from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.offers.yatf.utils.fixtures import default_genlog

from market.proto.content.mbo.MboParameters_pb2 import (
    Category,
    Parameter,
    BOOLEAN,
    ENUM,
    NUMERIC,
)
from market.proto.content.mbo.ExportReportModel_pb2 import (
    ExportReportModel,
    UngroupingInfo,
    ParameterValue,
)
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join
import yt.wrapper as yt


CATEGORY_ID = 90401
MODEL_ID = 1
WHITE_MODEL_ID = 2

BOOL_PARAM_1 = 11

NUM_PARAM_1 = 21
NUM_PARAM_2 = 22


def is_num_param(param_id):
    return param_id in [NUM_PARAM_1, NUM_PARAM_2]


ENUM_PARAM_1 = 31
ENUM_PARAM_2 = 32


@pytest.fixture(scope='module')
def gl_mbo():
    return [
        Category(
            hid=CATEGORY_ID,
            parameter=[
                Parameter(
                    id=11,
                    xsl_name='bool_param1',
                    value_type=BOOLEAN,
                    published=True
                ),
                Parameter(
                    id=21,
                    xsl_name='numeric_param1',
                    value_type=NUMERIC,
                    published=True
                ),
                Parameter(
                    id=22,
                    xsl_name='numeric_param2',
                    value_type=NUMERIC,
                    published=True
                ),
                Parameter(
                    id=31,
                    xsl_name='enum_param1',
                    value_type=ENUM,
                    published=True
                ),
                Parameter(
                    id=32,
                    xsl_name='enum_param2',
                    value_type=ENUM,
                    published=True
                ),
            ]
        )
    ]


@pytest.fixture(scope='module')
def ungrouped_models():
    def generate_model(id):
        return ExportReportModel(
            id=id,
            category_id=CATEGORY_ID,
            blue_ungrouping_info=[
                UngroupingInfo(
                    title='Group1',
                    parameter_values=[
                        ParameterValue(
                            param_id=21,
                            numeric_value='0.1',
                        ),
                        ParameterValue(
                            param_id=31,
                            option_id=7,
                        ),
                    ]
                ),
                UngroupingInfo(
                    title='Group2',
                    parameter_values=[
                        ParameterValue(
                            param_id=21,
                            numeric_value='115.200',
                        ),
                        ParameterValue(
                            param_id=31,
                            option_id=7,
                        ),
                    ]
                ),
            ]
        )

    return [generate_model(MODEL_ID), generate_model(WHITE_MODEL_ID)]


@pytest.fixture(scope='module')
def genlog_rows():
    def create_offer(magic, ware, model, params, is_blue=True):
        return default_genlog(
            classifier_magic_id=magic,
            offer_id=ware,
            ware_md5=ware,
            category_id=CATEGORY_ID,
            model_id=model,
            is_blue_offer=is_blue,
            offer_params=[
                {'enriched_param_id': yt.yson.YsonUint64(param_id), 'num': value}
                if is_num_param(param_id) else
                {'enriched_param_id': yt.yson.YsonUint64(param_id), 'id': yt.yson.YsonUint64(value)}
                for (param_id, value) in params
            ]
        )

    offers = [
        create_offer(
            '000066153519254f804f33eda7868cbd',
            '000000000000000000000w',
            MODEL_ID,
            [(NUM_PARAM_1, 0.1), (ENUM_PARAM_1, 7)]),                              # Group1
        create_offer(
            '111166153519254f804f33eda7868cbd',
            '000000000000000000001w',
            MODEL_ID,
            [(NUM_PARAM_1, 115.2), (ENUM_PARAM_1, 7)]),                            # Group2
        create_offer(
            '222266153519254f804f33eda7868cbd',
            '000000000000000000002w',
            MODEL_ID,
            [(BOOL_PARAM_1, 100), (NUM_PARAM_1, 115.2), (ENUM_PARAM_1, 7)]),       # Group1. Bool parameter was ignored

        create_offer(
            '333366153519254f804f33eda7868cbd',
            '000000000000000000003w',
            MODEL_ID,
            [(NUM_PARAM_2, 100.0), (NUM_PARAM_1, 115.2), (ENUM_PARAM_1, 7)]),        # Group1. Other parameter was ignored
        create_offer(
            '444466153519254f804f33eda7868cbd',
            '000000000000000000004w',
            MODEL_ID,
            [(ENUM_PARAM_2, 100), (NUM_PARAM_1, 115.2), (ENUM_PARAM_1, 7)]),        # Group1. Other parameter was ignored

        create_offer(
            '555566153519254f804f33eda7868cbd',
            '000000000000000000005w',
            MODEL_ID,
            [(NUM_PARAM_1, 0.1), (ENUM_PARAM_1, 8)]),       # Without group. Wrong value
        create_offer(
            '666666153519254f804f33eda7868cbd',
            '000000000000000000006w',
            MODEL_ID,
            [(NUM_PARAM_1, 0.0999), (ENUM_PARAM_1, 7)]),    # Without group. Wrong value
        create_offer(
            '777766153519254f804f33eda7868cbd',
            '000000000000000000007w',
            MODEL_ID,
            [(NUM_PARAM_1, 0.1)]),                          # Without group. Missed value
        create_offer(
            '888866153519254f804f33eda7868cbd',
            '000000000000000000008w',
            MODEL_ID,
            [(ENUM_PARAM_1, 7)]),                           # Without group. Missed value

        create_offer(
            '999966153519254f804f33eda7868cbd',
            '000000000000000000009w',
            WHITE_MODEL_ID + 1,
            [(NUM_PARAM_1, 0.1), (ENUM_PARAM_1, 7)]),       # Offer with hidden model
        create_offer(
            'aaaa66153519254f804f33eda7868cbd',
            '00000000000000000000Aw',
            WHITE_MODEL_ID,
            [(NUM_PARAM_1, 0.1), (ENUM_PARAM_1, 7)],
            False),                                         # White offer
    ]

    offers.append(default_genlog(
        offer_id='00000000000000000000Bw',
        ware_md5='00000000000000000000Bw',
        model_id=0,
        cluster_id=0,
        is_blue_offer=False,
    ))
    return offers


@pytest.fixture(scope='module')
def model_ids():
    return ModelIds([WHITE_MODEL_ID], [MODEL_ID])


@pytest.fixture(scope='module')
def gl_mbo_pb(gl_mbo):
    return GlMboPb(gl_mbo)


@pytest.fixture(scope='module')
def ungrouping_model_params(ungrouped_models):
    return UngroupingModelParams(
        ungrouped_models
    )


@pytest.fixture(scope='module')
def ungrouping_models(ungrouped_models):
    return UngroupingModels(
        ungrouped_models,
        range(1000, 2000)
    )


@pytest.fixture(scope="module")
def genlog_table(yt_server, genlog_rows):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0000'), genlog_rows)
    genlog_table.dump()
    return genlog_table


@pytest.yield_fixture(scope='module')
def offers_processor_workflow(
        yt_server,
        gl_mbo_pb,
        ungrouping_model_params,
        ungrouping_models,
        model_ids,
        genlog_table):
    input_table_paths = [genlog_table.get_path()]
    resources = {
        'gl_mbo_pbuf_sn': gl_mbo_pb,
        'ungrouping_model_params_gz': ungrouping_model_params,
        'ungrouping_models_gz': ungrouping_models,
        'model_ids': model_ids,
    }

    with OffersProcessorTestEnv(
            yt_server,
            use_genlog_scheme=True,
            input_table_paths=input_table_paths,
            **resources
    ) as env:
        env.execute()
        yield env


@pytest.yield_fixture(scope='module')
def genlog_snippet_workflow(
        yt_server,
        offers_processor_workflow,
        ungrouping_model_params,
        ungrouping_models,
        model_ids
):
    genlogs = []
    for id, glProto in enumerate(offers_processor_workflow.genlog_dicts):
        genlogs.append(glProto)

    resources = {
        'ungrouping_model_params_gz': ungrouping_model_params,
        'ungrouping_models_gz': ungrouping_models,
        'model_ids': model_ids,
    }
    with SnippetDiffBuilderTestEnv(
        'genlog_snippet_workflow',
        yt_server,
        offers=[],
        genlogs=genlogs,
        models=[],
        state=[],
        **resources
    ) as env:
        env.execute()
        env.verify()
        yield env


@pytest.yield_fixture(scope="module")
def mr_mindexer_direct_arc(yt_server, offers_processor_workflow):
    with MrMindexerBuildTestEnv() as build_env:
        build_env.execute_from_offers_list(yt_server, offers_processor_workflow.genlog_dicts)
        build_env.verify()

        resourses = {
            'merge_options': MrMindexerMergeOptions(
                input_portions_path=build_env.yt_index_portions_path,
                part=0,
                index_type=MrMindexerMergeIndexType.DIRECT_ARCH,
            ),
        }

        with MrMindexerMergeTestEnv(**resourses) as env:
            env.execute(yt_server)
            env.verify()
            env.outputs['indexarc'].load()
            yield env


@pytest.yield_fixture(scope="module")
def arc_workflow(mr_mindexer_direct_arc):
    resources = {
        'indexarc': mr_mindexer_direct_arc.outputs['indexarc']
    }
    with IndexarcTestEnv(**resources) as env:
        env.execute()
        env.verify()
        yield env


def test_ungrouping_all(arc_workflow, genlog_snippet_workflow):
    expected_values = [
        {
            'offer_id': '000000000000000000000w',
            'ungrouped_hyper_blue': None,
            'ungrouped_model_key_blue': '0.1_7',
            'ungrouped_model_title_blue': 'Group1',
        },
        {
            'offer_id': '000000000000000000001w',
            'ungrouped_hyper_blue': None,
            'ungrouped_model_key_blue': '115.2_7',
            'ungrouped_model_title_blue': 'Group2',
        },
        {
            'offer_id': '000000000000000000002w',
            'ungrouped_hyper_blue': None,
            'ungrouped_model_key_blue': '115.2_7',
            'ungrouped_model_title_blue': 'Group2',
        },
        {
            'offer_id': '000000000000000000003w',
            'ungrouped_hyper_blue': None,
            'ungrouped_model_key_blue': '115.2_7',
            'ungrouped_model_title_blue': 'Group2',
        },
        {
            'offer_id': '000000000000000000004w',
            'ungrouped_hyper_blue': None,
            'ungrouped_model_key_blue': '115.2_7',
            'ungrouped_model_title_blue': 'Group2',
        },
        {
            'offer_id': '000000000000000000005w',
            'ungrouped_hyper_blue': None,
            'ungrouped_model_key_blue': None,
            'ungrouped_model_title_blue': None,
        },
        {
            'offer_id': '000000000000000000006w',
            'ungrouped_hyper_blue': None,
            'ungrouped_model_key_blue': None,
            'ungrouped_model_title_blue': None,
        },
        {
            'offer_id': '000000000000000000007w',
            'ungrouped_hyper_blue': None,
            'ungrouped_model_key_blue': None,
            'ungrouped_model_title_blue': None,
        },
        {
            'offer_id': '000000000000000000008w',
            'ungrouped_hyper_blue': None,
            'ungrouped_model_key_blue': None,
            'ungrouped_model_title_blue': None,
        },
        {
            'offer_id': '000000000000000000009w',
            'ungrouped_hyper_blue': None,
            'ungrouped_model_key_blue': None,
            'ungrouped_model_title_blue': None,
        },
        {
            'offer_id': '00000000000000000000Aw',
            'ungrouped_hyper_blue': None,
            'ungrouped_model_key_blue': '0.1_7',
            'ungrouped_model_title_blue': 'Group1',
        },
        {
            'offer_id': '00000000000000000000Bw',
            'ungrouped_hyper_blue': None,
            'ungrouped_model_key_blue': None,
            'ungrouped_model_title_blue': None,
        },
    ]

    for expected in expected_values:
        assert_that(
            arc_workflow,
            HasArchiveEntry(expected)
        )
        assert_that(
            genlog_snippet_workflow,
            HasOutputStateRecord({'value': expected})
        )
