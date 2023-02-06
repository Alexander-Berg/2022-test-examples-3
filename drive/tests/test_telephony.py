from django.urls import reverse

from cars.callcenter.models.call_assignment import CallAssignment
from cars.users.factories.user import UserFactory
from .base import AdminAPITestCase


class RegisterCallTestCase(AdminAPITestCase):

    @property
    def start_call_url(self):
        return reverse('cars-admin:register-call', kwargs={'action': 'start'})

    @property
    def finish_call_url(self):
        return reverse('cars-admin:register-call', kwargs={'action': 'finish'})

    def setUp(self):
        super().setUp()
        self.calling_user_number = '+79991234567'
        self.operator_user_number = '7123'
        self.calling_user = UserFactory.create(
            phone=self.calling_user_number
        )

    # def test_start_call_ok(self):
    #    response = self.client.get(
    #        self.start_call_url,
    #        {
    #            'from_number': self.calling_user_number,
    #            'to_number': self.operator_user_number,
    #        },
    #    )
    #    self.assert_response_ok(response)
    #    self.assertEqual(CallAssignment.objects.count(), 1)
    #    ca = CallAssignment.objects.first()
    #    self.assertEqual(ca.from_number, self.calling_user_number)
    #    self.assertEqual(ca.from_user, self.calling_user)
        # self.assertEqual(ca.to_number, self.operator_user_number)

    # def test_finish_call_ok(self):
    #     phone1 = '+79991234567'
    #     phone2 = '7123'

    #     response = self.client.get(
    #         self.finish_call_url,
    #         {
    #             'from_number': phone1,
    #             'to_number': phone2,
    #         },
    #     )
    #     self.assert_response_ok(response)
