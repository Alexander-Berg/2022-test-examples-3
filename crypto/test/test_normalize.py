# -*- encoding: utf-8 -*-

from crypta.lib.python.text import normalize


def test_basic():
    phrase = 'Съешь ещё этих мягких французских булок, да выпей же чаю'
    expected = 'булка выпь да еще же мягкий съедать французский чаю этот'
    assert normalize(phrase) == expected
