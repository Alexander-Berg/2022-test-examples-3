# -*- coding: utf-8 -*-
import copy
import json
import random
import time
import uuid
import pytest
import mock

from mpfs.platform.common import PlatformConfigClientInfo
from test.parallelly.api.base import ApiTestCase, InternalPlatformTestClient

from mpfs.common.static import tags
from mpfs.common.util import from_json
from mpfs.core.services.data_api_profile_service import data_api_profile
from mpfs.core.services.data_api_service import data_api
from mpfs.platform.auth import InternalTokenAuth
from mpfs.platform.handlers import BasePlatformHandler
from mpfs.platform.fields import DateTimeToTSWithTimezoneField
from mpfs.platform.v1.personality.exceptions import PersonalityUnknownObjectTypeError

class TestDateTimeToTSWithTimezoneField(ApiTestCase):

    def test_to_native(self):
        field = DateTimeToTSWithTimezoneField(milliseconds=True)
        examples = [
            ("2014-09-16T14:00:00", (1410876000000, 0)),
            ("2014-09-16T14:00:00Z", (1410876000000, 0)),
            ("2014-09-16T14:00:00+01:00", (1410876000000 - 3600000, 1 * 3600)),
            ("2014-09-16T14:00:00-01:00", (1410876000000 + 3600000, -1 * 3600)),
            ("2014-09-16T14:00:00+02:00", (1410876000000 - 2 * 3600000, 2 * 3600)),
            ("2014-09-16T14:00:00-02:00", (1410876000000 + 2 * 3600000, -2 * 3600)),
            ("2014-09-16T14:00:00+03:00", (1410876000000 - 3 * 3600000, 3 * 3600)),
            ("2014-09-16T14:00:00-03:00", (1410876000000 + 3 * 3600000, -3 * 3600)),
        ]

        for date_str, val in examples:
            self.assertEqual(val, field.to_native(date_str))

    def test_from_native(self):
        field = DateTimeToTSWithTimezoneField(milliseconds=True)
        examples = [
            ((1410876000000, 0), "2014-09-16T14:00:00+00:00"),
            ((1410876000000 - 3600000, 1 * 3600), "2014-09-16T14:00:00+01:00"),
            ((1410876000000 + 3600000, -1 * 3600), "2014-09-16T14:00:00-01:00"),
            ((1410876000000 - 2 * 3600000, 2 * 3600), "2014-09-16T14:00:00+02:00"),
            ((1410876000000 + 2 * 3600000, -2 * 3600), "2014-09-16T14:00:00-02:00"),
            ((1410876000000 - 3 * 3600000, 3 * 3600), "2014-09-16T14:00:00+03:00"),
            ((1410876000000 + 3 * 3600000, -3 * 3600), "2014-09-16T14:00:00-03:00"),
        ]

        for val, date_str in examples:
            self.assertEqual(date_str, field.from_native(val))

    def test_to_side_conversion(self):
        field = DateTimeToTSWithTimezoneField(milliseconds=True)
        examples = {
            "2014-09-16T14:00:00" : "2014-09-16T14:00:00+00:00",
            "2014-09-16T14:00:00Z" : "2014-09-16T14:00:00+00:00",
            "2014-09-16T14:00:00+01:00" : "2014-09-16T14:00:00+01:00",
            "2014-09-16T14:00:00-01:00" : "2014-09-16T14:00:00-01:00",
            "2014-09-16T14:00:00+02:00" : "2014-09-16T14:00:00+02:00",
            "2014-09-16T14:00:00-02:00" : "2014-09-16T14:00:00-02:00",
            "2014-09-16T14:00:00+03:00" : "2014-09-16T14:00:00+03:00",
            "2014-09-16T14:00:00-03:00" : "2014-09-16T14:00:00-03:00",
        }

        for key, val in examples.iteritems():
            self.assertEqual(val, field.from_native(field.to_native(key)))


class DataApiProfileTest(ApiTestCase):
    api_mode = tags.platform.INTERNAL
    api_version = 'v1'
    uid = None
    master_slave_sync_delay = 0.04
    """Время синхронизации мастера (в который всё пишется) со слэйвами (с котороых всё читается)."""

    def generate_uid(self):
        return str(int(time.time() * 1000000000))

    def setup_method(self, method):
        super(DataApiProfileTest, self).setup_method(method)
        data_api.log = self.log
        data_api_profile.log = self.log


