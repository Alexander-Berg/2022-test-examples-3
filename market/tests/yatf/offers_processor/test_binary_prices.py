# coding: utf-8

import pytest
from hamcrest import assert_that

from market.idx.pylibrary.offer_flags.flags import (
    OfferFlags,
)
from market.idx.offers.yatf.utils.fixtures import default_genlog
from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.offers.yatf.matchers.offers_processor.env_matchers import \
    HasGenlogRecordRecursive

from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join
import yt.wrapper as yt


@pytest.fixture(
    params=[
        {
            'offers': [default_genlog(
                binary_price={
                    'price': yt.yson.YsonUint64(25138350),
                    'rate': 'CBRF',
                    'plus': 0.0,
                    'id': 'USD',
                    'ref_id': 'USD',
                },
                binary_oldprice={
                    'price': yt.yson.YsonUint64(33517800),
                    'rate': 'CBRF',
                    'plus': 0.0,
                    'id': 'USD',
                    'ref_id': 'USD',
                },
                binary_unverified_oldprice={
                    'price': yt.yson.YsonUint64(33517800),
                    'rate': 'CBRF',
                    'plus': 0.0,
                    'id': 'USD',
                    'ref_id': 'USD',
                },
                flags=OfferFlags.PICKUP | OfferFlags.STORE | OfferFlags.AVAILABLE,
            )],
            'expected': {
                'price': {
                    'price': 25138350,
                    'rate': 'CBRF',
                    'plus': 0.0,
                    'id': 'USD',
                    'ref_id': 'USD'
                },
                'oldprice': {
                    'price': 33517800,
                    'id': 'USD',
                    'ref_id': 'USD'
                }
            },
        },
    ],
    scope="module"
)
def cp_workflow_params(request):
    return request.param


@pytest.yield_fixture(scope="module")
def workflow(yt_server, cp_workflow_params):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0000'), cp_workflow_params['offers'])
    genlog_table.dump()
    input_table_paths = [genlog_table.get_path()]

    with OffersProcessorTestEnv(
            yt_server,
            use_genlog_scheme=True,
            input_table_paths=input_table_paths
    ) as env:
        env.execute()
        yield env


def test_binary_price(workflow, cp_workflow_params):
    """ Цена, используемая индексатором, рассчитывается на базе поля binary_price,
        если оно присутствует, а не берётся из поля price_expression.

        Старая цена, используемая индексатором, рассчитывается на базе поля binary_oldprice,
        если оно присутствует, а не берётся из поля oldprice_expression.
    """
    assert_that(workflow,
                HasGenlogRecordRecursive(
                    {
                        'binary_price': cp_workflow_params['expected']['price'],
                        'binary_unverified_oldprice': cp_workflow_params['expected']['oldprice'],
                    }
                ),
                u'GenerationLog contains market_sku field')
