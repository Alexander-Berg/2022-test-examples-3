# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import json
from datetime import datetime, timedelta

import freezegun
import mock
import pytest
import six

from yabus.etraffic import segments_provider
from yabus.etraffic.segments_provider import SegmentsDump, SegmentsProvider


@pytest.fixture
def dump_path(tmp_path):
    return tmp_path / "segments.json"


@pytest.fixture
def m_segments_dump():
    with mock.patch.object(SegmentsDump, "load", autospec=True, return_value=None), \
            mock.patch.object(SegmentsDump, "save", autospec=True):
        yield SegmentsDump


@pytest.fixture
def dummy_provider(dump_path):
    result = SegmentsProvider(update_period=timedelta(hours=1), dump_filename=six.text_type(dump_path))
    with mock.patch.object(result, "run_update_loop", autospec=True):
        yield result


@pytest.fixture
def m_sleep():
    with mock.patch.object(segments_provider, "sleep") as m_sleep:
        yield m_sleep


@pytest.fixture
def check_run(etraffic_client, dump_path, m_segments_dump, dummy_provider):
    @freezegun.freeze_time("2020-01-01")
    @mock.patch.object(etraffic_client, "raw_segments", return_value={'segments': [("1", "2"), ("1", "3"), ("4", "1")]})
    def run(m_get_raw_segments):
        dummy_provider.run(etraffic_client)

        # segments are received from client
        m_get_raw_segments.assert_called_once_with()

        # segments are saved to the dump file
        m_segments_dump.save.assert_called_once_with(
            SegmentsDump(datetime(2020, 1, 1), frozenset([("1", "2"), ("1", "3"), ("4", "1")])), dump_path
        )

        # update loop started
        dummy_provider.run_update_loop.assert_called_once_with()

        # segments are provided
        assert dummy_provider.get_segments() == frozenset([("1", "2"), ("1", "3"), ("4", "1")])

    return run


class TestSegmentsDump(object):
    def test_load_missing(self, dump_path):
        assert SegmentsDump.load(dump_path) is None

    @pytest.mark.parametrize("dump_created_at", ("2020-01-01T00:00:00", 1577836800,))
    def test_load(self, dump_created_at, dump_path):
        with dump_path.open("wb") as dump_file:
            json.dump({"created_at": dump_created_at, "dump": [["1", "2"]]}, dump_file)

        segments_dump = SegmentsDump.load(dump_path)
        assert segments_dump.created_at == datetime(2020, 1, 1)
        assert segments_dump.segments == frozenset([("1", "2")])

    def test_save(self, dump_path):
        SegmentsDump(datetime(2020, 1, 1), frozenset([("1", "2")])).save(dump_path)

        assert json.load(dump_path.open()) == {"created_at": "2020-01-01T00:00:00", "dump": [["1", "2"]]}

    def test_age(self):
        segments_dump = SegmentsDump(datetime(2020, 1, 1), frozenset([("1", "2")]))

        with freezegun.freeze_time("2020-01-01T12:00:00"):
            assert segments_dump.age == timedelta(hours=12)


class TestSegmentsProvider(object):
    def test_run_without_dump(self, check_run, m_sleep):
        check_run()
        assert not m_sleep.called

    @pytest.mark.parametrize(
        "dump_age, expected_sleep_call", ((timedelta(hours=1), False), (timedelta(hours=-1), True),)
    )
    def test_run_with_dump(self, dump_age, expected_sleep_call, m_segments_dump, dummy_provider, m_sleep, check_run):
        segments = frozenset([("5", "6")])
        original_update = dummy_provider._update

        def check_segments():
            assert dummy_provider.get_segments() == segments
            original_update()

        m_segments_dump.load.return_value = SegmentsDump(datetime(2020, 1, 1) - dump_age, segments)

        with mock.patch.object(dummy_provider, "_update", side_effect=check_segments):
            check_run()

        assert m_sleep.called == expected_sleep_call
