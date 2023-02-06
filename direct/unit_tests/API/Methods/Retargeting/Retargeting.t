#!/usr/bin/perl

use strict;
use warnings;

use utf8;

use my_inc '../../../../';

use Settings;

use Storable qw/dclone/;
use Test::Deep;
use Test::More tests => 9;

use Yandex::Test::UTF8Builder;

use_ok('API::Methods::Retargeting');

{
    no warnings qw/redefine once prototype/;
    no strict 'refs';
    foreach (
        qw/
        API::Methods::Retargeting::rbac_get_client_uids_by_clientid
        Retargeting::get_metrika_goals_by_uid
        Retargeting::mass_get_retargeting_conditions_by_ClientIDS
        /
    )
    {
        my $method_name = $1 if /([^:]+)$/;
        *{"$_"} = sub { check_method_args_and_get_reply($method_name, @_) };
    }
}

my %methods_args_and_replies;

sub check_method_args_and_get_reply {
    my $method_name = shift;

    my ($args, $reply);

    if ($methods_args_and_replies{$method_name} and @{$methods_args_and_replies{$method_name}}) {
        ($args, $reply) = splice(@{$methods_args_and_replies{$method_name}}, 0, 2);
        eq_deeply(\@_, $args) or fail("Wrong args in $method_name");
    } else {
        fail("No args for $method_name");
    }

    return $reply;
}

*Retargeting = \&API::Methods::Retargeting::Retargeting;

