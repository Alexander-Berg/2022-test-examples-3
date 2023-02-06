# coding: utf-8
from __future__ import unicode_literals, absolute_import, print_function, division

import pytest
from django.utils.encoding import force_text
from hamcrest import assert_that, contains_inanyorder, has_properties

from travel.avia.admin.avia_scripts.sync_with_rasp.sync_www_company_with_rasp import sync_companies
from travel.avia.library.python.common.models.schedule import Company
from travel.avia.library.python.tester.factories import create_company

pytestmark = [pytest.mark.dbuser]


def test_sync_www_company(rasp_repositories):
    Company.objects.all().delete()
    _company_in_rasp = create_company(id=500500)  # noqa: F841
    company_not_in_rasp = create_company(id=500501)

    rasp_company = rasp_repositories.create_carrier(Id=500500)
    rasp_new_company = rasp_repositories.create_carrier(Id=500555)

    sync_companies(rasp_repositories.carrier_repository)

    db_companies = list(Company.objects.all())
    assert len(db_companies) == 3
    assert_that(db_companies, contains_inanyorder(
        has_properties(id=company_not_in_rasp.id),
        _has_properties_by_rasp_company(rasp_company),
        _has_properties_by_rasp_company(rasp_new_company),
    ))


def _has_properties_by_rasp_company(rasp_company):
    return has_properties(
        id=rasp_company.Id,
        address=rasp_company.Address,
        home_station_id=rasp_company.HomeStationId or None,
        supplier_code=rasp_company.SupplierCode,
        sirena_id=rasp_company.SirenaId,
        iata=rasp_company.Iata,
        icao=rasp_company.Icao,
        icao_ru=rasp_company.IcaoRu,
        t_type_id=rasp_company.TransportType,
        is_freight=rasp_company.IsFreight,
        priority=rasp_company.Priority,
        email=rasp_company.Email,
        contact_info=rasp_company.ContactInfo,
        phone=rasp_company.Phone,
        phone_booking=rasp_company.PhoneBooking,
        description=rasp_company.Description,
        logo=rasp_company.Logo,
        icon=rasp_company.Icon,
        logo_mono=rasp_company.LogoMono,
        country_id=rasp_company.CountryId or None,
        hidden=rasp_company.IsHidden,
        strange=rasp_company.IsStrange,
        meta_title=rasp_company.MetaTitle,
        meta_description=rasp_company.MetaDescription,
        alliance_id=rasp_company.AllianceId or None,
        url=rasp_company.Url,
        title=rasp_company.Title,
        title_en=rasp_company.TitleEn,
        title_ru=rasp_company.TitleRu,
        title_uk=rasp_company.TitleUk,
        title_tr=rasp_company.TitleTr,
        short_title=rasp_company.ShortTitle,
        short_title_ru=rasp_company.ShortTitleRu,
        short_title_en=rasp_company.ShortTitleEn,
        short_title_uk=rasp_company.ShortTitleUk,
        short_title_tr=rasp_company.ShortTitleTr,
        bonus_name=rasp_company.BonusName,
        bonus_name_ru=rasp_company.BonusNameRu,
        bonus_name_en=rasp_company.BonusNameEn,
        bonus_name_uk=rasp_company.BonusNameUk,
        bonus_name_tr=rasp_company.BonusNameTr,
        registration_phone=rasp_company.RegistrationPhone,
        registration_phone_ru=rasp_company.RegistrationPhoneRu,
        registration_phone_en=rasp_company.RegistrationPhoneEn,
        registration_phone_uk=rasp_company.RegistrationPhoneUk,
        registration_phone_tr=rasp_company.RegistrationPhoneTr,
        registration_url=rasp_company.RegistrationUrl,
        registration_url_ru=rasp_company.RegistrationUrlRu,
        registration_url_en=rasp_company.RegistrationUrlEn,
        registration_url_uk=rasp_company.RegistrationUrlUk,
        registration_url_tr=rasp_company.RegistrationUrlTr,
        new_L_title=has_properties(
            ru_nominative=force_text(rasp_company.TitleRu),
            en_nominative=force_text(rasp_company.TitleEn),
            uk_nominative=force_text(rasp_company.TitleUk),
            tr_nominative=force_text(rasp_company.TitleTr),
        ),
    )


@pytest.mark.parametrize('uniq_field, proto_attr', (
    ('supplier_code', 'SupplierCode'),
    ('sirena_id', 'SirenaId'),
    ('icao', 'Icao'),
    ('icao_ru', 'IcaoRu'),
))
def test_move_uniq_field_value_to_other_company(rasp_repositories, uniq_field, proto_attr):
    Company.objects.all().delete()
    _old_company = create_company(id=500500, **{uniq_field: 'FIRST'})  # noqa: F841
    _rasp_old_company = rasp_repositories.create_carrier(Id=500500, **{proto_attr: 'SECOND'})  # noqa: F841
    rasp_new_company = rasp_repositories.create_carrier(Id=300300, **{proto_attr: 'FIRST'})

    sync_companies(rasp_repositories.carrier_repository)

    assert Company.objects.all().count() == 2
    assert Company.objects.get(**{uniq_field: 'FIRST'}).id == rasp_new_company.Id


def test_save_not_rasp_fields(rasp_repositories):
    Company.objects.all().delete()
    create_company(id=1, slug='avia-slug')
    rasp_repositories.create_carrier(Id=1)

    sync_companies(rasp_repositories.carrier_repository)

    assert Company.objects.all().count() == 1
    assert Company.objects.get(id=1).slug == 'avia-slug'
