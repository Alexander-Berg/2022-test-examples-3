use strict;
use warnings;
use utf8;
no warnings "redefine";

use Yandex::Test::UTF8Builder;
use Test::More;

use Settings;
use Yandex::DBUnitTest qw/:all/;
use MetrikaCounters;
use Direct::Model::MetrikaGoal;

BEGIN { use_ok('CampaignTools', 'validate_autobudget_goal_id'); }

*validate_autobudget_goal_id = \&CampaignTools::validate_autobudget_goal_id; 

my %db = (
    campaigns => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [{cid => 101, OrderID => 125, type => 'text'},
                  {cid => 301, OrderID => 0, type => 'performance'},
                  {cid => 302, OrderID => 671, type => 'performance'},
                  {cid => 401, OrderID => 542, type => 'dynamic'}],
        },
    },
    camp_metrika_goals => {
        original_db => PPC(shard => 'all'),
        rows => {
            1 => [{cid => 101, goal_id => 10416, goals_count => 10, context_goals_count => 22},
                  {cid => 101, goal_id => 10417, goals_count => 30, context_goals_count => 42},
                  {cid => 101, goal_id => 10418, goals_count => 50, context_goals_count => 52},
                  {cid => 301, goal_id => 10409, goals_count => 20, context_goals_count => 4},
                  {cid => 301, goal_id => 10411, goals_count => 13, context_goals_count => 1},

                  {cid => 302, goal_id => 10413, goals_count => 123, context_goals_count => 69},
                  {cid => 401, goal_id => 10420, goals_count => 34, context_goals_count => 85}],
        },
    },
    metrika_goals => {
        original_db => PPCDICT,
        rows => [
          {
            'parent_goal_id' => undef,
            'goal_type' => 'url',
            'goal_status' => 'Active',
            'subgoal_index' => undef,
            'counter_status' => 'Active',
            'goal_id' => '10409'
          },
          {
            'goal_status' => 'Active',
            'goal_type' => 'step',
            'counter_status' => 'Active',
            'subgoal_index' => 1,
            'goal_id' => '10410',
            'parent_goal_id' => undef
          },
          {
            'parent_goal_id' => 10409,
            'goal_type' => 'url',
            'goal_status' => 'Active',
            'counter_status' => 'Active',
            'subgoal_index' => undef,
            'goal_id' => '10411'
          },
          {
            'parent_goal_id' => undef,
            'goal_status' => 'Active',
            'goal_type' => 'number',
            'counter_status' => 'Active',
            'subgoal_index' => undef,
            'goal_id' => '10412'
          },
          {
            'goal_status' => 'Active',
            'goal_type' => 'url',
            'counter_status' => 'Active',
            'subgoal_index' => undef,
            'goal_id' => '10413',
            'parent_goal_id' => undef
          },
          {
            'subgoal_index' => undef,
            'counter_status' => 'Active',
            'goal_id' => '10414',
            'goal_type' => 'number',
            'goal_status' => 'Active',
            'parent_goal_id' => undef
          },
          {
            'goal_type' => 'url',
            'goal_status' => 'Active',
            'counter_status' => 'Active',
            'goal_id' => '10415',
            'subgoal_index' => undef,
            'parent_goal_id' => undef
          },
          {
            'goal_id' => '10416',
            'counter_status' => 'Active',
            'subgoal_index' => undef,
            'goal_status' => 'Active',
            'goal_type' => 'url',
            'parent_goal_id' => undef
          },
          {
            'parent_goal_id' => undef,
            'goal_type' => 'number',
            'goal_status' => 'Deleted',
            'counter_status' => 'Active',
            'subgoal_index' => undef,
            'goal_id' => '10417'
          },
          {
            'parent_goal_id' => undef,
            'subgoal_index' => undef,
            'counter_status' => 'Deleted',
            'goal_id' => '10418',
            'goal_type' => 'number',
            'goal_status' => 'Active'
          },
          {
              'parent_goal_id' => undef,
              'goal_type' => 'url',
              'goal_status' => 'Active',
              'subgoal_index' => undef,
              'counter_status' => 'Active',
              'goal_id' => '10420'
          },
        ],
    },
    shard_inc_cid => {
        original_db => PPCDICT,
        rows => [
            {cid => 101, ClientID => 1},
            {cid => 301, ClientID => 1},
            {cid => 302, ClientID => 1},
            {cid => 401, ClientID => 1},
        ],
    },
    shard_order_id => {
        original_db => PPCDICT,
        rows => [
            {OrderID => 101, ClientID => 1},
            {OrderID => 302, ClientID => 1},
        ],
    },
    shard_client_id => {
        original_db => PPCDICT,
        rows => [
            {ClientID => 1, shard => 1},
        ],
    },
);

