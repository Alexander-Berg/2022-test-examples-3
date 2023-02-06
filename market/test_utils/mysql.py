# -*- coding: utf-8 -*-

import os
import errno
import subprocess
import time
import tarfile
import pwd
import textwrap
import market.pylibrary.database as database

import pytest


class MySqlStarter(object):
    CONFIG_TEMPLATE = '''
                         [mysqld]
                         user                = {user}
                         pid-file            = {base_dir}/mysqld.pid
                         port                = {port}
                         basedir             = {base_dir}
                         datadir             = {base_dir}/{data_dir}/
                         lc-messages-dir     = {base_dir}/{messages_dir}
                         skip-external-locking
                         bind-address        = 127.0.0.1
                         log_error           = {base_dir}/mysql_error.log
                    '''.strip()

    def __init__(self, work_path, mysql_archive, port_manager=None, port=None):
        self.work_path = work_path
        self.mysql_path = os.path.join(self.work_path, 'mysqld')
        self.my_cnf_path = os.path.join(self.mysql_path, 'my.cnf')
        self.socket_path = os.path.join(self.mysql_path, 'varlibmysql', 'mysql.sock')
        self._port_manager = port_manager
        self.port = port or self._port_manager.get_port()
        self._mysql_archive = mysql_archive

    def _start_mysql(self):
        try:
            os.makedirs(self.mysql_path)
        except OSError as e:
            if e.errno != errno.EEXIST:
                raise
        mysql_parameters = {
            'base_dir': self.mysql_path,
            'data_dir': 'varlibmysql',
            'user': pwd.getpwuid(os.getuid()).pw_name,
            'messages_dir': 'share/english',
            'port': self.port,
        }

        with open(self.my_cnf_path, 'w') as mycnf:
            f = self.CONFIG_TEMPLATE.format(**mysql_parameters)
            mycnf.write(f)

        subprocess.call(
            [
                os.path.join(self.mysql_path, "scripts", "mysql_install_db"),
                '--user={}'.format(mysql_parameters['user']),
                '--basedir={}'.format(mysql_parameters['base_dir']),
                '--datadir={}'.format(mysql_parameters['data_dir']),
                '--skip-name-resolve',
                '--no-defaults'
            ],
            cwd=self.mysql_path,
        )
        subprocess.Popen(
            [
                os.path.join(self.mysql_path, 'bin', 'mysqld_safe'),
                '--defaults-file={}'.format(self.my_cnf_path),
                '--socket={}'.format('mysql.sock'),
                '--skip-syslog'
            ],
            cwd=self.mysql_path,
        )

        def check_alive(path, retry, delay):
            for i in range(retry):
                if os.path.exists(path):
                    return True
                time.sleep(delay)
            return False

        if not check_alive(self.socket_path, retry=20, delay=1):
            raise RuntimeError('Mysql did not start')

        subprocess.Popen(
            [
                os.path.join(self.mysql_path, 'bin', 'mysqladmin'),
                '--defaults-file={}'.format(self.my_cnf_path),
                '--socket={}'.format('varlibmysql/mysql.sock'),
                '--user=root',
                'password',
                ''
            ],
            cwd=self.mysql_path,
        )

    def _download_mysql(self):
        if os.path.exists(self.mysql_path):
            return
        if not os.path.exists(self.work_path):
            os.makedirs(self.work_path)
        path_archive = self._mysql_archive
        tar_file = tarfile.open(path_archive, "r")
        tar_file.extractall(self.work_path)
        os.remove(path_archive)

    def shutdown_mysql(self):
        self._download_mysql()
        p = subprocess.Popen(
            [
                os.path.join(self.mysql_path, 'bin', 'mysqladmin'),
                '--defaults-file={}'.format(self.my_cnf_path),
                '--socket={}'.format('varlibmysql/mysql.sock'),
                'shutdown',
                '--user=root',
                'password',
                ''
            ],
            cwd=self.mysql_path,
        )
        p.wait()
        if self._port_manager:
            self._port_manager.release()

    def start_mysql(self):
        self._download_mysql()
        self._start_mysql()

    def write_datasources(self, path):
        with open(path, 'w') as datasources_fd:
            datasources_fd.write(textwrap.dedent('''\
                [marketindexer.super]
                host=localhost
                port={}
                name=marketindexer2super
                user=root
                password=
            '''.format(self.port)))


@pytest.fixture(scope='session')
def mysql():
    """Поднимает локальный инстанс MySql для тестов"""
    import yatest.common
    from yatest.common import network
    mysql = MySqlStarter(
        work_path=yatest.common.work_path(),
        mysql_archive=yatest.common.build_path('market/pylibrary/mindexerlib/test_utils/bigdata/mysqld.tgz'),
        port_manager=network.PortManager(),
    )
    try:
        mysql.start_mysql()
        yield mysql
    finally:
        mysql.shutdown_mysql()


@pytest.fixture()
def reusable_mysql(mysql):
    """Фикстура для MySql с меньшим scope по завершению теста дропает базу"""
    try:
        yield mysql
    finally:
        database.drop_database({
            'hosts': ['localhost'],
            'port': mysql.port,
            'db': 'marketindexer2super',
            'user': 'root',
            'passwd': '',
            'drivername': 'mysql+pymysql'
        })
