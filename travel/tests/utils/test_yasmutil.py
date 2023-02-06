# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import json

import mock
import pytest
from django.conf import settings
from requests import HTTPError

import common
from common.tester.utils.replace_setting import replace_setting
from common.utils.yasmutil import (Metric, YasmMetricSender, YasmError, MeasurableDecorator, MeasurableDecoratorError,
                                   get_buckets, get_bucket_values_generator)


@pytest.fixture(scope='module', autouse=True)
def geo_tag():
    # Чтобы не дергался resource_explorer
    with mock.patch.object(YasmMetricSender, '_GEO_TAG', new='local'):
        yield


class TestMetric(object):
    @pytest.mark.parametrize('metric, prefix, expected', [
        (Metric('m_name', 10, 'axhh'), None, {'name': 'm_name_axhh', 'val': 10}),
        (Metric('m_name', 10, 'axhh'), 'sfx', {'name': 'sfx.m_name_axhh', 'val': 10})
    ])
    def test_to_dict(self, metric, prefix, expected):
        assert metric.to_dict(prefix) == expected


class TestYasmMetricSender(object):
    YASM_URL = 'http://localhost:{}/'.format(settings.YASMAGENT_PORT)
    YASM_TIER = settings.PKG_VERSION
    YASM_ITYPE = settings.YASMAGENT_ITYPE
    YASM_PRJ = settings.YASMAGENT_PROJECT

    @pytest.mark.parametrize('tags, prefix, expected', [
        (
            None,
            None,
            [{
                'name': 'foo_name_axxx',
                'val': 10,
                'tags': {'geo': 'local', 'itype': YASM_ITYPE, 'ctype': 'development', 'prj': YASM_PRJ,
                         'tier': YASM_TIER}
            }]
        ),
        (
            None,
            'prfx',
            [{
                'name': 'prfx.foo_name_axxx',
                'val': 10,
                'tags': {'geo': 'local', 'itype': YASM_ITYPE, 'ctype': 'development', 'prj': YASM_PRJ,
                         'tier': YASM_TIER}
            }]
        ),
        (
            {'geo': 'sas', 'itype': 'rasp_back', 'ctype': 'development', 'prj': 'rasp_back', 'tier': 'rasp'},
            None,
            [{
                'name': 'foo_name_axxx',
                'val': 10,
                'tags': {'geo': 'sas', 'itype': 'rasp_back', 'ctype': 'development', 'prj': 'rasp_back', 'tier': 'rasp'}
            }]
        ),
    ])
    def test_send_one_ok(self, httpretty, tags, prefix, expected):
        sender = YasmMetricSender(tags=tags, prefix=prefix)
        httpretty.register_uri(httpretty.POST, self.YASM_URL, status=206, body='{"status": "ok"}')
        sender.send_one(Metric('foo_name', 10, 'axxx'))
        assert json.loads(httpretty.last_request.body) == expected

    @pytest.mark.parametrize('code, body, expected', [
        (400, '{"status": "error", "error_code": 1, "error": "foo"}', YasmError),
        (500, 'Trash received!', HTTPError)
    ])
    def test_send_one_failed(self, httpretty, code, body, expected):
        sender = YasmMetricSender()
        with pytest.raises(expected):
            httpretty.register_uri(httpretty.POST, self.YASM_URL, status=code, body=body)
            sender.send_one(Metric('foo', 1, 'axxx'))

    @pytest.mark.parametrize('tags, prefix, expected', [
        (
            None,
            None,
            [{
                'values': [
                    {'name': 'foo_name_axxx', 'val': 10},
                    {'name': 'bar_name_hxxx', 'val': 20}
                ],
                'tags': {'geo': 'local', 'itype': YASM_ITYPE, 'ctype': 'development', 'prj': YASM_PRJ,
                         'tier': YASM_TIER}
            }]
        ),
        (
            None,
            'prfx',
            [{
                'values': [
                    {'name': 'prfx.foo_name_axxx', 'val': 10},
                    {'name': 'prfx.bar_name_hxxx', 'val': 20}
                ],
                'tags': {'geo': 'local', 'itype': YASM_ITYPE, 'ctype': 'development', 'prj': YASM_PRJ,
                         'tier': YASM_TIER}
            }]
        ),
        (
            {'geo': 'sas', 'itype': 'rasp_back', 'ctype': 'development', 'prj': 'rasp_back', 'tier': 'rasp'},
            None,
            [{
                'values': [
                    {'name': 'foo_name_axxx', 'val': 10},
                    {'name': 'bar_name_hxxx', 'val': 20}
                ],
                'tags': {'geo': 'sas', 'itype': 'rasp_back', 'ctype': 'development', 'prj': 'rasp_back', 'tier': 'rasp'}
            }]
        ),
    ])
    def test_send_many_ok(self, httpretty, tags, prefix, expected):
        sender = YasmMetricSender(tags=tags, prefix=prefix)
        httpretty.register_uri(httpretty.POST, self.YASM_URL, status=206, body='{"status": "ok"}')
        sender.send_many([Metric('foo_name', 10, 'axxx'), Metric('bar_name', 20, 'hxxx')])
        assert json.loads(httpretty.last_request.body) == expected

    @pytest.mark.parametrize('code, body, expected', [
        (400, '{"status": "error", "error_code": 1, "error": "foo"}', YasmError),
        (500, 'Trash received!', HTTPError)
    ])
    def test_send_many_failed(self, httpretty, code, body, expected):
        sender = YasmMetricSender()
        with pytest.raises(expected):
            httpretty.register_uri(httpretty.POST, self.YASM_URL, status=code, body=body)
            sender.send_many([Metric('foo', 1, 'axxx'), Metric('bar', 2, 'axxx')])


