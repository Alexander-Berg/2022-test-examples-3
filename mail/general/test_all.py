#!/usr/bin/env python
import doctest
import os
import sys

os.environ['DJANGO_SETTINGS_MODULE'] = 'test_settings'

sys.path.append('..')

from django.core.management import call_command


doctest_modules = [
    'mlcore.subscribe.operations.factory',
    'mlcore.subscribe.type_change',
    'mlcore.mailarchive.templatetags.mailarchive_extra',
]


def test_docstrings(modules_list, verbose=0):
    for module_path in modules_list:
        module = __import__(module_path, fromlist=[''])
        result = doctest.testmod(module, verbose=verbose)
        print result


def test_app(appname):
    call_command('test', appname)


if __name__ == '__main__':
    # TODO: doctests -> nose (unit)
    # test_docstrings(doctest_modules)
    test_app('mlcore')
