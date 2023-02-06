#!/usr/bin/perl

# $Id$

use warnings;
use strict;

use Test::More;
use Test::Deep;
use Test::Exception;

use Yandex::Test::UTF8Builder;
use Yandex::DBUnitTest qw/:all/;
use Yandex::DBTools;

use Settings;
use Retargeting;
use JSON qw//;

use Test::JavaIntapiMocks::GenerateObjectIds;

use utf8;

local $Yandex::DBShards::IDS_LOG_FILE = undef;

*save = *Retargeting::save_retargeting_condition;

sub to_json {
    my ($cond, $opts) = @_;
    for (map { @{$_->{goals}} } @$cond) {
        $_->{goal_id} = int($_->{goal_id});
        $_->{time} = int($_->{time});
    }
    return JSON::to_json($cond, $opts);
}

# Подготавливаем базу данных
my %db = (
    retargeting_conditions => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [],
            2 => [],
        },
    },
    retargeting_goals => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [],
            2 => [],
        },
    },
    users => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { uid => 1001190, ClientID => 1190 },
            ],
            2 => [
                { uid => 1006190, ClientID => 6190 },
            ],
        },
    },
    # заранее создаём кампанию, ссылающуюся на ret_cond_id = 4 aka $cond6
    campaigns => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [],
            2 => [
                { cid => 113, statusBsSynced => 'Yes', statusEmpty => 'No' },
                { cid => 200, statusBsSynced => 'Yes', statusEmpty => 'No' },
            ],
        },
    },
    phrases => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [],
            2 => [
                { pid => 111, bid => 112, cid => 113, statusBsSynced => 'Yes', LastChange => '2014-01-01 12:00:00' },
                { pid => 555, cid => 200, statusBsSynced => 'Yes', LastChange => '2014-01-01 12:00:00' },
                { pid => 600, cid => 200, statusBsSynced => 'No', LastChange => '2014-01-01 12:00:00' },
            ],
        },
    },
    bids_retargeting => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [],
            2 => [
                { pid => 111, bid => 112, ret_cond_id => 4, statusBsSynced => 'Yes' },
            ],
        },
    },
    bids_performance => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [],
            2 => [
                { pid => 555, perf_filter_id => 800, ret_cond_id => 3, is_deleted => 0, statusBsSynced => 'Yes', target_funnel => 'same_products' },
                { pid => 600, perf_filter_id => 560, ret_cond_id => undef, is_deleted => 0, price_cpc => 200, statusBsSynced => 'Yes', target_funnel => 'same_products' },
            ],
        }
    },
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            { ClientID =>  1190, shard => 1 },
            { ClientID =>  4669, shard => 1 },
            { ClientID =>  7083, shard => 1 },
            { ClientID =>  6190, shard => 2 },
            { ClientID => 11579, shard => 2 },
        ],
    },
    shard_uid => {
        original_db => PPCDICT,
        rows => [
            { uid => 1001190, ClientID => 1190 },
            { uid => 1006190, ClientID => 6190 },
        ],
    },
    shard_inc_ret_cond_id => {
        original_db => PPCDICT,
    },
    (map {$_ => {original_db => PPC(shard => 'all')}} qw/retargeting_multiplier_values hierarchical_multipliers banners/),
);
init_test_dataset(\%db);

dies_ok { save(cid => 300, {}) } 'dies on invalid type of id';
dies_ok { save(uid => 300, {}) } 'dies on unknown uid';

