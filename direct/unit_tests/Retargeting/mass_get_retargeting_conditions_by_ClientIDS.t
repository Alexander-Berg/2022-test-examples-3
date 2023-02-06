#!/usr/bin/perl

# $Id$

use warnings;
use strict;

use Test::More;
use Test::Deep;

use Yandex::Test::UTF8Builder;
use Yandex::DBUnitTest qw/:all/;
use Settings;
use Retargeting;

use utf8;

*g = *Retargeting::mass_get_retargeting_conditions_by_ClientIDS;

my %db = (
    retargeting_conditions => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { ret_cond_id => 6197, ClientID => 1190, retargeting_conditions_type => 'metrika_goals', condition_name => 'Посетил сайт', condition_json => '[{"type":"or","goals":[{"goal_id":"4000183712","goal_type":"goal","time":"1"}]}]' },
                { ret_cond_id => 44251, ClientID => 1190, retargeting_conditions_type => 'metrika_goals', condition_name => 'Возврат', condition_json => '[{"type":"or","goals":[{"goal_id":"4000183712","goal_type":"goal","time":"30"}]}]' },
                { ret_cond_id => 46828, ClientID => 4669, retargeting_conditions_type => 'metrika_goals', condition_name => 'Цель', condition_json => '[{"type":"or","goals":[{"goal_id":"4020690383","goal_type":"goal","time":"30"}]}]' },
                { ret_cond_id => 3621, ClientID => 7083, retargeting_conditions_type => 'metrika_goals', condition_name => 'Biz', condition_json => '[{"type":"or","goals":[{"goal_id":"4020818681","goal_type":"goal","time":"1"}]}]' },
                { ret_cond_id => 3622, ClientID => 7083, retargeting_conditions_type => 'metrika_goals', condition_name => 'nic.ru', condition_json => '[{"type":"or","goals":[{"goal_id":"4000040708","goal_type":"goal","time":"1"}]}]' },
            ],
            2 => [
                { ret_cond_id => 46236, ClientID => 6190, retargeting_conditions_type => 'metrika_goals', condition_name => 'Паллетные борта', condition_json => '[{"type":"or","goals":[{"goal_id":"4022072738","goal_type":"goal","time":"30"}]}]' },
                { ret_cond_id => 5273, ClientID => 11579, retargeting_conditions_type => 'metrika_goals', condition_name => 'Человек просмотрел 7 страниц', condition_json => '[{"type":"or","goals":[{"goal_id":"2175637","goal_type":"goal","time":"2"}]}]' },
                { ret_cond_id => 5276, ClientID => 11579, retargeting_conditions_type => 'metrika_goals', condition_name => 'Просмотр 7 страниц', condition_json => '[{"type":"or","goals":[{"goal_id":"2175637","goal_type":"goal","time":"1"}]}]' },
                { ret_cond_id => 6058, ClientID => 11579, retargeting_conditions_type => 'metrika_goals', condition_name => 'Ушел из корзины', condition_json => '[{"type":"or","goals":[{"goal_id":"2409802","goal_type":"goal","time":"1"}]},{"type":"not","goals":[{"goal_id":"2409805","goal_type":"goal","time":"1"}]}]' },
                { ret_cond_id => 6977, ClientID => 11579, retargeting_conditions_type => 'metrika_goals', condition_name => 'Ретаргетинг на корзину', condition_json => '[{"type":"or","goals":[{"goal_id":"2410477","goal_type":"goal","time":"2"}]},{"type":"not","goals":[{"goal_id":"2409805","goal_type":"goal","time":"2"}]},{"type":"not","goals":[{"goal_id":"2409808","goal_type":"goal","time":"2"}]}]' },
                { ret_cond_id => 7053, ClientID => 11579, retargeting_conditions_type => 'metrika_goals', condition_name => 'Отказ - форма контактов', condition_json => '[{"type":"or","goals":[{"goal_id":"2409805","goal_type":"goal","time":"2"}]},{"type":"not","goals":[{"goal_id":"2409808","goal_type":"goal","time":"2"}]}]' },
            ],
        },
    },
    retargeting_goals => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [
                { ret_cond_id =>  3621, goal_id => 4020818681, is_accessible => 0 },
                { ret_cond_id =>  3622, goal_id => 4000040708, is_accessible => 1 },
                { ret_cond_id =>  6197, goal_id => 4000183712, is_accessible => 1 },
                { ret_cond_id => 44251, goal_id => 4000183712, is_accessible => 0 },
                { ret_cond_id => 46828, goal_id => 4020690383, is_accessible => 1 },
            ],
            2 => [
                { ret_cond_id =>  5273, goal_id =>    2175637, is_accessible => 1 },
                { ret_cond_id =>  5276, goal_id =>    2175637, is_accessible => 0 },
                { ret_cond_id =>  6058, goal_id =>    2409802, is_accessible => 1 },
                { ret_cond_id =>  6058, goal_id =>    2409805, is_accessible => 1 },
                { ret_cond_id =>  6977, goal_id =>    2409805, is_accessible => 1 },
                { ret_cond_id =>  6977, goal_id =>    2409808, is_accessible => 0 },
                { ret_cond_id =>  6977, goal_id =>    2410477, is_accessible => 1 },
                { ret_cond_id =>  7053, goal_id =>    2409805, is_accessible => 1 },
                { ret_cond_id =>  7053, goal_id =>    2409808, is_accessible => 1 },
                { ret_cond_id => 46236, goal_id => 4022072738, is_accessible => 0 },
            ],
        },
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
    shard_inc_ret_cond_id => {
        original_db => PPCDICT,
        rows => [
            { ret_cond_id =>  6197, ClientID =>  1190 },
            { ret_cond_id => 44251, ClientID =>  1190 },
            { ret_cond_id => 46828, ClientID =>  4669 },
            { ret_cond_id =>  3621, ClientID =>  7083 },
            { ret_cond_id =>  3622, ClientID =>  7083 },
            { ret_cond_id => 46236, ClientID =>  6190 },
            { ret_cond_id =>  5273, ClientID => 11579 },
            { ret_cond_id =>  5276, ClientID => 11579 },
            { ret_cond_id =>  6058, ClientID => 11579 },
            { ret_cond_id =>  6977, ClientID => 11579 },
            { ret_cond_id =>  7053, ClientID => 11579 },
        ],
    },
);
init_test_dataset(\%db);

