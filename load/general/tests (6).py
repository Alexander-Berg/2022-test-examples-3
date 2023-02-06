import json
import pytest
import datetime

from parameterized import parameterized
import yaml

from api.views import _add_job_to_regression as set_job_regressions
from common.models import Job, Regression, RegressionMeta, RegressionSeries, Data, DataMeta, SLA, Case
from common.tests import CommonTestCase
from common.util import ClickhouseClient
from regression.util.stat_functions import StatFunctions
from regression.services import _validate_creation, check_filters, compute_regression_value, filter_job_metrics

from settings.base import BASE_DIR


class RegressionGetMetricsMetaTests(CommonTestCase):
    """
    Тесты ручки get_metrics_meta
    """

    def test_empty_request_400(self):
        """
        GIVEN backend started
        WHEN call the handler with empty parameters
        THEN HTTP 400 is given
        """
        resp = self.client.get('/get_metrics_meta/')
        self.assertEqual(resp.status_code, 400)

    def test_with_invalid_body_400(self):
        """
        GIVEN backend started
        WHEN call the handler with empty body
        THEN HTTP 400 is given
        """
        resp = self.client.get('/get_metrics_meta',
                               data={'job': ['1', '2', 'BOBBY TABLES']},
                               content_type='application/json'
                       )

        self.assertEqual(resp.status_code, 400)

    def test_with_jobs_not_found(self):
        """
        GIVEN backend started
        WHEN call the handler with valid body
        THEN HTTP 200 is given
        """
        resp = self.client.get('/get_metrics_meta',
                               data={'job': [1, 2, 3]},
                               content_type='application/json')
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(json.loads(resp.content.decode('utf-8')), {
            '1': {},
            '2': {
                '23897b45299d488fb156ee69a3852f48': {
                    '_has_histograms': '0',
                    'offset': '0',
                    'name': 'current',
                    'type': 'metric'
                }
            },
            '3': {
                '067df02b9e9c41c2b2face7811762214': {
                    '_has_histograms': '0'
                }
            }
        })

    def test_with_jobs_found(self):
        """
        GIVEN backend started
        WHEN call the handler with valid body
        THEN HTTP 200 is given

        NB: data is taken from testing DB
        """
        resp = self.client.get('/get_metrics_meta',
                               data={'job': [864, 827, 833, 901, 865]},
                               content_type='application/json')
        with open(BASE_DIR + '/regression/files/get_metrics_meta_response.json') as file:
            result = file.read()
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(json.loads(resp.content.decode('utf-8')), json.loads(result))


class GetRegressionTests(CommonTestCase):
    FILES_DIR = BASE_DIR + '/regression/files/'

    def test_no_regression(self):
        resp = self.client.get('/regression/no_such_regression_heh/')
        self.assertEqual(resp.status_code, 400)

    def test_successfull_get(self):
        resp = self.client.get('/regression/new_namemm_o/')
        self.assertEqual(resp.status_code, 200)
        with open(self.FILES_DIR + 'successfull_get.json') as file:
            model_response = json.loads(file.read())
        self.assertEqual(json.loads(resp.content.decode('utf8')), model_response)

    def test_function_with_args(self):
        resp = self.client.get('/regression/test_get_layout_a/')
        self.assertEqual(resp.status_code, 200)
        with open(self.FILES_DIR + 'get_regression_with_args.json') as file:
            model_response = json.loads(file.read())
        self.assertEqual(json.loads(resp.content), model_response)

    def test_regression_with_sla_name(self):
        resp = self.client.get('/regression/test_sla_name/')
        self.assertEqual(resp.status_code, 200)
        dict_response = json.loads(resp.content)
        self.assertEqual(dict_response['series_list'][0]['sla'][0]['name'], 'i am sla, aaalala')


