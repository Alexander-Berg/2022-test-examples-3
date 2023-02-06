import pytest
import requests_mock
from search.mon.wabbajack.libs.modlib.api_wrappers import walle
from walle_api import WalleClient

_ADDR = "mock://api.wall-e.yandex-team.ru/v1/get-hosts?resolve_deploy_configuration=False&fields=active_mac%2Cactive_mac_source%2Cactive_mac_time%2Cconfig%2Cdeploy_network%2Cdeploy_tags%2Cextra_vlans%2Chealth.check_statuses%2Chealth.reasons%2Chealth.status%2Cinv%2Cipmi_mac%2Clocation.city%2Clocation.country%2Clocation.datacenter%2Clocation.network_source%2Clocation.network_timestamp%2Clocation.physical_timestamp%2Clocation.port%2Clocation.queue%2Clocation.rack%2Clocation.short_datacenter_name%2Clocation.short_queue_name%2Clocation.switch%2Cmacs%2Cname%2Cproject%2Cprovisioner%2Crestrictions%2Cstate%2Cstatus%2Cstatus_audit_log_id%2Cstatus_timeout%2Ctask.audit_log_id%2Ctask.error%2Ctask.owner%2Ctask.status%2Ctask.status_message%2Cticket&strict=True"

_RESP = {'result': [{'active_mac': '00:15:b2:a7:7b:8c', 'active_mac_source': 'agent', 'active_mac_time': 1552664068, 'health': {'check_statuses': {'bmc': 'passed', 'cpu': 'passed', 'cpu_capping': 'passed', 'disk': 'passed', 'fs_check': 'passed', 'gpu': 'passed', 'link': 'passed', 'memory': 'passed', 'net64_check': 'passed', 'overheat': 'passed', 'rack': 'passed', 'reboots': 'passed', 'ssh': 'passed', 'switch': 'passed', 'tainted_kernel': 'passed', 'unreachable': 'passed', 'walle_host_certificate': 'passed', 'walle_meta': 'passed'}, 'status': 'ok'}, 'inv': 100411926, 'ipmi_mac': '00:15:b2:a7:7b:8f', 'location': {'city': 'SAS', 'country': 'RU', 'datacenter': 'SASTA', 'network_source': 'lldp', 'network_timestamp': 1552663948, 'physical_timestamp': 1537303175, 'port': 'ge1/0/19', 'queue': 'SAS-2.4.4', 'rack': '7', 'short_datacenter_name': 'sas', 'short_queue_name': 'sas2.4.4', 'switch': 'sas1-s739'}, 'macs': ['00:15:b2:a7:7b:8c', '00:15:b2:a7:7b:8d', 'e4:1d:2d:6d:bc:50'], 'name': 'sas2-9472.search.yandex.net', 'project': 'yp-iss-sas-dev', 'restrictions': ['automation', 'reboot'], 'state': 'assigned', 'status': 'ready', 'status_audit_log_id': 'e57d220166164b2d80154e80b63f17eb'}],'total': 1}


def mocked_hosts_state(hosts, *args, **kwargs):
    """A method replacing Requests.get
    Returns either a mocked response object (with json method)
    or the default response object if the url doesn't match
    one of those that have been supplied.
    """
    return _RESP

@pytest.fixture(scope='function')
def success_resps(monkeypatch):
    # finally, patch requests.Session with patched version
    monkeypatch.setattr(WalleClient, 'get_hosts', mocked_hosts_state)


class TestWalleApi:
    def setup_class(self):
        pass

    def test_hosts_state_return_type(self, success_resps):
        assert walle.hosts_state(hosts='sas2-9472.search.yandex.net') == _RESP['result']

    def teardown_class(self):
        pass


# EOF

