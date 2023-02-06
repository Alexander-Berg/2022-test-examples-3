import factory

from commerce.adv_backend.backend.models import UserRole


class UserRoleFactory(factory.DjangoModelFactory):
    class Meta:
        model = UserRole

    user = factory.SubFactory('commerce.adv_backend.tests.factory.UserFactory')
    group = 'common'
    role = 'superuser'
