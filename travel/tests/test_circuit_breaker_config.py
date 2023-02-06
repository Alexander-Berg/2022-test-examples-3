# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest
from requests import HTTPError

from travel.library.python.base_http_client import CircuitBreakerConfig
from travel.library.python.base_http_client.errors import InvalidConfigException


class HttpResponseStub(object):
    def __init__(self, status_code):
        self.status_code = status_code


def test_circuit_breaker_config():
    circuit_breaker_config = CircuitBreakerConfig(fail_max=10, reset_timeout=5)
    circuit_breaker = circuit_breaker_config.get_circuit_breaker()
    assert circuit_breaker.fail_max == 10
    assert circuit_breaker.reset_timeout == 5

    fun = circuit_breaker_config.exclude[0]
    assert fun(HTTPError('', response=HttpResponseStub(200))) is False
    assert fun(HTTPError('', response=HttpResponseStub(500))) is False
    assert fun(HTTPError('', response=HttpResponseStub(400))) is True
    assert fun(HTTPError('', response=HttpResponseStub(404))) is True


def test_invalid_circuit_breaker_config():
    with pytest.raises(InvalidConfigException):
        CircuitBreakerConfig()

    with pytest.raises(InvalidConfigException):
        CircuitBreakerConfig(fail_max=10)

    with pytest.raises(InvalidConfigException):
        CircuitBreakerConfig(reset_timeout=5)


def test_evolve():
    cbc = CircuitBreakerConfig(fail_max=1, reset_timeout=2)
    assert cbc.fail_max == 1
    assert cbc.reset_timeout == 2

    evolved_cbc = cbc.evolve(reset_timeout=5)

    assert evolved_cbc.fail_max == cbc.fail_max
    assert evolved_cbc.reset_timeout == 5
    assert cbc.reset_timeout == 2
    assert cbc.exclude[0](HTTPError('', response=HttpResponseStub(500))) is False
    assert cbc.exclude[0](HTTPError('', response=HttpResponseStub(400))) is True

    cbc.set_excluded_status_codes([404])
    evolved_cbc = cbc.evolve(reset_timeout=6)

    assert evolved_cbc.fail_max == cbc.fail_max
    assert evolved_cbc.reset_timeout == 6
    assert cbc.reset_timeout == 2
    assert cbc.exclude[0](HTTPError('', response=HttpResponseStub(400))) is False
    assert cbc.exclude[0](HTTPError('', response=HttpResponseStub(404))) is True


def test_evolve_wrong_param():
    cbc = CircuitBreakerConfig(fail_max=1, reset_timeout=2)
    with pytest.raises(TypeError):
        cbc.evolve(abc=5)


def test_set_excluded_status_codes():
    cbc = CircuitBreakerConfig(fail_max=1, reset_timeout=2)
    cbc.set_excluded_status_codes([404])

    assert cbc.fail_max == 1
    assert cbc.reset_timeout == 2
    assert cbc.exclude[0](HTTPError('', response=HttpResponseStub(400))) is False
    assert cbc.exclude[0](HTTPError('', response=HttpResponseStub(404))) is True
