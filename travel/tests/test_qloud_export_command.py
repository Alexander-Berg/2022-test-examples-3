# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import base64
import gzip
import io
import json

import mock
import pytest
import six
from hamcrest import assert_that, has_entry, has_entries

from travel.library.python.rasp_vault import common, cli


def gzip_decompress(data):
    buf = io.BytesIO(data)
    with gzip.GzipFile(fileobj=buf, mode='rb') as f:
        return f.read()


def build_valut_mock():
    client = mock.Mock()
    list_secret_item = {
        'uuid': 'sec-01',
        'name': 'mysecret-name',
        'last_secret_version': {'version': 'ver-02'},
        'tags': ['mytag'],
        'secret_roles': []
    }
    client.list_secrets.side_effect = [
        [list_secret_item],
        []
    ]
    client.get_version.return_value = {
        'secret_uuid': 'sec-01',
        'value': {
            'key': 'some_secret_key'
        },
        'version': 'ver-02'
    }
    return client


@pytest.mark.skipif(six.PY3, reason='not implemented for py3')
def test_qloud_export(capsys):
    client_mock = build_valut_mock()
    with mock.patch.object(common, 'get_vault_client', return_value=client_mock):
        cli.run(['qloud-export'])

    out, err = capsys.readouterr()

    result = json.loads(gzip_decompress(base64.b64decode(out)))
    assert_that(result, has_entry('mysecret-name', has_entries({
        'secret': 'sec-01',
        'version': 'ver-02',
        'value': {'key': 'some_secret_key'}
    })))
