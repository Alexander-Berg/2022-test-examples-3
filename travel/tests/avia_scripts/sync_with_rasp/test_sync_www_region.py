# coding: utf-8
from __future__ import unicode_literals, absolute_import, print_function, division

import pytest
from django.utils.encoding import force_text
from hamcrest import assert_that, contains_inanyorder, has_properties

from travel.library.python.dicts.factories.rasp_repositories import MSK_TZ_ID, EKB_TZ_ID
from travel.avia.admin.avia_scripts.sync_with_rasp.sync_www_region import sync_region, MIN_NOT_RASP_REGION_ID
from travel.avia.library.python.common.models.geo import Region
from travel.avia.library.python.tester.factories import create_region, create_translated_title

pytestmark = [pytest.mark.dbuser]


def test_sync_www_region(rasp_repositories):
    Region.objects.exclude(id=Region.MOSCOW_REGION_ID).delete()
    not_in_rasp_region = create_region(id=200500)

    rasp_msk = rasp_repositories.create_region(Id=Region.MOSCOW_REGION_ID, GeoId=1, TimeZoneId=MSK_TZ_ID)
    rasp_new_region = rasp_repositories.create_region(TimeZoneId=EKB_TZ_ID)

    sync_region(rasp_repositories.region_repository, rasp_repositories.tz_repository)

    db_regions = list(Region.objects.all())
    assert len(db_regions) == 3
    assert_that(db_regions, contains_inanyorder(
        has_properties(id=not_in_rasp_region.id),
        _has_properties_by_rasp_region(rasp_msk, rasp_repositories.tz_repository),
        _has_properties_by_rasp_region(rasp_new_region, rasp_repositories.tz_repository),
    ))


def _has_properties_by_rasp_region(rasp_region, tz_repository):
    return has_properties(
        id=rasp_region.Id,
        _geo_id=rasp_region.GeoId,
        agent_geo_id=rasp_region.AgentGeoId,
        title=force_text(rasp_region.TitleDefault),
        _kladr_id=rasp_region.KladrId,
        koatuu=rasp_region.Koatuu,
        country_id=rasp_region.CountryId,
        time_zone=tz_repository.get(rasp_region.TimeZoneId).Code,
        hidden=rasp_region.IsHidden,
        disputed_territory=rasp_region.IsDisputedTerritory,
        title_ru=force_text(rasp_region.TitleNominative.Ru),
        title_en=force_text(rasp_region.TitleNominative.En),
        title_uk=force_text(rasp_region.TitleNominative.Uk),
        title_tr=force_text(rasp_region.TitleNominative.Tr),
        new_L_title=has_properties(
            ru_nominative=force_text(rasp_region.TitleNominative.Ru),
            en_nominative=force_text(rasp_region.TitleNominative.En),
            uk_nominative=force_text(rasp_region.TitleNominative.Uk),
            tr_nominative=force_text(rasp_region.TitleNominative.Tr),
        ),
    )


def test_no_geo_id(rasp_repositories):
    rasp_region_without_geo_id = rasp_repositories.create_region(GeoId=0)

    sync_region(rasp_repositories.region_repository, rasp_repositories.tz_repository)

    assert Region.objects.get(id=rasp_region_without_geo_id.Id)._geo_id is None


def test_sync_by_key(rasp_repositories):
    Region.objects.all().delete()

    not_shiftable_id = MIN_NOT_RASP_REGION_ID + 1

    create_region(id=not_shiftable_id, _geo_id=100500, title='Find Me')
    rasp_repositories.create_region(Id=1, GeoId=100500, TitleDefault='Find Me')

    sync_region(rasp_repositories.region_repository, rasp_repositories.tz_repository)

    assert not Region.objects.filter(id=not_shiftable_id).exists()
    assert Region.objects.filter(id=1, _geo_id=100500, title='Find Me').exists()


@pytest.mark.parametrize('id, expected_same_id', (
    (MIN_NOT_RASP_REGION_ID - 200, False),
    (MIN_NOT_RASP_REGION_ID - 1, False),
    (MIN_NOT_RASP_REGION_ID, True),
    (MIN_NOT_RASP_REGION_ID + 1, True),
    (MIN_NOT_RASP_REGION_ID + 200, True),
))
def test_not_in_rasp(rasp_repositories, id,  expected_same_id):
    create_region(id=id, title='Not Rasp Region')

    sync_region(rasp_repositories.region_repository, rasp_repositories.tz_repository)

    region = Region.objects.get(title='Not Rasp Region')
    assert region.hidden
    if expected_same_id:
        assert region.id == id
    else:
        assert region.id != id


def test_shift_more_than_one_region(rasp_repositories):
    create_region(id=MIN_NOT_RASP_REGION_ID - 1, title='Not Rasp Region 1')
    create_region(id=MIN_NOT_RASP_REGION_ID - 2, title='Not Rasp Region 2')

    sync_region(rasp_repositories.region_repository, rasp_repositories.tz_repository)

    assert Region.objects.filter(title__startswith='Not Rasp Region').count() == 2


def test_swap_uniq_fields(rasp_repositories):
    Region.objects.all().delete()

    create_region(id=1, _geo_id=1, koatuu='1')
    create_region(id=2, _geo_id=2, koatuu='2')
    rasp_repositories.create_region(Id=1, GeoId=2, Koatuu='2')
    rasp_repositories.create_region(Id=2, GeoId=1, Koatuu='1')

    sync_region(rasp_repositories.region_repository, rasp_repositories.tz_repository)

    assert_that(Region.objects.get(id=1), has_properties(
        _geo_id=2, koatuu='2'
    ))
    assert_that(Region.objects.get(id=2), has_properties(
        _geo_id=1, koatuu='1'
    ))


def test_save_not_rasp_fields(rasp_repositories):
    Region.objects.all().delete()

    create_region(id=1, new_L_title_id=create_translated_title(de_nominative='My strange title').id)
    rasp_repositories.create_region(Id=1)

    sync_region(rasp_repositories.region_repository, rasp_repositories.tz_repository)

    assert Region.objects.all().count() == 1
    assert Region.objects.get(id=1).new_L_title.de_nominative == 'My strange title'