class EditRegressionTests(CommonTestCase):
    FILES_DIR = BASE_DIR + '/regression/files/'

    def test_wrong_config(self):
        with open(self.FILES_DIR + 'edit_regression_wrong_test.yaml') as file:
            data = file.read()
        resp = self.client.put('/regression/new_namemm_o/', data=data)
        self.assertEqual(resp.status_code, 422)

    def test_no_modify(self):
        with open(self.FILES_DIR + 'edit_regression_no_modify.yaml') as file:
            data = file.read()
        resp = self.client.put('/regression/new_namemm_o/', data=data)
        self.assertEqual(resp.status_code, 200)

    def test_modify_regression_meta(self):
        regression = Regression.objects.get(name__startswith='foo_b')
        name = regression.name
        resp = self.client.get('/regression/{}/'.format(name))
        config = json.loads(resp.content.decode('utf8'))
        config['meta']['new_key'] = 'new_value'

        r = self.client.put('/regression/{}/'.format(name), data=config)
        assert r.status_code == 200, r.content.decode('utf8')
        # meta_value = RegressionMeta.objects.get(regression=regression, key='new_key').value
        resp = self.client.get('/regression/{}/'.format(name))
        config = json.loads(resp.content.decode('utf8'))
        self.assertEqual(config['meta']['new_key'], 'new_value')

        config['meta']['new_key'] = 'another_value'
        r = self.client.put('/regression/{}/'.format(name), data=config)
        assert r.status_code == 200, r.content.decode('utf8')
        # meta_value = RegressionMeta.objects.get(regression=regression, key='new_key').value
        resp = self.client.get('/regression/{}/'.format(name))
        config = json.loads(resp.content.decode('utf8'))
        self.assertEqual(config['meta']['new_key'], 'another_value')

        RegressionMeta.objects.filter(regression=regression).delete()

    def test_modify_regression_sla(self):
        regression = Regression.objects.get(name__startswith='foo_b')
        name = regression.name
        resp = self.client.get('/regression/{}/'.format(name))
        config = json.loads(resp.content.decode('utf8'))

        new_sla = [{
            "filter": {"name": "interval_event", "type": "metric"},
            "sla": [{"function": "avg", "ge": 0.2}]
        }]
        config['series_list'] = new_sla
        self.client.put('/regression/{}/'.format(name), data=config)

        series_list = regression.regressionseries_set.all()
        sla_from_db = [{'filter': seria.filter,
                        'sla': [sla.dict_repr for sla in seria.sla_set.all()]} for seria in series_list]
        self.assertEqual(sla_from_db, new_sla)
        RegressionSeries.objects.filter(regression=regression).delete()

    def test_modify_regression_name(self):
        regression = Regression.objects.get(name__startswith='foo_f')
        old_name = regression.name
        resp = self.client.get('/regression/{}/'.format(old_name))
        config = json.loads(resp.content.decode('utf8'))
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(config, {'name': old_name, 'test_ids': [], 'meta': {}, 'series_list': []})

        new_name = 'regression_modified'
        config['name'] = new_name
        resp = self.client.put('/regression/{}/'.format(old_name), data=config)
        self.assertEqual(resp.status_code, 200)
        self.assertTrue(json.loads(resp.content.decode('utf8'))['regression'].startswith(new_name))

        regression = Regression.objects.get(name__startswith='regression_modified')
        regression.name = 'foo_f'
        regression.save()


