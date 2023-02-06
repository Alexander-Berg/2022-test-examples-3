# -*- coding: utf-8 -*-
"""
Created on Dec 2, 2016

@author: noob
"""

# coverage run --source='.' manage.py test api --settings=settings.unittest

import json
from collections import namedtuple
import logging
import urllib.request, urllib.parse, urllib.error
from django.http import Http404, HttpRequest, QueryDict
from django.contrib.auth.models import User
from datetime import datetime, timedelta
from api.views.imbalance import Imbalance
from common.models import JobImbalance, JobTrail, Component, Job, JobEvent, UploadToken, CustomUserReport, \
    JobMonitoringConfig
from common.util.clients import StartrekClient

from common.tests import CommonTestCase, clickhouse_required, ClickhouseClient, select_patched
from api.views.jobevent import JobEventHandler

from unittest import skip
from unittest.mock import patch


def success_mock(*args, **kwargs):
    return {'success': True, 'url': 'Fakyfake!'}


class APIBaseTest(CommonTestCase):

    def test_json_render(self):
        path = '/api/job/%d/summary.json?fields=n,status,dsc' % self.job1.id
        resp = self.client.get(path)
        self.assertEqual(resp.content, b'[{"n": %d, "status": "offline", "dsc": ""}]' % self.job1.id)

    def test_xml_render(self):
        path = '/api/job/%d/summary.xml?fields=n,status,dsc' % self.job1.id
        resp = self.client.get(path)
        self.assertEqual(
            resp.content,
            b'''\
<?xml version='1.0' encoding='UTF-8'?>
<response>
    <result>
        <n>%d</n>
        <status>offline</status>
        <dsc></dsc>
    </result>
</response>''' % self.job1.id
        )

    def test_csv_render(self):
        path = '/api/job/%d/summary.csv?fields=n,status,dsc' % self.job1.id
        resp = self.client.get(path)
        self.assertEqual(resp.content, b'n;status;dsc\r\n%d;offline;' % self.job1.id)

    def test_csvs_render(self):
        path = '/api/job/%d/summary.csvs?fields=n,status,dsc' % self.job1.id
        resp = self.client.get(path)
        self.assertEqual(resp.content, b'n;status;dsc\r\n%d;offline;' % self.job1.id)


class ImbalanceTest(CommonTestCase):
    def setUp(self):
        super(ImbalanceTest, self).setUp()
        self.imb1 = JobImbalance.objects.create(up=self.job1, hum_imbalance=500, rob_imbalance=200)
        self.imb2 = JobImbalance.objects.create(up=self.job2, hum_imbalance=0, rob_imbalance=100)

        self.imbalance_processor = Imbalance()

    def test_hum_imbalance(self):
        """
        must return job2's hum_imbalance
        """
        req = HttpRequest
        response = self.imbalance_processor.get(req, self.job1.n)
        self.assertEqual(
            response,
            [{'imbalance': self.imb1.hum_imbalance}]
        )

    def test_rob_imbalance(self):
        """
        must return job2's rob_imbalance
        because there's no hum_imbalance
        """
        req = HttpRequest
        response = self.imbalance_processor.get(req, self.job2.n)
        self.assertEqual(
            response,
            [{'imbalance': self.imb2.rob_imbalance}]
        )

    def test_404(self):
        """
        Passing job number that does not exist
        """
        req = HttpRequest
        self.assertRaises(Http404, self.imbalance_processor.get, req, (self.job1.n + self.job2.n))


class JobDataTest(CommonTestCase):

    maxDiff = None

    def test_online_job(self):
        """
        """
        self.job2.td = None
        self.job2.save()
        path = '/api/job/%d/data/cases.json' % self.job2.id
        resp = self.client.get(path)
        self.assertEqual(resp.status_code, 404)

    # @skip(reason='Problems with clickhouse todate select')
    @clickhouse_required
    def test_cases_json(self):
        """
        """
        path = '/api/job/%d/data/cases.json' % self.job1.id
        resp = self.client.get(path)
        self.assertEqual(type(json.loads(resp.content.decode('utf-8'))), list)
        self.assertEqual(type(json.loads(resp.content.decode('utf-8'))[0]), dict)
        job3 = Job.objects.create(  # empty job
            task=self.task1.key,
            fd=datetime.strptime('2017-11-14 00:25:50', '%Y-%m-%d %H:%M:%S'),
            td=datetime.now()
        )
        path = '/api/job/%d/data/cases.json' % job3.id
        resp = self.client.get(path)
        self.assertEqual(resp.status_code, 404)

    @skip(reason='No data for job1')
    def test_req_cases_json(self):
        """
        """
        path = '/api/job/%d/data/req_cases.json' % self.job1.id
        resp = self.client.get(path)
        self.assertEqual(type(json.loads(resp.content)), list)
        self.assertEqual(type(json.loads(resp.content)[0]), dict)

    def test_req_cases_json_empty_job(self):
        job3 = Job.objects.create(  # empty job
            task=self.task1.key,
            fd=datetime.strptime('2017-11-14 00:25:50', '%Y-%m-%d %H:%M:%S'),
            td=datetime.now()
        )
        path = '/api/job/%d/data/req_cases.json' % job3.id
        resp = self.client.get(path)
        self.assertEqual(resp.status_code, 404)

    @skip(reason='No data for job1')
    def test_cases_csv(self):
        """
        """
        path = '/api/job/%d/data/cases.csv' % self.job1.id
        resp = self.client.get(path)
        self.assertTrue(resp.content)
        self.assertEqual(type(resp.content), bytes)

    @skip(reason='No data for job1')
    def test_req_cases_csv(self):
        """
        """
        path = '/api/job/%d/data/req_cases.csv' % self.job1.id
        resp = self.client.get(path)
        self.assertTrue(resp.content)
        self.assertEqual(type(resp.content), bytes)


