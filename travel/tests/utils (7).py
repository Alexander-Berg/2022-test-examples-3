# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

import mock
import pytest

from travel.rasp.rasp_data.resource_checker.check_exception import CheckException
from travel.rasp.rasp_data.resource_checker.data_provider import DataProvider


def assert_check(check, data_provider, log=None, expected_fail=None):
    if log is None:
        log = mock.Mock()

    if expected_fail is None:
        assert check(data_provider, log) is None
    else:
        with pytest.raises(CheckException) as check_exception:
            check(data_provider, log)
        assert str(check_exception.value) == expected_fail


def create_data_provider(
    countries=(),
    settlements=()
):
    data_provider = DataProvider({})

    for country in countries:
        data_provider.country_repo.add_object(country)

    for settlement in settlements:
        data_provider.settlement_repo.add_object(settlement)

    return data_provider
