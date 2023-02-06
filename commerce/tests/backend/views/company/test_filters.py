from rest_framework.test import APITestCase, APIClient

from commerce.adv_backend.backend.utils import reverse

from commerce.adv_backend.tests.factory import TldFactory, CountryFactory,  CityFactory, CompanyCertificateFactory, CompanyOfficeFactory, CompanyFactory


class TestViewCompaniesFiltersCases(APITestCase):
    client = APIClient()
    maxDiff = None

    def test_get_filter_by_tld(self):
        CompanyOfficeFactory.create(
            company=CompanyFactory.create(
                tld=[
                    TldFactory.create(value='ru')
                ],
                certificates=[
                    CompanyCertificateFactory.create(code='direct', is_regional=True)
                ],
                is_active=True
            ),
            city=CityFactory.create(
                name='Yekaterinburg',
                display_priority=0,
                country=CountryFactory.create(name='Russia')
            )
        )
        CompanyOfficeFactory.create(
            company=CompanyFactory.create(
                tld=[
                    TldFactory.create(value='ru')
                ],
                certificates=[
                    CompanyCertificateFactory.create(code='direct', is_regional=True),
                    CompanyCertificateFactory.create(code='metrika')
                ],
                direct_budget=15000,
                is_active=True
            ),
            city=CityFactory.create(
                name='Moscow',
                display_priority=2,
                country=CountryFactory.create(name='Russia')
            )
        )
        CompanyOfficeFactory.create(
            company=CompanyFactory.create(
                tld=[
                    TldFactory.create(value='ru')
                ],
                certificates=[
                    CompanyCertificateFactory.create(code='metrika')
                ],
                is_active=True
            ),
            city=CityFactory.create(
                name='Saint Petersburg',
                display_priority=1,
                country=CountryFactory.create(name='Russia')
            )
        )
        CompanyOfficeFactory.create(
            company=CompanyFactory.create(
                tld=[
                    TldFactory.create(value='ru')
                ],
                certificates=[
                    CompanyCertificateFactory.create(code='metrika')
                ],
                is_active=True
            ),
            city=CityFactory.create(
                name='Kazan',
                display_priority=0,
                country=CountryFactory.create(name='Russia')
            )
        )
        CompanyOfficeFactory.create(
            company=CompanyFactory.create(
                tld=[
                    TldFactory.create(value='ru')
                ],
                certificates=[
                    CompanyCertificateFactory.create(code='direct', is_regional=True)
                ],
                direct_budget=13000,
                is_active=True
            ),
            city=CityFactory.create(
                name='Tel Aviv',
                display_priority=0,
                country=CountryFactory.create(name='Israel')
            )
        )
        CompanyOfficeFactory.create(
            company=CompanyFactory.create(
                tld=[
                    TldFactory.create(value='ru')
                ],
                certificates=[
                    CompanyCertificateFactory.create(code='direct', is_regional=True)
                ],
                direct_budget=None,
                is_active=True
            ),
            city=CityFactory.create(
                name='Barcelona',
                display_priority=0,
                country=CountryFactory.create(name='Spain')
            )
        )
        CompanyOfficeFactory.create(
            company=CompanyFactory.create(
                tld=[
                    TldFactory.create(value='ru')
                ],
                certificates=[
                    CompanyCertificateFactory.create(code='direct', is_regional=True)
                ],
                direct_budget=None,
                is_active=True
            ),
            city=CityFactory.create(
                name='Samara',
                display_priority=0,
                country=CountryFactory.create(name='Russia')
            )
        )
        CompanyOfficeFactory.create(
            company=CompanyFactory.create(
                tld=[
                    TldFactory.create(value='com')  # <--
                ],
                certificates=[
                    CompanyCertificateFactory.create(code='dialogs')
                ],
                direct_budget=100000,
                is_active=True
            ),
            city=CityFactory.create(
                name='Dubai',
                name_prepositional='in Dubai',
                display_priority=0,
                country=CountryFactory.create(
                    name='United Arab Emirates',
                    name_prepositional='in United Arab Emirates'
                )
            )
        )

        query_kwargs = {
            'format': 'json',
            'tld': 'ru'
        }
        response = self.client.get(reverse('v1:companies-filters', query_kwargs=query_kwargs))

        expected = {
            'certificates': [
                {
                    'code': 'direct',
                    'name': 'Direct',
                    'preposition': 'by',
                    'namePrepositional': 'Direct',
                    'isRegional': True
                },
                {
                    'code': 'metrika',
                    'name': 'Metrika',
                    'preposition': 'by',
                    'namePrepositional': 'Metrika',
                    'isRegional': False
                }
            ],
            'cities': [
                {
                    'geoId': 131,
                    'name': 'Tel Aviv',
                    'preposition': 'in',
                    'namePrepositional': 'Tel Aviv',
                    'country': {
                        'geoId': 181,
                        'name': 'Israel',
                        'preposition': 'in',
                        'namePrepositional': 'Israel'
                    }
                },
                {
                    'geoId': 213,
                    'name': 'Moscow',
                    'preposition': 'in',
                    'namePrepositional': 'Moscow',
                    'country': {
                        'geoId': 225,
                        'name': 'Russia',
                        'preposition': 'in',
                        'namePrepositional': 'Russia'
                    }
                },
                {
                    'geoId': 2,
                    'name': 'Saint Petersburg',
                    'preposition': 'in',
                    'namePrepositional': 'Saint Petersburg',
                    'country': {
                        'geoId': 225,
                        'name': 'Russia',
                        'preposition': 'in',
                        'namePrepositional': 'Russia'
                    }
                },
                {
                    'geoId': 43,
                    'name': 'Kazan',
                    'preposition': 'in',
                    'namePrepositional': 'Kazan',
                    'country': {
                        'geoId': 225,
                        'name': 'Russia',
                        'preposition': 'in',
                        'namePrepositional': 'Russia'
                    }
                },
                {
                    'geoId': 51,
                    'name': 'Samara',
                    'preposition': 'in',
                    'namePrepositional': 'Samara',
                    'country': {
                        'geoId': 225,
                        'name': 'Russia',
                        'preposition': 'in',
                        'namePrepositional': 'Russia'
                    }
                },
                {
                    'geoId': 54,
                    'name': 'Yekaterinburg',
                    'preposition': 'in',
                    'namePrepositional': 'Yekaterinburg',
                    'country': {
                        'geoId': 225,
                        'name': 'Russia',
                        'preposition': 'in',
                        'namePrepositional': 'Russia'
                    }
                },
                {
                    'geoId': 10429,
                    'name': 'Barcelona',
                    'preposition': 'in',
                    'namePrepositional': 'Barcelona',
                    'country': {
                        'geoId': 204,
                        'name': 'Spain',
                        'preposition': 'in',
                        'namePrepositional': 'Spain'
                    }
                }
            ],
            'countries': [
                {
                    'geoId': 181,
                    'name': 'Israel',
                    'preposition': 'in',
                    'namePrepositional': 'Israel'
                },
                {
                    'geoId': 225,
                    'name': 'Russia',
                    'preposition': 'in',
                    'namePrepositional': 'Russia'
                },
                {
                    'geoId': 204,
                    'name': 'Spain',
                    'preposition': 'in',
                    'namePrepositional': 'Spain'
                }
            ]
        }

        self.assertEqual(response.status_code, 200)
        self.assertJSONEqual(response.content, expected)

    def test_get_filter_by_certificate(self):
        CompanyOfficeFactory.create(
            company=CompanyFactory.create(
                tld=[
                    TldFactory.create(value='ru')
                ],
                certificates=[
                    CompanyCertificateFactory.create(code='direct', is_regional=True)
                ],
                is_active=True,
                is_partner=False
            ),
            city=CityFactory.create(
                name='Yekaterinburg',
                display_priority=2,
                country=CountryFactory.create(name='Russia')
            )
        )
        CompanyOfficeFactory.create(
            company=CompanyFactory.create(
                tld=[
                    TldFactory.create(value='ru')
                ],
                certificates=[],  # <--
                direct_budget=15000,
                is_active=True,
                is_partner=False  # <--
            ),
            city=CityFactory.create(
                name='Moscow',
                display_priority=2,
                country=CountryFactory.create(name='Russia')
            )
        )
        CompanyOfficeFactory.create(
            company=CompanyFactory.create(
                tld=[
                    TldFactory.create(value='ru')
                ],
                certificates=[],  # <--
                is_active=True,
                is_partner=True  # <--
            ),
            city=CityFactory.create(
                name='Barcelona',
                display_priority=1,
                country=CountryFactory.create(name='Spain')
            )
        )

        query_kwargs = {
            'format': 'json',
            'tld': 'ru'
        }
        response = self.client.get(reverse('v1:companies-filters', query_kwargs=query_kwargs))

        expected = {
            'certificates': [
                {
                    'code': 'direct',
                    'name': 'Direct',
                    'preposition': 'by',
                    'namePrepositional': 'Direct',
                    'isRegional': True
                }
            ],
            'cities': [
                {
                    'geoId': 54,
                    'name': 'Yekaterinburg',
                    'preposition': 'in',
                    'namePrepositional': 'Yekaterinburg',
                    'country': {
                        'geoId': 225,
                        'name': 'Russia',
                        'preposition': 'in',
                        'namePrepositional': 'Russia'
                    }
                },
                {
                    'geoId': 10429,
                    'name': 'Barcelona',
                    'preposition': 'in',
                    'namePrepositional': 'Barcelona',
                    'country': {
                        'geoId': 204,
                        'name': 'Spain',
                        'preposition': 'in',
                        'namePrepositional': 'Spain'
                    }
                }
            ],
            'countries': [
                {
                    'geoId': 225,
                    'name': 'Russia',
                    'preposition': 'in',
                    'namePrepositional': 'Russia'
                },
                {
                    'geoId': 204,
                    'name': 'Spain',
                    'preposition': 'in',
                    'namePrepositional': 'Spain'
                }
            ]
        }

        self.assertEqual(response.status_code, 200)
        self.assertJSONEqual(response.content, expected)

    def test_get_filter_by_active(self):
        CompanyOfficeFactory.create(
            company=CompanyFactory.create(
                tld=[
                    TldFactory.create(value='ru')
                ],
                certificates=[
                    CompanyCertificateFactory.create(code='direct', is_regional=True)
                ],
                is_active=True,
                is_partner=False
            ),
            city=CityFactory.create(
                name='Yekaterinburg',
                country=CountryFactory.create(name='Russia')
            )
        )
        CompanyOfficeFactory.create(
            company=CompanyFactory.create(
                tld=[
                    TldFactory.create(value='ru')
                ],
                certificates=[
                    CompanyCertificateFactory.create(code='direct', is_regional=True)
                ],
                is_active=False,  # <--
                is_partner=False
            ),
            city=CityFactory.create(
                name='Moscow',
                country=CountryFactory.create(name='Russia')
            )
        )

        query_kwargs = {
            'format': 'json',
            'tld': 'ru'
        }
        response = self.client.get(reverse('v1:companies-filters', query_kwargs=query_kwargs))

        expected = {
            'certificates': [
                {
                    'code': 'direct',
                    'name': 'Direct',
                    'preposition': 'by',
                    'namePrepositional': 'Direct',
                    'isRegional': True
                }
            ],
            'cities': [
                {
                    'geoId': 54,
                    'name': 'Yekaterinburg',
                    'preposition': 'in',
                    'namePrepositional': 'Yekaterinburg',
                    'country': {
                        'geoId': 225,
                        'name': 'Russia',
                        'preposition': 'in',
                        'namePrepositional': 'Russia'
                    }
                }
            ],
            'countries': [
                {
                    'geoId': 225,
                    'name': 'Russia',
                    'preposition': 'in',
                    'namePrepositional': 'Russia'
                }
            ]
        }

        self.assertEqual(response.status_code, 200)
        self.assertJSONEqual(response.content, expected)

    def test_get_without_tld(self):
        query_kwargs = {
            'format': 'json'
        }
        response = self.client.get(reverse('v1:companies-filters', query_kwargs=query_kwargs))

        expected = ["['tld'] query parameters are missing!"]

        self.assertEqual(response.status_code, 400)
        self.assertJSONEqual(response.content, expected)
