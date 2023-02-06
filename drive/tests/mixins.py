import datetime
from unittest.mock import MagicMock

from cars.billing.core.bonus_account_manager import BonusAccountManager
from cars.billing.core.payment_processor import (
    BonusPaymentProcessor, CardPaymentProcessor, PaymentProcessor,
)
from cars.calculator.tests.helper import CalculatorTestHelper
from cars.carsharing.factories.car import CarFactory
from cars.core.trust import StubTrustClient
from cars.users.factories.user import UserFactory
from cars.users.factories.user_credit_card import UserCreditCardFactory
from ..core.order_item_requests.carsharing import (
    CarsharingReservationOrderItemRequest,
    CarsharingReservationPaidOrderItemRequest,
)
from ..core.order_debt_manager import OrderDebtManager
from ..core.order_manager import OrderManager
from ..core.order_payment_processor import OrderPaymentProcessor
from ..core.order_request import OrderItemRequest, OrderPaymentMethodRequest, OrderRequest
from ..core.preliminary_payments_manager import OrderPreliminaryPaymentsManager
from ..models.order_item import OrderItem
from ..models.order_item_payment import OrderItemPayment
from ..models.order_item_tariff import (
    FixOrderItemTariffParams, OrderItemTariff, PerMinuteOrderItemTariffParams,
)
from ..models.order_payment_method import OrderPaymentMethod


class OrderTestCaseMixin:

    def setUp(self):
        super().setUp()

        self.user = UserFactory.create(uid=1)
        self.user.credit_card.delete()
        UserCreditCardFactory.create(
            user=self.user,
            paymethod_id='card-1234',
            pan_prefix='555555',
            pan_suffix='4444',
        )

        self.car = CarFactory.create()

        self.ch = CalculatorTestHelper(tc=self)
        self.ch.setUp()

        self.bonus_account_manager = BonusAccountManager.from_user(self.user)
        self.bonus_payment_processor = BonusPaymentProcessor()

        self.trust_client = StubTrustClient(default_product_id='777')
        self.trust_client.create_product(
            name='test',
            product_id='777',
            product_type='app',
        )
        self.paymethod_id = self.trust_client.create_payment_method(
            uid=self.user.uid,
            id_='card-1234'
        )['id']
        self.card_payment_processor = CardPaymentProcessor(
            trust_client=self.trust_client,
            fiscal_title='[test] Yandex.Drive',
            fiscal_nds='nds_18',
            clear_delay=datetime.timedelta(days=1),
        )

        self.payment_processor = PaymentProcessor(
            bonus_processor=self.bonus_payment_processor,
            card_processor=self.card_payment_processor,
        )

        self.order_payment_processor = OrderPaymentProcessor(
            payment_processor=self.payment_processor,
            chunked_payment_amount=1,
            push_client=MagicMock(),
        )
        self.order_debt_manager = OrderDebtManager(
            order_payment_processor=self.order_payment_processor,
            debt_order_termination_threshold=0,
            debt_order_termination_delay=0,
        )
        self.order_operation_notifier_mock = MagicMock()

        self.order_manager = OrderManager(
            trust_client=self.trust_client,
            order_payment_processor=self.order_payment_processor,
            order_debt_manager=self.order_debt_manager,
            preliminary_payments_manager=OrderPreliminaryPaymentsManager.from_settings(),
            order_operation_notifier=self.order_operation_notifier_mock,
            push_client=MagicMock(),
        )

        self.addCleanup(self.trust_client.clear)

    def create_fix_tariff(self, cost):
        return OrderItemTariff(
            type=OrderItemTariff.Type.FIX.value,
            fix_params=FixOrderItemTariffParams.objects.create(
                cost=cost,
            ),
        )

    def create_fix_order(self, item_cost, n_items=1, complete=True):
        tariff = self.create_fix_tariff(cost=item_cost)
        order = self.create_single_item_order(tariff=tariff, complete=False)

        for _ in range(1, n_items):
            item_request = self.create_reservation_paid_order_item_request(tariff=tariff)
            self.order_manager.add_order_item(
                order=order,
                order_item_request=item_request,
            )

        if complete:
            self.complete_order(order)

        return order

    def create_per_minute_order(self, minutes, cost_per_minute):
        tariff = OrderItemTariff(
            type=OrderItemTariff.Type.PER_MINUTE.value,
            per_minute_params=PerMinuteOrderItemTariffParams.objects.create(
                cost_per_minute=cost_per_minute,
            ),
        )
        order = self.create_single_item_order(tariff=tariff)
        self.order_manager.force_complete_order(order)

        item = order.items.get()
        item.finished_at = item.finished_at + datetime.timedelta(minutes=minutes)
        item.save()

        return order

    def create_single_item_order(self, tariff=None, started_at=None, complete=False):
        if tariff is None:
            tariff = self.create_fix_tariff(cost=1)

        order_request = OrderRequest(
            user=self.user,
            payment_method=OrderPaymentMethodRequest(
                type_=OrderPaymentMethod.Type.CARD,
                card_paymethod_id='card-1234',
            ),
            items=[self.create_reservation_order_item_request(tariff=tariff)],
        )
        order = self.order_manager.create_order(order_request).order

        if started_at:
            order.created_at = started_at
            order.save()

            item = order.items.get()
            item.started_at = started_at
            item.save()

        if complete:
            order = self.complete_order(order)

        return order

    def create_multiple_items_order(self, item_requests, complete=False):
        order_request = OrderRequest(
            user=self.user,
            payment_method=OrderPaymentMethodRequest(
                type_=OrderPaymentMethod.Type.CARD,
                card_paymethod_id='card-1234',
            ),
            items=item_requests,
        )
        order = self.order_manager.create_order(order_request).order
        if complete:
            self.order_manager.force_complete_order(order)
        return order

    def create_reservation_order_item_request(self, tariff):
        return OrderItemRequest(
            user=self.user,
            item_type=OrderItem.Type.CARSHARING_RESERVATION,
            impl=CarsharingReservationOrderItemRequest(
                user=self.user,
                car_id=self.car.id,
                max_duration_seconds=100,
            ),
            tariff=tariff,
        )

    def create_reservation_paid_order_item_request(self, tariff):
        return OrderItemRequest(
            user=self.user,
            item_type=OrderItem.Type.CARSHARING_RESERVATION_PAID,
            impl=CarsharingReservationPaidOrderItemRequest(
                user=self.user,
                car_id=self.car.id,
            ),
            tariff=tariff,
        )

    def complete_order(self, order):
        result = self.order_manager.send_action(
            order_item=order.get_sorted_items()[-1],
            action='finish',
            context=None,
        )
        order.refresh_from_db()
        return result

    def assert_all_payments_made(self, order):
        cost = self.order_payment_processor.get_order_cost(order)
        payments = [
            x.card_payment for x in OrderItemPayment.objects.filter(order_item__order=order)
        ]
        paid_amount = sum([x.amount for x in payments])
        self.assertEqual(cost, paid_amount)

    def assert_order_payment_status_equal(self, order, payment_status):
        order.refresh_from_db()
        self.assertIs(order.get_payment_status(), payment_status)

    def assert_user_status(self, status, user=None):
        if user is None:
            user = self.user
        user.refresh_from_db()
        self.assertIs(user.get_status(), status)
