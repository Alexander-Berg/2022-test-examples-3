# coding=utf-8
import pytest

from hamcrest import assert_that, equal_to

from market.idx.streams.src.prepare_model_streams_for_offers.yatf.test_env import (
    YtModelStreamsForOffersTestEnv,
    Offer,
    contains_only,
)
from market.idx.yatf.resources.yt_table_resource import YtTableResource

from mapreduce.yt.python.table_schema import extract_column_attributes


"""
    Тест проверяет YT джобу, которая конвертируем модельные стримы с тайтлами/алиасами
    в стримы для офферов.

    Конечная таблица -- это таблица с идентификатором оффера (ware_md5)
    и информацией о стриме: text = title/alias/marketing descr, регион и вес стрима.

    Исходные таблицы: стрим с title, стрим с alias, стрим с marketing_descr и шардированные оффера
"""


@pytest.fixture(scope='module')
def model_titles(yt_stuff):
    """Модельный стрим с тайтлами моделей"""
    # префикс title здесь просто для удобства
    data = [
        {'model_id': '71', 'text': 'title:Seven-Eleven'},
        {'model_id': '11', 'text': 'title:Одиннадцать'},
    ]
    table = YtTableResource(yt_stuff, "//home/in/models_titles", data)
    table.dump()

    return table


@pytest.fixture(scope='module')
def model_aliases(yt_stuff):
    """Модельный стрим с алиасами моделей"""
    # префикс alias здесь просто для удобства
    data = [
        {'model_id': '71', 'text': 'alias:Seventy one'},
        {'model_id': '5', 'text': 'alias:Пятак'},
        {'model_id': '5', 'text': 'alias:Пятерочка'},
    ]
    table = YtTableResource(yt_stuff, "//home/in/models_aliases", data)
    table.dump()

    return table


@pytest.fixture(scope='module')
def marketing_descriptions(yt_stuff):
    """Модельный стрим с маркетинговыми описаниями моделей"""
    # префикс marketing_descr здесь просто для удобства
    data = [
        {'model_id': '71', 'text': 'marketing_descr:7Я'},
        {'model_id': '5', 'text': 'marketing_descr:Магнит'},
        {'model_id': '111', 'text': 'marketing_descr:Дикси'},
    ]
    table = YtTableResource(yt_stuff, "//home/in/marketing_descrs", data)
    table.dump()

    return table


@pytest.fixture(scope='module')
def micro_model_descriptions(yt_stuff):
    """Модельный стрим с micro_model описаниями моделей"""
    # префикс micro_model_descr здесь просто для удобства
    data = [
        {'model_id': '5', 'text': 'micro_model_descr:размеры (ШхГхВ): 60x60x82 см'},
        {'model_id': '11', 'text': 'micro_model_descr:цвет: белый'},
    ]
    table = YtTableResource(yt_stuff, "//home/in/micro_model_descrs", data)
    table.dump()

    return table


@pytest.fixture(scope='module')
def cpa_queries(yt_stuff):
    """Модельный стрим с запросами по cpa заказам"""
    data = [
        {'model_id': '1', 'text': 'запрос 1'},
        {'model_id': '11', 'text': 'запрос 2'},
    ]
    table = YtTableResource(yt_stuff, "//home/in/cpa_queries", data)
    table.dump()

    return table


@pytest.fixture(scope='module')
def offers_dir(yt_stuff):
    """Возвращает путь до директории с офферными таблицами"""
    path = "//home/in/offers"
    data0 = [
        Offer(ware_md5="WareId_NoModel").to_row(),
        Offer(ware_md5="WareId_NoModelData", hyperid=1).to_row(),

        Offer(ware_md5="WareId_11_1", hyperid=11, is_fake_msku_offer=False).to_row(),
        Offer(ware_md5="WareId_5", hyperid=5).to_row(),
    ]
    table0 = YtTableResource(yt_stuff, path + "/0000", data0)
    table0.dump()

    data1 = [
        Offer(ware_md5="WareId_IsFakeMsku", hyperid=5, is_fake_msku_offer=True).to_row(),
        Offer(ware_md5="WareId_11_2", hyperid=11).to_row(),
        Offer(ware_md5="WareId_71", hyperid=71).to_row(),
        Offer(ware_md5="WareId_111", hyperid=111).to_row(),
    ]
    table1 = YtTableResource(yt_stuff, path + "/0001", data1)
    table1.dump()

    return path