init_test_dataset(\%db);

my $strategy_roi = {
    is_search_stop => 1,
    is_net_stop => 0, 
    is_autobudget => 1,
    name => 'autobudget_roi',
    search => {name => 'stop'},
    net => {
        name => 'autobudget_roi',
        goal_id => 4439510283,
        sum => 12000,
        bid => 30,
        roi_coef => 0.5,
        reserve_return => 1.31,
        profitability => 12,
    }
};
my $strategy_crr = {
    is_search_stop => 1,
    is_net_stop    => 0,
    is_autobudget  => 1,
    name           => 'autobudget_crr',
    search         => { name => 'stop' },
    net            => {
        name    => 'autobudget_crr',
        goal_id => 4439510283,
        sum     => 12000,
        crr     => 100,
    },
};
my $strategy_cpa = {
    is_search_stop => 1,
    is_net_stop => 0, 
    is_autobudget => 1,
    name => 'autobudget_avg_cpa_per_filter',
    search => {name => 'stop'},
    net => {
        name => 'autobudget_avg_cpa_per_filter',
        filter_avg_bid => 50,
        filter_avg_cpa => 200,
        goal_id => 84891206,
        sum => 15000,
        bid => 30,
    }
};

my $strategy_autobudget = {
    is_search_stop => 1,
    is_net_stop => 0,
    is_autobudget => 1,
    name => 'autobudget',
    search => {name => 'stop'},
    net => {
        name => 'autobudget',
        goal_id => 84891206,
        sum => 15000,
        bid => 30,
    }
};

my $goals_1 = [
    Direct::Model::MetrikaGoal->new(goal_id => 7102, counter_status => 'Active', goal_status => 'Active', goals_count => 0,  context_goals_count => 0),
    Direct::Model::MetrikaGoal->new(goal_id => 89123, counter_status => 'Active', goal_status => 'Active', goals_count => 0,  context_goals_count => 0),
    Direct::Model::MetrikaGoal->new(goal_id => 913, counter_status => 'Active', goal_status => 'Deleted', goals_count => 0,  context_goals_count => 0),
    Direct::Model::MetrikaGoal->new(goal_id => 800, counter_status => 'Deleted', goal_status => 'Deleted', goals_count => 0,  context_goals_count => 0),
];

my $goals_2 = [
    Direct::Model::MetrikaGoal->new(goal_id => 7712, counter_status => 'Active', goal_status => 'Active', goals_count => 0,  context_goals_count => 0),
];

subtest 'performace negative scenario' => sub {
    *MetrikaCounters::get_counters_goals = sub {
        return {
            700 => $goals_1,
        };
    };
    like validate_autobudget_goal_id(0, {type => 'performance', metrika_counters => '600,700', strategy => $strategy_roi, strategy_name => 'autobudget_roi'}), qr/Указанная цель не найдена/;
    like validate_autobudget_goal_id(2711, {type => 'performance', metrika_counters => '600,700', strategy => $strategy_roi, strategy_name => 'autobudget_roi'}), qr/Указанная цель не найдена/;
    like validate_autobudget_goal_id(800, {type => 'performance', metrika_counters => '', strategy => $strategy_roi, strategy_name => 'autobudget_roi'}), qr/Необходимо задать счетчик метрики/;
    # goal exists but counter is deleted
    like validate_autobudget_goal_id(800, {type => 'performance', metrika_counters => '600,700', strategy => $strategy_cpa, strategy_name => 'autobudget_avg_cpa_per_filter'}), qr/Указанная цель не найдена/;
};

subtest 'text campaign negative scenario' => sub {
    #goal_status = "Deleted"
    like validate_autobudget_goal_id(10417, {cid => 101, type => 'text', metrika_counters => '600,700', strategy => $strategy_roi, strategy_name => 'autobudget_roi'}), qr/Указанная цель не найдена/;
    #counter_status = "Deleted"
    like validate_autobudget_goal_id(10418, {cid => 101, type => 'text', metrika_counters => '600,700', strategy => $strategy_roi, strategy_name => 'autobudget_roi'}), qr/Указанная цель не найдена/;
};

