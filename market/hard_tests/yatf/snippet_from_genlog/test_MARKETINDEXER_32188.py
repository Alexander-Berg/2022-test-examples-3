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


MAGIC_CONST = 'много букв' * 285 + 'много'

test_data = [
    {
        'description': (
            'Royal Clima внутренние блоки премиум RCI-P32HN*2шт и 2шт ИК-пульт + наружный блок 2RFM-18HN.\n' +
            '● Энергоэффективность класса А++\n' +
            '● Инвертор EU ERP\n' +
            MAGIC_CONST
        ),
        'sales_notes': 'По Москве 10% в регионы по 100% предоплате.',
        'title': 'МУЛЬТИ-СПЛИТ-СИСТЕМА Royal Clima RCI-P32HN*2+RFM2-18HN На две комнаты 35м2 и 20м2',
        'type': 5,
        'shop_category_id': '1',
        'shop_category_path': 'Всё для радости и счастья',
        'shop_category_path_ids': '1',
        'shop_name': 'test_shop',
        'offer_id': '1',
    },
]


@pytest.fixture(scope="module")
def genlog_rows():
    feed = []
    for data in test_data:
        offer = default_genlog(**data)
        feed.append(offer)
    return feed


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
            input_table_paths=input_table_paths,
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
def expected_description_snippet():
    return [{
        'offer_id': '1',
        'description': (
            'Royal Clima внутренние блоки премиум RCI-P32HN*2шт и 2шт ИК-пульт + наружный блок 2RFM-18HN.\n' +
            '● Энергоэффективность класса А++\n' +
            '● Инвертор EU ERP\n' +
            MAGIC_CONST
        ),
        'sales_notes': 'По Москве 10% в регионы по 100% предоплате.',
        '_Title': 'МУЛЬТИ-СПЛИТ-СИСТЕМА Royal Clima RCI-P32HN*2+RFM2-18HN На две комнаты 35м2 и 20м2',
    }]


def test_MARKETINDEXER_32188_snippet(genlog_snippet_workflow, expected_description_snippet):
    # raise RuntimeError(genlog_snippet_workflow.output_state_table[0])
    for expected in expected_description_snippet:
        assert_that(
            genlog_snippet_workflow,
            HasOutputStateRecord({'value': expected})
        )
