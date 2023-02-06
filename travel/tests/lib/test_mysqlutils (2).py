# coding: utf-8
import os
import logging
import subprocess
from contextlib import closing
from StringIO import StringIO

import mock
import pytest
import MySQLdb
from django.conf import settings
from common.db.switcher import switcher
from travel.rasp.admin.lib import mysqlutils
from travel.rasp.admin.lib.mysqlutils import MysqlFileWriter, LoadInFileHelper, MysqlFileReader, get_mysql_conn_string, kill_all_connections_to_db, dump_db_by_alias
from travel.rasp.admin.lib.unittests.testcase import TestCase

log = logging.getLogger(__name__)


@pytest.mark.dbignore
def test_kill_all_connections_to_db_same_db():
    db_name = settings.DATABASES['default']['NAME']
    port = settings.DATABASES['default']['PORT']
    host = settings.DATABASES['default']['HOST']
    user = settings.DATABASES['default']['USER']
    password = settings.DATABASES['default']['PASSWORD']

    with closing(MySQLdb.connect(host=host, port=port, user=user, passwd=password, db=db_name, connect_timeout=1)) as conn:
        cursor = conn.cursor()
        cursor.execute('SELECT 1')

        kill_all_connections_to_db(db_name)

        with pytest.raises(MySQLdb.OperationalError):
            cursor.execute('SELECT 1')


@pytest.mark.dbignore
def test_kill_all_connections_to_db_other_db():
    current_db_name = settings.DATABASES['default']['NAME']

    db_name = 'information_schema'
    host = settings.DATABASES['default']['HOST']
    port = settings.DATABASES['default']['PORT']
    user = settings.DATABASES['default']['USER']
    password = settings.DATABASES['default']['PASSWORD']

    with closing(MySQLdb.connect(host=host, port=port, user=user, passwd=password, db=db_name, connect_timeout=1)) as conn:
        cursor = conn.cursor()
        cursor.execute('SELECT 1')

        kill_all_connections_to_db(current_db_name)

        cursor.execute('SELECT 1')


@pytest.mark.parametrize('conn_params, options, expected_res', [
    [
        {
            'host': 'my.host.ru',
            'port': 3342,
            'user': 'root111',
            'passwd': '456',
            'db': 'rasp_db_name'
        },
        '--max-allowed-packet=128M --net-buffer-length=8M',
        '--host=my.host.ru --port=3342 --user=root111 --password=456 --max-allowed-packet=128M --net-buffer-length=8M rasp_db_name'
    ],
    [
        {
            'host': 'my.host.ru',
            'user': 'root111',
            'db': 'rasp_db_name'
        },
        '--max-allowed-packet=128M',
        '--host=my.host.ru --user=root111 --max-allowed-packet=128M rasp_db_name'
    ],
    [
        {
            'db': 'rasp_db_name'
        },
        '',
        'rasp_db_name'
    ],
])
def test_get_mysql_conn_string(conn_params, options, expected_res):
    assert get_mysql_conn_string(conn_params, options) == expected_res


@mock.patch.object(switcher, 'get_db_alias', return_value='default')
def test_dump_db_by_alias(_mget_db_alias):
    filepath = os.path.join('tmp', 'dir', 'fulldb')

    conn_params = {
        'host': '1.2.3.4',
        'port': 567,
        'user': 'user0',
        'passwd': 'pwd9',
        'db': 'test_db',
    }
    expected_dump_cmd = 'mysqldump --host={h} --port={p} --user={u} --password={pwd} {options}'.format(
        h=conn_params['host'],
        p=conn_params['port'],
        u=conn_params['user'],
        pwd=conn_params['passwd'],
        options='--extended-insert --create-options '
                '--max-allowed-packet=128M --net-buffer-length=8M',
    )

    get_conn_res = mock.MagicMock()
    get_conn_res.conn_params = conn_params
    with mock.patch.object(mysqlutils, 'get_connection_by_role', autospec=True, return_value=get_conn_res), \
            mock.patch.object(mysqlutils, 'can_set_gtid_purged_off', return_value=True) , \
            mock.patch.object(subprocess, 'check_output', autospec=True) as full_dump_call:
        dump_db_by_alias('work_db', filepath)

    expected_cmd = '(set -o pipefail; {dump} --set-gtid-purged=OFF {db} | gzip -c > {file})'.format(
        dump=expected_dump_cmd,
        db=conn_params['db'],
        file=filepath
    )
    full_dump_call.assert_called_once_with(expected_cmd, shell=True, stderr=subprocess.STDOUT, executable='/bin/bash')

    with mock.patch.object(mysqlutils, 'get_connection_by_role', autospec=True, return_value=get_conn_res), \
            mock.patch.object(mysqlutils, 'can_set_gtid_purged_off', return_value=False), \
            mock.patch.object(subprocess, 'check_output', autospec=True) as schema_dump_call:
        dump_db_by_alias('work_db', filepath, schema_only=True)

    expected_cmd = '(set -o pipefail; {{ {dump} --no-data --ignore-table={db}.{t} {db};\n{dump} {db} {t}; }} ' \
                   '| gzip -c > {file})'.format(
        dump=expected_dump_cmd,
        db=conn_params['db'],
        t='django_migrations',
        file=filepath
    )
    schema_dump_call.assert_called_once_with(expected_cmd, shell=True, stderr=subprocess.STDOUT, executable='/bin/bash')


