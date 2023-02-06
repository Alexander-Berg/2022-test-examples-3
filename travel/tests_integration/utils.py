# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import os
import errno

import MySQLdb
from MySQLdb.connections import ProgrammingError


DB_EXISTS_ERR = 1007


def get_log_file(log_file):
    from travel.rasp.admin.lib.logs import create_current_file_run_log

    if log_file is None:
        log_file = create_current_file_run_log()

    return log_file


def create_directory(directory):
    try:
        os.makedirs(directory)
    except OSError as e:
        if e.errno != errno.EEXIST:
            raise


class DbConn(object):
    def __init__(self, host, port=3306, user='root', passwd='', db_name=None):
        self.host = host
        self.port = port
        self.user = user
        self.passwd = passwd
        self.db_name = db_name

        self._conn = None

    @property
    def conn(self):
        if not self._conn:
            self._conn = MySQLdb.connect(
                host=self.host,
                port=self.port,
                user=self.user,
                passwd=self.passwd,
            )

        return self._conn

    def create_db(self, fail_if_exists=True):
        cur = self.conn.cursor()
        sql = "CREATE DATABASE {db_name}".format(db_name=self.db_name)
        try:
            cur.execute(sql)
        except ProgrammingError as ex:
            if str(DB_EXISTS_ERR) in str(ex):
                if fail_if_exists:
                    raise
                else:
                    print(repr(ex))

    def execute(self, *sqls):
        cur = self.conn.cursor()

        if self.db_name:
            cur.execute('use {}'.format(self.db_name))

        for sql in sqls:
            cur.execute(sql)
            for row in cur.fetchall():
                yield row

        cur.close()

    def get_hostname(self):
        return next(self.execute('select @@hostname;'))[0]

    def get_tables_count(self):
        sql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '{db_name}';".format(db_name=self.db_name)
        res = self.execute(sql)
        return int(next(res)[0])

    def remove_all_tables(self):
        if self.get_tables_count() == 0:
            return ()

        # https://stackoverflow.com/a/18625545/2468006
        sqls = [
            """
            SET FOREIGN_KEY_CHECKS = 0;
            SET GROUP_CONCAT_MAX_LEN=32768;
            SET @tables = NULL;
            SELECT GROUP_CONCAT('`', table_name, '`') INTO @tables
              FROM information_schema.tables
              WHERE table_schema = '{db_name}';
            SELECT IFNULL(@tables,'dummy') INTO @tables;

            SET @tables = CONCAT('DROP TABLE IF EXISTS ', @tables);
            PREPARE stmt FROM @tables;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
            SET FOREIGN_KEY_CHECKS = 1;
            """.format(db_name=self.db_name),
        ]

        return self.execute(*sqls)