@pytest.yield_fixture(scope='module')
def workflow(yt_stuff,
             offers_dir,
             model_titles,
             model_aliases,
             marketing_descriptions,
             micro_model_descriptions,
             cpa_queries):
    resources = {}

    with YtModelStreamsForOffersTestEnv(**resources) as env:
        env.execute(yt_stuff,
                    offers_dir=offers_dir,  # offers_dir шардированная коллекция офферов
                    parts_count=2,
                    yt_title_streams_path=model_titles.get_path(),
                    yt_alias_streams_path=model_aliases.get_path(),
                    yt_marketing_descr_streams_path=marketing_descriptions.get_path(),
                    yt_micro_model_descr_streams_path=micro_model_descriptions.get_path(),
                    yt_cpa_queries_streams_path=cpa_queries.get_path(),
                    yt_title_streams_result_path="//home/streams/titles",
                    yt_alias_streams_result_path="//home/streams/aliases",
                    yt_marketing_descr_streams_result_path="//home/streams/marketing_descr",
                    yt_micro_model_descr_streams_result_path="//home/streams/micro_model_descr",
                    yt_cpa_queries_result_path="//home/streams/cpa_queries")
        env.verify()
        yield env


@pytest.fixture(scope='module')
def aliases_result(workflow):
    return workflow.outputs.get('aliases')


@pytest.fixture(scope='module')
def titles_result(workflow):
    return workflow.outputs.get('titles')


@pytest.fixture(scope='module')
def marketing_descrs_result(workflow):
    return workflow.outputs.get('marketing_descriptions')


@pytest.fixture(scope='module')
def micro_model_descrs_result(workflow):
    return workflow.outputs.get('micro_model_descriptions')


@pytest.fixture(scope='module')
def cpa_queries_result(workflow):
    return workflow.outputs.get('cpa_queries')


def test_aliases_table_exist(aliases_result, yt_stuff):
    assert_that(yt_stuff.get_yt_client().exists(aliases_result.get_path()), 'Table doesn\'t exist')


def test_titles_table_exist(titles_result, yt_stuff):
    assert_that(yt_stuff.get_yt_client().exists(titles_result.get_path()), 'Table doesn\'t exist')


def test_marketing_descrs_table_exist(marketing_descrs_result, yt_stuff):
    assert_that(yt_stuff.get_yt_client().exists(marketing_descrs_result.get_path()), 'Table doesn\'t exist')


def test_micro_model_descrs_table_exist(micro_model_descrs_result, yt_stuff):
    assert_that(yt_stuff.get_yt_client().exists(micro_model_descrs_result.get_path()), 'Table doesn\'t exist')


def test_cpa_queries_table_exist(cpa_queries_result, yt_stuff):
    assert_that(yt_stuff.get_yt_client().exists(cpa_queries_result.get_path()), 'Table doesn\'t exist')


def check_result_table_schema(table):
    assert_that(extract_column_attributes(list(table.schema)),
                equal_to([
                    {'required': False, "name": "ware_md5", "type": "string"},
                    {'required': False, "name": "part", "type": "uint64"},
                    {'required': False, "name": "region_id", "type": "uint64"},
                    {'required': False, "name": "text", "type": "string"},
                    {'required': False, "name": "value", "type": "string"},
                ]), "Schema is incorrect")


def test_aliases_table_schema(aliases_result):
    check_result_table_schema(aliases_result)


def test_titles_table_schema(titles_result):
    check_result_table_schema(titles_result)


def test_marketing_descr_table_schema(marketing_descrs_result):
    check_result_table_schema(marketing_descrs_result)


def test_micro_model_descr_table_schema(micro_model_descrs_result):
    check_result_table_schema(micro_model_descrs_result)


def test_cpa_queries_table_schema(cpa_queries_result):
    check_result_table_schema(cpa_queries_result)


