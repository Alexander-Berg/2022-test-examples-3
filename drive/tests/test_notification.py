import io

from django.urls import reverse

from cars.carsharing.factories import CarFactory
from cars.users.factories.user import UserFactory
from .base import AdminAPITestCase
from ..views.notification import SendEvacuationNotificationView


class EvacuationNotificationTestCase(AdminAPITestCase):

    def setUp(self):
        super().setUp()
        self.email = 'foo@bar.com'
        self.STS = '424242'
        self.car = CarFactory.create(
            registration_id=self.STS,
        )
        self.user = UserFactory.create(
            email=self.email,
        )

    @property
    def evacuation_url(self):
        return reverse('cars-admin:send-evacuation-notification', kwargs={
            'user_id': self.user.id
        })

    @property
    def inner_fine_url(self):
        return reverse('cars-admin:send-inner-fine-notification', kwargs={
            'user_id': self.user.id
        })

    def test_convert_data(self):
        convert = SendEvacuationNotificationView._convert_date
        self.assertEqual(
            convert('2018-01-01 10:10'),
            '1 января в 10:10',
        )

    def test_send_evacuation_notification(self):
        sum_to_pay = 500
        receipt_number = 1111
        response = self.client.post(
            self.evacuation_url,
            {
                'evacuation_date': '2018-01-01 10:10',
                'car_number': self.car.number,
                'sum_to_pay': sum_to_pay,
                'receipt_number': receipt_number,
            }
        )
        self.assertEqual(response.status_code, 200)

    def test_send_inner_fine_notification(self):
        sum_to_pay = 500
        response = self.client.post(
            self.inner_fine_url,
            {
                'violation_date': '2018-01-01 10:10',
                'paragraph': '1.12',
                'car_number': self.car.number,
                'sum_to_pay': sum_to_pay,
            },
            files={'img.jpg': io.BytesIO(b'binary data')},
        )
        self.assertEqual(response.status_code, 200)
