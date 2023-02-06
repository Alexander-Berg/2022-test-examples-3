from unittest import TestCase

from django.core.exceptions import ValidationError

from commerce.adv_backend.backend.validators import MaterialsUrlValidator


class TestValidatorMaterialsUrlCases(TestCase):
    validate = MaterialsUrlValidator()

    def test_incorrect_url(self):
        url = "i'm not url :("

        with self.assertRaises(ValidationError):
            self.validate(url)

    def test_not_ya_url(self):
        url = 'https://savichev.me'

        with self.assertRaises(ValidationError):
            self.validate(url)

    def test_with_query(self):
        url = 'https://yandex.ru/adv/news/sensation?debug=1&req=test'

        result = self.validate(url)

        self.assertEqual(result, None)

    def test_news_urls(self):
        urls = [
            'https://yandex-ad.cn/adv/news/sensation',
            'https://yandex.by/adv/news/sensation',
            'https://yandex.com.tr/adv/news/sensation',
            'https://yandex.com/adv/news/sensation',
            'https://yandex.kz/adv/news/sensation',
            'https://yandex.ru/adv/news/sensation'
        ]

        result = [self.validate(url) for url in urls]

        for value in result:
            self.assertEqual(value, None)

    def test_cases_urls(self):
        urls = [
            'https://yandex-ad.cn/adv/solutions/cases/cool-case',
            'https://yandex.by/adv/solutions/cases/cool-case',
            'https://yandex.com.tr/adv/solutions/cases/cool-case',
            'https://yandex.com/adv/solutions/cases/cool-case',
            'https://yandex.kz/adv/solutions/cases/cool-case',
            'https://yandex.ru/adv/solutions/cases/cool-case'
        ]

        result = [self.validate(url) for url in urls]

        for value in result:
            self.assertEqual(value, None)