class JobDistTest(CommonTestCase):

    def test_online_job(self):
        """
        """
        self.job2.td = None
        self.job2.save()
        path = '/api/job/%d/dist/cases.json' % self.job2.id
        resp = self.client.get(path)
        self.assertEqual(resp.status_code, 404)

    @clickhouse_required
    def test_empty_job(self):
        job3 = Job.objects.create(  # empty job
            task=self.task1.key,
            fd=datetime.strptime('2017-11-14 00:25:50', '%Y-%m-%d %H:%M:%S'),
            td=datetime.now()
        )
        path = '/api/job/%d/dist/cases.json' % job3.id
        resp = self.client.get(path)
        self.assertEqual(resp.content, b'[]')

    @skip('No data for job1')
    @clickhouse_required
    def test_cases_json(self):
        path = '/api/job/{job_id}/dist/cases.json'
        resp = self.client.get(path.format(job_id=self.job1.id))
        self.assertEqual(type(json.loads(resp.content.decode('utf-8'))), list)
        self.assertEqual(type(json.loads(resp.content.decode('utf-8'))[0]), dict)
        cc = ClickhouseClient()
        print((cc.select('select count() from loaddb.rt_microsecond_details_buffer')))
        print((cc.select('select count() from loaddb.rt_microsecond_details')))
        print((cc.select('select count() from loaddb.rt_quantiles')))

    def test_cases_json_empty_job(self):
        path = '/api/job/{job_id}/dist/cases.json'
        job3 = Job.objects.create(  # empty job
            task=self.task1.key,
            fd=datetime.strptime('2017-11-14 00:25:50', '%Y-%m-%d %H:%M:%S'),
            td=datetime.now()
        )
        resp = self.client.get(path.format(job_id=job3.id))
        self.assertEqual(json.loads(resp.content.decode('utf-8')), [])

    @skip('No data for job1')
    def test_cases_csv(self):
        """
        """
        path = '/api/job/%d/dist/cases.csv' % self.job1.id
        resp = self.client.get(path)
        self.assertTrue(resp.content)

    def test_cases_csv_empty_job(self):
        job3 = Job.objects.create(  # empty job
            task=self.task1.key,
            fd=datetime.strptime('2017-11-14 00:25:50', '%Y-%m-%d %H:%M:%S'),
            td=datetime.now()
        )
        path = '/api/job/%d/dist/cases.csv' % job3.id
        resp = self.client.get(path)
        self.assertEqual(resp.content, b'')

    @clickhouse_required
    def test_quantiles_json(self):
        path = '/api/job/%d/dist/percentiles.json' % self.job1.id
        resp = self.client.get(path)
        self.assertEqual(len(json.loads(resp.content)), 9)
        self.assertEqual(type(json.loads(resp.content)), list)
        self.assertEqual(type(json.loads(resp.content)[0]), dict)
        job3 = Job.objects.create(  # empty job
            task=self.task1.key,
            fd=datetime.strptime('2017-11-14 00:25:50', '%Y-%m-%d %H:%M:%S'),
            td=datetime.now()
        )
        path = '/api/job/%d/dist/percentiles.json' % job3.id
        resp = self.client.get(path)
        self.assertEqual(len(json.loads(resp.content)), 9)
        self.assertTrue(all(q['ms'] == 0.0 for q in json.loads(resp.content)))


class JobMetaInfoTest(CommonTestCase):

    def test_post(self):
        """
        horrible overall test
        """
        response = self.client.post(
            '/api/job/%d/edit.json' % self.job1.n,
            json.dumps({
                'starred': 0, 'instances': '100', 'command_line': './lunapark --script',
                'name': 'testname',
                'description': 'testdsc', 'loop': '1000.00',
                'ammo': '/home/undera/NetBeansProjects/Yandex/tank/dummy.ammo', 'version': '123',
                'tank_type': '1', 'component': 'cname', 'imbalance': 1000
            }), content_type='application/json')
        self.assertEqual(json.loads(response.content), [{'success': 1}])

    def test_post_digital_component(self):
        """
        expect error
        """
        response = self.client.post(
            '/api/job/%d/edit.json' % self.job1.n,
            json.dumps({
                'component': '0'
            }), content_type='application/json')
        self.assertEqual(json.loads(response.content), 'Digital component!')