subtest 'API::Methods::Retargeting::Retargeting - Get method' => sub {
    plan tests => 3;

    # without currency conversion
    my $self = {
        preprocess => {
            retargetings => {
                1 => { pid => 1, ret_id => 2, ret_cond_id => 3, is_suspended => 0, autobudgetPriority => 1, price_context => 5, },
                2 => { pid => 2, ret_id => 3, ret_cond_id => 4, is_suspended => 1, autobudgetPriority => 3, price_context => 7, },
                3 => { pid => 3, ret_id => 4, ret_cond_id => 5, is_suspended => 0, autobudgetPriority => 5, price_context => 9, },
                4 => { pid => 4, ret_id => 5, ret_cond_id => 6, is_suspended => 1, autobudgetPriority => undef, price_context => 11, },
            },
            pid2bid => { 1 => 2, 2 => 3, 3 => 4, 4 => 5, },
        },
    };
    my $params = { Action => 'Get', };

    cmp_bag(
        Retargeting( $self, $params )->{Retargetings},
        [
            {
                AdID                   => 2,
                AdGroupID              => 1,
                ContextPrice           => 5,
                StatusPaused           => 'No',
                RetargetingID          => 2,
                AutoBudgetPriority     => 'Low',
                RetargetingConditionID => 3,
            },
            {
                AdID                   => 3,
                AdGroupID              => 2,
                ContextPrice           => 7,
                StatusPaused           => 'Yes',
                RetargetingID          => 3,
                AutoBudgetPriority     => 'Medium',
                RetargetingConditionID => 4,
            },
            {
                AdID                   => 4,
                AdGroupID              => 3,
                ContextPrice           => 9,
                StatusPaused           => 'No',
                RetargetingID          => 4,
                AutoBudgetPriority     => 'High',
                RetargetingConditionID => 5,
            },
            {
                AdID                   => 5,
                AdGroupID              => 4,
                ContextPrice           => 11,
                StatusPaused           => 'Yes',
                RetargetingID          => 5,
                AutoBudgetPriority     => undef,
                RetargetingConditionID => 6,
            },
        ],
        'without currency conversion'
    );

    # mocking
    no strict 'refs';
    no warnings 'redefine';
    *{"API::Filter::currency_price_rounding"} = sub { $_[0] };
    *{"API::Filter::convert_currency"}        = sub { $_[0] * 2 };

    # with currency conversion (to RUB)
    $self = {
        preprocess => {
            retargetings => {
                1 => { pid => 1, ret_id => 2, ret_cond_id => 3, is_suspended => 0, autobudgetPriority => 1, price_context => 5, },
                2 => { pid => 2, ret_id => 3, ret_cond_id => 4, is_suspended => 1, autobudgetPriority => 3, price_context => 7, currency => 'RUB', },
                3 => { pid => 3, ret_id => 4, ret_cond_id => 5, is_suspended => 0, autobudgetPriority => 5, price_context => 9, currency => 'EUR', },
            },
            pid2bid => { 1 => 2, 2 => 3, 3 => 4, },
        },
    };
    $params = {
        Action  => 'Get',
        Options => { Currency => 'RUB', }, 
    };

    cmp_bag(
        Retargeting( $self, $params )->{Retargetings},
        [
            {
                AdID                   => 2,
                AdGroupID              => 1,
                Currency               => 'RUB',
                ContextPrice           => 10,
                StatusPaused           => 'No',
                RetargetingID          => 2,
                AutoBudgetPriority     => 'Low',
                RetargetingConditionID => 3,
            },
            {
                AdID                   => 3,
                AdGroupID              => 2,
                ContextPrice           => 7,
                Currency               => 'RUB',
                StatusPaused           => 'Yes',
                RetargetingID          => 3,
                AutoBudgetPriority     => 'Medium',
                RetargetingConditionID => 4,
            },
            {
                AdID                   => 4,
                AdGroupID              => 3,
                Currency               => 'RUB',
                ContextPrice           => 18,
                StatusPaused           => 'No',
                RetargetingID          => 4,
                AutoBudgetPriority     => 'High',
                RetargetingConditionID => 5,
            },
        ],
        'with currency conversion (to RUB)'
    );

    # with currency conversion (to UE)
    $self = {
        preprocess => {
            retargetings => {
                1 => { pid => 1, ret_id => 2, ret_cond_id => 3, is_suspended => 0, autobudgetPriority => 1, price_context => 5, },
                2 => { pid => 2, ret_id => 3, ret_cond_id => 4, is_suspended => 1, autobudgetPriority => 3, price_context => 7, currency => 'RUB', },
                3 => { pid => 3, ret_id => 4, ret_cond_id => 5, is_suspended => 0, autobudgetPriority => 5, price_context => 9, currency => 'EUR', },
            },
            pid2bid => { 1 => 2, 2 => 3, 3 => 4, },
        },
    };
    $params = { Action  => 'Get', };

    cmp_bag(
        Retargeting( $self, $params )->{Retargetings},
        [
            {
                AdID                   => 2,
                AdGroupID              => 1,
                ContextPrice           => 5,
                StatusPaused           => 'No',
                RetargetingID          => 2,
                AutoBudgetPriority     => 'Low',
                RetargetingConditionID => 3,
            },
            {
                AdID                   => 3,
                AdGroupID              => 2,
                ContextPrice           => 14,
                StatusPaused           => 'Yes',
                RetargetingID          => 3,
                AutoBudgetPriority     => 'Medium',
                RetargetingConditionID => 4,
            },
            {
                AdID                   => 4,
                AdGroupID              => 3,
                ContextPrice           => 18,
                StatusPaused           => 'No',
                RetargetingID          => 4,
                AutoBudgetPriority     => 'High',
                RetargetingConditionID => 5,
            },
        ],
        'with currency conversion (to UE)'
    );

    #done_testing();
};

subtest 'API::Methods::Retargeting::validate_retargeting - Delete method' => sub {
    plan tests => 1;

    no strict 'refs';
    no warnings 'redefine';

    my $test_params;

    *{'Retargeting::delete_group_retargetings'} = sub { $test_params = $_[0] };

        my $self = {
        preprocess => {
            retargetings => {
                1 => { pid => 1, ret_id => 2, ret_cond_id => 3, is_suspended => 0, autobudgetPriority => 1, price_context => 5, },
                2 => { pid => 2, ret_id => 3, ret_cond_id => 4, is_suspended => 1, autobudgetPriority => 3, price_context => 7, },
                3 => { pid => 3, ret_id => 4, ret_cond_id => 5, is_suspended => 0, autobudgetPriority => 5, price_context => 9, },
                4 => { pid => 4, ret_id => 5, ret_cond_id => 6, is_suspended => 1, autobudgetPriority => undef, price_context => 11, },
            },
            pid2bid => { 1 => 2, 2 => 3, 3 => 4, 4 => 5, },
        },
    };
    my $params = { Action => 'Delete', };

    Retargeting( $self, $params );

    is_deeply( $test_params, [ values %{ $self->{preprocess}{retargetings} } ], 'correct params given to Retargeting::delete_group_retargetings');

    #done_testing();
};

