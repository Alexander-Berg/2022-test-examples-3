import logging
from pathlib import Path
from mock import patch
import re
import json

import pytest
import responses

import yt.wrapper as yt

import uploading
import token_utils
from metrics_rest_test_utils import get_headers_stub, get_metrics_token_mock, get_wrong_metrics_token_mock
from test_utils import read_json_lines_test_data, read_text_test_data
import cli_test_utils

LOGGER = logging.getLogger("upload_test")

START_SERPSET_ID = 12345

MOCK_METRICS_URL = "https://metrics-calculation.mock.qloud.yandex-team.ru/api/json"
MOCK_SSC_URL = "https://ssc.mock.qloud.yandex-team.ru/api"

SERPSET_QUERIES_NONEXISTENT_TABLE = "//home/metrics/ytcompare/nonexistent/serpset_queries"
SERP_QUERY_METRICS_NONEXISTENT_TABLE = "//home/metrics/ytcompare/nonexistent/serp_query_metrics"
OBSERVATIONS_NONEXISTENT_TABLE = "//home/metrics/ytcompare/nonexistent/observations"


DEFAULT_LINKS = [
    {
        "name": "Default link",
        "url": "https://metrics.yandex-team.ru"
    }
]


class RequestMock:
    def __init__(self, start=START_SERPSET_ID):
        self.start = start - 1

    def __call__(self, request):
        if request.headers.get('Authorization') == get_headers_stub()['Authorization']:
            self.start += 1
            if request.body:
                for _ in request.body:
                    pass
                # Unless we do this, the rows generator is never consumed.
                # Meaning transformations in generator expressions are not evaluated and
                # their side effects don't take place.
                # Meaning metrics collection, encoding and such never happen.
            return 200, {}, str(self.start)
        else:
            return 401, {}, None


URLS = [
    {'type': responses.POST, 'url_regexp': MOCK_SSC_URL + r'/serpset/create/info'},
    {'type': responses.PUT, 'url_regexp': MOCK_SSC_URL + r'/serpset/\d+/creator'},
    {'type': responses.POST, 'url_regexp': MOCK_METRICS_URL + r'/\d+\?all=true'},
    {'type': responses.PUT, 'url_regexp': MOCK_SSC_URL + r'/serpset/\d+/enable'}
]


def _set_up_yt_proxy():
    yt.config = {
        "proxy":
            {"url": uploading.MOCK_YT_PROXY}
    }


def _set_up_callbacks(get_metrics_token_mock=get_metrics_token_mock):
    token_utils.get_metrics_token = get_metrics_token_mock
    for url in URLS:
        responses.add_callback(
            url['type'],
            re.compile(url['url_regexp']),
            callback=RequestMock(),
            content_type='application/json'
        )


def _check_rows_upload(
    tmp_path,
    rows_file,
    config_file,
    metrics_request_count,
    expected_urls,
    expected_serpset_ids,
    dynamic_tables_cli_argument=False
):
    rows = read_json_lines_test_data(Path("upload") / rows_file)
    config = read_text_test_data(Path("upload") / config_file)
    config_path = tmp_path / "config.json"
    config_path.write_text(config, encoding="utf-8")
    args = _build_args(config_path, dynamic_tables_cli_argument)
    serpset_ids = uploading.upload(rows, args)
    assert serpset_ids == expected_serpset_ids
    assert len(responses.calls) == metrics_request_count
    # checking urls
    assert all((re.fullmatch(expected_url['url_regexp'], actual_call.request.url) for expected_url, actual_call in zip(expected_urls, responses.calls)))
    # checking metadata
    for index, call in enumerate(responses.calls[::4]):
        assert call.request.body
        _check_metadata_upload(json.loads(call.request.body),
                               description=config_file + f'{index}; metrics yt calculation')
    # checking links
    for call in responses.calls[1::4]:
        assert call.request.body
        assert json.loads(call.request.body) == DEFAULT_LINKS


