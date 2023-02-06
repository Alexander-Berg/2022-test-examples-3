# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function


# определены тут, т.к. alias getter использует при настройке импорт из файлов (django-style)
def get_alias():
    return 'alias42'


def get_alias_to_default():
    return 'default'
