# -*- coding: utf-8 -*-
import json
import time
import pytest

from test.parallelly.api.disk.base import DiskApiTestCase
from mpfs.common.static import tags
from mpfs.core.services.smartcache_service import smartcache
from test.base_suit import UserTestCaseMixin


class PhotosliceTestCase(UserTestCaseMixin, DiskApiTestCase):
    uid = None
    api_version = 'v1'
    api_mode = tags.platform.INTERNAL

    def generate_uid(self):
        return str(int(time.time() * 1000000000))

    def setup_method(self, method):
        super(PhotosliceTestCase, self).setup_method(method)
        self.uid = self.generate_uid()
        self.login = str(self.uid)

    @pytest.mark.skipif(True, reason='https://st.yandex-team.ru/CHEMODAN-32396')
    def test_photoslice_init(self):
        with self.specified_client(uid=self.uid, login=self.login):
            resp = self.client.request('POST', '%s/disk/photoslice' % self.uid)
            self.assertEqual(resp.status_code, 200)
            res = json.loads(resp.content)
            assert 'photoslice_id' in res
            assert 'revision' in res
            assert 'href' in res
            assert 'templated' in res
            assert 'method' in res

    def test_photoslice_snapshot(self):
        data = """{"invocationInfo": {"req-id": "syVpQxjw","hostname": "osidorkin.haze.yandex.net","exec-duration-millis": "1135",
            "action": "getSmartCacheSnapshot","app-name": "smartcache-client","app-version": "15.10.0"},
        "result": {"clusterization_type": "geo", "photoslice_id": "xU7ecsAU2K7XNdrzJtfJ6wQuYeDl3w2650273844Up1nYBye0alhUXlNk1425486701899rd4cemMI1ow7XnHB",
                    "revision": 2,
                "index": {
                    "items": [
                        {"cluster_id": "1375243187000_1375243187000", "items_count": 1, "from": 1375243187000, "to": 1375243187000},
                        {"cluster_id": "1375244187000_1375244187000", "items_count": 1, "from": 1375244187000, "to": 1375244187000}
                    ]
                },
                "clusters": {
                    "items": [
                        {"cluster_id": "1375243187000_1375243187000","items":
                            [{
                                "item_id": "1_0000001375243187000_ac8529c2fa0efee95b92348b03f79e497f8e697308a2d78ab91672ffbac082f1",
                                "path": "/disk/DSC02553.JPG",
                                "width": 255,
                                "height": 1255,
                                "beauty": 1.0
                            }]
                        },
                        {"cluster_id": "1375244187000_1375244187000","items":
                            [{
                                "item_id": "1_0000001375244187000_ac8529c2fa0effe95b92348b03f79e497f8e697308a2d78ab91672ffbac082f1",
                                "path": "/disk/DSC02553.JPG",
                                "width": 255,
                                "height": 1255,
                                "beauty": 0.1
                            }]
                        }
                    ]
                }
            }

        }"""
        url = r'(.*)/smartcache/smartcache-snapshot?(.*)'
        smartcache.set_response_patch('GET', url, 200, data=data)
        with self.specified_client(uid=self.uid, login=self.login):
            resp = self.client.request('GET',
                                       '%s/disk/photoslice/AAABBBCCCDDD?revision=1' % self.uid)
            self.assertEqual(resp.status_code, 200)
            res = json.loads(resp.content)
            assert 'photoslice_id' in res
            assert 'revision' in res
            assert 'index' in res
            assert 'clusters' in res
            assert 'items' in res['clusters']
            assert 'items' in res['clusters']['items'][0]
            assert 'items' in res['clusters']['items'][1]
            assert 'path' in res['clusters']['items'][0]['items'][0]
            assert 'path' in res['clusters']['items'][1]['items'][0]
            path0 = res['clusters']['items'][0]['items'][0]['path']
            path1 = res['clusters']['items'][1]['items'][0]['path']
            assert path0.startswith('disk:')
            assert path1.startswith('disk:')
            assert 'width' in res['clusters']['items'][0]['items'][0]
            assert 'width' in res['clusters']['items'][1]['items'][0]
            assert 'height' in res['clusters']['items'][0]['items'][0]
            assert 'height' in res['clusters']['items'][1]['items'][0]
            assert 'beauty' in res['clusters']['items'][0]['items'][0]
            assert 'beauty' in res['clusters']['items'][1]['items'][0]

    def test_photoslice_delta(self):
        data = """{"invocationInfo": {"req-id": "02ZYwAuT", "hostname": "osidorkin.haze.yandex.net", "exec-duration-millis": "335", "action": "getSmartCacheDeltaList",
        "app-name": "smartcache-client", "app-version": "15.10.0"},
        "result": {"revision": 2, "total": 1, "limit": 1,
            "items": [
                {"base_revision": 1, "revision": 2,
                    "index_changes": [
                            {"change_type": "insert", "cluster_id": "1375244187000_1375244187000", "data": {"items_count": 1, "from": 1375244187000, "to": 1375244187000} }
                        ],
                    "items_changes": [
                            { "change_type": "insert", "cluster_id": "1375244187000_1375244187000",
                                "item_id": "1_0000001375244187000_ac8529c2fa0effe95b92348b03f79e497f8e697308a2d78ab91672ffbac082f1",
                                "data": {"path": "/disk/DSC12553.JPG", "width": 255, "height": 1255, "beauty": 0.5}
                            }
                        ]
                    }
                ]
            }
        }"""
        url = r'(.*)/smartcache/smartcache-deltas-list?(.*)'
        smartcache.set_response_patch('GET', url, 200, data=data)
        with self.specified_client(uid=self.uid, login=self.login):
            deltas_uri = '%s/disk/photoslice/AAABBBCCCDDD/deltas?base_revision=1' % self.uid
            resp = self.client.request('GET', deltas_uri)
            self.assertEqual(resp.status_code, 200)
            res = json.loads(resp.content)
            assert 'items' in res
            assert 'revision' in res
            assert 'total' in res
            assert 'limit' in res
            assert 'items_changes' in res['items'][0]
            assert 'data' in res['items'][0]['items_changes'][0]
            assert 'path' in res['items'][0]['items_changes'][0]['data']
            assert 'width' in res['items'][0]['items_changes'][0]['data']
            assert 'height' in res['items'][0]['items_changes'][0]['data']
            assert 'beauty' in res['items'][0]['items_changes'][0]['data']
            assert res['items'][0]['items_changes'][0]['data']['path'].startswith('disk:')


    def test_photoslice_delta_index_locality_insert(self):
        data = """{"invocationInfo": {"req-id": "0llLQ23b", "hostname": "osidorkin.haze.yandex.net", "exec-duration-millis": "382", "action": "getSmartCacheDeltaList",
            "app-name": "smartcache-client", "app-version": "100.2.0.1"},
            "result": {"revision": 20, "total": 3, "limit": 3,
                "items": [
                    { "base_revision": 17, "revision": 18,
                        "index_changes": [
                            { "change_type": "update", "cluster_id": "0000001420712662000_0000001420716056000",
                                "data": {
                                    "items_count": 47,
                                    "locality": { "change_type": "insert", "data": {"en": "Moscow", "ru": "Москва", "uk": "Москва", "tr": "Moskova" }}
                                }
                            },
                        ]
                    }
                ]
            }
        }"""
        url = r'(.*)/smartcache/smartcache-deltas-list?(.*)'
        smartcache.set_response_patch('GET', url, 200, data=data)
        with self.specified_client(uid=self.uid, login=self.login):
            deltas_uri = '%s/disk/photoslice/AAABBBCCCDDD/deltas?base_revision=1' % self.uid
            resp = self.client.request('GET', deltas_uri)
            self.assertEqual(resp.status_code, 200)
            res = json.loads(resp.content)
            assert 'items' in res
            assert 'index_changes' in res['items'][0]
            change = res['items'][0]['index_changes'][0]
            assert 'change_type' in change
            assert 'update' == change['change_type']
            assert 'data' in change
            data = change['data']
            assert 'locality' in data
            locality_change = data['locality']
            assert 'change_type' in locality_change
            assert 'insert' == locality_change['change_type']
            assert 'data' in locality_change
            assert 'en' in locality_change['data']

    def test_photoslice_delta_index_locality_remove(self):
        data = """{ "invocationInfo": { "req-id": "0llLQ23b", "hostname": "osidorkin.haze.yandex.net", "exec-duration-millis": "382", "action": "getSmartCacheDeltaList",
            "app-name": "smartcache-client", "app-version": "100.2.0.1" },
            "result": { "revision": 20, "total": 3, "limit": 3,
                 "items": [
                    { "base_revision": 1, "revision": 2,
                        "index_changes": [
                            { "change_type": "update", "cluster_id": "0000001420712662000_0000001420716056000",
                                "data": {"items_count": 45, "locality": { "change_type": "delete"}}
                            }
                        ]
                    }
                ]
            }
        }"""
        url = r'(.*)/smartcache/smartcache-deltas-list?(.*)'
        smartcache.set_response_patch('GET', url, 200, data=data)
        with self.specified_client(uid=self.uid, login=self.login):
            deltas_uri = '%s/disk/photoslice/AAABBBCCCDDD/deltas?base_revision=1' % self.uid
            resp = self.client.request('GET', deltas_uri)
            self.assertEqual(resp.status_code, 200)
            res = json.loads(resp.content)
            assert 'items' in res
            assert 'index_changes' in res['items'][0]
            change = res['items'][0]['index_changes'][0]
            assert 'change_type' in change
            assert 'update' == change['change_type']
            assert 'data' in change
            data = change['data']
            assert 'locality' in data
            locality_change = data['locality']
            assert 'change_type' in locality_change
            assert 'delete' == locality_change['change_type']
            assert 'data' not in locality_change

    def test_photoslice_delta_index_places_insert(self):
        data = """{"invocationInfo": {"req-id": "0llLQ23b", "hostname": "osidorkin.haze.yandex.net", "exec-duration-millis": "382", "action": "getSmartCacheDeltaList",
            "app-name": "smartcache-client", "app-version": "100.2.0.1"},
            "result": {"revision": 20, "total": 3, "limit": 3,
                "items": [
                    {"base_revision": 18, "revision": 19,
                        "index_changes": [
                            {
                                "change_type": "update",
                                "cluster_id": "0000001420712662000_0000001420716056000",
                                "data": {
                                    "places": [
                                        { "change_type": "insert", "place_index": 0,
                                            "data": { "en": "ulitsa Lva Tolstogo", "ru": "улица Льва Толстого", "uk": "улица Льва Толстого", "tr": "ulitsa Lva Tolstogo"}
                                        },
                                        { "change_type": "insert", "place_index": 1,
                                            "data": {"en": "proyezd Odoyevskogo", "ru": "проезд Одоевского", "uk": "проезд Одоевского", "tr": "proyezd Odoyevskogo"}
                                        }
                                    ]
                                }
                            }
                        ]
                    }
                ]
            }
        }"""
        url = r'(.*)/smartcache/smartcache-deltas-list?(.*)'
        smartcache.set_response_patch('GET', url, 200, data=data)
        with self.specified_client(uid=self.uid, login=self.login):
            deltas_uri = '%s/disk/photoslice/AAABBBCCCDDD/deltas?base_revision=1' % self.uid
            resp = self.client.request('GET', deltas_uri)
            self.assertEqual(resp.status_code, 200)
            res = json.loads(resp.content)
            assert 'items' in res
            assert 'index_changes' in res['items'][0]
            change = res['items'][0]['index_changes'][0]
            assert 'change_type' in change
            assert 'update' == change['change_type']
            assert 'data' in change
            data = change['data']
            assert 'places' in data
            places_changes = data['places']
            assert 'change_type' in places_changes[0]
            assert 'change_type' in places_changes[1]
            assert 'place_index' in places_changes[0]
            assert 'place_index' in places_changes[1]
            assert 'insert' == places_changes[0]['change_type']
            assert 'insert' == places_changes[1]['change_type']
            assert 'data' in places_changes[0]
            assert 'data' in places_changes[1]
            assert 'en' in places_changes[0]['data']
            assert 'en' in places_changes[1]['data']
