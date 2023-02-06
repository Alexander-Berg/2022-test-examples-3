#!/usr/bin/env python
# coding: utf-8

import six

from market.pylibrary.hide_cash_only_conditions import pylib


def verify_lib(test_name, json_text, expect):
    pylib.write_to_proto(json_text, test_name)
    pylib.verify_proto(six.ensure_binary(test_name), expect)


def test_proto():
    # Verifing when a file does not exist -- MUST BE FIRST!
    try:
        absent_file = "test_0"
        res = pylib.verify(six.ensure_binary(absent_file))
        assert False, "Exception didn't trhow for absent file '{}', result:{}".format(absent_file, res)
    except RuntimeError:
        pass

    # Verifing loading synthetic tests
    verify_lib('test_1', '{  }', (True, [], []))
    verify_lib('test_2', '{ "rids": [ 3, 1 ], "msku": [ "200", "100" ] }', (True, [1, 3], [six.ensure_binary('100'), six.ensure_binary('200')]))
