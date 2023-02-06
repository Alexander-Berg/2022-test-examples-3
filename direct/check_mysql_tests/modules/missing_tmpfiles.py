#!/usr/bin/env python2
# -*- encoding: utf-8 -*-

import cmysql

head = '''Проверка баз mysql на наличие битых временных таблиц.
          https://mariadb.com/resources/blog/get-rid-orphaned-innodb-temporary-tables-right-way
          https://bugs.mysql.com/bug.php?id=72135
          https://dev.mysql.com/doc/refman/5.7/en/innodb-troubleshooting-datadict.html
'''

def run(config, **kwargs):
    requests = { 'temp_tables' : "SELECT name FROM INFORMATION_SCHEMA.INNODB_SYS_TABLES WHERE NAME LIKE '%#sql%' GROUP BY name" }
    status, code = "", ""
    results = cmysql.query(requests, config)
    errors = [ '[{0}] {1}'.format(i, results[i][1]) for i in results if results[i][1] ]
    if len(errors) != 0:
        return (2, '; '.join(errors), __name__)
    tables = [ i[0] for i in results['temp_tables'][0] ]
    if tables:
        msg = 'FAILED. Found broken temp tables: {0}'.format(', '.join(tables))
        return (2, msg, __name__)
    msg = 'SUCCESS. Not found corrupted temp tables'
    return (0, msg, __name__)

def help():
    msg = '''Инструкция для исправления таблиц:
             1. скопировать frm соседней таблицы с именем побитой tmp таблицы
                 shell> cp ppc/bs_order_target_stat.frm ppc/#sql-6e34_186.frm
             2. зайти в базу и выполнить специальный запрос с удалением #mysql50#
                 mysql> set sql_log_bin=0;
                 mysql> use ppc;
                 mysql> DROP TABLE `#mysql50##sql-6e34_186`;
             3. проверить, что таблицы удалены
                 mysql> SELECT * FROM INFORMATION_SCHEMA.INNODB_SYS_TABLES WHERE NAME LIKE '%#sql%';
    '''
    return '\n'.join([head, msg])
