# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest

from travel.rasp.train_api.train_partners.base.reserve_tickets import get_notices


@pytest.mark.parametrize('notice_parts, expected_special_notice, expected_time_notice', [
    ([], None, None),
    (['СТАНЦИИ СНГ', 'ВРЕМЯ ОТПР МОСКОВСКОЕ'], 'СТАНЦИИ СНГ', 'ВРЕМЯ ОТПР МОСКОВСКОЕ'),
    (['ВОЗВРАТ ПО ТЕЛ', '8-800-7777-020'], 'ВОЗВРАТ ПО ТЕЛ. 8-800-7777-020', None),
    (['ВРЕМЯ ОТПР И ПРИБ МОСКОВСКОЕ'], None, 'ВРЕМЯ ОТПР И ПРИБ МОСКОВСКОЕ'),

    (['ВОЗВРАТ ПО ТЕЛ', '8-800-7777-020, ВРЕМЯ РАБОТЫ С 8 до 20', 'ВРЕМЯ ОТПР МОСКОВСКОЕ', 'ВРЕМЯ ПРИБ МЕСТНОЕ'],
     'ВОЗВРАТ ПО ТЕЛ. 8-800-7777-020, ВРЕМЯ РАБОТЫ С 8 до 20',
     'ВРЕМЯ ОТПР МОСКОВСКОЕ. ВРЕМЯ ПРИБ МЕСТНОЕ')
])
def test_get_notices(notice_parts, expected_special_notice, expected_time_notice):
    assert get_notices(notice_parts) == (expected_special_notice, expected_time_notice)
