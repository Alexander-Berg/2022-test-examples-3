# -*- coding: utf-8 -*-

import socket
import os
import pytest
import lxml.etree

pytest_plugins = [
    'runtime_tests.plugin',
]

def pytest_addoption(parser):
    parser.addoption('-H', '--host', action='store', default='localhost:80',  help='host:port with running report')
    parser.addoption('-R', '--reportpath', action='store', default='/hol/arkanavt',  help='Path to report')
    parser.addoption('-E', '--enable_unstable', action='store_true', default=False,  help='Enable unstable tests')
    parser.addoption('-D', '--enable_datatests', action='store_false', default=True,  help='Enable data tests')
    parser.addoption('-T', '--ticket', action='store', default='',  help='Choose ticket to run related tests')
    parser.addoption('--flags', action='store', default=False, help='additional flags')
    parser.addoption('--flaky_hack', action = 'store_true', default = False, help = "hack for flaky")

@pytest.fixture(scope='session')
def sock_family(request):
    sock_family = socket.AF_INET6 # prefer IPv6
    try:
        try:
            socket.getaddrinfo(request.config.option.host.split(':')[0], None, socket.AF_INET6)
        except socket.gaierror:
            sock_family = socket.AF_INET
    except :
        pytest.skip("Can't understand is host can ipv4. %s" % request.config.option.host)
    return sock_family

def get_ip(host, sock_family):
    ip = None
    try:
        ip = socket.getaddrinfo(host, None, sock_family)
        ip = ip[0][4][0]
    except socket.gaierror:
        print "WARNING: Can't get %s for hostname: %s" % ('IPv6' if sock_family == socket.AF_INET6 else 'IPv4', host)
    return ip

@pytest.fixture(scope='session')
def report_ip(request, sock_family):
    host = request.config.option.host.split(':')[0]
    return get_ip(host, sock_family)

@pytest.fixture(scope='session')
def local_ip(sock_family):
    host = socket.getfqdn()
    return get_ip(host, sock_family)

@pytest.fixture(scope='session')
def report_port(request):
    port = 80
    try:
        if len(request.config.option.host.split(':')) == 2:
            port = int(request.config.option.host.split(':')[1])
    except :
        pytest.skip("Can't get port from %s" % request.config.option.host)
    return port

@pytest.fixture(scope='session')
def sandbox(request):
    return request.config.option.sandbox

@pytest.fixture(autouse=True)
def skip_in_sandbox(request, sandbox):
    if request.node.get_marker('skip_in_sandbox') and sandbox:
        pytest.skip('skipped in sandbox')

@pytest.fixture(scope='session')
def enable_unstable(request):
    return request.config.option.enable_unstable

@pytest.fixture(autouse=True)
def unstable(request, enable_unstable):
    if request.node.get_marker('unstable') and not enable_unstable:
        pytest.skip('skipped unstable tests')

def pytest_unconfigure(config):
    # we need patch xml report
    # because of flaky plugin add element when rerun test
    if config.getoption('flaky_hack') and config.getoption('--junitxml'):
        old = '{}.old'.format(config.getoption('--junitxml'))
        new = config.getoption('--junitxml')
        os.rename(new, old)
        doc = lxml.etree.parse(old)
        root = doc.getroot()
        for elt in root.getiterator('testcase'):
            if not elt.attrib.has_key('name'):
                root.remove(elt)
        doc.write(new)

def pytest_collection_modifyitems(items, config):
    new_items = list()

    if config.option.ticket:
        for item in items:
            if item._request.node.get_marker('ticket'):
                if item._request.config.option.ticket in item._request.node.get_marker('ticket').args:
                    new_items.append(item)
        items[:] = new_items

    new_items = list()
    if config.getoption('flaky_hack'):
        def last_test_always_passed(*args):
            pass

        last_func = None
        for l in items:
            if last_func is None:
                last_func = l
            elif last_func.module.__name__ != l.module.__name__ or last_func.cls.__name__ != l.cls.__name__:
                last_func.cls.test_last_always_passed = last_test_always_passed
                n = pytest.Function(
                    'test_last_always_passed', last_func.parent, args=None, config=last_func.config,
                    callspec=None, callobj=last_test_always_passed, keywords=None, session=last_func.session,
                    fixtureinfo=None
                )
                new_items.append(n)
                last_func = l
            new_items.append(l)

        last_func.cls.test_last_always_passed = last_test_always_passed
        n = pytest.Function('test_last_always_passed', last_func.parent, args=None, config=last_func.config,
                            callspec=None, callobj=last_test_always_passed, keywords=None, session=last_func.session,
                            fixtureinfo=None)
        new_items.append(n)
        items[:] = new_items

@pytest.fixture(scope='session')
def root_dir(request):
    return os.path.join(os.path.dirname(__file__))

@pytest.fixture(scope='session')
def schema_dir(request, root_dir):
    return os.path.join(root_dir, 'schema_data')

@pytest.fixture(scope='class')
def schema_path_class(request, schema_dir):
    return os.path.join(schema_dir, request.node.name)

@pytest.fixture(scope='function')
def schema_path_to_contexts(request, schema_dir):
    return os.path.join(schema_dir, 'contexts')

@pytest.fixture(scope='function')
def schema_path(request, schema_path_class):
    return os.path.join(schema_path_class, request.node.name + '.json')

@pytest.fixture(scope='function')
def custom_schema_path(request, schema_path_class):

    def p(suffix):
        return os.path.join(schema_path_class, suffix)

    return p
