# coding=utf-8
import pytest
import json

from conftest import GENERATION, Offer, Category, Recipe
from market.idx.yatf.utils.utils import assert_rows_set_equal
from market.idx.pylibrary.mindexer_core.banner.banner import WhiteRecipeK50FeedProcessor


# Процессор, который тестируем.
# Не забыть перечислить fixture на все таблицы, которые нужны для процессору.
@pytest.fixture(scope='module')
def test_processor(config,
                   offers_table,
                   models_table,
                   categories_table,
                   vendors_table,
                   recipe_table,
                   yql_executor):
    processor = WhiteRecipeK50FeedProcessor(config=config,
                                            generation=GENERATION)
    processor.override_yql_executor(yql_executor)
    processor.make_result_table()

    return processor

DEFAULT_MBO_PARAMS = json.dumps([
    {
        'id': 123,
        'values': [456]
    },
    {
        'id': 124,
        'values': [457]
    }  # It's ok if offer fits more filtrations than recipe has.
])
DEFAULT_FILTERS = json.dumps([
    {
        'id': [123],
        'values': [{'id': [456]}]
    }
])

NO_VALUE_MBO_PARAMS = json.dumps([
    {
        'id': 444,
    }
])
NO_VALUE_FILTERS = json.dumps([
    {
        'id': [444],
        'name': ['some name']
    }
])

# Every offer filter should be in a recipe filter in order to fit
NO_OFFERS_MBO_PARAMS = json.dumps([
    {
        'id': 111
    }
])

NO_OFFERS_FILTERS = json.dumps([
    {
        'id': [111]
    },
    {
        'id': [222]
    }
])

SPECIAL_OFFERS_DATA =(
    [Offer(ware_md5="1" + str(i), picture_url='https://picture_correct.ru', price="RUR 600000000", vendor_id=0,
           model_id=0, category_id=31337, mbo_params=DEFAULT_MBO_PARAMS) for i in range(5)] +  # all filters pass
    [Offer(ware_md5="2" + str(i), picture_url='https://picture.ru', price="RUR 600000000", vendor_id=0,
           model_id=0, category_id=31339, mbo_params=DEFAULT_MBO_PARAMS) for i in range(5)] +  # nid is null
    [Offer(ware_md5="3" + str(i), picture_url='https://picture.ru', price="RUR 600000000", vendor_id=0,
           model_id=0, category_id=16440100, mbo_params=DEFAULT_MBO_PARAMS) for i in range(5)] +  # not banner allow
    [Offer(ware_md5="4" + str(i), picture_url='https://picture.ru', price="RUR 600000000", vendor_id=0,
           model_id=0, category_id=31341, mbo_params=DEFAULT_MBO_PARAMS) for i in range(5)] +  # category is not guru
    [Offer(ware_md5="5" + str(i), picture_url='https://picture.ru', price="RUR 600000000", vendor_id=242456, model_id=0,
           category_id=31342, mbo_params=DEFAULT_MBO_PARAMS) for i in range(5)] +  # bad vendor
    [Offer(ware_md5="7" + str(i), picture_url=None, price="RUR 600000000", vendor_id=0, model_id=1,
           category_id=31342, mbo_params=DEFAULT_MBO_PARAMS) for i in range(5)] +  # picture is null
    [Offer(ware_md5="8" + str(i), picture_url='https://picture_correct.ru', price="RUR 700000000", vendor_id=0, model_id=0,
           category_id=31337, mbo_params=NO_VALUE_MBO_PARAMS) for i in range(5)] +  # all filters pass
    [Offer(ware_md5="9" + str(i), picture_url='https://picture_correct.ru', price="RUR 800000000", vendor_id=0, model_id=0,
           category_id=31337, mbo_params=NO_OFFERS_MBO_PARAMS) for i in range(5)] +  # mbo params do not pass
    [Offer(ware_md5="10" + str(i), picture_url='https://picture.ru', price="RUR 800000000", vendor_id=0, model_id=0,
           category_id=12345, mbo_params=DEFAULT_MBO_PARAMS) for i in range(4)] +  # not enough offers
    [Offer(ware_md5="11" + str(i), picture_url='https://picture.ru', price="RUR 800000000", vendor_id=0, model_id=0,
           category_id=12346, mbo_params=DEFAULT_MBO_PARAMS) for i in range(5)]  # Wrong hyper_id
)

