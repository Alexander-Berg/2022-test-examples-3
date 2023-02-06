# -*- coding: utf8 -*-
from tempfile import NamedTemporaryFile

from unittest import TestCase

from mapreduce.yt.python.yt_stuff import YtStuff

from getter.exceptions import VerificationError
from getter.service.yt_resource.yt_file import YtFileResource
from getter.service.yt_resource.yt_table import YtTableResource


def download_and_read(source, resent_result):
    with NamedTemporaryFile() as fd:
        result = source.download(fd.name, None, resent_result)
        data = fd.read()

    return result, data


def create_tables(yt_client):
    yt_client.create('table', '//tmp/empty_table/latest', recursive=True)

    yt_client.create(
        'table',
        '//tmp/table_with_data/2019-11-20',
        recursive=True,
        attributes={
            'schema': [
                {'name': 'title', 'type': 'string'},
                {'name': 'market_sku', 'type': 'uint64'},
                {'name': 'ware_md5', 'type': 'string'},
            ],
            'strict': True,
        },
    )
    yt_client.write_table(
        '//tmp/table_with_data/2019-11-20',
        [{'title': 'test 1', 'market_sku': 1, 'ware_md5': '1a'},
         {'title': 'test 2', 'market_sku': 2, 'ware_md5': '2b'}],
        format='json',
        raw=False,
    )
    yt_client.link('//tmp/table_with_data/2019-11-20', '//tmp/table_with_data/latest')

    yt_client.create(
        'table',
        '//tmp/sns_main_pages_test_data/2021-03-24',
        recursive=True,
        attributes={
            'schema': [
                {'name': 'business_id', 'type': 'uint64'},
                {'name': 'shop_name', 'type': 'string'},
            ],
            'strict': True,
        },
    )
    yt_client.write_table(
        '//tmp/sns_main_pages_test_data/2021-03-24',
        [
            {'business_id': 123, 'shop_name': 'abacaba'},
            {'business_id': 987, 'shop_name': 'abc'},
        ],
        format='json',
        raw=False,
    )
    yt_client.link('//tmp/sns_main_pages_test_data/2021-03-24', '//tmp/sns_main_pages_test_data/latest')


def create_files(yt_client):
    yt_client.create('file', '//tmp/empty_file/latest', recursive=True)

    yt_client.create('file', '//tmp/file_with_data/2019-11-20', recursive=True)
    yt_client.write_file('//tmp/file_with_data/2019-11-20', 'qwe\nasd')
    yt_client.link('//tmp/file_with_data/2019-11-20', '//tmp/file_with_data/latest')


