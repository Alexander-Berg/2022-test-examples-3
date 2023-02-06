# coding: utf-8
import pytest
import time

from hamcrest import assert_that

from market.idx.offers.yatf.matchers.offers_processor.env_matchers import HasGenlogRecordRecursive
from market.idx.offers.yatf.resources.offers_processor.small_bids_table import SmallBidsTable
from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.offers.yatf.utils.fixtures import (
    default_genlog,
    default_shops_dat,
)

from market.mbi.mbi.proto.bidding.MbiBids_pb2 import Bid, Parcel

from market.idx.yatf.resources.cpa_category_xml import CpaCategories, CpaCategoryData
from market.idx.yatf.resources.shops_dat import ShopsDat
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join


@pytest.fixture(
    params=[
        {
            'offers_data': [
                {
                    'feed_id': 540061,
                    'offer_id': 'nonred_one_zero_fee',
                    'shop_id': 540390,
                    'cpa': 4,
                },
            ],
            'shops_data': {
                'datafeed_id': '540061',
                'business_id': '540390',
                'shop_id': '540390',
                'cpa': 'REAL',
                'shop_fee': '0',
            },
            'feed_data': {
                'id': '540061',
                'finished_session': '20170122_2316',
                'published_session': '20170122_2316'
            },
            'expected_fee': 1,
            'expected_business_id': 540390,
        },
    ],
    ids=[
        "NOT_RED"
    ],
    scope="module"
)
def workflow_params(request):
    return request.param


def create_bids(feed_id, offer_id):
    bids = Parcel(
        bids=[
            Bid(
                partner_id=1,
                domain_type='FEED_OFFER_ID',
                domain_id=offer_id,
                feed_id=feed_id,
                value_for_search=Bid.Value(
                    value=34,
                    modification_time=int(time.time())
                ),
                value_for_card=Bid.Value(
                    value=35,
                    modification_time=int(time.time())
                ),
                value_for_market_search_only=Bid.Value(
                    value=37,
                    modification_time=int(time.time())
                ),
                partner_type='SHOP',
                domain_ids=[str(feed_id), offer_id],
            )
        ]
    )

    return bids.SerializeToString()


@pytest.fixture(scope="module")
def genlog_rows(workflow_params):
    offers = []
    for data in workflow_params['offers_data']:
        offer = default_genlog(**data)
        offer['bids'] = create_bids(offer['feed_id'], offer['offer_id'])
        offer['business_id'] = 540390
        offers.append(offer)
    return offers


@pytest.fixture(scope="module")
def genlog_table(yt_server, genlog_rows):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0000'), genlog_rows)
    genlog_table.dump()
    return genlog_table


@pytest.yield_fixture(scope="module")
def workflow(workflow_params, yt_server, genlog_table):
    input_table_paths = [genlog_table.get_path()]

    shopsdat = ShopsDat(
        shops=[
            default_shops_dat(**workflow_params['shops_data'])
        ]
    )

    category_data = [
        {
            "hyper_id": "90401",
            "fee": "1",
            "regions": "225",
            "cpa-type": "cpc_and_cpa"
        }
    ]

    yt_client = yt_server.get_yt_client()
    resources = {
        'cpa_categories_xml': CpaCategories(CpaCategoryData(category_data)),
        'shops_dat': shopsdat,
        'small_bids_table': SmallBidsTable(yt_client, []),
    }

    with OffersProcessorTestEnv(
            yt_server,
            use_bids=True,
            use_genlog_scheme=True,
            input_table_paths=input_table_paths,
            **resources
    ) as env:
        env.execute()
        env.verify()
        yield env


def test_binary_price(workflow, workflow_params):
    assert_that(
        workflow,
        HasGenlogRecordRecursive(
            {
                'fee': workflow_params['expected_fee'],
            }
        )
    )


def test_business_id(workflow, workflow_params):
    assert_that(
        workflow,
        HasGenlogRecordRecursive(
            {
                'business_id': workflow_params['expected_business_id']
            }
        )
    )
