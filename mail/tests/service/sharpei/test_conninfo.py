import logging

import httpretty
import os
import pytest
from hamcrest import assert_that, equal_to, has_entries

from calendar_attach_processor.service.sharpei import Sharpei, SharpeiError
import calendar_attach_processor.arc as arc

logging.basicConfig(level=logging.DEBUG)
PATH = os.path.abspath(__file__)


@httpretty.activate
def test_failed_resp():
    url = "http://test-sharpei-request-failed.com/"
    httpretty.register_uri(httpretty.GET, url, responses=[httpretty.Response(body="", status=500)])
    with pytest.raises(SharpeiError):
        Sharpei(url, "lb-pg", "pwd").get_conn_info("12345")


@httpretty.activate
def test_uid_not_found_resp():
    url = "http://test-uid-not-found.com/"
    httpretty.register_uri(httpretty.GET, url, responses=[httpretty.Response(body="", status=404)])
    with pytest.raises(SharpeiError):
        Sharpei(url, "lb-pg", "pwd").get_conn_info("12345")


@httpretty.activate
def test_bad_resp():
    url = "http://test-bad-resp.com/"
    with open(arc.test_path("service/sharpei/input", PATH, "bad.json")) as resp:
        httpretty.register_uri(httpretty.GET, url, responses=[httpretty.Response(body=resp.read(), status=200)])
        with pytest.raises(SharpeiError):
            Sharpei(url, "lb-pg", "pwd").get_conn_info("12345")


@httpretty.activate
def test_uid_not_pg():
    url = "http://test-uid-not-pg.com/"
    with open(arc.test_path("service/sharpei/input", PATH, "not_pg.txt")) as resp:
        httpretty.register_uri(httpretty.GET, url, responses=[httpretty.Response(body=resp.read(), status=400)])
        with pytest.raises(SharpeiError):
            Sharpei(url, "lb-pg", "pwd").get_conn_info("12345")


@httpretty.activate
def test_ok_resp():
    url = "http://test-sharpei_request.com/"
    with open(arc.test_path("service/sharpei/input", PATH, "all_alive.json")) as resp:
        httpretty.register_uri(httpretty.GET, url, body=resp.read(), status=200)
        address = Sharpei(url, "lb-pg", "pwd").get_conn_info("1234")
        assert_that(address,
                    equal_to('host=xdb-test02e.cmail.yandex.net port=6432 dbname=maildb user=lb-pg password=pwd'))

        assert_that(httpretty.last_request().querystring, has_entries({
            "mode": ['read_only'],
            "format": ['json'],
            "uid": ['1234']
        }))
