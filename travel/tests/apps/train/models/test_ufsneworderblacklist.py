# coding: utf-8

from __future__ import unicode_literals

import pytest

from common.apps.train.models import UFSNewOrderBlackList


@pytest.mark.parametrize('number, numbers', [
    ('  ', set()),
    ('001R', {'001R'}),
    ('001R, 002U', {'001R', '002U'}),
])
def test_numbers(number, numbers):
    assert UFSNewOrderBlackList(number=number).numbers == numbers
