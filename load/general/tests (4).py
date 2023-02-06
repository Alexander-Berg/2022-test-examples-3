import json
import pytest
from common.tests import CommonTestCase
from common.models import JobMeta
from job_list.views import _preview


class JobListPageTests(CommonTestCase):
    """
    Тесты приложения job_list
    """

    def test_layout(self):
        """
        Функция layout
        Пока возвращает просто захардкоженный LAYOUT_TEMPLATE.
        :param request: django HTTP request
        :return: django HTTP request с json телом
        """
        resp = self.client.get('/job_list_layout')
        self.assertEqual(resp.status_code, 200)

    @pytest.mark.skip
    def test_job_list(self):
        """
        Тесты функции job_list

        - filter - набор фильтров, например filter=person:noob|direvius то есть person = или noob, или direvius
        - fields - набор требуемых полей
        - order - по какому полю или метаинформации сортировать. используем джанговый синтаксис для направления:
            id = asc, -id = desc
        - after - id последней стрельбы, которая уже есть на фронте
        - count - количество запрашиваемых стрельб

        :param request: django HTTP request
        :return: django HTTP response 200 со списком информации по стрельбам согласно запрошенным фильтрам и пр. Или 400.
        """

        # создаем метаинформацию для стрельб
        JobMeta.objects.create(
            job=self.job1,
            key='someKey',
            value='someValue'
        )
        JobMeta.objects.create(
            job=self.job2,
            key='someKey',
            value='anotherValue'
        )
        JobMeta.objects.create(
            job=self.job2,
            key='anotherKey',
            value='shmanotherValue'
        )
        JobMeta.objects.create(
            job=self.job1,
            key='_duration',
            value='555'
        )
        # resp = self.client.get('/job_list/')
        # self.assertTrue(len(json.loads(resp.content.decode('utf-8'))['tests']) > 0)
        # # Фильтрация по метаинформации
        # resp = self.client.get('/job_list/?filter=someKey:someValue')
        # self.assertEqual([t['_id'] for t in json.loads(resp.content.decode('utf-8'))['tests']], [self.job1.pk])
        # # Фильтрация по метаинформации с перечислением значений (ИЛИ)
        # # Сортировка по умолчанию - по id в обратном порядке.
        # resp = self.client.get('/job_list/?filter=someKey:someValue|anotherValue')
        # self.assertEqual([t['_id'] for t in json.loads(resp.content.decode('utf-8'))['tests']], [self.job2.pk, self.job1.pk])
        # # Фильтры учитываются как "И"
        # # Ожидаем ответ с в пустым списком
        # resp = self.client.get('/job_list/?filter=someKey:someValue&filter=anotherKey:shmanotherValue')
        # self.assertEqual([t['_id'] for t in json.loads(resp.content.decode('utf-8'))['tests']], [])
        # # Фильтры учитываются как "И"
        # # Ожидаем ответ с одной стрельбой, для которой оба условия соблюдены
        # resp = self.client.get('/job_list/?filter=someKey:anotherValue&filter=anotherKey:shmanotherValue')
        # self.assertEqual([t['_id'] for t in json.loads(resp.content.decode('utf-8'))['tests']], [self.job2.pk])
        # # Сортировка (order) по id в прямом порядке
        # resp = self.client.get('/job_list/?filter=someKey:someValue|anotherValue&order=id')
        # self.assertEqual([t['_id'] for t in json.loads(resp.content.decode('utf-8'))['tests']], [self.job1.pk, self.job2.pk])
        # # Сортировка (order) по метаинформации в обратном порядке
        # resp = self.client.get('/job_list/?filter=someKey:someValue|anotherValue&order=-someKey')
        # self.assertEqual([t['_id'] for t in json.loads(resp.content.decode('utf-8'))['tests']], [self.job1.pk, self.job2.pk])
        # # Сортировка (order) по метаинформации в прямом порядке
        # resp = self.client.get('/job_list/?filter=someKey:someValue|anotherValue&order=someKey')
        # self.assertEqual([t['_id'] for t in json.loads(resp.content.decode('utf-8'))['tests']], [self.job2.pk, self.job1.pk])
        # # Указываем
        # resp = self.client.get('/job_list/?filter=someKey:someValue|anotherValue&order=id&after=%s' % self.job1.pk)
        # self.assertEqual([t['_id'] for t in json.loads(resp.content.decode('utf-8'))['tests']], [self.job2.pk])
        # # Если при сортировке по мете не у всех стрельб будет мета с запрошенным ключом,
        # # то такие стрельбы попадут в конец списка
        # resp = self.client.get('/job_list/?order=anotherKey')
        # self.assertEqual([t['_id'] for t in json.loads(resp.content.decode('utf-8'))['tests']],
        #                  [self.job2.pk, self.job1.pk])
        # Фильтрация происходит по подстроке без учета регистра
        # Ожидаем ответ с обеими стрельбами
        resp = self.client.get('/job_list/?filter=someKey:value&order=id')
        self.assertEqual([t['_id'] for t in json.loads(resp.content.decode('utf-8'))['tests']],
                         [self.job1.pk, self.job2.pk])

    @pytest.mark.skip
    def test_job_list_groups(self):
        """
        Тест ручки /job_list_groups/, которая
        Возвращает список групп, на основании значений ключа, переданного в параметре groupby,
        среди стрельб, отфильтрованных переданными фильтрами
        :param request: django HTTP request
        - filter - набор фильтров, например filter=person:noob|direvius то есть person = или noob, или direvius
        - groupby - параметр - ключ метаинформации, значения которого и будут являться группами
        :return: django HTTP request со списком групп в формате json
        """
        JobMeta.objects.create(
            job=self.job1,
            key='someKey',
            value='someValue'
        )
        JobMeta.objects.create(
            job=self.job2,
            key='someKey',
            value='anotherValue'
        )
        JobMeta.objects.create(
            job=self.job2,
            key='anotherKey',
            value='anotherValue'
        )
        JobMeta.objects.create(
            job=self.job1,
            key='_duration',
            value='555'
        )
        self.job2.status = 'anew2'
        self.job2.save()
        resp = self.client.get('/job_list_groups/?groupby=status')
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(json.loads(resp.content.decode('utf-8')), ['anew2', 'new'])
        resp = self.client.get('/job_list_groups/?groupby=someKey')
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(json.loads(resp.content.decode('utf-8')), ['anotherValue', 'someValue'])

    @pytest.mark.skip
    def test__preview(self):
        p = _preview(self.job2)
        self.assertEqual(len(p.decode('utf-8').split('\n')), 66)
        self.assertEqual(len(p.decode('utf-8').split('\n')[0].split(',')), 12)
