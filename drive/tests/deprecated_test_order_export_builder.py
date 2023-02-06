from rest_framework.test import APITransactionTestCase

from cars.calculator.tests.helper import CalculatorTestHelper
from ..core.order_export_builder import OrderExportBuilder
from .mixins import OrderTestCaseMixin


class OrderExportBuilderTestCase(OrderTestCaseMixin, APITransactionTestCase):

    def setUp(self):
        super().setUp()

        self.builder = OrderExportBuilder(
            order_payment_processor=self.order_payment_processor,
        )

        self.ch = CalculatorTestHelper(tc=self)
        self.ch.setUp()

    def test_ok(self):
        order1 = self.create_single_item_order(complete=True)
        order2 = self.create_single_item_order(complete=True)
        export = self.builder.build_for_orders([order1, order2])

        self.assertIsNotNone(export)
        self.assertEqual(len(export), 2)

        order_data = export[0]
        self.assertEqual(order_data['id'], str(order1.id))
        self.assertEqual(len(order_data['items']), len(order1.get_sorted_items()))

    def test_not_completed_order(self):
        order1 = self.create_single_item_order(complete=True)
        order2 = self.create_single_item_order(complete=False)
        export = self.builder.build_for_orders([order1, order2])

        self.assertIsNotNone(export)
        self.assertEqual(len(export), 2)

        order_data = export[-1]
        self.assertEqual(order_data['id'], str(order2.id))
        self.assertIsNone(order_data['completed_at'])
