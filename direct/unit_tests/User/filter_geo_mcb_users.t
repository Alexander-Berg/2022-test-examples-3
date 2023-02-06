#!/usr/bin/perl

=pod
    $Id$
=cut

use strict;
use warnings;

use Test::More;
use Test::Deep;
use Test::Exception;

use Settings;
use Yandex::DBUnitTest qw/:all/;
use Yandex::DBTools;

use User;

use utf8;

$Yandex::DBTools::DONT_SEND_LETTERS = 1;

my %db = (
    campaigns => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                {cid => 101, uid => 1001, type => 'mcb'},
                {cid => 102, uid => 1002, type => 'text'},
                {cid => 103, uid => 1003, type => 'geo'},
                {cid => 104, uid => 1004, type => 'geo'},
                {cid => 105, uid => 1004, type => 'text'},
                {cid => 106, uid => 1005, type => 'text'},
                {cid => 107, uid => 1005, type => 'geo'},
                {cid => 108, uid => 1005, type => 'mcb'},
            ],
            2 => [
                {cid => 201, uid => 2001, type => 'mcb'},
                {cid => 202, uid => 2002, type => 'text'},
                {cid => 203, uid => 2003, type => 'geo'},
                {cid => 204, uid => 2004, type => 'text'},
                {cid => 205, uid => 2004, type => 'geo'},
                {cid => 206, uid => 2005, type => 'geo'},
                {cid => 207, uid => 2005, type => 'text'},
                {cid => 208, uid => 2005, type => 'geo'},
                {cid => 209, uid => 2006, type => 'geo'},
                {cid => 210, uid => 2006, type => 'geo'},
            ],
        },
    },
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            {ClientID => 11, shard => 1},
            {ClientID => 12, shard => 1},
            {ClientID => 13, shard => 1},
            {ClientID => 14, shard => 1},
            {ClientID => 15, shard => 1},
            {ClientID => 21, shard => 2},
            {ClientID => 22, shard => 2},
            {ClientID => 23, shard => 2},
            {ClientID => 24, shard => 2},
            {ClientID => 25, shard => 2},
            {ClientID => 26, shard => 2},
        ],
    },
    shard_uid => {
        original_db => PPCDICT,
        rows => [
            {uid => 1001, ClientID => 11},  # 1 media
            {uid => 1002, ClientID => 12},  # 1 direct 
            {uid => 1003, ClientID => 13},  # 1 geo
            {uid => 1004, ClientID => 14},  # 1 geo 1 direct
            {uid => 1005, ClientID => 15},  # 1 geo 1 direct 1 mcb
            {uid => 1006, ClientID => 16},  # none
            {uid => 2001, ClientID => 21},  # 1 media
            {uid => 2002, ClientID => 22},  # 1 direct
            {uid => 2003, ClientID => 23},  # 1 geo
            {uid => 2004, ClientID => 24},  # 1 geo 1 direct
            {uid => 2005, ClientID => 25},  # 2 geo 1 direct
            {uid => 2006, ClientID => 26},  # 2 geo
        ],
    },
);

init_test_dataset(\%db);

my @tests = (
    # [uids], [result], test_name
    [
        [1001, 1002],
        [1002],
        'direct user :1',
    ], [
        [2001, 2002],
        [2002],
        'direct user :2',
    ], [
        [1003, 2003],
        [],
        'geo users',
    ], [
        [1001, 1003, 2002],
        [2002],
        'geo and direct users',
    ], [
        [1001, 1003, 2002],
        [2002],
        'users with direct and geo camps',
    ], [
        [1004, 2004],
        bag(1004, 2004),
        'users with 1 geo and 1 direct camp'
    ], [
        [1005, 2005],
        bag(1005, 2005),
        'users with 2 direct|1 geo and 1 direct|2 geo camps'
    ], [
        [1001..1005, 2001..2006],
        bag(1002, 1004, 1005, 2002, 2004, 2005),
        'all previous users + one with two geo camps'
    ], [
        [],
        [],
        'empty array to filtering'
    ], [
        [1006],
        [1006],
        'user with no camps'
    ], [
        [123456789, 987654321],
        [123456789, 987654321],
        q/doesn't filter anything on non existent users/,
        'requests with unknown UID are not produced from API',
        # Кажется из API эта функция применяется только на "наших" данных
    ],

);

Test::More::plan(tests => 2 * @tests + 1);

foreach my $test (@tests) {
    SKIP: {
        skip $test->[3], 2 if $test->[3];

        my $data;
        lives_ok { $data = filter_geo_mcb_users($test->[0]); } "get data     " . $test->[2];
        cmp_deeply($data, $test->[1], "compare data " . $test->[2]);
    }
}

cmp_deeply(filter_geo_mcb_users([123456789]), [123456789], "not filter unknown users");
