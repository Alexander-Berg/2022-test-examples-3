# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest

from common.data_api.dzv.platfrom_text_replacer import PlatformTextReplacer
from common.models.schedule import PlatformRepresentation
from common.tester.factories import create_station

pytestmark = [pytest.mark.dbuser]


def test_platform_replace():
    station = create_station()
    prp = PlatformRepresentation.objects.create(
        station=station, reg_exp='^(\d+) (\d+)$', representation='{} платф {} путь'
    )
    PlatformRepresentation.objects.create(station=station, reg_exp='^(\d+)$', representation='{} платф')
    replacer = PlatformTextReplacer()

    assert replacer.get_platform_text('5 7', station) == '5 платф 7 путь'
    assert replacer.get_platform_text('5', station) == '5 платф'
    assert replacer.get_platform_text('5 7 9', station) is None
    assert replacer.get_platform_text('5 7lol', station) is None

    prp.representation = '{1} платф {0} путь'
    prp.save()
    replacer = PlatformTextReplacer()
    assert replacer.get_platform_text('5 7', station) == '7 платф 5 путь'
