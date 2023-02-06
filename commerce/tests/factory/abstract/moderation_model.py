import factory


class ModerationModelFactory(factory.DjangoModelFactory):
    class Meta:
        abstract = True

    need_approve = True

    @classmethod
    def _create(cls, model_class, *args, **kwargs):
        need_approve = kwargs.pop('need_approve')

        obj = super()._create(model_class, *args, **kwargs)

        if hasattr(obj, 'moderated_object') and need_approve:
            obj.moderated_object.approve()

        return obj
