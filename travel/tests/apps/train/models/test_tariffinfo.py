# coding: utf-8
from __future__ import unicode_literals

import pytest

from common.apps.train.models import TariffInfo


@pytest.mark.parametrize('attr', ('ufs_response_codes', 'im_response_codes'))
@pytest.mark.parametrize('value, expected', (
    ('', []),
    ('foo', ['foo']),
    ('foo ,   bar,baz', ['foo', 'bar', 'baz']),
))
def test_codes_lists(attr, value, expected):
    tariff_info = TariffInfo()
    setattr(tariff_info, attr, value)
    assert getattr(tariff_info, '{}_list'.format(attr)) == expected