class JobSummaryTest(CommonTestCase):

    def setUp(self):
        super(JobSummaryTest, self).setUp()
        self.job_imbalance1 = JobImbalance.objects.create(up=self.job1, hum_imbalance=111, rob_imbalance=111111)
        self.job_imbalance2 = JobImbalance.objects.create(up=self.job1, hum_imbalance=222, rob_imbalance=222222)

    def test_double_job_imbalance(self):
        """
        checks if job_imbalance_get returned job_imbalance object with higher precedence
        """
        self.job_imbalance1.hum_isimbalance = 1
        self.job_imbalance1.rob_isimbalance = 1
        self.job_imbalance1.save()
        self.job_imbalance2.hum_isimbalance = 1
        self.job_imbalance2.rob_isimbalance = 0
        self.job_imbalance2.save()
        response = self.client.get('/api/job/%d/summary.json?fields=imbalance_rps' % self.job1.n)
        updated_imbalance = JobImbalance.objects.get(n=self.job_imbalance2.n)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(json.loads(response.content)[0]['imbalance_rps'],
                         updated_imbalance.hum_imbalance)

    def test_job_imbalance_precedence(self):
        """
        checks if hum_processed in job_imbalance is always a precedence
        """
        self.job_imbalance1.hum_isimbalance = 0
        self.job_imbalance1.rob_isimbalance = 0
        self.job_imbalance2.hum_processed = 0
        self.job_imbalance1.save()
        self.job_imbalance2.hum_isimbalance = 1
        self.job_imbalance2.rob_isimbalance = 1
        self.job_imbalance2.hum_processed = 1
        self.job_imbalance2.save()
        response = self.client.post('/api/job/%d/summary.json?fields=imbalance_rps' % self.job1.n)
        updated_imbalance = JobImbalance.objects.get(n=self.job_imbalance2.n)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(json.loads(response.content)[0]['imbalance_rps'],
                         updated_imbalance.hum_imbalance)

    def test_get_status(self):
        self.job1.finalized = False
        self.job1.save()
        response = self.client.get('/api/job/%d/summary.json?fields=status' % self.job1.n)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(json.loads(response.content)[0]['status'], 'post-processing')
        JobTrail.objects.create(
            up=self.job1,
            min_rps=10,
            max_rps=200,
            http='200,201,502',
            net=0,
        )
        response = self.client.get('/api/job/%d/summary.json?fields=status' % self.job1.n)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(json.loads(response.content)[0]['status'], 'post-processing')
        self.job1.finalized = True
        self.job1.save()
        response = self.client.get('/api/job/%d/summary.json?fields=status' % self.job1.n)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(json.loads(response.content)[0]['status'], 'offline')
        self.job2.td = None
        self.job2.save()
        response = self.client.get('/api/job/%d/summary.json?fields=status' % self.job2.n)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(json.loads(response.content)[0]['status'], 'online')

    def test_get_duration(self):
        response = self.client.get('/api/job/%d/summary.json?fields=last_second_rps,duration' % self.job1.n)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(json.loads(response.content)[0]['last_second_rps'], 'last_second_rps')
        self.assertEqual(json.loads(response.content)[0]['duration'], 'last_second_rps')

    def test_get_component_name(self):
        c = Component.objects.create(tag='LOAD', name='ojbjvoiphpbo')
        self.job1.component = c.n
        self.job1.save()
        response = self.client.get('/api/job/%d/summary.json?fields=component_name' % self.job1.n)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(json.loads(response.content)[0]['component_name'], 'ojbjvoiphpbo')

    def test_fields(self):
        response = self.client.get('/api/job/%d/summary.json?fields=n,status,dsc' % self.job1.n)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(json.loads(response.content),
                         [{'n': self.job1.id, 'status': 'offline', 'dsc': ''}]
                         )


class JobCreateTest(CommonTestCase):

    def test_jobcreate(self):
        path = '/api/job/create.json'
        self.task1.td = None
        self.task1.save()
        resp = self.client.post(path, data=json.dumps(
            {'task': self.task1.key, 'person': 'lunapark', 'tank': 'kshm.t80.tanks.yandex.net',
             'host': 'skalolazka.t80.tanks.yandex.net', 'port': 80,
             'loadscheme': ['const(1,1)', 'step(1,10,2,2m)', 'line(10,20,28)'],
             'detailed_time': 'interval_real', 'notify': ['skalolazka']}), content_type='application/json')
        resp_content = json.loads(resp.content)
        print(resp_content)
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(type(resp_content), list)
        self.assertEqual(type(resp_content[0]), dict)
        self.assertEqual(set(resp_content[0].keys()), {'job', 'upload_token'})
        self.assertEqual(type(resp_content[0]['job']), int)

    def test_jobcreate_no_loadscheme(self):
        path = '/api/job/create.json'
        self.task2.td = None
        self.task2.save()
        resp = self.client.post(path, data=json.dumps(
            {
                'task': self.task2.key,
                'tank': 'localhost',
                'detailed_time': 'interval_real',
                'person': 'lunapark',
                'host': 'undera.t80.tanks',
                'loadscheme': [],
                'notify': [''], 'port': '8080'
            }), content_type='application/json')
        resp_content = json.loads(resp.content)
        self.assertEqual(type(resp_content), list)
        self.assertEqual(type(resp_content[0]), dict)
        self.assertEqual(set(resp_content[0].keys()), {'job', 'upload_token'})
        self.assertEqual(type(resp_content[0]['job']), int)


