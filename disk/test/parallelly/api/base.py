# -*- coding: utf-8 -*-
import os

os.environ['MPFS_PACKAGE'] = 'platform'
import types
import urllib
import urlparse

from collections import defaultdict
from contextlib import contextmanager

from test.base import MpfsBaseTestCase
from test.fixtures.users import default_user

from mpfs.common.static import tags
from mpfs.common.util import to_json
from mpfs.common.util.urls import urlencode
from mpfs.core.services.mpfsproxy_service import mpfsproxy
from mpfs.platform import set_api_mode
from mpfs.platform.auth import InternalAuth, OAuthAuth
from mpfs.platform.common import PlatformClient, PlatformUser
from mpfs.platform.dispatchers import InternalDispatcher, ExternalDispatcher
from mpfs.platform.resources import NamespaceResource, res
from mpfs.platform.routers import RegexRouter
from mpfs.platform.system.system.handlers import GetPing
from mpfs.platform.utils import build_flask_request
from mpfs.platform.v1.resources import V1Namespace
from mpfs.platform.v2.resources import V2Namespace
from mpfs.platform.handlers import GetApiInfoHandler, HeadApiInfoHandler


class BasePlatformTestClient(object):
    dispatcher = None
    router = None
    default_headers = {}

    def __init__(self, base_uri='http://localhost/'):
        self.base_uri = base_uri

    def build_request(self, method, uri, data=None, headers=None, ip=None):
        h = {}
        h.update(self.default_headers)
        if headers:
            h.update(headers)
        data = data or ''
        if isinstance(data, (dict, list)):
            data = to_json(data)
        return build_flask_request(method, uri, data=data, headers=h, ip=ip)

    def request(self, method, uri, query=None, data=None, headers=None, ip=None, *args, **kwargs):
        uri = urlparse.urljoin(self.base_uri, uri)
        scheme, netloc, path, inline_query, fragment = urlparse.urlsplit(uri)

        if query:
            query_dict = urlparse.parse_qs(inline_query)
            if isinstance(query, str):
                query = urlparse.parse_qs(query)
            query_dict.update(query)

            inline_query = urlencode(query_dict)

        uri = urlparse.urlunsplit((scheme, netloc, path, inline_query, fragment))
        request = self.build_request(method, uri, data, headers, ip)

        self.router.set_request(request)
        response = self.dispatcher.dispatch(request)
        return response

    def get(self, uri, query=None, headers=None, **kwargs):
        return self.request('GET', uri, query=query, headers=headers, **kwargs)

    def post(self, uri, query=None, data=None, headers=None, **kwargs):
        return self.request('POST', uri, query=query, data=data, headers=headers, **kwargs)

    def put(self, uri, query=None, data=None, headers=None, **kwargs):
        return self.request('PUT', uri, query=query, data=data, headers=headers, **kwargs)

    def patch(self, uri, query=None, data=None, headers=None, **kwargs):
        return self.request('PATCH', uri, query=query, data=data, headers=headers, **kwargs)

    def delete(self, uri, query=None, headers=None, **kwargs):
        return self.request('DELETE', uri, query=query, headers=headers, **kwargs)

    def head(self, uri, query=None, headers=None, **kwargs):
        return self.request('HEAD', uri, query=query, headers=headers, **kwargs)


class APIRootNamespace(NamespaceResource):
    relations = {
        'GET': GetApiInfoHandler,
        'HEAD': HeadApiInfoHandler,
        'ping': res(relations={'GET': GetPing}, hidden=True),
        'v1': V1Namespace,
        'v2': V2Namespace,
    }


class InternalPlatformTestClient(BasePlatformTestClient):
    dispatcher = None
    client_id = 'autompfstest'
    client_name = 'autompfstestservice'

    def __init__(self, *args, **kwargs):
        super(InternalPlatformTestClient, self).__init__(*args, **kwargs)
        self.router = RegexRouter(APIRootNamespace)
        self.dispatcher = InternalDispatcher(self.router)

    def request(self, method, uri, uid=None, query=None, *args, **kwargs):
        if uid and not bool(urlparse.urlparse(uri).netloc):
            uri = '%s/%s' % (uid, uri)

        q = {}
        if 'Authorization' not in (kwargs.get('headers') or {}) and kwargs.get('ip') is None:
            q.update({'client_id': self.client_id, 'client_name': self.client_name})

        if query:
            q.update(query)
        return super(InternalPlatformTestClient, self).request(method, uri, q, *args, **kwargs)


