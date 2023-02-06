from __future__ import absolute_import

from travel.avia.backend.repository.airlines import AirlineModel
from travel.avia.backend.repository.helpers import NationalBox
from travel.avia.library.python.tester.factories import create_translated_title


def create_airline_model(
        title, translated_title_repository=None,
        pk=1, slug='',
        popular_score_by_national_version=NationalBox({}),
        iata=None, sirena=None, icao=None, icao_ru=None, tariff=None, seo_description_key=None,
        alliance_id=None, baggage_rules_url='', baggage_rules='',
        logo='', logo_bgcolor='', registration_url='', url='',
        registration_url_locals=None, registration_phone='',
        registration_phone_locals=None,
        carryon_width=None,
        carryon_length=None,
        carryon_height=None,
        carryon_dimensions_sum=None,
        baggage_width=None,
        baggage_length=None,
        baggage_height=None,
        baggage_dimensions_sum=None,

):
    title_model = create_translated_title(ru_nominative=title)
    if translated_title_repository:
        translated_title_repository.fetch({title_model.id})

    return AirlineModel(
        translated_title_repository=translated_title_repository or {},
        pk=pk, title_id=title_model.id, slug=slug,
        popular_score_by_national_version=popular_score_by_national_version,
        iata=iata, sirena=sirena, icao=icao, icao_ru=icao_ru, tariff=tariff,
        seo_description_key=seo_description_key,
        alliance_id=alliance_id, baggage_rules_url=baggage_rules_url,
        baggage_rules=baggage_rules,
        logo=logo, logo_bgcolor=logo_bgcolor,
        registration_url=registration_url, url=url,
        registration_url_locals=registration_url_locals,
        registration_phone=registration_phone,
        registration_phone_locals=registration_phone_locals,
        hidden=False,
        carryon_width=carryon_width,
        carryon_length=carryon_length,
        carryon_height=carryon_height,
        carryon_dimensions_sum=carryon_dimensions_sum,
        baggage_width=baggage_width,
        baggage_length=baggage_length,
        baggage_height=baggage_height,
        baggage_dimensions_sum=baggage_dimensions_sum,
    )
