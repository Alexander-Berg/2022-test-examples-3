import pytest
import re

from hamcrest import assert_that, equal_to, contains

from unistat.meters import (
    BucketsCounters,
    Hist,
    make_buckets,
    TskvMonitor,
    TskvLogErrorsCount,
    AccessTskvCount,
    AccessTskvCountByFirstStatusDigit,
    AccessTskvCountByStatus,
    AccessTskvRequestTimeHist,
    HttpClientTskvRequestCountByStatus,
    HttpClientTskvRequestHist,
    HttpClientTskvRequestBytesInHist,
)


@pytest.mark.parametrize(('buckets', 'values', 'expected'), (
    ([], [], []),
    ([0], [0], [[0, 1]]),
    ([0], [1], [[0, 1]]),
    ([1], [0], [[1, 0]]),
    ([1], [0.5], [[1, 0]]),
    ([1], [1], [[1, 1]]),
    ([1], [2], [[1, 1]]),
    ([1, 2], [0], [[1, 0], [2, 0]]),
    ([1, 2], [0.5], [[1, 0], [2, 0]]),
    ([1, 2], [1], [[1, 1], [2, 0]]),
    ([1, 2], [1.5], [[1, 1], [2, 0]]),
    ([1, 2], [2], [[1, 0], [2, 1]]),
    ([1, 2], [3], [[1, 0], [2, 1]]),
    ([1, 2], [0, 0.5, 1, 1.5, 2, 3], [[1, 2], [2, 2]]),
))
def test_hist(buckets, values, expected):
    hist = Hist(buckets=buckets, name='name', get_value=lambda v: v)
    for value in values:
        hist.update(value)
    assert_that(hist.get(), equal_to(['name_hgram', expected]))


@pytest.mark.parametrize(('start', 'bucket_size', 'buckets_count', 'additions', 'expected'), (
    (0, 1, 1, [(0, 1)], [1]),
    (0, 1, 1, [(0, 0)], [0]),
    (0, 1, 1, [(0, 2)], [2]),
    (0, 1, 1, [(0, 1), (0.5, 1)], [2]),
    (0, 1, 1, [(0, 1), (0.5, 1), (1, 1)], [1]),
    (0, 2, 1, [(0, 1), (0.5, 1), (1, 1)], [3]),
    (0, 1, 2, [(0, 1), (0.5, 1), (1, 1)], [1, 2]),
    (0, 1, 2, [(0, 1), (0.5, 1), (1, 0)], [0, 2]),
    (0, 2, 2, [(0, 1), (0.5, 1), (1, 1)], [3, 0]),
    (0, 2, 2, [(0, 1), (1, 1), (2, 1), (3, 1)], [2, 2]),
    (0, 2, 2, [(0, 1), (1, 1), (2, 1), (3, 1), (4, 1)], [1, 2]),
))
def test_buckets_counters(start, bucket_size, buckets_count, additions, expected):
    counters = BucketsCounters(start=start, bucket_size=bucket_size, buckets_count=buckets_count)
    for current_time, value in additions:
        counters.add(current_time=current_time, value=value)
    assert_that(counters.get(), contains(*expected))


def test_make_buckets_should_return_50_buckets():
    assert_that(len(make_buckets(left=5, mid=25, right=40, timeout=50)), equal_to(50))


def test_make_buckets_should_return_buckets_in_ascending_order():
    result = make_buckets(left=5, mid=25, right=40, timeout=50)
    assert_that(result, equal_to(sorted(result)))


def test_make_buckets_should_work_with_float_parameters():
    assert_that(len(make_buckets(left=0.6, mid=4.3, right=4.8, timeout=10)), equal_to(50))


