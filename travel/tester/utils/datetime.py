# -*- coding: utf-8 -*-

from __future__ import absolute_import

from datetime import datetime

from travel.avia.library.python.tester.utils.replace_setting import ReplaceSetting


class ReplaceNow(ReplaceSetting):
    """
    Подменяет ENVIRONMENT_NOW
    Можно передвать datetime или строку в формате '%Y-%m-%d %H:%M:%S'

    Можно использовать как декоратор функции либо как context manager:

    @replace_now(dt)
    def foo():
        pass

    with replace_now(dt) as now:
        print now
    """

    def __init__(self, now):
        if isinstance(now, basestring):
            now = datetime.strptime(now, '%Y-%m-%d %H:%M:%S')

        super(ReplaceNow, self).__init__('ENVIRONMENT_NOW', now)


replace_now = ReplaceNow
