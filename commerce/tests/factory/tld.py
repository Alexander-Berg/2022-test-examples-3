import factory
from factory import fuzzy

from django.conf import settings

from commerce.adv_backend.backend.models import Tld


class TldFactory(factory.DjangoModelFactory):
    class Meta:
        model = Tld
        django_get_or_create = ('value',)

    value = fuzzy.FuzzyChoice(list(settings.TLD_TO_DB_LANG.keys()))
