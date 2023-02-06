import factory

from commerce.adv_backend.backend.models import ExternalUser


class ExternalUserFactory(factory.DjangoModelFactory):
    class Meta:
        model = ExternalUser
        django_get_or_create = ('username',)

    username = factory.Faker('user_name')
