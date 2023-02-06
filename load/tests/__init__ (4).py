import os
import json
from datetime import datetime
import subprocess

from django.test import Client
from django.test import TestCase

import settings
from common.models import JobImbalance, Server, Job, Component, Task
from common.util.meta import CLICKHOUSE_DB_STRUCT
from common.util.clients import MemCache, ClickhouseException, ClickhouseClient, CacheClient, StartrekClient, MDSClient


CONFIGINFO_YAML = ''' \
android: {enabled: false, package: yandextank.plugins.Android}
autostop:
  autostop: []
  enabled: true
  package: yandextank.plugins.Autostop
  report_file: autostop_report.txt
bfg: {enabled: false, package: yandextank.plugins.Bfg}
console: {enabled: false, package: yandextank.plugins.Console}
core: {affinity: '', api_jobno: 2018-05-04_10-33-34.256380, artifacts_base_dir: /var/lib/tankapi/tests,
  artifacts_dir: /var/lib/tankapi/tests/2018-05-04_10-33-34.256380, cmdline: /usr/lib/yandex/yandex-tank/bin/tankapi
    -6, lock_dir: /var/lock/, message: '', pid: 325287, taskset_path: taskset, uuid: 9cafeb1d-318a-46ff-bd73-1d7dda243af0}
influx: {enabled: false, package: yandextank.plugins.Influx}
jmeter: {enabled: false, jmeter_path: /usr/local/apache-jmeter-3.3/bin/jmeter, package: JMeter}
json_report: {enabled: true, monitoring_log: monitoring.log, package: yandextank.plugins.JsonReport,
  test_data_log: test_data.log}
overload: {enabled: false, package: yandextank.plugins.DataUploader}
pandora: {enabled: false, package: yandextank.plugins.Pandora, pandora_cmd: /usr/local/bin/pandora}
phantom:
  additional_libs: []
  address: f2nd-test-tank.haze.yandex.net:8080
  affinity: ''
  ammo_limit: -1
  ammo_type: phantom
  ammofile: https://storage-int.mds.yandex.net/get-load-ammo/23470/15e81abb61524a0580a23e6e0a8e08d3
  autocases: 0
  buffered_seconds: 2
  cache_dir: /var/lib/tankapi/tests/stpd-cache
  chosen_cases: ''
  client_certificate: ''
  client_cipher_suites: ''
  client_key: ''
  config: ''
  connection_test: true
  enabled: true
  enum_ammo: false
  file_cache: 8192
  force_stepping: 0
  gatling_ip: ''
  header_http: '1.0'
  headers: ['Connection:close']
  instances: 10
  load_profile: {load_type: rps, schedule: 'const(100,30s)'}
  loop: -1
  method_options: ''
  method_prefix: method_stream
  multi: []
  package: yandextank.plugins.Phantom
  phantom_http_entity: 8M
  phantom_http_field: 8K
  phantom_http_field_num: 128
  phantom_http_line: 1K
  phantom_modules_path: /usr/lib/phantom
  phantom_path: phantom
  phout_file: ''
  port: ''
  source_log_prefix: ''
  ssl: false
  tank_type: http
  threads: null
  timeout: 11s
  uris: []
  use_caching: true
  writelog: '0'
rcassert: {enabled: true, fail_code: 10, package: yandextank.plugins.RCAssert, pass: ''}
rcheck: {disk_limit: 2048, enabled: true, interval: 10s, mem_limit: 1024, package: yandextank.plugins.ResourceCheck}
shellexec: {catch_out: false, enabled: true, end: '', package: yandextank.plugins.ShellExec,
  poll: '', post_process: '', prepare: '', start: ''}
telegraf: {config: firestarter_mconf_f2nd_1525419214.24.xml, config_contents: '

    <Monitoring>

    <Host address="[target]" />

    </Monitoring>

    ', default_target: localhost, disguise_hostnames: false, enabled: true, kill_old: false,
  package: yandextank.plugins.Telegraf, ssh_timeout: 5s}
uploader:
  api_address: https://lunapark.yandex-team.ru/
  api_attempts: 60
  api_timeout: 10
  chunk_size: 500000
  component: ''
  connection_timeout: 30
  enabled: true
  ignore_target_lock: false
  job_dsc: ''
  job_name: '[phantom][multitag]'
  jobno_file: jobno_file.txt
  lock_targets: auto
  log_data_requests: true
  log_monitoring_requests: true
  log_other_requests: true
  log_status_requests: true
  maintenance_attempts: 10
  maintenance_timeout: 60
  meta: {ammo_path: 'https://storage-int.mds.yandex.net/get-load-ammo/23470/15e81abb61524a0580a23e6e0a8e08d3 ',
    cmdline: /usr/lib/yandex/yandex-tank/bin/tankapi -6, jobno: 1852312, launched_from: firestarter,
    loop_count: 999, multitag: true, target_host: f2nd-test-tank.haze.yandex.net,
    target_port: 8080, use_tank: ''}
  network_attempts: 60
  network_timeout: 10
  notify: []
  operator: f2nd
  package: yandextank.plugins.DataUploader
  send_status_period: 10
  strict_lock: false
  target_lock_duration: 30m
  task: LOAD-264
  threads_timeout: 60
  ver: ''
  writer_endpoint: ''
'''

