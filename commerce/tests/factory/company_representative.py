import factory

from commerce.adv_backend.backend.models import CompanyRepresentative


class CompanyRepresentativeFactory(factory.DjangoModelFactory):
    class Meta:
        model = CompanyRepresentative

    company = factory.SubFactory('commerce.adv_backend.tests.factory.CompanyFactory')
    representative = factory.SubFactory('commerce.adv_backend.tests.factory.ExternalUser')

    is_main = False
