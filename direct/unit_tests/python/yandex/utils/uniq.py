import unittest
from yandex.tap import tap_run

from yandex.utils import uniq

class UniqTestCase(unittest.TestCase):
    def test_uniq(self):
        """ test description """
        self.assertEqual(uniq([]), [], 'empty array')
        self.assertEqual(uniq([None]), [None], 'none array')
        self.assertEqual(uniq([None, None]), [None], 'none array')
        self.assertEqual(uniq([1,2,3]), [1,2,3], 'uniq_array')
        self.assertEqual(uniq([1,1,2,3]), [1,2,3], 'non_uniq_array')
        self.assertEqual(uniq([1,2,3,3]), [1,2,3], 'non_uniq_array')
        self.assertEqual(uniq([1,3,5,5]), [1,3,5], 'non_uniq_array')

if __name__ == '__main__':
    tap_run(tests=1)