subtest 'API::Methods::Retargeting::validate_retargeting - Add methods' => sub {
    plan tests => 9;

    my @ARGS;
    my $ID = 0;

    {
        no strict 'refs';
        no warnings 'redefine';

        *{'Retargeting::update_group_retargetings'} = sub { push @ARGS, $_[0]; [ ++$ID ] };
    }

    # do not process retargeting with errors
    my $self = {
        ret => [
            { Errors => [ 'fake' ], },
        ],
        preprocess => {
            converted => [
                { bid => 1, pid => 1, ret_cond_id => 1, },
            ],
        },
    };
    my $params = { Action => 'Add', };

    is_deeply(
        Retargeting( $self, $params ),
        {
            ActionsResult => [
                { Errors => [ 'fake' ], },
            ],
        },
        'do not process retargeting with errors'
    );

    # UpdateRetargeting warning
    $self = {
        preprocess => {
            camps   => { 1 => { currency => undef, }, },
            pid2cid => { 1 => 1, },
            converted => [
                { bid => 1, pid => 1, ret_id => 1, ret_cond_id => 1, price_context => 5, },
            ],
            camp_strategy => {
                1 => { is_autobudget => 0, },
            },
            old_retargetings_by_pid_hash => {
                1 => {
                    1 => { ret_id => 11, },
                },
            },
            adgroup_types => { 1 => 'base' },
        },
    };

    $params = { Action => 'Add', };

    is_deeply(
        Retargeting( $self, $params ),
        {
            ActionsResult => [
                {
                    RetargetingID => 1,
                    Warnings      => [
                        {
                            Description   => '{"id":11}',
                            WarningCode   => 208,
                            WarningString => 'Существующее условие ретаргетинга было обновлено',
                        },
                    ],
                },
            ],
        },
        'UpdateRetargeting warning'
    );

    is_deeply(
        \@ARGS,
        [
            {
                pid          => 1,
                bid          => 1,
                cid          => 1,
                currency     => undef,
                retargetings => [
                    { bid => 1, pid => 1, ret_id => 11, ret_cond_id => 1, price_context => 5, },
                ],
            },
        ],
        'check Retargeting::update_group_retargetings params - ret_id changed'
    );

    # clean up
    $ID   = 0;
    @ARGS = ();

    # RetargetingContextPriceIgnored warning - no old retargeting
    $self = {
        preprocess => {
            camps   => { 1 => { currency => undef, }, },
            pid2cid => { 1 => 1, },
            converted => [
                { bid => 1, pid => 1, ret_id => 1, ret_cond_id => 1, price_context => 5, },
            ],
            camp_strategy => {
                1 => { is_autobudget => 1, },
            },
            old_retargetings_by_pid_hash => { },
        },
    };

    $params = { Action => 'Add', };

    is_deeply(
        Retargeting( $self, $params ),
        {
            ActionsResult => [
                {
                    RetargetingID => 1,
                    Warnings      => [
                        {
                            Description   => '{}',
                            WarningCode   => 209,
                            WarningString => 'Ставка для условия ретаргетинга была проигнорирована, так как включен автобюджет',
                        },
                    ],
                },
            ],
        },
        'RetargetingContextPriceIgnored warning - no old retargeting'
    );

    is_deeply(
        \@ARGS,
        [
            {
                pid          => 1,
                bid          => 1,
                cid          => 1,
                currency     => undef,
                retargetings => [
                    { bid => 1, pid => 1, ret_id => 1, ret_cond_id => 1, price_context => 5, },
                ],
            },
        ],
        'check Retargeting::update_group_retargetings params'
    );

    # clean up
    $ID   = 0;
    @ARGS = ();

    # RetargetingContextPriceIgnored warning - new price_context != old price_context
    $self = {
        preprocess => {
            camps   => { 1 => { currency => undef, }, },
            pid2cid => { 1 => 1, },
            converted => [
                { bid => 1, pid => 1, ret_id => 1, ret_cond_id => 1, price_context => 5, },
            ],
            camp_strategy => {
                1 => { is_autobudget => 1, },
            },
            old_retargetings_by_pid_hash => {
                1 => {
                    1 => { ret_id => 11, price_context => 15, },
                },
            },
        },
    };

    $params = { Action => 'Add', };

    is_deeply(
        Retargeting( $self, $params ),
        {
            ActionsResult => [
                {
                    RetargetingID => 1,
                    Warnings      => [
                        {
                            Description   => '{"id":11}',
                            WarningCode   => 208,
                            WarningString => 'Существующее условие ретаргетинга было обновлено',
                        },
                        {
                            Description   => '{}',
                            WarningCode   => 209,
                            WarningString => 'Ставка для условия ретаргетинга была проигнорирована, так как включен автобюджет',
                        },
                    ],
                },
            ],
        },
        'RetargetingContextPriceIgnored warning - new price_context != old price_context'
    );

    is_deeply(
        \@ARGS,
        [
            {
                pid          => 1,
                bid          => 1,
                cid          => 1,
                currency     => undef,
                retargetings => [
                    { bid => 1, pid => 1, ret_id => 11, ret_cond_id => 1, price_context => 5, },
                ],
            },
        ],
        'check Retargeting::update_group_retargetings params'
    );

    # clean up
    $ID   = 0;
    @ARGS = ();

    # RetargetingConditionAlreadyExistsInAdGroup warning
    $self = {
        preprocess => {
            camps   => { 1 => { currency => undef, }, },
            pid2cid => { 1 => 1, },
            converted => [
                { bid => 1, pid => 1, ret_id => 1, ret_cond_id => 1, price_context => 5, },
                { bid => 2, pid => 1, ret_id => 2, ret_cond_id => 1, price_context => 10, },
            ],
            camp_strategy => {
                1 => { is_autobudget => 0, },
            },
            old_retargetings_by_pid_hash => { },
        },
    };

    $params = { Action => 'Add', };

    is_deeply(
        Retargeting( $self, $params ),
        {
            ActionsResult => [
                {
                    Warnings => [
                        {
                            Description   => '{}',
                            WarningCode   => 210,
                            WarningString => 'Условие не было добавлено. Группа уже содержит указанное условие',
                        },
                    ],
                },
                {
                    RetargetingID => 1, 
                },
            ],
        },
        'RetargetingConditionAlreadyExistsInAdGroup warning'
    );

    is_deeply(
        \@ARGS,
        [
            {
                pid          => 1,
                bid          => 2,
                cid          => 1,
                currency     => undef,
                retargetings => [
                    { bid => 2, pid => 1, ret_id => 2, ret_cond_id => 1, price_context => 10, },
                ],
            },
        ],
        'check Retargeting::update_group_retargetings params'
    );

    # clean up
    $ID   = 0;
    @ARGS = ();

    #done_testing();
};

