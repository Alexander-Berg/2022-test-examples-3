# -*- coding: utf-8 -*-

from datetime import (
    datetime,
    timedelta,
)
from market.pylibrary.ytdumper.ytdumper import (
    get_generations_to_delete_std_format,
    get_generations_to_delete_custom_format,
    YtCleaner,
)

path = "//foo/test_white_list"
generation = "20170303_1019"


def _prepare(yt_stuff, generations, white_list, black_list=None):
    if black_list is None:
        black_list = []

    yt = yt_stuff.get_yt_client()
    if yt.exists(path):
        for table in yt.list(path):
            yt.remove(path + "/" + table, recursive=True, force=True)
    generations.append(generation)

    for gen in generations:
        yt.create('table', path + "/" + gen, recursive=True)

    for l in white_list:
        if l not in black_list:
            yt.create('table', path + "/" + l, recursive=True)


def _yd_get_generations_to_delete(path, yt_proxy, white_list):
    ytcleaner = YtCleaner(path, generation, yt_proxy=yt_proxy, white_list=white_list)
    return ytcleaner.get_generations_to_delete(keep_count=1)


def _yt_clear(path, yt_proxy, white_list):
    ytcleaner = YtCleaner(path, generation, yt_proxy=yt_proxy, white_list=white_list)
    ytcleaner.clear(keep_count=1)


def _should_see_yt_tables(expected_tables, yt_stuff, generations, white_list, black_list=None):
    if black_list is None:
        black_list = []

    yt = yt_stuff.get_yt_client()
    _prepare(yt_stuff, generations, white_list, black_list)

    generations_to_delete = _yd_get_generations_to_delete(path, yt_stuff.get_server(), white_list)
    actual_tables = frozenset(generations).union(white_list).difference(black_list, generations_to_delete)
    assert actual_tables == expected_tables

    _yt_clear(path, yt_stuff.get_server(), white_list)
    assert frozenset(yt.list(path)) == expected_tables


# Tests for white_list
def test_common_white_list(yt_stuff):
    white_list = ["not_for_remove1", "not_for_remove2"]
    generations = ["20170204_2202", "20170218_2225", "20170219_2225"]

    expected_tables = frozenset([generation] + white_list)
    _should_see_yt_tables(expected_tables, yt_stuff, generations, white_list)


def test_empty_white_list(yt_stuff):
    white_list = []
    generations = ["20170204_2202", "20170218_2225", "20170219_2225"]

    expected_tables = frozenset([generation])
    _should_see_yt_tables(expected_tables, yt_stuff, generations, white_list)


def test_mixed_white_list(yt_stuff):
    grey_eminence = "not_in_the_list"
    white_list = ["not_for_remove1"]
    black_list = [grey_eminence]
    generations = ["20170204_2202", "20170218_2225", "20170219_2225"]

    expected_tables = frozenset([generation] + white_list)
    white_list.append(grey_eminence)
    _should_see_yt_tables(expected_tables, yt_stuff, generations, white_list, black_list)

GENERATIONS = [
    '20170204_2202',  # unique week 4, unique day 9
    '20170211_2210',  # unique week 3, unique day 8
    '20170218_2225',  # unique week 2, unique day 7
    '20170225_2123',  # unique week 1, unique day 6
    '20170225_2323',  # unique week 1, unique day 6
    '20170226_2106',  # unique week 0, unique day 5
    '20170226_2306',  # unique week 0, unique day 5
    '20170227_2304',  # unique week 0, unique day 4
    '20170228_2002',  # unique week 0, unique day 3
    '20170228_2202',  # unique week 0, unique day 3
    '20170301_2152',  # unique week 0, unique day 2
    '20170302_2310',  # unique week 0, unique day 1
    '20170303_0127',  # unique week 0, unique day 0
    '20170303_0327',  # unique week 0, unique day 0
    '20170303_0549',  # unique week 0, unique day 0
    '20170303_1019',  # unique week 0, unique day 0
]

NOW = datetime(2017, 3, 4, 0, 0)


def kept_generations(to_delete, generations=GENERATIONS):
    return sorted(
        frozenset(generations) - frozenset(to_delete),
        reverse=True
    )


def test_all():
    to_delete = get_generations_to_delete_std_format(
        GENERATIONS,
        keep_count=3,
        keep_daily=4,
        keep_weekly=2,
    )

    kept = kept_generations(to_delete)
    expected_kept = [
        '20170303_1019',
        '20170303_0549',
        '20170303_0327',
        '20170302_2310',
        '20170301_2152',
        '20170228_2202',
        '20170225_2323',
    ]

    assert expected_kept == kept


