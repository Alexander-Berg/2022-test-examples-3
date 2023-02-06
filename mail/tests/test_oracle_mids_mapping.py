import yatest.common
import psycopg2
import logging
import os
import requests
import time
from mail.oracle_mids_mapping.lib.database import DB
import subprocess
from mail.oracle_mids_mapping.lib.server import parse

ORACLE_MIDS_MAPPING_PORT = "8090"
PROC_NAME = 'oracle_mids_mapping'
MAX_START_RETRIES = 3
DOCKER_CONTAINER_NAME = 'test_db_container'
DB_PASSWORD = 'dbpassword'
logger = logging.getLogger("oracle_mids_mapping")
work_dir = yatest.common.work_path()


class TestOracleMidsMapping():
    @classmethod
    def setup_class(cls):
        cls.oracle_mids_mapping = TestOracleMidsMapping()
        cls.oracle_mids_mapping.start_daemon()
        cls.oracle_mids_mapping.start_hound()
        cls.oracle_mids_mapping.start_db()

        cls.oracle_mids_mapping.add_data_to_db()

    def get_daemon_ping(self, port):
        try:
            r = requests.get("http://localhost:" + port + "/ping")
            return r.text.strip() == 'pong'
        except:
            return False

    def ping_docker_container(self):
        cmd = ['docker', 'inspect', '-f', '\'{{.State.Running}}\'', DOCKER_CONTAINER_NAME]
        res = subprocess.call(cmd)
        if res == 0:
            return True
        else:
            return False

    def shutdown_daemon(self):
        os.system("ps -C " + PROC_NAME + " -o pid=|xargs kill -9")
        # os.system("pgrep " + PROC_NAME + " |xargs kill -9") # FOR MAC LOCAL DEBUG

    def start_daemon(self):
        config = yatest.common.source_path("mail/oracle_mids_mapping/tests/conf.yaml")

        oracle_mid_mapping_bin = yatest.common.binary_path("mail/oracle_mids_mapping/daemon/oracle_mids_mapping")

        self.shutdown_daemon()

        cmd = [oracle_mid_mapping_bin, "-c", config]
        ora_mids_mapping_execution = yatest.common.execute(cmd, shell=False, check_sanitizer=True, wait=False, cwd=work_dir)
        retries = 0
        while retries < MAX_START_RETRIES and not self.get_daemon_ping(ORACLE_MIDS_MAPPING_PORT):
            time.sleep(4)
            retries += 1

        if retries == MAX_START_RETRIES:
            raise Exception('Can\'t ping oracle_mids_mapping http server')

        return ora_mids_mapping_execution

    def stop_hound(self):
        try:
            os.system("ps | grep hound.py | grep -v grep | awk '{print $1}' | xargs kill -9")
        except Exception as e:
            assert e is None

    def start_hound(self):
        self.stop_hound()
        path = yatest.common.source_path("mail/oracle_mids_mapping/tests/hound.py")
        cmd = ['python2.7', path]
        execution = yatest.common.execute(cmd, shell=False, check_sanitizer=True, wait=False, cwd=work_dir)
        time.sleep(2)
        assert self.get_daemon_ping('9999')
        return execution

    def start_db(self):
        self.destroy_db()
        cmd = ['docker',
               'run',
               '-p',
               '12000:12000',
               '-e OPT_pgbouncer_listen_port=12000',
               '-e OPT_post_sql="DROP SCHEMA IF EXISTS mapping;\
               CREATE SCHEMA IF NOT EXISTS mapping;\
               CREATE TABLE mapping.ora_mids_mapping (\
               ora_mid bigint primary key,\
               pg_mid bigint,\
               uid bigint);"',
               '-e OPT_db_user=mlmapping',
               '-e OPT_db_passwd={}'.format(DB_PASSWORD),
               '-e OPT_db_name=mlmappingdb',
               '--name', DOCKER_CONTAINER_NAME,
               '-t',
               '-d',
               '-i',
               'registry.yandex.net/dbaas/minipgaas']

        start_db_execution = yatest.common.execute(cmd, shell=True, wait=True, cwd=work_dir)
        retries = 0
        while retries < MAX_START_RETRIES and not self.ping_docker_container():
            time.sleep(4)
            retries += 1

        if retries == MAX_START_RETRIES:
            raise Exception('Can\'t start docker container')

        return start_db_execution

    def destroy_db(self):
        try:
            cmd = ['docker', 'stop', DOCKER_CONTAINER_NAME]
            yatest.common.execute(cmd, shell=False, wait=True, cwd=work_dir)
            time.sleep(5)
        except Exception as e:
            logger.error('Docker stop error:{}'.format(str(e)))
        try:
            cmd = ['docker', 'rm', DOCKER_CONTAINER_NAME]
            yatest.common.execute(cmd, shell=False, wait=True, cwd=work_dir)
            time.sleep(5)
        except Exception as e:
            logger.error('Docker rm error:{}'.format(str(e)))

        return 0

    def create_db_conf(self):
        conf = {}
        conf['DB'] = {}
        conf['DB']['DBName'] = 'mlmappingdb'
        conf['DB']['TableName'] = 'mapping.ora_mids_mapping'
        conf['DB']['Host'] = 'localhost'
        conf['DB']['Port'] = 12000
        conf['DB']['User'] = 'mlmapping'
        conf['DB']['Password'] = 'dbpassword'
        return conf

    def add_data_to_db(self):
        conf = self.create_db_conf()
        db = DB(conf)
        retries = 0
        while retries < MAX_START_RETRIES:
            try:
                db.cursor().execute('INSERT INTO mapping.ora_mids_mapping VALUES (13, 438, 31819)')
                db.cursor().execute('INSERT INTO mapping.ora_mids_mapping VALUES (1849, 441, 138)')
                db.cursor().execute('INSERT INTO mapping.ora_mids_mapping VALUES (1049, 5138, 2188)')
                db.cursor().execute('INSERT INTO mapping.ora_mids_mapping VALUES (1, 11, 2188)')
                db.cursor().execute('INSERT INTO mapping.ora_mids_mapping VALUES (2, 22, 2188)')
                db.cursor().execute('INSERT INTO mapping.ora_mids_mapping VALUES (3, 33, 2188)')
                db.cursor().execute('INSERT INTO mapping.ora_mids_mapping VALUES (4, 44, 2188)')
                db.cursor().execute('INSERT INTO mapping.ora_mids_mapping VALUES (5, 55, 2188)')
                db.db().commit()
                break
            except psycopg2.DatabaseError as e:
                logger.info("DB error: " + str(e))
                retries += 1
                time.sleep(15)

        if retries == MAX_START_RETRIES:
            raise Exception("DB don't starting.")

        return 0

    def test_uid_by_mid(self):
        r = requests.get("http://localhost:" + ORACLE_MIDS_MAPPING_PORT + "/uid_by_mid?mid={}".format(1849))
        assert r.status_code == 200
        assert r.json() == {"uid": 138}

        r = requests.get("http://localhost:" + ORACLE_MIDS_MAPPING_PORT + "/uid_by_mid?mid={}".format(13))
        assert r.status_code == 200
        assert r.json() == {"uid": 31819}

        r = requests.get("http://localhost:" + ORACLE_MIDS_MAPPING_PORT + "/uid_by_mid?mid={}".format(1049))
        assert r.status_code == 200
        assert r.json() == {"uid": 2188}

        r = requests.get("http://localhost:" + ORACLE_MIDS_MAPPING_PORT + "/uid_by_mid?mid={}".format(1111))
        assert r.status_code == 404
        assert r.text.strip().find('Not Found') != -1

    def test_parse(self):
        default = 'd'

        def log_func(msg):
            print(msg)

        resp = ''
        assert default == parse(resp, default, log_func)

        resp = 'zzz'
        assert default == parse(resp, default, log_func)

        resp = '{"a": 1, "b": "x"}'
        assert default == parse(resp, default, log_func)

        resp = '{"envelopes": 1}'
        assert default == parse(resp, default, log_func)

        resp = '{"envelopes": []}'
        assert default == parse(resp, default, log_func)

        resp = '{"envelopes": [{"a": 1}, {"b": 2}]}'
        assert default == parse(resp, default, log_func)

        resp = '{"envelopes": [{"a": 1}]}'
        assert default == parse(resp, default, log_func)

        resp = '{"envelopes": [{"threadId": "tid"}]}'
        assert "tid" == parse(resp, default, log_func)

    def test_messages_by_thread(self):
        r = requests.get("http://localhost:" + ORACLE_MIDS_MAPPING_PORT + "/messages_by_thread?tid={}&uid=179".format(1849), allow_redirects=False)
        assert r.status_code == 302
        assert r.headers['Location'] == 'https://metacorp.mail.yandex.net:443/messages_by_thread?uid=179&tid={}'.format('441')

        r = requests.get("http://localhost:" + ORACLE_MIDS_MAPPING_PORT + "/messages_by_thread?tid={}&uid=179".format(13), allow_redirects=False)
        assert r.status_code == 302
        assert r.headers['Location'] == 'https://metacorp.mail.yandex.net:443/messages_by_thread?uid=179&tid={}'.format('438')

        r = requests.get("http://localhost:" + ORACLE_MIDS_MAPPING_PORT + "/messages_by_thread?tid={}&uid=179".format(1049), allow_redirects=False)
        assert r.status_code == 302
        assert r.headers['Location'] == 'https://metacorp.mail.yandex.net:443/messages_by_thread?uid=179&tid={}'.format('5138')

        r = requests.get("http://localhost:" + ORACLE_MIDS_MAPPING_PORT + "/messages_by_thread?tid={}&uid=179".format(1111), allow_redirects=False)
        assert r.status_code == 302
        assert r.headers['Location'] == 'https://metacorp.mail.yandex.net:443/messages_by_thread?uid=179&tid={}'.format('1111')

        r = requests.get("http://localhost:" + ORACLE_MIDS_MAPPING_PORT + "/messages_by_thread?tid={}&uid=179".format(1), allow_redirects=False)
        assert r.status_code == 302
        assert r.headers['Location'] == 'https://metacorp.mail.yandex.net:443/messages_by_thread?uid=179&tid={}'.format('999')

        r = requests.get("http://localhost:" + ORACLE_MIDS_MAPPING_PORT + "/messages_by_thread?tid={}&uid=179".format(2), allow_redirects=False)
        assert r.status_code == 302
        assert r.headers['Location'] == 'https://metacorp.mail.yandex.net:443/messages_by_thread?uid=179&tid={}'.format('22')

        r = requests.get("http://localhost:" + ORACLE_MIDS_MAPPING_PORT + "/messages_by_thread?tid={}&uid=179".format(3), allow_redirects=False)
        assert r.status_code == 302
        assert r.headers['Location'] == 'https://metacorp.mail.yandex.net:443/messages_by_thread?uid=179&tid={}'.format('33')

        r = requests.get("http://localhost:" + ORACLE_MIDS_MAPPING_PORT + "/messages_by_thread?tid={}&uid=179".format(4), allow_redirects=False)
        assert r.status_code == 302
        assert r.headers['Location'] == 'https://metacorp.mail.yandex.net:443/messages_by_thread?uid=179&tid={}'.format('44')

        r = requests.get("http://localhost:" + ORACLE_MIDS_MAPPING_PORT + "/messages_by_thread?tid={}&uid=179".format(5), allow_redirects=False)
        assert r.status_code == 302
        assert r.headers['Location'] == 'https://metacorp.mail.yandex.net:443/messages_by_thread?uid=179&tid={}'.format('55')

    @classmethod
    def teardown_class(cls):
        cls.oracle_mids_mapping.shutdown_daemon()
        cls.oracle_mids_mapping.stop_hound()
        cls.oracle_mids_mapping.destroy_db()
