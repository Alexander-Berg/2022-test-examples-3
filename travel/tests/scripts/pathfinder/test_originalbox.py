# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

import pytest

from common.models.geo import Station
from common.tester import transaction_context
from common.tester.factories import create_settlement
from travel.rasp.rasp_scripts.scripts.pathfinder.originalbox import OriginalBox


@pytest.fixture(scope='module')
@transaction_context.transaction_fixture
def stations(request):
    result = []
    settlement = create_settlement(title=u'Город')
    settlement.save()
    for title in (u'Первая', u'Вторая', u'Третья'):
        station = Station(title=title, majority_id=1, settlement=settlement)
        station.save()
        result.append(station.id)
    return result


@pytest.mark.dbuser
def test_originalbox(stations):
    StationBox = OriginalBox(
        'title',
        'settlement' | OriginalBox('title')
    )
    stations_qs = Station.objects.filter(id__in=stations)
    plain = StationBox.iter_queryset(stations_qs)
    assert next(plain).settlement.title == u'Город'
    chunked = StationBox.iter_queryset_chunked(stations_qs)
    assert len(chunked) == 3
    assert iter(chunked).next().settlement.title == u'Город'
