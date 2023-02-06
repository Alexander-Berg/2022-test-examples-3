# -*- coding: utf-8 -*-
import base64
import json
import os
import ujson
import urllib
import urllib2
import uuid
from httplib import OK

import flask
import mock
import urlparse
import collections
import datetime
import random
import requests
import string
import time

from contextlib import contextmanager
from itertools import cycle
from lxml import etree
from requests.models import Response, Request

from mpfs.common.errors import APIError
from mpfs.common.errors import TelemostApiNotFound
from mpfs.common.errors import TelemostApiForbidden
from mpfs.common.errors import TelemostApiBadRequest
from mpfs.common.errors import TelemostApiGone
from mpfs.common.util import from_json, to_json
from mpfs.config import settings
from mpfs.core.services.hbf_service import HbfService
from test.fixtures.services import TCMData
from test.fixtures.spam_checker_data import SpamCheckerData
from test.helpers.mediabilling_responses_data import IntervalsData
from test.helpers.stubs import base
from test.helpers.stubs.resources import (
    users_info,
    abook_info,
)
from test.fixtures.users import default_user

from mpfs.core.services.previewer_service import RegeneratePreviewResult
from mpfs.core.services.djfs_api_service import djfs_api, MPFS_ERROR_CODE_HEADER_NAME
from mpfs.core.services.staff_service import StaffUserRecord
from mpfs.core.services.tvm_service import TVMTicket
from mpfs.core.services.push_service import XivaSubscription, XivaNotificationStatus
from mpfs.core.services.data_api_service import DataApiService
from test.helpers.telemost_api_responses_data import CreatedConfData
from test.fixtures.passport import passport_responses
from test.helpers.utils import construct_requests_resp


class PushServicesStub(base.ChainedPatchBaseStub):
    """
    Заглушка для походов в Xiva(пуши)
    """
    deprecated_send = mock.patch(
        'mpfs.core.services.push_service.PushSender.send',
        return_value=200
    )
    send = mock.patch(
        'mpfs.core.services.push_service.XivaSendService.send',
        return_value=None
    )
    # Заглушки mpfs-адаптера к XivaSubscribeCommonService. Лучше не использовать
    subscribe_url = mock.patch(
        'mpfs.core.services.push_service.XivaSubscribeService.subscribe_url',
        return_value=None
    )
    subscribe_app = mock.patch(
        'mpfs.core.services.push_service.XivaSubscribeService.subscribe_app',
        return_value=None
    )
    unsubscribe_app = mock.patch(
        'mpfs.core.services.push_service.XivaSubscribeService.unsubscribe_app',
        return_value=None
    )
    # Заглушки походов в xiva. Используем их
    subscribe_url_common = mock.patch(
        'mpfs.core.services.push_service.XivaSubscribeCommonService.subscribe_url',
        return_value=None
    )
    subscribe_app_common = mock.patch(
        'mpfs.core.services.push_service.XivaSubscribeCommonService.subscribe_app',
        return_value=None
    )
    unsubscribe_app_common = mock.patch(
        'mpfs.core.services.push_service.XivaSubscribeCommonService.unsubscribe_app',
        return_value=None
    )
    subscriptions_list = mock.patch(
        'mpfs.core.services.push_service.XivaSubscribeCommonService.subscriptions_list',
        return_value=[XivaSubscription(uid=default_user.uid, uuid=None, xiva_id=u'392787fac9d68742ade5f075abe5ff687dcf386f'),
                      XivaSubscription(uid=default_user.uid, uuid=u'dfbde02632ed4s5a9h9fd1r497e64001', xiva_id=u'392787fac9d68742ade5f075abe5ff687dcf386c'),
                      XivaSubscription(uid=default_user.uid, uuid=u'dfbde02632ed4s5a9h9fd1r497e64002', xiva_id=u'mob:3b2c2149f3188e8dc3b7ad7a6764f356'),
                      XivaSubscription(uid=default_user.uid, uuid=u'dfbde02632ed4s5a9h9fd1r497e64004', xiva_id=u'mob:b351a4646dd78e3e1e475c255516619c'),
                      XivaSubscription(uid=default_user.uid, uuid=u'dfbde02632ed4s5a9h9fd1r497e64005', xiva_id=u'mob:e12ce883a75e4ca6b86aae00e7d2bd1b'),
                      XivaSubscription(uid=default_user.uid, uuid=None, xiva_id=u'mob:f20e1c0cd36ea31432e66d340b20b16c'),
                      XivaSubscription(uid=default_user.uid, uuid=u'dfbde02632ed4s5a9h9fd1r497e64006', xiva_id=u'mob:f20e1c0cd36ea31432e66d880b20b16c'),

        ]
    )
    batch_send = mock.patch(
        'mpfs.core.services.push_service.XivaSendService.batch_send',
        return_value=[XivaNotificationStatus(code=200, body='OK')]
    )

    @staticmethod
    def parse_send_call(mock_call):
        """
        Приводит объект вызова(call) метода отправки пушей в удобочитаемый словарь.

        Передавать вызов метода `mpfs.core.services.push_service.XivaSendService.send`
        https://docs.python.org/3/library/unittest.mock.html#call
        """
        result = {}
        args, kwargs = mock_call
        result['uid'] = args[0]
        result['event_name'] = args[1]
        result['raw_payload'] = args[2]
        try:
            result['json_payload'] = ujson.loads(args[2])
        except Exception:
            result['json_payload'] = None
        result['keys'] = kwargs.get('keys')
        result['connection_id'] = kwargs.get('connection_id')
        return result


class RateLimiterStub(base.ChainedPatchBaseStub):
    """
    Отключает RateLimiter

    Запросы всегда не лимитированы
    """
    rate_limiter_service_is_limit_exceeded = mock.patch(
        'mpfs.core.services.rate_limiter_service.RateLimiterService.is_limit_exceeded',
        return_value=False
    )


