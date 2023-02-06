# coding: utf-8

import pytest
from hamcrest import assert_that, has_entries

from common.models.factories import create_aeroex_tariff, create_tariff_type
from common.models.tariffs import TariffType
from travel.rasp.morda_backend.morda_backend.tariffs.suburban.serialization import SuburbanTariffSchema


@pytest.mark.dbuser
def test_suburban_tariff_schema():
    tariff = create_aeroex_tariff(tariff=10)
    assert_that(SuburbanTariffSchema().dump(tariff).data, has_entries({
        'price': has_entries(value=10, currency='RUR'),
        'title': tariff.type.L_title()
    }))

    tariff_type = create_tariff_type(
        code='xxx', title='Title', order=2, is_main=True, link='Link', category=TariffType.USUAL_CATEGORY
    )

    tariff = create_aeroex_tariff(tariff=10, type=tariff_type)
    assert_that(SuburbanTariffSchema().dump(tariff).data, has_entries({
        'price': has_entries(value=10, currency='RUR'),
        'title': 'Title',
        'url': 'Link'
    }))

    replace_type = create_tariff_type(
        code='yyy', title='Replaced', order=2, is_main=True, link='Replaced Link', category=TariffType.USUAL_CATEGORY
    )

    tariff_with_replace = create_aeroex_tariff(
        tariff=10, type=tariff_type, replace_tariff_type=replace_type
    )
    assert_that(SuburbanTariffSchema().dump(tariff_with_replace).data, has_entries({
        'price': has_entries(value=10, currency='RUR'),
        'title': 'Replaced',
        'url': 'Replaced Link'
    }))
