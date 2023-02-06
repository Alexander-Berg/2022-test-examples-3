# coding=utf-8
import pytest

from hamcrest import assert_that, equal_to, has_entries

from market.proto.content.mbo.ExportReportModel_pb2 import (
    ExportReportModel,
    LocalizedString,
    ParameterValue,
)
from market.proto.content.mbo.MboParameters_pb2 import Category, Word, Parameter, Option
from market.idx.yatf.resources.yt_table_resource import YtTableResource
from market.idx.tools.market_yt_data_upload.yatf.test_env import YtDataUploadTestEnv
from mapreduce.yt.python.table_schema import extract_column_attributes


@pytest.fixture(scope='module')
def categories():
    return [
        Category(
            hid=91522,
            name=[Word(name='Моторные автомасла')],
            parameter=[
                Parameter(
                    id=12694870,
                    value_type=5,
                    xsl_name="ACEAStandard",
                    name=[Word(name='Стандарт ACEA')],
                    published=True,
                ),
                Parameter(
                    id=12677993,
                    value_type=2,
                    xsl_name="MainType",
                    name=[Word(name='Тип')],
                    published=True,
                    option=[
                        Option(id=12678030, name=[Word(name='минеральное')], published=True),
                        Option(id=12678029, name=[Word(name='синтетическое')], published=True),
                    ]
                ),
                Parameter(
                    id=12670380,
                    value_type=3,
                    xsl_name="Volume",
                    published=True,
                    name=[Word(name='Объем упаковки')],
                )
            ],
            micro_model_template='''<ya_guru_modelcard>
<block name=\"Технические характеристики\">
<![CDATA[{MainType#ifnz}{MainType} {#endif}моторное масло{Volume#ifnz}, {Volume}л{#endif}{ACEAStandard#ifnz}, класс ACEA {ACEAStandard}{#endif}]]>
</block>
</ya_guru_modelcard>'''
        ),
        Category(
            hid=91523,
            name=[Word(name='Категория без micro_model_template')],
        )
    ]


@pytest.fixture(scope='module')
def models():
    return [
        ExportReportModel(id=7956408,
                          parent_id=0,
                          category_id=91522,
                          published_on_blue_market=True,
                          parameter_values=[
                              ParameterValue(param_id=12694870, value_type=5, xsl_name="ACEAStandard", str_value=[LocalizedString(value='стандартное')]),
                              ParameterValue(param_id=12677993, value_type=2, xsl_name="MainType", option_id=12678029),
                              ParameterValue(param_id=12670380, value_type=3, xsl_name="Volume", numeric_value="344.9"),
                          ]),
        ExportReportModel(id=7956409,    # will be drop because published_on_blue_market=false and no published_on_market
                          parent_id=0,
                          category_id=91522,
                          published_on_blue_market=False,
                          parameter_values=[
                              ParameterValue(param_id=12694870, value_type=5, xsl_name="ACEAStandard", str_value=[LocalizedString(value='стандартное')]),
                              ParameterValue(param_id=12677993, value_type=2, xsl_name="MainType", option_id=12678029),
                              ParameterValue(param_id=12670380, value_type=3, xsl_name="Volume", numeric_value="344.9"),
                          ]),
        ExportReportModel(id=7956410,    # will be drop because no micro_model_template in category
                          parent_id=0,
                          category_id=91523,
                          published_on_blue_market=True,
                          ),
        ExportReportModel(id=7956411,    # will be drop because no category data
                          parent_id=0,
                          category_id=91524,
                          published_on_blue_market=True,
                          ),
    ]


@pytest.fixture(scope='module')
def mbo_models_path():
    return "//home/test/mbo_export_models"


@pytest.fixture(scope='module')
def mbo_export_models_table(yt_server, mbo_models_path, models):

    schema = [
        dict(name="model_id", type="int64"),
        dict(name="category_id", type="int64"),
        dict(name="blue_published", type="boolean"),
        dict(name="data", type="string"),
    ]
    rows = [
        dict(
            model_id=model.id,
            category_id=model.category_id,
            data=model.SerializeToString()
        ) for model in models
    ]

    table = YtTableResource(yt_server, mbo_models_path, data=rows, attributes={'schema': schema})
    table.dump()
    return table


@pytest.fixture(scope='module')
def mbo_categories_path():
    return "//home/test/mbo_export_categories"


@pytest.fixture(scope='module')
def mbo_export_categories_table(yt_server, mbo_categories_path, categories):

    schema = [
        dict(name="hid", type="int64"),
        dict(name="data", type="string"),
    ]
    rows = [
        dict(
            hid=category.hid,
            data=category.SerializeToString()
        ) for category in categories
    ]

    table = YtTableResource(yt_server, mbo_categories_path, data=rows, attributes={'schema': schema})
    table.dump()
    return table


@pytest.yield_fixture(scope='module')
def workflow(yt_server, mbo_export_models_table, mbo_export_categories_table):
    with YtDataUploadTestEnv() as env:
        env.execute(yt_server, type="mbo_rendered_description", input_table=mbo_export_models_table.get_path(),
                    second_input_table=mbo_export_categories_table.get_path(), output_table="//home/test/descrs")
        env.verify()
        yield env


@pytest.fixture(scope='module')
def result_yt_table(workflow):
    return workflow.outputs.get('result_table')


def test_result_table_exist(result_yt_table, yt_server):
    assert_that(yt_server.get_yt_client().exists(result_yt_table.get_path()), 'Table exist')


def test_result_table_schema(result_yt_table):
    result_list = extract_column_attributes(list(result_yt_table.schema))
    expected_list = [
        {'required': False, "name": "model_id", "type": "int64", "sort_order": "ascending"},
        {'required': False, "name": "category_id", "type": "int64"},
        {'required': False, "name": "micro_model_rendered_descr", "type": "string"},
    ]
    assert_that(result_list, equal_to(expected_list), "Schema is incorrect")


def test_result_table_row_count(result_yt_table):
    """Проверяем, что в выходной таблице только одна модель, остальные выкинуты по разным причинам
    """
    assert_that(len(result_yt_table.data), equal_to(1), "Rows count equal count of models in file")


def test_model_micro_model_description(result_yt_table, models):
    """Проверяем, что у первой модели сгенерилось правильное описание
    """
    expected_micro_model_descr = '''синтетическое моторное масло, 344.9л, класс ACEA стандартное'''

    yt_row = result_yt_table.data[0]
    expected = {
        "micro_model_rendered_descr": expected_micro_model_descr
    }

    assert_that(yt_row,
                has_entries(expected),
                'Model micro_model_rendered_descr is the same as the given')