class ClckStub(base.ChainedPatchBaseStub):
    """
    Заглушка сокращателя урлов

    Имитирует поведение кликера
    """
    SHORT_URL_PATTERN = "http://dummy.ya.net/%s"

    def __init__(self, exclude_patches=None):
        self._data = {}
        self.generate = mock.patch(
            'mpfs.core.services.clck_service.Clck.generate',
            new=self._generate
        )
        self.short_url_to_full_url = mock.patch(
            'mpfs.core.services.clck_service.Clck.short_url_to_full_url',
            new=self._short_url_to_full_url
        )
        super(ClckStub, self).__init__(exclude_patches=exclude_patches)

    def _generate(self, url, mode=''):
        if mode == 'album':
            _id = 'a/' + str(uuid.uuid4())
        else:
            _id = 'd/' + str(uuid.uuid4())

        short_url = self.SHORT_URL_PATTERN % _id
        self._data[short_url] = url
        return _id, short_url

    def _short_url_to_full_url(self, short_url):
        short_url = short_url.split("?", 1)[0].split("#", 1)[0].rstrip('/')
        return self._data.get(short_url, '')


class TelemostApiRawResponseStub(object):
    def __init__(self, status=200, content=CreatedConfData.DEFAULT):
        self.patch_requests = mock.patch('requests.Session.send',
                                         return_value=self.construct_requests_resp(status=status, content=content))

    def construct_requests_resp(self, status=200, content='',
                                request_method='GET', request_url='https://telemost.dst.yandex.ru/v2/stub',
                                request_params=None, request_data=None, request_headers=None, request_body=None):
        request = Request(request_method, request_url,
                          params=request_params, data=request_data, headers=request_headers)
        request.body = request_body

        resp = Response()
        resp.status_code = status
        resp.request = request
        if isinstance(content, dict):
            content = to_json(content)
        resp._content = content

        return resp

    def __enter__(self):
        self.patch_requests.start()
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.patch_requests.stop()


class TCMStub(base.ChainedPatchBaseStub):
    def __init__(self, return_content=TCMData.DEFAULT, return_status=OK, side_effect=None, exclude_patches=None):
        return_value = construct_requests_resp(content=return_content,
                                               request_url='https://tcm.dst.yandex.net/api',
                                               status=return_status)
        if side_effect:
            self.patch_requests = mock.patch(
                'mpfs.core.services.telemost_conference_manager_service.TelemostConferenceManagerService.request',
                side_effect=side_effect
            )
        else:
            self.patch_requests = mock.patch(
                'mpfs.core.services.telemost_conference_manager_service.TelemostConferenceManagerService.request',
                return_value=return_value
            )
        super(TCMStub, self).__init__(exclude_patches=exclude_patches)


class TelemostApiStub(base.ChainedPatchBaseStub):

    def __init__(self, exclude_patches=None, create_response=None, get_response=None, raise_exception=False,
                 exception_to_raise=None):
        self._data = {}
        self.generate = mock.patch(
            'mpfs.core.services.telemost_api_service.TelemostApiService.create_conference',
            new=self._create_conference
        )
        self.short_url_to_full_url = mock.patch(
            'mpfs.core.services.telemost_api_service.TelemostApiService.get_conference',
            new=self._get_conference
        )
        super(TelemostApiStub, self).__init__(exclude_patches=exclude_patches)
        self.create_response = create_response
        self.get_response = get_response
        self.raise_exception = raise_exception
        self.exception_to_raise = exception_to_raise

    def _create_conference(self, uid=None, staff_only=False, is_permanent=False, external_meeting=None,
                           calendar_event_id=None):
        return self.create_response

    def _get_conference(self, conference_uri, uid=None):
        if self.raise_exception:
            raise self.exception_to_raise
        return self.get_response


class SendEmailStub(base.ChainedPatchBaseStub):
    """
    Отключает отправку писем
    """
    smtp_sendmail = mock.patch(
        'smtplib.SMTP'
    )
    mailer_sendmail = mock.patch(
        'mpfs.common.util.mailer.sendmail'
    )


class BigBillingStub(base.ChainedPatchBaseStub):
    """
    Заглушка для большого биллинга
    """
    TARGET = 'mpfs.core.services.billing_service.BillingServiceRouter'

    unsubscribe = mock.patch('%s.unsubscribe' % TARGET, return_value=None)
    order_place = mock.patch('%s.order_place' % TARGET, return_value=({
        'status': 'success',
        'status_code': 'order_created',
        'status_desc': 'order has been created',
        'service_order_id': '1765839281',
        'trust_payment_id': '56ab50c5795be27c462cb98d',
        'paymethod_id': 'trust_web_page'
    }, None))
    payment_make = mock.patch(
        '%s.payment_make' % TARGET,
        return_value={'url': 'http://fake-url.yandex.net', 'params': None}
    )
    list_payment_methods = mock.patch('%s.list_payment_methods' % TARGET, return_value={
        'status': 'success',
        'plates': [],
        'payment_methods': {}
    })

    # TODO. Mock this methods
    # create_product(self, product):


class MediaBillingStub(base.ChainedPatchBaseStub):
    """
    Заглушка для mediabilling'а
    """
    def __init__(self, status=200, content=IntervalsData.DEFAULT):
        #TODO(kis8ya): подумать над роутещей заглушкой:
        # если запрос в определенную ручку - отдавать соответствующий ответ
        # тогда можно врубить загрушку везде без финтов
        self.patch_requests = mock.patch('requests.Session.send',
                                         return_value=self.construct_requests_resp(status=status, content=content))

    def construct_requests_resp(self, status=200, content='',
                                request_method='GET', request_url='https://telemost.dst.yandex.ru/v2/stub',
                                request_params=None, request_data=None, request_headers=None, request_body=None):
        request = Request('GET', '')
        request.body = None

        resp = Response()
        resp.status_code = status
        resp.request = request
        if isinstance(content, dict):
            content = to_json(content)
        resp._content = content

        return resp

    def __enter__(self):
        self.patch_requests.start()
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.patch_requests.stop()


