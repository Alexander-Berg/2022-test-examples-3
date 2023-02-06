import unittest
import extsearch.audio.generative.py.common.mus as mus


class TestMus(unittest.TestCase):
    def test_interval(self):
        self.assertEqual(1, mus.interval('B', 'C'))
        self.assertEqual(11, mus.interval('C', 'B'))
        self.assertEqual(9, mus.interval('C', 'A'))

    def test_parse_major(self):
        chords = mus.progression_to_chords(('I', 'II', 'III', 'IV', 'V', 'VI', 'VII'), 'C')
        self.assertEqual(
            (('c', 'e', 'g'), ('d', 'f#', 'a'), ('e', 'g#', 'b'), ('f', 'a', 'c'), ('g', 'b', 'd'), ('a', 'c#', 'e'),
             ('b', 'd#', 'f#')),
            chords
        )

    def test_parse_major_in_minor_key(self):
        chords = mus.progression_to_chords(('I', 'II', 'III', 'IV', 'V', 'VI', 'VII'), 'e')
        self.assertEqual(
            (('e', 'g#', 'b'), ('f#', 'a#', 'c#'), ('g', 'b', 'd'), ('a', 'c#', 'e'), ('b', 'd#', 'f#'),
             ('c', 'e', 'g'), ('d', 'f#', 'a')),
            chords
        )

    def test_parse_flat_major(self):
        chords = mus.progression_to_chords(('bI', 'bII', 'bIII', 'bIV', 'bV', 'bVI', 'bVII'), 'C')
        self.assertEqual(
            (('b', 'd#', 'f#'), ('c#', 'f', 'g#'), ('d#', 'g', 'a#'), ('e', 'g#', 'b'), ('f#', 'a#', 'c#'),
             ('g#', 'c', 'd#'), ('a#', 'd', 'f')),
            chords
        )

    def test_parse_minor(self):
        chords = mus.progression_to_chords(('i', 'ii', 'iii', 'iv', 'v', 'vi', 'vii'), 'C')
        self.assertEqual(
            (('c', 'd#', 'g'), ('d', 'f', 'a'), ('e', 'g', 'b'), ('f', 'g#', 'c'), ('g', 'a#', 'd'), ('a', 'c', 'e'),
             ('b', 'd', 'f#')),
            chords
        )

    def test_parse_flat_minor(self):
        chords = mus.progression_to_chords(('bi', 'bii', 'biii', 'biv', 'bv', 'bvi', 'bvii'), 'C')
        self.assertEqual(
            (('b', 'd', 'f#'), ('c#', 'e', 'g#'), ('d#', 'f#', 'a#'), ('e', 'g', 'b'), ('f#', 'a', 'c#'),
             ('g#', 'b', 'd#'), ('a#', 'c#', 'f')),
            chords
        )

    def test_parse_sus(self):
        chords = mus.progression_to_chords(('Isus2', 'Isus4'), 'C')
        self.assertEqual((('c', 'd', 'g'), ('c', 'f', 'g')), chords)

    def test_parse_aug_dim(self):
        chords = mus.progression_to_chords(('Iaug', 'Idim'), 'C')
        self.assertEqual((('c', 'e', 'g#'), ('c', 'd#', 'f#')), chords)

    def test_parse_7_maj7(self):
        chords = mus.progression_to_chords(('I7', 'Imaj7'), 'C')
        self.assertEqual((('c', 'e', 'g', 'a#'), ('c', 'e', 'g', 'b')), chords)

    def test_get_allowed_notes_centered_at(self):
        result = mus.get_allowed_notes_centered_at(60, 9, ['c', 'e', 'g'])
        self.assertEqual([43, 48, 52, 55, 60, 64, 67, 72, 76], result)

    def test_pentatonic(self):
        self.assertEqual(['g', 'a', 'b', 'd', 'e'], mus.get_pentatonic_notes('G', False))
        self.assertEqual(['d', 'e', 'f', 'a', 'b'], mus.get_pentatonic_notes('D', True))
        self.assertEqual(['f', 'g', 'a', 'c', 'd'], mus.get_pentatonic_notes('F', False))

    def test_flat_sharp(self):
        chords = mus.progression_to_chords(('#I', 'bI'), 'e')
        self.assertEqual((('f', 'a', 'c'), ('d#', 'g', 'a#')), chords)

    def test_print(self):
        chords = mus.progression_to_chords('Isus2-Isus2-bVI-bVI-i-i-bIII-bIII-iv-i-bVII-bVII'.split('-'), 'e')
        # self.assertEqual((), chords)
        print(chords)


if __name__ == '__main__':
    unittest.main()
