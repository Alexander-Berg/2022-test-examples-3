# -*- coding: utf-8 -*-
from travel.rasp.admin.importinfo.models import StationMapping
from common.models.geo import Station


def make_station(**station_params):
    station = Station(**station_params)
    station.title = station.title or u"_test"
    station.majority_id = station.majority_id or 4
    station.t_type_id = station.t_type_id or 3
    station.save(force_insert=True)
    return station


def make_mapping(station, supplier, **mapping_params):
    return StationMapping.objects.create(station=station, supplier=supplier,
                                                 **mapping_params)
