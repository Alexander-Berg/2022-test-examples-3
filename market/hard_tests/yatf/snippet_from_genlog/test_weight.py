# coding=utf-8

from hamcrest import assert_that
import pytest

from market.idx.generation.yatf.matchers.snippet_diff_builder.env_matchers import HasOutputStateRecord
from market.idx.generation.yatf.test_envs.snippet_diff_builder import SnippetDiffBuilderTestEnv

from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.offers.yatf.utils.fixtures import default_genlog
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join


test_data = [
    {
        'offer_id': '1',
        'weight': 500.0,
        'expected_weight': '500',
    },
    {
        'offer_id': '2',
        'expected_weight': None,
    },
    {
        'offer_id': '3',
        'weight': 3568.0,
        'expected_weight': '3568'
    },
    {  # минимальное значение после 0
        'offer_id': '4',
        'weight': 0.001,
        'expected_weight': '0.001'
    },  # тесты на правильное округление
    {  # значение больше трех знаков после запятой, округление вниз по 4му числу
        'offer_id': '5',
        'weight': 1500.123456789,
        'expected_weight': '1500.123'
    },
    {  # значение больше трех знаков после запятой, округление вверх по 4му числу
        'offer_id': '6',
        'weight': 1500.123556789,
        'expected_weight': '1500.124'
    },
    {  # если не выходим за 3 знака, то округления до целого не будет
        'offer_id': '7',
        'weight': 1500.999,
        'expected_weight': '1500.999'
    },
    {  # если выходим за 3 знака, то округление будет математически до целого
        'offer_id': '8',
        'weight': 1500.9999,
        'expected_weight': '1501'
    }
]


@pytest.fixture(scope="module")
def genlog_rows():
    offers = []
    for data in test_data:
        offer = default_genlog(
            offer_id=data['offer_id'],
            weight=data.get('weight', None),
            snippet_weight=data['expected_weight'],
        )
        offers.append(offer)
    return offers


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
    ) as env:
        env.execute()
        env.verify()
        yield env


def test_dimensions_snippet(genlog_snippet_workflow):
    expected_weights = [
        {
            'offer_id': data['offer_id'],
            'weight': data['expected_weight'],
        }
        for data in test_data
    ]
    for expected in expected_weights:
        assert_that(
            genlog_snippet_workflow,
            HasOutputStateRecord({'value': expected})
        )
