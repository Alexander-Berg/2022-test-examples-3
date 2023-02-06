import json
import copy

import pytest

from test_utils import load_as_single_line_JSON
from test_utils import load_fixture_file
from test_utils import ExceptionThrowingParser

try:
    # arcadia imports
    import soy_workflow_wrapper as soy_ww  # noqa
except ImportError:
    import wrapper.soy_workflow_wrapper as soy_ww

try:
    import wrapper_lib as wlib  # noqa
except ImportError:
    import wrapper.wrapper_lib as wlib


@pytest.fixture
def raw():
    return load_as_single_line_JSON("raw_dummy_serp.json")


@pytest.fixture
def full():
    return load_as_single_line_JSON("full_dummy_serp.json")


@pytest.fixture
def new_format():
    return load_as_single_line_JSON("new_dummy_serp.json")


@pytest.fixture
def new_format_non_ascii():
    return load_as_single_line_JSON("new_dummy_serp_non-ascii.json")


@pytest.fixture
def raw_wo_content():
    return load_as_single_line_JSON("raw_dummy_serp_without_content.json")


@pytest.fixture
def new_wo_content():
    return load_as_single_line_JSON("new_dummy_serp_without_content.json")


@pytest.fixture
def over_yt_format():
    return load_as_single_line_JSON("over_yt_serp.json")


@pytest.fixture
def over_yt_format_with_timestamp():
    return load_as_single_line_JSON("over_yt_serp_with_timestamp.json")


@pytest.fixture
def over_yt_format_error():
    return load_as_single_line_JSON("over_yt_error.json")


@pytest.fixture
def over_yt_format_with_reqid_column():
    return load_as_single_line_JSON("over_yt_baobab_html_with_reqid_column.json")


@pytest.fixture
def over_yt_format_with_reqid_in_html():
    return load_as_single_line_JSON("over_yt_baobab_html_with_reqid_in_html.json")


@pytest.fixture
def over_yt_format_with_headers():
    return load_as_single_line_JSON("over_yt_baobab_html_with_headers.json")


def compare_output_for_mapper_wrapper(input_string, expected_parsed_serp,
                                      module="base_parsers", classname="JSONSerpParser", ignoreKeys=None):
    ignoreKeys = ignoreKeys or []
    actual_result = get_wrapper_output(input_string, module, classname)
    for k in ignoreKeys:
        if k in actual_result.get('parser-result', {}):
            del actual_result['parser-result'][k]
    assert actual_result == expected_parsed_serp


def test_yson_conversion():
    input_yson_stream = [
        {
            "FetchedResult": None
        },
        {
            "FetchedResult": "<html/>"
        }
    ]
    list(wlib._safe_yson_to_json(input_yson_stream))


def get_wrapper_output(input_string, module="base_parsers", classname="JSONSerpParser"):
    ParserClass = wlib.load_parser_class(module=module, classname=classname)
    parser = ParserClass()
    input_row = json.loads(input_string)
    return wlib.process_row(parser, input_row)


def _create_full_serp_from_parsed_serp(parsed_serp, serp_resources):
    full_serp = copy.deepcopy(parsed_serp)
    full_serp['serp-page']['serp-resources'] = serp_resources
    return full_serp


def test_mapper_wrapper_with_raw_serp_input():
    input_string = load_as_single_line_JSON("raw_dummy_serp.json")
    # Some of the components are replaced by strings for brevity.
    parsed_serp = {
        "class": "ru.yandex.qe.scraper.api.serp.ParsedSerp",
        "serp-page": {
            "class": "ru.yandex.qe.scraper.api.serp.page.ParsedSerpPage",
            "parser-results": {"a": 2},
            "serp-page-attempts": ["an attempt"]
        },
        "status": "status_value",
        "serpRequestExplained": {"some_key": "some_stuff"},
        "serp-request-explained": {"some_key": "some_stuff"}
    }

    serp_resources = {
        "main-page-url": "https://foo.bar/?q=meow",
        "resources": [
            {
                "content": "a resource"
            }
        ]
    }

    expected_raw_parsed_serp = {
        "request-id": 1,
        "parsed-serp": parsed_serp,
        "full-serp": _create_full_serp_from_parsed_serp(parsed_serp, serp_resources)
    }

    compare_output_for_mapper_wrapper(input_string, expected_raw_parsed_serp)


