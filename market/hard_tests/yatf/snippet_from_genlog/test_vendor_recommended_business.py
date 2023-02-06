# coding=utf-8

from hamcrest import (
    assert_that,
)
import pytest

from market.idx.generation.yatf.matchers.snippet_diff_builder.env_matchers import HasOutputStateRecord
from market.idx.generation.yatf.test_envs.snippet_diff_builder import SnippetDiffBuilderTestEnv

from market.idx.offers.yatf.resources.offers_indexer.vendor_recommended_business import VendorRecommendedBusiness
from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.offers.yatf.utils.fixtures import default_genlog, default_shops_dat

from market.idx.yatf.resources.shops_dat import ShopsDat
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join


test_data = [
    {
        # found in vendor_recommended_business.csv
        'offer_id': '1',
        'shop_id': 1,
        'business_id': 1,
        'datafeed_id': 1,
        'vendor_id': 1,
        'vendor_ds_id': 10,
        'is_recommended_by_vendor': True,
        'is_official_by_vendor': True,
    },
    {
        # found in vendor_recommended_business.csv
        'offer_id': '2',
        'shop_id': 2,
        'business_id': 2,
        'datafeed_id': 2,
        'vendor_id': 2,
        'vendor_ds_id': 20,
        'is_recommended_by_vendor': False,
        'is_official_by_vendor': False,
    },
    {
        # not found in vendor_recommended_business.csv
        'offer_id': '3',
        'shop_id': 3,
        'business_id': 3,
        'datafeed_id': 3,
        'vendor_id': 3,
        'vendor_ds_id': 0,
        'is_recommended_by_vendor': False,
        'is_official_by_vendor': False,
    },
    {
        # found in vendor_recommended_business.csv
        'offer_id': '4',
        'shop_id': 4,
        'business_id': 4,
        'datafeed_id': 4,
        'vendor_id': 4,
        'vendor_ds_id': 21,
        'is_recommended_by_vendor': True,
        'is_official_by_vendor': False,
    },
]


@pytest.fixture(scope="module")
def vendor_recommended_business():
    vendor_recommended_business = VendorRecommendedBusiness()
    # vendor_id, vendor_ds_id, business_id, is_recommended, is_official
    vendor_recommended_business.add(1, 10, 1, True, True)
    vendor_recommended_business.add(2, 20, 2, False, False)
    vendor_recommended_business.add(4, 21, 4, True, False)
    return vendor_recommended_business


@pytest.fixture(scope="module")
def genlog_rows():
    offers = []
    for data in test_data:
        offer = default_genlog(
            feed_id=data['datafeed_id'],
            shop_id=data['shop_id'],
            offer_id=data['offer_id'],
            vendor_id=data['vendor_id'],
            business_id=data['business_id']
        )
        offers.append(offer)
    return offers


@pytest.fixture(scope="module")
def genlog_table(yt_server, genlog_rows):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0000'), genlog_rows)
    genlog_table.dump()
    return genlog_table


@pytest.fixture(scope="module")
def shopsdat():
    shops = []
    for x in test_data:
        shop = default_shops_dat(
            business_id=str(x['business_id']),
            shop_id=str(x['shop_id']),
            datafeed_id=str(x['datafeed_id']),
        )
        shops.append(shop)
    return ShopsDat(shops=shops)


@pytest.yield_fixture(scope='module')
def offers_processor_workflow(yt_server, shopsdat, vendor_recommended_business, genlog_table):
    input_table_paths = [genlog_table.get_path()]
    resources = {
        'vendor_recommended_business_csv': vendor_recommended_business,
        'shops_dat': shopsdat
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
def genlog_snippet_workflow(yt_server, offers_processor_workflow):
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
    ) as env:
        env.execute()
        env.verify()
        yield env


@pytest.yield_fixture(scope="module")
def expected_vendor_recommended_business():
    return [
        {
            'offer_id': x['offer_id'],
            'vendor_recommended_business_datasource': str(x['vendor_ds_id']),
        }
        for x in test_data
    ]


def test_vendor_recommended_business_snippet(genlog_snippet_workflow, expected_vendor_recommended_business):
    for expected in expected_vendor_recommended_business:
        assert_that(
            genlog_snippet_workflow,
            HasOutputStateRecord({'value': expected})
        )
