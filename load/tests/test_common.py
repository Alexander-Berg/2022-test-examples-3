import json
import collections
from datetime import datetime

from django.contrib.auth.models import User
from django.test import TestCase
from django_yauth.user import create_user as get_or_create_user
from unittest import mock

from common.models import JobImbalance, Server, Component, MobileJobDataKey
from mobilepage.models import MobileJob
from common.tests import CommonTestCase, clickhouse_required
from common.util.clients import StartrekClient, CacheClient, MemCache, ClickhouseClient
from common.util.exceptions import TaskError
from common.util.aggregators import MonitoringAggregator, \
    RTHistogramsAggregator, RTDetailsAggregator, ProtoCodesAggregator, NetCodesAggregator, Aggregator


class UserDummy:
    default_email = 'default@email.ru'

    def __init__(self, login='lunapark'):
        self.login = login

    @staticmethod
    def is_authenticated():
        return True

    def get_username(self):
        return self.login


class CreateUserTest(CommonTestCase):
    def test_get_or_create_user(self):
        test_user = UserDummy('pytest_user')
        get_or_create_user(test_user)
        user = User.objects.get(username='pytest_user')
        self.assertEqual('default@email.ru', user.email)


class JobPropertiesTest(CommonTestCase):
    """
    testing Job model properties
    get_job_mock and create_fake_trail methods are available for every test
    """

    def test_job_id(self):
        """
        test Job "id" property
        """
        self.assertEqual(self.job1.id, self.job1.n)

    def test_job_author(self):
        """
        test Job "id" property
        """
        self.assertEqual(self.job1.author, self.job1.person)

    def test_job_start(self):
        """
        test Job "start" property
        """
        self.assertEqual(self.job1.start, self.job1.fd)

    def test_job_imbalance(self):
        """
        test Job "imbalance" property if JobImbalance exist for this Job
        """
        self.job_imbalance1 = JobImbalance(up=self.job1,
                                           hum_isimbalance=1,
                                           rob_isimbalance=0,
                                           hum_imbalance=1111,
                                           rob_imbalance=11)
        self.job_imbalance1.save()
        self.job_imbalance2 = JobImbalance(up=self.job2,
                                           hum_isimbalance=1,
                                           rob_isimbalance=1,
                                           hum_imbalance=2222,
                                           rob_imbalance=22)
        self.job_imbalance2.save()
        self.job_imbalance3 = JobImbalance(up=self.job2,
                                           hum_isimbalance=0,
                                           rob_isimbalance=1,
                                           hum_imbalance=3333,
                                           rob_imbalance=33)
        self.job_imbalance3.save()

        self.assertEqual(self.job2.imbalance, 2222)

    def test_job_imbalance_doesnotexist(self):
        """
        test Job "imbalance" property if there is no JobImbalance for this Job
        """
        self.assertEqual(self.job1.imbalance, 0)

    def test_online_job_quit_status_text(self):
        """
        checks if quit_status_text property returns "online" for online jobs
        """
        self.job2.quit_status = None
        self.job2.save()
        self.assertEqual(self.job2.quit_status_text, "online")

    def test_completed_job_quit_status_text(self):
        """
        checks if quit_status_text property returns "completed" for jobs with quit_status = 0
        """
        self.job1.quit_status = 0
        self.job1.save()
        self.assertEqual(self.job1.quit_status_text, "completed")

    def test_interrupted_job_quit_status_text(self):
        """
        checks if quit_status_text property returns "interrupted" for jobs with quit_status = 1
        """
        self.job1.quit_status = 1
        self.job1.save()
        self.assertEqual(self.job1.quit_status_text, "interrupted_generic_interrupt")

    def test_autostop_job_quit_status_text(self):
        """
        checks if quit_status_text property returns "autostop_net" for jobs with quit_status = 23
        """
        self.job1.quit_status = 23
        self.job1.save()
        self.assertEqual(self.job1.quit_status_text, "autostop_net")

    def test_other_job_quit_status_text(self):
        """
        checks if quit_status_text property returns "autostop_net" for jobs with unknown quit_status
        """
        self.job1.quit_status = 12312
        self.job1.save()
        self.assertEqual(self.job1.quit_status_text, "other")

    def test_job_tank_reduced(self):
        """
        checks if tank_reduced property returns properly reduced tank host
        """
        self.tank.host = "tankmegatank.yandex.ru"
        self.tank.save()
        self.job1.tank = self.tank
        self.job1.save()
        self.assertEqual(self.job1.tank_reduced, "tankmegatank")

    def test_job_srv_reduced(self):
        """
        checks if srv_reduced property returns properly reduced target host
        """
        self.srv = Server.objects.create(host="target.yandex.net")
        self.job1.srv = self.srv
        self.job1.save()
        self.assertEqual(self.job1.srv_reduced, "target")

    @clickhouse_required
    def test_job_targets(self):
        """
        checks if "targets" property of Job model works properly
        """
        aa = 1
        self.assertEqual([t.host for t in self.job1.targets], ['cpumem', 'localhost', 'man1-7139.search.yandex.net'])
        self.assertEqual(
            [t.host for t in self.job2.targets],
            [
                'advisor-api01h.cloud.load.mobile.yandex.net',
                'advisor-mrs01h.cloud.load.mobile.yandex.net',
                'launcher01h.cloud.load.mobile.yandex.net'
            ]
        )

    @clickhouse_required
    def test_job_monitoring_exists(self):
        """
        checks if "targets" property of Job model works properly
        """
        self.assertTrue(self.job1.monitoring_exists)

    @clickhouse_required
    def test_job_cases(self):
        """

        :return:
        """
        aa = 1
        self.assertEqual(self.job1.cases, ['common', 'stats'])
        self.assertEqual(self.job2.cases, ['/api/v1/grouped_apps', '/api/v2/app_info', '/api/v2/recommend'])

    # @skip('Manager is not accessible via Job instances')
    @clickhouse_required
    def test_job_tags(self):
        """

        :return:
        """
        self.assertEqual(self.job1.tags, ['common', 'stats'])
        self.assertEqual(self.job2.tags, ['/api/v1/grouped_apps', '/api/v2/app_info', '/api/v2/recommend'])

    def test_job_multitag(self):
        """

        :return:
        """
        self.assertFalse(self.job1.multitag)

    # @skip('Manager is not accessible via Job instances')
    @clickhouse_required
    def test_job_monitoring_exists(self):
        """
        checks if "targets" property of Job model works properly
        """
        self.assertFalse(self.job1.monitoring_only)

    # @skip('Manager is not accessible via Job instances')
    @clickhouse_required
    def test_job_data_started(self):
        """

        :return:
        """
        self.assertEqual(self.job1.data_started, datetime(2017, 11, 14, 0, 18, 56))
        self.assertEqual(self.job2.data_started, datetime(2017, 11, 14, 0, 25, 50))

    # @skip('Manager is not accessible via Job instances')
    @clickhouse_required
    def test_job_data_stopped(self):
        """

        :return:
        """
        self.assertEqual(self.job1.data_stopped, datetime(2017, 11, 14, 0, 29, 25))
        self.assertEqual(self.job2.data_stopped, datetime(2017, 11, 14, 0, 36, 49))

    # @skip('Manager is not accessible via Job instances')
    @clickhouse_required
    def test_job_data_started_unix(self):
        """

        :return:
        """
        self.assertEqual(self.job1.data_started_unix, 1510607936)
        self.assertEqual(self.job2.data_started_unix, 1510608350)

    # @skip('Manager is not accessible via Job instances')
    @clickhouse_required
    def test_job_data_stopped_unix(self):
        """

        :return:
        """
        self.assertEqual(self.job1.data_stopped_unix, 1510608565)
        self.assertEqual(self.job2.data_stopped_unix, 1510609009)

    # @skip('Manager is not accessible via Job instances')
    @clickhouse_required
    def test_job_duration(self):
        """

        :return:
        """
        self.assertEqual(self.job1.duration, 630)
        self.assertEqual(self.job2.duration, 660)

    # @skip('Manager is not accessible via Job instances')
    @clickhouse_required
    def test_job_duration_formatted(self):
        self.assertEqual(self.job1.duration_formatted, '00:10:30')
        self.assertEqual(self.job2.duration_formatted, '00:11:00')