class JobsOfflineTest(CommonTestCase):
    def setUp(self):
        """
        creating 20 fake jobs
        6 jobs are online
        """
        super(JobsOfflineTest, self).setUp()

        self.job3 = Job.objects.create(td=datetime.now())
        self.job4 = Job.objects.create(td=None)  # online
        self.job5 = Job.objects.create(td=datetime.now())
        self.job6 = Job.objects.create(td=datetime.now())
        self.job7 = Job.objects.create(td=None)  # online
        self.job8 = Job.objects.create(td=datetime.now())
        self.job9 = Job.objects.create(td=datetime.now())
        self.job10 = Job.objects.create(td=datetime.now())
        self.job11 = Job.objects.create(td=None)  # online
        self.job12 = Job.objects.create(td=datetime.now())
        self.job13 = Job.objects.create(td=datetime.now())
        self.job14 = Job.objects.create(td=None)  # online
        self.job15 = Job.objects.create(td=datetime.now())
        self.job16 = Job.objects.create(td=datetime.now())
        self.job17 = Job.objects.create(td=datetime.now())
        self.job18 = Job.objects.create(td=None)  # online
        self.job19 = Job.objects.create(td=datetime.now())
        self.job20 = Job.objects.create(td=datetime.now())

    def test_all_offline_jobs(self):
        """
        checks if returned offline jobs includes all jobs with td
        """
        path = '/api/job/offline.json?limit=20'
        resp = self.client.get(path)
        resp_content = json.loads(resp.content)
        self.assertEqual(len(resp_content), len(Job.objects.exclude(td=None)))
        self.assertEqual(set(j['n'] for j in resp_content),
                         {
                             self.job1.n,
                             self.job2.n,
                             self.job3.n,
                             self.job5.n,
                             self.job6.n,
                             self.job8.n,
                             self.job9.n,
                             self.job10.n,
                             self.job12.n,
                             self.job13.n,
                             self.job15.n,
                             self.job16.n,
                             self.job17.n,
                             self.job19.n,
                             self.job20.n,
                         })

    def test_all_offline_jobs_limit(self):
        """
        checks if returned limited amount of offline jobs with td
        """
        path = '/api/job/offline.json'
        resp = self.client.get(path)
        resp_content = json.loads(resp.content)
        self.assertEqual(len(resp_content), 10)

    def test_user_offline_jobs(self):
        """
        checks if returned offline jobs with td for specific user
        """

        user = 'good_user'
        User.objects.create(username=user)
        self.job1.person = user
        self.job1.save()
        self.job2.person = user
        self.job2.save()
        self.job3.person = user
        self.job3.save()
        self.job4.person = user
        self.job4.save()  # online
        self.job5.person = user
        self.job5.save()
        self.job6.person = user
        self.job6.save()

        path = '/api/job/offline.json?limit=20&user=' + user
        resp = self.client.get(path)
        resp_content = json.loads(resp.content)

        self.assertEqual(set(j['n'] for j in resp_content), {
            self.job1.n,
            self.job2.n,
            self.job3.n,
            self.job5.n,
            self.job6.n,
        })

    def test_user_offline_jobs_invalid_user(self):
        """
        checks if returned offline jobs with td for specific user
        """

        user = "someone"
        self.job1.person = user
        self.job1.save()
        self.job2.person = user
        self.job2.save()
        self.job3.person = user
        self.job3.save()
        self.job4.person = user
        self.job4.save()  # online
        self.job5.person = user
        self.job5.save()
        self.job6.person = user
        self.job6.save()

        path = '/api/job/offline.json?limit=20&user=' + user
        resp = self.client.get(path)
        resp_content = json.loads(resp.content)

        self.assertEqual(resp_content[0]['success'], 0)

    def test_user_offline_jobs_limit(self):
        """
        checks if returned limited amount of offline jobs with td for specific user
        """

        user = 'good_user'
        User.objects.create(username=user)
        self.job1.person = user
        self.job1.save()
        self.job2.person = user
        self.job2.save()
        self.job3.person = user
        self.job3.save()
        self.job4.person = user
        self.job4.save()  # online
        self.job5.person = user
        self.job5.save()
        self.job6.person = user
        self.job6.save()

        path = '/api/job/offline.json?limit=2&user=' + user
        resp = self.client.get(path)
        print('jobs_offline_user_limit::: ' + str(resp.content) + 'kkkk')
        self.assertEqual(
            set(j['n'] for j in json.loads(resp.content)),
            {self.job5.n, self.job6.n}
        )

    def test_get_specific_offline_job(self):
        """
        checks if returned specific offline job
        """
        path = '/api/job/offline.json?job=%d' % self.job3.n
        resp = self.client.get(path)
        resp_content = json.loads(resp.content)
        self.assertEqual(resp_content[0]['n'], self.job3.n)

    def test_get_specific_offline_job_online(self):
        """

        """
        path = '/api/job/offline.json?job=%d' % self.job4.n
        resp = self.client.get(path)
        resp_content = json.loads(resp.content)
        self.assertEqual(resp_content, [])

    def test_get_specific_offline_job_invalid(self):
        """

        """
        path = '/api/job/offline.json?job=%d' % (max([j.n for j in Job.objects.all()]) + 1)
        resp = self.client.get(path)
        resp_content = json.loads(resp.content)
        self.assertEqual(resp_content[0]['success'], 0)


