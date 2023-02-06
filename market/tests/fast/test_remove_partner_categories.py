# coding: utf-8

import pytest
from hamcrest import assert_that
from market.idx.datacamp.proto.category.PartnerCategory_pb2 import PartnerCategory
from market.idx.datacamp.proto.api.SyncCategory_pb2 import DeletePartnerCategoriesBatchRequest
from market.idx.datacamp.controllers.stroller.yatf.test_env import make_stroller
from market.idx.datacamp.yatf.matchers.matchers import HasStatus
from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampCategoriesTable


def gen_row(business_id, category_id):
    row = {
        'business_id': business_id,
        'category_id': category_id,
    }
    category = PartnerCategory(id=category_id)
    row['content'] = category.SerializeToString()
    return row


@pytest.fixture(scope='module')
def categories_table(yt_server, config):
    return DataCampCategoriesTable(
        yt_server,
        config.yt_categories_tablepath,
        data=[
            gen_row(100500, 1),
            gen_row(100500, 2),
            gen_row(100500, 3),
        ]
    )


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


def test_remove_categories(stroller, categories_table):
    response = stroller.post(
        '/v1/partners/100500/categories/remove',
        data=DeletePartnerCategoriesBatchRequest(category_ids=[1, 3]).SerializeToString()
    )
    assert_that(response, HasStatus(200))
    categories_table.load()
    assert_that(len(categories_table.data) == 1)
