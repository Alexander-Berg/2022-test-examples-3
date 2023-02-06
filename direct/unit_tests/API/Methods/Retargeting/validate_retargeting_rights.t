#!/usr/bin/perl

use strict;
use warnings;

use utf8;

use my_inc '../../../../';

use Settings;

use Test::More tests => 5;

use Yandex::DBUnitTest qw/:all/;

use Yandex::Test::UTF8Builder;

use_ok('API::Methods::Retargeting');

my $dataset = {

    shard_client_id => {
        original_db => PPCDICT(),
        rows => [ 
            { ClientID => 2, shard => 1, },
        ],
    },

    shard_inc_bid => {
        original_db => PPCDICT(),
        rows => [ 
            { bid => 1, ClientID => 2, },
            { bid => 2, ClientID => 2, },
            { bid => 3, ClientID => 2, },
        ],
    },

    shard_inc_cid => {
        original_db => PPCDICT(),
        rows => [ 
            { cid => 1, ClientID => 2, },
            { cid => 2, ClientID => 2, },
            { cid => 3, ClientID => 2, },
        ],
    },

    shard_inc_pid => {
        original_db => PPCDICT(),
        rows => [ 
            { pid => 1, ClientID => 2, },
            { pid => 2, ClientID => 2, },
            { pid => 3, ClientID => 2, },
        ],
    },

    shard_inc_ret_cond_id => {
        original_db => PPCDICT(),
        rows => [
            { ret_cond_id => 1, ClientID => 2, },
            { ret_cond_id => 2, ClientID => 2, },
            { ret_cond_id => 3, ClientID => 2, },
        ],
    },

    shard_uid => {
        original_db => PPCDICT(),
        rows => [ 
            { uid => 2, ClientID => 2, },
        ],
    },

    currency_convert_queue => {
        original_db => PPC(shard => 1),
        rows => {}
    },

    banners => {
        original_db => PPC(shard => 1),
        rows => {
            1 => [
                { cid => 1, bid => 1, },
                { cid => 2, bid => 2, },
                { cid => 3, bid => 3, },
            ],
        },
    },    

    campaigns => {
        original_db => PPC(shard => 1),
        rows => {
            1 => [
                { cid => 1, uid => 2, statusEmpty => 'no', },
                { cid => 2, uid => 2, statusEmpty => 'no', },
                { cid => 3, uid => 2, statusEmpty => 'no', currency => 'RUB', },
            ],
        },
    },

    phrases => {
        original_db => PPC(shard => 1),
        rows => {
            1 => [
                { cid => 1, pid => 1, },
                { cid => 2, pid => 2, },
                { cid => 3, pid => 3, },
            ],
        },
    },

    retargeting_conditions => {
        original_db => PPC(shard => 1),
        rows => {
            1 => [
                { ret_cond_id => 1, condition_name => 'Condition #1', condition_desc => 'Description for condition #1', condition_json => '[]', ClientID => 1, is_deleted => 0, },
                { ret_cond_id => 2, condition_name => 'Condition #2', condition_desc => 'Description for condition #2', condition_json => '[]', ClientID => 2, is_deleted => 1, },
                { ret_cond_id => 3, condition_name => 'Condition #3', condition_desc => 'Description for condition #3', condition_json => '[]', ClientID => 2, is_deleted => 0, },
            ],
        },
    },

    retargeting_goals => {
        original_db => PPC(shard => 1),
        rows => {
            1 => [
                { ret_cond_id => 3, is_accessible => 0, },
            ],
        },
    },

    users => {
        original_db => PPC(shard => 1),
        rows => {
            1 => [
                { uid => 2, ClientID => 2, login => 'holodilnikru' },
            ],
        },
    },

};

init_test_dataset( $dataset );

sub get_self {
    my %self = (
        client => {
            rbac_login_rights => {
                role => 'client'
            },
            user_info => {
                login => 'holodilnikru'
            },
            uid => 2
        },
        manager => {
            rbac_login_rights => {
                role => 'manager'
            },
            user_info => {
                login => 'holodilnikru'
            },
            uid => 1
        },
    );
    return $self{ $_[0] };
}

{
    no strict 'refs';
    no warnings 'redefine';

    *{"API::Methods::Retargeting::rbac_user_allow_edit_camps_detail"} = sub {
        my ($rbac, $uid, $cids) = @_;

        return { map { $_ => 1 } @$cids };
    };
}

*validate_retargeting_rights = \&API::Methods::Retargeting::validate_retargeting_rights;

