# -*- encoding: utf-8 -*-

from crypta.lib.python.text import guess_gender


def test_defaultman():
    assert guess_gender('Иван Иванов') == 'm'


def test_defaultwoman():
    assert guess_gender('Анна Иванова') == 'f'
