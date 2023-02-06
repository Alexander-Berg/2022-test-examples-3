from common.models import JobMeta, Job, Data, DataMeta, Case, Regression
from common.tests import CommonTestCase
from api.views import _add_job_to_regression, _get_job_object, _get_key_diff, \
    _update_job_meta, _update_job_object, _update_job_info
import json
import os
import pytest
import string
from django.db.models import ObjectDoesNotExist
import random
import datetime
from uuid import uuid4


def _generate_random_string(size=5, chars=string.ascii_lowercase + string.digits):
    return ''.join(random.choice(chars) for _ in range(size))


cur_time = int(datetime.datetime.now().timestamp() * 1000000)


class TestCreateJob(CommonTestCase):
    """
    Тесты ручки /create_job/, которая:
    Создает джобу
    Принимает параметры джобы в виде простого тела POST запроса.
    test_start обязателен и попадает в свойства самой джобы.
    На все остальные параметры, переданные в теле создаются записи в таблице job_meta.
    :param request: django HTTP request
    :return: django HTTP response 200 (id джобы плэйн текстом) | 400
    """

    def test_return_code_200(self):
        """
        Передаем корректный запрос.
        Ожидаем 200
        """
        data = {'test_start': 1234567890}
        resp = self.client.post('/create_job/', data=data)
        self.assertEqual(resp.status_code, 200)

    def test_correct_job_meta(self):
        data = {'test_start': 1234567890, 'status': 'integrated'}
        resp = self.client.post('/create_job/', data=data)
        self.assertEqual(resp.status_code, 200)
        job_obj = Job.objects.get(pk=resp.content)
        self.assertEqual(job_obj.status, data['status'])
        self.assertEqual(job_obj.test_start, data['test_start'])
        self.assertDictEqual(job_obj.meta, dict(zip(data.keys(), map(str, data.values()))))

    def test_ignore_job_parameter(self):
        """
        Передаем корректный запрос с указанием параметра job.
        Ожидаем 200 и игнорирование параметра job
        """
        data = {'test_start': 1234567890, 'somemetainfo': 'this is a new job', 'job': 5555, 'status': 'new'}
        resp = self.client.post('/create_job/', data=data)
        self.assertEqual(resp.status_code, 200)
        job_obj = Job.objects.get(pk=resp.content)
        self.assertDictEqual(job_obj.meta, dict(zip(data.keys(), map(str, data.values()))))

    def test_400_without_test_start(self):
        """
        Запрос без test_start
        Ожидаем 400
        """
        data = {'somemetainfo': 'this is a new job', 'job': 5555, 'status': 'new'}
        resp = self.client.post('/create_job/', data=data)
        self.assertEqual(resp.status_code, 400)


