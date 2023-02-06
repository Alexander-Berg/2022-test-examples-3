# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from logging import DEBUG, LogRecord


def _create_log_record(**kwargs):
    defaults = {
        'name': 'name', 'level': DEBUG, 'pathname': '', 'lineno': 1, 'msg': '', 'args': None, 'exc_info': None
    }
    defaults.update(kwargs)
    return LogRecord(**defaults)
