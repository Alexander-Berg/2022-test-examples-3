# -*- coding: utf-8 -*-
from __future__ import unicode_literals

import json
import mock
import requests
import datetime

from django.contrib.auth.models import User
from django.http import HttpResponse, HttpRequest
from takeout.tvm_ticketer import Ticketer
from common.models import Job

from django.test import TestCase
from django.test.client import RequestFactory

from .views import get_results, get_user_status, delete_user_data
from .takeout_tools import get_user_id_from_blackbox, send_response, collect_data

####################
# class TestTakeoutCreateJob(TestCase):

#     def setUp(self):
#         # Every test needs access to the request factory.
#         self.factory = RequestFactory()

#     # http codes

#     def test_code_no_uid_given(self):
#         # Give an error if there is no uid in request
#         req = self.factory.post(path='/takeout/create_task', data={'unixtime': '123456789'}, X_YA_SERVICE_TICKET='smth')
#         resp = get_results(req)
#         assert resp.status_code == 400

#     def test_code_valid_request(self):
#         # Give 200 if there is uid in request
#         req = self.factory.post(
#             path='/takeout/create_task',
#             data={'unixtime': '123456789', 'uid': '0'},
#             HTTP_X_YA_SERVICE_TICKET='smth'
#         )
#         resp = get_results(req)
#         assert resp.status_code == 200

#     # tvm header

#     def test_code_no_tvm_header(self):
#         # Give 400 if there is no tvm header
#         req = self.factory.post(
#             path='/takeout/create_task', data={'unixtime': '123456789', 'uid': '0'}
#         )
#         resp = get_results(req)
#         assert resp.status_code == 403

#     def test_code_not_valid_tvm_header(self):
#         # Give 400 if tvm header is not valid
#         req = self.factory.post(
#             path='/takeout/create_task', data={'unixtime': '123456789', 'uid': '0'},
#             HTTP_X_YA_SERVICE_TICKET=''
#         )
#         resp = get_results(req)
#         assert resp.status_code == 403

#     def test_response_has_tvm_ticket(self):
#         # TVM ticket must be included in response
#         req = self.factory.post(
#             path='/takeout/create_task',
#             data={'unixtime': '123456789', 'uid': '0'},
#             HTTP_X_YA_SERVICE_TICKET='smth'
#         )
#         resp = get_results(req)
#         assert resp.has_header('X-Ya-Service-Ticket')
##############
    # def test_get_tvm_ticket_returns_smth(self):
    #     # TVM ticket must be at least not empty
    #     ticket = get_tvm_ticket()
    #     assert ticket

    # response content

    # def test_no_such_user(self):
    #     # There is no such user in our database
    #     pass

def raise_error():
    raise Exception(text='Test')


def raise_user_does_not_exist():
    raise User.DoesNotExist(text='Test')


def raise_social_user_does_not_exist():
    raise UserSocialAuth.DoesNotExist(text='Test')


class UserForTest(object):  
        def __init__(self):
            self.user_id = 1
            self.email = 'test@load.ru'
            self.is_active = True


class JobForTest(object):  
        def __init__(self, n=1, fd='1970'):
            self.n = n
            self.user = 1
            self.fd = datetime.datetime.strptime(fd, '%Y')
            self.is_deleted = False