class FlightsTestCase(DataApiProfileTest):
    """Тесты ручек для работы с перелетами"""

    def setup_method(self, method):
        super(FlightsTestCase, self).setup_method(method)
        # генерируем для каждого теста новый uid, т.к. бэкэнд не позволяет удалять пользователей
        self.uid = self.generate_uid()
        self.login = str(self.uid)

    def test_save_flight_case_api(self):
        headers = {"Content-type": "application/json"}
        data = {
            "flight_number": "SU2030",
            "checkin_url": "http://www.aeroflot.ru/cms/online_registration",

            "airline": {
                "iata_code": "S7",
                "name": "S7 Airlines"
            },

            "departure": {
                "time": "2014-09-16T11:20:00Z",
                "geo_id": 213,
                "airport": {
                    "iata-code": "SVO",
                    "name": "Шереметьево",
                    "ya_schedule_id": "9600213"
                }
            },

            "arrival": {
                "time": "2014-09-16T14:00:00+0400",
                "geo_id": 10398,
                "airport": {
                    "iata-code": "SVO",
                    "name": "Шереметьево"
                }
            }
        }

        with self.specified_client(uid=self.uid, login=self.login):
            resp = self.client.request('PUT', 'case/personality/events/flights/actual/event-1', headers=headers, data=data, uid=self.uid)
            self.assertEqual(resp.status_code, 200)

            time.sleep(self.master_slave_sync_delay)  # к сожалению, бэкэнд не сразу осознаёт, что в нём что-то изменили

            resp = self.client.request('GET', 'personality/profile/events/flights/actual/', uid=self.uid)
            self.assertEqual(resp.status_code, 200)

            resp = from_json(resp.get_result())
            self.log.info(resp.items)
            self.assertEqual(1, len(resp['items']))
            self.assertEqual('event-1', resp['items'][0]['id'])

            resp = self.client.request('GET', 'personality/profile/events/flights/actual/event-1', uid=self.uid)
            self.assertEqual(resp.status_code, 200)

            resp = from_json(resp.get_result())
            self.assertEqual('event-1', resp['id'])
            self.assertEqual('mail', resp['data_source'])

            resp = self.client.request('DELETE', 'case/personality/events/flights/actual/event-1', uid=self.uid)
            self.assertEqual(resp.status_code, 200)

            time.sleep(self.master_slave_sync_delay)  # к сожалению, бэкэнд не сразу осознаёт, что в нём что-то изменили

            resp = self.client.request('GET', 'personality/profile/events/flights/actual/', uid=self.uid)
            self.assertEqual(resp.status_code, 200)

            resp = from_json(resp.get_result())
            self.log.info(resp.items)
            self.assertEqual(0, len(resp['items']))

            resp = self.client.request('GET', 'personality/profile/events/flights/actual/event-1', uid=self.uid)
            self.assertEqual(resp.status_code, 404)

    def test_delete_not_existing_flight_case_api(self):
        with self.specified_client(uid=self.uid, login=self.login):
            resp = self.client.request('DELETE', 'case/personality/events/flights/actual/event-with-not-existing-id', uid=self.uid)
            self.assertEqual(resp.status_code, 404)

    def test_delete_not_existing_flight_normal(self):
        with self.specified_client(uid=self.uid, login=self.login):
            resp = self.client.request('DELETE', 'personality/profile/events/flights/actual/event-with-not-existing-id', uid=self.uid)
            self.assertEqual(resp.status_code, 404)

    def test_save_flight_normal(self):
        headers = {"Content-type": "application/json"}
        data = {
            "flight_number": "SU2030",
            "checkin_url": "http://www.aeroflot.ru/cms/online_registration",
            "data_source": "rasp",

            "airline": {
                "iata_code": "S7",
                "name": "S7 Airlines"
            },

            "departure": {
                "time": "2014-09-16T11:20:00Z",
                "geo_id": 213,
                "airport": {
                    "iata-code": "SVO",
                    "name": "Шереметьево",
                    "ya_schedule_id": "9600213"
                }
            },

            "arrival": {
                "time": "2014-09-16T14:00:00+0400",
                "geo_id": 10398,
                "airport": {
                    "iata-code": "SVO",
                    "name": "Шереметьево"
                }
            }
        }

        with self.specified_client(uid=self.uid, login=self.login):
            resp = self.client.request('PUT', 'personality/profile/events/flights/actual/event-1', headers=headers, data=data, uid=self.uid)
            self.assertEqual(resp.status_code, 200)

            time.sleep(self.master_slave_sync_delay)  # к сожалению, бэкэнд не сразу осознаёт, что в нём что-то изменили

            resp = self.client.request('GET', 'personality/profile/events/flights/actual/', uid=self.uid)
            self.assertEqual(resp.status_code, 200)

            resp = from_json(resp.get_result())
            self.log.info(resp.items)
            self.assertEqual(1, len(resp['items']))
            self.assertEqual('event-1', resp['items'][0]['id'])

            resp = self.client.request('GET', 'personality/profile/events/flights/actual/event-1', uid=self.uid)
            self.assertEqual(resp.status_code, 200)

            resp = from_json(resp.get_result())
            self.assertEqual('event-1', resp['id'])
            self.assertEqual('rasp', resp['data_source'])

            resp = self.client.request('DELETE', 'personality/profile/events/flights/actual/event-1', uid=self.uid)
            self.assertEqual(resp.status_code, 200)

            time.sleep(self.master_slave_sync_delay)  # к сожалению, бэкэнд не сразу осознаёт, что в нём что-то изменили

            resp = self.client.request('GET', 'personality/profile/events/flights/actual/', uid=self.uid)
            self.assertEqual(resp.status_code, 200)

            resp = from_json(resp.get_result())
            self.log.info(resp.items)
            self.assertEqual(0, len(resp['items']))

            resp = self.client.request('GET', 'personality/profile/events/flights/actual/event-1', uid=self.uid)
            self.assertEqual(resp.status_code, 404)