class ExternalPlatformTestClient(BasePlatformTestClient):
    dispatcher = None

    def __init__(self, *args, **kwargs):
        super(ExternalPlatformTestClient, self).__init__(*args, **kwargs)
        self.router = RegexRouter(APIRootNamespace)
        self.dispatcher = ExternalDispatcher(self.router)


class ApiTestCase(MpfsBaseTestCase):
    api_mode = tags.platform.INTERNAL
    api_version = 'v1'

    _objects_patched_attrs = defaultdict(dict)

    def __init__(self, *args, **kwargs):
        super(ApiTestCase, self).__init__(*args, **kwargs)
        set_api_mode(self.api_mode)
        base_uri = 'http://localhost/%s/' % self.api_version
        self.internal_client = InternalPlatformTestClient(base_uri)
        self.external_client = ExternalPlatformTestClient(base_uri)

    def get_client(self, mode=None):
        mode = mode or self.api_mode
        if mode == tags.platform.INTERNAL:
            return self.internal_client
        else:
            return self.external_client

    client = property(get_client)

    def _set_object_patch(self, obj, attr_name, new_val):
        if attr_name not in self._objects_patched_attrs[obj]:
            self._objects_patched_attrs[obj][attr_name] = getattr(obj, attr_name, None)
        if hasattr(new_val, '__call__'):
            new_val = types.MethodType(new_val, obj)
        setattr(obj, attr_name, new_val)

    def set_dispatcher_patch(self, attr_name, new_val):
        """
        Заменяет атрибут диспатчера на переданное значение

        При этом запоминает оригинальное значение и восстанавливает все оригинальные значения при вызове метода
        `remove_dispatcher_patches`.
        """
        for d in (self.internal_client.dispatcher, self.external_client.dispatcher):
            self._set_object_patch(d, attr_name, new_val)

    def _remove_object_patch(self, obj, attr_name):
        patches = self._objects_patched_attrs.get(obj, {})
        if attr_name in patches:
            setattr(obj, attr_name, patches.pop(attr_name))

    def remove_dispatcher_patch(self, attr_name):
        """
        Удаляет патч установленный методом `set_dispatcher_patch` и восстанавливает оригинвальное значение атрибута.
        """
        for d in (self.internal_client.dispatcher, self.external_client.dispatcher):
            self._remove_object_patch(d, attr_name)

    @contextmanager
    def specified_client(self, id=InternalPlatformTestClient.client_id, name=InternalPlatformTestClient.client_name,
                         scopes=None, ip=None, uid=default_user.uid, login=None, display_name=None, platform_user=None,
                         user_details=None):

        def authorize_patch(auth_method, request):
            client = PlatformClient()
            client.ip = ip or request.get_real_remote_addr()
            client.id = id
            client.name = name
            client.display_name = display_name
            client.scopes = scopes or []
            request.client = client

            if uid is not None:
                user = PlatformUser()
                user.uid = uid
                user.login = login
                user.display_name = display_name
                if user_details is not None:
                    user.details = user_details
                request.user = user

            if platform_user is not None:
                request.user = platform_user

            return True

        auth_methods = (InternalAuth, OAuthAuth)
        for auth_method in auth_methods:
            self._set_object_patch(auth_method, 'authorize', authorize_patch)
        try:
            yield
        finally:
            for auth_method in auth_methods:
                self._remove_object_patch(auth_method, 'authorize')

    def _permissions_test(self, scopes_to_status_tuples, method, url, query=None):
        for scopes, status_code in scopes_to_status_tuples:
            with self.specified_client(scopes=scopes):
                resp = self.client.request(method, url, query=query)
                self.assertEqual(status_code, resp.status_code)

    def assertStatusCodeEqual(self, actual_status, expected_status):
        self.assertEqual(
            actual_status, expected_status,
            msg="Expected status code <{}> doesn't match actual status code <{}>.".format(
                expected_status, actual_status
            )
        )
