# coding=utf-8

from hamcrest import (
    assert_that,
)
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
        'length': 10.0,
        'width': 20.0,
        'height': 30.0,
        'expected_dimensions_dict': {'length': '10', 'width': '20', 'height': '30'},
    },
    {
        'offer_id': '2',
        'length': 100.0,
        'expected_dimensions_dict': None,
    },
    {
        'offer_id': '3',
        'width': 200.0,
        'expected_dimensions_dict': None,
    },
    {
        'offer_id': '4',
        'height': 300.0,
        'expected_dimensions_dict': None,
    },
    {
        'offer_id': '5',
        'expected_dimensions_dict': None,
    },
    {  # минимальное значение после 0
        'offer_id': '6',
        'length': 10.001,
        'width': 20.001,
        'height': 30.001,
        'expected_dimensions_dict': {'length': '10.001', 'width': '20.001', 'height': '30.001'}
    },
    # тесты на правильное округление
    {  # значение больше трех знаков после запятой, округление вниз по 4му числу
        'offer_id': '7',
        'length': 10.123456789,
        'width': 20.123456789,
        'height': 30.123456789,
        'expected_dimensions_dict': {'length': '10.123', 'width': '20.123', 'height': '30.123'},
    },
    {  # значение больше трех знаков после запятой, округление вверх по 4му числу
        'offer_id': '8',
        'length': 10.123556789,
        'width': 20.123556789,
        'height': 30.123556789,
        'expected_dimensions_dict': {'length': '10.124', 'width': '20.124', 'height': '30.124'},
    },
    {  # если не выходим за 3 знака, то округления до целого не будет
        'offer_id': '9',
        'length': 10.999,
        'width': 20.999,
        'height': 30.999,
        'expected_dimensions_dict': {'length': '10.999', 'width': '20.999', 'height': '30.999'}
    },
    {  # если выходим за 3 знака, то округление будет математически до целого
        'offer_id': '10',
        'length': 10.9999,
        'width': 20.9999,
        'height': 30.9999,
        'expected_dimensions_dict': {'length': '11', 'width': '21', 'height': '31'}
    }
]


@pytest.fixture(scope="module")
def genlog_rows():
    offers = []
    for data in test_data:
        offer = default_genlog(
            offer_id=data['offer_id'],
            length=data.get('length', None),
            width=data.get('width', None),
            height=data.get('height', None),
            dimensions=merge_dimensions(data['expected_dimensions_dict']),
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


def merge_dimensions(x):
    if x is None:
        return None
    return '{}/{}/{}'.format(
        x['length'], x['width'], x['height']
    )

# на первый взгляд dimensions и dimensions_str перепутаны по смыслу, но тем не менее тест корректен


def test_dimensions_snippet(genlog_snippet_workflow):
    expected_dimensions = [
        {
            'offer_id': data['offer_id'],
            'dimensions': merge_dimensions(data['expected_dimensions_dict']),
            'dimensions_str': None,  # not exists
        }
        for data in test_data
    ]

    for expected in expected_dimensions:
        assert_that(
            genlog_snippet_workflow,
            HasOutputStateRecord({'value': expected})
        )
