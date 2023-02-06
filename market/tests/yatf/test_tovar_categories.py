# coding=utf-8
import pytest

from hamcrest import assert_that, equal_to


from market.idx.tools.market_yt_data_upload.yatf.test_env import YtDataUploadTestEnv
from market.idx.yatf.resources.tovar_tree_pb import TovarTreePb, MboCategory

from mapreduce.yt.python.table_schema import extract_column_attributes


@pytest.fixture(scope="module")
def tovar_categories():
    return [
        MboCategory(hid=90401, tovar_id=0, no_search=True,
                    name='Все товары', unique_name='Все товары',
                    output_type=MboCategory.SIMPLE, aliases=['Все товары']),

        MboCategory(hid=198118, tovar_id=1523, parent_hid=90401,
                    name='Бытовая техника', unique_name='Бытовая техника',
                    output_type=MboCategory.SIMPLE, aliases=['Побутова техніка']),

        MboCategory(hid=90579, tovar_id=188, parent_hid=198118,
                    name='Мелкая техника для кухни', unique_name='Мелкая техника для кухни',
                    output_type=MboCategory.SIMPLE,
                    aliases=['Мелкая кухонная техника',
                             'Дрібна техніка для кухні',
                             'Дрібна кухонна техніка',
                             ]),

        MboCategory(hid=12327609, tovar_id=24870, parent_hid=90579,
                    name='Кухонные приборы для приготовления блюд',
                    unique_name='Приготовление блюд',
                    output_type=MboCategory.SIMPLE, aliases=['Приготування страв']),

        MboCategory(hid=90591, tovar_id=251, nids=[111], parent_hid=12327609,
                    name='Тостеры', unique_name='Тостеры',
                    output_type=MboCategory.VISUAL),

        MboCategory(hid=90592, tovar_id=252, parent_hid=90591,
                    name='Кухонные весы', unique_name='Кухонные весы',
                    output_type=MboCategory.GURU,
                    aliases=['Весы', 'Кухонні ваги', 'Ваги']),
        # blue nid is primary
        MboCategory(hid=198119, tovar_id=1524, parent_hid=90401,
                    name='Электроника', unique_name='Электроника',
                    output_type=MboCategory.GURULIGHT),
        # nid is different than blue nid. There is no any primary nid but some blue nids
        MboCategory(hid=7815034, tovar_id=21127, parent_hid=90401,
                    name='Сандалии', unique_name='Сандалии для мальчиков',
                    output_type=MboCategory.VISUAL,),
        # no any blue nid! So, blue_nid will be None
        MboCategory(hid=90402, tovar_id=1, parent_hid=90401,
                    name='Авто', unique_name='Товары для авто- и мототехники',
                    output_type=MboCategory.GURULIGHT,)
    ]


@pytest.yield_fixture(scope="module")
def workflow(yt_server, tovar_categories):
    resources = {
        "categories": TovarTreePb(tovar_categories)
    }

    with YtDataUploadTestEnv(**resources) as env:
        env.execute(yt_server, type='categories', output_table="//home/test/categories")
        env.verify()
        yield env


@pytest.fixture(scope="module")
def result_yt_table(workflow):
    return workflow.outputs.get('result_table')


def test_result_table_exist(result_yt_table, yt_server):
    assert_that(yt_server.get_yt_client().exists(result_yt_table.get_path()), 'Table exist')


def test_result_table_schema(result_yt_table):
    assert_that(extract_column_attributes(list(result_yt_table.schema)),
                equal_to([
                    {'required': False, "name": "hyper_id", "type": "int64"},
                    {'required': False, "name": "id", "type": "int64"},
                    {'required': False, "name": "name", "type": "string"},
                    {'required': False, "name": "uniq_name", "type": "string"},
                    {'required': False, "name": "parent", "type": "int64"},
                    {'required': False, "name": "parents", "type": "string"},
                    {'required': False, "name": "type", "type": "string"},
                    {'required': False, "name": "no_search", "type": "boolean"},
                    {'required': False, "name": "visual", "type": "boolean"},
                    {'required': False, "name": "nid", "type": "int64"},
                    {'required': False, "name": "blue_nid", "type": "int64"},
                ]), "Schema is incorrect")