subtest 'use all_goals optimization' => sub {
    *MetrikaCounters::get_counters_goals = sub {
        return {
            700 => $goals_1,
        };
    };

    like validate_autobudget_goal_id(0, {type => 'performance', metrika_counters => '600,700', strategy => $strategy_roi, strategy_name => 'autobudget_roi'}), qr/Указанная цель не найдена/;
    like validate_autobudget_goal_id(0, {type => 'performance', metrika_counters => '600,700', strategy => $strategy_cpa, strategy_name => 'autobudget_avg_cpa_per_filter'}), qr/Указанная цель не найдена/;
    #для остальных типов кампаний оптимизация "по всем целям" допустима
    ok !validate_autobudget_goal_id(0, {cid => 101, type => 'text', metrika_counters => '600,700', strategy => $strategy_roi, strategy_name => 'autobudget_roi'});
    ok !validate_autobudget_goal_id(0, {cid => 101, type => 'text', metrika_counters => '600,700', strategy => $strategy_cpa, strategy_name => 'autobudget_avg_cpa_per_filter'});
};


subtest 'no_not enough achievement' => sub {
    *MetrikaCounters::get_counters_goals = sub {
        return {
            600 => $goals_1,
        };
    };
    ok !validate_autobudget_goal_id(89123, {type => 'performance', metrika_counters => '600,700', strategy => $strategy_cpa, strategy_name => 'autobudget_avg_cpa_per_filter'});
    ok !validate_autobudget_goal_id(89123, {type => 'performance', metrika_counters => '600,700', strategy => $strategy_roi, strategy_name => 'autobudget_roi'});
};

subtest 'find goals in db' => sub {
    *MetrikaCounters::get_counters_goals = sub {
        die 'all goals must be in db';
    };
    ok !validate_autobudget_goal_id(10409, {cid => 301, type => 'performance', metrika_counters => 600, strategy => $strategy_cpa, strategy_name => 'autobudget_avg_cpa_per_filter'});
    ok !validate_autobudget_goal_id(10409, {cid => 301, type => 'performance', metrika_counters => '700,971', strategy => $strategy_roi, strategy_name => 'autobudget_roi'});

    ok !validate_autobudget_goal_id(10413, {cid => 302, type => 'performance', metrika_counters => '700,971', strategy => $strategy_roi, strategy_name => 'autobudget_roi'});
    ok !validate_autobudget_goal_id(10413, {cid => 302, type => 'performance', metrika_counters => '700,971', strategy => $strategy_cpa, strategy_name => 'autobudget_avg_cpa_per_filter'});

    ok !validate_autobudget_goal_id(10416, {cid => 101, type => 'text', metrika_counters => '600,700', strategy => $strategy_roi, strategy_name => 'autobudget_roi'});
    ok !validate_autobudget_goal_id(10416, {cid => 101, type => 'text', metrika_counters => '600,700', strategy => $strategy_cpa, strategy_name => 'autobudget_avg_cpa_per_filter'});
};

subtest 'request goals from metrika' => sub {
    *MetrikaCounters::get_counters_goals = sub {
        return {
            20913 => $goals_1,
            88127101 => $goals_2
        }
    };
    ok !validate_autobudget_goal_id(7102, {cid => 301, type => 'performance', metrika_counters => 20913, strategy => $strategy_cpa, strategy_name => 'autobudget_avg_cpa_per_filter'});
    like validate_autobudget_goal_id(7712, {cid => 301, type => 'performance', metrika_counters => '20913,88127101' , strategy => $strategy_roi, strategy_name => 'autobudget_roi'}),
         qr//;
};

subtest 'use meaningful_goals optimization for autobuget_roi' => sub {

    like validate_autobudget_goal_id($Settings::MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID, {cid => 301, type => 'performance', metrika_counters => '700,971', strategy => $strategy_roi, strategy_name => 'autobudget_roi'},
        meaningful_goals => [{ goal_id => 12, value => 25 }]) , qr/Для оптимизации по ключевым целям необходимо указать хотя бы одну ключевую цель, отличную от вовлеченных сессий/;

    like validate_autobudget_goal_id($Settings::MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID, {cid => 301, type => 'performance', metrika_counters => '700,971', strategy => $strategy_roi, strategy_name => 'autobudget_roi'},
        meaningful_goals => []) , qr/Для оптимизации по ключевым целям необходимо указать хотя бы одну ключевую цель, отличную от вовлеченных сессий/;

    #оптимизацию по ключевым целям разрешаем только в случае когда заданы ключевые цели отличные от вовлеченных сессий
    ok !validate_autobudget_goal_id($Settings::MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID, {cid => 301, type => 'performance', metrika_counters => '700,971', strategy => $strategy_roi, strategy_name => 'autobudget_roi'},
        meaningful_goals => [{ goal_id => 10416, value => 25 }]);
};