def test_mapper_wrapper_with_raw_serp_input_without_resources():
    input_string = load_as_single_line_JSON("raw_dummy_serp_without_resources.json")
    # Some of the components are replaced by strings for brevity.
    parsed_serp = {
        "class": "ru.yandex.qe.scraper.api.serp.ParsedSerp",
        "serp-page": {
            "class": "ru.yandex.qe.scraper.api.serp.page.ParsedSerpPage",
            "parser-results": {"a": 2},
            "serp-page-attempts": ["an attempt"]
        },
        "status": "status_value",
        "serpRequestExplained": {"some_key": "some_stuff"},
        "serp-request-explained": {"some_key": "some_stuff"}
    }

    expected_raw_parsed_serp = {
        "request-id": 1,
        "parsed-serp": parsed_serp,
        "full-serp": _create_full_serp_from_parsed_serp(parsed_serp, None)
    }

    compare_output_for_mapper_wrapper(input_string, expected_raw_parsed_serp)


def test_mapper_wrapper_with_full_serp_input(full):
    input_string = full
    parsed_serp = {
        "class": "ru.yandex.qe.scraper.api.serp.ParsedSerp",
        "serp-page": {
            "class": "ru.yandex.qe.scraper.api.serp.page.ParsedSerpPage",
            "parser-results": {"a": 2},
            "serp-page-attempts": ["an attempt"]
        },
        "status": "status_value",
        "serpRequestExplained": {"per-query-parameters": {"query-text": "query"}},
        "serp-request-explained": {"per-query-parameters": {"query-text": "query"}}
    }

    full_serp = copy.deepcopy(parsed_serp)

    serp_resourses = {
        "main-page-url": "https://foo.bar/?q=meow",
        "resources": [
            {
                "content": "{\"a\": 2}"
            }
        ]
    }
    full_serp['serp-page']['serp-resources'] = serp_resourses

    expected_full_parsed_serp = {
        "request-id": 1,
        "parsed-serp": parsed_serp,
        "full-serp": full_serp
    }

    compare_output_for_mapper_wrapper(input_string, expected_full_parsed_serp)


def test_mapper_wrapper_with_new_serp_input(new_format):
    input_string = new_format
    expected_full_parsed_serp = {
        "request-id": 1,
        "status": "status_value",
        "attempts": [{}],
        "serp-request-explained": {},
        "content": "{\"a\": 2}",
        "parser-result": {"a": 2},
    }
    compare_output_for_mapper_wrapper(input_string, expected_full_parsed_serp)


def test_mapper_wrapper_with_new_serp_input_non_ascii(new_format_non_ascii):
    input_string = new_format_non_ascii
    expected_full_parsed_serp = {
        "request-id": 1,
        "status": "status_value",
        "attempts": [
            {}
        ],
        "serp-request-explained": {},
        "content": u"{\"фу\": \"бар\"}",
        "parser-result": {u"фу": u"бар"},
    }
    compare_output_for_mapper_wrapper(input_string, expected_full_parsed_serp)


def test_mapper_wrapper_with_raw_serp_wo_content(raw_wo_content):
    input_string = raw_wo_content
    assert None is get_wrapper_output(input_string)['parsed-serp']['serp-page']['parser-results']


def test_mapper_wrapper_with_new_serp_wo_contnet(new_wo_content):
    input_string = new_wo_content
    assert None is get_wrapper_output(input_string)['parser-result']


@pytest.mark.freeze_time("2021-01-12")
def test_mapper_wrapper_with_scraper_over_yt_input(over_yt_format):
    input_string = over_yt_format
    expected_result = {
        "id": "875",
        "parser-result": {
            "a": "b",
            "headers": {
                "headers": [],
                "timestamp": 1610409600000
            }
        },
        "query": {
            "c": "d"
        },
        "processed-url": "https://someurl.com",
        "retries": None,
        "engine": None,
        "req_id": None,
        "response_headers": None
    }
    compare_output_for_mapper_wrapper(input_string, expected_result)


def test_mapper_wrapper_with_scraper_over_yt_input_with_timestamp(over_yt_format_with_timestamp):
    input_string = over_yt_format_with_timestamp
    expected_result = {
        "id": "875",
        "parser-result": {
            "a": "b",
            "headers": {
                "headers": [],
                "timestamp": 465130800
            }
        },
        "query": {
            "c": "d"
        },
        "processed-url": "https://someurl.com",
        "retries": None,
        "engine": None,
        "req_id": None,
        "response_headers": None
    }
    compare_output_for_mapper_wrapper(input_string, expected_result)


