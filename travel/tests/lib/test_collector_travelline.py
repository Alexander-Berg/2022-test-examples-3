# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from checkers import TaskChecker


def test_common_flow_v2():
    TaskChecker(
        test_fn='travel/cpa/tests/lib/data/collectors/travelline_v2/common_flow.toml',
    )
