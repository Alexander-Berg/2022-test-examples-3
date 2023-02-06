# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from checkers import TaskChecker


def test_common_flow_with_limit():
    TaskChecker(
        test_fn='travel/cpa/tests/lib/data/collectors/im_boy_limit/common_flow.toml'
    )