def test_mapper_wrapper_with_scraper_overy_yt_error(over_yt_format_error):
    expected_result = {
        "id": "1",
        "error": "Failed to acquire AH responses",
        "query": {},
        "retries": None,
        "engine": None,
        "req_id": None,
        "response_headers": None,
        "processed-url": "https://invalid_host.ru/search/?app_host_params=need_debug_info%3D1&debug=dump_sources_answer_stat"
                         "&exp_flags=baobab%3Dexport&flag=scraper_mapper_req_id%3D9&init_meta=use-src-tunneller&init_meta"
                         "=need_debug_info%3D1&init_meta=need_selected_with_address%3D1&init_meta=metasearch_native_protobuf"
                         "&init_meta=has_scraper_mapper%3Dda&init_meta=has_scraper_mapper_web%3Dda&lr=10262&no-tests=1&pc=brotli5"
                         "&reqinfo=scraperoverytID%3D72e71954-31f3dd75-677fdf4f-903d610e&srcrwr=SCRAPER_MAPPER%3Afb-n5846-sas.hahn"
                         ".yt.yandex.net%3A31076%3A10000&srcrwr=SCRAPER_MAPPER%3An5846-sas.hahn.yt.yandex.net%3A31076%3A10000&srcrwr"
                         "=IMAGES_LIKE%3Ainproc%3A//localhost/_subhost/images-like%224173947&text=%D1%81%D0%B8%D0%BD%D0%B2%D0%B8%D1"
                         "%81%D0%BA"
    }
    compare_output_for_mapper_wrapper(over_yt_format_error, expected_result)


@pytest.mark.freeze_time("2021-01-12")
def test_mapper_wrapper_with_scraper_over_yt_with_reqid_column_input(over_yt_format_with_reqid_column):
    input_string = over_yt_format_with_reqid_column
    expected_result = {
        "id": "875",
        "parser-result": {
            "headers": {
                "headers": [],
                "timestamp": 1610409600000
            },
            "components": [],
            "json.queryParams": {
                "ProximaPredict": {},
                "FreshDetector": {}
            },
            "urls.cleanUrl": "https://someurl.com",
            "text.requestId": "reqid_from_column"
        },
        "query": {
            "c": "d"
        },
        "processed-url": "https://someurl.com",
        "retries": None,
        "engine": None,
        "req_id": "reqid_from_column",
        "response_headers": None
    }
    compare_output_for_mapper_wrapper(input_string, expected_result,
                                      module="yandex_baobab.yandex_baobab_html_parser",
                                      classname="YandexBaobabHTMLParser",
                                      ignoreKeys=['json.WebKukaInfo'])


@pytest.mark.freeze_time("2021-01-12")
def test_mapper_wrapper_with_scraper_over_yt_with_reqid_in_html_input(over_yt_format_with_reqid_in_html):
    input_string = over_yt_format_with_reqid_in_html
    expected_result = {
        "id": "875",
        "parser-result": {
            "headers": {
                "headers": [],
                "timestamp": 1610409600000
            },
            "components": [],
            "json.queryParams": {
                "ProximaPredict": {},
                "FreshDetector": {}
            },
            "urls.cleanUrl": "https://someurl.com",
            "text.requestId": "reqid_from_html"
        },
        "query": {
            "c": "d"
        },
        "processed-url": "https://someurl.com",
        "retries": None,
        "engine": None,
        "req_id": None,
        "response_headers": None
    }
    compare_output_for_mapper_wrapper(input_string, expected_result,
                                      module="yandex_baobab.yandex_baobab_html_parser",
                                      classname="YandexBaobabHTMLParser",
                                      ignoreKeys=['json.WebKukaInfo'])


@pytest.mark.freeze_time("2021-01-12")
def test_mapper_wrapper_with_scraper_over_yt_with_headers_input(over_yt_format_with_headers):
    input_string = over_yt_format_with_headers
    expected_result = {
        "id": "875",
        "parser-result": {
            "headers": {
                "headers": [],
                "timestamp": 1610409600000
            },
            "components": [],
            "json.queryParams": {
                "ProximaPredict": {},
                "FreshDetector": {}
            },
            "urls.cleanUrl": "https://someurl.com",
            "text.requestId": "reqid_from_html"
        },
        "query": {
            "c": "d"
        },
        "processed-url": "https://someurl.com",
        "retries": None,
        "engine": None,
        "req_id": None,
        "response_headers": [
            {
                "name": "First_name",
                "value": "First_value"
            },
            {
                "name": "Second_name",
                "value": "Second_value"
            }
        ]
    }
    compare_output_for_mapper_wrapper(input_string, expected_result,
                                      module="yandex_baobab.yandex_baobab_html_parser",
                                      classname="YandexBaobabHTMLParser",
                                      ignoreKeys=['json.WebKukaInfo'])


def test_mapper_wrapper_ignore_table_switch():
    assert get_wrapper_output('{"$value": null}') is None


