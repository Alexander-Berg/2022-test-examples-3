import pytest

from common.models.geo import Settlement, Station
from common.tester.factories import create_settlement, create_station
from geosearch.views.point import ExcessPointKey, InvalidSlug, PointSearch


@pytest.mark.dbuser
def test_find_point_settlement():
    settlement = create_settlement()
    result = PointSearch.find_point(settlement.title, point_key=settlement.point_key)
    assert result.point == settlement


@pytest.mark.dbuser
def test_find_point_settlement_by_slug():
    settlement = create_settlement()
    result = PointSearch.find_point(settlement.title, slug=settlement.slug)
    assert result.point == settlement


@pytest.mark.dbuser
def test_find_no_point_settlement_by_slug():
    settlement = create_settlement()
    with pytest.raises(InvalidSlug):
        PointSearch.find_point(settlement.title, slug='not_{}'.format(settlement.slug))


@pytest.mark.dbuser
def test_find_point_settlement_by_slug_and_point():
    settlement = create_settlement()
    with pytest.raises(ExcessPointKey):
        PointSearch.find_point(settlement.title, point_key=settlement.point_key, slug=settlement.slug)


@pytest.mark.dbuser
def test_find_point_station():
    station = create_station()
    result = PointSearch.find_point(station.title, point_key=station.point_key)
    assert result.point == station


@pytest.mark.dbuser
def test_find_point_station_by_slug():
    station = create_station()
    result = PointSearch.find_point(station.title, slug=station.slug)
    assert result.point == station


@pytest.mark.dbuser
def test_find_no_point_station_by_slug():
    station = create_station()
    with pytest.raises(InvalidSlug):
        PointSearch.find_point(station.title, slug='not_{}'.format(station.slug))


@pytest.mark.dbuser
def test_find_point_station_by_slug_and_point():
    station = create_station()
    with pytest.raises(ExcessPointKey):
        PointSearch.find_point(station.title, point_key=station.point_key, slug=station.slug)


@pytest.mark.dbuser
def test_find_point_hidden_settlement():
    settlement = create_settlement(hidden=True)
    with pytest.raises(Settlement.DoesNotExist):
        PointSearch.find_point(settlement.title, point_key=settlement.point_key)

    assert PointSearch.find_point(settlement.title, point_key=settlement.point_key, can_return_hidden=True) is not None


@pytest.mark.dbuser
def test_find_point_hidden_settlement_by_slug():
    settlement = create_settlement(hidden=True)
    with pytest.raises(InvalidSlug):
        PointSearch.find_point(settlement.title, slug=settlement.slug)

    assert PointSearch.find_point(settlement.title, slug=settlement.slug, can_return_hidden=True) is not None


@pytest.mark.dbuser
def test_find_point_hidden_settlement_with_geo_id():
    settlement = create_settlement(hidden=True)
    settlement._geo_id = settlement.id
    settlement.save()
    with pytest.raises(Settlement.DoesNotExist):
        PointSearch.find_point(settlement.title, point_key=settlement.point_key)


@pytest.mark.dbuser
def test_find_point_hidden_station():
    station = create_station(hidden=True)
    with pytest.raises(Station.DoesNotExist):
        PointSearch.find_point(station.title, point_key=station.point_key)

    assert PointSearch.find_point(station.title, point_key=station.point_key, can_return_hidden=True) is not None
