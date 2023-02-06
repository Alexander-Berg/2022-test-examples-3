import pytest


def pytest_addoption(parser):
    parser.addoption('-B', '--betahost', action='store', default='video-trunk.balancer.serp.yandex', help='FQDN of host with running report without 1st level domain')
    parser.addoption('-T', '--templatehost', action='store', default='stable.priemka.yandex', help='FQDN of host with running report without 1st level domain')
    parser.addoption('-C', '--cgi', action='store', default='', help='Comma-separated list of additional CGI parameters')


@pytest.fixture(scope='session')
def betahost(request):
    return request.config.option.betahost

@pytest.fixture(scope='session')
def templatehost(request):
    return request.config.option.templatehost

@pytest.fixture(scope='session')
def cgi(request):
    return request.config.option.cgi
