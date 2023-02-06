# -*- coding: utf-8 -*-
import pytest

from travel.avia.library.python.django_namedtuples.queryset import ModelInterface

from travel.avia.library.python.common.models.geo import Station
from travel.avia.library.python.tester.factories import create_station


@pytest.mark.dbuser
def test_namedtuplequeryset():
    station_id = 3
    title = u'аэропорт'
    create_station(id=station_id, title=title)
    stations = Station.objects.all().namedtuples('id', 'title')
    assert len(stations) == 1
    s = Station.objects.all().namedtuples('id', 'title').get(id=station_id)
    assert s == stations[0]
    assert s.id == station_id
    assert s.title == title
    assert repr(s) == 'StationTuple(id={!r}, title={!r})'.format(s.id, s.title)
    s = Station.objects.all().namedtuples('pk', 'id').get(id=station_id)
    assert s.pk == s.id == station_id, 'Field pk should work'

    s = Station.objects.all().namedtuples('id', computational={
        'point_key': lambda values: 's{}'.format(values[0])
    }).get(id=station_id)
    assert s.point_key == 's{}'.format(s.id)


@pytest.mark.dbuser
def test_namedtuple_computational_conflict_names():
    station_id = 3
    create_station(id=station_id)
    with pytest.raises(ValueError) as e_info:
        Station.objects.all().namedtuples(
            'id', computational={'id': lambda row: row[0]}
        )
    assert isinstance(e_info.value, ValueError)
    assert str(e_info.value) == "Computational fields conflict: set(['id'])"


@pytest.mark.dbuser
def test_namedtuple_modelinterface():
    title = u'аэропорт'
    create_station(id=3, title=title, time_zone='anytzname')

    class StationTestInterface(ModelInterface):
        _fields = ('id', 'title',)

        def format_id_and_title(self):
            assert hasattr(self, 'id')
            assert hasattr(self, 'title')
            return u'{} {}'.format(self.id, self.title)

    station = Station.objects.all().namedtuples(
        'id', 'time_zone',
        # Do not specify 'title' here!
        interface=StationTestInterface
    )[0]
    assert station.time_zone == 'anytzname'
    assert station.format_id_and_title() == u'{} {}'.format(station.id, title)
    assert station.__class__.__name__ == 'StationTuple'


@pytest.mark.dbuser
def test_modelinterface_uses_computational_fields():
    mow_tzname = 'Europe/Moscow'
    create_station(id=3, time_zone=mow_tzname)
    time_zone = 'anytzname'

    class StationTestInterface(ModelInterface):
        _fields = ('id', 'time_zone',)

        def format_id_and_time_zone(self):
            assert hasattr(self, 'id')
            assert hasattr(self, 'time_zone')
            return u'{} {}'.format(self.id, self.time_zone)

    station = Station.objects.all().namedtuples(
        'id',
        computational={'time_zone': lambda row: time_zone},
        interface=StationTestInterface
    )[0]
    assert station.time_zone == time_zone
    assert (
        station.format_id_and_time_zone() == u'{} {}'.format(station.id, time_zone)
    ), 'Does not used computational'

    station = Station.objects.all().namedtuples(
        'id',
        interface=StationTestInterface
    )[0]
    assert station.time_zone == mow_tzname
    assert (
        station.format_id_and_time_zone() == u'{} {}'.format(station.id, mow_tzname)
    ), 'Does not used value from db'
