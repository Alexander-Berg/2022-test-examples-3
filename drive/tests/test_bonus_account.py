import decimal
import uuid

from django.urls import reverse

from cars.billing.models.bonus_account import BonusAccount, BonusAccountOperation
from cars.users.models.user import User
from .base import AdminAPITestCase


class BonusAccountTestCase(AdminAPITestCase):

    def get_debit_url(self, user=None):
        if user is None:
            user = self.user
        return reverse('cars-admin:user-bonus-account-debit', kwargs={'user_id': user.id})

    def get_credit_url(self, user=None):
        if user is None:
            user = self.user
        return reverse('cars-admin:user-bonus-account-credit', kwargs={'user_id': user.id})

    def get_operations_url(self, user=None):
        if user is None:
            user = self.user
        return reverse('cars-admin:user-bonus-account-operation-list', kwargs={'user_id': user.id})

    def make_credit_request(self, amount, comment=None, nonce=None, user=None):
        url = self.get_credit_url(user=user)
        return self.make_update_request(url=url, amount=amount, comment=comment, nonce=nonce)

    def make_debit_request(self, amount, comment=None, nonce=None, user=None):
        url = self.get_debit_url(user=user)
        return self.make_update_request(url=url, amount=amount, comment=comment, nonce=nonce)

    def make_update_request(self, url, amount, comment=None, nonce=None):
        if comment is None:
            comment = str(uuid.uuid4())
        if nonce is None:
            nonce = str(uuid.uuid4())

        data = {
            'amount': amount,
            'comment': comment,
            'nonce': nonce,
        }

        response = self.client.post(url, data=data)

        return response

    def test_debit(self):
        amount = decimal.Decimal('1.23')
        response = self.make_debit_request(amount=amount, comment='test')

        self.assert_response_success(response)

        account = BonusAccount.objects.get(user=self.user)
        self.assertEqual(account.balance, amount)

        operation = (
            BonusAccountOperation.objects
            .filter(bonus_account=account)
            .order_by('-created_at')
            .first()
        )
        self.assertEqual(operation.comment, 'test')

        bonus_account_data = response.data['user']['bonus_account']
        self.assertEqual(bonus_account_data['balance'], str(amount))
        self.assertEqual(bonus_account_data['generic']['earned'], str(amount))
        self.assertEqual(bonus_account_data['generic']['spent'], '0.00')

    def test_debit_same_nonce(self):
        amount = decimal.Decimal('1.23')

        response1 = self.make_debit_request(amount=amount, nonce='0xdeadbeef')
        self.assert_response_success(response1)

        response2 = self.make_debit_request(amount=amount, nonce='0xdeadbeef')
        self.assert_response_success(response2)

        self.assertEqual(BonusAccount.objects.get(user=self.user).balance, amount)

    def test_credit(self):
        amount = decimal.Decimal('1.23')
        balance = 2 * amount

        self.make_debit_request(amount=balance)
        response = self.make_credit_request(amount=amount, comment='test')

        self.assert_response_success(response)

        account = BonusAccount.objects.get(user=self.user)
        self.assertEqual(account.balance, amount)

        operation = (
            BonusAccountOperation.objects
            .filter(bonus_account=account)
            .order_by('-created_at')
            .first()
        )
        self.assertEqual(operation.comment, 'test')

        bonus_account_data = response.data['user']['bonus_account']
        self.assertEqual(bonus_account_data['balance'], str(amount))
        self.assertEqual(bonus_account_data['generic']['earned'], str(amount))
        self.assertEqual(bonus_account_data['generic']['spent'], '0.00')

    def test_credit_same_nonce(self):
        balance = decimal.Decimal(10)
        amount = decimal.Decimal('1.23')

        self.make_debit_request(amount=balance)

        response1 = self.make_credit_request(amount=amount, nonce='0xdeadbeef')
        self.assert_response_success(response1)

        response2 = self.make_credit_request(amount=amount, nonce='0xdeadbeef')
        self.assert_response_success(response2)

        self.assertEqual(BonusAccount.objects.get(user=self.user).balance, balance - amount)

    def test_credit_insufficient_funds(self):
        self.make_debit_request(amount=1)

        amount = decimal.Decimal('1.23')
        response = self.make_credit_request(amount=amount)

        self.assert_response_errors(response, code='user.bonus_account.insufficient_funds')
        self.assertEqual(BonusAccount.objects.get(user=self.user).balance, 1)

    def test_history(self):
        self.make_debit_request(amount=2)
        self.make_credit_request(amount=1)
        operations = list(BonusAccountOperation.objects.order_by('created_at'))
        self.assertEqual(len(operations), 2)

        created_by = User.objects.get(uid=1)

        op1 = operations[0]
        self.assertEqual(op1.amount, 2)
        self.assertEqual(op1.balance, 2)
        self.assertEqual(op1.created_by, created_by)

        op2 = operations[1]
        self.assertEqual(op2.amount, -1)
        self.assertEqual(op2.balance, 1)
        self.assertEqual(op2.created_by, created_by)

        response = self.client.get(self.get_operations_url())

        results_data = response.json()['results']
        self.assertEqual(len(results_data), 2)

        self.assertEqual(results_data[1]['amount'], '2.00')
        self.assertEqual(results_data[1]['balance'], '2.00')

        self.assertEqual(results_data[0]['amount'], '-1.00')
        self.assertEqual(results_data[0]['balance'], '1.00')
