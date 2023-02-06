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
def models(category):
    return [
        ExportReportModel(id=7956408,
                          parent_id=0,
                          category_id=category.hid,
                          vendor_id=152888,
                          published_on_market=True,
                          current_type='GURU',
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
        ExportReportModel(id=7956409,
                          parent_id=7956408,
                          category_id=category.hid,
                          vendor_id=152888,
                          published_on_market=True,
                          current_type='GURU',
                          created_date=1333742400000,
                          group_size=0,
                          micro_model_search='для фрирайда анатомический',
                          titles=[LocalizedString(isoCode='ru', value='Scott Air 25 green/black'),
                                  LocalizedString(isoCode='kz', value='ABCD green/black')],
                          aliases=[LocalizedString(isoCode='ru', value='Очень хороший девайс'),
                                   LocalizedString(isoCode='ru', value='Ну, очень хороший девайс')],
                          pictures=[
                              Picture(
                                  xslName="XL-Picture_3",
                                  url="//xl_picture/2_3",
                                  width=2003,
                                  height=2003,
                              ),
                              Picture(
                                  xslName="XL-Picture",
                                  url="//xl_picture/2_1",
                                  width=2001,
                                  height=2001,
                              ),
                              Picture(
                                  xslName="XL-Picture_2",
                                  url="//xl_picture/2_2",
                                  width=2002,
                                  height=2002,
                              )],
                          parameter_values=[
                              ParameterValue(
                                  xsl_name="VendorMinPublishTimestamp",
                                  str_value=[
                                      LocalizedString(isoCode='ru', value='1546473600')
                                  ]
                              )],
                          ),
        ExportReportModel(id=1,
                          parent_id=7956408,
                          category_id=category.hid,
                          vendor_id=152888,
                          published_on_market=True,
                          current_type='GURU',
                          created_date=1333742400000,
                          group_size=0,
                          micro_model_search='для фрирайда анатомический',
                          titles=[LocalizedString(isoCode='ru', value='Scott Air 25 green/black'),
                                  LocalizedString(isoCode='kz', value='ABCD green/black')],
                          aliases=[LocalizedString(isoCode='ru', value='Очень хороший девайс'),
                                   LocalizedString(isoCode='ru', value='Ну, очень хороший девайс')],
                          pictures=[
                              Picture(
                                  xslName="XL-Picture_2",
                                  url="//xl_picture/3",
                                  width=400,
                                  height=400,
                              )],
                          ),
    ]


@pytest.fixture(scope='module')
def mbo_models_table(yt_server, models):
    table = YtTableResource(
        yt_server,
        "//home/mbo/export/models",
        data=[
            dict(
                category_id=m.category_id,
                vendor_id=m.vendor_id,
                model_id=m.id,
                parent_id=m.parent_id,
                source_type="",
                current_type=m.current_type,
                sku_parent_model_id=0,  # m.sku_parent_model_id,
                is_sku=False,
                title=str(m.titles[0].value),
                published=m.published_on_market,
                blue_published=m.published_on_blue_market,
                created_date=str(m.created_date),
                deleted_date=None,
                data=m.SerializeToString(),
                pic='',
            ) for m in models
        ]
    )

    table.dump()

    return table


@pytest.yield_fixture(scope='module')
def workflow(yt_server, mbo_models_table):
    resources = {}

    with YtDataUploadTestEnv(**resources) as env:
        env.execute(
            yt_server,
            type='model_yt',
            output_table="//home/test/models",
            input_table=mbo_models_table.get_path()
        )
        env.verify()
        yield env


@pytest.fixture(scope='module')
def result_yt_table(workflow):
    return workflow.outputs.get('result_table')


def test_result_table_exist(result_yt_table, yt_server):
    assert_that(yt_server.get_yt_client().exists(result_yt_table.get_path()), 'Table exist')


def test_result_table_schema(result_yt_table):
    result_list = extract_column_attributes(list(map(dict, result_yt_table.schema)))
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


def test_result_table_row_count(result_yt_table, models):
    assert_that(len(result_yt_table.data), equal_to(len(models)), "Rows count equal count of models in file")


def test_model_with_sku_xl_picture(result_yt_table, models):
    """ Проверка наличия метаинформации для XL-Picture с url заданного формата
    """
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

    model = models[0]
    yt_row = result_yt_table.data[1]  # ordered by model_id

    expected = {
        "title": equal_to(model.titles[0].value),
        "picture": equal_to(model.pictures[0].url)
    }

    assert_that(yt_row,
                has_entries(expected),
                'Model XL-Picture is the same as the given')

    expected_pic = MetaPicture(width=100,
                               height=100,
                               group_id=123456,
                               namespace='mpic',
                               imagename='img_id654321')
    assert_that(get_pictures(yt_row['pic']),
                equal_to([('img_id654321', expected_pic)]),
                'Model picture is correct')


def test_model_with_xl_picture_and_additional_pictures(result_yt_table, models):
    """ Если в описании модели в поле pictures указаны XL-Picture и XL-Picture_2,
        то всегда выбирается только главная (XL-Picture). Порядок не важен.
    """
    model = models[1]
    yt_row = result_yt_table.data[2]  # ordered by model_id

    expected = {
        "title": equal_to(model.titles[0].value),
        "picture": equal_to(model.pictures[1].url),
        "pic": equal_to('')   # actualy undef
    }

    assert_that(yt_row,
                has_entries(expected),
                'Model XL-Picture in parameter is the same as the given')


def test_model_with_only_additional_pictures(result_yt_table, models):
    """ Даже если в описании модели есть XL-Picture_2 (дополнительная), но нет XL-Picture (главной),
        то информация о картинке не экспортируется
    """
    model = models[2]
    yt_row = result_yt_table.data[0]  # ordered by model_id

    expected = {
        "title": equal_to(model.titles[0].value),
        "picture": equal_to(None),
        "pic": equal_to(None)
    }

    assert_that(yt_row,
                has_entries(expected),
                'Model only with additional pictures exports empty pictrue')


def test_check_yt_order(result_yt_table):
    model_ids = [row['id'] for row in result_yt_table.data]
    assert_that(model_ids,
                equal_to(sorted(model_ids)),
                'Models are sorted by id')


def _should_see_aliases(yt_table_data, model):
    aliases = "".join(['"%s" ; ' % alias.value for alias in model.aliases])

    expected = {"title": equal_to(model.titles[0].value),
                "aliases": equal_to(aliases)}

    assert_that(yt_table_data,
                has_entries(expected),
                'Model with aliases in parameter is the same as the given')


def test_model_with_aliases(result_yt_table, models):
    _should_see_aliases(result_yt_table.data[2], models[1])


def test_model_with_empty_aliases(result_yt_table, models):
    _should_see_aliases(result_yt_table.data[1], models[0])


def test_seo_template(result_yt_table):
    assert(result_yt_table.data[1]['typePrefix'] == "")


def test_model_has_vendor_min_publish_timestamp_absent(result_yt_table, models):
    assert_that(result_yt_table.data[0],
                has_entries({"vendor_min_publish_timestamp": equal_to(0)}),
                "should be zero if not specified in ExportReportModel")


def test_model_vendor_min_publish_timestamp_incorrect_row(result_yt_table, models):
    assert_that(result_yt_table.data[1],
                has_entries({"vendor_min_publish_timestamp": equal_to(0)}),
                "should be zero if specified as non-uint64")


def test_model_vendor_min_publish_timestamp_converting(result_yt_table, models):
    convertedTimestamp = (int)(models[1].parameter_values[0].str_value[0].value)
    assert_that(result_yt_table.data[2],
                has_entries({"vendor_min_publish_timestamp": equal_to(convertedTimestamp)}),
                "incorrect convertion")