my $test_dataset1 = {
    4669 => {
        46828 => {
            ClientID => 4669,
            condition => [
                {
                    goals => [
                        {
                            goal_id => 4020690383,
                            goal_type => "goal",
                            time    => 30,
                        },
                    ],
                    type => 'or',
                },
            ],
            condition_desc => undef, 
            retargeting_conditions_type => 'metrika_goals',
            condition_name => 'Цель',
            is_accessible => 1,
            ret_cond_id => 46828,
            properties => '',
        },
    },
};
cmp_deeply(g([4669]), $test_dataset1,'1st shard: by ClientID');
cmp_deeply(g([], ret_cond_id => [46828]), $test_dataset1,'1st shard: by ret_cond_id');

my $test_dataset2 = {
    6190 => {
        46236 => {
            ClientID => 6190,
            condition => [
                {
                    goals => [
                        {
                            goal_id => 4022072738,
                            goal_type => "goal",
                            time    => 30,
                        },
                    ],
                    type => 'or',
                },
            ],
            condition_desc => undef,
            retargeting_conditions_type => 'metrika_goals',
            condition_name => 'Паллетные борта',
            is_accessible => 0,
            ret_cond_id => 46236,
            properties => '',
        },
    },
};
cmp_deeply(g([6190]), $test_dataset2, '2nd shard: by ClientID');
cmp_deeply(g([], ret_cond_id => [46236]), $test_dataset2, '2nd shard: by ret_cond_id');

