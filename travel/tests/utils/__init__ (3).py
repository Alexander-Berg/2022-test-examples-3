import factory
import pytz

from travel.rasp.pathfinder_maps.models.route import Route
from travel.rasp.pathfinder_maps.protos.utils import get_empty_result
from travel.rasp.pathfinder_maps.views import Handler


class HandlerFactory(factory.Factory):
    class Meta:
        model = Handler

    path_joiner = None
    polling_service = None


class MordaBackendClientStub:
    def __init__(self, data):
        self._storage = data

    async def get_pm_variants(self, from_type, from_id, to_type, to_id, departure_dt, ttypes):
        return self._storage.get((from_type, from_id, to_type, to_id, departure_dt), [])


class MapsClientStub:
    def __init__(self, data):
        self._storage = data

    async def route(self, rll, pctx=None, dtm=None, atm=None):
        return self._storage.get((rll, dtm), get_empty_result())


def create_station(s_id, title, lon, lat, tz, settlement):
    return {
        'id': s_id,
        'title': title,
        'longitude': lon,
        'latitude': lat,
        'timezone': pytz.timezone(tz),
        'settlement': settlement
    }


def create_settlement(s_id, title):
    return {
        'id': s_id,
        'title': title
    }


def create_route(thread_id, departure_datetime, departure_station_id, arrival_datetime, arrival_station_id, thread_info):
    route = Route(
        thread_id,
        departure_datetime,
        departure_station_id,
        arrival_datetime,
        arrival_station_id
    )
    route.thread_info = thread_info
    return route
