# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import pytest

from jurassic_lib.prometheus.client import MetricsClient
from jurassic_lib.prometheus.metrics import Metric
from jurassic_lib.prometheus.mock import MetricsClientMock
from jurassic_protoc.h import HistogramData, Bin

METRICS_TEXT = '''
# HELP rex_antirobot_captcha_redirects The amount of redirects to captcha.
# TYPE rex_antirobot_captcha_redirects counter
rex_antirobot_captcha_redirects 1000
# HELP rex_antirobot_read_spravkas The amount of read spravkas.
# TYPE rex_antirobot_read_spravkas counter
rex_antirobot_read_spravkas 2000
# HELP rex_antirobot_spravkas_total The amount of available spravkas at the start.
# TYPE rex_antirobot_spravkas_total counter
rex_antirobot_spravkas_total 3000
# HELP rex__nvhost_nsk__Maps__requests_failed An amount of failed requests in the project Maps.
# TYPE rex__nvhost_nsk__Maps__requests_failed counter
rex__nvhost_nsk__Maps__requests_failed 0
# HELP rex__nvhost_nsk__Maps__requests_total A total amount of requests in the project Maps.
# TYPE rex__nvhost_nsk__Maps__requests_total counter
rex__nvhost_nsk__Maps__requests_total 2495
# HELP rex__nvhost_nsk__Market__requests_failed An amount of failed requests in the project Market.
# TYPE rex__nvhost_nsk__Market__requests_failed counter
rex__nvhost_nsk__Market__requests_failed 1
# HELP rex__nvhost_nsk__Market__requests_total A total amount of requests in the project Market.
# TYPE rex__nvhost_nsk__Market__requests_total counter
rex__nvhost_nsk__Market__requests_total 425
# HELP rex_email_total The total amount of processed Email messages.
# TYPE rex_email_total counter
rex_email_total{status="fail"} 100
rex_email_total{status="sent"} 200
# HELP rex_task_request_duration_seconds A duration of a task request.
# TYPE rex_task_request_duration_seconds histogram
rex_task_request_duration_seconds_bucket{type="dns",le="0.3"} 126
rex_task_request_duration_seconds_bucket{type="dns",le="0.7"} 126
rex_task_request_duration_seconds_bucket{type="dns",le="1"} 126
rex_task_request_duration_seconds_bucket{type="dns",le="1.5"} 126
rex_task_request_duration_seconds_bucket{type="dns",le="2"} 126
rex_task_request_duration_seconds_bucket{type="dns",le="3"} 126
rex_task_request_duration_seconds_bucket{type="dns",le="4"} 126
rex_task_request_duration_seconds_bucket{type="dns",le="5"} 126
rex_task_request_duration_seconds_bucket{type="dns",le="+Inf"} 126
rex_task_request_duration_seconds_sum{type="dns"} 11.586353027999996
rex_task_request_duration_seconds_count{type="dns"} 126
rex_task_request_duration_seconds_bucket{type="http",le="0.3"} 11
rex_task_request_duration_seconds_bucket{type="http",le="0.7"} 222
rex_task_request_duration_seconds_bucket{type="http",le="1"} 255
rex_task_request_duration_seconds_bucket{type="http",le="1.5"} 282
rex_task_request_duration_seconds_bucket{type="http",le="2"} 292
rex_task_request_duration_seconds_bucket{type="http",le="3"} 295
rex_task_request_duration_seconds_bucket{type="http",le="4"} 296
rex_task_request_duration_seconds_bucket{type="http",le="5"} 296
rex_task_request_duration_seconds_bucket{type="http",le="+Inf"} 296
rex_task_request_duration_seconds_sum{type="http"} 182.62850017800005
rex_task_request_duration_seconds_count{type="http"} 296
'''


@pytest.mark.parametrize('text,metrics', (
    (METRICS_TEXT, {
        'rex_antirobot_read_spravkas': 2000,
        'rex_antirobot_spravkas_total': 3000,
    }),
))
def test_metric_parse_text(text, metrics):
    assert Metric.parse_text(text) == metrics


