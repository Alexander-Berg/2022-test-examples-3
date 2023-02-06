# -*- coding: utf-8 -*-

import pytest
from django.db import transaction

from common.models.geo import Station
from tester.factories import create_station


@pytest.mark.dbuser
def test_atomic_rollback():
    create_station(title=u'StationName1')
    with transaction.atomic():
        create_station(title=u'StationName2')
        transaction.set_rollback(True)

    assert Station.objects.get(title=u'StationName1')

    with pytest.raises(Station.DoesNotExist):
        Station.objects.get(title=u'StationName2')