class TestCreateCase(CommonTestCase):
    @pytest.mark.skip
    def test_return_code_200(self):
        """
        Send correct create_case request.
        :return: HTTP 200
        """

        data_obj = Data.objects.get(pk=9)
        data = {'parent': data_obj.tag(), 'name': 'test_return_code_200', 'tag': str(uuid4().hex)}
        resp = self.client.post('/create_case/', json.dumps(data), content_type="application/json")
        self.assertEqual(resp.status_code, 200)

    @pytest.mark.skip
    def test_create_valid_case(self):
        """
        Send correct create_case request.
        :return: Valid case id
        """
        data_obj = Data.objects.get(pk=1)
        data = {'parent': data_obj.tag(), 'name': 'test_create_valid_case', 'tag': str(uuid4().hex)}
        resp = self.client.post('/create_case/', json.dumps(data), content_type="application/json")
        case_obj = Case.objects.get(pk=resp.content)
        self.assertEqual(int(resp.content), case_obj.id)

    @pytest.mark.skip
    def test_empty_parent_tag(self):
        """
        Send create_case request with empty parent.
        :return: HTTP 400
        """
        data = {'parent': '', 'name': 'test_empty_parent_tag', 'tag': str(uuid4().hex)}
        resp = self.client.post('/create_case/', json.dumps(data), content_type="application/json")
        self.assertEqual(resp.status_code, 400)

    @pytest.mark.skip
    def test_parent_tag_not_valid(self):
        """
        Send create_case request with empty parent.
        :return: HTTP 400
        """
        data = {'parent': str(uuid4().hex), 'name': '', 'tag': str(uuid4().hex)}
        resp = self.client.post('/create_case/', json.dumps(data), content_type="application/json")
        self.assertEqual(resp.status_code, 400)

    @pytest.mark.skip
    def test_empty_case_name(self):
        """
        Send create_case request with empty name.
        :return: HTTP 400
        """
        data_obj = Data.objects.get(pk=9)
        data = {'parent': data_obj.tag(), 'name': '', 'tag': str(uuid4().hex)}
        resp = self.client.post('/create_case/', json.dumps(data), content_type='application/json')
        self.assertEqual(resp.status_code, 400)

    @pytest.mark.skip
    def test_case_name_overall(self):
        """
        Send create_case request with name 'overall'.
        :return: HTTP 400
        """
        data_obj = Data.objects.get(pk=9)
        data = {'parent': data_obj.tag(), 'name': 'overall', 'tag': str(uuid4().hex)}
        resp = self.client.post('/create_case/', json.dumps(data), content_type='application/json')
        self.assertEqual(resp.status_code, 400)

    @pytest.mark.skip
    def test_empty_tag(self):
        """
        Send create_case request with empty tag.
        :return: HTTP 400
        """
        data_obj = Data.objects.get(pk=9)
        data = {'parent': data_obj.tag(), 'name': 'test_empty_tag', 'tag': ''}
        resp = self.client.post('/create_case/', json.dumps(data), content_type='application/json')
        self.assertEqual(resp.status_code, 400)

    @pytest.mark.skip
    def test_two_names_for_same_tag(self):
        """
        Send two create_case requests with same tag but different names.
        :return: HTTP 400
        """
        data_obj = Data.objects.get(pk=9)
        tag = str(uuid4().hex)
        data_one = {'parent': data_obj.tag(), 'name': 'data_one', 'tag': tag}
        resp_one = self.client.post('/create_case/', json.dumps(data_one), content_type='application/json')
        self.assertEqual(resp_one.status_code, 200)
        data_two = {'parent': data_obj.tag(), 'name': 'data_two', 'tag': tag}
        resp_two = self.client.post('/create_case/', json.dumps(data_two), content_type='application/json')
        self.assertEqual(resp_two.status_code, 200)


class TestUpdateJob(CommonTestCase):
    """
    Тесты ручки /update_job/, которая:
    Обновляет свойства стрельбы и мета информацию стрельбы
    Принимает параметры в виде простого тела POST запроса или json.
    status и test_start обновляются прямо в записи самой джобы в таблице job
    для остальных параметров обновляется или создается запись в таблице job_meta
    :param request: django HTTP request
    - job - номер изменяемой джобы
    :return: django HTTP response 200 | 400
    """

    def test_return_code_200(self):
        resp = self.client.post('/update_job/?job=2290&job=2291', data={'1': '2'})
        self.assertEqual(resp.status_code, 200)

    def test_invalid_job_id(self):
        resp = self.client.post('/update_job/?job=2290&job=aaaa', data={'1': '2'})
        self.assertEqual(resp.status_code, 400)

    def test_update_job_attribute(self):
        self.client.post('/update_job/?job=2290',
                         data=json.dumps({'status': 'changed'}), content_type='application/json')
        job_object = Job.objects.get(id=2290)
        self.assertEqual(job_object.status, 'changed')

        self.client.post('/update_job/?job=2290', data=json.dumps({'status': 'new'}), content_type='application/json')
        job_object = Job.objects.get(id=2290)
        self.assertEqual(job_object.status, 'new')

    def test_update_job_meta(self):
        job = Job.objects.get(id=2291)
        job_name = job.meta['name']
        metric_value = _generate_random_string(size=10)

        result = self.client.post('/update_job/?job=2291',
                                  data=json.dumps({'name': metric_value}),
                                  content_type='application/json')
        job_meta_obj = JobMeta.objects.get(job_id=2291, key='name')
        self.assertEqual(result.status_code, 200)
        self.assertEqual(job_meta_obj.value, metric_value)

        self.client.post('/update_job/?job=2291', data=json.dumps({'name': job_name}),  content_type='application/json')
        job_meta_obj = JobMeta.objects.get(job_id=2291, key='name')
        self.assertEqual(job_meta_obj.value, job_name)

    def test_update_immutable_meta(self):
        job = Job.objects.get(id=3)
        job_meta_obj = JobMeta.objects.get(job_id=3, key='job')
        job_value = job_meta_obj.value
        self.client.post('/update_job/?job=3', data={'job': '1111'})
        self.assertEqual(job_meta_obj.value, '1789389')


