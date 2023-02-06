# coding: utf-8

import pytest
from hamcrest import assert_that

from market.idx.generation.yatf.test_envs.mr_mindexer import MrMindexerBuildTestEnv, MrMindexerMergeTestEnv
from market.idx.generation.yatf.resources.mr_mindexer.mr_mindexer_helpers import MrMindexerMergeOptions, MrMindexerMergeIndexType

from market.idx.offers.yatf.matchers.offers_indexer.env_matchers import HasLiterals, HasNoLiterals
from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.offers.yatf.utils.fixtures import default_genlog
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join


test_data = [
    {
        'ware_md5': 'jaPRZC2qhM8tmb0yQQapzA'  # without barcode
    },
    {
        'ware_md5': 'cxlZ3cTeKNxAzS6OMWX51g',  # min length
        'barcode': '1'
    },
    {
        'ware_md5': 'XwMatVDskhEwoWXenZRifQ',  # pre-max length=19
        'barcode': '1234567890123456789'
    },
    {
        'ware_md5': 'WBxjfubDtEU49gfweJaA1Q',  # max length=20
        'barcode': '12345678901234567890'
    },
    {
        'ware_md5': 'wymqSwv6wBsx5XlChPmrwQ',  # several correct barcodes
        'barcode': '12345670|1234567890128|123456789012'
    }
]


@pytest.fixture(scope="module")
def genlog_rows():
    offers = []
    for data in test_data:
        if 'barcode' in data:
            offer = default_genlog(ware_md5=data['ware_md5'], barcode=data['barcode'])
        else:
            offer = default_genlog(ware_md5=data['ware_md5'])
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
            input_table_paths=input_table_paths,
    ) as env:
        env.execute()
        yield env


@pytest.yield_fixture(scope="module")
def mr_mindexer_direct(yt_server, offers_processor_workflow):
    with MrMindexerBuildTestEnv() as build_env:
        build_env.execute_from_offers_list(yt_server, offers_processor_workflow.genlog_dicts)
        build_env.verify()

        resourses = {
            'merge_options': MrMindexerMergeOptions(
                input_portions_path=build_env.yt_index_portions_path,
                part=0,
                index_type=MrMindexerMergeIndexType.DIRECT,
            ),
        }

        with MrMindexerMergeTestEnv(**resourses) as env:
            env.execute(yt_server)
            env.verify()
            yield env


def test_barcode(mr_mindexer_direct):
    doc_id = -1
    for offer in test_data:
        doc_id += 1
        if 'barcode' not in offer:
            assert_that(mr_mindexer_direct, HasNoLiterals('#barcode', [doc_id]))
            continue

        for barcode in offer['barcode'].split('|'):
            assert_that(mr_mindexer_direct, HasLiterals('#barcode="' + barcode, [doc_id]))