class TaskPropertiesTest(CommonTestCase):
    """
    testing Task model properties
    """

    def test_task_project(self):
        """
        test Task "project" property
        """
        self.assertEqual(self.task1.project, 'LOAD')
        self.assertEqual(self.task2.project, 'SANDBOX')


class ComponentPropertiesTests(CommonTestCase):
    def test_jobs(self):
        component = Component.objects.create(tag='LOAD', name='some component')

        self.job1.component = component.n
        self.job1.save()
        self.job2.component = component.n
        self.job2.save()

        self.assertEqual(len(component.jobs), 2)
        self.assertTrue(all(j in component.jobs for j in [self.job1, self.job2]))


class StartrekClientTest(TestCase):
    """
    needs access to startrek api
    """

    def setUp(self):
        self.st_client = StartrekClient()

    def test_check_task_exists(self):
        assert self.st_client.check_task_exists('LUNAPARK-2188')

    def test_check_task_exists_not(self):
        self.assertRaises(TaskError, self.st_client.check_task_exists, 'LUNAPARK-100500')


class CacheClientTests(TestCase):
    def test_init(self):
        self.assertIsInstance(CacheClient(), MemCache)


def mock_check_task_exists(*args, **kwargs):
    return True


class ViewsTests(CommonTestCase):
    @clickhouse_required
    def test_daily_jobs_per_project(self):
        resp = self.client.get('/bb/daylyjobsperproject.csv?year=2016')
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(resp.content, b'')
        resp = self.client.get('/bb/daylyjobsperproject.csv?year=2017')
        self.assertEqual(resp.status_code, 200)
        # self.assertEqual(resp.content, b'LOAD,2017-11-14,2\r\n')
        self.job2.fd = datetime.strptime('2018-04-22 00:25:50', '%Y-%m-%d %H:%M:%S')
        self.job2.save()
        resp = self.client.get('/bb/daylyjobsperproject.csv?year=2018')
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(resp.content, b'LOAD,2018-04-22,1\r\n')
        resp = self.client.get('/bb/daylyjobsperproject.csv?year=2017')
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(resp.content, b'LOAD,2017-11-14,1\r\n')

    @clickhouse_required
    def test_daily_mobilejobs_per_project(self):
        MobileJob.objects.create(task=self.task1.key, fd=datetime(2017, 11, 14))
        MobileJob.objects.create(task=self.task1.key, fd=datetime(2018, 0o4, 22))
        resp = self.client.get('/bb/daylymobilejobsperproject.csv?year=2016')
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(resp.content, b'')
        resp = self.client.get('/bb/daylymobilejobsperproject.csv?year=2017')
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(resp.content, b'LOAD,2017-11-14,1\r\n')
        resp = self.client.get('/bb/daylymobilejobsperproject.csv?year=2018')
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(resp.content, b'LOAD,2018-04-22,1\r\n')

    @mock.patch.object(StartrekClient, 'check_task_exists', mock_check_task_exists)
    def test_check_correct_task(self):
        resp = self.client.get('/util/check_task?task=LOAD-204')
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(json.loads(resp.content), True)

    def test_check_wrong_task(self):
        resp = self.client.get('/util/check_task?task=LOAD-20838348344')
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(json.loads(resp.content), False)

    def test_check_no_task(self):
        resp = self.client.get('/util/check_task')
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(json.loads(resp.content), False)

    def test_check_wrong_task_format(self):
        resp = self.client.get('/util/check_task?task=LOAD-2ssfsf0838348344')
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(json.loads(resp.content), False)

    def test_ping(self):
        resp = self.client.get('/ping')
        self.assertEqual(resp.status_code, 200)

    def test_ping_fail(self):
        with mock.patch.object(ClickhouseClient, 'select', KeyError()):
            resp = self.client.get('/ping')
        self.assertEqual(resp.status_code, 500)


