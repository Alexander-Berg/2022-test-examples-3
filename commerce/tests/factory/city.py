import factory
from factory import fuzzy

from commerce.adv_backend.backend.models import City

from commerce.adv_backend.tests.factory.abstract import TranslationModelFactory
from commerce.adv_backend.tests.factory.data import CITIES


class CityFactory(TranslationModelFactory):
    class Meta:
        model = City
        django_get_or_create = ('geo_id',)

    geo_id = factory.lazy_attribute(lambda obj: CITIES[obj.name])
    name = fuzzy.FuzzyChoice(list(CITIES.keys()))
    preposition = 'in'
    name_prepositional = factory.lazy_attribute(lambda obj: obj.name)
    display_priority = 0
    country = factory.SubFactory('commerce.adv_backend.tests.factory.CountryFactory')