class JobsOnlineTest(CommonTestCase):
    def setUp(self):
        """
        creating 20 fake jobs
        5 jobs are online
        """
        super(JobsOnlineTest, self).setUp()

        self.job3 = Job.objects.create(td=datetime.now())
        self.job4 = Job.objects.create(td=None)  # online
        self.job5 = Job.objects.create(td=datetime.now())
        self.job6 = Job.objects.create(td=datetime.now())
        self.job7 = Job.objects.create(td=None)  # online
        self.job8 = Job.objects.create(td=datetime.now())
        self.job9 = Job.objects.create(td=datetime.now())
        self.job10 = Job.objects.create(td=datetime.now())
        self.job11 = Job.objects.create(td=None)  # online
        self.job12 = Job.objects.create(td=datetime.now())
        self.job13 = Job.objects.create(td=datetime.now())
        self.job14 = Job.objects.create(td=None)  # online
        self.job15 = Job.objects.create(td=datetime.now())
        self.job16 = Job.objects.create(td=datetime.now())
        self.job17 = Job.objects.create(td=datetime.now())
        self.job18 = Job.objects.create(td=None)  # online
        self.job19 = Job.objects.create(td=datetime.now())
        self.job20 = Job.objects.create(td=datetime.now())

    def test_all_online_jobs(self):
        """
        checks if returned offline jobs includes all jobs with td
        """
        path = '/api/job/online.json?later_than=1'
        resp = self.client.get(path)
        resp_content = json.loads(resp.content)
        self.assertEqual(set(j['n'] for j in resp_content),
                         {
                             self.job4.n,
                             self.job7.n,
                             self.job11.n,
                             self.job14.n,
                             self.job18.n,
                         })

    def test_all_online_jobs_limit(self):
        """
        checks if returned limited amount of offline jobs with td
        """
        path = '/api/job/online.json?later_than=1&limit=2'
        resp = self.client.get(path)
        resp_content = json.loads(resp.content)
        self.assertEqual(len(resp_content), 2)

    def test_user_online_jobs(self):
        """
        checks if returned offline jobs with td for specific user
        """

        user = 'good_user'
        User.objects.create(username=user)
        self.job1.person = user
        self.job1.save()
        self.job2.person = user
        self.job2.save()
        self.job3.person = user
        self.job3.save()
        self.job4.person = user
        self.job4.save()  # online
        self.job5.person = user
        self.job5.save()
        self.job6.person = user
        self.job6.save()
        self.job18.person = user
        self.job18.save()  # online

        path = '/api/job/online.json?later_than=1&user=' + user
        resp = self.client.get(path)
        resp_content = json.loads(resp.content)

        self.assertEqual(set(j['n'] for j in resp_content), {self.job4.n, self.job18.n})

    def test_user_online_jobs_fraud(self):
        """
        checks if returned offline jobs with td for specific user
        """

        user = "someone"
        self.job1.person = user
        self.job1.save()
        self.job2.person = user
        self.job2.save()
        self.job3.person = user
        self.job3.save()
        self.job4.person = user
        self.job4.save()  # online
        self.job5.person = user
        self.job5.save()
        self.job6.person = user
        self.job6.save()
        self.job18.person = user
        self.job18.save()  # online

        path = '/api/job/online.json?later_than=1&user=' + user
        resp = self.client.get(path)
        resp_content = json.loads(resp.content)

        self.assertEqual(resp_content[0]['success'], 0)

    # @skip('Results differ')
    def test_user_online_jobs_limit(self):
        """
        checks if returned limited amount of offline jobs with td for specific user
        """

        user = 'good_user'
        User.objects.create(username=user)
        self.job1.person = user
        self.job1.save()
        self.job2.person = user
        self.job2.save()
        self.job3.person = user
        self.job3.save()
        self.job4.person = user
        self.job4.save()  # online
        self.job5.person = user
        self.job5.save()
        self.job6.person = user
        self.job6.save()
        self.job18.person = user
        self.job18.save()  # online

        path = '/api/job/online.json?later_than=1&limit=1&user=' + user
        resp = self.client.get(path)
        resp_content = json.loads(resp.content)

        self.assertEqual(set(j['n'] for j in resp_content), {
            self.job4.n,
        })

    def test_later_than(self):
        path = '/api/job/online.json?later_than=%d' % self.job9.n
        resp = self.client.get(path)
        resp_content = json.loads(resp.content)
        self.assertEqual(set(j['n'] for j in resp_content),
                         {
                             self.job11.n,
                             self.job14.n,
                             self.job18.n,
                         })


class JobsQueuedTest(CommonTestCase):
    def setUp(self):
        """
        creating 20 fake jobs
        5 jobs are online
        5 have queue-ish status
        """
        super(JobsQueuedTest, self).setUp()

        self.job3 = Job.objects.create(td=None, status='queued')
        self.job4 = Job.objects.create(td=None)  # online
        self.job5 = Job.objects.create(td=datetime.now(), status='queued')  # offline!
        self.job6 = Job.objects.create(td=datetime.now())
        self.job7 = Job.objects.create(td=None)  # online
        self.job8 = Job.objects.create(td=datetime.now())
        self.job9 = Job.objects.create(td=datetime.now())
        self.job10 = Job.objects.create(td=datetime.now())
        self.job11 = Job.objects.create(td=None, status='configure_7f8v')
        self.job12 = Job.objects.create(td=datetime.now())
        self.job13 = Job.objects.create(td=datetime.now())
        self.job14 = Job.objects.create(td=None)  # online
        self.job15 = Job.objects.create(td=datetime.now())
        self.job16 = Job.objects.create(td=datetime.now())
        self.job17 = Job.objects.create(td=datetime.now())
        self.job18 = Job.objects.create(td=None)  # online
        self.job19 = Job.objects.create(td=None, status='prepare_87bi')
        self.job20 = Job.objects.create(td=None, status='start')

    def test_all_queued_jobs(self):
        """
        checks if returned offline jobs includes all jobs with td
        """
        path = '/api/job/queued.json'
        resp = self.client.get(path)
        resp_content = json.loads(resp.content)
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(set(j['n'] for j in resp_content),
                         {
                             self.job3.n,
                             self.job11.n,
                             self.job20.n,
                             self.job19.n,
                         })

    def test_all_queued_jobs_limit(self):
        """
        checks if returned limited amount of offline jobs with td
        """
        path = '/api/job/queued.json?limit=2'
        resp = self.client.get(path)
        resp_content = json.loads(resp.content)
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(len(resp_content), 2)

    def test_user_queued_jobs(self):
        """
        checks if returned offline jobs with td for specific user
        """

        user = 'good_user'
        User.objects.create(username=user)
        self.job1.person = user
        self.job1.save()
        self.job2.person = user
        self.job2.save()
        self.job3.person = user
        self.job3.save()  # queued
        self.job4.person = user
        self.job4.save()  # online
        self.job5.person = user
        self.job5.save()
        self.job6.person = user
        self.job6.save()
        self.job11.person = user
        self.job11.save()  # online, queued

        path = '/api/job/queued.json?user=' + user
        resp = self.client.get(path)
        self.assertEqual(resp.status_code, 200)
        resp_content = json.loads(resp.content)

        self.assertEqual(set(j['n'] for j in resp_content), {self.job3.n, self.job11.n})

    def test_user_queued_jobs_fraud(self):
        """
        checks if returned offline jobs with td for specific user
        """

        user = "someone"
        self.job1.person = user
        self.job1.save()
        self.job2.person = user
        self.job2.save()
        self.job3.person = user
        self.job3.save()  # queued
        self.job4.person = user
        self.job4.save()  # online
        self.job5.person = user
        self.job5.save()
        self.job6.person = user
        self.job6.save()
        self.job11.person = user
        self.job11.save()  # online, queued

        path = '/api/job/queued.json?user=' + user
        resp = self.client.get(path)
        self.assertEqual(resp.status_code, 200)
        resp_content = json.loads(resp.content)
        self.assertEqual(resp_content[0]['success'], 0)

    def test_user_queued_jobs_limit(self):
        """
        checks if returned limited amount of offline jobs with td for specific user
        """

        user = 'good_user'
        User.objects.create(username=user)
        self.job1.person = user
        self.job1.save()
        self.job2.person = user
        self.job2.save()
        self.job3.person = user
        self.job3.save()  # online, queued
        self.job4.person = user
        self.job4.save()  # online
        self.job5.person = user
        self.job5.save()
        self.job6.person = user
        self.job6.save()
        self.job11.person = user
        self.job11.save()  # online, queued

        path = '/api/job/queued.json?limit=1&user=' + user
        resp = self.client.get(path)
        self.assertEqual(resp.status_code, 200)
        resp_content = json.loads(resp.content)
        self.assertEqual(set(j['n'] for j in resp_content), {
            self.job3.n,
        })