class MysqlWriterTest(TestCase):
    def testSimple(self):
        stream = StringIO()
        writer = MysqlFileWriter(stream, ('f1', 'f2'))
        writer.writedict({'f1': '23', 'f2': u'sssfff'})
        writer.writedict({'f1': 5, 'f2': True})

        self.assertEqual(stream.getvalue(),
'''
"23"\t"sssfff"
"5"\t"1"
'''.strip())


class LoadInFileHelperTest(TestCase):
    def testQuote(self):
        self.assertEqual('"aaabbb"', LoadInFileHelper.quote('aaabbb'))
        self.assertEqual('"aaa\\nbbb"', LoadInFileHelper.quote('aaa\nbbb'))

    def testEscape(self):
        self.assertEqual('\\0', LoadInFileHelper.escape('\x00'))
        self.assertEqual('\\b', LoadInFileHelper.escape('\b'))
        self.assertEqual('\\n', LoadInFileHelper.escape('\n'))
        self.assertEqual('\\r', LoadInFileHelper.escape('\r'))
        self.assertEqual('\\t', LoadInFileHelper.escape('\t'))
        self.assertEqual('\\Z', LoadInFileHelper.escape('\x1a'))
        self.assertEqual('\\"', LoadInFileHelper.escape('"'))
        self.assertEqual('\\\\', LoadInFileHelper.escape('\\')),
        self.assertEqual('\\\\\\0\\n', LoadInFileHelper.escape('\\\x00\n')),

        self.assertEqual('aaa\\tbbb', LoadInFileHelper.escape('aaa\tbbb')),
        self.assertEqual('aaa\\0bbb', LoadInFileHelper.escape('aaa\x00bbb')),

    def testTransform(self):
        self.assertEqual('NULL', LoadInFileHelper.transform_value(None))
        self.assertEqual('"3"', LoadInFileHelper.transform_value(3))
        self.assertEqual('"1"', LoadInFileHelper.transform_value(True))
        self.assertEqual('"aaabbb"', LoadInFileHelper.transform_value('aaabbb'))
        self.assertEqual('"aaa\\nbbb"', LoadInFileHelper.transform_value('aaa\nbbb'))

    def testUnescape(self):
        self.assertEqual("aaa", LoadInFileHelper.unescape('aaa'))
        self.assertEqual("aaa\n\\", LoadInFileHelper.unescape('aaa\\n\\\\'))
        self.assertRaises(ValueError, LoadInFileHelper.unescape, 'aaa\\n\\')
        self.assertEqual("aaa\n\\bbb\x1a", LoadInFileHelper.unescape('aaa\\n\\\\bbb\\Z'))

    def testSimpleRestore(self):
        self.assertEqual(None, LoadInFileHelper.simple_restore_value('NULL'))
        self.assertEqual(u'aaa\nbbb', LoadInFileHelper.simple_restore_value('"aaa\\nbbb"'))
        self.assertRaises(ValueError, LoadInFileHelper.simple_restore_value, 'aaa\\nbbb')


class MysqlFileReaderTest(TestCase):
    def testSimple(self):
        data = [
            {'f1': '23', 'f2': u'sssfff'},
            {'f1': None, 'f2': u'sss\u0000aabb'},
            {'f1': 5, 'f2': True},
        ]

        out_data = [
            {'f1': '23', 'f2': u'sssfff'},
            {'f1': None, 'f2': u'sss\u0000aabb'},
            {'f1': u'5', 'f2': u'1'},
        ]

        stream = StringIO()
        writer = MysqlFileWriter(stream, ('f1', 'f2'))
        for rowdict in data:
            writer.writedict(rowdict)

        stream.seek(0)

        reader = MysqlFileReader(stream, ('f1', 'f2'))
        read_data = list(reader)

        self.assertListEqual(out_data, read_data)
