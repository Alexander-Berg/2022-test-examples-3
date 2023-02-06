# coding=utf-8

from hamcrest import (
    assert_that,
)
import pytest

from market.idx.generation.yatf.matchers.snippet_diff_builder.env_matchers import HasOutputStateRecord
from market.idx.generation.yatf.test_envs.snippet_diff_builder import SnippetDiffBuilderTestEnv

from market.idx.offers.yatf.resources.offers_indexer.ytfeed import YtFeed
from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.offers.yatf.utils.fixtures import default_offer

from market.idx.yatf.resources.glue_config import GlueConfig


# NMarket.Glue.FieldValue oneof value
# 'bool_value',
# 'double_value',
# 'float_value',
# 'int32_value',
# 'int64_value',
# 'uint32_value',
# 'uint64_value',
# 'string_value'

test_data = [
    {
        'offer_id': '1',
        'glue_fields': [
            {
                'glue_id': 1,
                'int32_value': 123,
            },
        ],
        'expected': '{}',  # not used as snippet by glue config
    },
    {
        'offer_id': '2',
        'glue_fields': [
            {
                'glue_id': 2,
                'bool_value': True,
            },
        ],
        'expected': '{"b":true}',
    },
    {
        'offer_id': '3',
        'glue_fields': [
            {
                'glue_id': 3,
                'double_value': 1.0001,
            },
        ],
        'expected': '{"c":"1.0001"}',
    },
    {
        'offer_id': '4',
        'glue_fields': [
            {
                'glue_id': 4,
                'float_value': 12.0001,
            },
        ],
        'expected': '{"d":"12.0001"}',
    },
    {
        'offer_id': '5',
        'glue_fields': [
            {
                'glue_id': 5,
                'int32_value': 2147483647,
            },
        ],
        'expected': '{"e":"2147483647"}',
    },
    {
        'offer_id': '6',
        'glue_fields': [
            {
                'glue_id': 6,
                'int64_value': 9223372036854775807,
            },
        ],
        'expected': '{"f":"9223372036854775807"}',
    },
    {
        'offer_id': '7',
        'glue_fields': [
            {
                'glue_id': 7,
                'uint32_value': 4294967295,
            },
        ],
        'expected': '{"g":"4294967295"}',
    },
    {
        'offer_id': '8',
        'glue_fields': [
            {
                'glue_id': 8,
                'uint64_value': 18446744073709551615,
            },
        ],
        'expected': '{"h":"18446744073709551615"}',
    },
    {
        'offer_id': '1234567',
        'glue_fields': [
            {
                'glue_id': 1234567,  # unkown by glue config glue_id
                'bool_value': False,
            },
        ],
        'expected': '{}',
    },
    {
        'offer_id': '9',
        'glue_fields': [
            {
                'glue_id': 9,
                'string_value': 'hello bzz13!',
            }
        ],
        'expected': '{"i":"hello bzz13!"}',
    },
    {
        'offer_id': '10',
        'glue_fields': [
            {
                'glue_id': 1,
                'int32_value': 123,
            },
            {
                'glue_id': 2,
                'bool_value': False,
            },
            {
                'glue_id': 3,
                'double_value': 1.0001,
            },
            {
                'glue_id': 4,
                'float_value': 12.0001,
            },
            {
                'glue_id': 5,
                'int32_value': 2147483647,
            },
            {
                'glue_id': 6,
                'int64_value': 9223372036854775807,
            },
            {
                'glue_id': 7,
                'uint32_value': 4294967295,
            },
            {
                'glue_id': 8,
                'uint64_value': 18446744073709551615,
            },
            {
                'glue_id': 9,
                'string_value': 'glue rules'
            },
        ],
        'expected': (
            '{'
            '"b":false,'
            '"c":"1.0001",'
            '"d":"12.0001",'
            '"e":"2147483647",'
            '"f":"9223372036854775807",'
            '"g":"4294967295",'
            '"h":"18446744073709551615",'
            '"i":"glue rules"'
            '}'
        ),
    },
]


