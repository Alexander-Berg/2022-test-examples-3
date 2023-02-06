import unittest

from crypta.graph.v1.python.v2.soup import soup_validation
from crypta.graph.v1.python.utils import utils


class TestSoupNormalization(unittest.TestCase):
    def test_good_yandexuid(self):
        normed = soup_validation.normalize("yandexuid", "10113701529442803")
        assert normed == "10113701529442803"

    def test_bad_yandexuid(self):
        normed = soup_validation.normalize("yandexuid", "2345235234523452353452")
        assert normed is None

    def test_good_email(self):
        normed = soup_validation.normalize("email", "blabla@yandex.ru")
        assert normed == "blabla@yandex.ru"

    def test_bad_email(self):
        normed = soup_validation.normalize("email", "te@st@yandex.ru")
        assert normed is None

    def test_good_phone(self):
        normed = soup_validation.normalize("phone", "+7(910)5551111")
        assert normed == "+79105551111"

    def test_bad_phone(self):
        normed = soup_validation.normalize("phone", "+89168751594")
        assert normed is None

    def test_good_uuid(self):
        normed = soup_validation.normalize("uuid", "00001c6d7415a9c924cdf60e87a80e2c")
        assert normed == "00001c6d7415a9c924cdf60e87a80e2c"

    def test_bad_uuid(self):
        normed = soup_validation.normalize("uuid", "00001c6d7415a9c924cdf60e87a80e2ca")
        assert normed is None

    def test_norm_login_to_email(self):
        email = utils.login_to_email("my_login")
        assert email == "my_login@yandex.ru"

        email = utils.login_to_email("my.login")
        assert email == "my.login@yandex.ru"

        email = utils.login_to_email("my-login")
        assert email == "my-login@yandex.ru"

        email = utils.login_to_email("my.domain@login.tu")
        assert email == "my.domain@login.tu"

    def test_norm_email_to_phone(self):
        phone = utils.email_to_phone("89168751595@yandex.ru")
        assert phone == "+79168751595"

        phone = utils.email_to_phone("login@yandex.ru")
        assert phone is None

        phone = utils.email_to_phone("84958764563")
        assert phone is None
