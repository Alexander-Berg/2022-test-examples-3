from __future__ import absolute_import

from travel.avia.backend.repository.iata_correction import IataCorrectionRepository
from travel.avia.library.python.tester.factories import create_company, create_iatacorrection
from travel.avia.library.python.tester.testcase import TestCase


class IataCorrectionRepositoryTest(TestCase):
    def setUp(self):
        self._repo = IataCorrectionRepository()

    def test_get_all(self):
        company1 = create_company()
        c1 = create_iatacorrection(
            code='C1',
            number='\\d\\d',
            company_id=company1.id
        )

        company2 = create_company()
        c2 = create_iatacorrection(
            code='C2',
            number='\\d',
            company_id=company2.id
        )

        self._repo.pre_cache()
        corrections = self._repo.get_all()

        assert len(corrections) == 2
        assert any(c.code == c1.code and c.number == c1.number and c.company_id == c1.company_id for c in corrections)
        assert any(c.code == c2.code and c.number == c2.number and c.company_id == c2.company_id for c in corrections)

    def test_get_corrections_by_iata(self):
        company1 = create_company()
        c1 = create_iatacorrection(
            code='C1',
            number='\\d\\d',
            company_id=company1.id
        )

        company2 = create_company()
        c2 = create_iatacorrection(
            code='C1',
            number='\\d\\d',
            company_id=company2.id
        )

        company3 = create_company()
        c3 = create_iatacorrection(
            code='C2',
            number='\\d',
            company_id=company3.id
        )

        self._repo.pre_cache()
        corrections_by_iata = self._repo.get_corrections_by_iata()

        assert len(corrections_by_iata) == 2
        assert c1.code in corrections_by_iata
        assert c3.code in corrections_by_iata

        assert len(corrections_by_iata[c1.code]) == 2
        assert len(corrections_by_iata[c3.code]) == 1

        assert any(
            c.code == c1.code and c.number == c1.number and c.company_id == c1.company_id
            for c in corrections_by_iata[c1.code]
        )
        assert any(
            c.code == c2.code and c.number == c2.number and c.company_id == c2.company_id
            for c in corrections_by_iata[c1.code]
        )
        assert any(
            c.code == c3.code and c.number == c3.number and c.company_id == c3.company_id
            for c in corrections_by_iata[c3.code]
        )