class SearchIndexerStub(base.ChainedPatchBaseStub):
    """
    Заглушка для сервиса индексации
    """
    push_change = mock.patch(
        'mpfs.core.services.index_service.SearchIndexer.push_change',
        return_value=None
    )
    get_file_body = mock.patch(
        'mpfs.core.services.index_service.SearchIndexer.get_file_body',
        return_value=None
    )
    start_reindex_for_quick_move = mock.patch(
        'mpfs.core.services.index_service.SearchIndexer.start_reindex_for_quick_move',
        return_value=None
    )


class DiskSearchStub(base.ChainedPatchBaseStub):
    """
    Заглушка для сервиса поиска по диску
    """
    open_url = mock.patch(
        'mpfs.core.services.search_service.DiskSearch.open_url',
        return_value=None
    )


class SearchDBStub(base.ChainedPatchBaseStub):
    """
    Заглушка для Поиска
    """
    folder_size = mock.patch('mpfs.core.services.search_service.SearchDB.folder_size', return_value=None)
    resources_info_by_file_ids = mock.patch(
        'mpfs.core.services.search_service.SearchDB.resources_info_by_file_ids',
        return_value=None
    )


class SearchDBTinyStub(base.ChainedPatchBaseStub):
    """
    Заглушка для Поиска, мокающая только open_url
    """
    open_url = mock.patch('mpfs.core.services.search_service.SearchDB.open_url', return_value='{}')


class KladunStub(base.ChainedPatchBaseStub):
    """
    Заглушка для Кладуна (aka Uploader)
    """
    kladun_open_url = mock.patch('mpfs.core.services.kladun_service.Kladun.open_url', return_value=None)
    kladun_extract_exif = mock.patch('mpfs.core.services.kladun_service.Kladun.extract_exif', return_value=(None, {}))
    upload_to_disk_post_request = mock.patch('mpfs.core.services.kladun_service.UploadToDisk.post_request',
                                             return_value=('http://localhost/echo?post_target=1',
                                                           'http://localhost/echo?poll_result=1'))
    patch_post_request = mock.patch('mpfs.core.services.kladun_service.Patch.post_request',
                                    return_value=('http://localhost/echo?patch_target=1',
                                                  'http://localhost/echo?request_status=1'))
    extract_file_post_request = mock.patch('mpfs.core.services.kladun_service.ExtractFileFromArchive.post_request',
                                           return_value=('http://localhost/echo?post_target=1',
                                                         'http://localhost/echo?poll_result=1'))
    capture_post_request = mock.patch('mpfs.core.services.kladun_service.Capture.post_request',
                                      return_value='http://localhost/echo?poll_result=1')
    office_post_request = mock.patch('mpfs.core.services.kladun_service.OfficeConvertAndUpload.post_request',
                                     return_value='http://localhost/echo?poll_result=1')
    check_service_is_alive = mock.patch('mpfs.core.services.kladun_service.Kladun.check_service_is_alive', return_value=True)

    def __init__(self, status_value_paths=None, status_values=None, checksums_obj=None):
        """Нужно передавать либо `status_value_paths`, либо `status_values`.

        :param status_value_paths: пути до файлов (ответов Кладуна о статусе загрузки)
        :param status_values: ответы Кладуна о статусе загрузки
        :type status_value_paths: collections.Iterable[str]
        :type status_values: collections.Iterable[lxml.etree.ElementTree]
        """
        super(KladunStub, self).__init__()
        if (status_value_paths is None and
            status_values is None):
            status_value_paths = ('fixtures/xml/kladun_store_1.xml',
                                  'fixtures/xml/kladun_store_2.xml',
                                  'fixtures/xml/kladun_store_3.xml')

        if status_value_paths:
            status_results = []
            for status_file_path in status_value_paths:
                with open(status_file_path) as status_file:
                    status_value = etree.fromstring(status_file.read())
                    status_results.append(status_value)
            self._status_results = cycle(status_results)
        else:
            self._status_results = cycle(status_values)

        self.status = mock.patch('mpfs.core.services.kladun_service.Kladun.status',
                                 new=self._status)
        if checksums_obj:
            if isinstance(checksums_obj, Exception):
                self.get_file_checksums = mock.patch('mpfs.core.services.kladun_service.Kladun.get_file_checksums',
                                                     side_effect=checksums_obj)
            else:
                self.get_file_checksums = mock.patch('mpfs.core.services.kladun_service.Kladun.get_file_checksums',
                                                     return_value=checksums_obj)

    def _status(self, *args, **kwargs):
        return next(self._status_results)


class PreviewerStub(base.ChainedPatchBaseStub):
    """
    Заглушка для Превьювера (ex. Uploader)
    """
    def _generate_album_preview(*args, **kwargs):
        return ''.join([random.choice(string.hexdigits) for _ in xrange(64)])

    previewer_open_url = mock.patch('mpfs.core.services.previewer_service.Previewer.open_url', return_value=None)
    generate_album_preview = mock.patch('mpfs.core.services.previewer_service.Previewer.generate_album_preview',
                                        side_effect=_generate_album_preview)
    regenerate_digest = mock.patch('mpfs.core.services.previewer_service.Previewer.regenerate_digest',
                                   return_value='123456.yadisk:654321.654321')
    check_service_is_alive = mock.patch('mpfs.core.services.previewer_service.Previewer.check_service_is_alive', return_value=True)

    def __init__(self, checksums_obj=None, album_preview_stid=None, regenerate_preview_result=None):
        super(PreviewerStub, self).__init__()
        if checksums_obj:
            self.get_file_checksums = mock.patch('mpfs.core.services.previewer_service.Previewer.get_file_checksums',
                                                 return_value=checksums_obj)
        if album_preview_stid is not None:
            self.generate_album_preview = mock.patch(
                'mpfs.core.services.previewer_service.Previewer.generate_album_preview',
                return_value=album_preview_stid
            )

        if regenerate_preview_result:
            self.previewer_regenerate_preview = mock.patch(
                'mpfs.core.services.previewer_service.Previewer.regenerate_preview',
                return_value=regenerate_preview_result
            )
        else:
            self.previewer_regenerate_preview = mock.patch(
                'mpfs.core.services.previewer_service.Previewer.regenerate_preview',
                return_value=RegeneratePreviewResult('123456.yadisk:123456.123456')
            )


