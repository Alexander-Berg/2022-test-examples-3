# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals


def filter_out_runtest_call_records(records):
    return [
        r for r in records if r.pathname != 'library/python/pytest/plugins/ya.py'
    ]
