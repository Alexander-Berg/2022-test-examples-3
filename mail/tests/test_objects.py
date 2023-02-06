from django.test import TestCase

from fan.models import Account
from fan.campaigns.create import create_campaign


def _rand_string(N=32):
    import random
    import string

    return "".join(random.choice(string.ascii_uppercase + string.digits) for _ in range(N))


def create_account():
    account = Account(name=_rand_string())
    account.save()
    return account


class ObjectBasicsTestCase(TestCase):
    def setUp(self):
        self.account = create_account()
        self.campaign = create_campaign(account=self.account)

    def test_letter_create(self):
        self.assertEqual(self.campaign.default_letter.code, "A")
        self.assertIsNotNone(self.campaign.get_letter("A"))
        self.assertIsNone(self.campaign.get_letter("B"))

        l = self.campaign.create_letter()
        self.assertEqual(l.code, "B")
        self.assertIsNotNone(self.campaign.get_letter("B"))

    def test_getters(self):
        self.assertEqual(self.account.get_campaign(self.campaign.pk).id, self.campaign.pk)
        self.assertIsNone(self.account.get_campaign(-1))

        # Проверим, что фильтрация по аккаунту работает
        account2 = create_account()
        self.assertIsNone(account2.get_campaign(self.campaign.pk))
