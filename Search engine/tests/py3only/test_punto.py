from search.martylib.punto import correct_keyboard_layout
from search.martylib.test_utils import TestCase


class TestPunto(TestCase):
    def test_translation(self):
        # noinspection SpellCheckingInspection
        cases = (
            ('дщдцрфе', 'lolwhat'),
            ('дщдwhat', 'lolwhat'),
            ('lolwhat', 'lolwhat'),
            ('', ''),
            ('LOLЦРФЕ', 'lolwhat'),
        )

        for i, o in cases:
            self.assertEqual(correct_keyboard_layout(i), o)
