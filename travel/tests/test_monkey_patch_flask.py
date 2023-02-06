# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import flask

from travel.rasp.train_api.monkey_patch_flask import _DummyLogger


def test_flask_version():
    assert flask.__version__ == '1.1.4', 'Версия flask изменилась, проверьте соответствие функций monkey_patch'


def test_dummy_logger():
    log = _DummyLogger()
    log.error('fffff', 141)
