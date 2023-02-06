# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import warnings

import pytest


def pytest_configure(config):
    # register markers
    config.addinivalue_line('markers', 'test_deprecated_stuff: Пропускаем тест в строгом режиме запуска')


def pytest_addoption(parser):
    group = parser.getgroup('rasp')
    group.addoption('--rasp-strict', action='store_true',
                    dest='raise_exception_on_rasp_warnings', default=False,
                    help='Кидать исключения для RaspDeprecationWarning')


@pytest.hookimpl(hookwrapper=True)
def pytest_runtest_call(item):
    if not item.config.option.raise_exception_on_rasp_warnings:
        yield
        return

    if 'test_deprecated_stuff' in item.keywords:
        yield
        return

    with warnings.catch_warnings():
        from travel.rasp.library.python.common23.utils.warnings import RaspDeprecationWarning
        warnings.filterwarnings("error", category=RaspDeprecationWarning)
        yield