SPECIAL_CATEGORIES_DATA = [
    Category(hyper_id=31337, nid=10000, uniq_name='Correct category', type='guru', parent=0, parents='31337,0'),
    Category(hyper_id=31338, nid=5000, uniq_name='Incorrect category', type='guru', parent=0, parents='31338,0'),
    Category(hyper_id=31339, nid=None, uniq_name='Incorrect category', type='guru', parent=0, parents='31339,0'),
    Category(hyper_id=16440100, nid=4000, uniq_name='Incorrect category', type='guru', parent=0, parents='16440100,0'),  # bad_parent_category
    Category(hyper_id=31341, nid=3000, uniq_name='Incorrect category', type='gurulight', parent=0, parents='31341,0'),
    Category(hyper_id=31342, nid=2000, uniq_name='Incorrect category', type='guru', parent=0, parents='31342,0'),
    Category(hyper_id=12345, nid=123456, uniq_name='Incorrect category', type='guru', parent=0, parents='12345,0'),
    Category(hyper_id=12346, nid=123457, uniq_name='Incorrect category', type='guru', parent=0, parents='12346,0'),
]

SPECIAL_RECIPE_DATA = [
    Recipe(recipe_id=1, hid=31337, name='small', header='small shoes', filters=DEFAULT_FILTERS),
    Recipe(recipe_id=2, hid=31339, name='small', header='small shoes', filters=DEFAULT_FILTERS),
    Recipe(recipe_id=3, hid=16440100, name='small', header='small shoes', filters=DEFAULT_FILTERS),
    Recipe(recipe_id=4, hid=31342, name='small', header='small shoes', filters=DEFAULT_FILTERS),
    Recipe(recipe_id=5, hid=31337, name='small', header='small shoes', filters=NO_VALUE_FILTERS),
    Recipe(recipe_id=6, hid=31337, name='small', header='small shoes', filters=NO_OFFERS_FILTERS),
    Recipe(recipe_id=7, hid=12345, name='small', header='small shoes', filters=DEFAULT_FILTERS),
]


# Строка выходной таблицы фида.
class FeedRow(object):
    def __init__(self, hyper_id, price, picture_url, model_picture_url, nid, category_name, recipe_name, recipe_header, recipe_filters_list, recipe_id):
        self.hyper_id = hyper_id
        self.price = price
        self.picture_url = picture_url
        self.model_picture_url = model_picture_url
        self.nid = nid
        self.category_name = category_name
        self.recipe_name = recipe_name
        self.recipe_header = recipe_header
        self.recipe_filters_list = recipe_filters_list
        self.recipe_id = recipe_id

    def toDict(self):
        return self.__dict__


# Ожидаемые данные в выходной таблице.
EXPECTED_DATA_SET = [
    FeedRow(hyper_id=31337, price=600000000, nid=10000, category_name='Correct category',
            picture_url='https://picture_correct.ru', recipe_name='small', recipe_header='small shoes',
            recipe_filters_list=['123:456'], recipe_id=1, model_picture_url='http://model_picture.ru'),
    FeedRow(hyper_id=31337, price=700000000, nid=10000, category_name='Correct category',
            picture_url='https://picture_correct.ru', recipe_name='small', recipe_header='small shoes',
            recipe_filters_list=['444:1'], recipe_id=5, model_picture_url='http://model_picture.ru')
]


# Переопределяем offers_data так, как для нашего фида есть свои тестовые данные.
@pytest.fixture(scope='module')
def offers_data():
    return [o.to_row() for o in SPECIAL_OFFERS_DATA]


# Переопределяем categories_data так, как для нашего фида есть свои тестовые данные.
@pytest.fixture(scope='module')
def categories_data():
    return [o.to_row() for o in SPECIAL_CATEGORIES_DATA]


@pytest.fixture(scope='module')
def recipe_data():
    return [o.to_row() for o in SPECIAL_RECIPE_DATA]


@pytest.fixture(scope='module')
def expected_data_set():
    return [o.toDict() for o in EXPECTED_DATA_SET]


# Проверка входныех
@pytest.skip('borisovnsk@ Не могу победить, тип данных в столбце params MstatModel в локальной YT')
def test_recipe_k50(result_table_data, expected_data_set):
    assert_rows_set_equal(result_table_data, expected_data_set)
