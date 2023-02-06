import mock

from common.data_api.search_stats.search_stats import SearchStats
from common.tester.utils.mongo import tmp_collection


class TestSearchStats(object):
    def test_get_top(self):
        items = [
            {
                't_type': 'all',
                'obj_type': 'c',
                'obj_id': 213,
                'search_type': 'c',
                'top_searches': [
                    {
                        'geo_id': 'all',
                        'data': [
                            {'obj_type': 'c', 'obj_id': 111, 'total': 42},
                            {'obj_type': 's', 'obj_id': 12, 'total': 43}
                        ]
                    },
                    {
                        'geo_id': 213,
                        'data': [
                            {'obj_type': 's', 'obj_id': 42, 'total': 44},
                            {'obj_type': 'c', 'obj_id': 43, 'total': 45}
                        ]
                    }
                ]
            },
            {
                't_type': 'suburban',
                'obj_type': 's',
                'obj_id': 1111,
                'search_type': 'all',
                'top_searches': [
                    {
                        'geo_id': 54,
                        'data': [
                            {'obj_type': 's', 'obj_id': 1, 'total': 3},
                            {'obj_type': 's', 'obj_id': 2, 'total': 4}
                        ]
                    },
                ]
            }
        ]

        with tmp_collection('c1') as col_from, tmp_collection('c2') as col_to:
            for item in items:
                col_from.insert(item)

            search_stats = SearchStats(col_from, col_to)
            result = list(search_stats.get_top(col_from, 'c213', 'all', 'c', limit=2, geo_id='all'))
            assert result == [('c111', 42), ('s12', 43)]

            result = list(search_stats.get_top(col_from, 'c213', 'all', 'c', limit=1, geo_id=213))
            assert result == [('s42', 44)]

            result = list(search_stats.get_top(col_from, 'c42', 'all', 'c', limit=300, geo_id='all'))
            assert not result

            result = list(search_stats.get_top(col_from, 's1111', 'suburban', 'all', limit=20, geo_id=54))
            assert result == [('s1', 3), ('s2', 4)]

    def test_shortcuts(self):
        search_stats = SearchStats('coll_from', 'coll_to')
        args, kwargs = ['s1111', 'suburban', 'all'], {'limit': 20, 'geo_id': 54}

        with mock.patch.object(SearchStats, 'get_top') as m_get_top:
            m_get_top.return_value = mock.sentinel.result
            result = search_stats.get_top_from(*args, **kwargs)
            assert result == mock.sentinel.result
            m_get_top.assert_called_once_with('coll_from', *args, **kwargs)

        with mock.patch.object(SearchStats, 'get_top') as m_get_top:
            m_get_top.return_value = mock.sentinel.result
            result = search_stats.get_top_to(*args, **kwargs)
            assert result == mock.sentinel.result
            m_get_top.assert_called_once_with('coll_to', *args, **kwargs)