def _check_yt_upload(
    yt_mock,
    expected_queries_count,
    expected_query_text,
    expected_metrics_count=26
):
    call_args_list = yt_mock.call_args_list

    queries_call_for_first_config = call_args_list[0]
    assert queries_call_for_first_config[0][0] == SERPSET_QUERIES_NONEXISTENT_TABLE
    first_config_payload = queries_call_for_first_config[0][1]
    LOGGER.info(f"{first_config_payload=}")
    assert len(first_config_payload) == 1
    queries_table_row = first_config_payload[0]
    assert len(queries_table_row["queries"]) == expected_queries_count
    assert queries_table_row["serpset_id"] == START_SERPSET_ID
    assert queries_table_row["queries"][0]["text"] == expected_query_text
    query_metrics_call_for_first_config = call_args_list[1]
    # query_metrics_call_for_first_config[0] collects positional arguments
    LOGGER.info(f"{query_metrics_call_for_first_config=}")
    assert query_metrics_call_for_first_config[0][0] == SERP_QUERY_METRICS_NONEXISTENT_TABLE
    query_metrics = query_metrics_call_for_first_config[0][1]
    assert len(query_metrics) == expected_metrics_count
    metrics_table_row = query_metrics[0]
    assert len(metrics_table_row["metric_values"]) == expected_queries_count
    return call_args_list


def _check_metadata_upload(metadata, description=None, basket=None, host_id=None, name=None):
    assert metadata['regionalType'] == 'RU'
    assert metadata['evaluationType'] == 'WEB'
    assert metadata['cronSerpDownloadId'] == 102814
    assert metadata['experimentId'] == '01744f55fe6f32a2a25e02f6822009be'
    assert metadata['ytCluster'] == 'gelfand.yt.yandex.net'
    assert metadata['ytRawSerpsTable'] == '//home/qe/scraper/testing/monitoring/rawSerps-7698659a-7fbc-49fe-91bb-a4a25707fb2e'
    assert metadata['date'] == '2020-09-02T18:01:09.933+03:00'
    assert metadata['expirationDate'] == '2020-10-02T18:16:09.530+03:00'
    if description:
        assert metadata['description'] == description
    if basket:
        assert metadata['queriesGroupId'] == basket
    if host_id:
        assert metadata['hostId'] == host_id
    if name:
        assert metadata['name'] == name


def _build_args(config_path, dynamic_tables=False):
    items = [
        "tool upload local --regional RU",
        "--evaluation WEB",
        "--cron 102814",
        "--host Ya.Prod.Monitoring.Testing",
        "--name Ya.Prod.Monitoring.Testing",
        f"--url {MOCK_METRICS_URL}",
        f"--ssc-url {MOCK_SSC_URL}",
        "--ui-url https://metrics.yandex-team.ru/mc/compare",
        "--date 2020-09-02T18:01:09.933+03:00",
        "--ytpath //home/qe/scraper/testing/monitoring/rawSerps-7698659a-7fbc-49fe-91bb-a4a25707fb2e",
        "--ythost gelfand.yt.yandex.net",
        "--expiration-date 2020-10-02T18:16:09.530+03:00",
        "--experiment 01744f55fe6f32a2a25e02f6822009be",
        "--basket 307271"
    ]
    if config_path:
        items.append(f" --config {config_path}")
    if dynamic_tables:
        items.extend([
            "--upload-metrics-to-dynamic-tables",
            "--metrics-table //home/qe/scraper/testing/monitoring/metrics-7698659a-7fbc-49fe-91bb-a4a25707fb2e"
        ])
    command = " ".join(items)
    LOGGER.info(f"Built {command=}")
    return cli_test_utils.check_cli_line(command)


@responses.activate
def test_multiconfig(tmp_path):
    _set_up_callbacks()
    _set_up_yt_proxy()
    with patch("yt.wrapper.insert_rows") as yt_mock:
        _check_rows_upload(
            tmp_path,
            rows_file="input_rows_multi_config.json",
            config_file="config_multi.json",
            metrics_request_count=8,
            expected_urls=URLS + URLS,
            expected_serpset_ids=[START_SERPSET_ID, START_SERPSET_ID + 1]
        )
        yt_mock.assert_not_called()