@pytest.fixture(scope="module")
def feed():
    feed = []
    for x in test_data:
        offer = default_offer(
            yx_shop_offer_id=x['offer_id'],
            glue_fields=x['glue_fields'],
        )
        feed.append(offer)

    return feed


@pytest.fixture(scope='module')
def glue_config():
    return GlueConfig(
        {
            'Fields': [
                {
                    'glue_id': 1,
                    'declared_cpp_type': 'UINT32',
                    'target_name': 'a',
                    'is_from_datacamp': True,
                    'source_field_path': 'unused',
                    'use_as_snippet': False,
                },
                {
                    'glue_id': 2,
                    'declared_cpp_type': 'BOOL',
                    'target_name': 'b',
                    'is_from_datacamp': True,
                    'source_field_path': 'unused',
                    'use_as_search_literal': True,
                    'use_as_snippet': True
                },
                {
                    'glue_id': 3,
                    'declared_cpp_type': 'DOUBLE',
                    'target_name': 'c',
                    'is_from_datacamp': True,
                    'source_field_path': 'unused',
                    'use_as_snippet': True,
                },
                {
                    'glue_id': 4,
                    'declared_cpp_type': 'FLOAT',
                    'target_name': 'd',
                    'is_from_datacamp': True,
                    'source_field_path': 'unused',
                    'use_as_snippet': True,
                },
                {
                    'glue_id': 5,
                    'declared_cpp_type': 'INT32',
                    'target_name': 'e',
                    'is_from_datacamp': True,
                    'source_field_path': 'unused',
                    'use_as_snippet': True,
                },
                {
                    'glue_id': 6,
                    'declared_cpp_type': 'INT64',
                    'target_name': 'f',
                    'is_from_datacamp': True,
                    'source_field_path': 'unused',
                    'use_as_search_literal': True,
                    'use_as_snippet': True,
                },
                {
                    'glue_id': 7,
                    'declared_cpp_type': 'UINT32',
                    'target_name': 'g',
                    'is_from_datacamp': True,
                    'source_field_path': 'unused',
                    'use_as_snippet': True,
                },
                {
                    'glue_id': 8,
                    'declared_cpp_type': 'UINT64',
                    'target_name': 'h',
                    'is_from_datacamp': True,
                    'source_field_path': 'unused',
                    'use_as_snippet': True,
                },
                {
                    'glue_id': 9,
                    'declared_cpp_type': 'STRING',
                    'target_name': 'i',
                    'is_from_datacamp': True,
                    'source_field_path': 'unused',
                    'use_as_snippet': True,
                },
            ],
        },
        'glue_config.json'
    )


@pytest.yield_fixture(scope='module')
def offers_processor_workflow(yt_server, feed):
    yt = yt_server.get_yt_client()
    resources = {
        'feed': YtFeed.from_list(yt, feed),
    }

    with OffersProcessorTestEnv(yt_server, **resources) as env:
        env.execute()
        yield env


@pytest.yield_fixture(scope='module')
def genlog_snippet_workflow(yt_server, offers_processor_workflow, glue_config):
    genlogs = []
    for id, glProto in enumerate(offers_processor_workflow.genlog_dicts):
        genlogs.append(glProto)

    with SnippetDiffBuilderTestEnv(
        'genlog_snippet_workflow',
        yt_server,
        offers=[],
        genlogs=genlogs,
        models=[],
        state=[],
        glue_config_path=glue_config.path
    ) as env:
        env.execute()
        env.verify()
        yield env


def test_glue_snippets(genlog_snippet_workflow):
    expected_ages = [
        {
            'offer_id': x['offer_id'],
            'glue_external_data': x['expected'],
        }
        for x in test_data
    ]
    for expected in expected_ages:
        assert_that(
            genlog_snippet_workflow,
            HasOutputStateRecord({'value': expected})
        )
