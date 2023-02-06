from django.test import TestCase

from cars.users.factories.user import UserFactory


class BaseOrderItemManagerTestCase(TestCase):

    def setUp(self):
        self.user = UserFactory.create(uid=1)
