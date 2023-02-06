# coding: utf-8
import os
import unittest
from datetime import datetime

import yatest.common

import logkeeper_minion.main as lm
from market.sre.tools.logkeeper.proto.logkeeper_pb2 import (
    LogMinion,
    LogApiReturn,
    LogGetConfig,
    LogConfig,
    LogSecrets,
    LogFile,
)

LOG_CONFIG_1 = LogConfig(
    DST_DIR='foo_dir',
    APP='foo_app',
    PATTERN='foo-error.log.*gz',
    SERVICE_NAME=['foo_service_sas', 'foo_service_vla'],
    LIFETIME=180,
)

LOG_CONFIG_2 = LogConfig(
    DST_DIR='bar_dir',
    APP='bar_app',
    PATTERN='bar-error.log.*gz',
    SERVICE_NAME=['bar_service_sas', 'bar_service_vla'],
    LIFETIME=180,
    KEEP_FILE=True,
)

LOG_SECRETS = LogSecrets(
    S3_ACCESS_KEY_ID='s3_sccess_key_id',
    S3_ACCESS_SECRET_KEY='s3_access_secret_key',
    S3_API_ENDPOINT='s3_api_endpoint',
    S3_BUCKET_NAME='s3_bucket_name',
)

LOG_GET_CONFIG_1 = LogGetConfig(secrets=LOG_SECRETS, configs=[LOG_CONFIG_1])
LOG_GET_CONFIG_2 = LogGetConfig(secrets=LOG_SECRETS, configs=[LOG_CONFIG_2])
LOG_API_RETURN_1 = LogApiReturn(statusCode=200, logGetConfig=LOG_GET_CONFIG_1)
LOG_API_RETURN_2 = LogApiReturn(statusCode=200, logGetConfig=LOG_GET_CONFIG_2)
LOG_MINION_1 = LogMinion(host='foo.market.yandex.net', service='foo_service_vla')
LOG_MINION_2 = LogMinion(host='bar.market.yandex.net', service='bar_service_vla')
LOG_FILE_1 = LogFile(
    baseDir='/mfsroot/public',
    fullPath=os.path.join(
        LOG_CONFIG_1.DST_DIR, LOG_MINION_1.service, LOG_MINION_1.host, LOG_CONFIG_1.APP, 'foo-error.log.1.gz'
    ),
    group='runtime',
    config=LOG_CONFIG_1.APP,
    host=LOG_MINION_1.host,
    mdate='2018-07-25',
    ttl=LOG_CONFIG_1.LIFETIME,
    size=40,
    tskv=False,
)
LOG_FILE_2 = LogFile(
    baseDir='/mfsroot/public',
    fullPath=os.path.join(
        LOG_CONFIG_2.DST_DIR, LOG_MINION_2.service, LOG_MINION_2.host, LOG_CONFIG_2.APP, 'bar-error.log.gz'
    ),
    group='runtime',
    config=LOG_CONFIG_2.APP,
    host=LOG_MINION_2.host,
    mdate='2018-07-25',
    ttl=LOG_CONFIG_2.LIFETIME,
    size=38,
    tskv=False,
)


class TestMinion(unittest.TestCase):
    source_dir = yatest.common.source_path('market/sre/tools/logkeeper-minion/tests/fixtures')

    def test_parsion_environment_variables(self):
        valid_env = dict()
        valid_env['HOST'] = 'foo.market.yandex.net'
        valid_env['SERVICE'] = 'foo_service_vla'
        valid_env['LOGSDIR'] = '/var/logs/yandex'
        self.assertEqual(lm.get_environment_variables(valid_env), (LOG_MINION_1, '/var/logs/yandex'))

        with self.assertRaises(SystemExit) as e:
            invalid_env = dict()
            invalid_env['HOST'] = 'foo.market.yandex.net'
            lm.get_environment_variables(invalid_env)
            self.assertEqual(e.exception.code, 1)

    def test_valid_exit(self):
        with self.assertRaises(SystemExit) as e:
            lm.exit_without_error('some args')
            self.assertEqual(e.exception.code, 0)

    def test_invalid_exit(self):
        with self.assertRaises(SystemExit) as e:
            lm.exit_with_error('some args')
            self.assertEqual(e.exception.code, 1)

    def test_get_logfile(self):
        mdate_file_1 = os.path.getmtime(os.path.join(self.source_dir, 'logs', 'foo_app', 'foo-error.log.1.gz'))
        mdate_file_2 = os.path.getmtime(os.path.join(self.source_dir, 'logs', 'bar_app', 'bar-error.log.gz'))
        LOG_FILE_1.mdate = datetime.utcfromtimestamp(mdate_file_1).strftime('%Y-%m-%d')
        LOG_FILE_2.mdate = datetime.utcfromtimestamp(mdate_file_2).strftime('%Y-%m-%d')
        for logfile, filepath, keep_file in lm.get_logfile(
            LOG_API_RETURN_1.logGetConfig, LOG_MINION_1, os.path.join(self.source_dir, 'logs')
        ):
            self.assertEquals(logfile, LOG_FILE_1)
            self.assertFalse(keep_file)
        for logfile, filepath, keep_file in lm.get_logfile(
            LOG_API_RETURN_2.logGetConfig, LOG_MINION_2, os.path.join(self.source_dir, 'logs')
        ):
            self.assertEquals(logfile, LOG_FILE_2)
            self.assertTrue(keep_file)


if __name__ == '__main__':
    unittest.main()
