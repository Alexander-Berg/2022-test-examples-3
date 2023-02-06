import json
from decimal import Decimal

from rest_framework.test import APITransactionTestCase, APIClient

from commerce.adv_backend.backend.libs import StartrekMessageBackend
from commerce.adv_backend.backend.models import Company, CompanyOffice
from commerce.adv_backend.backend.utils import reverse

from commerce.adv_backend.tests import factory

from unittest.mock import patch


class TestViewCompaniesCases(APITransactionTestCase):
    client = APIClient()
    maxDiff = None
    reset_sequences = True

    def test_get(self):
        factory.CompanyOfficeFactory.create(
            company=factory.CompanyFactory.create(
                tld=[
                    factory.TldFactory.create(value='ru'),
                    factory.TldFactory.create(value='com')
                ],
                certificates=[
                    factory.CompanyCertificateFactory.create(code='direct')
                ],
                description='Душевная компания',
                name='TEST Company Limited',
                representatives=[
                    factory.ExternalUserFactory.create(username='savichev')
                ],
                site='https://savichev.me',
                direct_budget=10000,
                direct_company_type='bidder',
                is_partner=False
            ),
            is_main=True,
            address='street1',
            email='some1@ya.ru',
            city=factory.CityFactory.create(
                name='Yekaterinburg',
                country=factory.CountryFactory.create(name='Russia'),
            ),
            map='osm',
            latitude='-35.701042',
            longitude='143.363605',
            map_zoom=20,
            phone=''
        )
        factory.CompanyOfficeFactory.create(
            company=factory.CompanyFactory.create(
                tld=[
                    factory.TldFactory.create(value='ru')
                ],
                certificates=[
                    factory.CompanyCertificateFactory.create(code='metrika')
                ],
                description='Не очень душевная компания',
                name='Злая компания',
                representatives=[
                    factory.ExternalUserFactory.create(username='angry')
                ],
                site='https://angry.ru',
                direct_budget=10000,
                is_partner=True
            ),
            is_main=True,
            address='street2',
            email='some2@ya.ru',
            city=factory.CityFactory.create(
                name='Yekaterinburg',
                country=factory.CountryFactory.create(name='Russia')
            ),
            map='osm',
            latitude='-35.701042',
            longitude='0.363605',
            map_zoom=15,
            phone=''
        )

        query_kwargs = {
            'format': 'json',
            'tld': 'ru'
        }
        response = self.client.get(reverse('v1:companies', query_kwargs=query_kwargs))

        expected = [
            {
                'certificates': [
                    {
                        'code': 'direct',
                        'name': 'Direct',
                        'preposition': 'by',
                        'namePrepositional': 'Direct',
                        'isRegional': False
                    }
                ],
                'description': 'Душевная компания',
                'logo': 'https://avatars.mds.yandex.net/get-adv/50995/2a00000168b8792298a697251c015e3d23e9/orig',
                'directBudget': 10000,
                'directCompanyType': 'bidder',
                'name': 'TEST Company Limited',
                'representatives': [{'username': 'savichev', 'isMain': False}],
                'site': 'https://savichev.me',
                'slug': 'test-company-limited',
                'tld': [
                    {
                        'id': 1,
                        'value': 'ru'
                    },
                    {
                        'id': 2,
                        'value': 'com'
                    }
                ],
                'materials': [],
                'types': [],
                'isActive': True,
                'isPartner': False,
                'offices': [
                    {
                        'id': 1,
                        'isMain': True,
                        'address': 'street1',
                        'email': 'some1@ya.ru',
                        'city': {
                            'geoId': 54,
                            'name': 'Yekaterinburg',
                            'preposition': 'in',
                            'namePrepositional': 'Yekaterinburg',
                            'country': {
                                'geoId': 225,
                                'name': 'Russia',
                                'preposition': 'in',
                                'namePrepositional': 'Russia'
                            },
                        },
                        'map': 'osm',
                        'latitude': '-35.701042',
                        'longitude': '143.363605',
                        'mapZoom': 20,
                        'phone': ''
                    }
                ]
            },
            {
                'certificates': [
                    {
                        'code': 'metrika',
                        'name': 'Metrika',
                        'preposition': 'by',
                        'namePrepositional': 'Metrika',
                        'isRegional': False
                    }
                ],
                'description': 'Не очень душевная компания',
                'logo': 'https://avatars.mds.yandex.net/get-adv/50995/2a00000168b8792298a697251c015e3d23e9/orig',
                'directBudget': 10000,
                'directCompanyType': None,
                'name': 'Злая компания',
                'representatives': [{'username': 'angry', 'isMain': False}],
                'site': 'https://angry.ru',
                'slug': 'zlaia-kompaniia',
                'tld': [
                    {
                        'id': 1,
                        'value': 'ru'
                    }
                ],
                'materials': [],
                'types': [],
                'isActive': True,
                'isPartner': True,
                'offices': [
                    {
                        'id': 2,
                        'isMain': True,
                        'address': 'street2',
                        'email': 'some2@ya.ru',
                        'city': {
                            'geoId': 54,
                            'name': 'Yekaterinburg',
                            'preposition': 'in',
                            'namePrepositional': 'Yekaterinburg',
                            'country': {
                                'geoId': 225,
                                'name': 'Russia',
                                'preposition': 'in',
                                'namePrepositional': 'Russia'
                            },
                        },
                        'map': 'osm',
                        'latitude': '-35.701042',
                        'longitude': '0.363605',
                        'mapZoom': 15,
                        'phone': ''
                    }
                ]
            }
        ]

        self.assertEqual(response.status_code, 200)
        self.assertCountEqual(response.json(), expected)

    def test_certificate_filter(self):
        factory.CompanyOfficeFactory.create(
            company=factory.CompanyFactory.create(
                tld=[
                    factory.TldFactory.create(value='ru'),
                ],
                certificates=[
                    factory.CompanyCertificateFactory.create(code='direct'),
                    factory.CompanyCertificateFactory.create(code='metrika')
                ],
                description='Душевная компания',
                name='TEST Company Limited',
                representatives=[
                    factory.ExternalUserFactory.create(username='savichev')
                ],
                site='https://savichev.me',
                direct_budget=10000,
                is_partner=False
            ),
            is_main=True,
            address='street1',
            email='some1@ya.ru',
            city=factory.CityFactory.create(
                name='Yekaterinburg',
                country=factory.CountryFactory.create(name='Russia')
            ),
            map='osm',
            latitude='-35.701042',
            longitude='143.363605',
            map_zoom=20,
            phone=''

        )
        factory.CompanyOfficeFactory.create(
            company=factory.CompanyFactory.create(
                tld=[
                    factory.TldFactory.create(value='ru')
                ],
                certificates=[
                    factory.CompanyCertificateFactory.create(code='metrika'),
                    factory.CompanyCertificateFactory.create(code='market')
                ],
                description='Не очень душевная компания',
                name='Злая компания',
                representatives=[
                    factory.ExternalUserFactory.create(username='angry')
                ],
                site='https://angry.ru',
                is_partner=False
            ),
            is_main=True,
            address='street2',
            email='some2@ya.ru',
            city=factory.CityFactory.create(
                name='Yekaterinburg',
                country=factory.CountryFactory.create(name='Russia')
            ),
            map='osm',
            latitude='-35.701042',
            longitude='0.363605',
            map_zoom=15,
            phone=''
        )

        query_kwargs = {
            'format': 'json',
            'tld': 'ru',
            'group': 'main'
        }

        response_direct = self.client.get(reverse('v1:companies', query_kwargs=query_kwargs) + '&certificate=direct')

        expected_direct = [
            {
                'certificates': [
                    {
                        'code': 'direct',
                        'name': 'Direct',
                        'preposition': 'by',
                        'namePrepositional': 'Direct',
                        'isRegional': False
                    },
                    {
                        'code': 'metrika',
                        'preposition': 'by',
                        'name': 'Metrika',
                        'namePrepositional': 'Metrika',
                        'isRegional': False
                    }
                ],
                'description': 'Душевная компания',
                'logo': 'https://avatars.mds.yandex.net/get-adv/50995/2a00000168b8792298a697251c015e3d23e9/orig',
                'directBudget': 10000,
                'directCompanyType': None,
                'name': 'TEST Company Limited',
                'representatives': [{'username': 'savichev', 'isMain': False}],
                'site': 'https://savichev.me',
                'slug': 'test-company-limited',
                'tld': [
                    {
                        'id': 1,
                        'value': 'ru'
                    }
                ],
                'materials': [],
                'types': [],
                'isActive': True,
                'isPartner': False,
                'offices': [
                    {
                        'id': 1,
                        'isMain': True,
                        'address': 'street1',
                        'email': 'some1@ya.ru',
                        'city': {
                            'geoId': 54,
                            'name': 'Yekaterinburg',
                            'preposition': 'in',
                            'namePrepositional': 'Yekaterinburg',
                            'country': {
                                'geoId': 225,
                                'name': 'Russia',
                                'preposition': 'in',
                                'namePrepositional': 'Russia'
                            },
                        },
                        'map': 'osm',
                        'latitude': '-35.701042',
                        'longitude': '143.363605',
                        'mapZoom': 20,
                        'phone': ''
                    }
                ]
            }
        ]

        self.assertEqual(response_direct.status_code, 200)
        self.assertJSONEqual(response_direct.content, expected_direct)

        response_metrika_market = self.client.get(
            reverse('v1:companies', query_kwargs=query_kwargs)
            + '&certificate=metrika'
            + '&certificate=market'
        )

        expected_metrika_market = [
            {
                'certificates': [
                    {
                        'code': 'metrika',
                        'name': 'Metrika',
                        'preposition': 'by',
                        'namePrepositional': 'Metrika',
                        'isRegional': False
                    },
                    {
                        'code': 'market',
                        'name': 'Market',
                        'preposition': 'by',
                        'namePrepositional': 'Market',
                        'isRegional': False
                    }
                ],
                'description': 'Не очень душевная компания',
                'logo': 'https://avatars.mds.yandex.net/get-adv/50995/2a00000168b8792298a697251c015e3d23e9/orig',
                'directBudget': None,
                'directCompanyType': None,
                'name': 'Злая компания',
                'representatives': [{'username': 'angry', 'isMain': False}],
                'site': 'https://angry.ru',
                'slug': 'zlaia-kompaniia',
                'tld': [
                    {
                        'id': 1,
                        'value': 'ru'
                    }
                ],
                'materials': [],
                'types': [],
                'isActive': True,
                'isPartner': False,
                'offices': [
                    {
                        'id': 2,
                        'isMain': True,
                        'address': 'street2',
                        'email': 'some2@ya.ru',
                        'city': {
                            'geoId': 54,
                            'name': 'Yekaterinburg',
                            'preposition': 'in',
                            'namePrepositional': 'Yekaterinburg',
                            'country': {
                                'geoId': 225,
                                'name': 'Russia',
                                'preposition': 'in',
                                'namePrepositional': 'Russia'
                            },
                        },
                        'map': 'osm',
                        'latitude': '-35.701042',
                        'longitude': '0.363605',
                        'mapZoom': 15,
                        'phone': ''
                    }
                ]
            }
        ]

        self.assertEqual(response_metrika_market.status_code, 200)
        self.assertJSONEqual(response_metrika_market.content, expected_metrika_market)

        response_direct_metrika = self.client.get(
            reverse('v1:companies', query_kwargs=query_kwargs)
            + '&certificate=direct'
            + '&certificate=metrika'
        )

        expected_direct_metrika = expected_direct

        self.assertEqual(response_direct.status_code, 200)
        self.assertCountEqual(response_direct_metrika.json(), expected_direct_metrika)

    def test_get_order_offices_by_city(self):
        factory.CompanyFactory.create(
            name='Company many offices',
            tld=[
                factory.TldFactory.create(value='ru')
            ],
            certificates=[
                factory.CompanyCertificateFactory.create(code='direct', is_regional=True)
            ]
        )

        company_with_offices = Company.objects.get(slug='company-many-offices')

        factory.CompanyOfficeFactory.create(
            company=company_with_offices,
            is_main=True,
            city=factory.CityFactory.create(name='Moscow', display_priority=2),
        )

        factory.CompanyOfficeFactory.create(
            company=company_with_offices,
            is_main=False,
            city=factory.CityFactory.create(name='Irkutsk', display_priority=0),
        )

        factory.CompanyOfficeFactory.create(
            company=company_with_offices,
            is_main=True,
            city=factory.CityFactory.create(name='Surgut', display_priority=0),
        )

        factory.CompanyOfficeFactory.create(
            company=company_with_offices,
            is_main=False,
            city=factory.CityFactory.create(name='Moscow', display_priority=2),
        )

        factory.CompanyOfficeFactory.create(
            company=company_with_offices,
            is_main=True,
            city=factory.CityFactory.create(name='Yekaterinburg', display_priority=0),
        )

        factory.CompanyOfficeFactory.create(
            company=company_with_offices,
            is_main=False,
            city=factory.CityFactory.create(name='Saint Petersburg', display_priority=1),
        )

        factory.CompanyOfficeFactory.create(
            company=company_with_offices,
            is_main=True,
            city=factory.CityFactory.create(name='Saint Petersburg', display_priority=1),
        )

        factory.CompanyOfficeFactory.create(
            company=company_with_offices,
            is_main=False,
            city=factory.CityFactory.create(name='Voronezh', display_priority=0),
        )

        factory.CompanyOfficeFactory.create(
            company=company_with_offices,
            is_main=True,
            city=factory.CityFactory.create(name='Tula', display_priority=0),
        )

        factory.CompanyOfficeFactory.create(
            company=company_with_offices,
            is_main=False,
            city=factory.CityFactory.create(name='Astrahan', display_priority=0),
        )

        query_kwargs = {
            'format': 'json',
            'tld': 'ru',
            'slug': 'company-many-offices'
        }
        response = self.client.get(reverse('v1:companies', query_kwargs=query_kwargs))

        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(response.json()), 1)

        actual_offices = []

        for office in response.json()[0]['offices']:
            actual_offices.append({
                'isMain': office['isMain'],
                'cityName': office['city']['name']
            })

        expected_offices = [
            {
                'isMain': True,
                'cityName': 'Moscow'
            },
            {
                'isMain': True,
                'cityName': 'Saint Petersburg'
            },
            {
                'isMain': True,
                'cityName': 'Surgut'
            },
            {
                'isMain': True,
                'cityName': 'Tula'
            },
            {
                'isMain': True,
                'cityName': 'Yekaterinburg'
            },
            {
                'isMain': False,
                'cityName': 'Moscow'
            },
            {
                'isMain': False,
                'cityName': 'Saint Petersburg'
            },
            {
                'isMain': False,
                'cityName': 'Astrahan'
            },
            {
                'isMain': False,
                'cityName': 'Irkutsk'
            },
            {
                'isMain': False,
                'cityName': 'Voronezh'
            }
        ]

        self.assertEqual(response.status_code, 200)
        self.assertEqual(actual_offices, expected_offices)

    def test_get_without_tld(self):
        query_kwargs = {
            'format': 'json'
        }
        response = self.client.get(reverse('v1:companies', query_kwargs=query_kwargs))

        expected = ["['tld'] query parameters are missing!"]

        self.assertEqual(response.status_code, 400)
        self.assertJSONEqual(response.content, expected)

    def test_post(self):
        factory.ExternalUserFactory.create(username='kvas')
        factory.TldFactory.create(value='com')
        city = factory.CityFactory.create(
            name='Yekaterinburg',
            country=factory.CountryFactory.create(name='Russia')
        )
        factory.CompanyCertificateFactory.create(code='dialogs')

        query_kwargs = {
            'format': 'json',
            'tld': 'com'
        }

        with patch.object(StartrekMessageBackend, 'send', return_value=None):
            response = self.client.post(
                reverse('v1:companies', query_kwargs=query_kwargs),
                data=json.dumps({
                    'certificates': ['dialogs'],
                    'logo': 'https://avatars.mds.yandex.net/get-adv/50995/2a00000168b8792298a697251c015e3d23e9/orig',
                    'materials': [],
                    'name': 'TEST Company Limited',
                    'offices': [
                        {
                            'address': 'ул. Хохрякова 10, 11 этаж',
                            'city': city.geo_id,
                            'is_main': True,
                            'latitude': 123.123,
                            'longitude': 321.321,
                            'map': 'yandex',
                            'map_zoom': 10
                        }
                    ],
                    'proposed_by': 'savichev',
                    'representatives': ['kvas'],
                    'site': 'https://savichev.me',
                    'tld': ['com'],
                    'types': []
                }),
                content_type='application/json'
            )

        expected = {
            'certificates': ['dialogs'],
            'description': '',
            'directBudget': None,
            'logo': 'https://avatars.mds.yandex.net/get-adv/50995/2a00000168b8792298a697251c015e3d23e9/orig',
            'materials': [],
            'name': 'TEST Company Limited',
            'offices': [
                {
                    'id': 1,
                    'address': 'ул. Хохрякова 10, 11 этаж',
                    'city': city.geo_id,
                    'email': None,
                    'isMain': True,
                    'latitude': '123.123000',
                    'longitude': '321.321000',
                    'map': 'yandex',
                    'mapZoom': 10,
                    'phone': ''
                }
            ],
            'proposedBy': 'savichev',
            'representatives': ['kvas'],
            'site': 'https://savichev.me',
            'slug': 'test-company-limited',
            'tld': ['com'],
            'types': []
        }

        self.assertEqual(response.status_code, 201)
        self.assertJSONEqual(response.content, expected)

        expected_object = {
            'description': '',
            'description_en': None,
            'description_ru': None,
            'description_tr': None,
            'description_zh_hans': None,
            'direct_budget': None,
            'direct_company_type': None,
            'is_approved': None,
            'id': 1,
            'logo': 'https://avatars.mds.yandex.net/get-adv/50995/2a00000168b8792298a697251c015e3d23e9/orig',
            'materials': [],
            'materials_en': [],
            'materials_ru': [],
            'materials_tr': [],
            'materials_zh_hans': [],
            'name': 'TEST Company Limited',
            'name_en': 'TEST Company Limited',
            'name_ru': None,
            'name_tr': None,
            'name_zh_hans': None,
            'proposed_by': 'savichev',
            'region_id': None,
            'site': 'https://savichev.me',
            'slug': 'test-company-limited',
            'is_active': False,
            'is_partner': False
        }

        self.assertEqual(len(Company.unmoderated_objects.all()), 1)

        created_company = Company.unmoderated_objects.get(id=1)
        created_company_raw = Company.unmoderated_objects.raw_values().get(id=1)

        self.assertEqual(created_company_raw, expected_object)

        # Проверяем relations
        expected_object_office = {
            'address': 'ул. Хохрякова 10, 11 этаж',
            'address_en': 'ул. Хохрякова 10, 11 этаж',
            'address_ru': None,
            'address_tr': None,
            'address_zh_hans': None,
            'city_id': 1,
            'company_id': 1,
            'email': None,
            'id': 1,
            'is_approved': None,
            'is_main': True,
            'latitude': Decimal('123.123000'),
            'longitude': Decimal('321.321000'),
            'map': 'yandex',
            'map_zoom': 10,
            'phone': '',
            'proposed_by': 'savichev'
        }

        self.assertEqual(len(CompanyOffice.unmoderated_objects.all()), 1)

        created_office_raw = CompanyOffice.unmoderated_objects.raw_values().get(id=1)

        self.assertEqual(created_office_raw, expected_object_office)

        self.assertEqual(list(created_company.certificates.values()), [
            {
                'id': 1,
                'code': 'dialogs',
                'name': 'Dialogs',
                'preposition': 'by',
                'name_prepositional': 'Dialogs',
                'is_regional': False
            }
        ])
        self.assertEqual(list(created_company.tld.values()), [
            {
                'id': 1,
                'value': 'com'
            }
        ])
        self.assertEqual(list(created_company.types.values()), [])

        # FIXME: основная выдача сломана, в неё попадают компании с is_approved=Null,
        # а default=True для is_approved ломает все тесты и админку
        # self.assertEqual(len(Company.objects.all()), 0)

    def test_post_without_tld(self):
        query_kwargs = {
            'format': 'json'
        }
        response = self.client.post(reverse('v1:companies', query_kwargs=query_kwargs))

        expected = ["['tld'] query parameters are missing!"]

        self.assertEqual(response.status_code, 400)
        self.assertJSONEqual(response.content, expected)

        self.assertEqual(len(Company.unmoderated_objects.all()), 0)
        self.assertEqual(len(Company.objects.all()), 0)

    def test_get_by_slugs(self):
        factory.CompanyOfficeFactory.create(
            company=factory.CompanyFactory.create(
                name='company-1',
                tld=[
                    factory.TldFactory.create(value='ru')
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
                name='company-2',
                tld=[
                    factory.TldFactory.create(value='ru')
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
            ),
            is_main=True
        )

        query_kwargs = {
            'format': 'json',
            'tld': 'ru',
            'slug': 'company-1'
        }

        response = self.client.get(reverse('v1:companies', query_kwargs=query_kwargs) + '&slug=company-2')

        actual_slugs = []

        for company in response.json():
            actual_slugs.append(company['slug'])

        expected_slugs = ['company-1', 'company-2']

        self.assertEqual(response.status_code, 200)
        self.assertEqual(sorted(actual_slugs), expected_slugs)

    def test_get_only_active(self):
        factory.CompanyOfficeFactory.create(
            company=factory.CompanyFactory.create(
                name='company-1',
                tld=[
                    factory.TldFactory.create(value='ru')
                ],
                is_active=True
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
                is_active=False
            ),
            city=factory.CityFactory.create(
                name='Yekaterinburg',
                country=factory.CountryFactory.create(name='Russia')
            )
        )

        query_kwargs = {
            'format': 'json',
            'tld': 'ru'
        }
        response = self.client.get(reverse('v1:companies', query_kwargs=query_kwargs))

        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(response.json()), 1)
        self.assertEqual(response.json()[0]['slug'], 'company-1')
