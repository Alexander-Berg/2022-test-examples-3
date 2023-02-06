# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest

from travel.library.python.base_http_client import RetryConfig
from travel.library.python.base_http_client.errors import InvalidConfigException


def test_retry_config():
    rc = RetryConfig(total=10)
    retries = rc.get_retry()
    assert retries.total == 10

    assert 500 in rc.status_forcelist
    assert 400 not in rc.status_forcelist
    assert 200 not in rc.status_forcelist


def test_invalid_retry_config():
    with pytest.raises(InvalidConfigException):
        RetryConfig()


def test_evolve():
    rc = RetryConfig(total=1)
    assert rc.total == 1
    assert rc.status is None

    evolved_rc = rc.evolve(status=2)
    assert evolved_rc.total == rc.total
    assert evolved_rc.status == 2
    assert rc.status is None
    assert 500 in rc.status_forcelist
    assert 400 not in rc.status_forcelist
    assert 200 not in rc.status_forcelist

    rc.set_excluded_status_codes([404])

    evolved_rc = rc.evolve(status=3)
    assert evolved_rc.total == rc.total
    assert evolved_rc.status == 3
    assert rc.status is None
    assert 400 in rc.status_forcelist
    assert 404 not in rc.status_forcelist


def test_evolve_wrong_param():
    rc = RetryConfig(total=1)
    with pytest.raises(TypeError):
        rc.evolve(abc=5)
