#! /usr/bin/python3
# -*- coding: utf-8 -*-

import sys

sys.path.append("../usr/sbin/")

from migrator import (
    calc_imap_collector_pos_with_skipped,
    calc_pop3_collector_pos_with_skipped,
    calc_last_mid_with_skipped_mids,
    MAX_MIDS_COUNT,
    calc_optimized_mids_count,
)
from nose.tools import *


def test_imap_with_empty_data():
    last_mid, skipped_mids = calc_imap_collector_pos_with_skipped({}, {}, {})
    eq_(last_mid, 0)
    eq_(skipped_mids, set())


def test_imap_skips_missing_messages_collected_previously():
    last_mid, skipped_mids = calc_imap_collector_pos_with_skipped(
        {1: {"uidvalidity": 10, "type": "inbox"}},
        {(1, 1): {"mid": 100}, (1, 2): {"mid": 101}},
        {"INBOX": {"messages": [1, 2, 3], "uidvalidity": 10}},
    )
    eq_(last_mid, 101)
    eq_(skipped_mids, set())


def test_imap_skips_missing_folders_collected_previously():
    last_mid, skipped_mids = calc_imap_collector_pos_with_skipped(
        {1: {"uidvalidity": 10, "type": "inbox"}},
        {(1, 1): {"mid": 100}, (1, 2): {"mid": 101}},
        {
            "INBOX": {"messages": [1, 2], "uidvalidity": 10},
            "missing_folder": {"messages": [1, 2, 3], "uidvalidity": 20},
        },
    )
    eq_(last_mid, 101)
    eq_(skipped_mids, set())


def test_imap_with_multiple_folders():
    last_mid, skipped_mids = calc_imap_collector_pos_with_skipped(
        {
            1: {"uidvalidity": 10, "type": "inbox"},
            2: {"uidvalidity": 10, "type": "sent"},
            3: {"uidvalidity": 10, "type": "user", "name": "iamfolder", "parent_fid": 0},
        },
        {
            (1, 1): {"mid": 100},
            (1, 2): {"mid": 101},
            (1, 3): {"mid": 102},
            (2, 1): {"mid": 103},
            (3, 1): {"mid": 104},
        },
        {
            "INBOX": {"messages": [1, 2, 3], "uidvalidity": 10},
            "Отправленные": {"messages": [1], "uidvalidity": 10},
            "iamfolder": {"messages": [1], "uidvalidity": 10},
        },
    )
    eq_(last_mid, 104)
    eq_(skipped_mids, set())


def test_imap_with_child_folders():
    last_mid, skipped_mids = calc_imap_collector_pos_with_skipped(
        {
            1: {"uidvalidity": 10, "type": "inbox"},
            2: {"uidvalidity": 24, "type": "user", "name": "folder", "parent_fid": 0},
            3: {"uidvalidity": 53, "type": "user", "name": "subfolder", "parent_fid": 2},
        },
        {
            (1, 1): {"mid": 100},
            (1, 2): {"mid": 101},
            (1, 3): {"mid": 102},
            (2, 1): {"mid": 103},
            (3, 1): {"mid": 104},
        },
        {
            "INBOX": {"messages": [1, 2, 3], "uidvalidity": 10},
            "folder": {"messages": [1], "uidvalidity": 24},
            "folder|subfolder": {"messages": [1], "uidvalidity": 53},
        },
    )
    eq_(last_mid, 104)
    eq_(skipped_mids, set())


def test_imap_with_system_subfolders():
    last_mid, skipped_mids = calc_imap_collector_pos_with_skipped(
        {
            1: {"uidvalidity": 10, "type": "inbox"},
            2: {"uidvalidity": 24, "type": "user", "name": "subfolder", "parent_fid": 1},
        },
        {
            (1, 1): {"mid": 100},
            (1, 2): {"mid": 101},
            (1, 3): {"mid": 108},
            (2, 1): {"mid": 103},
            (2, 2): {"mid": 99},
            (2, 3): {"mid": 110},
        },
        {
            "INBOX": {"messages": [1, 2, 3], "uidvalidity": 10},
            "INBOX|subfolder": {"messages": [1], "uidvalidity": 24},
        },
    )
    eq_(last_mid, 108)
    eq_(skipped_mids, set([99]))


