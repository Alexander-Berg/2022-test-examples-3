# -*- coding: utf-8 -*-

import mock
import urlparse

from mpfs.common.util import from_json, to_json


class DiskSearchSmartMockHelper(object):
    """Реализует честную логику работы ручек директории, используя кэш в памяти.
       Вызов mock() сбрасывает кэш.
       Подменяет только вызов open_url, анализирует url, query и имитирует поведение поиска.

       Примеры использования:
           with DiskSearchSmartMockHelper.mock():
               DiskSearchSmartMockHelper.add_to_index(self.uid, '/disk/1.jpg')
               response = self.json_ok('new_search', ...)
    """

    # uid -> path[]
    _indexed_documents_cache = {}

    @classmethod
    def add_to_index(cls, uid, path):
        if uid not in cls._indexed_documents_cache:
            cls._indexed_documents_cache[uid] = []
        cls._indexed_documents_cache[uid].append(path)

    @classmethod
    def mock(cls):
        cls._indexed_documents_cache = {}
        return mock.patch('mpfs.core.services.search_service.DiskSearch.open_url', wraps=cls._mock_open_url_wrapper)

    @staticmethod
    def _create_resposen_document(path):
        return {
            'docId': '1cbd873e0de6c418b52f0db62723e50376fa7d9ea1c7d8de7c39880110533af6',
            'url': '',
            'relevance': '1cbd873e0de6c418b52f0db62723e50376fa7d9ea1c7d8de7c39880110533af6',
            'properties': {
                'id': '1cbd873e0de6c418b52f0db62723e50376fa7d9ea1c7d8de7c39880110533af6',
                'key': path,
                'scope': 'name',
            }
        }

    @staticmethod
    def _create_response(search_query, paths):
        count = str(len(paths))
        if paths:
            groups = [
                {
                    'doccount': count,
                    'relevance': '1cbd873e0de6c418b52f0db62723e50376fa7d9ea1c7d8de7c39880110533af6',
                    'documents': [DiskSearchSmartMockHelper._create_resposen_document(x) for x in paths],
                },
            ]
        else:
            groups = []
        return {
            'request': search_query,
            'sortBy': {
                'how': 'mtime',
                'order': 'descending',
                'priority': 'no',
            },
            'groupings': [
                {
                    'attr': '',
                    'categ': '',
                    'docs': count,
                    'groups-on-page': '40',
                    'mode': 'flat',
                },
            ],
            'response': {
                'found': {
                    'all': count,
                    'phrase': count,
                    'strict': count,
                },
                'results': [
                    {
                        'attr': '',
                        'docs': count,
                        'found': {
                            'all': count,
                            'phrase': '0',
                            'strict': '0',
                        },
                        'groups': groups,
                    },
                ],
            }
        }

    @classmethod
    def _mock_open_url_wrapper(cls, url, method=None, headers=None, pure_data=None, **kwargs):
        # http://disk.search.yandex.net:19502/?service=disk&kps=297403391&text=sea&key=/disk/*&numdoc=40&format=json&only=id,key
        parsed_url = urlparse.urlparse(url)
        query = urlparse.parse_qs(parsed_url.query)
        if query['service'][0] != 'disk':
            raise NotImplementedError()
        uid = query['kps'][0]

        # supported keys: /disk/*, /trash/*, ...
        key = None
        if 'key' in query:
            key = query['key'][0][:-1]
        search_query = query['text'][0]
        aux_fodlers = query.get('aux_folder')

        paths = cls._indexed_documents_cache.get(uid)
        result = []
        for path in paths:
            if aux_fodlers:
                for aux_fodler in aux_fodlers:
                    if path.startswith('/' + aux_fodler + '/'):
                        break
                else:
                    continue
            else:
                if not path.startswith(key):
                    continue
            if search_query not in path:
                continue
            result.append(path)

        return to_json(DiskSearchSmartMockHelper._create_response(search_query, result))


class DiskGeoSearchSmartMockHelper(DiskSearchSmartMockHelper):
    @classmethod
    def _mock_open_url_wrapper(cls, url, method=None, headers=None, pure_data=None, **kwargs):
        #http://disk.search.yandex.net:19502/api/geo/org_photos?uid=128280859&latitude=55.0&longitude=37.0&time_start=0&time_end=0&distance=1000'
        parsed_url = urlparse.urlparse(url)
        query = urlparse.parse_qs(parsed_url.query)
        uid = query['uid'][0]
        search_query = ''

        paths = cls._indexed_documents_cache.get(uid)
        result = []
        for path in paths:
            result.append(path)

        return to_json(cls._create_response(search_query, result))


