# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import datetime as dt
from dateutil import parser, tz

from search.martylib.core.date_utils import UTC, CompatibleDateTime, get_datetime, get_timestamp, convert_to_utc
from search.martylib.test_utils import TestCase


class TestDatetime(TestCase):
    def _get_datetime_basic_test(self, source):
        """
        Makes sure result returned by `get_datetime` is an instance of `datetime.datetime` and is in UTC.
        """
        result = get_datetime(source)
        self.assertIsInstance(result, dt.datetime)
        self.assertIsInstance(result, CompatibleDateTime)
        self.assertEqual(result.tzinfo, UTC)
        self.assertTrue(hasattr(result, 'timestamp'))
        return result

    def test_CompatibleDateTime_to_and_from_datetime(self):
        source = dt.datetime.now()
        self.assertEqual(CompatibleDateTime._from_datetime(source).to_datetime(), source)

    def test_CompatibleDateTime_timestamp(self):
        self.assertEqual(CompatibleDateTime(1970, 1, 1, tzinfo=UTC).timestamp(), 0)

    def test_get_datetime_with_none(self):
        self.assertIsNone(get_datetime(None))

    def test_get_datetime_with_unaware_datetime(self):
        source = dt.datetime.now(tz=None)  # Naive, local timezone.
        self._get_datetime_basic_test(source)

    def test_get_datetime_with_aware_datetime(self):
        source = dt.datetime.now(tz=tz.gettz('Europe/Moscow'))  # Aware, Europe/Moscow.
        self._get_datetime_basic_test(source)

    def test_get_datetime_with_utc_datetime(self):
        source = dt.datetime.now(tz=UTC)  # Aware, UTC.
        self._get_datetime_basic_test(source)

    def test_get_datetime_with_int_unix_timestamp(self):
        source = 1485102270  # Aware, UTC.
        result = self._get_datetime_basic_test(source)
        self.assertEqual(result.day, 22)
        self.assertEqual(result.month, 1)
        self.assertEqual(result.year, 2017)
        self.assertEqual(result.hour, 16)
        self.assertEqual(result.minute, 24)

    def test_get_datetime_with_float_unix_timestamp(self):
        source = 1485102270.634922  # Aware, UTC.
        result = self._get_datetime_basic_test(source)
        self.assertEqual(result.day, 22)
        self.assertEqual(result.month, 1)
        self.assertEqual(result.year, 2017)
        self.assertEqual(result.hour, 16)
        self.assertEqual(result.minute, 24)

    def test_get_datetime_with_js_timestamp(self):
        source = 1485102270634  # Aware, UTC.
        result = self._get_datetime_basic_test(source)
        self.assertEqual(result.day, 22)
        self.assertEqual(result.month, 1)
        self.assertEqual(result.year, 2017)
        self.assertEqual(result.hour, 16)
        self.assertEqual(result.minute, 24)

    def test_get_datetime_with_iso_string(self):
        source = '2017-01-22T16:24:30.634922+00:00'  # Aware, UTC.
        result = self._get_datetime_basic_test(source)
        self.assertEqual(result.day, 22)
        self.assertEqual(result.month, 1)
        self.assertEqual(result.year, 2017)
        self.assertEqual(result.hour, 16)
        self.assertEqual(result.minute, 24)

    # FIXME: timestamps only work for +300
    # def test_get_datetime_with_custom_string(self):
    #     source = '2017-01-22 16:24:30'  # Naive (local timezone).
    #     result = self._get_datetime_basic_test(source)
    #
    #     # >>> datetime.datetime.fromtimestamp(1485091470.0).isoformat()
    #     # <<< '2017-01-22T16:24:30'
    #     self.assertEqual(result.timestamp(), 1485091470.0)
    #
    #     source = '2017-01-22'  # Naive, local timezone.
    #     result = self._get_datetime_basic_test(source)
    #
    #     # >>> datetime.datetime.fromtimestamp(1485032400.0).isoformat()
    #     # <<< '2017-01-22T00:00:00'
    #     self.assertEqual(result.timestamp(), 1485032400.0)

    def test_get_datetime_with_momentjs_formatted_string(self):
        source = "2017-02-28T12:47:43.317Z"
        result = self._get_datetime_basic_test(source)
        self.assertEqual(result.timestamp(), 1488286063.317)

        source = '"1488286063317"'
        result = self._get_datetime_basic_test(source)
        self.assertEqual(result.timestamp(), 1488286063.317)

    def test_get_datetime_utc_conversion(self):
        source = '2017-02-01T16:06:48.398336+03:00'  # Aware, Europe/Moscow.
        result = self._get_datetime_basic_test(source)
        parsed = parser.parse(source)

        self.assertEqual(result.timestamp(), get_timestamp(parsed))

    def test_convert_to_utc_naive(self):
        # This test is only useful when running in non-UTC timezone.
        # And anyway, converting naive datetime objects to any timezone is a bad idea.

        source = dt.datetime.now()  # Naive, local timezone.
        result = convert_to_utc(source)

        self.assertEqual(get_timestamp(source), get_timestamp(result))

    def test_convert_to_utc_aware_local(self):
        source = CompatibleDateTime.now(tz=tz.gettz('Europe/Moscow'))
        result = CompatibleDateTime._from_datetime(convert_to_utc(source))

        self.assertEqual(get_timestamp(source), result.timestamp())

    def test_convert_to_utc_aware_utc(self):
        source = dt.datetime.utcnow()
        result = convert_to_utc(source)

        self.assertEqual(get_timestamp(source), get_timestamp(result))

    # FIXME: 3 hour shift!
    # def test_date_with_no_original_tz(self):
    #     source = '2017-01-08'
    #     result = get_datetime(source)
    #     expected = CompatibleDateTime(2017, 1, 7, 21, 0, tzinfo=UTC)
    #
    #     self.assertEqual(result, expected)

    def test_date_with_original_tz(self):
        source = '2017-01-08'
        result = get_datetime(source, UTC)
        expected = CompatibleDateTime(2017, 1, 8, 0, 0, tzinfo=UTC)

        self.assertEqual(result, expected)

    def test_datetime_with_no_original_tz(self):
        source = CompatibleDateTime(2017, 1, 8, 18, 30, tzinfo=tz.gettz('Europe/Moscow'))
        result = get_datetime(source)
        expected = CompatibleDateTime(2017, 1, 8, 18, 30, tzinfo=UTC) - dt.timedelta(hours=3)

        self.assertEqual(result, expected)

    def test_datetime_with_original_tz(self):
        source = '2017-01-08 18:30'
        result = get_datetime(source, UTC)
        expected = CompatibleDateTime(2017, 1, 8, 18, 30, tzinfo=UTC)

        self.assertEqual(result, expected)

    def test_get_datetime_with_aware_datetime_and_original_tz(self):
        source = CompatibleDateTime(2017, 1, 8, 15, 30, tzinfo=UTC)

        with self.assertRaises(ValueError):
            get_datetime(source, UTC)
