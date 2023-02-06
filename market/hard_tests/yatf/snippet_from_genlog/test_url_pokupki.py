# coding=utf-8

from hamcrest import (
    assert_that,
)
import pytest

from market.idx.generation.yatf.matchers.snippet_diff_builder.env_matchers import (
    HasOutputStateRecord,
)

from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.generation.yatf.test_envs.snippet_diff_builder import SnippetDiffBuilderTestEnv


from market.idx.offers.yatf.utils.fixtures import (
    default_genlog,
    genererate_default_pictures,
)
from market.idx.pylibrary.offer_flags.flags import OfferFlags
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join
import yt.wrapper as yt


test_data = [
    # white offers
    {
        'offer_id': '3',
        'url': 'https://3.html',
        'feed_group_id': 'group id NO_VALUE',
        'ware_md5': 'cccccccccccccccccccccc',
        'expected_url': 'https://3.html',
    },
    {
        'offer_id': '30',
        'url': 'https://30.html',
        'ware_md5': 'cccccccccccccccccccccc',
        'expected_url': 'https://30.html',
    },

    # blue offers
    {
        'offer_id': '4',
        'is_blue_offer': True,
        'flags': OfferFlags.IS_FULFILLMENT,
        'ware_md5': 'dddddddddddddddddddddd',
        'expected_url': (
            'https://pokupki.market.yandex.ru/product/0?offerid=dddddddddddddddddddddd'
        ),
    },
    {
        'offer_id': '40',
        'is_blue_offer': True,
        'flags': OfferFlags.IS_FULFILLMENT,
        'model_title': 'новый иphone M Pro',
        'ware_md5': 'dddddddddddddddddddddd',
        'expected_url': (
            'https://pokupki.market.yandex.ru/product--novyi-iphone-m-pro/0?'
            'offerid=dddddddddddddddddddddd'
        ),
    },
    {
        'offer_id': '400',
        'market_sku': 100500,
        'is_blue_offer': True,
        'flags': OfferFlags.IS_FULFILLMENT,
        'model_title': 'новый иphone M Pro',
        'ware_md5': 'dddddddddddddddddddddd',
        'expected_url': (
            'https://pokupki.market.yandex.ru/product--novyi-iphone-m-pro/100500?'
            'offerid=dddddddddddddddddddddd'
        ),
    },
    {
        'offer_id': '5',
        'is_blue_offer': True,
        'is_fake_msku_offer': True,
        'ware_md5': 'eeeeeeeeeeeeeeeeeeeeee',
        'expected_url': (
            'https://pokupki.market.yandex.ru/product/0?'
            'offerid=eeeeeeeeeeeeeeeeeeeeee'
        ),
    },
    {
        'offer_id': '50',
        'is_blue_offer': True,
        'is_fake_msku_offer': True,
        'model_title': 'бубушкофон 3000',
        'ware_md5': 'eeeeeeeeeeeeeeeeeeeeee',
        'expected_url': (
            'https://pokupki.market.yandex.ru/product--bubushkofon-3000/0?'
            'offerid=eeeeeeeeeeeeeeeeeeeeee'
        ),
    },
    {
        'offer_id': '500',
        'market_sku': 500100,
        'is_blue_offer': True,
        'is_fake_msku_offer': True,
        'model_title': 'бубушкофон 3000',
        'ware_md5': 'eeeeeeeeeeeeeeeeeeeeee',
        'expected_url': (
            'https://pokupki.market.yandex.ru/product--bubushkofon-3000/500100?'
            'offerid=eeeeeeeeeeeeeeeeeeeeee'
        ),
    },
    {
        'offer_id': '510',
        'market_sku': 500200,
        'is_blue_offer': True,
        'is_fake_msku_offer': True,
        'model_title': 'бубушкофон 3000',
        'contex_info': {'experiment_id': 'exp', 'original_msku_id': yt.yson.YsonUint64(500100)},
        'ware_md5': 'eeeeeeeeeeeeeeeeeeeeee',
        'expected_url': (
            'https://pokupki.market.yandex.ru/product--bubushkofon-3000/500100?'
            'offerid=eeeeeeeeeeeeeeeeeeeeee'
        ),
    }
]


@pytest.yield_fixture(scope='module')
def genlog_rows():
    return [
        default_genlog(
            offer_id=data['offer_id'],
            ware_md5=data['ware_md5'],
            market_sku=data.get('market_sku', None),
            feed_group_id=data.get('feed_group_id', None),
            feed_group_id_hash=data.get('feed_group_id', None),
            is_blue_offer=data.get('is_blue_offer', False),
            is_fake_msku_offer=data.get('is_fake_msku_offer', False),
            flags=data.get('flags', None),
            contex_info=data.get('contex_info', None),
            url=data.get('url', None),
            model_title=data.get('model_title', None),
            pictures=genererate_default_pictures(),
        )
        for data in test_data
    ]


@pytest.fixture(scope="module")
def genlog_table(yt_server, genlog_rows):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0000'), genlog_rows)
    genlog_table.dump()
    return genlog_table


@pytest.yield_fixture(scope='module')
def offers_processor_workflow(yt_server, genlog_table):
    input_table_paths = [genlog_table.get_path()]

    with OffersProcessorTestEnv(
            yt_server,
            use_genlog_scheme=True,
            input_table_paths=input_table_paths
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
        use_pokupki_domain=True,
    ) as env:
        env.execute()
        env.verify()
        yield env


@pytest.yield_fixture(scope='module')
def expected_url_snippet():
    return [
        {
            'offer_id': x['offer_id'],
            '_Url': x.get('expected_url', None),
        }
        for x in test_data
    ]


def test_url_snippet(genlog_snippet_workflow, expected_url_snippet):
    for expected in expected_url_snippet:
        assert_that(
            genlog_snippet_workflow,
            HasOutputStateRecord({'value': expected})
        )