subtest 'API::Methods::Retargeting::validate_retargeting - Update methods' => sub {
    plan tests => 7;

    my @ARGS;
    my $ID = 0;

    {
        no strict 'refs';
        no warnings 'redefine';

        *{'Retargeting::update_group_retargetings'} = sub { push @ARGS, $_[0]; [ ++$ID ] };
    }

    # do not process retargeting with errors
    my $self = {
        ret => [
            { Errors => [ 'fake' ], },
        ],
        preprocess => {
            bid2pid => { 1 => 1, },
            converted => [
                { bid => 1, pid => 1, ret_cond_id => 1, },
            ],
        },
    };
    my $params = { Action => 'Update', };

    is_deeply(
        Retargeting( $self, $params ),
        {
            ActionsResult => [
                { Errors => [ 'fake' ], },
            ],
        },
        'do not process retargeting with errors'
    );

    # RetargetingContextPriceIgnored warning - no old retargeting
    $self = {
        preprocess => {
            camps   => { 1 => { currency => undef, }, },
            pid2cid => { 1 => 1, },
            converted => [
                { bid => 1, pid => 1, ret_id => 1, ret_cond_id => 1, price_context => 5, },
            ],
            camp_strategy => {
                1 => { is_autobudget => 1, },
            },
            old_retargetings => { },
        },
    };

    $params = { Action => 'Update', };

    is_deeply(
        Retargeting( $self, $params ),
        {
            ActionsResult => [
                {
                    RetargetingID => 1,
                    Warnings      => [
                        {
                            Description   => '{}',
                            WarningCode   => 209,
                            WarningString => 'Ставка для условия ретаргетинга была проигнорирована, так как включен автобюджет',
                        },
                    ],
                },
            ],
        },
        'RetargetingContextPriceIgnored warning - no old retargeting'
    );

    is_deeply(
        \@ARGS,
        [
            {
                pid          => 1,
                bid          => 1,
                cid          => 1,
                currency     => undef,
                retargetings => [
                    { bid => 1, pid => 1, ret_id => 1, ret_cond_id => 1, price_context => 5, },
                ],
            },
        ],
        'check Retargeting::update_group_retargetings params'
    );

    # clean up
    $ID   = 0;
    @ARGS = ();

    # RetargetingContextPriceIgnored warning - new price_context != old price_context
    $self = {
        preprocess => {
            camps   => { 1 => { currency => undef, }, },
            pid2cid => { 1 => 1, },
            converted => [
                { bid => 1, pid => 1, ret_id => 1, ret_cond_id => 1, price_context => 5, },
            ],
            camp_strategy => {
                1 => { is_autobudget => 1, },
            },
            old_retargetings => {
                1 => {
                    ret_id        => 1,
                    price_context => 10,
                },
            },
        },
    };

    $params = { Action => 'Add', };

    is_deeply(
        Retargeting( $self, $params ),
        {
            ActionsResult => [
                {
                    RetargetingID => 1,
                    Warnings      => [
                        {
                            Description   => '{}',
                            WarningCode   => 209,
                            WarningString => 'Ставка для условия ретаргетинга была проигнорирована, так как включен автобюджет',
                        },
                    ],
                },
            ],
        },
        'RetargetingContextPriceIgnored warning - new price_context != old price_context'
    );

    is_deeply(
        \@ARGS,
        [
            {
                pid          => 1,
                bid          => 1,
                cid          => 1,
                currency     => undef,
                retargetings => [
                    { bid => 1, pid => 1, ret_id => 1, ret_cond_id => 1, price_context => 5, },
                ],
            },
        ],
        'check Retargeting::update_group_retargetings params'
    );

    # clean up
    $ID   = 0;
    @ARGS = ();

    # RetargetingConditionAlreadyExistsInAdGroup warning
    $self = {
        preprocess => {
            camps   => { 1 => { currency => undef, }, },
            pid2cid => { 1 => 1, },
            converted => [
                { bid => 1, pid => 1, ret_id => 1, ret_cond_id => 1, price_context => 5, },
                { bid => 2, pid => 1, ret_id => 2, ret_cond_id => 1, price_context => 10, },
            ],
            camp_strategy => {
                1 => { is_autobudget => 0, },
            },
            old_retargetings_by_pid_hash => { },
        },
    };

    $params = { Action => 'Update', };

    is_deeply(
        Retargeting( $self, $params ),
        {
            ActionsResult => [
                {
                    Warnings => [
                        {
                            Description   => '{}',
                            WarningCode   => 210,
                            WarningString => 'Условие не было добавлено. Группа уже содержит указанное условие',
                        },
                    ],
                },
                {
                    RetargetingID => 1, 
                },
            ],
        },
        'RetargetingConditionAlreadyExistsInAdGroup warning'
    );

    is_deeply(
        \@ARGS,
        [
            {
                pid          => 1,
                bid          => 2,
                cid          => 1,
                currency     => undef,
                retargetings => [
                    { bid => 2, pid => 1, ret_id => 2, ret_cond_id => 1, price_context => 10, },
                ],
            },
        ],
        'check Retargeting::update_group_retargetings params'
    );

    # clean up
    $ID   = 0;
    @ARGS = ();

    #done_testing();
};