class CreateRegressionTests(CommonTestCase):
    REQUEST_DIR = BASE_DIR + '/regression/files/'

    def test_not_yaml(self):
        resp = self.client.post('/regression/create',
                                ': : : : :',
                                content_type='application/x-yaml')
        self.assertEqual(resp.status_code, 400)

    def test_not_dict(self):
        resp = self.client.post('/regression/create',
                                '[1, 2, 3]',
                                content_type='application/x-yaml')
        self.assertEqual(resp.status_code, 400)

    def test_empty_post(self):
        resp = self.client.post('/regression/create',
                                '',
                                content_type='application/x-yaml')
        self.assertEqual(resp.status_code, 400)

    def test_no_person(self):
        data = {'name': 'Vasya'}
        request = {
            'config': data
        }
        resp = self.client.post('/regression/create',
                                yaml.dump(request),
                                content_type='application/x-yaml')
        self.assertEqual(resp.status_code, 400)

    def test_no_name(self):
        data = {'group_by': 'id', 'series_list': []}
        request = {
            'person': 'belikoff',
            'config': data
        }
        resp = self.client.post('/regression/create',
                                yaml.dump(request),
                                content_type='application/x-yaml')
        self.assertEqual(resp.status_code, 422)
        self.assertIn('name', json.loads(resp.content.decode('utf8')).keys())

    def test_unknown_field(self):
        data = {'name': 'Vasya', 'surname': 'Ivanov'}
        request = {
            'person': 'belikoff',
            'config': data
        }
        resp = self.client.post('/regression/create',
                                yaml.dump(request),
                                content_type='application/x-yaml')
        self.assertEqual(resp.status_code, 422)
        self.assertIn('surname', json.loads(resp.content.decode('utf8')).keys())

    def test_not_list_series(self):
        with open(self.REQUEST_DIR + 'wrong_series_type.yaml') as file:
            data = yaml.safe_load(file.read())
        normalized_data, errors = _validate_creation(data)
        self.assertIn('series_list', errors)

    def test_test_id_not_number(self):
        with open(self.REQUEST_DIR + 'wrong_test_id_type.yaml') as file:
            data = yaml.safe_load(file.read())
        normalized_data, errors = _validate_creation(data)
        self.assertIn('test_ids[0]', errors)
        self.assertIn('test_ids[1]', errors)

    def test_full_request(self):
        with open(self.REQUEST_DIR + 'full_request.yaml') as file:
            data = yaml.safe_load(file.read())
        normalized_data, errors = _validate_creation(data)
        self.assertFalse(errors)
        self.assertEqual(data, normalized_data)

    def test_setting_defaults(self):
        with open(self.REQUEST_DIR + 'name_only.yaml') as file:
            data = yaml.safe_load(file.read())
        normalized_data, errors = _validate_creation(data)
        self.assertFalse(errors)
        self.assertIn('group_by', normalized_data)
        self.assertIn('series_list', normalized_data)
        self.assertIn('test_ids', normalized_data)
        self.assertIn('meta', normalized_data)

    def test_request_with_meta(self):
        with open(self.REQUEST_DIR + 'with_meta.yaml') as file:
            data = yaml.safe_load(file.read())
        normalized_data, errors = _validate_creation(data)
        self.assertFalse(errors)

    @pytest.mark.skip
    def test_simple_percent(self):
        with open(self.REQUEST_DIR + 'simple_percent.yaml') as file:
            data = yaml.safe_load(file.read())
        normalized_data, errors = _validate_creation(data)
        self.assertFalse(errors)

    @pytest.mark.skip
    def test_percent_multiple_args(self):
        with open(self.REQUEST_DIR + 'percent_multiple_args.yaml') as file:
            data = yaml.safe_load(file.read())
        normalized_data, errors = _validate_creation(data)
        self.assertFalse(errors)

    @pytest.mark.skip
    def test_percent_no_args(self):
        with open(self.REQUEST_DIR + 'test_percent_no_args.yaml') as file:
            data = yaml.safe_load(file.read())
        normalized_data, errors = _validate_creation(data)
        self.assertTrue(errors)

    def test_percent_wrong_formatting(self):
        with open(self.REQUEST_DIR + 'percent_wrong_formatting.yaml') as file:
            data = yaml.safe_load(file.read())
        normalized_data, errors = _validate_creation(data)
        self.assertTrue(errors)

    def test_max_with_args(self):
        with open(self.REQUEST_DIR + 'max_with_args.yaml') as file:
            data = yaml.safe_load(file.read())
        normalized_data, errors = _validate_creation(data)
        self.assertTrue(errors)

    def test_jsonpath_errors_test_ids(self):
        with open(self.REQUEST_DIR + 'jsonpath_errors_test1.yaml') as file:
            data = yaml.safe_load(file.read())
        normalized_data, errors = _validate_creation(data)
        with open(self.REQUEST_DIR + 'jsonpath_errors1.json') as file:
            model_errors = json.loads(file.read())
        self.assertEqual(errors, model_errors)

    @pytest.mark.skip
    def test_jsonpath_errors_two_lists(self):
        with open(self.REQUEST_DIR + 'jsonpath_errors_test2.yaml') as file:
            data = yaml.safe_load(file.read())
        normalized_data, errors = _validate_creation(data)
        with open(self.REQUEST_DIR + 'jsonpath_errors2.json') as file:
            model_errors = json.loads(file.read())
        self.assertEqual(errors, model_errors)

    def test_jsonpath_multiple_errors(self):
        with open(self.REQUEST_DIR + 'jsonpath_errors_test3.yaml') as file:
            data = yaml.safe_load(file.read())
        normalized_data, errors = _validate_creation(data)
        with open(self.REQUEST_DIR + 'jsonpath_errors3.json') as file:
            model_errors = json.loads(file.read())
        self.assertEqual(errors, model_errors)

    @pytest.mark.skip
    def test_jsonpath_function_errors(self):
        with open(self.REQUEST_DIR + 'jsonpath_errors_test4.yaml') as file:
            data = yaml.safe_load(file.read())
        normalized_data, errors = _validate_creation(data)
        with open(self.REQUEST_DIR + 'jsonpath_errors4.json') as file:
            model_errors = json.loads(file.read())
        self.assertEqual(errors, model_errors)

    def test_successfull_q_cum(self):
        with open(self.REQUEST_DIR + 'successfull_q_cum_test.yaml') as file:
            data = yaml.safe_load(file.read())
        normalized_data, errors = _validate_creation(data)
        self.assertFalse(errors)
        with open(self.REQUEST_DIR + 'successfull_q_cum.json') as file:
            model_normalized_data = json.loads(file.read())
        self.assertEqual(normalized_data, model_normalized_data)


