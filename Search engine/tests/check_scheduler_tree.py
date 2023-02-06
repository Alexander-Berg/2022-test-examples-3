import logging
# import pytest

from search.scraper_over_yt.package.plugin.configs.Configs import get_scheduler_pool_tree

logger = logging.getLogger("test_logger")
# logger.info("Info message")


def test_tree_correctness_summ_weigths():
    tree = get_scheduler_pool_tree()
    assert abs(145.91 - sum([child['weight'] for child in tree['children'].values()])) < 10 ** -4


def checkPoolOrder(pool):
    children_names = [name for name in pool['children']]
    assert children_names == sorted(children_names)
    if hasattr(pool, 'children'):
        for child in pool['children'].values():
            checkPoolOrder(pool)


def test_tree_correctness_alphabetic_order():
    checkPoolOrder(get_scheduler_pool_tree())
