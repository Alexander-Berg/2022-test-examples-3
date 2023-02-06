# coding=utf-8
import pytest

from hamcrest import assert_that, equal_to


from market.idx.streams.src.prepare_additional_model_streams.yatf.test_env import YtAdditionalModelStreamsTestEnv
from market.idx.yatf.resources.yt_table_resource import YtTableResource

from mapreduce.yt.python.table_schema import extract_column_attributes

"""
    Тест проверяет YT джобу, которая подготавливает из таблички со сгенеренными из мбо-шаблонов, конкретных
    параметров модели и названий параметров таблички со стримами (пока только micro_model_descriptions стримы).
    Конечная таблица -- это таблица с идентификатором, чтобы можно было добавить стрим к вебоским данным
    (в нашем случае model_id),
    и информация о стриме: text = alias, регион и вес стрима.
"""


@pytest.fixture(scope='module')
def rendered_descriptions_table(yt_stuff):
    data = [
        dict(model_id=13, micro_model_rendered_descr="111111"),
        dict(model_id=1313, micro_model_rendered_descr="2222222"),
        dict(model_id=131313, micro_model_rendered_descr="33333"),
        dict(model_id=77, micro_model_rendered_descr="44444"),
    ]
    table = YtTableResource(yt_stuff, "//home/in/rendered_descriptions", data)
    table.dump()

    return table


@pytest.yield_fixture(scope='module')
def workflow(yt_stuff, rendered_descriptions_table):
    resources = {}

    with YtAdditionalModelStreamsTestEnv(**resources) as env:
        env.execute(yt_stuff, rendered_descriptions_table.get_path(), "//home/streams/micro_model_descr")
        env.verify()
        yield env


@pytest.fixture(scope='module')
def micro_model_descr_yt_table(workflow):
    return workflow.outputs.get('micro_model_descr_table')


def test_micro_model_descr_table_exist(micro_model_descr_yt_table, yt_stuff):
    assert_that(yt_stuff.get_yt_client().exists(micro_model_descr_yt_table.get_path()), 'Table doesn\'t exist')


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


def test_micro_model_descr_table_schema(micro_model_descr_yt_table):
    check_result_table_schema(micro_model_descr_yt_table)


def test_micro_model_descr_streams(micro_model_descr_yt_table):
    """Проверяем что на выходе мап-редьюс операции в таблицу с micro_model_descr-стримом в колонку text попадают
       соотв. описания моделей (если они не пустые)"""
    assert_that(
        list(micro_model_descr_yt_table.data),
        equal_to([
            {'model_id': '13', 'region_id': 225, 'url': 'market.yandex.ru/product/13', 'text': '111111', 'value': '1', 'part': 0},
            {'model_id': '1313', 'region_id': 225, 'url': 'market.yandex.ru/product/1313', 'text': '2222222', 'value': '1', 'part': 0},
            {'model_id': '131313', 'region_id': 225, 'url': 'market.yandex.ru/product/131313', 'text': '33333', 'value': '1', 'part': 0},
            {'model_id': '77',  'region_id': 225, 'url': 'market.yandex.ru/product/77',  'text': '44444', 'value': '1', 'part': 0},
        ]),
        "No expected aliases")
