from cars.django.tests import CarsharingAPITransactionTestCase
from cars.users.factories.user import UserFactory


class AdminAPITestCase(CarsharingAPITransactionTestCase):

    def setUp(self):
        self.user = UserFactory.create(
            uid=1,
            is_superuser=True,
        )
        self.user_no_permissions = UserFactory.create(
            uid=2,
        )
