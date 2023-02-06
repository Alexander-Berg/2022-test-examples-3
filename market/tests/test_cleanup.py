import pytest
from market.sre.tools.market_alerts_configurator.lib.hosts_cleaner import HostsCleaner, TooManyHostsError


def test__get_hosts_to_remove(args):
    args.cleanup_tag = 'nonexistent_tag'
    managed_hosts = {'market.common': {'testhost'}}
    juggler_hosts = {'market.common': {'host1', 'testhost'}}
    cleaner = HostsCleaner(args, managed_hosts, create_api=False)
    cleaner._get_hosts_to_remove(managed_hosts, juggler_hosts)
    assert cleaner.hosts_to_remove == {'market.common': {'host1'}}


def test_TooManyHostsError(args):
    args.cleanup_tag = '_market_'
    managed_hosts = {'market.common': {'testhost'}}
    cleaner = HostsCleaner(args, managed_hosts, create_api=False)
    juggler_hosts = {'market.common': set(['host{}'.format(x) for x in range(cleaner.REMOVED_HOST_LIMIT + 1)])}
    with pytest.raises(TooManyHostsError):
        cleaner._get_hosts_to_remove(managed_hosts, juggler_hosts)
