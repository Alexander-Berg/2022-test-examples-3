import logging
import os

import httpretty
import pytest
from hamcrest import assert_that, equal_to, has_entries

from calendar_attach_processor.service.blackbox import BlackboxService, User, BlackboxError
import calendar_attach_processor.arc as arc

logging.basicConfig(level=logging.DEBUG)
PATH = os.path.abspath(__file__)


@httpretty.activate
def test_bb_request_failed():
    httpretty.register_uri(httpretty.GET, "http://test-bb-request-failed.com/", status=502)
    with pytest.raises(BlackboxError):
        BlackboxService("http://test-bb-request-failed.com/", "1234.1.1").resolve_recipient_by_uid("12343")


@httpretty.activate
def test_bb_request_succ():
    with open(arc.test_path("service/blackbox/input", PATH, "ml.json")) as resp:
        test_url_succ = "http://test-bb-request-succ/"
        httpretty.register_uri(httpretty.GET, test_url_succ, body=resp.read(), status=200)

        assert_that(BlackboxService(test_url_succ, "1234.1.1").resolve_recipient_by_uid("1234"),
                    equal_to(User("1120000000001387", "maillist", True)))

        assert_that(httpretty.has_request(), equal_to(True))
        assert_that(httpretty.last_request().querystring, has_entries({
            "method": ['userinfo'],
            "userip": ['1234.1.1'],
            "format": ['json'],
            "uid": ['1234'],
            "attributes": ['13'],
            "dbfields": ['hosts.db_id.2']
        }))
