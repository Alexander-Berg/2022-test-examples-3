# coding=utf-8
import unittest

from v2 import ids
from v2.soup import soup_validation
from utils import utils


class TestSoupNormalization(unittest.TestCase):
    def test_good_yandexuid(self):
        normed = soup_validation.normalize(ids.YANDEXUID, '23424234')
        assert normed == '23424234'

    def test_parse_yandexuid(self):
        normed = soup_validation.normalize(ids.YANDEXUID, '23424234a')
        assert normed == '23424234'

    def test_bad_yandexuid(self):
        normed = soup_validation.normalize(ids.YANDEXUID, '2345235234523452353452')
        assert normed is None

    def test_good_email(self):
        normed = soup_validation.normalize(ids.EMAIL, '23424234a@ya.ru')
        assert normed == '23424234a@yandex.ru'

    def test_parse_email(self):
        normed = soup_validation.normalize(ids.EMAIL, '{23424234a@ya.ru} ')
        assert normed is None  # WTF!!!!

    def test_bad_email(self):
        normed = soup_validation.normalize(ids.EMAIL, '23424234a@y.')
        assert normed is None

    def test_parse_phone(self):
        normed = soup_validation.normalize(ids.PHONE, '+7(910)5551111')
        assert normed == '+79105551111'

    def test_bad_phone(self):
        normed = soup_validation.normalize(ids.PHONE, '7910555....')
        assert normed is None

    def test_good_uuid(self):
        normed = soup_validation.normalize(ids.UUID, ' 00001c6d7415a9c924cdf60e87a80e2c')
        assert normed == '00001c6d7415a9c924cdf60e87a80e2c'

    def test_bad_uuid(self):
        normed = soup_validation.normalize(ids.UUID, '00001c6d7415a9c924cdf60e87a80e2ca')
        assert normed is None

    def test_remove_bad_symbols(self):
        normed = soup_validation.normalize("default", '"�ĭinst��"')
        assert normed == 'inst'

    def test_norm_login_to_email(self):
        email = utils.login_to_email("my_login")
        assert email == "my_login@yandex.ru"

        email = utils.login_to_email("my.login")
        assert email == "my-login@yandex.ru"

        email = utils.login_to_email("my.domain@login.tu")
        assert email == "my.domain@login.tu"