subtest 'API::Methods::Retargeting::validate_retargeting_rights - Add method' => sub {
    plan tests => 4;

    # do not process retargeting with errors
    my $client = get_self('client');
    $client->{ret} = [
        { Errors => [ 'fake' ], },
    ];
    $client->{preprocess} = {
        pid2cid   => { 1 => 1, },
        cid2clientid => { 1 => 2, 2 => 2, 3 => 2 },
        converted => [
            {
                bid         => 1,
                pid         => 1,
                ret_cond_id => 1,
            },
        ],
    };

    my $params = {
        Action       => 'Add',
        Retargetings => [
            { AdID => 1, RetargetingConditionID => 1, },
        ],
    };
    {
        no strict 'refs';
        no warnings 'redefine';
        *{'Client::mass_client_must_convert'} = sub {
            my ($client_ids) = @_;
            return {};
        };

        validate_retargeting_rights( $client, $params );
    }

    is_deeply(
        $client->{ret},
        [
            { Errors => [ 'fake' ], },
        ],
        'do not process retargeting with errors'
    );

    # NoRights error - Add and rbac_user_allow_edit_camps_detail return 0 
    {
        no strict 'refs';
        no warnings 'redefine';

        my $origin_subref = \&API::Methods::Retargeting::rbac_user_allow_edit_camps_detail;

        *API::Methods::Retargeting::rbac_user_allow_edit_camps_detail = sub {
            my ($rbac, $uid, $cids) = @_;

            return { map { $_ => 0 } @$cids };
        };

        my $client = get_self('client');
        $client->{preprocess} = {
            pid2cid   => { 1 => 1, },
            cid2clientid => { 1 => 2, 2 => 2, 3 => 2 },
            converted => [
                {
                    bid         => 1,
                    pid         => 1,
                    ret_cond_id => 1,
                },
            ],
        };

        my $params = {
            Action       => 'Add',
            Retargetings => [
                { AdID => 1, RetargetingConditionID => 1, },
            ],
        };
        validate_retargeting_rights( $client, $params );

        is_deeply(
            $client->{ret},
            [
                {
                    Errors => [
                        {
                            FaultCode   => 54,
                            FaultString => 'Недостаточно прав',
                            FaultDetail => '',
                        }
                    ],
                },
            ],
            'NoRights error - Add and rbac_user_allow_edit_camps_detail return 0'
        );

        *API::Methods::Retargeting::rbac_user_allow_edit_camps_detail = $origin_subref;
    }

    # NoRights error - Add and bid for not exists campaign
    $client = get_self('client');
    $client->{preprocess} = {
        pid2cid   => { },
        cid2clientid => { },
        converted => [
            {
                bid         => 1,
                pid         => 1,
                ret_cond_id => 1,
            },
        ],
    };

    $params = {
        Action       => 'Add',
        Retargetings => [
            { AdID => 1, RetargetingConditionID => 1, },
        ],
    };
    validate_retargeting_rights( $client, $params );

    is_deeply(
        $client->{ret},
        [
            {
                Errors => [
                    {
                        FaultCode   => 54,
                        FaultString => 'Недостаточно прав',
                        FaultDetail => '',
                    }
                ],
            },
        ],
        'NoRights error - Add and bid for not exists campaign'
    );

    # BadRetargetingConditionID error
    {
        no strict 'refs';
        no warnings 'redefine';

        my $origin_subref = \&Retargeting::mass_get_retargeting_conditions_by_ClientIDS;

        *{"Retargeting::mass_get_retargeting_conditions_by_ClientIDS($;%)"} = sub { +{ map { $_ => 1 } @{ $_[2] } } };

        my $client = get_self('client');
        $client->{preprocess} = {
            pid2cid   => { 1 => 1, },
            cid2clientid => { 1 => 2, 2 => 2, 3 => 2 },
            converted => [
                {
                    bid         => 1,
                    pid         => 1,
                    ret_cond_id => 1,
                },
            ],
        };

        my $params = {
            Action       => 'Add',
            Retargetings => [
                { AdID => 1, RetargetingConditionID => 1, },
            ],
        };
        validate_retargeting_rights( $client, $params );

        is_deeply(
            $client->{ret},
            [
                {
                    Errors => [
                        {
                            FaultCode   => 47,
                            FaultString => 'Неверный идентификатор условия ретаргетинга',
                            FaultDetail => 'Несуществующее условие ретаргетинга',
                        }
                    ],
                },
            ],
            'BadRetargetingConditionID error'
        );

        *Retargeting::mass_get_retargeting_conditions_by_ClientIDS = $origin_subref;
    }

    #done_testing();
};

