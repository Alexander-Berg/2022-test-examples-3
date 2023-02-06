# -*- coding: utf-8 -*-

import os
import pytest
import requests
from search.idl.meta_pb2 import TReport


def test():
    endpoint = 'http://{}/maps'.format(os.environ['RECIPE_GEOCODER_SOCKADDR'])
    response = requests.get(endpoint, params={'geocode': 'минск', 'results': 1, 'fsgta': 'll'})

    report = TReport()
    report.ParseFromString(response.content)
    doc = report.Grouping[0].Group[0].Document[0]

    assert doc.ArchiveInfo.Title == 'Минск'

    ll = None
    for gta in doc.FirstStageAttribute:
        if gta.Key == 'll':
            ll = gta.Value

    assert ll is not None
    lon, lat = map(float, ll.split(','))
    assert lon == pytest.approx(27.56, 0.1)
    assert lat == pytest.approx(53.90, 0.1)
