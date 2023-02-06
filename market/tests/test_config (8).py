# coding: utf8
from market.idx.datacamp.system_offers.lib.config import load_config
from six.moves.configparser import ConfigParser


def create_ini():
    config = ConfigParser()

    config.add_section('datacamp')
    config.set('datacamp', 'host', 'www.www')
    config.add_section('report')
    config.set('report', 'host', 'www.www')
    config.add_section('saas')
    config.set('saas', 'host', 'www.www')

    config.add_section('check_mining')
    config.set('check_mining', 'warning_timeout', '10')

    config.add_section('feed_test_p')
    config.set('feed_test_p', 'enabled', 'true')
    config.set('feed_test_p', 'business_id', '1')
    config.set('feed_test_p', 'feed_id', '2')
    config.set('feed_test_p', 'check_mining_id', '03')
    config.set('feed_test_p', 'feed_file_name', 'feed_file_name')

    config.add_section('feed_test_p2')
    config.set('feed_test_p', 'business_id', '1')
    config.set('feed_test_p', 'feed_id', '2')
    config.set('feed_test_p', 'check_mining_id', '03')
    config.set('feed_test_p', 'feed_file_name', 'feed_file_name')

    with open('test.ini', 'w') as configfile:
        config.write(configfile)


def test_config():
    create_ini()
    config = load_config('test', '.', '.')

    test_partner = config.feed_partners['test_p']
    assert test_partner.checks['check_mining'] == '03'
    assert test_partner.identifiers['feed_id'] == 2
    assert test_partner.feed_file_name == 'feed_file_name'

    assert 'test_p2' not in config.feed_partners
