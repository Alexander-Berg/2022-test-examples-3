# -*- coding: utf-8 -*-
from __future__ import absolute_import, division, print_function, unicode_literals

import pytest
import six

from util.point_key import PointKey, PointType


class TestPointKey(object):
    def test_load(self):
        assert PointKey.load('c213') == PointKey(PointType.SETTLEMENT, 213)
        assert PointKey.load('s123') == PointKey(PointType.STATION, 123)

        with pytest.raises(ValueError):
            PointKey.load('p123')

        with pytest.raises(ValueError):
            PointKey.load('123')

    def test_str(self):
        assert six.text_type(PointKey(PointType.SETTLEMENT, 213)) == 'c213'
        assert six.text_type(PointKey(PointType.STATION, 123)) == 's123'

    def test_station(self):
        assert PointKey.station(123) == PointKey(PointType.STATION, 123)

    def test_settlement(self):
        assert PointKey.settlement(213) == PointKey(PointType.SETTLEMENT, 213)
