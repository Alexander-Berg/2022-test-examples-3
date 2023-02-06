# coding: utf-8

import pytest
from hamcrest import assert_that, equal_to, has_length, empty

from market.idx.datacamp.controllers.stroller.yatf.test_env import make_stroller
from market.idx.yatf.matchers.yt_rows_matchers import HasDatacampCategoriesRows
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf
from market.idx.datacamp.proto.category import PartnerCategory_pb2 as PartnerCategory
from market.idx.datacamp.proto.api import SyncCategory_pb2 as SyncCategory
from market.idx.datacamp.yatf.matchers.matchers import HasStatus
from market.pylibrary.proto_utils import message_from_data


@pytest.yield_fixture(scope='module')
def stroller(
    config,
    yt_server,
    log_broker_stuff,
):
    with make_stroller(
        config,
        yt_server,
        log_broker_stuff,
    ) as stroller_env:
        yield stroller_env


def set_request(stroller, business_id, categories):
    data = message_from_data({
        'categories': {
            'categories': [{
                'business_id': business_id,
                'id': category_id,
                'parent_id': parent_id,
                'name': name,
                'meta': {
                    'timestamp': '2019-02-15T17:55:55Z',
                }
            } for category_id, parent_id, name in categories]
        }
    }, SyncCategory.UpdatePartnerCategories())
    return stroller.post(path='/v1/partners/{}/categories/flat'.format(business_id), data=data.SerializeToString())


def test_categories_flat(stroller):
    """ Добавляем и получаем категории бизнеса в плоском формате """
    business_id = 1

    # Пишем
    response = set_request(stroller, business_id=business_id, categories=[(1, 0, 'cat_1'), (2, 1, 'cat_1_2')])
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(SyncCategory.PartnerCategoriesResponse, {
        'categories': {
            'categories': [
                {'id': 1},
                {'id': 2},
            ]
        }
    }))
    assert_that(response.headers['Content-type'], equal_to('application/x-protobuf'))

    # Проверяем наличие в таблице
    assert_that(stroller.categories_table.data, HasDatacampCategoriesRows([{
        'business_id': 1,
        'category_id': 1,
        'content': IsSerializedProtobuf(PartnerCategory.PartnerCategory, {
            'name': 'cat_1'
        }),
    }, {
        'business_id': 1,
        'category_id': 2,
        'content': IsSerializedProtobuf(PartnerCategory.PartnerCategory, {
            'name': 'cat_1_2'
        }),
    }]))

    # Читаем
    response = stroller.get(path='/v1/partners/{}/categories/flat'.format(business_id))
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(SyncCategory.PartnerCategoriesResponse, {
        'categories': {
            'categories': [
                {'id': 1},
                {'id': 2},
            ]
        }
    }))
    assert_that(response.headers['Content-type'], equal_to('application/x-protobuf'))


def test_categories_auto_generated_id(stroller):
    """ Проверяем генерацию идентификатора категории """
    business_id = 1
    expected_categroy_id = 1553378339

    # Пишем категорию без идентификатора
    response = set_request(stroller, business_id=business_id, categories=[(None, None, 'auto_generated_cat_1')])
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(SyncCategory.PartnerCategoriesResponse, {
        'categories': {
            'categories': [
                {
                    'id': expected_categroy_id,
                    'name': 'auto_generated_cat_1',
                    'auto_generated_id': True,
                    'parent_id': None,
                },
            ]
        }
    }))
    assert_that(response.headers['Content-type'], equal_to('application/x-protobuf'))

    # Проверяем наличие в таблице
    assert_that(stroller.categories_table.data, HasDatacampCategoriesRows([{
        'business_id': business_id,
        'category_id': expected_categroy_id,
        'content': IsSerializedProtobuf(PartnerCategory.PartnerCategory, {
            'id': expected_categroy_id,
            'name': 'auto_generated_cat_1',
            'auto_generated_id': True,
            'parent_id': None,
        }),
    }]))


def test_categories_flat_filter_by_name(stroller):
    business_id = 2

    # Добавляем категории
    response = set_request(stroller, business_id=business_id, categories=[
        (1, None, 'categ_1'),
        (2, None, 'categ_2'),
        (3, 1, 'categ_3'),
        (4, 1, 'categ_4'),
        (5, 2, 'categ_5'),
        (6, 2, 'categ_4'),
        (7, 3, 'categ_4')
    ])

    assert_that(response, HasStatus(200))
    assert_that(response.headers['Content-type'], equal_to('application/x-protobuf'))

    # Проверим, что вернутся категории только с именами 'categ_2' и 'categ_4'
    request = message_from_data({'names': ['categ_2', 'categ_4']}, SyncCategory.GetPartnerCategoriesFlatFilterRequest())
    response = stroller.post(path='/v1/partners/{}/categories/flat_filter'.format(business_id), data=request.SerializeToString())

    expected_categories = [2, 4, 6, 7]
    assert_that(response, HasStatus(200))
    assert_that(response.data, IsSerializedProtobuf(SyncCategory.PartnerCategoriesResponse, {
        'categories': {
            'categories': has_length(len(expected_categories))
        }
    }))
    assert_that(response.data, IsSerializedProtobuf(SyncCategory.PartnerCategoriesResponse, {
        'categories': {
            'categories': [
                {'id': id}
            ] for id in expected_categories
        }
    }))

    # Проверим, что вернется пустой ответ, если нет категорий с заданным именем
    request = message_from_data({'names': ['non_exist_categ']}, SyncCategory.GetPartnerCategoriesFlatFilterRequest())
    response = stroller.post(path='/v1/partners/{}/categories/flat_filter'.format(business_id), data=request.SerializeToString())

    assert_that(response, HasStatus(200))
    assert_that(response.data, empty())
