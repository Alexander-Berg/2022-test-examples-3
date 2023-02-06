#!/usr/bin/env python2
# -*- encoding: utf-8 -*-

import cmysql
import re
import time

head = '''Проверяет доступность таблиц в БД инстанса. Нужен для выявления битых таблиц. Для подробного отчета по таблицам можно воспользоваться утилитой `mysql-simple-check-tables --debug`.
'''

DELTA = 600 #отствание не более 10 минут

def run(config, **kwargs):
    requests1 = { 'get_all_tables': "SELECT TABLE_SCHEMA, TABLE_NAME from information_schema.TABLES WHERE TABLE_SCHEMA NOT IN ('information_schema', 'performance_schema', 'sys') AND ENGINE in ('InnoDB', 'MyISAM')",
    }

    settings = kwargs[__name__] if kwargs.has_key(__name__) else {}
    excludes = settings.get('excludes', [])
    excludes = [re.compile(exclude) for exclude in excludes]

    results = cmysql.query(requests1, config)
    errors = [ '[{0}] {1}'.format(i, results[i][1]) for i in results if results[i][1] ]
    if len(errors) != 0:
        return (2, 'FAIL', '{1} for group {0}'.format(config, errors), __name__)

    requests2 = {}
    for bundle in results['get_all_tables'][0]:
        db, table = bundle
        full_name = '.'.join([db, table])
        rgxp = [ i.match(full_name) for i in excludes ]
        if any(rgxp): continue
        requests2[full_name] = "SELECT 1 FROM {db}.{table} LIMIT 1".format(db=db, table=table)

    results = cmysql.query(requests2, config)
    broken_tables = [full_name for full_name in results if results[full_name][1]]
    if len(broken_tables)>0:
        msg = 'FAILED. Found broken tables: {0}'.format(broken_tables)
        return (2, msg, __name__)

    msg = 'SUCCESS. Not found broken tales'
    return (0, msg, __name__)

def help():
    return head
