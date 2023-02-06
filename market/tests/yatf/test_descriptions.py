# coding=utf-8
import pytest

from hamcrest import assert_that, equal_to

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
            HtmlMainContent='Описание оффера 1 из Эльдорадо из table1'),
        TMarketMainContentPageItem(
            Url='http://eldorado.ru/offer2',
            HtmlMainContent='Описание оффера 2 из Эльдорадо из table1'),
        TMarketMainContentPageItem(
            Url='https://www.dom.ru/offer1?azaza=1&mrkt=up',
            HtmlMainContent='Описание оффера 1 из dom.ru из table1')]


@pytest.fixture(scope='module')
def market_main_content_table_2():
    return [
        # Will replace offer2 from table1 because table2 > table1 lexicographically
        TMarketMainContentPageItem(
            Url='http://eldorado.ru/offer2',
            HtmlMainContent='Описание оффера 2 из Эльдорадо из table2'),
        TMarketMainContentPageItem(
            Url='http://eldorado.ru/offer3',
            HtmlMainContent='Описание оффера 3 из Эльдорадо из table2'),
        # Will be gone as there is no such host in genlog
        TMarketMainContentPageItem(
            Url='http://shop.ru/offer1',
            HtmlMainContent='Описание оффера 1 из shop.ru из table2')]


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
        {'url': 'http://www.eldorado.ru/offer2'},
        {'url': 'https://www.eldorado.ru/offer3'},
        {'url': 'dom.ru/offer100500'}
    ]
    table = YtTableResource(yt_stuff, "//home/test_descriptions/genlog", data)
    table.dump()

    return table


@pytest.fixture(scope='module')
def descriptions_table(yt_stuff):
    data = [
        # This offer will remain -- it's not present in rthub tables and its
        # host is present in genlog table
        {'key': 'https://www.dom.ru/offer2?cpc=1&ololo=2',
         'subkey': 'dom.ru/offer2?ololo=2',
         'value': 'Старое описание оффера 2 из dom.ru'},

        # This offer will be gone -- its host is not present in genlog table
        {'key': 'http://www.magaz.ru/offer1',
         'subkey': 'magaz.ru/offer1',
         'value': 'Старое описание оффера 1 из magaz.ru'},

        # This offer will be replaced by the new one from table1 in rthub
        {'key': 'http://eldorado.ru/offer1',
         'subkey': 'eldorado.ru/offer1',
         'value': 'Старое описание оффера 1 из eldorado.ru'}
    ]
    table = YtTableResource(yt_stuff,
                            "//home/test_descriptions/descriptions",
                            data)
    table.dump()

    return table


@pytest.yield_fixture(scope='module')
def workflow(yt_stuff, rthub_path, genlog_table, descriptions_table):
    resources = {}

    with YtDescriptionStreamsTestEnv(**resources) as env:
        env.execute(yt_stuff,
                    rthub_path,
                    genlog_table.get_path(),
                    descriptions_table.get_path(),
                    "//home/test_descriptions/hosts")
        env.verify()
        yield env


@pytest.fixture(scope='module')
def result_descriptions_table(workflow):
    return workflow.outputs.get('descriptions_table')


@pytest.fixture(scope='module')
def result_hosts_table(workflow):
    return workflow.outputs.get('hosts_table')


def test_result_tables_exist(result_descriptions_table,
                             result_hosts_table,
                             yt_stuff):
    assert_that(yt_stuff.get_yt_client().exists(result_descriptions_table.get_path()),
                'Table with descriptions doesn\'t exist')
    assert_that(yt_stuff.get_yt_client().exists(result_hosts_table.get_path()),
                'Table with hosts doesn\'t exist')


def test_rthub_tables_dont_exist(yt_stuff):
    rthub_path = '//home/test_descriptions/rthub/market'
    for table_name in ('table1', 'table2'):
        assert_that(not yt_stuff.get_yt_client().exists(ypath_join(rthub_path, table_name)),
                    '{} in rthub exists'.format(table_name))


def test_hosts(result_hosts_table):
    sorted_by_host = lambda lst: sorted(lst, key=lambda row: row['host'])
    assert_that(sorted_by_host(list(result_hosts_table.data)),
                equal_to(sorted_by_host([
                    # only once; www and scheme shouldn't be removed
                    {'host': 'http://www.eldorado.ru'},

                    {'host': 'https://www.eldorado.ru'},
                    {'host': 'dom.ru'},
                ])),
                'Wrong hosts')


def test_descriptions(result_descriptions_table):
    sorted_by_key = lambda lst: sorted(lst, key=lambda row: row['key'])
    assert_that(sorted_by_key(list(result_descriptions_table.data)),
                equal_to(sorted_by_key([
                    # Description from table1 overrides old description
                    {'key': 'http://eldorado.ru/offer1',
                     'subkey': 'eldorado.ru/offer1',
                     'value': 'Описание оффера 1 из Эльдорадо из table1'},
                    # Description from table2 overrides description from table1
                    {'key': 'http://eldorado.ru/offer2',
                     'subkey': 'eldorado.ru/offer2',
                     'value': 'Описание оффера 2 из Эльдорадо из table2'},
                    # Just new description from table2
                    {'key': 'http://eldorado.ru/offer3',
                     'subkey': 'eldorado.ru/offer3',
                     'value': 'Описание оффера 3 из Эльдорадо из table2'},
                    # New description from table1 for dom.ru
                    {'key': 'https://www.dom.ru/offer1?azaza=1&mrkt=up',
                     'subkey': 'dom.ru/offer1?azaza=1',
                     'value': 'Описание оффера 1 из dom.ru из table1'},
                    # Old description is not overriden
                    {'key': 'https://www.dom.ru/offer2?cpc=1&ololo=2',
                     'subkey': 'dom.ru/offer2?ololo=2',
                     'value': 'Старое описание оффера 2 из dom.ru'},
                ])), 'Wrong descriptions')
