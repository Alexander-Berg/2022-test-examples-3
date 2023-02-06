from unittest import TestCase

from extsearch.video.robot.speech2text.tools import TAligner, TRecognizedWord


class TestAligner(TestCase):
    a = [
        TRecognizedWord('a', 0, 1),
        TRecognizedWord('a', 1, 2),
        TRecognizedWord('b', 2, 3),
        TRecognizedWord('b', 3, 4),
        TRecognizedWord('c', 4, 5),
    ]
    b = [
        TRecognizedWord('b', 3, 4),
        TRecognizedWord('b', 4, 5),
        TRecognizedWord('d', 5, 6),
        TRecognizedWord('d', 6, 7),
        TRecognizedWord('e', 7, 8),
    ]
    c = [
        TRecognizedWord('x', 3, 4),
        TRecognizedWord('x', 4, 5),
        TRecognizedWord('x', 5, 6),
        TRecognizedWord('x', 6, 7),
        TRecognizedWord('x', 7, 8),
    ]
    aligner = TAligner(5, 3)

    def test_aligner(self):
        self.assertEqual(self.aligner(self.a, 0, self.b, 3), self.a[:-1] + self.b[1:])
        self.assertEqual(self.aligner(self.a, 0, [], 3), self.a)
        self.assertEqual(self.aligner([], 0, self.b, 3), self.b[2:])

        self.assertEqual(self.aligner(self.a, 0, self.c, 3), self.a + self.c[2:])