def test_result_table_row_count(result_yt_table):
    assert_that(len(result_yt_table.data), equal_to(9), "Rows count equal count of categories in file")


def test_root_category(result_yt_table):
    root = result_yt_table.data[0]
    # <node id="75701" parent_id="0" position="0" name="Все товары" unique_name="Все товары" hid="90401"
    assert_that(root, equal_to({'hyper_id': 90401,
                                'id': 0,
                                'nid': None,
                                'blue_nid': 75701,
                                'name': "Все товары",
                                'uniq_name': "Все товары",
                                'parent': 0,
                                'parents': '90401,',
                                'type': 'simple',
                                'no_search': True,
                                'visual': False}), 'Root category is the same as the given')


def test_visual_category(result_yt_table):
    visual_category = result_yt_table.data[7]
    # <node id="82891" parent_id="79856" position="9" name="Тостеры" unique_name="Тостеры" hid="90591"
    assert_that(visual_category, equal_to({'hyper_id': 90591,
                                           'id': 251,
                                           'nid': 111,
                                           'blue_nid': 82891,
                                           'name': "Тостеры",
                                           'uniq_name': "Тостеры",
                                           'parent': 12327609,
                                           'parents': '90591,12327609,90579,198118,90401,',
                                           'type': 'cluster',
                                           'no_search': False,
                                           'visual': True}), 'Visual category is correct')


def test_leaf_category(result_yt_table):
    leaf = result_yt_table.data[8]
    # <node id="79870" parent_id="79861" position="1" name="Кухонные весы" unique_name="Кухонные весы" hid="90592"
    assert_that(leaf, equal_to({'hyper_id': 90592,
                                'id': 252,
                                'nid': None,
                                'blue_nid': 79870,
                                'name': "Кухонные весы",
                                'uniq_name': "Кухонные весы",
                                'parent': 90591,
                                'parents': '90592,90591,12327609,90579,198118,90401,',
                                'type': 'guru',
                                'no_search': False,
                                'visual': False}), 'Last category is correct')


# Check that primary blue nid will be taken
def test_primary_blue_nid(result_yt_table):
    leaf = result_yt_table.data[2]
    # <node id="80155" parent_id="75701" position="1" name="Электроника" unique_name="Электроника" hid="198119" >
    assert_that(leaf, equal_to({'hyper_id': 198119,
                                'id': 1524,
                                'nid': None,
                                'blue_nid': 80155,
                                'name': "Электроника",
                                'uniq_name': "Электроника",
                                'parent': 90401,
                                'parents': '198119,90401,',
                                'type': 'gurulight',
                                'no_search': False,
                                'visual': False}), 'Category isn\'t the same as the given')


# Check situation when no primary blue nid is provided. Will be taken one of blue nids
def test_not_primary_blue_nid(result_yt_table):
    leaf = result_yt_table.data[4]
    # <node id="77695" parent_id="77689"  name="Сандалии" unique_name="Сандалии для мальчиков" hid="7815034" />
    assert_that(leaf, equal_to({'hyper_id': 7815034,
                                'id': 21127,
                                'nid': None,
                                'blue_nid': 77695,
                                'name': "Сандалии",
                                'uniq_name': "Сандалии для мальчиков",
                                'parent': 90401,
                                'parents': '7815034,90401,',
                                'type': 'cluster',
                                'no_search': False,
                                'visual': True}), 'Category isn\'t the same as the given')


# No any blue nids are provided. Check that blue_nid is Null
def test_no_any_blue_nid(result_yt_table):
    leaf = result_yt_table.data[3]

    # In navigation tree <node id="72672" parent_id="72660" name="Сандалии" hid="7815034" is_primary="0" is_hidden="0" is_green="1" is_blue="1"/>

    assert_that(leaf, equal_to({'hyper_id': 90402,
                                'id': 1,
                                'nid': None,
                                'blue_nid': None,
                                'name': "Авто",
                                'uniq_name': "Товары для авто- и мототехники",
                                'parent': 90401,
                                'parents': '90402,90401,',
                                'type': 'gurulight',
                                'no_search': False,
                                'visual': False}), 'Category isn\'t the same as the given')
