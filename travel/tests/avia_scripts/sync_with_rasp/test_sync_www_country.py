# coding: utf-8
from __future__ import unicode_literals, absolute_import, print_function, division

import pytest
from django.utils.encoding import force_text
from hamcrest import assert_that, contains_inanyorder, has_properties

from travel.avia.admin.avia_scripts.sync_with_rasp.sync_www_country import sync_country
from travel.avia.library.python.common.models.currency import Currency
from travel.avia.library.python.common.models.geo import Country
from travel.avia.library.python.tester.factories import create_country, create_translated_title

pytestmark = [pytest.mark.dbuser]


def test_sync_www_country(rasp_repositories):
    Country.objects.exclude(id=Country.RUSSIA_ID).delete()
    not_in_rasp_country = create_country(id=100500)

    rasp_russia = rasp_repositories.create_country(Id=Country.RUSSIA_ID, GeoId=Country.RUSSIA_GEO_ID,
                                                   Code='RU', Code3='RUS', DomainZone='ru')
    rasp_new_country = rasp_repositories.create_country()

    sync_country(rasp_repositories.country_repository)

    db_countries = list(Country.objects.all())
    assert len(db_countries) == 3
    assert_that(db_countries, contains_inanyorder(
        has_properties(id=not_in_rasp_country.id),
        _has_properties_by_rasp_country(rasp_russia),
        _has_properties_by_rasp_country(rasp_new_country),
    ))


def _has_properties_by_rasp_country(rasp_country):
    return has_properties(
        id=rasp_country.Id,
        _geo_id=rasp_country.GeoId,
        code=rasp_country.Code,
        code3=rasp_country.Code3,
        title=force_text(rasp_country.TitleDefault),
        _kladr_id=rasp_country.KladrId,
        domain_zone=rasp_country.DomainZone,
        currency_id=rasp_country.CurrencyId or None,
        language=rasp_country.Language,
        title_ru=force_text(rasp_country.Title.Ru.Nominative),
        title_ru_genitive=force_text(rasp_country.Title.Ru.Genitive),
        title_ru_locative=force_text(rasp_country.Title.Ru.Prepositional),
        title_ru_preposition_v_vo_na=force_text(rasp_country.Title.Ru.LocativePreposition),
        title_en=force_text(rasp_country.Title.En.Nominative),
        title_uk=force_text(rasp_country.Title.Uk.Nominative),
        title_uk_accusative=force_text(rasp_country.Title.Uk.Accusative),
        title_tr=force_text(rasp_country.Title.Tr.Nominative),
        new_L_title=has_properties(
            ru_nominative=force_text(rasp_country.Title.Ru.Nominative),
            ru_genitive=force_text(rasp_country.Title.Ru.Genitive),
            ru_locative=force_text(rasp_country.Title.Ru.Prepositional),
            en_nominative=force_text(rasp_country.Title.En.Nominative),
            uk_nominative=force_text(rasp_country.Title.Uk.Nominative),
            uk_accusative=force_text(rasp_country.Title.Uk.Accusative),
            tr_nominative=force_text(rasp_country.Title.Tr.Nominative),
        ),
    )


def test_no_currency_id(rasp_repositories):
    rasp_country_without_currency = rasp_repositories.create_country(CurrencyId=0)

    sync_country(rasp_repositories.country_repository)

    assert Country.objects.get(id=rasp_country_without_currency.Id).currency is None


def test_not_existing_currency_id(rasp_repositories):
    not_existing_currency_id = 12345
    Currency.objects.filter(id=not_existing_currency_id).delete()
    rasp_country = rasp_repositories.create_country(CurrencyId=not_existing_currency_id)

    sync_country(rasp_repositories.country_repository)

    assert Country.objects.get(id=rasp_country.Id).currency is None


def test_no_geo_id(rasp_repositories):
    rasp_country_without_geo_id = rasp_repositories.create_country(GeoId=0)

    sync_country(rasp_repositories.country_repository)

    assert Country.objects.get(id=rasp_country_without_geo_id.Id)._geo_id is None


def test_swap_uniq_fields(rasp_repositories):
    Country.objects.all().delete()

    create_country(id=1, _geo_id=1, code='AA', code3='AAA', domain_zone='aa')
    create_country(id=2, _geo_id=2, code='BB', code3='BBB', domain_zone='bb')
    rasp_repositories.create_country(Id=1, GeoId=2, Code='BB', Code3='BBB', DomainZone='bb')
    rasp_repositories.create_country(Id=2, GeoId=1, Code='AA', Code3='AAA', DomainZone='aa')

    sync_country(rasp_repositories.country_repository)

    assert_that(Country.objects.get(id=1), has_properties(
        _geo_id=2, code='BB', code3='BBB', domain_zone='bb'
    ))
    assert_that(Country.objects.get(id=2), has_properties(
        _geo_id=1, code='AA', code3='AAA', domain_zone='aa'
    ))


def test_save_not_rasp_fields(rasp_repositories):
    Country.objects.all().delete()

    create_country(id=1, new_L_title_id=create_translated_title(de_nominative='My strange title').id)
    rasp_repositories.create_country(Id=1)

    sync_country(rasp_repositories.country_repository)

    assert Country.objects.all().count() == 1
    assert Country.objects.get(id=1).new_L_title.de_nominative == 'My strange title'