CONFIGINFO_INI = ''' \
[tank]
plugin_rcheck = yandextank.plugins.ResourceCheck
plugin_shellexec = yandextank.plugins.ShellExec
plugin_phantom = yandextank.plugins.Phantom
plugin_aggregate = yandextank.plugins.Aggregator
plugin_autostop = yandextank.plugins.Autostop
plugin_telegraf = yandextank.plugins.Telegraf
plugin_console = yandextank.plugins.Console
plugin_tips = yandextank.plugins.TipsAndTricks
plugin_rcassert = yandextank.plugins.RCAssert
plugin_jsonreport = yandextank.plugins.JsonReport
artifacts_base_dir = logs
plugin_uploader = yandextank.plugins.DataUploader
plugin_loadosophia =
plugin_web =
plugin_graphite =
plugin_dolbilo =
pid = 935112
api_jobno = 2017-08-21_12-22-15.097921

[bfg]
ammo_type = caseline

[overload]
api_address = https://overload.yandex.net/

[telegraf]
disguise_hostnames = 0

[monitoring]
disguise_hostnames = 0
config = monitoring.conf
ssh_timeout = 30s

[rcheck]
mem_limit = 1024

[aggregator]
verbose_histogram = 1

[phantom]
address = [back01h.afisha.load.yandex.net]:443
cache_dir = /var/lib/tankapi/tests/stpd-cache
header_http = 1.1
rps_schedule = const(1, 10m)
ammofile = /var/bmpt/tmp/jenkins/afisha/backend/afisha.back.3_x_places__schedule_info.ammo
writelog = proto_warning
ssl = 1
autocases = 0

[meta]
api_address = https://lunapark.yandex-team.ru/
log_data_requests = 1
log_monitoring_requests = 1
log_status_requests = 1
log_other_requests = 1
jenkinsbuild = https://jenkins-load.yandex-team.ru/job/afisha-backend-sla/275/
jenkinsjob = https://jenkins-load.yandex-team.ru/job/afisha-backend-sla/
task = CADMIN-1881
job_name = 3_x_places__schedule_info
job_dsc = yandex-afisha-backend=1.3.303-3-unstable~2017-08-18~1.gbp52f33a yandex-afisha-backend-api=1.3.303-3-unstable~2017-08-18~1.gbp52f33a yandex-afisha-backend-api-nginx-config=1.0.13
ver = yandex-afisha-backend=1.3.303-3-unstable~2017-08-18~1.gbp52f33a yandex-afisha-backend-api=1.3.303-3-unstable~2017-08-18~1.gbp52f33a yandex-afisha-backend-api-nginx-config=1.0.13
notify =
lock_targets = back01h.afisha.load.yandex.net afisha-srv01h.load.afisha.yandex.net afisha-srv02h.load.afisha.yandex.net afisha-srv03h.load.afisha.yandex.net mrs.afisha.load.yandex.net sas1-9660.media.yandex.net sas1-9031.media.yandex.net
component =

[jmeter]
jmeter_path = /usr/lib/yandex/apache-jmeter/bin/jmeter

[autostop]
autostop = quantile(95,570ms,5s) http(4xx,5%,5s) http(5xx,1,1s) net(xx,1,1s)

[shellexec]
post_process =
prepare =

'''


def select_patched(self, query, query_params=None, deserialize=True):
    """
    For ClickhouseClient
    :param self:
    :param query:
    :param query_params:
    :param deserialize:
    :return:
    """
    query = self._prepare_query(query, query_params=query_params)
    query = query.rstrip().rstrip(';') + ' FORMAT JSONCompact'
    query = query.replace('loaddb.', 'test_db.')
    assert query.lstrip()[:10].lower().startswith('select')
    r = self._post(query)
    if r.status_code != 200:
        raise ClickhouseException('{}\n{}\n=====\n{}\n===='.format(r.status_code, r.content.rstrip('\n'), query))
    if deserialize:
        return json.loads(r.content)['data']
    else:
        return r.content


