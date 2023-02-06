# coding: utf-8
from __future__ import unicode_literals, absolute_import, print_function, division

import pytest
from django.utils.encoding import force_text
from hamcrest import assert_that, contains_inanyorder, has_properties

from travel.library.python.dicts.factories.rasp_repositories import MSK_TZ_ID, EKB_TZ_ID
from travel.avia.admin.avia_scripts.sync_with_rasp.sync_www_settlement import (
    sync_settlement, MIN_NOT_RASP_SETTLEMENT_ID, SETTLEMENT_UNIQ_FIELDS_AND_PROTO_ATTRS
)
from travel.avia.library.python.common.models.geo import Settlement
from travel.avia.library.python.tester.factories import create_settlement, create_translated_title

pytestmark = [pytest.mark.dbuser]


def test_sync_www_settlement(rasp_repositories):
    Settlement.objects.exclude(id=Settlement.MOSCOW_ID).delete()
    not_in_rasp_settlement = create_settlement(id=500500)

    rasp_msk = rasp_repositories.create_settlement(Id=Settlement.MOSCOW_ID, GeoId=1, TimeZoneId=MSK_TZ_ID)
    rasp_new_settlement = rasp_repositories.create_settlement(TimeZoneId=EKB_TZ_ID)

    sync_settlement(rasp_repositories.settlement_repository, rasp_repositories.tz_repository)

    db_settlements = list(Settlement.objects.all())
    assert len(db_settlements) == 3
    assert_that(db_settlements, contains_inanyorder(
        has_properties(id=not_in_rasp_settlement.id),
        _has_properties_by_rasp_settlement(rasp_msk, rasp_repositories.tz_repository),
        _has_properties_by_rasp_settlement(rasp_new_settlement, rasp_repositories.tz_repository),
    ))


def _has_properties_by_rasp_settlement(rasp_settlement, tz_repository):
    return has_properties(
        id=rasp_settlement.Id,
        _geo_id=rasp_settlement.GeoId,
        agent_geo_id=rasp_settlement.AgentGeoId,
        _kladr_id=rasp_settlement.KladrId,
        koatuu=rasp_settlement.Koatuu,
        country_id=rasp_settlement.CountryId or None,
        time_zone=tz_repository.get(rasp_settlement.TimeZoneId).Code,
        hidden=rasp_settlement.IsHidden,
        _disputed_territory=rasp_settlement.IsDisputedTerritory,
        majority_id=rasp_settlement.Majority,
        suggest_order=rasp_settlement.SuggestOrder,
        region_id=rasp_settlement.RegionId or None,
        phone_info=rasp_settlement.PhoneInfo,
        phone_info_short=rasp_settlement.PhoneInfoShort,
        sirena_id=rasp_settlement.SirenaId,
        iata=rasp_settlement.Iata,
        big_city=rasp_settlement.BigCity,
        has_tablo=rasp_settlement.HasTablo,
        has_many_airports=rasp_settlement.HasManyAirports,
        longitude=rasp_settlement.Longitude,
        latitude=rasp_settlement.Latitude,

        title=force_text(rasp_settlement.TitleDefault),
        title_ru=force_text(rasp_settlement.Title.Ru.Nominative),
        title_ru_preposition_v_vo_na=force_text(rasp_settlement.Title.Ru.LocativePreposition),
        title_ru_genitive=force_text(rasp_settlement.Title.Ru.Genitive),
        title_ru_accusative=force_text(rasp_settlement.Title.Ru.Accusative),
        title_ru_locative=force_text(rasp_settlement.Title.Ru.Prepositional),
        title_en=force_text(rasp_settlement.Title.En.Nominative),
        title_uk=force_text(rasp_settlement.Title.Uk.Nominative),
        title_tr=force_text(rasp_settlement.Title.Tr.Nominative),
        new_L_title=has_properties(
            ru_nominative=force_text(rasp_settlement.Title.Ru.Nominative),
            ru_genitive=force_text(rasp_settlement.Title.Ru.Genitive),
            ru_accusative=force_text(rasp_settlement.Title.Ru.Accusative),
            ru_locative=force_text(rasp_settlement.Title.Ru.Prepositional),
            en_nominative=force_text(rasp_settlement.Title.En.Nominative),
            uk_nominative=force_text(rasp_settlement.Title.Uk.Nominative),
            tr_nominative=force_text(rasp_settlement.Title.Tr.Nominative),
        ),

        abbr_title=force_text(rasp_settlement.AbbrTitleDefault),
        abbr_title_ru=force_text(rasp_settlement.AbbrTitle.Ru),
        abbr_title_en=force_text(rasp_settlement.AbbrTitle.En),
        abbr_title_uk=force_text(rasp_settlement.AbbrTitle.Uk),
        abbr_title_tr=force_text(rasp_settlement.AbbrTitle.Tr),
        new_L_abbr_title=has_properties(
            ru_nominative=force_text(rasp_settlement.AbbrTitle.Ru),
            uk_nominative=force_text(rasp_settlement.AbbrTitle.Uk),
            en_nominative=force_text(rasp_settlement.AbbrTitle.En),
            tr_nominative=force_text(rasp_settlement.AbbrTitle.Tr),
        )
    )


