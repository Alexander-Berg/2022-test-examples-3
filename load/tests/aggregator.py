from load.projects.lunaparkapi.handlers import aggregator


class TestAggregator:
    test_aggregator = aggregator.Aggregator()

    def test_count_percentage(self):
        data = [34738, 40337]
        assert self.test_aggregator.count_percentage(data, sum(data)) == [46.271, 53.729]


class TestProtoCodesAggregator:

    test_aggregator = aggregator.ProtoCodesAggregator()

    raw_data = [
        ('', 0, 1.0),
        ('', 200, 79366.0),
        ('', 503, 14100.0),
        ('_features', 0, 1.0),
        ('_features', 200, 79366.0),
        ('_features', 503, 14100.0)
    ]

    def test_aggregate(self, mocker):
        mocker.patch(
            'load.projects.lunaparkapi.handlers.aggregator.ProtoCodesAggregator.get_raw_data',
            return_value=[
                (200, '', 34738), (200, 'tag1', 30000), (200, 'tag2', 4738),
                (503, '', 40337), (503, 'tag1', 40000), (503, 'tag2', 337)
            ]
        )
        assert self.test_aggregator.aggregate() == {
            'codes': ['200', '503'],
            'cases': {
                'overall': {
                    '200': {'amount': 34738, 'percentage': 46.271},
                    '503': {'amount': 40337, 'percentage': 53.729}
                },
                'tag1': {
                    '200': {'amount': 30000, 'percentage': 39.96},
                    '503': {'amount': 40000, 'percentage': 53.28}
                },
                'tag2': {
                    '200': {'amount': 4738, 'percentage': 6.311},
                    '503': {'amount': 337, 'percentage': 0.449}
                }
            }
        }