class JobEventsTest(CommonTestCase):
    def setUp(self):
        super(JobEventsTest, self).setUp()
        JobEvent.objects.create(
            job=self.job1,
            text='foo',
            tag='1',
            author='fighter',
            timestamp=datetime.fromtimestamp(1480682211)
        )
        JobEvent.objects.create(
            job=self.job1,
            text='bar',
            tag='1|2',
            author='fighter',
            timestamp=datetime.fromtimestamp(1480682221)
        )

        self.handler = JobEventHandler()

    def tearDown(self):
        JobEvent.objects.all().delete()

    def test_get_by_timestamp(self):
        """
        taking events for a timestamp
        """
        request_get = HttpRequest()
        request_get.GET = QueryDict('timestamp=1480682211')
        self.assertEqual(
            json.loads(self.handler.get(request_get, self.job1).content),
            [{'author': 'fighter', 'tag': '1', 'text': 'foo', 'timestamp': 1480682211}]
        )

    def test_get_by_tag(self):
        """
        taking events for tags
        """
        request_get = HttpRequest()
        request_get.GET = QueryDict('tag=1|2')
        self.assertEqual(
            json.loads(self.handler.get(request_get, self.job1).content),
            [{'author': 'fighter', 'tag': '1|2', 'text': 'bar', 'timestamp': 1480682221},
             {'author': 'fighter', 'tag': '1', 'text': 'foo', 'timestamp': 1480682211}]

        )
        request_get.GET = QueryDict('tag=1')
        self.assertEqual(
            json.loads(self.handler.get(request_get, self.job1).content),
            [{'author': 'fighter', 'tag': '1', 'text': 'foo', 'timestamp': 1480682211}, ]
        )
        request_get.GET = QueryDict('tag=2')
        self.assertEqual(
            json.loads(self.handler.get(request_get, self.job1).content),
            []
        )

    def test_get_by_interval(self):
        """
        taking events for intervals
        """
        request_get = HttpRequest()
        request_get.GET = QueryDict('from=1480682211')
        self.assertEqual(
            json.loads(self.handler.get(request_get, self.job1).content),
            [{'author': 'fighter', 'tag': '1|2', 'text': 'bar', 'timestamp': 1480682221},
             {'author': 'fighter', 'tag': '1', 'text': 'foo', 'timestamp': 1480682211}]
        )

        request_get.GET = QueryDict('to=1480682211')
        self.assertEqual(
            json.loads(self.handler.get(request_get, self.job1).content),
            [{'author': 'fighter', 'tag': '1', 'text': 'foo', 'timestamp': 1480682211}]
        )

        request_get.GET = QueryDict('from=1480682219&to=1480682221')
        self.assertEqual(
            json.loads(self.handler.get(request_get, self.job1).content),
            [{'author': 'fighter', 'tag': '1|2', 'text': 'bar', 'timestamp': 1480682221}]
        )

    def test_get_by_author(self):
        """
        taking events for an author
        """
        request_get = HttpRequest()
        request_get.GET = QueryDict('author=fighter')
        self.assertEqual(
            json.loads(self.handler.get(request_get, self.job1).content),
            [{'author': 'fighter', 'tag': '1|2', 'text': 'bar', 'timestamp': 1480682221},
             {'author': 'fighter', 'tag': '1', 'text': 'foo', 'timestamp': 1480682211}]
        )

        request_get.GET = QueryDict('author=shmighter')
        self.assertEqual(
            json.loads(self.handler.get(request_get, self.job1).content),
            []
        )

    def test_post(self):
        """

        """
        request_post = HttpRequest()
        request_post.POST = QueryDict('author=shmighter&tag=2&timestamp=1480682231&text=lol')
        self.handler.post(request_post, self.job1)

        request_get = HttpRequest()

        self.assertEqual(
            json.loads(self.handler.get(request_get, self.job1).content),
            [{'author': 'shmighter', 'tag': '2', 'text': 'lol', 'timestamp': 1480682231},
             {'author': 'fighter', 'tag': '1|2', 'text': 'bar', 'timestamp': 1480682221},
             {'author': 'fighter', 'tag': '1', 'text': 'foo', 'timestamp': 1480682211}]
        )


