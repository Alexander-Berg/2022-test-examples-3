try:
    # arcadia imports
    import soy_workflow_wrapper as soy_ww  # noqa
except ImportError:
    import wrapper.soy_workflow_wrapper as soy_ww

from test_utils import read_json_from_fixture_file, ExceptionThrowingParser


def test_create_resources_no_Headers():
    soy_ww._create_headers(dict())


def test_create_resources_None_Headers():
    soy_ww._create_headers({'Headers': None})


def test_create_attempts_None_FetchedResult():
    soy_ww._create_attempts({'FetchedResult': None, 'Url': None, 'Error': None}, None)


def test_create_attempts_None_FetchDurationMilliSeconds():
    attempt = soy_ww._create_attempts({'FetchDurationMilliSeconds': None, 'Url': None, 'Error': None}, None)
    assert attempt[0]['response-record']['receiving-time'] == 0


def test_create_attempts_No_FetchDurationMilliSeconds():
    attempt = soy_ww._create_attempts({'Url': None, 'Error': None}, None)
    assert attempt[0]['response-record']['receiving-time'] == 0


def test_create_attempts_Some_FetchDurationMilliSeconds():
    attempt = soy_ww._create_attempts({'FetchDurationMilliSeconds': 11, 'Url': None, 'Error': None}, None)
    assert attempt[0]['response-record']['receiving-time'] == 11


def test_process_scraper_graph_rows_exception_handling():
    response = read_json_from_fixture_file("yt_mapper_wrapper_test_data", "soy_workflow_reduce_input.json")
    config = read_json_from_fixture_file("yt_mapper_wrapper_test_data", "ssr_baobab_config.json")
    parser = ExceptionThrowingParser()
    results = list(soy_ww.process_scraper_graph_rows(parser, response, "METRICS", config))
    assert len(results) == 3
    assert results[0] == {'$attributes': {'table_index': 1}, '$value': None}
    payload = results[1]
    assert payload['exceptionClass'] == "ThisIsAnErrorFromParser"
    assert "ThisIsAnErrorFromParser" in payload['message']
    assert payload['parserClass'] == "ExceptionThrowingParser"
    assert payload['reqId'] == "0"
    assert payload['stackTrace'].startswith("Traceback (most recent call last)")
    assert payload['serpRequestExplained'] == soy_ww._merge_request(
        response,
        config
    )
    assert results[2] == {'$attributes': {'table_index': 0}, '$value': None}
