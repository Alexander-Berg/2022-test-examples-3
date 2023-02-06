from rest_framework.test import APITransactionTestCase, APIClient

from http.cookies import SimpleCookie

from yatest import common as yatest_common

from commerce.adv_backend.backend.serializers import CompaniesFormSerializer
from commerce.adv_backend.backend.utils import reverse

from commerce.adv_backend.tests.factory import CompanyCertificateFactory
from commerce.adv_backend.tests.backend.utils import create_test_user

FIXTURES_PATH = 'commerce/adv_backend/tests/backend/fixtures'


class TestViewCompaniesFormCases(APITransactionTestCase):
    client = APIClient()
    maxDiff = None
    reset_sequences = True

    def test_get(self):
        CompanyCertificateFactory.create(code='dialogs')
        CompanyCertificateFactory.create(code='direct')

        create_test_user('test-admin', 'superuser')

        self.client.cookies = SimpleCookie({'yandex_login': 'test-admin'})
        response = self.client.get(reverse('v1:companies-certificates'))

        self.assertEqual(response.status_code, 200)

        self.assertIsInstance(response.context['serializer'], CompaniesFormSerializer)
        self.assertEqual(response.context['button_text'], 'Отправить')
        self.assertEqual(response.context['title'], 'Форма загрузки компаний')
        self.assertEqual(response.context['url_name'], 'v1:companies-certificates')

        self.assertTemplateUsed(response, 'company/form.html', count=1)
        self.assertTemplateUsed(response, 'rest_framework/vertical/form.html', count=1)
        self.assertTemplateUsed(response, 'rest_framework/vertical/select.html', count=1)
        self.assertTemplateUsed(response, 'rest_framework/vertical/input.html', count=2)

        self.assertInHTML('<option value="1">Dialogs</option>', response.content.decode('utf-8'))
        self.assertInHTML('<option value="2">Direct</option>', response.content.decode('utf-8'))

    def test_get_check_permission(self):
        create_test_user('test-admin', 'asessor')

        self.client.cookies = SimpleCookie({'yandex_login': 'test-admin'})
        response = self.client.get(reverse('v1:companies-certificates'))

        self.assertEqual(response.status_code, 403)
        self.assertTemplateUsed(response, '403.html', count=1)

    def test_post(self):
        create_test_user('test-admin', 'superuser')

        add_list_path = yatest_common.source_path(f'{FIXTURES_PATH}/add_list.csv')
        remove_list_path = yatest_common.source_path(f'{FIXTURES_PATH}/remove_list.csv')

        with open(add_list_path) as add_list, open(remove_list_path) as remove_list:
            CompanyCertificateFactory.create(code='dialogs')

            self.client.cookies = SimpleCookie({'yandex_login': 'test-admin'})

            response = self.client.post(
                reverse('v1:companies-certificates'),
                data={
                    'service': 1,
                    'add_list': add_list,
                    'remove_list': remove_list
                }
            )

            self.assertEqual(response.status_code, 200)

            self.assertEqual(response.context['title'], 'Форма загрузки компаний')
            self.assertEqual(response.context['message'], 'Форма успешно отправлена')

            self.assertTemplateUsed(response, 'company/form_success.html', count=1)
