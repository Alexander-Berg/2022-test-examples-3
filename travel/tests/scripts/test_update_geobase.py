# -*- coding: utf-8 -*-

import pytest
import mock

from travel.avia.library.python.common.models.geo import Region
from travel.avia.admin.scripts import update_geobase
from travel.avia.admin.scripts.update_geobase import GeobaseUpdater
from travel.avia.library.python.tester.factories import create_region, create_country


TEST_GEO_ID1 = 11111111


def change_similar(title, dest='rus'):
    return title.replace(u'S', u'Ю')


@pytest.mark.parametrize('title,result', [
    [u'МосквS', u'МосквЮ'],
    [u'  МосквS ', u'МосквЮ'],
    [u'  Москва  ', u'Москва'],
    [u'Москва', u'Москва'],
])
@mock.patch.object(update_geobase, 'change_similar', side_effect=change_similar)
def test_fix_title(_patch, title, result):
    assert result == update_geobase.fix_title(title, 1)


@pytest.mark.dbuser
def test_update_region_by_geo_id():
    updater = GeobaseUpdater()
    country = create_country()
    region = create_region(title=u'', _geo_id=TEST_GEO_ID1, time_zone='', country=country)
    geo_region = mock.Mock()
    geo_region.configure_mock(timezone=b'Europe/Moscow', name=b'Регион', id=TEST_GEO_ID1)

    updater.update_region(geo_region, country)

    region = Region.objects.get(pk=region.pk)
    assert region.title == u'Регион'
    assert region.time_zone == u'Europe/Moscow'


@pytest.mark.dbuser
@pytest.mark.parametrize('attr_name,attr_value', [
    ['title', u'Старое название'],
    ['time_zone', u'Europe/Kiev'],
])
def test_update_region_by_geo_id_ignore_existed(attr_name, attr_value):
    updater = GeobaseUpdater()
    country = create_country()
    create_region_2 = create_region.mutate(title=u'Регион', _geo_id=TEST_GEO_ID1, time_zone='Europe/Moscow', country=country)
    region = create_region_2(**{attr_name: attr_value})
    geo_region = mock.Mock()
    geo_region.configure_mock(timezone=b'Europe/Moscow', name=b'Регион', id=TEST_GEO_ID1)

    updater.update_region(geo_region, country)

    region = Region.objects.get(pk=region.pk)
    assert getattr(region, attr_name) == attr_value


@pytest.mark.dbuser
def test_update_region_ignore_if_not_found_by_id_and_has_no_country():
    updater = GeobaseUpdater()
    country = create_country()
    create_region(title=u'Старый Регион', _geo_id=None, time_zone=u'Europe/Kiev', country=country)
    geo_region = mock.Mock()
    geo_region.configure_mock(timezone=b'Europe/Moscow', name=b'Регион', id=TEST_GEO_ID1)

    assert updater.update_region(geo_region, None) is None
    assert not Region.objects.filter(title=u'Регион').exists()


@pytest.mark.dbuser
def test_update_region_ignore_if_found_multiple_regions():
    updater = GeobaseUpdater()
    country = create_country()
    create_region(title=u'Регион', _geo_id=None, time_zone=u'Europe/Kiev', country=country)
    create_region(title=u'Регион', _geo_id=None, time_zone=u'Europe/Kiev', country=country)
    geo_region = mock.Mock()
    geo_region.configure_mock(timezone=b'Europe/Moscow', name=b'Регион', id=TEST_GEO_ID1)

    assert updater.update_region(geo_region, country) is None
    assert Region.objects.filter(title=u'Регион', country=country).count() == 2


@pytest.mark.dbuser
def test_update_region_if_found_by_name_in_the_same_country():
    updater = GeobaseUpdater()
    country = create_country()
    region = create_region(title=u'Регион', _geo_id=None, time_zone='', country=country)
    geo_region = mock.Mock()
    geo_region.configure_mock(timezone=b'Europe/Moscow', name=b'Регион', id=TEST_GEO_ID1)

    updater.update_region(geo_region, country)

    region = Region.objects.get(pk=region.pk)
    assert region._geo_id == TEST_GEO_ID1
    assert region.time_zone == u'Europe/Moscow'


@pytest.mark.dbuser
def test_update_region_by_geo_id_ignore_bad_timezone():
    updater = GeobaseUpdater()
    country = create_country()
    region = create_region(title=u'', _geo_id=TEST_GEO_ID1, time_zone='', country=country)
    geo_region = mock.Mock()
    geo_region.configure_mock(timezone=b'Europe/Moscowsssssss', name=b'Регион', id=TEST_GEO_ID1)

    updater.update_region(geo_region, country)

    region = Region.objects.get(pk=region.pk)
    assert region.title == u'Регион'
    assert not region.time_zone
