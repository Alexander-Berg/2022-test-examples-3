from __future__ import absolute_import

from travel.avia.backend.repository.region import RegionModel


def create_region_model(**kwargs):
    defaults = dict(
        translated_title_repository=None,
        pk=1,
        title_id=1,
        geo_id=None,
        country_id=None,
        is_disputed_territory=False
    )

    defaults.update(kwargs)

    return RegionModel(**defaults)
