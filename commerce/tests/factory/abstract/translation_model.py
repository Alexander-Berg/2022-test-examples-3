import factory

from modeltranslation.utils import auto_populate


class TranslationModelFactory(factory.DjangoModelFactory):
    class Meta:
        abstract = True

    need_populate = True

    @classmethod
    def _create(cls, model_class, *args, **kwargs):
        need_populate = kwargs.pop('need_populate')

        with auto_populate(need_populate):
            obj = super()._create(model_class, *args, **kwargs)

        return obj
