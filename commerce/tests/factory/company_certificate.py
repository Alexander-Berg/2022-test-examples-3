import factory
from factory import fuzzy

from commerce.adv_backend.backend.models import CompanyCertificate

from commerce.adv_backend.tests.factory.abstract import TranslationModelFactory
from commerce.adv_backend.tests.factory.data import COMPANY_CERTIFICATE_CODES


class CompanyCertificateFactory(TranslationModelFactory):
    class Meta:
        model = CompanyCertificate
        django_get_or_create = ('code',)

    is_regional = False

    code = fuzzy.FuzzyChoice(COMPANY_CERTIFICATE_CODES)
    preposition = 'by'
    name = factory.lazy_attribute(lambda obj: obj.code.capitalize())
    name_prepositional = factory.lazy_attribute(lambda obj: obj.code.capitalize())