subtest 'API::Methods::Retargeting::GetRetargetingGoals' => sub {
    plan tests => 4;

    %methods_args_and_replies = (
        rbac_get_client_uids_by_clientid => [
            [ 117 ] => [17, 18]
        ],
        get_metrika_goals_by_uid => [
            [ bag(17,18), timeout => ignore() ] => {
                17 => [
                    {
                        goal_id => 1,
                        goal_name => 'goal 1',
                        goal_domain => 'test.ru',
                        goal_type => 'goal'
                    }
                ],
                18 => [
                    {
                        goal_id => 10,
                        goal_name => 'segment 1',
                        goal_domain => 'site.ru',
                        goal_type => 'segment'
                    },
                    {
                        goal_id => 2,
                        goal_name => 'goal 2',
                        goal_domain => 'site2.ru',
                        goal_type => 'goal'
                    },
                ]
            }
        ]
    );

    my $self = {
        rbac_login_rights => { role => 'client' },
        user_info => { ClientID => 117, login => 'test' }
    };

    my $params = {};
    my $result = API::Methods::Retargeting::GetRetargetingGoals(dclone($self), dclone($params));
    ok($result, 'Had to get result');
    my $expected_result = bag(
        {
            GoalID => 1,
            Name => 'goal 1',
            GoalDomain => 'test.ru',
            Login => 'test',
            Type  => 'goal'
        },
        {
            GoalID => 10,
            Name => 'segment 1',
            GoalDomain => 'site.ru',
            Login => 'test',
            Type  => 'segment'
        },
        {
            GoalID => 2,
            Name => 'goal 2',
            GoalDomain => 'site2.ru',
            Login => 'test',
            Type  => 'goal'
        }
    );
    cmp_deeply($result, $expected_result, 'Had to get retargeting goal/segment');

    %methods_args_and_replies = (
        rbac_get_client_uids_by_clientid => [
            [ 117 ] => [17, 18]
        ],
        get_metrika_goals_by_uid => [
            [ bag(17,18), timeout => ignore() ] => {}
        ]
    );

    $result = API::Methods::Retargeting::GetRetargetingGoals(dclone($self), dclone($params));
    $expected_result = [];
    cmp_deeply($result, $expected_result, 'Had to get an empty result for transport error');

    %methods_args_and_replies = (
        rbac_get_client_uids_by_clientid => [
            [ 117 ] => [17, 18]
        ],
        get_metrika_goals_by_uid => [
            [ bag(17,18), timeout => ignore() ] => {
                17 => [],
                18 => []
            }
        ]
    );

    $result = API::Methods::Retargeting::GetRetargetingGoals(dclone($self), dclone($params));
    $expected_result = [];
    cmp_deeply($result, $expected_result, 'Had to get an empty result for no data from Metrika');
};

