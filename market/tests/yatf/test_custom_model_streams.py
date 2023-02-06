# coding=utf-8
import pytest

from hamcrest import assert_that, equal_to


from market.idx.streams.src.prepare_model_streams.yatf.test_env import YtCustomModelStreamsTestEnv
from market.idx.yatf.resources.yt_table_resource import YtTableResource

from mapreduce.yt.python.table_schema import extract_column_attributes

"""
    Тест проверяет YT джобу, которая подготавливает из модельной таблички табличку с alias стримами.
    Конечная таблица -- это таблица с идентификатором, чтобы можно было добавить стрим к вебоским данным
    (в нашем случае model_id),
    и информация о стриме: text = alias, регион и вес стрима.
"""


@pytest.fixture(scope='module')
def models_table(yt_stuff):
    data = [
        dict(id=13, aliases="", title=""),  # empty alias and empty title
        dict(id=1313, aliases="\"; \"", title="title 1313"),  # empty alias and not empty title
        dict(id=131313, aliases="\"\"", title="other title for 131313", full_description=""),  # other empty alias and empty full_description
        dict(id=77, aliases="\"alias77\" ;", title="", full_description="описание не попадет в стримы, т.к. модели нет на синем"),  # single alias
        # two aliases, full_description
        dict(id=11, aliases="\"alias11_1\" ; \"alias11_2\" ;", title="yet another title", full_description="описание", published_on_blue_market=True),
        # two aliases with symbols and spaces, full_description
        dict(id=7, aliases="\"alias7 with spaces\" ; \"alias7_2 with symbol ; !\" ;", title="", full_description="еще описание", published_on_blue_market=True),
        dict(id=5, aliases="\"alias5_with_Russia хАчу эту фетровую шляпу и айфон Х!!!\" ;", title="Тайтл модели на русском"),  # alias with Russia words
    ]
    table = YtTableResource(yt_stuff, "//home/in/models", data)
    table.dump()

    return table


@pytest.yield_fixture(scope='module')
def workflow(yt_stuff, models_table):
    resources = {}

    with YtCustomModelStreamsTestEnv(**resources) as env:
        env.execute(yt_stuff, models_table.get_path(), "//home/streams/models_aliases", "//home/streams/models_titles", "//home/streams/marketing_descrs")
        env.verify()
        yield env


@pytest.fixture(scope='module')
def aliases_yt_table(workflow):
    return workflow.outputs.get('aliases_table')


@pytest.fixture(scope='module')
def titles_yt_table(workflow):
    return workflow.outputs.get('titles_table')


@pytest.fixture(scope='module')
def marketing_descriptions_yt_table(workflow):
    return workflow.outputs.get('marketing_descriptions_table')


def test_aliases_table_exist(aliases_yt_table, yt_stuff):
    assert_that(yt_stuff.get_yt_client().exists(aliases_yt_table.get_path()), 'Table doesn\'t exist')


def test_titles_table_exist(titles_yt_table, yt_stuff):
    assert_that(yt_stuff.get_yt_client().exists(titles_yt_table.get_path()), 'Table doesn\'t exist')


def test_marketing_descriptions_table_exist(marketing_descriptions_yt_table, yt_stuff):
    assert_that(yt_stuff.get_yt_client().exists(marketing_descriptions_yt_table.get_path()), 'Table doesn\'t exist')


def check_result_table_schema(table):
    assert_that(extract_column_attributes(list(table.schema)),
                equal_to([
                    {'required': False, "name": "model_id", "type": "string"},
                    {'required': False, "name": "part", "type": "uint64"},
                    {'required': False, "name": "region_id", "type": "uint64"},
                    {'required': False, "name": "text", "type": "string"},
                    {'required': False, "name": "url", "type": "string"},
                    {'required': False, "name": "value", "type": "string"},
                ]), "Schema is incorrect")


def test_aliases_table_schema(aliases_yt_table):
    check_result_table_schema(aliases_yt_table)


def test_titles_table_schema(titles_yt_table):
    check_result_table_schema(titles_yt_table)


def test_marketing_descriptions_table_schema(marketing_descriptions_yt_table):
    check_result_table_schema(marketing_descriptions_yt_table)


def test_model_alias_streams(aliases_yt_table):
    """Проверяем что на выходе мап-редьюс операции в таблицу с aliases-стримом в колонку text попадают
       алиасы моделей (если они не пустые), при этом у одной модели может получится более одной записи,
       если в изначальной таблице в поле aliases было более одного алиаса"""
    assert_that(
        list(aliases_yt_table.data),
        equal_to([
            {'model_id': '77', 'region_id': 225, 'url': 'market.yandex.ru/product/77', 'text': 'alias77', 'value': '1', 'part': 0},
            {'model_id': '11', 'region_id': 225, 'url': 'market.yandex.ru/product/11', 'text': 'alias11_1', 'value': '1', 'part': 0},
            {'model_id': '11', 'region_id': 225, 'url': 'market.yandex.ru/product/11', 'text': 'alias11_2', 'value': '1', 'part': 0},
            {'model_id': '7',  'region_id': 225, 'url': 'market.yandex.ru/product/7',  'text': 'alias7 with spaces', 'value': '1', 'part': 0},
            {'model_id': '7',  'region_id': 225, 'url': 'market.yandex.ru/product/7',  'text': 'alias7_2 with symbol ; !', 'value': '1', 'part': 0},
            {'model_id': '5',  'region_id': 225, 'url': 'market.yandex.ru/product/5',  'text': 'alias5_with_Russia хАчу эту фетровую шляпу и айфон Х!!!', 'value': '1', 'part': 0},
        ]),
        "No expected aliases")


def test_model_title_streams(titles_yt_table):
    """Проверяем что на выходе мап-редьюс операции в таблицу со title-стримом в колонку text
       попадают тайтлы моделей (если они не пустые)"""
    assert_that(
        list(titles_yt_table.data),
        equal_to([
            {'model_id': '1313', 'region_id': 225, 'url': 'market.yandex.ru/product/1313', 'text': 'title 1313', 'value': '1', 'part': 0},
            {'model_id': '131313', 'region_id': 225, 'url': 'market.yandex.ru/product/131313', 'text': 'other title for 131313', 'value': '1', 'part': 0},
            {'model_id': '11', 'region_id': 225, 'url': 'market.yandex.ru/product/11', 'text': 'yet another title', 'value': '1', 'part': 0},
            {'model_id': '5',  'region_id': 225, 'url': 'market.yandex.ru/product/5',  'text': 'Тайтл модели на русском', 'value': '1', 'part': 0},
        ]),
        "No expected titles")


def test_model_marketing_descriptions_streams(marketing_descriptions_yt_table):
    """Проверяем что на выходе мап-редьюс операции в таблицу с  marketting description стримом в колонку text
       попадают маркетинговые описания моделей (если они не пустые)"""
    assert_that(
        list(marketing_descriptions_yt_table.data),
        equal_to([
            {'model_id': '11', 'region_id': 225, 'url': 'market.yandex.ru/product/11', 'text': 'описание', 'value': '1', 'part': 0},
            {'model_id': '7',  'region_id': 225, 'url': 'market.yandex.ru/product/7',  'text': 'еще описание', 'value': '1', 'part': 0},
        ]),
        "No expected descriptions")
