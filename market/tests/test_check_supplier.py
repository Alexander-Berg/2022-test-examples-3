# -*- coding: utf-8 -*-

import pytest
import six

from market.idx.api.backend.idxapi import create_flask_app
from market.idx.api.backend.marketindexer.storage.storage import Storage


class StorageMock(Storage):
    def get_feed_properties(self, supplier_id, warehouse_id, feed_id, shop_name, business_id=None):
        return []


@pytest.fixture(scope="module")
def app():
    return create_flask_app(StorageMock())


def test_show_error_if_all_important_params_are_null(app):
    '''
    Проверяем, что если все параметры (supplier_id, business_id, warehouse_id, feed_id, shop_name) Null, то выдается правильная ошибка
    '''

    urls = [
        '/v1/check_supplier/get?shop_name=',
        '/v1/check_supplier/get?supplier=',
        '/v1/check_supplier/get?supplier=&shop_name=',
        '/v1/check_supplier/get?supplier=&shop_name=&warehouse_id=',
        '/v1/check_supplier/get?supplier=&shop_name=&warehouse_id=&feed_id=',
        '/v1/check_supplier/get?supplier=&shop_name=&warehouse_id=&feed_id=&business_id=',
    ]
    error_string = 'Укажите критерии поиска поставщика!'

    with app.test_client() as client:
        for url in urls:
            resp = client.get(url)
            print(resp.data)
            print('resp.status_code = {}'.format(resp.status_code))
            assert resp.status_code == 200
            assert error_string in six.ensure_str(resp.data)
