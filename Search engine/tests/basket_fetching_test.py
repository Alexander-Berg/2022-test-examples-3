import json
import os

import responses

import pytest
import basket_fetching
import token_utils
from test_utils import read_json_test_data
from metrics_rest_test_utils import get_headers_stub, get_metrics_token_mock, get_wrong_metrics_token_mock

QGAAS_API_URL_TEMPLATE = "https://qgaas.ru/api/query/{}"

BASKETS = {
    'KEIRA': {
        'basket_id': 227482,
        'payload': read_json_test_data(os.path.join("fetching", "keira.json"))
    },
    'KEIRAS': {
        'basket_id': 307271,
        'payload': read_json_test_data(os.path.join("fetching", "keiras.json"))
    }
}

KEIRA_BASKET_ID = BASKETS['KEIRA']['basket_id']
KEIRAS_BASKET_ID = BASKETS['KEIRAS']['basket_id']


def request_callback(request):
    if request.headers.get('Authorization') == get_headers_stub()['Authorization']:
        for _, basket in BASKETS.items():
            if request.url.startswith(QGAAS_API_URL_TEMPLATE.format(basket['basket_id'])):
                return 200, {}, json.dumps(basket['payload'])
        else:
            return 404, {}, []
    else:
        return 401, {}, None


def _set_up_callbacks():
    token_utils.get_metrics_token = get_metrics_token_mock
    responses.add_callback(
        responses.GET,
        QGAAS_API_URL_TEMPLATE.format(KEIRA_BASKET_ID),
        callback=request_callback,
        content_type="application/json",
    )
    responses.add_callback(
        responses.GET,
        QGAAS_API_URL_TEMPLATE.format(KEIRAS_BASKET_ID),
        callback=request_callback,
        content_type="application/json",
    )


def test_unspecified_basket_should_not_be_fetched():
    token_utils.get_metrics_token = get_metrics_token_mock
    with pytest.raises(ValueError):
        result_generator = basket_fetching.get_queries(
            basket=None,
            config=None,
            url_pattern=QGAAS_API_URL_TEMPLATE
        )
        next(result_generator)


@responses.activate
def test_unauthorized_request_should_fail():
    _set_up_callbacks()
    token_utils.get_metrics_token = get_wrong_metrics_token_mock
    with pytest.raises(Exception):
        result_generator = basket_fetching.get_queries(
            basket=None,
            config=None,
            url_pattern=QGAAS_API_URL_TEMPLATE
        )
        next(result_generator)


@responses.activate
def test_simple_fetching_with_basket_argument():
    _set_up_callbacks()
    result = list(
        basket_fetching.get_queries(
            basket=KEIRAS_BASKET_ID,
            config=None,
            url_pattern=QGAAS_API_URL_TEMPLATE
        )
    )
    assert len(responses.calls) == 1
    assert len(result) == 3


@responses.activate
def test_fetching_with_list_of_configs():
    _set_up_callbacks()
    config_multiple = read_json_test_data(os.path.join("prepare", "config_multiple.json"))
    result = list(
        basket_fetching.get_queries(
            basket=KEIRAS_BASKET_ID,
            config=config_multiple,
            url_pattern=QGAAS_API_URL_TEMPLATE
        )
    )
    assert len(responses.calls) == 1
    assert len(result) == 3


def _set_up_no_diff(basket):
    _set_up_callbacks()
    config_multiple_baskets = read_json_test_data(os.path.join("prepare", "config_multiple_baskets.json"))
    result = list(
        basket_fetching.get_queries(
            basket=basket,
            config=config_multiple_baskets,
            url_pattern=QGAAS_API_URL_TEMPLATE
        )
    )
    return responses, result


@responses.activate
def test_fetching_with_no_diff_config():
    responses, result = _set_up_no_diff(basket=None)

    assert len(responses.calls) == 2  # two baskets are specified in config_multiple_baskets.json
    assert len(result) == 1 + 3


@responses.activate
def test_basket_argument_should_be_ignored_for_no_diff_config():
    cresponses, result = _set_up_no_diff(basket=123456)

    assert len(responses.calls) == 2
    expected_urls = {QGAAS_API_URL_TEMPLATE.format(basket_id) for basket_id in {KEIRA_BASKET_ID, KEIRAS_BASKET_ID}}
    assert {c.request.url for c in responses.calls} == expected_urls
    assert len(result) == 1 + 3
