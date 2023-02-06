# coding=utf-8

import os
import yashopweb
import unittest
import tempfile
import json


def _make_address():
    js = u'''{
        "city": "Saint Petersburg",
        "floor": "8",
        "house": "13",
        "street": "Pobedy",
        "postcode": "131488",
        "country": "Russia",
        "block": "777",
        "subway": "Petrovsko-Razumovskaya"
    }'''
    return json.loads(js)


def _make_region():
    js = '''{
        "type": "CITY",
        "id": 2,
        "parent": {
            "type": "SUBJECT_FEDERATION",
            "id": 10174,
            "parent": {
                "type": "COUNTRY_DISTRICT",
                "id": 17,
                "parent": {
                    "type": "COUNTRY",
                    "id": 225,
                    "name": "Россия"
                },
                "name": "Северо-Западный федеральный округ"
            },
            "name": "Санкт-Петербург и Ленинградская область"
        },
        "name": "Санкт-Петербург"
    }'''
    return json.loads(js)


def _make_delivery(add_dates=False):
    data = {
        'region': _make_region(),
        'address': _make_address(),
    }
    if add_dates:
        data['dates'] = {
          "fromDate": "03-02-2013",
          "toDate": "05-02-2013",
        }
    return data


def _make_order(add_dates=False):
    pass

class FlaskrTestCase(unittest.TestCase):

    def setUp(self):
        yashopweb.app.config['DATABASE'] = __name__
        yashopweb.app.config['TESTING'] = True
        self.app = yashopweb.app.test_client()
        with yashopweb.app.app_context():
            yashopweb.init_db(yashopweb.app.config['DATABASE'])
        self.db = yashopweb._get_db()

    def tearDown(self):
        yashopweb._db.clear_db()

    def test_root(self):
        rv = self.app.get('/')

    def test_api_root(self):
        rv = self.app.get('/api')
        assert b'This is API!' in rv.data

    def test_feed(self):
        rv = self.app.get('/feed')
        assert b'category id="1001"' in rv.data

    def test_orders(self):
        rv = self.app.get('/orders')
        assert u'iPhone 7+ Черный 256 Гб'.encode('utf-8') in rv.data

    def test_items(self):
        rv = self.app.get('/items')
        assert u'Nokia 3310 серенькая'.encode('utf-8') in rv.data

    def test_item(self):
        rv = self.app.get('/items/102/')
        assert u'amount = 15'.encode('utf-8') in rv.data

    def test_reset_db(self):
        rv = self.app.get('/reset_db')

    def test_process_test_orders(self):
        rv = self.app.get('/process_test_orders')

    def test_cart(self):
        data = {
            "cart": {
                "delivery": _make_delivery(),
                "currency": "RUR",
                "items": [
                    {
                        "count": 1,
                        "offerName": u"Nokia 3310 серенькая",
                        "feedId": 200312146,
                        "offerId": "101",
                        "feedCategoryId": "1001",
                    }
                ]
            }
        }
        js = json.dumps(data, indent=4, ensure_ascii=False)
        rv = self.app.post('/api/cart', data=js, content_type='application/json')
        assert '"price": 12500' in rv.data
        assert '"type": "POST"' in rv.data
        assert '"type": "DELIVERY"' in rv.data

    def test_accept(self):
        """ Test /accept method
        1) Заказ принимается (ответ "accepted": true, id правильный)
        2) Заказ появляться в БД
        3) Повторный вызов срабатывает и говорит "accepted": true, id тот же из пп.1
        4) При отмене заказа /accept возвращает ошибку
        """
        data = {
            "order": {
                "currency": "RUR",
                "fake": False,
                "id": 12345,
                "isBooked": False,
                "paymentType": "POSTPAID",
                "paymentMethod": "CASH_ON_DELIVERY",
                "delivery": _make_delivery(add_dates=True),
                "items":[
                    {
                        "count": 1,
                        "feedCategoryId": "1001",
                        "feedId": 200312146,
                        "offerId": "101",
                        "offerName": u"Nokia 3310 серенькая",
                        "price": 12500
                    },
                ],
                "notes": u"Привезите побыстрее, пожалуйста!"
              }
            }
        js = json.dumps(data, indent=4, ensure_ascii=False)
        rv = self.app.post('/api/order/accept', data=js, content_type='application/json')
        rdata = json.loads(rv.data)
        assert rdata['order']['accepted']
        order = self.db.orders[12345]
        self.assertEqual(order.notes, u"Привезите побыстрее, пожалуйста!")

    def test_status(self):
        data = {
            "order": {
                "currency": "RUR",
                "fake": False,
                "id": 1,
                "isBooked": False,
                "paymentType": "POSTPAID",
                "paymentMethod": "CASH_ON_DELIVERY",
                "delivery": _make_delivery(add_dates=True),
                "status": "PROCESSING",
                "items": [
                    {
                        "count": 1,
                        "feedCategoryId": "1001",
                        "feedId": 200312146,
                        "offerId": "101",
                        "offerName": u"Nokia 3310 серенькая",
                        "price": 12500
                    },
                ],
                "notes": u"Привезите побыстрее, пожалуйста!"
              }
            }
        assert self.db.orders[1].status == 'RESERVED'

        # RESERVED -> PROCESSING should be OK
        rv = self.app.post('/api/order/status', data=json.dumps(data, indent=4, ensure_ascii=False), content_type='application/json')
        assert rv.status_code == 200
        assert self.db.orders[1].status == 'PROCESSING'

        # PROCESSING -> RESERVED should be prohibited
        data['order']['status'] = 'RESERVED'
        data_str = json.dumps(data, indent=4, ensure_ascii=False)
        rv = self.app.post('/api/order/status', data=data_str, content_type='application/json')
        assert rv.status_code == 400
        assert 'Transition is not allowed' in rv.data

        # PROCESSING -> CANCELLED should be allowed
        data['order']['status'] = 'CANCELLED'
        data_str = json.dumps(data, indent=4, ensure_ascii=False)
        rv = self.app.post('/api/order/status', data=data_str, content_type='application/json')
        assert rv.status_code == 200
        assert self.db.orders[1].status == 'CANCELLED'

        # CANCELLED -> CANCELLED should be allowed
        data['order']['status'] = 'CANCELLED'
        data_str = json.dumps(data, indent=4, ensure_ascii=False)
        rv = self.app.post('/api/order/status', data=data_str, content_type='application/json')
        assert rv.status_code == 200
        assert self.db.orders[1].status == 'CANCELLED'


if __name__ == '__main__':
    unittest.main()