@pytest.mark.parametrize(('record', 'expected'), (
    (
        {'some': 'line'},
        {'unknown': ['err_unknown_summ', 0],
         'db_error': ['err_db_error_summ', 0],
         'net_error': ['err_net_error_summ', 0]}
    ),
    (
        {'level': 'info'},
        {'unknown': ['err_unknown_summ', 0],
         'db_error': ['err_db_error_summ', 0],
         'net_error': ['err_net_error_summ', 0]}
    ),
    (
        {'level': 'error'},
        {'unknown': ['err_unknown_summ', 1],
         'db_error': ['err_db_error_summ', 0],
         'net_error': ['err_net_error_summ', 0]}
    ),
    (
        {'level': 'error', 'category': 'some'},
        {'unknown': ['err_unknown_summ', 1],
         'db_error': ['err_db_error_summ', 0],
         'net_error': ['err_net_error_summ', 0]}
    ),
    (
        {'level': 'error', 'category': 'db'},
        {'unknown': ['err_unknown_summ', 0],
         'db_error': ['err_db_error_summ', 1],
         'net_error': ['err_net_error_summ', 0]}
    ),
    (
        {'level': 'error', 'category': 'net'},
        {'unknown': ['err_unknown_summ', 0],
         'db_error': ['err_db_error_summ', 0],
         'net_error': ['err_net_error_summ', 1]}
    ),
    (
        {'level': 'error', 'category': 'db net'},
        {'unknown': ['err_unknown_summ', 0],
         'db_error': ['err_db_error_summ', 1],
         'net_error': ['err_net_error_summ', 1]}
    ),
))
def test_tskv_errors_counters(record, expected):
    monitors = [
        TskvMonitor(name='db_error',
                    condition=lambda rec: 'db' in rec.get('category', '')),
        TskvMonitor(name='net_error',
                    condition=lambda rec: 'net' in rec.get('category', '')),
    ]
    counters = TskvLogErrorsCount(name_prefix='err', monitors=monitors)
    counters.update(record)
    assert_that(counters.get(), equal_to(expected))


@pytest.mark.parametrize(('records', 'expected'), (
    (
        [{'request': '/ping'}],
        ['xxx_get_summ', 0]
    ),
    (
        [{'request': '/get'}],
        ['xxx_get_summ', 1]
    ),
    (
        [{'request': '/get'}, {'request': '/ping'}],
        ['xxx_get_summ', 1]
    ),
    (
        [{'request': '/get'}, {'request': '/get'}],
        ['xxx_get_summ', 2]
    ),
))
def test_access_tskv_counters(records, expected):
    counters = AccessTskvCount(endpoint='get')
    for record in records:
        counters.update(record)
    assert_that(counters.get(), equal_to(expected))


@pytest.mark.parametrize(('records', 'expected'), (
    (
        [{'request': '/ping', 'status_code': '200'}],
        {}
    ),
    (
        [{'request': '/get', 'status_code': '200'}],
        {2: ['get_2xx_summ', 1]}
    ),
    (
        [
            {'request': '/get', 'status_code': '200'},
            {'request': '/ping', 'status_code': '200'},
        ],
        {2: ['get_2xx_summ', 1]}
    ),
    (
        [
            {'request': '/get', 'status_code': '200'},
            {'request': '/get', 'status_code': '200'},
        ],
        {2: ['get_2xx_summ', 2]}
    ),
    (
        [
            {'request': '/get', 'status_code': '200'},
            {'request': '/get', 'status_code': '500'},
        ],
        {2: ['get_2xx_summ', 1], 5: ['get_5xx_summ', 1]}
    ),
))
def test_access_tskv_count_by_first_status_digit(records, expected):
    counters = AccessTskvCountByFirstStatusDigit(endpoint='get')
    for record in records:
        counters.update(record)
    assert_that(counters.get(), equal_to(expected))


