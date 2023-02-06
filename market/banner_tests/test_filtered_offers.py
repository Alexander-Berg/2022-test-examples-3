# coding=utf-8
import pytest

from conftest import Offer, Model, GENERATION
from market.idx.yatf.utils.utils import assert_rows_set_equal
from market.idx.pylibrary.mindexer_core.banner.banner import FilteredOffersYqlProcessor


@pytest.fixture(scope='module')
def test_processor(config,
                   offers_table,
                   mstat_model_table,
                   models_table,
                   yql_executor):
    processor = FilteredOffersYqlProcessor(config=config,
                                           generation=GENERATION)
    processor.override_yql_executor(yql_executor)
    processor.make_result_table()

    return processor


VALID_PRICE = 'RUR 123'
INVALID_PRICE = 'USD 666'


SPECIAL_OFFERS_DATA = [
    Offer(ware_md5='0', title='Normal', price=VALID_PRICE, model_id=0),
    Offer(ware_md5='1', title='Normal', price=VALID_PRICE, has_gone=False, model_id=0),
    Offer(ware_md5='2', title='Hidden', price=VALID_PRICE, has_gone=True, model_id=0),
    Offer(ware_md5='3', title='Hidden', price=VALID_PRICE, disabled_by_dynamic=True, model_id=0),
    Offer(ware_md5='4', title='Hidden', price=VALID_PRICE, disabled_flags=2048, model_id=0),
    Offer(ware_md5='5', title='Normal', price=VALID_PRICE, model_id=1),
    Offer(ware_md5='6', title='Normal', price=VALID_PRICE, model_id=2),  # Плохая модель
    Offer(ware_md5='7', title='Normal', price=VALID_PRICE, model_id=3),
    Offer(ware_md5='8', title='Normal', price=INVALID_PRICE, model_id=0),  # Плохая цена
    Offer(ware_md5='9', title='Normal', price=VALID_PRICE),  # Оффер без модели
    Offer(ware_md5='10', title='Normal', price=VALID_PRICE, cluster_id=4)
]


@pytest.fixture(scope='module')
def offers_data():
    return [o.to_row() for o in SPECIAL_OFFERS_DATA]


SPECIAL_MODELS_DATA = [
    Model(id=0),
    Model(id=1, vendor_min_publish_timestamp=0),
    Model(id=2, vendor_min_publish_timestamp=8251845912),  # Далёкое будущее
    Model(id=3, vendor_min_publish_timestamp=1561807512),  # Явно прошлое
    Model(id=4, vendor_min_publish_timestamp=1561807512),  # Явно прошлое
]


@pytest.fixture(scope='module')
def models_data():
    return [o.to_row() for o in SPECIAL_MODELS_DATA]


EXPECTED_DATA_SET = [
    Offer(ware_md5='0', title='Normal', price=VALID_PRICE, model_id=0),
    Offer(ware_md5='1', title='Normal', price=VALID_PRICE, has_gone=False, model_id=0),
    Offer(ware_md5='5', title='Normal', price=VALID_PRICE, model_id=1),
    Offer(ware_md5='7', title='Normal', price=VALID_PRICE, model_id=3),
    Offer(ware_md5='9', title='Normal', price=VALID_PRICE),
    Offer(ware_md5='10', title='Normal', price=VALID_PRICE, cluster_id=4)
]


@pytest.fixture(scope='module')
def expected_data_set():
    return [o.to_row() for o in EXPECTED_DATA_SET]


@pytest.skip('borisovnsk@ Не могу победить, тип данных в столбце params MstatModel в локальной YT')
def test_filtered_offers(result_table_data, expected_data_set):
    assert_rows_set_equal(result_table_data, expected_data_set)
