from unittest import TestCase

from lib.utils import tsum


class TestTsum(TestCase):
    def test_remove_archive_name(self):
        self.assertEqual(
            tsum._remove_archive_name('test-app-1.53/lib/slf4j-api-1.7.22.jar'),
            'lib/slf4j-api-1.7.22.jar'
        )

        self.assertEqual(
            tsum._remove_archive_name('test-app-1.53/some-file-in-root.txt'),
            'some-file-in-root.txt'
        )
