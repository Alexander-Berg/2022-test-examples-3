import psycopg2

from psycopg2.extras import NamedTupleCursor


class XConf:
    def __init__(self, conninfo):
        self.conninfo = conninfo
        self._execute('TRUNCATE xconf.configurations', False)

    def _execute(self, query, expect_result, *params):
        with psycopg2.connect(self.conninfo) as connection:
            connection.autocommit = True
            with connection.cursor(cursor_factory=NamedTupleCursor) as cursor:
                print(cursor.mogrify(query, params))
                cursor.execute(query, params)
                print(connection.notices)
                if expect_result:
                    return cursor.fetchall()


    def put(self, conf_type, name, owner_id, settings, token=None, environment='any', revision=None):
        sql = '''
            SELECT * FROM xconf.put(%s, %s, %s, %s, %s, %s, %s)
        '''
        return self._execute(sql, True, conf_type, name, owner_id, settings, token, environment, revision)

    def get(self, conf_type, name):
        sql = '''
            SELECT * FROM xconf.configurations
            WHERE type = %s
                AND name = %s
        '''
        return self._execute(sql, True, conf_type, name)
