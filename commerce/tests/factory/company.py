import factory

from django.conf import settings

from commerce.adv_backend.backend.models import Company

from commerce.adv_backend.tests.factory.abstract import ModerationModelFactory, TranslationModelFactory


class CompanyFactory(TranslationModelFactory, ModerationModelFactory):
    class Meta:
        model = Company

    name = factory.Faker('company', locale=settings.FAKER_LANGUAGE)
    logo = 'https://avatars.mds.yandex.net/get-adv/50995/2a00000168b8792298a697251c015e3d23e9/orig'
    direct_budget = None
    site = factory.Sequence(lambda n: f'https://умный-маркетинг-{n}.рф')
    description = factory.Faker('text', max_nb_chars=200, ext_word_list=None, locale=settings.FAKER_LANGUAGE)
    materials = []

    proposed_by = ''
    region = factory.SubFactory('commerce.adv_backend.tests.factory.RegionFactory')
    is_partner = False
    is_active = True

    @factory.post_generation
    def certificates(self, create, certificates, **kwargs):
        if not create:
            return

        if certificates:
            self.certificates.add(*certificates)

    @factory.post_generation
    def tld(self, create, tld, **kwargs):
        if not create:
            return

        if tld:
            self.tld.add(*tld)

    @factory.post_generation
    def representatives(self, create, representatives, **kwargs):
        if not create:
            return

        if representatives:
            self.representatives.add(*representatives)