class PassportStub(base.ChainedPatchBaseStub):
    """
    Заглушка для сервиса Паспорт
    """
    subscribe = mock.patch('mpfs.core.services.passport_service.Passport.subscribe',
                           return_value=True)
    unsubscribe = mock.patch('mpfs.core.services.passport_service.Passport.unsubscribe',
                             return_value=True)

    def __init__(self, userinfo=None):
        super(PassportStub, self).__init__()

        if userinfo:
            self.userinfo = mock.patch('mpfs.core.services.passport_service.Passport.userinfo',
                                       return_value=userinfo)
        else:
            self.userinfo = mock.patch('mpfs.core.services.passport_service.Passport.userinfo',
                                       new=self._user_info)

        self.is_from_pdd = mock.patch('mpfs.core.services.passport_service.Passport.is_from_pdd',
                                      new=self._is_from_pdd)
        self.bulk_userinfo = mock.patch('mpfs.core.services.passport_service.Passport.bulk_userinfo',
                                        new=self._bulk_userinfo)
        self.get_hosted_domains = mock.patch('mpfs.core.services.passport_service.Passport.get_hosted_domains',
                                             new=self._get_hosted_domains)

    def _user_info(self, uid=None, login=None, *args, **kwargs):
        if uid:
            user_info = users_info.get_info_by_uid(uid)
        elif login:
            user_info = users_info.get_info_by_login(login)
        else:
            raise ValueError('uid or login should be provided')
        return user_info

    def _is_from_pdd(self, address=None, uid=None):
        user_info = self._user_info(uid=uid, login=address)
        return user_info['pdd']

    def _bulk_userinfo(self, uids, load_all_emails=False, add_private_data=False):
        result = [self._user_info(uid=uid)
                  for uid in uids]
        return result

    def _get_hosted_domains(self, domain):
        """Вернуть ответ метода ``Passport::get_hosted_domains``.

        Настоящий ответ означает что по указанному домену ``domain``
        не было найдено ни одной записи о доменах в базе Паспорта.
        """
        return []

    @staticmethod
    def reset_users_info():
        users_info.reset_users_info()

    @staticmethod
    def update_info_by_uid(uid, has_staff=None, is_2fa_enabled=None, yateam_uid=None, yateam_login=None, login=None,
                           language=None, sso_user=None, domain_id=None):
        users_info.update_info_by_uid(uid, has_staff=has_staff, is_2fa_enabled=is_2fa_enabled, yateam_uid=yateam_uid,
                                      yateam_login=yateam_login, login=login, language=language, sso_user=sso_user,
                                      domain_id=domain_id)


class PassportResponseMock(base.ChainedPatchBaseStub):
    """
    Заглушка для ответов внешнего сервиса Паспорт
    В отличие от PassportStub мокает именно ответы сервиса, а не passport_service.py
    """
    def __init__(self, response_file=None, render_response_func=None):
        super(PassportResponseMock, self).__init__()
        self.render_response_func = render_response_func

        if response_file:
            self.request = mock.patch('mpfs.core.services.passport_service.Passport.request',
                                      return_value=self._process_response_from_file(response_file))
        else:
            self.request = mock.patch('mpfs.core.services.passport_service.Passport.request',
                                      new=self._passport_response)

    def _process_response_from_file(self, response_file):
        template = passport_responses.get_response_template_from_file(response_file)
        return self._render_response(template)

    def _passport_response(self, method, relative_url, params=None, *args, **kwargs):
        uid = params['uid'] if params is not None else None

        if uid:
            template = passport_responses.get_response_by_uid(uid)
            return self._render_response(template)
        else:
            raise ValueError('uid or login should be provided')

    def _render_response(self, template):
        if self.render_response_func is not None:
            response = self.render_response_func(template)
        else:
            response = template.render()
        return PassportResponseMock.Response(response)

    class Response(object):
        def __init__(self, content):
            self.content = content


class TVMStub(base.ChainedPatchBaseStub):
    """
    Заглушка для сервиса TVM
    """

    class FlaskGMock(object):
        def __init__(self):
            self.tvm_ticket = None

        def get(self, item):
            return self.__dict__[item]

    flask = mock.patch.object(flask, 'g', new_callable=FlaskGMock)

    def __init__(self):
        super(TVMStub, self).__init__()
        self.new_tvm_ticket = TVMTicket.build_tvm_ticket('new_ticket', is_external=False)
        self.get_new_ticket = mock.patch('mpfs.core.services.tvm_service.tvm.get_new_ticket',
                                         return_value=self.new_tvm_ticket)


class DirectoryServiceStub(base.ChainedPatchBaseStub):
    notify_user_activate_invite = mock.patch(
        'mpfs.core.services.directory_service.DirectoryService.notify_user_activate_invite',
        return_value=None
    )

    get_organizations_by_uids = mock.patch(
        'mpfs.core.services.directory_service.DirectoryService.get_organizations_by_uids',
        return_value=mock.Mock(get=mock.Mock(return_value=[{'id': 12345678}]))
    )