class TestTakeoutDeleteUserData(TestCase):

    def setUp(self):
        # Every test needs access to the request factory.
        self.factory = RequestFactory()
        self.test_user = UserForTest()
        self.test_job = JobForTest()
        self.request = HttpRequest()
        self.response = requests.models.Response()
        # To model different queries from takeout
        self.tvm_ticket = 'test'
        self.ticket_is_correct = True
        self.yandex_uid = 1

    def test_get_user_id_from_blackbox(self):
        with mock.patch('requests.get') as correct:
            self.response._content = json.dumps({'users': [{'id': '33', 'uid': {'value': '3'}}]})
            correct.return_value = self.response
            self.assertEqual('3', get_user_id_from_blackbox('user', 'bb'))

    def test_get_error_from_blackbox(self):
        with mock.patch('requests.get') as error:
            self.response._content = json.dumps({'error': 'test_error', 'users': [{'id': '33', 'uid': {'value': '3'}}]})
            error.return_value = self.response
            self.assertEqual(None, get_user_id_from_blackbox('user', 'bb'))

    def test_get_nouid_from_blackbox(self):
        with mock.patch('requests.get') as nouid:
            self.response._content = json.dumps({'users': [{'id': '33', 'nouid': {'value': '3'}}]})
            nouid.return_value = self.response
            self.assertEqual(None, get_user_id_from_blackbox('user', 'bb'))

    def test_get_exception_from_blackbox(self):
        with mock.patch('requests.get') as exception:
            self.response.status_code = 500
            exception.return_value = self.response
            self.assertEqual(None, get_user_id_from_blackbox('user', 'bb'))

    def test_response_status(self):
        response = HttpResponse(json.dumps({'status': 'ok', 'data': [{'id': 'user_data', 'slug': 'user personal data and own\'s jobs', 'state': 'empty'}]}), content_type='application/json')
        response['X-Ya-Service-Ticket'] = self.tvm_ticket
        self.assertEqual(response.content, send_response('status', 'empty', tvm_ticket=self.tvm_ticket).content)
        self.assertEqual(response.status_code, send_response('status', 'empty', tvm_ticket=self.tvm_ticket).status_code)

    def test_response_error(self):
        response = HttpResponse(json.dumps({'status': 'error', 'errors': [{'code': 'test', 'message': 'test message'}]}), content_type='application/json')
        response['X-Ya-Service-Ticket'] = self.tvm_ticket
        self.assertEqual(response.content, send_response('error', 'test', 'test message', tvm_ticket=self.tvm_ticket).content)
        self.assertEqual(response.status_code, send_response('error', 'test', 'test message', tvm_ticket=self.tvm_ticket).status_code)

    @mock.patch('takeout.takeout_tools.get_user_id_from_blackbox')
    @mock.patch.object(Ticketer, 'get_ticket')
    @mock.patch.object(Ticketer, 'check_ticket')
    @mock.patch.object(User.objects, 'raw')
    def get_user_status_content(self, raw, check_t, get_t, get_uid):
        
        raw.return_value = [self.test_user]
        s_user.return_value = self.test_user
        get_t.return_value = self.tvm_ticket
        check_t.return_value = self.ticket_is_correct
        get_uid.return_value = self.yandex_uid

        return get_user_status(self.request).content

    @mock.patch('takeout.takeout_tools.get_user_id_from_blackbox')
    @mock.patch.object(Ticketer, 'get_ticket')
    @mock.patch.object(Ticketer, 'check_ticket')
    @mock.patch.object(User.objects, 'raw')
    @mock.patch.object(User.objects, 'filter')
    @mock.patch.object(Job.objects, 'filter')
    def delete_user_data_content(self, jobs, users, raw, check_t, get_t, get_uid):
        
        jobs.return_value = [self.test_job]
        users.return_value = [self.test_user]
        raw.return_value = [self.test_user]
        get_t.return_value = self.tvm_ticket
        check_t.return_value = self.ticket_is_correct
        get_uid.return_value = self.yandex_uid

        return delete_user_data(self.request).content

    def test_get_user_status(self):
        self.request.GET = {'request_id': 123}
        self.request.META = {'HTTP_X_YA_SERVICE_TICKET': 'test_service_ticket', 'HTTP_X_YA_USER_TICKET': 'test_user_ticket'}
        status_content = self.get_user_status_content()
        self.assertEqual(status_content, send_response('status', 'ready_to_delete', tvm_ticket=self.tvm_ticket).content)

    def test_delete_user_data(self):
        self.request.GET = {'request_id': 123}
        self.request.META = {'HTTP_X_YA_SERVICE_TICKET': 'test_service_ticket', 'HTTP_X_YA_USER_TICKET': 'test_user_ticket'}
        self.request.POST = {'id': ['user_data']}
        delete_content = self.delete_user_data_content()
        self.assertEqual(delete_content, send_response('data_deleted', tvm_ticket=self.tvm_ticket).content)

    def test_delete_user_data_unknown_type(self):
        self.request.GET = {'request_id': 123}
        self.request.META = {'HTTP_X_YA_SERVICE_TICKET': 'test_service_ticket', 'HTTP_X_YA_USER_TICKET': 'test_user_ticket'}
        self.request.POST = {'id': 'user_data'}
        delete_content = self.delete_user_data_content()
        self.assertEqual(delete_content, send_response(message='Unknow user data type', tvm_ticket=self.tvm_ticket).content)

    def test_get_user_status_not_active(self):
        self.request.GET = {'request_id': 123}
        self.request.META = {'HTTP_X_YA_SERVICE_TICKET': 'test_service_ticket', 'HTTP_X_YA_USER_TICKET': 'test_user_ticket'}
        self.test_user.is_active = False
        content = self.get_user_status_content()
        self.assertEqual(content, send_response('status', 'delete_in_progress', tvm_ticket=self.tvm_ticket).content)

    def test_get_user_status_no_request_id(self):
        status_content = self.get_user_status_content()
        delete_content = self.delete_user_data_content()
        self.assertEqual(status_content, send_response(message='No request_id in request', tvm_ticket=self.tvm_ticket).content)
        self.assertEqual(delete_content, send_response(message='No request_id in request', tvm_ticket=self.tvm_ticket).content)

    def test_get_user_status_no_user_ticket(self):
        self.request.GET = {'request_id': 123}
        status_content = self.get_user_status_content()
        delete_content = self.delete_user_data_content()
        self.assertEqual(status_content, send_response(message='No tvm user ticket', tvm_ticket=self.tvm_ticket).content)
        self.assertEqual(delete_content, send_response(message='No tvm user ticket', tvm_ticket=self.tvm_ticket).content)

    def test_get_user_status_wrong_service_ticket(self):
        self.request.GET = {'request_id': 123}
        self.request.META = {'HTTP_X_YA_SERVICE_TICKET': 'test_service_ticket', 'HTTP_X_YA_USER_TICKET': 'test_user_ticket'}
        self.ticket_is_correct = False
        status_content = self.get_user_status_content()
        delete_content = self.delete_user_data_content()
        self.assertEqual(status_content, send_response(message='No valid tvm service ticket found', tvm_ticket=self.tvm_ticket).content)
        self.assertEqual(delete_content, send_response(message='No valid tvm service ticket found', tvm_ticket=self.tvm_ticket).content)
    
    def test_get_user_status_no_yandex_uid(self):
        self.request.GET = {'request_id': 123}
        self.request.META = {'HTTP_X_YA_SERVICE_TICKET': 'test_service_ticket', 'HTTP_X_YA_USER_TICKET': 'test_user_ticket'}
        self.yandex_uid = ''
        status_content = self.get_user_status_content()
        delete_content = self.delete_user_data_content()
        self.assertEqual(status_content, send_response('error', 'Internal Error', 'Failed to get yandex_uid', tvm_ticket=self.tvm_ticket).content)
        self.assertEqual(delete_content, send_response('error', 'Internal Error', 'Failed to get yandex_uid', tvm_ticket=self.tvm_ticket).content)

    @mock.patch('takeout.takeout_tools.get_user_id_from_blackbox')
    @mock.patch.object(Ticketer, 'check_ticket')
    @mock.patch.object(User.objects, 'raw')
    def test_get_user_status_internal_error(self, raw, check_t, get_uid):
        raw.return_value = [self.test_user]
        check_t.return_value = self.ticket_is_correct
        get_uid.return_value = self.yandex_uid
        with mock.patch.object(Ticketer, 'get_ticket', new=raise_error):
            self.assertEqual(get_user_status(self.request).content, send_response('error', 'Internal Error', 'Failed to get TVM service tickets', tvm_ticket=self.tvm_ticket).content)
            self.assertEqual(delete_user_data(self.request).content, send_response('error', 'Internal Error', 'Failed to get TVM service tickets', tvm_ticket=self.tvm_ticket).content)
 
    @mock.patch('takeout.takeout_tools.get_user_id_from_blackbox')
    @mock.patch.object(Ticketer, 'get_ticket')
    @mock.patch.object(Ticketer, 'check_ticket')
    @mock.patch.object(User.objects, 'raw')
    def test_get_user_status_user_absent(self, raw, check_t, get_t, get_uid):
        raw.return_value = []
        check_t.return_value = self.ticket_is_correct
        get_t.return_value = self.tvm_ticket
        get_uid.return_value = self.yandex_uid
        self.request.GET = {'request_id': 123}
        self.request.META = {'HTTP_X_YA_SERVICE_TICKET': 'test_service_ticket', 'HTTP_X_YA_USER_TICKET': 'test_user_ticket'}
        with mock.patch.object(UserSocialAuth.objects, 'get', new=raise_social_user_does_not_exist):
            self.assertEqual(get_user_status(self.request).content, send_response('status', 'empty', tvm_ticket=self.tvm_ticket).content)