my $test_dataset3 = {
    1190 => {
        6197 => {
            ClientID => 1190,
            condition => [
                {
                    goals => [
                        {
                            goal_id => 4000183712,
                            goal_type => "goal",
                            time    => 1,
                        },
                    ],
                    type => 'or',
                },
            ],
            condition_desc => undef,
            retargeting_conditions_type => 'metrika_goals',
            condition_name => 'Посетил сайт',
            is_accessible => 1,
            ret_cond_id => 6197,
            properties => '',
        },
        44251 => {
            ClientID => 1190,
            condition => [
                {
                    goals => [
                        {
                            goal_id => 4000183712,
                            goal_type => "goal",
                            time    => 30,
                        },
                    ],
                    type => 'or',
                },
            ],
            condition_desc => undef,
            retargeting_conditions_type => 'metrika_goals',
            condition_name => 'Возврат',
            is_accessible => 0,
            ret_cond_id => 44251,
            properties => '',
        }
    },
    7083 => {
        3621 => {
            ClientID => 7083,
            condition => [
                {
                    goals => [
                        {
                            goal_id => 4020818681,
                            goal_type => "goal",
                            time    => 1,
                        },
                    ],
                    type => 'or',
                },
            ],
            condition_desc => undef,
            retargeting_conditions_type => 'metrika_goals',
            condition_name => 'Biz',
            is_accessible => 0,
            ret_cond_id => 3621,
            properties => '',
        },
        3622 => {
            ClientID => 7083,
            condition => [
                {
                    goals => [
                        {
                            goal_id => 4000040708,
                            goal_type => "goal",
                            time    => 1,
                        },
                    ],
                    type => 'or',
                },
            ],
            condition_desc => undef,
            retargeting_conditions_type => 'metrika_goals',
            condition_name => 'nic.ru',
            is_accessible => 1,
            ret_cond_id => 3622,
            properties => '',
        }
    },
    11579 => {
        5273 => {
            ClientID => 11579,
            condition => [
                {
                    goals => [
                        {
                            goal_id => 2175637,
                            goal_type => "goal",
                            time    => 2,
                        },
                    ],
                    type => 'or',
                },
            ],
            condition_desc => undef,
            retargeting_conditions_type => 'metrika_goals',
            condition_name => 'Человек просмотрел 7 страниц',
            is_accessible => 1,
            ret_cond_id => 5273,
            properties => '',
        },
        5276 => {
            ClientID => 11579,
            condition => [
                {
                    goals => [
                        {
                            goal_id => 2175637,
                            goal_type => "goal",
                            time    => 1,
                        },
                    ],
                    type => 'or',
                },
            ],
            condition_desc => undef,
            retargeting_conditions_type => 'metrika_goals',
            condition_name => 'Просмотр 7 страниц',
            is_accessible => 0,
            ret_cond_id => 5276,
            properties => '',
        },
        6058 => {
            ClientID => 11579,
            condition => bag(
                {
                    goals => [
                        {
                            goal_id => 2409802,
                            goal_type => "goal",
                            time    => 1,
                        },
                    ],
                    type => 'or',
                }, {
                    goals => [
                        {
                            goal_id => 2409805,
                            goal_type => "goal",
                            time    => 1,
                        },
                    ],
                    type => 'not',
                }
            ),
            condition_desc => undef,
            retargeting_conditions_type => 'metrika_goals',
            condition_name => 'Ушел из корзины',
            is_accessible => 1,
            ret_cond_id => 6058,
            properties => '',
        },
        6977 => {
            ClientID => 11579,
            condition => bag(
                {
                    goals => [
                        {
                            goal_id => 2410477,
                            goal_type => "goal",
                            time    => 2,
                        },
                    ],
                    type => 'or',
                }, {
                    goals => [
                        {
                            goal_id => 2409805,
                            goal_type => "goal",
                            time    => 2,
                        },
                    ],
                    type => 'not',
                }, {
                    goals => [
                        {
                            goal_id => 2409808,
                            goal_type => "goal",
                            time    => 2,
                        },
                    ],
                    type => 'not',
                }
            ),
            condition_desc => undef,
            retargeting_conditions_type => 'metrika_goals',
            condition_name => 'Ретаргетинг на корзину',
            is_accessible => 0,
            ret_cond_id => 6977,
            properties => '',
        },
        7053 => {
            ClientID => 11579,
            condition => bag(
                {
                    goals => [
                        {
                            goal_id => 2409805,
                            goal_type => "goal",
                            time    => 2,
                        },
                    ],
                    type => 'or',
                }, {
                    goals => [
                        {
                            goal_id => 2409808,
                            goal_type => "goal",
                            time    => 2,
                        },
                    ],
                    type => 'not',
                }
            ),
            condition_desc => undef,
            retargeting_conditions_type => 'metrika_goals',
            condition_name => 'Отказ - форма контактов',
            is_accessible => 1,
            ret_cond_id => 7053,
            properties => '',
        },
    },
};
cmp_deeply(g([1190,7083,11579]), $test_dataset3, 'both shards: by ClientID');
cmp_deeply(g([], ret_cond_id => [6197, 44251, 3621, 3622, 5273, 5276, 6058, 6977, 7053]), $test_dataset3, 'both shards: by ret_cond_id');

