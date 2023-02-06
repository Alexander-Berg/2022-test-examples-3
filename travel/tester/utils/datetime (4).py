# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime

import six

from travel.rasp.library.python.common23.utils.code_utils import ContextManagerAsDecorator
from travel.rasp.library.python.common23.date.environment import set_time_context, delete_time_context


class ReplaceNow(ContextManagerAsDecorator):
    """
    Устанавливает значение _time_context.naive_msk_now, которое учитывается в
    travel.rasp.library.python.common23.date.environment.now_aware.
    Можно передвать datetime или строку в формате '%Y-%m-%d %H:%M:%S'.

    Можно использовать как декоратор функции либо как context manager:

    @replace_now(dt)
    def foo():
        pass

    with replace_now(dt) as now:
        print now
    """

    def _parse_date(self, data):
        try:
            return datetime.strptime(data, '%Y-%m-%d %H:%M:%S')
        except ValueError:
            pass

        return datetime.strptime(data, '%Y-%m-%d')

    def __init__(self, now):
        if isinstance(now, six.string_types):
            now = self._parse_date(now)
        self.now = now

    def __enter__(self):
        set_time_context(self.now)
        return self.now

    def __exit__(self, *args):
        delete_time_context()


replace_now = ReplaceNow
