import pytest

from row import Serp, convert_row_to_metrics_format, CLEAN_URL, streaming_json_data
from test_utils import create_serp, create_failed_serp


QUERY = {
    "mapInfo": None,
    "country": "KZ",
    "region": {
        "name": None,
        "id": 162
    },
    "text": "ктулху",
    "device": "DESKTOP",
    "uid": 12345
}

RESPONSE_HEADERS = [
    {
        "name": "first_header_name",
        "value": "first_header_value"
    },
    {
        "name": "second_header_name",
        "value": "second_header_value"
    }
]


def test_minimal():
    serp = create_serp()
    assert serp.uid == '1'
    assert serp.query == {}
    assert serp.result == {}
    assert serp.components == []
    assert str(serp)  # test that representation not fails


def test_invalid():
    with pytest.raises(KeyError):
        Serp.from_row({})


def test_error():
    error = 'Test error'
    serp = create_failed_serp(error)
    assert serp.error == error


def test_convert_to_metrics():
    row = {
        "query": QUERY,
        "id": "884",
        "parser-result": {},
        'req_id': 'some_reqid',
        'response_headers': RESPONSE_HEADERS
    }
    result = convert_row_to_metrics_format(row)
    assert result['query']['text'] == "ктулху"
    assert result['text.requestId'] == "some_reqid"
    assert result['json.responseHeaders'] == RESPONSE_HEADERS
    assert 'ytrawlocation' not in result


def test_convert_to_metrics_error_row():
    row = {
        "query": QUERY,
        "id": "1",
        "error": "error message",
        'req_id': 'some_reqid',
        'response_headers': RESPONSE_HEADERS
    }
    result = convert_row_to_metrics_format(row)
    assert result['query']['text'] == "ктулху"
    assert result['headers']['errorMessage'] == "error message"
    assert result['text.requestId'] == "some_reqid"
    assert result['json.responseHeaders'] == RESPONSE_HEADERS
    assert 'ytrawlocation' not in result


def test_convert_to_metrics_with_yt_location():
    row = {
        "query": QUERY,
        "id": "884",
        "parser-result": {}
    }
    host = "hahn.yt.yandex.net"
    path = "//tmp/some_table_with_raw_data"
    result = convert_row_to_metrics_format(row, host, path)
    assert result['query']['text'] == "ктулху"
    assert result['ytrawlocation']['id'] == "884"
    assert result['ytrawlocation']['path'] == path
    assert result['ytrawlocation']['host'] == host


def test_result_modifications_corresponds_to_row():
    """
    Modifications to result should modify original dictionary.
    Calculation.map_row_group relies on this behavouir.
    """
    row = {
        "query": QUERY,
        "id": "1"
    }
    serp = Serp.from_row(row)
    serp.result['k'] = 'v'
    assert row['parser-result'] == {'k': 'v'}


def test_clean_url_in_headers():
    row = {
        'query': QUERY,
        'id': '1',
        'parser-result': {CLEAN_URL: 'cleanUrl'}
    }
    result = convert_row_to_metrics_format(row)
    assert result['headers']['cleanUrl'] == 'cleanUrl'


def test_retries_default_value():
    row = {
        'query': QUERY,
        'id': '1',
        'parser-result': {CLEAN_URL: 'cleanUrl'}
    }
    result = convert_row_to_metrics_format(row)
    assert result['double.redownloadAttempts'] is None


def test_retries_given_value_given_headers_without_array():
    row = {
        'query': QUERY,
        'id': '1',
        'parser-result': {CLEAN_URL: 'cleanUrl', 'headers': {'a': 'b'}},
        'retries': 3
    }
    result = convert_row_to_metrics_format(row)
    assert result['double.redownloadAttempts'] == 3


def test_streaming_json_empty():
    rows = ({"k": "v"} for i in range(0, 0))
    stream = streaming_json_data(rows)
    assert next(stream) == "["
    assert next(stream) == "]"


def test_streaming_json_single():
    rows = ({"k": "v"} for i in range(0, 1))
    stream = streaming_json_data(rows)
    assert next(stream) == "["
    assert next(stream) == '{"k": "v"}'
    assert next(stream) == "]"


def test_streaming_json_multiple():
    rows = ({"k": i} for i in range(0, 3))
    stream = streaming_json_data(rows)
    assert next(stream) == "["
    assert next(stream) == '{"k": 0}'
    assert next(stream) == ','
    assert next(stream) == '{"k": 1}'
    assert next(stream) == ','
    assert next(stream) == '{"k": 2}'
    assert next(stream) == "]"


def test_convert_query_param():
    query = QUERY.copy()
    query['params'] = [{'name': 'test', 'value': 'test_value'}]
    row = {
        'query': query,
        'id': '1'
    }
    result = convert_row_to_metrics_format(row)
    assert result['query_param.test'] == 'test_value'
    assert result['serp_query_param.test'] == 'test_value'