class TestTakeoutGetResults(TestCase):
    def setUp(self):
        # Every test needs access to the request factory.
        self.factory = RequestFactory()
        self.request = HttpRequest()
        # To model different queries from takeout
        self.tvm_ticket = 'test'
        self.ticket_is_correct = True
        self.expected_data = ['1;1971-01-01 00:00:00;https://overload.yandex.net/1', '2;1972-01-01 00:00:00;https://overload.yandex.net/2']
        self.expected_csv = 'test_id;start_date;test_link\n1;1971-01-01 00:00:00;https://overload.yandex.net/1\n2;1972-01-01 00:00:00;https://overload.yandex.net/2'

    @mock.patch.object(Job.objects, 'raw')
    def test_collect_data(self, raw_job):
        raw_job.return_value = [JobForTest(1, '1971'), JobForTest(2, '1972')]
        self.assertEqual(collect_data(1), self.expected_data)

    @mock.patch.object(Ticketer, 'check_ticket')
    @mock.patch.object(Ticketer, 'get_ticket')
    @mock.patch('takeout.takeout_tools.collect_data')
    def get_results_content(self, c_data, get_t, check_t):
        check_t.return_value = self.ticket_is_correct
        get_t.return_value = self.tvm_ticket
        c_data.return_value = self.expected_data
        return get_results(self.request).content

    def test_get_results_correct(self):
        self.request.META['YA_SERVICE_TICKET'] = self.tvm_ticket
        self.request.POST['uid'] = 1
        response = HttpResponse(
            json.dumps({'status': 'ok', 'data': {'overload.csv': self.expected_csv}}),
            content_type='application/json'
        )
        response['X_Ya_Service_Ticket'] = self.tvm_ticket
        self.assertEqual(self.get_results_content(), response.content)

    def test_get_results_no_data(self):
        self.request.META['YA_SERVICE_TICKET'] = self.tvm_ticket
        self.request.POST['uid'] = 1
        self.expected_data = []
        response = HttpResponse(
            json.dumps({"status": "no_data"}),
            content_type='application/json'
        )
        response['X_Ya_Service_Ticket'] = self.tvm_ticket
        self.assertEqual(self.get_results_content(), response.content)


    def test_get_results_no_uid(self):
        self.request.META['YA_SERVICE_TICKET'] = self.tvm_ticket
        response = HttpResponse(
            json.dumps({'status': 'error', 'message': 'no uid is given'}),
            content_type='application/json'
        )
        response['X_Ya_Service_Ticket'] = self.tvm_ticket
        self.assertEqual(self.get_results_content(), response.content)

    def test_get_results_invalid_tvm_ticket(self):
        self.request.META['YA_SERVICE_TICKET'] = self.tvm_ticket
        self.ticket_is_correct = False
        response = HttpResponse(
            json.dumps({'status': 'error', 'message': 'no valid tvm ticket found'}),
            content_type='application/json'
        )
        response['X_Ya_Service_Ticket'] = self.tvm_ticket
        self.assertEqual(self.get_results_content(), response.content)
