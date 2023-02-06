# coding: utf-8

import pytest
import json
from hamcrest import assert_that, contains_inanyorder
import market.idx.datacamp.proto.category.PartnerCategory_pb2 as DTC
from market.idx.datacamp.controllers.stroller.yatf.test_env import make_stroller
from market.idx.datacamp.yatf.matchers.matchers import HasStatus
from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampCategoriesTable


def gen_row(business_id, category_id, name, parent_id, auto_generated_id):
    row = {
        'business_id': business_id,
        'category_id': category_id,
    }
    category = DTC.PartnerCategory(id=category_id, name=name, parent_id=parent_id, auto_generated_id=auto_generated_id)
    row['content'] = category.SerializeToString()
    return row

# Правильность выдачи проверяется в юниттестах хэндлера TBusinessCategoriesHandle,
# тут же проверяем только работоспособность /v1/partners/*/categories, поэтому достаточно 2-х категорий
CATEGORIES = [
    gen_row(100500, 1, "category 1", None, False),
    gen_row(100500, 11, "category 11", 1, False),
    gen_row(100500, 12345, "category from csv", None, True)
]


@pytest.fixture(scope='module')
def categories_table(yt_server, config):
    return DataCampCategoriesTable(yt_server, config.yt_categories_tablepath, data=CATEGORIES)


@pytest.yield_fixture(scope='module')
def stroller(
        config,
        yt_server,
        log_broker_stuff,
        categories_table,
):
    with make_stroller(
            config,
            yt_server,
            log_broker_stuff,
            categories_table=categories_table,
    ) as stroller_env:
        yield stroller_env


def check_response(stroller, uri, expected_categories):
    response = stroller.get(uri)
    assert_that(response, HasStatus(200))
    assert_that(response.headers['Content-type'], 'application/json; charset=utf-8')
    result = json.loads(response.data)
    # в примере у узлов только по одному ребенку, нет необходимости игнорить порядок children
    assert_that(result, expected_categories)


def test_correct_partner(stroller):
    expected_categories_yml = {
        '1': {'id': '1', 'name': 'category 1', 'parentId': 'rootId', 'children': ['11'], 'isLeaf': False},
        '11': {'id': '11', 'name': 'category 11', 'parentId': '1', 'children': [], 'isLeaf': True},
    }
    expected_categories_csv = {
        '12345': {'id': '12345', 'name': 'category from csv', 'parentId': 'rootId', 'children': [], 'isLeaf': True},
    }
    result_all = {'rootId': {'id': 'rootId', 'name': 'rootId', 'children': contains_inanyorder('1', '12345'), 'isLeaf': False}}
    result_all.update(expected_categories_csv)
    result_all.update(expected_categories_yml)
    check_response(stroller, '/v1/partners/100500/categories', result_all)
    result_yml = {'rootId': {'id': 'rootId', 'name': 'rootId', 'children': ['1'], 'isLeaf': False}}
    result_yml.update(expected_categories_yml)
    check_response(stroller, '/v1/partners/100500/categories?only_yml=1', result_yml)


def test_bad_partner(stroller):
    expected_categories = {
        'rootId': {'id': 'rootId', 'name': 'rootId', 'children': [], 'isLeaf': True},
    }
    check_response(stroller, '/v1/partners/123/categories', expected_categories)
