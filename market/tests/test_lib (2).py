# coding: utf-8

"""Unit tests for the ghoul library.
"""

from __future__ import print_function

import fixtures
from fixtures import cores_dir, ghoul_confs  # noqa
import market.idx.admin.ghoul.lib.ghoul as ghoul


def test_iter_binaries(ghoul_confs):  # noqa
    assert set(fixtures.BINARIES) == set(ghoul.iter_binaries(ghoul_confs))


def test_load_binaries(ghoul_confs):  # noqa
    expected_binaries = set([
        'market-feedpars',
        'stats-convert',
    ])

    actual_binaries = set(ghoul.load_binaries(ghoul_confs, fixtures.CORE_LEN))
    assert expected_binaries == actual_binaries


def test_extract_core_binary():
    core_names = [
        (
            'core.20180114154503.stats-convert.6.ps01ht.890912.hash.disabled',
            'stats-convert',
        ),
        (
            'core.20180115174554.market-feedpars.6.ps01ht.878322.hash.disabled',
            'market-feedpars',
        ),
    ]

    for core, name in core_names:
        assert name == ghoul.extract_core_binary(core)


def test_iter_cores(cores_dir):  # noqa
    cores = set()
    for core_path, core_ts in ghoul.iter_cores(cores_dir):
        assert core_ts == fixtures.extract_timestamp(core_path.split('.')[1])
        cores.add(core_path)

    assert set(fixtures.CORE_NAMES) == cores


def test_format_core_counters_fresh():
    fresh_counter = {
        'foo': ghoul.CoreStats(fixtures.LAST_FP, 1),
    }
    counter = {
        'foo': ghoul.CoreStats(fixtures.LAST_FP, 2),
        'bar': ghoul.CoreStats(fixtures.LAST_SC, 1),
    }

    formatted_cores = ghoul.format_core_counters(counter, fresh_counter)
    assert formatted_cores.startswith('1;')
    assert ghoul.FRESH_CORES_PROMPT in formatted_cores
    assert ghoul.ALL_CORES_PROMPT in formatted_cores


def test_format_core_counters_fresh_red_alert():
    fresh_counter = {
        'foo': ghoul.CoreStats(fixtures.LAST_FP, 6),
    }
    counter = {
        'foo': ghoul.CoreStats(fixtures.LAST_FP, 2),
        'bar': ghoul.CoreStats(fixtures.LAST_SC, 1),
    }

    formatted_cores = ghoul.format_core_counters(counter, fresh_counter)
    assert formatted_cores.startswith('1;')
    assert ghoul.FRESH_CORES_PROMPT in formatted_cores
    assert ghoul.ALL_CORES_PROMPT in formatted_cores


def test_format_core_counters_no_fresh():
    fresh_counter = {
    }
    counter = {
        'foo': ghoul.CoreStats(fixtures.LAST_FP, 2),
        'bar': ghoul.CoreStats(fixtures.LAST_SC, 1),
    }

    formatted_cores = ghoul.format_core_counters(counter, fresh_counter)
    assert formatted_cores.startswith('1;')
    assert ghoul.FRESH_CORES_PROMPT not in formatted_cores
    assert ghoul.ALL_CORES_PROMPT in formatted_cores


def test_fromat_core_counters_empty():
    fresh_counter = {
    }
    counter = {
    }

    formatted_cores = ghoul.format_core_counters(counter, fresh_counter)
    assert formatted_cores.startswith('0;')
    assert ghoul.FRESH_CORES_PROMPT not in formatted_cores
    assert ghoul.ALL_CORES_PROMPT not in formatted_cores


def test_aggregate_cores(cores_dir, ghoul_confs):  # noqa
    cores = ghoul.iter_cores(cores_dir)
    binaries = ghoul.load_binaries(ghoul_confs, fixtures.CORE_LEN)

    relevant_threshold = fixtures.NOW - 5 * 24 * 60 * 60
    fresh_threshold = fixtures.NOW - 24 * 60 * 60
    counter, fresh_counter = ghoul.aggregate_cores(
        cores,
        binaries,
        relevant_threshold,
        fresh_threshold
    )

    expected_counter = {
        'market-feedpars': ghoul.CoreStats(fixtures.LAST_FP, 5),
        'stats-convert': ghoul.CoreStats(fixtures.LAST_SC, 5),
    }

    expected_fresh_counter = {
        'market-feedpars': ghoul.CoreStats(fixtures.LAST_FP, 5),
        'stats-convert': ghoul.CoreStats(fixtures.LAST_SC, 1),
    }

    assert expected_counter == dict(counter)
    assert expected_fresh_counter == dict(fresh_counter)


def test_remove_old_cores(cores_dir, ghoul_confs):  # noqa
    binaries = ghoul.load_binaries(ghoul_confs, fixtures.CORE_LEN)

    threshold = fixtures.NOW - 3 * 24 * 60 * 60
    counter = ghoul.remove_old_cores(
        cores_dir,
        binaries,
        threshold,
        remove_func=lambda f: print(f)
    )

    expected_counter = {
        'market-feedpars': 2,
        'stats-convert': 6,
        'unknown': 1,
    }

    assert expected_counter == dict(counter)