def test_empty_row():
    with pytest.raises(ValueError):
        get_wrapper_output('{}')


def test_saag():
    directory = "yt_mapper_wrapper_test_data"
    results = get_saag_results(
        "soy_workflow_reduce_input.json",
        "base_parsers", "JSONSerpParser",
        "ssr_baobab_config.json"
    )
    assert len(results) == 1, "Expected one line, but get %s" % results

    result = results[0]
    expected_result = json.loads(load_fixture_file(directory, "soy_workflow_output.json"))
    # Uncomment to regenerate "soy_workflow_output.json" (!!!only if you really sure the file must be regenerated!!!)
    # with open('soy_workflow_output.json', 'w') as outfile:
    #    json.dump(result, outfile)
    assert expected_result == result


@pytest.mark.freeze_time("2021-01-12")
def test_saag_missing_content():
    directory = "yt_mapper_wrapper_test_data"
    results = get_saag_results(
        "soy_workflow_reduce_input_missing_content.json",
        "yandex_images_market_json_parser", "YandexImagesMarketJSONParser",
        "ssr_yandex-images-alice-market-json_config.json"
    )
    assert len(results) == 2
    # Uncomment to regenerate "soy_workflow_output_missing_content.json" (!!!only if you really sure the file must be regenerated!!!)
    expected_output_filename = 'soy_workflow_output_missing_content_output.json'
    # with open(expected_output_filename, 'w') as outfile:
    #     json.dump(results, outfile)
    expected_result = json.loads(load_fixture_file(directory, expected_output_filename))
    assert expected_result == results


def check_process_stream_saag(tmpdir_factory, rows):
    results0 = get_saag_results(
        "soy_workflow_reduce_input.json",
        "yandex_baobab.yandex_baobab_parser", "YandexBaobabParser",
        "ssr_baobab_config.json"
    )

    config_path = str(tmpdir_factory.mktemp("configs").join('config.json'))
    with open(config_path, 'w') as temp_conf:
        temp_conf.write(load_fixture_file('yt_mapper_wrapper_test_data', 'ssr_baobab_config.json'))

    results1 = list(wlib._process_stream(rows, "yandex_baobab.yandex_baobab_parser", "YandexBaobabParser",
                                         saag=True,
                                         saag_config=config_path,
                                         forced_request_start_time='1970-01-01T00:00:00.000Z'))

    assert results0 == results1


@pytest.mark.freeze_time("1970-01-01")
def test_process_stream_only_responses(tmpdir_factory):
    table_rows = ['{"$attributes":{"table_index":0},"$value":null}'] + get_rows_from_file(
        "soy_workflow_reduce_input.json")
    check_process_stream_saag(tmpdir_factory, rows=table_rows)


def get_rows_from_file(input_filename):
    directory = "yt_mapper_wrapper_test_data"
    lines = load_fixture_file(directory, input_filename).splitlines()
    return lines


def get_saag_results(input_filename, parser_module, parser_class, config_filename, saag_format="SCRAPER"):
    directory = "yt_mapper_wrapper_test_data"
    lines = load_fixture_file(directory, input_filename).splitlines()
    rows = [json.loads(row) for row in lines]
    parser = wlib.load_parser_class(parser_module, parser_class)()
    config = json.loads(load_fixture_file(directory, config_filename))
    results = []
    for response in rows:
        results.extend(list(soy_ww.process_scraper_graph_rows(parser, response, saag_format, config,
                                                              forced_request_start_time='1970-01-01T00:00:00.000Z')))
    return results


def test_saag_wo_config():
    with pytest.raises(ValueError) as excinfo:
        next(wlib._process_stream([], "yandex_baobab.yandex_baobab_parser", "YandexBaobabParser", saag=True, saag_config=None))
    assert str(excinfo.value) == "--config expected in Scraper as a graph mode."


def test_extract_region_id():
    url = "https://yandex.az/search/" \
          "?exp_flags=enable-t-classes%3D1&exp_flags=baobab%3Dtree-new&lr=10253&no-tests=1&numdoc=10&" \
          "pron=scrape-serg-v-serg-v-1572598823713&reqinfo=scrape-serg-v-serg-v-1572598823713&" \
          "reqinfo=scraperoverytID%3D99e8676d-da386e29-415e5a3-734faa21&text=google.az"
    assert soy_ww.extract_region_id(url) == 10253


def test_parser_exception_for_soy_output(over_yt_format):
    parser = ExceptionThrowingParser()
    wrapper_output = wlib.process_row(parser, json.loads(over_yt_format))
    assert wrapper_output['parser-result'] is None
    assert "ThisIsAnErrorFromParser" in wrapper_output['error']
