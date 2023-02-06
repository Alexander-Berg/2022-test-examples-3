from __future__ import absolute_import

from travel.avia.backend.repository.settlement import SettlementModel


def create_settlement_model(**kwargs):
    defaults = dict(
        translated_title_repository=None,
        pk=1,
        title_id=1,
        iata=None,
        sirena=None,
        geo_id=None,
        country_id=None,
        region_id=None,
        is_disputed_territory=False,
        majority_id=1,
        pytz=None,
        utcoffset=0,
        latitude=0,
        longitude=0
    )

    defaults.update(kwargs)

    return SettlementModel(**defaults)
