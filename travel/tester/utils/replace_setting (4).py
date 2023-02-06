# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from functools import partial

from django.conf import settings

from travel.rasp.library.python.common23.utils.code_utils import ContextManagerAsDecorator


class ReplaceAttr(ContextManagerAsDecorator):
    """
    Подменяет атрибут на объекте на заданное значение.
    Можно использовать как декоратор функции либо как context manager.

    Например, для settings:

    replace_setting = partial(ReplaceAttr, obj=settings)

    @replace_setting(setting, value)
    def foo():
        pass

    with replace_setting(setting, value) as new_value:
        print(new_value)
    """

    def __init__(self, attr, value, obj):
        self.attr = attr
        self.value = value
        self.obj = obj
        self.old_value = None
        self.defined = False

    def __enter__(self):
        try:
            self.old_value = getattr(self.obj, self.attr)
        except AttributeError:
            self.defined = False
        else:
            self.defined = True

        setattr(self.obj, self.attr, self.value)

        return self.value

    def __exit__(self, exc_type, exc_val, exc_tb):
        if self.defined:
            setattr(self.obj, self.attr, self.old_value)
        else:
            delattr(self.obj, self.attr)


class ReplaceKey(ContextManagerAsDecorator):
    """
    Подменяет ключ объекта на заданное значение.
    Можно использовать как декоратор функции либо как context manager.
    """

    def __init__(self, obj, key, value):
        self.key = key
        self.value = value
        self.obj = obj
        self.old_value = None
        self.defined = False

    def __enter__(self):
        try:
            self.old_value = self.obj[self.key]
        except KeyError:
            self.defined = False
        else:
            self.defined = True

        self.obj[self.key] = self.value

        return self.value

    def __exit__(self, exc_type, exc_val, exc_tb):
        if self.defined:
            self.obj[self.key] = self.old_value
        else:
            del self.obj[self.key]


replace_setting = partial(ReplaceAttr, obj=settings)
