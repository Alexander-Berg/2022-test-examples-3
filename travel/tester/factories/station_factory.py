# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from django.db import models

from travel.rasp.library.python.common23.date.date_const import MSK_TIMEZONE
from travel.rasp.library.python.common23.models.core.directions.direction_marker import DirectionMarker
from travel.rasp.library.python.common23.models.core.directions.external_direction import ExternalDirection
from travel.rasp.library.python.common23.models.core.geo.station import Station
from travel.rasp.library.python.common23.tester.factories.base_factory import ModelFactory, factories
from travel.rasp.library.python.common23.tester.factories.factories import (
    DEFAULT_TRANSPORT_TYPE, create_settlement, create_direction, create_station_phone, create_station_code,
    create_external_direction, create_external_direction_marker
)


class StationFactory(ModelFactory):
    Model = Station
    default_kwargs = {
        'settlement': None,
        'title': 'НазваниеСтанции',
        't_type': DEFAULT_TRANSPORT_TYPE,
        'majority': 'main_in_city',
        'time_zone': MSK_TIMEZONE,
        'latitude': 1,
        'longitude': 1,
    }

    def create_object(self, kwargs):
        extra_params = kwargs.pop('__', None) or {}

        settlement = create_settlement(kwargs.pop('settlement', None))
        if settlement:
            kwargs['time_zone'] = kwargs.pop('time_zone', settlement.time_zone)
            kwargs['latitude'] = kwargs.pop('latitude', settlement.latitude)
            kwargs['longitude'] = kwargs.pop('longitude', settlement.longitude)
            kwargs['settlement'] = settlement

        station = super(StationFactory, self).create_object(kwargs)

        for system, code in extra_params.get('codes', {}).items():
            create_station_code(station=station, system=system, code=code)

        for phone_data in extra_params.get('phones', []):
            assert 'station' not in phone_data
            phone_data['station'] = station
            create_station_phone(**phone_data)

        direction = create_direction(extra_params.get('direction', None))
        if direction:
            dm_order = DirectionMarker.objects.aggregate(models.Max('order')).values()[0]

            DirectionMarker.objects.create(
                direction=direction, station=station,
                order=(0 if dm_order is None else dm_order + 1)
            )

        for ext_direction_params in extra_params.get('ext_directions', []):
            if isinstance(ext_direction_params, ExternalDirection):
                ext_direction = ext_direction_params
            else:
                ext_direction = create_external_direction(**ext_direction_params)

            create_external_direction_marker(station=station, external_direction=ext_direction)

        return station


create_station = StationFactory()
factories[Station] = create_station
