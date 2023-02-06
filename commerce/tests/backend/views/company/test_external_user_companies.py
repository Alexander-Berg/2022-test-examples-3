from rest_framework.test import APITransactionTestCase, APIClient

from commerce.adv_backend.backend.utils import reverse

from commerce.adv_backend.tests import factory


class TestViewExternalUserCompaniesCases(APITransactionTestCase):
    client = APIClient()
    maxDiff = None
    reset_sequences = True

    def test_get_with_filter_by_tld(self):
        factory.CompanyFactory.create(
            name='Company 1',
            slug='company-edit-1',
            tld=[
                factory.TldFactory.create(value='ru')
            ],
            representatives=[
                factory.ExternalUserFactory.create(username='yoy')
            ]
        )

        factory.CompanyFactory.create(
            name='Company 2',
            slug='company-edit-2',
            tld=[
                factory.TldFactory.create(value='com')
            ],
            representatives=[
                factory.ExternalUserFactory.create(username='yoy')
            ]
        )

        factory.CompanyFactory.create(
            name='Company 3',
            slug='company-edit-3',
            tld=[
                factory.TldFactory.create(value='ru')
            ],
            representatives=[
                factory.ExternalUserFactory.create(username='yoy')
            ]
        )

        query_kwargs = {
            'format': 'json',
            'tld': 'ru'
        }
        response = self.client.get(
            reverse('v1:external-user-companies', query_kwargs=query_kwargs),
            HTTP_X_YA_EXTERNAL_LOGIN='yoy'
        )

        expected = [
            {'slug': 'company-edit-1', 'name': 'Company 1', 'isActive': True},
            {'slug': 'company-edit-3', 'name': 'Company 3', 'isActive': True}
        ]

        self.assertEqual(response.status_code, 200)
        self.assertJSONEqual(response.content, expected)

    def test_get_with_filter_by_representative(self):
        factory.CompanyFactory.create(
            name='Company 1',
            slug='company-edit-1',
            tld=[
                factory.TldFactory.create(value='ru')
            ],
            representatives=[
                factory.ExternalUserFactory.create(username='kvas')
            ]
        )

        factory.CompanyFactory.create(
            name='Company 2',
            slug='company-edit-2',
            tld=[
                factory.TldFactory.create(value='ru')
            ],
            representatives=[
                factory.ExternalUserFactory.create(username='kvas'),
                factory.ExternalUserFactory.create(username='yukiru')
            ]
        )

        factory.CompanyFactory.create(
            name='Company 3',
            slug='company-edit-3',
            tld=[
                factory.TldFactory.create(value='ru')
            ],
            representatives=[
                factory.ExternalUserFactory.create(username='yukiru')
            ]
        )

        query_kwargs = {
            'format': 'json',
            'tld': 'ru'
        }
        response = self.client.get(
            reverse('v1:external-user-companies', query_kwargs=query_kwargs),
            HTTP_X_YA_EXTERNAL_LOGIN='kvas'
        )

        expected = [
            {'slug': 'company-edit-1', 'name': 'Company 1', 'isActive': True},
            {'slug': 'company-edit-2', 'name': 'Company 2', 'isActive': True}
        ]

        self.assertEqual(response.status_code, 200)
        self.assertJSONEqual(response.content, expected)

    def test_get_inactive_companies(self):
        factory.CompanyFactory.create(
            name='Active company',
            slug='active-company',
            tld=[
                factory.TldFactory.create(value='ru')
            ],
            representatives=[
                factory.ExternalUserFactory.create(username='want_to_office')
            ],
            is_active=True
        )
        factory.CompanyFactory.create(
            name='Inactive company',
            slug='inactive-company',
            tld=[
                factory.TldFactory.create(value='ru')
            ],
            representatives=[
                factory.ExternalUserFactory.create(username='want_to_office')
            ],
            is_active=False
        )

        query_kwargs = {
            'format': 'json',
            'tld': 'ru'
        }
        response = self.client.get(
            reverse('v1:external-user-companies', query_kwargs=query_kwargs),
            HTTP_X_YA_EXTERNAL_LOGIN='want_to_office'
        )

        expected = [
            {'slug': 'active-company', 'name': 'Active company', 'isActive': True},
            {'slug': 'inactive-company', 'name': 'Inactive company', 'isActive': False}
        ]

        self.assertEqual(response.status_code, 200)
        self.assertJSONEqual(response.content, expected)

    def test_get_without_tld(self):
        query_kwargs = {
            'format': 'json'
        }
        response = self.client.get(
            reverse('v1:external-user-companies', query_kwargs=query_kwargs),
            HTTP_X_YA_EXTERNAL_LOGIN='kvas'
        )

        expected = ["['tld'] query parameters are missing!"]

        self.assertEqual(response.status_code, 400)
        self.assertJSONEqual(response.content, expected)

    def test_get_without_external_login(self):
        factory.CompanyFactory.create(
            name='Company TOP',
            slug='company-top',
            tld=[
                factory.TldFactory.create(value='ru')
            ],
            representatives=[
                factory.ExternalUserFactory.create(username='yukiru')
            ]
        )

        query_kwargs = {
            'format': 'json',
            'tld': 'ru'
        }
        response = self.client.get(reverse('v1:external-user-companies', query_kwargs=query_kwargs))

        expected = []

        self.assertEqual(response.status_code, 200)
        self.assertJSONEqual(response.content, expected)