class TestGetBuckets(object):
    @pytest.mark.parametrize('first_bucket_size, bucket_multiplier, bucket_count, expected', [
        (1, 2, 1, [1]),
        (1, 2, 5, [1, 2, 4, 8, 16]),
        (123, 7, 8, [123, 861, 6027, 42189, 295323, 2067261, 14470827, 101295789]),
    ])
    def test_measurable_decorator_with_buckets(self, first_bucket_size, bucket_multiplier, bucket_count, expected):
        assert get_buckets(first_bucket_size, bucket_multiplier, bucket_count) == expected


class TestGetBucketValuesGenerator(object):
    @pytest.mark.parametrize('buckets, value, expected', [
        ([1, 2, 4, 8, 16], 5, [[0, 0], [1, 0], [2, 0], [4, 1], [8, 0]]),
        ([1, 2, 4, 8, 16], 1, [[0, 0], [1, 1], [2, 0], [4, 0], [8, 0]]),
        ([1, 2, 4, 8, 16], 2, [[0, 0], [1, 0], [2, 1], [4, 0], [8, 0]]),
        ([1, 2, 4, 8, 16], 105, [[0, 0], [1, 0], [2, 0], [4, 0], [8, 0]]),
    ])
    def test_measurable_decorator_with_buckets(self, buckets, value, expected):
        bucket_generator = get_bucket_values_generator(buckets)
        assert list(bucket_generator(value)) == expected