class VideoStreamingStub(base.ChainedPatchBaseStub):
    _video_info_common_response = ujson.loads(
        '''
        {
        "stream_id": "AHFKw-2KwOENaygEvl5DqTFxIfDMVkihkbGsHn4CSEcAuKdIbelp-CsI_tAFoLYe4v_m7ejnbFjtSzNwdn-IPayrykxS7amILHSSYrPJfKiMlWW7ZgMRqOxxmNz0hvCfqC6bJpRgIF-O_bLxVKb2WrtEtjN3ZXtR7iaCbGHVGkRlVq2NM-4S0Wpixwkjmz3yDjumYDwaQNI7JHJdS8GY3YMRyVaLN5kSHoyPz63ha5rz5ENf1nXfHuoWoaLx76z3d3ll35rvciChl6vvkc6sGpQKDOI_mAp9BIzPd-c3djcDjZKDNTZWmg0",
        "total": 5,
        "items": [
            {
            "resolution": "480p",
            "video_codec": "H.264",
            "audio_codec": "AAC",
            "width": 270,
            "height": 480,
            "links": {
                "https": "https://video.disk.yandex.net/hls-playlist/AHFKw-2KwOENaygEvl5DqTFxIfDMVkihkbGsHn4CSEcAuKdIbelp-CsI_tAFoLYe4v_m7ejnbFjtSzNwdn-IPayrykxS7amILHSSYrPJfKiMlWW7ZgMRqOxxmNz0hvCfqC6bJpRgIF-O_bLxVKb2WrtEtjN3ZXtR7iaCbGHVGkRlVq2NM-4S0Wpixwkjmz3yDjumYDwaQNI7JHJdS8GY3YMRyVaLN5kSHoyPz63ha5rz5ENf1nXfHuoWoaLx76z3d3ll35rvciChl6vvkc6sGpQKDOI_mAp9BIzPd-c3djcDjZKDNTZWmg0/playlist-redirect.m3u8?dimension=480p"
            },
            "container": "hls"
            },
            {
            "resolution": "360p",
            "video_codec": "H.264",
            "audio_codec": "AAC",
            "width": 204,
            "height": 360,
            "links": {
                "https": "https://video.disk.yandex.net/hls-playlist/AHFKw-2KwOENaygEvl5DqTFxIfDMVkihkbGsHn4CSEcAuKdIbelp-CsI_tAFoLYe4v_m7ejnbFjtSzNwdn-IPayrykxS7amILHSSYrPJfKiMlWW7ZgMRqOxxmNz0hvCfqC6bJpRgIF-O_bLxVKb2WrtEtjN3ZXtR7iaCbGHVGkRlVq2NM-4S0Wpixwkjmz3yDjumYDwaQNI7JHJdS8GY3YMRyVaLN5kSHoyPz63ha5rz5ENf1nXfHuoWoaLx76z3d3ll35rvciChl6vvkc6sGpQKDOI_mAp9BIzPd-c3djcDjZKDNTZWmg0/playlist-redirect.m3u8?dimension=360p"
            },
            "container": "hls"
            },
            {
            "resolution": "240p",
            "video_codec": "H.264",
            "audio_codec": "AAC",
            "width": 136,
            "height": 240,
            "links": {
                "https": "https://video.disk.yandex.net/hls-playlist/AHFKw-2KwOENaygEvl5DqTFxIfDMVkihkbGsHn4CSEcAuKdIbelp-CsI_tAFoLYe4v_m7ejnbFjtSzNwdn-IPayrykxS7amILHSSYrPJfKiMlWW7ZgMRqOxxmNz0hvCfqC6bJpRgIF-O_bLxVKb2WrtEtjN3ZXtR7iaCbGHVGkRlVq2NM-4S0Wpixwkjmz3yDjumYDwaQNI7JHJdS8GY3YMRyVaLN5kSHoyPz63ha5rz5ENf1nXfHuoWoaLx76z3d3ll35rvciChl6vvkc6sGpQKDOI_mAp9BIzPd-c3djcDjZKDNTZWmg0/playlist-redirect.m3u8?dimension=240p"
            },
            "container": "hls"
            },
            {
            "resolution": "720p",
            "video_codec": "H.264",
            "audio_codec": "AAC",
            "width": 406,
            "height": 720,
            "links": {
                "https": "https://video.disk.yandex.net/hls-playlist/AHFKw-2KwOENaygEvl5DqTFxIfDMVkihkbGsHn4CSEcAuKdIbelp-CsI_tAFoLYe4v_m7ejnbFjtSzNwdn-IPayrykxS7amILHSSYrPJfKiMlWW7ZgMRqOxxmNz0hvCfqC6bJpRgIF-O_bLxVKb2WrtEtjN3ZXtR7iaCbGHVGkRlVq2NM-4S0Wpixwkjmz3yDjumYDwaQNI7JHJdS8GY3YMRyVaLN5kSHoyPz63ha5rz5ENf1nXfHuoWoaLx76z3d3ll35rvciChl6vvkc6sGpQKDOI_mAp9BIzPd-c3djcDjZKDNTZWmg0/playlist-redirect.m3u8?dimension=720p"
            },
            "container": "hls"
            },
            {
            "links": {
                "https": "https://video.disk.yandex.net/hls-playlist/AHFKw-2KwOENaygEvl5DqTFxIfDMVkihkbGsHn4CSEcAuKdIbelp-CsI_tAFoLYe4v_m7ejnbFjtSzNwdn-IPayrykxS7amILHSSYrPJfKiMlWW7ZgMRqOxxmNz0hvCfqC6bJpRgIF-O_bLxVKb2WrtEtjN3ZXtR7iaCbGHVGkRlVq2NM-4S0Wpixwkjmz3yDjumYDwaQNI7JHJdS8GY3YMRyVaLN5kSHoyPz63ha5rz5ENf1nXfHuoWoaLx76z3d3ll35rvciChl6vvkc6sGpQKDOI_mAp9BIzPd-c3djcDjZKDNTZWmg0/master-playlist.m3u8"
            },
            "audio_codec": "AAC",
            "container": "hls",
            "resolution": "adaptive",
            "video_codec": "H.264"
            }
        ],
        "duration": 8770
        }
        '''

    )

    get_video_info = mock.patch(
        'mpfs.core.services.video_service.VideoStreaming._get_video_info',
        return_value=_video_info_common_response
    )


class BazingaInterfaceStub(base.ChainedPatchBaseStub):
    bulk_create_tasks = mock.patch(
        'mpfs.core.services.bazinga_service.BazingaInterface._bulk_create_tasks',
        side_effect=lambda tasks: range(len(tasks))
    )
    get_tasks_count = None

    def __init__(self, exclude_patches=None, tasks_count=0):
        super(BazingaInterfaceStub, self).__init__(exclude_patches=exclude_patches)
        self.get_tasks_count = mock.patch(
            'mpfs.core.services.bazinga_service.BazingaInterface.get_tasks_count',
            return_value=tasks_count
        )


