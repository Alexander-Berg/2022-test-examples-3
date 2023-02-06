# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest
from hamcrest import assert_that, contains_inanyorder

from common.data_api.train_api.tariffs.utils import get_possible_numbers


@pytest.mark.parametrize('number, variants', [
    ('001Ц', ['001Ц', '002Ц']),
    ('ФФ001Ц', ['ФФ001Ц']),
    ('50Ф', ['050Ф', '049Ф']),
    ('', ['']),
    ('100', ['099', '100']),
    ('strange', ['strange'])
])
def test_get_possible_numbers(number, variants):
    assert_that(get_possible_numbers(number), contains_inanyorder(*variants))
