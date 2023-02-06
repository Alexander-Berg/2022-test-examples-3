import json
import urlparse

import pytest


def get_hosts(source_data, skip_hosts=None):
    skip_hosts = skip_hosts or []
    hosts = []
    if isinstance(source_data, list):
        hosts = [source_data[0]]
    elif isinstance(source_data, dict):
        grouped_hosts = source_data["Hosts"]
        instances = [weighted_instances.strip("()").split("@")[0] for weighted_instances in grouped_hosts.split()]
        instances = ["//{}".format(instance) if "://" not in instance else instance for instance in instances]
        hosts = [urlparse.urlparse(instance).hostname for instance in instances]
    hosts = filter(lambda x: x not in skip_hosts, hosts)
    return hosts


@pytest.mark.request
@pytest.mark.config
def test_source_host_in_cache(source_data, dnscache):
    hosts = get_hosts(source_data, skip_hosts=['localhost'])
    assert set(hosts) <= set(dnscache), \
        "Source has unresolved host: '{}'".format(set(hosts) - set(dnscache))


@pytest.mark.request
@pytest.mark.config
def test_source_empty(source_data):
    hosts = get_hosts(source_data)
    assert bool(set(hosts)), "Source has no hosts"


def pytest_generate_tests(metafunc):
    conf_file = metafunc.config.option.config
    config = json.load(open(conf_file))
    dnscache_hosts = [item.split("=")[0] for item in config["_DNSCACHE_"]]
    sources = [(name, value) for name, value in config.items() if not name.startswith("_")]
    if 'source_data' in metafunc.fixturenames:
        metafunc.parametrize("source_data", [item[1] for item in sources], ids=[item[0] for item in sources])
    if 'dnscache' in metafunc.fixturenames:
        metafunc.parametrize("dnscache", [dnscache_hosts])


@pytest.mark.request
@pytest.mark.config
def test_metainfo(pytestconfig):
    conf_file = pytestconfig.option.config
    config = json.load(open(conf_file))
    metainfo = config["_METAINFO_"]
    assert metainfo["version"] != 'undefined', "version should be defined"
    assert metainfo["sandbox_task"].isdigit(), "sandbox_task should be digit"
