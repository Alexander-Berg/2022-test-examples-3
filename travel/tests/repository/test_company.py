from __future__ import absolute_import

from travel.avia.library.python.common.models.transport import TransportType
from travel.avia.backend.repository.company import CompanyRepository
from travel.avia.library.python.tester.factories import create_company
from travel.avia.library.python.tester.testcase import TestCase


class CompanyRepositoryTest(TestCase):
    def setUp(self):
        self._repo = CompanyRepository()

    def test_get_all(self):
        c1 = create_company(
            iata='iata1',
            sirena_id='sirena1',
            t_type_id=TransportType.PLANE_ID,
        )
        create_company(
            iata='iata2',
            sirena_id='sirena2',
            t_type_id=TransportType.BLABLACAR
        )  # repository should ignore company with a type other than PLANE_ID

        self._repo.pre_cache()
        companies = self._repo.get_all()

        assert len(companies) == 1
        assert companies[0].iata == c1.iata
        assert companies[0].sirena_id == c1.sirena_id

    def test_get_companies_by_id(self):
        c1 = create_company(
            iata='iata1',
            sirena_id='sirena1',
            t_type_id=TransportType.PLANE_ID,
        )
        c2 = create_company(
            iata='iata2',
            sirena_id='sirena2',
            t_type_id=TransportType.PLANE_ID,
        )
        create_company(
            iata='iata3',
            sirena_id='sirena3',
            t_type_id=TransportType.BLABLACAR
        )  # repository should ignore company with a type other than PLANE_ID

        self._repo.pre_cache()
        companies_by_id = self._repo.get_companies_by_id()

        assert len(companies_by_id) == 2
        assert any(c.iata == c1.iata and c.sirena_id == c1.sirena_id for c in companies_by_id.values())
        assert any(c.iata == c2.iata and c.sirena_id == c2.sirena_id for c in companies_by_id.values())

    def test_get_companies_by_iata(self):
        c1 = create_company(
            iata='iata1',
            sirena_id='sirena1',
            t_type_id=TransportType.PLANE_ID,
        )
        c2 = create_company(
            iata='iata1',
            sirena_id='sirena2',
            t_type_id=TransportType.PLANE_ID,
        )
        c3 = create_company(
            iata='iata2',
            sirena_id='sirena3',
            t_type_id=TransportType.PLANE_ID,
        )

        self._repo.pre_cache()
        companies_by_iata = self._repo.get_companies_by_iata()

        assert len(companies_by_iata) == 2

        assert c1.iata in companies_by_iata
        assert c3.iata in companies_by_iata

        assert len(companies_by_iata[c1.iata]) == 2
        assert len(companies_by_iata[c3.iata]) == 1

        assert any(c.iata == c1.iata and c.sirena_id == c1.sirena_id for c in companies_by_iata[c1.iata])
        assert any(c.iata == c2.iata and c.sirena_id == c2.sirena_id for c in companies_by_iata[c1.iata])
        assert any(c.iata == c3.iata and c.sirena_id == c3.sirena_id for c in companies_by_iata[c3.iata])

    def test_get_companies_by_sirena(self):
        c1 = create_company(
            iata='iata1',
            sirena_id='sirena1',
            t_type_id=TransportType.PLANE_ID,
        )
        c2 = create_company(
            iata='iata2',
            sirena_id='sirena2',
            t_type_id=TransportType.PLANE_ID,
        )

        self._repo.pre_cache()
        companies_by_sirena = self._repo.get_companies_by_sirena()

        assert len(companies_by_sirena) == 2

        assert c1.sirena_id in companies_by_sirena
        assert c2.sirena_id in companies_by_sirena

        assert len(companies_by_sirena[c1.sirena_id]) == 1
        assert len(companies_by_sirena[c2.sirena_id]) == 1

        assert companies_by_sirena[c1.sirena_id][0].sirena_id == c1.sirena_id
        assert companies_by_sirena[c1.sirena_id][0].iata == c1.iata

        assert companies_by_sirena[c2.sirena_id][0].sirena_id == c2.sirena_id
        assert companies_by_sirena[c2.sirena_id][0].iata == c2.iata
