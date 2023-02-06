from django.conf import settings

from http.cookies import SimpleCookie

from rest_framework.test import APITransactionTestCase, APIClient

from commerce.adv_backend.backend.models import Company
from commerce.adv_backend.backend.serializers import CompaniesDownloadPageSerializer
from commerce.adv_backend.backend.utils import reverse
from commerce.adv_backend.backend.views import CompaniesDownloadView

from commerce.adv_backend.tests import factory
from commerce.adv_backend.tests.backend.utils import create_test_user

from urllib.parse import urlencode

FIXTURES_PATH = 'commerce/adv_backend/tests/backend/fixtures'


class TestViewCompaniesDownloadCases(APITransactionTestCase):
    client = APIClient()
    maxDiff = None
    reset_sequences = True

    def test_get_page(self):
        factory.TldFactory.create(value='ru')
        factory.TldFactory.create(value='com')

        create_test_user('test-admin', 'superuser')

        self.client.cookies = SimpleCookie({'yandex_login': 'test-admin'})
        response = self.client.get(reverse('v1:companies-download'))

        self.assertEqual(response.status_code, 200)

        self.assertIsInstance(response.context['serializer'], CompaniesDownloadPageSerializer)
        self.assertEqual(response.context['button_text'], 'Выгрузить')
        self.assertEqual(response.context['title'], 'Выгрузка компаний')
        self.assertEqual(response.context['url_name'], 'v1:companies-download')

        self.assertTemplateUsed(response, 'company/download.html', count=1)
        self.assertTemplateUsed(response, 'rest_framework/vertical/form.html', count=1)
        self.assertTemplateUsed(response, 'rest_framework/vertical/select.html', count=1)

        self.assertInHTML('<option value="ru">ru</option>', response.content.decode('utf-8'))
        self.assertInHTML('<option value="com">com</option>', response.content.decode('utf-8'))

    def test_get_page_check_permission(self):
        create_test_user('test-admin', 'asessor')

        self.client.cookies = SimpleCookie({'yandex_login': 'test-admin'})
        response = self.client.get(reverse('v1:companies-download'))

        self.assertEqual(response.status_code, 403)
        self.assertTemplateUsed(response, '403.html', count=1)

    def test_post(self):
        factory.TldFactory.create(value='ru')
        factory.TldFactory.create(value='com')

        create_test_user('test-admin', 'superuser')

        self.client.cookies = SimpleCookie({'yandex_login': 'test-admin'})
        response = self.client.post(
            reverse('v1:companies-download'),
            data=urlencode({'tld': 'ru'}),
            content_type='application/x-www-form-urlencoded'
        )

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.get('Content-Type'), 'text/csv')
        self.assertEquals(response.get('Content-Disposition'), 'attachment; filename=companies_ru.csv')

    def test_post_invalid_tld(self):
        create_test_user('test-admin', 'superuser')

        self.client.cookies = SimpleCookie({'yandex_login': 'test-admin'})
        response = self.client.post(
            reverse('v1:companies-download'),
            data=urlencode({'tld': 'invalid'}),
            content_type='application/x-www-form-urlencoded'
        )

        self.assertEqual(response.status_code, 200)
        self.assertTemplateUsed(response, '400.html', count=1)

    def test_build_csv_data_filter_by_tld(self):
        factory.CompanyFactory.create(
            id=1,
            tld=[
                factory.TldFactory.create(value='ru'),
                factory.TldFactory.create(value='com')
            ],
            certificates=[
                factory.CompanyCertificateFactory.create(code='direct')
            ],
            name='First ru company',
            slug='first_ru_company',
            representatives=[
                factory.ExternalUserFactory.create(username='user-1'),
                factory.ExternalUserFactory.create(username='user-2')
            ],
            region=factory.RegionFactory.create(name='Sverdlovsk Oblast'),
            is_active=True,
            is_partner=False
        )

        first_company = Company.objects.get(id=1)

        factory.CompanyOfficeFactory.create(
            company=first_company,
            is_main=True,
            city=factory.CityFactory.create(name='Yekaterinburg')
        )
        # Офис в том же городе; проверяем, что город попадет в выгрузку один раз
        factory.CompanyOfficeFactory.create(
            company=first_company,
            is_main=True,
            city=factory.CityFactory.create(name='Yekaterinburg')
        )
        factory.CompanyOfficeFactory.create(
            company=first_company,
            is_main=True,
            city=factory.CityFactory.create(name='Samara')
        )
        factory.CompanyOfficeFactory.create(
            company=first_company,
            is_main=False,
            city=factory.CityFactory.create(name='Ufa')
        )

        factory.CompanyOfficeFactory.create(
            company=factory.CompanyFactory.create(
                id=2,
                tld=[
                    factory.TldFactory.create(value='ru')
                ],
                certificates=[
                    factory.CompanyCertificateFactory.create(code='market'),
                    factory.CompanyCertificateFactory.create(code='dialogs')
                ],
                name='Second ru company',
                slug='second_ru_company',
                representatives=[
                    factory.ExternalUserFactory.create(username='user-3'),
                ],
                region=factory.RegionFactory.create(name='Tver Oblast'),
                is_active=False,
                is_partner=False
            ),
            is_main=True,
            city=factory.CityFactory.create(name='Tver')
        )

        factory.CompanyOfficeFactory.create(
            company=factory.CompanyFactory.create(
                id=3,
                tld=[
                    factory.TldFactory.create(value='com')
                ],
                certificates=[
                    factory.CompanyCertificateFactory.create(code='market'),
                ],
                name='Com company',
                slug='com_company',
                representatives=[
                    factory.ExternalUserFactory.create(username='user-4'),
                ],
                region=factory.RegionFactory.create(name='Samara Oblast'),
                is_active=True
            ),
            is_main=True,
            city=factory.CityFactory.create(name='Yekaterinburg')
        )

        actual = CompaniesDownloadView().build_csv_data('ru')

        self.assertEqual(actual, [
            {
                'representatives': 'Главные представители',
                'title': 'Название',
                'slug': 'Слаг',
                'cities': 'Города',
                'region': 'Регион',
                'direct': 'Директ',
                'metrika': 'Метрика',
                'market': 'Маркет',
                'dialogs': 'Диалоги',
                'toloka': 'Толока',
                'is_active': 'Активно ли агентство',
                'is_partner': 'Является ли партнером',
                'company_admin_link': 'Ссылка на компанию в админке'
            },
            {
                'representatives': 'user-1, user-2',
                'title': 'First ru company',
                'slug': 'first_ru_company',
                'cities': 'Samara, Yekaterinburg',
                'region': 'Sverdlovsk Oblast',
                'direct': 'да',
                'metrika': 'нет',
                'market': 'нет',
                'dialogs': 'нет',
                'toloka': 'нет',
                'is_active': 'да',
                'is_partner': 'нет',
                'company_admin_link': f'{settings.ADMIN_BASE_URL}/admin/backend/company/1/change/'
            },
            {
                'representatives': 'user-3',
                'title': 'Second ru company',
                'slug': 'second_ru_company',
                'cities': 'Tver',
                'region': 'Tver Oblast',
                'direct': 'нет',
                'metrika': 'нет',
                'market': 'да',
                'dialogs': 'да',
                'toloka': 'нет',
                'is_active': 'нет',
                'is_partner': 'нет',
                'company_admin_link': f'{settings.ADMIN_BASE_URL}/admin/backend/company/2/change/'
            }
        ])

    def test_build_csv_data_empty_fields(self):
        factory.CompanyOfficeFactory.create(
            company=factory.CompanyFactory.create(
                id=1,
                tld=[
                    factory.TldFactory.create(value='ru')
                ],
                certificates=[],
                name='ru company',
                slug='ru_company',
                representatives=[],
                region=None,
                is_active=True,
                is_partner=True
            ),
            is_main=True,
            city=factory.CityFactory.create(name='Yekaterinburg')
        )

        actual = CompaniesDownloadView().build_csv_data('ru')

        self.assertEqual(actual, [
            {
                'representatives': 'Главные представители',
                'title': 'Название',
                'slug': 'Слаг',
                'cities': 'Города',
                'region': 'Регион',
                'direct': 'Директ',
                'metrika': 'Метрика',
                'market': 'Маркет',
                'dialogs': 'Диалоги',
                'toloka': 'Толока',
                'is_active': 'Активно ли агентство',
                'is_partner': 'Является ли партнером',
                'company_admin_link': 'Ссылка на компанию в админке'
            },
            {
                'representatives': '',
                'title': 'ru company',
                'slug': 'ru_company',
                'cities': 'Yekaterinburg',
                'region': '',
                'direct': 'нет',
                'metrika': 'нет',
                'market': 'нет',
                'dialogs': 'нет',
                'toloka': 'нет',
                'is_active': 'да',
                'is_partner': 'да',
                'company_admin_link': f'{settings.ADMIN_BASE_URL}/admin/backend/company/1/change/'
            }
        ])