my $test_dataset4 = {
    7083 => {
        3621 => $test_dataset3->{7083}->{3621},
    },
    11579 => {
        5276 => $test_dataset3->{11579}->{5276},
    },
};
cmp_deeply(g([], ret_cond_id => [3621, 5276]), $test_dataset4, 'both shards: by ret_cond_id');

my $test_dataset5 = {
    4669 => {
        46828 => {
            ClientID => 4669,
            condition => [
                {
                    goals => [
                        {
                            goal_id => 4020690383,
                            goal_type => "goal",
                            time    => 30,
                        },
                    ],
                    type => 'or',
                },
            ],
            condition_desc => undef,
            retargeting_conditions_type => 'metrika_goals',
            condition_name => 'Цель',
            ret_cond_id => 46828,
            properties => '',
        },
    },
};
cmp_deeply(g([],ret_cond_id => [46828], short => 1), $test_dataset5, '1st shard: by ret_cond_id - with "short" option');

cmp_deeply(
    g([9999,8888]),
    {
        8888 => {},
        9999 => {},
    },
    'ClientIDs without retargeting'
);

cmp_deeply(
    g([11579], ret_cond_id => [5273,7053]),
    {
        11579 => {
            5273 => {
                ClientID => 11579,
                condition => [
                    {
                        goals => [
                            {
                                goal_id => 2175637,
                                goal_type => "goal",
                                time    => 2,
                            },
                        ],
                        type => 'or',
                    },
                ],
                condition_desc => undef,
                retargeting_conditions_type => 'metrika_goals',
                condition_name => 'Человек просмотрел 7 страниц',
                is_accessible => 1,
                ret_cond_id => 5273,
                properties => '',
            },
            7053 => {
                ClientID => 11579,
                condition => bag(
                    {
                        goals => [
                            {
                                goal_id => 2409805,
                                goal_type => "goal",
                                time    => 2,
                            },
                        ],
                        type => 'or',
                    }, {
                        goals => [
                            {
                                goal_id => 2409808,
                                goal_type => "goal",
                                time    => 2,
                            },
                        ],
                        type => 'not',
                    }
                ),
                condition_desc => undef,
                retargeting_conditions_type => 'metrika_goals',
                condition_name => 'Отказ - форма контактов',
                is_accessible => 1,
                ret_cond_id => 7053,
                properties => '',
            },
        },
    },
    '2nd shard: by ClientID and ret_cond_id'
);

done_testing();

