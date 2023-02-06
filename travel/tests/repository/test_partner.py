# coding=utf-8
from __future__ import absolute_import

from travel.avia.library.python.tester.factories import create_partner
from travel.avia.library.python.tester.testcase import TestCase

from travel.avia.backend.repository.partner import PartnerRepository


class TestPartnerRepository(TestCase):
    def setUp(self):
        self._repository = PartnerRepository()

    def test_get_by_id(self):
        partner = create_partner(code='some1')
        other_partner = create_partner(code='some2')

        self._repository.pre_cache()

        model = self._repository.get_by_id(model_id=partner.id)
        assert model.pk == partner.id
        assert model.code == partner.code

        other_model = self._repository.get_by_id(model_id=other_partner.id)
        assert other_model.pk == other_partner.id
        assert other_model.code == other_partner.code

    def test_get_by_code(self):
        partner = create_partner(code='some1')
        other_partner = create_partner(code='some2')

        self._repository.pre_cache()

        model = self._repository.get_by_code(code=partner.code)
        assert model.pk == partner.id
        assert model.code == partner.code

        other_model = self._repository.get_by_code(code=other_partner.code)
        assert other_model.pk == other_partner.id
        assert other_model.code == other_partner.code