class AddJobToRegressionTests(CommonTestCase):
    def test_get_request(self):
        resp = self.client.get('/regression/wfqqfe_a/add_jobs?job=2500&job=2600&job=100500&job=2300')
        self.assertEqual(resp.status_code, 405)

    def test_no_regression(self):
        resp = self.client.post('/regression/there_is_no_regression_with_this_name/add_jobs/',
                                json.dumps([2500]),
                                content_type="application/json")
        self.assertEqual(resp.status_code, 400)

    def test_errors_request(self):
        resp = self.client.post('/regression/wfqqfe_a/add_jobs/',
                                json.dumps([2500, 2600, 100500, 2300]),
                                content_type="application/json")
        self.assertEqual(resp.status_code, 200)
        correct_result = {
            'added_tests': [],
            'errors':  [
                "Job with id 2500 already presented in regression",
                "Job with id 2600 already presented in regression",
                "Job with id 100500 doesn't exist",
                "Job with id 2300 already presented in regression"
            ]
        }
        self.assertEqual(json.loads(resp.content.decode('utf8')), correct_result)

    def test_successfull_addition(self):
        resp = self.client.post('/regression/wfqqfe_a/add_jobs/',
                                json.dumps([2000, 2800]),
                                content_type="application/json")
        self.assertEqual(resp.status_code, 200)
        correct_result = {
            'added_tests': [2000, 2800],
            'errors': []
        }

        job1 = Job.objects.get(id=2000)
        set_job_regressions(job1, ['wfqqfe_a '])

        job2 = Job.objects.get(id=2800)
        set_job_regressions(job2, ['wfqqfe_a'])

        self.assertEqual(json.loads(resp.content.decode('utf8')), correct_result)


