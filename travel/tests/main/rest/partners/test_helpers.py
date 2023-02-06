from __future__ import absolute_import

from mock import Mock
from typing import cast

from travel.avia.library.python.tester.testcase import TestCase

from travel.avia.backend.main.rest.partners.helpers import PartnerPopularityProvider
from travel.avia.backend.repository.national_version import national_version_repository
from travel.avia.backend.repository.partner_popularity import (
    PartnerPopularityRepository,
    RoutePartnerPopularityRepository, PartnerPopularityModel
)


class TestPartnerPopularityProvider(TestCase):
    def setUp(self):
        self._fake_partner_popularity_repository = Mock()
        self._fake_route_partner_popularity_repository = Mock()

        self._provider = PartnerPopularityProvider(
            partner_popularity_repository=cast(
                PartnerPopularityRepository,
                self._fake_partner_popularity_repository
            ),
            route_partner_popularity_repository=cast(
                RoutePartnerPopularityRepository,
                self._fake_route_partner_popularity_repository
            ),
            national_version_repository=national_version_repository
        )

    def test_prefer_route_info(self):
        self._fake_route_partner_popularity_repository.get_for = Mock(
            return_value=[
                PartnerPopularityModel(
                    partner_id=2,
                    score=420
                )
            ]
        )
        self._fake_partner_popularity_repository.get_for = Mock(return_value=[
            PartnerPopularityModel(
                partner_id=1,
                score=42
            )
        ])

        answer = self._provider.get_for(
            from_point_key='c213',
            to_point_key='c54',
            national_version='ru'
        )

        assert len(answer) == 1
        answer = answer[0]
        assert answer.partner_id == 2
        assert answer.score == 420

    def test_check_fallback(self):
        self._fake_route_partner_popularity_repository.get_for = Mock(
            return_value=[]
        )
        self._fake_partner_popularity_repository.get_for = Mock(return_value=[
            PartnerPopularityModel(
                partner_id=1,
                score=42
            )
        ])

        answer = self._provider.get_for(
            from_point_key='c213',
            to_point_key='c54',
            national_version='ru'
        )

        assert len(answer) == 1
        answer = answer[0]
        assert answer.partner_id == 1
        assert answer.score == 42
