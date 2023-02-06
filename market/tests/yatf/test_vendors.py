# coding=utf-8
import pytest

from hamcrest import assert_that, equal_to


from market.idx.tools.market_yt_data_upload.yatf.test_env import YtDataUploadTestEnv
from market.idx.tools.market_yt_data_upload.yatf.resources.global_vendors import GlobalVendors

from mapreduce.yt.python.table_schema import extract_column_attributes


@pytest.fixture(scope='module')
def global_vendors():
    return'''
<global-vendors>
    <vendor id="153043" name="Apple">
      <site>http://www.apple.com/ru</site>
      <picture>//avatars.mds.yandex.net/get-mpic/200316/img_id7529381856577181154/orig</picture>
      <logo-position>0</logo-position>
      <logos>
      <logo type="BRANDZONE">//avatars.mds.yandex.net/get-mpic/195452/img_id8818173109963086273/orig</logo>
      </logos>
      <has-article>false</has-article>
    </vendor>
    <vendor id="722218" name="April Music">
      <site>http://www.aprilmusic.com</site>
      <logo-position>0</logo-position>
      <has-article>false</has-article>
      <is-fake-vendor>false</is-fake-vendor>
    </vendor>
    <vendor id="152884" name="Barco">
      <description>Компания BARCO была основана в 1934 году.
Подразделение компании BARCO Display Systems занимается производством мониторов.
Мониторы BARCO предназначены для издательских и дизайнерских приложений,
для профессиональной цифровой фотографии - всех тех приложений,
где требуется высокое качество передачи цвета.</description>
      <site>http://www.barco.ru</site>
      <logos>
        <logo type="BRANDZONE">//avatars.mds.yandex.net/get-mpic/200316/img_id9183320649835817811/orig</logo>
      </logos>
      <is-fake-vendor>false</is-fake-vendor>
    </vendor>
    <vendor id="152948" name="Arcam">
      <description>ABCD</description>
      <site>http://www.arcam.co.uk</site>
      <picture>//avatars.mds.yandex.net/get-mpic/200316/img_id8485436188906564893/orig</picture>
      <logo-position>0</logo-position>
      <logos>
       <logo type="BRANDZONE">//avatars.mds.yandex.net/get-mpic/200316/img_id5763834793325191272/orig</logo>
      </logos>
      <has-article>false</has-article>
      <is-fake-vendor>true</is-fake-vendor>
    </vendor>
</global-vendors>'''


@pytest.yield_fixture(scope='module')
def workflow(yt_server, global_vendors):
    resources = {
        "vendor": GlobalVendors.from_string(global_vendors)
    }

    with YtDataUploadTestEnv(**resources) as env:
        env.execute(yt_server, type='vendor', output_table="//home/test/vendors")
        env.verify()
        yield env


@pytest.fixture(scope='module')
def result_yt_table(workflow):
    return workflow.outputs.get('result_table')


def test_result_table_exist(result_yt_table, yt_server):
    assert_that(yt_server.get_yt_client().exists(result_yt_table.get_path()), 'Table exist')


def test_result_table_schema(result_yt_table):
    assert_that(extract_column_attributes(list(result_yt_table.schema)),
                equal_to([
                    {'required': False, "name": "id", "type": "int64"},
                    {'required': False, "name": "name", "type": "string"},
                    {'required': False, "name": "site", "type": "string"},
                    {'required': False, "name": "picture", "type": "string"},
                    {'required': False, "name": "description", "type": "string"},
                    {'required': False, "name": "is-fake-vendor", "type": "boolean"},
                ]), "Schema is incorrect")


def test_result_table_row_count(result_yt_table):
    assert_that(len(result_yt_table.data), equal_to(4), "Rows count equal count of vendors in file")


def test_root_vendor(result_yt_table):
    root = result_yt_table.data[0]
    assert_that(
        root,
        equal_to({
            "id": 153043,
            "name": "Apple",
            "site": "http://www.apple.com/ru",
            "picture": "//avatars.mds.yandex.net/get-mpic/200316/img_id7529381856577181154/orig",
            "description": None,
            "is-fake-vendor": False,
        }),
        'First vendor is the same as the given'
    )


def test_large_non_ascii_description(result_yt_table):
    vendor_info = result_yt_table.data[2]
    assert_that(
        vendor_info,
        equal_to({
            "id": 152884,
            "name": "Barco",
            "site": "http://www.barco.ru",
            "picture": None,
            "description": (
                "Компания BARCO была основана в 1934 году.\n"
                "Подразделение компании BARCO Display Systems занимается производством мониторов.\n"
                "Мониторы BARCO предназначены для издательских и дизайнерских приложений,\n"
                "для профессиональной цифровой фотографии - всех тех приложений,\n"
                "где требуется высокое качество передачи цвета."
            ),
            "is-fake-vendor": False,
        }),
        'Large non-ascii description is the same as the given')


def test_leaf_category(result_yt_table):
    leaf = result_yt_table.data[3]
    assert_that(
        leaf,
        equal_to({
            "id": 152948,
            "name": "Arcam",
            "site": "http://www.arcam.co.uk",
            "picture": "//avatars.mds.yandex.net/get-mpic/200316/img_id8485436188906564893/orig",
            "description": "ABCD",
            "is-fake-vendor": True,
        }),
        'Last vendor is the same as the given'
    )