def test_imap_after_localization_switch():
    last_mid, skipped_mids = calc_imap_collector_pos_with_skipped(
        {
            1: {"uidvalidity": 10, "type": "sent"},
        },
        {
            (1, 1): {"mid": 100},
            (1, 2): {"mid": 101},
            (1, 3): {"mid": 108},
        },
        {
            "Sent": {"messages": [1, 2], "uidvalidity": 10},
            "Отправленные": {"messages": [3], "uidvalidity": 10},
        },
    )
    eq_(last_mid, 108)
    eq_(skipped_mids, set())


def test_imap_user_folder_with_system_name():
    last_mid, skipped_mids = calc_imap_collector_pos_with_skipped(
        {
            1: {"uidvalidity": 10, "type": "sent"},
            2: {"uidvalidity": 30, "type": "user", "name": "Отправленные", "parent_fid": 0},
        },
        {
            (1, 1): {"mid": 100},
            (1, 2): {"mid": 101},
            (1, 3): {"mid": 108},
            (2, 1): {"mid": 50},
            (2, 2): {"mid": 51},
            (2, 3): {"mid": 53},
        },
        {
            "Sent": {"messages": [1, 2], "uidvalidity": 10},
            "Отправленные": {"messages": [3], "uidvalidity": 30},
        },
    )
    eq_(last_mid, 101)
    eq_(skipped_mids, set([50, 51]))


def test_imap_user_folder_with_system_name_and_child():
    last_mid, skipped_mids = calc_imap_collector_pos_with_skipped(
        {
            1: {"uidvalidity": 10, "type": "sent"},
            2: {"uidvalidity": 30, "type": "user", "name": "Отправленные", "parent_fid": 0},
            3: {"uidvalidity": 10, "type": "user", "name": "Игорю", "parent_fid": 2},
        },
        {
            (1, 1): {"mid": 100},
            (1, 2): {"mid": 101},
            (1, 3): {"mid": 108},
            (2, 1): {"mid": 50},
            (2, 2): {"mid": 51},
            (2, 3): {"mid": 53},
            (3, 1): {"mid": 70},
            (3, 2): {"mid": 71},
            (3, 3): {"mid": 72},
        },
        {
            "Sent": {"messages": [1, 2], "uidvalidity": 10},
            "Отправленные": {"messages": [3], "uidvalidity": 30},
            "Отправленные|Игорю": {"messages": [2], "uidvalidity": 10},
        },
    )
    eq_(last_mid, 101)
    eq_(skipped_mids, set([50, 51, 70, 72]))


def test_imap_nothing_collected():
    last_mid, skipped_mids = calc_imap_collector_pos_with_skipped(
        {
            1: {"uidvalidity": 10, "type": "inbox"},
            2: {"uidvalidity": 24, "type": "user", "name": "folder", "parent_fid": 0},
            3: {"uidvalidity": 53, "type": "user", "name": "subfolder", "parent_fid": 2},
        },
        {
            (1, 1): {"mid": 100},
            (1, 2): {"mid": 101},
            (1, 3): {"mid": 102},
            (2, 1): {"mid": 103},
            (3, 1): {"mid": 104},
        },
        {},
    )
    eq_(last_mid, 0)
    eq_(skipped_mids, set())


def test_imap_collected_only_inbox_top():
    last_mid, skipped_mids = calc_imap_collector_pos_with_skipped(
        {
            1: {"uidvalidity": 10, "type": "inbox"},
            2: {"uidvalidity": 24, "type": "user", "name": "folder", "parent_fid": 0},
            3: {"uidvalidity": 53, "type": "user", "name": "subfolder", "parent_fid": 2},
        },
        {
            (1, 1): {"mid": 100},
            (1, 2): {"mid": 101},
            (1, 3): {"mid": 102},
            (1, 3): {"mid": 103},
            (2, 1): {"mid": 99},
            (3, 1): {"mid": 105},
        },
        {
            "INBOX": {"messages": [3, 4], "uidvalidity": 10},
        },
    )
    eq_(last_mid, 103)
    eq_(skipped_mids, set([99, 100, 101]))


def test_imap_aliases_for_duplicated_folders():
    last_mid, skipped_mids = calc_imap_collector_pos_with_skipped(
        {
            1: {"uidvalidity": 10, "type": "inbox"},
            2: {"uidvalidity": 24, "type": "user", "name": "INBOX", "parent_fid": 0},
        },
        {
            (1, 1): {"mid": 100},
            (1, 2): {"mid": 101},
            (1, 3): {"mid": 102},
            (1, 4): {"mid": 103},
            (2, 1): {"mid": 99},
            (2, 2): {"mid": 105},
        },
        {
            "INBOX": {"messages": [3, 4], "uidvalidity": 10},
            "INBOX_0": {"messages": [2], "uidvalidity": 24},
        },
    )
    eq_(last_mid, 105)
    eq_(skipped_mids, set([99, 100, 101]))