@pytest.mark.parametrize(('records', 'expected'), (
    (
        [{'request': '/ping', 'status_code': '200'}],
        {}
    ),
    (
        [{'request': '/get', 'status_code': '700'}],
        {}
    ),
    (
        [{'request': '/get', 'status_code': '200'}],
        {200: ['get_200_summ', 1]}
    ),
    (
        [
            {'request': '/get', 'status_code': '201'},
            {'request': '/ping', 'status_code': '201'},
        ],
        {201: ['get_201_summ', 1]}
    ),
    (
        [
            {'request': '/get', 'status_code': '503'},
            {'request': '/get', 'status_code': '503'},
        ],
        {503: ['get_503_summ', 2]}
    ),
    (
        [
            {'request': '/get', 'status_code': '499'},
            {'request': '/get', 'status_code': '504'},
        ],
        {499: ['get_499_summ', 1], 504: ['get_504_summ', 1]}
    ),
))
def test_access_tskv_count_by_status(records, expected):
    counters = AccessTskvCountByStatus(endpoint='get')
    for record in records:
        counters.update(record)
    assert_that(counters.get(), equal_to(expected))


@pytest.mark.parametrize(('records', 'expected'), (
    (
        [{'request': '/ping', 'profiler_total': '0.3'}],
        ['access_log_request_get_hgram', [[0, 0L], [0.1, 0L], [0.3, 0L]]]
    ),
    (
        [{'request': '/get', 'profiler_total': '0.3'}],
        ['access_log_request_get_hgram', [[0, 0L], [0.1, 0L], [0.3, 1L]]]
    ),
    (
        [
            {'request': '/get', 'profiler_total': '0.3'},
            {'request': '/get', 'profiler_total': '0.3'},
            {'request': '/get', 'profiler_total': '0.1'},
        ],
        ['access_log_request_get_hgram', [[0, 0L], [0.1, 1L], [0.3, 2L]]]
    ),
    (
        [
            {'request': '/get', 'profiler_total': '0.3'},
            {'request': '/ping', 'profiler_total': '0.3'},
            {'request': '/get', 'profiler_total': '0.1'},
        ],
        ['access_log_request_get_hgram', [[0, 0L], [0.1, 1L], [0.3, 1L]]]
    ),
))
def test_access_tskv_request_time_hist(records, expected):
    counters = AccessTskvRequestTimeHist(
        buckets=(0, 0.1, 0.3),
        endpoint='get'
    )
    for record in records:
        counters.update(record)
    assert_that(counters.get(), equal_to(expected))


@pytest.mark.parametrize(('records', 'expected'), (
    (
        [{'uri': 'chebureck.mail.yandex.net:4443/gate/get', 'status': '200'}],
        {}
    ),
    (
        [{'uri': 'storage.mail.yandex.net:4443/gate/get', 'status': '700'}],
        {}
    ),
    (
        [{'uri': 'storage.mail.yandex.net:4443/gate/get', 'status': '200'}],
        {200: ['storage_http_status_200_summ', 1L]}
    ),
    (
        [
            {'uri': 'storage.mail.yandex.net:4443/gate/get', 'status': '201'},
            {'uri': 'chebureck.mail.yandex.net:4443/gate/get', 'status': '201'},
        ],
        {201: ['storage_http_status_201_summ', 1L]}
    ),
    (
        [
            {'uri': 'storage.mail.yandex.net:4443/gate/get', 'status': '503'},
            {'uri': 'storage.mail.yandex.net:4443/gate/get', 'status': '503'},
        ],
        {503: ['storage_http_status_503_summ', 2L]}
    ),
    (
        [
            {'uri': 'storage.mail.yandex.net:4443/gate/get', 'status': '499'},
            {'uri': 'storage.mail.yandex.net:4443/gate/get', 'status': '504'},
        ],
        {499: ['storage_http_status_499_summ', 1L], 504: ['storage_http_status_504_summ', 1L]}
    ),
))
def test_http_client_tskv_request_count_by_status_storage(records, expected):
    storage_host_request_pattern = re.compile('^(?:https?://)?(storage.mail.yandex.net:4443)/gate')
    counters = HttpClientTskvRequestCountByStatus(
        uri_filter=lambda w: storage_host_request_pattern.search(w) is not None,
        name_prefix='storage_http_status'
    )
    for record in records:
        counters.update(record)
    assert_that(counters.get(), equal_to(expected))


