import pytest
import yatest.common
import tarfile
import os
import pwd
import time
import context
from yatest.common import network


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

    def __init__(self):
        self.work_path = yatest.common.work_path()
        self.mysql_path = os.path.join(self.work_path, 'mysqld')
        self.my_cnf_path = os.path.join(self.mysql_path, 'my.cnf')
        self.socket_path = os.path.join(self.mysql_path, 'varlibmysql', 'mysql.sock')
        self.port_manager = network.PortManager()
        self.port = self.port_manager.get_port()

    def start_mysql(self):
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

        yatest.common.execute(
            [
                os.path.join(self.mysql_path, "scripts", "mysql_install_db"),
                '--defaults-file={}'.format(self.my_cnf_path),
                '--skip-name-resolve'
            ],
            check_exit_code=True,
            cwd=self.mysql_path,
            wait=True,
        )

        yatest.common.execute(
            [
                os.path.join(self.mysql_path, 'bin', 'mysqld_safe'),
                '--defaults-file={}'.format(self.my_cnf_path),
                '--socket={}'.format('./mysql.sock'),
                '--skip-syslog'
            ],
            check_exit_code=True,
            cwd=self.mysql_path,
            wait=False,
        )

        def check_alive(path, retry, delay):
            for i in range(retry):
                if os.path.exists(path):
                    return True
                time.sleep(delay)
            return False

        if not check_alive(self.socket_path, retry=20, delay=1):
            raise RuntimeError('Mysql did not start')

        yatest.common.execute(
            [
                os.path.join(self.mysql_path, 'bin', 'mysqladmin'),
                '--defaults-file={}'.format(self.my_cnf_path),
                '--socket={}'.format('varlibmysql/mysql.sock'),
                '--user=root',
                'password',
                ''
            ],
            check_exit_code=True,
            cwd=self.mysql_path,
            wait=True,
        )

    def download_mysql(self):
        if os.path.exists(self.mysql_path):
            return
        if not os.path.exists(self.work_path):
            os.makedirs(self.work_path)
        path_archive = os.path.join(self.work_path, 'mysqld.tgz')
        tar_file = tarfile.open(path_archive, "r")
        tar_file.extractall(self.work_path)
        os.remove(path_archive)

    def shutdown_mysql(self):
        yatest.common.execute(
            [
                os.path.join(self.mysql_path, 'bin', 'mysqladmin'),
                '--defaults-file={}'.format(self.my_cnf_path),
                '--socket={}'.format('varlibmysql/mysql.sock'),
                'shutdown',
                '--user=root',
                'password',
                ''
            ],
            check_exit_code=True,
            cwd=self.mysql_path,
            wait=False,
        )
        self.port_manager.release()

    def init_mysql(self):
        self.download_mysql()
        self.start_mysql()


@pytest.fixture(scope='session', autouse=True)
def mysql_setup(request):
    mysql = MySqlStarter()
    mysql.init_mysql()
    context.cleanup()
    context.setup(mysql.port)

    def mysql_teardown():
        mysql.shutdown_mysql()

    request.addfinalizer(mysql_teardown)
