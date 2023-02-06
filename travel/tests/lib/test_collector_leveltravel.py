# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from checkers import TaskChecker


def test_common_flow():
    TaskChecker(
        test_fn='travel/cpa/tests/lib/data/collectors/leveltravel/common_flow.toml',
    )