def test_no_geo_id(rasp_repositories):
    rasp_settlement_without_geo_id = rasp_repositories.create_settlement(Id=200300, GeoId=0)

    sync_settlement(rasp_repositories.settlement_repository, rasp_repositories.tz_repository)

    assert Settlement.objects.get(id=rasp_settlement_without_geo_id.Id)._geo_id is None


def test_sync_by_geo_id(rasp_repositories):
    create_settlement(id=100500, _geo_id=100500)
    rasp_repositories.create_settlement(Id=200300, GeoId=100500)

    sync_settlement(rasp_repositories.settlement_repository, rasp_repositories.tz_repository)

    assert not Settlement.objects.filter(id=100500).exists()
    assert Settlement.objects.filter(id=200300, _geo_id=100500).exists()


def test_do_not_sync_by_geo_id_if_can_sync_by_id(rasp_repositories):
    city = create_settlement(id=100001, _geo_id=100002)
    rasp_repositories.create_settlement(Id=city.id, GeoId=100500)
    rasp_repositories.create_settlement(Id=100002, GeoId=city._geo_id)

    sync_settlement(rasp_repositories.settlement_repository, rasp_repositories.tz_repository)

    assert Settlement.objects.get(id=city.id)._geo_id == 100500
    assert Settlement.objects.get(id=100002)._geo_id == city._geo_id


def test_do_not_map_by_empty_geo_id(rasp_repositories):
    Settlement.objects.all().delete()

    create_settlement(id=100001, _geo_id=None)
    rasp_repositories.create_settlement(Id=200002, GeoId=None)

    sync_settlement(rasp_repositories.settlement_repository, rasp_repositories.tz_repository)

    assert Settlement.objects.all().count() == 2


@pytest.mark.parametrize('id, expected_same_id', (
    (MIN_NOT_RASP_SETTLEMENT_ID - 200, False),
    (MIN_NOT_RASP_SETTLEMENT_ID - 1, False),
    (MIN_NOT_RASP_SETTLEMENT_ID, True),
    (MIN_NOT_RASP_SETTLEMENT_ID + 1, True),
    (MIN_NOT_RASP_SETTLEMENT_ID + 200, True),
))
def test_not_in_rasp(rasp_repositories, id,  expected_same_id):
    create_settlement(id=id, title='Not Rasp Settlement')

    sync_settlement(rasp_repositories.settlement_repository, rasp_repositories.tz_repository)

    settlement = Settlement.objects.get(title='Not Rasp Settlement')
    assert settlement.hidden
    if expected_same_id:
        assert settlement.id == id
    else:
        assert settlement.id != id


def test_shift_more_than_one_settlement(rasp_repositories):
    create_settlement(id=MIN_NOT_RASP_SETTLEMENT_ID - 1, title='Not Rasp Settlement 1')
    create_settlement(id=MIN_NOT_RASP_SETTLEMENT_ID - 2, title='Not Rasp Settlement 2')

    sync_settlement(rasp_repositories.settlement_repository, rasp_repositories.tz_repository)

    assert Settlement.objects.filter(title__startswith='Not Rasp Settlement').count() == 2


def test_swap_geo_id(rasp_repositories):
    geo_id_1 = 123456
    geo_id_2 = 123457
    create_settlement(id=100500, _geo_id=geo_id_1)
    create_settlement(id=200300, _geo_id=geo_id_2)

    rasp_repositories.create_settlement(Id=200300, GeoId=geo_id_1)
    rasp_repositories.create_settlement(Id=100500, GeoId=geo_id_2)

    sync_settlement(rasp_repositories.settlement_repository, rasp_repositories.tz_repository)

    assert Settlement.objects.filter(_geo_id=geo_id_1).count() == 1
    assert Settlement.objects.filter(_geo_id=geo_id_2).count() == 1


