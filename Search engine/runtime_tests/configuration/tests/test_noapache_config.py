# coding:utf-8
import socket

import pytest

OPTIONS = [
    'MetaSearchOptions',
    'TimeoutTable',
    'KeepAlive',
    'Compression',
    'NGroupsForSourceMultiplier',
    'EventLog',
    # SEARCHPRODINCIDENTS-2200
    # 'WizardPort',
    # 'WizardTimeout',
    # 'WizardTasksTimeouts',
    'HashValue',
    # 'RequestsToRandomize',
    # 'SelectWizardHostsPolicy',
    # 'RemoteWizardSimultaneousRequests',
    # 'RemoteWizards',
    'SchemeOptions',
    'ConnectTimeout',
    'RearrangeDataDir:',
    'RearrangeDynamicDataDir:',
    'RequestThreads',
    'RequestQueueSize']

CHECK_OPTIONS = {
    # 'ReArrangeOptions': lambda x: len(x) > 100,
    # 'CompiledInOptions': lambda x: len(x) > 20,
}

CHECK_RULE_WITH_PARAMETERS = {
    # 'Fresh': CheckFreshRule,
}

CHECK_OPTIONS_LOCATION = {
    # 'RemoteWizards': lambda x: x.find("(") >= 0,
}


@pytest.mark.noapache
def test_sources_ServerDescr(configuration):
    '''
    у всех опций должен быть указан ServerDescr
    '''
    errors = filter(lambda source: len(source['ServerDescr']) <= 1, configuration.SearchSources)
    assert len(errors) == 0, 'SourceSearch error! ServerDescr doesn\'t have data for %s' % errors


@pytest.mark.noapache
def test_sources_CgiSearchPrefix(configuration):
    '''
    у всех опций должен быть указан CgiSearchPrefix
    '''
    errors = filter(lambda source: len(source['CgiSearchPrefix']) <= 1, configuration.SearchSources)
    assert len(errors) == 0, 'SourceSearch error! CgiSearchPrefix doesn\'t have data for %s' % errors


@pytest.mark.noapache
def test_sources_Options(configuration):
    '''
    у всех опций должен быть указан Options
    '''
    errors = filter(lambda source: len(source['Options']) <= 1, configuration.SearchSources)
    assert len(errors) == 0, 'SourceSearch error! Options doesn\'t have data for %s' % errors


@pytest.mark.noapache
@pytest.mark.skipif(True,
                    reason="dns checks are fragile")
def test_dns_list(configuration):
    '''
    значения в DNSCache должны соответствовать записям в dns
    '''
    wrong_dns = filter(lambda (host, ip): ip not in [addr[4][0] for addr in socket.getaddrinfo(host, None)],
                       configuration.dns_list)
    assert len(wrong_dns) == 0, ['DNSCache error! Hostname doesn\'t match ip address! %s != %s' % (ip, host) for
                                 ip, host in wrong_dns]


@pytest.mark.noapache
def test_dns_ipv6_only(configuration):
    '''
    значения в DNSCache должны быть ipv6-only если локация ipv6-only
    '''
    if "itag_ipv6_only" not in configuration.tags:
        pytest.skip("this test for ipv6-only location")
    wrong_dns = filter(lambda (host, ip): not is_ipv6(ip), configuration.dns_list)
    assert len(wrong_dns) == 0, "illegal ipv6 address in itag_ipv6_only location {}".format(wrong_dns)


@pytest.mark.noapache
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
        for rule in CHECK_RULE_WITH_PARAMETERS:
            startPos = option_data.find(" " + rule + "(")
            assert (option_data.startswith(rule + "(") or startPos != -1), 'Option error! Rule %s not found' % rule
            if startPos < 0:
                startPos = 0
            indent = 1 + (0 if startPos == 0 else 1)
            CHECK_RULE_WITH_PARAMETERS[rule](option_data, startPos + len(rule) + indent)
    if option in CHECK_OPTIONS_LOCATION and (
            is_location_msk_not_inner_not_man(configuration.location)):
        assert CHECK_OPTIONS_LOCATION[option](option_data), 'Option error! %s data has errors for MSK location: %s' % (
            option, option_data)


@pytest.mark.noapache
def test_option_compiled_rearrange(configuration):
    '''
    должна присутствовать одна опция из
      CompiledInOptions
      ReArrangeOptions
    '''
    assert 'CompiledInOptions' in configuration.options.keys() or 'ReArrangeOptions' in configuration.options.keys(), \
        'Option error! Option "CompiledInOptions" or "ReArrangeOptions" should be exist'


@pytest.mark.noapache
def test_option_compiled_rearrange_data(configuration):
    '''
    Опции CompiledInOptions, ReArrangeOptions должны содержать данные (если они присутствуют)
    '''
    if 'CompiledInOptions' in configuration.options.keys():
        assert (
            len(configuration.options["CompiledInOptions"]) > 30,
            'Option error! CompiledInOptions data are too short'
        )
    elif 'ReArrangeOptions' in configuration.options.keys():
        assert len(configuration.options["ReArrangeOptions"]) > 30, 'Option error! ReArrangeOptions data are too short'
    else:
        pytest.skip("CompiledInOptions, ReArrangeOptions are absent")


@pytest.mark.noapache
@pytest.mark.parametrize(('option',), [(opt,) for opt in OPTIONS])
def test_option_exist(option, configuration):
    '''
    опция должна присутствовать
    '''
    assert option in configuration.options.keys(), 'Option error! Option %s is missed' % option


@pytest.mark.noapache
def test_hosts_in_dns(configuration):
    '''
    host источника должен быть в dns
    '''
    dns_list = [host for host, ip in configuration.dns_list]
    errored_hosts = filter(lambda host: host != 'localhost' and host not in dns_list, configuration.sources)
    assert len(errored_hosts) == 0, 'DNS Error! Source hosts %s not in DNSCache' % errored_hosts


@pytest.mark.noapache
@pytest.mark.parametrize('source', ['WEB', 'QUICK', 'VIDEOP', 'IMAGESP', ])
def test_source_exist(source, configuration):
    '''
    источник должен присутствовать
    '''
    if not is_location_msk_not_inner_not_man(configuration.location):
        pytest.skip("This shoud be MSK location and not INNER or MAN")
    if configuration.project_type == 'IMGS' and source in ['QUICK', 'VIDEOP', 'WEB']:
        pytest.skip("Skipping {} source check for IMGS project".format(source))
    assert source in [src['ServerDescr'] for src in
                      configuration.SearchSources], 'Source error! Source %s is missed' % source


@pytest.mark.noapache
@pytest.mark.depricated
@pytest.mark.parametrize('source', ['WEB', 'VIDEOP', 'IMAGESP', ])
def test_source_cgi_search_prefix(source, configuration):
    '''
    CgiSearchPrefix источника должен содержать скобочки
    '''
    if not is_location_msk_not_inner_not_man(configuration.location):
        pytest.skip("This shoud be MSK location and not INNER or MAN")
    sourceobj = filter(lambda src: src['ServerDescr'] == source, configuration.SearchSources)[0]
    assert "(" in sourceobj['CgiSearchPrefix'], "%s has no groupping by DC: %s" % (source, sourceobj['CgiSearchPrefix'])


def is_ipv6(ip):
    try:
        socket.inet_pton(socket.AF_INET6, ip)
    except Exception:
        return False
    return True


def is_location_msk_not_inner_not_man(source_data_location):
    return source_data_location[0] == "MSK" and (
        "INNER" not in source_data_location and "MAN" not in source_data_location)
