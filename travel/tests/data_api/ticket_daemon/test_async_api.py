import time
from multiprocessing import TimeoutError

import pytest
from mock import ANY, MagicMock
from requests import Timeout

from common.data_api.ticket_daemon.async_api import get_results_async, AsyncResult


def _query(variants=None, statuses=None, exc=None, valid=True):
    query = MagicMock()
    if not valid:
        query.is_valid.return_value = False
    else:
        query.is_valid.return_value = True

    if exc:
        query.collect_variants.side_effect = exc
    else:
        query.collect_variants.return_value = (variants, statuses)
    return query


def test_get_results_async_send_query():
    queries = [_query(), _query()]
    results = get_results_async(queries, send_query=True)
    assert len(results) == 2
    for result in results:
        result.query.is_valid.assert_called_once()
        result.query.execute.assert_called_once_with(ANY, True)


def test_get_results_async_invalid_queries_filtered_out():
    queries = [_query(['foo'], [1]), _query(['bar'], [2], valid=False)]
    results = get_results_async(queries, send_query=True)
    assert len(results) == 1
    assert results[0].query == queries[0]


def test_get_results_async_do_not_send_query():
    query = _query(['foo'], ['querying'])
    results = get_results_async([query])
    assert len(results) == 1
    results[0].query.is_valid.assert_called_once()
    results[0].query.execute.assert_called_once_with(ANY, False)


def test_get_results_async_long_query():
    query = MagicMock()
    query.collect_variants = lambda: time.sleep(1)
    results = get_results_async([query], send_query=True, timeout=0.01)
    assert len(results) == 1
    with pytest.raises(TimeoutError):
        raise results[0].error


@pytest.mark.parametrize('result,expected', [
    (AsyncResult('foo', 'bar', {'foo': 'querying'}), True),
    (AsyncResult('foo', 'bar', {'foo': 'foo'}), False),
    (AsyncResult('foo', 'bar', {}, Timeout()), True)
])
def test_async_result_querying(result, expected):
    assert result.querying == expected
