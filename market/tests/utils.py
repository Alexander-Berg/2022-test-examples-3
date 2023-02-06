# -*- coding: utf-8 -*-

import flask
import os
import six

from hamcrest import not_none
from hamcrest.core.base_matcher import BaseMatcher
from yatest.common import source_path

'''
os.getenv('IDXAPI_CONFIG_PATH', '/etc/yandex/idxapi/common.ini'),
os.getenv('IDXAPI_LOCAL_CONFIG_PATH', '/etc/yandex/idxapi/local.ini'),
'''
COMMON_PATH_ENV_VAR = 'IDXAPI_CONFIG_PATH'
LOCAL_PATH_ENV_VAR = 'IDXAPI_LOCAL_CONFIG_PATH'
COMMON_PATH = os.path.abspath(source_path('market/idx/api/etc/common.ini'))
LOCAL_PATH = os.path.abspath(source_path('market/idx/api/etc/conf-available/testing.planeshift.ini'))


def set_config_env_variables():
    os.environ[COMMON_PATH_ENV_VAR] = COMMON_PATH
    os.environ[LOCAL_PATH_ENV_VAR] = LOCAL_PATH


def reset_config_env_variables():
    if COMMON_PATH_ENV_VAR in os.environ:
        os.environ.pop(COMMON_PATH_ENV_VAR)
    if LOCAL_PATH_ENV_VAR in os.environ:
        os.environ.pop(LOCAL_PATH_ENV_VAR)


class ResponseMatcher(BaseMatcher):
    def __init__(self, code, data, content_type):
        self.code = code
        self.data = data
        self.content_type = content_type
        self._reason = "Response: [{0}] {1}".format(self.code, self.data or "")
        if self.content_type:
            self._reason = "{0} with Content-type: {1}".format(self._reason, self.content_type)

    def _get_content_type(self, request):
        header = 'Content-type'
        if hasattr(request.headers, 'getheader'):
            return request.headers.getheader(header)
        return request.headers.get(header)

    def _matches(self, item):
        if not isinstance(item, flask.Response):
            return False

        if item.status_code != self.code:
            return False

        if self.content_type and self.content_type != self._get_content_type(item):
            return False

        if isinstance(self.data, BaseMatcher):
            return self.data.matches(six.ensure_str(item.data))

        return six.ensure_str(item.data) == six.ensure_str(self.data)

    def describe_to(self, description):
        description.append_text(self._reason)


def is_success_response(data=not_none(), content_type=None):
    return ResponseMatcher(code=200, data=data, content_type=content_type)


def is_error_response(data=not_none(), code=500, content_type=None):
    return ResponseMatcher(code=code, data=data, content_type=content_type)


def is_response(data, code, content_type=None):
    return ResponseMatcher(code=code, data=data, content_type=content_type)


def is_bad_response(data=not_none(), content_type=None):
    return ResponseMatcher(code=400, data=data, content_type=content_type)


def is_not_found_response(data=not_none(), code=404, content_type=None):
    return ResponseMatcher(code=code, data=data, content_type=content_type)


def is_redirection_response(data=not_none(), code=302, content_type=None):
    return ResponseMatcher(code=code, data=data, content_type=content_type)
