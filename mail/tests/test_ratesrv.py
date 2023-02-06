# coding: utf-8

import requests
import time

from .conftest import TEST_GROUP, TEST_LIMIT_NAME, TEST_DOMAIN, TEST_GC_CONFIG
from .conftest import TEST_LIMIT_NAME_RARE_GC, TEST_DOMAIN_NO_RECOVERY, TEST_RECOVERY_RATE_NO_RECOVERY
from .conftest import TEST_RECOVERY_RATE_DEFAULT, TEST_RECOVERY_RATE_FOR_TEST_DOMAIN
from .util import now_ms, verify_response


VALID_LIMITS = {
    "mxfront": (
        "bytes_for_rcpt", "bytes_for_rcpt_from_ip",
        "connections_from_ip",
        "msgs_for_rcpt",
        "msgs_for_rcpt_from_ip",
        "msgs_for_rcpt_from_sndr"
    ),
    "smtp": (
        "bytes_from_sndr",
        "msgs_for_rcpt_from_sndr",
        "msgs_from_sndr"
    )
}


def test_ping_works(ratesrv_client):
    response = ratesrv_client.ping()
    assert response.status_code == requests.codes.ok
    assert "pong" in response.text


def test_get_works(ratesrv_client):
    for group in VALID_LIMITS:
        for limit_name in VALID_LIMITS[group]:
            counter_id = "some-id"
            counter_name = "%s:%s:mega-unique-hash" % (group, limit_name)

            response = ratesrv_client.get({counter_id: counter_name})
            verify_response(response, counter_id, expected_value=0, expected_error=None)
            assert counter_name not in response.text


def test_broken_groups_are_rejected(ratesrv_client):
    counter_id = "cid"
    response = ratesrv_client.get({counter_id: "junk:greylisting:junk"})
    verify_response(response, counter_id, expected_value=None, expected_error="group not found")
    assert response.json()["counters"][counter_id]["status"] == "error"


def test_broken_subgroups_are_rejected(ratesrv_client):
    counter_id = "cid"
    response = ratesrv_client.get({counter_id: "smtp:junk:junk"})
    verify_response(response, counter_id, expected_value=None, expected_error="limit not found")
    assert response.json()["counters"][counter_id]["status"] == "error"


def test_increase_works(ratesrv_client):
    counter_id = "cid"
    counter_name = "mxfront:msgs_for_rcpt:whatever-increase-works"

    response = ratesrv_client.increase({counter_id: {"name": counter_name, "value": 1}})
    verify_response(response, counter_id, expected_value=1)
    assert counter_name not in response.text


def test_multiple_counters_are_supported(ratesrv_client):
    request_body = {}
    next_id = 0

    for group in VALID_LIMITS:
        for limit_name in VALID_LIMITS[group]:
            next_id += 1
            request_body[str(next_id)] = "%s:%s:unique-name-for-test-multiple-counters" % (group, limit_name)

    response = ratesrv_client.get(request_body)
    verify_response(response,
                    counter_id=[str(i + 1) for i in range(next_id)],
                    expected_value=[0] * next_id,
                    expected_available=[None] * next_id,
                    expected_error=[None] * next_id)


def test_increase_stacks(ratesrv_client):
    counter_id = "some-id"
    counter_name = "%s:%s@%s:mega-unique-hash-for-test-increase-stacks" % (TEST_GROUP, TEST_LIMIT_NAME_RARE_GC, TEST_DOMAIN_NO_RECOVERY)

    response = ratesrv_client.get({counter_id: counter_name})
    verify_response(response, counter_id, expected_value=0, expected_available=TEST_RECOVERY_RATE_NO_RECOVERY["threshold"])

    for i in range(10):
        increase_response = ratesrv_client.increase({counter_id: {"name": counter_name, "value": 2}})
        verify_response(increase_response, counter_id, expected_value=2 * (i + 1))


def test_recovery(ratesrv_client):
    counter_id = "fancy-id"
    counter_name = "%s:%s:unique-for-test-recovery" % (TEST_GROUP, TEST_LIMIT_NAME_RARE_GC)

    max_value = TEST_RECOVERY_RATE_DEFAULT["threshold"]
    recovered = TEST_RECOVERY_RATE_DEFAULT["recovery_rate"]
    recovery_interval = TEST_RECOVERY_RATE_DEFAULT["recovery_interval"]

    response = ratesrv_client.increase({counter_id: {"name": counter_name, "value": max_value}})
    verify_response(response, counter_id, expected_value=max_value)

    time_before_sleep = now_ms()
    time.sleep(0.4)
    time_after_sleep = now_ms()

    expected_value = max_value - recovered * ((time_after_sleep - time_before_sleep) // recovery_interval)
    response = ratesrv_client.get({counter_id: counter_name})
    verify_response(response, counter_id, expected_value=expected_value)


def test_gc(ratesrv_client):
    counter_id = "some-id"
    counter_name = "%s:%s@%s:mega-unique-hash-for-test-gc" % (TEST_GROUP, TEST_LIMIT_NAME, TEST_DOMAIN)

    unreasonably_big_counter_value = TEST_RECOVERY_RATE_FOR_TEST_DOMAIN["threshold"] * 15
    response = ratesrv_client.increase({counter_id: {"name": counter_name, "value": unreasonably_big_counter_value}})
    verify_response(response,
                    counter_id,
                    expected_value=unreasonably_big_counter_value,
                    expected_available=TEST_RECOVERY_RATE_FOR_TEST_DOMAIN["threshold"] - unreasonably_big_counter_value)

    time_before_sleep = now_ms()
    while now_ms() - time_before_sleep < TEST_GC_CONFIG["ttl"]:
        time.sleep(0.6)

    response = ratesrv_client.get({counter_id: counter_name})
    verify_response(response, counter_id, expected_value=0)
    # counter should be zeroed by gc if we wait long enough
    recovery_rate = TEST_RECOVERY_RATE_FOR_TEST_DOMAIN["recovery_rate"]
    assert (recovery_rate * (now_ms() - time_before_sleep) // TEST_RECOVERY_RATE_FOR_TEST_DOMAIN["recovery_interval"]) < unreasonably_big_counter_value
