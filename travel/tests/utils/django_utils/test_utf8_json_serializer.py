# -*- coding: utf-8 -*-

from travel.avia.library.python.common.models.geo import Station
from travel.avia.library.python.common.utils.django_utils.utf8_json_serializer import Serializer


def test_serializer():
    ss = Serializer()
    ss.serialize([Station(id=1, title_ru=u'Строка в utf8')], fields=('title_ru', ))
    assert ss.getvalue() == u'[{"model": "www.station", "pk": 1, "fields": {"title_ru": "Строка в utf8"}}]'
