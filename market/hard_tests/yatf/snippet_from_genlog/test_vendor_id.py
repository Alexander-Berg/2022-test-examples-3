# coding=utf-8

from hamcrest import (
    assert_that,
)
import pytest

from market.idx.generation.yatf.matchers.snippet_diff_builder.env_matchers import HasOutputStateRecord
from market.idx.generation.yatf.test_envs.snippet_diff_builder import SnippetDiffBuilderTestEnv
from market.idx.yatf.resources.mbo.global_vendors_xml import GlobalVendorsXml

from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.offers.yatf.utils.fixtures import default_genlog
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join


# TODO (bzz13): check this after MARKETOUT-28639'
test_data = [
    {
        'offer_id': '1',
        'vendor_id': 1,
        # 'expected_vendor_id': '1',
        'expected_vendor_id': None,
    },
    {
        'offer_id': '2',
        'vendor_id': 2,
        # 'expected_vendor_id': '2',
        'expected_vendor_id': None,
    },
    {
        'offer_id': '3',
        'vendor_id': 3,
        # 'expected_vendor_id': '3',
        'expected_vendor_id': None,
    },
    {
        'offer_id': '4',
        'vendor_id': None,
        'expected_vendor_id': None,
    },
]


@pytest.fixture(scope="module")
def global_vendors():
    xml = '''
        <global-vendors>
          <vendor id="1" name="name1">
            <site>site</site>
            <picture>picture</picture>
          </vendor>
          <vendor id="2" name="yandex">
            <is-fake-vendor>true</is-fake-vendor>
          </vendor>
        </global-vendors>
    '''
    return GlobalVendorsXml.from_str(xml)


@pytest.fixture(scope="module")
def genlog_rows():
    offers = []
    for data in test_data:
        offer = default_genlog(
            offer_id=data['offer_id'],
            vendor_id=data['vendor_id']
        )
        offers.append(offer)
    return offers


@pytest.fixture(scope="module")
def genlog_table(yt_server, genlog_rows):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0000'), genlog_rows)
    genlog_table.dump()
    return genlog_table


@pytest.yield_fixture(scope='module')
def offers_processor_workflow(yt_server, global_vendors, genlog_table):
    input_table_paths = [genlog_table.get_path()]

    resources = {
        'global_vendors_xml': global_vendors,
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
def expected_vendor_id():
    return [
        {
            'offer_id': x['offer_id'],
            'vendor_id': x['expected_vendor_id'],
        }
        for x in test_data
    ]


def test_vendor_id_snippet(genlog_snippet_workflow, expected_vendor_id):
    for expected in expected_vendor_id:
        assert_that(
            genlog_snippet_workflow,
            HasOutputStateRecord({'value': expected})
        )
