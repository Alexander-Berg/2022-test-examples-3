# coding: utf-8
from __future__ import unicode_literals, absolute_import, print_function, division

import pytest
from hamcrest import assert_that, contains_inanyorder, has_properties

from travel.library.python.dicts.factories.aviaalliance import TAviaAllianceFactory, TAviaAllianceTitleFactory, TAviaAllianceDescriptionFactory
from travel.avia.admin.avia_scripts.sync_with_rasp.sync_www_aviaalliance import sync_aviaalliance
from travel.avia.library.python.common.models.schedule import AviaAlliance
from travel.avia.library.python.tester.factories import create_aviaalliance

pytestmark = [pytest.mark.dbuser]


def test_sync_www_aviaalliance(rasp_repositories):
    # Setup
    AviaAlliance.objects.all().delete()

    # * One item is changed in some way (change fields including translations)
    create_aviaalliance(
        id=1,
        logo_svg='AAA-logo.svg',
        enabled=1,
        title='AAA',
        title_ru='AAA-ru', title_en='AAA-en', title_tr='AAA-tr', title_uk='AAA-ua',
        description='AAA-desc',
        description_ru='AAA-desc-ru', description_en='AAA-desc-en', description_tr='AAA-desc-tr',
        description_uk='AAA-desc-uk',
    )

    # * One item is deleted
    create_aviaalliance(
        id=2,
        logo_svg='BBB-logo.svg',
        enabled=1,
        title='BBB',
        title_ru='BBB-ru', title_en='BBB-en', title_tr='BBB-tr', title_uk='BBB-ua',
        description='BBB-desc',
        description_ru='BBB-desc-ru', description_en='BBB-desc-en', description_tr='BBB-desc-tr',
        description_uk='BBB-desc-uk',
    )

    # * One item is added via proto

    # Test

    rasp_data = [
        TAviaAllianceFactory(
            Id=1,
            LogoSvg='AAA-logo2.svg',
            Enabled=0,
            TitleDefault='AAA2',
            Title=TAviaAllianceTitleFactory(
                Ru='AAA-ru2',
                En='AAA-en2',
                Tr='AAA-tr2',
                Uk='AAA-ua2',
            ),
            DescriptionDefault='AAA-desc2',
            Description=TAviaAllianceDescriptionFactory(
                Ru='AAA-desc-ru2',
                En='AAA-desc-en2',
                Tr='AAA-desc-tr2',
                Uk='AAA-desc-uk2',
            ),
        ),
        TAviaAllianceFactory(
            Id=3,
            LogoSvg='CCC-logo2.svg',
            Enabled=0,
            TitleDefault='CCC2',
            Title=TAviaAllianceTitleFactory(
                Ru='CCC-ru2',
                En='CCC-en2',
                Tr='CCC-tr2',
                Uk='CCC-ua2',
            ),
            DescriptionDefault='CCC-desc2',
            Description=TAviaAllianceDescriptionFactory(
                Ru='CCC-desc-ru2',
                En='CCC-desc-en2',
                Tr='CCC-desc-tr2',
                Uk='CCC-desc-uk2',
            ),
        ),
    ]

    # First sync addes missing
    sync_aviaalliance(rasp_data)

    db_aviaalliance = list(AviaAlliance.objects.all())
    assert len(db_aviaalliance) == 3
    print(repr(db_aviaalliance))
    assert_that(db_aviaalliance, contains_inanyorder(
        has_properties(id=2),
        _has_properties_by_rasp_aviaalliance(rasp_data[0]),
        _has_properties_by_rasp_aviaalliance(rasp_data[1]),
    ))

    # Second sync check stability
    sync_aviaalliance(rasp_data)

    db_aviaalliance = list(AviaAlliance.objects.all())
    assert len(db_aviaalliance) == 3
    assert_that(db_aviaalliance, contains_inanyorder(
        has_properties(id=2),
        _has_properties_by_rasp_aviaalliance(rasp_data[0]),
        _has_properties_by_rasp_aviaalliance(rasp_data[1]),
    ))


def _has_properties_by_rasp_aviaalliance(rasp_aviaalliance):
    return has_properties(
        id=rasp_aviaalliance.Id,
        logo_svg=rasp_aviaalliance.LogoSvg,
        enabled=rasp_aviaalliance.Enabled,
        title=rasp_aviaalliance.TitleDefault,
        title_ru=rasp_aviaalliance.Title.Ru,
        title_en=rasp_aviaalliance.Title.En,
        title_uk=rasp_aviaalliance.Title.Uk,
        title_tr=rasp_aviaalliance.Title.Tr,
        new_L_title=has_properties(
            ru_nominative=rasp_aviaalliance.Title.Ru,
            en_nominative=rasp_aviaalliance.Title.En,
            uk_nominative=rasp_aviaalliance.Title.Uk,
            tr_nominative=rasp_aviaalliance.Title.Tr,
        ),
        description=rasp_aviaalliance.DescriptionDefault,
        description_ru=rasp_aviaalliance.Description.Ru,
        description_en=rasp_aviaalliance.Description.En,
        description_uk=rasp_aviaalliance.Description.Uk,
        description_tr=rasp_aviaalliance.Description.Tr,
        new_L_description=has_properties(
            ru=rasp_aviaalliance.Description.Ru,
            en=rasp_aviaalliance.Description.En,
            uk=rasp_aviaalliance.Description.Uk,
            tr=rasp_aviaalliance.Description.Tr,
        ),
    )
