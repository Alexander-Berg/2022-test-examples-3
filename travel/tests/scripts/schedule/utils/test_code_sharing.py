# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest

from travel.rasp.admin.importinfo.admin import CodeshareNumber
from travel.rasp.admin.scripts.schedule.utils.code_sharing import check_code_share_number, get_code_share_regexps


@pytest.mark.dbuser
def test_check_code_share_number():
    CodeshareNumber.objects.create(number_re='7R4[0-9][0-9][0-9]')
    assert check_code_share_number(get_code_share_regexps(), '7R 4489') is True
    assert check_code_share_number(get_code_share_regexps(), '7R4489') is True

    number = 'ab 1'
    assert check_code_share_number(get_code_share_regexps(), number) is False

    CodeshareNumber.objects.create(number_re='\w+\s\w+')
    assert check_code_share_number(get_code_share_regexps(), number) is True