subtest 'test preprocess_retargeting_condition for Add' => sub {
    plan tests => 1;

    my $self = {
        rbac_login_rights => { role => 'client' },
        user_info => { ClientID => 117, login => 'test' }
    };

    my $params = {
        Action => 'Add',
        RetargetingConditions => [
            {
                RetargetingConditionName => 'условие 1',
                RetargetingConditionDescription => 'описание условия 1',
                Login => 'test',
                RetargetingCondition => [
                    {
                        Type => 'or',
                        Goals => [
                            {
                               Time => 1,
                               GoalID => 16,
                               Type => 'goal',
                               ExcessField => 'value'
                            }
                        ]
                    }
                ]
            }
        ]
    };

    %methods_args_and_replies = (
        mass_get_retargeting_conditions_by_ClientIDS => [
            [ [ 117 ], short => 1 ] => { 117 => {} }
        ],
    );

    my $expected_result = {
        converted => [
            {
                ClientID  => 117,
                condition => [
                    {
                        goals => [
                            {
                                goal_id => 16,
                                time    => 1,
                                goal_type => 'goal'
                            }
                        ],
                        type => "or"
                    }
                ],
                condition_desc => "описание условия 1",
                condition_name => "условие 1",
                Login          => "test"
            }
        ],
        exists_cond => {
            117 => {}
        },
        login2clientid => {
            test => 117
        }
    };
    
    API::Methods::Retargeting::preprocess_retargeting_condition($self, $params);
    cmp_deeply($self->{preprocess}, $expected_result, 'creating preprocess struct');
};