class AddressesTestCase(ApiTestCase):
    data = {
        "address_line": u"Россия, Москва, улица Льва Толстого, 16 ",
        "address_line_short": u"ул. Льва Толстого, 16",
        "data_key": "some",
        "data_source": "bla",
        "latitude": 55.7351921,
        "longitude": 37.585504,
        "tags": ["work"],
        "title": u"Яндекс",
        "entrance_number": "2",
        "custom_metadata": '{"smth": "meta"}'
    }

    geopoint_data = {
        "address_line": u"transit",
        "address_line_short": u"transit",
        "data_key": "home=work=",
        "data_source": "bla",
        "latitude": 55.7351921,
        "longitude": 37.585504,
        "tags": ["work"],
        "title": u"transit"
    }

    headers = {"Content-type": "application/json"}
    uid = str(1234)
    created_addresses_ids = []

    @classmethod
    def teardown_class(cls):
        client = InternalPlatformTestClient('http://localhost/v1/')
        for address_id in cls.created_addresses_ids:
            client.request('DELETE',
                           'personality/profile/addresses/bla/%s' % address_id,
                           uid=cls.uid)
        super(AddressesTestCase, cls).teardown_class()

    def gen_key(self):
        key = str(random.randint(1, 10000000000))
        self.created_addresses_ids.append(key)
        return key

    def gen_rand_uid(self):
        return str(random.randint(1, 10000000000))

    def save_address(self, uid, key, data=None):
        data = data or self.data
        data["data_key"] = key
        return self.client.request('PUT', 'personality/profile/addresses', headers=self.headers, data=data, uid=uid)

    def get_address(self, uid, key):
        return self.client.request('GET', 'personality/profile/addresses/bla/' + key, uid=uid)

    def delete_address(self, uid, key):
        return self.client.request('DELETE', 'personality/profile/addresses/bla/' + key, uid=uid)

    def list_addresses(self, uid):
        return self.client.request('GET', 'personality/profile/addresses/bla', uid=uid)

    def check_fields(self, resp, expected, fieldsToCmp):
        for field in fieldsToCmp:
            self.assertEqual(resp[field], expected[field])

    def check_address_fields(self, resp):
        fieldsToCmp = ["address_line", "address_line_short", "title", "latitude", "longitude", "tags", "data_key",
                       "entrance_number", "custom_metadata"]
        self.check_fields(resp, self.data, fieldsToCmp)

    def check_geopoint_fields(self, resp=None, raw=None):
        fieldsToCmp = ["title", "latitude", "longitude", "tags", "data_key"]
        self.check_fields(resp, self.geopoint_data, fieldsToCmp)

    def test_save_get_address(self):
        with self.specified_client(uid=self.uid, login=self.uid):
            key = self.gen_key()

            resp = self.save_address(self.uid, key)
            self.assertEqual(resp.status_code, 200)

            resp = self.get_address(self.uid, key)
            self.assertEqual(resp.status_code, 200)
            self.check_address_fields(from_json(resp.get_result()))

    def test_save_delete_address(self):
        with self.specified_client(uid=self.uid, login=self.uid):
            key = self.gen_key()

            resp = self.save_address(self.uid, key)
            self.assertEqual(resp.status_code, 200)

            resp = self.delete_address(self.uid, key)
            self.assertEqual(resp.status_code, 204)

            resp = self.delete_address(self.uid, key)
            self.assertEqual(resp.status_code, 404)

    def test_list_addresses(self):
        uid = self.gen_rand_uid()
        with self.specified_client(uid=uid, login=uid):
            key1 = self.gen_key()
            resp = self.save_address(uid, key1)
            self.assertEqual(resp.status_code, 200)

            key2 = self.gen_key()
            resp = self.save_address(uid, key2)
            self.assertEqual(resp.status_code, 200)

            resp = self.list_addresses(uid)
            self.assertEqual(resp.status_code, 200)

            resp = from_json(resp.get_result())
            self.assertEquals(len(resp["items"]), 2)
            self.assertEquals(resp["total"], 2)
            self.assertEquals(resp["offset"], 0)
            self.assertEquals(resp["limit"], 20)

    def test_save_work_2times(self):
        with self.specified_client(uid=self.uid, login=self.uid):
            key = "work"

            resp = self.save_address(self.uid, key)
            self.assertEqual(resp.status_code, 200)

            resp = self.save_address(self.uid, key)
            self.assertEqual(resp.status_code, 200)

    def test_save_geopoint(self):
        with self.specified_client(uid=self.uid, login=self.uid):
            key = "home=work=0"

            resp = self.save_address(self.uid, key, self.geopoint_data)
            self.assertEqual(resp.status_code, 200)

    def test_save_get_geopoint(self):
        with self.specified_client(uid=self.uid, login=self.uid):
            key = "home=work=0"

            resp = self.save_address(self.uid, key, self.geopoint_data)
            self.assertEqual(resp.status_code, 200)

            resp = self.get_address(self.uid, key)
            self.assertEqual(resp.status_code, 200)
            self.check_geopoint_fields(from_json(resp.get_result()))

    def test_save_delete_geopoint(self):
        with self.specified_client(uid=self.uid, login=self.uid):
            key = "work=home=0"

            resp = self.save_address(self.uid, key, self.geopoint_data)
            self.assertEqual(resp.status_code, 200)

            resp = self.delete_address(self.uid, key)
            self.assertEqual(resp.status_code, 204)

            resp = self.get_address(self.uid, key)
            self.assertEqual(resp.status_code, 404)

    def test_list_with_geopoint_and_address(self):
        uid = self.gen_rand_uid()
        with self.specified_client(uid=uid, login=uid):
            key1 = self.gen_key()
            resp = self.save_address(uid, key1)
            self.assertEqual(resp.status_code, 200)

            key2 = "home=work=" + self.gen_key()
            resp = self.save_address(uid, key2, self.geopoint_data)
            self.assertEqual(resp.status_code, 200)

            resp = self.list_addresses(uid)
            self.assertEqual(resp.status_code, 200)

            resp = from_json(resp.get_result())
            self.assertEquals(len(resp["items"]), 2)
            self.assertEquals(resp["total"], 2)
            self.assertEquals(resp["offset"], 0)
            self.assertEquals(resp["limit"], 20)

            self.check_address_fields(resp["items"][0])
            self.check_geopoint_fields(resp["items"][1])


