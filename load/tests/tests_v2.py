import json
import os
from django.test import tag

from common.tests import CommonTestCase
from common.models import Job
from settings import BASE_DIR


class AmmoTest(CommonTestCase):
    fixtures = ['ammo.json']

    def test_get_list(self):
        resp = self.client.get('/api/v2/ammo/')
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(
            json.loads(resp.content),
            {'count': 1, 'next': None, 'previous': None, 'results':
                [{
                    'author': 'bacek',
                    'created_at': '2015-06-09T17:12:48',
                    'dsc': 'cache.ammo',
                    'flag': 0,
                    'hidden': 0,
                    'id': 23,
                    'last_used': '2015-06-09T17:12:48',
                    'mdsum': '',
                    'path': '/var/bmpt/users/bacek_sandbox/20150228/1425099462-550508_cache.ammo',
                    'private': 0,
                    'size': 0
                }]
             }
        )


class ServerTest(CommonTestCase):

    fixtures = ['server.json', ]

    def test_get_list(self):
        resp = self.client.get('/api/v2/server/')
        self.assertEqual(json.loads(resp.content)['count'], 5)

    def test_get_host_exact(self):
        resp = self.client.get('/api/v2/server/?host=t72')
        server_list = json.loads(resp.content)
        self.assertEqual(server_list['count'], 1)
        self.assertEqual(server_list['results'][0]['host'], 't72')

    def test_get_host_startswith(self):
        resp = self.client.get('/api/v2/server/?host__startswith=t72')
        server_list = json.loads(resp.content)
        self.assertEqual(server_list['count'], 3)
        self.assertTrue(all([server['host'].startswith('t72') for server in server_list['results']]))

    def test_get_host_regex(self):
        resp = self.client.get('/api/v2/server/?host__regex=tanks$')
        server_list = json.loads(resp.content)
        self.assertEqual(server_list['count'], 1)
        self.assertTrue(all([server['host'].endswith('tanks') for server in server_list['results']]))


class TaskTest(CommonTestCase):

    fixtures = ['task.json', ]

    def test_get_list(self):
        resp = self.client.get('/api/v2/task/')
        self.assertEqual(json.loads(resp.content)['count'], 3)

    def test_get_task_exact(self):
        resp = self.client.get('/api/v2/task/?key=VIDEO-15')
        task_list = json.loads(resp.content)
        self.assertEqual(task_list['count'], 1)
        self.assertEqual(task_list['results'][0]['key'], 'VIDEO-15')

    def test_get_task_startswith(self):
        resp = self.client.get('/api/v2/task/?key__startswith=VIDEO')
        task_list = json.loads(resp.content)
        self.assertEqual(task_list['count'], 2)
        self.assertTrue(all([task['key'].startswith('VIDEO') for task in task_list['results']]))


class ComponentTest(CommonTestCase):

    fixtures = ['component.json', ]

    def test_get_list(self):
        resp = self.client.get('/api/v2/components/')
        self.assertEqual(json.loads(resp.content)['count'], 4)

    def test_get_component_exact(self):
        resp = self.client.get('/api/v2/components/?name=bs80-old')
        component_list = json.loads(resp.content)
        self.assertEqual(component_list['count'], 1)
        self.assertEqual(component_list['results'][0]['name'], 'bs80-old')

    def test_get_component_startswith(self):
        resp = self.client.get('/api/v2/components/?name__startswith=bs80')
        component_list = json.loads(resp.content)
        self.assertEqual(component_list['count'], 2)
        self.assertTrue(all([component['name'].startswith('bs80') for component in component_list['results']]))


class KpiTest(CommonTestCase):

    fixtures = ['kpi.json', ]

    def test_get_list(self):
        resp = self.client.get('/api/v2/kpis/')
        self.assertEqual(json.loads(resp.content)['count'], 3)

    def test_get_component_id(self):
        resp = self.client.get('/api/v2/kpis/?component_id=6')
        kpi_list = json.loads(resp.content)
        self.assertEqual(kpi_list['count'], 1)
        self.assertEqual(kpi_list['results'][0]['component_id'], 6)

    def test_get_ktype(self):
        resp = self.client.get('/api/v2/kpis/?ktype=trail__expect')
        kpi_list = json.loads(resp.content)
        self.assertEqual(kpi_list['count'], 3)
        for result in kpi_list['results']:
            self.assertEqual(result['ktype'], 'trail__expect')

    def test_get_component_and_ktype(self):
        resp = self.client.get('/api/v2/kpis/?component_id=5&ktype=trail__expect')
        kpi_list = json.loads(resp.content)
        self.assertEqual(kpi_list['count'], 1)
        self.assertEqual(kpi_list['results'][0]['component_id'], 5)
        self.assertEqual(kpi_list['results'][0]['ktype'], 'trail__expect')


# TODO: https://st.yandex-team.ru/LUNAPARK-3522
# class RegressionCommentsTest(CommonTestCase):
#
#     fixtures = ['rcomments.json', ]
#
#     def test_get_list(self):
#         resp = self.client.get('/api/v2/regression_comments')
#         self.assertEqual(json.loads(resp.content), 6)
#
#     def test_get_author_exact(self):
#         resp = self.client.get('/api/v2/regression_comments/?author=darkk')
#         author_list = json.loads(resp.content)
#         self.assertEqual(author_list['count'], 1)
#         for result in author_list['results']:
#             self.assertEqual(result['author'], 'darkk')
#
#     def test_get_author_startswith(self):
#         resp = self.client.get('/api/v2/regression_comments/?author__startswith=no')
#         author_list = json.loads(resp.content)
#         self.assertEqual(author_list['count'], 4)
#         self.assertTrue(all([result['author'].startswith('noo') for result in author_list['results']]))
#
#     def test_post_comment(self):
#         data = {'text': 'oooops', 'author': 'apitest', 'created_at': '2015-03-27T15:23:41', 'job': 357319}
#         resp = self.client.post('/api/v2/regression_comments/', data=data, content_type='application/json')
#         self.assertEqual(resp.status_code, 201)
#         check = self.client.get('/api/v2/regression_comments/?author=apitest')
#         self.assertEqual(json.loads(check.content)['results'][0]['text'], 'oooops')
#
#     def test_patch_comment(self):
#         pass
#
#     def test_delete_comment(self):
#         pass

# TODO: https://st.yandex-team.ru/LUNAPARK-3522
class JobTest(CommonTestCase):

    def test_job_create(self):
        with open(os.path.join(BASE_DIR, 'www/api/files/config.yaml'), 'r') as input_config:
            str_config = input_config.read()
        resp = self.client.post('/api/v2/jobs/', data=str_config, content_type='text/plain')
        resp_data = json.loads(resp.content.decode('utf-8'))
        self.assertEqual(resp.status_code, 200)
        self.assertIn('n', resp_data.keys())

    def test_job_patch(self):
        job = Job()
        job.save()
        resp = self.client.patch('/api/v2/job/{}/?upload_token=token'.format(job.id), data={"status": "norm"},
                                 content_type='application/json')
        self.assertEqual(resp.status_code, 206)
        self.assertEqual(job.status, 'norm')

