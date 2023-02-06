import pytest
from hamcrest import (
    assert_that,
    equal_to,
    matches_regexp
)


from market.idx.offers.yatf.matchers.offers_processor.env_matchers import HasGenlogRecord


from market.idx.library.glue.proto.GlueConfig_pb2 import EReduceKeySchema

from market.idx.offers.yatf.resources.offers_glue.glue_tables import FullGlueTable

from market.idx.offers.yatf.test_envs.offers_glue import OffersGlueTestEnv
from market.idx.offers.yatf.resources.offers_indexer.ytfeed import YtFeed
from market.idx.offers.yatf.utils.fixtures import default_offer

from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from market.idx.yatf.resources.glue_config import GlueConfig


import yt.wrapper as yt
from yt.wrapper.ypath import ypath_join


@pytest.fixture(scope="module")
def offers(request):
    return [
        default_offer(
            business_id=1, feed_id=1, yx_shop_offer_id='offer_1', yx_ds_id=1, warehouse_id=15
        ),
        default_offer(
            business_id=1, feed_id=1, yx_shop_offer_id='offer_1', yx_ds_id=2, warehouse_id=16
        ),
        default_offer(
            business_id=1, feed_id=1, yx_shop_offer_id='offer_1', yx_ds_id=1, warehouse_id=17
        ),
        default_offer(
            business_id=2, feed_id=1, yx_shop_offer_id='offer_2', yx_ds_id=10, warehouse_id=14
        ),
        default_offer(
            business_id=3, feed_id=1, yx_shop_offer_id='offer_3', yx_ds_id=10, warehouse_id=15
        ),
        default_offer(
            business_id=5, feed_id=1, yx_shop_offer_id='offer_3', yx_ds_id=6, warehouse_id=12
        ),
    ]


@pytest.fixture(scope="module")
def full_extern_glues():
    return {
        'full_first': [
            {'business_id': 1, 'offer_id': 'offer_1', 'shop_id': 2, 'warehouse_id': 16, 'glue_fields': [{'glue_id': yt.yson.YsonUint64(4), 'float_value': yt.yson.YsonDouble(1.0)}]},
            {'business_id': 1, 'offer_id': 'offer_1', 'shop_id': 1, 'warehouse_id': 17, 'glue_fields': [{'glue_id': yt.yson.YsonUint64(4), 'float_value': yt.yson.YsonDouble(2.0)}]},
            {'business_id': 1, 'offer_id': 'offer_1', 'shop_id': 2, 'warehouse_id': 100500, 'glue_fields': [{'glue_id': yt.yson.YsonUint64(4), 'float_value': yt.yson.YsonDouble(3.0)}]},
            {'business_id': 5, 'offer_id': 'offer_3', 'shop_id': 6, 'warehouse_id': 12, 'glue_fields': [{'glue_id': yt.yson.YsonUint64(4), 'float_value': yt.yson.YsonDouble(4.0)}]},
        ],
        'named_genlog_field': [
            {'business_id': 1, 'offer_id': 'offer_1', 'shop_id': 1, 'warehouse_id': 15, 'glue_fields': [{'glue_id': yt.yson.YsonUint64(122), 'string_value': "offer_1"}]},
            {'business_id': 1, 'offer_id': 'offer_1', 'shop_id': 1, 'warehouse_id': 17, 'glue_fields': [{'glue_id': yt.yson.YsonUint64(122), 'string_value': "offer_1"}]},
            {'business_id': 2, 'offer_id': 'offer_2', 'shop_id': 10, 'warehouse_id': 14, 'glue_fields': [{'glue_id': yt.yson.YsonUint64(122), 'string_value': "offer_2"}]},
            {'business_id': 3, 'offer_id': 'offer_3', 'shop_id': 10, 'warehouse_id': 15, 'glue_fields': [{'glue_id': yt.yson.YsonUint64(122), 'string_value': "offer_3"}]},
        ]
    }


@pytest.fixture(scope='module')
def glue_config():
    return GlueConfig(
        {'Fields': [
            {
            'glue_id': 122,
            'declared_cpp_type': 'STRING',
            'target_name': 'hard2_dssm_embedding_str',
            'is_from_datacamp': False,
            'owner': 'marketindexer',
            'source_name': 'ext5',
            'data_limit_per_table': 777,
            'data_limit_per_offer': 888,
            'destination_table_path': 'converted_table5',
            'source_table_path': 'external_table5',
            'source_field_path': 'column_a',
            'reduce_key_schema': EReduceKeySchema.FULL_OFFER_ID,
            'use_as_genlog_field' : True
            },
        ]},
        'glue_config.json'
    )


@pytest.yield_fixture(scope="module")
def workflow(yt_server, offers, glue_config, full_extern_glues):
    resources = {
        'genlogs': YtFeed.from_list(
            yt_server.get_yt_client(),
            offers,
            custom_table_path=ypath_join(get_yt_prefix(), 'in', '0000')
        ),
        'glue_config': glue_config
    }

    resources.update({
        name: FullGlueTable(
            yt_stuff=yt_server,
            path=ypath_join(get_yt_prefix(), 'externdata', name),
            data=offers,
        ) for name, offers in full_extern_glues.iteritems()
    })

    output_table_path = ypath_join(get_yt_prefix(), 'out', '0000')

    with OffersGlueTestEnv(
        yt_server,
        output_genlog_yt_path=output_table_path,
        input_glue_yt_paths=[resources[name].get_path() for name in resources if name != 'genlogs' and name != 'glue_config'],
        **resources
    ) as env:
        env.execute()
        env.verify()
        yield env


def test_start(workflow, offers):
    assert_that(len(workflow.genlog), equal_to(len(offers)))


def test_genlog_convertion(workflow, offers):
    for offer in offers:
        assert_that(
            workflow,
            HasGenlogRecord({
                'business_id': offer['business_id'],
                'offer_id': offer['yx_shop_offer_id'],
                'shop_id': offer['yx_ds_id'],
                'warehouse_id': offer['warehouse_id']
            })
        )


def test_named_fields(workflow):
    expected_offers = [
        {'business_id': 1, 'offer_id': 'offer_1', 'shop_id': 1, 'warehouse_id': 15, 'hard2_dssm_embedding_str': 'offer_1'},
        {'business_id': 1, 'offer_id': 'offer_1', 'shop_id': 1, 'warehouse_id': 17, 'hard2_dssm_embedding_str': 'offer_1'},
        {'business_id': 2, 'offer_id': 'offer_2', 'shop_id': 10, 'warehouse_id': 14, 'hard2_dssm_embedding_str': 'offer_2'},
        {'business_id': 3, 'offer_id': 'offer_3', 'shop_id': 10, 'warehouse_id': 15, 'hard2_dssm_embedding_str': 'offer_3'},
    ]

    for offer in expected_offers:
        assert_that(
            workflow,
            HasGenlogRecord(
                {
                'business_id': offer['business_id'],
                'offer_id': offer['offer_id'],
                'shop_id': offer['shop_id'],
                'warehouse_id': offer['warehouse_id'],
                'hard2_dssm_embedding_str': offer['hard2_dssm_embedding_str'],
                }
            )
        )


def test_output_table_names(workflow):
    for table_path in workflow.output_tables_list:
        assert_that(
            table_path,
            matches_regexp("^[0-9]{4}")
        )
