# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import json

from django.test import Client

from common.models.geo import SuburbanZone
from common.tester.factories import create_settlement, create_station
from common.tester.testcase import TestCase


class TestSettlementDirections(TestCase):
    def setUp(self):
        self.client = Client()

        self.c1 = create_settlement()
        self.c1.suburban_zone = SuburbanZone.objects.create(settlement=self.c1, title='test')
        self.c1.save()
        self.c1.suburban_zone.externaldirection_set.create(code='test', title='test', full_title='full_test',
                                                           title_uk='test uk')

        connected_direction = self.c1.suburban_zone.externaldirection_set.create(code='connected', title='connected',
                                                                                 full_title='connected')
        connected_station = create_station(settlement=self.c1)
        connected_direction.externaldirectionmarker_set.create(station=connected_station, order=10)

        self.c2 = create_settlement()

    def test_directions(self):
        response = self.client.get('/uk/settlement/{}/directions/'.format(self.c1.id))

        assert response.status_code == 200
        data = json.loads(response.content)
        assert data == [
            {u'code': u'connected', u'connected': True, u'title': u'connected'},
            {u'code': u'test', u'connected': False, u'title': u'test uk'},
        ]

    def test_empty_directions(self):
        response = self.client.get('/uk/settlement/{}/directions/'.format(self.c2.id))

        assert response.status_code == 200
        data = json.loads(response.content)
        assert data == []