subtest 'API::Methods::Retargeting::validate_retargeting_rights - Update method' => sub {
    plan tests => 3;

    # do not process retargeting with errors
    my $client = get_self('client');
    $client->{ret} = [
        { Errors => [ 'fake' ], },
    ];
    $client->{preprocess} = {
        pid2cid   => { 1 => 1, },
        cid2clientid => { 1 => 2, 2 => 2, 3 => 2 },
        converted => [
            {
                bid         => 1,
                pid         => 1,
                ret_cond_id => 1,
            },
        ],
    };

    my $params = {
        Action       => 'Update',
        Retargetings => [
            { RetargetingID => 1, },
        ],
    };
    validate_retargeting_rights( $client, $params );

    is_deeply(
        $client->{ret},
        [
            { Errors => [ 'fake' ], },
        ],
        'do not process retargeting with errors'
    );

    # NoRights error - Update and rbac_user_allow_edit_camps_detail return 0 
    {
        no strict 'refs';
        no warnings 'redefine';

        my $origin_subref = \&API::Methods::Retargeting::rbac_user_allow_edit_camps_detail;

        *API::Methods::Retargeting::rbac_user_allow_edit_camps_detail = sub {
            my ($rbac, $uid, $cids) = @_;

            return { map { $_ => 0 } @$cids };
        };

        my $client = get_self('client');
        $client->{preprocess} = {
            pid2cid   => { 1 => 1, },
            cid2clientid => { 1 => 2, 2 => 2, 3 => 2 },
            converted => [
                {
                    bid         => 1,
                    pid         => 1,
                    ret_cond_id => 1,
                },
            ],
        };

        my $params = {
            Action       => 'Update',
            Retargetings => [
                { RetargetingID => 1, },
            ],
        };
        validate_retargeting_rights( $client, $params );

        is_deeply(
            $client->{ret},
            [
                {
                    Errors => [
                        {
                            FaultCode   => 54,
                            FaultString => 'Недостаточно прав',
                            FaultDetail => '',
                        }
                    ],
                },
            ],
            'NoRights error - Update and rbac_user_allow_edit_camps_detail return 0 '
        );

        *API::Methods::Retargeting::rbac_user_allow_edit_camps_detail = $origin_subref;
    }

    # BadRetargetingID error
    $client = get_self('client');
    $client->{preprocess} = {
        pid2cid   => { 1 => 1, },
        cid2clientid => { 1 => 2, 2 => 2, 3 => 2 },
        converted => [
            {
                bid    => 1,
                pid    => 1,
                ret_id => 1
            },
        ],
        old_retargetings => { },
    };

    $params = {
        Action       => 'Update',
        Retargetings => [
            { RetargetingID => 1, },
        ],
    };
    validate_retargeting_rights( $client, $params );

    is_deeply(
        $client->{ret},
        [
            {
                Errors => [
                    {
                        FaultCode   => 46,
                        FaultString => 'Неверный идентификатор ретаргетинга',
                        FaultDetail => 'Несуществующий RetargetingID',
                    }
                ],
            },
        ],
        'BadRetargetingConditionID error'
    );

    #done_testing();
};

subtest 'API::Methods::Retargeting::validate_retargeting_rights - Get method' => sub {
    plan tests => 1;

    no strict 'refs';
    no warnings 'redefine';

    *RBAC2::DirectChecks::rbac_cmd_user_allow_show_camps = sub { 1 };

    # NoRights error
    my $client = get_self('client');
    $client->{preprocess} = {
        retargetings => {
            1 => { bid => 1, pid => 1, },
            2 => { bid => 2, pid => 2, },
        },
    };

    is_deeply(
        validate_retargeting_rights( $client, { Action => 'Get', } ),
        'NoRights',
        'NoRights error and Get'
    );

    #done_testing();
};

subtest 'API::Methods::Retargeting::validate_retargeting_rights - Delete method' => sub {
    plan tests => 1;

    # NoRights
    no strict 'refs';
    no warnings 'redefine';

    *RBAC2::DirectChecks::rbac_cmd_user_allow_show_camps = sub { 1 };

    # NoRights error
    my $client = get_self('client');
    $client->{preprocess} = {
        retargetings => {
            1 => { bid => 1, pid => 1, },
            2 => { bid => 2, pid => 2, },
        },
    };

    is_deeply(
        validate_retargeting_rights( $client, { Action => 'Delete', } ),
        'NoRights',
        'NoRights error and Delete'
    );

    #done_testing();
};

#done_testing();
