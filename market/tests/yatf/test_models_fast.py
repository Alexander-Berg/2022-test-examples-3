# coding=utf-8
import pytest

from hamcrest import assert_that, equal_to, has_entries

from market.proto.content.mbo.ExportReportModel_pb2 import (
    ExportReportModel,
    LocalizedString,
    ParameterValue,
)
from market.proto.content.mbo.MboParameters_pb2 import Category, Word

from market.idx.tools.market_yt_data_upload.yatf.test_env import YtDataUploadTestEnv
from market.idx.tools.market_yt_data_upload.yatf.resources.models_pb import ModelsPb
from market.idx.tools.market_yt_data_upload.yatf.resources.parameters_pb import ParametersPb

from mapreduce.yt.python.table_schema import extract_column_attributes


MAX_FULL_DESCR_LENGTH = 512


@pytest.fixture(scope='module')
def category():
    return Category(hid=91522,
                    name=[Word(name='Бетономешалки')])


@pytest.fixture(scope='module')
def models(category):
    return [
        ExportReportModel(
            id=2441135,
            category_id=category.hid,
            published_on_market=True,
            current_type='GURU',
        ),
        ExportReportModel(
            id=2441136,
            category_id=category.hid,
            published_on_market=True,
            current_type='GURU',
            parameter_values=[
                ParameterValue(
                    param_id=15341921,
                    xsl_name='description',
                    str_value=[
                        LocalizedString(value='Какое описание выбрать?', isoCode='ru'),
                        LocalizedString(value='Нам сказали, выбирать самое длинное описание!', isoCode='ru'),
                        LocalizedString(value='Use only russian descriptions, even if it is shortest', isoCode='en'),
                    ],
                ),
            ],
        ),
    ]


def get_resources(models, category):
    return {
        'model_fast': ModelsPb(models, category.hid, 'models'),
        'parameters': ParametersPb(category),
    }


@pytest.yield_fixture(scope='module')
def workflow(yt_server, models, category):
    resources = get_resources(models, category)
    with YtDataUploadTestEnv(**resources) as env:
        env.execute(yt_server, type="model_fast", output_table="//home/test/models_fast")
        env.verify()
        yield env


@pytest.fixture(scope='module')
def result_yt_table(workflow):
    return workflow.outputs.get('result_table')


def test_result_table_exist(result_yt_table, yt_server):
    assert_that(yt_server.get_yt_client().exists(result_yt_table.get_path()), 'Table exist')


def test_result_table_schema(result_yt_table):
    result_list = extract_column_attributes(list(result_yt_table.schema))
    result_list.sort(key=lambda x: x['name'])
    expected_list = [
        {'required': False, "name": "id", "type": "int64", "sort_order": "ascending"},
        {'required': False, "name": "parent_id", "type": "int64"},
        {'required': False, "name": "category_id", "type": "int64"},
        {'required': False, "name": "vendor_id", "type": "int64"},
        {'required': False, "name": "source_type", "type": "string"},
        {'required': False, "name": "current_type", "type": "string"},
        {'required': False, "name": "created_date", "type": "uint64"},
        {'required': False, "name": "deleted", "type": "boolean"},
        {'required': False, "name": "experiment_flag", "type": "string"},
        {'required': False, "name": "group_size", "type": "int64"},
        {'required': False, "name": "micro_model_search", "type": "string"},
        {'required': False, "name": "modified_ts", "type": "uint64"},
        {'required': False, "name": "title", "type": "string"},
        {'required': False, "name": "picture", "type": "string"},
        {'required': False, "name": "pic", "type": "string"},
        {'required': False, "name": "aliases", "type": "string"},
        {'required': False, "name": "published_on_market", "type": "boolean"},
        {'required': False, "name": "published_on_blue_market", "type": "boolean"},
        {'required': False, "name": "full_description", "type": "string"},
        {'required': False, "name": "export_ts", "type": "uint64"},
        {'required': False, 'name': 'archived', 'type': 'boolean'},
        {'required': False, 'name': 'archived_date', 'type': 'uint64'},
        {'required': False, 'name': 'article', 'type': 'string'},
        {'required': False, 'name': 'blue_published', 'type': 'boolean'},
        {'required': False, 'name': 'check_date', 'type': 'uint64'},
        {'required': False, 'name': 'checked', 'type': 'boolean'},
        {'required': False, 'name': 'clusterizer_debug_info', 'type': 'string'},
        {'required': False, 'name': 'clusterizer_developer_properties', 'type': 'string'},
        {'required': False, 'name': 'created_user_id', 'type': 'int64'},
        {'required': False, 'name': 'deleted_date', 'type': 'uint64'},
        {'required': False, 'name': 'doubtful', 'type': 'boolean'},
        {'required': False, 'name': 'expired_date', 'type': 'uint64'},
        {'required': False, 'name': 'group_model_id', 'type': 'int64'},
        {'required': False, 'name': 'modified_user_id', 'type': 'int64'},
        {'required': False, 'name': 'published', 'type': 'boolean'},
        {'required': False, 'name': 'published_blue_date', 'type': 'uint64'},
        {'required': False, 'name': 'published_white_date', 'type': 'uint64'},
        {'required': False, 'name': 'shop_count', 'type': 'int64'},
        {'required': False, 'name': 'source', 'type': 'int64'},
        {'required': False, 'name': 'source_id', 'type': 'string'},
        {'required': False, 'name': 'source_yang_task_type', 'type': 'int64'},
        {'required': False, 'name': 'supplier_id', 'type': 'int64'},
        {'required': False, 'name': 'cs_gumoful', 'type': 'string'}
    ]
    expected_list.sort(key=lambda x: x['name'])
    assert_that(result_list, equal_to(expected_list), "Schema is incorrect")


def test_result_table_row_count(result_yt_table, models):
    assert_that(len(result_yt_table.data), equal_to(len(models)), "Rows count equal count of models in file")


def test_check_yt_order(result_yt_table):
    model_ids = [row['id'] for row in result_yt_table.data]
    assert_that(model_ids,
                equal_to(sorted(model_ids)),
                'Models are sorted by id')


def test_model_without_full_descriptions(result_yt_table, models):
    expected = {
        "id": 2441135,
        "full_description": None,
    }
    assert_that(result_yt_table.data[0],
                has_entries(expected),
                'Model without fullDescription')


def test_model_with_full_descriptions(result_yt_table, models):
    expected = {
        "id": 2441136,
        "full_description": "Нам сказали, выбирать самое длинное описание!",
    }
    assert_that(result_yt_table.data[1],
                has_entries(expected),
                'Model with fullDescription')
