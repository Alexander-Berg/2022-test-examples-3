# -*- coding: utf-8 -*-
import string
import random

from urlparse import urlparse, parse_qsl

from test.parallelly.api.base import ApiTestCase

import mpfs.platform.v1.disk.fields
import mpfs.core.services.kladun_service
import mpfs.core.albums.models

from test.conftest import setup_queue2
from mpfs.core.services.mpfsproxy_service import MpfsProxy
from mpfs.frontend.api.disk.json import JSON
from mpfs.frontend.api.disk.billing import Billing
from test.base_suit import JsonApiTestCaseMixin


class DiskApiTestCase(JsonApiTestCaseMixin, ApiTestCase):
    DEFAULT_MPFS_API_NAME = 'json'
    MPFS_API_MAP = {
        'json': JSON(),
        'billing': Billing(),
    }

    def _patch_mpfs_proxy(self):
        """
        Патчит MpfsProxyService так, чтоб он не слал запросы во внешний MPFS,
        а обрабатывал их внутри текущего процесса.
        """
        def mpfs_proxy_open_url_wrapper(mpfsproxy, url, *args, **kwargs):
            url_chunks = urlparse(url)
            mpfs_api_name, mpfs_api_method = filter(None, url_chunks.path.split('/'))[:2]
            mpfs_api = self.MPFS_API_MAP.get(mpfs_api_name)
            if not mpfs_api:
                msg = 'DiskApiTestCase supports only %s MPFS API(s). ' \
                      'Please add "%s" API to test.api.DiskApiTestCase.MPFS_API_MAP.'
                msg = msg % (','.join(['"%s"' % k for k in self.MPFS_API_MAP.keys()]),
                             mpfs_api_name)
                raise Exception(msg)

            # query = dict((k, v) for k, v in parse_qsl(url_chunks.query))
            # если в QS передали к примеру русское слово с percent-encoding, то внутри будут байты и
            # их нельзя тупо в юникод привести
            query = {
                k: v.decode('utf-8') for
                k, v in dict(
                    parse_qsl(url_chunks.query.encode('ascii'), keep_blank_values=True)
                ).items()
            }
            params = self.params(query)
            params['_request'].requestLine = '/json/'

            # Знай, id(self.request) == id(params['_request']) и это плохо, но это твоя родина
            if 'pure_data' in kwargs:
                self.request.data = kwargs['pure_data']

            mpfs_api.setup(params)

            if mpfsproxy._response_patches:
                response_patch = mpfsproxy.pop_response_patch(kwargs.get('method', 'GET'), url)
                if response_patch:
                    # эмулируем все переменные, так буд-то в самом деле сделали запрос
                    if response_patch[0] > 299:
                        err = MpfsProxy.api_error()
                        err.data = {'text': response_patch[1], 'code': response_patch[0]}
                        raise err
                    else:
                        return response_patch

            if 'pure_data' in kwargs:
                self.request.data = kwargs['pure_data']
            response = mpfs_api.process(mpfs_api_method)
            if self.response.status > 299:
                err = MpfsProxy.api_error()
                err.data = {'text': response, 'code': self.response.status}
                raise err
            elif kwargs.get('return_status'):
                return self.response.status, response, self.response.headers
            else:
                return response
        MpfsProxy.open_url = mpfs_proxy_open_url_wrapper

    def setup_method(self, method):
        super(DiskApiTestCase, self).setup_method(method)

        # сбрасываем кэш ибо MPFS странным образом запоминает результат запроса user_info и иногда
        # думает, что такой пользователь уже есть, хотя в базе его нет, т.к. она дропается после
        # каждого теста
        import mpfs.engine.process
        mpfs.engine.process.reset_cached()

        setup_queue2()

        self._patch_mpfs_proxy()
