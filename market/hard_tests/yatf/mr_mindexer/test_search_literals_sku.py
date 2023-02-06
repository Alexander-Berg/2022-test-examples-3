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


@pytest.fixture(scope="module")
def genlog_rows():
    # https://a.yandex-team.ru/arc/trunk/arcadia/market/indexer/yatf/resources/offers_indexer/feed.py?rev=3007979&blame=true#L54
    # "sku" would be passed to EnrichedOffer

    offer_without_sku = default_genlog(offer_id='1', ware_md5='09lEaAKkQll1XTaaaaaaaQ')

    offer_with_empty_sku = default_genlog(offer_id='2', ware_md5='kP3oC5KjARGI5f9EEkNGtA', sc_sku='')

    # 18 length of sku with according to
    # https://wiki.yandex-team.ru/users/kvmultiship/fullfillmetnSKU/#2.sku.formirovaniexranenieiresheniekonfliktov
    offer_with_sku = default_genlog(offer_id='3', ware_md5='t+HYw9KglGtvW5itMzVbdA', sc_sku='SOMESKUFROMUCBZZ13')

    offer_with_short_sku = default_genlog(offer_id='4', ware_md5='yNWXdRty80uhcFXrHX8ohA', sc_sku='12345678901234567')

    offer_with_long_sku = default_genlog(offer_id='5', ware_md5='Li/2sW0BMcWWmEkd3+7MHg', sc_sku='1234567890123456789')

    offer_with_spec_sku = default_genlog(offer_id='6', ware_md5='fDbQKU6BwzM0vDugM73auA', sc_sku='!@#%^&*(_+-=Qa"\'>\\')

    offer_with_utf8_sku = default_genlog(offer_id='7', ware_md5='NzA5NDU0MWI2ODgzNDY4Mg', sc_sku=u'ХэлоВорлд')

    offer_with_shop_sku = default_genlog(offer_id='8', ware_md5='zP3oN5KjARGI5f9EEkNGtA', shop_sku='vgvjc49coiv1mc5vb21d')

    offer_with_both_sku = default_genlog(offer_id='9', ware_md5='KklEaAKkQll1XTaaabbbbQ', shop_sku='mqct01s3b3fvsv89eipk', sc_sku='SOMESKUFROMUCBZZ14')

    return [
        offer_without_sku,
        offer_with_empty_sku,
        offer_with_sku,
        offer_with_short_sku,
        offer_with_long_sku,
        offer_with_spec_sku,
        offer_with_utf8_sku,
        offer_with_shop_sku,
        offer_with_both_sku,
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
def mr_mindexer_build(yt_server, offers_processor_workflow):
    with MrMindexerBuildTestEnv() as build_env:
        build_env.execute_from_offers_list(yt_server, offers_processor_workflow.genlog_dicts)
        build_env.verify()
        yield build_env


@pytest.yield_fixture(scope="module")
def mr_mindexer_direct(yt_server, mr_mindexer_build):
    resourses = {
        'merge_options': MrMindexerMergeOptions(
            input_portions_path=mr_mindexer_build.yt_index_portions_path,
            part=0,
            index_type=MrMindexerMergeIndexType.DIRECT,
        ),
    }

    with MrMindexerMergeTestEnv(**resourses) as env:
        env.execute(yt_server)
        env.verify()
        env.literal_lemmas.load()
        yield env


@pytest.yield_fixture(scope="module")
def mr_mindexer_direct_arc(yt_server, mr_mindexer_build):
    resourses = {
        'merge_options': MrMindexerMergeOptions(
            input_portions_path=mr_mindexer_build.yt_index_portions_path,
            part=0,
            index_type=MrMindexerMergeIndexType.DIRECT_ARCH,
        ),
    }

    with MrMindexerMergeTestEnv(**resourses) as env:
        env.execute(yt_server)
        env.verify()
        env.outputs['indexarc'].load()
        yield env


@pytest.fixture(scope="module")
def doc_id_by_offer_id(mr_mindexer_direct_arc):
    mapping = {}
    arc = mr_mindexer_direct_arc.outputs['indexarc']
    for i in arc.doc_ids:
        offer_id = arc.load_doc_description(i)['offer_id']
        mapping[offer_id] = i
    return mapping


@pytest.mark.skip(reason='disable test for MARKETINDEXER-9327')
def test_offer_sku_search_literal(mr_mindexer_direct, doc_id_by_offer_id):
    # https://a.yandex-team.ru/arc/trunk/arcadia/market/idx/yatf/matchers/env_matchers.py?rev=3011253&blame=true#L204

    # документ есть, а литерал отсутсвует
    assert_that(mr_mindexer_direct, HasNoLiterals('#sku', [doc_id_by_offer_id['1']]))

    # документ есть, а литерал отсутсвует
    assert_that(mr_mindexer_direct, HasNoLiterals('#sku', [doc_id_by_offer_id['2']]))

    # случай валидного sku
    assert_that(mr_mindexer_direct, HasLiterals('#sku="someskufromucbzz13' , [doc_id_by_offer_id['3']]))

    # документ есть, а литерал отсутсвует: слишком коротко
    assert_that(mr_mindexer_direct, HasNoLiterals('#sku', [doc_id_by_offer_id['4']]))

    # документ есть, а литерал отсутсвует: слишком длинно
    assert_that(mr_mindexer_direct, HasNoLiterals('#sku', [doc_id_by_offer_id['5']]))

    # документ есть, а литерал отсутсвует: допустимы только цифр и латинские буквы
    assert_that(mr_mindexer_direct, HasNoLiterals('#sku', [doc_id_by_offer_id['6']]))

    # документ есть, а литерал отсутсвует: допустимы только цифр и латинские буквы
    assert_that(mr_mindexer_direct, HasNoLiterals('#sku', [doc_id_by_offer_id['7']]))


def test_offer_shop_sku_literal(mr_mindexer_direct, doc_id_by_offer_id):
    assert_that(mr_mindexer_direct, HasLiterals('#sku="vgvjc49coiv1mc5vb21d' , [doc_id_by_offer_id['8']]))
    assert_that(mr_mindexer_direct, HasLiterals('#sku="mqct01s3b3fvsv89eipk' , [doc_id_by_offer_id['9']]))
