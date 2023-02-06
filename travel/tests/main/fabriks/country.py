from __future__ import absolute_import

from travel.avia.backend.repository.country import CountryModel


def create_country_model(**kwargs):
    defaults = dict(
        translated_title_repository=None,
        pk=1,
        title_id=1,
        geo_id=1,
        code='some'
    )

    defaults.update(kwargs)

    return CountryModel(**defaults)
