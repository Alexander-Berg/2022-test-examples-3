# coding: utf-8

from collections import defaultdict
from datetime import datetime

from market.idx.pylibrary.mindexer_core.geninfo import geninfo


def _mkgen(**kwargs):
    dd = defaultdict(str)
    dd.update(kwargs)
    return geninfo.Generation(dd)


def test_first_generation_of_the_week_after12_negative():
    g = _mkgen(start_date=datetime(2022, 1, 18, 17, 10))
    pg = _mkgen(start_date=datetime(2022, 1, 18, 8, 10))
    assert not geninfo.is_first_generation_of_the_week_after12(g, pg)


def test_first_generation_of_the_week_after12_negative2():
    g = _mkgen(start_date=datetime(2022, 1, 17, 7, 10))
    pg = _mkgen(start_date=datetime(2022, 1, 16, 8, 10))
    assert not geninfo.is_first_generation_of_the_week_after12(g, pg)


def test_first_generation_of_the_week_after12_negative3():
    g = _mkgen(start_date=datetime(2022, 1, 17, 7, 10))
    pg = _mkgen(start_date=datetime(2022, 1, 17, 5, 10))
    assert not geninfo.is_first_generation_of_the_week_after12(g, pg)


def test_first_generation_of_the_week_after12():
    g = _mkgen(start_date=datetime(2022, 1, 17, 17, 10))
    pg = _mkgen(start_date=datetime(2022, 1, 17, 8, 10))
    assert geninfo.is_first_generation_of_the_week_after12(g, pg)


def test_first_generation_of_the_week_after12_2():
    g = _mkgen(start_date=datetime(2022, 1, 18, 17, 10))
    pg = _mkgen(start_date=datetime(2022, 1, 17, 8, 10))
    assert geninfo.is_first_generation_of_the_week_after12(g, pg)


def test_first_generation_of_the_week_after12_3():
    g = _mkgen(start_date=datetime(2022, 1, 24, 8, 10))
    pg = _mkgen(start_date=datetime(2022, 1, 17, 8, 10))
    assert geninfo.is_first_generation_of_the_week_after12(g, pg)


def test_first_generation_of_the_day_after12_negative2():
    g = _mkgen(start_date=datetime(2022, 1, 17, 7, 10))
    pg = _mkgen(start_date=datetime(2022, 1, 16, 12, 10))
    assert not geninfo.is_first_generation_of_the_day_after12(g, pg)


def test_first_generation_of_the_day_after12_negative3():
    g = _mkgen(start_date=datetime(2022, 1, 16, 9, 10))
    pg = _mkgen(start_date=datetime(2022, 1, 16, 8, 10))
    assert not geninfo.is_first_generation_of_the_day_after12(g, pg)


def test_first_generation_of_the_day_after12_negative4():
    g = _mkgen(start_date=datetime(2022, 1, 16, 19, 10))
    pg = _mkgen(start_date=datetime(2022, 1, 16, 18, 10))
    assert not geninfo.is_first_generation_of_the_day_after12(g, pg)


def test_first_generation_of_the_day_after12():
    g = _mkgen(start_date=datetime(2022, 1, 16, 19, 10))
    pg = _mkgen(start_date=datetime(2022, 1, 16, 8, 10))
    assert geninfo.is_first_generation_of_the_day_after12(g, pg)


def test_first_generation_of_the_day_after12_2():
    g = _mkgen(start_date=datetime(2022, 1, 17, 9, 10))
    pg = _mkgen(start_date=datetime(2022, 1, 16, 8, 10))
    assert geninfo.is_first_generation_of_the_day_after12(g, pg)
