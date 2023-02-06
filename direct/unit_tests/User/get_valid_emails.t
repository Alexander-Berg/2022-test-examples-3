#!/usr/bin/perl

=pod
    $Id$
=cut

use strict;
use warnings;

use Test::More;
use Test::Deep;
use Test::Exception;

# use Data::Dumper;

use Settings;
use Yandex::DBUnitTest qw/:all/;
use Yandex::DBTools;

use User;
# use Primitives;

use utf8;

$Yandex::DBTools::DONT_SEND_LETTERS = 1;

my %db = (
    campaigns => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                {cid => 33, uid => 3333},
                {cid => 44, uid => 4444},
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
                {cid => 33, email => 'confirmed@yandex-team.ru', valid => 2},
                {cid => 44, email => 'user4444@yandex-team.ru'},
            ],
            2 => [
                {cid => 54, email => 'not_confirmed@yandex-team.ru', valid => 0},
                {cid => 55, email => 'noreply@yandex-team.ru'},
                {cid => 71, email => 'ppalex-client@yandex.ru'},
                {cid => 72, email => 'ppalex@yandex-team.ru'},
            ],
        },
    },
    users => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                {uid => 33, email => ''},
            ],
            2 => [
                {uid => 33, email => ''},
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
            {uid => 3333, ClientID => 3},
            {uid => 555, ClientID => 5},
            {uid => 7, ClientID => 7},
        ],
    },
);

init_test_dataset(\%db);

my @tests = (
    # uid, email, result, test_name
    [
        4444,
        undef,
        bag({email => 'user4444@yandex-team.ru', select => ''}),
        'shard 1: user4444@yandex-team.ru - w/o select',
    ], [
        4444,
        'user4444@yandex-team.ru',
        bag({email => 'user4444@yandex-team.ru', select => 'SELECTED'}),
        'shard 1: user4444@yandex-team.ru - selected',
    ], [
        4444,
        'selected@yandex-team.ru',
        bag(
            {email => 'user4444@yandex-team.ru', select => ''},
            {email => 'selected@yandex-team.ru', select => 'SELECTED'},
        ),
        'shard 1: user4444@yandex-team.ru - w/o select; selected@yandex-team.ru - selected',
    ], [
        555,
        undef,
        bag({email => 'noreply@yandex-team.ru', select => ''}),
        'shard 2: noreply@yandex-team.ru - w/o select',
    ], [
        555,
        'noreply@yandex-team.ru',
        bag({email => 'noreply@yandex-team.ru', select => 'SELECTED'}),
        'shard 2: noreply@yandex-team.ru - selected',
    ], [
        555,
        'selected@yandex-team.ru',
        bag(
            {email => 'selected@yandex-team.ru', select => 'SELECTED'},
            {email => 'noreply@yandex-team.ru', select => ''},
        ),
        'shard 2: noreply@yandex-team.ru - w/o select; selected@yandex-team.ru - selected',
    ], [
        7,
        undef,
        bag(
            {email => 'ppalex@yandex-team.ru', select => ''},
            {email => 'ppalex-client@yandex.ru', select => ''},
        ),
        'shard 2: ppalex@yandex-team.ru - w/o select; ppalex-client@yandex.ru - w/o select',
    ], [
        7,
        'ppalex@yandex-team.ru',
        bag(
            {email => 'ppalex@yandex-team.ru', select => 'SELECTED'},
            {email => 'ppalex-client@yandex.ru', select => ''},
        ),
        'shard 2: ppalex@yandex-team.ru - selected; ppalex-client@yandex.ru - w/o select',
    ], [
        7,
        'ppalex-client@yandex.ru',
        bag(
            {email => 'ppalex@yandex-team.ru', select => ''},
            {email => 'ppalex-client@yandex.ru', select => 'SELECTED'},
        ),
        'shard 2: ppalex@yandex-team.ru - w/o select; ppalex-client@yandex.ru - selected',
    ], [
        7,
        'selected@yandex-team.ru',
        bag(
            {email => 'ppalex@yandex-team.ru', select => ''},
            {email => 'ppalex-client@yandex.ru', select => ''},
            {email => 'selected@yandex-team.ru', select => 'SELECTED'},
        ),
        'shard 2: ppalex@yandex-team.ru - w/o select; ppalex-client@yandex.ru - w/o select, selected@yandex-team.ru - selected',
    ], [
        # По мотивам DIRECT-23420 - для новых пользователей попытка получить (чтобы подставить в новую, создаваемую кампанию) e-mail завершается ошибкой, т.к. мы еще ничего не знаем про данный uid
        100500,
        undef,
        [],
        'one unknown uid w/o select - result is empty set'
    ], [
        100500,
        'selected@yandex-team.ru',
        bag({email => 'selected@yandex-team.ru', select => 'SELECTED'}),
        'one unknown uid with select - selected@yandex-team.ru - selected',
    ],
);

Test::More::plan(tests => 2 * @tests);

foreach my $test (@tests) {
    my $data;
    lives_ok { $data = get_valid_emails($test->[0], $test->[1]); } "get data    \t" . $test->[3];
    cmp_deeply($data, $test->[2], "compare data\t" . $test->[3]);
}
