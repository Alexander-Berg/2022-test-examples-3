import factory
from factory import fuzzy

from commerce.adv_backend.backend.models import Region

from commerce.adv_backend.tests.factory.abstract import TranslationModelFactory
from commerce.adv_backend.tests.factory.data import REGIONS


class RegionFactory(TranslationModelFactory):
    class Meta:
        model = Region
        django_get_or_create = ('geo_id',)

    geo_id = factory.lazy_attribute(lambda obj: REGIONS[obj.name])
    name = fuzzy.FuzzyChoice(list(REGIONS.keys()))
