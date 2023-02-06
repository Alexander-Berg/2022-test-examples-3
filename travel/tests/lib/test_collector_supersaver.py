# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from checkers import TaskChecker


def test_common_flow(yt_stuff):
    TaskChecker(
        test_fn='travel/cpa/tests/lib/data/collectors/supersaver/common_flow.toml',
        yt_client=yt_stuff.yt_client,
        yt_proxy=yt_stuff.get_server(),
    )
