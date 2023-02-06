# -*- coding: utf-8 -*-

import unittest
import tempfile

import market.idx.pictures.thumbnails as thumbnails


class TestThumbnails(unittest.TestCase):
    def test_all_available_string(self):
        all_th = thumbnails.get_all_thumbnails()

        if all_th[-1:] != '\n':
            all_th += '\n'

        self.assertEqual(all_th.count('\n'), len(thumbnails._known_thumbnails))
        self.assertNotEqual(all_th.find('900x1200'), -1)
        self.assertNotEqual(all_th.find('1x1'), -1)

    def test_save_all_thumbnails(self):
        tmp_file = tempfile.NamedTemporaryFile()
        thumbnails.save_all_thumbnails(tmp_file.name)

        with open(tmp_file.name) as fn:
            all_thumbnails = fn.read()

        self.assertEqual(thumbnails.get_all_thumbnails(), all_thumbnails)

    def test_iter_thumbnails(self):
        book = tuple(thumbnails.get_type('bookcover'))

        self.assertIn((150, 150), book)
        self.assertEqual(len(book), 4)

        offer = tuple(thumbnails.get_type('offer'))
        self.assertIn((1, 1), offer)

    def test_has_all_thumbnails_from(self):
        book = thumbnails.get_type('bookcover')

        # book.thumbnails = 0x950
        self.assertTrue(book.has_all_thumbnails_from(thumbnails.ThumbnailType(0x140)))
        self.assertFalse(book.has_all_thumbnails_from(thumbnails.ThumbnailType(0x3)))

    def test_calc_new_thumbnails_from(self):
        book = thumbnails.get_type('bookcover')

        # book.thumbnails = 0x950
        # + 10 and +0 bit number thumbnail additionally needed
        self.assertEqual(book.calc_new_thumbnails_from(thumbnails.ThumbnailType(0xC51)).thumbnails, 0x401)

    def test_get_biggest_thumbnail(self):
        th = thumbnails._th
        thumbs = thumbnails.ThumbnailType(th['50x50'] | th['55x70'])
        self.assertEqual(thumbs.get_biggest_thumbnail(), (55, 70))
        thumbs = thumbnails.ThumbnailType(th['50x50'] | th['55x70'] | th['1x1'])
        self.assertEqual(thumbs.get_biggest_thumbnail(), (1, 1))
        thumbs = thumbnails.ThumbnailType(0)
        self.assertEqual(thumbs.get_biggest_thumbnail(), (0, 0))

    def test_calc_size(self):
        cs = thumbnails.calc_size

        self.assertEqual(cs((100, 100), (100, 100)), (100, 100))

        self.assertEqual(cs((1000, 100), (100, 100)), (100, 10))
        self.assertEqual(cs((100, 1000), (100, 100)), (10, 100))
        self.assertEqual(cs((1000, 2000), (100, 100)), (50, 100))
        self.assertEqual(cs((2000, 1000), (100, 100)), (100, 50))

        self.assertEqual(cs((50, 50), (100, 100)), None)
        self.assertEqual(cs((99, 99), (100, 100)), None)

        self.assertEqual(cs((1000, 10), (100, 100)), (100, 1))
        self.assertEqual(cs((1000, 10), (10, 100)), None)
        self.assertEqual(cs((10, 1000), (100, 10)), None)
        self.assertEqual(cs((1000, 1), (10, 10)), None)
        self.assertEqual(cs((1, 1000), (10, 10)), None)

        self.assertEqual(cs((100, 100), (1, 1)), (100, 100))
        self.assertEqual(cs((10000, 1000), (1, 1)), (3500, 350))

if __name__ == '__main__':
    unittest.main()