@pytest.mark.parametrize(('records', 'expected'), (
    (
        [{'uri': 'chebureck.mail.yandex.net:80/gate/get', 'total_time': '0.3'}],
        ['storage_timings_hgram', [[0, 0L], [0.1, 0L], [0.3, 0L]]]
    ),
    (
        [{'uri': 'storage.mail.yandex.net:4443/gate/get', 'total_time': '0.3'}],
        ['storage_timings_hgram', [[0, 0L], [0.1, 0L], [0.3, 1L]]]
    ),
    (
        [
            {'uri': 'storage.mail.yandex.net:4443/gate/get', 'total_time': '0.3'},
            {'uri': 'storage.mail.yandex.net:4443/gate/get', 'total_time': '0.3'},
            {'uri': 'storage.mail.yandex.net:4443/gate/get', 'total_time': '0.1'},
        ],
        ['storage_timings_hgram', [[0, 0L], [0.1, 1L], [0.3, 2L]]]
    ),
    (
        [
            {'uri': 'storage.mail.yandex.net:4443/gate/get', 'total_time': '0.3'},
            {'uri': 'chebureck.mail.yandex.net:80/gate/get', 'total_time': '0.3'},
            {'uri': 'storage.mail.yandex.net:4443/gate/get', 'total_time': '0.1'},
        ],
        ['storage_timings_hgram', [[0, 0L], [0.1, 1L], [0.3, 1L]]]
    ),
))
def test_http_client_tskv_request_hist_storage(records, expected):
    storage_host_request_pattern = re.compile('^(?:https?://)?(storage.mail.yandex.net:4443)/gate')
    counters = HttpClientTskvRequestHist(
        name='storage_timings',
        uri_filter=lambda w: storage_host_request_pattern.search(w) is not None,
        buckets=(0, 0.1, 0.3),
    )
    for record in records:
        counters.update(record)
    assert_that(counters.get(), equal_to(expected))


@pytest.mark.parametrize(('records', 'expected'), (
    (
        [{'uri': 'chebureck.mail.yandex.net:80/gate/get', 'bytes_in': '3'}],
        ['storage_bytes_in_hgram', [[0, 0L], [1, 0L], [3, 0L]]]
    ),
    (
        [{'uri': 'storage.mail.yandex.net:4443/gate/get', 'bytes_in': '3'}],
        ['storage_bytes_in_hgram', [[0, 0L], [1, 0L], [3, 1L]]]
    ),
    (
        [
            {'uri': 'storage.mail.yandex.net:4443/gate/get', 'bytes_in': '3'},
            {'uri': 'storage.mail.yandex.net:4443/gate/get', 'bytes_in': '3'},
            {'uri': 'storage.mail.yandex.net:4443/gate/get', 'bytes_in': '1'},
        ],
        ['storage_bytes_in_hgram', [[0, 0L], [1, 1L], [3, 2L]]]
    ),
    (
        [
            {'uri': 'storage.mail.yandex.net:4443/gate/get', 'bytes_in': '3'},
            {'uri': 'chebureck.mail.yandex.net:80/gate/get', 'bytes_in': '3'},
            {'uri': 'storage.mail.yandex.net:4443/gate/get', 'bytes_in': '1'},
        ],
        ['storage_bytes_in_hgram', [[0, 0L], [1, 1L], [3, 1L]]]
    ),
))
def test_http_client_tskv_request_bytes_in_hist_storage(records, expected):
    storage_host_request_pattern = re.compile('^(?:https?://)?(storage.mail.yandex.net:4443)/gate')
    counters = HttpClientTskvRequestBytesInHist(
        name='storage_bytes_in',
        uri_filter=lambda w: storage_host_request_pattern.search(w) is not None,
        buckets=(0, 1, 3),
    )
    for record in records:
        counters.update(record)
    assert_that(counters.get(), equal_to(expected))
