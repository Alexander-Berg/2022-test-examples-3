# coding=utf-8
from logging import Logger
from typing import cast
from mock import Mock

from travel.avia.library.python.avia_data.models import PopularPartners, PopularPartnersByRoute, \
    NationalVersion
from travel.avia.library.python.tester.factories import create_partner
from travel.avia.library.python.tester.testcase import TestCase

from travel.avia.admin.lib.popular_partners_builder import PopularPartnersBuilder
from travel.avia.admin.lib.redirect_provider import RedirectProvider
from travel.avia.admin.lib.yt_helpers import YtTableProvider


class TestAirlinePopularScoreUpdater(TestCase):
    def setUp(self):
        self._redirect_provider = Mock()
        self._fake_yt_table_provider = Mock()
        self._logger = Mock()

        self._builder = PopularPartnersBuilder(
            redirect_provider=cast(RedirectProvider, self._redirect_provider),
            yt_table_provider=cast(
                YtTableProvider, self._fake_yt_table_provider
            ),
            logger=cast(Logger, self._logger),
        )

    def _check_national_popular_partner(self, nv, expected):
        assert list(PopularPartners.objects.filter(
            national_version=NationalVersion.objects.get(code=nv).id
        ).order_by(
            '-score'
        ).values_list('partner_id', 'score')) == expected

    def _check_route_popular_partner(self, nv, route, expected):
        assert list(PopularPartnersByRoute.objects.filter(
            from_type=route[0][0],
            from_id=route[0][1],
            to_type=route[1][0],
            to_id=route[1][1],
            national_version=NationalVersion.objects.get(code=nv).id
        ).order_by(
            '-score'
        ).values_list('partner_id', 'score')) == expected

    def test_check_partner_precise_restrict_on_popular_partner_by_nv(self):
        create_partner(id=1000, code='c_1000')
        create_partner(id=2000, code='c_2000')
        create_partner(id=3000, code='c_3000')

        self._redirect_provider.collect = Mock(
            return_value={
                'ru': {
                    ('c213', 'c54'): {'1000': 91, '2000': 5, '3000': 4}
                },
                'ua': {
                    ('c213', 'c54'): {'3000': 91, '2000': 5, '1000': 4},
                },
            }
        )

        self._builder.update(
            precise=Mock(),
            route_precise=0.70,
            partner_precise=0.95
        )

        self._check_national_popular_partner(
            'ru', [(1000, 0.91), (2000, 0.05)]
        )
        self._check_national_popular_partner(
            'ua', [(3000, 0.91), (2000, 0.05)]
        )

    def test_check_partner_precise_restrict_on_popular_partner_by_route(self):

        create_partner(id=1000, code='c_1000')
        create_partner(id=2000, code='c_2000')
        create_partner(id=3000, code='c_3000')

        self._redirect_provider.collect = Mock(
            return_value={
                'ru': {
                    ('c213', 'c54'): {'1000': 91, '2000': 5, '3000': 4}
                }
            }
        )

        self._builder.update(
            precise=Mock(),
            route_precise=0.70,
            partner_precise=0.95
        )

        CT = PopularPartnersByRoute.SETTLEMENT_TYPE
        self._check_route_popular_partner(
            'ru', ((CT, 213), (CT, 54)), [(1000, 0.91), (2000, 0.05)]
        )

    def test_check_route_precise_restrict(self):
        create_partner(id=1000, code='c_1000')
        create_partner(id=2000, code='c_2000')
        create_partner(id=3000, code='c_3000')

        self._redirect_provider.collect = Mock(
            return_value={
                'ru': {
                    ('c213', 'c1'): {'1000': 20, '2000': 20, '3000': 1},
                    ('c213', 'c2'): {'1000': 20, '2000': 20, '3000': 1},
                    ('c213', 'c3'): {'1000': 20},
                }
            }
        )

        self._builder.update(
            precise=Mock(),
            route_precise=0.70,
            partner_precise=0.95
        )

        assert PopularPartnersByRoute.objects.filter(from_id=213,
                                                     to_id=1).count() == 2
        assert PopularPartnersByRoute.objects.filter(from_id=213,
                                                     to_id=2).count() == 2
        assert PopularPartnersByRoute.objects.filter(from_id=213,
                                                     to_id=3).count() == 0
