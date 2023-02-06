# coding: utf8

from __future__ import unicode_literals

import pytest
from hamcrest import assert_that, has_entries, ends_with

from common.apps.facility.factories import create_suburban_facility
from travel.rasp.morda_backend.morda_backend.serialization.segment_suburban_facilities import SuburbanFacilitySchema


@pytest.mark.dbuser
def test_suburban_facility_schema():
    facility = create_suburban_facility(code='motobike', title_ru='Провоз мотоцикла')
    facility.icon.name = 'moto.svg'
    facility.save()

    assert_that(SuburbanFacilitySchema().dump(facility).data, has_entries({
        'code': 'motobike',
        'title': 'Провоз мотоцикла',
        'icon': ends_with('moto.svg')
    }))