class QuellerStub(base.ChainedPatchBaseStub):
    def __init__(self, exclude_patches=None, tasks_count=0):
        super(QuellerStub, self).__init__(exclude_patches=exclude_patches)
        self.get_tasks_count = mock.patch(
            'mpfs.core.services.queller_service.QuellerService.get_tasks_count',
            return_value=tasks_count
        )


class MulcaServiceStub(base.ChainedPatchBaseStub):
    _fake_size = 777

    remove = mock.patch(
        'mpfs.core.services.mulca_service.Mulca.remove',
        return_value=204
    )
    get_file_size = mock.patch(
        'mpfs.core.services.mulca_service.Mulca.get_file_size',
        return_value=_fake_size
    )

    def __init__(self, exclude_patches=None, is_file_exist=True):
        super(MulcaServiceStub, self).__init__(exclude_patches=exclude_patches)
        self.is_file_exist = mock.patch(
            'mpfs.core.services.mulca_service.Mulca.is_file_exist',
            return_value=is_file_exist
        )


class StaffServiceMockHelper(object):
    @staticmethod
    def _make_response_item(staff_user_record):
        result = {
            'internal_uid': staff_user_record.yateam_uid,
            'internal_login': staff_user_record.yateam_login,
            'chief_uid': staff_user_record.chief_yateam_uid,
            'is_dismissed': staff_user_record.is_dismissed,
            'affiliation': staff_user_record.affiliation,
            'is_homeworker': staff_user_record.is_homeworker,
        }
        if staff_user_record.uid:
            result['external_uid'] = staff_user_record.uid
        if staff_user_record.login:
            result['extenral_login'] = staff_user_record.login
        return result

    @classmethod
    def create_mock_login_links_response(cls, staff_user_records):
        return to_json({record.yateam_login: cls._make_response_item(record) for record in staff_user_records})

    @classmethod
    def mock_get_user_info(cls, yateam_uid, yateam_login='yateam_login', uid=None, login=None, chief_yateam_uid=None, is_dismissed=False):
        record = StaffUserRecord(
            yateam_uid=yateam_uid,
            yateam_login=yateam_login,
            chief_yateam_uid=chief_yateam_uid,
            uid=uid,
            login=login,
            is_dismissed=is_dismissed,
            affiliation='yandex',
            is_homeworker=False,
        )
        mock_response = cls.create_mock_login_links_response([record])
        return mock.patch('mpfs.core.services.staff_service.StaffService.open_url', return_value=mock_response)


class StaffServiceSmartMockHelper(object):
    """Реализует честную логику работы ручек стаффа, используя кэш в памяти.
        В начале теста надо сбросить кэш.

       Пример использования:
           with StaffServiceSmartMockHelper.mock_get_user_info():
               StaffServiceSmartMockHelper.add_user_info('1234', uid='1234')
               StaffServiceSmartMockHelper.add_user_info('12345', uid='12345')
               staff_users = StaffService.get_all_user_infos()  # вернёт обе записи
    """
    _get_user_info_cache = {}

    @classmethod
    def clear_user_info_cache(cls):
        cls._get_user_info_cache = {}

    @classmethod
    def add_user_info(cls, yateam_uid, yateam_login='yateam_login', uid=None, login=None, chief_yateam_uid=None, is_dismissed=False):
        record = StaffUserRecord(
            yateam_uid=yateam_uid,
            yateam_login=yateam_login,
            chief_yateam_uid=chief_yateam_uid,
            uid=uid,
            login=login,
            is_dismissed=is_dismissed,
            affiliation='yandex',
            is_homeworker=False,
        )
        cls._get_user_info_cache[yateam_uid] = record

    @classmethod
    def remove_user_info(cls, yateam_uid):
        del cls._get_user_info_cache[yateam_uid]

    @classmethod
    def _mock_user_info_wrapper(cls, url, **kwargs):
        cache = cls._get_user_info_cache
        parsed_url = urlparse.urlparse(url)
        query = urlparse.parse_qs(parsed_url.query)
        if 'internal_uid' not in query:
            return StaffServiceMockHelper.create_mock_login_links_response(cache.itervalues())
        return StaffServiceMockHelper.create_mock_login_links_response([x for x in cache.itervalues() if x.yateam_uid in query['internal_uid']])

    @classmethod
    def mock_get_user_info(cls):
        return mock.patch('mpfs.core.services.staff_service.StaffService.open_url', wraps=cls._mock_user_info_wrapper)