class JobConfigInfoTest(CommonTestCase):
    """
    testing configinfo handle
    """

    def setUp(self):
        """
        setting up fake memcached data
        """
        super(JobConfigInfoTest, self).setUp()
        self.fake_configinfo = '''dsfvgdf
        sdfvswdfvsdfv
        [23e]234[{]]<[][]347823472394078r{}
        osdfv0234r09jdewf[o1idewf1
        wefkvgpwoefivj-2i034=oenrdfv=o2enfvkef;vk2n;fv2d;q,ds;vclkwndvf/n\nokdjfpoqewdn'''

    def test_get(self):
        """testing GET request. checks if fake configinfo is returned properly within response on GET request"""
        self.job1.configinfo = self.fake_configinfo
        self.job1.save()
        response = self.client.get('/api/job/%d/configinfo.txt' % self.job1.n)
        self.assertEqual(response.content.decode('utf-8'), self.fake_configinfo)

    def test_post(self):
        """testing POST request. checks if fake configinfo is properly set within POST request"""
        response = self.client.post('/api/job/%d/configinfo.txt' % self.job1.n,
                                    {'configinfo': self.fake_configinfo}
                                    )
        updated_job = Job.objects.get(n=self.job1.n)
        self.assertTrue(json.loads(response.content)[0]['success'])
        self.assertEqual(updated_job.configinfo, self.fake_configinfo)


class JobConfigInitialTest(CommonTestCase):
    """
    testing configinitial handle
    """

    def setUp(self):
        """
        setting up fake memcached data
        """
        super(JobConfigInitialTest, self).setUp()
        self.fake_configinitial = b'''dsfvgdf
        sdfvswdfvsdfv
        [23e]234[{]]<[][]347823472394078r{}
        osdfv0234r09jdewf[o1idewf1
        wefkvgpwoefivj-2i034=oenrdfv=o2enfvkef;vk2n;fv2d;q,ds;vclkwndvf/n\nokdjfpoqewdn'''

    def test_get(self):
        """testing GET request. checks if fake configinitial is returned properly within response on GET request"""
        self.job1.configinitial = self.fake_configinitial
        self.job1.save()
        response = self.client.get('/api/job/%d/configinitial.txt' % self.job1.n)
        self.assertEqual(response.content, self.fake_configinitial)


class JobMonitoringConfigTests(CommonTestCase):
    def setUp(self):
        """
        setting up fake memcached data
        """
        super(JobMonitoringConfigTests, self).setUp()
        self.fake_configinfo = b'''dsfvgdf
        sdfvswdfvsdfv
        [23e]234[{]]<tag></tag>
        osdfv0234r09jdewf[o1idewf1
        wefkvgpwoefivj-2i034=oenrdfv=o2enfvkef;vk2n;fv2d;q,ds;vclkwndvf/n\nokdjfpoqewdn'''

    def test_get(self):
        """testing GET request. checks if fake monitoringconfig is returned properly within response on GET request"""
        JobMonitoringConfig.objects.create(job=self.job1, contents=self.fake_configinfo)
        response = self.client.get('/api/job/%d/jobmonitoringconfig.txt' % self.job1.n)
        self.assertEqual(response.content, self.fake_configinfo)

    def test_post(self):
        """testing POST request. checks if fake configinfo is properly set within POST request"""
        resp = self.client.post('/api/job/%d/jobmonitoringconfig.txt' % self.job1.n,
                                {'monitoringconfig': self.fake_configinfo}
                                )
        print(resp.content)
        self.assertEqual(resp.status_code, 200)
        self.assertTrue(json.loads(resp.content)[0]['success'])
        self.assertEqual(JobMonitoringConfig.objects.filter(job=self.job1).count(), 1)
        self.assertEqual(JobMonitoringConfig.objects.get(job=self.job1).contents, self.fake_configinfo)


class AddAmmoTest(CommonTestCase):

    @patch('common.util.clients.MDSClient.post', success_mock)
    def test_add(self):

        user = 'good_user'
        User.objects.create(username=user)
        with open('/tmp/att_file', 'w') as attachment:
            attachment.write('lskdnf;ksjndflskjdnfvw')
        with open('/tmp/att_file') as attachment:
            resp = self.client.post('/api/addammo.json', {
                'login': user,
                'dsc': 'valid_dsc',
                'file': attachment,
            })
        self.assertTrue(json.loads(resp.content)[0]['success'])
        self.assertEqual(json.loads(resp.content)[0]['url'], 'Fakyfake!')


class CheckUploadTokenTests(CommonTestCase):

    def test_get_token_found(self):
        token = 'superdupertokenshmoken'
        UploadToken.objects.create(job=self.job1.n, token=token)
        resp = self.client.get('/api/job/%d/check_upload_token.json?upload_token=%s' % (self.job1.n, token))
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(resp.content, b'true')

    def test_get_token_invalid(self):
        resp = self.client.get('/api/job/%d/check_upload_token.json?upload_token=invalid_token' % self.job1.n)
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(resp.content, b'false')

    def test_get_no_token(self):
        resp = self.client.get('/api/job/%d/check_upload_token.json' % self.job1.n)
        self.assertEqual(resp.status_code, 400)


class AddCustomReportTests(CommonTestCase):

    def test_post(self):
        data = {'cur_name': 'mega_cur',
                'someplot': 1,
                'someplit': 1,
                'someplut': 1,
                'someplyt': 1,
                'csrfmiddlewaretoken': 'wekjfnlweijfnie'
                }
        self.client.post('/api/add_custom_report?job=%d' % self.job1.n, data=data)
        cur = CustomUserReport.objects.get(name='custom:mega_cur')
        self.assertEqual(set(cur.plots), set(['someplot', 'someplit', 'someplut', 'someplyt']))

    def test_get(self):
        resp = self.client.get('/api/add_custom_report?job=%d' % self.job1.n)
        self.assertEqual(resp.status_code, 405)


