#!/usr/bin/env python2
# -*- encoding: utf-8 -*-

import cmysql
import re

head = '''Проверяем наличие primary and unique not null ключей. Отсутствие их на реплике может приводить к fullscan.
          https://mariadb.com/kb/en/library/row-based-replication-with-no-primary-key/
'''

def run(config, **kwargs):
    requests1 = { 'binlog_format': "show variables like '%binlog_format%'",
    }
    requests2 = { 'full_tables': "select TABLE_SCHEMA, TABLE_NAME from information_schema.TABLES where TABLE_SCHEMA not in ('information_schema', 'sys', 'performance_schema', 'mysql', 'information_schema') AND ENGINE NOT IN ('ARCHIVE')",
                 'good_tables': "SELECT TABLE_SCHEMA, TABLE_NAME, sum(CASE WHEN NULLABLE = 'YES' THEN 1 else 0 end) as nullable FROM information_schema.statistics WHERE  (INDEX_NAME!='PRIMARY' AND NON_UNIQUE=0) OR INDEX_NAME='PRIMARY' GROUP BY TABLE_SCHEMA, TABLE_NAME, INDEX_NAME HAVING nullable=0",
    }

    settings = kwargs[__name__] if kwargs.has_key(__name__) else {}
    excludes = settings.get('excludes', [])

    results = cmysql.query(requests1, config)
    errors = [ '[{0}] {1}'.format(i, results[i][1]) for i in results if results[i][1] ]
    if len(errors) != 0:
        return (2, '; '.join(errors), __name__)

    if results['binlog_format'][0][0][1] != 'ROW':
        return (0, 'SKIP. Binlog_format not ROW: {0}'.format(results['binlog_format'][0][0][1]), __name__)

    results.update(cmysql.query(requests2, config))
    good_tables = [(i[0], i[1]) for i in results['good_tables'][0]]
    diff_tables = set(results['full_tables'][0])-set(good_tables)

    aggr_tables = ['{0}.{1}'.format(i[0], i[1]) for i in list(diff_tables)]

    for exclude in excludes:
        rgxp = re.compile(exclude)
        aggr_tables = [i for i in aggr_tables if rgxp.match(i) is None]

    if len(aggr_tables) != 0:
        msg = 'FAILED. Not found primary/uniq(not null) keys: {0}'.format(', '.join(aggr_tables))
        return (2, msg, __name__)

    msg = 'SUCCESS. All tables has primary/uniq(not null) keys'
    return (0, msg, __name__)

def help():
    return head