class DirectoryServiceSmartMockHelper(object):
    """Реализует честную логику работы ручек директории, используя кэш в памяти.
        В начале теста надо сбросить кэш.

       Пример использования:
           with DirectoryServiceSmartMockHelper.mock():
               StaffServiceSmartMockHelper.add_user_info('1234', uid='1234')
               StaffServiceSmartMockHelper.add_user_info('12345', uid='12345')
               staff_users = StaffService.get_all_user_infos()  # вернёт обе записи
    """

    _organization_limit_cache = {}
    _organization_used_cache = {}
    _organization_info_cache = {}

    @staticmethod
    def get_event_organization_added(organization_id):
        return {
            'content': {},
            'event': 'organization_added',
            'obj': {
                'id': int(organization_id),
            },
            'org_id': int(organization_id),
            'revision': 1000,
        }

    @staticmethod
    def get_event_organization_subscription_plan_changed(organization_id, is_paid):
        return {
            'content': {
                'subscription_plan': 'paid' if is_paid else 'free',
            },
            'event': 'organization_subscription_plan_changed',
            'obj': {
                'id': int(organization_id),
            },
            'org_id': int(organization_id),
            'revision': 1000,
        }

    @classmethod
    def clear_cache(cls):
        cls._organization_limit_cache = {}
        cls._organization_used_cache = {}
        cls._organization_info_cache = {}

    @classmethod
    def add_organization(cls, organization_id, limit, free, is_paid, in_migration=False):
        cls._organization_limit_cache[organization_id] = {'free': free, 'limit': limit}
        cls._organization_used_cache[organization_id] = {'usage_other': limit - free, 'usage_disk': 0}
        cls._organization_info_cache[organization_id] = {
            'id': int(organization_id),
            'subscription_plan': 'paid' if is_paid else 'free',
            'disk_limit': limit,
            'disk_usage': limit - free,
            'in_migration': in_migration,
        }

    @classmethod
    def add_organization_info(cls, organization_id, is_paid):
        cls._organization_info_cache[organization_id] = {
            'id': int(organization_id),
            'subscription_plan': 'paid' if is_paid else 'free',
        }

    @classmethod
    def add_organization_disk_limits(cls, organization_id, limit, free):
        cls._organization_limit_cache[organization_id] = {'free': free, 'limit': limit}
        cls._organization_used_cache[organization_id] = {'usage_other': limit - free, 'usage_disk': 0}
        cls._organization_info_cache[organization_id].update({
            'disk_limit': limit,
            'disk_usage': limit - free,
        })

    @classmethod
    def _mock_open_url_wrapper(cls, url, method=None, headers=None, pure_data=None, **kwargs):
        parsed_url = urlparse.urlparse(url)
        organization_id = headers.get('X-Org-ID')
        query = dict(urlparse.parse_qsl(parsed_url.query))

        if parsed_url.path.startswith('/v5/organizations/') and 'show' not in query:
            if organization_id not in cls._organization_info_cache:
                raise urllib2.HTTPError(url, 404, '{"message": "Organization does not exist"}', headers, None)
            if cls._organization_info_cache[organization_id]['in_migration']:
                e = APIError()
                e.data = {'text': '{"code": "not-ready", "message": "Organization is not ready yet"}',
                          'code': 403, 'headers': dict(headers)}
                raise e
            return to_json(cls._organization_info_cache[organization_id])

        # возвращает все организации из кэша по одной имитируя pagination Директории
        if parsed_url.path == '/v5/organizations/' and query['show'] == 'all':
            result = {'result': [], 'links': {}}
            if not cls._organization_info_cache:
                return to_json(result)

            if '__index' in query:
                index = int(query['__index'])
            else:
                index = 0
            result['result'].append(cls._organization_info_cache[sorted(cls._organization_info_cache.iterkeys())[index]])
            if index + 1 < len(cls._organization_info_cache):
                query['__index'] = index + 1
                next_url = urlparse.urlunparse(parsed_url._replace(query=urllib.urlencode(query)))
                result['links']['next'] = next_url
            return to_json(result)

        if parsed_url.path == '/v5/disk-usage/' and (method is None or method == 'GET'):
            if organization_id not in cls._organization_limit_cache:
                raise urllib2.HTTPError(url, 404, '{"message": "Organization does not exist"}', headers, None)
            if cls._organization_info_cache[organization_id]['in_migration']:
                e = APIError()
                e.data = {'text': '{"code": "not-ready", "message": "Organization is not ready yet"}',
                          'code': 403, 'headers': {}}
                raise e
            return to_json(cls._organization_limit_cache[organization_id])

        if parsed_url.path == '/v5/disk-usage/' and method == 'POST':
            if organization_id not in cls._organization_limit_cache:
                raise urllib2.HTTPError(url, 404, '{"message": "Organization does not exist"}', headers, None)
            usage = from_json(pure_data)['usage']
            limits = cls._organization_limit_cache[organization_id]
            used = cls._organization_used_cache[organization_id]
            used['usage_disk'] = usage
            limits['free'] = limits['limit'] - used['usage_other'] - used['usage_disk']
            return to_json(cls._organization_limit_cache[organization_id])

        raise NotImplementedError(url)

    @classmethod
    def mock(cls):
        return mock.patch('mpfs.core.services.directory_service.DirectoryService.open_url', wraps=cls._mock_open_url_wrapper)


class RestoreDBServiceStub(base.ChainedPatchBaseStub):
    def __init__(self, exclude_patches=None):
        self._data = collections.defaultdict(dict)
        self.put_personal = mock.patch(
            'mpfs.core.services.restore_db_service.RestoreDBService.put_personal',
            new=self._put_personal
        )
        self.delete_personal = mock.patch(
            'mpfs.core.services.restore_db_service.RestoreDBService.delete_personal',
            new=self._delete_personal
        )
        self.get_personal = mock.patch(
            'mpfs.core.services.restore_db_service.RestoreDBService.get_personal',
            new=self._get_personal
        )
        super(RestoreDBServiceStub, self).__init__(exclude_patches=exclude_patches)

    def _put_personal(self, uid, checksums_obj):
        self._data[uid][checksums_obj.hid] = {
            'id': checksums_obj.hid,
            'md5': checksums_obj.md5,
            'size': checksums_obj.size,
            'sha256': checksums_obj.sha256,
            'date_created': datetime.datetime.now().isoformat()
        }

    def _delete_personal(self, uid, hid):
        if uid in self._data:
            self._data[uid].pop(hid, None)

    def _get_personal(self, uid, hid):
        if uid in self._data:
            return self._data[uid].get(hid)


class HancomServiceStub(base.ChainedPatchBaseStub):
    def __init__(self, number_of_polls=1, exclude_patches=None):

        class HancomServiceSubsequentMock(object):
            def __init__(self, number_of_polls):
                self.try_number = 0
                self.current_time = int(time.time())
                self.number_of_polls = number_of_polls

            def get_document_status(self, *args, **kwargs):
                last_modified = self.current_time
                continue_polling = True
                if self.try_number + 1 >= self.number_of_polls:
                    continue_polling = False
                self.try_number += 1
                self.current_time += 1
                return last_modified, continue_polling

        self.hancom_get_document_status_mock = mock.patch(
            'mpfs.core.services.hancom_service.HancomService.get_document_status',
            HancomServiceSubsequentMock(number_of_polls).get_document_status
        )
        super(HancomServiceStub, self).__init__(exclude_patches=exclude_patches)


class AbookStub(base.ChainedPatchBaseStub):
    """Заменяем заглушкой abook (Адресную Книгу)"""

    def _open_url(url, post={}, cookie={}, **kwargs):
        response = None
        if 'searchContacts' in url:
            if 'mixin' in url:
                response = abook_info.pdd_data
            else:
                response = abook_info.default_data

        return response

    open_url = mock.patch('mpfs.core.services.abook_service.AbookService.open_url',
                          side_effect=_open_url)


