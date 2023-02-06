import os

import MySQLdb
import subprocess
from contextlib import closing

important_countries = [
    225,  # RUSSIA_ID
    149,  # BELARUS_ID
    187,  # UKRAINE_ID
    209,  # TAJIKISTAN_ID
    983,  # TURKEY_ID
    159,  # KAZAKHSTAN_ID
    117,  # LITVA_ID
]

important_settlements = [
    213,  # MOSCOW_ID
    2,    # SPB_ID
    143,  # KIEV_ID
]

DEBUG = False


def find_all_translations(db, regions):
    def get_commands():
        translations = (
            ('www_country', ['new_L_title'], important_countries),
            ('www_region', ['new_L_title'], regions),
            ('www_settlement', ['new_L_title', 'new_L_abbr_title'], important_settlements),
            ('www_stationmajority', ['new_L_title'], None),
            ('www_stationtype', ['new_L_name', 'new_L_prefix', 'new_L_railway_prefix'], None),
        )

        for table, fields, ids in translations:
            for field in fields:
                yield 'SELECT {field}_id FROM {table}{where}'.format(
                    table=table,
                    field=field,
                    where=' WHERE id IN ({})'.format(','.join(str(x) for x in ids)) if ids else ''
                )

    translated_title_ids = set()
    with closing(db.cursor()) as cur:
        for command in get_commands():
            if DEBUG:
                print(command)
            cur.execute(command)
            translated_title_ids.update(title_id for [title_id] in cur.fetchall())

    return translated_title_ids


def find_regions(db):
    with closing(db.cursor()) as cur:
        command = (
            'SELECT www_region.id from www_region '
            'INNER JOIN www_settlement ON www_settlement.region_id = www_region.id '
            'where www_settlement.id IN ({})'
        ).format(','.join(str(x) for x in important_settlements))
        cur.execute(command)

        importand_regions = set()
        importand_regions.update(region_id for [region_id] in cur.fetchall())
        return importand_regions


def get_path_to_mysql_binary(name, env_name='AVIA_MYSQL_BIN'):
    path = os.getenv(env_name)
    if path:
        return os.path.join(path, name)
    return name


def recreate(test_config, source_config):

    def mysqldump_command(tables='', where=False, export_data=True):
        executable = get_path_to_mysql_binary('mysqldump')
        command = '{executable} -u {user} --skip-comments {db} {tables}'.format(
            executable=executable,
            user=source_config['USER'],
            db=source_config['NAME'],
            tables=tables
        )
        if source_config.get('PORT'):
            command += ' --protocol=tcp --port={}'.format(source_config['PORT'])

        if export_data:
            command += ' --no-create-info'
        else:
            command += ' --no-data'

        if where:
            command += ' --where "{}"'.format(where)

        if source_config['PASSWORD']:
            command += ' -p{}'.format(source_config['PASSWORD'])

        return command

    def mysqlimport(dump_commands):
        executable = get_path_to_mysql_binary('mysql')
        command = '{executable} -u {user} {db}'.format(
            executable=executable,
            user=test_config['USER'],
            db=test_config['NAME'],
        )

        if test_config.get('PORT'):
            command += ' --protocol=tcp --port={}'.format(test_config['PORT'])

        if test_config['PASSWORD']:
            command += ' -p{}'.format(test_config['PASSWORD'])

        command = '({}) | {}'.format(';'.join(dump_commands), command)

        if DEBUG:
            print(command)
        subprocess.check_output(command, shell=True)

    def recreate_test_db(db):
        with closing(db.cursor()) as cur:
            cur.execute("DROP DATABASE IF EXISTS {} ".format(test_config['NAME']))
            cur.execute("CREATE DATABASE {} DEFAULT CHARSET utf8".format(test_config['NAME']))

    def mysqldump(db):
        regions = find_regions(db)

        dump_commands = [mysqldump_command(export_data=False)]

        dumps_models = [
            (['www_translated_title'], find_all_translations(db, regions)),
            ([
                'www_transporttype',
                'www_team',
                'www_service',
                'www_cabinclass',
                'www_citymajority',
                'www_stationmajority',
                'www_stationtype',
                'www_rthreadtype',
                'www_tarifftype',
                'www_codesystem',
                'order_emailtype',
                'www_transportsubtype',
                'avia_nationalversion'
            ], None),
            (['www_country'], important_countries),
            (['www_region'], regions),
            (['www_settlement'], important_settlements)
        ]
        for tables, ids in dumps_models:
            dump_commands.append(mysqldump_command(
                tables=' '.join(tables),
                where='id IN ({})'.format(','.join(str(x) for x in ids)) if ids else ''
            ))

        return dump_commands

    db = MySQLdb.connect(
        host=source_config['HOST'],
        user=source_config['USER'],
        db=source_config['NAME'],
        port=int(source_config.get('PORT') or 3306),
        passwd=source_config.get('PASSWORD'),
    )
    recreate_test_db(db)
    mysqlimport(mysqldump(db))
