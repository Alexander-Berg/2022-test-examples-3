# coding: utf-8

import requests
import time

from requests.exceptions import ConnectionError


def now_ms():
    return int(time.time() * 1000)


def ping_or_sleep(ratesrv_client):
    response = False
    try:
        response = ratesrv_client.ping()
    except (TimeoutError, ConnectionError):
        time.sleep(1)
    return response


def check_and_remove(response_counters, counter_id, expected_value=None, expected_available=None, expected_error=None):
    assert counter_id in response_counters

    counter = response_counters.pop(counter_id)

    if expected_value is not None:
        assert counter["current"] == expected_value
    elif expected_error is None:
        assert "current" in counter

    if expected_available is not None:
        assert counter["available"] == expected_available
    elif expected_error is None:
        assert "available" in counter

    if expected_error is not None:
        assert expected_error in counter["description"]
        assert counter["status"] == "error"


def verify_response(response, counter_id, expected_value=None, expected_available=None, expected_error=None):
    assert response.status_code == requests.codes.ok

    counters = response.json()["counters"]

    if expected_value is None or expected_available is None or expected_error is None or isinstance(counter_id, str):
        assert len(counters) == 1
        assert isinstance(counter_id, str)
        check_and_remove(counters, counter_id, expected_value, expected_available, expected_error)
    else:
        assert len(counter_id) == len(expected_value)
        assert len(expected_value) == len(expected_available)
        assert len(expected_available) == len(expected_error)
        assert len(expected_error) == len(counters)
        for i in range(len(counter_id)):
            check_and_remove(counters, counter_id[i], expected_value[i], expected_available[i], expected_error[i])