class YaTicketsTestCase(DataApiProfileTest):
    """Тесты ручек для работы с заказами из Я.Билетов"""

    def setup_method(self, method):
        super(YaTicketsTestCase, self).setup_method(method)
        # генерируем для каждого теста новый uid, т.к. бэкэнд не позволяет удалять пользователей
        self.uid = self.generate_uid()
        self.login = str(self.uid)

    def check_for_order_data(self, data):

        headers = {"Content-type": "application/json"}
        with self.specified_client(uid=self.uid, login=self.login):
            # Создали заказ через PUT

            resp = self.client.request('PUT', 'personality/profile/ya-tickets/orders/fullOrderId', headers=headers,
                                       data=data, uid=self.uid)
            assert resp.status_code == 200

            time.sleep(self.master_slave_sync_delay)  # к сожалению, бэкэнд не сразу осознаёт, что в нём что-то изменили

            # Проверили, что в бэкэнде сохранился именно он

            resp = self.client.request('GET', 'personality/profile/ya-tickets/orders/', uid=self.uid)
            assert resp.status_code == 200

            resp = from_json(resp.get_result())

            print resp['items']

            assert [data] == resp['items']

            resp = self.client.request('GET', 'personality/profile/ya-tickets/orders/fullOrderId', uid=self.uid)
            assert resp.status_code == 200

            resp = from_json(resp.get_result())

            assert data == resp

            # Удалили заказ

            resp = self.client.request('DELETE', 'personality/profile/ya-tickets/orders/fullOrderId', uid=self.uid)
            assert resp.status_code == 204

            time.sleep(self.master_slave_sync_delay)  # к сожалению, бэкэнд не сразу осознаёт, что в нём что-то изменили

            # Проверили, что в бэкэнде удалился

            resp = self.client.request('GET', 'personality/profile/ya-tickets/orders/', uid=self.uid)
            assert resp.status_code == 200

            resp = from_json(resp.get_result())
            self.log.info(resp.items)
            assert len(resp['items']) == 0

            resp = self.client.request('GET', 'personality/profile/ya-tickets/orders/fullOrderId', uid=self.uid)
            assert resp.status_code == 404

    def test_save_full_order(self):
        self.check_for_order_data({
            "order_id": "fullOrderId",
            "session": {
                "id": 101010,

                "date": "2020-03-13T20:00:00.000+09:00",
                "previous_date": "2020-03-12T20:00:00.000+05:00",
                "duration": 15,

                "event": {
                    "id": 101,
                    "type": "performance",
                    "name": "SaveTheWorld"
                },

                "venue": {
                    "id": 202,
                    "types": [
                        "cinema",
                        "museum"
                    ],
                    "name": "Kremlin",
                    "address": "RedSquare",
                    "region_id": 303,
                    "coordinates": {
                        "latitude": 55.7542,
                        "longitude": 37.62
                    },
                    "smartpass": False,
                    "subway": [
                        {
                            "title": "station1",
                            "lines": [
                                {
                                    "hex_color": "aabbcc"
                                },
                                {
                                    "hex_color": "ddeeff"
                                }
                            ]
                        },

                        {
                            "title": "station2",
                            "lines": [
                                {
                                    "hex_color": "001122"
                                },
                                {
                                    "hex_color": "334455"
                                }
                            ]
                        }
                    ]
                },
                "hall": {
                    "name": "HallName"
                }
            },

            "ticket_count": 1,
            "order_number": "orderNumberValue",
            "presentation_order_number": "presentationOrderNumberValue",
            "code_word": "CodeWord",

            "barcode": {
                "url": "https://barcode.com",
                "width": 505,
                "height": 606
            },

            "pkpass_barcode": {
                "url": "https://pkpass_barcode.com",
                "width": 707,
                "height": 808
            },
            "tickets": [
                {
                    "row": "909",
                    "place": "10101",
                    "ticket_number": "asdsa",
                    "admission": False,
                    "level_name": "A",
                    "category_name": "B",
                    "entrance_name": "C"
                },
                {
                    "admission": True
                }
            ]
        })

    def test_save_min_order(self):
        self.check_for_order_data({
            "order_id": "fullOrderId",
            "session": {
                "id": 101010,

                "date": "2020-03-13T20:00:00.000+09:00",

                "event": {
                    "id": 101,
                    "name": "SaveTheWorld"
                },

                "venue": {
                    "id": 202,
                    "types": [
                        "cinema",
                        "museum"
                    ],
                    "name": "Kremlin",
                    "address": "RedSquare",
                    "region_id": 303,
                    "coordinates": {
                        "latitude": 55.7542,
                        "longitude": 37.62
                    },
                    "smartpass": False,
                    "subway": []
                },
                "hall": {
                    "name": "HallName"
                }
            },

            "ticket_count": 1,
            "tickets": []
        })


