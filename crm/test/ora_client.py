import logging

import cx_Oracle

from crm.space.test.helpers import get_ora_config
from crm.space.test.stopwatch import Stopwatch


class Oracle(object):

    def __init__(self):
        self.logger = logging.getLogger(self.__class__.__name__)
        self.connection_string = get_ora_config()
        self.db = None
        self.cursor = None

    def connect(self):
        self.logger.debug("Connecting to Oracle")
        self.db = cx_Oracle.connect(self.connection_string)
        self.cursor = self.db.cursor()

    def disconnect(self):
        try:
            if self.cursor:
                self.cursor.close()
            if self.db:
                self.db.close()
            self.logger.debug("Disconnected from Oracle")
        except cx_Oracle.DatabaseError as ex:
            self.logger.error("Error on closing ora. ex: %s", ex)

    def query(self, sql, args={}):
        try:
            self.connect()
            sw = Stopwatch()
            self.logger.debug("Executing query. sql: %s, args: %s", sql, args)
            self.cursor.execute(sql, args)
            self.logger.info("Query executed. elapsed: %f ms", sw.elapsed())
            rows = []
            for row in self.cursor:
                rows.append(row)
            return rows
        finally:
            self.disconnect()

    def nonQueryBatch(self, sql, args={}):
        try:
            self.connect()
            self.logger.debug("Preparing batch query. sql: %s, len(args): %d", sql, len(args))
            for chunk in self._chunks(args, 1000):
                sw = Stopwatch()
                self.logger.debug("Executing batch query. args: %s", chunk)
                self.cursor.executemany(sql, chunk)
                self.logger.info("Batch query executed. elapsed: %f ms", sw.elapsed())
                errors = self.cursor.getbatcherrors()
                for error in errors:
                    self.logger.error("Error", error.message, "at row offset", error.offset, )

                if errors:
                    self.db.rollback()
                    raise RuntimeError('Oracle batch error')

            self.db.commit()
        finally:
            self.disconnect()

    def execute(self, sql, bindvars=None, commit=False):
        self.cursor.execute(sql, bindvars)

        if commit:
            self.db.commit()

    def _chunks(self, lst, n):
        """Yield successive n-sized chunks from lst."""
        for i in range(0, len(lst), n):
            yield lst[i:i + n]
