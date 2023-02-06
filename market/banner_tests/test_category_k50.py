# coding=utf-8
import pytest

from conftest import Offer, Category, GENERATION
from market.idx.yatf.utils.utils import assert_rows_set_equal
from market.idx.pylibrary.mindexer_core.banner.banner import WhiteCategoryK50FeedProcessor


# Процессор, который тестируем.
# Не забыть перечислить fixture на все таблицы, которые нужны для процессору.
@pytest.fixture(scope='module')
def test_processor(config,
                   offers_table,
                   models_table,
                   vendors_table,
                   categories_table,
                   yql_executor):
    processor = WhiteCategoryK50FeedProcessor(config=config,
                                              generation=GENERATION)
    processor.override_yql_executor(yql_executor)
    processor.make_result_table()

    return processor


# Строка выходной таблицы фида.
class FeedRow(object):
    def __init__(self, hyper_id, nid, category_name, price, picture_url, model_picture_url):
        self.hyper_id = hyper_id
        self.nid = nid
        self.category_name = category_name
        self.price = price
        self.picture_url = picture_url
        self.model_picture_url = model_picture_url

    def toDict(self):
        return self.__dict__

# Входные данные для фида - таблица офферов.

SPECIAL_OFFERS_DATA = [
    Offer(ware_md5="1", picture_url='https://picture_correct.ru', price="RUR 600000000", vendor_id=0, model_id=0, category_id=31337),  # all filters pass
    Offer(ware_md5="2", picture_url='https://picture.ru', price="RUR 600000000", vendor_id=0, model_id=0, category_id=31339),  # nid is null
    Offer(ware_md5="3", picture_url='https://picture.ru', price="RUR 600000000", vendor_id=0, model_id=0, category_id=16440100),  # not banner allow
    Offer(ware_md5="4", picture_url='https://picture.ru', price="RUR 600000000", vendor_id=0, model_id=0, category_id=31341),  # category is not guru
    Offer(ware_md5="5", picture_url='https://picture.ru', price="RUR 600000000", vendor_id=242456, model_id=0, category_id=31342),  # bad vendor
    Offer(ware_md5="6", picture_url='https://picture.ru', price="RUR 600000000", vendor_id=0, model_id=0, category_id=31343),  # not a leaf of category tree
    Offer(ware_md5="7", picture_url=None, price="RUR 600000000", vendor_id=0, model_id=1, category_id=31342),  # picture is null
    Offer(ware_md5="8", picture_url='https://more-expensive-picture.ru', price="RUR 700000000", vendor_id=0, model_id=0, category_id=31342),  # picture is null
]

SPECIAL_CATEGORIES_DATA = [
    Category(hyper_id=31337, nid=10000, uniq_name='Correct category', type='guru', parent=0, parents='31337,0'),
    Category(hyper_id=31338, nid=5000, uniq_name='Incorrect category', type='guru', parent=0, parents='31338,0'),
    Category(hyper_id=31339, nid=None, uniq_name='Incorrect category', type='guru', parent=0, parents='31339,0'),
    Category(hyper_id=16440100, nid=4000, uniq_name='Incorrect category', type='guru', parent=0, parents='16440100,0'),  # bad_parent_category
    Category(hyper_id=31341, nid=3000, uniq_name='Incorrect category', type='gurulight', parent=0, parents='31341,0'),
    Category(hyper_id=31342, nid=2000, uniq_name='Empty picture category', type='guru', parent=0, parents='31342,0'),
    Category(hyper_id=31343, nid=1000, uniq_name='Not a leaf category', type='guru', parent=0, parents='31343,0'),
    Category(hyper_id=313430, nid=5000, uniq_name='Incorrect category', type='guru', parent=31343, parents='313430,31343'),
]

# Ожидаемые данные в выходной таблице.
EXPECTED_DATA_SET = [
    FeedRow(hyper_id=31337, price=600000000, nid=10000, category_name='Correct category', picture_url='https://picture_correct.ru', model_picture_url='http://model_picture.ru'),
    FeedRow(hyper_id=31343, price=600000000, nid=1000, category_name='Not a leaf category', picture_url='https://picture.ru', model_picture_url='http://model_picture.ru'),
    FeedRow(hyper_id=31342, price=700000000, nid=2000, category_name='Empty picture category', picture_url='https://more-expensive-picture.ru', model_picture_url='http://model_picture.ru'),
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
def expected_data_set():
    return [o.toDict() for o in EXPECTED_DATA_SET]


# Проверка входныех
@pytest.skip('borisovnsk@ Не могу победить, тип данных в столбце params MstatModel в локальной YT')
def test_category_k50(result_table_data, expected_data_set):
    assert_rows_set_equal(result_table_data, expected_data_set)