class TestAddJobToRegression(CommonTestCase):

    def setUp(self):
        super(TestAddJobToRegression, self).setUp()
        self.job_1 = Job.objects.get(id=5)
        self.job_2 = Job.objects.get(id=6)
        self.job_3 = Job.objects.get(id=7)
        self.regression_1 = Regression.objects.create(name='temp_1', creation=cur_time)
        self.regression_2 = Regression.objects.create(name='temp_2', creation=cur_time)

    def test_object_does_not_exist(self):
        result = _add_job_to_regression(self.job_1, ['not_exist'])
        self.assertEqual(result, ['Regression with name not_exist not found'])

    def test_add_several_not_existent(self):
        result = _add_job_to_regression(self.job_1, ['not_exist', 'not_exist_again'])
        self.assertEqual(
            result, ['Regression with name not_exist not found', 'Regression with name not_exist_again not found']
        )

    def test_add_all_existent(self):
        result = _add_job_to_regression(self.job_1, ['temp_1', 'temp_2'])
        relation = Job.objects.filter(regression__jobs__exact=self.job_1)
        self.assertEqual(result, [])
        self.assertEqual(relation.count(), 2)

    def test_add_some_existent(self):
        result = _add_job_to_regression(self.job_2, ['temp_1', 'not_exist'])
        relation = Job.objects.filter(regression__jobs__exact=self.job_2)
        self.assertEqual(result, ['Regression with name not_exist not found'])
        self.assertEqual(relation.count(), 1)

    def test_remove_not_mentioned(self):
        result = _add_job_to_regression(self.job_3, ['temp_1', 'temp_2'])
        relation = Job.objects.filter(regression__jobs__exact=self.job_3)
        self.assertEqual(relation.count(), 2)
        _add_job_to_regression(self.job_3, ['not_exist'])
        relation = Job.objects.filter(regression__jobs__exact=self.job_3)
        self.assertEqual(relation.count(), 0)

    def tearDown(self):
        super(TestAddJobToRegression, self).tearDown()
        self.regression_1.delete()
        self.regression_2.delete()