class TestMeasurableDecorator(object):
    @pytest.mark.parametrize('setting_value, endpoint_name, expected', [
        (False, None, []),
        (True, None, [
            mock.call(prefix=None),
            mock.call().send_many([Metric(name='func_to_decorate.timings', value=[[0, 1]], suffix='ahhh')])]),
        (True, 'endpoint_name', [
            mock.call(prefix=None),
            mock.call().send_many([Metric(name='endpoint_name.timings', value=[[0, 1]], suffix='ahhh')])])
    ])
    def test_setting_enable_disable(self, setting_value, endpoint_name, expected):
        def func_to_decorate(x):
            return x

        with mock.patch.object(common.utils.yasmutil, 'YasmMetricSender') as m_yasm_metric_sender, \
                replace_setting('YASMAGENT_ENABLE_MEASURABLE', setting_value):
            measurable = MeasurableDecorator(endpoint_name, buckets=[10])
            func_to_test = measurable(func_to_decorate)
            assert func_to_test('foo') == 'foo'
            assert m_yasm_metric_sender.mock_calls == expected

    @replace_setting('YASMAGENT_ENABLE_MEASURABLE', True)
    @mock.patch.object(common.utils.yasmutil, 'YasmMetricSender')
    def test_handle_error_override(self, m_yasm_metric_sender):
        class measurable(MeasurableDecorator):
            def _handle_error(self, exc):
                return [Metric(self._name('foo_error'), 1, 'fuuu')]

        @measurable(buckets=[10])
        def func_to_decorate():
            raise Exception()

        with pytest.raises(Exception):
            func_to_decorate()
        assert m_yasm_metric_sender.mock_calls == [
            mock.call(prefix=None),
            mock.call().send_many([
                Metric(name='func_to_decorate.foo_error', value=1, suffix='fuuu'),
                Metric(name='func_to_decorate.timings', value=[[0, 1]], suffix='ahhh')
            ])
        ]

    def test_should_not_allow_to_use_instance_twice(self):
        measurable = MeasurableDecorator()
        measurable(lambda: None)
        with pytest.raises(MeasurableDecoratorError):
            measurable(lambda: None)

    @replace_setting('YASMAGENT_ENABLE_MEASURABLE', True)
    @mock.patch.object(common.utils.yasmutil, 'YasmMetricSender')
    def test_default_prefix_used(self, m_yasm_metric_sender):
        class measurable(MeasurableDecorator):
            prefix = 'some_prefix'

        @measurable(buckets=[10])
        def func_to_decorate():
            pass

        func_to_decorate()
        assert m_yasm_metric_sender.mock_calls == [
            mock.call(prefix='some_prefix'),
            mock.call().send_many([Metric(name='func_to_decorate.timings', value=[[0, 1]], suffix='ahhh')])
        ]

    @replace_setting('YASMAGENT_ENABLE_MEASURABLE', True)
    @mock.patch.object(common.utils.yasmutil.YasmMetricSender, 'send_many')
    def test_send_failed(self, m_send_many):
        m_send_many.side_effect = Exception('foo')
        measurable = MeasurableDecorator()
        func_to_test = measurable(lambda: True)

        assert func_to_test() is True

    @pytest.mark.parametrize('buckets, spent_time, expected', [
        ([1000, 2000, 4000, 8000, 16000], 6, [[0, 0], [1000, 0], [2000, 0], [4000, 1], [8000, 0]]),
        ([1000, 2000, 4000, 8000, 16000], 0, [[0, 1], [1000, 0], [2000, 0], [4000, 0], [8000, 0]]),
        ([1000, 2000, 4000, 8000, 16000], 1, [[0, 0], [1000, 1], [2000, 0], [4000, 0], [8000, 0]]),
        ([3000, 6000, 12000, 24000, 48000], 5, [[0, 0], [3000, 1], [6000, 0], [12000, 0], [24000, 0]]),
    ])
    def test_measurable_decorator_with_buckets(self, buckets, spent_time, expected):
        def func_to_decorate(x):
            return x

        expected = [
            mock.call(prefix=None),
            mock.call().send_many([Metric(
                name='func_to_decorate.timings',
                value=expected,
                suffix='ahhh')]
            )
        ]

        with mock.patch.object(common.utils.yasmutil, 'YasmMetricSender') as m_yasm_metric_sender, \
                replace_setting('YASMAGENT_ENABLE_MEASURABLE', True), \
                mock.patch('time.time', side_effect=[0, spent_time]):
            measurable = MeasurableDecorator(None, buckets=buckets)
            func_to_test = measurable(func_to_decorate)
            assert func_to_test('foo') == 'foo'
            assert m_yasm_metric_sender.mock_calls == expected
