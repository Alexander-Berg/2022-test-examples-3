# coding: utf-8
import os

import yatest.common

TEXT_DUMP_BIN = os.path.join('market', 'idx', 'models', 'bin', 'textdump', 'textdump')


def run_vclusters_textdump(flags, outname):
    in_path = yatest.common.test_source_path(os.path.join('data', 'models.pb'))

    command = [yatest.common.binary_path(TEXT_DUMP_BIN)] + flags + [in_path]

    with open(outname, "w") as out:
        yatest.common.execute(command, stdout=out)


def is_equal(expected_file, actual_file):
    expected = []
    actual = []
    with open(expected_file, 'r') as f:
        expected = f.readlines()

    with open(actual_file, 'r') as f:
        actual = f.readlines()

    assert expected == actual


def test_vclusters_textdump_full():
    out = os.path.join(yatest.common.output_path(), 'full.txt')
    run_vclusters_textdump(['--mode', 'all'], out)
    is_equal(yatest.common.test_source_path(os.path.join('data', 'out', 'full.txt')), out)


def test_vclusters_textdump_titles():
    out = os.path.join(yatest.common.output_path(), 'titles.txt')
    run_vclusters_textdump(['--mode', 'titles'], out)
    is_equal(yatest.common.test_source_path(os.path.join('data', 'out', 'titles.txt')), out)


def test_vclusters_textdump_ids():
    out = os.path.join(yatest.common.output_path(), 'ids.txt')
    run_vclusters_textdump(['--mode', 'ids'], out)
    is_equal(yatest.common.test_source_path(os.path.join('data', 'out', 'ids.txt')), out)


def test_vclusters_textdump_count():
    out = os.path.join(yatest.common.output_path(), 'count.txt')
    run_vclusters_textdump(['--mode', 'count'], out)
    is_equal(yatest.common.test_source_path(os.path.join('data', 'out', 'count.txt')), out)