class NewAddressesTestCase(ApiTestCase):
    api_version = 'v2'

    master_slave_sync_delay = 0.04
    """Время синхронизации мастера (в который всё пишется) со слэйвами (с котороых всё читается)."""

    data = {
        "address_line": u"Россия, Москва, улица Льва Толстого, 16 ",
        "address_line_short": u"ул. Льва Толстого, 16",
        "address_id": "some",

        "latitude": 55.7351921,
        "longitude": 37.585504,
        "tags": ["work"],
        "title": u"Яндекс"
    }

    headers = {"Content-type": "application/json"}
    uid = '1234'

    @classmethod
    def setup_class(cls):
        client = InternalPlatformTestClient('http://localhost/%s/' % cls.api_version)
        # Очищаем адреса для тестового юзера
        resp = client.request('GET', 'personality/profile/addresses/', uid=cls.uid)
        data = from_json(resp.get_result())
        for item in data['items']:
            try:
                client.request('DELETE', 'personality/profile/addresses/' + item['address_id'], uid=cls.uid)
            except:
                # Если адрес удалят пока мы итерируемся - пропускаем ошибку удаления
                pass

    def gen_key(self):
        return str(random.randint(1, 10000000000))

    def gen_rand_uid(self):
        return str(random.randint(1, 10000000000))

    def save_address(self, uid, key, data=None):
        data = data or self.data
        data = copy.deepcopy(data)
        data["address_id"] = key
        return self.client.request('PUT', 'personality/profile/addresses/' + key, headers=self.headers, data=data, uid=uid)

    def patch_address(self, uid, key, data):
        return self.client.request('PATCH', 'personality/profile/addresses/' + key, headers=self.headers, data=data, uid=uid)

    def tag_address(self, uid, key, tags):
        return self.client.request('PUT', 'personality/profile/addresses/' + key + "/tag?tags=" + ",".join(tags), headers=self.headers, uid=uid)

    def untag_address(self, uid, key, tags):
        return self.client.request('PUT', 'personality/profile/addresses/' + key + "/untag?tags=" + ",".join(tags), headers=self.headers, uid=uid)

    def touch_address(self, uid, key):
        return self.client.request('POST', 'personality/profile/addresses/' + key + "/touch", headers=self.headers, uid=uid)

    def get_address(self, uid, key):
        return self.client.request('GET', 'personality/profile/addresses/' + key, uid=uid)

    def delete_address(self, uid, key):
        return self.client.request('DELETE', 'personality/profile/addresses/' + key, uid=uid)

    def list_addresses(self, uid):
        return self.client.request('GET', 'personality/profile/addresses/', uid=uid)

    def check_fields(self, resp, expected, fields_to_cmp):
        for field in fields_to_cmp:
            assert resp[field] == expected[field]

    def check_address_fields(self, resp):
        fields_to_cmp = ["address_line", "address_line_short", "title", "latitude", "longitude", "tags"]
        self.check_fields(resp, self.data, fields_to_cmp)

    def test_save_get_address(self):
        with self.specified_client(uid=self.uid, login=self.uid):
            key = self.gen_key()

            resp = self.save_address(self.uid, key)
            assert resp.status_code == 200

            time.sleep(self.master_slave_sync_delay)  # к сожалению, бэкэнд не сразу осознаёт, что в нём что-то изменили

            resp = self.get_address(self.uid, key)
            assert resp.status_code == 200
            self.check_address_fields(from_json(resp.get_result()))

    def test_save_delete_address(self):
        with self.specified_client(uid=self.uid, login=self.uid):
            key = self.gen_key()

            resp = self.save_address(self.uid, key)
            assert resp.status_code == 200

            resp = self.delete_address(self.uid, key)
            assert resp.status_code == 204

            resp = self.delete_address(self.uid, key)
            assert resp.status_code == 404

    def test_list_addresses(self):
        uid = self.gen_rand_uid()
        with self.specified_client(uid=uid, login=uid):
            key1 = self.gen_key()
            resp = self.save_address(uid, key1)
            assert resp.status_code == 200

            key2 = self.gen_key()
            resp = self.save_address(uid, key2)
            assert resp.status_code == 200

            time.sleep(self.master_slave_sync_delay)  # к сожалению, бэкэнд не сразу осознаёт, что в нём что-то изменили

            resp = self.list_addresses(uid)
            assert resp.status_code == 200

            resp = from_json(resp.get_result())
            assert len(resp["items"]) == 2
            assert resp["total"] == 2
            assert resp["offset"] == 0
            assert resp["limit"] == 20

    def test_save_work_2times(self):
        with self.specified_client(uid=self.uid, login=self.uid):
            key = "work"

            resp = self.save_address(self.uid, key)
            assert resp.status_code == 200

            resp = self.save_address(self.uid, key)
            assert resp.status_code == 200

    def test_tag_address(self):
        with self.specified_client(uid=self.uid, login=self.uid):
            key = self.gen_key()

            resp = self.save_address(self.uid, key)
            assert resp.status_code == 200

            resp = self.tag_address(self.uid, key, ['tag1'])
            assert resp.status_code == 200

            time.sleep(self.master_slave_sync_delay)  # к сожалению, бэкэнд не сразу осознаёт, что в нём что-то изменили

            resp = self.get_address(self.uid, key)
            assert resp.status_code == 200
            resp = from_json(resp.get_result())

            assert 'tag1' in resp['tags']

            resp = self.tag_address(self.uid, key, ['tag2', 'tag3', 'tag4'])
            assert resp.status_code == 200

            time.sleep(self.master_slave_sync_delay)  # к сожалению, бэкэнд не сразу осознаёт, что в нём что-то изменили

            resp = self.get_address(self.uid, key)
            assert resp.status_code == 200
            resp = from_json(resp.get_result())

            assert 'tag2' in resp['tags']
            assert 'tag3' in resp['tags']
            assert 'tag4' in resp['tags']

    def test_untag_address(self):
        with self.specified_client(uid=self.uid, login=self.uid):
            key = self.gen_key()

            resp = self.save_address(self.uid, key)
            assert resp.status_code == 200

            resp = self.tag_address(self.uid, key, ['tag10', 'tag11', 'tag12'])
            assert resp.status_code == 200

            resp = self.untag_address(self.uid, key, ['tag11', 'tag12'])
            assert resp.status_code == 200

            time.sleep(self.master_slave_sync_delay)  # к сожалению, бэкэнд не сразу осознаёт, что в нём что-то изменили

            resp = self.get_address(self.uid, key)
            assert resp.status_code == 200
            resp = from_json(resp.get_result())

            assert 'tag10' in resp['tags']
            assert 'tag11' not in resp['tags']
            assert 'tag12' not in resp['tags']

    def test_touch_address(self):
        with self.specified_client(uid=self.uid, login=self.uid):
            key = self.gen_key()

            resp = self.save_address(self.uid, key)
            assert resp.status_code == 200

            time.sleep(self.master_slave_sync_delay)  # к сожалению, бэкэнд не сразу осознаёт, что в нём что-то изменили

            resp = self.get_address(self.uid, key)
            assert resp.status_code == 200
            resp1 = from_json(resp.get_result())

            resp = self.touch_address(self.uid, key)
            assert resp.status_code == 200

            time.sleep(self.master_slave_sync_delay)  # к сожалению, бэкэнд не сразу осознаёт, что в нём что-то изменили

            resp = self.get_address(self.uid, key)
            assert resp.status_code == 200
            resp = from_json(resp.get_result())

            assert resp['last_used'] != resp1['last_used']

    def test_patch_address(self):
        with self.specified_client(uid=self.uid, login=self.uid):
            key = self.gen_key()

            resp = self.save_address(self.uid, key)
            assert resp.status_code == 200

            resp = self.patch_address(self.uid, key, data={"title": "New title"})
            assert resp.status_code == 200

            time.sleep(self.master_slave_sync_delay)  # к сожалению, бэкэнд не сразу осознаёт, что в нём что-то изменили

            resp = self.get_address(self.uid, key)
            assert resp.status_code == 200
            resp = from_json(resp.get_result())

            assert resp['title'] == "New title"

    def test_personality_specific_uids(self):
        TOKEN = 'service_token'
        # Патчим метод авторизации, чтоб добавить в него наш токен
        for auth_method in BasePlatformHandler.default_auth_methods:
            if isinstance(auth_method, InternalTokenAuth):
                auth_method.client_info_by_token[TOKEN] = PlatformConfigClientInfo(
                    'test', 'test_client_id', 'test_client_name', ['yadisk:all'], []
                )

        uid = 'device-1234123345ABCDEF123ab'
        headers = {'Authorization': 'ClientToken token=%s;uid=%s' % (TOKEN, uid)}

        resp = self.client.get('personality/profile/addresses', headers=headers)
        assert resp.status_code == 200

        uid = 'yaid-1234123345890'
        headers = {'Authorization': 'ClientToken token=%s;uid=%s' % (TOKEN, uid)}

        resp = self.client.get('personality/profile/addresses', headers=headers)
        assert resp.status_code == 200