class TestUpdateJobInfo(CommonTestCase):

    def test_update_job_meta_delete_immutable(self):
        job = Job.objects.get(id=525)
        old_meta = job.meta
        result = _update_job_meta(job=job, data=job.meta, add=set(), update=set(), delete={'test_start'})
        self.assertEqual(result, ['Impossible to delete immutable field test_start'])
        self.assertEqual(set(job.meta.keys()), set(old_meta.keys()))

    def test_update_job_meta_delete_mutable(self):
        job = Job.objects.get(id=526)
        old_meta_keys = set(job.meta.keys())
        self.assertIn('device_os', old_meta_keys)
        new_meta = job.meta
        del new_meta['device_os']
        new_meta_keys = set(job.meta.keys())
        result = _update_job_meta(job=job, data=new_meta, add=set(), update=set(), delete={'device_os', })
        diff_keys = set(old_meta_keys - {'device_os'})
        self.assertEqual(result, [])
        self.assertEqual(new_meta_keys, diff_keys)
        _update_job_meta(job=job, data=dict(new_meta, **{'device_os': 'android 404'}), add={'device_os'}, update=set(), delete=set())

    def test_update_job_meta_update_immutable(self):
        job = Job.objects.get(id=525)
        old_meta_keys = job.meta.keys()
        result = _update_job_meta(job=job, data=job.meta, add=set(), update=set(), delete={'test_start'})
        self.assertEqual(result, ['Impossible to delete immutable field test_start'])
        self.assertEqual(set(job.meta.keys()), set(old_meta_keys))

    def test_update_job_meta_update_mutable(self):
        job = Job.objects.get(id=525)
        old_meta = job.full_meta
        new_meta = job.full_meta
        new_meta['task'] = 'some task'
        result = _update_job_meta(job=job, data=new_meta, add=set(), update={'task', }, delete=set())
        self.assertEqual(result, [])
        self.assertEqual(job.full_meta['task'], 'some task')
        result_2 = _update_job_meta(job=job, data=old_meta, add=set(), update={'task', },  delete=set())
        self.assertEqual(result_2, [])

    def test_update_job_meta_update_regression(self):
        self.maxDiff = None
        job = Job.objects.get(id=527)
        old_meta = job.meta

        r1 = Regression.objects.create(name='temp_527', creation=cur_time)
        r2 = Regression.objects.create(name='temp_527_2', creation=cur_time)

        old_meta['regression'] = 'temp_527'
        result_create_link = _update_job_meta(job=job, data=old_meta, add={'regression', }, update=set(), delete=set())
        self.assertEqual(result_create_link, [])
        self.assertEqual(Job.objects.filter(regression__jobs__exact=job).count(), 1)
        self.assertIn('regression', job.meta.keys())

        new_meta = job.meta
        new_meta['regression'] = 'temp_527, temp_527_2'

        result_update_regr = _update_job_meta(job=job, data=new_meta, add=set(), update={'regression', }, delete=set())
        self.assertEqual(result_update_regr, [])
        self.assertEqual(Job.objects.filter(regression__jobs__exact=job).count(), 2)

        r1.delete()
        r2.delete()
        self.assertEqual(Job.objects.filter(regression__jobs__exact=job).count(), 0)

    def test_update_job_meta_add_regression(self):
        job = Job.objects.get(id=551)
        old_full_meta = job.full_meta
        new_full_meta = dict(old_full_meta, **{'regression': 'test_1'})
        result = _update_job_meta(job=job, data=new_full_meta, add=set(['regression'],), update=set(), delete=set())
        self.assertEqual(result, [])
        relation = Job.objects.filter(regression__jobs__exact=job)
        self.assertEqual(relation.count(), 1)

    def test_update_job_meta_add_key(self):
        job = Job.objects.get(id=552)
        old_full_meta = job.full_meta
        new_full_meta = dict(old_full_meta, **{'new_meta_key': 'new_meta_value'})
        result_add = _update_job_meta(job=job, data=new_full_meta, add=set(['new_meta_key'],), update=set(), delete=set())
        self.assertEqual(result_add, [])
        self.assertEqual(job.full_meta, new_full_meta)
        result_del = _update_job_meta(job=job, data=new_full_meta, add=set(), update=set(), delete=set(['new_meta_key'],))
        self.assertEqual(result_del, [])

    def test_update_job_info_create_valid(self):
        job_no, errors = _update_job_info(job_id=None, test_start=167535678,
                                          meta={'person': 'luna test', 'test_start': 154}, mode='create')
        self.assertEqual(errors, [])
        self.assertGreater(job_no, 0)

    def test_update_job_info_update_invalid_job(self):
        job_no, errors = _update_job_info(job_id='invalid', meta={'person': 'luna test'}, mode='update')
        self.assertIn('invalid job invalid', errors)
        self.assertEqual('invalid', 'invalid')

    def test_update_job_info_update_not_existent_job(self):
        job_no, errors = _update_job_info(job_id=500000000000, meta={'person': 'luna test'}, mode='update')
        self.assertIn('job 500000000000 not found', errors)
        self.assertEqual(500000000000, 500000000000)

    def test_update_job_info_rewrite_invalid_job(self):
        job_no, errors = _update_job_info(job_id='invalid', meta={'person': 'luna test'}, mode='rewrite')
        self.assertIn('invalid job invalid', errors)
        self.assertEqual('invalid', 'invalid')

    def test_update_job_info_rewrite_not_existent_job(self):
        job_no, errors = _update_job_info(job_id=500000000000, meta={'person': 'luna test'}, mode='rewrite')
        self.assertIn('job 500000000000 not found', errors)
        self.assertEqual(500000000000, 500000000000)

    def test_update_job_object_valid_status(self):
        job = Job.objects.get(id=2290)
        result = _update_job_object(job_object=job, request_data={'status': 'changed'})
        self.assertEqual(result, [])
        self.assertEqual(Job.objects.get(id=2290).status, 'changed')
        result_again = _update_job_object(job_object=job, request_data={'status': 'new'})
        self.assertEqual(result_again, [])

    def test_update_job_object_valid__status(self):
        job = Job.objects.get(id=2290)
        result = _update_job_object(job_object=job, request_data={'_status': 'changed_status'})
        self.assertEqual(result, [])
        self.assertEqual(Job.objects.get(id=2290).status, 'changed_status')
        result_again = _update_job_object(job_object=job, request_data={'_status': 'new'})
        self.assertEqual(result_again, [])

    def test_update_job_object_valid_test_start(self):
        job = Job.objects.get(id=2290)
        result = _update_job_object(job_object=job, request_data={'test_start': 1122})
        self.assertEqual(result, [])
        self.assertEqual(Job.objects.get(id=2290).test_start, 1122)
        result_again = _update_job_object(job_object=job, request_data={'test_start': 948})
        self.assertEqual(result_again, [])

    def test_update_job_object_valid__test_start(self):
        job = Job.objects.get(id=2290)
        result = _update_job_object(job_object=job, request_data={'_test_start': 2211})
        self.assertEqual(result, [])
        self.assertEqual(Job.objects.get(id=2290).test_start, 2211)
        result_again = _update_job_object(job_object=job, request_data={'_test_start': 948})
        self.assertEqual(result_again, [])

    def test_update_job_object_invalid_attribute(self):
        job = Job.objects.get(id=2290)
        status = job.status
        test_start = job.test_start
        result = _update_job_object(job_object=job, request_data={'person': 'lunapark'})
        self.assertEqual(result, [])
        self.assertEqual(job.status, status)
        self.assertEqual(job.test_start, test_start)

    @pytest.mark.skip
    def test_update_job_object_invalid_test_start (self):
        job = Job.objects.get(id=2290)
        status = job.status
        test_start = job.test_start
        result = _update_job_object(job_object=job, request_data={'test_start': 'lunapark'})
        self.assertIn('impossible to update job 2290 with new status or test start time', result)
        self.assertEqual(job.status, status)
        self.assertEqual(job.test_start, test_start)
        result = _update_job_object(job_object=job, request_data={'test_start': 10001})
        self.assertEqual(result, [])