subtest 'preprocess_retargeting_condition for Update' => sub {
    plan tests => 1;

    my $self = {
        rbac_login_rights => { role => 'client' },
        user_info => { ClientID => 117, login => 'test' }
    };

    my $params = {
        Action => 'Update',
        RetargetingConditions => [
            {
                Fields => ["RetargetingCondition"],
                RetargetingConditionID =>   250346,
                RetargetingConditionName => 'условие 1',
                RetargetingConditionDescription => 'описание условия 1',
                RetargetingCondition => [
                    {
                        Type => 'or',
                        Goals => [
                            {
                               Time => 1,
                               GoalID => 16,
                               Type => 'goal',
                               ExcessField => 'value'
                            }
                        ]
                    }
                ]
            }
        ]
    };

    %methods_args_and_replies = (
        mass_get_retargeting_conditions_by_ClientIDS => [
            [ [], ret_cond_id => [ 250346 ] ] => {
                117 =>   {
                    250346 =>   {
                        ClientID =>   117,
                        condition =>   [
                            {
                                goals =>   [
                                    {
                                        goal_id =>   12,
                                        goal_type => 'goal',
                                        time =>   1
                                    }
                                ],
                                type =>   "or"
                            }
                        ],
                        condition_desc =>   "описание условия 1",
                        condition_name =>   "условие 1",
                        is_accessible =>   1,
                        ret_cond_id =>   250346
                    }
                }
            },
            [ [ 117 ], short => 1 ] => {
                117 =>   {
                    250346 =>   {
                        ClientID =>   117,
                        condition =>   [
                            {
                                goals =>   [
                                    {
                                        goal_id =>   12,
                                        goal_type => 'goal',
                                        time =>   1
                                    }
                                ],
                                type =>   "or"
                            }
                        ],
                        condition_desc =>   "описание условия 1",
                        condition_name =>   "условие 1",
                        is_accessible =>   1,
                        ret_cond_id =>   250346
                    }
                }
            }
        ],
    );

    my $expected_result = {
        converted => [
            {
                ClientID  => 117,
                condition => [
                    {
                        goals => [
                            {
                                goal_id => 16,
                                goal_type => 'goal',
                                time    => 1
                            }
                        ],
                        type => "or"
                    }
                ],
                condition_desc => "описание условия 1",
                condition_name => "условие 1",
                Fields          => [ "RetargetingCondition" ],
                is_accessible =>    1,
                ret_cond_id => 250346
            }
        ],
        exists_cond => {
            117 =>   {
                250346 =>   {
                    ClientID =>   117,
                    condition =>   [
                        {
                            goals =>   [
                                {
                                    goal_id =>   12,
                                    goal_type => 'goal',
                                    time =>   1
                                }
                            ],
                            type =>   "or"
                        }
                    ],
                    condition_desc =>   "описание условия 1",
                    condition_name =>   "условие 1",
                    is_accessible =>   1,
                    ret_cond_id =>   250346
                }
            }
        }
    };
    
    API::Methods::Retargeting::preprocess_retargeting_condition($self, $params);
    cmp_deeply($self->{preprocess}, $expected_result, 'creating preprocess struct');
};

subtest 'API::Methods::Retargeting::RetargetingCondition action Get' => sub {
    plan tests => 2;

    %methods_args_and_replies = ();

    my $self = {
        preprocess => {
            conditions => {
                117 => {
                    250346 => {
                        ClientID  => 117,
                        condition => [
                            {
                                goals => [
                                    {
                                        goal_id => 8,
                                        time    => 1,
                                        type    => 'goal'
                                    },
                                    {
                                        goal_id => 16,
                                        time    => 1,
                                        type    => 'segment'
                                    }
                                ],
                                type => "or"
                            }
                        ],
                        condition_desc => "описание условия 1",
                        condition_name => "условие 1",
                        is_accessible =>    1,
                        ret_cond_id => 250346
                    }
                }
            },
            clientid2login => {
                117 => [ 'test' ]
            }
        }
    };
    my $params = {
        Action => 'Get',
        SelectionCriteria => {
            Logins => ['test']
        }
    };
    my $result = API::Methods::Retargeting::RetargetingCondition($self, $params);
    ok($result, 'Had to get result');
    my $expected_result = {
        RetargetingConditions => [
            {
                IsAccessible         => 'Yes',
                Login                => 'test',
                RetargetingCondition => [
                    {
                        Goals => [
                            {
                                GoalID => 8,
                                Time   => 1
                            },
                            {
                                GoalID => 16,
                                Time   => 1
                            }
                        ],
                        Type => 'or'
                    }
                ],
                RetargetingConditionDescription => 'описание условия 1',
                RetargetingConditionID => 250346,
                RetargetingConditionName => 'условие 1'
            }
        ]
    };
    cmp_deeply($result, $expected_result, 'test filtering of goal type field');
};

#done_testing();
