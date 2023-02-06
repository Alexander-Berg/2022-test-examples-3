import pytest

from hamcrest import assert_that

from market.idx.offers.yatf.matchers.offers_processor.env_matchers import HasFeedlogRecord
from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.offers.yatf.utils.fixtures import default_genlog
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable


@pytest.fixture(scope="module")
def genlog_rows():
    offers = [
        default_genlog(),
        default_genlog(),
    ]
    return offers


@pytest.fixture(scope="module")
def genlog_table(yt_server, genlog_rows):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0000'), genlog_rows)
    genlog_table.dump()
    return genlog_table


@pytest.fixture(scope='module')
def dropped_offers_table(yt_server):
    yt = yt_server.get_yt_client()

    yt.create(
        'table',
        '//tmp/dropped_offers',
        recursive=True,
        attributes={
            'schema': [
                {'name': 'feed_id', 'type': 'uint64'},
                {'name': 'offer_id', 'type': 'string'},
            ],
            'strict': True,
        },
    )

    data = [
        {
            'feed_id': 101967,  # default feed id
            'offer_id': 'dropped-offer1'
        }
    ]
    yt.write_table('//tmp/dropped_offers', data)

    yield '//tmp/dropped_offers'

    yt.remove("//tmp/dropped_offers", force=True)


@pytest.yield_fixture(scope="module")
def workflow(yt_server, genlog_table, dropped_offers_table):
    input_table_paths = [genlog_table.get_path()]

    with OffersProcessorTestEnv(
        yt_server,
        dropped_offers_table_path=dropped_offers_table,
        use_genlog_scheme=True,
        input_table_paths=input_table_paths,
    ) as env:
        env.execute()
        yield env


def test_smart_index(workflow):
    """ Test that offers-processor counts per-feed number of offers excluded by the smart index """
    assert_that(
        workflow,
        HasFeedlogRecord({
            'feed_id': 101967,
            'indexation': {
                'statistics': {
                    'valid_offers': 2,
                    'smart_index_offers': 1
                }
            }
        }
    ))
