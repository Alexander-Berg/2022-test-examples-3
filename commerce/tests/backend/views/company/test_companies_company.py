import json

from rest_framework.test import APITransactionTestCase, APIClient

from commerce.adv_backend.backend.models import Company
from commerce.adv_backend.backend.utils import reverse

from commerce.adv_backend.tests import factory


class TestViewCompanySetCases(APITransactionTestCase):
    client = APIClient()
    maxDiff = None
    reset_sequences = True

    def test_get(self):
        factory.CompanyFactory.create(
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
            materials=[
                'https://yandex-ad.cn/adv/solutions/cases/kak-stat-uspeshnim-za-21-den',
                'https://yandex.ru/adv/news/igor-zapustil-raketi-na-mars'
            ],
            direct_budget=10000,
            direct_company_type='bidder',
            is_partner=True
        )

        company_limited = Company.objects.get(slug='test-company-limited')

        factory.CompanyOfficeFactory.create(
            company=company_limited,
            is_main=True,
            address='one',
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
            company=company_limited,
            is_main=False,
            address='two',
            email='some2@ya.ru',
            city=factory.CityFactory.create(
                name='Samara',
                country=factory.CountryFactory.create(name='Russia')
            ),
            map='osm',
            latitude='123',
            longitude='456',
            map_zoom=10,
            phone=''
        )

        factory.CompanyOfficeFactory.create(
            company=factory.CompanyFactory.create(
                tld=[
                    factory.TldFactory.create(value='com')
                ],
                name='Some other company'
            ),
            is_main=True
        )

        query_kwargs = {
            'format': 'json',
            'tld': 'ru'
        }
        response = self.client.get(
            reverse('v1:companies-company', kwargs={'slug': 'test-company-limited'}, query_kwargs=query_kwargs)
        )

        expected = {
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
            'materials': [
                'https://yandex-ad.cn/adv/solutions/cases/kak-stat-uspeshnim-za-21-den',
                'https://yandex.ru/adv/news/igor-zapustil-raketi-na-mars'
            ],
            'types': [],
            'isActive': True,
            'isPartner': True,
            'offices': [
                {
                    'id': 1,
                    'isMain': True,
                    'address': 'one',
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
                },
                {
                    'id': 2,
                    'isMain': False,
                    'address': 'two',
                    'email': 'some2@ya.ru',
                    'city': {
                        'geoId': 51,
                        'name': 'Samara',
                        'preposition': 'in',
                        'namePrepositional': 'Samara',
                        'country': {
                            'geoId': 225,
                            'name': 'Russia',
                            'preposition': 'in',
                            'namePrepositional': 'Russia'
                        },
                    },
                    'map': 'osm',
                    'latitude': '123.000000',
                    'longitude': '456.000000',
                    'mapZoom': 10,
                    'phone': ''
                }
            ]
        }

        self.assertEqual(response.status_code, 200)
        self.assertJSONEqual(response.content, expected)

    def test_get_without_tld(self):
        query_kwargs = {
            'format': 'json'
        }
        response = self.client.get(
            reverse('v1:companies-company', kwargs={'slug': 'some-slug'}, query_kwargs=query_kwargs)
        )

        expected = ["['tld'] query parameters are missing!"]

        self.assertEqual(response.status_code, 400)
        self.assertJSONEqual(response.content, expected)

    def test_get_not_found(self):
        factory.CompanyOfficeFactory.create(
            company=factory.CompanyFactory.create(
                tld=[
                    factory.TldFactory.create(value='com')
                ],
                name='Some company'
            ),
            is_main=True
        )

        query_kwargs = {
            'format': 'json',
            'tld': 'ru'
        }
        response = self.client.get(
            reverse('v1:companies-company', kwargs={'slug': 'not-found'}, query_kwargs=query_kwargs)
        )

        expected = {
            'detail': 'Не найдено.'
        }

        self.assertEqual(response.status_code, 404)
        self.assertJSONEqual(response.content, expected)

    def test_patch_without_yandex_login(self):
        factory.CompanyOfficeFactory.create(
            company=factory.CompanyFactory.create(
                tld=[
                    factory.TldFactory.create(value='ru'),
                    factory.TldFactory.create(value='com')
                ],
                certificates=[
                    factory.CompanyCertificateFactory.create(code='direct')
                ],
                region=None,
                description='Хитрая компания',
                name='TEST Company Zero Limited',
                representatives=[
                    factory.ExternalUserFactory.create(username='watermelon')
                ],
                site='https://example.com'
            ),
            is_main=True
        )

        query_kwargs = {
            'format': 'json',
            'tld': 'ru'
        }
        response = self.client.patch(
            reverse('v1:companies-company', kwargs={'slug': 'test-company-zero-limited'}, query_kwargs=query_kwargs),
            data=json.dumps({
                'proposedBy': 'anonymous',
                'directBudget': 15000
            }),
            content_type='application/json',
            HTTP_X_YA_EXTERNAL_LOGIN=''
        )

        self.assertEqual(response.status_code, 403)

    def test_patch_by_not_representative(self):
        factory.CompanyOfficeFactory.create(
            company=factory.CompanyFactory.create(
                tld=[
                    factory.TldFactory.create(value='ru'),
                    factory.TldFactory.create(value='com')
                ],
                certificates=[
                    factory.CompanyCertificateFactory.create(code='direct')
                ],
                region=None,
                description='Очередная компания',
                name='TEST Company Usual Limited',
                representatives=[
                    factory.ExternalUserFactory.create(username='savichev')
                ],
                site='https://example.com'
            ),
            is_main=True
        )

        query_kwargs = {
            'format': 'json',
            'tld': 'ru'
        }
        response = self.client.patch(
            reverse('v1:companies-company', kwargs={'slug': 'test-company-usual-limited'}, query_kwargs=query_kwargs),
            data=json.dumps({
                'proposedBy': 'angry',
                'directBudget': 0
            }),
            content_type='application/json',
            HTTP_X_YA_EXTERNAL_LOGIN='angry'
        )

        self.assertEqual(response.status_code, 403)

    # TODO: Потестировать локали
    def test_patch(self):
        factory.CompanyOfficeFactory.create(
            company=factory.CompanyFactory.create(
                tld=[
                    factory.TldFactory.create(value='ru'),
                    factory.TldFactory.create(value='com')
                ],
                certificates=[
                    factory.CompanyCertificateFactory.create(code='direct')
                ],
                region=None,
                description='Душевная компания',
                name='TEST Company Limited',
                representatives=[
                    factory.ExternalUserFactory.create(username='savichev')
                ],
                site='https://savichev.me',
                direct_budget=10000
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

        company = Company.objects.get(id=1)

        self.assertEqual(company.proposed_by, '')
        self.assertEqual(company.direct_budget, 10000)

        query_kwargs = {
            'format': 'json',
            'tld': 'ru'
        }
        response = self.client.patch(
            reverse('v1:companies-company', kwargs={'slug': 'test-company-limited'}, query_kwargs=query_kwargs),
            data=json.dumps({
                'proposedBy': 'kvas',
                'directBudget': 15000
            }),
            content_type='application/json',
            HTTP_X_YA_EXTERNAL_LOGIN='savichev'
        )

        expected = {
            'certificates': ['direct'],
            'description': 'Душевная компания',
            'directBudget': 15000,
            'logo': 'https://avatars.mds.yandex.net/get-adv/50995/2a00000168b8792298a697251c015e3d23e9/orig',
            'materials': [],
            'name': 'TEST Company Limited',
            'offices': [
                {
                    'id': 1,
                    'isMain': True,
                    'address': 'street1',
                    'email': 'some1@ya.ru',
                    'city': 54,
                    'map': 'osm',
                    'latitude': '-35.701042',
                    'longitude': '143.363605',
                    'mapZoom': 20,
                    'phone': ''
                }
            ],
            'proposedBy': 'kvas',
            'representatives': ['savichev'],
            'site': 'https://savichev.me',
            'slug': 'test-company-limited',
            'tld': ['ru', 'com'],
            'types': []
        }

        self.assertEqual(response.status_code, 200)
        self.assertJSONEqual(response.content, expected)

        company.refresh_from_db()

        self.assertEqual(company.moderated_object.changed_object.proposed_by, 'kvas')
        self.assertEqual(company.moderated_object.changed_object.direct_budget, 15000)

    def test_patch_with_wrong_materials(self):
        factory.CompanyOfficeFactory.create(
            company=factory.CompanyFactory.create(
                tld=[
                    factory.TldFactory.create(value='ru'),
                    factory.TldFactory.create(value='com')
                ],
                certificates=[
                    factory.CompanyCertificateFactory.create(code='direct')
                ],
                region=None,
                description='Активная компания',
                name='TEST Company Active',
                representatives=[
                    factory.ExternalUserFactory.create(username='active-user')
                ],
                site='https://savichev.me',
                direct_budget=10000
            ),
            is_main=True
        )

        company = Company.objects.get(id=1)

        self.assertEqual(company.proposed_by, '')
        self.assertEqual(company.direct_budget, 10000)

        query_kwargs = {
            'format': 'json',
            'tld': 'ru'
        }
        response = self.client.patch(
            reverse('v1:companies-company', kwargs={'slug': 'test-company-active'}, query_kwargs=query_kwargs),
            data=json.dumps({
                'materials': ['https://yandex.ru/adv/not-case/but-cool']
            }),
            content_type='application/json',
            HTTP_X_YA_EXTERNAL_LOGIN='active-user'
        )

        expected = {
            'materials': {
                '0': [
                    'Некорректная ссылка на материал: https://yandex.ru/adv/not-case/but-cool.',
                ]
            }
        }

        self.assertEqual(response.status_code, 400)
        self.assertJSONEqual(response.content, expected)

    def test_patch_without_tld(self):
        query_kwargs = {
            'format': 'json'
        }
        response = self.client.patch(
            reverse('v1:companies-company', kwargs={'slug': 'nevermind'}, query_kwargs=query_kwargs)
        )

        expected = ["['tld'] query parameters are missing!"]

        self.assertEqual(response.status_code, 400)
        self.assertJSONEqual(response.content, expected)

    def test_patch_not_found(self):
        factory.CompanyOfficeFactory.create(
            company=factory.CompanyFactory.create(
                name='Not found',
                tld=[
                    factory.TldFactory.create(value='com')
                ]
            ),
            is_main=True
        )
        query_kwargs = {
            'format': 'json',
            'tld': 'ru'
        }
        response = self.client.patch(
            reverse('v1:companies-company', kwargs={'slug': 'not-found'}, query_kwargs=query_kwargs)
        )

        expected = {
            'detail': 'Не найдено.'
        }

        self.assertEqual(response.status_code, 404)
        self.assertJSONEqual(response.content, expected)
