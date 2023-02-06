# coding=utf-8
import pytest
import copy

from conftest import GENERATION, Offer, Vendor, COMMON_VENDOR_DATA
from market.idx.yatf.utils.utils import assert_rows_set_equal
from market.idx.pylibrary.mindexer_core.banner.banner import WhiteVendorCategoryK50FeedProcessor
from test_category_k50 import SPECIAL_OFFERS_DATA, SPECIAL_CATEGORIES_DATA


# Процессор, который тестируем.
# Не забыть перечислить fixture на все таблицы, которые нужны для процессору.
@pytest.fixture(scope='module')
def test_processor(config,
                   offers_table,
                   models_table,
                   categories_table,
                   vendors_table,
                   yql_executor):
    processor = WhiteVendorCategoryK50FeedProcessor(config=config,
                                                    generation=GENERATION)
    processor.override_yql_executor(yql_executor)
    processor.make_result_table()

    return processor


# Строка выходной таблицы фида.
class FeedRow(object):
    def __init__(self, hyper_id, nid, category_name, vendor_id, vendor_name, price, picture_url, model_picture_url):
        self.hyper_id = hyper_id
        self.nid = nid
        self.category_name = category_name
        self.vendor_id = vendor_id
        self.vendor_name = vendor_name
        self.price = price
        self.picture_url = picture_url
        self.model_picture_url = model_picture_url

    def toDict(self):
        return self.__dict__

SPECIAL_OFFERS_DATA = copy.deepcopy(SPECIAL_OFFERS_DATA)
SPECIAL_VENDORS_DATA = copy.deepcopy(COMMON_VENDOR_DATA)

SPECIAL_OFFERS_DATA.extend([
    Offer(ware_md5="9", picture_url=None, price="RUR 600000000", vendor_id=10, model_id=1, category_id=31342)
])  # picture is null

SPECIAL_VENDORS_DATA.extend([
    Vendor(id=10, name='additional vendor')
])

# Ожидаемые данные в выходной таблице.
EXPECTED_DATA_SET = [
    FeedRow(hyper_id=31337, price=600000000, nid=10000, vendor_id=0, vendor_name='vendor1', category_name='Correct category',
            picture_url='https://picture_correct.ru', model_picture_url='http://model_picture.ru'),
    FeedRow(hyper_id=31342, price=600000000, nid=2000, vendor_id=10, vendor_name='additional vendor', category_name='Empty picture category',
            picture_url=None, model_picture_url=None),  # picture is null
    FeedRow(hyper_id=31342, price=700000000, nid=2000, vendor_id=0, vendor_name='vendor1', category_name='Empty picture category',
            picture_url='https://more-expensive-picture.ru', model_picture_url='http://model_picture.ru'),
]


# Переопределяем offers_data так, как для нашего фида есть свои тестовые данные.
@pytest.fixture(scope='module')
def offers_data():
    return [o.to_row() for o in SPECIAL_OFFERS_DATA]


# Переопределяем categories_data так, как для нашего фида есть свои тестовые данные.
@pytest.fixture(scope='module')
def categories_data():
    return [o.to_row() for o in SPECIAL_CATEGORIES_DATA]


# Переопределяем vendors_data так, как для нашего фида есть свои тестовые данные.
@pytest.fixture(scope='module')
def vendors_data():
    return [o.to_row() for o in SPECIAL_VENDORS_DATA]


@pytest.fixture(scope='module')
def expected_data_set():
    return [o.toDict() for o in EXPECTED_DATA_SET]


# Основная проверка
@pytest.skip('borisovnsk@ Не могу победить, тип данных в столбце params MstatModel в локальной YT')
def test_vendor_category_k50(result_table_data, expected_data_set):
    assert_rows_set_equal(result_table_data, expected_data_set)