class TestAPI(CommonTestCase):

    @pytest.mark.skip
    def test_create_metric(self):
        """
        Тесты ручки /create_metric/, которая:
        Создает метрику (data) и мета информацию к ней
        Принимает параметры метрики в виде простого тела POST запроса.
        - job - обзателен, указыает к какой джобе принадлежит эта метрика
        - offset становится параметром метрики (по умолчанию 0)
        - type становится параметром метрики (по умолчанию metrics)
        На все остальные параметры, переданные в теле создаются записи в таблице data_meta.
        name и group желательны, но имеют дефолт в виде --NONAME--
        :param request: django HTTP request
        :return: django HTTP response 200 (id метрики плэйн текстом) | 400
        """
        # Передаем корректный запрос.
        # Ожидаем 200 и наличие содержимого ответа
        data = {'offset': 999, 'name': 'some_name',
                'group': 'some', 'somemetainfo': 'ksdjfnsd', 'type': '5555', 'job': self.job2.id}
        resp = self.client.post('/create_metric/', data=data)
        self.assertEqual(resp.status_code, 200)
        self.assertTrue(resp.content)
        self.assertEqual(Data.objects.filter(job=self.job2, uniq_id=resp.content).count(), 1)
        # Запрос без указания номера стрельбы, к которой следует привязать метрику.
        # Ожидаем 400
        data = {'offset': 999, 'name': 'some_name',
                'group': 'some', 'somemetainfo': 'ksdjfnsd', 'type': '5555'}
        resp = self.client.post('/create_metric/', data=data)
        self.assertEqual(resp.status_code, 400)
        # Передаем корректный запрос без указания имени.
        # Ожидаем 200 и установленное по умолчанию имя метрики
        data = {'offset': 999, 'group': 'some', 'somemetainfo': 'ksdjfnsd', 'type': '5555', 'job': self.job2.id}
        resp = self.client.post('/create_metric/', data=data)
        self.assertEqual(resp.status_code, 200)
        data_object = Data.objects.get(job=self.job2, uniq_id=resp.content)
        self.assertEqual(DataMeta.objects.get(data=data_object, key='name').value, '--NONAME--')
        # Передаем корректный запрос без указания названия группы.
        # Ожидаем 200 и установленное по умолчанию название группы метрики
        data = {'offset': 999, 'name': 'some_name',
                'somemetainfo': 'ksdjfnsd', 'type': '5555', 'job': self.job2.id}
        resp = self.client.post('/create_metric/', data=data)
        self.assertEqual(resp.status_code, 200)
        data_object = Data.objects.get(job=self.job2, uniq_id=resp.content)
        self.assertEqual(DataMeta.objects.get(data=data_object, key='group').value, 'some')
        # Передаем корректный запрос без указания имени и названия группы.
        # Ожидаем 200 и установленное по умолчанию имя название группы метрики
        data = {'offset': 999, 'somemetainfo': 'ksdjfnsd', 'type': '5555', 'job': self.job2.id}
        resp = self.client.post('/create_metric/', data=data)
        self.assertEqual(resp.status_code, 200)
        data_object = Data.objects.get(job=self.job2, uniq_id=resp.content)
        self.assertEqual(DataMeta.objects.get(data=data_object, key='name').value, '--NONAME--')
        self.assertEqual(DataMeta.objects.get(data=data_object, key='group').value, '--NONAME--')

    @pytest.mark.skip
    def test_update_job_json(self):
        """
        Тесты ручки /update_job/, которая:
        Обновляет свойства стрельбы и мета информацию стрельбы
        Принимает параметры в виде простого тела POST запроса или json.
        status и test_start обновляются прямо в записи самой джобы в таблице job
        для остальных параметров обновляется или создается запись в таблице job_meta
        :param request: django HTTP request
        - job - номер изменяемой джобы
        :return: django HTTP response 200 | 400
        """
        data = {'test_start': 999, 'somemetainfo': 'ksdjfnsd', 'job': 5555, 'status': 'new'}
        resp = self.client.post(
            '/update_job/?job={}&job={}'.format(self.job1.id, self.job2.id),
            data=json.dumps(data),
            content_type='application/json'
        )
        self.assertEqual(resp.status_code, 200)
        updated_job1 = Job.objects.get(pk=self.job1.pk)
        self.assertEqual(updated_job1.test_start, data['test_start'])
        self.assertEqual(updated_job1.status, data['status'])
        self.assertEqual(updated_job1.meta, {'_duration': '53999000', 'somemetainfo': 'ksdjfnsd', '_test_start': '56765676567'})
        updated_job2 = Job.objects.get(pk=self.job2.pk)
        self.assertEqual(updated_job2.test_start, data['test_start'])
        self.assertEqual(updated_job2.status, data['status'])
        self.assertEqual(updated_job2.meta, {'_duration': '64999000', 'somemetainfo': 'ksdjfnsd'})

    @pytest.mark.skip
    def test_update_metric(self):
        """
        Тесты ручки /update_metric/, которая:
        Обновляет свойства метрики и мета информацию метрики
        Принимает параметры метрики в виде простого тела POST запроса или json.
        offset обновляется прямо в записи самой метрики в таблице data
        для остальных параметров обновляется или создается запись в таблице data_meta
        uniq_id изменить невозможно.
        :param request: django HTTP request
        - tag - уникальный айдишник метрики, которую надо изменить
        :return: 200 | 400
        """
        tag1 = 'e06b8e0759914b50962e40f0b863865d'
        tag2 = '04ac1a233bc94be589532872193e74f3'
        data = {'offset': 999, 'somemetainfo': 'ksdjfnsd', 'type': '5555'}
        resp = self.client.post('/update_metric/?tag={}&tag={}'.format(tag1, tag2), data=data)
        self.assertEqual(resp.status_code, 200)
        updated_data_object1 = Data.objects.get(uniq_id=tag1)
        self.assertEqual(updated_data_object1.type, 'metrics')  # этот атрибут ручка не меняет
        self.assertEqual(updated_data_object1.offset, data['offset'])
        self.assertIn('somemetainfo', updated_data_object1.meta)
        self.assertEqual(updated_data_object1.meta['somemetainfo'], data['somemetainfo'])
        updated_data_object2 = Data.objects.get(uniq_id=tag2)
        self.assertEqual(updated_data_object2.type, 'metrics')  # этот атрибут ручка не меняет
        self.assertEqual(updated_data_object2.offset, data['offset'])
        self.assertIn('somemetainfo', updated_data_object2.meta)
        self.assertEqual(updated_data_object2.meta['somemetainfo'], data['somemetainfo'])

    @pytest.mark.skip
    def test_update_metric_json(self):
        """
        Тесты ручки /update_metric/, которая:
        Обновляет свойства метрики и мета информацию метрики
        Принимает параметры метрики в виде простого тела POST запроса или json.
        offset обновляется прямо в записи самой метрики в таблице data
        для остальных параметров обновляется или создается запись в таблице data_meta
        uniq_id изменить невозможно.
        :param request: django HTTP request
        - tag - уникальный айдишник метрики, которую надо изменить
        :return: 200 | 400
        """
        tag1 = 'e06b8e0759914b50962e40f0b863865d'
        tag2 = '04ac1a233bc94be589532872193e74f3'
        data = {'offset': 999, 'somemetainfo': 'ksdjfnsd', 'type': '5555'}
        resp = self.client.post(
            '/update_metric/?tag=%s&tag=%s' % (tag1, tag2),
            data=json.dumps(data),
            content_type='application/json'
        )
        self.assertEqual(resp.status_code, 200)
        updated_data_object1 = Data.objects.get(uniq_id=tag1)
        self.assertEqual(updated_data_object1.type, 'metrics')  # этот атрибут ручка не меняет
        self.assertEqual(updated_data_object1.offset, data['offset'])
        self.assertIn('somemetainfo', updated_data_object1.meta)
        self.assertEqual(updated_data_object1.meta['somemetainfo'], data['somemetainfo'])
        updated_data_object2 = Data.objects.get(uniq_id=tag2)
        self.assertEqual(updated_data_object2.type, 'metrics')  # этот атрибут ручка не меняет
        self.assertEqual(updated_data_object2.offset, data['offset'])
        self.assertIn('somemetainfo', updated_data_object2.meta)
        self.assertEqual(updated_data_object2.meta['somemetainfo'], data['somemetainfo'])

    @pytest.mark.skip
    def test_close_job(self):
        """
        Тест ручки /close_job/, которая
        Выставляет status = finished
        триггерит агрегацию (в т.ч. duration)
        :param request: django HTTP request
        :return: django HTTP request 200 or 400
        """
        # дергаем ручку для двух джоб, проверяем, что статус стал finished и появилась мета для duration
        resp = self.client.get('/close_job/?job={}&job={}'.format(self.job1.pk, self.job2.pk))
        self.assertEqual(resp.status_code, 200)
        self.assertTrue(all([j.status == 'finished' for j in Job.objects.filter(id__in=(self.job1.id, self.job2.id))]))
        self.assertEqual(self.job1.meta['_duration'], '53999000')
        self.assertEqual(self.job2.meta['_duration'], '64999000')
        # дергаем ручку с указанием одного невалидного параметра (строка вместо номера стрельбы)
        resp = self.client.get('/close_job/?job={}&job={}'.format(self.job1.pk, 'jhbjhk'))
        self.assertEqual(resp.status_code, 400)

    @pytest.mark.skip
    def test_delete_job(self):
        """
        Тест ручки /delete_job/, которая
        Создает записаь в таблице deleted_job, копирует параметры удаляемой стрельбы в нее,
        переназначает метаинформацию и данные, приписывая к записи в deleted_job
        Удаляет стрельбу
        :param request: django HTTP request
        - job - номер удаляемой джобы
        :return: django HTTP response 200 | 400
        """
        # Передаем некорректный параметр (строку) вместо номера стрельбы
        # Ожидаем 400
        resp = self.client.get('/delete_job/?job=%s' % 'ksdjfw')
        self.assertEqual(resp.status_code, 400)
        # Передаем некорректный номер стрельбы
        # Ожидаем 400
        resp = self.client.get('/delete_job/?job=%s' % '0')
        self.assertEqual(resp.status_code, 400)
        # Передаем некорректный номер стрельбы
        # Ожидаем 400
        resp = self.client.get('/delete_job/?job=%s' % str(max([self.job2.id, self.job1.id]) + 3))
        self.assertEqual(resp.status_code, 400)
        # Передаем корректный запрос
        resp = self.client.get('/delete_job/?job=%s' % self.job2.id)
        self.assertEqual(resp.status_code, 200)
        # Убеждаемся, что менеджер объектов отфильтровывает удаленную стрельбу
        self.assertEqual([j.id for j in Job.objects.all()], [self.job1.id])

    @pytest.mark.skip
    def test_unfold_job(self):
        with open('%s/volta/common/tests/volta_job.zip' % os.getcwd(), 'rb') as zf:
            resp = self.client.post(
                '/unfold_job/',
                data=zf.read(),
                content_type='application/zip'
            )
            self.assertEqual(resp.status_code, 200)
            print(resp.content)

    def test_job_meta(self):
        resp = self.client.get('/get_job_meta/?job={}&job={}'.format(525, 526))
        self.assertEqual(resp.status_code, 200)
        self.assertIsInstance(json.loads(resp.content.decode('utf-8')), dict)

    @pytest.mark.skip
    def test_fragments(self):
        resp = self.client.get('/job/%s/fragments' % self.job1.id)
        self.assertEqual(resp.status_code, 200)
        self.assertTrue(json.loads(resp.content.decode('utf-8')))

    def test_get_invalid_job_object(self):
        with pytest.raises(AssertionError):
            _get_job_object('aa')
        with pytest.raises(ObjectDoesNotExist):
            _get_job_object(500000000000000000000000000000000000000000000)

    def test_get_valid_job_object(self):
        job = _get_job_object(5)
        self.assertEqual(Job.objects.get(id=5), job)

    def test_key_get_diff_create(self):
        new_keys = {'meta1', 'meta2'}
        add, update, delete = _get_key_diff(set(), new_keys, 'create')
        self.assertEqual((add, update, delete), ({'meta1', 'meta2'}, set(), set()))

    def test_key_get_diff_update(self):
        new_keys = {'meta1', 'meta2', 'quit_status'}
        add, update, delete = _get_key_diff(set(Job.objects.get(id=5).meta.keys()), new_keys, 'update')
        self.assertEqual((add, update, delete), ({'meta1', 'meta2'}, {'quit_status'}, set()))

    def test_key_get_diff_rewrite(self):
        new_keys = {'meta1', 'meta2', 'quit_status'}
        old = set(Job.objects.get(id=5).meta.keys())
        add, update, delete = _get_key_diff(old, new_keys, 'rewrite')
        self.assertEqual((add, update, delete), (new_keys - old, {'quit_status'}, old - new_keys))
