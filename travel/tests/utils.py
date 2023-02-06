# -*- encoding: utf-8 -*-
import json

import pytest


def dump_flag(flags, ab_flags=None):
    ab_flags = ab_flags or []
    return json.dumps({'flags': flags, 'abFlags': ab_flags})


flag_test_set = pytest.mark.parametrize('key, value, expected', [
    ('NOT_IN_FF', '1', False),
    ('NOT_IN_FF', '0', False),
    ('TEST1', '1', True),
    ('TEST1', '0', True),
    ('AB_TEST1', '1', True),
    ('AB_TEST1', '0', False),
    ('AB_TEST1', None, False),
])
