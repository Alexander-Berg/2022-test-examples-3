# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock
from pytest import raises

from common.tester.utils.replace_setting import replace_dynamic_setting
from travel.rasp.suburban_selling.manual_scripts.check_balance import check


def test_check():
    with mock.patch('travel.rasp.suburban_selling.manual_scripts.check_balance.get_balance', autospec=True) as m_get_balance, \
            replace_dynamic_setting('AEROEX_BALANCE_LIMIT', -100):
        m_get_balance.return_value = 'OK', -50
        assert check() == 'status: OK. balance: -50.'

        m_get_balance.return_value = 'OK', -101
        with raises(Exception):
            check()

        m_get_balance.return_value = 'FAIL', 10
        with raises(Exception):
            check()