my $cond1 = {
    condition => [
        {
            goals => [
                {
                    goal_id => 4000183712,
                    time    => 1,
                },
            ],
            type => 'or',
        },
    ],
    condition_desc => 'desc1',
    condition_name => 'Посетил сайт',
};
my $cond2 = {
    condition => [
        {
            goals => [
                {
                    goal_id => 4000183712,
                    time    => 30,
                },
            ],
            type => 'or',
        },
    ],
    condition_desc => 'desc2',
    condition_name => 'Возврат',
};
my $test_dataset1 = {
    retargeting_conditions => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { ret_cond_id => 1, ClientID => 1190, retargeting_conditions_type => 'metrika_goals', condition_name => 'Посетил сайт', condition_desc => 'desc1', condition_json => to_json($cond1->{condition}, {canonical => 1}) },
                { ret_cond_id => 2, ClientID => 1190, retargeting_conditions_type => 'metrika_goals', condition_name => 'Возврат', condition_desc => 'desc2', condition_json => to_json($cond2->{condition}, {canonical => 1}) },
            ],
            2 => [],
        },
    },
    retargeting_goals => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { ret_cond_id => 1, goal_id => 4000183712, is_accessible => 1 },
                { ret_cond_id => 2, goal_id => 4000183712, is_accessible => 1 },
            ],
            2 => [],
        },
    },
    shard_inc_ret_cond_id => {
        original_db => PPCDICT,
        rows => [
            { ret_cond_id => 1, ClientID => 1190 },
            { ret_cond_id => 2, ClientID => 1190 },
        ],
    },
};
# use Data::Dumper;
# print STDERR Dumper($test_dataset1);
lives_ok { save(uid => 1001190, $cond1) } 'create condition in 1st shard by uid';
$cond1->{ret_cond_id} = 1;

lives_ok { save(ClientID => 1190, $cond2) } 'create condition in 1st shard by ClientID';
$cond2->{ret_cond_id} = 2;

check_test_dataset( $test_dataset1, 'check database data for dataset1');

$cond1->{condition_desc} = 'new long description';
$cond1->{condition_name} = 'new name';
lives_ok { save(ClientID => 1190, $cond1) } 'update condition';
$test_dataset1->{retargeting_conditions}->{rows}->{1}->[0]->{condition_name} = $cond1->{condition_name};
$test_dataset1->{retargeting_conditions}->{rows}->{1}->[0]->{condition_desc} = $cond1->{condition_desc};

check_test_dataset( $test_dataset1, 'check database data for dataset1 after save');