@responses.activate
def test_multiconfig_with_dynamic_tables(tmp_path):
    _set_up_callbacks()
    _set_up_yt_proxy()

    with patch("yt.wrapper.insert_rows") as yt_mock:
        _check_rows_upload(
            tmp_path,
            rows_file="input_rows_multi_config.json",
            config_file="config_multi_dt_upload.json",
            metrics_request_count=8,
            expected_urls=URLS + URLS,
            expected_serpset_ids=[START_SERPSET_ID, START_SERPSET_ID + 1],
            dynamic_tables_cli_argument=True
        )
        assert yt_mock.call_count == 4

        call_args_list = _check_yt_upload(yt_mock, 3, "кира найтли другая")

        assert call_args_list[2][0][0] == SERPSET_QUERIES_NONEXISTENT_TABLE
        assert call_args_list[3][0][0] == SERP_QUERY_METRICS_NONEXISTENT_TABLE
        second_config_payload = call_args_list[3][0][1]
        LOGGER.info(f"{second_config_payload=}")
        assert second_config_payload[0]["serpset_id"] == START_SERPSET_ID + 1
        assert len(second_config_payload[0]["metric_values"]) == 3


@responses.activate
def test_single_config_with_dynamic_tables(tmp_path):
    _set_up_callbacks()
    _set_up_yt_proxy()
    with patch("yt.wrapper.insert_rows") as yt_mock:
        _check_rows_upload(
            tmp_path,
            rows_file="input_rows_single_config.json",
            config_file="config_single_dt_upload.json",
            metrics_request_count=4,
            expected_urls=URLS,
            expected_serpset_ids=[START_SERPSET_ID],
        )
        assert yt_mock.call_count == 2
        _check_yt_upload(yt_mock, 1, "кира найтли")


@responses.activate
def test_single_config_with_dynamic_tables_with_cli_dt(tmp_path):
    _set_up_callbacks()
    _set_up_yt_proxy()
    with patch("yt.wrapper.insert_rows") as yt_mock:
        _check_rows_upload(
            tmp_path,
            rows_file="input_rows_single_config.json",
            config_file="config_single.json",
            metrics_request_count=4,
            expected_urls=URLS,
            expected_serpset_ids=[START_SERPSET_ID],
            dynamic_tables_cli_argument=True,
        )
        assert yt_mock.call_count == 2
        _check_yt_upload(yt_mock, 1, "кира найтли")


@responses.activate
def test_single_config_with_dynamic_tables_with_config_and_cli_dt(tmp_path):
    # In this case config says don't upload to DTs and cli arg says do.
    # config should win.
    _set_up_callbacks()
    _set_up_yt_proxy()
    with patch("yt.wrapper.insert_rows") as yt_mock:
        _check_rows_upload(
            tmp_path,
            rows_file="input_rows_single_config.json",
            config_file="config_single_dt_upload_false.json",
            metrics_request_count=4,
            expected_urls=URLS,
            expected_serpset_ids=[START_SERPSET_ID],
            dynamic_tables_cli_argument=True,
        )
        assert yt_mock.call_count == 0


@responses.activate
def test_single_config(tmp_path):
    _set_up_callbacks()
    _set_up_yt_proxy()
    with patch("yt.wrapper.insert_rows") as yt_mock:
        _check_rows_upload(
            tmp_path,
            rows_file="input_rows_single_config.json",
            config_file="config_single.json",
            metrics_request_count=4,
            expected_urls=URLS,
            expected_serpset_ids=[START_SERPSET_ID],
            dynamic_tables_cli_argument=False,
        )

        yt_mock.assert_not_called()


@responses.activate
def test_unauthorized(tmp_path):
    _set_up_callbacks(get_metrics_token_mock=get_wrong_metrics_token_mock)
    _set_up_yt_proxy()
    with patch("yt.wrapper.insert_rows") as yt_mock:
        rows = read_json_lines_test_data(Path("upload") / "input_rows_multi_config.json")
        config = read_text_test_data(Path("upload") / "config_multi.json")
        config_path = tmp_path / "config.json"
        config_path.write_text(config, encoding="utf-8")

        args = _build_args(config_path)

        with pytest.raises(Exception):
            uploading.upload(rows, args)

        assert len(responses.calls) == 5

        yt_mock.assert_not_called()


def test_get_dt_paths_default():
    class Args:
        serpset_queries_table = 'a'
        serp_query_metrics_table = 'b'
        observations_table = 'c'

    assert uploading._get_dt_paths(
        Args,
        config_sq=None,
        config_sqm=None,
        config_ob=None,
    ) == ('a', 'b', 'c')