class DeleteJobFromRegression(CommonTestCase):
    def test_get_request(self):
        resp = self.client.get('/regression/wfqqfe_a/delete_jobs/?job=2981&job=3000')
        self.assertEqual(resp.status_code, 405)

    def test_no_regression(self):
        resp = self.client.post('/regression/there_is_no_regression_with_this_name/delete_jobs/',
                                json.dumps([2500]),
                                content_type="application/json")
        self.assertEqual(resp.status_code, 400)

    def test_errors_and_deletions(self):
        resp = self.client.post('/regression/wfqqfe_a/delete_jobs/',
                                json.dumps([2981, 2700]),
                                content_type="application/json")
        self.assertEqual(resp.status_code, 200)
        correct_result = {
            'deleted_tests': [2981],
            'errors': [
                "Job with id 2700 isn't presented in regression"
            ]
        }

        job = Job.objects.get(id=2981)
        set_job_regressions(job, ['wfqqfe_a'])

        self.assertEqual(json.loads(resp.content.decode('utf8')), correct_result)


class RegressionListTests(CommonTestCase):
    FILES_DIR = BASE_DIR + '/regression/files/'

    @pytest.mark.skip
    def test_empty_request(self):
        resp = self.client.get('/regression_list', {})
        self.assertEqual(resp.status_code, 200)
        result = json.loads(resp.content.decode('utf8'))
        self.assertTrue(result['no_more'])

    def test_order_by_id(self):
        resp = self.client.get('/regression_list', {'order': 'id'})
        self.assertEqual(resp.status_code, 200)
        result = json.loads(resp.content.decode('utf8'))
        with open(self.FILES_DIR + 'regr_list_order_id.json') as file:
            answer = json.loads(file.read())
        self.assertEqual(result, answer)


class GetRegressionMetaTests(CommonTestCase):
    def test_no_regressions(self):
        resp = self.client.get('/get_regression_meta', {})
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(json.loads(resp.content.decode('utf8')), {})

    def test_nonexistent_regression(self):
        params = {'regression': [100500]}
        resp = self.client.get('/get_regression_meta', params)
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(json.loads(resp.content.decode('utf8')), {})


class GetRegressionDataTests(CommonTestCase):

    def test_no_sla_given(self):
        resp = self.client.get('/get_regression_data')
        self.assertEqual(resp.status_code, 400)
        self.assertEqual(json.loads(resp.content.decode('utf-8')), {'errors': ['No sla ids given']})

    def test_not_int_sla_id(self):
        resp = self.client.get('/get_regression_data', {'sla_id': 'sdggsgsdg'})
        self.assertEqual(resp.status_code, 400)
        self.assertEqual(json.loads(resp.content.decode('utf-8')), {'errors': ['Sla ids should be integers: sdggsgsdg']})

    def test_missing_sla_id(self):
        resp = self.client.get('/get_regression_data', {'sla_id': 10000000000})
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(json.loads(resp.content.decode('utf-8')), {'result': '', 'errors': ['No data for sla ids: 10000000000']})

    def test_corrupted_sla_id(self):
        resp = self.client.get('/get_regression_data', {'sla_id': 33})
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(json.loads(resp.content.decode('utf-8')), {'result': '', 'errors': ['Corrupted sla ids: 33']})

    def test_positive_result(self):
        params = {'sla_id': [33, 36]}
        resp = self.client.get('/get_regression_data', params)
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(json.loads(resp.content),
                         {"result": "x,36_y\n1758,302157.0\n", "errors": ["Corrupted sla ids: 33"]})