def test_count_daily():
    to_delete = get_generations_to_delete_std_format(
        GENERATIONS,
        keep_count=3,
        keep_daily=4,
        keep_weekly=0,
    )

    kept = kept_generations(to_delete)
    expected_kept = [
        '20170303_1019',
        '20170303_0549',
        '20170303_0327',
        '20170302_2310',
        '20170301_2152',
        '20170228_2202',
    ]

    assert expected_kept == kept


def test_count_weekly():
    to_delete = get_generations_to_delete_std_format(
        GENERATIONS,
        keep_count=3,
        keep_daily=0,
        keep_weekly=2,
    )

    kept = kept_generations(to_delete)
    expected_kept = [
        '20170303_1019',
        '20170303_0549',
        '20170303_0327',
        '20170225_2323',
    ]

    assert expected_kept == kept


def test_count():
    to_delete = get_generations_to_delete_std_format(
        GENERATIONS,
        keep_count=3,
        keep_daily=0,
        keep_weekly=0,
    )

    kept = kept_generations(to_delete)
    expected_kept = [
        '20170303_1019',
        '20170303_0549',
        '20170303_0327',
    ]

    assert expected_kept == kept


def test_count_custom_format():
    generations = [
        'aaa',
        'bbb',
        'ccc',
        'ddd',
        'eee',
    ]

    to_delete = get_generations_to_delete_custom_format(
        generations,
        keep_count=3,
    )

    kept = kept_generations(to_delete, generations=generations)
    expected_kept = [
        'eee',
        'ddd',
        'ccc',
    ]

    assert expected_kept == kept


def test_zero():
    to_delete = get_generations_to_delete_std_format(
        GENERATIONS,
        keep_count=0,
        keep_daily=0,
        keep_weekly=0,
    )

    kept = kept_generations(to_delete)
    expected_kept = []

    assert expected_kept == kept


def test_time():
    to_delete = get_generations_to_delete_std_format(
        GENERATIONS,
        keep_cutoff=NOW - timedelta(days=2),
        keep_daily_cutoff=NOW - timedelta(days=365),
    )

    kept = kept_generations(to_delete)
    expected_kept = [
        '20170303_1019',
        '20170303_0549',
        '20170303_0327',
        '20170303_0127',
        '20170302_2310',
        '20170301_2152',
        '20170228_2202',
        '20170227_2304',
        '20170226_2306',
        '20170225_2323',
        '20170218_2225',
        '20170211_2210',
        '20170204_2202',
    ]

    assert expected_kept == kept


def test_time_cutoff():
    to_delete = get_generations_to_delete_std_format(
        GENERATIONS,
        keep_cutoff=NOW - timedelta(days=2),
    )

    kept = kept_generations(to_delete)
    expected_kept = [
        '20170303_1019',
        '20170303_0549',
        '20170303_0327',
        '20170303_0127',
        '20170302_2310',
    ]

    assert expected_kept == kept


def test_mixed():
    to_delete = get_generations_to_delete_std_format(
        GENERATIONS,
        keep_count=3,
        keep_daily_cutoff=NOW - timedelta(days=7),
    )

    kept = kept_generations(to_delete)
    expected_kept = [
        '20170303_1019',  # unique week 0, unique day 0
        '20170303_0549',  # unique week 0, unique day 0
        '20170303_0327',  # unique week 0, unique day 0
        '20170302_2310',  # unique week 0, unique day 1
        '20170301_2152',  # unique week 0, unique day 2
        '20170228_2202',  # unique week 0, unique day 3
        '20170227_2304',  # unique week 0, unique day 4
        '20170226_2306',  # unique week 0, unique day 5
        '20170225_2323',  # unique week 1, unique day 6
    ]

    assert expected_kept == kept


def test_empty_list():
    to_delete = get_generations_to_delete_std_format(
        [],
        keep_count=3,
        keep_daily=4,
        keep_weekly_cutoff=timedelta(days=365),
    )

    assert [] == to_delete


def test_is_recent(yt_stuff):
    generations = ["20170204_2202", "20170218_2225", "20170219_2225"]

    _prepare(yt_stuff, generations, [], [])
    ytcleaner = YtCleaner(path, generation, yt_proxy=yt_stuff.get_server())
    assert not ytcleaner.is_recent()
    ytcleaner.update_recent()
    assert ytcleaner.is_recent()