class GenericObjectTestCase(DataApiProfileTest):
    """Тесты для работы с типизированными объектами"""

    def setup_method(self, method):
        super(GenericObjectTestCase, self).setup_method(method)
        # генерируем для каждого теста новый uid, т.к. бэкэнд не позволяет удалять пользователей
        self.uid = self.generate_uid()
        self.login = str(self.uid)

    def send_request(self, http_method, ulr, data=None):
        with self.specified_client(uid=self.uid, login=self.login):
            resp = self.client.request(http_method, ulr, data=data, uid=self.uid)
            return resp

    def put_object(self, data):
        resp = self.send_request('PUT', 'personality/profile/my/type/Name/' + data['key'], data)
        assert resp.status_code == 200

    def test_put_generic_object(self):
        data = {"key": "objId1", "requiredProp": "requiredPropValue1"}
        self.put_object(data)

    def test_post_generic_object(self):
        data = {"requiredProp": "requiredPropValue2"}
        resp = self.send_request('POST', 'personality/profile/my/type/Name', data)
        assert resp.status_code == 200
        json_result = from_json(resp.get_result())
        assert json_result['requiredProp'] == 'requiredPropValue2'
        assert 'key' in json_result

    def test_get_generic_object(self):
        data = {"key": "objId3", "requiredProp": "requiredPropValue3"}
        self.put_object(data)
        time.sleep(self.master_slave_sync_delay)  # к сожалению, бэкэнд не сразу осознаёт, что в нём что-то изменили

        resp = self.send_request('GET', 'personality/profile/my/type/Name/objId3')
        assert resp.status_code == 200
        json_result = from_json(resp.get_result())
        assert json_result['key'] == 'objId3'
        assert json_result['requiredProp'] == 'requiredPropValue3'

    def test_get_not_existed_generic_object(self):
        resp = self.send_request('GET', 'personality/profile/my/type/Name/%s' % uuid.uuid1())
        assert resp.status_code == 404

    def test_get_generic_object_with_unknown_type(self):
        resp = self.send_request('GET', 'personality/profile/%s/%s' % (uuid.uuid1(), uuid.uuid1()))
        assert resp.status_code == 400
        json_result = from_json(resp.get_result())
        assert json_result['error'] == PersonalityUnknownObjectTypeError.__name__

    def test_delete_generic_object(self):
        data = {"key": "objId4", "requiredProp": "requiredPropValue4"}
        self.put_object(data)
        time.sleep(self.master_slave_sync_delay)  # к сожалению, бэкэнд не сразу осознаёт, что в нём что-то изменили

        resp = self.send_request('DELETE', 'personality/profile/my/type/Name/objId4')
        assert resp.status_code == 200

    def test_get_list_of_generic_objects(self):
        self.put_object({"key": "objId5", "requiredProp": "requiredPropValue5"})
        self.put_object({"key": "objId6", "requiredProp": "requiredPropValue6"})
        time.sleep(self.master_slave_sync_delay)  # к сожалению, бэкэнд не сразу осознаёт, что в нём что-то изменили

        resp = self.send_request('GET', 'personality/profile/my/type/Name?limit=101&offset=0')
        assert resp.status_code == 200
        json_result = from_json(resp.get_result())
        print json_result
        assert len(json_result["items"]) == 2
        assert json_result["total"] == 2
        assert json_result["offset"] == 0
        assert json_result["limit"] == 101

    @pytest.mark.skipif(True, reason='https://st.yandex-team.ru/CHEMODAN-32396')
    def test_generic_permissions_full(self):
        TOKEN = 'full_token'
        # Патчим метод авторизации, чтоб добавить в него наш токен
        for auth_method in BasePlatformHandler.default_auth_methods:
            if isinstance(auth_method, InternalTokenAuth):
                auth_method.client_info_by_token[TOKEN] = PlatformConfigClientInfo(
                    'test', 'test_client_id', 'test_client_name',
                    ['cloud_api.profile:generic.videosearch.likes.write',
                     'cloud_api.profile:generic.videosearch.likes.read'], []
                )

        headers = {'Authorization': 'ClientToken token=%s;uid=%s' % (TOKEN, self.uid)}

        # К этим ресурсам у нас не должно быть доступа ибо нет скоупа cloud_api.profile:generic.my.type.Name
        resp = self.client.post('personality/profile/my/type/Name',
                                data={"key": "objId5", "requiredProp": "requiredPropValue5"},
                                headers=headers)
        assert resp.status_code == 403
        # даже на чтение
        resp = self.client.get('personality/profile/my/type/Name/objId5',
                               headers=headers)
        assert resp.status_code == 403

        # А вот видеолайки можем писать
        resp = self.client.post('personality/profile/videosearch/likes',
                                data={"url": "url", "title": "test title", "description": "test description"},
                                headers=headers)
        assert resp.status_code == 200
        # и даже можем читать объекты в коллекции, несмотря на то что id-шника объекта нет в разрешении
        like_id = json.loads(resp.content)['id']
        resp = self.client.get('personality/profile/videosearch/likes/%s' % like_id, headers=headers)
        assert resp.status_code == 200
        # коллекцию тоже можем читать
        resp = self.client.get('personality/profile/videosearch/likes', headers=headers)
        assert resp.status_code == 200

    @pytest.mark.skipif(True, reason='https://st.yandex-team.ru/CHEMODAN-32396')
    def test_generic_permissions_readonly(self):
        TOKEN = 'ro_token'
        # Патчим метод авторизации, чтоб добавить в него наш токен
        for auth_method in BasePlatformHandler.default_auth_methods:
            if isinstance(auth_method, InternalTokenAuth):
                auth_method.client_info_by_token[TOKEN] = PlatformConfigClientInfo(
                    'test', 'ro_test_client_id', 'ro_test_client_name',
                    ['cloud_api.profile:generic.videosearch.likes.read'], []
                )

        headers = {'Authorization': 'ClientToken token=%s;uid=%s' % (TOKEN, self.uid)}

        # К этим ресурсам у нас не должно быть доступа ибо нет скоупа cloud_api.profile:generic.my.type.Name
        resp = self.client.post('personality/profile/my/type/Name',
                                data={"key": "objId5", "requiredProp": "requiredPropValue5"},
                                headers=headers)
        assert resp.status_code == 403
        # даже на чтение
        resp = self.client.get('personality/profile/my/type/Name/objId5',
                               headers=headers)
        assert resp.status_code == 403

        # Писать видеолайки тоже не можем
        resp = self.client.post('personality/profile/videosearch/likes',
                                data={"url": "url", "title": "test title", "description": "test description"},
                                headers=headers)
        assert resp.status_code == 403

        # А вот читать сколько угодно и даже можем читать объекты в коллекции,
        # несмотря на то что id-шника объекта нет в разрешении
        resp = self.client.get('personality/profile/videosearch/likes/FAKE_RESOURCE_ID', headers=headers)
        assert resp.status_code == 404
        # и коллекцию тоже можем читать
        resp = self.client.get('personality/profile/videosearch/likes', headers=headers)
        assert resp.status_code == 200