class GetLayoutTests(CommonTestCase):
    FILES_DIR = BASE_DIR + '/regression/files/'
    maxDiff = None

    def test_layout_400(self):
        resp = self.client.get('/get_regression_layout/')
        self.assertEqual(resp.status_code, 400)

        resp = self.client.get('get_regression_layout', {'regression': 'no_regression_with_this_name'})
        self.assertEqual(resp.status_code, 404)

    def test_custom_widget_name(self):
        sla = SLA.objects.get(id=175)
        sla.name = 'Check applied name'
        sla.save()
        self.assertEqual(sla.widget_name(), 'Check applied name')
        sla.name = ''
        sla.save()
        self.assertEqual(sla.widget_name(), 'min in name: interval_real __overall__')

    def test_complicated_layout(self):
        resp = self.client.get('/get_regression_layout/', {'regression': 'test_get_layout_a'})
        self.assertEqual(resp.status_code, 200)
        with open(self.FILES_DIR + 'complicated_layout.json') as file:
            model_response = json.loads(file.read())
        self.assertEqual(json.loads(resp.content), model_response)


class RegressionCheckFilterTests(object):
    """
    Тесты ручки check_filter_for_single_metrics
    """
    def test_no_metrics_found(self):
        filter_ = {'hostname': 'localhost', 'name': 'cpu_usage'}
        assert len(filter_job_metrics(job_id=147, fltr=filter_)) == 0

    def test_one_metrics_found(self):
         filter_ = {'hostname': 'localhost'}
         assert len(filter_job_metrics(job_id=147, fltr=filter_)) == 1

    def test_several_metrics_found(self):
        filter_ = {'importance': 'high'}
        assert len(filter_job_metrics(job_id=2929, fltr=filter_)) == 15

    def test_empty_filter(self):
        filter_ = {}
        assert len(filter_job_metrics(job_id=147, fltr=filter_)) == 5


class CheckFiltersTests(CommonTestCase):

    @classmethod
    def setUpClass(cls):
        super(CheckFiltersTests, cls).setUpClass()
        cls.job1 = Job.objects.create()
        data11 = Data.objects.create(job=cls.job1)
        Case.objects.create(data=data11, name=Case.OVERALL)
        DataMeta.objects.create(data=data11, key='foo', value='1')
        DataMeta.objects.create(data=data11, key='bar', value='1')

        data12 = Data.objects.create(job=cls.job1)
        Case.objects.create(data=data12, name=Case.OVERALL)
        DataMeta.objects.create(data=data12, key='foo', value='1')
        DataMeta.objects.create(data=data12, key='bar', value='2')

    def test_check_filters_fails(self):
        error = check_filters(
            [self.job1],
            [{'filter': {'foo': 1},
              'sla': {}}])
        assert len(error) == 1
        assert "Filter {'foo': 1} returned more than one metric" in error[0]

    def test_check_filters_passes_0(self):
        assert check_filters([self.job1],
                             [{'filter': {'foo': '2'},
                               'sla': {}}]) == []

    def test_check_filters_passes_1(self):
        assert check_filters([self.job1],
                             [{'filter': {'bar': '2'},
                               'sla': {}}]) == []


class ComputeRegressionValueTests(CommonTestCase):
    def test_no_value_for_metric(self):
        job = Job.objects.get(id=2980)
        sla = SLA.objects.get(id=271)
        result, error = compute_regression_value(
            job, sla.function, sla.args, sla.regression_series.filter)
        self.assertEqual(result, None)

    def test_value_error_for_metric(self):
        job = Job.objects.get(id=2989)
        sla = SLA.objects.get(id=247)
        result, error = compute_regression_value(
            job, sla.function, sla.args, sla.regression_series.filter)
        self.assertEqual(result, None)

    def test_compute_correct_value(self):
        job = Job.objects.get(id=2989)
        sla = SLA.objects.get(id=111)
        result, error = compute_regression_value(
            job, sla.function, sla.args, sla.regression_series.filter)
        self.assertEqual(result, 1566301139321901)


