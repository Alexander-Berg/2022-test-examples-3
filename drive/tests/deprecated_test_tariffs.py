import datetime

from django.test import TransactionTestCase
from django.utils import timezone
from factory import SubFactory

from cars.carsharing.factories.reservation import CarsharingReservationFactory
from ..factories.order_item import OrderItemFactory
from ..models import OrderItem, OrderItemTariff
from ..models.order_item_tariff import FixOrderItemTariffParams, PerMinuteOrderItemTariffParams
from .mixins import OrderTestCaseMixin


class FixTariffTestCase(OrderTestCaseMixin, TransactionTestCase):

    def test_simple(self):
        item = OrderItemFactory.create(
            tariff=OrderItemTariff.objects.create(
                type=OrderItemTariff.Type.FIX.value,
                fix_params=FixOrderItemTariffParams.objects.create(
                    cost=123.45,
                ),
            ),
            type=OrderItem.Type.CARSHARING_RESERVATION.value,
            carsharing_reservation=SubFactory(CarsharingReservationFactory),
            finished_at=timezone.now(),
        )
        cost = self.order_payment_processor.get_order_item_cost(item)
        self.assertEqual(cost, 123.45)


class PerMinuteTariffTestCase(OrderTestCaseMixin, TestCase):

    def test_simple(self):
        minute_ago = timezone.now() - datetime.timedelta(minutes=1)
        item = OrderItemFactory.create(
            tariff=OrderItemTariff.objects.create(
                type=OrderItemTariff.Type.PER_MINUTE.value,
                per_minute_params=PerMinuteOrderItemTariffParams.objects.create(
                    cost_per_minute=100,
                ),
            ),
            started_at=minute_ago,
            finished_at=timezone.now(),
            order__created_at=minute_ago,
            type=OrderItem.Type.CARSHARING_RESERVATION.value,
            carsharing_reservation=SubFactory(CarsharingReservationFactory),
        )
        cost = self.order_payment_processor.get_order_item_cost(item)
        self.assertAlmostEqual(cost, 100, delta=0.1)
