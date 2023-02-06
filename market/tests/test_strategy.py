# -*- coding: utf-8 -*-

import time
import pytest
from market.idx.pylibrary.disk_queue.disk_queue import DiskQueueStrategySimple


@pytest.fixture()
def strategy():
    return DiskQueueStrategySimple()


def test_strategy(strategy):
    item1 = strategy.create_item_id()
    time.sleep(1)
    item2 = strategy.create_item_id()
    time.sleep(1)
    item3 = strategy.create_item_id()

    assert item1 < item2
    assert item2 < item3
    assert item1 == strategy.dequeue_item([item2, item1, item3])