class AggregatorsTests(CommonTestCase):
    maxDiff = None

    def test_count_percentage(self):
        self.assertEqual(Aggregator.count_percentage([3, 34, 6, 23, 888, 44, 3]),
                         [0.3, 3.397, 0.599, 2.298, 88.711, 4.396, 0.3])

    def test_count_quantiles(self):
        self.assertEqual(Aggregator.count_quantiles([3, 34, 6, 23, 888, 44, 3]),
                         [0.3, 3.696, 4.296, 6.593, 95.305, 99.7, 100.0])

    @clickhouse_required
    def test_monitoring_aggregator(self):
        agg = MonitoringAggregator(self.job1)
        data = agg.aggregate()
        self.assertEqual(data['cpumem']['custom:pcpusys_proc0'],
                         {'average': 3, 'minimum': 3, 'median': 3, 'maximum': 3, 'stddev': 0})
        self.assertEqual(data['cpumem']['custom:pcpuuser_proc10'],
                         {'average': 1236.087, 'minimum': 5, 'median': 1236, 'maximum': 2531, 'stddev': 762.913})
        raw_data = agg.get_raw_data()
        self.assertEqual(raw_data[29], ['cpumem', 'custom:pcpuuser_total', 17928.75, 11184.124, 47, 36767, 17859])

    @clickhouse_required
    def test_rt_details_aggregator(self):
        agg = RTDetailsAggregator(self.job1)
        self.assertEqual(
            agg.aggregate(),
            collections.OrderedDict(
                [('resps', {'average': 4950.849, 'minimum': 651, 'median': 5050, 'maximum': 5186, 'stddev': 513.772}),
                 ('threads', {'average': 0.049, 'minimum': 0.028, 'median': 0.044, 'maximum': 0.393, 'stddev': 0.02}),
                 ('expect', {'average': 5.47, 'minimum': 4.87, 'median': 5.225, 'maximum': 17.451, 'stddev': 1.188}),
                 ('connect_time',
                  {'average': 0.116, 'minimum': 0.104, 'median': 0.114, 'maximum': 0.246, 'stddev': 0.009}),
                 ('send_time', {'average': 0.015, 'minimum': 0.012, 'median': 0.015, 'maximum': 0.015, 'stddev': 0}),
                 ('latency', {'average': 5.266, 'minimum': 4.671, 'median': 5.024, 'maximum': 17.25, 'stddev': 1.186}),
                 ('receive_time',
                  {'average': 0.074, 'minimum': 0.07, 'median': 0.073, 'maximum': 0.107, 'stddev': 0.002}),
                 ('input', {'average': 88361816.237, 'minimum': 11774033, 'median': 90120003, 'maximum': 92424180,
                            'stddev': 9176013.361}),
                 ('output', {'average': 1170644.621, 'minimum': 152884, 'median': 1193734, 'maximum': 1229326,
                             'stddev': 121480.274})
                 ]
            )
        )
        for param in list(agg.params_sql_mapping.keys()):
            self.assertIsInstance(agg.get_param_data(param), list)

    # @skip('Results differ')
    @clickhouse_required
    def test_rt_histograms_aggregator(self):
        agg = RTHistogramsAggregator(self.job1)
        data = agg.aggregate()
        self.assertEqual(data[8.1], {'count': 12202, 'quantile': 84.302, 'percent': 0.391})
        raw_data = agg.get_raw_data()
        self.assertEqual(raw_data[10], [0.4, 27184])

    # @skip('No data found for job1')
    @clickhouse_required
    def test_net_codes_aggregator(self):
        agg = NetCodesAggregator(self.job1)
        self.assertEqual(agg.aggregate(), {0: {'count': 3119035, 'percent': 100.0}})
        raw_data = agg.get_raw_data()
        self.assertEqual(raw_data, [[0, 3119035]])

    # @skip('No data found for job1')
    @clickhouse_required
    def test_proto_codes_aggregator(self):
        agg = ProtoCodesAggregator(self.job1)
        self.assertEqual(agg.aggregate(), {200: {'count': 3119035, 'percent': 100.0}})
        raw_data = agg.get_raw_data()
        self.assertEqual(raw_data, [[200, 3119035]])

