import pytest
from library.python import resource
import json

from market.monetize.efficiency.buybox.buybox_report_parser.lib.buybox_debug_parse import (
    make_productoffers_url,
    PRODUCTOFFERS_URL_TEMPLATE_WITH_CART,
    parse_response,
    form_pandas_readable_dict
)


def test_make_url_batch():
    report_host = "warehouse-report.blue.vs.market.yandex.net"
    msku_str = "100547293866,84440401"
    user_region_id = 213

    cart_str = "KYW9SFj-PpIs-4mAtY_uKw,KYW9SFj-PpIs-4mAtY_uKw"
    assert PRODUCTOFFERS_URL_TEMPLATE_WITH_CART % (report_host, user_region_id, msku_str, '', cart_str, '') \
           == make_productoffers_url(host=report_host, msku_id=[100547293866, 84440401], cart_str=cart_str,
                                     rids=user_region_id, enable_top6=False, rearr_flags="", raw_url_params="")


@pytest.mark.test_id(2)
def test_pandas_readable_dict():
    response = resource.find("/response.json")
    parsed_response = parse_response(json.loads(response))
    try:
        # Can't import pandas as it can be not installed
        pandas_seed = form_pandas_readable_dict(parsed_response)
        assert isinstance(pandas_seed, dict), "form_pandas_readable_dict should return dictionary"
        for column in pandas_seed:
            assert isinstance(pandas_seed[column], list), "dict values in result of form_pandas_readable_dict should be lists"
    except Exception as ex:
        assert False and str(ex)


@pytest.mark.test_id(3)
def test_price_after_cashback_and_randomization():
    response = resource.find("/response.json")
    parsed_response = parse_response(json.loads(response))
    for msku_dict in parsed_response:
        for msku in msku_dict:
            features = msku_dict[msku]['buybox_features']
            for candidate in features:
                price_after_cashback = candidate.price_after_cashback
                gmv_ue_randomized = candidate.gmv_ue_randomized
                price = candidate.price
                gmv = candidate.gmv
                if price_after_cashback != 0:  # 0 means NA
                    assert 1 <= price / price_after_cashback < 2, "strange price_after_cashback: {}".format(price_after_cashback)
                if gmv_ue_randomized != -1 and gmv != -1:  # -1 means NA
                    assert abs(gmv_ue_randomized) < 0.05 * gmv, r"gmv_ue_randomized was greater 5% of gmv"


@pytest.mark.test_id(4)
def test_error_parsing():
    response = {
        'error': {
            'code': 'INVALID_USER_CGI',
            'message': 'No pp parameter is set',
        }
    }
    parsed_response = parse_response(response)
    assert not parsed_response, 'Wrong behavior on response with error'


@pytest.mark.test_id(5)
def test_empty_result():
    response = {
        'search': {
            'results': [],
        }
    }
    parsed_response = parse_response(response)
    assert not parsed_response, 'Wrong behavior on response with empty result'


@pytest.mark.test_id(6)
def test_dsbs_is_not_dropship():
    response = resource.find("/response.json")
    parsed_response = parse_response(json.loads(response))
    dsbs_appeared_in_test = False
    for msku_dict in parsed_response:
        for msku in msku_dict:
            features = msku_dict[msku]['buybox_features']
            for candidate in features:
                if candidate.is_dsbs:
                    dsbs_appeared_in_test = True
                    assert candidate.offer_type != 'Dropship', 'DSBS offer marked as dropship'
    assert dsbs_appeared_in_test, 'Resource for the test should contain DSBS offer in response'
