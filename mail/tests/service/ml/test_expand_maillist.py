import logging
import os

import httpretty
import pytest
from hamcrest import assert_that, equal_to, has_entries

from calendar_attach_processor.service.ml import Maillist, Subscriber, MlError
from calendar_attach_processor.tvm.session import TvmSession
import calendar_attach_processor.arc as arc
from tests.fixtures.services import http_pretty, tvm, ticket_headers

logging.basicConfig(level=logging.DEBUG)
PATH = os.path.abspath(__file__)


def test_ml_request_failed(http_pretty, tvm, ticket_headers):
    url = "http://test-ml-request-failed.com/"
    httpretty.register_uri(httpretty.GET, url, status=502)
    with pytest.raises(MlError):
        Maillist(url, TvmSession('101', 'some-secret')).expand_maillists("mail-list@yandex-team.ru")


def test_ml_request_succ(http_pretty, tvm, ticket_headers):
    with open(arc.test_path("service/ml/input", PATH, "maillist_ok.json")) as resp:
        url = "http://test-ml-request-ok.com/"
        httpretty.register_uri(httpretty.GET, url, body=resp.read(), status=200)
        subscribers = Maillist(url, TvmSession('101', 'some-secret')).expand_maillists("mail-testing@yandex-team.ru")
        assert_that(subscribers, equal_to([Subscriber("user1@yandex-team.ru", False, "kateogar"),
                                           Subscriber("test-maillist@yandex-team.ru", False, "furita-test-29"),
                                           Subscriber("user2@yandex-team.ru", False, "stassiak")]))

        assert_that(httpretty.last_request().querystring, has_entries({
            "expand": ['yes'],
            "emails": ['mail-testing@yandex-team.ru']
        }))


def test_ml_request_bad(http_pretty, tvm, ticket_headers):
    with open(arc.test_path("service/ml/input", PATH, "bad.json")) as resp:
        url = "http://test-ml-request-bad.com/"
        httpretty.register_uri(httpretty.GET, url, body=resp.read(), status=200)
        with pytest.raises(MlError):
            Maillist(url, TvmSession('101', 'some-secret')).expand_maillists("mail-testing@yandex-team.ru")
