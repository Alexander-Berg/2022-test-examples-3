# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock

from common.models.geo import Station, Settlement
from common.tester.testcase import TestCase
from geosearch.views.point import PointSearch, StopWordError, NoPointsError, InvalidPointKey, TooShortError, InvalidSlug
from travel.rasp.morda_backend.morda_backend.search.parse_context.geosearch_wrapper import safe_get_point_list
from travel.rasp.morda_backend.tests.search.parse_context.geosearch_wrapper.factories import create_testing_point_list


class TestSafeGetPointsByAnyTransportType(TestCase):
    def setUp(self):
        super(TestSafeGetPointsByAnyTransportType, self).setUp()
        self.point_key = 'key'
        self.point_title = 'title'
        self.t_type = None
        self.point_slug = 'slug'

    def test_no_errors(self):
        expected_point_list = create_testing_point_list()

        with mock.patch.object(PointSearch, 'find_point', return_value=expected_point_list) as m_find_point:
            point_list, error_type = safe_get_point_list(self.point_key, self.point_title, self.t_type, self.point_slug)

            assert point_list == expected_point_list
            assert error_type is None
        m_find_point.assert_called_once_with(
            self.point_title, t_type=None, point_key=self.point_key, slug=self.point_slug, can_return_hidden=True
        )

    def test_error_too_general(self):
        self._test_error(StopWordError(self.point_title), 'too_general')

    def test_error_no_points(self):
        self._test_error(NoPointsError(self.point_title), 'point_not_found')

    def test_error_invalid_point_key(self):
        self._test_error(InvalidPointKey(self.point_key), 'point_not_found')

    def test_error_invalid_slug(self):
        self._test_error(InvalidSlug(self.point_slug), 'point_not_found')

    def test_error_too_short(self):
        self._test_error(TooShortError(self.point_title, 3), 'too_short')

    def test_error_station_not_found(self):
        self._test_error(Station.DoesNotExist(), 'station_not_found')

    def test_error_settlement_not_found(self):
        self._test_error(Settlement.DoesNotExist(), 'settlement_not_found')

    def _test_error(self, error, expected_error_type):
        with mock.patch.object(PointSearch, 'find_point', side_effect=error) as m_find_point:
            point_list, error_type = safe_get_point_list(self.point_key, self.point_title, self.t_type)

            assert point_list is None
            assert error_type == expected_error_type
        m_find_point.assert_called_once_with(
            self.point_title, t_type=None, point_key=self.point_key, slug=None, can_return_hidden=True
        )
