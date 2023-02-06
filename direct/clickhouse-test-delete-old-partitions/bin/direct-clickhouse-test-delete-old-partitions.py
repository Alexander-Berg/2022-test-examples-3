#!/usr/bin/python
# -*- coding: utf8 -*-

import sys
import argparse
import requests
from datetime import date


description = """
Удаляет старые партиции в тестовом clickhouse.
"""


TEST_CLICKHOUSE_DB_URL = "http://ppctest-clickhouse01i.ppc.yandex.ru:8123/"
#TEST_CLICKHOUSE_DB_URL = "http://localhost:8123/"
TEST_CLICKHOUSE_PASSWORD = open('/etc/direct-tokens/clickhouse_direct_test', 'r').read().rstrip()



def parse_arguments():
    parser = argparse.ArgumentParser(description=description, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument('-m', '--months-to-keep', dest='months_to_keep', help='За сколько полных последних месяцев оставить данные (по умолчанию 3)', type=int, default=3)
    parser.add_argument('-n', '--dry-run', dest='dry', help='Только напечатать список партиций, подлежащих удалению', action='store_true')
    parser.add_argument('-v', '--verbose', dest='verbose', help='Подробный режим (печатать удаляемые партиции)', action='store_true')
    args = parser.parse_args()

    if args.months_to_keep < 0:
        sys.exit(u'ошибка: -m/--months-to-keep должен быть неотрицательным')
    
    return args


def clh_query(query, stream=False):
    r = requests.post(TEST_CLICKHOUSE_DB_URL, params={'user': 'direct_test', 'database': 'direct_test', 'password': TEST_CLICKHOUSE_PASSWORD}, data=query, stream=stream)
    if r.status_code != 200:
        sys.exit(r.text)
    return r


def partitions_to_drop(before_ym):
    partitions = []
    
    r = clh_query("SELECT DISTINCT database, table, partition FROM system.parts WHERE active and partition < '%s'" % before_ym, stream=True)
    part_lines = r.iter_lines()
    for part_line in part_lines:
        if part_line:
            # format: tab separated values
            a = part_line.strip('\n').split('\t')
            db, table, partition = a[0], a[1], a[2]
            
            partitions.append((db, table, partition))
    
    return partitions


def drop_partitions(partitions, dry=False, verbose=False):
    for db, table, partition in partitions:
        if dry or verbose:
            print "%s.%s: partition %s" % (db, table, partition)
        if dry:
            continue

        r = clh_query("ALTER TABLE %s.%s DROP PARTITION %s" % (db, table, partition))
    return



def run():
    args = parse_arguments()

    today = date.today()
    before_year = int(today.year + (today.month-1 - args.months_to_keep)/12)
    before_month = (today.month-1 - args.months_to_keep) % 12 + 1
    before_ym = '%04d%02d' % (before_year, before_month)
    
    partitions = partitions_to_drop(before_ym)
    drop_partitions(partitions, args.dry, args.verbose)



if __name__ == '__main__':
    run()