class GetNotificationsCountHandlerTestCase(DataApiProfileTest):
    def test_get_notification_count(self):
        TOKEN = 'service_token'
        uid = 'device-1234123345ABCDEF123ab'
        # Патчим метод авторизации, чтоб добавить в него наш токен
        headers = {'Authorization': 'ClientToken token=%s;uid=%s' % (TOKEN, uid)}
        for auth_method in BasePlatformHandler.default_auth_methods:
            if isinstance(auth_method, InternalTokenAuth):
                auth_method.client_info_by_token[TOKEN] = PlatformConfigClientInfo(
                    'test', 'test_client_id', 'test_client_name', ['yadisk:all'], []
                )

        with mock.patch('mpfs.engine.http.client.open_url') as mock_obj:
            resp = self.client.get('/v1/personality/profile/notifications/unread-count', headers=headers)
            assert mock_obj.call_count == 1
            assert resp.status_code != 401
            for call in mock_obj.call_args_list:
                headers = call[1]['headers'].keys()
                assert 'X-Uid' in headers
                assert 'X-Ya-Service-Ticket' in headers


class DocsTestCase(DataApiProfileTest):

    def setup_method(self, method):
        super(DocsTestCase, self).setup_method(method)
        # генерируем для каждого теста новый uid, т.к. бэкэнд не позволяет удалять пользователей
        self.uid = self.generate_uid()
        self.login = str(self.uid)
        self.headers = {"Content-type": "application/json"}

    def test_save_new_document_with_resource_id(self):
        with mock.patch('mpfs.core.services.common_service.SERVICES_TVM_2_0_ENABLED', False),\
                self.specified_client(uid=self.uid, login=self.login):
            data = {
                "resource_id": "321:safdas"
            }
            # Создали новый документ
            resp = self.client.request('POST', 'personality/profile/docs/yadisk/docs_docx',
                                       headers=self.headers,
                                       data=data, uid=self.uid)
            assert resp.status_code == 200

            time.sleep(self.master_slave_sync_delay)

            # Проверили, что в бэкэнде сохранился именно он
            resp = self.client.request('GET', 'personality/profile/yadisk/docs_docx/', uid=self.uid)
            assert resp.status_code == 200

            resp = from_json(resp.get_result())
            new_doc = resp['items'][0]
            assert new_doc['resource_id'] == data['resource_id']
            assert 'ts' in new_doc

    def test_save_new_document_with_office_online_sharing_url(self):
        with mock.patch('mpfs.core.services.common_service.SERVICES_TVM_2_0_ENABLED', False),\
                self.specified_client(uid=self.uid, login=self.login):
            data = {
                "office_online_sharing_url": "some_url"
            }
            # Создали новый документ
            resp = self.client.request('POST', 'personality/profile/docs/yadisk/docs_docx',
                                       headers=self.headers,
                                       data=data, uid=self.uid)
            assert resp.status_code == 200

            time.sleep(self.master_slave_sync_delay * 2)

            # Проверили, что в бэкэнде сохранился именно он
            resp = self.client.request('GET', 'personality/profile/yadisk/docs_docx/', uid=self.uid)
            assert resp.status_code == 200

            resp = from_json(resp.get_result())
            new_doc = resp['items'][0]
            assert new_doc['office_online_sharing_url'] == data['office_online_sharing_url']
            assert 'ts' in new_doc

    def test_save_new_document_with_overflow(self):
        new_size = 10
        with mock.patch('mpfs.core.services.common_service.SERVICES_TVM_2_0_ENABLED', False),\
                mock.patch('mpfs.platform.v1.personality.handlers.DOCS_COLLECTION_SIZE', new_size),\
                self.specified_client(uid=self.uid, login=self.login):
            for i in range(new_size):
                data = {
                    'resource_id': 'resource_%s' % i
                }
                resp = self.client.request('POST', 'personality/profile/docs/yadisk/docs_docx',
                                           headers=self.headers,
                                           data=data, uid=self.uid)
                assert resp.status_code == 200

            time.sleep(self.master_slave_sync_delay * 2)

            # Проверили, что в бэкэнде сохранился именно он
            resp = self.client.request('GET', 'personality/profile/yadisk/docs_docx/', uid=self.uid)
            assert resp.status_code == 200

            resp = from_json(resp.get_result())
            items = resp['items']
            assert len(items) == new_size

            oldest_doc = sorted(items, key=lambda k: k.get('ts', 0))[0]
            data = {'office_online_sharing_url': 'some_url'}
            resp = self.client.request('POST', 'personality/profile/docs/yadisk/docs_docx',
                                       headers=self.headers,
                                       data=data, uid=self.uid)
            assert resp.status_code == 200

            time.sleep(self.master_slave_sync_delay * 2)

            # Проверили, что в бэкэнде сохранился именно он
            resp = self.client.request('GET', 'personality/profile/yadisk/docs_docx/', uid=self.uid)
            assert resp.status_code == 200
            resp = from_json(resp.get_result())
            items = resp['items']
            assert len(items) == new_size
            assert oldest_doc not in items
            assert [item for item in items if item.get('office_online_sharing_url') == data['office_online_sharing_url']]

    def test_already_deleted_document(self):
        new_size = 2
        fake_items = [
            {'id': 1, 'ts': 1, 'resource-id': '1'},
            {'id': 2, 'ts': 2, 'resource-id': '2'}
        ]
        with mock.patch('mpfs.core.services.common_service.SERVICES_TVM_2_0_ENABLED', False),\
                mock.patch('mpfs.platform.v1.personality.handlers.DOCS_COLLECTION_SIZE', new_size),\
                self.specified_client(uid=self.uid, login=self.login):

            data = {'office_online_sharing_url': 'some_url'}
            resp = self.client.request('POST', 'personality/profile/docs/yadisk/docs_docx',
                                       headers=self.headers,
                                       data=data, uid=self.uid)
            assert resp.status_code == 200

            time.sleep(self.master_slave_sync_delay * 2)

            with mock.patch('mpfs.platform.v1.personality.handlers.DocsSaveObjectHandler._get_all_docs',
                       return_value=fake_items):
                data = {'office_online_sharing_url': 'some_url_2'}
                resp = self.client.request('POST', 'personality/profile/docs/yadisk/docs_docx',
                                           headers=self.headers,
                                           data=data, uid=self.uid)
                assert resp.status_code == 200

            time.sleep(self.master_slave_sync_delay * 2)

            # Проверили, что в бэкэнде сохранился именно он
            resp = self.client.request('GET', 'personality/profile/yadisk/docs_docx/', uid=self.uid)
            assert resp.status_code == 200
            resp = from_json(resp.get_result())
            items = resp['items']
            assert len(items) == new_size
            assert [item for item in items if item.get('office_online_sharing_url') == data['office_online_sharing_url']]