@pytest.mark.parametrize('text,metrics', (
        (METRICS_TEXT, ({
            'rex_antirobot_captcha_redirects_summ': 1000,
            'rex_antirobot_read_spravkas_summ': 2000,
            'rex_antirobot_spravkas_total_summ': 3000,
            'rex_email_total__status_fail_summ': 100,
            'rex_email_total__status_sent_summ': 200,
            'rex__nvhost_nsk__Maps__requests_failed_summ': 0,
            'rex__nvhost_nsk__Maps__requests_total_summ': 2495,
            'rex__nvhost_nsk__Market__requests_failed_summ': 1,
            'rex__nvhost_nsk__Market__requests_total_summ': 425,
            'rex_task_request_duration_seconds_sum__type_dns_summ': 11.586353027999996,
            'rex_task_request_duration_seconds_count__type_dns_summ': 126,
            'rex_task_request_duration_seconds_sum__type_http_summ': 182.62850017800005,
            'rex_task_request_duration_seconds_count__type_http_summ': 296,
        }, {
            'rex_task_request_duration_seconds__type_dns_hgram': HistogramData(bins=[
                Bin(edge=0.0, value=126),
                Bin(edge=0.3, value=0),
                Bin(edge=0.7, value=0),
                Bin(edge=1, value=0),
                Bin(edge=1.5, value=0),
                Bin(edge=2, value=0),
                Bin(edge=3, value=0),
                Bin(edge=4, value=0),
                Bin(edge=5, value=0),
            ]),
            'rex_task_request_duration_seconds__type_http_hgram': HistogramData(bins=[
                Bin(edge=0.0, value=11),
                Bin(edge=0.3, value=211),
                Bin(edge=0.7, value=11),
                Bin(edge=1, value=33),
                Bin(edge=1.5, value=27),
                Bin(edge=2, value=10),
                Bin(edge=3, value=3),
                Bin(edge=4, value=1),
                Bin(edge=5, value=0),
            ]),
        })),
))
def test_metrics_client_add_prometheus_metrics(text, metrics):
    text_numerical, text_histograms = MetricsClient.get_prometheus_metrics(text)
    metrics_numerical, metrics_histograms = metrics
    assert sorted(text_histograms) == sorted(metrics_histograms)
    assert sorted(text_numerical) == sorted(metrics_numerical)


@pytest.mark.parametrize('hostname,short_hostname', (
    ('ya.ru', 'ya'),
    ('YA.RU', 'ya'),
    ('yandex-team.ru', 'yandex_team'),
    ('YANDEX-TEAM.RU', 'yandex_team'),
    ('en.wikipedia.org', 'en'),
    ('w3schools.com', 'w3schools'),
    ('W3SCHOOLS.COM', 'w3schools'),
    ('nvhost-nsk.extmon.yandex.net', 'nvhost_nsk'),
    ('[2a02:6b8:b011:4401:1e1b:dff:fe83:e441]', '2a02_6b8_b011_4401_1e1b_dff_fe83_e441'),  # for the unlikely event of IPv6 in AgentConfiguration hostname
))
def test_shorten_hostname(hostname, short_hostname):
    assert Metric.shorten_hostname(hostname) == short_hostname


@pytest.mark.parametrize('hostname,name', (
    (None, 'rex__up_annn'),
    ('test', 'rex__test__up_annn'),
))
def test_get_up_metric_name(hostname, name):
    assert Metric.get_up_metric_name(hostname) == name


@pytest.mark.parametrize('name,include_default,expected', (
    ('rex__up_annn', False, True),
    ('rex__up_annn', True, False),
    ('rex__test__up_annn', False, True),
    ('rex__test__up_annn', True, True),
))
def test_is_up_metric_name(name, include_default, expected):
    assert Metric.is_up_metric_name(name, include_default) == expected


class TestMetricsClientMock(object):
    def test_from_metrics_overrides_get_metrics(self):
        client = MetricsClientMock.from_metrics({})
        assert client.get_spravkas_remainder() == 0

        client = MetricsClientMock.from_metrics({
            Metric.spravkas_total.value: 99,
        })
        assert client.get_spravkas_remainder() == 99
