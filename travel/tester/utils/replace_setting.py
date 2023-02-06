# -*- coding: utf-8 -*-

from __future__ import absolute_import

import decorator
from django.conf import settings


class ReplaceSetting(object):
    """
    Подменяет нужную настройку на нужное значение.

    Можно использовать как декоратор функции либо как context manager:

    @replace_setting(setting, value)
    def foo():
        pass

    with replace_setting(setting, value) as new_value:
        print new_value
    """

    def __init__(self, setting, value):
        self.setting = setting
        self.value = value
        self.old_value = None
        self.defined = False

    def __enter__(self):
        try:
            self.old_value = getattr(settings, self.setting)
        except AttributeError:
            self.defined = False
        else:
            self.defined = True

        setattr(settings, self.setting, self.value)

        return self.value

    def __exit__(self, exc_type, exc_val, exc_tb):
        if self.defined:
            setattr(settings, self.setting, self.old_value)
        else:
            delattr(settings, self.setting)

    def __call__(self, func, *args, **kwargs):
        def wrapper(func, *args, **kwargs):
            with self:
                return func(*args, **kwargs)

        return decorator.decorate(func, wrapper)


replace_setting = ReplaceSetting
