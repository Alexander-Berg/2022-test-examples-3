# -*- encoding: utf-8 -*-
from travel.avia.travelers.tests.utils import enable_test_flag, get_test_flag


def test_no_exist_test_flag(fixture_flag_storage):
    assert not get_test_flag(fixture_flag_storage)


def test_no_turn_test_flag_with_update_flags(freezer, fixture_flag_storage, feature_flag_client):
    assert not get_test_flag(fixture_flag_storage)

    enable_test_flag(feature_flag_client)
    assert not get_test_flag(fixture_flag_storage)

    freezer.tick(fixture_flag_storage.timer - 1)
    assert not get_test_flag(fixture_flag_storage)


def test_turn_test_flag_after_timer(freezer, fixture_flag_storage, feature_flag_client):
    assert not get_test_flag(fixture_flag_storage)

    enable_test_flag(feature_flag_client)
    freezer.tick(fixture_flag_storage.timer + 1)
    assert get_test_flag(fixture_flag_storage)
