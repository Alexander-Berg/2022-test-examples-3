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
                {cid => 44, uid => 4444},
                {cid => 45, uid => 4444},
                {cid => 46, uid => 4444},
            ],
            2 => [
                {cid => 54, uid => 555},
                {cid => 55, uid => 555},
                {cid => 71, uid => 7},
                {cid => 72, uid => 7},
            ],
        },
    },
    camp_options => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                {cid => 44, email => 'user4444@yandex-team.ru'},
                {cid => 45, email => 'not_confirmed@yandex-team.ru', valid => 0},
                {cid => 46, email => 'another_not_confirmed@yandex-team.ru', valid => 1},
            ],
            2 => [
                {cid => 54, email => 'not_confirmed@yandex-team.ru', valid => 0},
                {cid => 55, email => 'noreply@yandex-team.ru'},
                {cid => 71, email => 'ppalex-client@yandex.ru'},
                {cid => 72, email => 'ppalex@yandex-team.ru'},
            ],
        },
    },
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            {ClientID => 4, shard => 1},
            {ClientID => 5, shard => 2},
            {ClientID => 7, shard => 2},
        ],
    },
    shard_uid => {
        original_db => PPCDICT,
        rows => [
            {uid => 4444, ClientID => 4},
            {uid => 555, ClientID => 5},
            {uid => 7, ClientID => 7},
        ],
    },
);

init_test_dataset(\%db);

my @tests = (
    # [uids], result, test_name, reason test skipping
    [
        [4444],
        {4444 => ['user4444@yandex-team.ru']},
        'one uid, one e-mail, shard 1',
    ], [
        [555],
        {555 => ['noreply@yandex-team.ru']},
        'one uid, one e-mail, shard 2',
    ], [
        [555, 4444],
        {
            555 => ['noreply@yandex-team.ru'],
            4444 => ['user4444@yandex-team.ru'],
        },
        'two uid, two e-mails, both shards',
    ], [
        [7],
        {7 => bag('ppalex@yandex-team.ru', 'ppalex-client@yandex.ru')},
        'one uid, two-emails',
    ], [
        [100500],
        {},
        'one unknown uid',
        'requests with unknown UID are not produced from API',
        # Кажется, что из API (только там эта функция используется сама по себе) не должны приходить запросы на несуществующие (у нас) uid, потому что uid для запроса там - это результат выборки из PPC
    ], [
        [],
        {},
        'empty reuest array'
    ],

);

Test::More::plan(tests => 2 * @tests + 1);

foreach my $test (@tests) {
    SKIP: {
        skip $test->[3], 2 if $test->[3];

        my $data;
        lives_ok { $data = get_users_campaigns_emails($test->[0]); } "get data     " . $test->[2];
        cmp_deeply($data, $test->[1], "compare data " . $test->[2]);
    };
}

cmp_deeply(get_users_campaigns_emails([123456789]), {}, 'dont die on unknown users');
