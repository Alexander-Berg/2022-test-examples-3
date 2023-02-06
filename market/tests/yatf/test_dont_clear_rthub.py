# coding=utf-8
import pytest

from hamcrest import assert_that

from robot.rthub.yql.protos.queries_pb2 import TMarketMainContentPageItem

from market.idx.streams.src.prepare_description_streams.yatf.test_env import (
    YtDescriptionStreamsTestEnv
)

from market.idx.yatf.resources.yt_table_resource import YtTableResource

from yt.wrapper.ypath import ypath_join

from zlib import compress


@pytest.fixture(scope='module')
def market_main_content_table_1():
    return [
        TMarketMainContentPageItem(
            Url='http://eldorado.ru/offer1',
            HtmlMainContent='Описание оффера 1 из Эльдорадо из table1')]


@pytest.fixture(scope='module')
def market_main_content_table_2():
    return [
        TMarketMainContentPageItem(
            Url='http://eldorado.ru/offer2',
            HtmlMainContent='Описание оффера 2 из Эльдорадо из table2')]


@pytest.fixture(scope='module')
def rthub_path(yt_stuff,
               market_main_content_table_1,
               market_main_content_table_2):
    path = '//home/test_descriptions/rthub/market'

    yt_stuff.get_yt_client().create(
        'map_node',
        path,
        recursive=True,
        ignore_existing=True,
    )

    data_for_table_1 = []
    for main_content in market_main_content_table_1:
        data_for_table_1 += [
            {'key': 'k1',
             'subkey': 'sk1',
             'value': compress(main_content.SerializeToString())}
        ]

    table1 = YtTableResource(yt_stuff,
                             ypath_join(path, 'table1'),
                             data_for_table_1)
    table1.dump()

    data_for_table_2 = []
    for main_content in market_main_content_table_2:
        data_for_table_2 += [
            {'key': 'k1',
             'subkey': 'sk1',
             'value': compress(main_content.SerializeToString())}
        ]

    table2 = YtTableResource(yt_stuff,
                             ypath_join(path, 'table2'),
                             data_for_table_2)
    table2.dump()

    return path


@pytest.fixture(scope='module')
def genlog_table(yt_stuff):
    data = [
        {'url': 'http://www.eldorado.ru/offer1'},
    ]
    table = YtTableResource(yt_stuff, "//home/test_descriptions/genlog", data)
    table.dump()

    return table


@pytest.fixture(scope='module')
def descriptions_table(yt_stuff):
    data = [
        {'key': 'https://www.dom.ru/offer2?cpc=1&ololo=2',
         'subkey': 'dom.ru/offer2?ololo=2',
         'value': 'Старое описание оффера 2 из dom.ru'},
    ]
    table = YtTableResource(yt_stuff,
                            "//home/test_descriptions/descriptions",
                            data)
    table.dump()

    return table


@pytest.yield_fixture(scope='module')
def workflow(yt_stuff, rthub_path, genlog_table, descriptions_table):
    resources = {}
    dont_clear_rthub = True

    with YtDescriptionStreamsTestEnv(**resources) as env:
        env.execute(yt_stuff,
                    rthub_path,
                    genlog_table.get_path(),
                    descriptions_table.get_path(),
                    "//home/test_descriptions/hosts",
                    dont_clear_rthub)
        env.verify()
        yield env


@pytest.fixture(scope='module')
def result_descriptions_table(workflow):
    return workflow.outputs.get('descriptions_table')


def test_rthub_tables_exist(yt_stuff, result_descriptions_table):
    rthub_path = '//home/test_descriptions/rthub/market'
    for table_name in ('table1', 'table2'):
        assert_that(yt_stuff.get_yt_client().exists(
                    ypath_join(rthub_path, table_name)),
                    '{} in rthub does not exist'.format(table_name))
