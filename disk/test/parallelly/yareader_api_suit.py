# -*- coding: utf-8 -*-
from test.base import DiskTestCase


class TestYaReaderApi(DiskTestCase):
    def test_upload_a_book(self):
        """
        Загружаем книгу и проверяем, что она загрузилась
        Предварительно создаем каталог
        """
        opts = {
            'uid': self.uid,
            'path': '/disk/books',
        }
        self.json_ok('mkdir', opts)
        file_data = {
            'mimetype': 'application/zip',
        }
        self.upload_file(self.uid, '/disk/books/book_1.epub', file_data=file_data)
        file_data = {
            'mimetype': 'text/xml',
        }
        self.upload_file(self.uid, '/disk/books/book_2.fb2', file_data=file_data)
        opts = {
            'uid': self.uid,
            'path': '/disk/books',
        }
        listing = self.json_ok('list', opts)
        self.assertEqual(len(listing), 3)

    def test_filter_books_while_listing(self):
        '''
        Проверка фильтра по книгам при листинге
        Загружаем книгу, а потом загружаем нечто, и убеждаемся, что оно отфильтровалось
        '''
        opts = {
            'uid': self.uid,
            'path': '/disk/books',
        }
        self.json_ok('mkdir', opts)
        file_data = {
            'mimetype': 'application/zip',
        }
        self.upload_file(self.uid, '/disk/books/book_1.epub', file_data=file_data)
        file_data = {
            'mimetype': 'audio/mp3',
        }
        self.upload_file(self.uid, '/disk/books/audio_book_1.mp3', file_data=file_data)
        opts = {
            'uid': self.uid,
            'path': '/disk/books',
        }
        listing = self.json_ok('list', opts)
        self.assertEqual(len(listing), 3)
        opts['type'] = 'book'
        listing = self.json_ok('list', opts)
        self.assertEqual(len(listing), 2)

    def test_list_yareader_location(self):
        '''
        Проверка работоспособности алиаса yareader
        Загружаем несколько книг в разные каталоги и убеждаемся, что
        они все есть в листинге по /yareader
        '''
        opts = {
            'uid': self.uid,
            'path': '/disk/books',
        }
        self.json_ok('mkdir', opts)
        file_data = {
            'mimetype': 'application/zip',
        }
        self.upload_file(self.uid, '/disk/books/book_1.epub', file_data=file_data)
        opts = {
            'uid': self.uid,
            'path': '/disk/new_books',
        }
        self.json_ok('mkdir', opts)
        file_data = {
            'mimetype': 'application/zip',
        }
        self.upload_file(self.uid, '/disk/new_books/new_book_1.epub', file_data=file_data)
        file_data = {
            'mimetype': 'text/xml',
        }
        self.upload_file(self.uid, '/disk/new_books/new_book_2.fb2', file_data=file_data)
        opts = {
            'uid': self.uid,
            'path': '/yareader',
        }
        listing = self.json_ok('list', opts)
        self.assertEqual(len(listing), 4)
        

