from unittest.mock import MagicMock

from django.urls import reverse

from cars.core.trust import StubTrustClient
from cars.django.tests import CarsharingAPITestCase
from cars.users.factories.user import UserFactory
from cars.users.models.user_credit_card import UserCreditCard, UserCreditCardHistory


class UserCreditCardTestCase(CarsharingAPITestCase):

    @property
    def url(self):
        return reverse('drive:user-credit-card')

    def setUp(self):
        self.user = UserFactory.create(uid=1)
        self.user.credit_card.delete()
        self.trust_client = StubTrustClient.from_settings(push_client=MagicMock())

    def tearDown(self):
        self.trust_client.clear()

    def post(self, paymethod_id):
        data = {
            'paymethod_id': paymethod_id,
        }
        response = self.client.post(self.url, data=data)
        return response

    def test_no_data(self):
        response = self.client.post(self.url)
        self.assert_response_bad_request(response)

    def test_no_paymethod_id(self):
        data = {
            'pan': {
                'prefix': '123456',
                'suffix': '1234',
            },
        }
        response = self.client.post(self.url, data=data)
        self.assert_response_bad_request(response)

    def test_not_bound(self):
        response = self.post(
            paymethod_id='card-x1234',
        )
        self.assert_response_errors(response, code='credit_card.not_bound')

    def test_incorrect_payment_id(self):
        self.trust_client.create_payment_method(
            uid=self.user.uid,
            id_='card-x1234',
            account='123456****1234',
        )
        response = self.post(
            paymethod_id='card-x4321',
        )
        self.assert_response_errors(response, code='credit_card.not_bound')

    def test_ok(self):
        self.trust_client.create_payment_method(
            uid=self.user.uid,
            id_='card-x1234',
            account='123456****1234',
        )

        response = self.post(
            paymethod_id='card-x1234',
        )
        self.assert_response_success(response)

        credit_card = UserCreditCard.objects.get(user=self.user)
        self.assertEqual(credit_card.paymethod_id, 'card-x1234')
        self.assertEqual(credit_card.pan_prefix, '123456')
        self.assertEqual(credit_card.pan_suffix, '1234')

    def test_bind_twice(self):
        self.trust_client.create_payment_method(
            uid=self.user.uid,
            id_='card-x1234',
            account='123456****1234',
        )
        self.trust_client.create_payment_method(
            uid=self.user.uid,
            id_='card-x4321',
            account='654321****4321',
        )

        response = self.post(
            paymethod_id='card-x1234',
        )
        self.assert_response_success(response)

        response = self.post(
            paymethod_id='card-x4321',
        )
        self.assert_response_success(response)

        credit_card = UserCreditCard.objects.get(user=self.user)
        self.assertEqual(credit_card.paymethod_id, 'card-x4321')

        credit_card_history = list(UserCreditCardHistory.objects.filter(user=self.user))
        self.assertEqual(len(credit_card_history), 1)
        history_entry = credit_card_history[0]
        self.assertEqual(history_entry.paymethod_id, 'card-x1234')