###########################################################
init_test_dataset(\%db);
my $cond3 = {
    condition => [
        {
            goals => [
                {
                    goal_id => 2175637,
                    time    => 2,
                },
            ],
            type => 'or',
        },
    ],
    condition_desc => '',
    condition_name => 'Человек просмотрел 7 страниц',
};
my $cond4 = {
    condition => [
        {
            goals => [
                {
                    goal_id => 2175637,
                    time    => 1,
                },
            ],
            type => 'or',
        },
    ],
    condition_desc => '',
    condition_name => 'Просмотр 7 страниц',
};
my $cond5 = {
    condition => [
        {
            goals => [
                {
                    goal_id => 2409802,
                    time    => 1,
                },
            ],
            type => 'or',
        }, {
            goals => [
                {
                    goal_id => 2409805,
                    time    => 1,
                },
            ],
            type => 'not',
        },
    ],
    condition_desc => '',
    condition_name => 'Ушел из корзины',
};
my $cond6 = {
    condition => [
        {
            goals => [
                {
                    goal_id => 2410477,
                    time    => 2,
                },
            ],
            type => 'or',
        }, {
            goals => [
                {
                    goal_id => 2409805,
                    time    => 2,
                },
            ],
            type => 'not',
        }, {
            goals => [
                {
                    goal_id => 2409808,
                    time    => 2,
                },
            ],
            type => 'not',
        },
    ],
    condition_desc => '',
    condition_name => 'Ретаргетинг на корзину',
};
my $cond7 = {
    condition => [
        {
            goals => [
                {
                    goal_id => 2409805,
                    time    => 2,
                },
            ],
            type => 'or',
        }, {
            goals => [
                {
                    goal_id => 2409808,
                    time    => 2,
                },
            ],
            type => 'not',
        },
    ],
    condition_desc => '',
    condition_name => 'Отказ - форма контактов',
};
my $cond8 = {
    condition => [
        {
            goals => [
                {
                    goal_id => 2409805,
                    time    => 2,
                },
            ],
            type => 'or',
        }, {
            goals => [
                {
                    goal_id => 2_000_000_005,
                    time    => 90,
                },
            ],
            type => 'not',
        },
        {
            goals => [
                {
                    goal_id => 2_000_000_008,
                    time    => 90,
                },
            ],
            type => 'or',
        }
    ],
    condition_desc => '',
    condition_name => 'Я Аудитории',
};
my $test_dataset2 = {
    retargeting_conditions => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [],
            2 => [
                { ret_cond_id => 1, ClientID => 6190, retargeting_conditions_type => 'metrika_goals', condition_name => 'Человек просмотрел 7 страниц', condition_desc => '', condition_json => to_json($cond3->{condition}, {canonical => 1}) },
                { ret_cond_id => 2, ClientID => 6190, retargeting_conditions_type => 'metrika_goals', condition_name => 'Просмотр 7 страниц', condition_desc => '', condition_json => to_json($cond4->{condition}, {canonical => 1}) },
                { ret_cond_id => 3, ClientID => 6190, retargeting_conditions_type => 'metrika_goals', condition_name => 'Ушел из корзины', condition_desc => '', condition_json => to_json($cond5->{condition}, {canonical => 1}) },
                { ret_cond_id => 4, ClientID => 6190, retargeting_conditions_type => 'metrika_goals', condition_name => 'Ретаргетинг на корзину', condition_desc => '', condition_json => to_json($cond6->{condition}, {canonical => 1}) },
                { ret_cond_id => 5, ClientID => 6190, retargeting_conditions_type => 'metrika_goals', condition_name => 'Отказ - форма контактов', condition_desc => '', condition_json => to_json($cond7->{condition}, {canonical => 1}) },
                { ret_cond_id => 6, ClientID => 6190, retargeting_conditions_type => 'metrika_goals', condition_name => 'Я Аудитории', condition_desc => '', condition_json => to_json($cond8->{condition}, {canonical => 1}) },
            ],
        },
    },
    retargeting_goals => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [],
            2 => [
                { ret_cond_id => 1, goal_id => 2175637, is_accessible => 1 },
                { ret_cond_id => 2, goal_id => 2175637, is_accessible => 1 },
                { ret_cond_id => 3, goal_id => 2409802, is_accessible => 1 },
                { ret_cond_id => 3, goal_id => 2409805, is_accessible => 1 },
                { ret_cond_id => 4, goal_id => 2410477, is_accessible => 1 },
                { ret_cond_id => 4, goal_id => 2409805, is_accessible => 1 },
                { ret_cond_id => 4, goal_id => 2409808, is_accessible => 1 },
                { ret_cond_id => 5, goal_id => 2409805, is_accessible => 1 },
                { ret_cond_id => 5, goal_id => 2409808, is_accessible => 1 },
                { ret_cond_id => 6, goal_id => 2409805, is_accessible => 1 },
                { ret_cond_id => 6, goal_id => 2_000_000_005, is_accessible => 1 },
                { ret_cond_id => 6, goal_id => 2_000_000_008, is_accessible => 1 },
            ],
        },
    },
    shard_inc_ret_cond_id => {
        original_db => PPCDICT,
        rows => [
            { ret_cond_id => 1, ClientID => 6190 },
            { ret_cond_id => 2, ClientID => 6190 },
            { ret_cond_id => 3, ClientID => 6190 },
            { ret_cond_id => 4, ClientID => 6190 },
            { ret_cond_id => 5, ClientID => 6190 },
            { ret_cond_id => 6, ClientID => 6190 },
        ],
    },
};

lives_ok {
    save(uid => 1006190, $cond3);
    save(uid => 1006190, $cond4);
} 'create condition in 2nd shard by uid';
$cond3->{ret_cond_id} = 1;
$cond4->{ret_cond_id} = 2;

lives_ok {
    save(ClientID => 6190, $cond5);
    save(ClientID => 6190, $cond6);
    save(ClientID => 6190, $cond7);
    save(ClientID => 6190, $cond8);
} 'create condition in 2nd shard by ClientID';
$cond5->{ret_cond_id} = 3;
$cond6->{ret_cond_id} = 4;
$cond7->{ret_cond_id} = 5;
$cond8->{ret_cond_id} = 6;

