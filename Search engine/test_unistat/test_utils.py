# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import bisect
import collections
import time

from search.horadric2.proto.structures import unistat_pb2

from search.martylib.test_utils import TestCase
from search.martylib.unistat.histogram import Histogram
from search.martylib.unistat.metrics import MetricStorage, GlobalMetricStorage, unistat_capture
from search.martylib.unistat.utils import clean_data, convert_to_list, merge_data


class TestUtils(TestCase):
    maxDiff = None
    DEFAULT_BIN_EDGES = Histogram.DEFAULT_BIN_EDGES

    class NonConvertableValue(object):
        def __str__(self):
            pass

    def counter_to_histogram_data(self, counter, edges=None):
        if edges is None:
            edges = self.DEFAULT_BIN_EDGES

        result_counter = collections.Counter()
        for key, value in list(counter.items()):
            if key < edges[0] or key >= edges[-1]:
                continue

            edge = edges[bisect.bisect_right(edges, key) - 1]
            result_counter[edge] += value

        return [(edge, result_counter[edge]) for edge in edges]

    def test_histogram_add_value(self):
        for data, edges in [
            ([1, 2, 3, 10, 1, 4, 5, 0, 100500, 2], [1.0, 2.0, 5.0, 10.0]),
            ([1, 5, 7, 100, 8, 10, 1, 5, -10, 6, 2, 4, 12], [1.0, 4.0, 7.0, 12.0]),
            ([4, 5], [1.0, 4.0, 7.0, 12.0]),
            ([3, 6], [1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0]),
            ([1, -7, 0, -1001], [2.0, 5.0, 10.0])
        ]:
            counter = collections.Counter()
            histogram = Histogram(bin_edges=edges)
            self.assertEqual(histogram.dump(), self.counter_to_histogram_data(counter, edges))

            for value in data:
                histogram.add_value(value)
                counter[value] += 1
                self.assertEqual(histogram.dump(), self.counter_to_histogram_data(counter, edges))

    def test_empty_histogram(self):
        with self.assertRaises(ValueError):
            Histogram([])

    def test_histogram_with_too_many_bins(self):
        with self.assertRaises(ValueError):
            Histogram(list(range(Histogram.MAX_BINS + 1)))

    def test_merge_data(self):
        # noinspection PyTypeChecker
        d1 = unistat_pb2.UnistatData(
            numerical={
                'foo_summ': 2.0,
            },
            histograms={
                'baz': unistat_pb2.HistogramData(bins=(
                    unistat_pb2.Bin(edge=1.0, value=2),
                    unistat_pb2.Bin(edge=2.0, value=5),
                )),
            },
            log_histograms={
                'woohoo': unistat_pb2.LogHistogramData(
                    values=[1, 4, 8, 12, 35]
                ),
            }
        )
        # noinspection PyTypeChecker
        d2 = unistat_pb2.UnistatData(
            numerical={
                'foo_summ': 2.0,
                'bar_summ': 3.0,
            },
            histograms={
                'baz': unistat_pb2.HistogramData(bins=(
                    unistat_pb2.Bin(edge=1.0, value=2),
                    unistat_pb2.Bin(edge=2.0, value=5),
                )),
                'foobar': unistat_pb2.HistogramData(bins=(
                    unistat_pb2.Bin(edge=3.0, value=3),
                )),
            },
            log_histograms={
                'kek': unistat_pb2.LogHistogramData(
                    values=[10, 41, 82, 128, 351]
                ),
                'shrek': unistat_pb2.LogHistogramData(
                    values=[1, 15]
                ),
            }
        )
        # noinspection PyTypeChecker
        expected = unistat_pb2.UnistatData(
            numerical={
                'foo_summ': 4.0,
                'bar_summ': 3.0,
            },
            histograms={
                'baz': unistat_pb2.HistogramData(bins=(
                    unistat_pb2.Bin(edge=1.0, value=4),
                    unistat_pb2.Bin(edge=2.0, value=10),
                )),
                'foobar': unistat_pb2.HistogramData(bins=(
                    unistat_pb2.Bin(edge=3.0, value=3),
                )),
            },
            log_histograms={
                'woohoo': unistat_pb2.LogHistogramData(
                    values=[1, 4, 8, 12, 35]
                ),
                'kek': unistat_pb2.LogHistogramData(
                    values=[10, 41, 82, 128, 351]
                ),
                'shrek': unistat_pb2.LogHistogramData(
                    values=[1, 15]
                ),
            }
        )
        d3 = merge_data(d1, d2)
        self.assertEqual(d3, expected)

        d4 = unistat_pb2.UnistatData()
        d4.numerical['foo'] += 1
        # noinspection PyTypeChecker
        d5 = unistat_pb2.UnistatData(numerical={'foo': 1})
        self.assertEqual(d4, d5)

    def test_clean_data(self):
        # noinspection PyTypeChecker
        histograms = {
            'bark': unistat_pb2.HistogramData(bins=(
                unistat_pb2.Bin(edge=0.5, value=0),
                unistat_pb2.Bin(edge=1.0, value=0),
                unistat_pb2.Bin(edge=2.0, value=0),
                unistat_pb2.Bin(edge=3.0, value=0),
            )),
            'baz': unistat_pb2.HistogramData(bins=(
                unistat_pb2.Bin(edge=0.0, value=0),
                unistat_pb2.Bin(edge=0.5,),
                unistat_pb2.Bin(edge=1.0, value=4),
                unistat_pb2.Bin(edge=2.0, value=10),
            )),
            'baz_baz': unistat_pb2.HistogramData(bins=(
            )),
            'foobar': unistat_pb2.HistogramData(bins=(
                unistat_pb2.Bin(edge=3.0, value=3),
            )),
        }
        d = unistat_pb2.UnistatData(
            numerical={
                'foo_summ': 4.0,
                'foo_foo_summ': 0.0,
                'bar_summ': 3.0,
                'bar_bar_summ': 0,
                'bard_summ': float('-Inf'),
                'bark_summ': float('Inf'),
                'barn_summ': float('NaN'),
                'realtime_attt': 0.0,
            },
            histograms=histograms,
        )
        # noinspection PyTypeChecker
        expected = unistat_pb2.UnistatData(
            numerical={
                'foo_summ': 4.0,
                'bar_summ': 3.0,
                'realtime_attt': 0.0,
            },
            histograms=histograms,
        )
        # noinspection PyTypeChecker
        clean_d = unistat_pb2.UnistatData(
            numerical={
                'foo_summ': 2.0,
                'realtime_attt': 0.0,
            },
            histograms=histograms,
        )
        self.assertEqual(clean_data(d), expected)
        self.assertEqual(clean_data(clean_d), clean_d)

    def test_convert_to_list(self):
        # noinspection PyTypeChecker
        data = unistat_pb2.UnistatData(
            numerical={
                'bar_summ': 3.0,
                'zzz_summ': 100.0,
                'foo_summ': 4.0,
            },
            histograms={
                'baz': unistat_pb2.HistogramData(bins=(
                    unistat_pb2.Bin(edge=1.0, value=4),
                    unistat_pb2.Bin(edge=2.0, value=10),
                )),
                'foobar': unistat_pb2.HistogramData(bins=(
                    unistat_pb2.Bin(edge=3.0, value=3),
                )),
            },
        )
        expected = [
            ('bar_summ', 3.0),
            ('zzz_summ', 100.0),
            ('foo_summ', 4.0),
            ('baz', [[1.0, 4], [2.0, 10]]),
            ('foobar', [[3.0, 3]]),
        ]
        self.assertEqual(sorted(convert_to_list(data)), sorted(expected))

    def test_tags(self):
        # noinspection PyTypeChecker
        expected = unistat_pb2.UnistatData(
            numerical={
                'user=test;test_tags-summ': 1.0,
                'foo=1;bar=2;test_tags-summ': 1.0,
                'test_tags-summ': 2.0,
            },
            histograms={
                'test_tags-hgram': unistat_pb2.HistogramData(
                    bins=(unistat_pb2.Bin(edge=i, value=2 * (i == 1.0)) for i in self.DEFAULT_BIN_EDGES)
                ),
                'user=test;test_tags-hgram': unistat_pb2.HistogramData(
                    bins=(unistat_pb2.Bin(edge=i, value=(i == 1.0)) for i in self.DEFAULT_BIN_EDGES)
                ),
                'foo=1;bar=2;test_tags-hgram': unistat_pb2.HistogramData(
                    bins=(unistat_pb2.Bin(edge=i, value=(i == 1.0)) for i in self.DEFAULT_BIN_EDGES)
                )
            },
        )

        metrics = MetricStorage('test_tags')
        metrics.add_histogram('hgram')

        metrics.increment('summ')
        metrics.increment('summ', user='test')
        metrics.increment('summ', foo=1, bar=2)
        metrics.increment('summ', baz=None)

        metrics.add_histogram_value('hgram', 1.0)
        metrics.add_histogram_value('hgram', 1.0, user='test')
        metrics.add_histogram_value('hgram', 1.0, foo=1, bar=2)
        metrics.add_histogram_value('hgram', 1.0, baz=None)

        self.assertEqual(clean_data(metrics.to_protobuf()), expected)

    def test_validation(self):
        expected = unistat_pb2.UnistatData()

        metrics = MetricStorage('test_validation')

        metrics.increment('invalid metric name')
        metrics.increment('valid-name', invalid_tag_name_super_long_wow_so_long_much_tag='valid_doge')
        metrics.increment('valid-name', valid_tag=self.NonConvertableValue())
        metrics.increment('valid-name', valid_tag='invalid tag value')

        self.assertEqual(expected, metrics.to_protobuf())

    def test_strict_validation(self):
        metrics = MetricStorage('test_strict_validation', strict_validation=True)
        tests = {
            'invalid metric name': {},
            'valid-name-1': {'invalid_tag_name_super_long_wow_so_long_much_tag': 'valid_doge'},
            'valid-name-2': {'valid_tag': self.NonConvertableValue()},
            'valid-name-3': {'valid_tag': 'invalid tag value'},
        }
        # ToDo: turn into subtests
        for metric, tags in tests.items():
            self.assertRaises(
                ValueError,
                metrics.increment,
                metric,
                **tags
            )

    def test_abnormal_edges(self):
        for abnormal_edges in [
            [1, 2, 3, float('inf')],
            [3, 5, 8, float('NaN'), 9, 11],
            [float('-inf'), 2, 3],
        ]:
            with self.assertRaises(ValueError):
                Histogram(abnormal_edges)

    def test_default(self):
        self.assertEqual(
            Histogram().dump(),
            self.counter_to_histogram_data(collections.Counter())
        )

    def test_capture(self):
        @unistat_capture
        def f(t):
            time.sleep(t)

        for i in range(3):
            f(1.1)

        for i in range(4):
            f(0.65)

        metrics = GlobalMetricStorage()
        self.assertEqual(
            metrics['global-unnamed-f-time_hgram'].dump(),
            self.counter_to_histogram_data(collections.Counter([1.1] * 3 + [0.65] * 4))
        )