def test_aliases_streams(aliases_result):
    """Проверяем что на выходе мап-редьюс операции в таблицу с aliases-стримом в колонку text попадают
       алиасы моделей приматченных к офферам (если офферы не являются фейковыми мскушными офферами),
       значение part соответствует исходному шарду оффера
    """

    expected = [
        {'ware_md5': 'WareId_5', 'region_id': 225L, 'text': 'alias:Пятак', 'value': '1', 'part': 0L},
        {'ware_md5': 'WareId_5', 'region_id': 225L, 'text': 'alias:Пятерочка', 'value': '1', 'part': 0L},
        {'ware_md5': 'WareId_71', 'region_id': 225L, 'text': 'alias:Seventy one', 'value': '1', 'part': 1L},
    ]
    assert_that(
        list(aliases_result.data),
        contains_only(expected),
        "No expected aliases")


def test_titles_streams(titles_result):
    """Проверяем что на выходе мап-редьюс операции в таблицу с title-стримом в колонку text
       попадают тайтлы моделей (если офферы не являются фейковыми мскушными офферами)
       значение part соответствует исходному шарду оффера
    """

    assert_that(
        list(titles_result.data),
        contains_only([
            {'ware_md5': 'WareId_11_1', 'region_id': 225L, 'text': 'title:Одиннадцать', 'value': '1', 'part': 0L},
            {'ware_md5': 'WareId_11_2', 'region_id': 225L, 'text': 'title:Одиннадцать', 'value': '1', 'part': 1L},
            {'ware_md5': 'WareId_71', 'region_id': 225L, 'text': 'title:Seven-Eleven', 'value': '1', 'part': 1L},
        ]),
        "No expected titles")


def test_marketing_descr_streams(marketing_descrs_result):
    """Проверяем что на выходе мап-редьюс операции в таблицу с marketing_description-стримом в колонку text
       попадают маркетинговые описания моделей (если офферы не являются фейковыми мскушными офферами)
       значение part соответствует исходному шарду оффера
    """

    assert_that(
        list(marketing_descrs_result.data),
        contains_only([
            {'ware_md5': 'WareId_71', 'region_id': 225L, 'text': 'marketing_descr:7Я', 'value': '1', 'part': 1L},
            {'ware_md5': 'WareId_5', 'region_id': 225L, 'text': 'marketing_descr:Магнит', 'value': '1', 'part': 0L},
            {'ware_md5': 'WareId_111', 'region_id': 225L, 'text': 'marketing_descr:Дикси', 'value': '1', 'part': 1L},
        ]),
        "No expected marketing descriptions")


def test_micro_model_descr_streams(micro_model_descrs_result):
    """Проверяем что на выходе мап-редьюс операции в таблицу с micro_model_description-стримом в колонку text
       попадают micro_model описания моделей (если офферы не являются фейковыми мскушными офферами)
       значение part соответствует исходному шарду оффера
    """

    assert_that(
        list(micro_model_descrs_result.data),
        contains_only([
            {'ware_md5': 'WareId_5', 'region_id': 225L, 'text': 'micro_model_descr:размеры (ШхГхВ): 60x60x82 см', 'value': '1', 'part': 0L},
            {'ware_md5': 'WareId_11_1', 'region_id': 225L, 'text': 'micro_model_descr:цвет: белый', 'value': '1', 'part': 0L},
            {'ware_md5': 'WareId_11_2', 'region_id': 225L, 'text': 'micro_model_descr:цвет: белый', 'value': '1', 'part': 1L},
        ]),
        "No expected micro model descriptions")


def test_cpa_queries_streams(cpa_queries_result):
    """Проверяем что на выходе мап-редьюс операции в таблицу с cpa_queries-стримом в колонку text
       попадают cpa запросы (если офферы не являются фейковыми мскушными офферами)
       значение part соответствует исходному шарду оффера
    """

    assert_that(
        list(cpa_queries_result.data),
        contains_only([
            {'ware_md5': 'WareId_NoModelData', 'region_id': 225L, 'text': 'запрос 1', 'value': '1', 'part': 0L},
            {'ware_md5': 'WareId_11_1', 'region_id': 225L, 'text': 'запрос 2', 'value': '1', 'part': 0L},
            {'ware_md5': 'WareId_11_2', 'region_id': 225L, 'text': 'запрос 2', 'value': '1', 'part': 1L},
        ]),
        "No expected cpa queries")
