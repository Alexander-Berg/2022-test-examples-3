import mock
from mock import patch
import json
from library.python import resource
import re

from market.tools.resource_monitor.lib.abc import AbcClient, AbcFakeResourcesReader


@patch.object(AbcClient, '_get')
def test_get_services(method):
    method.return_value = json.loads(resource.find('/data/abc_services_response.json'))
    x = AbcClient(token='token')
    services = x.get_services(parent_service_id=186)
    method.assert_called()
    assert len(services) == 5


def get_abc_contacts_response(request_url, params=None):
    return json.loads(resource.find('/data/abc_service_contacts_response.json'))


@patch.object(AbcClient, '_get')
def test_get_contacts(method):
    method.side_effect = get_abc_contacts_response
    x = AbcClient(token='token')
    contacts = x.get_service_contacts(service_id=186)
    method.assert_called()
    assert len(contacts) == 4
    assert next(c.content for c in contacts if c.type_code == 'other') == 'https://occupancy.yandex-team.ru/services/186/'


def get_abc_client_response(request_url, params=None):
    service_test_data = {
        'meta_market': '/data/abc_service_info_response.json',
    }
    contacts_test_data = {
        # report-general-api
        '1543': '/data/abc_other_contacts_response.json',
        # report-general-int
        '802': '/data/abc_other_contacts_55_response.json',
    }
    service_desc_test_data = {
        '905': '/data/abc_services_response.json'
    }
    patterns = {
        r'^.*/services/contacts/\?service=(?P<key>\d+)': contacts_test_data,
        r'^.*/services/\?slug=(?P<key>[a-z_]+)': service_test_data,
        r'^.*/services/\?parent=(?P<key>\d+)': service_desc_test_data
    }
    empty_file_name = '/data/abc_empty_response.json'
    filename = None
    for pattern, test_data in patterns.iteritems():
        m = re.match(pattern, request_url)
        if m:
            key = m.group('key')
            if key:
                filename = test_data.get(key, empty_file_name)
            break
    return json.loads(resource.find(filename))


@patch.object(AbcClient, '_get')
def test_fake_resources_reader(method):
    method.side_effect = get_abc_client_response
    abc_client = AbcClient(token='token')
    reader = AbcFakeResourcesReader(abc_client=abc_client, root_service_slug='meta_market')
    n = reader.find_service_name(resource_type=AbcFakeResourcesReader.TYPE_NANNY, tags={'itype': 'marketreport', 'prj': 'report-general-api'})
    assert n == 'market_report'
