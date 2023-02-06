# coding: utf-8

import pytest
from hamcrest import assert_that

from market.idx.generation.yatf.test_envs.mr_mindexer import MrMindexerBuildTestEnv, MrMindexerMergeTestEnv
from market.idx.generation.yatf.resources.mr_mindexer.mr_mindexer_helpers import MrMindexerMergeOptions, MrMindexerMergeIndexType

from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.offers.yatf.utils.fixtures import default_genlog
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join


@pytest.fixture(scope="module")
def genlog_rows():
    return [
        default_genlog(
            title='Common title'
        ),
        default_genlog(
            title='(1T02S50NL0) TK-1170'
        ),
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


def test_sentences(mr_mindexer_direct_arc):
    """ Проверка того, что типовые предложения в индексе строятся без изменений.

        Скобка в начале заголовка не должна влиять на первое предложение в индексе, т.к. на выдаче отображается
        именно оно. Была проблема, что заголовок '(1T02S50NL0) TK-1170' сохранялся в индексе, как
        '(1T02S50NL0) TK-1170(', т.к. он повторно добавлялся как второе предложение, но алгоритм в ядре поиска
        расценивал первые non-letter и non-number символы как завершение первого предложения и добавлял их в конец
        первого пердложения.
    """

    arc = mr_mindexer_direct_arc.outputs['indexarc']

    sentences1 = arc.load_doc_description(0)['sentences']
    assert_that(sentences1[0] == 'Common title')
    assert_that(sentences1[1].startswith('Common title'))

    sentences2 = arc.load_doc_description(1)['sentences']
    assert_that(sentences2[0] == '(1T02S50NL0) TK-1170')
    assert_that(sentences2[1].startswith('1T02S50NL0) TK-1170'))