def test_duplicate_geo_id(rasp_repositories):
    geo_id = 123458
    create_settlement(id=100500, _geo_id=geo_id)
    create_settlement(id=200300, _geo_id=None)

    rasp_repositories.create_settlement(Id=200300, GeoId=geo_id)

    sync_settlement(rasp_repositories.settlement_repository, rasp_repositories.tz_repository)


def test_do_not_lose_geo_id(rasp_repositories):
    geo_id = 123458
    rasp_repositories.create_settlement(Id=100500, GeoId=geo_id)
    # create our settlement in db with the same data
    sync_settlement(rasp_repositories.settlement_repository, rasp_repositories.tz_repository)

    # actually test that we don't lose geo_id
    sync_settlement(rasp_repositories.settlement_repository, rasp_repositories.tz_repository)

    assert Settlement.objects.get(id=100500)._geo_id == geo_id


@pytest.mark.parametrize('field, proto_attr', SETTLEMENT_UNIQ_FIELDS_AND_PROTO_ATTRS)
def test_swap_uniq_field(rasp_repositories, field, proto_attr):
    value_1 = '1'
    value_2 = '2'
    create_settlement(id=100500, **{field: value_1})
    create_settlement(id=200300, **{field: value_2})

    rasp_repositories.create_settlement(Id=200300, **{proto_attr: value_1})
    rasp_repositories.create_settlement(Id=100500, **{proto_attr: value_2})

    sync_settlement(rasp_repositories.settlement_repository, rasp_repositories.tz_repository)

    assert Settlement.objects.get(**{field: value_1}).id == 200300
    assert Settlement.objects.get(**{field: value_2}).id == 100500


@pytest.mark.parametrize('field, proto_attr', SETTLEMENT_UNIQ_FIELDS_AND_PROTO_ATTRS)
def test_duplicate_uniq_field(rasp_repositories, field, proto_attr):
    value = 'CODE'
    create_settlement(id=100500, **{field: value})
    create_settlement(id=200300, **{field: None})

    rasp_repositories.create_settlement(Id=200300, **{proto_attr: value})

    sync_settlement(rasp_repositories.settlement_repository, rasp_repositories.tz_repository)


@pytest.mark.parametrize('field, proto_attr', SETTLEMENT_UNIQ_FIELDS_AND_PROTO_ATTRS)
def test_do_not_lose_uniq_field(rasp_repositories, field, proto_attr):
    value = 'CODE'
    rasp_repositories.create_settlement(Id=100500, **{proto_attr: value})
    # create our settlement in db with the same data
    sync_settlement(rasp_repositories.settlement_repository, rasp_repositories.tz_repository)

    # actually test that we don't lose geo_id
    sync_settlement(rasp_repositories.settlement_repository, rasp_repositories.tz_repository)

    assert getattr(Settlement.objects.get(id=100500), field) == value


@pytest.mark.parametrize('field, proto_attr', SETTLEMENT_UNIQ_FIELDS_AND_PROTO_ATTRS)
def test_do_not_lose_uniq_field_in_not_rasp_settlement(rasp_repositories, field, proto_attr):
    value = 'CODE'
    create_settlement(id=100500, **{field: value})

    sync_settlement(rasp_repositories.settlement_repository, rasp_repositories.tz_repository)

    assert Settlement.objects.filter(**{field: value}).count() == 1


def test_save_not_rasp_fields(rasp_repositories):
    Settlement.objects.all().delete()

    create_settlement(
        id=1,
        service_comment='My strange service comment',
        new_L_title_id=create_translated_title(de_nominative='My strange title').id,
        new_L_abbr_title_id=create_translated_title(de_nominative='My strange title 2').id,
    )
    rasp_repositories.create_settlement(Id=1)

    sync_settlement(rasp_repositories.settlement_repository, rasp_repositories.tz_repository)

    assert Settlement.objects.all().count() == 1
    settlement = Settlement.objects.get(id=1)
    assert settlement.service_comment == 'My strange service comment'
    assert settlement.new_L_title.de_nominative == 'My strange title'
    assert settlement.new_L_abbr_title.de_nominative == 'My strange title 2'
