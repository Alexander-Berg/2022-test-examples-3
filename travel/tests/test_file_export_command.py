# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import gzip
import json

import mock
from hamcrest import assert_that, has_entry, has_entries

from travel.library.python.rasp_vault import common, cli


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


def test_file_export(tmpdir):
    filepath = str(tmpdir.join('export.json.gz'))
    client_mock = build_valut_mock()
    with mock.patch.object(common, 'get_vault_client', return_value=client_mock):
        cli.run(['file-export', '--file={}'.format(filepath)])

    with gzip.open(filepath, 'rb') as f:
        result = json.load(f)

    assert_that(result, has_entry('mysecret-name', has_entries({
        'secret': 'sec-01',
        'version': 'ver-02',
        'value': {'key': 'some_secret_key'}
    })))