check_test_dataset( $test_dataset2, 'check database data for dataset2');

# обновляем описание ретаргетинга и меняем одну из целей
$cond5->{condition_desc} = 'new long description';
$cond5->{condition_name} = 'new name';
$cond5->{condition}->[0]->{goals}->[0]->{time} = 5;
$cond5->{condition}->[0]->{goals}->[0]->{goal_id} = 2409808;
$cond5->{condition}->[1]->{goals}->[0]->{time} = 10;

lives_ok { save(ClientID => 6190, $cond5) } 'update condition (with goals)';
# обновляем условие
$test_dataset2->{retargeting_conditions}->{rows}->{2}->[2]->{condition_name} = $cond5->{condition_name};
$test_dataset2->{retargeting_conditions}->{rows}->{2}->[2]->{condition_desc} = $cond5->{condition_desc};
$test_dataset2->{retargeting_conditions}->{rows}->{2}->[2]->{condition_json} = to_json($cond5->{condition}, {canonical => 1});

# заменяем цель
$test_dataset2->{retargeting_goals}->{rows}->{2}->[2]->{goal_id} = $cond5->{condition}->[0]->{goals}->[0]->{goal_id};

check_test_dataset( $test_dataset2, 'check database data for dataset2 after save');

# удаляем часть целей из условия
$cond6->{condition} = [ $cond6->{condition}->[0] ];
# обновляем условие
$test_dataset2->{retargeting_conditions}->{rows}->{2}->[3]->{condition_json} = to_json($cond6->{condition}, {canonical => 1});
# удаляем цели
$test_dataset2->{retargeting_goals}->{rows}->{2} = [
    grep {
        $_->{ret_cond_id} != $cond6->{ret_cond_id}
        || $_->{goal_id} == $cond6->{condition}->[0]->{goals}->[0]->{goal_id}
    } @{ $test_dataset2->{retargeting_goals}->{rows}->{2} }
];

# statusBsSynced и LastChange группы должен скидываться при изменении входящих в неё условий ретаргетинга
$test_dataset2->{phrases} = $db{phrases};
$test_dataset2->{phrases}->{rows}->{2}->[0]->{statusBsSynced} = 'No';
$test_dataset2->{phrases}->{rows}->{2}->[1]->{statusBsSynced} = 'No';

lives_ok { save(ClientID => 6190, $cond6) } 'update condition (delete some goals)';

# Не можем проверить что LastChange поменялся силами change_test_dataset
delete $_->{LastChange} for @{$test_dataset2->{phrases}->{rows}->{2}};

my $last_change = get_one_field_sql(PPC(shard => 2), ["select UNIX_TIMESTAMP(LastChange) from phrases", where => { pid => 111 }]);
cmp_ok(abs($last_change - time()), '<=', 2, "check LastChange within small amount of seconds from now after second save");

check_test_dataset( $test_dataset2, 'check database data for dataset2 after second save');


$cond8->{condition}->[1]->{goals}->[0]->{time} = 30;
delete $cond8->{condition}->[2]->{goals}->[0]->{time};

lives_ok { save(ClientID => 6190, $cond8) } 'update condition (with audience)';
check_test_dataset( $test_dataset2, "audience without time don't change condition");

$cond8->{condition}->[0]->{goals}->[1] = {goal_id => 2_000_100_008};
lives_ok { save(ClientID => 6190, $cond8) } 'update condition (with audience)';

$cond8->{condition}->[0]->{goals}->[1]->{time} = 90;
$test_dataset2->{retargeting_conditions}->{rows}->{2}->[5]->{condition_json} = to_json($cond8->{condition}, {canonical => 1});
push @{$test_dataset2->{retargeting_goals}->{rows}->{2}}, {goal_id => 2_000_100_008};
check_test_dataset( $test_dataset2, "audience without time don't change condition");




done_testing();
