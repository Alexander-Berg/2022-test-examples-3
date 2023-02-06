#!/usr/bin/perl

# Юнит тесты на MobileContent::ajax_mobile_content_info

use strict;
use utf8;
use warnings;

use Test::Deep;
use Test::More;

use MobileContent qw/ajax_mobile_content_info/;
use Settings;

use Yandex::DBUnitTest qw/:all/;
use Yandex::Test::UTF8Builder;

my $dataset = {
    mobile_content => {
        original_db => PPC(shard => 'all'),
        like => 'mobile_content',
        rows => {
            1 => [
                {
                    # iOS: empty, unalailable
                    mobile_content_id => 1,
                    ClientID => 111,
                    store_content_id => 'id794957821',
                    store_country => 'RU',
                    os_type => 'iOS',
                    content_type => 'app',
                    is_available => 0,
                    create_time => '2015-07-06 13:45:18',
                    modify_time => '0000-00-00 00:00:00',
                    store_refresh_time => '0000-00-00 00:00:00',
                    statusBsSynced => 'No',
                    statusIconModerate => 'Ready',
                    tries_count => 123,
                },
            ],
            2 => [
                {
                    # iOS: full, alailable
                    mobile_content_id => 2,
                    ClientID => 222,
                    store_content_id => 'id930794252',
                    store_country => 'RU',
                    os_type => 'iOS',
                    content_type => 'app',
                    bundle_id => 'com.fantasymanager.realmadrid5',
                    is_available => 1,
                    create_time => '2015-07-06 13:45:41',
                    modify_time => '2015-07-06 13:50:22',
                    store_refresh_time => '2015-07-06 13:50:22',
                    statusBsSynced => 'No',
                    name => 'Real Madrid Fantasy Manager 2015',
                    prices_json => '{"RU":{"download":{"price_currency":"RUB","price":"0"}},"UA":{"download":{"price_currency":"USD","price":"0"}},"KZ":{"download":{"price_currency":"USD","price":"0"}},"TR":{"download":{"price_currency":"TRY","price":"0"}},"BY":{"download":{"price_currency":"USD","price":"0"}}}',
                    rating => 4.50,
                    rating_votes => 15,
                    icon_hash => undef,
                    statusIconModerate => 'Ready',
                    min_os_version => '6.0',
                    app_size_bytes => 85109985,
                    available_actions => 'download',
                    publisher_domain_id => 2955386,
                    genre => 'Games',
                    age_label => '6+',
                    tries_count => 0,
                },
            ],
            4 => [
                {
                    # Android: full, alailable
                    mobile_content_id => 4,
                    ClientID => 444,
                    store_content_id => 'com.gameloft.android.ANMP.GloftSIHM',
                    store_country => 'RU',
                    os_type => 'Android',
                    content_type => 'app',
                    is_available => 1,
                    create_time => '2015-07-09 18:19:48',
                    modify_time => '2015-07-09 18:19:48',
                    store_refresh_time => '2015-07-09 18:19:48',
                    statusBsSynced => 'No',
                    name => 'Совершенный Человек-Паук',
                    prices_json => '{"RU":{"download":{"price":"0","price_currency":"USD"}},"TR":{"download":{"price":"0","price_currency":"USD"}},"US":{"download":{"price":"0","price_currency":"USD"}}}',
                    rating => 4.20,
                    rating_votes => 1274679,
                    icon_hash => '15229/com.gameloft.android.ANMP.GloftSIHM__184f6d747e6f23b8eb81722c2d8f95be',
                    statusIconModerate => 'Ready',
                    min_os_version => '4.0',
                    app_size_bytes => 25165824,
                    available_actions => 'download',
                    publisher_domain_id => 2947296,
                    genre => 'GAME_ACTION',
                    age_label => '12+',
                    tries_count => 0,
                },
            ],
        },
    },

    domains => {
        original_db => PPC(shard => 'all'),
        like => 'domains',
        rows => {
            1 =>[
                {domain_id =>694150, domain => 'www.miniclip.com', reverse_domain => 'moc.pilcinim.www'},
            ],
            2 =>[
                {domain_id =>2955386, domain => 'fantasymanager.com', reverse_domain => 'moc.reganamysatnaf'},
            ],
            4 =>[
                {domain_id =>2947296, domain => 'gameloft.com', reverse_domain => 'moc.tfolemag'},
            ],
        },
    },

    domains_dict => {
        original_db => PPCDICT,
        rows => [
            {domain_id => 694150, domain => 'www.miniclip.com'},
            {domain_id => 2955386, domain => 'fantasymanager.com'},
            {domain_id => 2947296, domain => 'gameloft.com'},
        ],
    },

    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            { ClientID => 111, shard => 1 },
            { ClientID => 222, shard => 2 },
            { ClientID => 444, shard => 4 },
        ],
    },
};
init_test_dataset($dataset);

my $correct_client_id2resps = {
    111 => {
        response => {
            os_type => 'iOS',
            rating_votes => 47,
            min_os_version => '7.0',
            prices => {
                download => {
                    price => 0,
                    price_currency => 'RUB'
                }
            },
            name => 'Bike Rivals',
            is_available => 1,
            content_type => 'app',
            age_label => '6+',
            is_default_country => 0,
            store_country => 'RU',
            available_actions => [
                'download'
            ],
            rating => 4,
            icon_url => '//avatars.mds.yandex.net/get-itunes-icon/28548/5eb3b23d730295636ac3811ac877137c/icon',
            is_show_icon => 1
        },
        error => undef,
    },
    222 => {
        error => undef,
        response => {
            rating_votes => 15,
            is_default_country => 0,
            min_os_version => '6.0',
            name => 'Real Madrid Fantasy Manager 2015',
            icon_url => undef,
            os_type => 'iOS',
            content_type => 'app',
            rating => '4.50',
            store_country => 'RU',
            is_show_icon => 0,
            mobile_content_id => 2,
            age_label => '6+',
            available_actions => [
                'download'
            ],
            prices => {
                download => {
                    price_currency => 'RUB',
                    price => 0
                }
            },
            is_available => 1
        }
    },
    444 => {
        response => {
            content_type => 'app',
            store_country => 'RU',
            min_os_version => '4.0',
            prices => {
                download => {
                  price_currency => 'USD',
                  price => 0
                }
            },
            is_available => 1,
            os_type => 'Android',
            name => 'Совершенный Человек-Паук',
            mobile_content_id => 4,
            rating_votes => 1274679,
            icon_url => '//avatars.mds.yandex.net/get-google-play-app-icon/15229/com.gameloft.android.ANMP.GloftSIHM__184f6d747e6f23b8eb81722c2d8f95be/icon',
            is_show_icon => 1,
            is_default_country => 1,
            available_actions => [
                'download'
            ],
            age_label => '12+',
            rating => '4.20'
        },
        error => undef,
    },
};

cmp_deeply( ajax_mobile_content_info('https://itunes.apple.com/ru/app/id930794252', 222), $correct_client_id2resps->{222}, 'normal run is available content - ok');

done_testing;