class StatFunctionsTests(CommonTestCase):

    class Data:
        def __init__(self, tag, has_raw=False, has_histograms=False, has_aggregates=False):
            self.tag = tag
            self.has_raw = has_raw
            self.has_histograms = has_histograms
            self.has_aggregates = has_aggregates

    @classmethod
    def setUpClass(cls):
        super(StatFunctionsTests, cls).setUpClass()
        job = Job.objects.create()
        hist_data_object = Data.objects.create(job=job)
        cls.hist = Case.objects.create(data=hist_data_object, name=Case.OVERALL)

        raw_data_object = Data.objects.create(job=job)
        cls.raw = Case.objects.create(data=raw_data_object, name=Case.OVERALL)

        cls.aggr_tag = 'test_aggr_{}'.format(datetime.datetime.now().isoformat())
        aggr_data_object = Data.objects.create(job=job)
        cls.aggr = Case.objects.create(data=aggr_data_object, name=Case.OVERALL)

        cc_client = ClickhouseClient()
        hist_values = [
            (cls.hist.tag.hex, 1575650133, '200', 10),
            (cls.hist.tag.hex, 1575650133, '302', 5),
            (cls.hist.tag.hex, 1575650133, '404', 2),
            (cls.hist.tag.hex, 1575650133, '500', 3),
            # 20
            (cls.hist.tag.hex, 1575650134, '200', 10),
            (cls.hist.tag.hex, 1575650134, '302', 3),
            (cls.hist.tag.hex, 1575650134, '404', 10),
            (cls.hist.tag.hex, 1575650134, '500', 7),
            # 30
            (cls.hist.tag.hex, 1575650135, '200', 20),
            (cls.hist.tag.hex, 1575650135, '302', 10),
            (cls.hist.tag.hex, 1575650135, '400', 2),
            (cls.hist.tag.hex, 1575650135, '500', 8),
            # 40
            (cls.hist.tag.hex, 1575650136, '200', 15),
            (cls.hist.tag.hex, 1575650136, '305', 5),
            (cls.hist.tag.hex, 1575650136, '404', 4),
            (cls.hist.tag.hex, 1575650136, '500', 6),
            # 30
            (cls.hist.tag.hex, 1575650137, '200', 10),
            (cls.hist.tag.hex, 1575650137, '302', 5),
            (cls.hist.tag.hex, 1575650137, '404', 2),
            (cls.hist.tag.hex, 1575650137, '500', 3),
            # 20
            # 140
        ]
        cc_client.insert("INSERT INTO histograms (tag, ts, category, cnt) VALUES {values}",
                         {'values': ', '.join(str(v) for v in hist_values)},
                         escape_str=False)
        raw_values = [
            (cls.raw.tag.hex, 1575650133 + n, i) for n, i in enumerate([9, 15, 17, 23, 22, 21, 25, 26, 29, 28, 23, 19])
        ]
        cc_client.insert("INSERT INTO metrics (tag, ts, value) VALUES {values}",
                         {'values': ', '.join(str(v) for v in raw_values)},
                         escape_str=False)

    @parameterized.expand([
        (('200',), 46.4),
        (('3\d\d',), 20.0),
        (('.00', '4..'), 80.0)
    ])
    def test_percent(self, arg, expected):
        assert StatFunctions.percent(self.hist, *arg) == expected

    @parameterized.expand([
        (('200',), 65),
        (('3\d\d',), 28),
        (('30.', '2..'), 93)
    ])
    def test_total(self, arg, expected):
        assert StatFunctions.total(self.hist, *arg) == expected

    def test_median_hist(self):
        assert StatFunctions.median(self.hist) == 30

    def test_median_raw(self):
        assert StatFunctions.median(self.raw) == 22.5

    def test_percent_rank_hist(self):
        assert StatFunctions.percent_rank(self.hist, 200) == 46.4

    def test_stddev(self):
        assert StatFunctions.stddev(self.raw) == 5.48