def test_multiple_system_folders_with_missing_uidvalidity_in_second():
    last_mid, skipped_mids = calc_imap_collector_pos_with_skipped(
        {
            1: {"uidvalidity": 10, "type": "sent"},
        },
        {
            (1, 1): {"mid": 100},
            (1, 2): {"mid": 101},
            (1, 3): {"mid": 108},
        },
        {
            "Sent": {"messages": [2], "uidvalidity": 10},
            "Отправленные": {"messages": [3], "uidvalidity": 30},
        },
    )
    eq_(last_mid, 101)
    eq_(skipped_mids, set([100]))


@raises(Exception)
def test_multiple_system_folders_with_existing_uidvalidity():
    last_mid, skipped_mids = calc_imap_collector_pos_with_skipped(
        {
            1: {"uidvalidity": 10, "type": "sent"},
            2: {"uidvalidity": 24, "type": "user", "name": "INBOX", "parent_fid": 0},
        },
        {
            (1, 1): {"mid": 100},
            (1, 2): {"mid": 101},
            (1, 3): {"mid": 108},
            (2, 1): {"mid": 50},
        },
        {
            "Sent": {"messages": [2], "uidvalidity": 10},
            "Отправленные": {"messages": [3], "uidvalidity": 24},
        },
    )


def test_pop3_with_empty_data():
    last_mid, skipped_mids = calc_pop3_collector_pos_with_skipped(all_messages={}, uidls={})
    eq_(last_mid, 0)
    eq_(skipped_mids, set())


def test_pop3_nothing_collected():
    last_mid, skipped_mids = calc_pop3_collector_pos_with_skipped(
        all_messages={1: 1, 2: 2, 3: 3}, uidls={}
    )
    eq_(last_mid, 0)
    eq_(skipped_mids, set())


def test_pop3_all_collected():
    last_mid, skipped_mids = calc_pop3_collector_pos_with_skipped(
        all_messages={1: 1, 2: 2, 3: 3}, uidls={1, 2, 3}
    )
    eq_(last_mid, 3)
    eq_(skipped_mids, set())


def test_pop3_mosaically_collected():
    last_mid, skipped_mids = calc_pop3_collector_pos_with_skipped(
        all_messages={"a": 1, "b": 2, "c": 3, "d": 4, "e": 5}, uidls={"b", "d", "e"}
    )
    eq_(last_mid, 5)
    eq_(skipped_mids, {1, 3})


def test_too_many_skipped_mids():
    with assert_raises(Exception) as e:
        calc_last_mid_with_skipped_mids(
            all_mids=[mid for mid in range(1, MAX_MIDS_COUNT + 2)],
            collected_mids=[MAX_MIDS_COUNT + 1],
        )
    msg = e.exception.args[0]
    ok_(msg.startswith("too many skipped_mids"))


def test_calc_optimized_mids_count_optimized_to_zero():
    # last_mid = 3
    eq_(0, calc_optimized_mids_count(skipped_mids=[4, 5, 6], predownloaded_mids=[1, 2, 3]))


def test_calc_optimized_mids_count_no_skipped():
    eq_(0, calc_optimized_mids_count(skipped_mids=[], predownloaded_mids=[1, 2, 3]))


def test_calc_optimized_mids_count_no_predownloaded():
    eq_(0, calc_optimized_mids_count(skipped_mids=[4, 5, 6], predownloaded_mids=[]))


def test_calc_optimized_mids_count_two_pikes():
    # last_mid = 1 or last_mid = 3
    eq_(2, calc_optimized_mids_count(skipped_mids=[2, 4, 5], predownloaded_mids=[1, 3, 6]))


def test_calc_optimized_mids_count_smallest_last_mid():
    # last_mid = 0
    eq_(1, calc_optimized_mids_count(skipped_mids=[1, 2], predownloaded_mids=[3]))


def test_calc_optimized_mids_count_biggest_last_mid():
    # last_mid = 6
    eq_(2, calc_optimized_mids_count(skipped_mids=[1, 4], predownloaded_mids=[2, 3, 5, 6]))


def test_calc_optimized_mids_count_min_take_all_predownloaded_mids():
    # last_mid = 4
    eq_(1, calc_optimized_mids_count(skipped_mids=[1, 5, 6], predownloaded_mids=[2, 3, 4]))
