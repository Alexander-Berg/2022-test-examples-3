import pytest
import logging
from pathlib import Path
from schema.schema import SchemaError

from conftest import get_configs, get_config_content
from market.sre.tools.market_alerts_configurator.lib.juggler_config import JugglerConfig


@pytest.mark.parametrize('filename', get_configs())
def test_schema(filename, conf_dir, config_schema, check_schema, args):
    data = get_config_content(Path(conf_dir, filename))

    # Test schema
    try:
        config_schema.validate(data)
        juggler_config = JugglerConfig(args, data.get('juggler'), create_api=False)
        juggler_config.make_checks()
        check_schema.validate(juggler_config.checks)

    except SchemaError:
        logging.error('Error in config %s', filename)
        raise

    # Test unique service for host
    uniq_services = set()
    for check in data['juggler']['checks']:
        if check.get('service') in uniq_services:
            raise AssertionError('Service {} duplicated in file {}'.format(check.get('service'), filename))
        uniq_services.add(check.get('service'))

    # Validate data
    for check in juggler_config.checks:
        # Test namespaces
        # assert check['namespace'] in juggler_namespaces, 'Bad namespace {} in file {}'.format(
        #     check['namespace'], filename)

        # Test flaps
        flaps = check.get('flaps')
        if flaps and flaps != 'default':
            assert 'critical_time' in flaps and 'stable_time' in flaps, \
                'Both "critical_time" and "stable_time" are required'

            if flaps['critical_time'] > 0:
                assert flaps['critical_time'] >= flaps['stable_time'], \
                    '"critical_time" must be >= than "stable_time".'

        # Test aggregator_kwargs
        if check.get('aggregator_kwargs'):
            aggr_kwargs = check['aggregator_kwargs']
            aggr = check['aggregator']

            if not aggr_kwargs.get('unreach_service'):
                assert 'unreach_mode' not in aggr_kwargs, \
                    '"unreach_mode" cannot be specified without "unreach_service"'
            else:
                assert aggr_kwargs.get('unreach_mode'), \
                    'If exists "unreach_service" must be "unreach_mode"'

            aggr_with_limits = 'timed_more_than_limit_is_problem'

            # Иногда люди по ошибке проставляют неправильный агрегатор расчитывая на
            # возможность указать limits в kwargs.
            # limits можно на самом деле проставлять только для
            # агрегатора timed_more_than_limit_is_problem

            if 'limits' in aggr_kwargs:
                assert aggr == aggr_with_limits, \
                    'Check where error is happened has name %s. ' % check['service'] + \
                    '"limits" may be used only in "%s" aggregator! ' % aggr_with_limits + \
                    'You are using the aggregator with name "%s" ' % aggr + \
                    'See documentation here: https://nda.ya.ru/t/Jk9RTUGV46FVqe'

            # kwargs for more_than_limit_is_problem only
            kwargs_mtlip = [x for x in aggr_kwargs if x in ['crit_limit', 'warn_limit', 'mode', 'show_ok']]
            aggr_mtlip = 'more_than_limit_is_problem'
            if kwargs_mtlip and aggr != aggr_mtlip:
                assert aggr == \
                    'Check where error is happened has name %s. ' % check['service'] + \
                    '%s may be used only in "%s" aggregator! ' % (kwargs_mtlip, aggr_mtlip) + \
                    'You are using the aggregator with name "%s" ' % aggr + \
                    'See documentation here: https://nda.ya.ru/t/Jk9RTUGV46FVqe'

            assert 'aggregator' in check, \
                'Aggregator_kwargs cannot be set if aggregator is None'

        # Test children
        if check.get('children'):
            assert 'aggregator' in check, \
                'Aggregator must be set when children are defined'

        if check.get('service') in ('logrotate', 'logrotate_app', 'push-client-status'):
            assert '--kwargs' not in check.get('check_options', {}).get('args', []), \
                '"--kwargs" applicable only for disk-free-space, ping and custom pings'

        mandatory_tags = ("market", "_market_")
        for tag in mandatory_tags:
            assert tag in check.get('tags'), \
                "The check must contain mandatory tags {}".format(", ".join(mandatory_tags))


def test_host_uniq(conf_dir):
    hosts = {}
    for filename in get_configs():
        data = get_config_content(Path(conf_dir, filename))

        host = data['juggler']['default']['host']
        if host not in hosts:
            hosts[host] = filename
        else:
            raise AssertionError('Host {} duplicated in files {} and {}'.format(host, hosts[host], filename))
