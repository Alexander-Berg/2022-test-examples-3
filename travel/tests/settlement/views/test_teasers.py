# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import json

import mock
from django.test import Client
from hamcrest import assert_that, contains, has_entries

from common.models.teasers import Teaser
from common.tester.factories import create_settlement
from common.tester.testcase import TestCase


class TestSettlementTeasers(TestCase):
    def setUp(self):
        self.client = Client()
        self.settlement = create_settlement()

    def test_teasers(self):
        teaser_dict = {
            'normal': [
                Teaser(id=1),
                Teaser(id=2)
            ],
            'special': [
                Teaser(id=3),
                Teaser(id=4)
            ]
        }

        p_fetch_rasp_teasers = mock.patch('common.views.teasers.fetch_rasp_teasers', return_value=teaser_dict)

        with p_fetch_rasp_teasers as m_fetch_rasp_teasers:
            response = self.client.get('/uk/settlement/{}/teasers/?national_version=ua'.format(self.settlement.id))

            assert response.status_code == 200

            result = json.loads(response.content)

            assert_that(result, has_entries({
                'normal': contains(has_entries({'id': 1}), has_entries({'id': 2})),
                'special': contains(has_entries({'id': 3}), has_entries({'id': 4})),
            }))

        m_fetch_rasp_teasers.assert_called_once_with(['index', 'info_settlement'], self.settlement, False, u'ua', u'uk')
