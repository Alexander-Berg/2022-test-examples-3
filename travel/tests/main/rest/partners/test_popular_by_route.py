# coding=utf-8
from __future__ import absolute_import

from logging import Logger
from mock import Mock
from typing import cast

import ujson

from travel.avia.library.python.tester.factories import create_partner
from travel.avia.library.python.tester.testcase import TestCase

from travel.avia.backend.main.rest.partners.helpers import PartnerPopularityProvider
from travel.avia.backend.main.rest.partners.popular_by_route import RoutePartnerPopularityView
from travel.avia.backend.repository.partner import PartnerRepository
from travel.avia.backend.repository.partner_popularity import PartnerPopularityModel


class PopularByRouteViewTest(TestCase):
    def setUp(self):
        self._fake_partner_repository = Mock()
        self._fake_popularity_provider = Mock()

        self._view = RoutePartnerPopularityView(
            partner_repository=cast(
                PartnerRepository,
                self._fake_partner_repository
            ),
            partner_popularity_provider=cast(
                PartnerPopularityProvider,
                self._fake_popularity_provider
            ),
            logger=cast(Logger, Mock())
        )

    def test_view(self):
        partner = create_partner(code=u'some')
        another_partner = create_partner(code=u'another')

        self._fake_partner_repository.get_by_id = Mock(
            side_effect={
                partner.id: partner,
                another_partner.id: another_partner
            }.get
        )

        self._fake_popularity_provider.get_for = Mock(
            return_value=[
                PartnerPopularityModel(
                    partner_id=partner.id,
                    score=50
                ),
                PartnerPopularityModel(
                    partner_id=another_partner.id,
                    score=10
                ),
                PartnerPopularityModel(
                    partner_id=None,
                    score=10
                ),
            ]
        )

        result = self._view._unsafe_process({
            'national_version': u'ru',
            'lang': u'ru',
            'from_point_key': 'c213',
            'to_point_key': 'c54',
        })

        result = ujson.loads(result.response[0])
        assert result[u'status'] == u'ok'
        data = result[u'data']
        assert data == [
            {
                u'partner_code': u'some',
                u'score': 50
            },
            {
                u'partner_code': u'another',
                u'score': 10
            }
        ]

    def test_wrong_point_key(self):
        result = self._view._unsafe_process({
            'national_version': u'ru',
            'lang': u'ru',
            'from_point_key': 'c213',
            'to_point_key': 'l983',
        })
        result = ujson.loads(result.response[0])

        assert result[u'status'] == u'error'
        assert result[u'data'] == {'to_point_key': ['should start with s/c']}
