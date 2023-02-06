from rest_framework.test import APITransactionTestCase, APIClient

from commerce.adv_backend.backend.models import Company
from commerce.adv_backend.backend.utils import reverse

from commerce.adv_backend.tests import factory


class TestViewCompaniesSlugsListCases(APITransactionTestCase):
    client = APIClient()
    maxDiff = None
    reset_sequences = True

    def test_get(self):
        for i in range(2):
            factory.CompanyOfficeFactory.create(
                company=factory.CompanyFactory.create(
                    name=f'company-{i + 1}',
                    tld=[
                        factory.TldFactory.create(value='ru')
                    ],
                    certificates=[
                        factory.CompanyCertificateFactory.create(code='direct')
                    ]
                ),
                is_main=True
            )

        factory.CompanyOfficeFactory.create(
            company=factory.CompanyFactory.create(
                name='other-company',
                tld=[
                    factory.TldFactory.create(value='com')
                ],
                certificates=[
                    factory.CompanyCertificateFactory.create(code='direct')
                ]
            ),
            is_main=True
        )

        query_kwargs = {
            'format': 'json',
            'tld': 'ru'
        }
        response = self.client.get(reverse('v1:companies-slugs', query_kwargs=query_kwargs))

        expected = [
            'company-1',
            'company-2'
        ]

        self.assertEqual(response.status_code, 200)
        self.assertCountEqual(response.json(), expected)

    def test_get_with_empty_country_city_certificate(self):
        for i in range(2):
            factory.CompanyOfficeFactory.create(
                company=factory.CompanyFactory.create(
                    name=f'company-{i + 1}',
                    tld=[
                        factory.TldFactory.create(value='ru')
                    ],
                    certificates=[
                        factory.CompanyCertificateFactory.create(code='direct')
                    ]
                ),
                is_main=True
            )

        factory.CompanyOfficeFactory.create(
            company=factory.CompanyFactory.create(
                name='other-company',
                tld=[
                    factory.TldFactory.create(value='com')
                ],
                certificates=[
                    factory.CompanyCertificateFactory.create(code='direct')
                ]
            ),
            is_main=True
        )

        query_kwargs = {
            'format': 'json',
            'tld': 'ru',
            'country': '',
            'city': '',
            'certificate': ''
        }
        response = self.client.get(reverse('v1:companies-slugs', query_kwargs=query_kwargs))

        expected = [
            'company-1',
            'company-2'
        ]

        self.assertEqual(response.status_code, 200)
        self.assertCountEqual(response.json(), expected)

    def test_filter_by_budget(self):
        for i in range(5):
            factory.CompanyOfficeFactory.create(
                company=factory.CompanyFactory.create(
                    name=f'company with budget {i + 2}',
                    direct_budget=i + 2,
                    tld=[
                        factory.TldFactory.create(value='ru')
                    ],
                    certificates=[
                        factory.CompanyCertificateFactory.create(code='direct')
                    ]
                ),
                is_main=True
            )

        factory.CompanyOfficeFactory.create(
            company=factory.CompanyFactory.create(
                name='company com',
                tld=[
                    factory.TldFactory.create(value='com')
                ],
                certificates=[
                    factory.CompanyCertificateFactory.create(code='direct')
                ]
            ),
            is_main=True
        )

        self._filter_by_budget_and_compare(
            expected=[
                'company-with-budget-2',
                'company-with-budget-3',
                'company-with-budget-4',
                'company-with-budget-5',
                'company-with-budget-6'
            ],
            budget=8
        )
        self._filter_by_budget_and_compare(
            expected=[
                'company-with-budget-2',
                'company-with-budget-3',
            ],
            budget=3
        )
        self._filter_by_budget_and_compare(
            expected=[],
            budget=1
        )

    def _filter_by_budget_and_compare(self, expected, budget):
        query_kwargs = {
            'format': 'json',
            'budget': budget,
            'tld': 'ru'
        }
        response = self.client.get(reverse('v1:companies-slugs', query_kwargs=query_kwargs))

        self.assertEqual(response.status_code, 200)
        self.assertCountEqual(response.json(), expected)

    def test_get_without_tld(self):
        query_kwargs = {
            'format': 'json'
        }
        response = self.client.get(reverse('v1:companies-slugs', query_kwargs=query_kwargs))

        expected = ["['tld'] query parameters are missing!"]

        self.assertEqual(response.status_code, 400)
        self.assertJSONEqual(response.content, expected)

    def test_get_with_invalid_city(self):
        query_kwargs = {
            'format': 'json',
            'tld': 'ru',
            'city': 'invalid'
        }
        response = self.client.get(reverse('v1:companies-slugs', query_kwargs=query_kwargs))

        expected = {
            'city': {
                '0': [
                    'Требуется целочисленное значение.'
                ]
            }
        }

        self.assertEqual(response.status_code, 400)
        self.assertJSONEqual(response.content, expected)

    def test_get_with_invalid_country(self):
        query_kwargs = {
            'format': 'json',
            'tld': 'ru',
            'country': 'invalid'
        }
        response = self.client.get(reverse('v1:companies-slugs', query_kwargs=query_kwargs))

        expected = {
            'country': {
                '0': [
                    'Требуется целочисленное значение.'
                ]
            }
        }

        self.assertEqual(response.status_code, 400)
        self.assertJSONEqual(response.content, expected)

    def test_with_filter_group_and_regional_cert(self):
        factory.CompanyOfficeFactory.create(
            company=factory.CompanyFactory.create(
                name='company-main',
                tld=[
                    factory.TldFactory.create(value='ru')
                ],
                certificates=[
                    factory.CompanyCertificateFactory.create(code='direct', is_regional=True)
                ]
            ),
            city=factory.CityFactory.create(
                name='Yekaterinburg',
                country=factory.CountryFactory.create(name='Russia')
            ),
            is_main=True
        )

        factory.CompanyFactory.create(
            name='Company Other',
            tld=[
                factory.TldFactory.create(value='ru')
            ],
            certificates=[
                factory.CompanyCertificateFactory.create(code='direct', is_regional=True)
            ]
        )

        company_other = Company.objects.get(slug='company-other')

        factory.CompanyOfficeFactory.create(
            company=company_other,
            city=factory.CityFactory.create(
                name='Moscow',
                country=factory.CountryFactory.create(name='Russia')
            ),
            is_main=True
        )
        factory.CompanyOfficeFactory.create(
            company=company_other,
            city=factory.CityFactory.create(
                name='Yekaterinburg',
                country=factory.CountryFactory.create(name='Russia')
            ),
            is_main=False
        )

        factory.CompanyOfficeFactory.create(
            company=factory.CompanyFactory.create(
                name='company-not-in-result',
                tld=[
                    factory.TldFactory.create(value='ru')
                ],
                certificates=[
                    factory.CompanyCertificateFactory.create(code='direct', is_regional=True)
                ]
            ),
            city=factory.CityFactory.create(
                name='Samara',
                country=factory.CountryFactory.create(name='Russia')
            ),
            is_main=True
        )

        query_group_main_kwargs = {
            'format': 'json',
            'tld': 'ru',
            'group': 'main',
            'city': 54,  # Yekaterinburg
            'certificate': 'direct'
        }
        main_response = self.client.get(reverse('v1:companies-slugs', query_kwargs=query_group_main_kwargs))
        main_expected = ['company-main']

        self.assertEqual(main_response.status_code, 200)
        self.assertCountEqual(main_response.json(), main_expected)

        query_group_other_kwargs = {
            'format': 'json',
            'tld': 'ru',
            'group': 'offices',
            'city': 54,  # Yekaterinburg
            'certificate': 'direct'
        }
        other_response = self.client.get(reverse('v1:companies-slugs', query_kwargs=query_group_other_kwargs))
        other_expected = ['company-other']

        self.assertEqual(other_response.status_code, 200)
        self.assertCountEqual(other_response.json(), other_expected)

        query_group_unexpected_kwargs = {
            'format': 'json',
            'tld': 'ru',
            'group': 'unexpected',
            'city': 54,  # Yekaterinburg
            'certificate': 'direct'
        }
        unexpected_group_response = self.client.get(reverse('v1:companies-slugs', query_kwargs=query_group_unexpected_kwargs))
        unexpected_group_expected = ['company-main', 'company-other']

        self.assertEqual(unexpected_group_response.status_code, 200)
        self.assertCountEqual(unexpected_group_response.json(), unexpected_group_expected)

    def test_with_filter_other_group_without_regional_cert(self):
        factory.CompanyFactory.create(
            name='Company Other',
            tld=[
                factory.TldFactory.create(value='ru')
            ],
            certificates=[
                factory.CompanyCertificateFactory.create(code='metrika')
            ]
        )

        company_other = Company.objects.get(slug='company-other')

        factory.CompanyOfficeFactory.create(
            company=company_other,
            city=factory.CityFactory.create(
                name='Moscow',
                country=factory.CountryFactory.create(name='Russia')
            ),
            is_main=True
        )
        factory.CompanyOfficeFactory.create(
            company=company_other,
            city=factory.CityFactory.create(
                name='Yekaterinburg',
                country=factory.CountryFactory.create(name='Russia')
            ),
            is_main=False
        )

        query_kwargs = {
            'format': 'json',
            'tld': 'ru',
            'group': 'offices',
            'city': 54,  # Yekaterinburg
            'certificate': 'metrika'
        }
        response = self.client.get(reverse('v1:companies-slugs', query_kwargs=query_kwargs))
        expected = []

        self.assertEqual(response.status_code, 200)
        self.assertCountEqual(response.json(), expected)

    def test_with_filter_main_group_without_regional_cert(self):
        factory.CompanyOfficeFactory.create(
            company=factory.CompanyFactory.create(
                name='company-main-1',
                tld=[
                    factory.TldFactory.create(value='ru')
                ],
                certificates=[
                    factory.CompanyCertificateFactory.create(code='cpm')
                ]
            ),
            city=factory.CityFactory.create(
                name='Yekaterinburg',
                country=factory.CountryFactory.create(name='Russia')
            ),
            is_main=True
        )

        factory.CompanyOfficeFactory.create(
            company=factory.CompanyFactory.create(
                name='company-main-2',
                tld=[
                    factory.TldFactory.create(value='ru')
                ],
                certificates=[
                    factory.CompanyCertificateFactory.create(code='cpm')
                ]
            ),
            city=factory.CityFactory.create(
                name='Yekaterinburg',
                country=factory.CountryFactory.create(name='Russia')
            ),
            is_main=True
        )

        query_kwargs = {
            'format': 'json',
            'tld': 'ru',
            'group': 'main',
            'city': 54,
            'certificate': 'cpm'
        }
        response = self.client.get(reverse('v1:companies-slugs', query_kwargs=query_kwargs))
        expected = [
            'company-main-1',
            'company-main-2'
        ]

        self.assertEqual(response.status_code, 200)
        self.assertCountEqual(response.json(), expected)

    def test_with_filter_partners_group(self):
        factory.CompanyOfficeFactory.create(
            company=factory.CompanyFactory.create(
                name='company-1',
                tld=[
                    factory.TldFactory.create(value='ru')
                ],
                certificates=[
                    factory.CompanyCertificateFactory.create(code='cpm')
                ],
                is_partner=True
            ),
            city=factory.CityFactory.create(
                name='Yekaterinburg',
                country=factory.CountryFactory.create(name='Russia')
            )
        )

        factory.CompanyOfficeFactory.create(
            company=factory.CompanyFactory.create(
                name='company-2',
                tld=[
                    factory.TldFactory.create(value='ru')
                ],
                certificates=[
                    factory.CompanyCertificateFactory.create(code='cpm')
                ],
                is_partner=False
            ),
            city=factory.CityFactory.create(
                name='Yekaterinburg',
                country=factory.CountryFactory.create(name='Russia')
            )
        )

        factory.CompanyOfficeFactory.create(
            company=factory.CompanyFactory.create(
                name='company-3',
                tld=[
                    factory.TldFactory.create(value='ru')
                ],
                certificates=[
                    factory.CompanyCertificateFactory.create(code='cpm')
                ],
                is_partner=True
            ),
            city=factory.CityFactory.create(
                name='Samara',
                country=factory.CountryFactory.create(name='Russia')
            )
        )

        factory.CompanyOfficeFactory.create(
            company=factory.CompanyFactory.create(
                name='company-4',
                tld=[
                    factory.TldFactory.create(value='ru')
                ],
                certificates=[
                    factory.CompanyCertificateFactory.create(code='cpm')
                ],
                is_partner=True
            ),
            city=factory.CityFactory.create(
                name='Barcelona',
                country=factory.CountryFactory.create(name='Spain')
            )
        )

        query_kwargs = {
            'format': 'json',
            'tld': 'ru',
            'group': 'partners',
            'city': 54,  # Yekaterinburg
            'certificate': 'cpm'
        }
        response = self.client.get(reverse('v1:companies-slugs', query_kwargs=query_kwargs))
        expected = ['company-1']

        self.assertEqual(response.status_code, 200)
        self.assertCountEqual(response.json(), expected)

    def test_with_filter_by_country_without_city(self):
        factory.CompanyOfficeFactory.create(
            company=factory.CompanyFactory.create(
                name='company-1',
                tld=[
                    factory.TldFactory.create(value='ru')
                ],
                certificates=[
                    factory.CompanyCertificateFactory.create(code='cpm')
                ]
            ),
            city=factory.CityFactory.create(
                name='Yekaterinburg',
                country=factory.CountryFactory.create(name='Russia')
            )
        )

        factory.CompanyOfficeFactory.create(
            company=factory.CompanyFactory.create(
                name='company-2',
                tld=[
                    factory.TldFactory.create(value='ru')
                ],
                certificates=[
                    factory.CompanyCertificateFactory.create(code='cpm')
                ]
            ),
            city=factory.CityFactory.create(
                name='Barcelona',
                country=factory.CountryFactory.create(name='Spain')
            )
        )

        factory.CompanyOfficeFactory.create(
            company=factory.CompanyFactory.create(
                name='company-3',
                tld=[
                    factory.TldFactory.create(value='ru')
                ],
                certificates=[
                    factory.CompanyCertificateFactory.create(code='cpm')
                ]
            ),
            city=factory.CityFactory.create(
                name='Samara',
                country=factory.CountryFactory.create(name='Russia')
            )
        )

        query_kwargs = {
            'format': 'json',
            'tld': 'ru',
            'country': 225  # Russia
        }
        response = self.client.get(reverse('v1:companies-slugs', query_kwargs=query_kwargs))
        expected = ['company-1', 'company-3']

        self.assertEqual(response.status_code, 200)
        self.assertCountEqual(response.json(), expected)

    def test_with_filter_by_country_no_duplicate_companies(self):
        factory.CompanyFactory.create(
            name='test-company',
            tld=[
                factory.TldFactory.create(value='ru')
            ],
            certificates=[
                factory.CompanyCertificateFactory.create(code='cpm')
            ]
        )

        test_company = Company.objects.get(slug='test-company')

        factory.CompanyOfficeFactory.create(
            company=test_company,
            city=factory.CityFactory.create(
                name='Yekaterinburg',
                country=factory.CountryFactory.create(name='Russia')
            )
        )

        factory.CompanyOfficeFactory.create(
            company=test_company,
            city=factory.CityFactory.create(
                name='Moscow',
                country=factory.CountryFactory.create(name='Russia')
            )
        )

        query_kwargs = {
            'format': 'json',
            'tld': 'ru',
            'country': 225  # Russia
        }
        response = self.client.get(reverse('v1:companies-slugs', query_kwargs=query_kwargs))
        expected = ['test-company']

        self.assertEqual(response.status_code, 200)
        self.assertCountEqual(response.json(), expected)

    def test_order_by_budget(self):
        factory.CompanyFactory.create(
            name='company one',
            tld=[
                factory.TldFactory.create(value='ru')
            ],
            direct_budget=1000
        )
        factory.CompanyFactory.create(
            name='company two',
            tld=[
                factory.TldFactory.create(value='ru')
            ],
            direct_budget=2000
        )
        factory.CompanyFactory.create(
            name='company three',
            tld=[
                factory.TldFactory.create(value='ru')
            ],
            direct_budget=None
        )
        factory.CompanyFactory.create(
            name='company four',
            tld=[
                factory.TldFactory.create(value='ru')
            ],
            direct_budget=4000
        )

        asc_query_kwargs = {
            'format': 'json',
            'tld': 'ru',
            'budget_order': 'asc'
        }
        asc_response = self.client.get(reverse('v1:companies-slugs', query_kwargs=asc_query_kwargs))
        asc_expected = ['company-one', 'company-two', 'company-four', 'company-three']

        self.assertEqual(asc_response.status_code, 200)
        self.assertCountEqual(asc_response.json(), asc_expected)

        desc_query_kwargs = {
            'format': 'json',
            'tld': 'ru',
            'budget_order': 'desc'
        }
        desc_response = self.client.get(reverse('v1:companies-slugs', query_kwargs=desc_query_kwargs))
        desc_expected = ['company-four', 'company-two', 'company-one', 'company-three']

        self.assertEqual(desc_response.status_code, 200)
        self.assertCountEqual(desc_response.json(), desc_expected)