class EmptyPreparedRequest(object):
    def __init__(self):
        self.method = None
        self.url = None
        self.headers = None
        self.body = None


class SingleResponseMockSession(object):
    def __init__(self, response):
        self.response = response

    def prepare_request(self, request):
        return EmptyPreparedRequest()

    def send(self, prepared_request, **kwargs):
        return self.response

    def close(self):
        pass


class DjfsApiMockHelper(object):
    @staticmethod
    @contextmanager
    def mock_request(error_code=None, additional_error_data=None, status_code=None, content=None, title='NOT IMPLEMENTED'):
        response = requests.Response()
        response.request = EmptyPreparedRequest()
        if status_code:
            response.status_code = status_code
            response._content = '{}'

        if error_code:
            response.status_code = status_code or 400
            if status_code:
                response.status_code = status_code
            body = {'code': error_code, 'title': title}
            if additional_error_data:
                body['data'] = additional_error_data
            response._content = to_json(body)
            response.headers[MPFS_ERROR_CODE_HEADER_NAME] = '%d' % error_code

        if not status_code and not error_code:
            response.status_code = 200
            response._content = '{}'

        if content:
            response._content = content

        saved = djfs_api._session
        djfs_api._session = SingleResponseMockSession(response)
        yield
        djfs_api._session = saved


class DataApiStub(base.ChainedPatchBaseStub):
    """Заменяет заглушкой Data API.

    Умеет только возвращать 500 на PUT с данными.
    """
    _original_open_url = DataApiService.open_url

    def __init__(self, open_url_mock_kwargs=None, exclude_patches=None):
        if open_url_mock_kwargs is None:
            open_url_mock_kwargs = {
                'return_value':
                    (200,
                     '{"base_revision":7,"items":[],"total":0,"limit":100,"revision":7}',
                     {'content-type': 'application/json'})
            }

        self.open_url = mock.patch(
            'mpfs.core.services.data_api_service.DataApiService.open_url',
            **open_url_mock_kwargs
        )
        super(DataApiStub, self).__init__(exclude_patches=exclude_patches)

    @staticmethod
    def data_api_open_url_with_500_on_put_with_data(self, url, post={}, cookie={}, **kwargs):
        if kwargs.get('method') == 'PUT' and kwargs.get('pure_data'):
            error = self.api_error()
            error.data = {'text': "Internal EHOT", 'code': 500, 'headers': ''}
            raise error
        return DataApiStub._original_open_url(self, url, post, cookie=cookie, **kwargs)


class HbfServiceStub(base.ChainedPatchBaseStub):
    """
    Отключает HbfService

    Запросы всегда предсказуемы
    """
    tmp_cache_filename = '/tmp/mpfs_cache_hbf_service_stub'
    ip_included = '0.0.0.1'
    ip_excluded = '1.0.0.0'
    get_default_network_list_response = requests.Response()
    get_default_network_list_response.request = EmptyPreparedRequest()
    get_default_network_list_response.status_code = 200
    get_default_network_list_response._content = to_json([ip_included])
    get_default_network_list = mock.patch(
        'mpfs.core.services.hbf_service.HbfService.request',
        return_value=get_default_network_list_response
    )
    get_tmp_cache_file = mock.patch.dict(
        settings.services['HbfService'], {'cache_file_path': tmp_cache_filename},
    )

    def start(self):
        started_mocks = super(HbfServiceStub, self).start()
        self.get_tmp_cache_file.start()
        HbfService().update_cache()
        return started_mocks

    def stop(self):
        super(HbfServiceStub, self).stop()
        self.get_tmp_cache_file.stop()
        HbfService().update_cache()
        if os.path.exists(self.tmp_cache_filename):
            os.unlink(self.tmp_cache_filename)


class UAASStub(base.ChainedPatchBaseStub):
    def __init__(self, experiments_flags=('flagname1', 'flagname2'), experiments_testids=None, exclude_patches=None, exp_tmpl=None):
        super(UAASStub, self).__init__(exclude_patches=exclude_patches)
        if experiments_testids is None:
            experiments_testids = xrange(len(experiments_flags))

        if not exp_tmpl:
            exp_tmpl = '[{"HANDLER": "DISK", "CONTEXT": {"DISK": {"flags": ["%s"], "testid": ["%s"]}}}]'

        # в некоторых тестах не нужно делать форматирование строки
        try:
            encoded_experiments = [base64.b64encode(exp_tmpl % (experiment_flag, experiments_testids[i]))
                                   for i, experiment_flag in enumerate(experiments_flags)]
        except TypeError:
            encoded_experiments = [base64.b64encode(exp_tmpl)]

        mock_response = requests.Response()
        mock_response.headers = {'X-Yandex-ExpFlags': ','.join(encoded_experiments),
                                 'X-Yandex-ExpConfigVersion': 123}
        mock_response.status_code = 200

        self.request = mock.patch('mpfs.core.services.uaas_service.UAAS.request',
                                  return_value=mock_response)


class NewUAASStub(base.ChainedPatchBaseStub):
    def __init__(self, experiments=None, exclude_patches=None):
        if experiments is None:
            experiments = tuple()

        mock_response = requests.Response()
        mock_response._content = json.dumps({'uas_exp_flags': experiments})
        mock_response.status_code = 200

        self.get_disk_experiments = mock.patch(
            'mpfs.core.services.uaas_service.NewUAAS.request',
            return_value=mock_response
        )
        super(NewUAASStub, self).__init__(exclude_patches=exclude_patches)


class SpamCheckerStub(base.ChainedPatchBaseStub):
    def __init__(self, return_value=None, exclude_patches=None):
        if return_value is None:
            return_value = construct_requests_resp(content=SpamCheckerData.DEFAULT,
                                                   request_url='http://checkform2-test.n.yandex-team.ru')
        self.patch_requests = mock.patch(
            'mpfs.core.services.spam_checker_service.SpamCheckerService.request',
            return_value=return_value
        )
        super(SpamCheckerStub, self).__init__(exclude_patches=exclude_patches)
