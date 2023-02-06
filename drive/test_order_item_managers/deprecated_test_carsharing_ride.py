import decimal
from unittest.mock import patch

from cars.carsharing.factories.car import CarFactory
from cars.core.saas_drive import SaasDriveStub
from ...iface.order_item_request import IOrderItemRequestImpl
from ...core.order_item_managers.carsharing_ride import CarsharingRideManager
from ...core.order_item_requests.carsharing import CarsharingRideOrderItemRequest
from ...core.order_request import OrderItemRequestContext
from ...models.order_item_tariff import OrderItemTariff
from .base import BaseOrderItemManagerTestCase


class CarsharingRideOrderItemManagerTestCase(BaseOrderItemManagerTestCase):

    def setUp(self):
        super().setUp()
        self.car = CarFactory.create()
        self.mgr_class = CarsharingRideManager
        self.saas_drive = SaasDriveStub()

    def test_fix_tariff_offer_missing(self):
        with patch.object(self.mgr_class, '_saas_drive_client', self.saas_drive):
            with self.assertRaises(IOrderItemRequestImpl.Error):
                self.mgr_class.pick_from_order_item_request(
                    user=self.user,
                    request_impl=CarsharingRideOrderItemRequest(
                        user=self.user,
                        car_id=self.car.id,
                        fix_id='777',
                    ),
                    context=OrderItemRequestContext(
                        oauth_token=None,
                    ),
                )

    def test_fix_tariff(self):
        fix_price = decimal.Decimal('123.45')
        self.saas_drive.create_offer(
            oid='777',
            offer=self.saas_drive.build_offer_object(fix_price=fix_price),
        )

        with patch.object(self.mgr_class, '_saas_drive_client', self.saas_drive):
            tariff = self.mgr_class.pick_from_order_item_request(
                user=self.user,
                request_impl=CarsharingRideOrderItemRequest(
                    user=self.user,
                    car_id=self.car.id,
                    fix_id='777',
                ),
                context=OrderItemRequestContext(
                    oauth_token=None,
                ),
            )

        self.assertIs(tariff.get_type(), OrderItemTariff.Type.FIX)
        self.assertEqual(tariff.fix_params.cost, fix_price)
