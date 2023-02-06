from unittest import TestCase

from extsearch.video.robot.speech2text.tools import TRecognizedWord


class TestTRecognizedWord(TestCase):
    def test_init(self):
        foo = TRecognizedWord('foo', 0.1, 0.2)
        self.assertEqual(foo.word, 'foo')
        self.assertEqual(foo.start, 0.1)
        self.assertEqual(foo.end, 0.2)

    def test_eq(self):
        foo = TRecognizedWord('foo', 0.1, 0.2)
        bar = TRecognizedWord('foo', 0.3, 0.5)
        self.assertEqual(foo, 'foo')
        self.assertEqual(foo, bar)

    def test_less(self):
        foo = TRecognizedWord('foo', 1, 3)
        self.assertLess(foo, 4)
        self.assertFalse(foo < 3)
        self.assertLessEqual(foo, 3)

    def test_greater(self):
        foo = TRecognizedWord('foo', 1, 3)
        self.assertGreater(foo, 0)
        self.assertFalse(foo > 1)
        self.assertGreaterEqual(foo, 1)
