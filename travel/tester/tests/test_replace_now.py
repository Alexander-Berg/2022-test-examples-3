# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime

from travel.rasp.library.python.common23.tester.utils.datetime import replace_now
from travel.rasp.library.python.common23.date.date_const import MSK_TZ
from travel.rasp.library.python.common23.date.environment import now_aware, now, get_time_context


class TestReplaceNow(object):
    naive_dt = datetime(2010, 2, 2, 12, 10)
    msk_dt = MSK_TZ.localize(naive_dt)

    def test_context_mananger(self):
        with replace_now(self.naive_dt) as now_dt:
            assert now() == self.naive_dt
            assert now_aware() == MSK_TZ.localize(now_dt)
            assert now_dt == self.naive_dt
        assert get_time_context() is None

        str_dt = '2010-02-02 12:10:0'
        with replace_now(str_dt) as now_dt:
            assert now() == now_dt
            assert now_aware() == MSK_TZ.localize(now_dt)

    def test_decorator(self):
        @replace_now(self.naive_dt)
        def check_decorator():
            assert now() == self.naive_dt
            assert now_aware() == self.msk_dt

        check_decorator()
        assert get_time_context() is None

    @replace_now(naive_dt)
    def test_class_decorator(self):
        assert now() == self.naive_dt
        assert now_aware() == self.msk_dt