class TestYtResource(TestCase):
    @classmethod
    def setUpClass(cls):
        cls._yt_stuff = YtStuff()
        cls._yt_stuff.start_local_yt()

        cls._yt_client = cls._yt_stuff.get_yt_client()

        create_files(cls._yt_client)
        create_tables(cls._yt_client)

    @classmethod
    def tearDownClass(cls):
        cls._yt_stuff.stop_local_yt()

    def test_nonexistent_table_reading(self):
        """Попытка чтения из несуществующей таблицы в YT"""
        table = YtTableResource(self._yt_client, 'nonexistent_table', '//home/nonexistent/table', 'latest')
        result, _ = download_and_read(table, {})

        self.assertDictEqual(result, {'code': 500})

    def test_nonexistent_file_reading(self):
        """Попытка чтения из несуществующего файла в YT"""
        file = YtFileResource(self._yt_client, 'nonexistent_file', '//home/nonexistent/file', 'latest')
        result, _ = download_and_read(file, {})

        self.assertDictEqual(result, {'code': 500})

    def test_empty_file_reading(self):
        """Чтение из пустого файла в YT"""
        file = YtFileResource(self._yt_client, 'empty_file', '//tmp/empty_file', 'latest')

        result, data = download_and_read(file, {})

        self.assertDictEqual(result, {'yt_gen': 'latest', 'code': 200})
        self.assertEqual(data, '')

    def test_empty_table_reading(self):
        """Чтение из пустой таблицы в YT"""
        table = YtTableResource(self._yt_client, 'empty_table', '//tmp/empty_table', 'latest')

        result, data = download_and_read(table, {})

        self.assertDictEqual(result, {'yt_gen': 'latest', 'code': 200})
        self.assertEqual(data, '')

    def test_file_reading(self):
        """Чтение данных из файла в YT без форматирования"""
        file = YtFileResource(self._yt_client, 'file_with_data', '//tmp/file_with_data', '2019-11-20')
        result, data = download_and_read(file, {})

        self.assertDictEqual(result, {'code': 200, 'yt_gen': '2019-11-20'})
        self.assertEqual(data, 'qwe\nasd')

    def test_file_reading_with_formatting(self):
        """Чтение данные из файла в YT с форматированием"""
        def formatter(data):
            return 'line={}'.format(data)

        file = YtFileResource(self._yt_client, 'file_with_data', '//tmp/file_with_data', '2019-11-20', formatter=formatter)
        result, data = download_and_read(file, {})

        self.assertDictEqual(result, {'code': 200, 'yt_gen': '2019-11-20'})
        self.assertEqual(data, 'line=qwe\nline=asd')

    def test_table_link(self):
        """Чтение из таблицы по ссылке"""
        def formatter(data):
            return '{title}\t{market_sku}\t{ware_md5}\n'.format(**data)

        table = YtTableResource(self._yt_client, 'table_link', '//tmp/table_with_data', 'latest', formatter=formatter)
        result, data = download_and_read(table, {})

        self.assertDictEqual(result, {'yt_gen': '2019-11-20', 'code': 200})
        self.assertEqual(data, 'test 1\t1\t1a\ntest 2\t2\t2b\n')

    def test_sns_main_pages_table_link(self):
        """Чтение из sns_main_pages таблицы по ссылке"""
        def formatter(data):
            return '{business_id}\t{shop_name}\n'.format(**data)

        table = YtTableResource(self._yt_client, 'sns_main_pages_table_link', '//tmp/sns_main_pages_test_data', 'latest', formatter=formatter)
        result, data = download_and_read(table, {})
        self.assertDictEqual(result, {'yt_gen': '2021-03-24', 'code': 200})
        self.assertEqual(data, '123\tabacaba\n987\tabc\n')

    def test_sns_main_pages_table_reading_without_formatter(self):
        """Если для таблицы не указан форматер, пишем сериализованный json"""
        table = YtTableResource(self._yt_client, 'ns_main_pages_table_without_formatter', '//tmp/sns_main_pages_test_data', '2021-03-24')
        result, data = download_and_read(table, {})
        self.assertDictEqual(result, {'yt_gen': '2021-03-24', 'code': 200})
        self.assertEqual(data, '{"business_id": 123, "shop_name": "abacaba"}\n{"business_id": 987, "shop_name": "abc"}\n')

    def test_file_link(self):
        """Чтение по ссылке на файл"""
        file = YtFileResource(self._yt_client, 'file_link', '//tmp/file_with_data', 'latest')
        result, data = download_and_read(file, {})

        self.assertDictEqual(result, {'yt_gen': '2019-11-20', 'code': 200})
        self.assertEqual(data, 'qwe\nasd')

    def test_retry_read_latest_file(self):
        """Если в состоянии указана предыдущая генерация, совпадающая с последней, заново загрузку делать не надо"""
        table = YtTableResource(self._yt_client, 'table_retry', '//tmp/table_with_data', 'latest')
        result, _ = download_and_read(table, {'yt_gen': '2019-11-20'})

        self.assertDictEqual(result, {'code': 304, 'yt_gen': '2019-11-20'})

    def test_file_reading_as_table(self):
        """При попытке читать файл как таблицу кидается исключение VerificationError"""
        error_msg = r'A type of loaded object should be table, real value is file'

        file = YtTableResource(self._yt_client, 'file_as_table', '//tmp/file_with_data', '2019-11-20')
        with self.assertRaisesRegexp(VerificationError, error_msg):
            download_and_read(file, {})

        file_link = YtTableResource(self._yt_client, 'file_as_table', '//tmp/file_with_data', 'latest')
        with self.assertRaisesRegexp(VerificationError, error_msg):
            download_and_read(file_link, {})

    def test_table_reading_from_file(self):
        """При попытке читать таблицу как файл кидается исключение VerificationError"""
        error_msg = r'A type of loaded object should be file, real value is table'

        table = YtFileResource(self._yt_client, 'table_as_file', '//tmp/table_with_data', '2019-11-20')
        with self.assertRaisesRegexp(VerificationError, error_msg):
            download_and_read(table, {})

        table_link = YtFileResource(self._yt_client, 'table_as_file', '//tmp/table_with_data', 'latest')
        with self.assertRaisesRegexp(VerificationError, error_msg):
            download_and_read(table_link, {})

    def test_table_reading_without_formatter(self):
        """Если для таблицы не указан форматер, пишем сериализованный json"""
        table = YtTableResource(self._yt_client, 'table_without_formatter', '//tmp/table_with_data', '2019-11-20')
        result, data = download_and_read(table, {})

        self.assertDictEqual(result, {'yt_gen': '2019-11-20', 'code': 200})
        self.assertEqual(data, '{"ware_md5": "1a", "market_sku": 1, "title": "test 1"}\n{"ware_md5": "2b", "market_sku": 2, "title": "test 2"}\n')

    def test_file_reading_with_validation(self):
        """Если что-то не проходит валидацию, должно прокидаться исключение"""
        def valid_checker(path):
            with open(path, 'rt') as fd:
                data = fd.read()
            assert data == 'qwe\nasd'

        def invalid_checker(path):
            with open(path, 'rt') as fd:
                data = fd.read()
            assert data == 'qwe'

        valid_checker_file = YtFileResource(
            self._yt_client,
            'file_checking',
            '//tmp/file_with_data',
            'latest',
            checker=valid_checker,
        )
        result, _ = download_and_read(valid_checker_file, {})

        self.assertDictEqual(result, {'yt_gen': '2019-11-20', 'code': 200})

        invalid_checker_file = YtFileResource(
            self._yt_client,
            'file_checking',
            '//tmp/file_with_data',
            'latest',
            checker=invalid_checker,
        )
        with self.assertRaisesRegexp(AssertionError, r'checking failed for file: .*'):
            download_and_read(invalid_checker_file, {})

    def test_table_reading_with_validation(self):
        """Если что-то не проходит валидацию, должно прокидаться исключение"""
        def valid_formatter(data):
            return '{title}\t{market_sku}\t{ware_md5}\n'.format(**data)

        def invalid_formatter(data):
            return '{title}\t{market_sku}\t{ware_md5}\t{title}\n'.format(**data)

        def checker(path):
            with open(path, 'rt') as fd:
                for line in fd:
                    assert len(line.split('\t')) == 3

        valid_formatter_table = YtTableResource(
            self._yt_client,
            'table_checking',
            '//tmp/table_with_data',
            'latest',
            checker=checker,
            formatter=valid_formatter,
        )
        result, _ = download_and_read(valid_formatter_table, {})

        self.assertDictEqual(result, {'yt_gen': '2019-11-20', 'code': 200})

        invalid_formatter_table = YtTableResource(
            self._yt_client,
            'table_checking',
            '//tmp/table_with_data',
            'latest',
            checker=checker,
            formatter=invalid_formatter,
        )
        with self.assertRaisesRegexp(AssertionError, r'checking failed for file: .* assert 4 == 3.*'):
            download_and_read(invalid_formatter_table, {})
