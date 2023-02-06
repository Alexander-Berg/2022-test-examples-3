# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from copy import copy

from django.db import models
from django.utils.encoding import force_text


factories = {}
_empty_object_key = object()
_empty_object = object()


class BaseFactory(object):
    default_kwargs = {}

    def __init__(self, update_default_kwargs=None, set_default_kwargs=None):
        if set_default_kwargs:
            self.default_kwargs = set_default_kwargs
        if update_default_kwargs:
            self.default_kwargs = self.extend_kwargs(update_default_kwargs)

        self.key_processors = (
            self.process_key_factory,
            self.process_key_dict,
            self.process_none,
        )

    def __call__(self, __object_key=_empty_object_key, **kwargs):
        if __object_key is not _empty_object_key:
            for key_processor in self.key_processors:
                obj = key_processor(__object_key, kwargs)
                if obj is _empty_object:
                    return None
                elif obj is not None:
                    return obj

            else:
                raise Exception("Can not process object_key {!r}".format(force_text(__object_key)))

        try:
            kwargs = self.extend_kwargs(kwargs, resolve_callable=True)
            return self.create_object(kwargs)
        except Exception:
            try:
                print('Cannot create object in factory {}: {!r}'.format(self.__class__.__name__, force_text(kwargs)))
            except Exception:
                print('Cannot create object in factory {}'.format(self.__class__.__name__))
            raise

    def extend_kwargs(self, kwargs, resolve_callable=False):
        new_kwargs = copy(self.default_kwargs)
        new_kwargs.update(kwargs)

        if resolve_callable:
            for key, value in new_kwargs.items():
                if callable(value):
                    new_kwargs[key] = value()

        return new_kwargs

    def create_object(self, kwargs):
        raise NotImplementedError()

    def mutate(self, default_kwargs=None, **kwargs):
        return self.__class__(self.extend_kwargs(kwargs), set_default_kwargs=default_kwargs)

    def process_key_factory(self, key, kwargs):
        if isinstance(key, BaseFactory):
            kwargs = self.extend_kwargs(kwargs)
            return key(**kwargs)

    def process_key_dict(self, key, kwargs):
        if isinstance(key, dict):
            if kwargs:
                raise ValueError('Provide params via dict or kwargs')

            return self(**key)

    def process_none(self, key, kwargs):
        if key is None:
            return _empty_object


class ModelFactory(BaseFactory):
    Model = None

    def __init__(self, update_default_kwargs=None, set_default_kwargs=None):
        super(ModelFactory, self).__init__(
            update_default_kwargs=update_default_kwargs,
            set_default_kwargs=set_default_kwargs,
        )
        self.key_processors = (self.process_key_pk, self.process_key_model) + self.key_processors

    def process_key_pk(self, key, kwargs):
        if isinstance(key, int):
            return self.Model.objects.get(pk=key)

    def process_key_model(self, key, kwargs):
        if isinstance(key, self.Model):
            return key

    def create_object(self, kwargs):
        create_kwargs = {}

        for field in self.Model._meta.fields:
            if field.name not in kwargs:
                continue

            if isinstance(field, models.ForeignKey):
                factory = get_model_factory(field.rel.to)
                key = kwargs.pop(field.name)
                if key is not None:
                    create_kwargs[field.name] = factory(key)
            else:
                create_kwargs[field.name] = kwargs.pop(field.name)

        create_kwargs.update(kwargs)

        return self.Model.objects.create(**create_kwargs)


def get_model_factory(model_class):
    if model_class in factories:
        return factories[model_class]
    else:
        class TmpFactory(ModelFactory):
            Model = model_class

        return TmpFactory()
