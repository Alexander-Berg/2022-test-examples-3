# -*- coding: utf-8 -*-
import socket

import pytest


OPTIONS_DYNAMIC = [
    'FileNameWithPid'
]

OPTIONS = ['MetaSearchOptions',
           'TimeoutTable',
           'KeepAlive',
           'Compression',
           'NGroupsForSourceMultiplier',
           'SimReqDaemon',
           'ResInfoDaemon',
           'EventLog',
           'WizardPort',
           'WizardTimeout',
           'WizardTasksTimeouts',
           'HashValue',
           'RequestsToRandomize',
           'SelectWizardHostsPolicy',
           'RemoteWizardSimultaneousRequests',
           'RemoteWizards',
           'SchemeOptions',
           'ConnectTimeout',
           'CgiSearchPrefix:',
           'RearrangeDataDir:',
           'RearrangeDynamicDataDir:',
           'RequestThreads',
           'RequestQueueSize']

CHECK_OPTIONS = {

}

CHECK_OPTIONS_LOCATION = {
    # 'RemoteWizards': lambda x: x.find("(") >= 0,
}


@pytest.mark.upper
@pytest.mark.config
def test_sources_ServerDescr(configuration):
    '''
    у всех опций должен быть указан ServerDescr
    '''
    errors = filter(lambda source: len(source['ServerDescr']) <= 1, configuration.SearchSources)
    assert len(errors) == 0, 'SourceSearch error! ServerDescr doesn\'t have data for %s' % errors


@pytest.mark.upper
@pytest.mark.config
def test_sources_CgiSearchPrefix(configuration):
    '''
    у всех опций должен быть указан CgiSearchPrefix
    '''
    errors = filter(lambda source: len(source['CgiSearchPrefix']) <= 1, configuration.SearchSources)
    assert len(errors) == 0, 'SourceSearch error! CgiSearchPrefix doesn\'t have data for %s' % errors


@pytest.mark.upper
@pytest.mark.config
def test_sources_Options(configuration):
    '''
    у всех опций должен быть указан Options
    '''
    errors = filter(lambda source: len(source['Options']) <= 1, configuration.SearchSources)
    assert len(errors) == 0, 'SourceSearch error! Options doesn\'t have data for %s' % errors


@pytest.mark.upper
@pytest.mark.skipif(True,
                    reason="dns checks are fragile")
def test_dns_list(configuration):
    '''
    значения в DNSCache должны соответствовать записям в dns
    '''
    wrong_dns = filter(lambda (host, ip ): ip not in [addr[4][0] for addr in socket.getaddrinfo(host, None)],
                       configuration.dns_list)
    assert len(wrong_dns) == 0, ['DNSCache error! Hostname doesn\'t match ip address! %s != %s' % (ip, host) for
                                 ip, host in wrong_dns]

@pytest.mark.upper
def test_dns_ipv6_only(configuration):
    '''
    значения в DNSCache должны быть ipv6-only если локация ipv6-only
    '''
    if "itag_ipv6_only" not in configuration.tags: pytest.skip("this test for ipv6-only location")
    wrong_dns = filter(lambda (host, ip ): not is_ipv6(ip), configuration.dns_list)
    assert len(wrong_dns) == 0, "illegal ipv6 address in itag_ipv6_only location {}".format(wrong_dns)


@pytest.mark.upper
@pytest.mark.config
@pytest.mark.parametrize('option', OPTIONS)
def test_option_data(option, configuration):
    '''
    значение опции должно быть не пустым, и удовлетворять заданным проверкам.
    '''
    option_data = configuration.options[option]
    assert len(option_data.strip()) != 0 and option_data.strip() != '()', \
        'Option error! Option %s doesn\'t have any data' % option
    if option in CHECK_OPTIONS:
        assert CHECK_OPTIONS[option](option_data), 'Option error! %s data has errors: %s' % (option, option_data)
    if option in CHECK_OPTIONS_LOCATION and (
            is_location_msk_not_inner_not_man(configuration.location)):
        assert CHECK_OPTIONS_LOCATION[option](option_data), 'Option error! %s data has errors for MSK location: %s' % (
            option, option_data)


@pytest.mark.dynamic
@pytest.mark.upper
@pytest.mark.config
@pytest.mark.parametrize('option', OPTIONS_DYNAMIC)
def test_option_data_dynamic(option, configuration):
    '''
    значение опции должно быть не пустым, и удовлетворять заданным проверкам.
    '''
    option_data = configuration.options[option]
    assert len(option_data.strip()) != 0 and option_data.strip() != '()', \
        'Option error! Option %s doesn\'t have any data' % option
    if option in CHECK_OPTIONS:
        assert CHECK_OPTIONS[option](option_data), 'Option error! %s data has errors: %s' % (option, option_data)
    if option in CHECK_OPTIONS_LOCATION and (
            is_location_msk_not_inner_not_man(configuration.location)):
        assert CHECK_OPTIONS_LOCATION[option](option_data), 'Option error! %s data has errors for MSK location: %s' % (
            option, option_data)


@pytest.mark.upper
@pytest.mark.config
@pytest.mark.parametrize('option', OPTIONS)
def test_option_exist(option, configuration):
    '''
    опция должна присутствовать
    '''
    assert option in configuration.options.keys(), 'Option error! Option %s is missed' % option


@pytest.mark.dynamic
@pytest.mark.upper
@pytest.mark.config
@pytest.mark.parametrize('option', OPTIONS_DYNAMIC)
def test_option_exist_dynamic(option, configuration):
    '''
    опция должна присутствовать
    '''
    assert option in configuration.options.keys(), 'Option error! Option %s is missed' % option


@pytest.mark.upper
@pytest.mark.config
def test_hosts_in_dns(configuration):
    '''
    host источника должен быть в dns
    '''
    dns_list = [host for host, ip in configuration.dns_list]
    errored_hosts = filter(lambda host: host != 'localhost' and host not in dns_list, configuration.sources)
    assert len(errored_hosts) == 0, 'DNS Error! Source hosts %s not in DNSCache' % errored_hosts


def is_ipv6(ip):
    try:
        socket.inet_pton(socket.AF_INET6, ip)
    except:
        return False
    return True


def is_location_msk_not_inner_not_man(source_data_location):
    return source_data_location[0] == "MSK" and (
        "INNER" not in source_data_location and "MAN" not in source_data_location)