class TestCumulativeQuantilesAggregator:
    test_aggregator = aggregator.CumulativeQuantilesAggregator()
    raw_data = [
        ('', [1360, 1660, 1800, 2040, 2580, 3480, 5300, 7600]),
        ('/fast_command/run', [1340, 1420, 1440, 1470, 1580, 1660, 1770, 1970]),
        ('/hardcoded_music/prepare', [1330, 1430, 1430, 1430, 1530, 1530, 1530, 1530]),
        ('/hardcoded_music/render', [1460, 1570, 1570, 1570, 1570, 1570, 1570, 1570]),
        ('/hardcoded_response/run', [1140, 1270, 1310, 1370, 1430, 1560, 1850, 2050]),
        ('/market_how_much/apply_render', [1160, 1240, 1260, 1280, 1320, 1380, 1440, 1580]),
        ('/music/continue_prepare', [2820, 3120, 3280, 3420, 3600, 4140, 4960, 5700]),
        ('/music/continue_render', [2000, 2150, 2190, 2270, 2360, 2610, 2790, 3040]),
        ('/music/prepare', [2760, 3080, 3200, 3350, 3570, 4370, 5300, 6700]),
        ('/music/render', [4230, 4750, 4900, 5100, 5300, 5600, 6300, 7500]),
        ('/news/prepare', [1430, 1670, 1790, 2040, 2550, 3310, 5300, 7200]),
        ('/news/render', [4820, 5800, 5800, 5900, 5900, 6400, 6400, 6400]),
        ('/pack_http_apply_response', [1080, 1240, 1660, 1660, 1660, 1660, 1660, 1660]),
        ('/pack_http_continue_response', [900, 960, 980, 1020, 1050, 1140, 1180, 1260]),
        ('/pack_http_run_response', [880, 970, 990, 1020, 1060, 1120, 1200, 1280]),
        ('/search/entity_search_goodwin_callback', [2650, 3710, 4190, 4810, 5900, 11000, 18000, 21000]),
        ('/search/prepare', [3350, 4570, 5600, 7800, 16000, 20000, 49000, 63000]),
        ('/search/render', [4430, 6500, 6700, 7000, 7400, 9700, 9700, 9700]),
        ('/unpack_http_apply_request', [1310, 1390, 1390, 1520, 1520, 1520, 1520, 1520]),
        ('/unpack_http_continue_request', [1860, 2080, 2140, 2240, 2520, 3200, 3560, 3740]),
        ('/unpack_http_run_request', [1420, 1880, 1970, 2100, 2840, 4150, 6800, 13000]),
        ('/video_rater/prepare', [1720, 1850, 1850, 1850, 1850, 1850, 1850, 1850])
    ]
    expected = {
        'quantiles': ['50%', '75%', '80%', '85%', '90%', '95%', '98%', '99%'],
        'cases': {
            'overall': [1.36, 1.66, 1.8, 2.04, 2.58, 3.48, 5.3, 7.6],
            '/fast_command/run': [1.34, 1.42, 1.44, 1.47, 1.58, 1.66, 1.77, 1.97],
            '/hardcoded_music/prepare': [1.33, 1.43, 1.43, 1.43, 1.53, 1.53, 1.53, 1.53],
            '/hardcoded_music/render': [1.46, 1.57, 1.57, 1.57, 1.57, 1.57, 1.57, 1.57],
            '/hardcoded_response/run': [1.14, 1.27, 1.31, 1.37, 1.43, 1.56, 1.85, 2.05],
            '/market_how_much/apply_render': [1.16, 1.24, 1.26, 1.28, 1.32, 1.38, 1.44, 1.58],
            '/music/continue_prepare': [2.82, 3.12, 3.28, 3.42, 3.6, 4.14, 4.96, 5.7],
            '/music/continue_render': [2.0, 2.15, 2.19, 2.27, 2.36, 2.61, 2.79, 3.04],
            '/music/prepare': [2.76, 3.08, 3.2, 3.35, 3.57, 4.37, 5.3, 6.7],
            '/music/render': [4.23, 4.75, 4.9, 5.1, 5.3, 5.6, 6.3, 7.5],
            '/news/prepare': [1.43, 1.67, 1.79, 2.04, 2.55, 3.31, 5.3, 7.2],
            '/news/render': [4.82, 5.8, 5.8, 5.9, 5.9, 6.4, 6.4, 6.4],
            '/pack_http_apply_response': [1.08, 1.24, 1.66, 1.66, 1.66, 1.66, 1.66, 1.66],
            '/pack_http_continue_response': [0.9, 0.96, 0.98, 1.02, 1.05, 1.14, 1.18, 1.26],
            '/pack_http_run_response': [0.88, 0.97, 0.99, 1.02, 1.06, 1.12, 1.2, 1.28],
            '/search/entity_search_goodwin_callback': [2.65, 3.71, 4.19, 4.81, 5.9, 11.0, 18.0, 21.0],
            '/search/prepare': [3.35, 4.57, 5.6, 7.8, 16.0, 20.0, 49.0, 63.0],
            '/search/render': [4.43, 6.5, 6.7, 7.0, 7.4, 9.7, 9.7, 9.7],
            '/unpack_http_apply_request': [1.31, 1.39, 1.39, 1.52, 1.52, 1.52, 1.52, 1.52],
            '/unpack_http_continue_request': [1.86, 2.08, 2.14, 2.24, 2.52, 3.2, 3.56, 3.74],
            '/unpack_http_run_request': [1.42, 1.88, 1.97, 2.1, 2.84, 4.15, 6.8, 13.0],
            '/video_rater/prepare': [1.72, 1.85, 1.85, 1.85, 1.85, 1.85, 1.85, 1.85]
        }
    }

    def test_aggregate(self, mocker):
        mocker.patch(
            'load.projects.lunaparkapi.handlers.aggregator.CumulativeQuantilesAggregator.get_raw_data',
            return_value=self.raw_data
        )
        assert self.test_aggregator.aggregate() == self.expected
