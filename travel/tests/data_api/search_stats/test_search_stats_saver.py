from hamcrest import assert_that, has_entries, contains_inanyorder

from common.data_api.search_stats.search_stats_saver import SearchStatsSaver
from common.tester.utils.mongo import tmp_collection


class TestSearchStatsSaver(object):
    def test_valid(self):
        with tmp_collection('c1') as col_from, tmp_collection('c2') as col_to:
            search_stats_saver = SearchStatsSaver(col_from, col_to, top_size=2)
            stats = {
                'suburban': {
                    ('c', 213): {
                        ('c', 2): {
                            213: 21,
                            2: 22,
                            54: 23
                        },
                        ('s', 154): {
                            213: 31,
                            2: 32,
                            54: 33
                        },
                    },
                    ('s', 4242): {
                        ('s', 2): {
                            213: 41,
                            2: 42,
                            54: 43
                        },
                        ('c', 54): {
                            213: 51,
                            2: 52,
                            54: 53
                        },
                    },
                },
            }

            search_stats_saver.save_stats(stats)

            assert_that(col_from.find(), contains_inanyorder(
                *(has_entries(item) for item in [
                    {
                        'obj_type': 'c', 'obj_id': 213, 't_type': 'suburban', 'search_type': 'c',
                        'top_searches': [
                            {'data': [{'obj_type': 'c', 'total': 22, 'obj_id': 2}], 'geo_id': 2},
                            {'data': [{'obj_type': 'c', 'total': 21, 'obj_id': 2}], 'geo_id': 213},
                            {'data': [{'obj_type': 'c', 'total': 23, 'obj_id': 2}], 'geo_id': 54},
                            {'data': [{'obj_type': 'c', 'total': 66, 'obj_id': 2}], 'geo_id': 'all'}
                        ],
                    },
                    {
                        'obj_type': 'c', 'obj_id': 213, 't_type': 'suburban', 'search_type': 's',
                        'top_searches': [
                            {'data': [{'obj_type': 's', 'total': 32, 'obj_id': 154}], 'geo_id': 2},
                            {'data': [{'obj_type': 's', 'total': 31, 'obj_id': 154}], 'geo_id': 213},
                            {'data': [{'obj_type': 's', 'total': 33, 'obj_id': 154}], 'geo_id': 54},
                            {'data': [{'obj_type': 's', 'total': 96, 'obj_id': 154}], 'geo_id': 'all'}
                        ],
                    },
                    {
                        'obj_type': 'c', 'obj_id': 213, 't_type': 'suburban', 'search_type': 'all',
                        'top_searches': [
                            {
                                'geo_id': 2,
                                'data': [
                                    {'obj_type': 's', 'total': 32, 'obj_id': 154},
                                    {'obj_type': 'c', 'total': 22, 'obj_id': 2}
                                ],
                            },
                            {
                                'geo_id': 213,
                                'data': [
                                    {'obj_type': 's', 'total': 31, 'obj_id': 154},
                                    {'obj_type': 'c', 'total': 21, 'obj_id': 2}
                                ],
                            },
                            {
                                'geo_id': 54,
                                'data': [
                                    {'obj_type': 's', 'total': 33, 'obj_id': 154},
                                    {'obj_type': 'c', 'total': 23, 'obj_id': 2}
                                ],
                            },
                            {
                                'geo_id': 'all',
                                'data': [
                                    {'obj_type': 's', 'total': 96, 'obj_id': 154},
                                    {'obj_type': 'c', 'total': 66, 'obj_id': 2}
                                ],
                            }
                        ],
                    },
                    {
                        'obj_type': 's', 'obj_id': 4242, 't_type': 'suburban', 'search_type': 's',
                        'top_searches': [
                            {'data': [{'obj_type': 's', 'total': 42, 'obj_id': 2}], 'geo_id': 2},
                            {'data': [{'obj_type': 's', 'total': 41, 'obj_id': 2}], 'geo_id': 213},
                            {'data': [{'obj_type': 's', 'total': 43, 'obj_id': 2}], 'geo_id': 54},
                            {'data': [{'obj_type': 's', 'total': 126, 'obj_id': 2}], 'geo_id': 'all'}
                        ],
                    },
                    {
                        'obj_type': 's', 'obj_id': 4242, 't_type': 'suburban', 'search_type': 'c',
                        'top_searches': [
                            {'data': [{'obj_type': 'c', 'total': 52, 'obj_id': 54}], 'geo_id': 2},
                            {'data': [{'obj_type': 'c', 'total': 51, 'obj_id': 54}], 'geo_id': 213},
                            {'data': [{'obj_type': 'c', 'total': 53, 'obj_id': 54}], 'geo_id': 54},
                            {'data': [{'obj_type': 'c', 'total': 156, 'obj_id': 54}], 'geo_id': 'all'}
                        ],
                    },
                    {
                        'obj_type': 's', 'obj_id': 4242, 't_type': 'suburban', 'search_type': 'all',
                        'top_searches': [
                            {
                                'geo_id': 2,
                                'data': [
                                    {'obj_type': 'c', 'total': 52, 'obj_id': 54},
                                    {'obj_type': 's', 'total': 42, 'obj_id': 2}
                                ],
                            },
                            {
                                'geo_id': 213,
                                'data': [
                                    {'obj_type': 'c', 'total': 51, 'obj_id': 54},
                                    {'obj_type': 's', 'total': 41, 'obj_id': 2}
                                ],
                            },
                            {
                                'geo_id': 54,
                                'data': [
                                    {'obj_type': 'c', 'total': 53, 'obj_id': 54},
                                    {'obj_type': 's', 'total': 43, 'obj_id': 2}
                                ],
                            },
                            {
                                'geo_id': 'all',
                                'data': [
                                    {'obj_type': 'c', 'total': 156, 'obj_id': 54},
                                    {'obj_type': 's', 'total': 126, 'obj_id': 2}
                                ],
                            }
                        ],
                    },
                ])
            ))
