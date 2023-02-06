# coding=utf-8
from __future__ import absolute_import

from travel.avia.library.python.avia_data.models import (
    PopularPartners, NationalVersion,
    PopularPartnersByRoute
)
from travel.avia.library.python.tester.factories import create_partner
from travel.avia.library.python.tester.testcase import TestCase

from travel.avia.backend.repository.partner_popularity import (
    PartnerPopularityRepository,
    RoutePartnerPopularityRepository
)


class TestPartnerPopularityRepository(TestCase):
    def setUp(self):
        self._repository = PartnerPopularityRepository()
        self._ru_id = NationalVersion.objects.get(code='ru').id
        self._ua_id = NationalVersion.objects.get(code='ua').id

    def _create_stub(self, p, s, nv_id=None):
        PopularPartners(
            national_version_id=nv_id or self._ru_id,
            partner=p,
            score=s
        ).save()

    def _assert(self, actual, expect):
        assert [(x.partner_id, x.score) for x in actual] == expect

    def test_check_sort(self):
        partner = create_partner(code='some1')

        self._create_stub(partner, 10)
        self._create_stub(partner, 20)
        self._create_stub(partner, 1)

        self._repository.pre_cache()
        self._assert(self._repository.get_for(
            national_version_id=self._ru_id
        ), [
            (partner.id, 20),
            (partner.id, 10),
            (partner.id, 1)
        ])

    def test_check_difference_nv(self):
        partner = create_partner(code='some1')

        self._create_stub(partner, 10)
        self._create_stub(partner, 20, nv_id=self._ua_id)

        self._repository.pre_cache()
        self._assert(self._repository.get_for(
            national_version_id=self._ru_id
        ), [
            (partner.id, 10)
        ])
        self._assert(self._repository.get_for(
            national_version_id=self._ua_id
        ), [
            (partner.id, 20)
        ])

    def test_empty(self):
        self._repository.pre_cache()
        self._assert(self._repository.get_for(
            national_version_id=self._ru_id
        ), [])


class TestRoutePartnerPopularityRepository(TestCase):
    def setUp(self):
        self._repository = RoutePartnerPopularityRepository()
        self._ru_id = NationalVersion.objects.get(code='ru').id
        self._ua_id = NationalVersion.objects.get(code='ua').id

    def _create_stub(self, p, s, from_id, to_id, nv_id=None):
        PopularPartnersByRoute(
            national_version_id=nv_id or self._ru_id,
            partner=p,
            score=s,
            from_type=PopularPartnersByRoute.SETTLEMENT_TYPE,
            from_id=from_id,
            to_type=PopularPartnersByRoute.SETTLEMENT_TYPE,
            to_id=to_id
        ).save()

    def _assert(self, actual, expect):
        assert [(x.partner_id, x.score) for x in actual] == expect

    def test_check_sort(self):
        partner = create_partner(code='some1')

        self._create_stub(partner, 100, 213, 54)
        self._create_stub(partner, 200, 213, 54)
        self._create_stub(partner, 10, 213, 54)

        self._repository.pre_cache()
        self._assert(self._repository.get_for(
            national_version_id=self._ru_id,
            from_type=PopularPartnersByRoute.SETTLEMENT_TYPE, from_id=213,
            to_type=PopularPartnersByRoute.SETTLEMENT_TYPE, to_id=54,
        ), [
            (partner.id, 200),
            (partner.id, 100),
            (partner.id, 10)
        ])

    def test_check_difference_route(self):
        partner = create_partner(code='some1')

        self._create_stub(partner, 100, 213, 54)
        self._create_stub(partner, 200, 54, 213)

        self._repository.pre_cache()
        self._assert(self._repository.get_for(
            national_version_id=self._ru_id,
            from_type=PopularPartnersByRoute.SETTLEMENT_TYPE, from_id=213,
            to_type=PopularPartnersByRoute.SETTLEMENT_TYPE, to_id=54,
        ), [
            (partner.id, 100)
        ])
        self._assert(self._repository.get_for(
            national_version_id=self._ru_id,
            from_type=PopularPartnersByRoute.SETTLEMENT_TYPE, from_id=54,
            to_type=PopularPartnersByRoute.SETTLEMENT_TYPE, to_id=213,
        ), [
            (partner.id, 200)
        ])

    def test_empty(self):
        self._repository.pre_cache()
        self._assert(self._repository.get_for(
            national_version_id=self._ru_id,
            from_type=PopularPartnersByRoute.SETTLEMENT_TYPE, from_id=54,
            to_type=PopularPartnersByRoute.SETTLEMENT_TYPE, to_id=213,
        ), [])
