# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from checkers import TaskChecker


def test_common_flow(yt_stuff):
    TaskChecker(
        yt_client=yt_stuff.yt_client,
        yt_proxy=yt_stuff.get_server(),
        test_fn='travel/cpa/tests/lib/data/collectors/onetwotripru/common_flow.toml',
    )
