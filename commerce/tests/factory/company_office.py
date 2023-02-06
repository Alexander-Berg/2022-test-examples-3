import factory
from factory import fuzzy

from django.conf import settings

from commerce.adv_backend.backend.models import CompanyOffice
from commerce.adv_backend.backend.models.company_office import MAP_CHOICES

from commerce.adv_backend.tests.factory.abstract import ModerationModelFactory, TranslationModelFactory


class CompanyOfficeFactory(TranslationModelFactory, ModerationModelFactory):
    class Meta:
        model = CompanyOffice

    is_main = False
    company = factory.SubFactory('commerce.adv_backend.tests.factory.CompanyFactory')

    proposed_by = ''
    address = factory.Faker('address', locale=settings.FAKER_LANGUAGE)
    city = factory.SubFactory('commerce.adv_backend.tests.factory.CityFactory')
    latitude = factory.Faker('latitude', locale=settings.FAKER_LANGUAGE)
    longitude = factory.Faker('longitude', locale=settings.FAKER_LANGUAGE)
    map = fuzzy.FuzzyChoice([item[0] for item in MAP_CHOICES])
    map_zoom = fuzzy.FuzzyInteger(0, 15, step=3)
    phone = factory.Faker('phone_number', locale=settings.FAKER_LANGUAGE)
