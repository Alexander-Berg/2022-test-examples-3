from __future__ import absolute_import

from travel.avia.library.python.common.models.transport import TransportType
from travel.avia.backend.repository.station import StationModel


def create_airport_model(**kwargs):
    defaults = dict(
        translated_title_repository=None,
        pk=1,
        title_id=1,
        popular_title_id=2,
        iata=None,
        sirena=None,
        settlement_id=None,
        country_id=None,
        region_id=None,
        longitude=0,
        latitude=0,
        time_zone='',
        time_zone_utc_offset=0,
        station_type_id=None,
        transport_type=TransportType.PLANE_ID,
    )

    defaults.update(kwargs)

    return StationModel(**defaults)
