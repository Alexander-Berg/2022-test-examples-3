# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from travel.rasp.library.python.common23.models.texts.icao_transliteration import transliterate


class TestTransliteration(object):
    def test_empty(self):
        assert transliterate('') == ''

    def test_cyr(self):
        assert transliterate('Алёхино, Бобъяково, Роща, Айдарово') == 'Alekhino, Bobieiakovo, Roshcha, Aidarovo'
        translit = transliterate('съешь ещё этих мягких французских булок да выпей чаю')
        for char in translit:
            assert char == ' ' or ('z' >= char >= 'a')
        translit = transliterate("Державний класифікатор об'єктів адміністративно-територіального устрою України")
        for char in translit:
            assert char in " '-" or ('z' >= char >= 'A')

    def test_lat(self):
        assert transliterate('Điện Biên Province') == 'Dien Bien Province'
        assert transliterate('Đồng Tháp Province') == 'Dong Thap Province'
