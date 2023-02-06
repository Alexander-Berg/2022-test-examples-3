import factory
from factory import fuzzy

from commerce.adv_backend.backend.models import Country

from commerce.adv_backend.tests.factory.abstract import TranslationModelFactory
from commerce.adv_backend.tests.factory.data import COUNTRIES


class CountryFactory(TranslationModelFactory):
    class Meta:
        model = Country
        django_get_or_create = ('geo_id',)

    geo_id = factory.lazy_attribute(lambda obj: COUNTRIES[obj.name])
    name = fuzzy.FuzzyChoice(list(COUNTRIES.keys()))
    preposition = 'in'
    name_prepositional = factory.lazy_attribute(lambda obj: obj.name)
