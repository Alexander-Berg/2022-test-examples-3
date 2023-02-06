# -*- coding: utf-8 -*-

from common.middleware.locale_request import Locale
from common.tester.testcase import TestCase


class TestLocaleRequest(TestCase):

    def test_get_feedback_url_by_tld(self):
        locale = Locale()
        feedback_url_ru = locale.get_feedback_url_by_national_version_and_tld('ru', 'ru')
        assert feedback_url_ru == locale.get_feedback_url_by_national_version_and_tld('ru', 'kz')
        assert feedback_url_ru == locale.get_feedback_url_by_national_version_and_tld('ru', 'uz')
        assert feedback_url_ru == locale.get_feedback_url_by_national_version_and_tld('ru', 'by')
        assert feedback_url_ru != locale.get_feedback_url_by_national_version_and_tld('ua', 'ua')
