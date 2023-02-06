# coding: utf-8

import pytest
from hamcrest import assert_that, equal_to
import multiprocessing as mp
import time
import logging

from market.idx.datacamp.parser.lib.bomb import PyBomb


def runner(sleep_timeout, bomb_timeout):
    bomb = PyBomb(True, bomb_timeout, logging.getLogger())
    bomb.plant()
    time.sleep(sleep_timeout)
    bomb.defuse()
    bomb.plant()
    time.sleep(sleep_timeout)
    bomb.defuse()


def bad_runner():
    bomb = PyBomb(True, 4, logging.getLogger())
    bomb.plant()
    bomb.plant()
    bomb.defuse()
    bomb.defuse()


@pytest.mark.parametrize("bomb_timeout, exit_code", [(4, 0), (1, -9)])
def test_bomb(bomb_timeout, exit_code):
    proc = mp.Process(target=runner, args=[2, bomb_timeout])
    proc.start()
    proc.join()
    assert_that(proc.exitcode, equal_to(exit_code))


def test_bad_runner():
    proc = mp.Process(target=bad_runner)
    proc.start()
    proc.join()
    assert_that(proc.exitcode, not equal_to(0))
