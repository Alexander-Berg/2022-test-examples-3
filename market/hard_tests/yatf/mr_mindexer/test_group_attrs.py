# coding: utf-8

import pytest
from hamcrest import assert_that, equal_to

from market.idx.generation.yatf.test_envs.mr_mindexer import MrMindexerBuildTestEnv, MrMindexerMergeTestEnv
from market.idx.generation.yatf.resources.mr_mindexer.mr_mindexer_helpers import MrMindexerMergeOptions, MrMindexerMergeIndexType

from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.offers.yatf.utils.fixtures import default_genlog, default_shops_dat
from market.idx.yatf.resources.shops_dat import ShopsDat
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join


@pytest.fixture(scope="module")
def sample_shops_dat():
    shop = default_shops_dat()
    shop['datafeed_id'] = 3000
    shop['shop_id'] = 2000
    shop['name'] = 'shop1'

    shop2 = default_shops_dat()
    shop2['datafeed_id'] = 4000
    shop2['shop_id'] = 5000
    shop2['name'] = 'shop 2'
    return ShopsDat(shops=[shop, shop2])


@pytest.fixture(scope="module")
def genlog_rows():
    return [
        default_genlog(**{
            'feed_id': 3000,
            'flags': 0,
            'ware_md5': 'fDbQKU6BwzM0vDugM73auA',
            'shop_id': 2000,
            'offer_id': 'offer_1',
            'client_id': 28,
            'classifier_magic_id': '73ba6bb98d2aec3f32056a63fb1b9a04',
            'category_id': 91526  # Силовые тренажеры
        }),
        default_genlog(**{
            'feed_id': 5000,
            'flags': 0,
            'ware_md5': 'X0MnfU0GsjNmbZC4TPrhaQ',
            'shop_id': 4000,
            'offer_id': 'offer_1',
            'client_id': 28,
        }),
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
            input_table_paths=input_table_paths,
    ) as env:
        env.execute()
        yield env


@pytest.yield_fixture(scope="module")
def mr_mindexer_build(yt_server, sample_shops_dat, offers_processor_workflow):
    resourses = {
        'shops_utf8_dat': sample_shops_dat
    }

    with MrMindexerBuildTestEnv(**resourses) as env:
        env.execute_from_offers_list(yt_server, offers_processor_workflow.genlog_dicts)
        env.verify()
        yield env


@pytest.yield_fixture(scope="module")
def mr_merge_index_aa_options(mr_mindexer_build):
    return MrMindexerMergeOptions(
        input_portions_path=mr_mindexer_build.yt_index_portions_path,
        part=0,
        index_type=MrMindexerMergeIndexType.AA,
        with_sent=False,
    )


@pytest.yield_fixture(scope="module")
def mr_mindexer_aa(yt_server, mr_merge_index_aa_options):
    resourses = {
        'merge_options': mr_merge_index_aa_options,
    }

    with MrMindexerMergeTestEnv(**resourses) as env:
        env.execute(yt_server)
        env.verify()
        yield env


@pytest.yield_fixture(scope="module")
def mr_merge_index_arch_options(mr_mindexer_build):
    return MrMindexerMergeOptions(
        input_portions_path=mr_mindexer_build.yt_index_portions_path,
        part=0,
        index_type=MrMindexerMergeIndexType.DIRECT_ARCH,
        with_sent=False,
    )


@pytest.yield_fixture(scope="module")
def mr_mindexer_arch(yt_server, mr_merge_index_arch_options):
    resourses = {
        'merge_options': mr_merge_index_arch_options,
    }

    with MrMindexerMergeTestEnv(**resourses) as env:
        env.execute(yt_server)
        env.verify()
        yield env


def test_group_attrs_aa(mr_mindexer_aa, genlog_rows):
    mr_mindexer_aa.outputs['indexaa'].load()
    attrs = mr_mindexer_aa.outputs['indexaa'].get_group_attributes(0)

    assert_that(attrs['feed_id'], equal_to('3000'))
    assert_that(attrs['main_dsrcid'], equal_to('2000'))
    assert_that(attrs.get('cmagic_id'), equal_to(genlog_rows[0]['classifier_magic_id']))
    assert_that(attrs.get('hidd'), equal_to(str(genlog_rows[0]['category_id'])))
    assert_that(attrs['ts'], equal_to(attrs['offer_ts']))
    assert_that('randx' in attrs)


def test_group_attrs_arch(mr_mindexer_arch):
    mr_mindexer_arch.outputs['indexarc'].load()
    doc_ids = list(mr_mindexer_arch.outputs['indexarc'].doc_ids)
    assert len(doc_ids) == 2
