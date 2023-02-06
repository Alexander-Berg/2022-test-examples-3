from cars.calculator.tests.helper import CalculatorTestHelper
from cars.django.tests import CarsharingAPITransactionTestCase


class DriveAPITestCase(CarsharingAPITransactionTestCase):

    def setUp(self):
        super().setUp()
        self.ch = CalculatorTestHelper(tc=self)
        self.ch.setUp()
