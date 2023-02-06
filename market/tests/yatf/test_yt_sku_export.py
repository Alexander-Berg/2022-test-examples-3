# coding=utf-8
import pytest

from hamcrest import assert_that, equal_to, has_entries, has_item, all_of

from market.proto.content.mbo.ExportReportModel_pb2 import (
    ExportReportModel,
    LocalizedString,
    ParameterValue,
    Picture
)
from market.proto.content.mbo.MboParameters_pb2 import Category, Word
from market.proto.content.pictures_pb2 import Picture as MetaPicture

from market.idx.tools.market_yt_data_upload.yatf.test_env import YtDataUploadTestEnv
from market.idx.yatf.resources.yt_table_resource import YtTableResource

from mapreduce.yt.python.table_schema import extract_column_attributes


@pytest.fixture(scope='module')
def seo_template():
    return 'Губозакатательная машина'


@pytest.fixture(scope='module')
def category(seo_template):
    return Category(hid=91522,
                    name=[Word(name='Бетономешалки')],
                    seo_template='''<lingua>
                                        <type nominative='{0}' genitive='{0}' dative='{0}' accusative='{0}' />
                                  </lingua>'''.format(seo_template))


@pytest.fixture(scope='module')
def skus(category):
    return [
        ExportReportModel(id=7956408,
                          parent_id=0,
                          category_id=category.hid,
                          vendor_id=152888,
                          published_on_market=True,
                          current_type='SKU',
                          created_date=1333742400000,
                          group_size=2,
                          micro_model_search='для фрирайда анатомический',
                          titles=[LocalizedString(isoCode='ru', value='Scott Air 25'),
                                  LocalizedString(isoCode='kz', value='ABCD')],
                          aliases=[],
                          pictures=[
                              Picture(
                                  xslName="XL-Picture",
                                  url="//avatars.mds.yandex.net/get-mpic/123456/img_id654321/orig",
                                  width=100,
                                  height=100,
                              )],
                          parameter_values=[
                              ParameterValue(
                                  xsl_name="VendorMinPublishTimestamp",
                                  str_value=[
                                      LocalizedString(isoCode='ru', value='whynotbadstring')
                                  ]
                              )],
                          ),
    ]


@pytest.fixture(scope='module')
def mbo_sku_table(yt_server, skus):
    table = YtTableResource(
        yt_server,
        "//home/mbo/export/sku",
        data=[
            dict(
                category_id=sku.category_id,
                vendor_id=sku.vendor_id,
                model_id=sku.id,
                parent_id=sku.parent_id,
                source_type="",
                current_type=sku.current_type,
                sku_parent_model_id=0,  # m.sku_parent_model_id,
                is_sku=True,
                title=str(sku.titles[0].value),
                published=sku.published_on_market,
                blue_published=sku.published_on_blue_market,
                created_date=str(sku.created_date),
                deleted_date=None,
                data=sku.SerializeToString(),
                pic='',
            ) for sku in skus
        ]
    )

    return table


@pytest.yield_fixture(scope='module')
def workflow(yt_server, mbo_sku_table):
    resources = {
        'mbo_sku_table': mbo_sku_table
    }

    with YtDataUploadTestEnv(**resources) as env:
        env.execute(
            yt_server,
            type="sku_export_yt",
            output_table="//home/test/sku_export",
            input_table=mbo_sku_table.get_path()
        )
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
        {'required': False, "name": "id", "type": "int64", "sort_order": "ascending"},
        {'required': False, "name": "parent_id", "type": "int64"},
        {'required': False, "name": "category_id", "type": "int64"},
        {'required': False, "name": "vendor_id", "type": "int64"},
        {'required': False, "name": "current_type", "type": "string"},
        {'required': False, "name": "created_date", "type": "string"},
        {'required': False, "name": "experiment_flag", "type": "string"},
        {'required': False, "name": "group_size", "type": "int64"},
        {'required': False, "name": "micro_model_search", "type": "string"},
        {'required': False, "name": "title", "type": "string"},
        {'required': False, "name": "picture", "type": "string"},
        {'required': False, "name": "pic", "type": "string"},
        {'required': False, "name": "aliases", "type": "string"},
        {'required': False, "name": "typePrefix", "type": "string"},
        {'required': False, "name": "published_on_market", "type": "boolean"},
        {'required': False, "name": "published_on_blue_market", "type": "boolean"},
        {'required': False, "name": "vendor_min_publish_timestamp", "type": "int64"},
    ]

    column_matchers = [has_item(col) for col in expected_list]
    assert_that(result_list, all_of(*column_matchers), "Schema is incorrect")


def test_result_table_row_count(result_yt_table, skus):
    """Проверка количество строк в выходной таблице"""
    assert_that(len(result_yt_table.data), equal_to(len(skus)), "Rows count equal count of skus in file")


def test_sku_with_sku_xl_picture(result_yt_table, skus):
    """Проверка наличия метаинформации для XL-Picture с url заданного формата"""
    def get_pictures(data):
        from six import BytesIO as StringIO
        from market.pylibrary.lenval_stream import iter_file

        pictures = []
        pic_lenval = StringIO(data)
        for picSerialized in iter_file(pic_lenval):
            pic = MetaPicture()
            pic.ParseFromString(picSerialized)
        pictures.append((pic.imagename, pic))
        return pictures

    sku = skus[0]
    yt_row = result_yt_table.data[0]

    expected = {
        "title": equal_to(sku.titles[0].value),
        "picture": equal_to(sku.pictures[0].url)
    }

    assert_that(yt_row,
                has_entries(expected),
                'Sku XL-Picture is the same as the given')

    expected_pic = MetaPicture(width=100,
                               height=100,
                               group_id=123456,
                               namespace='mpic',
                               imagename='img_id654321')
    assert_that(get_pictures(yt_row['pic']),
                equal_to([('img_id654321', expected_pic)]),
                'Sku picture is correct')


def test_check_yt_order(result_yt_table):
    """Проверка сортировки таблицы"""
    model_ids = [row['id'] for row in result_yt_table.data]
    assert_that(model_ids,
                equal_to(sorted(model_ids)),
                'Models are sorted by id')