class RegressComponentListTests(CommonTestCase):

    def setUp(self):
        super(RegressComponentListTests, self).setUp()
        self.c1 = Component.objects.create(tag='LOAD', name='ojbjvoiphpbo', services=[])
        self.c2 = Component.objects.create(tag='LOAD', name='ojbjvo', services=[])

    def test_get(self):
        self.c1.services_json = json.dumps(['s1', 's2'])
        self.c1.save()
        resp = self.client.get('/api/regress/LOAD/componentlist.json')
        self.assertEqual(set(c['n'] for c in json.loads(resp.content)), {self.c1.n, self.c2.n})
        resp = self.client.get('/api/regress/LOAD/componentlist.json?service=')
        self.assertEqual(set(c['n'] for c in json.loads(resp.content)), {self.c2.n})
        resp = self.client.get('/api/regress/LOAD/componentlist.json?service=s1')
        self.assertEqual(set(c['n'] for c in json.loads(resp.content)), {self.c1.n})
        resp = self.client.get('/api/regress/LOAD/componentlist.json?order=-n')
        self.assertEqual([c['n'] for c in json.loads(resp.content)], [self.c2.n, self.c1.n])


class JobMonitoringTests(CommonTestCase):

    @clickhouse_required
    def test_from_to_params_formats(self):
        ch_client = ClickhouseClient()
        extremes = ch_client.select('''
            select min(time), max(time)
            from loaddb.monitoring_verbose_data
            where job_id=%d
        ''' % self.job1.n)
        from_date, to_date = extremes[0]
        # "%Y-%m-%d %H:%M:%S", "%Y-%m-%dT%H:%M:%S"
        resp = self.client.get(
            '/api/job/%d/monitoring.json#?start=%s&end=%s' %
            (
                self.job1.n,
                from_date,
                to_date.replace(' ', 'T')
            )
        )
        self.assertEqual(len(json.loads(resp.content)), 191)
        self.assertEqual(json.loads(resp.content)[1],
                         {'min': 5.0,
                          'max': 23.0,
                          'metric': 'custom:pcpusys_proc1',
                          'median': 15.0,
                          'host': 'cpumem',
                          'stddev': 5.57,
                          'avg': 14.317}
                         )
        #  "%Y-%m-%d %H:%M:%S", "%Y%m%d%H%M%S"
        # делаем from_date немного позже начала, чтобы убедиться, что наличие параметра влияет на результат
        from_date = datetime.strptime(from_date, "%Y-%m-%d %H:%M:%S") + timedelta(0, 30)
        resp = self.client.get(
            '/api/job/%d/monitoring.json?from=%s&to=%s' %
            (
                self.job1.n,
                from_date,
                to_date.replace(':', '').replace(' ', '').replace('-', '')
            )
        )
        metrics = json.loads(resp.content)
        for metric in metrics:
            if metric['metric'] == 'custom:pcpusys_proc1':
                proc_metric = metric
        self.assertEqual(len(metrics), 191)
        self.assertEqual(proc_metric,
                         {'min': 5,
                          'max': 23,
                          'metric': 'custom:pcpusys_proc1',
                          'median': 16,
                          'host': 'cpumem',
                          'stddev': 5.57,
                          'avg': 14.317}
                         )


class ServerLockTests(CommonTestCase):

    def test_lock_no_action(self):
        params = {
            'address': 'someaddress',
            'duration': '100',
            'jobno': self.job1.n,
        }
        url_params = urllib.parse.urlencode(params)
        resp = self.client.get('/api/server/lock.json?' + url_params)
        self.assertEqual(resp.status_code, 400)

    def test_lock_invalid_action(self):
        params = {
            'address': 'someaddress',
            'duration': '100',
            'jobno': self.job1.n,
            'action': 'shmock'
        }
        url_params = urllib.parse.urlencode(params)
        resp = self.client.get('/api/server/lock.json?' + url_params)
        self.assertEqual(resp.status_code, 400)

    def test_lock_empty_address(self):
        # empty address
        params = {
            'address': 'someaddress',
            'duration': '100',
            'jobno': self.job1.n,
            'action': 'lock',
            'address': ''
        }
        url_params = urllib.parse.urlencode(params)
        resp = self.client.get('/api/server/lock.json?' + url_params)
        self.assertEqual(resp.status_code, 400)

    def test_lock_success(self):
        params = {
            'address': 'someaddress',
            'duration': '100',
            'jobno': self.job1.n,
            'action': 'lock',
            'address': 'someaddress'
        }
        url_params = urllib.parse.urlencode(params)
        resp = self.client.get('/api/server/lock.json?' + url_params)
        self.assertEqual(resp.status_code, 200)
        self.assertTrue(json.loads(resp.content)[0]['success'])


# class TaskSummaryTests(CommonTestCase):
#     maxDiff = None
#
#     class PseudoTask:
#         class PseudoStatus1:
#             key = 'opened'
#
#         class PseudoStatus2:
#             key = 'closed'
#
#         class PseudoUser1:
#             login = 'user1'
#
#         class PseudoUser2:
#             login = 'user2'
#
#         key = 'VOIC-437'
#         createdAt = '2015-08-21T10:57:51.447+0000'
#         updatedAt = '2016-08-21T10:57:51.447+0000'
#         status = PseudoStatus1()
#         summary = 'Some summary'
#         description = 'Some description'
#         createdBy = PseudoUser1()
#         assignee = PseudoUser2
#         version = 42
#
#     # def return_pseudo_task():
#     #
#     #     return PseudoTask()
#
#     # @patch('common.util.clients.StartrekClient.get_task', new_callable=return_pseudo_task)
#     def test_get_valid_opened_task(self):
#         with patch('common.util.clients.StartrekClient') as MockClass:
#             instance = MockClass.return_value
#             instance.get_task.return_value = self.PseudoTask
#             self.a
#             resp = self.client.get('/api/task/voice-437/summary.json')
#         result = [{
#             'task': 'VOICE-437',
#             'status': 'Open',
#             'opened': '2015-08-21T10:57:51.447+0000',
#             'closed': None,
#             'name':  'Some summary',
#             'dsc': 'Some description',
#             'reporter': 'user1',
#             'assignee': 'user2',
#             'version': 42}]
#         self.assertEqual(result, json.loads(resp.content.decode('utf-8')))
