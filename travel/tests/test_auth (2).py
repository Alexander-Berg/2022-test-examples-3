# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import sys
from base64 import b64encode

from travel.library.python.base_http_client import OAuthHeaderCreator, HTTPBasicAuthHeaderCreator


def test_oauth_header_creator():
    header_creator = OAuthHeaderCreator('token')
    assert header_creator.get_headers() == {'Authorization': 'OAuth token'}


def test_http_basic_auth_header_creator():
    # Код продублирован из стандартной реализации параметра auth библиотеки requests
    # https://github.com/psf/requests/blob/33cf965f7271ab4978ed551754db37865c4085db/requests/auth.py#L60
    username = 'user_login'.encode('latin1')
    password = 'user_password'.encode('latin1')
    b64_str = b64encode(b':'.join((username, password))).strip()
    coded_str = b64_str.encode('ascii') if sys.version_info[0] < 3 else b64_str.decode('ascii')

    header_creator = HTTPBasicAuthHeaderCreator('user_login', 'user_password')
    assert header_creator.get_headers() == {'Authorization': 'Basic {}'.format(coded_str)}