subtest 'use meaningful_goals optimization for autobuget_crr' => sub {

    like validate_autobudget_goal_id($Settings::MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID, {cid => 301, type => 'performance', metrika_counters => '700,971', strategy => $strategy_crr, strategy_name => 'autobudget_crr'},
        (),
        meaningful_goals => [{ goal_id => 12, value => 25 }]) , qr/Для оптимизации по ключевым целям необходимо указать хотя бы одну ключевую цель, отличную от вовлеченных сессий/;

    like validate_autobudget_goal_id($Settings::MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID, {cid => 301, type => 'performance', metrika_counters => '700,971', strategy => $strategy_crr, strategy_name => 'autobudget_crr'},
        (),
        meaningful_goals => []) , qr/Для оптимизации по ключевым целям необходимо указать хотя бы одну ключевую цель, отличную от вовлеченных сессий/;

    #оптимизацию по ключевым целям разрешаем только в случае когда заданы ключевые цели отличные от вовлеченных сессий
    ok !validate_autobudget_goal_id($Settings::MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID, {cid => 301, type => 'performance', metrika_counters => '700,971', strategy => $strategy_crr, strategy_name => 'autobudget_crr'},
        (),
        meaningful_goals => [{ goal_id => 10416, value => 25 }]);

    #impossible to set goal_id=13 for autobudget_crr with pay_for_conversion when has_all_meaningful_goals_for_pay_for_conversion_strategies_allowed is 0
    like validate_autobudget_goal_id($Settings::MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID, {cid => 301, type => 'performance', metrika_counters => '700,971', strategy => $strategy_crr, strategy_name => 'autobudget_crr'},
        pay_for_conversion => 1,
        meaningful_goals => []) , qr/Указанная цель не найдена/;

    #has_all_meaningful_goals_for_pay_for_conversion_strategies_allowed => 1 для смартов
    ok !validate_autobudget_goal_id($Settings::MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID, {cid => 301, type => 'performance', metrika_counters => '700,971', strategy => $strategy_crr, strategy_name => 'autobudget_crr'},
        pay_for_conversion => 1,
        has_all_meaningful_goals_for_pay_for_conversion_strategies_allowed => 1,
        meaningful_goals => [{ goal_id => 10409, value => 65 }]);

    #has_all_meaningful_goals_for_pay_for_conversion_strategies_allowed => 1 для ТГО
    ok !validate_autobudget_goal_id($Settings::MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID, {cid => 101, type => 'text', metrika_counters => '700,971', strategy => $strategy_crr, strategy_name => 'autobudget_crr'},
        pay_for_conversion => 1,
        has_all_meaningful_goals_for_pay_for_conversion_strategies_allowed => 1,
        meaningful_goals => [{ goal_id => 10416, value => 25 }]);

    #has_all_meaningful_goals_for_pay_for_conversion_strategies_allowed => 1 для ДО
    ok !validate_autobudget_goal_id($Settings::MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID, {cid => 401, type => 'dynamic', metrika_counters => '700,971', strategy => $strategy_crr, strategy_name => 'autobudget_crr'},
        pay_for_conversion => 1,
        has_all_meaningful_goals_for_pay_for_conversion_strategies_allowed => 1,
        meaningful_goals => [{ goal_id => 10420, value => 12 }]);
};

subtest 'use meaningful_goals optimization for autobuget' => sub {

    #ключевые цели не заданы
    like validate_autobudget_goal_id($Settings::MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID, {cid => 301, type => 'text', metrika_counters => '700,971', strategy => $strategy_autobudget, strategy_name => 'autobudget'},
        meaningful_goals => [{ goal_id => 12, value => 25 }]) , qr/Для оптимизации по ключевым целям необходимо указать хотя бы одну ключевую цель, отличную от вовлеченных сессий/;

    like validate_autobudget_goal_id($Settings::MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID, {cid => 301, type => 'text', metrika_counters => '700,971', strategy => $strategy_autobudget, strategy_name => 'autobudget'},
        has_cpa_week_budget_key_goals_enabled => 1,
        meaningful_goals => []) , qr/Для оптимизации по ключевым целям необходимо указать хотя бы одну ключевую цель, отличную от вовлеченных сессий/;

    #оптимизацию по ключевым целям разрешаем только в случае когда заданы ключевые цели отличные от вовлеченных сессий
    ok !validate_autobudget_goal_id($Settings::MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID, {cid => 301, type => 'text', metrika_counters => '700,971', strategy => $strategy_autobudget, strategy_name => 'autobudget'},
        meaningful_goals => [{ goal_id => 10416, value => 25 }]);
};

done_testing;
