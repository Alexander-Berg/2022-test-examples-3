# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from functools import partial

import pytest

from travel.library.python.base_http_client import CircuitBreakerConfig, RetryConfig
from travel.library.python.base_http_client.errors import WrongClientConfigurationException
from travel.library.python.base_http_client.scope_configurator import ScopeConfigurator


def test_invalid_init():
    with pytest.raises(WrongClientConfigurationException) as ex:
        ScopeConfigurator(
            disable_retry_config=True,
            retry_config=RetryConfig(total=10)
        )
    assert "Can't init with disable_retry_config=True and retry_config" in str(ex.value)

    with pytest.raises(WrongClientConfigurationException) as ex:
        ScopeConfigurator(
            disable_circuit_breaker_config=True,
            circuit_breaker_config=CircuitBreakerConfig(fail_max=1, reset_timeout=10)
        )
    assert "Can't init with disable_circuit_breaker_config=True and circuit_breaker_config" in str(ex.value)

    with pytest.raises(WrongClientConfigurationException) as ex:
        ScopeConfigurator(
            disable_timeout=True,
            timeout=10
        )
    assert "Can't init with disable_timeout=True and timeout" in str(ex.value)


retry_config_1 = RetryConfig(total=10)
retry_config_2 = RetryConfig(connect=5)

PartialScopeConfigurator = partial(
    ScopeConfigurator,
    disable_timeout=True,
    disable_circuit_breaker_config=True
)
sc__drc_True__rc_None = PartialScopeConfigurator(disable_retry_config=True)
sc__drc_False__rc_None = PartialScopeConfigurator(disable_retry_config=False)
sc__drc_None__rc_None = PartialScopeConfigurator()
sc__drc_False__rc_1 = PartialScopeConfigurator(disable_retry_config=False, retry_config=retry_config_1)
sc__drc_None__rc_1 = PartialScopeConfigurator(retry_config=retry_config_1)
sc__drc_False__rc_2 = PartialScopeConfigurator(disable_retry_config=False, retry_config=retry_config_2)
sc__drc_None__rc_2 = PartialScopeConfigurator(retry_config=retry_config_2)


@pytest.mark.parametrize(
    'config_1,config_2,expected_config',
    [
        (sc__drc_True__rc_None, sc__drc_True__rc_None, sc__drc_True__rc_None),
        (sc__drc_True__rc_None, sc__drc_False__rc_None, None),
        (sc__drc_True__rc_None, sc__drc_None__rc_None, sc__drc_True__rc_None),
        (sc__drc_True__rc_None, sc__drc_False__rc_2, sc__drc_False__rc_2),
        (sc__drc_True__rc_None, sc__drc_None__rc_2, sc__drc_False__rc_2),

        (sc__drc_False__rc_None, sc__drc_True__rc_None, sc__drc_True__rc_None),
        (sc__drc_False__rc_None, sc__drc_False__rc_None, None),
        (sc__drc_False__rc_None, sc__drc_None__rc_None, None),
        (sc__drc_False__rc_None, sc__drc_False__rc_2, sc__drc_False__rc_2),
        (sc__drc_False__rc_None, sc__drc_None__rc_2, sc__drc_False__rc_2),

        (sc__drc_False__rc_1, sc__drc_True__rc_None, sc__drc_True__rc_None),
        (sc__drc_False__rc_1, sc__drc_False__rc_None, sc__drc_False__rc_1),
        (sc__drc_False__rc_1, sc__drc_None__rc_None, sc__drc_False__rc_1),
        (sc__drc_False__rc_1, sc__drc_False__rc_2, sc__drc_False__rc_2),
        (sc__drc_False__rc_1, sc__drc_None__rc_2, sc__drc_False__rc_2),
    ]
)
def test_merge(config_1, config_2, expected_config):
    if expected_config is None:
        with pytest.raises(WrongClientConfigurationException):
            config_1.merge(config_2)
    else:
        config_res = config_1.merge(config_2)
        assert config_res.disable_retry_config == expected_config.disable_retry_config
        assert config_res.retry_config == expected_config.retry_config