def clickhouse_required(func):
    """
    Unittest decorator. Creates test Clickhouse database and drops it after test.
    It's important because data should contain jobs ids which are unique for each test.

    Realized as a decorator to make database creation test-specific, not global.
    """

    def wrapper(*args, **kwargs):
        # args[0] для теста это его self
        job1_id = args[0].job1.id
        job2_id = args[0].job2.id

        # TODO: move test resources to sandbox?
        db_data_source = {
            'j1': {
                'rt_quantiles':
                    'https://storage-int.mds.yandex.net/get-load-ammo/24135/ad125a7209674cd3ac299613844651da',
                'rt_microsecond_details':
                    'https://storage-int.mds.yandex.net/get-load-ammo/29344/fbfc8692659e45bca88ec1cd803204e8',
                'rt_microsecond_histograms':
                    'https://storage-int.mds.yandex.net/get-load-ammo/21373/28fe9e99e8984b4ebf73a9afb56d7251',
                'monitoring_verbose_data':
                    'https://storage-int.mds.yandex.net/get-load-ammo/29344/2e356b3c2fcd445bb0ad19b1b8b388b7',
                'proto_codes':
                    'https://storage-int.mds.yandex.net/get-load-ammo/24135/59610c842a524a859116500158d99a69',
                'net_codes':
                    'https://storage-int.mds.yandex.net/get-load-ammo/24135/a74843a215c74eccb6ba82231e96ae3d',
            },
            'j2': {
                'rt_quantiles':
                    'https://storage-int.mds.yandex.net/get-load-ammo/29344/8016b736a70d4f78ac927ecb9cb58895',
                'rt_microsecond_details':
                    'https://storage-int.mds.yandex.net/get-load-ammo/29344/78d4d350b806475e90ebbb4da4a6efd2',
                'rt_microsecond_histograms':
                    'https://storage-int.mds.yandex.net/get-load-ammo/24135/dff5d688fc404c76a53a521859fd9f23',
                'monitoring_verbose_data':
                    'https://storage-int.mds.yandex.net/get-load-ammo/29344/3a3f9cc3de654728bbbfb736492e92aa',
                'proto_codes':
                    'https://storage-int.mds.yandex.net/get-load-ammo/21373/00f33621d28e4041948310b83e19c585',
                'net_codes':
                    'https://storage-int.mds.yandex.net/get-load-ammo/29344/c61eaae15ed34da7b9829ac45eba54ba',
            },
        }

        mds = MDSClient()

        base_dir = settings.BASE_DIR + '/www/common/tests/test_resources/'
        if not os.path.exists(base_dir):
            os.mkdir(base_dir)

        # Проверяем есть ли файлы с данными для кликхауса, если нет, то загружаем из MDS
        for j, f in list(db_data_source.items()):
            for f_name in f:
                file_path = base_dir + j + '_' + f_name
                if not os.path.exists(file_path):
                    data = mds.get(db_data_source[j][f_name])['content']
                    assert data, 'Could not retrieve clickhouse data for {}_{}'.format(j, f_name)
                    with open(file_path, 'wb') as file_obj:
                        file_obj.write(data)

        # Делаем структуру базы в кликхаусе
        with open('/tmp/clickhouse_struct', 'w') as f:
            f.write('''CREATE DATABASE IF NOT EXISTS loaddb;
            ''' + CLICKHOUSE_DB_STRUCT
                    )
        subprocess.check_call('clickhouse-client --multiquery < /tmp/clickhouse_struct', shell=True)

        # Заполняем базу данными, подменяя номера стрельб актуальными для текущего теста
        for f_name in os.listdir(base_dir):
            if f_name.startswith('j1'):
                with open(base_dir + f_name) as f:
                    data = f.read().strip()
                data %= {'job_id': job1_id}
                with open('/tmp/' + f_name, 'w') as file_obj:
                    file_obj.write(data)
            elif f_name.startswith('j2'):
                with open(base_dir + f_name) as f:
                    data = f.read().strip()
                data %= {'job_id': job2_id}
                with open('/tmp/' + f_name, 'w') as file_obj:
                    file_obj.write(data)

            subprocess.check_call(
                'clickhouse-client --database=loaddb --query="INSERT INTO {} FORMAT CSV" < /tmp/{}'.format(
                    f_name.split('_', 1)[1], f_name), shell=True
            )
        try:
            # Выполняем тест
            # with mock.patch.object(ClickhouseClient, 'select', select_patched):
            return func(*args, **kwargs)
        finally:
            # Дропаем базу (потому что для других юниттестов нужна будет база с другими номерами стрельб.)
            subprocess.check_call('echo "drop database loaddb" | clickhouse-client', shell=True)

    return wrapper


class CommonTestCase(TestCase):
    def setUp(self):
        """
        setting up fake tasks and jobs before every test
        """
        # TODO: надо чтобы схемы нагрузки были разными для стрельб. и разнообразными, чтобы тестить хелперы, например.
        # TODO: Нужна еще мультитеговая стрельба..

        self.client = Client()
        self.tank = Server()
        self.tank.save()

        self.task1 = Task(key='LOAD-204')
        self.task2 = Task(key='SANDBOX-204')

        self.job1 = Job(  # 1674266
            task=self.task1.key,
            fd=datetime.strptime('2017-11-14 00:18:56', '%Y-%m-%d %H:%M:%S'),
            td=datetime.now(),
            configinfo=CONFIGINFO_YAML,
        )
        self.job2 = Job(  # 1674271
            task=self.task1.key,
            fd=datetime.strptime('2017-11-14 00:25:50', '%Y-%m-%d %H:%M:%S'),
            td=datetime.now(),
            configinfo=CONFIGINFO_INI,
        )
        self.job1.save()
        self.job2.save()

