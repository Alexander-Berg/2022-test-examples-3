#!/usr/bin/python
# -*- coding: utf-8 -*-

import unittest

from market.pylibrary.shopsutil import transliterate


class TestTransliterate(unittest.TestCase):
    def test_to_latin(self):
        # every cyrillic letter appears at least in one word
        translite_to_latin = {
            transliterate.u('фельдъегерь'): 'feldeger',
            transliterate.u('гёмбёц'): 'gyombyots',
            transliterate.u('йох'): 'yoh',
            transliterate.u('пращур'): 'praschur',
            transliterate.u('казинет'): 'kazinet',
            transliterate.u('экосез'): 'ekosez',
            transliterate.u('живчик'): 'zhivchik',
            transliterate.u('шея'): 'sheya',
            transliterate.u('ют'): 'yut',
            transliterate.u('тын'): 'tyn',
        }

        for transilterate_from, transilterate_to in list(translite_to_latin.items()):
            self.assertEqual(transilterate_to, transliterate.transliterate_to_latin(transilterate_from))

    def test_to_cyrillic(self):
        translite_to_cyrillic = {
            # every latin letter appears at least in one word
            'professor': transliterate.u('профессор'),
            'dominant': transliterate.u('доминант'),
            'boy': transliterate.u('бой'),
            'quiz': transliterate.u('куиз'),
            'wave': transliterate.u('ваве'),
            'complex': transliterate.u('комплекс'),
            'joke': transliterate.u('джоке'),
            'huge': transliterate.u('хуге'),

            # combinations
            'borsch': transliterate.u('борщ'),
            'church': transliterate.u('чурч'),
            'yard': transliterate.u('ярд'),
            'your': transliterate.u('ёур'),
            'wish': transliterate.u('виш'),
            'zhyuts': transliterate.u('жюц'),
        }

        for transilterate_from, transilterate_to in list(translite_to_cyrillic.items()):
            self.assertEqual(transilterate_to, transliterate.transliterate_to_cyrillic(transilterate_from))


if __name__ == '__main__':
    unittest.main()
