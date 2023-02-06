import decimal

from django.test import TestCase

from cars.users.models.user import User
from ..core.bonus_account_manager import BonusAccountManager
from ..factories.bonus_account import BonusAccountFactory
from ..models.bonus_account import BonusAccount


class BonusAccountManagerTestCase(TestCase):

    def setUp(self):
        self._account = BonusAccountFactory.create()
        self.mgr = BonusAccountManager(self.account)

    @property
    def account(self):
        return BonusAccount.objects.get(id=self._account.id)

    def test_update_registration_taxi_cashback_earned(self):
        value = decimal.Decimal('123.45')
        double_value = 2 * value

        self.mgr.update_registration_taxi_cashback_earned(value)
        self.assertEqual(self.account.balance, value)
        self.assertEqual(self.account.registration_taxi_cashback_earned, value)
        self.assertEqual(self.account.registration_taxi_cashback_spent, 0)

        self.mgr.update_registration_taxi_cashback_earned(double_value)
        self.assertEqual(self.account.balance, double_value)
        self.assertEqual(self.account.registration_taxi_cashback_earned, double_value)
        self.assertEqual(self.account.registration_taxi_cashback_spent, 0)

        with self.assertRaises(Exception):
            self.mgr.update_registration_taxi_cashback_earned(value)

    def test_withdraw_from_registration_taxi_cashback(self):
        self.mgr.update_registration_taxi_cashback_earned(1)
        self.mgr.withdraw(1)
        self.assertEqual(self.account.balance, 0)
        self.assertEqual(self.account.registration_taxi_cashback_earned, 1)
        self.assertEqual(self.account.registration_taxi_cashback_spent, 1)

    def test_withdraw_from_generic(self):
        self.mgr.debit_generic(1, operator=User.objects.first(), comment='', nonce='')
        self.mgr.withdraw(1)
        self.assertEqual(self.account.balance, 0)
        self.assertEqual(self.account.generic_earned, 1)
        self.assertEqual(self.account.generic_spent, 1)

    def test_withdraw_from_mix(self):
        self.mgr.update_registration_taxi_cashback_earned(1)
        self.mgr.debit_generic(1, operator=User.objects.first(), comment='', nonce='')
        self.mgr.withdraw(2)
        self.assertEqual(self.account.balance, 0)
        self.assertEqual(self.account.registration_taxi_cashback_earned, 1)
        self.assertEqual(self.account.registration_taxi_cashback_spent, 1)
        self.assertEqual(self.account.generic_earned, 1)
        self.assertEqual(self.account.generic_spent, 1)

    def test_withdraw_insufficient_funds(self):
        with self.assertRaises(BonusAccountManager.InsufficientFundsError):
            self.mgr.withdraw(1)
