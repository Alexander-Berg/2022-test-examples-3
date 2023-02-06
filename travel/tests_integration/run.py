# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

"""
https://st.yandex-team.ru/RASPADMIN-1018
Скрипт для интеграционного тестирования админки.

Запускается Большой импорт (БИ) и переключение.

Запускаем БИ на небольшом наборе данных.
При этом импорты идут из подготовленных заранее файлов.
"""


import os
import subprocess
import sys
import logging

import requests

from travel.library.recipe_utils.utils import Timer
from travel.rasp.admin.tests_integration.utils import create_directory, DbConn
from travel.library.python.resource import extract_resources


log = logging.getLogger(__name__)


def download_file(url, file_to, headers=None):
    get_response = requests.get(url, stream=True, headers=headers)
    with open(file_to, 'wb') as f:
        for chunk in get_response.iter_content(chunk_size=1024):
            if chunk:  # filter out keep-alive new chunks
                f.write(chunk)


class IntegrationTests(object):
    def __init__(self, init_logging=True):
        self.work_dir = os.getenv('RASP_TESTS_WORK_DIR') or os.path.dirname(sys.executable)
        if init_logging:
            self.init_logging()

        self.django_settings = os.getenv('DJANGO_SETTINGS_MODULE', 'travel.rasp.admin.tests_integration.local_settings')
        self.geobase_data_path = os.getenv('RASP_GEOBASE_DATA_PATH') or os.path.join(self.work_dir, 'geodata6.bin')

        self.db1_settings = self._get_db_settings_from_env('DB1', 'rasp_integration_db1')
        self.db2_settings = self._get_db_settings_from_env('DB2', 'rasp_integration_db2')
        self.maintenance_settings = self._get_db_settings_from_env('MAINTENANCE', 'rasp_integration_maintenance')

        self.mongo_host = os.getenv('RASP_TESTS_MONGO_HOST', '127.0.0.1')
        self.mongo_port = os.getenv('RASP_TESTS_MONGO_PORT', '27017')
        self.mongo_db = os.getenv('RASP_TESTS_MONGO_DB', 'rasp_integration')

        self.skip_init = os.getenv('RASP_TESTS_SKIP_INIT') == '1'
        self.skip_bi = os.getenv('RASP_TESTS_SKIP_BI') == '1'
        self.skip_switch = os.getenv('RASP_TESTS_SKIP_SWITCH') == '1'

        log.info('DB1: {} {}'.format(self.db1_settings['host'], self.db1_settings['db_name']))
        log.info('DB2: {} {}'.format(self.db2_settings['host'], self.db2_settings['db_name']))
        log.info('MAINTENANCE: {} {}'.format(self.maintenance_settings['host'], self.maintenance_settings['db_name']))

    def run(self):
        self.set_service_flag()

        if not self.skip_init:
            self.init_filesystem_data()
            self.init_geobase()
            self.init_mysql()
            self.can_run_integration_check()
            self.prepare_db()
        else:
            log.info('skipping initialization')
            self.can_run_integration_check()

        if not self.skip_bi:
            self.big_import()
        else:
            log.info('skipping big_import')

        if not self.skip_switch:
            self.switch_bases()
        else:
            log.info('skipping switch_bases')

    def init_geobase(self):
        geobase_data_path = os.path.join(self.work_dir, 'geodata6.bin')
        if not os.path.exists(geobase_data_path):
            geodata_url = 'https://proxy.sandbox.yandex-team.ru/last/GEODATA6BIN_STABLE?owner=GEOBASE&attrs=%7B%22released%22:%22stable%22%7D'
            headers = {}
            sandbox_oauth_token = os.getenv('SANDBOX_OAUTH_TOKEN', None)
            if sandbox_oauth_token:
                headers['Authorization'] = 'OAuth {}'.format(sandbox_oauth_token)
            with Timer('downloading geobase to {}'.format(geobase_data_path)):
                download_file(geodata_url, geobase_data_path, headers)

        self.geobase_data_path = geobase_data_path
        log.info('Using geobase from {}'.format(self.geobase_data_path))

    def init_mysql_maintenance(self):
        maintenance_db = DbConn(**self.maintenance_settings)
        maintenance_db.create_db(fail_if_exists=False)

        log.info('MAINTENANCE: removing {} tables'.format(maintenance_db.get_tables_count()))
        res = maintenance_db.remove_all_tables()
        log.info(list(res))

        log.info('MAINTENANCE: creating table "conf"')
        res = maintenance_db.execute("""
            CREATE TABLE conf (
              name varchar(255) NOT NULL,
              value varchar(255) DEFAULT NULL,
              description varchar(255) NOT NULL,
              PRIMARY KEY (name)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8;
        """)
        log.info(list(res))

        log.info('MAINTENANCE: filling table "conf"')
        res = maintenance_db.execute("""
            INSERT INTO conf VALUES
            ('last_successful_switch','2016-08-20 05:22:53',''),
            ('work_db','main0_rasp',''),
            ('service_db','main1_rasp',''),
            ('service_db_dump','some_not_existed_file.sql.gz',''),
            ('sync_key','2016-08-20 04:56:55.494623:0.0140169430054','');
            COMMIT;
        """)
        log.info(list(res))

    def init_mysql(self):
        self.init_mysql_maintenance()

        db1 = DbConn(**self.db1_settings)
        db1.create_db(fail_if_exists=False)
        log.info('DB1: removing {} tables'.format(db1.get_tables_count()))
        res = db1.remove_all_tables()
        log.info(list(res))

        db2 = DbConn(**self.db2_settings)
        db2.create_db(fail_if_exists=False)
        log.info('DB2: removing {} tables'.format(db2.get_tables_count()))
        res = db2.remove_all_tables()
        log.info(list(res))

    def init_filesystem_data(self):
        dirs = [
            'scripts',
            'data',
            'www/db/scripts/data',
            'www/db/scripts/export/public',
            'media/data/export',
            'pathfinder/railway',
            'log/yt'
        ]
        for directory in dirs:
            full_dir_path = os.path.join(self.work_dir, directory)
            log.info('creating directory: %s', full_dir_path)
            create_directory(full_dir_path)

        extract_resources('travel/rasp/admin/tests_integration/data/', base_path=os.path.join(self.work_dir, 'data'))
        extract_resources('travel/rasp/admin/tests_integration/scripts_data/', base_path=os.path.join(self.work_dir, 'www/db/scripts/data'))
        extract_resources('travel/rasp/admin/tests_integration/railway/', base_path=os.path.join(self.work_dir, 'pathfinder/railway'))
        subprocess.check_output(['chmod', '+x', os.path.join(self.work_dir, 'pathfinder/railway/railway')])

    def set_service_flag(self):
        os.environ['RASP_SERVICE_INSTANCE'] = 'true'

    def can_run_integration_check(self):
        self.call('travel.rasp.admin.tests_integration.can_run_tests')

    def prepare_db(self):
        self.call('travel.rasp.admin.tests_integration.prepare_db', args=['-v'])

    def big_import(self):
        self.call('travel.rasp.admin.tests_integration.big_import', args=['-v'])

    def switch_bases(self):
        self.call('travel.rasp.admin.tests_integration.switch_bases', args=['-v'])

    def _get_db_settings_from_env(self, db_type, default_db_name):
        return {
            'db_name': os.getenv('RASP_TESTS_MYSQL_NAME_{}'.format(db_type), default_db_name),
            'host': os.getenv('RASP_TESTS_MYSQL_HOST_{}'.format(db_type), '127.0.0.1'),
            'port': int(os.getenv('RASP_TESTS_MYSQL_PORT_{}'.format(db_type), '3306')),
            'user': os.getenv('RASP_TESTS_MYSQL_USER_{}'.format(db_type), 'root'),
            'passwd': os.getenv('RASP_TESTS_MYSQL_PASSWORD_{}'.format(db_type), ''),
        }

    def _get_env_from_db_settings(self, db_type, db_settings):
        return {
            'RASP_TESTS_MYSQL_NAME_{}'.format(db_type): db_settings['db_name'],
            'RASP_TESTS_MYSQL_HOST_{}'.format(db_type): db_settings['host'],
            'RASP_TESTS_MYSQL_PORT_{}'.format(db_type): str(db_settings['port']),
            'RASP_TESTS_MYSQL_USER_{}'.format(db_type): db_settings['user'],
            'RASP_TESTS_MYSQL_PASSWORD_{}'.format(db_type): db_settings['passwd'],
        }

    def _get_env(self, endpoint):
        env = {
            'PYTHONUNBUFFERED': '1',
            'Y_PYTHON_ENTRY_POINT': endpoint,
            'RASP_VAULT_STUB_SECRETS': '1',
            'DJANGO_SETTINGS_MODULE': self.django_settings,

            'RASP_GEOBASE_DATA_PATH': self.geobase_data_path,
            'RASP_GEOBASE_LAZY_LOAD': '1',

            'RASP_TESTS_MONGO_HOST': self.mongo_host,
            'RASP_TESTS_MONGO_PORT': self.mongo_port,
            'RASP_TESTS_MONGO_DB': self.mongo_db,
        }

        env.update(self._get_env_from_db_settings('DB1', self.db1_settings))
        env.update(self._get_env_from_db_settings('DB2', self.db2_settings))
        env.update(self._get_env_from_db_settings('MAINTENANCE', self.maintenance_settings))

        return env

    def call(self, endpoint, env=None, args=()):
        with Timer('run process: {}'.format(endpoint)):
            process_env = os.environ.copy()
            process_env.update(self._get_env(endpoint))
            if process_env.get('PYTHONIOENCODING') is None:
                process_env['PYTHONIOENCODING'] = 'utf_8'

            if env:
                process_env.update(env)

            cmd = [sys.executable] + list(args)

            proc = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, env=process_env, bufsize=1)
            for line in iter(proc.stdout.readline, b''):
                try:
                    line = line.decode('utf8')
                    msg = "[{}] {}".format(endpoint, line.rstrip()).encode('utf8')
                except UnicodeDecodeError:
                    msg = b"[{}] {}".format(str(endpoint), line.rstrip())

                log.info(msg)

            proc.wait()

            if proc.returncode != 0:
                raise Exception('{} failed: {}'.format(endpoint, proc.returncode))

            return proc

    def init_logging(self):
        log_format = b'%(name)s %(process)d %(asctime)s %(levelname)s: %(message)s'
        formatter = logging.Formatter(log_format)

        handler = logging.StreamHandler()
        handler.setFormatter(formatter)
        log.addHandler(handler)

        log_path = os.path.join(self.work_dir, 'integration.log')
        handler = logging.FileHandler(log_path, mode='w')
        handler.setFormatter(formatter)
        log.addHandler(handler)

        log.level = logging.DEBUG

        return log


def main():
    # run_script()
    it = IntegrationTests()
    it.run()


def run_script():
    """
    Пример запуска отдельного скрипта в таком же окружении, как интеграционные тесты.
    Для дебага.
    """
    it = IntegrationTests()
    env = it._get_env('')
    os.environ.update(env)
    import travel.rasp.admin.scripts.load_project  # noqa

    # from travel.rasp.admin.scripts.schedule.tis_train import import_tis
    # import_tis.do_import()
    from travel.rasp.admin.scripts.make_fresh_dump import upload_mysql_dump_schema

    print('upload_mysql_dump_schema')
    export_folder = '/home/monitorius/work/arc/arcadia/travel/rasp/admin/bin/tests_integration/www/db/scripts/export/'
    dump_path = '/home/monitorius/work/arc/arcadia/travel/rasp/admin/bin/tests_integration/www/db/scripts/export/schema_20170201000000_switching_main1_rasp_service_db.sql.gz'
    upload_mysql_dump_schema(dump_path, export_folder)
    print('upload_mysql_dump_schema done')


if __name__ == '__main__':
    main()
