import datetime
from unittest.mock import MagicMock

from django.urls import reverse

from cars.billing.core.payment_processor import (
    BonusPaymentProcessor, CardPaymentProcessor, PaymentProcessor,
)
from cars.carsharing.factories.car import CarFactory
from cars.core.trust import StubTrustClient
from cars.orders.core.order_debt_manager import OrderDebtManager
from cars.orders.core.order_manager import OrderManager
from cars.orders.core.order_payment_processor import OrderPaymentProcessor
from cars.orders.core.order_request import OrderItemRequestContext, OrderRequest
from cars.orders.core.preliminary_payments_manager import OrderPreliminaryPaymentsManager
from cars.orders.models.order_item import OrderItem
from cars.users.factories.user import UserFactory
from cars.users.factories.user_credit_card import UserCreditCardFactory


class OrdersHelper:

    def __init__(self, tc, user=None, car=None):
        self._tc = tc

        if user is None:
            user = UserFactory.create(
                uid=1,
            )
            user.credit_card.delete()
            UserCreditCardFactory.create(
                user=user,
                paymethod_id='card-1234',
                pan_prefix='555555',
                pan_suffix='4444',
            )

        if car is None:
            car = CarFactory.create()

        self.user = user
        self.car = car

        self.order_operation_notifier_mock = MagicMock()
        self.push_client_mock = MagicMock()

        self.trust_client = StubTrustClient(default_product_id='777')
        self.trust_client.create_product(
            name='test',
            product_id='777',
            product_type='app',
        )
        self.card_paymethod_id = self.trust_client.create_payment_method(
            uid=self.user.uid,
            id_='card-1234'
        )['id']

        self.bonus_payment_processor = BonusPaymentProcessor()
        self.card_payment_processor = CardPaymentProcessor(
            trust_client=self.trust_client,
            fiscal_title='test',
            fiscal_nds='test',
            clear_delay=datetime.timedelta(days=1),
        )
        self.payment_processor = PaymentProcessor(
            bonus_processor=self.bonus_payment_processor,
            card_processor=self.card_payment_processor,
        )
        self.order_payment_processor = OrderPaymentProcessor(
            payment_processor=self.payment_processor,
            chunked_payment_amount=0,
            push_client=self.push_client_mock,
        )
        self.order_debt_manager = OrderDebtManager(
            order_payment_processor=self.order_payment_processor,
            debt_order_termination_threshold=0,
            debt_order_termination_delay=0,
        )
        self.preliminary_payments_manager = OrderPreliminaryPaymentsManager(
            order_payment_processor=self.order_payment_processor,
            amount=2,
            n_orders_threshold=10,
            enable_fraction_above_threshold=0.5,
            enable_fraction_below_threshold=0.5,
        )

        self.order_manager = OrderManager(
            trust_client=self.trust_client,
            order_payment_processor=self.order_payment_processor,
            order_debt_manager=self.order_debt_manager,
            order_operation_notifier=self.order_operation_notifier_mock,
            preliminary_payments_manager=self.preliminary_payments_manager,
            push_client=self.push_client_mock,
        )

    def finalize(self):
        self.trust_client.clear()

    def assert_order_payment_status_equal(self, order, payment_status):
        self._tc.assertIs(order.get_payment_status(), payment_status)

    def get_intro_url(self):
        return reverse('drive:intro')

    def get_order_list_url(self):
        return reverse('drive:order-list')

    def get_order_item_details_url(self, order_id, order_item_id):
        return reverse(
            'drive:order-item-details',
            kwargs={
                'order_id': order_id,
                'order_item_id': order_item_id,
            },
        )

    def get_order_item_list_url(self, order_id):
        return reverse(
            'drive:order-item-list',
            kwargs={
                'order_id': order_id,
            },
        )

    def make_intro_request(self):
        response = self._tc.client.get(self.get_intro_url())
        self._tc.assert_response_ok(response)
        return response

    def initialize_reservation(self, user=None, car=None):
        if user is None:
            user = self.user

        if car is None:
            car = self.car

        order_request = OrderRequest.from_dict(
            user=user,
            data={
                'items': [
                    {
                        'type': 'carsharing_reservation',
                        'params': {
                            'car_id': str(car.id),
                        },
                    },
                ],
            },
            context=OrderItemRequestContext(
                oauth_token=None,
            ),
        )

        order = self.order_manager.create_order(order_request).order

        return order

    def make_reservation_order(self, user=None, car_id=None):
        if user is None:
            user = self.user

        with self._tc.as_user(user):
            response = self._tc.client.post(
                self.get_order_list_url(),
                data=self.get_reservation_payload(car_id=car_id),
            )

        return response

    def get_reservation_payload(self, car_id=None):
        if car_id is None:
            car_id = self.car.id

        data = {
            'items': [
                {
                    'type': OrderItem.Type.CARSHARING_RESERVATION.value,
                    'params': {
                        'car_id': str(car_id),
                    },
                },
            ],
        }

        return data

    def make_acceptance_request(self, order_id, car_id=None, user=None):
        if car_id is None:
            car_id = self.car.id

        if user is None:
            user = self.user

        url = self.get_order_item_list_url(order_id)
        data = {
            'type': 'carsharing_acceptance',
            'params': {
                'car_id': str(car_id),
            },
        }
        with self._tc.as_user(user):
            response = self._tc.client.post(url, data=data)

        return response

    def complete_acceptance(self, order_id, car_id=None, user=None,
                            car_condition='ok', fuel_card_present=True,
                            sts_present=True, insurance_present=True):
        if car_id is None:
            car_id = self.car.id

        if user is None:
            user = self.user

        acceptance_item = (
            OrderItem
            .objects
            .filter(
                order_id=order_id,
                type=OrderItem.Type.CARSHARING_ACCEPTANCE.value,
            )
            .first()
        )
        if not acceptance_item:
            response = self.make_acceptance_request(order_id=order_id, car_id=car_id)
            acceptance_item_id = response.json()['order']['items'][-1]['id']
        else:
            acceptance_item_id = str(acceptance_item.id)

        url = self.get_order_item_details_url(
            order_id=order_id,
            order_item_id=acceptance_item_id,
        )
        data = {
            'action': 'report_condition',
            'params': {
                'car_condition': car_condition,
                'fuel_card_present': fuel_card_present,
                'insurance_present': insurance_present,
                'sts_present': sts_present,
            },
        }
        with self._tc.as_user(user):
            response = self._tc.client.post(url, data=data)
        self._tc.assert_response_success(response)

    def make_ride_request(self, order_id, car_id=None, fix_id=None, user=None):
        if car_id is None:
            car_id = self.car.id

        if user is None:
            user = self.user

        url = self.get_order_item_list_url(order_id)
        data = {
            'type': 'carsharing_ride',
            'params': {
                'car_id': str(car_id),
            },
        }
        if fix_id is not None:
            data['params']['fix_id'] = fix_id

        with self._tc.as_user(user):
            response = self._tc.client.post(url, data=data)

        return response

    def make_finish_order_item_request(self, order_id, order_item_id, user=None):
        if user is None:
            user = self.user

        url = self.get_order_item_details_url(
            order_id=order_id,
            order_item_id=order_item_id,
        )
        data = {
            'action': 'finish',
        }

        with self._tc.as_user(user):
            response = self._tc.client.post(url, data=data)

        return response
