# -*- coding: utf-8 -*-

import pytest

from travel.avia.library.python.geosearch.models import DefaultPoint
from travel.avia.library.python.tester.factories import create_country, create_settlement, create_station


DEFAULT_TITLE = u'название'


@pytest.mark.dbuser
def test_is_default_title_point_notexist():
    assert not DefaultPoint.is_default_title_point(DEFAULT_TITLE, create_settlement())


@pytest.mark.dbuser
@pytest.mark.parametrize('point_type, point_factory', (
    ('settlement', create_settlement),
    ('station', create_station),
))
def test_is_default_title_point_generic(point_type, point_factory):
    point = point_factory()
    default_point = point_factory()
    DefaultPoint.objects.create(title=DEFAULT_TITLE, **{point_type: default_point})

    assert not DefaultPoint.is_default_title_point(DEFAULT_TITLE, point)
    assert DefaultPoint.is_default_title_point(DEFAULT_TITLE, default_point)


@pytest.mark.dbuser
def test_is_default_title_point_country():
    country = create_country()
    DefaultPoint.objects.create(title=DEFAULT_TITLE, settlement=create_settlement())

    assert not DefaultPoint.is_default_title_point(DEFAULT_TITLE, country)
